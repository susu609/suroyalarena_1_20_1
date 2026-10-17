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

- `totalHP` = unit HP x count. A Big Slime counts all three generations
  (20 + 20 + 20 = 60).
- `totalDPS` = damage / interval x count.

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

`cost_divisor` is a tuning knob. At 5.4 the starter set averages under 3
elixir. Lowering it makes every troop more expensive. Decide after playtests.

### Starter troop costs

| Card | Total HP | Total DPS | Modifiers | Raw | Elixir |
|---|---|---|---|---|---|
| Zombie x2 | 40 | 6.0 | - | 2.87 | 3 |
| Skeleton Archer x1 | 20 | 4.0 | range | 2.07 | 2 |
| Spider x4 | 40 | 8.0 | count 4, speed | 3.39 | 3 |
| Skeletons x3 | 33 | 5.0 | count 3, speed | 2.43 | 2 |
| Big Slime x1 | 60 | 3.57 | - | 2.71 | 3 |
| Iron Golem x1 | 160 | 7.5 | towers only | 5.45 | 5 |

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

## Spell cost

```
value  = damagePerTarget + (stun + freeze seconds) * 4
share  = min(1, maxTargets / 4)        (1 when the spell hits everything)
elixir = round(1 + value * radius * share / spell_cost_divisor)
spell_cost_divisor = 20.8
```

The divisor is anchored on a 4-elixir fireball (325 damage, radius 2.5 in the
reference data, i.e. 270 / 13). Burn damage is not counted. A second of
freeze or stun is worth 4 damage per target; healing counts like damage.

| Spell | Damage per target | Radius | Raw | Elixir |
|---|---|---|---|---|
| Arrow Storm | 16 | 3.0 | 3.31 | 3 |
| Fireball | 25 | 2.5 | 4.00 | 4 |
| Lightning | 40 + 0.5 s stun (42), 3 targets | 3.5 | 6.30 | 6 |
| Potion of Healing | 16 healing | 3.0 | 3.31 | 3 |
| Freeze | 4 + 4 s freeze (20) | 3.0 | 3.88 | 4 |
| TNT | 30 | 3.0 | 5.33 | 5 |

Spells deal 30% damage to towers (`spell_tower_damage_multiplier`); TNT
deals 50%, and a healing potion never touches towers.

## Towers

| Tower | HP | Damage | Interval | Range |
|---|---|---|---|---|
| Princess | 110 | 3 | 0.8 s (16 ticks) | 7.0 |
| King | 185 | 4 | 1.0 s (20 ticks) | 7.0 |

The King tower is asleep until it is damaged or one of its Princess towers is
destroyed. Tower range is measured from the tower's edge (its 4x4 footprint) to
the target's edge, like unit ranges, so a Princess tower (7.0) always outranges
an archer (6) and a Pillager (6.5).

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
