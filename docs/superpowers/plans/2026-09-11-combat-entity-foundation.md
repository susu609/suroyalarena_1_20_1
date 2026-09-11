# Royal Arena Combat Entity Foundation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:
> subagent-driven-development (recommended) or superpowers:executing-plans to
> implement this plan task-by-task. Steps use checkbox (`- [x]`) syntax for
> tracking.

**Goal:** Give deployed troops explicit building targets backed by real tower
hitbox entities and stop session-owned undead placeholders from daylight
burning.

**Architecture:** `MatchInstance` and `TowerState` remain authoritative domain
state. `ServerMatchSession` owns ephemeral world representatives (
`TowerHitboxEntity`) and `ServerTroop` owns a stable logical `TowerId` lock; the
session resolves that lock to a hitbox UUID for movement/attack.

**Tech Stack:** Java 17, Minecraft Java 1.20.1, Forge 47.4.23, official
mappings, Sponge Mixin 0.8.5, SuModLib 1.0.0.

**Spec:** `docs/superpowers/specs/2026-09-11-combat-entity-foundation-design.md`

## Global Constraints

- Package `net.susu609.royalarena`; mod id `royalarena`.
- Never import `net.minecraft.client.*` from common/server packages.
- Tower HP/winner stay server-authoritative in `MatchInstance`/`TowerState`.
- Part 11 targets buildings only; no troop-vs-troop combat yet.
- Do not alter sunlight behavior for ordinary Minecraft mobs outside Royal Arena
  sessions.

---

### Task 1: Stable logical target lock

**Files:**

- Modify: `src/main/java/net/susu609/royalarena/server/battle/ServerTroop.java`
- Modify: `src/test/java/net/susu609/royalarena/DomainSmokeTest.java`

**Interfaces:**

- Consumes: `MatchInstance.chooseTowerTarget(TeamSide,double)` and
  `TowerState.isDestroyed()`.
- Produces: `TowerId targetTower(MatchInstance match, double worldX)` and
  `TowerId targetTowerId()`.

- [x] Write a smoke test proving a troop keeps its first lane target when its X
  position changes and retargets the king after the lane tower is destroyed.
- [x] Run the pure-domain smoke test and confirm it fails because target locking
  is absent.
- [x] Add the minimal target-lock state/methods to `ServerTroop`.
- [x] Run the pure-domain smoke test and confirm it passes.

### Task 2: Tower hitbox entity registration

**Files:**

- Create: `src/main/java/net/susu609/royalarena/entity/ArenaEntityTypes.java`
- Create:
  `src/main/java/net/susu609/royalarena/entity/building/TowerHitboxEntity.java`
- Modify: `src/main/java/net/susu609/royalarena/RoyalArena.java`
- Modify: `src/main/java/net/susu609/royalarena/client/ClientModEvents.java`

**Interfaces:**

- Produces: registry object `ArenaEntityTypes.TOWER_HITBOX` and an invisible
  fixed-size entity suitable for session-owned target geometry.

- [x] Register `royalarena:tower_hitbox` through a Forge `DeferredRegister` on
  the mod event bus.
- [x] Implement a no-physics, pickable, non-damaging `TowerHitboxEntity` with no
  persistent match state.
- [x] Register the vanilla `NoopRenderer` for the entity type on the client mod
  bus.
- [x] Statically verify client imports remain under the client package only.

### Task 3: Session building lifecycle and entity-based combat target

**Files:**

- Modify:
  `src/main/java/net/susu609/royalarena/server/match/ServerMatchSession.java`
- Create: `src/main/java/net/susu609/royalarena/mixin/MobSunBurnMixin.java`
- Modify: `src/main/resources/royalarena.mixins.json`

**Interfaces:**

- Consumes: `ServerTroop.targetTower(...)`, `ArenaEntityTypes.TOWER_HITBOX`,
  `MatchInstance.damageTower(...)`.
- Produces: one tower hitbox per `TowerId`, lookup by UUID, destruction/cleanup
  lifecycle.

- [x] Spawn six tower hitboxes immediately after the arena blocks are built.
- [x] Resolve troop movement/attack to the hitbox entity belonging to the locked
  logical target.
- [x] On tower destruction, remove the hitbox and mark the block tower
  destroyed.
- [x] Add a scoped `Mob.isSunBurnTick()` mixin so only `royalarena_troop`
  entities ignore daylight burning while normal fire damage still works.
- [x] Remove all troop and tower hitbox entities before clearing the arena.

### Task 4: Documentation and package verification

**Files:**

- Modify: `build.gradle`
- Modify: `docs/ONLINE.md`
- Modify: `docs/PARTS.md`
- Modify: `docs/VERIFICATION.md`

- [x] Document Part 11 scope and deferred troop-vs-troop/custom troop work.
- [x] Preserve the working two-client dev setup: parent `client` has no username
  override; `client2` adds only `Dev2`; document local offline-mode requirement.
- [x] Run domain smoke verification.
- [x] Scan common/server source for client imports and template residue.
- [x] Scan changed Java sources for obvious syntax/bracing errors.
- [x] Package one clean ZIP without `.git`, `build`, `.gradle`, or run
  directories.
