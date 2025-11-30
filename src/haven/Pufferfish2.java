/*
 * This file is just a conversion of the Pufferfish2 reference
 * implementation to Java, specifically for the Haven & Hearth game
 * client environment. Thec copyright of this file should thus be
 * considered the same as that of said reference implementation, as
 * follows:
 *
 * Copyright 2015, Jeremi M Gosney. All rights reserved.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package haven;

public class Pufferfish2 {
    private static final Utils.BinAscii ENCODING = new Utils.Base64("./ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789", '\0');
    private static final long[] IV = {
	0x243f6a8885a308d3L, 0x13198a2e03707344L,
	0xa4093822299f31d0L, 0x082efa98ec4e6c89L,
	0x452821e638d01377L, 0xbe5466cf34e90c6cL,
	0xc0ac29b7c97c50ddL, 0x3f84d5b5b5470917L,
	0x9216d5d98979fb1bL, 0xd1310ba698dfb5acL,
	0x2ffd72dbd01adfb7L, 0xb8e1afed6a267e96L,
	0xba7c9045f12c7f99L, 0x24a19947b3916cf7L,
	0x0801f2e2858efc16L, 0x636920d871574e69L,
	0xa458fea3f4933d7eL, 0x0d95748f728eb658L,
    };
    private static final Digest.Algorithm DIGEST = Digest.SHA512;
    private static final int DIGEST_LENGTH = DIGEST.diglen();
    private static final int SBOX_N = 4;

    private static long[] b2l(byte[] buf) {
	long[] ret = new long[buf.length / 8];
	for(int i = 0; i < ret.length; i++)
	    ret[i] = Utils.int64d(buf, i * 8);
	return(ret);
    }

    private static byte[] l2b(long[] buf) {
	byte[] ret = new byte[buf.length * 8];
	for(int i = 0; i < buf.length; i++)
	    Utils.int64e(buf[i], ret, i * 8);
	return(ret);
    }

    private static byte[] HMAC(byte[] key, byte[] data) {
	return(Digest.hash(Digest.HMAC.of(DIGEST, key), data));
    }

    public static class Instance {
	private final int cost_t, cost_m;
	private final int log2_sbox_sz, sbox_sz;
	private final long[][] S;
	private final long[] P = new long[18];
	private long L = 0, R = 0;
	private long[] salt_u64;

	private byte[] HASH_SBOX(byte[] x) {
	    for(int i = 0; i < SBOX_N; i++)
		x = HMAC(x, l2b(S[i]));
	    return(x);
	}

	private long F(long x) {
	    return(((S[0][(int)(x >>> (64 - log2_sbox_sz))] ^
		     S[1][(int)(x >>> 35) & (sbox_sz - 1)]) +
		     S[2][(int)(x >>> 19) & (sbox_sz - 1)]) ^
		     S[3][(int)(x >>>  3) & (sbox_sz - 1)]);
	}

	private void ENCIPHER() {
	    L ^= P[0];
	    R = (R ^ F(L)) ^ P[ 1];
	    L = (L ^ F(R)) ^ P[ 2];
	    R = (R ^ F(L)) ^ P[ 3];
	    L = (L ^ F(R)) ^ P[ 4];
	    R = (R ^ F(L)) ^ P[ 5];
	    L = (L ^ F(R)) ^ P[ 6];
	    R = (R ^ F(L)) ^ P[ 7];
	    L = (L ^ F(R)) ^ P[ 8];
	    R = (R ^ F(L)) ^ P[ 9];
	    L = (L ^ F(R)) ^ P[10];
	    R = (R ^ F(L)) ^ P[11];
	    L = (L ^ F(R)) ^ P[12];
	    R = (R ^ F(L)) ^ P[13];
	    L = (L ^ F(R)) ^ P[14];
	    R = (R ^ F(L)) ^ P[15];
	    L = (L ^ F(R)) ^ P[16];
	    R ^= P[17];
	    long LL = R;
	    long RR = L;
	    L = LL;
	    R = RR;
	}

	private void EXPANDSTATE(long[] buf, long a, long b, int c, int d) {
	    L ^= a;
	    R ^= b;
	    ENCIPHER();
	    buf[c] = L;
	    buf[d] = R;
	}

	private void EXPANDSTATE_NULL(long[] buf, int a, int b) {
	    ENCIPHER();
	    buf[a] = L;
	    buf[b] = R;
	}

	private void ENCRYPT_P() {
	    for(int i = 0; i < P.length; i += 2)
		EXPANDSTATE(P, salt_u64[(i + 0) % 8], salt_u64[(i + 1) % 8], i + 0, i + 1);
	}

	private void ENCRYPT_S() {
	    for(int s = 0; s < SBOX_N; s++)
		for(int i = 0; i < sbox_sz; i+= 2)
		    EXPANDSTATE(S[s], salt_u64[(i + 0) % 8], salt_u64[(i + 1) % 8], i + 0, i + 1);
	}

	private void REKEY(byte[] key) {
	    long[] key_u64 = b2l(key);
	    for(int i = 0; i < P.length; i++)
		P[i] ^= key_u64[i % 8];
	    for(int i = 0; i < P.length; i += 2)
		EXPANDSTATE_NULL(P, i + 0, i + 1);
	    for(int s = 0; s < SBOX_N; s++) {
		for(int i = 0; i < sbox_sz; i += 2)
		    EXPANDSTATE_NULL(S[s], i + 0, i + 1);
	    }
	}

	public Instance(int cost_t, int cost_m) {
	    this.cost_t = cost_t;
	    this.cost_m = cost_m;
	    log2_sbox_sz = cost_m + 5;
	    if(log2_sbox_sz > 30) throw(new IllegalArgumentException("m=" + cost_m));
	    sbox_sz = 1 << log2_sbox_sz;
	    S = new long[SBOX_N][sbox_sz];
	}

	public byte[] hash(byte[] key_r, byte[] salt_r) {
	    byte[] salt = HMAC(new byte[0], salt_r);
	    byte[] key = HMAC(salt, key_r);
	    salt_u64 = b2l(salt);
	    for(int i = 0; i < SBOX_N; i++) {
		for(int j = 0; j < sbox_sz; j += DIGEST_LENGTH / 8) {
		    key = HMAC(key, salt);
		    for(int k = 0; k < DIGEST_LENGTH / 8; k++)
			S[i][j + k] = Utils.int64d(key, k * 8);
		}
	    }
	    key = HASH_SBOX(key);
	    long[] key_u64 = b2l(key);
	    for(int i = 0; i < P.length; i++)
		P[i] = IV[i] ^ key_u64[i % 8];
	    ENCRYPT_P();
	    ENCRYPT_S();
	    for(long r = 0; r <= 1L << cost_t; r++) {
		L = R = 0;
		key = HASH_SBOX(key);
		REKEY(key);
	    }
	    key = HASH_SBOX(key);
	    return(key);
	}
    }

    public static byte[] hash(byte[] key, byte[] salt, int t, int m) {
	return(new Instance(t, m).hash(key, salt));
    }

    public static void main(String[] args) {
	int m, t;
	byte[] salt, key;
	if(args.length == 2) {
	    byte[] settings = ENCODING.dec(args[0]);
	    t = settings[0];
	    m = settings[1];
	    salt = Utils.splice(settings, 2);
	    key = args[1].getBytes(Utils.utf8);
	} else {
	    t = Integer.parseInt(args[0]);
	    m = Integer.parseInt(args[1]);
	    salt = args[2].getBytes(Utils.utf8);
	    key = args[3].getBytes(Utils.utf8);
	}
	for(int i = 0; i < 10; i++) {
	    double st = Utils.rtime();
	    byte[] hash = hash(key, salt, t, m);
	    double tm = Utils.rtime() - st;
	    System.out.printf("%s %f\n", ENCODING.enc(hash), tm * 1000);
	}
    }
}
