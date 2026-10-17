package net.susu609.royalarena.server.match;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.susu609.royalarena.RoyalArena;
import net.susu609.royalarena.arena.ArenaLayout;
import net.susu609.royalarena.arena.ArenaPoint;
import net.susu609.royalarena.bot.BotBrain;
import net.susu609.royalarena.card.BattleCard;
import net.susu609.royalarena.config.ArenaCommonConfig;
import net.susu609.royalarena.entity.ArenaEntityTypes;
import net.susu609.royalarena.entity.building.TowerHitboxEntity;
import net.susu609.royalarena.entity.building.TowerVisualType;
import net.susu609.royalarena.match.*;
import net.susu609.royalarena.network.ModNetwork;
import net.susu609.royalarena.network.s2c.S2CBotDragPacket;
import net.susu609.royalarena.network.s2c.S2CMatchSnapshotPacket;
import net.susu609.royalarena.network.s2c.S2CShakePacket;
import net.susu609.royalarena.server.arena.ArenaBuilder;
import net.susu609.royalarena.server.arena.ArenaReservation;
import net.susu609.royalarena.server.battle.BattleField;
import net.susu609.royalarena.tower.TowerId;
import net.susu609.royalarena.tower.TowerKind;

import java.util.*;

public final class ServerMatchSession {
    private static final int COUNTDOWN_TICKS = 20 * 3;
    private static final int FINISH_DISPLAY_TICKS = 20 * 3;

    private final MatchInstance match;
    private final ServerLevel arenaLevel;
    private final ArenaReservation reservation;
    private final Map<UUID, PlayerReturnState> returnStates = new HashMap<>();
    private final Map<TowerId, UUID> towerHitboxes =
            new EnumMap<>(TowerId.class);
    private final BattleField battle;
    private final BotBrain bot;

    private int tickCounter;
    /** The bot's card on its way to the arena, and the ticks it still needs. */
    private BotBrain.Play dragging;
    private int dragTicksLeft;
    private static final int DRAG_MIN_TICKS = 10;
    private static final int DRAG_MAX_TICKS = 18;
    private int finishedTicks;
    private boolean complete;

    /** {@code bot} plays for its side; null when both sides are players. */
    public ServerMatchSession(MatchInstance match, ServerLevel arenaLevel,
            ArenaReservation reservation, BotBrain bot)
    {
        this.bot = bot;
        this.match = match;
        this.arenaLevel = arenaLevel;
        this.reservation = reservation;
        this.battle = new BattleField(arenaLevel, match, this::towerDestroyed);
    }
    public void closeNow(MinecraftServer server) {
        if (complete)
            return;
        if (match.state() != MatchState.FINISHED)
            match.finish(null);
        endAndRestore(server);
    }
    public boolean complete() {
        return complete;
    }
    public void forceStop() {
        if (match.state() != MatchState.FINISHED)
            match.finish(null);
    }
    public void handleDeploy(ServerPlayer sender, int slot,
            BlockPos requested)
    {
        if (complete || sender.serverLevel() != arenaLevel)
            return;
        TeamSide side = match.sideOf(sender.getUUID());
        if (side == null)
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
    /** The bot plays through the same rules as a player. */
    private void botPlay(MinecraftServer server) {
        if (bot == null)
            return;
        if (dragging != null) {
            // The bot's "hand" is still carrying the card to its spot.
            if (--dragTicksLeft > 0)
                return;
            BotBrain.Play play = dragging;
            dragging = null;
            if (match.tryDeploy(bot.side(), play.slot(), play.x(),
                    play.z()).accepted())
                sync(server);
            return;
        }
        bot.setSettings(ArenaCommonConfig.botSettings());
        BotBrain.Play play = bot.tick(match, battle.engine());
        if (play != null)
            startDrag(server, play);
    }

    /**
     * Like a player dragging a card: the clients see the card travel from
     * the bot's King tower to its spot (a red hand with the card's ghosts),
     * and the card is played when it arrives. Faraway spots take longer.
     */
    private void startDrag(MinecraftServer server, BotBrain.Play play) {
        ArenaPoint king = match.arena().towerPoint(TowerId.king(bot.side()));
        double fromX = king.x();
        double fromZ = king.z() - ArenaLayout.sideSign(bot.side()) * 3.0D;
        double toX = play.x() + 0.5D;
        double toZ = play.z() + 0.5D;
        double distance = Math.hypot(toX - fromX, toZ - fromZ);
        dragging = play;
        dragTicksLeft = Math.max(DRAG_MIN_TICKS, Math.min(DRAG_MAX_TICKS,
                (int) Math.round(DRAG_MIN_TICKS + distance * 0.5D)));
        BattleCard card = match.player(bot.side()).deck().cardAt(play.slot());
        S2CBotDragPacket packet = new S2CBotDragPacket(card, bot.side(),
                (float) fromX, (float) fromZ, (float) toX, (float) toZ,
                dragTicksLeft);
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
            match.finish(side.opponent());
    }
    public ArenaReservation reservation() {
        return reservation;
    }
    public void start(MinecraftServer server) {
        ArenaBuilder.build(arenaLevel, match.arena());
        spawnTowerHitboxes();
        preparePlayer(server, TeamSide.BLUE);
        preparePlayer(server, TeamSide.RED);
        match.startCountdown(COUNTDOWN_TICKS);
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
    private void spawnTowerHitboxes() {
        for (TowerId id : TowerId.values()) {
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
                ModNetwork.sendToPlayer(
                        player, new S2CMatchSnapshotPacket(
                                MatchSnapshot.from(match, side)));
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
            if (finishedTicks == 1)
                sync(server);
            if (finishedTicks >= FINISH_DISPLAY_TICKS) {
                endAndRestore(server);
            }
        }
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
        arenaLevel.playSound(null, point.x(), y, point.z(),
                SoundEvents.GENERIC_EXPLODE, SoundSource.BLOCKS, 1.5F, 0.8F);
        arenaLevel.playSound(null, point.x(), y, point.z(),
                SoundEvents.STONE_BREAK, SoundSource.BLOCKS, 1.5F, 0.6F);
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
            UUID id = match.player(side).playerId();
            ServerPlayer player = server.getPlayerList().getPlayer(id);
            if (player == null)
                continue;
            PlayerReturnState state = returnStates.get(id);
            if (state != null) {
                ServerLevel targetLevel = server.getLevel(state.dimension());
                if (targetLevel != null) {
                    player.teleportTo(
                            targetLevel, state.x(), state.y(), state.z(),
                            state.yaw(), state.pitch());
                }
                player.setGameMode(state.gameType());
            }
            ModNetwork.sendToPlayer(
                    player,
                    new S2CMatchSnapshotPacket(MatchSnapshot.inactive()));
        }
        ArenaBuilder.clear(arenaLevel, match.arena());
        complete = true;
    }
}
