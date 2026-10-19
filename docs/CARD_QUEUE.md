# Card queue

The autopilot (`docs/AUTOPILOT.md`) adds the first unchecked card that is not
marked **(by hand)**, one per cycle. Cards marked (by hand) need a new
mechanic big enough to want a person watching: they are built in a normal
session, not by a cycle. Write your own designs at the top under "From the
user". Cost always comes from the formula; the numbers below are a
direction. Only mobs and potions that exist in Minecraft 1.20.1.

Rule for every Nether card: **fire immune** (`BattleCard.fireImmune()` and
`"fire_immune": true` in the JSON): Fireball and other fire still hurt it,
but it never burns. Already on Blaze, Ghast, Magma Cube, Zombified Piglin,
Hoglin, Wither Skeleton and Piglin Brute; add it to every new Nether mob.

## From the user

- [x] Zombie Villager (`minecraft:zombie_villager`): weaker than a Zombie, but
  a big group (5), so it is cheap swarm pressure. Count 5, about half a
  Zombie's health each, small hits.
- [x] Spider Jockey (`minecraft:spider` with a Skeleton on it): ONE unit,
  sturdier than the four-Spider card: a Skeleton archer riding a Spider.
  Splits when the spider dies (the Skeleton stays and shoots, like a Skeleton
  Archer). Around 4 elixir.
- [x] Fox (`minecraft:fox`): a fast small pack, nothing special (no armour
  piercing). Count 3-4, low health, quick bites.
- [x] Skeleton Rider (`minecraft:skeleton_horse` with a Skeleton Archer on
  it): one archer on a war horse that goes for the towers (buildings only),
  very fragile ("bones fall apart"). Horse carries it fast. Reuses
  `buildingsOnly`; ranged, range under 7.
- [x] Strider (`minecraft:strider`): Nether, so fire immune, but it does NOT
  cross the river. Its own trick (Claude's pick, say if you want another): it
  leaves a short-lived fire trail that burns enemies standing on it (1 a
  second). Fast and fragile.
- [ ] Ender Pearl (spell, `minecraft:ender_pearl`) **(by hand)**: a SMALL
  area, thrown on one troop. Own troop: teleported forward toward the enemy
  by a short, capped distance (about 5 blocks, never past the river into the
  enemy half). Enemy troop: pushed back toward its own King by the same
  distance. Whichever is better for the thrower. Cheap spell, 2 elixir.
- [ ] Warden (`minecraft:warden`) **(by hand)**: slow, heavy Nether-deep
  tank. Walks slowly; once a tower is in range it fires a sonic boom with a
  long delay (a line hit like the Evoker's, repeating). Drawn to the LOUDEST
  sound: every explosion or strong spell (Fireball, TNT, Lightning, a Creeper
  going off, a falling tower) in the last 5 s within 12 blocks pulls it
  toward the loudest one, then back to the tower. Range stays under 7.
- [ ] Shulker (`minecraft:shulker`) **(by hand)**: the first BUILDING card.
  Played on your own half, it stands still with health and 20 s to live:
  homing bullets that lift the target for 1.5 s (it cannot walk or attack,
  like a stun). When the time runs out it teleports away and vanishes. Enemy
  troops and towers can attack it like a troop; its health drains with time.

## Proposed by Claude (same rules)

- [x] Goat (`minecraft:goat`): a ram: hits knock back much harder than the
  Ravager, low health, slow swing. Reuses knockback.
- [x] Llama (`minecraft:llama`): spits from 4-5 blocks, slows a little. Reuses
  ranged and slowing.
- [x] Piglin (`minecraft:piglin`, Nether, fire immune): crossbow, range 5, two
  quick shots then a long reload. Reuses ranged.
- [ ] Allay (`minecraft:allay`) **(by hand)**: flies beside the nearest own
  troop and heals those within 2 blocks a little each second; does not fight.
- [x] Poison Cloud (spell) **(by hand)**: a circle that poisons everyone in
  it for 5 s (2 a second); stays 5 s.
- [x] Cobweb (spell) **(by hand)**: a circle for 4 s; troops in it walk 40%
  slower; no damage.
- [x] Swiftness Potion (spell) **(by hand)**: own troops in the circle run 40%
  faster for 5 s.

## Done

- [x] Husk, Wither Skeleton, Piglin Brute, Polar Bear, Phantom, Silverfish,
  Bee, Evoker, Guardian, Snow Golem (see docs/CARDS.md).
