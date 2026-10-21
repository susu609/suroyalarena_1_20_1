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
| 2:00 | 60 s left, x2 elixir | **Arena Rush** (A minor, 144 bpm): Am Em/G F E7 Am Dm F E | 60 s, once |
| 2:30 | 30 s left | (inside Rush, bar 19: 16th arp, brass, bells, timpani) | |
| 2:50 | 10 s countdown | (inside Rush, bar 31: bass pedal, ticks, brass stabs, snare roll, riser) | |
| 3:00 | sudden death | **Last Stand** (D minor, 144 bpm): Dm Bb Gm A7 Dm F/C Bb A | 120 s, once |
| 4:00 | 60 s left (x3 elixir) | (inside Last Stand, bar 37: brass, bells, 16th arp, four on the floor) | |
| 4:50 | 10 s countdown | (inside Last Stand, bar 67: countdown as above, darker) | |

At 144 bpm one bar is 5/3 s, so every boundary falls on a bar line. A match
that ends early (a King falls, a tower in sudden death) just stops the
music. The sandbox has no clock, so it stays on Arena March.

| | Scripts |
|---|---|
| Notes | `scripts/music/arena_march.py` (phase 1), `late_scores.py` (Rush, Last Stand) |
| Accompaniment (bass, arp, drums by bar) | `arranger.py`; note names and chords in `notes.py` |
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
| Form | A bells (bars 1-8), A with a pling line under the bells (9-16), B horn tune (17-24), A in D major, horn and bells (25-32), piano bridge (33-40) with a snare roll and riser back into the loop; markers in the MIDI |
| Harmony | A: C G/B Am F C/E F D7 Gsus4-G. B: Am F C G Am F Bb G-A (into D). Bridge: Fmaj7 Em7 Dm7 G7sus Fmaj7 Em7 Dm7 G |
| Hook | two bars: a pickup, a leap up a fifth to a long note, a turn; Rush and Last Stand bring it back in minor |

## Why it sounds less machine-made

| What | Where |
|---|---|
| Played a few ms early or late (drums tighter), downbeat and beats leaned on, off 16ths lighter, no two hits equally loud | `synth.Mix.play(humanize=True)` |
| Bass changes by bar (four patterns; the 4th walks into the next chord), arp has four shapes, the 8th bar thins out | `arranger.py` |
| Drum grooves alternate, ghost snares, a short pickup every 4 bars and a fill (snare ramp or toms) every 8 | `arranger.drums_bar` |
| Rests in the tune, sections louder and softer (0.85 to 1.1, bridge 0.7), a key lift | `arena_march.SECTIONS` |
| Each later phase louder (-20.5, -18.5, -17.5 dBFS rms), each opens on a hit, the old piece fades 1 s in game | `compose_*.py`, `late_scores._hit`, `FadingMusic` |

Edit the notes in `arena_march.py`, then run both scripts: the Ogg and the
MIDI stay in step.

## Demos: five 30-second directions

`python3 scripts/music/demos.py [out_dir]` (default `build/music/demos`)
writes five original 30 s demos, to pick a direction before writing a full
piece. Same instruments and accompaniment as the in-game music.

| Demo | Mood | Tempo, key | Instruments |
|---|---|---|---|
| 1 Bridge Charge | heroic march | 120 bpm, Bb major | horn tune with bells, brass B section, march snare, fanfare bar and cadence |
| 2 Goblin Gold | playful | 96 bpm, F major | plucked (pizzicato) bass and harp, staccato bells, woodblock |
| 3 Tower Siege | tense battle | 144 bpm, D minor | 16th harp ostinato, strings, brass and horn, timpani, riser |
| 4 Tavern Jig | swung dance | 128 bpm, G major | plucky lead, harp, claps, bells and horn on the repeat |
| 5 Crown Fanfare | victory | 160 bpm, C major | brass call, tune twice (second with strings and timpani) |

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
| Piano (bridge) | Piano or celesta, soft |
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
loop point, brought to -20.5 dBFS rms (Rush -18.5, Last Stand -17.5) with peaks under -3 dBFS (so the sound
effects stay clear), encoded to Ogg Vorbis and dropped in place of
`arena_phase1.ogg`; `MusicSmokeTest` checks the file and `sounds.json`.
