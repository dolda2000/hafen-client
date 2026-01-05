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

public interface Maybe<T> extends Supplier<T> {
    public boolean has();

    public static class MissingValue extends RuntimeException {
	public MissingValue() {}
	public MissingValue(Throwable cause) {super(cause);}
	public MissingValue(String msg) {super(msg);}
	public MissingValue(String msg, Throwable cause) {super(msg, cause);}

	public static MissingValue wrap(Throwable cause) {
	    if(cause instanceof MissingValue)
		return((MissingValue)cause);
	    return(new MissingValue(cause));
	}
    }

    public default T or(T val) {
	return(has() ? get() : val);
    }

    public default <R> Maybe<R> map(Function<? super T, ? extends R> fn) {
	Maybe<T> t = this;
	return(new Maybe<R>() {
		public R get() {return(fn.apply(t.get()));}
		public boolean has() {return(t.has());}
	    });
    }

    public static <T> Maybe<T> of(T value) {
	return(new Maybe<T>() {
		public T get() {return(value);}
		public boolean has() {return(true);}
	    });
    }

    public static <T> Maybe<T> from(Supplier<? extends T> value) {
	try {
	    return(of(value.get()));
	} catch(Throwable t) {
	    return(not(t));
	}
    }

    public static <T> Maybe<T> reason(String reason) {
	return(new Maybe<T>() {
		public T get() {throw(new MissingValue(reason));}
		public boolean has() {return(false);}
	    });
    }

    public static <T> Maybe<T> reason(Supplier<String> reason) {
	return(new Maybe<T>() {
		public T get() {throw(new MissingValue(reason.get()));}
		public boolean has() {return(false);}
	    });
    }

    static final Maybe<?> empty = new Maybe<Object>() {
	    public Object get() {throw(new MissingValue());}
	    public boolean has() {return(false);}
	};
    @SuppressWarnings("unchecked")
    public static <T> Maybe<T> not() {
	return((Maybe<T>)empty);
    }

    public static <T> Maybe<T> not(Throwable cause) {
	return(new Maybe<T>() {
		public T get() {throw(MissingValue.wrap(cause));}
		public boolean has() {return(false);}
	    });
    }

    public static <T> Maybe<T> not(Supplier<? extends Throwable> cause) {
	return(new Maybe<T>() {
		public T get() {throw(MissingValue.wrap(cause.get()));}
		public boolean has() {return(false);}
	    });
    }
}
