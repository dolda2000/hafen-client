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
import java.nio.channels.*;
import java.security.*;
import java.security.cert.*;
import javax.net.ssl.*;
import java.nio.ByteBuffer;
import java.security.cert.Certificate;

public class SslChannel implements ByteChannel {
    public final ByteChannel bk;
    public final SSLEngine ssl;
    private ByteBuffer inbuf = null, outbuf = null;
    private ByteBuffer readbuf = ByteBuffer.allocate(512);
    private boolean haveshook = false;

    private static SSLContext defctx() {
	try {
	    return(SSLContext.getDefault());
	} catch(NoSuchAlgorithmException e) {
	    throw(new RuntimeException(e));
	}
    }

    private void initssl() {
	ssl.setUseClientMode(true);
    }

    public SslChannel(ByteChannel bk, SSLEngine ssl) {
	this.bk = bk;
	this.ssl = ssl;
	initssl();
    }

    public SslChannel(ByteChannel bk, SSLContext ctx) {
	this.bk = bk;
	if(ctx == null)
	    ctx = defctx();
	this.ssl = ctx.createSSLEngine();
	initssl();
    }

    public SslChannel(ByteChannel bk, SSLContext ctx, String host, int port) {
	this.bk = bk;
	if(ctx == null)
	    ctx = defctx();
	this.ssl = ctx.createSSLEngine(host, port);
	initssl();
    }

    public SslChannel(ByteChannel bk) {
	this(bk, (SSLContext)null);
    }

    private void tasks() {
	while(true) {
	    Runnable task = ssl.getDelegatedTask();
	    if(task == null)
		break;
	    task.run();
	}
    }

    private static final ByteBuffer empty = ByteBuffer.allocate(0);
    private void checkshake() throws IOException {
	if(!haveshook) {
	    ssl.beginHandshake();
	    haveshook = true;
	}
	SSLEngineResult.HandshakeStatus stat = ssl.getHandshakeStatus();
	while(true) {
	    switch(stat) {
	    case FINISHED:
	    case NOT_HANDSHAKING:
		return;
	    case NEED_WRAP: {
		if(outbuf == null)
		    outbuf = ByteBuffer.allocate(ssl.getSession().getPacketBufferSize());
		outbuf.clear();
		SSLEngineResult rv = ssl.wrap(empty, outbuf);
		switch(rv.getStatus()) {
		case OK: case CLOSED: break;
		default: throw(new RuntimeException(String.valueOf(rv.getStatus())));
		case BUFFER_OVERFLOW:
		    outbuf = ByteBuffer.allocate(outbuf.capacity() * 2).put((ByteBuffer)outbuf.flip());
		    break;
		}
		outbuf.flip();
		while(outbuf.hasRemaining())
		    bk.write(outbuf);
		stat = rv.getHandshakeStatus();
		break;
	    }
	    case NEED_UNWRAP: {
		SSLEngineResult rv = fill();
		if(rv == null)
		    throw(new SSLException("unexpected EOF from peer"));
		stat = rv.getHandshakeStatus();
		break;
	    }
	    case NEED_TASK:
		tasks();
		stat = ssl.getHandshakeStatus();
		break;
	    default:
		throw(new RuntimeException(String.valueOf(stat)));
	    }
	}
    }

    private SSLEngineResult fill() throws IOException {
	if(inbuf == null)
	    inbuf = ByteBuffer.allocate(ssl.getSession().getPacketBufferSize());
	while(inbuf.position() == 0) {
	    if(bk.read(inbuf) < 0)
		return(null);
	}
	inbuf.flip();
	SSLEngineResult rv = ssl.unwrap(inbuf, readbuf);
	inbuf.compact();
	switch(rv.getStatus()) {
	case OK: case CLOSED: break;
	default: throw(new RuntimeException(String.valueOf(rv.getStatus())));
	case BUFFER_UNDERFLOW:
	    if(bk.read(inbuf) < 0)
		throw(new SSLException("unexpected EOF from peer"));
	    break;
	case BUFFER_OVERFLOW:
	    readbuf = ByteBuffer.allocate(readbuf.capacity() * 2).put((ByteBuffer)readbuf.flip());
	    break;
	}
	tasks();
	return(rv);
    }

    public void handshake() throws IOException {
	checkshake();
    }

    public int read(ByteBuffer dst) throws IOException {
	checkshake();
	while(readbuf.position() == 0) {
	    if(ssl.isInboundDone())
		return(-1);
	    if(fill() == null) {
		/* This is a "clean" EOF, but is that actually OK to
		 * accept? Many HTTPS servers in actual fact seem to
		 * behave this way. */
		return(-1);
	    }
	}
	int rlen = readbuf.flip().limit(), xf = rlen;
	if(dst.remaining() < rlen)
	    readbuf.limit(xf = dst.remaining());
	dst.put(readbuf);
	readbuf.limit(rlen);
	readbuf.compact();
	return(xf);
    }

    public int write(ByteBuffer src, boolean ac) throws IOException {
	if(outbuf == null)
	    outbuf = ByteBuffer.allocate(ssl.getSession().getPacketBufferSize());
	int bytes = 0, plimit = src.limit(), max = ssl.getSession().getApplicationBufferSize();
	if(src.remaining() > max)
	    src.limit(src.position() + max);
	try {
	    retry: while(true) {
		checkshake();
		outbuf.clear();
		SSLEngineResult rv = ssl.wrap(src, outbuf);
		tasks();
		bytes += rv.bytesConsumed();
		switch(rv.getStatus()) {
		case OK:
		    break;
		case CLOSED:
		    if(ac)
			break;
		default:
		    throw(new RuntimeException(String.valueOf(rv.getStatus())));
		case BUFFER_OVERFLOW:
		    outbuf = ByteBuffer.allocate(outbuf.capacity() * 2).put((ByteBuffer)outbuf.flip());
		    continue retry;
		}
		outbuf.flip();
		while(outbuf.hasRemaining())
		    bk.write(outbuf);
		if(!src.hasRemaining() || (bytes > 0))
		    return(bytes);
	    }
	} finally {
	    src.limit(plimit);
	}
    }

    public int write(ByteBuffer src) throws IOException {
	return(write(src, false));
    }

    public void close() throws IOException {
	if(bk.isOpen() && haveshook) {
	    ssl.closeOutbound();
	    try {
		write(empty, true);
	    } catch(IOException e) {
		new Warning(e, "could not shut down ssl channel").issue();
	    }
	}
	bk.close();
    }

    public boolean isOpen() {
	return(bk.isOpen());
    }

    public SSLSession session() {
	return(ssl.getSession());
    }

    public void checkname(String host) throws IOException {
	checkshake();
	SSLSession sess = session();
	Certificate peer = sess.getPeerCertificates()[0];
	String dns = null;
	InetAddress ip = null;
	try {
	    ip = Utils.inet_pton(host);
	} catch(IllegalArgumentException e) {
	    dns = host;
	}
	if(peer instanceof X509Certificate) {
	    X509Certificate xc = (X509Certificate)peer;
	    try {
		Collection<List<?>> altnames = xc.getSubjectAlternativeNames();
		if(altnames == null)
		    throw(new SSLException("Unnamed authentication server certificate"));
		for(List<?> name : altnames) {
		    int type = ((Number)name.get(0)).intValue();
		    if((type == 2) && (dns != null)) {
			if(Utils.eq(name.get(1), dns))
			    return;
		    } else if((type == 7) && (ip != null)) {
			try {
			    if(Utils.eq(Utils.inet_pton((String)name.get(1)), ip))
				return;
			} catch(IllegalArgumentException e) {
			}
		    }
		}
	    } catch(CertificateException e) {
		throw(new SSLException("Illegal authentication server certificate", e));
	    }
	    throw(new SSLException("Authentication server name mismatch"));
	} else {
	    throw(new SSLException("Unknown certificate type, cannot validate: " + peer.getClass().getName()));
	}
	// throw(new AssertionError("unreachable"));
    }
}
