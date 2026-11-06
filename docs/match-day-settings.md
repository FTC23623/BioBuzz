# Match-day settings

Two checklists: **A** turns off everything that slows the loop down, **B** is what the
competition rules need. Do both before an event, build, deploy, then INIT HyDrive and read
the screen as in `event-checklist.md` section 2.

Every setting below is in `TeamCode/.../bot/BotProperties.java`. Search the file for the
name. Write the value into the file: a change made on FTC Dashboard is gone at STOP, and the
dashboard is off at an event anyway.

## A. Loop time: turn off what costs time

Each of these adds work to every loop. Set them as shown, top to bottom.

| # | Setting | Set to | What it costs when on |
|---|---|---|---|
| 1 | `testLog` | `false` | Writes a CSV line to the hub's storage every loop (0050). |
| 2 | `testTelemetry` | `false` | About 40 extra Driver Station lines every loop (0050). |
| 3 | `launcherOnlyTelemetry` | `false` | Launcher numbers every loop. Only for tuning the launchers (0041). |
| 4 | `quietTelemetry` | your choice | `true` is the fastest loop: after START the screen shows only loop time and battery, and the parts skip the hardware reads they make just for the screen. `false` shows the full screen. INIT shows everything either way (0041). |
| 5 | `competitionMode` | `true` | With it `false`, every telemetry line is also sent to FTC Dashboard over Wi-Fi, and the Limelight web-page relay runs (0022). |
| 6 | Robot Controller app menu: **Disable Dashboard** | disabled | The dashboard server itself. `competitionMode` stops our code sending to it; this stops the server. |
| 7 | `poseRecorderDisabled`, `motorTunerDisabled`, `launchServoTunerDisabled` | `true` (already) | Tools for the pit. Only their own OpModes turn them on. |

Check: start HyDrive, and on the screen `LoopTime average (ms)` should be as low as you have
seen it. INIT must **not** say `TEST LOG ON` or `TEST TELEMETRY ON`.

To measure loop time before an event, turn `quietTelemetry` on, watch `LoopTime average`,
then turn things back on one at a time to see what each costs.

## B. Match rules

| # | Setting | Set to | Why |
|---|---|---|---|
| 1 | `competitionMode` | `true` | Competition Manual R704.D: during a match only the Driver Station app may show telemetry; FTC Dashboard is prohibited. With it `true` our code sends nothing to the dashboard and offers no live tuning (0022). |
| 2 | Robot Controller app menu: **Disable Dashboard** | disabled | Same rule. INIT says `Competition mode: dashboard off.` only when both 1 and 2 are done; `DASHBOARD IS STILL ON` means do 2, then INIT again. |
| 3 | `visionAnyTag` | `false` | `true` aims at the other alliance's hive. INIT shouts `EVERY TAG COUNTS` while it is on. |
| 4 | `testLog`, `testTelemetry` | `false` | The log stays on the hub, which the rules allow, but it costs loop time and fills the storage. |
| 5 | OpModes | install the match build: `./gradlew :TeamCode:installDebug -PmatchBuild` | Only `Auto: Garden Side A`, `Auto: Flower Side B` / `C`, their `Blue:` / `Red:` backups and the three HyDrives are on the Driver Station; `Test:`, `Tool:`, `Practice:` and shape OpModes are left out (0053). A color backup only for a dead alliance sensor (0021). |
| 6 | Laptop | off the robot's Wi-Fi | So nothing but the Driver Station talks to the robot in a match. Check the Competition Manual's wireless rules for the exact wording. |

Then the rest of `event-checklist.md` section 1 (frame size, launch curve, Safe Mode).

## After the event

Install the normal build again (no `-PmatchBuild`) to get the test and tool OpModes back.
Put back what you use for practice: `competitionMode` `false`, Robot Controller menu
**Enable Dashboard**, and `testLog` / `testTelemetry` / `launcherOnlyTelemetry` as needed.
