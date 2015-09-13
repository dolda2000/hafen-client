package haven;

import java.util.ArrayList;
import java.util.List;

public class CheckBoxList extends Listbox<CheckBox> {
    private static final Coord padding = new Coord(2, 2);

    private final List<CheckBox> boxes = new ArrayList<CheckBox>();
    private final List<Text> labels = new ArrayList<Text>();
    private final List<CheckBoxListListener> listeners = new ArrayList<CheckBoxListListener>();

    public CheckBoxList(int w, int h) {
        super(w, h, Text.std.height() + padding.y * 2);
    }

    public void addItem(String label, boolean checked) {
        final int index = boxes.size();
        CheckBox cb = add(new CheckBox("") {
            public void changed(boolean a) {
                for (CheckBoxListListener listener : listeners)
                    listener.itemChecked(index, a);
            }
        });
        cb.a = checked;
        boxes.add(cb);
        labels.add(Text.std.render(label));
    }

    public void clear() {
        for (CheckBox box : boxes)
            box.destroy();
        boxes.clear();
        labels.clear();
        change(-1);
        showsel();
    }

    public void addListener(CheckBoxListListener listener) {
        listeners.add(listener);
    }

    public void removeListener(CheckBoxListListener listener) {
        listeners.remove(listener);
    }

    @Override
    public void change(int index) {
        super.change(index);
        for (CheckBoxListListener listener : listeners)
            listener.itemSelected(index);
    }

    @Override
    public void draw(GOut g) {
        for (CheckBox box : boxes)
            box.hide();
        super.draw(g);
    }

    @Override
    protected CheckBox listitem(int i) {
        return boxes.get(i);
    }

    @Override
    protected int listitems() {
        return boxes.size();
    }

    @Override
    protected void drawbg(GOut g) {
        g.chcolor(0, 0, 0, 128);
        g.frect(Coord.z, sz);
        g.chcolor();
    }

    @Override
    protected void drawitem(GOut g, CheckBox cb, int i) {
        g = g.reclip(padding, sz.sub(padding.mul(2)));
        cb.c = rootxlate(g.tx);
        cb.show();
        // draw label here because clicks on it shouldn't change checkbox state
        Text label = labels.get(i);
        g.image(label.tex(), cb.loff.add(cb.box.sz().x, cb.box.sz().y - label.sz().y));
    }
}
