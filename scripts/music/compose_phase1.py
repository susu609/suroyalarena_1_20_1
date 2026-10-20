#!/usr/bin/env python3
"""
Composes "Arena March", the music of a match's first phase (x1 elixir), and
writes it as a seamless 75 s loop.

    python3 scripts/music/compose_phase1.py [out.ogg]

Original tune and original synthesis (numpy only; ffmpeg encodes the Ogg):
a light heroic march at 128 bpm in C major, played on instruments that
imitate Minecraft's note blocks (bass, harp, bell, pling, snare, hat, kick)
plus a soft synthesised horn for the big tune. 40 bars of 4/4 = 75 s.
The notes are in arena_march.py; the sounds in synth.py.
"""
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
from arena_march import LOOP_SECONDS, STEP, events  # noqa: E402
from synth import Mix, finish  # noqa: E402

mix = Mix(LOOP_SECONDS, tail=3.0)
mix.play(events(), STEP)
finish(mix, sys.argv[1] if len(sys.argv) > 1 else
       "src/main/resources/assets/royalarena/sounds/music/arena_phase1.ogg",
       loop=True, rms_db=-20.5)
