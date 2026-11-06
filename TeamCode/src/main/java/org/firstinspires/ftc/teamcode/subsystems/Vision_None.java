package org.firstinspires.ftc.teamcode.subsystems;

import org.firstinspires.ftc.teamcode.bot.BotProperties;
import org.firstinspires.ftc.teamcode.objects.HydraOpMode;

/**
 * The Vision for a robot with no camera. It never sees a tag, so aim assist does nothing,
 * and the drive still works. The Driver Station says why, every loop: a camera that is
 * plugged in but missing from the robot configuration looks exactly like no camera, and
 * the only clue is this line (docs/decisions/0035).
 */
public class Vision_None implements Vision {
    private final HydraOpMode mOp;
    private final BotProperties mProperties;

    public Vision_None(HydraOpMode op, BotProperties properties) {
        mOp = op;
        mProperties = properties;
    }

    @Override
    public boolean Init() {
        ShowStatus();
        return true;
    }

    @Override
    public void HandleUserInput() {
    }

    /** Nothing to see. Say so every loop, so a misnamed camera is noticed. */
    @Override
    public void Process() {
        ShowStatus();
    }

    private void ShowStatus() {
        mOp.mTelemetry.addLine("NO CAMERA FOUND: nothing named '" + mProperties.cfgLimelight
                + "' (a Limelight 3A) or '" + mProperties.cfgWebcam
                + "' (a Webcam) in the robot configuration. Aim assist is off, the light stays red.");
    }

    @Override
    public boolean SeesTag() {
        return false;
    }

    @Override
    public int GetTagId() {
        return 0;
    }

    @Override
    public double GetBearing() {
        return 0;
    }

    @Override
    public double GetRange() {
        return 0;
    }

    @Override
    public double GetGroupRange() {
        return 0;
    }

    @Override
    public double GetGroupBearing() {
        return 0;
    }

    /** This camera does not work out which side of the hive it is on: always 0, no offset. */
    @Override
    public double GetSideDegrees() {
        return 0;
    }

    @Override
    public int CountTags() {
        return 0;
    }

    @Override
    public int CountIgnoredTags() {
        return 0;
    }
}
