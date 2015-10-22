package haven.tasks;

import haven.UI;

import java.util.ArrayList;
import java.util.List;

public class TaskManager {
    private final List<Task> tasks = new ArrayList<Task>();
    private final TaskContext context;

    public TaskManager(UI ui) {
        context = new TaskContext(ui);
    }

    public void add(final Task task) {
        tasks.add(task);
        task.setContext(context);
        task.setListener(new TaskListener() {
            @Override
            public void stopped() {
                tasks.remove(task);
            }
        });
        task.start();
    }

    public void tick(double dt) {
        for (Task task : tasks)
            task.tick(dt);
    }
}
