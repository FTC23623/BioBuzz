# 0030: An auto's LAUNCH waits for speed; a tag wait launches anyway near the end

Status: Accepted. Not yet tried on the robot.
Date: 2026-10-31
Adds to 0023 (autos launch) and 0028 (rollers wait for the gates).

## Context

The coach asked for the autos to actually launch at a LAUNCH step. The step already opened
both gates, but two things could still stop a ball going out:

1. **The gates opened at once**, even with the launchers still spinning up. StrategyA's first
   LAUNCH is about a second after START.
2. **`LAUNCH_WHEN_TAG_SEEN` waited forever** for one of our hive's tags. Blue is the default
   (no magnet), and blue counts only tags 38-41 and 42-45. A robot facing a red hive, or not
   seeing any tag, never launched. StrategyB is only that step.

## Decision

A launch step now runs:

1. **Hold still.** The light blinks yellow (orange while waiting for a tag).
2. **Tag steps only:** wait for one of our hive's tags, then turn to face the middle of its
   group. If none is seen by `autoLaunchAnywayAtSeconds` (26 s after START), skip the aim and
   launch facing the way the robot is. AUTO is 30 s.
3. **Wait for speed:** until the launchers are at speed, at most `autoSpinUpSeconds` (2 s).
   A launcher missing from the robot configuration does not hold this up.
4. **Open both gates** for `autoLaunchSeconds` (2 s). After the gate delay (0028) the intake
   and transfer push the balls in.
5. **Close the gates** and drive the next path.

The Driver Station shows each stage, plus the time since START.

## Consequences

- A launch step can take up to 2 s longer when the launchers are slow to spin up.
- StrategyB now always launches by about 26 s, tag or no tag. Lower
  `autoLaunchAnywayAtSeconds` if the robot needs time to drive after it.

## Update 2026-11-03: the first launch waits for speed, and speed must hold

The coach asked that the first launch of every auto be at speed. The launchers only start at
START, and StrategyC and D launch about a second later, so the 2 s limit could open the gates
on a wheel still spinning up, and a single "at speed" reading could catch a wheel passing
through the target on its way up.

- **The first LAUNCH waits up to `autoFirstSpinUpSeconds` (5 s)**; later ones keep
  `autoSpinUpSeconds` (2 s). Only a launcher that never gets there opens anyway.
- **At speed must hold `autoAtSpeedHoldSeconds` (0.25 s) without a break** before the gates
  open, watched every loop.
- The Driver Station shows `Waiting for launchers to reach speed: yes, 1.2 of at most 5.0 s
  (first launch)`.
- Tests: `LaunchSpinUpTest` (3).
