package org.firstinspires.ftc.teamcode.subsystems;

import com.qualcomm.robotcore.hardware.Gamepad;
import com.qualcomm.robotcore.hardware.Servo;
import org.firstinspires.ftc.teamcode.bot.BotProperties;
import org.firstinspires.ftc.teamcode.objects.HydraOpMode;
import org.firstinspires.ftc.teamcode.objects.Subsystem;

/**
 * The flower extractor: a servo arm that swings out (deploys) to work the flower, and
 * swings back in (stores) out of the way.
 *
 * <p>Controls (operator, gamepad 2):
 * <ul>
 *   <li><b>Triangle / Y (hold)</b>: deployed. Let go: stored.
 * </ul>
 *
 * <p>Hold to deploy, like the launch gates: the moment the operator lets go it comes back
 * in, so it is never left out by mistake. The two spots are {@code flowerExtractorClosed}
 * (stored) and {@code flowerExtractorOpen} (deployed) in BotProperties, measured with
 * Test: Launch Servo Tuning (docs/decisions/0020).
 *
 * <p>A servo can't report where it is, it goes where it was last told. So nothing moves
 * during INIT. At START it goes to stored. A robot with no extractor servo in its
 * configuration still runs: the Driver Station says so at INIT.
 */
public class FlowerExtractor implements Subsystem {
    private final HydraOpMode mOp;
    private final BotProperties mProperties;
    private final Gamepad mGamepad;
    /** null when the servo is not in the robot configuration. */
    private final Servo mServo;

    private boolean mDeployHeld = false;

    public FlowerExtractor(HydraOpMode op, BotProperties properties, Gamepad gamepad) {
        mOp = op;
        mProperties = properties;
        mGamepad = gamepad;
        mServo = op.mHardwareMap.tryGet(Servo.class, properties.cfgFlowerExtractor);
    }

    @Override
    public boolean Init() {
        // No setPosition here: the servo must not move during INIT.
        if (mServo == null) {
            mOp.mTelemetry.addLine("Flower extractor: MISSING. Nothing named '"
                    + mProperties.cfgFlowerExtractor + "' in the robot configuration (a Servo).");
        } else {
            mOp.mTelemetry.addData("Flower extractor", "stored at START (%.3f). Gamepad 2: hold triangle to deploy",
                    mProperties.flowerExtractorClosed);
        }
        return true;
    }

    @Override
    public void HandleUserInput() {
        // Deployed only while held: no debounce, so it comes back the moment triangle is let go.
        mDeployHeld = mGamepad.triangle;
    }

    @Override
    public void Process() {
        String state = "stored";
        if (mDeployHeld) {
            SetPosition(mProperties.flowerExtractorOpen);
            state = "DEPLOYED";
        } else {
            SetPosition(mProperties.flowerExtractorClosed);
        }
        mOp.mTelemetry.addData("Flower extractor (gamepad 2: hold triangle)", state);
    }

    /** true: deployed. false: stored. An auto could call this. */
    public void SetDeployed(boolean deployed) {
        mDeployHeld = deployed;
    }

    public boolean IsDeployed() {
        return mDeployHeld;
    }

    private void SetPosition(double position) {
        if (mServo != null) {
            mServo.setPosition(position);
        }
    }
}
