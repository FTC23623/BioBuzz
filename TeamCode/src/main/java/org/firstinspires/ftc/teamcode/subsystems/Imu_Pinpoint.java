package org.firstinspires.ftc.teamcode.subsystems;

import com.qualcomm.hardware.gobilda.GoBildaPinpointDriver;
import org.firstinspires.ftc.robotcore.external.navigation.AngleUnit;
import org.firstinspires.ftc.robotcore.external.navigation.DistanceUnit;
import org.firstinspires.ftc.teamcode.objects.HydraOpMode;
import org.firstinspires.ftc.teamcode.types.Constants;

/**
 * Heading from the goBILDA Pinpoint odometry computer.
 *
 * <p>Uses the Pinpoint driver built into FTC SDK 12. Decode carried its own 1,088-line
 * copy of this driver; PORT-PLAN P4 said to try the built-in one first.
 *
 * <p>The robot must sit still for about a quarter second after INIT while the Pinpoint
 * calibrates its gyro. {@link #Init()} returns false until it is ready.
 */
public class Imu_Pinpoint implements Imu {
    private final HydraOpMode mOp;
    private final GoBildaPinpointDriver mPinpoint;
    private boolean mReady;
    /** Raw Pinpoint heading that counts as 0. Changed by {@link #ResetYaw()}. */
    private double mYawZeroDeg;
    /** Heading read this loop, so every reader in the loop sees the same number. */
    private double mYawDeg;

    public Imu_Pinpoint(HydraOpMode op) {
        mOp = op;
        mPinpoint = op.mHardwareMap.get(GoBildaPinpointDriver.class, Constants.cfgPinpoint);

        // Same setup as Decode's PinpointLocalizer constructor.
        double mmPerTick = Constants.pinpointInPerTick * 25.4;
        mPinpoint.setEncoderResolution(1 / mmPerTick, DistanceUnit.MM);
        mPinpoint.setOffsets(mmPerTick * Constants.pinpointParYTicks,
                mmPerTick * Constants.pinpointPerpXTicks, DistanceUnit.MM);
        mPinpoint.setEncoderDirections(GoBildaPinpointDriver.EncoderDirection.FORWARD,
                GoBildaPinpointDriver.EncoderDirection.FORWARD);
        mPinpoint.resetPosAndIMU();

        mReady = false;
        mYawZeroDeg = 0;
        mYawDeg = 0;
    }

    /**
     * Wait for the Pinpoint to finish calibrating. Kept from Decode Imu_Pinpoint.InitPinpoint:
     * pick the error check for the hardware version, and on version 2 read only the
     * registers we use.
     */
    @Override
    public boolean Init() {
        if (mReady) {
            return true;
        }
        mPinpoint.update();
        GoBildaPinpointDriver.DeviceStatus status = mPinpoint.getDeviceStatus();
        switch (status) {
            case READY:
                int version = mPinpoint.getDeviceVersion();
                if (version == 2) {
                    mPinpoint.setErrorDetectionType(GoBildaPinpointDriver.ErrorDetectionType.CRC);
                    mPinpoint.setBulkReadScope(
                            GoBildaPinpointDriver.Register.X_POSITION,
                            GoBildaPinpointDriver.Register.Y_POSITION,
                            GoBildaPinpointDriver.Register.H_ORIENTATION,
                            GoBildaPinpointDriver.Register.X_VELOCITY,
                            GoBildaPinpointDriver.Register.Y_VELOCITY,
                            GoBildaPinpointDriver.Register.H_VELOCITY,
                            GoBildaPinpointDriver.Register.DEVICE_STATUS);
                } else {
                    mPinpoint.setErrorDetectionType(GoBildaPinpointDriver.ErrorDetectionType.LOCAL_TEST);
                }
                mOp.mTelemetry.addData("Pinpoint version", version);
                mOp.mTelemetry.addLine("Pinpoint ready");
                mReady = true;
                break;
            case CALIBRATING:
            case NOT_READY:
                mOp.mTelemetry.addLine("Pinpoint calibrating. Keep the robot still.");
                break;
            default:
                // Any FAULT_ status: a pod is unplugged, the gyro ran away, or a read failed.
                mOp.mTelemetry.addData("Pinpoint FAULT", status);
                break;
        }
        return mReady;
    }

    @Override
    public void HandleUserInput() {
    }

    /** Read the Pinpoint once per loop. */
    @Override
    public void Process() {
        mPinpoint.update();
        mYawDeg = AngleUnit.normalizeDegrees(mPinpoint.getHeading(AngleUnit.DEGREES) - mYawZeroDeg);
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
        mYawZeroDeg = mPinpoint.getHeading(AngleUnit.DEGREES);
        mYawDeg = 0;
    }
}
