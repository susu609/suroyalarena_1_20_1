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
| Skeletons | `minecraft:skeleton` (melee) | 3 | 10 | 2 | 1.2 | 1.2 | 1.33 | **2** |
| Big Slime | `minecraft:slime` | 1 | 20 | 5 | 1.4 | 1.2 | 0.67 | **3** |

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

Burning troops are shown on fire; the burn damage comes from the engine (the
mobs themselves are invulnerable).

## Spell vs troop kill table

| | Zombie (20) | Archer (20) | Spider (10) | Skeleton (10) | Slime big (20) | Slime medium (10) | Slime small (5) |
|---|---|---|---|---|---|---|---|
| Arrow Storm (16) | survives | survives | dies | dies | survives | dies | dies |
| Fireball (25) | dies | dies | dies | dies | dies | dies | dies |

Fireball kills a big slime in one hit, but the children it splits into spawn
after the explosion and are not hit. Arrow Storm lasts 4 s, so it also clears
children that split inside the circle.

## Starter deck and extra troops

The starter deck is Zombie, Skeleton Archer, Spider, Skeletons, Big Slime,
Iron Golem, Arrow Storm and Fireball. Stray and Vindicator were fillers while
the spells did not exist; they stay as cards for later decks. Costs come from
the same formula:

| Card | Mob | Count | HP | Damage | Interval | Range | Speed | Elixir |
|---|---|---|---|---|---|---|---|---|
| Stray | `minecraft:stray` (bow) | 1 | 20 | 3 | 1.2 | 6.0 | 1.0 | **2** |
| Vindicator | `minecraft:vindicator` (axe) | 1 | 24 | 7 | 1.4 | 1.2 | 1.1 | **2** |
| Iron Golem | `minecraft:iron_golem` | 1 | 100 | 8 | 1.6 | 1.5 | 0.67 | **4** |

## Library cards: Creeper, Witch, Wolves, Ravager

In the card library and the bot's random decks, not in the starter deck.
Costs follow the same formula, with two more modifiers: area damage x1.3 and
self-destruct x0.5 (`CostFormula`, balance.json).

| Card | Mob | Count | HP | Damage | Interval | Range | Speed | Trait | Elixir |
|---|---|---|---|---|---|---|---|---|---|
| Creeper | `minecraft:creeper` | 1 | 20 | 30 | 1.0 | 0.5 | 1.0 | explodes on its first hit, splash 2.5 (towers too), then dies | **3** |
| Witch | `minecraft:witch` | 1 | 26 | 8 | 1.5 | 5.0 | 0.8 | thrown splash potion, splash 1.5 | **4** |
| Wolves | `minecraft:wolf` | 3 | 8 | 3 | 0.8 | 1.2 | 1.6 | fast pack | **3** |
| Ravager | `minecraft:ravager` | 1 | 100 | 10 | 1.6 | 1.5 | 0.75 | knocks its target back 1 block | **5** |

Splash hits every other enemy whose body touches the circle around the
target, and enemy towers whose footprint does (`TroopTraits`,
`CombatEngine.splash`).

## Model size

The three Skeletons are drawn at 80% and the four Spiders at 75%
(`BattleCard.modelScale`), with smaller bodies in the engine to match
(radius 0.24 and 0.45), so the swarms bunch up more tightly.

## Current code gap

| Item | Code today | Target |
|---|---|---|
| Starting elixir | 5 (`ElixirMeter.START_TENTHS`) | 6 |
| Elixir regeneration | 3.0 s per elixir (6 ticks per tenth) | 2.8 s, then 1.4 s, then ~0.95 s |
| Match length | 180 s, no overtime or tower-count win | 180 s + 120 s overtime |
