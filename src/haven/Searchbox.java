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
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

public abstract class Searchbox<T> extends Listbox<T> {
    private static final int C = 1, M = 2;
    public String searching = null;
    private int[] sought;
    private int cursidx;
    private boolean[] found;
    private Text info;

    protected abstract boolean searchmatch(int idx, String text);

    public Searchbox(int w, int h, int itemh) {
	super(w, h, itemh);
	setcanfocus(true);
    }

    public boolean keydown(KeyEvent ev) {
	int mod = 0;
	if((ev.getModifiersEx() & InputEvent.CTRL_DOWN_MASK) != 0) mod |= C;
	if((ev.getModifiersEx() & (InputEvent.META_DOWN_MASK | InputEvent.ALT_DOWN_MASK)) != 0) mod |= M;
	char c = ev.getKeyChar();
	if(c == ev.CHAR_UNDEFINED)
	    c = '\0';
	int code = ev.getKeyCode();
	if(mod == 0) {
	    if(c == 8) {
		if(searching != null) {
		    search(searching.substring(0, searching.length() - 1));
		    return(true);
		}
	    } else if(c == 10) {
		if(searching != null) {
		    stopsearch();
		    return(true);
		}
	    } else if(c == 27) {
		if(searching != null) {
		    stopsearch();
		    return(true);
		}
	    } else if(code == KeyEvent.VK_UP) {
		if((searching != null) && (cursidx > 0)) {
		    cursidx--;
		    display(sought[cursidx]);
		    change(listitem(sought[cursidx]));
		    updinfo();
		    return(true);
		}
	    } else if(code == KeyEvent.VK_DOWN) {
		if((searching != null) && (cursidx < sought.length - 1)) {
		    cursidx++;
		    display(sought[cursidx]);
		    change(listitem(sought[cursidx]));
		    updinfo();
		    return(true);
		}
	    } else if(c >= 32) {
		search(((searching == null) ? "" : searching) + c);
		return(true);
	    }
	}
	return(super.keydown(ev));
    }

    private void updinfo() {
	if(cursidx >= 0)
	    this.info = Text.renderf(Color.WHITE, "%s (%d/%d)", searching, cursidx + 1, sought.length);
	else
	    this.info = Text.renderf(Color.WHITE, "%s (%d)", searching, sought.length);
    }

    public void search(String text) {
	if(text.length() < 1) {
	    stopsearch();
	    return;
	}
	int ps = (searching == null) ? -1 : ((cursidx < 0) ? -1 : sought[cursidx]);
	int[] si = new int[listitems()];
	boolean[] fm = new boolean[si.length];
	int ns = 0, nc = -1, ncc = -1;
	for(int i = 0; i < si.length; i++) {
	    if(searchmatch(i, text)) {
		if(ps == i)
		    nc = ns;
		if((ncc == -1) || ((ps >= 0) && (i < ps)))
		    ncc = ns;
		si[ns++] = i;
		fm[i] = true;
	    }
	}
	if(nc < 0)
	    nc = ncc;
	si = Utils.splice(si, 0, ns);
	this.searching = text;
	this.sought = si;
	this.cursidx = nc;
	this.found = fm;
	updinfo();
	if(nc >= 0) {
	    display(si[nc]);
	    change(listitem(si[nc]));
	}
    }

    public void draw(GOut g) {
	super.draw(g);
	if(searching != null) {
	    g.aimage(info.tex(), g.sz, 1, 1);
	}
    }

    public void stopsearch() {
	searching = null;
	sought = null;
	found = null;
	info = null;
    }

    public void lostfocus() {
	super.lostfocus();
	stopsearch();
    }

    public boolean soughtitem(int i) {
	return((searching != null) && (i >= 0) && (i < found.length) && found[i]);
    }

    public boolean mousedown(Coord c, int button) {
	parent.setfocus(this);
	stopsearch();
	return(super.mousedown(c, button));
    }
}
