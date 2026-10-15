# Tuning the drive

How to get the robot from "the motors spin" to "it drives a path by itself".
Do the steps in order. Each one has a check, so you know when it worked.

Safety, every time:

- **Wheels off the ground** for steps 1, 2 and 3. Hand on STOP.
- **Safe Mode key in** (quarter speed) for every step except the Pedro tuners. The tuners
  run at full power on purpose.
- The Pedro tuners need **floor space: about 8 feet ahead, 8 feet to the left, and room
  behind and to the sides.** Nobody in that space.

| Step | What it tunes | Where the numbers go | Time |
|---|---|---|---|
| [1. Wiring](#1-wiring-wheels-off-the-ground) | Each motor spins the right way | Robot configuration, not code | 10 min |
| [2. Heading](#2-heading-still-off-the-ground) | The hub IMU reads turns the right way | `BotProperties`: `hubLogoFacing`, `hubUsbFacing` | 10 min |
| [3. Pod ports](#3-pod-ports-still-off-the-ground) | Which port each odometry pod is on | `BotProperties`: `cfgForwardPodPort`, `cfgStrafePodPort` | 10 min |
| [4. Drive feel](#4-drive-feel-on-the-floor) | How TeleOp feels to the driver | `BotProperties`: the drive section | 20 min |
| [5. Pedro AutoTune](#5-pedro-autotune-on-the-floor-full-power) | Everything Pedro needs to follow a path | `pedro/PedroConstants.java` | 1 to 2 hours |
| [6. First auto](#6-first-auto) | Proves it all | Nothing | 15 min |

---

## 1. Wiring (wheels off the ground)

1. Run **Test: Drive Motors**.
2. Hold each D-pad direction: up = front left, right = front right, down = back right,
   left = back left.
3. **Check:** each wheel rolls the way the robot drives forward.

A wheel that rolls backward is fixed in the wiring or the robot configuration, never in
code. A wheel that does not move at all is on the wrong port.

## 2. Heading (still off the ground)

1. Run **HyDrive** and stay in INIT.
2. The Driver Station says which heading source it picked. With no Pinpoint it says
   `Heading from: Control Hub IMU`.
3. Turn the robot left by hand, about a quarter turn.
4. **Check:** `Yaw` goes up as you turn left. Turn it back: `Yaw` comes back to 0.

If `Yaw` goes down, or barely moves, the hub's mounting is set wrong. Look at the Control
Hub on the robot: which way does the REV logo face, and which way do the USB ports point?
Put those in `hubLogoFacing` and `hubUsbFacing` in `BotProperties.java`, build, and try
again. See [decision 0005](decisions/0005-hub-imu-fallback.md).

## 3. Pod ports (still off the ground)

The two odometry pods plug into the encoder jacks of two motor ports. Pedro reads a pod
by the name of the motor on that port. The names in `BotProperties` are guesses until
someone checks.

1. Run **Test: Odometry Pods**. It shows a count for each of the four motor ports.
2. Roll the **forward** pod wheel with one finger. One count changes. That port is the
   forward pod's port.
3. Press circle to zero the counts. Roll the **strafe** pod wheel. That port is the strafe
   pod's port.
4. **Check:** the ports match `cfgForwardPodPort` and `cfgStrafePodPort` in
   `BotProperties.java`. If not, change them and build.

Which way the count goes (up or down) does not matter here. The Two Wheel Tuner in
step 5 works that out.

## 4. Drive feel (on the floor)

Safe Mode key in. Drive with **HyDrive**: forward, strafe, turn. Then turn the robot a
quarter turn and push the stick forward: it should still go away from the driver
(field-centric). Press circle: the controller buzzes and the way the robot faces now
becomes forward.

Then take the key out for full speed and let the driver try these, one at a time, in
`BotProperties.java`:

| Setting | What it changes | Normal value |
|---|---|---|
| `driveSlow` | Top speed while holding the left trigger, for lining up | `0.25` |
| `driveNormal` | Top speed with no trigger held | `1` |
| `driveBoosted` | Top speed while holding the right trigger | `1` |
| `driveStrafeGain` | Sideways gets a little extra, because mecanum wheels strafe slower | `1.1` |
| `driveMotorSlewRate` | Most a motor's power may change in one loop. Smaller is smoother, bigger is snappier | `0.2` |
| `fieldCentricDrive` | `true`: forward is away from the driver. `false`: forward is the way the robot faces | `true` |

Change one, build, drive, decide. Write down what the driver liked.

## 5. Pedro AutoTune (on the floor, full power)

Pedro Pathing needs three sets of numbers, and only the real robot can give them. Pedro's
AutoTune page runs the tests and prints the lines to paste into
`TeamCode/src/main/java/org/firstinspires/ftc/teamcode/pedro/PedroConstants.java`.

**Opening AutoTune**

1. Robot on. Join the robot's Wi-Fi with a laptop, the same way the Driver Station does.
2. In a browser, open `http://192.168.43.1:10158`.
3. The page lists the four tuners from `pedro/Tuning.java`: Mecanum Tuner, Two Wheel
   Tuner, Foresight Tuner, and Tests. Run them in that order. The page walks through each
   step and shows the result in a Java tab. Every result line starts with `c.`.

**5a. Mecanum Tuner (wheels off the ground is fine)**

- It asks for the four motor names. Ours: `leftFront`, `rightFront`, `leftBack`,
  `rightBack`.
- It spins each motor and asks which way it spun.
- **Paste** the result lines into `DriveConfig()`.
- **Check:** left motors `FORWARD`, right motors `REVERSE`, the same as `Drive.java`. If
  the tuner says otherwise, step 1 was not finished.

**5b. Two Wheel Tuner (on the floor, you push the robot)**

- It asks for the forward pod's motor name and the strafe pod's motor name (step 3), the
  IMU name (`imu`), and the hub's logo and USB directions (step 2).
- It asks how far you will push: 48 inches is two tiles. Measure it with tape first.
- Then it has you push the robot forward 48 inches exactly, push it left 48 inches exactly,
  push forward again, push left again, and spin it in place half a turn to the left.
- **Paste** the result lines into `LocalizerConfig()`.
- **Check:** after pasting, run the Tests tuner's Localization Test: push the robot
  forward and the x number grows, push left and y grows, turn left and the heading grows.

**5c. Foresight Tuner (full power, clear the floor)**

This is the one that drives by itself at full speed. Key out, everyone clear, hand on STOP.

- It asks for a distance (48 inches), a speed, and a braking distance (at least 15
  inches). The defaults are fine.
- It then runs ten short drives one after another: forward and strafe top speed, forward
  and strafe coasting, heading braking (it turns back and forth), heading (it spins in
  place), forward and strafe braking (it drives forward and back, left and right, at
  several speeds), and forward and strafe correction (it moves one or two feet and stops).
- **Paste** the 17 result lines into `ForesightConfig()`.
- Then set `foresightTuned = true` at the top of `PedroConstants.java`, and build.

**5d. Tests**

The Tests tuner has a Hold Test, Line Test and Curve Test. Run them with the key out. The
robot should go where the line on the page says and stop without wobbling.

## 6. First auto

Key in, so it moves at quarter speed. Put the robot on its start spot, facing forward.
Run **Shape: C (no turning)**. It drives a C two tiles tall: forward, slide left, back.

- **Check:** it ends about two tiles to the left of where it started, still facing forward.
- At quarter speed it may brake early and creep to the end. That is expected: the Foresight
  numbers were measured at full speed. See [decision 0008](decisions/0008-pedro-pathing-autos.md).
- Then **Shape: C (turning)** and **Shape: Z**.

When the shapes are right, the drive is tuned. Any auto the team writes from here on is
a list of paths in `pedro/ShapePaths.java` and a 15-line OpMode.

## If something goes wrong

| What you see | Likely cause | Try |
|---|---|---|
| `PEDRO IS NOT TUNED YET` on the Driver Station | `foresightTuned` is still `false` | Step 5c |
| The auto drives the mirror image of the shape | A pod direction is flipped | Run the Two Wheel Tuner again, paste, build |
| The auto turns the wrong way or spins | Hub mounting is wrong | Step 2 |
| The Localization Test's x and y are swapped | The pod ports are swapped | Step 3 |
| One wheel does not move in the Mecanum Tuner | Wrong port or a loose cable | Step 1 |
| The robot fights itself in the tuners | A drive motor is in `RUN_USING_ENCODER` with a pod on its port | `SafeModeMecanum` sets `RUN_WITHOUT_ENCODER`; check nothing else changed it |
