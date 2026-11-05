# 0046: The autos go straight on to TeleOp, and keep the heading

Status: Accepted, 2026-11-05. Not yet run on the field. The start facings are CHECK.

## Context

After the auto, the drivers had to find HyDrive in the list, and HyDrive zeroed the heading
at INIT. Field-centric "forward" then pointed whichever way the auto had left the robot,
not away from the driver, and nobody may touch the robot between the auto and TeleOp to
point it straight. The coach asked for three things:

1. Each auto picks its TeleOp when it ends, so the drivers only press INIT and START.
2. TeleOp keeps the heading the auto left.
3. The color-sensing autos list first, the color backups after, and A, B and C get names
   by field side.

## Decision

- **Names**: StrategyA is `Garden Side Auto A`, B is `Flower Side Auto B`, C is
  `Flower Side Auto C`. The backups add their color: `Garden Side Auto A Red (backup)` and
  so on. Each name is a `NAME` constant in the auto's class, and the backups build theirs
  from it. StrategyD is left as it is: the team is not running it.
- **Order**: the three color-sensing autos have no group. The Driver Station lists the
  no-group OpModes first, then the groups in ABC order, so "Backup" comes next.
- **The TeleOp after**: `preselectTeleOp` on each `@Autonomous`. The color-sensing autos
  pick `HyDrive`, which also reads the magnet. A Red backup picks `HyDrive Red (backup)`,
  a Blue one `HyDrive Blue (backup)`, so a dead sensor stays handled in TeleOp. The names
  come from the constants in `HyDrive`, so a typo can't break it.
- **Heading**: each auto says which way it starts facing, seen from the driver
  (`StartFacingDeg`: 0 away, 90 the driver's left, -90 the driver's right):

  | Auto | Start facing |
  |---|---|
  | Garden Side Auto A | -90, the driver's right |
  | Flower Side Auto B | 90, the driver's left |
  | Flower Side Auto C | 90, the driver's left |

  Every loop, the auto saves start facing + Pedro's heading in `objects/HeadingHandoff`.
  It is a static field, so it lives from one OpMode to the next in the same Robot
  Controller app. At INIT, once the heading is ready, TeleOp takes it and sets the yaw to
  it (`Imu.SetYaw`) instead of 0. It is taken only once.
- **Safety**: an auto clears any old heading at its INIT. A heading older than
  `headingHandoffMaxSeconds` (120) is not used, in case the robot was carried back in
  practice. `headingFromAuto` false turns it all off. Circle still re-sets forward.
- INIT says `Forward: kept from the auto. ...` or `Forward: the way the robot faces now`.

## CHECK on the field

1. Run Garden Side Auto A to the end. The Driver Station should show HyDrive ready. Press
   INIT: it should say `Forward: kept from the auto`. Press START and push the stick
   forward: the robot should drive away from the driver, whichever way it faces.
2. If it drives to the side or backward, that auto's start facing is wrong (or its sign
   is): fix `StartFacingDeg` in its class. Do the same for B and C.
3. A backup auto: check it picks the backup HyDrive of the same color.

## Consequences

- An auto with no start facing (StrategyD, the Shapes, Forward One Tile) hands nothing
  over: TeleOp zeroes the heading at INIT, as before.
- The Pinpoint is reset and re-calibrated at TeleOp INIT, so the robot must sit still for
  a moment, as always. It does: nobody touches it between auto and TeleOp.
- Tests: `HeadingHandoffTest`, `MatchOpModesTest` (names, groups, the TeleOp each picks,
  start facings), `BotTest` (INIT keeps the heading, or zeroes it), `ImuHubTest`
  (`SetYaw` on the hub and the Pinpoint).
