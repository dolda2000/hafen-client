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

public class HelpWnd extends Window {
    public static final RichText.Foundry fnd;
    public Indir<Resource> res;
    private Indir<Resource> showing = null;
    private final RichTextBox text;
    
    static {
	fnd = new RichText.Foundry();
	fnd.aa = true;
    }
    
    public HelpWnd(Coord c, Widget parent, Indir<Resource> res) {
	super(c, new Coord(300, 430), parent, "Help");
	this.res = res;
	this.text = new RichTextBox(Coord.z, new Coord(300, 400), this, "", fnd);
	new Button(new Coord(100, 410), 100, this, "Dismiss") {
	    public void click() {
		HelpWnd.this.wdgmsg("close");
	    }
	};
    }
    
    public void tick(double dt) {
	super.tick(dt);
	if(res != showing) {
	    try {
		text.settext(res.get().layer(Resource.pagina).text);
		showing = res;
	    } catch(Loading e) {}
	}
    }
}
