# 0020: Test: Launch Servo Tuning finds each launch servo's closed and open spots

Status: Accepted. Not yet tried on the robot. Servo names and range are CHECK. The flower extractor is driven in HyDrive by gamepad 2 triangle since 0033.
Date: 2026-10-29

## Context

A ball launches when its servo opens. The launch code needs two spots per servo, closed
and open, for the pollen and nectar servos. The coach wants a TeleOp that steps each servo
5 degrees at a time and shows where it is, so the team can write the numbers down.

## Decision

- **`subsystems/LaunchServoTuner`** owns all three servos. The Bot builds it only when
  `launchServoTunerDisabled` is false (it is true by default).
- **`Test: Launch Servo Tuning`** turns it on, and turns off the drive, the launcher and
  the LED strip (it shares the D-pad).
- **Open / closed test.** Triangle/Y snaps the pollen servo to open, and the next press to
  closed. Circle/B does the same for nectar. Both start closed. Part way open counts as not
  open, so the next press opens it fully. Same buttons as those launchers in Motor Tuning.
- **Controls, gamepad 1.** D-pad up/down: pollen servo. D-pad right/left: nectar servo.
  Right/left bumper: flower extractor servo (`cfgFlowerExtractor`, `flowerExtractor`).
  Each press moves one `launchServoStepDeg` (5). Holding does not repeat.
- **The flower extractor shares the launch servos' range and step.** If it is a different
  kind of servo, give it its own range setting.
- **The screen shows both units**, for example `position 0.517 = 155 deg`. **Record the
  position number.** That is what `Servo.setPosition` takes, and it stays right even if
  the degrees setting is wrong.
- **Nothing moves during INIT.** A servo can't report where it is, so INIT shows where
  each will go at START: all three to closed.
- **Safe range, measured 2026-10-29.** Each launch servo only moves between its closed and
  open spots: nectar closed 0, open 0.267; pollen closed 0.267, open 0 (it faces the other
  way). These are `nectarServoClosed/Open` and `pollenServoClosed/Open`, ready for the
  launch code. The screen adds CLOSED or OPEN at each end.
- **Flower extractor, measured 2026-10-29:** closed (stored) 0, open (deployed) 0.567, then moved 5 degrees further to 0.584.
  `flowerExtractorClosed/Open`. Cross/A is its open / closed test. The bumpers may go
  `flowerExtractorTuneExtraDeg` (10) past open, to about 0.617, to try a further deployed
  spot; cross still deploys to 0.584.
- **Servo speed is set by the servo, not the code.** Cross, circle and triangle send the
  end position in one step, so the servo already moves at full speed. Faster needs
  hardware: a faster gearing of the same servo, or more voltage (a Servo Power Module).
- **The last step to an end is tiny.** 0.267 is 80.1 degrees, not a multiple of 5, so the
  step that reaches the end is only 0.1 degrees.
- **The target is kept in degrees**, so steps add up exactly and the screen never shows
  154.99999.

## CHECK on the robot

1. **`cfgPollenServo` / `cfgNectarServo`** (`pollenServo`, `nectarServo`) were picked here.
   Match them to the robot configuration, or rename the configuration.
2. **`launchServoRangeDeg`** (300 = goBILDA 2000 series; REV Smart Servo = 270). Only the
   degrees on the screen depend on it.

## Consequences

- The four closed/open numbers are already `BotProperties` settings. The launch code,
  when it is written, reads them from there. Re-measure here and change them there if the mechanism changes.
