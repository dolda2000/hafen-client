package haven;

import haven.resutil.Ridges;

import java.awt.*;
import java.awt.image.BufferedImage;

public class MinimapRenderer {
    private final MCache map;

    public MinimapRenderer(MCache map) {
        this.map = map;
    }

    public BufferedImage draw(MCache.Grid grid) {
        Coord sz = MCache.cmaps;
        BufferedImage[] texes = new BufferedImage[256];
        BufferedImage buf = TexI.mkbuf(sz);
        Coord c = new Coord();
        for(c.y = 0; c.y < sz.y; c.y++) {
            for(c.x = 0; c.x < sz.x; c.x++) {
                int t = grid.gettile(c);
                BufferedImage tex = tileimg(t, texes);
                int rgb = 0;
                if(tex != null)
                    rgb = tex.getRGB(Utils.floormod(c.x + grid.ul.x, tex.getWidth()),
                            Utils.floormod(c.y + grid.ul.y, tex.getHeight()));
                if (rgb == 0)
                    rgb = Integer.MAX_VALUE;
                buf.setRGB(c.x, c.y, rgb);
            }
        }
        for(c.y = 1; c.y < sz.y - 1; c.y++) {
            for(c.x = 1; c.x < sz.x - 1; c.x++) {
                int t = grid.gettile(c);
                Tiler tl = map.tiler(t);
                if(tl instanceof Ridges.RidgeTile) {
                    if (brokenp(map, grid, c)) {
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
        for(c.y = 1; c.y < sz.y - 1; c.y++) {
            for(c.x = 1; c.x < sz.x - 1; c.x++) {
                int t = grid.gettile(c);
                if ((grid.gettile(c.add(-1, 0)) > t) ||
                        (grid.gettile(c.add(1, 0)) > t) ||
                        (grid.gettile(c.add(0, -1)) > t) ||
                        (grid.gettile(c.add(0, 1)) > t))
                    buf.setRGB(c.x, c.y, Color.BLACK.getRGB());
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

    private static boolean brokenp(MCache map, MCache.Grid grid, Coord tc) {
        Tiler t = map.tiler(grid.gettile(tc));
        if(!(t instanceof Ridges.RidgeTile))
            return(false);
        int bz = ((Ridges.RidgeTile)t).breakz();
        for(Coord ec : Ridges.tecs) {
            t = map.tiler(grid.gettile(tc.add(ec)));
            if(t instanceof Ridges.RidgeTile)
                bz = Math.min(bz, ((Ridges.RidgeTile)t).breakz());
        }
        for(int i = 0; i < 4; i++) {
            if(Math.abs(grid.getz(tc.add(Ridges.tccs[(i + 1) % 4])) - grid.getz(tc.add(Ridges.tccs[i]))) > bz)
                return(true);
        }
        return(false);
    }
}
