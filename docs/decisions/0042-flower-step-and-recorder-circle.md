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
