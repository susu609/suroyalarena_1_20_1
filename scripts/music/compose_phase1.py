#!/usr/bin/env python3
"""
Composes "Arena March", the music of a match's first phase (x1 elixir), and
writes it as a seamless 75 s loop.

    python3 scripts/music/compose_phase1.py [out.ogg]

Original tune and original synthesis (numpy only; ffmpeg encodes the Ogg):
a light heroic march at 128 bpm in C major, played on instruments that
imitate Minecraft's note blocks (bass, harp, bell, pling, snare, hat, kick)
plus a soft synthesised horn for the big tune. 40 bars of 4/4 = 75 s.

Form (8 bars each): A tune on bells / A again, fuller / B horn tune /
A with horn and bells / bridge, then back to the start.
Nothing here is taken from any other game's music.
"""
import subprocess
import sys
import wave
from pathlib import Path

import numpy as np

SR = 44100
BPM = 128
STEP = 60.0 / BPM / 4.0          # one sixteenth
BAR_STEPS = 16
BARS = 40
LOOP_SECONDS = BARS * BAR_STEPS * STEP   # 75.0
TAIL = 3.0                        # seconds of ring-out wrapped onto the start

NOTE_INDEX = {"C": 0, "D": 2, "E": 4, "F": 5, "G": 7, "A": 9, "B": 11}


def midi(name):
    """'C#5' / 'Bb3' -> MIDI number."""
    letter, rest = name[0], name[1:]
    shift = 0
    while rest and rest[0] in "#b":
        shift += 1 if rest[0] == "#" else -1
        rest = rest[1:]
    return 12 * (int(rest) + 1) + NOTE_INDEX[letter] + shift


def hz(m):
    return 440.0 * 2.0 ** ((m - 69) / 12.0)


rng = np.random.default_rng(609)


def t_axis(seconds):
    return np.arange(int(seconds * SR)) / SR


def adsr(n, attack, release):
    env = np.ones(n)
    a = min(n, int(attack * SR))
    r = min(n, int(release * SR))
    if a:
        env[:a] = np.linspace(0.0, 1.0, a)
    if r:
        env[n - r:] *= np.linspace(1.0, 0.0, r)
    return env


# ------------------------------------------------------------ instruments
def bass(m, secs):
    t = t_axis(secs)
    f = hz(m)
    tone = (np.sin(2 * np.pi * f * t) + 0.35 * np.sin(2 * np.pi * 2 * f * t) +
            0.12 * np.sin(2 * np.pi * 3 * f * t))
    return tone * np.exp(-t * 4.5) * adsr(len(t), 0.003, 0.03)


def harp(m, secs):
    t = t_axis(secs)
    f = hz(m)
    tone = sum(a * np.sin(2 * np.pi * k * f * t) * np.exp(-t * d)
               for k, a, d in ((1, 1.0, 3.0), (2, 0.4, 5.0), (3, 0.18, 7.0),
                               (4, 0.08, 9.0)))
    return tone * adsr(len(t), 0.002, 0.05)


def bell(m, secs):
    t = t_axis(secs)
    f = hz(m)
    tone = sum(a * np.sin(2 * np.pi * k * f * t) * np.exp(-t * d)
               for k, a, d in ((1, 1.0, 2.2), (2.0, 0.5, 3.0), (3.01, 0.3, 4.5),
                               (4.2, 0.22, 6.0), (5.4, 0.1, 8.0)))
    return tone * adsr(len(t), 0.001, 0.08)


def pling(m, secs):
    t = t_axis(secs)
    f = hz(m)
    # A soft square-ish pluck: odd harmonics, fast decay.
    tone = sum((1.0 / k) * np.sin(2 * np.pi * k * f * t) for k in (1, 3, 5, 7))
    return tone * np.exp(-t * 6.0) * adsr(len(t), 0.001, 0.04)


def horn(m, secs):
    t = t_axis(secs)
    f = hz(m) * (1.0 + 0.003 * np.sin(2 * np.pi * 5.2 * t) *
                 np.minimum(1.0, t * 3.0))
    phase = 2 * np.pi * np.cumsum(f) / SR
    # A saw softened by a falling harmonic series: brass-like, no harshness.
    tone = sum((1.0 / k ** 1.3) * np.sin(k * phase) for k in range(1, 10))
    return tone * adsr(len(t), 0.05, 0.18)


def kick():
    t = t_axis(0.28)
    f = 48 + 90 * np.exp(-t * 28)
    return np.sin(2 * np.pi * np.cumsum(f) / SR) * np.exp(-t * 11)


def snare(gain=1.0):
    t = t_axis(0.2)
    noise = rng.standard_normal(len(t))
    body = np.sin(2 * np.pi * 190 * t) * np.exp(-t * 30)
    return gain * (0.75 * noise * np.exp(-t * 22) + 0.45 * body)


def hat(gain=1.0):
    t = t_axis(0.06)
    noise = np.diff(rng.standard_normal(len(t) + 1))   # bright
    return gain * noise * np.exp(-t * 70)


# --------------------------------------------------------------- the score
CHORDS = {
    "C": (("C2", "C3"), ("C4", "E4", "G4")),
    "G": (("G2", "G3"), ("G3", "B3", "D4")),
    "Am": (("A2", "A3"), ("A3", "C4", "E4")),
    "F": (("F2", "F3"), ("F3", "A3", "C4")),
}

A_CHORDS = ["C", "G", "Am", "F", "C", "G", "F", "G"]
B_CHORDS = ["Am", "F", "C", "G", "Am", "F", "G", "G"]

# (step in bar, note, length in steps)
A_TUNE = [
    [(0, "E5", 3), (3, "G5", 1), (4, "C6", 4), (8, "G5", 4), (12, "E5", 2), (14, "D5", 2)],
    [(0, "D5", 3), (3, "G5", 1), (4, "B5", 4), (8, "G5", 4), (12, "D5", 4)],
    [(0, "C5", 3), (3, "E5", 1), (4, "A5", 4), (8, "E5", 4), (12, "C5", 2), (14, "D5", 2)],
    [(0, "A4", 3), (3, "C5", 1), (4, "F5", 4), (8, "E5", 2), (10, "D5", 2), (12, "C5", 4)],
    [(0, "E5", 3), (3, "G5", 1), (4, "C6", 4), (8, "B5", 2), (10, "C6", 2), (12, "D6", 4)],
    [(0, "B5", 4), (4, "G5", 4), (8, "D5", 4), (12, "G5", 4)],
    [(0, "A5", 4), (4, "F5", 4), (8, "C6", 4), (12, "A5", 4)],
    [(0, "G5", 6), (6, "A5", 2), (8, "B5", 4), (12, "D6", 4)],
]
B_TUNE = [
    [(0, "E5", 6), (6, "D5", 2), (8, "C5", 8)],
    [(0, "F5", 6), (6, "E5", 2), (8, "A5", 8)],
    [(0, "G5", 4), (4, "E5", 4), (8, "C5", 4), (12, "E5", 4)],
    [(0, "D5", 6), (6, "E5", 2), (8, "B4", 8)],
    [(0, "E5", 4), (4, "A5", 4), (8, "C6", 8)],
    [(0, "A5", 4), (4, "C6", 4), (8, "A5", 8)],
    [(0, "G5", 4), (4, "B5", 4), (8, "D6", 8)],
    [(0, "D6", 8), (8, "B5", 4), (12, "G5", 4)],
]

# What plays in each of the five 8-bar sections.
SECTIONS = [
    dict(chords=A_CHORDS, tune=A_TUNE, lead=("bell",), drums="light",
         harp=True, pad=False),
    dict(chords=A_CHORDS, tune=A_TUNE, lead=("pling", "bell"), drums="march",
         harp=True, pad=False),
    dict(chords=B_CHORDS, tune=B_TUNE, lead=("horn",), drums="march",
         harp=True, pad=True),
    dict(chords=A_CHORDS, tune=A_TUNE, lead=("horn", "bell"), drums="full",
         harp=True, pad=True),
    dict(chords=A_CHORDS, tune=A_TUNE, lead=("harp",), drums="kick",
         harp=False, pad=False),
]

INSTR = {"bell": bell, "pling": pling, "horn": horn, "harp": harp}
GAIN = {"bell": 0.36, "pling": 0.24, "horn": 0.26, "harp": 0.30}
PAN = {"bell": 0.25, "pling": -0.3, "horn": 0.0, "harp": -0.2, "bass": 0.0,
       "arp": 0.35, "pad": 0.0, "kick": 0.0, "snare": 0.1, "hat": -0.25}

N = int((LOOP_SECONDS + TAIL) * SR)
mix = np.zeros((N, 2))


def put(sound, start_step, pan, gain=1.0, step_offset=0.0):
    start = int((start_step * STEP + step_offset) * SR)
    end = min(N, start + len(sound))
    if start >= N or end <= start:
        return
    left = np.cos((pan + 1) * np.pi / 4)
    right = np.sin((pan + 1) * np.pi / 4)
    chunk = sound[:end - start] * gain
    mix[start:end, 0] += chunk * left
    mix[start:end, 1] += chunk * right


def note(kind, name, start_step, length_steps, gain=1.0, pan=None, tail=0.0):
    secs = length_steps * STEP + tail
    sound = INSTR[kind](midi(name), secs)
    put(sound, start_step, PAN[kind] if pan is None else pan,
        gain * GAIN[kind])


for s, sec in enumerate(SECTIONS):
    for b in range(8):
        bar = s * 8 + b
        base = bar * BAR_STEPS
        chord = sec["chords"][b]
        (bass_lo, bass_hi), triad = CHORDS[chord]

        # Bass: root on the beat, octave on the offbeat 8ths (note-block bass).
        for i, (step, name) in enumerate(((0, bass_lo), (4, bass_lo),
                                          (6, bass_hi), (8, bass_lo),
                                          (12, bass_lo), (14, bass_hi))):
            put(bass(midi(name), 0.5), base + step, PAN["bass"], 0.42)

        # Harp arpeggio in 8ths up and down the triad.
        if sec["harp"]:
            order = (0, 1, 2, 1, 0, 1, 2, 1)
            for i, k in enumerate(order):
                name = triad[k]
                m = midi(name) + 12
                put(harp(m, 0.6), base + i * 2, PAN["arp"], 0.16)

        # Sustained pad (horn, soft) under the big sections.
        if sec["pad"] and b % 2 == 0:
            for name in triad:
                put(horn(midi(name), 2 * BAR_STEPS * STEP), base, PAN["pad"],
                    0.05)

        # The tune.
        for step, name, length in sec["tune"][b]:
            for kind in sec["lead"]:
                shift = ""   # octave doubling for the bell under the horn
                n = name
                if kind == "bell" and "horn" in sec["lead"]:
                    n = name[:-1] + str(int(name[-1]) + 1)
                gain = 1.0 if kind != "bell" or "horn" not in sec["lead"] else 0.6
                note(kind, n, base + step, length, gain, tail=0.25)

        # Drums.
        d = sec["drums"]
        if d in ("light", "march", "full", "kick"):
            if d != "light" or b >= 4:
                for step in (0, 8):
                    put(kick(), base + step, PAN["kick"], 0.65)
        if d in ("march", "full") or (d == "light" and b >= 4):
            for step in (4, 12):
                put(snare(0.8), base + step, PAN["snare"], 0.45)
            put(snare(0.4), base + 14, PAN["snare"], 0.25)
        if d in ("light", "march", "full"):
            for step in range(0, 16, 2):
                loud = 1.0 if step % 4 == 0 else 0.55
                put(hat(loud), base + step, PAN["hat"], 0.24)
        if d == "full" and b == 7:
            # A snare roll into the bridge.
            for step in (10, 11, 12, 13, 14, 15):
                put(snare(0.6), base + step, PAN["snare"], 0.3)

# --------------------------------------------------------- space and finish


def reverb(x, wet=0.16):
    ir_len = int(0.55 * SR)
    t = np.arange(ir_len) / SR
    ir = rng.standard_normal((ir_len, 2)) * np.exp(-t * 7.0)[:, None]
    ir[:int(0.012 * SR)] = 0.0
    out = np.zeros_like(x)
    size = 1 << int(np.ceil(np.log2(len(x) + ir_len)))
    for c in range(2):
        conv = np.fft.irfft(np.fft.rfft(x[:, c], size) *
                            np.fft.rfft(ir[:, c], size), size)[:len(x)]
        out[:, c] = conv
    out *= np.max(np.abs(x)) / (np.max(np.abs(out)) + 1e-9)
    return x * (1 - wet) + out * wet


mix = reverb(mix)

# Wrap the ring-out of the end onto the beginning for a seamless loop.
loop_n = int(LOOP_SECONDS * SR)
tail = mix[loop_n:]
loop = mix[:loop_n].copy()
loop[:len(tail)] += tail

# Gentle glue: soft limiter, then a music level (-19 dBFS rms) that leaves
# room for the sound effects; the peak stays under -3 dBFS.
loop = np.tanh(loop * 1.1) / np.tanh(1.1)
loop *= 10 ** (-19.0 / 20) / np.sqrt(np.mean(loop ** 2))
assert np.max(np.abs(loop)) < 10 ** (-3.0 / 20), "too peaky for the level"
loop = loop.astype(np.float32)

# ------------------------------------------------------------------ output
out = Path(sys.argv[1] if len(sys.argv) > 1 else
           "src/main/resources/assets/royalarena/sounds/music/arena_phase1.ogg")
out.parent.mkdir(parents=True, exist_ok=True)
wav = out.with_suffix(".wav")
with wave.open(str(wav), "wb") as f:
    f.setnchannels(2)
    f.setsampwidth(2)
    f.setframerate(SR)
    f.writeframes((np.clip(loop, -1, 1) * 32767).astype("<i2").tobytes())
subprocess.run(["ffmpeg", "-y", "-loglevel", "error", "-i", str(wav),
                "-c:a", "libvorbis", "-q:a", "4", str(out)], check=True)
wav.unlink()

peak = float(np.max(np.abs(loop)))
rms = float(np.sqrt(np.mean(loop ** 2)))
print(f"{out}: {LOOP_SECONDS:.1f} s, peak {20*np.log10(peak):.1f} dBFS, "
      f"rms {20*np.log10(rms):.1f} dBFS, {out.stat().st_size/1024:.0f} KiB")
