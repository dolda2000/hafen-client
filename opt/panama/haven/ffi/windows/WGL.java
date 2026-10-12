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

import haven.*;
import haven.ffi.*;
import haven.ffi.gl.*;
import java.util.*;
import java.nio.*;
import java.lang.invoke.*;
import java.lang.foreign.*;
import static haven.ffi.ABI.*;
import static haven.ffi.FUtils.*;
import static java.lang.foreign.ValueLayout.ADDRESS;
import static haven.ffi.windows.Win32.*;

public abstract class WGL {
    public static final int WGL_CONTEXT_MAJOR_VERSION_ARB = 0x2091;
    public static final int WGL_CONTEXT_MINOR_VERSION_ARB = 0x2092;
    public static final int WGL_CONTEXT_FLAGS_ARB = 0x2094;
    public static final int WGL_CONTEXT_PROFILE_MASK_ARB = 0x9126;
    public static final int WGL_CONTEXT_DEBUG_BIT_ARB = 0x0001;
    public static final int WGL_CONTEXT_FORWARD_COMPATIBLE_BIT = 0x0002;
    public static final int WGL_CONTEXT_CORE_PROFILE_BIT_ARB = 0x0001;
    public static final int WGL_CONTEXT_COMPATIBILITY_PROFILE_BIT_ARB = 0x0002;

    public static final int ERROR_INVALID_VERSION_ARB = 0x2095;
    public static final int ERROR_INVALID_PROFILE_ARB = 0x2096;

    abstract MemorySegment wglGetProcAddress(String name);
    abstract MemorySegment proclookup(String name);
    public abstract Handle wglCreateContext(Handle hDC);
    public abstract void wglDeleteContext(Handle hGLRC);
    public abstract void wglMakeCurrent(Handle hDC, Handle hGLRC);
    public abstract Handle wglGetCurrentDC();

    public abstract Ext ext();

    static class Opengl32_dll extends WGL {
	private static final MemoryLayout BOOL = Win32.Win64Unicode.BOOL;
	private static final MemoryLayout HANDLE = Win32.Win64Unicode.HANDLE;
	private static final MemoryLayout HDC = Win32.Win64Unicode.HDC;
	private static final MemoryLayout LPCSTR = ADDRESS;
	static final MemoryLayout HGLRC = HANDLE;
	private final SymbolLookup opengl32 = loadlib("OPENGL32.DLL", Arena.global());
	private final Win32 win = Win32.get();

	private final MethodHandle wglGetProcAddress = ld.downcallHandle(opengl32.find("wglGetProcAddress").get(), FunctionDescriptor.of(ADDRESS, LPCSTR));
	MemorySegment wglGetProcAddress(String name) {
	    MemorySegment rv;
	    try(Arena st = Arena.ofConfined()) {
		rv = (MemorySegment)wglGetProcAddress.invoke(st.allocateFrom(name, Utils.ascii));
	    } catch(Throwable e) {
		throw(new InvocationException(e));
	    }
	    if(nullp(rv))
		return(null);
	    return(rv);
	}

	MemorySegment proclookup(String name) {
	    Optional<MemorySegment> builtin = opengl32.find(name);
	    if(builtin.isPresent())
		return(builtin.get());
	    return(wglGetProcAddress(name));
	}

	private MethodHandle lookup(String name, FunctionDescriptor sig) {
	    MemorySegment addr = wglGetProcAddress(name);
	    if(addr == null)
		return(null);
	    return(ld.downcallHandle(addr, sig));
	}

	private final MethodHandle wglCreateContext = ld.downcallHandle(opengl32.find("wglCreateContext").get(), FunctionDescriptor.of(HGLRC, HDC));
	public Handle wglCreateContext(Handle hDC) {
	    MemorySegment rv;
	    try {
		rv = (MemorySegment)wglCreateContext.invoke(hDC.bits);
	    } catch(Throwable e) {
		throw(new InvocationException(e));
	    }
	    if(nullp(rv))
		throw(win.lasterror());
	    return(Handle.of(rv));
	}

	private final MethodHandle wglDeleteContext = ld.downcallHandle(opengl32.find("wglDeleteContext").get(), FunctionDescriptor.of(BOOL, HGLRC));
	public void wglDeleteContext(Handle hGLRC) {
	    int rv;
	    try {
		rv = (int)wglDeleteContext.invoke(hGLRC.bits);
	    } catch(Throwable e) {
		throw(new InvocationException(e));
	    }
	    if(rv == 0)
		throw(win.lasterror());
	}

	private final MethodHandle wglMakeCurrent = ld.downcallHandle(opengl32.find("wglMakeCurrent").get(), FunctionDescriptor.of(BOOL, HDC, HGLRC));
	public void wglMakeCurrent(Handle hDC, Handle hGLRC) {
	    int rv;
	    try {
		rv = (int)wglMakeCurrent.invoke(hDC.bits, nhandle(hGLRC));
	    } catch(Throwable e) {
		throw(new InvocationException(e));
	    }
	    if(rv == 0)
		throw(win.lasterror());
	}

	private final MethodHandle wglGetCurrentDC = ld.downcallHandle(opengl32.find("wglGetCurrentDC").get(), FunctionDescriptor.of(HDC));
	public Handle wglGetCurrentDC() {
	    MemorySegment rv;
	    try {
		rv = (MemorySegment)wglGetCurrentDC.invoke();
	    } catch(Throwable e) {
		throw(new InvocationException(e));
	    }
	    if(nullp(rv))
		throw(null);
	    return(Handle.of(rv));
	}

	public class Ext extends WGL.Ext {
	    private final MethodHandle wglGetExtensionsStringARB = lookup("wglGetExtensionsStringARB", FunctionDescriptor.of(ADDRESS, HDC));
	    public String wglGetExtensionsStringARB(Handle hDC) {
		if(wglGetExtensionsStringARB == null)
		    throw(new MissingFunction("wglGetExtensionsStringARB"));
		MemorySegment rv;
		try {
		    rv = (MemorySegment)wglGetExtensionsStringARB.invoke(hDC.bits);
		} catch(Throwable e) {
		    throw(new InvocationException(e));
		}
		if(nullp(rv))
		    return(null);
		return(rv.reinterpret(Long.MAX_VALUE).getString(0, Utils.ascii));
	    }

	    private final MethodHandle wglGetExtensionsStringEXT = lookup("wglGetExtensionsStringEXT", FunctionDescriptor.of(ADDRESS));
	    public String wglGetExtensionsStringEXT() {
		if(wglGetExtensionsStringEXT == null)
		    throw(new MissingFunction("wglGetExtensionsStringEXT"));
		MemorySegment rv;
		try {
		    rv = (MemorySegment)wglGetExtensionsStringEXT.invoke();
		} catch(Throwable e) {
		    throw(new InvocationException(e));
		}
		if(nullp(rv))
		    return(null);
		return(rv.reinterpret(Long.MAX_VALUE).getString(0, Utils.ascii));
	    }

	    private final MethodHandle wglCreateContextAttribsARB = lookup("wglCreateContextAttribsARB", FunctionDescriptor.of(ADDRESS, HDC, HGLRC, ADDRESS));
	    public Handle wglCreateContextAttribsARB(Handle hDC, Handle hshareContext, int[] attribs) {
		if(wglCreateContextAttribsARB == null)
		    throw(new MissingFunction("wglCreateContextAttribsARB"));
		if(attribs.length % 2 != 0)
		    throw(new IllegalArgumentException());
		if(attribs[attribs.length - 2] != 0)
		    throw(new IllegalArgumentException());
		MemorySegment rv;
		try(Arena st = Arena.ofConfined()) {
		    MemorySegment acopy = st.allocate(C_INT, attribs.length);
		    for(int i = 0; i < attribs.length; i++)
			setint(acopy, i * C_INT.byteSize(), C_INT, attribs[i]);
		    try {
			rv = (MemorySegment)wglCreateContextAttribsARB.invoke(hDC.bits, nhandle(hshareContext), acopy);
		    } catch(Throwable e) {
			throw(new InvocationException(e));
		    }
		    if(nullp(rv))
			throw(win.lasterror());
		    return(Handle.of(rv));
		}
	    }

	    private final MethodHandle wglSwapIntervalEXT = lookup("wglSwapIntervalEXT", FunctionDescriptor.of(BOOL, C_INT));
	    public void wglSwapIntervalEXT(int interval) {
		if(wglSwapIntervalEXT == null)
		    throw(new MissingFunction("wglSwapIntervalEXT"));
		int rv;
		try {
		    rv = (int)wglSwapIntervalEXT.invoke(interval);
		} catch(Throwable e) {
		    throw(new InvocationException(e));
		}
		if(rv == 0)
		    throw(win.lasterror());
	    }
	}

	public Ext ext() {
	    return(new Ext());
	}
    }

    public static abstract class Ext {
	public abstract String wglGetExtensionsStringARB(Handle hDC);
	public abstract String wglGetExtensionsStringEXT();
	public abstract Handle wglCreateContextAttribsARB(Handle hDC, Handle hshareContext, int[] attribs);
	public abstract void wglSwapIntervalEXT(int interval);
    }

    class Resolved extends OpenGL.Base {
	protected MethodHandle lookup(String name, FunctionDescriptor sig, Linker.Option... options) {
	    MemorySegment addr = proclookup(name);
	    if(addr == null)
		return(null);
	    return(ld.downcallHandle(addr, sig, options));
	}
    }

    public OpenGL gl() {
	return(new Resolved());
    }

    private static WGL instance = null;
    public static WGL get() {
	if(instance == null) {
	    synchronized(WGL.class) {
		if(instance == null)
		    instance = tryload("WGL", Opengl32_dll::new);
	    }
	}
	return(instance);
    }
}
