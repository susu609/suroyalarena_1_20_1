# Comparing two cards

`/royalarena compare <a> <b>` (card ids or quoted names; Tab completes the
ids) or **Compare cards...** on the sandbox panel measures two cards with the
real combat engine (`sim/CardCompare`, built on `sim/Sim`, the same
simulation the balance tests use). No match starts; it takes about half a
second. The sandbox screen lets you pick A and B from the card picker, swap
them, and **Show on arena** plays them at the left bridge, A for BLUE and B
for RED.

## Troop against troop

| Row | How it is measured | Leader |
|---|---|---|
| Duel at the bridge | 1 against 1 at the left bridge, each side in turn on BLUE's half, averaged; health left after 60 s | more than 10 points more health left |
| Same elixir | copies of each card so the elixir put in is about equal (up to 5), only shown when that is not 1 against 1 | as above |
| Attacking into the other | `Sim.push`: one crosses the bridge into the other defending in front of its tower, towers shooting; elixir ahead (enemy health destroyed times its cost, tower damage at 25 per elixir, minus its own losses) | more than 0.5 elixir apart |
| Alone at a tower | `Sim.solo`: tower health taken per elixir, and seconds to arrive | more than 1 apart |
| Witch behind it | what a Witch walking behind adds to the attack above, her own losses counted (a Skeleton Archer when a Witch is being compared) | more than 0.5 apart |
| Attacking into / defending against swarms | the average `push` against Skeletons, Wolves, Silverfish | more than 0.5 apart |

A tower-only troop (Hoglin, Iron Golem...) skips the duels and the pool:
it does not fight troops.

## The pool

Each troop is also set against every fighting card (60 or so): half the
difference between attacking into it and being attacked by it. More than 0.5
elixir is a win, under minus that a loss, between a draw. Only the totals
and the three cards beaten and lost to most are shown, never the full list:

```
Giant vs 50 cards: beats 32, loses to 2, draws 16
  beats most: Spider Jockey, Wolves, Cave Spider
  loses most to: Guardian, Pillager
```

## Spells

A spell is measured by the share of a troop card's health one cast takes off
(burning, wither and pulses run for 8 s): on the other card if it is a
troop, and on average over the whole pool, with the elixir it trades (the
share destroyed times the troop's cost, minus the spell's) and how long it
stops the troop. Two spells are compared on the pool: the average share,
the share per elixir, how many cards one cast destroys, and the average stop.
A spell against a troop shows the spell's numbers in its own column.

Not included: tribe bonuses (they depend on a deck), and anything that
needs the bot (use `scripts/balance.sh` for that).
