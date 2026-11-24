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

## Consequences

- One auto can do the work of A and D together. None is converted yet.
- The tilt window is still marked CHECK in BotProperties. A wrong window means "not tipped"
  every time, so the auto always takes the not-tipped side.
- Tool: Pose Recorder and the Visualizer export do not know the new markers yet. A branchy
  auto is written by hand.
