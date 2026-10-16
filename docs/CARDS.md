# Royal Arena Cards

Starter card set on the vanilla health scale: **1 mod HP = 1 vanilla HP**
(a Zombie has 20). Every card summons N copies of one vanilla mob; spells are
circular area effects. Costs come from the formula in `docs/BALANCE.md`.

Machine-readable definitions live in
`src/main/resources/data/royalarena/cards/`. Nothing loads them yet; the
current `BattleCard` enum still drives the game (see "Current code gap").

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

Spells deal damage by server-side area checks on a tick schedule. Arrow and
fireball entities are visual only. Spells deal **30%** damage to towers.

| Spell | Elixir | Behaviour |
|---|---|---|
| Arrow Storm | **3** | Circle radius 3 blocks for 4 s; every 5 ticks (0.25 s) deals 1 damage to each enemy inside: **16 total**, 4 DPS. |
| Fireball | **4** | Travels 1 s, explodes radius 2.5 blocks for **25** damage, knocks back 1 block, burns 1 damage/s for 3 s. |

## Spell vs troop kill table

| | Zombie (20) | Archer (20) | Spider (10) | Skeleton (10) | Slime big (20) | Slime medium (10) | Slime small (5) |
|---|---|---|---|---|---|---|---|
| Arrow Storm (16) | survives | survives | dies | dies | survives | dies | dies |
| Fireball (25) | dies | dies | dies | dies | dies | dies | dies |

Fireball kills a big slime in one hit, but the children it splits into spawn
after the explosion and are not hit. Arrow Storm lasts 4 s, so it also clears
children that split inside the circle.

## Current code gap

These values are the target design. The code still uses the Part 7
placeholders, to be replaced in a later part:

| Item | Code today | Target |
|---|---|---|
| Cards | `BattleCard` enum: Zombie, Skeleton, Spider, Slime, Wolf, Blaze, Vindicator, Iron Golem (single unit, `towerDamage` only) | Definitions above |
| Princess / King tower HP | 1400 / 2400 (`MatchInstance`) | 110 / 185 |
| Starting elixir | 5 (`ElixirMeter.START_TENTHS`) | 6 |
| Elixir regeneration | 3.0 s per elixir (6 ticks per tenth) | 2.8 s, then 1.4 s, then ~0.95 s |
| Match length | 180 s, no overtime | 180 s + 120 s overtime |

The starter deck needs 8 cards; this set has 7. The 8th card is not chosen
yet.
