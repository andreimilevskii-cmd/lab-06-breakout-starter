/*
 * MainActivity.kt — ITSE-1333 Breakout starter
 *
 * Notice what's DIFFERENT from our earlier apps (Simple App, Tic Tac Toe):
 *
 *   1. We extend plain android.app.Activity, not AppCompatActivity.
 *      AppCompat exists to backport modern *widget* styling — but a game
 *      draws every pixel itself on a Canvas, so we don't need the
 *      appcompat/material libraries at all (check app/build.gradle.kts:
 *      the dependencies block is empty).
 *
 *   2. There is NO XML layout and NO setContentView(R.layout.…).
 *      The entire UI is one custom view — GameView — created in code
 *      and handed to setContentView directly.
 *
 * All the interesting stuff lives in GameView.kt. Start reading there.
 *
 * ITSE-1333-001 Fall 2026 (CRN 40105) — Weeks 6–10 Breakout arc.
 */

package edu.austincc.itse1333.breakout

import android.app.Activity
import android.os.Bundle

/** Hosts the game: the whole screen is one [GameView]. */
class MainActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // One line of UI: the game view IS the screen.
        // (Orientation is locked to portrait in AndroidManifest.xml, so we
        // don't have to survive rotation mid-game — one less problem for now.)
        setContentView(GameView(this))
    }
}
