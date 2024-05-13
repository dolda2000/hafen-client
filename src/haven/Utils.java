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

import java.awt.RenderingHints;
import java.io.*;
import java.nio.*;
import java.nio.file.*;
import java.net.*;
import java.lang.ref.*;
import java.lang.reflect.*;
import java.util.prefs.*;
import java.security.*;
import java.util.*;
import java.util.function.*;
import java.awt.Graphics;
import java.awt.Color;
import java.awt.image.*;

public class Utils {
    public static final java.nio.charset.Charset utf8 = java.nio.charset.Charset.forName("UTF-8");
    public static final java.nio.charset.Charset ascii = java.nio.charset.Charset.forName("US-ASCII");
    public static final java.awt.image.ColorModel rgbm = java.awt.image.ColorModel.getRGBdefault();
    private static Preferences prefs = null;

    static Coord imgsz(BufferedImage img) {
	return(new Coord(img.getWidth(), img.getHeight()));
    }

    public static boolean checkhit(BufferedImage img, Coord c, int thres) {
	if(!c.isect(Coord.z, imgsz(img)))
	    return(false);
	if(img.getRaster().getNumBands() < 4)
	    return(true);
	return(img.getRaster().getSample(c.x, c.y, 3) >= thres);
    }
    public static boolean checkhit(BufferedImage img, Coord c) {
	return(checkhit(img, c, 128));
    }

    public static void defer(final Runnable r) {
	Defer.later(new Defer.Callable<Object>() {
		public Object call() {
		    r.run();
		    return(null);
		}
	    });
    }

    public static URI uri(String uri) {
	try {
	    return(new URI(uri));
	} catch(URISyntaxException e) {
	    throw(new IllegalArgumentException(uri, e));
	}
    }

    public static URL url(String url) {
	try {
	    return(uri(url).toURL());
	} catch(MalformedURLException e) {
	    throw(new IllegalArgumentException(url, e));
	}
    }

    public static Path path(String path) {
	if(path == null)
	    return(null);
	return(FileSystems.getDefault().getPath(path));
    }

    public static Path pj(Path base, String... els) {
	for(String el : els)
	    base = base.resolve(el);
	return(base);
    }

    public static Path srcpath(Class<?> cl) {
	java.security.ProtectionDomain d = cl.getProtectionDomain();
	if(d == null) throw(new IllegalArgumentException(String.valueOf(cl) + " has no prortection domain"));
	java.security.CodeSource s = d.getCodeSource();
	if(s == null) throw(new IllegalArgumentException(String.valueOf(cl) + " has no code source"));
	URL url = s.getLocation();
	if(url == null) throw(new IllegalArgumentException(String.valueOf(cl) + " has no location"));
	try {
	    return(Paths.get(url.toURI()));
	} catch(java.net.URISyntaxException e) {
	    throw(new IllegalArgumentException(String.valueOf(cl) + " has a malformed location", e));
	}
    }

    public static int drawtext(Graphics g, String text, Coord c) {
	java.awt.FontMetrics m = g.getFontMetrics();
	g.drawString(text, c.x, c.y + m.getAscent());
	return(m.getHeight());
    }

    static Coord textsz(Graphics g, String text) {
	java.awt.FontMetrics m = g.getFontMetrics();
	java.awt.geom.Rectangle2D ts = m.getStringBounds(text, g);
	return(new Coord((int)ts.getWidth(), (int)ts.getHeight()));
    }

    static void aligntext(Graphics g, String text, Coord c, double ax, double ay) {
	java.awt.FontMetrics m = g.getFontMetrics();
	java.awt.geom.Rectangle2D ts = m.getStringBounds(text, g);
	g.drawString(text, (int)(c.x - ts.getWidth() * ax), (int)(c.y + m.getAscent() - ts.getHeight() * ay));
    }

    public static String fpformat(int num, int div, int dec) {
	StringBuilder buf = new StringBuilder();
	boolean s = false;
	if(num < 0) {
	    num = -num; s = true;
	}
	for(int i = 0; i < div - dec; i++)
	    num /= 10;
	for(int i = 0; i < dec; i++) {
	    buf.append((char)('0' + (num % 10)));
	    num /= 10;
	}
	buf.append('.');
	if(num == 0) {
	    buf.append('0');
	} else {
	    while(num > 0) {
		buf.append((char)('0' + (num % 10)));
		num /= 10;
	    }
	}
	if(s)
	    buf.append('-');
	return(buf.reverse().toString());
    }

    public static String thformat(long num) {
	return(String.format("%,d", num));
    }

    /* These are horribly imprecise and ugly technically speaking, but
     * they should do for these simple purposes. */
    public static String odformat(double num, int md) {
	if(num < 0)
	    return("-" + odformat(-num, md));
	long dm = 1;
	for(int i = 0; i < md; i++) dm *= 10;
	long raw = (long)Math.round(num * dm);
	long ip = raw / dm;
	long dp = raw % dm;
	if(dp == 0)
	    return(Long.toString(ip));
	StringBuilder buf = new StringBuilder();
	buf.append(ip);
	buf.append('.');
	for(dm /= 10; dm > dp; dm /= 10) buf.append('0');
	while((dp % 10) == 0) dp /= 10;
	buf.append(dp);
	return(buf.toString());
    }

    public static String odformat2(double num, int md) {
	if(num < 0)
	    return("-" + odformat2(-num, md));
	if(num == 0)
	    return("0");
	long tm = 1;
	for(int i = 0; i < md; i++) tm *= 10;
	long dm;
	for(dm = tm; ((long)Math.round(num * dm)) < tm; dm *= 10);
	if(dm > tm)
	    dm /= 10;
	long raw = (long)Math.round(num * dm);
	long ip = raw / dm;
	long dp = raw % dm;
	if(dp == 0)
	    return(Long.toString(ip));
	StringBuilder buf = new StringBuilder();
	buf.append(ip);
	buf.append('.');
	for(dm /= 10; dm > dp; dm /= 10) buf.append('0');
	while((dp % 10) == 0) dp /= 10;
	buf.append(dp);
	return(buf.toString());
    }

    static void line(Graphics g, Coord c1, Coord c2) {
	g.drawLine(c1.x, c1.y, c2.x, c2.y);
    }

    static void AA(Graphics g) {
	java.awt.Graphics2D g2 = (java.awt.Graphics2D)g;
	g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);		
    }

    public static Random mkrandoom(long seed) {
	long rev = 0;
	for(int i = 0; i < 32; i++) {
	    rev |= (seed & (1l << i)) << (63 - (i * 2));
	    rev |= (seed & (1l << (63 - i))) >>> (63 - (i * 2));
	}
	seed = seed ^ rev;
	seed = (seed & 0x0000ffffffffffffl) ^ ((rev & 0xffffffffffff0000l) >>> 16);
	return(new Random(seed));
    }

    public static double fgrandoom(Random rnd) {
	long raw = rnd.nextLong();
	// 0000 bbbb baaa aabb bbba aaaa bbbb baaa aabb bbba aaaa bbbb baaa aabb bbba aaaa
	raw = (raw & 0x007c1f07c1f07c1fl) + ((raw & 0x0f83d0f83d0f83f0l) >> 5);
	// 0000 0000 bbbb bb00 00aa aaaa 0000 bbbb bb00 00aa aaaa 0000 bbbb bb00 00aa aaaa
	raw = (raw & 0x00003f0003f0003fl) + ((raw & 0x00fc000fc000fc00l) >> 10);
	// 0000 0000 0000 0000 0aaa aaaa 0000 0000 0000 0bbb bbbb 0000 0000 0000 0ccc cccc
	raw = ((raw & 0x00007f0000000000l) >> 40) + ((raw & 0x0000000007f00000l) >> 20) + (raw & 0x000000000000007fl);
	return((raw - 186) * (1.0 / 31.0));
    }

    private static Map<Object, Object> sysprefs() {
	try {
	    Properties buf = new Properties();
	    Optional<Path> pfile = Optional.ofNullable(System.getProperty("haven.prefs", null)).map(Utils::path);
	    if(pfile.isPresent() && Files.exists(pfile.get())) {
		try(InputStream fp = Files.newInputStream(pfile.get())) {
		    buf.load(fp);
		} catch(IOException e) {
		    new Warning(e, "could not read preferences file").level(Warning.ERROR).issue();
		}
	    }
	    for(Map.Entry<?, ?> ent : System.getProperties().entrySet()) {
		if((ent.getKey() instanceof String) && (ent.getValue() instanceof String) &&
		   ((String)ent.getKey()).startsWith("haven.prefs."))
		{
		    buf.put(((String)ent.getKey()).substring(12), (String)ent.getValue());
		}
	    }
	    return(buf);
	} catch(SecurityException e) {
	    return(Collections.emptyMap());
	}
    }

    public static final Config.Variable<String> prefspec = Config.Variable.prop("haven.prefspec", "hafen");
    public static Preferences prefs() {
	if(prefs == null) {
	    synchronized(Utils.class) {
		if(prefs == null) {
		    Map<Object, Object> sysprefs = sysprefs();
		    if(!sysprefs.isEmpty()) {
			prefs = new MapPrefs("haven", sysprefs);
		    } else {
			Preferences node = Preferences.userNodeForPackage(Utils.class);
			if(prefspec.get() != null)
			    node = node.node(prefspec.get());
			prefs = node;
		    }
		}
	    }
	}
	return(prefs);
    }

    public static String getpref(String prefname, String def) {
	try {
	    return(prefs().get(prefname, def));
	} catch(SecurityException e) {
	    return(def);
	}
    }

    public static void setpref(String prefname, String val) {
	try {
	    if(val == null)
		prefs().remove(prefname);
	    else
		prefs().put(prefname, val);
	} catch(SecurityException e) {
	}
    }

    public static int getprefi(String prefname, int def) {
	try {
	    return(prefs().getInt(prefname, def));
	} catch(SecurityException e) {
	    return(def);
	}
    }

    public static void setprefi(String prefname, int val) {
	try {
	    prefs().putInt(prefname, val);
	} catch(SecurityException e) {
	}
    }

    public static double getprefd(String prefname, double def) {
	try {
	    return(prefs().getDouble(prefname, def));
	} catch(SecurityException e) {
	    return(def);
	}
    }

    public static void setprefd(String prefname, double val) {
	try {
	    prefs().putDouble(prefname, val);
	} catch(SecurityException e) {
	}
    }

    public static boolean getprefb(String prefname, boolean def) {
	try {
	    return(prefs().getBoolean(prefname, def));
	} catch(SecurityException e) {
	    return(def);
	}
    }

    public static void setprefb(String prefname, boolean val) {
	try {
	    prefs().putBoolean(prefname, val);
	} catch(SecurityException e) {
	}
    }

    public static Coord getprefc(String prefname, Coord def) {
	try {
	    String val = prefs().get(prefname, null);
	    if(val == null)
		return(def);
	    int x = val.indexOf('x');
	    if(x < 0)
		return(def);
	    return(new Coord(Integer.parseInt(val.substring(0, x)), Integer.parseInt(val.substring(x + 1))));
	} catch(SecurityException e) {
	    return(def);
	}
    }

    public static void setprefc(String prefname, Coord val) {
	try {
	    String enc = (val == null) ? "" : val.x + "x" + val.y;
	    prefs().put(prefname, enc);
	} catch(SecurityException e) {
	}
    }

    public static byte[] getprefb(String prefname, byte[] def) {
	try {
	    return(prefs().getByteArray(prefname, def));
	} catch(SecurityException e) {
	    return(def);
	}
    }

    public static void setprefb(String prefname, byte[] val) {
	try {
	    prefs().putByteArray(prefname, val);
	} catch(SecurityException e) {
	}
    }

    public static List<String> getprefsl(String prefname, String[] def) {
	byte[] enc = getprefb(prefname, null);
	if(enc == null)
	    return((def == null) ? null : Arrays.asList(def));
	ByteBuffer buf = ByteBuffer.wrap(enc);
	ArrayList<String> ret = new ArrayList<>();
	for(int i = 0, s = 0; i < buf.capacity(); i++) {
	    if(buf.get(i) == 0) {
		buf.position(s).limit(i);
		CharBuffer dec = utf8.decode(buf);
		ret.add(dec.toString());
		s = i + 1;
		buf.limit(buf.capacity());
	    }
	}
	ret.trimToSize();
	return(ret);
    }

    public static void setprefsl(String prefname, Iterable<? extends CharSequence> val) {
	ByteBuffer buf = ByteBuffer.allocate(1024);
	for(CharSequence str : val) {
	    ByteBuffer enc = utf8.encode(CharBuffer.wrap(str));
	    buf = growbuf(buf, enc.remaining() + 1);
	    buf.put(enc);
	    buf.put((byte)0);
	}
	buf.flip();
	byte[] enc = new byte[buf.remaining()];
	buf.get(enc);
	setprefb(prefname, enc);
    }

    public static String getprop(String propname, String def) {
	try {
	    String ret;
	    if((ret = System.getProperty(propname)) != null)
		return(ret);
	    return(def);
	} catch(SecurityException e) {
	    return(def);
	}
    }

    public static int iv(Object arg) {
	return(((Number)arg).intValue());
    }

    public static long uiv(Object arg) {
	return(uint32(iv(arg)));
    }

    public static float fv(Object arg) {
	return(((Number)arg).floatValue());
    }

    public static double dv(Object arg) {
	return(((Number)arg).doubleValue());
    }

    public static boolean bv(Object arg) {
	return(iv(arg) != 0);
    }

    /* Nested format: [[KEY, VALUE], [KEY, VALUE], ...] */
    public static <K, V> Map<K, V> mapdecn(Object ob, Class<K> kt, Class<V> vt) {
	Map<K, V> ret = new HashMap<>();
	Object[] enc = (Object[])ob;
	for(Object sob : enc) {
	    Object[] ent = (Object[])sob;
	    ret.put(kt.cast(ent[0]), vt.cast(ent[1]));
	}
	return(ret);
    }
    public static Map<Object, Object> mapdecn(Object ob) {
	return(mapdecn(ob, Object.class, Object.class));
    }
    public static Object mapencn(Map<?, ?> map) {
	Object[] ret = new Object[map.size()];
	int a = 0;
	for(Map.Entry<?, ?> ent : map.entrySet())
	    ret[a++] = new Object[] {ent.getKey(), ent.getValue()};
	return(ret);
    }

    /* Flat format: [KEY, VALUE, KEY, VALUE, ...] */
    public static <K, V> Map<K, V> mapdecf(Object ob, Class<K> kt, Class<V> vt) {
	Map<K, V> ret = new HashMap<>();
	Object[] enc = (Object[])ob;
	for(int a = 0; a < enc.length - 1; a += 2)
	    ret.put(kt.cast(enc[a]), vt.cast(enc[a + 1]));
	return(ret);
    }
    public static Map<Object, Object> mapdecf(Object ob) {
	return(mapdecf(ob, Object.class, Object.class));
    }
    public static Object mapencf(Map<?, ?> map) {
	Object[] ret = new Object[map.size() * 2];
	int a = 0;
	for(Map.Entry<?, ?> ent : map.entrySet()) {
	    ret[a + 0] = ent.getKey();
	    ret[a + 1] = ent.getValue();
	}
	return(ret);
    }

    public static int sb(int n, int b) {
	return((n << (32 - b)) >> (32 - b));
    }

    public static int ub(byte b) {
	return(((int)b) & 0xff);
    }

    public static byte sb(int b) {
	return((byte)b);
    }

    public static byte f2s8(float v) {
	return((byte)Math.max(Math.min(Math.round(v * 127f), 127), -127));
    }

    public static byte f2u8(float v) {
	return((byte)Math.max(Math.min(Math.round(v * 255f), 255), 0));
    }

    public static long uint32(int n) {
	return(n & 0xffffffffl);
    }

    public static int uint16d(byte[] buf, int off) {
	return(ub(buf[off]) | (ub(buf[off + 1]) << 8));
    }

    public static int int16d(byte[] buf, int off) {
	return((int)(short)uint16d(buf, off));
    }

    public static long uint32d(byte[] buf, int off) {
	return((long)ub(buf[off]) | ((long)ub(buf[off + 1]) << 8) | ((long)ub(buf[off + 2]) << 16) | ((long)ub(buf[off + 3]) << 24));
    }

    public static void uint32e(long num, byte[] buf, int off) {
	buf[off] = (byte)(num & 0xff);
	buf[off + 1] = (byte)((num & 0x0000ff00) >> 8);
	buf[off + 2] = (byte)((num & 0x00ff0000) >> 16);
	buf[off + 3] = (byte)((num & 0xff000000) >> 24);
    }

    public static int int32d(byte[] buf, int off) {
	return((int)uint32d(buf, off));
    }

    public static long int64d(byte[] buf, int off) {
	long b = 0;
	for(int i = 0; i < 8; i++)
	    b |= ((long)ub(buf[off + i])) << (i * 8);
	return(b);
    }

    public static int intvard(byte[] buf, int off) {
	int len = buf.length - off;
	switch(len) {
	case 4:
	    return(int32d(buf, off));
	case 2:
	    return(int16d(buf, off));
	case 1:
	    return(buf[off]);
	default:
	    throw(new IllegalArgumentException(Integer.toString(len)));
	}
    }

    public static void int64e(long num, byte[] buf, int off) {
	for(int i = 0; i < 8; i++) {
	    buf[off++] = (byte)(num & 0xff);
	    num >>>= 8;
	}
    }

    public static void int32e(int num, byte[] buf, int off) {
	uint32e(((long)num) & 0xffffffff, buf, off);
    }

    public static void uint16e(int num, byte[] buf, int off) {
	buf[off] = sb(num & 0xff);
	buf[off + 1] = sb((num & 0xff00) >> 8);
    }

    public static void int16e(short num, byte[] buf, int off) {
	uint16e(((int)num) & 0xffff, buf, off);
    }

    public static String strd(byte[] buf, int[] off) {
	int i;
	for(i = off[0]; buf[i] != 0; i++);
	String ret;
	try {
	    ret = new String(buf, off[0], i - off[0], "utf-8");
	} catch(UnsupportedEncodingException e) {
	    throw(new IllegalArgumentException(e));
	}
	off[0] = i + 1;
	return(ret);
    }

    public static double floatd(byte[] buf, int off) {
	int e = buf[off];
	long t = uint32d(buf, off + 1);
	int m = (int)(t & 0x7fffffffL);
	boolean s = (t & 0x80000000L) != 0;
	if(e == -128) {
	    if(m == 0)
		return(0.0);
	    throw(new RuntimeException("Invalid special float encoded (" + m + ")"));
	}
	double v = (((double)m) / 2147483648.0) + 1.0;
	if(s)
	    v = -v;
	return(Math.pow(2.0, e) * v);
    }

    public static float float32d(byte[] buf, int off) {
	return(Float.intBitsToFloat(int32d(buf, off)));
    }

    public static double float64d(byte[] buf, int off) {
	return(Double.longBitsToDouble(int64d(buf, off)));
    }

    public static void float32e(float num, byte[] buf, int off) {
	int32e(Float.floatToIntBits(num), buf, off);
    }

    public static void float64e(double num, byte[] buf, int off) {
	int64e(Double.doubleToLongBits(num), buf, off);
    }

    public static void float9995d(int word, float[] ret) {
	int xb = (word & 0x7f800000) >> 23, xs = ((word & 0x80000000) >> 31) & 1,
	    yb = (word & 0x003fc000) >> 14, ys = ((word & 0x00400000) >> 22) & 1,
	    zb = (word & 0x00001fe0) >>  5, zs = ((word & 0x00002000) >> 13) & 1;
	int me = (word & 0x1f) - 15;
	int xe = Integer.numberOfLeadingZeros(xb) - 24,
	    ye = Integer.numberOfLeadingZeros(yb) - 24,
	    ze = Integer.numberOfLeadingZeros(zb) - 24;
	if(xe == 8) ret[0] = 0; else ret[0] = Float.intBitsToFloat((xs << 31) | ((me - xe + 127) << 23) | ((xb << (xe + 16)) & 0x007fffff));
	if(ye == 8) ret[1] = 0; else ret[1] = Float.intBitsToFloat((ys << 31) | ((me - ye + 127) << 23) | ((yb << (ye + 16)) & 0x007fffff));
	if(ze == 8) ret[2] = 0; else ret[2] = Float.intBitsToFloat((zs << 31) | ((me - ze + 127) << 23) | ((zb << (ze + 16)) & 0x007fffff));
    }

    public static float hfdec(short bits) {return(HalfFloat.bits(bits));}
    public static short hfenc(float f)    {return(HalfFloat.bits(f));}
    public static float mfdec(byte bits)  {return(MiniFloat.bits(bits));}
    public static byte  mfenc(float f)    {return(MiniFloat.bits(f));}

    public static void uvec2oct(float[] buf, float x, float y, float z) {
	float m = 1.0f / (Math.abs(x) + Math.abs(y) + Math.abs(z));
	float hx = x * m, hy = y * m;
	if(z >= 0) {
	    buf[0] = hx;
	    buf[1] = hy;
	} else {
	    buf[0] = (1 - Math.abs(hy)) * Math.copySign(1, hx);
	    buf[1] = (1 - Math.abs(hx)) * Math.copySign(1, hy);
	}
    }

    public static void oct2uvec(float[] buf, float x, float y) {
	float z = 1 - (Math.abs(x) + Math.abs(y));
	if(z < 0) {
	    float xc = x, yc = y;
	    x = (1 - Math.abs(yc)) * Math.copySign(1, xc);
	    y = (1 - Math.abs(xc)) * Math.copySign(1, yc);
	}
	float f = 1 / (float)Math.sqrt((x * x) + (y * y) + (z * z));
	buf[0] = x * f;
	buf[1] = y * f;
	buf[2] = z * f;
    }

    public static Coord3f oct2uvec(float x, float y) {
	float[] buf = new float[3];
	oct2uvec(buf, x, y);
	return(Coord3f.of(buf[0], buf[1], buf[2]));
    }

    static char num2hex(int num) {
	if(num < 10)
	    return((char)('0' + num));
	else
	    return((char)('A' + num - 10));
    }

    static int hex2num(char hex) {
	if((hex >= '0') && (hex <= '9'))
	    return(hex - '0');
	else if((hex >= 'a') && (hex <= 'f'))
	    return(hex - 'a' + 10);
	else if((hex >= 'A') && (hex <= 'F'))
	    return(hex - 'A' + 10);
	else
	    throw(new IllegalArgumentException());
    }

    public static String byte2hex(byte[] in) {
	StringBuilder buf = new StringBuilder();
	for(byte b : in) {
	    buf.append(num2hex((b & 0xf0) >> 4));
	    buf.append(num2hex(b & 0x0f));
	}
	return(buf.toString());
    }

    public static byte[] hex2byte(String hex) {
	if(hex.length() % 2 != 0)
	    throw(new IllegalArgumentException("Invalid hex-encoded string"));
	byte[] ret = new byte[hex.length() / 2];
	for(int i = 0, o = 0; i < hex.length(); i += 2, o++)
	    ret[o] = (byte)((hex2num(hex.charAt(i)) << 4) | hex2num(hex.charAt(i + 1)));
	return(ret);
    }

    private final static String base64set = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/";
    private final static int[] base64rev;
    static {
	int[] rev = new int[128];
	for(int i = 0; i < 128; rev[i++] = -1);
	for(int i = 0; i < base64set.length(); i++)
	    rev[base64set.charAt(i)] = i;
	base64rev = rev;
    }
    public static String base64enc(byte[] in) {
	StringBuilder buf = new StringBuilder();
	int p = 0;
	while(in.length - p >= 3) {
	    buf.append(base64set.charAt( (in[p + 0] & 0xfc) >> 2));
	    buf.append(base64set.charAt(((in[p + 0] & 0x03) << 4) | ((in[p + 1] & 0xf0) >> 4)));
	    buf.append(base64set.charAt(((in[p + 1] & 0x0f) << 2) | ((in[p + 2] & 0xc0) >> 6)));
	    buf.append(base64set.charAt(  in[p + 2] & 0x3f));
	    p += 3;
	}
	if(in.length == p + 1) {
	    buf.append(base64set.charAt( (in[p + 0] & 0xfc) >> 2));
	    buf.append(base64set.charAt( (in[p + 0] & 0x03) << 4));
	    buf.append("==");
	} else if(in.length == p + 2) {
	    buf.append(base64set.charAt( (in[p + 0] & 0xfc) >> 2));
	    buf.append(base64set.charAt(((in[p + 0] & 0x03) << 4) | ((in[p + 1] & 0xf0) >> 4)));
	    buf.append(base64set.charAt( (in[p + 1] & 0x0f) << 2));
	    buf.append("=");
	}
	return(buf.toString());
    }
    public static byte[] base64dec(String in) {
	ByteArrayOutputStream buf = new ByteArrayOutputStream();
	int cur = 0, b = 8;
	for(int i = 0; i < in.length(); i++) {
	    char c = in.charAt(i);
	    if(c >= 128)
		throw(new IllegalArgumentException());
	    if(c == '=')
		break;
	    int d = base64rev[c];
	    if(d == -1)
		throw(new IllegalArgumentException());
	    b -= 6;
	    if(b <= 0) {
		cur |= d >> -b;
		buf.write(cur);
		b += 8;
		cur = 0;
	    }
	    cur |= d << b;
	}
	return(buf.toByteArray());
    }

    public static String[] splitwords(String text) {
	ArrayList<String> words = new ArrayList<String>();
	StringBuilder buf = new StringBuilder();
	String st = "ws";
	int i = 0;
	while(i < text.length()) {
	    char c = text.charAt(i);
	    if(st == "ws") {
		if(!Character.isWhitespace(c))
		    st = "word";
		else
		    i++;
	    } else if(st == "word") {
		if(c == '"') {
		    st = "quote";
		    i++;
		} else if(c == '\\') {
		    st = "squote";
		    i++;
		} else if(Character.isWhitespace(c)) {
		    words.add(buf.toString());
		    buf = new StringBuilder();
		    st = "ws";
		} else {
		    buf.append(c);
		    i++;
		}
	    } else if(st == "quote") {
		if(c == '"') {
		    st = "word";
		    i++;
		} else if(c == '\\') {
		    st = "sqquote";
		    i++;
		} else {
		    buf.append(c);
		    i++;
		}
	    } else if(st == "squote") {
		buf.append(c);
		i++;
		st = "word";
	    } else if(st == "sqquote") {
		buf.append(c);
		i++;
		st = "quote";
	    }
	}
	if(st == "word")
	    words.add(buf.toString());
	if((st != "ws") && (st != "word"))
	    return(null);
	return(words.toArray(new String[0]));
    }

    public static String[] splitlines(String text) {
	ArrayList<String> ret = new ArrayList<String>();
	int p = 0;
	while(true) {
	    int p2 = text.indexOf('\n', p);
	    if(p2 < 0) {
		ret.add(text.substring(p));
		break;
	    }
	    ret.add(text.substring(p, p2));
	    p = p2 + 1;
	}
	return(ret.toArray(new String[0]));
    }

    static int atoi(String a) {
	try {
	    return(Integer.parseInt(a));
	} catch(NumberFormatException e) {
	    return(0);
	}
    }

    static void readtileof(InputStream in) throws IOException {
        byte[] buf = new byte[4096];
        while(true) {
            if(in.read(buf, 0, buf.length) < 0)
                return;
        }
    }

    public static byte[] readall(InputStream in) throws IOException {
	byte[] buf = new byte[4096];
	int off = 0;
	while(true) {
	    if(off == buf.length) {
		byte[] n = new byte[buf.length * 2];
		System.arraycopy(buf, 0, n, 0, buf.length);
		buf = n;
	    }
	    int ret = in.read(buf, off, buf.length - off);
	    if(ret < 0) {
		byte[] n = new byte[off];
		System.arraycopy(buf, 0, n, 0, off);
		return(n);
	    }
	    off += ret;
	}
    }

    public static interface IOFunction<T> {
	/* Checked exceptions banzai :P */
	public T run() throws IOException;
    }

    /* XXX: Sometimes, the client is getting strange and weird OS
     * errors on Windows. For example, file sharing violations are
     * sometimes returned even though Java always opens
     * RandomAccessFiles in non-exclusive mode, and other times,
     * permission is spuriously denied. I've had zero luck in trying
     * to find a root cause for these errors, so just assume the error
     * is transient and retry. :P */
    public static <T> T ioretry(IOFunction<? extends T> task) throws IOException {
	double[] retimes = {0.01, 0.1, 0.5, 1.0, 5.0};
	Throwable last = null;
	boolean intr = false;
	try {
	    for(int r = 0; true; r++) {
		try {
		    return(task.run());
		} catch(RuntimeException | IOException exc) {
		    if(last == null)
			new Warning(exc, "weird I/O error occurred on " + String.valueOf(task)).issue();
		    if(last != null)
			exc.addSuppressed(last);
		    last = exc;
		    if(r < retimes.length) {
			try {
			    Thread.sleep((long)(retimes[r] * 1000));
			} catch(InterruptedException irq) {
			    Thread.currentThread().interrupted();
			    intr = true;
			}
		    } else {
			throw(exc);
		    }
		}
	    }
	} finally {
	    if(intr)
		Thread.currentThread().interrupt();
	}
    }

    private static void dumptg(ThreadGroup tg, PrintWriter out, int indent) {
	for(int o = 0; o < indent; o++)
	    out.print("    ");
	out.println("G: \"" + tg.getName() + "\"");
	Thread[] ths = new Thread[tg.activeCount() * 2];
	ThreadGroup[] tgs = new ThreadGroup[tg.activeGroupCount() * 2];
	int nt = tg.enumerate(ths, false);
	int ng = tg.enumerate(tgs, false);
	for(int i = 0; i < nt; i++) {
	    Thread ct = ths[i];
	    for(int o = 0; o < indent + 1; o++)
		out.print("    ");
	    out.println("T: \"" + ct.getName() + "\"");
	}
	for(int i = 0; i < ng; i++) {
	    ThreadGroup cg = tgs[i];
	    dumptg(cg, out, indent + 1);
	}
    }

    public static void dumptg(ThreadGroup tg, PrintWriter out) {
	if(tg == null) {
	    tg = Thread.currentThread().getThreadGroup();
	    while(tg.getParent() != null)
		tg = tg.getParent();
	}
	dumptg(tg, out, 0);
	out.flush();
    }

    public static void dumparr(Object[] arr, PrintStream out, boolean term) {
	out.print('[');
	boolean f = true;
	for(Object o : arr) {
	    if(!f) out.print(", "); f = false;
	    if(o instanceof Object[])
		dumparr((Object[])o, out, false);
	    else
		out.print(o);
	}
	out.print(']');
	if(term) out.println();
    }

    public static void dumparr(double[] arr, PrintStream out, boolean term) {
	if(arr == null) {
	    out.print("null");
	} else {
	    out.print('[');
	    boolean f = true;
	    for(double v : arr) {
		if(!f) out.print(", "); f = false;
		out.print(v);
	    }
	    out.print(']');
	}
	if(term) out.println();
    }

    public static void dumparr(float[] arr, PrintStream out, boolean term) {
	if(arr == null) {
	    out.print("null");
	} else {
	    out.print('[');
	    boolean f = true;
	    for(float v : arr) {
		if(!f) out.print(", "); f = false;
		out.print(v);
	    }
	    out.print(']');
	}
	if(term) out.println();
    }

    public static void dumparr(long[] arr, PrintStream out, boolean term) {
	if(arr == null) {
	    out.print("null");
	} else {
	    out.print('[');
	    boolean f = true;
	    for(long i : arr) {
		if(!f) out.print(", "); f = false;
		out.print(i);
	    }
	    out.print(']');
	}
	if(term) out.println();
    }

    public static void dumparr(int[] arr, PrintStream out, boolean term) {
	if(arr == null) {
	    out.print("null");
	} else {
	    out.print('[');
	    boolean f = true;
	    for(int i : arr) {
		if(!f) out.print(", "); f = false;
		out.print(i);
	    }
	    out.print(']');
	}
	if(term) out.println();
    }

    public static void dumparr(short[] arr, PrintStream out, boolean term) {
	if(arr == null) {
	    out.print("null");
	} else {
	    out.print('[');
	    boolean f = true;
	    for(int i : arr) {
		if(!f) out.print(", "); f = false;
		out.print(i);
	    }
	    out.print(']');
	}
	if(term) out.println();
    }

    public static void hexdump(byte[] arr, PrintStream out, int width) {
	if(arr == null) {
	    out.println("null");
	    return;
	}
	if(width <= 0)
	    width = 16;
	for(int i = 0; i < arr.length; i += width) {
	    out.printf("%08x:\t", i);
	    for(int o = 0; (o < width) && (i + o < arr.length); o++) {
		if(o > 0) out.print(' ');
		out.printf("%02x", arr[i + o]);
	    }
	    for(int o = (Math.min(width, arr.length - i) * 3) - 1, w = (width * 3) - 1 + 8; o < w; o++)
		out.print(' ');
	    for(int o = 0; (o < width) && (i + o < arr.length); o++) {
		int b = arr[i + o] & 0xff;
		if((b < 32) || (b >= 127))
		    out.print('.');
		else
		    out.print((char)b);
	    }
	    out.print('\n');
	}
    }

    public static void hexdump(ByteBuffer arr, PrintStream out, int width) {
	if(arr == null) {
	    out.println("null");
	    return;
	}
	if(width <= 0)
	    width = 16;
	for(int i = 0; i < arr.capacity(); i += width) {
	    out.printf("%08x:\t", i);
	    for(int o = 0; (o < width) && (i + o < arr.capacity()); o++) {
		if(o > 0) out.print(' ');
		out.printf("%02x", arr.get(i + o) & 0xff);
	    }
	    for(int o = (Math.min(width, arr.capacity() - i) * 3) - 1, w = (width * 3) - 1 + 8; o < w; o++)
		out.print(' ');
	    for(int o = 0; (o < width) && (i + o < arr.capacity()); o++) {
		int b = arr.get(i + o) & 0xff;
		if((b < 32) || (b >= 127))
		    out.print('.');
		else
		    out.print((char)b);
	    }
	    out.print('\n');
	}
    }

    public static String titlecase(String str) {
	return(Character.toTitleCase(str.charAt(0)) + str.substring(1));
    }

    public static Color contrast(Color col) {
	int max = Math.max(col.getRed(), Math.max(col.getGreen(), col.getBlue()));
	if(max > 128) {
	    return(new Color(col.getRed() / 4, col.getGreen() / 4, col.getBlue() / 4, col.getAlpha()));
	} else if(max == 0) {
	    return(Color.WHITE);
	} else {
	    int f = 65025 / max;
	    return(new Color((col.getRed() * f) / 255, (col.getGreen() * f) / 255, (col.getBlue() * f) / 255, col.getAlpha()));
	}
    }

    public static Color clipcol(int r, int g, int b, int a) {
	if(r < 0)   r = 0;
	if(r > 255) r = 255;
	if(g < 0)   g = 0;
	if(g > 255) g = 255;
	if(b < 0)   b = 0;
	if(b > 255) b = 255;
	if(a < 0)   a = 0;
	if(a > 255) a = 255;
	return(new Color(r, g, b, a));
    }

    public static Color col16(int col) {
        return(new Color(((col & 0xf000) >> 12) * 17,
                         ((col & 0x0f00) >>  8) * 17,
                         ((col & 0x00f0) >>  4) * 17,
                         ((col & 0x000f) >>  0) * 17));
    }

    public static BufferedImage outline(BufferedImage img, Color col) {
	Coord sz = imgsz(img).add(2, 2);
	BufferedImage ol = TexI.mkbuf(sz);
	Object fcol = ol.getColorModel().getDataElements(col.getRGB(), null);
	Raster src = img.getRaster();
	WritableRaster dst = ol.getRaster();
	for(int y = 0; y < sz.y; y++) {
	    for(int x = 0; x < sz.x; x++) {
		boolean t;
		if((y == 0) || (x == 0) || (y == sz.y - 1) || (x == sz.x - 1)) {
		    t = true;
		} else {
		    t = src.getSample(x - 1, y - 1, 3) < 250;
		}
		if(!t)
		    continue;
		if(((x > 1) && (y > 0) && (y < sz.y - 1) && (src.getSample(x - 2, y - 1, 3) >= 250)) ||
		   ((x > 0) && (y > 1) && (x < sz.x - 1) && (src.getSample(x - 1, y - 2, 3) >= 250)) ||
		   ((x < sz.x - 2) && (y > 0) && (y < sz.y - 1) && (src.getSample(x, y - 1, 3) >= 250)) ||
		   ((x > 0) && (y < sz.y - 2) && (x < sz.x - 1) && (src.getSample(x - 1, y, 3) >= 250)))
		    dst.setDataElements(x, y, fcol);
	    }
	}
	return(ol);
    }

    public static BufferedImage outline2(BufferedImage img, Color col) {
	BufferedImage ol = outline(img, col);
	Graphics g = ol.getGraphics();
	g.drawImage(img, 1, 1, null);
	g.dispose();
	return(ol);
    }

    public static int floordiv(int a, int b) {
	if(a < 0)
	    return(((a + 1) / b) - 1);
	else
	    return(a / b);
    }

    public static int floormod(int a, int b) {
	int r = a % b;
	if(r < 0)
	    r += b;
	return(r);
    }

    /* Note: Math.floor has historically been surprisingly slow to the
     * extent that it has required a floordiv implementation that
     * avoids using it. That doesn't seem to be the case any longer,
     * but maybe keep an eye open... */
    public static int floordiv(float a, float b) {
	return((int)Math.floor(a / b));
    }
    public static int floordiv(double a, double b) {
	return((int)Math.floor(a / b));
    }

    public static float floormod(float a, float b) {
	return(a - (floordiv(a, b) * b));
    }
    public static double floormod(double a, double b) {
	return(a - (floordiv(a, b) * b));
    }

    public static double cangle(double a) {
	while(a > Math.PI)
	    a -= Math.PI * 2;
	while(a < -Math.PI)
	    a += Math.PI * 2;
	return(a);
    }

    public static double cangle2(double a) {
	while(a > Math.PI * 2)
	    a -= Math.PI * 2;
	while(a < 0)
	    a += Math.PI * 2;
	return(a);
    }

    public static double clip(double d, double min, double max) {
	if(d < min)
	    return(min);
	if(d > max)
	    return(max);
	return(d);
    }

    public static float clip(float d, float min, float max) {
	if(d < min)
	    return(min);
	if(d > max)
	    return(max);
	return(d);
    }

    public static int clip(int i, int min, int max) {
	if(i < min)
	    return(min);
	if(i > max)
	    return(max);
	return(i);
    }

    public static double clipnorm(double d, double min, double max) {
	if(d < min)
	    return(0.0);
	if(d > max)
	    return(1.0);
	return((d - min) / (max - min));
    }

    public static <E, O extends Comparable<? super O>> E max(Collection<E> from, Function<? super E, O> key) {
	E ret = null;
	O max = null;
	for(E el : from) {
	    O score = key.apply(el);
	    if((max == null) || (score.compareTo(max) > 0)) {
		ret = el;
		max = score;
	    }
	}
	return(ret);
    }

    public static <E, O extends Comparable<? super O>> E min(Collection<E> from, Function<? super E, O> key) {
	E ret = null;
	O max = null;
	for(E el : from) {
	    O score = key.apply(el);
	    if((max == null) || (score.compareTo(max) < 0)) {
		ret = el;
		max = score;
	    }
	}
	return(ret);
    }

    public static <E extends Comparable<? super E>> E max(Collection<E> from) {return(max(from, Function.identity()));}
    public static <E extends Comparable<? super E>> E min(Collection<E> from) {return(min(from, Function.identity()));}

    public static float gcd(float x, float y, float E) {
	float a = Math.max(x, y), b = Math.min(x, y);
	while(b > E) {
	    float c = a % b;
	    a = b;
	    b = c;
	}
	return(a);
    }

    public static float smoothstep(float d) {
	return(d * d * (3 - (2 * d)));
    }

    public static double smoothstep(double d) {
	return(d * d * (3 - (2 * d)));
    }

    public static Color blendcol(Color in, Color bl) {
	int f1 = bl.getAlpha();
	int f2 = 255 - bl.getAlpha();
	return(new Color(((in.getRed() * f2) + (bl.getRed() * f1)) / 255,
			 ((in.getGreen() * f2) + (bl.getGreen() * f1)) / 255,
			 ((in.getBlue() * f2) + (bl.getBlue() * f1)) / 255,
			 in.getAlpha()));
    }

    public static Color blendcol(Color x, Color y, double a) {
	int f1 = (int)(a * 255), f2 = 255 - f1;
	return(new Color(((x.getRed()   * f2) + (y.getRed()   * f1)) / 255,
			 ((x.getGreen() * f2) + (y.getGreen() * f1)) / 255,
			 ((x.getBlue()  * f2) + (y.getBlue()  * f1)) / 255,
			 ((x.getAlpha() * f2) + (y.getAlpha() * f1)) / 255));
    }

    public static Color colmul(Color a, Color b) {
	return(new Color((a.getRed()  * b.getRed() ) / 255, (a.getGreen() * b.getGreen()) / 255,
			 (a.getBlue() * b.getBlue()) / 255, (a.getAlpha() * b.getAlpha()) / 255));
    }

    public static Color preblend(Color c1, Color c2) {
	double a1 = c1.getAlpha() / 255.0;
	double a2 = c2.getAlpha() / 255.0;
	/* I can't help but feel that this should be possible to
	 * express in some simpler form, but I can't see how. */
	double ac = a1 + a2 - (a1 * a2);
	return(new Color((int)Math.round((((c2.getRed()   * a2) - (c1.getRed()   * a2)) / ac) + c1.getRed()),
			 (int)Math.round((((c2.getGreen() * a2) - (c1.getGreen() * a2)) / ac) + c1.getGreen()),
			 (int)Math.round((((c2.getBlue()  * a2) - (c1.getBlue()  * a2)) / ac) + c1.getBlue()),
			 (int)Math.round(ac * 255)));
    }

    public static void serialize(Object obj, OutputStream out) throws IOException {
	ObjectOutputStream oout = new ObjectOutputStream(out);
	oout.writeObject(obj);
	oout.flush();
    }

    public static byte[] serialize(Object obj) {
	ByteArrayOutputStream out = new ByteArrayOutputStream();
	try {
	    serialize(obj, out);
	} catch(IOException e) {
	    throw(new RuntimeException(e));
	}
	return(out.toByteArray());
    }

    public static Object deserialize(InputStream in) throws IOException {
	ObjectInputStream oin = new ObjectInputStream(in);
	try {
	    return(oin.readObject());
	} catch(ClassNotFoundException e) {
	    return(null);
	}
    }

    public static Object deserialize(byte[] buf) {
	if(buf == null)
	    return(null);
	InputStream in = new ByteArrayInputStream(buf);
	try {
	    return(deserialize(in));
	} catch(IOException e) {
	    return(null);
	}
    }

    public static boolean parsebool(String s) {
	if(s == null)
	    throw(new IllegalArgumentException(s));
	else if(s.equalsIgnoreCase("1") || s.equalsIgnoreCase("on") || s.equalsIgnoreCase("true") || s.equalsIgnoreCase("yes"))
	    return(true);
	else if(s.equalsIgnoreCase("0") || s.equalsIgnoreCase("off") || s.equalsIgnoreCase("false") || s.equalsIgnoreCase("no"))
	    return(false);
	throw(new IllegalArgumentException(s));
    }

    public static boolean eq(Object a, Object b) {
	return((a == b) || ((a != null) && a.equals(b)));
    }

    public static boolean parsebool(String s, boolean def) {
	try {
	    return(parsebool(s));
	} catch(IllegalArgumentException e) {
	    return(def);
	}
    }

    /* Just in case anyone doubted that Java is stupid. :-/ */
    public static FloatBuffer bufcp(float[] a) {
	FloatBuffer b = mkfbuf(a.length);
	b.put(a);
	b.rewind();
	return(b);
    }
    public static ShortBuffer bufcp(short[] a) {
	ShortBuffer b = mksbuf(a.length);
	b.put(a);
	b.rewind();
	return(b);
    }
    public static FloatBuffer bufcp(FloatBuffer a) {
	a.rewind();
	FloatBuffer ret = mkfbuf(a.remaining());
	ret.put(a).rewind();
	return(ret);
    }
    public static IntBuffer bufcp(IntBuffer a) {
	a.rewind();
	IntBuffer ret = mkibuf(a.remaining());
	ret.put(a).rewind();
	return(ret);
    }
    public static ByteBuffer mkbbuf(int n) {
	try {
	    return(ByteBuffer.allocateDirect(n).order(ByteOrder.nativeOrder()));
	} catch(OutOfMemoryError e) {
	    /* At least Sun's class library doesn't try to collect
	     * garbage if it's out of direct memory, which is pretty
	     * stupid. So do it for it, then. */
	    System.gc();
	    return(ByteBuffer.allocateDirect(n).order(ByteOrder.nativeOrder()));
	}
    }
    public static FloatBuffer mkfbuf(int n) {
	return(mkbbuf(n * 4).asFloatBuffer());
    }
    public static ShortBuffer mksbuf(int n) {
	return(mkbbuf(n * 2).asShortBuffer());
    }
    public static IntBuffer mkibuf(int n) {
	return(mkbbuf(n * 4).asIntBuffer());
    }

    /*
    public static ByteBuffer wbbuf(int n) {
	return(mkbbuf(n));
    }
    public static IntBuffer wibuf(int n) {
	return(mkibuf(n));
    }
    public static FloatBuffer wfbuf(int n) {
	return(mkfbuf(n));
    }
    public static ShortBuffer wsbuf(int n) {
	return(mksbuf(n));
    }
    */
    public static ByteBuffer wbbuf(int n) {
	return(ByteBuffer.wrap(new byte[n]).order(ByteOrder.nativeOrder()));
    }
    public static IntBuffer wibuf(int n) {
	return(IntBuffer.wrap(new int[n]));
    }
    public static FloatBuffer wfbuf(int n) {
	return(FloatBuffer.wrap(new float[n]));
    }
    public static ShortBuffer wsbuf(int n) {
	return(ShortBuffer.wrap(new short[n]));
    }
    public static FloatBuffer wbufcp(FloatBuffer a) {
	a.rewind();
	FloatBuffer ret = wfbuf(a.remaining());
	ret.put(a.slice()).rewind();
	return(ret);
    }
    public static IntBuffer wbufcp(IntBuffer a) {
	a.rewind();
	IntBuffer ret = wibuf(a.remaining());
	ret.put(a.slice()).rewind();
	return(ret);
    }

    public static ByteBuffer growbuf(ByteBuffer buf, int req) {
	if(buf.remaining() >= req)
	    return(buf);
	int sz = buf.capacity();
	while(sz - buf.position() < req)
	    sz <<= 1;
	return(ByteBuffer.allocate(sz).order(buf.order()).put((ByteBuffer)buf.flip()));
    }

    public static float[] c2fa(Color c) {
	return(new float[] {
		((float)c.getRed() / 255.0f),
		((float)c.getGreen() / 255.0f),
		((float)c.getBlue() / 255.0f),
		((float)c.getAlpha() / 255.0f)
	    });
    }

    @SuppressWarnings("unchecked")
    public static <T> T[] mkarray(Class<T> cl, int len) {
	return((T[])Array.newInstance(cl, len));
    }

    @SuppressWarnings("unchecked")
    public static <T> T[] splice(T[] src, int off, int len) {
	T[] dst = (T[])Array.newInstance(src.getClass().getComponentType(), len);
	System.arraycopy(src, off, dst, 0, len);
	return(dst);
    }
    public static <T> T[] splice(T[] src, int off) {
	return(splice(src, off, src.length - off));
    }

    public static byte[] splice(byte[] src, int off, int len) {
	byte[] dst = new byte[len];
	System.arraycopy(src, off, dst, 0, len);
	return(dst);
    }
    public static byte[] splice(byte[] src, int off) {
	return(splice(src, off, src.length - off));
    }

    public static float[] splice(float[] src, int off, int len) {
	float[] dst = new float[len];
	System.arraycopy(src, off, dst, 0, len);
	return(dst);
    }
    public static float[] splice(float[] src, int off) {
	return(splice(src, off, src.length - off));
    }

    public static double[] splice(double[] src, int off, int len) {
	double[] dst = new double[len];
	System.arraycopy(src, off, dst, 0, len);
	return(dst);
    }
    public static double[] splice(double[] src, int off) {
	return(splice(src, off, src.length - off));
    }

    public static int[] splice(int[] src, int off, int len) {
	int[] dst = new int[len];
	System.arraycopy(src, off, dst, 0, len);
	return(dst);
    }
    public static int[] splice(int[] src, int off) {
	return(splice(src, off, src.length - off));
    }

    @SuppressWarnings("unchecked")
    public static <T> T[] extend(T[] src, int off, int nl) {
	T[] dst = (T[])Array.newInstance(src.getClass().getComponentType(), nl);
	System.arraycopy(src, off, dst, 0, Math.min(src.length - off, dst.length));
	return(dst);
    }

    public static <T> T[] extend(T[] src, int nl) {
	return(extend(src, 0, nl));
    }

    public static <T, E extends T> T[] extend(T[] src, E ne) {
	T[] ret = extend(src, 0, src.length + 1);
	ret[src.length] = ne;
	return(ret);
    }

    public static <T, E extends T> T[] extend(T[] src, E[] ne) {
	T[] ret = extend(src, 0, src.length + ne.length);
	System.arraycopy(ne, 0, ret, src.length, ne.length);
	return(ret);
    }

    public static int[] extend(int[] src, int nl) {
	int[] dst = new int[nl];
	System.arraycopy(src, 0, dst, 0, Math.min(src.length, dst.length));
	return(dst);
    }

    public static double[] extend(double[] src, int nl) {
	double[] dst = new double[nl];
	System.arraycopy(src, 0, dst, 0, Math.min(src.length, dst.length));
	return(dst);
    }

    public static float[] extend(float[] src, int nl) {
	float[] dst = new float[nl];
	System.arraycopy(src, 0, dst, 0, Math.min(src.length, dst.length));
	return(dst);
    }

    public static short[] extend(short[] src, int nl) {
	short[] dst = new short[nl];
	System.arraycopy(src, 0, dst, 0, Math.min(src.length, dst.length));
	return(dst);
    }

    public static <T> T el(Iterable<T> c) {
	Iterator<T> i = c.iterator();
	if(!i.hasNext()) return(null);
	return(i.next());
    }

    public static <T> T take(Iterable<T> c) {
	Iterator<T> i = c.iterator();
	if(!i.hasNext()) return(null);
	T ret = i.next();
	i.remove();
	return(ret);
    }

    public static <T> List<T> reversed(List<T> ls) {
	return(new AbstractList<T>() {
		public int size() {
		    return(ls.size());
		}

		public T get(int i) {
		    return(ls.get(ls.size() - 1 - i));
		}

		public ListIterator<T> listIterator(int first) {
		    ListIterator<T> bk = ls.listIterator(ls.size() - first);
		    return(new ListIterator<T>() {
			    public boolean hasNext() {return(bk.hasPrevious());}
			    public boolean hasPrevious() {return(bk.hasNext());}
			    public T next() {return(bk.previous());}
			    public T previous() {return(bk.next());}
			    public int nextIndex() {return(ls.size() - bk.previousIndex() - 1);}
			    public int previousIndex() {return(ls.size() - bk.nextIndex() - 1);}

			    public void set(T el) {bk.set(el);}
			    public void remove() {bk.remove();}
			    public void add(T el) {bk.add(el);}
			});
		}

		public ListIterator<T> listIterator() {return(listIterator(0));}
		public Iterator<T> iterator() {return(listIterator());}

		public T set(int i, T el) {
		    return(ls.set(ls.size() - 1 - i, el));
		}

		public void add(int i, T el) {
		    ls.add(ls.size() - i, el);
		}

		public T remove(int i) {
		    return(ls.remove(ls.size() - 1 - i));
		}

		public String toString() {
		    return(String.format("#<reversed %s>", ls));
		}
	    });
    }

    public static <T> int index(T[] arr, T el) {
	for(int i = 0; i < arr.length; i++) {
	    if(Objects.equals(arr[i], el))
		return(i);
	}
	return(-1);
    }

    public static boolean strcheck(String str, IntPredicate p) {
	for(int i = 0; i < str.length(); i++) {
	    if(!p.test(str.charAt(i)))
		return(false);
	}
	return(true);
    }

    public static <T> T find(Iterable<? extends T> in, Predicate<? super T> p) {
	for(T obj : in) {
	    if(p.test(obj))
		return(obj);
	}
	return(null);
    }

    @SafeVarargs
    public static <T> T or(Supplier<T>... vals) {
	for(Supplier<T> val : vals) {
	    T ret = val.get();
	    if(ret != null)
		return(ret);
	}
	return(null);
    }

    public static <T> void clean(Collection<T> c, Consumer<? super T> clean) {
	for(T item : c)
	    clean.accept(item);
	c.clear();
    }

    public static <T> T construct(Constructor<T> cons, Object... args) {
	try {
	    return(cons.newInstance(args));
	} catch(InstantiationException e) {
	    throw(new RuntimeException(e));
	} catch(IllegalAccessException e) {
	    throw(new RuntimeException(e));
	} catch(InvocationTargetException e) {
	    if(e.getCause() instanceof RuntimeException)
		throw((RuntimeException)e.getCause());
	    throw(new RuntimeException(e.getCause()));
	}
    }

    public static <T> T construct(Class<T> cl) {
	try {
	    return(construct(cl.getConstructor()));
	} catch(NoSuchMethodException e) {
	    throw(new RuntimeException(e));
	}
    }

    public static Object invoke(Method mth, Object ob, Object... args) {
	try {
	    return(mth.invoke(ob, args));
	} catch(IllegalAccessException e) {
	    throw(new RuntimeException(e));
	} catch(InvocationTargetException e) {
	    if(e.getCause() instanceof RuntimeException)
		throw((RuntimeException)e.getCause());
	    throw(new RuntimeException(e.getCause()));
	}
    }

    public static <R> Function<Object[], R> consfun(Class<R> cl, Class<?>... args) throws NoSuchMethodException {
	Constructor<R> cons = cl.getConstructor(args);
	return(iargs -> construct(cons, iargs));
    }

    public static <R> Function<Object[], R> smthfun(Class<?> cl, String name, Class<R> rtype, Class<?>...args) throws NoSuchMethodException {
	Method mth = cl.getDeclaredMethod(name, args);
	if(!rtype.isAssignableFrom(mth.getReturnType()))
	    throw(new NoSuchMethodException("unexpected return type: " + mth.getReturnType()));
	int mod = mth.getModifiers();
	if(((mod & Modifier.STATIC) == 0) || ((mod & Modifier.PUBLIC) == 0))
	    throw(new NoSuchMethodException("expected public static method"));
	return(iargs -> rtype.cast(invoke(mth, null, iargs)));
    }

    public static String urlencode(String in) {
	StringBuilder buf = new StringBuilder();
	byte[] enc;
	try {
	    enc = in.getBytes("utf-8");
	} catch(java.io.UnsupportedEncodingException e) {
	    /* ¦] */
	    throw(new Error(e));
	}
	for(byte c : enc) {
	    if(((c >= 'a') && (c <= 'z')) || ((c >= 'A') && (c <= 'Z')) ||
	       ((c >= '0') && (c <= '9')) || (c == '.')) {
		buf.append((char)c);
	    } else {
		buf.append("%" + Utils.num2hex((c & 0xf0) >> 4) + Utils.num2hex(c & 0x0f));
	    }
	}
	return(buf.toString());
    }

    public static URI uriparam(URI base, String... pars) {
	StringBuilder buf = new StringBuilder();
	if(base.getQuery() != null)
	    buf.append(base.getQuery());
	for(int i = 0; i < pars.length; i += 2) {
	    if(buf.length() > 0)
		buf.append('&');
	    buf.append(urlencode(pars[i]));
	    buf.append('=');
	    buf.append(urlencode(pars[i + 1]));
	}
	try {
	    return(new URI(base.getScheme(), base.getAuthority(), base.getPath(), buf.toString(), base.getFragment()));
	} catch(URISyntaxException e) {
	    throw(new RuntimeException(e));
	}
    }

    public static <C> C hascause(Throwable t, Class<C> c) {
	while(t != null) {
	    if(c.isInstance(t))
		return(c.cast(t));
	    t = t.getCause();
	}
	return(null);
    }

    public static double ntime() {
	return(System.currentTimeMillis() / 1e3);
    }

    private static final long rtimeoff = System.nanoTime();
    public static double rtime() {
	return((System.nanoTime() - rtimeoff) / 1e9);
    }

    public static class MapBuilder<K, V> {
	private final Map<K, V> bk;

	public MapBuilder(Map<K, V> bk) {
	    this.bk = bk;
	}

	public MapBuilder<K, V> put(K k, V v) {
	    bk.put(k, v);
	    return(this);
	}

	public Map<K, V> map() {
	    return(Collections.unmodifiableMap(bk));
	}
    }

    public static class Range extends AbstractList<Integer> {
	public final int min, max, step;

	public Range(int min, int max, int step) {
	    if(step == 0)
		throw(new IllegalArgumentException());
	    this.min = min; this.max = max; this.step = step;
	}

	public int size() {
	    return(Math.max((max - min + step - 1) / step, 0));
	}

	public Integer get(int idx) {
	    int rv = min + (step * idx);
	    if((rv < min) || (rv >= max))
		throw(new NoSuchElementException());
	    return(rv);
	}
    }

    public static List<Integer> range(int min, int max, int step) {return(new Range(min, max, step));}
    public static List<Integer> range(int min, int max) {return(range(min, max, 1));}
    public static List<Integer> range(int max) {return(range(0, max));}

    public static <T> Indir<T> cache(Indir<T> src) {
	return(new Indir<T>() {
		private T val;
		private boolean has = false;

		public T get() {
		    if(!has) {
			val = src.get();
			has = true;
		    }
		    return(val);
		}
	    });
    }

    public static <V, R> Indir<R> transform(Supplier<? extends V> val, Function<? super V, ? extends R> xf) {
	return(new Indir<R>() {
		private V last;
		private R res;
		private boolean has = false;

		public R get() {
		    V v = val.get();
		    if(!has || !Utils.eq(last, v)) {
			res = xf.apply(v);
			last = v;
		    }
		    return(res);
		}
	    });
    }

    public static <K, V> MapBuilder<K, V> map() {
	return(new MapBuilder<K, V>(new HashMap<K, V>()));
    }

    public static <F, T> Iterator<T> map(Iterator<F> from, Function<? super F, ? extends T> fn) {
	return(new Iterator<T>() {
		boolean h = false;
		T n;

		public boolean hasNext() {
		    if(h)
			return(true);
		    if(!from.hasNext())
			return(false);
		    n = fn.apply(from.next());
		    h = true;
		    return(true);
		}

		public T next() {
		    if(!hasNext())
			throw(new NoSuchElementException());
		    T ret = n;
		    h = false;
		    n = null;
		    return(ret);
		}

		public void remove() {
		    from.remove();
		}
	    });
    }

    public static <E> Iterator<E> filter(Iterator<E> from, Predicate<? super E> filter) {
	return(new Iterator<E>() {
		boolean h = false;
		E n;

		public boolean hasNext() {
		    while(!h) {
			if(!from.hasNext())
			    return(false);
			E g = from.next();
			if(filter.test(g)) {
			    n = g;
			    h = true;
			    break;
			}
		    }
		    return(true);
		}

		public E next() {
		    if(!hasNext())
			throw(new NoSuchElementException());
		    E ret = n;
		    h = false;
		    n = null;
		    return(ret);
		}

		public void remove() {
		    from.remove();
		}
	    });
    }

    public static void checkirq() throws InterruptedException {
	if(Thread.interrupted())
	    throw(new InterruptedException());
    }

    public static <T, F> Iterator<T> filter(Iterator<F> from, Class<T> filter) {
	return(map(filter(from, filter::isInstance), filter::cast));
    }

    public static <E, T extends Collection<E>> T merge(T dst, Iterable<? extends E> a, Iterable<? extends E> b, Comparator<? super E> cmp) {
	Iterator<? extends E> i = a.iterator(), o = b.iterator();
	if(i.hasNext() && o.hasNext()) {
	    E e = i.next(), f = o.next();
	    while(true) {
		if(cmp.compare(e, f) <= 0) {
		    dst.add(e);
		    if(i.hasNext()) {
			e = i.next();
		    } else {
			dst.add(f);
			break;
		    }
		} else {
		    dst.add(f);
		    if(o.hasNext()) {
			f = o.next();
		    } else {
			dst.add(e);
			break;
		    }
		}
	    }
	}
	while(i.hasNext())
	    dst.add(i.next());
	while(o.hasNext())
	    dst.add(o.next());
	return(dst);
    }

    public static <T> Optional<T> ocast(Object x, Class<T> cl) {
	if(cl.isInstance(x))
	    return(Optional.of(cl.cast(x)));
	return(Optional.empty());
    }

    public static int sidcmp(Object a, Object b) {
	int ah = System.identityHashCode(a);
	int bh = System.identityHashCode(b);
	if(ah < bh)
	    return(-1);
	else if(ah > bh)
	    return(1);
	return(0);
    }

    public static final Object formatter(String fmt, Object... args) {
	return(new Object() {
		public String toString() {
		    return(String.format(fmt, args));
		}
	    });
    }

    public static class AddressFormatException extends IllegalArgumentException {
	public final String addr, type;

	public AddressFormatException(String message, CharSequence addr, String type) {
	    super(message);
	    this.addr = addr.toString();
	    this.type = type;
	}

	public AddressFormatException(String message, CharSequence addr, String type, Throwable cause) {
	    super(message, cause);
	    this.addr = addr.toString();
	    this.type = type;
	}

	public String getMessage() {
	    return(super.getMessage() + ": " + addr + " (" + type + ")");
	}
    }

    public static Inet4Address in4_pton(CharSequence as) {
	int dbuf = -1, o = 0;
	byte[] abuf = new byte[4];
	for(int i = 0; i < as.length(); i++) {
	    char c = as.charAt(i);
	    if((c >= '0') && (c <= '9')) {
		dbuf = (((dbuf < 0) ? 0 : dbuf) * 10) + (c - '0');
		if(dbuf >= 256)
		    throw(new AddressFormatException("illegal octet", as, "in4"));
	    } else if(c == '.') {
		if(dbuf < 0)
		    throw(new AddressFormatException("dot without preceding octet", as, "in4"));
		if(o >= 3)
		    throw(new AddressFormatException("too many address octets", as, "in4"));
		abuf[o++] = (byte)dbuf;
		dbuf = -1;
	    } else {
		throw(new AddressFormatException("illegal address character", as, "in4"));
	    }
	}
	if(dbuf < 0)
	    throw(new AddressFormatException("end without preceding octet", as, "in4"));
	if(o != 3)
	    throw(new AddressFormatException("too few address octets", as, "in4"));
	abuf[o++] = (byte)dbuf;
	try {
	    return((Inet4Address)InetAddress.getByAddress(abuf));
	} catch(UnknownHostException e) {
	    throw(new RuntimeException(e));
	}
    }

    public static InetAddress in6_pton(CharSequence as) {
	int hbuf = -1, dbuf = -1, p = 0, v4map = -1;
	int[] o = {0, 0};
	byte[][] abuf = {new byte[16], new byte[16]};
	String scope = null;
	for(int i = 0; i < as.length(); i++) {
	    char c = as.charAt(i);
	    int dv = -1;
	    if((c >= '0') && (c <= '9'))
		dv = c - '0';
	    else if((c >= 'A') && (c <= 'F'))
		dv = c + 10 - 'A';
	    else if((c >= 'a') && (c <= 'f'))
		dv = c + 10 - 'a';
	    if(dv >= 0) {
		if(hbuf < 0)
		    hbuf = dbuf = 0;
		hbuf = (hbuf * 16) + dv;
		if(hbuf >= 65536)
		    throw(new AddressFormatException("illegal address number", as, "in6"));
		if(dbuf >= 0)
		    dbuf = (dv >= 10) ? -1 : ((dbuf * 10) + dv);
		if(dbuf >= 256)
		    dbuf = -1;
	    } else if(c == ':') {
		if(v4map >= 0)
		    throw(new AddressFormatException("illegal embedded v4 address", as, "in6"));
		if(hbuf < 0) {
		    if(p == 0) {
			if(o[p] == 0) {
			    if((i < as.length() - 1) && (as.charAt(i + 1) == ':')) {
				p = 1;
				i++;
			    } else {
				throw(new AddressFormatException("colon without preceeding address number", as, "in6"));
			    }
			} else {
			    p = 1;
			}
		    } else {
			throw(new AddressFormatException("duplicate zero-string", as, "in6"));
		    }
		} else {
		    if(o[p] >= 14)
			throw(new AddressFormatException("too many address numbers", as, "in6"));
		    abuf[p][o[p]++] = (byte)((hbuf & 0xff00) >> 8);
		    abuf[p][o[p]++] = (byte) (hbuf & 0x00ff);
		    hbuf = -1;
		}
	    } else if(c == '.') {
		if((hbuf < 0) || (dbuf < 0))
		    throw(new AddressFormatException("illegal embedded v4 octet", as, "in6"));
		if((p == 0) && (o[p] == 0))
		    throw(new AddressFormatException("embedded v4 at start of address", as, "in6"));
		if(v4map++ >= 2)
		    throw(new AddressFormatException("too many embedded v4 octets", as, "in6"));
		if(o[p] >= 15)
		    throw(new AddressFormatException("too many address numbers", as, "in6"));
		abuf[p][o[p]++] = (byte)dbuf;
		hbuf = -1;
	    } else if(c == '%') {
		scope = as.subSequence(i + 1, as.length()).toString();
		break;
	    } else {
		throw(new AddressFormatException("illegal address character", as, "in6"));
	    }
	}
	if(hbuf < 0) {
	    if((p < 1) || (o[p] > 0))
		throw(new AddressFormatException("unterminated address", as, "in6"));
	} else {
	    if(v4map < 0) {
		if(o[p] >= 15)
		    throw(new AddressFormatException("too many address numbers", as, "in6"));
		abuf[p][o[p]++] = (byte)((hbuf & 0xff00) >> 8);
		abuf[p][o[p]++] = (byte) (hbuf & 0x00ff);
	    } else {
		if(dbuf < 0)
		    throw(new AddressFormatException("illegal embedded v4 octet", as, "in6"));
		if(v4map != 2)
		    throw(new AddressFormatException("too few embedded v4 octets", as, "in6"));
		if(o[p] >= 16)
		    throw(new AddressFormatException("too many address numbers", as, "in6"));
		abuf[p][o[p]++] = (byte)dbuf;
	    }
	}
	byte[] fbuf;
	if(p == 0) {
	    if(o[0] != 16)
		throw(new AddressFormatException("too few address numbers", as, "in6"));
	    fbuf = abuf[0];
	} else {
	    if((o[0] + o[1]) >= 16)
		throw(new AddressFormatException("illegal zero-string", as, "in6"));
	    fbuf = new byte[16];
	    System.arraycopy(abuf[0], 0, fbuf, 0, o[0]);
	    System.arraycopy(abuf[1], 0, fbuf, 16 - o[1], o[1]);
	}
	try {
	    if(scope == null)
		return(InetAddress.getByAddress(fbuf));
	    try {
		return(Inet6Address.getByAddress(null, fbuf, Integer.parseInt(scope)));
	    } catch(NumberFormatException e) {
		try {
		    NetworkInterface iface = NetworkInterface.getByName(scope);
		    if(iface == null)
			throw(new AddressFormatException("could not resolve scoped interface: " + scope, as, "in6"));
		    return(Inet6Address.getByAddress(null, fbuf, iface));
		} catch(SocketException e2) {
		    throw(new AddressFormatException("could not resolve scoped interface: " + scope, as, "in6", e));
		}
	    }
	} catch(UnknownHostException e) {
	    throw(new RuntimeException(e));
	}
    }

    public static InetAddress inet_pton(CharSequence as) {
	try {
	    return(in4_pton(as));
	} catch(IllegalArgumentException e) {
	    try {
		return(in6_pton(as));
	    } catch(IllegalArgumentException e2) {
		e2.addSuppressed(e);
		throw(e2);
	    }
	}
    }

    @SuppressWarnings("unchecked")
    public static int compare(Object[] a, Object[] b) {
	int i = 0;
	for(i = 0; (i < a.length) && (i < b.length); i++) {
	    if((a[i] == null) && (b[i] == null)) {
	    } else if(a[i] == null) {
		return(-1);
	    } else if(b[i] == null) {
		return(1);
	    } else {
		if(a[i].getClass() != b[i].getClass()) {
		    return(a[i].getClass().getName().compareTo(b[i].getClass().getName()));
		} else if(Comparable.class.isAssignableFrom(a[i].getClass())) {
		    return(((Comparable)a[i]).compareTo(b[i]));
		} else {
		    if(a[i] != b[i])
			return(sidcmp(a[i], b[i]));
		}
	    }
	}
	if(a.length < b.length)
	    return(-1);
	if(a.length > b.length)
	    return(1);
	return(0);
    }

    public static final Comparator<Object> idcmp = new Comparator<Object>() {
	int eid = 0;
	final Map<Ref, Long> emerg = new HashMap<Ref, Long>();
	final ReferenceQueue<Object> cleanq = new ReferenceQueue<Object>();

	class Ref extends WeakReference<Object> {
	    final int h;

	    Ref(Object o, ReferenceQueue<Object> queue) {
		super(o, queue);
		this.h = System.identityHashCode(o);
	    }

	    public int hashCode() {
		return(h);
	    }

	    public boolean equals(Object o) {
		if(o == this)
		    return(true);
		if(!(o instanceof Ref))
		    return(false);
		Object or = ((Ref)o).get();
		Object sr = get();
		return((or != null) && (sr != null) && (or == sr));
	    }
	}

	private void clean() {
	    Reference<? extends Object> ref;
	    while((ref = cleanq.poll()) != null)
		emerg.remove(ref);
	}

	public int compare(Object a, Object b) {
	    if(a == b)
		return(0);
	    if(a == null)
		return(1);
	    if(b == null)
		return(-1);
	    int ah = System.identityHashCode(a);
	    int bh = System.identityHashCode(b);
	    if(ah < bh)
		return(-1);
	    else if(ah > bh)
		return(1);

	    synchronized(emerg) {
		if(eid == 0)
		    Warning.warn("could not impose ordering in idcmp, using slow-path");
		clean();
		Ref ar = new Ref(a, cleanq), br = new Ref(b, cleanq);
		Long ai, bi;
		if((ai = emerg.get(ar)) == null)
		    emerg.put(ar, ai = ((long)ah << 32) | (((long)eid++) & 0xffffffffl));
		if((bi = emerg.get(br)) == null)
		    emerg.put(br, bi = ((long)ah << 32) | (((long)eid++) & 0xffffffffl));
		if(ai < bi)
		    return(-1);
		else if(ai > bi)
		    return(1);
		throw(new RuntimeException("Comparison identity crisis"));
	    }
	}
    };

    static {
	Console.setscmd("die", new Console.Command() {
		public void run(Console cons, String[] args) {
		    throw(new Error("Triggered death"));
		}
	    });
	Console.setscmd("sleep", new Console.Command() {
		public void run(Console cons, String[] args) {
		    long ms = (long)(Double.parseDouble(args[1]) * 1000);
		    try {
			Thread.sleep(ms);
		    } catch(InterruptedException e) {
			Thread.currentThread().interrupt();
			throw(new RuntimeException(e));
		    }
		}
	    });
	Console.setscmd("lockdie", new Console.Command() {
		public void run(Console cons, String[] args) {
		    Object m1 = new Object(), m2 = new Object();
		    int[] sync = {0};
		    new HackThread(() -> {
			    try {
				synchronized(m2) {
				    synchronized(sync) {
					while(sync[0] != 1)
					    sync.wait();
					sync[0] = 2;
					sync.notifyAll();
				    }
				    synchronized(m1) {
					synchronized(sync) {
					    sync[0] = 3;
					    sync.notifyAll();
					}
				    }
				}
			    } catch(InterruptedException e) {}
		    }, "Deadlocker").start();
		    try {
			synchronized(m1) {
			    synchronized(sync) {
				sync[0] = 1;
				sync.notifyAll();
				while(sync[0] != 2)
				    sync.wait();
			    }
			    synchronized(m2) {
				synchronized(sync) {
				    sync[0] = 3;
				    sync.notifyAll();
				}
			    }
			}
		    } catch(InterruptedException e) {}
		}
	    });
	Console.setscmd("threads", new Console.Command() {
		public void run(Console cons, String[] args) {
		    Utils.dumptg(null, cons.out);
		}
	    });
	Console.setscmd("gc", new Console.Command() {
		public void run(Console cons, String[] args) {
		    System.gc();
		}
	    });
    }
}
