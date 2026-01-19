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

public class UnionMap<K, V> extends AbstractMap<K, V> {
    private final Collection<Map<? extends K, ? extends V>> parts;

    public UnionMap(Collection<Map<? extends K, ? extends V>> parts) {
	this.parts = parts;
    }

    /* XXX: This assumes there are no null values in the part maps. */
    public V get(Object key) {
	for(Map<? extends K, ? extends V> info : parts) {
	    V ret = info.get(key);
	    if(ret != null)
		return(ret);
	}
	return(null);
    }

    public Set<Entry<K, V>> entrySet() {
	return(new AbstractSet<Entry<K, V>>() {
		public Iterator<Entry<K, V>> iterator() {
		    Iterator<Map<? extends K, ? extends V>> di = parts.iterator();
		    return(new Iterator<Entry<K, V>>() {
			    Iterator<? extends Entry<? extends K, ? extends V>> ci;
			    Entry<? extends K, ? extends V> cur = null;

			    public boolean hasNext() {
				if(cur != null)
				    return(true);
				while((ci == null) || !ci.hasNext()) {
				    if(!di.hasNext())
					return(false);
				    ci = di.next().entrySet().iterator();
				}
				cur = ci.next();
				return(true);
			    }

			    public Entry<K, V> next() {
				if(!hasNext())
				    throw(new NoSuchElementException());
				Entry<K, V> ret = new SimpleImmutableEntry<>(cur.getKey(), cur.getValue());
				cur = null;
				return(ret);
			    }
			});
		}

		public int size() {
		    int n = 0;
		    for(Object e : this) n++;
		    return(n);
		}
	    });
    }
}
