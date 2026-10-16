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
against a bot on RED; see `docs/BOT.md`.

Development shortcut: `/royalarena debug_start` starts a match with the first
two online players. `/royalarena stop_all` ends all active matches.

## Testing

`DomainSmokeTest` covers the pure-Java rules for team mirroring, deploy zones,
camera mirroring, deck cycling, elixir/lifecycle and King tower victory.

When Gradle is available, `check` also depends on `runDomainSmoke`.

See `docs/PARTS.md` for the implementation breakdown and `docs/ONLINE.md` for
deployment scope.
