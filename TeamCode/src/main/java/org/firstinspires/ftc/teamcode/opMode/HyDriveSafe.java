package org.firstinspires.ftc.teamcode.opMode;

import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import org.firstinspires.ftc.teamcode.bot.BotProperties;

/**
 * {@link HyDrive} with the launchers switched off. Everything else is the same: drive,
 * intake, gates, camera and lights.
 *
 * <p>Use it for driver practice and for bench work near people, where two wheels at 6000 RPM
 * are not needed. START does not spin anything up. The Bot never touches the launcher motors.
 *
 * <p>Not in a match build (TeamCode/match-build-excludes.txt).
 */
@TeleOp(name = "HyDrive Safe (no launchers)", group = "Practice")
public class HyDriveSafe extends HyDrive {
    @Override
    protected void ChooseDriveOptions(BotProperties properties) {
        properties.launcherDisabled = true;
    }
}
