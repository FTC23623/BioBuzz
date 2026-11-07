# 0050: Roller power mode, launch RPM lock, test log, match-day settings

Status: Accepted, 2026-11-06. Everything here is off by default. Not yet driven.

## Context

Four asks from the coach before Hartland:

1. Try running the intake and transfer by plain power instead of the hub's speed control.
2. Stop the pollen speed jumping during a launch.
3. For testing, record the buttons pressed (launch, aim, ...), the Limelight's distance,
   and the transfer's RPM and power.
4. One clear list of what to switch off for loop time and for the match rules.

## Decision

- **`rollerUsePower`** (false): the intake and transfer get `setPower` instead of
  `setVelocity`. The power is the RPM setting as a share of `intakeMaxRpm` (1150), so the
  same settings work in both modes: 1150 is full power, 575 half, the 400 reverse 0.35.
  The motor switches between RUN_USING_ENCODER and RUN_WITHOUT_ENCODER when the setting
  changes, so it can be flipped on the dashboard's Intake group. The encoder still counts,
  so the actual RPM still shows. `Roller.PowerForRpm`.
- **`launchLocksRpm`** (false): while the launch gates are open, `Launcher.FollowDistance`
  holds the target it had when they opened ("LOCKED for the launch" on the screen). It
  follows again once they close. TeleOp and the autos. The Bot hands the Launcher the
  gates (`Launcher.SetLaunchGates`).
- **`testLog`** (false): `subsystems/TestLog`, last in the Bot's list, writes one CSV line
  per loop to `/sdcard/FIRST/logs/TestLog_<date>_<time>.csv`. Columns: time, loop time;
  every button by what it does (`aim`, `launch`, `slow`, `drift`, `arc_left`, ... for the
  driver; `intake_toggle`, `reverse`, `trim_up`, ... for the operator), 1 held / 0 not;
  the switches (wiggle, steady, always-track, gates open); the tag, nearest and group
  distance, tilt, bearing; pollen and nectar target and actual RPM; intake and transfer
  target and actual RPM and power; power mode. It flushes every 10 lines, so STOP loses
  at most a few loops. Copy the file off the hub (Android file transfer, or `adb pull`)
  and open it in a spreadsheet.
- **`testTelemetry`** (false): the same numbers on the Driver Station, one line each, named
  like the file's columns with a "T " in front (`T launch: 1`), even with quiet or
  launcher-only telemetry on. Every value is a plain number, buttons 0 or 1, so FTC
  Dashboard's graph view can plot each one over time. An unknown value (no tilt) is left
  out, since the dashboard can't draw it. (Changed the same day from a few combined lines
  that listed the buttons by name, coach.)
- **`docs/match-day-settings.md`**: A, loop-time settings in order; B, the match-rule
  settings; linked from the top of `event-checklist.md`.

## CHECK on the robot

1. `rollerUsePower` on: the rollers should run about as fast as before; the "Transfer
   power" line says POWER MODE. If too slow under load, raise `transferRpm`/`intakeRpm`
   (still out of 1150) or go back to speed mode.
2. `launchLocksRpm` on: pull the trigger while driving in or out; "Launcher speed from
   distance" should say LOCKED and the target hold until you let go.
3. `testLog` on: after a run, `/sdcard/FIRST/logs/` has a new CSV with a header row.

## Consequences

- With everything off, nothing changes: the default behaviour is today's.
- Tests: `RollerTest` (speed mode, power mode, switching back, the power share),
  `LauncherTest` (the lock holds while open, off keeps following), `TestLogTest` (off does
  nothing, the file's columns and values, the screen lines, number format).
