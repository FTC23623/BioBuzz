# 0058: Auto speed-up steps and telemetry groups

Status: Accepted. Not yet tried on the robot.
Adds to 0042 (FLOWER), 0047 (INTAKE_ON/OFF), 0056 (settle) and 0041 (quiet telemetry).

## Context

The autos are repeatable (coach, 2026-11-27), so the next gain is time. The Driver Station
was also noisy: every part printed all it knew, with no way to pick.

## Decision

New steps for `BuildPaths()` (all in PedroAuto):

- `FLOWER_OUT` / `FLOWER_IN`: swing the extractor and carry on at once, so it is open on the
  drive to the flower. A `FLOWER` after `FLOWER_OUT` skips its swing wait ("arm already out").
- `LAUNCH_NECTAR`: nectar wheel to `autoNectarLaunchMinRpm` (2785), settle if
  `autoLaunchSettles`, open only the nectar gate for `autoLaunchSeconds`.
- `LAUNCH_UNTIL_EMPTY`: gates open until every ball switch (pollenSensor1-4,
  nectorSensor1-4) has read empty `autoEmptyClearSeconds` (0.3 s), never past
  `autoLaunchUntilEmptyMaxSeconds` (4 s). No switch found: `autoLaunchSeconds`.
  Ball switches: `BallSensors`; `ballSensorsInverted` flips them (CHECK with
  Test: Pollen and Nectar Sensors).

Telemetry: the auto shows "Step n of m: <what>", "Next step", and how far a drive has left.
Eight groups (`TelemetryGroup`, `tlm...` in BotProperties) switch on the dashboard's
Telemetry group: auto step, auto launch, pose, launcher, vision, drive, intake, lights.
Quiet settings still hush everything in TeleOp; INIT always shows all.
The auto launch group also shows the hive's bearing off centre during a launch.

## Check on the robot

Ball switch polarity; the nectar floor; empty time (0.3 s) not ending early on a bouncing
ball; the arm fully out by the end of the drive after `FLOWER_OUT`.
