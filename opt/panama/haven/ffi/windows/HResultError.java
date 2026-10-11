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

import haven.ffi.*;

public class HResultError extends RuntimeException {
    public static final int DRAGDROP_S_CANCEL   = 0x00040101;
    public static final int E_NOTIMPL           = 0x80004001;
    public static final int E_NOINTERFACE       = 0x80004002;
    public static final int E_FAIL              = 0x80004005;
    public static final int REGDB_E_CLASSNOTDEF = 0x80040154;
    public static final int ERROR_CANCELLED     = 0x800704c7;
    public final int hresult;

    public HResultError(int hresult) {
	this.hresult = hresult;
    }

    public String getMessage() {
	return(FUtils.constname(HResultError.class, hresult));
    }
}
