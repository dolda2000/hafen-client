package haven;

import java.awt.event.KeyEvent;

public abstract class GrabberWnd extends Window implements MapView.Grabber {
    private final GameUI gui;
    private final Widget btn;
    private final MapView.Grabber grabber;
    private final Label label;

    public GrabberWnd(String title, String label, GameUI gui) {
        super(new Coord(150, 50), title);
        this.label = add(new Label(label));
        this.btn = new Button(40, "Done");
        add(btn, this.asz.sub(btn.sz));
        this.grabber = gui.map.new GrabXL(this);
        this.gui = gui;
        this.gui.map.enol(16);
        this.gui.map.grab(grabber);
    }

    protected final void setLabel(String text) {
        label.settext(text);
    }

    @Override
    public void show() {
        super.show();
        parent.setfocus(this);
    }

    public final void close() {
        this.cbtn.click();
    }

    protected abstract void done();

    @Override
    public void destroy() {
        gui.map.disol(16);
        gui.map.release(grabber);
        super.destroy();
    }

    public boolean globtype(char key, java.awt.event.KeyEvent ev) {
        if(key == KeyEvent.VK_ENTER) {
            if (!done) {
                done = true;
                done();
                close();
                return true;
            }
        }
        return false;
    }

    @Override
    public void wdgmsg(Widget sender, String msg, Object... args) {
        if((sender == this) && (msg.equals("close"))) {
            destroy();
        } else if (sender == this.btn) {
            done();
            close();
        } else {
            super.wdgmsg(sender, msg, args);
        }
    }
}
