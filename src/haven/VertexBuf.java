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

import java.nio.*;
import java.util.*;
import javax.media.opengl.*;

public class VertexBuf {
    public static final GLState.Slot<Binding> bound = new GLState.Slot<Binding>(GLState.Slot.Type.GEOM, Binding.class);
    public final AttribArray[] bufs;
    public final int num;
    
    public VertexBuf(AttribArray... bufs) {
	AttribArray[] na = new AttribArray[bufs.length];
	na[0] = bufs[0];
	int num = na[0].size();
	for(int i = 1; i < bufs.length; i++) {
	    na[i] = bufs[i];
	    if(na[i].size() != num)
		throw(new RuntimeException("Buffer sizes do not match"));
	}
	this.bufs = na;
	this.num = num;
    }
    
    public <T extends AttribArray> T buf(Class<T> type) {
	for(AttribArray a : bufs) {
	    if(type.isInstance(a))
		return(type.cast(a));
	}
	return(null);
    }

    public abstract class Binding extends GLState {
	public void prep(GLState.Buffer buf) {
	    buf.put(bound, this);
	}
    }

    public class MemBinding extends Binding {
	public void apply(GOut g) {
	    for(int i = 0; i < bufs.length; i++) {
		if(bufs[i] instanceof GLArray)
		    ((GLArray)bufs[i]).bind(g, false);
	    }
	}

	public void unapply(GOut g) {
	    for(int i = 0; i < bufs.length; i++) {
		if(bufs[i] instanceof GLArray)
		    ((GLArray)bufs[i]).unbind(g);
	    }
	}
    }

    public abstract static class AttribArray {
	public final int n;
	
	public AttribArray(int n) {
	    this.n = n;
	}
	
	public abstract Buffer data();
	public abstract Buffer direct();
	public abstract int elsize();
	
	public int size() {
	    Buffer b = data();
	    b.rewind();
	    return(b.capacity() / this.n);
	}

	/* XXX: It would be terribly nice if GLArray could be a
	 * multiply inhereted class and these could be put in it
	 * instead; but alas, this is Java. QQ */
	private GLBuffer bufobj;
	private int bufmode = GL.GL_STATIC_DRAW;
	private boolean update = false;

	public void bindvbo(GOut g) {
	    GL2 gl = g.gl;
	    synchronized(this) {
		if((bufobj != null) && (bufobj.gl != gl))
		    dispose();
		if(bufobj == null) {
		    bufobj = new GLBuffer(gl);
		    gl.glBindBuffer(GL.GL_ARRAY_BUFFER, bufobj.id);
		    Buffer data = data();
		    data.rewind();
		    gl.glBufferData(GL.GL_ARRAY_BUFFER, data.remaining() * elsize(), data, bufmode);
		    GOut.checkerr(gl);
		    update = false;
		} else if(update) {
		    gl.glBindBuffer(GL.GL_ARRAY_BUFFER, bufobj.id);
		    Buffer data = data();
		    data.rewind();
		    gl.glBufferData(GL.GL_ARRAY_BUFFER, data.remaining() * elsize(), data, bufmode);
		    update = false;
		} else {
		    gl.glBindBuffer(GL.GL_ARRAY_BUFFER, bufobj.id);
		}
	    }
	}

	public void vbomode(int mode) {
	    bufmode = mode;
	    dispose();
	}

	public void dispose() {
	    synchronized(this) {
		if(bufobj != null) {
		    bufobj.dispose();
		    bufobj = null;
		}
	    }
	}

	public void update() {
	    update = true;
	}
    }

    public static interface GLArray {
	public void bind(GOut g, boolean asvbo);
	public void unbind(GOut g);
	public Object progid(GOut g);
    }

    public abstract static class FloatArray extends AttribArray {
	public FloatBuffer data;
	
	public FloatArray(int n, FloatBuffer data) {
	    super(n);
	    data.rewind();
	    if(data.capacity() % n != 0)
		throw(new RuntimeException(String.format("float-array length %d does not match element count %d", data.capacity(), n)));
	    this.data = data;
	}
	
	public FloatBuffer data() {return(data);}
	public FloatBuffer direct() {
	    if(!data.isDirect())
		data = Utils.bufcp(data);
	    return(data);
	}
	public int elsize() {return(4);}
    }
    
    public abstract static class IntArray extends AttribArray {
	public IntBuffer data;
	
	public IntArray(int n, IntBuffer data) {
	    super(n);
	    data.rewind();
	    if(data.capacity() % n != 0)
		throw(new RuntimeException(String.format("int-array length %d does not match element count %d", data.capacity(), n)));
	    this.data = data;
	}
	
	public IntBuffer data() {return(data);}
	public IntBuffer direct() {
	    if(!data.isDirect())
		data = Utils.bufcp(data);
	    return(data);
	}
	public int elsize() {return(4);}
    }
    
    public static class VertexArray extends FloatArray implements GLArray {
	public VertexArray(FloatBuffer data) {
	    super(3, data);
	}
	
	public VertexArray dup() {return(new VertexArray(Utils.bufcp(data)));}
	
	public void bind(GOut g, boolean asvbo) {
	    GL2 gl = g.gl;
	    if(asvbo) {
		bindvbo(g);
		gl.glVertexPointer(3, GL.GL_FLOAT, 0, 0);
		gl.glBindBuffer(GL.GL_ARRAY_BUFFER, 0);
	    } else {
		data.rewind();
		gl.glVertexPointer(3, GL.GL_FLOAT, 0, direct());
	    }
	    gl.glEnableClientState(GL2.GL_VERTEX_ARRAY);
	}
	
	public void unbind(GOut g) {
	    g.gl.glDisableClientState(GL2.GL_VERTEX_ARRAY);
	}

	public Object progid(GOut g) {return(null);}
    }
    
    public static class NormalArray extends FloatArray implements GLArray {
	public NormalArray(FloatBuffer data) {
	    super(3, data);
	}
	
	public NormalArray dup() {return(new NormalArray(Utils.bufcp(data)));}

	public void bind(GOut g, boolean asvbo) {
	    GL2 gl = g.gl;
	    if(asvbo) {
		bindvbo(g);
		gl.glNormalPointer(GL.GL_FLOAT, 0, 0);
		gl.glBindBuffer(GL.GL_ARRAY_BUFFER, 0);
	    } else {
		data.rewind();
		gl.glNormalPointer(GL.GL_FLOAT, 0, direct());
	    }
	    gl.glEnableClientState(GL2.GL_NORMAL_ARRAY);
	}
	
	public void unbind(GOut g) {
	    g.gl.glDisableClientState(GL2.GL_NORMAL_ARRAY);
	}

	public Object progid(GOut g) {return(null);}
    }

    public static class ColorArray extends FloatArray implements GLArray {
	public ColorArray(FloatBuffer data) {
	    super(4, data);
	}
	
	public ColorArray dup() {return(new ColorArray(Utils.bufcp(data)));}

	public void bind(GOut g, boolean asvbo) {
	    GL2 gl = g.gl;
	    if(asvbo) {
		bindvbo(g);
		gl.glColorPointer(4, GL.GL_FLOAT, 0, 0);
		gl.glBindBuffer(GL.GL_ARRAY_BUFFER, 0);
	    } else {
		data.rewind();
		gl.glColorPointer(4, GL.GL_FLOAT, 0, direct());
	    }
	    gl.glEnableClientState(GL2.GL_COLOR_ARRAY);
	}
	
	public void unbind(GOut g) {
	    g.gl.glDisableClientState(GL2.GL_COLOR_ARRAY);
	}

	public Object progid(GOut g) {return(null);}
    }

    public static class TexelArray extends FloatArray implements GLArray {
	public TexelArray(FloatBuffer data) {
	    super(2, data);
	}

	public TexelArray dup() {return(new TexelArray(Utils.bufcp(data)));}

	public void bind(GOut g, boolean asvbo) {
	    GL2 gl = g.gl;
	    if(asvbo) {
		bindvbo(g);
		gl.glTexCoordPointer(2, GL.GL_FLOAT, 0, 0);
		gl.glBindBuffer(GL.GL_ARRAY_BUFFER, 0);
	    } else {
		data.rewind();
		gl.glTexCoordPointer(2, GL.GL_FLOAT, 0, direct());
	    }
	    gl.glEnableClientState(GL2.GL_TEXTURE_COORD_ARRAY);
	}
	
	public void unbind(GOut g) {
	    g.gl.glDisableClientState(GL2.GL_TEXTURE_COORD_ARRAY);
	}

	public Object progid(GOut g) {return(null);}
    }

    public static class NamedFloatArray extends FloatArray implements GLArray {
	public final haven.glsl.Attribute attr;
	private int bound = -1;

	public NamedFloatArray(int n, FloatBuffer data, haven.glsl.Attribute attr) {
	    super(n, data);
	    this.attr = attr;
	}

	public void bind(GOut g, boolean asvbo) {
	    if(g.st.prog != null) {
		if((bound = g.st.prog.cattrib(attr)) != -1) {
		    GL2 gl = g.gl;
		    if(asvbo) {
			bindvbo(g);
			gl.glVertexAttribPointer(bound, n, GL2.GL_FLOAT, false, 0, 0);
			gl.glBindBuffer(GL.GL_ARRAY_BUFFER, 0);
		    } else {
			data.rewind();
			gl.glVertexAttribPointer(bound, n, GL2.GL_FLOAT, false, 0, direct());
		    }
		    gl.glEnableVertexAttribArray(bound);
		}
	    }
	}

	public void unbind(GOut g) {
	    if(bound != -1) {
		g.gl.glDisableVertexAttribArray(bound);
		bound = -1;
	    }
	}

	public Object progid(GOut g) {
	    if(g.st.prog == null)
		return(null);
	    return(Integer.valueOf(g.st.prog.cattrib(attr)));
	}
    }

    public static class Vec1Array extends NamedFloatArray implements GLArray {
	public Vec1Array(FloatBuffer data, haven.glsl.Attribute attr) {
	    super(1, data, attr);
	}
    }
    public static class Vec2Array extends NamedFloatArray implements GLArray {
	public Vec2Array(FloatBuffer data, haven.glsl.Attribute attr) {
	    super(2, data, attr);
	}
    }
    public static class Vec3Array extends NamedFloatArray implements GLArray {
	public Vec3Array(FloatBuffer data, haven.glsl.Attribute attr) {
	    super(3, data, attr);
	}
    }
    public static class Vec4Array extends NamedFloatArray implements GLArray {
	public Vec4Array(FloatBuffer data, haven.glsl.Attribute attr) {
	    super(4, data, attr);
	}
    }

    public void dispose() {
	for(AttribArray buf : bufs)
	    buf.dispose();
    }

    @Resource.LayerName("vbuf")
    public static class VertexRes extends Resource.Layer {
	public transient final VertexBuf b;
	
	public VertexRes(Resource res, byte[] buf) {
	    res.super();
	    ArrayList<AttribArray> bufs = new ArrayList<AttribArray>();
	    int fl = Utils.ub(buf[0]);
	    int num = Utils.uint16d(buf, 1);
	    int off = 3;
	    while(off < buf.length) {
		int id = Utils.ub(buf[off++]);
		if(id == 0) {
		    FloatBuffer data = Utils.wfbuf(num * 3);
		    for(int i = 0; i < num * 3; i++)
			data.put((float)Utils.floatd(buf, off + (i * 5)));
		    off += num * 5 * 3;
		    bufs.add(new VertexArray(data));
		} else if(id == 1) {
		    FloatBuffer data = Utils.wfbuf(num * 3);
		    for(int i = 0; i < num * 3; i++)
			data.put((float)Utils.floatd(buf, off + (i * 5)));
		    off += num * 5 * 3;
		    bufs.add(new NormalArray(data));
		} else if(id == 2) {
		    FloatBuffer data = Utils.wfbuf(num * 2);
		    for(int i = 0; i < num * 2; i++)
			data.put((float)Utils.floatd(buf, off + (i * 5)));
		    off += num * 5 * 2;
		    bufs.add(new TexelArray(data));
		} else if(id == 3) {
		    int mba = Utils.ub(buf[off++]);
		    IntBuffer ba = Utils.wibuf(num * mba);
		    for(int i = 0; i < num * mba; i++)
			ba.put(-1);
		    ba.rewind();
		    FloatBuffer bw = Utils.wfbuf(num * mba);
		    int[] na = new int[num];
		    List<String> bones = new ArrayList<String>();
		    while(true) {
			int[] ob = {off};
			String bone = Utils.strd(buf, ob);
			off = ob[0];
			if(bone.length() == 0)
			    break;
			int bidx = bones.size();
			bones.add(bone);
			while(true) {
			    int run = Utils.uint16d(buf, off); off += 2;
			    int st = Utils.uint16d(buf, off); off += 2;
			    if(run == 0)
				break;
			    for(int i = 0; i < run; i++) {
				float w = (float)Utils.floatd(buf, off);
				off += 5;
				int v = i + st;
				int cna = na[v]++;
				if(cna >= mba)
				    continue;
				bw.put(v * mba + cna, w);
				ba.put(v * mba + cna, bidx);
			    }
			}
		    }
		    normweights(bw, ba, mba);
		    bufs.add(new MorphedMesh.MorphedBuf.BoneArray(mba, ba, bones.toArray(new String[0])));
		    bufs.add(new MorphedMesh.MorphedBuf.WeightArray(mba, bw));
		}
	    }
	    this.b = new VertexBuf(bufs.toArray(new AttribArray[0]));
	}
	
	private static void normweights(FloatBuffer bw, IntBuffer ba, int mba) {
	    int i = 0;
	    while(i < bw.capacity()) {
		float tw = 0.0f;
		int n = 0;
		for(int o = 0; o < mba; o++) {
		    if(ba.get(i + o) < 0)
			break;
		    tw += bw.get(i + o);
		    n++;
		}
		if(tw != 1.0f) {
		    for(int o = 0; o < n; o++)
			bw.put(i + o, bw.get(i + o) / tw);
		}
		i += mba;
	    }
	}
	
	public void init() {}
    }
}
