package org.firstinspires.ftc.teamcode.subsystems;

import com.qualcomm.robotcore.hardware.TouchSensor;
import com.qualcomm.robotcore.util.ElapsedTime;
import org.firstinspires.ftc.teamcode.bot.BotProperties;
import org.firstinspires.ftc.teamcode.objects.HydraOpMode;
import org.firstinspires.ftc.teamcode.objects.Subsystem;
import org.firstinspires.ftc.teamcode.types.TelemetryGroup;

/**
 * The robot's ball switches: {@code pollenSensor1}..{@code pollenSensor4} and
 * {@code nectorSensor1}..{@code nectorSensor4} (REV Touch Sensors). Together they say whether
 * any ball is left in the robot, so an auto's LAUNCH_UNTIL_EMPTY can keep the launcher
 * running until the last one is gone (docs/decisions/0058).
 *
 * <p>Check the wiring with Test: Pollen and Nectar Sensors. {@code ballSensorsInverted}
 * flips every switch if a ball reads OPEN there.
 *
 * <p>A switch missing from the robot configuration is ignored. If none are found,
 * {@link #AnyFound()} is false and nothing waits on them.
 */
public class BallSensors implements Subsystem {
    private final HydraOpMode mOp;
    private final BotProperties mProperties;
    private final TouchSensor[] mPollen;
    private final TouchSensor[] mNectar;
    /** Time since a ball was last seen on any switch. */
    private final ElapsedTime mSinceBall = new ElapsedTime();

    public BallSensors(HydraOpMode op, BotProperties properties) {
        mOp = op;
        mProperties = properties;
        mPollen = Find(op, properties.cfgPollenSensors, properties.ballSensorCount);
        mNectar = Find(op, properties.cfgNectarSensors, properties.ballSensorCount);
    }

    private static TouchSensor[] Find(HydraOpMode op, String prefix, int count) {
        TouchSensor[] found = new TouchSensor[count];
        for (int i = 0; i < count; i++) {
            found[i] = op.mHardwareMap.tryGet(TouchSensor.class, prefix + (i + 1));
        }
        return found;
    }

    /** Is a ball on this switch? Pressed means a ball, unless the switches are inverted. */
    static boolean BallOn(boolean pressed, boolean inverted) {
        return pressed != inverted;
    }

    /**
     * How long the robot has had no ball on any switch, in seconds. 0 while a ball is seen,
     * and 0 when no switch is in the configuration (so nothing can say "empty").
     */
    static double EmptySeconds(boolean anyFound, boolean anyBall, double sinceBallSeconds) {
        if (!anyFound || anyBall) {
            return 0;
        }
        return sinceBallSeconds;
    }

    /** true when at least one ball switch is in the robot configuration. */
    public boolean AnyFound() {
        for (TouchSensor sensor : mPollen) {
            if (sensor != null) {
                return true;
            }
        }
        for (TouchSensor sensor : mNectar) {
            if (sensor != null) {
                return true;
            }
        }
        return false;
    }

    private int CountBalls(TouchSensor[] sensors) {
        int balls = 0;
        for (TouchSensor sensor : sensors) {
            if (sensor != null && BallOn(sensor.isPressed(), mProperties.ballSensorsInverted)) {
                balls = balls + 1;
            }
        }
        return balls;
    }

    /** How many switches have a ball on them right now. */
    public int BallsSeen() {
        return CountBalls(mPollen) + CountBalls(mNectar);
    }

    /** Seconds with every switch empty, in a row. See {@link #EmptySeconds}. */
    public double EmptyForSeconds() {
        return EmptySeconds(AnyFound(), BallsSeen() > 0, mSinceBall.seconds());
    }

    /** One switch, for the Driver Station: "ball", "-" (empty) or "?" (not in the configuration). */
    private String Mark(TouchSensor sensor) {
        if (sensor == null) {
            return "?";
        }
        return BallOn(sensor.isPressed(), mProperties.ballSensorsInverted) ? "BALL" : "-";
    }

    private String Row(TouchSensor[] sensors) {
        StringBuilder row = new StringBuilder();
        for (int i = 0; i < sensors.length; i++) {
            row.append(i + 1).append(':').append(Mark(sensors[i])).append("  ");
        }
        return row.toString().trim();
    }

    /** Pollen and nectar switches on the Driver Station. */
    public void ShowStatus() {
        if (!AnyFound()) {
            mOp.mTelemetry.addLine("Ball sensors: none found (" + mProperties.cfgPollenSensors + "1.., "
                    + mProperties.cfgNectarSensors + "1..)");
            return;
        }
        mOp.mTelemetry.addData("Ball sensors, pollen", Row(mPollen));
        mOp.mTelemetry.addData("Ball sensors, nectar", Row(mNectar));
    }

    @Override
    public boolean Init() {
        return true;
    }

    @Override
    public void HandleUserInput() {
    }

    @Override
    public void Process() {
        if (BallsSeen() > 0) {
            mSinceBall.reset();
        }
        if (mOp.Shows(TelemetryGroup.INTAKE)) {
            ShowStatus();
        }
    }
}
