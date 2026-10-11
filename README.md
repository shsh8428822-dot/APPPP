# Yiddish Learner Android

אפליקציית Android ללימוד יידיש למתחילים. כוללת שיעור מובנה, כרטיסיות, תרגול, חזרה במרווחים ותרגול שיחה לא מקוון.

## Build
GitHub Actions builds a debug APK on pushes to `main` and exposes it as a workflow artifact. Open **Actions → Android APK Build → latest run → Artifacts** to download `YiddishLearner-debug-apk`.

## Scope and limitations
- Learning content is bundled in the app and works offline.
- Speech playback depends on whether the Android device has a Yiddish Text-to-Speech voice installed; the app will explain when it is unavailable.
- The included chat practice is a lightweight offline practice helper, not a cloud GPT integration. A real AI tutor needs a secured backend/API key and network access.
- This is an initial learning prototype; proofread Yiddish spellings and dialect variants with a fluent speaker before public release.


## Offline Yiddish dictionary (10,000 entries)
Before building, GitHub Actions downloads machine-readable Yiddish data from Kaikki/Wiktionary and creates an offline TSV asset bundled into the APK. The dictionary screen supports searching Yiddish words or English glosses and adding selected entries to spaced repetition. The first 12 lesson words retain their Hebrew translations; the broader 10,000-entry reference dictionary currently shows English glosses, not Hebrew translations. Dataset source: https://kaikki.org/dictionary/Yiddish/index.html . The data is derived from Wiktionary; see the applicable CC BY-SA license at https://creativecommons.org/licenses/by-sa/4.0/ .
