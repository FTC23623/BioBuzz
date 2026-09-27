package org.firstinspires.ftc.teamcode.pedro;

import com.pedropathing.algorithm.Foresight;
import com.pedropathing.revhub.localizers.TwoWheelLocalizer;
import com.pedropathing.tuning.autotune.Procedure;
import com.pedropathing.tuning.autotune.Tuner;
import org.firstinspires.ftc.teamcode.pedro.procedures.ForesightTuner;
import org.firstinspires.ftc.teamcode.pedro.procedures.MecanumTuner;
import org.firstinspires.ftc.teamcode.pedro.procedures.Tests;
import org.firstinspires.ftc.teamcode.pedro.procedures.TwoWheelTuner;

/**
 * The tuners that show up in Pedro's AutoTune web page, http://192.168.43.1:10158 while
 * connected to the robot's Wi-Fi. Run them in this order: Mecanum, Two Wheel, Foresight.
 *
 * <p><b>SAFETY: the tuners ignore Safe Mode.</b> They have to, to measure the robot's real
 * top speed. The Foresight Tuner drives at FULL POWER for about 4 feet, forward and left.
 * Floor only, clear space, hand on STOP.
 *
 * <p>The {@code hardwareMap -> ...} arrows are how Pedro's tuners ask for "a way to build
 * the part". They are Pedro's style, kept as-is here; the team's own code doesn't use them.
 * The procedures/ folder is copied unchanged from Pedro's Quickstart (BSD 3-Clause Clear).
 */
public final class Tuning {
    private Tuning() {
    }

    /** Tuners measure the real robot, so Safe Mode is off for them. */
    private static final boolean safeModeForTuners = false;

    @Tuner
    public static Procedure mecanumTuner() {
        return new MecanumTuner();
    }

    @Tuner
    public static Procedure twoWheelTuner() {
        return new TwoWheelTuner();
    }

    @Tuner
    public static Procedure foresightTuner() {
        return new ForesightTuner(
                hardwareMap -> new TwoWheelLocalizer(hardwareMap, PedroConstants.LocalizerConfig()),
                hardwareMap -> new SafeModeMecanum(hardwareMap, PedroConstants.DriveConfig(), safeModeForTuners));
    }

    /** Drive, localizer, and (once tuned) path-following tests. */
    @Tuner
    public static Procedure tests() {
        if (PedroConstants.foresightTuned) {
            return new Tests(
                    hardwareMap -> new SafeModeMecanum(hardwareMap, PedroConstants.DriveConfig(), safeModeForTuners),
                    hardwareMap -> new TwoWheelLocalizer(hardwareMap, PedroConstants.LocalizerConfig()),
                    () -> new Foresight(PedroConstants.ForesightConfig()));
        }
        return new Tests(
                hardwareMap -> new SafeModeMecanum(hardwareMap, PedroConstants.DriveConfig(), safeModeForTuners),
                hardwareMap -> new TwoWheelLocalizer(hardwareMap, PedroConstants.LocalizerConfig()),
                null);
    }
}
