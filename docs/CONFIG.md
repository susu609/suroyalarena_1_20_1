# Configuration

Su Royal Arena has two Forge config files. Forge creates them the first time
the game starts with the mod, and **reloads them when you save the file**.
You do not need to restart, and a running match picks up the new values.

| File | Side | Where (dev runs) |
|---|---|---|
| `royalarena-client.toml` | each player's game | `run-client1/config/`, `run-client2/config/` |
| `royalarena-common.toml` | whoever runs the match (the integrated server in singleplayer, or a dedicated server) | `run-client1/config/` in singleplayer, `run-server/config/` on a dedicated server |

In a normal Minecraft install both files are in `.minecraft/config/`.

## `royalarena-client.toml`: camera

```toml
[camera]
	pitch = 68.0       # degrees below the horizontal; 90 = straight down (20..89)
	distance = 26.0    # blocks from the camera to the point it looks at (5..80)
	lookAhead = 6.5    # blocks from the arena centre toward your side that it looks at (-16..16)
	fov = 0.0          # field of view in a match; 0 = keep your normal FOV (0..110)
```

The camera stays behind your own side and looks straight down the length of
the arena (no sideways swing). `pitch` sets how steep it is:

- `60..70`: almost top-down, the far towers seen at a slight diagonal (the
  default, 68).
- `80..89`: practically straight down.
- below `55`: a lower, more side-on view; raise `distance` so the far King
  tower stays on screen.

If the far end of the arena is cut off, increase `distance` or `fov`. If the
arena looks small, decrease `distance`. If your own towers hide behind the card
panel, increase `lookAhead`.

## `royalarena-client.toml`: effects

```toml
[effects]
	cameraShake = 1.0      # how hard the camera shakes on heavy hits; 0 = never (0..2)
	damageNumbers = true   # damage and healing numbers over troops and towers
	worldTextScale = 1.0   # size of damage numbers and health bars (0.5..2)
	teamRings = true       # a blue or red ring under every troop
	battleMusic = true     # the Pigstep disc on the Music volume while a battle runs
	menuHint = true        # "Press M to open Royal Arena" in the top left corner outside a match
```

## `royalarena-common.toml`: troops, bot, debug

```toml
[troops]
	speedMultiplier = 1.0   # multiplies every troop's walking speed (0.1..5)

[bot]
	attackMinElixir = 7     # elixir the bot saves before it attacks; lower = more aggressive (1..10)
	thinkEveryTicks = 10    # how often the bot looks at the board; 20 ticks = 1 s (1..100)
	minGapTicks = 40        # fewest ticks between two bot plays (0..400)
	personality = "random"  # random (a new one each match), tank_push, rush, counter or spell_siege

[debug]
	showRoutes = false      # particles from each walking troop to the point it walks to
	logMovement = false     # log troop movement once a second, and warn when a troop is stuck

[stats]
	recordMatches = true    # one line per finished match in royalarena/matches.jsonl (docs/MATCH_LOG.md)
```

Bot difficulty, roughly:

| Difficulty | `attackMinElixir` | `thinkEveryTicks` | `minGapTicks` |
|---|---|---|---|
| Easy | 10 | 30 | 80 |
| Normal (default) | 7 | 10 | 40 |
| Hard | 4 | 5 | 20 |

## Debugging troops that do not move

1. Set `showRoutes = true` and `logMovement = true` in
   `royalarena-common.toml` and save.
2. Start a match. Every walking troop draws a line of white particles to the
   point it is heading for, and a green particle at that point.
3. The log (`run-client1/logs/latest.log`) gets lines like:

   ```
   [royalarena] troop 1a2b3c4d entity.minecraft.zombie at (3.50, 81.00, 9.20) -> (2.00, 1.00) wanted 0.05 moved 0.05 onGround=true hp=20.00
   [royalarena] troop 1a2b3c4d entity.minecraft.zombie at (3.50, 81.00, 9.20) -> stays (in range or no target) wanted 0.00 moved 0.00 onGround=true hp=20.00
   [royalarena] troop 5e6f7a8b entity.minecraft.spider at (...) has wanted to walk for 2 s but has not moved (onGround=false)
   ```

   - `wanted` above 0 but `moved 0.00`, or the "has not moved" warning: the
     mob is told to walk but something in the world blocks it (a block, the
     arena edge, it is stuck in the air: look at `onGround`).
   - `stays (in range or no target)`: the combat engine is not asking the
     troop to walk; it thinks it is attacking or has nothing to go for.
   - No `[royalarena] troop` lines at all: the jar you run does not have this
     code (rebuild from the latest branch).

Send those lines along with a screenshot when reporting a movement bug.
