"""
The score of "Arena March" (the first phase's music), shared by the
synthesiser (compose_phase1.py) and the MIDI export (export_midi.py), so
both always play the same notes.

events() lists every note in the order the synthesiser renders them (the
drum noise depends on that order). Each event carries both what the
synthesiser needs (secs, gain, arg) and what MIDI needs (steps, velocity).
"""

BPM = 128
STEP = 60.0 / BPM / 4.0          # one sixteenth
BAR_STEPS = 16
BARS = 40
LOOP_SECONDS = BARS * BAR_STEPS * STEP   # 75.0

NOTE_INDEX = {"C": 0, "D": 2, "E": 4, "F": 5, "G": 7, "A": 9, "B": 11}


def midi(name):
    """'C#5' / 'Bb3' -> MIDI number."""
    letter, rest = name[0], name[1:]
    shift = 0
    while rest and rest[0] in "#b":
        shift += 1 if rest[0] == "#" else -1
        rest = rest[1:]
    return 12 * (int(rest) + 1) + NOTE_INDEX[letter] + shift


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

LEAD_TAIL = 0.25   # seconds a lead note rings past its length


def _ev(part, note, step, steps, secs, gain, velocity, arg=None):
    return dict(part=part, note=note, step=step, steps=steps, secs=secs,
                gain=gain, velocity=velocity, arg=arg)


def events():
    """Every note of the loop, in render order."""
    out = []
    for s, sec in enumerate(SECTIONS):
        for b in range(8):
            base = (s * 8 + b) * BAR_STEPS
            (bass_lo, bass_hi), triad = CHORDS[sec["chords"][b]]

            # Bass: root on the beat, octave on the offbeat 8ths.
            for step, name in ((0, bass_lo), (4, bass_lo), (6, bass_hi),
                               (8, bass_lo), (12, bass_lo), (14, bass_hi)):
                out.append(_ev("bass", midi(name), base + step, 2, 0.5, 0.42,
                               92))

            # Harp arpeggio in 8ths up and down the triad.
            if sec["harp"]:
                for i, k in enumerate((0, 1, 2, 1, 0, 1, 2, 1)):
                    out.append(_ev("arp", midi(triad[k]) + 12, base + i * 2, 2,
                                   0.6, 0.16, 64))

            # Sustained pad under the big sections.
            if sec["pad"] and b % 2 == 0:
                for name in triad:
                    out.append(_ev("pad", midi(name), base, 2 * BAR_STEPS,
                                   2 * BAR_STEPS * STEP, 0.05, 52))

            # The tune; under the horn the bell doubles an octave up.
            for step, name, length in sec["tune"][b]:
                for kind in sec["lead"]:
                    doubled = kind == "bell" and "horn" in sec["lead"]
                    m = midi(name) + (12 if doubled else 0)
                    out.append(_ev("lead_" + kind, m, base + step, length,
                                   length * STEP + LEAD_TAIL,
                                   0.6 if doubled else 1.0,
                                   76 if doubled else 100))

            # Drums.
            d = sec["drums"]
            if d != "light" or b >= 4:
                for step in (0, 8):
                    out.append(_ev("kick", None, base + step, 1, None, 0.65,
                                   112))
            if d in ("march", "full") or (d == "light" and b >= 4):
                for step in (4, 12):
                    out.append(_ev("snare", None, base + step, 1, None, 0.45,
                                   100, arg=0.8))
                out.append(_ev("snare", None, base + 14, 1, None, 0.25, 58,
                               arg=0.4))
            if d in ("light", "march", "full"):
                for step in range(0, 16, 2):
                    loud = 1.0 if step % 4 == 0 else 0.55
                    out.append(_ev("hat", None, base + step, 1, None, 0.24,
                                   72 if loud == 1.0 else 46, arg=loud))
            if d == "full" and b == 7:
                # A snare roll into the bridge.
                for step in (10, 11, 12, 13, 14, 15):
                    out.append(_ev("snare", None, base + step, 1, None, 0.3,
                                   70 + (step - 10) * 8, arg=0.6))
    return out
