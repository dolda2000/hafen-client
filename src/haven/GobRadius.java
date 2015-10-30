package haven;

import java.util.HashMap;
import java.util.Map;

public class GobRadius {
    private static final Map<String, Gob.Overlay> overlays;

    static {
        Indir<Resource> bprad = Resource.remote().load("gfx/fx/bprad");
        overlays = new HashMap<String, Gob.Overlay>();
        overlays.put("gfx/terobjs/minesupport", new Gob.Overlay(-1, bprad, new MessageBuf(new byte[] { -24, 3 }, 0, 2)));
        overlays.put("gfx/terobjs/column", new Gob.Overlay(-1, bprad, new MessageBuf(new byte[] { -30, 4 }, 0, 2)));
        overlays.put("gfx/terobjs/beehive", new Gob.Overlay(-1, bprad, new MessageBuf(new byte[] { -36, 5 }, 0, 2)));
    }

    public static void toggle(final OCache objects, boolean show) {
        try {
            synchronized (objects) {
                for (Gob gob : objects) {
                    Resource res = gob.getres();
                    if (res == null)
                        continue;
                    Gob.Overlay overlay = overlays.get(res.name);
                    if (overlay != null) {
                        if (show)
                            gob.ols.add(overlay);
                        else
                            gob.ols.remove(overlay);
                    }
                }
            }
        } catch (Loading e) {}
    }
}
