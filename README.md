# istii_the_great

Offline-first gamified study app (native Android, Java, no external libraries).
100% free, no ads, no account. Everything stays on the phone.

## Build the APK (free, on GitHub)
1. Push this whole repo to GitHub (branch `main`).
2. Open the **Actions** tab, workflow **Build istii_the_great APK** runs automatically
   (or press *Run workflow*).
3. Download the artifact **istii-the-great-debug-apk**, unzip it and install `app-debug.apk`.

`app/istii-debug.jks` is a fixed debug keystore so every new build installs over the previous one.

If the app ever crashes, the next launch shows an error-report screen - send a screenshot.
