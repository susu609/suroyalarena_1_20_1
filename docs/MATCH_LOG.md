# Match log

Every finished match adds one line of JSON to `royalarena/matches.jsonl` in
the game folder of whoever runs the match: the dedicated server, or the
client in singleplayer (dev runs: `run-client1/royalarena/matches.jsonl`).
The file only grows on that computer; nothing is sent anywhere. Turn it off
with `recordMatches = false` in `royalarena-common.toml` (`[stats]`).

Matches stopped before the battle starts are not logged. Matches stopped
without a result (the server stopping) are logged with `"end": "abandoned"`;
leave them out when counting wins.

## Privacy

Players appear only as an 8-character code (`"player": "294a0cdd"`): a
SHA-256 hash of their UUID, so the same player keeps the same code across
matches, but the code does not show who it is. No names, no chat, no IP
addresses. The bot has `"player": null`.

## A line, spread out

```jsonc
{
  "v": 1,                          // format version (MatchLog.FORMAT)
  "mod": "0.1.0",                  // mod version
  "id": "50339b52",                // first 8 characters of the match id
  "started": "2025-10-17T10:00:00Z",
  "mode": "bot",                   // "bot" (solo) or "pvp"
  "seconds": 300,                  // battle time, overtime included
  "overtime": true,
  "end": "tower_health",           // king, overtime_tower, crowns,
                                   // tower_health, disconnect, abandoned
  "winner": "red",                 // blue, red or draw
  "crowns": {"blue": 0, "red": 0},
  "towers": {"blue_left": 39, "blue_right": 110, "blue_king": 185,
             "red_left": 101, "red_right": 67, "red_king": 185},
  "blue": {
    "kind": "player",              // or "bot"
    "player": "294a0cdd",
    // a bot side also has "personality": "tank_push" | "rush" | "counter" | "spell_siege"
    "deck": ["healing_potion", "stray", "tnt", "fireball",
             "zombie", "magma_cube", "skeleton_archer", "freeze"],
    "fullElixirSeconds": 0,        // time sat at 10 elixir (income wasted)
    "elixir": [50, 55, 11, ...],   // tenths of elixir at start, then every 10 s
    "towerShots": 433.5,           // health this side's towers took off troops
    "cards": {                     // every card of the deck, played or not
      "stray": {
        "plays": 10,
        "elixir": 20,              // plays x cost
        "firstPlay": 14,           // seconds into the battle, null if never
        "damage": 195.5,           // health taken off enemy troops
        "towerDamage": 15,         // health taken off enemy towers
        "kills": 8,                // enemy troops it finished off
        "healing": 0               // health given back to own troops
      },
      ...
    }
  },
  "red": { ... }
}
```

Damage is what really came off: a 25 damage Fireball on a Zombie with 20
health left counts 20. Burn damage counts for the spell that set the burn,
splash for the troop that splashed, and an arrow still counts for its archer
if the archer died while the arrow flew. Health and damage are on the
vanilla scale like everything else (a Zombie has 20).

## Reading it

Each line is a whole JSON document, so any JSON tool reads the file line by
line. For example, with Python:

```python
import json
matches = [json.loads(l) for l in open("matches.jsonl")
           if l.strip()]
played = [m for m in matches if m["end"] != "abandoned"]
```

`MatchLogSmokeTest` plays a bot match, checks the line is valid JSON and that
its numbers agree with the match: plays match the accepted cards, elixir is
plays x cost, and the tower damage credited to a side's cards adds up exactly
to the tower health the other side lost.
