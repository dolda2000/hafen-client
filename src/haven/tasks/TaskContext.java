package haven.tasks;

import haven.GameUI;
import haven.UI;

class TaskContext {
    private final UI ui;

    public TaskContext(UI ui) {
        this.ui = ui;
    }

    public GameUI gui() {
        return ui.gui;
    }
}
