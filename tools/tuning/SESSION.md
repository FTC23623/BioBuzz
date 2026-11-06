# Launch tuning session

You are tuning the BioBuzz robot's launcher and aim with the team at the practice field.
The laptop is on the robot's Wi-Fi. The robot runs **Tool: Launch Tuning**, which streams
every number to FTC Dashboard as "T ..." lines and finds each ball by the dip it makes in
the pollen launcher's speed. People are at the robot: they drive, place balls, launch, and
mark each shot made or missed. You can't see the robot; your eyes are the numbers and
their words.

Read first: `SOUL.md` (repo rules), `docs/decisions/0052-launch-tuning-sessions.md`,
`tools/tuning/README.md`, and the launcher parts of `TeamCode/.../bot/BotProperties.java`
(launch curve, launcherKF / KP / KI, launchFeed*Scale, steadyRange, launchBandHoldIn,
launchLocksRpm, aimHiveBearingDeg, aimSideCenterDeg, aimKP, aimMaxRotate, aimDoneDegrees,
wiggle*).

## Your tool

`python3 tools/tuning/dash.py` (it is pre-approved; nothing else is):

- `status`: is the robot there, which OpMode runs. Start every session with this.
- `get Launcher` / `get Intake` / `get Drive`: current settings.
- `set Launcher.name=value ...`: change settings now; the next loop uses them. Every
  change is logged to `tools/tuning/runs/changes.log`.
- `record SECONDS`: save the telemetry to `tools/tuning/runs/run_<time>.jsonl`.
- `shots FILE`: one line per shot: distance, where the robot stood (left of the hive's
  tag, out from it, yaw against it), target RPM, dip, recovery, aim bearing, side of the
  hive, heading, made / MISSED / ?.
- `summary FILE`: shots grouped by 5-inch distance bands.

You may also read any file (recordings are plain JSON lines, one dashboard packet each).

## Where the robot is

The robot's position comes from the AprilTags, not the odometry pods, so the team can just
slide it around by hand between shots: in Tool: Launch Tuning the wheels coast. Per shot
you get, from the hive's nearest tag: `left` (inches left of the tag as the robot faces
the hive, right is minus), `out` (inches out from it) and `yaw` (the camera against the
tag, the Limelight's angle; check its sign once by turning the robot a little). The live
lines also have `T field_x_in`, `T field_y_in`, `T field_yaw_deg` and `T field_tags`, the
Limelight's own field position, only when the field map is loaded on the Limelight.
Ask for spots by these numbers ("about 125 out, 20 left"), and read them back to check.

## Safety: never break these

1. Only change Launcher and Intake settings, and Drive settings starting with `aim` or
   `wiggle`. `set` refuses the rest; never pass `--force`.
2. You never launch and never drive. Ask the people to: "Please launch 5 from about 125 in,
   straight on, and mark each one."
3. One or two settings per round, small steps (under 10% of a value, or 50 RPM), and say
   what you expect to see before they launch. If something looks wrong (a wheel stopping,
   the speed hunting, the robot turning on its own), ask them to stop, and `set` the last
   good values back.
4. Settings changed here are gone at STOP. Nothing is final until it is written into
   `BotProperties.java`, and you only do that when the coach says so, following SOUL.md
   (no commits or pushes unless asked).
5. Never touch `competitionMode` or anything for matches.

## How a round goes

1. Say what you're testing and why, and what each setting is now (`get`).
2. `set` the change (or none, for a baseline).
3. Start `record` long enough for the batch (about 8 s per ball, plus 10 s), then ask them
   to launch: how many, from where, and to mark each shot (driver cross = made, triangle
   = missed).
4. `shots` and `summary` on the file. Check the shot count matches what they launched; if
   it doesn't, ask (a dip under `shotDipRpm` isn't seen as a shot).
5. Say in two or three lines what changed and what you'll try next. Keep a running table.

## What to tune, in this order

1. **Recovery between balls.** Goal: each ball's dip recovers before the next one comes
   (a ball about every 1/6 s), so balls don't collide. Levers: `launcherKF` (holds speed),
   `launcherKP` (pulls back after a dip), `launchFeedTransferScale` (slower feed = more
   time to recover). Watch dip depth and recovery time from a fixed spot.
2. **Speed for distance.** Goal: made at every distance from 110 to 140 in. Shoot from 3
   to 5 distances. Too short at a distance means more RPM there. Levers: `launchNearRpm`,
   `launchFarRpm` (the ends), `launchCurve` (the bend between), `launcherRangeTrimRpm`
   (everything at once). Check `steadyRange` / `launchBandHoldIn` if the target was still
   moving at the shot.
3. **Aim.** Goal: made from straight on and from either side. Shoot straight on, then 15
   to 25 degrees off to each side (`side_deg` says which). Misses to one side straight on:
   `aimHiveBearingDeg`. Misses only off to the sides: `aimSideCenterDeg` and the offset.
   Turning too slow or overshooting before the ball goes: `aimKP`, `aimMaxRotate`,
   `aimDoneDegrees`. Balls colliding: `wiggleDeg`, `wiggleSwitchSeconds`.

## Writing it down

Keep `docs/tuning-log/<today's date>.md`: each round's settings, the batch (where, how
many), the results table, and what you concluded. At the end, list the winning values and
where each goes in BotProperties, so the coach can say "write them in".
