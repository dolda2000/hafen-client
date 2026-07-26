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
import java.util.function.*;
import java.awt.event.*;
import static haven.KeyMatch.*;

public interface ReadLine {
    public char[] buffer();
    public int length();
    public int point();
    public void point(int p);
    public int mark();
    public void mark(int m);
    public void setline(String line);
    public boolean key(char c, KeyEvent ev, int mod);
    public double mtime();

    public default boolean empty() {return(length() == 0);}
    public default String line() {return(new String(buffer(), 0, length()));}
    public default Text render(Text.Foundry f) {return(f.render(line()));}
    public default void select(int from, int to) {mark(from); point(to);}

    public default boolean lneq(String ln) {
	int len = length();
	if(ln.length() != len)
	    return(false);
	char[] b = buffer();
	for(int i = 0; i < len; i++) {
	    if(b[i] != ln.charAt(i))
		return(false);
	}
	return(true);
    }

    public default boolean key(KeyEvent ev) {
	int mod = UI.modflags(ev);
	char c = ev.getKeyChar();
	if(c == KeyEvent.CHAR_UNDEFINED)
	    c = '\0';
	if(((mod & C) != 0) && (c < 32)) {
	    /* Undo Java's TTY Control-code mangling */
	    if(ev.getKeyCode() == KeyEvent.VK_BACK_SPACE) {
	    } else if(ev.getKeyCode() == KeyEvent.VK_ENTER) {
	    } else if(ev.getKeyCode() == KeyEvent.VK_TAB) {
	    } else if(ev.getKeyCode() == KeyEvent.VK_ESCAPE) {
	    } else {
		if((ev.getModifiersEx() & InputEvent.SHIFT_DOWN_MASK) != 0)
		    c = (char)(c + 'A' - 1);
		else
		    c = (char)(c + 'a' - 1);
	    }
	}
	return(key(c, ev, mod));
    }

    public static interface Owner {
	public UI ui();
	public default void changed(ReadLine buf) {}
	public default void done(ReadLine buf) {}
    }

    public static abstract class Base implements ReadLine {
	public final Owner owner;
	public char[] buf = new char[16];
	public int length = 0;
	public int point = 0;
	public int seq = 0;
	public double mtime;

	public Base(Owner owner, String init) {
	    this.owner = owner;
	    line(init);
	    point(length);
	}

	public String line(int off, int len) {
	    return(new String(buf, off, len));
	}

	public void line(String ln) {
	    if(buf.length < ln.length()) {
		Arrays.fill(buf, (char)0);
		buf = new char[ln.length() * 2];
	    }
	    ln.getChars(0, length = ln.length(), buf, 0);
	    seq++;
	}

	public char[] remove(int off, int len) {
	    System.arraycopy(buf, off + len, buf, off, (length -= len) - off);
	    seq++;
	    return(buf);
	}

	public char[] ensure(int off, int len) {
	    if(length + len > buf.length) {
		int nl = buf.length * 2;
		while(length + len > nl)
		    nl *= 2;
		char[] nb = Arrays.copyOf(buf, nl);
		Arrays.fill(buf, (char)0);
		buf = nb;
	    }
	    System.arraycopy(buf, off, buf, off + len, (length += len) - len - off);
	    seq++;
	    return(buf);
	}

	public static boolean wordchar(char c) {
	    return(Character.isLetterOrDigit(c));
	}

	public int wordstart(int from) {
	    while((from > 0) && !wordchar(buf[from - 1])) from--;
	    while((from > 0) && wordchar(buf[from - 1])) from--;
	    return(from);
	}

	public int wordend(int from) {
	    while((from < length) && !wordchar(buf[from])) from++;
	    while((from < length) && wordchar(buf[from])) from++;
	    return(from);
	}

	protected abstract boolean key2(char c, KeyEvent ev, int mod);

	public boolean key(char c, KeyEvent ev, int mod) {
	    int pseq = this.seq;
	    boolean ret = key2(c, ev, mod);
	    if(this.seq != pseq)
		owner.changed(this);
	    if(ret)
		mtime = Utils.rtime();
	    return(ret);
	}

	public char[] buffer() {return(buf);}
	public int length() {return(length);}
	public int point() {return(point);}
	public void point(int p) {point = p;}
	public double mtime() {return(mtime);}

	public void setline(String line) {
	    if(!lneq(line)) {
		line(line);
		if(point > length)
		    point = length;
		owner.changed(this);
	    }
	}

	private Text tcache = null;
	public Text render(Text.Foundry f) {
	    if((tcache == null) || !lneq(tcache.text))
		tcache = f.render(line());
	    return(tcache);
	}

	static {
	    Console.setscmd("editmode", (cons, args) -> {
		Utils.setpref("editmode", args[1]);
	    });
	}
    }

    public static class PCLine extends Base {
	public int mark = -1;

	public PCLine(Owner owner, String init) {
	    super(owner, init);
	}

	public void clipset(Clipboard c) {
	    String text = line(Math.min(mark, point), Math.abs(point - mark));
	    c.put(new Clipboard.Contents(new Clipboard.Item<CharSequence>(Clipboard.Format.TEXT, text)));
	}

	public static Promise<CharSequence> cliptext(Clipboard c) {
	    return(c.get().then(cnt -> cnt.or(Clipboard.Format.TEXT, null).get()));
	}

	public Promise<CharSequence> cliptext() {
	    return(cliptext(owner.ui().wnd.clipboard(Clipboard.Std.PRIMARY))
		   .then(val -> {
		       if(val != null)
			   return(new Promise<CharSequence>().resolve(val));
		       return(cliptext(owner.ui().wnd.clipboard(Clipboard.Std.CLIPBOARD)));
		   }));
	}

	private void paste(CharSequence text) {
	    if(text == null)
		return;
	    synchronized(owner.ui()) {
		rmsel();
		int l = 0;
		for(; l < text.length(); l++) {
		    if(text.charAt(l) < 32)
			break;
		}
		char[] dst = ensure(point, l);
		for(int i = 0; i < l; i++)
		    dst[point++] = text.charAt(i);
		owner.changed(this);
	    }
	}

	public int mark() {return(mark);}
	public void mark(int mark) {this.mark = mark;}

	public void setline(String line) {
	    super.setline(line);
	    mark = -1;
	}

	public void rmsel() {
	    if(mark >= 0) {
		int a = Math.min(point, mark), b = Math.max(point, mark);
		remove(a, b - a);
		point = a; mark = -1;
	    }
	}

	private void cksel() {
	    if(mark >= 0) {
		clipset(owner.ui().wnd.clipboard(Clipboard.Std.PRIMARY));
	    }
	}

	public boolean key2(char c, KeyEvent ev, int mod) {
	    int code = ev.getKeyCode();
	    boolean s = (mod & S) != 0;
	    mod &= ~S;
	    if((c == 8) && (mod == 0)) {
		if(mark < 0) {
		    if(point > 0)
			remove(--point, 1);
		} else {
		    rmsel();
		}
	    } else if((c == 8) && (mod == C)) {
		if(mark < 0) {
		    int b = wordstart(point);
		    remove(b, point - b);
		    point = b;
		} else {
		    rmsel();
		}
	    } else if(Widget.key_act.match(ev)) {
		owner.done(this);
	    } else if((c == 127) && (mod == 0)) {
		if(mark < 0) {
		    if(point < length)
			remove(point, 1);
		} else {
		    rmsel();
		}
	    } else if((c == 127) && (mod == C)) {
		if(mark < 0) {
		    int b = wordend(point);
		    remove(point, b - point);
		} else {
		    rmsel();
		}
	    } else if((c >= 32) && (mod == 0)) {
		rmsel();
		ensure(point, 1)[point++] = c;
	    } else if((code == KeyEvent.VK_LEFT) && (mod == 0)) {
		mark = s ? (mark < 0) ? point : mark : -1;
		if(point > 0)
		    point--;
	    } else if((code == KeyEvent.VK_LEFT) && (mod == C)) {
		mark = s ? (mark < 0) ? point : mark : -1;
		point = wordstart(point);
	    } else if((code == KeyEvent.VK_RIGHT) && (mod == 0)) {
		mark = s ? (mark < 0) ? point : mark : -1;
		if(point < length)
		    point++;
	    } else if((code == KeyEvent.VK_RIGHT) && (mod == C)) {
		mark = s ? (mark < 0) ? point : mark : -1;
		point = wordend(point);
	    } else if((code == KeyEvent.VK_HOME) && (mod == 0)) {
		mark = s ? (mark < 0) ? point : mark : -1;
		point = 0;
	    } else if((code == KeyEvent.VK_END) && (mod == 0)) {
		mark = s ? (mark < 0) ? point : mark : -1;
		point = length;
	    } else if((c == 'v') && (mod == C)) {
		cliptext().map(this::paste).report(owner.ui(), "Clipboard error");
	    } else if((c == 'c') && (mod == C)) {
		if(mark >= 0)
		    clipset(owner.ui().wnd.clipboard(Clipboard.Std.CLIPBOARD));
		else
		    return(false);
	    } else {
		return(false);
	    }
	    cksel();
	    return(true);
	}
    }

    public static class EmacsLine extends Base {
	private int mark, yankpos, undopos;
	private boolean tmm;
	private String last = "";
	private List<String> yanklist = new ArrayList<String>();
	private List<UndoState> undolist = new ArrayList<UndoState>();
	{undolist.add(new UndoState());}

	public EmacsLine(Owner owner, String init) {
	    super(owner, init);
	}

	private class UndoState {
	    private String line;
	    private int point;

	    private UndoState() {
		this.line = line();
		this.point = point();
	    }
	}

	private void save() {
	    if(!lneq(undolist.get(undolist.size() - 1).line))
		undolist.add(new UndoState());
	}

	private void mode(String mode) {
	    if((mode == "") || (last != mode))
		save();
	    last = mode;
	}

	private String lastsel = "", lastclip = "";
	private void killclipboard() {
	    String cl;
	    if(!(cl = cliptext(owner.ui().wnd.clipboard(Clipboard.Std.PRIMARY))).equals(lastsel)) {
		lastsel = cl;
		kill(cl);
		return;
	    }
	    if(!(cl = cliptext(owner.ui().wnd.clipboard(Clipboard.Std.CLIPBOARD))).equals(lastclip)) {
		lastclip = cl;
		kill(cl);
		return;
	    }
	}

	private void kill(String text) {
	    killclipboard();
	    yanklist.add(text);
	}

	private String cliptext(Clipboard c) {
	    try {
		Clipboard.Item<CharSequence> item = c.fetch().find(Clipboard.Format.TEXT);
		if(item != null)
		    return(item.fetch().toString());
	    } catch(Exception e) {
		owner.ui().error("Clipboard error: " + e.getMessage());
	    }
	    return("");
	}

	public int mark() {return(tmm ? mark : -1);}
	public void mark(int m) {
	    if(m < 0) {
		tmm = false;
	    } else {
		this.mark = m;
		tmm = true;
	    }
	}

	public void rmsel() {
	    if(tmm) {
		int a = Math.min(point, mark), b = Math.max(point, mark);
		remove(a, b - a);
		point = mark = a; tmm = false;
	    }
	}

	public boolean key2(char c, KeyEvent ev, int mod) {
	    mod &= ~S;
	    if(mark > length)
		mark = length;
	    String last = this.last;
	    if((c == 8) && (mod == 0)) {
		mode("erase");
		if(point > 0)
		    remove(--point, 1);
	    } else if((c == 8) && ((mod == C) || (mod == M))) {
		mode("backward-kill-word");
		save();
		int b = wordstart(point);
		if(last == "backward-kill-word")
		    yanklist.set(yanklist.size() - 1, line(b, point - b) + yanklist.get(yanklist.size() - 1));
		else
		    kill(line(b, point - b));
		remove(b, point - b);
		point = b;
	    } else if(Widget.key_act.match(ev)) {
		owner.done(this);
	    } else if((c == 'd') && (mod == C)) {
		mode("erase");
		if(point < length)
		    remove(point, 1);
	    } else if((c == 'd') && (mod == M)) {
		mode("kill-word");
		save();
		int b = wordend(point);
		if(last == "kill-word")
		    yanklist.set(yanklist.size() - 1, yanklist.get(yanklist.size() - 1) + line(point, b - point));
		else
		    kill(line(point, b - point));
		remove(point, b - point);
	    } else if((c == 'b') && (mod == C)) {
		mode("move");
		if(point > 0)
		    point--;
	    } else if((c == 'b') && (mod == M)) {
		mode("move");
		point = wordstart(point);
	    } else if((c == 'f') && (mod == C)) {
		mode("move");
		if(point < length)
		    point++;
	    } else if((c == 'f') && (mod == M)) {
		mode("move");
		point = wordend(point);
	    } else if((c == 'a') && (mod == C)) {
		mode("move");
		point = 0;
	    } else if((c == 'e') && (mod == C)) {
		mode("move");
		point = length;
	    } else if((c == 't') && (mod == C)) {
		mode("transpose");
		if((length >= 2) && (point > 0)) {
		    if(point < length) {
			char t = buf[point - 1];
			buf[point - 1] = buf[point];
			buf[point] = t;
			point++;
		    } else {
			char t = buf[point - 2];
			buf[point - 2] = buf[point - 1];
			buf[point - 1] = t;
		    }
		}
		seq++;
	    } else if((c == 'k') && (mod == C)) {
		mode("");
		kill(line(point, length - point));
		length = point;
		seq++;
	    } else if((c == 'w') && (mod == M)) {
		mode("");
		if(mark < point) {
		    kill(line(mark, point - mark));
		} else {
		    kill(line(point, mark - point));
		}
	    } else if((c == 'w') && (mod == C)) {
		mode("");
		if(mark < point) {
		    kill(line(mark, point - mark));
		    remove(mark, point - mark);
		} else {
		    kill(line(point, mark - point));
		    remove(point, mark - point);
		}
	    } else if((c == 'y') && (mod == C)) {
		mode("yank");
		save();
		killclipboard();
		yankpos = yanklist.size();
		if(yankpos > 0) {
		    String yank = yanklist.get(--yankpos);
		    mark = point;
		    yank.getChars(0, yank.length(), ensure(point, yank.length()), point);
		    point = mark + yank.length();
		}
	    } else if((c == 'y') && (mod == M)) {
		mode("yank");
		save();
		if((last == "yank") && (yankpos > 0)) {
		    String yank = yanklist.get(--yankpos);
		    if(yank.length() > point - mark)
			yank.getChars(0, yank.length(), ensure(point, yank.length() - (point - mark)), mark);
		    else
			yank.getChars(0, yank.length(), remove(point, (point - mark) - yank.length()), mark);
		    point = mark + yank.length();
		}
	    } else if((c == ' ') && (mod == C)) {
		mode("");
		mark = point;
	    } else if((c == '_') && (mod == C)) {
		mode("undo");
		save();
		if(last != "undo")
		    undopos = undolist.size() - 1;
		if(undopos > 0) {
		    UndoState s = undolist.get(--undopos);
		    line(s.line);
		    point = s.point;
		}
	    } else if((c == 'g') && (mod == C)) {
		if(tmm)
		    tmm = false;
		else
		    return(false);
	    } else if((c >= 32) && (mod == 0)) {
		mode("insert");
		rmsel();
		ensure(point, 1)[point++] = c;
	    } else {
		return(false);
	    }
	    if(mark > length)
		mark = length;
	    return(true);
	}
    }

    public static ReadLine make(Owner owner, String init) {
	switch(Utils.getpref("editmode", "pc")) {
	case "emacs":
	    return(new EmacsLine(owner, init));
	default:
	    return(new PCLine(owner, init));
	}
    }
}
