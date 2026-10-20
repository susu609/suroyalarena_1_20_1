"""
The score of "Arena March" (the first phase's music), shared by the
synthesiser (compose_phase1.py) and the MIDI export (export_midi.py), so
both always play the same notes.

The tune grows from one two-bar hook (a short pickup, a leap up a fifth to a
long note, then a little turn); the late pieces bring the same hook back in
minor. The A tune runs through a borrowed D7, the B tune through a borrowed
Bb, the fourth section lifts a whole step to D major, and the bridge drops
to a soft piano before the drums roll back into the loop.

events() lists every note in the order the synthesiser renders them (the
drum noise depends on that order). Each event carries both what the
synthesiser needs (secs, gain, arg) and what MIDI needs (steps, velocity).
"""
from arranger import (BAR_STEPS, arp_bar, at, bass_bar, below, drums_bar,
                      ev, scaled)
from notes import chord, midi, transpose

BPM = 128
STEP = 60.0 / BPM / 4.0          # one sixteenth
BARS = 40
LOOP_SECONDS = BARS * BAR_STEPS * STEP   # 75.0

CHORDS = {
    "C": chord("C2", "C4", "E4", "G4"),
    "C/E": chord("E2", "C4", "E4", "G4"),
    "G/B": chord("B1", "G3", "B3", "D4"),
    "G": chord("G2", "G3", "B3", "D4"),
    "Gsus4": chord("G2", "G3", "C4", "D4"),
    "Am": chord("A2", "A3", "C4", "E4"),
    "F": chord("F2", "F3", "A3", "C4"),
    "D7": chord("D2", "D4", "F#4", "A4", "C5"),
    "Bb": chord("Bb1", "Bb3", "D4", "F4"),
    "A": chord("A2", "A3", "C#4", "E4"),
    "Fmaj7": chord("F2", "F3", "A3", "C4", "E4"),
    "Em7": chord("E2", "E3", "G3", "B3", "D4"),
    "Dm7": chord("D2", "D3", "F3", "A3", "C4"),
    "G7sus": chord("G2", "G3", "C4", "D4", "F4"),
}

# One entry per bar; a pair splits the bar in halves.
A_CHORDS = ["C", "G/B", "Am", "F", "C/E", "F", "D7", ("Gsus4", "G")]
B_CHORDS = ["Am", "F", "C", "G", "Am", "F", "Bb", ("G", "A")]
BRIDGE_CHORDS = ["Fmaj7", "Em7", "Dm7", "G7sus", "Fmaj7", "Em7", "Dm7", "G"]

# (step in bar, note, length in steps); gaps are rests.
A_TUNE = [
    [(0, "G4", 3), (3, "C5", 1), (4, "G5", 6), (10, "E5", 2), (12, "F5", 2), (14, "E5", 2)],
    [(0, "D5", 4), (4, "B4", 2), (6, "D5", 2), (8, "G4", 6)],
    [(0, "A4", 3), (3, "C5", 1), (4, "A5", 6), (10, "G5", 2), (12, "E5", 2), (14, "C5", 2)],
    [(0, "D5", 3), (3, "C5", 1), (4, "A4", 4), (8, "C5", 4), (12, "D5", 2), (14, "E5", 2)],
    [(0, "G5", 3), (3, "G5", 1), (4, "C6", 6), (10, "B5", 2), (12, "A5", 2), (14, "G5", 2)],
    [(0, "A5", 4), (4, "F5", 2), (6, "A5", 2), (8, "C6", 4), (12, "A5", 2), (14, "F5", 2)],
    [(0, "F#5", 3), (3, "A5", 1), (4, "D6", 4), (8, "C6", 2), (10, "A5", 2), (12, "F#5", 4)],
    [(0, "G5", 6), (6, "F5", 1), (7, "E5", 1), (8, "D5", 4)],
]
B_TUNE = [
    [(0, "E5", 6), (6, "D5", 2), (8, "C5", 4), (12, "E5", 4)],
    [(0, "F5", 6), (6, "E5", 2), (8, "C5", 6)],
    [(0, "G5", 4), (4, "E5", 2), (6, "G5", 2), (8, "C6", 8)],
    [(0, "B5", 4), (4, "A5", 2), (6, "G5", 2), (8, "D5", 6)],
    [(0, "E5", 6), (6, "D5", 2), (8, "C5", 4), (12, "A5", 4)],
    [(0, "A5", 4), (4, "C6", 4), (8, "F5", 6)],
    [(0, "D6", 4), (4, "Bb5", 2), (6, "F5", 2), (8, "Bb5", 4), (12, "D6", 4)],
    [(0, "D6", 4), (4, "B5", 4), (8, "A5", 4), (12, "C#6", 4)],
]
# The bridge: pieces of the hook on the harp over a soft piano.
BRIDGE_TUNE = [
    [(0, "A4", 3), (3, "C5", 1), (4, "A5", 6)],
    [(4, "G5", 2), (6, "E5", 2), (8, "B4", 6)],
    [(0, "D5", 3), (3, "F5", 1), (4, "C6", 6), (10, "A5", 2), (12, "F5", 4)],
    [(0, "G5", 8), (8, "F5", 4), (12, "D5", 4)],
    [(0, "E5", 3), (3, "F5", 1), (4, "C6", 6), (12, "A5", 4)],
    [(0, "B5", 4), (4, "G5", 4), (8, "E5", 8)],
    [(0, "F5", 3), (3, "A5", 1), (4, "D6", 4), (8, "C6", 4), (12, "A5", 4)],
    [(0, "B5", 4), (4, "D6", 4)],
]

# What plays in each of the five 8-bar sections. key: semitones up.
SECTIONS = [
    dict(chords=A_CHORDS, tune=A_TUNE, lead=("bell",), drums="light",
         arp=True, pad=False, key=0, dyn=0.85),
    dict(chords=A_CHORDS, tune=A_TUNE, lead=("bell", "pling_under"),
         drums="march", arp=True, pad=False, key=0, dyn=0.95),
    dict(chords=B_CHORDS, tune=B_TUNE, lead=("horn",), drums="march",
         arp=True, pad=True, key=0, dyn=1.0),
    dict(chords=A_CHORDS, tune=A_TUNE, lead=("horn", "bell"), drums="full",
         arp=True, pad=True, key=2, dyn=1.1),
    dict(chords=BRIDGE_CHORDS, tune=BRIDGE_TUNE, lead=("harp",),
         drums="half", arp=False, pad=False, key=0, dyn=0.7, piano=True),
]

LEAD_TAIL = 0.25   # seconds a lead note rings past its length


def _bar_chords(sec, b):
    names = sec["chords"][b]
    names = names if isinstance(names, tuple) else (names,)
    return [transpose(CHORDS[n], sec["key"]) for n in names]


def _section(s, sec, next_bass_of):
    out = []
    for b in range(8):
        bar = s * 8 + b
        base = bar * BAR_STEPS
        chords = _bar_chords(sec, b)

        bass_bar(out, base, chords, next_bass_of(bar + 1), bar, STEP,
                 "long" if sec.get("piano") else "march")
        if sec["arp"]:
            arp_bar(out, base, chords, bar, sparse=b == 7)
        if sec["pad"] and b % 2 == 0:
            for m in chords[0][1][:3]:
                out.append(ev("pad", m, base, 2 * BAR_STEPS,
                              2 * BAR_STEPS * STEP, 0.05, 52))
        if sec.get("piano"):
            # Chord on the one, a lighter echo of the top on the "and" of 3.
            for i, m in enumerate(chords[0][1]):
                out.append(ev("piano", m, base + i, BAR_STEPS - i,
                              BAR_STEPS * STEP, 0.9, 72))
            for m in sorted(chords[-1][1])[-2:]:
                out.append(ev("piano", m + 12, base + 10, 6, 6 * STEP + 0.4,
                              0.5, 56))

        # The tune; under the horn the bell doubles an octave up, and in
        # section 2 a soft pling sings the chord tone under the bell.
        for step, name, length in sec["tune"][b]:
            m = midi(name) + sec["key"]
            secs = length * STEP + LEAD_TAIL
            for kind in sec["lead"]:
                if kind == "pling_under":
                    out.append(ev("lead_pling", below(m, at(chords, step)),
                                  base + step, length, secs, 0.7, 74))
                    continue
                doubled = kind == "bell" and "horn" in sec["lead"]
                out.append(ev("lead_" + kind, m + (12 if doubled else 0),
                              base + step, length, secs,
                              0.6 if doubled else 1.0, 76 if doubled else 100))

        if sec["drums"] == "light":
            drums_bar(out, base, bar, "march" if b >= 4 else "light",
                      fills=b >= 4)
        else:
            drums_bar(out, base, bar, sec["drums"], fills=not sec.get("piano"))

    first = s * 8 * BAR_STEPS
    if s in (0, 3):
        out.append(ev("crash", None, first, 1, None, 0.45 if s == 0 else 0.55,
                      110))
    if s == 3:
        out.append(ev("timp", midi("D3"), first, 2, None, 0.5, 108))
    if sec.get("piano"):
        # A snare roll and a riser in the last bar, back into the loop.
        last = first + 7 * BAR_STEPS
        for step in range(8, 16):
            out.append(ev("snare", None, last + step, 1, None,
                          0.18 + 0.03 * (step - 8), 60 + 6 * (step - 8),
                          arg=0.6))
        out.append(ev("riser", None, last, 1, BAR_STEPS * STEP, 0.55, 90))
    return scaled(out, sec["dyn"])


def events():
    """Every note of the loop, in render order."""
    def next_bass_of(bar):
        bar %= BARS
        sec = SECTIONS[bar // 8]
        return _bar_chords(sec, bar % 8)[0][0]

    out = []
    for s, sec in enumerate(SECTIONS):
        out.extend(_section(s, sec, next_bass_of))
    return out
