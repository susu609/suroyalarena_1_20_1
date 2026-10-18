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
| Skeleton Archer | `minecraft:skeleton` (bow) | 1 | 20 | 4 | 1.0 | 5.5 | 1.0 | **2** |
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
| Freeze | **3** | The King throws a snowball (0.5 s); every enemy troop and tower in a 3 block circle takes 8 damage and is **frozen for 4 s**: no walking, no attacking, their attack does not charge. Frozen mobs shake like in powder snow. |
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
| Creeper | `minecraft:creeper` | 1 | 20 | 30 | 1.0 | 0.5 | 1.6 | explodes on its first hit, splash 2.5 (towers too), then dies | **3** |
| Witch | `minecraft:witch` | 1 | 26 | 8 | 1.5 | 5.0 | 0.8 | thrown splash potion, splash 1.5 | **4** |
| Wolves | `minecraft:wolf` | 3 | 8 | 3 | 0.8 | 1.2 | 1.6 | fast pack | **3** |
| Ravager | `minecraft:ravager` | 1 | 100 | 8 | 1.6 | 1.5 | 1.0 | knocks its target back 1 block | **5** |

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
| Enderman | `minecraft:enderman` | 1 | 58 | 6 | 1.2 | 1.2 | 1.0 | teleports out of a ranged shot or a spell that would hit it, 4.5 blocks away, once every 8 s; a blow is not dodged | **4** |
| Cave Spider | `minecraft:cave_spider` | 4 | 6 | 2 | 1.0 | 1.2 | 1.6 | each bite poisons for 3 s at 1 a second (pale green drips); a new bite replaces the poison, so the cost counts the 1 a bite adds before the next | **3** |
| Vex | `minecraft:vex` | 3 | 4 | 16 | 1.0 | 1.2 | 2.0 | dies when it strikes: one big blow and gone | **2** |
| Ghast | `minecraft:ghast` | 1 | 28 | 22 | 3.0 | 5.5 | 0.35 | slow, long-ranged fireball with a 1.5 blast; range stays under the Princess tower (7) | **5** |
| Drowned | `minecraft:drowned` | 1 | 22 | 4 | 2.0 | 5.0 | 1.0 | trident from 5 blocks; may also be played on the river | **2** |
| Zombified Piglin | `minecraft:zombified_piglin` | 2 | 26 | 4 | 1.2 | 1.2 | 0.9 | a tougher Zombie: more health and hits harder, golden sword | **3** |
| Blaze | `minecraft:blaze` | 1 | 30 | 8 | 2.0 | 6.0 | 1.0 | hovers 1.5 blocks up; crosses the river anywhere (no bridge); frost weakness: Freeze and frost hits (Stray arrows, snowballs) hurt it twice as hard, Freeze holds it twice as long; water weakness: 1.5x damage while on the river | **3** |

Bot-versus-bot win rates and fixes: see "Tuning into 40-60%" below.

## Tuning into 40-60%

Goal: every card's deck wins 40-60% of bot-versus-bot matches
(`BalanceReport`). Before, over 1000 matches: Ghast 63%, Ravager 60%,
Freeze 24%; everything else 42-58%. Each change keeps the cost on the
formula, and was measured over 2000-4000 matches.

| Card | Change | Why | Win rate |
|---|---|---|---|
| Ghast | range 6.5 -> 6.0, blast 2.5 -> 1.5, HP 26 -> 27 (still 5) | It dealt the most damage and kills per elixir of any card. Range was the strong lever (range 5 alone: 51%); smaller shots at the same DPS changed nothing, and HP or blast size alone little. 6.0 is the Skeleton Archer's range. | 63% -> 56% |
| Ravager | HP 110 -> 100 (still 5) | Was at the high edge. | 60% -> 55% |
| Freeze | 4 -> 3 elixir, damage 4 -> 8 | A 6 s freeze won no more than 4 s, so a second of freeze now costs 2 instead of 4 in the spell formula. The bot also only counts a freeze on enemies fighting its own troops or towers (`docs/BOT.md`). | 24% -> 44% |
| Enderman | HP 50 -> 58 (still 4) | Was at the low edge. | 42% -> 48% |
| Cave Spider | HP 5 -> 6, bite 1 -> 1.5 (still 3) | The formula counted the full 3 poison per bite, but a new bite replaces the poison, so a bite adds 1. Now counted right, which left room for more. It deals the least damage per elixir of any troop. | 44% -> 43% |
| Creeper | speed 1.33 -> 1.6 (still 3) | Already counted as fast in the formula; reaching its target sooner is free. HP did nothing. | 45% -> 43% |

After (4000 matches): Skeleton Archer 58% is the highest, Cave Spider and
Creeper 43% the lowest. With the second wave (below) Pillager went back to
61%: range 6.5 -> 6.0 (still 3) brought it to 56%; Cave Spider's bite went
1.5 -> 2 (still 3). 2000 matches give each card about +/-3%, 4000 about
+/-2%.

## Second wave: Husk, Wither Skeleton, Piglin Brute, Polar Bear, Phantom, Silverfish, Bee, Evoker, Guardian

| Card | Mob | Count | HP | Damage | Interval | Range | Speed | Rule | Elixir |
|---|---|---|---|---|---|---|---|---|---|
| Husk | `minecraft:husk` | 2 | 18 | 3 | 1.0 | 1.2 | 0.83 | its hit slows for 1 s (hunger) | **3** |
| Wither Skeleton | `minecraft:wither_skeleton` | 1 | 38 | 10 | 1.6 | 1.5 | 1.0 | stone sword; withers: 1.5 a second for 3 s (black dust) | **3** |
| Piglin Brute | `minecraft:piglin_brute` | 1 | 60 | 14 | 1.8 | 1.2 | 1.0 | golden axe: big, slow blows | **4** |
| Polar Bear | `minecraft:polar_bear` | 1 | 76 | 7 | 1.2 | 1.2 | 1.0 | frost-proof: Freeze only hurts it, Stray arrows do not slow it | **4** |
| Phantom | `minecraft:phantom` | 1 | 34 | 6 | 1.0 | 1.2 | 2.0 | flies 2 blocks up straight over the river and only goes for towers | **3** |
| Silverfish | `minecraft:silverfish` | 5 | 4 | 1 | 0.8 | 1.2 | 1.6 | a cheap swarm | **2** |
| Bee | `minecraft:bee` | 5 | 4 | 3 | 1.0 | 1.2 | 2.0 | flies 1 block up; stings once, poisons 1.5 a second for 3 s, and dies | **2** |
| Evoker | `minecraft:evoker` | 1 | 30 | 10 | 2.0 | 5.0 | 0.8 | fangs bite every enemy on a 1.2 wide line from it through its target | **4** |
| Guardian | `minecraft:guardian` | 1 | 46 | 14 | 2.0 | 5.5 | 0.6 | laser: locks on 2 s before hitting a new target; may be played on the river and hits 1.5x from there; swims 0.6 up | **4** |
| Zombie Villager | `minecraft:zombie_villager` | 5 | 9 | 2 | 1.2 | 1.2 | 0.8 | a big, weak swarm: each one is far weaker than a Zombie, five of them together are not (model 0.85) | **3** |
| Snow Golem | `minecraft:snow_golem` | 1 | 40 | 1 | 1.0 | 5.0 | 1.0 | snowballs that barely hurt but slow for 1.5 s; frost: double damage to a Blaze; leaves its vanilla snow trail (cleared with the arena) | **2** |

Each cost is the formula's. New modifiers (docs/BALANCE.md): frost-proof
x1.05, line damage x1.25, lock-on x0.85, stronger on the river x1.05; a unit
that dies on its hit counts all of its poison.

Tuning (bot matches, 4000): Polar Bear HP 85 -> 76 (63% -> 57%), Wither
Skeleton HP 44 -> 38 (61% -> 56%), Silverfish 6 -> 5 bodies (60% -> 53%),
Guardian HP 40 -> 46 (42% -> 46%). Phantom started as a pair of 4-elixir
fighters at 38%; as one 3-elixir flyer that only hits towers it is at 44%,
and it passes the tower-only band (`PowerCreepTest`). Bee is at the low edge
(41%): its cost is at the top of 2 elixir, so a buff would make it 3.

After (4000 matches, 37 cards): every card between 41% and 58%; Polar Bear
and Skeleton Archer the highest, Bee the lowest.

## Nether mobs do not burn

Every Nether mob is fire immune: Fireball still takes its damage off, but
nothing is set alight (`BattleCard.fireImmune()`, `"fire_immune"` in the JSON,
`CombatEngine.applySpellHits`). Blaze, Ghast, Magma Cube, Zombified Piglin,
Hoglin, Wither Skeleton and Piglin Brute have it; every future Nether card
must. Burn was never part of the cost, so costs did not change; 8000 bot
matches stayed in the 42-58% band.

## Tactics (Attack, Defense, Support)

Each card's JSON has a hand-written `"roles"` list of `attack`, `defense`,
`support`; `BattleCard.tactics()` holds the same (a test checks they match,
so change both). The deck screen filters by them. Current choice:

| Tactic | Cards |
|---|---|
| Attack | Spider, Big Slime, Iron Golem, Creeper, Wolves, Ravager, Hoglin, Magma Cube, Enderman, Drowned, Blaze, Wither Skeleton, Piglin Brute, Polar Bear, Phantom, Fireball, Lightning, Freeze, TNT |
| Defense | Zombie, Skeleton Archer, Skeletons, Big Slime, Stray, Vindicator, Creeper, Witch, Pillager, Magma Cube, Cave Spider, Vex, Ghast, Zombified Piglin, Husk, Wither Skeleton, Polar Bear, Silverfish, Bee, Evoker, Guardian, Arrow Storm, Fireball, TNT |
| Support | Skeleton Archer, Stray, Witch, Pillager, Ghast, Drowned, Blaze, Bee, Evoker, Arrow Storm, Potion of Healing, Freeze |

Autopilot cycle 1 (4000 bot matches): Snow Golem added at 51%. Skeleton
Archer range 6.0 -> 5.5 (60% -> 58%), Ghast range 6.0 -> 5.5 and HP 27 -> 28
(59% -> 57%), Blaze range 6.5 -> 6.0 (59% -> under 55%); costs unchanged.
Every card 42-58%.

Autopilot cycle 3 (4000 bot matches): Zombie Villager added at 49-51%.
Bee sits at 40%, under the 42% line. Tried: HP 4 -> 4.4 (38%), poison
1.5 -> 1.75 (40%), 3 elixir with HP 5 (32%, worse: the extra elixir costs
more than the stats give). At 2 elixir the formula leaves no room (raw 2.37,
the limit is 2.49), so it stays; if it should be stronger, make the sting
hit towers too or let a Bee survive its sting.

