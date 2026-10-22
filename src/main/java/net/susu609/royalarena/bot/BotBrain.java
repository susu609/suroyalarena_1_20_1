package net.susu609.royalarena.bot;

import net.susu609.royalarena.arena.ArenaBox;
import net.susu609.royalarena.arena.ArenaLayout;
import net.susu609.royalarena.arena.ArenaPoint;
import net.susu609.royalarena.card.BattleCard;
import net.susu609.royalarena.card.BattleDeck;
import net.susu609.royalarena.card.CardRole;
import net.susu609.royalarena.card.SpellStats;
import net.susu609.royalarena.combat.CombatEngine;
import net.susu609.royalarena.combat.CombatUnit;
import net.susu609.royalarena.match.ElixirMeter;
import net.susu609.royalarena.match.MatchInstance;
import net.susu609.royalarena.match.MatchPlayer;
import net.susu609.royalarena.match.MatchState;
import net.susu609.royalarena.match.TeamSide;
import net.susu609.royalarena.tower.TowerId;
import net.susu609.royalarena.tower.TowerKind;
import net.susu609.royalarena.tower.TowerState;

import java.util.ArrayList;
import java.util.Collections;
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
 *
 * <p>On top of that each bot has a {@link BotPersonality} (drawn from the
 * seed unless set), and every personality uses the same skills: a defender
 * chosen to counter the threat (splash on swarms, damage on a big unit), the
 * weaker lane with an occasional switch or split, a cheap troop to pull a
 * lone attacker into the middle of both towers, and a quick attack while the
 * opponent's elixir is low. It never wakes a sleeping enemy King with a
 * spell: that only gives the opponent a third tower that shoots.</p>
 */
public final class BotBrain {
    public static final int THINK_EVERY_TICKS = 10;
    public static final int MIN_GAP_TICKS = 40;
    public static final int ATTACK_MIN_TENTHS = 70;
    private static final int DEFENCE_DEPTH = 3;
    private static final int ATTACK_DEPTH = 4;
    /** About how long the server shows the bot dragging a card. */
    public static final int DRAG_TICKS = 14;
    /** Enemy health a spell must take off, per elixir it costs. */
    public static final double SPELL_VALUE_PER_ELIXIR = 8.0D;
    /** Share of its damage per second a hastened troop adds. */
    private static final double HASTE_ATTACK = 0.75D;
    /** Share of its damage per second a slowed (webbed) troop loses. */
    private static final double WEB_ATTACK = 0.5D;
    /** A healing potion is only worth it on troops at or under this share. */
    private static final double HEAL_BELOW = 0.7D;
    /** Opponent elixir (tenths) at or under which a cheap attack punishes. */
    private static final int PUNISH_BELOW_TENTHS = 20;
    /** Own elixir (tenths) needed for that punishing attack. */
    private static final int PUNISH_WITH_TENTHS = 40;
    /** How long after a tank a Tank Push keeps feeding troops behind it. */
    private static final int PUSH_TICKS = 200;
    /** How long after its first troop a Rush bot sends the second. */
    private static final int RUSH_PAIR_TICKS = 80;
    /** How long after a defence a Counter bot looks for a counter-push. */
    private static final int COUNTER_TICKS = 140;
    /** Princess health share under which a Spell Siege bot casts on it. */
    private static final double SIEGE_BELOW = 0.3D;
    /** Blocks past a range at which an enemy still counts as fighting. */
    private static final double ENGAGED_MARGIN = 2.0D;

    /** A card to play: hand slot and the block to deploy on. */
    public record Play(int slot, int x, int z) {
    }

    private final TeamSide side;
    private final Random random;
    private BotSettings settings = BotSettings.DEFAULT;
    private int ticks;
    private int lastPlayTick = Integer.MIN_VALUE / 2;
    private BotPersonality personality;
    /** Tick of the last tank a Tank Push bot played, and its lane X. */
    private int pushTick = Integer.MIN_VALUE / 2;
    /** Tick of the last defence. */
    private int defendedTick = Integer.MIN_VALUE / 2;
    /** Lane a Rush bot plays next: true = left. */
    private boolean rushLeft;
    /** The last Rush play, which a second troop follows into the lane. */
    private Play rushPlay;
    private int rushTick = Integer.MIN_VALUE / 2;

    public BotBrain(TeamSide side, long seed) {
        this(side, seed, BotPersonality.pick(seed));
    }

    public BotBrain(TeamSide side, long seed, BotPersonality personality) {
        this.side = side;
        this.random = new Random(seed);
        this.personality = personality;
        this.rushLeft = (seed & 1L) == 0L;
    }

    public BotPersonality personality() {
        return personality;
    }

    /** Overrides the drawn personality (the config). */
    public void setPersonality(BotPersonality personality) {
        if (personality != null)
            this.personality = personality;
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
        if (play == null && personality == BotPersonality.SPELL_SIEGE)
            play = siege(match, engine, me);
        if (play != null)
            return played(play);
        CombatUnit threat = nearestThreat(match, engine);
        if (threat != null) {
            play = kite(match, engine, me, threat);
            if (play == null) {
                int slot = counterAffordable(engine, me, threat);
                play = slot < 0 ? null : defence(arena, slot, threat);
            }
            if (play != null)
                defendedTick = ticks;
        } else {
            play = followUp(match, engine, me);
            if (play == null && wantsToAttack(match, me))
                play = attackPlay(match, me);
            // A building is for defence: it only goes out unprompted when
            // the elixir would otherwise overflow.
            if (play != null && me.deck().hand().get(play.slot()).isBuilding()
                    && me.elixirTenths() < ElixirMeter.MAX_TENTHS)
                play = null;
        }
        return play == null ? null : played(play);
    }

    /**
     * Attack when this personality's threshold is reached, or at once (with
     * a little elixir) while the opponent has just spent nearly all of its:
     * it cannot answer for a few seconds. A player who counts every card
     * knows the opponent's elixir too.
     */
    private boolean wantsToAttack(MatchInstance match, MatchPlayer me) {
        if (me.elixirTenths() >=
                personality.attackMinTenths(settings.attackMinTenths()))
            return true;
        return match.player(side.opponent()).elixirTenths() <=
                PUNISH_BELOW_TENTHS &&
                me.elixirTenths() >= PUNISH_WITH_TENTHS;
    }

    /** The attacking card and spot for this personality, or null to wait. */
    private Play attackPlay(MatchInstance match, MatchPlayer me) {
        ArenaLayout arena = match.arena();
        int sign = ArenaLayout.sideSign(side);
        switch (personality) {
            case TANK_PUSH: {
                int tank = tankSlot(me, true);
                if (tank >= 0) {
                    pushTick = ticks;
                    // From the back, so support troops can catch up.
                    Play back = attack(match, tank);
                    int z = arena.center().z() + sign *
                            (ArenaLayout.RIVER_HALF + 11);
                    return clamped(arena, tank, back.x(), z);
                }
                // A tank in hand that it cannot pay for yet: keep saving,
                // unless the elixir would overflow.
                if (tankSlot(me, false) >= 0 &&
                        me.elixirTenths() < ElixirMeter.MAX_TENTHS)
                    return null;
                int slot = cheapestAffordable(me);
                return slot < 0 ? null : attack(match, slot);
            }
            case RUSH: {
                int slot = cheapestAffordable(me);
                if (slot < 0)
                    return null;
                Play lane = rushLane(match, slot);
                rushLeft = !rushLeft;
                rushPlay = lane;
                rushTick = ticks;
                return lane;
            }
            default: {
                int slot = randomAffordable(me);
                return slot < 0 ? null : attack(match, slot);
            }
        }
    }

    /**
     * A Tank Push feeds troops in behind its tank; a Counter bot sends one
     * in behind the defenders that survived a defence. Null when neither.
     */
    private Play followUp(MatchInstance match, CombatEngine engine,
            MatchPlayer me)
    {
        int sign = ArenaLayout.sideSign(side);
        ArenaLayout arena = match.arena();
        if (personality == BotPersonality.TANK_PUSH &&
                ticks - pushTick < PUSH_TICKS &&
                me.elixirTenths() >= 30)
        {
            CombatUnit tank = null;
            for (CombatUnit own : engine.units()) {
                if (own.side() == side && own.alive() &&
                        arena.sideOf(own.z()) != -sign && isTank(own) &&
                        (tank == null || own.health() > tank.health()))
                    tank = own;
            }
            int slot = supportSlot(me);
            if (tank != null && slot >= 0)
                return clamped(arena, slot, (int) Math.floor(tank.x()),
                        (int) Math.floor(tank.z() + sign * 2.5D));
        }
        // A Rush bot sends a second troop after the first, so the tower
        // cannot take them one at a time.
        if (personality == BotPersonality.RUSH && rushPlay != null &&
                ticks - rushTick < RUSH_PAIR_TICKS)
        {
            int slot = cheapestAffordable(me);
            if (slot >= 0) {
                Play pair = clamped(arena, slot, rushPlay.x(), rushPlay.z());
                rushPlay = null;
                return pair;
            }
        }
        if (personality == BotPersonality.COUNTER &&
                ticks - defendedTick < COUNTER_TICKS &&
                me.elixirTenths() >= 40)
        {
            CombatUnit front = null;
            for (CombatUnit own : engine.units()) {
                if (own.side() == side && own.alive() &&
                        arena.sideOf(own.z()) == sign &&
                        (front == null || own.z() * sign < front.z() * sign))
                    front = own;
            }
            int slot = randomAffordable(me);
            if (front != null && slot >= 0) {
                defendedTick = Integer.MIN_VALUE / 2;
                return clamped(arena, slot, (int) Math.floor(front.x()),
                        (int) Math.floor(front.z() + sign * 1.5D));
            }
        }
        return null;
    }

    /**
     * Pulls a lone attacker into the middle, where both Princess towers
     * shoot it: a cheap troop (2 elixir or less) in the centre of the own
     * half, sometimes. Useless on troops that only hit towers, and only when
     * no own troop already stands near it.
     */
    private Play kite(MatchInstance match, CombatEngine engine, MatchPlayer me,
            CombatUnit threat)
    {
        if (threat.stats().traits().buildingsOnly() || random.nextInt(3) != 0)
            return null;
        for (CombatUnit unit : engine.units()) {
            if (!unit.alive() || unit == threat)
                continue;
            double d = Math.hypot(unit.x() - threat.x(), unit.z() - threat.z());
            if (unit.side() == side && d < 5.0D)
                return null;
            if (unit.side() != side && d < 3.0D)
                return null; // a group: fight it, do not pull it
        }
        int slot = -1;
        for (int candidate : affordableSlots(me)) {
            BattleCard card = me.deck().hand().get(candidate);
            if (card.cost() <= 2 && (slot < 0 ||
                    card.cost() < me.deck().hand().get(slot).cost()))
                slot = candidate;
        }
        if (slot < 0)
            return null;
        ArenaLayout arena = match.arena();
        int sign = ArenaLayout.sideSign(side);
        return clamped(arena, slot, arena.center().x(),
                arena.center().z() + sign * (ArenaLayout.RIVER_HALF + 5));
    }

    /**
     * A Spell Siege bot finishes an enemy Princess tower under
     * {@link #SIEGE_BELOW} of its health with a damage spell, keeping
     * another spell in hand unless this one destroys the tower.
     */
    private Play siege(MatchInstance match, CombatEngine engine,
            MatchPlayer me)
    {
        List<BattleCard> hand = me.deck().hand();
        int spells = 0;
        for (BattleCard card : hand)
            spells += card.isSpell() ? 1 : 0;
        ArenaLayout arena = match.arena();
        for (TowerId id : new TowerId[] {TowerId.left(side.opponent()),
                TowerId.right(side.opponent())})
        {
            TowerState tower = match.tower(id);
            if (tower.isDestroyed() ||
                    tower.health() > tower.maxHealth() * SIEGE_BELOW)
                continue;
            ArenaPoint at = arena.towerPoint(id);
            for (int slot = 0; slot < BattleDeck.HAND_SIZE; slot++) {
                BattleCard card = hand.get(slot);
                if (!card.isSpell() || card.spell().friendly() ||
                        card.spell().towerMultiplier() <= 0.0D ||
                        card.spell().damagePerPulse() <= 0.0D ||
                        !me.elixir().canSpend(card.cost()) ||
                        !arena.canPlay(side, card, at.x(), at.z()) ||
                        wakesKing(match, engine, card, at.x() + 0.5D,
                                at.z() + 0.5D))
                    continue;
                double toTower = card.spell().damagePerPulse() *
                        card.spell().pulses() * card.spell().towerMultiplier();
                if (spells > 1 || toTower >= tower.health())
                    return new Play(slot, at.x(), at.z());
            }
        }
        return null;
    }

    /**
     * Whether a hurting spell centred here would touch the enemy King while
     * it still sleeps: the hit wakes it, and then it shoots too.
     */
    private boolean wakesKing(MatchInstance match, CombatEngine engine,
            BattleCard card, double x, double z)
    {
        TeamSide enemy = side.opponent();
        // Only a hit that takes health off wakes the King.
        if (card.spell().friendly() || card.spell().poison() ||
                card.spell().damagePerPulse() * card.spell().towerMultiplier()
                        <= 0.0D || engine.kingAwake(enemy))
            return false;
        return match.arena().distanceToTower(x, z, TowerId.king(enemy)) <=
                card.spell().radius();
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
            // A spell that deals nothing (Cobweb, Swiftness) only pays off
            // in a big fight: it waits for half as much again.
            if (card.spell().damagePerPulse() <= 0.0D)
                needed *= 1.5D;
            // Milk is aimed at its own troops too, to free them.
            boolean heal = card.spell().friendly() || card.spell().cleanse();
            List<int[]> aims = new ArrayList<>();
            for (CombatUnit aim : engine.units()) {
                // Healing and Swiftness go on own troops, the rest on
                // enemies; an Ender Pearl on either.
                if (((aim.side() == side) != heal && !card.pearls()) ||
                        !aim.alive())
                    continue;
                // Enemies walk toward this side (own troops away from it)
                // while the card is dragged and the spell is in flight.
                // A thrown spell flies from the King, so far aims need more.
                double lead = aim.stats().speed() *
                        (arena.deployTicks(card, side, aim.x(), aim.z())
                                + DRAG_TICKS) / 20.0D * 0.5D;
                int sign = ArenaLayout.sideSign(side) * (heal ? -1 : 1);
                aims.add(new int[] {(int) Math.floor(aim.x()),
                        (int) Math.floor(aim.z() + sign * lead)});
            }
            if (card.spell().freezeTicks() > 0) {
                // Freezing a tower helps the troops under its fire.
                for (TowerId id : TowerId.values()) {
                    if (id.side() != side && !match.tower(id).isDestroyed()) {
                        ArenaPoint at = arena.towerPoint(id);
                        aims.add(new int[] {at.x(), at.z()});
                    }
                }
            }
            for (int[] aim : aims) {
                int x = clamp(aim[0], arena.minX(), arena.maxX());
                int z = arena.nearestReach(side, card,
                        clamp(aim[1], arena.minZ(), arena.maxZ()));
                if (wakesKing(match, engine, card, x + 0.5D, z + 0.5D))
                    continue;
                double value = spellValue(match, engine, card, x + 0.5D,
                        z + 0.5D);
                if (personality == BotPersonality.SPELL_SIEGE)
                    value *= 1.2D;
                if (value >= needed && value > bestValue) {
                    best = new Play(slot, x, z);
                    bestValue = value;
                }
            }
        }
        return best;
    }

    /**
     * What Milk Bucket centred on (x, z) is worth: for each own troop that
     * is fighting, the damage it gets to deal again (frozen or webbed time)
     * and the burn it no longer takes; for each enemy, its haste lost, less
     * the freeze, web and burn this side had put on it. 0 below 2 units.
     */
    private double cleanseValue(MatchInstance match, CombatEngine engine,
            SpellStats spell, double x, double z)
    {
        double total = 0.0D;
        int touched = 0;
        for (CombatUnit unit : engine.units()) {
            if (!unit.alive() || Math.hypot(unit.x() - x, unit.z() - z) >
                    spell.radius() + unit.radius())
                continue;
            double dps = unit.stats().damage() * 20.0D /
                    unit.stats().attackTicks();
            double stopped = unit.frozenTicks() / 20.0D * dps +
                    (unit.webbed() ? unit.slowTicks() / 20.0D * dps * 0.5D :
                            0.0D) + unit.burnTicks() / 20.0D;
            double hasted = unit.hasteTicks() / 20.0D * dps * HASTE_ATTACK;
            if (unit.side() == side)
                total += (fighting(match, engine, unit) ? stopped : 0.0D) -
                        hasted;
            else
                total += hasted - stopped;
            touched++;
        }
        return touched < 2 ? 0.0D : total;
    }

    /**
     * What a spell centred on (x, z) is worth: the enemy health it would
     * remove, plus for each unit it freezes or stuns while that unit fights
     * this side the damage the unit does not deal meanwhile (only the
     * strongest few for a spell with {@code maxTargets}), plus the shots a
     * frozen enemy tower does not fire at own troops; or the health a
     * healing potion would give back to own troops. Poison counts by what
     * each target is made of; a slow (Cobweb, Snowball Volley) is worth its
     * seconds on an enemy that has crossed to this side; Swiftness is worth
     * a third of the damage per second of each own troop that is fighting,
     * for as long as it lasts, plus the walk it saves. 0 below 2 targets.
     */
    private double spellValue(MatchInstance match, CombatEngine engine,
            BattleCard card, double x, double z)
    {
        SpellStats spell = card.spell();
        if (spell.cleanse())
            return cleanseValue(match, engine, spell, x, z);
        if (card.pearls())
            return pearlValue(match, engine, spell, x, z);
        if (card.douses())
            return waterValue(match, engine, spell, x, z);
        if (card.dropsOnOne())
            return anvilValue(match, engine, spell, x, z);
        if (card.rings())
            return bellValue(match, engine, spell, x, z);
        double perTarget = spell.damagePerTarget();
        // The skull's wither keeps hurting after the blast.
        if (card.withers())
            perTarget += BattleCard.WITHER_PER_SECOND *
                    BattleCard.WITHER_TICKS / 20.0D;
        double seconds = Math.max(spell.freezeTicks(), spell.stunTicks()) /
                20.0D;
        List<Double> values = new ArrayList<>();
        double slowSeconds = spell.slowTicks() <= 0 ? 0.0D :
                (spell.durationTicks() + spell.slowTicks()) / 20.0D;
        double hasteSeconds = spell.hasteTicks() / 20.0D;
        for (CombatUnit unit : engine.units()) {
            if ((unit.side() == side) != spell.friendly() || !unit.alive())
                continue;
            if (Math.hypot(unit.x() - x, unit.z() - z) >
                    spell.radius() + unit.radius())
                continue;
            double dps = unit.stats().damage() * 20.0D /
                    unit.stats().attackTicks();
            if (hasteSeconds > 0.0D) {
                // Only troops already in a fight gain from it.
                if (fighting(match, engine, unit))
                    values.add(hasteSeconds * dps * HASTE_ATTACK);
                continue;
            }
            if (spell.poison()) {
                BattleCard of = BattleCard.forStats(unit.stats());
                double factor = of == null ? 1.0D : of.body().poisonFactor();
                if (factor > 0.0D)
                    values.add(Math.min(unit.health(), perTarget * factor));
                continue;
            }
            if (spell.heal()) {
                // Health only matters to a troop that is fighting now and
                // has lost a good share of it.
                if (fighting(match, engine, unit) &&
                        unit.health() <= unit.maxHealth() * HEAL_BELOW)
                    values.add(Math.min(unit.maxHealth() - unit.health(),
                            perTarget));
            }
            else
                // Armour takes its share off the hit.
                values.add(Math.min(unit.health(),
                        perTarget * unit.stats().armorFactor()) +
                        (engaged(match, engine, unit) ? seconds * dps : 0.0D) +
                        (slowSeconds > 0.0D && engaged(match, engine, unit) ?
                                slowSeconds * dps * WEB_ATTACK : 0.0D));
        }
        if (seconds > 0.0D) {
            for (TowerId id : TowerId.values()) {
                if (id.side() == side || match.tower(id).isDestroyed() ||
                        match.arena().distanceToTower(x, z, id) >
                                spell.radius() ||
                        !underFire(match, engine, id))
                    continue;
                TowerKind kind = id.kind();
                values.add(seconds * kind.damage() * 20.0D /
                        kind.attackTicks());
            }
        }
        if (values.size() < 2)
            return 0.0D;
        values.sort(Collections.reverseOrder());
        int count = spell.maxTargets() > 0 ?
                Math.min(spell.maxTargets(), values.size()) : values.size();
        double value = 0.0D;
        for (int i = 0; i < count; i++)
            value += values.get(i);
        return value;
    }

    /**
     * What an Ender Pearl landing on (x, z) is worth: on an enemy hitting
     * one of this side's towers, the damage it does not deal while it walks
     * back (half of it: it comes again). Like the engine, it takes the
     * troop nearest the centre; the bot does not pearl its own troops.
     */
    private double pearlValue(MatchInstance match, CombatEngine engine,
            SpellStats spell, double x, double z)
    {
        CombatUnit best = null;
        double bestDistance = Double.MAX_VALUE;
        for (CombatUnit unit : engine.units()) {
            BattleCard card = BattleCard.forStats(unit.stats());
            if (!unit.alive() || (card != null && card.isBuilding()))
                continue;
            double d = Math.hypot(unit.x() - x, unit.z() - z);
            if (d <= spell.radius() + unit.radius() && d < bestDistance) {
                best = unit;
                bestDistance = d;
            }
        }
        if (best == null || best.stats().speed() <= 0.0D)
            return 0.0D;
        double dps = best.stats().damage() * 20.0D / best.stats().attackTicks();
        double seconds = BattleCard.PEARL_DISTANCE / best.stats().speed();
        // Only an enemy already hitting one of this side's towers is worth
        // sending back: it walks in again, so half the time counts.
        if (best.side() == side || !hittingTower(match, best))
            return 0.0D;
        return seconds * dps * 0.5D;
    }

    /**
     * What an Anvil on (x, z) is worth: like the engine, it falls on the
     * enemy troop nearest the centre; the damage it takes off (half the
     * armour ignored) and, on a troop that is fighting, the stun.
     */
    private double anvilValue(MatchInstance match, CombatEngine engine,
            SpellStats spell, double x, double z)
    {
        CombatUnit best = null;
        double bestDistance = Double.MAX_VALUE;
        for (CombatUnit unit : engine.units()) {
            if (unit.side() == side || !unit.alive())
                continue;
            double d = Math.hypot(unit.x() - x, unit.z() - z);
            if (d <= spell.radius() + unit.radius() && d < bestDistance) {
                best = unit;
                bestDistance = d;
            }
        }
        if (best == null)
            return 0.0D;
        double dps = best.stats().damage() * 20.0D / best.stats().attackTicks();
        double hit = spell.damagePerPulse() *
                (0.5D + 0.5D * best.stats().armorFactor());
        return Math.min(best.health(), hit) +
                (engaged(match, engine, best) ?
                        spell.stunTicks() / 20.0D * dps : 0.0D);
    }

    /**
     * What a Bell on (x, z) is worth: for each enemy in it that is fighting,
     * the damage it does not deal while stunned and walking to the bell;
     * an invisible or playing-dead troop seen again, a mirror image gone.
     * 0 below 2 enemies.
     */
    private double bellValue(MatchInstance match, CombatEngine engine,
            SpellStats spell, double x, double z)
    {
        double total = 0.0D;
        int touched = 0;
        double seconds = (spell.stunTicks() + BattleCard.BELL_LURE_TICKS) /
                20.0D;
        for (CombatUnit unit : engine.units()) {
            if (unit.side() == side || !unit.alive() ||
                    Math.hypot(unit.x() - x, unit.z() - z) >
                            spell.radius() + unit.radius())
                continue;
            touched++;
            double dps = unit.stats().damage() * 20.0D /
                    unit.stats().attackTicks();
            if (engaged(match, engine, unit))
                total += seconds * dps;
            if (unit.untargetable())
                total += unit.health() * 0.25D;
        }
        return touched < 2 ? 0.0D : total;
    }

    /** Whether an enemy troop is within reach of one of this side's towers. */
    private boolean hittingTower(MatchInstance match, CombatUnit enemy) {
        for (TowerId id : TowerId.values()) {
            if (id.side() == side && !match.tower(id).isDestroyed() &&
                    match.arena().distanceToTower(enemy.x(), enemy.z(), id) -
                            enemy.radius() <= enemy.stats().range() + 0.5D)
                return true;
        }
        return false;
    }

    /**
     * What a Water Bucket on (x, z) is worth: its damage on the enemies water
     * hurts, the burn it saves own troops, and the time pushed enemies that
     * fight this side lose. 0 below 2 units touched.
     */
    private double waterValue(MatchInstance match, CombatEngine engine,
            SpellStats spell, double x, double z)
    {
        double total = 0.0D;
        int touched = 0;
        for (CombatUnit unit : engine.units()) {
            if (!unit.alive() || Math.hypot(unit.x() - x, unit.z() - z) >
                    spell.radius() + unit.radius())
                continue;
            touched++;
            if (unit.side() == side) {
                total += unit.burnTicks() / 20.0D;
                continue;
            }
            BattleCard card = BattleCard.forStats(unit.stats());
            if (card != null && card.hurtByWater())
                total += Math.min(unit.health(), BattleCard.WATER_DAMAGE);
            if (engaged(match, engine, unit) && unit.stats().speed() > 0.0D)
                total += spell.knockback() / unit.stats().speed() *
                        unit.stats().damage() * 20.0D / unit.stats().attackTicks();
        }
        return touched < 2 ? 0.0D : total;
    }

    /**
     * Whether an own troop is fighting: an enemy troop or tower is within its
     * range or the enemy's own, give or take {@link #ENGAGED_MARGIN}.
     */
    private boolean fighting(MatchInstance match, CombatEngine engine,
            CombatUnit own)
    {
        double reach = own.stats().range() + ENGAGED_MARGIN;
        for (CombatUnit enemy : engine.units()) {
            if (enemy.side() == side || !enemy.alive())
                continue;
            double edge = Math.hypot(own.x() - enemy.x(),
                    own.z() - enemy.z()) - own.radius() - enemy.radius();
            if (edge <= Math.max(reach, enemy.stats().range() + ENGAGED_MARGIN))
                return true;
        }
        for (TowerId id : TowerId.values()) {
            if (id.side() != side && !match.tower(id).isDestroyed() &&
                    match.arena().distanceToTower(own.x(), own.z(), id) -
                            own.radius() <= Math.max(reach,
                            id.kind().range() + ENGAGED_MARGIN))
                return true;
        }
        return false;
    }

    /**
     * Whether an enemy is fighting this side, or about to: an own troop or
     * tower is within its range or the troop's own, give or take
     * {@link #ENGAGED_MARGIN}. Freezing an enemy nobody is near wastes it.
     */
    private boolean engaged(MatchInstance match, CombatEngine engine,
            CombatUnit enemy)
    {
        double reach = enemy.stats().range() + ENGAGED_MARGIN;
        for (CombatUnit own : engine.units()) {
            if (own.side() != side || !own.alive())
                continue;
            double edge = Math.hypot(own.x() - enemy.x(),
                    own.z() - enemy.z()) - own.radius() - enemy.radius();
            if (edge <= Math.max(reach, own.stats().range() + ENGAGED_MARGIN))
                return true;
        }
        for (TowerId id : TowerId.values()) {
            if (id.side() == side && !match.tower(id).isDestroyed() &&
                    match.arena().distanceToTower(enemy.x(), enemy.z(), id) -
                            enemy.radius() <= reach)
                return true;
        }
        return false;
    }

    /**
     * Whether this enemy tower shoots at own troops: one stands within its
     * range (a sleeping King shoots nothing, and the hit would wake it).
     */
    private boolean underFire(MatchInstance match, CombatEngine engine,
            TowerId id)
    {
        if (id.kind() == TowerKind.KING && !engine.kingAwake(id.side()))
            return false;
        for (CombatUnit own : engine.units()) {
            if (own.side() == side && own.alive() &&
                    match.arena().distanceToTower(own.x(), own.z(), id) -
                            own.radius() <= id.kind().range())
                return true;
        }
        return false;
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
        int roll = random.nextInt(10);
        // Now and then the other lane (a switch), while both towers stand.
        if (roll < 2 && !left.isDestroyed() && !right.isDestroyed())
            lane = lane == TowerId.left(enemy) ? TowerId.right(enemy) :
                    TowerId.left(enemy);
        int x = arena.towerPoint(lane).x() + random.nextInt(3) - 1;
        // Or, for a slow card, the middle behind the King: it picks a lane
        // late, so the opponent cannot tell which one to defend.
        if (roll == 9 && me(match).deck().hand().get(slot).stats() != null &&
                me(match).deck().hand().get(slot).stats().speed() < 1.0D)
            return clamped(arena, slot, arena.center().x() + (sign > 0 ? 0 : -1),
                    arena.center().z() + sign * (ArenaLayout.RIVER_HALF + 11));
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

    private static boolean isTank(CombatUnit unit) {
        BattleCard card = BattleCard.forStats(unit.stats());
        return card != null && CardRole.of(card).contains(CardRole.TANK);
    }

    private MatchPlayer me(MatchInstance match) {
        return match.player(side);
    }

    /** A Rush play at the bridge of the lane whose turn it is. */
    private Play rushLane(MatchInstance match, int slot) {
        ArenaLayout arena = match.arena();
        TeamSide enemy = side.opponent();
        TowerId lane = rushLeft ? TowerId.left(enemy) : TowerId.right(enemy);
        if (match.tower(lane).isDestroyed())
            lane = rushLeft ? TowerId.right(enemy) : TowerId.left(enemy);
        int sign = ArenaLayout.sideSign(side);
        return clamped(arena, slot, arena.towerPoint(lane).x(),
                arena.center().z() + sign * (ArenaLayout.RIVER_HALF + 1));
    }

    /** Cheapest troop it can pay for now (fastest on ties), or -1. */
    private int cheapestAffordable(MatchPlayer me) {
        int best = -1;
        for (int slot : affordableSlots(me)) {
            BattleCard card = me.deck().hand().get(slot);
            if (best < 0) {
                best = slot;
                continue;
            }
            BattleCard other = me.deck().hand().get(best);
            if (card.cost() < other.cost() || card.cost() == other.cost() &&
                    card.stats().speed() > other.stats().speed())
                best = slot;
        }
        return best;
    }

    /** A tank in hand (affordable only, or any), or -1. */
    private int tankSlot(MatchPlayer me, boolean affordable) {
        List<BattleCard> hand = me.deck().hand();
        for (int slot = 0; slot < BattleDeck.HAND_SIZE; slot++) {
            BattleCard card = hand.get(slot);
            if (!card.isSpell() && CardRole.of(card).contains(CardRole.TANK) &&
                    (!affordable || me.elixir().canSpend(card.cost())))
                return slot;
        }
        return -1;
    }

    /** A troop to walk behind a tank: ranged first, never another tank. */
    private int supportSlot(MatchPlayer me) {
        int best = -1;
        for (int slot : affordableSlots(me)) {
            BattleCard card = me.deck().hand().get(slot);
            if (CardRole.of(card).contains(CardRole.TANK))
                continue;
            if (best < 0 || card.stats().range() >
                    me.deck().hand().get(best).stats().range())
                best = slot;
        }
        return best;
    }

    /**
     * The defender that answers this threat best: total health on the board
     * as before, plus area damage against a group, plus damage per second
     * against one big unit, and no swarm into an attacker that splashes. A
     * Tank Push bot keeps its tanks for attacking. Troops that only hit
     * towers cannot defend.
     */
    private int counterAffordable(CombatEngine engine, MatchPlayer me,
            CombatUnit threat)
    {
        int group = 0;
        for (CombatUnit unit : engine.units()) {
            if (unit.side() != side && unit.alive() && Math.hypot(
                    unit.x() - threat.x(), unit.z() - threat.z()) <= 3.0D)
                group++;
        }
        boolean big = threat.health() >= 40.0D;
        boolean splashes = threat.stats().traits().splash() ||
                threat.stats().traits().lines();
        int best = -1;
        double bestScore = -Double.MAX_VALUE;
        for (int slot : affordableSlots(me)) {
            BattleCard card = me.deck().hand().get(slot);
            var stats = card.stats();
            if (stats.traits().buildingsOnly())
                continue;
            double score = stats.health() * stats.count() / 40.0D;
            double dps = stats.damage() * 20.0D / stats.attackTicks() *
                    stats.count();
            if (group >= 3 && (stats.traits().splash() ||
                    stats.traits().lines()))
                score += 3.0D;
            if (big)
                score += dps / 4.0D;
            if (splashes && stats.count() >= 3)
                score -= 2.0D;
            if (personality == BotPersonality.TANK_PUSH &&
                    CardRole.of(card).contains(CardRole.TANK))
                score -= 5.0D;
            // A building pulls troops that only hit towers, and an End
            // Crystal is a trap for anything that walks into it.
            if (card.isBuilding())
                score += threat.stats().traits().buildingsOnly() ? 4.0D :
                        card.explodesOnDeath() ? 2.5D : 1.5D;
            if (score > bestScore) {
                best = slot;
                bestScore = score;
            }
        }
        return best;
    }
}
