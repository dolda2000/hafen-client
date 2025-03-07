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

import java.util.function.*;

public class ResID extends Number implements Indir<Resource> {
    public final int id;
    public final Resource.Resolver src;

    private ResID(int id, Resource.Resolver src) {
	this.id = id;
	this.src = src;
    }

    private ResID(int id) {
	this(id, null);
    }

    public static ResID of(int id) {
	return(new ResID(id));
    }

    public ResID src(Resource.Resolver src) {
	return(new ResID(id, src));
    }

    public Resource get() {
	return(src.getres(id).get());
    }

    public int intValue() {return(id);}

    public byte byteValue() {return((byte)id);}
    public short shortValue() {return((short)id);}
    public long longValue() {return((long)id);}
    public float floatValue() {return((float)id);}
    public double doubleValue() {return((double)id);}

    public int hashCode() {
	return(id);
    }
    public boolean equals(Object x) {
	return((x instanceof ResID) && (((ResID)x).id == id));
    }

    public String toString() {
	return("#<res-id " + id + ">");
    }

    public static class ResolveMapper implements Function<Object, Object> {
	public final Resource.Resolver rr;

	public ResolveMapper(Resource.Resolver rr) {
	    this.rr = rr;
	}

	public Object apply(Object obj) {
	    if(obj instanceof ResID)
		return(((ResID)obj).src(rr));
	    return(obj);
	}
    }
}
