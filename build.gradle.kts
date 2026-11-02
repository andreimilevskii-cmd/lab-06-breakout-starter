// Root build file for ITSE-1333 Lab 6.
plugins {
    id("com.android.application") version "9.3.2" apply false
}

/**
 * Student-facing submission checker: builds the app and checks the evidence
 * and documentation the compiler cannot. Feature grading follows the handout.
 */
tasks.register("checkSubmission") {
    group = "verification"
    description = "Builds the app and checks the required Lab 6 submission files."
    dependsOn(":app:assembleDebug")

    doLast {
        val failures = mutableListOf<String>()

        fun verify(label: String, passed: Boolean, hint: String) {
            if (passed) println("PASS: $label") else { println("FAIL: $label — $hint"); failures += label }
        }

        listOf(
            "app/build.gradle.kts",
            "app/src/main/AndroidManifest.xml",
            "build.gradle.kts",
            "gradle/wrapper/gradle-wrapper.jar",
            "gradle/wrapper/gradle-wrapper.properties",
            "gradlew",
            "gradlew.bat",
            "settings.gradle.kts",
            "README.md",
            "WRITEUP.md",
        ).forEach { path ->
            verify("required file $path", file(path).isFile, "restore this file at the repository path shown")
        }

        verify(
            "one Android project at the repository root",
            !file("app/app").exists(),
            "do not create a second Android project inside the app module",
        )

        // Exact, case-sensitive names: macOS and Windows would accept
        // App-Running.PNG through file(...).exists(), but Linux and GitHub do not.
        val evidenceNames = file("evidence").list()?.toSet() ?: emptySet()
        fun evidenceFile(name: String): Boolean {
            val f = file("evidence/$name")
            return name in evidenceNames && f.isFile && f.length() > 0
        }

        verify(
            "running-app evidence",
            evidenceFile("app-running.png"),
            "save the emulator or phone screenshot as evidence/app-running.png (exactly this lowercase name; if Git already tracks it under another capitalization, rename it with git mv -f, for example git mv -f evidence/App-Running.PNG evidence/app-running.png)",
        )

        val writeupText = file("WRITEUP.md").takeIf { it.isFile }?.readText() ?: ""
        // Only the answers count: the preamble itself names TODO_WRITEUP.
        val answers = writeupText.substringAfter("### Q1", "")
        val writeupDone = Regex("(?m)^### Q[1-5]\\b").findAll(writeupText).count() == 5 &&
            "TODO_WRITEUP" !in answers
        verify(
            "five write-up answers",
            writeupDone,
            "keep exactly the five headings ### Q1 to ### Q5 (no extra headings starting with ### Q1..Q5) and replace every TODO_WRITEUP with your answer",
        )

        val readmeText = file("README.md").takeIf { it.isFile }?.readText() ?: ""
        verify(
            "README device record",
            "TODO_DEVICE" !in readmeText,
            "replace TODO_DEVICE with the emulator profile or Android phone you used",
        )
        verify(
            "README Notes section",
            Regex("(?m)^## Notes\\s*$").containsMatchIn(readmeText),
            "add a '## Notes' heading to README.md with two or three sentences on what was hardest",
        )
        verify(
            "gameplay recording",
            evidenceFile("gameplay.mp4") || "drive.google.com" in readmeText,
            "save the recording as evidence/gameplay.mp4 (exactly this lowercase name; if Git already tracks it under another capitalization, rename it with git mv -f, for example git mv -f evidence/Gameplay.MP4 evidence/gameplay.mp4), or put its Drive link under ## Notes if it is over 25 MB",
        )

        val kdocMissing = fileTree("app/src/main").matching { include("**/*.kt") }.files
            .filter { "/**" !in it.readText() }
            .map { it.name }
        verify(
            "a KDoc block is present in every Kotlin file (complete KDoc is graded by hand)",
            kdocMissing.isEmpty(),
            "add a KDoc block (/** ... */) to: ${kdocMissing.joinToString()}; complete KDoc is graded by hand",
        )

        if (failures.isNotEmpty()) {
            throw GradleException(
                "Submission check failed: ${failures.size} item(s) need attention. Read the FAIL hints above.",
            )
        }
        println("PASS: Lab 6 structure check complete (files and evidence only); your features are graded by hand against LAB6.md.")
    }
}
