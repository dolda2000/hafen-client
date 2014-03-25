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

public class MorphedMesh extends FastMesh {
    private static Map<Morpher.Factory, Collection<MorphedBuf>> bufs = new CacheMap<Morpher.Factory, Collection<MorphedBuf>>(CacheMap.RefType.WEAK);
    
    private static MorphedBuf buf(VertexBuf buf, Morpher.Factory morph) {
	Collection<MorphedBuf> bl;
	synchronized(bufs) {
	    bl = bufs.get(morph);
	    if(bl == null)
		bufs.put(morph, bl = new LinkedList<MorphedBuf>());
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
        super(mesh, buf(mesh.vert, pose));
    }
    
    public boolean setup(RenderList rl) {
	((MorphedBuf)vert).update();
	return(super.setup(rl));
    }
    
    protected boolean compile() {
	return(false);
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

    public static class MorphedBuf extends VertexBuf {
	public final VertexBuf from;
	private final Morpher morph;
	
	private static AttribArray[] ohBitterSweetJavaDays(VertexBuf from) {
	    AttribArray[] ret = new AttribArray[from.bufs.length];
	    for(int i = 0; i < from.bufs.length; i++) {
		if(from.bufs[i] instanceof VertexArray) {
		    ret[i] = ((VertexArray)from.bufs[i]).dup();
		    ret[i].vbomode(javax.media.opengl.GL.GL_DYNAMIC_DRAW);
		} else if(from.bufs[i] instanceof NormalArray) {
		    ret[i] = ((NormalArray)from.bufs[i]).dup();
		    ret[i].vbomode(javax.media.opengl.GL.GL_DYNAMIC_DRAW);
		} else if(from.bufs[i] instanceof PoseMorph.BoneArray) {
		    ret[i] = ((PoseMorph.BoneArray)from.bufs[i]).dup();
		} else {
		    ret[i] = from.bufs[i];
		}
	    }
	    return(ret);
	}

	private MorphedBuf(VertexBuf buf, Morpher.Factory morph) {
	    super(ohBitterSweetJavaDays(buf));
	    this.from = buf;
	    this.morph = morph.create(this);
	}
	
	public void update() {
	    if(!morph.update())
		return;
	    VertexBuf.VertexArray apos = buf(VertexArray.class);
	    VertexBuf.NormalArray anrm = buf(NormalArray.class);
	    FloatBuffer opos = from.buf(VertexArray.class).data, onrm = from.buf(NormalArray.class).data;
	    FloatBuffer npos =                        apos.data, nnrm =                        anrm.data;
	    morph.morphp(npos, opos);
	    morph.morphd(nnrm, onrm);
	    apos.update(); anrm.update();
	}
    }

    public static Morpher.Factory combine(final Morpher.Factory... parts) {
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
