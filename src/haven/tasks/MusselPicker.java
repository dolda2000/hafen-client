package haven.tasks;

import haven.*;

public class MusselPicker extends Task {
    private State state;
    private Gob mussel;
    private Widget window;
    private String error;

    @Override
    protected void onStart() {
        state = new FindMussel();
    }

    @Override
    protected void onStop() {
        if (window != null)
            window.destroy();
        if (error != null && !error.isEmpty())
            context().error(error);
        else
            context().info("Task completed");
    }

    @Override
    protected void onTick(double dt) {
        this.state.tick(dt);
    }

    private void setState(State value) {
        this.state = value;
    }

    private interface State {
        void tick(double dt);
    }

    private class FindMussel implements State {
        @Override
        public void tick(double dt) {
            mussel = context().findObjectByName(200, "mussels");
            if (mussel != null) {
                if (window == null)
                    window = context().gui().add(new StatusWindow(), 300, 200);
                context().click(mussel, 3, 0);
                setState(new WaitMenu("Pick", 2));
            } else {
                if (window == null)
                    error = "No mussels nearby";
                stop();
            }
        }
    }

    private class WaitMenu implements State {
        private final String text;
        private final double timeout;
        private double t;

        public WaitMenu(String text, double timeout) {
            this.text = text;
            this.timeout = timeout;
        }

        @Override
        public void tick(double dt) {
            FlowerMenu menu = context().getMenu();
            if (menu != null) {
                for (FlowerMenu.Petal opt : menu.opts) {
                    if (opt.name.equals(text)) {
                        menu.choose(opt);
                        menu.destroy();
                        setState(new WaitGobRemoval(mussel.id, 5));
                        return;
                    }
                }
            } else {
                t += dt;
                if (t > timeout) {
                    error = "Couldn't pick mussel in time";
                    stop();
                }
            }
        }
    }

    private class WaitGobRemoval implements State {
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
                setState(new Wait(0.5)); // wait for animation to complete
            } else {
                t += dt;
                if (t > timeout) {
                    error = "Couldn't pick mussel in time";
                    stop();
                }
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
                setState(new FindMussel());
        }
    }

    private class StatusWindow extends Window {
        public StatusWindow() {
            super(Coord.z, "Picking mussels...");
            setLocal(true);
            add(new Button(120, "Cancel") {
                public void click() {
                    MusselPicker.this.stop();
                }
            });
            pack();
        }

        public void wdgmsg(Widget sender, String msg, Object... args) {
            if (sender == this && msg.equals("close")) {
                MusselPicker.this.stop();
            }
            super.wdgmsg(sender, msg, args);
        }
    }
}
