#!/usr/bin/env python3
"""
Composes the two later pieces of the battle music (docs/MUSIC.md):

    python3 scripts/music/compose_late.py

  arena_rush.ogg    2:00 -> 3:00 of a match, exactly 60 s
  last_stand.ogg    sudden death, 3:00 -> 5:00, exactly 120 s

Each is played once from the moment its phase begins (the game starts it on
the phase change), so neither loops. The notes are in late_scores.py, the
sounds in synth.py.
"""
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
from late_scores import (LAST_STAND_SECONDS, RUSH_SECONDS, STEP,  # noqa: E402
                         last_stand_events, rush_events)
from synth import Mix, finish  # noqa: E402

OUT = Path("src/main/resources/assets/royalarena/sounds/music")

for name, seconds, events in (
        ("arena_rush", RUSH_SECONDS, rush_events()),
        ("last_stand", LAST_STAND_SECONDS, last_stand_events())):
    mix = Mix(seconds, tail=2.5)
    mix.play(events, STEP)
    finish(mix, OUT / f"{name}.ogg", loop=False)
