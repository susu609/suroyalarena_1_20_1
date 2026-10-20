"""Note names, chords and scales shared by the scores."""

NOTE_INDEX = {"C": 0, "D": 2, "E": 4, "F": 5, "G": 7, "A": 9, "B": 11}
MAJOR = (0, 2, 4, 5, 7, 9, 11)
MINOR = (0, 2, 3, 5, 7, 8, 10)          # natural minor
HARMONIC_MINOR = (0, 2, 3, 5, 7, 8, 11)


def midi(name):
    """'C#5' / 'Bb3' -> MIDI number."""
    letter, rest = name[0], name[1:]
    shift = 0
    while rest and rest[0] in "#b":
        shift += 1 if rest[0] == "#" else -1
        rest = rest[1:]
    return 12 * (int(rest) + 1) + NOTE_INDEX[letter] + shift


def chord(bass, *tones):
    """A chord as (bass MIDI, [chord tone MIDI...]) from note names."""
    return midi(bass), [midi(t) for t in tones]


def transpose(spec, semitones):
    bass, tones = spec
    return bass + semitones, [t + semitones for t in tones]


def diatonic(m, degrees, root, scale):
    """m moved by `degrees` steps along the scale (root = MIDI pitch class)."""
    pcs = [(root + s) % 12 for s in scale]
    pc = m % 12
    if pc not in pcs:              # a chromatic note: move by semitones
        return m + (-3 if degrees < 0 else 3) * abs(degrees) // 2
    i = pcs.index(pc)
    octave = 0
    j = i + degrees
    while j < 0:
        j += len(pcs)
        octave -= 1
    while j >= len(pcs):
        j -= len(pcs)
        octave += 1
    base = m - ((pc - pcs[0]) % 12)            # the scale root at or below m
    return base + octave * 12 + ((pcs[j] - pcs[0]) % 12)
