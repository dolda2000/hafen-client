package haven;

import haven.States.ColState;

import java.awt.*;

public class GobPath extends Sprite {
    private static final GobPathConfig config = new GobPathConfig();

    private Moving move = null;
    private Gob gob;

    public GobPath(Gob gob) {
	super(gob, null);
	this.gob = gob;
    }

    private String resname() {
	try {
	    Resource res = gob.getres();
	    if(res != null) {
		return res.name;
	    }
	} catch (Resource.Loading ignored) { }
	return null;
    }

    public void draw(GOut g) {
	Coord t = target();
	if(t == null) {return;}
	boolean good = false;
	Coord td = Coord.z;
	int tz = 0;
	try {
	    Coord ss = new Coord((int) (t.x - gob.loc.c.x), (int) (t.y + gob.loc.c.y));
	    td = ss.rotate(-gob.a);
	    tz = (int) (gob.glob.map.getcz(t) - gob.glob.map.getcz(gob.rc)) + 1;
	    good = true;
	} catch (Exception ignored) { }
	if(!good) { return; }

	g.apply();
	BGL gl = g.gl;
	gl.glLineWidth(2.0F);
	gl.glBegin(1);
	gl.glVertex3i(0, 0, 3);
	gl.glVertex3i(td.x, td.y, tz);
	gl.glEnd();
	GOut.checkerr(gl);
    }

    private Coord target() {
	Moving move = move();
	if(move != null) {
	    Class<? extends GAttrib> aClass = move.getClass();
	    if(aClass == LinMove.class) {
		return ((LinMove) move).t;
	    } else if(aClass == Homing.class) {
		return getGobCoords(((Homing) move).tgt());
	    } else if(aClass == Following.class) {
		return getGobCoords(((Following) move).tgt());
	    }
	}
	return null;
    }

    private Coord getGobCoords(Gob gob) {
	if(gob != null) {
	    Gob.GobLocation loc = gob.loc;
	    if(loc != null) {
		Coord3f c = loc.c;
		if(c != null) {
		    return new Coord((int) c.x, -(int) c.y);
		}
	    }
	}
	return null;
    }

    public boolean setup(RenderList list) {
	GobPathConfig.Entry cfg = config.get(resname());
	if (!cfg.show) { return false;}
	Color color = cfg.color;
	KinInfo ki = gob.getattr(KinInfo.class);
	if(ki != null) {
	    color = BuddyWnd.gc[ki.group];
	}
	list.prepo(new ColState(color));
    list.prepo(last);
    list.prepo(States.ndepthtest);
    return true;
    }

    public synchronized Moving move() {
	return move;
    }

    public synchronized void move(Moving m) {
	move = m;
    }

    public synchronized void stop() {
	move = null;
    }
}