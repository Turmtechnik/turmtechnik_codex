-- Turmtechnik Android – SQLite-Schema (Export aus PlatinenDatabaseHelper.java)
-- Verwendung: Auf dem Router (wenn SQLite verfügbar) oder PC: sqlite3 turmtechnik_config.db < schema-android.sql
-- Datenbankversion im Android-Projekt: 23

CREATE TABLE platinen_config (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  platine_nummer INTEGER NOT NULL UNIQUE,
  ip_adresse TEXT,
  port INTEGER,
  aktiv BOOLEAN DEFAULT 0,
  mac_adresse TEXT,
  online BOOLEAN DEFAULT 0,
  relais_anzahl INTEGER DEFAULT 32,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE io_config (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  key TEXT NOT NULL UNIQUE,
  value TEXT,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE nebenuhr_config (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  uhr_name TEXT NOT NULL,
  zeile_index INTEGER NOT NULL,
  relais_a INTEGER DEFAULT 0,
  relais_b INTEGER DEFAULT 0,
  impuls_dauer_1 INTEGER DEFAULT 100,
  impuls_dauer_2 INTEGER DEFAULT 50,
  uhr_name_display TEXT,
  modus TEXT DEFAULT '12',
  angezeigte_zeit INTEGER DEFAULT 0,
  mondphase_ist INTEGER DEFAULT 0,
  last_relais_a INTEGER DEFAULT 0,
  aktiv INTEGER DEFAULT 1,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  UNIQUE(uhr_name, zeile_index)
);

CREATE TABLE tagtypen (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  name TEXT NOT NULL UNIQUE,
  programm_typ TEXT NOT NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE programme (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  tagtyp_id INTEGER NOT NULL,
  zeile_index INTEGER NOT NULL,
  startzeit TEXT NOT NULL,
  funktion TEXT NOT NULL,
  melodie_name TEXT,
  dauer_heizung TEXT,
  montag BOOLEAN DEFAULT 0,
  dienstag BOOLEAN DEFAULT 0,
  mittwoch BOOLEAN DEFAULT 0,
  donnerstag BOOLEAN DEFAULT 0,
  freitag BOOLEAN DEFAULT 0,
  samstag BOOLEAN DEFAULT 0,
  sonntag BOOLEAN DEFAULT 0,
  immer BOOLEAN DEFAULT 0,
  periodisch INTEGER DEFAULT 0,
  start_datum TEXT,
  ende_datum TEXT,
  verknuepfte_taste TEXT,
  prioritaet INTEGER DEFAULT 0,
  festtag_datum TEXT,
  festtag_jahr INTEGER,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (tagtyp_id) REFERENCES tagtypen(id) ON DELETE CASCADE,
  UNIQUE(tagtyp_id, zeile_index)
);

CREATE TABLE osterfeiertage (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  name TEXT NOT NULL UNIQUE,
  oster_offset INTEGER NOT NULL,
  tagtyp_name TEXT,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE feste_feiertage (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  name TEXT NOT NULL,
  datum TEXT NOT NULL,
  tagtyp_name TEXT,
  verschieben_auf_sonntag BOOLEAN DEFAULT 0,
  sonntag_im_monat INTEGER DEFAULT 0,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  UNIQUE(name, datum)
);

CREATE TABLE sonderfeiertage (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  feiertag_id INTEGER NOT NULL,
  jahr INTEGER NOT NULL,
  berechnetes_datum TEXT NOT NULL,
  tagtyp_name TEXT,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (feiertag_id) REFERENCES feste_feiertage(id) ON DELETE CASCADE,
  UNIQUE(feiertag_id, jahr)
);

CREATE TABLE vorschwingen_config (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  vorschwingen_relais INTEGER NOT NULL,
  laeuten_relais INTEGER NOT NULL,
  zeit_sekunden INTEGER NOT NULL,
  vorschwingen_platine INTEGER NOT NULL,
  laeuten_platine INTEGER NOT NULL,
  vorschwingen_relais_berechnet INTEGER,
  laeuten_relais_berechnet INTEGER,
  aktiv INTEGER DEFAULT 1,
  sortierung INTEGER DEFAULT 0,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE midi_config (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  dateiname TEXT NOT NULL UNIQUE,
  abspielgeschwindigkeit REAL DEFAULT 1.0,
  aktiv INTEGER DEFAULT 1,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE midi_noten_relais (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  note INTEGER NOT NULL UNIQUE,
  relais_nummer INTEGER NOT NULL,
  impuls_laenge_ms INTEGER DEFAULT 100,
  aktiv INTEGER DEFAULT 1,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE melodien (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  name TEXT NOT NULL UNIQUE,
  vorlauf_minuten INTEGER DEFAULT 0,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE melodie_zeilen (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  melodie_id INTEGER NOT NULL,
  zeile_index INTEGER NOT NULL,
  beginn_zeit TEXT,
  dauer_sekunden REAL,
  zeiteinheit TEXT DEFAULT 'Sec',
  kloeppel_a TEXT, kloeppel_b TEXT, kloeppel_c TEXT, kloeppel_d TEXT,
  kloeppel_e TEXT, kloeppel_f TEXT, kloeppel_g TEXT, kloeppel_h TEXT,
  kloeppel_i TEXT, kloeppel_j TEXT, kloeppel_k TEXT, kloeppel_l TEXT,
  kloeppel_m TEXT, kloeppel_n TEXT, kloeppel_o TEXT, kloeppel_p TEXT,
  sound_mp3 TEXT,
  melodie_xls TEXT,
  FOREIGN KEY (melodie_id) REFERENCES melodien(id) ON DELETE CASCADE,
  UNIQUE(melodie_id, zeile_index)
);

CREATE TABLE beschriftung_tasten (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  zeile_index INTEGER NOT NULL,
  c1 TEXT, c2 TEXT, c3 TEXT, c4 TEXT, c5 TEXT, c6 TEXT, c7 TEXT, c8 TEXT, c9 TEXT, c10 TEXT, c13 TEXT,
  sonder_id INTEGER,
  sound TEXT,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE schlagwerk_config (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  typ INTEGER NOT NULL UNIQUE,
  start_zeit TEXT,
  ende_zeit TEXT,
  hammer_stunden_relais INTEGER DEFAULT 0,
  hammer_viertel_relais INTEGER DEFAULT 0,
  pause_viertel_ms INTEGER DEFAULT 0,
  pause_viertel_zu_stunde_ms INTEGER DEFAULT 0,
  halbstunde_schlaganzahl INTEGER DEFAULT 1,
  volle_stunde_anzahl INTEGER DEFAULT 12,
  schlagen_wenn_melodie_laeuft INTEGER DEFAULT 0,
  kein_schlagwerk_ostern INTEGER DEFAULT 0,
  kein_schlagwerk_karfreitag INTEGER DEFAULT 0,
  kein_schlagwerk_karsamstag INTEGER DEFAULT 0,
  aktiv INTEGER DEFAULT 1,
  sortierung INTEGER DEFAULT 0,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Indizes
CREATE INDEX IF NOT EXISTS idx_platinen_nummer ON platinen_config(platine_nummer);
CREATE INDEX IF NOT EXISTS idx_nebenuhr_uhr_name ON nebenuhr_config(uhr_name);
CREATE INDEX IF NOT EXISTS idx_tagtypen_name ON tagtypen(name);
CREATE INDEX IF NOT EXISTS idx_programme_tagtyp ON programme(tagtyp_id);
CREATE INDEX IF NOT EXISTS idx_programme_startzeit ON programme(startzeit);
CREATE INDEX IF NOT EXISTS idx_osterfeiertage_name ON osterfeiertage(name);
CREATE INDEX IF NOT EXISTS idx_feste_feiertage_datum ON feste_feiertage(datum);
CREATE INDEX IF NOT EXISTS idx_sonderfeiertage_datum ON sonderfeiertage(berechnetes_datum, jahr);
CREATE INDEX IF NOT EXISTS idx_vorschwingen_aktiv ON vorschwingen_config(aktiv);
CREATE INDEX IF NOT EXISTS idx_midi_config_dateiname ON midi_config(dateiname);
CREATE INDEX IF NOT EXISTS idx_midi_noten_relais_note ON midi_noten_relais(note);
CREATE INDEX IF NOT EXISTS idx_melodien_name ON melodien(name);
CREATE INDEX IF NOT EXISTS idx_melodie_zeilen_melodie ON melodie_zeilen(melodie_id);
CREATE INDEX IF NOT EXISTS idx_beschriftung_tasten_zeile ON beschriftung_tasten(zeile_index);
CREATE INDEX IF NOT EXISTS idx_schlagwerk_config_typ ON schlagwerk_config(typ);
