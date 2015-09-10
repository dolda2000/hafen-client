package me.kt;

import haven.*;

import javax.media.opengl.GL2;
import java.nio.FloatBuffer;

public class GridOutline implements Rendered {
    private final MCache map;
    private final FloatBuffer[] vertexBuffers;
    private final int area;
    private final Coord size;
    private final States.ColState color;
    private Location location;
    private int curIndex;

    public GridOutline(MCache map, Coord size) {
        this.map = map;
        this.size = size;
        this.area = (size.x + 1) * (size.y + 1);
        this.color = new States.ColState(255, 255, 255, 64);

        // double-buffer to prevent flickering
        vertexBuffers = new FloatBuffer[2];
        for (int i = 0; i < vertexBuffers.length; i++)
            vertexBuffers[i] = Utils.mkfbuf(this.area * 3 * 4);
        curIndex = 0;
    }

    @Override
    public void draw(GOut g) {
        g.apply();
        BGL gl = g.gl;
        FloatBuffer vbuf = getVertexBuffer();
        vbuf.rewind();
        gl.glEnableClientState(GL2.GL_VERTEX_ARRAY);
        gl.glVertexPointer(3, GL2.GL_FLOAT, 0, vbuf);
        gl.glDrawArrays(GL2.GL_LINES, 0, area * 4);
        gl.glDisableClientState(GL2.GL_VERTEX_ARRAY);
    }

    @Override
    public boolean setup(RenderList rl) {
        rl.prepo(location);
        rl.prepo(States.ndepthtest);
        rl.prepo(last);
        rl.prepo(color);
        return true;
    }

    public void update(Coord ul) {
        try {
            location = Location.xlate(new Coord3f(ul.x * MCache.tilesz.x, -ul.y * MCache.tilesz.y, 0.0F));
            swapBuffers();
            FloatBuffer vbuf = getVertexBuffer();
            for (int y = 0; y <= size.y - 1; y++) {
                for (int x = 0; x <= size.x - 1; x++) {
                    Coord a = new Coord(ul.x + x, ul.y + y);
                    int az = map.getz(a);
                    int bz = map.getz(a.add(1, 0));
                    int cz = map.getz(a.add(1, 1));
                    vbuf.put(x * MCache.tilesz.x).put(-y * MCache.tilesz.y).put(az);
                    vbuf.put((x + 1) * MCache.tilesz.x).put(-y * MCache.tilesz.y).put(bz);
                    vbuf.put((x + 1) * MCache.tilesz.x).put(-y * MCache.tilesz.y).put(bz);
                    vbuf.put((x + 1) * MCache.tilesz.x).put(-(y + 1) * MCache.tilesz.y).put(cz);
                }
            }
        } catch (Loading e) {}
    }

    private FloatBuffer getVertexBuffer() {
        return vertexBuffers[curIndex];
    }

    private void swapBuffers() {
        curIndex = (curIndex + 1) % 2;
    }
}
