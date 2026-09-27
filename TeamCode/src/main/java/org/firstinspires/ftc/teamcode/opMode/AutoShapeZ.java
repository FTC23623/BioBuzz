package org.firstinspires.ftc.teamcode.opMode;

import com.pedropathing.paths.Path;
import com.qualcomm.robotcore.eventloop.opmode.Autonomous;
import java.util.List;
import org.firstinspires.ftc.teamcode.pedro.ShapePaths;

/** Drives a Z with sharp corners, always facing forward. The shape is in {@link ShapePaths#Z()}. */
@Autonomous(name = "Shape: Z", group = "Shapes")
public class AutoShapeZ extends PedroAuto {
    @Override
    protected List<Path> BuildPaths() {
        return ShapePaths.Z();
    }
}
