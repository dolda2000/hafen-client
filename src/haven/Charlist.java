/*
 *  This file is part of the Haven & Hearth game client.
 *  Copyright (C) 2009 Fredrik Tolf <fredrik@dolda2000.com>, and
 *                     Björn Johannessen <johannessen.bjorn@gmail.com>
 *
 *  Redistribution and/or modification of this file is subject to the
 *  terms of the GNU Lesser General Public License, version 3, as
 *  published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  Other parts of this source tree adhere to other copying
 *  rights. Please see the file `COPYING' in the root directory of the
 *  source tree for details.
 *
 *  A copy the GNU Lesser General Public License is distributed along
 *  with the source tree of which this file is a part in the file
 *  `doc/LPGL-3'. If it is missing for any reason, please see the Free
 *  Software Foundation's website at <http://www.fsf.org/>, or write
 *  to the Free Software Foundation, Inc., 59 Temple Place, Suite 330,
 *  Boston, MA 02111-1307 USA
 */

package haven;

import java.awt.Color;
import java.util.*;

public class Charlist extends Widget {
    public static final Coord bsz = UI.scale(289, 96);
    public static final Text.Furnace tf = new PUtils.BlurFurn(new PUtils.TexFurn(new Text.Foundry(Text.fraktur, 20).aa(true), Window.ctex), UI.scale(2), UI.scale(2), Color.BLACK);
    public static final int margin = UI.scale(6);
    public static final int btnw = UI.scale(100);
    public final int height;
    public final IButton sau, sad;
    public final List<Char> chars = new ArrayList<Char>();
    public final Boxlist list;
    public Avaview avalink;

    @RName("charlist")
    public static class $_ implements Factory {
	public Widget create(UI ui, Object[] args) {
	    return(new Charlist(Utils.iv(args[0])));
	}
    }

    public Charlist(int height) {
	super(Coord.z);
	this.height = height;
	setcanfocus(true);
	sau = adda(new IButton("gfx/hud/buttons/csau", "u", "d", "o"), bsz.x / 2, 0, 0.5, 0)
	    .action(() -> scroll(-1));
	list = add(new Boxlist(height), 0, sau.c.y + sau.sz.y + margin);
	sad = adda(new IButton("gfx/hud/buttons/csad", "u", "d", "o"), bsz.x / 2, list.c.y + list.sz.y + margin, 0.5, 0)
	    .action(() -> scroll(1));
	sau.hide(); sad.hide();
	resize(new Coord(bsz.x, sad.c.y + sad.sz.y));
    }

    public static class Char {
	public final String name;
	public Composited.Desc avadesc;
	public Resource.Resolver avamap;
	public Collection<ResData> avaposes;

	public Char(String name) {
	    this.name = name;
	}

	public void ava(Composited.Desc desc, Resource.Resolver resmap, Collection<ResData> poses) {
	    this.avadesc = desc;
	    this.avamap = resmap;
	    this.avaposes = poses;
	}
    }

    public class Charbox extends Widget {
	public final Char chr;
	public final Avaview ava;

	public Charbox(Char chr) {
	    super(bsz);
	    this.chr = chr;
	    Widget avaf = adda(Frame.with(this.ava = new Avaview(Avaview.dasz, -1, "avacam"), false), Coord.of(sz.y / 2), 0.5, 0.5);
	    add(new Img(tf.render(chr.name).tex()), avaf.pos("ur").adds(5, 0));
	    adda(new Button(UI.scale(100), "Play"), pos("cbr").subs(10, 2), 1.0, 1.0).action(() -> Charlist.this.wdgmsg("play", chr.name));
	}

	public void tick(double dt) {
	    if(chr.avadesc != ava.avadesc)
		ava.pop(chr.avadesc, chr.avamap);
	}

	public void draw(GOut g) {
	    if(list.sel == chr)
		g.chcolor(255, 255, 128, 255);
	    ISBox.box.draw(g, Coord.z, sz);
	    g.chcolor();
	    super.draw(g);
	}

	public boolean mousedown(Coord c, int button) {
	    super.mousedown(c, button);
	    list.change(chr);
	    return(true);
	}
    }

    public class Boxlist extends SListBox<Char, Charbox> {
	public Boxlist(int h) {
	    super(Coord.of(bsz.x, ((bsz.y + margin) * h) - margin), bsz.y, margin);
	}

	protected List<Char> items() {return(chars);}
	protected Charbox makeitem(Char chr, int idx, Coord sz) {return(new Charbox(chr));}

	protected void drawslot(GOut g, Char item, int idx, Area area) {}
	public boolean mousewheel(Coord c, int amount) {return(false);}
	protected boolean unselect(int button) {return(false);}
	protected boolean autoscroll() {return(false);}

	public void change(Char chr) {
	    super.change(chr);
	    display(chr);
	    if((avalink != null) && (chr.avadesc != null)) {
		avalink.pop(chr.avadesc.clone(), chr.avamap);
		avalink.chposes(chr.avaposes, false);
	    }
	}
    }

    protected void added() {
	parent.setfocus(this);
    }

    private int scrolltgt = -1;
    private double scrollval = -1;
    public void tick(double dt) {
	if(scrolltgt >= 0) {
	    if(scrollval < 0)
		scrollval = list.scrollval();
	    double d = scrollval - scrolltgt;
	    double nv = scrolltgt + (d * Math.pow(0.5, dt * 50));
	    if(Math.abs(nv - scrolltgt) < 1) {
		nv = scrolltgt;
		scrolltgt = -1;
		scrollval = -1;
	    }
	    list.scrollval((int)Math.round(scrollval = nv));
	}
	super.tick(dt);
    }

    public void scroll(int amount) {
	scrolltgt = Utils.clip(((scrolltgt < 0) ? list.scrollval() : scrolltgt) + ((bsz.y + margin) * amount), list.scrollmin(), list.scrollmax());
    }

    public boolean mousedown(Coord c, int button) {
	boolean hit = false;
	if(super.mousedown(c, button))
	    return(true);
	return(hit);
    }

    public boolean mousewheel(Coord c, int amount) {
	scroll(amount);
	return(true);
    }

    public void uimsg(String msg, Object... args) {
	if(msg == "add") {
	    Char c = new Char((String)args[0]);
	    if(args.length > 1) {
		Object[] rawdesc = (Object[])args[1];
		Collection<ResData> poses = new ArrayList<>();
		Composited.Desc desc = Composited.Desc.decode(ui.sess, rawdesc);
		Resource.Resolver map = new Resource.Resolver.ResourceMap(ui.sess, (Object[])args[2]);
		if(rawdesc.length > 3) {
		    Object[] rawposes = (Object[])rawdesc[3];
		    for(int i = 0; i < rawposes.length; i += 2)
			poses.add(new ResData(ui.sess.getresv(rawposes[i]), new MessageBuf((byte[])rawposes[i + 1])));
		}
		c.ava(desc, map, poses);
	    }
	    synchronized(chars) {
		chars.add(c);
		if(chars.size() > height) {
		    sau.show();
		    sad.show();
		}
		if(list.sel == null)
		    list.change(c);
	    }
	} else if(msg == "ava") {
	    String cnm = (String)args[0];
	    Object[] rawdesc = (Object[])args[1];
	    Collection<ResData> poses = new ArrayList<>();
	    Composited.Desc ava = Composited.Desc.decode(ui.sess, rawdesc);
	    Resource.Resolver map = new Resource.Resolver.ResourceMap(ui.sess, (Object[])args[2]);
	    if(rawdesc.length > 3) {
		Object[] rawposes = (Object[])rawdesc[3];
		for(int i = 0; i < rawposes.length; i += 2)
		    poses.add(new ResData(ui.sess.getresv(rawposes[i]), new MessageBuf((byte[])rawposes[i + 1])));
	    }
	    synchronized(chars) {
		for(Char c : chars) {
		    if(c.name.equals(cnm)) {
			c.ava(ava, map, poses);
			break;
		    }
		}
	    }
	} else if(msg == "biggu") {
	    int id = Utils.iv(args[0]);
	    if(id < 0) {
		avalink = null;
	    } else {
		Widget tgt = ui.getwidget(id);
		if(tgt instanceof ProxyFrame)
		    avalink = (Avaview)((ProxyFrame)tgt).ch;
		else if(tgt instanceof Avaview)
		    avalink = (Avaview)tgt;
	    }
	} else {
	    super.uimsg(msg, args);
	}
    }

    public boolean keydown(java.awt.event.KeyEvent ev) {
	if(ev.getKeyCode() == ev.VK_UP) {
	    if(!chars.isEmpty())
		list.change(chars.get(Math.max(chars.indexOf(list.sel) - 1, 0)));
	    return(true);
	} else if(ev.getKeyCode() == ev.VK_DOWN) {
	    if(!chars.isEmpty())
		list.change(chars.get(Math.min(chars.indexOf(list.sel) + 1, chars.size() - 1)));
	    return(true);
	} else if(ev.getKeyCode() == ev.VK_ENTER) {
	    if(list.sel != null)
		wdgmsg("play", list.sel.name);
	    return(true);
	}
	return(false);
    }
}
