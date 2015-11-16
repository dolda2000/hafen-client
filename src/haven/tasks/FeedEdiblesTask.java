package haven.tasks;

import haven.GItem;

public class FeedEdiblesTask extends FeedTask {
    private static final String[] edibles = {
            "gfx/invobjs/beet",
            "gfx/invobjs/beetleaves",
            "gfx/invobjs/carrot",
            "gfx/invobjs/straw",
            "gfx/invobjs/seed-flax",
            "gfx/invobjs/seed-hemp",
            "gfx/invobjs/seed-grape",
            "gfx/invobjs/seed-tobacco",
            "gfx/invobjs/seed-pipeweed",
            "gfx/invobjs/seed-pumpkin",
            "gfx/invobjs/pumpkin",
            "gfx/invobjs/pumpkinflesh",
            "gfx/invobjs/seed-carrot",
            "gfx/invobjs/yellowonion",
            "gfx/invobjs/seed-barley"
    };

    public FeedEdiblesTask(String objectName) {
        super(objectName);
    }

    @Override
    protected boolean isFiller(GItem item) {
        String resName = item.resname();
        if (resName != null && !resName.isEmpty()) {
            for (String food : edibles)
                if (resName.contains(food))
                    return true;
        }
        return false;
    }
}
