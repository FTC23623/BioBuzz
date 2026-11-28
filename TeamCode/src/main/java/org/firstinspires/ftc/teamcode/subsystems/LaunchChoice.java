package org.firstinspires.ftc.teamcode.subsystems;

import com.qualcomm.robotcore.hardware.Gamepad;
import org.firstinspires.ftc.teamcode.bot.BotProperties;
import org.firstinspires.ftc.teamcode.objects.AimAssist;

/**
 * Which launcher the operator (gamepad 2) is aiming or launching with, read the same way by
 * the drive (aim), the gates (launch) and the launcher (speed from distance).
 *
 * <p>With {@code operatorSwapsLaunchers} on, left and right swap: the left bumper and
 * trigger run nectar, the right ones pollen. The list below is for it off.
 *
 * <ul>
 *   <li><b>Left bumper (hold)</b>: aim pollen. <b>Left trigger (hold)</b>: launch pollen.
 *   <li><b>Right bumper (hold)</b>: aim nectar. <b>Right trigger (hold)</b>: launch nectar.
 *   <li>With {@code launchTriggerAims} on, a launch trigger aims for its launcher too.
 *   <li>Both sides at once: <b>BOTH</b> launchers (aim between them, both gates open).
 *       With {@code launchBothTogether} off, <b>pollen wins</b> instead.
 * </ul>
 *
 * <p>Where the hive's middle should sit in the camera once aimed ({@link #AimAtDeg}) is the
 * side offset ({@code aimHiveBearingDeg}, by which side of the hive the robot stands, the
 * same for both) plus the launcher offset ({@link #LauncherOffsetDeg}: each launcher sits
 * 2 inches to one side of the camera, so pollen and nectar aim opposite ways). Nectar also
 * turns {@code nectarAimLeftDeg} (1) further left.
 */
public enum LaunchChoice {
    NONE,
    POLLEN,
    NECTAR,
    /** Both launchers at once (both buttons held, with launchBothTogether). */
    BOTH;

    /** true when this choice includes the pollen launcher. */
    public boolean UsesPollen() {
        return this == POLLEN || this == BOTH;
    }

    /** true when this choice includes the nectar launcher. */
    public boolean UsesNectar() {
        return this == NECTAR || this == BOTH;
    }

    /** Which launcher the operator is launching with: left trigger pollen, right trigger nectar. */
    public static LaunchChoice Launching(Gamepad operator, BotProperties p) {
        return Pick(PollenTrigger(operator, p) > p.trgBtnThresh, NectarTrigger(operator, p) > p.trgBtnThresh,
                p.launchBothTogether);
    }

    /** How far the pollen trigger is pulled, 0 to 1: the left one, or the right with the swap. */
    public static double PollenTrigger(Gamepad operator, BotProperties p) {
        return p.operatorSwapsLaunchers ? operator.right_trigger : operator.left_trigger;
    }

    /** How far the nectar trigger is pulled, 0 to 1: the right one, or the left with the swap. */
    public static double NectarTrigger(Gamepad operator, BotProperties p) {
        return p.operatorSwapsLaunchers ? operator.left_trigger : operator.right_trigger;
    }

    /** true while the pollen bumper is held: the left one, or the right with the swap. */
    public static boolean PollenBumper(Gamepad operator, BotProperties p) {
        return p.operatorSwapsLaunchers ? operator.right_bumper : operator.left_bumper;
    }

    /** true while the nectar bumper is held: the right one, or the left with the swap. */
    public static boolean NectarBumper(Gamepad operator, BotProperties p) {
        return p.operatorSwapsLaunchers ? operator.left_bumper : operator.right_bumper;
    }

    /**
     * Which launcher the robot should aim: the bumpers, and the launch triggers too with
     * {@code launchTriggerAims} on.
     */
    public static LaunchChoice Aiming(Gamepad operator, BotProperties p) {
        boolean pollen = PollenBumper(operator, p)
                || (p.launchTriggerAims && PollenTrigger(operator, p) > p.trgBtnThresh);
        boolean nectar = NectarBumper(operator, p)
                || (p.launchTriggerAims && NectarTrigger(operator, p) > p.trgBtnThresh);
        return Pick(pollen, nectar, p.launchBothTogether);
    }

    /** Both asked for: BOTH, or with {@code allowBoth} off, pollen wins. */
    static LaunchChoice Pick(boolean pollen, boolean nectar, boolean allowBoth) {
        if (pollen && nectar && allowBoth) {
            return BOTH;
        }
        if (pollen) {
            return POLLEN;
        }
        if (nectar) {
            return NECTAR;
        }
        return NONE;
    }

    /**
     * Where the hive's middle should sit in the camera once aimed for this launcher, in
     * degrees, left positive: the side offset plus the launcher offset. For nectar, minus
     * {@code nectarAimLeftDeg}: the hive sitting further right in the camera means the
     * robot has turned further left. Feed it to {@code AimAssist.AimError}.
     *
     * <pre>
     *     aimHiveBearingDeg 1, 2 in launchers, pollen left, 110 in:
     *     robot left of the hive:  pollen +1.0 + 1.04 = +2.04   nectar +1.0 - 1.04 = -0.04
     *     robot right of the hive: pollen -1.0 + 1.04 = +0.04   nectar -1.0 - 1.04 = -2.04
     * </pre>
     *
     * @param sideDeg which side of the hive the robot stands, left positive. 0 = not known.
     * @param rangeIn inches from the camera to the tag. 0 or less = not known.
     */
    public double AimAtDeg(double sideDeg, double rangeIn, BotProperties p) {
        if (this == BOTH) {
            // Between the two: with the launchers on opposite sides of the camera their
            // offsets mostly cancel, leaving the side offset and half of nectar's extra turn.
            return (POLLEN.AimAtDeg(sideDeg, rangeIn, p) + NECTAR.AimAtDeg(sideDeg, rangeIn, p)) / 2
                    + p.bothAimOffsetDeg;
        }
        double side = AimAssist.SideOffset(sideDeg, p.aimHiveBearingDeg, p.aimSideCenterDeg);
        double aimAt = side + LauncherOffsetDeg(rangeIn, p);
        if (this == NECTAR) {
            aimAt = aimAt - p.nectarAimLeftDeg;
        }
        return aimAt;
    }

    /**
     * The launcher offset, in degrees, left positive: where the hive should sit in the camera
     * so this launcher, {@code launcherSideOffsetIn} to one side of the camera, points at it.
     *
     * <pre>
     *     atan(2 / 110) = 1.04 deg     atan(2 / 145) = 0.79 deg
     *     pollen left of the camera: +, so the robot turns a little right. Nectar: -.
     * </pre>
     *
     * The camera's tilt cancels out: tx, the angle across the image, already shrinks with the
     * straight-line range the same way. No range: {@code launcherOffsetNoRangeDeg}. NONE
     * counts as pollen, as before.
     *
     * @param rangeIn inches from the camera to the tag. 0 or less = not known.
     */
    public double LauncherOffsetDeg(double rangeIn, BotProperties p) {
        double degrees;
        if (rangeIn > 0) {
            degrees = Math.toDegrees(Math.atan(p.launcherSideOffsetIn / rangeIn));
        } else {
            degrees = p.launcherOffsetNoRangeDeg;
        }
        boolean launcherOnLeft = p.pollenLeftOfCamera;
        if (this == NECTAR) {
            launcherOnLeft = !launcherOnLeft;
        }
        // A launcher on the left wants the hive a little left in the camera.
        return launcherOnLeft ? degrees : -degrees;
    }
}
