package me.kt;

import haven.*;

import javax.media.opengl.GL2;
import java.nio.FloatBuffer;

public class TileOutline implements Rendered {
    private final MCache map;
    private FloatBuffer cposb;
    private final int area;
    private final Coord sz;
    private Location dloc;
    private States.ColState color;

    public TileOutline(MCache map, Coord sz) {
        this.map = map;
        this.sz = sz;
        this.area = (sz.x + 1) * (sz.y + 1);
        this.cposb = Utils.mkfbuf(this.area * 3 * 4);
        this.color = new States.ColState(255, 255, 255, 64);
    }

    @Override
    public void draw(GOut g) {
        g.apply();
        BGL gl = g.gl;
        this.cposb.rewind();
        gl.glEnableClientState(GL2.GL_VERTEX_ARRAY);
        gl.glVertexPointer(3, GL2.GL_FLOAT, 0, this.cposb);
        gl.glDrawArrays(GL2.GL_LINES, 0, this.area * 4);
        gl.glDisableClientState(GL2.GL_VERTEX_ARRAY);
    }

    @Override
    public boolean setup(RenderList rl) {
        rl.prepo(dloc);
        rl.prepo(States.ndepthtest);
        rl.prepo(last);
        rl.prepo(color);
        return true;
    }

    public void update(Coord ul) {
        try {
            dloc = Location.xlate(new Coord3f(ul.x * MCache.tilesz.x, -ul.y * MCache.tilesz.y, 0.0F));
            // double-buffer to prevent flickering
            FloatBuffer buf = Utils.mkfbuf(this.area * 3 * 4);
            for (int y = 0; y <= sz.y - 1; y++) {
                for (int x = 0; x <= sz.x - 1; x++) {
                    Coord a = new Coord(ul.x + x, ul.y + y);
                    int az = map.getz(a);
                    int bz = map.getz(a.add(1, 0));
                    int cz = map.getz(a.add(1, 1));
                    buf.put(x * MCache.tilesz.x).put(-y * MCache.tilesz.y).put(az);
                    buf.put((x + 1) * MCache.tilesz.x).put(-y * MCache.tilesz.y).put(bz);
                    buf.put((x + 1) * MCache.tilesz.x).put(-y * MCache.tilesz.y).put(bz);
                    buf.put((x + 1) * MCache.tilesz.x).put(-(y + 1) * MCache.tilesz.y).put(cz);
                }
            }
            this.cposb = buf;
        } catch (Loading e) {}
    }
}
