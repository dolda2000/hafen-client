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
import haven.Resource.Tileset;
import haven.Resource.Tile;
import java.util.zip.Inflater;

public class MCache {
    Tileset[] sets = null;
    Grid last = null;
    Map<Coord, Request> req = new TreeMap<Coord, Request>();
    Map<Coord, Grid> grids = new TreeMap<Coord, Grid>();
    Session sess;
    Set<Overlay> ols = new HashSet<Overlay>();
    public static final Coord tilesz = new Coord(11, 11);
    public static final Coord cmaps = new Coord(100, 100);
    Random gen;
    Map<Integer, Defrag> fragbufs = new TreeMap<Integer, Defrag>();
	
    private static class Request {
	private long lastreq = 0;
	private int reqs = 0;
    }

    public class Overlay {
	Coord c1, c2;
	int mask;
		
	public Overlay(Coord c1, Coord c2, int mask) {
	    this.c1 = c1;
	    this.c2 = c2;
	    this.mask = mask;
	    ols.add(this);
	}
		
	public void destroy() {
	    ols.remove(this);
	}
		
	public void update(Coord c1, Coord c2) {
	    this.c1 = c1;
	    this.c2 = c2;
	}
    }
	
    public class Grid {
	public int tiles[][];
	public Tile gcache[][];
	public Tile tcache[][][];
	public int ol[][];
	Collection<Gob> fo = new LinkedList<Gob>();
	boolean regged = false;
	Coord gc;
	OCache oc = sess.glob.oc;
	String mnm;
		
	public Grid(Coord gc) {
	    this.gc = gc;
	    tiles = new int[cmaps.x][cmaps.y];
	    ol = new int[cmaps.x][cmaps.y];
	    gcache = new Tile[cmaps.x][cmaps.y];
	    tcache = new Tile[cmaps.x][cmaps.y][];
	}
		
	public int gettile(Coord tc) {
	    return(tiles[tc.x][tc.y]);
	}
		
	public int getol(Coord tc) {
	    return(ol[tc.x][tc.y]);
	}
		
	public void remove() {
	    if(regged) {
		oc.lrem(fo);
		regged = false;
	    }
	}
		
	public void makeflavor() {
	    fo.clear();
	    Coord c = new Coord(0, 0);
	    Coord tc = gc.mul(cmaps);
	    for(c.y = 0; c.y < cmaps.x; c.y++) {
		for(c.x = 0; c.x < cmaps.y; c.x++) {
		    Tileset set = sets[tiles[c.x][c.y]];
		    if(set.flavobjs.size() > 0) {
			Random rnd = mkrandoom(c);
			if(rnd.nextInt(set.flavprob) == 0) {
			    Resource r = set.flavobjs.pick(rnd);
			    Gob g = new Gob(sess.glob, c.add(tc).mul(tilesz), -1, 0); 
			    g.setattr(new ResDrawable(g, r));
			    fo.add(g);
			}
		    }
		}
	    }
	    if(!regged) {
		oc.ladd(fo);
		regged = true;
	    }
	}
		
	public int randoom(Coord c, int r) {
	    return(MCache.this.randoom(c.add(gc.mul(cmaps)), r));
	}

	public Random mkrandoom(Coord c) {
	    return(MCache.this.mkrandoom(c.add(gc.mul(cmaps))));
	}
    }
	
    private Tileset loadset(String name, int ver) {
	Resource res = Resource.load(name, ver);
	res.loadwait();
	return(res.layer(Resource.tileset));
    }
	
    public MCache(Session sess) {
	this.sess = sess;
	sets = new Tileset[256];
	gen = new Random();
    }

    private static void initrandoom(Random r, Coord c) {
	r.setSeed(c.x);
	r.setSeed(r.nextInt() ^ c.y);
    }

    public int randoom(Coord c) {
	int ret;
		
	synchronized(gen) {
	    initrandoom(gen, c);
	    ret = Math.abs(gen.nextInt());
	    return(ret);
	}
    }
	
    public int randoom(Coord c, int r) {
	return(randoom(c) % r);
    }
	
    public static Random mkrandoom(Coord c) {
	Random ret = new Random();
	initrandoom(ret, c);
	return(ret);
    }

    private void replace(Grid g) {
	if(g == last)
	    last = null;
    }

    public void invalidate(Coord cc) {
	synchronized(req) {
	    if(req.get(cc) == null)
		req.put(cc, new Request());
	}
    }
    
    public void invalblob(Message msg) {
	int type = msg.uint8();
	if(type == 0) {
	    invalidate(msg.coord());
	} else if(type == 1) {
	    Coord ul = msg.coord();
	    Coord lr = msg.coord();
	    trim(ul, lr);
	} else if(type == 2) {
	    trimall();
	}
    }
	
    public Tile[] gettrans(Coord tc) {
	Grid g;
	synchronized(grids) {
	    Coord gc = tc.div(cmaps);
	    if((last != null) && last.gc.equals(gc))
		g = last;
	    else
		last = g = grids.get(gc);
	}
	if(g == null)
	    return(null);
	Coord gtc = tc.mod(cmaps);
	if(g.tcache[gtc.x][gtc.y] == null) {
	    int tr[][] = new int[3][3];
	    for(int y = -1; y <= 1; y++) {
		for(int x = -1; x <= 1; x++) {
		    if((x == 0) && (y == 0))
			continue;
		    int tn = gettilen(tc.add(new Coord(x, y)));
		    if(tn < 0)
			return(null);
		    tr[x + 1][y + 1] = tn;
		}
	    }
	    if(tr[0][0] >= tr[1][0]) tr[0][0] = -1;
	    if(tr[0][0] >= tr[0][1]) tr[0][0] = -1;
	    if(tr[2][0] >= tr[1][0]) tr[2][0] = -1;
	    if(tr[2][0] >= tr[2][1]) tr[2][0] = -1;
	    if(tr[0][2] >= tr[0][1]) tr[0][2] = -1;
	    if(tr[0][2] >= tr[1][2]) tr[0][2] = -1;
	    if(tr[2][2] >= tr[2][1]) tr[2][2] = -1;
	    if(tr[2][2] >= tr[1][2]) tr[2][2] = -1;
	    int bx[] = {0, 1, 2, 1};
	    int by[] = {1, 0, 1, 2};
	    int cx[] = {0, 2, 2, 0};
	    int cy[] = {0, 0, 2, 2};
	    ArrayList<Tile> buf = new ArrayList<Tile>();
	    for(int i = gettilen(tc) - 1; i >= 0; i--) {
		if((sets[i] == null) || (sets[i].btrans == null) || (sets[i].ctrans == null))
		    continue;
		int bm = 0, cm = 0;
		for(int o = 0; o < 4; o++) {
		    if(tr[bx[o]][by[o]] == i)
			bm |= 1 << o;
		    if(tr[cx[o]][cy[o]] == i)
			cm |= 1 << o;
		}
		if(bm != 0)
		    buf.add(sets[i].btrans[bm - 1].pick(randoom(tc)));
		if(cm != 0)
		    buf.add(sets[i].ctrans[cm - 1].pick(randoom(tc)));
	    }
	    g.tcache[gtc.x][gtc.y] = buf.toArray(new Tile[0]);
	}
	return(g.tcache[gtc.x][gtc.y]);
    }

    public Tile getground(Coord tc) {
	Grid g;
	synchronized(grids) {
	    Coord gc = tc.div(cmaps);
	    if((last != null) && last.gc.equals(gc))
		g = last;
	    else
		last = g = grids.get(gc);
	}
	if(g == null)
	    return(null);
	Coord gtc = tc.mod(cmaps);
	if(g.gcache[gtc.x][gtc.y] == null) {
	    Tileset ts = sets[g.gettile(gtc)];
	    g.gcache[gtc.x][gtc.y] = ts.ground.pick(randoom(tc));
	}
	return(g.gcache[gtc.x][gtc.y]);
    }

    public int gettilen(Coord tc) {
	Grid g;
	synchronized(grids) {
	    Coord gc = tc.div(cmaps);
	    if((last != null) && last.gc.equals(gc))
		g = last;
	    else
		last = g = grids.get(gc);
	}
	if(g == null)
	    return(-1);
	return(g.gettile(tc.mod(cmaps)));
    }
	
    public Tileset gettile(Coord tc) {
	int tn = gettilen(tc);
	if(tn == -1)
	    return(null);
	return(sets[tn]);
    }
	
    public int getol(Coord tc) {
	Grid g;
	synchronized(grids) {
	    Coord gc = tc.div(cmaps);
	    if((last != null) && last.gc.equals(gc))
		g = last;
	    else
		last = g = grids.get(gc);
	}
	if(g == null)
	    return(-1);
	int ol = g.getol(tc.mod(cmaps));
	for(Overlay lol : ols) {
	    if(tc.isect(lol.c1, lol.c2.add(lol.c1.inv()).add(new Coord(1, 1))))
		ol |= lol.mask;
	}
	return(ol);
    }
	
    public void mapdata2(Message msg) {
	Coord c = msg.coord();
	String mmname = msg.string().intern();
	if(mmname.equals(""))
	    mmname = null;
	int[] pfl = new int[256];
	while(true) {
	    int pidx = msg.uint8();
	    if(pidx == 255)
		break;
	    pfl[pidx] = msg.uint8();
	}
	Message blob = new Message(0);
	{
	    Inflater z = new Inflater();
	    z.setInput(msg.blob, msg.off, msg.blob.length - msg.off);
	    byte[] buf = new byte[10000];
	    while(true) {
		try {
		    int len;
		    if((len = z.inflate(buf)) == 0) {
			if(!z.finished())
			    throw(new RuntimeException("Got unterminated map blob"));
			break;
		    }
		    blob.addbytes(buf, 0, len);
		} catch(java.util.zip.DataFormatException e) {
		    throw(new RuntimeException("Got malformed map blob", e));
		}
	    }
	}
	synchronized(req) {
	    synchronized(grids) {
		if(req.containsKey(c)) {
		    int i = 0;
		    Grid g = new Grid(c);
		    g.mnm = mmname;
		    for(int y = 0; y < cmaps.y; y++) {
			for(int x = 0; x < cmaps.x; x++) {
			    g.tiles[x][y] = blob.uint8();
			}
		    }
		    for(int y = 0; y < cmaps.y; y++) {
			for(int x = 0; x < cmaps.x; x++)
			    g.ol[x][y] = 0;
		    }
		    while(true) {
			int pidx = blob.uint8();
			if(pidx == 255)
			    break;
			int fl = pfl[pidx];
			int type = blob.uint8();
			Coord c1 = new Coord(blob.uint8(), blob.uint8());
			Coord c2 = new Coord(blob.uint8(), blob.uint8());
			int ol;
			if(type == 0) {
			    if((fl & 1) == 1)
				ol = 2;
			    else
				ol = 1;
			} else if(type == 1) {
			    if((fl & 1) == 1)
				ol = 8;
			    else
				ol = 4;
			} else {
			    throw(new RuntimeException("Unknown plot type " + type));
			}
			for(int y = c1.y; y <= c2.y; y++) {
			    for(int x = c1.x; x <= c2.x; x++) {
				g.ol[x][y] |= ol;
			    }
			}
		    }
		    req.remove(c);
		    g.makeflavor();
		    if(grids.containsKey(c)) {
			grids.get(c).remove();
			replace(grids.remove(c));
		    }
		    grids.put(c, g);
		}
	    }
	}
    }
    
    public void mapdata(Message msg) {
	long now = System.currentTimeMillis();
	int pktid = msg.int32();
	int off = msg.uint16();
	int len = msg.uint16();
	Defrag fragbuf;
	synchronized(fragbufs) {
	    if((fragbuf = fragbufs.get(pktid)) == null) {
		fragbuf = new Defrag(len);
		fragbufs.put(pktid, fragbuf);
	    }
	    fragbuf.add(msg.blob, 8, msg.blob.length - 8, off);
	    fragbuf.last = now;
	    if(fragbuf.done()) {
		mapdata2(fragbuf.msg());
		fragbufs.remove(pktid);
	    }
	
	    /* Clean up old buffers */
	    for(Iterator<Map.Entry<Integer, Defrag>> i = fragbufs.entrySet().iterator(); i.hasNext();) {
		Map.Entry<Integer, Defrag> e = i.next();
		Defrag old = e.getValue();
		if(now - old.last > 10000)
		    i.remove();
	    }
	}
    }
    
    public void tilemap(Message msg) {
	while(!msg.eom()) {
	    int id = msg.uint8();
	    String resnm = msg.string();
	    int resver = msg.uint16();
	    sets[id] = loadset(resnm, resver);
	}
    }
	
    public void trimall() {
	synchronized(req) {
	    synchronized(grids) {
		for(Grid g : grids.values())
		    g.remove();
		grids.clear();
		req.clear();
	    }
	}
    }
	
    public void trim(Coord ul, Coord lr) {
	synchronized(grids) {
	    for(Iterator<Map.Entry<Coord, Grid>> i = grids.entrySet().iterator(); i.hasNext();) {
		Map.Entry<Coord, Grid> e = i.next();
		Coord gc = e.getKey();
		Grid g = e.getValue();
		if((gc.x < ul.x) || (gc.y < ul.y) || (gc.x > lr.x) || (gc.y > lr.y)) {
		    i.remove();
		    g.remove();
		}
	    }
	}
	synchronized(req) {
	    for(Iterator<Coord> i = req.keySet().iterator(); i.hasNext();) {
		Coord gc = i.next();
		if((gc.x < ul.x) || (gc.y < ul.y) || (gc.x > lr.x) || (gc.y > lr.y))
		    i.remove();
	    }
	}
    }
	
    public void request(Coord gc) {
	synchronized(req) {
	    if(!req.containsKey(gc))
		req.put(gc, new Request());
	}
    }
	
    public void sendreqs() {
	long now = System.currentTimeMillis();
	synchronized(req) {
	    for(Iterator<Map.Entry<Coord, Request>> i = req.entrySet().iterator(); i.hasNext();) {
		Map.Entry<Coord, Request> e = i.next();
		Coord c = e.getKey();
		Request r = e.getValue();
		if(now - r.lastreq > 1000) {
		    r.lastreq = now;
		    if(++r.reqs >= 5) {
			i.remove();
		    } else {
			Message msg = new Message(Session.MSG_MAPREQ);
			msg.addcoord(c);
			sess.sendmsg(msg);
		    }
		}
	    }
	}
    }
}
