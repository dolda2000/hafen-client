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
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.image.BufferedImage;

public class FastText {
    public static final VertexArray.Layout vf = new VertexArray.Layout(new VertexArray.Layout.Input(Ortho2D.pos, new VectorFormat(2, NumberFormat.SINT16), 0, 0, 8),
								       new VertexArray.Layout.Input(ColorTex.texc, new VectorFormat(2, NumberFormat.UNORM16), 0, 4, 8));
    public static final Font font = UI.scale(Text.sans, 10);
    public static final int h;
    public static final FontMetrics meter;
    private static final TexI ct;
    private static final int[] sx = new int[256], cw = new int[256];

    private FastText() {}

    static {
	BufferedImage junk = TexI.mkbuf(new Coord(1, 1));
	Graphics tmpl = junk.getGraphics();
	tmpl.setFont(font);
	meter = tmpl.getFontMetrics();
	h = meter.getHeight();
	tmpl.dispose();
	int cx = 0;
	for(char i = 32; i < 256; i++) {
	    cw[i] = meter.stringWidth(Character.toString(i));
	    sx[i] = cx;
	    cx += cw[i] + 2;
	}
	BufferedImage buf = TexI.mkbuf(new Coord(cx, meter.getAscent() + meter.getDescent()));
	Graphics g = buf.getGraphics();
	g.setFont(font);
	for(char i = 32; i < 256; i++)
	    g.drawString(Character.toString(i), sx[i], meter.getAscent());
	g.dispose();
	ct = new TexI(buf);
    }

    public static int textw(String text) {
	int r = 0;
	for(int i = 0; i < text.length(); i++)
	    r += cw[text.charAt(i)];
	return(r);
    }
    
    public static void aprint(GOut g, Coord c, double ax, double ay, String text) {
	Coord lc = c.add(g.tx);
	if(ax > 0)
	    lc.x -= Math.round(textw(text) * ax);
	int h = meter.getAscent() + meter.getDescent();
	if(ay > 0)
	    lc.y -= Math.round(h * ay);
	short[] data = new short[text.length() * 4 * 4];
	short[] idx = new short[text.length() * 6];
	for(int i = 0; i < text.length(); i++) {
	    char cc = text.charAt(i);
	    int vo = i * 4, so = vo * 4, io = i * 6;
	    int w = cw[cc];
	    short x1 = (short)lc.x, x2 = (short)(lc.x + w), y1 = (short)lc.y, y2 = (short)(lc.y + h);
	    short tx1 = (short)(((sx[cc] * 65535) + (ct.tdim.x / 2)) / ct.tdim.x), tx2 = (short)((((sx[cc] + w) * 65535) + (ct.tdim.x / 2)) / ct.tdim.x);
	    short ty1 = 0, ty2 = (short)((h * 65535) / ct.tdim.y);
	    data[so +  0] = x1; data[so +  1] = y1; data[so +  2] = tx1; data[so +  3] = ty1;
	    data[so +  4] = x1; data[so +  5] = y2; data[so +  6] = tx1; data[so +  7] = ty2;
	    data[so +  8] = x2; data[so +  9] = y1; data[so + 10] = tx2; data[so + 11] = ty1;
	    data[so + 12] = x2; data[so + 13] = y2; data[so + 14] = tx2; data[so + 15] = ty2;
	    idx[io + 0] = (short)(vo + 0); idx[io + 1] = (short)(vo + 1); idx[io + 2] = (short)(vo + 2);
	    idx[io + 3] = (short)(vo + 1); idx[io + 4] = (short)(vo + 3); idx[io + 5] = (short)(vo + 2);
	    lc.x += w;
	}
	g.out.draw1(g.state().copy().prep(ct.st()),
		    new Model(Model.Mode.TRIANGLES,
			      new VertexArray(vf, new VertexArray.Buffer(data.length * 2, DataBuffer.Usage.EPHEMERAL, DataBuffer.Filler.of(data))),
			      new Model.Indices(idx.length, NumberFormat.UINT16, DataBuffer.Usage.EPHEMERAL, DataBuffer.Filler.of(idx))));
    }
    
    public static void print(GOut g, Coord c, String text) {
	aprint(g, c, 0.0, 0.0, text);
    }
    
    public static void aprintf(GOut g, Coord c, double ax, double ay, String fmt, Object... args) {
	aprint(g, c, ax, ay, String.format(fmt, args));
    }
    
    public static void printf(GOut g, Coord c, String fmt, Object... args) {
	print(g, c, String.format(fmt, args));
    }
}
