# 0031: The indicator blinks red for the other alliance's tags, and says why at INIT

Status: Accepted. Not yet tried on the robot.
Date: 2026-11-02
Adds to 0015 (the indicator light) and 0021 (vision drops the other alliance's tags).

## Context

Since 0021 the camera drops the other alliance's tags before anything looks at them. That
left the indicator light with one red for two different problems at INIT: the camera sees
nothing, or it sees tags but they are the other alliance's (the magnet is wrong, or the
robot is pointed at the wrong tags). The drive team could not tell which from the light, so
they could not confirm the robot was picking up the RIGHT tags before START.

Both INIT loops (HyDrive and every PedroAuto) already ran the light each loop through
`Bot.ShowStatus()`. Nothing in the loops changed.

## Decision

1. **`Vision.CountIgnoredTags()`**: how many tags in view are not ours. Both cameras already
   counted these for the Driver Station; now the count is on the interface. `Vision_None`
   says 0.
2. **Blinking red** = no tag of ours seen, but the other alliance's tags are in view. Same
   `indicatorRed` and `indicatorBlinkMs` as before; no new number. Solid red stays "no tag
   at all". Our tags still win: blue and green are unchanged, and launching (yellow) and
   waiting for a tag (orange) still come first.
3. **One Driver Station line every loop**, `Indicator light: <color>: <why>`, for example
   `BLINKING RED: 2 tags in view, but the other alliance's. We are BLUE. Check the alliance
   magnet.` With no light in the robot configuration the line says NOT CONFIGURED and what
   the color would have been, so an unplugged or misnamed light is noticed at INIT.

## CHECK on the robot

1. INIT HyDrive with no magnet (BLUE). Point the camera at blue tags (38-45): blinking blue,
   blue or green. Point it at red tags (30-37): BLINKING red, and the Driver Station line says
   to check the magnet. Cover the camera: solid red.
2. Put the magnet on (RED) and repeat: the colors swap sides.
3. INIT any auto: the same colors and the same line, under "Put the robot on its start spot".

## Consequences

- Blinking red and solid red differ only in time. The Driver Station line is the tie-break.
- The light's legend at INIT (`Indicator.Init()`) and the comments in BotProperties say
  "of our tags" now.
- Tests: `IndicatorTest` (13), `VisionLimelightTest` checks the ignored count.
