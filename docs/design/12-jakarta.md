# 12. Jakarta Servlet Support

**Status:** Deferred · **Target:** Undecided

## Motivation

Move Berlioz to the Jakarta Servlet namespace for modern servlet containers when the application migration window is clear.

The migration remains intentionally deferred while many existing applications remain on `javax.servlet`. The `javax` line continues as the active release until a migration plan is agreed.

## Likely Outcome

- A Jakarta-based Berlioz line for current containers.
- Clear migration notes for applications moving from `javax.servlet` to `jakarta.servlet`.
- A defined maintenance policy for the `javax.servlet` line.

## Open Questions

- Should Jakarta support be a breaking major release, a parallel artifact, or a later 1.x line?
- Should the `javax.servlet` line become maintenance-only immediately, or remain active until a specific application migration threshold is reached?
- Should servlet-specific code be isolated enough to support parallel `javax` and `jakarta` artifacts?
- How should the mock and kickstart modules track the migration?
