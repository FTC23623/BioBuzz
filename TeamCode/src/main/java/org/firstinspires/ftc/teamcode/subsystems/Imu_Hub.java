package org.firstinspires.ftc.teamcode.subsystems;

import com.qualcomm.hardware.rev.RevHubOrientationOnRobot;
import com.qualcomm.robotcore.hardware.IMU;
import org.firstinspires.ftc.robotcore.external.navigation.AngleUnit;
import org.firstinspires.ftc.teamcode.objects.HydraOpMode;
import org.firstinspires.ftc.teamcode.types.Constants;

/**
 * Heading from the IMU built into the REV Control Hub.
 *
 * <p>Used when the robot has no Pinpoint (see {@link ImuSelector}). Ported from Decode
 * subsystems/Imu_Hub.java, which PORT-PLAN had marked to drop; it came back because the
 * test robot has no Pinpoint right now (docs/decisions/0005).
 *
 * <p>The hub must be told how it is mounted, or "turning left" can read as tilting. The
 * mounting is in Constants.hubLogoFacing and Constants.hubUsbFacing.
 */
public class Imu_Hub implements Imu {
    private final HydraOpMode mOp;
    private final IMU mImu;
    private boolean mTried;
    private boolean mReady;
    /** Heading read this loop, so every reader in the loop sees the same number. */
    private double mYawDeg;

    public Imu_Hub(HydraOpMode op) {
        mOp = op;
        mImu = op.mHardwareMap.get(IMU.class, Constants.cfgHubImu);
        mTried = false;
        mReady = false;
        mYawDeg = 0;
    }

    /**
     * Tell the IMU how the hub is mounted and zero the heading. Tried once. If it fails,
     * INIT still finishes: the drive works, just robot-centric (heading stays 0).
     */
    @Override
    public boolean Init() {
        if (!mTried) {
            mTried = true;
            mReady = mImu.initialize(new IMU.Parameters(new RevHubOrientationOnRobot(
                    Constants.hubLogoFacing, Constants.hubUsbFacing)));
            if (mReady) {
                mImu.resetYaw();
                mYawDeg = 0;
            }
        }
        mOp.mTelemetry.addLine(mReady ? "Hub IMU ready"
                : "Hub IMU FAULT: did not start. Field-centric will act robot-centric.");
        return true;
    }

    @Override
    public void HandleUserInput() {
    }

    /** Read the IMU once per loop. The hub IMU is not part of the bulk read, so each read costs time. */
    @Override
    public void Process() {
        if (mReady) {
            mYawDeg = mImu.getRobotYawPitchRollAngles().getYaw(AngleUnit.DEGREES);
        }
    }

    @Override
    public boolean Ready() {
        return mReady;
    }

    @Override
    public double GetYaw() {
        return mYawDeg;
    }

    @Override
    public void ResetYaw() {
        mImu.resetYaw();
        mYawDeg = 0;
    }
}
