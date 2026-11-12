"""Wikimedia Commons'dan ochiq litsenziyali fon rasmlarini topish/yuklash (CI'da ishlaydi).
mode=scan: nomzodlar ro'yxati + kichik ko'rinishlar (contact sheet).
mode=fetch: scripts/backgrounds.json dagi fayllarni yuklab, telefonga mos kesib webp qiladi."""
import json, os, sys, io, re, time, urllib.parse, urllib.request
from PIL import Image, ImageDraw

API = "https://commons.wikimedia.org/w/api.php"
UA = {"User-Agent": "HijriyTaqvimBot/1.0 (https://github.com/rndesign2001-coder/Hijriy)"}
OK = re.compile(r"^(cc0|public domain|pd|cc by(-sa)? ?[1-4]\.0|cc by(-sa)? ?2\.5|cc by(-sa)? ?3\.0.*)", re.I)

def get(params):
    params = dict(params, format="json", formatversion="2")
    url = API + "?" + urllib.parse.urlencode(params)
    for i in range(4):
        try:
            with urllib.request.urlopen(urllib.request.Request(url, headers=UA), timeout=60) as r:
                return json.load(r)
        except Exception as e:
            print("retry", e); time.sleep(3)
    return {}

def blob(url):
    for i in range(4):
        try:
            with urllib.request.urlopen(urllib.request.Request(url, headers=UA), timeout=120) as r:
                return r.read()
        except Exception as e:
            print("retry", e); time.sleep(5)
    raise RuntimeError(url)

def info(titles, width):
    out = []
    for i in range(0, len(titles), 40):
        d = get({"action": "query", "titles": "|".join(titles[i:i+40]), "prop": "imageinfo",
                 "iiprop": "url|size|extmetadata", "iiurlwidth": width})
        for p in d.get("query", {}).get("pages", []):
            ii = (p.get("imageinfo") or [None])[0]
            if not ii: continue
            m = ii.get("extmetadata", {})
            v = lambda k: re.sub("<[^>]+>", "", (m.get(k) or {}).get("value", "")).strip()
            out.append({"title": p["title"], "w": ii.get("width"), "h": ii.get("height"),
                        "thumb": ii.get("thumburl"), "page": ii.get("descriptionurl"),
                        "license": v("LicenseShortName"), "artist": v("Artist")[:120]})
    return out

def scan():
    titles = []
    cfg = json.load(open("scripts/scan_cfg.json")) if os.path.exists("scripts/scan_cfg.json") else None
    cats = cfg["cats"] if cfg else ["Quality images of Mecca", "Featured pictures of Mecca", "Valued images of Mecca", "Kaaba",
            "Masjid al-Haram", "Masjid al-Haram at night", "Quality images of Medina", "Featured pictures of Medina",
            "Al-Masjid an-Nabawi", "Green Dome", "Exterior of Al-Masjid an-Nabawi", "Umbrellas of Al-Masjid an-Nabawi",
            "Abraj Al Bait", "Quality images of mosques in Saudi Arabia"]
    for c in cats:
        d = get({"action": "query", "list": "categorymembers", "cmtitle": "Category:" + c, "cmtype": "file", "cmlimit": 200})
        got = [x["title"] for x in d.get("query", {}).get("categorymembers", [])]
        print(c, len(got)); titles += got
    for q in cfg["queries"] if cfg else ["Kaaba Masjid al-Haram", "Masjid al-Haram night", "Masjid an-Nabawi", "Prophet's Mosque Medina dome", "Mecca Kaaba aerial"]:
        d = get({"action": "query", "list": "search", "srsearch": q + " filetype:bitmap", "srnamespace": 6, "srlimit": 50})
        got = [x["title"] for x in d.get("query", {}).get("search", [])]
        print(q, len(got)); titles += got
    titles = list(dict.fromkeys(t for t in titles if t.lower().endswith((".jpg", ".jpeg", ".png", ".webp", ".tif", ".tiff"))))
    items = [x for x in info(titles, 480) if x["w"] and x["w"] >= 1600 and x["w"] >= x["h"] and OK.match(x["license"] or "")]
    os.makedirs("out", exist_ok=True)
    json.dump(items, open("out/candidates.json", "w"), ensure_ascii=False, indent=1)
    print("nomzodlar:", len(items))
    # contact sheets: 4x5 grid, raqamli
    tiles = []
    for i, x in enumerate(items[:240]):
        try:
            im = Image.open(io.BytesIO(blob(x["thumb"]))).convert("RGB"); im.thumbnail((360, 240))
            t = Image.new("RGB", (360, 260), "black"); t.paste(im, (0, 0))
            ImageDraw.Draw(t).text((6, 242), f"#{i} {x['w']}x{x['h']} {x['license'][:16]}", fill="yellow")
            tiles.append(t)
        except Exception as e:
            print("skip", x["title"], e)
    for s in range(0, len(tiles), 20):
        sheet = Image.new("RGB", (360 * 4, 260 * 5), "black")
        for k, t in enumerate(tiles[s:s+20]):
            sheet.paste(t, ((k % 4) * 360, (k // 4) * 260))
        sheet.save(f"out/sheet_{s//20:02d}.jpg", quality=80)

def fetch():
    cfg = json.load(open("scripts/backgrounds.json"))
    os.makedirs("out/res", exist_ok=True)
    meta = {x["title"]: x for x in info([c["title"] for c in cfg], 2560)}
    credits = []
    for c in cfg:
        x = meta[c["title"]]
        im = Image.open(io.BytesIO(blob(x["thumb"]))).convert("RGB")
        # telefon ekrani uchun: balandlikni 1600 px ga, kenglik markaz (yoki fx) bo'yicha 1080 px kesiladi
        W, H = c.get("W", 1440), c.get("H", 1800)
        sc = max(W / im.width, H / im.height)
        im = im.resize((round(im.width * sc), round(im.height * sc)), Image.LANCZOS)
        fx = c.get("fx", 0.5)
        left = min(max(0, round(im.width * fx - W / 2)), im.width - W)
        top = min(max(0, round(im.height * c.get("fy", 0.5) - H / 2)), im.height - H)
        im = im.crop((left, top, left + W, top + H))
        im.save(f"out/res/{c['name']}.webp", "WEBP", quality=78, method=6)
        credits.append({"name": c["name"], "title": c["title"], "artist": x["artist"], "license": x["license"], "page": x["page"]})
        print(c["name"], os.path.getsize(f"out/res/{c['name']}.webp"))
    json.dump(credits, open("out/res/credits.json", "w"), ensure_ascii=False, indent=1)

def pick():
    items = json.load(open("scripts/pick.json"))
    os.makedirs("out", exist_ok=True)
    tiles = []
    for x in items:
        try:
            im = Image.open(io.BytesIO(blob(x["thumb"]))).convert("RGB"); im.thumbnail((360, 240))
            t = Image.new("RGB", (360, 260), "black"); t.paste(im, (0, 0))
            ImageDraw.Draw(t).text((6, 242), f"#{x['idx']} {x['w']}x{x['h']} {x['license'][:16]}", fill="yellow")
            tiles.append(t)
        except Exception as e:
            print("skip", x["title"], e)
    for s in range(0, len(tiles), 20):
        sheet = Image.new("RGB", (360 * 4, 260 * 5), "black")
        for k, t in enumerate(tiles[s:s+20]):
            sheet.paste(t, ((k % 4) * 360, (k // 4) * 260))
        sheet.save(f"out/pick_{s//20:02d}.jpg", quality=82)

{"scan": scan, "fetch": fetch, "pick": pick}[sys.argv[1]]()
