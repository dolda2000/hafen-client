package haven.render.gl;

import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.IntFunction;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/* Regression coverage for the StreamBuffer reuse mechanism that the
 * STREAM prepare path relies on. If GLEnvironment.fillbuf ever stops
 * routing STREAM-backed targets through StreamBuffer.Fill, this test
 * does not catch that directly -- it only guards the pool semantics
 * the optimization is built on. The fillbuf routing itself is asserted
 * structurally by the prepare() code (which now constructs Fill
 * directly rather than depending on fillbuf returning the right
 * subtype). */
public class StreamBufferPoolTest {

    private static class StubBuffer implements SysBuffer {
	final ByteBuffer data;
	boolean disposed = false;
	StubBuffer(int sz) {data = ByteBuffer.allocate(sz);}
	public ByteBuffer data() {return(data);}
	public void dispose() {disposed = true;}
    }

    private static class CountingAlloc implements IntFunction<SysBuffer> {
	final AtomicInteger n = new AtomicInteger();
	final java.util.List<StubBuffer> handed = new java.util.ArrayList<>();
	public SysBuffer apply(int sz) {
	    n.incrementAndGet();
	    StubBuffer b = new StubBuffer(sz);
	    handed.add(b);
	    return(b);
	}
    }

    @Test
    void getThenPutThenGetReusesSingleBuffer() {
	CountingAlloc alloc = new CountingAlloc();
	StreamBuffer.Pool pool = new StreamBuffer.Pool(64, alloc);

	ByteBuffer a = pool.get();
	pool.put(a);
	ByteBuffer b = pool.get();

	assertSame(a, b, "buffer should be reused after put");
	assertEquals(1, alloc.n.get(), "exactly one SysBuffer allocation expected");
	assertEquals(1, pool.allocated());
    }

    @Test
    void twoConcurrentGetsAllocateTwoBuffers() {
	CountingAlloc alloc = new CountingAlloc();
	StreamBuffer.Pool pool = new StreamBuffer.Pool(64, alloc);

	ByteBuffer a = pool.get();
	ByteBuffer b = pool.get();

	assertNotSame(a, b);
	assertEquals(2, alloc.n.get());
	assertEquals(2, pool.allocated());
    }

    @Test
    void freedSlotIsReusedOnNextGet() {
	CountingAlloc alloc = new CountingAlloc();
	StreamBuffer.Pool pool = new StreamBuffer.Pool(64, alloc);

	ByteBuffer a = pool.get();
	ByteBuffer b = pool.get();
	ByteBuffer c = pool.get();
	assertEquals(3, alloc.n.get());

	pool.put(b);
	ByteBuffer d = pool.get();
	assertSame(b, d, "freed middle slot should be reused");
	assertEquals(3, alloc.n.get(), "no new allocation when a slot is free");
    }

    @Test
    void getRewindsBufferOnReuse() {
	StreamBuffer.Pool pool = new StreamBuffer.Pool(8, sz -> new StubBuffer(sz));
	ByteBuffer a = pool.get();
	a.position(4);
	pool.put(a);
	ByteBuffer b = pool.get();
	assertSame(a, b);
	assertEquals(0, b.position(), "buffer should be rewound on reuse");
    }

    @Test
    void disposeReleasesAllAllocatedBuffers() {
	CountingAlloc alloc = new CountingAlloc();
	StreamBuffer.Pool pool = new StreamBuffer.Pool(64, alloc);

	pool.get();
	pool.get();
	pool.get();
	assertEquals(3, alloc.handed.size());
	for(StubBuffer sb : alloc.handed)
	    assertFalse(sb.disposed);

	pool.dispose();
	for(StubBuffer sb : alloc.handed)
	    assertTrue(sb.disposed, "every allocated SysBuffer should be disposed");
	assertEquals(0, pool.allocated());
    }

    @Test
    void putWithUnknownBufferIsHarmless() {
	StreamBuffer.Pool pool = new StreamBuffer.Pool(8, sz -> new StubBuffer(sz));
	pool.put(ByteBuffer.allocate(8));
    }

    @Test
    void putNullThrows() {
	StreamBuffer.Pool pool = new StreamBuffer.Pool(8, sz -> new StubBuffer(sz));
	assertThrows(NullPointerException.class, () -> pool.put(null));
    }
}
