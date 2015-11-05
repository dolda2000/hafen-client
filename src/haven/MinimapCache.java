package haven;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

import static haven.MCache.cmaps;

public class MinimapCache {
    private static final SimpleDateFormat datefmt = new SimpleDateFormat("yyyy-MM-dd HH.mm.ss");

    private final MinimapRenderer renderer;
    private String session;
    private Coord sp;
    private MCache.Grid cgrid = null;

    private final Map<Pair<MCache.Grid, Integer>, Defer.Future<MinimapTile>> cache =
            new LinkedHashMap<Pair<MCache.Grid, Integer>, Defer.Future<MinimapTile>>(50, 0.75f, true) {
                protected boolean removeEldestEntry(Map.Entry<Pair<MCache.Grid, Integer>, Defer.Future<MinimapTile>> eldest) {
                    if (size() > 20) {
                        try {
                            MinimapTile t = eldest.getValue().get();
                            t.img.dispose();
                            eldest.getValue().cancel();
                        } catch(RuntimeException e) {
                        }
                        return(true);
                    }
                    return(false);
                }
            };

    public MinimapCache(MinimapRenderer renderer) {
        this.renderer = renderer;
    }

    public Defer.Future<MinimapTile> get(final MCache.Grid grid, final int seq) {
        Defer.Future<MinimapTile> f = cache.get(new Pair<MCache.Grid, Integer>(grid, seq));
        if(f == null) {
            f = Defer.later(new Defer.Callable<MinimapTile>() {
                @Override
                public MinimapTile call() {
                    BufferedImage img = renderer.draw(grid);
                    MinimapTile mapTile = new MinimapTile(new TexI(img), grid.ul, grid, seq);
                    if (Config.minimapEnableSave.get())
                        store(img, grid.gc);
                    return mapTile;
                }
            });
            cache.put(new Pair<MCache.Grid, Integer>(grid, seq), f);
        }
        return f;
    }

    public boolean checkSession(MCache.Grid grid) {
        if (!Config.minimapEnableSave.get()) {
            cgrid = null;
            return false;
        }
        // might be wrong, but it seems that character position is always (0, 0) after transitions
        if (cgrid == null || (grid.gc.x == 0 && grid.gc.y == 0 && cgrid.id != grid.id)) {
            sp = grid.gc;
            synchronized (cache) {
                for (Defer.Future<MinimapTile> v : cache.values()) {
                    if(v != null && v.done()) {
                        MinimapTile tile = v.get();
                        if(tile != null && tile.img != null) {
                            tile.img.dispose();
                        }
                    } else if (v != null)
                        v.cancel();
                }
                cache.clear();
            }
            session = datefmt.format(new Date(System.currentTimeMillis()));
            if (true) {
                (new File(mapsessfolder())).mkdirs();
                try {
                    Writer currentSessionFile = new FileWriter(mapfile("currentsession.js"));
                    currentSessionFile.write("var currentSession = '" + session + "';\n");
                    currentSessionFile.close();
                } catch (IOException e) {}
            }
            cgrid = grid;
            return true;
        }
        return false;
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
        if (img == null) return;
        Coord c = cg.sub(sp);
        String fileName = mapsessfile(String.format("tile_%d_%d.png", c.x, c.y));
        File outputfile = new File(fileName);
        try {
            ImageIO.write(img, "png", outputfile);
        } catch (IOException e) {}
    }
}
