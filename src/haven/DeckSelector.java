package haven;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;

public abstract class DeckSelector extends Window {
    private static final int MAX_NUMBER = 9;
    private static final int MIN_WIDTH = 150;

    private Label[] deckNumbers = new Label[0];
    private Label[] decks = new Label[0];
    private int selected = -1;

    public DeckSelector() {
        super(Coord.z, "Switch deck...");
    }

    public void select(int index) {
    }

    public abstract int getSelected();

    @Override
    public void cdraw(GOut g) {
        if (selected != -1) {
            Widget label = deckNumbers[selected];
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
        if (num >= 0 && num < decks.length) {
            selected = num;
            return true;
        }
        return false;
    }

    @Override
    public boolean keyup(KeyEvent ev) {
        int num = (ev.getKeyChar() - '1');
        if (num >= 0 && num < decks.length) {
            select(num);
            hide();
            return true;
        }
        return false;
    }

    private void updateDecks() {
        // clear
        selected = -1;
        for (int i = 0; i < decks.length; i++) {
            deckNumbers[i].destroy();
            decks[i].destroy();
        }
        // update
        List<String> deckNames = filterEmpty(Config.getDeckNames(ui.sess.username, ui.sess.charname).get());
        int count = Math.min(MAX_NUMBER, deckNames.size());
        deckNumbers = new Label[count];
        decks = new Label[count];
        int y = 0;
        for (int i = 0; i < count; i++) {
            deckNumbers[i] = add(new Label(String.format("(%d)", i + 1), CharWnd.attrf), 2, y);
            deckNumbers[i].setcolor(Color.YELLOW);
            decks[i] = add(new Label(deckNames.get(i), CharWnd.attrf), 28, y);
            y += 20;
        }
        resize(Math.max(contentsz().x, MIN_WIDTH), contentsz().y);
        selected = getSelected();
    }

    private static List<String> filterEmpty(String[] array) {
        ArrayList<String> result = new ArrayList<String>(array.length);
        for (String str : array)
            if (str != null && !str.isEmpty())
                result.add(str);
        return result;
    }
}
