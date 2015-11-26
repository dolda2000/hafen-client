package haven;

import haven.minimap.CustomIcon;
import haven.minimap.CustomIconCache;

import java.awt.*;

public class CustomGobIcon extends GAttrib implements MinimapIcon {
    private final CustomIconCache icons;
    private CustomIcon icon;

    public CustomGobIcon(Gob gob, CustomIconCache icons) {
        super(gob);
        this.icons = icons;
    }

    @Override
    public Color color() {
        return icon.color();
    }

    @Override
    public Tex tex() {
        return icon.tex();
    }

    @Override
    public boolean visible() {
        if (icon == null) {
            if (gob.isPlayer()) {
                icon = CustomIcon.player(gob);
            } else {
                Resource res = gob.getres();
                if (res == null)
                    return false;
                icon = icons.get(res.name);
                if (icon == null)
                    icon = CustomIcon.defaultIcon(gob);
            }
        }
        return (icon != CustomIcon.none) && icons.enabled() && (icon.tex() != null);
    }
}
