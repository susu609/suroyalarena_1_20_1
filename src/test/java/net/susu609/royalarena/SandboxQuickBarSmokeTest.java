package net.susu609.royalarena;

import net.susu609.royalarena.card.BattleCard;
import net.susu609.royalarena.match.SandboxQuickBar;

import java.util.List;

/** The sandbox quick bar: pinned cards first and kept, then the last picks. */
public final class SandboxQuickBarSmokeTest {
    private SandboxQuickBarSmokeTest() {
    }

    public static void run() {
        SandboxQuickBar bar = new SandboxQuickBar();
        check(bar.at(0) == null, "an empty bar has empty slots");
        bar.use(BattleCard.ZOMBIE);
        bar.use(BattleCard.FIREBALL);
        bar.use(BattleCard.ZOMBIE);
        check(bar.cards().equals(List.of(BattleCard.ZOMBIE, BattleCard.FIREBALL)),
                "the last pick comes first, without doubles: " + bar.cards());
        bar.togglePin(BattleCard.FIREBALL);
        check(bar.at(0) == BattleCard.FIREBALL && bar.pinned(BattleCard.FIREBALL),
                "a pinned card goes first");
        BattleCard[] all = BattleCard.values();
        for (int i = 0; i < 20; i++)
            bar.use(all[i]);
        check(bar.cards().size() == SandboxQuickBar.SIZE &&
                bar.at(0) == BattleCard.FIREBALL && bar.at(1) == all[19],
                "the bar keeps 8, the pin stays: " + bar.cards());
        bar.togglePin(BattleCard.FIREBALL);
        check(!bar.pinned(BattleCard.FIREBALL) &&
                bar.cards().size() == SandboxQuickBar.SIZE,
                "unpinned, it is a recent card again");
        SandboxQuickBar loaded = new SandboxQuickBar();
        loaded.loadPinned(List.of("giant", "no_such_card", "bell", "giant"));
        check(loaded.cards().equals(List.of(BattleCard.GIANT, BattleCard.BELL)),
                "saved pins load, unknown ids skipped: " + loaded.cards());
        check(loaded.pinnedIds().equals(List.of("giant", "bell")),
                "pins save as ids");
        System.out.println("RoyalArena sandbox quick bar tests: PASS");
    }

    private static void check(boolean condition, String name) {
        if (!condition)
            throw new AssertionError(name);
    }
}
