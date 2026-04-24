# GLEnvironment test coverage — gaps and plan

Status of unit-test coverage for `haven.render.gl.GLEnvironment` and the
collaborators it owns. Ranked by cost-to-cover so the next round of
investment can pick from the top.

The `StreamBuffer.Pool` extraction (see `StreamBufferPoolTest`) is the
template: small refactors that isolate state behind an injectable
boundary make the GL layer testable without standing up a real GL
context.

## Cheap — no further refactor needed

These run today against the already-extracted `StreamBuffer.Pool` and
`StreamBuffer.Fill`.

- **Pool concurrency stress.** 8 threads × 1000 get/put cycles. Assert:
  - no two concurrent `get()` calls return the same `ByteBuffer`
    identity,
  - total allocations stay ≤ peak concurrent gets,
  - after all threads finish and `put` everything, `allocated()` equals
    peak concurrency (no leaked extra allocations).
- **Pool reuse ordering.** Current behaviour: lowest-index free slot
  reused first. Pin it with a test so a future LIFO/cache-locality
  rewrite is a deliberate decision, not an accident.

## Medium — small refactor unlocks

Each one needs a focused extraction (a couple of methods become a
package-private helper class) so a fake `Environment`/queue can be
injected.

- ~~**`StreamBuffer.Fill` lifecycle.**~~ Done — added a test-only
  `StreamBuffer(int, Pool, Environment)` ctor and covered by
  `StreamBufferFillTest` (compatible/dispose-idempotent/get-handoff
  semantics).

- ~~**`runStreamFill` proxy semantics.**~~ Done — extracted into
  `StreamFiller.runWithPreallocated` and covered by `StreamFillerTest`.
  Case 1 (whole-range `fillbuf(target)` returns the pre-allocated
  `Fill`) is the direct regression guard for the original
  `ClassCastException` we fixed.

- ~~**prepq/submitted draining order.**~~ Done — queues + invalid flag
  extracted into `RenderQueue<R>` and covered by `RenderQueueTest`,
  including a stressed invariant test that no render can land in a
  drain's submitted snapshot without its prep also being in the same
  drain (or a prior drain).

- ~~**`enqprep` rejection when invalid.**~~ Done — covered by
  `RenderQueueTest.enqueueRejectedAfterInvalidate`. The
  `GLEnvironment.enqprep` wrapper around it is a 3-line glue
  (abort + dispose on rejection) that doesn't merit its own test.

## Hard — needs a real GL or heavyweight harness

Worth attempting only if a regression in this area actually bites.

- **End-to-end STREAM prepare → process → `glBufferData` byte
  verification.** Requires either a headless GL context or a
  byte-capturing fake `GL` interface implementation (the GL interface
  is ~350 lines).
- **`dispose()` interleaved with in-flight prep across threads** —
  hazard 3 from `f8ef8d6d4`. Needs the queue manager extraction *plus*
  a way to step-pause the prep thread.
- **Texture path caching by env identity.** `prepare(Texture2D)`
  switching envs must dispose the old `GLTexture.Tex2D` and create a
  new one. Same constraints as the STREAM prepare end-to-end test.

## Where to invest next

The medium tier is fully burned down. Everything that remains is in
the hard tier and needs a real GL context or step-pause harness.

Historic note: the proxy-semantics test in particular turned the fix
in `runStreamFill` from "documented invariant" into
"regression-locked invariant" — at the cost of one small extraction.
The cheap tier is mechanical and worth burning down opportunistically.
