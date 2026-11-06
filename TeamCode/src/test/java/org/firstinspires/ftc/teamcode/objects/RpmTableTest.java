package org.firstinspires.ftc.teamcode.objects;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.firstinspires.ftc.teamcode.bot.BotProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** The launch curve as a lookup table: the same answers as working it out (0038). */
@DisplayName("RpmTable")
class RpmTableTest {
    private static final double EPS = 1e-9;
    private final BotProperties p = new BotProperties();
    private final RpmTable table = new RpmTable();

    @BeforeEach
    void setUp() {
        p.launchNearIn = 110;
        p.launchNearRpm = 2800;
        p.launchFarIn = 140;
        p.launchFarRpm = 3275;
        p.launchCurve = 1.1;
        p.launchBandIn = 2;
    }

    private double worked(double stepIn) {
        return RpmTable.CurveRpm(stepIn, p.launchNearIn, p.launchNearRpm, p.launchFarIn,
                p.launchFarRpm, p.launchCurve);
    }

    @Test
    @DisplayName("every step from the table matches the curve worked out, ends included")
    void matchesTheCurve() {
        table.UpdateFrom(p);
        for (double step = 100; step <= 150; step = step + 2) {
            assertEquals(worked(step), table.Rpm(step), EPS, "step " + step);
        }
        assertEquals(2800, table.Rpm(110), EPS);
        assertEquals(2942, table.Rpm(120), EPS);
        assertEquals(3104, table.Rpm(130), EPS);
        assertEquals(3240, table.Rpm(138), EPS);
        assertEquals(3275, table.Rpm(140), EPS);
    }

    @Test
    @DisplayName("a setting changed on the dashboard: the table is built again")
    void rebuildsOnChange() {
        table.UpdateFrom(p);
        assertEquals(3275, table.Rpm(140), EPS);
        p.launchFarRpm = 3375;
        p.launchCurve = 1.25;
        table.UpdateFrom(p);
        assertEquals(3375, table.Rpm(140), EPS);
        assertEquals(worked(130), table.Rpm(130), EPS);
    }

    @Test
    @DisplayName("steps that don't divide the span evenly: the last step still comes out right")
    void unevenSteps() {
        p.launchBandIn = 4;   // steps 110, 114 ... 138, then the far shot from 140
        table.UpdateFrom(p);
        assertEquals(worked(138), table.Rpm(138), EPS);
        assertEquals(3275, table.Rpm(140), EPS);
    }

    @Test
    @DisplayName("no steps (launchBandIn 0): worked out each time, as before")
    void noSteps() {
        p.launchBandIn = 0;
        table.UpdateFrom(p);
        assertEquals(worked(123.4), table.Rpm(123.4), EPS);
    }
}
