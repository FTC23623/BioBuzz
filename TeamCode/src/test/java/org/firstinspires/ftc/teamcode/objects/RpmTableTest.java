package org.firstinspires.ftc.teamcode.objects;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.firstinspires.ftc.teamcode.bot.BotProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** The launch speeds as a lookup table: the same answers as working them out (0038). */
@DisplayName("RpmTable")
class RpmTableTest {
    private static final double EPS = 1e-9;
    private final BotProperties p = new BotProperties();
    private final RpmTable table = new RpmTable();

    @BeforeEach
    void setUp() {
        p.launchNearIn = 110;
        p.launchNearRpm = 2760;
        p.launchMid1In = 120;
        p.launchMid1Rpm = 2831;
        p.launchMid2In = 130;
        p.launchMid2Rpm = 2983;
        p.launchFarIn = 140;
        p.launchFarRpm = 3115;
        p.launchBandIn = 2;
    }

    private double worked(double stepIn) {
        return RpmTable.LineRpm(stepIn, RpmTable.PointsIn(p), RpmTable.PointsRpm(p));
    }

    @Test
    @DisplayName("every step from the table matches the lines worked out, ends included")
    void matchesTheLines() {
        table.UpdateFrom(p);
        for (double step = 100; step <= 150; step = step + 2) {
            assertEquals(worked(step), table.Rpm(step), EPS, "step " + step);
        }
    }

    @Test
    @DisplayName("each measured shot comes out exactly; past the far shot stays at the far speed")
    void hitsEveryMeasuredShot() {
        table.UpdateFrom(p);
        assertEquals(2760, table.Rpm(100), EPS);   // nearer than the near shot
        assertEquals(2760, table.Rpm(110), EPS);
        assertEquals(2831, table.Rpm(120), EPS);
        assertEquals(2983, table.Rpm(130), EPS);
        assertEquals(3115, table.Rpm(140), EPS);
        assertEquals(3115, table.Rpm(145), EPS);   // the coach's 145 in
    }

    @Test
    @DisplayName("between two shots: a straight line, rounded")
    void straightLineBetween() {
        table.UpdateFrom(p);
        assertEquals(2774, table.Rpm(112), EPS);   // 2760 + 71 x 2/10 = 2774.2
        assertEquals(2892, table.Rpm(124), EPS);   // 2831 + 152 x 4/10 = 2891.8
        assertEquals(3089, table.Rpm(138), EPS);   // 2983 + 132 x 8/10 = 3088.6
    }

    @Test
    @DisplayName("a setting changed on the dashboard: the table is built again")
    void rebuildsOnChange() {
        table.UpdateFrom(p);
        assertEquals(2983, table.Rpm(130), EPS);
        p.launchMid2Rpm = 3000;
        table.UpdateFrom(p);
        assertEquals(3000, table.Rpm(130), EPS);
        assertEquals(worked(128), table.Rpm(128), EPS);
    }

    @Test
    @DisplayName("steps that don't divide the span evenly: the last step still comes out right")
    void unevenSteps() {
        p.launchBandIn = 4;   // steps 110, 114 ... 138, then the far shot from 140
        table.UpdateFrom(p);
        assertEquals(worked(138), table.Rpm(138), EPS);
        assertEquals(3115, table.Rpm(140), EPS);
    }

    @Test
    @DisplayName("no steps (launchBandIn 0): worked out each time, as before")
    void noSteps() {
        p.launchBandIn = 0;
        table.UpdateFrom(p);
        assertEquals(worked(123.4), table.Rpm(123.4), EPS);
    }

    @Test
    @DisplayName("a middle shot typed out of order is skipped, no divide by zero")
    void outOfOrderIsSkipped() {
        p.launchMid1In = 110;   // same as the near shot: skipped
        // 110 to 130 is now one straight line: 2760 + (2983 - 2760) x 10/20 = 2871.5, rounded 2872
        assertEquals(2872, worked(120), EPS);
    }
}
