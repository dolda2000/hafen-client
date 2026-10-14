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

import haven.iosys.tk.*;
import java.util.*;

public class AWTCompat {
    public static final Map<Key.Std, Integer> revawt = new HashMap<>();
    private static final java.awt.Component awtdummy = new java.awt.Component() {};
    static {
	for(Map.Entry<Integer, Key.Std> k : AWTToolkit.stdsyms.entrySet())
	    revawt.put(k.getValue(), k.getKey());
    }

    public static int awtmods(Collection<Key.Mod> mods) {
	int ret = 0;
	for(Key.Mod mod : mods) {
	    switch(mod) {
	    case SHIFT: ret |= java.awt.event.InputEvent.SHIFT_DOWN_MASK; break;
	    case CONTROL: ret |= java.awt.event.InputEvent.CTRL_DOWN_MASK; break;
	    case META: ret |= java.awt.event.InputEvent.META_DOWN_MASK; break;
	    case ALT: ret |= java.awt.event.InputEvent.ALT_DOWN_MASK; break;
	    }
	}
	return(ret);
    }

    private static class ExtKeyEvent extends java.awt.event.KeyEvent {
	private ExtKeyEvent(int id, long time, int mods, int code, char chr, int location) {
	    super(awtdummy, id, time, mods, code, chr, location);
	}

	public int getExtendedKeyCode() {return(getKeyCode());}
    }

    public static java.awt.event.KeyEvent mkawt(Toolkit.KeyEvent ev) {
	int id = 0;
	if(ev instanceof Toolkit.KeyDownEvent)
	    id = java.awt.event.KeyEvent.KEY_PRESSED;
	else if(ev instanceof Toolkit.KeyUpEvent)
	    id = java.awt.event.KeyEvent.KEY_RELEASED;
	char c = java.awt.event.KeyEvent.CHAR_UNDEFINED;
	if(ev.string() != "")
	    c = ev.string().charAt(0);
	return(new ExtKeyEvent(id, System.currentTimeMillis(), awtmods(ev.mods()),
			       revawt.getOrDefault(ev.key().primary(revawt.keySet()), java.awt.event.KeyEvent.VK_UNDEFINED),
			       c, java.awt.event.KeyEvent.KEY_LOCATION_UNKNOWN));
    }

    public static int buttonid(MouseBtn btn) {
	if(btn == MouseBtn.Std.LEFT)
	    return(1);
	else if(btn == MouseBtn.Std.MIDDLE)
	    return(2);
	else if(btn == MouseBtn.Std.RIGHT)
	    return(3);
	return(0);
    }

    public static java.awt.event.MouseEvent mkawt(Toolkit.MouseEvent ev) {
	int id = 0;
	if(ev instanceof Toolkit.MouseDownEvent)
	    id = java.awt.event.MouseEvent.MOUSE_PRESSED;
	else if(ev instanceof Toolkit.MouseUpEvent)
	    id = java.awt.event.MouseEvent.MOUSE_RELEASED;
	return(new java.awt.event.MouseEvent(awtdummy, id, System.currentTimeMillis(), awtmods(ev.mods()),
					     ev.wndc().x, ev.wndc().y, 0, false,
					     (ev instanceof Toolkit.MouseButtonEvent) ? buttonid(((Toolkit.MouseButtonEvent)ev).button()) : java.awt.event.MouseEvent.NOBUTTON));
    }
}
