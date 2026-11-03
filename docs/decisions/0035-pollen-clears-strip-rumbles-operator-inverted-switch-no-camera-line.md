# 0035: Pollen clears every layer and rumbles the operator; the switch is inverted; "no camera" says so every loop

Status: Accepted. Not yet tried on the robot.
Date: 2026-11-02
Changes 0033/0034 (the pollen strip), 0026 (the pollen switch), 0011 (the no-camera message).

## Context

From the field, same evening:

1. The whole-strip yellow (0033) did not take the whole strip. The underglow (layer 1) and
   the D-pad color (layer 0) were still drawn, and the Prism's layer order is a CHECK we have
   never settled. The coach asked to clear all layers.
2. The operator can't watch the strip while loading. A rumble on gamepad 2 says "ball in".
3. **The pollen switch reads pressed with nothing on it** and lets go when a ball arrives.
   So the strip was yellow with an empty intake.
4. **HyDrive shows no Limelight lines at all.** `VisionSelector` only builds `Vision_Limelight`
   when something named `limelight` is in the robot configuration; otherwise it hands the Bot
   a `Vision_None`, which said so once at INIT and then went quiet. A camera that is plugged
   in but misnamed or missing from the configuration looked exactly like no camera, and the
   indicator light stayed red. This is the most likely reason the Limelight "sees nothing".

## Decision

1. **Pollen owns the strip.** The moment pollen is detected, `LightStrip` clears every layer
   and draws yellow over the whole strip on layer 2, so nothing else shows whichever way the
   Prism stacks layers. When pollen has been gone for `pollenHoldMs`, every layer is cleared
   again and the alliance underglow is drawn back. A D-pad demo color is lost then; press it
   again. Each transition is ten clears and one or two draws, and transitions are rare.
2. **`PollenSensor.Process()` rumbles the operator's gamepad** for `pollenRumbleMs` (300) the
   loop pollen is first detected, once per arrival. 0 turns it off. After START only.
3. **`pollenSensorInverted = true`**: the raw switch is flipped, so `IsPressed()` always means
   pollen. If the wiring is fixed later, set it false.
4. **`Vision_None` prints `NO CAMERA FOUND: nothing named 'limelight' (a Limelight 3A) or
   'Webcam 1' (a Webcam) in the robot configuration` every loop**, at INIT and after START.

## CHECK on the robot

1. INIT HyDrive and read the top of the Driver Station. If it says NO CAMERA FOUND: open the
   robot configuration on the Driver Station, add the Limelight 3A (it is a USB device, under
   the Control Hub) and name it exactly `limelight`. Save, activate, INIT again. The Limelight
   lines and the pipeline type should appear, and the light should leave solid red once a tag
   of ours is in view.
2. Empty intake: the strip shows the alliance underglow, not yellow. Put a ball on the
   switch: the whole strip goes yellow, gamepad 2 buzzes once. Take it away: half a second
   later the yellow goes and the underglow comes back.

## Consequences

- The underglow disappears while a ball is in. If the drive team wants the strip fully dark
  with no ball instead of the underglow, delete the `ShowAlliance()` call in
  `LightStrip.ShowStatus()`.
- Tests: `PollenSensorTest` (7), `LightStripTest` (clear-then-yellow order, underglow back).
