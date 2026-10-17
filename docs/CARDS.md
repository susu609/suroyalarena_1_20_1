# Royal Arena Cards

Starter card set on the vanilla health scale: **1 mod HP = 1 vanilla HP**
(a Zombie has 20). Every card summons N copies of one vanilla mob; spells are
circular area effects. Costs come from the formula in `docs/BALANCE.md`.

Machine-readable definitions live in
`src/main/resources/data/royalarena/cards/`. The game reads the same numbers
from the `BattleCard` enum; `CombatSmokeTest` fails if a JSON file and the
enum disagree, and if a card's cost differs from `CostFormula`.

## Troops

Stats are per unit. Attack interval is in seconds (ticks in the JSON).
Move speed is in blocks per second.

| Card | Mob | Count | HP | Damage | Interval | Range | Speed | Elixir |
|---|---|---|---|---|---|---|---|---|
| Zombie | `minecraft:zombie` | 2 | 20 | 3 | 1.0 | 1.2 | 0.83 | **3** |
| Skeleton Archer | `minecraft:skeleton` (bow) | 1 | 20 | 4 | 1.0 | 6.0 | 1.0 | **2** |
| Spider | `minecraft:spider` | 4 | 10 | 2 | 1.0 | 1.2 | 1.5 | **3** |
| Skeletons | `minecraft:skeleton` (bare hands) | 3 | 11 | 1.5 | 0.9 | 1.2 | 1.33 | **2** |
| Big Slime | `minecraft:slime` | 1 | 20 | 5 | 1.4 | 1.2 | 0.85 | **3** |

Zombie and Skeleton Archer keep vanilla health. Swarm units are thinner than
vanilla on purpose: Spider x4 at vanilla 16 HP would cost 4.81 elixir, and
Skeletons x3 at vanilla 20 HP would cost 4.41.

### Big Slime split

On death a slime splits into **2** slimes with **half** the health and half the
damage. Slimes split twice, then the smallest size dies normally:

| Size | Count | HP each | Damage each | Total HP |
|---|---|---|---|---|
| Big | 1 | 20 | 5 | 20 |
| Medium | 2 | 10 | 2.5 | 20 |
| Small | 4 | 5 | 1.25 | 20 |

Each generation keeps the same total HP and total DPS. One card makes at most
7 entities.

Split rules for implementation:

- Children inherit team and lane target from the parent.
- Children spawn at the parent's death position, spread within ~1 block.
- Children skip deploy time but get ~0.3 s of invulnerability, so one spell
  pulse cannot hit both the parent and its children.
- Slimes killed by towers split normally.
- A per-team entity cap (suggested: 40) applies to children too.

## Spells

Spells deal damage by server-side area checks on a tick schedule
(`CombatEngine.castSpell`, stats in `SpellStats`). The arrows and the fireball
are drawn by the clients as real 3D models (`ProjectileRenderer`): the
fireball arcs from the caster's King tower and lands as the spell does, and
each Arrow Storm volley lands with a damage pulse. A spell can be played **anywhere on the arena**, including
the enemy half and the river. It hits enemy units whose body touches the
circle, and enemy towers whose footprint touches it for **30%** of the damage
(a hit King tower wakes up). Own troops and towers are never hit.

| Spell | Elixir | Behaviour |
|---|---|---|
| Arrow Storm | **3** | Arrows fly 0.5 s, then a circle of radius 3 blocks for 4 s; every 5 ticks (0.25 s) deals 1 damage to each enemy inside: **16 total**, 4 DPS. |
| Fireball | **4** | Travels 1 s, explodes radius 2.5 blocks for **25** damage, knocks back 1 block, burns 1 damage/s for 3 s. |
| Lightning | **6** | After 0.75 s, real (harmless) lightning bolts strike the **3** enemy troops or towers with the most health in a 3.5 block circle: **40** damage each (12 to a tower), and a 0.5 s stun that also restarts their attack. |
| Potion of Healing | **3** | The King throws a splash potion of healing (0.5 s); for 4 s, every 0.5 s it heals each **own** troop inside a 3 block circle by 2: **16 total**, never above full health, never towers. |
| Freeze | **4** | The King throws a snowball (0.5 s); every enemy troop and tower in a 3 block circle takes 4 damage and is **frozen for 4 s**: no walking, no attacking, their attack does not charge. Frozen mobs shake like in powder snow. |
| TNT | **5** | Primed TNT falls from the sky for 1.5 s, then explodes in a 3 block circle: **30** damage, knocks back 2 blocks, and **50%** (15) to towers instead of 30%. |

Burning troops are shown on fire; the burn damage comes from the engine (the
mobs themselves are invulnerable).

## Spell vs troop kill table

| | Zombie (20) | Archer (20) | Spider (10) | Skeleton (11) | Slime big (20) | Slime medium (10) | Slime small (5) |
|---|---|---|---|---|---|---|---|
| Arrow Storm (16) | survives | survives | dies | dies | survives | dies | dies |
| Fireball (25) | dies | dies | dies | dies | dies | dies | dies |

Fireball kills a big slime in one hit, but the children it splits into spawn
after the explosion and are not hit. Arrow Storm lasts 4 s, so it also clears
children that split inside the circle.

## Weapons

Armed mobs fight like their weapon (the cost formula keeps their elixir):

| Weapon | Who | Hit | Interval | Feel |
|---|---|---|---|---|
| Axe | Vindicator | 12 | 2.4 s | one heavy blow: kills a Spider, a Skeleton or a Wolf per swing, slow against swarms |
| Bare hands | Skeletons | 1.5 | 0.9 s | quick light blows |
| Bow | Skeleton Archer, Stray | 4 / 3 | 1.0 / 1.2 s | steady |
| Crossbow | Pillager | 8 | 2.0 s | slower than a bow, hits harder, longer range |

## Starter deck and extra troops

The starter deck is Zombie, Skeleton Archer, Spider, Skeletons, Big Slime,
Iron Golem, Arrow Storm and Fireball. Stray and Vindicator were fillers while
the spells did not exist; they stay as cards for later decks. Costs come from
the same formula:

| Card | Mob | Count | HP | Damage | Interval | Range | Speed | Elixir |
|---|---|---|---|---|---|---|---|---|
| Stray | `minecraft:stray` (bow) | 1 | 20 | 3 | 1.2 | 6.0 | 1.0 | **2** |

Stray arrows chill: a troop hit walks at 60% speed for 1 s (snowflakes on
it), cost x1.15.
| Vindicator | `minecraft:vindicator` (axe) | 1 | 24 | 12 | 2.4 | 1.2 | 1.1 | **2** |
| Iron Golem | `minecraft:iron_golem` | 1 | 160 | 12 | 1.6 | 1.5 | 0.9 | **5** |

The Iron Golem only targets towers (see "Tower-only troops" below).

## Library cards: Creeper, Witch, Wolves, Ravager

In the card library and the bot's random decks, not in the starter deck.
Costs follow the same formula, with two more modifiers: area damage x1.3 and
self-destruct x0.5 (`CostFormula`, balance.json).

| Card | Mob | Count | HP | Damage | Interval | Range | Speed | Trait | Elixir |
|---|---|---|---|---|---|---|---|---|---|
| Creeper | `minecraft:creeper` | 1 | 20 | 30 | 1.0 | 0.5 | 1.33 | explodes on its first hit, splash 2.5 (towers too), then dies | **3** |
| Witch | `minecraft:witch` | 1 | 26 | 8 | 1.5 | 5.0 | 0.8 | thrown splash potion, splash 1.5 | **4** |
| Wolves | `minecraft:wolf` | 3 | 8 | 3 | 0.8 | 1.2 | 1.6 | fast pack | **3** |
| Ravager | `minecraft:ravager` | 1 | 110 | 8 | 1.6 | 1.5 | 1.0 | knocks its target back 1 block | **5** |

Splash hits every other enemy whose body touches the circle around the
target, and enemy towers whose footprint does (`TroopTraits`,
`CombatEngine.splash`).

## Library cards: Hoglin, Pillager, Magma Cube

| Card | Mob | Count | HP | Damage | Interval | Range | Speed | Trait | Elixir |
|---|---|---|---|---|---|---|---|---|---|
| Hoglin | `minecraft:hoglin` | 1 | 45 | 8 | 1.4 | 1.2 | 1.6 | charges the towers, ignores troops | **3** |
| Pillager | `minecraft:pillager` (crossbow) | 1 | 30 | 8 | 2.0 | 6.5 | 1.0 | long range, charges its crossbow | **3** |
| Magma Cube | `minecraft:magma_cube` | 1 | 36 | 7 | 1.4 | 1.2 | 0.83 | splits once into 2 (18 HP, 3.5 damage) | **4** |

NoAI Hoglins never turn into Zoglins, so the arena dimension does not need to
be piglin-safe.

## Tower-only troops

Iron Golem and Hoglin (`TroopTraits.buildingsOnly`, role "Targets towers")
ignore every enemy troop and walk straight to their lane tower, like a Clash
Royale Giant or Hog Rider. Enemy troops and towers still hit them. They cannot
defend, so the cost formula gives them x0.85 and the bot never plays them to
defend.

## Rebalance: tanks reach the towers

Measured with the combat engine: one card played at its own bridge, nobody
defending, until it dies (before -> after).

| Card | Time to the tower | Health left on arrival | Damage to the tower (of 110) |
|---|---|---|---|
| Iron Golem | 13.2 s -> 9.8 s | 56% -> 85% | 56 -> 110 (tower falls at 24 s) |
| Ravager | 11.5 s -> 8.6 s | 60% -> 84% | 80 -> 110 |
| Big Slime | 13.9 s -> 11.1 s | 75% -> 55% (splits on the way) | 7 -> 28 |
| Zombie x2 | dies on the way | 35% | 0 -> 12 |
| Creeper | dies on the way | explodes on the tower | 0 -> 30 |

What changed:

- **Princess tower** 4 -> **3** damage per 0.8 s, range 7.5 -> **7.0**:
  troops come under fire about one block later, and lose a quarter less on
  the walk. The 110 HP stays.
- **Iron Golem** 4 -> **5** elixir: 160 HP (was 100), 12 damage (was 8),
  speed 0.9 (was 0.67), targets towers only.
- **Ravager** 130 HP (was 100), speed 1.0 (was 0.75), still 5 elixir (later
  110 HP and 8 damage, see the power band in `docs/BALANCE.md`).
- **Big Slime** speed 0.85 (was 0.67). **Creeper** speed 1.33 (was 1.0).

## Model size

The three Skeletons are drawn at 90% and the four Spiders at 75%
(`BattleCard.modelScale`), with smaller bodies in the engine to match
(radius 0.27 and 0.45), so the swarms bunch up more tightly.

## Current code gap

| Item | Code today | Target |
|---|---|---|
| Starting elixir | 5 (`ElixirMeter.START_TENTHS`) | 6 |
| Elixir regeneration | done: 2.8 s, 1.4 s in the last minute, 0.95 s after the first minute of overtime | |
| Match length | done: 180 s; level crowns go to 120 s of sudden-death overtime | |

## Mobs with their own rules (Enderman, Cave Spider, Vex, Ghast, Drowned, Zombified Piglin, Blaze)

Each brings one rule of its own (`TroopTraits`); the stats are on the vanilla
health scale like the rest. Elixir comes from `CostFormula`, and the band
(`PowerCreepTest`) and the bot report (`BalanceReport`) check every card.

| Card | Mob | Count | HP | Damage | Interval | Range | Speed | Rule | Elixir |
|---|---|---|---|---|---|---|---|---|---|
| Enderman | `minecraft:enderman` | 1 | 50 | 6 | 1.2 | 1.2 | 1.0 | teleports out of a ranged shot or a spell that would hit it, 4.5 blocks away, once every 8 s; a blow is not dodged | **4** |
| Cave Spider | `minecraft:cave_spider` | 4 | 5 | 1 | 1.0 | 1.2 | 1.6 | each bite poisons for 3 s at 1 a second (pale green drips); the poison counts as damage in the cost | **3** |
| Vex | `minecraft:vex` | 3 | 4 | 16 | 1.0 | 1.2 | 2.0 | dies when it strikes: one big blow and gone | **2** |
| Ghast | `minecraft:ghast` | 1 | 26 | 22 | 3.0 | 6.5 | 0.35 | slow, long-ranged fireball with a 2.5 blast; range stays under the Princess tower (7) | **5** |
| Drowned | `minecraft:drowned` | 1 | 22 | 4 | 2.0 | 5.0 | 1.0 | trident from 5 blocks; may also be played on the river | **2** |
| Zombified Piglin | `minecraft:zombified_piglin` | 2 | 26 | 4 | 1.2 | 1.2 | 0.9 | a tougher Zombie: more health and hits harder, golden sword | **3** |
| Blaze | `minecraft:blaze` | 1 | 30 | 8 | 2.0 | 6.5 | 1.0 | hovers 1.5 blocks up; crosses the river anywhere (no bridge); frost weakness: Freeze hits it twice as hard and freezes it twice as long; water weakness: 1.5x damage while on the river | **3** |

Bot-versus-bot win rates over 500 matches (random balanced decks, both sides
swapped): Ghast 67% (strong), Blaze 54%, Vex 50%, Zombified Piglin 47%,
Drowned 47%, Cave Spider 46%, Enderman 43%. The 1-versus-1 push score
(elixir ahead per fight): Cave Spider -1.14 (weakest, near the band's edge),
Enderman -0.96, Vex +0.26, Ghast +0.77, Drowned +0.13, Zombified Piglin +0.07,
Blaze +0.76.

Open points, from the numbers above:

- **Ghast is strong in bot games.** Its blast hits every enemy in the circle
  and it keeps firing as long as it lives. Options: a bigger cost factor for
  large splash radii, or a shorter splash.
- **Cave Spider** is the weakest card in the push measure. The poison pays
  only while the spider lives, and it dies to the first tower shot. More
  poison also makes the card dearer, because poison counts in the cost.
- **Enderman** dodges only the shots that would land on it, and only once
  every 8 s; in a push it loses most fights. It is safe from being overpowered.

