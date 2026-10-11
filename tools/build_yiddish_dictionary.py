#!/usr/bin/env python3
"""Create a 10,000-entry offline Yiddish dictionary with Hebrew machine-translated glosses."""
import json
import pathlib
import re
import urllib.request

SOURCE_URL = "https://kaikki.org/dictionary/Yiddish/kaikki.org-dictionary-Yiddish.jsonl"
MODEL_URLS = [
    "https://argos-net.com/v1/translate-en_he-1_5.argosmodel",
    "https://cdn.argosopentech.io/translate-en_he-1_5.argosmodel",
]
TARGET = pathlib.Path("app/src/main/assets/yiddish_dictionary.tsv")
MODEL_PATH = pathlib.Path(".cache/translate-en_he-1_5.argosmodel")
CACHE_PATH = pathlib.Path(".cache/yiddish_glosses_he.json")
TARGET_COUNT = 10_000
USER_AGENT = "YiddishLearnerAndroid/1.1 (offline dictionary build)"
HEBREW_RANGE = range(0x0590, 0x05FF + 1)

def clean_field(value):
    return re.sub(r"[\t\r\n]+", " ", str(value or "")).strip()

def has_hebrew(value):
    return any(ord(ch) in HEBREW_RANGE for ch in value)

def is_yiddish_script(word):
    return has_hebrew(word)

def choose_gloss(item):
    for sense in item.get("senses", []) or []:
        vals = [clean_field(g) for g in (sense.get("glosses") or []) if clean_field(g)]
        if vals:
            return "; ".join(vals[:2])[:360]
    return ""

def download(url, path):
    path.parent.mkdir(parents=True, exist_ok=True)
    request = urllib.request.Request(url, headers={"User-Agent": USER_AGENT})
    with urllib.request.urlopen(request, timeout=240) as response, path.open("wb") as output:
        while True:
            chunk = response.read(1024 * 1024)
            if not chunk:
                break
            output.write(chunk)

def load_translator():
    from argostranslate import package as argos_package
    from argostranslate import translate as argos_translate

    installed = argos_translate.get_installed_languages()
    en = next((language for language in installed if language.code == "en"), None)
    he = next((language for language in installed if language.code == "he"), None)
    direct = en.get_translation(he) if en and he else None
    if direct is not None:
        return direct
    if not MODEL_PATH.exists():
        last_error = None
        for url in MODEL_URLS:
            try:
                print(f"Downloading English-to-Hebrew translation model from {url}", flush=True)
                download(url, MODEL_PATH)
                last_error = None
                break
            except Exception as exc:
                last_error = exc
                if MODEL_PATH.exists():
                    MODEL_PATH.unlink()
        if last_error:
            raise SystemExit(f"Could not download the English-to-Hebrew model: {last_error}")

    print("Installing the English-to-Hebrew translation model", flush=True)
    argos_package.install_from_path(str(MODEL_PATH))
    installed = argos_translate.get_installed_languages()
    en = next((language for language in installed if language.code == "en"), None)
    he = next((language for language in installed if language.code == "he"), None)
    if not en or not he:
        raise SystemExit("The Hebrew translation model did not install correctly.")
    direct = en.get_translation(he)
    if direct is None:
        raise SystemExit("No direct English-to-Hebrew translation path was found.")
    return direct

def load_cache():
    try:
        data = json.loads(CACHE_PATH.read_text(encoding="utf-8"))
        return data if isinstance(data, dict) else {}
    except (OSError, json.JSONDecodeError):
        return {}

def save_cache(cache):
    CACHE_PATH.parent.mkdir(parents=True, exist_ok=True)
    temporary = CACHE_PATH.with_suffix(".tmp")
    temporary.write_text(json.dumps(cache, ensure_ascii=False), encoding="utf-8")
    temporary.replace(CACHE_PATH)

def main():
    request = urllib.request.Request(SOURCE_URL, headers={"User-Agent": USER_AGENT})
    unique = {}
    with urllib.request.urlopen(request, timeout=240) as response:
        for raw in response:
            try:
                item = json.loads(raw.decode("utf-8"))
            except (UnicodeDecodeError, json.JSONDecodeError):
                continue
            if item.get("lang_code") not in ("yi", "yid"):
                continue
            word = clean_field(item.get("word"))
            pos = clean_field(item.get("pos"))
            if not word or not is_yiddish_script(word):
                continue
            if pos.lower() in {"romanization", "letter", "character", "punctuation mark", "symbol"}:
                continue
            gloss = choose_gloss(item)
            if not gloss:
                continue
            key = word.casefold()
            if key not in unique:
                unique[key] = (word, pos, gloss)
            elif len(gloss) > len(unique[key][2]):
                old_word, old_pos, _ = unique[key]
                unique[key] = (old_word, old_pos or pos, gloss)

    entries = sorted(unique.values(), key=lambda row: (row[0].casefold(), row[0]))
    if len(entries) < TARGET_COUNT:
        raise SystemExit(
            f"Expected at least {TARGET_COUNT} distinct Yiddish-script entries with glosses, "
            f"but found only {len(entries)}. Refusing to ship fabricated or duplicate entries."
        )
    entries = entries[:TARGET_COUNT]
    raw_glosses = list(dict.fromkeys(gloss for _, _, gloss in entries))
    cache = load_cache()
    missing = [gloss for gloss in raw_glosses if gloss not in cache]
    print(
        f"Collected {len(entries)} unique entries; {len(raw_glosses)} distinct glosses, "
        f"{len(missing)} need Hebrew translation.",
        flush=True,
    )
    translator = load_translator() if missing else None
    translated = {}
    failures = 0
    for number, gloss in enumerate(raw_glosses, start=1):
        if gloss in cache and isinstance(cache[gloss], str) and cache[gloss].strip():
            translated[gloss] = cache[gloss]
            continue
        try:
            result = clean_field(translator.translate(gloss))
            if not result or not has_hebrew(result):
                result = "נדרש בירור במילון עברי"
                failures += 1
            else:
                cache[gloss] = result
        except Exception as exc:
            print(f"Warning: translation failed for gloss {number}: {exc}", flush=True)
            result = "נדרש בירור במילון עברי"
            failures += 1
        translated[gloss] = result
        if number % 100 == 0:
            print(f"Processed {number}/{len(raw_glosses)} distinct glosses", flush=True)
        if number % 50 == 0:
            save_cache(cache)
    if failures > 500:
        raise SystemExit(
            f"Too many glosses ({failures}) lacked a usable Hebrew translation. "
            "Refusing to publish a dictionary with too many unresolved meanings."
        )

    TARGET.parent.mkdir(parents=True, exist_ok=True)
    with TARGET.open("w", encoding="utf-8", newline="") as out:
        out.write("id\tyiddish\tpos\tmeaning_he\n")
        for index, (word, pos, gloss) in enumerate(entries, start=1):
            out.write(f"{index}\t{clean_field(word)}\t{clean_field(pos)}\t{clean_field(translated[gloss])}\n")
    save_cache(cache)
    print(
        f"Wrote {len(entries)} unique Yiddish entries with Hebrew meanings to {TARGET}. "
        f"Unresolved meanings requiring dictionary review: {failures}.",
        flush=True,
    )

if __name__ == "__main__":
    main()
