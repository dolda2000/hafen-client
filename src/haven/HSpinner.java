package haven;

import java.awt.image.BufferedImage;

public class HSpinner extends Widget {
    private static final BufferedImage left = Resource.loadimg("gfx/hud/hspin-left");
    private static final BufferedImage right = Resource.loadimg("gfx/hud/hspin-right");

    private final IButton leftButton;
    private final IButton rightButton;

    public HSpinner() {
        leftButton = add(new IButton(left, left, left));
        rightButton = add(new IButton(right, right, right), leftButton.sz.x + 4, 0);
        pack();
    }

    protected void change(int delta) {}

    @Override
    public void wdgmsg(Widget sender, String msg, Object... args) {
        if (msg.equals("activate")) {
            if (sender == leftButton)
                change(-1);
            else if (sender == rightButton)
                change(1);
        } else
            super.wdgmsg(sender, msg, args);
    }
}
