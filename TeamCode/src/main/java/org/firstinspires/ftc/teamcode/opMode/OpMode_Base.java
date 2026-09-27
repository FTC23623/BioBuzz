package org.firstinspires.ftc.teamcode.opMode;

import com.qualcomm.hardware.lynx.LynxModule;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import java.util.ArrayList;
import java.util.List;
import org.firstinspires.ftc.teamcode.objects.HydraOpMode;
import org.firstinspires.ftc.teamcode.objects.Subsystem;

/**
 * Shared plumbing for every team OpMode.
 *
 * <p>Ported from Decode opMode/OpMode_Base.java. The Decode version took a red/blue
 * VisionMode in its constructor. That is gone: alliance comes from the badge switch, not
 * from which OpMode the driver picked (docs/decisions/0001).
 */
public abstract class OpMode_Base extends LinearOpMode {
    protected HydraOpMode mOpMode;
    protected List<LynxModule> mAllHubs;
    protected final List<Subsystem> mSystems = new ArrayList<>();

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
