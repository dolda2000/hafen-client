package haven;

public class CustomGobIcon extends BaseGobIcon {
    private final CustomIconConfig config;
    private final Indir<Resource> res;
    private Tex tex;

    public CustomGobIcon(Gob gob, CustomIconConfig config) {
        super(gob);
        this.config = config;
        this.res = (MinimapIcons.isVisible(gob))
            ? Resource.local().load(MinimapIcons.getIconResourceName(gob))
            : null;
    }

    @Override
    public Tex tex() {
        if (tex == null) {
            Resource.Image img = res.get().layer(Resource.imgc);
            tex = img.tex();
        }
        return tex;
    }

    @Override
    public boolean visible() {
        return (res != null) && MinimapIcons.isVisible(gob) && config.enabled();
    }
}
