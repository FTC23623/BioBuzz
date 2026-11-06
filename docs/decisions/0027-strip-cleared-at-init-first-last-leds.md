# 0027: Strip cleared at INIT; LED ranges are first and last LED

Status: Accepted. Not yet tried on the strip. Prism stop counting is CHECK.
Date: 2026-10-31
Replaces the LED numbers in 0026 (underglow 12 to 24).

## Context

The Prism keeps showing whatever the last OpMode sent. The coach wants every INIT to start
from a dark strip, then the underglow on LEDs 12 to 35 (counted from 0, both ends lit) in
the sensed alliance color, then the pollen LEDs when the pollen switch is closed.

The old settings mixed two ways of counting the end of a run. `underglowStopLed = 24` was
"one past the last", but the pollen comments said "LEDs 0 to 3" for `Stop = 3`.

## Decision

1. **Clear first.** The first INIT loop clears all ten Prism layers, once. Then the
   alliance (layer 1) and, if the switch is already closed, the pollen LEDs (layers 2 and 3)
   are sent fresh.
2. **Every range is FIRST and LAST LED, both lit, counted from 0.** `underglowFirstLed = 12`,
   `underglowLastLed = 35`, `pollenLedsAFirst/ALast = 0/3`, `pollenLedsBFirst/BLast = 6/9`.
   `stripLedCount = 36`.
3. **One place turns LAST into the Prism's stop**: `LightStrip.PrismStop`. With
   `prismStopIsOnePastLast = true` (the guide's example), 12 to 35 is sent as start 12,
   stop 36.

## CHECK on the strip

1. INIT: the whole strip goes dark, then LEDs 12 to 35 light in the alliance color. Count:
   24 LEDs. If it is 23 (LED 35 dark), set `prismStopIsOnePastLast = false`.
2. Close the pollen switch: LEDs 0 to 3 and 6 to 9 go yellow (4 each). Open it: dark.
3. Press START: nothing on the strip moves.

## Consequences

- The pollen LEDs now include LED 3 and LED 9. Before, with "one past the last" counting,
  they lit 0 to 2 and 6 to 8.
- A new LED signal: pick a free layer (4 to 9) and a first/last pair in BotProperties.
