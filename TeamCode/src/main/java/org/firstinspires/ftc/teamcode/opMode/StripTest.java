package org.firstinspires.ftc.teamcode.opMode;

import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.hardware.DigitalChannel;
import com.qualcomm.robotcore.util.ElapsedTime;
import org.firstinspires.ftc.teamcode.bot.BotProperties;
import org.firstinspires.ftc.teamcode.objects.HydraOpMode;
import org.firstinspires.ftc.teamcode.subsystems.LightStrip;

/**
 * The LED strip, on its own. Nothing moves. It shows two things:
 * <ul>
 *   <li><b>Pollen:</b> the first LEDs (the table below) light only while the pollen sensor
 *       sees something.
 *   <li><b>Alliance:</b> LEDs 6 to 12 show the alliance color, read from the magnetic
 *       switch on the badge.
 * </ul>
 *
 * <p><b>Pollen.</b> Balls fly past the laser, so the table goes on the strip only after the
 * sensor has seen something for {@code HOLD_SECONDS} in a row. Every {@code RECHECK_SECONDS}
 * we look again; if it sees nothing, the pollen lights go off.
 *
 * <p><b>Alliance.</b> During INIT we read the switch every loop, so the color follows the
 * badge as it is put on. When START is pressed the color is locked for the whole run. A
 * magnet means one alliance and no magnet the other (BotProperties says which). An unplugged
 * switch looks like "no magnet", so check the color on the strip before every match.
 *
 * <p>The strip is redrawn when something changes and every {@code RECHECK_SECONDS}, because
 * each message to the Prism takes time. The Driver Station shows the raw sensor values.
 */
@TeleOp(name = "Test: Light Strip", group = "Test")
public class StripTest extends LinearOpMode {

    /** The sensor must see something this long, without a break, before the lights come on. */
    private static final double HOLD_SECONDS = 3;
    /** Look again this often. Also the pause between redraws that nothing asked for. */
    private static final double RECHECK_SECONDS = 3;

    /** The alliance color fills this many LEDs, starting at this one (the first LED is 0). */
    private static final int ALLIANCE_FIRST_LED = 6;
    private static final int ALLIANCE_LEDS = 7;

    /** Pollen lights: one row per run of color: how many LEDs, then red, green, blue (0 to 255). */
    private static final int[][] RUNS = {
            {3, 255, 255, 0},   // 3 yellow
            {1, 255, 0, 0},     // 1 red
    };

    @Override
    public void runOpMode() {
        BotProperties properties = new BotProperties();
        HydraOpMode op = new HydraOpMode(telemetry, hardwareMap, gamepad1, gamepad2);
        LightStrip strip = new LightStrip(op, properties, gamepad1);
        DigitalChannel pollenSensor = hardwareMap.get(DigitalChannel.class, properties.cfgPollenSensor);
        pollenSensor.setMode(DigitalChannel.Mode.INPUT);
        DigitalChannel allianceSensor = hardwareMap.get(DigitalChannel.class, properties.cfgAllianceSensor);
        allianceSensor.setMode(DigitalChannel.Mode.INPUT);
        strip.Init();

        ElapsedTime nearFor = new ElapsedTime();     // how long the pollen sensor has seen something
        ElapsedTime sinceDraw = new ElapsedTime();   // time since the strip was last drawn
        boolean wasNear = false;
        boolean pollenLit = false;
        boolean blue = false;
        boolean drawn = false;                       // has the strip been drawn at all yet?
        boolean drawnPollenLit = false;
        boolean drawnBlue = false;

        while (opModeInInit() || opModeIsActive()) {
            // Sense the alliance. Only during INIT: once START is pressed it stays locked.
            boolean magnet = allianceSensor.getState() == properties.allianceSensorHighWhenMagnet;
            if (opModeInInit()) {
                blue = (magnet == properties.allianceMagnetMeansBlue);
            }

            // Sense pollen: is something in front of the laser, and for how long?
            boolean near = pollenSensor.getState() == properties.pollenSensorHighWhenNear;
            if (near && !wasNear) {
                nearFor.reset();    // it just started seeing something: start counting
            }
            wasNear = near;
            if (near && nearFor.seconds() >= HOLD_SECONDS) {
                pollenLit = true;
            }

            // Re-check: the sensor can say "near" for a moment when nothing is there. If it
            // sees nothing now, the pollen lights go off.
            boolean recheckDue = sinceDraw.seconds() >= RECHECK_SECONDS;
            if (recheckDue && !near) {
                pollenLit = false;
            }

            // Draw when something changed, and again at every re-check in case an earlier
            // message never arrived.
            boolean changed = !drawn || pollenLit != drawnPollenLit || blue != drawnBlue;
            if (changed || recheckDue) {
                strip.Show(Table(pollenLit, blue));
                sinceDraw.reset();
                drawn = true;
                drawnPollenLit = pollenLit;
                drawnBlue = blue;
            }

            telemetry.addData("Pollen sensor", near ? "something near" : "nothing");
            telemetry.addData("Alliance sensor (raw pin)", allianceSensor.getState());
            telemetry.addData("Alliance sensor", magnet ? "magnet" : "no magnet");
            telemetry.addData("Alliance", (blue ? "BLUE" : "RED") + (opModeInInit() ? " (follows the badge until START)" : " (locked)"));
            strip.ShowStatus();
            telemetry.update();
            idle();
        }
    }

    /**
     * What the strip shows, as runs of color from LED 0: the pollen lights (or dark), a dark
     * gap, then the alliance color on its LEDs.
     */
    private static int[][] Table(boolean pollenLit, boolean blue) {
        int[][] pollen = pollenLit ? RUNS : new int[0][];
        int ledsUsed = 0;
        for (int[] run : pollen) {
            ledsUsed += run[0];
        }
        int gap = ALLIANCE_FIRST_LED - ledsUsed;

        int rows = pollen.length + (gap > 0 ? 1 : 0) + 1;
        int[][] table = new int[rows][];
        int row = 0;
        for (int[] run : pollen) {
            table[row++] = run;
        }
        if (gap > 0) {
            table[row++] = new int[] {gap, 0, 0, 0};
        }
        table[row] = blue
                ? new int[] {ALLIANCE_LEDS, 0, 0, 255}
                : new int[] {ALLIANCE_LEDS, 255, 0, 0};
        return table;
    }
}
