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
- **`StreamBuffer.Fill.compatible(env)`.** Returns true only for the
  owning env, false for a foreign one. One-line guard against cross-env
  data corruption.
- **`Fill.dispose()` idempotency + Finalizer race.** Call `dispose()`
  explicitly, then trigger the `Finalizer` cleaner; assert the pool sees
  exactly one `put` (i.e. the `clear[0]` flag short-circuits the second
  path).
- **`Fill.get()` transfers ownership.** After `get()`, dispose must NOT
  auto-`put` — the caller (the prep lambda's `jdret.put(gl, xfbuf)`)
  takes over. Observable via the pool: `Fill` constructed → `get()`
  called → `dispose()` called → pool slot is still marked used.

## Medium — small refactor unlocks

Each one needs a focused extraction (a couple of methods become a
package-private helper class) so a fake `Environment`/queue can be
injected.

- **`runStreamFill` proxy semantics.** Extract the proxy-`Environment`
  factory from `GLEnvironment.runStreamFill` into a static method on a
  small holder. Then test against a fake `Environment` and synthetic
  `Filler`s:
  1. Filler that calls `env.fillbuf(buf)` — no copy, returns the
     pre-allocated `Fill` identity.
  2. Filler that calls `env.fillbuf(otherTarget)` — falls through to
     the back env, does NOT get the pre-allocated `Fill`.
  3. Filler that calls `env.fillbuf(buf, 0, halfSize)` — partial range,
     falls through.
  4. Filler that allocates its own `FillBuffer` and returns it — the
     fallback copy path runs and the bytes land in the `Fill`.

  Case 1 is the direct regression guard for the original
  `ClassCastException` we fixed: it would have failed the moment
  someone reordered `buf.ro` again, regardless of whether `fillbuf` is
  involved.

- **prepq/submitted draining order.** Extract the queue manager (the
  part of `process()` that snapshots `submitted` then `prepq`) into a
  class with injectable executors. Test: while a writer is enqueueing
  prep, a reader submits a render — the reader's render must execute
  strictly after the prep in the next `process()` pass.

- **`enqprep` rejection when invalid.** Set `invalid = true`, call
  `enqprep(p)`, assert `p.gl.abort()` and `p.dispose()` ran and the
  queue stays empty. Same queue extraction unlocks this.

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

The medium tier pays off most. The proxy-semantics test in particular
turns the fix in `runStreamFill` from "documented invariant" into
"regression-locked invariant" — at the cost of one small extraction.
The cheap tier is mechanical and worth burning down opportunistically.
