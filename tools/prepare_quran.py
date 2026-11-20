#!/usr/bin/env python3
"""Copy the QCF4 Madani-Mushaf data (pages + fonts) into the app assets WITHOUT touching a single
glyph, and refuse to continue (exit 1 -> build fails) if anything is missing or the counts are wrong.
usage: prepare_quran.py <extracted npm package dir> <assets/quran output dir>"""
import json, os, shutil, sys

src, out = sys.argv[1], sys.argv[2]
JUZ = [1,22,42,62,82,102,121,142,162,182,202,222,242,262,282,302,322,342,362,382,402,422,442,462,482,502,522,542,562,582]
def die(m):
    print("QURAN CHECK FAILED:", m); sys.exit(1)

fonts_dir = os.path.join(src, "fonts-woff2")
def font_file(name):
    return name + ".woff2" if name == "QCF4_QBSML" else name + "_W.woff2"

verses, surah_start, surah_name, used_fonts = set(), {}, {}, set()
os.makedirs(os.path.join(out, "pages"), exist_ok=True)
os.makedirs(os.path.join(out, "fonts"), exist_ok=True)
for p in range(1, 605):
    fn = "%03d.json" % p
    path = os.path.join(src, "pages", fn)
    if not os.path.isfile(path): die("missing page " + fn)
    d = json.load(open(path, encoding="utf-8"))
    if d.get("page") != p or not d.get("lines"): die("bad page " + fn)
    used_fonts.add(d["font"])
    for s in d.get("surahs", []):
        if s["verse_start"] == 1:
            if s["id"] in surah_start: die("surah %s starts twice" % s["id"])
            surah_start[s["id"]] = p
        surah_name[s["id"]] = s["name_arabic"]
    for ln in d["lines"]:
        for w in ln["words"]:
            used_fonts.add(w["font"])
            if "code" not in w: die("word without code on " + fn)
            if w.get("verse_key"): verses.add(w["verse_key"])
    shutil.copyfile(path, os.path.join(out, "pages", fn))

if len(verses) != 6236: die("verses = %d (expected 6236)" % len(verses))
if sorted(surah_start) != list(range(1, 115)): die("surahs found: %d (expected 114)" % len(surah_start))
for f in used_fonts:
    fp = os.path.join(fonts_dir, font_file(f))
    if not os.path.isfile(fp): die("missing font " + fp)
for f in sorted(os.listdir(fonts_dir)):
    if f.endswith(".woff2"): shutil.copyfile(os.path.join(fonts_dir, f), os.path.join(out, "fonts", f))
for extra in ("LICENSE.md",):
    if os.path.isfile(os.path.join(src, extra)): shutil.copyfile(os.path.join(src, extra), os.path.join(out, extra))

meta = {"pages": 604, "verses": 6236, "juz": JUZ,
        "surahs": [{"id": i, "name": surah_name[i], "page": surah_start[i]} for i in range(1, 115)]}
json.dump(meta, open(os.path.join(out, "meta.json"), "w", encoding="utf-8"), ensure_ascii=False)
print("OK: 604 pages, 114 surahs, 6236 verses, %d fonts" % len(os.listdir(os.path.join(out, "fonts"))))
