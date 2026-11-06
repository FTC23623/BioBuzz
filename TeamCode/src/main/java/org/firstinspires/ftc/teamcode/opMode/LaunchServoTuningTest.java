package org.firstinspires.ftc.teamcode.opMode;

import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import org.firstinspires.ftc.teamcode.bot.Bot;
import org.firstinspires.ftc.teamcode.bot.BotProperties;

/**
 * Find the closed and open spots for the pollen and nectar launch servos. Fingers clear
 * of the servo arms: they move when START is pressed.
 *
 * <p>Gamepad 1. Holding a button does not repeat; D-pad and bumpers move 5 degrees:
 * <ul>
 *   <li><b>Triangle / Y</b>: pollen servo open / closed. <b>Circle / B</b>: nectar.
 *   <li><b>D-pad up / down</b>: pollen servo.
 *   <li><b>D-pad right / left</b>: nectar servo.
 *   <li><b>Cross / A</b>: flower extractor open (deployed) / closed (stored).
 *   <li><b>Right / left bumper</b>: flower extractor servo.
 * </ul>
 *
 * <p>All three servos start closed and stay between their closed and open spots in
 * BotProperties (launch servos 0 to 0.267, flower extractor 0 to 0.584). Only these three
 * servos move. See docs/decisions/0020.
 */
@TeleOp(name = "Test: Launch Servo Tuning", group = "Test")
public class LaunchServoTuningTest extends LinearOpMode {

    @Override
    public void runOpMode() {
        BotProperties properties = new BotProperties();
        // Only the servo tuner. Nothing but these servos moves in this test.
        properties.launchServoTunerDisabled = false;
        properties.driveDisabled = true;
        properties.launcherDisabled = true;
        // The gates and the flower extractor use the same servos as the tuner.
        properties.launchGatesDisabled = true;
        properties.flowerExtractorDisabled = true;
        properties.stripDisabled = true;

        Bot bot = new Bot(this, properties);
        bot.Init();

        // A servo can't say where it is, so show where each will go at START.
        while (opModeInInit()) {
            bot.StartLoop();
            bot.ShowStatus();
            telemetry.addLine("Servos do not move until START. Fingers clear.");
            telemetry.addData("At START, pollen servo goes to", "%.3f (closed)",
                    properties.pollenServoClosed);
            telemetry.addData("At START, nectar servo goes to", "%.3f (closed)",
                    properties.nectarServoClosed);
            telemetry.addData("At START, flower extractor goes to", "%.3f (closed)",
                    properties.flowerExtractorClosed);
            telemetry.addData("Launch servos stay between", "%.3f and %.3f",
                    properties.nectarServoClosed, properties.nectarServoOpen);
            telemetry.addData("Flower extractor: closed / open", "%.3f / %.3f",
                    properties.flowerExtractorClosed, properties.flowerExtractorOpen);
            telemetry.addData("Bumpers may push the extractor past open by", "%.0f deg",
                    properties.flowerExtractorTuneExtraDeg);
            telemetry.addLine("Triangle / Y: pollen servo open / closed");
            telemetry.addLine("Circle / B: nectar servo open / closed");
            telemetry.addLine("Cross / A: flower extractor open / closed");
            telemetry.addLine("D-pad up / down: pollen servo, 5 deg per press");
            telemetry.addLine("D-pad right / left: nectar servo, 5 deg per press");
            telemetry.addLine("Right / left bumper: flower extractor, 5 deg per press");
            bot.EndLoop();
        }

        while (opModeIsActive()) {
            bot.StartLoop();
            bot.HandleUserInput();
            bot.Process();
            bot.EndLoop();
        }
    }
}
