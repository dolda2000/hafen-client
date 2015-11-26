package haven.transformers;

import haven.*;

import java.util.ArrayList;
import java.util.List;

public class CupboardFlattener implements ResourceTransformer.Transformer {

    private static final float factor = 0.1f;
    private static final double angle = Math.PI / 2; // rotation angle
    private static final double ca = Math.cos(angle);
    private static final double sa = Math.sin(angle);

    public void transform(Resource res) {
        int scale = Config.cupboardScale.get();
        if (scale >= 100 || scale < 10)
            return;

        boolean rotate = scale < 30;
        boolean unclip = scale <= 15;

        if (rotate) {
            // rotate animations
            for (Skeleton.ResPose pose : res.layers(Skeleton.ResPose.class)) {
                for (Skeleton.Track track : pose.tracks) {
                    if (track.frames.length > 2) {
                        double a0 = track.frames[0].rang;
                        double an = track.frames[track.frames.length - 1].rang;
                        for (Skeleton.Track.Frame frame : track.frames) {
                            float rang = unclip ? (float) (a0 + (an - a0) * (frame.time / pose.len)) : frame.rang;
                            // swap rotation axis
                            frame.rot = Skeleton.rotasq(new float[4], new float[]{
                                    Math.abs(frame.rax[2]),
                                    frame.rax[1],
                                    frame.rax[0]}, rang);
                        }
                    }
                }
            }
        }
        // process vertices
        VertexBuf.VertexRes vbuf = res.layer(VertexBuf.VertexRes.class);
        if (vbuf != null) {
            for (VertexBuf.AttribArray array : vbuf.b.bufs) {
                if (array instanceof VertexBuf.VertexArray) {
                    VertexBuf.VertexArray va = (VertexBuf.VertexArray)array;
                    List<Coord3f> vertices = new ArrayList<Coord3f>();

                    // bounding box
                    Coord3f min = new Coord3f(Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE);
                    Coord3f max = new Coord3f(Float.MIN_VALUE, Float.MIN_VALUE, Float.MIN_VALUE);

                    va.data.rewind();
                    while (va.data.hasRemaining()) {
                        Coord3f v = new Coord3f(va.data.get(), va.data.get(), va.data.get());
                        min.x = Math.min(min.x, v.x);
                        min.y = Math.min(min.y, v.y);
                        min.z = Math.min(min.z, v.z);
                        max.x = Math.max(max.x, v.x);
                        max.y = Math.max(max.y, v.y);
                        max.z = Math.max(max.z, v.z);
                        vertices.add(v);
                    }

                    // find multiplier to make cupboard square
                    float w = Math.abs(max.y - min.y);
                    float h = Math.abs(max.z - min.z);
                    float k = w / h;

                    float sx = rotate ? (scale / 100.0f) : 1.0f;
                    float sz = rotate ? k : (scale / 100.0f);

                    // make basis at 0, 0, 0
                    for (Coord3f v : vertices) {
                        v.x -= min.x;
                        v.y -= min.y;
                        v.z -= min.z;
                    }

                    // transform vertices
                    for (Coord3f v : vertices) {
                        v.x = v.x * sx;
                        v.z = v.z * sz;
                        if (rotate) {
                            float ox = v.x;
                            float oz = v.z;
                            v.x = (float) (ox * ca - oz * sa) + max.z * k;
                            v.z = (float) (ox * sa + oz * ca);
                        }
                    }

                    // write transformed vertex data back
                    va.data.rewind();
                    for (Coord3f v : vertices) {
                        va.data.put(v.x + min.x);
                        va.data.put(v.y + min.y);
                        va.data.put(v.z + min.z);
                    }

                    // transform skeleton
                    for (Skeleton.Res sk : res.layers(Skeleton.Res.class)) {
                        for (Skeleton.Bone bone : sk.s.bones.values()) {
                            // scale
                            float x = bone.ipos.x - min.x;
                            float z = bone.ipos.z - min.z;
                            x *= sx;
                            z *= sz;
                            if (rotate) {
                                //rotate
                                float rx = (float) (x * ca - z * sa);
                                float rz = (float) (x * sa + z * ca);
                                x = rx;
                                z = rz;
                                // swap axis
                                float tmp = bone.irax.x;
                                bone.irax.x = bone.irax.z;
                                bone.irax.z = tmp;
                            }
                            bone.ipos.x = min.x + x;
                            bone.ipos.z = min.z + z;
                        }
                    }
                }
            }
        }
    }
}
