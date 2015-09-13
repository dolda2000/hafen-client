package haven;

import haven.minimap.CustomIcon;
import haven.minimap.CustomIconConfig;
import haven.util.Optional;

import java.awt.*;

public class CustomGobIcon extends BaseGobIcon {
    private final CustomIconConfig config;
    private Optional<CustomIcon> icon;

    public CustomGobIcon(Gob gob, CustomIconConfig config) {
        super(gob);
        this.config = config;
    }

    @Override
    public Color color() {
        return icon.hasValue() ? icon.getValue().color : null;
    }

    @Override
    public Tex tex() {
        return icon.hasValue() ? icon.getValue().tex : null;
    }

    @Override
    public boolean visible() {
        if (icon == null) {
            Resource res = gob.getres();
            if (res == null)
                return false;
            icon = Optional.of(config.getIcon(res.name));
        }
        return icon.hasValue() && config.enabled();
    }
}
