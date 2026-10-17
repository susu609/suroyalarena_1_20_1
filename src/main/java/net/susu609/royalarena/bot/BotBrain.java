package net.susu609.royalarena.bot;

import net.susu609.royalarena.arena.ArenaBox;
import net.susu609.royalarena.arena.ArenaLayout;
import net.susu609.royalarena.arena.ArenaPoint;
import net.susu609.royalarena.card.BattleCard;
import net.susu609.royalarena.card.BattleDeck;
import net.susu609.royalarena.combat.CombatEngine;
import net.susu609.royalarena.combat.CombatUnit;
import net.susu609.royalarena.match.MatchInstance;
import net.susu609.royalarena.match.MatchPlayer;
import net.susu609.royalarena.match.MatchState;
import net.susu609.royalarena.match.TeamSide;
import net.susu609.royalarena.tower.TowerId;
import net.susu609.royalarena.tower.TowerState;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * A simple opponent that plays cards through the same {@code tryDeploy} rules
 * as a human, so elixir, the hand cycle, deploy areas and the deploy delay all
 * apply to it. Pure Java: it only reads the match and the combat engine.
 *
 * <p>Every {@link #THINK_EVERY_TICKS} ticks (the defaults; see
 * {@link BotSettings}) it looks at the board:</p>
 * <ul>
 * <li><b>Spell.</b> If a spell in hand would hit enemy units worth at least
 * {@link #SPELL_VALUE_PER_ELIXIR} health per elixir (two units or more), it
 * casts it where it hits most, a little ahead of where they walk.</li>
 * <li><b>Defend.</b> If an enemy unit stands on its bank of the river it
 * plays the sturdiest troop card it can afford a few blocks behind that unit, in
 * the same lane.</li>
 * <li><b>Attack.</b> Otherwise, once it has at least
 * {@link #ATTACK_MIN_TENTHS} tenths of elixir, it plays a random affordable
 * troop card at the back of the lane of the weaker enemy Princess tower.</li>
 * </ul>
 * It never plays twice within {@link #MIN_GAP_TICKS} ticks. The random seed
 * makes a bot reproducible.
 */
public final class BotBrain {
    public static final int THINK_EVERY_TICKS = 10;
    public static final int MIN_GAP_TICKS = 40;
    public static final int ATTACK_MIN_TENTHS = 70;
    private static final int DEFENCE_DEPTH = 3;
    private static final int ATTACK_DEPTH = 4;
    /** Enemy health a spell must take off, per elixir it costs. */
    public static final double SPELL_VALUE_PER_ELIXIR = 8.0D;

    /** A card to play: hand slot and the block to deploy on. */
    public record Play(int slot, int x, int z) {
    }

    private final TeamSide side;
    private final Random random;
    private BotSettings settings = BotSettings.DEFAULT;
    private int ticks;
    private int lastPlayTick = Integer.MIN_VALUE / 2;

    public BotBrain(TeamSide side, long seed) {
        this.side = side;
        this.random = new Random(seed);
    }

    public TeamSide side() {
        return side;
    }

    /** Difficulty knobs; can change between ticks (the config file). */
    public void setSettings(BotSettings settings) {
        this.settings = settings;
    }

    /**
     * Call once per match tick. Returns the card to play now, or null. The
     * caller passes the result to {@code match.tryDeploy}.
     */
    public Play tick(MatchInstance match, CombatEngine engine) {
        ticks++;
        if (match.state() != MatchState.BATTLE ||
                ticks % settings.thinkEveryTicks() != 0 ||
                ticks - lastPlayTick < settings.minGapTicks())
            return null;

        MatchPlayer me = match.player(side);
        ArenaLayout arena = match.arena();
        Play play = spell(match, engine, me);
        if (play != null)
            return played(play);
        CombatUnit threat = nearestThreat(match, engine);
        if (threat != null) {
            int slot = sturdiestAffordable(me);
            play = slot < 0 ? null : defence(arena, slot, threat);
        } else if (me.elixirTenths() >= settings.attackMinTenths()) {
            int slot = randomAffordable(me);
            play = slot < 0 ? null : attack(match, slot);
        }
        return play == null ? null : played(play);
    }

    private Play played(Play play) {
        lastPlayTick = ticks;
        return play;
    }

    /** The enemy unit on this side's bank that is closest to its King. */
    private CombatUnit nearestThreat(MatchInstance match, CombatEngine engine) {
        ArenaLayout arena = match.arena();
        ArenaPoint king = arena.towerPoint(TowerId.king(side));
        int sign = ArenaLayout.sideSign(side);
        CombatUnit best = null;
        double bestDistance = Double.MAX_VALUE;
        for (CombatUnit unit : engine.units()) {
            if (unit.side() == side || !unit.alive() ||
                    arena.sideOf(unit.z()) != sign)
                continue;
            double distance =
                    Math.hypot(unit.x() - king.x(), unit.z() - king.z());
            if (distance < bestDistance) {
                best = unit;
                bestDistance = distance;
            }
        }
        return best;
    }

    /** The best spell play worth its elixir, or null. */
    private Play spell(MatchInstance match, CombatEngine engine,
            MatchPlayer me)
    {
        ArenaLayout arena = match.arena();
        List<BattleCard> hand = me.deck().hand();
        Play best = null;
        double bestValue = 0.0D;
        for (int slot = 0; slot < BattleDeck.HAND_SIZE; slot++) {
            BattleCard card = hand.get(slot);
            if (!card.isSpell() || !me.elixir().canSpend(card.cost()))
                continue;
            double needed = card.cost() * SPELL_VALUE_PER_ELIXIR;
            for (CombatUnit aim : engine.units()) {
                if (aim.side() == side || !aim.alive())
                    continue;
                // Units walk toward this side while the spell is in flight.
                double lead = aim.stats().speed() *
                        card.spell().castTicks() / 20.0D * 0.5D;
                int x = clamp((int) Math.floor(aim.x()), arena.minX(),
                        arena.maxX());
                int z = clamp((int) Math.floor(aim.z() +
                        ArenaLayout.sideSign(side) * lead), arena.minZ(),
                        arena.maxZ());
                double value = spellValue(engine, card, x + 0.5D, z + 0.5D);
                if (value >= needed && value > bestValue) {
                    best = new Play(slot, x, z);
                    bestValue = value;
                }
            }
        }
        return best;
    }

    /** Enemy health a spell centred on (x, z) would remove; 0 below 2 hits. */
    private double spellValue(CombatEngine engine, BattleCard card, double x,
            double z)
    {
        double perTarget = card.spell().damagePerTarget();
        double value = 0.0D;
        int hits = 0;
        for (CombatUnit unit : engine.units()) {
            if (unit.side() == side || !unit.alive())
                continue;
            if (Math.hypot(unit.x() - x, unit.z() - z) <=
                    card.spell().radius() + unit.radius()) {
                value += Math.min(unit.health(), perTarget);
                hits++;
            }
        }
        return hits >= 2 ? value : 0.0D;
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private Play defence(ArenaLayout arena, int slot, CombatUnit threat) {
        int sign = ArenaLayout.sideSign(side);
        int x = (int) Math.floor(threat.x());
        int z = (int) Math.floor(threat.z() + sign * DEFENCE_DEPTH);
        return clamped(arena, slot, x, z);
    }

    private Play attack(MatchInstance match, int slot) {
        ArenaLayout arena = match.arena();
        TeamSide enemy = side.opponent();
        TowerState left = match.tower(TowerId.left(enemy));
        TowerState right = match.tower(TowerId.right(enemy));
        TowerId lane;
        if (left.isDestroyed() != right.isDestroyed())
            lane = left.isDestroyed() ? TowerId.right(enemy) :
                    TowerId.left(enemy);
        else if (left.health() != right.health())
            lane = left.health() < right.health() ? TowerId.left(enemy) :
                    TowerId.right(enemy);
        else
            lane = random.nextBoolean() ? TowerId.left(enemy) :
                    TowerId.right(enemy);
        int sign = ArenaLayout.sideSign(side);
        int x = arena.towerPoint(lane).x() + random.nextInt(3) - 1;
        int z = arena.center().z() + sign *
                (ArenaLayout.RIVER_HALF + ATTACK_DEPTH + random.nextInt(3));
        return clamped(arena, slot, x, z);
    }

    private Play clamped(ArenaLayout arena, int slot, int x, int z) {
        ArenaBox box = arena.deployArea(side);
        return new Play(slot, Math.max(box.minX(), Math.min(box.maxX(), x)),
                Math.max(box.minZ(), Math.min(box.maxZ(), z)));
    }

    /** Hand slots of troop cards the bot can pay for now. */
    private List<Integer> affordableSlots(MatchPlayer me) {
        List<Integer> slots = new ArrayList<>();
        List<BattleCard> hand = me.deck().hand();
        for (int slot = 0; slot < BattleDeck.HAND_SIZE; slot++) {
            if (!hand.get(slot).isSpell() &&
                    me.elixir().canSpend(hand.get(slot).cost()))
                slots.add(slot);
        }
        return slots;
    }

    private int randomAffordable(MatchPlayer me) {
        List<Integer> slots = affordableSlots(me);
        return slots.isEmpty() ? -1 : slots.get(random.nextInt(slots.size()));
    }

    /** Most total health on the board for the elixir; first slot on ties. */
    private int sturdiestAffordable(MatchPlayer me) {
        int best = -1;
        double bestHealth = -1.0D;
        for (int slot : affordableSlots(me)) {
            var stats = me.deck().hand().get(slot).stats();
            double health = stats.health() * stats.count();
            if (health > bestHealth) {
                best = slot;
                bestHealth = health;
            }
        }
        return best;
    }
}
