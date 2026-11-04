# 0040: The launch needs a camera lock; aim and tracking only while a button is down; an extra ball is spat out

Status: Accepted. Not yet driven. Reject timing and the switch's position are CHECK. The launch lock is switched OFF by default since later on 2026-11-03 (`launchNeedsLock = false`, on the dashboard's Launcher group). Reject timing changed the same evening: pollen must be detected 0.5 s (`pollenRejectAfterSeconds`), and the intake reverses 0.75 s (`pollenRejectSeconds`). Raised to 1 s on 2026-11-04 so a ball passing the switch is not mistaken for a full robot.
Date: 2026-11-03
Changes 0036 (the launch trigger opened the gates whatever the camera saw), 0037 (one feed share for both rollers), 0038 (the launcher followed the camera all the time) and 0039 (slow mode at a quarter).

## Context

From the field on 2026-11-03:

1. A launch with the robot pointed away from the hive is a wasted ball. The driver should be
   stopped and told. A camera that dies mid-match must not stop us shooting.
2. At half feed speed the balls ran into each other at the launcher.
3. Aim assist looked like it ran with nothing pressed. Camera tracking should only happen
   while a button is down.
4. Too many balls is a penalty. The pollen switch only sees a ball once the robot is full.
5. Slow mode at a quarter was too slow.

## Decision

1. **Camera lock** (`LaunchGates`, `launchNeedsLock`). The driver's trigger opens the gates
   only while a tag of ours is in view and the hive's middle is within `aimDoneDegrees`.
   Off-center, the trigger turns the robot (0036) and the gates open once it faces the hive.
   No tag: the gates stay shut and gamepad 1 buzzes once per pull (`launchNoLockRumbleMs`).
   **The operator's right trigger, held, overrides the lock.** An auto's LAUNCH is not checked.
2. **Feed shares.** `launchFeedIntakeScale` 0.5, `launchFeedTransferScale` 0.3.
3. **Aim only on a button.** `Drive_Manual` reads the buttons again before it acts, and a loop
   with no fresh gamepad reading neither aims nor drives anything left over. The Driver Station
   shows `Aim assist: off` and the raw `Aim buttons` values. In TeleOp the launcher follows
   the distance table only while the driver holds aim or launch, and holds its speed
   otherwise. Autos keep following: they have no buttons.
4. **Auto-reject** (`Intake`, `pollenAutoReject`). While the operator is intaking, pollen
   detected for `pollenRejectAfterSeconds` (0.2) starts a reject: intake backward, transfer
   forward, for `pollenRejectSeconds` (0.5). Then the operator's intake and transfer stop
   until the left bumper and left trigger are both let go. "Detected" uses the switch's
   half-second hold (0034), so the holes in a ball don't restart the count. Launching and
   square are never interrupted. In an auto the intake goes off after a reject.
5. **`driveSlow = 0.5`.**

## CHECK on the robot

1. Robot facing away from the hive, pull the launch trigger: gates stay shut, gamepad 1 buzzes,
   the screen says `NO LOCK`. Gamepad 2 holds R2 and gamepad 1 pulls again: it launches.
2. Hive in view off to the side, pull the trigger: the robot turns, then launches.
3. Nothing pressed, hive in view: the robot does not turn, the screen says `Aim assist: off`,
   and the launcher line says `not tracking`. If the robot still turns, read `Aim buttons`:
   a launch trigger above 0.05 with nothing pressed is a worn trigger or a bad gamepad.
4. Fill the robot and keep intaking: about 0.2 s after the fourth ball reaches the switch the
   intake spits one out, then both rollers stop until gamepad 2 lets go.
5. Launch a full load: balls go in one at a time at 30% transfer.

## Consequences

- A blind launch in TeleOp needs two people. That is the point.
- The pollen wheel no longer pre-spins to the right speed while driving in; it changes when
  aim is pressed. The table's bands are about 130 RPM apart, which the wheel covers quickly.
- The reject depends on where the switch sits. If it sees the third ball, raise
  `pollenRejectAfterSeconds` or move the switch.
- Tests: `LaunchGatesTest` (lock, buzz, override, 9), `DriveAimTest` (stale input, 3),
  `LauncherTest` (tracking only while aiming, 2), `IntakeTest` (reject, 5).
