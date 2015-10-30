package haven;

import java.util.ArrayList;
import java.util.List;

public class CustomSettingsPanel extends OptWnd.Panel {
    private static final Text.Foundry categoryFont = new Text.Foundry(Text.fraktur, 15).aa(true);

    private final CategoryList categoryList;
    private final Frame frame;

    public CustomSettingsPanel(final OptWnd opts) {
        this.categoryList = add(new CategoryList(120, 15));
        this.frame = add(new Frame(300, categoryList.sz.y), categoryList.sz.x + 5, 0);

        categoryList.add(new Category(0, "General", createGeneral()));
        categoryList.add(new Category(0, "Audio", createAudio()));
        categoryList.add(new Category(0, "Display", null));
        categoryList.add(new Category(1, "Game", createGameDisplay()));
        categoryList.add(new Category(1, "UI", createGameUI()));
        categoryList.change(0);

        add(opts.new PButton(80, "Back", 27, opts.main), frame.c.x + frame.sz.x - 80, frame.sz.y + 5);
        pack();
    }

    private static Widget createGeneral() {
        Widget panel = new Widget();
        int y = 0;
        panel.add(new PrefCheckBox("Enable account storing (requires restart)", Config.enableAccountStoring), new Coord(0, y));
        y += 20;
        panel.add(new PrefCheckBox("Enable minimap storing", Config.minimapEnableSave), new Coord(0, y));
        y += 20;
        panel.add(new PrefCheckBox("Enable auto hearth", Config.enableAutoHearth), new Coord(0, y));
        y += 20;
        panel.add(new PrefCheckBox("Enable free camera rotation", Config.snapCamera, true), new Coord(0, y));
        y += 20;
        panel.add(new PrefCheckBox("Enable kin status notifications", Config.showKinNotifications), new Coord(0, y));
        y += 20;
        panel.add(new PrefCheckBox("Play alarm for unknown or RED players", Config.enableStrangerAlarm), new Coord(0, y));
        y += 20;
        panel.add(new PrefCheckBox("Toggle tracking on startup", Config.toggleTracking), new Coord(0, y));
        y += 20;
        panel.add(new PrefCheckBox("Use CTRL instead of ALT for quality transfer", Config.useControlForSortTransfer), new Coord(0, y));
        y += 20;
        panel.add(new PrefCheckBox("Display timestamps in chat log", Config.showChatTimestamps), new Coord(0, y));
        panel.pack();
        return panel;
    }

    private static Widget createAudio() {
        Widget panel = new Widget();
        int y = 0;
        panel.add(new Label("Alarm volume"), new Coord(0, y));
        y += 15;
        panel.add(new HSlider(280, 0, 1000, Config.alarmVolume.get()) {
            public void changed() {
                Config.alarmVolume.set(val);
            }
        }, new Coord(0, y));
        panel.pack();
        return panel;
    }

    private static Widget createGameDisplay() {
        Widget panel = new Widget();
        int y = 0;
        panel.add(new PrefCheckBox("Hide flavor objects", Config.showFlavor, true), new Coord(0, y));
        y += 20;
        panel.add(new PrefCheckBox("Simple crops (requires relogin)", Config.simplifyCrops), new Coord(0, y));
        y += 20;
        panel.add(new PrefCheckBox("Display growth stage on grown trees", Config.showGobInfoForGrownTrees), new Coord(0, y));
        y += 20;
        panel.add(new PrefCheckBox("Highlight party members", Config.highlightParty), new Coord(0, y));
        y += 20;
        panel.add(new PrefCheckBox("Display additional defense bars", Config.showCustomDefenseBars), new Coord(0, y));
        y += 20;
        panel.add(new PrefCheckBox("Display server grid", Config.showServerGrid) {
            public void set(boolean val) {
                super.set(val);
                if (ui != null && ui.gui != null)
                    ui.gui.map.serverGridOverlay.setVisible(val);
            }
        }, new Coord(0, y));
        y += 20;
        panel.add(new Label("Cupboards scale (requires restart):"), new Coord(0, y));
        y += 15;
        final Label sc = panel.add(new Label(""), new Coord(260, y));
        panel.add(new HSlider(255, 10, 100, Config.cupboardScale.get()) {
            protected void added() {
                dpy();
                this.c.y = sc.c.y + ((sc.sz.y - this.sz.y) / 2);
            }

            void dpy() {
                sc.settext(String.format("%d%%", val));
            }

            public void changed() {
                dpy();
                Config.cupboardScale.set(val);
            }
        }, new Coord(0, y));
        y += 20;
        panel.add(new Label("Item quality display mode:"), new Coord(0, y));
        y += 15;
        panel.add(new Label("All"), new Coord(0, y));
        panel.add(new Label("Avg"), new Coord(130, y));
        panel.add(new Label("Max"), new Coord(265, y));
        y += 10;
        panel.add(new HSlider(280, 0, 2, 0) {
            protected void attach(UI ui) {
                super.attach(ui);
                val = Config.showQualityMode.get();
            }
            public void changed() {
                Config.showQualityMode.set(val);
            }
        }, new Coord(0, y));
        panel.pack();
        return panel;
    }

    private static Widget createGameUI() {
        Widget panel = new Widget();
        int y = 0;
        panel.add(new PrefCheckBox("Display hunger meter", Config.showHungerMeter) {
            public void set(boolean val) {
                super.set(val);
                GameUI ui = getparent(GameUI.class);
                if (ui == null) return;
                if (val)
                    ui.addcmeter(new HungerMeter(ui.chrwdg.glut));
                else
                    ui.delcmeter(HungerMeter.class);
            }
        }, new Coord(0, y));
        y += 20;
        panel.add(new PrefCheckBox("Display FEP meter", Config.showFepMeter) {
            public void set(boolean val) {
                super.set(val);
                GameUI ui = getparent(GameUI.class);
                if (ui == null) return;
                if (val)
                    ui.addcmeter(new FepMeter(ui.chrwdg.feps));
                else
                    ui.delcmeter(FepMeter.class);
            }
        }, new Coord(0, y));
        y += 20;
        panel.add(new PrefCheckBox("Display game time indicator", Config.showClock) {
            public void set(boolean val) {
                super.set(val);
                GameUI ui = getparent(GameUI.class);
                if (ui != null)
                    ui.cal.show(val);
            }
        }, new Coord(0, y));
        y += 20;
        panel.add(new PrefCheckBox("Disable flower menu animations", Config.enableMenuAnimation, true), new Coord(0, y));
        y += 20;
        panel.add(new PrefCheckBox("Display additional belt for Fn keys", Config.showCustomFKeysBelt) {
            public void set(boolean val) {
                super.set(val);
                if (ui != null && ui.gui != null)
                    ui.gui.fkeybelt.show(val);
            }
        }, new Coord(0, y));
        panel.pack();
        return panel;
    }

    private static class Frame extends Widget {
        private static final IBox box = new IBox("gfx/hud/tab", "tl", "tr", "bl", "br", "extvl", "extvr", "extht", "exthb");
        private static final Coord margin = new Coord(10, 10);

        private Widget content;

        public Frame(int width, int height) {
            super(new Coord(width, height));
        }

        @Override
        public void draw(GOut g) {
            super.draw(g);
            box.draw(g, Coord.z, sz);
        }

        @Override
        public Coord xlate(Coord c, boolean in) {
            return in ? c.add(margin) : c.sub(margin);
        }

        public void setContent(Widget widget) {
            if (content != null) content.destroy();
            content = widget;
            if (content != null) add(content);
        }
    }

    private class CategoryList extends Listbox<Category> {
        private final List<Category> categories;

        public CategoryList(int width, int height) {
            super(width, height, categoryFont.height() + 2);
            categories = new ArrayList<Category>();
        }

        public void add(Category category) {
            categories.add(category);
        }

        @Override
        public void change(int index) {
            if (index >= 0 && index < categories.size()) {
                super.change(index);
                Category selected = categories.get(index);
                frame.setContent(selected.widget);
            }
        }

        @Override
        protected Category listitem(int i) {
            return categories.get(i);
        }

        @Override
        protected int listitems() {
            return categories.size();
        }

        @Override
        protected void drawbg(GOut g) {
            g.chcolor(0, 0, 0, 128);
            g.frect(Coord.z, sz);
            g.chcolor();
        }

        @Override
        protected void drawitem(GOut g, Category item, int i) {
            g.aimage(item.name.tex(), new Coord(5 + item.level * 10, itemh / 2), 0, 0.5);
        }
    }

    private static class Category {
        public int level;
        public final Text name;
        public final Widget widget;

        private Category(int level, String name, Widget widget) {
            this.level = level;
            this.name = categoryFont.render(name);
            this.widget = widget;
        }
    }

    public static class PrefCheckBox extends CheckBox {
        private final Config.Pref<Boolean> pref;
        private final boolean invert;

        public PrefCheckBox(String label, Config.Pref<Boolean> pref, boolean invert) {
            super(label);
            this.pref = pref;
            this.a = pref.get() ^ invert;
            this.invert = invert;
        }

        public PrefCheckBox(String label, Config.Pref<Boolean> pref) {
            this(label, pref, false);
        }

        @Override
        public void set(boolean val) {
            pref.set(val ^ invert);
            a = val;
        }
    }
}
