package haven;

import java.awt.*;

public abstract class BaseGobIcon extends GAttrib {
    public BaseGobIcon(Gob gob) {
        super(gob);
    }

    public Color color() {
        return Color.WHITE;
    }

    public boolean visible() {
        return true;
    }

    public abstract Tex tex();
}
