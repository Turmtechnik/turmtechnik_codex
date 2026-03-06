# -*- coding: utf-8 -*-
import sqlite3
import os
db_path = r"c:\Users\ThinkStation P920\Downloads\turmtechnik_config_9972_Virgen_2026-02-08 (2).db"
if not os.path.exists(db_path):
    print("DB nicht gefunden:", db_path)
    exit(1)
db = sqlite3.connect(db_path)
cur = db.cursor()
cur.execute("SELECT name FROM sqlite_master WHERE type='table' ORDER BY name")
print("Tabellen:", [r[0] for r in cur.fetchall()])
print()
cur.execute("SELECT id, name, tagtyp_name, oster_offset FROM osterfeiertage ORDER BY oster_offset")
rows = cur.fetchall()
print("osterfeiertage:")
for r in rows:
    print(" ", r)
print()
cur.execute("SELECT id, name, datum, tagtyp_name, verschieben_auf_sonntag FROM feste_feiertage ORDER BY datum LIMIT 40")
rows2 = cur.fetchall()
print("feste_feiertage (Auszug):")
for r in rows2:
    print(" ", r)
print()
cur.execute("SELECT id, feiertag_id, jahr, berechnetes_datum, tagtyp_name FROM sonderfeiertage WHERE jahr=2026 ORDER BY berechnetes_datum")
rows3 = cur.fetchall()
print("sonderfeiertage 2026:")
for r in rows3:
    print(" ", r)
db.close()
