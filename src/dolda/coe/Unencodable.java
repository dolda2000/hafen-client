package dolda.coe;

public class Unencodable {
    public final Class<?> cl;
    public final Throwable cause;

    public Unencodable(Class<?> cl, Throwable cause) {
	this.cl = cl;
	this.cause = cause;
    }
}
