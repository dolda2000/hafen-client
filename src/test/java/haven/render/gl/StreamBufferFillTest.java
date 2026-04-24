package haven.render.gl;

import java.nio.ByteBuffer;

import haven.render.*;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/* Lifecycle coverage for StreamBuffer.Fill. Uses the test-only
 * StreamBuffer ctor that skips GLBuffer/env wiring so we can drive
 * Fill purely against a fake Pool/Environment. */
public class StreamBufferFillTest {

    private static class StubBuffer implements SysBuffer {
	final ByteBuffer data;
	StubBuffer(int sz) {data = ByteBuffer.allocate(sz);}
	public ByteBuffer data() {return(data);}
	public void dispose() {}
    }

    private static StreamBuffer.Pool pool(int size) {
	return new StreamBuffer.Pool(size, sz -> new StubBuffer(sz));
    }

    private static class FakeEnv implements Environment {
	public Render render() {throw new UnsupportedOperationException();}
	public FillBuffer fillbuf(DataBuffer t, int from, int to) {throw new UnsupportedOperationException();}
	public DrawList drawlist() {throw new UnsupportedOperationException();}
	public void submit(Render cmd) {throw new UnsupportedOperationException();}
	public void dispose() {}
	public Caps caps() {throw new UnsupportedOperationException();}
	public boolean compatible(DrawList ob) {return(true);}
	public boolean compatible(Texture ob) {return(true);}
	public boolean compatible(DataBuffer ob) {return(true);}
    }

    @Test
    void compatibleOnlyWithOwningEnv() {
	FakeEnv owner = new FakeEnv();
	FakeEnv other = new FakeEnv();
	StreamBuffer sb = new StreamBuffer(16, pool(16), owner);
	StreamBuffer.Fill fill = sb.new Fill();

	assertTrue(fill.compatible(owner));
	assertFalse(fill.compatible(other));
    }

    /* Construct, then dispose without get(): the Fill returns its
     * buffer to the pool, so allocated() stays at 1 but the slot is
     * free for the next get(). */
    @Test
    void disposeReturnsBufferToPool() {
	StreamBuffer.Pool p = pool(16);
	StreamBuffer sb = new StreamBuffer(16, p, new FakeEnv());

	StreamBuffer.Fill fill = sb.new Fill();
	assertEquals(1, p.allocated());

	fill.dispose();
	assertEquals(1, p.allocated(), "no new allocation; same slot");

	// Subsequent Fill should reuse the freed slot, not allocate fresh.
	ByteBuffer reused = sb.new Fill().data;
	assertSame(fill.data, reused, "buffer returned to pool should be reused");
	assertEquals(1, p.allocated());
    }

    /* dispose() must be idempotent (Finalizer guard prevents double-put). */
    @Test
    void disposeIsIdempotent() {
	StreamBuffer.Pool p = pool(8);
	StreamBuffer sb = new StreamBuffer(8, p, new FakeEnv());

	StreamBuffer.Fill fill = sb.new Fill();
	fill.dispose();
	// Second dispose must NOT call put() again — that would throw
	// "buf already free" out of Pool.put.
	assertDoesNotThrow(() -> fill.dispose());
    }

    /* After Fill.get() (the explicit handoff used by the prep path), the
     * Fill no longer owns the buffer — dispose() must not return it to
     * the pool a second time. */
    @Test
    void getThenDisposeDoesNotAutoPut() {
	StreamBuffer.Pool p = pool(8);
	StreamBuffer sb = new StreamBuffer(8, p, new FakeEnv());

	StreamBuffer.Fill fill = sb.new Fill();
	ByteBuffer handed = fill.get();
	assertSame(fill.data, handed);

	// Buffer is now "in flight" — pool still considers it used.
	// Disposing the Fill must NOT put it back, otherwise the GL-side
	// caller that holds `handed` would see its buffer reused under it.
	fill.dispose();

	// Slot is still in use; a fresh Fill should allocate a new one.
	StreamBuffer.Fill fresh = sb.new Fill();
	assertNotSame(fill.data, fresh.data, "in-flight buffer must not be reused before its put()");
	assertEquals(2, p.allocated());

	// Once the GL side returns it via the StreamBuffer.put path, the
	// next Fill reuses it.
	sb.put(handed);
	fresh.dispose();
	assertSame(handed, sb.new Fill().data);
    }

    /* push() exposes the underlying ByteBuffer; pull() copies into it. */
    @Test
    void pullCopiesIntoBackingBuffer() {
	StreamBuffer.Pool p = pool(4);
	StreamBuffer sb = new StreamBuffer(4, p, new FakeEnv());
	StreamBuffer.Fill fill = sb.new Fill();

	byte[] payload = {10, 20, 30, 40};
	fill.pull(ByteBuffer.wrap(payload));

	assertEquals(10, fill.data.get(0));
	assertEquals(40, fill.data.get(3));
    }

    /* get() rewinds the buffer it returns. */
    @Test
    void getRewindsBuffer() {
	StreamBuffer.Pool p = pool(8);
	StreamBuffer sb = new StreamBuffer(8, p, new FakeEnv());
	StreamBuffer.Fill fill = sb.new Fill();
	fill.data.position(5);

	ByteBuffer handed = fill.get();
	assertEquals(0, handed.position());
    }
}
