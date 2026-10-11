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

import java.util.*;
import java.lang.invoke.*;
import java.lang.foreign.*;
import haven.ffi.windows.Win32.*;
import haven.ffi.windows.COM.*;
import static haven.ffi.ABI.*;
import static java.lang.foreign.ValueLayout.ADDRESS;

public interface IEnumFORMATETC extends IUnknown, IEnum<COM.FORMATETC> {
    public static final IID<IEnumFORMATETC> IID = new IID<>(IEnumFORMATETC.class, 0x00000103_0000_0000l, 0xc000_000000000046l);

    public static final Descriptor sig_Next = IEnum.sig_Next(COM.get().C_FORMATETC(), COM.get()::FORMATETC_fromcom);
    public List<FORMATETC> Next(int count);
    public static final Descriptor sig_Skip = IEnum.sig_Skip(COM.get().C_FORMATETC());
    public boolean Skip(int count);
    public static final Descriptor sig_Reset = IEnum.sig_Reset(COM.get().C_FORMATETC());
    public void Reset();
    public static final Descriptor sig_Clone = IEnum.sig_Clone(COM.get().C_FORMATETC());
    public IEnumFORMATETC Clone();
}
