# Launch tuning with Claude

A practice-field session where Claude changes the launcher and aim settings live, reads
what the robot measured, and asks you to launch and mark shots. See
`docs/decisions/0052-launch-tuning-sessions.md`.

## Before

- The robot: the normal build, with Tool: Launch Tuning on it (`./gradlew :TeamCode:installDebug`,
  or Android Studio). The match build (`-PmatchBuild`) leaves it out.
- The Robot Controller app's menu: **Enable Dashboard**. `competitionMode` must be `false`.
- The laptop: on the robot's Wi-Fi, with this repo, Python 3 and Claude Code (`claude`).
- Balls, a hive, and two people: one drives and launches, one places balls.

## Start

1. Driver Station: run **Tool: Launch Tuning** (INIT, START).
2. Laptop, in the repo: `tools/tuning/tune.sh`.
   It checks the robot answers, then opens Claude Code with the session's instructions
   (`tools/tuning/SESSION.md`). Only `tools/tuning/dash.py` runs without asking.
3. Do what Claude asks: launch N balls from a spot, and mark each one on the driver's
   gamepad: **cross = it went in, triangle = it missed** (one rumble / two).

## During

- Claude changes one or two settings at a time and says what it expects. Say stop any time.
- Changes are live and gone at STOP; nothing is saved to the code unless you ask.
- Recordings and a log of every change are in `tools/tuning/runs/` (not committed).
- Claude keeps notes in `docs/tuning-log/<date>.md`.

## After

Ask Claude to write the winning values into `BotProperties.java`. Turn the dashboard off
again before an event (`docs/match-day-settings.md`).

## By hand

`python3 tools/tuning/dash.py --help`: `status`, `get`, `set`, `record`, `shots`, `summary`.
Tests (no robot needed): `python3 tools/tuning/test_dash.py`.
