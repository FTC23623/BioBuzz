# Event checklist

Three lists: what to change in the code before an event, what to read on the Driver
Station at INIT before every match, and the names the code expects in the robot
configuration. Laptop setup is in `laptop-setup.md`. **First do `match-day-settings.md`**:
the loop-time and competition-rule settings, step by step.

## 1. Before the event

Change these, build green, deploy, then INIT HyDrive once and read section 2 top to bottom.

| Setting | Where | For an event | Why |
|---|---|---|---|
| `competitionMode` | `BotProperties.java`, last line | `true` | Rule R704.D: nothing may stream to FTC Dashboard in a match. The code stops sending; the dashboard itself is switched off in the Robot Controller app (menu, Disable Dashboard). INIT must then say `Competition mode: dashboard off.` (0022) |
| `botProperties.autoSpeedScale = 0.5` | any auto's `BuildPaths()` | none left (2026-11-06) | Half speed is for checking a path on the field. If one comes back, take it out once that path has run clean three times at full speed. INIT says `SLOW AUTO` while one is there. |
| `testLog`, `testTelemetry`, `launcherOnlyTelemetry` | `BotProperties.java` | `false` | Testing only, they cost loop time. See `match-day-settings.md`. |
| `frameLengthIn`, `frameWidthIn` | `BotProperties.java` | the competition frame, bumpers on | StrategyA backs up by half the frame's diagonal before its first turn. Measure, write the numbers in, then watch that turn clear the wall. |
| `launchNearRpm`, `launchFarRpm`, `launchCurve` | `BotProperties.java` | the numbers that scored | The pollen launcher's speed curve over camera range, 110 to 140 in, in 2-inch steps (0038). A trim that held all day (`launcherRangeTrimRpm`, D-pad up / down) belongs in the two RPMs; the trim goes back to 0. `pollenLauncherRpm` is the speed used with no tag in view. |
| `visionAnyTag` | `BotProperties.java` | `false` | Practice only. True at an event aims at the other alliance's hive. INIT shouts `EVERY TAG COUNTS` while it is on. |
| `safeModeEnabled` | `BotProperties.java` | decide | `false` (today) is full speed, key or no key. `true` lets the key limit the drive to 25% in the pit. |

OpModes for a match: `Garden Side Auto A`, `Flower Side Auto B` or `Flower Side Auto C`
(top of the list), then `HyDrive`. When the auto ends, the Driver Station picks HyDrive by
itself: press INIT, then START. The `Backup` group (`... Red (backup)`, `... Blue
(backup)`) is for a dead alliance sensor only, and each picks the HyDrive backup of its
own color. `Test:` and `Tool:` OpModes never run in a match (0046).

## 2. At INIT, every match

Put the robot on its start spot, facing the way the auto starts, BEFORE pressing INIT: the
odometry zeroes where the robot is at INIT. Hold it still for a second after INIT while the
Pinpoint calibrates. Then read the Driver Station top to bottom.

| Line | Good | Bad means |
|---|---|---|
| `Competition mode: dashboard off.` | that line | `PRACTICE MODE` = `competitionMode` is false. `DASHBOARD IS STILL ON` = Robot Controller menu, Disable Dashboard, INIT again. |
| `SAFE MODE ...` or `FULL SPEED ...` | what the field needs | `SAFE MODE: key is in` on the field = pull the key, INIT again. |
| `ALLIANCE: RED (magnet sensed)` or `BLUE (no magnet)` | our color | `NO SENSOR` = STOP, pick the Red or Blue backup OpMode. Wrong color = move the magnet; it re-reads until START. |
| `Forward: kept from the auto` (TeleOp, right after the auto) | that line | `Forward: the way the robot faces now` right after an auto = the heading was not handed over. In the match, turn the robot to face away from you and press circle. (0046) |
| `Heading ready: true` (TeleOp) | true | The screen stuck on `Pinpoint calibrating. Keep the robot still.` = the robot was moving. `Pinpoint FAULT` = a pod is unplugged. Autos do not show this line: check it in HyDrive first. |
| `Camera: Limelight, pipeline 0` and `Limelight: pipeline 0 is apriltag` | both | `NO CAMERA FOUND` = Limelight missing or misnamed in the configuration. `NOT CONNECTED` = the USB cable. Any other pipeline type = fix it on the Limelight web page. |
| `Indicator light: ...` and the light itself, robot pointed at our hive | blinking blue, blue or green | `BLINKING RED` = it sees only the other alliance's tags: check the magnet. Solid red = no tag in view. |
| `EVERY TAG COUNTS` | not on the screen | `visionAnyTag` is true. Never at an event. |
| `Launcher target RPM (pollen / nectar)` | the no-tag speed, about 3100 / 0 | 0 pollen = the trim was pushed down too far. |
| `... MISSING. Nothing named '...'` | none | a motor, servo or sensor is missing from the configuration: see section 3. |
| `Light strip: showing underglow` and the strip | alliance color | yellow with no ball = `pollenSensorInverted` is wrong. Dark = strip missing from the configuration, or INIT again to clear and redraw it. |
| Autos: `Paths to drive N` and no `SLOW AUTO` line | N as the auto says | `SLOW AUTO` = the `autoSpeedScale` line is still in that auto. |

By hand, before INIT: launch gates closed and flower extractor stored (servos do not move
until START, so they stay where they were left), magnet on for red, Safe Mode key out,
fresh battery (the `Battery volts` line after START reads about 13 or more on a full one).

## 3. Robot configuration names

The Driver Station's configuration must use these names exactly. They live in
`BotProperties.java` (`cfg...`). The four drive motors are required: an OpMode throws at
INIT without them. Everything else says `MISSING` at INIT and the OpMode runs on.

| Name | Configure as | Where | Used by |
|---|---|---|---|
| `leftFront`, `rightFront`, `leftBack`, `rightBack` | motor | Control Hub motor ports | the drive, Pedro in the autos |
| `pinpoint` | goBILDA Pinpoint Odometry Computer | an I2C port | heading in TeleOp, position in the autos and the Pose Recorder |
| `imu` | REV internal IMU (every Control Hub has it) | built in | heading only when there is no Pinpoint |
| `pollenLauncher`, `nectarLauncher` | motor, encoder cable plugged in | motor ports | the launchers (speed control needs the encoder) |
| `intake`, `transfer` | motor, encoder cable plugged in (1150 RPM gearmotor) | motor ports | the intake and transfer |
| `pollenServo`, `nectarServo` | Servo | servo ports | the launch gates |
| `flowerExtractor` | Servo | servo port | the flower extractor arm |
| `led` | Servo | servo port | the indicator light |
| `headlight` | Servo | servo port | the headlight |
| `limelight` | Limelight 3A | USB, under the Control Hub | the camera |
| `Webcam 1` | Webcam | USB | the camera, only when there is no Limelight |
| `allianceMagnet` | REV Touch Sensor (the magnetic limit switch) | digital port | the alliance |
| `pollenSensor` | REV Touch Sensor | digital port | pollen LEDs and the operator rumble |
| `liftHomeSwitch` | REV Touch Sensor | digital port | the Safe Mode key, only while `safeModeEnabled` is true |
| `strip` | goBILDA Prism LED Driver | an I2C port that is not the Pinpoint's | the LED strip |

A part that is switched off for an OpMode (`...Disabled = true` in that OpMode) does not
have to be in the configuration at all.
