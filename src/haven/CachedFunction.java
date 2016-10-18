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

public class CachedFunction<P, R> implements Function<P, R> {
    public final Function<P, R> back;
    public final Consumer<? super R> dispose;
    private final Map<P, R> cache;

    public CachedFunction(int size, Function<P, R> back, Consumer<? super R> dispose) {
	this.back = back;
	this.dispose = dispose;
	this.cache = new Cache(size);
    }

    public CachedFunction(int size, Function<P, R> back) {
	this(size, back, null);
    }

    private class Cache extends LinkedHashMap<P, R> {
	private final int size;

	public Cache(int size) {
	    super(size, 0.75f, true);
	    this.size = size;
	}

	protected boolean removeEldestEntry(Map.Entry<P, R> eldest) {
	    if(size() > size) {
		if(dispose != null)
		    dispose.accept(eldest.getValue());
		return(true);
	    }
	    return(false);
	}
    }

    public R apply(P param) {
	if(cache.containsKey(param))
	    return(cache.get(param));
	R ret = back.apply(param);
	cache.put(param, ret);
	return(ret);
    }
}
