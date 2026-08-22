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
import haven.ffi.gl.*;
import java.lang.invoke.*;
import java.lang.foreign.*;
import java.util.*;
import haven.ffi.objc.Runtime.*;
import java.lang.foreign.MemoryLayout.PathElement;
import haven.ffi.objc.Runtime.Class;
import static haven.ffi.ABI.*;
import static haven.ffi.FUtils.*;
import static java.lang.foreign.ValueLayout.ADDRESS;

public abstract class CGL {
    public static final int NSOpenGLPFADoubleBuffer  =  5;
    public static final int NSOpenGLPFAColorSize     =  8;
    public static final int NSOpenGLPFAAlphaSize     = 11;
    public static final int NSOpenGLPFADepthSize     = 12;
    public static final int NSOpenGLPFAOffscreen     = 53;
    public static final int NSOpenGLPFAFullscreen    = 54;
    public static final int NSOpenGLPFAAccelerated   = 73;
    public static final int NSOpenGLPFAWindow        = 80;
    public static final int NSOpenGLPFAOpenGLProfile = 99;

    public static final int NSOpenGLProfileVersionLegacy  = 0x1000;
    public static final int NSOpenGLProfileVersion3_2Core = 0x3200;
    public static final int NSOpenGLProfileVersion4_1Core = 0x4100;

    public static final int NSOpenGLCPSwapInterval = 222;

    public interface NSOpenGLPixelFormat {
	public ID id();
    }

    public interface NSOpenGLContext {
	public ID id();
	public void makeCurrentContext();
	public void clearCurrentContext();
	public void setView(AppKit.NSView view);
	public void update();
	public void clearDrawable();
	public void flushBuffer();
	public void setParameters(int[] vals, int param);
    }

    public abstract NSOpenGLPixelFormat NSOpenGLPixelFormat(int[] attribs);
    public abstract NSOpenGLContext NSOpenGLContext(NSOpenGLPixelFormat fmt, NSOpenGLContext share);
    public abstract OpenGL gl();

    static class VersionA extends CGL {
	private static final MemoryLayout NSOpenGLPixelFormatAttribute = ValueLayout.JAVA_INT;
	private final SymbolLookup dylib = SymbolLookup.libraryLookup("/System/Library/Frameworks/OpenGL.framework/OpenGL", Arena.global());
	private final Runtime rt = Runtime.get();
	private final SEL sel_alloc = rt.sel_registerName("alloc");

	private final Class cls_NSOpenGLPixelFormat = rt.objc_getClass("NSOpenGLPixelFormat");
	private final SEL sel_initWithAttributes = rt.sel_registerName("initWithAttributes:");
	class NSOpenGLPixelFormat implements CGL.NSOpenGLPixelFormat {
	    final ID id;

	    NSOpenGLPixelFormat(ID id) {
		this.id = id;
		rt.gcrelease(this, id);
	    }

	    public ID id() {return(id);}
	}

	public NSOpenGLPixelFormat NSOpenGLPixelFormat(int[] attribs) {
	    if(attribs[attribs.length - 1] != 0)
		throw(new IllegalArgumentException());
	    try(Arena st = Arena.ofConfined()) {
		ID id = rt.objc_msgSend_id(cls_NSOpenGLPixelFormat.id(), sel_alloc);
		MemorySegment acopy = memcpy(st.allocate(NSOpenGLPixelFormatAttribute, attribs.length), attribs);
		if((id = rt.objc_msgSend_id(id, sel_initWithAttributes, acopy)) == null)
		    return(null);
		return(new NSOpenGLPixelFormat(id));
	    }
	}

	private final Class cls_NSOpenGLContext = rt.objc_getClass("NSOpenGLContext");
	private final SEL sel_initWithFormat_shareContext = rt.sel_registerName("initWithFormat:shareContext:");
	private final SEL sel_makeCurrentContext = rt.sel_registerName("makeCurrentContext");
	private final SEL sel_clearCurrentContext = rt.sel_registerName("clearCurrentContext");
	private final SEL sel_setView = rt.sel_registerName("setView:");
	private final SEL sel_update = rt.sel_registerName("update");
	private final SEL sel_clearDrawable = rt.sel_registerName("clearDrawable");
	private final SEL sel_flushBuffer = rt.sel_registerName("flushBuffer");
	private final SEL sel_setValue_forParameter = rt.sel_registerName("setValues:forParameter:");
	class NSOpenGLContext implements CGL.NSOpenGLContext {
	    final ID id;

	    NSOpenGLContext(ID id) {
		this.id = id;
		rt.gcrelease(this, id);
	    }

	    public ID id() {return(id);}

	    public void makeCurrentContext() {
		rt.objc_msgSend_void(id, sel_makeCurrentContext);
	    }

	    public void clearCurrentContext() {
		rt.objc_msgSend_void(cls_NSOpenGLContext.id(), sel_clearCurrentContext);
	    }

	    public void setView(AppKit.NSView view) {
		rt.objc_msgSend_void(id, sel_setView, view.id());
	    }

	    public void update() {
		rt.objc_msgSend_void(id, sel_update);
	    }

	    public void clearDrawable() {
		rt.objc_msgSend_void(id, sel_clearDrawable);
	    }

	    public void flushBuffer() {
		rt.objc_msgSend_void(id, sel_flushBuffer);
	    }

	    public void setParameters(int[] vals, int param) {
		try(Arena st = Arena.ofConfined()) {
		    MemorySegment buf = memcpy(st.allocate(C_INT, vals.length), vals);
		    rt.objc_msgSend_void(id, sel_setValue_forParameter, buf, param);
		}
	    }
	}

	public NSOpenGLContext NSOpenGLContext(CGL.NSOpenGLPixelFormat fmt, CGL.NSOpenGLContext share) {
	    ID id = rt.objc_msgSend_id(cls_NSOpenGLContext.id(), sel_alloc);
	    if((id = rt.objc_msgSend_id(id, sel_initWithFormat_shareContext, fmt.id(), (share == null) ? null : share.id())) == null)
		return(null);
	    return(new NSOpenGLContext(id));
	}

	class Resolved extends OpenGL.Base {
	    protected MethodHandle lookup(String name, FunctionDescriptor sig, Linker.Option... options) {
		Optional<MemorySegment> addr = dylib.find(name);
		if(!addr.isPresent())
		    return(null);
		return(ld.downcallHandle(addr.get(), sig, options));
	    }
	}

	public OpenGL gl() {
	    return(new Resolved());
	}
    }

    private static CGL instance = null;
    public static CGL get() {
	if(instance == null) {
	    synchronized(CGL.class) {
		if(instance == null) {
		    instance = new VersionA();
		}
	    }
	}
	return(instance);
    }
}
