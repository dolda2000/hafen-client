package haven;

public class MinimapIconsWindow extends Window {

    public MinimapIconsWindow(Coord sz, String cap, String iconType) {
        super(sz, cap);
        MinimapIcons.addSelection(iconType, this);
    }

    @Override
    public void wdgmsg(Widget sender, String msg, Object... args) {
        if(sender == cbtn) {
            hide();
        } else {
            super.wdgmsg(sender, msg, args);
        }
    }
}
