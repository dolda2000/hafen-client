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
import static haven.ffi.ABI.*;
import static java.lang.foreign.ValueLayout.ADDRESS;

public interface IShellItem extends IUnknown {
    public static final IID<IShellItem> IID = new IID<>(IShellItem.class, 0x43826d1e_e718_42eel, 0xbc55_a1e261c37bfel);

    public static final int SIGDN_NORMALDISPLAY = 0;
    public static final int SIGDN_PARENTRELATIVEPARSING = 0x80010001;
    public static final int SIGDN_DESKTOPABSOLUTEPARSING = 0x80028000;
    public static final int SIGDN_PARENTRELATIVEEDITING = 0x80031001;
    public static final int SIGDN_DESKTOPABSOLUTEEDITING = 0x8004c000;
    public static final int SIGDN_FILESYSPATH = 0x80058000;
    public static final int SIGDN_URL = 0x80068000;
    public static final int SIGDN_PARENTRELATIVEFORADDRESSBAR = 0x8007c001;
    public static final int SIGDN_PARENTRELATIVE = 0x80080001;
    public static final int SIGDN_PARENTRELATIVEFORUI = 0x80094001;

    public static final Descriptor sig_GetParent = new Descriptor(4, FunctionDescriptor.of(com.HRESULT(), ADDRESS, ADDRESS), ADDRESS);
    public IShellItem GetParent();
    public static final Descriptor sig_GetDisplayName = new Descriptor(5, FunctionDescriptor.of(com.HRESULT(), ADDRESS, C_ENUM, ADDRESS), ADDRESS);
    public String GetDisplayName(int name);
    public static final Descriptor sig_GetAttributes = new Descriptor(6, FunctionDescriptor.of(com.HRESULT(), ADDRESS, C_ENUM, ADDRESS), C_ENUM);
    public int GetAttributes(int mask);
    public static final Descriptor sig_Compare = new Descriptor(7, FunctionDescriptor.of(com.HRESULT(), ADDRESS, ADDRESS, C_ENUM, ADDRESS), C_INT);
    public int Compare(IShellItem si, int hint);
}
