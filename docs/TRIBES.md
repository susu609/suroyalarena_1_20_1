# Tribes

Every troop card belongs to one or two mob tribes. A deck counts the
different cards it carries of each tribe; at 2, 4, 6 and 8 cards a tribe
reaches a step, and every troop of that tribe on that side gets the step's
bonus for the whole match. Steps add up: a tribe at 6 cards has its 2, 4
and 6 bonuses. Steps 2 and 8 are plain stat bonuses, steps 4 and 6 an
effect. Spells belong to no tribe. A troop in two tribes gets both tribes'
bonuses.

Code: `card/Tribe.java` (tribes, steps, counting), `card/TribeBonus.java`
(what a bonus can change), `BattleCard.tribes()`, `combat/CombatEngine.java`
(applies them; `setTribes`). Config `troops.tribes` (default on). The deck
screen shows "Tribes: Zombie 4/6, Night 2/4" (count / next step) and each
troop's tooltip names its tribes.

## The tribes

| Tribe | Cards | 2 | 4 | 6 | 8 |
|---|---|---|---|---|---|
| Zombie | Zombie, Husk, Drowned, Zombie Villager, Zombified Piglin, Phantom | +5% health | heals 0.15 a second | heals 5% of the damage it deals | +15% health |
| Skeleton | Skeleton Archer, Skeletons, Stray, Wither Skeleton, Skeleton Rider, Spider Jockey | +5% damage | hits ignore armour | attacks 10% faster | +25% damage |
| Illager | Vindicator, Pillager, Evoker, Vex, Witch, Ravager | +5% damage | +20% damage to towers | attacks 5% faster | +25% damage |
| Nether | Piglin Brute, Blaze, Ghast, Hoglin, Magma Cube, Wither Skeleton, Zombified Piglin | +5% health | hits burn 0.5 a second for 2 s | +1 armour | +25% health |
| Arthropod | Spider, Cave Spider, Silverfish, Bee, Spider Jockey | walks 10% faster | hits poison 0.5 a second for 2 s | attacks 25% faster | +20% health and damage |
| Beast | Wolves, Fox, Polar Bear, Hoglin, Bee, Ravager | +5% damage | heals 10% of the damage it deals | walks 5% faster | +25% damage |
| Night | Zombie, Phantom, Skeleton Archer, Witch, Spider, Creeper, Enderman, Big Slime | +3% health and damage | its first hit freezes the target 0.2 s | +1 armour | +15% health and damage |
| Golem | Iron Golem, Snow Golem | +2 armour | | | |
| Slime | Big Slime, Magma Cube | +15% health | | | |
| Aquatic | Drowned, Guardian | +25% damage from the river | | | |

Only Night has 8 cards today, so step 8 is reachable only there (and only
with a deck of 8 troops, no spell). Nether reaches 8 when the queued Piglin
and Strider join; Arthropod has no step 6 or 8 yet (5 cards).

## Rules

- Counting: different cards, not units (Spider counts 1, not 3). Empty
  deck slots count nothing.
- Health and damage bonuses apply when the troop appears; a split child
  (slimes) inherits them, a Spider Jockey's or Skeleton Rider's archer gets
  its own (Skeleton Archer: Skeleton and Night).
- Armour bonuses add to the card's armour (docs/COMBAT.md). "Hits ignore
  armour" covers its splash too, not spells.
- Burn and poison from a tribe follow the usual rules: a fire-immune
  (Nether) troop never burns, golems are never poisoned; a card's own
  poison (Cave Spider, Bee, Wither Skeleton) takes the place of the tribe's.
- Lifesteal heals what the hit really took off (after armour).
- The first-hit freeze holds the victim's swing timer; it does not restart
  it (a stun would).
- The bot builds half of its random decks around a tribe (4 or 6 of its
  cards, `BotDeck.around`), so bots use the steps too.

## Balance

Tribe bonuses are power outside the cost formula, so they are measured on
their own: `scripts/tribes.sh 400` plays decks built around 4 (and 6) cards
of a tribe against plain random bot decks, each pairing twice with the
sides swapped, once with tribe bonuses and once without. "Off" is how the
same cards do together, "on" what the game plays, the gain what the steps
add. The target: about +5 at 4 cards, about +12 at 6.

The card win rates in `scripts/balance.sh` are measured on plain random
decks (with tribes on: a plain deck often reaches step 2 somewhere), so the
40-60% band means the same as before.

The tribes page, https://claude.ai/artifact/RswrRHCnGdbZyWxXkaVQp1 (built
by `scripts/card-library.sh` as `tribes.html`), shows the latest numbers
and has a deck tester; republish it when tribes or cards change.
Known issue: decks of 6 Skeleton-tribe cards already win about 85% against
plain decks without any bonus (cheap ranged troops together); the tribe
bonus is not the cause, and the cards each sit inside 40-60%.
