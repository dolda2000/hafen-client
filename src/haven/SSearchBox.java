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
import java.awt.Color;

public abstract class SSearchBox<I, W extends Widget> extends SListBox<I, W> {
    public String searching = null;
    private List<I> filtered = null;
    private Text info;

    protected abstract List<? extends I> allitems();
    protected abstract boolean searchmatch(I item, String text);

    protected List<? extends I> items() {
	if(searching != null)
	    return(filtered);
	return(allitems());
    }

    public SSearchBox(Coord sz, int itemh, int marg) {
	super(sz, itemh, marg);
	setcanfocus(true);
    }

    public SSearchBox(Coord sz, int itemh) {
	this(sz, itemh, 0);
    }

    public boolean keydown(KeyDownEvent ev) {
	if((ev.mods & (KeyMatch.C | KeyMatch.M)) == 0) {
	    if(ev.c == 8) {
		if(searching != null) {
		    search(searching.substring(0, searching.length() - 1));
		    return(true);
		}
	    } else if(key_act.match(ev)) {
		if(searching != null) {
		    stopsearch();
		    return(true);
		}
	    } else if(key_esc.match(ev)) {
		if(searching != null) {
		    stopsearch();
		    return(true);
		}
	    } else if(ev.code == ev.awt.VK_UP) {
		List<? extends I> items = items();
		if(items.size() > 0) {
		    int p = items.indexOf(sel);
		    if(p < 0) p = 0;
		    if(p > 0) p -= 1;
		    change(items.get(p));
		    display(p);
		}
		return(true);
	    } else if(ev.code == ev.awt.VK_DOWN) {
		List<? extends I> items = items();
		if(items.size() > 0) {
		    int p = items.indexOf(sel);
		    if(p < 0) p = items.size() - 1;
		    if(p < items.size() - 1) p += 1;
		    change(items.get(p));
		    display(p);
		}
		return(true);
	    } else if(ev.c >= 32) {
		search(((searching == null) ? "" : searching) + ev.c);
		return(true);
	    }
	}
	return(super.keydown(ev));
    }

    private void updinfo() {
	this.info = Text.renderf(Color.WHITE, "%s (%d/%d)", searching, filtered.size(), allitems().size());
    }

    public void search(String text) {
	if(text.length() < 1) {
	    stopsearch();
	    return;
	}
	List<I> found = new ArrayList<>();
	List<? extends I> items = allitems();
	boolean sf = false, bs = true;
	int ncc = -1;
	for(I item : items) {
	    if(item == sel)
		bs = false;
	    if(searchmatch(item, text)) {
		if(item == sel)
		    sf = true;
		if(bs || (ncc < 0))
		    ncc = found.size();
		found.add(item);
	    }
	}
	filtered = found;
	searching = text;
	if(!sf) {
	    if(ncc >= 0)
		change(found.get(ncc));
	    else
		change(null);
	}
	if(sel != null)
	    display(sel);
	updinfo();
    }

    public void draw(GOut g) {
	super.draw(g);
	if(searching != null) {
	    g.aimage(info.tex(), g.sz(), 1, 1);
	}
    }

    public void stopsearch() {
	searching = null;
	filtered = null;
	if(sel != null)
	    display(sel);
    }

    public void lostfocus() {
	super.lostfocus();
	stopsearch();
    }

    public boolean mousedown(MouseDownEvent ev) {
	parent.setfocus(this);
	return(super.mousedown(ev));
    }
}
