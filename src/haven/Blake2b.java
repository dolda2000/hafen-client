/*
 * This file is just a conversion of the Blake2b reference
 * implementation to Java, specifically for the Haven & Hearth game
 * client environment. Thec copyright of this file should thus be
 * considered the same as that of said reference implementation, as
 * follows:
 *
 * Copyright 2012, Samuel Neves <sneves@dei.uc.pt>.  You may use this under the
 * terms of the CC0, the OpenSSL Licence, or the Apache Public License 2.0, at
 * your option.  The terms of these licenses can be found at:
 *
 * - CC0 1.0 Universal : http://creativecommons.org/publicdomain/zero/1.0
 * - OpenSSL license   : https://www.openssl.org/source/license.html
 * - Apache 2.0        : http://www.apache.org/licenses/LICENSE-2.0
 *
 * More information about the BLAKE2 hash function can be found at
 * https://blake2.net.
 */

package haven;

public class Blake2b implements Digest.Algorithm {
    public static final int BLOCKBYTES = 128;
    public static final int OUTBYTES = 64;
    public static final int KEYBYTES = 64;
    public static final int SALTBYTES = 16;
    public static final int PERSONALBYTES = 16;
    private static final long IV[] = {
	0x6a09e667f3bcc908L, 0xbb67ae8584caa73bL,
	0x3c6ef372fe94f82bL, 0xa54ff53a5f1d36f1L,
	0x510e527fade682d1L, 0x9b05688c2b3e6c1fL,
	0x1f83d9abfb41bd6bL, 0x5be0cd19137e2179L,
    };
    private static final byte Σ[][] = {
	{ 0,  1,  2,  3,  4,  5,  6,  7,  8,  9, 10, 11, 12, 13, 14, 15},
	{14, 10,  4,  8,  9, 15, 13,  6,  1, 12,  0,  2, 11,  7,  5,  3},
	{11,  8, 12,  0,  5,  2, 15, 13, 10, 14,  3,  6,  7,  1,  9,  4},
	{ 7,  9,  3,  1, 13, 12, 11, 14,  2,  6,  5, 10,  4,  0, 15,  8},
	{ 9,  0,  5,  7,  2,  4, 10, 15, 14,  1, 11, 12,  6,  8,  3, 13},
	{ 2, 12,  6, 10,  0, 11,  8,  3,  4, 13,  7,  5, 15, 14,  1,  9},
	{12,  5,  1, 15, 14, 13,  4, 10,  0,  7,  6,  3,  9,  2,  8, 11},
	{13, 11,  7, 14, 12,  1,  3,  9,  5,  0, 15,  4,  8,  6,  2, 10},
	{ 6, 15, 14,  9, 11,  3,  0,  8, 12,  2, 13,  7,  1,  4, 10,  5},
	{10,  2,  8,  4,  7,  6,  1,  5, 15, 11,  9, 14,  3, 12, 13 , 0},
	{ 0,  1,  2,  3,  4,  5,  6,  7,  8,  9, 10, 11, 12, 13, 14, 15},
	{14, 10,  4,  8,  9, 15, 13,  6,  1, 12,  0,  2, 11,  7,  5,  3},
    };
    private final int digest_length;
    private final int key_length;
    private final int fanout = 1, depth = 1;
    private final int leaf_length = 0, node_offset = 0;
    private final int xof_length = 0, node_depth = 0;
    private final int inner_length = 0;
    private final byte[] salt = new byte[SALTBYTES];
    private final byte[] personal = new byte[PERSONALBYTES];
    private State zygote = null;
    private boolean first = true;

    public Blake2b(int digest_length, byte[] key, byte[] salt, byte[] personal) {
	this.digest_length = digest_length;
	if(salt != null)
	    System.arraycopy(salt, 0, this.salt, 0, salt.length);
	if(personal != null)
	    System.arraycopy(personal, 0, this.personal, 0, personal.length);
	if(key != null) {
	    if((key_length = key.length) > KEYBYTES)
		throw(new IllegalArgumentException("key"));
	    first = false;
	    zygote = new State();
	    zygote.update(key);
	    zygote.update(new byte[BLOCKBYTES - key.length]);
	} else {
	    key_length = 0;
	}
    }

    public Blake2b(byte[] key) {
	this(OUTBYTES, key, null, null);
    }

    public Blake2b(int digest_length) {
	this(digest_length, null, null, null);
    }

    public Blake2b() {
	this(OUTBYTES);
    }

    public int diglen() {return(digest_length);}
    public int blocklen() {return(BLOCKBYTES);}
    public Digest get() {
	if(first) {
	    first = false;
	    return(new State());
	} else {
	    if(zygote == null)
		zygote = new State();
	    return(zygote.copy());
	}
    }

    private byte[] packed() {
	byte[] ret = new byte[64];
	ret[0] = Utils.sb(digest_length);
	ret[1] = Utils.sb(key_length);
	ret[2] = Utils.sb(fanout);
	ret[3] = Utils.sb(depth);
	Utils.int32e(leaf_length, ret, 4);
	Utils.int32e(node_offset, ret, 8);
	Utils.int32e(xof_length, ret, 12);
	ret[16] = Utils.sb(node_depth);
	ret[17] = Utils.sb(inner_length);
	System.arraycopy(salt, 0, ret, 32, SALTBYTES);
	System.arraycopy(personal, 0, ret, 48, PERSONALBYTES);
	return(ret);
    }

    public class State implements Digest {
	private final long[] h = new long[8];
	private final long[] t = new long[2];
	private final long[] f = new long[2];
	private final byte[] buf = new byte[BLOCKBYTES];
	private final int outlen = digest_length;
	private int buflen = 0;
	private boolean last_node = false;

	private State() {
	    for(int i = 0; i < h.length; i++)
		h[i] = IV[i];
	    byte[] p = packed();
	    for(int i = 0; i < h.length; i++)
		h[i] ^= Utils.int64d(p, i * 8);
	}

	private State(State from) {
	    System.arraycopy(from.h, 0, this.h, 0, this.h.length);
	    System.arraycopy(from.t, 0, this.t, 0, this.t.length);
	    System.arraycopy(from.f, 0, this.f, 0, this.f.length);
	    System.arraycopy(from.buf, 0, this.buf, 0, this.buf.length);
	    this.buflen = from.buflen;
	    this.last_node = from.last_node;
	}

	private void increment_counter(long inc) {
	    long p = t[0];
	    t[0] += inc;
	    if(t[0] < p)
		t[1]++;
	}

	private void set_lastnode() {
	    f[1] = -1;
	}

	private boolean is_lastblock() {
	    return(f[0] != 0);
	}

	private void set_lastblock() {
	    if(last_node)
		set_lastnode();
	    f[0] = -1;
	}

	private void G(long[] m, long[] v, int r, int i, int a, int b, int c, int d) {
	    v[a] = v[a] + v[b] + m[Σ[r][2 * i + 0]];
	    v[d] = Long.rotateRight(v[d] ^ v[a], 32);
	    v[c] = v[c] + v[d];
	    v[b] = Long.rotateRight(v[b] ^ v[c], 24);
	    v[a] = v[a] + v[b] + m[Σ[r][2 * i + 1]];
	    v[d] = Long.rotateRight(v[d] ^ v[a], 16);
	    v[c] = v[c] + v[d];
	    v[b] = Long.rotateRight(v[b] ^ v[c], 63);
	}

	private void ROUND(long[] m, long[] v, int r) {
	    G(m, v, r, 0,  0,  4,  8, 12);
	    G(m, v, r, 1,  1,  5,  9, 13);
	    G(m, v, r, 2,  2,  6, 10, 14);
	    G(m, v, r, 3,  3,  7, 11, 15);
	    G(m, v, r, 4,  0,  5, 10, 15);
	    G(m, v, r, 5,  1,  6, 11, 12);
	    G(m, v, r, 6,  2,  7,  8, 13);
	    G(m, v, r, 7,  3,  4,  9, 14);
	}

	private void compress(byte[] buf, int off) {
	    long[] m = new long[16];
	    long[] v = new long[16];
	    for(int i = 0; i < 16; i++)
		m[i] = Utils.int64d(buf, off + (i * 8));
	    for(int i = 0; i < 8; i++)
		v[i] = h[i];
	    v[ 8] = IV[0];
	    v[ 9] = IV[1];
	    v[10] = IV[2];
	    v[11] = IV[3];
	    v[12] = IV[4] ^ t[0];
	    v[13] = IV[5] ^ t[1];
	    v[14] = IV[6] ^ f[0];
	    v[15] = IV[7] ^ f[1];
	    for(int i = 0; i < 12; i++)
		ROUND(m, v, i);
	    for(int i = 0; i < 8; i++)
		h[i] = h[i] ^ v[i] ^ v[i + 8];
	}

	public Digest update(byte[] src, int off, int len) {
	    if(is_lastblock())
		throw(new IllegalStateException("digest already finalized"));
	    int fill = buf.length - buflen;
	    if(len > fill) {
		System.arraycopy(src, off, buf, buflen, fill);
		buflen = 0;
		off += fill;
		len -= fill;
		increment_counter(BLOCKBYTES);
		compress(buf, 0);
		while(len > BLOCKBYTES) {
		    increment_counter(BLOCKBYTES);
		    compress(src, off);
		    off += BLOCKBYTES;
		    len -= BLOCKBYTES;
		}
	    }
	    if(len > 0) {
		System.arraycopy(src, off, buf, buflen, len);
		buflen += len;
	    }
	    return(this);
	}

	public byte[] digest() {
	    if(!is_lastblock()) {
		increment_counter(buflen);
		set_lastblock();
		for(int i = buflen; i < BLOCKBYTES; i++)
		    buf[i] = 0;
		compress(buf, 0);
	    }
	    byte[] ret = new byte[OUTBYTES];
	    for(int i = 0; i < OUTBYTES / 8; i++)
		Utils.int64e(h[i], ret, i * 8);
	    if(outlen < BLOCKBYTES)
		ret = Utils.splice(ret, 0, outlen);
	    return(ret);
	}

	public State copy() {
	    return(new State(this));
	}
    }
}
