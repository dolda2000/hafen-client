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

import haven.Button;
import haven.Label;
import haven.*;
import haven.VertexBuf.AttribData;
import haven.Window;
import haven.VertexBuf.VertexData;
import haven.render.*;
import haven.render.DataBuffer.Filler;
import haven.render.DataBuffer.Usage;
import haven.render.Model.Indices;
import haven.render.Model.Mode;
import haven.render.Pipe.Op;
import haven.render.RenderTree.Node;
import haven.render.RenderTree.Slot;
import haven.render.States.DepthBias;
import haven.render.States.Depthtest;
import haven.render.States.Depthtest.Test;
import haven.render.TickList.TickNode;
import haven.render.TickList.Ticking;
import haven.render.VertexArray.Buffer;
import haven.render.VertexArray.Layout;
import haven.render.VertexArray.Layout.Input;

import java.awt.*;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

public class LandSurvey extends Window {
    static final Layout pfmt;
    private static final Op olmat;

    static {
	pfmt = new Layout(new Input(Homo3D.vertex, new VectorFormat(3, NumberFormat.FLOAT32), 0, 0, 16), new Input(VertexColor.color, new VectorFormat(4, NumberFormat.UNORM8), 0, 12, 16));
	olmat = Op.compose(new BaseColor(new Color(255, 0, 0, 64)), Rendered.postfx, new DepthBias(-2.0F, -2.0F));
    }

    final Coord ul;
    final Coord br;
    final FastMesh ol;
    final Location dloc;
    final Label albl;
    final Label zdlbl;
    final Label wlbl;
    final Label dlbl;
    final HSlider zset;
    MapView mv;
    LandSurvey.Display dsp;
    Slot s_dsp;
    Slot s_ol;
    int tz;
    private boolean upd = true;
    private long sendtz = 0L;
    private int olseq = -1;

    public LandSurvey(Coord var1, Coord var2, int var3) {
	super(Coord.z, "Land survey", true);
	this.ul = var1;
	this.br = var2;
	this.tz = var3;
	this.dloc = Location.xlate(new Coord3f((float) this.ul.x * (float) MCache.tilesz.x, (float) (-this.ul.y) * (float) MCache.tilesz.y, 0.0F));
	VertexData var4 = new VertexData(FloatBuffer.wrap(new float[]{0.0F, 0.0F, 0.0F, (float) (var2.x - var1.x) * (float) MCache.tilesz.x, 0.0F, 0.0F, (float) (var2.x - var1.x) * (float) MCache.tilesz.x, (float) (-(var2.y - var1.y)) * (float) MCache.tilesz.y, 0.0F, 0.0F, (float) (-(var2.y - var1.y)) * (float) MCache.tilesz.y, 0.0F}));
	this.ol = new FastMesh(new VertexBuf(var4), ShortBuffer.wrap(new short[]{0, 3, 1, 1, 3, 2}));
	this.albl = this.add(new Label(String.format("Area: %d m²", (var2.x - var1.x) * (var2.y - var1.y))), 0, 0);
	this.zdlbl = this.add(new Label("..."), 0, UI.scale(15));
	this.wlbl = this.add(new Label("..."), 0, UI.scale(30));
	this.dlbl = this.add(new Label("..."), 0, UI.scale(45));
	this.zset = this.add(new HSlider(UI.scale(225), -1, 1, var3) {
	    public void changed() {
		LandSurvey.this.tz = this.val;
		LandSurvey.this.upd = true;
		LandSurvey.this.sendtz = System.currentTimeMillis() + 500L;
	    }
	}, 0, UI.scale(60));
	this.add(new Button(UI.scale(100), "Make level") {
	    public void click() {
		LandSurvey.this.wdgmsg("lvl", LandSurvey.this.tz);
	    }
	}, 0, UI.scale(90));
	this.add(new Button(UI.scale(100), "Remove") {
	    public void click() {
		LandSurvey.this.wdgmsg("rm");
	    }
	}, UI.scale(125), UI.scale(90));
	this.pack();
    }

    public static Widget mkwidget(UI var0, Object... var1) {
	Coord var2 = (Coord) var1[0];
	Coord var3 = (Coord) var1[1];
	int var4 = ((Number) var1[2]).intValue();
	return new LandSurvey(var2, var3, var4);
    }

    protected void attached() {
	super.attached();
	this.mv = this.getparent(GameUI.class).map;
	this.dsp = new LandSurvey.Display();
    }

    private int autoz() {
	MCache var1 = this.mv.ui.sess.glob.map;
	int var2 = 0;
	int var3 = 0;
	Coord var4 = new Coord();

	for (var4.y = this.ul.y; var4.y <= this.br.y; ++var4.y) {
	    for (var4.x = this.ul.x; var4.x <= this.br.x; ++var4.x) {
		var2 += var1.getz(var4);
		++var3;
	    }
	}

	return (int) Math.round((double) var2 / (double) var3);
    }

    private void updmap() {
	MCache var1 = this.mv.ui.sess.glob.map;
	Coord var2 = new Coord();
	int var3 = 2147483647;
	int var4 = -2147483648;
	int var5 = 0;
	int var6 = 0;

	for (var2.y = this.ul.y; var2.y <= this.br.y; ++var2.y) {
	    for (var2.x = this.ul.x; var2.x <= this.br.x; ++var2.x) {
		int var7 = var1.getz(var2);
		var3 = Math.min(var3, var7);
		var4 = Math.max(var4, var7);
		var5 += this.tz - var7;
		if (var7 > this.tz) {
		    var6 += var7 - this.tz;
		}
	    }
	}

	this.zset.min = var3 - 11;
	this.zset.max = var4 + 11;
	this.zdlbl.settext(String.format("Peak to trough: %.1f m", (double) (var4 - var3) / 10.0D));
	if (var5 >= 0) {
	    this.wlbl.settext(String.format("Units of soil required: %d", var5));
	} else {
	    this.wlbl.settext(String.format("Units of soil left over: %d", -var5));
	}

	this.dlbl.settext(String.format("Units of soil to dig: %d", var6));
	this.dsp.update = true;
    }

    public void tick(double var1) {
	super.tick(var1);
	if (this.tz == -2147483648) {
	    try {
		this.zset.val = this.tz = this.autoz();
		this.olseq = this.mv.ui.sess.glob.map.olseq;
		this.upd = true;
	    } catch (Loading var5) {
	    }
	} else {
	    if (this.upd || this.olseq != this.mv.ui.sess.glob.map.olseq) {
		try {
		    this.updmap();
		    this.olseq = this.mv.ui.sess.glob.map.olseq;
		    this.upd = false;
		} catch (Loading var4) {
		}
	    }

	    if (this.s_dsp == null && this.olseq != -1) {
		this.s_dsp = this.mv.drawadd(this.dsp);
		this.s_ol = this.mv.drawadd(this.ol);
	    }

	    if (this.s_ol != null) {
		this.s_ol.cstate(Op.compose(olmat, Location.xlate(new Coord3f((float) this.ul.x * (float) MCache.tilesz.x, (float) (-this.ul.y) * (float) MCache.tilesz.y, (float) this.tz))));
	    }
	}

	if (this.sendtz != 0L && this.sendtz > System.currentTimeMillis()) {
	    this.wdgmsg("tz", this.tz);
	    this.sendtz = 0L;
	}

    }

    public void destroy() {
	if (this.s_dsp != null) {
	    this.s_dsp.remove();
	    this.s_ol.remove();
	}

	super.destroy();
    }

    class Display implements Rendered, Node, Ticking, TickNode {
	final Op ptsz = new PointSize(3.0F);
	final MCache map;
	final int area;
	final Model model;
	boolean update = true;

	Display() {
	    this.map = LandSurvey.this.mv.ui.sess.glob.map;
	    this.area = (LandSurvey.this.br.x - LandSurvey.this.ul.x + 1) * (LandSurvey.this.br.y - LandSurvey.this.ul.y + 1);
	    VertexArray var2 = new VertexArray(LandSurvey.pfmt, new Buffer(this.area * LandSurvey.pfmt.inputs[0].stride, Usage.STATIC, this::initfill));
	    this.model = new Model(Mode.POINTS, var2, null);
	}

	public void draw(Pipe var1, Render var2) {
	    var2.draw(var1, this.model);
	}

	private FillBuffer initfill(Buffer var1, Environment var2) {
	    try {
		return this.fill(var1, var2);
	    } catch (Loading var4) {
		return Filler.zero().fill(var1, var2);
	    }
	}

	private FillBuffer fill(Buffer var1, Environment var2) {
	    FillBuffer var3 = var2.fillbuf(var1);
	    ByteBuffer var4 = var3.push();
	    Coord var5 = new Coord();

	    for (var5.y = LandSurvey.this.ul.y; var5.y <= LandSurvey.this.br.y; ++var5.y) {
		for (var5.x = LandSurvey.this.ul.x; var5.x <= LandSurvey.this.br.x; ++var5.x) {
		    int var6 = this.map.getz(var5);
		    var4.putFloat((float) (var5.x - LandSurvey.this.ul.x) * (float) MCache.tilesz.x).putFloat((float) (-(var5.y - LandSurvey.this.ul.y)) * (float) MCache.tilesz.y).putFloat((float) LandSurvey.this.tz);
		    if (LandSurvey.this.tz < var6) {
			var4.put((byte) -1).put((byte) 0).put((byte) -1).put((byte) -1);
		    } else if (LandSurvey.this.tz > var6) {
			var4.put((byte) 0).put((byte) -128).put((byte) -1).put((byte) -1);
		    } else {
			var4.put((byte) 0).put((byte) -1).put((byte) 0).put((byte) -1);
		    }
		}
	    }

	    return var3;
	}

	public void autogtick(Render var1) {
	    if (this.update) {
		try {
		    var1.update(this.model.va.bufs[0], this::fill);
		    this.update = false;
		} catch (Loading var3) {
		}
	    }

	}

	public Ticking ticker() {
	    return this;
	}

	public void added(Slot var1) {
	    var1.ostate(Op.compose(LandSurvey.this.dloc, this.ptsz, new Depthtest(Test.TRUE), Rendered.last, VertexColor.instance));
	}
    }
}
