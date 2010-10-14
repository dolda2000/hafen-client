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

public class Coord3f {
    public float x, y, z;
    public static Coord3f o = new Coord3f(0, 0, 0);
    
    public Coord3f(float x, float y, float z) {
	this.x = x;
	this.y = y;
	this.z = z;
    }
    
    public Coord3f add(float ax, float ay, float az) {
	return(new Coord3f(x + ax, y + ay, z + az));
    }

    public Coord3f add(Coord3f b) {
	return(add(b.x, b.y, b.z));
    }
    
    public Coord3f sadd(float e, float a, float r) {
	return(add((float)Math.cos(a) * (float)Math.cos(e) * r, (float)Math.sin(a) * (float)Math.cos(e) * r, (float)Math.sin(e) * r));
    }
    
    public Coord3f sub(float ax, float ay, float az) {
	return(new Coord3f(x - ax, y - ay, z - az));
    }

    public Coord3f sub(Coord3f b) {
	return(sub(b.x, b.y, b.z));
    }
    
    public Coord3f mul(float f) {
	return(new Coord3f(x * f, y * f, z * f));
    }
    
    public Coord3f inv() {
	return(new Coord3f(-x, -y, -z));
    }
    
    public float dist(Coord3f o) {
	float dx = o.x - x;
	float dy = o.y - y;
	float dz = o.z - z;
	return((float)Math.sqrt((dx * dx) + (dy * dy) + (dz * dz)));
    }
    
    public String toString() {
	return(String.format("(%f, %f, %f)", x, y, z));
    }
}
