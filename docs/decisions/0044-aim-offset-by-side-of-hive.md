# 0044: The aim offset flips with the side of the hive the robot stands on

Status: Accepted, 2026-11-04. Not yet driven. The side's sign (`aimSideReversed`) and
the dead band (`aimSideCenterDeg`) are CHECK.

## Context

Since 2026-11-03 every aim (aim assist, the launch lock, the autos' aim) held the hive's
middle `aimHiveBearingDeg` left of center, which turned the robot that much further
right, for the pollen launcher sitting left of the camera. It worked from the left side of
the hive. From the right side the coach found it needs the opposite: turn further LEFT.

## Decision

The offset's direction follows which side of the hive the robot stands:

| Robot stands | Offset | Turns |
|---|---|---|
| left of the hive, more than `aimSideCenterDeg` (5) | `+aimHiveBearingDeg` | that much further right |
| right of the hive | `-aimHiveBearingDeg` | that much further left |
| within 5 deg of straight in front, or side not known | 0 | at the hive's middle |

- **Which side:** the Limelight's camera pose in the nearest tag's space
  (`getCameraPoseTargetSpace`). `Vision_Limelight.SideDegrees` turns its X (right as
  you face the tag) and Z (out of the tag) into degrees around the tag, left positive.
  The Driver Station shows it as "Robot stands ... deg to the left/right of the hive".
- **One rule, one place:** `AimAssist.SideOffset(side, aimHiveBearingDeg,
  aimSideCenterDeg)` gives where the hive should sit; `AimError` is unchanged. Drive_Manual,
  LaunchGates and `PedroAuto.AimHeading` all use it.
- **The dead band** stops the aim jumping 2 x the offset when the robot stands right in
  front and the side reading wobbles across 0.
- The webcam and no-camera Visions return side 0, so they aim at the middle.

## CHECK on the robot

1. Stand clearly LEFT of the hive, camera on it. The Driver Station must say "to the left".
   If it says right, tick `aimSideReversed` on the dashboard's Drive group (and write it
   into BotProperties).
2. Shoot from both sides and from in front. A miss toward the hive's middle from both sides
   means `aimHiveBearingDeg` is too big; a miss away from it, too small.

## Consequences

- From the left the robot aims as before. From the right it now aims the other way.
- Right in front, there is no offset. Before, it was always the full offset to the right.
- Tests: `AimAssistTest` (the flip and the band), `VisionLimelightTest` (the side's sign),
  `DriveAimTest`, `LaunchGatesTest` and `LaunchSpinUpTest` (both sides).
