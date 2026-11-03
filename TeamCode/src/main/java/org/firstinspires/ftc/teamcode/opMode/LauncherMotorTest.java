package org.firstinspires.ftc.teamcode.opMode;

import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import org.firstinspires.ftc.teamcode.bot.Bot;
import org.firstinspires.ftc.teamcode.bot.BotProperties;

/**
 * Launcher speed test. Wheel guard on, hands clear, hand on STOP.
 *
 * <p>Both launchers spin up the moment START is pressed, like HyDrive, and there is no
 * button to stop them: press STOP.
 *
 * <p>Gamepad 2 (operator):
 * <ul>
 *   <li><b>D-pad up / down</b>: pollen target up / down 50 RPM. <b>Right / left</b>: nectar.
 * </ul>
 * <p>Gamepad 1: <b>hold right trigger</b> to open the launch gates.
 *
 * <p>Only the launchers and gates run. The drive is switched off, so the wheels never move.
 *
 * <p>The target starts at 4000 RPM in this test. The Driver Station shows target and
 * actual RPM, so you can see how close the speed control gets. See docs/decisions/0009
 * for tuning.
 */
@TeleOp(name = "Test: Launcher", group = "Test")
public class LauncherMotorTest extends LinearOpMode {

    @Override
    public void runOpMode() {
        BotProperties properties = new BotProperties();
        // This test is about the launcher. Leave the drive out of it.
        properties.driveDisabled = true;
        // Fixed targets for this test. Only this OpMode is changed.
        properties.pollenLauncherRpm = 4000;
        properties.nectarLauncherRpm = 4000;
        // The distance table would move the pollen target whenever a tag is in view.
        properties.launcherRangeTuning = false;
        // Launchers only. The gates stay on, so a ball can be launched to test a speed.
        properties.intakeDisabled = true;
        properties.flowerExtractorDisabled = true;

        Bot bot = new Bot(this, properties);
        bot.Init();

        while (opModeInInit()) {
            bot.StartLoop();
            bot.ShowStatus();
            telemetry.addLine("START spins both launchers up. STOP stops them.");
            telemetry.addLine("Gamepad 2: D-pad up/down pollen, right/left nectar.");
            telemetry.addLine("Gamepad 1: hold right trigger to open the launch gates.");
            bot.EndLoop();
        }

        // START was pressed. Spin up now, the same as HyDrive: nothing to press.
        bot.SpinLaunchers(true);

        while (opModeIsActive()) {
            bot.StartLoop();
            bot.HandleUserInput();
            bot.Process();
            bot.EndLoop();
        }
    }
}
