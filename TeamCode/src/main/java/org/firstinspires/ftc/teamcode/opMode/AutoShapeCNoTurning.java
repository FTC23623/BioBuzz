package org.firstinspires.ftc.teamcode.opMode;

import com.pedropathing.paths.Path;
import com.qualcomm.robotcore.eventloop.opmode.Autonomous;
import java.util.List;
import org.firstinspires.ftc.teamcode.pedro.ShapePaths;

/**
 * Drives a C while always facing forward, sliding sideways on the mecanum wheels.
 * The shape is in {@link ShapePaths#CWithoutTurning()}.
 */
@Autonomous(name = "Shape: C (no turning)", group = "Shapes")
public class AutoShapeCNoTurning extends PedroAuto {
    @Override
    protected List<Path> BuildPaths() {
        return ShapePaths.CWithoutTurning();
    }
}
