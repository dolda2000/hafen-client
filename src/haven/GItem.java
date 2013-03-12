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
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.util.*;

public class GItem extends AWidget implements ItemInfo.ResOwner {
    public Indir<Resource> res;
    public int meter = 0;
    public int num = -1;
    private Object[] rawinfo;
    private List<ItemInfo> info = Collections.emptyList();
    
    @RName("item")
    public static class $_ implements Factory {
	public Widget create(Coord c, Widget parent, Object[] args) {
	    int res = (Integer)args[0];
	    return(new GItem(parent, parent.ui.sess.getres(res)));
	}
    }
    
    public interface ColorInfo {
	public Color olcol();
    }
    
    public interface NumberInfo {
	public int itemnum();
    }

    public class Amount extends ItemInfo implements NumberInfo {
	private final int num;
	
	public Amount(int num) {
	    super(GItem.this);
	    this.num = num;
	}
	
	public int itemnum() {
	    return(num);
	}
    }
    
    public GItem(Widget parent, Indir<Resource> res) {
	super(parent);
	this.res = res;
    }

    public Glob glob() {
	return(ui.sess.glob);
    }

    public List<ItemInfo> info() {
	if(info == null)
	    info = ItemInfo.buildinfo(this, rawinfo);
	return(info);
    }
    
    public Resource resource() {
	return(res.get());
    }

    public void uimsg(String name, Object... args) {
	if(name == "num") {
	    num = (Integer)args[0];
	} else if(name == "chres") {
	    res = ui.sess.getres((Integer)args[0]);
	} else if(name == "tt") {
	    info = null;
	    rawinfo = args;
	} else if(name == "meter") {
	    meter = (Integer)args[0];
	}
    }
}
