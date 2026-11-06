package org.firstinspires.ftc.teamcode.objects;

import com.acmerobotics.dashboard.FtcDashboard;
import com.acmerobotics.dashboard.config.reflection.FieldProvider;
import java.lang.reflect.Field;

/**
 * Puts settings on FTC Dashboard's Config tab, so they can be changed while the robot runs.
 *
 * <p>The dashboard needs to know where each number lives. We hand it the settings object
 * and the field's name, like "driveSlow". When someone types a new number on the laptop,
 * the dashboard writes it straight into that field, and the next loop uses it.
 *
 * <pre>
 *     DashboardTuning.Show("Drive", properties, new String[] {"driveSlow", "driveStrafeGain"});
 * </pre>
 *
 * <p>A change made on the dashboard is gone at STOP. Write the number that works into the
 * settings file. See docs/decisions/0022.
 *
 * <p>Not at an event: competition rule R704.D bans the dashboard during matches. The caller
 * checks for that; this class only checks that the dashboard is running at all.
 */
public final class DashboardTuning {
    private DashboardTuning() {
    }

    /**
     * Show each named field of {@code settings} on the dashboard, in a group called
     * {@code category}. A field shown before (from an older OpMode) is replaced.
     *
     * @return how many fields were put on the dashboard. 0 when it is not running. A name
     *     with no public field behind it is skipped, so a typo can't crash INIT.
     */
    public static int Show(String category, Object settings, String[] fieldNames) {
        FtcDashboard dashboard = FtcDashboard.getInstance();
        if (dashboard == null) {
            return 0;
        }
        int shown = 0;
        for (String name : fieldNames) {
            Field field = FindField(settings, name);
            if (field != null) {
                dashboard.addConfigVariable(category, name, new FieldProvider<Object>(field, settings));
                shown = shown + 1;
            }
        }
        dashboard.updateConfig();
        return shown;
    }

    /** The public field with this name, or null when there is none. */
    static Field FindField(Object settings, String name) {
        try {
            return settings.getClass().getField(name);
        } catch (NoSuchFieldException e) {
            return null;
        }
    }
}
