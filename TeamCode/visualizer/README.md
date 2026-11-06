# Visualizer files

Paths for the [Pedro Pathing Visualizer](https://visualizer.pedropathing.com/), the
top-down field planner for the path library our autos use (docs/decisions/0051).

**See our autos on the field.** Run the tests (`./gradlew :TeamCode:testDebugUnitTest`).
Each run writes one file per auto and alliance to `TeamCode/build/visualizer/`, from the
autos' own code. In the visualizer pick the BIOBUZZ field, then open a file with its file
menu. LAUNCH, FLOWER and INTAKE steps show as named waits.

**Plan a new auto there.** Draw it in the visualizer. Put a wait where the robot should
launch, work a flower or switch the intake, and name it `LAUNCH`, `LAUNCH WHEN TAG SEEN`,
`FLOWER`, `INTAKE_ON` or `INTAKE_OFF`. Save the `.pp` file into this folder and run the
tests: the Java for it is in `TeamCode/build/visualizer-import/<name>.java.txt`. Paste it
into `BuildPaths()` of a copy of `AutoShapeZ`, like a Pose Recorder recording.
