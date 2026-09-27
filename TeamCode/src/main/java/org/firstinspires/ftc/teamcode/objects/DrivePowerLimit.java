package org.firstinspires.ftc.teamcode.objects;

import org.firstinspires.ftc.teamcode.types.Constants;

/**
 * The Safe Mode speed cap for drive motors. Every drive motor power goes through here.
 *
 * <p>In Safe Mode the power is scaled down to a quarter, so the stick still feels smooth
 * from 0 to full, and then clamped so nothing can get past the cap. Outside Safe Mode the
 * power passes through unchanged.
 */
public final class DrivePowerLimit {
    private DrivePowerLimit() {
    }

    /**
     * @param power the power the drive wants, normally -1 to 1
     * @param safeMode true to cap the power at Constants.safeModeMaxPower
     * @return the power to send to the motor. A broken number (NaN) becomes 0, stopped.
     */
    public static double Apply(double power, boolean safeMode) {
        if (Double.isNaN(power)) {
            return 0;
        }
        if (!safeMode) {
            return power;
        }
        double cap = Constants.safeModeMaxPower;
        return Math.max(-cap, Math.min(power * cap, cap));
    }
}
