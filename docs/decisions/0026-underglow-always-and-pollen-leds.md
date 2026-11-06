# 0026: Underglow shows the alliance all the time; pollen sensor lights LEDs yellow

Status: Accepted. Not yet tried on the strip. LED numbers replaced by 0027 (underglow 12 to 35). Pollen LEDs are the whole strip since 0033; since 0035 pollen clears every layer first and the switch reading is inverted.
Date: 2026-10-31
Replaces point 3 of 0021 (whole strip during INIT, underglow from START).

## Context

At START the code rewrote the alliance layer from the whole strip (0 to 24) to the
underglow (12 to 24). On the robot the LEDs jumped ("reindexed") at that moment. The coach
wants the underglow to match the alliance the whole time. A pollen sensor (a switch) is now
on the robot, and should light some LEDs yellow while it is pressed.

## Decision

- **Underglow only, from INIT on.** Layer 1 shows the alliance on `underglowStartLed` to
  `underglowStopLed` (12 to 24) from the first INIT loop. START sends nothing new. It is
  re-sent only if the alliance changes before START (the magnet moved).
- **`subsystems/PollenSensor`**, a switch configured as a "REV Touch Sensor" named
  `pollenSensor`. Missing sensor = never pressed, and the Driver Station says so.
- **Pollen LEDs.** While pressed, `pollenLedsAStart`-`pollenLedsAStop` (0 to 3) and
  `pollenLedsBStart`-`pollenLedsBStop` (6 to 9) are yellow (255, 200, 0). A Prism layer
  covers one run of LEDs, so each group has its own layer: 2 and 3. Let go: both layers
  clear. Sent only when the switch changes. Works before and after START.
- **The D-pad colors** stay on layer 0, LEDs 0 to the underglow.

## CHECK on the strip

1. INIT: LEDs 12 to 24 are the alliance color, the rest dark. Press START: nothing moves.
2. Press the pollen switch: 0 to 3 and 6 to 9 go yellow. Let go: dark.
3. If a D-pad color hides the yellow (or the other way round), the Prism draws layers in
   the other order from what we guessed. Swap the layer numbers.

## Consequences

- During INIT only the underglow shows the alliance, not the whole strip. Check it there.
- Another LED signal = another layer (4 to 9 are free) and a range in BotProperties.
