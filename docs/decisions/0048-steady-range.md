# 0048: Steady range, for a steadier launcher speed

Status: Accepted, 2026-11-05, on by default since the same day. Not yet driven.

## Context

While aiming, the pollen launcher's target RPM jumped around. Two causes:

1. The distance came from ONE tag, the biggest in the picture. The hive has several tags
   at slightly different distances, and which one looks biggest swaps frame to frame.
2. The speed curve (0038) steps every 2 in, about 30 RPM a step. A distance that wobbles
   across a step's edge flips the target every loop.

The coach wanted both fixed, as something they can switch on and off to compare.

## Decision

- **`steadyRange`** (false): the switch. **Gamepad 2 cross** flips it during a run (one
  rumble on, two off); the dashboard's Launcher group has it too. Off is exactly the old
  behaviour.
- With it on:
  - **Average our tags** (`steadyRangeAverage`, true): the distance is the average of
    every tag in view from the nearest tag's group (our hive), the same tags aim already
    averages for its bearing. A tag with no distance is left out.
    `AimAssist.GroupAverage` does the sum for both.
  - **Hold the step** (`launchBandHoldIn`, 1 in): the speed stays on its step until the
    distance is 1 in past the step's edge. On the 120 step (120 to 122), 119.2 and 122.8
    both stay 120; 118.9 goes to 118, 123.1 to 122. The steps stay 2 in apart, so the
    speed still follows the distance as closely. 0 = no hold.
- "Launcher speed from distance" starts with `raw` or `STEADY (N tags, step S in)`, so the
  two can be compared on the Driver Station. The Limelight's "Group middle" line shows the
  averaged distance next to the nearest tag's.

## CHECK on the robot

1. Park about 125 in from the hive and hold aim. Watch "Launcher speed from distance"
   with steady off: note how often the RPM changes.
2. Gamepad 2 cross (one rumble), same spot: the RPM should hold still. Then drive slowly
   in and out: it should still step, a little later than before.
3. Shots from a few distances in each mode. If steady wins, set `steadyRange = true` in
   BotProperties.
4. Still jumpy with steady on: try `launchBandHoldIn` 2, or `launchBandIn` 5.

## Consequences

- With the hold, a step changes up to 1 in later than before, driving in or out.
- Autos follow the same switch: steady is off in the autos unless it is set in BotProperties.
- Tests: `AimAssistTest` (the average skips zeros), `VisionLimelightTest` (group range),
  `LauncherTest` (the hold, off is unchanged, the group range, cross toggles).

## Update, 2026-11-05: on by default

Coach: `steadyRange` is **true** from now on, so TeleOp and the autos both use it. Gamepad
2 cross still turns it off (two rumbles) for a comparison.
