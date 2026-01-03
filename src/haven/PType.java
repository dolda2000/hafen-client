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
import haven.Utils.ArgumentFormatException;
import haven.PType.PTypes.*;

public interface PType<T> {
    public static final PType<String> STR = new Cast<>(String.class);
    public static final PType<Number> NUM = new Cast<>(Number.class);
    public static final PType<Integer> INT = new MapValue<>("int", NUM, Number::intValue);
    public static final PType<Long> UINT = new MapValue<>("uint", INT, Utils::uint32);
    public static final PType<Float> FLOAT = new MapValue<>("float", NUM, Number::floatValue);
    public static final PType<Double> DOUBLE = new MapValue<>("double", NUM, Number::doubleValue);
    public static final PType<Boolean> BOOL = new Or<>("bool", new Cast<>(Boolean.class), new MapValue<>(null, INT, v -> v != 0));
    public static final PType<Indir<Resource>> IRES = new MapValue<>("ires", new Cast<>(Indir.class), s -> () -> (Resource)s.get());
    public static final PType<Resource> RES = new Or<>("res", new Cast<>(Resource.class),
						       new OFunction<>(null, v -> {
							       if(!(v instanceof Indir))
								   return(Optional.empty());
							       Object res = ((Indir)v).get();
							       if(!(res instanceof Resource))
								   return(Optional.empty());
							       return(Optional.of((Resource)res));
						       }));
    public static final PType<Object[]> OBJS = new Or<>("object-array", new Cast<>(Object[].class),
						       new MapValue<>(null, new Cast<>(List.class), l -> ((List<?>)l).toArray(new Object[0])));
    public static final PType<List<?>> LIST = new Or<>("object-list", new MapValue<>(null, new Cast<List>(List.class), l -> (List<?>)l),
							    new MapValue<>(null, new Cast<>(Object[].class), a -> Arrays.asList(a)));
    public static final PType<Map<?, ?>> MAP = new MapValue<>("map", new Cast<>(Map.class), m -> (Map<?, ?>)m);

    public Optional<? extends T> opt(Object val);
    public T of(Object val);
    public default boolean is(Object val) {
	return(opt(val).isPresent());
    }

    public static class PTypes {
	public static interface Named<T> extends PType<T> {
	    public String name();

	    public default T of(Object val) {
		if(val == null)
		    return(null);
		return(opt(val).orElseThrow(() -> new ArgumentFormatException(name(), val)));
	    }
	}

	public static class Or<T> implements Named<T> {
	    public final String name;
	    private final Collection<PType<? extends T>> variants;

	    @SafeVarargs
	    public Or(String name, PType<? extends T>... variants) {
		this.name = name;
		this.variants = Arrays.asList(variants);
	    }

	    public String name() {return(name);}

	    public Optional<? extends T> opt(Object val) {
		for(PType<? extends T> var : variants) {
		    Optional<? extends T> ret = var.opt(val);
		    if(ret.isPresent())
			return(ret);
		}
		return(Optional.empty());
	    }
	}

	public static class OFunction<T> implements Named<T> {
	    public final String name;
	    public final Function<Object, Optional<? extends T>> fun;

	    public OFunction(String name, Function<Object, Optional<? extends T>> fun) {
		this.name = name;
		this.fun = fun;
	    }

	    public String name() {return(name);}

	    public Optional<? extends T> opt(Object val) {
		return(fun.apply(val));
	    }
	}

	public static class Cast<T> implements Named<T> {
	    public final Class<T> cl;

	    public Cast(Class<T> cl) {
		this.cl = cl;
	    }

	    public String name() {
		return(cl.getSimpleName().toLowerCase());
	    }

	    public Optional<T> opt(Object val) {
		if(!cl.isInstance(val))
		    return(Optional.empty());
		return(Optional.of(cl.cast(val)));
	    }
	}

	public static class MapValue<R, P> implements Named<R> {
	    public final String name;
	    public final PType<P> bk;
	    public final Function<P, R> xf;

	    public MapValue(String name, PType<P> bk, Function<P, R> xf) {
		this.name = name;
		this.bk = bk;
		this.xf = xf;
	    }

	    public String name() {return(name);}

	    public Optional<R> opt(Object val) {
		return(bk.opt(val).map(xf));
	    }
	}
    }
}
