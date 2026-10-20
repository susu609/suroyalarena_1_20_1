"""
The scores of the two later pieces of the battle music, timed to a match
(3:00 regulation, then 2:00 sudden death):

  "Arena Rush"   2:00 -> 3:00, A minor. 36 bars at 144 bpm = exactly 60 s.
                 bars 0-17   x2 elixir, "60 seconds left" (0:00-0:30 of it)
                 bars 18-29  "30 seconds left": more layers, hits
                 bars 30-35  "10 seconds left": a countdown, riser, snare roll
  "Last Stand"   3:00 -> 5:00, D minor. 72 bars at 144 bpm = exactly 120 s.
                 bars 0-35   sudden death
                 bars 36-65  "60 seconds left": brass and bells join, busier
                 bars 66-71  "10 seconds left": countdown

At 144 bpm one bar is 5/3 s, so every phase boundary of a match falls on a
bar line. Both pieces are played once (not looped), exactly as long as their
phase, and open on a hit (cymbal, timpani, a low brass chord). Original
tunes: the hook of "Arena March" comes back in minor, over a harmonic-minor
dominant (E7, A7).

Same event shape as arena_march.py (the synthesiser and the MIDI export both
read it).
"""
from arranger import (BAR_STEPS, arp_bar, at, bass_bar, drums_bar, ev,
                      scaled)
from notes import chord, midi

BPM = 144
STEP = 60.0 / BPM / 4.0          # one sixteenth

RUSH_BARS = 36
LAST_STAND_BARS = 72
RUSH_SECONDS = RUSH_BARS * BAR_STEPS * STEP          # 60.0
LAST_STAND_SECONDS = LAST_STAND_BARS * BAR_STEPS * STEP   # 120.0

A_MINOR = {
    "Am": chord("A2", "A3", "C4", "E4"),
    "Em/G": chord("G2", "E3", "G3", "B3"),
    "F": chord("F2", "F3", "A3", "C4"),
    "E7": chord("E2", "E3", "G#3", "B3", "D4"),
    "Dm": chord("D2", "D3", "F3", "A3"),
    "E": chord("E2", "E3", "G#3", "B3"),
}
D_MINOR = {
    "Dm": chord("D2", "D3", "F3", "A3"),
    "Bb": chord("Bb1", "Bb3", "D4", "F4"),
    "Gm": chord("G2", "G3", "Bb3", "D4"),
    "A7": chord("A1", "A3", "C#4", "E4", "G4"),
    "F/C": chord("C2", "F3", "A3", "C4"),
    "A": chord("A1", "A3", "C#4", "E4"),
}
RUSH_CHORDS = ["Am", "Em/G", "F", "E7", "Am", "Dm", "F", "E"]
STAND_CHORDS = ["Dm", "Bb", "Gm", "A7", "Dm", "F/C", "Bb", "A"]

# (step in bar, note, length in steps), one list per bar of the 8.
RUSH_TUNE = [
    [(0, "E4", 3), (3, "A4", 1), (4, "E5", 6), (10, "C5", 2), (12, "D5", 2), (14, "C5", 2)],
    [(0, "B4", 4), (4, "G4", 2), (6, "B4", 2), (8, "E4", 6)],
    [(0, "F4", 3), (3, "A4", 1), (4, "F5", 6), (10, "E5", 2), (12, "C5", 2), (14, "A4", 2)],
    [(0, "G#4", 4), (4, "B4", 2), (6, "D5", 2), (8, "E5", 6)],
    [(0, "E5", 3), (3, "E5", 1), (4, "A5", 6), (10, "G5", 2), (12, "E5", 2), (14, "C5", 2)],
    [(0, "D5", 4), (4, "F5", 2), (6, "A5", 2), (8, "F5", 4), (12, "D5", 4)],
    [(0, "C5", 3), (3, "F5", 1), (4, "A5", 4), (8, "G5", 2), (10, "F5", 2), (12, "E5", 4)],
    [(0, "E5", 8), (8, "G#4", 4), (12, "B4", 4)],
]
STAND_TUNE = [
    [(0, "A4", 3), (3, "D5", 1), (4, "A5", 6), (10, "F5", 2), (12, "G5", 2), (14, "F5", 2)],
    [(0, "D5", 4), (4, "Bb4", 2), (6, "D5", 2), (8, "F4", 6)],
    [(0, "G4", 3), (3, "Bb4", 1), (4, "G5", 6), (10, "F5", 2), (12, "D5", 2), (14, "Bb4", 2)],
    [(0, "C#5", 4), (4, "E5", 2), (6, "G5", 2), (8, "A5", 6)],
    [(0, "A5", 3), (3, "A5", 1), (4, "D6", 6), (10, "C6", 2), (12, "A5", 2), (14, "F5", 2)],
    [(0, "C6", 4), (4, "A5", 2), (6, "F5", 2), (8, "A5", 4), (12, "C6", 4)],
    [(0, "D6", 3), (3, "Bb5", 1), (4, "F5", 4), (8, "G5", 2), (10, "A5", 2), (12, "Bb5", 4)],
    [(0, "A5", 8), (8, "C#5", 4), (12, "E5", 4)],
]

# What the countdown stabs on (the fifth above the pedal).
RUSH_STAB = ("A5", "C6", "E6")
STAND_STAB = ("D5", "F5", "A5")


def _bar(out, bar, chords, next_bass, tune, layers, pedal, stab):
    """Every event of one bar for the layers switched on."""
    base = bar * BAR_STEPS
    bars = [chords]

    if "bass" in layers:
        bass_bar(out, base, bars, next_bass, bar, STEP, "drive", 0.5)
    if "pedal" in layers:
        for step in range(0, 16, 2):
            out.append(ev("bass", midi(pedal), base + step, 2, 0.4, 0.4, 96))
    if "arp8" in layers or "arp16" in layers:
        arp_bar(out, base, bars, bar, 2 if "arp8" in layers else 1, 0.14,
                secs=0.5, sparse=bar % 8 == 7)
    if "strings" in layers:
        for m in chords[1][:3]:
            out.append(ev("strings", m, base, BAR_STEPS, BAR_STEPS * STEP,
                          1.0, 52))
    if "tune" in layers:
        for step, name, length in tune[bar % 8]:
            secs = length * STEP + 0.25
            if "horn" in layers:
                out.append(ev("lead_horn", midi(name), base + step, length,
                              secs, 1.0, 100))
            if "brass" in layers:
                out.append(ev("lead_brass", midi(name), base + step, length,
                              secs, 1.0, 104))
            if "bell8" in layers:
                out.append(ev("lead_bell", midi(name) + 12, base + step,
                              length, secs, 0.6, 76))
    if "stabs" in layers:
        for step in (0, 4, 8, 12):
            for name in stab:
                out.append(ev("lead_brass", midi(name), base + step, 2,
                              2 * STEP + 0.2, 0.9, 100))

    # Drums: a groove with fills, or the countdown's straight hits.
    for kit in ("drive", "dark"):
        if kit in layers:
            drums_bar(out, base, bar, kit)
    if "kick4" in layers:
        for step in (0, 4, 8, 12):
            out.append(ev("kick", None, base + step, 1, None, 0.65, 112))
    if "snare8" in layers:
        for step in range(0, 16, 2):
            out.append(ev("snare", None, base + step, 1, None, 0.45, 80,
                          arg=0.7))
    if "snare16" in layers:
        for step in range(16):
            out.append(ev("snare", None, base + step, 1, None, 0.55,
                          60 + step * 4, arg=0.8))
    if "timp" in layers:
        lo = chords[0]
        out.append(ev("timp", lo + (12 if lo < 40 else 0), base, 2, None,
                      0.5, 108))
    if "tick" in layers:
        for step in (0, 4, 8, 12):
            out.append(ev("tick", 88 if step else 95, base + step, 1, None,
                          0.8, 90))


def _hit(out, bar, spec):
    """The opening hit: cymbal, timpani on the root, a low brass chord."""
    base = bar * BAR_STEPS
    lo, tones = spec
    out.append(ev("timp", lo + (12 if lo < 40 else 0), base, 2, None, 0.7,
                  120))
    for m in tones[:3]:
        out.append(ev("lead_brass", m, base, 4, 4 * STEP + 0.5, 1.1, 110))


def _piece(bars, chords, progression, tune, zones, pedal, stab):
    """
    zones: list of (first_bar, layers, crash_at_start, dynamic); each runs to
    the next.
    """
    out = []
    _hit(out, 0, chords[progression[0]])
    for z, (first, layers, crash_at_start, dyn) in enumerate(zones):
        last = zones[z + 1][0] if z + 1 < len(zones) else bars
        zone = []
        if crash_at_start:
            zone.append(ev("crash", None, first * BAR_STEPS, 1, None, 0.5,
                           112))
        for bar in range(first, last):
            spec = chords[progression[bar % 8]]
            next_bass = chords[progression[(bar + 1) % 8]][0]
            _bar(zone, bar, spec, next_bass, tune(bar), layers, pedal, stab)
        # A countdown also gets a riser from its first bar.
        if "riser" in layers:
            zone.append(ev("riser", None, first * BAR_STEPS, 1,
                           (last - first) * BAR_STEPS * STEP, 0.8, 100))
        out.extend(scaled(zone, dyn))
    return out


COUNTDOWN = {"pedal", "tick", "stabs", "kick4", "snare8", "riser"}
ROLL = COUNTDOWN - {"snare8"} | {"snare16", "timp"}


def rush_events():
    base = {"bass", "arp8", "strings", "tune", "horn", "drive"}
    more = {"bass", "arp16", "strings", "tune", "horn", "brass", "bell8",
            "drive", "timp"}
    zones = [
        (0, base, True, 0.95),
        (18, more, True, 1.05),
        (30, COUNTDOWN, False, 1.0),
        (33, ROLL, False, 1.0),
    ]
    return _piece(RUSH_BARS, A_MINOR, RUSH_CHORDS, lambda bar: RUSH_TUNE,
                  zones, "A2", RUSH_STAB)


def last_stand_events():
    dark = {"bass", "strings", "tune", "horn", "dark", "timp"}
    busier = {"bass", "arp16", "strings", "tune", "horn", "brass", "bell8",
              "drive", "timp"}
    zones = [
        (0, dark, True, 0.95),
        (36, busier, True, 1.08),
        (66, COUNTDOWN, False, 1.0),
        (69, ROLL, False, 1.0),
    ]
    return _piece(LAST_STAND_BARS, D_MINOR, STAND_CHORDS,
                  lambda bar: STAND_TUNE, zones, "D2", STAND_STAB)
