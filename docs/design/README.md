# Design Documents

Each roadmap theme has a design document: why the work is needed, its planned or delivered shape, the decisions taken and the questions still open. The [roadmap](../../ROADMAP.md) lists the themes with their target release and status.

Numbers are stable identifiers, not priorities; they are referenced from other projects (for example Berlioz Admin) and are never reused.

| Status | Meaning |
|---|---|
| Exploring | Problem understood, approach open |
| Proposed | Approach sketched |
| Designed | Ready to implement |
| Done | Released; the document records what was delivered and why |

## Writing a design document

Name the file `NN-short-name.md` and start with:

```markdown
# NN. Title

**Status:** Proposed · **Target:** 1.0

## Motivation
## Proposal
## Decisions
## Open Questions
```

Keep the document focused on the shape of the change and its rationale. Step-by-step implementation plans stay out of the repository; what shipped goes in the [changelog](../../CHANGELOG.md).
