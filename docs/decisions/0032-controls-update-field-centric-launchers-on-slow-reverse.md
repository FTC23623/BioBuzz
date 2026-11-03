# 0032: Controls update: field-centric HyDrive, launchers on from START, slower reverse, drift on the left trigger

Status: Accepted. Not yet driven. The reverse speed is a starting guess.
Date: 2026-11-02
Changes parts of 0018 (the drift button) and 0023 (the driver's left trigger, the launcher button).

## Context

After driving the robot the coach asked for four changes:

1. Square (reverse) at full speed emptied the robot in one tap. The operator needs to let
   out one ball at a time.
2. The launchers should already be spinning when a mode starts, so the first shot is ready.
3. HyDrive should drive field-centric. It was robot-centric; only Practice: Field Centric
   drove the other way.
4. Drift assist should be on the driver's left trigger, not the right bumper.

## Decision

| Who | Button | Now does | Was |
|---|---|---|---|
| Driver, gamepad 1 | Left stick | Drive and strafe, field-centric | Robot-centric |
| Driver | Right bumper (hold) | Slow drive | Drift assist (Practice only) |
| Driver | Left trigger (hold) | Drift assist (Practice only; nothing in HyDrive) | Slow drive |
| Operator, gamepad 2 | Right bumper | Launchers OFF, press again ON | Launchers on, press again off |
| Operator | Square (hold) | Intake and transfer backward at `intakeReverseRpm` / `transferReverseRpm` = 1000 | 2900 |

- **`fieldCentricDrive = true`** in BotProperties. At INIT, point the robot away from the
  driver; circle re-sets forward during the match. The Practice TeleOps and Tool: Pose
  Recorder still set their own.
- **HyDrive calls `bot.SpinLaunchers(true)` the moment START is pressed**, the same line the
  autos already had (with `autoRunScoring`). Not during INIT: the robot must be still then.
  The test OpModes are unchanged; they start with the launchers off.
- **Slow mode moved to the right bumper** to make room for drift on the left trigger. The
  driver's buttons are now: left bumper aim, right bumper slow, left trigger drift (Practice),
  right trigger launch.
- **Reverse is a third of forward.** 1000 RPM is a guess; both numbers are on the dashboard's
  Intake group. Tune there, then write the winner into BotProperties.

## CHECK on the robot

1. START HyDrive: both launchers spin up with nothing pressed. Gamepad 2 right bumper stops
   them; again starts them.
2. Hold square on gamepad 2 with balls loaded: they come out slowly enough to let go after
   one. If not, lower the two reverse speeds on the dashboard.
3. Turn the robot 90 degrees and push the left stick forward: it drives away from the driver,
   not the way it faces.
4. Practice: Field Centric, hold the left trigger while turning: the robot pivots on the
   inside back wheel. Gamepad 1 right bumper: the drive slows to a quarter.

## Consequences

- Drift assist was off in HyDrive at first. 0034 turned it on everywhere, the same day.
- A launcher wheel missing from the robot configuration (last season's robot) is ignored by
  `SpinLaunchers`, so the Practice TeleOps still run there.
- Tests: `DrivePracticeOptionsTest` (drift on the trigger, slow on the bumper, 9 tests).
