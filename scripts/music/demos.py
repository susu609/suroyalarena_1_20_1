#!/usr/bin/env python3
"""
Five 30-second demos of battle music in the manner of a mobile strategy
game (brass fanfares, pizzicato, march snares), for choosing a direction
before a full piece is written. Original tunes, no game's audio or melody.

    python3 scripts/music/demos.py [out_dir]

  1 Bridge Charge   heroic march          120 bpm  Bb major  15 bars
  2 Goblin Gold     playful pizzicato      96 bpm  F major   12 bars
  3 Tower Siege     tense battle          144 bpm  D minor   18 bars
  4 Tavern Jig      swung tavern dance    128 bpm  G major   16 bars
  5 Crown Fanfare   victory fanfare       160 bpm  C major   20 bars
  6-8 one theme in three phases (normal, double elixir, sudden death):
                    128 bpm  A minor  16 bars each

Every demo is exactly 30 s. Same instruments as the in-game music
(synth.py) and the same accompaniment patterns (arranger.py).
"""
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
from arranger import (BAR_STEPS, arp_bar, at, bass_bar, drums_bar, ev,  # noqa: E402
                      scaled)
from notes import chord, midi, transpose  # noqa: E402
from synth import Mix, finish  # noqa: E402

SECONDS = 30.0


# --------------------------------------------------------------- building
def chords_of(table, names, key):
    """Bar chord lists (a pair splits the bar) from chord names."""
    bars = []
    for name in names:
        parts = name if isinstance(name, tuple) else (name,)
        bars.append([transpose(table[p], key) for p in parts])
    return bars


def section(out, first_bar, bars, step_s, tune=None, leads=(), key=0,
            bass="march", arp=None, arp_secs=0.6, pad=False, strings=False,
            drums=None, crash=False, timp=False, dyn=1.0):
    """
    One stretch of bars: bass, optional arp (2 = 8ths, 1 = 16ths), pad or
    strings, a drum kit, and the tune on each lead (part, octave, gain).
    """
    part = []
    for i, chords in enumerate(bars):
        bar = first_bar + i
        base = bar * BAR_STEPS
        nxt = bars[i + 1][0][0] if i + 1 < len(bars) else chords[0][0]
        if bass:
            bass_bar(part, base, chords, nxt, bar, step_s, bass)
        if arp:
            arp_bar(part, base, chords, bar, rate=arp, secs=arp_secs,
                    sparse=i % 8 == 7)
        if pad and i % 2 == 0:
            for m in chords[0][1][:3]:
                part.append(ev("pad", m, base, 2 * BAR_STEPS,
                               2 * BAR_STEPS * step_s, 0.05, 52))
        if strings:
            for c, ch in enumerate(chords):
                half = BAR_STEPS // len(chords)
                for m in ch[1][:3]:
                    part.append(ev("strings", m, base + c * half, half,
                                   half * step_s, 1.0, 54))
        if drums:
            drums_bar(part, base, bar, drums)
        if timp:
            lo = chords[0][0]
            part.append(ev("timp", lo + (12 if lo < 40 else 0), base, 2, None,
                           0.45, 104))
        if tune:
            for step, name, length in tune[i % len(tune)]:
                m = midi(name) + key
                for lead, octave, gain in leads:
                    part.append(ev(lead, m + 12 * octave, base + step, length,
                                   length * step_s + 0.25, gain,
                                   100 if gain >= 1.0 else 76))
    if crash:
        part.append(ev("crash", None, first_bar * BAR_STEPS, 1, None, 0.5, 112))
    out.extend(scaled(part, dyn))


def ending(out, bar, spec, step_s, hits=(0, 4, 8)):
    """Stabs on the chord, a timpani and cymbal on the last, a held bass."""
    base = bar * BAR_STEPS
    lo, tones = spec
    for step in hits:
        last = step == hits[-1]
        length = BAR_STEPS - step if last else 2
        for m in tones[:3]:
            out.append(ev("lead_brass", m + 12, base + step, length,
                          length * step_s + (0.8 if last else 0.15), 1.0, 110))
        out.append(ev("timp", lo + (12 if lo < 40 else 0), base + step, 2,
                      None, 0.6, 118))
        out.append(ev("kick", None, base + step, 1, None, 0.7, 118))
    out.append(ev("crash", None, base + hits[-1], 1, None, 0.6, 118))
    out.append(ev("bass", lo, base + hits[-1], BAR_STEPS - hits[-1],
                  (BAR_STEPS - hits[-1]) * step_s, 0.5, 100))


def roll(out, bar, step_s, start=8):
    """A snare roll up the second half of a bar, into what comes next."""
    base = bar * BAR_STEPS
    for i, step in enumerate(range(start, BAR_STEPS)):
        out.append(ev("snare", None, base + step, 1, None, 0.2 + 0.04 * i,
                      60 + 6 * i, arg=0.6))


def swing(events, amount=0.6):
    """Off-beat 8ths late (a shuffle): step 2 of every beat moves on."""
    for e in events:
        if e["part"] not in ("pad", "strings", "crash") and \
                float(e["step"]) % 4 == 2:
            e["step"] = e["step"] + amount
    return events


# ---------------------------------------------------------------- chords
MAJOR_C = {
    "C": chord("C3", "C4", "E4", "G4"),
    "C/E": chord("E2", "C4", "E4", "G4"),
    "Dm": chord("D3", "D4", "F4", "A4"),
    "Dm7": chord("D3", "D4", "F4", "A4", "C5"),
    "Em": chord("E2", "E4", "G4", "B4"),
    "F": chord("F2", "F3", "A3", "C4"),
    "G": chord("G2", "G3", "B3", "D4"),
    "G7": chord("G2", "G3", "B3", "D4", "F4"),
    "Am": chord("A2", "A3", "C4", "E4"),
    "D": chord("D3", "D4", "F#4", "A4"),
}
MINOR_D = {
    "Dm": chord("D2", "D3", "F3", "A3"),
    "Bb": chord("Bb1", "Bb3", "D4", "F4"),
    "C": chord("C2", "C4", "E4", "G4"),
    "A": chord("A1", "A3", "C#4", "E4"),
    "Gm": chord("G2", "G3", "Bb3", "D4"),
    "F": chord("F2", "F3", "A3", "C4"),
}


# ----------------------------------------------------------------- demos
def bridge_charge():
    """Heroic march in Bb: a fanfare bar, the horn tune, a brass B, a cadence."""
    bpm, key = 120, -2
    step = 60.0 / bpm / 4.0
    tune_a = [
        [(0, "G4", 2), (2, "C5", 2), (4, "E5", 4), (8, "D5", 2), (10, "C5", 2), (12, "G4", 4)],
        [(0, "A4", 2), (2, "C5", 2), (4, "F5", 6), (10, "E5", 2), (12, "D5", 4)],
        [(0, "B4", 2), (2, "D5", 2), (4, "G5", 4), (8, "F5", 2), (10, "E5", 2), (12, "D5", 4)],
        [(0, "E5", 6), (6, "D5", 2), (8, "C5", 8)],
        [(0, "F5", 2), (2, "F5", 2), (4, "A5", 4), (8, "G5", 2), (10, "F5", 2), (12, "E5", 4)],
        [(0, "D5", 2), (2, "E5", 2), (4, "F5", 4), (8, "A5", 4), (12, "F5", 4)],
        [(0, "G5", 4), (4, "F5", 2), (6, "E5", 2), (8, "D5", 4), (12, "B4", 4)],
        [(0, "C5", 4), (4, "G4", 2), (6, "C5", 2), (8, "E5", 8)],
    ]
    tune_b = [
        [(0, "E5", 6), (6, "A5", 2), (8, "G5", 4), (12, "E5", 4)],
        [(0, "F5", 6), (6, "A5", 2), (8, "C6", 8)],
        [(0, "G5", 4), (4, "E5", 4), (8, "C5", 4), (12, "E5", 4)],
        [(0, "D5", 4), (4, "G5", 4), (8, "B5", 8)],
    ]
    out = []
    # Bar 0: the fanfare, brass on the tonic, snare roll in.
    ending(out, 0, transpose(MAJOR_C["C"], key), step, hits=(0, 3, 6))
    roll(out, 0, step, start=10)
    section(out, 1, chords_of(MAJOR_C, ["C", "F", "G", "C/E", "F", "Dm7",
                                        "G7", "C"], key), step,
            tune_a, (("lead_horn", 0, 1.0), ("lead_bell", 1, 0.55)), key,
            arp=2, drums="march", crash=True, dyn=0.95)
    section(out, 9, chords_of(MAJOR_C, ["Am", "F", "C", "G"], key), step,
            tune_b, (("lead_brass", 0, 1.0), ("lead_bell", 1, 0.5)), key,
            arp=1, arp_secs=0.4, strings=True, drums="full", crash=True,
            timp=True, dyn=1.08)
    roll(out, 12, step)
    section(out, 13, chords_of(MAJOR_C, [("F", "G")], key), step,
            [[(0, "A5", 8), (8, "B5", 8)]], (("lead_brass", 0, 1.0),), key,
            strings=True, drums="full", dyn=1.1)
    ending(out, 14, transpose(MAJOR_C["C"], key), step)
    return bpm, out


def goblin_gold():
    """Playful pizzicato in F: plucked bass and arp, staccato bells, woodblock."""
    bpm, key = 96, 5
    step = 60.0 / bpm / 4.0
    tune = [
        [(0, "E5", 1), (2, "G5", 1), (4, "C6", 2), (8, "B5", 1), (10, "G5", 1), (12, "E5", 2)],
        [(0, "A5", 1), (2, "E5", 1), (4, "C5", 2), (6, "E5", 1), (8, "A5", 2), (12, "G5", 2)],
        [(0, "F5", 1), (2, "A5", 1), (4, "C6", 2), (8, "A5", 1), (10, "F5", 1), (12, "C5", 2)],
        [(0, "D5", 1), (2, "G5", 1), (4, "B5", 2), (8, "D6", 2), (12, "B5", 1), (14, "G5", 1)],
        [(0, "E5", 1), (1, "F5", 1), (2, "G5", 1), (4, "C6", 2), (8, "E6", 1), (10, "C6", 1), (12, "G5", 2)],
        [(0, "A5", 1), (2, "C6", 1), (4, "E6", 2), (8, "C6", 1), (10, "A5", 1), (12, "E5", 2)],
        [(0, "F5", 1), (2, "D5", 1), (4, "F5", 1), (6, "A5", 1), (8, "G5", 1), (10, "B5", 1), (12, "D6", 2)],
        [(0, "C6", 2), (4, "G5", 2), (8, "C5", 4)],
    ]
    tune_b = [
        [(0, "A5", 1), (2, "F5", 1), (4, "C5", 1), (6, "F5", 1), (8, "A5", 2), (12, "C6", 2)],
        [(0, "B5", 1), (2, "G5", 1), (4, "D5", 1), (6, "G5", 1), (8, "B5", 2), (12, "D6", 2)],
        [(0, "C6", 1), (2, "B5", 1), (4, "A5", 1), (6, "G5", 1), (8, "F5", 1), (10, "E5", 1), (12, "D5", 2)],
    ]
    out = []
    pizz = dict(bass="long", arp=2, arp_secs=0.22)
    section(out, 0, chords_of(MAJOR_C, ["C", "Am", "F", "G", "C", "Am",
                                        ("Dm", "G"), "C"], key), step,
            tune, (("lead_bell", 0, 1.0),), key, drums="half", dyn=0.9,
            **pizz)
    section(out, 8, chords_of(MAJOR_C, ["F", "G", ("Em", "Am")], key), step,
            tune_b, (("lead_bell", 0, 1.0), ("lead_pling", -1, 0.6)), key,
            drums="march", dyn=1.0, **pizz)
    # A woodblock on every off-beat 8th, all the way.
    for bar in range(11):
        for s in range(2, 16, 4):
            out.append(ev("tick", 84, bar * BAR_STEPS + s, 1, None, 0.45, 70))
    ending(out, 11, transpose(MAJOR_C["C"], key), step, hits=(0, 2, 6))
    return bpm, out


def tower_siege():
    """Tense battle in D minor: 16th strings, timpani, brass tune, a riser."""
    bpm = 144
    step = 60.0 / bpm / 4.0
    tune = [
        [(0, "D5", 4), (4, "A4", 2), (6, "D5", 2), (8, "F5", 4), (12, "E5", 2), (14, "D5", 2)],
        [(0, "Bb4", 6), (6, "D5", 2), (8, "F5", 8)],
        [(0, "C5", 4), (4, "E5", 2), (6, "G5", 2), (8, "F5", 2), (10, "E5", 2), (12, "C5", 4)],
        [(0, "C#5", 4), (4, "E5", 4), (8, "A5", 8)],
    ]
    high = [[(s, n[:-1] + str(int(n[-1]) + 1), l) for s, n, l in bar]
            for bar in tune]
    prog = ["Dm", "Bb", "C", "A"]
    out = []
    section(out, 0, chords_of(MINOR_D, prog, 0), step, bass="drive", arp=1,
            arp_secs=0.3, drums="dark", timp=True, crash=True, dyn=0.9)
    section(out, 4, chords_of(MINOR_D, prog * 2, 0), step, tune,
            (("lead_brass", 0, 1.0), ("lead_horn", 0, 0.7)), 0, bass="drive",
            arp=1, arp_secs=0.3, strings=True, drums="drive", timp=True,
            crash=True, dyn=1.0)
    section(out, 12, chords_of(MINOR_D, prog, 0), step, high,
            (("lead_brass", 0, 1.0), ("lead_bell", 0, 0.5)), 0, bass="drive",
            arp=1, arp_secs=0.3, strings=True, drums="drive", timp=True,
            crash=True, dyn=1.1)
    out.append(ev("riser", None, 14 * BAR_STEPS, 1, 2 * BAR_STEPS * step, 0.7,
                  100))
    section(out, 16, chords_of(MINOR_D, ["Gm"], 0), step, bass="drive",
            strings=True, timp=True, dyn=1.1)
    roll(out, 16, step, start=4)
    ending(out, 17, MINOR_D["Dm"], step, hits=(0, 3, 6))
    return bpm, out


def tavern_jig():
    """Swung tavern dance in G: plucky lead, harp, bells join the repeat."""
    bpm = 128
    step = 60.0 / bpm / 4.0
    tune = [
        [(0, "D5", 2), (2, "G5", 2), (4, "B5", 2), (6, "A5", 2), (8, "G5", 2), (10, "D5", 2), (12, "B4", 4)],
        [(0, "C5", 2), (2, "E5", 2), (4, "G5", 2), (6, "E5", 2), (8, "C5", 4), (12, "E5", 4)],
        [(0, "D5", 2), (2, "F#5", 2), (4, "A5", 2), (6, "F#5", 2), (8, "D5", 2), (10, "E5", 2), (12, "F#5", 4)],
        [(0, "G5", 6), (6, "F#5", 2), (8, "G5", 8)],
        [(0, "E5", 2), (2, "G5", 2), (4, "B5", 4), (8, "A5", 2), (10, "G5", 2), (12, "E5", 4)],
        [(0, "C5", 2), (2, "E5", 2), (4, "A5", 4), (8, "G5", 2), (10, "E5", 2), (12, "C5", 4)],
        [(0, "D5", 2), (2, "A5", 2), (4, "F#5", 2), (6, "D5", 2), (8, "E5", 2), (10, "F#5", 2), (12, "A5", 4)],
        [(0, "G5", 4), (4, "D5", 4), (8, "G4", 8)],
    ]
    # The tune is written in G already (key 0 over chords built in C + 7).
    prog = ["C", "F", "G", "C", "Am", "F", "G", "C"]
    out = []
    section(out, 0, chords_of(MAJOR_C, prog, 7), step, tune,
            (("lead_pling", 0, 1.0),), 0, arp=2, arp_secs=0.4,
            drums="march", dyn=0.95)
    section(out, 8, chords_of(MAJOR_C, prog[:7], 7), step, tune,
            (("lead_pling", 0, 1.0), ("lead_bell", 0, 0.6),
             ("lead_horn", -1, 0.45)), 0, arp=2, arp_secs=0.4, pad=True,
            drums="full", crash=True, dyn=1.05)
    # Hand claps (ticks) on 2 and 4.
    for bar in range(15):
        for s in (4, 12):
            out.append(ev("tick", 76, bar * BAR_STEPS + s, 1, None, 0.5, 80))
    swing(out)
    ending(out, 15, transpose(MAJOR_C["C"], 7), step, hits=(0, 3, 6))
    return bpm, out


def crown_fanfare():
    """Victory fanfare in C: a call, the tune twice (the second fuller)."""
    bpm = 160
    step = 60.0 / bpm / 4.0
    call = [[(0, "C5", 2), (2, "C5", 1), (3, "C5", 1), (4, "G5", 4), (8, "E5", 2), (10, "G5", 2), (12, "C6", 4)],
            [(0, "C6", 12)]]
    tune = [
        [(0, "E5", 3), (3, "F5", 1), (4, "G5", 4), (8, "C6", 4), (12, "G5", 4)],
        [(0, "A5", 3), (3, "G5", 1), (4, "F5", 4), (8, "A5", 4), (12, "C6", 4)],
        [(0, "B5", 3), (3, "A5", 1), (4, "G5", 4), (8, "D6", 4), (12, "B5", 4)],
        [(0, "C6", 8), (8, "G5", 4), (12, "E5", 4)],
        [(0, "F5", 3), (3, "G5", 1), (4, "A5", 4), (8, "F5", 4), (12, "A5", 4)],
        [(0, "G5", 3), (3, "A5", 1), (4, "B5", 4), (8, "G5", 4), (12, "D5", 4)],
        [(0, "E5", 2), (2, "F5", 2), (4, "G5", 2), (6, "A5", 2), (8, "B5", 4), (12, "D6", 4)],
        [(0, "C6", 12)],
    ]
    prog = ["C", "F", "G", "C", "F", "G", ("Dm", "G"), "C"]
    out = []
    section(out, 0, chords_of(MAJOR_C, ["C", "C"], 0), step, call,
            (("lead_brass", 0, 1.0), ("lead_horn", -1, 0.7)), 0, bass="long",
            timp=True, crash=True, dyn=1.0)
    roll(out, 1, step)
    section(out, 2, chords_of(MAJOR_C, prog, 0), step, tune,
            (("lead_brass", 0, 1.0), ("lead_bell", 1, 0.5)), 0, arp=2,
            drums="march", crash=True, dyn=0.95)
    section(out, 10, chords_of(MAJOR_C, prog, 0), step, tune,
            (("lead_brass", 0, 1.0), ("lead_horn", -1, 0.7),
             ("lead_bell", 1, 0.55)), 0, arp=1, arp_secs=0.35, strings=True,
            drums="full", crash=True, timp=True, dyn=1.1)
    roll(out, 17, step)
    ending(out, 18, MAJOR_C["C"], step, hits=(0, 2, 4, 8))
    out.append(ev("crash", None, 19 * BAR_STEPS, 1, None, 0.4, 100))
    return bpm, out


# ----------------------------------------------- one theme in three phases
# "Same mould, new melody": one 128 bpm A-minor march scored three ways, the
# way a battle theme is staged (normal, double elixir, sudden death). The
# tempo, key and instruments are my choices, not measured from any game.
MINOR_A = {
    "Am": chord("A2", "A3", "C4", "E4"),
    "F": chord("F2", "F3", "A3", "C4"),
    "G": chord("G2", "G3", "B3", "D4"),
    "Dm": chord("D2", "D3", "F3", "A3"),
    "E7": chord("E2", "E3", "G#3", "B3", "D4"),
    "C": chord("C3", "C4", "E4", "G4"),
    "Em": chord("E2", "E3", "G3", "B3"),
}
PHASE_BPM = 128
PHASE_PROG = ["Am", "Am", "F", "G", "Am", "Am", "Dm", "E7",
              "C", "G", "Am", "F", "Dm", "E7", "Am", "Am"]
# Eight bars of tune (played twice in 16 bars): a pickup and a leap to a
# long note, a falling answer, a rising sequence, a turn into the cadence.
PHASE_TUNE = [
    [(0, "A4", 2), (2, "A4", 2), (4, "E5", 6), (10, "D5", 2), (12, "C5", 4)],
    [(0, "A4", 2), (2, "C5", 2), (4, "F5", 6), (10, "E5", 2), (12, "D5", 4)],
    [(0, "G4", 2), (2, "B4", 2), (4, "D5", 4), (8, "G5", 4), (12, "F5", 2), (14, "D5", 2)],
    [(0, "E5", 8), (8, "B4", 4), (12, "G#4", 4)],
    [(0, "A4", 2), (2, "A4", 2), (4, "E5", 6), (10, "G5", 2), (12, "E5", 4)],
    [(0, "F5", 2), (2, "E5", 2), (4, "D5", 4), (8, "A5", 4), (12, "F5", 4)],
    [(0, "E5", 4), (4, "C5", 2), (6, "E5", 2), (8, "G#5", 4), (12, "B5", 4)],
    [(0, "A5", 8), (8, "E5", 8)],
]


def _phase_chords(prog):
    return chords_of(MINOR_A, prog, 0)


def phase_normal():
    """Phase 1, x1 elixir: light march, horn and bells, arp, no strings."""
    step = 60.0 / PHASE_BPM / 4.0
    out = []
    section(out, 0, _phase_chords(PHASE_PROG[:15]), step, PHASE_TUNE,
            (("lead_horn", 0, 1.0), ("lead_bell", 1, 0.45)), 0, arp=2,
            drums="march", crash=True, dyn=0.9)
    ending(out, 15, MINOR_A["Am"], step, hits=(0, 4, 8))
    return PHASE_BPM, out


def phase_double():
    """Phase 2, x2 elixir: the same tune a step fuller and driving."""
    step = 60.0 / PHASE_BPM / 4.0
    out = []
    section(out, 0, _phase_chords(PHASE_PROG[:15]), step, PHASE_TUNE,
            (("lead_brass", 0, 1.0), ("lead_horn", 0, 0.6),
             ("lead_bell", 1, 0.5)), 0, bass="drive", arp=1, arp_secs=0.35,
            strings=True, drums="drive", crash=True, timp=True, dyn=1.05)
    ending(out, 15, MINOR_A["Am"], step, hits=(0, 4, 8))
    return PHASE_BPM, out


def phase_sudden_death():
    """Phase 3, sudden death: darker, sparse tune low, relentless 16ths."""
    step = 60.0 / PHASE_BPM / 4.0
    prog = ["Am", "Am", "F", "E7", "Am", "Am", "Dm", "E7",
            "F", "G", "Am", "E7", "Dm", "F", "E7", "E7"]
    low = [[(st, n[:-1] + str(int(n[-1]) - 1), ln) for st, n, ln in bar]
           for bar in PHASE_TUNE]
    out = []
    section(out, 0, _phase_chords(prog[:14]), step, low,
            (("lead_brass", 0, 1.0),), 0, bass="drive", arp=1, arp_secs=0.3,
            strings=True, drums="dark", timp=True, crash=True, dyn=1.0)
    # The last two bars: the countdown feel (ticks, riser, snare roll).
    section(out, 14, _phase_chords(prog[14:]), step, None, (), 0,
            bass="drive", strings=True, timp=True, dyn=1.1)
    for bar in range(14, 16):
        for st in (0, 4, 8, 12):
            out.append(ev("tick", 88 if st else 95, bar * BAR_STEPS + st, 1,
                          None, 0.7, 90))
    out.append(ev("riser", None, 14 * BAR_STEPS, 1, 2 * BAR_STEPS * step, 0.7,
                  100))
    roll(out, 15, step, start=4)
    out.append(ev("crash", None, 15 * BAR_STEPS + 15, 1, None, 0.5, 110))
    return PHASE_BPM, out


DEMOS = [
    ("demo1_bridge_charge", bridge_charge),
    ("demo2_goblin_gold", goblin_gold),
    ("demo3_tower_siege", tower_siege),
    ("demo4_tavern_jig", tavern_jig),
    ("demo5_crown_fanfare", crown_fanfare),
    ("demo6_phase1_normal", phase_normal),
    ("demo7_phase2_double_elixir", phase_double),
    ("demo8_phase3_sudden_death", phase_sudden_death),
]


def main():
    target = Path(sys.argv[1] if len(sys.argv) > 1 else "build/music/demos")
    target.mkdir(parents=True, exist_ok=True)
    for name, make in DEMOS:
        bpm, events = make()
        step = 60.0 / bpm / 4.0
        mix = Mix(SECONDS, tail=2.0)
        mix.play(events, step)
        # Demos for listening, not game files: a little more headroom.
        # The three phases of one theme get louder in order, like the game.
        level = {"demo6_phase1_normal": -21.0,
                 "demo7_phase2_double_elixir": -18.5,
                 "demo8_phase3_sudden_death": -17.0}.get(name, -19.0)
        finish(mix, target / f"{name}.ogg", loop=False, rms_db=level,
               peak_db=-1.0)


if __name__ == "__main__":
    main()
