package haven.tasks;

import haven.GItem;

public class FeedCoalTask extends FeedTask {

    public FeedCoalTask(String objectName, int count) {
        super(objectName, count);
    }

    @Override
    protected boolean isFiller(GItem item) {
        String resName = item.resname();
        return "gfx/invobjs/coal".equals(resName) || "gfx/invobjs/blackcoal".equals(resName);
    }
}
