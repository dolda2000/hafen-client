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
    private static final Map<Indir<Resource>, Image> cache = new WeakHashMap<>();
    public final Indir<Resource> res;
    private Image img;

    public GobIcon(Gob g, Indir<Resource> res) {
	super(g);
	this.res = res;
    }

    public static class Image {
	public final Resource res;
	public final Tex tex;
	public Coord cc;
	public boolean rot;
	public double ao;
	public int z;

	public Image(Resource res) {
	    this.res = res;
	    Resource.Image rimg = res.layer(Resource.imgc);
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
		tex = new TexI(buf);
	    }
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
    }

    public Image img() {
	if(this.img == null) {
	    synchronized(cache) {
		Image img = cache.get(res);
		if(img == null) {
		    img = new Image(res.get());
		    cache.put(res, img);
		}
		this.img = img;
	    }
	}
	return(this.img);
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
	public Resource.Spec res;
	public boolean show, defshow, notify;
	public String resns;
	public Path filens;
	public boolean mark, markset;

	public Setting(Resource.Spec res) {
	    this.res = res;
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
	    return(this.lres = this.res.loadsaved(Resource.remote()));
	}

	private int markdata() {
	    byte[] data = resource().flayer(Resource.imgc).kvdata.get("mm/mark");
	    if(data == null)
		return(0);
	    return(Utils.intvard(data, 0));
	}

	public boolean getmarkablep() {
	    return(markdata() != 0);
	}

	public boolean getmarkp() {
	    if(markset)
		return(mark);
	    return(markdata() == 2);
	}
    }

    public static class Settings implements Serializable {
	public static final byte[] sig = "Icons".getBytes(Utils.ascii);
	public Map<String, Setting> settings = new HashMap<>();
	public int tag = -1;
	public boolean notify = false;

	public Setting get(Resource.Named res) {
	    Setting ret = settings.get(res.name);
	    if((ret != null) && (ret.res.ver < res.ver))
		ret.res = new Resource.Spec(null, res.name, res.ver);
	    return(ret);
	}

	public Setting get(Resource res) {
	    return(get(res.indir()));
	}

	public void receive(int tag, Setting[] conf) {
	    Map<String, Setting> nset = new HashMap<>(settings);
	    for(int i = 0; i < conf.length; i++) {
		String nm = conf[i].res.name;
		Setting prev = nset.get(nm);
		if(prev == null)
		    nset.put(nm, conf[i]);
		else if(prev.res.ver < conf[i].res.ver)
		    prev.res = conf[i].res;
	    }
	    this.settings = nset;
	    this.tag = tag;
	}

	public void save(Message dst) {
	    Map<Object, Object> buf = new HashMap<>();
	    buf.put("tag", tag);
	    if(notify)
		buf.put("notify", 1);
	    List<Object> abuf = new ArrayList<>();
	    for(Setting set : settings.values()) {
		Map<Object, Object> sbuf = new HashMap<>();
		sbuf.put("res", new Object[] {set.res.name, set.res.ver});
		if(set.show)    sbuf.put("s", 1);
		if(set.defshow) sbuf.put("d", 1);
		if(set.notify)  sbuf.put("n", 1);
		if(set.markset) sbuf.put("m", set.mark ? 1 : 0);
		if(set.resns != null)  sbuf.put("R", set.resns);
		if(set.filens != null) sbuf.put("W", set.filens.toString());
		abuf.add(Utils.mapencn(sbuf));
	    }
	    buf.put("icons", abuf.toArray(new Object[0]));

	    dst.addbytes(sig);
	    dst.adduint8(3);
	    dst.addlist(Utils.mapencn(buf));
	}

	public static Settings loadold(Message buf) {
	    if(!Arrays.equals(buf.bytes(sig.length), sig))
		throw(new Message.FormatError("Invalid signature"));
	    int ver = buf.uint8();
	    if((ver < 1) || (ver > 2))
		throw(new Message.FormatError("Unknown version: " + ver));
	    Settings ret = new Settings();
	    ret.tag = buf.int32();
	    if(ver >= 2)
		ret.notify = (buf.uint8() != 0);
	    while(true) {
		String resnm = buf.string();
		if(resnm.equals(""))
		    break;
		int resver = buf.uint16();
		Resource.Spec res = new Resource.Spec(null, resnm, resver);
		Setting set = new Setting(res);
		boolean setdef = false;
		data: while(true) {
		    int datum = buf.uint8();
		    switch(datum) {
		    case (int)'s':
			set.show = (buf.uint8() != 0);
			break;
		    case (int)'d':
			set.defshow = (buf.uint8() != 0);
			setdef = true;
			break;
		    case (int)'n':
			set.notify = (buf.uint8() != 0);
			break;
		    case (int)'R':
			set.resns = buf.string();
			break;
		    case (int)'W':
			try {
			    set.filens = Utils.path(buf.string());
			} catch(RuntimeException e) {
			    new Warning(e, "could not read path").issue();
			}
			break;
		    case 0:
			break data;
		    default:
			throw(new Message.FormatError("Unknown datum: " + datum));
		    }
		}
		if(!setdef)
		    set.defshow = set.show;
		ret.settings.put(res.name, set);
	    }
	    return(ret);
	}

	public static Settings load(Message blob) {
	    if(!Arrays.equals(blob.bytes(sig.length), sig))
		throw(new Message.FormatError("Invalid signature"));
	    int ver = blob.uint8();
	    if((ver < 3) || (ver > 3))
		throw(new Message.FormatError("Unknown version: " + ver));
	    Settings ret = new Settings();
	    Map<Object, Object> root = Utils.mapdecn(blob.tto());
	    ret.tag = Utils.iv(root.get("tag"));
	    ret.notify = Utils.bv(root.getOrDefault("notify", 0));
	    for(Object eicon : (Object[])root.get("icons")) {
		Map<Object, Object> icon = Utils.mapdecn(eicon);
		Object[] eres = (Object[])icon.get("res");
		Resource.Spec res = new Resource.Spec(null, (String)eres[0], Utils.iv(eres[1]));
		Setting set = new Setting(res);
		set.show    = Utils.bv(icon.getOrDefault("s", 0));
		set.defshow = Utils.bv(icon.getOrDefault("d", 0));
		set.notify  = Utils.bv(icon.getOrDefault("n", 0));
		set.resns   = (String)icon.getOrDefault("R", null);
		if(icon.containsKey("m")) {
		    set.markset = true;
		    set.mark = Utils.bv(icon.get("m"));
		}
		try {
		    set.filens = Utils.path((String)icon.getOrDefault("W", null));
		} catch(RuntimeException e) {
		    new Warning(e, "could not read path").issue();
		}
		ret.settings.put(res.name, set);
	    }
	    return(ret);
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
	private final Runnable save;
	private final PackCont.LinPack cont;
	private final IconList list;
	private Widget setbox;

	public static class Icon {
	    public final Setting conf;
	    public String name;

	    public Icon(Setting conf) {this.conf = conf;}
	}

	private <T> Consumer<T> andsave(Consumer<T> main) {
	    return(val -> {main.accept(val); if(save != null) save.run();});
	}

	private static final Text.Foundry elf = CharWnd.attrf;
	private static final int elh = elf.height() + UI.scale(2);
	public class IconList extends SSearchBox<Icon, IconList.IconLine> {
	    private List<Icon> ordered = Collections.emptyList();
	    private Map<String, Setting> cur = null;
	    private boolean reorder = false;

	    private IconList(Coord sz) {
		super(sz, elh);
	    }

	    public class IconLine extends SListWidget.ItemWidget<Icon> {
		public IconLine(Coord sz, Icon icon) {
		    super(IconList.this, sz, icon);
		    Widget prev;
		    prev = adda(new CheckBox("").state(() -> icon.conf.notify).set(andsave(val -> icon.conf.notify = val)).settip("Notify"),
				sz.x - UI.scale(2) - (sz.y / 2), sz.y / 2, 0.5, 0.5);
		    prev = adda(new CheckBox("").state(() -> icon.conf.show).set(andsave(val -> icon.conf.show = val)).settip("Display"),
				prev.c.x - UI.scale(2) - (sz.y / 2), sz.y / 2, 0.5, 0.5);
		    add(SListWidget.IconText.of(Coord.of(prev.c.x - UI.scale(2), sz.y), () -> item.conf.resource()), Coord.z);
		}
	    }

	    protected boolean searchmatch(Icon icon, String text) {
		return((icon.name != null) &&
		       (icon.name.toLowerCase().indexOf(text.toLowerCase()) >= 0));
	    }
	    protected List<Icon> allitems() {return(ordered);}
	    protected IconLine makeitem(Icon icon, int idx, Coord sz) {return(new IconLine(sz, icon));}

	    public void tick(double dt) {
		Map<String, Setting> cur = this.cur;
		if(cur != conf.settings) {
		    cur = conf.settings;
		    ArrayList<Icon> ordered = new ArrayList<>(cur.size());
		    for(Setting conf : cur.values())
			ordered.add(new Icon(conf));
		    this.cur = cur;
		    this.ordered = ordered;
		    reorder = true;
		}
		if(reorder) {
		    reorder = false;
		    for(Icon icon : ordered) {
			if(icon.name == null) {
			    try {
				Resource.Tooltip name = icon.conf.resource().layer(Resource.tooltip);
				icon.name = (name == null) ? "???" : name.t;
			    } catch(Loading l) {
				reorder = true;
			    }
			}
		    }
		    Collections.sort(ordered, (a, b) -> {
			    if((a.name == null) && (b.name == null))
				return(0);
			    if(a.name == null)
				return(1);
			    if(b.name == null)
				return(-1);
			    return(a.name.compareTo(b.name));
			});
		}
		super.tick(dt);
	    }

	    public boolean keydown(java.awt.event.KeyEvent ev) {
		if(ev.getKeyCode() == java.awt.event.KeyEvent.VK_SPACE) {
		    if(sel != null) {
			sel.conf.show = !sel.conf.show;
			if(save != null)
			    save.run();
		    }
		    return(true);
		}
		return(super.keydown(ev));
	    }

	    public void change(Icon icon) {
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
			if(save != null)
			    save.run();
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

	public SettingsWindow(Settings conf, Runnable save) {
	    super(Coord.z, "Icon settings");
	    this.conf = conf;
	    this.save = save;
	    add(this.cont = new PackCont.LinPack.VPack(), Coord.z).margin(UI.scale(5)).packpar(true);
	    list = cont.last(new IconList(UI.scale(250, 500)), 0);
	    cont.last(new HRuler(list.sz.x), 0);
	    cont.last(new CheckBox("Notification on newly seen icons") {
		    {this.a = conf.notify;}

		    public void changed(boolean val) {
			conf.notify = val;
			if(save != null)
			    save.run();
		    }
		}, UI.scale(5));
	    cont.pack();
	}
    }

    @OCache.DeltaType(OCache.OD_ICON)
    public static class $icon implements OCache.Delta {
	public void apply(Gob g, OCache.AttrDelta msg) {
	    int resid = msg.uint16();
	    Indir<Resource> res;
	    if(resid == 65535) {
		g.delattr(GobIcon.class);
	    } else {
		int ifl = msg.uint8();
		g.setattr(new GobIcon(g, OCache.Delta.getres(g, resid)));
	    }
	}
    }
}
