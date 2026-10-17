# Autopilot: one maintenance cycle

A scheduled Claude Code run follows this file every few hours (see "Schedule"
below). One cycle = maintain, add at most one card, balance, document, push.
Edit this file to change what the cycles do; the next cycle reads it fresh.

## Rules

- Work on branch `ccr-7ae41114-t0mcxu` only. Never push to `master`, never
  open a pull request, never force-push.
- Follow `CLAUDE.md` (`RL`, never `new ResourceLocation(`).
- Never skip, disable or weaken a test to get green. A test whose number
  changed because a card changed gets the new number, with the reason in its
  message.
- Every cost equals `CostFormula` (`costsFollowTheFormula`). Changing a
  formula constant is allowed only with the evidence written in
  `docs/BALANCE.md`.
- Gradle cannot reach Forge's Maven in the cloud: test with
  `scripts/pure-test.sh`. Code that imports Minecraft/Forge cannot be compiled
  here, so check every vanilla API against
  `https://raw.githubusercontent.com/extremeheat/extracted_minecraft_data/client1.20.1/client/net/minecraft/...`
  before using it, and keep such changes small.
- If the cycle cannot finish green, revert the cycle's own unfinished work,
  write the blocker in `docs/AUTOPILOT_LOG.md`, and push only the log.
- At most one new card and at most three balance changes per cycle.

## Steps

1. **Sync.** `git pull origin ccr-7ae41114-t0mcxu`. Read the last entries of
   `docs/AUTOPILOT_LOG.md` and the queue in `docs/CARD_QUEUE.md`.
2. **Maintain.** Run `scripts/pure-test.sh`; fix any failure first. Also
   `grep -rn "new ResourceLocation(" src/main/java` (must be empty) and look
   for docs that disagree with the code (costs and stats in `docs/CARDS.md`).
3. **Add one card**, the first unchecked one in `docs/CARD_QUEUE.md` that is
   not marked "(by hand)" (designs written by the user come before Claude's
   proposals). Every Nether mob gets `fireImmune()` and `"fire_immune": true`.
   Stop adding when the roster has 55 cards or nothing unmarked is left. Touch points, as for the seven
   mobs in commit 8c9ba7d:
   - `card/BattleCard.java` (append before the spells; cost from the formula),
     `data/royalarena/cards/<id>.json` (same numbers, `model_scale`),
     `model_sources/card_art/royalarena_card_art.js` (CARDS: id and cost)
   - tactics: `"roles"` in the JSON (`attack`, `defense`, `support`) and the
     same in `BattleCard.tactics()`
   - a new rule: `card/TroopTraits.java` (+ formula modifier with a reason),
     `combat/CombatEngine.java`, `client/screen/DeckScreen.java` (tooltip),
     `server/battle/BattleField.java` (visuals), JSON trait key in
     `CombatSmokeTest`
   - a test of the rule (like `NewMobsSmokeTest`); range under 7.0
   - bump `PROTOCOL` in `network/ModNetwork.java` when the card list changes
   - `docs/CARDS.md` row, `docs/COMBAT.md` for a new rule
   - tick the card in the queue
4. **Balance.** `scripts/pure-test.sh && scripts/balance.sh 4000`. Every card
   should be within 42-58% (target band 40-60%, 2 points of slack for noise).
   For a card outside, change formula-neutral levers first (range, splash,
   speed past 1.33, HP-damage swap), measure each try with
   `scripts/balance.sh 2000`, keep the cost on the formula, and record what
   moved it in `docs/CARDS.md` ("Tuning into 40-60%"). Bot behaviour fixes go
   in `bot/BotBrain.java` and `docs/BOT.md`.
5. **Document.** `java -cp build/pure net.susu609.royalarena.BalanceReport
   1000 docs/BALANCE_REPORT.md`, a Part in `docs/PARTS.md` when code changed,
   one line in `docs/AUTOPILOT_LOG.md`:
   `- <UTC date> <short hash>: added X; tuned Y (a% -> b%); band lo-hi%`.
6. **Push.** Commit (clear message, the session's trailers), then
   `git push -u origin ccr-7ae41114-t0mcxu`. If nothing changed, push nothing.
7. **Report** in one short message (Vietnamese): what was added, tuned,
   fixed, the band, anything that needs the user (a design question, a
   blocker).

## Schedule

A scheduled task fires this cycle every 4 hours into the Claude Code
session that set it up. Stop it by telling that session "dừng autopilot" (or
delete the scheduled task in claude.ai/code). Each cycle uses usage like a
normal session turn.
