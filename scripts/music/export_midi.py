#!/usr/bin/env python3
"""
Writes "Arena March" as a standard MIDI file (type 1, one track per part),
for orchestrating it with real instruments in a DAW (BandLab, LMMS,
FL Studio...) or feeding it to an AI music app as a melody reference.

    python3 scripts/music/export_midi.py [march|rush|last_stand] [out.mid]

The notes come from arena_march.py, the same score the in-game Ogg is
synthesised from. General MIDI programs are a starting suggestion for an
orchestral version; swap them freely in the DAW.
"""
import struct
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
import arena_march  # noqa: E402
import late_scores  # noqa: E402

PPQ = 480
TICKS_PER_STEP = PPQ // 4

# piece -> (title, bpm, bars, events, section names)
PIECES = {
    "march": ("Arena March (Su Royal Arena, phase 1)", arena_march.BPM,
              arena_march.BARS, arena_march.events,
              ("A - bells", "A - fuller", "B - horn tune",
               "A - horn and bells", "Bridge")),
    "rush": ("Arena Rush (Su Royal Arena, 2:00-3:00)", late_scores.BPM,
             late_scores.RUSH_BARS, late_scores.rush_events,
             ("60 s left", "30 s left (bar 19)", "10 s left (bar 31)")),
    "last_stand": ("Last Stand (Su Royal Arena, sudden death)",
                   late_scores.BPM, late_scores.LAST_STAND_BARS,
                   late_scores.last_stand_events,
                   ("sudden death", "60 s left (bar 37)",
                    "10 s left (bar 67)")),
}
# Where each section starts, in bars.
SECTION_BARS = {"march": (0, 8, 16, 24, 32), "rush": (0, 18, 30),
                "last_stand": (0, 36, 66)}
BAR_STEPS = 16

# part -> (track name, channel, GM program (0-based), orchestral idea)
TRACKS = {
    "lead_horn": ("Lead - Horn (big tune)", 0, 60, "French horns / trumpets"),
    "lead_bell": ("Lead - Bells", 1, 9, "glockenspiel"),
    "lead_pling": ("Lead - Pling", 2, 80, "piccolo / flute, or a square lead"),
    "lead_brass": ("Lead - Brass (tense)", 7, 56, "trumpets and trombones"),
    "strings": ("Strings", 8, 48, "string section, sustained"),
    "lead_harp": ("Lead - Harp (bridge)", 3, 46, "harp"),
    "arp": ("Harp arpeggio", 4, 46, "harp, or pizzicato strings"),
    "pad": ("Pad", 5, 48, "string section, sustained"),
    "bass": ("Bass", 6, 32, "low brass / cellos + double bass"),
    "tom": ("Toms", 10, 117, "melodic toms / taiko"),
    "timp": ("Timpani", 11, 47, "timpani"),
    "drums": ("Drums", 9, 0, "march snare, bass drum, hi-hat / cymbals"),
}
DRUM_NOTE = {"kick": 36, "snare": 38, "hat": 42, "crash": 49, "tick": 76}
# Parts that are not notes in the MIDI (noise sweeps): left out.
SKIP = {"riser"}


def vlq(n):
    out = [n & 0x7F]
    n >>= 7
    while n:
        out.append((n & 0x7F) | 0x80)
        n >>= 7
    return bytes(reversed(out))


def track(chunks):
    """A track chunk from (tick, order, bytes) events, sorted."""
    data = b""
    now = 0
    for tick, _, msg in sorted(chunks, key=lambda c: (c[0], c[1])):
        data += vlq(tick - now) + msg
        now = tick
    data += b"\x00\xff\x2f\x00"
    return b"MTrk" + struct.pack(">I", len(data)) + data


def meta_name(name):
    raw = name.encode("utf-8")
    return b"\xff\x03" + vlq(len(raw)) + raw


def main():
    args = sys.argv[1:]
    piece = args.pop(0) if args and args[0] in PIECES else "march"
    title, bpm, bars, events, sections = PIECES[piece]
    out = Path(args[0] if args else f"build/music/{piece}.mid")
    out.parent.mkdir(parents=True, exist_ok=True)
    per_part = {key: [] for key in TRACKS}
    for e in events():
        if e["part"] in SKIP:
            continue
        drum = e["part"] in DRUM_NOTE
        part = "drums" if drum else e["part"]
        per_part[part].append(e)

    chunks = []
    # Conductor track: name, tempo, 4/4, loop markers.
    tempo = 60_000_000 // bpm
    end = bars * BAR_STEPS * TICKS_PER_STEP
    conductor = [
        (0, 0, meta_name(title)),
        (0, 1, b"\xff\x51\x03" + tempo.to_bytes(3, "big")),
        (0, 2, b"\xff\x58\x04\x04\x02\x18\x08"),
        (0, 3, b"\xff\x06" + vlq(9) + b"loopStart"),
        (end, 4, b"\xff\x06" + vlq(7) + b"loopEnd"),
    ]
    for first_bar, name in zip(SECTION_BARS[piece], sections):
        label = name.encode()
        conductor.append((first_bar * BAR_STEPS * TICKS_PER_STEP, 5,
                          b"\xff\x06" + vlq(len(label)) + label))
    chunks.append(track(conductor))

    for part, (name, ch, program, _idea) in TRACKS.items():
        evs = per_part[part]
        if not evs:
            continue
        msgs = [(0, 0, meta_name(name))]
        if ch != 9:
            msgs.append((0, 1, bytes([0xC0 | ch, program])))
        for e in evs:
            note = DRUM_NOTE[e["part"]] if part == "drums" else e["note"]
            if part == "tick":
                note = 76
            on = e["step"] * TICKS_PER_STEP
            length = max(1, e["steps"] * TICKS_PER_STEP - 10)
            vel = max(1, min(127, int(e["velocity"])))
            # Note-offs sort before note-ons at the same tick.
            msgs.append((on, 3, bytes([0x90 | ch, note, vel])))
            msgs.append((on + length, 2, bytes([0x80 | ch, note, 0])))
        chunks.append(track(msgs))

    header = b"MThd" + struct.pack(">IHHH", 6, 1, len(chunks), PPQ)
    out.write_bytes(header + b"".join(chunks))
    print(f"{out}: {len(chunks)} tracks, {bars} bars at {bpm} bpm")


if __name__ == "__main__":
    main()
