package org.firstinspires.ftc.teamcode.opMode;

import com.qualcomm.hardware.lynx.LynxModule;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import java.util.ArrayList;
import java.util.List;
import org.firstinspires.ftc.teamcode.objects.HydraOpMode;
import org.firstinspires.ftc.teamcode.objects.Subsystem;
import org.firstinspires.ftc.teamcode.subsystems.SafeModeKey;

/**
 * Shared plumbing for every team OpMode, TeleOp and Auto.
 *
 * <p>Ported from Decode opMode/OpMode_Base.java. The Decode version took a red/blue
 * VisionMode in its constructor. That is gone: alliance comes from the badge switch, not
 * from which OpMode the driver picked (docs/decisions/0001).
 *
 * <p>Every OpMode starts with {@link #CreateHydraOpMode()}. That also reads the Safe Mode
 * switch, so no OpMode can forget it (docs/decisions/0004).
 */
public abstract class OpMode_Base extends LinearOpMode {
    protected HydraOpMode mOpMode;
    protected SafeModeKey mSafeModeKey;
    protected List<LynxModule> mAllHubs;
    protected final List<Subsystem> mSystems = new ArrayList<>();

    /**
     * Build the context object every subsystem gets, and check the Safe Mode key.
     * Call this first in runOpMode(), before creating any subsystem. This is the only
     * time the key is read; see docs/decisions/0007.
     */
    protected void CreateHydraOpMode() {
        mOpMode = new HydraOpMode(telemetry, hardwareMap, gamepad1, gamepad2);
        mSafeModeKey = new SafeModeKey(mOpMode);
        mSystems.add(mSafeModeKey);
    }

    /**
     * Read every hub's sensors in one trip per loop instead of one trip per sensor.
     * Call {@link #ClearLynxHubCaches()} at the top of every loop after this.
     */
    protected void SetLynxHubsManual() {
        mAllHubs = mOpMode.mHardwareMap.getAll(LynxModule.class);
        for (LynxModule module : mAllHubs) {
            module.setBulkCachingMode(LynxModule.BulkCachingMode.MANUAL);
        }
    }

    /** Throw away last loop's readings so this loop reads fresh ones. */
    protected void ClearLynxHubCaches() {
        for (LynxModule module : mAllHubs) {
            module.clearBulkCache();
        }
    }

    /** Call Init() on every subsystem until all say ready, or START or STOP is pressed. */
    protected void InitializeAllSystems() {
        boolean init;
        do {
            init = true;
            ClearLynxHubCaches();
            for (Subsystem system : mSystems) {
                init &= system.Init();
            }
            mOpMode.mTelemetry.update();
            idle();
        } while (!init && !isStarted() && !isStopRequested());
    }
}
