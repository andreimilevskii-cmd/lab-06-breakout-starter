# Lab 6 Write-up

Answer each question from your own work on this lab. A few sentences per
question is plenty. Mention something specific, such as a function, an error
message you actually saw, or a commit. Replace each `TODO_WRITEUP` with your
answer.

### Q1 — Brick grid: how did you compute each brick's position and size from the screen size? What brick width, height, and gap did that give you on the emulator or phone you used?

I used 8 columns and 5 rows. Brick width is 10.8% of screen width, height is 3.5% of screen height, and gap is 0.8% of screen width. At 1080×1920, that gives 116.64 px width, 67.2 px height, and 8.64 px gap. These are calculated, not emulator-measured.

### Q2 — Collision: how does your code detect a ball–brick hit, and what did you do so one hit removes only one brick and the ball does not hit the same brick again on the next update?

I used RectF.intersects() to detect collisions. The hit brick is immediately removed using iterator.remove(), and break ensures only one brick is removed per update. The ball's vertical velocity reverses.

### Q3 — Allocations: which `Paint`, `RectF`, and list objects did you create once, and where? Did you catch anything allocating inside `update()` or `drawFrame()`?

I created Paint objects, reusable RectF objects, and the brick list as fields. Each brick gets a RectF during reset. update() creates an iterator, and drawFrame() creates the score string each frame.

### Q4 — Fixed timestep: in the debugger-pause lag-spike test from the handout's Hints, how long did you pause, and exactly what did the ball, bricks, and score do when the game resumed?

I did not run the debugger-pause test. The expected behavior is that elapsed time is capped at 0.25 seconds, preventing a huge ball jump. Bricks and score change only when collisions occur.

### Q5 — Threads: name the functions that read or change your brick list and say which thread runs each one. `resetGame()` runs on the UI thread (it is called from `surfaceChanged()`). What in the starter — the `ready` flag, the `join()` in `surfaceDestroyed()` — makes that safe, and why must collision and drawing stay on the game thread?

resetGame() modifies bricks on the UI thread; update() removes bricks and drawFrame() reads them on the game thread. The ready flag prevents updates before initial setup, and join() waits for shutdown. Collision and drawing stay on the game thread to avoid conflicting access. However, resizing after startup would require additional synchronization.