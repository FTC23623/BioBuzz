# 0057: Nectar gets its own distance curve; autos can branch on "has the hive tipped?"

Status: Accepted, 2026-11-21. Not yet shot or driven.
Changes 0038 ("Pollen only. Nectar keeps its own fixed target"). Adds to 0047 and 0049.

## Context

After Hartland the coach asked for:

- nectar tuned with the operator's D-pad left / right, and nectar 50 RPM faster "through
  the curve". Until now nectar had no curve, just one fixed speed (3100);
- pollen 75 RPM slower through the curve (reverted the same day: pollen stays as it was);
- the intake roller running while an auto launches (it was off between FLOWER steps, 0043);
- a way for an auto to take a different route depending on whether the hive has tipped.
  Garden Side D exists only because A assumes it has.

## Decision

**Launcher speeds**

- Nectar follows its own four-shot curve, at the same four distances as pollen:
  `nectarNearRpm`, `nectarMid1Rpm`, `nectarMid2Rpm`, `nectarFarRpm`. It started as the
  pollen curve plus 50. Both launchers use the same distance step, so the steady-range
  hold (0048) moves them together.
- Pollen is unchanged. A 75 RPM cut was tried and reverted the same day (coach);
  `autoLaunchMinRpm` stays 2735.

| in | 110 | 120 | 130 | 140+ |
|---|---|---|---|---|
| pollen | 2760 | 2831 | 2983 | 3115 |
| nectar | 2810 | 2881 | 3033 | 3165 |

- **D-pad right / left** trims the nectar curve (`nectarRangeTrimRpm`), the way up / down
  trims pollen's (`launcherRangeTrimRpm`). With `launcherRangeTuning` off, both set the
  speed directly, as before.
- **Lock**: an open nectar gate holds the nectar speed (with `launchLocksRpm`). A pollen
  settle or an open pollen gate holds only pollen. Nectar still never waits to settle.
- The new numbers are on the dashboard's Launcher group.

**Auto intake while launching**

- Once a LAUNCH (or LAUNCH_WHEN_TAG_SEEN) opens the gates, the intake roller runs at
  `launchFeedIntakeScale` until the gates close, then goes back to what it was doing. It
  does not start during the spin-up wait, while the gates are still shut.

**Tipped branches** (`PedroAuto`)

- `CHECK_TIPPED`: hold still and look for at most `autoTipLookSeconds` (1 s). A tag of ours
  with its tilt in the tipped window (`tagTippedMinDeg` to `tagTippedMaxDeg`, the 0049
  test) means tipped. `autoTippedNeedsTilt = false` counts any tag of ours as tipped.
- `IF_TIPPED` ... `IF_NOT_TIPPED` ... `END_IF`: the steps under the answer that does not
  match are skipped. Either side may be left out. One level only, no IF inside an IF.
- Both sides must end where the robot stands at `END_IF`, because the next path starts there.
- `PedroAuto.NextStep` makes the choice. It has no hardware, so `TippedBranchTest` checks it
  on a laptop.

**Which side is up, and field-spot autos** (added 2026-11-24)

- `CHECK_TIPPED` now also remembers WHICH cell tipped, from the tipped tag's id:
  `gardenSideTags` ({34-37} red, {42-45} blue) and `flowerSideTags` ({30-33} red, {38-41}
  blue; coach, 2026-11-25: the first guess was the wrong way round) in
  BotProperties. CHECK both against Competition Manual section 9.9: the split is a guess.
- `IF_GARDEN_SIDE_UP` and `IF_FLOWER_SIDE_UP` join `IF_TIPPED` (either side) and
  `IF_NOT_TIPPED` (neither). Each IF step is asked by itself, and its steps run to the next
  IF step or `END_IF`. So don't mix `IF_TIPPED` with the side steps in one IF.
- A tipped tag on neither list is "tipped, side not known": `IF_TIPPED` runs, the side
  steps don't. Only the nearest tag is looked at.
- **A down cell means the other side is up** (`autoDownMeansOtherSideUp`, on). Coach,
  2026-11-24: from close and at an angle, coming off the flower by the garden, the camera
  sees the underside of a down cell. Measured there: the tag's roll is -34 with the cell up
  and 25 with it down. `CHECK_TIPPED` now reads the roll (`autoTipFromRoll`), not the pitch
  window TeleOp's always-track uses (0049, untouched):

  | Roll | Answer |
  |---|---|
  | -54 to -14 (`autoTagUpMinDeg`, `autoTagUpMaxDeg`) | this tag's cell is up |
  | -14 to 5 | the buffer: no answer |
  | 5 to 45 (`autoTagDownMinDeg`, `autoTagDownMaxDeg`) | this cell is down, so the OTHER side is up |

  Each window is the reading plus and minus 20. The Limelight's "Auto tip check" line
  shows the roll and the answer. CHECK the roll from every other spot an auto checks from,
  and with the hive level (nothing tipped yet): it must land in the buffer or outside both.
- **Swap the operator's launchers** (`operatorSwapsLaunchers`, off; 2026-11-25): on, the
  left bumper and trigger run nectar and the right ones pollen. Bumpers and triggers swap
  together, and "pollen wins when both are pressed" follows pollen to the right side. On the
  dashboard's Launcher group. Every reader of those buttons goes through `LaunchChoice`.
- **Pollen curve down 25** (coach, 2026-11-25): 2635, 2706, 2858, 2990 (was 2660, 2731,
  2883, 3015). Nectar's curve is unchanged. `autoLaunchMinRpm` moved with it, 2635 to 2610,
  so it stays at or below the near shot with the same margin.
- **FLOWER: intake only until the arm is out** (coach, 2026-11-25): back 7 and strokes of 9
  are unchanged (back 3 / strokes of 3, then back 6, were tried the same day and put back),
  so the robot reaches 2 inches past the recorded spot and ends 7 behind it. The intake (roller and
  transfer) turns on at the first back-up and stays on through the settle and the arm
  swinging out; from the first stroke it goes back to what the auto does between steps
  (INTAKE_ON / INTAKE_OFF, 0047). Before, it ran for the whole step.
- **Both launchers at once** (`launchBothTogether`, on; coach, 2026-11-25): both buttons held
  (triggers, or bumpers to aim) is a new `LaunchChoice.BOTH`. Before, pollen won and nectar
  waited for the pollen trigger to be let go. Now both gates open together, after the pollen
  settle (so nectar waits for that, as pollen does). The robot aims at the middle of the two
  launchers' aims (their opposite offsets mostly cancel) plus `bothAimOffsetDeg` (0): TUNE
  it by shooting both from a few distances. Let go of one: the other keeps going with no
  restart. Add one: the new gate gets its own opening wait. `launchBothTogether` off puts
  "pollen wins" back. The test log has `pollen_gate_open`, `nectar_gate_open` and
  `gates_fully_open`.
- **Nectar settles before it opens** (`teleopNectarSettles`, on; coach, 2026-11-25). The
  cause of the nectar gate seeming to have "already waited": there is ONE transfer, so a ball
  is already pressed against the closed nectar gate, and it left the moment the gate opened,
  with the nectar wheel not yet on its speed. Only pollen settled (0056); nectar now has its
  own curve (above), so it settles the same way: its target locks, and its gate waits for the
  wheel to stay within `launchSettleBandRpm` for `launchSettleHoldSeconds`, at most
  `launchSettleTimeoutSeconds`. With both triggers each gate waits for its own wheel, so one
  may open a moment before the other; the transfer's opening wait starts again for the
  second. Autos are unchanged: their LAUNCH still settles pollen only.
- `FieldAuto`: a `PedroAuto` whose spots are field spots from the Pedro Pathing Visualizer.
  It must give its start (`FieldStart()`). `Flower Side F` and `Garden Side G` are two,
  each a 12 inch back-up until a route is pasted in.

## Consequences

- One auto can do the work of A and D together. None is converted yet.
- The tilt window is still marked CHECK in BotProperties. A wrong window means "not tipped"
  every time, so the auto always takes the not-tipped side.
- Tool: Pose Recorder and the Visualizer export do not know the new markers yet. A branchy
  auto is written by hand.
