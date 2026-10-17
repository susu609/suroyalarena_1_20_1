package net.susu609.royalarena.server.match;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.GameType;
import net.susu609.royalarena.RoyalArena;
import net.susu609.royalarena.arena.ArenaPoint;
import net.susu609.royalarena.bot.BotBrain;
import net.susu609.royalarena.config.ArenaCommonConfig;
import net.susu609.royalarena.entity.ArenaEntityTypes;
import net.susu609.royalarena.entity.building.TowerHitboxEntity;
import net.susu609.royalarena.entity.building.TowerVisualType;
import net.susu609.royalarena.match.*;
import net.susu609.royalarena.network.ModNetwork;
import net.susu609.royalarena.network.s2c.S2CMatchSnapshotPacket;
import net.susu609.royalarena.server.arena.ArenaBuilder;
import net.susu609.royalarena.server.arena.ArenaReservation;
import net.susu609.royalarena.server.battle.BattleField;
import net.susu609.royalarena.tower.TowerId;

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
    private void botPlay() {
        if (bot == null)
            return;
        bot.setSettings(ArenaCommonConfig.botSettings());
        BotBrain.Play play = bot.tick(match, battle.engine());
        if (play != null)
            match.tryDeploy(bot.side(), play.slot(), play.x(), play.z());
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
                botPlay();
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
    /** A tower fell: remove its model, crack its base and play the blast. */
    private void towerDestroyed(TowerId id) {
        removeTowerHitbox(id);
        ArenaPoint point = match.arena().towerPoint(id);
        ArenaBuilder.markTowerDestroyed(arenaLevel, point);
        arenaLevel.sendParticles(ParticleTypes.EXPLOSION_EMITTER, point.x(),
                point.y() + 2.0D, point.z(), 1, 0.0D, 0.0D, 0.0D, 0.0D);
        arenaLevel.playSound(null, point.x(), point.y() + 2.0D, point.z(),
                SoundEvents.GENERIC_EXPLODE, SoundSource.BLOCKS, 1.5F, 0.8F);
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
