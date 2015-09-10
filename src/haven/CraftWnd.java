package haven;

import haven.util.CollectionListener;

import java.awt.event.KeyEvent;
import java.util.*;

public class CraftWnd extends Window {
    private static final int WIDTH = 200;

    private final TextEntry entry;
    private final List<Glob.Pagina> all = new ArrayList<Glob.Pagina>();
    private final CraftList list;
    private PaginaeListener listener;

    public CraftWnd() {
        super(Coord.z, "Craft...");
        setcanfocus(true);
        setfocusctl(true);
        entry = add(new TextEntry(WIDTH, "") {
            @Override
            public void activate(String text) {
                getparent(GameUI.class).menu.wdgmsg("act", ((Object[])list.sel.cmd));
            }

            @Override
            protected void changed() {
                super.changed();
                refilter();
            }

            @Override
            public boolean keydown(KeyEvent e) {
                if(e.getKeyCode() == KeyEvent.VK_UP) {
                    list.change(Math.max(list.selindex - 1, 0));
                    list.showsel();
                    return true;
                } else if(e.getKeyCode() == KeyEvent.VK_DOWN) {
                    list.change((Math.min(list.selindex + 1, list.listitems() - 1)));
                    list.showsel();
                    return true;
                } else {
                    return super.keydown(e) ;
                }
            }
        });
        setfocus(entry);
        list = add(new CraftList(WIDTH, 10) {
            @Override
            protected void itemactivate(CraftItem item) {
                getparent(GameUI.class).menu.wdgmsg("act", ((Object[])list.sel.cmd));
            }
        }, 0, entry.sz.y + 5);


        pack();
    }

    @Override
    public void show() {
        super.show();
        entry.settext("");
        list.change(0);
        list.showsel();
        parent.setfocus(this);
    }

    @Override
    public void lostfocus() {
        super.lostfocus();
        hide();
    }

    @Override
    public void tick(double dt) {
        if (ui == null)
            return;

        if (listener == null) {
            // make initial list
            all.clear();
            synchronized (ui.sess.glob.paginae) {
                for (Glob.Pagina pagina : ui.sess.glob.paginae) {
                    if (iscraft(pagina))
                        all.add(pagina);
                }
                listener = new PaginaeListener();
                ui.sess.glob.paginae.addListener(listener);
                refilter();
            }
        }
    }

    @Override
    public void wdgmsg(Widget sender, String msg, Object... args) {
        if((sender == this) && (msg.equals("close"))) {
            hide();
        } else {
            super.wdgmsg(sender, msg, args);
        }
    }

    private void refilter() {
        list.clear();
        for (Glob.Pagina p : all) {
            if (p.res.get().layer(Resource.action).name.toLowerCase().contains(entry.text.toLowerCase()))
                list.add(p);
        }
        list.sort(new ItemComparator());
        if (list.listitems() > 0) {
            list.change(Math.min(list.selindex, list.listitems() - 1));
            list.sb.val = 0;
            list.showsel();
        }
    }

    private class PaginaeListener implements CollectionListener<Glob.Pagina> {
        @Override
        public void onItemAdded(Glob.Pagina item) {
            if (iscraft(item)) {
                all.add(item);
                refilter();
            }
        }

        @Override
        public void onItemRemoved(Glob.Pagina item) {
            all.remove(item);
            refilter();
        }
    }

    private class ItemComparator implements Comparator<CraftList.CraftItem> {
        @Override
        public int compare(CraftList.CraftItem a, CraftList.CraftItem b) {
            return a.name.compareTo(b.name);
        }
    }

    private static boolean iscraft(Glob.Pagina pagina) {
        Resource res = null;
        try {
            res = pagina.res();
        } catch (Loading e) {
            try {
                e.waitfor();
                res = pagina.res();
            } catch (InterruptedException ex) {}
        }
        return (res != null) && res.name.startsWith("paginae/craft/");
    }
}
