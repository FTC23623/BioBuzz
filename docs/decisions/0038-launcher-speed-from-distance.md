# 0038: The pollen launcher's speed follows the distance to the tag

Status: Accepted. Since 2026-11-04 a curve in 2-inch steps, 2750 at 110 in to 3225 at 140 in, curve 1.25 (see the updates at the end). Two rows measured; the two between are guesses. Not yet shot.
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

## Update 2026-11-03: a gentle curve in 2-inch steps

The four 10-inch rows are gone. The speed is now a curve through the two measured shots,
2900 RPM at 110 in and 3325 at 140 in (3300 plus 25), stepped every `launchBandIn` (2) inches:

```
rpm = near + (far - near) x ((step start - 110) / 30) ^ launchCurve
```

The ball flies in an arc, so each step out needs a little more than the last.
`launchCurve` is 1.1, a subtle bend, because the old 120 and 130 guesses already sat close
to a straight line. 1 is straight; 2 would be a parabola. All six numbers
(`launchNearIn`, `launchNearRpm`, `launchFarIn`, `launchFarRpm`, `launchCurve`,
`launchBandIn`) are in BotProperties; the RPMs, the curve and the step are on the
dashboard's Launcher group. `Launcher.RpmForRange` is still the one place that reads them.

| in | 110 | 112 | 116 | 120 | 124 | 130 | 134 | 138 | 140+ |
|---|---|---|---|---|---|---|---|---|---|
| RPM | 2900 | 2922 | 2972 | 3027 | 3084 | 3172 | 3232 | 3294 | 3325 |

Tuning: a miss near the hive moves `launchNearRpm`, a miss far out moves `launchFarRpm`, and
misses in the middle with both ends right move `launchCurve` (up if the middle falls short).

## Update 2026-11-04: inertia wheel, near shot 150 lower

An inertia wheel went on the pollen launcher. The near shot came down 150 RPM, to
`launchNearRpm` 2750; the far shot stays `launchFarRpm` 3325. The middle had come down
too, so `launchCurve` went from 1.1 to 1.25 to keep it low (coach).

| in | 110 | 112 | 116 | 120 | 124 | 130 | 134 | 138 | 140+ |
|---|---|---|---|---|---|---|---|---|---|
| RPM | 2750 | 2769 | 2827 | 2896 | 2972 | 3096 | 3185 | 3277 | 3325 |

`autoLaunchMinRpm` came down by the same 150, 2950 to 2800, so an auto's near launch
does not sit out its spin-up wait (docs/decisions/0030).

**Later on 2026-11-04:** the peak came down 100, `launchFarRpm` 3325 to 3225 (coach).

| in | 110 | 112 | 116 | 120 | 124 | 130 | 134 | 138 | 140+ |
|---|---|---|---|---|---|---|---|---|---|
| RPM | 2750 | 2766 | 2814 | 2870 | 2933 | 3036 | 3109 | 3186 | 3225 |
