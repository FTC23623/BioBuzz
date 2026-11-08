# 0056: Settle before launch, on the launch command

Status: Accepted. Not yet tried on the robot.
Adds to 0030 (auto launch waits for speed), 0038 (speed from distance) and 0050 (launch lock).

## Context

Since 0030's later update, an auto's LAUNCH opens at `autoLaunchMinRpm` (2800): a fixed floor,
reached for one reading, not tied to the target. TeleOp's trigger did not look at the
launcher at all. While driving, the distance keeps moving the target, so a wheel can be
mid-change when the gates open.

## Decision

The launch command itself (the driver's trigger, or an auto's LAUNCH) now:

1. **Locks** the pollen target: the distance stops moving it.
2. **Waits to settle**: the wheel within `launchSettleBandRpm` (40) of the locked target,
   without a break, for `launchSettleHoldSeconds` (0.15 s, three speed readings).
3. **Times out** after `launchSettleTimeoutSeconds` (1 s): the gates open anyway.
4. Opens the gates. With `launchLocksRpm` on, the target stays locked while they are open.

While driving, nothing changes: the distance still keeps the speed close, so most settles
are short.

- **TeleOp: on** (`teleopLaunchSettles = true`). Only after the camera lock (0040) allows
  the launch. Once per pull: the gates then stay open while the trigger is held. Letting
  go, or losing the lock while still settling, unlocks the target.
- **Autos: off** (`autoLaunchSettles = false`). On, a LAUNCH first waits for
  `autoLaunchMinRpm` as before, then settles.
- **Autos, `LAUNCH_WHEN_TAG_SEEN`: on** (`autoTagLaunchSettles = true`), even with
  `autoLaunchSettles` off. The tag sets a new, farther target right before the launch (Flower
  Side B launches from far), so the wheel can pass `autoLaunchMinRpm` well short of it.
  After the aim, it waits for `autoLaunchMinRpm`, then locks and settles.
- Only the pollen wheel is watched (nectar is not in this competition, 0023). A robot with
  no pollen wheel counts as settled at once.
- The logic is `objects/SpeedSettle`, tested in `SpeedSettleTest`. All five settings are
  on the dashboard's Launcher group.

## Consequences

- A TeleOp launch can take up to 1 s longer when the wheel is far off. The Driver Station
  shows `LOCKED, settling, -60 RPM off, 0.20 s` while it waits.
- With `launchWiggle` on, the wiggle can start before the gates open.
- If 0030's problem comes back (the wheel never holds 40 RPM), widen the band toward 50 or
  shorten the hold before turning it off.
