# Tower Model Rendering Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:
> subagent-driven-development (recommended) or superpowers:executing-plans to
> implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for
> tracking.

**Goal:** Render the supplied Crown and King Blockbench structures as typed
tower entities while retaining the Part 11 authoritative tower-domain flow.

**Architecture:** `TowerHitboxEntity` remains the common physical target.
`CrownTowerEntity` and `KingTowerEntity` provide model-specific entity types and
dimensions; client-only models/renderers are registered through Forge client mod
events. `ServerMatchSession` chooses the typed entity from logical `TowerKind`.

**Tech Stack:** Java 17, Minecraft 1.20.1, Forge 47.4.23, official mappings,
Blockbench Mojang-model export.

**Spec:** `docs/superpowers/specs/2026-09-11-tower-model-rendering-design.md`

## Global Constraints

- No client imports in common/server packages.
- `TowerState` remains authoritative for HP/win state.
- Preserve the supplied Blockbench geometry and shared texture atlas.

---

### Task 1: Tower visual mapping and entity types

**Files:**

- Create: `entity/building/TowerVisualType.java`
- Create: `entity/building/CrownTowerEntity.java`
- Create: `entity/building/KingTowerEntity.java`
- Modify: `entity/building/TowerHitboxEntity.java`
- Modify: `entity/ArenaEntityTypes.java`
- Test: `DomainSmokeTest.java`

- [x] Write a failing smoke assertion for Crown/King visual mapping and 3x4 /
  4x4 bounds.
- [x] Run the pure smoke compile and confirm it fails because `TowerVisualType`
  does not exist.
- [x] Add the visual mapping and typed tower entities.
- [x] Run the pure smoke suite and confirm it passes.

### Task 2: Blockbench client models and renderers

**Files:**

- Create: `client/model/CrownTowerModel.java`
- Create: `client/model/KingTowerModel.java`
- Create: `client/render/entity/CrownTowerRenderer.java`
- Create: `client/render/entity/KingTowerRenderer.java`
- Modify: `client/ClientModEvents.java`
- Create: `assets/royalarena/textures/entity/tower/tower_blocks.png`

- [x] Adapt both supplied Mojang-model exports to the current package/entity
  names.
- [x] Register model layer definitions and typed renderers.
- [x] Render with the shared 128x128 atlas extracted from the supplied
  `.bbmodel` files.

### Task 3: Match spawn integration and legacy marker removal

**Files:**

- Modify: `server/match/ServerMatchSession.java`
- Modify: `server/arena/ArenaBuilder.java`
- Create: `model_sources/towers/King Tower.bbmodel`
- Create: `model_sources/towers/Crown Towers.bbmodel`

- [x] Spawn typed tower entities according to `TowerKind` and side orientation.
- [x] Keep the `TowerId -> UUID` target mapping and damage flow unchanged.
- [x] Remove old wool/gold pillar visuals to prevent overlap.
- [x] Preserve Blockbench source models for future edits.

### Task 4: Verification and packaging

- [x] Run fresh pure-domain smoke tests.
- [x] Check client imports remain isolated from common/server code.
- [x] Check renderer/layer registration and resources are present.
- [x] Run Java syntax/static checks available in this environment.
- [x] Create ZIP with a single project root and validate with `unzip -t`.
