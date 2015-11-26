package haven;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class GameUILayout {
    private final List<Draggable> draggables = new ArrayList<Draggable>();
    private Coord sz;

    public void addDraggable(Widget widget, RelativePosition defaultPosition, boolean keepHorzOffset, boolean keepVertOffset) {
        draggables.add(new Draggable(widget, defaultPosition, keepHorzOffset, keepVertOffset));
        if (sz != null)
            clip(widget, sz);
    }

    public void removeDraggable(Widget widget) {
        Iterator<Draggable> i = draggables.iterator();
        while (i.hasNext()) {
            Draggable d = i.next();
            if (d.widget == widget) {
                i.remove();
                return;
            }
        }
    }

    public void update(Coord sz) {
        if (Coord.z.equals(sz))
            return;
        if (this.sz != null) {
            for (Draggable d : draggables) {
                int x = d.widget.c.x;
                int y = d.widget.c.y;

                if (d.keepHorzOffset && (x > this.sz.x - (d.widget.sz.x + x)))
                    x = sz.x - (this.sz.x - x);
                if (d.keepVertOffset && (y > this.sz.y - (d.widget.sz.y + y)))
                    y = sz.y - (this.sz.y - y);

                d.widget.c = new Coord(x, y);
                clip(d.widget, sz);
            }
            this.sz = sz;
        } else {
            // place widgets at default positions
            this.sz = sz;
            for (Draggable d : draggables) {
                if (d.widget.c.equals(Coord.z)) {
                    moveToPosition(d.widget, d.defaultPosition);
                }
                clip(d.widget, sz);
            }
        }
    }

    private void moveToPosition(Widget widget, RelativePosition pos) {
        Coord c = new Coord(pos.offset);
        if (pos.horz == HAlign.Right)
            c.x = sz.x - widget.sz.x - c.x;
        if (pos.vert == VAlign.Bottom)
            c.y = sz.y - widget.sz.y - c.y;
        widget.move(c);
    }

    private static void clip(Widget widget, Coord sz) {
        int x = Utils.clip(widget.c.x, 0, sz.x - widget.sz.x);
        int y = Utils.clip(widget.c.y, 0, sz.y - widget.sz.y);
        widget.move(x, y);
    }

    private static class Draggable {
        public final Widget widget;
        public final RelativePosition defaultPosition;
        private final boolean keepHorzOffset;
        private final boolean keepVertOffset;

        private Draggable(Widget widget, RelativePosition defaultPosition, boolean keepHorzOffset, boolean keepVertOffset) {
            this.widget = widget;
            this.defaultPosition = defaultPosition;
            this.keepHorzOffset = keepHorzOffset;
            this.keepVertOffset = keepVertOffset;
        }
    }

    public static class RelativePosition {
        public final HAlign horz;
        public final VAlign vert;
        public final Coord offset;

        public RelativePosition(HAlign horz, VAlign vert, Coord offset) {
            this.horz = horz;
            this.vert = vert;
            this.offset = offset;
        }
    }

    public enum HAlign {
        None,
        Left,
        Center,
        Right
    }

    public enum VAlign {
        None,
        Top,
        Center,
        Bottom
    }
}
