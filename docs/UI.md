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
| Card cost | bottom centre of the card, 10 x 10 | purple drop |
| Next label / card | 6, 314 / 10, 324, 18, 23 | next card ~58% of a hand card |
| Elixir bar | 41, 341, 153, 8 | continuous, 10 tick marks, under the cards |
| Elixir drop | 36, 336, 16, 17 | whole elixir number |
| Timer box | 164, 1, 35, 15 | "Time left:" + m:ss |
| Crown counters | 186, 116 (opponent) / 186, 182 (you), 13 x 15 | team colours |

At 1080 px height `u` is about 3.03, so a card is 94 x 118 px.

Cards show the mob's spawn egg as art and a small key number (1-4). Cards that
cost more than the current elixir are greyed out. A hovered card rises 3 units
with a light frame; the selected card rises 8 units with a gold frame and glow.
The click area includes the space a card rises into.

## Camera

The strategic camera sits behind the local side and looks straight down the
arena axis from high above (`ArenaCameraProfile`: pitch 68 degrees, 30 blocks
from a point 5 blocks toward the local side of the centre). There is
no sideways swing; the "diagonal" view is the downward tilt. RED's camera is
BLUE's rotated 180 degrees, so both players see their own half the same way.

The numbers are chosen so the whole arena sits between the top of the screen
and the HUD panel on a 16:9 window (vertical FOV 70); `DomainSmokeTest` checks
this for both sides. Players can change pitch, distance, look-ahead and FOV in
`royalarena-client.toml` (see `docs/CONFIG.md`); the defaults live in
`ArenaCameraProfile.DEFAULT` and the test checks that they fit. Cursor projection uses the camera's real view vector, so deployment
still lands under the cursor.

## Health bars

- Towers: always shown, ten segments plus the remaining HP, team coloured.
- Troops: shown once damaged. The troop's custom name is coloured by team on
  the server (entity tags are not synced to clients), and the bar takes that
  colour.

Bars are billboards drawn like vanilla name tags (text backgrounds), with
filled and empty segments side by side so they never overlap.

## Deploy preview and delay

- With a card selected, the tile under the cursor is tinted **green** when
  deployable (own half, enough elixir) and **red** otherwise; green tiles also
  show a translucent ghost of the troop. Clicking a red tile sends nothing and
  keeps the card selected (the server still validates every deploy).
- An accepted click spends elixir and cycles the hand at once, then the troop
  waits `MatchInstance.DEPLOY_TICKS` (20 ticks = 1 s). Both players see the
  ghost and a team-coloured square that fills up; the troop spawns when it is
  full.
- Ghosts are invisible client-only entities. Vanilla draws invisible mobs
  translucent for spectators, and arena players are spectators.

