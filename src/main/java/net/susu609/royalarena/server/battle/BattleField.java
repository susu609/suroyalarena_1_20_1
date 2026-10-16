package net.susu609.royalarena.server.battle;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Slime;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;
import net.susu609.modlib.resource.RL;
import net.susu609.royalarena.RoyalArena;
import net.susu609.royalarena.arena.ArenaLayout;
import net.susu609.royalarena.arena.ArenaPoint;
import net.susu609.royalarena.arena.ArenaWaypoint;
import net.susu609.royalarena.card.BattleCard;
import net.susu609.royalarena.card.TroopStats;
import net.susu609.royalarena.combat.ChildSpawn;
import net.susu609.royalarena.combat.CombatEngine;
import net.susu609.royalarena.combat.CombatUnit;
import net.susu609.royalarena.combat.Formation;
import net.susu609.royalarena.combat.TickResult;
import net.susu609.royalarena.match.MatchInstance;
import net.susu609.royalarena.match.PendingDeploy;
import net.susu609.royalarena.match.TeamSide;
import net.susu609.royalarena.tower.TowerId;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Bridges the pure {@link CombatEngine} and the world: spawns the mobs of a
 * deployed card, copies their positions into the engine before every tick,
 * then walks them, plays the attacks, mirrors health onto the mobs and
 * removes the dead. Mobs have no vanilla AI; all behaviour comes from the
 * engine.
 */
public final class BattleField {
    private static final double LINE_STEP = 0.6D;

    private final ServerLevel level;
    private final MatchInstance match;
    private final ArenaLayout arena;
    private final CombatEngine engine;
    private final Consumer<TowerId> onTowerDestroyed;

    public BattleField(ServerLevel level, MatchInstance match,
            Consumer<TowerId> onTowerDestroyed)
    {
        this.level = level;
        this.match = match;
        this.arena = match.arena();
        this.engine = new CombatEngine(match);
        this.onTowerDestroyed = onTowerDestroyed;
    }

    public CombatEngine engine() {
        return engine;
    }

    // --------------------------------------------------------------- spawn

    /** Creates the mobs of a card whose deploy time ran out. */
    public void deploy(PendingDeploy deploy) {
        BattleCard card = deploy.card();
        TroopStats stats = card.stats();
        double spacing = Math.max(0.8D, stats.radius() * 2.0D + 0.3D);
        double[][] offsets = Formation.offsets(stats.count(), spacing);
        int sign = ArenaLayout.sideSign(deploy.side());
        for (double[] offset : offsets) {
            ArenaWaypoint spot = arena.clampSpawn(
                    deploy.x() + 0.5D + offset[0],
                    deploy.z() + 0.5D + offset[1], sign);
            Entity entity = createMob(card, deploy.side(), 0, stats.health(),
                    spot.x(), spot.z());
            if (entity != null) {
                engine.add(entity.getUUID(), deploy.side(), stats, spot.x(),
                        spot.z());
            }
        }
    }

    private void spawnChild(ChildSpawn child) {
        BattleCard card = cardOf(child.stats());
        if (card == null)
            return;
        Entity entity = createMob(card, child.side(), child.generation(),
                child.health(), child.x(), child.z());
        if (entity != null)
            engine.add(entity.getUUID(), child);
    }

    private static BattleCard cardOf(TroopStats stats) {
        for (BattleCard card : BattleCard.values()) {
            if (card.stats() == stats)
                return card;
        }
        return null;
    }

    private Entity createMob(BattleCard card, TeamSide side, int generation,
            double health, double x, double z)
    {
        EntityType<?> type = ForgeRegistries.ENTITY_TYPES.getValue(
                RL.require(card.entityId()));
        Entity entity = type == null ? null : type.create(level);
        if (entity == null) {
            RoyalArena.LOGGER.warn("Card {} could not create entity {}",
                    card.id(), card.entityId());
            return null;
        }
        entity.moveTo(x, arena.floorY() + 1.0D, z,
                side == TeamSide.BLUE ? 180.0F : 0.0F, 0.0F);
        entity.setCustomName(Component.literal(card.displayName()).withStyle(
                side == TeamSide.BLUE ? ChatFormatting.BLUE :
                        ChatFormatting.RED));
        entity.addTag("royalarena_troop");
        entity.addTag("royalarena_team_" + side.name().toLowerCase());
        entity.setInvulnerable(true);
        if (entity instanceof Slime slime && card.mobSize() > 0)
            slime.setSize(Math.max(1, card.mobSize() >> generation), false);
        if (entity instanceof Mob mob) {
            mob.setNoAi(true);
            mob.setPersistenceRequired();
            equip(mob, card);
        }
        if (entity instanceof LivingEntity living) {
            if (living.getAttribute(Attributes.MAX_HEALTH) != null)
                living.getAttribute(Attributes.MAX_HEALTH)
                        .setBaseValue(health);
            living.setHealth((float) health);
        }
        if (!level.addFreshEntity(entity)) {
            RoyalArena.LOGGER.warn("Could not add {} for card {}",
                    card.entityId(), card.id());
            return null;
        }
        return entity;
    }

    private static void equip(Mob mob, BattleCard card) {
        if (card.heldItemId() == null)
            return;
        Item item = ForgeRegistries.ITEMS.getValue(
                RL.require(card.heldItemId()));
        if (item == null)
            return;
        mob.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(item));
        mob.setDropChance(EquipmentSlot.MAINHAND, 0.0F);
    }

    // ---------------------------------------------------------------- tick

    public void tick() {
        for (CombatUnit unit : new ArrayList<>(engine.units())) {
            Entity entity = level.getEntity(unit.id());
            if (entity == null || !entity.isAlive()) {
                engine.remove(unit.id());
                continue;
            }
            unit.setPosition(entity.getX(), entity.getZ());
        }

        TickResult result = engine.tick();
        Set<UUID> walking = applyMoves(result.moves());
        stopOthers(walking);
        for (TickResult.Attack attack : result.attacks())
            playAttack(attack);
        mirrorHealth();
        for (TickResult.Death death : result.deaths())
            removeDead(death);
        for (TowerId id : result.destroyedTowers())
            onTowerDestroyed.accept(id);
    }

    /** Zeroes all troop movement, used when the match ends. */
    public void freeze() {
        stopOthers(Set.of());
    }

    /** Discards every troop mob. */
    public void clear() {
        for (CombatUnit unit : new ArrayList<>(engine.units())) {
            Entity entity = level.getEntity(unit.id());
            if (entity != null)
                entity.discard();
            engine.remove(unit.id());
        }
    }

    private Set<UUID> applyMoves(List<TickResult.Move> moves) {
        Set<UUID> walking = new HashSet<>();
        for (TickResult.Move move : moves) {
            Entity entity = level.getEntity(move.unit());
            if (entity == null)
                continue;
            double dx = move.x() - entity.getX();
            double dz = move.z() - entity.getZ();
            double distance = Math.hypot(dx, dz);
            if (distance < 1.0E-4D)
                continue;
            double stride = Math.min(move.speed(), distance);
            entity.setDeltaMovement(dx / distance * stride,
                    entity.getDeltaMovement().y, dz / distance * stride);
            face(entity, dx, dz);
            walking.add(move.unit());
        }
        return walking;
    }

    private void stopOthers(Set<UUID> walking) {
        for (CombatUnit unit : engine.units()) {
            if (walking.contains(unit.id()))
                continue;
            Entity entity = level.getEntity(unit.id());
            if (entity != null) {
                entity.setDeltaMovement(
                        0.0D, entity.getDeltaMovement().y, 0.0D);
            }
        }
    }

    private static void face(Entity entity, double dx, double dz) {
        float yaw = (float) (Mth.atan2(dz, dx) * (180.0D / Math.PI)) - 90.0F;
        entity.setYRot(yaw);
        if (entity instanceof LivingEntity living) {
            living.setYHeadRot(yaw);
            living.setYBodyRot(yaw);
        }
    }

    // ------------------------------------------------------------- effects

    private void playAttack(TickResult.Attack attack) {
        double[] from = attack.attackerUnit() != null ?
                unitPoint(attack.attackerUnit()) :
                towerPoint(attack.attackerTower(), 3.5D);
        double[] to = attack.victimUnit() != null ?
                unitPoint(attack.victimUnit()) :
                towerPoint(attack.victimTower(), 2.0D);
        if (from == null || to == null)
            return;

        if (attack.attackerUnit() != null) {
            Entity attacker = level.getEntity(attack.attackerUnit());
            if (attacker != null) {
                face(attacker, to[0] - from[0], to[2] - from[2]);
                if (attacker instanceof LivingEntity living)
                    living.swing(InteractionHand.MAIN_HAND);
            }
        }
        if (attack.victimUnit() != null) {
            Entity victim = level.getEntity(attack.victimUnit());
            if (victim != null)
                level.broadcastEntityEvent(victim, (byte) 2);
        }

        if (attack.ranged()) {
            drawLine(from, to);
            level.playSound(null, from[0], from[1], from[2],
                    attack.attackerTower() != null ?
                            SoundEvents.CROSSBOW_SHOOT :
                            SoundEvents.ARROW_SHOOT, SoundSource.HOSTILE,
                    0.6F, 1.0F);
        } else {
            level.playSound(null, to[0], to[1], to[2],
                    SoundEvents.PLAYER_ATTACK_STRONG, SoundSource.HOSTILE,
                    0.6F, 1.0F);
        }
        level.sendParticles(ParticleTypes.CRIT, to[0], to[1], to[2], 3, 0.2D,
                0.2D, 0.2D, 0.1D);
    }

    private void drawLine(double[] from, double[] to) {
        double dx = to[0] - from[0];
        double dy = to[1] - from[1];
        double dz = to[2] - from[2];
        double length = Math.sqrt(dx * dx + dy * dy + dz * dz);
        int steps = Math.max(1, (int) (length / LINE_STEP));
        for (int i = 1; i < steps; i++) {
            double t = i / (double) steps;
            level.sendParticles(ParticleTypes.CRIT, from[0] + dx * t,
                    from[1] + dy * t, from[2] + dz * t, 1, 0.0D, 0.0D, 0.0D,
                    0.0D);
        }
    }

    private double[] unitPoint(UUID id) {
        Entity entity = level.getEntity(id);
        if (entity == null)
            return null;
        return new double[] {entity.getX(),
                entity.getY() + entity.getBbHeight() * 0.7D, entity.getZ()};
    }

    private double[] towerPoint(TowerId id, double height) {
        ArenaPoint p = arena.towerPoint(id);
        return new double[] {p.x(), p.y() + height, p.z()};
    }

    /** Copies engine health onto the mobs so health bars follow it. */
    private void mirrorHealth() {
        for (CombatUnit unit : engine.units()) {
            Entity entity = level.getEntity(unit.id());
            if (entity instanceof LivingEntity living &&
                    living.getHealth() != (float) unit.health())
                living.setHealth((float) unit.health());
        }
    }

    /**
     * Removes a dead unit's mob and spawns its split children. The mob is
     * discarded while its vanilla health is still above zero, so vanilla
     * slime splitting does not kick in on top of the card's own rule.
     */
    private void removeDead(TickResult.Death death) {
        Entity entity = level.getEntity(death.unit());
        if (entity != null) {
            level.sendParticles(ParticleTypes.POOF, entity.getX(),
                    entity.getY() + entity.getBbHeight() * 0.5D,
                    entity.getZ(), 8, 0.2D, 0.2D, 0.2D, 0.02D);
            entity.discard();
        }
        for (ChildSpawn child : death.children())
            spawnChild(child);
    }
}
