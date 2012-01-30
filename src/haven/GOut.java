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

import java.awt.Color;
import java.awt.image.BufferedImage;
import javax.media.opengl.*;
import java.nio.*;

public class GOut {
    public final GL gl;
    public final GLConfig gc;
    public Coord ul, sz, tx;
    private States.ColState color = new States.ColState(Color.WHITE);
    public final GLContext ctx;
    private final GOut root;
    public final GLState.Applier st;
    private final GLState.Buffer def2d;
	
    protected GOut(GOut o) {
	this.gl = o.gl;
	this.gc = o.gc;
	this.ul = o.ul;
	this.sz = o.sz;
	this.tx = o.tx;
	this.color = o.color;
	this.ctx = o.ctx;
	this.root = o.root;
	this.st = o.st;
	this.def2d = o.def2d;
	st.set(def2d);
    }

    public GOut(GL gl, GLContext ctx, GLConfig cfg, GLState.Applier st, GLState.Buffer def2d, Coord sz) {
	this.gl = gl;
	this.gc = cfg;
	this.ul = this.tx = Coord.z;
	this.sz = sz;
	this.ctx = ctx;
	this.st = st;
	this.root = this;
	this.def2d = def2d;
    }
    
    public static class GLException extends RuntimeException {
	public int code;
	public String str;
	private static javax.media.opengl.glu.GLU glu = new javax.media.opengl.glu.GLU();
	
	public GLException(int code) {
	    super("GL Error: " + code + " (" + glu.gluErrorString(code) + ")");
	    this.code = code;
	    this.str = glu.gluErrorString(code);
	}
    }

    public static void checkerr(GL gl) {
	int err = gl.glGetError();
	if(err != 0)
	    throw(new GLException(err));
    }

    private void checkerr() {
	checkerr(gl);
    }
	
    public GOut root() {
	return(root);
    }
    
    public GLState.Buffer basicstate() {
	return(def2d.copy());
    }

    public void image(BufferedImage img, Coord c) {
	if(img == null)
	    return;
	Tex tex = new TexI(img);
	image(tex, c);
	tex.dispose();
    }
	
    public void image(Resource.Image img, Coord c) {
	if(img == null)
	    return;
	image(img.tex(), c.add(img.o));
    }

    public void image(Tex tex, Coord c) {
	if(tex == null)
	    return;
	st.set(def2d);
	state(color);
	tex.crender(this, c.add(tx), ul, sz);
	checkerr();
    }
	
    public void aimage(Tex tex, Coord c, double ax, double ay) {
	Coord sz = tex.sz();
	image(tex, c.add((int)((double)sz.x * -ax), (int)((double)sz.y * -ay)));
    }
	
    public void image(Tex tex, Coord c, Coord sz) {
	if(tex == null)
	    return;
	st.set(def2d);
	state(color);
	tex.crender(this, c.add(tx), ul, this.sz, sz);
	checkerr();
    }
	
    public void image(Tex tex, Coord c, Coord ul, Coord sz) {
	if(tex == null)
	    return;
	st.set(def2d);
	state(color);
	ul = ul.add(this.tx);
	Coord br = ul.add(sz);
	if(ul.x < this.ul.x)
	    ul.x = this.ul.x;
	if(ul.y < this.ul.y)
	    ul.y = this.ul.y;
	if(br.x > this.ul.x + this.sz.x)
	    br.x = this.ul.x + this.sz.x;
	if(br.y > this.ul.y + this.sz.y)
	    br.y = this.ul.y + this.sz.y;
	tex.crender(this, c.add(this.tx), ul, br.sub(ul));
	checkerr();
    }
	
    private void vertex(Coord c) {
	gl.glVertex2i(c.x + tx.x, c.y + tx.y);
    }
	
    public void apply() {
	st.apply(this);
    }
    
    public void state(GLState st) {
	this.st.prep(st);
    }
    
    public void line(Coord c1, Coord c2, double w) {
	st.set(def2d);
	state(color);
	apply();
	gl.glLineWidth((float)w);
	gl.glBegin(GL.GL_LINES);
	vertex(c1);
	vertex(c2);
	gl.glEnd();
	checkerr();
    }
    
    public void text(String text, Coord c) {
	atext(text, c, 0, 0);
    }
	
    public void atext(String text, Coord c, double ax, double ay) {
	Text t = Text.render(text);
	Tex T = t.tex();
	Coord sz = t.sz();
	image(T, c.add((int)((double)sz.x * -ax), (int)((double)sz.y * -ay)));
	T.dispose();
	checkerr();
    }
    
    public void poly(Coord... c) {
	st.set(def2d);
	state(color);
	apply();
	gl.glBegin(GL.GL_POLYGON);
	for(Coord vc : c)
	    vertex(vc);
	gl.glEnd();
	checkerr();
    }
    
    public void poly2(Object... c) {
	st.set(def2d);
	st.put(States.color, null);
	apply();
	gl.glBegin(GL.GL_POLYGON);
	for(int i = 0; i < c.length; i += 2) {
	    Coord vc = (Coord)c[i];
	    Color col = (Color)c[i + 1];
	    gl.glColor4f((col.getRed() / 255.0f), (col.getGreen() / 255.0f), (col.getBlue() / 255.0f), (col.getAlpha() / 255.0f));
	    vertex(vc);
	}
	gl.glEnd();
	checkerr();
    }

    public void frect(Coord ul, Coord sz) {
	st.set(def2d);
	state(color);
	apply();
	gl.glBegin(GL.GL_QUADS);
	vertex(ul);
	vertex(ul.add(new Coord(sz.x, 0)));
	vertex(ul.add(sz));
	vertex(ul.add(new Coord(0, sz.y)));
	gl.glEnd();
	checkerr();
    }
	
    public void frect(Coord c1, Coord c2, Coord c3, Coord c4) {
	st.set(def2d);
	state(color);
	apply();
	gl.glBegin(GL.GL_QUADS);
	vertex(c1);
	vertex(c2);
	vertex(c3);
	vertex(c4);
	gl.glEnd();
	checkerr();
    }
	
    public void fellipse(Coord c, Coord r, int a1, int a2) {
	st.set(def2d);
	state(color);
	apply();
	gl.glBegin(GL.GL_TRIANGLE_FAN);
	vertex(c);
	for(int i = a1; i <= a2; i += 5) {
	    double a = (i * Math.PI * 2) / 360.0;
	    vertex(c.add((int)(Math.cos(a) * r.x), -(int)(Math.sin(a) * r.y)));
	}
	gl.glEnd();
	checkerr();
    }
	
    public void fellipse(Coord c, Coord r) {
	fellipse(c, r, 0, 360);
    }
	
    public void rect(Coord ul, Coord sz) {
	Coord ur, bl, br;
	ur = new Coord(ul.x + sz.x - 1, ul.y);
	bl = new Coord(ul.x, ul.y + sz.y - 1);
	br = new Coord(ur.x, bl.y);
	line(ul, ur, 1);
	line(ur, br, 1);
	line(br, bl, 1);
	line(bl, ul, 1);
    }
	
    public void chcolor(Color c) {
	if(c.equals(this.color.c))
	    return;
	this.color = new States.ColState(c);
    }
    
    public void chcolor(int r, int g, int b, int a) {
	chcolor(Utils.clipcol(r, g, b, a));
    }
	
    public void chcolor() {
	chcolor(Color.WHITE);
    }
    
    Color getcolor() {
	return(color.c);
    }
	
    public GOut reclip(Coord ul, Coord sz) {
	GOut g = new GOut(this);
	g.tx = this.tx.add(ul);
	g.ul = new Coord(g.tx);
	Coord gbr = g.ul.add(sz), tbr = this.ul.add(this.sz);
	if(g.ul.x < this.ul.x)
	    g.ul.x = this.ul.x;
	if(g.ul.y < this.ul.y)
	    g.ul.y = this.ul.y;
	if(gbr.x > tbr.x)
	    gbr.x = tbr.x;
	if(gbr.y > tbr.y)
	    gbr.y = tbr.y;
	g.sz = gbr.sub(g.ul);
	return(g);
    }
    
    public GOut reclipl(Coord ul, Coord sz) {
	GOut g = new GOut(this);
	g.tx = this.tx.add(ul);
	g.ul = new Coord(g.tx);
	g.sz = sz;
	return(g);
    }
    
    public Color getpixel(Coord c) {
	IntBuffer tgt = ByteBuffer.allocateDirect(4).order(ByteOrder.nativeOrder()).asIntBuffer();
	tgt.rewind();
	gl.glReadPixels(c.x + tx.x, root.sz.y - c.y - tx.y, 1, 1, GL.GL_RGBA, GL.GL_UNSIGNED_INT_8_8_8_8, tgt);
	checkerr();
	long rgb = ((long)tgt.get(0)) & 0xffffffffl;
	int r = (int)((rgb & 0xff000000l) >> 24);
	int g = (int)((rgb & 0x00ff0000l) >> 16);
	int b = (int)((rgb & 0x0000ff00l) >> 8);
	return(new Color(r, g, b));
    }
}
