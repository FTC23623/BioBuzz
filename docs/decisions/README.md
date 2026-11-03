# Decisions

Why the code is shaped the way it is. One file per decision, numbered in order.

Read these before changing anything they cover. If a decision changes, write a new file
that replaces the old one and mark the old one **Replaced by NNNN**. Do not rewrite history.

| # | Decision | Status |
|---|---|---|
| [0001](0001-one-opmode-alliance-from-hardware.md) | One OpMode; alliance comes from a badge switch | Partly replaced by 0021 |
| [0002](0002-drive-port.md) | How the mecanum drive came over from Decode | Accepted, bench test pending |
| [0003](0003-quality-gates.md) | Tests and checks from day one, aimed at the coach | Accepted, partly built |
| [0004](0004-safe-mode.md) | Safe Mode: drive limited to 25% | Accepted; switch replaced by 0007 |
| [0005](0005-hub-imu-fallback.md) | Use the Control Hub IMU when there is no Pinpoint | Accepted, mounting not checked yet |
| [0006](0006-code-for-students.md) | Write the code for students to read | Accepted |
| [0007](0007-safe-mode-key.md) | Safe Mode input: magnetic key, checked once at INIT | Accepted; touch sensor stands in |
| [0008](0008-pedro-pathing-autos.md) | Autonomous uses Pedro Pathing 3, odometry pods on the hub | Accepted, not tuned yet |
| [0009](0009-launcher-velocity-control.md) | The launcher holds a speed with our own control loop | Accepted, not tuned yet |
| [0010](0010-bot-and-bot-properties.md) | OpModes talk to a Bot; settings live in BotProperties | Accepted, not on the robot yet |
| [0011](0011-vision-aim-assist.md) | Aim assist: hold the left bumper, face the nearest AprilTag | Accepted, turn direction unchecked |
| [0012](0012-ftc-dashboard.md) | FTC Dashboard mirrors the telemetry as live graphs | Accepted; live tuning added by 0022 |
| [0013](0013-pinpoint-for-pedro.md) | Pedro uses the Pinpoint when the robot has one | Accepted, not tuned yet |
| [0014](0014-safe-mode-off-for-foresight.md) | Safe Mode switched off while Foresight is tested | Accepted, temporary |
| [0015](0015-indicator-light.md) | Indicator light: red, blinking blue, blue, green for 0, 1, 2, 3+ tags | Accepted, not on the robot yet |
| [0016](0016-paths-live-in-the-opmode.md) | An auto's paths live in the auto's own file | Accepted |
| [0017](0017-led-strip-prism.md) | The LED strip is a goBILDA Prism driver over I2C | Accepted, two CHECKs on the strip |
| [0018](0018-driver-practice-options.md) | Driver practice options come over from Decode as BotProperties settings | Accepted, not driven yet |
| [0019](0019-motor-tuning-mode.md) | Test: Motor Tuning spins the intake, transfer and both launchers | Accepted; shared targets replaced by 0023 |
| [0020](0020-launch-servo-tuning.md) | Test: Launch Servo Tuning finds each launch servo's closed and open spots | Accepted, not on the robot yet |
| [0021](0021-alliance-magnet-and-backup-autos.md) | Alliance from one magnet sensor; backup autos per color | Accepted, sensor not on the robot yet; LED part replaced by 0026 |
| [0022](0022-competition-mode-and-live-tuning.md) | Competition mode switch; live tuning on the dashboard at practice | Accepted, competitionMode false today |
| [0023](0023-teleop-scoring-controls.md) | TeleOp scoring: driver launches, operator feeds and reverses | Accepted, not driven yet |
| [0024](0024-pose-recorder.md) | Pose recorder: drive the route, save the spots, paste the Java | Accepted, replay not built |
| [0025](0025-roller-gains-and-battery-graph.md) | Intake and transfer gains on the dashboard; battery volts graph | Accepted, gains not checked on the robot |
| [0026](0026-underglow-always-and-pollen-leds.md) | Underglow shows the alliance all the time; pollen sensor lights LEDs yellow | Accepted, not on the strip yet |
| [0027](0027-strip-cleared-at-init-first-last-leds.md) | Strip cleared at INIT; LED ranges are first and last LED (underglow 12-35) | Accepted, Prism stop counting is CHECK |
| [0028](0028-transfer-waits-for-launch-gate.md) | On the launch trigger, the transfer waits for the gates to swing open | Accepted, servo speed is CHECK |
| [0029](0029-launcher-wrong-way-allows-short-reverse.md) | The launcher wrong-way stop allows a short reverse | Accepted, not on the robot yet |
| [0030](0030-auto-launch-waits-for-speed.md) | An auto's LAUNCH waits for speed; a tag wait launches anyway near the end | Accepted, not on the robot yet |
| [0031](0031-indicator-blinks-red-for-other-alliance-tags.md) | The indicator blinks red for the other alliance's tags, and says why at INIT | Accepted, not on the robot yet |
| [0032](0032-controls-update-field-centric-launchers-on-slow-reverse.md) | Controls update: field-centric HyDrive, launchers on from START, slower reverse, drift on the left trigger | Accepted, not driven yet |
| [0033](0033-pollen-whole-strip-flower-extractor-any-tag-practice.md) | Pollen lights the whole strip; flower extractor on triangle; any-tag practice switch | Accepted, not on the robot yet |
| [0034](0034-transfer-stops-for-gate-pollen-hold-drift-in-hydrive.md) | The transfer stops while the gate opens; pollen yellow holds half a second; drift assist in HyDrive | Accepted, not driven yet |
| [0035](0035-pollen-clears-strip-rumbles-operator-inverted-switch-no-camera-line.md) | Pollen clears every layer and rumbles the operator; the switch is inverted; "no camera" says so every loop | Accepted, not on the robot yet |
| [0036](0036-launch-snaps-to-hive-middle-launch-distance-strip-hard-clear.md) | Launching snaps to the hive's middle and notes the distance; the strip clear is forced | Accepted, not driven yet |
| [0037](0037-rollers-are-1150-rpm-motors-half-speed-launch-feed.md) | The intake and transfer are 1150 RPM motors (145.1 ticks per turn); a launch is fed at half speed | Accepted, ticks per turn is CHECK |
| [0038](0038-launcher-speed-from-distance.md) | The pollen launcher's speed follows the distance to the tag, in 10-inch bands | Accepted, two rows measured |

## Template

```markdown
# NNNN: Title

Status: Proposed | Accepted | Replaced by NNNN
Date: YYYY-MM-DD

## Context
What problem, and what was true when we decided.

## Decision
What we chose.

## Consequences
What this makes easier, what it makes harder, what to watch for.
```
