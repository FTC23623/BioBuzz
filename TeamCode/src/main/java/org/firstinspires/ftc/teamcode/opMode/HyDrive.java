package org.firstinspires.ftc.teamcode.opMode;

import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import org.firstinspires.ftc.teamcode.bot.Bot;
import org.firstinspires.ftc.teamcode.bot.BotProperties;

/**
 * The one TeleOp. The alliance comes from the magnet on the robot, not from the OpMode
 * name. If the magnet sensor is not working, {@link HyDriveRed} and {@link HyDriveBlue}
 * (group "Backup") set the alliance themselves, like the backup autos (docs/decisions/0021).
 *
 * <p>This OpMode builds the {@link Bot} and runs the loop. The Bot does the rest: each
 * loop, every part of the robot reads the gamepads, then every part does its work.
 *
 * <p>The drive is field-centric: "forward" on the stick is away from the driver, whichever
 * way the robot faces. At INIT, point the robot away from the driver. That direction
 * becomes "forward". Press circle during the match to re-set it.
 *
 * <p>The launchers spin up the moment START is pressed, so the first shot is ready.
 *
 * <p>Gamepad 1, driver: sticks drive, left bumper (hold) slow, left trigger (hold) drift
 * assist, right bumper (hold) aims at our hive, D-pad left / right while aiming arcs around
 * the hive, <b>right trigger (hold) launches</b>: once the camera is locked on our hive the
 * gates open, then the intake and transfer push the balls in. No tag in view: the gates
 * stay shut and the gamepad buzzes.
 * <br>Gamepad 2, operator: the launchers are always on, D-pad up/down pollen speed,
 * right/left nectar speed, left bumper runs the intake and transfer (on / off), <b>left
 * trigger (hold)</b> runs them while held, <b>square (hold)</b> runs them backward, slowly,
 * to spit out extra balls, <b>right trigger (hold)</b> lets a launch through with no camera
 * lock. See docs/decisions/0023, 0032, 0039 and 0040.
 */
@TeleOp(name = "HyDrive")
public class HyDrive extends LinearOpMode {

    @Override
    public void runOpMode() {
        BotProperties properties = new BotProperties();
        ChooseDriveOptions(properties);

        Bot bot = new Bot(this, properties);
        bot.Init();

        // Waiting for START. Shows Safe Mode and the heading, so they can be checked.
        while (opModeInInit()) {
            bot.StartLoop();
            bot.ShowStatus();
            bot.EndLoop();
        }

        // START was pressed. Spin the launchers up right away, so the first shot is ready.
        // They stay on until STOP: no button turns them off.
        bot.SpinLaunchers(true);

        // This loop runs about 50 times a second until STOP.
        while (opModeIsActive()) {
            bot.StartLoop();
            bot.HandleUserInput();
            bot.Process();
            bot.EndLoop();
        }
    }

    /**
     * HyDrive drives the normal way, so this does nothing. A practice TeleOp copies HyDrive
     * and changes drive settings here, before the Bot is built. See {@link PracticeFieldCentric}.
     */
    protected void ChooseDriveOptions(BotProperties properties) {
    }
}
