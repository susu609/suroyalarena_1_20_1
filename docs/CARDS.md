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
| Skeleton Archer | `minecraft:skeleton` (bow) | 1 | 20 | 4 | 1.0 | 4.5 | 1.0 | **2** |
| Spider | `minecraft:spider` | 3 | 16 | 2 | 1.0 | 1.2 | 1.5 | **3** |
| Skeletons | `minecraft:skeleton` (bare hands) | 2 | 20 | 1.5 | 0.9 | 1.2 | 1.33 | **2** |
| Big Slime | `minecraft:slime` | 1 | 36 | 5 | 1.4 | 1.2 | 0.75 | **4** |

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
are drawn by the clients as real 3D models (`ProjectileRenderer`). Thrown
spells (Arrow Storm, Fireball, the potions, Freeze, Cobweb, Snowball Volley)
fly from the caster's **King tower** to the spot, so like in Clash Royale a
far target is reached later: flight = distance from the King / the spell's
flight speed (blocks a second, `flight_speed` in the card JSON), at least 4
ticks (`SpellStats.flightTicks`, `ArenaLayout.deployTicks`). TNT falls from
the sky and Lightning strikes, so they keep a fixed delay (`travel_ticks`).
Each Arrow Storm volley is shot from the King and lands with its damage
pulse. A spell can be played **anywhere on the arena**, including
the enemy half and the river. It hits enemy units whose body touches the
circle, and enemy towers whose footprint touches it for **30%** of the damage
(a hit King tower wakes up). Own troops and towers are never hit.

| Spell | Elixir | Behaviour |
|---|---|---|
| Arrow Storm | **3** | Arrows fly from the King at 24 blocks/s, then **3 volleys** 0.4 s apart on a circle of radius 2.3 blocks: 7 damage each to every enemy inside, **21 total** in 0.8 s; **50%** to towers. |
| Fireball | **4** | Travels 1 s, explodes radius 2.5 blocks for **25** damage, knocks back 1 block, burns 1.5 damage/s for 3 s. |
| Lightning | **6** | After 0.75 s, real (harmless) lightning bolts strike the **3** enemy troops or towers with the most health in a 3.5 block circle: **40** damage each (18 to a tower, 45%), and a 0.5 s stun that also restarts their attack. |
| Potion of Healing | **3** | The King throws a splash potion of healing (0.3 s); for 4 s, every 0.5 s it heals each **own** troop inside a 3 block circle by 2: **16 total**, never above full health, never towers. |
| Freeze | **3** | The King throws a snowball (0.25 s); every enemy troop and tower in a 3 block circle takes 8 damage (6 to a tower, 80%) and is **frozen for 4 s**: no walking, no attacking, their attack does not charge. Frozen mobs shake like in powder snow. |
| TNT | **5** | Primed TNT falls from the sky for 1.2 s, then explodes in a 3 block circle: **30** damage, knocks back 2 blocks, and **50%** (15) to towers instead of 30%. It can be cast only on your own half and up to the middle of the bridge, never on the enemy half. |
| Poison Cloud | **2** | The King throws a lingering potion (0.25 s); a 3.8 block cloud stays 3 s and poisons every enemy troop in it 2 a second: **6 total**, 1.5x on the living (9), as it is on the undead, nothing on golems. Never hurts towers. |
| Cobweb | **2** | The King throws a web (0.4 s); for 5 s every enemy troop in a 3.5 block circle walks 60% slower and attacks at half speed, and keeps it 1.75 s after leaving. No damage. Frost-proof troops (Polar Bear) ignore it. |
| Swiftness Potion | **2** | The King throws a splash potion of swiftness (0.25 s); every **own** troop in a 3.5 block circle walks 50% and attacks 75% faster for 6 s. |
| Snowball Volley | **2** | A burst of snowballs (0.4 s) on a 2.5 block circle: **6** damage, knocks back 1.5 blocks, and webs the troops for 1.5 s (60% slower walk, half-speed attack). 30% to towers. |
| Zap | **2** | Falls after 0.25 s: every enemy troop and tower in a 2.5 block circle takes **6** damage (30% to a tower) and is **shocked for 0.5 s**: it stops walking, and its attack timer restarts, so a swing about to land starts over. No target limit (Lightning hits 3). |
| Milk Bucket | **1** | The King throws a splash of milk (16 blocks/s): every unit in a 3 block circle, **both sides**, loses every effect, good and bad (poison, burning, slow, web, freeze, stun, haste), and the towers in it thaw. No damage. |

Burning troops are shown on fire; the burn damage comes from the engine (the
mobs themselves are invulnerable).

## Spell vs troop kill table

| | Zombie (20, armour 2) | Archer (20) | Spider (16) | Skeleton (20) | Slime big (36) | Slime medium (18) | Slime small (9) |
|---|---|---|---|---|---|---|---|
| Arrow Storm (21) | survives (19.3 taken) | dies | dies | dies | survives | dies | dies |
| Fireball (25) | dies | dies | dies | dies | survives | dies | dies |

Arrow Storm's volleys come 0.4 s apart, so children a slime splits into
inside the circle are still hit by the volleys left.

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
| Iron Golem | `minecraft:iron_golem` | 1 | 100 (armour 4) | 15 | 2.75 | 1.5 | 0.9 | **5** |

The Iron Golem is a slow-hitting tank that fights whatever is in front of
it: 4 armour (16% off every hit; poison and fire go through), one heavy blow
every 2.75 s. Until Part 64 it only targeted towers and two of its pushes
took a tower whatever was in the way.

## Library cards: Creeper, Witch, Wolves, Ravager

In the card library and the bot's random decks, not in the starter deck.
Costs follow the same formula, with two more modifiers: area damage x1.3 and
self-destruct x0.5 (`CostFormula`, balance.json).

| Card | Mob | Count | HP | Damage | Interval | Range | Speed | Trait | Elixir |
|---|---|---|---|---|---|---|---|---|---|
| Creeper | `minecraft:creeper` | 1 | 20 | 30 | 1.0 | 0.5 | 1.8 | explodes on its first hit, splash 3.0 (towers too), then dies | **3** |
| Witch | `minecraft:witch` | 1 | 26 | 8 | 1.5 | 5.0 | 0.8 | thrown splash potion, splash 1.5 | **4** |
| Wolves | `minecraft:wolf` | 3 | 8 | 3 | 0.8 | 1.2 | 1.9 | fast pack | **3** |
| Ravager | `minecraft:ravager` | 1 | 100 | 8 | 1.6 | 1.5 | 1.0 | knocks its target back 1 block | **5** |

Splash hits every other enemy whose body touches the circle around the
target, and enemy towers whose footprint does (`TroopTraits`,
`CombatEngine.splash`).

## Library cards: Hoglin, Pillager, Magma Cube

| Card | Mob | Count | HP | Damage | Interval | Range | Speed | Trait | Elixir |
|---|---|---|---|---|---|---|---|---|---|
| Hoglin | `minecraft:hoglin` | 1 | 40 | 10 | 1.4 | 1.2 | 1.6 | charges the towers, ignores troops | **3** |
| Pillager | `minecraft:pillager` (crossbow) | 1 | 24 | 11 | 2.0 | 6.0 | 1.0 | long range, charges its crossbow | **3** |
| Magma Cube | `minecraft:magma_cube` | 1 | 16 | 7 | 1.4 | 1.2 | 0.83 | vanilla armour 12 (3 x size 4: -48% from hits); splits once into 2 (8 HP, 3.5 damage, same armour) | **3** |

NoAI Hoglins never turn into Zoglins, so the arena dimension does not need to
be piglin-safe.

## Tower-only troops

The Hoglin (`TroopTraits.buildingsOnly`, role "Targets towers")
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
| Enderman | `minecraft:enderman` | 1 | 40 | 7 | 1.2 | 1.2 | 1.0 | teleports out of a ranged shot or a spell that would hit it, 4.5 blocks away, once every 5 s; a blow is not dodged | **3** |
| Cave Spider | `minecraft:cave_spider` | 3 | 12 | 2 | 1.0 | 1.2 | 1.6 | each bite poisons for 3 s at 1 a second (pale green drips); a new bite replaces the poison, so the cost counts the 1 a bite adds before the next | **3** |
| Vex | `minecraft:vex` | 2 | 14 | 10 | 1.0 | 1.2 | 2.0 | dies when it strikes: one big blow and gone | **2** |
| Ghast | `minecraft:ghast` | 1 | 10 | 22 | 3.0 | 5.0 | 0.3 | slow, long-ranged fireball with a 1.5 blast; range stays under the Princess tower (7) | **3** |
| Drowned | `minecraft:drowned` | 1 | 20 | 4 | 2.0 | 5.0 | 1.0 | trident from 5 blocks; may also be played on the river | **2** |
| Zombified Piglin | `minecraft:zombified_piglin` | 2 | 20 | 4 | 1.2 | 1.2 | 0.9 | a tougher Zombie: more health and hits harder, golden sword | **3** |
| Blaze | `minecraft:blaze` | 1 | 20 | 8 | 2.0 | 4.5 | 1.0 | hovers 1.5 blocks up; crosses the river anywhere (no bridge); frost weakness: Freeze and frost hits (Stray arrows, snowballs) hurt it twice as hard, Freeze holds it twice as long; water weakness: 1.5x damage while on the river | **2** |

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
| Ghast (again) | HP 28 -> 33, damage 22 -> 19 (still 5) | Back at 59-60% after later changes; the HP-damage swap keeps the cost and makes it a bit easier to kill. | 60% -> 57% |
| Ghast, Skeleton Archer | range 5.5 -> 5.0 (still 5 and 2) | The user's pick: the Princess tower (7) now outranges them by 2. The Spider Jockey's rider shoots 5.0 too. | 60% -> 55%, 59% -> 55% |
| Husk | 2 x 18 HP, 3 dmg -> 3 x 13 HP, 2.5 dmg; slow 1 s -> 2 s; speed 0.83 -> 1.0 (still 3) | The user's pick (more, smaller bodies). The body swap alone moved it 41% -> 42%; the longer slow and walking speed (both formula-neutral) did the rest. | 41% -> 45% |
| Hoglin | HP 45 -> 50, damage 8 -> 10 (still 3, raw 3.27) | Slowing it to 1.33 made it arrive under 60% health (`tanksReachTheTowerHealthy`), so it keeps 1.6 and gets the room the formula had. | 43% -> 44% |
| Enderman | HP 58 -> 68, teleport every 8 s -> 5 s (still 4) | The formula left room (raw 3.78 -> 4.09); the cooldown is formula-neutral. | 43% -> 50% |
| Evoker | range 5.0 -> 4.5 (still 4) | Rose to 59% once the archers lost range. | 59% -> 56% |
| Creeper (again) | speed 1.6 -> 1.8, blast 2.5 -> 3.0, HP 20 -> 23 (still 3, raw 3.48) | At 40% once the bots got personalities; speed past 1.33 and splash size are free in the formula, the HP used the room left. | 40% -> 43-45% |
| Skeleton Archer (again) | range 5.0 -> 4.5 (still 2) | At 59-60% after Fox joined; slower walking did nothing. The Spider Jockey's rider shoots 4.5 too. | 60% -> 56% |
| Skeleton Rider | started at 24 HP / 6 damage / range 4.5 / speed 1.8: 10 tower damage per elixir, under the tower-only band (12); 26 / 8 / 5.5 passes it; then 60% in bot matches, so speed 1.8 -> 1.4 (free above 1.33) | Autopilot cycle 5. | 60% -> 57% |
| Wither Skeleton | speed 1.0 -> 0.85 (still 3) | Reached 59-60%; walking speed under 1.33 is free. Cave Spider (41%) did not move with 1.6 -> 1.9, reverted. | 60% -> 58% |
| Spells (free buffs) | Arrow Storm flies 0.3 s (was 0.5) and does 50% to towers (30%); Fireball burns 1.5/s (1); Lightning 45% to towers (30%); Healing flies 0.3 s (0.5); Freeze flies 0.25 s (0.5) and does 80% to towers (30%); TNT falls 1.2 s (1.5). Costs unchanged | Flight time, tower share and burn are not in the spell formula. | Freeze 43% -> 50%, Arrow Storm 45% -> 48%; the rest 48-50% |
| Potion of Healing | no stat change: the bot heals only troops that are fighting and at 70% health or less (`docs/BOT.md`) | It was thrown on troops walking in the back. | 43% -> 45% |
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
| Husk | `minecraft:husk` | 2 | 20 | 2.5 | 1.0 | 1.2 | 1.0 | its hit slows for 2 s (hunger) | **3** |
| Wither Skeleton | `minecraft:wither_skeleton` | 1 | 20 | 10 | 1.6 | 1.5 | 0.85 | stone sword; withers: 1.5 a second for 3 s (black dust) | **2** |
| Piglin Brute | `minecraft:piglin_brute` | 1 | 50 | 14 | 1.8 | 1.2 | 0.8 | golden axe: big, slow blows | **4** |
| Polar Bear | `minecraft:polar_bear` | 1 | 30 | 7 | 1.2 | 1.2 | 1.2 | frost-proof: Freeze only hurts it, Stray arrows do not slow it | **3** |
| Phantom | `minecraft:phantom` | 1 | 20 | 7 | 1.0 | 1.2 | 3.0 | flies 2 blocks up straight over the river and only goes for towers | **2** |
| Silverfish | `minecraft:silverfish` | 4 | 8 | 1 | 0.8 | 1.2 | 1.6 | a cheap swarm | **2** |
| Bee | `minecraft:bee` | 3 | 10 | 0.9 | 1.0 | 1.2 | 2.0 | flies 1 block up; stings once a second, each sting poisons 1 a second for 3 s; turns angry (vanilla angry face) on its first sting | **2** |
| Evoker | `minecraft:evoker` | 1 | 24 | 10 | 2.0 | 4.0 | 0.5 | fangs bite every enemy on a 1.2 wide line from it through its target | **3** |
| Guardian | `minecraft:guardian` | 1 | 30 | 14 | 2.0 | 5.5 | 0.6 | laser: locks on 2 s before hitting a new target; may be played on the river and hits 1.5x from there; swims 0.6 up | **3** |
| Zombie Villager | `minecraft:zombie_villager` | 4 | 20 | 1 | 1.2 | 1.2 | 0.8 | a big, weak-hitting swarm: vanilla zombie health and armour 2, but 1 damage a blow (model 0.85) | **3** |
| Spider Jockey | `minecraft:spider` | 1 | 16 | 3 | 1.0 | 4.5 | 1.4 | one unit: a Skeleton on a Spider (model 0.9), shoots from the saddle, and when the spider dies the Skeleton stays as a Skeleton Archer (20 health, range 5.5) | **3** |
| Skeleton Rider | `minecraft:skeleton_horse` | 1 | 15 | 6 | 1.0 | 6.0 | 1.4 | a Skeleton on a skeleton horse (vanilla 15 HP) that only shoots towers; when the horse dies the Skeleton stays as a Skeleton Archer (20 HP) that fights anything (model 1.0) | **3** |
| Strider | `minecraft:strider` | 1 | 20 | 3 | 1.0 | 1.2 | 1.7 | Nether, fire immune, but walks the bridges like everyone; leaves a burning trail (a patch every 0.5 s that lasts 2 s): an enemy standing on it burns 1 a second, and a moment after (model 1.0) | **2** |
| Goat | `minecraft:goat` | 1 | 10 | 5 | 2.0 | 1.2 | 1.3 | a ram: its slow head-butt knocks the target back 3 blocks, three times the Ravager's push; vanilla health, so fragile; 1 elixir (model 1.0) | **1** |
| Llama | `minecraft:llama` | 1 | 22 | 3 | 1.5 | 4.5 | 0.9 | spits from 4.5 blocks; the spit slows for 0.75 s, less than a Stray's arrow; vanilla health 15-30, the middle (model 1.0) | **2** |
| Piglin | `minecraft:piglin` (crossbow) | 1 | 16 | 5 | 3.0 | 5.0 | 1.0 | Nether, fire immune; fires 2 quick bolts 0.4 s apart, then reloads for the rest of its 3 s (the cost counts both bolts) (model 1.0) | **2** |
| Fox | `minecraft:fox` | 2 | 10 | 2 | 0.7 | 1.2 | 1.9 | a fast small pack with quick bites, the fastest walker; nothing special (no armour piercing) | **2** |
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

Now (4000 matches, 40 cards, bots with personalities, after the range and
body tuning): every card between 40% and 58%. Highest Skeleton Archer 58%,
Polar Bear and Pillager 57%; lowest Creeper 40%, Cave Spider and Freeze 43%.
Matches last 267 s on average.

## Poison and what a troop is made of

`BattleCard.body()` (and `"body"` in every troop JSON; a test keeps them
equal) puts each troop in one of three groups: **living** (1.5x poison),
**undead or spirit** (Zombie, Zombie Villager, Zombified Piglin, Husk,
Drowned, Wither Skeleton, Skeleton, Skeleton Archer, Stray, Phantom, Blaze,
Vex: 1x) and **construct** (Iron Golem, Snow Golem: immune). The cost formula
still counts poison at 1x, as the average over the roster is close to that.
8000 bot matches after the change: every card 40-59% (Ghast 59%, Bee 40%,
unchanged by it). Undead being unaffected, as in vanilla, was left out on
purpose; say if you want them immune too.

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

By hand, after that: the Bee survives its sting and stings once a second
(sting 1, poison 1 a second, the user's design): 41% -> 57%. Big Slime gets
36 health (it is a size-4 slime, the biggest vanilla one; children 18 and 9):
at 3 elixir with 4 damage it won 71%, so it costs 4 now (raw 3.64), walks
0.75 instead of 0.85 (formula-neutral below 1.33): 56%.

## Vanilla health and armour (Part 55)

Every mob card's unit health is its mob's vanilla max health, so the cards
keep Minecraft's proportions: a Zombie 20, a Piglin Brute 50 (2.5 Zombies),
an Iron Golem or Ravager 100. Swarm cards lost bodies instead of each body
being weaker (Spider 3 x 16, Cave Spider 3 x 12, Husk 2 x 20, Skeletons
2 x 20, Fox 2 x 10, Vex 2 x 14, Silverfish 4 x 8, Bee 3 x 10, Zombie
Villager 4 x 20). Two exceptions keep their health: Big Slime 36 (picked
for its 4x size) and Snow Golem 40 (4 would not last a second).
`scripts/card-library/vanilla.json` has every mob's vanilla numbers.

Armour (docs/COMBAT.md) is vanilla too: the zombie family 2 points, the
Magma Cube 12, and the Piglin Brute a golden helmet and chestplate (7) that
it wears. The cost formula counts health / (1 - 0.04 x armour).

Each cost was taken again from the formula; the tuning to stay in 40-60%
(4000 bot matches):

| Card | Change | Why | Win rate |
|---|---|---|---|
| Iron Golem | damage 12 -> 14 (vanilla 7-21), 5 elixir | At 100 HP and 4 elixir it took a whole tower alone (27.5 tower damage per elixir, band 12-25). | 22 per elixir, 43% |
| Iron Golem | fights every target; armour 4; 15 damage every 2.75 s (was 14 every 1.6 s, towers only); 5 elixir | Defenders could not stop it: two pushes took a tower. 18 every 2.5 s (the first try) came out at +1.46 push score, over the 1.1 cap. | push score +0.98, 55% |
| Phantom | speed 2.0 -> 3.0 (free above 1.33), damage 7 | At 20 HP it fell before the tower: 9 tower damage per elixir. | 14 per elixir, 52% |
| Skeleton Rider | horse 15 HP that leaves its Skeleton Archer (as the Spider Jockey), damage 6, range 6.0 | 15 HP alone gave 8 tower damage per elixir. | 13 per elixir, 56% |
| Evoker | range 4.5 -> 4.0, speed 0.8 -> 0.7 (3 elixir) | 63% at 3 elixir. | 63% -> 57% |
| Blaze | range 6.0 -> 4.5 (2 elixir) | 63% at 2 elixir. | 63% -> 56% |
| Zombie Villager | 5 -> 4 (still 3) | 61%: five vanilla-health zombies were too much. | 61% -> 55% |
| Bee | damage 1 -> 0.9, 3 -> 2 elixir | 41% at 3 elixir. | 41% -> 52% |
| Poison Cloud | 6 -> 3 s (6 total), 3 -> 2 elixir, flies 0.25 s | 39%: swarm bodies now have more health. | 39% -> 42% |
| Wolves | speed 1.6 -> 1.9 (free) | 41%. | 41% -> 44% |

Band after: 41-58% (Snowball Volley 41%, Swiftness Potion 41-43%: within the
2 points of noise).

## Tribes (Part 56)

Every troop belongs to one or two tribes (Zombie, Skeleton, Illager,
Nether, Arthropod, Beast, Night, Golem, Slime, Aquatic); 2, 4, 6 and 8
different cards of a tribe in a deck give its troops bonuses. The members,
steps and how they were measured are in docs/TRIBES.md. A new card needs
its tribes in `BattleCard.tribes()` (one or two).

Autopilot cycle 6 (4000 bot matches): Strider added at 43-45% (Nether and
Beast tribes; fire trail x1.15 in the formula). Evoker speed 0.7 -> 0.6
(60% -> 56%), Ghast speed 0.35 -> 0.3 (59% -> 57%), Snowball Volley flies
0.6 -> 0.4 s (40% -> 42%); costs unchanged. Pillager 58-60% and Vex 40-41%
are at the edges for the next cycle.

Autopilot cycle 7 (4000 bot matches): Goat added at 50-52% (Beast tribe,
1 elixir: at vanilla health the formula gives 1.02). Piglin Brute speed
1.0 -> 0.9 (61% -> 58%). Tried and reverted: Pillager range 6.0 -> 5.5
(61% -> 61%) and Cobweb flight 0.4 -> 0.25 s (39% -> 39%); Pillager needs
another lever (its 11 damage), Cobweb is limited by how the bot casts it.

Autopilot cycle 8 (4000 bot matches): Llama added at 44-45% (Beast tribe,
2 elixir). Evoker speed 0.6 -> 0.5 (60% -> 58%), Polar Bear speed 1.0 ->
1.2 (41% -> 42%). Pillager speed 1.0 -> 0.85 did nothing (61% -> 62%),
reverted. Pillager has now resisted range and speed: it needs a person's
call (an HP-damage swap away from its vanilla 24 health, or 4 elixir).

Autopilot cycle 9 (4000 bot matches): Piglin added at 49-50% (Nether
tribe; new burst trait: 2 shots, then the reload). Piglin Brute speed 0.9
-> 0.8 (60% -> 57%). Strider speed 1.7 -> 2.0 did nothing (41%) and broke
"the Fox is the fastest walker", reverted. Still waiting on the user:
Pillager 62-63%; Cobweb and Swiftness Potion 39% are limited by the bot.

Autopilot cycle 10 (4000 bot matches, no card left in the queue but the
"(by hand)" ones): tried a lower bar for the bot's no-damage spells
(1.5x -> 1.2x value) and Snowball Volley knockback 1.5 -> 2.0; Cobweb,
Swiftness Potion and Snowball Volley stayed at 39%, both reverted. The
three cheap support spells seem to lose to a troop in the same slot in bot
matches; they need a person's call (redesign, or a support role the bot
plays differently). Pillager 62% still waits for the user.

## Batch 1 of vanilla-behaviour cards: Cat, Frog, Piglets, Milk Bucket

Each does what its mob does in vanilla rather than copying a Clash Royale
role (`MobsWave3SmokeTest`):

| Card | Elixir | Stats | Rule |
|---|---|---|---|
| Cat | 1 | 10 HP, 3 damage a second, range 1.0, speed 1.3, Beast | **Creepers and Phantoms will not come within 4 blocks** of an enemy Cat (vanilla: they flee cats): inside, they drop their target and back away instead of attacking (`CombatEngine.scaringCat`). |
| Frog | 1 | 10 HP, 3 damage every 1.2 s, range 2.0 (its tongue), speed 1.0, Aquatic | **Swallows the smallest slimes and magma cubes whole**: a lick kills a Big Slime or Magma Cube that can split no more (`swallows`); anything else only takes the lick. |
| Piglets | 3 | 4 small pigs (scale 0.6), 10 HP each, 4 damage every 1.5 s, speed 1.8, towers only, Beast | A fast swarm that runs for the towers; splash damage clears it. Three pigs at 2 elixir fell to the tower before doing anything (7.5 tower damage per elixir, band 12-25), so four at 3 (16 per elixir). |
| Milk Bucket | 1 | spell, radius 3 | See the spell table: clears everything on both sides, so it frees your troops from a Freeze, a web or a burn but also takes your Swiftness off. Its value (0.8 per unit) is in the spell formula. |

Win rates (4000 bot matches): Frog 52%, Milk Bucket 51%, Cat 48%,
Piglets 48%.

## Batch 2 of vanilla-behaviour cards

(`MobsWave4SmokeTest`; win rates from 4000 bot matches.)

| Card | Elixir | Stats | Rule | Win rate |
|---|---|---|---|---|
| Zoglin | 3 | 40 HP, 8 damage every 1.4 s, knockback 1, speed 1.4, towers only, fire immune, undead; Zombie and Nether | **Rage**: each swing comes up to 60% sooner as it loses health (full rage at a quarter of its health or less). | 48% |
| Killer Rabbits | 2 | 3 rabbits (vanilla's killer bunny), 3 HP and **8 armour** each, 4 damage a second, speed 1.8; Beast | A fast, hard-biting swarm that splash and spells wipe out. | 52% |
| Chicken Jockey | 2 | a chicken (4 HP, speed 1.8) carrying a baby zombie; Zombie | **Two lives**: when the chicken falls, the baby zombie (a Zombie, 20 HP, armour 2) fights on. | 50% |
| Panda | 2 | 20 HP, 6 damage every 1.5 s, speed 0.8; Beast | **Sneeze** every 4 s: small enemies (body radius under 0.5: Fox, Spider, Silverfish, the Skeletons...) within 1.5 blocks are blown 1.5 blocks back. | 41% |
| Turtle | 1 | 30 HP, **10 armour** (its shell), a 1-damage nip every 2 s, speed 0.5; Aquatic | **Swims**: three times as fast on the river. A cheap shield that barely fights. | 56% |
| Camel | 2 | 32 HP, 2.5 damage a second (its Husk rider), range 1.5, speed 1.0; Beast | **Dash**: its first blow hits twice as hard and knocks the target 1.5 blocks back. When it falls, the Husk fights on. | 58% |

A rider's damage (the Camel's Husk, the Chicken Jockey's zombie, the Spider
Jockey's archer) counts for the card that was played in the match stats,
not for the rider's own card.

## Batch 3: buildings and the Warden

**Buildings** (`BattleCard.isBuilding`) stand still, lose their health over
30 s, and troops that only hit towers (Hoglin, Piglets, Zoglin...) go for
the nearest one they see first, as in Clash Royale. They deploy on the own
half like troops. `PowerCreepTest` measures them on defence only; the bot
plays them to defend (they pull tower-only troops) and otherwise only when
its elixir is full.

| Card | Elixir | Stats | Rule | Win rate |
|---|---|---|---|---|
| Shulker | 2 | building, 30 HP, 3 damage every 2 s, range 6; End | Its homing bullets **lift** a ground troop for 0.8 s (no walking, no attacking). **Shell**: shut, it takes 60% of a hit; it opens for 0.75 s each time it fires. | 44% |
| End Crystal | 2 | building, 10 HP; End | Never attacks. When broken it **blows up**: 40 damage to every troop of **both sides** within 3 blocks. A trap. | 34% (weak: the bot seldom baits with it) |
| Warden | 8 | 150 HP, no armour, 15 damage every 2.75 s (about an armourless Iron Golem), speed 0.8; Night | **Blind**: walks for the towers and turns only on enemies that attacked in the last 2 s within 8 blocks (silent troops and buildings cannot distract it). **Sonic Boom** every 15 s: 20 damage through armour on its target (troop or tower) within 6 blocks. Its 8 elixir comes from these abilities (see docs/BALANCE.md). | 46% |

A 1-elixir End Crystal (24 damage) won 47% but its defence score went
over the band (+1.21), so it is 2 elixir with a bigger blast.

Villager is not made yet: each profession will be its own card later.

## Batch 4: Illusioner, Axolotl, Endermite, Allay, Magma Cube, three spells

| Card | Elixir | Stats | Rule | Win rate |
|---|---|---|---|---|
| Illusioner | 3 | 24 HP, bow 5 every 1.5 s, range 5.5; Illager | **Mirror images**: the first time it drops under half its health, 3 images (1 HP, no attack) step out around it for 4 s; whoever was on it looks again and may pick an image. **Blindness**: every 3rd arrow cuts its target's range to 60% for 3 s. | 49% |
| Axolotl | 2 | 18 HP, 4 every 1 s, melee; hits 1.5x from the river; Aquatic | **Plays dead** once: hurt under 30%, it lies still for 3 s, nothing can pick it as a target, and it gets half its health back. | 43% |
| Endermite | 1 | 3 of them, 4 HP, 1 every 0.8 s, fast; End, Arthropod | **Vanish** after 10 s. An enemy **Enderman** within 6 blocks drops its target to chase them (vanilla endermen hate endermites). | 52% |
| Allay | 2 | 16 HP, flies (crosses the river); Golem | **Never attacks.** It flies just behind the nearest own troop and **heals** every own troop within 2 blocks by 2 a second. | 44% |
| Magma Cube (reworked) | 2 (was 3) | 2 small cubes (vanilla size 2), 12 HP each, 3 every 1.5 s, no armour, no split; Nether, Slime, fire immune | **Leap**: an enemy within 3 blocks gets jumped on (an archer first, even behind a blocker); the landing deals 1.5x and splashes 1 block. Once every 3 s. A Frog swallows it whole. | 58% |
| Wither Skull | 2 | spell, radius 2, 6 damage, thrown from the King | **Wither** 5 s: 1 a second through armour, and **no healing** meanwhile (Healing, Allay, Axolotl, Zombie regen, lifesteal). Wither Skeletons are immune. Towers take the blast only. | 43% |
| Ender Pearl | 2 | spell, radius 1, no damage | Moves the **one troop nearest the centre**: an own troop 5 blocks toward the enemy, never past the river; an enemy 5 blocks back toward its own King. Buildings stay. | 54% |
| Water Bucket | 2 | spell, radius 2.5, no damage to most | Puts out **fire** on both sides (burning troops, a Strider's trail), **pushes** enemies 1.5 blocks, and deals **12** to the enemies water hurts: Blaze, Enderman, Snow Golem, Strider (an Enderman that can still teleport gets away). | 42% |

Win rates from 4000 bot matches. Allay was 3 elixir and won 39%: it is 2
now (its ability counts x2.0 in the cost). Endermite was 4 mites and won
62%: it is 3 now. The Wither Skull's numbers are the chosen ones; the formula
makes it 2 elixir, not 3 (its wither and the healing it blocks are in the
cost). Pillager (62-64%) and Camel (62-64%) were already over 60% before this
batch and are left as they were.

## Riders: two lives

A jockey's rider is its own troop from the start (`CombatEngine.addRider`,
`SplitRule.into` now means "carries"): it rides, aims and fights from the
saddle and never walks while mounted; whichever of the two dies first, the
other goes on alone (a rider whose mount falls gets off and walks; a mount
whose rider falls keeps running). The rider's damage counts for the played
card. The cost adds the rider's damage to the mount's (`CostFormula`).

| Card | Elixir (was) | Mount | Rider | Win rate |
|---|---|---|---|---|
| Spider Jockey | **4** (3) | spider 16 HP, bites 2 a second | Skeleton Archer | 45% |
| Skeleton Rider | 3 | skeleton horse 20 HP (was 15), kicks 2, towers only | Skeleton Archer, shoots only buildings while mounted | 50% |
| Chicken Jockey | 2 | chicken 4 HP, pecks 1 | baby Zombie | 51% |
| Camel | **3** (2) | camel 32 HP, 2.5 and its dash | Husk | 43% (was 62-64% at 2) |
