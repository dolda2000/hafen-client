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

import java.util.function.*;
import java.awt.Color;

public class Progress extends Widget {
    public static final int defh = UI.scale(20);
    private static final int m = UI.scale(1);
    public float a;
    private Supplier<Float> val;
    private Function<? super Float, ?> text;
    private Function<?, String> fmt;
    private Function<?, Color> col;

    @RName("prog")
    public static class $_ implements Factory {
	public Widget create(UI ui, Object[] args) {
	    Progress ret = new Progress(UI.scale(Utils.iv(args[0])));
	    if(args.length > 1)
		ret.a = Utils.fv(args[1]);
	    return(ret);
	}
    }

    public Progress(Coord sz) {
	super(sz);
    }

    public Progress(int w) {
	this(Coord.of(w, defh));
    }

    public Progress val(Supplier<Float> val) {this.val = val; return(this);}
    public <V> Progress text(Function<? super Float, V> text, Function<? super V, String> fmt, Function<? super V, Color> col) {
	this.text = text;
	this.fmt = fmt;
	this.col = col;
	return(this);
    }
    public <V> Progress text(Supplier<V> text, Function<? super V, String> fmt, Function<? super V, Color> col) {return(text(a -> text.get(), fmt, col));}
    public <V> Progress text(Function<? super Float, V> text, Function<? super V, String> fmt) {return(text(text, fmt, val -> Color.WHITE));}
    public <V> Progress text(Supplier<V> text, Function<? super V, String> fmt) {return(text(a -> text.get(), fmt, val -> Color.WHITE));}
    public Progress text(Function<? super Float, ?> text) {return(text(text, String::valueOf));}
    public Progress text(Supplier<?> text) {return(text(a -> text.get(), String::valueOf));}
    public Progress percent() {return(text(a -> (int)Math.floor(a * 100), p -> String.format("%d%%", p)));}

    private Tex rt = null;
    private Object pt;
    public void draw(GOut g) {
	float a = (val == null) ? this.a : val.get();
	g.chcolor(0, 0, 0, 255);
	g.frect(Coord.z, sz);
	g.chcolor(128, 0, 0, 255);
	int mw = (int)Math.floor((sz.x - (m * 2)) * a);
	g.frect(Coord.of(m), new Coord(mw, sz.y - (m * 2)));
	g.chcolor();
	Object t = (text == null) ? null : text.apply(a);
	if((rt != null) && !Utils.eq(t, pt)) {
	    rt.dispose();
	    rt = null;
	}
	if((rt == null) && (text != null)) {
	    @SuppressWarnings("unchecked") Color col = ((Function<Object, Color>)this.col).apply(t);
	    @SuppressWarnings("unchecked") String fmt = ((Function<Object, String>)this.fmt).apply(t);
	    rt = new TexI(Utils.outline2(Text.render(fmt, col).img, Utils.contrast(col)));
	}
	if(rt != null)
	    g.aimage(rt, sz.div(2), 0.5, 0.5);
    }

    public void uimsg(String msg, Object... args) {
	if(msg == "p") {
	    a = Utils.fv(args[0]);
	} else {
	    super.uimsg(msg, args);
	}
    }
}
