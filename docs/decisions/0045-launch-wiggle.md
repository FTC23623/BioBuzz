# 0045: Launch wiggle, square with the launch trigger

Status: Accepted, 2026-11-04, as an experiment. Not yet driven. Every number is CHECK.

## Context

Balls fed one after another sometimes run into each other on the way out. The coach
measured the launcher taking a ball every 10 frames of a 60 fps video: one every 1/6 s.
The idea to try: swing the robot a little left, then a little right, so each ball leaves
on the other side from the one before.

## Decision

- **Button:** the driver holds **square** while holding the **launch trigger**. Square
  alone does nothing, so it can't start by accident. Square was free on gamepad 1.
- **The swing:** `wiggleDeg` (1) left of the heading the wiggle started at, then 1 right,
  switching every `wiggleSwitchSeconds` (10 / 60 = 0.167 s, a side per ball). Left first.
- **Heading from the IMU (Pinpoint), not the camera:** the camera's picture is a few
  loops old, too slow for a swing that changes every 1/6 s. No IMU heading, no wiggle.
- **The turn:** `wiggleKP` (0.1 power per degree), never past `wiggleMaxRotate` (0.3).
  Switching sides is 2 degrees to go: 0.2 power. While wiggling, the wiggle's turn replaces
  aim's, so aim first, then hold square. Let go of square and aim takes the turn back.
- The math is in `objects/Wiggle.java` (`Side`, `Turn`), tested on its own.

## CHECK on the robot

1. Hold the launch trigger and square with no balls. The "Wiggle" telemetry line shows
   the side and how far off the start the robot is. It should reach about +1 and -1.
   Not moving that far: raise `wiggleKP` or `wiggleMaxRotate`. Overshooting: lower them.
2. Then with balls. If they still collide, try `wiggleDeg` 2. If the switch falls between
   balls badly, adjust `wiggleSwitchSeconds` (it should match the ball rate).
3. With `launchNeedsLock` on, the lock is checked every loop and a 1-degree swing can
   leave the 1-degree lock window and close the gates. The lock is off by default.

All four numbers are on the dashboard's Drive group.

## Consequences

- TeleOp only. The autos don't wiggle.
- Tests: `WiggleTest` (sides, the turn, across 180 / -180), `DriveAimTest` (square with
  the trigger, square alone, no IMU).

## Update, 2026-11-05: a toggle, on by default

Coach: the wiggle is now a setting, `launchWiggle`, **on at START**. Square on gamepad 1
turns it off and on again (one rumble on, two off); the dashboard's Drive group has it
too. With it on, every launch wiggles: hold the launch trigger and nothing else.

Because the trigger also aims (`launchTriggerAims`), the wiggle waits until aim has
settled: the hive within `aimDoneDegrees` (1) of where it should sit, or no tag in view.
Then it swings around that heading until the trigger is let go; a new camera reading does
not take the turn back. So a launch still snaps to the hive first, then wiggles.
Tests: `DriveAimTest` (the trigger alone wiggles, aim first, square toggles, on by default).
