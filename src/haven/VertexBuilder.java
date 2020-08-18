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
import haven.render.*;
import haven.render.sl.Attribute;
import haven.render.VertexArray.Layout;
import haven.render.VertexArray.Layout.Input;

public class VertexBuilder {
    public final Layout fmt;
    public final int vsz;
    private final byte[] cur;
    private int vn = 0;
    private byte[] data;

    public VertexBuilder(Layout fmt) {
	if(fmt.nbufs != 1)
	    throw(new IllegalArgumentException());
	this.vsz = fmt.inputs[0].stride;
	for(Input el : fmt.inputs) {
	    if((el.stride != vsz) || (el.offset >= vsz))
		throw(new IllegalArgumentException());
	}
	this.fmt = fmt;
	this.cur = new byte[vsz];
	this.data = new byte[vsz * 16];
    }

    public VertexBuilder set(int a, int el, float v) {
	Input inp = fmt.inputs[a];
	if((el < 0) || (el >= inp.el.nc))
	    throw(new IllegalArgumentException(String.format("setting attrib %s element %d/%d", a, el, inp.el.nc)));
	int o = inp.offset + (inp.el.cf.size * el);
	switch(inp.el.cf) {
	case FLOAT32:
	    Utils.int32e(Float.floatToIntBits(v), cur, o);
	    break;
	case FLOAT16:
	    Utils.int16e(Utils.hfenc(v), cur, o);
	    break;

	case UNORM8:
	    cur[o] = (byte)Math.round(Math.max(Math.min(v, 1f), 0f) * 255f);
	    break;
	case UNORM16:
	    Utils.uint16e(Math.round(Math.max(Math.min(v, 1f), 0f) * 65535f), cur, o);
	    break;
	case UNORM32:
	    Utils.uint32e(Math.round(Math.max(Math.min(v, 1f), 0f) * 4294967295.0), cur, o);
	    break;
	case SNORM8:
	    cur[o] = (byte)Math.round(Math.max(Math.min(v, 1f), -1f) * 127f);
	    break;
	case SNORM16:
	    Utils.int16e((short)Math.round(Math.max(Math.min(v, 1f), -1f) * 32767f), cur, o);
	    break;
	case SNORM32:
	    Utils.int32e((int)Math.round(Math.max(Math.min(v, 1f), -1f) * 2147483647.0), cur, o);
	    break;

	case UINT8:
	    cur[o] = (byte)Math.round(Math.max(Math.min(v, 255f), 0f));
	    break;
	case UINT16:
	    Utils.uint16e(Math.round(Math.max(Math.min(v, 65535f), 0f)), cur, o);
	    break;
	case SINT8:
	    cur[o] = (byte)Math.round(Math.max(Math.min(v, 127f), -128f));
	    break;
	case SINT16:
	    Utils.uint16e(Math.round(Math.max(Math.min(v, 32767f), -32768f)), cur, o);
	    break;
	default:
	    throw(new RuntimeException("unsupported number format: " + fmt.inputs[a].el.cf));
	}
	return(this);
    }

    public VertexBuilder set(int a, float... v) {
	for(int i = 0; i < v.length; i++)
	    set(a, i, v[i]);
	return(this);
    }

    public VertexBuilder set(int a, Coord3f c) {
	int nc = fmt.inputs[a].el.nc;
	set(a, 0, c.x);
	if(nc > 1)
	    set(a, 1, c.y);
	if(nc > 2)
	    set(a, 2, c.z);
	return(this);
    }

    public VertexBuilder set(int a, FColor c) {
	int nc = fmt.inputs[a].el.nc;
	set(a, 0, c.r);
	if(nc > 1)
	    set(a, 1, c.g);
	if(nc > 2)
	    set(a, 2, c.b);
	if(nc > 3)
	    set(a, 2, c.a);
	return(this);
    }

    public VertexBuilder set(int a, java.awt.Color c) {
	int nc = fmt.inputs[a].el.nc;
	set(a, 0, c.getRed() / 255f);
	if(nc > 1)
	    set(a, 1, c.getGreen() / 255f);
	if(nc > 2)
	    set(a, 2, c.getBlue() / 255f);
	if(nc > 3)
	    set(a, 2, c.getAlpha() / 255f);
	return(this);
    }

    public int voff() {
	return(vn);
    }

    public int emit() {
	int off = vn * vsz;
	while(off + vsz > data.length)
	    data = Arrays.copyOf(data, data.length * 2);
	System.arraycopy(cur, 0, data, off, vsz);
	return(vn++);
    }

    public VertexBuilder emitm() {
	emit();
	return(this);
    }

    public byte[] fina() {
	return(Arrays.copyOf(data, vn * vsz));
    }

    public VertexArray finv() {
	byte[] data = fina();
	return(new VertexArray(fmt, new VertexArray.Buffer(data.length, DataBuffer.Usage.STATIC, DataBuffer.Filler.of(data))));
    }

    public static class LayoutBuilder {
	private final List<Attribute> attr = new ArrayList<>();
	private final List<VectorFormat> efmt = new ArrayList<>();
	public final int bufidx;

	public LayoutBuilder(int bufidx) {
	    this.bufidx = bufidx;
	}

	public LayoutBuilder() {
	    this(0);
	}

	public LayoutBuilder add(Attribute attr, VectorFormat efmt) {
	    this.attr.add(attr);
	    this.efmt.add(efmt);
	    return(this);
	}

	public Layout fin() {
	    int n = attr.size();
	    int[] offs = new int[n];
	    int off = 0;
	    for(int i = 0; i < n; i++) {
		/* Apparently, all attributes should be aligned to four bytes. */
		int align = 4;
		off = (off + align - 1) & ~(align - 1);
		offs[i] = off;
		off += efmt.get(i).size();
	    }
	    int galign = 4;
	    off = (off + galign - 1) & ~(galign - 1);
	    Input[] els = new Input[n];
	    for(int i = 0; i < n; i++)
		els[i] = new Input(attr.get(i), efmt.get(i), bufidx, offs[i], off);
	    return(new Layout(els));
	}

	public VertexBuilder buf() {
	    return(new VertexBuilder(fin()));
	}
    }

    public static LayoutBuilder fmt() {
	return(new LayoutBuilder());
    }
}
