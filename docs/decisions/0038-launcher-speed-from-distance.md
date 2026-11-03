# 0038: The pollen launcher's speed follows the distance to the tag

Status: Accepted. Two rows measured; the two between are guesses. Not yet shot.
Date: 2026-11-02
Adds to 0009 (speed control) and 0023 (the D-pad sets the speed).

## Context

A farther shot needs a faster wheel. The coach measured 2900 RPM scoring from 110 in and
3300 from past 140 in, and wants the speed set in 10-inch steps from the camera's distance
to the tag, so the drivers are not guessing speeds mid-match. The camera already gives the
range (0011), and every launch now notes it (0036), so the table can be filled in from
matches.

## Decision

- **`launchRpmByRangeIn`** in BotProperties: rows of {inches, RPM}, 10 inches apart:
  110 → 2900, 120 → 3030, 130 → 3170, 140 → 3300. The 120 and 130 rows are a straight
  line between the two measured ends. **The band rule**: the last row whose distance is not
  more than the range. 115 in is the 110 row, 139 in the 130 row, past 140 the 140 row,
  nearer than 110 the 110 row. `Launcher.RpmForRange` is the one place that reads it.
- **Continuous, not at the trigger.** While a tag of ours is in view, every loop sets the
  pollen target from the table, so the wheel is already at the right speed when the gates
  open. With no tag the target holds its last value, so a blind shot still has a speed.
- **`launcherRangeTrimRpm`** (0) is added to every row. **D-pad up / down trims** instead of
  setting the speed while range tuning is on, so the operator can still nudge every shot
  faster or slower on the day. On the dashboard's Launcher group.
- **`launcherRangeTuning = false`** turns it off: D-pad and dashboard set the speed as before.
  Test: Launcher sets it false, since it tests fixed speeds.
- Pollen only. Nectar keeps its own fixed target.

## Filling the table in

After a match, read `Last launch (camera)` (0036) and whether the shot scored. A miss at a
distance means that row's RPM is off: change it and shoot again from there. The trim is for
the day (battery, ball wear); a trim that stays the same for a whole event belongs in the
rows.

## Consequences

- With the camera on the hive from far away, the wheel sits at 3300 while the robot drives
  in. It spends more battery and is louder; it is also ready to shoot at any moment.
- A tag seen but with no distance (the Limelight could not place it) holds the last speed.
- Tests: `LauncherTest` (the band rule, follows the table, trim, no tag holds, off).
