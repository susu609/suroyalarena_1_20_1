"""
The accompaniment every score shares: bass lines, harp arpeggios and drum
grooves that change with the bar's place in its phrase, so no two bars in a
row are copies (the fourth bar of four walks into the next chord, the eighth
bar of eight gets a drum fill).

A bar's harmony is a list of chords that split it evenly (one chord, or two
halves). A chord is notes.chord(bass, *tones) with the chord's root first in
`tones`.
"""

BAR_STEPS = 16


def ev(part, note, step, steps, secs, gain, velocity, arg=None):
    return dict(part=part, note=note, step=step, steps=steps, secs=secs,
                gain=gain, velocity=velocity, arg=arg)


def scaled(events, factor):
    """The same events louder or softer (a section's dynamic)."""
    for e in events:
        e["gain"] *= factor
        e["velocity"] = max(1, min(127, int(round(e["velocity"] * factor))))
    return events


def at(chords, step):
    """The chord sounding at a step of the bar."""
    return chords[min(len(chords) - 1, step * len(chords) // BAR_STEPS)]


def fifth_above(bass, root):
    pc = (root + 7) % 12
    m = bass + 1
    while m % 12 != pc:
        m += 1
    return m


def below(m, chord, gap=3):
    """The highest tone of the chord at least `gap` semitones under m."""
    pcs = {t % 12 for t in chord[1]}
    n = m - gap
    while n % 12 not in pcs:
        n -= 1
    return n


# ------------------------------------------------------------------- bass
# (step, which, length): r root, o octave, 5 fifth, a walk into the next bass
BASS = {
    "march": (
        ((0, "r", 2), (4, "r", 2), (6, "o", 2), (8, "r", 2), (12, "r", 2),
         (14, "o", 2)),
        ((0, "r", 3), (3, "r", 1), (4, "5", 2), (6, "o", 2), (8, "r", 2),
         (10, "5", 2), (12, "o", 2), (14, "5", 2)),
        ((0, "r", 4), (6, "r", 2), (8, "5", 4), (12, "o", 2), (14, "r", 2)),
        ((0, "r", 2), (4, "r", 2), (6, "o", 2), (8, "r", 2), (12, "5", 2),
         (14, "a", 2)),
    ),
    "drive": (
        tuple((s, "r" if s % 4 == 0 else "o", 2) for s in range(0, 16, 2)),
        ((0, "r", 2), (2, "r", 2), (4, "o", 2), (6, "r", 2), (8, "5", 2),
         (10, "r", 2), (12, "o", 2), (14, "5", 2)),
        ((0, "r", 3), (3, "r", 1), (4, "r", 2), (6, "o", 2), (8, "r", 3),
         (11, "r", 1), (12, "5", 2), (14, "o", 2)),
        ((0, "r", 2), (2, "o", 2), (4, "r", 2), (6, "o", 2), (8, "5", 2),
         (10, "o", 2), (12, "5", 2), (14, "a", 2)),
    ),
    "long": (((0, "r", 8), (8, "5", 8)),) * 3 + (((0, "r", 8), (8, "r", 6),
                                                   (14, "a", 2)),),
}


def bass_bar(out, base, chords, next_bass, bar, step_s, style="march",
             gain=0.42):
    for step, which, length in BASS[style][bar % 4]:
        bass, tones = at(chords, step)
        if which == "a" and next_bass != bass:
            m = next_bass - 1 if next_bass > bass else next_bass + 1
        elif which in ("5", "a"):
            m = fifth_above(bass, tones[0])
        else:
            m = bass + (12 if which == "o" else 0)
        out.append(ev("bass", m, base + step, length,
                      max(0.5, length * step_s + 0.15), gain,
                      100 if step == 0 else 88))


# -------------------------------------------------------------------- arp
ARP = ((0, 1, 2, 1, 0, 1, 2, 1),   # up and down
       (0, 2, 1, 2, 0, 2, 1, 2),   # broken
       (0, 1, 2, 3, 0, 1, 2, 3),   # up, over the octave
       (3, 2, 1, 0, 2, 1, 0, 1))   # down


def arp_bar(out, base, chords, bar, rate=2, gain=0.16, octave=12,
            sparse=False, secs=0.6):
    pattern = ARP[bar % 4]
    for i in range(BAR_STEPS // rate):
        step = i * rate
        if sparse and step % 4:
            continue
        tones = sorted(at(chords, step)[1])
        if len(tones) == 3:
            tones.append(tones[0] + 12)
        out.append(ev("arp", tones[pattern[i % 8]] + octave, base + step,
                      rate, secs, gain, 70 if step % 4 == 0 else 58))


# ------------------------------------------------------------------ drums
FILL_TOMS = (57, 55, 52, 50)   # A3 G3 E3 D3


def _kick(out, base, step):
    out.append(ev("kick", None, base + step, 1, None, 0.65, 112))


def _snare(out, base, step, gain=0.45, velocity=100, arg=0.8):
    out.append(ev("snare", None, base + step, 1, None, gain, velocity,
                  arg=arg))


def _hat(out, base, step, gain=0.24):
    loud = 1.0 if step % 4 == 0 else 0.55
    out.append(ev("hat", None, base + step, 1, None, gain,
                  72 if loud == 1.0 else 46, arg=loud))


def drums_bar(out, base, bar, kit, fills=True):
    """
    kit: "light" (hats), "half" (kick on 1 and 3, hats), "march" (kick,
    backbeat snare, ghost notes), "full" (march, busier), "drive" (four on
    the floor, backbeat, 16th hats), "dark" (half-time with backbeat).
    The 8th bar of 8 ends in a fill (snare ramp, or toms every other time);
    the 4th bar of 4 in a short pickup.
    """
    groove_b = bar % 2 == 1
    big_fill = fills and bar % 8 == 7 and kit not in ("light", "half")
    small_fill = fills and bar % 4 == 3 and not big_fill and kit not in (
        "light", "half")
    end = 12 if big_fill else 16

    kicks = {"light": (), "half": (0, 8), "march": (0, 8), "dark": (0, 8),
             "full": (0, 6, 8) if groove_b else (0, 8, 11),
             "drive": (0, 4, 8, 12)}[kit]
    for step in kicks:
        if step < end:
            _kick(out, base, step)
    if kit in ("march", "full", "drive", "dark"):
        for step in (4, 12):
            if step < end:
                _snare(out, base, step)
        ghosts = (7, 14) if groove_b else (14,)
        if kit == "full":
            ghosts = (7, 10, 15) if groove_b else (3, 14)
        for step in ghosts:
            if step < end and not (small_fill and step >= 14):
                _snare(out, base, step, 0.22, 54, 0.4)
    hats = range(0, end, 1 if kit in ("drive", "full") else 2)
    for step in hats:
        if kit == "full" and step % 2 and step not in (6, 14):
            continue
        _hat(out, base, step, 0.2 if kit == "drive" else 0.24)

    if small_fill:
        _snare(out, base, 14, 0.3, 70, 0.6)
        _snare(out, base, 15, 0.36, 82, 0.7)
    if big_fill:
        if (bar // 8) % 2 == 0:
            for i, step in enumerate(range(12, 16)):
                _snare(out, base, step, 0.32 + 0.06 * i, 74 + 10 * i, 0.7)
        else:
            _snare(out, base, 12, 0.4, 92)
            for step, m in zip((13, 14, 15), FILL_TOMS[1:]):
                out.append(ev("tom", m, base + step, 1, 0.5, 1.0, 100))
            out.append(ev("tom", FILL_TOMS[0], base + 12, 1, 0.5, 0.8, 90))
