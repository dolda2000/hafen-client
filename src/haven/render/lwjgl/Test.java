package haven.render.lwjgl;

import java.awt.*;
import java.awt.event.*;
import org.lwjgl.opengl.awt.*;
import static org.lwjgl.opengl.GL30.*;

public class Test {
    Frame wnd;
    TestPanel p;
    volatile boolean done = false;

    class TestPanel extends AWTGLCanvas {
	TestPanel() {
	    setSize(1024, 768);
	}

	public void initGL() {
	}

	public void paintGL() {
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
    }

    void run() {
	while(!done) {
	    p.runInContext(this::draw);
	}
    }

    void draw() {
	
    }

    public void main(String[] args) {
	Test t = new Test();
	t.run();
	System.exit(0);
    }
}
