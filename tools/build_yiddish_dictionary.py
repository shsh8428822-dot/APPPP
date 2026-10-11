#!/usr/bin/env python3
"""Build a compact offline Yiddish dictionary from Kaikki's machine-readable Wiktionary data."""
import json
import pathlib
import re
import urllib.request

SOURCE_URL = "https://kaikki.org/dictionary/Yiddish/kaikki.org-dictionary-Yiddish.jsonl"
TARGET = pathlib.Path("app/src/main/assets/yiddish_dictionary.tsv")
TARGET_COUNT = 10_000
USER_AGENT = "YiddishLearnerAndroid/1.1 (offline dictionary build)"

def clean_field(value):
    return re.sub(r"[\t\r\n]+", " ", str(value or "")).strip()

def is_yiddish_script(word):
    return any(0x0590 <= ord(ch) <= 0x05FF for ch in word)

def choose_gloss(item):
    for sense in item.get("senses", []) or []:
        glosses = sense.get("glosses") or []
        vals = [clean_field(g) for g in glosses if clean_field(g)]
        if vals:
            return "; ".join(vals[:2])[:360]
    return ""

def main():
    request = urllib.request.Request(SOURCE_URL, headers={"User-Agent": USER_AGENT})
    unique = {}
    with urllib.request.urlopen(request, timeout=180) as response:
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
    TARGET.parent.mkdir(parents=True, exist_ok=True)
    with TARGET.open("w", encoding="utf-8", newline="") as out:
        out.write("id\tyiddish\tpos\tgloss\n")
        for index, (word, pos, gloss) in enumerate(entries, start=1):
            out.write(f"{index}\t{clean_field(word)}\t{clean_field(pos)}\t{clean_field(gloss)}\n")
    print(f"Wrote {len(entries)} unique Yiddish dictionary entries to {TARGET}")

if __name__ == "__main__":
    main()
