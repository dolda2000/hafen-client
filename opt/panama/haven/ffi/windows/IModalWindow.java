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
import haven.ffi.windows.Win32.*;
import haven.ffi.windows.COM.*;
import static java.lang.foreign.ValueLayout.ADDRESS;

public interface IModalWindow extends IUnknown {
    public static final IID<IModalWindow> IID = new IID<>(IModalWindow.class, 0xb4db1657_70d7_485el, 0x8e3e_6fcb5a5c1802l);
    static final COM com = COM.get();

    public static final Descriptor sig_Show = new Descriptor(3, FunctionDescriptor.of(com.HRESULT(), ADDRESS, com.HWND()));
    public void Show(Handle hwndOwner);
}
