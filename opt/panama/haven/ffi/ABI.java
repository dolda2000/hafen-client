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

package haven.ffi;

import haven.*;
import java.lang.foreign.*;
import java.util.*;
import java.nio.charset.*;

public class ABI {
    public static final Charset C_CHARSET = Charset.defaultCharset();
    public static final Linker ld = ld();
    public static final Map<String, MemoryLayout> systypes = (ld != null) ? ld.canonicalLayouts() : new FallbackLayouts();
    public static final MemoryLayout C_CHAR = systypes.get("char");
    public static final MemoryLayout C_SHORT = systypes.get("short");
    public static final MemoryLayout C_INT = systypes.get("int");
    public static final MemoryLayout C_LONG = systypes.get("long");
    public static final MemoryLayout C_FLOAT = systypes.get("float");
    public static final MemoryLayout C_DOUBLE = systypes.get("double");
    public static final MemoryLayout SIZE_T = systypes.get("size_t");
    public static final MemoryLayout C_ENUM = C_INT;
    public static final MemoryLayout PTRINT_T = (ValueLayout.ADDRESS.byteSize() == 8) ? ValueLayout.JAVA_LONG : ValueLayout.JAVA_INT;
    /* XXX? This is kind of cheating, but not being able to handle invoke() calls with flexible typing is quite restricting... */
    public static final MemoryLayout C_BOOL = C_CHAR;

    private static final Linker ld() {
	try {
	    return(Linker.nativeLinker());
	} catch(UnsupportedOperationException e) {
	    Warning.warn("FFM not supported");
	    return(null);
	} catch(Exception e) {
	    new Warning(e, "FFM initializeation failed").issue();
	    return(null);
	}
    }

    private static class FallbackLayouts extends AbstractMap<String, MemoryLayout> {
	public static final MemoryLayout invalid = MemoryLayout.paddingLayout(1);

	public Set<Map.Entry<String, MemoryLayout>> entrySet() {
	    return(Collections.emptySet());
	}

	public MemoryLayout get(Object key) {
	    return(invalid);
	}

	public boolean containsKey() {
	    return(true);
	}
    }
}
