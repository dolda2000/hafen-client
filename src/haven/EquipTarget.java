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

package haven;

import haven.render.*;
import java.util.function.*;

public interface EquipTarget {
    public final Supplier<Pipe.Op> nil = () -> Pipe.Op.nil;

    public Supplier<? extends Pipe.Op> eqpoint(String nm, Message dat);

    public static class NoSuchTarget extends IllegalArgumentException {
	public final String tgt, nm, ctx;

	public NoSuchTarget(EquipTarget tgt, String nm, Object ctx) {
	    this.tgt = String.valueOf(tgt);
	    this.nm = nm;
	    this.ctx = (ctx == null) ? null : String.valueOf(ctx);
	}

	public String getMessage() {
	    if(ctx == null)
		return(String.format("No such eqpoint: %s on %s", nm, tgt));
	    return(String.format("No such eqpoint: %s on %s, from %s", nm, tgt, ctx));
	}
    }

    public static Supplier<? extends Pipe.Op> eqpoint(EquipTarget tgt, String nm, Message dat, Object ctx) {
	if(tgt == null)
	    throw(new NoSuchTarget(null, nm, ctx));
	Supplier<? extends Pipe.Op> ret = tgt.eqpoint(nm, dat);
	if(ret == null)
	    throw(new NoSuchTarget(tgt, nm, ctx));
	return(ret);
    }

    public static Supplier<? extends Pipe.Op> eqpoint(EquipTarget tgt, String nm, Message dat) {
	return(eqpoint(tgt, nm, dat, null));
    }
}
