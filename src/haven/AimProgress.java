package haven;

import java.awt.*;
import java.lang.reflect.Field;
import java.text.DecimalFormat;

public class AimProgress extends Widget {
    private static final DecimalFormat format = new DecimalFormat("#.##");

    private final Widget aim;
    private double lastValue = 0;
    private Field valueField = null;
    private Text text;

    public AimProgress(Widget aim) {
        this.aim = aim;
        try {
            Class cl = aim.getClass();
            valueField = cl.getDeclaredField("val");
            valueField.setAccessible(true);
            text = Text.renderstroked("0%", Color.WHITE, Color.BLACK);
        } catch (Exception e) {
            text = null;
            e.printStackTrace();
        }
        resize(aim.sz);
    }

    @Override
    public void draw(GOut g) {
        if (valueField == null)
            return;
        try {
            this.c = aim.c.add(0, 2);
            double value = (Double)valueField.get(aim);
            if (value != lastValue) {
                lastValue = value;
                text = Text.renderstroked(format.format(value * 100) + "%",  Color.WHITE, Color.BLACK);
            }
            g.aimage(text.tex(), sz.div(2), 0.5, 0.5);
        } catch (IllegalAccessException ignore) {
        }
    }
}
