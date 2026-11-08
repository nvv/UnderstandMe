---
name: release
description: Prepare a release of the Android app. Bumps versionCode/versionName, checks the release bundle builds, summarizes changes since the last release for release notes. Use when the user wants to cut a new Play Store version.
argument-hint: [new versionName]
---

# Prepare a release

The signed AAB is produced by GitHub Actions (`.github/workflows/release.yml`, manual
`workflow_dispatch`): it runs `./gradlew bundleRelease` on JDK 17 and signs with repository secrets.
This skill only prepares the code. The user triggers the workflow and uploads to Play.

## 1. Versions (`app/build.gradle.kts`)

- `versionCode`: +1. It must be higher than every build ever uploaded to Play (ask the user if unsure).
- `versionName`: what the user asks for. Otherwise propose one (patch bump for fixes, minor bump for features) and confirm.

## 2. Release notes

- Find the last release point: ask the user, or look for the last `bump version*` commit (`git log --oneline`).
- Summarize user-visible changes since then in plain language, a few bullets, as a draft for the
  Play Console "What's new" text (under 500 characters).

## 3. Check the build

- `./gradlew test bundleRelease`. The AAB lands in `app/build/outputs/bundle/release/app-release.aab` (unsigned locally).
- If Gradle/JDK requirements changed since the workflow was written, check the workflow's
  `java-version` still works (Gradle 9 + AGP 9 need JDK 17+), and tell the user if it needs updating.

## 4. Hand over

Report:
- the version change
- the release-notes draft
- the build result
- what to test on a device before publishing (especially release-only behaviour, if minification is ever enabled)

Don't commit, tag or push. The user does that after review.
