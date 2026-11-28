package org.firstinspires.ftc.teamcode.objects;

import com.qualcomm.robotcore.hardware.Gamepad;
import com.qualcomm.robotcore.hardware.HardwareMap;
import org.firstinspires.ftc.robotcore.external.Telemetry;
import org.firstinspires.ftc.teamcode.bot.BotProperties;
import org.firstinspires.ftc.teamcode.types.Alliance;
import org.firstinspires.ftc.teamcode.types.TelemetryGroup;

/**
 * Everything a subsystem needs from the OpMode, in one object.
 *
 * <p>Each subsystem gets this in its constructor. That way a subsystem never has to
 * know which OpMode it is running in.
 */
public class HydraOpMode {
    public final Telemetry mTelemetry;
    public final HardwareMap mHardwareMap;
    public final Gamepad mDriverGamepad;
    public final Gamepad mOperatorGamepad;

    /**
     * Which alliance we are on. Read from the alliance badge switch during INIT.
     * Stays UNKNOWN until that switch is built. Nothing in the drive depends on it yet.
     */
    public Alliance mAlliance;

    /**
     * true: drive motors are limited to BotProperties.safeModeMaxPower. Set once from the Safe
     * Mode key at INIT, then never changed. Starts true so that an OpMode that never checks
     * the key is slow, not fast. See docs/decisions/0007-safe-mode-key.md.
     */
    public boolean mSafeMode;

    /**
     * true: parts skip their Driver Station lines, and the hardware reads made only for
     * them, to save loop time. The Bot sets it from {@code quietTelemetry} after START and
     * clears it during INIT, so the INIT checks always show (docs/decisions/0041).
     */
    public boolean mQuiet;

    /**
     * The settings, for the telemetry group switches ({@code tlm...}). The Bot sets it when it
     * is built. null (a test with no Bot): every group is on.
     */
    public BotProperties mProperties;

    /**
     * Should this group's Driver Station lines show now? Not while the quiet settings hush
     * everything ({@link #mQuiet}), and not while the group's own switch is off.
     */
    public boolean Shows(TelemetryGroup group) {
        if (mQuiet) {
            return false;
        }
        return mProperties == null || mProperties.TelemetryOn(group);
    }

    /** How long the last loop took, in milliseconds. */
    public double mLoopTime;

    public HydraOpMode(Telemetry telemetry, HardwareMap hardwareMap,
                       Gamepad driverGamepad, Gamepad operatorGamepad) {
        mTelemetry = telemetry;
        mHardwareMap = hardwareMap;
        mDriverGamepad = driverGamepad;
        mOperatorGamepad = operatorGamepad;
        mAlliance = Alliance.UNKNOWN;
        mSafeMode = true;
        mLoopTime = 0;
    }
}
