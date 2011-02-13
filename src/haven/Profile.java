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

import java.awt.Graphics;
import java.awt.Color;
import java.util.*;

public class Profile {
    public Frame[] hist;
    private int i = 0;
    private static Color[] cols;
    
    static {
	cols = new Color[16];
	for(int i = 0; i < 16; i++) {
	    int r = ((i & 4) != 0)?1:0;
	    int g = ((i & 2) != 0)?1:0;
	    int b = ((i & 1) != 0)?1:0;
	    if((i & 8) != 0) {
		r *= 255;
		g *= 255;
		b *= 255;
	    } else {
		r *= 128;
		g *= 128;
		b *= 128;
	    }
	    cols[i] = new Color(r, g, b);
	}
    }
    
    public class Frame {
	public String nm[];
	public long total, prt[];
	private List<Long> pw = new LinkedList<Long>();
	private List<String> nw = new LinkedList<String>();
	private long then, last;
	
	public Frame() {
	    start();
	}
	
	public void start() {
	    last = then = System.nanoTime();
	}
	
	public void tick(String nm) {
	    long now = System.nanoTime();
	    pw.add(now - last);
	    nw.add(nm);
	    last = now;
	}
	
	public void add(String nm, long tm)
	{
	    pw.add(tm);
	    nw.add(nm);
	}

	public void tick(String nm, long subtm) {
	    long now = System.nanoTime();
	    pw.add(now - last - subtm);
	    nw.add(nm);
	    last = now;
	}
	
	public void fin() {
	    total = System.nanoTime() - then;
	    nm = new String[nw.size()];
	    prt = new long[pw.size()];
	    for(int i = 0; i < pw.size(); i++) {
		nm[i] = nw.get(i);
		prt[i] = pw.get(i);
	    }
	    hist[i] = this;
	    if(++i >= hist.length)
		i = 0;
	    pw = null;
	    nw = null;
	}
	
	public String toString() {
	    StringBuilder buf = new StringBuilder();
	    for(int i = 0; i < prt.length; i++) {
		if(i > 0)
		    buf.append(", ");
		buf.append(nm[i] + ": " + prt[i]);
	    }
	    buf.append(", total: " + total);
	    return(buf.toString());
	}
    }
    
    public Profile(int hl) {
	hist = new Frame[hl];
    }
    
    public Frame last() {
	if(i == 0)
	    return(hist[hist.length - 1]);
	return(hist[i - 1]);
    }
    
    public Tex draw(int h, long scale) {
	TexIM ret = new TexIM(new Coord(hist.length, h));
	Graphics g = ret.graphics();
	for(int i = 0; i < hist.length; i++) {
	    Frame f = hist[i];
	    if(f == null)
		continue;
	    long a = 0;
	    for(int o = 0; o < f.prt.length; o++) {
		long c = a + f.prt[o];
		g.setColor(cols[o]);
		g.drawLine(i, (int)(h - (a / scale)), i, (int)(h - (c / scale)));
		a = c;
	    }
	}
	ret.update();
	return(ret);
    }
}
