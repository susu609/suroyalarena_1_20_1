# Tribes

Every troop card belongs to one or two mob tribes. A deck counts the
different cards it carries of each tribe; at 2, 3, 4, 5 and 6 cards a tribe
reaches a step, and every troop of that tribe on that side gets the step's
bonus for the whole match. Only that tribe's troops get it: a Vindicator
in a Zombie deck gets nothing from the Zombie tribe. Steps add up: a tribe
at 5 cards has its 2, 3, 4 and 5 bonuses. Most steps are small; step 5 is
each tribe's special rule (rising again, a totem, a last flame...). Spells
belong to no tribe. A troop in two tribes gets both tribes' bonuses.

Code: `card/Tribe.java` (tribes, steps, counting), `card/TribeBonus.java`
(what a bonus can change), `BattleCard.tribes()`, `combat/CombatEngine.java`
and `combat/CombatUnit.java` (apply them; `setTribes`), the tribe events in
`TickResult` (particles and sounds in `BattleField`). Config `troops.tribes`
(default on). The deck screen shows "Tribes: Zombie 4/5, Night 2/3" (count /
next step) and each troop's tooltip names its tribes.

## The tribes

| Tribe | Cards | 2 | 3 | 4 | 5 (special) | 6 |
|---|---|---|---|---|---|---|
| Zombie | Zombie, Husk, Drowned, Zombie Villager, Zombified Piglin, Phantom, Zoglin, Chicken Jockey | +5% health | heals 0.15 a second | heals 5% of the damage it deals | **Rise again**: a fallen Zombie-tribe troop gets up with 25% health (one for the side every 20 s; souls, cure sound) | +5% health |
| Skeleton | Skeleton Archer, Skeletons, Stray, Wither Skeleton, Skeleton Rider, Spider Jockey | +5% damage | attacks 5% faster | hits ignore armour | **Volley**: every 3rd shot also hits a second enemy in range for half | +5% damage |
| Illager | Vindicator, Pillager, Evoker, Vex, Witch, Ravager | +5% damage | +10% damage to towers | walks 5% faster | **Totem of Undying**: a killing blow leaves it at 1 health, unhurt for 0.5 s (one for the side every 20 s) | +5% damage |
| Nether | Piglin Brute, Blaze, Ghast, Hoglin, Magma Cube, Wither Skeleton, Zombified Piglin, Strider, Piglin, Zoglin | +5% health | hits burn 0.5 a second for 2 s | +1 armour | **Last flame**: when it dies, enemies within 2 blocks burn 1 a second for 3 s | +5% health |
| Arthropod | Spider, Cave Spider, Silverfish, Bee, Spider Jockey | walks 10% faster | hits poison 0.5 a second for 2 s | attacks 10% faster | **Silk**: its hits slow the target's walk for 0.5 s | +5% health and damage (needs a 6th card) |
| Beast | Wolves, Fox, Polar Bear, Hoglin, Bee, Ravager, Strider, Goat, Llama, Cat, Piglets, Killer Rabbits, Panda, Camel | +5% damage | heals 10% of the damage it deals | walks 5% faster | **Pack**: +5% damage for each other Beast within 4 blocks (up to 3) | +5% damage |
| Night | Zombie, Phantom, Skeleton Archer, Witch, Spider, Creeper, Enderman, Big Slime | +2% health and damage | its first hit freezes the target 0.1 s | +1 armour | **Shadow**: an arrow or shot that would hit it misses (one for the side every 16 s) | +2% health and damage |
| Golem | Iron Golem, Snow Golem | +2 armour | | | | |
| Slime | Big Slime, Magma Cube | +15% health | | | | |
| Aquatic | Drowned, Guardian, Frog, Turtle | +25% damage from the river | | | | |

Arthropod has 5 cards, so its step 6 waits for a sixth.

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
- Rise again, the totem and Shadow are for the whole side, not every troop:
  one revive every 20 s, one totem every 20 s, one dodge every 16 s (given
  to each troop of a swarm they made a second life per body, far too
  strong: Zombie decks went from 45% to 94%). A troop is saved at most
  once. They work against any death (blows, spells, poison, burning); a
  troop that dies by its own strike (Creeper, Vex) does not rise.
- Volley needs a second enemy troop within the shooter's range; a
  tower-only troop (Skeleton Rider) has no volley.
- Shadow dodges arrows, fireballs and other shots in flight (tower shots
  too), not melee blows or spells.
- The bot builds half of its random decks around a tribe (3 to 6 of its
  cards, `BotDeck.around`), so bots use the steps too.

## Balance

Tribe bonuses are power outside the cost formula, so they are measured on
their own: `scripts/tribes.sh 400` plays decks built around 2 to 6 cards
of a tribe against plain random bot decks, each pairing twice with the
sides swapped, once with tribe bonuses and once without. "Off" is how the
same cards do together, "on" what the game plays, the gain what the steps
add. Bot matches mostly go to overtime and are decided by a few points of
tower damage, so small edges move win rates a lot; read the gains to
compare tribes, not as what a player would see. Latest (400 matches a
row): a few points for steps 2-4, +6 to +19 at 5 cards, +10 to +27 at 6.

The card win rates in `scripts/balance.sh` are measured on plain random
decks (with tribes on: a plain deck often reaches step 2 somewhere), so the
40-60% band means the same as before.

The tribes page, https://claude.ai/artifact/RswrRHCnGdbZyWxXkaVQp1 (built
by `scripts/card-library.sh` as `tribes.html`), shows the latest numbers
and has a deck tester; republish it when tribes or cards change.
Known issue: decks of 6 Skeleton-tribe cards already win about 85% against
plain decks without any bonus (cheap ranged troops together); the tribe
bonus is not the cause, and the cards each sit inside 40-60%.
