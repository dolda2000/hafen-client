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

public class Img extends Widget {
    private final Indir<Resource> res;
    private Tex img;
    public boolean hit = false;
	
    static {
	Widget.addtype("img", new WidgetFactory() {
		public Widget create(Coord c, Widget parent, Object[] args) {
		    Indir<Resource> res;
		    int a = 0;
		    if(args[a] instanceof String) {
			String nm = (String)args[a++];
			int ver = (args.length > a)?((Integer)args[a++]):-1;
			res = new Resource.Spec(nm, ver);
		    } else {
			res = parent.ui.sess.getres((Integer)args[a++]);
		    }
		    Img ret = new Img(c, res, parent);
		    if(args.length > a)
			ret.hit = (Integer)args[a++] != 0;
		    return(ret);
		}
	    });
    }

    public void draw(GOut g) {
	try {
	    if(img == null) {
		img = res.get().layer(Resource.imgc).tex();
		resize(img.sz());
	    }
	    g.image(img, Coord.z);
	} catch(Loading e) {}
    }
	
    public Img(Coord c, Tex img, Widget parent) {
	super(c, img.sz(), parent);
	this.res = null;
	this.img = img;
    }

    public Img(Coord c, Indir<Resource> res, Widget parent) {
	super(c, Coord.z, parent);
	this.res = res;
    }

    public void uimsg(String name, Object... args) {
	if(name == "ch") {
	    img = Resource.loadtex((String)args[0]);
	}
    }
    
    public boolean mousedown(Coord c, int button) {
	if(hit) {
	    wdgmsg("click", c, button, ui.modflags());
	    return(true);
	}
	return(false);
    }
}
