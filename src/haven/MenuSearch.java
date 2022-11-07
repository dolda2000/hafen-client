package haven;

import java.util.*;
import java.awt.image.BufferedImage;
import java.awt.event.KeyEvent;
import haven.MenuGrid.Pagina;
import haven.MenuGrid.PagButton;

public class MenuSearch extends Window {
    public final MenuGrid menu;
    public final Results rls;
    public final TextEntry sbox;
    private Pagina root;
    private List<Result> cur = Collections.emptyList();
    private List<Result> filtered = Collections.emptyList();
    private boolean recons = false;

    public class Result {
	public final PagButton btn;

	private Result(PagButton btn) {
	    this.btn = btn;
	}
    }

    private static final Text.Foundry elf = CharWnd.attrf;
    private static final int elh = elf.height() + UI.scale(2);
    public class Results extends SListBox<Result, Widget> {
	private Results(Coord sz) {
	    super(sz, elh);
	}

	protected List<Result> items() {return(filtered);}

	protected Widget makeitem(Result el, int idx, Coord sz) {
	    return(new ItemWidget<Result>(this, sz, el) {
		    {
			add(new IconText(sz) {
				protected BufferedImage img() {return(item.btn.img());}
				protected String text() {return(el.btn.name());}
				protected int margin() {return(0);}
				protected Text.Foundry foundry() {return(elf);}
			    }, Coord.z);
		    }

		    private double lastcl = 0;
		    public boolean mousedown(Coord c, int button) {
			boolean psel = sel == item;
			super.mousedown(c, button);
			double now = Utils.rtime();
			if(psel) {
			    if(now - lastcl < 0.5)
				menu.use(item.btn, new MenuGrid.Interaction(1, ui.modflags()), false);
			}
			lastcl = now;
			return(true);
		    }
		});
	}
    }

    public MenuSearch(MenuGrid menu) {
	super(Coord.z, "Action search");
	this.menu = menu;
	rls = add(new Results(UI.scale(250, 500)), Coord.z);
	sbox = add(new TextEntry(UI.scale(250), "") {
		protected void changed() {
		    refilter();
		}

		public void activate(String text) {
		    if(rls.sel != null)
			menu.use(rls.sel.btn, new MenuGrid.Interaction(1, ui.modflags()), false);
		    if(!ui.modctrl)
			MenuSearch.this.wdgmsg("close");
		}
	    }, 0, rls.sz.y);
	pack();
	setroot(null);
    }

    private void refilter() {
	List<Result> found = new ArrayList<>();
	String needle = sbox.text().toLowerCase();
	for(Result res : this.cur) {
	    if(res.btn.name().toLowerCase().indexOf(needle) >= 0)
		found.add(res);
	}
	this.filtered = found;
	int idx = filtered.indexOf(rls.sel);
	if(idx < 0) {
	    if(filtered.size() > 0) {
		rls.change(filtered.get(0));
		rls.display(0);
	    }
	} else {
	    rls.display(idx);
	}
    }

    private void updlist() {
	recons = false;
	Pagina root = this.root;
	List<PagButton> found = new ArrayList<>();
	{
	    Collection<Pagina> leaves = new ArrayList<>();
	    synchronized(menu.paginae) {
		leaves.addAll(menu.paginae);
	    }
	    for(Pagina pag : leaves) {
		try {
		    if(root == null) {
			found.add(pag.button());
		    } else {
			for(Pagina parent = pag; parent != null; parent = menu.paginafor(parent.act().parent)) {
			    if(parent == root) {
				found.add(pag.button());
				break;
			    }
			}
		    }
		} catch(Loading l) {
		    recons = true;
		}
	    }
	}
	Collections.sort(found, Comparator.comparing(PagButton::name));
	Map<PagButton, Result> prev = new HashMap<>();
	for(Result pr : this.cur)
	    prev.put(pr.btn, pr);
	List<Result> results = new ArrayList<>();
	for(PagButton btn : found) {
	    Result pr = prev.get(btn);
	    if(pr != null)
		results.add(pr);
	    else
		results.add(new Result(btn));
	}
	this.cur = results;
	refilter();
    }

    public void setroot(Pagina nr) {
	root = nr;
	updlist();
	rls.sb.val = 0;
    }

    public void tick(double dt) {
	if(menu.cur != root)
	    setroot(menu.cur);
	if(recons)
	    updlist();
	super.tick(dt);
    }

    public boolean keydown(KeyEvent ev) {
	if(ev.getKeyCode() == KeyEvent.VK_DOWN) {
	    int idx = filtered.indexOf(rls.sel);
	    if((idx >= 0) && (idx < filtered.size() - 1)) {
		idx++;
		rls.change(filtered.get(idx));
		rls.display(idx);
	    }
	    return(true);
	} else if(ev.getKeyCode() == KeyEvent.VK_UP) {
	    int idx = filtered.indexOf(rls.sel);
	    if(idx > 0) {
		idx--;
		rls.change(filtered.get(idx));
		rls.display(idx);
	    }
	    return(true);
	} else {
	    return(super.keydown(ev));
	}
    }
}
