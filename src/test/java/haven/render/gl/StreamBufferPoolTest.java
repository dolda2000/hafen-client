package haven.render.gl;

import java.nio.ByteBuffer;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
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

    /* Pin the current reuse policy: the lowest-index free slot is reused
     * first. If a future change moves to LIFO (e.g. for cache locality),
     * this test must be updated deliberately rather than silently. */
    @Test
    void reusesLowestIndexFreeSlot() {
	CountingAlloc alloc = new CountingAlloc();
	StreamBuffer.Pool pool = new StreamBuffer.Pool(8, alloc);

	ByteBuffer a = pool.get();   // slot 0
	ByteBuffer b = pool.get();   // slot 1
	ByteBuffer c = pool.get();   // slot 2
	assertEquals(3, alloc.n.get());

	pool.put(a);
	pool.put(c);
	// slots 0 and 2 free; lowest-index policy picks 0
	assertSame(a, pool.get());
	// slot 2 still free
	assertSame(c, pool.get());
	assertEquals(3, alloc.n.get(), "no new allocation needed");
    }

    /* Stress the pool from multiple threads. Two safety invariants:
     *
     *   (1) Mutual exclusion. While a buffer is held, no other get()
     *       may hand it out. Tracked with an IdentityHashMap of buffer
     *       -> owning thread; a duplicate hand-out fails the test.
     *
     *   (2) Bounded growth. Total allocations must not exceed the peak
     *       number of buffers held concurrently. If two threads race
     *       and both grow the pool when one would have sufficed, this
     *       fails.
     *
     * Both invariants hold today because Pool.get/put are synchronized.
     * The test guards against accidental locking regressions (e.g. a
     * future "optimization" that drops the monitor). */
    @Test
    void concurrencyStress() throws InterruptedException {
	final int threads = 8;
	final int iterations = 1000;
	final int bufSize = 32;

	CountingAlloc alloc = new CountingAlloc();
	StreamBuffer.Pool pool = new StreamBuffer.Pool(bufSize, alloc);

	IdentityHashMap<ByteBuffer, Thread> heldBy = new IdentityHashMap<>();
	AtomicInteger heldCount = new AtomicInteger(0);
	AtomicInteger peakHeld = new AtomicInteger(0);
	AtomicReference<String> failure = new AtomicReference<>(null);

	CountDownLatch start = new CountDownLatch(1);
	CountDownLatch done = new CountDownLatch(threads);
	Thread[] workers = new Thread[threads];

	for(int t = 0; t < threads; t++) {
	    workers[t] = new Thread(() -> {
		    try {
			start.await();
			for(int i = 0; (i < iterations) && (failure.get() == null); i++) {
			    ByteBuffer buf = pool.get();
			    synchronized(heldBy) {
				Thread prev = heldBy.put(buf, Thread.currentThread());
				if(prev != null) {
				    failure.compareAndSet(null,
					"buffer handed out twice: held by " + prev + " and " + Thread.currentThread());
				    return;
				}
			    }
			    int held = heldCount.incrementAndGet();
			    int prevPeak;
			    do {
				prevPeak = peakHeld.get();
				if(held <= prevPeak) break;
			    } while(!peakHeld.compareAndSet(prevPeak, held));

			    // brief use; force a memory barrier
			    buf.put(0, (byte)(i & 0x7f));

			    synchronized(heldBy) {
				heldBy.remove(buf);
			    }
			    heldCount.decrementAndGet();
			    pool.put(buf);
			}
		    } catch(InterruptedException e) {
			Thread.currentThread().interrupt();
		    } finally {
			done.countDown();
		    }
		}, "pool-stress-" + t);
	    workers[t].start();
	}

	start.countDown();
	assertTrue(done.await(30, TimeUnit.SECONDS), "stress workers did not finish in time");

	assertNull(failure.get(), failure.get());
	assertEquals(0, heldCount.get(), "all buffers should be released by end");
	assertTrue(alloc.n.get() <= peakHeld.get(),
		   "allocations (" + alloc.n.get() + ") should not exceed peak concurrent holds (" + peakHeld.get() + ")");
	assertTrue(peakHeld.get() <= threads,
		   "peak concurrent holds (" + peakHeld.get() + ") cannot exceed thread count");
	assertEquals(alloc.n.get(), pool.allocated(),
		     "every allocated SysBuffer should still be in the pool");

	// All allocated buffers should be distinct identities.
	Set<Integer> ids = new HashSet<>();
	for(StubBuffer sb : alloc.handed)
	    assertTrue(ids.add(System.identityHashCode(sb.data)),
		       "allocator should produce distinct ByteBuffers");
    }
}
