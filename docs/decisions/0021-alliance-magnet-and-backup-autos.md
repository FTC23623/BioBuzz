# 0021: Alliance from one magnet sensor; backup autos per color

Status: Accepted. Sensor not on the robot yet. LED numbers are CHECK. LED part (point 3) replaced by 0026. Practice switch visionAnyTag and the named ignored tags added by 0033.
Date: 2026-10-30
Replaces part of 0001 (the two badge switches, and "never times color").

## Context

0001 planned two limit switches pressed by the alliance badge. They were never built. The
first competition needs the alliance for one thing above all: aim assist and the autos
must only lock onto OUR hive. Competition Manual section 9.9 gives the tag ids:

| Hive | Tag ids |
|---|---|
| Red, far side | 30-33 |
| Red, audience side | 34-37 |
| Blue, audience side | 38-41 |
| Blue, far side | 42-45 |

## Decision

1. **One REV Magnetic Limit Switch**, configured as a REV Touch Sensor named
   `allianceMagnet`. Magnet sensed = RED. No magnet, or no sensor, = BLUE.
2. **Read every loop during INIT**, fixed at START (`subsystems/AllianceSensor`). The drive
   team can move the magnet and watch the result before the match.
3. **LED strip shows it.** During INIT the whole strip is the alliance color. From START
   on, LEDs `underglowStartLed` to `underglowStopLed` (12 to 24) stay that color as
   underglow. The alliance is on Prism layer 1; the D-pad demo colors are on layer 0 and
   only light the LEDs before the underglow.
4. **Vision drops the other alliance's tags** before anything looks at them
   (`BotProperties.HiveTags`). Aim assist, the indicator light and `LAUNCH_WHEN_TAG_SEEN`
   all see only our hive.
5. **Backup autos**, group "Backup": `Auto: StrategyA Red (backup)` and so on. Each sets
   `allianceOverride` and skips the sensor. Same paths both colors: the field is not
   mirrored for these strategies; only the hive changes. The normal autos read the magnet.

## Consequences

- An unplugged sensor reads BLUE, not UNKNOWN. A red team that loses the sensor gets blue
  aim. The Driver Station says "NO SENSOR" in that case; pick a backup auto.
- `stripLedCount` is now 24 and the underglow range is a guess. CHECK on the strip whether
  the Prism's stop LED is inclusive.
- 0001's "refuse to START with UNKNOWN" no longer applies: there is no UNKNOWN from the
  sensor.
