package haven.tasks;

import haven.*;

public abstract class FeedTask extends FsmTask {
    private final String objectName;
    private final int maxItemCount;
    private Gob gob;
    private int itemCount;

    public FeedTask(String objectName, int maxItemCount) {
        this.objectName = objectName;
        this.maxItemCount = maxItemCount;
    }

    public FeedTask(String objectName) {
        this(objectName, -1);
    }

    protected abstract boolean isFiller(GItem item);

    @Override
    protected State getInitialState() {
        return new FindTrough();
    }

    private class FindTrough extends State {
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

    private class FindFood extends State {
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

    private class WaitForItem extends State {
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
                    waitTime(0.1, new WaitForItem(2));
                } else {
                    context().itemact(gob, 0);
                    stop();
                }

            } else {
                t += dt;
                if (t > timeout) {
                    if ((maxItemCount != -1) && (maxItemCount != itemCount))
                        stop(String.format("Only %d out of %d items were put", itemCount, maxItemCount));
                    else
                        stop();
                }
            }
        }

        private boolean isLastItem() {
            return (maxItemCount != -1) && (maxItemCount == itemCount);
        }
    }
}
