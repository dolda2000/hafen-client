package haven;

import java.awt.image.BufferedImage;

public class HSpinner extends Widget {
    private static final BufferedImage left = Resource.loadimg("gfx/hud/hspin-left");
    private static final BufferedImage lefth = Resource.loadimg("gfx/hud/hspin-left-h");
    private static final BufferedImage right = Resource.loadimg("gfx/hud/hspin-right");
    private static final BufferedImage righth = Resource.loadimg("gfx/hud/hspin-right-h");

    private final IButton leftButton;
    private final IButton rightButton;

    public HSpinner() {
        leftButton = add(new IButton(left, left, lefth));
        rightButton = add(new IButton(right, right, righth), leftButton.sz.x + 2, 0);
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
