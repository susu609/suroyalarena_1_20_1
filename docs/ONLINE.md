# Online deployment

Royal Arena MVP does not need a separate web API, database, Redis, login service
or global matchmaking backend.

## Current model

```text
Player A ─┐
          ├── one Forge 1.20.1 dedicated server
Player B ─┘       ├── royalarena
                  └── sumodlib
```

`MatchQueue` only pairs players already connected to that Minecraft server.
Match runtime is held in server memory and is authoritative.

## Local development

The Gradle configuration includes:

- `client` -> Dev1
- `client2` -> Dev2
- `server` -> dedicated server working directory `run-server`

For a fast two-client test, connect Dev1 and Dev2 to the same server and either
use the Matching screen (`M`) or `/royalarena debug_start`.

## Internet access

For development or a small private test, expose the Forge dedicated server
through normal port-forwarding or a TCP tunnel service. This can be free
depending on the chosen provider and limits.

For a public server that must remain available 24/7, use a machine/VPS/Minecraft
host that can run Forge 1.20.1 and lets you install both custom JARs (
`royalarena` and `sumodlib`). Hosting cost is infrastructure cost, not a
requirement of the matchmaking code itself.

## When an external backend becomes useful

Only add a separate service when the design actually needs multiple Minecraft
servers/global queues, persistent global ranking, account-level data shared
between servers, or fleet orchestration. That is outside Parts 0–10 MVP.
