package haven.minimap;

import haven.*;
import haven.util.Optional;

public class CustomGobIcon extends BaseGobIcon {
    private final CustomIconConfig config;
    private Optional<Tex> tex;

    public CustomGobIcon(Gob gob, CustomIconConfig config) {
        super(gob);
        this.config = config;
    }

    @Override
    public Tex tex() {
        return tex.getValue();
    }

    @Override
    public boolean visible() {
        if (tex == null) {
            Resource res = gob.getres();
            if (res == null)
                return false;
            tex = Optional.of(config.getIcon(res.name));
        }
        return tex.hasValue() && config.enabled();
    }
}
