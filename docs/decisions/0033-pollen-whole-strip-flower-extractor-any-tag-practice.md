# 0033: Pollen lights the whole strip; the flower extractor on triangle; an any-tag practice switch

Status: Accepted. Not yet tried on the robot.
Date: 2026-11-02
Changes the pollen LEDs of 0026/0027, adds a driver for 0020's flower extractor servo, adds to 0021's tag filter.

## Context

Three asks from the field on 2 Nov:

1. The pollen sensor should light **all** the LEDs yellow, not two small groups. The holes
   in a ball make the switch open and close as the ball turns, so the yellow will flicker a
   little. The coach is fine with that for now.
2. **The Limelight "doesn't see tags"**: the indicator light stays red and aim assist does
   nothing. Nothing in the camera code changed except 0021's filter: only our alliance's
   tags (red 30-37, blue 38-45) count. With no magnet on the robot the alliance reads BLUE,
   and any tag that is not 38-45 (the red hive, or last season's tags 20-24) is dropped
   before the light or aim assist sees it. The Driver Station counted dropped tags but did
   not name them, so the cause was invisible from the field.
3. **The operator's triangle** should deploy the flower extractor. The servo and its two spots
   (`flowerExtractorClosed` 0, `flowerExtractorOpen` 0.584) existed since 0020 but nothing in
   HyDrive moved it.

## Decision

1. **Pollen LEDs = the whole strip.** `pollenLedsFirst = 0`, `pollenLedsLast = 35`, one layer
   (2). The second group and layer 3 are gone. Change the two numbers to go back to a group.
2. **The camera names what it dropped.** Under "Other alliance's tags ignored" the Driver
   Station now prints `seen but NOT OURS: tag 24, tag 20. We are BLUE; ours are 38 to 45.`
   With 0031, the indicator light blinks red at the same time. Both cameras do this.
3. **`visionAnyTag`** in BotProperties, false by default. true: every tag counts, for practice
   with old tags or no magnet. The Driver Station warns in capitals every loop while it is on.
   NEVER true at an event: aim assist would lock onto the other hive.
4. **`subsystems/FlowerExtractor`**: gamepad 2 **triangle (hold)** = deployed, let go = stored.
   Hold, not toggle, like the launch gates, so it is never left out by mistake. Nothing moves
   at INIT; START sends stored. `flowerExtractorDisabled` leaves it out of an OpMode; the
   servo tuner, motor tuning and launcher test OpModes set it, since they own the same servo
   or should move nothing but motors.

## If the Limelight still sees nothing, in this order

1. **Driver Station, "Other alliance's tags ignored"** above 0, and the light blinking red:
   the tag in view is not ours. Put the magnet on for red, or set `visionAnyTag = true` for a
   practice session with old tags. Put it back to false.
2. **"Limelight: pipeline 0 is ..."** must say an AprilTag type. If it says color or
   something else, pipeline 0 was changed in the Limelight web page (http://192.168.43.1:5801,
   laptop on the robot's Wi-Fi). Make it an AprilTag pipeline, or point
   `limelightAprilTagPipeline` at the one that is.
3. **"Limelight result: NOT CONNECTED"**: the USB cable from the Limelight to the hub.
4. **The headlight** is new since the camera last worked. Set `headlightDisabled = true` for one
   run; if tags come back, the light is washing them out and needs aiming or dimming.

## Consequences

- With the whole strip yellow, the D-pad colors and the alliance underglow disappear while a
  ball is sensed. That is the ask.
- Triangle on gamepad 2 is taken. Free there now: both sticks, right trigger, left bumper,
  circle.
- Tests: `FlowerExtractorTest` (5), `LightStripTest` (whole strip), `VisionLimelightTest`
  (named ignored tags, practice switch).
