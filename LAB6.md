<!-- acc-docs 1.0-preview -->
# Lab 6 — Breakout I: Custom Views, Canvas, Game Loop

**ITSE-1333-001 — Mobile Application Development** (CRN 40105)
**Instructor:** Gregory MacDonald — Fall 2026
**Assigned:** Wednesday, September 30, 2026 (Week 6) · **Due:** Sunday, October 18, 2026 (11:59 PM Central) · **Points:** 100
**Accept the assignment** (this creates your private repository): https://classroom50.org/gmd-acc-org/mobile-application-development-40105/assignments/lab6-breakout-i/accept

> **Your repository is the source of truth.** Accepting the assignment gives you a private GitHub repository that already contains the Android Studio project, `README.md`, `WRITEUP.md`, and this handout as `LAB6.md`. The README's **Start here**, **Required work**, **Verify before pushing**, **Submit**, and **Expected repository structure** sections are the exact instructions. If a step in the README and this handout ever disagree, follow the README; this handout owns the rubric and grading. This page tells you what the lab is, why it exists, and how it is graded.

## What this lab is

The start of our five-week Breakout arc: custom views, Canvas, drawing, graphics primitives. Your repository is a working paddle-and-ball starter — a paddle that follows your finger and a ball that bounces off the walls and paddle, drawn at ~60 FPS. This lab you turn it into something that's recognizably *Breakout*: a wall of bricks, ball–brick collision, and a score counter.

1. **Understand the starter** — SurfaceView, the game thread, the fixed timestep, the draw contract (Part 1). Reading, not typing — and it comes first.
2. **Brick grid** — 5 × 8, proportional to the screen, two alternating colors (Part 2)
3. **Ball–brick collision** — the brick dies, the ball bounces, the score climbs (Part 3)
4. **Score counter** — drawn with `Canvas.drawText()` (Part 4)
5. **Style, comments, and proof of play** — no allocations in the loop, comments that say *why*, a short recording (Part 5)

Parts 2–4 build on each other in that order: collision needs bricks, and a score needs collisions to count. Collision, the fixed timestep and the game thread (write-up questions 4 and 5) are covered in full in the Week 7 class on October 7: start the grid now, and the due date leaves room to finish the rest after that class.

**The starter uses no libraries at all** — no AppCompat, no Material, no XML layout. `MainActivity` is ten lines; everything lives in `GameView.kt`. **Read `GameView.kt` top to bottom before writing any code.** Every concept is commented and tagged with the week it's taught (`[W6]`, `[W7]`, `[W8]`, `[W9]`), and every place you need to add code is marked `TODO(student)`. The ones for this lab say `[W6 — Breakout I]`; leave the `[W9 — Full Game]` ones alone — lives and game-over states come later in the arc.

**Borrowed code.** The web is full of Breakout tutorials. Per the syllabus: you may *read* anything, but the code you submit must be **yours**, and any borrowed snippet needs an authorship comment. Projects that are primarily someone else's code earn zero — for the assignment *and* the semester's participation grade.

## Part 1 — Understand what you were handed (this matters for the midterm)

**What `MainActivity` shows you.** It extends plain `android.app.Activity`, not `AppCompatActivity` — AppCompat exists to backport modern *widget* styling, and a game draws every pixel itself, so the `dependencies` block in `app/build.gradle.kts` is empty. There is no XML layout and no `setContentView(R.layout.…)`; the entire UI is one custom view, created in code and handed to `setContentView` directly. The manifest locks the Activity to portrait, so the play field never rotates mid-game — one less problem, for now.

**SurfaceView vs. View.** Our Tic Tac Toe UI used regular Views: you change state, call `invalidate()`, and Android's *UI thread* calls `onDraw()` whenever it gets around to it. That's a *reactive* model — perfect for UIs that only change when tapped, terrible for a ball that must move 60 times per second whether or not anyone touches the screen. `GameView` extends **`SurfaceView`**, which owns a separate drawing surface that any thread is allowed to draw into at any time.

**Why a dedicated thread?** The starter creates a game thread in `surfaceCreated()` and shuts it down in `surfaceDestroyed()`. The thread runs a loop: measure elapsed time → update physics → draw a frame → repeat. Because this happens *off* the UI thread, the game keeps its frame rate even while Android is busy elsewhere — and the UI thread stays free to deliver touch events. The price of threads is care: note the `@Volatile running` flag (so the loop actually sees the stop request) and the `join()` in `surfaceDestroyed()` (which *blocks* until the thread is fully dead — drawing into a destroyed surface is a crash). We cover threads formally in Week 7; the starter is your first working example.

**Why a fixed timestep?** The loop doesn't feed raw frame time into the physics. It accumulates real elapsed time and consumes it in identical 1/60-second slices: a lag spike becomes *several small updates* instead of one giant step (which would let the ball tunnel straight through a brick), and a 120 Hz phone plays at exactly the same speed as a 60 Hz one. Physics stays deterministic; drawing just shows the latest state. `MAX_FRAME_SECONDS` is the other half of the defense: a huge gap (the app was paused) is clamped so the ball doesn't teleport.

**The draw contract.** Every frame: `holder.lockCanvas()` → draw everything → `holder.unlockCanvasAndPost(canvas)` **in a `try/finally`**. If you ever return without unlocking, the surface stays locked forever and the app freezes. The starter also redraws the *entire* frame every time — clear to black, then paddle, ball, and (soon) your bricks. There's no "erase just the ball" in game rendering; you repaint the world.

**Keep the starter's shape.** The README's **Required work** says it in one line: `SurfaceView`, a dedicated render thread with a `volatile` stop flag, and `lockCanvas` / `unlockCanvasAndPost` inside `try` / `finally`. Your additions go at the `TODO(student)` markers; the plumbing around them stays.

## Part 2 — Brick grid

- **5 rows × 8 columns** of bricks near the top of the screen (the `TODO(student)` in `resetGame()` marks the spot).
- Each brick is a `RectF`. Size them **proportionally to screen width/height** (the starter never hard-codes positions or sizes in pixels — keep it that way; text size and text position are the one exception, as in Part 4), with a small visible gap between bricks.
- **Two alternating colors** (checkerboard or by-row both fine) — two `Paint` objects, created once as fields, *never* inside the draw loop. The `TODO(student)` next to `paddlePaint` and `ballPaint` marks where they go.
- Bricks are drawn every frame from the game's state (a list of live bricks, or bricks with an `alive` flag — your choice; a small `data class Brick(val rect: RectF, val paint: Paint)` is a clean approach). The `TODO(student)` in `drawFrame()` marks where they get painted.

Build the grid with two nested loops (`for (row in 0 until 5) for (col in 0 until 8)`). Compute each brick's left/top from `col`/`row` times the brick's width/height, plus the gap. Screen size isn't known until `surfaceChanged()` fires — that's why grid-building belongs in `resetGame(w, h)`, not in the constructor.

## Part 3 — Ball–brick collision

- Each update, test the ball against every *live* brick. `RectF.intersects(a, b)` works if you also make a small `RectF` around the ball; or compare the ball's circle against the brick rect directly.
- On hit: the brick **disappears** (permanently — until reset), the ball's **vertical velocity flips**, and the score increases (add the score variable now if you like; Part 4 draws it).
- One hit should kill **one** brick — if your ball plows through three bricks in one frame, break out of the loop after the first hit.
- The collision `TODO(student)` in `update()` marks where this goes. Collision runs on the **game thread**. `resetGame()` builds the brick list on the UI thread before the game loop starts using it; after that, only the game thread touches the brick list.

Flip `ballVY` *and* nudge the ball out of the brick it hit, or it can hit the same spot twice in consecutive frames — the starter's wall and paddle bounces show the pattern (set the position, then flip the velocity). `MutableList.removeAll { ... }` or an `alive` flag both work for brick removal — an `alive` flag makes "reset the level" trivial later in the arc.

## Part 4 — Score counter

- Points per brick are your choice (10/brick is classic).
- Draw it with **`Canvas.drawText()`** in the corner of the screen, using a `textSize` large enough to read (~48f+). Create the text `Paint` once, as a field — the starter's comment shows the one-liner. The starter's `drawText("Score: $score", …)` hint builds a small new string each frame; that one is fine for this lab. For zero allocations, keep the text in a field and rebuild it only when the score changes.
- Score resets when the game resets.

## Part 5 — Style, comments, and proof of play

**Style and comments.** Keep the starter's commenting standard: your new code should explain *why*, not just *what*. **No allocations in the draw/update path** — no `Paint()`, `RectF()`, or list creation per frame; build once in `resetGame()`, reuse forever (allocating 60 times a second is garbage-collector stutter you can see). Meaningful names, consistent formatting, no dead code. And KDoc on every class and function you add or change, including the `Brick` data class if you write one. The functions that hold this lab's TODOs (`update()`, `drawFrame()`, `resetGame()`) count as changed, so they need KDoc too, and so does the `GameView` class itself, since you add fields to it; use `@param` and `@return` where a function has parameters or a return value. The starter's `//` comments are the explanation standard; KDoc is the documentation standard, and both apply.

**The recording.** A 15–45 second screen recording of gameplay on your emulator or device — show the ball breaking bricks and the score climbing. Save it as `evidence/gameplay.mp4`, the name the README uses. `adb shell screenrecord --time-limit 30 /sdcard/gameplay.mp4` (it stops by itself after 30 seconds, so play while it records) followed by `adb pull /sdcard/gameplay.mp4 evidence/` writes an MP4 directly. Don't stop it with Ctrl+C: in some terminals that leaves a file that won't play. A physical phone's built-in screen recorder works too. If `adb` isn't on your PATH, it's in `platform-tools/` under the SDK folder from Lab 2. Over 25 MB? Put it on Drive, shared so anyone at austincc.edu with the link can view, and put the link under your README's `## Notes` heading instead of committing the file. Don't rename a `.webm` to `.mp4` — record again instead.

**The screenshot and the note.** `evidence/app-running.png` — one frame of play with bricks on screen and the score visible. And two or three sentences in your README under the `## Notes` heading the README names, on what was hardest. Use the heading exactly.

## Grading — Lab 6

The three components and their weights:

| Component | Weight | Points |
|---|---|---|
| **Code Quality** — bricks, collision, score, and style (detail below) | 50% | 50 |
| **Administrative Correctness** — submission specs (including the recording and the README Notes), **complete KDoc**, clean Gradle build with no errors, runs on an emulator or phone. Points earned. | 25% | 25 |
| **Write-up** — answers to this lab's write-up questions, from your own work | 25% | 25 |
| **Total** | | **100** |

**Code Quality scoring detail:**

| Item | Points |
|---|---|
| Brick grid: 5×8, proportional layout, 2 alternating colors | 15 |
| Ball–brick collision: detection, removal, bounce | 20 |
| Score: tracked and drawn with `drawText` | 10 |
| Code style & comments | 5 |
| **Total (of Lab 6's 100)** | **50** |

**The Write-up (25%).** Your repository's `WRITEUP.md` holds five short questions about this lab's own material: the brick grid, collision, allocations, the fixed timestep, and the game thread. Question 4 asks about the lag-spike test in **Hints and common stumbles** — run it before you answer. A few sentences per question is plenty. Mention something specific from your own work, such as a function name, an error message you actually saw, or a commit. **A missing write-up earns zero on the Write-up criterion only (25 of Lab 6's 100 points)** — Code Quality and Administrative Correctness are graded independently on their own merits and keep whatever points they earn. Write it as you go; the specifics are the whole point, and they're the first thing you forget.

## Turning it in

**One GitHub repository** — the one Classroom 50 created for you, completed. The README's **Expected repository structure** is the authoritative list of what must be in it; the only things you add are your code in `GameView.kt`, your screenshot, your recording, your write-up, your README's Notes, and your documentation. **Commit as you go** — grid, collision, score.

**Submit in three steps:**

1. **Run the submission checker, commit, and push.** From the repository root, run `checkSubmission` — the README's **Verify before pushing** section has the exact command for your platform, and it is also available from Android Studio's Gradle panel. It builds the app and prints PASS or FAIL for every required item with a repair hint. It checks structure and evidence; the rubric above is what your features are graded against. Then commit and `git push`.
2. **Run `gh student submit` from inside your repository.** It tags the commit you are submitting; run it again if you push more work. If you don't run it, your last push to the default branch is what gets graded. One-time setup if the command is not found: `gh extension install foundation50/gh-student`. If it fails, check `gh auth status` (sign in with `gh auth login`) and try again; if it still fails, email me the exact error message. A failed submit command never costs you points; your last push is graded.
3. **Paste your repository URL into Blackboard.** After your final push, paste the normal HTTPS web URL of your repository (`https://github.com/gmd-acc-org/...`) into the Blackboard assignment **Lab 6 — Breakout I: Repository URL Proof**.

If you can clone your own repository into an empty folder, open it in Android Studio, run it, and knock out a row of bricks, your submission is complete — that's also the honest test.

**Submission rules** (same as every lab this course):

- **Due 11:59 PM Central, Sun 10/18.**
- **One resubmission.** Turn work in early, get a grade and feedback, resubmit **once** before the due date. Using this requires submitting **several days early** — time for me to grade, you to fix, you to resubmit.
- **Don't submit to get feedback** on incomplete work. Submit when you believe it's complete and correct. Need help before then? Email me — that's what I'm for.
- **Late work (points are earned, not deducted):** up to 24 h late earns up to 80; 24–48 h late earns up to 70; **after 48 h the lab is closed.** Talk to me *before* the window closes if something is going wrong.

## Hints and common stumbles

- **Run the untouched starter first.** Paddle follows your finger, ball bounces off everything — including the floor, which is on purpose: losing a ball costs a life later in the arc, and the `[W9 — Full Game]` `TODO(student)` marks the spot. Leave it bouncing this lab.
- Build the grid with two nested loops; compute left/top from `col`/`row`. If every brick lands in the same place, you multiplied by the index in one axis and forgot the other.
- Screen size isn't known until `surfaceChanged()` fires — that's why grid-building belongs in `resetGame(w, h)`, not in the constructor. `screenW` is `0f` until then, and a grid built from zero is invisible.
- Flip `ballVY` *and* nudge the ball out of the brick it hit, or it can hit the same spot twice in consecutive frames.
- **The ball tunnels through a brick** at high speed: check that collision runs inside `update()` — inside the fixed-timestep loop — not once per drawn frame.
- **The app freezes on the first brick hit**: something threw inside `drawFrame()` before `unlockCanvasAndPost`. The `try/finally` is there so the canvas is released anyway; read Logcat for the exception (an index out of range in your brick list is the usual one).
- **Stutter every second or so**: you're allocating in `update()` or `drawFrame()`. Search both for `Paint(`, `RectF(`, `listOf(`, `mutableListOf(`. Build once in `resetGame()`, reuse forever.
- `MutableList.removeAll { ... }` or an `alive` flag both work for brick removal — an `alive` flag makes "reset the level" trivial when the arc gets there.
- Test on a lag spike: pause the app in the debugger for a second or two (run it with **Debug 'app'**, press the **Pause Program** button, then **Resume Program**). Pulling down the notification shade does not pause the game, so it is not a lag test. The fixed-timestep loop should recover without the ball teleporting through a brick — that is what `MAX_FRAME_SECONDS` and the fixed slices buy you. (Backgrounding the app fully tears down the surface and `resetGame` rebuilds the world — that's the starter's behavior, fine for now.)
- **`adb` says "more than one device"**: an emulator and a phone are both connected. `adb -s <serial> shell screenrecord ...` (serials from `adb devices`), or unplug one.
- Commit early and often: starter runs, grid drawn, collision works, score drawn. If something breaks, `git diff` shows exactly what changed.

## Why this assignment

Every real-time game is this file. A surface you own, a thread that owns the clock, physics that advances in fixed slices, and a renderer that repaints the world from state — Breakout II, the sensor work, and your final project all run inside the loop you're reading this week, which is also why Part 1 is on the midterm. Tic Tac Toe taught you to separate state from view when the view only changes on a tap; Breakout is the same separation with the view changing sixty times a second whether anyone taps or not. Bricks, collision, and a score are the smallest additions that make the loop *a game* — and the no-allocation, proportional-layout, why-not-what habits you pick up adding them are the ones that keep the next four weeks from drowning.

---

*The Breakout arc is adapted, Java to Kotlin, from Dana Marshall's Spring 2025 original.*
