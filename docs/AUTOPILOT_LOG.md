# Autopilot log

One line per cycle, newest last (`docs/AUTOPILOT.md`).

- 2026-10-17 7eb9804: tuned Ghast (63% -> 57%), Ravager (60% -> 55%), Freeze (24% -> 44%), Enderman, Cave Spider, Creeper; band 43-58% (done by hand, before the autopilot)
- 2026-10-17 8909346: added Husk, Wither Skeleton, Piglin Brute, Polar Bear, Phantom, Silverfish, Bee, Evoker, Guardian; deck filters; band 41-58% (by hand)
- 2026-10-17 e790250: added Snow Golem; tuned Skeleton Archer (60% -> 58%), Ghast (59% -> 57%), Blaze (59% -> <55%); band 42-58%
- 2026-10-17 0a065f0: maintenance only (queue empty); tests green; band 42-58% (Polar Bear, Skeleton Archer 58%; Bee, Zombie 42%); no change
- 2026-10-18 18b0874: added Zombie Villager (51%); Bee 40% below the band, three tweaks tried and none helped (see CARDS.md); band 40-58%
- 2026-10-18 1f345e4: added Spider Jockey (52%); tuned Ghast (60% -> 57%); band 41-59% (Wither Skeleton 59%, Bee 41%: noise at the edge); TNT reach limit and sound wiring done by hand earlier
- 2026-10-18 e5ec81d: by hand: Bee lives on and stings once a second (41% -> 55%); Big Slime 36 health at 4 elixir, speed 0.75 (71% at 3 elixir -> 56%)
- 2026-10-18 2e6c43c: by hand: Ghast and Skeleton Archer range 5.0 (60% -> 55%, 59% -> 55%); Husk 3 x 13 HP, slow 2 s, speed 1.0 (41% -> 45%); Hoglin 50/10 (43% -> 44%); Enderman 68 HP, teleport every 5 s (43% -> 50%); Evoker range 4.5 (59% -> 56%); bot heals only fighting troops at 70% or less (Healing 43% -> 45%)
- 2026-10-18 78dc43f: by hand: bot personalities (Tank Push 55%, Rush 43%, Counter 55%, Spell Siege 46%) and shared bot skills; matches 284 s -> 267 s; band 40-58% (Creeper 40%: tune it next)
- 2026-10-18 a088338: by hand: watch mode, readable damage numbers and bars, deep team colours, docs and stats page refreshed; band 40-58% (Skeleton Archer 58%, Creeper 40%); next card in the queue: Fox
