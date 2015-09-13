package haven;

import haven.minimap.CustomIcon;
import haven.minimap.CustomIconCache;
import haven.util.Optional;

import java.awt.*;

public class CustomGobIcon extends BaseGobIcon {
    private final CustomIconCache icons;
    private Optional<CustomIcon> icon;

    public CustomGobIcon(Gob gob, CustomIconCache icons) {
        super(gob);
        this.icons = icons;
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
            icon = Optional.of(icons.get(res.name));
        }
        return icon.hasValue() && icons.enabled();
    }
}
