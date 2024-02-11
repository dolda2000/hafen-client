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

import java.util.function.*;
import haven.render.*;

public class Following extends Moving {
    public final long tgt;
    public final Indir<Resource> xfres;
    public final String xfname;

    public Following(Gob gob, long tgt, Indir<Resource> xfres, String xfname) {
	super(gob);
	this.tgt = tgt;
	this.xfres = xfres;
	this.xfname = xfname;
    }

    public Coord3f getc() {
	Gob tgt = gob.glob.oc.getgob(this.tgt);
	if(tgt == null)
	    return(gob.getrc());
	return(tgt.getc());
    }

    private double lastv = 0.0;
    public double getv() {
	Gob tgt = gob.glob.oc.getgob(this.tgt);
	if(tgt != null) {
	    Moving mv = tgt.getattr(Moving.class);
	    if(mv == null)
		lastv = 0.0;
	    else
		lastv = mv.getv();
	}
	return(lastv);
    }

    public Gob tgt() {
	return(gob.glob.oc.getgob(this.tgt));
    }

    private class XfResolver implements Runnable {
	final Gob tgt;
	Supplier<? extends Pipe.Op> xf = null;

	XfResolver(Gob tgt) {
	    this.tgt = tgt;
	    tgt.defer(this);
	}

	public void run() {
	    this.xf = xfres.get().flayer(Skeleton.BoneOffset.class, xfname).from(tgt);
	}
    }

    private Pipe.Op xf = null, lpxf = null, lbxf = null;
    private XfResolver bxf = null;
    public Pipe.Op xf() {
	synchronized(this) {
	    Gob tgt = tgt();
	    if(tgt != null) {
		if((bxf == null) || (tgt != bxf.tgt))
		    bxf = new XfResolver(tgt);
	    } else {
		bxf = null;
	    }
	    Pipe.Op cpxf = (tgt == null) ? null : tgt.placed.placement();
	    Pipe.Op cbxf = ((bxf == null) || (bxf.xf == null)) ? null : bxf.xf.get();
	    if((xf == null) || !Utils.eq(cbxf, lbxf) || !Utils.eq(cpxf, lpxf)) {
		xf = Pipe.Op.compose(cpxf, cbxf);
		lpxf = cpxf;
		lbxf = cbxf;
	    }
	}
	return(xf);
    }

    @OCache.DeltaType(OCache.OD_FOLLOW)
    public static class $follow implements OCache.Delta {
	public void apply(Gob g, OCache.AttrDelta msg) {
	    long oid = msg.uint32();
	    if(oid != 0xffffffffl) {
		Indir<Resource> xfres = OCache.Delta.getres(g, msg.uint16());
		String xfname = msg.string();
		g.setattr(new Following(g, oid, xfres, xfname));
	    } else {
		g.delattr(Following.class);
	    }
	}
    }
}
