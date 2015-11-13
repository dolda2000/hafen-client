package haven;

public class CustomGobInfo extends GAttrib {
    private static final Indir<Resource> minitower = Resource.remote().load("gfx/terobjs/arch/custom/stonetower");

    private final InfoCache<Class> spriteClass = new InfoCache<Class>() {
        protected Class getValue() {
            Resource res = gob.getres();
            if (res != null) {
                Resource.CodeEntry ce = res.layer(Resource.CodeEntry.class);
                if (ce != null) {
                    return ce.getClassByTag("spr", false);
                }
            }
            return null;
        }
    };

    private final InfoCache<CropInfo> cropInfo = new InfoCache<CropInfo>() {
        protected CropInfo getValue() {
            if (hasSpriteKind("GrowingPlant") || hasSpriteKind("TrellisPlant")) {
                int stage = 0;
                int maxStage = 0;
                for (FastMesh.MeshRes layer : gob.getres().layers(FastMesh.MeshRes.class)) {
                    if (layer.id / 10 > maxStage)
                        maxStage = layer.id / 10;
                }
                Message data = GobInfo.getDrawableData(gob);
                if (data != null)
                    stage = data.uint8();
                return new CropInfo(stage, maxStage);
            }
            if (!spriteClass.hasValue)
                throw new Loading();
            return CropInfo.empty;
        }
    };

    private final InfoCache<Boolean> isTree = new InfoCache<Boolean>() {
        protected Boolean getValue() {
            Resource res = gob.getres();
            return (res != null) && res.name.startsWith("gfx/terobjs/trees") && !(res.name.endsWith("log") || res.name.endsWith("stump"));
        }
    };

    private final InfoCache<Boolean> isTower = new InfoCache<Boolean>() {
        protected Boolean getValue() {
            Resource res = gob.getres();
            return (res != null) && res.name.equals("gfx/terobjs/arch/stonetower");
        }
    };

    private Drawable replacement;

    public CustomGobInfo(Gob gob) {
        super(gob);
    }

    public boolean isCrop() {
        CropInfo info = cropInfo.get();
        return (info != null) && (info != CropInfo.empty);
    }

    public boolean isHidden() {
        return Config.hideModeEnabled.get() && isCrop();
    }

    public Drawable getReplacement() {
        if (Config.hideModeEnabled.get() && getValueOrDefault(isTree.get())) {
            if (replacement == null) {
                try {
                    Resource stump = Resource.remote().loadwait(gob.getres().name + "stump");
                    replacement = new ResDrawable(gob, stump);
                } catch (Exception e) {
                    // no stump -> replace with itself
                    replacement = gob.getattr(ResDrawable.class);
                }
            }
            return replacement;
        }
        if (Config.displayMiniTowers.get() && getValueOrDefault(isTower.get())) {
            if (replacement == null) {
                ResDrawable d = gob.getattr(ResDrawable.class);
                MessageBuf buf = d.sdt.clone();
                buf.rewind();
                replacement = new ResDrawable(gob, minitower, buf);
            }
            return replacement;
        }
        return null;
    }

    public boolean hasSpriteKind(String kind) {
        Class cl = spriteClass.get();
        return (cl != null) && (cl.getSimpleName().equals(kind) || cl.getSuperclass().getSimpleName().equals(kind));
    }

    private abstract class InfoCache<T> {
        private boolean hasValue;
        private T value;

        protected abstract T getValue();

        public T get() {
            if (!hasValue) {
                try {
                    value = getValue();
                    hasValue = true;
                } catch (Loading e) {
                }
            }
            return value;
        }
    }

    private static class CropInfo {
        public static final CropInfo empty = new CropInfo(-1, -1);

        public final int stage;
        public final int maxStage;

        private CropInfo(int stage, int maxStage) {
            this.stage = stage;
            this.maxStage = maxStage;
        }
    }

    private static boolean getValueOrDefault(Boolean b) {
        return (b != null) && b;
    }
}
