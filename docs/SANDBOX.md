# Sandbox

A practice arena with no clock and no opponent: one player plays any card
for either side, to try matchups, test a change, or watch how troops pick
their targets.

Start it with `/royalarena sandbox` or the **Sandbox** button on the
matching screen (M). You sit on BLUE's seat; nobody plays RED.

## Panel

The hand is replaced by a panel down the left edge (`client/hud/SandboxHud`):

| Button | What it does |
|---|---|
| Side: BLUE / RED (**Tab**) | The side the picked card is played for. Troops go on that side's half (and the river for Drowned), spells anywhere (TNT up to the middle), as in a match. |
| Cards... (**C**) | The card picker (`SandboxCardScreen`): every card in a grid, a name search, All / Troops / Spells, and a tribe filter. A click picks the card; the arena click plays it (right click drops it). The name under the panel shows the pick. |
| Clear troops | Removes every troop, landed spell, shot in the air and card on its way. |
| Restore towers | Every tower back to full health and asleep (Kings); fallen towers stand again and the rubble goes. |
| Towers: unbreakable / take damage | Unbreakable (the default): towers take nothing, but a hit King still wakes up and shoots. With damage on, towers fall as usual; a fallen King does not end the sandbox. |
| Elixir: endless / B n / R n | Endless (the default): cards cost nothing and both meters stay full. Off: each side has its own meter, filling at the normal 1x rate, and a card needs its side's elixir. |
| `||` 0.5x 1x 2x | Pause, half, normal or double speed (half: one match tick every other server tick). The panel works while paused, and cards played wait until time runs again. |
| Tribes... | `SandboxTribeScreen`: the tribe step (0 to its last) each side plays with, chosen by hand since the sandbox has no deck. Troops get their tribe bonus when they appear, so a change applies to the next ones. |
| Stats: on / off | The overlay (`SandboxStatsRenderer`): each troop's attack range (team colour, from the body's edge) and sight (faint white), and a yellow line to the troop it is aiming at (orange to its tower). Only on this screen. |
| Exit sandbox | Back to where you were. |

The "!" markers, the white spell-target light and the deploy ghosts work as
in a match, for the side the panel shows.

## Code

- `match/SandboxSettings.java` (the switches), `MatchInstance.enableSandbox`,
  `sandboxDeploy` (any card, no hand cycle), `resetTowers`, `clearPending`;
  no clock, no end, elixir rate 1; unbreakable towers in `damageTower`
  (and `hitWhileUnbreakable`, read by `CombatEngine.kingAwake`).
- `CombatEngine.tribeLevels` takes the sandbox steps; the sandbox session
  turns tribes on whatever the config says. `clearSpellsAndShots`.
- `server/match/MatchManager.startSandbox` / `handleSandbox`,
  `ServerMatchSession.handleSandbox` and `ticksThisServerTick` (speed).
  Sandbox matches are not written to the match log.
- `network/c2s/C2SSandboxPacket` (every panel action),
  `MatchSnapshot.Sandbox` (switches, both meters, tribe steps, troop aims),
  protocol 26.
- Client: `client/state/ClientSandbox` (pick, side, overlay),
  `ClientMatchState.selectedCard / deploySide / canDeploySelected` (shared
  with the match HUD), keys `SANDBOX_SIDE` (Tab) and `SANDBOX_CARDS` (C).
- Test: `SandboxSmokeTest`.

Tab is also vanilla's player list key; both happen. Rebind either under
Controls if the list gets in the way.
