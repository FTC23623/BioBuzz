# 0024: Pose recorder: drive the route, save the spots, paste the Java

Status: Accepted. Not yet tried on the robot. Replay auto not built.
Date: 2026-10-30
First half of PORT-PLAN P19 (Train mode).

## Context

Writing an auto's poses by hand is slow and error-prone: measure, guess, run, fix. The
coach saw a factory app where a robot arm is moved by hand and "remembers" positions. The
same idea fits Pedro: drive there, press a button, get the Pose.

## Decision

- **`Tool: Pose Recorder`**: HyDrive, robot-centric, plus `subsystems/PoseRecorder` on
  gamepad 1. Cross saves a spot, triangle saves a spot plus LAUNCH, square forgets the last.
- **Position from Pedro's own localizer** (`PedroConstants.CreateLocalizer`), zeroed at
  INIT. The spots are in the same inches and degrees as the autos (x forward, y left).
- **Record spots, never motor powers.** Pedro corrects for battery and carpet when it
  drives them later.
- **Output is Java**: one `Pose` per spot, a `line(...)` between each, `.constant(...)` or
  a smooth turn for the heading, `paths.add(LAUNCH)` after triangle spots. Written after
  every press to `/sdcard/FIRST/recordings/PoseRecording_<date>.java.txt`.
- Headings are unwrapped (350 after 0 is written as -10) so the auto turns the short way.

## Consequences

- Students get the file off the hub with Android Studio's Device Explorer, or
  `adb pull /sdcard/FIRST/recordings`.
- The recording is only as good as the start: square the robot on the same spot for the
  recording and the auto.
- Not built: a replay auto that reads the file at INIT. Copy-paste first; build replay
  once students have used the recorder.
