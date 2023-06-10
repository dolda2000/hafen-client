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

public class DRandom {
    private final Random rnd = new Random(0);
    private final long seed;

    public DRandom() {
	this.seed = new Random().nextLong();
    }

    public DRandom(long seed) {
	this.seed = seed;
    }

    public DRandom(Random from) {
	this(from.nextLong());
    }

    private Random seed(long a) {
	rnd.setSeed(seed ^ a);
	return(rnd);
    }

    private Random seed(long a, long b) {
	rnd.setSeed(seed ^ a);
	rnd.setSeed(rnd.nextLong() ^ b);
	return(rnd);
    }

    private Random seed(long... a) {
	rnd.setSeed(seed ^ a[0]);
	for(int i = 1; i < a.length; i++)
	    rnd.setSeed(rnd.nextLong() ^ a[i]);
	return(rnd);
    }

    public int randi(long a) {return(seed(a).nextInt());}
    public int randi(long a, long b) {return(seed(a, b).nextInt());}
    public int randi(long... a) {return(seed(a).nextInt());}

    public long randl(long a) {return(seed(a).nextLong());}
    public long randl(long a, long b) {return(seed(a, b).nextLong());}
    public long randl(long... a) {return(seed(a).nextLong());}

    public float randf(long a) {return(seed(a).nextFloat());}
    public float randf(long a, long b) {return(seed(a, b).nextFloat());}
    public float randf(long... a) {return(seed(a).nextFloat());}

    public double randd(long a) {return(seed(a).nextDouble());}
    public double randd(long a, long b) {return(seed(a, b).nextDouble());}
    public double randd(long... a) {return(seed(a).nextDouble());}
}
