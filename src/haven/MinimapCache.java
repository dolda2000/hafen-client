package haven;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class MinimapCache {
    private static final SimpleDateFormat datefmt = new SimpleDateFormat("yyyy-MM-dd HH.mm.ss");

    private final MinimapRenderer renderer;
    private String session;
    private Coord sp;
    private MCache.Grid cgrid = null;

    private final Map<Coord, Defer.Future<MinimapTile>> rendering = new HashMap<Coord, Defer.Future<MinimapTile>>();
    private final Map<Coord, MinimapTile> tiles = new LinkedHashMap<Coord, MinimapTile>(100, 0.75f, true) {
        protected boolean removeEldestEntry(Map.Entry<Coord, MinimapTile> eldest) {
            if (size() > 100) {
                eldest.getValue().img.dispose();
                return(true);
            }
            return(false);
        }
    };


    public MinimapCache(MinimapRenderer renderer) {
        this.renderer = renderer;
    }

    public MinimapTile get(final Coord c, final MCache.Grid grid, final int seq) {
        Coord key = new Coord(c);
        MinimapTile tile = tiles.get(key);
        if ((tile == null || tile.seq != seq) && (grid != null)) {
            Defer.Future<MinimapTile> f = rendering.get(key);
            if (f == null) {
                f = Defer.later(new Defer.Callable<MinimapTile>() {
                    @Override
                    public MinimapTile call() {
                        BufferedImage img = renderer.draw(grid);
                        MinimapTile mapTile = new MinimapTile(new TexI(img), seq);
                        if (Config.minimapEnableSave.get())
                            store(img, grid.gc);
                        return mapTile;
                    }
                });
                rendering.put(key, f);
            }
            if (f.done()) {
                tile = f.get();
                rendering.remove(key);
                tiles.put(key, tile);
            }
        }
        return tile;
    }

    public void checkSession(MCache.Grid grid) {
        // might be wrong, but it seems that character position is always (0, 0) after transitions
        if (cgrid == null || (grid.gc.x == 0 && grid.gc.y == 0 && cgrid.id != grid.id)) {
            // discard all cached tiles
            clearCache();
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

    private void clearCache() {
        for (MinimapTile tile : tiles.values())
            tile.img.dispose();
        tiles.clear();

        for (Defer.Future<MinimapTile> v : rendering.values()) {
            if (v != null && v.done()) {
                MinimapTile t = v.get();
                if (t != null && t.img != null) {
                    t.img.dispose();
                }
            }
        }
        rendering.clear();
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
        Coord c = cg.sub(sp);
        String fileName = mapsessfile(String.format("tile_%d_%d.png", c.x, c.y));
        File outputfile = new File(fileName);
        try {
            ImageIO.write(img, "png", outputfile);
        } catch (IOException e) {}
    }
}
