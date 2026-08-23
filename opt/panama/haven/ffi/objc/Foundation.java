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
    public static interface NSString extends Runtime.NSObject {
	public ID id();
	public String str();
    }

    public abstract NSString NSString(ID id);
    public abstract NSString NSString(String str);
    public abstract String fromNSString(ID id);

    public static interface NSArray extends Runtime.NSObject, Iterable<ID> {
	public ID id();
	public int size();
	public ID get(int idx);

	public default Iterator<ID> iterator() {
	    return(new Iterator<ID>() {
		private int i = 0;
		public boolean hasNext() {return(i < size());}
		public ID next() {return(get(i++));}
	    });
	}
    }

    abstract NSArray NSArray(ID id);
    public abstract NSArray NSArray(NSObject... objects);

    public static interface NSDictionary extends Runtime.NSObject {
	public ID id();
	public ID valueForKey(NSString key);
	public ID valueForKey(String key);
	public ID objectForKey(NSObject key);
    }

    abstract NSDictionary NSDictionary(ID id);

    public static interface NSData extends Runtime.NSObject {
	public ID id();
	public byte[] data();
    }

    abstract NSData NSData(ID id, boolean retain);

    public static interface NSValue extends Runtime.NSObject {
	public ID id();
	public CoreGraphics.CGPoint pointValue();
	public CoreGraphics.CGSize sizeValue();
	public CoreGraphics.CGRect rectValue();
    }

    abstract NSValue NSValue(ID id);

    public static interface NSNumber extends Runtime.NSObject {
	public ID id();
	public int intValue();
    }

    abstract NSNumber NSNumber(ID id);

    public static interface NSProcessInfo {
	public String operatingSystemVersionString();
    }

    public abstract NSProcessInfo processInfo();

    static class VersionC extends Foundation {
	private final SymbolLookup dylib = SymbolLookup.libraryLookup("/System/Library/Frameworks/Foundation.framework/Foundation", Arena.global());
	private final Runtime rt = Runtime.get();
	private final SEL sel_alloc = rt.sel_registerName("alloc");
	private final SEL sel_retain = rt.sel_registerName("retain");

	private final Runtime.Class cls_NSString = rt.objc_getClass("NSString");
	private final SEL sel_UTF8String = rt.sel_registerName("UTF8String");
	class NSString implements Foundation.NSString {
	    public final ID id;

	    NSString(ID id) {
		this.id = id;
	    }

	    static NSString unretained(VersionC fnd, ID id) {
		return(fnd.new NSString(id));
	    }
	    static NSString retained(VersionC fnd, ID id) {
		NSString ret = unretained(fnd, id);
		fnd.rt.gcrelease(ret, id);
		return(ret);
	    }
	    static NSString retain(VersionC fnd, ID id) {
		return(retained(fnd, fnd.rt.objc_msgSend_id(id, fnd.sel_retain)));
	    }

	    public ID id() {return(id);}

	    public String str() {
		return(rt.objc_msgSend_ptr(id, sel_UTF8String).reinterpret(Long.MAX_VALUE).getString(0, Utils.utf8));
	    }
	    public String toString() {return(str());}
	}

	public NSString NSString(ID id) {
	    if(id == null)
		return(null);
	    return(NSString.retain(this, id));
	}

	public String fromNSString(ID id) {
	    if(id == null)
		return(null);
	    return(NSString.unretained(this, id).str());
	}

	private final SEL sel_initWithUTF8String = rt.sel_registerName("initWithUTF8String:");
	public NSString NSString(String str) {
	    try(Arena st = Arena.ofConfined()) {
		return(NSString.retained(this, rt.objc_msgSend_id(rt.objc_msgSend_id(cls_NSString.id(), sel_alloc), sel_initWithUTF8String, st.allocateFrom(str, Utils.utf8))));
	    }
	}

	private final Runtime.Class cls_NSArray = rt.objc_getClass("NSArray");
	private final SEL sel_count = rt.sel_registerName("count");
	private final SEL sel_objectAtIndex = rt.sel_registerName("objectAtIndex:");
	class NSArray implements Foundation.NSArray {
	    public final ID id;

	    NSArray(ID id) {
		this.id = id;
	    }

	    static NSArray unretained(VersionC fnd, ID id) {
		return(fnd.new NSArray(id));
	    }
	    static NSArray retained(VersionC fnd, ID id) {
		NSArray ret = unretained(fnd, id);
		fnd.rt.gcrelease(ret, id);
		return(ret);
	    }
	    static NSArray retain(VersionC fnd, ID id) {
		return(retained(fnd, fnd.rt.objc_msgSend_id(id, fnd.sel_retain)));
	    }

	    public ID id() {return(id);}

	    public int size() {return(rt.objc_msgSend_NSUInt(id, sel_count));}
	    public ID get(int idx) {return(rt.objc_msgSend_id(id, sel_objectAtIndex, idx));}
	}

	NSArray NSArray(ID id) {
	    return(NSArray.retained(this, id));
	}

	private final SEL sel_initWithObjects_count = rt.sel_registerName("initWithObjects:count:");
	public NSArray NSArray(NSObject... objects) {
	    try(Arena st = Arena.ofConfined()) {
		MemorySegment buf = st.allocate(ADDRESS, objects.length);
		for(int i = 0; i < objects.length; i++)
		    buf.setAtIndex(ADDRESS, i, objects[i].id().mem());
		return(NSArray.retained(this, rt.objc_msgSend_id(rt.objc_msgSend_id(cls_NSArray.id(), sel_alloc), sel_initWithObjects_count, buf, objects.length)));
	    }
	}

	private final SEL sel_valueForKey = rt.sel_registerName("valueForKey:");
	private final SEL sel_objectForKey = rt.sel_registerName("objectForKey:");
	class NSDictionary implements Foundation.NSDictionary {
	    public final ID id;

	    NSDictionary(ID id) {
		this.id = id;
	    }

	    static NSDictionary unretained(VersionC fnd, ID id) {
		return(fnd.new NSDictionary(id));
	    }
	    static NSDictionary retained(VersionC fnd, ID id) {
		NSDictionary ret = unretained(fnd, id);
		fnd.rt.gcrelease(ret, id);
		return(ret);
	    }
	    static NSDictionary retain(VersionC fnd, ID id) {
		return(retained(fnd, fnd.rt.objc_msgSend_id(id, fnd.sel_retain)));
	    }

	    public ID id() {return(id);}

	    public ID valueForKey(Foundation.NSString key) {
		return(rt.objc_msgSend_id(id, sel_valueForKey, key.id()));
	    }
	    public ID valueForKey(String key) {
		return(valueForKey(NSString(key)));
	    }

	    public ID objectForKey(NSObject key) {
		return(rt.objc_msgSend_id(id, sel_objectForKey, key.id()));
	    }
	}

	NSDictionary NSDictionary(ID id) {
	    return(NSDictionary.retained(this, id));
	}

	private final SEL sel_bytes = rt.sel_registerName("bytes");
	private final SEL sel_length = rt.sel_registerName("length");
	class NSData implements Foundation.NSData {
	    public final ID id;

	    NSData(ID id) {
		this.id = id;
	    }

	    static NSData unretained(VersionC fnd, ID id) {
		return(fnd.new NSData(id));
	    }
	    static NSData retained(VersionC fnd, ID id) {
		NSData ret = unretained(fnd, id);
		fnd.rt.gcrelease(ret, id);
		return(ret);
	    }
	    static NSData retain(VersionC fnd, ID id) {
		return(retained(fnd, fnd.rt.objc_msgSend_id(id, fnd.sel_retain)));
	    }

	    public ID id() {return(id);}

	    public byte[] data() {
		return(memcpy(rt.objc_msgSend_ptr(id, sel_bytes), 0, rt.objc_msgSend_NSUInt(id, sel_length)));
	    }
	}

	NSData NSData(ID id, boolean retain) {
	    if(id == null)
		return(null);
	    NSData rv = NSData.retained(this, id);
	    if(retain) rt.retain(rv);
	    return(rv);
	}

	private final SEL sel_pointValue = rt.sel_registerName("pointValue");
	private final SEL sel_sizeValue = rt.sel_registerName("sizeValue");
	private final SEL sel_rectValue = rt.sel_registerName("rectValue");
	class NSValue implements Foundation.NSValue {
	    public final ID id;

	    NSValue(ID id) {
		this.id = id;
	    }

	    static NSValue unretained(VersionC fnd, ID id) {
		return(fnd.new NSValue(id));
	    }
	    static NSValue retained(VersionC fnd, ID id) {
		NSValue ret = unretained(fnd, id);
		fnd.rt.gcrelease(ret, id);
		return(ret);
	    }
	    static NSValue retain(VersionC fnd, ID id) {
		return(retained(fnd, fnd.rt.objc_msgSend_id(id, fnd.sel_retain)));
	    }

	    public ID id() {return(id);}

	    public CoreGraphics.CGPoint pointValue() {
		return(CoreGraphics.get().objc_msgSend_CGPoint(id, sel_pointValue));
	    }
	    public CoreGraphics.CGSize sizeValue() {
		return(CoreGraphics.get().objc_msgSend_CGSize(id, sel_sizeValue));
	    }
	    public CoreGraphics.CGRect rectValue() {
		return(CoreGraphics.get().objc_msgSend_CGRect(id, sel_rectValue));
	    }
	}

	NSValue NSValue(ID id) {
	    return(NSValue.retained(this, id));
	}

	private final SEL sel_intValue = rt.sel_registerName("intValue");
	class NSNumber implements Foundation.NSNumber {
	    public final ID id;

	    NSNumber(ID id) {
		this.id = id;
	    }

	    static NSNumber unretained(VersionC fnd, ID id) {
		return(fnd.new NSNumber(id));
	    }
	    static NSNumber retained(VersionC fnd, ID id) {
		NSNumber ret = unretained(fnd, id);
		fnd.rt.gcrelease(ret, id);
		return(ret);
	    }
	    static NSNumber retain(VersionC fnd, ID id) {
		return(retained(fnd, fnd.rt.objc_msgSend_id(id, fnd.sel_retain)));
	    }

	    public ID id() {return(id);}

	    public int intValue() {
		return(rt.objc_msgSend_int(id, sel_intValue));
	    }
	}

	NSNumber NSNumber(ID id) {
	    return(NSNumber.retained(this, id));
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
		return(NSString.retained(VersionC.this, rt.objc_msgSend_id(id, sel_operatingSystemVersionString)).str());
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
