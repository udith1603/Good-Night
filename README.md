# Good Night Sender

Schedule a time, and WhatsApp opens with your message already typed in.
You just tap Send.

This repo builds itself into an installable `.apk` using GitHub Actions —
you never need to install Android Studio, Java, or the Android SDK on your
own computer.

## Get the APK (no local setup needed)

1. **Create a new GitHub repository** (private is fine) — e.g. `good-night-sender`.
2. **Upload every file in this project**, keeping the folder structure
   exactly as-is (the `.github/workflows/build-apk.yml` file must stay at
   `.github/workflows/build-apk.yml`). Easiest way: on github.com, use
   "Add file → Upload files" and drag the whole folder in, or use
   `git push` if you're comfortable with git.
3. Go to the **Actions** tab of your repo. A build should already be
   running (it triggers automatically on push). If not, click
   **"Build APK" → "Run workflow"**.
4. Wait ~2-3 minutes for it to go green.
5. Click into the finished run, scroll to **Artifacts**, and download
   **good-night-sender-apk** — it's a zip containing `app-debug.apk`.
6. Transfer that `.apk` to your phone (Google Drive, email to yourself,
   USB, whatever's easiest) and tap it to install.
7. Android will warn about installing from an unknown source the first
   time — that's expected for any app not from the Play Store. Allow it
   for that one file.

Every time you push a change to this repo, a fresh APK builds
automatically — same download steps.

## Using the app

1. Open the app.
2. Enter her number in international format, digits only, no `+`, no
   spaces — e.g. `919812345678`.
3. Type your goodnight message.
4. Pick the time on the picker.
5. Leave **Repeat every day** checked (recommended, so you set it once
   and forget it).
6. Tap **Add scheduled message**.
7. **Long-press** any entry in the list below to Test it immediately or
   Delete it.

On first launch Android may prompt for an "Alarms & reminders" permission
— allow it, otherwise exact-time alarms won't fire on Android 12+.

## Why one tap and not zero taps

WhatsApp has no public API for personal numbers to send without user
interaction. The only way to remove that last tap is an Accessibility
Service that auto-clicks Send for you — it's doable, but more fragile
(breaks if WhatsApp updates its UI) and needs a broad permission grant.
Happy to build that as a v2 if you want to upgrade later.

## If a battery-hungry phone (Xiaomi/Oppo/Vivo/etc.) skips the alarm

Go to Settings → Battery → find "Good Night Sender" → disable battery
optimization / allow background activity for it. Aggressive OEM battery
managers are the #1 cause of "missed" alarms on Android, not this app.
