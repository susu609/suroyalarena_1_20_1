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
cost more than the current elixir are greyed out; the selected card has a gold
frame.

## Camera

The strategic camera sits behind the local side and is swung 10 degrees around
the arena centre (`ArenaCameraProfile`). RED's camera is BLUE's rotated 180
degrees, so both players see their own half the same way. Cursor projection
already uses the camera's real view vector, so deployment still lands under
the cursor.

## Health bars

- Towers: always shown, ten segments plus the remaining HP, team coloured.
- Troops: shown once damaged. The troop's custom name is coloured by team on
  the server (entity tags are not synced to clients), and the bar takes that
  colour.

Bars are billboards drawn like vanilla name tags (text backgrounds), with
filled and empty segments side by side so they never overlap.
