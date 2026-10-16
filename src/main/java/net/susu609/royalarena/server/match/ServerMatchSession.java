package net.susu609.royalarena.server.match;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;
import net.susu609.royalarena.RoyalArena;
import net.susu609.royalarena.arena.ArenaPoint;
import net.susu609.royalarena.arena.ArenaWaypoint;
import net.susu609.royalarena.card.BattleCard;
import net.susu609.royalarena.entity.ArenaEntityTypes;
import net.susu609.royalarena.entity.building.TowerHitboxEntity;
import net.susu609.royalarena.entity.building.TowerVisualType;
import net.susu609.royalarena.match.*;
import net.susu609.royalarena.network.ModNetwork;
import net.susu609.royalarena.network.s2c.S2CMatchSnapshotPacket;
import net.susu609.royalarena.server.arena.ArenaBuilder;
import net.susu609.royalarena.server.arena.ArenaReservation;
import net.susu609.royalarena.server.battle.ServerTroop;
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
    private final List<ServerTroop> troops = new ArrayList<>();

    private int tickCounter;
    private int finishedTicks;
    private boolean complete;

    public ServerMatchSession(MatchInstance match, ServerLevel arenaLevel,
            ArenaReservation reservation)
    {
        this.match = match;
        this.arenaLevel = arenaLevel;
        this.reservation = reservation;
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
    /** Spawns troops whose one-second deploy time has run out. */
    private void spawnReadyDeploys() {
        for (PendingDeploy deploy : match.drainReadyDeploys()) {
            if (!spawnTroop(deploy.card(), deploy.side(), deploy.x(),
                    deploy.z()))
            {
                RoyalArena.LOGGER.warn(
                        "Deployed card {} could not create entity {}",
                        deploy.card().id(), deploy.card().entityId());
            }
        }
    }
    private boolean spawnTroop(BattleCard card, TeamSide side, int x, int z) {
        EntityType<?> type = ForgeRegistries.ENTITY_TYPES.getValue(
                new ResourceLocation(card.entityId()));
        if (type == null)
            return false;
        Entity entity = type.create(arenaLevel);
        if (entity == null)
            return false;
        entity.moveTo(
                x + 0.5D, match.arena().floorY() + 1.0D, z + 0.5D,
                side == TeamSide.BLUE ? 180.0F : 0.0F, 0.0F);
        entity.setCustomName(Component.literal(card.displayName()).withStyle(
                side == TeamSide.BLUE ? ChatFormatting.BLUE :
                        ChatFormatting.RED));
        entity.addTag("royalarena_troop");
        entity.addTag("royalarena_team_" + side.name().toLowerCase());
        if (entity instanceof Mob mob) {
            mob.setNoAi(true);
            mob.setPersistenceRequired();
        }
        if (!arenaLevel.addFreshEntity(entity))
            return false;
        troops.add(new ServerTroop(
                entity.getUUID(), side, card.towerDamage(),
                card.moveSpeed()));
        return true;
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
            if (match.state() == MatchState.BATTLE)
                tickTroops();
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
    private void tickTroops() {
        Iterator<ServerTroop> iterator = troops.iterator();
        while (iterator.hasNext()) {
            ServerTroop troop = iterator.next();
            Entity entity = arenaLevel.getEntity(troop.entityId());
            if (entity == null || !entity.isAlive()) {
                iterator.remove();
                continue;
            }
            TowerId targetId = troop.targetTower(match, entity.getX());
            TowerHitboxEntity targetEntity = towerHitbox(targetId);
            if (targetEntity == null || targetEntity.isRemoved()) {
                entity.setDeltaMovement(Vec3.ZERO);
                continue;
            }
            ArenaWaypoint waypoint = match.arena().riverWaypoint(
                    troop.side(), entity.getX(), entity.getZ());
            if (waypoint != null) {
                steer(entity, troop, waypoint.x(), waypoint.z());
                continue;
            }
            double dx = targetEntity.getX() - entity.getX();
            double dz = targetEntity.getZ() - entity.getZ();
            double horizontal = Math.sqrt(dx * dx + dz * dz);
            double attackDistance = targetEntity.getBbWidth() * 0.5D +
                    entity.getBbWidth() * 0.5D + 0.75D;
            if (horizontal <= attackDistance) {
                entity.setDeltaMovement(Vec3.ZERO);
                if (troop.readyToAttack()) {
                    boolean wasAlive = !match.tower(targetId).isDestroyed();
                    match.damageTower(targetId, troop.towerDamage());
                    troop.resetAttackCooldown();
                    if (wasAlive && match.tower(targetId).isDestroyed()) {
                        removeTowerHitbox(targetId);
                        ArenaBuilder.markTowerDestroyed(
                                arenaLevel, match.arena().towerPoint(targetId));
                    }
                }
                continue;
            }
            steer(entity, troop, targetEntity.getX(), targetEntity.getZ());
        }
    }
    private static void steer(Entity entity, ServerTroop troop, double x,
            double z)
    {
        double dx = x - entity.getX();
        double dz = z - entity.getZ();
        double horizontal = Math.sqrt(dx * dx + dz * dz);
        if (horizontal < 1.0E-4D) {
            entity.setDeltaMovement(Vec3.ZERO);
            return;
        }
        double speed = troop.moveSpeed();
        entity.setDeltaMovement(
                dx / horizontal * speed, entity.getDeltaMovement().y,
                dz / horizontal * speed);
    }
    private TowerHitboxEntity towerHitbox(TowerId id) {
        UUID uuid = towerHitboxes.get(id);
        if (uuid == null)
            return null;
        Entity entity = arenaLevel.getEntity(uuid);
        return entity instanceof TowerHitboxEntity hitbox ? hitbox : null;
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
        for (ServerTroop troop : troops) {
            Entity entity = arenaLevel.getEntity(troop.entityId());
            if (entity != null)
                entity.discard();
        }
        troops.clear();
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
