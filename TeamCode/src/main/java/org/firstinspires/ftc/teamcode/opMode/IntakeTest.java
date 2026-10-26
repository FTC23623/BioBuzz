package org.firstinspires.ftc.teamcode.opMode;

import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import org.firstinspires.ftc.teamcode.bot.Bot;
import org.firstinspires.ftc.teamcode.bot.BotProperties;

/**
 * Intake and transfer test. Hands clear, hand on STOP.
 *
 * <p>Press START and both motors, `intake` and `transfer`, run at full power until STOP.
 * No buttons for those. The `pollenLauncher` is the same speed control as the nectar
 * launcher, starting at 3000 RPM. Gamepad 1: hold right trigger to spin it, right and left
 * bumper to raise and lower the target by 50 RPM. Nothing else on the robot runs: the drive, launcher, camera, lights and
 * sensors are all switched off. (The Safe Mode key is the one thing that is always
 * checked; it needs the touch sensor `liftHomeSwitch` in the robot configuration.)
 *
 * <p>If a motor spins the wrong way, flip {@code intakeReversed} or {@code transferReversed}
 * in BotProperties.
 */
@TeleOp(name = "Test: Intake", group = "Test")
public class IntakeTest extends LinearOpMode {

    @Override
    public void runOpMode() {
        BotProperties properties = new BotProperties();
        // Only the intake and the transfer. Everything else is left out.
        properties.driveDisabled = true;
        properties.launcherDisabled = true;
        properties.visionDisabled = true;
        properties.indicatorDisabled = true;
        properties.stripDisabled = true;
        properties.intakeDisabled = false;
        properties.pollenLauncherDisabled = false;

        Bot bot = new Bot(this, properties);
        bot.Init();

        while (opModeInInit()) {
            bot.StartLoop();
            bot.ShowStatus();
            telemetry.addLine("Press START: intake and transfer run at full power.");
            telemetry.addLine("Hold right trigger: the pollen launcher spins. Bumpers change its speed.");
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
