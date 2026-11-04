# 0042: A FLOWER step for the autos, recorded with circle

Status: Accepted. Not yet driven. Distances and the arm time are starting numbers.
Date: 2026-11-03
Adds to 0024 (the pose recorder) and 0030 (LAUNCH steps). Replaces the 9 / 3 inch flower plan from the Part B notes.

## Context

The autos need to work a flower: pull the extractor across it and collect what falls. The
coach wants it recorded like a LAUNCH: drive to the flower, press a button, and the auto does
the rest from there.

## Decision

- **`FLOWER`**, a step marker in `PedroAuto` like `LAUNCH`. From where the last path ended,
  with the intake running the whole time:
  1. back up `autoFlowerBackIn` (6 in),
  2. extractor out, hold still `autoExtractorSeconds` (0.5 s),
  3. forward then back `autoFlowerStrokeIn` (7 in), `autoFlowerPasses` times (2),
  4. extractor in, hold still `autoExtractorSeconds`.

  Every move is along the way the robot faces, so it ends 6 in behind the recorded spot,
  still facing the flower. Each move has a give-up time like any path. After the step the
  intake goes back to `autoRunScoring` (on, for today's autos).
- **Tool: Pose Recorder, circle / B**: save this spot and add `paths.add(FLOWER);` after it.
  Circle was free there: the recorder drives robot-centric, so it never resets the heading.
- `Bot.SetFlowerExtractor()` lets an auto move the arm.

## CHECK on the robot

1. Record a spot in front of a flower with circle, paste it into a copy of AutoShapeZ, run
   it at half speed. Back, arm out, four strokes, arm in. The Driver Station's `Flower`
   line counts the stages.
2. If the arm hits the flower on the way out, raise `autoFlowerBackIn`. If it misses the
   flower on the strokes, change `autoFlowerStrokeIn`. If the arm is still moving when the
   robot starts, raise `autoExtractorSeconds`.

## Consequences

- The routine is about 28 inches of driving plus 1 s of arm, roughly 3 to 4 s at full speed.
- The recorded spot is where the robot meets the flower, not where it ends.
- Tests: `FlowerStepTest` (5), `PoseRecorderTest` (circle, 2).

## Update 2026-11-03: back up 9, and stop before the arm goes out

On the robot the arm swung out before the robot had finished backing up. When Pedro ends a
path the robot can still be braking, and the arm stage held the robot wherever it was at
that moment. Now:

- **`autoFlowerBackIn` = 9** (was 6), for more room from the flower.
- After the back-up the robot **holds the planned backed-up spot** (not where it happened to
  be) for **`autoFlowerSettleSeconds` = 0.3**, and only then swings the arm out.
- The arm still has `autoExtractorSeconds` (0.5) before the first forward stroke starts.

The sequence is now: back 9, settle 0.3 s, arm out 0.5 s, forward 7, back 7, forward 7,
back 7, arm in 0.5 s. The robot ends 9 in behind the recorded spot. If the arm still goes out
early, raise `autoFlowerSettleSeconds`.


## Update 2026-11-03, later: strokes of 8, and a second at the flower

The coach asked for one inch further into the flower and another second of extraction.

- **`autoFlowerStrokeIn` = 8** (was 7). The robot reaches 1 in closer to the flower and
  still ends 9 in behind the recorded spot.
- **`autoFlowerDwellSeconds` = 1**: at the end of each forward stroke the robot holds still
  for a second. 0 leaves the hold out.
- The step is now a plain list of actions (`PedroAuto.FlowerActions`): back up, settle, arm
  out, forward, hold, back, forward, hold, back, arm in. The Driver Station's `Flower` line
  names the one running, like `5 of 10: at the flower`.
