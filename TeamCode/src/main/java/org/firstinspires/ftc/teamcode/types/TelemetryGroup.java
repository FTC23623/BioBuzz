package org.firstinspires.ftc.teamcode.types;

/**
 * A block of Driver Station lines that can be switched on or off on its own, from the
 * dashboard's "Telemetry" group ({@code tlm...} in BotProperties). The quiet settings
 * ({@code quietTelemetry}, {@code launcherOnlyTelemetry}) still hush everything, whatever
 * the groups say.
 */
public enum TelemetryGroup {
    /** An auto: which step it is on, what that step is, and the next one. */
    AUTO_STEP,
    /** An auto: the launch, aim, tip and flower status lines. */
    AUTO_LAUNCH,
    /** An auto: x, y and heading, and the yaw handed to TeleOp. */
    POSE,
    /** The launchers and the launch gates. */
    LAUNCHER,
    /** The camera: tags, range, bearing, tip. */
    VISION,
    /** The drive: aim assist, launch hold, arc, wiggle. */
    DRIVE,
    /** The intake, the pollen and nectar sensors, the flower extractor. */
    INTAKE,
    /** The indicator light and the light strip. */
    LIGHTS
}
