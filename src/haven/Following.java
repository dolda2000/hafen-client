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

public class Following extends Moving {
    int tgt;
    float zo;
    double lastv = 0.0;
	
    public Following(Gob gob, int tgt, float zo) {
	super(gob);
	this.tgt = tgt;
	this.zo = zo;
    }
	
    public Coord3f getc() {
	Gob tgt = gob.glob.oc.getgob(this.tgt);
	if(tgt == null)
	    return(gob.getrc());
	return(tgt.getc().add(0.0f, 0.0f, zo));
    }
    
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
}
