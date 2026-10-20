# Music

The mod's own music, its score, and how to turn it into a fuller recording.
Sound effects are in docs/SOUND_DESIGN.md.

## The whole battle: three pieces on the match's clock

A match is 3:00 of regulation and 2:00 of sudden death. Each piece starts
when its phase starts and is exactly as long (`match/MusicPlan.java`,
checked by `MusicSmokeTest`). The timeline came from the user's reference
of a mobile strategy game's battle cues (only the times; no audio or
melody was taken):

| Match time | What happens | Piece | Length |
|---|---|---|---|
| 0:00 (countdown) | start | **Arena March** (C major, 128 bpm), looped | 75 s loop |
| 2:00 | 60 s left, x2 elixir | **Arena Rush** (A minor, 144 bpm) | 60 s, once |
| 2:30 | 30 s left | (inside Rush, bar 19: more layers, bells, timpani, tom fills) | |
| 2:50 | 10 s countdown | (inside Rush, bar 31: bass pedal, ticks, brass stabs, snare roll, riser) | |
| 3:00 | sudden death | **Last Stand** (D minor, 144 bpm) | 120 s, once |
| 4:00 | 60 s left (x3 elixir) | (inside Last Stand, bar 37: octave up, busier) | |
| 4:50 | 10 s countdown | (inside Last Stand, bar 67: countdown as above, darker) | |

At 144 bpm one bar is 5/3 s, so every boundary falls on a bar line. A match
that ends early (a King falls, a tower in sudden death) just stops the
music. The sandbox has no clock, so it stays on Arena March.

| | Scripts |
|---|---|
| Notes | `scripts/music/arena_march.py` (phase 1), `late_scores.py` (Rush, Last Stand) |
| Sounds and Ogg encoding | `synth.py`; run `compose_phase1.py` and `compose_late.py` |
| MIDI | `python3 scripts/music/export_midi.py march|rush|last_stand [out.mid]` |

## Arena March (phase 1)

An original tune composed for this mod (no melody or audio from any other
game), heard from the countdown until the elixir speeds up.

| | |
|---|---|
| Score | `scripts/music/arena_march.py` (the only place the notes live) |
| In-game audio | `scripts/music/compose_phase1.py` synthesises it into `assets/royalarena/sounds/music/arena_phase1.ogg` (note-block instruments plus a soft horn) |
| MIDI | `python3 scripts/music/export_midi.py [out.mid]` (default `build/music/arena_march_phase1.mid`) |
| Tempo, key, length | 128 bpm, C major, 4/4, 40 bars = exactly 75 s, loops seamlessly |
| Form | A bells (bars 1-8), A fuller (9-16), B horn tune (17-24), A horn and bells (25-32), bridge (33-40); markers in the MIDI |
| Harmony | A: C G Am F C G F G. B: Am F C G Am F G G |

Edit the notes in `arena_march.py`, then run both scripts: the Ogg and the
MIDI stay in step.

## Orchestrating it for real

The synthesised version is light (note blocks); a real orchestration gets
the weight of a mobile strategy game's battle music. The MIDI has one track
per part with a General MIDI program as a starting point:

| MIDI track | Suggested real instruments |
|---|---|
| Lead - Horn (big tune) | French horns in unison, trumpets on the top notes |
| Lead - Bells | Glockenspiel (keep it: it is the Minecraft colour) |
| Lead - Pling | Piccolo or flute, or keep a square-wave lead |
| Lead - Harp (bridge) | Harp |
| Harp arpeggio | Harp, or pizzicato strings |
| Pad | Strings, sustained (add low brass in section 4) |
| Bass | Cellos and double basses, tuba on the downbeats |
| Drums | Field snare (march), bass drum, cymbals; a timpani roll at bar 32 |

Free tools: **BandLab** (browser, with orchestral packs) or **LMMS**
(desktop). Import the MIDI, pick instruments per track, export WAV.

## Asking an AI music app (Suno, Udio...)

Use the MIDI or the in-game Ogg as a melody reference if the app supports
an audio or melody upload; otherwise describe it. Do not name other games
or artists in the prompt (the apps refuse it, and the tune should stay
ours). Check the app's terms for using the result in a mod.

Prompt, orchestral:

> Instrumental, cheerful heroic march for a fantasy strategy battle, 128 bpm,
> C major, 4/4. Brass fanfare melody on French horns and trumpets,
> glockenspiel doubling the melody, harp arpeggios, pizzicato strings,
> marching snare and bass drum, light cymbals. Playful and confident,
> cartoon fantasy, bright mix, no vocals. Seamless loop, 75 seconds,
> ends leading back into the start.

Prompt, blockier (closer to the in-game version):

> Instrumental chiptune-orchestral march, 128 bpm, C major. Plucky
> note-block style bells and harp, square-wave lead, soft brass section for
> the main theme, march snare and kick, warm bass. Cheerful, adventurous,
> blocky pixel world, no vocals. Seamless 75 second loop.

## Bringing a new recording into the game

Send the file (WAV or Ogg, 44.1 kHz). It is then cut to exactly 75 s on the
loop point, brought to -19 dBFS rms with peaks under -3 dBFS (so the sound
effects stay clear), encoded to Ogg Vorbis and dropped in place of
`arena_phase1.ogg`; `MusicSmokeTest` checks the file and `sounds.json`.
