package haven.tasks;

import haven.*;

public abstract class FeedTask extends Task {
    private final String objectName;
    private final int maxItemCount;
    private State state;
    private Gob gob;
    private String error;
    private int itemCount;

    public FeedTask(String objectName, int maxItemCount) {
        this.objectName = objectName;
        this.state = new FindTrough();
        this.maxItemCount = maxItemCount;
    }

    public FeedTask(String objectName) {
        this(objectName, -1);
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
            context().info("Done");
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
            // check if there is filler at the hand already
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
                itemCount++;
                if (!isLastItem()) {
                    // click with shift
                    context().itemact(gob, 1);
                    setState(new Wait(0.2));
                } else {
                    context().itemact(gob, 0);
                    stop();
                }

            } else {
                t += dt;
                if (t > timeout) {
                    if (!isLastItem())
                        error = String.format("Only %d out of %d items were put", itemCount, maxItemCount);
                    stop();
                }
            }
        }

        private boolean isLastItem() {
            return (maxItemCount != -1) && (maxItemCount == itemCount);
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
