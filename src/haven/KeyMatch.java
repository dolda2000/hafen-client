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

import java.awt.event.*;
import static java.awt.event.KeyEvent.VK_UNDEFINED;

public class KeyMatch {
    public static final int S = 1, C = 2, M = 4;
    public static final KeyMatch nil = new KeyMatch('\0', false, VK_UNDEFINED, "None", 0, 0);
    public char chr;
    public boolean casematch;
    public int code;
    public String keyname;
    public int modmask, modmatch;

    public KeyMatch(char chr, boolean casematch, int code, String keyname, int modmask, int modmatch) {
	this.chr = casematch ? chr : Character.toUpperCase(chr);
	this.casematch = casematch;
	this.code = code;
	this.keyname = keyname;
	this.modmask = modmask;
	this.modmatch = modmatch;
    }

    private static int mods(KeyEvent ev) {
	int ret = 0;
	if((ev.getModifiersEx() & InputEvent.SHIFT_DOWN_MASK) != 0) ret |= S;
	if((ev.getModifiersEx() & InputEvent.CTRL_DOWN_MASK) != 0) ret |= C;
	if((ev.getModifiersEx() & (InputEvent.META_DOWN_MASK | InputEvent.ALT_DOWN_MASK)) != 0) ret |= M;
	return(ret);
    }

    public boolean match(KeyEvent ev) {
	int mod = mods(ev);
	if((mod & modmask) != modmatch)
	    return(false);
	if(chr != 0) {
	    char evc = ev.getKeyChar();
	    if(((mod & C) != 0) && (evc < 32)) {
		/* Undo Java's TTY Control-code mangling */
		switch(ev.getKeyCode()) {
		case KeyEvent.VK_BACK_SPACE:
		case KeyEvent.VK_ENTER:
		case KeyEvent.VK_TAB:
		case KeyEvent.VK_ESCAPE:
		    break;
		default:
		    evc = (char)(evc + 'A' - 1);
		    break;
		}
	    }
	    if(!casematch)
		evc = Character.toUpperCase(evc);
	    return(evc == chr);
	} else if(code != VK_UNDEFINED) {
	    return(ev.getExtendedKeyCode() == code);
	} else {
	    return(false);
	}
    }

    public String name() {
	StringBuilder buf = new StringBuilder();
	if((modmatch & S) != 0)
	    buf.append("Shift+");
	if((modmatch & C) != 0)
	    buf.append("Ctrl+");
	if((modmatch & M) != 0)
	    buf.append("Alt+");
	buf.append(keyname);
	return(buf.toString());
    }

    private boolean equals(KeyMatch that) {
	return((this.chr == that.chr) && (this.casematch == that.casematch) && (this.code == that.code) && (this.modmask == that.modmask) && (this.modmatch == that.modmatch));
    }

    public boolean equals(Object o) {
	return((o instanceof KeyMatch) && equals((KeyMatch)o));
    }

    public static KeyMatch forchar(char chr, int mods) {
	return(new KeyMatch(chr, false, VK_UNDEFINED, Character.toString(chr), C | M, mods));
    }

    public static KeyMatch forcode(int code, int mods) {
	return(new KeyMatch('\0', false, code, KeyEvent.getKeyText(code), S | C | M, mods));
    }

    public static KeyMatch forevent(KeyEvent ev, int modmask) {
	int mod = mods(ev) & modmask;
	char key = Character.toUpperCase(ev.getKeyChar());
	int code = ev.getExtendedKeyCode();
	if(key == KeyEvent.CHAR_UNDEFINED)
	    key = 0;
	if(code != VK_UNDEFINED) {
	    String nm;
	    if(ev.getKeyCode() != VK_UNDEFINED)
		nm = KeyEvent.getKeyText(ev.getKeyCode());
	    else
		nm = String.format("%X", code);
	    return(new KeyMatch('\0', false, code, nm, modmask, mod));
	}
	if(key > 32)
	    return(new KeyMatch(key, false, VK_UNDEFINED, Character.toString(key), modmask, mod));
	return(null);
    }

    public String reduce() {
	StringBuilder buf = new StringBuilder();
	if((modmask & S) != 0)
	    buf.append(((modmatch & S) != 0) ? 'S' : 's');
	if((modmask & C) != 0)
	    buf.append(((modmatch & C) != 0) ? 'C' : 'c');
	if((modmask & M) != 0)
	    buf.append(((modmatch & M) != 0) ? 'M' : 'm');
	if(chr != 0)
	    buf.append(String.format("g%s%x:%s", casematch ? "=" : "", (int)chr, keyname));
	else if(code != VK_UNDEFINED)
	    buf.append(String.format("k%x:%s", (int)code, keyname));
	else
	    buf.append(String.format("n"));
	return(buf.toString());
    }

    public static String reduce(KeyMatch key) {
	if(key == null)
	    return("");
	return(key.reduce());
    }

    public static KeyMatch restore(String desc) {
	int modmask = 0, modmatch = 0, p = 0;
	while(p < desc.length()) {
	    switch(desc.charAt(p++)) {
	    case 'S': modmatch |= S; case 's': modmask |= S; break;
	    case 'C': modmatch |= C; case 'c': modmask |= C; break;
	    case 'M': modmatch |= M; case 'm': modmask |= M; break;
	    case 'g': {
		boolean casematch = false;
		if(desc.charAt(p) == '=') {
		    casematch = true;
		    p++;
		}
		int p2 = desc.indexOf(':', p);
		char chr = (char)Integer.parseInt(desc.substring(p, p2), 16);
		String nm = desc.substring(p2 + 1);
		return(new KeyMatch(chr, casematch, VK_UNDEFINED, nm, modmask, modmatch));
	    }
	    case 'k': {
		int p2 = desc.indexOf(':', p);
		int code = (char)Integer.parseInt(desc.substring(p, p2), 16);
		String nm = desc.substring(p2 + 1);
		return(new KeyMatch('\0', false, code, nm, modmask, modmatch));
	    }
	    case 'n':
		return(nil);
	    default:
		return(null);
	    }
	}
	return(null);
    }

    public static class Capture extends Button {
	public KeyMatch key;
	private UI.Grab grab = null;

	private static String namefor(KeyMatch key) {
	    if(key == null)
		return("None");
	    return(key.name());
	}

	public Capture(int w, KeyMatch key) {
	    super(w, namefor(key));
	    this.key = key;
	}

	public void set(KeyMatch key) {
	    change(namefor(key));
	    this.key = key;
	}

	public void click() {
	    if(grab == null) {
		change("...");
		grab = ui.grabkeys(this);
	    } else {
		grab.remove();
		grab = null;
		change(namefor(this.key));
	    }
	}

	protected boolean handle(KeyEvent ev) {
	    switch(ev.getKeyCode()) {
	    case KeyEvent.VK_SHIFT: case KeyEvent.VK_CONTROL: case KeyEvent.VK_ALT:
	    case KeyEvent.VK_META: case KeyEvent.VK_WINDOWS:
		return(false);
	    }
	    int code = ev.getKeyCode();
	    if(code == KeyEvent.VK_ESCAPE) {
		change(namefor(this.key));
		return(true);
	    }
	    if(code == KeyEvent.VK_DELETE) {
		set(nil);
		return(true);
	    }
	    KeyMatch key = forevent(ev, S | C | M);
	    if(key != null)
		set(key);
	    else
		change(namefor(this.key));
	    return(true);
	}

	public boolean keydown(KeyEvent ev) {
	    if(grab == null)
		return(super.keydown(ev));
	    if(handle(ev)) {
		grab.remove();
		grab = null;
	    }
	    return(true);
	}
    }
}
