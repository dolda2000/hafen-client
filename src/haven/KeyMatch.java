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

public abstract class KeyMatch {
    public static final int S = 1, C = 2, M = 4, SUPER = 8, MODS = S | C | M;
    public String keyname;
    public int modmask, modmatch;

    public KeyMatch(String keyname, int modmask, int modmatch) {
	this.keyname = keyname;
	this.modmask = modmask & MODS;
	this.modmatch = modmatch & MODS;
    }

    public static class CharMatch extends KeyMatch {
	public char chr;
	public boolean casematch;

	public CharMatch(char chr, boolean casematch, String keyname, int modmask, int modmatch) {
	    super(keyname, modmask, modmatch);
	    this.chr = casematch ? chr : Character.toUpperCase(chr);
	    this.casematch = casematch;
	}

	public boolean match(KeyEvent ev, int modign) {
	    int mod = mods(ev);
	    if((mod & modmask & ~modign) != (modmatch & ~modign))
		return(false);
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
	}

	private boolean equals(CharMatch that) {
	    return((this.chr == that.chr) && (this.casematch == that.casematch) && equals((KeyMatch)that));
	}

	public boolean equals(Object o) {
	    return((o instanceof CharMatch) && equals((CharMatch)o));
	}

	public String reduce() {
	    StringBuilder buf = new StringBuilder();
	    reducemods(buf);
	    buf.append(String.format("g%s%x:%s", casematch ? "=" : "", (int)chr, keyname));
	    return(buf.toString());
	}

	public static CharMatch restore(char id, String desc, int modmask, int modmatch) {
	    int p = 0;
	    boolean casematch = false;
	    if(desc.charAt(p) == '=') {
		casematch = true;
		p++;
	    }
	    int p2 = desc.indexOf(':', p);
	    char chr = (char)Integer.parseInt(desc.substring(p, p2), 16);
	    String nm = desc.substring(p2 + 1);
	    return(new CharMatch(chr, casematch, nm, modmask, modmatch));
	}
    }

    public static class CodeMatch extends KeyMatch {
	public int code;
	public boolean extmatch;

	public CodeMatch(int code, boolean extmatch, String keyname, int modmask, int modmatch) {
	    super(keyname, modmask, modmatch);
	    this.code = code;
	    this.extmatch = extmatch;
	}

	public boolean match(KeyEvent ev, int modign) {
	    int mod = mods(ev);
	    if((mod & modmask & ~modign) != (modmatch & ~modign))
		return(false);
	    if(extmatch)
		return(ev.getExtendedKeyCode() == code);
	    else
		return(ev.getKeyCode() == code);
	}

	private boolean equals(CodeMatch that) {
	    return((this.code == that.code) && (this.extmatch == that.extmatch) && equals((KeyMatch)that));
	}

	public boolean equals(Object o) {
	    return((o instanceof CodeMatch) && equals((CodeMatch)o));
	}

	public String reduce() {
	    StringBuilder buf = new StringBuilder();
	    reducemods(buf);
	    buf.append(String.format("%c%x:%s", extmatch ? 'k' : 'K', (int)code, keyname));
	    return(buf.toString());
	}

	public static CodeMatch restore(char id, String desc, int modmask, int modmatch) {
	    int p = desc.indexOf(':');
	    int code = Integer.parseInt(desc.substring(0, p), 16);
	    String nm = desc.substring(p + 1);
	    return(new CodeMatch(code, id == 'k', nm, modmask, modmatch));
	}
    }

    public static final KeyMatch nil = new KeyMatch("None", 0, 0) {
	public boolean match(KeyEvent ev, int modign) {
	    return(false);
	}

	public String reduce() {
	    StringBuilder buf = new StringBuilder();
	    reducemods(buf);
	    buf.append('n');
	    return(buf.toString());
	}
    };

    public static int mods(KeyEvent ev) {return(UI.modflags(ev));}

    @Deprecated
    public abstract boolean match(KeyEvent ev, int modign);

    @Deprecated
    public boolean match(KeyEvent ev) {
	return(match(ev, 0));
    }

    public boolean match(Widget.KbdEvent ev, int modign) {
	return(match(ev.awt, modign));
    }

    public boolean match(Widget.KbdEvent ev) {
	return(match(ev.awt));
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

    protected boolean equals(KeyMatch that) {
	return((this.modmask == that.modmask) && (this.modmatch == that.modmatch));
    }

    public static KeyMatch forchar(char chr, int modmask, int modmatch) {
	return(new CharMatch(chr, false, Character.toString(chr), modmask, modmatch));
    }
    public static KeyMatch forchar(char chr, int mods) {
	return(forchar(chr, S | C | M, mods));
    }

    public static KeyMatch forcode(int code, int modmask, int modmatch) {
	return(new CodeMatch(code, false, KeyEvent.getKeyText(code), modmask, modmatch));
    }
    public static KeyMatch forcode(int code, int mods) {
	return(forcode(code, S | C | M, mods));
    }

    @Deprecated
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
	    else if(!Character.isISOControl(key))
		nm = Character.toString(key);
	    else
		nm = String.format("%X", code);
	    return(new CodeMatch(code, true, nm, modmask, mod));
	}
	if(!Character.isISOControl(key))
	    return(new CharMatch(key, false, Character.toString(key), modmask, mod));
	return(null);
    }
    public static KeyMatch forevent(Widget.KbdEvent ev, int modmask) {
	return(forevent(ev.awt, modmask));
    }

    protected void reducemods(StringBuilder buf) {
	if((modmask & S) != 0)
	    buf.append(((modmatch & S) != 0) ? 'S' : 's');
	if((modmask & C) != 0)
	    buf.append(((modmatch & C) != 0) ? 'C' : 'c');
	if((modmask & M) != 0)
	    buf.append(((modmatch & M) != 0) ? 'M' : 'm');
    }
    public abstract String reduce();

    public static String reduce(KeyMatch key) {
	if(key == null)
	    return("");
	return(key.reduce());
    }

    public static KeyMatch restore(String desc) {
	int modmask = 0, modmatch = 0, p = 0;
	while(p < desc.length()) {
	    char c = desc.charAt(p++);
	    switch(c) {
	    case 'S': modmatch |= S; case 's': modmask |= S; break;
	    case 'C': modmatch |= C; case 'c': modmask |= C; break;
	    case 'M': modmatch |= M; case 'm': modmask |= M; break;
	    case 'g':
		return(CharMatch.restore(c, desc.substring(p), modmask, modmatch));
	    case 'k': case 'K':
		return(CodeMatch.restore(c, desc.substring(p), modmask, modmatch));
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
	    super(w, namefor(key), false);
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

	protected KeyMatch mkmatch(KbdEvent ev) {
	    return(forevent(ev, MODS));
	}

	protected boolean handle(KbdEvent ev) {
	    switch(ev.code) {
	    case KeyEvent.VK_SHIFT: case KeyEvent.VK_CONTROL: case KeyEvent.VK_ALT:
	    case KeyEvent.VK_META: case KeyEvent.VK_WINDOWS:
		return(false);
	    }
	    if(ev.code == KeyEvent.VK_ESCAPE) {
		change(namefor(this.key));
		return(true);
	    }
	    if(ev.code == KeyEvent.VK_DELETE) {
		set(nil);
		return(true);
	    }
	    KeyMatch key = mkmatch(ev);
	    if(key != null)
		set(key);
	    else
		change(namefor(this.key));
	    return(true);
	}

	public boolean keydown(KeyDownEvent ev) {
	    if(!ev.grabbed)
		return(super.keydown(ev));
	    if(handle(ev)) {
		grab.remove();
		grab = null;
	    }
	    return(true);
	}
    }

    public static String modname(int mods) {
	if(mods == 0)
	    return("None");
	StringBuilder buf = new StringBuilder();
	if((mods & S) != 0) {
	    if(buf.length() > 0)
		buf.append('+');
	    buf.append("Shift");
	}
	if((mods & C) != 0) {
	    if(buf.length() > 0)
		buf.append('+');
	    buf.append("Ctrl");
	}
	if((mods & M) != 0) {
	    if(buf.length() > 0)
		buf.append('+');
	    buf.append("Alt");
	}
	if(buf.length() == 0)
	    return("???");
	return(buf.toString());
    }

    public static class ModCapture extends Button  {
	public final int mask;
	public int match, nmatch;
	private UI.Grab grab = null;

	public ModCapture(int w, int mask, int match) {
	    super(w, modname(match), false);
	    this.mask = mask;
	    this.match = match;
	}

	public ModCapture(int w, int match) {
	    this(w, MODS, match);
	}

	public void set(int match) {
	    change(modname(match));
	    this.match = match;
	}

	public void click() {
	    if(grab == null) {
		change("...");
		grab = ui.grabkeys(this);
		nmatch = 0;
	    } else {
		grab.remove();
		grab = null;
		change(modname(match));
	    }
	}

	protected boolean handle(KeyEvent ev) {
	    switch(ev.getKeyCode()) {
	    case KeyEvent.VK_ESCAPE:
		change(modname(match));
		return(true);
	    case KeyEvent.VK_DELETE:
		set(0);
		return(true);
	    }
	    return(false);
	}

	public boolean keyup(KeyUpEvent ev) {
	    if(!ev.grabbed)
		return(super.keyup(ev));
	    switch(ev.code) {
	    case KeyEvent.VK_SHIFT: case KeyEvent.VK_CONTROL: case KeyEvent.VK_ALT:
	    case KeyEvent.VK_META: case KeyEvent.VK_WINDOWS:
		grab.remove();
		grab = null;
		set(nmatch);
		return(true);
	    }
	    return(true);
	}

	public boolean keydown(KeyDownEvent ev) {
	    if(!ev.grabbed)
		return(super.keydown(ev));
	    switch(ev.code) {
	    case KeyEvent.VK_SHIFT: case KeyEvent.VK_CONTROL: case KeyEvent.VK_ALT:
	    case KeyEvent.VK_META: case KeyEvent.VK_WINDOWS:
		nmatch |= (mods(ev.awt) & mask);
		return(true);
	    }
	    if(handle(ev)) {
		grab.remove();
		grab = null;
	    }
	    return(true);
	}
    }
}
