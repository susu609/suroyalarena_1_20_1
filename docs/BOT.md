# Royal Arena Bot

`bot/BotBrain` (pure Java) is the opponent in solo matches. It plays through
`MatchInstance.tryDeploy`, the same call a player's click ends in, so elixir,
the hand cycle, deploy areas and the one-second deploy delay all apply to it.

## Starting a solo match

- Press `M` and choose **Play vs Bot**, or run `/royalarena bot` (alias
  `/royalarena practice`). You play BLUE; the bot plays RED.
- Two players can still match with **Find Match** or `/royalarena debug_start`.

## Watching bot against bot

Press `M` and choose **Watch Bot vs Bot**: a setup screen picks each bot's
deck (**Random** balanced bot deck, **My deck** = the deck you saved in the
Deck screen, or **Starter**: Zombie, Skeleton Archer, Spider, Skeletons, Big
Slime, Iron Golem, Arrow Storm, Fireball) and personality (Random or one of
the four), RED on the left and BLUE on the right; **Start** begins. The
screen remembers the last choice, and **Play again** watches the same setup.
`/royalarena watch` starts one with the last setup (random the first time). The camera looks at the
whole arena from high up and side-on (`ArenaCameraProfile.watch`): RED's half
on the left, BLUE's on the right. A panel at each edge of the screen
(`SpectatorHud`, 20% of the width each, RED left, BLUE right) shows the team
and personality, crowns, the four cards in hand (grey when the bot cannot
pay), the next card and the elixir bar (it throbs when full). The timer stays
at the top with "WATCHING x1 [X]" under it; `X` (rebindable, "Watch Speed")
switches between normal and double speed (the server runs two match ticks
per tick). Clicks and card keys do nothing. Each bot's hand is drawn in its
colour while it carries a card, with a faint ring and the card's name over
the spot where it will land. The chat names both personalities. **Play again** on the result screen starts another watched match.
The match log marks it `"mode": "watch"`.

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

## Personalities

Each match the bot draws one of four personalities from its seed (the
match id), so two bot matches seldom play the same. The chat tells the player
which one: "Your opponent plays Rush". `personality` in
`royalarena-common.toml` fixes one for practice (`random` by default). The
match log records it (`"personality"` on the bot's side).

| Personality | Attacks at | How it attacks |
|---|---|---|
| Tank Push | 9 elixir | Saves for a tank (Iron Golem, Big Slime, Ravager, Magma Cube...) and drops it at the back of a lane, then feeds support troops (ranged first) 2.5 blocks behind it for 10 s. While a tank it cannot pay for yet is in hand, it keeps saving. It never defends with a tank. |
| Rush | 4.5 elixir | The cheapest troop (fastest on ties) right at the bridge, a second one into the same lane within 4 s, the lanes taking turns. |
| Counter | 10 elixir only | Never opens. After a defence it sends a troop in 1.5 blocks behind the defender that got furthest, within 7 s, so they cross together. |
| Spell Siege | 7 elixir | Like the plain bot, but a spell's worth counts 1.2x, and a Princess tower under 30% health gets a damage spell, keeping another spell in hand unless this one destroys the tower. |

Skills every personality has:

- **Counter the threat.** The defender is scored on total health (as
  before), +3 for area damage against a group of three or more, + a quarter of
  its damage per second against one unit with 40+ health, -2 for a swarm
  against an attacker that splashes.
- **Weaker lane, sometimes not.** It attacks the weaker Princess tower, but
  one attack in five goes to the other lane, and one in ten (a slow troop)
  starts in the middle behind its King so the opponent cannot tell the lane.
- **Pull (kite).** One time in three, a lone attacker on its half (not one
  that only hits towers) gets a 1-2 elixir troop in the middle of the half,
  where both Princess towers shoot it.
- **Punish.** When the opponent is down to 2 elixir and it has 4, it attacks
  at once: the opponent cannot answer for a few seconds. (It reads the
  opponent's elixir, as a player who counts every card would.)
- **Never wake the King early.** No spell that hurts may touch the enemy King
  while it sleeps; a sleeping King shoots nothing, an awake one is a third
  tower.

Bot against bot over 4000 matches: Tank Push 55%, Counter 55%, Spell Siege
46%, Rush 43% (Rush was 35% before it paired its troops). Matches end sooner
(about 267 s instead of 284 s) and fewer go to overtime.

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
