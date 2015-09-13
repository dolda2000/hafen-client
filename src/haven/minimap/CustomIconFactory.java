package haven.minimap;

import haven.*;

import java.awt.*;
import java.awt.image.BufferedImage;

public class CustomIconFactory {
    private static final Font font = Resource.loadfont("ui/msreferencesansserif").deriveFont(9f);
    private static final FontMetrics metrics;

    static {
        BufferedImage junk = TexI.mkbuf(new Coord(10, 10));
        Graphics tmpl = junk.getGraphics();
        tmpl.setFont(font);
        metrics = tmpl.getFontMetrics();
    }

    public CustomIcon text(String text, Color color) {
        text = text.toUpperCase();
        Coord sz = new Coord(metrics.stringWidth(text) + 3, metrics.getAscent() + metrics.getDescent());
        if(sz.x < 1)
            sz = sz.add(1, 0);
        BufferedImage img = TexI.mkbuf(sz);
        Graphics g = img.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, sz.x - 1, sz.y - 1);
        g.setColor(Color.BLACK);
        g.drawRect(0, 0, sz.x - 1, sz.y - 1);
        g.setFont(font);
        FontMetrics m = g.getFontMetrics();
        g.drawString(text, 1, m.getAscent());
        g.dispose();
        return new CustomIcon(new TexI(img), color);
    }

}
