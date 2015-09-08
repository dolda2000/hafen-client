package haven;

import java.util.*;

public class CraftList extends Listbox<CraftList.CraftItem> {
    private static final Text.Foundry font = new Text.Foundry(Text.fraktur, 15).aa(true);
    private final List<CraftItem> items = new ArrayList<CraftItem>();
    private final Map<Glob.Pagina, CraftItem> map = new HashMap<Glob.Pagina, CraftItem>();

    public CraftList(int w, int h) {
        super(w, h, font.height() + 2);
    }

    public void add(Glob.Pagina pagina) {
        CraftItem item = new CraftItem(pagina);
        map.put(pagina, item);
        items.add(item);
    }

    public void remove(Glob.Pagina pagina) {
        CraftItem item = map.remove(pagina);
        if (item != null)
            items.remove(item);
    }

    public void clear() {
        map.clear();
        items.clear();
    }

    public void sort(Comparator<CraftItem> comparator) {
        Collections.sort(items, comparator);
    }

    @Override
    protected CraftItem listitem(int i) {
        return items.get(i);
    }

    @Override
    protected int listitems() {
        return items.size();
    }

    @Override
    protected void drawbg(GOut g) {
        g.chcolor(0, 0, 0, 128);
        g.frect(Coord.z, sz);
        g.chcolor();
    }

    @Override
    protected void drawitem(GOut g, CraftItem item, int i) {
        g.image(item.icon, Coord.z);
        g.aimage(item.rname.tex(), new Coord(itemh + 5, itemh / 2), 0, 0.5);
    }

    public class CraftItem {
        public final String name;
        public final Text rname;
        public final Tex icon;
        public final String[] cmd;

        public CraftItem(Glob.Pagina pagina) {
            Resource.AButton act = pagina.act();
            this.name = act.name;
            this.cmd = act.ad;
            this.rname = font.render(name);
            this.icon = new TexI(PUtils.convolvedown(pagina.res.get().layer(Resource.imgc).img, new Coord(itemh, itemh), CharWnd.iconfilter));
        }
    }
}
