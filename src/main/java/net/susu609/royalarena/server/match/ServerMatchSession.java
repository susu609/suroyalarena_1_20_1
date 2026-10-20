package net.susu609.royalarena.server.match;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.item.FireworkRocketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.fml.ModList;
import net.susu609.royalarena.RoyalArena;
import net.susu609.royalarena.arena.ArenaLayout;
import net.susu609.royalarena.arena.ArenaPoint;
import net.susu609.royalarena.bot.BotBrain;
import net.susu609.royalarena.card.BattleCard;
import net.susu609.royalarena.card.Tribe;
import net.susu609.royalarena.combat.CombatUnit;
import net.susu609.royalarena.network.c2s.C2SSandboxPacket;
import net.susu609.royalarena.config.ArenaCommonConfig;
import net.susu609.royalarena.entity.ArenaEntityTypes;
import net.susu609.royalarena.entity.building.TowerHitboxEntity;
import net.susu609.royalarena.entity.building.TowerVisualType;
import net.susu609.royalarena.match.*;
import net.susu609.royalarena.network.ModNetwork;
import net.susu609.royalarena.network.s2c.S2CBotDragPacket;
import net.susu609.royalarena.network.s2c.S2CMatchResultPacket;
import net.susu609.royalarena.network.s2c.S2CMatchSnapshotPacket;
import net.susu609.royalarena.network.s2c.S2CArenaSoundPacket;
import net.susu609.royalarena.network.s2c.S2CShakePacket;
import net.susu609.royalarena.server.arena.ArenaBuilder;
import net.susu609.royalarena.server.arena.ArenaReservation;
import net.susu609.royalarena.server.battle.BattleField;
import net.susu609.royalarena.server.stats.MatchLogWriter;
import net.susu609.royalarena.stats.MatchLog;
import net.susu609.royalarena.tower.TowerId;
import net.susu609.royalarena.tower.TowerKind;

import java.util.*;

public final class ServerMatchSession {
    private static final int COUNTDOWN_TICKS = 20 * 3;
    /**
     * Longest the result screen is shown before the players are sent home
     * anyway; OK sends a player home sooner.
     */
    private static final int RESULT_MAX_TICKS = 20 * 15;
    /** Ticks after the end at which a firework goes up over the winner. */
    private static final int[] FIREWORK_TICKS = {28, 36, 46};

    private final MatchInstance match;
    private final ServerLevel arenaLevel;
    private final ArenaReservation reservation;
    private final Map<UUID, PlayerReturnState> returnStates = new HashMap<>();
    private final Map<TowerId, UUID> towerHitboxes =
            new EnumMap<>(TowerId.class);
    private final BattleField battle;
    /** Bots, one per side they play; empty when both sides are players. */
    private final List<Bot> bots = new ArrayList<>();
    /** The player watching a bot-versus-bot match, or null. */
    private final UUID spectator;
    private int speed = 1;
    /** Half match ticks owed by a slowed sandbox (0.5x runs every other tick). */
    private int halfTicks;

    /** A bot and the card its "hand" is carrying to the arena. */
    private static final class Bot {
        final BotBrain brain;
        BotBrain.Play dragging;
        int dragTicksLeft;

        Bot(BotBrain brain) {
            this.brain = brain;
        }
    }

    private int tickCounter;
    private static final int DRAG_MIN_TICKS = 10;
    private static final int DRAG_MAX_TICKS = 18;
    private int finishedTicks;
    private boolean complete;
    private long startedMillis = System.currentTimeMillis();
    private boolean logged;
    /** Players who closed the result screen (sent home already). */
    private final Set<UUID> done = new HashSet<>();
    /** Players who asked for another match against the bot. */
    private final List<UUID> rematch = new ArrayList<>();

    /** {@code bot} plays for its side; null when both sides are players. */
    public ServerMatchSession(MatchInstance match, ServerLevel arenaLevel,
            ArenaReservation reservation, BotBrain bot)
    {
        this(match, arenaLevel, reservation,
                bot == null ? List.of() : List.of(bot), null);
    }

    /**
     * Any number of bots; with {@code spectator} set both sides are bots and
     * that player (BLUE's id) only watches.
     */
    public ServerMatchSession(MatchInstance match, ServerLevel arenaLevel,
            ArenaReservation reservation, List<BotBrain> bots,
            UUID spectator)
    {
        for (BotBrain brain : bots)
            this.bots.add(new Bot(brain));
        this.spectator = spectator;
        this.match = match;
        this.arenaLevel = arenaLevel;
        this.reservation = reservation;
        this.battle = new BattleField(arenaLevel, match, this::towerDestroyed);
        // A sandbox picks its tribe steps by hand, so they are always on.
        if (match.sandbox() != null)
            battle.engine().setTribes(true);
    }
    public void closeNow(MinecraftServer server) {
        if (complete)
            return;
        if (match.state() != MatchState.FINISHED)
            match.finish(null, MatchRecord.EndReason.ABANDONED);
        logMatch();
        endAndRestore(server);
    }
    public boolean complete() {
        return complete;
    }
    public void forceStop() {
        if (match.state() != MatchState.FINISHED)
            match.finish(null, MatchRecord.EndReason.ABANDONED);
    }
    public void handleDeploy(ServerPlayer sender, int slot,
            BlockPos requested)
    {
        if (complete || sender.serverLevel() != arenaLevel)
            return;
        TeamSide side = match.sideOf(sender.getUUID());
        if (side == null || botOf(side) != null)
            return;
        int x = requested.getX();
        int z = requested.getZ();
        DeployResult result = match.tryDeploy(side, slot, x, z);
        if (!result.accepted())
            return;
        MinecraftServer server = sender.getServer();
        if (server != null)
            sync(server);
    }
    /**
     * Adds this match to the match log (once), unless it is turned off or
     * the match never got to the battle.
     */
    private void logMatch() {
        if (logged || match.sandbox() != null ||
                !ArenaCommonConfig.RECORD_MATCHES.get() ||
                match.record().battleTicks() == 0)
            return;
        logged = true;
        MatchLog.Side[] sides = new MatchLog.Side[2];
        for (TeamSide side : TeamSide.values()) {
            BotBrain brain = botOf(side);
            sides[side.ordinal()] = brain != null ?
                    MatchLog.Side.bot(brain.personality().id()) :
                    MatchLog.Side.player(match.player(side).playerId());
        }
        String version = ModList.get().getModContainerById(RoyalArena.MOD_ID)
                .map(c -> c.getModInfo().getVersion().toString())
                .orElse("unknown");
        MatchLogWriter.append(MatchLog.line(match, battle.engine().stats(),
                sides[TeamSide.BLUE.ordinal()], sides[TeamSide.RED.ordinal()],
                spectator != null ? "watch" : bots.isEmpty() ? "pvp" : "bot",
                version, startedMillis));
    }

    /** The bot playing this side, or null for a player. */
    private BotBrain botOf(TeamSide side) {
        for (Bot bot : bots) {
            if (bot.brain.side() == side)
                return bot.brain;
        }
        return null;
    }

    /** Whether this is a bot-versus-bot match someone watches. */
    public boolean watching() {
        return spectator != null;
    }

    /** Playback speed of a watched match: 1, or 2 (two ticks per tick). */
    public int speed() {
        return speed;
    }

    /**
     * Match ticks to run this server tick: the watch speed, or a sandbox's
     * (paused, 0.5x, 1x, 2x). A finished match always runs, so it can end.
     */
    public int ticksThisServerTick() {
        SandboxSettings box = match.sandbox();
        if (box == null || match.state() == MatchState.FINISHED)
            return speed;
        halfTicks += box.speed();
        int ticks = halfTicks / 2;
        halfTicks %= 2;
        return ticks;
    }

    // ------------------------------------------------------------- sandbox

    /** A sandbox panel request (docs/SANDBOX.md); only from the BLUE player. */
    public void handleSandbox(ServerPlayer sender, C2SSandboxPacket packet) {
        SandboxSettings box = match.sandbox();
        MinecraftServer server = sender.getServer();
        if (box == null || complete || server == null ||
                !sender.getUUID().equals(match.blue().playerId()))
            return;
        switch (packet.action()) {
            case DEPLOY -> {
                BattleCard card = BattleCard.byId(packet.card());
                if (card == null || !match.sandboxDeploy(packet.side(), card,
                        packet.pos().getX(), packet.pos().getZ()).accepted())
                    return;
            }
            case CLEAR -> {
                battle.clear();
                battle.engine().clearSpellsAndShots();
                match.clearPending();
            }
            case RESTORE_TOWERS -> restoreTowers();
            case TOWERS_UNBREAKABLE -> box.setTowersInvulnerable(
                    packet.value() != 0);
            case INFINITE_ELIXIR -> box.setInfiniteElixir(packet.value() != 0);
            case SPEED -> box.setSpeed(packet.value());
            case TRIBE -> {
                Tribe[] tribes = Tribe.values();
                if (packet.tribe() >= 0 && packet.tribe() < tribes.length)
                    box.setTribeLevel(packet.side(), tribes[packet.tribe()],
                            packet.value());
            }
            case EXIT -> {
                closeNow(server);
                return;
            }
            default -> {
                return;
            }
        }
        sync(server);
    }

    /**
     * Every tower back to full health: the floor is laid again (the rubble
     * goes) and the fallen towers' models come back.
     */
    private void restoreTowers() {
        match.resetTowers();
        ArenaBuilder.build(arenaLevel, match.arena());
        spawnTowerHitboxes();
        battle.forgetTowers();
    }

    /** What every troop aims at, for the sandbox stats overlay. */
    private List<MatchSnapshot.Aim> aims() {
        List<MatchSnapshot.Aim> out = new ArrayList<>();
        for (CombatUnit unit : battle.engine().units()) {
            TowerId tower = unit.target() == null ? unit.lockedTower() : null;
            out.add(new MatchSnapshot.Aim(unit.id(), unit.target(),
                    tower == null ? -1 : tower.ordinal()));
        }
        return out;
    }

    /** The spectator switches between normal and double speed. */
    public void toggleSpeed(MinecraftServer server, UUID playerId) {
        if (!playerId.equals(spectator) || complete)
            return;
        speed = speed == 1 ? 2 : 1;
        sync(server);
    }

    /** What the spectator sees of both bots; null for a played side. */
    private MatchSnapshot.Watch watchFor(TeamSide side) {
        if (spectator == null || side != TeamSide.BLUE)
            return null;
        BotBrain blue = botOf(TeamSide.BLUE);
        BotBrain red = botOf(TeamSide.RED);
        return MatchSnapshot.watch(match,
                blue == null ? "" : blue.personality().displayName(),
                red == null ? "" : red.personality().displayName(), speed);
    }

    /** Every bot plays through the same rules as a player. */
    private void botPlay(MinecraftServer server) {
        for (Bot bot : bots) {
            if (bot.dragging != null) {
                // The bot's "hand" is still carrying the card to its spot.
                if (--bot.dragTicksLeft > 0)
                    continue;
                BotBrain.Play play = bot.dragging;
                bot.dragging = null;
                if (match.tryDeploy(bot.brain.side(), play.slot(), play.x(),
                        play.z()).accepted())
                    sync(server);
                continue;
            }
            bot.brain.setSettings(ArenaCommonConfig.botSettings());
            BotBrain.Play play = bot.brain.tick(match, battle.engine());
            if (play != null)
                startDrag(server, bot, play);
        }
    }

    /**
     * Like a player dragging a card: the clients see the card travel from
     * the bot's King tower to its spot (a red hand with the card's ghosts),
     * and the card is played when it arrives. Faraway spots take longer.
     */
    private void startDrag(MinecraftServer server, Bot bot,
            BotBrain.Play play)
    {
        TeamSide botSide = bot.brain.side();
        ArenaPoint king = match.arena().towerPoint(TowerId.king(botSide));
        double fromX = king.x();
        double fromZ = king.z() - ArenaLayout.sideSign(botSide) * 3.0D;
        double toX = play.x() + 0.5D;
        double toZ = play.z() + 0.5D;
        double distance = Math.hypot(toX - fromX, toZ - fromZ);
        bot.dragging = play;
        bot.dragTicksLeft = Math.max(DRAG_MIN_TICKS, Math.min(DRAG_MAX_TICKS,
                (int) Math.round(DRAG_MIN_TICKS + distance * 0.5D)));
        BattleCard card = match.player(botSide).deck().cardAt(play.slot());
        S2CBotDragPacket packet = new S2CBotDragPacket(card, botSide,
                (float) fromX, (float) fromZ, (float) toX, (float) toZ,
                bot.dragTicksLeft);
        for (TeamSide side : TeamSide.values()) {
            ServerPlayer player = server.getPlayerList()
                    .getPlayer(match.player(side).playerId());
            if (player != null)
                ModNetwork.sendToPlayer(player, packet);
        }
    }

    /** Spawns troops whose deploy time ran out and lands arrived spells. */
    private void spawnReadyDeploys() {
        for (PendingDeploy deploy : match.drainReadyDeploys())
            battle.deploy(deploy);
    }
    public MatchInstance match() {
        return match;
    }

    public void onPlayerDisconnected(UUID playerId) {
        TeamSide side = match.sideOf(playerId);
        if (side != null && match.state() != MatchState.FINISHED)
            match.finish(side.opponent(), MatchRecord.EndReason.DISCONNECT);
    }
    public ArenaReservation reservation() {
        return reservation;
    }
    public void start(MinecraftServer server) {
        startedMillis = System.currentTimeMillis();
        ArenaBuilder.build(arenaLevel, match.arena());
        spawnTowerHitboxes();
        preparePlayer(server, TeamSide.BLUE);
        preparePlayer(server, TeamSide.RED);
        // A sandbox starts at once.
        match.startCountdown(match.sandbox() != null ? 0 : COUNTDOWN_TICKS);
        sync(server);
    }

    private void preparePlayer(MinecraftServer server, TeamSide side) {
        ServerPlayer player =
                server.getPlayerList().getPlayer(match.player(side).playerId());
        if (player == null)
            return;
        returnStates.put(player.getUUID(), PlayerReturnState.capture(player));
        player.setGameMode(GameType.SPECTATOR);
        ArenaPoint anchor = match.arena().controllerAnchor(side);
        player.teleportTo(
                arenaLevel, anchor.x() + 0.5D, anchor.y(), anchor.z() + 0.5D,
                side == TeamSide.BLUE ? 180.0F : 0.0F, 35.0F);
    }

    private void keepControllerPlayersAnchored(MinecraftServer server) {
        if (tickCounter % 10 != 0)
            return;
        for (TeamSide side : TeamSide.values()) {
            ServerPlayer player = server.getPlayerList()
                    .getPlayer(match.player(side).playerId());
            if (player == null)
                continue;
            ArenaPoint anchor = match.arena().controllerAnchor(side);
            player.teleportTo(
                    arenaLevel, anchor.x() + 0.5D, anchor.y(),
                    anchor.z() + 0.5D, side == TeamSide.BLUE ? 180.0F : 0.0F,
                    35.0F);
        }
    }
    /** Tower models for every tower that has none (all of them at the start). */
    private void spawnTowerHitboxes() {
        for (TowerId id : TowerId.values()) {
            if (towerHitboxes.containsKey(id))
                continue;
            TowerHitboxEntity hitbox =
                    ArenaEntityTypes.createTower(id.kind(), arenaLevel);
            if (hitbox == null) {
                RoyalArena.LOGGER.error(
                        "Could not create tower hitbox for {}", id);
                continue;
            }
            ArenaPoint point = match.arena().towerPoint(id);
            double centerOffset =
                    TowerVisualType.from(id.kind()).blockGridCenterOffset();
            hitbox.moveTo(
                    point.x() + centerOffset, point.y(),
                    point.z() + centerOffset,
                    id.side() == TeamSide.BLUE ? 180.0F : 0.0F, 0.0F);
            hitbox.addTag("royalarena_building");
            hitbox.addTag("royalarena_tower_" +
                    id.name().toLowerCase(java.util.Locale.ROOT));
            if (arenaLevel.addFreshEntity(hitbox)) {
                towerHitboxes.put(id, hitbox.getUUID());
            } else {
                RoyalArena.LOGGER.error(
                        "Could not add tower hitbox for {}", id);
            }
        }
    }
    private void sync(MinecraftServer server) {
        for (TeamSide side : TeamSide.values()) {
            ServerPlayer player = server.getPlayerList()
                    .getPlayer(match.player(side).playerId());
            if (player != null) {
                MatchSnapshot.Sandbox box = match.sandbox() == null ? null :
                        MatchSnapshot.sandbox(match, aims());
                ModNetwork.sendToPlayer(
                        player, new S2CMatchSnapshotPacket(
                                MatchSnapshot.from(match, side,
                                        watchFor(side), box)));
            }
        }
    }
    public void tick(MinecraftServer server) {
        if (complete)
            return;
        tickCounter++;

        if (match.state() != MatchState.FINISHED) {
            keepControllerPlayersAnchored(server);
            match.tick();
            spawnReadyDeploys();
            if (match.state() == MatchState.BATTLE) {
                botPlay(server);
                battle.tick();
                battle.showFlights(match.pendingDeploys());
            }
            if (tickCounter % 5 == 0 || match.state() == MatchState.FINISHED)
                sync(server);
        }

        if (match.state() == MatchState.FINISHED) {
            finishedTicks++;
            if (finishedTicks == 1) {
                if (match.winner() != null)
                    battle.celebrate(match.winner());
                logMatch();
                sync(server);
                sendResult(server);
            }
            for (int at : FIREWORK_TICKS) {
                if (finishedTicks == at)
                    fireworkOverWinner();
            }
            if (finishedTicks >= RESULT_MAX_TICKS || everyoneDone(server))
                endAndRestore(server);
        }
    }

    /** Opens the result screen of every player in the match. */
    private void sendResult(MinecraftServer server) {
        MatchResult result = MatchResult.of(match, battle.engine().stats(),
                !bots.isEmpty());
        String[] names = new String[2];
        for (TeamSide side : TeamSide.values()) {
            ServerPlayer player = server.getPlayerList()
                    .getPlayer(match.player(side).playerId());
            BotBrain brain = botOf(side);
            names[side.ordinal()] = brain != null ?
                    "Bot (" + brain.personality().displayName() + ")" :
                    player != null ? player.getGameProfile().getName() :
                            "Player";
        }
        for (TeamSide side : TeamSide.values()) {
            ServerPlayer player = server.getPlayerList()
                    .getPlayer(match.player(side).playerId());
            if (player != null)
                ModNetwork.sendToPlayer(player, new S2CMatchResultPacket(
                        result, side, names[0], names[1],
                        match.blue().playerId(), match.red().playerId()));
        }
    }

    /**
     * A player closed the result screen: they go home now, and with
     * {@code again} a new match against the bot starts once this one is
     * cleared away.
     */
    public void resultDone(MinecraftServer server, UUID playerId,
            boolean again)
    {
        if (complete || match.state() != MatchState.FINISHED ||
                !done.add(playerId))
            return;
        if (again && !bots.isEmpty())
            rematch.add(playerId);
        ServerPlayer player = server.getPlayerList().getPlayer(playerId);
        if (player != null)
            restore(server, player);
        if (everyoneDone(server))
            endAndRestore(server);
    }

    /** Players who asked for a rematch; read once the session is complete. */
    public List<UUID> rematchPlayers() {
        return Collections.unmodifiableList(rematch);
    }

    /** Every player still online has closed the result screen. */
    private boolean everyoneDone(MinecraftServer server) {
        for (TeamSide side : TeamSide.values()) {
            UUID id = match.player(side).playerId();
            if (server.getPlayerList().getPlayer(id) != null &&
                    !done.contains(id))
                return false;
        }
        return true;
    }

    /** A rocket bursting in the winner's colour over their King tower. */
    private void fireworkOverWinner() {
        TeamSide winner = match.winner();
        if (winner == null)
            return;
        ArenaPoint king = match.arena().towerPoint(TowerId.king(winner));
        CompoundTag explosion = new CompoundTag();
        explosion.putByte(FireworkRocketItem.TAG_EXPLOSION_TYPE,
                (byte) FireworkRocketItem.Shape.LARGE_BALL.getId());
        explosion.putIntArray(FireworkRocketItem.TAG_EXPLOSION_COLORS,
                winner == TeamSide.BLUE ? new int[] {0x3C8DFF, 0xFFD84A} :
                        new int[] {0xFF4E4E, 0xFFD84A});
        explosion.putBoolean(FireworkRocketItem.TAG_EXPLOSION_TRAIL, true);
        ListTag explosions = new ListTag();
        explosions.add(explosion);
        CompoundTag fireworks = new CompoundTag();
        fireworks.putByte(FireworkRocketItem.TAG_FLIGHT, (byte) 1);
        fireworks.put(FireworkRocketItem.TAG_EXPLOSIONS, explosions);
        ItemStack rocket = new ItemStack(Items.FIREWORK_ROCKET);
        rocket.getOrCreateTag().put(FireworkRocketItem.TAG_FIREWORKS,
                fireworks);
        double spread = (arenaLevel.random.nextDouble() - 0.5D) * 4.0D;
        arenaLevel.addFreshEntity(new FireworkRocketEntity(arenaLevel,
                king.x() + 0.5D + spread, king.y() + 6.0D, king.z() + 0.5D,
                rocket));
    }
    /**
     * A tower fell: remove its model, leave a pile of rubble where it stood,
     * and play the blast: an explosion, stone bricks flying, dust, and a
     * camera shake for everyone watching.
     */
    private void towerDestroyed(TowerId id) {
        removeTowerHitbox(id);
        ArenaPoint point = match.arena().towerPoint(id);
        ArenaBuilder.markTowerDestroyed(arenaLevel, point);
        ArenaBuilder.rubble(arenaLevel, point, id.kind() == TowerKind.KING ?
                ArenaLayout.KING_SIZE / 2 : ArenaLayout.PRINCESS_SIZE / 2);
        double y = point.y() + 2.0D;
        arenaLevel.sendParticles(ParticleTypes.EXPLOSION_EMITTER, point.x(),
                y, point.z(), 1, 0.0D, 0.0D, 0.0D, 0.0D);
        arenaLevel.sendParticles(ParticleTypes.EXPLOSION, point.x(), y,
                point.z(), 8, 1.5D, 1.0D, 1.5D, 0.0D);
        arenaLevel.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK,
                        Blocks.STONE_BRICKS.defaultBlockState()), point.x(), y,
                point.z(), 160, 1.6D, 1.4D, 1.6D, 0.4D);
        arenaLevel.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, point.x(),
                point.y() + 1.0D, point.z(), 20, 1.4D, 0.5D, 1.4D, 0.02D);
        S2CArenaSoundPacket.send(arenaLevel, point.x(), y, point.z(),
                SoundEvents.GENERIC_EXPLODE, SoundSource.BLOCKS, 1.0F, 0.8F);
        S2CArenaSoundPacket.send(arenaLevel, point.x(), y, point.z(),
                SoundEvents.STONE_BREAK, SoundSource.BLOCKS, 1.0F, 0.6F);
        ModNetwork.sendNear(arenaLevel, point.x(), y, point.z(), 96.0D,
                new S2CShakePacket(id.kind() == TowerKind.KING ? 2.0F : 1.2F));
    }
    private void removeTowerHitbox(TowerId id) {
        UUID uuid = towerHitboxes.remove(id);
        if (uuid == null)
            return;
        Entity entity = arenaLevel.getEntity(uuid);
        if (entity != null)
            entity.discard();
    }
    private void endAndRestore(MinecraftServer server) {
        battle.clear();
        for (UUID uuid : towerHitboxes.values()) {
            Entity entity = arenaLevel.getEntity(uuid);
            if (entity != null)
                entity.discard();
        }
        towerHitboxes.clear();
        for (TeamSide side : TeamSide.values()) {
            ServerPlayer player = server.getPlayerList()
                    .getPlayer(match.player(side).playerId());
            if (player != null)
                restore(server, player);
        }
        ArenaBuilder.clear(arenaLevel, match.arena());
        complete = true;
    }

    /**
     * Sends a player back where they were before the match (once) and tells
     * their client the match is over.
     */
    private void restore(MinecraftServer server, ServerPlayer player) {
        PlayerReturnState state = returnStates.remove(player.getUUID());
        if (state == null)
            return;
        ServerLevel targetLevel = server.getLevel(state.dimension());
        if (targetLevel != null) {
            player.teleportTo(
                    targetLevel, state.x(), state.y(), state.z(),
                    state.yaw(), state.pitch());
        }
        player.setGameMode(state.gameType());
        ModNetwork.sendToPlayer(
                player, new S2CMatchSnapshotPacket(MatchSnapshot.inactive()));
    }
}
