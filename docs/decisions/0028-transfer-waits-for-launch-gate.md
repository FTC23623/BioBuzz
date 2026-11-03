# 0028: On the launch trigger, the transfer waits for the gates to swing open

Status: Accepted. 0034: a transfer that was already running now STOPS while the gate opens, instead of only waiting when off. Not yet tried on the robot. Servo model and speed are CHECK.
Date: 2026-10-31
Adds to 0023 (the transfer feeds the launch).

## Context

Since 0023's update, holding the driver's right trigger opens both launch gates and runs the
transfer at the same moment. The transfer could push a ball into a gate that is still
swinging open. A servo can't report where it is, so we can't ask it "are you open yet?".

## Decision

- **Work the time out from the servo's numbers** (`LaunchGates.OpenSeconds`):

  ```
  degrees it turns = |open - closed| x launchServoRangeDeg
  seconds = degrees / 60 x launchServoSecondsPer60Deg + launchServoExtraSeconds
  ```

  The slower of the two gates counts. Today: 0.267 x 300 = 80 degrees, 80 / 60 x 0.11 = 0.15 s,
  plus 0.05 = **0.20 s**. (First set to 0.25 s per 60, a 0.38 s wait; the coach asked for
  about 0.2 s on 2026-10-31.)
- **The clock starts** the loop the open position is first sent, and starts over on every
  new pull of the trigger.
- **`LaunchGates.IsFullyOpen()`** is true once that time has passed. `Intake` waits for it
  before running the transfer for a launch. (Since 2026-10-31 the intake runs with it, to
  push along any ball stuck lower down.) If the operator already has the rollers on, they
  keep running.
- **The speed setting:** goBILDA lists the 2000 series at 7.4 V: 25-2 Torque 0.17 s/60°,
  25-3 Speed 0.07, 25-4 Super Speed 0.035. The hub's servo ports give about 5 V, so a servo
  is slower there. `launchServoSecondsPer60Deg` is 0.11, for a 0.2 s wait.
  Both timing settings are on the dashboard (Launcher group).

## CHECK on the robot

1. Which 2000 series model are the launch servos? For a 25-3 try 0.10, for a 25-4 try 0.05.
2. Hold the trigger and watch: the gate should be fully open just before the transfer
   starts. The Driver Station shows "opening, 0.12 of 0.38 s" during the wait. Too early:
   raise the extra seconds. A long pause: lower the seconds per 60.

## Consequences

- Every launch starts about 0.2 s later than before. In an auto, that comes out of the
  `autoLaunchSeconds` stop.
- A change to the closed or open spots (Test: Launch Servo Tuning) changes the wait by
  itself. Nothing else to update.
