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

import java.io.*;
import java.net.*;
import java.util.*;
import javax.net.ssl.*;
import java.security.cert.*;
import java.security.SecureRandom;
import java.math.BigInteger;

public class AuthClient implements Closeable {
    public static final Config.Variable<Boolean> strictcert = Config.Variable.propb("haven.auth-cert-strict", true);
    private static final SslHelper ssl;
    private final Socket sk;
    private final InputStream skin;
    private final OutputStream skout;
    
    static {
	ssl = new SslHelper();
	try {
	    ssl.trust(Resource.class.getResourceAsStream("authsrv.crt"));
	} catch(Exception e) {
	    throw(new RuntimeException(e));
	}
    }

    public AuthClient(String host, int port) throws IOException {
	boolean fin = false;
	SSLSocket sk = ssl.connect(host, port);
	try {
	    if(strictcert.get())
		checkname(host, sk.getSession());
	    this.sk = sk;
	    skin = sk.getInputStream();
	    skout = sk.getOutputStream();
	    fin = true;
	} finally {
	    if(!fin)
		sk.close();
	}
    }

    private void checkname(String host, SSLSession sess) throws IOException {
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

    public SocketAddress address() {
	return(sk.getRemoteSocketAddress());
    }

    public String trypasswd(String user, byte[] phash) throws IOException {
	Message rpl = cmd("pw", user, phash);
	String stat = rpl.string();
	if(stat.equals("ok")) {
	    String acct = rpl.string();
	    return(acct);
	} else if(stat.equals("no")) {
	    return(null);
	} else {
	    throw(new RuntimeException("Unexpected reply `" + stat + "' from auth server"));
	}
    }

    public String trytoken(String user, byte[] token) throws IOException {
	Message rpl = cmd("token", user, token);
	String stat = rpl.string();
	if(stat.equals("ok")) {
	    String acct = rpl.string();
	    return(acct);
	} else if(stat.equals("no")) {
	    return(null);
	} else {
	    throw(new RuntimeException("Unexpected reply `" + stat + "' from auth server"));
	}
    }
    
    public byte[] getcookie() throws IOException {
	Message rpl = cmd("cookie");
	String stat = rpl.string();
	if(stat.equals("ok")) {
	    return(rpl.bytes(32));
	} else {
	    throw(new RuntimeException("Unexpected reply `" + stat + "' from auth server"));
	}
    }

    public static class TokenInfo {
	public byte[] id = new byte[] {};
	public String desc = "";

	public TokenInfo id(byte[] id) {this.id = id; return(this);}
	public TokenInfo desc(String desc) {this.desc = desc; return(this);}

	public Object[] encode() {
	    Object[] ret = {};
	    if(this.id.length > 0)
		ret = Utils.extend(ret, new Object[] {new Object[] {"id", this.id}});
	    if(this.desc.length() > 0)
		ret = Utils.extend(ret, new Object[] {new Object[] {"desc", this.desc}});
	    return(ret);
	}

	public static TokenInfo forhost() {
	    TokenInfo ret = new TokenInfo();
	    if((ret.id = Utils.getprefb("token-id", ret.id)).length == 0) {
		ret.id = new byte[16];
		new SecureRandom().nextBytes(ret.id);
		Utils.setprefb("token-id", ret.id);
	    }
	    if((ret.desc = Utils.getpref("token-desc", null)) == null) {
		try {
		    ret.desc = InetAddress.getLocalHost().getHostName();
		} catch(UnknownHostException e) {
		    ret.desc = "";
		}
	    }
	    return(ret);
	}
    }

    public byte[] gettoken(TokenInfo info) throws IOException {
	Message rpl = cmd("mktoken", info.encode());
	String stat = rpl.string();
	if(stat.equals("ok")) {
	    return(rpl.bytes(32));
	} else {
	    throw(new RuntimeException("Unexpected reply `" + stat + "' from auth server"));
	}
    }
    public byte[] gettoken() throws IOException {
	return(gettoken(TokenInfo.forhost()));
    }
    
    public void close() throws IOException {
	sk.close();
    }

    private void sendmsg(MessageBuf msg) throws IOException {
	if(msg.size() > 65535)
	    throw(new RuntimeException("Too long message in AuthClient (" + msg.size() + " bytes)"));
	byte[] buf = new byte[msg.size() + 2];
	buf[0] = (byte)((msg.size() & 0xff00) >> 8);
	buf[1] = (byte)(msg.size() & 0x00ff);
	msg.fin(buf, 2);
	skout.write(buf);
    }
    
    private void esendmsg(Object... args) throws IOException {
	MessageBuf buf = new MessageBuf();
	for(Object arg : args) {
	    if(arg instanceof String) {
		buf.addstring((String)arg);
	    } else if(arg instanceof byte[]) {
		buf.addbytes((byte[])arg);
	    } else if(arg instanceof Object[]) {
		buf.addlist((Object[])arg);
	    } else {
		throw(new RuntimeException("Illegal argument to esendmsg: " + arg.getClass()));
	    }
	}
	sendmsg(buf);
    }

    private static void readall(InputStream in, byte[] buf) throws IOException {
	int rv;
	for(int i = 0; i < buf.length; i += rv) {
	    rv = in.read(buf, i, buf.length - i);
	    if(rv < 0)
		throw(new IOException("Premature end of input"));
	}
    }

    private Message recvmsg() throws IOException {
	byte[] header = new byte[2];
	readall(skin, header);
	int len = (Utils.ub(header[0]) << 8) | Utils.ub(header[1]);
	byte[] buf = new byte[len];
	readall(skin, buf);
	return(new MessageBuf(buf));
    }
    
    public Message cmd(Object... args) throws IOException {
	esendmsg(args);
	return(recvmsg());
    }
    
    public static abstract class Credentials {
	public abstract String tryauth(AuthClient cl) throws IOException;
	public abstract String name();
	public void discard() {}
	
	public static class AuthException extends RuntimeException {
	    public AuthException(String msg) {
		super(msg);
	    }
	}
    }

    public static class SrpAssertion {
	public static final Digest.Algorithm digest = Digest.SHA256;
	/* Safe 4096-bit prime and generator from RFC 5054 */
	public static final BigInteger N = b2i(new byte[] {
		  -1,   -1,   -1,   -1,   -1,   -1,   -1,   -1,  -55,   15,  -38,  -94,   33,  104,  -62,   52,
		 -60,  -58,   98, -117, -128,  -36,   28,  -47,   41,    2,   78,    8, -118,  103,  -52,  116,
		   2,   11,  -66,  -90,   59,   19, -101,   34,   81,   74,    8,  121, -114,   52,    4,  -35,
		 -17, -107,   25,  -77,  -51,   58,   67,   27,   48,   43,   10,  109,  -14,   95,   20,   55,
		  79,  -31,   53,  109,  109,   81,  -62,   69,  -28, -123,  -75,  118,   98,   94,  126,  -58,
		 -12,   76,   66,  -23,  -90,   55,  -19,  107,   11,   -1,   92,  -74,  -12,    6,  -73,  -19,
		 -18,   56,  107,   -5,   90, -119,  -97,  -91,  -82,  -97,   36,   17,  124,   75,   31,  -26,
		  73,   40,  102,   81,  -20,  -28,   91,   61,  -62,    0,  124,  -72,  -95,   99,  -65,    5,
		-104,  -38,   72,   54,   28,   85,  -45, -102,  105,   22,   63,  -88,   -3,   36,  -49,   95,
		-125,  101,   93,   35,  -36,  -93,  -83, -106,   28,   98,  -13,   86,   32, -123,   82,  -69,
		 -98,  -43,   41,    7,  112, -106, -106,  109,  103,   12,   53,   78,   74,  -68, -104,    4,
		 -15,  116,  108,    8,  -54,   24,   33,  124,   50, -112,   94,   70,   46,   54,  -50,   59,
		 -29,  -98,  119,   44,   24,   14, -122,    3, -101,   39, -125,  -94,  -20,    7,  -94, -113,
		 -75,  -59,   93,  -16,  111,   76,   82,  -55,  -34,   43,  -53,  -10, -107,   88,   23,   24,
		  57, -107,   73,  124,  -22, -107,  106,  -27,   21,  -46,   38,   24, -104,   -6,    5,   16,
		  21,  114, -114,   90, -118,  -86,  -60,   45,  -83,   51,   23,   13,    4,   80,  122,   51,
		 -88,   85,   33,  -85,  -33,   28,  -70,  100,  -20,   -5, -123,    4,   88,  -37,  -17,   10,
		-118,  -22,  113,   87,   93,    6,   12,  125,  -77, -105,   15, -123,  -90,  -31,  -28,  -57,
		 -85,  -11,  -82, -116,  -37,    9,   51,  -41,   30, -116, -108,  -32,   74,   37,   97,  -99,
		 -50,  -29,  -46,   38,   26,  -46,  -18,  107,  -15,   47,   -6,    6,  -39, -118,    8,  100,
		 -40,  118,    2,  115,   62,  -56,  106,  100,   82,   31,   43,   24,   23,  123,   32,   12,
		 -69,  -31,   23,   87,  122,   97,   93,  108,  119,    9, -120,  -64,  -70,  -39,   70,  -30,
		   8,  -30,   79,  -96,  116,  -27,  -85,   49,   67,  -37,   91,   -4,  -32,   -3,   16, -114,
		  75, -126,  -47,   32,  -87,   33,    8,    1,   26,  114,   60,   18,  -89, -121,  -26,  -41,
		-120,  113, -102,   16,  -67,  -70,   91,   38, -103,  -61,   39,   24,  106,  -12,  -30,   60,
		  26, -108,  104,   52,  -74,   21,   11,  -38,   37, -125,  -23,  -54,   42,  -44,   76,  -24,
		 -37,  -69,  -62,  -37,    4,  -34, -114,   -7,   46, -114,   -4,   20,   31,  -66,  -54,  -90,
		  40,  124,   89,   71,   78,  107,  -64,   93, -103,  -78, -106,   79,  -96, -112,  -61,  -94,
		  35,   59,  -95, -122,   81,   91,  -25,  -19,   31,   97,   41,  112,  -50,  -30,  -41,  -81,
		 -72,   27,  -35,  118,   33,  112,   72,   28,  -48,    6, -111,   39,  -43,  -80,   90,  -87,
		-109,  -76,  -22, -104, -115, -113,  -35,  -63, -122,   -1,  -73,  -36, -112,  -90,  -64, -113,
		  77,  -12,   53,  -55,   52,    6,   49, -103,   -1,   -1,   -1,   -1,   -1,   -1,   -1,   -1,
	    });
	public static final BigInteger g = n(2);
	public static final BigInteger k = b2i(Digest.hash(digest, i2b(N), i2b(g)));
	public final byte[] A;
	public final byte[] K;

	public static BigInteger n(long v) {
	    return(BigInteger.valueOf(v));
	}

	public static BigInteger b2i(byte[] b) {
	    return(new BigInteger(1, b));
	}

	public static byte[] i2b(BigInteger i) {
	    byte[] ret = i.toByteArray();
	    if(ret[0] == 0)
		ret = Utils.splice(ret, 1);
	    return(ret);
	}

	public SrpAssertion(byte[] phash, byte[] Bb) {
	    BigInteger B = b2i(Bb).mod(N);
	    if(B.equals(n(0)))
		throw(new Credentials.AuthException("Invalid SRP challenge"));
	    BigInteger x = b2i(phash);
	    byte[] ab = new byte[32];
	    new SecureRandom().nextBytes(ab);
	    BigInteger a = b2i(ab);
	    BigInteger A = g.modPow(a, N);
	    BigInteger u = b2i(Digest.hash(digest, i2b(A), i2b(B)));
	    BigInteger S = B.subtract(k.multiply(g.modPow(x, N))).mod(N).modPow(a.add(u.multiply(x)), N);
	    this.A = i2b(A);
	    this.K = Digest.hash(digest, i2b(S));
	}

	public byte[] sign(byte[] msg) {
	    return(Digest.hash(Digest.HMAC.of(Digest.SHA256, K), msg));
	}
    }

    public static class NativeCred extends Credentials {
	public final String username;
	private final byte[] pw;
	private final Runnable clean;
	
	public NativeCred(String username, byte[] pw) {
	    this.username = username;
	    this.pw = pw;
	    clean = Finalizer.finalize(this, () -> Arrays.fill(pw, (byte)0));
	}
	
	public NativeCred(String username, String pw) {
	    this(username, pw.getBytes(Utils.utf8));
	}
	
	public String name() {
	    return(username);
	}

	public static byte[] prehash(byte[] pw, Object[] spec) {
	    if(Utils.eq(spec[0], "sha256")) {
		return(Digest.hash(Digest.SHA256, pw));
	    } else if(Utils.eq(spec[0], "pbkdf2")) {
		return(Digest.pbkdf2(Digest.HMAC.of(Digest.SHA256, pw), (byte[])spec[2], 1 << Utils.iv(spec[1]), 32));
	    } else {
		throw(new AuthException("Unknown password prehash: " + spec[0]));
	    }
	}
	
	private byte[] hashpw(AuthClient cl) throws IOException {
	    Message rpl = cl.cmd("pwdata", username);
	    String stat = rpl.string();
	    if(stat.equals("no"))
		throw(new AuthException(rpl.string()));
	    else if(!stat.equals("ok"))
		throw(new RuntimeException("Unexpected reply " + stat + " from auth server"));
	    Object[] pwdata = rpl.list();
	    if(Utils.eq(pwdata[0], "prehash")) {
		return(prehash(pw, Utils.splice(pwdata, 1)));
	    } else if(Utils.eq(pwdata[0], "srp")) {
		byte[] phash = prehash(pw, (Object[])pwdata[1]);
		byte[] B = (byte[])pwdata[2];
		byte[] chal = (byte[])pwdata[3];
		SrpAssertion srp = new SrpAssertion(phash, B);
		MessageBuf resp = new MessageBuf();
		resp.addlist(srp.A, chal, srp.sign(chal));
		return(resp.fin());
	    } else {
		return(prehash(pw, pwdata));
	    }
	}

	public String tryauth(AuthClient cl) throws IOException {
	    Message rpl = cl.cmd("pw", username, hashpw(cl));
	    String stat = rpl.string();
	    if(stat.equals("ok")) {
		String acct = rpl.string();
		return(acct);
	    } else if(stat.equals("no")) {
		String err = rpl.string();
		throw(new AuthException(err));
	    } else {
		throw(new RuntimeException("Unexpected reply `" + stat + "' from auth server"));
	    }
	}
	
	public void discard() {
	    clean.run();
	}
    }

    public static class TokenCred extends Credentials implements Serializable {
	public final String acctname;
	public final byte[] token;
	private final Runnable clean;
	
	public TokenCred(String acctname, byte[] token) {
	    this.acctname = acctname;
	    if((this.token = token).length != 32)
		throw(new IllegalArgumentException("Token must be 32 bytes"));
	    clean = Finalizer.finalize(this, () -> Arrays.fill(token, (byte)0));
	}
	
	public String name() {
	    return(acctname);
	}
	
	public String tryauth(AuthClient cl) throws IOException {
	    Message rpl = cl.cmd("token", acctname, token);
	    String stat = rpl.string();
	    if(stat.equals("ok")) {
		String acct = rpl.string();
		return(acct);
	    } else if(stat.equals("no")) {
		String err = rpl.string();
		throw(new AuthException(err));
	    } else {
		throw(new RuntimeException("Unexpected reply `" + stat + "' from auth server"));
	    }
	}

	public void discard() {
	    clean.run();
	}
    }

    public static void main(final String[] args) throws Exception {
	Thread t = new HackThread(new Runnable() {
		public void run() {
		    try {
			AuthClient test = new AuthClient("127.0.0.1", 1871);
			try {
			    String acct = new NativeCred(args[0], args[1]).tryauth(test);
			    if(acct == null) {
				System.err.println("failed");
				return;
			    }
			    System.out.println(acct);
			    System.out.println(Utils.byte2hex(test.getcookie()));
			} finally {
			    test.close();
			}
		    } catch(Exception e) {
			throw(new RuntimeException(e));
		    }
		}
	    }, "Test");
	t.start();
	t.join();
    }
}
