package net.susu609.royalarena;

import net.susu609.royalarena.card.BattleCard;
import net.susu609.royalarena.card.MobSounds;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

/**
 * Every troop's hurt, death and attack sound names a real 1.20.1 sound event
 * (the list in src/test/resources, taken from vanilla sounds.json), so a
 * misspelt id cannot fall silent in the game.
 */
public final class MobSoundsSmokeTest {
    private static final Path EVENTS =
            Path.of("src/test/resources/vanilla_sound_events_1_20_1.txt");

    private MobSoundsSmokeTest() {
    }

    public static void run() {
        if (!Files.exists(EVENTS))
            return;
        Set<String> events = new HashSet<>();
        try {
            for (String line : Files.readAllLines(EVENTS)) {
                if (!line.isBlank() && !line.startsWith("#"))
                    events.add(line.trim());
            }
        } catch (IOException e) {
            throw new AssertionError(e);
        }
        for (BattleCard card : BattleCard.values()) {
            if (card.isSpell())
                continue;
            for (boolean tiny : new boolean[] {false, true}) {
                known(events, MobSounds.hurt(card.entityId(), tiny), card);
                known(events, MobSounds.death(card.entityId(), tiny), card);
            }
            String attack = MobSounds.attack(card.entityId());
            if (attack != null)
                known(events, attack, card);
        }
        check(MobSounds.hurt("minecraft:cave_spider", false)
                .equals("entity.spider.hurt"), "a Cave Spider sounds like a Spider");
        check(MobSounds.death("minecraft:slime", true)
                .equals("entity.slime.death_small"), "a tiny slime sounds small");
        check(MobSounds.hurt("minecraft:zombie", true)
                .equals("entity.zombie.hurt"), "only slimes have small sounds");
        System.out.println("RoyalArena mob sounds tests: PASS");
    }

    private static void known(Set<String> events, String id, BattleCard card) {
        check(events.contains(id), card.id() + ": no 1.20.1 sound " + id);
    }

    private static void check(boolean condition, String name) {
        if (!condition)
            throw new AssertionError(name);
    }
}
