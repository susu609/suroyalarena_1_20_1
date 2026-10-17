"""Turn a transparent mob picture into a 248x312 card portrait, styled like the
Blockbench plugin (card blue, soft light, ground shadow), plus a preview of the
card in the hand with its cost badge.

Usage: python3 picture_to_card.py <picture.png> <card id>.png preview.png [cost]
Needs Pillow (pip install pillow).
"""
import math, sys
from PIL import Image, ImageDraw, ImageFilter

src, out, preview = sys.argv[1], sys.argv[2], sys.argv[3]
W, H = 248, 312
BASE = (0x3b, 0x5b, 0x8c)

def shade(c, a):
    return tuple(max(0, min(255, round(v + a * 255))) for v in c)

# Radial gradient like the plugin: light at (W/2, 0.4H) fading to darker edges.
bg = Image.new('RGBA', (W, H))
light, dark = shade(BASE, 0.22), shade(BASE, -0.12)
px = bg.load()
for y in range(H):
    for x in range(W):
        d = math.hypot(x - W / 2, y - H * 0.42)
        t = max(0.0, min(1.0, (d - W * 0.1) / (H * 0.75 - W * 0.1)))
        px[x, y] = tuple(round(light[i] + (dark[i] - light[i]) * t) for i in range(3)) + (255,)

mob = Image.open(src).convert('RGBA')
mob = mob.crop(mob.getbbox())
scale = min(H * 0.9 / mob.height, W * 0.88 / mob.width)
mob = mob.resize((round(mob.width * scale), round(mob.height * scale)), Image.LANCZOS)
x = (W - mob.width) // 2 + 10          # a little right: the cost badge covers the top-left corner
y = H - mob.height - 10

# Soft ground shadow under the feet.
shadow = Image.new('RGBA', (W, H), (0, 0, 0, 0))
ImageDraw.Draw(shadow).ellipse((x + mob.width * 0.18, H - 26, x + mob.width * 0.92, H - 6), fill=(0, 0, 0, 110))
bg = Image.alpha_composite(bg, shadow.filter(ImageFilter.GaussianBlur(5)))
bg.alpha_composite(mob, (x, y))
bg.convert('RGB').save(out, optimize=True)

# Preview: the card in the hand (edge + cost badge), like the HUD draws it.
s = W / 31
edge, badge, over = max(2, round(1.5 * s)), round(11 * s), round(2.5 * s)
pv = Image.new('RGBA', (W + over * 2 + 40, H + over * 2 + 40), (68, 68, 68, 255))
ox, oy = over + 20, over + 20
d = ImageDraw.Draw(pv)
d.rectangle((ox, oy, ox + W - 1, oy + H - 1), fill=(0x1e, 0x24, 0x33))
pv.paste(bg.resize((W - 2 * edge, H - 2 * edge)), (ox + edge, oy + edge))
d.rectangle((ox - over, oy - over, ox - over + badge - 1, oy - over + badge - 1), fill=(0x1a, 0x0e, 0x1f))
d.rectangle((ox - over + 2, oy - over + 2, ox - over + badge - 3, oy - over + badge - 3), fill=(0xd0, 0x5c, 0xff))
try:
    from PIL import ImageFont
    font = ImageFont.truetype('DejaVuSans-Bold.ttf', round(badge * 0.7))
except OSError:
    font = None
d.text((ox - over + badge / 2, oy - over + badge / 2), sys.argv[4] if len(sys.argv) > 4 else '?', fill='white', anchor='mm', font=font)
pv.save(preview)
