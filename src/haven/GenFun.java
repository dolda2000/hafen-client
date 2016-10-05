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
import java.lang.reflect.*;

public class GenFun<T> {
    public final Class<T> iface;
    public final T call;
    private final Map<Class<?>, T> cache = new java.util.concurrent.ConcurrentHashMap<>();
    private final Map<Class<?>, T> registry = new HashMap<>();

    public GenFun(Class<T> iface) {
	this.iface = iface;
	this.call = iface.cast(Proxy.newProxyInstance(iface.getClassLoader(), new Class<?>[] {iface}, new Handler()));
    }

    public GenFun<T> register(Class<?> cl, T impl) {
	synchronized(registry) {
	    registry.put(cl, impl);
	    cache.clear();
	}
	return(this);
    }

    private static final InvocationHandler passthrough = new InvocationHandler() {
	    public Object invoke(Object proxy, Method method, Object[] args) {
		try {
		    return(method.invoke(args[0], args));
		} catch(IllegalAccessException e) {
		    throw(new RuntimeException(e));
		} catch(InvocationTargetException e) {
		    if(e.getCause() instanceof RuntimeException)
			throw((RuntimeException)e.getCause());
		    throw(new RuntimeException(e));
		}
	    }
	};

    public static class MissingImplementationException extends RuntimeException {
	public MissingImplementationException(GenFun function, Object target) {
	    super("Missing implementation for genfun on " + function.iface.getName() + " for " + target.getClass().getName());
	}
    }

    private class Handler implements InvocationHandler {
	public Object invoke(Object proxy, Method method, Object[] args) {
	    Class<?> cl = args[0].getClass();
	    T impl = cache.get(cl);
	    if(impl == null) {
		if(iface.isAssignableFrom(cl)) {
		    impl = iface.cast(Proxy.newProxyInstance(iface.getClassLoader(), new Class<?>[] {iface}, passthrough));
		} else {
		    synchronized(registry) {
			for(Class<?> scl = cl; scl != null; scl = scl.getSuperclass()) {
			    if((impl = registry.get(scl)) != null)
				break;
			}
		    }
		}
		if(impl == null)
		    throw(new MissingImplementationException(GenFun.this, args[0]));
		cache.put(cl, impl);
	    }
	    return(null);
	}
    }
}
