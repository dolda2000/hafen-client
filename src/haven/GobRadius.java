package haven;

import javax.media.opengl.GL;
import java.awt.*;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.HashMap;
import java.util.Map;

public class GobRadius {
    private static final Map<String, RadiusSpriteFactory> factories;
    private static final Map<Gob, Gob.Overlay> overlays;

    static {
        overlays = new HashMap<Gob, Gob.Overlay>();
        factories = new HashMap<String,RadiusSpriteFactory>();
        factories.put("gfx/terobjs/minesupport", new RadiusSpriteFactory(100, new Color(192, 0, 0, 64)));
        factories.put("gfx/terobjs/column", new RadiusSpriteFactory(125, new Color(192, 0, 0, 64)));
        factories.put("gfx/terobjs/beehive", new RadiusSpriteFactory(150, new Color(192, 0, 0, 64)));
        factories.put("gfx/terobjs/trough", new RadiusSpriteFactory(200, new Color(0, 192, 0, 64)));
    }

    public static void toggle(final OCache objects, boolean show) {
        try {
            if (show) {
                synchronized (objects) {
                    for (Gob gob : objects) {
                        Resource res = gob.getres();
                        if (res == null)
                            continue;
                        RadiusSpriteFactory factory = factories.get(res.name);
                        if (factory != null) {
                            Gob.Overlay overlay = new Gob.Overlay(factory.create(gob));
                            gob.ols.add(overlay);
                            overlays.put(gob, overlay);
                        }
                    }
                }
            } else {
                for (Map.Entry<Gob, Gob.Overlay> entry : overlays.entrySet()) {
                    entry.getKey().ols.remove(entry.getValue());
                }
                overlays.clear();
            }
        } catch (Loading e) {}
    }

    private static class RadiusSpriteFactory {
        private final int radius;
        private final Color color;

        public RadiusSpriteFactory(int radius, Color color) {
            this.radius = radius;
            this.color = color;
        }

        public Sprite create(Gob owner) {
            return new RadiusSprite(owner, radius, owner.getc().z, color);
        }
    }

    private static class RadiusSprite extends Sprite {
        final GLState smat;
        final VertexBuf.VertexArray posa;
        final VertexBuf.NormalArray nrma;
        final ShortBuffer sidx;
        private Coord lc;
        float[] barda;

        protected RadiusSprite(Sprite.Owner owner, float radius, float z, Color color) {
            super(owner, null);
            this.smat = new States.ColState(color);
            int i = Math.max(24, (int)(Math.PI * 2 * radius / 11.0D));
            FloatBuffer posa = Utils.mkfbuf(i * 3 * 2);
            FloatBuffer nrma = Utils.mkfbuf(i * 3 * 2);
            ShortBuffer sidx = Utils.mksbuf(i * 6);
            for (int j = 0; j < i; j++) {
                float s = (float)Math.sin(Math.PI * 2 * j / i);
                float c = (float)Math.cos(Math.PI * 2 * j / i);
                posa.put(j * 3, c * radius).put(j * 3 + 1, s * radius).put(j * 3 + 2, z + 20.0F);
                posa.put((i + j) * 3, c * radius).put((i + j) * 3 + 1, s * radius).put((i + j) * 3 + 2, z);
                nrma.put(j * 3, c).put(j * 3 + 1, s).put(j * 3 + 2, 0.0F);
                nrma.put((i + j) * 3, c).put((i + j) * 3 + 1, s).put((i + j) * 3 + 2, 0.0F);
                int k = j * 6;
                sidx.put(k, (short) j).put(k + 1, (short)(j + i)).put(k + 2, (short)((j + 1) % i));
                sidx.put(k + 3, (short) (j + i)).put(k + 4, (short)((j + 1) % i + i)).put(k + 5, (short)((j + 1) % i));
            }
            this.posa = new VertexBuf.VertexArray(posa);
            this.nrma = new VertexBuf.NormalArray(nrma);
            this.sidx = sidx;
        }

        @Override
        public void draw(GOut g) {
            g.state(smat);
            g.apply();

            this.posa.bind(g, false);
            this.nrma.bind(g, false);
            this.sidx.rewind();
            g.gl.glDrawElements(GL.GL_TRIANGLES, this.sidx.limit(), GL.GL_UNSIGNED_SHORT, this.sidx);

            this.posa.unbind(g);
            this.nrma.unbind(g);
        }

        @Override
        public boolean setup(RenderList d) {
            d.prepo(Rendered.eyesort);
            d.prepo(Material.nofacecull);
            Location.goback(d.state(), "gobx");
            d.state().put(States.color, null);
            return true;
        }
    }
}
