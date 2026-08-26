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

public abstract class Carbon {
    public static final int kVK_ANSI_A = 0x00;
    public static final int kVK_ANSI_S = 0x01;
    public static final int kVK_ANSI_D = 0x02;
    public static final int kVK_ANSI_F = 0x03;
    public static final int kVK_ANSI_H = 0x04;
    public static final int kVK_ANSI_G = 0x05;
    public static final int kVK_ANSI_Z = 0x06;
    public static final int kVK_ANSI_X = 0x07;
    public static final int kVK_ANSI_C = 0x08;
    public static final int kVK_ANSI_V = 0x09;
    public static final int kVK_ANSI_B = 0x0B;
    public static final int kVK_ANSI_Q = 0x0C;
    public static final int kVK_ANSI_W = 0x0D;
    public static final int kVK_ANSI_E = 0x0E;
    public static final int kVK_ANSI_R = 0x0F;
    public static final int kVK_ANSI_Y = 0x10;
    public static final int kVK_ANSI_T = 0x11;
    public static final int kVK_ANSI_1 = 0x12;
    public static final int kVK_ANSI_2 = 0x13;
    public static final int kVK_ANSI_3 = 0x14;
    public static final int kVK_ANSI_4 = 0x15;
    public static final int kVK_ANSI_6 = 0x16;
    public static final int kVK_ANSI_5 = 0x17;
    public static final int kVK_ANSI_Equal = 0x18;
    public static final int kVK_ANSI_9 = 0x19;
    public static final int kVK_ANSI_7 = 0x1A;
    public static final int kVK_ANSI_Minus = 0x1B;
    public static final int kVK_ANSI_8 = 0x1C;
    public static final int kVK_ANSI_0 = 0x1D;
    public static final int kVK_ANSI_RightBracket = 0x1E;
    public static final int kVK_ANSI_O = 0x1F;
    public static final int kVK_ANSI_U = 0x20;
    public static final int kVK_ANSI_LeftBracket = 0x21;
    public static final int kVK_ANSI_I = 0x22;
    public static final int kVK_ANSI_P = 0x23;
    public static final int kVK_ANSI_L = 0x25;
    public static final int kVK_ANSI_J = 0x26;
    public static final int kVK_ANSI_Quote = 0x27;
    public static final int kVK_ANSI_K = 0x28;
    public static final int kVK_ANSI_Semicolon = 0x29;
    public static final int kVK_ANSI_Backslash = 0x2A;
    public static final int kVK_ANSI_Comma = 0x2B;
    public static final int kVK_ANSI_Slash = 0x2C;
    public static final int kVK_ANSI_N = 0x2D;
    public static final int kVK_ANSI_M = 0x2E;
    public static final int kVK_ANSI_Period = 0x2F;
    public static final int kVK_ANSI_Grave = 0x32;
    public static final int kVK_ANSI_KeypadDecimal = 0x41;
    public static final int kVK_ANSI_KeypadMultiply = 0x43;
    public static final int kVK_ANSI_KeypadPlus = 0x45;
    public static final int kVK_ANSI_KeypadClear = 0x47;
    public static final int kVK_ANSI_KeypadDivide = 0x4B;
    public static final int kVK_ANSI_KeypadEnter = 0x4C;
    public static final int kVK_ANSI_KeypadMinus = 0x4E;
    public static final int kVK_ANSI_KeypadEquals = 0x51;
    public static final int kVK_ANSI_Keypad0 = 0x52;
    public static final int kVK_ANSI_Keypad1 = 0x53;
    public static final int kVK_ANSI_Keypad2 = 0x54;
    public static final int kVK_ANSI_Keypad3 = 0x55;
    public static final int kVK_ANSI_Keypad4 = 0x56;
    public static final int kVK_ANSI_Keypad5 = 0x57;
    public static final int kVK_ANSI_Keypad6 = 0x58;
    public static final int kVK_ANSI_Keypad7 = 0x59;
    public static final int kVK_ANSI_Keypad8 = 0x5B;
    public static final int kVK_ANSI_Keypad9 = 0x5C;

    public static final int kVK_Return = 0x24;
    public static final int kVK_Tab = 0x30;
    public static final int kVK_Space = 0x31;
    public static final int kVK_Delete = 0x33;
    public static final int kVK_Escape = 0x35;
    public static final int kVK_Command = 0x37;
    public static final int kVK_Shift = 0x38;
    public static final int kVK_CapsLock = 0x39;
    public static final int kVK_Option = 0x3A;
    public static final int kVK_Control = 0x3B;
    public static final int kVK_RightShift = 0x3C;
    public static final int kVK_RightOption = 0x3D;
    public static final int kVK_RightControl = 0x3E;
    public static final int kVK_Function = 0x3F;
    public static final int kVK_F17 = 0x40;
    public static final int kVK_VolumeUp = 0x48;
    public static final int kVK_VolumeDown = 0x49;
    public static final int kVK_Mute = 0x4A;
    public static final int kVK_F18 = 0x4F;
    public static final int kVK_F19 = 0x50;
    public static final int kVK_F20 = 0x5A;
    public static final int kVK_F5 = 0x60;
    public static final int kVK_F6 = 0x61;
    public static final int kVK_F7 = 0x62;
    public static final int kVK_F3 = 0x63;
    public static final int kVK_F8 = 0x64;
    public static final int kVK_F9 = 0x65;
    public static final int kVK_F11 = 0x67;
    public static final int kVK_F13 = 0x69;
    public static final int kVK_F16 = 0x6A;
    public static final int kVK_F14 = 0x6B;
    public static final int kVK_F10 = 0x6D;
    public static final int kVK_F12 = 0x6F;
    public static final int kVK_F15 = 0x71;
    public static final int kVK_Help = 0x72;
    public static final int kVK_Home = 0x73;
    public static final int kVK_PageUp = 0x74;
    public static final int kVK_ForwardDelete = 0x75;
    public static final int kVK_F4 = 0x76;
    public static final int kVK_End = 0x77;
    public static final int kVK_F2 = 0x78;
    public static final int kVK_PageDown = 0x79;
    public static final int kVK_F1 = 0x7A;
    public static final int kVK_LeftArrow = 0x7B;
    public static final int kVK_RightArrow = 0x7C;
    public static final int kVK_DownArrow = 0x7D;
    public static final int kVK_UpArrow = 0x7E;

    public static final int kVK_ISO_Section = 0x0A;

    public static final int kVK_JIS_Yen = 0x5D;
    public static final int kVK_JIS_Underscore = 0x5E;
    public static final int kVK_JIS_KeypadComma = 0x5F;
    public static final int kVK_JIS_Eisu = 0x66;
    public static final int kVK_JIS_Kana = 0x68;

    public static final int activeFlag = 1 <<  0;
    public static final int btnState   = 1 <<  7;
    public static final int cmdKey     = 1 <<  8;
    public static final int shiftKey   = 1 <<  9;
    public static final int alphaLock  = 1 << 10;
    public static final int optionKey  = 1 << 11;
    public static final int controlKey = 1 << 12;

    public static final int kUCKeyActionDown    = 0;
    public static final int kUCKeyActionUp      = 1;
    public static final int kUCKeyActionAuthKey = 2;
    public static final int kUCKeyActionDisplay = 3;
    public static final int kUCKeyTranslateNoDeadKeysMask = 1 << 0;

    public static final String kTISCategoryKeyboardInputSource = "TISCategoryKeyboardInputSource";
    public static final String kTISCategoryPaletteInputSource = "TISCategoryPaletteInputSource";
    public static final String kTISCategoryInkInputSource = "TISCategoryInkInputSource";
    public static final String kTISTypeKeyboardLayout = "TISTypeKeyboardLayout";
    public static final String kTISTypeKeyboardInputMethodWithoutModes = "TISTypeKeyboardInputMethodWithoutModes";
    public static final String kTISTypeKeyboardInputMethodModeEnabled = "TISTypeKeyboardInputMethodModeEnabled";
    public static final String kTISTypeKeyboardInputMode = "TISTypeKeyboardInputMode";
    public static final String kTISTypeKeyboardCharacterPalette = "TISTypeKeyboardCharacterPalette";
    public static final String kTISTypeKeyboardKeyboardViewer = "TISTypeKeyboardKeyboardViewer";
    public static final String kTISTypeKeyboardInk = "TISTypeKeyboardInk";

    public static interface UCKeyboardLayout {
    }

    public static interface TISInputSource {
	public String inputSourceCategory();
	public String inputSourceType();
	public String inputSourceID();
	public UCKeyboardLayout unicodeKeyLayoutData();
    }

    public abstract List<TISInputSource> TISCreateInputSourceList(boolean all);
    public abstract TISInputSource TISCopyCurrentKeyboardLayoutInputSource();
    public abstract TISInputSource TISCopyCurrentASCIICapableKeyboardLayoutInputSource();
    public abstract int LMGetKbdType();
    public abstract String UCKeyTranslate(Carbon.UCKeyboardLayout layout, int virtualKeyCode, int keyAction, int modifierKeyState, int keyboardType, int keyTranslateOptions);

    static class VersionA extends Carbon {
	private final SymbolLookup dylib = SymbolLookup.libraryLookup("/System/Library/Frameworks/Carbon.framework/Carbon", Arena.global());
	private final CoreFoundation cf = CoreFoundation.get();
	private static final Charset C_UNICHARSET = Charset.forName(ByteOrder.nativeOrder() == ByteOrder.LITTLE_ENDIAN ? "UTF-16LE" : "UTF-16BE");
	private static final MemoryLayout CFTypeRef = CoreFoundation.VersionA.CFTypeRef;
	private static final MemoryLayout UniChar = ValueLayout.JAVA_CHAR;
	private static final MemoryLayout UniCharCount = C_LONG;
	private static final MemoryLayout OSStatus = ValueLayout.JAVA_INT;
	private static final MemoryLayout UInt16 = ValueLayout.JAVA_SHORT;
	private static final MemoryLayout UInt32 = ValueLayout.JAVA_INT;
	private static final MemoryLayout OC_BOOL = Runtime.objc4.OC_BOOL;
	private static final Runtime rt = Runtime.get();
	private static final Foundation fnd = Foundation.get();

	class UCKeyboardLayout implements Carbon.UCKeyboardLayout {
	    final MemorySegment mem;
	    final Object keep;

	    UCKeyboardLayout(MemorySegment mem, Object keep) {
		this.mem = mem;
		this.keep = keep;
	    }
	}

	private final MemorySegment kTISPropertyInputSourceCategory = dylib.find("kTISPropertyInputSourceCategory").get().reinterpret(ADDRESS.byteSize()).get(ADDRESS, 0);
	private final MemorySegment kTISPropertyInputSourceType = dylib.find("kTISPropertyInputSourceType").get().reinterpret(ADDRESS.byteSize()).get(ADDRESS, 0);
	private final MemorySegment kTISPropertyInputSourceID = dylib.find("kTISPropertyInputSourceID").get().reinterpret(ADDRESS.byteSize()).get(ADDRESS, 0);
	private final MemorySegment kTISPropertyUnicodeKeyLayoutData = dylib.find("kTISPropertyUnicodeKeyLayoutData").get().reinterpret(ADDRESS.byteSize()).get(ADDRESS, 0);
	private final MethodHandle TISGetInputSourceProperty = ld.downcallHandle(dylib.find("TISGetInputSourceProperty").get(), FunctionDescriptor.of(ADDRESS, ADDRESS, ADDRESS));
	class TISInputSource implements Carbon.TISInputSource {
	    final MemorySegment ref;

	    TISInputSource(MemorySegment ref, boolean release) {
		this.ref = ref;
		if(release)
		    cf.gcrelease(this, ref);
	    }

	    MemorySegment property(MemorySegment key) {
		try {
		    return((MemorySegment)TISGetInputSourceProperty.invoke(ref, key));
		} catch(Throwable e) {throw(new RuntimeException(e));}
	    }

	    public String inputSourceCategory() {
		return(fnd.fromNSString(rt.id(property(kTISPropertyInputSourceCategory))).intern());
	    }

	    public String inputSourceType() {
		return(fnd.fromNSString(rt.id(property(kTISPropertyInputSourceType))).intern());
	    }

	    public String inputSourceID() {
		return(fnd.fromNSString(rt.id(property(kTISPropertyInputSourceID))).intern());
	    }

	    public UCKeyboardLayout unicodeKeyLayoutData() {
		return(new UCKeyboardLayout(cf.CFData(property(kTISPropertyUnicodeKeyLayoutData), false, this).getBytePtr(), this));
	    }
	}

	private final MethodHandle TISCreateInputSourceList = ld.downcallHandle(dylib.find("TISCreateInputSourceList").get(), FunctionDescriptor.of(ADDRESS, ADDRESS, OC_BOOL));
	public List<Carbon.TISInputSource> TISCreateInputSourceList(boolean all) {
	    MemorySegment rv;
	    try {
		rv = (MemorySegment)TISCreateInputSourceList.invoke(MemorySegment.NULL, all ? (byte)1 : (byte)0);
	    } catch(Throwable e) {throw(new RuntimeException(e));}
	    NSArray list = fnd.NSArray(rt.id(rv));
	    ArrayList<Carbon.TISInputSource> ret = new ArrayList<>();
	    for(int i = 0; i < list.size(); i++)
		ret.add(new TISInputSource(cf.CFRetain(list.get(i).mem()), true));
	    return(ret);
	}

	private final MethodHandle TISCopyCurrentKeyboardLayoutInputSource = ld.downcallHandle(dylib.find("TISCopyCurrentKeyboardLayoutInputSource").get(), FunctionDescriptor.of(ADDRESS));
	public TISInputSource TISCopyCurrentKeyboardLayoutInputSource() {
	    MemorySegment rv;
	    try {
		rv = (MemorySegment)TISCopyCurrentKeyboardLayoutInputSource.invoke();
	    } catch(Throwable e) {throw(new RuntimeException(e));}
	    return(new TISInputSource(rv, true));
	}

	private final MethodHandle TISCopyCurrentASCIICapableKeyboardLayoutInputSource = ld.downcallHandle(dylib.find("TISCopyCurrentASCIICapableKeyboardLayoutInputSource").get(), FunctionDescriptor.of(ADDRESS));
	public TISInputSource TISCopyCurrentASCIICapableKeyboardLayoutInputSource() {
	    MemorySegment rv;
	    try {
		rv = (MemorySegment)TISCopyCurrentASCIICapableKeyboardLayoutInputSource.invoke();
	    } catch(Throwable e) {throw(new RuntimeException(e));}
	    return(new TISInputSource(rv, true));
	}

	private final MethodHandle LMGetKbdType = ld.downcallHandle(dylib.find("LMGetKbdType").get(), FunctionDescriptor.of(ValueLayout.JAVA_BYTE));
	public int LMGetKbdType() {
	    try {
		return(0xff & (byte)LMGetKbdType.invoke());
	    } catch(Throwable e) {throw(new RuntimeException(e));}
	}

	private final MethodHandle UCKeyTranslate = ld.downcallHandle(dylib.find("UCKeyTranslate").get(), FunctionDescriptor.of(OSStatus, ADDRESS, UInt16, UInt16, UInt32, UInt32, UInt32, ADDRESS, UniCharCount, ADDRESS, ADDRESS));
	public String UCKeyTranslate(Carbon.UCKeyboardLayout layout, int virtualKeyCode, int keyAction, int modifierKeyState, int keyboardType, int keyTranslateOptions) {
	    int max = 256;
	    try(Arena st = Arena.ofConfined()) {
		MemorySegment buf = st.allocate(UniChar, max), lbuf = st.allocate(UniCharCount), stbuf = st.allocate(UInt32);
		int rv;
		try {
		    rv = (int)UCKeyTranslate.invoke(((UCKeyboardLayout)layout).mem, (short)virtualKeyCode, (short)keyAction, modifierKeyState, keyboardType, keyTranslateOptions, stbuf, max, lbuf, buf);
		} catch(Throwable e) {throw(new RuntimeException(e));}
		if(rv != 0)
		    return(null);
		return(nstring(buf, 0, (int)(UniChar.byteSize() * getint(lbuf, 0, UniCharCount, false)), C_UNICHARSET));
	    }
	}
    }

    private static Carbon instance = null;
    public static Carbon get() {
	if(instance == null) {
	    synchronized(Carbon.class) {
		if(instance == null) {
		    instance = new VersionA();
		}
	    }
	}
	return(instance);
    }
}
