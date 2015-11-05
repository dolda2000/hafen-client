package haven;

public class MinimapTile {
    public final Tex img;
    public final Coord ul;
    public final MCache.Grid grid;
    public final int seq;

    public MinimapTile(Tex img, Coord ul, MCache.Grid grid, int seq) {
        this.img = img;
        this.ul = ul;
        this.grid = grid;
        this.seq = seq;
    }
}
