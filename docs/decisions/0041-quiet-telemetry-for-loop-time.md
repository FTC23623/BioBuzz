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
- **`Test: HyDrive Quiet`**: HyDrive with quiet on, nothing else different.

## CHECK on the robot

Run HyDrive, then Test: HyDrive Quiet, each for a minute with the intake and transfer on.
Write down `LoopTime average` for both. A difference over about 5 ms is worth keeping quiet
on in matches, or trimming the lines that cost the most.

## Consequences

- In quiet mode the drive team loses the NO LOCK line and the aim lines. The gamepad buzz
  still says NO LOCK.
- Tests: `VisionLimelightTest` (no status read), `LaunchGatesTest` (nothing printed).
