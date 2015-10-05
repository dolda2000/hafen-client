package haven;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;

public abstract class DeckSelector extends Window {
    private static final int MAX_NUMBER = 9;
    private static final int MIN_WIDTH = 150;

    private Deck[] decks = new Deck[0];
    private int selected = -1;

    public DeckSelector() {
        super(Coord.z, "Switch deck...");
    }

    public abstract void select(int index);
    public abstract int getSelected();

    @Override
    public void cdraw(GOut g) {
        Coord c = new Coord(2, 0);
        for (Deck deck : decks) {
            if (deck.num - 1 == selected) {
                g.chcolor(0, 0, 0, 128);
                g.frect(new Coord(0, c.y), new Coord(csz.x, deck.name.sz().y));
                g.chcolor();
            }
            g.image(deck.numText.tex(), c);
            g.image(deck.name.tex(), c.add(28, 0));
            c.y += 20;
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
            select(decks[num].index);
            hide();
            return true;
        }
        return false;
    }

    private void updateDecks() {
        int selectedDeckIndex = getSelected();
        String[] all = Config.getDeckNames(ui.sess.username, ui.sess.charname).get();
        List<String> filtered = filterEmpty(all);
        int count = Math.min(MAX_NUMBER, filtered.size());
        decks = new Deck[count];
        int w = MIN_WIDTH;
        int h = 0;
        for (int i = 0; i < count; i++) {
            String name = filtered.get(i);
            int index = Utils.indexOf(all, name);
            if (index == selectedDeckIndex)
                selected = i;
            decks[i] = new Deck(index, i + 1, name);
            w = Math.max(decks[i].name.sz().x, MIN_WIDTH);
            h += decks[i].name.sz().y;
        }
        resize(w, h);
    }

    private static List<String> filterEmpty(String[] array) {
        ArrayList<String> result = new ArrayList<String>(array.length);
        for (String str : array)
            if (str != null && !str.isEmpty())
                result.add(str);
        return result;
    }

    private static class Deck {
        public final int index;
        public final int num;
        public final Text name;
        public final Text numText;

        public Deck(int index, int num, String name) {
            this.index = index;
            this.num = num;
            this.name = CharWnd.attrf.render(name);
            this.numText = CharWnd.attrf.render(String.format("(%d)", num), Color.YELLOW);
        }
    }
}
