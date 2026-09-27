package org.firstinspires.ftc.teamcode.opMode;

import com.pedropathing.paths.Path;
import com.qualcomm.robotcore.eventloop.opmode.Autonomous;
import java.util.List;
import org.firstinspires.ftc.teamcode.pedro.ShapePaths;

/** Drives a C, turning like a car. The shape is in {@link ShapePaths#CWithTurning()}. */
@Autonomous(name = "Shape: C (turning)", group = "Shapes")
public class AutoShapeCTurning extends PedroAuto {
    @Override
    protected List<Path> BuildPaths() {
        return ShapePaths.CWithTurning();
    }
}
