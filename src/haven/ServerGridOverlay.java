package haven;

import javax.media.opengl.GL2;
import java.nio.FloatBuffer;

import static haven.MCache.cutsz;
import static haven.MCache.tilesz;
import static haven.MCache.sgridsz;

public class ServerGridOverlay extends MapOverlay {
    private final FloatBuffer[] vertexBuffers;
    private final int area;
    private final Coord size;
    private final States.ColState color;
    private Location location;
    private Coord ul;
    private int curIndex;

    public ServerGridOverlay(MCache map, Coord size) {
        super(map);
        this.size = size;
        this.area = (size.x + 1) * (size.y + 1);
        this.color = new States.ColState(36, 255, 0, 128);

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
        FloatBuffer vbuf = getCurrentBuffer();
        vbuf.rewind();
        gl.glEnableClientState(GL2.GL_VERTEX_ARRAY);
        gl.glVertexPointer(3, GL2.GL_FLOAT, 0, vbuf);
        gl.glLineWidth(1.0F);
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

    @Override
    public void update(Coord cc) {
        if (!mapPositionChanged(cc))
            return;
        try {
            // TODO: coord conversion methods to make code like this readable?
            this.ul = cc.div(tilesz).div(cutsz).sub(MapView.view, MapView.view).mul(cutsz).mul(tilesz).div(sgridsz);
            this.location = Location.xlate(new Coord3f(this.ul.x * sgridsz.x, -this.ul.y * sgridsz.y, 0.0F));

            swapBuffers();
            Coord c = new Coord();
            for (c.y = this.ul.y; c.y < this.ul.y + size.y; c.y++)
                for (c.x = this.ul.x; c.x < this.ul.x + size.x; c.x++) {
                    addLineStrip(mapToScreen(c), mapToScreen(c.add(1, 0)), mapToScreen(c.add(1, 1)));
                }

        } catch (Loading e) {}
    }

    private Coord3f mapToScreen(Coord c) {
        return new Coord3f((c.x - ul.x) * sgridsz.x, -(c.y - ul.y) * sgridsz.y, map.getz(c.mul(sgridsz).div(tilesz)));
    }

    private void addLineStrip(Coord3f... vertices) {
        FloatBuffer vbuf = getCurrentBuffer();
        for (int i = 0; i < vertices.length - 1; i++) {
            Coord3f a = vertices[i];
            Coord3f b = vertices[i + 1];
            vbuf.put(a.x).put(a.y).put(a.z);
            vbuf.put(b.x).put(b.y).put(b.z);
        }
    }

    private FloatBuffer getCurrentBuffer() {
        return vertexBuffers[curIndex];
    }

    private void swapBuffers() {
        curIndex = (curIndex + 1) % 2;
        getCurrentBuffer().rewind();
    }
}
