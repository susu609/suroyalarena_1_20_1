package net.susu609.royalarena.server.battle;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.monster.AbstractIllager;
import net.minecraft.world.entity.monster.AbstractSkeleton;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.MagmaCube;
import net.minecraft.world.entity.monster.Pillager;
import net.minecraft.world.entity.monster.Ravager;
import net.minecraft.world.entity.monster.Slime;
import net.minecraft.world.entity.monster.Spider;
import net.minecraft.world.entity.monster.Witch;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.monster.hoglin.Hoglin;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
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
import net.susu609.royalarena.combat.Separation;
import net.susu609.royalarena.combat.TickResult;
import net.susu609.royalarena.config.ArenaCommonConfig;
import net.susu609.royalarena.match.MatchInstance;
import net.susu609.royalarena.match.PendingDeploy;
import net.susu609.royalarena.match.TeamSide;
import net.susu609.royalarena.network.ModNetwork;
import net.susu609.royalarena.network.s2c.S2CAoeFlashPacket;
import net.susu609.royalarena.network.s2c.S2CBurnPacket;
import net.susu609.royalarena.network.s2c.S2CProjectilePacket;
import net.susu609.royalarena.network.s2c.S2CShakePacket;
import net.susu609.royalarena.tower.TowerId;
import net.susu609.royalarena.tower.TowerKind;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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
    /** Players this close to a shot see its arrow. */
    private static final double PROJECTILE_VIEW_RANGE = 96.0D;
    /** Arrows drawn per Arrow Storm volley (one volley per pulse). */
    private static final int VOLLEY_ARROWS = 5;
    /** Fewest ticks between two red hurt flashes of one troop. */
    private static final int FLASH_GAP = 6;

    private final ServerLevel level;
    private final MatchInstance match;
    private final ArenaLayout arena;
    private final CombatEngine engine;
    private final Consumer<TowerId> onTowerDestroyed;
    /** Ticks each troop has wanted to walk without moving (debug log). */
    private final Map<UUID, Integer> stuckTicks = new HashMap<>();
    private final TroopAnimator animator = new TroopAnimator();
    /** Engine health each unit had when it last flashed red. */
    private final Map<UUID, Double> flashedHealth = new HashMap<>();
    /** Game tick each unit last flashed red. */
    private final Map<UUID, Long> flashedAt = new HashMap<>();
    /** Burn ticks last sent to the clients, per unit. */
    private final Map<UUID, Integer> burnShown = new HashMap<>();
    /** Pending spells whose flight has already been shown. */
    private final Set<String> launched = new HashSet<>();

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

    /**
     * Creates the mobs of a card whose deploy time ran out, or lands a spell
     * whose flight time ran out.
     */
    public void deploy(PendingDeploy deploy) {
        BattleCard card = deploy.card();
        if (card.isSpell()) {
            engine.castSpell(deploy.side(), card, deploy.x() + 0.5D,
                    deploy.z() + 0.5D);
            return;
        }
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
        engine.setSpeedMultiplier(ArenaCommonConfig.SPEED_MULTIPLIER.get());
        for (CombatUnit unit : new ArrayList<>(engine.units())) {
            Entity entity = level.getEntity(unit.id());
            if (entity == null || !entity.isAlive()) {
                engine.remove(unit.id());
                animator.forget(unit.id());
                burnShown.remove(unit.id());
                flashedHealth.remove(unit.id());
                flashedAt.remove(unit.id());
                continue;
            }
            unit.setPosition(entity.getX(), entity.getZ());
        }

        TickResult result = engine.tick();
        for (TickResult.Push push : result.pushes())
            knockBack(push);
        moveUnits(result.moves());
        for (TickResult.SpellPulse pulse : result.spellPulses())
            playSpell(pulse);
        for (TickResult.Attack attack : result.attacks())
            playAttack(attack);
        for (TickResult.Attack attack : result.landed())
            playHit(attack);
        mirrorHealth();
        showFrozenTowers();
        for (TickResult.Death death : result.deaths())
            removeDead(death);
        for (TowerId id : result.destroyedTowers())
            onTowerDestroyed.accept(id);
    }

    /** Discards every troop mob. */
    public void clear() {
        for (CombatUnit unit : new ArrayList<>(engine.units())) {
            Entity entity = level.getEntity(unit.id());
            if (entity != null)
                entity.discard();
            engine.remove(unit.id());
            stuckTicks.remove(unit.id());
            animator.forget(unit.id());
            burnShown.remove(unit.id());
            flashedHealth.remove(unit.id());
            flashedAt.remove(unit.id());
        }
    }

    /**
     * Moves every troop mob one step. Mobs are NoAI, and a NoAI mob does not
     * run its travel/physics step at all, so setting a velocity does nothing:
     * the mob has to be moved with Entity.move, which also does the block
     * collisions. Gravity is applied here for the same reason.
     */
    private void moveUnits(List<TickResult.Move> moves) {
        Map<UUID, double[]> walk = new HashMap<>();
        Map<UUID, TickResult.Move> moveOf = new HashMap<>();
        for (TickResult.Move move : moves) {
            CombatUnit unit = engine.unit(move.unit());
            Entity entity = unit == null ? null : level.getEntity(move.unit());
            if (entity == null)
                continue;
            double dx = move.x() - unit.x();
            double dz = move.z() - unit.z();
            double distance = Math.hypot(dx, dz);
            if (distance < 1.0E-4D)
                continue;
            double stride = Math.min(move.speed(), distance);
            walk.put(unit.id(),
                    new double[] {dx / distance * stride, dz / distance * stride});
            moveOf.put(unit.id(), move);
            face(entity, dx, dz);
        }
        boolean showRoutes = ArenaCommonConfig.SHOW_ROUTES.get();
        boolean logMovement = ArenaCommonConfig.LOG_MOVEMENT.get();
        List<CombatUnit> units = engine.units();
        for (CombatUnit unit : units) {
            Entity entity = level.getEntity(unit.id());
            if (entity == null)
                continue;
            double[] own = walk.getOrDefault(unit.id(), new double[2]);
            if (entity instanceof Slime slime) {
                double factor = animator.slimeStride(slime,
                        walk.containsKey(unit.id()));
                own = new double[] {own[0] * factor, own[1] * factor};
            }
            double[] push = Separation.offset(unit, units, arena);
            double beforeX = entity.getX();
            double beforeZ = entity.getZ();
            step(entity, own[0] + push[0], own[1] + push[1],
                    animator.takeLift(unit.id()));
            boolean walking = walk.containsKey(unit.id());
            animator.updatePose(entity, unit.target() != null, walking);
            if (!walking)
                faceTarget(entity, unit);
            TickResult.Move move = moveOf.get(unit.id());
            if (showRoutes && move != null && level.getGameTime() % 5 == 0)
                drawRoute(entity, move);
            if (logMovement)
                logMovement(unit, entity, move, Math.hypot(own[0], own[1]),
                        Math.hypot(entity.getX() - beforeX,
                                entity.getZ() - beforeZ));
        }
    }

    /** Particle trail from a troop to the point it is walking to. */
    private void drawRoute(Entity entity, TickResult.Move move) {
        double y = entity.getY() + 0.3D;
        double dx = move.x() - entity.getX();
        double dz = move.z() - entity.getZ();
        double length = Math.min(8.0D, Math.hypot(dx, dz));
        double total = Math.max(1.0E-6D, Math.hypot(dx, dz));
        for (double t = 0.0D; t <= length; t += 0.5D) {
            level.sendParticles(ParticleTypes.END_ROD,
                    entity.getX() + dx / total * t, y,
                    entity.getZ() + dz / total * t, 1, 0.0D, 0.0D, 0.0D, 0.0D);
        }
        level.sendParticles(ParticleTypes.HAPPY_VILLAGER, move.x(), y + 0.5D,
                move.z(), 3, 0.1D, 0.1D, 0.1D, 0.0D);
    }

    private void logMovement(CombatUnit unit, Entity entity,
            TickResult.Move move, double wanted, double moved)
    {
        int stuck = wanted > 0.005D && moved < 0.001D ?
                stuckTicks.getOrDefault(unit.id(), 0) + 1 : 0;
        stuckTicks.put(unit.id(), stuck);
        String id = unit.id().toString().substring(0, 8);
        if (stuck == 40) {
            RoyalArena.LOGGER.warn(
                    "[royalarena] troop {} {} at ({}, {}, {}) has wanted to " +
                            "walk for 2 s but has not moved (onGround={})",
                    id, entity.getType().getDescriptionId(),
                    fmt(entity.getX()), fmt(entity.getY()), fmt(entity.getZ()),
                    entity.onGround());
        }
        if ((level.getGameTime() + unit.id().hashCode()) % 20 != 0)
            return;
        RoyalArena.LOGGER.info(
                "[royalarena] troop {} {} at ({}, {}, {}) -> {} wanted {} " +
                        "moved {} onGround={} hp={}",
                id, entity.getType().getDescriptionId(), fmt(entity.getX()),
                fmt(entity.getY()), fmt(entity.getZ()),
                move == null ? "stays (in range or no target)" :
                        "(" + fmt(move.x()) + ", " + fmt(move.z()) + ")",
                fmt(wanted), fmt(moved), entity.onGround(),
                fmt(unit.health()));
    }

    private static String fmt(double value) {
        return String.format(java.util.Locale.ROOT, "%.2f", value);
    }

    /** Turns a troop that stands fighting toward what it fights. */
    private void faceTarget(Entity entity, CombatUnit unit) {
        double tx;
        double tz;
        Entity enemy = unit.target() == null ? null :
                level.getEntity(unit.target());
        if (enemy != null) {
            tx = enemy.getX();
            tz = enemy.getZ();
        } else if (unit.lockedTower() != null) {
            ArenaPoint p = arena.towerPoint(unit.lockedTower());
            tx = p.x();
            tz = p.z();
        } else {
            return;
        }
        double dx = tx - entity.getX();
        double dz = tz - entity.getZ();
        if (dx * dx + dz * dz > 1.0E-4D)
            face(entity, dx, dz);
    }

    /**
     * One movement step with gravity; {@code lift} > 0 starts a jump (slime
     * hops, tosses, pounces).
     */
    private static void step(Entity entity, double dx, double dz,
            double lift)
    {
        double fall = lift > 0.0D ? lift : entity.onGround() ? -0.1D :
                Math.max(entity.getDeltaMovement().y - 0.08D, -1.5D);
        entity.move(MoverType.SELF, new Vec3(dx, fall, dz));
        entity.setDeltaMovement(0.0D, entity.onGround() ? 0.0D : fall, 0.0D);
    }

    private static void face(Entity entity, double dx, double dz) {
        float yaw = (float) (Mth.atan2(dz, dx) * (180.0D / Math.PI)) - 90.0F;
        entity.setYRot(yaw);
        if (entity instanceof LivingEntity living) {
            living.setYHeadRot(yaw);
            living.setYBodyRot(yaw);
        }
    }

    /** Moves a mob the way the engine knocked its unit back. */
    private void knockBack(TickResult.Push push) {
        Entity entity = level.getEntity(push.unit());
        if (entity != null)
            entity.move(MoverType.SELF, new Vec3(push.dx(), 0.0D, push.dz()));
    }

    // -------------------------------------------------------------- spells

    /**
     * Shows spells still in flight: the fireball arcs from the caster's King
     * tower to its target; arrows mark the circle they are about to hit.
     */
    public void showFlights(List<PendingDeploy> pending) {
        Set<String> current = new HashSet<>();
        for (PendingDeploy deploy : pending) {
            if (!deploy.card().isSpell())
                continue;
            String key = deploy.side() + ":" + deploy.card() + ":" +
                    deploy.x() + ":" + deploy.z();
            current.add(key);
            if (!launched.add(key))
                continue;
            // First tick in the air: launch what lands when the spell does.
            double tx = deploy.x() + 0.5D;
            double tz = deploy.z() + 0.5D;
            double floor = arena.floorY() + 1.0D;
            ArenaPoint king = arena.towerPoint(TowerId.king(deploy.side()));
            double[] hand = {king.x(), king.y() + TowerKind.KING.shotHeight(),
                    king.z()};
            switch (deploy.card()) {
                case FIREBALL -> {
                    sendProjectile(S2CProjectilePacket.Kind.FIREBALL, hand,
                            new double[] {tx, floor + 0.5D, tz},
                            deploy.ticksLeft(), 3.0F);
                    level.playSound(null, hand[0], hand[1], hand[2],
                            SoundEvents.BLAZE_SHOOT, SoundSource.HOSTILE, 0.8F,
                            1.0F);
                }
                case HEALING_POTION, FREEZE -> {
                    // The King throws a splash potion or a snowball.
                    sendProjectile(deploy.card() == BattleCard.FREEZE ?
                                    S2CProjectilePacket.Kind.SNOWBALL :
                                    S2CProjectilePacket.Kind.HEALING_POTION,
                            hand, new double[] {tx, floor + 0.3D, tz},
                            deploy.ticksLeft(), 4.0F);
                    level.playSound(null, hand[0], hand[1], hand[2],
                            deploy.card() == BattleCard.FREEZE ?
                                    SoundEvents.SNOWBALL_THROW :
                                    SoundEvents.SPLASH_POTION_THROW,
                            SoundSource.HOSTILE, 0.8F, 1.0F);
                }
                case TNT -> {
                    // Dropped from high above, primed and flashing.
                    sendProjectile(S2CProjectilePacket.Kind.TNT,
                            new double[] {tx, floor + 16.0D, tz},
                            new double[] {tx, floor, tz}, deploy.ticksLeft(),
                            0.0F);
                    level.playSound(null, tx, floor + 8.0D, tz,
                            SoundEvents.TNT_PRIMED, SoundSource.HOSTILE, 1.0F,
                            1.0F);
                }
                case LIGHTNING -> level.sendParticles(
                        ParticleTypes.ELECTRIC_SPARK, tx, floor + 0.3D, tz, 30,
                        deploy.card().spell().radius() * 0.5D, 0.1D,
                        deploy.card().spell().radius() * 0.5D, 0.05D);
                default -> arrowVolley(deploy.side(), deploy.card(), tx, tz,
                        deploy.ticksLeft());
            }
        }
        launched.retainAll(current);
    }

    /**
     * Arrow Storm arrows coming down on random points of the circle from the
     * caster's side, landing in {@code ticks}.
     */
    private void arrowVolley(TeamSide side, BattleCard card, double x,
            double z, int ticks)
    {
        double floor = arena.floorY() + 1.0D;
        double radius = card.spell().radius();
        int sign = ArenaLayout.sideSign(side);
        for (int i = 0; i < VOLLEY_ARROWS; i++) {
            double angle = level.random.nextDouble() * Math.PI * 2.0D;
            double r = Math.sqrt(level.random.nextDouble()) * radius;
            double tx = x + Math.cos(angle) * r;
            double tz = z + Math.sin(angle) * r;
            double[] from = {tx + (level.random.nextDouble() - 0.5D) * 2.0D,
                    floor + 9.0D, tz + sign * 5.0D};
            sendProjectile(S2CProjectilePacket.Kind.ARROW, from,
                    new double[] {tx, floor + 0.1D, tz}, ticks, 1.0F);
        }
        level.playSound(null, x, floor + 6.0D, z, SoundEvents.ARROW_SHOOT,
                SoundSource.HOSTILE, 0.5F, 0.8F);
    }

    private void playSpell(TickResult.SpellPulse pulse) {
        double floor = arena.floorY() + 1.0D;
        var spell = pulse.card().spell();
        double radius = spell.radius();
        double x = pulse.x();
        double z = pulse.z();
        switch (pulse.card()) {
            case FIREBALL -> {
                flash(x, z, radius, S2CAoeFlashPacket.RED);
                level.sendParticles(ParticleTypes.EXPLOSION, x, floor + 0.5D,
                        z, 4, radius * 0.4D, 0.3D, radius * 0.4D, 0.0D);
                level.sendParticles(ParticleTypes.FLAME, x, floor + 0.3D, z,
                        40, radius * 0.5D, 0.2D, radius * 0.5D, 0.05D);
                level.playSound(null, x, floor, z, SoundEvents.GENERIC_EXPLODE,
                        SoundSource.HOSTILE, 0.8F, 1.2F);
            }
            case TNT -> {
                flash(x, z, radius, S2CAoeFlashPacket.RED);
                shake(x, z, 0.8F);
                level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, x,
                        floor + 0.5D, z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
                level.sendParticles(ParticleTypes.EXPLOSION, x, floor + 0.5D,
                        z, 10, radius * 0.5D, 0.4D, radius * 0.5D, 0.0D);
                level.sendParticles(ParticleTypes.LARGE_SMOKE, x, floor + 0.5D,
                        z, 30, radius * 0.5D, 0.3D, radius * 0.5D, 0.05D);
                level.playSound(null, x, floor, z, SoundEvents.GENERIC_EXPLODE,
                        SoundSource.HOSTILE, 1.2F, 0.8F);
            }
            case LIGHTNING -> {
                shake(x, z, 0.5F);
                // A real (harmless) bolt on every unit and tower struck.
                for (TickResult.Strike strike : pulse.strikes()) {
                    LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level);
                    if (bolt == null)
                        continue;
                    bolt.moveTo(strike.x(), floor, strike.z());
                    bolt.setVisualOnly(true);
                    level.addFreshEntity(bolt);
                    flash(strike.x(), strike.z(), 1.2D,
                            S2CAoeFlashPacket.YELLOW);
                }
                if (pulse.strikes().isEmpty())
                    flash(x, z, radius, S2CAoeFlashPacket.YELLOW);
            }
            case FREEZE -> {
                flash(x, z, radius, S2CAoeFlashPacket.ICE);
                level.sendParticles(ParticleTypes.SNOWFLAKE, x, floor + 0.5D,
                        z, 60, radius * 0.5D, 0.4D, radius * 0.5D, 0.02D);
                level.sendParticles(ParticleTypes.ITEM_SNOWBALL, x,
                        floor + 0.3D, z, 20, radius * 0.4D, 0.1D, radius * 0.4D,
                        0.1D);
                level.playSound(null, x, floor, z, SoundEvents.GLASS_BREAK,
                        SoundSource.HOSTILE, 0.8F, 1.4F);
                level.playSound(null, x, floor, z, SoundEvents.POWDER_SNOW_PLACE,
                        SoundSource.HOSTILE, 1.0F, 0.8F);
            }
            case HEALING_POTION -> {
                if (pulse.index() % 2 == 0)
                    flash(x, z, radius, S2CAoeFlashPacket.GREEN);
                if (pulse.index() == 0) {
                    level.sendParticles(ParticleTypes.EFFECT, x, floor + 0.3D,
                            z, 40, radius * 0.5D, 0.2D, radius * 0.5D, 0.1D);
                    level.playSound(null, x, floor, z,
                            SoundEvents.SPLASH_POTION_BREAK,
                            SoundSource.HOSTILE, 0.8F, 1.0F);
                }
                for (TickResult.Strike strike : pulse.strikes()) {
                    level.sendParticles(ParticleTypes.HEART, strike.x(),
                            floor + 1.6D, strike.z(), 1, 0.2D, 0.1D, 0.2D,
                            0.0D);
                }
            }
            default -> {
                // Arrow Storm: this pulse's arrows land now; launch the next
                // volley so it lands with the next pulse.
                flash(x, z, radius, S2CAoeFlashPacket.RED);
                if (pulse.index() + 1 < spell.pulses())
                    arrowVolley(pulse.side(), pulse.card(), x, z,
                            spell.pulseInterval());
                level.sendParticles(ParticleTypes.CRIT, x, floor + 0.2D, z, 6,
                        radius * 0.5D, 0.05D, radius * 0.5D, 0.05D);
                if (pulse.index() % 2 == 0)
                    level.playSound(null, x, floor, z, SoundEvents.ARROW_HIT,
                            SoundSource.HOSTILE, 0.5F,
                            0.9F + level.random.nextFloat() * 0.3F);
            }
        }
    }

    // ------------------------------------------------------------- effects

    /** A blow struck or a shot fired: turn, swing, and launch the arrow. */
    private void playAttack(TickResult.Attack attack) {
        double[] from = attack.attackerUnit() != null ?
                unitPoint(attack.attackerUnit()) :
                towerPoint(attack.attackerTower(),
                        attack.attackerTower().kind().shotHeight());
        double[] to = target(attack);
        if (from == null || to == null)
            return;
        if (attack.attackerUnit() != null) {
            Entity attacker = level.getEntity(attack.attackerUnit());
            if (attacker != null) {
                face(attacker, to[0] - from[0], to[2] - from[2]);
                animator.onAttack(attacker);
            }
        }
        Entity attacker = attack.attackerUnit() == null ? null :
                level.getEntity(attack.attackerUnit());
        if (attacker instanceof Creeper) {
            explode(from, attack.splash());
            return;
        }
        if (!attack.ranged())
            return;
        double distance = Math.hypot(to[0] - from[0], to[2] - from[2]);
        if (attacker instanceof Witch) {
            sendProjectile(S2CProjectilePacket.Kind.POTION, from, to,
                    attack.flightTicks(), (float) (distance * 0.3D));
            level.playSound(null, from[0], from[1], from[2],
                    SoundEvents.WITCH_THROW, SoundSource.HOSTILE, 0.6F, 1.0F);
            return;
        }
        sendProjectile(S2CProjectilePacket.Kind.ARROW, from, to,
                attack.flightTicks(), (float) (distance * 0.12D));
        level.playSound(null, from[0], from[1], from[2],
                attack.attackerTower() != null || attacker instanceof Pillager ?
                        SoundEvents.CROSSBOW_SHOOT :
                        SoundEvents.ARROW_SHOOT, SoundSource.HOSTILE, 0.6F,
                1.0F);
    }

    /** Shakes the camera of the players watching (S2CShakePacket). */
    private void shake(double x, double z, float strength) {
        ModNetwork.sendNear(level, x, arena.floorY(), z, PROJECTILE_VIEW_RANGE,
                new S2CShakePacket(strength));
    }

    /** A red circle flashing on the floor where area damage lands. */
    private void flash(double x, double z, double radius) {
        flash(x, z, radius, S2CAoeFlashPacket.RED);
    }

    /** A circle of {@code rgb} flashing on the floor. */
    private void flash(double x, double z, double radius, int rgb) {
        double floor = arena.floorY() + 1.0D;
        ModNetwork.sendNear(level, x, floor, z, PROJECTILE_VIEW_RANGE,
                new S2CAoeFlashPacket((float) x, (float) floor, (float) z,
                        (float) radius, rgb));
    }

    /** A Creeper going off: blast particles over its splash circle. */
    private void explode(double[] at, double radius) {
        flash(at[0], at[2], radius);
        double floor = arena.floorY() + 1.0D;
        level.sendParticles(ParticleTypes.EXPLOSION, at[0], floor + 0.5D,
                at[2], 6, radius * 0.4D, 0.3D, radius * 0.4D, 0.0D);
        level.sendParticles(ParticleTypes.POOF, at[0], floor + 0.3D, at[2], 20,
                radius * 0.5D, 0.2D, radius * 0.5D, 0.05D);
        level.playSound(null, at[0], floor, at[2], SoundEvents.GENERIC_EXPLODE,
                SoundSource.HOSTILE, 0.8F, 1.1F);
    }

    /** Damage applied this tick: a melee blow, or an arrow arriving. */
    private void playHit(TickResult.Attack attack) {
        double[] to = target(attack);
        if (to == null)
            return;
        if (attack.victimUnit() != null) {
            Entity victim = level.getEntity(attack.victimUnit());
            if (attack.attackerUnit() != null && !attack.ranged()) {
                Entity attacker = level.getEntity(attack.attackerUnit());
                animator.onHit(attacker, victim);
                if (attacker instanceof IronGolem || attacker instanceof Ravager)
                    shake(to[0], to[2], 0.25F);
            }
        }
        if (attack.ranged() && attack.splash() > 0.0D) {
            // A splash potion breaking over its circle.
            flash(attack.x(), attack.z(), attack.splash());
            double floor = arena.floorY() + 1.0D;
            level.sendParticles(ParticleTypes.WITCH, attack.x(), floor + 0.3D,
                    attack.z(), 24, attack.splash() * 0.5D, 0.2D,
                    attack.splash() * 0.5D, 0.05D);
            level.playSound(null, attack.x(), floor, attack.z(),
                    SoundEvents.SPLASH_POTION_BREAK, SoundSource.HOSTILE, 0.6F,
                    1.0F);
            return;
        }
        level.playSound(null, to[0], to[1], to[2],
                attack.ranged() ? SoundEvents.ARROW_HIT :
                        SoundEvents.PLAYER_ATTACK_STRONG,
                SoundSource.HOSTILE, 0.6F, 1.0F);
        level.sendParticles(ParticleTypes.CRIT, to[0], to[1], to[2], 3, 0.2D,
                0.2D, 0.2D, 0.1D);
    }

    private double[] target(TickResult.Attack attack) {
        return attack.victimUnit() != null ? unitPoint(attack.victimUnit()) :
                towerPoint(attack.victimTower(), 2.0D);
    }

    private void sendProjectile(S2CProjectilePacket.Kind kind, double[] from,
            double[] to, int ticks, float arc)
    {
        ModNetwork.sendNear(level, (from[0] + to[0]) / 2.0D, from[1],
                (from[2] + to[2]) / 2.0D, PROJECTILE_VIEW_RANGE,
                new S2CProjectilePacket(kind, (float) from[0],
                        (float) from[1], (float) from[2], (float) to[0],
                        (float) to[1], (float) to[2], Math.max(1, ticks),
                        arc));
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

    /**
     * Copies engine health onto the mobs so health bars follow it, and tells
     * the clients when a unit starts burning (they draw embers instead of
     * vanilla fire; the burn damage comes from the engine).
     */
    private void mirrorHealth() {
        for (CombatUnit unit : engine.units()) {
            Entity entity = level.getEntity(unit.id());
            if (entity instanceof LivingEntity living &&
                    living.getHealth() != (float) unit.health())
                living.setHealth((float) unit.health());
            if (entity != null)
                flashIfHurt(unit, entity);
            // Frozen or stunned: vanilla's frozen look, the mob shakes.
            if (entity != null && unit.frozen())
                entity.setTicksFrozen(entity.getTicksRequiredToFreeze());
            int burn = unit.burnTicks();
            int known = burnShown.getOrDefault(unit.id(), 0);
            if (entity != null && burn > known) {
                // Newly set alight or burning again: the clients draw it.
                ModNetwork.sendNear(level, entity.getX(), entity.getY(),
                        entity.getZ(), PROJECTILE_VIEW_RANGE,
                        new S2CBurnPacket(entity.getId(), burn));
            }
            if (burn > 0)
                burnShown.put(unit.id(), burn);
            else
                burnShown.remove(unit.id());
        }
    }

    /**
     * The vanilla hurt look (red tint, flinch, hurt sound) whenever a unit
     * has lost health, whatever hit it: blows, arrows, splash, spells and
     * burning. At most every {@link #FLASH_GAP} ticks, and a burn has to add
     * up to half a heart, so a burning troop flickers instead of glowing.
     */
    private void flashIfHurt(CombatUnit unit, Entity entity) {
        double before = flashedHealth.getOrDefault(unit.id(), unit.maxHealth());
        if (unit.health() > before) {
            // Healed: flash from the new level on.
            flashedHealth.put(unit.id(), unit.health());
            return;
        }
        long now = level.getGameTime();
        if (before - unit.health() < 0.5D ||
                now - flashedAt.getOrDefault(unit.id(), -100L) < FLASH_GAP)
            return;
        flashedHealth.put(unit.id(), unit.health());
        flashedAt.put(unit.id(), now);
        level.broadcastDamageEvent(entity, level.damageSources().generic());
    }

    /** Snowflakes drifting around a frozen tower. */
    private void showFrozenTowers() {
        if (level.getGameTime() % 5 != 0)
            return;
        for (TowerId id : TowerId.values()) {
            if (engine.towerFrozenTicks(id) <= 0 || match.tower(id).isDestroyed())
                continue;
            ArenaPoint p = arena.towerPoint(id);
            level.sendParticles(ParticleTypes.SNOWFLAKE, p.x(), p.y() + 2.5D,
                    p.z(), 12, 1.6D, 1.6D, 1.6D, 0.01D);
        }
    }

    /** What a dead troop breaks into: bones, slime, wool, iron... */
    private static Item deathBits(Entity entity) {
        if (entity instanceof MagmaCube)
            return Items.MAGMA_CREAM;
        if (entity instanceof Slime)
            return Items.SLIME_BALL;
        if (entity instanceof AbstractSkeleton)
            return Items.BONE;
        if (entity instanceof Zombie)
            return Items.ROTTEN_FLESH;
        if (entity instanceof Spider)
            return Items.STRING;
        if (entity instanceof Wolf)
            return Items.WHITE_WOOL;
        if (entity instanceof IronGolem)
            return Items.IRON_INGOT;
        if (entity instanceof Ravager || entity instanceof Hoglin)
            return Items.LEATHER;
        if (entity instanceof Witch)
            return Items.GLASS_BOTTLE;
        if (entity instanceof Creeper)
            return Items.GUNPOWDER;
        if (entity instanceof AbstractIllager)
            return Items.EMERALD;
        return null;
    }

    /**
     * Removes a dead unit's mob and spawns its split children. The mob is
     * discarded while its vanilla health is still above zero, so vanilla
     * slime splitting does not kick in on top of the card's own rule.
     */
    private void removeDead(TickResult.Death death) {
        animator.forget(death.unit());
        burnShown.remove(death.unit());
        flashedHealth.remove(death.unit());
        flashedAt.remove(death.unit());
        Entity entity = level.getEntity(death.unit());
        if (entity != null) {
            double y = entity.getY() + entity.getBbHeight() * 0.5D;
            double spread = entity.getBbWidth() * 0.4D;
            level.sendParticles(ParticleTypes.POOF, entity.getX(), y,
                    entity.getZ(), 8, 0.2D, 0.2D, 0.2D, 0.02D);
            Item bits = deathBits(entity);
            if (bits != null)
                level.sendParticles(new ItemParticleOption(ParticleTypes.ITEM,
                                new ItemStack(bits)), entity.getX(), y,
                        entity.getZ(), 14, spread, 0.25D, spread, 0.12D);
            if (entity instanceof MagmaCube)
                level.sendParticles(ParticleTypes.LAVA, entity.getX(), y,
                        entity.getZ(), 6, spread, 0.2D, spread, 0.0D);
            entity.discard();
        }
        for (ChildSpawn child : death.children())
            spawnChild(child);
    }
}
