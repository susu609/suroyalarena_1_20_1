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
and uses an 8-row gap so the lanes feel open. Tower range is measured from the tower's edge, so a
Princess tower (7.0) covers the ground up to a block past the river: troops
are shot right after they step off a bridge, not while they are on it.

## Troop route

Troops spawn without vanilla AI and are steered by `ServerMatchSession`.
`ArenaLayout.riverWaypoint` sends a troop to the near end of the bridge on its
own side of the arena, then across it, and returns null once the troop is
past the river; after that it walks straight at its tower.

## Practice match

`Practice Match` on the Matching screen (`M`) or `/royalarena practice` starts
a match with the player as BLUE and an empty RED side, to try cards and the
map alone.

## Roads

The floor carries a dirt road (`minecraft:dirt_path`, `ArenaLayout.isPath`)
laid out like Clash Royale's: on each half a "U" through the three towers.

- A 2-wide lane runs from each bridge straight through its Princess tower
  (bridge, lane and tower share one axis) and on behind it.
- Level with the middle of the King tower, a 2-row road turns in from both
  lanes and meets the King's sides.
- There is no road across the front of the towers, none in the river and
  none under a tower.

The road is mirrored on both axes, and `DomainSmokeTest` checks that it links
both bridges of a side to all three of its towers. It is decoration: troops
walk the same routes on grass and road.

## Where matches are played

Matches run in their own empty dimension, `royalarena:arena`
(`data/royalarena/dimension/arena.json`, a sumodlib void dimension: noon
forever, nothing generated). Arenas sit on a 64-block grid from the origin
with the floor at Y 64 (`ArenaDimension`, `ArenaAllocator`), so they never
land on terrain whatever world type the save uses. Players are moved there
for the match and back to where they were afterwards. A player who logs out
during a match and comes back later is sent to the overworld spawn.

If the dimension is missing (an old jar of sumodlib), matches fall back to
the sky above the overworld spawn as before.

For testing, `runClient` opens the Void world "Royal Arena Test" straight
away (sumodlib's `sumodlib.devWorld` property in `build.gradle`; `client2`
does not, so it can join over LAN), and the dev server creates Void worlds
(`level-type=sumodlib:void`).
