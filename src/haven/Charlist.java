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

import java.util.*;

public class Charlist extends Widget {
    public static final Tex bg = Resource.loadtex("gfx/hud/avakort");
    public static final int margin = UI.scale(6);
    public static final int btnw = UI.scale(100);
    public int height, y, sel = 0;
    public IButton sau, sad;
    public List<Char> chars = new ArrayList<Char>();
    Avaview avalink;
    
    public static class Char {
	public static final Text.Foundry tf = new Text.Foundry(Text.serif, 20).aa(true);
	public final String name;
	public Composited.Desc avadesc;
	public Resource.Resolver avamap;
	public Collection<ResData> avaposes;
	Text nt;
	Avaview ava;
	Button plb;
	
	public Char(String name) {
	    this.name = name;
	    nt = tf.render(name);
	}

	public void ava(Composited.Desc desc, Resource.Resolver resmap, Collection<ResData> poses) {
	    this.avadesc = desc;
	    this.avamap = resmap;
	    this.avaposes = poses;
	    ava.pop(desc, resmap);
	}
    }
    
    @RName("charlist")
    public static class $_ implements Factory {
	public Widget create(UI ui, Object[] args) {
	    return(new Charlist((Integer)args[0]));
	}
    }

    public Charlist(int height) {
	super(Coord.z);
	this.height = height;
	y = 0;
	setcanfocus(true);
	sau = adda(new IButton("gfx/hud/buttons/csau", "u", "d", "o") {
		public void click() {
		    scroll(-1);
		}
	    }, bg.sz().x / 2, 0, 0.5, 0);
	sad = adda(new IButton("gfx/hud/buttons/csad", "u", "d", "o") {
		public void click() {
		    scroll(1);
		}
	    }, bg.sz().x / 2, sau.c.y + sau.sz.y + (bg.sz().y * height) + (margin * (height - 1)), 0.5, 0);
	sau.hide(); sad.hide();
	resize(new Coord(bg.sz().x, sad.c.y + sad.sz.y));
    }

    protected void added() {
	parent.setfocus(this);
    }

    public void scroll(int amount) {
	y += amount;
	synchronized(chars) {
	    if(y > chars.size() - height)
		y = chars.size() - height;
	}
	if(y < 0)
	    y = 0;
    }
    
    public void draw(GOut g) {
	int y = sau.c.y + sau.sz.y;
	synchronized(chars) {
	    for(Char c : chars) {
		c.ava.hide();
		c.plb.hide();
	    }
	    for(int i = 0; (i < height) && (i + this.y < chars.size()); i++) {
		boolean sel = (i + this.y) == this.sel;
		Char c = chars.get(i + this.y);
		if(hasfocus && sel) {
		    g.chcolor(255, 255, 128, 255);
		    g.image(bg, new Coord(0, y));
		    g.chcolor();
		} else {
		    g.image(bg, new Coord(0, y));
		}
		c.ava.show();
		c.plb.show();
		int off = (bg.sz().y - c.ava.sz.y) / 2;
		c.ava.c = new Coord(off, off + y);
		c.plb.c = UI.scale(new Coord(-10, - 2)).add(bg.sz()).add(0, y).sub(c.plb.sz);
		g.image(c.nt.tex(), UI.scale(new Coord(5, 0)).add(off + c.ava.sz.x, off + y));
		y += bg.sz().y + margin;
	    }
	}
	super.draw(g);
    }
    
    public boolean mousedown(Coord c, int button) {
	boolean hit = false;
	if(button == 1) {
	    synchronized(chars) {
		for(int i = 0, y = sau.c.y + sau.sz.y; (i < height) && (i + this.y < chars.size()); i++, y += bg.sz().y + margin) {
		    if(c.isect(new Coord(0, y), bg.sz())) {
			if(i + this.y != sel)
			    chsel(i + this.y);
			hit = true;
			break;
		    }
		}
	    }
	}
	if(super.mousedown(c, button))
	    return(true);
	return(hit);
    }

    public boolean mousewheel(Coord c, int amount) {
	scroll(amount);
	return(true);
    }
    
    public void wdgmsg(Widget sender, String msg, Object... args) {
	if(sender instanceof Button) {
	    synchronized(chars) {
		for(Char c : chars) {
		    if(sender == c.plb)
			wdgmsg("play", c.name);
		}
	    }
	} else if(sender instanceof Avaview) {
	} else {
	    super.wdgmsg(sender, msg, args);
	}
    }

    public void uimsg(String msg, Object... args) {
	if(msg == "add") {
	    Char c = new Char((String)args[0]);
	    c.ava = add(new Avaview(Avaview.dasz, -1, "avacam"));
	    c.ava.hide();
	    if(args.length > 1) {
		Object[] rawdesc = (Object[])args[1];
		Collection<ResData> poses = new ArrayList<>();
		Composited.Desc desc = Composited.Desc.decode(ui.sess, rawdesc);
		Resource.Resolver map = new Resource.Resolver.ResourceMap(ui.sess, (Object[])args[2]);
		if(rawdesc.length > 3) {
		    Object[] rawposes = (Object[])rawdesc[3];
		    for(int i = 0; i < rawposes.length; i += 2)
			poses.add(new ResData(ui.sess.getres((Integer)rawposes[i]), new MessageBuf((byte[])rawposes[i + 1])));
		}
		c.ava(desc, map, poses);
	    }
	    c.plb = add(new Button(btnw, "Play"));
	    c.plb.hide();
	    synchronized(chars) {
		int idx = chars.size();
		chars.add(c);
		if(chars.size() > height) {
		    sau.show();
		    sad.show();
		}
		if(idx == sel) {
		    chsel(sel);
		}
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
		    poses.add(new ResData(ui.sess.getres((Integer)rawposes[i]), new MessageBuf((byte[])rawposes[i + 1])));
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
	    int id = (Integer)args[0];
	    if(id < 0)
		avalink = null;
	    else
		avalink = (Avaview)ui.getwidget(id);
	} else {
	    super.uimsg(msg, args);
	}
    }

    private void seladj() {
	if(sel < y)
	    y = sel;
	else if(sel >= y + height)
	    y = sel - height + 1;
    }

    private void chsel(int idx) {
	sel = idx;
	seladj();
	if(avalink != null) {
	    Char chr = chars.get(idx);
	    if(chr.avadesc != null) {
		avalink.pop(chr.avadesc.clone(), chr.avamap);
		avalink.chposes(chr.avaposes, false);
	    }
	}
    }

    public boolean keydown(java.awt.event.KeyEvent ev) {
	if(ev.getKeyCode() == ev.VK_UP) {
	    chsel(Math.max(sel - 1, 0));
	    return(true);
	} else if(ev.getKeyCode() == ev.VK_DOWN) {
	    chsel(Math.min(sel + 1, chars.size() - 1));
	    return(true);
	} else if(ev.getKeyCode() == ev.VK_ENTER) {
	    if((sel >= 0) && (sel < chars.size())) {
		chars.get(sel).plb.click();
	    }
	    return(true);
	}
	return(false);
    }
}
