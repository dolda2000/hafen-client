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

import javax.media.opengl.*;

public class Matrix4f {
    public final float[] m;
    
    public Matrix4f() {
	m = new float[16];
    }
    
    public Matrix4f(Matrix4f b) {
	this();
	System.arraycopy(b.m, 0, m, 0, 16);
    }
    
    public Matrix4f(float e00, float e01, float e02, float e03, float e10, float e11, float e12, float e13, float e20, float e21, float e22, float e23, float e30, float e31, float e32, float e33) {
	this();
	m[ 0] = e00; m[ 4] = e01; m[ 8] = e02; m[12] = e03;
	m[ 1] = e10; m[ 5] = e11; m[ 9] = e12; m[13] = e13;
	m[ 2] = e20; m[ 6] = e21; m[10] = e22; m[14] = e23;
	m[ 3] = e30; m[ 7] = e31; m[11] = e32; m[15] = e33;
    }
    
    public Matrix4f(float[] m) {
	this.m = m;
    }
    
    public static Matrix4f identity() {
	return(new Matrix4f(1, 0, 0, 0,
			    0, 1, 0, 0,
			    0, 0, 1, 0,
			    0, 0, 0, 1));
    }

    public float get(int x, int y) {
	return(m[y + (x * 4)]);
    }

    public void set(int x, int y, float v) {
	m[y + (x * 4)] = v;
    }

    public Matrix4f add(Matrix4f b) {
	Matrix4f n = new Matrix4f();
	for(int i = 0; i < 16; i++)
	    n.m[i] = m[i] + b.m[i];
	return(n);
    }
    
    public Matrix4f transpose() {
	Matrix4f n = new Matrix4f();
	for(int y = 0; y < 4; y++) {
	    for(int x = 0; x < 4; x++) {
		n.set(x, y, get(y, x));
	    }
	}
	return(n);
    }
    
    public Matrix4f trim3(float e33) {
	Matrix4f n = new Matrix4f(this);
	n.m[3] = n.m[7] = n.m[11] = n.m[12] = n.m[13] = n.m[14] = 0;
	n.m[15] = e33;
	return(n);
    }
    
    public void getgl(GL gl, int matrix) {
	gl.glGetFloatv(matrix, m, 0);
    }
    
    public void loadgl(GL gl) {
	gl.glLoadMatrixf(m, 0);
    }
    
    public static Matrix4f fromgl(GL gl, int matrix) {
	Matrix4f m = new Matrix4f();
	m.getgl(gl, matrix);
	return(m);
    }
    
    public String toString() {
	StringBuilder buf = new StringBuilder();
	buf.append('[');
	for(int y = 0; y < 4; y++) {
	    if(y > 0)
		buf.append(", ");
	    buf.append('[');
	    for(int x = 0; x < 4; x++) {
		if(x > 0)
		    buf.append(", ");
		buf.append(Float.toString(get(x, y)));
	    }
	    buf.append(']');
	}
	buf.append(']');
	return(buf.toString());
    }

    public String toString2() {
	StringBuilder buf = new StringBuilder();
	for(int y = 0; y < 4; y++) {
	    buf.append('[');
	    for(int x = 0; x < 4; x++) {
		if(x > 0)
		    buf.append(", ");
		buf.append(Float.toString(get(x, y)));
	    }
	    buf.append("]\n");
	}
	return(buf.toString());
    }
}
