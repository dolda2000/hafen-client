package haven.tasks;

import haven.*;

public class Forager extends FsmTask {
    private final String[] objectNames;
    private final int radius;
    private final int maxItemCount;
    private Gob obj;
    private Widget window;
    private int itemCount;

    public Forager(int radius, int itemCount, String... objectNames) {
        this.objectNames = objectNames;
        this.radius = radius;
        this.maxItemCount = itemCount;
    }

    @Override
    protected void onStop() {
        if (window != null)
            window.destroy();
    }

    @Override
    protected State getInitialState() {
        return new FindObject();
    }

    private class FindObject extends State {
        @Override
        public void tick(double dt) {
            obj = context().findObjectByNames(radius, objectNames);
            if (obj != null) {
                if (window == null && maxItemCount > 1)
                    window = context().gui().add(new StatusWindow(), 300, 200);
                context().click(obj, 3, 0);
                waitMenu(2, "Pick", new Callback<Boolean>() {
                    @Override
                    public void done(Boolean success) {
                        if (success)
                            setState(new WaitGobRemoval(obj.id, 5));
                        else
                            stop("Couldn't pick anything in time");
                    }
                });
            } else {
                if (window == null)
                    stop("Nothing to pick nearby");
                else
                    stop();
            }
        }
    }

    private class WaitGobRemoval extends State {
        private final long gobId;
        private final double timeout;
        private double t;

        public WaitGobRemoval(long gobId, double timeout) {
            this.gobId = gobId;
            this.timeout = timeout;
        }

        @Override
        public void tick(double dt) {
            Gob gob = context().findObjectById(gobId);
            if (gob == null) {
                itemCount++;
                if (itemCount == maxItemCount)
                    stop();
                else
                    waitTime(0.5, new FindObject()); // wait for animation to complete
            } else {
                t += dt;
                if (t > timeout) {
                    stop("Couldn't pick anything in time");
                }
            }
        }
    }

    private class StatusWindow extends Window {
        public StatusWindow() {
            super(Coord.z, "Picking...");
            setLocal(true);
            add(new Button(120, "Cancel") {
                public void click() {
                    Forager.this.stop();
                }
            });
            pack();
        }

        public void wdgmsg(Widget sender, String msg, Object... args) {
            if (sender == this && msg.equals("close")) {
                Forager.this.stop();
            }
            super.wdgmsg(sender, msg, args);
        }
    }
}
