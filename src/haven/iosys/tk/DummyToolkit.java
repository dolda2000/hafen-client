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

package haven.iosys.tk;

import haven.*;
import haven.render.*;
import java.util.function.*;
import java.awt.image.BufferedImage;

public class DummyToolkit implements Toolkit {
    public static final DummyToolkit instance = new DummyToolkit();

    public Cursor.Caps cursorcaps() {return(new Cursor.Caps(Integer.MAX_VALUE, 0));}
    public Cursor makecursor(BufferedImage img, Coord hotspot) {throw(new UnsupportedOperationException());}
    public Windeye window() {throw(new UnsupportedOperationException());}
    public void dispose() {}
    public String description() {return("Dummy toolkit");}

    public static abstract class DummyWindow implements Windeye {
	public Toolkit toolkit() {return(DummyToolkit.instance);}

	public void add(Toolkit.EventListener l) {}
	public Windeye show(boolean vis) {return(this);}
	public Windeye title(String title) {return(this);}
	public Windeye icon(BufferedImage img) {return(this);}
	public Windeye cursor(Cursor c) {return(this);}
	public Windeye sizing(Sizing infox) {return(this);}
	public Windeye state(State st) {return(this);}

	public State state() {return(State.NORMAL);}
	public boolean focused() {return(true);}

	public void swapbuffers(Render buf, Object mode) {}

	public void dispose() {}

	public static DummyWindow of(Coord size, Environment env, Consumer<Render> swap) {
	    return(new DummyWindow() {
		public Coord size() {
		    return(size);
		}

		public Environment env() {
		    return(env);
		}

		public Pipe.Op fbstate() {
		    return(Pipe.Op.nil);
		}

		public void swapbuffers(Render buf, Object mode) {
		    if(swap != null)
			swap.accept(buf);
		}
	    });
	}
    }
}
