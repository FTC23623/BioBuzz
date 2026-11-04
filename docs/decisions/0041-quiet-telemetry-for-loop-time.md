# 0041: Quiet telemetry, to measure what the Driver Station lines cost

Status: Accepted. Not yet measured on the robot.
Date: 2026-11-03

## Context

The robot drives less reliably with the intake and transfer running. One suspect is loop
time. Most telemetry is cheap text, but some of it reads hardware just to fill the screen:
the Limelight's status every loop, and the power of four drive motors and two rollers.

## Decision

- **`quietTelemetry`** in BotProperties, false by default. After START every part skips its
  Driver Station lines and the reads made only for them. The screen shows `QUIET TELEMETRY`,
  `LoopTime`, `LoopTime average (ms)` (about the last 50 loops) and `Battery volts`. INIT
  always shows everything: the Bot clears quiet in `ShowStatus()`.
- **A dashboard switch, not an OpMode.** `quietTelemetry` is on the dashboard's Drive group
  and counts on the next loop. Off by default, so HyDrive and every match run with the full
  screen. (A separate Test: HyDrive Quiet OpMode came first and was dropped the same day.)

## CHECK on the robot

Run HyDrive with the intake and transfer on. Read `LoopTime average` for a minute, tick
`quietTelemetry` on the dashboard, read it for another minute, untick it. A difference over about 5 ms is worth keeping quiet
on in matches, or trimming the lines that cost the most.

## Consequences

- In quiet mode the drive team loses the NO LOCK and "turning to the hive" lines, so a launch
  held shut by the camera lock (0040) looks like a broken launch. Only the gamepad buzz is
  left. Measure with quiet on, never drive a match with it.
- Tests: `VisionLimelightTest` (no status read), `LaunchGatesTest` (nothing printed).
