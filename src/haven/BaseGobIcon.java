package haven;

public abstract class BaseGobIcon extends GAttrib {
    public BaseGobIcon(Gob gob) {
        super(gob);
    }

    abstract boolean visible();
    abstract Tex tex();
}
