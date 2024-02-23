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

import java.net.*;
import java.util.*;
import java.util.function.*;
import java.io.*;
import java.nio.*;
import java.lang.ref.*;

public class Session implements Resource.Resolver {
    public static final int PVER = 28;

    public static final int MSG_SESS = 0;
    public static final int MSG_REL = 1;
    public static final int MSG_ACK = 2;
    public static final int MSG_BEAT = 3;
    public static final int MSG_MAPREQ = 4;
    public static final int MSG_MAPDATA = 5;
    public static final int MSG_OBJDATA = 6;
    public static final int MSG_OBJACK = 7;
    public static final int MSG_CLOSE = 8;
    public static final int SESSERR_AUTH = 1;
    public static final int SESSERR_BUSY = 2;
    public static final int SESSERR_CONN = 3;
    public static final int SESSERR_PVER = 4;
    public static final int SESSERR_EXPR = 5;
    public static final int SESSERR_MESG = 6;

    static final int ackthresh = 30;

    public final Connection conn;
    public int connfailed = 0;
    public String connerror = null;
    LinkedList<PMessage> uimsgs = new LinkedList<PMessage>();
    String username;
    final Map<Integer, CachedRes> rescache = new TreeMap<Integer, CachedRes>();
    public final Glob glob;
    public byte[] sesskey;
    private boolean closed = false;

    @SuppressWarnings("serial")
    public static class MessageException extends RuntimeException {
	public Message msg;
		
	public MessageException(String text, Message msg) {
	    super(text);
	    this.msg = msg;
	}
    }

    public static class LoadingIndir extends Loading {
	public final int resid;
	private transient final CachedRes res;
	
	private LoadingIndir(CachedRes res) {
	    super("Waiting to resolve resource reference " + res.resid + "...");
	    this.res = res;
	    this.resid = res.resid;
	}

	public void waitfor(Runnable callback, Consumer<Waitable.Waiting> reg) {
	    synchronized(res) {
		if(res.resnm != null) {
		    reg.accept(Waitable.Waiting.dummy);
		    callback.run();
		} else {
		    reg.accept(res.wq.add(callback));
		}
	    }
	}

	public boolean boostprio(int prio) {
	    res.boostprio(prio);
	    return(true);
	}
    }

    private static class CachedRes {
	private final Waitable.Queue wq = new Waitable.Queue();
	private final int resid;
	private String resnm = null;
	private int resver;
	private Reference<Ref> ind;
	private int prio = -6;

	private CachedRes(int id) {
	    resid = id;
	}

	private class Ref implements Indir<Resource> {
	    private Resource res;

	    public Resource get() {
		if(res == null) {
		    synchronized(CachedRes.this) {
			if(res == null) {
			    if(resnm == null)
				throw(new LoadingIndir(CachedRes.this));
			    res = Resource.remote().load(resnm, resver, prio).get();
			}
		    }
		}
		return(res);
	    }

	    public String toString() {
		if(res == null) {
		    return("<res:" + resid + ">");
		} else {
		    return("<" + res + ">");
		}
	    }

	    private void reset() {
		res = null;
	    }
	}

	private Ref get() {
	    Ref ind = (this.ind == null) ? null : (this.ind.get());
	    if(ind == null)
		this.ind = new WeakReference<Ref>(ind = new Ref());
	    return(ind);
	}

	public void boostprio(int prio) {
	    if(this.prio < prio)
		this.prio = prio;
	}

	public void set(String nm, int ver) {
	    Resource.remote().load(nm, ver, -10);
	    synchronized(this) {
		this.resnm = nm;
		this.resver = ver;
		get().reset();
		wq.wnotify();
	    }
	}
    }

    private CachedRes cachedres(int id) {
	synchronized(rescache) {
	    CachedRes ret = rescache.get(id);
	    if(ret != null)
		return(ret);
	    ret = new CachedRes(id);
	    rescache.put(id, ret);
	    return(ret);
	}
    }

    public Indir<Resource> getres(int id, int prio) {
	CachedRes res = cachedres(id);
	res.boostprio(prio);
	return(res.get());
    }

    public Indir<Resource> getres(int id) {
	return(getres(id, 0));
    }

    private void handlerel(PMessage msg) {
	if((msg.type == RMessage.RMSG_NEWWDG) || (msg.type == RMessage.RMSG_WDGMSG) ||
	   (msg.type == RMessage.RMSG_DSTWDG) || (msg.type == RMessage.RMSG_ADDWDG) ||
	   (msg.type == RMessage.RMSG_WDGBAR))
	{
	    postuimsg(msg);
	} else if(msg.type == RMessage.RMSG_MAPIV) {
	    glob.map.invalblob(msg);
	} else if(msg.type == RMessage.RMSG_GLOBLOB) {
	    glob.blob(msg);
	} else if(msg.type == RMessage.RMSG_RESID) {
	    int resid = msg.uint16();
	    String resname = msg.string();
	    int resver = msg.uint16();
	    cachedres(resid).set(resname, resver);
	} else if(msg.type == RMessage.RMSG_SFX) {
	    Indir<Resource> resid = getres(msg.uint16());
	    double vol = ((double)msg.uint16()) / 256.0;
	    double spd = ((double)msg.uint16()) / 256.0;
	    glob.loader.defer(() -> {
		    Audio.CS clip = Audio.fromres(resid.get());
		    if(spd != 1.0)
			clip = new Audio.Resampler(clip).sp(spd);
		    if(vol != 1.0)
			clip = new Audio.VolAdjust(clip, vol);
		    Audio.play(clip);
		}, null);
	} else if(msg.type == RMessage.RMSG_MUSIC) {
	    String resnm = msg.string();
	    int resver = msg.uint16();
	    boolean loop = !msg.eom() && (msg.uint8() != 0);
	    if(Music.enabled) {
		if(resnm.equals(""))
		    Music.play(null, false);
		else
		    Music.play(Resource.remote().load(resnm, resver), loop);
	    }
	} else if(msg.type == RMessage.RMSG_SESSKEY) {
	    sesskey = msg.bytes();
	} else {
	    throw(new MessageException("Unknown rmsg type: " + msg.type, msg));
	}
    }

    private final Connection.Callback conncb = new Connection.Callback() {
	    public void closed() {
		synchronized(uimsgs) {
		    closed = true;
		    uimsgs.notifyAll();
		}
	    }

	    public void handle(PMessage msg) {
		handlerel(msg);
	    }

	    public void handle(OCache.ObjDelta delta) {
		glob.oc.receive(delta);
	    }

	    public void mapdata(Message msg) {
		glob.map.mapdata(msg);
	    }
	};

    public Session(SocketAddress server, String username, byte[] cookie, Object... args) throws InterruptedException {
	this.conn = new Connection(server, username);
	this.username = username;
	this.glob = new Glob(this);
	conn.add(conncb);
	conn.connect(cookie, args);
    }

    public void close() {
	conn.close();
    }

    public void queuemsg(PMessage pmsg) {
	conn.queuemsg(pmsg);
    }

    public void postuimsg(PMessage msg) {
	synchronized(uimsgs) {
	    uimsgs.add(msg);
	    uimsgs.notifyAll();
	}
    }

    public PMessage getuimsg() throws InterruptedException {
	synchronized(uimsgs) {
	    while(true) {
		if(!uimsgs.isEmpty())
		    return(uimsgs.remove());
		if(closed)
		    return(null);
		uimsgs.wait();
	    }
	}
    }

    public void sendmsg(PMessage msg) {
	conn.send(msg);
    }

    public void sendmsg(byte[] msg) {
	conn.send(ByteBuffer.wrap(msg));
    }
}
