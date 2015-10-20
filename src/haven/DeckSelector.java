package haven;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;

public abstract class DeckSelector extends Window {
    private static final int MAX_DECK_COUNT = 9;
    private static final int MIN_WIDTH = 150;

    private DeckList deckList;

    public DeckSelector() {
        super(Coord.z, "Switch deck...");
        setLocal(true);
        setHideOnClose(true);
    }

    protected abstract int getSelectedDeck();
    protected abstract void setSelectedDeck(int deckIndex);

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
    public boolean keydown(KeyEvent e) {
        int keyCode = e.getKeyCode();
        if (keyCode == KeyEvent.VK_UP) {
            int index = deckList.selindex - 1;
            if (index < 0)
                index = deckList.listitems() - 1;
            deckList.change(index);
            return true;
        } else if (keyCode == KeyEvent.VK_DOWN) {
            int index = deckList.selindex + 1;
            if (index >= deckList.listitems())
                index = 0;
            deckList.change(index);
            return true;
        } else if (keyCode == KeyEvent.VK_ENTER) {
            if (deckList.sel != null) {
                selectDeck(deckList.sel);
                return true;
            }
        } else {
            int num = (e.getKeyChar() - '1');
            if (num >= 0 && num < deckList.listitems()) {
                deckList.change(num);
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean keyup(KeyEvent ev) {
        int num = (ev.getKeyChar() - '1');
        if (num >= 0 && num < deckList.listitems()) {
            selectDeck(deckList.listitem(num));
            return true;
        }
        return false;
    }

    private void selectDeck(Deck deck) {
        setSelectedDeck(deck.deckIndex);
        hide();
    }

    private void updateDecks() {
        String[] all = Config.getDeckNames(ui.sess.username, ui.sess.charname).get();
        List<String> filtered = filterEmpty(all);

        int selectedDeck = getSelectedDeck();
        int count = Math.min(MAX_DECK_COUNT, filtered.size());
        int sel = -1;

        Deck[] decks = new Deck[count];
        for (int i = 0; i < count; i++) {
            String name = filtered.get(i);
            int index = Utils.indexOf(all, name);
            decks[i] = new Deck(index, i + 1, name);
            if (index == selectedDeck)
                sel = i;
        }

        if (deckList != null)
            deckList.destroy();
        deckList = add(new DeckList(decks));
        deckList.change(sel);
        pack();
    }

    private static List<String> filterEmpty(String[] array) {
        ArrayList<String> result = new ArrayList<String>(array.length);
        for (String str : array)
            if (str != null && !str.isEmpty())
                result.add(str);
        return result;
    }

    private class DeckList extends Listbox<Deck> {
        private final Deck[] decks;

        public DeckList(Deck[] decks) {
            super(MIN_WIDTH, decks.length, CharWnd.attrf.height() + 2);
            this.decks = decks;
        }

        @Override
        protected void itemactivate(Deck item) {
            selectDeck(item);
        }

        @Override
        protected Deck listitem(int i) {
            return decks[i];
        }

        @Override
        protected int listitems() {
            return decks.length;
        }

        @Override
        protected void drawbg(GOut g) {
        }

        @Override
        protected void drawsel(GOut g) {
            g.chcolor(0, 0, 0, 128);
            g.frect(Coord.z, g.sz);
            g.chcolor();
        }

        @Override
        protected void drawitem(GOut g, Deck deck, int i) {
            if (deck != null) {
                g.image(deck.numText.tex(), c);
                g.image(deck.name.tex(), c.add(28, 0));
            }
        }
    }

    private static class Deck {
        public final int deckIndex;
        public final int num;
        public final Text name;
        public final Text numText;

        public Deck(int index, int num, String name) {
            this.deckIndex = index;
            this.num = num;
            this.name = CharWnd.attrf.render(name);
            this.numText = CharWnd.attrf.render(String.format("(%d)", num), Color.YELLOW);
        }
    }
}
