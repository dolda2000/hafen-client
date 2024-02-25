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

import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;

public class TextEntry extends Widget implements ReadLine.Owner {
    public static final Color defcol = new Color(255, 205, 109), dirtycol = new Color(255, 232, 209);
    public static final Color selcol = new Color(24, 80, 192);
    public static final Text.Foundry fnd = new Text.Foundry(Text.serif, 12).aa(true);
    public static final Tex lcap = Resource.loadtex("gfx/hud/text/l");
    public static final Tex rcap = Resource.loadtex("gfx/hud/text/r");
    public static final Tex mext = Resource.loadtex("gfx/hud/text/m");
    public static final Tex caret = Resource.loadtex("gfx/hud/text/caret");
    public static final int toffx = lcap.sz().x;
    public static final Coord coff = UI.scale(new Coord(-2, 0));
    public static final int wmarg = lcap.sz().x + rcap.sz().x + UI.scale(1);
    public boolean dshow = false;
    public ReadLine buf;
    public int sx;
    public boolean pw = false;
    private boolean dirty = false;
    private double focusstart;
    private Text.Line tcache = null;
    private UI.Grab d = null;

    @RName("text")
    public static class $_ implements Factory {
	public Widget create(UI ui, Object[] args) {
	    return(new TextEntry(UI.scale(Utils.iv(args[0])), (String)args[1]));
	}
    }

    public void settext(String text) {
	buf.setline(text);
	redraw();
    }

    public void rsettext(String text) {
	buf = ReadLine.make(this, text);
	redraw();
    }

    public void commit() {
	dirty = false;
	redraw();
    }

    public void uimsg(String name, Object... args) {
	if(name == "settext") {
	    settext((String)args[0]);
	} else if(name == "sel") {
	    if(args.length == 0) {
		buf.select(0, buf.length());
	    } else {
		int f = (args[0] == null) ? buf.length() : Utils.clip(Utils.iv(args[0]), 0, buf.length());
		int t = (args[1] == null) ? buf.length() : Utils.clip(Utils.iv(args[1]), 0, buf.length());
		buf.select(f, t);
	    }
	} else if(name == "get") {
	    wdgmsg("text", buf.line());
	} else if(name == "pw") {
	    pw = Utils.bv(args[0]);
	} else if(name == "dshow") {
	    dshow = Utils.bv(args[0]);
	} else if(name == "cmt") {
	    commit();
	} else {
	    super.uimsg(name, args);
	}
    }

    protected String dtext() {
	if(pw) {
	    char[] dp = new char[buf.length()];
	    java.util.Arrays.fill(dp, '\u2022');
	    return(new String(dp));
	} else {
	    return(buf.line());
	}
    }

    protected void redraw() {
	if(tcache != null) {
	    tcache.tex().dispose();
	    tcache = null;
	}
    }

    public void draw(GOut g) {
	Text.Line tcache = this.tcache;
	if(tcache == null)
	    this.tcache = tcache = fnd.render(dtext(), (dshow && dirty) ? dirtycol : defcol);
	int point = buf.point(), mark = buf.mark();
	g.image(mext, Coord.z, sz);
	if(mark >= 0) {
	    int px = tcache.advance(point) - sx, mx = tcache.advance(mark) - sx;
	    g.chcolor(selcol);
	    g.frect2(Coord.of(Math.min(px, mx) + toffx, (sz.y - tcache.sz().y) / 2),
		     Coord.of(Math.max(px, mx) + toffx, (sz.y + tcache.sz().y) / 2));
	    g.chcolor();
	}
	g.image(tcache.tex(), Coord.of(toffx - sx, (sz.y - tcache.sz().y) / 2));
	g.image(lcap, Coord.z);
	g.image(rcap, Coord.of(sz.x - rcap.sz().x, 0));
	if(hasfocus) {
	    int cx = tcache.advance(point);
	    if(cx < sx) {sx = cx;}
	    if(cx > sx + (sz.x - wmarg)) {sx = cx - (sz.x - wmarg);}
	    int lx = cx - sx;
	    if(((Utils.rtime() - Math.max(focusstart, buf.mtime())) % 1.0) < 0.5)
		g.image(caret, coff.add(toffx + lx, (sz.y - tcache.img.getHeight()) / 2));
	}
    }

    public TextEntry(int w, String deftext) {
	super(new Coord(w, mext.sz().y));
	rsettext(deftext);
	setcanfocus(true);
    }

    protected void changed() {
	dirty = true;
    }

    public void activate(String text) {
	if(canactivate)
	    wdgmsg("activate", text);
    }

    public void done(ReadLine buf) {
	activate(buf.line());
    }

    public void changed(ReadLine buf) {
	redraw();
	TextEntry.this.changed();
    }

    public boolean gkeytype(KeyEvent ev) {
	activate(buf.line());
	return(true);
    }

    public boolean keydown(KeyEvent e) {
	return(buf.key(e));
    }

    public void mousemove(Coord c) {
	if((d != null) && (tcache != null)) {
	    int p = tcache.charat(c.x + sx - toffx);
	    if(buf.mark() < 0)
		buf.mark(buf.point());
	    buf.point(p);
	}
    }

    public boolean mousedown(Coord c, int button) {
	parent.setfocus(this);
	if((button == 1) && (tcache != null)) {
	    buf.point(tcache.charat(c.x + sx - toffx));
	    buf.mark(-1);
	    d = ui.grabmouse(this);
	}
	return(true);
    }

    public boolean mouseup(Coord c, int button) {
	if((button == 1) && (d != null)) {
	    d.remove();
	    d = null;
	    return(true);
	}
	return(false);
    }

    public void gotfocus() {
	focusstart = Utils.rtime();
    }

    public void resize(int w) {
	resize(w, sz.y);
	redraw();
    }

    public String text() {
	return(buf.line());
    }
}
