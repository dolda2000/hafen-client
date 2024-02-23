package haven.render.lwjgl;

import haven.*;
import haven.render.*;
import java.awt.*;
import java.awt.event.*;
import org.lwjgl.opengl.awt.*;
import java.awt.Frame;
import static org.lwjgl.opengl.GL30.*;

public class Test {
    static final FColor gay = new FColor(1.0f, 0.0f, 0.5f);
    Frame wnd;
    TestPanel p;
    volatile boolean done = false;
    LWJGLEnvironment env;

    static class TestPanel extends AWTGLCanvas {
	TestPanel() {
	    super(mkcaps());
	    setSize(1024, 768);
	}

	static GLData mkcaps() {
	    GLData caps = new GLData();
	    caps.profile = GLData.Profile.CORE;
	    caps.majorVersion = 3;
	    caps.minorVersion = 2;
	    return(caps);
	}

	public void initGL() {}
	public void paintGL() {}

	public LWJGLEnvironment mkenv() {
	    org.lwjgl.opengl.GL.createCapabilities();
	    return(new LWJGLEnvironment(Area.sized(Coord.of(getWidth(), getHeight()))));
	}
    }

    Test() {
	wnd = new Frame("Test");
	p = new TestPanel();
	wnd.add(p);
	wnd.pack();
	wnd.setResizable(false);
	p.requestFocus();
	wnd.addWindowListener(new WindowAdapter() {
		public void windowClosing(WindowEvent e) {
		    done = true;
		}
	    });
	wnd.setVisible(true);
	try {
	    env = p.executeInContext(p::mkenv);
	} catch(Exception e) {
	    throw(new RuntimeException(e));
	}
	reshape(Area.sized(Coord.of(p.getWidth(), p.getHeight())));
    }

    Pipe base;
    void draw(Render g) {
	Pipe base = this.base.copy();
	g.clear(base, FragColor.fragcol, gay);
	g.clear(base, 1.0);
	{
	    Pipe st = base.copy();
	    st.prep(VertexColor.instance);
	    VertexArray.Layout fmt = new VertexArray.Layout(new VertexArray.Layout.Input(Ortho2D.pos, new VectorFormat(2, NumberFormat.FLOAT32), 0, 0, 24),
							    new VertexArray.Layout.Input(VertexColor.color, new VectorFormat(4, NumberFormat.FLOAT32), 0, 8, 24));
	    float[] data = {
		50, 50, 1, 0, 0, 1,
		25, 86, 0, 1, 0, 1,
		75, 86, 0, 0, 1, 1,
	    };
	    g.draw(st, Model.Mode.TRIANGLES, null, fmt, 3, data);
	}
	{
	    Pipe st = base.copy();
	    st.prep(VertexColor.instance);
	    VertexArray.Layout fmt = new VertexArray.Layout(new VertexArray.Layout.Input(Ortho2D.pos, new VectorFormat(2, NumberFormat.SINT16), 0, 0, 8),
							    new VertexArray.Layout.Input(VertexColor.color, new VectorFormat(4, NumberFormat.UNORM8), 0, 4, 8));
	    byte[] data = {
		(byte)150, 0, 50, 0, (byte)255, 0, 0, (byte)255,
		(byte)125, 0, 86, 0, 0, (byte)255, 0, (byte)255,
		(byte)175, 0, 86, 0, 0, 0, (byte)255, (byte)255,
	    };
	    Model mod = new Model(Model.Mode.TRIANGLES,
				  new VertexArray(fmt, new VertexArray.Buffer(data.length, DataBuffer.Usage.EPHEMERAL, DataBuffer.Filler.of(data))),
				  null, 0, 3);
	    g.draw(st, mod);
	    mod.dispose();
	}
    }

    void draw() {
	Render g = env.render();
	draw(g);
	env.submit(g);
	p.runInContext(() -> {
		env.process(LWJGLWrap.instance);
		p.swapBuffers();
	    });
    }

    void run() {
	while(!done)
	    draw();
    }

    void reshape(Area wnd) {
	Pipe base = new BufPipe();
	base.prep(new States.Viewport(wnd)).prep(new Ortho2D(wnd)).prep(FragColor.blend(new BlendMode()));
	base.prep(new FragColor<>(FragColor.defcolor)).prep(new DepthBuffer<>(DepthBuffer.defdepth));
	this.base = base;
    }

    public static void main(String[] args) {
	Test t = new Test();
	t.run();
	System.exit(0);
    }
}
