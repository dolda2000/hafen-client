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

public class Glob {
    public static final int GMSG_TIME = 0;
    public static final int GMSG_ASTRO = 1;
    public static final int GMSG_LIGHT = 2;
	
    public long time;
    public Astronomy ast;
    public OCache oc = new OCache(this);
    public MCache map;
    public Session sess;
    public Party party;
    public Set<Pagina> paginae = new HashSet<Pagina>();
    private Map<Resource, Pagina> pmap = new WeakHashMap<Resource, Pagina>();
    public Map<String, CAttr> cattr = new HashMap<String, CAttr>();
    public Map<Integer, Buff> buffs = new TreeMap<Integer, Buff>();
    public java.awt.Color amblight = null;
    
    public Glob(Session sess) {
	this.sess = sess;
	map = new MCache(sess);
	party = new Party(this);
    }
    
    public static class CAttr extends Observable {
	String nm;
	int base, comp;
	
	public CAttr(String nm, int base, int comp) {
	    this.nm = nm.intern();
	    this.base = base;
	    this.comp = comp;
	}
	
	public void update(int base, int comp) {
	    if((base == this.base) && (comp == this.comp))
		return;
	    this.base = base;
	    this.comp = comp;
	    setChanged();
	    notifyObservers(null);
	}
    }
    
    public static class Pagina implements java.io.Serializable {
	private final java.lang.ref.WeakReference<Resource> res;
	public State st;
	public int time;
	public Image img;
	
	public interface Image {
	    public Tex tex();
	}

	public static enum State {
	    ENABLED, DISABLED, TICKING
	    
	    public Image img(final Pagina pag) {
		return(new Image() {
			public Tex tex() {
			    if(pag.res() == null)
				return(null);
			    return(pag.res().layer(Resource.imgc).tex());
			}
		    });
	    }
	}
	
	public Pagina(Resource res) {
	    this.res = new java.lang.ref.WeakReference<Resource>(res);
	    state(State.ENABLED);
	}
	
	public Resource res() {
	    return(res.get());
	}
	
	public Resource.AButton act() {
	    if(res().loading)
		return(null);
	    return(res().layer(Resource.action));
	}
	
	public void state(State st) {
	    this.st = st;
	    this.img = st.img(this);
	}
    }
	
    private static double defix(int i) {
	return(((double)i) / 1e9);
    }
	
    public void blob(Message msg) {
	while(!msg.eom()) {
	    switch(msg.uint8()) {
	    case GMSG_TIME:
		time = msg.int32();
		break;
	    case GMSG_ASTRO:
		double dt = defix(msg.int32());
		double mp = defix(msg.int32());
		double yt = defix(msg.int32());
		boolean night = (dt < 0.25) || (dt > 0.75);
		ast = new Astronomy(dt, mp, yt, night);
		break;
	    case GMSG_LIGHT:
		amblight = msg.color();
		break;
	    }
	}
    }
	
    public Pagina paginafor(Resource res) {
	if(res == null)
	    return(null);
	synchronized(pmap) {
	    Pagina p = pmap.get(res);
	    if(p == null)
		pmap.put(res, p = new Pagina(res));
	    return(p);
	}
    }

    public void paginae(Message msg) {
	synchronized(paginae) {
	    while(!msg.eom()) {
		int act = msg.uint8();
		if(act == '+') {
		    String nm = msg.string();
		    int ver = msg.uint16();
		    Pagina pag = paginafor(Resource.load(nm, ver));
		    paginae.add(pag); 
		    int t;
		    while((t = msg.uint8()) != 0) {
			if(t == '!') {
			    pag.state(Pagina.State.DISABLED);
			} else if(t == '*') {
			    pag.time = msg.int32();
			    pag.st = Pagina.State.TICKING;
			}
		    }
		} else if(act == '-') {
		    String nm = msg.string();
		    int ver = msg.uint16();
		    paginae.remove(paginafor(Resource.load(nm, ver))); 
		}
	    }
	}
    }
    
    public void cattr(Message msg) {
	synchronized(cattr) {
	    while(!msg.eom()) {
		String nm = msg.string();
		int base = msg.int32();
		int comp = msg.int32();
		CAttr a = cattr.get(nm);
		if(a == null) {
		    a = new CAttr(nm, base, comp);
		    cattr.put(nm, a);
		} else {
		    a.update(base, comp);
		}
	    }
	}
    }
    
    public void buffmsg(Message msg) {
	String name = msg.string().intern();
	synchronized(buffs) {
	    if(name == "clear") {
		buffs.clear();
	    } else if(name == "set") {
		int id = msg.int32();
		Indir<Resource> res = sess.getres(msg.uint16());
		String tt = msg.string();
		int ameter = msg.int32();
		int nmeter = msg.int32();
		int cmeter = msg.int32();
		int cticks = msg.int32();
		boolean major = msg.uint8() != 0;
		Buff buff;
		if((buff = buffs.get(id)) == null) {
		    buff = new Buff(id, res);
		} else {
		    buff.res = res;
		}
		if(tt.equals(""))
		    buff.tt = null;
		else
		    buff.tt = tt;
		buff.ameter = ameter;
		buff.nmeter = nmeter;
		buff.ntext = null;
		buff.cmeter = cmeter;
		buff.cticks = cticks;
		buff.major = major;
		buff.gettime = System.currentTimeMillis();
		buffs.put(id, buff);
	    } else if(name == "rm") {
		int id = msg.int32();
		buffs.remove(id);
	    }
	}
    }
}
