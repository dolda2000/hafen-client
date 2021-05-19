package haven.render.gl;

import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.*;
import java.nio.file.*;
import javax.imageio.*;
import com.jogamp.opengl.*;
import com.jogamp.newt.event.*;
import com.jogamp.newt.opengl.*;
import com.jogamp.opengl.util.Animator;
import haven.*;
import haven.render.*;
import haven.render.States;
import haven.render.sl.*;
import static haven.render.sl.Cons.*;
import static haven.render.sl.Type.*;

public class Test implements GLEventListener, KeyListener {
    static final FColor gay = new FColor(1.0f, 0.0f, 0.5f);
    GLWindow wnd;
    GLEnvironment env;
    volatile boolean done;
    Pipe base;
    Area shape;

    Test() {
	GLCapabilities caps = new GLCapabilities(GLProfile.getDefault());
	wnd = GLWindow.create(caps);
	wnd.setTitle("Test");
	wnd.setSize(1024, 768);
	wnd.setVisible(true);
	wnd.addGLEventListener(this);
	wnd.addKeyListener(this);
	wnd.addWindowListener(new WindowAdapter() {
		public void windowDestroyed(WindowEvent ev) {
		    done = true;
		}
	    });
    }

    void run() {
	while(!done) {
	    wnd.display();
	}
    }

    public static void main(String[] args) {
	Test t = new Test();
	t.run();
	System.exit(0);
    }

    static final Coord texsz;
    static final byte[] texdat;
    static final Texture2D.Sampler2D tex;
    static {
	try {
	    try(InputStream in = Files.newInputStream(Utils.path("/tmp/test.png"))) {
		BufferedImage img = ImageIO.read(in);
		texsz = new Coord(img.getWidth(), img.getHeight());
		texdat = TexI.convert(img, texsz);
	    }
	} catch(IOException e) {
	    throw(new RuntimeException(e));
	}
	tex = new Texture2D.Sampler2D(new Texture2D(texsz.x, texsz.y, DataBuffer.Usage.STATIC, new VectorFormat(4, NumberFormat.UNORM8), new VectorFormat(4, NumberFormat.UNORM8),
						    (img, env) -> {
							if(img.level != 0)
							    return(null);
							FillBuffer buf = env.fillbuf(img);
							buf.pull(ByteBuffer.wrap(texdat));
							return(buf);
						    }));
    }

    static class VertexColor extends State {
	static final Slot<VertexColor> slot = new Slot<>(Slot.Type.DRAW, VertexColor.class);
	static final Attribute color = new Attribute(VEC4, "color");

	static final AutoVarying fcolor = new AutoVarying(VEC4) {
		protected Expression root(VertexContext vctx) {
		    return(color.ref());
		}
	    };

	static final ShaderMacro shader = prog -> {
	    FragColor.fragcol(prog.fctx).mod(in -> mul(in, fcolor.ref()), 0);
	};
	public ShaderMacro shader() {return(shader);}
	public void apply(Pipe p) {p.put(slot, this);}
    }

    static class Tex2D extends State {
	static final Slot<Tex2D> slot = new Slot<>(Slot.Type.DRAW, Tex2D.class);
	static final Attribute texc = new Attribute(VEC2, "texc");
	static final Uniform usmp = new Uniform(SAMPLER2D, "tex2d", p -> p.get(slot).tex, slot);
	final Texture2D.Sampler2D tex;

	Tex2D(Texture2D.Sampler2D tex) {
	    this.tex = tex;
	}

	static final AutoVarying ftexc = new AutoVarying(VEC2) {
		protected Expression root(VertexContext vctx) {
		    return(texc.ref());
		}
	    };

	static final ShaderMacro shader = prog -> {
	    FragColor.fragcol(prog.fctx).mod(in -> mul(in, texture2D(usmp.ref(), ftexc.ref())), 0);
	};
	public ShaderMacro shader() {return(shader);}
	public void apply(Pipe p) {p.put(slot, this);}
    }

    final VertexColor vc = new VertexColor();
    final Texture2D.Sampler2D tbuf = new Texture2D.Sampler2D(new Texture2D(512, 512, DataBuffer.Usage.STATIC, new VectorFormat(4, NumberFormat.UNORM8), null));
    final Texture2D dbuf = new Texture2D(512, 512, DataBuffer.Usage.STATIC, Texture.DEPTH, new VectorFormat(1, NumberFormat.FLOAT32), null);
    private void display(Render g) {
	Pipe fb = base.copy();
	Area fbarea = new Area(Coord.z, new Coord(dbuf.w, dbuf.h));
	fb.prep(new States.Viewport(fbarea)).prep(new Ortho2D(fbarea));
	fb.prep(new FragColor<>(tbuf.tex.image(0))).prep(new DepthBuffer<>(dbuf.image(0)));
	g.clear(fb, FragColor.fragcol, gay);
	g.clear(fb, 1.0);
	{
	    Pipe pipe = fb.copy();
	    vc.apply(pipe);
	    VertexArray.Layout fmt = new VertexArray.Layout(new VertexArray.Layout.Input(Ortho2D.pos, new VectorFormat(2, NumberFormat.FLOAT32), 0, 0, 24),
							    new VertexArray.Layout.Input(VertexColor.color, new VectorFormat(4, NumberFormat.FLOAT32), 0, 8, 24));
	    float[] data = {
		50, 50, 1, 0, 0, 1,
		25, 86, 0, 1, 0, 1,
		75, 86, 0, 0, 1, 1,
	    };
	    g.draw(pipe, Model.Mode.TRIANGLES, null, fmt, 3, data);
	}

	{
	    Pipe pipe = fb.copy();
	    vc.apply(pipe);
	    VertexArray.Layout fmt = new VertexArray.Layout(new VertexArray.Layout.Input(Ortho2D.pos, new VectorFormat(2, NumberFormat.SINT16), 0, 0, 8),
							    new VertexArray.Layout.Input(VertexColor.color, new VectorFormat(4, NumberFormat.UNORM8), 0, 4, 8));
	    byte[] data = {
		(byte)150, 0, 50, 0, (byte)255, 0, 0, (byte)255,
		(byte)125, 0, 86, 0, 0, (byte)255, 0, (byte)255,
		(byte)175, 0, 86, 0, 0, 0, (byte)255, (byte)255,
	    };
	    g.draw(pipe, new Model(Model.Mode.TRIANGLES,
				   new VertexArray(fmt, new VertexArray.Buffer(data.length, DataBuffer.Usage.EPHEMERAL, DataBuffer.Filler.of(data))),
				   null, 0, 3));
	}

	{
	    Pipe pipe = fb.copy();
	    new Tex2D(tex).apply(pipe);
	    VertexArray.Layout fmt = new VertexArray.Layout(new VertexArray.Layout.Input(Ortho2D.pos, new VectorFormat(2, NumberFormat.SINT16), 0, 0, 6),
							    new VertexArray.Layout.Input(Tex2D.texc, new VectorFormat(2, NumberFormat.UNORM8), 0, 4, 6));
	    byte[] data = {
		(byte)100, 0, (byte)228, 0, 0, (byte)255,
		(byte)100, 0, 100, 0, 0, 0,
		(byte)228, 0, (byte)228, 0, (byte)255, (byte)255,
		(byte)225, 0, 100, 0, (byte)255, 0,
	    };
	    g.draw(pipe, new Model(Model.Mode.TRIANGLE_STRIP,
				   new VertexArray(fmt, new VertexArray.Buffer(data.length, DataBuffer.Usage.EPHEMERAL, DataBuffer.Filler.of(data))),
				   null, 0, 4));
	}

	{
	    Pipe pipe = base.copy();
	    new Tex2D(tbuf).apply(pipe);
	    VertexArray.Layout fmt = new VertexArray.Layout(new VertexArray.Layout.Input(Ortho2D.pos, new VectorFormat(2, NumberFormat.FLOAT32), 0, 0, 16),
							    new VertexArray.Layout.Input(Tex2D.texc, new VectorFormat(2, NumberFormat.FLOAT32), 0, 8, 16));
	    float[] data = {
		100, 612, 0, 0,
		100, 100, 0, 1,
		612, 612, 1, 0,
		612, 100, 1, 1,
	    };
	    g.draw(pipe, new Model(Model.Mode.TRIANGLE_STRIP,
				   new VertexArray(fmt, new VertexArray.Buffer(data.length * 4, DataBuffer.Usage.EPHEMERAL, DataBuffer.Filler.of(data))),
				   null, 0, 4));
	}
    }

    public void display(GLAutoDrawable wnd) {
	try {
	    GL3 gl = wnd.getGL().getGL3();
	    // gl = new TraceGL3(gl, System.err);
	    if((env == null) || (wnd.getContext() != env.ctx)) {
		if(env != null)
		    System.err.println("switching contexts");
		env = new GLEnvironment(gl, wnd.getContext(), shape);
		gl.setSwapInterval(1);
	    }
	    if(!env.shape().equals(shape))
		env.reshape(shape);
	    GLRender g = env.render();
	    display(g);
	    env.submit(g);
	    env.process(gl);
	} catch(BGL.BGLException e) {
	    e.printStackTrace();
	    e.dump.dump();
	    System.exit(1);
	} catch(Throwable e) {
	    e.printStackTrace();
	    System.exit(1);
	}
    }

    public void init(GLAutoDrawable wnd) {
    }

    public void dispose(GLAutoDrawable wnd) {
    }

    public void reshape(GLAutoDrawable wnd, int x, int y, int w, int h) {
	Pipe base = new BufPipe();
	Area area = Area.sized(new Coord(x, y), new Coord(w, h));
	base.prep(new States.Viewport(area)).prep(new Ortho2D(area)).prep(FragColor.blend(new BlendMode()));
	base.prep(new FragColor<>(FragColor.defcolor)).prep(new DepthBuffer<>(DepthBuffer.defdepth));
	// base.prep(new FragColor(new Texture2D(512, 512, DataBuffer.Usage.STATIC, new VectorFormat(4, NumberFormat.UNORM8), null).image(0)));
	// base.prep(new DepthBuffer(new Texture2D(512, 512, DataBuffer.Usage.STATIC, Texture.DEPTH, new VectorFormat(1, NumberFormat.FLOAT32), null).image(0)));
	this.shape = area;
	this.base = base;
    }

    public void keyPressed(KeyEvent ev) {
	if(ev.getKeyCode() == KeyEvent.VK_ESCAPE) {
	    done = true;
	}
    }

    public void keyReleased(KeyEvent ev) {
    }
}
