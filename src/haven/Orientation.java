package haven;

public enum Orientation {
    Horizontal,
    Vertical;

    public Orientation invert() {
        switch (this) {
            case Horizontal:
                return Vertical;
            case Vertical:
                return Horizontal;
            default:
                return null;
        }
    }
}
