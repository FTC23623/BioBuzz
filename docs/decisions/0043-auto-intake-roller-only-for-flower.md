# 0043: In autos, the intake roller runs only during FLOWER; the transfer feeds launches at 30%

Status: Accepted. Not yet driven.
Date: 2026-11-03
Changes 0023 (autos ran the intake and transfer from START) and Part B item B4.

## Context

Autos ran both rollers flat out from START. Driving over balls with the intake on picks up
more than we want, and the coach wants intaking in an auto to be on purpose: during a FLOWER
step now, and a separate intake step later.

## Decision

- **The transfer runs from START to the end of every auto**, as before.
- **The intake roller stays off** (`autoIntakeRoller = false`), except during a FLOWER step,
  which turns it on and puts it back afterwards. `Intake.SetIntakeRollerOn` and
  `Bot.SetIntakeRoller` do this; TeleOp never calls them, so TeleOp is unchanged.
- **A launch feeds the transfer at `launchFeedTransferScale` (30%)**, with the transfer
  paused while the gates swing open, the same as TeleOp. The intake roller stays off during
  an auto launch.
- **No auto-reject with the intake roller off**: nothing is coming in, and a reject would
  stop the transfer for the rest of the auto.

## CHECK on the robot

Run any auto with balls loaded: the intake roller is still until a FLOWER step, then runs
until the arm is back in. At each LAUNCH the transfer pauses, then feeds slowly.

## Consequences

- A ball the robot drives over is not picked up between FLOWER steps.
- `autoIntakeRoller = true` brings back the old behavior for an auto that needs it.
- Tests: `IntakeTest` (transfer only, 30% feed, FLOWER on, no reject, default; 5).
