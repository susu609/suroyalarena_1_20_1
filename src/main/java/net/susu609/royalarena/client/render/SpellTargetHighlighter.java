package net.susu609.royalarena.client.render;

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
 * While a spell card is selected, outlines (vanilla glowing outline, on this
 * client only) every enemy troop and tower the spell would hit where the
 * cursor points: troops whose body touches the circle, towers whose
 * footprint does, like {@code CombatEngine}; only the strongest few for
 * Lightning, and the own troops a healing potion would heal.
 */
public final class SpellTargetHighlighter {
    private static final Set<Entity> GLOWING = new HashSet<>();

    private SpellTargetHighlighter() {
    }

    /** Once per client tick. */
    public static void tick() {
        Set<Entity> next = new HashSet<>();
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null && ClientMatchState.active() &&
                ClientMatchState.state() == MatchState.BATTLE) {
            int slot = ClientMatchState.selectedSlot();
            BattleCard card = slot >= 0 && slot < ClientMatchState.hand().size()
                    ? ClientMatchState.hand().get(slot) : null;
            BlockPos target = card != null && card.isSpell() ?
                    ArenaCameraController.projectCursorToArena() : null;
            if (target != null && ClientMatchState.canDeployAt(slot,
                    target.getX(), target.getZ()))
                collect(mc, card.spell(), target.getX() + 0.5D,
                        target.getZ() + 0.5D, next);
        }
        for (Entity entity : GLOWING) {
            if (!next.contains(entity))
                entity.setGlowingTag(false);
        }
        for (Entity entity : next)
            entity.setGlowingTag(true);
        GLOWING.clear();
        GLOWING.addAll(next);
    }

    public static void clear() {
        for (Entity entity : GLOWING)
            entity.setGlowingTag(false);
        GLOWING.clear();
    }

    private static void collect(Minecraft mc, SpellStats spell, double x,
            double z, Set<Entity> out)
    {
        ArenaLayout arena = ClientMatchState.arena();
        // A healing potion lights up the own troops it would heal.
        TeamSide target = spell.heal() ? ClientMatchState.side() :
                ClientMatchState.side().opponent();
        List<Entity> hits = new ArrayList<>();
        List<Double> health = new ArrayList<>();
        for (Entity entity : mc.level.entitiesForRendering()) {
            if (entity instanceof CrownTowerEntity ||
                    entity instanceof KingTowerEntity) {
                TowerId id = arena.towerAt(entity.getX(), entity.getZ());
                if (!spell.heal() && id != null && id.side() == target &&
                        ClientMatchState.towerHealth(id) > 0 &&
                        arena.distanceToTower(x, z, id) <= spell.radius()) {
                    hits.add(entity);
                    health.add((double) ClientMatchState.towerHealth(id));
                }
                continue;
            }
            if (!(entity instanceof LivingEntity living) || !entity.isAlive() ||
                    TroopVisuals.sideOf(entity) != target)
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
