# 0018: Driver practice options come over from Decode as BotProperties settings

Status: Accepted. Not yet driven on last season's robot. Drift moved to the left trigger by 0032.
Date: 2026-10-29

## Context

The coach added driver practice to Decode (uncommitted there): `DrivePractice` and three
leaf TeleOps, plus three options in Decode's `Drive_Manual`: swapped sticks, tight turns
and drift assist. The team wants to practice with this season's code on last season's
robot. BioBuzz's drive was already ported from Decode (0002), and its motor names, motor
directions and hub IMU mounting are still Decode's, so the Bot runs on that robot as is.
The lights, camera and Safe Mode key all cope with missing hardware.

## Decision

- **Each option is a `BotProperties` setting**, off by default: `driveSwapStrafeRotate`,
  `driveTightTurns`, `driveDriftAssist`, and their gains. HyDrive drives the same as before.
- **The math lives in `MecanumMath`** (`DriftDrive`, `DriftStrafe`, and a `FieldCentric`
  that takes extra robot-centric drive and strafe), so it is tested on a laptop.
- **Practice TeleOps copy HyDrive** and change settings in one method,
  `ChooseDriveOptions`, before the Bot is built: **Practice: Field Centric**, **Practice:
  Robot Centric**, **Practice: Robot Centric Swapped**. They turn on the same options as
  Decode's three. HyDrive stays the one match TeleOp.
- **Kept from Decode on purpose:** tight turns do nothing while swapped. The swapped turn
  gain (0.5) is used instead.

## Changed from Decode

- **Field-centric drift scaling.** Decode scales by the turned values plus the drift; here
  the drift is added to the scaling total, the same way the sticks are. The result can be
  a little gentler than Decode while drifting. The 1.41 issue from 0002 is unchanged.
- **Decode's left bumper aim is still overwritten** by its square snap code on the next
  line. BioBuzz fixed that in 0011; nothing to port.

## Consequences

- **The right bumper is the launcher's "faster" button.** Drift assist uses it too, so
  never turn both on in one OpMode. Practice TeleOps leave the launcher off, like HyDrive.
- **The drift gains assume a square robot.** On a long robot the pivot lands a bit inside
  or outside the wheel. Tune `driveDriftSideGain` and `driveDriftRearGain` on the field.
