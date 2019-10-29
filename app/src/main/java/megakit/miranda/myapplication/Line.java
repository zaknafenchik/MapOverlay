package megakit.miranda.myapplication;


import android.graphics.PointF;

import org.jetbrains.annotations.NotNull;

public class Line {
    PointF p1;
    PointF p2;

    public Line() {
    }

    public Line(PointF pA, PointF pB) {
        p1 = pA;
        p2 = pB;
    }

    public Line(int x1, int y1, int x2, int y2) {
        p1 = new PointF(x1, y1);
        p2 = new PointF(x2, y2);
    }

    @NotNull
    public String toString() {
        return "{(" + p1.x + ", " + p1.y + ")(" + p2.x + ", " + p2.y + ")}";
    }

    public boolean equals(Object o) {
        if (o instanceof Line) {
            return ((Line) o).p1.x == p1.x && ((Line) o).p1.y == p1.y && ((Line) o).p2.x == p2.x
                    && ((Line) o).p2.y == p2.y;
        }
        return false;
    }
}