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

package haven.render.sl;

import java.util.*;
import haven.*;

public interface ShaderMacro {
    public void modify(ProgramContext prog);

    public static final ShaderMacro nil = new ShaderMacro() {
	    public void modify(ProgramContext prog) {}
	    public String toString() {return("nil");}
	};

    public static class Composed implements ShaderMacro {
	private static final WeakHashedSet<ShaderMacro> composed = new WeakHashedSet<>(Hash.eq);
	public final Collection<ShaderMacro> smacs;

	public Composed(Collection<ShaderMacro> smacs) {
	    this.smacs = smacs;
	}

	public void modify(ProgramContext prog) {
	    for(ShaderMacro smac : smacs)
		smac.modify(prog);
	}

	public String toString() {
	    return(smacs.toString());
	}

	public boolean equals(Object that) {
	    return((that instanceof Composed) && (((Composed)that).smacs.equals(this.smacs)));
	}

	public int hashCode() {
	    return(smacs.hashCode());
	}
    }

    public static ShaderMacro compose(Collection<ShaderMacro> smacs) {
	if(smacs.isEmpty())
	    return(nil);
	synchronized(Composed.composed) {
	    return(Composed.composed.intern(new Composed(smacs)));
	}
    }

    public static ShaderMacro compose(ShaderMacro... smacs) {
	return(compose(Arrays.asList(smacs)));
    }

    public static final ShaderMacro dump = new ShaderMacro() {
	    public void modify(ProgramContext prog) {
		prog.dump = true;
	    }
	};
}
