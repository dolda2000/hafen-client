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
import java.util.*;
import java.awt.image.BufferedImage;

public interface Windeye {
    public Toolkit toolkit();

    public void add(Toolkit.EventListener l);
    public void dispose();

    public Windeye show(boolean show);
    public Windeye title(String title);
    public Windeye icon(BufferedImage icon);
    public Windeye cursor(Cursor cursor);
    public Windeye sizing(Sizing sizing);
    public Windeye state(State state);

    public Coord size();
    public State state();
    public boolean focused();
    public default Visibility visible() {return(Visibility.UNKNOWN);}

    public Environment env();
    public Pipe.Op fbstate();
    public void swapbuffers(Render g, Object mode);

    public default Clipboard clipboard(Object id) {return(Clipboard.nil);}
    public default Windeye drophandler(DropHandler h) {return(this);}

    public default void stats(Collection<String> buf) {}

    public static enum State {
	MINIMIZED, NORMAL, MAXIMIZED, EXCLUSIVE
    }

    public static enum Visibility {
	UNKNOWN, FULL, PARTIAL, NONE
    }

    public static class Sizing {
	public Coord fixsize = null, normsize = null, minsize = null, maxsize = null;

	public Sizing() {}
	public Sizing(Sizing from) {
	    this.fixsize = from.fixsize;
	    this.normsize = from.normsize;
	    this.minsize = from.minsize;
	    this.maxsize = from.maxsize;
	}

	public Sizing fixsize(Coord v) {fixsize = v; return(this);}
	public Sizing normsize(Coord v) {normsize = v; return(this);}
	public Sizing minsize(Coord v)  {minsize = v;  return(this);}
	public Sizing maxsize(Coord v)  {maxsize = v;  return(this);}
    }
}
