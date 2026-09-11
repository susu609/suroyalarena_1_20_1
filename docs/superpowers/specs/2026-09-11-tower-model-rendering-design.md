# Royal Arena Tower Model Rendering Design

## Goal

Replace the temporary wool/gold tower markers with the user's Blockbench King
Tower and Crown Tower models while preserving Part 11's server-authoritative
tower state and target hitboxes.

## Constraints

- Minecraft Java 1.20.1, Forge 47.4.23, official 1.20.1 mappings, Java 17.
- Package `net.susu609.royalarena`, mod id `royalarena`.
- `MatchInstance`/`TowerState` remain authoritative for HP and winner.
- Renderer/model classes remain client-only; common/server code has no
  `net.minecraft.client.*` imports.
- Keep `TowerHitboxEntity` as the common physical-target base.
- Use the supplied Blockbench geometry without redesigning it.

## Part 12 scope

1. Add `CrownTowerEntity` and `KingTowerEntity` subclasses with hitbox
   dimensions matching the supplied model bounds: Crown 3x4 blocks, King 4x4
   blocks. Crown centers on a block (`+0.5` world offset); the even-width King
   model centers between blocks (`+0.0`) so its 16-pixel cubes align to the
   world block grid.
2. Register `royalarena:crown_tower` and `royalarena:king_tower`. Keep the Part
   11 `tower_hitbox` registration for compatibility, but new matches spawn the
   typed tower entities.
3. Adapt the supplied Blockbench Java exports into `CrownTowerModel` and
   `KingTowerModel` under the client package.
4. Extract the shared 128x128 Blockbench atlas to
   `textures/entity/tower/tower_blocks.png` and register layer
   definitions/renderers on the mod client event bus.
5. Spawn Crown models for `TowerKind.PRINCESS` and King models for
   `TowerKind.KING`; the world entity remains a view/target of logical tower
   state rather than the HP owner.
6. Remove the old wool/gold pillar tower markers so they do not overlap the
   entity models. The destroyed-tower floor marker remains.
7. Keep the supplied `.bbmodel` sources under `model_sources/towers/` for future
   Blockbench edits.

## Rendering transform

The Blockbench exports use vanilla entity-model coordinates where 16 model units
equal one Minecraft block and the ground plane is at model Y=24. Renderers use
the vanilla entity-model transform (`Y` rotation, `scale(-1,-1,1)`,
`translate(0,-1.501,0)`) so the structures sit on the arena floor while
retaining their original proportions.

## Testing

`DomainSmokeTest` verifies the logical tower-kind to visual-type mapping and
model-matched hitbox sizes. Source checks verify client/common separation,
renderer/layer registration, resource presence and spawn wiring. Full
ForgeGradle compilation/runtime rendering must still be run on the development
machine.
