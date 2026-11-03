# 0029: The launcher wrong-way stop allows a short reverse

Status: Accepted. Not yet tried on the robot.
Date: 2026-10-31
Replaces the wrong-way stop in 0019.

## Context

Our speed control never pushes a launcher backward. A motor set the wrong way would read a
negative speed, and the control would keep adding power, up to full. So 0019 stopped a
launcher the first time it read below -100 RPM, for the rest of the OpMode.

On the robot that stop fired during normal running. It checked one 50 ms reading, even with
the launcher off, and never let go. A ball, or a wheel kicked while coasting, reads backward
for a moment. One reading like that stopped the launcher until restart.

## Decision

- **Only while powered.** A launcher that is off can be spun any way. Nothing is counted.
- **Only a long reverse.** A powered launcher must read below `launcherWrongWayRpm` (-100)
  without a break for `launcherWrongWaySeconds` (1.0 s) before it is stopped. Any forward
  reading starts the count over.
- **Not for good.** Turning the launcher off (operator's right bumper, or the tuner's
  button) clears the stop. On again tries again.
- The Driver Station shows "backward for 0.25 s (allowed 1.00)" while a reverse is being
  counted. Both settings are on the dashboard (Launcher group).

## Consequences

- A motor truly set the wrong way now runs backward at up to full power for about 1 s
  before it is stopped, instead of 50 ms. Keep hands clear of the wheels when testing a new
  direction. Lower `launcherWrongWaySeconds` if 1 s is too long.
- In an auto the launchers are never turned off, so a stop there lasts to the end.
