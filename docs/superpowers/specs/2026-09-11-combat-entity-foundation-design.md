# Royal Arena Combat Entity Foundation Design

## Goal

Replace coordinate-only tower targeting with explicit server-side building
hitbox entities and stable per-troop targets, while keeping the existing Part
0-10 match/tower domain authoritative.

## Constraints

- Minecraft Java 1.20.1, Forge 47.4.23, official 1.20.1 mappings, Java 17.
- Package `net.susu609.royalarena`, mod id `royalarena`.
- Keep `MatchInstance`/`TowerState` authoritative for tower HP and winner; world
  entities are physical representatives, not persistent match state.
- No `net.minecraft.client.*` imports in common/server code.
- Keep current vanilla troop placeholders for this part; do not expand into
  troop-vs-troop combat yet.
- Prevent arena-controlled undead placeholders from remaining on fire in
  daylight without changing global vanilla mob behavior.

## Part 11 scope

1. Add a custom invisible `TowerHitboxEntity` with a real bounding box and no
   gameplay state of its own.
2. Spawn one hitbox entity for each logical tower when a match starts; keep a
   `TowerId -> entity UUID` mapping inside `ServerMatchSession`.
3. Give each `ServerTroop` a locked `TowerId` target. The initial target is
   selected from its lane. The lock is retained even if the troop crosses the
   center line; it changes only after that tower is destroyed.
4. Move/attack against the physical tower hitbox entity while applying damage
   only through `MatchInstance.damageTower`.
5. Discard a tower hitbox when its logical tower is destroyed and discard all
   building hitboxes when the match ends.
6. Prevent vanilla daylight burning only for entities tagged `royalarena_troop`
   by injecting into `Mob.isSunBurnTick()`. Normal fire, lava and future
   spell/fire damage remain unchanged.
7. Keep troop-vs-troop target acquisition, pathfinding, ranged combat,
   buildings/cards, and custom troop subclasses for later parts.

## Entity registration/rendering

`TowerHitboxEntity` is registered with Forge `DeferredRegister<EntityType<?>>`
under `royalarena:tower_hitbox`. It is `MobCategory.MISC`, fixed-size,
no-physics and invisible. A client-only vanilla `NoopRenderer` is registered
through `EntityRenderersEvent.RegisterRenderers`; this prevents missing-renderer
failures while leaving the existing block tower markers visible.

## Testing

Extend the pure `DomainSmokeTest` so a `ServerTroop` proves target locking: it
selects a lane tower once, stays locked after X position changes, and retargets
the enemy king after that lane tower is destroyed. Forge-specific entity
registration/session wiring is statically reviewed here; full Forge
compilation/runtime validation remains for the normal development machine.
