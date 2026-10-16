# Su Royal Arena: notes for Claude

## ResourceLocation: use `RL`, never `new ResourceLocation(...)`

In Minecraft 1.20.1 the `ResourceLocation` constructors are deprecated for
removal and produce `[removal]` compiler warnings. Build ids with
`net.susu609.modlib.resource.RL` (from `sumodlib`) instead:

| Instead of | Use |
|---|---|
| `new ResourceLocation(ns, path)` | `RL.of(ns, path)` |
| `new ResourceLocation("ns:path")` (trusted id) | `RL.require("ns:path")` |
| `new ResourceLocation("minecraft", path)` | `RL.mc(path)` |
| parsing config or user input | `RL.parse(value)` (returns `null` when invalid) |

`RL.of` and `RL.require` throw `IllegalArgumentException` on an invalid id, so
use them only for ids written in code or data shipped with the mod.

`DomainSmokeTest` fails the build (`gradlew check`) if `new ResourceLocation(`
appears anywhere in `src/main/java`.
