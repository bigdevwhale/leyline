"""Builds app/src/demo/assets/demo.json: a traveller some weeks into the game in central Moscow, for screenshots.

Times are written relative to BASE; the app shifts them to «now» when it seeds the demo (data/Demo.kt).
The quarters come from tools/demo_places.json (an Overpass dump of Moscow's place nodes).

    python tools/demo.py
"""
import json
import math
import random
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
PLACES = ROOT / "tools" / "demo_places.json"
OUT = ROOT / "app" / "src" / "demo" / "assets" / "demo.json"
BASE = 1_800_000_000_000
DAY = 86_400_000
HOUR = 3_600_000
EARTH = 6_371_000.0
CELL = 250.0

KNOWN = {
    "2330497992": ("water", "homes"),     # Пресненский – home
    "2330491203": ("old", "bazaar"),      # Арбат
    "2330503343": ("garden", "old"),      # Хамовники
    "2330493066": ("old", "water"),       # Замоскворечье
    "2330505264": ("water", "old"),       # Якиманка
    "2330498613": ("bazaar", "old"),      # Тверской
    "2330496472": ("bazaar", "scholar"),  # Мещанский
    "2330492561": ("gate", "old"),        # Басманный
    "2331500885": ("gate", "bazaar"),     # Красносельский
    "2330643830": ("old", "bazaar"),      # Китай-город
    "2330514392": ("garden", "homes"),    # Беговой
    "3783340452": ("gate", "water"),      # Дорогомилово
    "2331513033": ("garden", "water"),    # Лужники
    "2330679958": ("scholar", "garden"),  # Воробьёвы Горы
    "2330537356": ("forge", "gate"),      # Савёловский
}
HOME = "2330497992"
WORK = "2330496472"
ING = {"garden": "resin", "water": "mist", "old": "dust", "scholar": "ink", "forge": "spark", "bazaar": "copper", "gate": "feather", "homes": "wax"}


def local(lat0, lon0, lat, lon):
    return ((lon - lon0) * math.pi / 180 * EARTH * math.cos(lat0 * math.pi / 180), (lat - lat0) * math.pi / 180 * EARTH)


def dist(a, b):
    return math.hypot((a["lat"] - b["lat"]) * 111_200, (a["lon"] - b["lon"]) * 111_200 * math.cos(math.radians(a["lat"])))


def main():
    els = json.loads(PLACES.read_text(encoding="utf-8"))
    order = {"suburb": 0, "quarter": 1, "neighbourhood": 2}
    places = []
    for e in sorted(els, key=lambda e: order.get(e["tags"].get("place"), 3)):
        t = e["tags"]
        q = {"id": str(e["id"]), "ru": t.get("name:ru") or t["name"], "en": t.get("name:en") or t["name"], "lat": e["lat"], "lon": e["lon"]}
        if any(dist(q, p) < 600 for p in places) and q["id"] not in KNOWN:
            continue
        places.append(q)
    byid = {p["id"]: p for p in places}
    home = byid[HOME]
    lat0, lon0 = home["lat"], home["lon"]
    r = random.Random(11)

    def cell(lat, lon):
        x, y = local(lat0, lon0, lat, lon)
        return (math.floor(x / CELL), math.floor(y / CELL))

    def walk(a, b):
        n = max(abs(b[0] - a[0]), abs(b[1] - a[1]))
        if n == 0:
            return [a]
        out = []
        for i in range(n + 1):
            c = (a[0] + round((b[0] - a[0]) * i / n), a[1] + round((b[1] - a[1]) * i / n))
            if c not in out:
                out.append(c)
        return out

    def key(c):
        return f"{c[0]},{c[1]}"

    def edge(a, b):
        ka, kb = key(a), key(b)
        return f"{ka}|{kb}" if ka < kb else f"{kb}|{ka}"

    def near(lat, lon):
        return min((p for p in places if p["id"] in KNOWN), key=lambda p: dist(p, {"lat": lat, "lon": lon}))

    cells, ley, deep = {}, {}, {}
    trips = []
    # routes: (from, via, to, ride, times) – the commute is ridden most, so its thread grows thick
    routes = [
        (HOME, "2330498613", WORK, "car", 14), (HOME, "2330491203", "2330503343", "car", 6),
        (HOME, "3783340452", "2331513033", "bus", 5), (WORK, "2330643830", "2330492561", "metro", 5),
        (HOME, "2330514392", "2330537356", "car", 4), ("2330503343", "2330505264", "2330493066", "tram", 4),
        (WORK, "2331500885", "2330492561", "bus", 3), ("2330491203", "2330679958", "2331513033", "car", 2),
    ]
    t_end = BASE - 3 * HOUR
    weathers = ["clear", "clouds", "rain", "clear", "snow", "fog", "clouds", "clear"]
    met_order = ["water", "old", "bazaar", "garden", "gate", "scholar"]
    rides = [(a, v, b, ride) for a, v, b, ride, n in routes for _ in range(n)]
    r.shuffle(rides)
    rides = rides[:43]
    seen_q = set()
    for i, (a, v, b, ride) in enumerate(rides):
        if r.random() < 0.5:
            a, b = b, a
        pa, pv, pb = byid[a], byid[v], byid[b]
        jit = lambda p: (p["lat"] + r.uniform(-0.002, 0.002), p["lon"] + r.uniform(-0.003, 0.003))
        stops = [jit(pa), jit(pv), jit(pb)]
        pts = []
        for (la1, lo1), (la2, lo2) in zip(stops, stops[1:]):
            for k in range(12):
                f = k / 12
                pts.append((round(la1 + (la2 - la1) * f + r.uniform(-0.0006, 0.0006), 5), round(lo1 + (lo2 - lo1) * f + r.uniform(-0.0009, 0.0009), 5)))
        pts.append(stops[-1])
        meters = sum(dist({"lat": x1, "lon": y1}, {"lat": x2, "lon": y2}) for (x1, y1), (x2, y2) in zip(pts, pts[1:])) * 1.25
        cs = []
        for (la, lo) in pts:
            c = cell(la, lo)
            for w in walk(cs[-1], c) if cs else [c]:
                if not cs or w != cs[-1]:
                    cs.append(w)
        new = sum(1 for c in cs if key(c) not in cells)
        for c in cs:
            cells[key(c)] = cells.get(key(c), 0) + 1
        target = deep if ride == "metro" else ley
        for c1, c2 in zip(cs, cs[1:]):
            target[edge(c1, c2)] = target.get(edge(c1, c2), 0) + 1
        qs = []
        for (la, lo) in pts[::4] + [pts[-1]]:
            q = near(la, lo)["id"]
            if q not in qs:
                qs.append(q)
        fresh = [q for q in qs if q not in seen_q]
        seen_q.update(qs)
        dur = int(meters / (9 if ride == "car" else 6) * 1000)
        start = t_end - dur
        hour = (start // HOUR) % 24
        night = i % 6 == 2
        wx = weathers[i % len(weathers)]
        gains = {"wind": 1 + int(meters / 4000)}
        gains["stone"] = 1 if ride in ("bus", "tram") else 0
        if night:
            gains["moon"] = 2
        if wx == "rain":
            gains["storm"] = 1
        if wx == "snow":
            gains["frost"] = 1
        if wx == "clear" and not night:
            gains["sun"] = 1
        if ride == "metro":
            gains["ore"] = 2
        for q in qs[:3]:
            ing = ING[KNOWN[q][0]]
            gains[ing] = gains.get(ing, 0) + 1
        if i % 9 == 4:
            gains["shard"] = 1
        if i % 7 == 1:
            gains["dawn"] = 1
        gains = {k: v for k, v in gains.items() if v > 0}
        met = [met_order[len([t for t in trips if t["met"]])]] if i in (40, 34, 27, 19, 11, 5) and len([t for t in trips if t["met"]]) < len(met_order) else []
        notes = []
        if i == 0:
            notes = ["Над крышами мелькнул блуждающий огонёк – и оставил искру|A will-o'-the-wisp flickered over the roofs and left a spark"]
        if i == 8:
            notes = ["Вы пересекли реку – туман сам лёг в котомку|You crossed the river and the mist settled into your bag"]
        power = int(meters / 100) + 10 * len(fresh)
        trips.append({
            "start": start, "end": t_end, "ride": ride, "meters": round(meters, 1), "fast": round(meters * 0.3, 1) if ride == "car" else 0.0,
            "jam": r.choice([0, 0, 120, 300, 540]) if ride in ("car", "bus") else 0, "jamWait": 0, "under": 4 if ride == "metro" else 0,
            "weather": wx, "temp": round(r.uniform(-4, 6), 1),
            "fLat": pts[0][0], "fLon": pts[0][1], "lLat": pts[-1][0], "lLon": pts[-1][1], "lAt": t_end,
            "pts": [x for p in pts for x in p] if i < 6 else [],
            "quarters": qs, "fresh": fresh, "newCells": new, "knots": 0,
            "night": night, "twilight": False, "full": False, "gains": gains, "power": power, "xp": round(meters / 1000, 2),
            "osm": 1, "found": [], "met": met, "notes": notes, "harvest": False, "seeker": False, "endS": 0, "startS": 0,
        })
        t_end -= int(r.uniform(0.6, 1.4) * DAY)

    quarters = []
    for p in places:
        if dist(p, home) > 9000:
            continue
        q = {"id": p["id"], "ru": p["ru"], "en": p["en"], "lat": p["lat"], "lon": p["lon"], "kind": "", "second": "", "visits": 0, "firstDay": 0, "guessed": False}
        if p["id"] in KNOWN:
            k, s = KNOWN[p["id"]]
            q.update(kind=k, second=s, visits=sum(1 for t in trips if p["id"] in t["quarters"]), firstDay=(BASE - 40 * DAY) // DAY)
        quarters.append(q)

    sanct = []
    for sid, (pid, name, ward, lvl, visits) in enumerate([(HOME, "Дом", "w_grove", 2, 24), (WORK, "Работа", "w_lantern", 1, 15), ("2330503343", "", "", 0, 4)], start=1):
        p = byid[pid]
        sanct.append({"id": sid, "lat": p["lat"] + 0.0011, "lon": p["lon"] - 0.0015, "visits": visits, "name": name, "ward": ward, "wardLevel": lvl, "since": BASE - (40 - sid * 6) * DAY})

    car_km = sum(t["meters"] for t in trips if t["ride"] == "car") / 1000
    def mount(beast, ride, stage, name="", extra=0.0):
        ts = [t for t in trips if t["ride"] == ride]
        km = sum(t["meters"] for t in ts) / 1000 + extra
        return {"beast": beast, "km": round(km, 1), "stage": stage, "rides": len(ts), "nightKm": round(sum(t["meters"] for t in ts if t["night"]) / 1000, 1),
                "wet": sum(1 for t in ts if t["weather"] in ("rain", "snow")), "fastKm": round(sum(t["fast"] for t in ts) / 1000, 1),
                "jamMin": sum(t["jam"] for t in ts) // 60, "rivers": 3, "newCells": sum(t["newCells"] for t in ts),
                "born": BASE - 42 * DAY, "last": max((t["end"] for t in ts), default=BASE - DAY), "name": name}

    state = {
        "v": 1, "base": BASE, "onboarded": True, "homeLat": lat0, "homeLon": lon0,
        "fixLat": byid["2330491203"]["lat"] + 0.001, "fixLon": byid["2330491203"]["lon"] + 0.002,
        "quarters": quarters, "here": "2330491203", "power": 1460,
        "bag": {"wind": 9, "stone": 5, "mist": 4, "resin": 3, "moon": 4, "dawn": 2, "storm": 2, "frost": 1, "sun": 3, "ore": 3,
                "wax": 2, "dust": 5, "ink": 2, "spark": 3, "feather": 3, "lily": 1, "copper": 4, "shard": 1},
        "seen": ["wind", "stone", "mist", "resin", "moon", "dawn", "storm", "frost", "sun", "ore", "wax", "dust", "ink", "spark", "feather", "lily", "copper", "shard"],
        "known": ["tailwind", "harvest", "seeker", "longroad", "awaken1", "awaken2", "w_grove", "w_well", "w_lantern", "g_water", "g_old", "g_bazaar", "g_garden"],
        "stock": {"tailwind": 1, "seeker": 1},
        "brewed": {"tailwind": 4, "harvest": 2, "seeker": 2, "longroad": 1, "awaken1": 3, "awaken2": 1, "w_grove": 2, "w_lantern": 1, "g_water": 1, "g_old": 1, "g_bazaar": 1},
        "buffs": {"longroad": 2},
        "mounts": [mount("car", "car", 2), mount("bus", "bus", 1), mount("tram", "tram", 0), mount("metro", "metro", 0)],
        "beast": "car", "cells": cells, "ley": ley, "deep": deep, "sanctums": sanct, "trips": trips, "trip": None,
        "spirits": {"water": 4, "old": 4, "bazaar": 3, "garden": 2, "gate": 1, "scholar": 0},
        "spiritCount": {"water": 9, "old": 7, "bazaar": 5, "garden": 3, "gate": 2, "scholar": 1},
        "spiritSince": {k: BASE - (30 - i * 4) * DAY for i, k in enumerate(["water", "old", "bazaar", "garden", "gate", "scholar"])},
        "told": ["i.trip", "i.spirit", "i.sanctum", "i.rides", "i.ontrip", "i.tower", "i.map", "i.lab", "i.stable"],
        "theme": "", "voice": False, "ride": "car", "nextId": 4, "btAddr": "", "btName": "", "reach": 9000, "placesV": 2,
        "aurora": False, "mirror": False, "great": False, "lastCell": "",
        "tried": ["wind+stone", "moon+sun", "dust+mist+stone"], "decor": ["lamp", "books", "basket", "mushrooms", "garland"], "bloomDay": 0,
    }
    OUT.parent.mkdir(parents=True, exist_ok=True)
    OUT.write_text(json.dumps(state, ensure_ascii=False), encoding="utf-8")
    print(OUT, len(trips), "trips", len(quarters), "quarters", len(cells), "cells", len(ley), "threads", round(car_km), "car km")


if __name__ == "__main__":
    main()
