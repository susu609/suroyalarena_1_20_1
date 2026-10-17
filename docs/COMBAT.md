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

## Movement

Troop mobs are NoAI mobs, and a NoAI mob never runs its travel/physics step:
setting a velocity on it does nothing and it stands still (and floats, since
gravity is part of that step). `BattleField` therefore moves each mob itself
with `Entity.move` (which also does block collisions) and applies gravity by
hand. Overlapping units are nudged apart by `combat/Separation`, capped at 60%
of the unit's walking speed so crowds keep moving; units on a bridge are not
nudged, so nobody is pushed into the water. A unit anywhere over the width of
its bridge counts as lined up to cross, and one at the bank slides sideways
instead of walking back, so crowds at the bridge mouth do not stall.

## Arrows in flight

Ranged attacks (archers and towers) fire arrows that fly at 0.8 blocks per
tick (16 blocks/s), 2 to 15 ticks depending on the distance
(`CombatEngine.flightTicks`). The damage lands when the arrow arrives
(`TickResult.landed`), as in Clash Royale: a shooter that dies after firing
still lands its arrow, and an arrow whose target died meanwhile is lost.
Melee blows land on the tick they are struck.


`BattleField` runs once per tick: copy each mob's position into the engine,
run the tick, move the mobs toward the waypoints, then play the attacks (turn
and swing; a shot sends `S2CProjectilePacket` and the clients draw a real 3D
arrow flying along a slight arc, `ProjectileRenderer`) and the hits that land
(hurt flash, hit sound, a few crit particles), copy engine health onto the mobs so health bars follow it, discard the
dead and spawn slime children. Mobs have no vanilla AI, are invulnerable to
vanilla damage and are discarded (not killed) so vanilla slime splitting never
adds to the card's own.

## Animations

NoAI mobs do not animate attacks by themselves, so `TroopAnimator` sets the
vanilla triggers from what the engine decides:

| Troop | Animation | Trigger |
|---|---|---|
| Zombie, Skeletons (bare hands), Vindicator | arms up / axe raised | `setAggressive` while it has an enemy or stands fighting |
| Skeleton Archer, Stray | bow raised, string drawn, released on the shot | aggressive pose, `startUsingItem` / `stopUsingItem` on the bow |
| Pillager | crossbow charged while it takes aim, crossbow sound on the shot | `setChargingCrossbow` and `startUsingItem` on the crossbow |
| Iron Golem | arm swing and golem attack sound; the victim is tossed up a little | entity event 4, a small upward hop of the victim |
| Hoglin, Ravager | head swing and attack sound | entity event 4 |
| Slime, Magma Cube | moves in hops and squishes on landing; bounces when it attacks | jumps every 5-8 ticks (bigger slimes jump higher), moving only while in the air, fast enough that its average speed is the engine's |
| Spider | pounces on each bite | small hop |

Troops standing to fight turn toward their enemy or tower. Hops and tosses
only change a mob's height, never the position the engine uses. Walking legs
animate on their own from the movement.

## Battle effects

All drawn on each client; the three in `royalarena-client.toml [effects]`
can be turned down or off.

| Effect | What | Where |
|---|---|---|
| Damage numbers | red "-5" over a troop, orange over a tower, green "+2" for healing, blue "Frozen"; worked out from the health the server already syncs, small burns added up | `DamageNumbers` (`damageNumbers`) |
| Team rings | a blue or red ring on the floor under every troop | `TroopEffects` (`teamRings`) |
| Tower falls | explosion, stone bricks flying, dust, a pile of slab rubble troops can step over, a crown popping up and flying into the crown counter | `ServerMatchSession.towerDestroyed`, `ArenaBuilder.rubble`, `CrownFlights` |
| Camera shake | TNT, Lightning, Iron Golem and Ravager blows, a falling tower (a King hardest) | `S2CShakePacket`, `CameraShake` (`cameraShake` 0..2) |
| Death bits | bones, slime balls, magma cream and lava, rotten flesh, string, wool, iron, leather, gunpowder, glass, emeralds | `BattleField.deathBits` |
| Full elixir | the bar throbs and glows at 10 | `BattleHudOverlay` |
| Final seconds | the last 10 s counted down big in the middle, the clock turns red; "x2 ELIXIR!", "x3 ELIXIR!", "OVERTIME!" banners; an "x2" tag on the elixir bar | `BattleHudOverlay` |
| Footstep dust | Iron Golem, Ravager and Hoglin kick up the ground they walk on | `TroopEffects` |

## Mobs with their own rules

- **Teleport (Enderman).** A ranged shot still in flight that would land on
  an Enderman, or a spell whose circle reaches it, makes it step 4.5 blocks
  straight away from the shooter (or the spell's centre) instead. It then
  waits 8 s. A melee blow is not dodged. Clients see portal particles at both
  ends and the teleport sound (`TickResult.Teleport`, `BattleField.teleportMob`).
- **Poison (Cave Spider).** Every hit that lands poisons the victim for 3 s at
  1 damage a second; a new bite replaces the poison. The damage is credited to
  the Cave Spider. Poisoned troops show pale green drips every half second.
- **Hover (Blaze).** The mob floats 1.5 blocks up and keeps its height
  (`setNoGravity`); the engine does not change.
- **Crossing the river (Blaze).** Its route goes straight to the target, over
  the water, instead of to a bridge (`CombatEngine.planUnit`).
- **Frost weakness (Blaze).** A freezing spell does 2x damage to it and
  freezes it for twice as long.
- **Water weakness (Blaze).** It takes 1.5x damage while its centre is in the
  river rows (`CombatEngine.damageUnit`).
- **Deploy on the river (Drowned).** The card may be played anywhere on the
  river rows, in addition to its own half (`ArenaLayout.canPlay`).
- **Dies on the strike (Vex).** Like the Creeper, without the blast.

- **Frost-proof (Polar Bear).** Freezing spells still hurt it but never
  freeze it, and slowing arrows do not slow it (`CombatUnit.slow`).
- **Line (Evoker).** Its hit also lands on every other enemy whose body
  touches a 1.2 wide line from where the Evoker stood, through its target, to
  its range + 1 (`CombatEngine.line`). Fangs hit at once (no flight).
- **Lock-on (Guardian).** A new target (troop or tower) restarts a 2 s charge
  before the first laser (`CombatUnit.lockOn`); the laser hits at once. From
  the river rows it hits 1.5x as hard. It may be played on the river.
- **Frost hits (Stray, Snow Golem).** A hit that slows is frost: a
  frost-weak unit (Blaze) takes double damage from it.
- **Sting (Bee).** Dies on its hit like the Vex, and the poison it leaves runs
  its full 3 s.
- **Tower diver (Phantom).** Only targets towers and flies straight over the
  river.

Hover, the river and the frost/water rules are in the engine, so they count in
the 1-versus-1 measures; the look (the floating mob, the portal burst) is
client-side only and has not been seen in the game yet.

## Area damage, burning, the bot's hand

- **Hurt flash.** Every troop that loses health, from any source (blows,
  arrows, splash, spells, burning), gets vanilla's red hurt tint, flinch and
  hurt sound (`BattleField.flashIfHurt`, a vanilla damage event). At most
  every 6 ticks per troop, and burning has to add up to half a heart, so a
  burning troop flickers rather than staying red.

- **Red flash.** Wherever area damage lands (a Creeper blast, a Witch
  potion, the Fireball, each Arrow Storm volley) the clients flash a red
  circle of the damage radius on the floor: two quick flashes fading out over
  0.6 s (`S2CAoeFlashPacket`, `AoeFlashRenderer`).
- **Burning.** A Fireball's burn is no longer vanilla fire around the whole
  mob. The server tells the clients how long a troop burns
  (`S2CBurnPacket`); they draw small flames on its body, embers popping off,
  a little smoke, and flash it red each second as the burn bites
  (`BurnEffects`).
- **The bot drags its cards.** Instead of a card appearing at once, the
  clients see a red hand carry the card's ghosts (or a spell's circle) from
  the bot's King tower to the spot, easing in and out with a little wobble,
  in 0.5 to 0.9 s depending on the distance; the card is played when it
  arrives, then the usual deploy time runs (`S2CBotDragPacket`,
  `BotDragRenderer`, `ServerMatchSession.startDrag`). Elixir is spent on
  arrival. The bot aims spells a little further ahead to allow for it. In a
  match between two players nothing of the other player's hand is shown.

## Not built yet

Flying and building-only troops, hit knockback,
tower-count scoring and overtime.
