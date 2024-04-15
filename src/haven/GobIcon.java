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
import java.util.function.*;
import java.io.*;
import java.nio.file.*;
import java.awt.image.*;
import java.awt.Color;
import javax.swing.JFileChooser;
import javax.swing.filechooser.*;

public class GobIcon extends GAttrib {
    private static final int size = UI.scale(20);
    public static final PUtils.Convolution filter = new PUtils.Hanning(1);
    public final Indir<Resource> res;
    public final byte[] sdt;
    private Icon icon;

    public GobIcon(Gob g, Indir<Resource> res, byte[] sdt) {
	super(g);
	this.res = res;
	this.sdt = sdt;
    }

    public static abstract class Icon {
	public static final Object[] nilid = new Object[0];
	public final OwnerContext owner;
	public final Resource res;

	public Icon(OwnerContext owner, Resource res) {
	    this.owner = owner;
	    this.res = res;
	}

	public static enum Markable {
	    UNMARKABLE, NONDEFAULT, DEFAULT
	}

	public abstract String name();
	public abstract BufferedImage image();
	public abstract void draw(GOut g, Coord cc);
	public abstract boolean checkhit(Coord c);
	public Object[] id() {return(nilid);}
	public int z() {return(0);}
	public Markable markable() {return(Markable.UNMARKABLE);}

	@Resource.PublishedCode(name = "mapicon")
	public static interface Factory {
	    public Icon create(OwnerContext owner, Resource res, Message sdt);
	    public Collection<? extends Icon> enumerate(OwnerContext owner, Resource res, Message sdt);
	}
    }

    public static class Image {
	private static final Map<Resource, Image> cache = new WeakHashMap<>();
	public final BufferedImage img;
	public final Tex tex;
	public Coord cc;
	public boolean rot;
	public double ao;
	public int z;

	public Image(Resource res) {
	    Resource.Image rimg = res.layer(Resource.imgc);
	    BufferedImage img = rimg.scaled();
	    Tex tex = rimg.tex();
	    if ((tex.sz().x > size) || (tex.sz().y > size)) {
		BufferedImage buf = rimg.img;
		buf = PUtils.rasterimg(PUtils.blurmask2(buf.getRaster(), 1, 1, Color.BLACK));
		Coord tsz;
		if(buf.getWidth() > buf.getHeight())
		    tsz = new Coord(size, (size * buf.getHeight()) / buf.getWidth());
		else
		    tsz = new Coord((size * buf.getWidth()) / buf.getHeight(), size);
		buf = PUtils.convolve(buf, tsz, filter);
		tex = new TexI(img = buf);
	    }
	    this.img = img;
	    this.tex = tex;
	    this.cc = tex.sz().div(2);
	    byte[] data = rimg.kvdata.get("mm/rot");
	    if(data != null) {
		this.rot = true;
		this.ao = Utils.float32d(data, 0) * (Math.PI / 180f);
	    }
	    this.z = rimg.z;
	    data = rimg.kvdata.get("mm/z");
	    if(data != null)
		this.z = Utils.intvard(data, 0);
	}

	public static Image get(Resource res) {
	    synchronized(cache) {
		Image img = cache.get(res);
		if(img == null) {
		    img = new Image(res);
		    cache.put(res, img);
		}
		return(img);
	    }
	}
    }

    public static class ImageIcon extends Icon {
	public final Image img;
	private final Gob gob = owner.fcontext(Gob.class, false);

	public ImageIcon(OwnerContext owner, Resource res, Image img) {
	    super(owner, res);
	    this.img = img;
	}

	public String name() {
	    Resource.Tooltip name = res.layer(Resource.tooltip);
	    return(name == null ? "???" : name.t);
	}

	public BufferedImage image() {
	    return(res.flayer(Resource.imgc).img);
	}

	public void draw(GOut g, Coord cc) {
	    if(!img.rot)
		g.image(img.tex, cc.sub(img.cc));
	    else
		g.rotimage(img.tex, cc, img.cc, ((gob == null) ? 0 : -gob.a) + img.ao);
	}

	public boolean checkhit(Coord c) {
	    Coord oc = c.add(img.cc);
	    if(!oc.isect(Coord.z, PUtils.imgsz(img.img)))
		return(false);
	    if(img.img.getRaster().getNumBands() < 4)
		return(true);
	    return(img.img.getRaster().getSample(oc.x, oc.y, 3) >= 128);
	}

	public int z() {
	    return(img.z);
	}

	private int markdata() {
	    byte[] data = res.flayer(Resource.imgc).kvdata.get("mm/mark");
	    if(data == null)
		return(0);
	    return(Utils.intvard(data, 0));
	}

	public Markable markable() {
	    switch(markdata()) {
	    case 1:
		return(Markable.NONDEFAULT);
	    case 2:
		return(Markable.DEFAULT);
	    default:
		return(Markable.UNMARKABLE);
	    }
	}

	public static final Factory factory = new Factory() {
		public ImageIcon create(OwnerContext owner, Resource res, Message sdt) {
		    return(new ImageIcon(owner, res, Image.get(res)));
		}

		public Collection<ImageIcon> enumerate(OwnerContext owner, Resource res, Message sdt) {
		    return(Collections.singletonList(new ImageIcon(owner, res, Image.get(res))));
		}
	    };
    }

    public static Icon.Factory getfac(Resource res) {
	Icon.Factory fac = res.getcode(Icon.Factory.class, false);
	if(fac != null)
	    return(fac);
	return(ImageIcon.factory);
    }

    public Icon icon() {
	if(this.icon == null) {
	    Resource res = this.res.get();
	    this.icon = getfac(res).create(gob, res, new MessageBuf(sdt));
	}
	return(this.icon);
    }

    private static Consumer<UI> resnotif(String nm) {
	return(ui -> {
		Indir<Resource> resid = Resource.local().load(nm);
		ui.sess.glob.loader.defer(() -> {
			Resource res;
			try {
			    res = resid.get();
			} catch(Loading l) {
			    throw(l);
			} catch(RuntimeException e) {
			    ui.error("Could not play " + nm);
			    return;
			}
			Audio.CS clip = Audio.fromres(res);
			ui.sfx(clip);
		    }, null);
	    });
    }

    private static Consumer<UI> wavnotif(Path path) {
	return(ui -> {
		ui.sess.glob.loader.defer(() -> {
			Audio.CS clip;
			InputStream fail = null;
			try {
			    fail = Files.newInputStream(path);
			    clip = Audio.PCMClip.fromwav(new BufferedInputStream(fail));
			    fail = null;
			} catch(IOException e) {
			    String msg = e.getMessage();
			    if(e instanceof FileSystemException)
				msg = "Could not open file";
			    ui.error("Could not play " + path + ": " + msg);
			    return;
			} finally {
			    if(fail != null) {
				try {
				    fail.close();
				} catch(IOException e) {
				    new Warning(e, "unexpected error on close").issue();
				}
			    }
			}
			ui.sfx(clip);
		    }, null);
	    });
    }

    private static final Map<Object, Double> lastnotifs = new HashMap<>();
    private static Consumer<UI> notiflimit(Consumer<UI> bk, Object id) {
	return(ui -> {
		double now = Utils.rtime();
		synchronized(lastnotifs) {
		    Double last = lastnotifs.get(id);
		    if((last != null) && (now - last < 0.5))
			return;
		    lastnotifs.put(id, now);
		}
		bk.accept(ui);
	    });
    }

    public static class Setting implements Serializable {
	public final ID id;
	public final Icon icon;
	public final Settings.ResID from;
	public Resource.Saved res;
	public boolean show, defshow, notify;
	public String resns;
	public Path filens;
	public boolean mark, markset;

	public static class ID {
	    public final String res;
	    public final Object[] sub;

	    public ID(String res, Object[] sub) {
		this.res = res;
		this.sub = sub;
	    }

	    public int hashCode() {
		return((res.hashCode() * 31) + Arrays.deepHashCode(sub));
	    }

	    public boolean equals(ID that) {
		return(this.res.equals(that.res) && Arrays.deepEquals(this.sub, that.sub));
	    }

	    public boolean equals(Object x) {
		return((x instanceof ID) && equals((ID)x));
	    }
	}

	public Setting(Resource.Saved res, Object[] id, Icon icon, Settings.ResID from) {
	    this.res = res;
	    this.id = new ID(res.name, id);
	    this.icon = icon;
	    this.from = from;
	}

	public Setting(Resource.Saved res, Object[] id) {
	    this(res, id, null, null);
	}

	public Setting(Icon icon, Settings.ResID from) {
	    this(new Resource.Saved(Resource.remote(), icon.res.name, icon.res.ver), icon.id(), icon, from);
	}

	public Consumer<UI> notification() {
	    if(resns != null)
		return(notiflimit(resnotif(resns), resns));
	    if(filens != null)
		return(notiflimit(wavnotif(filens), filens));
	    return(null);
	}

	private Resource lres;
	public Resource resource() {
	    if(this.lres != null)
		return(this.lres);
	    return(this.lres = this.res.get());
	}

	public boolean getmarkablep() {
	    return(icon.markable() != Icon.Markable.UNMARKABLE);
	}

	public boolean getmarkp() {
	    if(markset)
		return(mark);
	    return(icon.markable() == Icon.Markable.DEFAULT);
	}
    }

    public static class Settings implements OwnerContext, Serializable {
	public static final byte[] sig = "Icons".getBytes(Utils.ascii);
	public final UI ui;
	public final String filename;
	public Map<Setting.ID, Setting> settings = new HashMap<>();
	public int tag = -1;
	public boolean notify = false;

	public Settings(UI ui, String filename) {
	    this.ui = ui;
	    this.filename = filename;
	}

	public Setting get(Icon icon) {
	    return(settings.get(new Setting.ID(icon.res.name, icon.id())));
	}

	public static class ResID {
	    public final Resource.Saved res;
	    public final byte[] data;

	    public ResID(Resource.Saved res, byte[] data) {
		this.res = res;
		this.data = data;
	    }

	    public int hashCode() {
		return((res.name.hashCode() * 31) + Arrays.hashCode(data));
	    }

	    public boolean equals(ResID that) {
		return(this.res.name.equals(that.res.name) && Arrays.equals(this.data, that.data));
	    }

	    public boolean equals(Object x) {
		return((x instanceof ResID) && equals((ResID)x));
	    }
	}

	private Loader loading = null;
	public class Loader implements Runnable {
	    public final Queue<ResID> load = new ArrayDeque<>();
	    public final Map<ResID, Setting> defaults = new HashMap<>();
	    public final Map<ResID, Collection<Setting>> resolve = new HashMap<>();
	    public boolean save = false, adv = false;
	    public Integer tag = null;
	    private final Collection<Icon> advbuf = new ArrayList<>();
	    private ResID r = null;
	    private Loader next = null;
	    private Map<Setting.ID, Setting> nset = null;

	    private void merge(Setting set, Setting conf) {
		set.show    = conf.show;
		set.defshow = conf.defshow;
		set.notify  = conf.notify;
		set.resns   = conf.resns;
		set.filens  = conf.filens;
		if(set.markset = conf.markset)
		    set.mark = conf.mark;
	    }

	    public void run() {
		if(nset == null)
		    nset = new HashMap<>(settings);
		while(true) {
		    if((r == null) && ((r = load.poll()) == null))
			break;
		    Resource res;
		    try {
			res = r.res.get(-10);
		    } catch(Resource.NoSuchResourceException e) {
			r = null;
			continue;
		    }
		    Icon.Factory fac = getfac(res);
		    for(Icon icon : fac.enumerate(Settings.this, res, new MessageBuf(r.data))) {
			Setting set = new Setting(icon, r);
			Setting def = defaults.get(r);
			if(def != null)
			    merge(set, def);
			Setting prev = nset.get(set.id);
			if((prev == null) || (prev.res.ver < set.res.ver)) {
			    if(prev != null)
				merge(set, prev);
			    else
				advbuf.add(icon);
			    nset.put(set.id, set);
			}
		    }
		    Collection<Setting> sets = resolve.remove(r);
		    if(sets != null) {
			for(Setting conf : sets) {
			    Setting set = nset.get(conf.id);
			    if(set != null)
				merge(set, conf);
			}
		    }
		    r = null;
		}
		settings = nset;
		if(tag != null)
		    Settings.this.tag = tag;
		if(save)
		    save();
		if(adv && notify) {
		    Set<String> names = new HashSet<>();
		    advbuf.forEach(icon -> names.add(icon.name()));
		    synchronized(ui) {
			for(String nm : names)
			    ui.msg(String.format("%s added to list of seen icons.", nm));
		    }
		}
		synchronized(Settings.this) {
		    if(loading == this)
			loading = null;
		    else
			ui.loader.defer(next, null);
		}
	    }

	    public void submit() {
		synchronized(Settings.this) {
		    if(loading == null)
			ui.loader.defer(this, null);
		    else
			loading.next = this;
		    loading = this;
		}
	    }
	}

	private final ClassResolver<Settings> ctxr = new ClassResolver<Settings>()
	    .add(Glob.class, s -> s.ui.sess.glob)
	    .add(Session.class, s -> s.ui.sess);
	public <T> T context(Class<T> cl) {return(ctxr.context(cl, this));}

	public void receive(Object[] args) {
	    int tag = Utils.iv(args[0]);
	    if(args[1] instanceof String) {
		int a = 1;
		Resource.Saved res = new Resource.Saved(Resource.remote(), (String)args[a++], Utils.iv(args[a++]));
		byte[] data = (args[a] instanceof byte[]) ? (byte[])args[a++] : new byte[0];
		ResID id = new ResID(res, data);
		Setting def = new Setting(res, Icon.nilid);
		def.show = def.defshow = Utils.bv(args[a++]);
		Loader l = new Loader();
		l.save = true;
		l.adv = true;
		l.tag = tag;
		l.load.add(id);
		l.defaults.put(id, def);
		l.submit();
	    } else if(args[1] instanceof Object[]) {
		Object[] sub = (Object[])args[1];
		int a = 0;
		Loader l = new Loader();
		l.save = true;
		l.tag = tag;
		Collection<GobIcon.Setting> csets = new ArrayList<>();
		while(a < sub.length) {
		    Resource.Saved res = new Resource.Saved(Resource.remote(), (String)sub[a++], Utils.iv(sub[a++]));
		    byte[] data = (sub[a] instanceof byte[]) ? (byte[])sub[a++] : new byte[0];
		    int fl = Utils.iv(sub[a++]);
		    ResID id = new ResID(res, data);
		    Setting def = new Setting(res, Icon.nilid);
		    def.show = def.defshow = ((fl & 1) != 0);
		    l.load.add(id);
		    l.defaults.put(id, def);
		}
		l.submit();
	    }
	}

	private static void encodeset(Map<Object, Object> buf, Setting set) {
	    if(set.show)    buf.put("s", 1);
	    if(set.defshow) buf.put("d", 1);
	    if(set.notify)  buf.put("n", 1);
	    if(set.markset) buf.put("m", set.mark ? 1 : 0);
	    if(set.resns != null)  buf.put("R", set.resns);
	    if(set.filens != null) buf.put("W", set.filens.toString());
	}

	public void save(Message dst) {
	    Map<ResID, Collection<Setting>> byid = new HashMap<>();
	    for(Setting set : settings.values())
		byid.computeIfAbsent(set.from, k -> new ArrayList<>()).add(set);
	    Map<Object, Object> buf = new HashMap<>();
	    buf.put("tag", tag);
	    if(notify)
		buf.put("notify", 1);
	    List<Object> abuf = new ArrayList<>();
	    for(Map.Entry<ResID, Collection<Setting>> ent : byid.entrySet()) {
		ResID id = ent.getKey();
		Map<Object, Object> rbuf = new HashMap<>();
		if(id.data.length == 0)
		    rbuf.put("res", new Object[] {id.res.name, id.res.savever()});
		else
		    rbuf.put("res", new Object[] {id.res.name, id.res.savever(), id.data});
		Collection<Object> sub = new ArrayList<>();
		for(Setting set : ent.getValue()) {
		    if(set.id.sub.length == 0) {
			encodeset(rbuf, set);
		    } else {
			Map<Object, Object> sbuf = new HashMap<>();
			sbuf.put("id", set.id.sub);
			encodeset(sbuf, set);
			sub.add(Utils.mapencn(sbuf));
		    }
		}
		if(!sub.isEmpty())
		    rbuf.put("sub", sub.toArray(new Object[0]));
		abuf.add(Utils.mapencn(rbuf));
	    }
	    buf.put("icons", abuf.toArray(new Object[0]));

	    dst.addbytes(sig);
	    dst.adduint8(3);
	    dst.addlist(Utils.mapencn(buf));
	}

	private static void parseset(Setting set, Map<Object, Object> data) {
	    set.show    = Utils.bv(data.getOrDefault("s", 0));
	    set.defshow = Utils.bv(data.getOrDefault("d", 0));
	    set.notify  = Utils.bv(data.getOrDefault("n", 0));
	    set.resns   = (String)data.getOrDefault("R", null);
	    if(data.containsKey("m")) {
		set.markset = true;
		set.mark = Utils.bv(data.get("m"));
	    }
	    try {
		set.filens = Utils.path((String)data.getOrDefault("W", null));
	    } catch(RuntimeException e) {
		new Warning(e, "could not read path").issue();
	    }
	}

	public void load(Message blob) {
	    if(!Arrays.equals(blob.bytes(sig.length), sig))
		throw(new Message.FormatError("Invalid signature"));
	    int ver = blob.uint8();
	    if((ver < 3) || (ver > 3))
		throw(new Message.FormatError("Unknown version: " + ver));
	    Map<Object, Object> root = Utils.mapdecn(blob.tto());
	    this.tag = Utils.iv(root.get("tag"));
	    this.notify = Utils.bv(root.getOrDefault("notify", 0));
	    Loader l = new Loader();
	    for(Object eicon : (Object[])root.get("icons")) {
		Map<Object, Object> icon = Utils.mapdecn(eicon);
		Object[] eres = (Object[])icon.get("res");
		ResID res = new ResID(new Resource.Saved(Resource.remote(), (String)eres[0], Utils.iv(eres[1])),
				      (eres.length > 2) ? (byte[])eres[2] : new byte[0]);
		Collection<Setting> sets = new ArrayList<>();
		Setting set = new Setting(res.res, Icon.nilid);
		parseset(set, icon);
		sets.add(set);
		if(icon.containsKey("sub")) {
		    for(Object esub : (Object[])icon.get("sub")) {
			Map<Object, Object> sub = Utils.mapdecn(esub);
			set = new Setting(res.res, (Object[])sub.get("id"));
			parseset(set, sub);
			sets.add(set);
		    }
		}
		l.load.add(res);
		l.resolve.put(res, sets);
	    }
	    l.submit();
	}

	public void save() {
	    if(ResCache.global == null)
		return;
	    try(StreamMessage fp = new StreamMessage(ResCache.global.store(filename))) {
		save(fp);
	    } catch(Exception e) {
		new Warning(e, "failed to store icon-conf").issue();
	    }
	}

	private boolean saveagain = false, saving = false;
	private void dsave0() {
	    save();
	    synchronized(this) {
		if(saveagain) {
		    Defer.later(this::dsave0, null);
		    saveagain = false;
		} else {
		    saving = false;
		}
	    }
	}

	public void dsave() {
	    synchronized(this) {
		if(!saving) {
		    Defer.later(this::dsave0, null);
		    saving = true;
		} else {
		    saveagain = true;
		}
	    }
	}

	public static Settings load(UI ui, String name) throws IOException {
	    if(ResCache.global == null)
		return(new Settings(ui, name));
	    try(StreamMessage fp = new StreamMessage(ResCache.global.fetch(name))) {
		Settings ret = new Settings(ui, name);
		ret.load(fp);
		return(ret);
	    } catch(FileNotFoundException e) {
	    }
	    return(new Settings(ui, name));
	}
    }

    public static class NotificationSetting {
	public final String name, res;
	public final Path wav;

	private NotificationSetting(String name, String res, Path wav) {this.name = name; this.res = res; this.wav = wav;}
	public NotificationSetting(String name, String res) {this(name, res, null);}
	public NotificationSetting(String name, Path wav)   {this(name, null, wav);}
	public NotificationSetting(Path wav) {this(wav.getFileName().toString(), wav);}

	public boolean act(Setting conf) {
	    return(Utils.eq(conf.resns, this.res) && Utils.eq(conf.filens, wav));
	}

	public static final NotificationSetting nil = new NotificationSetting("None", null, null);
	public static final NotificationSetting other = new NotificationSetting("Select file...", null, null);
	public static final List<NotificationSetting> builtin;

	static {
	    List<NotificationSetting> buf = new ArrayList<>();
	    buf.add(new NotificationSetting("Bell 1", "sfx/hud/mmap/bell1"));
	    buf.add(new NotificationSetting("Bell 2", "sfx/hud/mmap/bell2"));
	    buf.add(new NotificationSetting("Bell 3", "sfx/hud/mmap/bell3"));
	    buf.add(new NotificationSetting("Wood 1", "sfx/hud/mmap/wood1"));
	    buf.add(new NotificationSetting("Wood 2", "sfx/hud/mmap/wood2"));
	    buf.add(new NotificationSetting("Wood 3", "sfx/hud/mmap/wood3"));
	    buf.add(new NotificationSetting("Wood 4", "sfx/hud/mmap/wood4"));
	    builtin = buf;
	}
    }

    public static class SettingsWindow extends Window {
	public final Settings conf;
	private final PackCont.LinPack cont;
	private final IconList list;
	private Widget setbox;

	public static class ListIcon {
	    public final Setting conf;
	    public final String name;
	    public final Object[] id;

	    public ListIcon(Setting conf) {
		this.conf = conf;
		this.name = conf.icon.name();
		this.id = conf.icon.id();
	    }
	}

	private <T> Consumer<T> andsave(Consumer<T> main) {
	    return(val -> {main.accept(val); conf.dsave();});
	}

	private static final Text.Foundry elf = CharWnd.attrf;
	private static final int elh = elf.height() + UI.scale(2);
	public class IconList extends SSearchBox<ListIcon, IconList.IconLine> {
	    private List<ListIcon> ordered = Collections.emptyList();
	    private Map<Setting.ID, Setting> cur = null;

	    private IconList(Coord sz) {
		super(sz, elh);
	    }

	    public class IconLine extends SListWidget.ItemWidget<ListIcon> {
		public IconLine(Coord sz, ListIcon icon) {
		    super(IconList.this, sz, icon);
		    Widget prev;
		    prev = adda(new CheckBox("").state(() -> icon.conf.notify).set(andsave(val -> icon.conf.notify = val)).settip("Notify"),
				sz.x - UI.scale(2) - (sz.y / 2), sz.y / 2, 0.5, 0.5);
		    prev = adda(new CheckBox("").state(() -> icon.conf.show).set(andsave(val -> icon.conf.show = val)).settip("Display"),
				prev.c.x - UI.scale(2) - (sz.y / 2), sz.y / 2, 0.5, 0.5);
		    add(SListWidget.IconText.of(Coord.of(prev.c.x - UI.scale(2), sz.y), item.conf.icon::image, item.conf.icon::name), Coord.z);
		}
	    }

	    protected boolean searchmatch(ListIcon icon, String text) {
		return((icon.name != null) &&
		       (icon.name.toLowerCase().indexOf(text.toLowerCase()) >= 0));
	    }
	    protected List<ListIcon> allitems() {return(ordered);}
	    protected IconLine makeitem(ListIcon icon, int idx, Coord sz) {return(new IconLine(sz, icon));}

	    public void tick(double dt) {
		Map<Setting.ID, Setting> cur = this.cur;
		if(cur != conf.settings) {
		    cur = conf.settings;
		    ArrayList<ListIcon> ordered = new ArrayList<>(cur.size());
		    for(Setting conf : cur.values())
			ordered.add(new ListIcon(conf));
		    this.cur = cur;
		    this.ordered = ordered;
		    Collections.sort(ordered, (a, b) -> {
			    int c;;
			    if((c = a.name.compareTo(b.name)) != 0)
				return(c);
			    if((c = Utils.compare(a.id, b.id)) != 0)
				return(c);
			    return(0);
			});
		}
		super.tick(dt);
	    }

	    public boolean keydown(java.awt.event.KeyEvent ev) {
		if(ev.getKeyCode() == java.awt.event.KeyEvent.VK_SPACE) {
		    if(sel != null) {
			sel.conf.show = !sel.conf.show;
			conf.dsave();
		    }
		    return(true);
		}
		return(super.keydown(ev));
	    }

	    public void change(ListIcon icon) {
		super.change(icon);
		if(setbox != null) {
		    setbox.destroy();
		    setbox = null;
		}
		if(icon != null) {
		    setbox = cont.after(new IconSettings(sz.x - UI.scale(10), icon.conf), list, UI.scale(5));
		}
	    }
	}

	public class IconSettings extends Widget {
	    public final Setting conf;
	    public final NotifBox nb;

	    public IconSettings(int w, Setting conf) {
		super(Coord.z);
		this.conf = conf;
		Widget prev = add(new CheckBox("Display").state(() -> conf.show).set(andsave(val -> conf.show = val)),
				  0, 0);
		add(new CheckBox("Notify").state(() -> conf.notify).set(andsave(val -> conf.notify = val)),
		    w / 2, 0);
		Button pb = new Button(UI.scale(50), "Play") {
			protected void depress() {}
			protected void unpress() {}
			public void click() {play();}
		    };
		prev = add(new Label("Sound to play on notification:"), prev.pos("bl").adds(0, 5));
		nb = new NotifBox(w - pb.sz.x - UI.scale(15));
		addhl(prev.pos("bl").adds(0, 2), w, prev = Frame.with(nb, false), pb);
		if(conf.getmarkablep()) {
		    add(new CheckBox("Place permanent marker")
			.state(() -> conf.markset ? conf.mark : conf.getmarkp())
			.set(andsave(val -> {conf.markset = true; conf.mark = val;})),
			prev.pos("bl").adds(0, 5));
		}
		pack();
	    }

	    public class NotifBox extends SDropBox<NotificationSetting, Widget> {
		private final List<NotificationSetting> items = new ArrayList<>();

		public NotifBox(int w) {
		    super(w, UI.scale(160), UI.scale(20));
		    items.add(NotificationSetting.nil);
		    for(NotificationSetting notif : NotificationSetting.builtin)
			items.add(notif);
		    if(conf.filens != null)
			items.add(new NotificationSetting(conf.filens));
		    items.add(NotificationSetting.other);
		    for(NotificationSetting item : items) {
			if(item.act(conf)) {
			    change(item);
			    break;
			}
		    }
		}

		protected List<NotificationSetting> items() {return(items);}
		protected Widget makeitem(NotificationSetting item, int idx, Coord sz) {return(SListWidget.TextItem.of(sz, Text.std, () -> item.name));}

		private void selectwav() {
		    java.awt.EventQueue.invokeLater(() -> {
			    JFileChooser fc = new JFileChooser();
			    fc.setFileFilter(new FileNameExtensionFilter("PCM wave file", "wav"));
			    if(fc.showOpenDialog(null) != JFileChooser.APPROVE_OPTION)
				return;
			    for(Iterator<NotificationSetting> i = items.iterator(); i.hasNext();) {
				NotificationSetting item = i.next();
				if(item.wav != null)
				    i.remove();
			    }
			    NotificationSetting ws = new NotificationSetting(fc.getSelectedFile().toPath());
			    items.add(items.indexOf(NotificationSetting.other), ws);
			    change(ws);
			});
		}

		public void change(NotificationSetting item) {
		    super.change(item);
		    if(item == NotificationSetting.other) {
			selectwav();
		    } else {
			conf.resns = item.res;
			conf.filens = item.wav;
			SettingsWindow.this.conf.dsave();
		    }
		}
	    }

	    private void play() {
		NotificationSetting sel = nb.sel;
		if(sel == null) sel = NotificationSetting.nil;
		if(sel.res != null)
		    resnotif(sel.res).accept(ui);
		else if(sel.wav != null)
		    wavnotif(sel.wav).accept(ui);
	    }
	}

	public SettingsWindow(Settings conf) {
	    super(Coord.z, "Icon settings");
	    this.conf = conf;
	    add(this.cont = new PackCont.LinPack.VPack(), Coord.z).margin(UI.scale(5)).packpar(true);
	    list = cont.last(new IconList(UI.scale(250, 500)), 0);
	    cont.last(new HRuler(list.sz.x), 0);
	    cont.last(new CheckBox("Notification on newly seen icons") {
		    {this.a = conf.notify;}

		    public void changed(boolean val) {
			conf.notify = val;
			conf.dsave();
		    }
		}, UI.scale(5));
	    cont.pack();
	}
    }

    @OCache.DeltaType(OCache.OD_ICON)
    public static class $icon implements OCache.Delta {
	public void apply(Gob g, OCache.AttrDelta msg) {
	    int resid = msg.uint16();
	    if(resid == 65535) {
		g.delattr(GobIcon.class);
	    } else {
		Indir<Resource> res = OCache.Delta.getres(g, resid);
		int ifl = msg.uint8();
		byte[] sdt = msg.bytes();
		GobIcon cur = g.getattr(GobIcon.class);
		if((cur == null) || (cur.res != res) || !Arrays.equals(cur.sdt, sdt))
		    g.setattr(new GobIcon(g, OCache.Delta.getres(g, resid), sdt));
	    }
	}
    }
}
