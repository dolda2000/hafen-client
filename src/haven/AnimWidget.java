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
import haven.Widget.*;

public interface AnimWidget {
    /* Duplicated from Widget. */
    public <E extends Event> void listen(Class<E> t, EventHandler<? super E> h);
    public boolean deafen(EventHandler<?> h);
    public <H extends EventHandler<?>> H listening(Class<H> cl);

    public static class Handler implements EventHandler<TickEvent> {
	public final AnimWidget w;
	public final Collection<Anim> anims = new ArrayList<>();
	public final Collection<Anim> nanims = new ArrayList<>();

	public Handler(AnimWidget w) {
	    this.w = w;
	}

	private void checkempty() {
	    if(anims.isEmpty() && nanims.isEmpty())
		w.deafen(this);
	}

	public boolean handle(TickEvent ev) {
	    anims.addAll(nanims);
	    nanims.clear();
	    for(Iterator<Anim> i = anims.iterator(); i.hasNext();) {
		Anim anim = i.next();
		if(anim.tick(ev.dt))
		    i.remove();
	    }
	    checkempty();
	    return(false);
	}

	public void add(Anim anim) {
	    nanims.add(anim);
	}

	public void remove(Anim anim) {
	    nanims.remove(this);
	    anims.remove(this);
	    checkempty();
	}

	public static Handler get(AnimWidget w, boolean creat) {
	    Handler ret = w.listening(Handler.class);
	    if(ret == null) {
		if(!creat)
		    return(null);
		ret = new Handler(w);
		w.listen(TickEvent.class, ret);
	    }
	    return(ret);
	}
    }

    public default <A extends Anim> A animated(Class<A> type) {
	Handler h = Handler.get(this, false);
	if(h == null)
	    return(null);
	for(Anim a : h.nanims) {
	    if(type.isInstance(a))
		return(type.cast(a));
	}
	for(Anim a : h.anims) {
	    if(type.isInstance(a))
		return(type.cast(a));
	}
	return(null);
    }

    public default void clearanims(Class<?> type) {
	Handler h = Handler.get(this, false);
	if(h == null)
	    return;
	for(Iterator<Anim> i = h.nanims.iterator(); i.hasNext();) {
	    Anim a = i.next();
	    if(type.isInstance(a))
		i.remove();
	}
	for(Iterator<Anim> i = h.anims.iterator(); i.hasNext();) {
	    Anim a = i.next();
	    if(type.isInstance(a))
		i.remove();
	}
    }

    public abstract class Anim {
	public final AnimWidget w;

	public Anim(AnimWidget w) {
	    this.w = w;
	    Handler.get(w, true).add(this);
	}

	public void clear() {
	    Handler.get(w, true).remove(this);
	}

	public abstract boolean tick(double dt);
    }

    public abstract class NormAnim extends Anim {
	private double a = 0.0;
	private final double s;
	
	public NormAnim(AnimWidget w, double s) {
	    super(w);
	    this.s = 1.0 / s;
	}

	public boolean tick(double dt) {
	    a += dt;
	    double na = a * s;
	    if(na >= 1.0) {
		ntick(1.0);
		return(true);
	    } else {
		ntick(na);
		return(false);
	    }
	}

	public abstract void ntick(double a);
    }
}
