# 0022: Competition mode switch; live tuning on the dashboard at practice

Status: Accepted. competitionMode is false today.
Date: 2026-10-30
Replaces part of 0012 ("Not used: live-editable @Config settings").

## Context

Competition Manual 2026-27, rule R704:

- **R704.C**: programming laptops must be disconnected from the robot's Wi-Fi during a match.
- **R704.D**: only the Driver Station app may stream telemetry. "Additional
  logging/streaming services, such as those hosted by third party plugins and tools such as
  FTC Dashboard ... are prohibited. No continuous video stream is allowed."

At practice we want the opposite: drift, steering, launcher and intake numbers changed
from a laptop while the robot runs.

## Decision

1. **`BotProperties.competitionMode`**, locked (`static final`), like Safe Mode. When true:
   no telemetry is sent to the dashboard, no live tuning, the webcam does not stream to it,
   and the Limelight web-page relay does not start.
2. **The dashboard itself can only be turned off in the Robot Controller app** (menu,
   Disable Dashboard; it remembers). The library has no public switch. So the Driver
   Station shows one line at the top of INIT: practice mode, all clear, or "DASHBOARD IS
   STILL ON".
3. **Live tuning** at practice (`objects/DashboardTuning`). The Bot hands the dashboard the
   OpMode's own BotProperties object and a list of field names (`Bot.LiveTuningDrive`,
   `LiveTuningLauncher`, `LiveTuningIntake`). The dashboard's Config tab writes straight
   into those fields. Subsystems read the fields every loop, so a change counts at once.
4. **More numbers to graph.** Each launcher shows target, actual, error, power and the
   F / P / I parts of the power. Intake and transfer show target, actual, power and the
   hub's PIDF gains.

## Consequences

- Before an event: set `competitionMode = true`, deploy, and Disable Dashboard in the
  Robot Controller menu. After: set it back.
- A change on the dashboard is gone at STOP. Write the winner into BotProperties.
- To tune a new setting live, add its name to a `LiveTuning` list. A test checks every
  name is a real field.
