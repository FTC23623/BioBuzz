# 0052: Launch tuning sessions with Claude over FTC Dashboard

Status: Accepted, 2026-11-06. Built and tested without a robot; the first real session
also checks the link. Practice only.

## Context

The coach wants to tune the launcher, the feed and the aim with Claude changing settings
directly and reading what the robot measures: the speed dips as balls go through, where the
robot stands, the aim angle, with the team only launching and saying made or missed.

FTC Dashboard (0.6.0) is already on the robot at practice. Its page talks to the robot over
a WebSocket at `ws://192.168.43.1:8000`: `GET_CONFIG` / `RECEIVE_CONFIG` for the settings,
`SAVE_CONFIG` with a `configDiff` to change them (the same fields the Drive, Launcher and
Intake groups show, 0022), and `RECEIVE_TELEMETRY` packets with every telemetry line.

## Decision

- **On the robot** (nothing changes in HyDrive or the autos unless these are switched on):
  - `objects/ShotTracker`: a ball is the pollen speed falling more than `shotDipRpm` (100)
    under the target with the gates open; the shot ends once it is back within
    `shotRecoveredRpm` (30), or after `shotTimeoutSeconds` (1.5). Measured on the robot every
    loop, since the dashboard only samples the lines every 100 ms or so.
  - `TestLog` (0050) adds columns: `heading_deg`, `side_deg`, `shots`, `in_shot`, and the
    last shot's `shot_target_rpm`, `shot_dip_rpm`, `shot_recovery_s`, `shot_recovered`,
    `shot_range_in` (averaged distance when the dip started), `shot_mark`.
  - `tuningMarks`: driver cross marks the last shot made (one rumble), triangle missed (two).
  - **Tool: Launch Tuning** (`opMode/LaunchTuningTool`): HyDrive with `testLog`,
    `testTelemetry`, `tuningMarks` and `quietTelemetry` on. People drive and launch.
- **On the laptop** (`tools/tuning/`, Python standard library only):
  - `dash.py`: `status`, `get`, `set Group.name=value`, `record SECONDS`, `shots FILE`,
    `summary FILE`. `set` only changes Launcher, Intake, `Drive.aim*` and `Drive.wiggle*`
    unless `--force`, refuses names that don't exist, reads the value back, and logs every
    change to `tools/tuning/runs/changes.log`. Recordings go to `tools/tuning/runs/`
    (ignored by git).
  - `test_dash.py`: the client against a pretend dashboard on the laptop.
  - `SESSION.md`: the instructions a tuning session runs with: safety rules (never drive or
    launch, small steps, stop on anything odd, nothing into BotProperties unless the coach
    says), how a round goes, what to tune in which order (recovery, speed for distance,
    aim), and a log in `docs/tuning-log/<date>.md`.
  - `tune.sh`: checks the robot answers, then starts an interactive Claude Code session
    with `SESSION.md` added to its instructions and only `dash.py` (and reading files)
    allowed without asking. Interactive, not `claude -p`: a round waits for people to launch.

## CHECK at the first session

1. `python3 tools/tuning/dash.py status` with Tool: Launch Tuning running.
2. `get Launcher`, `set` one harmless value and back: the dashboard page shows it change.
3. `record 20` while launching 3 balls and marking them: `shots` lists 3 shots with marks.
   No shots: lower `shotDipRpm`. Two per ball: raise `shotRecoveredRpm` a little.

## Consequences

- The dashboard is banned in matches (R704.D); this is for the practice field only, and
  `competitionMode` true switches the dashboard side of our code off anyway.
- Tests: `ShotTrackerTest`, `TestLogTest` (a shot and its mark, marks off), `test_dash.py`.
