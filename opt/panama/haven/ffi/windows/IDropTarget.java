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

import java.lang.invoke.*;
import java.lang.foreign.*;
import haven.*;
import haven.ffi.windows.Win32.*;
import haven.ffi.windows.COM.*;
import static haven.ffi.ABI.*;
import static java.lang.foreign.ValueLayout.ADDRESS;

public interface IDropTarget extends IUnknown {
    public static final IID<IDropTarget> IID = new IID<>(IDropTarget.class, 0x00000122_0000_0000l, 0xc000_000000000046l);
    static final Win32 win = Win32.get();
    static final COM com = COM.get();

    public static final int DROPEFFECT_NONE = 0;
    public static final int DROPEFFECT_COPY = 1;
    public static final int DROPEFFECT_MOVE = 2;
    public static final int DROPEFFECT_LINK = 4;
    public static final int DROPEFFECT_SCROLL = 0x80000000;

    public static final Descriptor sig_DragEnter = new Descriptor(3, FunctionDescriptor.of(com.HRESULT(), ADDRESS, ADDRESS, com.DWORD(), win.C_POINT(), ADDRESS), com.DWORD());
    public default int DragEnter(IDataObject dataobj, int keystate, Coord pt) {throw(new HResultError(HResultError.E_NOTIMPL));}
    public static final Descriptor sig_DragOver = new Descriptor(4, FunctionDescriptor.of(com.HRESULT(), ADDRESS, com.DWORD(), win.C_POINT(), ADDRESS), com.DWORD());
    public default int DragOver(int keystate, Coord pt) {throw(new HResultError(HResultError.E_NOTIMPL));}
    public static final Descriptor sig_DragLeave = new Descriptor(5, FunctionDescriptor.of(com.HRESULT(), ADDRESS));
    public default void DragLeave() {}
    public static final Descriptor sig_Drop = new Descriptor(6, FunctionDescriptor.of(com.HRESULT(), ADDRESS, ADDRESS, com.DWORD(), win.C_POINT(), ADDRESS), com.DWORD());
    public default int Drop(IDataObject dataobj, int keystate, Coord pt) {throw(new HResultError(HResultError.E_NOTIMPL));}
}
