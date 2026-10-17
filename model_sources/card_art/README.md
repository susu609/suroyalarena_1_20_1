# Card art template for Blockbench

`royalarena_card_art.js` is a Blockbench plugin that renders the open model as
a card portrait for the battle HUD. Every card comes out the same way:

- the card's 31:39 shape (124x156, **248x312** or 496x624 px);
- the subject framed by its bounding sphere, so a Spider and an Iron Golem
  fill the card alike, from the same angle;
- a card-blue background (soft light, flat, any colour, or transparent);
- a PNG named after the card id, ready for
  `src/main/resources/assets/royalarena/textures/gui/card/<card id>.png`
  (or the same path in a resource pack).

## Install

Blockbench: **File > Plugins**, the "Load Plugin from File" button, pick
`model_sources/card_art/royalarena_card_art.js`. It adds **Tools > Render Card
Art**.

## Make a card

1. Open the mob's model in Blockbench with its texture (your own `.bbmodel`,
   an OptiFine CEM `.jem`, a Bedrock `.geo.json`, ...). Pose it if you like:
   the render uses the model as it is shown.
2. **Tools > Render Card Art**.
3. Pick the card. Leave the other settings for a consistent set:

   | Setting | Default | Notes |
   |---|---|---|
   | Size | 248 x 312 | 8 px per card unit; the HUD scales it |
   | Angle | Fixed | or "Current view" to use the angle you are looking from |
   | Yaw / Pitch | 30 / 15 | if you see the back of the mob, add 180 to yaw |
   | Field of view | 30 | lower = flatter, more like an icon |
   | Zoom | 1.0 | above 1 crops closer, below 1 adds room |
   | Move subject up | 0 | positive moves the mob up on the card |
   | Background | Card blue, soft light | |

4. The result dialog shows the exported image and how it looks in the hand
   (card edge and the cost badge on the top-left corner; those are not in the
   PNG). Keep the mob's face clear of the top-left corner.
5. **Save PNG**, choose the card folder, keep the file name. In game press
   **F3+T** to reload.

Spells have no mob: model the icon (an arrow volley, a fire charge) in
Blockbench and render it the same way, or draw the PNG by hand.

The plugin remembers the last settings while Blockbench is open. Its card
list must match `BattleCard` (ids and costs); `DomainSmokeTest` fails when
they drift apart, so add a new card to both.

## From a picture instead of a model

`picture_to_card.py` turns a picture with a transparent background (a render,
a screenshot with the background removed) into the same card style:

```
python3 model_sources/card_art/picture_to_card.py zombies.png \
    src/main/resources/assets/royalarena/textures/gui/card/zombie.png \
    preview.png 3
```

The mob is fitted to about 90% of the card height, nudged right to keep the
top-left corner free for the cost badge, and stands on a soft shadow.
`preview.png` shows the card in the hand with the cost given last.
