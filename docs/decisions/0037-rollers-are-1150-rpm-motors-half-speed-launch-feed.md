# 0037: The intake and transfer are 1150 RPM motors; a launch is fed at half speed

Status: Accepted. Not yet on the robot. The ticks-per-turn number is a CHECK.
Date: 2026-11-02
Changes 0019 (ticks per turn "settled" as the launchers' 28) and 0023/0032 (the roller speeds).

## Context

The coach: the intake and transfer top out at 1150 RPM, the launchers at 6000. So they are
not the same motor, which 0019 had settled on 2026-10-30. A 1150 RPM goBILDA motor has a
5.2:1 gearbox, and goBILDA gives 145.1 encoder ticks per turn of its output shaft; the code
had 28 (a bare 6000 RPM motor). Every roller RPM on the screen was therefore off by about
5x, and the "2900 RPM" targets were really about 560 RPM at the output.

The coach also wants the rollers at 100% when intaking on purpose and at 50% while feeding
a launch, so balls go into the launcher one at a time at a steady rate.

## Decision

- **`intakeTicksPerRev = 145.1`**, **`intakeMaxRpm = 1150`**. Output RPM, like the launchers'
  numbers are shaft RPM.
- **`intakeRpm = transferRpm = 1150`**: full speed when cross or the left trigger runs them.
- **`launchFeedScale = 0.5`**: while the gates are fully open, both run at this fraction of
  the speeds above. **Launching wins over cross**: the feed speed is about the launcher, not
  who pressed what. Square (reverse) still wins over everything. On the dashboard's Intake
  group.
- **Reverse `= 400`** for both, about a third of full, keeping 0032's ratio.

## CHECK on the robot

1. Test: Motor Tuning, intake at 1150: the actual RPM should read about 1150 with the power
   near 1. If the actual sits far below the target at power 1, `intakeTicksPerRev` is too
   big. If the target is reached at a low power, it is too small. The launchers' 28 was
   wrong by about 5x; 145.1 is goBILDA's number for the 1150 RPM motor (5203-2402-0005).
2. HyDrive: cross on, the rollers run flat out. Pull the launch trigger: after the 0.2 s
   gate pause they run at half. Let go: full again.

## Consequences

- 100% of a motor's free speed leaves the speed control no headroom under load: the hub
  will sit at full power and the actual RPM will read a little under 1150. That is fine for
  an intake. If the launch feed is still too fast or too slow, change `launchFeedScale` on
  the dashboard, then write it here.
- Tests: `IntakeTest` (feed at half, launch wins over cross, speeds fit the motor).
