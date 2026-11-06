package org.firstinspires.ftc.teamcode.subsystems;

import com.qualcomm.robotcore.hardware.Gamepad;
import com.qualcomm.robotcore.hardware.Servo;
import org.firstinspires.ftc.teamcode.bot.BotProperties;
import org.firstinspires.ftc.teamcode.objects.Debouncer;
import org.firstinspires.ftc.teamcode.objects.HydraOpMode;
import org.firstinspires.ftc.teamcode.objects.Subsystem;

/**
 * Turn the pollen and nectar launch servos a few degrees at a time, to find where each
 * one is closed and where it is open. The flower extractor servo is tuned here too.
 * Only Test: Launch Servo Tuning uses this.
 *
 * <p>Controls (gamepad 1). One action per press; holding does not repeat:
 * <ul>
 *   <li><b>Triangle / Y</b>: pollen servo open / closed test. Opens it if it isn't open,
 *       closes it if it is.
 *   <li><b>Circle / B</b>: nectar servo open / closed test, the same way.
 *   <li><b>Cross / A</b>: flower extractor open (deployed) / closed (stored) test.
 *   <li><b>D-pad up / down</b>: pollen servo +5 / -5 degrees.
 *   <li><b>D-pad right / left</b>: nectar servo +5 / -5 degrees.
 *   <li><b>Right / left bumper</b>: flower extractor +5 / -5 degrees.
 * </ul>
 *
 * <p>The screen shows each servo's position two ways: the number the code sends (0 to 1),
 * and the same spot in degrees. Write down the position number: that is what the launch
 * code will use. See docs/decisions/0020.
 *
 * <p>Safe range: each servo only moves between its closed and open spots in BotProperties
 * (launch servos 0 to 0.267, flower extractor 0 to 0.584), so it can't push past the end
 * of the mechanism. The bumpers may take the flower extractor 10 degrees past open, to try
 * a further deployed spot.
 *
 * <p>A servo can't report where it is. It goes where it was last told. So nothing moves
 * until START. Then all three servos go to closed.
 */
public class LaunchServoTuner implements Subsystem {
    private final HydraOpMode mOp;
    private final BotProperties mProperties;
    private final Gamepad mGamepad;
    private final Servo mPollenServo;
    private final Servo mNectarServo;
    private final Servo mExtractorServo;

    private final Debouncer mPollenUpButton;
    private final Debouncer mPollenDownButton;
    private final Debouncer mNectarUpButton;
    private final Debouncer mNectarDownButton;
    private final Debouncer mExtractorUpButton;
    private final Debouncer mExtractorDownButton;
    private final Debouncer mPollenOpenCloseButton;
    private final Debouncer mNectarOpenCloseButton;
    private final Debouncer mExtractorOpenCloseButton;

    // Kept in degrees, so 5 + 5 + 5 is exactly 15, with no rounding creeping in.
    private double mPollenDeg;
    private double mNectarDeg;
    private double mExtractorDeg;

    public LaunchServoTuner(HydraOpMode op, BotProperties properties, Gamepad gamepad) {
        mOp = op;
        mProperties = properties;
        mGamepad = gamepad;
        mPollenServo = op.mHardwareMap.get(Servo.class, properties.cfgPollenServo);
        mNectarServo = op.mHardwareMap.get(Servo.class, properties.cfgNectarServo);
        mExtractorServo = op.mHardwareMap.get(Servo.class, properties.cfgFlowerExtractor);

        // Every servo starts closed: the safe spot.
        mPollenDeg = PositionToDegrees(properties.pollenServoClosed);
        mNectarDeg = PositionToDegrees(properties.nectarServoClosed);
        mExtractorDeg = PositionToDegrees(properties.flowerExtractorClosed);

        mPollenUpButton = new Debouncer(properties.debounce);
        mPollenDownButton = new Debouncer(properties.debounce);
        mNectarUpButton = new Debouncer(properties.debounce);
        mNectarDownButton = new Debouncer(properties.debounce);
        mExtractorUpButton = new Debouncer(properties.debounce);
        mExtractorDownButton = new Debouncer(properties.debounce);
        mPollenOpenCloseButton = new Debouncer(properties.debounce);
        mNectarOpenCloseButton = new Debouncer(properties.debounce);
        mExtractorOpenCloseButton = new Debouncer(properties.debounce);
    }

    @Override
    public boolean Init() {
        // No setPosition here: the servos must not move during INIT.
        ShowStatus();
        return true;
    }

    @Override
    public void HandleUserInput() {
        // Open / closed test: jump straight to the other end of the safe range.
        if (Pressed(mPollenOpenCloseButton, mGamepad.triangle)) {
            mPollenDeg = OpenOrClose(GetPollenPosition(),
                    mProperties.pollenServoClosed, mProperties.pollenServoOpen);
        }
        if (Pressed(mNectarOpenCloseButton, mGamepad.circle)) {
            mNectarDeg = OpenOrClose(GetNectarPosition(),
                    mProperties.nectarServoClosed, mProperties.nectarServoOpen);
        }
        if (Pressed(mExtractorOpenCloseButton, mGamepad.cross)) {
            mExtractorDeg = OpenOrClose(GetExtractorPosition(),
                    mProperties.flowerExtractorClosed, mProperties.flowerExtractorOpen);
        }

        // Fine tuning: one small step per press.
        double step = mProperties.launchServoStepDeg;
        if (Pressed(mPollenUpButton, mGamepad.dpad_up)) {
            mPollenDeg = KeepPollenSafe(mPollenDeg + step);
        }
        if (Pressed(mPollenDownButton, mGamepad.dpad_down)) {
            mPollenDeg = KeepPollenSafe(mPollenDeg - step);
        }
        if (Pressed(mNectarUpButton, mGamepad.dpad_right)) {
            mNectarDeg = KeepNectarSafe(mNectarDeg + step);
        }
        if (Pressed(mNectarDownButton, mGamepad.dpad_left)) {
            mNectarDeg = KeepNectarSafe(mNectarDeg - step);
        }
        if (Pressed(mExtractorUpButton, mGamepad.right_bumper)) {
            mExtractorDeg = KeepExtractorSafe(mExtractorDeg + step);
        }
        if (Pressed(mExtractorDownButton, mGamepad.left_bumper)) {
            mExtractorDeg = KeepExtractorSafe(mExtractorDeg - step);
        }
    }

    /** true once per press: the button was held long enough, and this press is now used up. */
    private static boolean Pressed(Debouncer button, boolean down) {
        button.In(down);
        if (button.Out()) {
            button.Used();
            return true;
        }
        return false;
    }

    @Override
    public void Process() {
        mPollenServo.setPosition(GetPollenPosition());
        mNectarServo.setPosition(GetNectarPosition());
        mExtractorServo.setPosition(GetExtractorPosition());
        ShowStatus();
    }

    /** Open now? Close it. Anywhere else, even part way? Open it. Returns degrees. */
    private double OpenOrClose(double position, double closed, double open) {
        if (IsAt(position, open)) {
            return PositionToDegrees(closed);
        }
        return PositionToDegrees(open);
    }

    private double KeepPollenSafe(double degrees) {
        return KeepBetween(degrees, mProperties.pollenServoClosed, mProperties.pollenServoOpen);
    }

    private double KeepNectarSafe(double degrees) {
        return KeepBetween(degrees, mProperties.nectarServoClosed, mProperties.nectarServoOpen);
    }

    /** Closed to open, plus a little extra past open so a further deployed spot can be tried. */
    private double KeepExtractorSafe(double degrees) {
        return KeepBetween(degrees, mProperties.flowerExtractorClosed, ExtractorTuneLimit());
    }

    /** The furthest the bumpers may push the extractor: past open, away from closed. */
    public double ExtractorTuneLimit() {
        double extra = DegreesToPosition(mProperties.flowerExtractorTuneExtraDeg,
                mProperties.launchServoRangeDeg);
        if (mProperties.flowerExtractorOpen < mProperties.flowerExtractorClosed) {
            return mProperties.flowerExtractorOpen - extra;
        }
        return mProperties.flowerExtractorOpen + extra;
    }

    /**
     * Keep a servo between two positions. Either one may be the bigger number: pollen's
     * closed spot is above its open spot, nectar's is below.
     */
    private double KeepBetween(double degrees, double positionA, double positionB) {
        double lowest = PositionToDegrees(Math.min(positionA, positionB));
        double highest = PositionToDegrees(Math.max(positionA, positionB));
        return Math.max(lowest, Math.min(degrees, highest));
    }

    /** Degrees to the 0-to-1 position a servo is sent. 150 of 300 degrees is 0.5. */
    public static double DegreesToPosition(double degrees, double rangeDeg) {
        return degrees / rangeDeg;
    }

    private double PositionToDegrees(double position) {
        return position * mProperties.launchServoRangeDeg;
    }

    public double GetPollenPosition() {
        return DegreesToPosition(mPollenDeg, mProperties.launchServoRangeDeg);
    }

    public double GetNectarPosition() {
        return DegreesToPosition(mNectarDeg, mProperties.launchServoRangeDeg);
    }

    public double GetExtractorPosition() {
        return DegreesToPosition(mExtractorDeg, mProperties.launchServoRangeDeg);
    }

    private void ShowStatus() {
        mOp.mTelemetry.addData("Pollen servo (triangle/Y, D-pad up/down)", "position %.3f = %.0f deg%s",
                GetPollenPosition(), mPollenDeg, EndName(GetPollenPosition(),
                        mProperties.pollenServoClosed, mProperties.pollenServoOpen));
        mOp.mTelemetry.addData("Nectar servo (circle/B, D-pad right/left)", "position %.3f = %.0f deg%s",
                GetNectarPosition(), mNectarDeg, EndName(GetNectarPosition(),
                        mProperties.nectarServoClosed, mProperties.nectarServoOpen));
        mOp.mTelemetry.addData("Flower extractor (cross/A, bumpers R+/L-)", "position %.3f = %.0f deg%s",
                GetExtractorPosition(), mExtractorDeg, EndName(GetExtractorPosition(),
                        mProperties.flowerExtractorClosed, mProperties.flowerExtractorOpen));
    }

    /** " CLOSED" or " OPEN" when a servo is at that end of its safe range. */
    private static String EndName(double position, double closed, double open) {
        if (IsAt(position, closed)) {
            return " CLOSED";
        }
        if (IsAt(position, open)) {
            return " OPEN";
        }
        return "";
    }

    /** Close enough counts: 80.1 / 300 can come out as 0.26700000000000002. */
    private static boolean IsAt(double position, double target) {
        return Math.abs(position - target) < 0.0005;
    }
}
