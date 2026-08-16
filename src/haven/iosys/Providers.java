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

package haven.iosys;

import haven.*;
import java.util.*;
import java.util.function.*;
import java.lang.annotation.*;

public class Providers<S, A extends Annotation> {
    private final String desc;
    private final Supplier<String> config;
    private final Class<A> annotation;
    private final Function<A, String> nmattr;

    public Providers(String desc, Supplier<String> config, Class<A> annotation, Function<A, String> nmattr) {
	this.desc = desc;
	this.config = config;
	this.annotation = annotation;
	this.nmattr = nmattr;
    }

    public static interface Factory<S> {
	public S open(String... args);
	public default int priority() {return(0);}
	public default boolean experimental() {return(false);}
	public default boolean autouse() {return(true);}
    }

    private Map<String, Factory<? extends S>> find() {
	Map<String, Factory<? extends S>> ret = new HashMap<>();
	ClassLoader loader = annotation.getClassLoader();
	for(String clnm : dolda.jglob.Loader.get(annotation).names()) {
	    try {
		Class<?> cl;
		try {
		    cl = loader.loadClass(clnm);
		} catch(ClassNotFoundException e) {
		    continue;
		}
		String nm = nmattr.apply(cl.getAnnotation(annotation));
		Factory<?> fac;
		try {
		    fac = (Factory<?>)Utils.invoke(cl.getDeclaredMethod("get"), null);
		} catch(NoSuchMethodException e) {
		    throw(new AssertionError(e));
		} catch(Unavailable | LinkageError e) {
		    fac = new Factory<S>() {
			    public S open(String... args) {throw(new Unavailable(e));}
			    public int priority() {return(-1000);}
			};
		}
		@SuppressWarnings("unchecked")
		    Factory<? extends S> cfac = (Factory<? extends S>)fac;
		ret.put(nm, cfac);
	    } catch(UnsupportedClassVersionError e) {
		continue;
	    }
	}
	return(ret);
    }

    private Map<String, Factory<? extends S>> found = null;
    public Map<String, Factory<? extends S>> found() {
	if(found == null) {
	    synchronized(this) {
		if(found == null) {
		    found = find();
		}
	    }
	}
	return(found);
    }

    public static <S> S findfirst(Collection<Factory<? extends S>> prov, String desc) {
	List<Factory<? extends S>> types = new ArrayList<>(prov);
	Collections.sort(types, Comparator.comparing(Factory<? extends S>::priority).reversed());
	Collection<Throwable> errors = new ArrayList<>();
	S first = null;
	for(Factory<? extends S> type : types) {
	    if(!type.autouse() || (type.experimental() && !Config.exp.get()))
		continue;
	    try {
		first = type.open();
		break;
	    } catch(Unavailable e) {
		errors.add(e);
	    }
	}
	if(first == null) {
	    Unavailable exc = new Unavailable("could find no working " + desc);
	    errors.forEach(exc::addSuppressed);
	    throw(exc);
	}
	return(first);
    }

    private S instance = null;
    public S instance() {
	if(instance == null) {
	    synchronized(this) {
		if(instance == null) {
		    if(Utils.eq(config.get(), "help")) {
			List<Map.Entry<String, Factory<? extends S>>> types = new ArrayList<>(found().entrySet());
			Collections.sort(types, Comparator.comparing(Map.Entry<String, Factory<? extends S>>::getValue, Comparator.comparing(Factory::priority)).reversed());
			for(Map.Entry<String, Factory<? extends S>> ent : types)
			    System.out.printf("name: %-19s priority: %5d%s\n", ent.getKey(), ent.getValue().priority(), ent.getValue().autouse() ? "" : " (manual only)");
			System.exit(0);
		    } else if(config.get() != null) {
			String spec = config.get();
			int p = spec.indexOf(':');
			String fnm = (p < 0) ? spec : spec.substring(0, p);
			Factory<? extends S> f = found().get(fnm);
			if(f == null)
			    throw(new Unavailable("no such " + desc + " name: " + fnm));
			instance = (p < 0) ? f.open() : f.open(spec.substring(p + 1));;
		    } else {
			instance = findfirst(found().values(), desc);
		    }
		}
	    }
	}
	return(instance);
    }
}
