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
| [0039](0039-controls-update-aim-arc-launchers-always-on.md) | Controls update: aim and slow swap bumpers, the D-pad arcs around the hive, launchers always on, intake on the left bumper | Accepted, not driven yet |
| [0040](0040-launch-lock-aim-only-on-button-auto-reject.md) | The launch needs a camera lock (operator R2 overrides); aim and tracking only while a button is down; an extra ball is spat out; slow at half | Accepted, not driven yet |
| [0041](0041-quiet-telemetry-for-loop-time.md) | Quiet telemetry, a dashboard switch, to measure what the Driver Station lines cost | Accepted, not measured yet |
| [0042](0042-flower-step-and-recorder-circle.md) | A FLOWER step for the autos (back 6, arm out, 7-inch strokes twice, arm in), recorded with circle | Accepted, not driven yet |
| [0043](0043-auto-intake-roller-only-for-flower.md) | In autos the intake roller runs only during FLOWER; the transfer feeds launches at 30% | Accepted, not driven yet |
| [0044](0044-aim-offset-by-side-of-hive.md) | The aim offset flips with the side of the hive the robot stands on | Accepted, not driven yet |
| [0045](0045-launch-wiggle.md) | Launch wiggle: every launch swings 1 deg left and right, a side per ball; on by default, square toggles | Experiment, not driven yet |
| [0046](0046-auto-to-teleop-handoff.md) | The autos go straight on to TeleOp and keep the heading; renamed by field side, color-sensing first | Accepted, not run on the field yet |
| [0047](0047-auto-intake-on-off-steps.md) | INTAKE_ON and INTAKE_OFF auto steps, recorded with D-pad up / down | Accepted, not driven yet |
| [0048](0048-steady-range.md) | Steady range: average our tags and hold each speed step; on by default, gamepad 2 cross toggles | On by default, not driven yet |
| [0049](0049-always-track-range.md) | Always-track range: follow the distance whenever a tipped-up tag of ours is seen, gamepad 2 circle | Experiment, off by default |
| [0050](0050-roller-power-launch-lock-test-log.md) | Roller power mode, launch RPM lock, test log and test telemetry, match-day settings | Accepted, all off by default |
| [0051](0051-pedro-visualizer-tools.md) | Autos on the field: start spots, export to and import from the Pedro Pathing Visualizer | Accepted, laptop tools only |
| [0052](0052-launch-tuning-sessions.md) | Launch tuning sessions: Claude changes settings and reads shots over FTC Dashboard; Tool: Launch Tuning | Accepted, practice only, not yet run |
| [0053](0053-match-build-and-auto-names.md) | Match build (`matchBuild=true`, Android Studio or `-PmatchBuild`) leaves test and tool OpModes out, turns competition mode and the match settings on; autos named Auto / Blue / Red | Accepted |
| [0054](0054-position-from-tags.md) | Position from the AprilTags (left of / out from the hive, the Limelight's field pose) for tuning; wheels coast in Tool: Launch Tuning | Accepted, signs CHECK |
| [0055](0055-pose-recorder-blips-and-centric.md) | Pose Recorder: a saved step blips its mechanism; back / share switches robot- and field-centric | Accepted |

## TODO: confirm on the robot after this week's tuning

Most of 0018 to 0038 still say "not driven yet" or "not on the robot yet". Each line below
is the one thing to see on the robot before its status above changes. Tick the box, then
edit the status in the table. The statuses stay as they are until then. Each decision's
own CHECK section has the detail.

- [ ] **0018** Hold the left trigger while turning: the robot pivots on its inside back wheel. Tune `driveDriftSideGain` and `driveDriftRearGain` if the pivot lands off the wheel.
- [ ] **0019** Test: Motor Tuning: both launchers spin forward (both are `Reversed = true`), the intake pulls in, the transfer carries up. The rollers' ticks per turn are 0037's now.
- [ ] **0020** The launch servos answer to `pollenServo` and `nectarServo` in the configuration, and `launchServoRangeDeg` matches the servo (300 goBILDA, 270 REV).
- [ ] **0021** The magnet sensor is on the robot as `allianceMagnet`: magnet = `RED`, none = `BLUE`, and the Driver Station says which.
- [ ] **0022** Thursday night, after the last tuning session: `competitionMode = true`, Disable Dashboard in the Robot Controller app, INIT says `Competition mode: dashboard off.`
- [ ] **0023** Driver's right trigger launches; operator's left bumper (0039), left trigger and square run the rollers forward, held, and backward.
- [ ] **0024** StrategyC and StrategyD, pasted from the recorder, drive the route that was recorded.
- [ ] **0025** INIT shows `Intake hub started with P / I / D / F` as 10 / 3 / 0 / 0. If not, copy the hub's numbers into the `roller` settings.
- [ ] **0026 / 0027** INIT: the strip goes dark, then LEDs 12 to 35 light in the alliance color. Count 24. If LED 35 is dark, `prismStopIsOnePastLast = false`. START changes nothing on the strip.
- [ ] **0028** Hold the launch trigger: the gate is fully open just before the transfer starts, about 0.2 s. Too early, raise `launchServoExtraSeconds`.
- [ ] **0029** A launcher kicked backward for a moment keeps running; only a full second backward stops it. There is no off button now (0039): a stopped wheel needs the OpMode restarted.
- [ ] **0030** An auto's LAUNCH opens the gates once `Launchers at speed` is true. StrategyB with no tag in view still launches by 26 s.
- [ ] **0031** Camera on the other alliance's tags: the indicator BLINKS red and the Driver Station line says to check the magnet. On ours: blinking blue, blue, green.
- [ ] **0032** START HyDrive: launchers spin with nothing pressed, and nothing stops them (0039). Square lets out one ball at a time (400 RPM). Forward on the stick is away from the driver with the robot turned.
- [ ] **0033** A ball on the pollen switch lights the whole strip yellow. The Driver Station names dropped tags (`seen but NOT OURS`). `visionAnyTag` is false. Triangle on gamepad 2 deploys the extractor.
- [ ] **0034** Cross on, then hold the trigger: the transfer stops for the gate's 0.2 s, the intake does not. The yellow holds half a second after the ball leaves.
- [ ] **0035** INIT HyDrive shows the Limelight lines, not `NO CAMERA FOUND`. Empty intake: underglow, not yellow (`pollenSensorInverted`). A ball: yellow and one rumble on gamepad 2.
- [ ] **0036** Right bumper (0039) with two hive tags in view faces between them; the launch trigger turns toward the hive. `Last launch (camera)` shows the inches.
- [ ] **0037** Test: Motor Tuning, intake target 1150: actual reads about 1150 at power near 1 (`intakeTicksPerRev` 145.1). A launch feeds both rollers at half.
- [ ] **0038** Shoot from 110 and 140 in to set `launchNearRpm` and `launchFarRpm`, then from 120 and 130 to set `launchCurve`. A trim that held all day goes into the two RPMs.
- [ ] **0042** A circle-recorded spot runs back 6, arm out, four 7-inch strokes, arm in, with the intake on; the `Flower` line counts the stages.
- [ ] **0039** Right bumper aims, left bumper slows. Right bumper plus D-pad left or right: the robot circles the hive keeping its distance (the `Arc` line shows holding and now). Gamepad 2 left bumper toggles the rollers; its right bumper and cross do nothing.
- [ ] **0040** Trigger with no tag: shut and gamepad 1 buzzes; gamepad 2 R2 overrides. Nothing pressed: no turning, `Aim assist: off`. A fifth ball is spat out and the rollers stop until gamepad 2 lets go. Transfer feeds at 30%. Slow is half speed.

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
