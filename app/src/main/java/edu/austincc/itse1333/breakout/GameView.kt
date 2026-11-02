
/*
 * GameView.kt — ITSE-1333 Breakout starter — THE PEDAGOGICAL CORE
 *
 * This one file demonstrates every game-programming concept from the
 * Week 6–10 lectures. Concepts are tagged with the week they're taught:
 *
 *   [W6] Custom views, Canvas, drawing primitives (Paint, RectF, circles)
 *   [W7] Game loop, dedicated thread, fixed timestep, SurfaceView
 *   [W8] Touch input (onTouchEvent / MotionEvent)  — sound comes later in W8
 *   [W9] Lives / game states (marked as TODO(student) — Breakout: Full Game)
 *
 * WHY SurfaceView INSTEAD OF View?
 * A regular View is drawn by the UI thread: you call invalidate(), Android
 * calls onDraw() when it feels like it. Fine for buttons; bad for games —
 * if the UI thread is busy, your frame is late. A SurfaceView owns a
 * separate drawing surface that ANY thread may draw into, whenever it
 * wants. That lets us run a dedicated game thread that redraws ~60x/sec
 * no matter what the UI thread is doing.
 *
 * THE CONTRACT: SurfaceHolder.Callback
 * You may only draw while the surface exists. Android tells us when it
 * appears/disappears via surfaceCreated()/surfaceDestroyed(). We start our
 * thread in surfaceCreated() and shut it down (join!) in surfaceDestroyed().
 * Drawing after surfaceDestroyed() returns = crash. That's why the join
 * matters: it BLOCKS until the game thread has fully exited.
 */

package edu.austincc.itse1333.breakout

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView

/**
 * Renders and updates the Breakout game on a dedicated SurfaceView thread.
 *
 * @param context Android context used to create the view.
 */
class GameView(context: Context) : SurfaceView(context), SurfaceHolder.Callback {

    companion object {
        // [W7] Fixed timestep: physics always advances in identical slices
        // (1/60 s), no matter how fast the device renders. See gameLoop().
        private const val FIXED_DT = 1f / 60f

        // [W7] If the app is paused/hiccups, the frame gap can be huge.
        // Cap it so the ball doesn't teleport through the paddle.
        private const val MAX_FRAME_SECONDS = 0.25f
    }

    // ------------------------------------------------------------------
    // [W7] GAME THREAD PLUMBING
    // ------------------------------------------------------------------
    // @Volatile: this flag is WRITTEN by the UI thread (surfaceDestroyed)
    // and READ by the game thread (the while-loop). Without @Volatile the
    // game thread is allowed to cache the value and might never see it
    // change — the classic "my thread won't stop" bug.
    @Volatile private var running = false

    // Set true once the surface has real dimensions (surfaceChanged).
    // The loop skips update/draw until then.
    @Volatile private var ready = false

    private var gameThread: Thread? = null

    // ------------------------------------------------------------------
    // GAME STATE (owned by the game thread once the loop is running)
    // ------------------------------------------------------------------
    private var screenW = 0f
    private var screenH = 0f

    // [W6] The paddle is just a rectangle. RectF = float rectangle.
    private val paddle = RectF()

    // [W8] Written by the UI thread in onTouchEvent, read by the game
    // thread in update() — cross-thread again, so @Volatile again.
    // -1 means "no touch yet; leave the paddle where it is".
    @Volatile private var paddleTargetX = -1f

    // [W6] The ball: position + radius…
    private var ballX = 0f
    private var ballY = 0f
    private var ballRadius = 0f
    // …and velocity in PIXELS PER SECOND (not per frame!), so movement
    // is dt-scaled and identical on a slow phone and a fast tablet.
    private var ballVX = 0f
    private var ballVY = 0f

    /**
     * A drawable brick occupying a rectangle in the game world.
     *
     * @param bounds The brick's on-screen rectangle.
     * @param paint The shared paint used to draw its color.
     */
    private data class Brick(val bounds: RectF, val paint: Paint)

    private val bricks = mutableListOf<Brick>()
    private val ballBounds = RectF()
    private var score = 0
    // TODO(student) [W9 — Full Game]: lives counter + game state
    //   (PLAYING / GAME_OVER / WIN) go here.

    // ------------------------------------------------------------------
    // [W6] PAINTS — a Paint holds color/style so Canvas knows how to draw.
    // Create them ONCE here, never inside the draw loop (allocating 60x/sec
    // causes garbage-collector stutter).
    // ------------------------------------------------------------------
    private val paddlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(66, 165, 245) }
    private val ballPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
    private val brickPaints = arrayOf(
        Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(239, 83, 80) },
        Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(255, 202, 40) }
    )
    private val scorePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 48f
    }

    init {
        // Register for surface lifecycle callbacks — without this line,
        // surfaceCreated() is never called and nothing ever happens.
        holder.addCallback(this)
        isFocusable = true
    }

    // ==================================================================
    // [W7] SURFACE LIFECYCLE — SurfaceHolder.Callback
    // ==================================================================

    /** Surface exists → safe to draw → start the game thread. */
    override fun surfaceCreated(holder: SurfaceHolder) {
        running = true
        val thread = Thread { gameLoop() }   // lambda = Runnable (SAM conversion)
        gameThread = thread
        thread.start()
    }

    /** Called at least once after surfaceCreated — NOW we know our size. */
    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        resetGame(width.toFloat(), height.toFloat())
        ready = true
    }

    /**
     * Surface is about to disappear (app backgrounded, activity finished).
     * We must guarantee the game thread is DONE before this method returns,
     * or it could draw into a dead surface and crash.
     */
    override fun surfaceDestroyed(holder: SurfaceHolder) {
        running = false            // 1. ask the loop to exit…
        try {
            gameThread?.join()     // 2. …then WAIT until it actually has.
        } catch (e: InterruptedException) {
            // Interrupted while waiting — nothing sensible to do here.
        }
        gameThread = null
        ready = false
    }

    // ==================================================================
    // [W7] THE GAME LOOP — runs on the game thread, not the UI thread
    // ==================================================================
    //
    // Pattern: measure real elapsed time, accumulate it, then consume it
    // in FIXED slices. Update and draw are separate steps:
    //
    //   while running:
    //     accumulator += real time since last lap
    //     while accumulator >= 1/60:  update(1/60); accumulator -= 1/60
    //     draw()
    //
    // WHY FIXED TIMESTEP? If we passed the raw frame time into the physics,
    // a lag spike would produce one giant step (ball tunnels through a
    // brick); a 120 Hz screen would play "faster" than a 60 Hz one unless
    // everything is perfectly dt-scaled. Fixed slices make physics
    // deterministic; the renderer just draws the latest state.
    private fun gameLoop() {
        var previousNanos = System.nanoTime()
        var accumulator = 0f

        while (running) {
            // Elapsed REAL time since the previous lap, in seconds.
            val now = System.nanoTime()
            var frameSeconds = (now - previousNanos) / 1_000_000_000f
            previousNanos = now
            if (frameSeconds > MAX_FRAME_SECONDS) frameSeconds = MAX_FRAME_SECONDS

            if (!ready) {          // surfaceChanged hasn't sized us yet
                Thread.sleep(5)    // don't hot-spin the CPU while we wait
                continue
            }

            accumulator += frameSeconds
            while (accumulator >= FIXED_DT) {
                update(FIXED_DT)
                accumulator -= FIXED_DT
            }

            drawFrame()
        }
    }

    // ==================================================================
    // UPDATE — all movement & collision, in fixed dt slices
    // ==================================================================
    /**
     * Advances ball and paddle physics, including brick hits, by one fixed step.
     *
     * @param dt Elapsed simulation time in seconds.
     */
    private fun update(dt: Float) {
        // --- [W8] Paddle follows the finger (x only) -------------------
        if (paddleTargetX >= 0f) {
            val halfW = paddle.width() / 2f
            // coerceIn clamps, so the paddle can't slide off screen.
            val centerX = paddleTargetX.coerceIn(halfW, screenW - halfW)
            paddle.offsetTo(centerX - halfW, paddle.top)
        }

        // --- [W6] Ball movement: position += velocity * time -----------
        ballX += ballVX * dt
        ballY += ballVY * dt

        // --- [W6] Wall bounces: flip the velocity component ------------
        if (ballX - ballRadius < 0f) {              // left wall
            ballX = ballRadius
            ballVX = -ballVX
        } else if (ballX + ballRadius > screenW) {  // right wall
            ballX = screenW - ballRadius
            ballVX = -ballVX
        }
        if (ballY - ballRadius < 0f) {              // ceiling
            ballY = ballRadius
            ballVY = -ballVY
        } else if (ballY + ballRadius > screenH) {  // floor
            // TODO(student) [W9 — Full Game]: losing the ball costs a life
            // and respawns it (or triggers GAME_OVER). For now: bounce, so
            // the starter demo runs forever.
            ballY = screenH - ballRadius
            ballVY = -ballVY
        }

        // --- [W6] Paddle bounce ----------------------------------------
        // Only when moving DOWN (prevents the ball sticking inside the
        // paddle and re-bouncing every frame).
        if (ballVY > 0f &&
            ballX in paddle.left..paddle.right &&
            ballY + ballRadius >= paddle.top &&
            ballY + ballRadius <= paddle.bottom
        ) {
            ballY = paddle.top - ballRadius
            ballVY = -ballVY
            // "English": hit near the paddle edge → push the ball sideways.
            // -1 (left edge) … 0 (center) … +1 (right edge)
            val hitOffset = (ballX - paddle.centerX()) / (paddle.width() / 2f)
            ballVX = (ballVX + hitOffset * screenW * 0.25f)
                .coerceIn(-screenW * 0.9f, screenW * 0.9f)
        }

        // Remove only the first hit brick so the ball bounces once per step.
        ballBounds.set(ballX - ballRadius, ballY - ballRadius, ballX + ballRadius, ballY + ballRadius)
        val iterator = bricks.iterator()
        while (iterator.hasNext()) {
            val brick = iterator.next()
            if (RectF.intersects(ballBounds, brick.bounds)) {
                iterator.remove()
                ballVY = -ballVY
                score += 10
                break
            }
        }
    }

    // ==================================================================
    // [W6+W7] DRAW — lock the canvas, paint the frame, ALWAYS unlock
    // ==================================================================
    /** Draws the current game state, always releasing the locked canvas. */
    private fun drawFrame() {
        // lockCanvas() can return null (surface mid-teardown) → just skip.
        val canvas: Canvas = holder.lockCanvas() ?: return
        try {
            canvas.drawColor(Color.BLACK)                       // clear frame
            canvas.drawRect(paddle, paddlePaint)                // [W6] paddle
            canvas.drawCircle(ballX, ballY, ballRadius, ballPaint) // [W6] ball

            for (brick in bricks) {
                canvas.drawRect(brick.bounds, brick.paint)
            }
            canvas.drawText("Score: $score", 32f, 64f, scorePaint)
            // TODO(student) [W9 — Full Game]: draw lives + GAME OVER / WIN.
        } finally {
            // unlockCanvasAndPost SHOWS the frame. The try/finally
            // guarantees we never leave the canvas locked — a locked-forever
            // canvas freezes the app.
            holder.unlockCanvasAndPost(canvas)
        }
    }

    // ==================================================================
    // [W8] TOUCH INPUT — runs on the UI thread!
    // ==================================================================
    // We do NOT move the paddle here. We just record where the finger is
    // (one @Volatile float) and let update() — on the game thread — do the
    // moving. One-way, single-value communication keeps this thread-safe
    // without locks. (Mutexes/synchronized: lecture W8–W9.)
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> paddleTargetX = event.x
            MotionEvent.ACTION_UP -> performClick()   // accessibility nicety
        }
        return true   // true = "I consumed this event, keep sending me more"
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }

    // ==================================================================
    // RESET — (re)build the world for the current screen size.
    // Everything is proportional to screen size: no hard-coded pixels,
    // so it looks the same on a phone and a 10" tablet.
    // ==================================================================
    /**
     * Resets the ball, paddle, score, and 5-by-8 brick grid for this surface.
     *
     * @param w Surface width in pixels.
     * @param h Surface height in pixels.
     */
    private fun resetGame(w: Float, h: Float) {
        screenW = w
        screenH = h

        val paddleW = w * 0.28f
        val paddleH = h * 0.018f
        paddle.set(
            w / 2f - paddleW / 2f,
            h * 0.92f,
            w / 2f + paddleW / 2f,
            h * 0.92f + paddleH
        )

        ballRadius = w * 0.02f
        ballX = w / 2f
        ballY = h * 0.55f
        ballVX = w * 0.30f    // px/sec — crosses the screen in ~3.3 s
        ballVY = -h * 0.35f   // negative = upward (y grows downward!)

        score = 0
        bricks.clear()
        val margin = w * 0.04f
        val gap = w * 0.008f
        val gridTop = h * 0.12f
        val brickW = (w - 2f * margin - 7f * gap) / 8f
        val brickH = h * 0.035f
        for (row in 0 until 5) {
            for (column in 0 until 8) {
                val left = margin + column * (brickW + gap)
                val top = gridTop + row * (brickH + gap)
                bricks.add(Brick(
                    RectF(left, top, left + brickW, top + brickH),
                    brickPaints[(row + column) % 2]
                ))
            }
        }
    }
}
