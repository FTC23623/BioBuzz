# 0054: Position from the AprilTags for tuning, so the robot can be slid by hand

Status: Accepted, 2026-11-06. The yaw sign and the field position are CHECK.

## Context

Launch tuning sessions (0052) need to know where the robot was for each shot. The coach:
work it out from the hive's AprilTags, not the odometry pods, so the team can just slide
the robot around between shots.

## Decision

- **`objects/TagPosition`**, from `Vision.GetPosition()` (Limelight only; the webcam and
  no camera give "not known"):
  - From the hive, the nearest tag of ours: `hiveLeftIn` (inches left of the tag as the
    robot faces the hive, right is minus, same sign as `side_deg` and `aimSideReversed`),
    `hiveOutIn` (out from the tag), `hiveYawDeg` (the camera against the tag, the Limelight's
    yaw). From the tag's camera pose in target space (`getCameraPoseTargetSpace`).
  - On the field: the Limelight's MegaTag 1 pose (`getBotpose`), inches from the field's
    middle in the Limelight's axes, with how many tags it used. Only when the field map is
    loaded on the Limelight; otherwise "not known".
- **Test log / test telemetry** (0050): `hive_left_in`, `hive_out_in`, `hive_yaw_deg`,
  `field_x_in`, `field_y_in`, `field_yaw_deg`, `field_tags`; and for the last shot, where the
  robot was when its dip started: `shot_hive_left_in`, `shot_hive_out_in`,
  `shot_hive_yaw_deg`, `shot_heading_deg`. `dash.py shots` shows them.
- **`driveCoastWhenIdle`** (false): the drive motors coast instead of braking when the
  sticks are let go, so the robot slides by hand. Only Tool: Launch Tuning turns it on; a
  match build forces it off (0053).

## CHECK

1. In Tool: Launch Tuning, slide the robot 12 in to the left (as it faces the hive):
   `T hive_left_in` should go up by about 12. If it goes down, `aimSideReversed` is set the
   wrong way too.
2. Turn the robot a little left: note which way `T hive_yaw_deg` goes, for reading it.
3. `T field_tags` above 0 means the Limelight has a field map; then `T field_x_in` / `y`
   should move as the robot moves.

## Consequences

- Nothing in a match OpMode changes: the position only goes to the test log and telemetry.
- Tests: `VisionLimelightTest` (the signs, the field pose only with tags, nothing known),
  `TestLogTest` (where the robot stood at the shot), `test_dash.py` (the shots table).
