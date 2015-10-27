package haven.tasks;

import java.util.List;

public class AddKinsTask extends Task {
    private final List<String> secrets;
    private int cur;
    private double t = 0.1;

    public AddKinsTask(List<String> secrets) {
        this.secrets = secrets;
    }

    @Override
    protected void onStop() {
        context().info(String.format("Added %d secrets", secrets.size()));
    }

    @Override
    protected void onTick(double dt) {
        if (cur < secrets.size()) {
            if (t >= 0.1) { // every 100 ms
                context().gui().buddies.wdgmsg("bypwd", secrets.get(cur));
                cur++;
                t = 0;
            } else {
                t += dt;
            }
        } else {
            stop();
        }
    }
}
