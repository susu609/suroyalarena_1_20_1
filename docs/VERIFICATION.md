# Verification — 2026-10-18

Checks run on `master` at the documentation refresh (`a088338`), network
protocol 15, 40 cards. The earlier report (2026-09-11, Parts 0-11) is in the
git history of this file.

## Passed here

- PASS — `scripts/pure-test.sh`: every pure-Java smoke test through
  `DomainSmokeTest`: combat, bot, spells, deck, traits, power creep (the cost
  and power bands), match log, new mobs, wave 2 mobs, result screen, mob
  sounds, sound cues, bot personalities, domain (cameras including the
  watch view, deploy zones, match lifecycle). 14 suites.
- PASS — every card's JSON matches the code (`CombatSmokeTest`), and every
  cost equals the cost formula (`costsFollowTheFormula`).
- PASS — every sound id the mod plays exists in vanilla 1.20.1
  (`MobSoundsSmokeTest`, `SoundCuesSmokeTest` against
  `src/test/resources/vanilla_sound_events_1_20_1.txt`).
- PASS — no `new ResourceLocation(` in `src/main/java` (`CLAUDE.md`; ids go
  through `RL` from sumodlib).
- PASS — no `net.minecraft.client` or `com.mojang.blaze3d` import outside
  `client/**`.
- PASS — no `examplemod` or `suforgetemplate` left in `src/main`.
- PASS — every JSON file under `src/main/resources` parses.
- PASS — bot-versus-bot report over 4000 matches: every card 40-58%, the four
  bot personalities 43-55% (`scripts/balance.sh`, `docs/BALANCE_REPORT.md`).
- PASS — stats page data regenerated over 2000 matches (`docs/stats/data/`).

## Not checked here

The cloud session that made these changes cannot reach Forge's Maven, so the
Forge and Minecraft code was not compiled here: it was checked with `javac`
against the pure classes (only missing Minecraft symbols reported), each
vanilla API used was looked up in the decompiled 1.20.1 client sources, and
call sites of changed methods were reviewed by hand. Two compile errors still
reached the IntelliJ build that way (`BattleField` `distance`,
`MatchManager.launch`) and were fixed. Before a release, on a machine with
Forge:

1. `gradlew build` (it runs `check` and `runDomainSmoke`).
2. `runClient`: Play vs Bot, Watch Bot vs Bot (setup screen, side panels,
   `X` for x2), the result screen and Play again.
3. `runClient` + `runClient2` on LAN: Find Match between two players.
4. Listen to the sounds (`docs/SOUND_DESIGN.md`) and check the damage
   numbers and health bars at your GUI scale.
