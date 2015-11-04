package haven.tasks;

public abstract class Task {
    private TaskContext context;
    private TaskListener listener;
    private boolean stopped;
    private String error;

    final void start() {
        onStart();
    }

    public final void stop() {
        stopped = true;
    }

    final void tick(double dt) {
        if (!stopped) {
            onTick(dt);
        } else {
            onStop();
            if (listener != null)
                listener.stopped();
            if (error != null && !error.isEmpty())
                context().error(error);
        }
    }

    final TaskContext context() {
        return context;
    }

    final void setContext(TaskContext value) {
        this.context = value;
    }

    final void setListener(TaskListener listener) {
        this.listener = listener;
    }

    final void stop(String error) {
        this.error = error;
        stop();
    }

    protected void onStart() {}
    protected void onStop() {}
    protected void onTick(double dt) {}
}
