package haven;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class TabStrip extends Widget {
    private final List<Button> buttons = new ArrayList<Button>();
    private Button selected;

    protected void selected(Button button) {}

    public int getSelectedButtonIndex() {
        return buttons.indexOf(selected);
    }

    public int getButtonCount() {
        return buttons.size();
    }

    public Button insert(int index, Tex image, String text, String tooltip) {
        final Button button = add(new Button(image, text) {
            public void click() {
                select(this);
            }
        });
        button.tooltip = tooltip;
        buttons.add(index, button);
        alignButtons();
        return button;
    }

    public void select(int buttonIndex) {
        select(buttons.get(buttonIndex));
    }

    public void select(Button button) {
        if (selected != button) {
            for (Button b : buttons) {
                b.setActive(b == button);
            }
            selected = button;
            selected(button);
        }
    }

    public Button remove(int buttonIndex) {
        Button button = buttons.remove(buttonIndex);
        button.destroy();
        alignButtons();
        return button;
    }

    public void remove(Button button) {
        if (buttons.remove(button)) {
            button.destroy();
            alignButtons();
        }
    }

    private void alignButtons() {
        int x = 0;
        for (Button button : buttons) {
            button.c = new Coord(x, 0);
            x += button.sz.x - 1;
        }
        pack();
    }

    public abstract static class Button extends Widget {
        public static final Coord padding = new Coord(5, 2);
        public static final Text.Foundry font = new Text.Foundry(Text.fraktur, 14).aa(true);
        private static final IBox frame = new IBox("gfx/hud/tab", "tl", "tr", "bl", "br", "extvl", "extvr", "extht", "exthb");
        private static final Color bg = new Color(0, 0, 0, 128);
        private Tex image;
        private Text text;
        private boolean active;

        Button(Tex image, String text) {
            this.image = image;
            this.text = font.render(text);
            int w = this.text.sz().x + this.image.sz().x + padding.x * 2;
            if (text != null && !text.isEmpty())
                w += 10; // space between image and text
            int h = Math.max(this.text.sz().y, this.image.sz().y) + padding.y * 2;
            resize(w, h);
        }

        public abstract void click();

        @Override
        public void draw(GOut g) {
            if (active) {
                g.chcolor(bg);
                g.frect(Coord.z, sz);
                g.chcolor();
            }
            frame.draw(g, Coord.z, sz);
            g.image(image, padding);
            g.image(text.tex(), new Coord(image.sz().x + 10, padding.y));
        }

        @Override
        public boolean mousedown(Coord c, int button) {
            if (button == 1) {
                click();
                return true;
            }
            return false;
        }

        void setActive(boolean value) {
            this.active = value;
        }
    }
}
