# Royal Arena Balance

How card costs, towers, elixir and match time are derived. Values here match
`src/main/resources/data/royalarena/balance.json`.

## Reference data

Timing and the shape of the cost formula are calibrated against Clash Royale
game data extracted by RoyaleAPI (`cr-api-data`, snapshot October 2023,
level-1 stats). Troops themselves are original Minecraft-mob cards and do not
copy Clash Royale cards.

The original data uses roughly 13x larger HP numbers (a 3-elixir melee unit has
about 690 HP). The mod divides HP and damage by 13 so a Zombie has vanilla 20
HP.

## Troop cost

```
P      = sqrt(totalHP * totalDPS) * modifiers
elixir = round(P / cost_divisor)          cost_divisor = 5.4
```

- `totalHP` = effective unit HP x count, where effective HP = HP / (1 -
  0.04 x armour points) (a Zombie's 2 points: 20 / 0.92 = 21.7). A Big Slime
  counts all three generations (36 + 2x18 + 4x9 = 108). A Spider Jockey
  counts the spider and the Skeleton Archer it leaves (16 + 20 = 36).
- Unit HP is the mob's vanilla max health (Zombie 20, Piglin Brute 50, Iron
  Golem 100); two cards keep their own (Big Slime 36, chosen for its 4x size;
  Snow Golem 40, as 4 would not last a second). `scripts/card-library/vanilla.json`
  lists every mob's vanilla health and armour.
- `totalDPS` = damage / interval x count. Poison counts as damage, but only
  what one hit's poison deals before the next hit replaces it: poison per
  second x min(poison time, interval). A Cave Spider (1 a second for 3 s,
  biting every 1 s) adds 1 per bite, not 3. A unit that dies on its hit
  (Creeper, Vex) counts all of its poison; the Bee now lives on and stings
  again, so it counts like the Cave Spider.

Modifiers multiply together:

| Condition | Modifier |
|---|---|
| Range >= 4 blocks | x1.25, and +4% for each block past 4 (6.5 blocks: x1.375) |
| Flying | x1.2 |
| Move speed >= 1.33 blocks/s | x1.1 |
| Count 3-4 | x0.93 |
| Count >= 5 | x0.85 |
| Area damage | x1.3 |
| Self-destruct (dies on its first hit) | x0.5 |
| Targets towers only | x0.85 |
| Knockback | x1.1 |
| Slows what it hits | x1.15 |
| Teleports out of shots and spells | x1.2 |
| Crosses the river anywhere | x1.15 |
| May be played on the river | x1.05 |
| Frost or water weakness (each) | x0.9 |
| Frost-proof (Polar Bear) | x1.05 |
| Line damage (Evoker) | x1.25 |
| Locks on before each new target (Guardian) | x0.85 |
| Hits harder from the river (Guardian) | x1.05 |
| Leaves a burning trail (Strider) | x1.15 |

`cost_divisor` is a tuning knob. At 5.4 the starter set averages under 3
elixir. Lowering it makes every troop more expensive. Decide after playtests.

### Starter troop costs

| Card | Total HP (with armour) | Total DPS | Modifiers | Raw | Elixir |
|---|---|---|---|---|---|
| Zombie x2 (armour 2) | 43.5 | 6.0 | - | 2.99 | 3 |
| Skeleton Archer x1 | 20 | 4.0 | range 4.5 | 2.11 | 2 |
| Spider x3 | 48 | 6.0 | count 3, speed | 3.21 | 3 |
| Skeletons x2 | 40 | 3.33 | speed | 2.35 | 2 |
| Big Slime x1 | 108 | 3.57 | - | 3.64 | 4 |
| Iron Golem x1 | 100 | 8.75 | towers only | 4.66 | 5 |

## Tribes

Tribe bonuses (docs/TRIBES.md) are power outside the formula. They are
measured on their own with `scripts/tribes.sh` (decks built around a tribe,
with and without the bonuses); the card win rates below are measured on
plain random decks.

## Power band (`PowerCreepTest`)

Cost alone misses how cards play out, so the build also measures every
card in the combat engine (`Sim`) and fails when one leaves the band:

| Card kind | Measure | Band |
|---|---|---|
| Troops that fight | push score: elixir it comes out ahead, averaged over attacking across the bridge into every other fighting troop defending under its tower, and defending against each; tower damage counts 1 elixir per 25 health | -1.2 .. +1.1 |
| Tower-only troops | tower health taken per elixir, walking in alone | 12 .. 25 |
| Spells | cost formula value per elixir paid | 0.90 .. 1.12 |

The failure lists every card's number, so the fix is easy to aim.

## Bot-versus-bot report (`gradlew balanceReport`)

`gradlew balanceReport` (or `-Pmatches=500`) plays random balanced bot decks
against each other, each pairing twice with the sides swapped, and writes
`docs/BALANCE_REPORT.md`: each card's deck win rate with a 95% margin, how
often the bots play it, and how long matches last. About 1 s per 100
matches. It is not part of `build`; run it after a balance change.

## Stats page (`docs/stats/`)

`gradlew statsReport` writes every card's numbers, the push matrix and the bot
results to `docs/stats/data/`, keeping a history of runs; `docs/stats/index.html`
shows them on a phone (`gradlew statsServe` on the same Wi-Fi), and the
`stats-site` workflow publishes it at
https://susu609.github.io/suroyalarena_1_20_1/. See `docs/stats/README.md`.

## Spell cost

```
value  = damagePerTarget + (stun + freeze seconds) * 2
         + slow seconds * 1 + haste seconds * 1
share  = min(1, maxTargets / 4)        (1 when the spell hits everything)
elixir = round(1 + value * radius * share / spell_cost_divisor)
spell_cost_divisor = 20.8
```

The divisor is anchored on a 4-elixir fireball (325 damage, radius 2.5 in the
reference data, i.e. 270 / 13). Burn damage is not counted. A second of
freeze or stun is worth 2 damage per target; healing counts like damage. (It
was 4, but in bot matches a 6 s Freeze won no more than a 4 s one: stopping
troops is worth less than hurting them.) A second of a target slowed (a web:
60% slower walk, half-speed attack) or of an own troop hastened is worth 1,
half a second of freeze; a slow lasts from the first pulse to its slow time
after the last. Poison (Poison Cloud) counts as damage at 1x. Flight time,
tower share and burn are not counted, so they are free levers.

| Spell | Damage per target | Radius | Raw | Elixir |
|---|---|---|---|---|
| Arrow Storm | 16 | 3.0 | 3.31 | 3 |
| Fireball | 25 | 2.5 | 4.00 | 4 |
| Lightning | 40 + 0.5 s stun (41), 3 targets | 3.5 | 6.17 | 6 |
| Potion of Healing | 16 healing | 3.0 | 3.31 | 3 |
| Freeze | 8 + 4 s freeze (16) | 3.0 | 3.31 | 3 |
| TNT | 30 | 3.0 | 5.33 | 5 |
| Poison Cloud | 12 poison | 3.8 | 3.19 | 3 |
| Cobweb | 5.75 s slow | 3.5 | 1.97 | 2 |
| Swiftness Potion | 6 s haste | 3.5 | 2.01 | 2 |
| Snowball Volley | 6 + 1.5 s slow (7.5) | 2.5 | 1.90 | 2 |

Spells deal 30% damage to towers (`spell_tower_damage_multiplier`); Arrow
Storm and TNT deal 50%, Lightning 45%, Freeze 80%, and a healing potion,
Swiftness, Cobweb and Poison Cloud never touch towers. TNT can be cast only on
the caster's own half and up to the middle of the bridge
(`SpellStats.middleReach`), so it defends and cannot be thrown at an enemy
tower; the formula does not count reach.

## Towers

| Tower | HP | Damage | Interval | Range |
|---|---|---|---|---|
| Princess | 110 | 3 | 0.8 s (16 ticks) | 7.0 |
| King | 185 | 4 | 1.0 s (20 ticks) | 7.0 |

The King tower is asleep until it is damaged or one of its Princess towers is
destroyed. Tower range is measured from the tower's edge (its 4x4 footprint) to
the target's edge, like unit ranges, so a Princess tower (7.0) always outranges
an archer (4.5), a Ghast (5.0) and a Pillager (6.0).

## Elixir and match time

| Setting | Value |
|---|---|
| Starting elixir | 6 |
| Max elixir | 10 |
| Regulation time | 180 s (3600 ticks) |
| Overtime | 120 s (2400 ticks) |

| Phase | Match ticks | Ticks per elixir | Rate |
|---|---|---|---|
| 1 | 0-2400 | 56 (2.8 s) | x1 |
| 2 | 2400-4800 | 28 (1.4 s) | x2 |
| 3 | 4800-6000 | 19 (~0.95 s) | x3 |

Phase 2 covers the last minute of regulation and the first minute of overtime.

At the end of regulation the side with more crowns wins. With level crowns
the match goes to overtime: the first tower destroyed wins at once, and if
none falls the most tower health left wins (`MatchInstance.timeUp`).
The reference data uses 0.93 s for x3; 19 ticks is the nearest whole tick.

Minimum time between two card plays: 20 / 10 / 7 ticks in phases 1 / 2 / 3
(reference: 1000 / 500 / 350 ms).

## Movement scale

1 tile = 1 block. Reference "medium" speed is about 1 block/s. Vanilla mobs
walk about 4.3 blocks/s, so troops must use their own move speed instead of
vanilla defaults. The exact attribute value per block/s still needs measuring
in game.
