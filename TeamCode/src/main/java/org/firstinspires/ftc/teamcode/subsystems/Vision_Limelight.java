package org.firstinspires.ftc.teamcode.subsystems;

import com.qualcomm.hardware.limelightvision.LLResult;
import com.qualcomm.hardware.limelightvision.LLResultTypes;
import com.qualcomm.hardware.limelightvision.Limelight3A;
import com.qualcomm.robotcore.util.ElapsedTime;
import java.util.List;
import org.firstinspires.ftc.robotcore.external.navigation.DistanceUnit;
import org.firstinspires.ftc.robotcore.external.navigation.Pose3D;
import org.firstinspires.ftc.robotcore.external.navigation.Position;
import org.firstinspires.ftc.teamcode.bot.BotProperties;
import org.firstinspires.ftc.teamcode.objects.HydraOpMode;

/**
 * AprilTags from a Limelight 3A, the camera Decode's robot carried.
 *
 * <p>The Limelight finds the tags itself and hands over a list. The pipeline it uses must
 * be an AprilTag pipeline, set up once in the Limelight's own web page
 * ({@code limelightAprilTagPipeline} in BotProperties says which number).
 *
 * <p><b>Nearest tag:</b> the tag that fills the most of the picture. Nearer tags look
 * bigger. <b>Bearing:</b> the Limelight counts right as positive ({@code tx}); we count
 * left as positive, like yaw, so the sign is flipped here. Ported from Decode
 * subsystems/LimelightVision.java, without the pipeline-per-alliance switch.
 */
public class Vision_Limelight implements Vision {
    private final HydraOpMode mOp;
    private final BotProperties mProperties;
    private final Limelight3A mLimelight;
    private final ElapsedTime mSinceSeen;

    private boolean mStarted = false;
    private boolean mEverSeen = false;
    private int mCount = 0;
    private int mTagId = 0;
    private double mBearing = 0;
    private double mRange = 0;
    private double mArea = 0;

    public Vision_Limelight(HydraOpMode op, BotProperties properties) {
        mOp = op;
        mProperties = properties;
        mLimelight = op.mHardwareMap.get(Limelight3A.class, properties.cfgLimelight);
        mSinceSeen = new ElapsedTime();
    }

    /** Start the Limelight once, on the AprilTag pipeline. */
    @Override
    public boolean Init() {
        if (!mStarted) {
            mStarted = true;
            mLimelight.setPollRateHz(mProperties.limelightPollRateHz);
            mLimelight.pipelineSwitch(mProperties.limelightAprilTagPipeline);
            mLimelight.start();
        }
        mOp.mTelemetry.addData("Camera", "Limelight, pipeline " + mProperties.limelightAprilTagPipeline);
        return true;
    }

    @Override
    public void HandleUserInput() {
    }

    /** Take the newest result and remember the nearest tag in it. */
    @Override
    public void Process() {
        mCount = 0;
        LLResult result = mLimelight.getLatestResult();
        if (result != null && result.isValid()) {
            List<LLResultTypes.FiducialResult> tags = result.getFiducialResults();
            mCount = tags.size();
            LLResultTypes.FiducialResult nearest = Biggest(tags);
            if (nearest != null) {
                mTagId = nearest.getFiducialId();
                mBearing = -nearest.getTargetXDegrees();
                mRange = RangeInches(nearest.getTargetPoseCameraSpace());
                mArea = nearest.getTargetArea();
                mEverSeen = true;
                mSinceSeen.reset();
            }
        }
        ShowStatus();
    }

    /** The tag that fills the most of the picture, or null when the list is empty. */
    static LLResultTypes.FiducialResult Biggest(List<LLResultTypes.FiducialResult> tags) {
        LLResultTypes.FiducialResult biggest = null;
        for (LLResultTypes.FiducialResult tag : tags) {
            if (biggest == null) {
                biggest = tag;
            } else if (tag.getTargetArea() > biggest.getTargetArea()) {
                biggest = tag;
            }
        }
        return biggest;
    }

    /**
     * Straight-line distance from the camera to the tag. The Limelight gives the tag's
     * position in meters; this turns it into inches.
     *
     * @return inches, or 0 when the Limelight did not work out a position
     */
    static double RangeInches(Pose3D tagPose) {
        if (tagPose == null) {
            return 0;
        }
        Position position = tagPose.getPosition();
        if (position == null || position.unit == null) {
            return 0;
        }
        Position inches = position.toUnit(DistanceUnit.INCH);
        double flat = inches.x * inches.x + inches.y * inches.y + inches.z * inches.z;
        return Math.sqrt(flat);
    }

    @Override
    public boolean SeesTag() {
        if (!mEverSeen) {
            return false;
        }
        return mSinceSeen.milliseconds() <= mProperties.visionMaxAgeMs;
    }

    @Override
    public int GetTagId() {
        if (!SeesTag()) {
            return 0;
        }
        return mTagId;
    }

    @Override
    public double GetBearing() {
        if (!SeesTag()) {
            return 0;
        }
        return mBearing;
    }

    @Override
    public double GetRange() {
        if (!SeesTag()) {
            return 0;
        }
        return mRange;
    }

    @Override
    public int CountTags() {
        return mCount;
    }

    private void ShowStatus() {
        mOp.mTelemetry.addData("Tags in view", mCount);
        if (SeesTag()) {
            mOp.mTelemetry.addData("Nearest tag", "id %d, %.1f deg to the %s, %.0f in away",
                    mTagId, Math.abs(mBearing), Side(mBearing), mRange);
            mOp.mTelemetry.addData("Tag fills", "%.1f%% of the picture", mArea);
            mOp.mTelemetry.addData("Tag last seen", "%.0f ms ago", mSinceSeen.milliseconds());
        } else {
            mOp.mTelemetry.addLine("Nearest tag: none");
        }
    }

    private static String Side(double bearing) {
        if (bearing >= 0) {
            return "left";
        }
        return "right";
    }
}
