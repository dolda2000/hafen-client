package haven;

import java.util.HashMap;
import java.util.Map;

public class CraftWindow extends Window {
    private static final IBox frame = new IBox("gfx/hud/tab", "tl", "tr", "bl", "br", "extvl", "extvr", "extht", "exthb");
    private final TabStrip tabStrip;
    private final Map<String, TabStrip.Button> tabs = new HashMap<String, TabStrip.Button>();
    private Widget makeWidget;
    private Glob.Pagina lastAction;

    public CraftWindow() {
        super(Coord.z, "Crafting");
        tabStrip = add(new TabStrip() {
            protected void selected(Button button) {
                for (Map.Entry<String, Button> entry : tabs.entrySet()) {
                    Glob.Pagina pagina = (Glob.Pagina)entry.getValue().tag;
                    if (entry.getValue().equals(button) && pagina != lastAction) {
                        ui.gui.wdgmsg("act", (Object[])pagina.act().ad);
                        lastAction = null;
                        break;
                    }
                }
            }
        });
        setLocal(true);
        setHideOnClose(true);
        setfocusctl(true);
    }

    public void setLastAction(Glob.Pagina value) {
        lastAction = value;
    }

    @Override
    public void wdgmsg(Widget sender, String msg, Object... args) {
        if ((sender == this) && msg.equals("close")) {
            if (makeWidget != null)
                makeWidget.wdgmsg("close");
        }
        super.wdgmsg(sender, msg, args);
    }

    @Override
    public <T extends Widget> T add(T child) {
        child = super.add(child);
        if (child instanceof Makewindow) {
            if (lastAction != null) {
                addTab(lastAction);
            }
            makeWidget = child;
            makeWidget.c = new Coord(5, tabStrip.sz.y + 5);
            makeWidget.resize(Math.max(makeWidget.sz.x, tabStrip.sz.x), makeWidget.sz.y);
        }
        return child;
    }

    @Override
    public void cdestroy(Widget w) {
        if (makeWidget == w) {
            makeWidget = null;
            if (visible)
                hide();
        }
    }

    @Override
    public void cdraw(GOut g) {
        super.cdraw(g);
        frame.draw(g, new Coord(0, Math.max(0, tabStrip.sz.y - 1)), asz.sub(0, tabStrip.sz.y));
    }

    @Override
    public void resize(Coord sz) {
        super.resize(sz.add(5, 5));
    }

    @Override
    public boolean globtype(char ch, java.awt.event.KeyEvent ev) {
        if (visible && ch == 9 && ev.isShiftDown()) {
            int nextIndex = tabStrip.getSelectedButtonIndex() + 1;
            if (nextIndex >= tabStrip.getButtonCount())
                nextIndex = 0;
            tabStrip.select(nextIndex);
            return true;
        }
        return super.globtype(ch, ev);
    }

    @Override
    public void hide() {
        super.hide();
        if (makeWidget != null)
            makeWidget.wdgmsg("close");
    }

    private void addTab(Glob.Pagina pagina) {
        String resName = pagina.res().name;
        if (tabs.containsKey(resName)) {
            TabStrip.Button old = tabs.get(resName);
            tabStrip.remove(old);
        }
        Tex icon = new TexI(PUtils.convolvedown(pagina.res.get().layer(Resource.imgc).img, new Coord(20, 20), CharWnd.iconfilter));
        String text = pagina.act().name;
        if (text.length() > 12)
            text = text.substring(0, 12 - 2) + "..";
        TabStrip.Button added = tabStrip.insert(0, icon, text, pagina.act().name);
        added.tag = pagina;
        tabStrip.select(added);
        if (tabStrip.getButtonCount() > 4) {
            removeTab(tabStrip.getButtonCount() - 1);
        }
        tabs.put(resName, added);
    }

    private void removeTab(int index) {
        TabStrip.Button removed = tabStrip.remove(index);
        tabs.values().remove(removed);
    }
}
