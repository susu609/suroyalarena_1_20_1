package net.susu609.royalarena;

import net.susu609.royalarena.card.BattleCard;
import net.susu609.royalarena.card.CardRole;
import net.susu609.royalarena.card.CardTactic;
import net.susu609.royalarena.card.CostFormula;
import net.susu609.royalarena.card.SpellStats;
import net.susu609.royalarena.card.TroopStats;
import net.susu609.royalarena.card.TroopTraits;
import net.susu609.royalarena.stats.Json;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Card data for the card library page ({@code scripts/card-library.sh}):
 * every card's stats, tactics and facts straight from the code, and its win
 * rate in bot matches ({@link BalanceReport}). The script adds the card art,
 * the branch status and the queue.
 *
 * <p>Arguments: number of bot matches (default 4000), output file.</p>
 */
public final class CardLibraryReport {
    private CardLibraryReport() {
    }

    public static void main(String[] args) throws IOException {
        int matches = args.length > 0 ? Integer.parseInt(args[0]) : 4000;
        Path out = Path.of(args.length > 1 ? args[1] : "build/card-library/cards.json");
        BalanceReport.Result bots = BalanceReport.play(matches);
        List<Object> cards = new ArrayList<>();
        for (BattleCard card : BattleCard.values()) {
            Map<String, Object> c = new LinkedHashMap<>();
            c.put("id", card.id());
            c.put("name", card.displayName());
            c.put("cost", card.cost());
            List<Object> tactics = new ArrayList<>();
            for (CardTactic t : card.tactics())
                tactics.add(t.label());
            c.put("tactics", tactics);
            List<Object> tribes = new ArrayList<>();
            for (net.susu609.royalarena.card.Tribe t : card.tribes())
                tribes.add(t.labelVi());
            c.put("tribes", tribes);
            if (card.isSpell())
                spell(c, card.spell());
            else
                troop(c, card);
            BalanceReport.Tally tally = bots.cards.get(card);
            c.put("win", tally == null ? 0 : (int) Math.round(tally.rate() * 100.0D));
            cards.add(c);
        }
        Files.createDirectories(out.toAbsolutePath().getParent());
        Files.writeString(out, Json.write(cards));
        System.out.println(cards.size() + " cards, " + matches + " matches -> " + out);
    }

    private static void spell(Map<String, Object> c, SpellStats s) {
        c.put("kind", "spell");
        c.put("raw", round2(CostFormula.raw(s)));
        List<Object> f = new ArrayList<>();
        f.add(String.format(Locale.ROOT, "radius %.1f", s.radius()));
        f.add(String.format(Locale.ROOT, "flies %.2f s", s.castTicks() / 20.0D));
        if (s.damagePerPulse() > 0) {
            if (s.pulses() > 1) {
                String what = s.heal() ? "heals" : s.poison() ? "poison" : "damage";
                f.add(String.format(Locale.ROOT, "%s %.0f over %.1f s", what,
                        s.damagePerPulse() * s.pulses(), s.pulses() * s.pulseInterval() / 20.0D));
            } else {
                f.add(String.format(Locale.ROOT, "%.0f damage", s.damagePerPulse()));
            }
        }
        if (s.maxTargets() > 0)
            f.add(s.maxTargets() + " targets");
        if (s.stunTicks() > 0)
            f.add("stun " + s.stunTicks() / 20.0D + " s");
        if (s.freezeTicks() > 0)
            f.add("freeze " + s.freezeTicks() / 20 + " s");
        if (s.slowTicks() > 0)
            f.add("web");
        if (s.hasteTicks() > 0)
            f.add("haste " + s.hasteTicks() / 20 + " s");
        if (s.knockback() > 0)
            f.add("push " + s.knockback());
        if (s.burnTicks() > 0)
            f.add(String.format(Locale.ROOT, "burn %.1f/s", s.burnPerSecond()));
        if (s.middleReach())
            f.add("own half only");
        if (!s.friendly() && !s.poison() && s.damagePerPulse() > 0)
            f.add(Math.round(s.towerMultiplier() * 100) + "% to towers");
        c.put("facts", f);
    }

    private static void troop(Map<String, Object> c, BattleCard card) {
        TroopStats s = card.stats();
        c.put("kind", "troop");
        c.put("raw", round2(CostFormula.raw(s)));
        c.put("count", s.count());
        c.put("hp", s.health());
        c.put("dmg", s.damage());
        c.put("every", round2(s.attackTicks() / 20.0D));
        c.put("range", Math.round(s.range() * 10.0D) / 10.0D);
        c.put("speed", round2(s.speed()));
        c.put("armor", s.armor());
        c.put("ticks", s.attackTicks());
        // What the card config page needs to recompute the cost.
        c.put("traitMod", CostFormula.traitModifier(s));
        c.put("generations", s.split() == null || s.split().into() != null ?
                1 : 1 + s.split().generations());
        c.put("riderHp", s.split() != null && s.split().into() != null ?
                s.split().into().effectiveHealth() : 0.0D);
        c.put("poisonPerSecond", s.traits().poisonPerSecond());
        c.put("poisonTicks", s.traits().poisonTicks());
        c.put("selfDestruct", s.traits().selfDestruct());
        c.put("body", card.body().name().toLowerCase(Locale.ROOT));
        c.put("fire", card.fireImmune());
        List<Object> roles = new ArrayList<>();
        for (CardRole r : CardRole.of(card))
            roles.add(r.label());
        c.put("roles", roles);
        TroopTraits t = s.traits();
        List<Object> f = new ArrayList<>();
        if (t.splash())
            f.add("splash " + t.splashRadius());
        if (t.selfDestruct())
            f.add("dies on hit");
        if (t.knockback() > 0)
            f.add("knockback " + t.knockback());
        if (t.buildingsOnly())
            f.add("towers only");
        if (t.slows())
            f.add("slows");
        if (t.poisonTicks() > 0)
            f.add("poison");
        if (t.teleports())
            f.add("teleports");
        if (t.hover() > 0)
            f.add("flies");
        if (t.crossesRiver())
            f.add("crosses river");
        if (t.deployOnRiver())
            f.add("river deploy");
        if (t.frostImmune())
            f.add("frost-proof");
        if (t.lines())
            f.add("line hit");
        if (t.charges())
            f.add("lock-on");
        if (t.riverStrong())
            f.add("strong on river");
        if (t.frostWeak())
            f.add("frost-weak");
        if (s.split() != null)
            f.add(s.split().into() != null ? "leaves an archer" : "splits");
        c.put("facts", f);
    }

    private static double round2(double d) {
        return Math.round(d * 100.0D) / 100.0D;
    }
}
