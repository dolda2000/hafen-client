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

import java.util.*;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.InputEvent;

public class UI {
    public RootWidget root;
    private Widget keygrab, mousegrab;
    public Map<Integer, Widget> widgets = new TreeMap<Integer, Widget>();
    public Map<Widget, Integer> rwidgets = new HashMap<Widget, Integer>();
    Receiver rcvr;
    public Coord mc, lcc = Coord.z;
    public Session sess;
    public boolean modshift, modctrl, modmeta, modsuper;
    public Object lasttip;
    long lastevent, lasttick;
    public Widget mouseon;
    public Console cons = new WidgetConsole();
    private Collection<AfterDraw> afterdraws = null;
    
    {
	lastevent = lasttick = System.currentTimeMillis();
    }
	
    public interface Receiver {
	public void rcvmsg(int widget, String msg, Object... args);
    }
    
    public interface AfterDraw {
	public void draw(GOut g);
    }
	
    private class WidgetConsole extends Console {
	{
	    setcmd("q", new Command() {
		    public void run(Console cons, String[] args) {
			HackThread.tg().interrupt();
		    }
		});
	    setcmd("lo", new Command() {
		    public void run(Console cons, String[] args) {
			sess.close();
		    }
		});
	}
	
	private void findcmds(Map<String, Command> map, Widget wdg) {
	    if(wdg instanceof Directory) {
		Map<String, Command> cmds = ((Directory)wdg).findcmds();
		synchronized(cmds) {
		    map.putAll(cmds);
		}
	    }
	    for(Widget ch = wdg.child; ch != null; ch = ch.next)
		findcmds(map, ch);
	}

	public Map<String, Command> findcmds() {
	    Map<String, Command> ret = super.findcmds();
	    findcmds(ret, root);
	    return(ret);
	}
    }

    @SuppressWarnings("serial")
    public static class UIException extends RuntimeException {
	public String mname;
	public Object[] args;
		
	public UIException(String message, String mname, Object... args) {
	    super(message);
	    this.mname = mname;
	    this.args = args;
	}
    }
	
    public UI(Coord sz, Session sess) {
	root = new RootWidget(this, sz);
	widgets.put(0, root);
	rwidgets.put(root, 0);
	this.sess = sess;
    }
	
    public void setreceiver(Receiver rcvr) {
	this.rcvr = rcvr;
    }
	
    public void bind(Widget w, int id) {
	widgets.put(id, w);
	rwidgets.put(w, id);
    }
    
    public void drawafter(AfterDraw ad) {
	synchronized(afterdraws) {
	    afterdraws.add(ad);
	}
    }

    public void tick() {
	long now = System.currentTimeMillis();
	root.tick((now - lasttick) / 1000.0);
	lasttick = now;
    }

    public void draw(GOut g) {
	afterdraws = new LinkedList<AfterDraw>();
	root.draw(g);
	synchronized(afterdraws) {
	    for(AfterDraw ad : afterdraws) {
		ad.draw(g);
	    }
	}
	afterdraws = null;
    }
	
    public void newwidget(int id, String type, int parent, Object[] pargs, Object... cargs) throws InterruptedException {
	synchronized(this) {
	    Widget pwdg = widgets.get(parent);
	    if(pwdg == null)
		throw(new UIException("Null parent widget " + parent + " for " + id, type, cargs));
	    Widget wdg = pwdg.makechild(type.intern(), pargs, cargs);
	    bind(wdg, id);
	}
    }
	
    public void grabmouse(Widget wdg) {
	mousegrab = wdg;
    }
	
    public void grabkeys(Widget wdg) {
	keygrab = wdg;
    }
	
    private void removeid(Widget wdg) {
	if(rwidgets.containsKey(wdg)) {
	    int id = rwidgets.get(wdg);
	    widgets.remove(id);
	    rwidgets.remove(wdg);
	}
	for(Widget child = wdg.child; child != null; child = child.next)
	    removeid(child);
    }
	
    public void destroy(Widget wdg) {
	if((mousegrab != null) && mousegrab.hasparent(wdg))
	    mousegrab = null;
	if((keygrab != null) && keygrab.hasparent(wdg))
	    keygrab = null;
	removeid(wdg);
	wdg.destroy();
	wdg.unlink();
    }
    
    public void destroy(int id) {
	synchronized(this) {
	    if(widgets.containsKey(id)) {
		Widget wdg = widgets.get(id);
		destroy(wdg);
	    }
	}
    }
	
    public void wdgmsg(Widget sender, String msg, Object... args) {
	int id;
	synchronized(this) {
	    if(!rwidgets.containsKey(sender))
		throw(new UIException("Wdgmsg sender (" + sender.getClass().getName() + ") is not in rwidgets", msg, args));
	    id = rwidgets.get(sender);
	}
	if(rcvr != null)
	    rcvr.rcvmsg(id, msg, args);
    }
	
    public void uimsg(int id, String msg, Object... args) {
	Widget wdg;
	synchronized(this) {
	    wdg = widgets.get(id);
	}
	if(wdg != null)
	    wdg.uimsg(msg.intern(), args);
	else
	    throw(new UIException("Uimsg to non-existent widget " + id, msg, args));
    }
	
    private void setmods(InputEvent ev) {
	int mod = ev.getModifiersEx();
	modshift = (mod & InputEvent.SHIFT_DOWN_MASK) != 0;
	modctrl = (mod & InputEvent.CTRL_DOWN_MASK) != 0;
	modmeta = (mod & (InputEvent.META_DOWN_MASK | InputEvent.ALT_DOWN_MASK)) != 0;
	/*
 	modsuper = (mod & InputEvent.SUPER_DOWN_MASK) != 0;
	*/
    }

    public void type(KeyEvent ev) {
	setmods(ev);
	if(keygrab == null) {
	    if(!root.type(ev.getKeyChar(), ev))
		root.globtype(ev.getKeyChar(), ev);
	} else {
	    keygrab.type(ev.getKeyChar(), ev);
	}
    }
	
    public void keydown(KeyEvent ev) {
	setmods(ev);
	if(keygrab == null) {
	    if(!root.keydown(ev))
		root.globtype((char)0, ev);
	} else {
	    keygrab.keydown(ev);
	}
    }
	
    public void keyup(KeyEvent ev) {
	setmods(ev);
	if(keygrab == null)
	    root.keyup(ev);
	else
	    keygrab.keyup(ev);		
    }
	
    private Coord wdgxlate(Coord c, Widget wdg) {
	return(c.add(wdg.c.inv()).add(wdg.parent.rootpos().inv()));
    }
	
    public boolean dropthing(Widget w, Coord c, Object thing) {
	if(w instanceof DropTarget) {
	    if(((DropTarget)w).dropthing(c, thing))
		return(true);
	}
	for(Widget wdg = w.lchild; wdg != null; wdg = wdg.prev) {
	    Coord cc = w.xlate(wdg.c, true);
	    if(c.isect(cc, wdg.sz)) {
		if(dropthing(wdg, c.add(cc.inv()), thing))
		    return(true);
	    }
	}
	return(false);
    }

    public void mousedown(MouseEvent ev, Coord c, int button) {
	setmods(ev);
	lcc = mc = c;
	if(mousegrab == null)
	    root.mousedown(c, button);
	else
	    mousegrab.mousedown(wdgxlate(c, mousegrab), button);
    }
	
    public void mouseup(MouseEvent ev, Coord c, int button) {
	setmods(ev);
	mc = c;
	if(mousegrab == null)
	    root.mouseup(c, button);
	else
	    mousegrab.mouseup(wdgxlate(c, mousegrab), button);
    }
	
    public void mousemove(MouseEvent ev, Coord c) {
	setmods(ev);
	mc = c;
	if(mousegrab == null)
	    root.mousemove(c);
	else
	    mousegrab.mousemove(wdgxlate(c, mousegrab));
    }
	
    public void mousewheel(MouseEvent ev, Coord c, int amount) {
	setmods(ev);
	lcc = mc = c;
	if(mousegrab == null)
	    root.mousewheel(c, amount);
	else
	    mousegrab.mousewheel(wdgxlate(c, mousegrab), amount);
    }
    
    public int modflags() {
	return((modshift?1:0) |
	       (modctrl?2:0) |
	       (modmeta?4:0) |
	       (modsuper?8:0));
    }
}
