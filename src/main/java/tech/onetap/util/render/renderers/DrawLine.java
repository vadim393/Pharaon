package tech.onetap.util.render.renderers;

import net.minecraft.util.math.Vec3d;

public class DrawLine {
    public final Vec3d start;
    public final Vec3d end;
    public final int colorStart;
    public final int colorEnd;
    public final float width;

    public DrawLine(Vec3d start, Vec3d end, int colorStart, int colorEnd, float width) {
        this.start = start;
        this.end = end;
        this.colorStart = colorStart;
        this.colorEnd = colorEnd;
        this.width = width;
    }
}
