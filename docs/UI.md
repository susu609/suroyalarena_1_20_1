# Royal Arena Battle UI

The battle HUD copies the proportions of a 200x356 portrait Clash Royale
screenshot. One unit `u = screen height / 356`; the 200-unit reference column
is centred horizontally. All positions live in `client/hud/HudLayout.java`
(pure Java, covered by `DomainSmokeTest`).

## Layout (reference units)

| Element | Position (x, y, w, h) | Notes |
|---|---|---|
| Control panel | 37, 290, 163, 66 | wooden panel, bottom ~18.5% |
| Hand cards | 47 + 37.5 * slot, 297, 31, 39 | portrait 4:5 |
| Card cost | top-left corner of the card, 11 x 11, sticking out 2.5 | purple drop, dimmer when unaffordable |
| Next label / card | 6, 314 / 10, 324, 18, 23 | next card ~58% of a hand card |
| Elixir bar | 41, 341, 153, 8 | continuous, 10 tick marks, under the cards |
| Elixir drop | 36, 336, 16, 17 | whole elixir number |
| Timer box | 164, 1, 35, 15 | "Time left:" + m:ss |
| Crown counters | 186, 116 (opponent) / 186, 182 (you), 13 x 15 | team colours |

At 1080 px height `u` is about 3.03, so a card is 94 x 118 px.

Cards show only their art and their cost on the top-left corner (no troop
count, no key number; the keys 1-4 still pick the cards). The art is a portrait PNG if one
exists, otherwise the mob's spawn egg (an arrow for Arrow Storm, a fire
charge for Fireball). Portraits are optional and live at
`assets/royalarena/textures/gui/card/<card id>.png` in this mod or any
resource pack (ids: `zombie`, `skeleton_archer`, `spider`, `skeletons`,
`big_slime`, `stray`, `vindicator`, `iron_golem`, `creeper`, `witch`,
`wolves`, `ravager`, `hoglin`, `pillager`, `magma_cube`, `arrow_storm`,
`fireball`, `lightning`, `healing_potion`, `freeze`, `tnt`; a new card's id is its
lower-case enum name).
Any size works, the image is stretched over the card face (31:39, so 124x156 or
248x312 is a good size); press F3+T after adding one. See `CardArt`. The
Blockbench plugin in `model_sources/card_art/` renders portraits in this
format from a mob model. Cards that
cost more than the current elixir are greyed out. A hovered card rises 3 units
with a light frame and tint; the selected card rises 8 units with a gold frame
and glow. The rise is animated (eases out over about 45 ms,
`HudLayout.approachLift`), and the frame, art, tint and cost badge
all follow the same lift, so the whole card moves as one piece. The cost
badge is drawn above the grey "can't afford" overlay so the cost stays
readable. The click area includes the space a card rises into.

## Camera

The strategic camera sits behind the local side and looks straight down the
arena axis from high above (`ArenaCameraProfile`: pitch 68 degrees, 26 blocks
from a point 6.5 blocks toward the local side of the centre). There is
no sideways swing; the "diagonal" view is the downward tilt. RED's camera is
BLUE's rotated 180 degrees, so both players see their own half the same way.

The numbers are chosen so the whole arena sits between the top of the screen
and the HUD panel on a 16:9 window (vertical FOV 70); `DomainSmokeTest` checks
this for both sides. Players can change pitch, distance, look-ahead and FOV in
`royalarena-client.toml` (see `docs/CONFIG.md`); the defaults live in
`ArenaCameraProfile.DEFAULT` and the test checks that they fit. Cursor projection uses the camera's real view vector, so deployment
still lands under the cursor. A config file written before these defaults
keeps its own `distance` and `lookAhead`; delete the two lines to get the new
ones.

While watching bots the camera is different: high above the middle and
side-on (`ArenaCameraProfile.watch`: yaw -90, pitch 74, 28 blocks), so RED's
half is on the left and BLUE's on the right; `DomainSmokeTest` checks that the
whole arena fits between the two side panels.

## Health bars

- Towers: always shown, a long 16-segment bar (almost twice a troop's)
  with "health/max" written on it, team coloured. The King's floats above
  the player standing on the tower.
- Troops: shown once damaged. The troop's custom name is coloured by team on
  the server (entity tags are not synced to clients); the bar reads the side
  from that colour and uses the deep team colour.
- Team colours: deep blue `#1F4FD6` and deep red `#C81E1E`
  (`WorldHealthBar.BLUE_RGB` / `RED_RGB`), also used by the rings under the
  troops, the deploy preview and the HUD counters.

Bars are billboards drawn like vanilla name tags (text backgrounds), with
filled and empty segments side by side so they never overlap. Each bar has a
black frame (2 px on towers, 1 px on troops) drawn behind it, and the
"health/max" label has a black outline: the outline in the normal text layer
and the label in the polygon-offset layer, as vanilla draws glow-ink signs
(drawing both in one layer made them z-fight). Tower bars are drawn at 0.065,
troop bars at 0.032 blocks per font pixel.

## Damage numbers

Red "-5" over a troop, orange over a tower, green "+2" for healing, blue
"Frozen" (`DamageNumbers`, `damageNumbers` to turn off). They are 1.5x the
first size, outlined in black like the bar labels, pop up, rise and shrink
away at the end (no fading: a see-through letter would show its outline).
Being depth-tested they are pulled about a block toward the camera so the
troop's body does not hide them. `worldTextScale` (0.5..2) in
`royalarena-client.toml` scales them and the health bars.

## The King

A player model stands on top of each King tower (`KingFigure`, drawn by
`KingTowerRenderer`): it wears the skin of that side's player (the bot is
Steve or Alex). While the King tower sleeps it dozes, head down, with a
snore bubble every 1.5 s. When the tower wakes (it is hit, or one of its
Princess towers falls) the King jumps with both arms up, with a level-up
chime; then it breathes, looks around, and throws its arm forward every time
its tower shoots; the King's arrows leave from its hand.

Towers and their King flash with vanilla's red hurt tint for 8 ticks
whenever the tower loses health (`WorldHealthBar.towerOverlay`). The snapshot packet
carries both players' ids for this.

## Deploy preview and delay

- With a card selected, the tile under the cursor is tinted **green** when
  deployable (own half, enough elixir) and **red** otherwise; green tiles also
  show translucent ghosts of every unit the card brings (two Zombies, four
  Spiders...), in the formation and at the size they will appear. Clicking a red tile sends nothing and
  keeps the card selected (the server still validates every deploy).
  Every enemy troop the ghosts would draw gets a bouncing yellow **"!"**
  over its head (`AggroMarkers`): its sight reaches a ghost, by the engine's
  rule (tower-only troops never turn; a melee troop does not see across the
  river unless one of them is on a bridge). A troop already fighting
  something close keeps it, so "!" means it may turn.
- A selected **spell** shows its circle of effect instead of a tile and a
  ghost, and every enemy troop and tower it would hit lights up **white**,
  the whole model, pulsing (vanilla's Creeper-flash overlay, on your screen
  only; `SpellTargetHighlighter`, `LivingEntityRendererMixin`, towers through
  `WorldHealthBar.towerOverlay`). Lightning lights only the 3 it would strike,
  Healing and Swiftness your own troops. Spells can be aimed anywhere on the arena, so the circle is green
  everywhere on it when the elixir is there. A spell in flight shows a
  team-coloured circle that fills up until it lands.
- An accepted click spends elixir and cycles the hand at once, then the troop
  waits `MatchInstance.DEPLOY_TICKS` (20 ticks = 1 s). Both players see the
  ghost and a team-coloured square that fills up; the troop spawns when it is
  full.
- Ghosts are invisible client-only entities. Vanilla draws invisible mobs
  translucent for spectators, and arena players are spectators.

## Deck screen

Press `M` and choose **Deck**. The top row holds the 8 deck slots, the card
library below shows every card (all cards are unlocked).

- Click a library card to put it in the first empty slot. With a full deck the
  card is picked up (gold frame); click the deck card it replaces. Right-click
  drops it.
- Click a deck card, or its library card (marked "In deck"), to take it out.
- Above the library: tabs **All / Troops / Spells / Buildings** (no building
  cards yet), **Sort** (library order or cheapest first), and filter buttons
  **Attack / Defense / Support** (hand-written per card, `"roles"` in its
  JSON) and **Tank / Swarm / Ranged / Area** (from the stats). Pressed ones
  are yellow and all of them must match. The library shows 16 cards a page;
  the `<` `>` arrows or the mouse wheel over the library turn the page, and
  the line above it shows how many cards match.
- Hover any card for its numbers: cost, roles and tactics, health x count, damage and
  interval, range and speed, split; for spells radius, flight time, damage,
  knockback, burn and the 30% tower damage.
- The line under the deck shows the card count and the average elixir.
- **Save** (only for 8 different cards) sends the deck to the server, which
  checks it again and stores it with the world
  (`data/royalarena_decks.dat`). **Random** fills a balanced random deck,
  **Starter** the starter deck; neither is saved until you press Save.

## Outside a match

With no screen open, the top left corner reads "Press [M] to open Royal
Arena", the key taken from the key binding (`menuHint` to turn it off; F3
hides it).

## Watching bots

**Watch Bot vs Bot** on the Matching screen opens `WatchSetupScreen`: each
bot's deck (Random, My deck, Starter) and personality (Random or one of
four), RED on the left and BLUE on the right; Start. During the match
(`SpectatorHud`) a panel at each screen edge, 17% of the width, shows the
team and personality, crowns, the four cards in hand (grey when unaffordable),
the next card and the elixir bar (it throbs when full). The timer stays at the
top with "WATCHING x1 [X]" under it; `X` switches to double speed. The normal
hand panel is hidden, and clicks and card keys do nothing. A bot carrying a
card shows a hand in its colour, a faint ring at the landing spot and the
card's name above it. See `docs/BOT.md`.

## Result screen

When a match ends the server sends `S2CMatchResultPacket` and every player
in it sees `ResultScreen` over the dimmed arena (design:
https://claude.ai/artifact/RacR1QSbNCJp9kgUspzSsC). The animation
(`ResultTimeline`, about 2 s; a click or a key skips it):

1. The arena dims (35% -> 62%) and the name ribbons slide in: the opponent's
   red one on top, yours blue below, each with the face from the player's
   skin (a Zombie face for the bot). Sound: toast in.
2. The crowns pop one by one, the opponent's first, then yours left, right,
   King; each grows to 125% and settles. Sound: experience orb, a little
   higher each time.
3. VICTORY! (gold), DEFEAT (red) or DRAW (white) zooms in at three times the
   font size; light rays turn behind the winner's King crown (faint and in
   the middle on a draw). Sound: challenge complete, or beacon off. The
   server fires rockets in the winner's colours over their King tower.
4. Your MVP (the card that dealt the most damage to troops and towers) on a
   toast, then **Play again** (bot matches only) and **OK**.

Textures: vanilla Advancement challenge frames (gold when the crown was
won, grey when not) with the golden helmet as the crown for now, the toast,
the buttons and the font with its shadow; ours are the ribbons and the rays
(`textures/gui/result/`). The battle HUD hides while the screen is open.

OK (or Esc) sends you home at once (`C2SResultDonePacket`); Play again also
starts a new bot match. Players who do nothing are sent home after 15 s.

Every match starts from a shuffle of the saved deck. A player who never saved
a deck plays the starter deck.
