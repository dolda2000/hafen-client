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
import java.util.function.*;

public interface OwnerContext {
    public <T> T context(Class<T> cl);

    public static class NoContext extends RuntimeException {
	public final Class<?> requested;

	public NoContext(String message, Throwable cause, Class<?> requested) {
	    super(message, cause);
	    this.requested = requested;
	}
	public NoContext(String message, Class<?> requested) {
	    super(message);
	    this.requested = requested;
	}
	public NoContext(Class<?> requested) {
	    this(String.format("No %s context here", requested), requested);
	}
    }

    public static class ClassResolver<T> {
	private final Map<Class<?>, Function<T, ?>> reg = new HashMap<>();

	public <C> ClassResolver<T> add(Class<C> cl, Function<T, ? extends C> p) {
	    synchronized(reg) {
		reg.put(cl, p);
	    }
	    return(this);
	}

	@SuppressWarnings("unchecked")
	private <C> Function<T, ? extends C> get(Class<C> cl) {
	    Function<T, ?> p;
	    synchronized(reg) {
		p = reg.get(cl);
		if(p == null) {
		    for(Map.Entry<Class<?>, Function<T, ?>> pr : reg.entrySet()) {
			if(cl.isAssignableFrom(pr.getKey())) {
			    reg.put(cl, p = pr.getValue());
			    break;
			}
		    }
		}
	    }
	    return((Function<T, ? extends C>)p);
	}

	public <C> C context(Class<C> cl, T on) {
	    Function<T, ? extends C> p = get(cl);
	    if(p == null)
		throw(new NoContext(cl));
	    return(get(cl).apply(on));
	}

	public OwnerContext curry(T on) {
	    return(new OwnerContext() {
		    public <C> C context(Class<C> cl) {
			return(ClassResolver.this.context(cl, on));
		    }
		});
	}
    }

    public static final ClassResolver<UI> uictx = new ClassResolver<UI>()
	.add(Glob.class, ui -> ui.sess.glob)
	.add(Session.class, ui -> ui.sess);
}
