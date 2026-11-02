# ITSE-1333 Lab 6 — Breakout I — Custom Views, Canvas, Game Loop

**Assigned:** September 30, 2026<br>
**Due:** October 18, 2026 at 11:59 PM (Central)<br>
**Points:** 100<br>
**Full handout:** `LAB6.md` in this repository. The handout owns the feature
spec, the rubric, and grading; this README owns the mechanics (get the project,
build, verify, push, submit). If a step in this README and the handout ever
disagree, follow this README; the handout owns the rubric and grading.

Use the same Android Studio you used for Lab 5; this project uses the same
Gradle (9.7.0) and Android Gradle Plugin (9.3.2) versions.

This Classroom 50 repository is already a complete Android Studio project.
The repository root is the project root; `app/` is the Android application
module. **Do not create a blank Android Studio project and pull this repository
on top of it.** Both projects would contain Gradle and app files, which creates
conflicts. Get the repository onto your computer first, then open that folder
in Android Studio. You never need to set up Git inside Android Studio.

## Start here

1. Accept the Lab 6 Classroom 50 assignment and open the private GitHub
   repository it creates.
2. Get the project onto your computer the same way you did in Lab 1: copy the
   repository's HTTPS URL, open a terminal, and clone it.

   ```text
   git clone <your-private-Classroom-50-repository-URL>
   ```

   Replace the whole placeholder, including the `<` and `>`, with your
   repository's HTTPS URL.
3. Start Android Studio and choose **Open** on the Welcome screen, or
   **File > Open**. Select the cloned folder, the one that contains
   `settings.gradle.kts`, and choose **OK**. When Android Studio asks whether
   to trust the project, choose **Trust Project**.
4. Confirm that the opened root contains `settings.gradle.kts`, this README,
   and the `app/` folder. Do not choose **New Project**, and do not create a
   second project inside `app/`.
5. Let Gradle sync. If Android Studio offers to install Android SDK 34, accept
   the license and install it. The first sync takes a few minutes; later syncs
   are quick.
6. Choose an emulator or a connected Android phone, then run the starter. It
   should build and launch before you change anything.

## Required work

- Work in `GameView.kt` at the `TODO(student)` markers: the 5×8 brick grid, ball-brick collision, and the score drawn with `drawText`.
- Keep the starter's shape: `SurfaceView`, a dedicated render thread with a `volatile` stop flag, and `lockCanvas` / `unlockCanvasAndPost` inside `try` / `finally`.
- A 15–45 second screen recording of play, saved as `evidence/gameplay.mp4` (`LAB6.md` Part 5 has the `adb shell screenrecord --time-limit 30` command). If it is over 25 MB, upload it to Google Drive instead, shared so anyone at austincc.edu with the link can view, and put the link under `## Notes` in this README.
- Two or three sentences in this README, under a `## Notes` heading you add, on what was hardest.
- Run the finished app on an emulator or physical Android phone and save the
  screenshot as `evidence/app-running.png`.
- KDoc on every class and function you add or change, including the `Brick`
  data class if you write one. The functions that hold this lab's TODOs
  (`update()`, `drawFrame()`, `resetGame()`) count as changed, so they need
  KDoc too, and so does the `GameView` class itself, since you add fields to it;
  use `@param` and `@return` where a function has parameters or a return value.
- Answer the five write-up questions in `WRITEUP.md` from your own work.
  Replace every `TODO_WRITEUP`.
- Replace the device line below with the emulator profile or Android phone you
  used.

**Device used:** Pixel 8 Pro

## Verify before pushing

From the repository root, run the command for your platform.

Windows (PowerShell or Command Prompt):

```text
.\gradlew.bat checkSubmission
```

macOS or Linux (and Git Bash on Windows):

```text
./gradlew checkSubmission
```

The task builds the Android app, prints `PASS` or `FAIL` for every required
item, gives a repair hint for each failure, and exits unsuccessfully until all
checks pass. It checks structure and evidence; the handout's rubric is what
your features are graded against.

If the command says `JAVA_HOME` is not set, run the same check from Android
Studio instead: open the **Gradle** panel and double-click
**Tasks > verification > checkSubmission**.

Then commit and push:

```text
git status
git add .
git commit -m "Complete Lab 6"
git push
```

Open your private repository in GitHub and confirm that the latest commit and
all required files are visible. Push freely while you work; the submission
steps below say which commit is graded.

## Submit

1. Run `checkSubmission` (above), commit, and `git push`.
2. Submit through Classroom 50 with the GitHub CLI, from inside this
   repository:

   ```text
   gh student submit
   ```

   The command tags the commit you are submitting; run it again if you push
   more work. If you don't run it, your last push to the default branch is
   what gets graded. One-time setup if the command is not found:
   `gh extension install foundation50/gh-student`. If it fails, check
   `gh auth status` (sign in with `gh auth login`) and try again; if it still
   fails, email me the exact error message. A failed submit command never
   costs you points; your last push is graded.
3. After your final push, paste the normal HTTPS web URL of this repository
   (`https://github.com/gmd-acc-org/...`) into the Blackboard assignment
   **Lab 6 — Breakout I: Repository URL Proof**. Use the repository's web
   address, not the SSH or `.git` URL, a file or commit link, or the
   Classroom 50 accept link.

## Expected repository structure

```text
your-repository/
  .gitignore
  LAB6.md
  README.md
  WRITEUP.md
  app/build.gradle.kts
  app/src/main/AndroidManifest.xml
  app/src/main/java/edu/austincc/itse1333/breakout/GameView.kt
  app/src/main/java/edu/austincc/itse1333/breakout/MainActivity.kt
  app/src/main/res/values/strings.xml
  build.gradle.kts
  gradle.properties
  gradle/wrapper/gradle-wrapper.jar
  gradle/wrapper/gradle-wrapper.properties
  gradlew
  gradlew.bat
  settings.gradle.kts
  evidence/app-running.png
  evidence/gameplay.mp4      (or a Drive link under ## Notes in README.md)
```

Do not commit `local.properties`, `.gradle/`, `build/`, or `app/build/`.
