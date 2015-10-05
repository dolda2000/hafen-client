package haven;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;

public abstract class DeckSelector extends Window {
    private static final int MAX_NUMBER = 9;
    private static final int MIN_WIDTH = 150;

    private int[] indexes = new int[0];
    private Label[] numLabels = new Label[0];
    private Label[] deckLabels = new Label[0];
    private int selected = -1;

    public DeckSelector() {
        super(Coord.z, "Switch deck...");
    }

    public void select(int index) {
    }

    public abstract int getSelected();

    @Override
    public void cdraw(GOut g) {
        if (selected >= 0) {
            Widget label = numLabels[selected];
            g.chcolor(0, 0, 0, 128);
            g.frect(new Coord(0, label.c.y), new Coord(csz.x, label.sz.y));
            g.chcolor();
        }
    }

    @Override
    public void show() {
        super.show();
        parent.setfocus(this);
        updateDecks();
    }

    @Override
    public void lostfocus() {
        super.lostfocus();
        hide();
    }

    @Override
    public void wdgmsg(Widget sender, String msg, Object... args) {
        if((sender == this) && (msg.equals("close"))) {
            hide();
        } else {
            super.wdgmsg(sender, msg, args);
        }
    }

    @Override
    public boolean keydown(KeyEvent ev) {
        int num = (ev.getKeyChar() - '1');
        if (num >= 0 && num < deckLabels.length) {
            selected = num;
            return true;
        }
        return false;
    }

    @Override
    public boolean keyup(KeyEvent ev) {
        int num = (ev.getKeyChar() - '1');
        if (num >= 0 && num < deckLabels.length) {
            select(indexes[num]);
            hide();
            return true;
        }
        return false;
    }

    private void updateDecks() {
        // clear
        selected = -1;
        for (int i = 0; i < deckLabels.length; i++) {
            numLabels[i].destroy();
            deckLabels[i].destroy();
        }
        // update
        String[] all = Config.getDeckNames(ui.sess.username, ui.sess.charname).get();
        List<String> filtered = filterEmpty(all);
        int count = Math.min(MAX_NUMBER, filtered.size());
        numLabels = new Label[count];
        deckLabels = new Label[count];
        indexes = new int[count];
        int sel =  getSelected();
        int y = 0;
        for (int i = 0; i < count; i++) {
            indexes[i] = Utils.indexOf(all, filtered.get(i));
            if (indexes[i] == sel)
                selected = i;
            numLabels[i] = add(new Label(String.format("(%d)", i + 1), CharWnd.attrf), 2, y);
            numLabels[i].setcolor(Color.YELLOW);
            deckLabels[i] = add(new Label(filtered.get(i), CharWnd.attrf), 28, y);
            y += 20;
        }
        resize(Math.max(contentsz().x, MIN_WIDTH), contentsz().y);
    }

    private static List<String> filterEmpty(String[] array) {
        ArrayList<String> result = new ArrayList<String>(array.length);
        for (String str : array)
            if (str != null && !str.isEmpty())
                result.add(str);
        return result;
    }
}
