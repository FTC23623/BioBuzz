# 0036: Launching snaps to the hive's middle and notes the distance; the strip clear is forced

Status: Accepted. Not yet driven. The Prism clear is still a CHECK.
Date: 2026-11-02
Changes 0011 (aim assist aimed at one tag), 0023 (the launch trigger), 0017/0035 (the strip clear).

## Context

The Limelight is configured and seen now (0035). Three more asks from the same session:

1. A TeleOp launch should face the middle of the hive, like `LAUNCH_WHEN_TAG_SEEN` in the
   autos. Aim assist faced the nearest tag, and only on the left bumper.
2. The strip still showed yellow while the Driver Station said "cleared". Two things were
   true: the status text was set only by the clear and the D-pad, never by the underglow or
   pollen draws, so it lied; and whether the Prism blanks a layer on "no animation" is a
   CHECK we have never settled.
3. The coach wants the distance to the tag at the moment of each launch, to match launcher
   speed to distance.

## Decision

1. **Aim at the group middle.** `Drive_Manual` turns toward `Vision.GetGroupBearing()`, the
   middle of the hive's tags, not the nearest tag. **The launch trigger aims too** while it
   is held (`launchTriggerAims`, true), so a launch snaps to the hive; the left bumper still
   aims without launching. The aim is continuous while held, unlike the auto's pick-once.
   The sticks still drive and strafe.
2. **The gates note each launch.** `LaunchGates` takes the camera. The loop the gates are
   told to open (trigger, or an auto's LAUNCH), it remembers the distance to the nearest tag,
   its id and the bearing to the hive's middle, and shows `Last launch (camera): #3: 62 in
   from tag 31, hive middle 1.5 deg to the right` until the next launch. `Last launch range
   (in)` is a number, so the dashboard graphs it. No tag: `#3: no tag in view`. The Robot
   Controller log is not used: unit tests have no Android log.
3. **A clear that clears.** `PrismLedDriver.ClearLayer` now writes brightness 0, then "no
   animation". If the Prism ignores "none", brightness 0 still darkens the layer.
   **D-pad left clears every layer**, whatever the code believes is showing; the underglow
   and the pollen yellow redraw on the next loop. The "Light strip" status line now says
   what was last drawn (underglow, pollen, cleared), not only the last clear.

## CHECK on the robot

1. Hold the left bumper with two hive tags in view: the robot faces between them. Pull the
   launch trigger from off-center: it turns to the hive while the gates open.
2. After one launch, read `Last launch (camera)`. Note the inches beside the launcher RPM
   that scored. That table is the launcher tuning.
3. Ball on the switch, then away: half a second later the yellow goes. If it STAYS yellow
   with the status line saying "pollen gone": press D-pad left. If that clears it, the
   Prism needs the full clear each time and we move it into `ShowPollen`. If even D-pad left
   leaves yellow, the Prism's "none" and brightness 0 both fail and the next try is painting
   black (brightness 0, color 0,0,0, solid) in `ClearLayer`.

## Consequences

- Aim assist with a single tag in view behaves as before: a group of one is that tag.
- A launch with the robot pointed away from the hive turns the robot while the gates open.
  If the drivers prefer the old behavior, `launchTriggerAims = false`.
- `ClearLayer` is two I2C writes, so a full clear is twenty. Still only at INIT, on D-pad
  left, and on pollen transitions.
- Tests: `DriveAimTest` (group middle, trigger aims, switch off), `LaunchGatesTest` (range
  remembered, no tag, one per pull), `LightStripTest` (D-pad left clears all).
