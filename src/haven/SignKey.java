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

import java.math.*;
import java.util.*;
import java.util.function.*;
import java.security.*;
import java.security.spec.*;
import java.security.interfaces.*;

public interface SignKey {
    public static interface Algorithm {
	public SignKey generate();
    }

    public static interface Signer {
	public Signer update(byte[] buf, int off, int len);
	public default Signer update(byte[] part) {return(update(part, 0, part.length));}
	public byte[] sign();
    }

    public static interface Verifier {
	public Verifier update(byte[] buf, int off, int len);
	public default Verifier update(byte[] part) {return(update(part, 0, part.length));}
	public boolean verify(byte[] sig);
    }

    public Signer sign();
    public Verifier verify();
    public default byte[] sign(byte[]... parts) {
	Signer s = sign();
	for(byte[] part : parts)
	    s.update(part);
	return(s.sign());
    }
    public default boolean verify(byte[] sig, byte[]... parts) {
	Verifier v = verify();
	for(byte[] part : parts)
	    v.update(part);
	return(v.verify(sig));
    }

    public static abstract class Builtin implements SignKey {
	protected abstract Signature signer();
	protected abstract Signature verifier();

	protected byte[] int2ext(byte[] sig) {return(sig);}
	protected byte[] ext2int(byte[] sig) {return(sig);}

	public Signer sign() {
	    Signature sig = signer();
	    return(new Signer() {
		    public Signer update(byte[] buf, int off, int len) {
			try {
			    sig.update(buf, off, len);
			} catch(SignatureException e) {
			    throw(new AssertionError(e));
			}
			return(this);
		    }

		    public byte[] sign() {
			try {
			    return(int2ext(sig.sign()));
			} catch(SignatureException e) {
			    throw(new AssertionError(e));
			}
		    }
		});
	}

	public Verifier verify() {
	    Signature vfy = verifier();
	    return(new Verifier() {
		    public Verifier update(byte[] buf, int off, int len) {
			try {
			    vfy.update(buf, off, len);
			} catch(SignatureException e) {
			    throw(new AssertionError(e));
			}
			return(this);
		    }

		    public boolean verify(byte[] sig) {
			try {
			    return(vfy.verify(ext2int(sig)));
			} catch(SignatureException e) {
			    return(false);
			}
		    }
		});
	}

	private static KeyPairGenerator getgen(String nm, AlgorithmParameterSpec... pars) {
	    try {
		KeyPairGenerator ret = KeyPairGenerator.getInstance(nm);
		for(AlgorithmParameterSpec par : pars)
		    ret.initialize(par);
		return(ret);
	    } catch(NoSuchAlgorithmException | InvalidAlgorithmParameterException e) {
		throw(new UnsupportedOperationException(nm, e));
	    }
	}
    }

    public static class ECDSA extends Builtin {
	public final Curve crv;
	public final Hash hash;
	public final Format fmt;
	public final ECPrivateKey prv;
	public final ECPublicKey pub;

	public static enum Curve {
	    SECP256R1(new ECGenParameterSpec("secp256r1")),
	    SECP384R1(new ECGenParameterSpec("secp384r1")),
	    SECP521R1(new ECGenParameterSpec("secp521r1"));

	    private final KeyPairGenerator gen;
	    private final ECParameterSpec params;
	    Curve(AlgorithmParameterSpec par) {
		this.gen = Builtin.getgen("EC", par);
		try {
		    AlgorithmParameters pgen = AlgorithmParameters.getInstance("EC");
		    pgen.init(par);
		    this.params = pgen.getParameterSpec(ECParameterSpec.class);
		} catch(NoSuchAlgorithmException | InvalidParameterSpecException e) {
		    throw(new UnsupportedOperationException(e));
		}
	    }
	}

	public static enum Hash {
	    SHA256(32, Curve.SECP256R1), SHA384(48, Curve.SECP384R1), SHA512(64, Curve.SECP521R1);

	    public final int len;
	    public final Curve defcurve;

	    Hash(int len, Curve crv) {
		this.len = len;
		this.defcurve = crv;
	    }
	}

	public static enum Format {
	    X690, P1363;
	}

	public ECDSA(Curve crv, Hash hash, Format fmt, ECPublicKey pub, ECPrivateKey prv) {
	    this.crv = crv;
	    this.hash = hash;
	    this.fmt = fmt;
	    this.prv = prv;
	    this.pub = pub;
	}

	private Signature instance() {
	    try {
		switch(hash) {
		case SHA256: return(Signature.getInstance("SHA256withECDSA"));
		case SHA384: return(Signature.getInstance("SHA384withECDSA"));
		case SHA512: return(Signature.getInstance("SHA512withECDSA"));
		default: throw(new AssertionError());
		}
	    } catch(NoSuchAlgorithmException e) {
		throw(new AssertionError(e));
	    }
	}

	protected Signature signer() {
	    if(prv == null)
		throw(new UnsupportedOperationException());
	    Signature sig = instance();
	    try {sig.initSign(prv);} catch(InvalidKeyException e) {throw(new AssertionError(e));}
	    return(sig);
	}

	protected Signature verifier() {
	    Signature sig = instance();
	    try {sig.initVerify(pub);} catch(InvalidKeyException e) {throw(new AssertionError(e));}
	    return(sig);
	}

	private byte[] unsign(byte[] n) {
	    if((n.length == hash.len + 1) && (n[0] == 0))
		return(Utils.splice(n, 1));
	    if(n.length == hash.len)
		return(n);
	    throw(new Message.FormatError(String.format("unexpected number length %i (expected %i)", n.length, hash.len)));
	}

	private byte[] decberblock(Message data, int checktag) {
	    int tag = data.uint8();
	    int ln = data.uint8();
	    if((ln & 0x80) != 0) {
		int ln2 = 0;
		for(int i = 0; i < (ln & 0x7f); i++)
		    ln2 = (ln2 << 8) + data.uint8();
		ln = ln2;
	    }
	    if(tag != checktag)
		throw(new Message.FormatError(String.format("unexpected BER tag %x (expected %x)", tag, checktag)));
	    return(data.bytes(ln));
	}

	private byte[] encderblock(int tag, byte[] data) {
	    MessageBuf buf = new MessageBuf();
	    buf.adduint8(tag);
	    int ln = data.length;
	    if(ln < 128) {
		buf.adduint8(ln);
	    } else {
		byte[] lbuf = new byte[4];
		int o = 4;
		while(ln > 0) {
		    lbuf[--o] = (byte)(ln & 0xff);
		    ln >>= 8;
		}
		buf.addbytes(lbuf, o, lbuf.length - o);
	    }
	    buf.addbytes(data);
	    return(buf.fin());
	}

	protected byte[] int2ext(byte[] sig) {
	    if(fmt == Format.P1363) {
		Message buf = new MessageBuf(sig);
		buf = new MessageBuf(decberblock(buf, 0x30));
		byte[] r = decberblock(buf, 0x02);
		byte[] s = decberblock(buf, 0x02);
		return(Utils.concat(unsign(r), unsign(s)));
	    } else if(fmt == Format.X690) {
		return(sig);
	    }
	    throw(new AssertionError());
	}

	protected byte[] ext2int(byte[] sig) {
	    if(fmt == Format.P1363) {
		int p = sig.length / 2;
		byte[] r = Utils.splice(sig, 0, p);
		byte[] s = Utils.splice(sig, p);
		if(r[0] < 0) r = Utils.concat(new byte[] {0}, r);
		if(s[0] < 0) s = Utils.concat(new byte[] {0}, s);
		return(encderblock(0x30, Utils.concat(encderblock(0x02, r), encderblock(0x02, s))));
	    } else if(fmt == Format.X690) {
		return(sig);
	    }
	    throw(new AssertionError());
	}

	public static ECDSA generate(Curve crv, Hash hash, Format fmt) {
	    KeyPair eck = crv.gen.generateKeyPair();
	    return(new ECDSA(crv, hash, fmt, (ECPublicKey)eck.getPublic(), (ECPrivateKey)eck.getPrivate()));
	}
    }

    public static abstract class RSA extends Builtin {
	public final RSAPrivateKey prv;
	public final RSAPublicKey pub;

	public RSA(RSAPublicKey pub, RSAPrivateKey prv) {
	    this.prv = prv;
	    this.pub = pub;
	}
    }

    public static class RSA_PKCS1 extends RSA {
	public final Hash hash;

	public static enum Hash {
	    SHA256, SHA384, SHA512;
	}

	public RSA_PKCS1(Hash hash, RSAPublicKey pub, RSAPrivateKey prv) {
	    super(pub, prv);
	    this.hash = hash;
	}

	private Signature instance() {
	    try {
		switch(hash) {
		case SHA256: return(Signature.getInstance("SHA256withRSA"));
		case SHA384: return(Signature.getInstance("SHA384withRSA"));
		case SHA512: return(Signature.getInstance("SHA512withRSA"));
		default: throw(new AssertionError());
		}
	    } catch(NoSuchAlgorithmException e) {
		throw(new AssertionError(e));
	    }
	}

	protected Signature signer() {
	    if(prv == null)
		throw(new UnsupportedOperationException());
	    Signature sig = instance();
	    try {sig.initSign(prv);} catch(InvalidKeyException e) {throw(new AssertionError(e));}
	    return(sig);
	}

	protected Signature verifier() {
	    Signature sig = instance();
	    try {sig.initVerify(pub);} catch(InvalidKeyException e) {throw(new AssertionError(e));}
	    return(sig);
	}

	public static RSA_PKCS1 generate(int size, Hash hash) {
	    KeyPair rsk = Builtin.getgen("RSA", new RSAKeyGenParameterSpec(size, RSAKeyGenParameterSpec.F4)).generateKeyPair();
	    return(new RSA_PKCS1(hash, (RSAPublicKey)rsk.getPublic(), (RSAPrivateKey)rsk.getPrivate()));
	}
	public static RSA_PKCS1 generate(Hash hash) {return(generate(2048, hash));}
    }

    public static class RSA_PSS extends RSA {
	public final Hash hash;

	public static enum Hash {
	    SHA256(32, "SHA-256"), SHA384(48, "SHA-384"), SHA512(64, "SHA-512");

	    private final AlgorithmParameterSpec par;
	    private final boolean supported;
	    Hash(int len, String name) {
		this.par = new PSSParameterSpec(name, "MGF1", new MGF1ParameterSpec(name), len, 1);
		boolean s = true;
		try {
		    Signature.getInstance("RSASSA-PSS").setParameter(this.par);
		} catch(Exception e) {
		    s = false;
		}
		this.supported = s;
	    }
	}

	public RSA_PSS(Hash hash, RSAPublicKey pub, RSAPrivateKey prv) {
	    super(pub, prv);
	    this.hash = hash;
	}

	private Signature instance() {
	    try {
		Signature sig = Signature.getInstance("RSASSA-PSS");
		sig.setParameter(hash.par);
		return(sig);
	    } catch(NoSuchAlgorithmException | InvalidAlgorithmParameterException e) {
		throw(new AssertionError(e));
	    }
	}

	protected Signature signer() {
	    if(prv == null)
		throw(new UnsupportedOperationException());
	    Signature sig = instance();
	    try {sig.initSign(prv);} catch(InvalidKeyException e) {throw(new AssertionError(e));}
	    return(sig);
	}

	protected Signature verifier() {
	    Signature sig = instance();
	    try {sig.initVerify(pub);} catch(InvalidKeyException e) {throw(new AssertionError(e));}
	    return(sig);
	}

	public static RSA_PSS generate(int size, Hash hash) {
	    if(!hash.supported)
		throw(new UnsupportedOperationException("RSA-PSS-" + hash.name()));
	    KeyPair rsk = Builtin.getgen("RSA", new RSAKeyGenParameterSpec(size, RSAKeyGenParameterSpec.F4)).generateKeyPair();
	    return(new RSA_PSS(hash, (RSAPublicKey)rsk.getPublic(), (RSAPrivateKey)rsk.getPrivate()));
	}
	public static RSA_PSS generate(Hash hash) {return(generate(2048, hash));}
    }

    public static class HMAC implements SignKey {
	private final Digest.Algorithm dig;
	public final byte[] key;

	public HMAC(Digest.Algorithm dig, byte[] key) {
	    this.dig = dig;
	    this.key = key;
	}

	public Digest hmac() {
	    return(new Digest.HMAC(dig, key));
	}

	public Signer sign() {
	    Digest dig = hmac();
	    return(new Signer() {
		    public Signer update(byte[] buf, int off, int len) {
			dig.update(buf, off, len);
			return(this);
		    }

		    public byte[] sign() {
			return(dig.digest());
		    }
		});
	}

	public Verifier verify() {
	    Digest dig = hmac();
	    return(new Verifier() {
		    public Verifier update(byte[] buf, int off, int len) {
			dig.update(buf, off, len);
			return(this);
		    }

		    public boolean verify(byte[] sig) {
			return(MessageDigest.isEqual(dig.digest(), sig));
		    }
		});
	}

	public static Algorithm alg(Digest.Algorithm dig) {
	    return(new Algorithm() {
		    public SignKey generate() {
			byte[] key = new byte[dig.diglen()];
			new SecureRandom().nextBytes(key);
			return(new HMAC(dig, key));
		    }
		});
	}
    }

    public static class EncodingException extends RuntimeException {
	public EncodingException(String msg) {super(msg);}
    }

    public static class FormatException extends RuntimeException {
	public FormatException(String msg) {super(msg);}
    }

    public static class JWK {
	public static final Algorithm ES256 = () -> ECDSA.generate(ECDSA.Curve.SECP256R1, ECDSA.Hash.SHA256, ECDSA.Format.P1363);
	public static final Algorithm ES384 = () -> ECDSA.generate(ECDSA.Curve.SECP384R1, ECDSA.Hash.SHA384, ECDSA.Format.P1363);
	public static final Algorithm ES512 = () -> ECDSA.generate(ECDSA.Curve.SECP521R1, ECDSA.Hash.SHA512, ECDSA.Format.P1363);
	public static final Algorithm RS256 = () -> RSA_PKCS1.generate(RSA_PKCS1.Hash.SHA256);
	public static final Algorithm RS384 = () -> RSA_PKCS1.generate(RSA_PKCS1.Hash.SHA384);
	public static final Algorithm RS512 = () -> RSA_PKCS1.generate(RSA_PKCS1.Hash.SHA512);
	public static final Algorithm PS256 = () -> RSA_PSS.generate(RSA_PSS.Hash.SHA256);
	public static final Algorithm PS384 = () -> RSA_PSS.generate(RSA_PSS.Hash.SHA384);
	public static final Algorithm PS512 = () -> RSA_PSS.generate(RSA_PSS.Hash.SHA512);
	public static final Algorithm HS256 = HMAC.alg(Digest.SHA256);
	public static final Algorithm HS384 = HMAC.alg(Digest.SHA384);
	public static final Algorithm HS512 = HMAC.alg(Digest.SHA512);

	public static BigInteger b2i(byte[] b) {
	    return(new BigInteger(1, b));
	}

	public static byte[] i2b(BigInteger i) {
	    byte[] ret = i.toByteArray();
	    if(ret[0] == 0)
		ret = Utils.splice(ret, 1);
	    return(ret);
	}

	private static void format(Map<Object, Object> buf, ECDSA sig, boolean pub) {
	    buf.put("kty", "EC");
	    switch(sig.crv) {
	    case SECP256R1: buf.put("crv", "P-256"); break;
	    case SECP384R1: buf.put("crv", "P-384"); break;
	    case SECP521R1: buf.put("crv", "P-521"); break;
	    default: throw(new EncodingException(sig.hash.name()));
	    }
	    switch(sig.hash) {
	    case SHA256: buf.put("alg", "ES256"); break;
	    case SHA384: buf.put("alg", "ES384"); break;
	    case SHA512: buf.put("alg", "ES512"); break;
	    default: throw(new EncodingException(sig.hash.name()));
	    }
	    buf.put("x", Utils.ub64.enc(i2b(sig.pub.getW().getAffineX())));
	    buf.put("y", Utils.ub64.enc(i2b(sig.pub.getW().getAffineY())));
	    if(!pub) {
		if(sig.prv == null)
		    throw(new UnsupportedOperationException());
		buf.put("d", Utils.ub64.enc(i2b(sig.prv.getS())));
	    }
	}

	private static void format(Map<Object, Object> buf, RSA sig, boolean pub) {
	    buf.put("kty", "RSA");
	    buf.put("e", Utils.ub64.enc(i2b(sig.pub.getPublicExponent())));
	    buf.put("n", Utils.ub64.enc(i2b(sig.pub.getModulus())));
	    if(!pub) {
		if(sig.prv == null)
		    throw(new UnsupportedOperationException());
		buf.put("d", Utils.ub64.enc(i2b(sig.prv.getPrivateExponent())));
	    }
	}

	private static void format(Map<Object, Object> buf, RSA_PKCS1 sig, boolean pub) {
	    format(buf, (RSA)sig, pub);
	    switch(sig.hash) {
	    case SHA256: buf.put("alg", "RS256"); break;
	    case SHA384: buf.put("alg", "RS384"); break;
	    case SHA512: buf.put("alg", "RS512"); break;
	    default: throw(new EncodingException(sig.hash.name()));
	    }
	}

	private static void format(Map<Object, Object> buf, RSA_PSS sig, boolean pub) {
	    format(buf, (RSA)sig, pub);
	    switch(sig.hash) {
	    case SHA256: buf.put("alg", "RS256"); break;
	    case SHA384: buf.put("alg", "RS384"); break;
	    case SHA512: buf.put("alg", "RS512"); break;
	    default: throw(new EncodingException(sig.hash.name()));
	    }
	}

	private static void format(Map<Object, Object> buf, HMAC sig, boolean pub) {
	    if(sig.dig == Digest.SHA256) {
		buf.put("alg", "HS256");
	    } else if(sig.dig == Digest.SHA384) {
		buf.put("alg", "HS384");
	    } else if(sig.dig == Digest.SHA512) {
		buf.put("alg", "HS512");
	    } else {
		throw(new EncodingException(String.valueOf(sig.dig)));
	    }
	    buf.put("kty", "oct");
	    buf.put("k", Utils.ub64.enc(sig.key));
	}

	public static Map<Object, Object> format(SignKey sig, boolean pub) {
	    Map<Object, Object> ret = new HashMap<>();
	    if(sig instanceof ECDSA) {
		format(ret, (ECDSA)sig, pub);
	    } else if(sig instanceof RSA_PKCS1) {
		format(ret, (RSA_PKCS1)sig, pub);
	    } else if(sig instanceof RSA_PSS) {
		format(ret, (RSA_PSS)sig, pub);
	    } else if(sig instanceof HMAC) {
		format(ret, (HMAC)sig, pub);
	    } else {
		throw(new EncodingException("signature type unsupported for JWK: " + sig));
	    }
	    return(ret);
	}

	public static class JWKFormatException extends RuntimeException {
	    public final Map<?, ?> data;

	    public JWKFormatException(Map<?, ?> data, String msg) {
		super(msg + ": " + String.valueOf(data));
		this.data = data;
	    }
	}

	private static <T extends Key> Function<KeySpec, T> parser(Class<T> kcl, String algo) {
	    KeyFactory p;
	    try {
		p = KeyFactory.getInstance(algo);
	    } catch(NoSuchAlgorithmException e) {
		return(spec -> {throw(new UnsupportedOperationException(algo));});
	    }
	    if(PrivateKey.class.isAssignableFrom(kcl)) {
		return(spec -> {
			try {
			    return(kcl.cast(p.generatePrivate(spec)));
			} catch(InvalidKeySpecException e) {
			    throw(new AssertionError(e));
			}
		    });
	    } else {
		return(spec -> {
			try {
			    return(kcl.cast(p.generatePublic(spec)));
			} catch(InvalidKeySpecException e) {
			    throw(new AssertionError(e));
			}
		    });
	    }
	}

	private static final Function<KeySpec, RSAPublicKey>  rsapub = parser(RSAPublicKey.class,  "RSA");
	private static final Function<KeySpec, RSAPrivateKey> rsaprv = parser(RSAPrivateKey.class, "RSA");
	private static Pair<RSAPublicKey, RSAPrivateKey> rsaparse(Map<?, ?> data) {
	    if(!Utils.eq(data.get("kty"), "RSA"))
		throw(new JWKFormatException(data, "unexpected key type for RSA: " + data.get("kty")));
	    BigInteger n = b2i(Utils.ub64.dec(Utils.sv(data.get("n"))));
	    RSAPublicKey pub = rsapub.apply(new RSAPublicKeySpec(n, b2i(Utils.ub64.dec(Utils.sv(data.get("e"))))));
	    RSAPrivateKey prv = null;
	    if(data.containsKey("d"))
		prv = rsaprv.apply(new RSAPrivateKeySpec(n, b2i(Utils.ub64.dec(Utils.sv(data.get("d"))))));
	    return(new Pair<>(pub, prv));
	}

	private static final Function<KeySpec, ECPublicKey>  ecpub = parser(ECPublicKey.class,  "EC");
	private static final Function<KeySpec, ECPrivateKey> ecprv = parser(ECPrivateKey.class, "EC");
	private static ECDSA parse(Map<?, ?> data, ECDSA.Hash hash) {
	    if(!Utils.eq(data.get("kty"), "EC"))
		throw(new JWKFormatException(data, "unexpected key type for EC: " + data.get("kty")));
	    ECDSA.Curve crv;
	    switch(Utils.sv(data.get("crv"))) {
	    case "P-256": crv = ECDSA.Curve.SECP256R1; break;
	    default: throw(new JWKFormatException(data, "unexpected elliptic curve name: " + data.get("crv")));
	    }
	    ECPublicKey pub = ecpub.apply(new ECPublicKeySpec(new ECPoint(b2i(Utils.ub64.dec(Utils.sv(data.get("x")))), b2i(Utils.ub64.dec(Utils.sv(data.get("y"))))), crv.params));
	    ECPrivateKey prv = null;
	    if(data.containsKey("d"))
		prv = ecprv.apply(new ECPrivateKeySpec(b2i(Utils.ub64.dec(Utils.sv(data.get("d")))), crv.params));
	    return(new ECDSA(crv, hash, ECDSA.Format.P1363, pub, prv));
	}

	private static RSA_PKCS1 parse(Map<?, ?> data, RSA_PKCS1.Hash hash) {
	    Pair<RSAPublicKey, RSAPrivateKey> key = rsaparse(data);
	    return(new RSA_PKCS1(hash, key.a, key.b));
	}

	private static RSA_PSS parse(Map<?, ?> data, RSA_PSS.Hash hash) {
	    Pair<RSAPublicKey, RSAPrivateKey> key = rsaparse(data);
	    return(new RSA_PSS(hash, key.a, key.b));
	}

	private static HMAC hmacparse(Map<?, ?> data, Digest.Algorithm dig) {
	    if(!Utils.eq(data.get("kty"), "oct"))
		throw(new JWKFormatException(data, "unexpected key type for HMAC: " + data.get("kty")));
	    return(new HMAC(dig, Utils.ub64.dec(Utils.sv(data.get("k")))));
	}

	public static SignKey parse(Map<?, ?> data) {
	    String alg = Utils.sv(data.get("alg"));
	    if(alg == null)
		throw(new JWKFormatException(data, "not a jwk sigalg"));
	    switch(alg) {
	    case "ES256": return(parse(data, ECDSA.Hash.SHA256));
	    case "ES384": return(parse(data, ECDSA.Hash.SHA384));
	    case "ES512": return(parse(data, ECDSA.Hash.SHA512));
	    case "RS256": return(parse(data, RSA_PKCS1.Hash.SHA256));
	    case "RS384": return(parse(data, RSA_PKCS1.Hash.SHA384));
	    case "RS512": return(parse(data, RSA_PKCS1.Hash.SHA512));
	    case "PS256": return(parse(data, RSA_PSS.Hash.SHA256));
	    case "PS384": return(parse(data, RSA_PSS.Hash.SHA384));
	    case "PS512": return(parse(data, RSA_PSS.Hash.SHA512));
	    case "HS256": return(hmacparse(data, Digest.SHA256));
	    case "HS384": return(hmacparse(data, Digest.SHA384));
	    case "HS512": return(hmacparse(data, Digest.SHA512));
	    default: throw(new JWKFormatException(data, "unknown jwk sigalg name: " + alg));
	    }
	}
    }

    public static void main(String[] args) {
	System.out.println(Utils.hex.enc(JWK.PS256.generate().sign(args[0].getBytes(Utils.utf8))));
    }
}
