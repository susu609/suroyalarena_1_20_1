# Card queue

The autopilot (`docs/AUTOPILOT.md`) adds the first unchecked card, one per
cycle. Write your own designs at the top under "From the user": they go
first, and their numbers are a direction (cost comes from the formula).
Claude's proposals below can be edited, reordered or deleted. Only mobs that
exist in Minecraft 1.20.1.

## From the user

(empty: add `- [ ] Mob: idea` lines here)

## Proposed by Claude

Done in Part 37 are ticked (their final stats are in docs/CARDS.md).


- [x] Husk (`minecraft:husk`): a Zombie whose hit slows (hunger); 2 units, a little less health than Zombies. Reuses `slowing`.
- [x] Wither Skeleton (`minecraft:wither_skeleton`, stone sword): tall, slow-swinging heavy melee whose hit withers (poison, dark particles instead of green). Reuses `poisoning`.
- [x] Snow Golem (`minecraft:snow_golem`): ranged snowballs for almost no damage that slow; strong against Blaze (frost). Reuses `slowing`; Blaze weakness to its snowballs is a new rule.
- [x] Piglin Brute (`minecraft:piglin_brute`, golden axe): high damage, slow axe like the Vindicator but sturdier, 4 elixir.
- [x] Phantom (`minecraft:phantom`): fragile flyer that crosses the river anywhere and hovers; hit-and-run. Reuses `hovering`.
- [x] Silverfish (`minecraft:silverfish`): swarm of 6 tiny fast units, 1 elixir range. Count >= 5 modifier.
- [x] Polar Bear (`minecraft:polar_bear`): melee mini-tank, immune to Freeze (frost) as its rule.
- [x] Guardian (`minecraft:guardian`): laser beam that charges 2 s then hits hard; can be played on the river and fights better there (reverse water rule).
- [x] Evoker (`minecraft:evoker`): stays back and summons fangs in a line toward its target (new rule: line splash).
- [x] Bee (`minecraft:bee`): swarm of 5 that sting once, poison and die. Reuses `selfDestruct` + `poisoning`.
