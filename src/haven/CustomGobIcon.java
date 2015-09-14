package haven;

import haven.minimap.CustomIcon;
import haven.minimap.CustomIconCache;

import java.awt.*;

public class CustomGobIcon extends BaseGobIcon {
    private static final String PLAYER_RES = "gfx/borka/body";

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
            Resource res = gob.getres();
            if (res == null)
                return false;
            if (res.name.equals(PLAYER_RES)) {
                icon = CustomIcon.player(gob);
            } else {
                icon = icons.get(res.name);
                if (icon == null)
                    icon = CustomIcon.none;
            }
        }
        return (icon != CustomIcon.none) && icons.enabled();
    }
}
