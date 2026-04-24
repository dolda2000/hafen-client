# GL rendering — architecture and invariants

This is a map of the GL rendering layer (`src/haven/render/gl/`) as of
branch `eliminate-glenv-prep`. Focus: how commands get from game code
onto the GPU, how GL resources are tracked and disposed, and where the
subtle invariants live.

File references are cited inline. Line numbers drift — treat them as
anchors, not addresses.

## Contents

- [Top-level shape](#top-level-shape) — `GLEnvironment` / `GLRender` / `GLDrawList`, the `BGL` buffered-GL indirection
- [Frame lifecycle](#frame-lifecycle) — prep → submitted → disposeall ordering in `process()`, plus the `RenderQueue.drain` invariant and why submit-from-any-thread is safe
- [Core primitives](#core-primitives) — `BGL`, `GLObject` (rc/disp/dispseq), `Sequence`
- [Disposal and the seq ring](#disposal-and-the-seq-ring) — the seqhead/seqtail ring and why one stuck Sequence blocks all downstream tail advancement
- [VAO / EBO binding](#vao--ebo-binding) — caching model, the historical crash bug, both layers of the fix
- [STREAM buffers](#stream-buffers) — `StreamBuffer`/`Pool`/`Fill`, publication order
- [State application: Applier and GLState](#state-application-applier-and-glstate) — `Applier`, `curstate`, state diffing
- [Thread model](#thread-model) — submit call-site table, shared-state protection
- [Caches and their lifetime keys](#caches-and-their-lifetime-keys) — key types and ref-types
- [Known bugs and fixes applied on `eliminate-glenv-prep`](#known-bugs-and-fixes-applied-on-eliminate-glenv-prep) — chronological commit list + current fixes
- [Instrumentation points](#instrumentation-points) — seqstats, LEAK_CHECK, disptrace, debuglog
- [Pointers](#pointers)

## Top-level shape

Three collaborator classes own almost everything:

- **`GLEnvironment`** (`GLEnvironment.java`) — per-GL-context singleton.
  Owns the render queue, the dispose ring, the STREAM-buffer pool, the
  program/VAO caches, and the `process()` loop that drains queued work
  onto the real GL.
- **`GLRender`** (`GLRender.java`) — a recorder for one batch of GL
  commands. Client code calls `env.render()` to get one, records draws
  into it, and calls `env.submit(r)` to hand it off. Each `GLRender`
  owns a `BufferBGL` (its command list) and a `Sequence` (see "Disposal
  and the seq ring" below).
- **`GLDrawList`** (`GLDrawList.java`) — a persistent, sorted list of
  draw slots that replays per frame with minimal state churn. Each
  `DrawSlot` holds a per-slot `BufferBGL` (`main`) and an array of
  refcounted `Setting` objects (VAO binding, FBO config, pipe state,
  uniforms).

Recording is decoupled from execution by the **`BGL`** abstraction
(`BGL.java`) — a buffered GL command queue. `BufferBGL` records into
a growable `Command[]`; `BGL.run(GL)` replays against a real JOGL `GL`.
Every command is a lambda capturing its operands, so the recording
thread need not be the executing thread.

## Frame lifecycle

One frame, one call to `GLEnvironment.process(GL)` on the renderer
thread (`JOGLPanel.java:174`, `GLPanel.java` similar).

```
Client threads                    Renderer thread
(game, AWT, workers)
---------------                    ---------------
env.prepare(...)      ────→        RenderQueue
env.submit(render)    ────→          (prep queue + submitted queue)
                                           │
                                           ▼
                                   env.process(gl):
                                     snap = queue.drain()
                                     for p in snap.prep:    p.gl.run(gl); p.dispose()
                                     for c in snap.submitted: c.gl.run(gl); c.dispose()
                                     checkqueries(gl)
                                     disposeall().run(gl)
                                     clean()
```

### Prep vs submitted

- **Prep** renders are synchronous setup work that must run *before*
  any draw that depends on them — typically data-store uploads
  (`glBufferData`, `glTexImage2D`, etc.) enqueued by `GLEnvironment`'s
  three `prepare(...)` overloads (`GLEnvironment.java:466-497`). Prep
  runs first in `process()` (`GLEnvironment.java:321-338`).
- **Submitted** renders are the caller-visible draw batches handed in
  via `env.submit(render)` (`GLEnvironment.java:377-394`). They run
  after prep (`GLEnvironment.java:339-351`).
- **`disposeall()`** runs last (`GLEnvironment.java:353`), actually
  calling `glDelete*` on objects that hit `rc == 0` and whose
  `dispseq` is now older than `seqtail`.

### RenderQueue ordering invariant

`RenderQueue.drain()` (`RenderQueue.java:91-102`) snapshots the
*submitted* queue first, *then* the prep queue. Two independent
monitors (`submittedMon`, `prepMon`) guard each queue.

The invariant this protects: any `submit()` that raced in after the
submitted snapshot is deferred to the next frame — and its
corresponding prep work (always enqueued *before* the submit in the
client code) is guaranteed to also be deferred, never ending up in the
earlier prep snapshot without its matching submit. The stale prep
will land in the *next* drain's prep list, still ahead of the draws
that depend on it.

This is why `submit()` is safe from multiple threads.

## Core primitives

### `BGL` — buffered GL

`BGL.java` defines the GL-like interface. `BufferBGL` records commands
(`BufferBGL.java`). Every record is a lambda capturing its args; the
command array grows as needed. `trim()` returns a minimal copy for
long-lived storage (used by `GLDrawList` settings).

### `GLObject` — refcounted GL handles

`GLObject.java` is the base for every wrapped GL name: `GLBuffer`,
`GLTexture2D`, `GLVertexArray`, `GLProgram`, `GLShader`, `GLSampler`,
`GLFrameBuffer`, `GLRenderBuffer`, `GLQuery`.

- **`rc`** (`GLObject.java:38`) — reference count. `get()` increments,
  `put()` decrements. When `rc == 0` and `dispose()` has been called,
  `dispose0()` stages the actual delete.
- **`disp`** flag — latches when `dispose()` is called; prevents double
  dispose and cooperates with `rc` so a resource still being used
  (`rc > 0`) doesn't get deleted underneath a live render.
- **`dispseq`** (`GLObject.java:39`) — the `seqhead` value at the time
  of `dispose0()`. Determines when the object's `glDelete*` may run
  (see next section).
- **`glid()`** throws `UseAfterFreeException` if called post-delete —
  a Java-level use-after-free surfaces as an exception, not a
  driver-side NULL deref.

### `Sequence` — lifetime tracking

`GLEnvironment.Sequence` (`GLEnvironment.java:1040+`) is a small
`Disposable` that registers a monotonic sequence number in the
`sequse[]` ring via `seqreg()` and unregisters via `sequnreg()`.

One `Sequence` per `GLRender` (`GLRender.java:48`) — there are no
other owners in the codebase. Its `disposed()` path has a
belt-and-braces `Finalizer.finalize(owner, ...)` wire so GC can claim
it if explicit `dispose()` is missed (but that path also logs
`"disposal sequence leaked"`).

## Disposal and the seq ring

This is the subtlest mechanism and the one that keeps the deferred
delete honest.

### `dispseq` and `disposeall()`

When a `GLObject` is Java-level disposed (`rc == 0 && disp == true`),
`dispose0()` does **not** call `glDelete*` — instead it stamps
`dispseq = env.dispseq()` (the current `seqhead`) and pushes the
object onto `env.disposed` (`GLObject.java:52-62`).

Each frame, `GLEnvironment.disposeall()` (`GLEnvironment.java:395-418`)
walks `env.disposed` and deletes any object whose `dispseq < seqtail`.

### Why the deferral

Between Java-level dispose and the actual `glDelete*`, there may be
queued renders that still reference the object — their `BufferBGL`
captured a Java reference but the GL command only needs the numeric
name at replay time. If we deleted the name as soon as `rc == 0`, a
queued command could be replayed against a dead name.

The `Sequence` tied to each `GLRender` keeps `seqtail` pinned at or
below the oldest still-in-flight render's `dispseq`, so disposal waits
until every command list that could reference the object has been
processed and disposed.

### The ring and seqtail advancement

`sequse[]` is a fixed-size ring of booleans indexed by sequence number
mod ring size. `seqreg()` advances `seqhead` and marks the slot true;
`sequnreg()` marks false and, *if the freed slot is exactly at
`seqtail`*, advances `seqtail` forward until it hits the next
still-in-flight slot (`GLEnvironment.java:1018-1024`).

**Key consequence:** a single never-disposed Sequence at the tail
blocks *all* subsequent tail advancement, even if thousands of
younger Sequences have been properly freed. `seqhead - seqtail` grows
without bound, and `disposeall()` backs up because no `GLObject`'s
`dispseq` ever falls below the stuck tail.

The ring size starts at `0x8000` (32k slots), sized to observed
steady-state after the ring's churn-doubling logic resized it during
warm-up (`ea26a4f8a`). A warning fires at `0x20000` (4× steady-state)
as an early leak signal (`GLEnvironment.java:967-976`).

`GLEnvironment.seqstats()` returns `{span, alive}` where `span =
seqhead - seqtail` and `alive` is the true count of `sequse[] == true`
slots. If `span >> alive`, the tail is stuck. Process logs this every
30k frames (`GLEnvironment.java:355-367`).

### Known Sequence-leak paths (patched on this branch)

- **Empty-submit leak** (`GLEnvironment.submit`, fixed): if a caller
  passes a `GLRender` whose `gl()` was never called, `submit()` used
  to early-return without disposing. The caller had already handed
  over ownership, so the `Sequence` leaked to finalization. Now
  disposes (`GLEnvironment.java:383-393`).
- **Exception-in-prepare leak** (three `prepare(...)` overloads,
  fixed): `new GLRender(...)` happened before the user's consumer /
  `bglCreate` / `bglSubmit` call; a throw there skipped `enqprep` and
  leaked the `GLRender`. Each overload now uses try/finally with a
  success flag so any throw (including `Error`) disposes the partial
  render (`GLEnvironment.java:466-497`).

## VAO / EBO binding

The crash analysis in `bin/hs_err_pid23396_analysis.md` covers this in
depth. The short version:

### VAO caching

`GLVertexArray.ProgIndex.get` (`GLVertexArray.java:227-240`) caches
one VAO per `(Model, program.attribs[])`. The VAO is `init`'d once
with whichever `GL_ELEMENT_ARRAY_BUFFER` was current at creation
time (`GLVertexArray.java:85-167`, explicit bind at 111-116). The
cached VAO is **not** re-init'd if the Model's EBO later changes —
for STREAM-backed indices the EBO can change because
`StreamBuffer.rbuf` is recreated whenever `buf.ro` is invalidated.

`GLDrawList` additionally caches a `VaoSetting` per `(vao, ebo)`
pair (`GLDrawList.java:826-836`). Different EBOs with the same VAO
produce different `VaoSetting` instances.

### The historical bug (fixed on this branch)

The state-tracker `VaoBindState.applyto` (`VaoBindState.java`)
previously only rebound the EBO when the VAO itself changed. A
transition `(vao=V, ebo=A) → (vao=V, ebo=B)` silently skipped the
EBO rebind. The VAO's *GL-internal* EBO slot then kept pointing at
A (from init time). If A was later deleted, the next
`glDrawElements` ran with no EBO bound, and the driver fell back to
the legacy "indices is a client pointer" path — dereferencing
`indices=0` as NULL.

Two fixes applied (both kept):

1. **State-tracker fix** (`VaoBindState.java:62-69`): `applyto` now
   rebinds on same-VAO/EBO-only transitions, mirroring `apply()`.
2. **Draw-site fix** (`GLDrawList.java:875-882`): the per-draw
   `BufferBGL` unconditionally emits
   `glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, ebo)` immediately before
   `glDrawElements`. This is the robust layer — the VAO's internal
   EBO slot is overridden right before it matters, so no amount of
   state-tracker confusion can reach the draw.

The `DO_GL_EBO_FIXUP` constant in `VaoBindState` is effectively a
permanent `true` — the author already acknowledged VAO-tracked EBO
state is unreliable. A future cleanup can inline it.

## STREAM buffers

STREAM-class data buffers (`DataBuffer.Usage.STREAM`) are uploaded to
a recyclable `GLBuffer` owned by a `StreamBuffer`. The flow lives in
`GLEnvironment.prepare(Model.Indices)` and the equivalent for vertex
arrays (`GLEnvironment.java:494+`).

- `StreamBuffer` wraps a `GLBuffer` (`rbuf`) plus a `Pool` of
  recyclable transfer `ByteBuffer`s.
- On first prepare (or after `buf.ro` is invalidated), a new
  `StreamBuffer` is created. `runStreamFill` (`StreamFiller.java:
  runWithPreallocated`) pulls data from the Filler into a
  pre-allocated `Fill` directly, avoiding an intermediate
  `FillBuffers.Array` allocation. A prep render is enqueued that
  runs `Vao0State.apply` + `glBufferData`.
- `buf.ro` is assigned **after** the prep is enqueued
  (`GLEnvironment.java:530-532`) — deliberately, so any concurrent
  reader that observes the new `ro` sees the upload sitting ahead of
  any draw they later submit (commit `33a4b26d6`).
- `StreamBuffer.Pool` is synchronized; the data-store upload via
  `runStreamFill` is synchronous on the submit thread, not deferred
  (`StreamFiller.java:42-66`).

## State application: Applier and GLState

`Applier` (`Applier.java`) is the state-diff machinery. A `Pipe`
(logical state soup) gets compiled into a `Pipe` of `GLState`
instances, one per slot (`VboState`, `VaoBindState`, `FboState`,
`Vao0State`, per-pipe states, uniforms). `Applier.apply(gl, that)`
walks slots and calls `this.states[i].applyto(gl, that.states[i])`,
which emits only the GL calls needed to transition from current state
to target state.

`curstate` on `GLEnvironment` (`GLEnvironment.java:48`) persists the
last applied state across renders *within a process() call* —
recording and carrying state from one render into the next so
`applyto` can minimize work. It's read and written only under
`synchronized(drawmon)` inside `process()`, so it's
renderer-thread-confined.

## Thread model

| Actor | What it does |
| --- | --- |
| Renderer / GL context thread | Runs `GLEnvironment.process()`, disposal, queries. The only thread that ever touches the real `GL` object. |
| Game / AWT / worker threads | Call `env.render()`, record commands into the returned `GLRender`, call `env.submit(...)`. Also call `env.prepare(...)` (indirectly, via model/texture upload paths). |
| Finalizer | Last-ditch Sequence unregistration for leaked `GLRender`s. Also `GLObject.LEAK_CHECK` leak tracer. |

Submit call sites:

| Site | Thread | Shape |
| --- | --- | --- |
| `GLPanel.java:416` (`Loop.run`) | Renderer/GL | Fire-and-forget |
| `MapView.java:2154, 2181` | Game/UI | Pre-built `GLRender` |
| `Fightsess.java:476` | Game/UI | Pre-built, one-shot fence |
| `rs/DrawBuffer.java:65, 93` | Game/UI | Pre-built |
| `JOGLPanel.java:174` | GL context | Direct `process()` |
| `Test.java` (two of them) | Test driver | Test-only |

Shared-state protection:

- `RenderQueue` — two monitors, drain ordering described above.
- `curstate` — `synchronized(drawmon)` inside `process()`.
- `disposed` list (`GLEnvironment.java`) — `synchronized(disposed)`.
- `sequse`, `seqhead`, `seqtail` — `synchronized(seqmon)`.
- `StreamBuffer.Pool` — internally synchronized.
- `buf.ro` on `Model.Indices` / vertex arrays — `synchronized(buf)`.

## Caches and their lifetime keys

| Cache | Key | Ref type | Notes |
| --- | --- | --- | --- |
| `GLEnvironment.progcache` | shader hash + shader list | — | Program linking |
| `GLVertexArray.ProgIndex` (per Model) | `program.attribs[]` | — | VAO init once |
| `GLDrawList.vaos` | `(GLVertexArray, GLBuffer)` | WEAK | Per (vao, ebo) `VaoSetting` |
| `GLDrawList.settings` | `SettingKey` (program, vid, depid) | — | Setting refcounting |
| `StreamBuffer.Pool` | — | strong | Transfer-buffer recycling |

## Known bugs and fixes applied on `eliminate-glenv-prep`

Referenced in commit history and `bin/hs_err_pid23396_analysis.md`.
Ordered by commit:

1. `beaff5f39` — Eliminated `this.prep` shared render; each prepare
   now gets its own `GLRender`.
2. `33a4b26d6` — Decouple STREAM prepare from `buf.ro` publication
   order (upload enqueued before ro assignment).
3. `aec453e0e` — `StreamBuffer.Pool` concurrency stress and reuse-order
   tests.
4. `5b35ca266` — Extracted `StreamFiller.runWithPreallocated`; CCE
   regression pinned.
5. `93c8e3e8b` — `StreamBuffer.Fill` lifecycle tests via test-only ctor.
6. `50e1859b6` — Extracted `RenderQueue`; locked the submitted-first
   drain invariant.
7. `3d37a8937` — `GLRender.update` routes STREAM uploads through
   `runStreamFill`.
8. `ea26a4f8a` — Sized dispose ring to observed steady-state (32k).
9. **VAO/EBO fix** — `VaoBindState.applyto` rebinds EBO on
   same-VAO/EBO-only transitions; draw site unconditionally rebinds
   EBO immediately before `glDrawElements`.
10. **Sequence-leak fixes** — `env.submit` disposes empty renders;
    three `prepare(...)` overloads dispose on any throw; `process()`
    periodically logs `seq-ring: span=N alive=M` to detect stuck
    tails.

## Instrumentation points

- **Seq ring stats** — `GLEnvironment.seqstats()`; auto-logged from
  `process()` every 30k frames if span > 1000.
- **Leak check** — `GLObject.LEAK_CHECK` (on by default) wires a
  `Finalizer.leakcheck` per object; finalization of an undisposed
  object logs the creation-site stack trace.
- **Sequence finalization** — `Sequence.disposed()` logs
  `"disposal sequence leaked"` if finalization fires before
  explicit `dispose()`.
- **`disptrace`** (`GLObject.java:64`) — captures the throwable at
  first `dispose()` call so a later use-after-free can be blamed.
- **`debuglog`** in `process()` — optional `glDebugMessageCallback`
  routing (`GLEnvironment.java`).

## Pointers

- Crash analysis: `bin/hs_err_pid23396_analysis.md`.
- Test coverage roadmap: `doc/gl-test-coverage.md`.
- GPU profiling notes: `doc/gpu-profiling/`.
