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

import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.util.*;

public class GItem extends AWidget {
    public Indir<Resource> res;
    public int meter = 0;
    public int num = -1;
    private Object[] rawinfo;
    private List<Info> info = Collections.emptyList();
    
    static {
	Widget.addtype("item", new WidgetFactory() {
		public Widget create(Coord c, Widget parent, Object[] args) {
		    int res = (Integer)args[0];
		    return(new GItem(parent, parent.ui.sess.getres(res)));
		}
	    });
    }
    
    @Resource.PublishedCode(name = "tt")
    public static interface InfoFactory {
	public Info build(GItem item, Object... args);
    }
    
    public abstract class Info {
	public GItem item() {
	    return(GItem.this);
	}
    }

    public interface ColorInfo {
	public Color olcol();
    }
    
    public interface NumberInfo {
	public int itemnum();
    }

    public abstract class Tip extends Info {
	public abstract BufferedImage longtip();
    }
    
    public class AdHoc extends Tip {
	public final Text str;
	
	public AdHoc(String str) {
	    this.str = Text.render(str);
	}
	
	public BufferedImage longtip() {
	    return(str.img);
	}
    }

    public class Name extends Tip {
	public final Text str;
	
	public Name(Text str) {
	    this.str = str;
	}
	
	public Name(String str) {
	    this(Text.render(str));
	}
	
	public BufferedImage longtip() {
	    return(str.img);
	}
    }
    
    public class Contents extends Tip {
	public final List<Info> sub;
	private final Text.Line ch = Text.render("Contents:");
	
	public Contents(List<Info> sub) {
	    this.sub = sub;
	}
	
	public BufferedImage longtip() {
	    BufferedImage stip = GItem.longtip(sub);
	    BufferedImage img = TexI.mkbuf(new Coord(stip.getWidth() + 10, stip.getHeight() + 15));
	    Graphics g = img.getGraphics();
	    g.drawImage(ch.img, 0, 0, null);
	    g.drawImage(stip, 10, 15, null);
	    g.dispose();
	    return(img);
	}
    }
    
    public class Amount extends Info implements NumberInfo {
	private final int num;
	
	public Amount(int num) {
	    this.num = num;
	}
	
	public int itemnum() {
	    return(num);
	}
    }
    
    public static BufferedImage catimgs(int margin, BufferedImage... imgs) {
	int w = 0, h = -margin;
	for(BufferedImage img : imgs) {
	    if(img.getWidth() > w)
		w = img.getWidth();
	    h += img.getHeight() + margin;
	}
	BufferedImage ret = TexI.mkbuf(new Coord(w, h));
	Graphics g = ret.getGraphics();
	int y = 0;
	for(BufferedImage img : imgs) {
	    g.drawImage(img, 0, y, null);
	    y += img.getHeight() + margin;
	}
	g.dispose();
	return(ret);
    }

    public static BufferedImage catimgsh(int margin, BufferedImage... imgs) {
	int w = -margin, h = 0;
	for(BufferedImage img : imgs) {
	    if(img.getHeight() > h)
		h = img.getHeight();
	    w += img.getWidth() + margin;
	}
	BufferedImage ret = TexI.mkbuf(new Coord(w, h));
	Graphics g = ret.getGraphics();
	int x = 0;
	for(BufferedImage img : imgs) {
	    g.drawImage(img, x, (h - img.getHeight()) / 2, null);
	    x += img.getWidth() + margin;
	}
	g.dispose();
	return(ret);
    }

    public static BufferedImage longtip(List<Info> info) {
	List<BufferedImage> buf = new ArrayList<BufferedImage>();
	for(Info ii : info) {
	    if(ii instanceof GItem.Tip) {
		GItem.Tip tip = (GItem.Tip)ii;
		buf.add(tip.longtip());
	    }
	}
	return(catimgs(0, buf.toArray(new BufferedImage[0])));
    }

    public GItem(Widget parent, Indir<Resource> res) {
	super(parent);
	this.res = res;
    }
    
    public List<Info> buildinfo(Object[] rawinfo) {
	List<Info> ret = new ArrayList<Info>();
	for(Object o : rawinfo) {
	    if(o instanceof Object[]) {
		Object[] a = (Object[])o;
		Resource ttres = ui.sess.getres((Integer)a[0]).get();
		InfoFactory f = ttres.layer(Resource.CodeEntry.class).get(InfoFactory.class);
		ret.add(f.build(this, a));
	    } else if(o instanceof String) {
		ret.add(new AdHoc((String)o));
	    } else {
		throw(new ClassCastException("Unexpected object type " + o.getClass() + " in item info array."));
	    }
	}
	return(ret);
    }

    public static <T> T find(Class<T> cl, List<Info> il) {
	for(Info inf : il) {
	    if(cl.isInstance(inf))
		return(cl.cast(inf));
	}
	return(null);
    }

    public List<Info> info() {
	if(info == null) {
	    info = buildinfo(rawinfo);
	}
	return(info);
    }
    
    private static String dump(Object arg) {
	if(arg instanceof Object[]) {
	    StringBuilder buf = new StringBuilder();
	    buf.append("[");
	    boolean f = true;
	    for(Object a : (Object[])arg) {
		if(!f)
		    buf.append(", ");
		buf.append(dump(a));
		f = false;
	    }
	    buf.append("]");
	    return(buf.toString());
	} else {
	    return(arg.toString());
	}
    }

    public void uimsg(String name, Object... args) {
	if(name == "num") {
	    num = (Integer)args[0];
	} else if(name == "chres") {
	    res = ui.sess.getres((Integer)args[0]);
	} else if(name == "tt") {
	    info = null;
	    rawinfo = args;
	} else if(name == "meter") {
	    meter = (Integer)args[0];
	}
    }
}
