# 0019: Test: Motor Tuning spins the intake, transfer and both launchers

Status: Accepted. Not yet tried on the robot. Directions and intake ticks per turn are CHECK. Shared targets replaced by 0023. 0037: the rollers are 1150 RPM motors, 145.1 ticks per turn, not the launchers' 28.
Date: 2026-10-29

## Context

The robot now has four mechanism motors: `intake`, `transfer`, `pollenLauncher` and
`nectarLauncher`. The coach wants one TeleOp to find good speeds for them. For now, the
two launchers share one RPM, and the intake and transfer share another.

## Decision

- **`subsystems/MotorTuner`** owns all four motors. The Bot builds it only when
  `motorTunerDisabled` is false (it is true by default).
- **`Test: Motor Tuning`** turns the tuner on and turns off the drive, the launcher (it
  shares the nectar motor) and the LED strip (it shares the D-pad).
- **Controls, gamepad 1.** Each shape button turns its motor on, and the next press turns it off:
  triangle/Y pollen launcher, circle/B nectar launcher, cross/A intake, square/X transfer.
  D-pad up/down changes the launcher target by `launcherRpmStep` (50). D-pad right/left
  changes the intake target by `intakeRpmStep` (25; 50 since 0023).
- **Launchers use our `VelocityController`** with the nectar launcher's numbers, the same
  steps as `Launcher` (0009). A speed found here means the same thing there.
- **Intake and transfer use the hub's speed control** (`RUN_USING_ENCODER` +
  `setVelocity`), option B in 0009. There are no tuned gains for these motors yet.
- **Wrong-way stop.** Our speed control never pushes backward. A launcher set the wrong
  way would read a negative RPM and climb to full power. Below -100 RPM, that launcher stops
  until restart, and the screen says to fix its direction. (Changed by 0029: only after a
  second backward while powered, and off-and-on clears it.)

## CHECK on the robot

1. **`pollenLauncherReversed`**: if the screen says the pollen launcher spins the wrong way, flip it.
   **`nectarLauncherReversed`**: added 2026-10-30.
   **2026-10-31: both launcher settings now mean what they say**, like `intakeReversed`:
   true = the motor is set REVERSE, false = FORWARD. (Before, false meant REVERSE, carried
   over from Decode, so "not reversed" pollen was really reversed.) Pollen false (FORWARD,
   the normal way), nectar true (REVERSE, mounted the other way on this bot).
2. **`intakeReversed` / `transferReversed`**: the intake should pull in and the transfer carry up.
3. **`intakeTicksPerRev`**: settled 2026-10-30. The intake and transfer are the same motor
   as the launchers, so 28. (It was 537.7, a goBILDA 312 RPM guess. That made a screen
   "150 RPM" really about 2900, so the starting speeds became 2900 to keep the same feel.)
4. **Encoder cables.** Plugged in on the intake and transfer (2026-10-30). The hub's speed
   control needs them; without them the motor runs at full power.

## Consequences

- Splitting the targets later (one per launcher, or intake apart from transfer) only
  touches `MotorTuner`.
- When the real intake and launcher subsystems exist, the speeds found here go into their
  BotProperties settings.
