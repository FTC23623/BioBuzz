# Decisions

Why the code is shaped the way it is. One file per decision, numbered in order.

Read these before changing anything they cover. If a decision changes, write a new file
that replaces the old one and mark the old one **Replaced by NNNN**. Do not rewrite history.

| # | Decision | Status |
|---|---|---|
| [0001](0001-one-opmode-alliance-from-hardware.md) | One OpMode; alliance comes from a badge switch | Accepted, switch not built yet |
| [0002](0002-drive-port.md) | How the mecanum drive came over from Decode | Accepted, bench test pending |
| [0003](0003-quality-gates.md) | Tests and checks from day one, aimed at the coach | Accepted, partly built |
| [0004](0004-safe-mode.md) | Safe Mode: drive limited to 25% unless a robot switch says FULL | Accepted, switch not wired yet |
| [0005](0005-hub-imu-fallback.md) | Use the Control Hub IMU when there is no Pinpoint | Accepted, mounting not checked yet |
| [0006](0006-code-for-students.md) | Write the code for students to read | Accepted |

## Template

```markdown
# NNNN: Title

Status: Proposed | Accepted | Replaced by NNNN
Date: YYYY-MM-DD

## Context
What problem, and what was true when we decided.

## Decision
What we chose.

## Consequences
What this makes easier, what it makes harder, what to watch for.
```
