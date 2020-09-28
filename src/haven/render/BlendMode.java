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

package haven.render;

import haven.*;
import java.util.*;

public class BlendMode {
    public final Function cfn, afn;
    public final Factor csrc, cdst, asrc, adst;
    public final FColor color;

    public enum Function {
	ADD, SUB, RSUB, MIN, MAX;
    }
    public enum Factor {
	ZERO, ONE,
	SRC_COLOR, DST_COLOR, INV_SRC_COLOR, INV_DST_COLOR,
	SRC_ALPHA, DST_ALPHA, INV_SRC_ALPHA, INV_DST_ALPHA,
	CONST_COLOR, INV_CONST_COLOR, CONST_ALPHA, INV_CONST_ALPHA,
    }

    public BlendMode(Function cfn, Factor csrc, Factor cdst, Function afn, Factor asrc, Factor adst, FColor color) {
	this.cfn = cfn; this.csrc = csrc; this.cdst = cdst;
	this.afn = afn; this.asrc = asrc; this.adst = adst;
	this.color = color;
    }

    public BlendMode(Function cfn, Factor csrc, Factor cdst, Function afn, Factor asrc, Factor adst) {
	this(cfn, csrc, cdst, afn, asrc, adst, null);
    }
    public BlendMode(Factor csrc, Factor cdst, Factor asrc, Factor adst) {
	this(Function.ADD, csrc, cdst, Function.ADD, asrc, adst);
    }
    public BlendMode(Function fn, Factor src, Factor dst) {
	this(fn, src, dst, fn, src, dst);
    }
    public BlendMode(Factor src, Factor dst) {
	this(Function.ADD, src, dst);
    }
    public BlendMode() {
	this(Factor.SRC_ALPHA, Factor.INV_SRC_ALPHA);
    }

    public int hashCode() {
	return(Objects.hash(cfn, csrc, cdst, afn, asrc, adst, color));
    }

    public boolean equals(Object o) {
	if(!(o instanceof BlendMode))
	    return(false);
	BlendMode that = (BlendMode)o;
	return((this.cfn == that.cfn) && (this.csrc == that.csrc) && (this.cdst == that.cdst) &&
	       (this.afn == that.afn) && (this.asrc == that.asrc) && (this.adst == that.adst) &&
	       Utils.eq(this.color, that.color));
    }

    public String toString() {return(String.format("#<blend-mode %s(%s, %s) %s(%s %s)>", cfn, csrc, cdst, afn, asrc, adst));}
}
