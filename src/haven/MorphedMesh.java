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

import java.util.*;
import java.nio.*;
import java.lang.ref.*;
import haven.render.*;

public class MorphedMesh extends FastMesh {
    private static Map<Morpher.Factory, Collection<MorphedBuf>> bufs = new CacheMap<Morpher.Factory, Collection<MorphedBuf>>(CacheMap.RefType.WEAK);
    public final FastMesh from;
    
    private static MorphedBuf buf(VertexBuf buf, Morpher.Factory morph) {
	Collection<MorphedBuf> bl;
	synchronized(bufs) {
	    bl = bufs.get(morph);
	    if(bl == null)
		bufs.put(morph, bl = new ArrayList<MorphedBuf>(1));
	}
	synchronized(bl) {
	    for(MorphedBuf b : bl) {
		if(b.from == buf)
		    return(b);
	    }
	    MorphedBuf b = new MorphedBuf(buf, morph);
	    bl.add(b);
	    return(b);
	}
    }
    
    public MorphedMesh(FastMesh mesh, Morpher.Factory pose) {
	/* XXX: Would be nice to be able to reuse the index buffer as-is. */
        super(buf(mesh.vert, pose), mesh.indb);
	this.from = mesh;
    }
    
    public void update(Render g) {
	((MorphedBuf)vert).mupdate(g);
    }

    public String toString() {
	return("morphed(" + from + ")");
    }

    public static interface Morpher {
	public static interface Factory {
	    public Morpher create(MorphedBuf buf);
	}

	public boolean update();
	public void morphp(FloatBuffer dst, FloatBuffer src);
	public void morphd(FloatBuffer dst, FloatBuffer src);
    }

    public static enum MorphType {
	NONE, DUP, POS, DIR
    }

    public static interface MorphData {
	public MorphType morphtype();
	public VertexBuf.AttribData dup();
    }

    public static class MorphedBuf extends VertexBuf {
	public final VertexBuf from;
	private final Morpher morph;
	private final Pair[] parrays, darrays;

	private static class Pair {
	    final FloatData o, n;
	    Pair(FloatData o, FloatData n) {this.o = o; this.n = n;}
	}

	private static AttribData[] ohBitterSweetJavaDays(VertexBuf from, Collection<Pair> pos, Collection<Pair> dir) {
	    AttribData[] ret = new AttribData[from.bufs.length];
	    for(int i = 0; i < from.bufs.length; i++) {
		MorphType type = (from.bufs[i] instanceof MorphData) ? ((MorphData)from.bufs[i]).morphtype() : MorphType.NONE;
		if(type == MorphType.NONE) {
		    ret[i] = from.bufs[i];
		} else {
		    ret[i] = ((MorphData)from.bufs[i]).dup();
		    if(type == MorphType.POS) {
			pos.add(new Pair((FloatData)from.bufs[i], (FloatData)ret[i]));
			// XXXRENDER ret[i].vbomode(javax.media.opengl.GL.GL_DYNAMIC_DRAW);
		    } else if(type == MorphType.DIR) {
			dir.add(new Pair((FloatData)from.bufs[i], (FloatData)ret[i]));
			// XXXRENDER ret[i].vbomode(javax.media.opengl.GL.GL_DYNAMIC_DRAW);
		    }
		}
	    }
	    return(ret);
	}

	private MorphedBuf(VertexBuf buf, Morpher.Factory morph, Collection<Pair> pos, Collection<Pair> dir) {
	    super(ohBitterSweetJavaDays(buf, pos, dir));
	    this.from = buf;
	    this.morph = morph.create(this);
	    this.parrays = pos.toArray(new Pair[0]);
	    this.darrays = dir.toArray(new Pair[0]);
	}

	private MorphedBuf(VertexBuf buf, Morpher.Factory morph) {
	    this(buf, morph, new LinkedList<Pair>(), new LinkedList<Pair>());
	}

	public void mupdate(Render g) {
	    if(!morph.update())
		return;
	    for(Pair p : parrays)
		morph.morphp(p.n.data, p.o.data);
	    for(Pair p : darrays)
		morph.morphd(p.n.data, p.o.data);
	    update(g);
	}
    }

    public static Morpher.Factory combine(final Morpher.Factory... parts) {
	if(parts.length == 1)
	    return(parts[0]);
	return(new Morpher.Factory() {
		public Morpher create(MorphedBuf vb) {
		    final Morpher[] mparts = new Morpher[parts.length];
		    for(int i = 0; i < parts.length; i++)
			mparts[i] = parts[i].create(vb);
		    return(new Morpher() {
			    public boolean update() {
				boolean ret = false;
				for(Morpher p : mparts) {
				    if(p.update())
					ret = true;
				}
				return(ret);
			    }

			    public void morphp(FloatBuffer dst, FloatBuffer src) {
				for(Morpher p : mparts) {
				    p.morphp(dst, src);
				    src = dst;
				}
			    }

			    public void morphd(FloatBuffer dst, FloatBuffer src) {
				for(Morpher p : mparts) {
				    p.morphd(dst, src);
				    src = dst;
				}
			    }
			});
		}
	    });
    }
}
