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
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

public class Widget {
    public UI ui;
    public Coord c, sz;
    public Widget next, prev, child, lchild, parent;
    public boolean focustab = false, focusctl = false, hasfocus = false, visible = true;
    private boolean canfocus = false, autofocus = false;
    public boolean canactivate = false, cancancel = false;
    public Widget focused;
    public Resource cursor = null;
    public Object tooltip = null;
    private Widget prevtt;
    static Map<String, WidgetFactory> types = new TreeMap<String, WidgetFactory>();
    static Class<?>[] barda = {Img.class, TextEntry.class, MapView.class, FlowerMenu.class,
			       Window.class, Button.class, Inventory.class, GItem.class, Listbox.class,
			       Makewindow.class, Chatwindow.class, Textlog.class, Equipory.class, IButton.class,
			       Cal.class, Avaview.class, NpcChat.class,
			       Label.class, Progress.class, VMeter.class, Partyview.class,
			       MenuGrid.class, CheckBox.class,
			       ISBox.class, Fightview.class, IMeter.class, MapMod.class,
			       GiveButton.class, Charlist.class, BuddyWnd.class,
			       Speedget.class, Bufflist.class, GameUI.class};
	
    static {
	addtype("cnt", new WidgetFactory() {
		public Widget create(Coord c, Widget parent, Object[] args) {
		    return(new Widget(c, (Coord)args[0], parent));
		}
	    });
    }
	
    public static void initbardas() {
	try {
	    for(Class<?> c : barda)
		Class.forName(c.getName(), true, c.getClassLoader());
	} catch(ClassNotFoundException e) {
	    throw(new Error(e));
	}
    }
	
    public static void addtype(String name, WidgetFactory fct) {
	synchronized(types) {
	    types.put(name, fct);
	}
    }
	
    public static WidgetFactory gettype2(String name) throws InterruptedException {
	if(name.indexOf('/') < 0) {
	    synchronized(types) {
		return(types.get(name));
	    }
	} else {
	    int ver = -1, p;
	    if((p = name.indexOf(':')) > 0) {
		ver = Integer.parseInt(name.substring(p + 1));
		name = name.substring(0, p);
	    }
	    Resource res = Resource.load(name, ver);
	    res.loadwaitint();
	    return(res.layer(Resource.CodeEntry.class).get(WidgetFactory.class));
	}
    }
    
    public static WidgetFactory gettype(String name) {
	WidgetFactory f;
	try {
	    f = gettype2(name);
	} catch(InterruptedException e) {
	    /* XXX: This is not proper behavior. On the other hand,
	     * InterruptedException should not be checked. :-/ */
	    throw(new RuntimeException("Interrupted while loading resource widget", e));
	}
	if(f == null)
	    throw(new RuntimeException("No such widget type: " + name));
	return(f);
    }
    
    public Widget(UI ui, Coord c, Coord sz) {
	this.ui = ui;
	this.c = c;
	this.sz = sz;
    }
	
    public Widget(Coord c, Coord sz, Widget parent) {
	synchronized(parent.ui) {
	    this.ui = parent.ui;
	    this.c = c;
	    this.sz = sz;
	    this.parent = parent;
	    link();
	}
    }
    
    public Widget makechild(String type, Object[] pargs, Object[] cargs) {
	Coord c = (Coord)pargs[0];
	return(gettype(type).create(c, this, cargs));
    }
	
    public void link() {
	synchronized(ui) {
	    if(parent.lchild != null)
		parent.lchild.next = this;
	    if(parent.child == null)
		parent.child = this;
	    this.prev = parent.lchild;
	    parent.lchild = this;
	}
    }
    
    public void linkfirst() {
	synchronized(ui) {
	    if(parent.child != null)
		parent.child.prev = this;
	    if(parent.lchild == null)
		parent.lchild = this;
	    this.next = parent.child;
	    parent.child = this;
	}
    }
	
    public void unlink() {
	synchronized(ui) {
	    if(next != null)
		next.prev = prev;
	    if(prev != null)
		prev.next = next;
	    if(parent.child == this)
		parent.child = next;
	    if(parent.lchild == this)
		parent.lchild = prev;
	    next = null;
	    prev = null;
	}
    }
	
    public Coord xlate(Coord c, boolean in) {
	return(c);
    }
	
    public Coord rootpos() {
	if(parent == null)
	    return(new Coord(0, 0));
	return(xlate(parent.rootpos().add(c), true));
    }
	
    public boolean hasparent(Widget w2) {
	for(Widget w = this; w != null; w = w.parent) {
	    if(w == w2)
		return(true);
	}
	return(false);
    }
	
    public void gotfocus() {
	if(focusctl && (focused != null)) {
	    focused.hasfocus = true;
	    focused.gotfocus();
	}
    }
	
    public void destroy() {
	if(canfocus)
	    setcanfocus(false);
	parent.cdestroy(this);
    }
    
    public void cdestroy(Widget w) {
    }
	
    public void lostfocus() {
	if(focusctl && (focused != null)) {
	    focused.hasfocus = false;
	    focused.lostfocus();
	}
    }
	
    public void setfocus(Widget w) {
	if(focusctl) {
	    if(w != focused) {
		Widget last = focused;
		focused = w;
		if(last != null)
		    last.hasfocus = false;
		w.hasfocus = true;
		if(last != null)
		    last.lostfocus();
		w.gotfocus();
		if((ui != null) && ui.rwidgets.containsKey(w))
		    wdgmsg("focus", ui.rwidgets.get(w));
	    }
	    if(parent != null)
		parent.setfocus(this);
	} else {
	    parent.setfocus(w);
	}
    }
	
    public void setcanfocus(boolean canfocus) {
	this.autofocus = this.canfocus = canfocus;
	if(parent != null) {
	    if(canfocus) {
		parent.newfocusable(this);
	    } else {
		parent.delfocusable(this);
	    }
	}
    }
	
    public void newfocusable(Widget w) {
	if(focusctl) {
	    if(focused == null)
		setfocus(w);
	} else {
	    parent.newfocusable(w);
	}
    }
	
    public void delfocusable(Widget w) {
	if(focusctl) {
	    if(focused == w)
		findfocus();
	} else {
	    parent.delfocusable(w);
	}
    }
	
    private void findfocus() {
	/* XXX: Might need to check subwidgets recursively */
	focused = null;
	for(Widget w = lchild; w != null; w = w.prev) {
	    if(w.visible && w.autofocus) {
		focused = w;
		focused.hasfocus = true;
		w.gotfocus();
		break;
	    }
	}
    }
	
    public void setfocusctl(boolean focusctl) {
	if(this.focusctl = focusctl) {
	    findfocus();
	    setcanfocus(true);
	}
    }
	
    public void setfocustab(boolean focustab) {
	if(focustab && !focusctl)
	    setfocusctl(true);
	this.focustab = focustab;
    }
	
    public void uimsg(String msg, Object... args) {
	if(msg == "tabfocus") {
	    setfocustab(((Integer)args[0] != 0));
	} else if(msg == "act") {
	    canactivate = (Integer)args[0] != 0;
	} else if(msg == "cancel") {
	    cancancel = (Integer)args[0] != 0;
	} else if(msg == "autofocus") {
	    autofocus = (Integer)args[0] != 0;
	} else if(msg == "focus") {
	    Widget w = ui.widgets.get((Integer)args[0]); 
	    if(w != null) {
		if(w.canfocus)
		    setfocus(w);
	    }
	} else if(msg == "curs") {
	    if(args.length == 0)
		cursor = null;
	    else
		cursor = Resource.load((String)args[0], (Integer)args[1]);
	} else {
	    System.err.println("Unhandled widget message: " + msg);
	}
    }
	
    public void wdgmsg(String msg, Object... args) {
	wdgmsg(this, msg, args);
    }
	
    public void wdgmsg(Widget sender, String msg, Object... args) {
	if(parent == null)
	    ui.wdgmsg(sender, msg, args);
	else
	    parent.wdgmsg(sender, msg, args);
    }
	
    public void tick(double dt) {
	Widget next;
	
	for(Widget wdg = child; wdg != null; wdg = next) {
	    next = wdg.next;
	    wdg.tick(dt);
	}
    }

    public void draw(GOut g) {
	Widget next;
		
	for(Widget wdg = child; wdg != null; wdg = next) {
	    next = wdg.next;
	    if(!wdg.visible)
		continue;
	    Coord cc = xlate(wdg.c, true);
	    wdg.draw(g.reclip(cc, wdg.sz));
	}
    }
	
    public boolean mousedown(Coord c, int button) {
	for(Widget wdg = lchild; wdg != null; wdg = wdg.prev) {
	    if(!wdg.visible)
		continue;
	    Coord cc = xlate(wdg.c, true);
	    if(c.isect(cc, wdg.sz)) {
		if(wdg.mousedown(c.add(cc.inv()), button)) {
		    return(true);
		}
	    }
	}
	return(false);
    }
	
    public boolean mouseup(Coord c, int button) {
	for(Widget wdg = lchild; wdg != null; wdg = wdg.prev) {
	    if(!wdg.visible)
		continue;
	    Coord cc = xlate(wdg.c, true);
	    if(c.isect(cc, wdg.sz)) {
		if(wdg.mouseup(c.add(cc.inv()), button)) {
		    return(true);
		}
	    }
	}
	return(false);
    }
	
    public boolean mousewheel(Coord c, int amount) {
	for(Widget wdg = lchild; wdg != null; wdg = wdg.prev) {
	    if(!wdg.visible)
		continue;
	    Coord cc = xlate(wdg.c, true);
	    if(c.isect(cc, wdg.sz)) {
		if(wdg.mousewheel(c.add(cc.inv()), amount)) {
		    return(true);
		}
	    }
	}
	return(false);
    }
	
    public void mousemove(Coord c) {
	for(Widget wdg = lchild; wdg != null; wdg = wdg.prev) {
	    if(!wdg.visible)
		continue;
	    Coord cc = xlate(wdg.c, true);
	    wdg.mousemove(c.add(cc.inv()));
	}
    }
	
    public boolean globtype(char key, KeyEvent ev) {
	for(Widget wdg = child; wdg != null; wdg = wdg.next) {
	    if(wdg.globtype(key, ev))
		return(true);
	}
	return(false);
    }
	
    public boolean type(char key, KeyEvent ev) {
	if(canactivate) {
	    if(key == 10) {
		wdgmsg("activate");
		return(true);
	    }
	}
	if(cancancel) {
	    if(key == 27) {
		wdgmsg("cancel");
		return(true);
	    }
	}
	if(focusctl) {
	    if(focused != null) {
		if(focused.type(key, ev))
		    return(true);
		if(focustab) {
		    if(key == '\t') {
			Widget f = focused;
			while(true) {
			    if((ev.getModifiers() & InputEvent.SHIFT_MASK) == 0)
				f = (f.next == null)?child:f.next;
			    else
				f = (f.prev == null)?lchild:f.prev;
			    if(f.canfocus)
				break;
			}
			setfocus(f);
			return(true);
		    } else {
			return(false);
		    }
		} else {
		    return(false);
		}
	    } else {
		return(false);
	    }
	} else {
	    for(Widget wdg = child; wdg != null; wdg = wdg.next) {
		if(wdg.type(key, ev))
		    return(true);
	    }
	    return(false);
	}
    }
	
    public boolean keydown(KeyEvent ev) {
	if(focusctl) {
	    if(focused != null) {
		if(focused.keydown(ev))
		    return(true);
		return(false);
	    } else {
		return(false);
	    }
	} else {
	    for(Widget wdg = child; wdg != null; wdg = wdg.next) {
		if(wdg.keydown(ev))
		    return(true);
	    }
	}
	return(false);
    }
	
    public boolean keyup(KeyEvent ev) {
	if(focusctl) {
	    if(focused != null) {
		if(focused.keyup(ev))
		    return(true);
		return(false);
	    } else {
		return(false);
	    }
	} else {
	    for(Widget wdg = child; wdg != null; wdg = wdg.next) {
		if(wdg.keyup(ev))	
		    return(true);
	    }
	}
	return(false);
    }
    
    public void pack() {
	Coord max = new Coord(0, 0);
	for(Widget wdg = child; wdg != null; wdg = wdg.next) {
	    Coord br = wdg.c.add(wdg.sz);
	    if(br.x > max.x)
		max.x = br.x;
	    if(br.y > max.y)
		max.y = br.y;
	}
	resize(max);
    }
    
    public void resize(Coord sz) {
	this.sz = sz;
	for(Widget ch = child; ch != null; ch = ch.next)
	    ch.presize();
    }
    
    public void presize() {
    }
	
    public void raise() {
	synchronized(ui) {
	    unlink();
	    link();
	}
    }
    
    public void lower() {
	synchronized(ui) {
	    unlink();
	    linkfirst();
	}
    }
    
    @Deprecated
    public <T extends Widget> T findchild(Class<T> cl) {
	for(Widget wdg = child; wdg != null; wdg = wdg.next) {
	    if(cl.isInstance(wdg))
		return(cl.cast(wdg));
	    T ret = wdg.findchild(cl);
	    if(ret != null)
		return(ret);
	}
	return(null);
    }
    
    public <T extends Widget> Set<T> children(final Class<T> cl) {
	return(new AbstractSet<T>() {
		public int size() {
		    int i = 0;
		    for(T w : this)
			i++;
		    return(i);
		}
		
		public Iterator<T> iterator() {
		    return(new Iterator<T>() {
			    T cur = n(Widget.this);
			    
			    private T n(Widget w) {
				Widget n;
				if(w == null) {
				    return(null);
				} else if(w.child != null) {
				    n = w.child;
				} else if(cur.next != null) {
				    n = w.next;
				} else if(cur.parent == Widget.this) {
				    return(null);
				} else {
				    n = cur.parent;
				}
				if(cl.isInstance(n))
				    return(cl.cast(n));
				else
				    return(n(n));
			    }
			    
			    public T next() {
				T ret = cur;
				cur = n(ret);
				return(ret);
			    }
			    
			    public boolean hasNext() {
				return(cur != null);
			    }
			    
			    public void remove() {
				throw(new UnsupportedOperationException());
			    }
			});
		}
	    });
    }

    public Resource getcurs(Coord c) {
	Resource ret;
		
	for(Widget wdg = lchild; wdg != null; wdg = wdg.prev) {
	    if(!wdg.visible)
		continue;
	    Coord cc = xlate(wdg.c, true);
	    if(c.isect(cc, wdg.sz)) {
		if((ret = wdg.getcurs(c.add(cc.inv()))) != null)
		    return(ret);
	    }
	}
	return(cursor);
    }

    public Object tooltip(Coord c, boolean again) {
	if(tooltip != null) {
	    prevtt = null;
	    return(tooltip);
	}
	for(Widget wdg = lchild; wdg != null; wdg = wdg.prev) {
	    if(!wdg.visible)
		continue;
	    Coord cc = xlate(wdg.c, true);
	    if(c.isect(cc, wdg.sz)) {
		Object ret = wdg.tooltip(c.add(cc.inv()), again && (wdg == prevtt));
		if(ret != null) {
		    prevtt = wdg;
		    return(ret);
		}
	    }
	}
	prevtt = null;
	return(null);
    }
    
    public <T extends Widget> T getparent(Class<T> cl) {
	for(Widget w = this; w != null; w = w.parent) {
	    if(cl.isInstance(w))
		return(cl.cast(w));
	}
	return(null);
    }

    public void hide() {
	visible = false;
	if(canfocus)
	    parent.delfocusable(this);
    }

    public void show() {
	visible = true;
	if(canfocus)
	    parent.newfocusable(this);
    }
    
    public boolean show(boolean show) {
	if(show)
	    show();
	else
	    hide();
	return(show);
    }
}
