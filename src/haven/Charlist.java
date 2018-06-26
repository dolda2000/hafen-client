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
    public static final int margin = 6;
    public int height, y, sel = 0;
    public IButton sau, sad;
    public List<Char> chars = new ArrayList<Char>();
    
    public static class Char {
	static Text.Foundry tf = new Text.Foundry(Text.serif, 20).aa(true);
	public String name;
	Text nt;
	Avaview ava;
	Button plb;
	
	public Char(String name) {
	    this.name = name;
	    nt = tf.render(name);
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
		c.plb.c = bg.sz().add(-10, y - 2).sub(c.plb.sz);
		g.image(c.nt.tex(), new Coord(off + c.ava.sz.x + 5, off + y));
		y += bg.sz().y + margin;
	    }
	}
	super.draw(g);
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
		Composited.Desc desc = Composited.Desc.decode(ui.sess, (Object[])args[1]);
		Resource.Resolver map = new Resource.Resolver.ResourceMap(ui.sess, (Object[])args[2]);
		c.ava.pop(desc, map);
	    }
	    c.plb = add(new Button(100, "Play"));
	    c.plb.hide();
	    synchronized(chars) {
		chars.add(c);
		if(chars.size() > height) {
		    sau.show();
		    sad.show();
		}
	    }
	} else if(msg == "ava") {
	    String cnm = (String)args[0];
	    Composited.Desc ava = Composited.Desc.decode(ui.sess, (Object[])args[1]);
	    Resource.Resolver map = new Resource.Resolver.ResourceMap(ui.sess, (Object[])args[2]);
	    synchronized(chars) {
		for(Char c : chars) {
		    if(c.name.equals(cnm)) {
			c.ava.pop(ava);
			break;
		    }
		}
	    }
	}
    }

    private void seladj() {
	if(sel < y)
	    y = sel;
	else if(sel >= y + height)
	    y = sel - height + 1;
    }

    public boolean keydown(java.awt.event.KeyEvent ev) {
	if(ev.getKeyCode() == ev.VK_UP) {
	    sel = Math.max(sel - 1, 0);
	    seladj();
	    return(true);
	} else if(ev.getKeyCode() == ev.VK_DOWN) {
	    sel = Math.min(sel + 1, chars.size() - 1);
	    seladj();
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
