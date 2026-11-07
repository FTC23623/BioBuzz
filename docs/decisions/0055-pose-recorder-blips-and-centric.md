# 0055: Pose Recorder: steps blip their mechanism, back / share switches the sticks

Status: Accepted, 2026-11-07. Tool: Pose Recorder only (not in a match build, 0053).

## Context

When recording an auto, nothing showed a LAUNCH, FLOWER or INTAKE step had been saved
except a rumble. And the recorder drove robot-centric only; the coach wanted to switch.

## Decision

- **Blips**: saving a spot with a step runs that step's mechanism for
  `recorderBlipSeconds` (0.6 s), so the team sees it counted:
  - FLOWER: the flower extractor swings out, then back.
  - INTAKE_ON: the intake roller runs forward. INTAKE_OFF: it runs backward. The transfer
    stays still, so no ball goes up.
  - LAUNCH: the indicator light blinks yellow, as in an auto's LAUNCH. The launch gates do
    not open: the launchers spin from START, so a loaded ball would fly.
  The recorder holds the blip each loop after the parts read their own buttons
  (`FlowerExtractor.SetDeployed`, `Intake.BlipIntakeRpm`, one loop at a time,
  `Indicator.SetLaunching`); when it ends they go back to their buttons. A new blip ends the
  last one.
- **Back / Share** (gamepad 1) switches the sticks between robot-centric (at START) and
  field-centric, where forward is the way the robot faced at INIT (on the auto's start
  spot). One rumble field-centric, two robot-centric; the screen says which.
- **`circleResetsForward`** (true): the recorder sets it false, so circle is only FLOWER
  there, never "reset forward" in field-centric.

## Consequences

- HyDrive and the autos are unchanged.
- Tests: `PoseRecorderTest` (each step blips its part, a blip ends on time, back / share
  switches), `IntakeTest` (a blip lasts one loop), `DriveAimTest` (circle and
  `circleResetsForward`).
