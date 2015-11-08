package haven;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.*;

public class MinimapCache implements Disposable {
    private static final SimpleDateFormat datefmt = new SimpleDateFormat("yyyy-MM-dd HH.mm.ss");

    private final TextureAtlas atlas;
    private final TextureAtlas.Region[] pool;
    private final MinimapRenderer renderer;
    private String session;
    private Coord sp;
    private MCache.Grid cgrid = null;
    private int nextPoolIndex;

    private final Map<Coord, Defer.Future<BufferedImage>> rendering = new HashMap<Coord, Defer.Future<BufferedImage>>();
    private final Map<Coord, MinimapTile> tiles = new HashMap<Coord, MinimapTile>();
    private final Map<TextureAtlas.Region, Coord> used = new HashMap<TextureAtlas.Region, Coord>();

    public MinimapCache(MinimapRenderer renderer) {
        this.renderer = renderer;
        this.atlas = new TextureAtlas(1024, 1024);
        this.pool = new TextureAtlas.Region[100];
        for (int i = 0; i < this.pool.length; i++)
            this.pool[i] = this.atlas.add(100, 100);
        this.nextPoolIndex = 0;
    }

    @Override
    public void dispose() {
        atlas.dispose();
    }

    public MinimapTile get(final Coord c, final MCache.Grid grid, final int seq) {
        Coord key = new Coord(c);
        MinimapTile tile = tiles.get(key);
        if ((tile == null || tile.seq != seq) && (grid != null)) {
            Defer.Future<BufferedImage> f = rendering.get(key);
            if (f == null) {
                f = Defer.later(new Defer.Callable<BufferedImage>() {
                    @Override
                    public BufferedImage call() {
                        BufferedImage img = renderer.draw(grid.ul, MCache.cmaps);
                        if (Config.minimapEnableSave.get())
                            store(img, grid.gc);
                        return img;
                    }
                });
                rendering.put(key, f);
            }
            if (f.done()) {
                BufferedImage img = f.get();
                if (tile == null) {
                    tile = createTile(key, seq, img);
                    tiles.put(key, tile);
                } else {
                    tile.img.update(img);
                }
                rendering.remove(key);
            }
        }
        return tile;
    }

    public void checkSession(MCache.Grid grid) {
        // might be wrong, but it seems that character position is always (0, 0) after transitions
        if (cgrid == null || (grid.gc.x == 0 && grid.gc.y == 0 && cgrid.id != grid.id)) {
            // discard all cached tiles
            clearCaches();
            cgrid = grid;
            if (Config.minimapEnableSave.get()) {
                sp = grid.gc;
                session = datefmt.format(new Date(System.currentTimeMillis()));
                if (true) {
                    (new File(mapsessfolder())).mkdirs();
                    try {
                        Writer currentSessionFile = new FileWriter(mapfile("currentsession.js"));
                        currentSessionFile.write("var currentSession = '" + session + "';\n");
                        currentSessionFile.close();
                    } catch (IOException e) {}
                }
            }
        }
    }

    private void clearCaches() {
        tiles.clear();
        rendering.clear();
        used.clear();
        nextPoolIndex = 0;
    }

    private MinimapTile createTile(Coord c, int seq, BufferedImage image) {
        TextureAtlas.Region tex = pool[nextPoolIndex];

        // remove tile currently used for this region
        Coord old = used.remove(tex);
        if (old != null) {
            tiles.remove(old);
        }

        // use region for the new tile
        tex.update(image);
        used.put(tex, c);

        nextPoolIndex++;
        if (nextPoolIndex >= pool.length)
            nextPoolIndex = 0;

        return new MinimapTile(tex, seq);
    }

    private String mapfolder(){
        return String.format("%s/map/", ".");
    }

    private String mapfile(String file){
        return String.format("%s%s", mapfolder(), file);
    }

    private String mapsessfile(String file){
        return String.format("%s%s/%s", mapfolder(), session, file);
    }

    private String mapsessfolder(){
        return mapsessfile("");
    }

    private void store(BufferedImage img, Coord cg) {
        if (img == null || sp == null) return;
        String fileName = "";
        try {
            Coord c = cg.sub(sp);
            fileName = mapsessfile(String.format("tile_%d_%d.png", c.x, c.y));
            File file = new File(fileName);
            ImageIO.write(img, "png", file);
        } catch (Exception e) {
            System.out.println("Error saving tile: " + fileName);
            e.printStackTrace();
        }
    }
}
