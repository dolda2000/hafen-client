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
import java.io.*;
import java.net.*;
import java.nio.*;
import java.nio.channels.*;
import java.security.*;
import javax.crypto.*;
import javax.crypto.spec.*;

public class Connection {
    public static final Config.Variable<Boolean> encrypt = Config.Variable.propb("haven.hcrypt", false);
    private static final double ACK_HOLD = 0.030;
    private static final double OBJACK_HOLD = 0.08, OBJACK_HOLD_MAX = 0.5;
    public final SocketAddress server;
    private final Collection<Callback> cbs = new ArrayList<>();
    private final DatagramChannel sk;
    private final Selector sel;
    private final SelectionKey key;
    private Worker worker;
    private int tseq;
    private boolean alive = true;
    private Crypto crypt;

    public Connection(SocketAddress server) {
	this.server = server;
	try {
	    this.sk = DatagramChannel.open();
	    try {
		sk.connect(server);
	    } catch(SocketException e) {
		/* Apparently, connect() can throw, among other
		 * things, NoRouteToHostException. */
		throw(new SessionConnError());
	    }
	    sk.configureBlocking(false);

	    sel = Selector.open();
	    key = sk.register(sel, SelectionKey.OP_READ);
	} catch(IOException e) {
	    throw(new RuntimeException(e));
	}
    }

    public static interface Callback {
	public default void closed() {};
	public default void handle(PMessage msg) {};
	public default void handle(OCache.ObjDelta delta) {};
	public default void mapdata(Message msg) {};

	public static class Dumper implements Callback {
	    public final Writer out;
	    private final double epoch;

	    public Dumper(Writer out) {
		this.out = out;
		this.epoch = Utils.rtime();
	    }

	    private void printf(String format, Object... args) {
		try {
		    out.write(String.format(format, args));
		} catch(IOException e) {
		    throw(new RuntimeException(e));
		}
	    }

	    public void closed() {
		printf("%4.6f close\n", Utils.rtime() - epoch);
		try {
		    out.close();
		} catch(IOException e) {
		    throw(new RuntimeException(e));
		}
	    }

	    public void handle(PMessage msg) {
		printf("%4.6f rmsg %d %s\n", Utils.rtime() - epoch, msg.type, Utils.bprint.enc(msg.bytes()));
	    }

	    public void handle(OCache.ObjDelta msg) {
		printf("%4.6f objd", Utils.rtime() - epoch);
		String fl = "";
		if(msg.initframe > 0) fl += "i";
		if((msg.fl & 2) != 0) fl += "v";
		if((msg.fl & 4) != 0) fl += "o";
		if(msg.rem) fl += "d";
		printf(" %s %d %d", (fl == "") ? "n" : fl, msg.id, msg.frame);
		if(msg.initframe > 0) printf(" %d", msg.initframe);
		for(OCache.AttrDelta attr : msg.attrs)
		    printf(" %d:%s", attr.type, Utils.bprint.enc(attr.bytes()));
		printf("\n");
	    }

	    public void mapdata(Message msg) {
		printf("%4.6f map %s\n", Utils.rtime() - epoch, Utils.b64.enc(msg.bytes()));
	    }
	}
    }

    public Connection add(Callback cb) {
	cbs.add(cb);
	return(this);
    }

    public static class DecryptException extends Exception {
	public DecryptException(String msg, Throwable cause) {super(msg, cause);}
	public DecryptException(String msg) {super(msg);}
    }

    private static boolean supported() {
	try {
	    Cipher.getInstance("AES/GCM/NoPadding");
	    return(true);
	} catch(Exception e) {
	    return(false);
	}
    }

    private class Crypto {
	private final Cipher cipher;
	private final Key tkey, rkey;
	private final NavigableSet<Long> rseqs = new TreeSet<>();
	private long tseq;

	private Crypto(byte[] cookie, byte[] salt) {
	    try {
		this.cipher = Cipher.getInstance("AES/GCM/NoPadding");
	    } catch(Exception e) {
		throw(new UnsupportedOperationException(e));
	    }
	    rseqs.add(-1L);
	    tkey = new SecretKeySpec(Digest.hkdf(Digest.SHA256, salt, cookie, "client".getBytes(Utils.ascii), 16), "AES");
	    rkey = new SecretKeySpec(Digest.hkdf(Digest.SHA256, salt, cookie, "server".getBytes(Utils.ascii), 16), "AES");
	}

	public synchronized byte[] encrypt(byte[] msg) {
	    long seq = tseq++;
	    byte[] iv = new byte[8];
	    Utils.int64e(seq, iv, 0);
	    try {
		cipher.init(Cipher.ENCRYPT_MODE, tkey, new GCMParameterSpec(128, iv));
	    } catch(InvalidKeyException | InvalidAlgorithmParameterException e) {
		throw(new AssertionError(e));
	    }
	    byte[] ct;
	    try {
		ct = cipher.doFinal(msg);
	    } catch(IllegalBlockSizeException | BadPaddingException e) {
		throw(new AssertionError(e));
	    }
	    byte[] ret = new byte[ct.length + 3];
	    System.arraycopy(ct, 0, ret, 3, ct.length);
	    ret[0] = (byte)((seq >>  0) & 0xff);
	    ret[1] = (byte)((seq >>  8) & 0xff);
	    ret[2] = (byte)((seq >> 16) & 0xff);
	    return(ret);
	}

	public synchronized byte[] decrypt(byte[] msg) throws DecryptException {
	    long mseq = rseqs.last();
	    long loseq = (msg[0] & 0xff) | ((msg[1] & 0xff) << 8) | ((msg[2] & 0xff) << 16);
	    long seq = (mseq & ~0xffffffL) | loseq;
	    if((Utils.sb(seq - mseq, 24) > 0) && (seq < mseq))
		seq += 0x1000000L;
	    else if((Utils.sb(loseq - mseq, 24) < 0) && (seq > mseq))
		seq -= 0x1000000L;
	    if(seq <= rseqs.first())
		throw(new DecryptException("duplicated packet"));
	    byte[] iv = new byte[8];
	    Utils.int64e(seq, iv, 0);
	    try {
		cipher.init(Cipher.DECRYPT_MODE, rkey, new GCMParameterSpec(128, iv));
	    } catch(InvalidKeyException | InvalidAlgorithmParameterException e) {
		throw(new AssertionError(e));
	    }
	    byte[] ret;
	    try {
		ret = cipher.doFinal(msg, 3, msg.length - 3);
	    } catch(IllegalBlockSizeException e) {
		throw(new AssertionError(e));
	    } catch(BadPaddingException e) {
		throw(new DecryptException("decryption failed", e));
	    }
	    if(!rseqs.add(seq))
		throw(new DecryptException("duplicated packet"));
	    while(rseqs.size() > 128)
		rseqs.pollFirst();
	    return(ret);
	}

	public PMessage encrypt(PMessage msg) {
	    byte[] buf = new byte[1 + msg.size()];
	    buf[0] = (byte)msg.type;
	    msg.fin(buf, 1);
	    PMessage ret = new PMessage(Session.MSG_CRYPT);
	    ret.addbytes(encrypt(buf));
	    return(ret);
	}

	public PMessage decrypt(MessageBuf msg) throws DecryptException {
	    byte[] dec = decrypt(msg.bytes());
	    return(new PMessage(dec[0], dec, 1, dec.length - 1));
	}
    }

    private class Worker extends HackThread {
	private Task init;
	
	private Worker(Task init) {
	    super("Connection worker");
	    setDaemon(true);
	    this.init = init;
	}

	public void run() {
	    Task task = init;
	    init = null;
	    try {
		/* Poor man's tail recursion. */
		while(task != null)
		    task = task.run();
	    } finally {
		try {
		    alive = false;
		    for(Callback cb : cbs)
			cb.closed();
		} finally {
		    try {
			sk.close();
			sel.close();
		    } catch(IOException e) {
			throw(new RuntimeException(e));
		    }
		}
	    }
	}
    }

    public interface Task {
	public Task run();
    }

    private void start(Task init) {
	synchronized(this) {
	    if(worker != null)
		throw(new IllegalStateException());
	    worker = new Worker(init);
	    worker.start();
	}
    }

    public boolean alive() {
	return(alive && (worker != null));
    }

    private final ByteBuffer recvbuf = ByteBuffer.allocate(65536);
    private PMessage recv() throws IOException {
	recvbuf.clear();
	int ret = sk.read(recvbuf);
	if(ret < 0) {
	    throw(new Error());
	} else if(ret == 0) {
	    return(null);
	} else {
	    recvbuf.flip();
	    byte type = recvbuf.get();
	    byte[] buf = new byte[recvbuf.remaining()];
	    recvbuf.get(buf);
	    return(new PMessage(type, buf));
	}
    }

    public void send(ByteBuffer msg) {
	try {
	    sk.write(msg);
	} catch(IOException e) {
	    /* Generally assume errors are transient and treat them as
	     * packet loss, but are there perhaps errors that
	     * shouldn't be considered transient? */
	}
    }

    public void send(PMessage msg) {
	if((crypt != null) && (msg.type != Session.MSG_CRYPT))
	    msg = crypt.encrypt(msg);
	ByteBuffer buf = ByteBuffer.allocate(msg.size() + 1);
	buf.put((byte)msg.type);
	msg.fin(buf);
	buf.flip();
	send(buf);
    }

    public void close() {
	if(worker == null)
	    throw(new IllegalStateException());
	worker.interrupt();
    }

    private boolean select(double timeout) throws IOException {
	sel.selectedKeys().clear();
	sel.select((long)Math.ceil(timeout * 1000));
	return(key.isReadable());
    }

    private void wake() {
	sel.wakeup();
    }

    private final List<RMessage> pending = new LinkedList<>();

    private class Connect implements Task {
	private final PMessage msg;
	private int result = -1;
	private Throwable cause;
	private String message;
	private Crypto crypt;

	private Connect(String username, boolean encrypt, byte[] cookie, Object... args) {
	    msg = new PMessage(Session.MSG_SESS);
	    String protocol = "Hafen";
	    if(!Config.confid.equals(""))
		protocol += "/" + Config.confid;
	    if(!encrypt) {
		msg.adduint16(2);
		msg.addstring(protocol);
		msg.adduint16(Session.PVER);
		msg.addstring(username);
		msg.adduint16(cookie.length);
		msg.addbytes(cookie);
		msg.addlist(args);
	    } else {
		byte[] salt = new byte[16];
		new SecureRandom().nextBytes(salt);
		msg.adduint16(2);
		msg.addstring("HCrypt");
		msg.adduint16(1);
		msg.addstring(username);
		msg.adduint8(salt.length);
		msg.addbytes(salt);
		MessageBuf enc = new MessageBuf();
		enc.addstring(protocol);
		enc.adduint16(Session.PVER);
		enc.addlist(args);
		crypt = new Crypto(cookie, salt);
		msg.addbytes(crypt.encrypt(enc.fin()));
	    }
	}

	public Task run() {
	    int retries = 0;
	    double last = 0;
	    try {
		while(true) {
		    double now = Utils.rtime();
		    if(now - last > 2) {
			if(++retries > 5) {
			    result = Session.SESSERR_CONN;
			    return(null);
			}
			send(msg);
			last = now;
		    }
		    try {
			if(select(Math.max(0.0, last + 2 - now))) {
			    PMessage msg = recv();
			    boolean cr = false;
			    if((msg != null) && (msg.type == Session.MSG_CRYPT) && (crypt != null)) {
				msg = crypt.decrypt(msg);
				cr = true;
			    }
			    if((msg != null) && (msg.type == Session.MSG_SESS)) {
				int error = msg.uint8();
				if(error == 0) {
				    if((crypt == null) || cr) {
					result = 0;
					Connection.this.crypt = crypt;
					return(new Main());
				    }
				} else {
				    this.result = error;
				    if(error == Session.SESSERR_MESG)
					message = msg.string();
				    return(null);
				}
			    }
			}
		    } catch(ClosedByInterruptException | CancelledKeyException e) {
			return(null);
		    } catch(DecryptException e) {
			new Warning(e).ctrace(false).issue();
		    } catch(IOException e) {
			result = Session.SESSERR_CONN;
			cause = e;
			return(null);
		    }
		}
	    } finally {
		synchronized(this) {
		    if(result < 0)
			result = Session.SESSERR_CONN;
		    notifyAll();
		}
	    }
	}
    }

    private static class ObjAck {
	long id;
	int frame;
	double frecv, lrecv;

	ObjAck(long id, int frame, double recv) {
	    this.id = id;
	    this.frame = frame;
	    this.frecv = this.lrecv = recv;
	}
    }

    private class Main implements Task {
	private final Map<Short, RMessage> waiting = new HashMap<>();
	private final Map<Long, ObjAck> objacks = new HashMap<>();
	private double now, lasttx;
	private short rseq, ackseq;
	private double acktime = -1;
	private byte[] fragbuf = null;
	private int fragtype;

	private void handlerel(PMessage msg) {
	    if(msg.type == RMessage.RMSG_FRAGMENT) {
		int head = msg.uint8();
		if((head & 0x80) == 0) {
		    if(fragbuf != null)
			throw(new Session.MessageException("Got start fragment while still defragmenting", msg));
		    fragbuf = msg.bytes();
		    fragtype = head;
		} else {
		    if((head == 0x80) || (head == 0x81)) {
			byte[] frag = msg.bytes();
			byte[] curbuf = fragbuf;
			byte[] newbuf = new byte[curbuf.length + frag.length];
			System.arraycopy(curbuf, 0, newbuf, 0, curbuf.length);
			System.arraycopy(frag, 0, newbuf, curbuf.length, frag.length);
			fragbuf = newbuf;
			if(head == 0x81) {
			    PMessage nmsg = new PMessage(fragtype, fragbuf);
			    fragbuf = null;
			    handlerel(nmsg);
			}
		    } else {
			throw(new Session.MessageException("Got invalid fragment type: " + head, msg));
		    }
		}
	    } else {
		for(Iterator<Callback> i = cbs.iterator(); i.hasNext();) {
		    Callback cb = i.next();
		    cb.handle(i.hasNext() ? msg.clone() : msg);
		}
	    }
	}

	private void gotrel(RMessage msg) {
	    short sd = (short)(msg.seq - rseq);
	    if(sd == 0) {
		short lastack;
		do {
		    handlerel(msg);
		    lastack = rseq++;
		    msg = waiting.remove(rseq);
		} while(msg != null);
		sendack(lastack);
	    } else if(sd > 0) {
		waiting.put((short)msg.seq, msg);
	    }
	}

	private void sendack(short seq) {
	    if(acktime < 0)
		acktime = now;
	    ackseq = seq;
	}

	private void gotack(short seq) {
	    synchronized(pending) {
		for(Iterator<RMessage> i = pending.iterator(); i.hasNext();) {
		    RMessage msg = i.next();
		    short sd = (short)(msg.seq - seq);
		    if(sd <= 0)
			i.remove();
		    else
			break;
		}
	    }
	}

	private void gotmapdata(MessageBuf msg) {
	    for(Iterator<Callback> i = cbs.iterator(); i.hasNext();) {
		Callback cb = i.next();
		cb.mapdata(i.hasNext() ? msg.clone() : msg);
	    }
	}

	private void gotobjdata(Message msg) {
	    while(!msg.eom()) {
		int fl = msg.uint8();
		long id = msg.uint32();
		int fr = msg.int32();
		OCache.ObjDelta delta = new OCache.ObjDelta(fl, id, fr);
		if((fl & 1) != 0)
		    delta.initframe = fr;
		if((fl & 8) != 0)
		    delta.initframe = msg.int32();
		while(true) {
		    int afl = 0, len, type = msg.uint8();
		    if(type == OCache.OD_END)
			break;
		    if((type & 0x80) == 0) {
			len = (type & 0x78) >> 3;
			if(len > 0)
			    len++;
			type = OCache.compodmap[type & 0x7];
		    } else {
			type = type & 0x7f;
			if(((afl = msg.uint8()) & 0x80) == 0) {
			    len = afl & 0x7f;
			    afl = 0;
			} else {
			    len = msg.uint16();
			}
		    }
		    OCache.AttrDelta attr = new OCache.AttrDelta(delta, type, msg, len);
		    if(type == OCache.OD_REM) {
			delta.rem = true;
		    } else {
			delta.attrs.add(attr);
		    }
		}
		for(Iterator<Callback> i = cbs.iterator(); i.hasNext();) {
		    Callback cb = i.next();
		    cb.handle(i.hasNext() ? delta.clone() : delta);
		}
		ObjAck ack = objacks.get(id);
		if(ack == null) {
		    objacks.put(id, ack = new ObjAck(id, fr, now));
		} else {
		    if(fr > ack.frame) {
			ack.frame = fr;
			ack.lrecv = now;
		    }
		}
	    }
	}

	private void handlemsg(PMessage msg) {
	    switch(msg.type) {
	    case Session.MSG_SESS: {
		break;
	    }
	    case Session.MSG_REL: {
		int seq = msg.uint16();
		while(!msg.eom()) {
		    int type = msg.uint8();
		    RMessage rmsg;
		    if((type & 0x80) != 0) {
			rmsg = new RMessage(type & 0x7f, msg.bytes(msg.uint16()));
		    } else {
			rmsg = new RMessage(type, msg.bytes());
		    }
		    rmsg.seq = seq++;
		    gotrel(rmsg);
		}
		break;
	    }
	    case Session.MSG_ACK: {
		gotack((short)msg.uint16());
		break;
	    }
	    case Session.MSG_MAPDATA: {
		gotmapdata(msg);
		break;
	    }
	    case Session.MSG_OBJDATA: {
		gotobjdata(msg);
		break;
	    }
	    }
	}

	private double min2(double a, double b) {
	    return((a < 0) ? b : Math.min(a, b));
	}

	private double sendpending() {
	    double mint = -1;
	    synchronized(pending) {
		for(RMessage msg : pending) {
		    double txtime;
		    if(msg.retx == 0)
			txtime = 0;
		    else if(msg.retx <= 1)
			txtime = 0.08;
		    else if(msg.retx <= 3)
			txtime = 0.20;
		    else if(msg.retx <= 9)
			txtime = 0.62;
		    else
			txtime = 2.00;
		    txtime = msg.last + txtime;
		    if(now >= txtime) {
			PMessage rmsg = new PMessage(Session.MSG_REL);
			rmsg.adduint16(msg.seq).adduint8(msg.type).addbytes(msg.fin());
			send(rmsg);
			msg.last = now;
			msg.retx++;
			lasttx = now;
		    } else {
			mint = min2(mint, txtime);
		    }
		}
	    }
	    return(mint);
	}

	private double sendobjacks() {
	    double mint = -1;
	    PMessage msg = null;
	    for(Iterator<ObjAck> i = objacks.values().iterator(); i.hasNext();) {
		ObjAck ack = i.next();
		double txtime = Math.min(ack.lrecv + OBJACK_HOLD, ack.frecv + OBJACK_HOLD_MAX);
		if(txtime <= now) {
		    if(msg == null) {
			msg = new PMessage(Session.MSG_OBJACK);
		    } else if(msg.size() > 1000 - 8) {
			send(msg);
			msg = new PMessage(Session.MSG_OBJACK);
		    }
		    msg.adduint32(ack.id);
		    msg.addint32(ack.frame);
		    i.remove();
		} else {
		    mint = min2(mint, txtime);
		}
	    }
	    if(msg != null) {
		send(msg);
		lasttx = now;
	    }
	    return(mint);
	}

	public Task run() {
	    lasttx = now = Utils.rtime();
	    double pendto = now;
	    while(true) {
		double to = 5 - (now - lasttx);
		if(acktime > 0)
		    to = Math.min(to, acktime + ACK_HOLD - now);
		if(pendto >= 0)
		    to = Math.min(to, pendto - now);

		try {
		    Utils.checkirq();
		    boolean readable = select(Math.max(to, 0));
		    now = Utils.rtime();
		    if(readable) {
			PMessage msg;
			while((msg = recv()) != null) {
			    if(crypt != null) {
				if(msg.type != Session.MSG_CRYPT)
				    continue;
				msg = crypt.decrypt(msg);
			    }
			    if(msg.type == Session.MSG_CLOSE)
				return(new Close(true));
			    handlemsg(msg);
			}
		    }
		} catch(ClosedByInterruptException | CancelledKeyException | InterruptedException e) {
		    return(new Close(false));
		} catch(PortUnreachableException e) {
		    return(null);
		} catch(DecryptException e) {
		    new Warning(e).ctrace(false).issue();
		} catch(IOException e) {
		    new Warning(e, "connection error").issue();
		    return(null);
		}

		pendto = min2(sendpending(), sendobjacks());
		if((acktime > 0) && (now - acktime >= ACK_HOLD)) {
		    send((PMessage)new PMessage(Session.MSG_ACK).adduint16(ackseq));
		    acktime = -1;
		    lasttx = now;
		}
		if(now - lasttx >= 5) {
		    send(new PMessage(Session.MSG_BEAT));
		    lasttx = now;
		}
	    }
	}
    }

    private class Close implements Task {
	private boolean sawclose;

	private Close(boolean sawclose) {
	    this.sawclose = sawclose;
	}

	public Task run() {
	    int retries = 0;
	    double last = 0;
	    while(true) {
		double now = Utils.rtime();
		if(now - last > 0.5) {
		    if(++retries > 5)
			return(null);
		    send(new PMessage(Session.MSG_CLOSE));
		    last = now;
		}
		try {
		    if(select(Math.max(0.0, last + 0.5 - now))) {
			PMessage msg = recv();
			if(msg != null) {
			    if((msg.type == Session.MSG_CRYPT) && (crypt != null))
				msg = crypt.decrypt(msg);
			    if(msg.type == Session.MSG_CLOSE)
				sawclose = true;
			}
		    }
		} catch(ClosedByInterruptException | CancelledKeyException e) {
		    /* XXX: I'm not really sure what causes
		     * CancelledKeyExceptions to occur here, but they
		     * seem to be somewhat common in practice. As far
		     * as my understanding goes, the channel should
		     * not be closed by interrupts so long as it is
		     * non-blocking, and interrupting a selecting
		     * thread shouldn't cause any channel closure. */
		    return(null);
		} catch(DecryptException e) {
		    new Warning(e).ctrace(false).issue();
		} catch(IOException e) {
		    return(null);
		}
		if(sawclose)
		    return(null);
	    }
	}
    }

    public void queuemsg(PMessage pmsg) {
	RMessage msg = new RMessage(pmsg);
	synchronized(pending) {
	    msg.seq = tseq;
	    tseq = (tseq + 1) & 0xffff;
	    pending.add(msg);
	}
	wake();
    }

    public boolean encrypted() {
	return(crypt != null);
    }

    public static class SessionError extends RuntimeException {
	public final int code;

	public SessionError(int code, String reason) {
	    super(reason);
	    this.code = code;
	}

	public SessionError(String reason) {
	    this(-1, reason);
	}
    }
    public static class SessionAuthError extends SessionError {
	public SessionAuthError() {super(Session.SESSERR_AUTH, "Invalid authentication token");}
    }
    public static class SessionBusyError extends SessionError {
	public SessionBusyError() {super(Session.SESSERR_BUSY, "Already logged in");}
    }
    public static class SessionConnError extends SessionError {
	public SessionConnError() {super(Session.SESSERR_CONN, "Could not connect to server");}
    }
    public static class SessionPVerError extends SessionError {
	public SessionPVerError() {super(Session.SESSERR_PVER, "This client is too old");}
    }
    public static class SessionExprError extends SessionError {
	public SessionExprError() {super(Session.SESSERR_EXPR, "Authentication token expired");}
    }

    public void connect(String username, boolean encrypt, byte[] cookie, Object... args) throws InterruptedException {
	Connect init = new Connect(username, encrypt, cookie, args);
	start(init);
	try {
	    synchronized(init) {
		while(init.result < 0)
		    init.wait();
	    }
	} catch(InterruptedException e) {
	    close();
	    throw(e);
	}
	if(init.result == 0)
	    return;
	close();
	switch(init.result) {
	case Session.SESSERR_AUTH:
	    throw(new SessionAuthError());
	case Session.SESSERR_BUSY:
	    throw(new SessionBusyError());
	case Session.SESSERR_CONN:
	    throw(new SessionConnError());
	case Session.SESSERR_PVER:
	    throw(new SessionPVerError());
	case Session.SESSERR_EXPR:
	    throw(new SessionExprError());
	case Session.SESSERR_MESG:
	    throw(new SessionError(Session.SESSERR_MESG, init.message));
	default:
	    throw(new SessionError("Connection failed: " + init.result));
	}
    }
}
