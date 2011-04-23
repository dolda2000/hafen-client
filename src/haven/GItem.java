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

public class GItem extends AWidget {
    public Indir<Resource> res;
    public Color olcol = null;
    public int meter = 0;
    public int num = -1;
    
    static {
	Widget.addtype("item", new WidgetFactory() {
		public Widget create(Coord c, Widget parent, Object[] args) {
		    int res = (Integer)args[0];
		    return(new GItem(parent, parent.ui.sess.getres(res)));
		}
	    });
    }
    
    public GItem(Widget parent, Indir<Resource> res) {
	super(parent);
	this.res = res;
    }
    
    public void uimsg(String name, Object... args) {
	if(name == "num") {
	    num = (Integer)args[0];
	} else if(name == "chres") {
	    res = ui.sess.getres((Integer)args[0]);
	} else if(name == "color") {
	    olcol = (Color)args[0];
	    if(olcol.getAlpha() == 0)
		olcol = null;
	} else if(name == "tt") {
	} else if(name == "meter") {
	    meter = (Integer)args[0];
	}
    }
}
