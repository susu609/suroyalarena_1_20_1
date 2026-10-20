"""
Shared synthesis for the mod's music (compose_*.py): note-block style
instruments plus a few extra voices for the tense late pieces, a stereo
mixer, a light reverb and the final level / Ogg encoding.

numpy only; ffmpeg encodes the Ogg. Everything here is original.
"""
import subprocess
import wave
from pathlib import Path

import numpy as np

SR = 44100
rng = np.random.default_rng(609)


def hz(m):
    return 440.0 * 2.0 ** ((m - 69) / 12.0)


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


# ------------------------------------------------- voices for the late pieces
def strings(m, secs):
    """A slow, slightly detuned saw ensemble: a bowed section."""
    t = t_axis(secs)
    f = hz(m)
    tone = 0.0
    for detune in (-0.004, 0.0, 0.004):
        phase = 2 * np.pi * np.cumsum(np.full(len(t), f * (1 + detune))) / SR
        tone = tone + sum((1.0 / k ** 1.1) * np.sin(k * phase)
                          for k in range(1, 8))
    return tone * adsr(len(t), 0.12, 0.25)


def brass(m, secs):
    """A brighter, harder horn for the tense sections."""
    t = t_axis(secs)
    f = hz(m) * (1.0 + 0.004 * np.sin(2 * np.pi * 5.8 * t) *
                 np.minimum(1.0, t * 4.0))
    phase = 2 * np.pi * np.cumsum(f) / SR
    tone = sum((1.0 / k ** 0.9) * np.sin(k * phase) for k in range(1, 14))
    return tone * adsr(len(t), 0.03, 0.12)


def timpani(m, secs=0.9):
    t = t_axis(secs)
    f = hz(m)
    drop = f * (1.0 + 0.25 * np.exp(-t * 25))
    tone = np.sin(2 * np.pi * np.cumsum(drop) / SR)
    return (tone + 0.3 * rng.standard_normal(len(t)) * np.exp(-t * 60)) * \
        np.exp(-t * 4.5)


def crash(secs=2.2):
    t = t_axis(secs)
    noise = np.diff(rng.standard_normal(len(t) + 1))
    return noise * np.exp(-t * 2.2) * adsr(len(t), 0.002, 0.2)


def riser(secs):
    """Rising filtered noise that swells toward its end."""
    t = t_axis(secs)
    noise = np.diff(rng.standard_normal(len(t) + 1))
    sweep = np.sin(2 * np.pi * np.cumsum(300 + 3200 * (t / secs) ** 2) / SR)
    swell = (t / secs) ** 2
    return (0.6 * noise + 0.6 * sweep) * swell * adsr(len(t), 0.01, 0.01)


def tick(m=88):
    """A short bright click for countdowns."""
    t = t_axis(0.09)
    return np.sin(2 * np.pi * hz(m) * t) * np.exp(-t * 55)


def tom(m, secs=0.5):
    t = t_axis(secs)
    f = hz(m) * (1.0 + 0.5 * np.exp(-t * 30))
    return np.sin(2 * np.pi * np.cumsum(f) / SR) * np.exp(-t * 9)


def piano(m, secs):
    """A soft hammered string: slightly stretched partials, two detuned
    strings per note, upper partials dying first."""
    t = t_axis(secs)
    f = hz(m)
    tone = 0.0
    for k in range(1, 9):
        fk = f * k * np.sqrt(1 + 0.0004 * k * k)
        if fk > SR / 2.2:
            break
        decay = 1.4 + 0.8 * k + f / 500
        tone = tone + (1.0 / k ** 1.3) * np.exp(-t * decay) * (
            np.sin(2 * np.pi * fk * t) + 0.6 * np.sin(2 * np.pi * fk * 1.0012 * t))
    return tone * adsr(len(t), 0.002, 0.12)


# --------------------------------------------------------- parts -> sounds
# part name -> (function taking (note, secs), gain, pan)
VOICES = {
    "lead_bell": (bell, 0.36, 0.25),
    "lead_pling": (pling, 0.24, -0.3),
    "lead_horn": (horn, 0.26, 0.0),
    "lead_harp": (harp, 0.30, -0.2),
    "lead_brass": (brass, 0.22, 0.0),
    "strings": (strings, 0.10, 0.0),
    "bass": (bass, 1.0, 0.0),
    "arp": (harp, 1.0, 0.35),
    "pad": (horn, 1.0, 0.0),
    "tom": (tom, 0.5, -0.2),
    "piano": (piano, 0.16, -0.1),
}

# Parts that are not pushed or pulled by the beat (sustained or effects).
UNACCENTED = {"pad", "strings", "crash", "riser"}
DRUMS = {"kick", "snare", "hat", "tom", "timp", "crash", "riser", "tick"}


class Mix:
    def __init__(self, seconds, tail=0.0):
        self.seconds = seconds
        self.n = int((seconds + tail) * SR)
        self.buf = np.zeros((self.n, 2))

    def put(self, sound, at_seconds, pan=0.0, gain=1.0):
        start = int(at_seconds * SR)
        end = min(self.n, start + len(sound))
        if start >= self.n or end <= start:
            return
        left = np.cos((pan + 1) * np.pi / 4)
        right = np.sin((pan + 1) * np.pi / 4)
        chunk = sound[:end - start] * gain
        self.buf[start:end, 0] += chunk * left
        self.buf[start:end, 1] += chunk * right

    def play(self, events, step_seconds, humanize=True, bar_steps=16):
        """
        Render events (arena_march.py's shape) in order. humanize plays them
        like a person would: a few ms early or late (less for drums), the
        downbeat and the beats leaned on, the off 16ths lighter, and no two
        hits exactly as loud. Its own random stream, so the drum noise is
        unchanged.
        """
        feel = np.random.default_rng(1609)
        for e in events:
            part = e["part"]
            at = e["step"] * step_seconds
            g = e["gain"]
            if humanize:
                spread = 0.003 if part in DRUMS else 0.007
                at = max(0.0, at + float(np.clip(feel.normal(0, spread),
                                                 -2 * spread, 2 * spread)))
                g *= 1 + feel.uniform(-0.05, 0.05)
                if part not in UNACCENTED:
                    pos = e["step"] % bar_steps
                    g *= (1.12 if pos == 0 else 1.06 if pos % 4 == 0 else
                          0.94 if pos % 2 else 1.0)
            if part in VOICES:
                fn, gain, pan = VOICES[part]
                self.put(fn(e["note"], e["secs"]), at, pan, g * gain)
            elif part == "kick":
                self.put(kick(), at, 0.0, g)
            elif part == "snare":
                self.put(snare(e["arg"]), at, 0.1, g)
            elif part == "hat":
                self.put(hat(e["arg"]), at, -0.25, g)
            elif part == "timp":
                self.put(timpani(e["note"]), at, 0.0, g)
            elif part == "crash":
                self.put(crash(), at, 0.0, g)
            elif part == "riser":
                self.put(riser(e["secs"]), at, 0.0, g)
            elif part == "tick":
                self.put(tick(e["note"]), at, 0.3, g)
            else:
                raise KeyError(part)


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


def finish(mix, out, loop, rms_db=-19.0, peak_db=-3.0):
    """
    Reverb, level and encode. loop=True wraps the ring-out past the end onto
    the start (a seamless loop); loop=False cuts at the end with a short fade
    (a piece that is played once, exactly as long as its phase).
    """
    x = reverb(mix.buf)
    n = int(mix.seconds * SR)
    if loop:
        body = x[:n].copy()
        tail = x[n:]
        body[:len(tail)] += tail
    else:
        body = x[:n].copy()
        fade = int(0.15 * SR)
        body[-fade:] *= np.linspace(1.0, 0.0, fade)[:, None]
    # Gentle glue, then a music level that leaves room for the effects.
    body = np.tanh(body * 1.1) / np.tanh(1.1)
    body *= 10 ** (rms_db / 20) / np.sqrt(np.mean(body ** 2))
    assert np.max(np.abs(body)) < 10 ** (peak_db / 20), "too peaky for the level"
    body = body.astype(np.float32)

    out = Path(out)
    out.parent.mkdir(parents=True, exist_ok=True)
    wav = out.with_suffix(".wav")
    with wave.open(str(wav), "wb") as f:
        f.setnchannels(2)
        f.setsampwidth(2)
        f.setframerate(SR)
        f.writeframes((np.clip(body, -1, 1) * 32767).astype("<i2").tobytes())
    subprocess.run(["ffmpeg", "-y", "-loglevel", "error", "-i", str(wav),
                    "-c:a", "libvorbis", "-q:a", "4", str(out)], check=True)
    wav.unlink()
    peak = float(np.max(np.abs(body)))
    rms = float(np.sqrt(np.mean(body ** 2)))
    print(f"{out}: {mix.seconds:.1f} s, peak {20*np.log10(peak):.1f} dBFS, "
          f"rms {20*np.log10(rms):.1f} dBFS, {out.stat().st_size/1024:.0f} KiB")
