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
import haven.iosys.*;
import java.lang.annotation.*;
import java.util.*;
import java.io.*;
import java.awt.image.BufferedImage;

public interface Toolkit {
    public static final Config.Variable<String> toolkit = Config.Variable.prop("haven.toolkit", null);

    public default Collection<Monitor> monitors() {return(Collections.emptyList());}
    public Cursor.Caps cursorcaps();
    public Cursor makecursor(BufferedImage img, Coord hotspot);
    public default boolean sharedenvs() {return(false);}
    public Windeye window();

    public void dispose();
    public String description();

    public default FilePicker.Factory picker() {
	return(FilePicker.nil);
    }
    public default void browse(java.net.URI location) throws IOException {
	throw(new IOException("No web browser available."));
    }

    public static interface Event {
    }

    public static interface CloseRequest extends Event {
    }

    public static interface KeyEvent extends Event {
	public String string();
	public Key key();
	public Set<Key.Mod> mods();
    }
    public static interface KeyDownEvent extends KeyEvent {
	public Key.Sym sym();
    }
    public static interface KeyUpEvent extends KeyEvent {}

    public static interface MouseEvent extends Event {
	public Coord wndc();
	public Set<MouseBtn> held();
	public Set<Key.Mod> mods();
    }
    public static interface MouseButtonEvent extends MouseEvent {
	public MouseBtn button();
    }
    public static interface MouseDownEvent extends MouseButtonEvent {}
    public static interface MouseUpEvent extends MouseButtonEvent {}
    public static interface MouseMoveEvent extends MouseEvent {}
    public static interface MouseWheelEvent extends MouseEvent {
	public static enum Axis {
	    VERT, HORIZ;
	}

	public Axis axis();
	public int amount();
	public double subamount();
    }

    public static interface EventListener {
	public void event(Event ev);
    }

    @dolda.jglob.Discoverable
    @Target(ElementType.TYPE)
    @Retention(RetentionPolicy.RUNTIME)
    public static @interface Available {
	String name();
    }

    static final Providers<Toolkit, Available> prov = new Providers<>("toolkit", toolkit::get, Available.class, Available::name);
    public static Map<String, Providers.Factory<? extends Toolkit>> toolkits() {
	return(prov.found());
    }

    public static Toolkit instance() {
	return(prov.instance());
    }
}
