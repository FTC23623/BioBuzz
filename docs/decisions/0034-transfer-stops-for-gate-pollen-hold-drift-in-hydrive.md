# 0034: The transfer stops while the gate opens; pollen yellow holds half a second; drift assist in HyDrive

Status: Accepted. Not yet driven.
Date: 2026-11-02
Changes 0028 (the transfer only waited), 0033 (the pollen flicker), 0032 (drift was Practice only).

## Context

1. 0028 made the transfer **wait** for the gate when the rollers were off. When the operator
   already had them running (cross) and the driver pulled the trigger, the transfer kept
   pushing a ball against the gate for the 0.2 s it takes to swing open.
2. 0033 lit the whole strip yellow on the pollen switch and accepted the flicker from the
   holes in a ball. The coach wants a clean signal: yellow while there is pollen, dark half a
   second after the last ball leaves.
3. Drift assist should be in HyDrive, not only the Practice TeleOps.

## Decision

1. **`Intake`: while the gates are opening** (`LaunchGates.IsOpen()` and not yet
   `IsFullyOpen()`), the transfer is set to 0 whatever cross says. The intake carries on as
   it was. Fully open: both run forward, as before. Square (reverse) still wins over
   everything. The Driver Station says `transfer PAUSED while the gate opens`.
2. **`PollenSensor.IsDetected()`**: pressed now, or pressed within the last `pollenHoldMs`
   (500). The strip uses it instead of the raw `IsPressed()`. The screen shows both.
3. **`driveDriftAssist = true`** by default. The left trigger drifts in HyDrive and the
   Practice TeleOps alike. Pose Recorder gets it too, harmlessly.

## CHECK on the robot

1. Cross on (rollers running), then hold the launch trigger: the transfer stops for about
   0.2 s, then runs. The intake never stops. Let go: both back to running.
2. Roll a ball over the pollen switch: steady yellow, no flicker. Take it away: dark about
   half a second later. If a slow-turning ball still flickers, raise `pollenHoldMs`.
3. HyDrive, hold the left trigger while turning: the robot pivots on the inside back wheel.

## Consequences

- Nothing else waits on the gate. If a ball is already in the transfer against the gate
  when the trigger is pulled, the pause lets the gate push it back; raise
  `launchServoExtraSeconds` if the gate still binds.
- Tests: `IntakeTest` (pause, resume, reverse wins), `LightStripTest` (hold bridges a hole,
  clears after the hold), `DrivePracticeOptionsTest` (drift on by default).
