# Royal Arena Combat

Fighting is decided by `combat/CombatEngine` (pure Java, no Minecraft
imports) and acted out in the world by `server/battle/BattleField`.

## Rules

- **Targets.** A troop chases the nearest enemy troop in sight (5.5 blocks, or
  range + 1 for archers) and keeps it until it is 25% farther than sight. With
  no enemy in sight it walks to its lane tower, locked when it is first
  ticked; once that tower falls it goes for the enemy King tower.
- **River.** Melee troops ignore enemies on the other bank unless those stand
  on a bridge; ranged troops shoot across. Anyone crossing walks to the bridge
  on its own side, over it, and on to its target (`ArenaLayout.route`).
- **Range.** Measured from edge to edge: unit radius to unit radius, or unit
  radius to the 4x4 tower footprint.
- **Timing.** A unit attacks on the first tick it is in range, then every
  `attackTicks`. Everything in a tick is decided from the state at its start
  and damage is applied together, so two units can kill each other.
- **Towers.** Princess towers shoot the nearest enemy in range (4 damage every
  16 ticks), then keep that target while it stays in range. A King tower
  (4 damage every 20 ticks) stays asleep until it is damaged or one of its
  Princess towers falls. Troops hit towers through `MatchInstance.damageTower`,
  so a destroyed King tower still ends the match.
- **Slimes.** When a Big Slime dies it splits into 2 slimes with half health,
  damage and size, twice (20 -> 2x10 -> 4x5). Children keep team and lane,
  spawn near the parent (pushed out of the water) and are invulnerable for
  6 ticks.
- **Formations.** The units of a card stand in a row (2), triangle (3) or grid
  around the tapped tile, clamped to the arena.

## In the world

`BattleField` runs once per tick: copy each mob's position into the engine,
run the tick, set mob velocities toward the waypoints, then play the hits
(swing, hurt flash, hit particles; arrows and tower shots draw a particle
line), copy engine health onto the mobs so health bars follow it, discard the
dead and spawn slime children. Mobs have no vanilla AI, are invulnerable to
vanilla damage and are discarded (not killed) so vanilla slime splitting never
adds to the card's own.

## Not built yet

Arrow Storm and Fireball, flying and building-only troops, hit knockback,
tower-count scoring and overtime.
