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
import java.lang.invoke.*;
import java.lang.foreign.*;
import java.util.function.*;
import haven.ffi.objc.Runtime.*;
import haven.ffi.objc.Foundation.*;
import static haven.ffi.FUtils.*;

public abstract class Forwarder implements NSObject {
    private static final Runtime rt = Runtime.get();
    private static final Foundation fnd = Foundation.get();
    private static final SEL sel_alloc =rt.sel_registerName("alloc");
    private static final SEL sel_init = rt.sel_registerName("init");
    private static final Arena localarena = Arena.ofAuto();
    public final ID id;

    private static final SEL sel_forwardInvocation = rt.sel_registerName("forwardInvocation:");
    private static final Runtime.Class cls_NSObject = rt.objc_getClass("NSObject");
    private static final Runtime.Class cls_Forwarder;
    static {
	cls_Forwarder = rt.objc_allocateClassPair(cls_NSObject, "IOSYSForwarder", 0);
	rt.class_addIvar(cls_Forwarder, "java", 4, 2, "i");
	rt.class_addMethod(cls_Forwarder, rt.sel_registerName("respondsToSelector:"),
			   supcall(localarena, MethodHandles.lookup(), Forwarder.class, "respondsToSelector", null,
				   rt.OC_BOOL(), rt.C_ID(), rt.C_SEL(), rt.C_SEL()), "B@::");
	rt.class_addMethod(cls_Forwarder, rt.sel_registerName("methodSignatureForSelector:"),
			   supcall(localarena, MethodHandles.lookup(), Forwarder.class, "methodSignatureForSelector", null,
				   rt.C_ID(), rt.C_ID(), rt.C_SEL(), rt.C_SEL()), "@@::");
	rt.class_addMethod(cls_Forwarder, sel_forwardInvocation,
			   supcall(localarena, MethodHandles.lookup(), Forwarder.class, "forwardInvocation", null,
				   null, rt.C_ID(), rt.C_SEL(), rt.C_ID()), "v@:@");
	rt.objc_registerClassPair(cls_Forwarder);
    }

    private static final ObjectTracker<Forwarder> cur_Forwarder = new ObjectTracker<>(cls_Forwarder);
    public Forwarder() {
	this.id = rt.objc_msgSend_id(rt.objc_msgSend_id(cls_Forwarder.id(), sel_alloc), sel_init);
	cur_Forwarder.reg(this);
    }

    public ID id() {return(id);}

    public abstract boolean supports(SEL sel);
    public abstract NSMethodSignature signature(SEL sel);
    public abstract boolean forward(NSInvocation inv);

    private static byte respondsToSelector(MemorySegment objp, MemorySegment sel, MemorySegment arg) {
	return(cur_Forwarder.wrap(objp, obj -> {
	    return((byte)(obj.supports(rt.sel(arg)) ? 1 : 0));
	}, (byte)0));
    }

    private static MemorySegment methodSignatureForSelector(MemorySegment objp, MemorySegment sel, MemorySegment arg) {
	return(cur_Forwarder.wrap(objp, obj -> {
	    return(obj.signature(rt.sel(arg)).id().mem());
	}, MemorySegment.NULL));
    }

    private static void forwardInvocation(MemorySegment objp, MemorySegment sel, MemorySegment invp) {
	cur_Forwarder.wrap(objp, obj -> {
	    ID inv = rt.id(invp);
	    if(!obj.forward(fnd.NSInvocation(inv, false, false)))
		rt.objc_msgSendSuper_void(obj.id(), cls_NSObject, sel_forwardInvocation, inv);
	});
    }
}
