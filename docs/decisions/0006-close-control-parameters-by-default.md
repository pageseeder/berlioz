# 0006. Close control parameters by default

- **Date:** 2026-07-21 (0.14.0)
- **Status:** Accepted

## Context

Request parameters such as `berlioz-reload` and `berlioz-profile` let anyone who can reach the application reload its configuration, clear caches or expose timings. The options considered for 0.14.0 were requiring an explicit control key, disabling controls unless configured, or a separate enable flag.

## Decision

Control parameters are ignored unless the request is authorized by one of three independent channels, any one of which is enough:

- **Key**: `berlioz.control.key`, a shared secret sent as `Authorization: Berlioz <key>`. Empty by default, so the channel is closed.
- **Network**: `berlioz.control.network`, `off` by default. `loopback` and `lan` are development conveniences only: behind a reverse proxy, every request appears to come from the proxy. As a safety net, every address in `X-Forwarded-For`, `Forwarded` and `X-Real-IP` must also match.
- **Delegated**: the host application sets the request attribute `ControlAuthorization.CONTROL_AUTHORIZED_ATTRIBUTE` after its own authentication.

Unauthorized control parameters are silently ignored. Unrecognized option values fail closed.

## Consequences

- A default deployment exposes no control surface.
- Developers typically set `berlioz.control.network=loopback` locally; production uses the key or the delegated channel.
- Other features reuse the same authorization rather than adding their own, such as the `requires-control` servlet parameter that guards the kickstart `*.src` mapping.
- The set of control parameters is kept to a minimum: since 0.14.5, `berlioz-reload` (with scopes) and `berlioz-profile` ([theme 17](../design/17-scoped-reload.md)).
