package haven.render.gl;

import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicReference;

import haven.render.*;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/* Regression coverage for StreamFiller — the proxy/copy machinery the
 * STREAM prepare path uses to route Filler writes into a pre-allocated
 * StreamBuffer.Fill. The original ClassCastException at GLEnvironment
 * line ~593 happened because the fast path depended on buf.ro publication
 * order; the proxy here breaks that coupling. Test 1 (whole-range
 * fillbuf) is the direct regression guard for that bug. */
public class StreamFillerTest {

    /* --- Fakes --- */

    private static class FakeBuffer implements DataBuffer {
	final int sz;
	FakeBuffer(int sz) {this.sz = sz;}
	public int size() {return(sz);}
    }

    /* A FillBuffer impl that records pull() bytes and dispose() calls. */
    private static class FakeFill implements FillBuffer {
	final int sz;
	final ByteBuffer data;
	boolean disposed = false;
	final java.util.List<byte[]> pulls = new java.util.ArrayList<>();
	FakeFill(int sz) {this.sz = sz; this.data = ByteBuffer.allocate(sz);}
	public int size() {return(sz);}
	public boolean compatible(Environment env) {return(true);}
	public ByteBuffer push() {return(data);}
	public void pull(ByteBuffer buf) {
	    byte[] b = new byte[buf.remaining()];
	    buf.duplicate().get(b);
	    pulls.add(b);
	    data.position(0);
	    data.put(b);
	}
	public void dispose() {disposed = true;}
    }

    /* Back environment used as the fall-through. Only fillbuf matters
     * for these tests; everything else throws so unintended use is loud. */
    private static class FakeBackEnv implements Environment {
	final java.util.List<Object[]> fillbufCalls = new java.util.ArrayList<>();
	java.util.function.BiFunction<DataBuffer, int[], FillBuffer> fillbufImpl =
	    (tgt, range) -> new FakeFill(range[1] - range[0]);
	public FillBuffer fillbuf(DataBuffer target, int from, int to) {
	    fillbufCalls.add(new Object[]{target, from, to});
	    return(fillbufImpl.apply(target, new int[]{from, to}));
	}
	public Render render() {throw new UnsupportedOperationException();}
	public DrawList drawlist() {throw new UnsupportedOperationException();}
	public void submit(Render cmd) {throw new UnsupportedOperationException();}
	public void dispose() {throw new UnsupportedOperationException();}
	public Caps caps() {throw new UnsupportedOperationException();}
	public boolean compatible(DrawList ob) {return(true);}
	public boolean compatible(Texture ob) {return(true);}
	public boolean compatible(DataBuffer ob) {return(true);}
    }

    /* --- Tests --- */

    /* Direct regression guard for the original CCE: a standard Filler
     * calls env.fillbuf(target) and writes into the returned FillBuffer.
     * The proxy must hand back the pre-allocated Fill (no copy, no
     * fall-through to back). */
    @Test
    void wholeRangeFillbufReturnsPreallocated() {
	FakeBuffer target = new FakeBuffer(64);
	FakeFill pre = new FakeFill(64);
	FakeBackEnv back = new FakeBackEnv();
	AtomicReference<FillBuffer> handed = new AtomicReference<>();

	DataBuffer.Filler<DataBuffer> filler = (tgt, env) -> {
	    FillBuffer fb = env.fillbuf(tgt);
	    handed.set(fb);
	    fb.push().put(0, (byte)0x42);
	    return(fb);
	};

	StreamFiller.runWithPreallocated(back, target, 64, filler, pre, pre::pull);

	assertSame(pre, handed.get(), "proxy must return the pre-allocated Fill for whole-range fillbuf(target)");
	assertEquals(0, back.fillbufCalls.size(), "back env must not see fillbuf for whole-range target");
	assertFalse(pre.disposed, "pre-allocated Fill must not be disposed on the fast path");
	assertTrue(pre.pulls.isEmpty(), "no copy/pull on the fast path");
	assertEquals((byte)0x42, pre.data.get(0), "Filler's write landed directly in pre");
    }

    /* Filler's two-arg fillbuf maps to the same fast path. */
    @Test
    void wholeRangeFillbufRangeFormReturnsPreallocated() {
	FakeBuffer target = new FakeBuffer(32);
	FakeFill pre = new FakeFill(32);
	FakeBackEnv back = new FakeBackEnv();
	AtomicReference<FillBuffer> handed = new AtomicReference<>();

	DataBuffer.Filler<DataBuffer> filler = (tgt, env) -> {
	    FillBuffer fb = env.fillbuf(tgt, 0, 32);
	    handed.set(fb);
	    return(fb);
	};

	StreamFiller.runWithPreallocated(back, target, 32, filler, pre, pre::pull);

	assertSame(pre, handed.get());
	assertEquals(0, back.fillbufCalls.size());
    }

    /* fillbuf for a different target must fall through to the back env;
     * the pre-allocated Fill is not handed out for unrelated buffers. */
    @Test
    void otherTargetFallsThroughToBackEnv() {
	FakeBuffer target = new FakeBuffer(64);
	FakeBuffer other = new FakeBuffer(16);
	FakeFill pre = new FakeFill(64);
	FakeBackEnv back = new FakeBackEnv();
	AtomicReference<FillBuffer> handed = new AtomicReference<>();

	DataBuffer.Filler<DataBuffer> filler = (tgt, env) -> {
	    FillBuffer fb = env.fillbuf(other);
	    handed.set(fb);
	    /* The Filler still has to return *something* targeting tgt;
	     * pretend it got the real target buffer separately. */
	    return(env.fillbuf(tgt));
	};

	StreamFiller.runWithPreallocated(back, target, 64, filler, pre, pre::pull);

	assertNotSame(pre, handed.get(), "fillbuf(other) must not return pre-allocated Fill");
	assertEquals(1, back.fillbufCalls.size(), "back env must see exactly the one fillbuf(other) call");
	assertSame(other, back.fillbufCalls.get(0)[0]);
    }

    /* A partial-range fillbuf(target, 0, half) is NOT the whole-range
     * fast path and must fall through to the back env. */
    @Test
    void partialRangeFallsThroughToBackEnv() {
	FakeBuffer target = new FakeBuffer(64);
	FakeFill pre = new FakeFill(64);
	FakeBackEnv back = new FakeBackEnv();
	AtomicReference<FillBuffer> handed = new AtomicReference<>();

	DataBuffer.Filler<DataBuffer> filler = (tgt, env) -> {
	    FillBuffer fb = env.fillbuf(tgt, 0, 32);
	    handed.set(fb);
	    return(env.fillbuf(tgt));   // whole-range — gets pre, returned to satisfy contract
	};

	StreamFiller.runWithPreallocated(back, target, 64, filler, pre, pre::pull);

	assertNotSame(pre, handed.get(), "partial range must fall through to back env");
	assertEquals(1, back.fillbufCalls.size());
	assertArrayEquals(new Object[]{target, 0, 32}, back.fillbufCalls.get(0));
    }

    /* If the Filler bypasses env.fillbuf entirely and returns its own
     * FillBuffer, the helper must copy the bytes into the pre-allocated
     * Fill via the supplied pull callback, then dispose the source. */
    @Test
    void bypassedFillbufTriggersCopyFallback() {
	FakeBuffer target = new FakeBuffer(8);
	FakeFill pre = new FakeFill(8);
	FakeBackEnv back = new FakeBackEnv();

	byte[] payload = {1, 2, 3, 4, 5, 6, 7, 8};
	FakeFill bypass = new FakeFill(8);
	bypass.data.put(payload);   // leave position at 8 so flip() yields full data

	DataBuffer.Filler<DataBuffer> filler = (tgt, env) -> bypass;

	StreamFiller.runWithPreallocated(back, target, 8, filler, pre, pre::pull);

	assertEquals(0, back.fillbufCalls.size(), "back env not consulted on bypass path");
	assertTrue(bypass.disposed, "bypass FillBuffer must be disposed after copy");
	assertEquals(1, pre.pulls.size(), "pull called exactly once with bypass bytes");
	assertArrayEquals(payload, pre.pulls.get(0));
	assertFalse(pre.disposed, "pre-allocated Fill is not disposed by the helper");
    }
}
