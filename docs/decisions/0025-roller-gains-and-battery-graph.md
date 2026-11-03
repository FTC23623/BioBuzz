# 0025: Intake and transfer gains on the dashboard; battery volts graph

Status: Accepted. Not yet tried on the robot. Starting gains are CHECK.
Date: 2026-10-31

## Context

The intake and transfer hold their speed with the hub's own speed control (0019). Its
P / I / D / F gains were only shown, never set, so they could not be tuned. The launcher's
gains were already live on the dashboard (0022). The coach also wants battery voltage on
the graphs.

## Decision

- **`rollerKP`, `rollerKI`, `rollerKD`, `rollerKF`** in BotProperties, shared by the
  intake and transfer. They are in `Bot.LiveTuningIntake`, so they show on the dashboard's
  Config tab under "Intake".
- **`Roller` sends them to the hub** at INIT and again only when one changes, since each
  message to the hub costs loop time.
- **Starting values 10 / 3 / 0 / 0**, the REV hub's usual defaults, so the motors behave
  the same until someone tunes them.
- **Telemetry** shows the gains in use and the gains the hub "started with" at INIT.
- **`Battery volts`** is a telemetry line every loop, so it graphs. It is read every
  `batteryReadMs` (250), not every loop: each read is an extra trip to the hub. With two
  hubs, the lowest reading is shown.

## CHECK on the robot

1. INIT, read "Intake hub started with P / I / D / F". If it is not 10 / 3 / 0 / 0, copy
   those numbers into the `roller` settings, so the starting point is the hub's own.

## Consequences

- Tune order: graph `Intake target RPM` against `Intake actual RPM`. Change kF first, then
  kP, then kI. Write the winner into BotProperties: dashboard changes are gone at STOP.
- The intake and transfer share one set of gains. Splitting them only touches `Roller` and
  BotProperties.
- Graph `Battery volts` while the launchers spin up. A weak battery shows as a dip there
  first, and explains a launcher that cannot reach its target.
