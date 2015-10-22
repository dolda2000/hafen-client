package haven.tasks;

import haven.*;

public abstract class FeedTask extends Task {
    private final String objectName;
    private State state;
    private Gob gob;
    private String error;

    public FeedTask(String objectName) {
        this.objectName = objectName;
        this.state = new FindTrough();
    }

    protected abstract boolean isFiller(GItem item);

    @Override
    protected void onTick(double dt) {
        state.tick(dt);
    }

    @Override
    protected void onStop() {
        if (error != null && !error.isEmpty())
            context().error(error);
        else
            context().info("Task completed");
    }

    private void setState(State value) {
        this.state = value;
    }

    private void stop(String error) {
        this.error = error;
        stop();
    }

    private interface State {
        void tick(double dt);
    }

    private class FindTrough implements State {
        @Override
        public void tick(double dt) {
            gob = context().findObjectByName(50, objectName);
            if (gob != null) {
                setState(new FindFood());
            } else {
                stop("Couldn't find object to fill");
            }
        }
    }

    private class FindFood implements State {
        @Override
        public void tick(double dt) {
            // check if there is food at the hand already
            GItem item = context().getItemAtHand();
            if (item == null) {
                Inventory inv = context().playerInventory();
                for (Widget w = inv.child; w != null; w = w.next) {
                    if (w instanceof GItem && isFiller((GItem) w)) {
                        item = (GItem)w;
                        break;
                    }
                }
            } else if (!isFiller(item)) {
                stop("Item in the hand is not a filler");
                return;
            }
            if (item != null) {
                Utils.takeItem(item);
                setState(new WaitForItem(2));
            } else {
                stop("Couldn't find any filler");
            }
        }
    }

    private class WaitForItem implements State {
        private final double timeout;
        private double t;

        public WaitForItem(double timeout) {
            this.timeout = timeout;
        }

        @Override
        public void tick(double dt) {
            GItem item = context().getItemAtHand();
            if (item != null) {
                // click with shift
                context().itemact(gob, 1);
                setState(new Wait(0.2));
            } else {
                t += dt;
                if (t > timeout)
                    stop();
            }
        }
    }

    private class Wait implements State {
        private final double timeout;
        private double t;

        public Wait(double timeout) {
            this.timeout = timeout;
        }

        @Override
        public void tick(double dt) {
            t += dt;
            if (t > timeout)
                setState(new WaitForItem(2));
        }
    }
}
