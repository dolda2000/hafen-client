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

package haven.ffi.x11;

import haven.*;
import haven.ffi.*;
import java.lang.invoke.*;
import java.lang.foreign.*;
import java.nio.*;
import java.util.*;
import java.util.function.*;
import java.lang.foreign.MemoryLayout.PathElement;
import static haven.ffi.ABI.*;
import static haven.ffi.FUtils.*;
import static java.lang.foreign.ValueLayout.ADDRESS;

public abstract class XLib {
    public static final int None = 0;
    public static final int Success = 0;
    public static final int CurrentTime = 0;

    public static final long NoEventMask = 0L;
    public static final long KeyPressMask = (1L<<0);
    public static final long KeyReleaseMask = (1L<<1);
    public static final long ButtonPressMask = (1L<<2);
    public static final long ButtonReleaseMask = (1L<<3);
    public static final long EnterWindowMask = (1L<<4);
    public static final long LeaveWindowMask = (1L<<5);
    public static final long PointerMotionMask = (1L<<6);
    public static final long PointerMotionHintMask = (1L<<7);
    public static final long Button1MotionMask = (1L<<8);
    public static final long Button2MotionMask = (1L<<9);
    public static final long Button3MotionMask = (1L<<10);
    public static final long Button4MotionMask = (1L<<11);
    public static final long Button5MotionMask = (1L<<12);
    public static final long ButtonMotionMask = (1L<<13);
    public static final long KeymapStateMask = (1L<<14);
    public static final long ExposureMask = (1L<<15);
    public static final long VisibilityChangeMask = (1L<<16);
    public static final long StructureNotifyMask = (1L<<17);
    public static final long ResizeRedirectMask = (1L<<18);
    public static final long SubstructureNotifyMask = (1L<<19);
    public static final long SubstructureRedirectMask = (1L<<20);
    public static final long FocusChangeMask = (1L<<21);
    public static final long PropertyChangeMask = (1L<<22);
    public static final long ColormapChangeMask = (1L<<23);
    public static final long OwnerGrabButtonMask = (1L<<24);

    public static final int KeyPress = 2;
    public static final int KeyRelease = 3;
    public static final int ButtonPress = 4;
    public static final int ButtonRelease = 5;
    public static final int MotionNotify = 6;
    public static final int EnterNotify = 7;
    public static final int LeaveNotify = 8;
    public static final int FocusIn = 9;
    public static final int FocusOut = 10;
    public static final int KeymapNotify = 11;
    public static final int Expose = 12;
    public static final int GraphicsExpose = 13;
    public static final int NoExpose = 14;
    public static final int VisibilityNotify = 15;
    public static final int CreateNotify = 16;
    public static final int DestroyNotify = 17;
    public static final int UnmapNotify = 18;
    public static final int MapNotify = 19;
    public static final int MapRequest = 20;
    public static final int ReparentNotify = 21;
    public static final int ConfigureNotify = 22;
    public static final int ConfigureRequest = 23;
    public static final int GravityNotify = 24;
    public static final int ResizeRequest = 25;
    public static final int CirculateNotify = 26;
    public static final int CirculateRequest = 27;
    public static final int PropertyNotify = 28;
    public static final int SelectionClear = 29;
    public static final int SelectionRequest = 30;
    public static final int SelectionNotify = 31;
    public static final int ColormapNotify = 32;
    public static final int ClientMessage = 33;
    public static final int MappingNotify = 34;
    public static final int GenericEvent = 35;

    public static final int ShiftMask = (1<<0);
    public static final int LockMask = (1<<1);
    public static final int ControlMask = (1<<2);
    public static final int Mod1Mask = (1<<3);
    public static final int Mod2Mask = (1<<4);
    public static final int Mod3Mask = (1<<5);
    public static final int Mod4Mask = (1<<6);
    public static final int Mod5Mask = (1<<7);
    public static final int Button1Mask = (1<<8);
    public static final int Button2Mask = (1<<9);
    public static final int Button3Mask = (1<<10);
    public static final int Button4Mask = (1<<11);
    public static final int Button5Mask = (1<<12);

    public static final int InputOutput = 1;
    public static final int InputOnly = 2;

    public static final long CWBackPixmap = (1L<<0);
    public static final long CWBackPixel = (1L<<1);
    public static final long CWBorderPixmap = (1L<<2);
    public static final long CWBorderPixel = (1L<<3);
    public static final long CWBitGravity = (1L<<4);
    public static final long CWWinGravity = (1L<<5);
    public static final long CWBackingStore = (1L<<6);
    public static final long CWBackingPlanes = (1L<<7);
    public static final long CWBackingPixel = (1L<<8);
    public static final long CWOverrideRedirect = (1L<<9);
    public static final long CWSaveUnder = (1L<<10);
    public static final long CWEventMask = (1L<<11);
    public static final long CWDontPropagate = (1L<<12);
    public static final long CWColormap = (1L<<13);
    public static final long CWCursor = (1L<<14);

    public static final int CWX = (1<<0);
    public static final int CWY = (1<<1);
    public static final int CWWidth = (1<<2);
    public static final int CWHeight = (1<<3);
    public static final int CWBorderWidth = (1<<4);
    public static final int CWSibling = (1<<5);
    public static final int CWStackMode = (1<<6);

    public static final int AllocNone = 0;
    public static final int AllocAll = 1;

    public static final int VisibilityUnobscured = 0;
    public static final int VisibilityPartiallyObscured = 1;
    public static final int VisibilityFullyObscured = 2;

    public static final int PropertyNewValue = 0;
    public static final int PropertyDelete = 1;
    public static final int PropModeReplace = 0;
    public static final int PropModePrepend = 1;
    public static final int PropModeAppend = 2;

    public static final String XNVaNestedList = "XNVaNestedList";
    public static final String XNQueryInputStyle = "queryInputStyle";
    public static final String XNClientWindow = "clientWindow";
    public static final String XNInputStyle = "inputStyle";
    public static final String XNFocusWindow = "focusWindow";
    public static final String XNResourceName = "resourceName";
    public static final String XNResourceClass = "resourceClass";
    public static final String XNGeometryCallback = "geometryCallback";
    public static final String XNDestroyCallback = "destroyCallback";
    public static final String XNFilterEvents = "filterEvents";
    public static final String XNPreeditStartCallback = "preeditStartCallback";
    public static final String XNPreeditDoneCallback = "preeditDoneCallback";
    public static final String XNPreeditDrawCallback = "preeditDrawCallback";
    public static final String XNPreeditCaretCallback = "preeditCaretCallback";
    public static final String XNPreeditStateNotifyCallback = "preeditStateNotifyCallback";
    public static final String XNPreeditAttributes = "preeditAttributes";
    public static final String XNStatusStartCallback = "statusStartCallback";
    public static final String XNStatusDoneCallback = "statusDoneCallback";
    public static final String XNStatusDrawCallback = "statusDrawCallback";
    public static final String XNStatusAttributes = "statusAttributes";
    public static final String XNArea = "area";
    public static final String XNAreaNeeded = "areaNeeded";
    public static final String XNSpotLocation = "spotLocation";
    public static final String XNColormap = "colorMap";
    public static final String XNStdColormap = "stdColorMap";
    public static final String XNForeground = "foreground";
    public static final String XNBackground = "background";
    public static final String XNBackgroundPixmap = "backgroundPixmap";
    public static final String XNFontSet = "fontSet";
    public static final String XNLineSpace = "lineSpace";
    public static final String XNCursor = "cursor";

    public static final String XNQueryIMValuesList = "queryIMValuesList";
    public static final String XNQueryICValuesList = "queryICValuesList";
    public static final String XNVisiblePosition = "visiblePosition";
    public static final String XNR6PreeditCallback = "r6PreeditCallback";
    public static final String XNStringConversionCallback = "stringConversionCallback";
    public static final String XNStringConversion = "stringConversion";
    public static final String XNResetState = "resetState";
    public static final String XNHotKey = "hotKey";
    public static final String XNHotKeyState = "hotKeyState";
    public static final String XNPreeditState = "preeditState";
    public static final String XNSeparatorofNestedList = "separatorofNestedList";

    public static final long XIMPreeditArea = 0x0001L;
    public static final long XIMPreeditCallbacks = 0x0002L;
    public static final long XIMPreeditPosition = 0x0004L;
    public static final long XIMPreeditNothing = 0x0008L;
    public static final long XIMPreeditNone = 0x0010L;
    public static final long XIMStatusArea = 0x0100L;
    public static final long XIMStatusCallbacks = 0x0200L;
    public static final long XIMStatusNothing = 0x0400L;
    public static final long XIMStatusNone = 0x0800L;
    public static final int XBufferOverflow = -1;
    public static final int XLookupNone = 1;
    public static final int XLookupChars = 2;
    public static final int XLookupKeySym = 3;
    public static final int XLookupBoth = 4;

    public static final int XkbMajorVersion = 1;
    public static final int XkbMinorVersion = 0;
    public static final int XkbNumKbdGroups = 4;
    public static final int XkbNumVirtualMods = 16;
    public static final int XkbNumIndicators = 32;
    public static final int XkbKeyNameLength = 4;
    public static final int XkbUseCoreKbd = 0x0100;
    public static final int XkbClientMapMask     = 1 << 0;
    public static final int XkbServerMapMask     = 1 << 1;
    public static final int XkbCompatMapMask     = 1 << 2;
    public static final int XkbIndicatorMapMask  = 1 << 3;
    public static final int XkbNamesMask         = 1 << 4;
    public static final int XkbGeometryMask      = 1 << 5;
    public static final int XkbControlsMask      = 1 << 6;
    public static final int XkbKeyTypesMask           = (1<<0);
    public static final int XkbKeySymsMask            = (1<<1);
    public static final int XkbModifierMapMask        = (1<<2);
    public static final int XkbExplicitComponentsMask = (1<<3);
    public static final int XkbKeyActionsMask         = (1<<4);
    public static final int XkbKeyBehaviorsMask       = (1<<5);
    public static final int XkbVirtualModsMask        = (1<<6);
    public static final int XkbVirtualModMapMask      = (1<<7);
    public static final int XkbKeycodesNameMask    = (1<<0);
    public static final int XkbGeometryNameMask    = (1<<1);
    public static final int XkbSymbolsNameMask     = (1<<2);
    public static final int XkbPhysSymbolsNameMask = (1<<3);
    public static final int XkbTypesNameMask       = (1<<4);
    public static final int XkbCompatNameMask      = (1<<5);
    public static final int XkbKeyTypeNamesMask    = (1<<6);
    public static final int XkbKTLevelNamesMask    = (1<<7);
    public static final int XkbIndicatorNamesMask  = (1<<8);
    public static final int XkbKeyNamesMask        = (1<<9);
    public static final int XkbKeyAliasesMask      = (1<<10);
    public static final int XkbVirtualModNamesMask = (1<<11);
    public static final int XkbGroupNamesMask      = (1<<12);
    public static final int XkbRGNamesMask         = (1<<13);
    public static final int XkbWrapIntoRange     = 0x00;
    public static final int XkbClampIntoRange    = 0x40;
    public static final int XkbRedirectIntoRange = 0x80;

    public static final long USPosition = (1L << 0);
    public static final long USSize = (1L << 1);
    public static final long PPosition = (1L << 2);
    public static final long PSize = (1L << 3);
    public static final long PMinSize = (1L << 4);
    public static final long PMaxSize = (1L << 5);
    public static final long PResizeInc = (1L << 6);
    public static final long PAspect = (1L << 7);
    public static final long PBaseSize = (1L << 8);
    public static final long PWinGravity = (1L << 9);

    public static class XID {
	public static final XID None = new XID(0);
	final long bits;

	private XID(long bits) {this.bits = bits;}
	static XID of(long bits) {return((bits == 0) ? None : new XID(bits));}

	public int hashCode() {return(Long.hashCode(bits));}
	public boolean equals(Object x) {return((x instanceof XID) && (((XID)x).bits == this.bits));}
	public String toString() {
	    return("#" + Long.toUnsignedString(bits, 16));
	}

	public long bits() {
	    return(bits);
	}
    }

    public static class Atom {
	public static final Atom nil = new Atom(0);
	final long bits;

	private Atom(long bits) {this.bits = bits;}
	static Atom of(long bits) {return((bits == 0) ? nil : new Atom(bits));}

	public int hashCode() {return(Long.hashCode(bits));}
	public boolean equals(Object x) {return((x instanceof Atom) && (((Atom)x).bits == this.bits));}
	public String toString() {
	    return("#" + Long.toUnsignedString(bits, 16));
	}

	/* Pre-allocated atoms */
	public static final Atom WM_NORMAL_HINTS = Atom.of(40);
	public static final Atom WM_SIZE_HINTS = Atom.of(41);
    }

    public static class VisualID {
	final long bits;

	private VisualID(long bits) {this.bits = bits;}
	static VisualID of(long bits) {return(new VisualID(bits));}

	public int hashCode() {return(Long.hashCode(bits));}
	public boolean equals(Object x) {return((x instanceof VisualID) && (((VisualID)x).bits == this.bits));}
	public String toString() {
	    return("#" + Long.toUnsignedString(bits, 16));
	}
    }

    public static class ExtensionInfo {
	public final String name;
	public final int opcode, event, error;

	public ExtensionInfo(String name, int opcode, int event, int error) {
	    this.name = name;
	    this.opcode = opcode;
	    this.event = event;
	    this.error = error;
	}
    }

    public static class XIM {
	final MemorySegment mem;

	XIM(MemorySegment mem) {
	    this.mem = mem;
	}
    }

    public static class XIC {
	final MemorySegment mem;

	XIC(MemorySegment mem) {
	    this.mem = mem;
	}
    }

    public static abstract class Display extends StructInstance {
	public final XLib lib;
	public boolean closed = false;

	protected Display(XLib lib, MemorySegment mem) {
	    super(mem);
	    this.lib = lib;
	}

	MemorySegment mem() {return(mem);}

	public abstract int fd();
	public abstract int default_screen();
	public abstract int nscreens();
	public abstract MemorySegment screens();
	public abstract List<? extends Screen> lscreens();
    }

    public static abstract class Screen extends StructInstance {
	protected Screen(MemorySegment mem) {
	    super(mem);
	}

	MemorySegment mem() {return(mem);}

	public abstract XID root();
    }

    public static abstract class Visual extends StructInstance {
	protected Visual(MemorySegment mem) {
	    super(mem);
	}

	MemorySegment mem() {return(mem);}
    }

    public static abstract class XVisualInfo extends StructInstance {
	protected XVisualInfo(MemorySegment mem) {
	    super(mem);
	}

	MemorySegment mem() {return(mem);}

	public abstract int depth();
	public abstract Visual visual();
    }

    public static abstract class XSetWindowAttributes extends StructInstance {
	protected XSetWindowAttributes(MemorySegment mem) {
	    super(mem);
	}

	MemorySegment mem() {return(mem);}

	public abstract XSetWindowAttributes event_mask(long value);
	public abstract XSetWindowAttributes background_pixel(long value);
	public abstract XSetWindowAttributes colormap(XID value);
	public abstract XSetWindowAttributes cursor(XID value);
    }

    public static abstract class XWindowChanges extends StructInstance {
	protected XWindowChanges(MemorySegment mem) {
	    super(mem);
	}

	MemorySegment mem() {return(mem);}

	public abstract XWindowChanges x(int value);
	public abstract XWindowChanges y(int value);
	public abstract XWindowChanges width(int value);
	public abstract XWindowChanges height(int value);
	public abstract XWindowChanges border_width(int value);
	public abstract XWindowChanges sibling(XID value);
	public abstract XWindowChanges stack_mode(int value);
    }

    public static abstract class XModifierMapping extends StructInstance {
	protected XModifierMapping(MemorySegment mem) {
	    super(mem);
	}

	MemorySegment mem() {return(mem);}

	public abstract int[][] mapping();
    }

    public static abstract class XEvent extends UnionInstance {
	protected XEvent(MemorySegment mem) {
	    super(mem);
	}

	MemorySegment mem() {return(mem);}
	public abstract int type();
	public abstract XEvent type(int value);

	public abstract int serial();
	public abstract boolean send_event();
	public abstract Display display();
	public abstract XID window();
	public abstract XEvent window(XID value);

	public abstract XKeyEvent xkey();
	public abstract XExposeEvent xexpose();
	public abstract XVisibilityEvent xvisibility();
	public abstract XPropertyEvent xproperty();
	public abstract XConfigureEvent xconfigure();
	public abstract XSelectionClearEvent xselectionclear();
	public abstract XSelectionRequestEvent xselectionrequest();
	public abstract XSelectionEvent xselection();
	public abstract XClientMessageEvent xclient();
	public abstract XGenericEvent xgeneric();
    }

    public static abstract class XErrorEvent extends StructInstance {
	protected XErrorEvent(MemorySegment mem) {
	    super(mem);
	}

	MemorySegment mem() {return(mem);}

	public abstract int type();
	public abstract XID resourceid();
	public abstract long serial();
	public abstract int error_code();
	public abstract int request_code();
	public abstract int minor_code();
    }

    public static abstract class XKeyEvent extends StructInstance {
	protected XKeyEvent(MemorySegment mem) {
	    super(mem);
	}

	MemorySegment mem() {return(mem);}

	public abstract long time();
	public abstract int state();
	public abstract int keycode();
	public abstract int lookup(byte[] buffer, XID[] keysym, XComposeStatus status);
	public abstract int utf8lookup(XIC xic, byte[] buffer, XID[] keysym, int[] status);
    }

    public static abstract class XExposeEvent extends StructInstance {
	protected XExposeEvent(MemorySegment mem) {
	    super(mem);
	}

	MemorySegment mem() {return(mem);}
	public abstract int x();
	public abstract int y();
	public abstract int width();
	public abstract int height();
	public abstract int count();
    }

    public static abstract class XVisibilityEvent extends StructInstance {
	protected XVisibilityEvent(MemorySegment mem) {
	    super(mem);
	}

	MemorySegment mem() {return(mem);}
	public abstract int state();
    }

    public static abstract class XPropertyEvent extends StructInstance {
	protected XPropertyEvent(MemorySegment mem) {
	    super(mem);
	}

	MemorySegment mem() {return(mem);}
	public abstract Atom atom();
	public abstract int state();
    }

    public static abstract class XClientMessageEvent extends StructInstance {
	protected XClientMessageEvent(MemorySegment mem) {
	    super(mem);
	}

	MemorySegment mem() {return(mem);}

	public abstract Atom message_type();
	public abstract XClientMessageEvent message_type(Atom value);
	public abstract int format();
	public abstract XClientMessageEvent format(int value);
	public abstract byte[] b();
	public abstract XClientMessageEvent b(byte[] value);
	public abstract XClientMessageEvent b(int i, byte value);
	public abstract short[] s();
	public abstract XClientMessageEvent s(short[] value);
	public abstract XClientMessageEvent s(int i, short value);
	public abstract long[] l();
	public abstract XClientMessageEvent l(long[] value);
	public abstract XClientMessageEvent l(int i, long value);

	public Atom[] a() {
	    long[] bits = l();
	    Atom[] ret = new Atom[bits.length];
	    for(int i = 0; i < bits.length; i++)
		ret[i] = Atom.of(bits[i]);
	    return(ret);
	}
	public XClientMessageEvent a(Atom[] value) {
	    long[] bobs = new long[value.length];
	    for(int i = 0; i < value.length; i++)
		bobs[i] = (value[i] == null) ? 0 : value[i].bits;
	    return(l(bobs));
	}
	public XClientMessageEvent a(int i, Atom value) {
	    return(l(i, value.bits));
	}
	public XID[] x() {
	    long[] bits = l();
	    XID[] ret = new XID[bits.length];
	    for(int i = 0; i < bits.length; i++)
		ret[i] = XID.of(bits[i]);
	    return(ret);
	}
	public XClientMessageEvent x(XID[] value) {
	    long[] bobs = new long[value.length];
	    for(int i = 0; i < value.length; i++)
		bobs[i] = (value[i] == null) ? 0 : value[i].bits;
	    return(l(bobs));
	}
	public XClientMessageEvent x(int i, XID value) {
	    return(l(i, value.bits));
	}
    }

    public static abstract class XConfigureEvent extends StructInstance {
	protected XConfigureEvent(MemorySegment mem) {
	    super(mem);
	}

	MemorySegment mem() {return(mem);}

	public abstract XID event();
	public abstract XID window();
	public abstract int x();
	public abstract int y();
	public abstract int width();
	public abstract int height();
	public abstract int border_width();
	public abstract XID above();
	public abstract boolean override_redirect();
    }

    public static abstract class XSelectionClearEvent extends StructInstance {
	protected XSelectionClearEvent(MemorySegment mem) {
	    super(mem);
	}

	MemorySegment mem() {return(mem);}

	public abstract XID window();
	public abstract Atom selection();
	public abstract long time();
    }

    public static abstract class XSelectionRequestEvent extends StructInstance {
	protected XSelectionRequestEvent(MemorySegment mem) {
	    super(mem);
	}

	MemorySegment mem() {return(mem);}

	public abstract XID owner();
	public abstract XID requestor();
	public abstract Atom selection();
	public abstract Atom target();
	public abstract Atom property();
	public abstract long time();
    }

    public static abstract class XSelectionEvent extends StructInstance {
	protected XSelectionEvent(MemorySegment mem) {
	    super(mem);
	}

	MemorySegment mem() {return(mem);}

	public abstract XID requestor();
	public abstract Atom selection();
	public abstract Atom target();
	public abstract Atom property();
	public abstract long time();

	public abstract XSelectionEvent requestor(XID value);
	public abstract XSelectionEvent selection(Atom value);
	public abstract XSelectionEvent target(Atom value);
	public abstract XSelectionEvent property(Atom value);
	public abstract XSelectionEvent time(long value);
    }

    public static abstract class XGenericEvent extends StructInstance {
	protected XGenericEvent(MemorySegment mem) {
	    super(mem);
	}

	MemorySegment mem() {return(mem);}

	public abstract int extension();
	public abstract int evtype();
	public abstract int cookie();
	public abstract MemorySegment data();
    }

    public static abstract class XIMStyles extends StructInstance {
	protected XIMStyles(MemorySegment mem) {
	    super(mem);
	}

	MemorySegment mem() {return(mem);}

	public abstract long[] styles();
    }

    public static abstract class XComposeStatus extends StructInstance {
	protected XComposeStatus(MemorySegment mem) {
	    super(mem);
	}

	MemorySegment mem() {return(mem);}
    }

    public static class XProperty {
	public final Atom name, type;
	public final int format;
	public final int len;
	private final Display dpy;
	private final MemorySegment data;

	public XProperty(Display dpy, Atom name, Atom type, int format, int len, MemorySegment data) {
	    this.dpy = dpy;
	    this.name = name;
	    this.type = type;
	    this.format = format;
	    this.len = len;
	    this.data = data;
	}

	public XProperty(Display dpy, Atom name, Atom type, int format, int len, ByteBuffer data) {
	    this(dpy, name, type, format, len, MemorySegment.ofBuffer(data));
	}

	public XProperty(Display dpy, Atom name, Atom type, int format, int len, byte[] data) {
	    this(dpy, name, type, format, len, MemorySegment.ofArray(data));
	}

	public byte b(int i) {
	    return(data.get(ValueLayout.JAVA_BYTE, i * ValueLayout.JAVA_BYTE.byteSize()));
	}
	public byte[] copy(byte[] dst, int doff, int soff, int len) {
	    for(int i = 0; i < len; i++)
		dst[i + doff] = data.get(ValueLayout.JAVA_BYTE, (i + soff) * ValueLayout.JAVA_BYTE.byteSize());
	    return(dst);
	}
	public byte[] b() {
	    return(copy(new byte[len], 0, 0, len));
	}

	public short s(int i) {
	    return(data.get(ValueLayout.JAVA_SHORT, i * ValueLayout.JAVA_SHORT.byteSize()));
	}
	public short[] s() {
	    short[] ret = new short[len];
	    for(int i = 0; i < len; i++)
		ret[i] = data.get(ValueLayout.JAVA_SHORT, i * ValueLayout.JAVA_SHORT.byteSize());
	    return(ret);
	}

	public long l(int i) {
	    return(data.get(ValueLayout.JAVA_LONG, i * ValueLayout.JAVA_LONG.byteSize()));
	}
	public long[] l() {
	    long[] ret = new long[len];
	    for(int i = 0; i < len; i++)
		ret[i] = getint(data, C_LONG.byteSize() * i, C_LONG, true);
	    return(ret);
	}

	public Atom a(int i) {
	    return(Atom.of(l(i)));
	}
	public Atom[] a() {
	    long[] l = l();
	    Atom[] r = new Atom[l.length];
	    for(int i = 0; i < l.length; r[i] = Atom.of(l[i]), i++);
	    return(r);
	}

	public XID x(int i) {
	    return(XID.of(l(i)));
	}
	public XID[] x() {
	    long[] l = l();
	    XID[] r = new XID[l.length];
	    for(int i = 0; i < l.length; r[i] = XID.of(l[i]), i++);
	    return(r);
	}

	public String toString() {
	    if(dpy.closed) throw(new IllegalStateException());
	    StringBuilder buf = new StringBuilder();
	    if(len * format < 512) {
		if(format == 8) {
		    for(int i = 0; i < len; i++)
			buf.append(String.format(" %02x", b(i) & 0xff));
		} else if(format == 16) {
		    for(int i = 0; i < len; i++)
			buf.append(String.format(" %04x", s(i) & 0xffff));
		} else if(format == 32) {
		    for(int i = 0; i < len; i++)
			buf.append(String.format(" %08x", l(i) & 0xffffffffl));
		}
	    }
	    return(String.format("#<x-property %s %s %d %d%s>", dpy.lib.XGetAtomName(dpy, name), dpy.lib.XGetAtomName(dpy, type), format, len, buf));
	}
    }

    public static interface XrmDatabase {}

    public static class XrmValue {
	public final String type;
	public final byte[] data;

	public XrmValue(String type, byte[] data) {
	    this.type = type;
	    this.data = data;
	}

	public String string() {
	    if(!Utils.eq(type, "String"))
		throw(new IllegalArgumentException(type));
	    if((data.length == 0) || (data[data.length - 1] != 0))
		throw(new IllegalArgumentException(Utils.bprint.enc(data)));
	    return(new String(data, 0, data.length - 1, C_CHARSET));
	}

	public String toString() {
	    return(type + ": " + ((data == null) ? "''" : Utils.bprint.enc(data)));
	}
    }

    public static class XSizeHints {
	public long flags;
	public int x, y;
	public int width, height;
	public int min_width, min_height;
	public int max_width, max_height;
	public int width_inc, height_inc;
	public int min_aspect_x, min_aspect_y;
	public int max_aspect_x, max_aspect_y;
	public int base_width, base_height;
	public int win_gravity;
    }

    public static class XkbExtensionInfo {
	public final int opcode, event, error, major, minor;

	public XkbExtensionInfo(int opcode, int event, int error, int major, int minor) {
	    this.opcode = opcode;
	    this.event = event;
	    this.error = error;
	    this.major = major;
	    this.minor = minor;
	}
    }

    public static interface XkbSymMap {
	public XID[][] keysyms();
	public int groupinfo();
	public default int egroup(int g) {
	    if(g < 0)
		throw(new IllegalArgumentException());
	    int inf = groupinfo();
	    int n = inf & 0x0f;
	    if(g < n)
		return(g);
	    int rb = inf & 0xc0;
	    int rn = (inf & 0x30) >> 4;
	    switch(rb) {
	    case XkbClampIntoRange: return(Math.max(Math.min(g, n - 1), 0));
	    case XkbRedirectIntoRange: return(Math.max(Math.min(rn, n - 1), 0));
	    case XkbWrapIntoRange: default: return(g % n);
	    }
	}
    }

    public static abstract class XkbClientMap extends StructInstance {
	protected XkbClientMap(MemorySegment mem) {
	    super(mem);
	}

	MemorySegment mem() {return(mem);}

	public abstract List<? extends XkbSymMap> key_sym_map();
	public abstract byte[] modmap();
    }

    public static abstract class XkbNames extends StructInstance {
	protected XkbNames(MemorySegment mem) {
	    super(mem);
	}

	MemorySegment mem() {return(mem);}

	public abstract Atom[] vmods();
	public abstract List<String> keys();
	public abstract List<Pair<String, String>> key_aliases();
    }

    public static abstract class XkbDesc extends StructInstance {
	protected XkbDesc(MemorySegment mem) {
	    super(mem);
	}

	MemorySegment mem() {return(mem);}

	public abstract int flags();
	public abstract int device_spec();
	public abstract int min_key_code();
	public abstract int max_key_code();
	public abstract XkbClientMap map();
	public abstract XkbNames names();
    }

    private static final ThreadLocal<Supplier<RuntimeException>> lasterror = new ThreadLocal<>();
    private static int handleerror(XLib xlib, MemorySegment dpy, MemorySegment err) {
	try {
	    Supplier<RuntimeException> lost = lasterror.get();
	    XException exc = new XException(xlib.XErrorEvent(err));
	    if(lost != null)
		exc.addSuppressed(lost.get());
	    lasterror.set(() -> exc);
	} catch(Throwable t) {
	    t.printStackTrace();
	}
	return(0);
    }
    public static void checkerror() {
	Supplier<RuntimeException> err = lasterror.get();
	lasterror.remove();
	if(err != null)
	    throw(err.get());
    }

    public abstract int XInitThreads();
    public abstract void XSetErrorHandler();
    public abstract Display XOpenDisplay(String name);
    public abstract int XCloseDisplay(Display dpy);
    public abstract Atom XInternAtom(XLib.Display dpy, String name, boolean only_if_exists);
    public abstract Atom[] XInternAtoms(XLib.Display dpy, String[] names, boolean only_if_exists);
    public abstract int XFree(MemorySegment mem);
    public abstract String XGetAtomName(XLib.Display dpy, Atom atom);
    public abstract String XServerVendor(XLib.Display dpy);
    public abstract int XVendorRelease(XLib.Display dpy);
    public abstract ExtensionInfo XQueryExtension(XLib.Display dpy, String name);
    public abstract XID XCreateColormap(XLib.Display dpy, XID w, Visual visual, int alloc);
    public abstract Coord XQueryBestCursor(XLib.Display dpy, XID d, Coord size);
    public abstract void XDefineCursor(XLib.Display dpy, XID w, XID cursor);
    public abstract void XFreeCursor(XLib.Display dpy, XID cursor);
    public abstract XID XCreateWindow(XLib.Display dpy, XID parent, int x, int y, int width, int height, int border_width, int depth, int cl, XLib.Visual visual, long valuemask, XSetWindowAttributes attributes);
    public abstract int XDestroyWindow(XLib.Display dpy, XID w);
    public abstract int[] XDisplayKeycodes(XLib.Display dpy);
    public abstract XID[][] XGetKeyboardMapping(XLib.Display dpy, int first_keycode, int keycode_count);
    public abstract XModifierMapping XGetModifierMapping(XLib.Display dpy);
    public abstract int XSelectInput(Display dpy, XID w, long event_mask);
    public abstract boolean XFilterEvent(XEvent ev, XID w);
    public abstract int XUnmapWindow(Display dpy, XID w);
    public abstract int XMapWindow(Display dpy, XID w);
    public abstract int XMapRaised(Display dpy, XID w);
    public abstract int XConfigureWindow(XLib.Display dpy, XID w, int valuemask, XWindowChanges values);
    public abstract Coord XTranslateCoordinates(XLib.Display dpy, XID src_w, XID dest_w, Coord src);
    public abstract XProperty XGetWindowProperty(XLib.Display dpy, XID w, Atom property, boolean delete, Atom reg_type);
    public abstract int XChangeProperty(XLib.Display dpy, XID w, Atom property, Atom type, int mode, byte[] data);
    public abstract int XChangeProperty(XLib.Display dpy, XID w, Atom property, Atom type, int mode, short[] data);
    public abstract int XChangeProperty(XLib.Display dpy, XID w, Atom property, Atom type, int mode, long[] data);
    public abstract int XDeleteProperty(XLib.Display dpy, XID w, Atom property);
    public abstract int XStoreName(XLib.Display dpy, XID w, String window_name);
    public abstract int XIconifyWindow(XLib.Display dpy, XID w, int screen_number);
    public abstract int XNextEvent(XLib.Display dpy, XEvent event_return);
    public abstract int XPending(XLib.Display dpy);
    public abstract int XSync(XLib.Display dpy, boolean discard);
    public abstract int XSendEvent(XLib.Display dpy, XID w, boolean propagate, long event_mask, XLib.XEvent event_send);
    public abstract String XKeysymToString(XID keysym);
    public abstract XIM XOpenIM(Display dpy, String res_name, String res_class);
    public abstract int XCloseIM(XIM xim);
    public abstract Map<String, ?> XGetIMValues(XIM xim, Collection<String> values);
    public abstract XIC XCreateIC(XIM xim, Map<String, ?> values);
    public abstract void XDestroyIC(XIC xic);
    public abstract void XSetICFocus(XIC xic);
    public abstract void XUnsetICFocus(XIC xic);
    public abstract Map<String, ?> XGetICValues(XIC xic, Collection<String> values);

    public abstract XSetWindowAttributes XSetWindowAttributes();
    public abstract XWindowChanges XWindowChanges();
    public abstract XEvent XEvent();
    public abstract XComposeStatus XComposeStatus();
    abstract XErrorEvent XErrorEvent(MemorySegment mem);

    public abstract XID XGetSelectionOwner(XLib.Display dpy, Atom selection);
    public abstract int XSetSelectionOwner(XLib.Display dpy, Atom selection, XID owner, long time);
    public abstract int XConvertSelection(XLib.Display dpy, Atom selection, Atom target, Atom property, XID requestor, long time);

    public abstract void XrmInitialize();
    public abstract String XResourceManagerString(Display dpy);
    public abstract String XScreenResourceString(Screen screen);
    public abstract XrmDatabase XrmGetStringDatabase(String data);
    public abstract XrmDatabase XrmGetFileDatabase(String filename);
    public abstract XrmDatabase XrmMergeDatabases(XrmDatabase source, XrmDatabase target);
    public abstract XrmValue XrmGetResource(XrmDatabase database, String name, String cls);

    public abstract boolean XkbLibraryVersion(int[] version);
    public abstract XkbExtensionInfo XkbQueryExtension(Display dpy, int major, int minor);
    public abstract boolean XkbSetDetectableAutoRepeat(Display dpy, boolean detectable, boolean[] supported);
    public abstract XkbDesc XkbGetKeyboard(Display dpy, int which, int device_spec);
    public abstract XkbDesc XkbGetMap(Display dpy, int which, int device_spec);
    public abstract void XkbGetNames(Display dpy, int which, XkbDesc xkb);

    /* Helper functions */
    public int XChangeProperty(XLib.Display dpy, XID w, Atom property, Atom type, int mode, Atom[] data) {
	long[] bits = new long[data.length];
	for(int i = 0; i < data.length; i++)
	    bits[i] = (data[i] == null) ? 0 : data[i].bits;
	return(XChangeProperty(dpy, w, property, type, mode, bits));
    }

    public int XConfigureWindow(XLib.Display dpy, XID window, Integer x, Integer y, Integer w, Integer h, Integer bw, XID sibling, Integer mode) {
	XWindowChanges values = XWindowChanges();
	int mask = 0;
	if(x != null) {mask |= CWX; values.x(x);}
	if(y != null) {mask |= CWY; values.y(y);}
	if(w != null) {mask |= CWWidth; values.width(w);}
	if(h != null) {mask |= CWHeight; values.height(h);}
	if(bw != null) {mask |= CWBorderWidth; values.border_width(bw);}
	if(sibling != null) {mask |= CWSibling; values.sibling(sibling);}
	if(mode != null) {mask |= CWStackMode; values.stack_mode(mode);}
	return(XConfigureWindow(dpy, window, mask, values));
    }

    public int XResizeWindow(XLib.Display dpy, XID window, int w, int h) {
	return(XConfigureWindow(dpy, window, null, null, w, h, null, null, null));
    }

    public void XSetWMNormalHints(Display dpy, XID w, XSizeHints hints) {
	long[] v = new long[18];
	v[0] = hints.flags;
	v[1] = hints.x; v[2] = hints.y;
	v[3] = hints.width; v[4] = hints.height;
	v[5] = hints.min_width; v[6] = hints.min_height;
	v[7] = hints.max_width; v[8] = hints.max_height;
	v[9] = hints.width_inc; v[10] = hints.height_inc;
	v[11] = hints.min_aspect_x; v[12] = hints.min_aspect_y;
	v[13] = hints.max_aspect_x; v[14] = hints.max_aspect_y;
	v[15] = hints.base_width; v[16] = hints.base_height;
	v[17] = hints.win_gravity;
	XChangeProperty(dpy, w, Atom.WM_NORMAL_HINTS, Atom.WM_SIZE_HINTS, PropModeReplace, v);
    }

    /* Macros */
    public int ConnectionNumber(Display dpy) {return(dpy.fd());}
    public int DefaultScreen(Display dpy) {return(dpy.default_screen());}
    public int ScreenCount(Display dpy) {return(dpy.nscreens());}
    public Screen ScreenOfDisplay(Display dpy, int scr) {return(dpy.lscreens().get(scr));}
    public XID DefaultRootWindow(Display dpy) {return(ScreenOfDisplay(dpy, DefaultScreen(dpy)).root());}
    public int XkbGroupForCoreState(int s) {return((s >> 13) & 0x3);}

    static class libX11_so_6 extends XLib {
	static final MemoryLayout C_Status = C_INT;
	static final MemoryLayout C_XID = C_LONG;
	static final MemoryLayout C_Atom = C_LONG;
	static final MemoryLayout C_Time = C_LONG;
	static final MemoryLayout C_VisualID = C_LONG;
	static final MemoryLayout C_XBool = C_INT;
	static final MemoryLayout C_XPointer = ADDRESS;
	static final MemoryLayout C_XIMStyle = C_LONG;
	static final MemoryLayout C_KeyCode = C_CHAR;
	private final SymbolLookup xlib = SymbolLookup.libraryLookup("libX11.so.6", Arena.global());

	private static void set(MemorySegment mem, VarHandle var, long offset, XID value) {
	    if(C_XID instanceof ValueLayout.OfLong)
		var.set(mem, offset, (long)value.bits);
	    else
		var.set(mem, offset, (int)value.bits);
	}

	private static void set(MemorySegment mem, VarHandle var, long offset, Atom value) {
	    if(C_Atom instanceof ValueLayout.OfLong)
		var.set(mem, offset, (long)value.bits);
	    else
		var.set(mem, offset, (int)value.bits);
	}

	private static void settime(MemorySegment mem, VarHandle var, long offset, long value) {
	    if(C_Time instanceof ValueLayout.OfLong)
		var.set(mem, offset, (long)value);
	    else
		var.set(mem, offset, (int)value);
	}

	static final StructLayout _Visual = struct(new MemoryLayout[] {
		ADDRESS.withName("ext_data"),
		C_VisualID.withName("visualid"),
		C_INT.withName("class"),
		C_LONG.withName("red_mask"),
		C_LONG.withName("green_mask"),
		C_LONG.withName("blue_mask"),
		C_INT.withName("bits_per_rgb"),
		C_INT.withName("map_entries"),
	    });
	public static class Visual extends XLib.Visual {
	    Visual(MemorySegment mem) {
		super(mem);
	    }

	    protected StructLayout $layout() {return(_Visual);}
	}

	static final StructLayout _XVisualInfo = struct(new MemoryLayout[] {
		ADDRESS.withName("visual"),
		C_VisualID.withName("visualid"),
		C_INT.withName("screen"),
		C_INT.withName("depth"),
		C_INT.withName("class"),
		C_LONG.withName("red_mask"),
		C_LONG.withName("green_mask"),
		C_LONG.withName("blue_mask"),
		C_INT.withName("colormap_size"),
		C_INT.withName("bits_per_rgb"),
	    });
	public static class XVisualInfo extends XLib.XVisualInfo {
	    XVisualInfo(MemorySegment mem) {
		super(mem);
	    }

	    protected StructLayout $layout() {return(_XVisualInfo);}

	    private static final VarHandle visual = _XVisualInfo.varHandle(PathElement.groupElement("visual"));
	    public Visual visual() {return(new Visual((MemorySegment)visual.get(mem, 0)));}
	    private static final VarHandle depth = _XVisualInfo.varHandle(PathElement.groupElement("depth"));
	    public int depth() {return((int)depth.get(mem, 0));}
	}

	static final StructLayout _XSetWindowAttributes = struct(new MemoryLayout[] {
		C_XID.withName("background_pixmap"),
		C_LONG.withName("background_pixel"),
		C_XID.withName("border_pixmap"),
		C_LONG.withName("border_pixel"),
		C_INT.withName("bit_gravity"),
		C_INT.withName("win_gravity"),
		C_INT.withName("backing_store"),
		C_LONG.withName("backing_planes"),
		C_LONG.withName("backing_pixel"),
		C_XBool.withName("save_under"),
		C_LONG.withName("event_mask"),
		C_LONG.withName("do_not_propagate_mask"),
		C_XBool.withName("override_redirect"),
		C_XID.withName("colormap"),
		C_XID.withName("cursor"),
	    });
	public static class XSetWindowAttributes extends XLib.XSetWindowAttributes {
	    XSetWindowAttributes(MemorySegment mem) {
		super(mem);
	    }

	    XSetWindowAttributes() {
		super(Arena.ofAuto().allocate(_XSetWindowAttributes.byteSize(), _XSetWindowAttributes.byteAlignment()));
	    }

	    protected StructLayout $layout() {return(_XSetWindowAttributes);}

	    private static final VarHandle background_pixel = _XSetWindowAttributes.varHandle(PathElement.groupElement("background_pixel"));
	    public XSetWindowAttributes background_pixel(long value) {background_pixel.set(mem, 0, value); return(this);}
	    private static final VarHandle event_mask = _XSetWindowAttributes.varHandle(PathElement.groupElement("event_mask"));
	    public XSetWindowAttributes event_mask(long value) {event_mask.set(mem, 0, value); return(this);}
	    private static final VarHandle colormap = _XSetWindowAttributes.varHandle(PathElement.groupElement("colormap"));
	    public XSetWindowAttributes colormap(XID value) {colormap.set(mem, 0, value.bits); return(this);}
	    private static final VarHandle cursor = _XSetWindowAttributes.varHandle(PathElement.groupElement("cursor"));
	    public XSetWindowAttributes cursor(XID value) {cursor.set(mem, 0, value.bits); return(this);}
	}

	static final StructLayout _XWindowChanges = struct(new MemoryLayout[] {
		C_INT.withName("x"),
		C_INT.withName("y"),
		C_INT.withName("width"),
		C_INT.withName("height"),
		C_INT.withName("border_width"),
		C_XID.withName("sibling"),
		C_INT.withName("stack_mode"),
	    });
	public static class XWindowChanges extends XLib.XWindowChanges {
	    XWindowChanges(MemorySegment mem) {
		super(mem);
	    }

	    XWindowChanges() {
		super(Arena.ofAuto().allocate(_XWindowChanges));
	    }

	    protected StructLayout $layout() {return(_XWindowChanges);}

	    private static final VarHandle x = _XWindowChanges.varHandle(PathElement.groupElement("x"));
	    public XWindowChanges x(int value) {x.set(mem, 0, value); return(this);}
	    private static final VarHandle y = _XWindowChanges.varHandle(PathElement.groupElement("y"));
	    public XWindowChanges y(int value) {y.set(mem, 0, value); return(this);}
	    private static final VarHandle width = _XWindowChanges.varHandle(PathElement.groupElement("width"));
	    public XWindowChanges width(int value) {width.set(mem, 0, value); return(this);}
	    private static final VarHandle height = _XWindowChanges.varHandle(PathElement.groupElement("height"));
	    public XWindowChanges height(int value) {height.set(mem, 0, value); return(this);}
	    private static final VarHandle border_width = _XWindowChanges.varHandle(PathElement.groupElement("border_width"));
	    public XWindowChanges border_width(int value) {border_width.set(mem, 0, value); return(this);}
	    private static final VarHandle sibling = _XWindowChanges.varHandle(PathElement.groupElement("sibling"));
	    public XWindowChanges sibling(XID value) {sibling.set(mem, 0, value.bits); return(this);}
	    private static final VarHandle stack_mode = _XWindowChanges.varHandle(PathElement.groupElement("stack_mode"));
	    public XWindowChanges stack_mode(int value) {stack_mode.set(mem, 0, value); return(this);}
	}

	static final StructLayout _XModifierMapping = struct(new MemoryLayout[] {
		C_INT.withName("max_keypermod"),
		ADDRESS.withName("modifiermap"),
	    });
	public static class XModifierMapping extends XLib.XModifierMapping {
	    XModifierMapping(libX11_so_6 xlib, MemorySegment mem) {
		super(mem);
		Finalizer.finalize(this, () -> xlib.XFreeModifiermap(mem));
	    }

	    protected StructLayout $layout() {return(_XModifierMapping);}

	    private static final VarHandle max_keypermod = _XModifierMapping.varHandle(PathElement.groupElement("max_keypermod"));
	    public int max_keypermod() {return((int)max_keypermod.get(mem, 0));}
	    private static final VarHandle modifiermap = _XModifierMapping.varHandle(PathElement.groupElement("modifiermap"));
	    public MemorySegment modifiermap() {return(((MemorySegment)modifiermap.get(mem, 0)).reinterpret(C_KeyCode.byteSize() * max_keypermod() * 8));}

	    public int[][] mapping() {
		int nk = max_keypermod();
		MemorySegment map = modifiermap();
		int[][] ret = new int[8][];
		for(int i = 0; i < 8; i++) {
		    int[] buf = new int[nk];
		    int n = 0;
		    for(int o = 0; o < nk; o++) {
			int key = (int)getint(map, C_KeyCode.byteSize() * ((i * nk) + o), C_KeyCode, false);
			if(key != 0)
			    buf[n++] = key;
		    }
		    ret[i] = Arrays.copyOf(buf, n);
		}
		return(ret);
	    }
	}

	static final StructLayout _XAnyEvent = struct(new MemoryLayout[] {
		C_INT.withName("type"),
		C_LONG.withName("serial"),
		C_XBool.withName("send_event"),
		ADDRESS.withName("display"),
		C_XID.withName("window"),
	    });
	static final UnionLayout _XEvent = MemoryLayout.unionLayout(new MemoryLayout[] {
		C_INT.withName("type"),
		_XAnyEvent.withName("xany"),
		MemoryLayout.sequenceLayout(24, C_LONG).withName("pad"),
	    });
	public static class XEvent extends XLib.XEvent {
	    final libX11_so_6 xlib;

	    XEvent(libX11_so_6 xlib, MemorySegment mem) {
		super(mem);
		this.xlib = xlib;
	    }

	    XEvent(libX11_so_6 xlib) {
		super(Arena.ofAuto().allocate(_XEvent.byteSize(), _XEvent.byteAlignment()));
		this.xlib = xlib;
	    }

	    protected UnionLayout $layout() {return(_XEvent);}

	    private static final VarHandle type = _XEvent.varHandle(PathElement.groupElement("type"));
	    public int type() {return((int)type.get(mem, 0));}
	    public XEvent type(int value) {type.set(mem, 0, value); return(this);}

	    private static final VarHandle serial = _XEvent.varHandle(PathElement.groupElement("xany"), PathElement.groupElement("serial"));
	    public int serial() {return((int)serial.get(mem, 0));}
	    private static final VarHandle send_event = _XEvent.varHandle(PathElement.groupElement("xany"), PathElement.groupElement("send_event"));
	    public boolean send_event() {return(((int)send_event.get(mem, 0)) != 0);}
	    private static final VarHandle display = _XEvent.varHandle(PathElement.groupElement("xany"), PathElement.groupElement("display"));
	    public Display display() {return(new Display(xlib, (MemorySegment)display.get(mem, 0)));}
	    private static final VarHandle window = _XEvent.varHandle(PathElement.groupElement("xany"), PathElement.groupElement("window"));
	    public XID window() {return(XID.of((long)window.get(mem, 0)));}
	    public XEvent window(XID value) {
		if(C_XID instanceof ValueLayout.OfLong)
		    window.set(mem, 0, (long)value.bits);
		else
		    window.set(mem, 0, (int)value.bits);
		return(this);
	    }

	    public XKeyEvent xkey() {return(new XKeyEvent(xlib, mem));}
	    public XExposeEvent xexpose() {return(new XExposeEvent(mem));}
	    public XVisibilityEvent xvisibility() {return(new XVisibilityEvent(mem));}
	    public XPropertyEvent xproperty() {return(new XPropertyEvent(mem));}
	    public XConfigureEvent xconfigure() {return(new XConfigureEvent(mem));}
	    public XSelectionClearEvent xselectionclear() {return(new XSelectionClearEvent(mem));}
	    public XSelectionRequestEvent xselectionrequest() {return(new XSelectionRequestEvent(mem));}
	    public XSelectionEvent xselection() {return(new XSelectionEvent(mem));}
	    public XClientMessageEvent xclient() {return(new XClientMessageEvent(mem));}
	    public XGenericEvent xgeneric() {return(new XGenericEvent(xlib, mem));}
	}

	static final StructLayout _XErrorEvent = struct(new MemoryLayout[] {
		C_INT.withName("type"),
		ADDRESS.withName("display"),
		C_XID.withName("resourceid"),
		C_LONG.withName("serial"),
		C_CHAR.withName("error_code"),
		C_CHAR.withName("request_code"),
		C_CHAR.withName("minor_code"),
	    });
	public static class XErrorEvent extends XLib.XErrorEvent {
	    XErrorEvent(MemorySegment mem) {
		super(mem);
	    }

	    protected StructLayout $layout() {return(_XErrorEvent);}

	    private static final VarHandle type = _XErrorEvent.varHandle(PathElement.groupElement("type"));
	    public int type() {return((int)type.get(mem, 0));}
	    private static final VarHandle resourceid = _XErrorEvent.varHandle(PathElement.groupElement("resourceid"));
	    public XID resourceid() {return(XID.of((long)resourceid.get(mem, 0)));}
	    private static final VarHandle serial = _XErrorEvent.varHandle(PathElement.groupElement("serial"));
	    public long serial() {return((long)serial.get(mem, 0));}
	    private static final VarHandle error_code = _XErrorEvent.varHandle(PathElement.groupElement("error_code"));
	    public int error_code() {return(((byte)error_code.get(mem, 0)) & 0xff);}
	    private static final VarHandle request_code = _XErrorEvent.varHandle(PathElement.groupElement("request_code"));
	    public int request_code() {return(((byte)request_code.get(mem, 0)) & 0xff);}
	    private static final VarHandle minor_code = _XErrorEvent.varHandle(PathElement.groupElement("minor_code"));
	    public int minor_code() {return(((byte)minor_code.get(mem, 0)) & 0xff);}
	}

	static final StructLayout _XKeyEvent = struct(new MemoryLayout[] {
		C_INT.withName("type"),
		C_LONG.withName("serial"),
		C_XBool.withName("send_event"),
		ADDRESS.withName("display"),
		C_XID.withName("window"),
		C_XID.withName("root"),
		C_XID.withName("subwindow"),
		C_Time.withName("time"),
		C_INT.withName("x"),
		C_INT.withName("y"),
		C_INT.withName("x_root"),
		C_INT.withName("y_root"),
		C_INT.withName("state"),
		C_INT.withName("keycode"),
		C_XBool.withName("same_screen"),
	    });
	public static class XKeyEvent extends XLib.XKeyEvent {
	    final libX11_so_6 xlib;
	    XKeyEvent(libX11_so_6 xlib, MemorySegment mem) {
		super(mem);
		this.xlib = xlib;
	    }

	    protected StructLayout $layout() {return(_XKeyEvent);}

	    private static final VarHandle time = _XKeyEvent.varHandle(PathElement.groupElement("time"));
	    public long time() {return((long)time.get(mem, 0));}
	    private static final VarHandle state = _XKeyEvent.varHandle(PathElement.groupElement("state"));
	    public int state() {return((int)state.get(mem, 0));}
	    private static final VarHandle keycode = _XKeyEvent.varHandle(PathElement.groupElement("keycode"));
	    public int keycode() {return((int)keycode.get(mem, 0));}

	    public int lookup(byte[] buffer, XID[] keysym, XLib.XComposeStatus status) {
		try(Arena st = Arena.ofConfined()) {
		    MemorySegment cbuf = st.allocate(buffer.length);
		    MemorySegment sbuf = st.allocate(C_XID);
		    int rv = xlib.XLookupString(mem, cbuf, sbuf, status == null ? MemorySegment.NULL : ((XComposeStatus)status).mem());
		    memcpy(buffer, cbuf, 0, 0, rv);
		    if(keysym != null)
			keysym[0] = XID.of(getint(sbuf, 0, C_XID, false));
		    return(rv);
		}
	    }

	    public int utf8lookup(XIC xic, byte[] buffer, XID[] keysym, int[] status) {
		try(Arena st = Arena.ofConfined()) {
		    MemorySegment cbuf = st.allocate(buffer.length);
		    MemorySegment sbuf = st.allocate(C_XID);
		    MemorySegment rbuf = st.allocate(C_Status);
		    int rv = xlib.Xutf8LookupString(xic, mem, cbuf, sbuf, rbuf);
		    memcpy(buffer, cbuf, 0, 0, rv);
		    if(keysym != null)
			keysym[0] = XID.of(getint(sbuf, 0, C_XID, false));
		    if(status != null)
			status[0] = (int)getint(rbuf, 0, C_Status, false);
		    return(rv);
		}
	    }
	}

	static final StructLayout _XExposeEvent = struct(new MemoryLayout[] {
		C_INT.withName("type"),
		C_LONG.withName("serial"),
		C_XBool.withName("send_event"),
		ADDRESS.withName("display"),
		C_XID.withName("window"),
		C_INT.withName("x"),
		C_INT.withName("y"),
		C_INT.withName("width"),
		C_INT.withName("height"),
		C_INT.withName("count"),
	    });
	public static class XExposeEvent extends XLib.XExposeEvent {
	    XExposeEvent(MemorySegment mem) {
		super(mem);
	    }

	    protected StructLayout $layout() {return(_XExposeEvent);}

	    private static final VarHandle x = _XExposeEvent.varHandle(PathElement.groupElement("x"));
	    public int x() {return((int)x.get(mem, 0));}
	    private static final VarHandle y = _XExposeEvent.varHandle(PathElement.groupElement("y"));
	    public int y() {return((int)y.get(mem, 0));}
	    private static final VarHandle width = _XExposeEvent.varHandle(PathElement.groupElement("width"));
	    public int width() {return((int)width.get(mem, 0));}
	    private static final VarHandle height = _XExposeEvent.varHandle(PathElement.groupElement("height"));
	    public int height() {return((int)height.get(mem, 0));}
	    private static final VarHandle count = _XExposeEvent.varHandle(PathElement.groupElement("count"));
	    public int count() {return((int)count.get(mem, 0));}
	}

	static final StructLayout _XVisibilityEvent = struct(new MemoryLayout[] {
		C_INT.withName("type"),
		C_LONG.withName("serial"),
		C_XBool.withName("send_event"),
		ADDRESS.withName("display"),
		C_XID.withName("window"),
		C_INT.withName("state"),
	    });
	public static class XVisibilityEvent extends XLib.XVisibilityEvent {
	    XVisibilityEvent(MemorySegment mem) {
		super(mem);
	    }

	    protected StructLayout $layout() {return(_XVisibilityEvent);}

	    private static final VarHandle state = _XVisibilityEvent.varHandle(PathElement.groupElement("state"));
	    public int state() {return((int)state.get(mem, 0));}
	}

	static final StructLayout _XPropertyEvent = struct(new MemoryLayout[] {
		C_INT.withName("type"),
		C_LONG.withName("serial"),
		C_XBool.withName("send_event"),
		ADDRESS.withName("display"),
		C_XID.withName("window"),
		C_Atom.withName("atom"),
		C_Time.withName("time"),
		C_INT.withName("state"),
	    });
	public static class XPropertyEvent extends XLib.XPropertyEvent {
	    XPropertyEvent(MemorySegment mem) {
		super(mem);
	    }

	    protected StructLayout $layout() {return(_XPropertyEvent);}

	    private static final VarHandle atom = _XPropertyEvent.varHandle(PathElement.groupElement("atom"));
	    public Atom atom() {return(Atom.of((long)atom.get(mem, 0)));}
	    private static final VarHandle state = _XPropertyEvent.varHandle(PathElement.groupElement("state"));
	    public int state() {return((int)state.get(mem, 0));}
	}

	static final StructLayout _XClientMessageEvent = struct(new MemoryLayout[] {
		C_INT.withName("type"),
		C_LONG.withName("serial"),
		C_XBool.withName("send_event"),
		ADDRESS.withName("display"),
		C_XID.withName("window"),
		C_Atom.withName("message_type"),
		C_INT.withName("format"),
		MemoryLayout.unionLayout(new MemoryLayout[] {
			MemoryLayout.sequenceLayout(20, C_CHAR).withName("b"),
			MemoryLayout.sequenceLayout(10, C_SHORT).withName("s"),
			MemoryLayout.sequenceLayout(5, C_LONG).withName("l"),
		    }).withName("data"),
	    });
	public static class XClientMessageEvent extends XLib.XClientMessageEvent {
	    XClientMessageEvent(MemorySegment mem) {
		super(mem);
	    }

	    protected StructLayout $layout() {return(_XClientMessageEvent);}

	    private static final VarHandle message_type = _XClientMessageEvent.varHandle(PathElement.groupElement("message_type"));
	    public Atom message_type() {return(Atom.of((long)message_type.get(mem, 0)));}
	    public XClientMessageEvent message_type(Atom value) {
		if(C_Atom instanceof ValueLayout.OfLong)
		    message_type.set(mem, 0, (long)value.bits);
		else
		    message_type.set(mem, 0, (int)value.bits);
		return(this);
	    }
	    private static final VarHandle format = _XClientMessageEvent.varHandle(PathElement.groupElement("format"));
	    public int format() {return((int)format.get(mem, 0));}
	    public XClientMessageEvent format(int value) {format.set(mem, 0, value); return(this);}

	    private static final long data = _XClientMessageEvent.byteOffset(PathElement.groupElement("data"));
	    public byte[] b() {
		byte[] ret = new byte[20];
		for(int i = 0; i < ret.length; i++)
		    ret[i] = (byte)getint(mem, data + (i * C_CHAR.byteSize()), C_CHAR, true);
		return(ret);
	    }
	    public XClientMessageEvent b(byte[] value) {
		for(int i = 0; i < 20; i++)
		    setint(mem, data + (i * C_CHAR.byteSize()), C_CHAR, (i < value.length) ? value[i] : 0);
		return(this);
	    }
	    public XClientMessageEvent b(int i, byte value) {
		if((i < 0) || (i >= 20)) throw(new ArrayIndexOutOfBoundsException(i));
		setint(mem, data + (i * C_CHAR.byteSize()), C_CHAR, value);
		return(this);
	    }
	    public short[] s() {
		short[] ret = new short[10];
		for(int i = 0; i < ret.length; i++)
		    ret[i] = (short)getint(mem, data + (i * C_SHORT.byteSize()), C_SHORT, true);
		return(ret);
	    }
	    public XClientMessageEvent s(short[] value) {
		for(int i = 0; i < 10; i++)
		    setint(mem, data + (i * C_SHORT.byteSize()), C_SHORT, (i < value.length) ? value[i] : 0);
		return(this);
	    }
	    public XClientMessageEvent s(int i, short value) {
		if((i < 0) || (i >= 10)) throw(new ArrayIndexOutOfBoundsException(i));
		setint(mem, data + (i * C_SHORT.byteSize()), C_SHORT, value);
		return(this);
	    }
	    public long[] l() {
		long[] ret = new long[5];
		for(int i = 0; i < ret.length; i++)
		    ret[i] = (long)getint(mem, data + (i * C_LONG.byteSize()), C_LONG, true);
		return(ret);
	    }
	    public XClientMessageEvent l(long[] value) {
		for(int i = 0; i < 5; i++)
		    setint(mem, data + (i * C_LONG.byteSize()), C_LONG, (i < value.length) ? value[i] : 0);
		return(this);
	    }
	    public XClientMessageEvent l(int i, long value) {
		if((i < 0) || (i >= 5)) throw(new ArrayIndexOutOfBoundsException(i));
		setint(mem, data + (i * C_LONG.byteSize()), C_LONG, value);
		return(this);
	    }
	}

	static final StructLayout _XConfigureEvent = struct(new MemoryLayout[] {
		C_INT.withName("type"),
		C_LONG.withName("serial"),
		C_XBool.withName("send_event"),
		ADDRESS.withName("display"),
		C_XID.withName("event"),
		C_XID.withName("window"),
		C_INT.withName("x"),
		C_INT.withName("y"),
		C_INT.withName("width"),
		C_INT.withName("height"),
		C_INT.withName("border_width"),
		C_XID.withName("above"),
		C_XBool.withName("override_redirect"),
	    });
	public static class XConfigureEvent extends XLib.XConfigureEvent {
	    XConfigureEvent(MemorySegment mem) {
		super(mem);
	    }

	    protected StructLayout $layout() {return(_XConfigureEvent);}

	    private static final VarHandle event = _XConfigureEvent.varHandle(PathElement.groupElement("event"));
	    public XID event() {return(XID.of((long)event.get(mem, 0)));}
	    private static final VarHandle window = _XConfigureEvent.varHandle(PathElement.groupElement("window"));
	    public XID window() {return(XID.of((long)window.get(mem, 0)));}
	    private static final VarHandle x = _XConfigureEvent.varHandle(PathElement.groupElement("x"));
	    public int x() {return((int)x.get(mem, 0));}
	    private static final VarHandle y = _XConfigureEvent.varHandle(PathElement.groupElement("y"));
	    public int y() {return((int)y.get(mem, 0));}
	    private static final VarHandle width = _XConfigureEvent.varHandle(PathElement.groupElement("width"));
	    public int width() {return((int)width.get(mem, 0));}
	    private static final VarHandle height = _XConfigureEvent.varHandle(PathElement.groupElement("height"));
	    public int height() {return((int)height.get(mem, 0));}
	    private static final VarHandle border_width = _XConfigureEvent.varHandle(PathElement.groupElement("border_width"));
	    public int border_width() {return((int)border_width.get(mem, 0));}
	    private static final VarHandle above = _XConfigureEvent.varHandle(PathElement.groupElement("above"));
	    public XID above() {return(XID.of((long)above.get(mem, 0)));}
	    private static final VarHandle override_redirect = _XConfigureEvent.varHandle(PathElement.groupElement("override_redirect"));
	    public boolean override_redirect() {return(((int)override_redirect.get(mem, 0)) != 0);}
	}

	static final StructLayout _XSelectionClearEvent = struct(new MemoryLayout[] {
		C_INT.withName("type"),
		C_LONG.withName("serial"),
		C_XBool.withName("send_event"),
		ADDRESS.withName("display"),
		C_XID.withName("window"),
		C_Atom.withName("selection"),
		C_Time.withName("time"),
	    });
	public static class XSelectionClearEvent extends XLib.XSelectionClearEvent {
	    XSelectionClearEvent(MemorySegment mem) {
		super(mem);
	    }

	    protected StructLayout $layout() {return(_XSelectionClearEvent);}

	    private static final VarHandle window = _XSelectionClearEvent.varHandle(PathElement.groupElement("window"));
	    public XID window() {return(XID.of((long)window.get(mem, 0)));}
	    private static final VarHandle selection = _XSelectionClearEvent.varHandle(PathElement.groupElement("selection"));
	    public Atom selection() {return(Atom.of((long)selection.get(mem, 0)));}
	    private static final VarHandle time = _XSelectionClearEvent.varHandle(PathElement.groupElement("time"));
	    public long time() {return((long)time.get(mem, 0));}
	}

	static final StructLayout _XSelectionRequestEvent = struct(new MemoryLayout[] {
		C_INT.withName("type"),
		C_LONG.withName("serial"),
		C_XBool.withName("send_event"),
		ADDRESS.withName("display"),
		C_XID.withName("owner"),
		C_XID.withName("requestor"),
		C_Atom.withName("selection"),
		C_Atom.withName("target"),
		C_Atom.withName("property"),
		C_Time.withName("time"),
	    });
	public static class XSelectionRequestEvent extends XLib.XSelectionRequestEvent {
	    XSelectionRequestEvent(MemorySegment mem) {
		super(mem);
	    }

	    protected StructLayout $layout() {return(_XSelectionRequestEvent);}

	    private static final VarHandle owner = _XSelectionRequestEvent.varHandle(PathElement.groupElement("owner"));
	    public XID owner() {return(XID.of((long)owner.get(mem, 0)));}
	    private static final VarHandle requestor = _XSelectionRequestEvent.varHandle(PathElement.groupElement("requestor"));
	    public XID requestor() {return(XID.of((long)requestor.get(mem, 0)));}
	    private static final VarHandle selection = _XSelectionRequestEvent.varHandle(PathElement.groupElement("selection"));
	    public Atom selection() {return(Atom.of((long)selection.get(mem, 0)));}
	    private static final VarHandle target = _XSelectionRequestEvent.varHandle(PathElement.groupElement("target"));
	    public Atom target() {return(Atom.of((long)target.get(mem, 0)));}
	    private static final VarHandle property = _XSelectionRequestEvent.varHandle(PathElement.groupElement("property"));
	    public Atom property() {return(Atom.of((long)property.get(mem, 0)));}
	    private static final VarHandle time = _XSelectionRequestEvent.varHandle(PathElement.groupElement("time"));
	    public long time() {return((long)time.get(mem, 0));}
	}

	static final StructLayout _XSelectionEvent = struct(new MemoryLayout[] {
		C_INT.withName("type"),
		C_LONG.withName("serial"),
		C_XBool.withName("send_event"),
		ADDRESS.withName("display"),
		C_XID.withName("requestor"),
		C_Atom.withName("selection"),
		C_Atom.withName("target"),
		C_Atom.withName("property"),
		C_Time.withName("time"),
	    });
	public static class XSelectionEvent extends XLib.XSelectionEvent {
	    XSelectionEvent(MemorySegment mem) {
		super(mem);
	    }

	    protected StructLayout $layout() {return(_XSelectionEvent);}

	    private static final VarHandle requestor = _XSelectionEvent.varHandle(PathElement.groupElement("requestor"));
	    public XID requestor() {return(XID.of((long)requestor.get(mem, 0)));}
	    public XSelectionEvent requestor(XID value) {set(mem, requestor, 0, value); return(this);}
	    private static final VarHandle selection = _XSelectionEvent.varHandle(PathElement.groupElement("selection"));
	    public Atom selection() {return(Atom.of((long)selection.get(mem, 0)));}
	    public XSelectionEvent selection(Atom value) {set(mem, selection, 0, value); return(this);}
	    private static final VarHandle target = _XSelectionEvent.varHandle(PathElement.groupElement("target"));
	    public Atom target() {return(Atom.of((long)target.get(mem, 0)));}
	    public XSelectionEvent target(Atom value) {set(mem, target, 0, value); return(this);}
	    private static final VarHandle property = _XSelectionEvent.varHandle(PathElement.groupElement("property"));
	    public Atom property() {return(Atom.of((long)property.get(mem, 0)));}
	    public XSelectionEvent property(Atom value) {set(mem, property, 0, value); return(this);}
	    private static final VarHandle time = _XSelectionEvent.varHandle(PathElement.groupElement("time"));
	    public long time() {return((long)time.get(mem, 0));}
	    public XSelectionEvent time(long value) {settime(mem, time, 0, value); return(this);}
	}

	static final StructLayout _XGenericEventCookie = struct(new MemoryLayout[] {
		C_INT.withName("type"),
		C_LONG.withName("serial"),
		C_XBool.withName("send_event"),
		ADDRESS.withName("display"),
		C_INT.withName("extension"),
		C_INT.withName("evtype"),
		C_INT.withName("cookie"),
		ADDRESS.withName("data"),
	    });
	public static class XGenericEvent extends XLib.XGenericEvent {
	    final libX11_so_6 xlib;
	    private MemorySegment evdata;
	    private boolean fetched;

	    XGenericEvent(libX11_so_6 xlib, MemorySegment mem) {
		super(mem);
		this.xlib = xlib;
	    }

	    protected StructLayout $layout() {return(_XGenericEventCookie);}

	    private static final VarHandle extension = _XGenericEventCookie.varHandle(PathElement.groupElement("extension"));
	    public int extension() {return((int)extension.get(mem, 0));}
	    private static final VarHandle evtype = _XGenericEventCookie.varHandle(PathElement.groupElement("evtype"));
	    public int evtype() {return((int)evtype.get(mem, 0));}
	    private static final VarHandle cookie = _XGenericEventCookie.varHandle(PathElement.groupElement("cookie"));
	    public int cookie() {return((int)cookie.get(mem, 0));}

	    private static final VarHandle display = _XGenericEventCookie.varHandle(PathElement.groupElement("display"));
	    private static final VarHandle data = _XGenericEventCookie.varHandle(PathElement.groupElement("data"));
	    public MemorySegment data() {
		if(!fetched) {
		    fetched = true;
		    libX11_so_6 xlib = this.xlib;
		    MemorySegment mem = this.mem;
		    MemorySegment dpy = (MemorySegment)display.get(mem, 0);
		    if(xlib.XGetEventData(dpy, mem))
			evdata = (MemorySegment)data.get(mem, 0);
		    Finalizer.finalize(this, () -> xlib.XFreeEventData(dpy, mem));
		}
		return(evdata);
	    }
	}

	static final StructLayout _XComposeStatus = struct(new MemoryLayout[] {
		C_XPointer.withName("compose_ptr"),
		C_INT.withName("chars_matched"),
	    });
	public static class XComposeStatus extends XLib.XComposeStatus {
	    XComposeStatus(MemorySegment mem) {
		super(mem);
	    }

	    XComposeStatus() {
		super(Arena.ofAuto().allocate(_XComposeStatus));
	    }

	    protected StructLayout $layout() {return(_XComposeStatus);}
	}

	static final StructLayout _XIMStyles = struct(new MemoryLayout[] {
		C_SHORT.withName("count_styles"),
		ADDRESS.withName("supported_styles")
	    });
	public static class XIMStyles extends XLib.XIMStyles {
	    XIMStyles(libX11_so_6 xlib, MemorySegment mem) {
		super(mem);
		Finalizer.finalize(this, () -> xlib.XFree(mem));
	    }

	    protected StructLayout $layout() {return(_XIMStyles);}

	    private static final VarHandle count_styles = _XIMStyles.varHandle(PathElement.groupElement("count_styles"));
	    public int count_styles() {return((short)count_styles.get(mem, 0));}
	    private static final VarHandle supported_styles = _XIMStyles.varHandle(PathElement.groupElement("supported_styles"));
	    public MemorySegment supported_styles() {return((MemorySegment)supported_styles.get(mem, 0));}

	    public long[] styles() {
		int n = count_styles();
		MemorySegment sl = supported_styles().reinterpret(ADDRESS.byteSize() * n);
		long[] ret = new long[n];
		for(int i = 0; i < n; i++)
		    ret[i] = getint(sl, C_XIMStyle.byteSize() * i, C_XIMStyle, false);
		return(ret);
	    }
	}

	private static final StructLayout _Display = struct(new MemoryLayout[] {
		ADDRESS.withName("ext_data"),
		ADDRESS.withName("private1"),
		C_INT.withName("fd"),
		C_INT.withName("private2"),
		C_INT.withName("proto_major_version"),
		C_INT.withName("proto_minor_version"),
		ADDRESS.withName("vendor"),
		C_XID.withName("private3"),
		C_XID.withName("private4"),
		C_XID.withName("private5"),
		C_INT.withName("private6"),
		ADDRESS.withName("resource_alloc"),
		C_INT.withName("byte_order"),
		C_INT.withName("bitmap_unit"),
		C_INT.withName("bitmap_pad"),
		C_INT.withName("bitmap_bit_order"),
		C_INT.withName("nformats"),
		ADDRESS.withName("pixmap_format"),
		C_INT.withName("private8"),
		C_INT.withName("release"),
		ADDRESS.withName("private9"),
		ADDRESS.withName("private10"),
		C_INT.withName("qlen"),
		C_LONG.withName("last_request_read"),
		C_LONG.withName("request"),
		ADDRESS.withName("private11"),
		ADDRESS.withName("private12"),
		ADDRESS.withName("private13"),
		ADDRESS.withName("private14"),
		C_INT.withName("max_request_size"),
		ADDRESS.withName("db"),
		ADDRESS.withName("private15"),
		ADDRESS.withName("display_name"),
		C_INT.withName("default_screen"),
		C_INT.withName("nscreens"),
		ADDRESS.withName("screens"),
		C_LONG.withName("motion_buffer"),
		C_LONG.withName("private16"),
		C_INT.withName("min_keycode"),
		C_INT.withName("max_keycode"),
		ADDRESS.withName("private17"),
		ADDRESS.withName("private18"),
		C_INT.withName("private19"),
		ADDRESS.withName("xdefaults"),
	    });
	public static class Display extends XLib.Display {
	    private Display(XLib lib, MemorySegment mem) {
		super(lib, mem);
	    }

	    protected StructLayout $layout() {return(_Display);}

	    private static final VarHandle fd = _Display.varHandle(PathElement.groupElement("fd"));
	    public int fd() {return((int)fd.get(mem, 0));}

	    private static final VarHandle default_screen = _Display.varHandle(PathElement.groupElement("default_screen"));
	    public int default_screen() {return((int)default_screen.get(mem, 0));}

	    private static final VarHandle nscreens = _Display.varHandle(PathElement.groupElement("nscreens"));
	    public int nscreens() {return((int)nscreens.get(mem, 0));}

	    private static final VarHandle screens = _Display.varHandle(PathElement.groupElement("screens"));
	    public MemorySegment screens() {return((MemorySegment)screens.get(mem, 0));}

	    public List<Screen> lscreens() {return(new MemArray<Screen>(screens(), _Screen, nscreens(), Screen::new));}
	}

	public static final StructLayout _Screen = struct(new MemoryLayout[] {
	    ADDRESS.withName("ext_data"),
	    ADDRESS.withName("display"),
	    C_XID.withName("root"),
	    C_INT.withName("width"),
	    C_INT.withName("height"),
	    C_INT.withName("mwidth"),
	    C_INT.withName("mheight"),
	    C_INT.withName("ndepths"),
	    ADDRESS.withName("depths"),
	    C_INT.withName("root_depth"),
	    ADDRESS.withName("root_visual"),
	    ADDRESS.withName("default_gc"),
	    C_XID.withName("cmap"),
	    C_LONG.withName("white_pixel"),
	    C_LONG.withName("black_pixel"),
	    C_INT.withName("max_maps"),
	    C_INT.withName("min_maps"),
	    C_INT.withName("backing_store"),
	    C_XBool.withName("save_udners"),
	    C_LONG.withName("root_input_mask"),
	});
	public static class Screen extends XLib.Screen {
	    private Screen(MemorySegment mem) {
		super(mem);
	    }

	    protected StructLayout $layout() {return(_Screen);}

	    private static final VarHandle root = _Screen.varHandle(PathElement.groupElement("root"));
	    public XID root() {return(XID.of((long)root.get(mem, 0)));}
	}

	private final MethodHandle XInitThreads = ld.downcallHandle(xlib.find("XInitThreads").get(), FunctionDescriptor.of(C_Status));
	public int XInitThreads() {
	    try {
		return((int)XInitThreads.invoke());
	    } catch(Throwable e) {
		throw(new RuntimeException(e));
	    } finally {
		checkerror();
	    }
	}

	private final MethodHandle XSetErrorHandler = ld.downcallHandle(xlib.find("XSetErrorHandler").get(), FunctionDescriptor.of(ADDRESS, ADDRESS));
	public MemorySegment XSetErrorHandler(MemorySegment handler) {
	    try {
		return((MemorySegment)XSetErrorHandler.invoke(handler));
	    } catch(Throwable e) {
		throw(new RuntimeException(e));
	    } finally {
		checkerror();
	    }
	}

	private final MethodHandle XSetIOErrorHandler = ld.downcallHandle(xlib.find("XSetIOErrorHandler").get(), FunctionDescriptor.of(ADDRESS, ADDRESS));
	public MemorySegment XSetIOErrorHandler(MemorySegment handler) {
	    try {
		return((MemorySegment)XSetIOErrorHandler.invoke(handler));
	    } catch(Throwable e) {
		throw(new RuntimeException(e));
	    } finally {
		checkerror();
	    }
	}

	private final MethodHandle XSetIOErrorExitHandler = ld.downcallHandle(xlib.find("XSetIOErrorExitHandler").get(), FunctionDescriptor.of(ADDRESS, ADDRESS, ADDRESS, ADDRESS));
	public MemorySegment XSetIOErrorExitHandler(XLib.Display dpy, MemorySegment handler, MemorySegment userdata) {
	    try {
		return((MemorySegment)XSetIOErrorExitHandler.invoke(dpy.mem(), handler, userdata));
	    } catch(Throwable e) {
		throw(new RuntimeException(e));
	    } finally {
		checkerror();
	    }
	}

	private MemorySegment handlerp;
	public void XSetErrorHandler() {
	    {
		MethodHandle handler = slookup(MethodHandles.lookup(), XLib.class, "handleerror", Integer.TYPE, XLib.class, MemorySegment.class, MemorySegment.class);
		handler = MethodHandles.insertArguments(handler, 0, this);
		handlerp = ld.upcallStub(handler, FunctionDescriptor.of(C_INT, ADDRESS, ADDRESS), Arena.ofAuto());
		XSetErrorHandler(handlerp);
	    }
	}

	private final MethodHandle XOpenDisplay = ld.downcallHandle(xlib.find("XOpenDisplay").get(), FunctionDescriptor.of(ADDRESS.withTargetLayout(_Display), ADDRESS));
	public Display XOpenDisplay(String name) {
	    MemorySegment ret;
	    try {
		if(name == null) {
		    ret = (MemorySegment)XOpenDisplay.invoke(MemorySegment.NULL);
		} else {
		    try(Arena st = Arena.ofConfined()) {
			ret = (MemorySegment)XOpenDisplay.invoke(st.allocateFrom(name, C_CHARSET));
		    }
		}
	    } catch(Throwable e) {
		throw(new RuntimeException(e));
	    } finally {
		checkerror();
	    }
	    return(nullp(ret) ? null : new Display(this, ret));
	}

	private final MethodHandle XCloseDisplay = ld.downcallHandle(xlib.find("XCloseDisplay").get(), FunctionDescriptor.of(C_INT, ADDRESS));
	public int XCloseDisplay(XLib.Display dpy) {
	    try {
		dpy.closed = true;
		return((int)XCloseDisplay.invoke(dpy.mem()));
	    } catch(Throwable e) {
		throw(new RuntimeException(e));
	    } finally {
		checkerror();
	    }
	}

	private final MethodHandle XInternAtom = ld.downcallHandle(xlib.find("XInternAtom").get(), FunctionDescriptor.of(C_Atom, ADDRESS, ADDRESS, C_XBool));
	public Atom XInternAtom(XLib.Display dpy, String name, boolean only_if_exists) {
	    try(Arena st = Arena.ofConfined()) {
		MemorySegment stname = st.allocateFrom(name);
		try {
		    if(C_Atom instanceof ValueLayout.OfLong)
			return(Atom.of((long)XInternAtom.invoke(dpy.mem(), stname, only_if_exists ? 1 : 0)));
		    else
			return(Atom.of((int)XInternAtom.invoke(dpy.mem(), stname, only_if_exists ? 1 : 0)));
		} catch(Throwable e) {
		    throw(new RuntimeException(e));
		} finally {
		    checkerror();
		}
	    }
	}

	private final MethodHandle XInternAtoms = ld.downcallHandle(xlib.find("XInternAtoms").get(), FunctionDescriptor.of(C_Status, ADDRESS, ADDRESS, C_INT, C_XBool, ADDRESS));
	public Atom[] XInternAtoms(XLib.Display dpy, String[] names, boolean only_if_exists) {
	    try(Arena st = Arena.ofConfined()) {
		MemorySegment nary = st.allocate(ADDRESS, names.length);
		MemorySegment rary = st.allocate(C_Atom, names.length);
		for(int i = 0; i < names.length; i++)
		    nary.set(ADDRESS, ADDRESS.byteSize() * i, st.allocateFrom(names[i]));
		try {
		    if((int)XInternAtoms.invoke(dpy.mem(), nary, names.length, only_if_exists ? 1 : 0, rary) == 0)
			return(null);
		} catch(Throwable e) {
		    throw(new RuntimeException(e));
		} finally {
		    checkerror();
		}
		Atom[] ret = new Atom[names.length];
		for(int i = 0; i < names.length; i++)
		    ret[i] = Atom.of(getint(rary, C_Atom.byteSize() * i, C_Atom, false));
		return(ret);
	    }
	}

	private final MethodHandle XFree = ld.downcallHandle(xlib.find("XFree").get(), FunctionDescriptor.of(C_INT, ADDRESS));
	public int XFree(MemorySegment mem) {
	    try {
		return((int)XFree.invoke(mem));
	    } catch(Throwable e) {
		throw(new RuntimeException(e));
	    } finally {
		checkerror();
	    }
	}

	private final MethodHandle XGetAtomName = ld.downcallHandle(xlib.find("XGetAtomName").get(), FunctionDescriptor.of(ADDRESS, ADDRESS, C_Atom));
	public String XGetAtomName(XLib.Display dpy, Atom atom) {
	    if(atom.bits == 0)
		return(null);
	    MemorySegment name;
	    try {
		if(C_Atom instanceof ValueLayout.OfLong)
		    name = (MemorySegment)XGetAtomName.invoke(dpy.mem(), (long)atom.bits);
		else
		    name = (MemorySegment)XGetAtomName.invoke(dpy.mem(), (int)atom.bits);
	    } catch(Throwable e) {
		throw(new RuntimeException(e));
	    } finally {
		checkerror();
	    }
	    String ret = name.reinterpret(Long.MAX_VALUE).getString(0, C_CHARSET);
	    XFree(name);
	    return(ret);
	}

	private final MethodHandle XServerVendor = ld.downcallHandle(xlib.find("XServerVendor").get(), FunctionDescriptor.of(ADDRESS, ADDRESS));
	public String XServerVendor(XLib.Display dpy) {
	    MemorySegment name;
	    try {
		name = (MemorySegment)XServerVendor.invoke(dpy.mem());
	    } catch(Throwable e) {
		throw(new RuntimeException(e));
	    } finally {
		checkerror();
	    }
	    String ret = name.reinterpret(Long.MAX_VALUE).getString(0, C_CHARSET);
	    return(ret);
	}

	private final MethodHandle XVendorRelease = ld.downcallHandle(xlib.find("XVendorRelease").get(), FunctionDescriptor.of(C_INT, ADDRESS));
	public int XVendorRelease(XLib.Display dpy) {
	    try {
		return((int)XVendorRelease.invoke(dpy.mem()));
	    } catch(Throwable e) {
		throw(new RuntimeException(e));
	    } finally {
		checkerror();
	    }
	}

	private final MethodHandle XQueryExtension = ld.downcallHandle(xlib.find("XQueryExtension").get(), FunctionDescriptor.of(C_XBool, ADDRESS, ADDRESS, ADDRESS, ADDRESS, ADDRESS));
	public ExtensionInfo XQueryExtension(XLib.Display dpy, String name) {
	    try(Arena st = Arena.ofConfined()) {
		MemorySegment nnm = st.allocateFrom(name);
		MemorySegment opcode = st.allocate(C_INT), event = st.allocate(C_INT), error = st.allocate(C_INT);
		int rv;
		try {
		    rv = (int)XQueryExtension.invoke(dpy.mem(), nnm, opcode, event, error);
		} catch(Throwable e) {
		    throw(new RuntimeException(e));
		} finally {
		    checkerror();
		}
		if(rv == 0)
		    return(null);
		return(new ExtensionInfo(name, (int)getint(opcode, 0, C_INT, true), (int)getint(event, 0, C_INT, true), (int)getint(error, 0, C_INT, true)));
	    }
	}

	private final MethodHandle XCreateColormap = ld.downcallHandle(xlib.find("XCreateColormap").get(), FunctionDescriptor.of(C_XID, ADDRESS, C_XID, ADDRESS, C_INT));
	public XID XCreateColormap(XLib.Display dpy, XID w, XLib.Visual visual, int alloc) {
	    try {
		if(C_XID instanceof ValueLayout.OfLong)
		    return(XID.of((long)XCreateColormap.invoke(dpy.mem(), (long)w.bits, visual.mem(), alloc)));
		else
		    return(XID.of((int)XCreateColormap.invoke(dpy.mem(), (int)w.bits, visual.mem(), alloc)));
	    } catch(Throwable e) {
		throw(new RuntimeException(e));
	    } finally {
		checkerror();
	    }
	}

	private final MethodHandle XQueryBestCursor = ld.downcallHandle(xlib.find("XQueryBestCursor").get(), FunctionDescriptor.of(C_Status, ADDRESS, C_XID, C_INT, C_INT, ADDRESS, ADDRESS));
	public Coord XQueryBestCursor(XLib.Display dpy, XID d, Coord size) {
	    try(Arena st = Arena.ofConfined()) {
		MemorySegment x = st.allocate(C_INT), y = st.allocate(C_INT);
		try {
		    if(C_XID instanceof ValueLayout.OfLong)
			XQueryBestCursor.invoke(dpy.mem(), (long)d.bits, size.x, size.y, x, y);
		    else
			XQueryBestCursor.invoke(dpy.mem(), (int)d.bits, size.x, size.y, x, y);
		} catch(Throwable e) {
		    throw(new RuntimeException(e));
		} finally {
		    checkerror();
		}
		return(Coord.of((int)getint(x, 0, C_INT, false), (int)getint(y, 0, C_INT, false)));
	    }
	}

	private final MethodHandle XDefineCursor = ld.downcallHandle(xlib.find("XDefineCursor").get(), FunctionDescriptor.of(C_INT, ADDRESS, C_XID, C_XID));
	public void XDefineCursor(XLib.Display dpy, XID w, XID cursor) {
	    try {
		if(C_XID instanceof ValueLayout.OfLong)
		    XDefineCursor.invoke(dpy.mem(), (long)w.bits, (long)cursor.bits);
		else
		    XDefineCursor.invoke(dpy.mem(), (int)w.bits, (int)cursor.bits);
	    } catch(Throwable e) {
		throw(new RuntimeException(e));
	    } finally {
		checkerror();
	    }
	}

	private final MethodHandle XFreeCursor = ld.downcallHandle(xlib.find("XFreeCursor").get(), FunctionDescriptor.of(C_INT, ADDRESS, C_XID));
	public void XFreeCursor(XLib.Display dpy, XID cursor) {
	    try {
		if(C_XID instanceof ValueLayout.OfLong)
		    XFreeCursor.invoke(dpy.mem(), (long)cursor.bits);
		else
		    XFreeCursor.invoke(dpy.mem(), (int)cursor.bits);
	    } catch(Throwable e) {
		throw(new RuntimeException(e));
	    } finally {
		checkerror();
	    }
	}

	private final MethodHandle XCreateWindow = ld.downcallHandle(xlib.find("XCreateWindow").get(), FunctionDescriptor.of(C_XID, ADDRESS, C_XID, C_INT, C_INT, C_INT, C_INT, C_INT, C_INT, C_INT, ADDRESS, C_LONG, ADDRESS));
	public XID XCreateWindow(XLib.Display dpy, XID parent, int x, int y, int width, int height, int border_width, int depth, int cl, XLib.Visual visual, long valuemask, XLib.XSetWindowAttributes attributes) {
	    try {
		if(C_XID instanceof ValueLayout.OfLong)
		    return(XID.of((long)XCreateWindow.invoke(dpy.mem(), (long)parent.bits, x, y, width, height, border_width, depth, cl, visual.mem(), valuemask, attributes.mem())));
		else
		    return(XID.of((int)XCreateWindow.invoke(dpy.mem(), (int)parent.bits, x, y, width, height, border_width, depth, cl, visual.mem(), valuemask, attributes.mem())));
	    } catch(Throwable e) {
		throw(new RuntimeException(e));
	    } finally {
		checkerror();
	    }
	}

	private final MethodHandle XDestroyWindow = ld.downcallHandle(xlib.find("XDestroyWindow").get(), FunctionDescriptor.of(C_INT, ADDRESS, C_XID));
	public int XDestroyWindow(XLib.Display dpy, XID w) {
	    try {
		if(C_XID instanceof ValueLayout.OfLong)
		    return((int)XDestroyWindow.invoke(dpy.mem(), (long)w.bits));
		else
		    return((int)XDestroyWindow.invoke(dpy.mem(), (int)w.bits));
	    } catch(Throwable e) {
		throw(new RuntimeException(e));
	    } finally {
		checkerror();
	    }
	}

	private final MethodHandle XDisplayKeycodes = ld.downcallHandle(xlib.find("XDisplayKeycodes").get(), FunctionDescriptor.of(C_INT, ADDRESS, ADDRESS, ADDRESS));
	public int[] XDisplayKeycodes(XLib.Display dpy) {
	    try(Arena st = Arena.ofConfined()) {
		MemorySegment minbuf = st.allocate(C_INT), maxbuf = st.allocate(C_INT);
		try {
		    XDisplayKeycodes.invoke(dpy.mem(), minbuf, maxbuf);
		} catch(Throwable e) {
		    throw(new RuntimeException(e));
		} finally {
		    checkerror();
		}
		return(new int[] {(int)getint(minbuf, 0, C_INT, true), (int)getint(maxbuf, 0, C_INT, true)});
	    }
	}

	private final MethodHandle XGetKeyboardMapping = ld.downcallHandle(xlib.find("XGetKeyboardMapping").get(), FunctionDescriptor.of(ADDRESS, ADDRESS, C_KeyCode, C_INT, ADDRESS));
	public XID[][] XGetKeyboardMapping(XLib.Display dpy, int first_keycode, int keycode_count) {
	    try(Arena st = Arena.ofConfined()) {
		MemorySegment ret, nbuf = st.allocate(C_INT);
		try {
		    ret = (MemorySegment)XGetKeyboardMapping.invoke(dpy.mem(), (byte)first_keycode, keycode_count, nbuf);
		} catch(Throwable e) {
		    throw(new RuntimeException(e));
		} finally {
		    checkerror();
		}
		if(nullp(ret))
		    return(null);
		int spk = (int)getint(nbuf, 0, C_INT, false);
		ret = ret.reinterpret(spk * keycode_count * C_XID.byteSize());
		XID[][] buf = new XID[keycode_count][];
		for(int i = 0; i < keycode_count; i++) {
		    XID[] kbuf = new XID[spk];
		    int n = 0;
		    for(int o = 0; o < spk; o++) {
			long s = getint(ret, C_XID.byteSize() * ((i * spk) + o), C_XID, false);
			if(s != 0)
			    kbuf[n++] = XID.of(s);
		    }
		    buf[i] = Arrays.copyOf(kbuf, n);
		}
		XFree(ret);
		return(buf);
	    }
	}

	private final MethodHandle XGetModifierMapping = ld.downcallHandle(xlib.find("XGetModifierMapping").get(), FunctionDescriptor.of(ADDRESS, ADDRESS));
	public XModifierMapping XGetModifierMapping(XLib.Display dpy) {
	    MemorySegment ret;
	    try {
		ret = (MemorySegment)XGetModifierMapping.invoke(dpy.mem());
	    } catch(Throwable e) {
		throw(new RuntimeException(e));
	    } finally {
		checkerror();
	    }
	    return(new XModifierMapping(this, ret));
	}

	private final MethodHandle XFreeModifiermap = ld.downcallHandle(xlib.find("XFreeModifiermap").get(), FunctionDescriptor.of(C_INT, ADDRESS));
	public int XFreeModifiermap(MemorySegment mem) {
	    try {
		return((int)XFreeModifiermap.invoke(mem));
	    } catch(Throwable e) {
		throw(new RuntimeException(e));
	    } finally {
		checkerror();
	    }
	}

	private final MethodHandle XSelectInput = ld.downcallHandle(xlib.find("XSelectInput").get(), FunctionDescriptor.of(C_INT, ADDRESS, C_XID, C_LONG));
	public int XSelectInput(XLib.Display dpy, XID w, long event_mask) {
	    try {
		if(C_XID instanceof ValueLayout.OfLong)
		    return((int)XSelectInput.invoke(dpy.mem(), (long)w.bits, event_mask));
		else
		    return((int)XSelectInput.invoke(dpy.mem(), (int)w.bits, event_mask));
	    } catch(Throwable e) {
		throw(new RuntimeException(e));
	    } finally {
		checkerror();
	    }
	}

	private final MethodHandle XFilterEvent = ld.downcallHandle(xlib.find("XFilterEvent").get(), FunctionDescriptor.of(C_XBool, ADDRESS, C_XID));
	public boolean XFilterEvent(XLib.XEvent ev, XID w) {
	    try {
		if(C_XID instanceof ValueLayout.OfLong)
		    return((int)XFilterEvent.invoke(ev.mem(), (long)w.bits) != 0);
		else
		    return((int)XFilterEvent.invoke(ev.mem(), (int)w.bits) != 0);
	    } catch(Throwable e) {
		throw(new RuntimeException(e));
	    } finally {
		checkerror();
	    }
	}

	private final MethodHandle XUnmapWindow = ld.downcallHandle(xlib.find("XUnmapWindow").get(), FunctionDescriptor.of(C_INT, ADDRESS, C_XID));
	public int XUnmapWindow(XLib.Display dpy, XID w) {
	    try {
		if(C_XID instanceof ValueLayout.OfLong)
		    return((int)XUnmapWindow.invoke(dpy.mem(), (long)w.bits));
		else
		    return((int)XUnmapWindow.invoke(dpy.mem(), (int)w.bits));
	    } catch(Throwable e) {
		throw(new RuntimeException(e));
	    } finally {
		checkerror();
	    }
	}

	private final MethodHandle XMapWindow = ld.downcallHandle(xlib.find("XMapWindow").get(), FunctionDescriptor.of(C_INT, ADDRESS, C_XID));
	public int XMapWindow(XLib.Display dpy, XID w) {
	    try {
		if(C_XID instanceof ValueLayout.OfLong)
		    return((int)XMapWindow.invoke(dpy.mem(), (long)w.bits));
		else
		    return((int)XMapWindow.invoke(dpy.mem(), (int)w.bits));
	    } catch(Throwable e) {
		throw(new RuntimeException(e));
	    } finally {
		checkerror();
	    }
	}

	private final MethodHandle XMapRaised = ld.downcallHandle(xlib.find("XMapRaised").get(), FunctionDescriptor.of(C_INT, ADDRESS, C_XID));
	public int XMapRaised(XLib.Display dpy, XID w) {
	    try {
		if(C_XID instanceof ValueLayout.OfLong)
		    return((int)XMapRaised.invoke(dpy.mem(), (long)w.bits));
		else
		    return((int)XMapRaised.invoke(dpy.mem(), (int)w.bits));
	    } catch(Throwable e) {
		throw(new RuntimeException(e));
	    } finally {
		checkerror();
	    }
	}

	private final MethodHandle XConfigureWindow = ld.downcallHandle(xlib.find("XConfigureWindow").get(), FunctionDescriptor.of(C_INT, ADDRESS, C_XID, C_INT, ADDRESS));
	public int XConfigureWindow(XLib.Display dpy, XID w, int valuemask, XLib.XWindowChanges values) {
	    try {
		if(C_XID instanceof ValueLayout.OfLong)
		    return((int)XConfigureWindow.invoke(dpy.mem(), (long)w.bits, valuemask, values.mem()));
		else
		    return((int)XConfigureWindow.invoke(dpy.mem(), (int)w.bits, valuemask, values.mem()));
	    } catch(Throwable e) {
		throw(new RuntimeException(e));
	    } finally {
		checkerror();
	    }
	}	

	private final MethodHandle XTranslateCoordinates = ld.downcallHandle(xlib.find("XTranslateCoordinates").get(), FunctionDescriptor.of(C_INT, ADDRESS, C_XID, C_XID, C_INT, C_INT, ADDRESS, ADDRESS, ADDRESS));
	public Coord XTranslateCoordinates(XLib.Display dpy, XID src_w, XID dest_w, Coord src) {
	    try(Arena st = Arena.ofConfined()) {
		MemorySegment xbuf = st.allocate(C_INT), ybuf = st.allocate(C_INT), wbuf = st.allocate(C_XID);
		int rv;
		try {
		    if(C_XID instanceof ValueLayout.OfLong)
			rv = (int)XTranslateCoordinates.invoke(dpy.mem(), (long)src_w.bits, (long)dest_w.bits, src.x, src.y, xbuf, ybuf, wbuf);
		    else
			rv = (int)XTranslateCoordinates.invoke(dpy.mem(), (int)src_w.bits, (int)dest_w.bits, src.x, src.y, xbuf, ybuf, wbuf);
		} catch(Throwable e) {
		    throw(new RuntimeException(e));
		} finally {
		    checkerror();
		}
		if(rv == 0)
		    return(null);
		return(Coord.of((int)getint(xbuf, 0, C_INT, true), (int)getint(ybuf, 0, C_INT, true)));
	    }
	}

	private final MethodHandle XGetWindowProperty = ld.downcallHandle(xlib.find("XGetWindowProperty").get(), FunctionDescriptor.of(C_INT, ADDRESS, C_XID, C_Atom, C_LONG, C_LONG, C_XBool, C_Atom, ADDRESS, ADDRESS, ADDRESS, ADDRESS, ADDRESS));
	public XProperty XGetWindowProperty(XLib.Display dpy, XID w, Atom property, boolean delete, Atom reg_type) {
	    try(Arena st = Arena.ofConfined()) {
		MemorySegment tbuf = st.allocate(C_Atom), fbuf = st.allocate(C_INT), nbuf = st.allocate(C_LONG), rbuf = st.allocate(C_LONG), dbuf = st.allocate(ADDRESS);
		int stat;
		try {
		    if(C_XID instanceof ValueLayout.OfLong)
			stat = (int)XGetWindowProperty.invoke(dpy.mem(), (long)w.bits, (long)property.bits, 0, 1 << 24, delete ? 1 : 0, (long)reg_type.bits, tbuf, fbuf, nbuf, rbuf, dbuf);
		    else
			stat = (int)XGetWindowProperty.invoke(dpy.mem(), (int)w.bits, (int)property.bits, 0, 1 << 24, delete ? 1 : 0, (int)reg_type.bits, tbuf, fbuf, nbuf, rbuf, dbuf);
		} catch(Throwable e) {
		    throw(new RuntimeException(e));
		} finally {
		    checkerror();
		}
		if(stat != Success)
		    return(null);
		MemorySegment data = dbuf.get(ADDRESS, 0);
		if(getint(rbuf, 0, C_LONG, false) != 0) {
		    XFree(data);
		    throw(new XLibException("huge properties are not supported yet"));
		}
		int format = (int)getint(fbuf, 0, C_INT, true);
		int len = (int)getint(nbuf, 0, C_LONG, false);
		if(format == 8)
		    data = data.reinterpret(len * C_CHAR.byteSize());
		else if(format == 16)
		    data = data.reinterpret(len * C_SHORT.byteSize());
		else if(format == 32)
		    data = data.reinterpret(len * C_LONG.byteSize());
		XProperty ret = new XProperty(dpy, property, Atom.of(getint(tbuf, 0, C_Atom, false)), format, len, data);
		MemorySegment jpbarda = data;
		Finalizer.finalize(ret, () -> XFree(jpbarda));
		return(ret);
	    }
	}

	private final MethodHandle XChangeProperty = ld.downcallHandle(xlib.find("XChangeProperty").get(), FunctionDescriptor.of(C_INT, ADDRESS, C_XID, C_Atom, C_Atom, C_INT, C_INT, ADDRESS, C_INT));
	public int XChangeProperty(XLib.Display dpy, XID w, Atom property, Atom type, int mode, byte[] data) {
	    try(Arena st = Arena.ofConfined()) {
		MemorySegment buf = memcpy(st.allocate(data.length), data);
		try {
		    if(C_XID instanceof ValueLayout.OfLong)
			return((int)XChangeProperty.invoke(dpy.mem(), (long)w.bits, (long)property.bits, (long)type.bits, 8, mode, buf, data.length));
		    else
			return((int)XChangeProperty.invoke(dpy.mem(), (int)w.bits, (int)property.bits, (int)type.bits, 8, mode, buf, data.length));
		} catch(Throwable e) {
		    throw(new RuntimeException(e));
		} finally {
		    checkerror();
		}
	    }
	}

	public int XChangeProperty(XLib.Display dpy, XID w, Atom property, Atom type, int mode, short[] data) {
	    try(Arena st = Arena.ofConfined()) {
		MemorySegment buf = memcpy(st.allocate(C_SHORT.byteSize() * data.length), data);
		try {
		    if(C_XID instanceof ValueLayout.OfLong)
			return((int)XChangeProperty.invoke(dpy.mem(), (long)w.bits, (long)property.bits, (long)type.bits, 16, mode, buf, data.length));
		    else
			return((int)XChangeProperty.invoke(dpy.mem(), (int)w.bits, (int)property.bits, (int)type.bits, 16, mode, buf, data.length));
		} catch(Throwable e) {
		    throw(new RuntimeException(e));
		} finally {
		    checkerror();
		}
	    }
	}

	public int XChangeProperty(XLib.Display dpy, XID w, Atom property, Atom type, int mode, long[] data) {
	    try(Arena st = Arena.ofConfined()) {
		MemorySegment buf = memcpy(st.allocate(C_LONG.byteSize() * data.length), data);
		try {
		    if(C_XID instanceof ValueLayout.OfLong)
			return((int)XChangeProperty.invoke(dpy.mem(), (long)w.bits, (long)property.bits, (long)type.bits, 32, mode, buf, data.length));
		    else
			return((int)XChangeProperty.invoke(dpy.mem(), (int)w.bits, (int)property.bits, (int)type.bits, 32, mode, buf, data.length));
		} catch(Throwable e) {
		    throw(new RuntimeException(e));
		} finally {
		    checkerror();
		}
	    }
	}

	private final MethodHandle XDeleteProperty = ld.downcallHandle(xlib.find("XDeleteProperty").get(), FunctionDescriptor.of(C_INT, ADDRESS, C_XID, C_Atom));
	public int XDeleteProperty(XLib.Display dpy, XID w, Atom property) {
	    try {
		if(C_XID instanceof ValueLayout.OfLong)
		    return((int)XDeleteProperty.invoke(dpy.mem(), (long)w.bits, (long)property.bits));
		else
		    return((int)XDeleteProperty.invoke(dpy.mem(), (int)w.bits, (int)property.bits));
	    } catch(Throwable e) {
		throw(new RuntimeException(e));
	    } finally {
		checkerror();
	    }
	}

	private final MethodHandle XStoreName = ld.downcallHandle(xlib.find("XStoreName").get(), FunctionDescriptor.of(C_INT, ADDRESS, C_XID, ADDRESS));
	public int XStoreName(XLib.Display dpy, XID w, String window_name) {
	    try(Arena st = Arena.ofConfined()) {
		MemorySegment name = st.allocateFrom(window_name, Utils.utf8);
		try {
		    if(C_XID instanceof ValueLayout.OfLong)
			return((int)XStoreName.invoke(dpy.mem(), (long)w.bits, name));
		    else
			return((int)XStoreName.invoke(dpy.mem(), (int)w.bits, name));
		} catch(Throwable e) {
		    throw(new RuntimeException(e));
		} finally {
		    checkerror();
		}
	    }
	}

	private final MethodHandle XIconifyWindow = ld.downcallHandle(xlib.find("XIconifyWindow").get(), FunctionDescriptor.of(C_Status, ADDRESS, C_XID, C_INT));
	public int XIconifyWindow(XLib.Display dpy, XID w, int screen_number) {
	    try {
		if(C_XID instanceof ValueLayout.OfLong)
		    return((int)XIconifyWindow.invoke(dpy.mem(), (long)w.bits, screen_number));
		else
		    return((int)XIconifyWindow.invoke(dpy.mem(), (int)w.bits, screen_number));
	    } catch(Throwable e) {
		throw(new RuntimeException(e));
	    } finally {
		checkerror();
	    }
	}

	private final MethodHandle XNextEvent = ld.downcallHandle(xlib.find("XNextEvent").get(), FunctionDescriptor.of(C_INT, ADDRESS, ADDRESS));
	public int XNextEvent(XLib.Display dpy, XLib.XEvent event_return) {
	    try {
		return((int)XNextEvent.invoke(dpy.mem(), event_return.mem()));
	    } catch(Throwable e) {
		throw(new RuntimeException(e));
	    } finally {
		checkerror();
	    }
	}

	private final MethodHandle XPending = ld.downcallHandle(xlib.find("XPending").get(), FunctionDescriptor.of(C_INT, ADDRESS));
	public int XPending(XLib.Display dpy) {
	    try {
		return((int)XPending.invoke(dpy.mem()));
	    } catch(Throwable e) {
		throw(new RuntimeException(e));
	    } finally {
		checkerror();
	    }
	}

	private final MethodHandle XSync = ld.downcallHandle(xlib.find("XSync").get(), FunctionDescriptor.of(C_INT, ADDRESS, C_XBool));
	public int XSync(XLib.Display dpy, boolean discard) {
	    try {
		return((int)XSync.invoke(dpy.mem(), discard ? 1 : 0));
	    } catch(Throwable e) {
		throw(new RuntimeException(e));
	    } finally {
		checkerror();
	    }
	}

	private final MethodHandle XSendEvent = ld.downcallHandle(xlib.find("XSendEvent").get(), FunctionDescriptor.of(C_Status, ADDRESS, C_XID, C_XBool, C_LONG, ADDRESS));
	public int XSendEvent(XLib.Display dpy, XID w, boolean propagate, long event_mask, XLib.XEvent event_send) {
	    try {
		if(C_XID instanceof ValueLayout.OfLong)
		    return((int)XSendEvent.invoke(dpy.mem(), (long)w.bits, propagate ? 1 : 0, event_mask, event_send.mem()));
		else
		    return((int)XSendEvent.invoke(dpy.mem(), (int)w.bits, propagate ? 1 : 0, event_mask, event_send.mem()));
	    } catch(Throwable e) {
		throw(new RuntimeException(e));
	    } finally {
		checkerror();
	    }
	}

	private final MethodHandle XKeysymToString = ld.downcallHandle(xlib.find("XKeysymToString").get(), FunctionDescriptor.of(ADDRESS, C_XID));
	public String XKeysymToString(XID keysym) {
	    MemorySegment name;
	    try {
		if(C_XID instanceof ValueLayout.OfLong)
		    name = (MemorySegment)XKeysymToString.invoke((long)keysym.bits);
		else
		    name = (MemorySegment)XKeysymToString.invoke((int)keysym.bits);
	    } catch(Throwable e) {
		throw(new RuntimeException(e));
	    } finally {
		checkerror();
	    }
	    return(nullp(name) ? null : name.reinterpret(Long.MAX_VALUE).getString(0, C_CHARSET));
	}

	private final MethodHandle XLookupString = ld.downcallHandle(xlib.find("XLookupString").get(), FunctionDescriptor.of(C_INT, ADDRESS, ADDRESS, C_INT, ADDRESS, ADDRESS));
	public int XLookupString(MemorySegment ev, MemorySegment cbuf, MemorySegment sbuf, MemorySegment compose) {
	    try {
		return((int)XLookupString.invoke(ev, cbuf, (int)cbuf.byteSize(), sbuf, compose));
	    } catch(Throwable e) {
		throw(new RuntimeException(e));
	    } finally {
		checkerror();
	    }
	}

	private final MethodHandle Xutf8LookupString = ld.downcallHandle(xlib.find("Xutf8LookupString").get(), FunctionDescriptor.of(C_INT, ADDRESS, ADDRESS, ADDRESS, C_INT, ADDRESS, ADDRESS));
	public int Xutf8LookupString(XIC xic, MemorySegment ev, MemorySegment cbuf, MemorySegment sbuf, MemorySegment status) {
	    try {
		return((int)Xutf8LookupString.invoke(xic.mem, ev, cbuf, (int)cbuf.byteSize(), sbuf, status));
	    } catch(Throwable e) {
		throw(new RuntimeException(e));
	    } finally {
		checkerror();
	    }
	}

	private final MethodHandle XOpenIM = ld.downcallHandle(xlib.find("XOpenIM").get(), FunctionDescriptor.of(ADDRESS, ADDRESS, ADDRESS, ADDRESS, ADDRESS));
	public XIM XOpenIM(XLib.Display dpy, String res_name, String res_class) {
	    MemorySegment ret;
	    try(Arena st = Arena.ofConfined()) {
		ret = (MemorySegment)XOpenIM.invoke(dpy.mem(), MemorySegment.NULL, (res_name == null) ? MemorySegment.NULL : st.allocateFrom(res_name), (res_class == null) ? MemorySegment.NULL : st.allocateFrom(res_class));
	    } catch(Throwable e) {
		throw(new RuntimeException(e));
	    } finally {
		checkerror();
	    }
	    return(nullp(ret) ? null : new XIM(ret));
	}

	private final MethodHandle XCloseIM = ld.downcallHandle(xlib.find("XCloseIM").get(), FunctionDescriptor.of(C_Status, ADDRESS));
	public int XCloseIM(XIM xim) {
	    try {
		return((int)XCloseIM.invoke(xim.mem));
	    } catch(Throwable e) {
		throw(new RuntimeException(e));
	    } finally {
		checkerror();
	    }
	}

	private MemorySegment XVaCreateNestedList(Arena st, Map<String, ?> values) {
	    int i = 0, n = values.size();
	    MemorySegment ret = st.allocate(ADDRESS.byteSize() * ((n + 1) * 2));
	    for(Map.Entry<String, ?> v : values.entrySet()) {
		if(i >= n) throw(new AssertionError());
		switch(v.getKey()) {
		case XNVaNestedList: {
		    @SuppressWarnings("unchecked") Map<String, ?> nested = (Map<String, ?>)v.getValue();
		    ret.set(ADDRESS, ADDRESS.byteSize() * ((i * 2) + 0), st.allocateFrom(v.getKey()));
		    ret.set(ADDRESS, ADDRESS.byteSize() * ((i * 2) + 1), XVaCreateNestedList(st, nested));
		    break;
		}
		case XNInputStyle: {
		    long val = ((Number)v.getValue()).longValue();
		    ret.set(ADDRESS, ADDRESS.byteSize() * ((i * 2) + 0), st.allocateFrom(v.getKey()));
		    setint(ret, ADDRESS.byteSize() * ((i * 2) + 1), PTRINT_T, val);
		    break;
		}
		case XNClientWindow: {
		    XID val = (XID)v.getValue();
		    ret.set(ADDRESS, ADDRESS.byteSize() * ((i * 2) + 0), st.allocateFrom(v.getKey()));
		    setint(ret, ADDRESS.byteSize() * ((i * 2) + 1), C_XID, val.bits);
		    break;
		}
		case XNFocusWindow: {
		    XID val = (XID)v.getValue();
		    ret.set(ADDRESS, ADDRESS.byteSize() * ((i * 2) + 0), st.allocateFrom(v.getKey()));
		    setint(ret, ADDRESS.byteSize() * ((i * 2) + 1), C_XID, val.bits);
		    break;
		}
		default:
		    throw(new IllegalArgumentException("unsupported XIM value: " + v.getKey()));
		}
		i++;
	    }
	    return(ret);
	}

	private MemorySegment initimlist(Arena st, Collection<String> values) {
	    int i = 0, n = values.size();
	    MemorySegment ret = st.allocate(ADDRESS.byteSize() * ((n + 1) * 2));
	    for(String val : values) {
		if(i >= n) throw(new AssertionError());
		ret.set(ADDRESS, ADDRESS.byteSize() * i * 2, st.allocateFrom(val));
		switch(val) {
		case XNQueryInputStyle: {
		    ret.set(ADDRESS, ADDRESS.byteSize() * ((i * 2) + 1), st.allocate(ADDRESS));
		    break;
		}
		case XNFilterEvents: {
		    ret.set(ADDRESS, ADDRESS.byteSize() * ((i * 2) + 1), st.allocate(PTRINT_T));
		    break;
		}
		default:
		    throw(new IllegalArgumentException("unsupported XIM value: " + val));
		}
		i++;
	    }
	    return(ret);
	}

	private void parseimlist(MemorySegment raw, int n, Map<String, Object> buf) {
	    for(int i = 0; i < n; i++) {
		String k = raw.get(ADDRESS, ADDRESS.byteSize() * i * 2).reinterpret(Long.MAX_VALUE).getString(0, C_CHARSET);
		switch(k) {
		case XNQueryInputStyle: {
		    buf.put(k, new XIMStyles(this, raw.get(ADDRESS, ADDRESS.byteSize() * ((i * 2) + 1)).reinterpret(ADDRESS.byteSize()).get(ADDRESS, 0)));
		    break;
		}
		case XNFilterEvents: {
		    buf.put(k, getint(raw.get(ADDRESS, ADDRESS.byteSize() * ((i * 2) + 1)).reinterpret(PTRINT_T.byteSize()), 0, PTRINT_T, false));
		    break;
		}
		default:
		    throw(new IllegalArgumentException("unsupported XIM value: " + k));
		}
	    }
	}

	private final MethodHandle XGetIMValues = ld.downcallHandle(xlib.find("XGetIMValues").get(), FunctionDescriptor.of(ADDRESS, ADDRESS, ADDRESS, ADDRESS, ADDRESS), Linker.Option.firstVariadicArg(1));
	public Map<String, ?> XGetIMValues(XIM xim, Collection<String> values) {
	    try(Arena st = Arena.ofConfined()) {
		MemorySegment ret, valbuf = initimlist(st, values);
		try {
		    ret = (MemorySegment)XGetIMValues.invoke(xim.mem, st.allocateFrom(XNVaNestedList), valbuf, MemorySegment.NULL);
		} catch(Throwable e) {
		    throw(new RuntimeException(e));
		} finally {
		    checkerror();
		}
		Map<String, Object> vret = new HashMap<>();
		parseimlist(valbuf, values.size(), vret);
		if(!nullp(ret))
		    throw(new RuntimeException("unrecognized XIM value name: " + ret.reinterpret(Long.MAX_VALUE).getString(0, C_CHARSET)));
		return(vret);
	    }
	}

	private final MethodHandle XCreateIC = ld.downcallHandle(xlib.find("XCreateIC").get(), FunctionDescriptor.of(ADDRESS, ADDRESS, ADDRESS, ADDRESS, ADDRESS), Linker.Option.firstVariadicArg(1));
	public XIC XCreateIC(XIM xim, Map<String, ?> values) {
	    try(Arena st = Arena.ofConfined()) {
		MemorySegment valbuf = XVaCreateNestedList(st, values);
		MemorySegment ret;
		try {
		    ret = (MemorySegment)XCreateIC.invoke(xim.mem, st.allocateFrom(XNVaNestedList), valbuf, MemorySegment.NULL);
		} catch(Throwable e) {
		    throw(new RuntimeException(e));
		} finally {
		    checkerror();
		}
		return(nullp(ret) ? null : new XIC(ret));
	    }
	}

	private final MethodHandle XDestroyIC = ld.downcallHandle(xlib.find("XDestroyIC").get(), FunctionDescriptor.ofVoid(ADDRESS));
	public void XDestroyIC(XIC xic) {
	    try {
		XDestroyIC.invoke(xic.mem);
	    } catch(Throwable e) {
		throw(new RuntimeException(e));
	    } finally {
		checkerror();
	    }
	}

	private final MethodHandle XSetICFocus = ld.downcallHandle(xlib.find("XSetICFocus").get(), FunctionDescriptor.ofVoid(ADDRESS));
	public void XSetICFocus(XIC xic) {
	    try {
		XSetICFocus.invoke(xic.mem);
	    } catch(Throwable e) {
		throw(new RuntimeException(e));
	    } finally {
		checkerror();
	    }
	}

	private final MethodHandle XUnsetICFocus = ld.downcallHandle(xlib.find("XUnsetICFocus").get(), FunctionDescriptor.ofVoid(ADDRESS));
	public void XUnsetICFocus(XIC xic) {
	    try {
		XUnsetICFocus.invoke(xic.mem);
	    } catch(Throwable e) {
		throw(new RuntimeException(e));
	    } finally {
		checkerror();
	    }
	}

	private final MethodHandle XGetICValues = ld.downcallHandle(xlib.find("XGetICValues").get(), FunctionDescriptor.of(ADDRESS, ADDRESS, ADDRESS, ADDRESS, ADDRESS), Linker.Option.firstVariadicArg(1));
	public Map<String, ?> XGetICValues(XIC xic, Collection<String> values) {
	    try(Arena st = Arena.ofConfined()) {
		MemorySegment ret, valbuf = initimlist(st, values);
		try {
		    ret = (MemorySegment)XGetICValues.invoke(xic.mem, st.allocateFrom(XNVaNestedList), valbuf, MemorySegment.NULL);
		} catch(Throwable e) {
		    throw(new RuntimeException(e));
		} finally {
		    checkerror();
		}
		Map<String, Object> vret = new HashMap<>();
		parseimlist(valbuf, values.size(), vret);
		if(!nullp(ret))
		    throw(new RuntimeException("unrecognized XIC value name: " + ret.reinterpret(Long.MAX_VALUE).getString(0, C_CHARSET)));
		return(vret);
	    }
	}

	private final MethodHandle XGetEventData = ld.downcallHandle(xlib.find("XGetEventData").get(), FunctionDescriptor.of(C_XBool, ADDRESS, ADDRESS));
	public boolean XGetEventData(MemorySegment dpy, MemorySegment cookie) {
	    try {
		return(((int)XGetEventData.invoke(dpy, cookie)) != 0);
	    } catch(Throwable e) {
		throw(new RuntimeException(e));
	    } finally {
		checkerror();
	    }
	}

	private final MethodHandle XFreeEventData = ld.downcallHandle(xlib.find("XFreeEventData").get(), FunctionDescriptor.ofVoid(ADDRESS, ADDRESS));
	public void XFreeEventData(MemorySegment dpy, MemorySegment cookie) {
	    try {
		XFreeEventData.invoke(dpy, cookie);
	    } catch(Throwable e) {
		throw(new RuntimeException(e));
	    } finally {
		checkerror();
	    }
	}

	private final MethodHandle XGetSelectionOwner = ld.downcallHandle(xlib.find("XGetSelectionOwner").get(), FunctionDescriptor.of(C_XID, ADDRESS, C_Atom));
	public XID XGetSelectionOwner(XLib.Display dpy, Atom selection) {
	    try {
		if(C_Atom instanceof ValueLayout.OfLong)
		    return(XID.of((long)XGetSelectionOwner.invoke(dpy.mem(), (long)selection.bits)));
		else
		    return(XID.of((long)XGetSelectionOwner.invoke(dpy.mem(), (int)selection.bits)));
	    } catch(Throwable e) {
		throw(new RuntimeException(e));
	    } finally {
		checkerror();
	    }
	}

	private final MethodHandle XSetSelectionOwner = ld.downcallHandle(xlib.find("XSetSelectionOwner").get(), FunctionDescriptor.of(C_INT, ADDRESS, C_Atom, C_XID, C_Time));
	public int XSetSelectionOwner(XLib.Display dpy, Atom selection, XID owner, long time) {
	    try {
		if(C_Atom instanceof ValueLayout.OfLong)
		    return((int)XSetSelectionOwner.invoke(dpy.mem(), (long)selection.bits, (long)owner.bits, (long)time));
		else
		    return((int)XSetSelectionOwner.invoke(dpy.mem(), (int)selection.bits, (int)owner.bits, (int)time));
	    } catch(Throwable e) {
		throw(new RuntimeException(e));
	    } finally {
		checkerror();
	    }
	}

	private final MethodHandle XConvertSelection = ld.downcallHandle(xlib.find("XConvertSelection").get(), FunctionDescriptor.of(C_INT, ADDRESS, C_Atom, C_Atom, C_Atom, C_XID, C_Time));
	public int XConvertSelection(XLib.Display dpy, Atom selection, Atom target, Atom property, XID requestor, long time) {
	    try {
		if(C_Atom instanceof ValueLayout.OfLong)
		    return((int)XConvertSelection.invoke(dpy.mem(), (long)selection.bits, (long)target.bits, (long)property.bits, (long)requestor.bits, (long)time));
		else
		    return((int)XConvertSelection.invoke(dpy.mem(), (int)selection.bits, (int)target.bits, (int)property.bits, (int)requestor.bits, (int)time));
	    } catch(Throwable e) {
		throw(new RuntimeException(e));
	    } finally {
		checkerror();
	    }
	}

	static class XrmDatabase implements XLib.XrmDatabase {
	    private final MemorySegment[] memp;
	    private final Runnable clean;

	    public XrmDatabase(libX11_so_6 lib, MemorySegment mem) {
		MemorySegment[] memp = {mem};
		this.memp = memp;
		this.clean = Finalizer.finalize(this, () -> {
		    if(memp[0] != null)
			lib.XrmDestroyDatabase(memp[0]);
		});
	    }

	    MemorySegment mem() {return(memp[0]);}

	    void invalid() {
		memp[0] = null;
	    }
	}

	private final MethodHandle XrmDestroyDatabase = ld.downcallHandle(xlib.find("XrmDestroyDatabase").get(), FunctionDescriptor.ofVoid(ADDRESS));
	void XrmDestroyDatabase(MemorySegment mem) {
	    try {
		XrmDestroyDatabase.invoke(mem);
	    } catch(Throwable e) {
		throw(new RuntimeException(e));
	    }
	}

	private final MethodHandle XResourceManagerString = ld.downcallHandle(xlib.find("XResourceManagerString").get(), FunctionDescriptor.of(ADDRESS, ADDRESS));
	public String XResourceManagerString(XLib.Display dpy) {
	    MemorySegment rv;
	    try {
		rv = (MemorySegment)XResourceManagerString.invoke(((Display)dpy).mem());
	    } catch(Throwable e) {
		throw(new RuntimeException(e));
	    }
	    return(nullp(rv) ? null : (rv.reinterpret(Long.MAX_VALUE).getString(0, C_CHARSET)));
	}

	private final MethodHandle XScreenResourceString = ld.downcallHandle(xlib.find("XScreenResourceString").get(), FunctionDescriptor.of(ADDRESS, ADDRESS));
	public String XScreenResourceString(XLib.Screen screen) {
	    MemorySegment rv;
	    try {
		rv = (MemorySegment)XScreenResourceString.invoke(((Screen)screen).mem());
	    } catch(Throwable e) {
		throw(new RuntimeException(e));
	    }
	    String ret = null;
	    if(!nullp(rv)) {
		ret = (rv.reinterpret(Long.MAX_VALUE).getString(0, C_CHARSET));
		XFree(rv);
	    }
	    return(ret);
	}

	private final MethodHandle XrmInitialize = ld.downcallHandle(xlib.find("XrmInitialize").get(), FunctionDescriptor.ofVoid());
	public void XrmInitialize() {
	    try {
		XrmInitialize.invoke();
	    } catch(Throwable e) {
		throw(new RuntimeException(e));
	    }
	}

	private final MethodHandle XrmGetStringDatabase = ld.downcallHandle(xlib.find("XrmGetStringDatabase").get(), FunctionDescriptor.of(ADDRESS, ADDRESS));
	public XrmDatabase XrmGetStringDatabase(String data) {
	    if(data == null)
		return(null);
	    try(Arena st = Arena.ofConfined()) {
		MemorySegment rv;
		try {
		    rv = (MemorySegment)XrmGetStringDatabase.invoke(st.allocateFrom(data, C_CHARSET));
		} catch(Throwable e) {
		    throw(new RuntimeException(e));
		}
		return(nullp(rv) ? null : new XrmDatabase(this, rv));
	    }
	}

	private final MethodHandle XrmGetFileDatabase = ld.downcallHandle(xlib.find("XrmGetFileDatabase").get(), FunctionDescriptor.of(ADDRESS, ADDRESS));
	public XrmDatabase XrmGetFileDatabase(String filename) {
	    try(Arena st = Arena.ofConfined()) {
		MemorySegment rv;
		try {
		    rv = (MemorySegment)XrmGetFileDatabase.invoke(st.allocateFrom(filename, C_CHARSET));
		} catch(Throwable e) {
		    throw(new RuntimeException(e));
		}
		return(nullp(rv) ? null : new XrmDatabase(this, rv));
	    }
	}

	private final MethodHandle XrmMergeDatabases = ld.downcallHandle(xlib.find("XrmMergeDatabases").get(), FunctionDescriptor.ofVoid(ADDRESS, ADDRESS));
	public XLib.XrmDatabase XrmMergeDatabases(XLib.XrmDatabase source, XLib.XrmDatabase target) {
	    if(source == null)
		return(target);
	    try(Arena st = Arena.ofConfined()) {
		MemorySegment tbuf = st.allocateFrom(ADDRESS, (target == null) ? MemorySegment.NULL : (((XrmDatabase)target).mem()));
		try {
		    XrmMergeDatabases.invoke(((XrmDatabase)source).mem(), tbuf);
		} catch(Throwable e) {
		    throw(new RuntimeException(e));
		}
		((XrmDatabase)source).invalid();
		if(target != null)
		    ((XrmDatabase)target).invalid();
		MemorySegment rv = tbuf.get(ADDRESS, 0);
		return(nullp(rv) ? null : new XrmDatabase(this, rv));
	    }
	}

	public static final StructLayout _XrmValue = struct(new MemoryLayout[] {
		C_INT.withName("size"),
		ADDRESS.withName("addr"),
	    });
	private static final VarHandle _XrmValue_size = _XrmValue.varHandle(PathElement.groupElement("size"));
	private static final VarHandle _XrmValue_addr = _XrmValue.varHandle(PathElement.groupElement("addr"));

	private final MethodHandle XrmGetResource = ld.downcallHandle(xlib.find("XrmGetResource").get(), FunctionDescriptor.of(C_XBool, ADDRESS, ADDRESS, ADDRESS, ADDRESS, ADDRESS));
	public XrmValue XrmGetResource(XLib.XrmDatabase database, String name, String cls) {
	    if(database == null)
		return(null);
	    try(Arena st = Arena.ofConfined()) {
		int rv;
		MemorySegment rtbuf = st.allocate(ADDRESS), rvbuf = st.allocate(_XrmValue);
		try {
		    rv = (int)XrmGetResource.invoke(((XrmDatabase)database).mem(), st.allocateFrom(name, C_CHARSET), st.allocateFrom(cls, C_CHARSET), rtbuf, rvbuf);
		} catch(Throwable e) {
		    throw(new RuntimeException(e));
		}
		if(rv == 0)
		    return(null);
		MemorySegment rt = rtbuf.get(ADDRESS, 0);
		int sz = (int)_XrmValue_size.get(rvbuf, 0);
		MemorySegment data = (MemorySegment)_XrmValue_addr.get(rvbuf, 0);
		return(new XrmValue(nullp(rt) ? null : rt.reinterpret(Long.MAX_VALUE).getString(0, C_CHARSET),
				    nullp(data) ? null : memcpy(new byte[sz], data.reinterpret(sz), 0, 0, sz)));
	    }
	}

	private final MethodHandle XkbLibraryVersion = ld.downcallHandle(xlib.find("XkbLibraryVersion").get(), FunctionDescriptor.of(C_XBool, ADDRESS, ADDRESS));
	public boolean XkbLibraryVersion(int[] version) {
	    try(Arena st = Arena.ofConfined()) {
		MemorySegment major = st.allocate(C_INT);
		MemorySegment minor = st.allocate(C_INT);
		setint(major, 0, C_INT, version[0]);
		setint(minor, 0, C_INT, version[1]);
		int ret;
		try {
		    ret = (int)XkbLibraryVersion.invoke(major, minor);
		} catch(Throwable e) {
		    throw(new RuntimeException(e));
		} finally {
		    checkerror();
		}
		version[0] = (int)getint(major, 0, C_INT, true);
		version[1] = (int)getint(minor, 0, C_INT, true);
		return(ret != 0);
	    }
	}

	private final MethodHandle XkbQueryExtension = ld.downcallHandle(xlib.find("XkbQueryExtension").get(), FunctionDescriptor.of(C_XBool, ADDRESS, ADDRESS, ADDRESS, ADDRESS, ADDRESS, ADDRESS));
	public XkbExtensionInfo XkbQueryExtension(XLib.Display dpy, int major, int minor) {
	    try(Arena st = Arena.ofConfined()) {
		MemorySegment opbuf = st.allocate(C_INT), evbuf = st.allocate(C_INT), erbuf = st.allocate(C_INT);
		MemorySegment Vbuf = st.allocate(C_INT), vbuf = st.allocate(C_INT);
		setint(Vbuf, 0, C_INT, major);
		setint(vbuf, 0, C_INT, minor);
		int ret;
		try {
		    ret = (int)XkbQueryExtension.invoke(dpy.mem(), opbuf, evbuf, erbuf, Vbuf, vbuf);
		} catch(Throwable e) {
		    throw(new RuntimeException(e));
		} finally {
		    checkerror();
		}
		if(ret == 0)
		    return(null);
		return(new XkbExtensionInfo((int)getint(opbuf, 0, C_INT, true), (int)getint(evbuf, 0, C_INT, true), (int)getint(erbuf, 0, C_INT, true),
					    (int)getint(Vbuf, 0, C_INT, true), (int)getint(vbuf, 0, C_INT, true)));
	    }
	}

	private final MethodHandle XkbSetDetectableAutoRepeat = ld.downcallHandle(xlib.find("XkbSetDetectableAutoRepeat").get(), FunctionDescriptor.of(C_XBool, ADDRESS, C_XBool, ADDRESS));
	public boolean XkbSetDetectableAutoRepeat(XLib.Display dpy, boolean detectable, boolean[] supported) {
	    try(Arena st = Arena.ofConfined()) {
		MemorySegment sbuf = st.allocate(C_XBool);
		int ret;
		try {
		    ret = (int)XkbSetDetectableAutoRepeat.invoke(dpy.mem(), detectable ? 1 : 0, sbuf);
		} catch(Throwable e) {
		    throw(new RuntimeException(e));
		} finally {
		    checkerror();
		}
		if(supported != null)
		    supported[0] = (getint(sbuf, 0, C_XBool, false) != 0);
		return(ret != 0);
	    }
	}

	public static final StructLayout _XkbSymMapRec = struct(new MemoryLayout[] {
	    MemoryLayout.sequenceLayout(XkbNumKbdGroups, C_CHAR).withName("kt_index"),
	    C_CHAR.withName("group_info"),
	    C_CHAR.withName("width"),
	    C_SHORT.withName("offset"),
	});
	public static class XkbSymMap extends StructInstance implements XLib.XkbSymMap {
	    public final XkbClientMap map;

	    private XkbSymMap(XkbClientMap map, MemorySegment mem) {
		super(mem);
		this.map = map;
	    }

	    protected StructLayout $layout() {return(_XkbSymMapRec);}
	    
	    private static final VarHandle group_info = _XkbSymMapRec.varHandle(PathElement.groupElement("group_info"));
	    public int groupinfo() {return((int)group_info.get(mem, 0));}

	    private static final VarHandle width = _XkbSymMapRec.varHandle(PathElement.groupElement("width"));
	    private static final VarHandle offset = _XkbSymMapRec.varHandle(PathElement.groupElement("offset"));
	    public XID[][] keysyms() {
		int gn = groupinfo() & 0x0f;
		int w = (int)width.get(mem, 0);
		int off = (int)offset.get(mem, 0);
		MemorySegment syms = map.syms();
		XID[][] ret = new XID[gn][w];
		for(int g = 0; g < gn; g++) {
		    for(int l = 0; l < w; l++)
			ret[g][l] = XID.of(getint(syms, C_XID.byteSize() * off++, C_XID, false));
		}
		return(ret);
	    }
	}

	public static final StructLayout _XkbClientMapRec = struct(new MemoryLayout[] {
	    C_CHAR.withName("size_types"),
	    C_CHAR.withName("num_types"),
	    ADDRESS.withName("types"),
	    C_SHORT.withName("size_syms"),
	    C_SHORT.withName("num_syms"),
	    ADDRESS.withName("syms"),
	    ADDRESS.withName("key_sym_map"),
	    ADDRESS.withName("modmap"),
	});
	public static class XkbClientMap extends XLib.XkbClientMap {
	    public final XkbDesc xkb;

	    private XkbClientMap(XkbDesc xkb, MemorySegment mem) {
		super(mem);
		this.xkb = xkb;
	    }

	    protected StructLayout $layout() {return(_XkbClientMapRec);}

	    private static final VarHandle size_syms = _XkbClientMapRec.varHandle(PathElement.groupElement("size_syms"));
	    private static final VarHandle syms = _XkbClientMapRec.varHandle(PathElement.groupElement("syms"));
	    MemorySegment syms() {MemorySegment rv = (MemorySegment)syms.get(mem, 0); return(nullp(rv) ? null : rv.reinterpret(((int)size_syms.get(mem, 0)) * C_XID.byteSize()));}
	    private static final VarHandle key_sym_map = _XkbClientMapRec.varHandle(PathElement.groupElement("key_sym_map"));
	    public List<XkbSymMap> key_sym_map() {return(new MemArray<XkbSymMap>((MemorySegment)key_sym_map.get(mem, 0),
										 _XkbSymMapRec,
										 xkb.max_key_code() + 1,
										 mem -> new XkbSymMap(this, mem)));}

	    private static final VarHandle modmap = _XkbClientMapRec.varHandle(PathElement.groupElement("modmap"));
	    public byte[] modmap() {
		MemorySegment p = (MemorySegment)modmap.get(mem, 0);
		if(nullp(p))
		    return(null);
		byte[] ret = new byte[xkb.max_key_code() + 1];
		p = p.reinterpret(C_CHAR.byteSize() * ret.length);
		for(int i = 0; i < ret.length; i++)
		    ret[i] = (byte)getint(p, C_CHAR.byteSize() * i, C_CHAR, false);
		return(ret);
	    }
	}

	public static final StructLayout _XkbKeyNameRec = struct(new MemoryLayout[] {
	    MemoryLayout.sequenceLayout(XkbKeyNameLength, C_CHAR).withName("name"),
	});
	public static final StructLayout _XkbKeyAliasRec = struct(new MemoryLayout[] {
	    MemoryLayout.sequenceLayout(XkbKeyNameLength, C_CHAR).withName("real"),
	    MemoryLayout.sequenceLayout(XkbKeyNameLength, C_CHAR).withName("alias"),
	});
	public static final StructLayout _XkbNamesRec = struct(new MemoryLayout[] {
	    C_Atom.withName("keycodes"),
	    C_Atom.withName("geometry"),
	    C_Atom.withName("symbols"),
	    C_Atom.withName("types"),
	    C_Atom.withName("compat"),
	    MemoryLayout.sequenceLayout(XkbNumVirtualMods, C_Atom).withName("vmods"),
	    MemoryLayout.sequenceLayout(XkbNumIndicators, C_Atom).withName("indicators"),
	    MemoryLayout.sequenceLayout(XkbNumKbdGroups, C_Atom).withName("groups"),
	    ADDRESS.withName("keys"),
	    ADDRESS.withName("key_aliases"),
	    ADDRESS.withName("radio_groups"),
	    C_Atom.withName("phys_symbols"),
	    C_CHAR.withName("num_keys"),
	    C_CHAR.withName("num_key_aliases"),
	    C_SHORT.withName("num_rg"),
	});
	public static class XkbNames extends XLib.XkbNames {
	    public final XkbDesc xkb;

	    private XkbNames(XkbDesc xkb, MemorySegment mem) {
		super(mem);
		this.xkb = xkb;
	    }

	    protected StructLayout $layout() {return(_XkbNamesRec);}

	    private static final VarHandle vmods = _XkbNamesRec.varHandle(PathElement.groupElement("vmods"), PathElement.sequenceElement());
	    public Atom[] vmods() {
		Atom[] ret = new Atom[XkbNumVirtualMods];
		for(int i = 0; i < XkbNumVirtualMods; i++)
		    ret[i] = Atom.of((long)vmods.get(mem, 0, i));
		return(ret);
	    }

	    private static String name2str(MemorySegment mem) {
		byte[] name = memcpy(mem, 0, (int)C_CHAR.byteSize() * XkbKeyNameLength);
		int l = XkbKeyNameLength;
		while((l > 0) && (name[l - 1] == 0)) l--;
		return(new String(name, 0, l, Utils.ascii));
	    }

	    private static final long kar_real = _XkbKeyAliasRec.byteOffset(PathElement.groupElement("real"));
	    private static final long kar_alias = _XkbKeyAliasRec.byteOffset(PathElement.groupElement("alias"));
	    private static Pair<String, String> alias2pair(MemorySegment mem) {
		return(Pair.of(name2str(mem.asSlice(kar_real,  _XkbKeyNameRec.byteSize())),
			       name2str(mem.asSlice(kar_alias, _XkbKeyNameRec.byteSize()))));
	    }

	    private static final VarHandle keys = _XkbNamesRec.varHandle(PathElement.groupElement("keys"));
	    private static final VarHandle num_keys = _XkbNamesRec.varHandle(PathElement.groupElement("num_keys"));
	    public List<String> keys() {return(new MemArray<String>((MemorySegment)keys.get(mem, 0), _XkbKeyNameRec,
								    (int)num_keys.get(mem, 0) & 0xff,
								    XkbNames::name2str));}

	    private static final VarHandle key_aliases = _XkbNamesRec.varHandle(PathElement.groupElement("key_aliases"));
	    private static final VarHandle num_key_aliases = _XkbNamesRec.varHandle(PathElement.groupElement("num_key_aliases"));
	    public List<Pair<String, String>> key_aliases() {
		return(new MemArray<Pair<String, String>>((MemorySegment)key_aliases.get(mem, 0), _XkbKeyAliasRec,
							  (int)num_key_aliases.get(mem, 0) & 0xff,
							  XkbNames::alias2pair));
	    }
	}

	public static final StructLayout _XkbDescRec = struct(new MemoryLayout[] {
	    ADDRESS.withName("dpy"),
	    C_SHORT.withName("flags"),
	    C_SHORT.withName("device_spec"),
	    C_KeyCode.withName("min_key_code"),
	    C_KeyCode.withName("max_key_code"),

	    ADDRESS.withName("ctrls"),
	    ADDRESS.withName("server"),
	    ADDRESS.withName("map"),
	    ADDRESS.withName("indicators"),
	    ADDRESS.withName("names"),
	    ADDRESS.withName("compat"),
	    ADDRESS.withName("geom"),
	});
	public static class XkbDesc extends XLib.XkbDesc {
	    private XkbDesc(MemorySegment mem) {
		super(mem);
	    }

	    protected StructLayout $layout() {return(_XkbDescRec);}

	    private static final VarHandle flags = _XkbDescRec.varHandle(PathElement.groupElement("flags"));
	    public int flags() {return((int)flags.get(mem, 0) & 0xffff);}
	    private static final VarHandle device_spec = _XkbDescRec.varHandle(PathElement.groupElement("device_spec"));
	    public int device_spec() {return((int)device_spec.get(mem, 0) & 0xffff);}
	    private static final VarHandle min_key_code = _XkbDescRec.varHandle(PathElement.groupElement("min_key_code"));
	    public int min_key_code() {return((int)min_key_code.get(mem, 0) & 0xff);}
	    private static final VarHandle max_key_code = _XkbDescRec.varHandle(PathElement.groupElement("max_key_code"));
	    public int max_key_code() {return((int)max_key_code.get(mem, 0) & 0xff);}

	    private static final VarHandle map = _XkbDescRec.varHandle(PathElement.groupElement("map"));
	    public XkbClientMap map() {MemorySegment rv = (MemorySegment)map.get(mem, 0); return(nullp(rv) ? null : new XkbClientMap(this, rv));}
	    private static final VarHandle names = _XkbDescRec.varHandle(PathElement.groupElement("names"));
	    public XkbNames names() {MemorySegment rv = (MemorySegment)names.get(mem, 0); return(nullp(rv) ? null : new XkbNames(this, rv));}
	}

	private final MethodHandle XkbFreeKeyboard = ld.downcallHandle(xlib.find("XkbFreeKeyboard").get(), FunctionDescriptor.ofVoid(ADDRESS, C_INT, C_XBool));
	void XkbFreeKeyboard(XLib.XkbDesc xkb, int which, boolean free_all) {
	    try {
		XkbFreeKeyboard.invoke(xkb.mem(), which, free_all ? 1 : 0);
	    } catch(Throwable e) {
		throw(new RuntimeException(e));
	    } finally {
		checkerror();
	    }
	}

	private final MethodHandle XkbGetKeyboard = ld.downcallHandle(xlib.find("XkbGetKeyboard").get(), FunctionDescriptor.of(ADDRESS, ADDRESS, C_INT, C_INT));
	public XkbDesc XkbGetKeyboard(XLib.Display dpy, int which, int device_spec) {
	    MemorySegment rv;
	    try {
		rv = (MemorySegment)XkbGetKeyboard.invoke(dpy.mem(), which, device_spec);
	    } catch(Throwable e) {
		throw(new RuntimeException(e));
	    } finally {
		checkerror();
	    }
	    if(nullp(rv))
		return(null);
	    XkbDesc xkb = new XkbDesc(rv);
	    Finalizer.finalize(xkb, () -> XkbFreeKeyboard(xkb, 0, true));
	    return(xkb);
	}

	private final MethodHandle XkbGetMap = ld.downcallHandle(xlib.find("XkbGetMap").get(), FunctionDescriptor.of(ADDRESS, ADDRESS, C_INT, C_INT));
	public XkbDesc XkbGetMap(XLib.Display dpy, int which, int device_spec) {
	    MemorySegment rv;
	    try {
		rv = (MemorySegment)XkbGetMap.invoke(dpy.mem(), which, device_spec);
	    } catch(Throwable e) {
		throw(new RuntimeException(e));
	    } finally {
		checkerror();
	    }
	    if(nullp(rv))
		return(null);
	    XkbDesc xkb = new XkbDesc(rv);
	    Finalizer.finalize(xkb, () -> XkbFreeKeyboard(xkb, 0, true));
	    return(xkb);
	}

	private final MethodHandle XkbGetNames = ld.downcallHandle(xlib.find("XkbGetNames").get(), FunctionDescriptor.of(C_Status, ADDRESS, C_INT, ADDRESS));
	public void XkbGetNames(XLib.Display dpy, int which, XLib.XkbDesc xkb) {
	    int rv;
	    try {
		rv = (int)XkbGetNames.invoke(dpy.mem(), which, xkb.mem());
	    } catch(Throwable e) {
		throw(new RuntimeException(e));
	    } finally {
		checkerror();
	    }
	    if(rv != Success)
		throw(new XStatusException(rv));
	}

	public XSetWindowAttributes XSetWindowAttributes() {return(new XSetWindowAttributes());}
	public XWindowChanges XWindowChanges() {return(new XWindowChanges());}
	public XEvent XEvent() {return(new XEvent(this));}
	public XComposeStatus XComposeStatus() {return(new XComposeStatus());}
	XErrorEvent XErrorEvent(MemorySegment mem) {return(new XErrorEvent(mem));}
    }

    private static XLib instance = null;
    public static XLib get() {
	if(instance == null) {
	    synchronized(XLib.class) {
		if(instance == null) {
		    instance = new libX11_so_6();
		}
	    }
	}
	return(instance);
    }
}
