# Royal Arena Map

An even **18 x 32** arena rebuilt for every match by `ArenaBuilder` and
cleared when the match ends. `ArenaLayout` is the single source of
coordinates for towers, bridges, river, deploy areas and camera anchors.

## Layout

Every size is even (towers 4x4, bridges 2 wide, river 2 rows), so both mirror
axes lie on block boundaries at the arena centre. BLUE owns +Z (bottom), RED
owns -Z (top). Row numbers are Z offsets from the centre.

```
     012345678901234567
-16  ..................
-15  .......kkkk.......
-14  .......kkkk.......
-13  .pppp..kkkk..pppp.
-12  .pppp..kkkk..pppp.
-11  .pppp........pppp.
-10  .pppp........pppp.
 -9  ..................
 -8  ..................
 -7  ..................
 -6  ..................
 -5  ..................
 -4  ..................
 -3  ..................
 -2  ..................
 -1  ~~==~~~~~~~~~~==~~
  0  ~~==~~~~~~~~~~==~~
  1  ..................
  2  ..................
  3  ..................
  4  ..................
  5  ..................
  6  ..................
  7  ..................
  8  ..................
  9  .PPPP........PPPP.
 10  .PPPP........PPPP.
 11  .PPPP..KKKK..PPPP.
 12  .PPPP..KKKK..PPPP.
 13  .......KKKK.......
 14  .......KKKK.......
 15  ..................
```

`P`/`p` Princess tower (BLUE/RED), `K`/`k` King tower, `~` river,
`=` bridge.

| Part | Value |
|---|---|
| Floor | 18 x 32, 15 rows per side plus 2 river rows |
| River | Water, 1 deep on a stone bed, rows -1 and 0 |
| Bridges | 2 wide, spruce planks, X offsets -7..-6 and 5..6 |
| Princess towers | 4x4, centre X = +-6 (same line as the bridges) |
| River to Princess | 8 empty rows |
| King tower | 4x4 at the centre, 1 empty row behind it |
| Border | Stone bricks on the sides; blue/red concrete behind each half |
| Deploy area | The owner's whole half, river excluded |

Reference: the Clash Royale wiki gives an 18 x 32 tile arena, a 4x4 King
tower 1 tile from the back, 3x3 Princess towers and 2-tile bridges in most
arenas. The mod stretches the Princess model to 4x4 to keep everything even,
and uses an 8-row gap so the lanes feel open. At that distance a Princess
tower (range 7.5) only reaches troops about 2.5 blocks after they leave the
bridge.

## Troop route

Troops spawn without vanilla AI and are steered by `ServerMatchSession`.
`ArenaLayout.riverWaypoint` sends a troop to the near end of the bridge on its
own side of the arena, then across it, and returns null once the troop is
past the river; after that it walks straight at its tower.

## Practice match

`Practice Match` on the Matching screen (`M`) or `/royalarena practice` starts
a match with the player as BLUE and an empty RED side, to try cards and the
map alone.
