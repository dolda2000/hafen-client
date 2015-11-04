package haven;

import java.awt.event.KeyEvent;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class NumEntry extends TextEntry {
    private static final Set<Integer> NUM_KEYS = new HashSet<Integer>(Arrays.asList(
            KeyEvent.VK_0, KeyEvent.VK_1, KeyEvent.VK_2, KeyEvent.VK_3, KeyEvent.VK_4,
            KeyEvent.VK_5, KeyEvent.VK_6, KeyEvent.VK_7, KeyEvent.VK_8, KeyEvent.VK_9,
            KeyEvent.VK_NUMPAD0, KeyEvent.VK_NUMPAD1, KeyEvent.VK_NUMPAD2, KeyEvent.VK_NUMPAD3, KeyEvent.VK_NUMPAD4,
            KeyEvent.VK_NUMPAD5, KeyEvent.VK_NUMPAD6, KeyEvent.VK_NUMPAD7, KeyEvent.VK_NUMPAD8, KeyEvent.VK_NUMPAD9
    ));
    private static final Set<Integer> CONTROL_KEYS = new HashSet<Integer>(Arrays.asList(
        KeyEvent.VK_LEFT, KeyEvent.VK_RIGHT,
        KeyEvent.VK_ENTER, KeyEvent.VK_BACK_SPACE, KeyEvent.VK_DELETE
    ));

    private final int maxLength;

    public NumEntry(int w, Integer defaultValue, int maxLength) {
        super(w, defaultValue.toString());
        this.maxLength = maxLength;
    }

    public int getValue() {
        if (text != null && !text.isEmpty())
            return Integer.parseInt(text);
        return 0;
    }

    @Override
    public boolean type(char c, KeyEvent ev) {
        int keyCode = ev.getKeyCode();
        if (keyCode == 0) {
            keyCode = ev.getKeyChar();
        }
        if (isControlKey(keyCode) || (this.text.length() < maxLength && isNumKey(keyCode))) {
            return super.type(c, ev);
        } else {
            return false;
        }
    }

    private static boolean isNumKey(int key) {
        return NUM_KEYS.contains(key);
    }

    private static boolean isControlKey(int key) {
        return CONTROL_KEYS.contains(key);
    }
}
