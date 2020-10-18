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

public class HomoCoord4f {
    public float x, y, z, w;

    public HomoCoord4f(float x, float y, float z, float w) {
	this.x = x;
	this.y = y;
	this.z = z;
	this.w = w;
    }

    public HomoCoord4f(float x, float y, float z) {
	this(x, y, z, 1.0f);
    }

    public HomoCoord4f(Coord3f c) {
	this(c.x, c.y, c.z);
    }

    public boolean equals(HomoCoord4f o) {
	return((o.x == x) && (o.y == y) && (o.z == z));
    }

    public boolean equals(Object o) {
	return((o instanceof HomoCoord4f) && equals((HomoCoord4f)o));
    }

    public static HomoCoord4f fromclip(Matrix4f proj, Coord3f cc) {
	Matrix4f ip = proj.invert();
	float w = (1.0f - (cc.x * ip.m[3]) - (cc.y * ip.m[7]) - (cc.z * ip.m[11])) / ip.m[15];
	return(ip.mul4(new HomoCoord4f(cc.x, cc.y, cc.z, w)));
    }

    public static HomoCoord4f fromndc(Matrix4f proj, Coord3f nc) {
	Matrix4f ip = proj.invert();
	float w = 1.0f / ((nc.x * ip.m[3]) + (nc.y * ip.m[7]) + (nc.z * ip.m[11]) + ip.m[15]);
	return(ip.mul4(new HomoCoord4f(nc.x * w, nc.y * w, nc.z * w, w)));
    }

    public static HomoCoord4f lineclip(HomoCoord4f a, HomoCoord4f b) {
	float x0 = a.x, y0 = a.y, z0 = a.z, w0 = a.w;
	float dx = b.x - a.x, dy = b.y - a.y, dz = b.z - a.z, dw = b.w - a.w;
	float nxt = (-w0 - x0) / (dx + dw);
	float pxt = ( w0 - x0) / (dx - dw);
	float nyt = (-w0 - y0) / (dy + dw);
	float pyt = ( w0 - y0) / (dy - dw);
	float nzt = (-w0 - z0) / (dz + dw);
	float pzt = ( w0 - z0) / (dz - dw);
	float t = Float.POSITIVE_INFINITY;
	if(nxt >= 0) t = Math.min(t, nxt);
	if(pxt >= 0) t = Math.min(t, pxt);
	if(nyt >= 0) t = Math.min(t, nyt);
	if(pyt >= 0) t = Math.min(t, pyt);
	if(nzt >= 0) t = Math.min(t, nzt);
	if(pzt >= 0) t = Math.min(t, pzt);
	return(new HomoCoord4f(x0 + (t * dx), y0 + (t * dy),
			       z0 + (t * dz), w0 + (t * dw)));
    }

    public boolean clipped() {
	return((w <= 0) ||
	       (x < -w) || (x > w) ||
	       (y < -w) || (y > w) ||
	       (z < -w) || (z > w));
    }

    public Coord3f pdiv() {
	float f = 1.0f / w;
	return(new Coord3f(x * f, y * f, z * f));
    }

    public Coord3f toview(Area view) {
	Coord3f ndc = pdiv();
	return(new Coord3f(view.ul.x + ((( ndc.x + 1) * 0.5f) * (view.br.x - view.ul.x)),
			   view.ul.y + (((-ndc.y + 1) * 0.5f) * (view.br.y - view.ul.y)),
			   (ndc.z + 1) * 0.5f));
    }

    public String toString() {
	return(String.format("(%f, %f, %f, %f)", x, y, z, w));
    }
}
