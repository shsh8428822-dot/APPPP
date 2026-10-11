# Yiddish Learner Android

אפליקציית Android ללימוד יידיש למתחילים. כוללת שיעור מובנה, כרטיסיות, תרגול, חזרה במרווחים ותרגול שיחה לא מקוון.

## Build
GitHub Actions builds a debug APK on pushes to `main` and exposes it as a workflow artifact. Open **Actions → Android APK Build → latest run → Artifacts** to download `YiddishLearner-debug-apk`.

## Scope and limitations
- Learning content is bundled in the app and works offline.
- Speech playback depends on whether the Android device has a Yiddish Text-to-Speech voice installed; the app will explain when it is unavailable.
- The included chat practice is a lightweight offline practice helper, not a cloud GPT integration. A real AI tutor needs a secured backend/API key and network access.
- This is an initial learning prototype; proofread Yiddish spellings and dialect variants with a fluent speaker before public release.
