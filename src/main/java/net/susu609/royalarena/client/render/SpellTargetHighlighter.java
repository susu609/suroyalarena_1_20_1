package net.susu609.royalarena.client.render;

import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.susu609.royalarena.arena.ArenaLayout;
import net.susu609.royalarena.card.BattleCard;
import net.susu609.royalarena.card.SpellStats;
import net.susu609.royalarena.client.camera.ArenaCameraController;
import net.susu609.royalarena.client.state.ClientMatchState;
import net.susu609.royalarena.entity.building.CrownTowerEntity;
import net.susu609.royalarena.entity.building.KingTowerEntity;
import net.susu609.royalarena.match.MatchState;
import net.susu609.royalarena.match.TeamSide;
import net.susu609.royalarena.tower.TowerId;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * While a spell card is selected, lights up white (the whole model, pulsing,
 * on this client only) every enemy troop and tower the spell would hit where
 * the cursor points: troops whose body touches the circle, towers whose
 * footprint does, like {@code CombatEngine}; only the strongest few for
 * Lightning, and the own troops a healing potion would heal. Troops are lit
 * through {@code LivingEntityRendererMixin}, towers through
 * {@link WorldHealthBar#towerOverlay}.
 */
public final class SpellTargetHighlighter {
    /** Whiteness the pulse swings between (vanilla overlay: 0 none, 1 full). */
    private static final float MIN_WHITE = 0.25F;
    private static final float MAX_WHITE = 0.7F;
    private static final long PULSE_MILLIS = 800L;

    private static final Set<Entity> LIT = new HashSet<>();

    private SpellTargetHighlighter() {
    }

    /** Once per client tick. */
    public static void tick() {
        Set<Entity> next = new HashSet<>();
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null && ClientMatchState.active() &&
                ClientMatchState.state() == MatchState.BATTLE) {
            BattleCard card = ClientMatchState.selectedCard();
            BlockPos target = card != null && card.isSpell() ?
                    ArenaCameraController.projectCursorToArena() : null;
            if (target != null && ClientMatchState.canDeploySelected(
                    target.getX(), target.getZ()))
                collect(mc, card.spell(), target.getX() + 0.5D,
                        target.getZ() + 0.5D, next);
        }
        LIT.clear();
        LIT.addAll(next);
    }

    public static void clear() {
        LIT.clear();
    }

    /**
     * How white to draw the entity now (0 when the selected spell would not
     * hit it), pulsing between {@link #MIN_WHITE} and {@link #MAX_WHITE}.
     */
    public static float whiteness(Entity entity) {
        if (LIT.isEmpty() || !LIT.contains(entity))
            return 0.0F;
        double phase = (Util.getMillis() % PULSE_MILLIS) /
                (double) PULSE_MILLIS * Math.PI * 2.0D;
        return MIN_WHITE + (MAX_WHITE - MIN_WHITE) *
                (float) (0.5D + 0.5D * Math.sin(phase));
    }

    private static void collect(Minecraft mc, SpellStats spell, double x,
            double z, Set<Entity> out)
    {
        ArenaLayout arena = ClientMatchState.arena();
        // Healing and Swiftness light up the own troops they would reach.
        TeamSide caster = ClientMatchState.deploySide();
        TeamSide target = spell.friendly() ? caster : caster.opponent();
        List<Entity> hits = new ArrayList<>();
        List<Double> health = new ArrayList<>();
        for (Entity entity : mc.level.entitiesForRendering()) {
            if (entity instanceof CrownTowerEntity ||
                    entity instanceof KingTowerEntity) {
                TowerId id = arena.towerAt(entity.getX(), entity.getZ());
                // Milk washes every tower it touches, both sides.
                boolean hitsTower = spell.cleanse() || !spell.friendly() &&
                        !spell.poison() && spell.towerMultiplier() > 0.0D &&
                        id != null && id.side() == target;
                if (hitsTower && id != null &&
                        ClientMatchState.towerHealth(id) > 0 &&
                        arena.distanceToTower(x, z, id) <= spell.radius()) {
                    hits.add(entity);
                    health.add((double) ClientMatchState.towerHealth(id));
                }
                continue;
            }
            TeamSide of = TroopVisuals.sideOf(entity);
            if (!(entity instanceof LivingEntity living) || !entity.isAlive() ||
                    of == null || !spell.cleanse() && of != target)
                continue;
            double reach = spell.radius() +
                    entity.getBbWidth() * TroopVisuals.scale(entity) / 2.0D;
            if (Math.hypot(entity.getX() - x, entity.getZ() - z) <= reach) {
                hits.add(entity);
                health.add((double) living.getHealth());
            }
        }
        if (spell.maxTargets() <= 0 || hits.size() <= spell.maxTargets()) {
            out.addAll(hits);
            return;
        }
        // Lightning: only the ones with the most health, like the engine.
        List<Integer> order = new ArrayList<>();
        for (int i = 0; i < hits.size(); i++)
            order.add(i);
        order.sort((a, b) -> Double.compare(health.get(b), health.get(a)));
        for (int i = 0; i < spell.maxTargets(); i++)
            out.add(hits.get(order.get(i)));
    }
}
