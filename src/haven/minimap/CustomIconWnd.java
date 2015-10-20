package haven.minimap;

import haven.*;

public class CustomIconWnd extends Window {
    private final CheckBoxList groups;
    private final CheckBoxList matches;
    private CustomIconConfig config;

    public CustomIconWnd() {
        super(Coord.z, "Toggle...");
        setLocal(true);
        setHideOnClose(true);

        add(new Label("Groups:"), 0, 0);
        add(new Label("Objects:"), 160, 0);
        groups = add(new CheckBoxList(150, 20), 0, 15);
        matches = add(new CheckBoxList(150, 20), 160, 15);

        add(new Button(310, "Apply") {
            public void click() {
                config.save();
                ui.sess.glob.icons.reset();
            }
        }, 0, groups.sz.y + 15);

        add(new Button(310, "Save & close") {
            public void click() {
                config.save();
                ui.sess.glob.icons.reset();
                CustomIconWnd.this.hide();
            }
        }, 0, groups.sz.y + 45);
        pack();

        groups.addListener(new CheckBoxListAdapter() {
            public void itemSelected(int index) {
                matches.clear();
                if (index == -1)
                    return;
                CustomIconGroup group = config.groups.get(index);
                for (CustomIconMatch match : group.matches)
                    matches.addItem(match.name, match.show);
            }

            public void itemChecked(int index, boolean checked) {
                CustomIconGroup group = config.groups.get(index);
                group.show = checked;
            }
        });

        matches.addListener(new CheckBoxListAdapter() {
            public void itemChecked(int index, boolean checked) {
                if (index == -1)
                    return;
                CustomIconGroup group = config.groups.get(groups.selindex);
                CustomIconMatch match = group.matches.get(index);
                match.show = checked;
            }
        });
    }

    @Override
    protected void attach(UI ui) {
        super.attach(ui);
    }

    @Override
    public void show() {
        super.show();
        config = ui.sess.glob.icons.config;
        groups.clear();
        for (CustomIconGroup group : config.groups)
            groups.addItem(group.name, group.show);
        if (config.groups.size() > 0)
            groups.change(0);
        raise();
    }
}
