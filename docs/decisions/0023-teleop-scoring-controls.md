# 0023: TeleOp scoring controls: driver launches, operator feeds

Status: Accepted. Not yet driven. Servo and motor directions are CHECK (0019, 0020). Driver's left trigger, launcher start-up and reverse speed changed by 0032. 0038: D-pad up / down trims the distance table instead of setting the pollen speed.
Date: 2026-10-30
Replaces part of 0019 (one shared target for both launchers, one for intake and transfer).

## Context

HyDrive drove, and nothing else. The launchers, intake, transfer and launch servos only
ran in the test OpModes. The coach asked for: hold-to-open launch buttons, a reverse for
when the robot has picked up too much, and every RPM tuned on its own. The driver wants to
launch with the triggers. Nectar is not part of the first competition, so both gates open
together for now.

## Decision

| Who | Button | Does |
|---|---|---|
| Driver, gamepad 1 | Right trigger (hold) | Both launch gates open; once open, the intake and transfer run forward to push balls in (if they were off). Let go: closed. |
| Driver | Left trigger | Slow drive (unchanged). Boost is gone. |
| Driver | Left bumper (hold) | Aim at our hive (0021). |
| Operator, gamepad 2 | Right bumper | Launchers spin up / off. |
| Operator | D-pad up / down | Pollen launcher speed. |
| Operator | D-pad right / left | Nectar launcher speed. |
| Operator | Cross | Intake and transfer on / off. |
| Operator | Left trigger (hold) | Intake and transfer on while held (2026-10-31). |
| Operator | Square (hold) | Intake and transfer BACKWARD. Let go: back to before. |

- New parts: `Launcher` (two `LauncherWheel`s), `Intake` (two `Roller`s), `LaunchGates`.
  Each has a `...Disabled` switch and still runs when its hardware is missing.
- **Every speed is its own setting**: `pollenLauncherRpm`, `nectarLauncherRpm`,
  `intakeRpm`, `transferRpm`, `intakeReverseRpm`, `transferReverseRpm`. The D-pad writes
  into those fields, so the dashboard and the robot always agree.
- Test: Motor Tuning: each face button also picks its motor; D-pad up / down changes the
  picked motor's speed only.
- Autos (`autoRunScoring`, on by default): launchers spin and the intake runs from START;
  a LAUNCH step opens both gates for `autoLaunchSeconds`.

**Update 2026-10-31.** The transfer feeds the launch: `Intake` is handed the `LaunchGates`
and runs the transfer forward while they are open, even when the rollers are off. The intake
is left as it was. Square (reverse) still wins, since it is for jams. The operator's left
trigger is a hold-to-run, on top of cross's on / off.

## Consequences

- `launcherStartRpm`, `intakeStartRpm` and `driveBoosted` are gone.
- Opening the gates does not wait for "at speed". Watch `Launchers at speed` on the screen;
  if early shots fall short, make the gates wait (a small change in `LaunchGates`).
- Gamepad 2 is needed for HyDrive now. One-person practice still drives on gamepad 1.
