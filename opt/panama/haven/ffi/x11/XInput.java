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
import haven.ffi.x11.XLib.*;
import java.util.*;
import java.lang.invoke.*;
import java.lang.foreign.*;
import java.lang.foreign.MemoryLayout.PathElement;
import static haven.ffi.ABI.*;
import static haven.ffi.FUtils.*;
import static haven.ffi.x11.XLib.checkerror;
import static java.lang.foreign.ValueLayout.ADDRESS;

public abstract class XInput {
    public static final int XIAllDevices = 0;
    public static final int XIAllMasterDevices = 1;

    public static final int XI_DeviceChanged = 1;
    public static final int XI_KeyPress = 2;
    public static final int XI_KeyRelease = 3;
    public static final int XI_ButtonPress = 4;
    public static final int XI_ButtonRelease = 5;
    public static final int XI_Motion = 6;
    public static final int XI_Enter = 7;
    public static final int XI_Leave = 8;
    public static final int XI_FocusIn = 9;
    public static final int XI_FocusOut = 10;
    public static final int XI_HierarchyChanged = 11;
    public static final int XI_PropertyEvent = 12;
    public static final int XI_RawKeyPress = 13;
    public static final int XI_RawKeyRelease = 14;
    public static final int XI_RawButtonPress = 15;
    public static final int XI_RawButtonRelease = 16;
    public static final int XI_RawMotion = 17;
    public static final int XI_TouchBegin = 18 /* XI 2.2 */;
    public static final int XI_TouchUpdate = 19;
    public static final int XI_TouchEnd = 20;
    public static final int XI_TouchOwnership = 21;
    public static final int XI_RawTouchBegin = 22;
    public static final int XI_RawTouchUpdate = 23;
    public static final int XI_RawTouchEnd = 24;
    public static final int XI_BarrierHit = 25 /* XI 2.3 */;
    public static final int XI_BarrierLeave = 26;
    public static final int XI_GesturePinchBegin = 27 /* XI 2.4 */;
    public static final int XI_GesturePinchUpdate = 28;
    public static final int XI_GesturePinchEnd = 29;
    public static final int XI_GestureSwipeBegin = 30;
    public static final int XI_GestureSwipeUpdate = 31;
    public static final int XI_GestureSwipeEnd = 32;

    public static final int XIMasterPointer = 1;
    public static final int XIMasterKeyboard = 2;
    public static final int XISlavePointer = 3;
    public static final int XISlaveKeyboard = 4;
    public static final int XIFloatingSlave = 5;
    public static final int XIKeyClass = 0;
    public static final int XIButtonClass = 1;
    public static final int XIValuatorClass = 2;
    public static final int XIScrollClass = 3;
    public static final int XITouchClass = 8;
    public static final int XIGestureClass = 9;

    public static final int XIModeRelative = 0;
    public static final int XIModeAbsolute = 1;
    public static final int XIScrollTypeVertical = 1;
    public static final int XIScrollTypeHorizontal = 2;
    public static final int XIScrollFlagNoEmulation = (1 << 0);
    public static final int XIScrollFlagPreferred = (1 << 1);

    public static final int XIKeyRepeat = (1 << 16);
    public static final int XIPointerEmulated = (1 << 16);
    public static final int XITouchPendingEnd = (1 << 16);
    public static final int XITouchEmulatingPointer = (1 << 17);

    public abstract static class XIAnyClassInfo extends StructInstance {
	protected XIAnyClassInfo(MemorySegment mem) {
	    super(mem);
	}

	MemorySegment mem() {return(mem);}

	public abstract int type();
	public abstract int sourceid();

	public abstract XIButtonClassInfo button();
	public abstract XIValuatorClassInfo valuator();
	public abstract XIScrollClassInfo scroll();
    }

    public abstract static class XIButtonClassInfo extends StructInstance {
	private XIButtonClassInfo(MemorySegment mem) {
	    super(mem);
	}

	MemorySegment mem() {return(mem);}

	public abstract int type();
	public abstract int sourceid();
	public abstract Atom[] labels();
	public abstract Collection<? extends Integer> state();
    }

    public abstract static class XIValuatorClassInfo extends StructInstance {
	protected XIValuatorClassInfo(MemorySegment mem) {
	    super(mem);
	}

	MemorySegment mem() {return(mem);}

	public abstract int type();
	public abstract int sourceid();
	public abstract int number();
	public abstract Atom label();
	public abstract double min();
	public abstract double max();
	public abstract double value();
	public abstract int resolution();
	public abstract int mode();
    }

    public abstract static class XIScrollClassInfo extends StructInstance {
	protected XIScrollClassInfo(MemorySegment mem) {
	    super(mem);
	}

	MemorySegment mem() {return(mem);}

	public abstract int type();
	public abstract int sourceid();
	public abstract int number();
	public abstract int scroll_type();
	public abstract double increment();
	public abstract int flags();
    }

    public abstract static class XIDeviceInfo extends StructInstance {
	protected XIDeviceInfo(MemorySegment mem) {
	    super(mem);
	}

	MemorySegment mem() {return(mem);}

	public abstract int deviceid();
	public abstract String name();
	public abstract int use();
	public abstract int attachment();
	public abstract List<? extends XIAnyClassInfo> classes();
    }

    public static class XIEventMask {
	int deviceid;
	Set<Integer> mask = new HashSet<>();

	public XIEventMask(int deviceid, int... events) {
	    this.deviceid = deviceid;
	    mask(events);
	}

	public XIEventMask deviceid(int value) {deviceid = value; return(this);}

	public XIEventMask mask(int... events) {
	    mask.clear();
	    return(set(events));
	}
	public XIEventMask set(int... events) {
	    for(int i : events)
		mask.add(i);
	    return(this);
	}
	public XIEventMask clear(int... events) {
	    for(int i : events)
		mask.remove(i);
	    return(this);
	}
    }

    public abstract static class XIModifierState extends StructInstance {
	protected XIModifierState(MemorySegment mem) {
	    super(mem);
	}

	MemorySegment mem() {return(mem);}

	public abstract int base();
	public abstract int latched();
	public abstract int locked();
	public abstract int effective();
    }

    public static interface XIGenericEvent {
	public int evtype();
	public int deviceid();
	public int sourceid();
	public int detail();
	public XID root();
	public XID event();
	public XID child();
	public double root_x();
	public double root_y();
	public double event_x();
	public double event_y();
	public Collection<? extends Integer> buttons();
	public XIModifierState mods();
	public XIModifierState group();
    }

    public abstract static class XIDeviceEvent extends StructInstance implements XIGenericEvent {
	protected XIDeviceEvent(MemorySegment mem) {
	    super(mem);
	}

	MemorySegment mem() {return(mem);}

	public abstract int evtype();
	public abstract int deviceid();
	public abstract int sourceid();
	public abstract int detail();
	public abstract XID root();
	public abstract XID event();
	public abstract XID child();
	public abstract double root_x();
	public abstract double root_y();
	public abstract double event_x();
	public abstract double event_y();
	public abstract int flags();
	public abstract Collection<? extends Integer> buttons();
	public abstract Map<Integer, Double> valuators();
	public abstract XIModifierState mods();
	public abstract XIModifierState group();
    }

    public abstract static class XIFocusEvent extends StructInstance implements XIGenericEvent {
	protected XIFocusEvent(MemorySegment mem) {
	    super(mem);
	}

	MemorySegment mem() {return(mem);}

	public abstract int evtype();
	public abstract int deviceid();
	public abstract int sourceid();
	public abstract int detail();
	public abstract XID root();
	public abstract XID event();
	public abstract XID child();
	public abstract double root_x();
	public abstract double root_y();
	public abstract double event_x();
	public abstract double event_y();
	public abstract int mode();
	public abstract boolean focus();
	public abstract boolean same_screen();
	public abstract Collection<? extends Integer> buttons();
	public abstract XIModifierState mods();
	public abstract XIModifierState group();
    }

    public abstract int XIQueryVersion(Display dpy, int[] version);
    public abstract List<? extends XIDeviceInfo> XIQueryDevice(Display dpy, int deviceid);
    public abstract int XISelectEvents(Display dpy, XID win, List<? extends XInput.XIEventMask> events);
    public abstract XIDeviceEvent XIDeviceEvent(XGenericEvent from);
    public abstract XIFocusEvent XIFocusEvent(XGenericEvent from);

    static class libXi_so_6 extends XInput {
	private static final MemoryLayout C_Status = XLib.libX11_so_6.C_Status;
	private static final MemoryLayout C_XBool = XLib.libX11_so_6.C_XBool;
	private static final MemoryLayout C_XID = XLib.libX11_so_6.C_XID;
	private static final MemoryLayout C_Atom = XLib.libX11_so_6.C_Atom;
	private static final MemoryLayout C_Time = XLib.libX11_so_6.C_Time;
	private final SymbolLookup xi = loadlib("libXi.so.6", Arena.global());

	private final MethodHandle XIQueryVersion = ld.downcallHandle(xi.find("XIQueryVersion").get(), FunctionDescriptor.of(C_Status, ADDRESS, ADDRESS, ADDRESS));
	public int XIQueryVersion(Display dpy, int[] version) {
	    try(Arena st = Arena.ofConfined()) {
		MemorySegment major = st.allocate(C_INT);
		MemorySegment minor = st.allocate(C_INT);
		setint(major, 0, C_INT, version[0]);
		setint(minor, 0, C_INT, version[1]);
		int ret;
		try {
		    ret = (int)XIQueryVersion.invoke(dpy.mem(), major, minor);
		} catch(Throwable e) {
		    throw(new InvocationException(e));
		} finally {
		    checkerror();
		}
		version[0] = (int)getint(major, 0, C_INT, true);
		version[1] = (int)getint(minor, 0, C_INT, true);
		return(ret);
	    }
	}

	private static final StructLayout _XIButtonState = struct(new MemoryLayout[] {
		C_INT.withName("mask_len"),
		ADDRESS.withName("mask"),
	    });
	public static class XIButtonState extends StructInstance {
	    private XIButtonState(MemorySegment mem) {super(mem);}
	    protected StructLayout $layout() {return(_XIButtonState);}

	    private static final VarHandle mask_len = _XIButtonState.varHandle(PathElement.groupElement("mask_len"));
	    private static final VarHandle mask = _XIButtonState.varHandle(PathElement.groupElement("mask"));
	    public Collection<? extends Integer> buttons() {
		int ln = (int)mask_len.get(mem, 0);
		MemorySegment m = ((MemorySegment)mask.get(mem, 0)).reinterpret(ln);
		Collection<Integer> ret = new ArrayList<>();
		for(int i = 0; i < ln; i++) {
		    int b = m.get(ValueLayout.JAVA_BYTE, i) & 0xff;
		    for(int o = 0; o < 8; o++) {
			if((b & (1 << o)) != 0)
			    ret.add((i * 8) + o);
		    }
		}
		return(ret);
	    }
	}

	private static final StructLayout _XIAnyClassInfo = struct(new MemoryLayout[] {
		C_INT.withName("type"),
		C_INT.withName("sourceid"),
	    });
	public static class XIAnyClassInfo extends XInput.XIAnyClassInfo {
	    public final XIDeviceInfo dev;

	    private XIAnyClassInfo(XIDeviceInfo dev, MemorySegment mem) {
		super(mem);
		this.dev = dev;
	    }

	    protected StructLayout $layout() {return(_XIAnyClassInfo);}

	    private static final VarHandle type = _XIAnyClassInfo.varHandle(PathElement.groupElement("type"));
	    public int type() {return((int)type.get(mem, 0));}
	    private static final VarHandle sourceid = _XIAnyClassInfo.varHandle(PathElement.groupElement("sourceid"));
	    public int sourceid() {return((int)sourceid.get(mem, 0));}

	    public XIButtonClassInfo button() {return(new XIButtonClassInfo(dev, mem));}
	    public XIValuatorClassInfo valuator() {return(new XIValuatorClassInfo(dev, mem));}
	    public XIScrollClassInfo scroll() {return(new XIScrollClassInfo(dev, mem));}
	}

	private static final StructLayout _XIButtonClassInfo = struct(new MemoryLayout[] {
		C_INT.withName("type"),
		C_INT.withName("sourceid"),
		C_INT.withName("num_buttons"),
		ADDRESS.withName("labels"),
		_XIButtonState.withName("state"),
	    });
	public static class XIButtonClassInfo extends XInput.XIButtonClassInfo {
	    public final XIDeviceInfo dev;

	    private XIButtonClassInfo(XIDeviceInfo dev, MemorySegment mem) {
		super(mem);
		this.dev = dev;
	    }

	    protected StructLayout $layout() {return(_XIButtonClassInfo);}

	    private static final VarHandle type = _XIButtonClassInfo.varHandle(PathElement.groupElement("type"));
	    public int type() {return((int)type.get(mem, 0));}
	    private static final VarHandle sourceid = _XIButtonClassInfo.varHandle(PathElement.groupElement("sourceid"));
	    public int sourceid() {return((int)sourceid.get(mem, 0));}
	    private static final VarHandle num_buttons = _XIButtonClassInfo.varHandle(PathElement.groupElement("num_buttons"));
	    private static final VarHandle labels = _XIButtonClassInfo.varHandle(PathElement.groupElement("labels"));
	    public Atom[] labels() {
		Atom[] ret = new Atom[(int)num_buttons.get(mem, 0)];
		MemorySegment l = ((MemorySegment)labels.get(mem, 0)).reinterpret(C_Atom.byteSize() * ret.length);
		for(int i = 0; i < ret.length; i++)
		    ret[i] = Atom.of(getint(l, C_Atom.byteSize() * i, C_Atom, false));
		return(ret);
	    }
	    private static final long state = _XIButtonClassInfo.byteOffset(PathElement.groupElement("state"));
	    public Collection<? extends Integer> state() {
		return(new XIButtonState(mem.asSlice(state, 0)).buttons());
	    }
	}

	private static final StructLayout _XIValuatorClassInfo = struct(new MemoryLayout[] {
		C_INT.withName("type"),
		C_INT.withName("sourceid"),
		C_INT.withName("number"),
		C_Atom.withName("label"),
		C_DOUBLE.withName("min"),
		C_DOUBLE.withName("max"),
		C_DOUBLE.withName("value"),
		C_INT.withName("resolution"),
		C_INT.withName("mode"),
	    });
	public static class XIValuatorClassInfo extends XInput.XIValuatorClassInfo {
	    public final XIDeviceInfo dev;

	    private XIValuatorClassInfo(XIDeviceInfo dev, MemorySegment mem) {
		super(mem);
		this.dev = dev;
	    }

	    protected StructLayout $layout() {return(_XIValuatorClassInfo);}

	    private static final VarHandle type = _XIValuatorClassInfo.varHandle(PathElement.groupElement("type"));
	    public int type() {return((int)type.get(mem, 0));}
	    private static final VarHandle sourceid = _XIValuatorClassInfo.varHandle(PathElement.groupElement("sourceid"));
	    public int sourceid() {return((int)sourceid.get(mem, 0));}
	    private static final VarHandle number = _XIValuatorClassInfo.varHandle(PathElement.groupElement("number"));
	    public int number() {return((int)number.get(mem, 0));}
	    private static final VarHandle label = _XIValuatorClassInfo.varHandle(PathElement.groupElement("label"));
	    public Atom label() {return(Atom.of((long)label.get(mem, 0)));}
	    private static final VarHandle min = _XIValuatorClassInfo.varHandle(PathElement.groupElement("min"));
	    public double min() {return((double)min.get(mem, 0));}
	    private static final VarHandle max = _XIValuatorClassInfo.varHandle(PathElement.groupElement("max"));
	    public double max() {return((double)max.get(mem, 0));}
	    private static final VarHandle value = _XIValuatorClassInfo.varHandle(PathElement.groupElement("value"));
	    public double value() {return((double)value.get(mem, 0));}
	    private static final VarHandle resolution = _XIValuatorClassInfo.varHandle(PathElement.groupElement("resolution"));
	    public int resolution() {return((int)resolution.get(mem, 0));}
	    private static final VarHandle mode = _XIValuatorClassInfo.varHandle(PathElement.groupElement("mode"));
	    public int mode() {return((int)mode.get(mem, 0));}
	}

	private static final StructLayout _XIScrollClassInfo = struct(new MemoryLayout[] {
		C_INT.withName("type"),
		C_INT.withName("sourceid"),
		C_INT.withName("number"),
		C_INT.withName("scroll_type"),
		C_DOUBLE.withName("increment"),
		C_INT.withName("flags"),
	    });
	public static class XIScrollClassInfo extends XInput.XIScrollClassInfo {
	    public final XIDeviceInfo dev;

	    private XIScrollClassInfo(XIDeviceInfo dev, MemorySegment mem) {
		super(mem);
		this.dev = dev;
	    }

	    protected StructLayout $layout() {return(_XIScrollClassInfo);}

	    private static final VarHandle type = _XIScrollClassInfo.varHandle(PathElement.groupElement("type"));
	    public int type() {return((int)type.get(mem, 0));}
	    private static final VarHandle sourceid = _XIScrollClassInfo.varHandle(PathElement.groupElement("sourceid"));
	    public int sourceid() {return((int)sourceid.get(mem, 0));}
	    private static final VarHandle number = _XIScrollClassInfo.varHandle(PathElement.groupElement("number"));
	    public int number() {return((int)number.get(mem, 0));}
	    private static final VarHandle scroll_type = _XIScrollClassInfo.varHandle(PathElement.groupElement("scroll_type"));
	    public int scroll_type() {return((int)scroll_type.get(mem, 0));}
	    private static final VarHandle increment = _XIScrollClassInfo.varHandle(PathElement.groupElement("increment"));
	    public double increment() {return((double)increment.get(mem, 0));}
	    private static final VarHandle flags = _XIScrollClassInfo.varHandle(PathElement.groupElement("flags"));
	    public int flags() {return((int)flags.get(mem, 0));}
	}

	private static final StructLayout _XIDeviceInfo = struct(new MemoryLayout[] {
		C_INT.withName("deviceid"),
		ADDRESS.withName("name"),
		C_INT.withName("use"),
		C_INT.withName("attachment"),
		C_XBool.withName("enabled"),
		C_INT.withName("num_classes"),
		ADDRESS.withName("classes"),
	    });
	public static class XIDeviceInfo extends XInput.XIDeviceInfo {
	    public final Object group;

	    private XIDeviceInfo(MemorySegment mem, Object group) {
		super(mem);
		this.group = group;
	    }

	    protected StructLayout $layout() {return(_XIDeviceInfo);}

	    private static final VarHandle deviceid = _XIDeviceInfo.varHandle(PathElement.groupElement("deviceid"));
	    public int deviceid() {return((int)deviceid.get(mem, 0));}
	    private static final VarHandle use = _XIDeviceInfo.varHandle(PathElement.groupElement("use"));
	    public int use() {return((int)use.get(mem, 0));}
	    private static final VarHandle attachment = _XIDeviceInfo.varHandle(PathElement.groupElement("attachment"));
	    public int attachment() {return((int)attachment.get(mem, 0));}
	    private static final VarHandle num_classes = _XIDeviceInfo.varHandle(PathElement.groupElement("num_classes"));
	    public int num_classes() {return((int)num_classes.get(mem, 0));}
	    private static final VarHandle name = _XIDeviceInfo.varHandle(PathElement.groupElement("name"));
	    public String name() {
		return(((MemorySegment)name.get(mem, 0)).reinterpret(Long.MAX_VALUE).getString(0, C_CHARSET));
	    }
	    private static final VarHandle classes = _XIDeviceInfo.varHandle(PathElement.groupElement("classes"));
	    public List<XIAnyClassInfo> classes() {
		XIAnyClassInfo[] ret = new XIAnyClassInfo[num_classes()];
		MemorySegment arr = ((MemorySegment)classes.get(mem, 0)).reinterpret(ADDRESS.byteSize() * ret.length);
		for(int i = 0; i < ret.length; i++)
		    ret[i] = new XIAnyClassInfo(this, arr.get(ADDRESS, ADDRESS.byteSize() * i));
		return(Arrays.asList(ret));
	    }
	}

	private final MethodHandle XIFreeDeviceInfo = ld.downcallHandle(xi.find("XIFreeDeviceInfo").get(), FunctionDescriptor.ofVoid(ADDRESS));
	private void XIFreeDeviceInfo(MemorySegment info) {
	    try {
		XIFreeDeviceInfo.invoke(info);
	    } catch(Throwable e) {
		throw(new InvocationException(e));
	    } finally {
		checkerror();
	    }
	}

	private final MethodHandle XIQueryDevice = ld.downcallHandle(xi.find("XIQueryDevice").get(), FunctionDescriptor.of(ADDRESS, ADDRESS, C_INT, ADDRESS));
	public List<XIDeviceInfo> XIQueryDevice(Display dpy, int deviceid) {
	    try(Arena st = Arena.ofConfined()) {
		MemorySegment ndev = st.allocate(C_INT);
		MemorySegment mem;
		try {
		    mem = (MemorySegment)XIQueryDevice.invoke(dpy.mem(), deviceid, ndev);
		} catch(Throwable e) {
		    throw(new InvocationException(e));
		} finally {
		    checkerror();
		}
		Object[] gbuf = {null};
		List<XIDeviceInfo> ret = new MemArray<XIDeviceInfo>(mem, _XIDeviceInfo, (int)getint(ndev, 0, C_INT, true), dmem -> new XIDeviceInfo(dmem, gbuf[0]));
		gbuf[0] = ret;
		Finalizer.finalize(ret, () -> XIFreeDeviceInfo(mem));
		return(ret);
	    }
	}

	private static final StructLayout _XIEventMask = struct(new MemoryLayout[] {
		C_INT.withName("deviceid"),
		C_INT.withName("mask_len"),
		ADDRESS.withName("mask"),
	    });
	public static class XIEventMask extends StructInstance {
	    private MemorySegment curmask = null;

	    private XIEventMask(MemorySegment mem) {
		super(mem);
	    }

	    private XIEventMask(Arena st) {
		this(st.allocate(_XIEventMask));
	    }

	    private XIEventMask(Arena st, XInput.XIEventMask from) {
		this(st);
		populate(st, from);
	    }

	    public void populate(Arena st, XInput.XIEventMask from) {
		deviceid(from.deviceid);
		int maxev = 0;
		for(int ev : from.mask)
		    maxev = Math.max(maxev, ev);
		int mlen = (maxev >> 3) + 1;
		mask_len.set(mem, 0, mlen);
		MemorySegment mask = st.allocate(mlen);
		for(int ev : from.mask)
		    mask.set(ValueLayout.JAVA_BYTE, ev >> 3, (byte)(mask.get(ValueLayout.JAVA_BYTE, ev >> 3) | (1 << (ev & 7))));
		this.mask.set(mem, 0, mask);
	    }

	    protected StructLayout $layout() {return(_XIEventMask);}

	    private static final VarHandle deviceid = _XIEventMask.varHandle(PathElement.groupElement("deviceid"));
	    public XIEventMask deviceid(int value) {deviceid.set(mem, 0, value); return(this);}

	    private static final VarHandle mask_len = _XIEventMask.varHandle(PathElement.groupElement("mask_len"));
	    private static final VarHandle mask = _XIEventMask.varHandle(PathElement.groupElement("mask"));
	}

	private final MethodHandle XISelectEvents = ld.downcallHandle(xi.find("XISelectEvents").get(), FunctionDescriptor.of(C_INT, ADDRESS, C_XID, ADDRESS, C_INT));
	public int XISelectEvents(Display dpy, XID win, List<? extends XInput.XIEventMask> events) {
	    try(Arena st = Arena.ofConfined()) {
		int n = events.size();
		MemorySegment evmem = st.allocate(_XIEventMask, n);
		for(int i = 0; i < n; i++)
		    new XIEventMask(evmem.asSlice(_XIEventMask.byteSize() * i, _XIEventMask)).populate(st, events.get(i));
		try {
		    if(C_XID instanceof ValueLayout.OfLong)
			return((int)XISelectEvents.invoke(dpy.mem(), (long)win.bits, evmem, n));
		    else
			return((int)XISelectEvents.invoke(dpy.mem(), (int)win.bits, evmem, n));
		} catch(Throwable e) {
		    throw(new InvocationException(e));
		} finally {
		    checkerror();
		}
	    }
	}

	private static final StructLayout _XIValuatorState = struct(new MemoryLayout[] {
		C_INT.withName("mask_len"),
		ADDRESS.withName("mask"),
		ADDRESS.withName("values"),
	    });
	private static final StructLayout _XIModifierState = struct(new MemoryLayout[] {
		C_INT.withName("base"),
		C_INT.withName("latched"),
		C_INT.withName("locked"),
		C_INT.withName("effective"),
	    });
	public static class XIModifierState extends XInput.XIModifierState {
	    private XIModifierState(MemorySegment mem) {super(mem);}
	    protected StructLayout $layout() {return(_XIModifierState);}

	    private static final VarHandle base = _XIModifierState.varHandle(PathElement.groupElement("base"));
	    public int base() {return((int)base.get(mem, 0));}
	    private static final VarHandle latched = _XIModifierState.varHandle(PathElement.groupElement("latched"));
	    public int latched() {return((int)latched.get(mem, 0));}
	    private static final VarHandle locked = _XIModifierState.varHandle(PathElement.groupElement("locked"));
	    public int locked() {return((int)locked.get(mem, 0));}
	    private static final VarHandle effective = _XIModifierState.varHandle(PathElement.groupElement("effective"));
	    public int effective() {return((int)effective.get(mem, 0));}
	}

	private static final StructLayout _XIDeviceEvent = struct(new MemoryLayout[] {
		C_INT.withName("type"),
		C_LONG.withName("serial"),
		C_XBool.withName("send_event"),
		ADDRESS.withName("display"),
		C_INT.withName("extension"),
		C_INT.withName("evtype"),
		C_Time.withName("time"),
		C_INT.withName("deviceid"),
		C_INT.withName("sourceid"),
		C_INT.withName("detail"),
		C_XID.withName("root"),
		C_XID.withName("event"),
		C_XID.withName("child"),
		C_DOUBLE.withName("root_x"),
		C_DOUBLE.withName("root_y"),
		C_DOUBLE.withName("event_x"),
		C_DOUBLE.withName("event_y"),
		C_INT.withName("flags"),
		_XIButtonState.withName("buttons"),
		_XIValuatorState.withName("valuators"),
		_XIModifierState.withName("mods"),
		_XIModifierState.withName("group"),
	    });

	public static class XIDeviceEvent extends XInput.XIDeviceEvent {
	    public final XGenericEvent from;

	    private XIDeviceEvent(XGenericEvent from) {
		super(from.data());
		if(nullp(mem))
		    throw(new NullPointerException("event data"));
		this.from = from;
	    }

	    protected StructLayout $layout() {return(_XIDeviceEvent);}

	    private static final VarHandle evtype = _XIDeviceEvent.varHandle(PathElement.groupElement("evtype"));
	    public int evtype() {return((int)evtype.get(mem, 0));}
	    private static final VarHandle deviceid = _XIDeviceEvent.varHandle(PathElement.groupElement("deviceid"));
	    public int deviceid() {return((int)deviceid.get(mem, 0));}
	    private static final VarHandle sourceid = _XIDeviceEvent.varHandle(PathElement.groupElement("sourceid"));
	    public int sourceid() {return((int)sourceid.get(mem, 0));}
	    private static final VarHandle detail = _XIDeviceEvent.varHandle(PathElement.groupElement("detail"));
	    public int detail() {return((int)detail.get(mem, 0));}
	    private static final VarHandle root = _XIDeviceEvent.varHandle(PathElement.groupElement("root"));
	    public XID root() {return(XID.of((long)root.get(mem, 0)));}
	    private static final VarHandle event = _XIDeviceEvent.varHandle(PathElement.groupElement("event"));
	    public XID event() {return(XID.of((long)event.get(mem, 0)));}
	    private static final VarHandle child = _XIDeviceEvent.varHandle(PathElement.groupElement("child"));
	    public XID child() {return(XID.of((long)child.get(mem, 0)));}
	    private static final VarHandle root_x = _XIDeviceEvent.varHandle(PathElement.groupElement("root_x"));
	    public double root_x() {return((double)root_x.get(mem, 0));}
	    private static final VarHandle root_y = _XIDeviceEvent.varHandle(PathElement.groupElement("root_y"));
	    public double root_y() {return((double)root_y.get(mem, 0));}
	    private static final VarHandle event_x = _XIDeviceEvent.varHandle(PathElement.groupElement("event_x"));
	    public double event_x() {return((double)event_x.get(mem, 0));}
	    private static final VarHandle event_y = _XIDeviceEvent.varHandle(PathElement.groupElement("event_y"));
	    public double event_y() {return((double)event_y.get(mem, 0));}
	    private static final VarHandle flags = _XIDeviceEvent.varHandle(PathElement.groupElement("flags"));
	    public int flags() {return((int)flags.get(mem, 0));}
	    private static final long buttons = _XIDeviceEvent.byteOffset(PathElement.groupElement("buttons"));
	    public Collection<? extends Integer> buttons() {
		return(new XIButtonState(mem.asSlice(buttons, 0)).buttons());
	    }
	    private static final VarHandle valuators_mask_len = _XIDeviceEvent.varHandle(PathElement.groupElement("valuators"), PathElement.groupElement("mask_len"));
	    private static final VarHandle valuators_mask = _XIDeviceEvent.varHandle(PathElement.groupElement("valuators"), PathElement.groupElement("mask"));
	    private static final VarHandle valuators_values = _XIDeviceEvent.varHandle(PathElement.groupElement("valuators"), PathElement.groupElement("values"));
	    public Map<Integer, Double> valuators() {
		int ln = (int)valuators_mask_len.get(mem, 0);
		MemorySegment m = ((MemorySegment)valuators_mask.get(mem, 0)).reinterpret(ln);
		MemorySegment v = ((MemorySegment)valuators_values.get(mem, 0)).reinterpret(C_DOUBLE.byteSize() * ln * 8);
		Map<Integer, Double> ret = new HashMap<>();
		int nv = 0;
		for(int i = 0; i < ln; i++) {
		    int b = m.get(ValueLayout.JAVA_BYTE, i) & 0xff;
		    for(int o = 0; o < 8; o++) {
			if((b & (1 << o)) != 0)
			    ret.put((i * 8) + o, v.get(ValueLayout.JAVA_DOUBLE, C_DOUBLE.byteSize() * nv++));
		    }
		}
		return(ret);
	    }
	    private static final long mods = _XIDeviceEvent.byteOffset(PathElement.groupElement("mods"));
	    public XIModifierState mods() {return(new XIModifierState(mem.asSlice(mods, 0)));}
	    private static final long group = _XIDeviceEvent.byteOffset(PathElement.groupElement("group"));
	    public XIModifierState group() {return(new XIModifierState(mem.asSlice(group, 0)));}
	}

	public XIDeviceEvent XIDeviceEvent(XGenericEvent xev) {
	    return(new XIDeviceEvent(xev));
	}

	private static final StructLayout _XIFocusEvent = struct(new MemoryLayout[] {
		C_INT.withName("type"),
		C_LONG.withName("serial"),
		C_XBool.withName("send_event"),
		ADDRESS.withName("display"),
		C_INT.withName("extension"),
		C_INT.withName("evtype"),
		C_Time.withName("time"),
		C_INT.withName("deviceid"),
		C_INT.withName("sourceid"),
		C_INT.withName("detail"),
		C_XID.withName("root"),
		C_XID.withName("event"),
		C_XID.withName("child"),
		C_DOUBLE.withName("root_x"),
		C_DOUBLE.withName("root_y"),
		C_DOUBLE.withName("event_x"),
		C_DOUBLE.withName("event_y"),
		C_INT.withName("mode"),
		C_XBool.withName("focus"),
		C_XBool.withName("same_screen"),
		_XIButtonState.withName("buttons"),
		_XIModifierState.withName("mods"),
		_XIModifierState.withName("group"),
	    });
	public static class XIFocusEvent extends XInput.XIFocusEvent {
	    public final XGenericEvent from;

	    private XIFocusEvent(XGenericEvent from) {
		super(from.data());
		if(nullp(mem))
		    throw(new NullPointerException("event data"));
		this.from = from;
	    }

	    protected StructLayout $layout() {return(_XIFocusEvent);}

	    private static final VarHandle evtype = _XIFocusEvent.varHandle(PathElement.groupElement("evtype"));
	    public int evtype() {return((int)evtype.get(mem, 0));}
	    private static final VarHandle deviceid = _XIFocusEvent.varHandle(PathElement.groupElement("deviceid"));
	    public int deviceid() {return((int)deviceid.get(mem, 0));}
	    private static final VarHandle sourceid = _XIFocusEvent.varHandle(PathElement.groupElement("sourceid"));
	    public int sourceid() {return((int)sourceid.get(mem, 0));}
	    private static final VarHandle detail = _XIFocusEvent.varHandle(PathElement.groupElement("detail"));
	    public int detail() {return((int)detail.get(mem, 0));}
	    private static final VarHandle root = _XIFocusEvent.varHandle(PathElement.groupElement("root"));
	    public XID root() {return(XID.of((long)root.get(mem, 0)));}
	    private static final VarHandle event = _XIFocusEvent.varHandle(PathElement.groupElement("event"));
	    public XID event() {return(XID.of((long)event.get(mem, 0)));}
	    private static final VarHandle child = _XIFocusEvent.varHandle(PathElement.groupElement("child"));
	    public XID child() {return(XID.of((long)child.get(mem, 0)));}
	    private static final VarHandle root_x = _XIFocusEvent.varHandle(PathElement.groupElement("root_x"));
	    public double root_x() {return((double)root_x.get(mem, 0));}
	    private static final VarHandle root_y = _XIFocusEvent.varHandle(PathElement.groupElement("root_y"));
	    public double root_y() {return((double)root_y.get(mem, 0));}
	    private static final VarHandle event_x = _XIFocusEvent.varHandle(PathElement.groupElement("event_x"));
	    public double event_x() {return((double)event_x.get(mem, 0));}
	    private static final VarHandle event_y = _XIFocusEvent.varHandle(PathElement.groupElement("event_y"));
	    public double event_y() {return((double)event_y.get(mem, 0));}
	    private static final VarHandle mode = _XIFocusEvent.varHandle(PathElement.groupElement("mode"));
	    public int mode() {return((int)mode.get(mem, 0));}
	    private static final VarHandle focus = _XIFocusEvent.varHandle(PathElement.groupElement("focus"));
	    public boolean focus() {return(((int)focus.get(mem, 0)) != 0);}
	    private static final VarHandle same_screen = _XIFocusEvent.varHandle(PathElement.groupElement("same_screen"));
	    public boolean same_screen() {return(((int)same_screen.get(mem, 0)) != 0);}
	    private static final long buttons = _XIFocusEvent.byteOffset(PathElement.groupElement("buttons"));
	    public Collection<? extends Integer> buttons() {
		return(new XIButtonState(mem.asSlice(buttons, 0)).buttons());
	    }
	    private static final long mods = _XIFocusEvent.byteOffset(PathElement.groupElement("mods"));
	    public XIModifierState mods() {return(new XIModifierState(mem.asSlice(mods, 0)));}
	    private static final long group = _XIFocusEvent.byteOffset(PathElement.groupElement("group"));
	    public XIModifierState group() {return(new XIModifierState(mem.asSlice(group, 0)));}
	}

	public XIFocusEvent XIFocusEvent(XGenericEvent xev) {
	    return(new XIFocusEvent(xev));
	}
    }

    private static XInput instance = null;
    public static XInput get() {
	if(instance == null) {
	    synchronized(XInput.class) {
		if(instance == null)
		    instance = tryload("libXi", libXi_so_6::new);
	    }
	}
	return(instance);
    }
}
