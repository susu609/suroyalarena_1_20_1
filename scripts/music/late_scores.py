"""
The scores of the two later pieces of the battle music, timed to a match
(3:00 regulation, then 2:00 sudden death):

  "Arena Rush"   2:00 -> 3:00, A minor. 36 bars at 144 bpm = exactly 60 s.
                 bars 0-17   x2 elixir, "60 seconds left" (0:00-0:30 of it)
                 bars 18-29  "30 seconds left": more layers, hits
                 bars 30-35  "10 seconds left": a countdown, riser, snare roll
  "Last Stand"   3:00 -> 5:00, D minor. 72 bars at 144 bpm = exactly 120 s.
                 bars 0-35   sudden death
                 bars 36-65  "60 seconds left": octave up, busier
                 bars 66-71  "10 seconds left": countdown

At 144 bpm one bar is 5/3 s, so every phase boundary of a match falls on a
bar line. Both pieces are played once (not looped), exactly as long as their
phase. Original tunes; the tune of "Arena March" reappears in minor.

Same event shape as arena_march.py (the synthesiser and the MIDI export both
read it).
"""
from arena_march import midi

BPM = 144
STEP = 60.0 / BPM / 4.0          # one sixteenth
BAR_STEPS = 16

RUSH_BARS = 36
LAST_STAND_BARS = 72
RUSH_SECONDS = RUSH_BARS * BAR_STEPS * STEP          # 60.0
LAST_STAND_SECONDS = LAST_STAND_BARS * BAR_STEPS * STEP   # 120.0


def _ev(part, note, step, steps, secs, gain, velocity, arg=None):
    return dict(part=part, note=note, step=step, steps=steps, secs=secs,
                gain=gain, velocity=velocity, arg=arg)


# chord -> (bass low, bass high, triad)
A_MINOR = {
    "Am": ("A2", "A3", ("A3", "C4", "E4")),
    "F": ("F2", "F3", ("F3", "A3", "C4")),
    "C": ("C3", "C4", ("C4", "E4", "G4")),
    "G": ("G2", "G3", ("G3", "B3", "D4")),
}
D_MINOR = {
    "Dm": ("D2", "D3", ("D3", "F3", "A3")),
    "Bb": ("Bb1", "Bb2", ("Bb3", "D4", "F4")),
    "F": ("F2", "F3", ("F3", "A3", "C4")),
    "C": ("C2", "C3", ("C4", "E4", "G4")),
}

RUSH_TUNE = {
    "Am": [(0, "E5", 3), (3, "A5", 1), (4, "C6", 4), (8, "A5", 4), (12, "E5", 2), (14, "A5", 2)],
    "F": [(0, "F5", 3), (3, "A5", 1), (4, "C6", 4), (8, "A5", 4), (12, "F5", 2), (14, "A5", 2)],
    "C": [(0, "G5", 3), (3, "C6", 1), (4, "E6", 4), (8, "C6", 4), (12, "G5", 2), (14, "C6", 2)],
    "G": [(0, "B5", 3), (3, "D6", 1), (4, "G6", 4), (8, "D6", 4), (12, "B5", 2), (14, "D6", 2)],
}
STAND_TUNE_1 = {
    "Dm": [(0, "D5", 2), (2, "D5", 2), (4, "F5", 4), (8, "A5", 4), (12, "G5", 2), (14, "F5", 2)],
    "Bb": [(0, "Bb4", 2), (2, "Bb4", 2), (4, "D5", 4), (8, "F5", 4), (12, "E5", 2), (14, "D5", 2)],
    "F": [(0, "C5", 2), (2, "C5", 2), (4, "F5", 4), (8, "A5", 4), (12, "C6", 4)],
    "C": [(0, "E5", 2), (2, "E5", 2), (4, "G5", 4), (8, "C6", 4), (12, "B5", 2), (14, "A5", 2)],
}
STAND_TUNE_2 = {
    "Dm": [(0, "A5", 4), (4, "D6", 4), (8, "C6", 4), (12, "A5", 4)],
    "Bb": [(0, "F5", 4), (4, "Bb5", 4), (8, "A5", 4), (12, "F5", 4)],
    "F": [(0, "A5", 4), (4, "C6", 4), (8, "F6", 4), (12, "C6", 4)],
    "C": [(0, "G5", 4), (4, "C6", 4), (8, "B5", 4), (12, "G5", 4)],
}

# What the countdown stabs on (the fifth above the pedal).
RUSH_STAB = ("A5", "C6", "E6")
STAND_STAB = ("D5", "F5", "A5")


def _octave(name, up):
    return name[:-1] + str(int(name[-1]) + up)


def _bar(out, bar, chord, chords, tune, layers, pedal, stab, bars):
    """Every event of one bar for the layers switched on."""
    base = bar * BAR_STEPS
    lo, hi, triad = chords[chord]

    if "bass" in layers:
        for step in range(0, 16, 2):
            name = lo if step % 4 == 0 else hi
            out.append(_ev("bass", midi(name), base + step, 2, 0.4, 0.5, 90))
    if "pedal" in layers:
        for step in range(0, 16, 2):
            out.append(_ev("bass", midi(pedal), base + step, 2, 0.4, 0.4, 96))
    if "arp8" in layers or "arp16" in layers:
        stepsize = 2 if "arp8" in layers else 1
        for i in range(16 // stepsize):
            out.append(_ev("arp", midi(triad[(0, 1, 2, 1)[i % 4]]) + 12,
                           base + i * stepsize, stepsize, 0.5, 0.14, 62))
    if "strings" in layers:
        for name in triad:
            out.append(_ev("strings", midi(name), base, BAR_STEPS,
                           BAR_STEPS * STEP, 1.0, 52))
    if "tune" in layers:
        for step, name, length in tune[chord]:
            if "horn" in layers:
                out.append(_ev("lead_horn", midi(name), base + step, length,
                               length * STEP + 0.25, 1.0, 100))
            if "brass" in layers:
                out.append(_ev("lead_brass", midi(name), base + step, length,
                               length * STEP + 0.25, 1.0, 104))
            if "bell8" in layers:
                out.append(_ev("lead_bell", midi(_octave(name, 1)),
                               base + step, length, length * STEP + 0.25,
                               0.6, 76))
    if "stabs" in layers:
        for step in (0, 4, 8, 12):
            for name in stab:
                out.append(_ev("lead_brass", midi(name), base + step, 2,
                               2 * STEP + 0.2, 0.9, 100))

    # Drums.
    if "kick4" in layers:
        for step in (0, 4, 8, 12):
            out.append(_ev("kick", None, base + step, 1, None, 0.65, 112))
    if "kick2" in layers:
        for step in (0, 8):
            out.append(_ev("kick", None, base + step, 1, None, 0.65, 112))
    if "snare24" in layers:
        for step in (4, 12):
            out.append(_ev("snare", None, base + step, 1, None, 0.5, 100,
                           arg=0.8))
    if "snare8" in layers:
        for step in range(0, 16, 2):
            out.append(_ev("snare", None, base + step, 1, None, 0.45, 80,
                           arg=0.7))
    if "snare16" in layers:
        for step in range(16):
            out.append(_ev("snare", None, base + step, 1, None, 0.55,
                           60 + step * 4, arg=0.8))
    if "hat8" in layers:
        for step in range(0, 16, 2):
            loud = 1.0 if step % 4 == 0 else 0.55
            out.append(_ev("hat", None, base + step, 1, None, 0.24,
                           72 if loud == 1.0 else 46, arg=loud))
    if "hat16" in layers:
        for step in range(16):
            loud = 1.0 if step % 4 == 0 else 0.5
            out.append(_ev("hat", None, base + step, 1, None, 0.2,
                           72 if loud == 1.0 else 40, arg=loud))
    if "timp" in layers:
        out.append(_ev("timp", midi(lo) + (12 if midi(lo) < 40 else 0), base,
                       2, None, 0.5, 108))
    if "fill" in layers:
        for step, name in zip((12, 13, 14, 15), ("A3", "G3", "E3", "D3")):
            out.append(_ev("tom", midi(name), base + step, 1, 0.5, 1.0, 96))
    if "tick" in layers:
        for step in (0, 4, 8, 12):
            out.append(_ev("tick", 88 if step else 95, base + step, 1, None,
                           0.8, 90))


def _piece(bars, chords, progression, tune_for, zones, pedal, stab):
    """
    zones: list of (first_bar, layers, crash_at_start); each runs to the next.
    """
    out = []
    for z, (first, layers, crash_at_start) in enumerate(zones):
        last = zones[z + 1][0] if z + 1 < len(zones) else bars
        if crash_at_start:
            out.append(_ev("crash", None, first * BAR_STEPS, 1, None, 0.5,
                           112))
        for bar in range(first, last):
            chord = progression[bar % 4]
            _bar(out, bar, chord, chords, tune_for(bar), layers, pedal, stab,
                 bars)
        # The last bars of a countdown also get a riser from its first bar.
        if "riser" in layers:
            out.append(_ev("riser", None, first * BAR_STEPS, 1,
                           (last - first) * BAR_STEPS * STEP, 0.8, 100))
    return out


def rush_events():
    base = {"bass", "arp8", "strings", "tune", "horn", "kick4", "snare24",
            "hat8"}
    more = {"bass", "arp16", "strings", "tune", "horn", "brass", "bell8",
            "kick4", "snare24", "hat16", "timp"}
    countdown = {"pedal", "tick", "stabs", "kick4", "snare8", "riser"}
    zones = [
        (0, base, True),
        (18, more, True),
        (30, countdown, False),
        (33, countdown - {"snare8"} | {"snare16", "timp"}, False),
    ]
    out = _piece(RUSH_BARS, A_MINOR, ["Am", "F", "C", "G"],
                 lambda bar: RUSH_TUNE, zones, "A2", RUSH_STAB)
    # A tom fill at the end of each four bars in the middle zone.
    for bar in range(21, 30, 4):
        for step, name in zip((12, 13, 14, 15), ("A3", "G3", "E3", "D3")):
            out.append(_ev("tom", midi(name), bar * BAR_STEPS + step, 1, 0.5,
                           1.0, 96))
    return out


def last_stand_events():
    dark = {"bass", "strings", "tune", "horn", "kick2", "snare24", "hat8",
            "timp"}
    busier = {"bass", "arp16", "strings", "tune", "horn", "brass", "bell8",
              "kick4", "snare24", "hat16", "timp"}
    countdown = {"pedal", "tick", "stabs", "kick4", "snare8", "riser"}
    zones = [
        (0, dark, True),
        (36, busier, True),
        (66, countdown, False),
        (69, countdown - {"snare8"} | {"snare16", "timp"}, False),
    ]
    out = _piece(LAST_STAND_BARS, D_MINOR, ["Dm", "Bb", "F", "C"],
                 lambda bar: STAND_TUNE_1 if bar < 18 else STAND_TUNE_2,
                 zones, "D2", STAND_STAB)
    for bar in range(39, 66, 4):
        for step, name in zip((12, 13, 14, 15), ("A3", "G3", "E3", "D3")):
            out.append(_ev("tom", midi(name), bar * BAR_STEPS + step, 1, 0.5,
                           1.0, 96))
    return out
