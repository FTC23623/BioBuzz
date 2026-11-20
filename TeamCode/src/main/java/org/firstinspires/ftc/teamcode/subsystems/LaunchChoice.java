package org.firstinspires.ftc.teamcode.subsystems;

import com.qualcomm.robotcore.hardware.Gamepad;
import org.firstinspires.ftc.teamcode.bot.BotProperties;
import org.firstinspires.ftc.teamcode.objects.AimAssist;

/**
 * Which launcher the operator (gamepad 2) is aiming or launching with, read the same way by
 * the drive (aim), the gates (launch) and the launcher (speed from distance).
 *
 * <ul>
 *   <li><b>Left bumper (hold)</b>: aim pollen. <b>Left trigger (hold)</b>: launch pollen.
 *   <li><b>Right bumper (hold)</b>: aim nectar. <b>Right trigger (hold)</b>: launch nectar.
 *   <li>With {@code launchTriggerAims} on, a launch trigger aims for its launcher too.
 *   <li>Both sides at once: <b>pollen wins</b>.
 * </ul>
 *
 * <p>Where the hive's middle should sit in the camera once aimed ({@link #AimAtDeg}) is the
 * side offset ({@code aimHiveBearingDeg}, by which side of the hive the robot stands, the
 * same for both) plus the launcher offset ({@link #LauncherOffsetDeg}: each launcher sits
 * 2 inches to one side of the camera, so pollen and nectar aim opposite ways).
 */
public enum LaunchChoice {
    NONE,
    POLLEN,
    NECTAR;

    /** Which launcher the operator is launching with: left trigger pollen, right trigger nectar. */
    public static LaunchChoice Launching(Gamepad operator, BotProperties p) {
        return Pick(operator.left_trigger > p.trgBtnThresh, operator.right_trigger > p.trgBtnThresh);
    }

    /**
     * Which launcher the robot should aim: the bumpers, and the launch triggers too with
     * {@code launchTriggerAims} on.
     */
    public static LaunchChoice Aiming(Gamepad operator, BotProperties p) {
        boolean pollen = operator.left_bumper
                || (p.launchTriggerAims && operator.left_trigger > p.trgBtnThresh);
        boolean nectar = operator.right_bumper
                || (p.launchTriggerAims && operator.right_trigger > p.trgBtnThresh);
        return Pick(pollen, nectar);
    }

    /** Pollen wins when both are asked for. */
    static LaunchChoice Pick(boolean pollen, boolean nectar) {
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
     * degrees, left positive: the side offset plus the launcher offset. Feed it to
     * {@code AimAssist.AimError}.
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
        double side = AimAssist.SideOffset(sideDeg, p.aimHiveBearingDeg, p.aimSideCenterDeg);
        return side + LauncherOffsetDeg(rangeIn, p);
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
