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
import haven.PType.PTypes.*;

public interface PType<T> {
    public static final PType<String> STR = new Cast<>(String.class);
    public static final PType<Number> NUM = new Cast<>(Number.class);
    public static final PType<Integer> INT = new MapValue<>(NUM, Number::intValue);
    public static final PType<Long> UINT = new MapValue<>(INT, Utils::uint32);
    public static final PType<Float> FLOAT = new MapValue<>(NUM, Number::floatValue);
    public static final PType<Double> DOUBLE = new MapValue<>(NUM, Number::doubleValue);
    public static final PType<Boolean> BOOL = new Or<>("bool", new Cast<>(Boolean.class), new MapValue<>(INT, v -> v != 0));
    public static final PType<Indir<Resource>> IRES = new Or<>("ires",
							       new MapValue<>(new Cast<>(Indir.class), s -> () -> (Resource)s.get()),
							       new MapValue<>(new Cast<>(Resource.class), Resource::indir));
    public static final PType<Resource> RES = new Or<>("res", new Cast<>(Resource.class),
						       new OFunction<>(null, v -> {
							       if(v instanceof Indir) {
								   Object res = ((Indir)v).get();
								   if(res instanceof Resource)
								       return(Maybe.of((Resource)res));
							       }
							       return(Maybe.not(() -> new ValueFormatException("resource", v)));
						       }));
    public static final PType<Object[]> OBJS = new Or<>("object-array", new Cast<>(Object[].class),
						       new MapValue<>(new Cast<>(List.class), l -> ((List<?>)l).toArray(new Object[0])));
    public static final PType<List<?>> LIST = new Or<>("object-list", new MapValue<>(new Cast<List>(List.class), l -> (List<?>)l),
							    new MapValue<>(new Cast<>(Object[].class), a -> Arrays.asList(a)));
    public static final PType<Map<?, ?>> MAP = new MapValue<>(new Cast<>(Map.class), m -> (Map<?, ?>)m);
    public static final PType<Coord> COORD = new Cast<>(Coord.class);
    public static final PType<Coord2d> FCOORD = new Cast<>(Coord2d.class);
    public static final PType<java.awt.Color> COLOR = new Cast<>(java.awt.Color.class);
    public static final PType<FColor> FCOLOR = new Or<>("fcolor", new Cast<>(FColor.class),
							new MapValue<>(COLOR, FColor::new));
    public static final PType<UID> UID = new Cast<>(UID.class);

    public Maybe<? extends T> opt(Object val);
    public default T of(Object val) {
	if(val == null)
	    return(null);
	return(opt(val).get());
    }
    public default boolean is(Object val) {
	return(opt(val).has());
    }

    public static class ValueFormatException extends Maybe.MissingValue {
	public final String expected;
	public final Object got;

	public ValueFormatException(String expected, Object got) {
	    this.expected = expected;
	    this.got = got;
	}

	public String getMessage() {
	    String got;
	    try {
		got = String.valueOf(this.got);
	    } catch(Throwable t) {
		got = "!formatting error (" + this.got.getClass() + ", " + t + ")";
	    }
	    return(String.format("expected %s, got %s", expected, got));
	}
    }

    public static class PTypes {
	public static class Or<T> implements PType<T> {
	    public final String name;
	    private final Collection<PType<? extends T>> variants;

	    @SafeVarargs
	    public Or(String name, PType<? extends T>... variants) {
		this.name = name;
		this.variants = Arrays.asList(variants);
	    }

	    public Maybe<? extends T> opt(Object val) {
		for(PType<? extends T> var : variants) {
		    Maybe<? extends T> ret = var.opt(val);
		    if(ret.has())
			return(ret);
		}
		return(Maybe.not(() -> new ValueFormatException(name, val)));
	    }
	}

	public static class OFunction<T> implements PType<T> {
	    public final String name;
	    public final Function<Object, Maybe<? extends T>> fun;

	    public OFunction(String name, Function<Object, Maybe<? extends T>> fun) {
		this.name = name;
		this.fun = fun;
	    }

	    public Maybe<? extends T> opt(Object val) {
		return(fun.apply(val));
	    }
	}

	public static class Cast<T> implements PType<T> {
	    public final Class<T> cl;

	    public Cast(Class<T> cl) {
		this.cl = cl;
	    }

	    public Maybe<T> opt(Object val) {
		if(!cl.isInstance(val))
		    return(Maybe.not(() -> new ValueFormatException(cl.getSimpleName().toLowerCase(), val)));
		return(Maybe.of(cl.cast(val)));
	    }
	}

	public static class MapValue<R, P> implements PType<R> {
	    public final PType<P> bk;
	    public final Function<P, R> xf;

	    public MapValue(PType<P> bk, Function<P, R> xf) {
		this.bk = bk;
		this.xf = xf;
	    }

	    public Maybe<R> opt(Object val) {
		return(bk.opt(val).map(xf));
	    }
	}
    }
}
