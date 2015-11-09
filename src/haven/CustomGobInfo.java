package haven;

public class CustomGobInfo extends GAttrib {
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

    private final InfoCache<Drawable> replacement = new InfoCache<Drawable>() {
        protected Drawable getValue() {
            Resource res = gob.getres();
            if (res != null) {
                try {
                    // is tree?
                    if (res.name.startsWith("gfx/terobjs/trees") && !(res.name.endsWith("log") || res.name.endsWith("stump"))) {
                        Resource stump = Resource.remote().loadwait(res.name + "stump");
                        return new ResDrawable(gob, stump);
                    }
                } catch (Exception e) {
                    return null;
                }
            }
            return null;
        }
    };

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
        return Config.hideModeEnabled.get() ? replacement.get() : null;
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
}
