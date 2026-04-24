package haven.render.gl;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/* Coverage for the RenderQueue extracted from GLEnvironment. The
 * critical invariant is the "snapshot submitted before prep" ordering
 * in drain(): a render landing in the submitted snapshot must always
 * have its prep already in the prep snapshot. */
public class RenderQueueTest {

    @Test
    void prepBeforeSubmittedInSnapshot() {
	RenderQueue<String> q = new RenderQueue<>();
	q.enqueuePrep("p1");
	q.enqueueSubmitted("s1");
	q.enqueuePrep("p2");
	q.enqueueSubmitted("s2");

	RenderQueue.Snapshot<String> snap = q.drain();
	assertEquals(List.of("p1", "p2"), snap.prep);
	assertEquals(List.of("s1", "s2"), snap.submitted);
	assertEquals(0, q.prepSize());
	assertEquals(0, q.submittedSize());
    }

    /* Hazard guard: while drain() is running, a producer enqueues a
     * (prep, render) pair. The invariant requires that if the render
     * lands in this drain's submitted snapshot, the prep must also be
     * in the same drain's prep snapshot. The current implementation
     * achieves this by snapshotting submitted FIRST, then prep -- so
     * a (prep, render) pair enqueued strictly between the two snapshot
     * steps will defer the render to the next pass. The pair is never
     * split across drains. Stress this race many times.
     *
     * If a future change reorders the snapshot steps (prep first, then
     * submitted), this test will fail: a render whose prep was just
     * enqueued could land in the submitted snapshot without its prep
     * landing in the prep snapshot. */
    @Test
    void noRenderEverSeenWithoutItsPrep() throws InterruptedException {
	for(int trial = 0; trial < 200; trial++) {
	    RenderQueue<String> q = new RenderQueue<>();
	    final int items = 50;

	    CountDownLatch start = new CountDownLatch(1);
	    Thread producer = new Thread(() -> {
		try { start.await(); } catch(InterruptedException ie) { return; }
		for(int i = 0; i < items; i++) {
		    q.enqueuePrep("p" + i);
		    q.enqueueSubmitted("r" + i);
		}
	    });
	    producer.start();
	    start.countDown();

	    // Drain repeatedly until producer finishes; verify invariant
	    // each pass and accumulate observed prep/render IDs.
	    java.util.Set<Integer> seenPrep = new java.util.HashSet<>();
	    java.util.Set<Integer> seenRender = new java.util.HashSet<>();
	    while(producer.isAlive() || (q.prepSize() + q.submittedSize() > 0)) {
		RenderQueue.Snapshot<String> snap = q.drain();
		java.util.Set<Integer> prepIds = new java.util.HashSet<>();
		for(String p : snap.prep)
		    prepIds.add(Integer.parseInt(p.substring(1)));
		for(String r : snap.submitted) {
		    int id = Integer.parseInt(r.substring(1));
		    assertTrue(prepIds.contains(id) || seenPrep.contains(id),
			       "render r" + id + " seen without its prep p" + id
			       + " in this or any prior snapshot (trial " + trial + ")");
		    seenRender.add(id);
		}
		seenPrep.addAll(prepIds);
		Thread.yield();
	    }
	    producer.join();

	    assertEquals(items, seenPrep.size());
	    assertEquals(items, seenRender.size());
	}
    }

    @Test
    void enqueueRejectedAfterInvalidate() {
	RenderQueue<String> q = new RenderQueue<>();
	q.invalidate();
	assertFalse(q.enqueuePrep("p"));
	assertFalse(q.enqueueSubmitted("s"));
	assertEquals(0, q.prepSize());
	assertEquals(0, q.submittedSize());
	assertTrue(q.isInvalid());
    }

    @Test
    void enqueueAcceptedBeforeInvalidate() {
	RenderQueue<String> q = new RenderQueue<>();
	assertTrue(q.enqueuePrep("p"));
	assertTrue(q.enqueueSubmitted("s"));
	q.invalidate();
	// drain still returns what was already enqueued -- callers use
	// this on dispose to abort the in-flight items.
	RenderQueue.Snapshot<String> snap = q.drain();
	assertEquals(List.of("p"), snap.prep);
	assertEquals(List.of("s"), snap.submitted);
    }

    @Test
    void awaitSubmittedBlocksUntilEnqueue() throws InterruptedException {
	RenderQueue<String> q = new RenderQueue<>();
	AtomicBoolean returned = new AtomicBoolean(false);
	AtomicReference<Throwable> err = new AtomicReference<>();
	Thread waiter = new Thread(() -> {
	    try {
		q.awaitSubmitted();
		returned.set(true);
	    } catch(Throwable t) {
		err.set(t);
	    }
	});
	waiter.start();

	// Give the waiter time to enter wait()
	Thread.sleep(50);
	assertFalse(returned.get(), "awaitSubmitted must block on empty queue");

	q.enqueueSubmitted("x");
	waiter.join(2000);
	assertNull(err.get());
	assertTrue(returned.get(), "awaitSubmitted must return after enqueue");
    }

    @Test
    void awaitSubmittedReturnsImmediatelyIfNonEmpty() throws InterruptedException {
	RenderQueue<String> q = new RenderQueue<>();
	q.enqueueSubmitted("x");
	// Should not block.
	q.awaitSubmitted();
    }
}
