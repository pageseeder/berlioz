# 0001. Record architecture decisions

- **Date:** 2026-10-29
- **Status:** Accepted

## Context

Decisions about Berlioz were recorded in several places: struck-through open questions in the roadmap, dated decision blocks inside roadmap themes, private implementation plans and notes. Developers using or contributing to Berlioz could not easily find why something works the way it does, and decisions were lost when a release section was rewritten.

## Decision

Record decisions that apply across releases or themes as Architecture Decision Records in `docs/decisions/`, using Michael Nygard's format. Decisions that only concern one roadmap theme stay in the Decisions section of that theme's design document in `docs/design/`.

Implementation plans remain private working documents; once a release ships, its lasting decisions become records here and its changes go in the changelog.

## Consequences

- The roadmap only lists direction and status; it links here for rationale.
- Records are immutable once accepted. Changing a decision means writing a new record that supersedes the old one.
- Existing decisions were recorded retroactively, dated with the release or day they were taken.
