# 0053: A match build, and autos named Auto / Blue / Red

Status: Accepted, 2026-11-06.

## Context

The Driver Station lists 30 OpModes: the match autos and HyDrives, but also eight `Test:`,
two `Tool:`, three `Practice:`, the shape autos, Forward One Tile and StrategyD, which the
team isn't running. At a match that is a list to get wrong. The coach asked for a build
that leaves the test and tool OpModes out, and for the autos to be easier to find.

## Decision

- **Match build**: `./gradlew :TeamCode:installDebug -PmatchBuild` (or `assembleDebug`)
  leaves every class named in `TeamCode/match-build-excludes.txt` out of the app (a Gradle
  source exclude in `TeamCode/build.gradle`). Kept: the three match autos, their six color
  backups, HyDrive and its two backups (12 OpModes). Left out: every `Test:`, `Tool:`,
  `Practice:` and `Shape:` OpMode, Forward One Tile, and StrategyD with its backups (20).
  Without `-PmatchBuild` nothing changes. The unit tests need every OpMode, so a match build
  refuses to run them.
- **`MatchBuildTest`** reads the list: every name must be a real OpMode, every OpMode in
  the Test, Tool, Practice, Shapes or Basic group must be on it, and none of the 12 match
  OpModes may be. A new test OpMode not added to the list fails the tests.
- **Names and groups** (the Driver Station sorts by group, then name):

  | Group | OpModes |
  |---|---|
  | `Auto` | `Auto: Garden Side A`, `Auto: Flower Side B`, `Auto: Flower Side C` (read the magnet) |
  | `Blue` | `Blue: Garden Side A`, `Blue: Flower Side B`, `Blue: Flower Side C` |
  | `Red` | `Red: Garden Side A`, `Red: Flower Side B`, `Red: Flower Side C` |

  Each auto's `NAME` is the plain name ("Garden Side A"); the annotation adds the prefix.
  The TeleOp each one picks is unchanged (0046). The visualizer files are named after the
  plain name (`Garden Side A Blue.pp`).

## Update, 2026-11-06: Android Studio, and the match build sets the match settings

- **Android Studio**: `matchBuild` is an ordinary Gradle property, `false` in the project's
  `gradle.properties`. Set it `true` there and Run, or add `-PmatchBuild` under Settings >
  Build, Execution, Deployment > Compiler > Command-line Options. `-PmatchBuild` and
  `-PmatchBuild=true` are on; `false` is off.
- **`BuildConfig.MATCH_BUILD`**: the build tells the code which it is. INIT shows
  `Match build: ...` or `Full build: ...`.
- **`competitionMode = BuildConfig.MATCH_BUILD`**: a match build is always competition mode
  (no dashboard output, no live tuning, no Limelight relay); a normal build never is. This
  replaces editing the line by hand (it was held false until after Thursday's tuning).
- **`BotProperties.ApplyMatchBuild`**, called first in the Bot: a match build forces
  `testLog`, `testTelemetry`, `tuningMarks`, `launcherOnlyTelemetry`, `driveCoastWhenIdle`
  and `visionAnyTag` off. `quietTelemetry` stays as set (the coach's choice).
- The dashboard server itself is still switched off by hand (Robot Controller menu,
  Disable Dashboard): the library's own switch is private, and its `stop()` is for the
  app's lifecycle, not for an OpMode to call.

## Consequences

- Before an event: install the match build (`match-day-settings.md` B5,
  `event-checklist.md`). After: install the normal build to get the tools back.
- Tool: Launch Tuning (0052) is not in a match build, by design.
