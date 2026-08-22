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

package haven.ffi.objc;

import haven.*;
import haven.ffi.*;
import haven.ffi.objc.Runtime.*;
import haven.ffi.objc.Foundation.*;
import haven.ffi.objc.CoreGraphics.*;
import java.lang.invoke.*;
import java.lang.foreign.*;
import java.util.*;
import java.util.function.*;
import java.nio.*;
import java.nio.charset.*;
import haven.ffi.objc.Runtime.Class;
import java.lang.foreign.MemoryLayout.PathElement;
import static haven.ffi.ABI.*;
import static haven.ffi.FUtils.*;
import static java.lang.foreign.ValueLayout.ADDRESS;

public abstract class CoreFoundation {
    public static interface CFData {
	public MemorySegment getBytePtr();
    }

    abstract void CFRelease(MemorySegment object);
    abstract MemorySegment CFRetain(MemorySegment object);
    abstract void gcrelease(Object obj, MemorySegment object);

    abstract CFData CFData(MemorySegment ref, boolean release, Object keep);

    static class VersionA extends CoreFoundation {
	private final SymbolLookup dylib = SymbolLookup.libraryLookup("/System/Library/Frameworks/CoreFoundation.framework/CoreFoundation", Arena.global());
	static final MemoryLayout CFTypeRef = ADDRESS;

	private final MethodHandle CFRelease = ld.downcallHandle(dylib.find("CFRelease").get(), FunctionDescriptor.ofVoid(CFTypeRef));
	void CFRelease(MemorySegment object) {
	    try {
		CFRelease.invoke(object);
	    } catch(Throwable e) {throw(new RuntimeException(e));}
	}

	private final MethodHandle CFRetain = ld.downcallHandle(dylib.find("CFRetain").get(), FunctionDescriptor.of(CFTypeRef, CFTypeRef));
	MemorySegment CFRetain(MemorySegment object) {
	    try {
		return((MemorySegment)CFRetain.invoke(object));
	    } catch(Throwable e) {throw(new RuntimeException(e));}
	}

	void gcrelease(Object obj, MemorySegment object) {
	    if(object == null)
		throw(new NullPointerException());
	    Finalizer.finalize(obj, () -> CFRelease(object));
	}

	private final MethodHandle CFDataGetBytePtr = ld.downcallHandle(dylib.find("CFDataGetBytePtr").get(), FunctionDescriptor.of(ADDRESS, ADDRESS));
	class CFData implements CoreFoundation.CFData {
	    final MemorySegment ref;
	    final Object keep;

	    CFData(MemorySegment ref, boolean release, Object keep) {
		this.ref = ref;
		this.keep = keep;
		if(release)
		    gcrelease(this, ref);
	    }

	    public MemorySegment getBytePtr() {
		try {
		    return((MemorySegment)CFDataGetBytePtr.invoke(ref));
		} catch(Throwable e) {throw(new RuntimeException(e));}
	    }
	}

	CFData CFData(MemorySegment ref, boolean release, Object keep) {
	    return(new CFData(ref, release, keep));
	}
    }

    private static CoreFoundation instance = null;
    public static CoreFoundation get() {
	if(instance == null) {
	    synchronized(CoreFoundation.class) {
		if(instance == null) {
		    instance = new VersionA();
		}
	    }
	}
	return(instance);
    }
}
