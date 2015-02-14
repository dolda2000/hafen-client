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

public class Defrag {
    byte[] blob;
    int len;
    long last = 0;
    final int[] ms1 = new int[20], ms2 = new int[20];
    
    public Defrag(int len) {
	this.len = len;
	this.blob = new byte[len];
	ms1[0] = 0;
	ms2[0] = len;
	for(int i = 1; i < 20; i++)
	    ms1[i] = -1;
    }
    
    private void addm(int m1, int m2) {
	for(int i = 0; i < ms1.length; i++) {
	    if(ms1[i] == -1) {
		ms1[i] = m1;
		ms2[i] = m2;
		return;
	    }
	}
	throw(new RuntimeException("Ran out of segment buffers!"));
    }
    
    public void add(byte[] blob, int boff, int blen, int off) {
	System.arraycopy(blob, boff, this.blob, off, blen);
	for(int i = 0; i < ms1.length; i++) {
	    if(ms1[i] == -1)
		continue;
	    int m1 = ms1[i], m2 = ms2[i];
	    int s1 = off, s2 = off + blen;
	    if((m1 >= s1) && (m2 <= s2)) {
		ms1[i] = -1;
	    } else if((m1 >= s1) && (m1 < s2) && (m2 >= s2)) {
		ms1[i] = s2;
	    } else if((m1 < s1) && (m2 >= s1) && (m2 <= s2)) {
		ms2[i] = s1;
	    } else if((m1 < s1) && (m2 > s2)) {
		ms2[i] = s1;
		addm(s2, m2);
	    }
	}
    }
    
    public void add(byte[] blob, int off) {
	add(blob, 0, blob.length, off);
    }

    public boolean done() {
	for(int i = 0; i < ms1.length; i++) {
	    if(ms1[i] != -1)
		return(false);
	}
	return(true);
    }
    
    public Message msg() {
	return(new MessageBuf(blob));
    }
}
