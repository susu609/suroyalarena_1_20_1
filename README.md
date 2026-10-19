# Su Royal Arena — Minecraft Forge 1.20.1

Prototype 1v1 arena/card battle inspired by the pacing and lane-control idea of
Clash Royale, implemented as an independent Minecraft Forge mod.

## Environment

- Minecraft Java 1.20.1
- Forge 47.4.23
- Mojang official mappings 1.20.1
- Java 17
- Mod ID: `royalarena`
- Root package: `net.susu609.royalarena`
- Shared dependency: `sumodlib` 1.0.0, built from
  [susu609/sumodlib_1_20_1](https://github.com/susu609/sumodlib_1_20_1) by
  [JitPack](https://jitpack.io/#susu609/sumodlib_1_20_1)

## Getting `sumodlib`

By default Gradle downloads it from JitPack as
`com.github.susu609:sumodlib_1_20_1:1.0.0` (`sumodlib_source=jitpack` in
`gradle.properties`). That needs a git tag named `1.0.0` on the sumodlib repo:
create a GitHub release with that tag, then open
https://jitpack.io/#susu609/sumodlib_1_20_1 and press **Get it** once so
JitPack builds it. Until the tag exists you can set
`sumodlib_jitpack_version=master-SNAPSHOT` (any branch works as
`<branch>-SNAPSHOT`).

To use your own copy instead, run `./gradlew publishToMavenLocal` in
`sumodlib_1_20_1` and set `sumodlib_source=local`.

Once a version is released on Maven Central (see `PUBLISHING.md` in
`sumodlib_1_20_1`), set `sumodlib_source=central` to use
`io.github.susu609:sumodlib:<sumodlib_version>`. Central is not the default
yet because nothing has been published there.

### Troubleshooting

`Could not find net.susu609.modlib:sumodlib:..._mapped_official_1.20.1` (or the
`com.github.susu609` equivalent) means Gradle cannot find the original
artifact: it is not in Maven Local (`sumodlib_source=local`) or JitPack has not
built that tag yet. After fixing it, reload Gradle with `--refresh-dependencies`;
if it still fails, delete `~/.gradle/caches/forge_gradle/bundled_deobf_repo`.

## MVP flow

1. Start one Forge server with Royal Arena + SuModLib.
2. Connect two players.
3. Press `M` to open Matching, then both choose **Find Match**.
4. First player is BLUE, second is RED.
5. During battle the strategic camera releases the cursor. Click a HUD card or
   use `1`–`4` to select it.
6. Left click a position on the arena to deploy; right click clears selection.
7. Server validates match state, side deploy area, slot and elixir before
   spawning a troop.
8. Troops move toward enemy Princess tower, then King tower. Destroying King
   tower ends the match.
9. Princess/King tower targets are visible custom entity models generated from
   the supplied Blockbench Crown/King tower sources.

Solo: press `M` and choose **Play vs Bot** (or `/royalarena bot`) to play BLUE
against a bot on RED. Each match the bot draws one of four personalities
(Tank Push, Rush, Counter, Spell Siege); the chat names it. See `docs/BOT.md`.

Watch: press `M` and choose **Watch Bot vs Bot** (or `/royalarena watch`) to
pick each bot's deck and personality and watch them play from a side-on
camera, with each bot's hand and elixir in a panel at the screen edge; `X`
switches to double speed. See `docs/BOT.md` and `docs/UI.md`.

Sandbox: press `M` and choose **Sandbox** (or `/royalarena sandbox`) to play
any card for either side with no clock: `Tab` switches side, `C` picks a
card, and the panel clears troops, restores towers, makes towers
unbreakable, gives endless elixir, pauses or slows time, sets tribe steps
and shows each troop's range, sight and target. See `docs/SANDBOX.md`.

Outside a match the top left corner reminds you that `M` opens the mod's
screen.

Deck: press `M` and choose **Deck** to build and save your 8-card deck from
the card library (see `docs/UI.md`).

Settings (camera angle, troop speed, bot difficulty, debug): see
`docs/CONFIG.md`.

Development shortcut: `/royalarena debug_start` starts a match with the first
two online players. `/royalarena stop_all` ends all active matches.

## Testing

`DomainSmokeTest` runs every pure-Java smoke test: team mirroring, deploy
zones, cameras, deck rules, elixir and match lifecycle, combat, spells, every
card's rule, the cost formula, the bot and its personalities, sounds, the
match log and the result screen. `gradlew check` runs it (`runDomainSmoke`).

Without Forge (for example in a cloud session), `scripts/pure-test.sh`
compiles the pure Java code and runs the same tests, and
`scripts/balance.sh 4000` prints each card's bot-versus-bot win rate. Card
numbers and the stats page: `gradlew balanceReport`, `gradlew statsReport`,
`gradlew statsServe` (see `docs/BALANCE.md`; run them from the project root).

See `docs/PARTS.md` for the implementation breakdown and `docs/ONLINE.md` for
deployment scope.
