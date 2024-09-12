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

import haven.render.*;
import java.util.*;
import java.util.function.*;
import java.io.*;
import java.net.*;
import java.awt.event.*;
import java.awt.datatransfer.*;
import javax.swing.filechooser.*;
import javax.imageio.*;
import java.awt.Color;
import java.awt.image.*;
import javax.swing.JFileChooser;
import static haven.PUtils.*;
import static haven.render.Texture.Filter.*;

public class DynresWindow extends Window {
    public static final Config.Variable<URI> service = Config.Services.var("dynresurl", "");
    public static final Coord itemsz = UI.scale(128, 128);
    public final Future<List<Preview.Spec>> previews = Preview.Spec.fetch();
    public final Color[] pal;
    public final List<Image> imgs = new ArrayList<>();
    private final Adder adder;

    @RName("dynres")
    public static class $_ implements Factory {
	public Widget create(UI ui, Object[] args) {
	    return(new DynresWindow(Utils.cast((Object[])args[0], Color.class)));
	}
    }

    public DynresWindow(Color[] pal) {
	super(Coord.z, "Imagery", true);
	this.pal = pal;
	this.adder = (service.get() == null) ? null : add(new Adder(itemsz));
	arrange();
    }

    public static Map<String, Object> ttoresp(HttpURLConnection conn) throws IOException {
	int status = conn.getResponseCode();
	if((status >= 500) && (status < 600))
	    throw(new IOException("The server encountered an interal error."));
	try(InputStream in = conn.getErrorStream()) {
	    if(in != null) {
		if(!Utils.eq(conn.getContentType(), "application/x-haven-ttol"))
		    throw(new IOException("Unexpected reply from server: " + status + " " + conn.getResponseMessage()));
		Object[] data = new StreamMessage(in).list();
		return(Utils.mapdecn(data, String.class, Object.class));
	    }
	}
	try(InputStream in = conn.getInputStream()) {
	    if(!Utils.eq(conn.getContentType(), "application/x-haven-ttol"))
		throw(new IOException("Unexpected reply from server" + status + " " + conn.getResponseMessage()));
	    Object[] data = new StreamMessage(in).list();
	    return(Utils.mapdecn(data, String.class, Object.class));
	}
    }

    public static String auth(Session sess) {
	return("Haven " +
	       Utils.base64enc(Utils.concat(sess.username.getBytes(Utils.utf8),
					    new byte[] {0},
					    Digest.hash(Digest.HMAC.of(Digest.SHA256, sess.sesskey),
							"dynres".getBytes(Utils.ascii)))));
    }

    public BufferedImage process(BufferedImage in) {
	int w = in.getWidth(), h = in.getHeight();
	int d = Math.min(1 << Math.round(Math.log((w + h) / 2) / Math.log(2)), 512);
	if((d != w) || (d != h)) {
	    synchronized(ui) {
		ui.msg("Note: The image should be square and be an even power of two in size for best results.");
	    }
	}
	WritableRaster buf = imgraster(Coord.of(d));
	boolean mismatch = false;
	for(int y = 0; y < d; y++) {
	    int sy = (y * h) / d;
	    for(int x = 0; x < d; x++) {
		int sx = (x * w) / d;
		int rgba = in.getRGB(sx, sy);
		int a = (rgba & 0xff000000) >>> 24;
		if(a < 128) {
		    for(int b = 0; b < 4; b++)
			buf.setSample(x, y, b, 0);
		} else {
		    Color closest = null;
		    int mdiff = 0;
		    for(Color col : pal) {
			if(col.getAlpha() == 0)
			    continue;
			int diff =
			    Math.abs(col.getRed()   - ((rgba & 0x00ff0000) >>> 16)) +
			    Math.abs(col.getGreen() - ((rgba & 0x0000ff00) >>>  8)) +
			    Math.abs(col.getBlue()  - ((rgba & 0x000000ff) >>>  0));
			if((closest == null) || (diff < mdiff)) {
			    closest = col;
			    mdiff = diff;
			}
		    }
		    mismatch |= mdiff > 0;
		    buf.setSample(x, y, 0, closest.getRed());
		    buf.setSample(x, y, 1, closest.getGreen());
		    buf.setSample(x, y, 2, closest.getBlue());
		    buf.setSample(x, y, 3, 255);
		}
	    }
	}
	if(mismatch) {
	    synchronized(ui) {
		ui.msg("Note: The image contains colors outside the valid palette, and has been clamped to it.");
	    }
	}
	return(rasterimg(buf));
    }

    public static class PaletteCopy implements Transferable, ClipboardOwner {
	public final Color[] pal;
	private final Map<DataFlavor, Supplier<Object>> types;

	public PaletteCopy(Color[] pal) {
	    this.pal = pal;
	    types = Utils.<DataFlavor, Supplier<Object>>map()
		.put(DataFlavor.imageFlavor, this::img)
		.put(DataFlavor.stringFlavor, this::text)
		.map();
	}

	private BufferedImage img() {
	    int w;
	    for(w = (int)Math.floor(Math.sqrt(pal.length)); w > 1; w--) {
		if((pal.length % w) == 0) {
		    w = pal.length / w;
		    break;
		}
	    }
	    if(w == 0)
		w = (int)Math.ceil(Math.sqrt(pal.length));
	    Coord csz = Coord.of(20, 20);
	    WritableRaster buf = imgraster(csz.mul(w, (pal.length + w - 1) / w));
	    for(int i = 0 ; i < pal.length; i++) {
		Color c = pal[i];
		int X = i % w, Y = i / w;
		for(int y = Y * csz.y; y < (Y + 1) * csz.y; y++) {
		    for(int x = X * csz.x; x < (X + 1) * csz.x; x++) {
			buf.setSample(x, y, 0, c.getRed());
			buf.setSample(x, y, 1, c.getGreen());
			buf.setSample(x, y, 2, c.getBlue());
			buf.setSample(x, y, 3, 255);
		    }
		}
	    }
	    return(rasterimg(buf));
	}

	private String text() {
	    StringBuilder buf = new StringBuilder();
	    for(Color c : pal)
		buf.append(String.format("#%02X%02X%02X\n", c.getRed(), c.getGreen(), c.getBlue()));
	    return(buf.toString());
	}

	public DataFlavor[] getTransferDataFlavors() {
	    return(types.keySet().toArray(new DataFlavor[0]));
	}
	public boolean isDataFlavorSupported(DataFlavor f) {
	    return(types.containsKey(f));
	}
	public Object getTransferData(DataFlavor f) {
	    return(types.get(f).get());
	}
	public void lostOwnership(Clipboard c, Transferable t) {
	}
    }

    public class Adder extends Widget implements DropTarget {
	private final List<Future<BufferedImage>> processing = new LinkedList<>();
	private SListMenu menu;

	public Adder(Coord sz) {
	    super(sz);
	    adda(new Label("New image..."), sz.div(2), 0.5, 0.5);
	    settip(mktip(), true);
	}

	private String mktip() {
	    StringBuilder tip = new StringBuilder();
	    tip.append("$i{Tips for formatting images:}\n" +
		       "\n" +
		       " \u2022 Images should optimally be even powers of two in size. If they are not, they will be automatically resized.\n" +
		       " \u2022 The following colors are accepted:\n");
	    int w = 5;
	    for(int i = 0; i < pal.length; i++) {
		Color c = pal[i];
		    tip.append(((i % w) == 0) ? "\u2003" : ", ");
		double val = (0.2126 * c.getRed() / 255.0) + (0.7152 * c.getGreen() / 255.0) + (0.0722 * c.getBlue() / 255.0);
		tip.append(String.format("$bg[%d,%d,%d]{%s{$font[Monospaced]{$b{#%02X%02X%02X}}}}",
					 c.getRed(), c.getGreen(), c.getBlue(),
					 RichText.Parser.col2a((val > 0.25) ? Color.BLACK : Color.WHITE),
					 c.getRed(), c.getGreen(), c.getBlue()));
		if(((i + 1) % w) == 0)
		    tip.append("\n");
	    }
	    return(tip.toString());
	}

	public void draw(GOut g) {
	    super.draw(g);
	    Window.wbox.draw(g, Coord.z, sz);
	}

	public void create(Supplier<BufferedImage> img) {
	    if(!previews.done()) {
		ui.error("Please wait, still downloading preview information...");
		return;
	    }
	    processing.add(Defer.later(() -> {
			BufferedImage pre = img.get();
			if(pre == null)
			    return(null);
			return(process(pre));
		    }));
	}

	public void tick(double dt) {
	    super.tick(dt);
	    for(Iterator<Future<BufferedImage>> i = processing.iterator(); i.hasNext();) {
		Future<BufferedImage> proc = i.next();
		if(proc.done()) {
		    try {
			BufferedImage img = proc.get();
			if(img != null)
			    getparent(GameUI.class).addchild(new PreviewWindow(img, previews.get()), "misc", new Coord2d(0.2, 0.2));
		    } catch(Defer.DeferredException e) {
			ui.error(e.getCause().getMessage());
		    }
		    i.remove();
		}
	    }
	}

	private void open(File file) {
	    create(() -> {
		    try {
			BufferedImage img = ImageIO.read(file);
			if(img == null)
			    throw(new IOException("File format not recognized."));
			return(img);
		    } catch(IOException e) {
			throw(new RuntimeException(e));
		    }
		});
	}

	private void open() {
	    java.awt.EventQueue.invokeLater(() -> {
		    JFileChooser fc = new JFileChooser();
		    fc.setFileFilter(new FileNameExtensionFilter("Image file", "png", "jpg", "jpeg", "bmp"));
		    if(fc.showOpenDialog(null) != JFileChooser.APPROVE_OPTION)
			return;
		    open(fc.getSelectedFile());
		});
	}

	private BufferedImage getpaste(Clipboard c) throws IOException {
	    if(c == null)
		return(null);
	    try {
		return((BufferedImage)c.getData(DataFlavor.imageFlavor));
	    } catch(ClassCastException e) {
	    } catch(IllegalStateException e) {
		throw(new IOException(e.getMessage(), e));
	    } catch(UnsupportedFlavorException e) {
	    }
	    return(null);
	}

	private void paste() {
	    create(() -> {
		    try {
			BufferedImage img = getpaste(java.awt.Toolkit.getDefaultToolkit().getSystemClipboard());
			if(img == null)
			    throw(new RuntimeException("The clipboard contains no image."));
			return(img);
		    } catch(IOException e) {
			throw(new RuntimeException(e));
		    }
		});
	}

	public boolean mousedown(Coord c, int btn) {
	    if(btn == 2) {
		create(() -> {
			try {
			    return(getpaste(java.awt.Toolkit.getDefaultToolkit().getSystemSelection()));
			} catch(IOException e) {
			    throw(new RuntimeException(e));
			}
		    });
		return(true);
	    }
	    return(super.mousedown(c, btn));
	}

	private void copypal() {
	    Clipboard cb = java.awt.Toolkit.getDefaultToolkit().getSystemClipboard();
	    if(cb != null) {
		PaletteCopy xf = new PaletteCopy(pal);
		try {
		    cb.setContents(xf, xf);
		} catch(IllegalStateException e) {
		}
	    }
	}

	public boolean mousehover(Coord c, boolean hovering) {
	    boolean menuhover = (menu != null) && (menu.parent != null) && menu.rootarea().contains(ui.mc);
	    if((hovering || menuhover) && (menu == null)) {
		menu = SListMenu.of(UI.scale(250, 200), null,
				    Arrays.asList(SListMenu.Action.of("Select from file...", this::open),
						  SListMenu.Action.of("Paste from clipboard...", this::paste),
						  SListMenu.Action.of("Copy palette to clipboard", this::copypal)))
		    .addat(this, pos("cbl"));
	    } else if(!(hovering || menuhover) && (menu != null)) {
		menu.reqdestroy();
		menu = null;
	    }
	    return(hovering);
	}

	public boolean drophover(Coord c, boolean hovering, Object thing) {
	    if(thing instanceof SystemDrop) {
		SystemDrop d = (SystemDrop)thing;
		return(hovering && (d.supports(DataFlavor.imageFlavor) || d.supports(DataFlavor.javaFileListFlavor)));
	    }
	    return(false);
	}

	public boolean dropthing(Coord c, Object thing) {
	    if(thing instanceof SystemDrop) {
		SystemDrop d = (SystemDrop)thing;
		boolean rv = false;
		if(d.supports(DataFlavor.imageFlavor)) {
		    try {
			BufferedImage img = (BufferedImage)d.receive(DataFlavor.imageFlavor);
			if(img != null) {
			    create(() -> img);
			    rv = true;
			}
		    } catch(ClassCastException e) {
		    } catch(IOException e) {
			ui.error(e.getMessage());
		    }
		} else if(d.supports(DataFlavor.javaFileListFlavor)) {
		    List files = null;
		    try {
			files = (List)d.receive(DataFlavor.javaFileListFlavor);
		    } catch(ClassCastException e) {
		    } catch(IOException e) {
			ui.error(e.getMessage());
		    }
		    if(files != null) {
			for(Object o : files) {
			    File f = (File)o;
			    open(f);
			}
			rv = true;
		    }
		}
		return(rv);
	    }
	    return(false);
	}
    }

    public boolean keydown(KeyEvent ev) {
	if(super.keydown(ev))
	    return(true);
	if(adder != null) {
	    int mod = UI.modflags(ev);
	    if((ev.getKeyChar() == 22) ||
	       ((ev.getKeyCode() == KeyEvent.VK_INSERT) && (mod == KeyMatch.S)))
		{
		    adder.paste();
		    return(true);
		}
	}
	return(false);
    }

    public class Image extends Widget implements Transferable, ClipboardOwner {
	public final UID id;
	public final Indir<Resource> res;
	private BufferedImage img;
	private SListMenu menu;

	public Image(Coord sz, UID id) {
	    super(sz);
	    this.id = id;
	    this.res = Resource.remote().dynres(id);
	}

	public Image(UID id) {
	    this(itemsz, id);
	}

	private TexRender tex = null;
	public void draw(GOut g) {
	    IBox b = Window.wbox;
	    try {
		if(tex == null)
		    tex = res.get().flayer(TexR.class).tex();
		g.image(tex, b.btloff(), sz.sub(b.bisz()));
	    } catch(Loading l) {
	    }
	    b.draw(g, Coord.z, sz);
	}

	private void craft(boolean master) {
	    DynresWindow.this.wdgmsg("make", id, master ? 1 : 0);
	}

	private void delete0() {
	    try {
		HttpURLConnection conn = (HttpURLConnection)Http.open(Utils.uriparam(service.get().resolve("remove"), "uid", id.toString()).toURL());
		conn.setDoOutput(true);
		conn.addRequestProperty("Authorization", auth(ui.sess));
		conn.connect();
		try(OutputStream out = conn.getOutputStream()) {
		}
		Map<String, Object> resp = ttoresp(conn);
		if(Utils.eq(resp.get("status"), "error"))
		    throw(new IOException((String)resp.get("message")));
	    } catch(IOException e) {
		synchronized(ui) {
		    ui.error(e.getMessage());
		}
	    }
	}

	private void delete() {
	    Defer.later(this::delete0, null);
	}

	private void copy() {
	    Clipboard c = java.awt.Toolkit.getDefaultToolkit().getSystemClipboard();
	    if(c != null) {
		try {
		    if(this.img == null)
			this.img = res.get().flayer(TexR.class).tex().fill();
		    c.setContents(this, this);
		} catch(Loading l) {
		}
	    }
	}

	public DataFlavor[] getTransferDataFlavors() {
	    return(new DataFlavor[] {DataFlavor.imageFlavor});
	}
	public boolean isDataFlavorSupported(DataFlavor f) {
	    return(Utils.eq(f, DataFlavor.imageFlavor));
	}
	public BufferedImage getTransferData(DataFlavor f) {
	    return(img);
	}
	public void lostOwnership(Clipboard c, Transferable t) {
	}

	public boolean mousehover(Coord c, boolean hovering) {
	    boolean menuhover = (menu != null) && (menu.parent != null) && menu.rootarea().contains(ui.mc);
	    if((hovering || menuhover) && (menu == null)) {
		menu = SListMenu.of(UI.scale(250, 200), null,
				    Arrays.asList(SListMenu.Action.of("Paint Sketch", () -> craft(false)),
						  SListMenu.Action.of("Paint Masterpiece", () -> craft(true)),
						  SListMenu.Action.of("Copy to clipboard", this::copy),
						  SListMenu.Action.of("Remove", this::delete)))
		    .addat(this, pos("cbl"));
	    } else if(!(hovering || menuhover) && (menu != null)) {
		menu.reqdestroy();
		menu = null;
	    }
	    return(hovering);
	}

	public void setinfo(int fields, float outline) {
	    settip(String.format("Fields: %,d\nContours: %.2f", fields, outline), true);
	}
    }

    public static class Preview extends Widget {
	public static final UID key = UID.of(1);
	public final List<Spec> specs;
	public View view;
	private final Indir<Resource> vres;
	private final SDropBox<Spec, Widget> list;

	public Preview(int minw, List<Spec> specs, TexL tex) {
	    this.specs = new ArrayList<>(specs);
	    vres = consres(tex);
	    Widget prev = add(new Label("Preview on:"), 0, 0);
	    String last = Utils.getpref("dynres-pv/lastspec", "");
	    Spec defspec = Utils.find(specs, s -> s.name.equals(last));
	    if(defspec == null)
		defspec = this.specs.get(0);
	    Collections.sort(this.specs, (a, b) -> a.name.compareTo(b.name));
	    prev = add(Frame.with(list = SDropBox.of(Math.max(UI.scale(250), minw) - Window.wbox.bisz().x, UI.scale(160), UI.scale(15),
						     this.specs, (spec, sz) -> SListWidget.TextItem.of(sz, Text.std, () -> spec.name), this::set),
				  false),
		       prev.pos("bl").adds(0, 5));
	    list.change(defspec);
	    add(Frame.with(view = new View(Coord.of(prev.sz.x), list.sel, vres), true), prev.pos("bl").adds(0, 5));
	    pack();
	}

	public static Indir<Resource> consres(TexL tex) {
	    Resource.Virtual cons = new Resource.Virtual("dyn/" + key, 1);
	    cons.add(new TexR.Image(cons, tex));
	    return(() -> cons);
	}

	public void set(Spec spec) {
	    if((view != null) && (spec != view.spec)) {
		View nview = view.parent.add(new View(view.sz, spec, vres), view.c);
		view.destroy();
		view = nview;
		Utils.setpref("dynres-pv/lastspec", spec.name);
	    }
	}

	public static class Spec {
	    public final String name;
	    public final Indir<Resource> res;
	    public final byte[] sdt;
	    public final int sdtoff;
	    public final Map<Integer, Indir<Resource>> resmap = new HashMap<>();
	    public final Pipe.Op st;

	    public Spec(Map<String, Object> spec) {
		this.name = (String)spec.get("name");
		Object[] res = (Object[])spec.get("res");
		this.res = new Resource.Spec(Resource.remote(), (String)res[0], Utils.iv(res[1]));;
		Object[] sdt = (Object[])spec.get("sdt");
		if(sdt == null) {
		    this.sdt = null;
		    this.sdtoff = -1;
		} else {
		    this.sdt = (byte[])sdt[0];
		    this.sdtoff = (sdt.length > 1) ? Utils.iv(sdt[1]) : -1;
		    if(sdt.length > 2) {
			for(Object obj : (Object[])sdt[2]) {
			    Object[] rdesc = (Object[])obj;
			    resmap.put(Utils.iv(rdesc[0]), new Resource.Spec(Resource.remote(), (String)rdesc[1], Utils.iv(rdesc[2])));
			}
		    }
		}
		Object[] loc = (Object[])spec.get("loc");
		if(loc == null) {
		    st = null;
		} else {
		    Matrix4f xf = new Matrix4f();
		    for(int i = 0; i < 16; i++)
			xf.m[i] = Utils.fv(loc[i]);
		    st = new Location(xf);
		}
	    }

	    public static Future<List<Spec>> fetch() {
		Defer.Callable<List<Spec>> t = () -> {
		    if(service.get() == null)
			return(null);
		    List<Spec> ret = new ArrayList<>();
		    try(InputStream in = Http.fetch(service.get().resolve("previews").toURL())) {
			for(Object desc : new StreamMessage(in).list())
			    ret.add(new Spec(Utils.mapdecn(desc, String.class, Object.class)));
		    } catch(IOException exc) {
			throw(new RuntimeException(exc));
		    }
		    return(ret);
		};
		return(Defer.later(t));
	    }

	    public static final OwnerContext.ClassResolver<Owner> ctxr = new OwnerContext.ClassResolver<Owner>()
		.add(Resource.Resolver.class, o -> o);
	    public class Owner implements Sprite.Owner, Resource.Resolver {
		public final Sprite.Owner bk;
		public final Indir<Resource> vres;

		public Owner(Sprite.Owner bk, Indir<Resource> vres) {
		    this.bk = bk;
		    this.vres = vres;
		}

		public Indir<Resource> getres(int id) {
		    return(resmap.get(id));
		}

		public Indir<Resource> dynres(UID uid) {
		    if(Utils.eq(uid, key))
			return(vres);
		    return(null);
		}

		public <T> T context(Class<T> cl) {
		    return(OwnerContext.orparent(cl, ctxr.context(cl, this), bk));
		}

		public Random mkrandoom() {return(bk.mkrandoom());}
		@SuppressWarnings("deprecation") public Resource getres() {return(bk.getres());}
	    }

	    public Sprite create(Sprite.Owner owner, Indir<Resource> vres) {
		Message sdt = Message.nil;
		if(this.sdt != null) {
		    byte[] dat = this.sdt;
		    if(sdtoff >= 0)
			dat = ((MessageBuf)new MessageBuf().addbytes(Utils.splice(dat, 0, sdtoff)).adduniqid(key).addbytes(Utils.splice(dat, sdtoff))).fin();
		    sdt = new MessageBuf(dat);
		}
		return(Sprite.create(new Owner(owner, vres), res.get(), sdt));
	    }
	}

	public static class View extends PView implements Sprite.Owner {
	    public final Spec spec;
	    private final Indir<Resource> vres;
	    private Sprite spr;
	    private RenderTree.Slot slot;
	    private float field, elev, angl;
	    private float tfield = Float.NaN, telev, tangl;

	    public View(Coord sz, Spec spec, Indir<Resource> vres) {
		super(sz);
		this.spec = spec;
		this.vres = vres;
		basic(Camera.class, Camera.pointed(Coord3f.o, 200, (float)Math.PI / 6, (float)Math.PI / 4));
		basic.add(new DirLight(new Color(96, 96, 160), new Color(255, 255, 208), Color.WHITE, Coord3f.of(1, 1, 1).norm()));
	    }

	    private void makeproj() {
	    }

	    public void resize(Coord sz) {
		super.resize(sz);
	    }

	    protected FColor clearcolor() {
		return(new FColor(0, 0, 0, 0.5f));
	    }

	    public static Volume3f getbounds(RenderTree.Node spr) {
		Volume3f ret = null;
		RenderTree tree = new RenderTree();
		tree.add(spr);
		for(RenderTree.Slot s : tree.slots()) {
		    if(s.obj() instanceof FastMesh) {
			FastMesh m = (FastMesh)s.obj();
			ret = (ret == null) ? m.bounds() : ret.include(m.bounds());
		    }
		}
		return(ret);
	    }

	    private void updatecam(double dt) {
		double tf = 3;
		float cf = 1f - (float)Math.pow(500, -dt * tf);
		float pfield = field;
		float maxfield = sz.x * (float)Math.sqrt(2) / 8f;
		float minfield = 10f;
		if(Float.isNaN(tfield))
		    field = tfield = maxfield;
		if(tfield < minfield)
		    tfield = minfield;
		if((tfield > maxfield))
		    tfield = maxfield;
		field = field + ((tfield - field) * cf);
		elev = elev + ((telev - elev) * cf);
		angl = angl + ((tangl - angl) * cf);
		if(field != pfield) {
		    float aspect = ((float)sz.y) / ((float)sz.x);
		    basic(Projection.class, Projection.ortho(-field, field, -field * aspect, field * aspect, 1, 5000));
		}
		if(slot != null) {
		    Location rot = new Location(Transform.makerot(new Matrix4f(), Coord3f.zu, angl)
						.mul1(Transform.makerot(new Matrix4f(), Coord3f.yu, elev)));
		    slot.ostate(rot);
		}
	    }

	    public void tick(double dt) {
		super.tick(dt);
		if(slot == null) {
		    try {
			if(spr == null)
			    spr = spec.create(this, vres);
			Volume3f bnd = getbounds(spr);
			Coord3f mid = bnd.p.add(bnd.n).div(2);
			RenderTree.Node n  = Pipe.Op.compose(spec.st, Location.xlate(mid.neg())).apply(spr, false);
			slot = basic.add(Pipe.Op.nil.apply(n, false));
		    } catch(Loading l) {}
		}
		if(spr != null)
		    spr.tick(dt);
		updatecam(dt);
	    }

	    public void gtick(Render out) {
		super.gtick(out);
		if(spr != null)
		    spr.gtick(out);
	    }

	    public static final OwnerContext.ClassResolver<View> ctxr = new OwnerContext.ClassResolver<View>()
		.add(View.class, v -> v)
		.add(Glob.class, v -> v.ui.sess.glob)
		.add(Session.class, v -> v.ui.sess);
	    public <T> T context(Class<T> cl) {
		return(ctxr.context(cl, this));
	    }

	    public Random mkrandoom() {return(new Random());}
	    @SuppressWarnings("deprecation") public Resource getres() {throw(new UnsupportedOperationException());}

	    public boolean mousewheel(Coord c, int amount) {
		tfield += amount * 10;
		return(true);
	    }

	    private Coord dragstart;
	    private UI.Grab grab;
	    private float dragelev, dragangl;
	    public boolean mousedown(Coord c, int btn) {
		if((btn == 1) && (grab == null)) {
		    dragstart = c;
		    dragelev = telev;
		    dragangl = tangl;
		    grab = ui.grabmouse(this);
		    return(true);
		}
		return(super.mousedown(c, btn));
	    }

	    public boolean mouseup(Coord c, int btn) {
		if((btn == 1) && (grab != null)) {
		    grab.remove();
		    grab = null;
		}
		return(super.mouseup(c, btn));
	    }

	    public void mousemove(Coord c) {
		super.mousemove(c);
		if(grab != null) {
		    tangl = dragangl + ((float)c.x - dragstart.x) * 0.01f;
		    telev = dragelev + ((float)c.y - dragstart.y) * 0.01f;
		}
	    }
	}
    }

    public static class PreviewWindow extends Window {
	public final BufferedImage img;
	public final TexL tex;
	private final Display display;
	private final Button uploadbtn;
	private Upload upload;
	private Progress prog;

	public PreviewWindow(BufferedImage img, List<Preview.Spec> previews) {
	    super(Coord.z, "Preview", true);
	    this.img = img;
	    this.tex = new TexL.Fixed(img);
	    tex.mipmap(Mipmapper.dav);
	    tex.img.magfilter(LINEAR).minfilter(LINEAR).mipfilter(LINEAR);
	    Widget prev = display = add(new Display(tex.sz().max(128, 128)), 0, 0);
	    if(previews != null)
		prev = add(new Preview(display.sz.x, previews, tex), prev.pos("bl").adds(0, 10));
	    uploadbtn = add(new Button(UI.scale(100), "Upload", false, this::upload), prev.pos("bl").adds(0, 10));
	    pack();
	    display.move(Coord.of((csz().x - display.sz.x) / 2, display.c.y));
	}

	public class Display extends Widget {
	    public Display(Coord sz) {
		super(sz.add(Window.wbox.bisz()));
	    }

	    public void draw(GOut g) {
		try {
		    g.image(tex, Window.wbox.btloff(), sz.sub(Window.wbox.bisz()));
		} catch(Loading l) {
		}
		Window.wbox.draw(g, Coord.z, sz);
	    }
	}

	public void reqclose() {
	    reqdestroy();
	}

	private class Upload implements Runnable {
	    volatile byte[] data;
	    volatile int off;
	    Map<String, Object> resp;

	    void prepare() throws IOException {
		ByteArrayOutputStream buf = new ByteArrayOutputStream();
		ImageIO.write(img, "PNG", buf);
		data = buf.toByteArray();
		buf = null;
	    }

	    void post() throws IOException {
		HttpURLConnection conn = (HttpURLConnection)Http.open(service.get().resolve("create").toURL());
		conn.setDoOutput(true);
		conn.setFixedLengthStreamingMode(data.length);
		conn.addRequestProperty("Content-Type", "image/png");
		conn.addRequestProperty("Authorization", auth(ui.sess));
		conn.connect();
		try(OutputStream out = conn.getOutputStream()) {
		    off = 0;
		    while(off < data.length) {
			int len = Math.min(data.length - off, 1024);
			out.write(data, off, len);
			off += len;
		    }
		}
		resp = ttoresp(conn);
	    }

	    void handle() throws IOException {
		if(Utils.eq(resp.get("status"), "error"))
		    throw(new IOException((String)resp.get("message")));
		synchronized(ui) {
		    reqdestroy();
		}
	    }

	    void restore() {
		prog.destroy();
		uploadbtn.show();
	    }

	    public void run() {
		try {
		    prepare();
		    post();
		    handle();
		} catch(IOException e) {
		    synchronized(ui) {
			ui.error(e.getMessage());
		    }
		} finally {
		    upload = null;
		    synchronized(ui) {
			restore();
		    }
		}
	    }

	    float prog() {
		if(data == null)
		    return(0);
		return((float)off / (float)data.length);
	    }

	    String text() {
		if(data == null) {
		    return("Preparing...");
		} else if(off == 0) {
		    return("Connection...");
		} else if(off < data.length) {
		    return(String.format("Sending (%.1f/%.1f kB)...", off * 0.001, data.length * 0.001));
		} else {
		    return("Processing...");
		}
	    }
	}

	public void upload() {
	    if(upload == null) {
		Defer.later(upload = new Upload(), null);
		uploadbtn.hide();
		prog = add(new Progress(csz().x).val(upload::prog).text(upload::text), uploadbtn.c);
		pack();
	    }
	}
    }

    private void arrange() {
	int n = 0;
	Coord pos = Coord.z;
	for(Image img : imgs) {
	    img.move(pos);
	    if((++n % 5) == 0)
		pos = img.pos("bl").adds(0, 10).x(0);
	    else
		pos = img.pos("ur").adds(10, 0);
	}
	if(adder != null)
	    adder.move(pos);
	pack();
    }

    private Image find(UID id) {
	for(Image img : imgs) {
	    if(Utils.eq(img.id, id))
		return(img);
	}
	return(null);
    }

    public void uimsg(String nm, Object... args) {
	if(nm == "add") {
	    UID id = (UID)args[0];
	    Image img = add(new Image(UI.scale(128, 128), id));
	    if(args.length > 1)
		img.setinfo(Utils.iv(args[1]), Utils.fv(args[2]));
	    imgs.add(img);
	    arrange();
	} else if(nm == "rem") {
	    Image img = find((UID)args[0]);
	    imgs.remove(img);
	    img.reqdestroy();
	    arrange();
	} else {
	    super.uimsg(nm, args);
	}
    }
}
