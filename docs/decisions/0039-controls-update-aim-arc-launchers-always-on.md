# 0039: Controls update: aim and slow swap bumpers, the D-pad arcs around the hive, launchers always on, intake on the left bumper

Status: Accepted. Not yet driven. The arc numbers are starting guesses.
Date: 2026-11-03
Changes 0032 (slow mode on the right bumper, the launcher off / on button), 0023 (the intake toggle on cross), 0036 (aim on the left bumper, the D-pad left hard clear) and 0017 (the D-pad demo colors).

## Context

After driving on 2026-11-03 the coach asked for four changes:

1. Aim belongs next to launch, and slow next to drift: the two "at the hive" actions on the
   right hand, the two "drive carefully" actions on the left.
2. The strip's D-pad colors are not needed at a competition. The D-pad should instead let
   the driver walk around the hive while aiming, keeping the distance.
3. The launchers are never wanted off in a match, so the off / on button is a way to lose
   a shot by accident.
4. The intake toggle should sit above the intake hold.

## Decision

| Who | Button | Now does | Was |
|---|---|---|---|
| Driver, gamepad 1 | Right bumper (hold) | Aim at the middle of our hive | Slow mode |
| Driver | Left bumper (hold) | Slow mode | Aim |
| Driver | D-pad left / right (hold, while aiming) | Arc around the hive, keeping the distance | Strip colors (green / clear) |
| Driver | D-pad up / down | Nothing | Strip colors (red / blue) |
| Operator, gamepad 2 | Right bumper | Nothing | Launchers off / on |
| Operator | Left bumper | Intake and transfer on / off | Nothing |
| Operator | Cross | Nothing | Intake and transfer on / off |

Unchanged: the launch trigger still aims on its own (0036), the left trigger still drifts
(0032), the operator's left trigger still holds the rollers on and square still reverses.

**The arc.** While `Drive_Manual` is aiming (right bumper or launch trigger, and a tag of
ours in view), D-pad left strafes left and D-pad right strafes right at `arcStrafePower`
(0.4). Aim keeps the nose on the hive. The distance to the tag the loop the arc started is
remembered, and a little drive holds it: `AimAssist.HoldRange`, `arcRangeKP` (0.02 power per
inch) at most `arcMaxDrive` (0.4), forward when too far, backward when too near. The three
together walk the robot around the hive in a circle. The strafe and the hold are
robot-centric, like drift assist, so field-centric driving does not turn them. Let go and
the next arc measures its own distance. All three numbers are on the dashboard's Drive
group. Without the aim button, or with no tag in view, the D-pad does nothing.

**Always on.** `Launcher` has no spin button. HyDrive, the autos and Test: Launcher call
`SpinLaunchers(true)` at START and nothing turns them off before STOP.

**The strip reads no gamepad.** `LightStrip` has no gamepad at all now. The underglow and
the pollen yellow run by themselves. Test: Light Strip still talks to the Prism driver
directly.

## CHECK on the robot

1. Hold the right bumper with a hive tag in view: the robot turns to face the hive. Hold the
   left bumper and push the stick: a quarter of the speed.
2. Hold the right bumper and D-pad left: the robot circles the hive to its left, nose on the
   hive. The Driver Station's `Arc` line shows the distance it is holding and the distance
   now. If the circle spirals out, raise `arcRangeKP`; if it hunts in and out, lower it; if
   it is too fast or too slow, change `arcStrafePower`. D-pad right circles the other way.
3. D-pad with the bumper up: nothing moves and the strip does not change.
4. START HyDrive: both launchers spin with nothing pressed. Gamepad 2's right bumper does
   nothing. STOP stops them.
5. Gamepad 2 left bumper: rollers on; again: off. Cross does nothing.

## Consequences

- A launcher stopped for spinning the wrong way (0029) stays stopped until the OpMode is
  restarted. There is no button to try again mid-match.
- The hard clear of the strip from D-pad left (0036's diagnostic) is gone. INIT clears and
  redraws every layer, and pollen coming or going does a full clear too.
- Free now: gamepad 1 cross, square, triangle, D-pad up / down; gamepad 2 right bumper,
  right trigger, cross, circle, both sticks.
- Tests: `DriveAimTest` (the arc, 8), `AimAssistTest` (hold range, 3),
  `DrivePracticeOptionsTest` (slow on the left bumper), `LauncherTest` (no off button),
  `IntakeTest` (the left bumper), `LightStripTest` (no buttons).
