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
import java.awt.Color;
import java.awt.image.*;
import java.awt.color.ColorSpace;
import javax.imageio.*;
import javax.imageio.metadata.*;
import javax.imageio.stream.*;
import org.w3c.dom.*;
import java.io.*;
import java.net.*;

public class Screenshooter extends Window {
    public static final ComponentColorModel outcm = new ComponentColorModel(ColorSpace.getInstance(ColorSpace.CS_sRGB), new int[] {8, 8, 8}, false, false, ComponentColorModel.OPAQUE, DataBuffer.TYPE_BYTE);
    public static final PUtils.Convolution thumbflt = new PUtils.Lanczos(3);
    public final URL tgt;
    public final Shot shot;
    private final TextEntry comment;
    private final CheckBox decobox, pub;
    private Label prog;
    private Coord btnc;
    private Button btn;

    public Screenshooter(URL tgt, Shot shot) {
	super(Coord.z, "Screenshot");
	this.tgt = tgt;
	this.shot = shot;
	Coord tsz = shot.tsz();
	add(new Widget(tsz) {
		public void draw(GOut g) {
		    TexI tex = decobox.a ? shot.uit : shot.mapt;
		    g.image(tex, Coord.z);
		}
	    }, new Coord(0, 0));
	this.decobox = adda(new CheckBox("Include interface", true), new Coord(tsz.x + 5, tsz.y / 2), 0, 0.5);
	Label clbl = add(new Label("If you wish, leave a comment:"), new Coord(0, tsz.y + 5));
	this.comment = add(new TextEntry(tsz.x + 130, "") {
		public void activate(String text) {
		    upload();
		}
	    }, new Coord(0, clbl.c.y + clbl.sz.y + 5));
	this.pub = add(new CheckBox("Make public"), new Coord(0, comment.c.y + comment.sz.y + 5));
	pub.a = true;
	btnc = new Coord((comment.sz.x - 125) / 2, pub.c.y + pub.sz.y + 20);
	btn = add(new Button(125, "Upload", false, this::upload), btnc);
	pack();
    }

    public void wdgmsg(Widget sender, String msg, Object... args) {
	if((sender == this) && (msg == "close")) {
	    ui.destroy(this);
	} else {
	    super.wdgmsg(sender, msg, args);
	}
    }

    public static class Shot {
	public final BufferedImage map, ui;
	public final TexI mapt, uit;
	public String comment, camera;
	public boolean fsaa, fl, sdw;

	public Shot(BufferedImage map, BufferedImage ui) {
	    this.map = map;
	    this.ui = ui;
	    Coord sz = PUtils.imgsz(map);
	    int w = Math.min(200 * sz.x / sz.y, 150);
	    int h = w * sz.y / sz.x;
	    Coord tsz = new Coord(w, h);
	    this.mapt = new TexI(PUtils.convolvedown(map, tsz, thumbflt));
	    this.uit = new TexI(PUtils.convolvedown(ui, tsz, thumbflt));
	}

	public Coord sz() {return(PUtils.imgsz(map));}
	public Coord tsz() {return(mapt.sz());}
    }

    public static interface ImageFormat {
	public String ctype();
	public void write(OutputStream out, BufferedImage img, Shot info) throws IOException;
    }

    public static final ImageFormat png = new ImageFormat() {
	    public String ctype() {return("image/png");}

	    void cmt(Node tlist, String key, String val) {
		Element cmt = new IIOMetadataNode("TextEntry");
		cmt.setAttribute("keyword", key);
		cmt.setAttribute("value", val);
		cmt.setAttribute("encoding", "utf-8");
		cmt.setAttribute("language", "");
		cmt.setAttribute("compression", "none");
		tlist.appendChild(cmt);
	    }

	    public void write(OutputStream out, BufferedImage img, Shot info) throws IOException {
		ImageTypeSpecifier type = ImageTypeSpecifier.createFromRenderedImage(img);
		ImageWriter wr = ImageIO.getImageWriters(type, "PNG").next();
		IIOMetadata dat = wr.getDefaultImageMetadata(type, null);
		Node root = dat.getAsTree("javax_imageio_1.0");
		Node tlist = new IIOMetadataNode("Text");
		if(info.comment != null)
		    cmt(tlist, "Comment", info.comment);
		if(info.camera != null)
		    cmt(tlist, "haven.cam", info.camera);
		cmt(tlist, "haven.fsaa", info.fsaa?"y":"n");
		cmt(tlist, "haven.flight", info.fl?"y":"n");
		cmt(tlist, "haven.sdw", info.sdw?"y":"n");
		cmt(tlist, "haven.conf", Config.confid);
		root.appendChild(tlist);
		dat.setFromTree("javax_imageio_1.0", root);
		ImageOutputStream iout = ImageIO.createImageOutputStream(out);
		wr.setOutput(iout);
		wr.write(new IIOImage(img, null, dat));
	    }
	};

    public static final ImageFormat jpeg = new ImageFormat() {
	    public String ctype() {return("image/jpeg");}

	    public void write(OutputStream out, BufferedImage img, Shot info) throws IOException {
		ImageTypeSpecifier type = ImageTypeSpecifier.createFromRenderedImage(img);
		ImageWriter wr = ImageIO.getImageWriters(type, "JPEG").next();
		IIOMetadata dat = wr.getDefaultImageMetadata(type, null);

		Node root = dat.getAsTree("javax_imageio_jpeg_image_1.0");
		Node mseq;
		for(mseq = root.getFirstChild(); (mseq != null) && !mseq.getLocalName().equals("markerSequence"); mseq = mseq.getNextSibling());
		if(mseq == null) {
		    mseq = new IIOMetadataNode("markerSequence");
		    root.appendChild(mseq);
		}
		if(info.comment != null) {
		    IIOMetadataNode cmt = new IIOMetadataNode("com");
		    cmt.setUserObject(info.comment.getBytes("utf-8"));
		    mseq.appendChild(cmt);
		}

		MessageBuf hdat = new MessageBuf();
		hdat.addstring2("HSSI1");
		if(info.camera != null) {
		    hdat.addstring("cam");
		    hdat.addstring(info.camera);
		}
		hdat.addstring("fsaa");
		hdat.addstring(info.fsaa?"y":"n");
		hdat.addstring("flight");
		hdat.addstring(info.fl?"y":"n");
		hdat.addstring("sdw");
		hdat.addstring(info.sdw?"y":"n");
		hdat.addstring("conf");
		hdat.addstring(Config.confid);
		IIOMetadataNode app4 = new IIOMetadataNode("unknown");
		app4.setAttribute("MarkerTag", "228");
		app4.setUserObject(hdat.fin());
		mseq.appendChild(app4);

		dat.setFromTree("javax_imageio_jpeg_image_1.0", root);

		ImageOutputStream iout = ImageIO.createImageOutputStream(out);
		wr.setOutput(iout);
		wr.write(new IIOImage(img, null, dat));
	    }
	};

    public static class UploadError extends IOException {
	public UploadError(String message) {
	    super(message);
	}
    }

    public class Uploader extends HackThread {
	private final BufferedImage img;
	private final Shot info;
	private final ImageFormat fmt;
	private volatile Closeable hackint = null;
	private boolean cancelled = false;

	public Uploader(BufferedImage img, Shot info, ImageFormat fmt) {
	    super("Screenshot uploader");
	    this.img = img;
	    this.info = info;
	    this.fmt = fmt;
	}

	public void run() {
	    try {
		upload(img, info, fmt);
	    } catch(InterruptedIOException e) {
		setstate("Cancelled");
		synchronized(ui) {
		    ui.destroy(btn);
		    btn = add(new Button(125, "Retry", false, Screenshooter.this::upload), btnc);
		}
	    } catch(IOException e) {
		if(cancelled)
		    setstate("Cancelled");
		else if(e instanceof UploadError)
		    setstate("Error: " + e.getMessage());
		else
		    setstate("Could not upload image");
		synchronized(ui) {
		    ui.destroy(btn);
		    btn = add(new Button(125, "Retry", false, Screenshooter.this::upload), btnc);
		}
	    }
	}

	private void setstate(String t) {
	    synchronized(ui) {
		if(prog != null)
		    ui.destroy(prog);
		prog = adda(new Label(t), btnc.add(62, -15), 0.5, 0.0);
	    }
	}

	private BufferedImage convert(BufferedImage img) {
	    WritableRaster buf = PUtils.byteraster(PUtils.imgsz(img), 3);
	    BufferedImage ret = new BufferedImage(outcm, buf, false, null);
	    java.awt.Graphics g = ret.getGraphics();
	    g.drawImage(img, 0, 0, null);
	    g.dispose();
	    return(ret);
	}

	public void upload(BufferedImage ss, Shot info, ImageFormat fmt) throws IOException {
	    setstate("Preparing image...");
	    ByteArrayOutputStream buf = new ByteArrayOutputStream();

	    fmt.write(buf, convert(ss), info);

	    byte[] data = buf.toByteArray();
	    buf = null;
	    setstate("Connecting...");
	    URL pared = Utils.urlparam(tgt, "p", pub.a?"y":"n");
	    HttpURLConnection conn = (HttpURLConnection)pared.openConnection();
	    conn.setDoOutput(true);
	    conn.setFixedLengthStreamingMode(data.length);
	    conn.addRequestProperty("Content-Type", fmt.ctype());
	    MessageBuf auth = new MessageBuf();
	    auth.addstring2(ui.sess.username + "/");
	    auth.addbytes(ui.sess.sesskey);
	    conn.addRequestProperty("Authorization", "Haven " + Utils.base64enc(auth.fin()));
	    conn.connect();
	    OutputStream out = conn.getOutputStream();
	    try {
		hackint = out;
		int off = 0;
		while(off < data.length) {
		    setstate(String.format("Uploading (%d%%)...", (off * 100) / data.length));
		    int len = Math.min(1024, data.length - off);
		    out.write(data, off, len);
		    off += len;
		}
	    } finally {
		hackint = null;
		out.close();
	    }
	    setstate("Awaiting response...");
	    InputStream in = conn.getInputStream();
	    final URL result;
	    try {
		hackint = in;
		if(conn.getContentType().equals("text/x-error-response"))
		    throw(new UploadError(new String(Utils.readall(in), "utf-8")));
		if(!conn.getContentType().equals("text/x-target-url"))
		    throw(new IOException("Unexpected type of reply from server"));
		byte[] b = Utils.readall(in);
		try {
		    result = new URL(new String(b, "utf-8"));
		} catch(MalformedURLException e) {
		    throw((IOException)new IOException("Unexpected reply from server").initCause(e));
		}
	    } finally {
		hackint = null;
		in.close();
	    }
	    setstate("Done");
	    synchronized(ui) {
		ui.destroy(btn);
		btn = add(new Button(125, "Open in browser", false, () -> {
			    if(WebBrowser.self != null)
				WebBrowser.self.show(result);
			}), btnc);
	    }
	}

	public void interrupt() {
	    cancelled = true;
	    Closeable c = hackint;
	    if(c != null) {
		try {
		    c.close();
		} catch(IOException e) {
		}
	    }
	    super.interrupt();
	}
    }

    public void upload() {
	shot.comment = comment.text;
	final Uploader th = new Uploader(decobox.a ? shot.ui : shot.map, shot, png);
	th.start();
	ui.destroy(btn);
	btn = add(new Button(125, "Cancel", false, th::interrupt), btnc);
    }

    public static void take(GameUI gameui, URL tgt) {
	new Object() {
	    BufferedImage map = null, ui = null;
	    {
		gameui.map.delay2(g -> g.getimage(Coord.z, g.sz, img -> {
			    map = img;
			    checkcomplete(g);
			}));
		gameui.ui.drawafter(g -> g.getimage(Coord.z, g.sz, img -> {
			    ui = img;
			    checkcomplete(g);
			}));
	    }

	    private void checkcomplete(GOut g) {
		if((map != null) && (ui != null)) {
		    GLSettings pref = g.gc.pref;
		    String camera = gameui.map.camera.getClass().getName();
		    Utils.defer(() -> {
			    Shot shot = new Shot(map, ui);
			    shot.fl = pref.flight.val;
			    shot.sdw = pref.lshadow.val;
			    shot.fsaa = pref.fsaa.val;
			    shot.camera = camera;
			    gameui.addchild(new Screenshooter(tgt, shot), "misc", new Coord2d(0.1, 0.1));
			});
		}
	    }
	};
    }
}
