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
import java.lang.annotation.*;
import haven.render.*;
import haven.render.VertexArray.Layout;
import haven.render.sl.Attribute;

public class VertexBuf {
    public final AttribData[] bufs;
    public final int num;
    private VertexArray data = null;
    private VertexArray.Buffer dbuf = null;

    public VertexBuf(AttribData... bufs) {
	AttribData[] na = new AttribData[bufs.length];
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

    public <T extends AttribData> T buf(Class<T> type) {
	for(AttribData a : bufs) {
	    if(type.isInstance(a))
		return(type.cast(a));
	}
	return(null);
    }

    private static Layout fmtfor(AttribData[] allbufs) {
	int n = 0;
	for(AttribData buf : allbufs) {
	    if(buf.attr != null)
		n++;
	}
	AttribData[] bufs = new AttribData[n];
	n = 0;
	for(AttribData buf : allbufs) {
	    if(buf.attr != null)
		bufs[n++] = buf;
	}
	/* XXX: This algorithm assumes the maximum and minimum
	 * alignment is four, which is currently true for all vertex
	 * buffers, but may have to be adjusted. */
	int[] offs = new int[bufs.length];
	int galign = 4, off = 0;
	for(int i = 0; i < bufs.length; i++) {
	    // Apparently, all attributes should be aligned to four bytes.
	    int align = 4;
	    off = ((off + align - 1) / align) * align;
	    offs[i] = off;
	    off += bufs[i].elfmt.size();
	}
	off = ((off + galign - 1) / galign) * galign;
	int elsize = off;
	Layout.Input[] inputs = new Layout.Input[bufs.length];
	for(int i = 0; i < bufs.length; i++)
	    inputs[i] = new Layout.Input(bufs[i].attr, bufs[i].elfmt, 0, offs[i], elsize);
	return(new Layout(inputs));
    }

    protected VertexArray fmtdata() {
	Layout fmt = fmtfor(bufs);
	this.dbuf = new VertexArray.Buffer(fmt.inputs[0].stride * num, DataBuffer.Usage.STATIC, this::fill).shared();
	return(new VertexArray(fmt, dbuf).shared());
    }

    public VertexArray data() {
	if(data == null) {
	    synchronized(this) {
		if(data == null)
		    data = fmtdata();
	    }
	}
	return(data);
    }

    private FillBuffer fill(VertexArray.Buffer vbuf, Environment env) {
	VertexArray.Layout fmt = data().fmt;
	FillBuffer dst = env.fillbuf(vbuf);
	ByteBuffer buf = dst.push();
	int inp = 0;
	for(AttribData attr : bufs) {
	    if(attr.attr == null)
		continue;
	    attr.data(buf, fmt.inputs[inp++].offset, fmt.inputs[0].stride);
	}
	return(dst);
    }

    public void update(Render g) {
	if(data != null) {
	    if(data.bufs.length != 1) throw(new AssertionError());
	    g.update(data().bufs[0], this::fill);
	}
    }

    public abstract static class AttribData {
	public final Attribute attr;
	public final VectorFormat elfmt;

	public AttribData(Attribute attr, VectorFormat elfmt) {
	    this.attr = attr;
	    this.elfmt = elfmt;
	}

	public abstract void data(ByteBuffer dst, int offset, int stride);
	public abstract int size();
    }

    public abstract static class FloatData extends AttribData {
	public final FloatBuffer data;

	public FloatData(Attribute attr, int n, FloatBuffer data) {
	    super(attr, new VectorFormat(n, NumberFormat.FLOAT32));
	    if(data.capacity() % n != 0)
		throw(new RuntimeException(String.format("float-array length %d does not match element count %d", data.capacity(), n)));
	    this.data = data;
	}

	public int size() {return(data.capacity() / elfmt.nc);}

	public void data(ByteBuffer bdst, int offset, int stride) {
	    if((offset % 4) != 0)
		throw(new AssertionError());
	    FloatBuffer dst = bdst.asFloatBuffer();
	    if(stride == elfmt.size()) {
		dst.position(offset / 4);
		dst.put(data);
	    } else if((stride % 4) == 0) {
		for(int i = 0, o = offset / 4, fs = stride / 4; i < data.capacity(); i += elfmt.nc, o += fs) {
		    for(int e = 0; e < elfmt.nc; e++)
			dst.put(o + e, data.get(i + e));
		}
	    } else {
		throw(new AssertionError());
	    }
	}
    }

    public abstract static class IntData extends AttribData {
	public final IntBuffer data;

	public IntData(Attribute attr, int n, IntBuffer data) {
	    super(attr, new VectorFormat(n, NumberFormat.SINT32));
	    if(data.capacity() % n != 0)
		throw(new RuntimeException(String.format("int-array length %d does not match element count %d", data.capacity(), n)));
	    this.data = data;
	}

	public int size() {return(data.capacity() / elfmt.nc);}

	public void data(ByteBuffer bdst, int offset, int stride) {
	    if((offset % 4) != 0)
		throw(new AssertionError());
	    IntBuffer dst = bdst.asIntBuffer();
	    if(stride == elfmt.size()) {
		dst.position(offset / 4);
		dst.put(data);
	    } else if((stride % 4) == 0) {
		for(int i = 0, o = offset / 4, fs = stride / 4; i < data.capacity(); i += elfmt.nc, o += fs) {
		    for(int e = 0; e < elfmt.nc; e++)
			dst.put(o + e, data.get(i + e));
		}
	    } else {
		throw(new AssertionError());
	    }
	}
    }

    @ResName("pos2")
    public static class VertexData extends FloatData {
	public VertexData(FloatBuffer data) {
	    super(Homo3D.vertex, 3, data);
	}

	public VertexData(Resource res, Message buf, int nv) {
	    this(loadbuf2(Utils.wfbuf(nv * 3), buf));
	}
    }
    @ResName("pos")
    public static class VertexDecode implements DataCons {
	public void cons(Collection<AttribData> dst, Resource res, Message buf, int nv) {
	    dst.add(new VertexData(loadbuf(Utils.wfbuf(nv * 3), buf)));
	}
    }
    
    @ResName("nrm2")
    public static class NormalData extends FloatData {
	public NormalData(FloatBuffer data) {
	    super(Homo3D.normal, 3, data);
	}

	public NormalData(Resource res, Message buf, int nv) {
	    this(loadbuf2(Utils.wfbuf(nv * 3), buf));
	}
    }
    @ResName("nrm")
    public static class NormalDecode implements DataCons {
	public void cons(Collection<AttribData> dst, Resource res, Message buf, int nv) {
	    dst.add(new NormalData(loadbuf(Utils.wfbuf(nv * 3), buf)));
	}
    }

    @ResName("col2")
    public static class ColorData extends FloatData {
	public ColorData(FloatBuffer data) {
	    super(VertexColor.color, 4, data);
	}

	public ColorData(Resource res, Message buf, int nv) {
	    this(loadbuf2(Utils.wfbuf(nv * 4), buf));
	}
    }
    @ResName("col")
    public static class ColorDecode implements DataCons {
	public void cons(Collection<AttribData> dst, Resource res, Message buf, int nv) {
	    dst.add(new ColorData(loadbuf(Utils.wfbuf(nv * 4), buf)));
	}
    }

    @ResName("tex2")
    public static class TexelData extends FloatData {
	public TexelData(FloatBuffer data) {
	    super(Tex2D.texc, 2, data);
	}

	public TexelData(Resource res, Message buf, int nv) {
	    this(loadbuf2(Utils.wfbuf(nv * 2), buf));
	}
    }
    @ResName("tex")
    public static class TexelDecode implements DataCons {
	public void cons(Collection<AttribData> dst, Resource res, Message buf, int nv) {
	    dst.add(new TexelData(loadbuf(Utils.wfbuf(nv * 2), buf)));
	}
    }

    public void dispose() {
	synchronized(this) {
	    if(data != null) {
		data.dispose();
		dbuf.dispose();
		data = null;
		dbuf = null;
	    }
	}
    }

    @dolda.jglob.Discoverable
    @Target(ElementType.TYPE)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface ResName {
	public String value();
    }

    public interface DataCons {
	public void cons(Collection<AttribData> dst, Resource res, Message buf, int nvert);
    }

    public static FloatBuffer loadbuf(FloatBuffer dst, Message buf) {
	for(int i = 0; i < dst.capacity(); i++)
	    dst.put(i, buf.float32());
	return(dst);
    }

    public static FloatBuffer loadbuf2(FloatBuffer dst, Message buf) {
	int ver = buf.uint8();
	if(ver != 1)
	    throw(new RuntimeException("Unknown vertex-data version: " + ver));
	String fmt = buf.string();
	switch(fmt) {
	case "f4": {
	    for(int i = 0; i < dst.capacity(); i++)
		dst.put(i, buf.float32());
	    break;
	}
	case "f2": {
	    for(int i = 0; i < dst.capacity(); i++)
		dst.put(i, Utils.hfdec((short)buf.int16()));
	    break;
	}
	case "f1": {
	    for(int i = 0; i < dst.capacity(); i++)
		dst.put(i, Utils.mfdec((byte)buf.int8()));
	    break;
	}
	case "sn4": {
	    float F = buf.float32() / 2147483647.0f;
	    for(int i = 0; i < dst.capacity(); i++)
		dst.put(i, buf.int32() * F);
	    break;
	}
	case "sn2": {
	    float F = buf.float32() / 32767.0f;
	    for(int i = 0; i < dst.capacity(); i++)
		dst.put(i, buf.int16() * F);
	    break;
	}
	case "sn1": {
	    float F = buf.float32() / 127.0f;
	    for(int i = 0; i < dst.capacity(); i++)
		dst.put(i, buf.int8() * F);
	    break;
	}
	case "un4": {
	    float F = buf.float32() / 4294967295.0f;
	    for(int i = 0; i < dst.capacity(); i++)
		dst.put(i, buf.uint32() * F);
	    break;
	}
	case "un2": {
	    float F = buf.float32() / 65535.0f;
	    for(int i = 0; i < dst.capacity(); i++)
		dst.put(i, buf.uint16() * F);
	    break;
	}
	case "un1": {
	    float F = buf.float32() / 255.0f;
	    for(int i = 0; i < dst.capacity(); i++)
		dst.put(i, buf.uint8() * F);
	    break;
	}
	case "uvec1": {
	    int i = 0;
	    float F = 1.0f / 127.0f;
	    float[] vb = new float[3];
	    while(i < dst.capacity()) {
		Utils.oct2uvec(vb, buf.int8() * F, buf.int8() * F);
		dst.put(i++, vb[0]);
		dst.put(i++, vb[1]);
		dst.put(i++, vb[2]);
	    }
	    break;
	}
	case "uvec2": {
	    int i = 0;
	    float F = 1.0f / 32767.0f;
	    float[] vb = new float[3];
	    while(i < dst.capacity()) {
		Utils.oct2uvec(vb, buf.int16() * F, buf.int16() * F);
		dst.put(i++, vb[0]);
		dst.put(i++, vb[1]);
		dst.put(i++, vb[2]);
	    }
	    break;
	}
	default:
	    throw(new RuntimeException("Unknown vertex-data format: " + fmt));
	}
	return(dst);
    }

    private static final Map<String, DataCons> rnames = new TreeMap<String, DataCons>();

    static {
	for(Class<?> cl : dolda.jglob.Loader.get(ResName.class).classes()) {
	    String nm = cl.getAnnotation(ResName.class).value();
	    if(DataCons.class.isAssignableFrom(cl)) {
		rnames.put(nm, Utils.construct(cl.asSubclass(DataCons.class)));
	    } else if(AttribData.class.isAssignableFrom(cl)) {
		final java.lang.reflect.Constructor<? extends AttribData> cons;
		try {
		    cons = cl.asSubclass(AttribData.class).getConstructor(Resource.class, Message.class, Integer.TYPE);
		} catch(NoSuchMethodException e) {
		    throw(new Error("No proper constructor for res-consable vertex-array class " + cl, e));
		}
		rnames.put(nm, new DataCons() {
			public void cons(Collection<AttribData> dst, Resource res, Message buf, int num) {
			    dst.add(Utils.construct(cons, res, buf, num));
			}
		    });
	    } else {
		throw(new Error("Illegal vertex-array constructor class: " + cl));
	    }
	}
    }

    @Resource.LayerName("vbuf2")
    public static class VertexRes extends Resource.Layer implements Resource.IDLayer<Integer>{
	public transient final VertexBuf b;
	public final int id;
	
	private VertexRes(Resource res, VertexBuf b) {
	    res.super();
	    this.b = b;
	    this.id = 0;
	}

	public VertexRes(Resource res, Message buf) {
	    res.super();
	    List<AttribData> bufs = new LinkedList<AttribData>();
	    int fl = buf.uint8();
	    int ver = (fl & 0xf);
	    if(ver >= 2)
		throw(new Resource.LoadException(String.format("Unknown vbuf version: %d", ver), res));
	    if((fl & ~0xf) != 0)
		throw(new Resource.LoadException(String.format("Unknown vbuf flags: %02x", fl), res));
	    if(ver >= 1)
		this.id = buf.int16();
	    else
		this.id = 0;
	    int num = buf.uint16();
	    while(!buf.eom()) {
		String nm = buf.string();
		DataCons cons = rnames.get(nm);
		if(cons == null)
		    throw(new Resource.LoadException("Unknown vertex-array name: " + nm, res));
		if(ver >= 1) {
		    Message sub = new LimitMessage(buf, buf.int32());
		    cons.cons(bufs, res, sub, num);
		    sub.skip();
		} else {
		    cons.cons(bufs, res, buf, num);
		}
	    }
	    this.b = new VertexBuf(bufs.toArray(new AttribData[0])) {
		    public String toString() {
			return(String.format("#<vertexbuf %s>", res.name));
		    }
		};
	}
	
	public void init() {}

	public Integer layerid() {return(id);}
    }
}
