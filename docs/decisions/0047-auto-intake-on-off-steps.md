# 0047: INTAKE_ON and INTAKE_OFF auto steps, recorded with D-pad up / down

Status: Accepted, 2026-11-05. Not yet driven.

## Context

Since 0043 the intake roller is off in the autos except during a FLOWER step, so the robot
doesn't pick up extra balls while driving. The coach now wants to pick balls up on purpose
along a route: turn the intake on at one spot and off again at another, and record that
with Tool: Pose Recorder like LAUNCH and FLOWER.

## Decision

- **Two new steps in `PedroAuto`**, markers like `LAUNCH` and `FLOWER`:
  - `INTAKE_ON`: the intake roller and the transfer run from here on.
  - `INTAKE_OFF`: the roller stops. The transfer goes back to what the auto does
    (`autoRunScoring`, on today), so LAUNCH steps still feed.
- **They take no time**: the next path starts on the next loop. Put them after the path to
  the spot where the intake should change:

  ```java
  paths.add(toSpot3);
  paths.add(INTAKE_ON);
  paths.add(toSpot4);     // drives through the balls with the intake on
  paths.add(INTAKE_OFF);
  ```

- **FLOWER keeps its own intake**: it runs the roller during the flower, then puts it back
  to the last INTAKE_ON / INTAKE_OFF (before, it always went back to `autoIntakeRoller`).
- **Tool: Pose Recorder**: **D-pad up** saves the spot and adds `paths.add(INTAKE_ON);`
  after it; **D-pad down** saves the spot and adds `paths.add(INTAKE_OFF);`. The Driver
  Station line ends `, INTAKE_ON` or `, INTAKE_OFF`. Square still forgets the last spot.
  D-pad up and down did nothing on gamepad 1 before.

## CHECK on the robot

1. Record a short route: cross, D-pad up, cross, D-pad down. The file should have the two
   steps after the spots.
2. Run it: the roller should start at the D-pad-up spot and stop at the D-pad-down spot.
   The transfer runs the whole way.

## Consequences

- Each intake step makes the robot stop at its spot for a moment, like any path end.
- Tests: `PoseRecorderTest` (the buttons, the Java, the Driver Station line),
  `FlowerStepTest` (the markers are all different), `RecordedAutosTest` (paths still
  connect across intake steps). The auto loop itself needs a robot to test, like FLOWER.
