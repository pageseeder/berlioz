# 8. Authentication And Authorization Guards

**Status:** Exploring · **Target:** 1.0

## Motivation

Berlioz should make it easy for applications to guard services and generators without becoming responsible for authentication itself.

Authentication remains a separate concern. Applications, servlet containers, filters, reverse proxies, session systems, or identity providers can establish the current user or principal before Berlioz executes a service. Berlioz can then provide small authorization hooks to decide whether a service or generator may run.

Berlioz Admin also needs this: it wraps every Berlioz generator it uses in a delegating class that checks the admin verdict first, because Berlioz cannot guard a service. A guard would let applications use Berlioz generators directly and ensure a new service cannot forget the check ([theme 18](18-admin-overlay-upstreaming.md), item 10).

## Expected Behavior

- Return `401 Unauthorized` when authentication is required but no authenticated user is available.
- Return `403 Forbidden` when the user is authenticated but lacks the required permission, role, or capability.
- Use Problem Details for machine-readable authorization failures.
- Include `WWW-Authenticate` headers for authentication schemes that require them.

## Possible Guard Models

- Programmatic generator guards, for example `Guard.authenticated().and(canWrite(document))`.
- Generator interfaces such as `GuardedGenerator`, `RequiresAuthorization`, or `PermissionCheck`.
- Service-level requirements in `services.xml`, with application-defined meanings for roles and permissions.
- An application-provided `AuthorizationManager` that evaluates declared requirements.
- Lightweight [interceptors](09-interceptors-observability.md) that run before services or generators.
- Optional annotation support in a separate module, for example `@RequiresRole` or `@RequiresPermission`.

The core model should not force role-based, permission-based, attribute-based, or application-specific policy. It should provide a small result model:

- Allowed.
- Authentication required.
- Forbidden.
- Custom problem response.

## Open Questions

- Should authorization requirements be declared primarily by generator interfaces, service configuration, or both?
- What minimum user/principal abstraction should Berlioz expose without taking ownership of authentication?
- Should authorization checks run at service level, generator level, or both?
