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
    public static final int PVER = 29;

    public static final int MSG_SESS = 0;
    public static final int MSG_REL = 1;
    public static final int MSG_ACK = 2;
    public static final int MSG_BEAT = 3;
    public static final int MSG_MAPREQ = 4;
    public static final int MSG_MAPDATA = 5;
    public static final int MSG_OBJDATA = 6;
    public static final int MSG_OBJACK = 7;
    public static final int MSG_CLOSE = 8;
    public static final int MSG_CRYPT = 9;
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
    public final User user;
    final Map<Integer, CachedRes> rescache = new TreeMap<Integer, CachedRes>();
    public final Glob glob;
    public SignKey sesskey;
    private boolean closed = false;

    public static class User {
	public final String name;
	public String alias = null, readname = null, prsname = null;

	public User(String name) {
	    this.name = name;
	}

	public User alias(String val) {alias = val; return(this);}
	public User readname(String val) {readname = val; return(this);}
	public User prsname(String val) {prsname = val; return(this);}

	public String readname() {return((readname != null) ? readname : name);}
	public String prsname() {return((prsname != null) ? prsname : name);}
	public String reauth() {return(name);}

	public User copy() {
	    User ret = new User(this.name);
	    ret.alias = this.alias;
	    ret.readname = this.readname;
	    ret.prsname = this.prsname;
	    return(ret);
	}
    }

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

    public Resource.Pool pool() {
	return(Resource.remote());
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
		if(res != null) {
		    return("<" + res + ">");
		} else if(resnm != null) {
		    return("<!" + resnm + ":" + resver + ">");
		} else {
		    return("<res:" + resid + ">");
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

    public Indir<Resource> dynres(UID uid) {
	return(Resource.remote().dynres(uid));
    }

    public final Function<Object, Object> resmapper = new ResID.ResolveMapper(this);

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
	} else if(msg.type == RMessage.RMSG_SESSKEY) {
	    sesskey = new SignKey.HMAC(Digest.SHA256, msg.bytes());
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

    public Session(SocketAddress server, User user, boolean encrypt, byte[] cookie, Object... args) throws InterruptedException {
	this.conn = new Connection(server);
	this.user = user;
	this.glob = new Glob(this);
	conn.add(conncb);
	conn.connect((user.alias != null) ? user.alias : user.name, encrypt, cookie, args);
	sesskey = SignKey.JWK.ES256.generate();
	queuemsg((PMessage)new PMessage(RMessage.RMSG_SESSKEY).addtto(SignKey.JWK.format(sesskey, true)));
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
