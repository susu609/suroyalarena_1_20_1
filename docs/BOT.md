# Royal Arena Bot

`bot/BotBrain` (pure Java) is the opponent in solo matches. It plays through
`MatchInstance.tryDeploy`, the same call a player's click ends in, so elixir,
the hand cycle, deploy areas and the one-second deploy delay all apply to it.

## Starting a solo match

- Press `M` and choose **Play vs Bot**, or run `/royalarena bot` (alias
  `/royalarena practice`). You play BLUE; the bot plays RED.
- Two players can still match with **Find Match** or `/royalarena debug_start`.

## Its deck

Each match the bot draws a random deck (`BotDeck`): 8 different cards with at
least one spell, one tank (one body with 50+ health, split children
included: Iron Golem, Big Slime, Ravager, Magma Cube) and one ranged troop, averaging 2.8 to 3.6
elixir, in a random order. The seed comes from the match id.

## What it does

It thinks every 10 ticks and never plays twice within 40 ticks.

- **Spell.** If a spell in its hand would hit at least two enemy units and
  take off at least 8 health per elixir it costs (24 for Arrow Storm, 32 for
  Fireball), it casts it where it hits most, a little ahead of where the units
  walk. It never wastes a spell on a single unit.
- **Defend.** If an enemy unit stands on its bank of the river, it plays the
  sturdiest troop card it can afford (most total health; troops that only
  target towers, Iron Golem and Hoglin, are never used to defend) three blocks behind that
  unit, in the same lane.
- **Newer spells.** The same rule: each is cast where it is worth 8
  health per elixir over at least two targets. A freeze or stun is worth the
  damage the unit would have dealt meanwhile (its damage per second x the
  seconds), and only for an enemy that is fighting: within its range or one
  of the bot's own troops' range (+2 blocks) of an own troop or tower. Freeze
  can also go on an enemy tower while own troops stand in its range; the
  tower's missed shots count. Lightning counts only its 3 strongest targets,
  and a healing potion goes on its own damaged troops that are fighting (an
  enemy troop or tower within reach) and at 70% health or less (the health it would
  give back).
- **Attack.** Otherwise, once it has 7 elixir, it plays a random affordable
  troop card near the back of the lane whose enemy Princess tower is weaker (or the
  surviving lane when one has fallen, or a random lane when equal).
- It uses a random seed (taken from the match id), so one game is
  reproducible in tests but each match plays differently.

## Tuning

The numbers at the top of `BotBrain` (`THINK_EVERY_TICKS`, `MIN_GAP_TICKS`,
`ATTACK_MIN_TENTHS`, `DEFENCE_DEPTH`, `ATTACK_DEPTH`) are the difficulty knobs.
A lower `ATTACK_MIN_TENTHS` makes it attack earlier and more often.

## What the tests show

`BotSmokeTest` runs whole games with a harness that ticks like the server:

- Alone against an idle opponent a bot takes a Princess tower inside three
  minutes (seed 2; seeds 4 and 6 also did) and damages the King, but does not
  win the match outright.
- Two bots against each other trade troops all game but rarely hurt a tower:
  every push is met by a defender and the towers out-range most attackers.
  Whether that is the right balance is a separate decision from the bot.
- Every bot play is accepted by the server rules, stays in its own deploy area
  and respects the gap between plays.

Not built: spells (there are none yet), smarter lane choice, pushing two cards
together, difficulty levels.
