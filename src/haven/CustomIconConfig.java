package haven;

public class CustomIconConfig {
    private boolean enabled;

    public CustomIconConfig() {
        enabled = Config.getCustomIconsEnabled();
    }

    public boolean enabled() {
        return enabled;
    }

    public void toggle() {
        enabled = !enabled;
        Config.setCustomIconsEnabled(enabled);
    }
}
