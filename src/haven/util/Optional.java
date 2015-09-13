package haven.util;

public class Optional<T> {
    private final T value;

    protected Optional(T value) {
        this.value = value;
    }

    public static <T> Optional<T> of(T value) {
        return new Optional<T>(value);
    }

    public static <T> Optional<T> empty() {
        return new Optional<T>(null);
    }

    public T getValue() {
        return value;
    }

    public boolean hasValue() {
        return (value != null);
    }
}
