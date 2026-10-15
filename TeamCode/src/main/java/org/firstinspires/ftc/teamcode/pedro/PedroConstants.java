package org.firstinspires.ftc.teamcode.pedro;

import com.pedropathing.algorithm.Foresight;
import com.pedropathing.algorithm.ForesightConfig;
import com.pedropathing.follower.Follower;
import com.pedropathing.revhub.drivetrains.Mecanum;
import com.pedropathing.revhub.drivetrains.MecanumConfig;
import com.pedropathing.revhub.localizers.Encoder;
import com.pedropathing.revhub.localizers.RevHubIMU;
import com.pedropathing.revhub.localizers.TwoWheelConfig;
import com.pedropathing.revhub.localizers.TwoWheelLocalizer;
import com.qualcomm.hardware.rev.RevHubOrientationOnRobot;
import com.qualcomm.robotcore.hardware.DcMotorSimple;
import com.qualcomm.robotcore.hardware.HardwareMap;
import org.firstinspires.ftc.teamcode.bot.BotProperties;

// Pedro's AutoTune prints Foresight settings that use these three. They are imported now so
// the pasted lines compile without hunting for imports.
import com.pedropathing.controllers.Controller;
import com.pedropathing.math.Matrix;
import com.pedropathing.math.Vector2D;

/**
 * Pedro Pathing's settings for this robot, and the one place that builds a {@link Follower}.
 *
 * <p>Pedro needs three things (docs/decisions/0008):
 * <ol>
 *   <li><b>Drivetrain</b>: which motors, and which way they spin. {@link #DriveConfig}
 *   <li><b>Localizer</b>: how the robot knows where it is. Two odometry pods plus the Control
 *       Hub IMU. {@link #LocalizerConfig}
 *   <li><b>Foresight</b>: how hard to push to stay on the path and stop on time. These numbers
 *       only come from running AutoTune on the real robot. {@link #ForesightConfig()}
 * </ol>
 *
 * <p>Tuning order (AutoTune: join the robot's Wi-Fi, open http://192.168.43.1:10158):
 * Mecanum Tuner, then Two Wheel Tuner, then Foresight Tuner. After each one, copy the lines
 * from AutoTune's Java tab into the matching method below. They all start with {@code c.},
 * which is why the settings variable below is named {@code c}.
 *
 * <p>The motor names, pod ports, and hub mounting are the robot's, not Pedro's, so they come
 * from {@link BotProperties}. Only Pedro's own numbers live in this file.
 */
public final class PedroConstants {
    private PedroConstants() {
    }

    /**
     * false until the Foresight Tuner has been run and its numbers pasted into
     * {@link #ForesightConfig()}. The shape autos refuse to move while this is false.
     */
    public static final boolean foresightTuned = false;

    /** The four drive motors. Names and directions match {@code subsystems/Drive.java}. */
    public static MecanumConfig DriveConfig(BotProperties properties) {
        // Pedro wants its settings handed over inside a "settings -> { }" block. We give it an
        // empty one, then fill the settings in one line at a time.
        MecanumConfig c = new MecanumConfig(settings -> { });
        c.frontLeftName.set(properties.cfgDriveFrontLeft);
        c.frontRightName.set(properties.cfgDriveFrontRight);
        c.backLeftName.set(properties.cfgDriveBackLeft);
        c.backRightName.set(properties.cfgDriveBackRight);
        // Left side forward, right side reverse: the same as Drive.java. The Mecanum Tuner
        // checks these; if it disagrees, fix Drive.java too.
        c.frontLeftDirection.set(DcMotorSimple.Direction.FORWARD);
        c.backLeftDirection.set(DcMotorSimple.Direction.FORWARD);
        c.frontRightDirection.set(DcMotorSimple.Direction.REVERSE);
        c.backRightDirection.set(DcMotorSimple.Direction.REVERSE);
        return c;
    }

    /**
     * Two odometry pods plugged into hub encoder jacks, plus the Control Hub IMU for heading.
     *
     * <p>Until the Two Wheel Tuner has been run, these are starting guesses. The inches per
     * tick is Decode's measured value for these same pods. Offsets 0 and FORWARD directions
     * are placeholders.
     */
    public static TwoWheelConfig LocalizerConfig(BotProperties properties) {
        TwoWheelConfig c = new TwoWheelConfig(settings -> { });
        c.xPodName.set(properties.cfgForwardPodPort);
        c.yPodName.set(properties.cfgStrafePodPort);
        c.imuName.set(properties.cfgHubImu);
        c.forwardTicksToInches.set(properties.pinpointInPerTick);
        c.strafeTicksToInches.set(properties.pinpointInPerTick);
        c.xPodDirection.set(Encoder.FORWARD);
        c.yPodDirection.set(Encoder.FORWARD);
        c.xPodOffset.set(0.0);
        c.yPodOffset.set(0.0);
        RevHubOrientationOnRobot hubMounting =
                new RevHubOrientationOnRobot(properties.hubLogoFacing, properties.hubUsbFacing);
        c.imu.set(new RevHubIMU(hubMounting));
        return c;
    }

    /**
     * How Pedro follows a path. Empty until the Foresight Tuner has been run: paste the
     * {@code c.} lines from AutoTune's Java tab here, then set {@link #foresightTuned} to true.
     */
    public static ForesightConfig ForesightConfig() {
        ForesightConfig c = new ForesightConfig(settings -> { });
        // PASTE the Foresight Tuner's lines here.
        return c;
    }

    /**
     * Build a Follower: the part of Pedro that drives the robot along a path. OpModes get
     * theirs from {@code Bot.CreateFollower()}, which fills these in.
     *
     * @param properties the robot's settings: motor names, pod ports, hub mounting
     * @param safeMode true: every drive power is limited to the Safe Mode speed (decision 0004)
     */
    public static Follower CreateFollower(HardwareMap hardwareMap, BotProperties properties,
                                          boolean safeMode) {
        TwoWheelLocalizer localizer = new TwoWheelLocalizer(hardwareMap, LocalizerConfig(properties));
        Mecanum drivetrain = new SafeModeMecanum(hardwareMap, DriveConfig(properties), safeMode);
        Foresight algorithm = new Foresight(ForesightConfig());
        return new Follower(localizer, drivetrain, algorithm);
    }
}
