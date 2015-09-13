package haven;

public abstract class BaseGobIcon extends GAttrib {
    public BaseGobIcon(Gob gob) {
        super(gob);
    }

    public abstract boolean visible();
    public abstract Tex tex();
}
