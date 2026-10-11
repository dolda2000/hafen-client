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

package haven.ffi.windows;

import java.util.*;
import java.util.function.*;
import java.lang.invoke.*;
import java.lang.foreign.*;
import haven.ffi.*;
import haven.ffi.windows.COM.*;
import static haven.ffi.ABI.*;
import static java.lang.foreign.ValueLayout.ADDRESS;
import static haven.ffi.FUtils.*;

public interface IEnum<E> extends Iterable<E> {
    public List<E> Next(int count);
    public boolean Skip(int count);
    public void Reset();
    public IEnum<E> Clone();
    static final COM com = COM.get();

    public static Descriptor sig_Next(MemoryLayout nt, Function<MemorySegment, ?> cons) {
	Descriptor desc = new Descriptor(3, FunctionDescriptor.of(com.HRESULT(), ADDRESS, com.ULONG(), ADDRESS, ADDRESS), nt);
	MethodHandle fun = ld.downcallHandle(desc.sig);
	desc.downcall((self, args) -> {
	    int count = (Integer)args[0];
	    Arena alloc = Arena.ofAuto();
	    MemorySegment vbuf = alloc.allocate(nt, count), nbuf = alloc.allocate(com.ULONG());
	    int rv;
	    try {
		rv = (int)fun.invoke(com.getvtab(self, desc.vindex), self, count, vbuf, nbuf);;
	    } catch(Throwable t) {
		throw(new InvocationException(t));
	    }
	    if(!com.SUCCEEDED(rv))
		throw(new HResultError(rv));
	    int n = (int)getint(nbuf, 0, com.ULONG(), false);
	    return(new MemArray<>(vbuf, nt, n, cons));
	});
	desc.upcall(UpcallAdapter.nil);
	return(desc);
    }
    public static Descriptor sig_Skip(MemoryLayout nt) {
	return(new Descriptor(4, FunctionDescriptor.of(com.HRESULT(), ADDRESS, com.ULONG())));
    }
    public static Descriptor sig_Reset(MemoryLayout nt) {
	return(new Descriptor(5, FunctionDescriptor.of(com.HRESULT(), ADDRESS)));
    }
    public static Descriptor sig_Clone(MemoryLayout nt) {
	return(new Descriptor(6, FunctionDescriptor.of(com.HRESULT(), ADDRESS, ADDRESS), ADDRESS));
    }

    public default Iterator<E> iterator() {
	return(new Iterator<E>() {
	    Iterator<E> sub = null;
	    boolean end = false;

	    public boolean hasNext() {
		if(sub == null) {
		    if(end)
			return(false);
		    List<E> next = Next(16);
		    if(next.size() < 16)
			end = true;
		    if(next.isEmpty())
			return(false);
		    sub = next.iterator();
		}
		return(true);
	    }

	    public E next() {
		if(!hasNext())
		    throw(new NoSuchElementException());
		E ret = sub.next();
		if(!sub.hasNext())
		    sub = null;
		return(ret);
	    }
	});
    }
}
