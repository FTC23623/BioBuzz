# 0051: Autos on the field, with the Pedro Pathing Visualizer

Status: Accepted, 2026-11-06. Laptop tools only; the robot runs no new code. The start
spots are the coach's description, CHECK on the field.

## Context

The coach wanted a top-down view of each auto on the field, and to try the path planner
other teams use. For Pedro Pathing that is the official Pedro Pathing Visualizer
(visualizer.pedropathing.com, github.com/Pedro-Pathing/Visualizer, Apache-2.0). It has a
BIOBUZZ field, saves `.pp` files (JSON, format 2.0.0) and exports Pedro Java.

Our autos measure every spot from their start (Tool: Pose Recorder works the same way), so
they need one more number to be drawn on the field: where they start.

## Decision

- **Field coordinates** (the visualizer's): inches from the bottom-left corner of its
  BIOBUZZ picture, x right, y up, 0 to 141.5 (the floor inside the walls); heading 0
  pointing right, left positive. The walls cover 1.25 in of each edge tile, so the seams
  are at 22.75, 46.75, 70.75, 94.75, 118.75. The field is the same for both alliances
  turned half a turn about the middle. `objects/FieldPose` (`ToField`, `FromField`,
  `OtherAlliance`).
- **Start spots** (`PedroAuto.FieldStartBlue()`, blue; red turned half a turn). Coach: front
  against the wall, directly in front of the cell, on the alliance side of the tile seam;
  the robot is 18 x 18, so its center is 9 in off the wall and 9 in from the seam:

  | Auto | Blue start (x, y, heading) |
  |---|---|
  | Garden Side Auto A | (55.75, 9, -90): bottom wall, east of the seam at 46.75 |
  | Flower Side Auto B | (55.75, 132.5, 90): top wall, east of the seam at 46.75 |
  | Flower Side Auto C | (55.75, 132.5, 90) |

  Which wall: B and C only stay on the field from the top wall, and from there their
  FLOWER stops land in front of the top and left wall cells. A faces the opposite way, the
  bottom wall. This matches the TeleOp hand-over (0046): the blue driver on the east wall.
  A was first put beside the bottom-wall cell (x 103.75); the coach moved it two tiles left
  the same day. From there its FLOWER stops land at the left and top wall cells too.
- **Our autos → visualizer**: `VisualizerExportTest` writes `TeamCode/build/visualizer/
  <auto> Blue.pp` and `... Red.pp` every test run, from the autos' own `BuildPaths()`.
  LAUNCH, LAUNCH WHEN TAG SEEN, FLOWER, INTAKE_ON and INTAKE_OFF go in as named waits.
  It also fails if any route leaves the field. `tools/PedroVisualizerFile` writes the JSON.
  Each path's turn is measured the way Pedro turns (summed in small steps), because Pedro
  reports headings 0 to 360 and "end less start" would turn some paths the long way.
- **Visualizer → our autos**: put a `.pp` file in `TeamCode/visualizer/` and run the tests;
  `VisualizerImportTest` writes `TeamCode/build/visualizer-import/<name>.java.txt`, a
  `BuildPaths()` body like the Pose Recorder's, spots measured from the start, plus the
  `FieldStartBlue()` line. Named waits become our steps (any case); other waits become a
  comment. Lines, curves (control points), constant, linear and tangential headings carry
  over; a piecewise heading becomes a constant to set by hand. `tools/PedroVisualizerImport`,
  with `tools/MiniJson` (the Android JSON classes don't run in laptop tests).
- **A page to share**: "Autos on the field", the three routes drawn on the BIOBUZZ field,
  either alliance (published 2026-11-06, regenerated from the export files by hand).

## CHECK

1. Open `Garden Side A Blue.pp` in the visualizer (BIOBUZZ field). The start should
   match where the robot is put on the field. If not, fix `FieldStartBlue()` in that auto.
2. Flower Side Auto B ends next to the red garden on the blue alliance. If that's not
   where it should go, the start spot or the recording is off.

## Consequences

- Nothing on the robot reads `FieldStartBlue()`; the autos still drive from wherever INIT is.
- Tests: `VisualizerExportTest` (files, routes on the field, field math, turn direction,
  file shape), `VisualizerImportTest` (round trip of Auto B, curves, step names, JSON).
