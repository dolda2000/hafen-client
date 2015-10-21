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
    private Coord cgrid = null;

    private final Map<Coord, Defer.Future<MinimapTile>> cache =
            new LinkedHashMap<Coord, Defer.Future<MinimapTile>>(50, 0.75f, true) {
                protected boolean removeEldestEntry(Map.Entry<Coord, Defer.Future<MinimapTile>> eldest) {
                    if (size() > 900) {
                        try {
                            MinimapTile t = eldest.getValue().get();
                            t.img.dispose();
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

    public Defer.Future<MinimapTile> get(Coord gc) {
        final Coord tgc = new Coord(gc);
        Defer.Future<MinimapTile> f = cache.get(tgc);
        if(f == null) {
            f = Defer.later(new Defer.Callable<MinimapTile>() {
                @Override
                public MinimapTile call() {
                    Coord ul = tgc.mul(cmaps);
                    BufferedImage img = renderer.draw(ul, MCache.cmaps);
                    MinimapTile mapTile = new MinimapTile(new TexI(img), ul, tgc);
                    if (Config.minimapEnableSave.get())
                        store(img, tgc);
                    return mapTile;
                }
            });
            cache.put(tgc, f);
        }
        return f;
    }

    public void checkSession(Coord gc) {
        if (!Config.minimapEnableSave.get()) {
            cgrid = null;
            return;
        }
        if(cgrid == null || cgrid.manhattan(gc) > 5){
            sp = gc;
            synchronized (cache) {
                for (Defer.Future<MinimapTile> v : cache.values()) {
                    if(v != null && v.done()) {
                        MinimapTile tile = v.get();
                        if(tile != null && tile.img != null) {
                            tile.img.dispose();
                        }
                    }
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
        }
        cgrid = gc;
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
