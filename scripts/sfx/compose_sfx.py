#!/usr/bin/env python3
"""
Synthesises the battle's shot, hit and spell sounds in the manner of
Minecraft's own (no game's audio is used or shipped): short, dull, foley-like
noises rather than musical tones, each with a few variants that the game
picks at random, like vanilla's bowhit1-4.

    python3 scripts/sfx/compose_sfx.py [out_dir]

The shapes were matched by numbers to the vanilla 1.20.1 sounds of the same
moments (scripts/sfx/analyze.py: length, attack, brightness, energy per
band). What those numbers said:

  bow          0.11 s, soft 45 ms swell, 60% of the energy under 250 Hz,
               no ringing string: a low "fwump" of air
  arrow hit    a low thunk, then a 0.4-0.9 s high rattle of the shaft
  stone        0.1-0.3 s, dull, 80% under 250 Hz
  glass        0.3-0.6 s, sharp, nearly all over 4 kHz
  powder snow  0.3-0.4 s, a soft 70 ms crunch, 1-4 kHz
  amethyst     a bright ring at 4-9 kHz
  bubble       0.1-0.2 s blips at 500-1400 Hz, rising

Writes sounds/sfx/<name><n>.ogg (mono, peak -1 dBFS, like vanilla's).
numpy and ffmpeg, like the music.
"""
import subprocess
import sys
import wave
from pathlib import Path

import numpy as np

SR = 44100
PEAK = 10 ** (-1 / 20)


# ------------------------------------------------------------- building blocks
def t_axis(secs):
    return np.arange(int(secs * SR)) / SR


def biquad_bandpass(x, f, q):
    """RBJ band-pass (constant peak gain)."""
    f = min(f, SR * 0.45)
    w = 2 * np.pi * f / SR
    alpha = np.sin(w) / (2 * q)
    b0, b2 = alpha, -alpha
    a0, a1, a2 = 1 + alpha, -2 * np.cos(w), 1 - alpha
    b0, b2, a1, a2 = b0 / a0, b2 / a0, a1 / a0, a2 / a0
    y = np.zeros_like(x)
    x1 = x2 = y1 = y2 = 0.0
    for i, v in enumerate(x):
        out = b0 * v + b2 * x2 - a1 * y1 - a2 * y2
        x2, x1 = x1, v
        y2, y1 = y1, out
        y[i] = out
    return y


def band(rng, secs, f, q):
    return biquad_bandpass(rng.standard_normal(int(secs * SR)), f, q)


def swell(n, attack, tau):
    """Rises over `attack` (a soft sine curve), then decays with time `tau`."""
    t = np.arange(n) / SR
    rise = np.sin(0.5 * np.pi * np.clip(t / max(attack, 1e-4), 0, 1)) ** 2
    return rise * np.exp(-np.clip(t - attack, 0, None) / tau)


def hit(n, tau):
    t = np.arange(n) / SR
    return np.exp(-t / tau)


def place(buf, sound, at, gain=1.0):
    start = int(at * SR)
    end = min(len(buf), start + len(sound))
    if end > start:
        buf[start:end] += gain * sound[:end - start]


def norm(x):
    return x * PEAK / np.max(np.abs(x))


# ------------------------------------------------------------------- sounds
def bow(rng, pitch=1.0, length=1.0):
    """A low puff of air: vanilla's bow has no string ring."""
    secs = 0.14 * length
    n = int(secs * SR)
    body = band(rng, secs, 150 * pitch, 1.1) * swell(n, 0.035 * length,
                                                      0.03 * length)
    air = band(rng, secs, 1600 * pitch, 1.2) * swell(n, 0.02 * length,
                                                      0.025 * length)
    # A wide band of noise has far more energy than a narrow low one.
    return body + 0.07 * air


def stone_knock(rng, pitch=1.0):
    secs = 0.16
    n = int(secs * SR)
    out = band(rng, secs, 110 * pitch, 1.6) * hit(n, 0.025)
    for i in range(3):
        at = 0.004 + 0.012 * i + rng.uniform(0, 0.006)
        grit = band(rng, 0.02, rng.uniform(2200, 3400), 2.5) * hit(
            int(0.02 * SR), 0.004)
        place(out, grit, at, 0.25 * (1 - 0.25 * i))
    return out


def bow_shot(rng):
    return bow(rng, rng.uniform(0.9, 1.1))


def tower_shot(rng):
    # The bow pitched down (heavier), on a light knock of stone.
    out = np.zeros(int(0.26 * SR))
    place(out, stone_knock(rng, rng.uniform(0.9, 1.1)), 0.0, 0.7)
    place(out, bow(rng, rng.uniform(0.66, 0.74), length=1.3), 0.008)
    return out


def volley(rng):
    out = np.zeros(int(0.5 * SR))
    for i, at in enumerate((0.0, 0.05, 0.1, 0.16, 0.22)):
        place(out, bow(rng, rng.uniform(0.82, 1.08)),
              at + rng.uniform(0, 0.015), 1.0 - 0.1 * i)
    return out


def crossbow_shot(rng):
    # A latch click, a second click, then the release thump.
    out = np.zeros(int(0.3 * SR))
    for at, f in ((0.0, rng.uniform(900, 1100)),
                  (0.04, rng.uniform(1100, 1400))):
        place(out, band(rng, 0.02, f, 4.0) * hit(int(0.02 * SR), 0.005), at,
              0.6)
    n = int(0.25 * SR)
    place(out, band(rng, 0.25, 110, 1.5) * swell(n, 0.006, 0.05), 0.05, 1.5)
    place(out, band(rng, 0.25, 1000, 0.6) * swell(n, 0.004, 0.06), 0.05, 0.6)
    return out


def arrow_hit(rng):
    # A thunk into the target, then the shaft's rattle.
    secs = rng.uniform(0.4, 0.6)
    n = int(secs * SR)
    out = np.zeros(n)
    place(out, band(rng, 0.08, rng.uniform(160, 220), 1.5) *
          hit(int(0.08 * SR), 0.03), 0.0, 12.0)
    t = np.arange(n) / SR
    rate = rng.uniform(26, 38)
    flutter = (0.5 + 0.5 * np.sin(2 * np.pi * rate * t)) ** 2
    rattle = band(rng, secs, rng.uniform(5500, 8000), 1.0) * flutter * hit(
        n, rng.uniform(0.09, 0.15))
    place(out, rattle, 0.004, 0.25)
    ring = np.sin(2 * np.pi * rng.uniform(380, 460) * t[:int(0.08 * SR)]) * \
        hit(int(0.08 * SR), 0.02)
    place(out, ring, 0.0, 0.25)
    return out


def stone_hit(rng):
    return stone_knock(rng, rng.uniform(0.9, 1.2))


def spell_throw(rng):
    # Vanilla throws potions with the bow's sound: a lighter puff.
    return bow(rng, rng.uniform(1.2, 1.35), length=0.9)


def _shards(rng, out, count, lo, hi, start=0.0, gain=1.0):
    for _ in range(count):
        at = start + rng.uniform(0, 0.08)
        secs = rng.uniform(0.04, 0.12)
        t = t_axis(secs)
        shard = sum(np.sin(2 * np.pi * rng.uniform(lo, hi) * t)
                    for _ in range(3)) * hit(len(t), secs / 3)
        place(out, shard, at, gain * rng.uniform(0.15, 0.3))


def zap(rng):
    # A crack, a low rumble, and bright crackles (lightning made small).
    out = np.zeros(int(0.6 * SR))
    place(out, band(rng, 0.03, 4000, 0.4) * hit(int(0.03 * SR), 0.006), 0.0)
    n = int(0.5 * SR)
    place(out, band(rng, 0.5, 70, 0.7) * swell(n, 0.01, 0.12), 0.0, 5.0)
    for _ in range(10):
        at = rng.uniform(0.0, 0.35)
        secs = rng.uniform(0.015, 0.04)
        t = t_axis(secs)
        place(out, np.sin(2 * np.pi * rng.uniform(4000, 9000) * t) *
              hit(len(t), secs / 3), at, 0.1)
    crackle = band(rng, 0.3, 5000, 1.0)
    crackle *= rng.random(len(crackle)) < 0.02
    place(out, crackle * hit(len(crackle), 0.1), 0.0, 1.2)
    return out


def freeze_hit(rng):
    # Glass breaking, a crunch of powder snow, an amethyst ring.
    out = np.zeros(int(0.9 * SR))
    place(out, band(rng, 0.02, 6500, 0.5) * hit(int(0.02 * SR), 0.005), 0.0)
    _shards(rng, out, 8, 3000, 11000)
    n = int(0.35 * SR)
    crunch = band(rng, 0.35, 2500, 0.7)
    grains = np.repeat(rng.random(n // 220 + 1) < 0.5, 220)[:n]
    place(out, crunch * grains * swell(n, 0.06, 0.08), 0.02, 1.2)
    t = t_axis(0.8)
    chime = (np.sin(2 * np.pi * 4650 * rng.uniform(0.97, 1.03) * t) +
             0.7 * np.sin(2 * np.pi * 7250 * rng.uniform(0.97, 1.03) * t))
    place(out, chime * swell(len(t), 0.01, 0.25), 0.03, 0.12)
    return out


def _bubble(rng):
    secs = rng.uniform(0.05, 0.1)
    t = t_axis(secs)
    f0 = rng.uniform(450, 650) if rng.random() < 0.8 else \
        rng.uniform(1100, 1450)
    freq = f0 * (1 + 0.5 * t / secs)
    return np.sin(2 * np.pi * np.cumsum(freq) / SR) * swell(len(t), 0.015,
                                                            secs / 4)


def poison_hit(rng):
    # A bottle breaking, then the first bubbles.
    out = np.zeros(int(0.7 * SR))
    place(out, band(rng, 0.02, 5000, 0.5) * hit(int(0.02 * SR), 0.005), 0.0)
    _shards(rng, out, 5, 4000, 9000, gain=3.0)
    for i in range(5):
        place(out, _bubble(rng), 0.12 + 0.08 * i + rng.uniform(0, 0.03), 0.3)
    return out


def poison_bubble(rng):
    out = np.zeros(int(0.3 * SR))
    for i in range(int(rng.integers(2, 4))):
        place(out, _bubble(rng), 0.06 * i + rng.uniform(0, 0.02))
    return out


# name -> (maker, variants)
SOUNDS = {
    "bow_shot": (bow_shot, 3),
    "tower_shot": (tower_shot, 3),
    "volley": (volley, 2),
    "crossbow_shot": (crossbow_shot, 3),
    "arrow_hit": (arrow_hit, 4),
    "stone_hit": (stone_hit, 3),
    "spell_throw": (spell_throw, 3),
    "zap": (zap, 2),
    "freeze_hit": (freeze_hit, 2),
    "poison_hit": (poison_hit, 2),
    "poison_bubble": (poison_bubble, 4),
}


def write_ogg(samples, out):
    wav = out.with_suffix(".wav")
    with wave.open(str(wav), "wb") as f:
        f.setnchannels(1)
        f.setsampwidth(2)
        f.setframerate(SR)
        f.writeframes((np.clip(samples, -1, 1) * 32767).astype("<i2").tobytes())
    subprocess.run(["ffmpeg", "-y", "-loglevel", "error", "-i", str(wav),
                    "-c:a", "libvorbis", "-q:a", "4", str(out)], check=True)
    wav.unlink()


def main():
    target = Path(sys.argv[1] if len(sys.argv) > 1 else
                  "src/main/resources/assets/royalarena/sounds/sfx")
    target.mkdir(parents=True, exist_ok=True)
    for old in target.glob("*.ogg"):
        old.unlink()
    for s, (name, (make, variants)) in enumerate(SOUNDS.items()):
        for v in range(1, variants + 1):
            rng = np.random.default_rng(609 + 100 * s + v)
            samples = norm(make(rng))
            fade = int(0.004 * SR)
            samples[-fade:] *= np.linspace(1, 0, fade)
            write_ogg(samples, target / f"{name}{v}.ogg")
        print(f"{name}: {variants} variants")


if __name__ == "__main__":
    main()
