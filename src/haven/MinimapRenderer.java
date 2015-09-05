package haven;

import haven.resutil.Ridges;

import java.awt.*;
import java.awt.image.BufferedImage;

public class MinimapRenderer {
    private final MCache map;

    public MinimapRenderer(MCache map) {
        this.map = map;
    }

    public BufferedImage draw(Coord ul, Coord sz) {
        BufferedImage[] texes = new BufferedImage[256];
        BufferedImage buf = TexI.mkbuf(sz);
        Coord c = new Coord();
        for(c.y = 0; c.y < sz.y; c.y++) {
            for(c.x = 0; c.x < sz.x; c.x++) {
                int t = map.gettile(ul.add(c));
                BufferedImage tex = tileimg(t, texes);
                int rgb = 0;
                if(tex != null)
                    rgb = tex.getRGB(Utils.floormod(c.x + ul.x, tex.getWidth()),
                            Utils.floormod(c.y + ul.y, tex.getHeight()));
                if (rgb == 0)
                    rgb = Integer.MAX_VALUE;
                buf.setRGB(c.x, c.y, rgb);
            }
        }
        for(c.y = 1; c.y < sz.y - 1; c.y++) {
            for(c.x = 1; c.x < sz.x - 1; c.x++) {
                int t = map.gettile(ul.add(c));
                Tiler tl = map.tiler(t);
                if(tl instanceof Ridges.RidgeTile) {
                    if(Ridges.brokenp(map, ul.add(c))) {
                        for(int y = c.y - 1; y <= c.y + 1; y++) {
                            for(int x = c.x - 1; x <= c.x + 1; x++) {
                                Color cc = new Color(buf.getRGB(x, y));
                                buf.setRGB(x, y, Utils.blendcol(cc, Color.BLACK, ((x == c.x) && (y == c.y))?1:0.1).getRGB());
                            }
                        }
                    }
                }
            }
        }
        for(c.y = 0; c.y < sz.y; c.y++) {
            for(c.x = 0; c.x < sz.x; c.x++) {
                try {
                    int t = map.gettile(ul.add(c));
                    if ((map.gettile(ul.add(c).add(-1, 0)) > t) ||
                            (map.gettile(ul.add(c).add(1, 0)) > t) ||
                            (map.gettile(ul.add(c).add(0, -1)) > t) ||
                            (map.gettile(ul.add(c).add(0, 1)) > t))
                        buf.setRGB(c.x, c.y, Color.BLACK.getRGB());
                } catch (Loading ex) {
                    // ignore loading exceptions
                    // might be better to add check beforehand to improve performance
                }
            }
        }
        return(buf);
    }

    private BufferedImage tileimg(int t, BufferedImage[] texes) {
        BufferedImage img = texes[t];
        if(img == null) {
            Resource r = map.tilesetr(t);
            if(r == null)
                return(null);
            Resource.Image ir = r.layer(Resource.imgc);
            if(ir == null)
                return(null);
            img = ir.img;
            texes[t] = img;
        }
        return(img);
    }

}
