#!/usr/bin/env python3
"""
Synthesises the battle's shot and spell-cast sounds (original, no game's
audio): bow shots for archers and towers, the arrow volley, and the casts of
Zap, Freeze and Poison. Reads the vanilla feel of the bow (a string
twang over an air whoosh) and of Clash Royale's spells (a cast that swells,
then a clear shape), and mixes them in the mod's own way.

    python3 scripts/sfx/compose_sfx.py [out_dir]

Writes one Ogg per sound into assets/royalarena/sounds/sfx/. Each file is
quiet (peak -14 dBFS) because the game plays them at volume 2.5, which
carries them 40 blocks instead of vanilla's 16 (see ArenaSfx.java).
numpy and ffmpeg, like the music.
"""
import subprocess
import sys
import wave
from pathlib import Path

import numpy as np

sys.path.insert(0, str(Path(__file__).resolve().parent.parent / "music"))
from synth import SR, t_axis, adsr, rng  # noqa: E402

PEAK = 10 ** (-14 / 20)


def _sweep(f0, f1, secs, shape=1.0):
    """Phase of a tone gliding from f0 to f1 Hz over secs."""
    t = t_axis(secs)
    f = f0 + (f1 - f0) * (t / secs) ** shape
    return 2 * np.pi * np.cumsum(f) / SR


def _whoosh(secs, center, width, peak_at=0.3):
    """Band-limited air: noise through a crude resonant bandpass swell."""
    t = t_axis(secs)
    noise = np.diff(rng.standard_normal(len(t) + 1))
    # Two cascaded one-pole filters around the centre frequency.
    alpha = np.clip(2 * np.pi * center / SR, 0.01, 0.9)
    low = np.zeros_like(noise)
    acc = 0.0
    for i, x in enumerate(noise):
        acc += alpha * (x - acc)
        low[i] = acc
    band = low - np.convolve(low, np.ones(max(1, int(width * SR))) /
                             max(1, int(width * SR)), mode="same")
    swell = np.sin(np.pi * np.clip(t / secs, 0, 1) ** peak_at)
    return band * swell


def _string(f, secs, drop=0.0):
    """A plucked string: a bright start that dies in a few hundred ms."""
    t = t_axis(secs)
    freq = f * (1 - drop * (1 - np.exp(-t * 30)))
    phase = 2 * np.pi * np.cumsum(freq) / SR
    tone = sum((1.0 / k) * np.sin(k * phase) * np.exp(-t * (3 + 2 * k))
               for k in range(1, 6))
    return tone * adsr(len(t), 0.001, 0.02)


def _click(secs=0.01):
    t = t_axis(secs)
    return rng.standard_normal(len(t)) * np.exp(-t * 900)


def _norm(x):
    return x * PEAK / np.max(np.abs(x))


def _place(buf, sound, at):
    start = int(at * SR)
    end = min(len(buf), start + len(sound))
    buf[start:end] += sound[:end - start]


# ----------------------------------------------------------------- sounds
def bow_shot():
    n = 0.45
    out = np.zeros(int(n * SR) + 10)
    _place(out, _string(230, 0.35, drop=0.3) * 0.9, 0.0)
    _place(out, _whoosh(0.3, 2400, 0.0015) * 0.35, 0.02)
    _place(out, _click() * 0.5, 0.0)
    return _norm(out)


def tower_shot():
    # A heavier bow: a deeper string, a thump, and a longer air swell.
    out = np.zeros(int(0.7 * SR) + 10)
    t = t_axis(0.12)
    thump = np.sin(2 * np.pi * np.cumsum(90 + 60 * np.exp(-t * 40)) / SR)
    _place(out, thump * np.exp(-t * 30) * 0.9, 0.0)
    _place(out, _string(170, 0.45, drop=0.25) * 0.8, 0.02)
    _place(out, _whoosh(0.5, 1500, 0.0025, peak_at=0.5) * 0.4, 0.04)
    return _norm(out)


def volley():
    # Arrow Storm: five twangs in a staggered cluster, then the swish.
    out = np.zeros(int(0.95 * SR) + 10)
    for i, (at, f) in enumerate(((0.0, 230), (0.05, 260), (0.11, 210),
                                 (0.16, 250), (0.23, 225))):
        _place(out, _string(f, 0.3, drop=0.2) * (0.8 - 0.07 * i), at)
    _place(out, _whoosh(0.7, 2000, 0.002, peak_at=0.4) * 0.45, 0.0)
    return _norm(out)


def zap_cast():
    # A crackle of sparks falling into a short electric sweep.
    n = 0.5
    t = t_axis(n)
    saw = 2 * ((_sweep(1900, 260, n, shape=0.6) / (2 * np.pi)) % 1) - 1
    sweep = saw * np.exp(-t * 6) * 0.5
    out = np.zeros(int(n * SR) + 10)
    out[:len(sweep)] += sweep
    for at in np.sort(rng.uniform(0.0, 0.42, 14)):
        _place(out, _click(0.02) * rng.uniform(0.4, 1.0), at)
    return _norm(out)


def freeze_cast():
    # A rising glassy shimmer with ice ticks.
    n = 0.8
    t = t_axis(n)
    out = np.zeros(int(n * SR) + 10)
    for k, a in ((1, 0.5), (2.01, 0.25), (3.0, 0.15)):
        phase = _sweep(1200 * k, 2600 * k, n, shape=1.4)
        out[:len(t)] += a * np.sin(phase) * adsr(len(t), 0.15, 0.3)
    for at in (0.12, 0.3, 0.47, 0.61):
        tick = np.sin(2 * np.pi * 3200 * t_axis(0.05)) * np.exp(-t_axis(0.05) * 120)
        _place(out, tick * 0.4, at)
    return _norm(out)


def poison_cast():
    # Bubbles rising through a gurgle.
    n = 0.8
    out = np.zeros(int(n * SR) + 10)
    for at in np.sort(rng.uniform(0.0, 0.65, 11)):
        f0 = rng.uniform(260, 340)
        secs = rng.uniform(0.06, 0.1)
        t = t_axis(secs)
        f = f0 * (1 + 1.2 * t / secs)
        bubble = np.sin(2 * np.pi * np.cumsum(f) / SR) * np.exp(-t * 35)
        _place(out, bubble * rng.uniform(0.5, 0.9), at)
    _place(out, _whoosh(0.8, 500, 0.004, peak_at=0.6) * 0.25, 0.0)
    return _norm(out)



SOUNDS = {
    "bow_shot": bow_shot,
    "tower_shot": tower_shot,
    "volley": volley,
    "zap_cast": zap_cast,
    "freeze_cast": freeze_cast,
    "poison_cast": poison_cast,
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
    for name, make in SOUNDS.items():
        samples = make()
        write_ogg(samples, target / f"{name}.ogg")
        peak = 20 * np.log10(np.max(np.abs(samples)))
        print(f"{name}: {len(samples) / SR:.2f} s, peak {peak:.1f} dBFS")


if __name__ == "__main__":
    main()
