# 0007: The Safe Mode input is a magnetic key, checked once at INIT

Status: Accepted. Replaces the switch part of 0004. The magnetic key isn't built yet; a
touch sensor stands in for bench testing.
Date: 2026-09-27

## Context

0004 used a toggle switch read during INIT. The coach settled on something harder to get
wrong: a REV Magnetic Limit Switch on the robot and a 3D printed key with a magnet. Key
in = full speed. Key out = Safe Mode.

Two more things the coach decided:

- The key only goes in or comes out **before INIT**. Nobody touches it during a match.
- A sensor blip mid-match must never change the robot's speed. The **power switch** is
  the real kill switch, not Safe Mode.

For testing now, the robot has a touch sensor named `liftHomeSwitch`. Pressing it
pretends the key is out.

## Decision

- `SafeModeKey` reads the sensor **once**, when the OpMode is set up at INIT, and
  never again. It keeps the sensor as a local variable, so later code can't read it by
  accident. A test checks the sensor is read exactly once, even over 100 loops.
- The sensor is configured as a **REV Touch Sensor**, the way REV recommends for the
  magnetic switch. Both sensors then read the same way in code: `isPressed()`.
- `Constants.cfgSafeModeKey` names the sensor. `Constants.keyPressedMeansKeyIn` says what
  "pressed" means:

| Hardware | `cfgSafeModeKey` | `keyPressedMeansKeyIn` | Pressed means |
|---|---|---|---|
| Touch sensor (now, for testing) | `liftHomeSwitch` | `false` | key OUT (Safe Mode) |
| Magnetic switch + key (later) | the magnetic switch's name | `true` | key IN (full speed) |

- Missing from the configuration means SAFE, with a warning. Everything else in 0004
  stands: 25% limit, scale then clamp, applied in `Drive.LimitPower`.

## Consequences

- **The magnetic key is fail-safe.** An unplugged sensor or a lost key reads "not
  pressed", which is key OUT, which is Safe Mode.
- **The touch stand-in is NOT fail-safe.** Unplugged reads "not pressed", which it
  treats as key IN: full speed. The Driver Station says so during INIT. That's accepted
  for bench testing only; switch to the magnetic key before competition.
- To change the speed, you must stop the OpMode and press INIT again. Changing the key
  after INIT shows no change on the Driver Station, on purpose.
- Before a match: key in, then INIT, then check the Driver Station says `FULL SPEED`.
- `liftHomeSwitch` belongs to the lift. When the lift is ported, the key needs its own
  sensor and name.
