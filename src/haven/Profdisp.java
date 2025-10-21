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
import java.util.*;
import java.nio.*;
import java.awt.Color;
import haven.Profile.*;

public class Profdisp extends Widget {
    public static final Resource.Image markimg = Resource.loadrimg("gfx/hud/arwmark");
    public static final Color[] cols = Utils.vgapal;
    private static final int h = UI.scale(80);
    public final Profile prof;
    public double mt = 0.05;
    private double dscale = 0;
    private Tex sscl = null;
    private int[] mark = new int[0];

    public Profdisp(Profile prof) {
	super(new Coord(prof.hist.length + UI.scale(50), h + markimg.ssz.y + UI.scale(10)));
	this.prof = prof;
	setcanfocus(true);
    }

    public class Buffer {
	public final Tex tex;
	private final Texture2D btex;

	public Buffer() {
	    btex = new Texture2D(prof.hist.length, h, DataBuffer.Usage.STREAM, new VectorFormat(4, NumberFormat.UNORM8), null);
	    tex = new TexRaw(new Texture2D.Sampler2D(btex));
	}

	private void draw(ByteBuffer buf, double scale) {
	    buf.order(ByteOrder.LITTLE_ENDIAN);
	    int w = prof.hist.length;
	    IntBuffer data = buf.asIntBuffer();
	    for(int i = 0; i < prof.hist.length; i++) {
		Part f = prof.hist[i];
		if(f == null)
		    continue;
		int y = h - 1;
		int O = (y * w) + i;
		double a = 0;
		List<Part> p = f.sub();
		for(int o = 0; o <= p.size(); o++) {
		    if(o < p.size())
			a += p.get(o).d();
		    else
			a = f.d();
		    Color col = cols[(o + 1) % cols.length];
		    int r = col.getRed(), g = col.getGreen(), b = col.getBlue();
		    int rgb = (r << 0) | (g << 8) | (b << 16) | (255 << 24);
		    for(int th = h - 1 - (int)(a / scale); (y >= 0) && (y >= th); y--, O -= w)
			data.put(O, rgb);
		}
	    }
	}

	public void update(Render r, double scale) {
	    r.update(btex.image(0), (img, env) -> {
		    FillBuffer buf = env.fillbuf(img);
		    draw(buf.push(), scale);
		    return(buf);
		});
	}
    }

    private static final String[] units = {"s", "ms", "\u00b5s", "ns"};
    private Buffer display;
    public void draw(GOut g) {
	if((sscl == null) || (dscale < mt * 0.70) || (dscale > mt)) {
	    int p = (int)Math.floor(Math.log10(mt));
	    double b = Math.pow(10.0, p) * 0.5;
	    dscale = Math.floor(mt / b) * b;
	    int u = Utils.clip(-Utils.floordiv(p, 3), 0, units.length - 1);
	    if(sscl != null)
		sscl.dispose();
	    sscl = Text.render(String.format("%.1f %s", dscale * Math.pow(10.0, u * 3), units[u])).tex();
	}
	g.image(display.tex, Coord.z);
	int sy = (int)Math.round((1 - (dscale / mt)) * h);
	g.chcolor(192, 192, 192, 128);
	g.line(new Coord(0, sy), new Coord(prof.hist.length, sy), 1);
	g.chcolor();
	g.image(sscl, new Coord(prof.hist.length + UI.scale(2), sy - (sscl.sz().y / 2)));
	for(int i = mark.length - 1; i >= 0; i--)
	    g.image(markimg, Coord.of(mark[i] - (markimg.ssz.x / 2), h + UI.scale(i)));
    }

    public void tick(double dt) {
	double[] ttl = new double[prof.hist.length];
	Integer[] order = new Integer[ttl.length];
	for(int i = 0; i < prof.hist.length; i++) {
	    order[i] = i;
	    if(prof.hist[i] != null)
		ttl[i] = prof.hist[i].d();
	}
	Arrays.sort(order, Comparator.comparing(i -> -ttl[i]));
	int ti = ttl.length - 1;
	for(int i = ttl.length - 1; i >= 0; i--) {
	    if(ttl[order[i]] != 0) {
		ti = i / 10;
		break;
	    }
	}
	if(ti >= 0)
	    mt = ttl[order[ti]];
	else
	    mt = 0.05;
	mt *= 1.1;
	int[] mark = new int[10];
	int n = 0;
	for(int i = 0; (i < 1) || ((i < mark.length) && (ttl[order[i]] >= mt)); i++)
	    mark[n++] = order[i];
	this.mark = Utils.splice(mark, 0, n);
    }

    public void gtick(Render g) {
	if(display == null)
	    display = new Buffer();
	display.update(g, mt / h);
    }

    public boolean keydown(KeyDownEvent ev) {
	if(ev.c == 'd') {
	    prof.dump(System.err);
	    return(true);
	}
	return(super.keydown(ev));
    }

    public boolean mousedown(MouseDownEvent ev) {
	for(int m : mark) {
	    if(Area.sized(Coord.of(m - (markimg.ssz.x / 2), h), markimg.ssz).contains(ev.c)) {
		prof.hist[m].dump(System.err);
		return(true);
	    }
	}
	return(super.mousedown(ev));
    }

    public String tooltip(Coord c, Widget prev) {
	c = xlate(c, false);
	if((c.x >= 0) && (c.x < prof.hist.length) && (c.y >= 0) && (c.y < h)) {
	    int x = c.x;
	    int y = c.y;
	    double t = (h - y) * (mt / h);
	    Part f = prof.hist[x];
	    if(f != null) {
		for(Part p : f.sub()) {
		    if((t -= p.d()) < 0)
			return(String.format("%s: %.2f ms, %s: %.2f ms", f.nm, f.d() * 1000, p.nm, p.d() * 1000));
		}
	    }
	}
	for(int m : mark) {
	    if(Area.sized(Coord.of(m - (markimg.ssz.x / 2), h), markimg.ssz).contains(c)) {
		Part f = prof.hist[m];
		return(String.format("%s: %.2f ms", f.nm, f.d() * 1000));
	    }
	}
	return("");
    }
}
