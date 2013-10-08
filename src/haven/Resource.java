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

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.annotation.*;
import java.util.*;
import java.net.*;
import java.io.*;
import javax.imageio.*;
import java.awt.Graphics;
import java.awt.image.BufferedImage;

public class Resource implements Comparable<Resource>, Prioritized, Serializable {
    private final static Map<String, Resource> cache;
    private static Loader loader;
    private static CacheSource prscache;
    public static ThreadGroup loadergroup = null;
    private static Map<String, LayerFactory<?>> ltypes = new TreeMap<String, LayerFactory<?>>();
    static Set<Resource> loadwaited = new HashSet<Resource>();
    public static Class<Image> imgc = Image.class;
    public static Class<Tile> tile = Tile.class;
    public static Class<Neg> negc = Neg.class;
    public static Class<Anim> animc = Anim.class;
    public static Class<Tileset> tileset = Tileset.class;
    public static Class<Pagina> pagina = Pagina.class;
    public static Class<AButton> action = AButton.class;
    public static Class<Audio> audio = Audio.class;
    public static Class<Tooltip> tooltip = Tooltip.class;
    
    static {
	if(Config.softres)
	    cache = new CacheMap<String, Resource>();
	else
	    cache = new TreeMap<String, Resource>();
    }

    static {
	if(!Config.nolocalres)
	    loader = new Loader(new JarSource());
	try {
	    String dir = Config.resdir;
	    if(dir == null)
		dir = System.getenv("HAVEN_RESDIR");
	    if(dir != null)
		chainloader(new Loader(new FileSource(new File(dir))));
	} catch(Exception e) {
	    /* Ignore these. We don't want to be crashing the client
	     * for users just because of errors in development
	     * aids. */
	}
    }
	
    private LoadException error;
    private Collection<Layer> layers = new LinkedList<Layer>();
    public final String name;
    public int ver;
    public boolean loading;
    public ResSource source;
    private transient Indir<Resource> indir = null;
    int prio = 0;

    public static class Spec implements Indir<Resource> {
	public final String name;
	public final int ver;

	public Spec(String name, int ver) {
	    this.name = name;
	    this.ver = ver;
	}

	public Resource get(int prio) {
	    return(load(name, ver));
	}
	
	public Resource get() {
	    return(get(0));
	}
    }

    private Resource(String name, int ver) {
	this.name = name;
	this.ver = ver;
	error = null;
	loading = true;
    }
	
    public static void addcache(ResCache cache) {
	CacheSource src = new CacheSource(cache);
	prscache = src;
	chainloader(new Loader(src));
    }

    public static void addurl(URL url) {
	ResSource src = new HttpSource(url);
	final CacheSource mc = prscache;
	if(mc != null) {
	    src = new TeeSource(src) {
		    public OutputStream fork(String name) throws IOException {
			return(mc.cache.store("res/" + name));
		    }
		};
	}
	chainloader(new Loader(src));
    }
    
    private static void chainloader(Loader nl) {
	synchronized(Resource.class) {
	    if(loader == null) {
		loader = nl;
	    } else {
		Loader l;
		for(l = loader; l.next != null; l = l.next);
		l.chain(nl);
	    }
	}
    }
    
    public static Resource load(String name, int ver, int prio) {
	Resource res;
	synchronized(cache) {
	    res = cache.get(name);
	    if(res != null) {
		if((res.ver != -1) && (ver != -1)) {
		    if(res.ver < ver) {
			res = null;
			cache.remove(name);
		    } else if(res.ver > ver) {
			/* Throw LoadException rather than
			 * RuntimeException here, to make sure
			 * obsolete resources doing nested loading get
			 * properly handled. This could be the wrong
			 * way of going about it, however; I'm not
			 * sure. */
			throw(new LoadException(String.format("Weird version number on %s (%d > %d), loaded from %s", res.name, res.ver, ver, res.source), res));
		    }
		} else if(ver == -1) {
		    if(res.error != null) {
			res = null;
			cache.remove(name);
		    }
		}
	    }
	    if(res != null) {
		res.boostprio(prio);
		return(res);
	    }
	    res = new Resource(name, ver);
	    res.prio = prio;
	    cache.put(name, res);
	}
	loader.load(res);
	return(res);
    }
    
    public static int numloaded() {
	synchronized(cache) {
	    return(cache.size());
	}
    }
    
    public static Collection<Resource> cached() {
	synchronized(cache) {
	    return(cache.values());
	}
    }
	
    public static Resource load(String name, int ver) {
	return(load(name, ver, 0));
    }

    public static int qdepth() {
	int ret = 0;
	for(Loader l = loader; l != null; l = l.next)
	    ret += l.queue.size();
	return(ret);
    }
    
    public static Resource load(String name) {
	return(load(name, -1));
    }
    
    public void boostprio(int newprio) {
	if(prio < newprio)
	    prio = newprio;
    }
    
    public Resource loadwaitint() throws InterruptedException {
	synchronized(this) {
	    boostprio(10);
	    while(loading) {
		wait();
	    }
	}
	return(this);
    }
	
    public String basename() {
	int p = name.lastIndexOf('/');
	if(p < 0)
	    return(name);
	return(name.substring(p + 1));
    }

    public Resource loadwait() {
	boolean i = false;
	synchronized(loadwaited) {
	    loadwaited.add(this);
	}
	synchronized(this) {
	    boostprio(10);
	    while(loading) {
		try {
		    wait();
		} catch(InterruptedException e) {
		    i = true;
		}
	    }
	}
	if(i)
	    Thread.currentThread().interrupt();
	return(this);
    }
	
    public static interface ResSource {
	public InputStream get(String name) throws IOException;
    }
    
    public static abstract class TeeSource implements ResSource, Serializable {
	public ResSource back;
	
	public TeeSource(ResSource back) {
	    this.back = back;
	}
	
	public InputStream get(String name) throws IOException {
	    StreamTee tee = new StreamTee(back.get(name));
	    tee.setncwe();
	    tee.attach(fork(name));
	    return(tee);
	}
	
	public abstract OutputStream fork(String name) throws IOException;
	
	public String toString() {
	    return("forking source backed by " + back);
	}
    }
    
    public static class CacheSource implements ResSource, Serializable {
	public transient ResCache cache;
	
	public CacheSource(ResCache cache) {
	    this.cache = cache;
	}
	
	public InputStream get(String name) throws IOException {
	    return(cache.fetch("res/" + name));
	}
	
	public String toString() {
	    return("cache source backed by " + cache);
	}
    }

    public static class FileSource implements ResSource, Serializable {
	File base;
	
	public FileSource(File base) {
	    this.base = base;
	}
	
	public InputStream get(String name) {
	    File cur = base;
	    String[] parts = name.split("/");
	    for(int i = 0; i < parts.length - 1; i++)
		cur = new File(cur, parts[i]);
	    cur = new File(cur, parts[parts.length - 1] + ".res");
	    try {
		return(new FileInputStream(cur));
	    } catch(FileNotFoundException e) {
		throw((LoadException)(new LoadException("Could not find resource in filesystem: " + name, this).initCause(e)));
	    }
	}
	
	public String toString() {
	    return("filesystem res source (" + base + ")");
	}
    }

    public static class JarSource implements ResSource, Serializable {
	public InputStream get(String name) {
	    InputStream s = Resource.class.getResourceAsStream("/res/" + name + ".res");
	    if(s == null)
		throw(new LoadException("Could not find resource locally: " + name, JarSource.this));
	    return(s);
	}
	
	public String toString() {
	    return("local res source");
	}
    }
    
    public static class HttpSource implements ResSource, Serializable {
	private final transient SslHelper ssl;
	public URL baseurl;
	
	{
	    ssl = new SslHelper();
	    try {
		ssl.trust(ssl.loadX509(Resource.class.getResourceAsStream("ressrv.crt")));
	    } catch(java.security.cert.CertificateException e) {
		throw(new Error("Invalid built-in certificate", e));
	    } catch(IOException e) {
		throw(new Error(e));
	    }
	    ssl.ignoreName();
	}
	
	public HttpSource(URL baseurl) {
	    this.baseurl = baseurl;
	}
		
	private URL encodeurl(URL raw) throws IOException {
	    /* This is "kinda" ugly. It is, actually, how the Java
	     * documentation recommend that it be done, though... */
	    try {
		return(new URL(new URI(raw.getProtocol(), raw.getHost(), raw.getPath(), raw.getRef()).toASCIIString()));
	    } catch(URISyntaxException e) {
		throw(new IOException(e));
	    }
	}

	public InputStream get(String name) throws IOException {
	    URL resurl = encodeurl(new URL(baseurl, name + ".res"));
	    URLConnection c;
	    if(resurl.getProtocol().equals("https"))
		c = ssl.connect(resurl);
	    else
		c = resurl.openConnection();
	    c.addRequestProperty("User-Agent", "Haven/1.0");
	    return(c.getInputStream());
	}

	public String toString() {
	    return("HTTP res source (" + baseurl + ")");
	}
    }

    private static class Loader implements Runnable {
	private ResSource src;
	private Loader next = null;
	private Queue<Resource> queue = new PrioQueue<Resource>();
	private transient Thread th = null;
	
	public Loader(ResSource src) {
	    this.src = src;
	}
	
	public void chain(Loader next) {
	    this.next = next;
	}
	
	public void load(Resource res) {
	    synchronized(queue) {
		queue.add(res);
		queue.notifyAll();
	    }
	    synchronized(Loader.this) {
		if(th == null) {
		    th = new HackThread(loadergroup, Loader.this, "Haven resource loader");
		    th.setDaemon(true);
		    th.start();
		}
	    }
	}
		
	public void run() {
	    try {
		while(true) {
		    Resource cur;
		    synchronized(queue) {
			while((cur = queue.poll()) == null)
			    queue.wait();
		    }
		    synchronized(cur) {
			handle(cur);
		    }
		    cur = null;
		}
	    } catch(InterruptedException e) {
	    } finally {
		synchronized(Loader.this) {
		    /* Yes, I know there's a race condition. */
		    th = null;
		}
	    }
	}
		
	private void handle(Resource res) {
	    InputStream in = null;
	    try {
		res.error = null;
		res.source = src;
		try {
		    try {
			in = src.get(res.name);
			res.load(in);
			res.loading = false;
			res.notifyAll();
			return;
		    } catch(IOException e) {
			throw(new LoadException(e, res));
		    }
		} catch(LoadException e) {
		    if(next == null) {
			res.error = e;
			res.loading = false;
			res.notifyAll();
		    } else {
			next.load(res);
		    }
		} catch(RuntimeException e) {
		    throw(new LoadException(e, res));
		}
	    } finally {
		try {
		    if(in != null)
			in.close();
		} catch(IOException e) {}
	    }
	}
    }
	
    public static class LoadException extends RuntimeException {
	public Resource res;
	public ResSource src;
	    
	public LoadException(String msg, ResSource src) {
	    super(msg);
	    this.src = src;
	}
	    
	public LoadException(String msg, Resource res) {
	    super(msg);
	    this.res = res;
	}

	public LoadException(String msg, Throwable cause, Resource res) {
	    super(msg, cause);
	    this.res = res;
	}
	    
	public LoadException(Throwable cause, Resource res) {
	    super("Load error in resource " + res.toString() + ", from " + res.source, cause);
	    this.res = res;
	}
    }
    
    public static class Loading extends haven.Loading {
	public final Resource res;
	
	public Loading(Resource res) {
	    this.res = res;
	}

	public String toString() {
	    return("#<Resource " + res.name + ">");
	}

	public boolean canwait() {return(true);}
	public void waitfor() throws InterruptedException {
	    res.loadwaitint();
	}
    }
	
    public static Coord cdec(byte[] buf, int off) {
	return(new Coord(Utils.int16d(buf, off), Utils.int16d(buf, off + 2)));
    }
	
    public abstract class Layer implements Serializable {
	public abstract void init();
	
	public Resource getres() {
	    return(Resource.this);
	}
    }

    public interface LayerFactory<T extends Layer> {
	public T cons(Resource res, byte[] buf);
    }

    public static class LayerConstructor<T extends Layer> implements LayerFactory<T> {
	public final Class<T> cl;
	private final Constructor<T> cons;
	
	public LayerConstructor(Class<T> cl) {
	    this.cl = cl;
	    try {
		this.cons = cl.getConstructor(Resource.class, byte[].class);
	    } catch(NoSuchMethodException e) {
		throw(new RuntimeException("No proper constructor found for layer type " + cl.getName(), e));
	    }
	}
	
	public T cons(Resource res, byte[] buf) {
	    try {
		return(cons.newInstance(res, buf));
	    } catch(InstantiationException e) {
		throw(new LoadException(e, res));
	    } catch(IllegalAccessException e) {
		throw(new LoadException(e, res));
	    } catch(InvocationTargetException e) {
		Throwable c = e.getCause();
		if(c instanceof RuntimeException)
		    throw((RuntimeException)c);
		else
		    throw(new LoadException(e, res));
	    }
	}
    }

    public static void addltype(String name, LayerFactory<?> cons) {
	ltypes.put(name, cons);
    }
    
    public static <T extends Layer> void addltype(String name, Class<T> cl) {
	addltype(name, new LayerConstructor<T>(cl));
    }
    
    @dolda.jglob.Discoverable
    @Target(ElementType.TYPE)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface LayerName {
	public String value();
    }

    static {
	for(Class<?> cl : dolda.jglob.Loader.get(LayerName.class).classes()) {
	    String nm = cl.getAnnotation(LayerName.class).value();
	    if(LayerFactory.class.isAssignableFrom(cl)) {
		try {
		    addltype(nm, cl.asSubclass(LayerFactory.class).newInstance());
		} catch(InstantiationException e) {
		    throw(new Error(e));
		} catch(IllegalAccessException e) {
		    throw(new Error(e));
		}
	    } else if(Layer.class.isAssignableFrom(cl)) {
		addltype(nm, cl.asSubclass(Layer.class));
	    } else {
		throw(new Error("Illegal resource layer class: " + cl));
	    }
	}
    }

    public interface IDLayer<T> {
	public T layerid();
    }

    @LayerName("image")
    public class Image extends Layer implements Comparable<Image>, IDLayer<Integer> {
	public transient BufferedImage img;
	transient private Tex tex;
	public final int z, subz;
	public final boolean nooff;
	public final int id;
	private int gay = -1;
	public Coord sz;
	public Coord o;
		
	public Image(byte[] buf) {
	    z = Utils.int16d(buf, 0);
	    subz = Utils.int16d(buf, 2);
	    /* Obsolete flag 1: Layered */
	    nooff = (buf[4] & 2) != 0;
	    id = Utils.int16d(buf, 5);
	    o = cdec(buf, 7);
	    try {
		img = ImageIO.read(new ByteArrayInputStream(buf, 11, buf.length - 11));
	    } catch(IOException e) {
		throw(new LoadException(e, Resource.this));
	    }
	    if(img == null)
		throw(new LoadException("Invalid image data in " + name, Resource.this));
	    sz = Utils.imgsz(img);
	}
		
	public synchronized Tex tex() {
	    if(tex != null)
		return(tex);
	    tex = new TexI(img) {
		    public String toString() {
			return("TexI(" + Resource.this.name + ", " + id + ")");
		    }
		};
	    return(tex);
	}
		
	private boolean detectgay() {
	    for(int y = 0; y < sz.y; y++) {
		for(int x = 0; x < sz.x; x++) {
		    if((img.getRGB(x, y) & 0x00ffffff) == 0x00ff0080)
			return(true);
		}
	    }
	    return(false);
	}
		
	public boolean gayp() {
	    if(gay == -1)
		gay = detectgay()?1:0;
	    return(gay == 1);
	}

	public int compareTo(Image other) {
	    return(z - other.z);
	}
	
	public Integer layerid() {
	    return(id);
	}
		
	public void init() {}
    }

    @LayerName("tooltip")
    public class Tooltip extends Layer {
	public final String t;
                
	public Tooltip(byte[] buf) {
	    try {
		t = new String(buf, "UTF-8");
	    } catch(UnsupportedEncodingException e) {
		throw(new LoadException(e, Resource.this));
	    }
	}
                
	public void init() {}
    }

    @LayerName("tile")
    public class Tile extends Layer {
	transient BufferedImage img;
	transient private Tex tex;
	public final int id;
	public final int w;
	public final char t;
		
	public Tile(byte[] buf) {
	    t = (char)Utils.ub(buf[0]);
	    id = Utils.ub(buf[1]);
	    w = Utils.uint16d(buf, 2);
	    try {
		img = ImageIO.read(new ByteArrayInputStream(buf, 4, buf.length - 4));
	    } catch(IOException e) {
		throw(new LoadException(e, Resource.this));
	    }
	    if(img == null)
		throw(new LoadException("Invalid image data in " + name, Resource.this));
	}

	public synchronized Tex tex() {
	    if(tex == null)
		tex = new TexI(img);
	    return(tex);
	}
		
	public void init() {}
    }

    @LayerName("neg")
    public class Neg extends Layer {
	public Coord cc;
	public Coord[][] ep;
		
	public Neg(byte[] buf) {
	    int off;
			
	    cc = cdec(buf, 0);
	    ep = new Coord[8][0];
	    int en = buf[16];
	    off = 17;
	    for(int i = 0; i < en; i++) {
		int epid = buf[off];
		int cn = Utils.uint16d(buf, off + 1);
		off += 3;
		ep[epid] = new Coord[cn];
		for(int o = 0; o < cn; o++) {
		    ep[epid][o] = cdec(buf, off);
		    off += 4;
		}
	    }
	}
		
	public void init() {}
    }

    @LayerName("anim")
    public class Anim extends Layer {
	private int[] ids;
	public int id, d;
	public Image[][] f;
		
	public Anim(byte[] buf) {
	    id = Utils.int16d(buf, 0);
	    d = Utils.uint16d(buf, 2);
	    ids = new int[Utils.uint16d(buf, 4)];
	    if(buf.length - 6 != ids.length * 2)
		throw(new LoadException("Invalid anim descriptor in " + name, Resource.this));
	    for(int i = 0; i < ids.length; i++)
		ids[i] = Utils.int16d(buf, 6 + (i * 2));
	}
		
	public void init() {
	    f = new Image[ids.length][];
	    Image[] typeinfo = new Image[0];
	    for(int i = 0; i < ids.length; i++) {
		LinkedList<Image> buf = new LinkedList<Image>();
		for(Image img : layers(Image.class, false)) {
		    if(img.id == ids[i])
			buf.add(img);
		}
		f[i] = buf.toArray(typeinfo);
	    }
	}
    }

    @LayerName("tileset2")
    public class Tileset extends Layer {
	private String tn = "gnd";
	private Object[] ta = new Object[0];
	private transient Tiler.Factory tfac;
	public WeightList<Resource> flavobjs = new WeightList<Resource>();
	public WeightList<Tile> ground;
	public WeightList<Tile>[] ctrans, btrans;
	public int flavprob;

	private Tileset() {
	}

	public Tileset(byte[] bbuf) {
	    Message buf = new Message(0, bbuf);
	    while(!buf.eom()) {
		int p = buf.uint8();
		switch(p) {
		case 0:
		    tn = buf.string();
		    ta = buf.list();
		    break;
		case 1:
		    int flnum = buf.uint16();
		    flavprob = buf.uint16();
		    for(int i = 0; i < flnum; i++) {
			String fln = buf.string();
			int flv = buf.uint16();
			int flw = buf.uint8();
			try {
			    flavobjs.add(load(fln, flv), flw);
			} catch(RuntimeException e) {
			    throw(new LoadException("Illegal resource dependency", e, Resource.this));
			}
		    }
		    break;
		default:
		    throw(new LoadException("Invalid tileset part " + p + "  in " + name, Resource.this));
		}
	    }
	}

	public Tiler.Factory tfac() {
	    synchronized(this) {
		if(tfac == null) {
		    CodeEntry ent = layer(CodeEntry.class);
		    if(ent != null)
			return(ent.get(Tiler.Factory.class));
		    return(Tiler.byname(tn));
		}
		return(tfac);
	    }
	}

	private void packtiles(Collection<Tile> tiles, Coord tsz) {
	    if(tiles.size() < 1)
		return;
	    int min = -1, minw = -1, minh = -1, mine = -1;
	    final int nt = tiles.size();
	    for(int i = 1; i <= nt; i++) {
		int w = Tex.nextp2(tsz.x * i);
		int h;
		if((nt % i) == 0)
		    h = nt / i;
		else
		    h = (nt / i) + 1;
		h = Tex.nextp2(tsz.y * h);
		int a = w * h;
		int e = (w < h)?h:w;
		if((min == -1) || (a < min) || ((a == min) && (e < mine))) {
		    min = a;
		    minw = w;
		    minh = h;
		    mine = e;
		}
	    }
	    final Tile[] order = new Tile[nt];
	    final Coord[] place = new Coord[nt];
	    Tex packbuf = new TexL(new Coord(minw, minh)) {
		    {
			mipmap(Mipmapper.avg);
			minfilter(javax.media.opengl.GL2.GL_NEAREST_MIPMAP_LINEAR);
			centroid = true;
		    }

		    protected BufferedImage fill() {
			BufferedImage buf = TexI.mkbuf(dim);
			Graphics g = buf.createGraphics();
			for(int i = 0; i < nt; i++)
			    g.drawImage(order[i].img, place[i].x, place[i].y, null);
			g.dispose();
			return(buf);
		    }

		    public String toString() {
			return("TileTex(" + Resource.this.name + ")");
		    }
		};
	    int x = 0, y = 0, n = 0;
	    for(Tile t :  tiles) {
		if(y >= minh)
		    throw(new LoadException("Could not pack tiles into calculated minimum texture", Resource.this));
		order[n] = t;
		place[n] = new Coord(x, y);
		t.tex = new TexSI(packbuf, place[n], tsz);
		n++;
		if((x += tsz.x) > (minw - tsz.x)) {
		    x = 0;
		    y += tsz.y;
		}
	    }
	}

	@SuppressWarnings("unchecked")
	public void init() {
	    WeightList<Tile> ground = new WeightList<Tile>();
	    WeightList<Tile>[] ctrans = new WeightList[15];
	    WeightList<Tile>[] btrans = new WeightList[15];
	    for(int i = 0; i < 15; i++) {
		ctrans[i] = new WeightList<Tile>();
		btrans[i] = new WeightList<Tile>();
	    }
	    int cn = 0, bn = 0;
	    Collection<Tile> tiles = new LinkedList<Tile>();
	    Coord tsz = null;
	    for(Tile t : layers(Tile.class, false)) {
		if(t.t == 'g') {
		    ground.add(t, t.w);
		} else if(t.t == 'b') {
		    btrans[t.id - 1].add(t, t.w);
		    bn++;
		} else if(t.t == 'c') {
		    ctrans[t.id - 1].add(t, t.w);
		    cn++;
		}
		tiles.add(t);
		if(tsz == null) {
		    tsz = Utils.imgsz(t.img);
		} else {
		    if(!Utils.imgsz(t.img).equals(tsz)) {
			throw(new LoadException("Different tile sizes within set", Resource.this));
		    }
		}
	    }
	    if(ground.size() > 0)
		this.ground = ground;
	    if(cn > 0)
		this.ctrans = ctrans;
	    if(bn > 0)
		this.btrans = btrans;
	    packtiles(tiles, tsz);
	}
    }

    /* Only for backwards compatibility */
    @LayerName("tileset")
    public static class OrigTileset implements LayerFactory<Tileset> {
	public Tileset cons(Resource res, byte[] buf) {
	    Tileset ret = res.new Tileset();
	    int[] off = new int[1];
	    off[0] = 0;
	    int fl = Utils.ub(buf[off[0]++]);
	    int flnum = Utils.uint16d(buf, off[0]);
	    off[0] += 2;
	    ret.flavprob = Utils.uint16d(buf, off[0]);
	    off[0] += 2;
	    for(int i = 0; i < flnum; i++) {
		String fln = Utils.strd(buf, off);
		int flv = Utils.uint16d(buf, off[0]);
		off[0] += 2;
		int flw = Utils.ub(buf[off[0]++]);
		try {
		    ret.flavobjs.add(load(fln, flv), flw);
		} catch(RuntimeException e) {
		    throw(new LoadException("Illegal resource dependency", e, res));
		}
	    }
	    return(ret);
	}
    }

    @LayerName("pagina")
    public class Pagina extends Layer {
	public final String text;
		
	public Pagina(byte[] buf) {
	    try {
		text = new String(buf, "UTF-8");
	    } catch(UnsupportedEncodingException e) {
		throw(new LoadException(e, Resource.this));
	    }
	}
		
	public void init() {}
    }

    @LayerName("action")
    public class AButton extends Layer {
	public final String name;
	public final Resource parent;
	public final char hk;
	public final String[] ad;
		
	public AButton(byte[] buf) {
	    int[] off = new int[1];
	    off[0] = 0;
	    String pr = Utils.strd(buf, off);
	    int pver = Utils.uint16d(buf, off[0]);
	    off[0] += 2;
	    if(pr.length() == 0) {
		parent = null;
	    } else {
		try {
		    parent = load(pr, pver);
		} catch(RuntimeException e) {
		    throw(new LoadException("Illegal resource dependency", e, Resource.this));
		}
	    }
	    name = Utils.strd(buf, off);
	    Utils.strd(buf, off); /* Prerequisite skill */
	    hk = (char)Utils.uint16d(buf, off[0]);
	    off[0] += 2;
	    ad = new String[Utils.uint16d(buf, off[0])];
	    off[0] += 2;
	    for(int i = 0; i < ad.length; i++)
		ad[i] = Utils.strd(buf, off);
	}
		
	public void init() {}
    }
    
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    public @interface PublishedCode {
	String name();
	Class<? extends Instancer> instancer() default Instancer.class;
	public interface Instancer {
	    public Object make(Class<?> cl) throws InstantiationException, IllegalAccessException;
	}
    }

    @LayerName("code")
    public class Code extends Layer {
	public final String name;
	transient public final byte[] data;
		
	public Code(byte[] buf) {
	    int[] off = new int[1];
	    off[0] = 0;
	    name = Utils.strd(buf, off);
	    data = new byte[buf.length - off[0]];
	    System.arraycopy(buf, off[0], data, 0, data.length);
	}
		
	public void init() {}
    }

    public class ResClassLoader extends ClassLoader {
	public ResClassLoader(ClassLoader parent) {
	    super(parent);
	}
	
	public Resource getres() {
	    return(Resource.this);
	}
	
	public String toString() {
	    return("cl:" + Resource.this.toString());
	}
    };

    public static Resource classres(final Class<?> cl) {
	return(java.security.AccessController.doPrivileged(new java.security.PrivilegedAction<Resource>() {
		    public Resource run() {
			ClassLoader l = cl.getClassLoader();
			if(l instanceof ResClassLoader)
			    return(((ResClassLoader)l).getres());
			throw(new RuntimeException("Cannot fetch resource of non-resloaded class " + cl));
		    }
		}));
    }

    public <T> T getcode(Class<T> cl, boolean fail) {
	CodeEntry e = layer(CodeEntry.class);
	if(e == null) {
	    if(fail)
		throw(new RuntimeException("Tried to fetch non-present res-loaded class " + cl.getName() + " from " + Resource.this.name));
	    return(null);
	}
	return(e.get(cl, fail));
    }

    public static class LibClassLoader extends ClassLoader {
	private final ClassLoader[] classpath;
	
	public LibClassLoader(ClassLoader parent, Collection<ClassLoader> classpath) {
	    super(parent);
	    this.classpath = classpath.toArray(new ClassLoader[0]);
	}
	
	public Class<?> findClass(String name) throws ClassNotFoundException {
	    for(ClassLoader lib : classpath) {
		try {
		    return(lib.loadClass(name));
		} catch(ClassNotFoundException e) {}
	    }
	    throw(new ClassNotFoundException("Could not find " + name + " in any of " + Arrays.asList(classpath).toString()));
	}
    }

    @LayerName("codeentry")
    public class CodeEntry extends Layer {
	private String clnm;
	private Map<String, Code> clmap = new TreeMap<String, Code>();
	private Map<String, String> pe = new TreeMap<String, String>();
	private Collection<Resource> classpath = new LinkedList<Resource>();
	transient private ClassLoader loader;
	transient private Map<String, Class<?>> lpe = null;
	transient private Map<Class<?>, Object> ipe = new HashMap<Class<?>, Object>();
		
	public CodeEntry(byte[] buf) {
	    int[] off = new int[1];
	    off[0] = 0;
	    while(off[0] < buf.length) {
		int t = buf[off[0]++];
		if(t == 1) {
		    while(true) {
			String en = Utils.strd(buf, off);
			String cn = Utils.strd(buf, off);
			if(en.length() == 0)
			    break;
			pe.put(en, cn);
		    }
		} else if(t == 2) {
		    while(true) {
			String ln = Utils.strd(buf, off);
			if(ln.length() == 0)
			    break;
			int ver = Utils.uint16d(buf, off[0]); off[0] += 2;
			classpath.add(Resource.load(ln, ver));
		    }
		} else {
		    throw(new LoadException("Unknown codeentry data type: " + t, Resource.this));
		}
	    }
	}
		
	public void init() {
	    for(Code c : layers(Code.class, false))
		clmap.put(c.name, c);
	}
	
	public ClassLoader loader(final boolean wait) {
	    synchronized(CodeEntry.this) {
		if(this.loader == null) {
		    this.loader = java.security.AccessController.doPrivileged(new java.security.PrivilegedAction<ClassLoader>() {
			    public ClassLoader run() {
				ClassLoader parent = Resource.class.getClassLoader();
				if(classpath.size() > 0) {
				    Collection<ClassLoader> loaders = new LinkedList<ClassLoader>();
				    for(Resource res : classpath) {
					if(wait)
					    res.loadwait();
					loaders.add(res.layer(CodeEntry.class).loader(wait));
				    }
				    parent = new LibClassLoader(parent, loaders);
				}
				return(new ResClassLoader(parent) {
					public Class<?> findClass(String name) throws ClassNotFoundException {
					    Code c = clmap.get(name);
					    if(c == null)
						throw(new ClassNotFoundException("Could not find class " + name + " in resource (" + Resource.this + ")"));
					    return(defineClass(name, c.data, 0, c.data.length));
					}
				    });
			    }
			});
		}
	    }
	    return(this.loader);
	}

	private void load() {
	    synchronized(CodeEntry.class) {
		if(lpe != null)
		    return;
		ClassLoader loader = loader(false);
		lpe = new TreeMap<String, Class<?>>();
		try {
		    for(Map.Entry<String, String> e : pe.entrySet()) {
			String name = e.getKey();
			String clnm = e.getValue();
			Class<?> cl = loader.loadClass(clnm);
			lpe.put(name, cl);
		    }
		} catch(ClassNotFoundException e) {
		    throw(new LoadException(e, Resource.this));
		}
	    }
	}
	
	public <T> T get(Class<T> cl, boolean fail) {
	    load();
	    PublishedCode entry = cl.getAnnotation(PublishedCode.class);
	    if(entry == null)
		throw(new RuntimeException("Tried to fetch non-published res-loaded class " + cl.getName() + " from " + Resource.this.name));
	    Class<?> acl;
	    synchronized(lpe) {
		if(lpe.get(entry.name()) == null) {
		    if(fail)
			throw(new RuntimeException("Tried to fetch non-present res-loaded class " + cl.getName() + " from " + Resource.this.name));
		    return(null);
		} else {
		    acl = lpe.get(entry.name());
		}
	    }
	    try {
		synchronized(ipe) {
		    Object pinst;
		    if((pinst = ipe.get(acl)) != null) {
			return(cl.cast(pinst));
		    } else {
			T inst;
			Object rinst;
			if(entry.instancer() != PublishedCode.Instancer.class)
			    rinst = entry.instancer().newInstance().make(acl);
			else
			    rinst = acl.newInstance();
			try {
			    inst = cl.cast(rinst);
			} catch(ClassCastException e) {
			    throw(new ClassCastException("Published class in " + Resource.this.name + " is not of type " + cl));
			}
			ipe.put(acl, inst);
			return(inst);
		    }
		}
	    } catch(InstantiationException e) {
		throw(new RuntimeException(e));
	    } catch(IllegalAccessException e) {
		throw(new RuntimeException(e));
	    }
	}

	public <T> T get(Class<T> cl) {
	    return(get(cl, true));
	}
    }

    @LayerName("audio")
    public class Audio extends Layer implements IDLayer<String> {
	transient public byte[] coded;
	public final String id;
	public double bvol = 1.0;

	public Audio(byte[] coded, String id) {
	    this.coded = coded;
	    this.id = id.intern();
	}

	public Audio(byte[] buf) {
	    this(buf, "cl");
	}

	public void init() {}

	public InputStream pcmstream() {
	    try {
		return(new dolda.xiphutil.VorbisStream(new ByteArrayInputStream(coded)).pcmstream());
	    } catch(IOException e) {
		throw(new RuntimeException(e));
	    }
	}

	public String layerid() {
	    return(id);
	}
    }

    @LayerName("audio2")
    public static class Audio2 implements LayerFactory<Audio> {
	public Audio cons(Resource res, byte[] buf) {
	    int[] off = {0};
	    int ver = buf[off[0]++];
	    if((ver == 1) || (ver == 2)) {
		String id = Utils.strd(buf, off);
		double bvol = 1.0;
		if(ver == 2) {
		    bvol = Utils.uint16d(buf, off[0]) / 1000.0; off[0] += 2;
		}
		byte[] data = new byte[buf.length - off[0]];
		System.arraycopy(buf, off[0], data, 0, buf.length - off[0]);
		Audio ret = res.new Audio(data, id);
		ret.bvol = bvol;
		return(ret);
	    } else {
		throw(new LoadException("Unknown audio layer version: " + ver, res));
	    }
	}
    }

    @LayerName("midi")
    public class Music extends Resource.Layer {
	transient javax.sound.midi.Sequence seq;
	
	public Music(byte[] buf) {
	    try {
		seq = javax.sound.midi.MidiSystem.getSequence(new ByteArrayInputStream(buf));
	    } catch(javax.sound.midi.InvalidMidiDataException e) {
		throw(new LoadException("Invalid MIDI data", Resource.this));
	    } catch(IOException e) {
		throw(new LoadException(e, Resource.this));
	    }
	}
	
	public void init() {}
    }

    @LayerName("font")
    public class Font extends Layer {
	public transient final java.awt.Font font;

	public Font(byte[] buf) {
	    int[] off = {0};
	    int ver = buf[off[0]++];
	    if(ver == 1) {
		int type = buf[off[0]++];
		if(type == 0) {
		    try {
			this.font = java.awt.Font.createFont(java.awt.Font.TRUETYPE_FONT, new ByteArrayInputStream(buf, off[0], buf.length - off[0]));
		    } catch(Exception e) {
			throw(new RuntimeException(e));
		    }
		} else {
		    throw(new LoadException("Unknown font type: " + type, Resource.this));
		}
	    } else {
		throw(new LoadException("Unknown font layer version: " + ver, Resource.this));
	    }
	}

	public void init() {}
    }

    private void readall(InputStream in, byte[] buf) throws IOException {
	int ret, off = 0;
	while(off < buf.length) {
	    ret = in.read(buf, off, buf.length - off);
	    if(ret < 0)
		throw(new LoadException("Incomplete resource at " + name, this));
	    off += ret;
	}
    }
	
    public <L extends Layer> Collection<L> layers(final Class<L> cl, boolean th) {
	if(loading && th)
	    throw(new Loading(this));
	checkerr();
	return(new AbstractCollection<L>() {
		public int size() {
		    int s = 0;
		    for(L l : this)
			s++;
		    return(s);
		}
		
		public Iterator<L> iterator() {
		    return(new Iterator<L>() {
			    Iterator<Layer> i = layers.iterator();
			    L c = n();
			    
			    private L n() {
				while(i.hasNext()) {
				    Layer l = i.next();
				    if(cl.isInstance(l))
					return(cl.cast(l));
				}
				return(null);
			    }
			    
			    public boolean hasNext() {
				return(c != null);
			    }
			    
			    public L next() {
				L ret = c;
				if(ret == null)
				    throw(new NoSuchElementException());
				c = n();
				return(ret);
			    }
			    
			    public void remove() {
				throw(new UnsupportedOperationException());
			    }
			});
		}
	    });
    }
    
    public <L extends Layer> Collection<L> layers(Class<L> cl) {
	return(layers(cl, true));
    }
	
    public <L extends Layer> L layer(Class<L> cl, boolean th) {
	if(loading && th)
	    throw(new Loading(this));
	checkerr();
	for(Layer l : layers) {
	    if(cl.isInstance(l))
		return(cl.cast(l));
	}
	return(null);
    }
	
    public <L extends Layer> L layer(Class<L> cl) {
	return(layer(cl, true));
    }
    
    public <I, L extends IDLayer<I>> L layer(Class<L> cl, I id) {
	if(loading)
	    throw(new Loading(this));
	checkerr();
	for(Layer l : layers) {
	    if(cl.isInstance(l)) {
		L ll = cl.cast(l);
		if(ll.layerid().equals(id))
		    return(ll);
	    }
	}
	return(null);
    }
    
    public int compareTo(Resource other) {
	checkerr();
	int nc = name.compareTo(other.name);
	if(nc != 0)
	    return(nc);
	if(ver != other.ver)
	    return(ver - other.ver);
	if(other != this)
	    throw(new RuntimeException("Resource identity crisis!"));
	return(0);
    }
	
    public boolean equals(Object other) {
	if(!(other instanceof Resource) || (other == null))
	    return(false);
	return(compareTo((Resource)other) == 0);
    }
	
    private void load(InputStream in) throws IOException {
	String sig = "Haven Resource 1";
	byte buf[] = new byte[sig.length()];
	readall(in, buf);
	if(!sig.equals(new String(buf)))
	    throw(new LoadException("Invalid res signature", this));
	buf = new byte[2];
	readall(in, buf);
	int ver = Utils.uint16d(buf, 0);
	List<Layer> layers = new LinkedList<Layer>();
	if(this.ver == -1) {
	    this.ver = ver;
	} else {
	    if(ver != this.ver)
		throw(new LoadException("Wrong res version (" + ver + " != " + this.ver + ")", this));
	}
	outer: while(true) {
	    StringBuilder tbuf = new StringBuilder();
	    while(true) {
		byte bb;
		int ib;
		if((ib = in.read()) == -1) {
		    if(tbuf.length() == 0)
			break outer;
		    throw(new LoadException("Incomplete resource at " + name, this));
		}
		bb = (byte)ib;
		if(bb == 0)
		    break;
		tbuf.append((char)bb);
	    }
	    buf = new byte[4];
	    readall(in, buf);
	    int len = Utils.int32d(buf, 0);
	    buf = new byte[len];
	    readall(in, buf);
	    LayerFactory<?> lc = ltypes.get(tbuf.toString());
	    if(lc == null)
		continue;
	    layers.add(lc.cons(this, buf));
	}
	this.layers = layers;
	for(Layer l : layers)
	    l.init();
    }
	
    public Indir<Resource> indir() {
	if(indir != null)
	    return(indir);
	indir = new Indir<Resource>() {
	    public Resource res = Resource.this;
			
	    public Resource get() {
		if(loading)
		    throw(new Loading(Resource.this));
		return(Resource.this);
	    }
			
	    public void set(Resource r) {
		throw(new RuntimeException());
	    }
			
	    public int compareTo(Indir<Resource> x) {
		return(Resource.this.compareTo(this.getClass().cast(x).res));
	    }
	};
	return(indir);
    }
	
    public void checkerr() {
	if(error != null)
	    throw(new RuntimeException("Delayed error in resource " + name + " (v" + ver + "), from " + source, error));
    }
	
    public int priority() {
	return(prio);
    }

    public static BufferedImage loadimg(String name) {
	Resource res = load(name);
	res.loadwait();
	return(res.layer(imgc).img);
    }
	
    public static Tex loadtex(String name) {
	Resource res = load(name);
	res.loadwait();
	return(res.layer(imgc).tex());
    }
	
    public String toString() {
	return(name + "(v" + ver + ")");
    }
	
    public static void loadlist(InputStream list, int prio) throws IOException {
	BufferedReader in = new BufferedReader(new InputStreamReader(list, "us-ascii"));
	String ln;
	while((ln = in.readLine()) != null) {
	    int pos = ln.indexOf(':');
	    if(pos < 0)
		continue;
	    String nm = ln.substring(0, pos);
	    int ver;
	    try {
		ver = Integer.parseInt(ln.substring(pos + 1));
	    } catch(NumberFormatException e) {
		continue;
	    }
	    try {
		load(nm, ver, prio);
	    } catch(RuntimeException e) {
	    }
	}
	in.close();
    }

    public static void dumplist(Collection<Resource> list, Writer dest) {
	PrintWriter out = new PrintWriter(dest);
	List<Resource> sorted = new ArrayList<Resource>(list);
	Collections.sort(sorted);
	for(Resource res : sorted) {
	    if(res.loading)
		continue;
	    out.println(res.name + ":" + res.ver);
	}
    }

    public static void updateloadlist(File file) throws Exception {
	BufferedReader r = new BufferedReader(new FileReader(file));
	Map<String, Integer> orig = new HashMap<String, Integer>();
	String ln;
	while((ln = r.readLine()) != null) {
	    int pos = ln.indexOf(':');
	    if(pos < 0) {
		System.err.println("Weird line: " + ln);
		continue;
	    }
	    String nm = ln.substring(0, pos);
	    int ver = Integer.parseInt(ln.substring(pos + 1));
	    orig.put(nm, ver);
	}
	r.close();
	for(String nm : orig.keySet())
	    load(nm);
	while(true) {
	    int d = qdepth();
	    if(d == 0)
		break;
	    System.out.print("\033[1GLoading... " + d + "\033[K");
	    Thread.sleep(500);
	}
	System.out.println();
	Collection<Resource> cur = new LinkedList<Resource>();
	for(Map.Entry<String, Integer> e : orig.entrySet()) {
	    String nm = e.getKey();
	    int ver = e.getValue();
	    Resource res = load(nm);
	    res.loadwait();
	    res.checkerr();
	    if(res.ver != ver)
		System.out.println(nm + ": " + ver + " -> " + res.ver);
	    cur.add(res);
	}
	Writer w = new OutputStreamWriter(new FileOutputStream(file), "UTF-8");
	try {
	    dumplist(cur, w);
	} finally {
	    w.close();
	}
    }

    public static void main(String[] args) throws Exception {
	String cmd = args[0].intern();
	if(cmd == "update") {
	    updateloadlist(new File(args[1]));
	}
    }
}
