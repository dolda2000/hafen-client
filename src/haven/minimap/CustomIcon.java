package haven.minimap;

import haven.*;

import java.awt.*;

public abstract class CustomIcon {
    public static final CustomIcon none = new None();

    public abstract Tex tex();
    public abstract Color color();

    public static CustomIcon simple(Tex tex, Color color) {
        return new Simple(tex, color);
    }

    public static CustomIcon player(Gob gob) {
        return new Player(gob);
    }

    private static class None extends CustomIcon {
        public Tex tex() {
            return null;
        }

        public Color color() {
            return null;
        }
    }

    private static class Player extends CustomIcon {
        private static final Tex tex = Resource.loadtex("gfx/hud/mmap/pl");
        private final Gob gob;

        public Player(Gob gob) {
            this.gob = gob;
        }

        public Tex tex() {
            return tex;
        }

        public Color color() {
            KinInfo kinInfo = gob.getattr(KinInfo.class);
            return (kinInfo != null) ? BuddyWnd.gc[kinInfo.group] : Color.RED;
        }
    }

    private static class Simple extends CustomIcon {
        private final Tex tex;
        private final Color color;

        public Simple(Tex tex, Color color) {
            this.tex = tex;
            this.color = color;
        }

        public Tex tex() {
            return tex;
        }

        public Color color() {
            return color;
        }
    }
}
