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

public class BackCache<K, V> {
    public final Function<K, V> load;
    public final BiConsumer<K, V> store;
    public final BiConsumer<K, V> dispose;
    private final Map<K, V> cache;

    public BackCache(int size, Function<K, V> load, BiConsumer<K, V> store, BiConsumer<K, V> dispose) {
	this.load = load;
	this.store = store;
	this.dispose = dispose;
	this.cache = new Cache(size);
    }

    public BackCache(int size, Function<K, V> load, BiConsumer<K, V> store) {
	this(size, load, store, null);
    }

    private class Cache extends LinkedHashMap<K, V> {
	private final int size;

	private Cache(int size) {
	    super(size, 0.75f, true);
	    this.size = size;
	}

	protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
	    if(size() > size) {
		if(dispose != null)
		    dispose.accept(eldest.getKey(), eldest.getValue());
		return(true);
	    }
	    return(false);
	}
    }

    public boolean cached(K key) {
	return(cache.containsKey(key));
    }

    public V get(K key) {
	if(cache.containsKey(key))
	    return(cache.get(key));
	V ret = load.apply(key);
	cache.put(key, ret);
	return(ret);
    }

    public void put(K key, V val) {
	store.accept(key, val);
	cache.put(key, val);
    }
}
