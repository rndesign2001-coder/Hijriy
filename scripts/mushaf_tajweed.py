"""mushaf.json qatorlariga tajvid ranglarini ko'chiradi (quran.json dagi cpfair/quran-tajweed belgilari asosida).
Natija: app/src/main/assets/mushaf_tajweed.json — {"p": [[ [s,e,r, s,e,r, ...] har qator uchun ], ...]}"""
import json, re, sys
A = 'app/src/main/assets/'
q = json.load(open(A + 'quran.json'))
m = json.load(open(A + 'mushaf.json'))
L = lambda w: re.sub('[^ء-ؿف-يٱ]', '', w)

# Tanzil so'zlari: (matn, har harf uchun qoida yoki -1)
tw = []
for s in q['suras']:
    for a in s['a']:
        text, ann = a[0], a[1]
        col = [-1] * len(text)
        for i in range(0, len(ann) - 2, 3):
            for k in range(ann[i], min(ann[i + 1], len(text))):
                col[k] = ann[i + 2]
        pos = 0
        for w in text.split(' '):
            if w:
                tw.append((w, col[pos:pos + len(w)]))
            pos += len(w) + 1

# Mushaf so'zlari: (sahifa, qator, boshlanish indeksi, so'z)
mw = []
for pi, p in enumerate(m['pages']):
    for li, l in enumerate(p):
        if l[0] not in (0, 1):
            continue
        pos = 0
        for w in l[1].split(' '):
            if w and not w.startswith('۝') and w not in ('۞', '۩'):
                mw.append((pi, li, pos, w))
            pos += len(w) + 1

def project(src, cols, dst):
    """Tanzil so'zi ranglarini mushaf so'ziga ko'chiradi (tatvil va kichik farqlarga chidamli)."""
    out = [-1] * len(dst)
    j = 0
    for i, ch in enumerate(dst):
        if ch == 'ـ':  # tatvil — oldingi harf rangi
            out[i] = out[i - 1] if i else -1
            continue
        k = j
        while k < len(src) and k < j + 4 and src[k] != ch:
            k += 1
        if k < len(src) and src[k] == ch:
            out[i] = cols[k]; j = k + 1
        else:
            out[i] = cols[j] if j < len(cols) else -1
    return out

spans = [[[] for _ in p] for p in m['pages']]
def emit(pi, li, base, cols):
    i = 0
    while i < len(cols):
        if cols[i] < 0:
            i += 1; continue
        r = cols[i]; s = i
        while i < len(cols) and cols[i] == r:
            i += 1
        spans[pi][li] += [base + s, base + i, r]

i = j = 0; skipped = 0
while i < len(tw) and j < len(mw):
    (sw, sc), (pi, li, pos, w) = tw[i], mw[j]
    if L(sw) == L(w) or L(sw).replace('ء', '') == L(w).replace('ء', ''):
        emit(pi, li, pos, project(sw, sc, w)); i += 1; j += 1
    elif j + 1 < len(mw) and L(sw) == L(w) + L(mw[j + 1][3]):
        n = len(w)
        emit(pi, li, pos, project(sw[:n], sc[:n], w))
        p2 = mw[j + 1]
        emit(p2[0], p2[1], p2[2], project(sw[n:], sc[n:], p2[3]))
        i += 1; j += 2
    elif i + 1 < len(tw) and L(sw) + L(tw[i + 1][0]) == L(w):
        sw2 = sw + tw[i + 1][0]; sc2 = sc + tw[i + 1][1]
        emit(pi, li, pos, project(sw2, sc2, w)); i += 2; j += 1
    else:
        emit(pi, li, pos, project(sw, sc, w)); i += 1; j += 1; skipped += 1
print('mos kelmagan so\'zlar:', skipped, file=sys.stderr)
json.dump({"src": "Tajvid: cpfair/quran-tajweed (CC BY 4.0) — mushaf qatorlariga moslangan", "p": spans},
          open(A + 'mushaf_tajweed.json', 'w'), ensure_ascii=False, separators=(',', ':'))
