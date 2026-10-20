#!/usr/bin/env python3
"""
Coarse descriptors of a short sound, to match a synthesised sound to the
feel of a reference by numbers (the reference itself is never copied):

    python3 scripts/sfx/analyze.py a.ogg [b.ogg ...]

length   until the level stays 40 dB under the peak
attack   start to peak
decay    peak to 20 dB under it
hits     separate onsets (a rattle has many, a twang one)
tonal    1 - spectral flatness of the loud part (0 noise, 1 pure tone)
peaks    the three strongest frequencies of the first 120 ms
centroid brightness at 0-30 ms, 30-120 ms and after
bands    share of energy under 250 Hz, 250-1k, 1k-4k, over 4k
"""
import subprocess
import sys

import numpy as np

SR = 44100


def load(path):
    raw = subprocess.run(["ffmpeg", "-loglevel", "error", "-i", path, "-f",
                          "f32le", "-ac", "1", "-ar", str(SR), "-"],
                         capture_output=True, check=True).stdout
    return np.frombuffer(raw, "<f4").astype(np.float64)


def envelope(x, win=0.005):
    n = max(1, int(win * SR))
    frames = len(x) // n
    return np.sqrt(np.mean(x[:frames * n].reshape(frames, n) ** 2, axis=1) +
                   1e-12), n / SR


def centroid(seg):
    if len(seg) < 64:
        return 0.0
    spec = np.abs(np.fft.rfft(seg * np.hanning(len(seg))))
    f = np.fft.rfftfreq(len(seg), 1 / SR)
    return float(np.sum(f * spec) / (np.sum(spec) + 1e-12))


def describe(x):
    env, dt = envelope(x)
    db = 20 * np.log10(env / env.max())
    peak_i = int(np.argmax(env))
    above = np.where(db > -40)[0]
    length = (above[-1] + 1) * dt
    after = np.where(db[peak_i:] < -20)[0]
    decay = (after[0] if len(after) else len(db) - peak_i) * dt
    rise = np.diff(db)
    hits = int(np.sum((rise[1:] > 6) & (db[2:] > -25)))
    loud = x[:int(min(len(x), max(length, 0.05) * SR))]
    spec = np.abs(np.fft.rfft(loud * np.hanning(len(loud)))) + 1e-12
    flatness = float(np.exp(np.mean(np.log(spec))) / np.mean(spec))
    f = np.fft.rfftfreq(len(loud), 1 / SR)
    first = x[:int(0.12 * SR)]
    s1 = np.abs(np.fft.rfft(first * np.hanning(len(first))))
    f1 = np.fft.rfftfreq(len(first), 1 / SR)
    peaks = []
    for i in np.argsort(s1)[::-1]:
        if f1[i] < 60:
            continue
        if all(abs(f1[i] - p) > 40 for p in peaks):
            peaks.append(float(f1[i]))
        if len(peaks) == 3:
            break
    power = spec ** 2
    bands = [float(np.sum(power[(f >= lo) & (f < hi)]) / np.sum(power))
             for lo, hi in ((0, 250), (250, 1000), (1000, 4000), (4000, SR))]
    a, b = int(0.03 * SR), int(0.12 * SR)
    return dict(length=length, attack=peak_i * dt, decay=decay, hits=hits,
                tonal=1 - flatness, peaks=peaks,
                centroid=(centroid(x[:a]), centroid(x[a:b]),
                          centroid(x[b:int(length * SR)])),
                bands=bands)


def line(name, d):
    c = "/".join(f"{v / 1000:.1f}" for v in d["centroid"])
    bands = " ".join(f"{v * 100:3.0f}" for v in d["bands"])
    peaks = ",".join(f"{p:.0f}" for p in d["peaks"])
    return (f"{name:34s} len {d['length']:.2f} att {d['attack'] * 1000:3.0f}ms "
            f"dec {d['decay'] * 1000:4.0f}ms hits {d['hits']:2d} "
            f"tonal {d['tonal']:.2f} cent {c} kHz bands% {bands} "
            f"peaks {peaks}")


if __name__ == "__main__":
    for path in sys.argv[1:]:
        print(line(path.rsplit("/", 1)[-1][:-4], describe(load(path))))
