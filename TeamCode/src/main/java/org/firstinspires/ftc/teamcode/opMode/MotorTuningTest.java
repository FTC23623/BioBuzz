package org.firstinspires.ftc.teamcode.opMode;

import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import org.firstinspires.ftc.teamcode.bot.Bot;
import org.firstinspires.ftc.teamcode.bot.BotProperties;

/**
 * Find the right speeds for the intake, transfer and launchers. Wheel guards on, hands
 * clear of the rollers, hand on STOP.
 *
 * <p>Gamepad 1:
 * <ul>
 *   <li><b>Triangle / Y</b>: pollen launcher on / off. <b>Circle / B</b>: nectar launcher.
 *   <li><b>Cross / A</b>: intake on / off. <b>Square / X</b>: transfer.
 *   <li><b>D-pad up / down</b>: the motor whose button was pressed last, faster / slower.
 * </ul>
 *
 * <p>Each motor has its own speed. With the dashboard open, they can be typed in there too.
 *
 * <p>Only these four motors run. The drive is switched off, so the wheels never move.
 * When a speed works, write it in BotProperties. See docs/decisions/0019.
 */
@TeleOp(name = "Test: Motor Tuning", group = "Test")
public class MotorTuningTest extends LinearOpMode {

    @Override
    public void runOpMode() {
        BotProperties properties = new BotProperties();
        // Only the tuner. The launcher would fight it for the nectar motor, and the LED
        // strip would fight it for the D-pad.
        properties.motorTunerDisabled = false;
        properties.driveDisabled = true;
        properties.launcherDisabled = true;
        properties.intakeDisabled = true;
        properties.launchGatesDisabled = true;
        // Only motors move in this test.
        properties.flowerExtractorDisabled = true;
        properties.stripDisabled = true;

        Bot bot = new Bot(this, properties);
        bot.Init();

        while (opModeInInit()) {
            bot.StartLoop();
            bot.ShowStatus();
            // The tuner only shows its status after START, so list the buttons here.
            telemetry.addLine("Press START, then each button turns its motor on / off:");
            telemetry.addLine("  Triangle / Y: pollen launcher");
            telemetry.addLine("  Circle / B: nectar launcher");
            telemetry.addLine("  Cross / A: intake");
            telemetry.addLine("  Square / X: transfer");
            telemetry.addLine("D-pad up / down: speed of the motor pressed last");
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
