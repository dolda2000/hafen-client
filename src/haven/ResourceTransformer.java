package haven;

import haven.transformers.CupboardFlattener;

import java.util.HashMap;
import java.util.Map;

public class ResourceTransformer {
    private static Map<String, Transformer> transformers;

    static {
        transformers = new HashMap<String, Transformer>();
        transformers.put("gfx/terobjs/cupboard", new CupboardFlattener());
    }

    public static void transform(Resource res) {
        Transformer t = transformers.get(res.name);
        if (t != null)
            t.transform(res);
    }

    public interface Transformer {
        void transform(Resource res);
    }
}
