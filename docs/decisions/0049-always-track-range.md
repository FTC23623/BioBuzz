# 0049: Always-track range, once the hive has tipped

Status: Experiment, 2026-11-05. Off by default. The tilt numbers are CHECK.

## Context

In TeleOp the pollen speed only follows the distance while the driver holds aim or launch
(coach, 2026-11-03). Each aim then starts with the wheel ramping up or down to the new
speed. The coach wants a mode that follows the distance whenever our tags are seen, so
the wheel is already there. But the hive's tags can be seen before its basket tips up,
and a speed for a hive that can't be scored yet is wasted, so it should only follow once
the tags read as tipped.

## Decision

- **`rangeAlwaysTracks`** (false): the switch. **Gamepad 2 circle** flips it (one rumble
  on, two off); the dashboard's Launcher group has it and the numbers below. With it on,
  TeleOp follows the distance with no aim button down whenever a tag of ours is seen and
  tipped. While aiming, it follows as before, tipped or not. Autos are unchanged.
- **Tipped** (`rangeNeedsTipped`, true): the nearest tag's tilt in the Limelight's camera
  space, its pitch (or roll, with `tagTiltFromRoll`), is from `tagTippedMinDeg` (-20) to
  `tagTippedMaxDeg` (20). An unknown tilt (no pose, or the webcam) never counts as tipped.
  `rangeNeedsTipped` false follows any tag of ours.
- The Limelight shows a "Tag tilt" line: pitch, roll and yaw of the nearest tag, and the
  window. "Launcher speed from distance" starts with `ALWAYS` when this mode set it, or
  says why it didn't: no tag, or `tag not tipped, tilt N`.
- Steady range (0048) and the trim work the same on top.

## CHECK on the field

1. Turn it on (gamepad 2 circle). Point the robot at the hive with the basket down, then
   tip it up by hand. Watch "Tag tilt": which of pitch and roll changes? If it is roll,
   set `tagTiltFromRoll` true.
2. Note the tilt with the basket down and tipped. Set `tagTippedMinDeg` / `MaxDeg` so the
   tipped reading is inside and the down reading is outside, with some room.
3. With the basket tipped and no button down, the RPM line should say `ALWAYS` and follow
   the distance; with it down, `tag not tipped`.

## Consequences

- With it on, the speed changes while driving around, not just while aiming. The wheel
  works a little more; aiming starts at the right speed.
- The guessed window (-20 to 20) assumes a tipped tag faces the camera square. Until it is
  checked, leave `rangeNeedsTipped` on and `rangeAlwaysTracks` off for matches.
- Tests: `LauncherTest` (the window, off holds, tipped follows, untipped and unknown hold,
  no tilt check, circle toggles), `VisionLimelightTest` (pitch, roll, no pose).
