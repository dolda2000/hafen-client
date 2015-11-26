package haven.tasks;

import haven.Callback;
import haven.FlowerMenu;

public abstract class FsmTask extends Task {
    private State state;

    @Override
    protected void onStart() {
        state = getInitialState();
        if (state != null)
            state.init();
    }

    @Override
    protected void onStop() {
        if (state != null)
            state.destroy();
    }

    @Override
    protected void onTick(double dt) {
        if (state != null)
            state.tick(dt);
    }

    protected abstract State getInitialState();

    protected final void setState(State newState) {
        if (state != null) state.destroy();
        state = newState;
        if (state != null) state.init();
    }

    protected final void waitTime(double timeout, State next) {
        setState(new Wait(timeout, next));
    }

    protected final void waitMenu(double timeout, String text, Callback<Boolean> callback) {
        setState(new WaitMenu(timeout, text, callback));
    }

    private class Wait extends State {
        private final State next;
        private final double timeout;
        private double t;

        public Wait(double timeout, State next) {
            this.next = next;
            this.timeout = timeout;
        }

        @Override
        public void tick(double dt) {
            t += dt;
            if (t > timeout)
                setState(next);
        }
    }

    private class WaitMenu extends State {
        private final String text;
        private final double timeout;
        private final Callback<Boolean> callback;
        private double t;

        public WaitMenu(double timeout, String text, Callback<Boolean> callback) {
            this.text = text;
            this.timeout = timeout;
            this.callback = callback;
        }

        @Override
        public void tick(double dt) {
            FlowerMenu menu = context().getMenu();
            if (menu != null) {
                for (FlowerMenu.Petal opt : menu.opts) {
                    if (opt.name.equals(text)) {
                        menu.choose(opt);
                        menu.destroy();
                        callback.done(true);
                        return;
                    }
                }
            } else {
                t += dt;
                if (t > timeout) {
                    callback.done(false);
                }
            }
        }
    }

    public static abstract class State {
        public void init() {}
        public void destroy() {}
        public abstract void tick(double dt);
    }
}
