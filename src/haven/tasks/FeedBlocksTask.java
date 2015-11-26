package haven.tasks;

import haven.GItem;

public class FeedBlocksTask extends FeedTask {
    private final static String prefix = "gfx/invobjs/wblock-";

    public FeedBlocksTask(String objectName) {
        super(objectName);
    }

    @Override
    protected boolean isFiller(GItem item) {
        return item.resname().startsWith(prefix);
    }
}
