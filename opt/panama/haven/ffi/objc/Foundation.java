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
import java.lang.invoke.*;
import java.lang.foreign.*;
import java.lang.foreign.MemoryLayout.PathElement;
import java.util.*;
import static haven.ffi.ABI.*;
import static haven.ffi.FUtils.*;
import static java.lang.foreign.ValueLayout.ADDRESS;

public abstract class Foundation {
    public static interface NSString {
	public ID id();
	public String str();
    }

    public abstract NSString NSString(String str);

    public static interface NSProcessInfo {
	public String operatingSystemVersionString();
    }

    public abstract NSProcessInfo processInfo();

    static class VersionC extends Foundation {
	private final SymbolLookup dylib = SymbolLookup.libraryLookup("/System/Library/Frameworks/Foundation.framework/Foundation", Arena.global());
	private final Runtime rt = Runtime.get();
	private final SEL sel_alloc = rt.sel_registerName("alloc");

	private final Runtime.Class NSString = rt.objc_getClass("NSString");
	private final SEL sel_UTF8String = rt.sel_registerName("UTF8String");
	class NSString implements Foundation.NSString {
	    public final ID id;

	    NSString(ID id) {
		this.id = id;
		rt.gcrelease(this, id);
	    }

	    public ID id() {return(id);}

	    public String str() {
		return(rt.objc_msgSend_ptr(id, sel_UTF8String).reinterpret(Long.MAX_VALUE).getString(0, Utils.utf8));
	    }
	    public String toString() {return(str());}
	}

	private final SEL sel_initWithUTF8String = rt.sel_registerName("initWithUTF8String:");
	public NSString NSString(String str) {
	    try(Arena st = Arena.ofConfined()) {
		return(new NSString(rt.objc_msgSend_id(rt.objc_msgSend_id(NSString.id(), sel_alloc), sel_initWithUTF8String, st.allocateFrom(str, Utils.utf8))));
	    }
	}

	private final Runtime.Class NSProcessInfo = rt.objc_getClass("NSProcessInfo");
	private final SEL sel_operatingSystemVersionString = rt.sel_registerName("operatingSystemVersionString");
	class NSProcessInfo implements Foundation.NSProcessInfo {
	    public final ID id;

	    NSProcessInfo(ID id) {
		this.id = id;
	    }

	    public ID id() {return(id);}

	    public String operatingSystemVersionString() {
		return(new NSString(rt.objc_msgSend_id(id, sel_operatingSystemVersionString)).str());
	    }
	}

	private final SEL sel_processInfo = rt.sel_registerName("processInfo");
	public NSProcessInfo processInfo() {
	    return(new NSProcessInfo(rt.objc_msgSend_id(NSProcessInfo.id(), sel_processInfo)));
	}
    }

    private static Foundation instance = null;
    public static Foundation get() {
	if(instance == null) {
	    synchronized(Foundation.class) {
		if(instance == null) {
		    instance = new VersionC();
		}
	    }
	}
	return(instance);
    }
}
