# Su Royal Arena MVP Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:
> subagent-driven-development (recommended) or superpowers:executing-plans to
> implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for
> tracking.

**Goal:** Build the approved Royal Arena 1v1 foundation from template cleanup
through local-server matchmaking.

**Architecture:** Pure Java domain classes own deterministic rules and are
directly smoke-tested. Forge adapters own world/entity/network/client
integration, with all authoritative decisions executed on the server and
camera/HUD kept client-only.

**Tech Stack:** Java 17, Minecraft Java 1.20.1, Forge 47.4.23, official
mappings, Sponge Mixin 0.8.5, SuModLib 1.0.0.

**Spec:** `docs/superpowers/specs/2026-09-11-royal-arena-mvp-design.md`

## Global Constraints
- Package `net.susu609.royalarena`; mod id `royalarena`.
- Never import `net.minecraft.client.*` from common/server packages.
- Match/card/tower decisions are server-authoritative.
- No database, web API, Redis, Supabase or cross-server matchmaking.
- Keep battle interaction as overlay + input, not an always-open `Screen`.

---

### Task 0: Template cleanup

**Files:** modify `settings.gradle`, `gradle.properties`,
`src/main/resources/royalarena.mixins.json`; move resources to
`assets/royalarena`; remove sample config.
- [x] Replace template project identity.
- [x] Restore `mod_license` and clean description line.
- [x] Replace `examplemod.refmap.json` with `royalarena.refmap.json`.
- [x] Remove `assets/examplemod` and sample `WConfig` from output.

### Task 1-3: Domain, arena, camera transform

**Files:** create `match/*`, `arena/*`, `card/*`,
`client/camera/ArenaCameraProfile.java`, `ArenaCameraTransform.java`.

- [x] Write `DomainSmokeTest` first and verify it fails because production
  classes are absent.
- [x] Implement minimal domain and geometry classes.
- [x] Compile/run smoke test and verify pass.

### Task 4: Server lifecycle + sync
**Files:** create `server/match/*`, `network/*`, `event/CommonForgeEvents.java`.

- [x] Start two-player sessions, allocate/build arena, enter countdown/battle,
  restore players at finish.
- [x] Sync snapshots to both players and clean up logout/disconnect.

### Task 5: Client camera/HUD/input

**Files:** create `client/state/*`, `client/camera/ArenaCameraController.java`,
`client/hud/BattleHudOverlay.java`, `client/input/*`,
`client/ClientModEvents.java`, `client/ClientForgeEvents.java`.
- [x] Bind M and 1-4 keys.
- [x] Render four-card hand, next card, elixir, timer, towers.
- [x] Use a client-only marker entity as detached camera and mirror it by team.
- [x] Release the cursor under the fixed battle camera, project cursor position
  onto the arena floor, send the selected card deploy request, and cancel
  vanilla click.

### Task 6-8: Deployment, elixir/deck, towers/win

**Files:** create `server/battle/*`; extend `MatchInstance`, network snapshot
and arena builder.
- [x] Validate state/side/slot/elixir/deploy zone entirely on server.
- [x] Spawn a tagged vanilla mob placeholder and cycle deck only after accepted
  deployment.
- [x] Move server-tracked troops toward enemy towers and apply logical tower
  damage.
- [x] End match when king tower reaches zero and sync winner.

### Task 9: Matching screen + queue

**Files:** create `match/MatchQueue.java`, queue packets,
`client/screen/MatchingScreen.java`, queue client state.
- [x] M opens screen.
- [x] Join/cancel packets update server queue.
- [x] Pair first two valid online players and create a match.
- [x] Close matching screen when an active snapshot arrives.

### Task 10: Online deployment boundary
**Files:** modify `build.gradle`; create `docs/ONLINE.md`.
- [x] Add isolated `server` run configuration.
- [x] Document one-server queue architecture and optional tunnel/VPS deployment
  without adding backend dependencies.

### Final verification
- [x] Run pure domain smoke test.
- [x] Scan common/server packages for `net.minecraft.client` imports.
- [x] Scan output for `examplemod`/`suforgetemplate` residue.
- [x] Verify packet ids are unique and direction-specific.
- [x] Package one ZIP with project-root structure and no `.git`, build output,
  or run directories.
