package tom.turmtechnik;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.Cursor;
import android.util.Log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * SQLite-Helper für Platinen-Konfiguration.
 */
public class PlatinenDatabaseHelper extends SQLiteOpenHelper {
    private static final String TAG = "PlatinenDatabaseHelper";
    private static final String DATABASE_NAME = "turmtechnik_config.db";
    private static final int DATABASE_VERSION = 30; // 30: pending_relais_a fuer Nebenuhr-Neustart-Replay
    
    private static final String TABLE_PLATINEN = "platinen_config";
    private static final String TABLE_IO_CONFIG = "io_config";
    private static final String TABLE_NEBENUHR_CONFIG = "nebenuhr_config";
    private static final String TABLE_TAGTYPEN = "tagtypen";
    private static final String TABLE_PROGRAMME = "programme";
    private static final String TABLE_OSTERFEIERTAGE = "osterfeiertage";
    private static final String TABLE_FESTE_FEIERTAGE = "feste_feiertage";
    private static final String TABLE_SONDERFEIERTAGE = "sonderfeiertage";
    private static final String TABLE_VORSCHWINGEN_CONFIG = "vorschwingen_config";
    private static final String TABLE_MIDI_CONFIG = "midi_config";
    private static final String TABLE_MIDI_NOTEN_RELAIS = "midi_noten_relais";
    private static final String TABLE_MELODIEN = "melodien";
    private static final String TABLE_MELODIE_ZEILEN = "melodie_zeilen";
    private static final String TABLE_BESCHRIFTUNG_TASTEN = "beschriftung_tasten";
    private static final String TABLE_SCHLAGWERK_CONFIG = "schlagwerk_config";
    private static final String TABLE_RELAIS_KATEGORIEN = "relais_kategorien";
    private static final String TABLE_RELAIS_ZUORDNUNG = "relais_zuordnung";
    private static final String TABLE_UI_FUNKTION = "ui_funktion";
    private static final String TABLE_LAYOUT_ITEM = "layout_item";
    
    private static final String CREATE_TABLE_PLATINEN = 
        "CREATE TABLE " + TABLE_PLATINEN + " (" +
        "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
        "platine_nummer INTEGER NOT NULL UNIQUE, " +
        "ip_adresse TEXT, " +
        "port INTEGER, " +
        "aktiv BOOLEAN DEFAULT 0, " +
        "mac_adresse TEXT, " +
        "online BOOLEAN DEFAULT 0, " +
        "relais_anzahl INTEGER DEFAULT 32, " +
        "impuls_antwort_erwartet TEXT, " +
        "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
        "updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
        ")";
    
    private static final String CREATE_TABLE_IO_CONFIG = 
        "CREATE TABLE " + TABLE_IO_CONFIG + " (" +
        "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
        "key TEXT NOT NULL UNIQUE, " +
        "value TEXT, " +
        "updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
        ")";
    
    private static final String CREATE_TABLE_NEBENUHR_CONFIG = 
        "CREATE TABLE " + TABLE_NEBENUHR_CONFIG + " (" +
        "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
        "uhr_name TEXT NOT NULL, " +                    // "A", "B", "C", "D"
        "zeile_index INTEGER NOT NULL, " +              // Excel-Zeile (3, 4, 5, 6)
        "relais_a INTEGER DEFAULT 0, " +                // Relais A Nummer (für Wechselschaltung)
        "relais_b INTEGER DEFAULT 0, " +                // Relais B Nummer (für Wechselschaltung)
        "impuls_dauer_1 INTEGER DEFAULT 100, " +        // Impulsdauer 1 (Millisekunden)
        "impuls_dauer_2 INTEGER DEFAULT 50, " +         // Impulsdauer 2 / Pause (Millisekunden)
        "uhr_name_display TEXT, " +                      // Anzeige-Name
        "modus TEXT DEFAULT '12', " +                    // "12" oder "MOND"
        "angezeigte_zeit INTEGER DEFAULT 0, " +          // Aktuelle angezeigte Zeit (12h-Minuten, 0-719)
        "mondphase_ist INTEGER DEFAULT 0, " +            // Aktueller Impulswert im Mondzyklus für Monduhr D
        "last_relais_a INTEGER DEFAULT 0, " +            // Letztes verwendetes Relais (1=A, 0=B) für Wechselschaltung
        "aktiv INTEGER DEFAULT 1, " +                    // Aktiv-Status (1=aktiv, 0=inaktiv)
        "impuls_ausstehend INTEGER DEFAULT 0, " +         // 1 = letzter Impuls ist offen und wird nach Neustart mit gleichem Relais wiederholt
        "pending_relais_a INTEGER DEFAULT NULL, " +       // NULL = kein offener Impuls, 1 = Relais A wiederholen, 0 = Relais B wiederholen
        "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
        "updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
        "UNIQUE(uhr_name, zeile_index)" +
        ")";
    
    private static final String CREATE_TABLE_TAGTYPEN = 
        "CREATE TABLE " + TABLE_TAGTYPEN + " (" +
        "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
        "name TEXT NOT NULL UNIQUE, " +                    // Dateiname (z.B. "Weihnachten.xls")
        "programm_typ TEXT NOT NULL, " +                  // "normal", "festtag_variabel", "festtag_fest", "benutzer"
        "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
        "updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
        ")";
    
    private static final String CREATE_TABLE_PROGRAMME = 
        "CREATE TABLE " + TABLE_PROGRAMME + " (" +
        "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
        "tagtyp_id INTEGER NOT NULL, " +                  // Foreign Key zu tagtypen
        "zeile_index INTEGER NOT NULL, " +               // Original Excel-Zeile (0-basiert)
        "startzeit TEXT NOT NULL, " +                     // "HH:MM:SS" oder "SA"/"SU"
        "funktion TEXT NOT NULL, " +                      // "Melodie" oder "Heizung"
        "melodie_name TEXT, " +                           // NULL wenn Heizung
        "dauer_heizung TEXT, " +                          // "hh:mm:ss" Format, NULL wenn Melodie
        "montag BOOLEAN DEFAULT 0, " +
        "dienstag BOOLEAN DEFAULT 0, " +
        "mittwoch BOOLEAN DEFAULT 0, " +
        "donnerstag BOOLEAN DEFAULT 0, " +
        "freitag BOOLEAN DEFAULT 0, " +
        "samstag BOOLEAN DEFAULT 0, " +
        "sonntag BOOLEAN DEFAULT 0, " +
        "immer BOOLEAN DEFAULT 0, " +                    // "1" → true
        "periodisch INTEGER DEFAULT 0, " +               // 0=immer, 1=Sommer, 2=Winter
        "start_datum TEXT, " +                            // "TT.MM" oder "TT.MM.JJJJ.A" oder "TT.T"
        "ende_datum TEXT, " +                             // "TT.MM" oder "TT.MM.JJJJ.A" oder "TT.T"
        "verknuepfte_taste TEXT, " +                      // NULL wenn keine
        "prioritaet INTEGER DEFAULT 0, " +               // 1-9
        "festtag_datum TEXT, " +                          // Für Feiertage: "TT.MM" oder "TT.MM.JJJJ"
        "festtag_jahr INTEGER, " +                        // Für variable Feiertage
        "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
        "updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
        "FOREIGN KEY (tagtyp_id) REFERENCES " + TABLE_TAGTYPEN + "(id) ON DELETE CASCADE, " +
        "UNIQUE(tagtyp_id, zeile_index)" +
        ")";
    
    private static final String CREATE_TABLE_OSTERFEIERTAGE = 
        "CREATE TABLE " + TABLE_OSTERFEIERTAGE + " (" +
        "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
        "name TEXT NOT NULL UNIQUE, " +                    // Feiertagsname (z.B. "Aschermittwoch")
        "oster_offset INTEGER NOT NULL, " +                // Tage +/- zum Osterdatum (z.B. -46, -7, 0, +1)
        "tagtyp_name TEXT, " +                             // Tagtyp-Dateiname (z.B. "Karfreitag.xls")
        "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
        "updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
        ")";
    
    private static final String CREATE_TABLE_FESTE_FEIERTAGE = 
        "CREATE TABLE " + TABLE_FESTE_FEIERTAGE + " (" +
        "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
        "name TEXT NOT NULL, " +                           // Bezeichnung (z.B. "Weihnachten")
        "datum TEXT NOT NULL, " +                           // Fixes Datum im Format "TT.MM" (z.B. "25.12")
        "tagtyp_name TEXT, " +                              // Tagtyp-Dateiname (z.B. "Weihnachten.xls")
        "verschieben_auf_sonntag BOOLEAN DEFAULT 0, " +      // Wenn true: Verschiebung auf Sonntag aktiviert
        "sonntag_im_monat INTEGER DEFAULT 0, " +             // 0 = nächster Sonntag, 1-4 = 1.-4. Sonntag im Monat
        "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
        "updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
        "UNIQUE(name, datum)" +
        ")";
    
    private static final String CREATE_TABLE_SONDERFEIERTAGE = 
        "CREATE TABLE " + TABLE_SONDERFEIERTAGE + " (" +
        "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
        "feiertag_id INTEGER NOT NULL, " +              // Foreign Key zu feste_feiertage
        "jahr INTEGER NOT NULL, " +                      // Jahr (z.B. 2026)
        "berechnetes_datum TEXT NOT NULL, " +            // Berechnetes Datum im Format "TT.MM" (z.B. "18.1")
        "tagtyp_name TEXT, " +                           // Tagtyp-Dateiname (z.B. "Feiertag.xls")
        "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
        "updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
        "FOREIGN KEY (feiertag_id) REFERENCES " + TABLE_FESTE_FEIERTAGE + "(id) ON DELETE CASCADE, " +
        "UNIQUE(feiertag_id, jahr)" +
        ")";
    
    private static final String CREATE_TABLE_VORSCHWINGEN_CONFIG = 
        "CREATE TABLE " + TABLE_VORSCHWINGEN_CONFIG + " (" +
        "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
        "vorschwingen_relais INTEGER NOT NULL, " +          // Original Relais-Nummer (1-32)
        "laeuten_relais INTEGER NOT NULL, " +               // Original Kloeppel-Relais-Nummer (1-32)
        "zeit_sekunden INTEGER NOT NULL, " +                // Vorschwingzeit in Sekunden
        "vorschwingen_platine INTEGER NOT NULL, " +         // Platine-Nummer (1-3)
        "laeuten_platine INTEGER NOT NULL, " +              // Platine-Nummer für Kloeppel (1-3)
        "vorschwingen_relais_berechnet INTEGER, " +         // Berechnete Relais-Nummer (für Kompatibilität)
        "laeuten_relais_berechnet INTEGER, " +              // Berechnete Kloeppel-Relais-Nummer (für Kompatibilität)
        "aktiv INTEGER DEFAULT 1, " +                       // Aktiv-Status
        "sortierung INTEGER DEFAULT 0, " +                  // Sortierreihenfolge
        "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
        "updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
        ")";
    
    private static final String CREATE_TABLE_MIDI_CONFIG = 
        "CREATE TABLE " + TABLE_MIDI_CONFIG + " (" +
        "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
        "dateiname TEXT NOT NULL UNIQUE, " +
        "abspielgeschwindigkeit REAL DEFAULT 1.0, " +
        "aktiv INTEGER DEFAULT 1, " +
        "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
        "updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
        ")";
    
    private static final String CREATE_TABLE_MIDI_NOTEN_RELAIS = 
        "CREATE TABLE " + TABLE_MIDI_NOTEN_RELAIS + " (" +
        "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
        "note INTEGER NOT NULL UNIQUE, " +
        "relais_nummer INTEGER NOT NULL, " +
        "impuls_laenge_ms INTEGER DEFAULT 100, " +
        "aktiv INTEGER DEFAULT 1, " +
        "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
        "updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
        ")";
    
    private static final String CREATE_TABLE_MELODIEN = 
        "CREATE TABLE " + TABLE_MELODIEN + " (" +
        "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
        "name TEXT NOT NULL UNIQUE, " +
        "vorlauf_minuten INTEGER DEFAULT 0, " +
        "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
        "updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
        ")";
    
    private static final String CREATE_TABLE_MELODIE_ZEILEN = 
        "CREATE TABLE " + TABLE_MELODIE_ZEILEN + " (" +
        "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
        "melodie_id INTEGER NOT NULL, " +
        "zeile_index INTEGER NOT NULL, " +
        "beginn_zeit TEXT, " +
        "dauer_sekunden REAL, " +
        "zeiteinheit TEXT DEFAULT 'Sec', " +
        "kloeppel_a TEXT, " +
        "kloeppel_b TEXT, " +
        "kloeppel_c TEXT, " +
        "kloeppel_d TEXT, " +
        "kloeppel_e TEXT, " +
        "kloeppel_f TEXT, " +
        "kloeppel_g TEXT, " +
        "kloeppel_h TEXT, " +
        "kloeppel_i TEXT, " +
        "kloeppel_j TEXT, " +
        "kloeppel_k TEXT, " +
        "kloeppel_l TEXT, " +
        "kloeppel_m TEXT, " +
        "kloeppel_n TEXT, " +
        "kloeppel_o TEXT, " +
        "kloeppel_p TEXT, " +
        "sound_mp3 TEXT, " +
        "melodie_xls TEXT, " +
        "FOREIGN KEY (melodie_id) REFERENCES " + TABLE_MELODIEN + "(id) ON DELETE CASCADE, " +
        "UNIQUE(melodie_id, zeile_index)" +
        ")";
    
    private static final String CREATE_TABLE_BESCHRIFTUNG_TASTEN =
        "CREATE TABLE " + TABLE_BESCHRIFTUNG_TASTEN + " (" +
        "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
        "zeile_index INTEGER NOT NULL, " +
        "c1 TEXT, " +
        "c2 TEXT, " +
        "c3 TEXT, " +
        "c4 TEXT, " +
        "c5 TEXT, " +
        "c6 TEXT, " +
        "c7 TEXT, " +
        "c8 TEXT, " +
        "c9 TEXT, " +
        "c10 TEXT, " +
        "c13 TEXT, " +
        "sonder_id INTEGER, " +
        "sound TEXT, " +
        "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
        ")";
    
    private static final String CREATE_TABLE_SCHLAGWERK_CONFIG =
        "CREATE TABLE " + TABLE_SCHLAGWERK_CONFIG + " (" +
        "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
        "typ INTEGER NOT NULL UNIQUE, " +
        "start_zeit TEXT, " +
        "ende_zeit TEXT, " +
        "hammer_stunden_relais INTEGER DEFAULT 0, " +
        "hammer_viertel_relais INTEGER DEFAULT 0, " +
        "pause_viertel_ms INTEGER DEFAULT 0, " +
        "pause_viertel_zu_stunde_ms INTEGER DEFAULT 0, " +
        "halbstunde_schlaganzahl INTEGER DEFAULT 1, " +
        "volle_stunde_anzahl INTEGER DEFAULT 12, " +
        "schlagen_wenn_melodie_laeuft INTEGER DEFAULT 0, " +
        "kein_schlagwerk_ostern INTEGER DEFAULT 0, " +
        "kein_schlagwerk_karfreitag INTEGER DEFAULT 0, " +
        "kein_schlagwerk_karsamstag INTEGER DEFAULT 0, " +
        "aktiv INTEGER DEFAULT 1, " +
        "sortierung INTEGER DEFAULT 0, " +
        "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
        "updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
        ")";
    
    private static final String CREATE_TABLE_RELAIS_KATEGORIEN =
        "CREATE TABLE " + TABLE_RELAIS_KATEGORIEN + " (" +
        "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
        "name TEXT NOT NULL, " +
        "sortierung INTEGER DEFAULT 0, " +
        "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
        "updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
        ")";
    
    private static final String CREATE_TABLE_RELAIS_ZUORDNUNG =
        "CREATE TABLE " + TABLE_RELAIS_ZUORDNUNG + " (" +
        "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
        "software_relais_id INTEGER NOT NULL UNIQUE, " +
        "name TEXT, " +
        "kategorie_id INTEGER REFERENCES " + TABLE_RELAIS_KATEGORIEN + "(id), " +
        "platine_nummer INTEGER NOT NULL, " +
        "relais_nummer INTEGER NOT NULL, " +
        "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
        "updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
        ")";

    private static final String CREATE_TABLE_UI_FUNKTION =
        "CREATE TABLE " + TABLE_UI_FUNKTION + " (" +
        "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
        "code_alt INTEGER NOT NULL UNIQUE, " +
        "key_name TEXT NOT NULL UNIQUE, " +
        "anzeige_name TEXT NOT NULL, " +
        "beschreibung TEXT, " +
        "aktiv INTEGER DEFAULT 1, " +
        "sortierung INTEGER DEFAULT 0, " +
        "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
        "updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
        ")";

    private static final String CREATE_TABLE_LAYOUT_ITEM =
        "CREATE TABLE " + TABLE_LAYOUT_ITEM + " (" +
        "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
        "page_nummer INTEGER NOT NULL DEFAULT 1, " +
        "slot_index INTEGER NOT NULL, " +
        "grid_row INTEGER DEFAULT 0, " +
        "grid_col INTEGER DEFAULT 0, " +
        "grid_width INTEGER DEFAULT 1, " +
        "grid_height INTEGER DEFAULT 1, " +
        "label_text TEXT, " +
        "button_id TEXT, " +
        "target_kind TEXT, " +
        "target_id INTEGER, " +
        "legacy_code TEXT, " +
        "legacy_platine TEXT, " +
        "legacy_hammerzeit TEXT, " +
        "legacy_funktion TEXT, " +
        "sound TEXT, " +
        "sichtbar INTEGER DEFAULT 1, " +
        "sortierung INTEGER DEFAULT 0, " +
        "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
        "updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
        "UNIQUE(page_nummer, slot_index)" +
        ")";
    
    private static final String CREATE_INDEX = 
        "CREATE INDEX IF NOT EXISTS idx_platinen_nummer ON " + TABLE_PLATINEN + "(platine_nummer)";
    
    private static final String CREATE_INDEX_NEBENUHR = 
        "CREATE INDEX IF NOT EXISTS idx_nebenuhr_uhr_name ON " + TABLE_NEBENUHR_CONFIG + "(uhr_name)";
    
    private static final String CREATE_INDEX_TAGTYPEN = 
        "CREATE INDEX IF NOT EXISTS idx_tagtypen_name ON " + TABLE_TAGTYPEN + "(name)";
    
    private static final String CREATE_INDEX_PROGRAMME_TAGTYP = 
        "CREATE INDEX IF NOT EXISTS idx_programme_tagtyp ON " + TABLE_PROGRAMME + "(tagtyp_id)";
    
    private static final String CREATE_INDEX_PROGRAMME_STARTZEIT = 
        "CREATE INDEX IF NOT EXISTS idx_programme_startzeit ON " + TABLE_PROGRAMME + "(startzeit)";
    
    private static final String CREATE_INDEX_OSTERFEIERTAGE = 
        "CREATE INDEX IF NOT EXISTS idx_osterfeiertage_name ON " + TABLE_OSTERFEIERTAGE + "(name)";
    
    private static final String CREATE_INDEX_FESTE_FEIERTAGE = 
        "CREATE INDEX IF NOT EXISTS idx_feste_feiertage_datum ON " + TABLE_FESTE_FEIERTAGE + "(datum)";
    
    private static final String CREATE_INDEX_SONDERFEIERTAGE = 
        "CREATE INDEX IF NOT EXISTS idx_sonderfeiertage_datum ON " + TABLE_SONDERFEIERTAGE + "(berechnetes_datum, jahr)";
    
    private static final String CREATE_INDEX_VORSCHWINGEN = 
        "CREATE INDEX IF NOT EXISTS idx_vorschwingen_aktiv ON " + TABLE_VORSCHWINGEN_CONFIG + "(aktiv)";
    
    private static final String CREATE_INDEX_MIDI_CONFIG = 
        "CREATE INDEX IF NOT EXISTS idx_midi_config_dateiname ON " + TABLE_MIDI_CONFIG + "(dateiname)";
    
    private static final String CREATE_INDEX_MIDI_NOTEN_RELAIS = 
        "CREATE INDEX IF NOT EXISTS idx_midi_noten_relais_note ON " + TABLE_MIDI_NOTEN_RELAIS + "(note)";
    
    private static final String CREATE_INDEX_MELODIEN = 
        "CREATE INDEX IF NOT EXISTS idx_melodien_name ON " + TABLE_MELODIEN + "(name)";
    
    private static final String CREATE_INDEX_MELODIE_ZEILEN = 
        "CREATE INDEX IF NOT EXISTS idx_melodie_zeilen_melodie ON " + TABLE_MELODIE_ZEILEN + "(melodie_id)";
    
    private static final String CREATE_INDEX_BESCHRIFTUNG_TASTEN =
        "CREATE INDEX IF NOT EXISTS idx_beschriftung_tasten_zeile ON " + TABLE_BESCHRIFTUNG_TASTEN + "(zeile_index)";
    
    private static final String CREATE_INDEX_SCHLAGWERK_CONFIG =
        "CREATE INDEX IF NOT EXISTS idx_schlagwerk_config_typ ON " + TABLE_SCHLAGWERK_CONFIG + "(typ)";
    
    private static final String CREATE_INDEX_RELAIS_ZUORDNUNG_SOFTWARE =
        "CREATE INDEX IF NOT EXISTS idx_relais_zuordnung_software_id ON " + TABLE_RELAIS_ZUORDNUNG + "(software_relais_id)";
    private static final String CREATE_INDEX_RELAIS_ZUORDNUNG_KATEGORIE =
        "CREATE INDEX IF NOT EXISTS idx_relais_zuordnung_kategorie ON " + TABLE_RELAIS_ZUORDNUNG + "(kategorie_id)";
    private static final String CREATE_INDEX_UI_FUNKTION_CODE =
        "CREATE INDEX IF NOT EXISTS idx_ui_funktion_code_alt ON " + TABLE_UI_FUNKTION + "(code_alt)";
    private static final String CREATE_INDEX_LAYOUT_ITEM_PAGE_SLOT =
        "CREATE INDEX IF NOT EXISTS idx_layout_item_page_slot ON " + TABLE_LAYOUT_ITEM + "(page_nummer, slot_index)";
    
    private static PlatinenDatabaseHelper instance;
    private Context context;
    
    public PlatinenDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        this.context = context;
    }
    
    public static synchronized PlatinenDatabaseHelper getInstance(Context context) {
        if (instance == null) {
            instance = new PlatinenDatabaseHelper(context.getApplicationContext());
        }
        return instance;
    }

    /**
     * Schließt die Singleton-Instanz und alle DB-Verbindungen (für Restore/Import).
     * Nach dem Überschreiben der DB-Datei liefert getInstance() wieder eine neue Verbindung.
     */
    public static synchronized void closeInstanceForRestore() {
        if (instance != null) {
            try {
                instance.close();
            } catch (Exception e) {
                Log.w(TAG, "closeInstanceForRestore", e);
            }
            instance = null;
        }
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // Kein Excel mehr beim Start – nur DB-Defaults. Excel nur bei manuellem Import.
        createTablesAndDefaults(db, false);
    }

    /**
     * Erstellt alle Tabellen und Standarddaten. Bei runExcelMigrations=true werden zusätzlich
     * vorhandene Excel-Dateien migriert; bei false reine Werkszustand-DB ohne Excel (z. B. für „Anlage frisch aufsetzen“).
     */
    private void createTablesAndDefaults(SQLiteDatabase db, boolean runExcelMigrations) {
        Log.d(TAG, "Erstelle Datenbank-Tabellen" + (runExcelMigrations ? " (mit Excel-Migration)" : " (Werkszustand ohne Excel)"));
        db.execSQL(CREATE_TABLE_PLATINEN);
        db.execSQL(CREATE_TABLE_IO_CONFIG);
        db.execSQL(CREATE_TABLE_NEBENUHR_CONFIG);
        db.execSQL(CREATE_TABLE_TAGTYPEN);
        db.execSQL(CREATE_TABLE_PROGRAMME);
        db.execSQL(CREATE_TABLE_OSTERFEIERTAGE);
        db.execSQL(CREATE_TABLE_FESTE_FEIERTAGE);
        db.execSQL(CREATE_TABLE_SONDERFEIERTAGE);
        db.execSQL(CREATE_TABLE_VORSCHWINGEN_CONFIG);
        db.execSQL(CREATE_TABLE_MIDI_CONFIG);
        db.execSQL(CREATE_TABLE_MIDI_NOTEN_RELAIS);
        db.execSQL(CREATE_TABLE_MELODIEN);
        db.execSQL(CREATE_TABLE_MELODIE_ZEILEN);
        db.execSQL(CREATE_TABLE_BESCHRIFTUNG_TASTEN);
        db.execSQL(CREATE_TABLE_SCHLAGWERK_CONFIG);
        db.execSQL(CREATE_TABLE_RELAIS_KATEGORIEN);
        db.execSQL(CREATE_TABLE_RELAIS_ZUORDNUNG);
        db.execSQL(CREATE_TABLE_UI_FUNKTION);
        db.execSQL(CREATE_TABLE_LAYOUT_ITEM);
        db.execSQL(CREATE_INDEX);
        db.execSQL(CREATE_INDEX_NEBENUHR);
        db.execSQL(CREATE_INDEX_TAGTYPEN);
        db.execSQL(CREATE_INDEX_PROGRAMME_TAGTYP);
        db.execSQL(CREATE_INDEX_PROGRAMME_STARTZEIT);
        db.execSQL(CREATE_INDEX_OSTERFEIERTAGE);
        db.execSQL(CREATE_INDEX_FESTE_FEIERTAGE);
        db.execSQL(CREATE_INDEX_SONDERFEIERTAGE);
        db.execSQL(CREATE_INDEX_VORSCHWINGEN);
        db.execSQL(CREATE_INDEX_MIDI_CONFIG);
        db.execSQL(CREATE_INDEX_MIDI_NOTEN_RELAIS);
        db.execSQL(CREATE_INDEX_MELODIEN);
        db.execSQL(CREATE_INDEX_MELODIE_ZEILEN);
        db.execSQL(CREATE_INDEX_BESCHRIFTUNG_TASTEN);
        db.execSQL(CREATE_INDEX_SCHLAGWERK_CONFIG);
        db.execSQL(CREATE_INDEX_RELAIS_ZUORDNUNG_SOFTWARE);
        db.execSQL(CREATE_INDEX_RELAIS_ZUORDNUNG_KATEGORIE);
        db.execSQL(CREATE_INDEX_UI_FUNKTION_CODE);
        db.execSQL(CREATE_INDEX_LAYOUT_ITEM_PAGE_SLOT);
        
        // Default-Modus: wifi
        android.content.ContentValues values = new android.content.ContentValues();
        values.put("key", "modus");
        values.put("value", "wifi");
        db.insert(TABLE_IO_CONFIG, null, values);
        
        // Default Power off Akku Prozent: 75%
        android.content.ContentValues akkuValues = new android.content.ContentValues();
        akkuValues.put("key", "power_off_akku_prozent");
        akkuValues.put("value", "75");
        db.insert(TABLE_IO_CONFIG, null, akkuValues);

        // Default Autostart der App: ein (App soll immer laufen, außer Akku niedrig + kein Netz)
        android.content.ContentValues autostartValues = new android.content.ContentValues();
        autostartValues.put("key", "autostart_der_app");
        autostartValues.put("value", "ein");
        db.insert(TABLE_IO_CONFIG, null, autostartValues);
        
        // Default Nebenuhr-Konfigurationen (A, B, C, D)
        String[] uhrNamen = {"A", "B", "C", "D"};
        int[] zeilen = {3, 4, 5, 6};
        String[] displayNamen = {"Nebenuhr A", "Nebenuhr B", "Nebenuhr C", "Monduhr D"};
        String[] modi = {"12", "12", "12", "MOND"};
        for (int i = 0; i < uhrNamen.length; i++) {
            android.content.ContentValues nebenuhrValues = new android.content.ContentValues();
            nebenuhrValues.put("uhr_name", uhrNamen[i]);
            nebenuhrValues.put("zeile_index", zeilen[i]);
            nebenuhrValues.put("relais_a", 0); // Standard 0 = Keins (wird über Taste/Config zugewiesen)
            nebenuhrValues.put("relais_b", 0);
            nebenuhrValues.put("impuls_dauer_1", 1); // 1 Sekunde
            nebenuhrValues.put("impuls_dauer_2", 1);  // 1 Sekunde Pause
            nebenuhrValues.put("uhr_name_display", displayNamen[i]);
            nebenuhrValues.put("modus", modi[i]);
            nebenuhrValues.put("angezeigte_zeit", 0);
            nebenuhrValues.put("mondphase_ist", 0);
            nebenuhrValues.put("last_relais_a", 0);
            nebenuhrValues.put("aktiv", 1);
            db.insert(TABLE_NEBENUHR_CONFIG, null, nebenuhrValues);
        }
        
        // Standard-Osterfeiertage einfügen
        insertDefaultOsterfeiertage(db);
        
        // Schlagwerk-Config: Typ 1 und Typ 2
        insertDefaultSchlagwerkConfig(db);
        
        // Standard-Relais-Kategorien
        insertDefaultRelaisKategorien(db);
        insertDefaultUiFunktionen(db);
        
        if (!runExcelMigrations) {
            // Neuanlage/Werkszustand: Normalprogramm + Stop/Automatik + feste Feiertage (ohne Excel)
            insertDefaultNormalprogramm(db);
            insertDefaultBeschriftungTastenNeuanlage(db);
            insertDefaultFesteFeiertage(db);
        }
        
        if (runExcelMigrations) {
            // Migriere Daten aus Excel (falls vorhanden)
            migrateFromExcel(db);
            try {
                migrateOsterfeiertageFromExcel(db);
            } catch (Exception e) {
                Log.w(TAG, "Fehler bei Migration von Osterfeiertage-Tagtypen in onCreate", e);
            }
            try {
                migrateFesteFeiertageFromExcel(db);
            } catch (Exception e) {
                Log.w(TAG, "Fehler bei Migration von Feste-Feiertage in onCreate", e);
            }
            try {
                migrateVorschwingenFromExcel(db);
            } catch (Exception e) {
                Log.w(TAG, "Fehler bei Migration von Vorschwingen in onCreate", e);
            }
        }
        ensureLayoutItemsFromBeschriftungTasten(db);
    }

    /**
     * Setzt die Datenbank auf Werkszustand zurück: alle Tabellen werden gelöscht und neu mit
     * Standarddaten angelegt – ohne Excel-Import. Für „Anlage frisch aufsetzen“ ohne Excel.
     * Alle bestehenden Konfigurationsdaten (Programme, Melodien, Platinen, Beschriftungstasten usw.) gehen verloren.
     */
    public void resetToFactoryState() {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_RELAIS_ZUORDNUNG);
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_RELAIS_KATEGORIEN);
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_LAYOUT_ITEM);
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_UI_FUNKTION);
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_SCHLAGWERK_CONFIG);
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_BESCHRIFTUNG_TASTEN);
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_MELODIE_ZEILEN);
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_MELODIEN);
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_MIDI_NOTEN_RELAIS);
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_MIDI_CONFIG);
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_VORSCHWINGEN_CONFIG);
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_SONDERFEIERTAGE);
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_FESTE_FEIERTAGE);
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_OSTERFEIERTAGE);
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_PROGRAMME);
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_TAGTYPEN);
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_NEBENUHR_CONFIG);
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_IO_CONFIG);
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_PLATINEN);
            createTablesAndDefaults(db, false);
            db.setTransactionSuccessful();
            Log.d(TAG, "Datenbank auf Werkszustand zurückgesetzt (ohne Excel)");
        } finally {
            db.endTransaction();
        }
    }
    
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.d(TAG, "Upgrade Datenbank von Version " + oldVersion + " zu " + newVersion);
        
        if (oldVersion < 2) {
            // Version 1 -> 2: Füge mac_adresse und online Spalten hinzu
            try {
                db.execSQL("ALTER TABLE " + TABLE_PLATINEN + " ADD COLUMN mac_adresse TEXT");
                Log.d(TAG, "Spalte mac_adresse hinzugefügt");
            } catch (Exception e) {
                Log.w(TAG, "Spalte mac_adresse existiert bereits oder Fehler beim Hinzufügen", e);
            }
            
            try {
                db.execSQL("ALTER TABLE " + TABLE_PLATINEN + " ADD COLUMN online BOOLEAN DEFAULT 0");
                Log.d(TAG, "Spalte online hinzugefügt");
            } catch (Exception e) {
                Log.w(TAG, "Spalte online existiert bereits oder Fehler beim Hinzufügen", e);
            }
        }
        
        if (oldVersion < 3) {
            // Version 2 -> 3: Füge nebenuhr_config Tabelle hinzu
            try {
                db.execSQL(CREATE_TABLE_NEBENUHR_CONFIG);
                db.execSQL(CREATE_INDEX_NEBENUHR);
                Log.d(TAG, "Tabelle nebenuhr_config hinzugefügt");
                
                // Default Nebenuhr-Konfigurationen (A, B, C, D)
                String[] uhrNamen = {"A", "B", "C", "D"};
                int[] zeilen = {3, 4, 5, 6};
                String[] displayNamen = {"Nebenuhr A", "Nebenuhr B", "Nebenuhr C", "Monduhr D"};
                String[] modi = {"12", "12", "12", "MOND"};
                for (int i = 0; i < uhrNamen.length; i++) {
                    android.content.ContentValues nebenuhrValues = new android.content.ContentValues();
                    nebenuhrValues.put("uhr_name", uhrNamen[i]);
                    nebenuhrValues.put("zeile_index", zeilen[i]);
                    nebenuhrValues.put("relais_a", 0);
                    nebenuhrValues.put("relais_b", 0);
                    nebenuhrValues.put("impuls_dauer_1", 100);
                    nebenuhrValues.put("impuls_dauer_2", 50);
                    nebenuhrValues.put("uhr_name_display", displayNamen[i]);
                    nebenuhrValues.put("modus", modi[i]);
                    nebenuhrValues.put("angezeigte_zeit", 0);
                    nebenuhrValues.put("mondphase_ist", 0);
                    nebenuhrValues.put("last_relais_a", 0);
                    nebenuhrValues.put("aktiv", 1);
                    db.insert(TABLE_NEBENUHR_CONFIG, null, nebenuhrValues);
                }
                
                // Migriere Nebenuhr-Daten aus Excel (falls vorhanden)
                migrateNebenuhrenFromExcel(db);
            } catch (Exception e) {
                Log.e(TAG, "Fehler beim Hinzufügen der nebenuhr_config Tabelle", e);
            }
        }
        
        if (oldVersion < 4) {
            // Version 3 -> 4: Füge tagtypen und programme Tabellen hinzu
            try {
                db.execSQL(CREATE_TABLE_TAGTYPEN);
                db.execSQL(CREATE_TABLE_PROGRAMME);
                db.execSQL(CREATE_INDEX_TAGTYPEN);
                db.execSQL(CREATE_INDEX_PROGRAMME_TAGTYP);
                db.execSQL(CREATE_INDEX_PROGRAMME_STARTZEIT);
                Log.d(TAG, "Tabellen tagtypen und programme hinzugefügt");
                
                // Migriere Tagtypen und Programme aus Excel-Dateien
                migrateTagtypenAndProgrammeFromExcel(db);
            } catch (Exception e) {
                Log.e(TAG, "Fehler beim Hinzufügen der tagtypen/programme Tabellen", e);
            }
        }
        
        if (oldVersion < 5) {
            // Version 4 -> 5: Füge osterfeiertage Tabelle hinzu
            try {
                db.execSQL(CREATE_TABLE_OSTERFEIERTAGE);
                db.execSQL(CREATE_INDEX_OSTERFEIERTAGE);
                Log.d(TAG, "Tabelle osterfeiertage hinzugefügt");
                
                // Standard-Osterfeiertage einfügen
                insertDefaultOsterfeiertage(db);
                
                // Migriere Tagtypen aus Excel (falls vorhanden)
                migrateOsterfeiertageFromExcel(db);
            } catch (Exception e) {
                Log.e(TAG, "Fehler beim Hinzufügen der osterfeiertage Tabelle", e);
            }
        }
        
        if (oldVersion < 6) {
            // Version 5 -> 6: Füge feste_feiertage Tabelle hinzu
            try {
                db.execSQL(CREATE_TABLE_FESTE_FEIERTAGE);
                db.execSQL(CREATE_INDEX_FESTE_FEIERTAGE);
                Log.d(TAG, "Tabelle feste_feiertage hinzugefügt");
                
                // Migriere feste Feiertage aus Excel (falls vorhanden)
                migrateFesteFeiertageFromExcel(db);
            } catch (Exception e) {
                Log.e(TAG, "Fehler beim Hinzufügen der feste_feiertage Tabelle", e);
            }
        }
        
        if (oldVersion < 7) {
            // Version 6 -> 7: Neue Nebenuhr-Struktur (relais_a/b, impuls_dauer_1/2, modus, mondphase_ist, last_relais_a) + Monduhr D
            try {
                Log.d(TAG, "Migriere Nebenuhr-Struktur von Version 6 zu 7");
                
                // Prüfe ob alte Spalten existieren
                Cursor cursor = db.rawQuery("PRAGMA table_info(" + TABLE_NEBENUHR_CONFIG + ")", null);
                boolean hasRelaisGerade = false;
                boolean hasRelaisUngerade = false;
                boolean hasImpulsZeit = false;
                boolean hasImpulsPause = false;
                boolean hasRelaisA = false;
                boolean hasRelaisB = false;
                boolean hasImpulsDauer1 = false;
                boolean hasImpulsDauer2 = false;
                boolean hasModus = false;
                boolean hasAngezeigteZeit = false;
                boolean hasMondphaseIst = false;
                boolean hasLastRelaisA = false;
                boolean hasAktiv = false;
                
                if (cursor != null) {
                    while (cursor.moveToNext()) {
                        String colName = cursor.getString(1);
                        if ("relais_gerade".equals(colName)) hasRelaisGerade = true;
                        if ("relais_ungerade".equals(colName)) hasRelaisUngerade = true;
                        if ("impuls_zeit".equals(colName)) hasImpulsZeit = true;
                        if ("impuls_pause".equals(colName)) hasImpulsPause = true;
                        if ("relais_a".equals(colName)) hasRelaisA = true;
                        if ("relais_b".equals(colName)) hasRelaisB = true;
                        if ("impuls_dauer_1".equals(colName)) hasImpulsDauer1 = true;
                        if ("impuls_dauer_2".equals(colName)) hasImpulsDauer2 = true;
                        if ("modus".equals(colName)) hasModus = true;
                        if ("angezeigte_zeit".equals(colName)) hasAngezeigteZeit = true;
                        if ("mondphase_ist".equals(colName)) hasMondphaseIst = true;
                        if ("last_relais_a".equals(colName)) hasLastRelaisA = true;
                        if ("aktiv".equals(colName)) hasAktiv = true;
                    }
                    cursor.close();
                }
                
                // Füge neue Spalten hinzu
                if (!hasRelaisA) {
                    db.execSQL("ALTER TABLE " + TABLE_NEBENUHR_CONFIG + " ADD COLUMN relais_a INTEGER DEFAULT 0");
                    Log.d(TAG, "Spalte relais_a hinzugefügt");
                }
                if (!hasRelaisB) {
                    db.execSQL("ALTER TABLE " + TABLE_NEBENUHR_CONFIG + " ADD COLUMN relais_b INTEGER DEFAULT 0");
                    Log.d(TAG, "Spalte relais_b hinzugefügt");
                }
                if (!hasImpulsDauer1) {
                    db.execSQL("ALTER TABLE " + TABLE_NEBENUHR_CONFIG + " ADD COLUMN impuls_dauer_1 INTEGER DEFAULT 1");
                    Log.d(TAG, "Spalte impuls_dauer_1 hinzugefügt");
                }
                if (!hasImpulsDauer2) {
                    db.execSQL("ALTER TABLE " + TABLE_NEBENUHR_CONFIG + " ADD COLUMN impuls_dauer_2 INTEGER DEFAULT 1");
                    Log.d(TAG, "Spalte impuls_dauer_2 hinzugefügt");
                }
                if (!hasModus) {
                    db.execSQL("ALTER TABLE " + TABLE_NEBENUHR_CONFIG + " ADD COLUMN modus TEXT DEFAULT '12'");
                    Log.d(TAG, "Spalte modus hinzugefügt");
                }
                if (!hasAngezeigteZeit) {
                    db.execSQL("ALTER TABLE " + TABLE_NEBENUHR_CONFIG + " ADD COLUMN angezeigte_zeit INTEGER DEFAULT 0");
                    Log.d(TAG, "Spalte angezeigte_zeit hinzugefügt");
                }
                if (!hasMondphaseIst) {
                    db.execSQL("ALTER TABLE " + TABLE_NEBENUHR_CONFIG + " ADD COLUMN mondphase_ist INTEGER DEFAULT 0");
                    Log.d(TAG, "Spalte mondphase_ist hinzugefügt");
                }
                if (!hasLastRelaisA) {
                    db.execSQL("ALTER TABLE " + TABLE_NEBENUHR_CONFIG + " ADD COLUMN last_relais_a INTEGER DEFAULT 0");
                    Log.d(TAG, "Spalte last_relais_a hinzugefügt");
                }
                if (!hasAktiv) {
                    db.execSQL("ALTER TABLE " + TABLE_NEBENUHR_CONFIG + " ADD COLUMN aktiv INTEGER DEFAULT 1");
                    Log.d(TAG, "Spalte aktiv hinzugefügt");
                }
                
                // Migriere bestehende Daten: relais_gerade → relais_a, relais_ungerade → relais_b
                if (hasRelaisGerade && hasRelaisA) {
                    db.execSQL("UPDATE " + TABLE_NEBENUHR_CONFIG + " SET relais_a = relais_gerade WHERE relais_a = 0 OR relais_a IS NULL");
                    Log.d(TAG, "Daten migriert: relais_gerade → relais_a");
                }
                if (hasRelaisUngerade && hasRelaisB) {
                    db.execSQL("UPDATE " + TABLE_NEBENUHR_CONFIG + " SET relais_b = relais_ungerade WHERE relais_b = 0 OR relais_b IS NULL");
                    Log.d(TAG, "Daten migriert: relais_ungerade → relais_b");
                }
                
                // Migriere bestehende Daten: impuls_zeit → impuls_dauer_1 (Sekunden bleiben Sekunden)
                if (hasImpulsZeit && hasImpulsDauer1) {
                    db.execSQL("UPDATE " + TABLE_NEBENUHR_CONFIG + " SET impuls_dauer_1 = impuls_zeit WHERE impuls_dauer_1 = 1 OR impuls_dauer_1 IS NULL");
                    Log.d(TAG, "Daten migriert: impuls_zeit → impuls_dauer_1 (Sekunden)");
                }
                
                // Migriere bestehende Daten: impuls_pause → impuls_dauer_2 (Sekunden bleiben Sekunden)
                if (hasImpulsPause && hasImpulsDauer2) {
                    db.execSQL("UPDATE " + TABLE_NEBENUHR_CONFIG + " SET impuls_dauer_2 = impuls_pause WHERE impuls_dauer_2 = 1 OR impuls_dauer_2 IS NULL");
                    Log.d(TAG, "Daten migriert: impuls_pause → impuls_dauer_2 (Sekunden)");
                }
                
                // Konvertiere bestehende Millisekunden-Werte zu Sekunden (falls vorhanden)
                // Prüfe ob Werte > 100 sind (wahrscheinlich Millisekunden)
                Cursor checkMs = db.rawQuery("SELECT id, impuls_dauer_1, impuls_dauer_2 FROM " + TABLE_NEBENUHR_CONFIG + " WHERE impuls_dauer_1 > 100 OR impuls_dauer_2 > 100", null);
                if (checkMs != null && checkMs.getCount() > 0) {
                    android.content.ContentValues updateValues = new android.content.ContentValues();
                    while (checkMs.moveToNext()) {
                        int id = checkMs.getInt(0);
                        int dauer1 = checkMs.getInt(1);
                        int dauer2 = checkMs.getInt(2);
                        if (dauer1 > 100) {
                            updateValues.put("impuls_dauer_1", dauer1 / 1000);
                        }
                        if (dauer2 > 100) {
                            updateValues.put("impuls_dauer_2", dauer2 / 1000);
                        }
                        if (updateValues.size() > 0) {
                            db.update(TABLE_NEBENUHR_CONFIG, updateValues, "id = ?", new String[]{String.valueOf(id)});
                            updateValues.clear();
                        }
                    }
                    Log.d(TAG, "Millisekunden-Werte zu Sekunden konvertiert");
                }
                if (checkMs != null) checkMs.close();
                
                // Setze modus auf "12" für bestehende Nebenuhren A, B, C
                if (hasModus) {
                    db.execSQL("UPDATE " + TABLE_NEBENUHR_CONFIG + " SET modus = '12' WHERE modus IS NULL AND uhr_name IN ('A', 'B', 'C')");
                    Log.d(TAG, "Modus auf '12' gesetzt für Nebenuhren A, B, C");
                }
                
                // Füge Monduhr D hinzu (falls nicht vorhanden)
                Cursor checkD = db.query(TABLE_NEBENUHR_CONFIG, new String[]{"id"}, "uhr_name = ?", new String[]{"D"}, null, null, null);
                if (!checkD.moveToFirst()) {
                    android.content.ContentValues nebenuhrDValues = new android.content.ContentValues();
                    nebenuhrDValues.put("uhr_name", "D");
                    nebenuhrDValues.put("zeile_index", 6);
                    nebenuhrDValues.put("relais_a", 0);
                    nebenuhrDValues.put("relais_b", 0);
                    nebenuhrDValues.put("impuls_dauer_1", 1);
                    nebenuhrDValues.put("impuls_dauer_2", 1);
                    nebenuhrDValues.put("uhr_name_display", "Monduhr D");
                    nebenuhrDValues.put("modus", "MOND");
                    nebenuhrDValues.put("angezeigte_zeit", 0);
                    nebenuhrDValues.put("mondphase_ist", 0);
                    nebenuhrDValues.put("last_relais_a", 0);
                    nebenuhrDValues.put("aktiv", 1);
                    db.insert(TABLE_NEBENUHR_CONFIG, null, nebenuhrDValues);
                    Log.d(TAG, "Monduhr D hinzugefügt");
                }
                checkD.close();
                
                Log.d(TAG, "Migration von Version 6 zu 7 abgeschlossen");
            } catch (Exception e) {
                Log.e(TAG, "Fehler bei Migration von Version 6 zu 7", e);
            }
        }
        
        if (oldVersion < 8) {
            // Version 7 -> 8: Füge vorschwingen_config Tabelle hinzu
            try {
                db.execSQL(CREATE_TABLE_VORSCHWINGEN_CONFIG);
                db.execSQL(CREATE_INDEX_VORSCHWINGEN);
                Log.d(TAG, "Tabelle vorschwingen_config hinzugefügt");
                
                // Migriere Vorschwingen aus Excel (falls vorhanden)
                migrateVorschwingenFromExcel(db);
            } catch (Exception e) {
                Log.e(TAG, "Fehler beim Hinzufügen der vorschwingen_config Tabelle", e);
            }
        }
        
        if (oldVersion < 9) {
            // Version 8 -> 9: Füge relais_anzahl Spalte zu Platinen hinzu
            try {
                db.execSQL("ALTER TABLE " + TABLE_PLATINEN + " ADD COLUMN relais_anzahl INTEGER DEFAULT 32");
                Log.d(TAG, "Spalte relais_anzahl zu platinen_config hinzugefügt");
            } catch (Exception e) {
                Log.w(TAG, "Spalte relais_anzahl existiert bereits oder Fehler beim Hinzufügen", e);
            }
        }
        
        if (oldVersion < 10) {
            // Version 9 -> 10: Füge verschieben_auf_sonntag Spalte zu feste_feiertage hinzu
            try {
                db.execSQL("ALTER TABLE " + TABLE_FESTE_FEIERTAGE + " ADD COLUMN verschieben_auf_sonntag BOOLEAN DEFAULT 0");
                Log.d(TAG, "Spalte verschieben_auf_sonntag zu feste_feiertage hinzugefügt");
            } catch (Exception e) {
                Log.w(TAG, "Spalte verschieben_auf_sonntag existiert bereits oder Fehler beim Hinzufügen", e);
            }
        }
        
        if (oldVersion < 11) {
            // Version 10 -> 11: Füge sonderfeiertage Tabelle hinzu
            try {
                db.execSQL(CREATE_TABLE_SONDERFEIERTAGE);
                db.execSQL(CREATE_INDEX_SONDERFEIERTAGE);
                Log.d(TAG, "Tabelle sonderfeiertage hinzugefügt");
            } catch (Exception e) {
                Log.e(TAG, "Fehler beim Hinzufügen der sonderfeiertage Tabelle", e);
            }
        }
        if (oldVersion < 12) {
            // Version 11 -> 12: Füge sonntag_im_monat Spalte zu feste_feiertage hinzu
            try {
                db.execSQL("ALTER TABLE " + TABLE_FESTE_FEIERTAGE + " ADD COLUMN sonntag_im_monat INTEGER DEFAULT 0");
                Log.d(TAG, "Spalte sonntag_im_monat zu feste_feiertage hinzugefügt");
            } catch (Exception e) {
                Log.w(TAG, "Spalte sonntag_im_monat existiert bereits oder Fehler beim Hinzufügen", e);
            }
        }
        
        if (oldVersion < 13) {
            // Version 12 -> 13: Füge MIDI-Tabellen hinzu
            try {
                db.execSQL(CREATE_TABLE_MIDI_CONFIG);
                db.execSQL(CREATE_TABLE_MIDI_NOTEN_RELAIS);
                db.execSQL(CREATE_INDEX_MIDI_CONFIG);
                db.execSQL(CREATE_INDEX_MIDI_NOTEN_RELAIS);
                Log.d(TAG, "MIDI-Tabellen hinzugefügt");
            } catch (Exception e) {
                Log.e(TAG, "Fehler beim Hinzufügen der MIDI-Tabellen", e);
            }
        }
        
        if (oldVersion < 14) {
            // Version 13 -> 14: Füge Melodien-Tabellen hinzu
            try {
                db.execSQL(CREATE_TABLE_MELODIEN);
                db.execSQL(CREATE_TABLE_MELODIE_ZEILEN);
                db.execSQL(CREATE_INDEX_MELODIEN);
                db.execSQL(CREATE_INDEX_MELODIE_ZEILEN);
                Log.d(TAG, "Melodien-Tabellen hinzugefügt");
            } catch (Exception e) {
                Log.e(TAG, "Fehler beim Hinzufügen der Melodien-Tabellen", e);
            }
        }
        
        if (oldVersion < 15) {
            // Version 14 -> 15: Entferne .xls Endung von allen Tagtyp-Namen
            try {
                Log.d(TAG, "Migriere Tagtyp-Namen: Entferne .xls Endung");
                Cursor cursor = db.query(TABLE_TAGTYPEN, new String[]{"id", "name"}, null, null, null, null, null);
                int migrated = 0;
                try {
                    while (cursor.moveToNext()) {
                        int id = cursor.getInt(0);
                        String name = cursor.getString(1);
                        if (name != null && name.toLowerCase().endsWith(".xls")) {
                            String nameWithoutXls = name.substring(0, name.length() - 4);
                            // Prüfe ob Name ohne .xls bereits existiert
                            Cursor checkCursor = db.query(TABLE_TAGTYPEN, new String[]{"id"}, "name = ?", new String[]{nameWithoutXls}, null, null, null);
                            boolean exists = checkCursor.moveToFirst();
                            checkCursor.close();
                            
                            if (!exists) {
                                // Update Name
                                android.content.ContentValues values = new android.content.ContentValues();
                                values.put("name", nameWithoutXls);
                                db.update(TABLE_TAGTYPEN, values, "id = ?", new String[]{String.valueOf(id)});
                                migrated++;
                                Log.d(TAG, "Tagtyp umbenannt: '" + name + "' -> '" + nameWithoutXls + "'");
                            } else {
                                Log.w(TAG, "Tagtyp '" + name + "' kann nicht umbenannt werden, da '" + nameWithoutXls + "' bereits existiert");
                            }
                        }
                    }
                } finally {
                    cursor.close();
                }
                Log.d(TAG, "Tagtyp-Migration abgeschlossen: " + migrated + " Tagtypen umbenannt");
            } catch (Exception e) {
                Log.e(TAG, "Fehler bei Migration der Tagtyp-Namen", e);
            }
        }
        
        if (oldVersion < 16) {
            // Version 15 -> 16: Tabelle beschriftung_tasten (Import aus Beschriftung-Tasten.xls)
            try {
                db.execSQL(CREATE_TABLE_BESCHRIFTUNG_TASTEN);
                db.execSQL(CREATE_INDEX_BESCHRIFTUNG_TASTEN);
                Log.d(TAG, "Tabelle beschriftung_tasten hinzugefügt");
            } catch (Exception e) {
                Log.e(TAG, "Fehler beim Hinzufügen der Tabelle beschriftung_tasten", e);
            }
        }

        if (oldVersion < 21) {
            // Version 20 -> 21: beschriftung_tasten um Funktion (c1), Sonder-ID, Grafik-Pfade (c6-c10) erweitern
            try {
                addColumnIfNotExists(db, TABLE_BESCHRIFTUNG_TASTEN, "c1", "TEXT");
                addColumnIfNotExists(db, TABLE_BESCHRIFTUNG_TASTEN, "sonder_id", "INTEGER");
                addColumnIfNotExists(db, TABLE_BESCHRIFTUNG_TASTEN, "c6", "TEXT");
                addColumnIfNotExists(db, TABLE_BESCHRIFTUNG_TASTEN, "c7", "TEXT");
                addColumnIfNotExists(db, TABLE_BESCHRIFTUNG_TASTEN, "c8", "TEXT");
                addColumnIfNotExists(db, TABLE_BESCHRIFTUNG_TASTEN, "c9", "TEXT");
                addColumnIfNotExists(db, TABLE_BESCHRIFTUNG_TASTEN, "c10", "TEXT");
                Log.d(TAG, "beschriftung_tasten auf Version 21 erweitert");
            } catch (Exception e) {
                Log.e(TAG, "Fehler bei Migration beschriftung_tasten auf v21", e);
            }
        }
        if (oldVersion < 22) {
            try {
                addColumnIfNotExists(db, TABLE_BESCHRIFTUNG_TASTEN, "sound", "TEXT");
                Log.d(TAG, "beschriftung_tasten auf Version 22 erweitert (sound)");
            } catch (Exception e) {
                Log.e(TAG, "Fehler bei Migration beschriftung_tasten auf v22", e);
            }
        }
        if (oldVersion < 23) {
            try {
                db.execSQL(CREATE_TABLE_SCHLAGWERK_CONFIG);
                db.execSQL(CREATE_INDEX_SCHLAGWERK_CONFIG);
                insertDefaultSchlagwerkConfig(db);
                Log.d(TAG, "Tabelle schlagwerk_config hinzugefügt (Version 23)");
            } catch (Exception e) {
                Log.e(TAG, "Fehler beim Hinzufügen der Tabelle schlagwerk_config", e);
            }
        }
        if (oldVersion < 24) {
            try {
                db.execSQL("ALTER TABLE " + TABLE_NEBENUHR_CONFIG + " ADD COLUMN impuls_ausstehend INTEGER DEFAULT 0");
                Log.d(TAG, "Spalte impuls_ausstehend zu nebenuhr_config hinzugefügt (Version 24)");
            } catch (Exception e) {
                Log.w(TAG, "Spalte impuls_ausstehend existiert bereits oder Fehler", e);
            }
        }
        if (oldVersion < 25) {
            try {
                db.execSQL("ALTER TABLE " + TABLE_PLATINEN + " ADD COLUMN impuls_antwort_erwartet TEXT");
                Log.d(TAG, "Spalte impuls_antwort_erwartet zu platinen_config hinzugefügt (Version 25)");
            } catch (Exception e) {
                Log.w(TAG, "Spalte impuls_antwort_erwartet existiert bereits oder Fehler", e);
            }
        }
        if (oldVersion < 30) {
            try {
                db.execSQL("ALTER TABLE " + TABLE_NEBENUHR_CONFIG + " ADD COLUMN pending_relais_a INTEGER DEFAULT NULL");
                Log.d(TAG, "Spalte pending_relais_a zu nebenuhr_config hinzugefügt (Version 30)");
            } catch (Exception e) {
                Log.w(TAG, "Spalte pending_relais_a existiert bereits oder Fehler", e);
            }
        }
        if (oldVersion < 26) {
            Cursor c = null;
            try {
                c = db.query(TABLE_OSTERFEIERTAGE, new String[]{"id"}, "name = ? OR oster_offset = ?", new String[]{"Vortag Palmsonntag", "-8"}, null, null, null);
                if (c == null || !c.moveToFirst()) {
                    android.content.ContentValues values = new android.content.ContentValues();
                    values.put("name", "Vortag Palmsonntag");
                    values.put("oster_offset", -8);
                    db.insert(TABLE_OSTERFEIERTAGE, null, values);
                    Log.d(TAG, "Osterfeiertag 'Vortag Palmsonntag' (Offset -8) eingefügt (Version 26)");
                }
            } catch (Exception e) {
                Log.w(TAG, "Vortag Palmsonntag einfügen (Version 26)", e);
            } finally {
                if (c != null) c.close();
            }
        }
        if (oldVersion < 27) {
            try {
                db.execSQL("UPDATE " + TABLE_OSTERFEIERTAGE + " SET tagtyp_name = 'Normalprogramm' WHERE tagtyp_name IS NULL OR tagtyp_name != 'Normalprogramm'");
                db.execSQL("UPDATE " + TABLE_FESTE_FEIERTAGE + " SET tagtyp_name = 'Normalprogramm' WHERE tagtyp_name IS NULL OR tagtyp_name != 'Normalprogramm'");
                Log.d(TAG, "Alle Oster-/feste Feiertage und Vortage auf Normalprogramm gesetzt (Version 27)");
            } catch (Exception e) {
                Log.w(TAG, "Feiertage auf Normalprogramm setzen (Version 27)", e);
            }
        }
        if (oldVersion < 28) {
            try {
                db.execSQL(CREATE_TABLE_RELAIS_KATEGORIEN);
                db.execSQL(CREATE_TABLE_RELAIS_ZUORDNUNG);
                db.execSQL(CREATE_INDEX_RELAIS_ZUORDNUNG_SOFTWARE);
                db.execSQL(CREATE_INDEX_RELAIS_ZUORDNUNG_KATEGORIE);
                insertDefaultRelaisKategorien(db);
                Log.d(TAG, "Tabellen relais_kategorien und relais_zuordnung hinzugefügt (Version 28)");
            } catch (Exception e) {
                Log.e(TAG, "Fehler beim Hinzufügen der Relais-Kategorien/Zuordnung (Version 28)", e);
            }
        }
        if (oldVersion < 29) {
            try {
                db.execSQL(CREATE_TABLE_UI_FUNKTION);
                db.execSQL(CREATE_TABLE_LAYOUT_ITEM);
                db.execSQL(CREATE_INDEX_UI_FUNKTION_CODE);
                db.execSQL(CREATE_INDEX_LAYOUT_ITEM_PAGE_SLOT);
                insertDefaultUiFunktionen(db);
                ensureLayoutItemsFromBeschriftungTasten(db);
                Log.d(TAG, "Tabellen ui_funktion und layout_item hinzugefuegt (Version 29)");
            } catch (Exception e) {
                Log.e(TAG, "Fehler beim Hinzufuegen von ui_funktion/layout_item (Version 29)", e);
            }
        }
    }

    private void addColumnIfNotExists(SQLiteDatabase db, String table, String column, String type) {
        try {
            db.execSQL("ALTER TABLE " + table + " ADD COLUMN " + column + " " + type);
        } catch (android.database.sqlite.SQLiteException e) {
            if (!e.getMessage().contains("duplicate column")) {
                throw e;
            }
        }
    }

    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // App-Version ist älter als die vorhandene DB (z. B. älteres APK installiert).
        // Standard wäre Abbruch. Stattdessen: DB zurücksetzen, damit die App startet.
        Log.w(TAG, "DB-Downgrade von Version " + oldVersion + " auf " + newVersion + " – setze Datenbank zurück (Daten gehen verloren).");
        try {
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_SCHLAGWERK_CONFIG);
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_BESCHRIFTUNG_TASTEN);
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_MELODIE_ZEILEN);
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_MELODIEN);
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_MIDI_NOTEN_RELAIS);
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_MIDI_CONFIG);
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_VORSCHWINGEN_CONFIG);
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_SONDERFEIERTAGE);
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_FESTE_FEIERTAGE);
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_OSTERFEIERTAGE);
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_PROGRAMME);
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_TAGTYPEN);
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_NEBENUHR_CONFIG);
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_IO_CONFIG);
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_PLATINEN);
            onCreate(db);
        } catch (Exception e) {
            Log.e(TAG, "Fehler beim DB-Downgrade/Reset", e);
            throw new IllegalStateException("Datenbank-Downgrade fehlgeschlagen. Bitte App-Daten löschen oder neuere App installieren.", e);
        }
    }
    
    /**
     * Lädt alle Platinen aus der Datenbank.
     */
    public List<Platine> getAllPlatinen() {
        List<Platine> platinen = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        
        Cursor cursor = db.query(TABLE_PLATINEN,
            new String[]{"id", "platine_nummer", "ip_adresse", "port", "aktiv", "mac_adresse", "online", "relais_anzahl", "impuls_antwort_erwartet"},
            null, null, null, null, "platine_nummer ASC");
        
        try {
            while (cursor.moveToNext()) {
                Platine platine = new Platine();
                platine.id = cursor.getInt(0);
                platine.platineNummer = cursor.getInt(1);
                platine.ipAdresse = cursor.isNull(2) ? null : cursor.getString(2);
                platine.port = cursor.isNull(3) ? 50210 : cursor.getInt(3);
                platine.aktiv = cursor.getInt(4) == 1;
                platine.macAdresse = cursor.isNull(5) ? null : cursor.getString(5);
                platine.online = cursor.getInt(6) == 1;
                int ra = cursor.isNull(7) ? 32 : cursor.getInt(7);
                platine.relaisAnzahl = (ra <= 0) ? 32 : ra;
                platine.impulsAntwortErwartet = cursor.getColumnCount() > 8 && !cursor.isNull(8) ? cursor.getString(8) : null;
                platinen.add(platine);
            }
        } finally {
            cursor.close();
        }
        
        // Stelle sicher, dass alle 4 Platinen vorhanden sind
        for (int i = 1; i <= 4; i++) {
            boolean found = false;
            for (Platine p : platinen) {
                if (p.platineNummer == i) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                Platine platine = new Platine();
                platine.platineNummer = i;
                platine.ipAdresse = null;
                platine.port = 50210;
                platine.aktiv = false;
                platine.macAdresse = null;
                platine.online = false;
                platine.relaisAnzahl = 32;
                platinen.add(platine);
            }
        }
        
        // Sortiere nach Platine-Nummer (Collections.sort für API < 24)
        Collections.sort(platinen, new java.util.Comparator<Platine>() {
            @Override
            public int compare(Platine a, Platine b) {
                return Integer.compare(a.platineNummer, b.platineNummer);
            }
        });
        
        return platinen;
    }
    
    /**
     * Lädt eine Platine nach Nummer.
     */
    public Platine getPlatine(int platineNummer) {
        SQLiteDatabase db = getReadableDatabase();
        
        Cursor cursor = db.query(TABLE_PLATINEN,
            new String[]{"id", "platine_nummer", "ip_adresse", "port", "aktiv", "mac_adresse", "online", "relais_anzahl", "impuls_antwort_erwartet"},
            "platine_nummer = ?",
            new String[]{String.valueOf(platineNummer)},
            null, null, null);
        
        try {
            if (cursor.moveToFirst()) {
                Platine platine = new Platine();
                platine.id = cursor.getInt(0);
                platine.platineNummer = cursor.getInt(1);
                platine.ipAdresse = cursor.isNull(2) ? null : cursor.getString(2);
                platine.port = cursor.isNull(3) ? 50210 : cursor.getInt(3);
                platine.aktiv = cursor.getInt(4) == 1;
                platine.macAdresse = cursor.isNull(5) ? null : cursor.getString(5);
                platine.online = cursor.getInt(6) == 1;
                int ra = cursor.isNull(7) ? 32 : cursor.getInt(7);
                platine.relaisAnzahl = (ra <= 0) ? 32 : ra;
                platine.impulsAntwortErwartet = cursor.getColumnCount() > 8 && !cursor.isNull(8) ? cursor.getString(8) : null;
                return platine;
            }
        } finally {
            cursor.close();
        }
        
        // Platine nicht gefunden, erstelle leere Platine
        Platine platine = new Platine();
        platine.platineNummer = platineNummer;
        platine.ipAdresse = null;
        platine.port = 50210;
        platine.aktiv = false;
        platine.macAdresse = null;
        platine.online = false;
        platine.relaisAnzahl = 32;
        return platine;
    }
    
    /**
     * Speichert eine Platine (INSERT oder UPDATE).
     */
    public void savePlatine(Platine platine) {
        SQLiteDatabase db = getWritableDatabase();
        
        // Setze aktiv basierend auf IP
        platine.aktiv = (platine.ipAdresse != null && 
                        !platine.ipAdresse.equals("0") && 
                        !platine.ipAdresse.trim().isEmpty());
        
        // Prüfe ob Platine bereits existiert
        Platine existing = getPlatine(platine.platineNummer);
        
        if (existing.id > 0) {
            // UPDATE
            android.content.ContentValues values = new android.content.ContentValues();
            values.put("ip_adresse", platine.ipAdresse);
            values.put("port", platine.port);
            values.put("aktiv", platine.aktiv ? 1 : 0);
            values.put("mac_adresse", platine.macAdresse);
            values.put("online", platine.online ? 1 : 0);
            values.put("relais_anzahl", platine.relaisAnzahl > 0 ? platine.relaisAnzahl : 32);
            values.put("impuls_antwort_erwartet", platine.impulsAntwortErwartet);
            values.put("updated_at", "CURRENT_TIMESTAMP");
            
            db.update(TABLE_PLATINEN, values, "platine_nummer = ?", 
                new String[]{String.valueOf(platine.platineNummer)});
            Log.d(TAG, "Platine " + platine.platineNummer + " aktualisiert");
        } else {
            // INSERT
            android.content.ContentValues values = new android.content.ContentValues();
            values.put("platine_nummer", platine.platineNummer);
            values.put("ip_adresse", platine.ipAdresse);
            values.put("port", platine.port);
            values.put("aktiv", platine.aktiv ? 1 : 0);
            values.put("mac_adresse", platine.macAdresse);
            values.put("online", platine.online ? 1 : 0);
            values.put("relais_anzahl", platine.relaisAnzahl > 0 ? platine.relaisAnzahl : 32);
            values.put("impuls_antwort_erwartet", platine.impulsAntwortErwartet);
            
            db.insert(TABLE_PLATINEN, null, values);
            Log.d(TAG, "Platine " + platine.platineNummer + " erstellt");
        }
    }
    
    /**
     * Löscht eine Platine (setzt IP auf NULL, aktiv auf false).
     */
    public void deletePlatine(int platineNummer) {
        SQLiteDatabase db = getWritableDatabase();
        
        android.content.ContentValues values = new android.content.ContentValues();
        values.put("ip_adresse", (String) null);
        values.put("port", (Integer) null);
        values.put("aktiv", 0);
        values.put("mac_adresse", (String) null);
        values.put("online", 0);
        values.put("updated_at", "CURRENT_TIMESTAMP");
        
        db.update(TABLE_PLATINEN, values, "platine_nummer = ?", 
            new String[]{String.valueOf(platineNummer)});
        Log.d(TAG, "Platine " + platineNummer + " gelöscht (deaktiviert)");
    }
    
    /**
     * Migriert Daten aus System.xls Sheet 14 in die Datenbank.
     */
    private void migrateFromExcel(SQLiteDatabase db) {
        try {
            String sdCardPath = android.os.Environment.getExternalStorageDirectory().getPath();
            String systemXlsPath = sdCardPath + StaticConstants.excellSystemString;
            java.io.File systemFile = new java.io.File(systemXlsPath);
            
            if (!systemFile.exists()) {
                Log.d(TAG, "System.xls nicht gefunden, überspringe Migration");
                return;
            }
            
            ExcelRead excelread = new ExcelRead();
            excelread.openXlsSheet(systemXlsPath, 14);
            
            Log.d(TAG, "Migriere Platinen-Daten aus System.xls Sheet 14");
            
            // Modus aus Excel lesen
            try {
                String bluetemp = excelread.getCellString(0, 1);
                String modus = (bluetemp != null && bluetemp.equals("AUS")) ? "bluetooth" : "wifi";
                
                // Prüfe ob Modus bereits existiert (könnte schon in onCreate erstellt worden sein)
                Cursor modusCursor = db.query(TABLE_IO_CONFIG, new String[]{"id"}, "key = ?", new String[]{"modus"}, null, null, null);
                boolean modusExists = modusCursor.moveToFirst();
                modusCursor.close();
                
                if (modusExists) {
                    // UPDATE
                    android.content.ContentValues modusValues = new android.content.ContentValues();
                    modusValues.put("value", modus);
                    modusValues.put("updated_at", "CURRENT_TIMESTAMP");
                    db.update(TABLE_IO_CONFIG, modusValues, "key = ?", new String[]{"modus"});
                } else {
                    // INSERT
                    android.content.ContentValues modusValues = new android.content.ContentValues();
                    modusValues.put("key", "modus");
                    modusValues.put("value", modus);
                    db.insert(TABLE_IO_CONFIG, null, modusValues);
                }
                Log.d(TAG, "Modus migriert: " + modus);
            } catch (Exception e) {
                Log.w(TAG, "Fehler beim Migrieren des Modus", e);
            }
            
            // Power off Akku Prozent aus Excel lesen
            try {
                String akkuProzentTemp = excelread.getCellString(1, 3);
                if (akkuProzentTemp != null && !akkuProzentTemp.trim().isEmpty()) {
                    try {
                        int akkuProzent = Integer.parseInt(akkuProzentTemp.trim());
                        // Prüfe ob Wert bereits existiert
                        Cursor akkuCursor = db.query(TABLE_IO_CONFIG, new String[]{"id"}, "key = ?", new String[]{"power_off_akku_prozent"}, null, null, null);
                        boolean akkuExists = akkuCursor.moveToFirst();
                        akkuCursor.close();
                        
                        if (akkuExists) {
                            // UPDATE
                            android.content.ContentValues akkuValues = new android.content.ContentValues();
                            akkuValues.put("value", String.valueOf(akkuProzent));
                            akkuValues.put("updated_at", "CURRENT_TIMESTAMP");
                            db.update(TABLE_IO_CONFIG, akkuValues, "key = ?", new String[]{"power_off_akku_prozent"});
                        } else {
                            // INSERT
                            android.content.ContentValues akkuValues = new android.content.ContentValues();
                            akkuValues.put("key", "power_off_akku_prozent");
                            akkuValues.put("value", String.valueOf(akkuProzent));
                            db.insert(TABLE_IO_CONFIG, null, akkuValues);
                        }
                        Log.d(TAG, "Power off Akku Prozent migriert: " + akkuProzent + "%");
                    } catch (NumberFormatException e) {
                        Log.w(TAG, "Ungültiger Power off Akku Prozent Wert: " + akkuProzentTemp, e);
                    }
                }
            } catch (Exception e) {
                Log.w(TAG, "Fehler beim Migrieren des Power off Akku Prozent", e);
            }
            
            // Platinen aus Excel lesen
            for (int i = 1; i <= 4; i++) {
                try {
                    String ip = excelread.getCellString(i, 1);
                    String portStr = excelread.getCellString(i + 4, 1);
                    
                    if (ip != null && !ip.equals("0") && !ip.trim().isEmpty()) {
                        Platine platine = new Platine();
                        platine.platineNummer = i;
                        platine.ipAdresse = ip.trim();
                        
                        if (portStr != null && !portStr.trim().isEmpty()) {
                            try {
                                platine.port = Integer.parseInt(portStr.trim());
                            } catch (NumberFormatException e) {
                                platine.port = 50210;  // Default
                            }
                        } else {
                            platine.port = 50210;  // Default
                        }
                        
                        platine.aktiv = true;
                        
                        // In Datenbank speichern
                        android.content.ContentValues values = new android.content.ContentValues();
                        values.put("platine_nummer", platine.platineNummer);
                        values.put("ip_adresse", platine.ipAdresse);
                        values.put("port", platine.port);
                        values.put("aktiv", platine.aktiv ? 1 : 0);
                        
                        db.insert(TABLE_PLATINEN, null, values);
                        Log.d(TAG, "Platine " + i + " migriert: " + platine.ipAdresse + ":" + platine.port);
                    }
                } catch (Exception e) {
                    Log.w(TAG, "Fehler beim Migrieren von Platine " + i, e);
                }
            }
            
            excelread.closeWorkbook();
            
            // Migriere auch Nebenuhr-Daten aus Excel (falls Tabelle existiert)
            try {
                migrateNebenuhrenFromExcel(db);
            } catch (Exception e) {
                Log.w(TAG, "Fehler bei Migration von Nebenuhr-Daten", e);
            }
            
            // Migriere Osterfeiertage-Tagtypen aus Excel (falls Tabelle existiert)
            try {
                migrateOsterfeiertageFromExcel(db);
            } catch (Exception e) {
                Log.w(TAG, "Fehler bei Migration von Osterfeiertage-Daten", e);
            }
            
            // Migriere Feste Feiertage aus Excel (falls Tabelle existiert)
            try {
                migrateFesteFeiertageFromExcel(db);
            } catch (Exception e) {
                Log.w(TAG, "Fehler bei Migration von Feste-Feiertage-Daten", e);
            }
            
            Log.d(TAG, "Migration abgeschlossen");
            
        } catch (Exception e) {
            Log.e(TAG, "Fehler bei Migration von Excel", e);
        }
    }
    
    /**
     * Führt einen vollständigen Import aller Excel-Daten in die Datenbank durch.
     * Wird z. B. beim App-Start auf Bestätigung des Benutzers ausgeführt.
     * Liest System.xls, Programmtage/*.xls, Osterfeiertage, Feste Feiertage, Vorschwingen, Nebenuhren.
     */
    public void runFullExcelImport() {
        SQLiteDatabase db = getWritableDatabase();
        try {
            Log.d(TAG, "Vollständiger Excel-Import wird ausgeführt");
            migrateFromExcel(db);
            migrateTagtypenAndProgrammeFromExcel(db);
            migrateVorschwingenFromExcel(db);
            migrateOsterfeiertageFromExcel(db, true);
            migrateFesteFeiertageFromExcel(db, true);
            Log.d(TAG, "Vollständiger Excel-Import abgeschlossen");
        } catch (Exception e) {
            Log.e(TAG, "Fehler beim vollständigen Excel-Import", e);
        }
    }
    
    /**
     * Importiert nur System.xls (Sheet 14: IO/Platinen/Akku, Sheet 15: Nebenuhr)
     * sowie Osterfeiertage und Feste Feiertage in die DB. Für manuellen Import von alter Anlage.
     */
    public void runSystemExcelImport() {
        SQLiteDatabase db = getWritableDatabase();
        migrateFromExcel(db);
    }
    
    /**
     * Importiert Tagtypen und Programme aus Excel-Dateien im Ordner Programmtage.
     * Für manuellen Import von alter Anlage.
     */
    public void runTagtypenProgrammeExcelImport() {
        SQLiteDatabase db = getWritableDatabase();
        migrateTagtypenAndProgrammeFromExcel(db);
    }
    
    /**
     * Lädt den aktuellen Modus (bluetooth oder wifi).
     */
    public String getModus() {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.query(TABLE_IO_CONFIG,
            new String[]{"value"},
            "key = ?",
            new String[]{"modus"},
            null, null, null);
        
        try {
            if (cursor.moveToFirst()) {
                return cursor.getString(0);
            }
        } finally {
            cursor.close();
        }
        
        // Default: wifi
        return "wifi";
    }
    
    /**
     * Setzt den Modus (bluetooth oder wifi).
     */
    public void setModus(String modus) {
        SQLiteDatabase db = getWritableDatabase();
        
        android.content.ContentValues values = new android.content.ContentValues();
        values.put("value", modus);
        values.put("updated_at", "CURRENT_TIMESTAMP");
        
        int updated = db.update(TABLE_IO_CONFIG, values, "key = ?", new String[]{"modus"});
        
        if (updated == 0) {
            // Eintrag existiert nicht, erstelle ihn
            values.put("key", "modus");
            db.insert(TABLE_IO_CONFIG, null, values);
        }
        
        Log.d(TAG, "Modus gesetzt: " + modus);
    }

    /**
     * Liest den Platinen-Demo-Modus aus io_config (key: platinen_demo_modus).
     * 1 / ein = Demo (keine echte Hardware), 0 / aus = normal.
     */
    public boolean getPlatinenDemoModus() {
        String v = getConfigValue("platinen_demo_modus");
        if (v == null || v.trim().isEmpty()) return false;
        v = v.trim().toLowerCase();
        return "1".equals(v) || "ein".equals(v) || "true".equals(v);
    }

    /**
     * Setzt den Platinen-Demo-Modus in io_config.
     */
    public void setPlatinenDemoModus(boolean demo) {
        setConfigValue("platinen_demo_modus", demo ? "1" : "0");
        Log.d(TAG, "Platinen-Demo-Modus gesetzt: " + demo);
    }
    
    /**
     * Lädt den Scan-Relais-Wert (ms) aus io_config.
     * @return Wert in ms, oder -1 wenn nicht gesetzt (dann Excel/Default nutzen).
     */
    public int getScanRelaisMS() {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.query(TABLE_IO_CONFIG,
            new String[]{"value"},
            "key = ?",
            new String[]{"scan_relais_ms"},
            null, null, null);
        try {
            if (cursor.moveToFirst()) {
                String valueStr = cursor.getString(0);
                if (valueStr != null && !valueStr.trim().isEmpty()) {
                    try {
                        return Integer.parseInt(valueStr.trim());
                    } catch (NumberFormatException e) {
                        Log.w(TAG, "Ungültiger scan_relais_ms in DB: " + valueStr);
                    }
                }
            }
        } finally {
            cursor.close();
        }
        return -1;
    }

    /**
     * Speichert den Scan-Relais-Wert (ms) in io_config.
     */
    public void setScanRelaisMS(int ms) {
        SQLiteDatabase db = getWritableDatabase();
        android.content.ContentValues values = new android.content.ContentValues();
        values.put("key", "scan_relais_ms");
        values.put("value", String.valueOf(ms));
        values.put("updated_at", "CURRENT_TIMESTAMP");
        int updated = db.update(TABLE_IO_CONFIG, values, "key = ?", new String[]{"scan_relais_ms"});
        if (updated == 0) {
            db.insert(TABLE_IO_CONFIG, null, values);
        }
        Log.d(TAG, "scan_relais_ms gesetzt: " + ms);
    }

    /**
     * Lädt den Power off Akku Prozent Wert.
     */
    public int getPowerOffAkkuProzent() {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.query(TABLE_IO_CONFIG,
            new String[]{"value"},
            "key = ?",
            new String[]{"power_off_akku_prozent"},
            null, null, null);
        
        try {
            if (cursor.moveToFirst()) {
                String valueStr = cursor.getString(0);
                try {
                    return Integer.parseInt(valueStr);
                } catch (NumberFormatException e) {
                    Log.w(TAG, "Ungültiger Power off Akku Prozent Wert in DB: " + valueStr);
                }
            }
        } finally {
            cursor.close();
        }
        
        // Default: 75%
        return 75;
    }

    /**
     * Lädt den Autostart-der-App Wert aus io_config (key "autostart_der_app").
     * Wert "ein" oder "1" = true, sonst false. Null wenn Eintrag nicht vorhanden.
     */
    public Boolean getAutostartDerApp() {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.query(TABLE_IO_CONFIG,
            new String[]{"value"},
            "key = ?",
            new String[]{"autostart_der_app"},
            null, null, null);
        try {
            if (cursor.moveToFirst()) {
                String value = cursor.getString(0);
                if (value == null) return null;
                String v = value.trim().toLowerCase();
                return "ein".equals(v) || "1".equals(v) || "true".equals(v) || "ja".equals(v);
            }
        } finally {
            cursor.close();
        }
        return null;
    }
    
    /**
     * Setzt den Power off Akku Prozent Wert.
     */
    public void setPowerOffAkkuProzent(int prozent) {
        SQLiteDatabase db = getWritableDatabase();
        
        android.content.ContentValues values = new android.content.ContentValues();
        values.put("value", String.valueOf(prozent));
        values.put("updated_at", "CURRENT_TIMESTAMP");
        
        int updated = db.update(TABLE_IO_CONFIG, values, "key = ?", new String[]{"power_off_akku_prozent"});
        
        if (updated == 0) {
            // Eintrag existiert nicht, erstelle ihn
            values.put("key", "power_off_akku_prozent");
            db.insert(TABLE_IO_CONFIG, null, values);
        }
        
        Log.d(TAG, "Power off Akku Prozent gesetzt: " + prozent + "%");
    }
    
    /**
     * Lädt einen beliebigen io_config-Wert (key-value).
     * @return value oder null wenn key nicht existiert
     */
    public String getConfigValue(String key) {
        if (key == null || key.trim().isEmpty()) return null;
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.query(TABLE_IO_CONFIG,
            new String[]{"value"},
            "key = ?",
            new String[]{key.trim()},
            null, null, null);
        try {
            if (cursor.moveToFirst()) {
                return cursor.getString(0);
            }
        } finally {
            cursor.close();
        }
        return null;
    }
    
    /**
     * Speichert einen beliebigen io_config-Wert (key-value).
     * Überschreibt vorhandenen Eintrag.
     */
    public void setConfigValue(String key, String value) {
        if (key == null || key.trim().isEmpty()) return;
        SQLiteDatabase db = getWritableDatabase();
        android.content.ContentValues values = new android.content.ContentValues();
        values.put("key", key.trim());
        values.put("value", value != null ? value : "");
        String timestamp = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.US).format(new java.util.Date());
        values.put("updated_at", timestamp);
        int updated = db.update(TABLE_IO_CONFIG, values, "key = ?", new String[]{key.trim()});
        if (updated == 0) {
            db.insert(TABLE_IO_CONFIG, null, values);
        }
    }

    private static final String KEY_UEBERSETZUNG_PREFIX = "uebersetzung_";
    private static final String KEY_UEBERSETZUNG_COUNT = "uebersetzung_count";

    private static String getUebersetzungKey(int index) {
        return String.format(java.util.Locale.US, "%s%05d", KEY_UEBERSETZUNG_PREFIX, index);
    }

    public List<String> getAlleUebersetzungen() {
        List<String> texte = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.query(TABLE_IO_CONFIG,
            new String[]{"key", "value"},
            "key LIKE ?",
            new String[]{KEY_UEBERSETZUNG_PREFIX + "%"},
            null, null,
            "key ASC");
        try {
            while (cursor.moveToNext()) {
                texte.add(cursor.isNull(1) ? "" : cursor.getString(1));
            }
        } finally {
            cursor.close();
        }
        return texte;
    }

    public void saveUebersetzungen(List<String> texte) {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            db.delete(TABLE_IO_CONFIG, "key LIKE ? OR key = ?", new String[]{KEY_UEBERSETZUNG_PREFIX + "%", KEY_UEBERSETZUNG_COUNT});

            if (texte != null) {
                String timestamp = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.US).format(new java.util.Date());
                for (int i = 0; i < texte.size(); i++) {
                    android.content.ContentValues values = new android.content.ContentValues();
                    values.put("key", getUebersetzungKey(i));
                    values.put("value", texte.get(i) != null ? texte.get(i) : "");
                    values.put("updated_at", timestamp);
                    db.insert(TABLE_IO_CONFIG, null, values);
                }
                android.content.ContentValues countValues = new android.content.ContentValues();
                countValues.put("key", KEY_UEBERSETZUNG_COUNT);
                countValues.put("value", String.valueOf(texte.size()));
                countValues.put("updated_at", timestamp);
                db.insert(TABLE_IO_CONFIG, null, countValues);
            }

            db.setTransactionSuccessful();
            Log.d(TAG, "Übersetzungen gespeichert: " + (texte != null ? texte.size() : 0));
        } finally {
            db.endTransaction();
        }
    }

    public int importSprachdateiFromExcel(String excelPath) {
        if (excelPath == null || excelPath.trim().isEmpty()) return 0;
        ExcelRead excelread = new ExcelRead();
        try {
            excelread.openXls(excelPath.trim());
        } catch (Exception e) {
            Log.e(TAG, "Sprachdatei Excel öffnen fehlgeschlagen: " + excelPath, e);
            return 0;
        }

        try {
            int zeilenAnzahl = excelread.getCellZeilen();
            List<String> texte = new ArrayList<>();
            for (int i = 0; i < zeilenAnzahl - 2 && (i + 2) < zeilenAnzahl; i++) {
                String text = excelread.getCellString(1, i + 2);
                texte.add(text != null ? text : "");
            }
            if (texte.isEmpty()) {
                Log.w(TAG, "Sprachdatei Excel ohne importierbare Texte: " + excelPath);
                return 0;
            }
            saveUebersetzungen(texte);
            Log.d(TAG, "Sprachdatei aus Excel importiert: " + texte.size() + " Texte");
            return texte.size();
        } catch (jxl.read.biff.BiffException e) {
            Log.e(TAG, "BiffException beim Importieren der Sprachdatei: " + excelPath, e);
            return 0;
        } catch (java.io.IOException e) {
            Log.e(TAG, "IOException beim Importieren der Sprachdatei: " + excelPath, e);
            return 0;
        } catch (Exception e) {
            Log.e(TAG, "Fehler beim Importieren der Sprachdatei: " + excelPath, e);
            return 0;
        } finally {
            try {
                excelread.closeWorkbook();
            } catch (Exception e) {
                Log.w(TAG, "Sprachdatei closeWorkbook", e);
            }
        }
    }

    public static final String KEY_MOND_IMPULSE_PRO_PHASE = "mond_impulse_pro_phase";
    public static final int DEFAULT_MOND_IMPULSE_PRO_PHASE = 60;
    public static final String KEY_LAYOUT_GRID_COLS = "layout_grid_cols";
    public static final String KEY_LAYOUT_GRID_ROWS = "layout_grid_rows";
    public static final int DEFAULT_LAYOUT_GRID_COLS = 6;
    public static final int DEFAULT_LAYOUT_GRID_ROWS = 8;

    public int getMondImpulseProPhase() {
        String value = getConfigValue(KEY_MOND_IMPULSE_PRO_PHASE);
        if (value != null && !value.trim().isEmpty()) {
            try {
                return normalizeMondImpulseProPhase(Integer.parseInt(value.trim()));
            } catch (NumberFormatException e) {
                Log.w(TAG, "Ungültiger Wert für mond_impulse_pro_phase: " + value, e);
            }
        }
        return DEFAULT_MOND_IMPULSE_PRO_PHASE;
    }

    public void setMondImpulseProPhase(int impulseProPhase) {
        setConfigValue(KEY_MOND_IMPULSE_PRO_PHASE, String.valueOf(normalizeMondImpulseProPhase(impulseProPhase)));
    }

    public int getLayoutGridCols() {
        String value = getConfigValue(KEY_LAYOUT_GRID_COLS);
        if (value != null && !value.trim().isEmpty()) {
            try {
                return normalizeLayoutGridCols(Integer.parseInt(value.trim()));
            } catch (NumberFormatException e) {
                Log.w(TAG, "Ungueltiger Wert fuer layout_grid_cols: " + value, e);
            }
        }
        return DEFAULT_LAYOUT_GRID_COLS;
    }

    private int getLayoutGridCols(SQLiteDatabase db) {
        String value = getConfigValue(db, KEY_LAYOUT_GRID_COLS);
        if (value != null && !value.trim().isEmpty()) {
            try {
                return normalizeLayoutGridCols(Integer.parseInt(value.trim()));
            } catch (NumberFormatException e) {
                Log.w(TAG, "Ungueltiger Wert fuer layout_grid_cols: " + value, e);
            }
        }
        return DEFAULT_LAYOUT_GRID_COLS;
    }

    public void setLayoutGridCols(int cols) {
        setConfigValue(KEY_LAYOUT_GRID_COLS, String.valueOf(normalizeLayoutGridCols(cols)));
    }

    public int getLayoutGridRows() {
        String value = getConfigValue(KEY_LAYOUT_GRID_ROWS);
        if (value != null && !value.trim().isEmpty()) {
            try {
                return normalizeLayoutGridRows(Integer.parseInt(value.trim()));
            } catch (NumberFormatException e) {
                Log.w(TAG, "Ungueltiger Wert fuer layout_grid_rows: " + value, e);
            }
        }
        return DEFAULT_LAYOUT_GRID_ROWS;
    }

    public void setLayoutGridRows(int rows) {
        setConfigValue(KEY_LAYOUT_GRID_ROWS, String.valueOf(normalizeLayoutGridRows(rows)));
    }

    private String getConfigValue(SQLiteDatabase db, String key) {
        if (db == null || key == null || key.trim().isEmpty()) return null;
        Cursor cursor = db.query(TABLE_IO_CONFIG,
            new String[]{"value"},
            "key = ?",
            new String[]{key.trim()},
            null, null, null);
        try {
            if (cursor.moveToFirst()) {
                return cursor.getString(0);
            }
        } finally {
            cursor.close();
        }
        return null;
    }

    public static int normalizeMondImpulseProPhase(int impulseProPhase) {
        if (impulseProPhase < 4) return 4;
        if (impulseProPhase > 360) return 360;
        return impulseProPhase;
    }

    public static int normalizeMondphase(int mondphase, int impulseProPhase) {
        int maxImpulse = normalizeMondImpulseProPhase(impulseProPhase);
        int normalized = mondphase % maxImpulse;
        if (normalized < 0) normalized += maxImpulse;
        return normalized;
    }

    public static int normalizeLayoutGridCols(int cols) {
        if (cols < 2) return 2;
        if (cols > 10) return 10;
        return cols;
    }

    public static int normalizeLayoutGridRows(int rows) {
        if (rows < 1) return 1;
        if (rows > 24) return 24;
        return rows;
    }

    /** Max. Anzahl Benutzerprogramm-Slots (wie ConfigWebServer / TurmtechnikActivity). */
    private static final int BENUTZERPROGRAMME_MAX = 20;

    /**
     * Entfernt Benutzerprogramme, deren Datum mehr als 2 Tage in der Vergangenheit liegt.
     * Wird beim Laden der Benutzerprogramme (App oder Web-UI) aufgerufen.
     */
    public void cleanupAlteBenutzerprogramme() {
        java.util.Calendar heute = java.util.Calendar.getInstance();
        heute.set(java.util.Calendar.HOUR_OF_DAY, 0);
        heute.set(java.util.Calendar.MINUTE, 0);
        heute.set(java.util.Calendar.SECOND, 0);
        heute.set(java.util.Calendar.MILLISECOND, 0);
        java.util.Calendar grenze = (java.util.Calendar) heute.clone();
        grenze.add(java.util.Calendar.DAY_OF_MONTH, -2);
        for (int i = 0; i < BENUTZERPROGRAMME_MAX; i++) {
            String on = getConfigValue("benutzer_" + i + "_on");
            if (!"1".equals(on) && !"true".equalsIgnoreCase(on != null ? on : "")) continue;
            String tagS = getConfigValue("benutzer_" + i + "_tag");
            String monatS = getConfigValue("benutzer_" + i + "_monat");
            String jahrS = getConfigValue("benutzer_" + i + "_jahr");
            if (tagS == null || tagS.isEmpty() || monatS == null || monatS.isEmpty() || jahrS == null || jahrS.isEmpty()) continue;
            int tag, monat, jahr;
            try {
                tag = Integer.parseInt(tagS);
                monat = Integer.parseInt(monatS);
                jahr = Integer.parseInt(jahrS);
                if (monat < 1 || monat > 12 || tag < 1 || tag > 31) continue;
            } catch (NumberFormatException e) { continue; }
            java.util.Calendar progDatum = java.util.Calendar.getInstance();
            progDatum.set(jahr, monat - 1, tag, 0, 0, 0);
            progDatum.set(java.util.Calendar.MILLISECOND, 0);
            // Löschen wenn Datum am Grenztag (heute − 2) oder davor liegt (also 2+ Tage her)
            if (!progDatum.after(grenze)) {
                String key = "benutzer_" + i + "_";
                setConfigValue(key + "on", "0");
                setConfigValue(key + "tag", "1");
                setConfigValue(key + "monat", "1");
                setConfigValue(key + "jahr", String.valueOf(heute.get(java.util.Calendar.YEAR)));
                setConfigValue(key + "stunde", "12");
                setConfigValue(key + "minute", "0");
                setConfigValue(key + "melodie", "");
                setConfigValue(key + "tagtyp", "");
                Log.d(TAG, "Benutzerprogramm Slot " + i + " gelöscht (Datum " + tag + "." + monat + "." + jahr + " älter als 2 Tage).");
            }
        }
    }

    /** Keys für Fernsteuern-Konfiguration (ehemals Fernsteuern-Speicher-Ort.xls). */
    public static final String KEY_FERNSTEUERN_INTERNET_POLLING_MS = "fernsteuern_internet_polling_ms";
    public static final String KEY_FERNSTEUERN_TIME_SERVER_EIN_AUS = "fernsteuern_time_server_ein_aus";
    public static final String KEY_FERNSTEUERN_TIME_SERVER_IP = "fernsteuern_time_server_ip";
    public static final String KEY_FERNSTEUERN_TIME_SERVER_MAX_OFFSET_MINUTEN = "fernsteuern_time_server_max_offset_minuten";
    public static final String KEY_FERNSTEUERN_TIME_SERVER_ABFRAGE_INTERVALL_MS = "fernsteuern_time_server_abfrage_intervall_ms";
    public static final String KEY_FERNSTEUERN_SERIAL_GPS_EIN_AUS = "fernsteuern_serial_gps_ein_aus";
    public static final String KEY_FERNSTEUERN_SERIAL_GPS_IP = "fernsteuern_serial_gps_ip";
    public static final String KEY_FERNSTEUERN_SERIAL_GPS_PORT = "fernsteuern_serial_gps_port";

    /** Default: Internet-Polling 5 Minuten in ms. */
    public static final int DEFAULT_FERNSTEUERN_INTERNET_POLLING_MS = 60000 * 5;

    /**
     * Konfiguration Fernsteuern (ehemals aus Fernsteuern-Speicher-Ort.xls, jetzt io_config).
     */
    public static class FernsteuernConfig {
        public int internetPollingMs = DEFAULT_FERNSTEUERN_INTERNET_POLLING_MS;
        public String timeServerEinAus = "EIN";
        public String timeServerIp = "";
        public String timeServerMaxOffsetMinuten = "";
        public String timeServerAbfrageIntervallMs = "";
        public boolean serialGPSOnOff = false;
        public String serialGPSIp = "";
        public int serialGPSPort = 0;
    }

    /**
     * Lädt die Fernsteuern-Konfiguration aus der Datenbank (io_config).
     * Fehlende Werte = Defaults. Null wenn noch kein Eintrag existiert (dann Excel-Migration möglich).
     */
    public FernsteuernConfig getFernsteuernConfig() {
        FernsteuernConfig c = new FernsteuernConfig();
        String v = getConfigValue(KEY_FERNSTEUERN_INTERNET_POLLING_MS);
        if (v != null && !v.trim().isEmpty()) {
            try { c.internetPollingMs = Integer.parseInt(v.trim()); } catch (NumberFormatException e) { }
        }
        v = getConfigValue(KEY_FERNSTEUERN_TIME_SERVER_EIN_AUS);
        if (v != null) c.timeServerEinAus = v.trim();
        v = getConfigValue(KEY_FERNSTEUERN_TIME_SERVER_IP);
        if (v != null) c.timeServerIp = v.trim();
        v = getConfigValue(KEY_FERNSTEUERN_TIME_SERVER_MAX_OFFSET_MINUTEN);
        if (v != null) c.timeServerMaxOffsetMinuten = v.trim();
        v = getConfigValue(KEY_FERNSTEUERN_TIME_SERVER_ABFRAGE_INTERVALL_MS);
        if (v != null) c.timeServerAbfrageIntervallMs = v.trim();
        v = getConfigValue(KEY_FERNSTEUERN_SERIAL_GPS_EIN_AUS);
        c.serialGPSOnOff = v != null && "EIN".equals(v.trim());
        v = getConfigValue(KEY_FERNSTEUERN_SERIAL_GPS_IP);
        if (v != null) c.serialGPSIp = v.trim();
        v = getConfigValue(KEY_FERNSTEUERN_SERIAL_GPS_PORT);
        if (v != null && !v.trim().isEmpty()) {
            try { c.serialGPSPort = Integer.parseInt(v.trim()); } catch (NumberFormatException e) { }
        }
        return c;
    }

    /**
     * Speichert die Fernsteuern-Konfiguration in der Datenbank (io_config).
     */
    public void saveFernsteuernConfig(FernsteuernConfig c) {
        if (c == null) return;
        setConfigValue(KEY_FERNSTEUERN_INTERNET_POLLING_MS, String.valueOf(c.internetPollingMs));
        setConfigValue(KEY_FERNSTEUERN_TIME_SERVER_EIN_AUS, c.timeServerEinAus != null ? c.timeServerEinAus : "");
        setConfigValue(KEY_FERNSTEUERN_TIME_SERVER_IP, c.timeServerIp != null ? c.timeServerIp : "");
        setConfigValue(KEY_FERNSTEUERN_TIME_SERVER_MAX_OFFSET_MINUTEN, c.timeServerMaxOffsetMinuten != null ? c.timeServerMaxOffsetMinuten : "");
        setConfigValue(KEY_FERNSTEUERN_TIME_SERVER_ABFRAGE_INTERVALL_MS, c.timeServerAbfrageIntervallMs != null ? c.timeServerAbfrageIntervallMs : "");
        setConfigValue(KEY_FERNSTEUERN_SERIAL_GPS_EIN_AUS, c.serialGPSOnOff ? "EIN" : "AUS");
        setConfigValue(KEY_FERNSTEUERN_SERIAL_GPS_IP, c.serialGPSIp != null ? c.serialGPSIp : "");
        setConfigValue(KEY_FERNSTEUERN_SERIAL_GPS_PORT, String.valueOf(c.serialGPSPort));
    }

    /**
     * Prüft, ob mindestens ein Fernsteuern-Wert in der DB steht (dann keine Excel-Migration nötig).
     */
    public boolean hasFernsteuernConfigInDb() {
        return getConfigValue(KEY_FERNSTEUERN_INTERNET_POLLING_MS) != null;
    }

    /** Keys für Anlagenstandort (ehemals Anlagenstandort.xls, Sonnenauf-/untergang). */
    public static final String KEY_ANLAGENSTANDORT_BREITENGRAD = "anlagenstandort_breitengrad";
    public static final String KEY_ANLAGENSTANDORT_LAENGENGRAD = "anlagenstandort_laengengrad";
    public static final String KEY_ANLAGENSTANDORT_ANPASSUNG_SA_MIN = "anlagenstandort_anpassung_sonnenaufgang_min";
    public static final String KEY_ANLAGENSTANDORT_ANPASSUNG_SU_MIN = "anlagenstandort_anpassung_sonnenuntergang_min";
    public static final String KEY_ANLAGENSTANDORT_RUNDEN_MIN = "anlagenstandort_runden_min";
    public static final String KEY_ANLAGENSTANDORT_ORT = "anlagenstandort_ort";
    public static final String KEY_ANLAGENSTANDORT_PLZ = "anlagenstandort_plz";

    /**
     * Konfiguration Anlagenstandort (ehemals Anlagenstandort.xls, Sonnenauf-/untergang).
     */
    public static class AnlagenstandortConfig {
        public String breitengrad = "";
        public String laengengrad = "";
        public String ort = "";
        public String plz = "";
        public String anpassungSonnenaufgangMinuten = "0";
        public String anpassungSonnenuntergangMinuten = "0";
        public String rundenMinuten = "5";
    }

    /**
     * Lädt die Anlagenstandort-Konfiguration aus der Datenbank (io_config).
     */
    public AnlagenstandortConfig getAnlagenstandortConfig() {
        AnlagenstandortConfig c = new AnlagenstandortConfig();
        String v = getConfigValue(KEY_ANLAGENSTANDORT_BREITENGRAD);
        if (v != null) c.breitengrad = v.trim();
        v = getConfigValue(KEY_ANLAGENSTANDORT_LAENGENGRAD);
        if (v != null) c.laengengrad = v.trim();
        v = getConfigValue(KEY_ANLAGENSTANDORT_ORT);
        if (v != null) c.ort = v.trim();
        v = getConfigValue(KEY_ANLAGENSTANDORT_PLZ);
        if (v != null) c.plz = v.trim();
        v = getConfigValue(KEY_ANLAGENSTANDORT_ANPASSUNG_SA_MIN);
        if (v != null) c.anpassungSonnenaufgangMinuten = v.trim();
        v = getConfigValue(KEY_ANLAGENSTANDORT_ANPASSUNG_SU_MIN);
        if (v != null) c.anpassungSonnenuntergangMinuten = v.trim();
        v = getConfigValue(KEY_ANLAGENSTANDORT_RUNDEN_MIN);
        if (v != null) c.rundenMinuten = v.trim();
        return c;
    }

    /**
     * Speichert die Anlagenstandort-Konfiguration in der Datenbank (io_config).
     */
    public void saveAnlagenstandortConfig(AnlagenstandortConfig c) {
        if (c == null) return;
        setConfigValue(KEY_ANLAGENSTANDORT_BREITENGRAD, c.breitengrad != null ? c.breitengrad : "");
        setConfigValue(KEY_ANLAGENSTANDORT_LAENGENGRAD, c.laengengrad != null ? c.laengengrad : "");
        setConfigValue(KEY_ANLAGENSTANDORT_ORT, c.ort != null ? c.ort : "");
        setConfigValue(KEY_ANLAGENSTANDORT_PLZ, c.plz != null ? c.plz : "");
        setConfigValue(KEY_ANLAGENSTANDORT_ANPASSUNG_SA_MIN, c.anpassungSonnenaufgangMinuten != null ? c.anpassungSonnenaufgangMinuten : "0");
        setConfigValue(KEY_ANLAGENSTANDORT_ANPASSUNG_SU_MIN, c.anpassungSonnenuntergangMinuten != null ? c.anpassungSonnenuntergangMinuten : "0");
        setConfigValue(KEY_ANLAGENSTANDORT_RUNDEN_MIN, c.rundenMinuten != null ? c.rundenMinuten : "5");
    }

    /**
     * Prüft, ob mindestens ein Anlagenstandort-Wert in der DB steht.
     */
    public boolean hasAnlagenstandortConfigInDb() {
        return getConfigValue(KEY_ANLAGENSTANDORT_BREITENGRAD) != null;
    }
    
    /**
     * Lädt alle Nebenuhr-Konfigurationen aus der Datenbank.
     */
    public List<NebenuhrConfig> getAllNebenuhren() {
        List<NebenuhrConfig> nebenuhren = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        
        Cursor cursor = db.query(TABLE_NEBENUHR_CONFIG,
            new String[]{"id", "uhr_name", "zeile_index", "relais_a", "relais_b",
                        "impuls_dauer_1", "impuls_dauer_2", "uhr_name_display",
                        "modus", "angezeigte_zeit", "mondphase_ist", "last_relais_a", "aktiv", "impuls_ausstehend", "pending_relais_a"},
            null, null, null, null, "zeile_index ASC");
        
        try {
            while (cursor.moveToNext()) {
                NebenuhrConfig nebenuhr = new NebenuhrConfig();
                nebenuhr.id = cursor.getInt(0);
                nebenuhr.uhrName = cursor.getString(1);
                nebenuhr.zeile = cursor.getInt(2);
                nebenuhr.relaisA = cursor.getInt(3);
                nebenuhr.relaisB = cursor.getInt(4);
                nebenuhr.impulsDauer1 = cursor.getInt(5);
                nebenuhr.impulsDauer2 = cursor.getInt(6);
                nebenuhr.uhrNameDisplay = cursor.isNull(7) ? null : cursor.getString(7);
                nebenuhr.modus = normalizeNebenuhrModus(cursor.isNull(8) ? "12" : cursor.getString(8), nebenuhr.zeile);
                nebenuhr.angezeigteZeit = normalizeNebenuhrAngezeigteZeit(cursor.getInt(9), nebenuhr.zeile);
                nebenuhr.mondphaseIst = cursor.getInt(10);
                nebenuhr.lastRelaisA = cursor.getInt(11) == 1;
                nebenuhr.aktiv = cursor.getInt(12) == 1;
                nebenuhr.impulsAusstehend = cursor.getColumnCount() > 13 && cursor.getInt(13) == 1;
                nebenuhr.pendingRelaisA = cursor.getColumnCount() > 14 && !cursor.isNull(14) ? (cursor.getInt(14) == 1) : null;
                nebenuhren.add(nebenuhr);
            }
        } finally {
            cursor.close();
        }
        
        // Stelle sicher, dass alle 4 Nebenuhren vorhanden sind (A, B, C, D)
        String[] uhrNamen = {"A", "B", "C", "D"};
        int[] zeilen = {3, 4, 5, 6};
        String[] displayNamen = {"Nebenuhr A", "Nebenuhr B", "Nebenuhr C", "Monduhr D"};
        String[] modi = {"12", "12", "12", "MOND"};
        for (int i = 0; i < uhrNamen.length; i++) {
            boolean found = false;
            for (NebenuhrConfig n : nebenuhren) {
                if (n.zeile == zeilen[i]) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                NebenuhrConfig nebenuhr = new NebenuhrConfig();
                nebenuhr.uhrName = uhrNamen[i];
                nebenuhr.zeile = zeilen[i];
                nebenuhr.relaisA = 0;
                nebenuhr.relaisB = 0;
                nebenuhr.impulsDauer1 = 1;
                nebenuhr.impulsDauer2 = 1;
                nebenuhr.uhrNameDisplay = displayNamen[i];
                nebenuhr.modus = modi[i];
                nebenuhr.angezeigteZeit = 0;
                nebenuhr.mondphaseIst = 0;
                nebenuhr.lastRelaisA = false;
                nebenuhr.aktiv = true;
                nebenuhren.add(nebenuhr);
            }
        }
        
        // Sortiere nach Zeile (Collections.sort für API < 24)
        Collections.sort(nebenuhren, new java.util.Comparator<NebenuhrConfig>() {
            @Override
            public int compare(NebenuhrConfig a, NebenuhrConfig b) {
                return Integer.compare(a.zeile, b.zeile);
            }
        });
        
        return nebenuhren;
    }
    
    /**
     * Holt eine Nebenuhr-Konfiguration nach Zeile (3=A, 4=B, 5=C, 6=D).
     */
    public NebenuhrConfig getNebenuhrByZeile(int zeile) {
        SQLiteDatabase db = getReadableDatabase();
        
        Cursor cursor = db.query(TABLE_NEBENUHR_CONFIG,
            new String[]{"id", "uhr_name", "zeile_index", "relais_a", "relais_b",
                        "impuls_dauer_1", "impuls_dauer_2", "uhr_name_display",
                        "modus", "angezeigte_zeit", "mondphase_ist", "last_relais_a", "aktiv", "impuls_ausstehend", "pending_relais_a"},
            "zeile_index = ?",
            new String[]{String.valueOf(zeile)},
            null, null, null);
        
        try {
            if (cursor.moveToFirst()) {
                NebenuhrConfig nebenuhr = new NebenuhrConfig();
                nebenuhr.id = cursor.getInt(0);
                nebenuhr.uhrName = cursor.getString(1);
                nebenuhr.zeile = cursor.getInt(2);
                nebenuhr.relaisA = cursor.getInt(3);
                nebenuhr.relaisB = cursor.getInt(4);
                nebenuhr.impulsDauer1 = cursor.getInt(5);
                nebenuhr.impulsDauer2 = cursor.getInt(6);
                nebenuhr.uhrNameDisplay = cursor.isNull(7) ? null : cursor.getString(7);
                nebenuhr.modus = normalizeNebenuhrModus(cursor.isNull(8) ? "12" : cursor.getString(8), zeile);
                nebenuhr.angezeigteZeit = normalizeNebenuhrAngezeigteZeit(cursor.getInt(9), zeile);
                nebenuhr.mondphaseIst = cursor.getInt(10);
                nebenuhr.lastRelaisA = cursor.getInt(11) == 1;
                nebenuhr.aktiv = cursor.getInt(12) == 1;
                nebenuhr.impulsAusstehend = cursor.getColumnCount() > 13 && cursor.getInt(13) == 1;
                nebenuhr.pendingRelaisA = cursor.getColumnCount() > 14 && !cursor.isNull(14) ? (cursor.getInt(14) == 1) : null;
                return nebenuhr;
            }
        } finally {
            cursor.close();
        }
        
        // Fallback: Erstelle Standard-Konfiguration
        NebenuhrConfig nebenuhr = new NebenuhrConfig();
        String[] uhrNamen = {"A", "B", "C", "D"};
        int[] zeilen = {3, 4, 5, 6};
        String[] displayNamen = {"Nebenuhr A", "Nebenuhr B", "Nebenuhr C", "Monduhr D"};
        String[] modi = {"12", "12", "12", "MOND"};
        for (int i = 0; i < zeilen.length; i++) {
            if (zeilen[i] == zeile) {
                nebenuhr.uhrName = uhrNamen[i];
                nebenuhr.zeile = zeile;
                nebenuhr.relaisA = 0;
                nebenuhr.relaisB = 0;
                nebenuhr.impulsDauer1 = 1;
                nebenuhr.impulsDauer2 = 1;
                nebenuhr.uhrNameDisplay = displayNamen[i];
                nebenuhr.modus = modi[i];
                nebenuhr.angezeigteZeit = 0;
                nebenuhr.mondphaseIst = 0;
                nebenuhr.lastRelaisA = false;
                nebenuhr.aktiv = true;
                return nebenuhr;
            }
        }
        
        return null;
    }
    
    /**
     * Speichert eine Nebenuhr-Konfiguration (INSERT oder UPDATE).
     */
    public void saveNebenuhr(NebenuhrConfig nebenuhr) {
        SQLiteDatabase db = getWritableDatabase();
        nebenuhr.modus = normalizeNebenuhrModus(nebenuhr.modus, nebenuhr.zeile);
        nebenuhr.angezeigteZeit = normalizeNebenuhrAngezeigteZeit(nebenuhr.angezeigteZeit, nebenuhr.zeile);
        
        // Prüfe ob Nebenuhr bereits existiert
        Cursor cursor = db.query(TABLE_NEBENUHR_CONFIG,
            new String[]{"id"},
            "uhr_name = ? AND zeile_index = ?",
            new String[]{nebenuhr.uhrName, String.valueOf(nebenuhr.zeile)},
            null, null, null);
        
        boolean exists = cursor.moveToFirst();
        cursor.close();
        
        Log.d(TAG, "saveNebenuhr: " + nebenuhr.uhrName + " (Zeile " + nebenuhr.zeile + ") existiert=" + exists);
        
        if (exists) {
            // UPDATE
            // WICHTIG: Lade aktuelle Zustandswerte aus DB, damit sie nicht überschrieben werden
            // wenn sie nicht explizit gesetzt wurden (z.B. beim Speichern aus Web-UI)
            Cursor currentCursor = db.query(TABLE_NEBENUHR_CONFIG,
                new String[]{"angezeigte_zeit", "mondphase_ist", "last_relais_a", "pending_relais_a"},
                "uhr_name = ? AND zeile_index = ?",
                new String[]{nebenuhr.uhrName, String.valueOf(nebenuhr.zeile)},
                null, null, null);
            
            int currentAngezeigteZeit = nebenuhr.angezeigteZeit;
            int currentMondphaseIst = nebenuhr.mondphaseIst;
            boolean currentLastRelaisA = nebenuhr.lastRelaisA;
            Boolean currentPendingRelaisA = nebenuhr.pendingRelaisA;
            
            if (currentCursor.moveToFirst()) {
                // Wenn Zustandswerte nicht explizit gesetzt wurden (0/false), behalte DB-Werte
                // Nur wenn Werte explizit gesetzt wurden (nicht 0 oder nicht false), verwende neue Werte
                int dbAngezeigteZeit = normalizeNebenuhrAngezeigteZeit(currentCursor.getInt(0), nebenuhr.zeile);
                int dbMondphaseIst = currentCursor.getInt(1);
                int dbLastRelaisA = currentCursor.getInt(2);
                Boolean dbPendingRelaisA = currentCursor.isNull(3) ? null : (currentCursor.getInt(3) == 1);
                
                // Behalte DB-Werte, wenn neue Werte nicht explizit gesetzt wurden
                // (0 könnte ein gültiger Wert sein, daher prüfen wir ob das Objekt explizit gesetzt wurde)
                // Da wir nicht wissen ob explizit gesetzt, behalten wir DB-Werte wenn neue Werte 0/false sind
                // UND die DB-Werte nicht 0/false sind (dann wurden sie wahrscheinlich nicht explizit gesetzt)
                if (nebenuhr.angezeigteZeit == 0 && dbAngezeigteZeit != 0) {
                    currentAngezeigteZeit = dbAngezeigteZeit;
                    Log.d(TAG, "Behalte angezeigte_zeit aus DB: " + dbAngezeigteZeit);
                }
                if (nebenuhr.mondphaseIst == 0 && dbMondphaseIst != 0) {
                    currentMondphaseIst = dbMondphaseIst;
                    Log.d(TAG, "Behalte mondphase_ist aus DB: " + dbMondphaseIst);
                }
                // last_relais_a: Immer den übergebenen Wert verwenden (z. B. nach manuellem Stellen
                // der Nebenuhr in SetNebenuhrActivity wird getoggelt, damit der nächste Impuls zählt).
                currentLastRelaisA = nebenuhr.lastRelaisA;
                currentPendingRelaisA = nebenuhr.pendingRelaisA != null ? nebenuhr.pendingRelaisA : dbPendingRelaisA;
            }
            currentCursor.close();
            
            android.content.ContentValues values = new android.content.ContentValues();
            values.put("relais_a", nebenuhr.relaisA);
            values.put("relais_b", nebenuhr.relaisB);
            values.put("impuls_dauer_1", nebenuhr.impulsDauer1);
            values.put("impuls_dauer_2", nebenuhr.impulsDauer2);
            values.put("uhr_name_display", nebenuhr.uhrNameDisplay);
            values.put("modus", nebenuhr.modus != null ? nebenuhr.modus : "12");
            values.put("angezeigte_zeit", currentAngezeigteZeit);
            values.put("mondphase_ist", currentMondphaseIst);
            values.put("last_relais_a", currentLastRelaisA ? 1 : 0);
            values.put("aktiv", nebenuhr.aktiv ? 1 : 0);
            values.put("impuls_ausstehend", nebenuhr.impulsAusstehend ? 1 : 0);
            if (currentPendingRelaisA == null) {
                values.putNull("pending_relais_a");
            } else {
                values.put("pending_relais_a", currentPendingRelaisA ? 1 : 0);
            }
            values.put("updated_at", "CURRENT_TIMESTAMP");
            
            int rowsUpdated = db.update(TABLE_NEBENUHR_CONFIG, values, 
                "uhr_name = ? AND zeile_index = ?",
                new String[]{nebenuhr.uhrName, String.valueOf(nebenuhr.zeile)});
            Log.d(TAG, "Nebenuhr " + nebenuhr.uhrName + " (Zeile " + nebenuhr.zeile + ") aktualisiert: " + rowsUpdated + " Zeilen, RelaisA=" + nebenuhr.relaisA + ", RelaisB=" + nebenuhr.relaisB + ", angezeigte_zeit=" + currentAngezeigteZeit + ", mondphase_ist=" + currentMondphaseIst + ", last_relais_a=" + currentLastRelaisA);
        } else {
            // INSERT
            android.content.ContentValues values = new android.content.ContentValues();
            values.put("uhr_name", nebenuhr.uhrName);
            values.put("zeile_index", nebenuhr.zeile);
            values.put("relais_a", nebenuhr.relaisA);
            values.put("relais_b", nebenuhr.relaisB);
            values.put("impuls_dauer_1", nebenuhr.impulsDauer1);
            values.put("impuls_dauer_2", nebenuhr.impulsDauer2);
            values.put("uhr_name_display", nebenuhr.uhrNameDisplay);
            values.put("modus", nebenuhr.modus != null ? nebenuhr.modus : "12");
            values.put("angezeigte_zeit", nebenuhr.angezeigteZeit);
            values.put("mondphase_ist", nebenuhr.mondphaseIst);
            values.put("last_relais_a", nebenuhr.lastRelaisA ? 1 : 0);
            values.put("aktiv", nebenuhr.aktiv ? 1 : 0);
            values.put("impuls_ausstehend", nebenuhr.impulsAusstehend ? 1 : 0);
            if (nebenuhr.pendingRelaisA == null) {
                values.putNull("pending_relais_a");
            } else {
                values.put("pending_relais_a", nebenuhr.pendingRelaisA ? 1 : 0);
            }
            
            db.insert(TABLE_NEBENUHR_CONFIG, null, values);
            Log.d(TAG, "Nebenuhr " + nebenuhr.uhrName + " (Zeile " + nebenuhr.zeile + ") erstellt");
        }
    }
    
    // ---------- Relais-Kategorien & Relais-Zuordnung ----------
    
    public List<RelaisKategorie> getAllRelaisKategorien() {
        SQLiteDatabase db = getReadableDatabase();
        List<RelaisKategorie> list = new ArrayList<>();
        Cursor cursor = db.query(TABLE_RELAIS_KATEGORIEN,
            new String[]{"id", "name", "sortierung"},
            null, null, null, null, "sortierung ASC, id ASC");
        try {
            while (cursor.moveToNext()) {
                RelaisKategorie k = new RelaisKategorie();
                k.id = cursor.getInt(0);
                k.name = cursor.getString(1);
                k.sortierung = cursor.getInt(2);
                list.add(k);
            }
        } finally {
            cursor.close();
        }
        return list;
    }
    
    public RelaisKategorie getRelaisKategorie(int id) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.query(TABLE_RELAIS_KATEGORIEN,
            new String[]{"id", "name", "sortierung"},
            "id = ?", new String[]{String.valueOf(id)}, null, null, null);
        try {
            if (cursor.moveToFirst()) {
                RelaisKategorie k = new RelaisKategorie();
                k.id = cursor.getInt(0);
                k.name = cursor.getString(1);
                k.sortierung = cursor.getInt(2);
                return k;
            }
        } finally {
            cursor.close();
        }
        return null;
    }
    
    public long insertRelaisKategorie(RelaisKategorie k) {
        SQLiteDatabase db = getWritableDatabase();
        android.content.ContentValues values = new android.content.ContentValues();
        values.put("name", k.name != null ? k.name : "");
        values.put("sortierung", k.sortierung);
        return db.insert(TABLE_RELAIS_KATEGORIEN, null, values);
    }
    
    public int updateRelaisKategorie(RelaisKategorie k) {
        SQLiteDatabase db = getWritableDatabase();
        android.content.ContentValues values = new android.content.ContentValues();
        values.put("name", k.name != null ? k.name : "");
        values.put("sortierung", k.sortierung);
        values.put("updated_at", "CURRENT_TIMESTAMP");
        return db.update(TABLE_RELAIS_KATEGORIEN, values, "id = ?", new String[]{String.valueOf(k.id)});
    }
    
    public int deleteRelaisKategorie(int id) {
        SQLiteDatabase db = getWritableDatabase();
        db.execSQL("UPDATE " + TABLE_RELAIS_ZUORDNUNG + " SET kategorie_id = NULL WHERE kategorie_id = ?", new String[]{String.valueOf(id)});
        return db.delete(TABLE_RELAIS_KATEGORIEN, "id = ?", new String[]{String.valueOf(id)});
    }
    
    public List<RelaisZuordnung> getAllRelaisZuordnung(Integer kategorieId) {
        SQLiteDatabase db = getReadableDatabase();
        List<RelaisZuordnung> list = new ArrayList<>();
        String where = kategorieId != null ? "kategorie_id = ?" : null;
        String[] whereArgs = kategorieId != null ? new String[]{String.valueOf(kategorieId)} : null;
        Cursor cursor = db.query(TABLE_RELAIS_ZUORDNUNG,
            new String[]{"id", "software_relais_id", "name", "kategorie_id", "platine_nummer", "relais_nummer"},
            where, whereArgs, null, null, "software_relais_id ASC");
        try {
            while (cursor.moveToNext()) {
                RelaisZuordnung z = new RelaisZuordnung();
                z.id = cursor.getInt(0);
                z.softwareRelaisId = cursor.getInt(1);
                z.name = cursor.isNull(2) ? null : cursor.getString(2);
                z.kategorieId = cursor.isNull(3) ? null : cursor.getInt(3);
                z.platineNummer = cursor.getInt(4);
                z.relaisNummer = cursor.getInt(5);
                list.add(z);
            }
        } finally {
            cursor.close();
        }
        return list;
    }
    
    public RelaisZuordnung getRelaisZuordnungBySoftwareId(int softwareRelaisId) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.query(TABLE_RELAIS_ZUORDNUNG,
            new String[]{"id", "software_relais_id", "name", "kategorie_id", "platine_nummer", "relais_nummer"},
            "software_relais_id = ?", new String[]{String.valueOf(softwareRelaisId)}, null, null, null);
        try {
            if (cursor.moveToFirst()) {
                RelaisZuordnung z = new RelaisZuordnung();
                z.id = cursor.getInt(0);
                z.softwareRelaisId = cursor.getInt(1);
                z.name = cursor.isNull(2) ? null : cursor.getString(2);
                z.kategorieId = cursor.isNull(3) ? null : cursor.getInt(3);
                z.platineNummer = cursor.getInt(4);
                z.relaisNummer = cursor.getInt(5);
                return z;
            }
        } finally {
            cursor.close();
        }
        return null;
    }
    
    public RelaisZuordnung getRelaisZuordnungById(int id) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.query(TABLE_RELAIS_ZUORDNUNG,
            new String[]{"id", "software_relais_id", "name", "kategorie_id", "platine_nummer", "relais_nummer"},
            "id = ?", new String[]{String.valueOf(id)}, null, null, null);
        try {
            if (cursor.moveToFirst()) {
                RelaisZuordnung z = new RelaisZuordnung();
                z.id = cursor.getInt(0);
                z.softwareRelaisId = cursor.getInt(1);
                z.name = cursor.isNull(2) ? null : cursor.getString(2);
                z.kategorieId = cursor.isNull(3) ? null : cursor.getInt(3);
                z.platineNummer = cursor.getInt(4);
                z.relaisNummer = cursor.getInt(5);
                return z;
            }
        } finally {
            cursor.close();
        }
        return null;
    }
    
    /**
     * Löst eine Software-Relais-ID in Hardware (Platine + Relais) auf. Für spätere App-Nutzung.
     * @return int[2] = { platineNummer, relaisNummer } oder null wenn nicht gefunden
     */
    public int[] resolveSoftwareRelaisToHardware(int softwareRelaisId) {
        RelaisZuordnung z = getRelaisZuordnungBySoftwareId(softwareRelaisId);
        if (z == null) return null;
        return new int[]{ z.platineNummer, z.relaisNummer };
    }
    
    public long insertRelaisZuordnung(RelaisZuordnung z) {
        SQLiteDatabase db = getWritableDatabase();
        android.content.ContentValues values = new android.content.ContentValues();
        values.put("software_relais_id", z.softwareRelaisId);
        values.put("name", z.name);
        values.put("kategorie_id", z.kategorieId);
        values.put("platine_nummer", z.platineNummer);
        values.put("relais_nummer", z.relaisNummer);
        return db.insert(TABLE_RELAIS_ZUORDNUNG, null, values);
    }
    
    public int updateRelaisZuordnung(RelaisZuordnung z) {
        SQLiteDatabase db = getWritableDatabase();
        android.content.ContentValues values = new android.content.ContentValues();
        values.put("software_relais_id", z.softwareRelaisId);
        values.put("name", z.name);
        values.put("kategorie_id", z.kategorieId);
        values.put("platine_nummer", z.platineNummer);
        values.put("relais_nummer", z.relaisNummer);
        values.put("updated_at", "CURRENT_TIMESTAMP");
        return db.update(TABLE_RELAIS_ZUORDNUNG, values, "id = ?", new String[]{String.valueOf(z.id)});
    }
    
    public int deleteRelaisZuordnung(int id) {
        SQLiteDatabase db = getWritableDatabase();
        return db.delete(TABLE_RELAIS_ZUORDNUNG, "id = ?", new String[]{String.valueOf(id)});
    }
    
    /**
     * Migriert Nebenuhr-Daten aus System.xls Sheet 15 in die Datenbank.
     */
    private void migrateNebenuhrenFromExcel(SQLiteDatabase db) {
        try {
            // Prüfe ob bereits Daten in der Datenbank vorhanden sind
            Cursor checkCursor = db.query(TABLE_NEBENUHR_CONFIG, 
                new String[]{"COUNT(*) as count"}, null, null, null, null, null);
            boolean hasData = false;
            if (checkCursor.moveToFirst()) {
                hasData = checkCursor.getInt(0) > 0;
            }
            checkCursor.close();
            
            if (hasData) {
                Log.d(TAG, "Nebenuhr-Daten bereits in Datenbank vorhanden, überspringe Migration");
                return;
            }
            
            String sdCardPath = android.os.Environment.getExternalStorageDirectory().getPath();
            String systemXlsPath = sdCardPath + StaticConstants.excellSystemString;
            java.io.File systemFile = new java.io.File(systemXlsPath);
            
            if (!systemFile.exists()) {
                Log.d(TAG, "System.xls nicht gefunden, überspringe Nebenuhr-Migration");
                return;
            }
            
            ExcelRead excelread = new ExcelRead();
            excelread.openXlsSheet(systemXlsPath, 15);
            
            Log.d(TAG, "Migriere Nebenuhr-Daten aus System.xls Sheet 15");
            
            String[] uhrNamen = {"A", "B", "C"};
            int[] zeilen = {3, 4, 5};
            
            for (int i = 0; i < uhrNamen.length; i++) {
                String uhrName = uhrNamen[i];
                int zeile = zeilen[i];
                
                try {
                    NebenuhrConfig nebenuhr = new NebenuhrConfig();
                    nebenuhr.uhrName = uhrName;
                    nebenuhr.zeile = zeile;
                    
                    // Impuls-Dauer 1 (0, zeile) - von Sekunden zu Millisekunden
                    try {
                        String impulsZeitStr = excelread.getCellString(0, zeile);
                        int impulsZeitSekunden = (impulsZeitStr != null && !impulsZeitStr.trim().isEmpty()) 
                            ? Integer.parseInt(impulsZeitStr.trim()) : 1;
                        nebenuhr.impulsDauer1 = impulsZeitSekunden; // Sekunden bleiben Sekunden
                    } catch (Exception e) {
                        nebenuhr.impulsDauer1 = 1; // Default: 1 Sekunde
                    }
                    
                    // Impuls-Dauer 2 / Pause (1, zeile) - von Sekunden zu Millisekunden
                    try {
                        String impulsPauseStr = excelread.getCellString(1, zeile);
                        int impulsPauseSekunden = (impulsPauseStr != null && !impulsPauseStr.trim().isEmpty()) 
                            ? Integer.parseInt(impulsPauseStr.trim()) : 1;
                        nebenuhr.impulsDauer2 = impulsPauseSekunden; // Sekunden bleiben Sekunden
                    } catch (Exception e) {
                        nebenuhr.impulsDauer2 = 1; // Default: 1 Sekunde
                    }
                    
                    // Relais A (3, zeile) - war relais_gerade
                    try {
                        String relaisAStr = excelread.getCellString(3, zeile);
                        nebenuhr.relaisA = (relaisAStr != null && !relaisAStr.trim().isEmpty() && !relaisAStr.equals("0")) 
                            ? Integer.parseInt(relaisAStr.trim()) : 0;
                    } catch (Exception e) {
                        nebenuhr.relaisA = 0;
                    }
                    
                    // Relais B (4, zeile) - war relais_ungerade
                    try {
                        String relaisBStr = excelread.getCellString(4, zeile);
                        nebenuhr.relaisB = (relaisBStr != null && !relaisBStr.trim().isEmpty() && !relaisBStr.equals("0")) 
                            ? Integer.parseInt(relaisBStr.trim()) : 0;
                    } catch (Exception e) {
                        nebenuhr.relaisB = 0;
                    }
                    
                    // Uhr-Name (5, zeile)
                    try {
                        String uhrNameDisplay = excelread.getCellString(5, zeile);
                        nebenuhr.uhrNameDisplay = (uhrNameDisplay != null && !uhrNameDisplay.trim().isEmpty()) 
                            ? uhrNameDisplay.trim() : uhrName;
                    } catch (Exception e) {
                        nebenuhr.uhrNameDisplay = uhrName;
                    }
                    
                    // Setze Standard-Werte
                    nebenuhr.modus = "12";
                    nebenuhr.angezeigteZeit = 0;
                    nebenuhr.mondphaseIst = 0;
                    nebenuhr.lastRelaisA = false;
                    nebenuhr.aktiv = true;
                    
                    // In Datenbank speichern
                    android.content.ContentValues values = new android.content.ContentValues();
                    values.put("uhr_name", nebenuhr.uhrName);
                    values.put("zeile_index", nebenuhr.zeile);
                    values.put("relais_a", nebenuhr.relaisA);
                    values.put("relais_b", nebenuhr.relaisB);
                    values.put("impuls_dauer_1", nebenuhr.impulsDauer1);
                    values.put("impuls_dauer_2", nebenuhr.impulsDauer2);
                    values.put("uhr_name_display", nebenuhr.uhrNameDisplay);
                    values.put("modus", nebenuhr.modus);
                    values.put("angezeigte_zeit", nebenuhr.angezeigteZeit);
                    values.put("mondphase_ist", nebenuhr.mondphaseIst);
                    values.put("last_relais_a", nebenuhr.lastRelaisA ? 1 : 0);
                    values.put("aktiv", nebenuhr.aktiv ? 1 : 0);
                    
                    db.insert(TABLE_NEBENUHR_CONFIG, null, values);
                    Log.d(TAG, "Nebenuhr " + uhrName + " migriert: Impuls=" + nebenuhr.impulsDauer1 + "/" + nebenuhr.impulsDauer2 + 
                          ", Relais=" + nebenuhr.relaisA + "/" + nebenuhr.relaisB);
                } catch (Exception e) {
                    Log.w(TAG, "Fehler beim Migrieren von Nebenuhr " + uhrName, e);
                }
            }
            
            excelread.closeWorkbook();
            Log.d(TAG, "Nebenuhr-Migration abgeschlossen");
            
        } catch (Exception e) {
            Log.e(TAG, "Fehler bei Migration von Nebenuhr-Daten aus Excel", e);
        }
    }
    
    /**
     * Entity-Klasse für Nebenuhr-Konfiguration.
     */
    public static class NebenuhrConfig {
        public int id;
        public String uhrName;          // "A", "B", "C", "D"
        public int zeile;               // Excel-Zeile (3, 4, 5, 6)
        public int relaisA;             // Relais A Nummer (für Wechselschaltung)
        public int relaisB;             // Relais B Nummer (für Wechselschaltung)
        public int impulsDauer1;         // Impulsdauer 1 (Millisekunden)
        public int impulsDauer2;         // Impulsdauer 2 / Pause (Millisekunden)
        public String uhrNameDisplay;   // Anzeige-Name
        public String modus;            // "12" oder "MOND"
        public int angezeigteZeit;      // Aktuelle angezeigte Zeit (12h-Minuten, 0-719)
        public int mondphaseIst;        // Aktueller Impulswert im Mondzyklus für Monduhr D
        public boolean lastRelaisA;     // Letztes verwendetes Relais (true=A, false=B) für Wechselschaltung
        public boolean aktiv;           // Aktiv-Status (true=aktiv, false=inaktiv)
        /** true = angezeigteZeit wurde vor Impuls gespeichert, Impuls ist noch nicht durch (Absturzfall). Beim Laden dann angezeigteZeit − 1 verwenden. */
        public boolean impulsAusstehend;
        public Boolean pendingRelaisA;
    }

    private static String normalizeNebenuhrModus(String modus, int zeile) {
        return zeile == 6 ? "MOND" : "12";
    }

    private static int normalizeNebenuhrAngezeigteZeit(int angezeigteZeit, int zeile) {
        if (zeile == 6) {
            return 0;
        }
        int normalized = angezeigteZeit % 720;
        if (normalized < 0) {
            normalized += 720;
        }
        return normalized;
    }
    
    /**
     * Entity für Relais-Kategorie (z. B. Nebenuhr, Schlagwerk).
     */
    public static class RelaisKategorie {
        public int id;
        public String name;
        public int sortierung;
    }
    
    /**
     * Entity für Software-Relais-Zuordnung (software_relais_id → Platine + Relais).
     */
    public static class RelaisZuordnung {
        public int id;
        public int softwareRelaisId;
        public String name;
        public Integer kategorieId;
        public int platineNummer;
        public int relaisNummer;
    }

    /**
     * Stammdaten fuer UI-Funktionen (frueher Sonder-/Systemtasten 1100-1206).
     */
    public static class UiFunktion {
        public int id;
        public int codeAlt;
        public String keyName;
        public String anzeigeName;
        public String beschreibung;
        public boolean aktiv;
        public int sortierung;
    }

    /**
     * Neues Layout-Modell als Nachfolger von Beschriftung-Tasten.
     * Es beschreibt Position/Anzeige und verweist auf UI-Funktionen oder spaeter andere Zielobjekte.
     */
    public static class LayoutItem {
        public int id;
        public int pageNummer;
        public int slotIndex;
        public int gridRow;
        public int gridCol;
        public int gridWidth;
        public int gridHeight;
        public String labelText;
        public String buttonId;
        public String targetKind;
        public Integer targetId;
        public String legacyCode;
        public String legacyPlatine;
        public String legacyHammerzeit;
        public String legacyFunktion;
        public String sound;
        public boolean sichtbar;
        public int sortierung;
    }
    
    /**
     * Zeile aus Beschriftung-Tasten. c1=Funktion (Tastentyp), c2=Beschriftung, c3=Relais, c4=Hammerzeit,
     * c5=Platine, c6-c10=Grafik-Pfade, c13=buttonId, sonder_id=Systemtaste (1100=STOP, 1101=AUTOMATIC, 1102=HOME, 1103=HELP, 1104=SCHLAGWERK_ON_OFF, 1202=ZWEITE_SEITE, …).
     */
    public static class BeschriftungTastenRow {
        public int id;
        public int zeileIndex;
        public String c1;   // Funktion (Normal, Verknüpft, Sofort Start, Schwingen, Melodie, …)
        public String c2;   // Beschriftung
        public String c3;   // Relais-Nummer
        public String c4;   // Hammerzeit
        public String c5;   // Platine-Nummer
        public String c6;   // Grafik normal
        public String c7;   // Grafik manual_ein_ok
        public String c8;   // Grafik manual_ein_error
        public String c9;   // Grafik automatic_ein_ok
        public String c10;  // Grafik automatic_ein_error
        public String c13;  // buttonId
        public Integer sonderId; // 1100=STOP, 1101=AUTOMATIC, 1102=HOME, 1103=HELP, 1202=ZWEITE_SEITE, 1203=PROGRAMM_EINGEBEN, 1204=PROGRAMM_ABFRAGEN, 1205=SET_NEBENUHR
        public String sound; // Sound-Dateiname (z. B. für Glocke beim Tastendruck), Excel-Spalte 11
    }

    private static final String[] BESCHRIFTUNG_TASTEN_COLUMNS = new String[]{
        "id", "zeile_index", "c1", "c2", "c3", "c4", "c5", "c6", "c7", "c8", "c9", "c10", "c13", "sonder_id", "sound"
    };
    
    /** Anzahl Tasten-Slots (Seite 1 + Seite 2). Wenn Tabelle leer ist, werden so viele „Leer“-Zeilen angelegt. */
    private static final int BESCHRIFTUNG_TASTEN_DEFAULT_COUNT = 48;

    /** Vorlage „leer“: c1–c5 überall „leer“, c13 leer. */
    private static void putLeerRow(android.content.ContentValues values, int zeileIndex) {
        values.clear();
        values.put("zeile_index", zeileIndex);
        values.put("c1", "leer");
        values.put("c2", "leer");
        values.put("c3", "leer");
        values.put("c4", "leer");
        values.put("c5", "leer");
        values.put("c13", "");
        values.put("sound", "");
    }

    /** Vorlage „NULL“: c1–c5 und c13 überall „NULL“. */
    private static void putNullRow(android.content.ContentValues values, int zeileIndex) {
        values.clear();
        values.put("zeile_index", zeileIndex);
        values.put("c1", "NULL");
        values.put("c2", "NULL");
        values.put("c3", "NULL");
        values.put("c4", "NULL");
        values.put("c5", "NULL");
        values.put("c13", "NULL");
        values.put("sound", "");
    }

    /** Liefert eine Zeile mit Vorlage „NULL“ (c1–c5, c13 = "NULL") für zeileIndex. Z. B. zum Einfügen einer NULL-Slot-Zeile. */
    public static BeschriftungTastenRow createNullTemplateRow(int zeileIndex) {
        BeschriftungTastenRow r = new BeschriftungTastenRow();
        r.id = 0;
        r.zeileIndex = zeileIndex;
        r.c1 = "NULL";
        r.c2 = "NULL";
        r.c3 = "NULL";
        r.c4 = "NULL";
        r.c5 = "NULL";
        r.c13 = "NULL";
        r.c6 = r.c7 = r.c8 = r.c9 = r.c10 = null;
        r.sonderId = null;
        r.sound = null;
        return r;
    }

    /** Liefert eine Zeile mit Vorlage „leer“ (c1–c5 = "leer", c13 = "") für zeileIndex. */
    public static BeschriftungTastenRow createLeerTemplateRow(int zeileIndex) {
        BeschriftungTastenRow r = new BeschriftungTastenRow();
        r.id = 0;
        r.zeileIndex = zeileIndex;
        r.c1 = "leer";
        r.c2 = "leer";
        r.c3 = "leer";
        r.c4 = "leer";
        r.c5 = "leer";
        r.c13 = "";
        r.c6 = r.c7 = r.c8 = r.c9 = r.c10 = null;
        r.sonderId = null;
        r.sound = null;
        return r;
    }

    /**
     * Stellt sicher, dass beschriftung_tasten mindestens 48 Zeilen hat. Ist die Tabelle leer, werden 24 Zeilen
     * mit Vorlage „leer“ (c1–c5 = „leer“) und 24 Zeilen mit Vorlage „NULL“ (c1–c5 = „NULL“) eingefügt.
     */
    public void ensureBeschriftungTastenFilledWithLeer() {
        SQLiteDatabase db = getWritableDatabase();
        Cursor c = null;
        try {
            c = db.query(TABLE_BESCHRIFTUNG_TASTEN, new String[]{"id"}, null, null, null, null, null, "1");
            if (c.moveToFirst()) {
                return; // Es gibt mindestens eine Zeile, nichts tun
            }
        } finally {
            if (c != null) c.close();
        }
        db.beginTransaction();
        try {
            android.content.ContentValues values = new android.content.ContentValues();
            for (int i = 0; i < 24; i++) {
                putLeerRow(values, i);
                db.insert(TABLE_BESCHRIFTUNG_TASTEN, null, values);
            }
            for (int i = 24; i < BESCHRIFTUNG_TASTEN_DEFAULT_COUNT; i++) {
                putNullRow(values, i);
                db.insert(TABLE_BESCHRIFTUNG_TASTEN, null, values);
            }
            db.setTransactionSuccessful();
            Log.d(TAG, "beschriftung_tasten war leer – 24 Leer- und 24 NULL-Vorlagen eingefügt");
        } finally {
            db.endTransaction();
        }
    }

    /**
     * Lädt alle Beschriftung-Tasten-Zeilen aus der Datenbank (Reihenfolge nach zeile_index).
     * Ist die Tabelle leer, werden zuerst 48 „Leer“-Zeilen angelegt, dann geladen.
     */
    public List<BeschriftungTastenRow> getBeschriftungTasten() {
        ensureBeschriftungTastenFilledWithLeer();
        List<BeschriftungTastenRow> rows = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.query(TABLE_BESCHRIFTUNG_TASTEN,
            BESCHRIFTUNG_TASTEN_COLUMNS,
            null, null, null, null, "zeile_index ASC");
        try {
            int idxId = cursor.getColumnIndexOrThrow("id");
            int idxZeile = cursor.getColumnIndexOrThrow("zeile_index");
            int idxC1 = cursor.getColumnIndex("c1");
            int idxC2 = cursor.getColumnIndexOrThrow("c2");
            int idxC3 = cursor.getColumnIndexOrThrow("c3");
            int idxC4 = cursor.getColumnIndexOrThrow("c4");
            int idxC5 = cursor.getColumnIndexOrThrow("c5");
            int idxC6 = cursor.getColumnIndex("c6");
            int idxC7 = cursor.getColumnIndex("c7");
            int idxC8 = cursor.getColumnIndex("c8");
            int idxC9 = cursor.getColumnIndex("c9");
            int idxC10 = cursor.getColumnIndex("c10");
            int idxC13 = cursor.getColumnIndexOrThrow("c13");
            int idxSonder = cursor.getColumnIndex("sonder_id");
            int idxSound = cursor.getColumnIndex("sound");
            while (cursor.moveToNext()) {
                BeschriftungTastenRow r = new BeschriftungTastenRow();
                r.id = cursor.getInt(idxId);
                r.zeileIndex = cursor.getInt(idxZeile);
                r.c1 = idxC1 >= 0 && !cursor.isNull(idxC1) ? cursor.getString(idxC1) : null;
                r.c2 = cursor.isNull(idxC2) ? null : cursor.getString(idxC2);
                r.c3 = cursor.isNull(idxC3) ? null : cursor.getString(idxC3);
                r.c4 = cursor.isNull(idxC4) ? null : cursor.getString(idxC4);
                r.c5 = cursor.isNull(idxC5) ? null : cursor.getString(idxC5);
                r.c6 = idxC6 >= 0 && !cursor.isNull(idxC6) ? cursor.getString(idxC6) : null;
                r.c7 = idxC7 >= 0 && !cursor.isNull(idxC7) ? cursor.getString(idxC7) : null;
                r.c8 = idxC8 >= 0 && !cursor.isNull(idxC8) ? cursor.getString(idxC8) : null;
                r.c9 = idxC9 >= 0 && !cursor.isNull(idxC9) ? cursor.getString(idxC9) : null;
                r.c10 = idxC10 >= 0 && !cursor.isNull(idxC10) ? cursor.getString(idxC10) : null;
                r.c13 = cursor.isNull(idxC13) ? null : cursor.getString(idxC13);
                r.sonderId = idxSonder >= 0 && !cursor.isNull(idxSonder) ? cursor.getInt(idxSonder) : null;
                r.sound = idxSound >= 0 && !cursor.isNull(idxSound) ? cursor.getString(idxSound) : null;
                rows.add(r);
            }
        } finally {
            cursor.close();
        }
        return rows;
    }

    public List<UiFunktion> getAllUiFunktionen() {
        List<UiFunktion> rows = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.query(TABLE_UI_FUNKTION,
            new String[]{"id", "code_alt", "key_name", "anzeige_name", "beschreibung", "aktiv", "sortierung"},
            null, null, null, null, "sortierung ASC, code_alt ASC");
        try {
            while (cursor.moveToNext()) {
                UiFunktion item = new UiFunktion();
                item.id = cursor.getInt(0);
                item.codeAlt = cursor.getInt(1);
                item.keyName = cursor.getString(2);
                item.anzeigeName = cursor.getString(3);
                item.beschreibung = cursor.isNull(4) ? null : cursor.getString(4);
                item.aktiv = cursor.getInt(5) != 0;
                item.sortierung = cursor.getInt(6);
                rows.add(item);
            }
        } finally {
            cursor.close();
        }
        return rows;
    }

    public List<LayoutItem> getAllLayoutItems() {
        ensureLayoutItemsBackfilled();
        List<LayoutItem> rows = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.query(TABLE_LAYOUT_ITEM,
            new String[]{"id", "page_nummer", "slot_index", "grid_row", "grid_col", "grid_width", "grid_height",
                "label_text", "button_id", "target_kind", "target_id", "legacy_code", "legacy_platine",
                "legacy_hammerzeit", "legacy_funktion", "sound", "sichtbar", "sortierung"},
            null, null, null, null, "page_nummer ASC, slot_index ASC");
        try {
            while (cursor.moveToNext()) {
                LayoutItem item = new LayoutItem();
                item.id = cursor.getInt(0);
                item.pageNummer = cursor.getInt(1);
                item.slotIndex = cursor.getInt(2);
                item.gridRow = cursor.getInt(3);
                item.gridCol = cursor.getInt(4);
                item.gridWidth = cursor.getInt(5);
                item.gridHeight = cursor.getInt(6);
                item.labelText = cursor.isNull(7) ? null : cursor.getString(7);
                item.buttonId = cursor.isNull(8) ? null : cursor.getString(8);
                item.targetKind = cursor.isNull(9) ? null : cursor.getString(9);
                item.targetId = cursor.isNull(10) ? null : cursor.getInt(10);
                item.legacyCode = cursor.isNull(11) ? null : cursor.getString(11);
                item.legacyPlatine = cursor.isNull(12) ? null : cursor.getString(12);
                item.legacyHammerzeit = cursor.isNull(13) ? null : cursor.getString(13);
                item.legacyFunktion = cursor.isNull(14) ? null : cursor.getString(14);
                item.sound = cursor.isNull(15) ? null : cursor.getString(15);
                item.sichtbar = cursor.getInt(16) != 0;
                item.sortierung = cursor.getInt(17);
                rows.add(item);
            }
        } finally {
            cursor.close();
        }
        return rows;
    }

    public void rebuildLayoutItemsFromBeschriftungTasten() {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            db.delete(TABLE_LAYOUT_ITEM, null, null);
            ensureLayoutItemsFromBeschriftungTasten(db);
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    /**
     * Importiert Beschriftung-Tasten aus einer Excel-Datei (Beschriftung-Tasten.xls, Sheet 0).
     * Spalten: 0=Funktion (c1, optional), 2=Beschriftung (c2), 3=Relais (c3), 4=Hammerzeit (c4), 5=Platine (c5), 13=buttonId (c13).
     * Es werden bis zu 48 Zeilen gelesen; fehlende Zeilen werden als „Leer“ ergänzt.
     * @param excelPath absoluter Pfad zur .xls-Datei
     * @return Anzahl importierter Zeilen (0 bei Fehler)
     */
    public int importBeschriftungTastenFromExcel(String excelPath) {
        if (excelPath == null || excelPath.trim().isEmpty()) return 0;
        ExcelRead excelread = new ExcelRead();
        try {
            excelread.openXlsSheet(excelPath.trim(), 0);
        } catch (Exception e) {
            Log.e(TAG, "Excel öffnen fehlgeschlagen: " + excelPath, e);
            return 0;
        }
        try {
            int maxZeilen = excelread.getCellZeilen();
            Log.d(TAG, "Beschriftung-Tasten Excel: Pfad=" + excelPath + ", getCellZeilen()=" + maxZeilen);
            if (maxZeilen <= 0) {
                Log.w(TAG, "Excel-Sheet hat keine Zeilen (leer oder falsches Format?)");
            }
            List<BeschriftungTastenRow> rows = new ArrayList<>(48);
            for (int zeileIndex = 0; zeileIndex < 48; zeileIndex++) {
                int excelRow = zeileIndex + 1;
                BeschriftungTastenRow r = new BeschriftungTastenRow();
                r.id = 0;
                r.zeileIndex = zeileIndex;
                String c2 = excelRow <= maxZeilen ? safeGetCellString(excelread, 2, excelRow) : null;
                String c2Trim = c2 != null ? c2.trim() : "";
                if ("NULL".equals(c2Trim) || "null".equals(c2Trim)) {
                    r.c1 = "NULL";
                    r.c2 = "NULL";
                    r.c3 = "NULL";
                    r.c4 = "NULL";
                    r.c5 = "NULL";
                    r.c13 = "NULL";
                } else if (c2 == null || c2Trim.isEmpty() || "leer".equalsIgnoreCase(c2Trim)) {
                    /* Vorlage „leer“: überall „leer“, sonst geht es nicht */
                    r.c1 = "leer";
                    r.c2 = "leer";
                    r.c3 = "leer";
                    r.c4 = "leer";
                    r.c5 = "leer";
                    r.c13 = "";
                } else {
                    String c1 = safeGetCellString(excelread, 0, excelRow);
                    r.c1 = (c1 != null && !c1.isEmpty()) ? c1 : "Normal";
                    r.c2 = c2.trim();
                    String c3 = safeGetCellString(excelread, 3, excelRow);
                    r.c3 = c3 != null ? c3 : "";
                    String c4 = safeGetCellString(excelread, 4, excelRow);
                    r.c4 = c4 != null ? c4 : "";
                    String c5 = safeGetCellString(excelread, 5, excelRow);
                    r.c5 = (c5 != null && !c5.isEmpty()) ? c5 : "1";
                    String c13 = safeGetCellString(excelread, 13, excelRow);
                    r.c13 = c13 != null ? c13 : "";
                }
                // Grafik-Felder (Tasten-Farben) und Sound aus Excel Spalten 6–11 übernehmen
                if (excelRow <= maxZeilen) {
                    String ec6 = safeGetCellString(excelread, 6, excelRow);
                    String ec7 = safeGetCellString(excelread, 7, excelRow);
                    String ec8 = safeGetCellString(excelread, 8, excelRow);
                    String ec9 = safeGetCellString(excelread, 9, excelRow);
                    String ec10 = safeGetCellString(excelread, 10, excelRow);
                    String ec11 = safeGetCellString(excelread, 11, excelRow);
                    r.c6 = (ec6 != null && !ec6.trim().isEmpty() && !"null".equalsIgnoreCase(ec6.trim())) ? ec6.trim() : null;
                    r.c7 = (ec7 != null && !ec7.trim().isEmpty() && !"null".equalsIgnoreCase(ec7.trim())) ? ec7.trim() : null;
                    r.c8 = (ec8 != null && !ec8.trim().isEmpty() && !"null".equalsIgnoreCase(ec8.trim())) ? ec8.trim() : null;
                    r.c9 = (ec9 != null && !ec9.trim().isEmpty() && !"null".equalsIgnoreCase(ec9.trim())) ? ec9.trim() : null;
                    r.c10 = (ec10 != null && !ec10.trim().isEmpty() && !"null".equalsIgnoreCase(ec10.trim())) ? ec10.trim() : null;
                    r.sound = (ec11 != null && !ec11.trim().isEmpty() && !"null".equalsIgnoreCase(ec11.trim())) ? ec11.trim() : null;
                } else {
                    r.c6 = r.c7 = r.c8 = r.c9 = r.c10 = null;
                    r.sound = null;
                }
                r.sonderId = null;
                rows.add(r);
            }
            replaceBeschriftungTasten(rows);
            Log.d(TAG, "Beschriftung-Tasten aus Excel importiert: " + rows.size() + " Zeilen");
            return rows.size();
        } catch (Exception e) {
            Log.e(TAG, "Fehler beim Import Beschriftung-Tasten aus Excel", e);
            return 0;
        } finally {
            try {
                excelread.closeWorkbook();
            } catch (Exception e) {
                Log.w(TAG, "Excel closeWorkbook", e);
            }
        }
    }

    /**
     * Liefert die Standard-Vorlage für „Neue Anlage“: 48 Slots, Zeile 0 = Leer, 1 = Stop, 2 = Automatik,
     * 3–23 = Leer, 24–47 = NULL. So erscheinen in der App nur Stop- und Automatik-Taste auf Seite 1.
     */
    public List<BeschriftungTastenRow> getDefaultBeschriftungTastenNeuanlage() {
        List<BeschriftungTastenRow> rows = new ArrayList<>(BESCHRIFTUNG_TASTEN_DEFAULT_COUNT);
        for (int i = 0; i < BESCHRIFTUNG_TASTEN_DEFAULT_COUNT; i++) {
            if (i == 1) {
                BeschriftungTastenRow r = new BeschriftungTastenRow();
                r.id = 0;
                r.zeileIndex = 1;
                r.c1 = "Stop";
                r.c2 = "Stop";
                r.c3 = r.c4 = r.c5 = r.c13 = "";
                r.c6 = r.c7 = r.c8 = r.c9 = r.c10 = null;
                r.sonderId = SONDER_ID_STOP;
                r.sound = "";
                rows.add(r);
            } else if (i == 2) {
                BeschriftungTastenRow r = new BeschriftungTastenRow();
                r.id = 0;
                r.zeileIndex = 2;
                r.c1 = "Automatik";
                r.c2 = "Automatik";
                r.c3 = r.c4 = r.c5 = r.c13 = "";
                r.c6 = r.c7 = r.c8 = r.c9 = r.c10 = null;
                r.sonderId = SONDER_ID_AUTOMATIK;
                r.sound = "";
                rows.add(r);
            } else if (i < 24) {
                rows.add(createLeerTemplateRow(i));
            } else {
                rows.add(createNullTemplateRow(i));
            }
        }
        return rows;
    }

    /**
     * Setzt das Layout (Seite 1 und 2) auf die Neuanlage-Vorlage zurück: nur Stop und Automatik, Rest Leer/NULL.
     * Wird z. B. bei „Neue Anlage“ in den Anlagendaten aufgerufen.
     */
    public void replaceBeschriftungTastenWithNeuanlageDefault() {
        replaceBeschriftungTasten(getDefaultBeschriftungTastenNeuanlage());
        Log.d(TAG, "Layout (beschriftung_tasten) auf Neuanlage-Vorlage zurückgesetzt");
    }

    /**
     * Ersetzt alle Einträge in beschriftung_tasten durch die übergebene Liste.
     * Reihenfolge im Array wird als zeile_index (0, 1, 2, …) gespeichert. Für Drag & Drop.
     */
    public void replaceBeschriftungTasten(List<BeschriftungTastenRow> rows) {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            db.delete(TABLE_BESCHRIFTUNG_TASTEN, null, null);
            android.content.ContentValues values = new android.content.ContentValues();
            for (int i = 0; i < rows.size(); i++) {
                BeschriftungTastenRow r = rows.get(i);
                values.clear();
                values.put("zeile_index", i);
                values.put("c1", r.c1 != null ? r.c1 : "");
                values.put("c2", r.c2 != null ? r.c2 : "");
                values.put("c3", r.c3 != null ? r.c3 : "");
                values.put("c4", r.c4 != null ? r.c4 : "");
                values.put("c5", r.c5 != null ? r.c5 : "");
                values.put("c6", r.c6);
                values.put("c7", r.c7);
                values.put("c8", r.c8);
                values.put("c9", r.c9);
                values.put("c10", r.c10);
                values.put("c13", r.c13 != null ? r.c13 : "");
                if (r.sonderId != null) {
                    values.put("sonder_id", r.sonderId);
                }
                values.put("sound", r.sound != null ? r.sound : "");
                db.insert(TABLE_BESCHRIFTUNG_TASTEN, null, values);
            }
            db.delete(TABLE_LAYOUT_ITEM, null, null);
            ensureLayoutItemsFromBeschriftungTasten(db);
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }
    
    /**
     * Entity-Klasse für Vorschwingen-Konfiguration.
     */
    public static class VorschwingenConfig {
        public int id;
        public int vorschwingenRelais;           // Original Relais-Nummer (1-32)
        public int laeutenRelais;                // Original Kloeppel-Relais-Nummer (1-32)
        public int zeitSekunden;                 // Vorschwingzeit in Sekunden
        public int vorschwingenPlatine;          // Platine-Nummer (1-3)
        public int laeutenPlatine;               // Platine-Nummer für Kloeppel (1-3)
        public int vorschwingenRelaisBerechnet;  // Berechnete Relais-Nummer (für Kompatibilität)
        public int laeutenRelaisBerechnet;       // Berechnete Kloeppel-Relais-Nummer (für Kompatibilität)
        public boolean aktiv;                    // Aktiv-Status
        public int sortierung;                   // Sortierreihenfolge
    }
    
    /**
     * Lädt alle Vorschwingen-Konfigurationen aus der Datenbank.
     */
    public List<VorschwingenConfig> getAllVorschwingen() {
        List<VorschwingenConfig> vorschwingen = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        
        Cursor cursor = db.query(TABLE_VORSCHWINGEN_CONFIG,
            new String[]{"id", "vorschwingen_relais", "laeuten_relais", "zeit_sekunden", 
                        "vorschwingen_platine", "laeuten_platine", "vorschwingen_relais_berechnet",
                        "laeuten_relais_berechnet", "aktiv", "sortierung"},
            null, null, null, null, "sortierung ASC, id ASC");
        
        try {
            while (cursor.moveToNext()) {
                VorschwingenConfig config = new VorschwingenConfig();
                config.id = cursor.getInt(0);
                config.vorschwingenRelais = cursor.getInt(1);
                config.laeutenRelais = cursor.getInt(2);
                config.zeitSekunden = cursor.getInt(3);
                config.vorschwingenPlatine = cursor.getInt(4);
                config.laeutenPlatine = cursor.getInt(5);
                config.vorschwingenRelaisBerechnet = cursor.getInt(6);
                config.laeutenRelaisBerechnet = cursor.getInt(7);
                config.aktiv = cursor.getInt(8) == 1;
                config.sortierung = cursor.getInt(9);
                vorschwingen.add(config);
            }
        } finally {
            cursor.close();
        }
        
        return vorschwingen;
    }
    
    /**
     * Lädt eine Vorschwingen-Konfiguration nach ID.
     */
    public VorschwingenConfig getVorschwingenById(int id) {
        SQLiteDatabase db = getReadableDatabase();
        
        Cursor cursor = db.query(TABLE_VORSCHWINGEN_CONFIG,
            new String[]{"id", "vorschwingen_relais", "laeuten_relais", "zeit_sekunden", 
                        "vorschwingen_platine", "laeuten_platine", "vorschwingen_relais_berechnet",
                        "laeuten_relais_berechnet", "aktiv", "sortierung"},
            "id = ?",
            new String[]{String.valueOf(id)},
            null, null, null);
        
        try {
            if (cursor.moveToFirst()) {
                VorschwingenConfig config = new VorschwingenConfig();
                config.id = cursor.getInt(0);
                config.vorschwingenRelais = cursor.getInt(1);
                config.laeutenRelais = cursor.getInt(2);
                config.zeitSekunden = cursor.getInt(3);
                config.vorschwingenPlatine = cursor.getInt(4);
                config.laeutenPlatine = cursor.getInt(5);
                config.vorschwingenRelaisBerechnet = cursor.getInt(6);
                config.laeutenRelaisBerechnet = cursor.getInt(7);
                config.aktiv = cursor.getInt(8) == 1;
                config.sortierung = cursor.getInt(9);
                return config;
            }
        } finally {
            cursor.close();
        }
        
        return null;
    }
    
    /**
     * Speichert eine Vorschwingen-Konfiguration (INSERT oder UPDATE).
     */
    public void saveVorschwingen(VorschwingenConfig config) {
        SQLiteDatabase db = getWritableDatabase();
        
        // Berechne Relais-Nummern; 0 = identisch mit Läuten, -1 = Kein Vorschwing Relais (0 in Liste)
        int vorschwingenRelaisBerechnet = (config.vorschwingenRelais == 0 || config.vorschwingenRelais == -1) ? 0
                : config.vorschwingenRelais + ((config.vorschwingenPlatine - 1) * 32);
        int laeutenRelaisBerechnet = config.laeutenRelais + ((config.laeutenPlatine - 1) * 32);
        
        android.content.ContentValues values = new android.content.ContentValues();
        values.put("vorschwingen_relais", config.vorschwingenRelais);
        values.put("laeuten_relais", config.laeutenRelais);
        values.put("zeit_sekunden", config.zeitSekunden);
        values.put("vorschwingen_platine", config.vorschwingenPlatine);
        values.put("laeuten_platine", config.laeutenPlatine);
        values.put("vorschwingen_relais_berechnet", vorschwingenRelaisBerechnet);
        values.put("laeuten_relais_berechnet", laeutenRelaisBerechnet);
        values.put("aktiv", config.aktiv ? 1 : 0);
        values.put("sortierung", config.sortierung);
        values.put("updated_at", System.currentTimeMillis());
        
        if (config.id > 0) {
            // UPDATE
            int rowsUpdated = db.update(TABLE_VORSCHWINGEN_CONFIG, values, "id = ?", 
                new String[]{String.valueOf(config.id)});
            Log.d(TAG, "Vorschwingen ID " + config.id + " aktualisiert: " + rowsUpdated + " Zeilen");
        } else {
            // INSERT
            long newId = db.insert(TABLE_VORSCHWINGEN_CONFIG, null, values);
            config.id = (int) newId;
            Log.d(TAG, "Vorschwingen erstellt: ID " + config.id);
        }
    }
    
    /**
     * Löscht eine Vorschwingen-Konfiguration.
     */
    public void deleteVorschwingen(int id) {
        SQLiteDatabase db = getWritableDatabase();
        int rowsDeleted = db.delete(TABLE_VORSCHWINGEN_CONFIG, "id = ?", 
            new String[]{String.valueOf(id)});
        Log.d(TAG, "Vorschwingen ID " + id + " gelöscht: " + rowsDeleted + " Zeilen");
    }
    
    /**
     * Entity-Klasse für Schlagwerk-Konfiguration (Typ 1 und Typ 2).
     */
    public static class SchlagwerkConfig {
        public int id;
        public int typ;                          // 1 oder 2
        public String startZeit;                // z.B. "08:00"
        public String endeZeit;                  // z.B. "18:00"
        public int hammerStundenRelais;
        public int hammerViertelRelais;
        public int pauseViertelMs;
        public int pauseViertelZuStundeMs;
        public int halbstundeSchlaganzahl;       // 1 oder 2
        public int volleStundeAnzahl;            // 1-12
        public boolean schlagenWennMelodieLaeuft; // EIN=true, AUS=false
        public boolean keinSchlagwerkOstern;
        public boolean keinSchlagwerkKarfreitag;
        public boolean keinSchlagwerkKarsamstag;
        public boolean aktiv;
        public int sortierung;
    }
    
    private static final String[] SCHLAGWERK_CONFIG_COLUMNS = new String[]{
        "id", "typ", "start_zeit", "ende_zeit", "hammer_stunden_relais", "hammer_viertel_relais",
        "pause_viertel_ms", "pause_viertel_zu_stunde_ms", "halbstunde_schlaganzahl", "volle_stunde_anzahl",
        "schlagen_wenn_melodie_laeuft", "kein_schlagwerk_ostern", "kein_schlagwerk_karfreitag", "kein_schlagwerk_karsamstag",
        "aktiv", "sortierung"
    };
    
    /**
     * Lädt alle Schlagwerk-Konfigurationen (Typ 1 und Typ 2).
     */
    public List<SchlagwerkConfig> getAllSchlagwerkConfigs() {
        List<SchlagwerkConfig> list = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.query(TABLE_SCHLAGWERK_CONFIG, SCHLAGWERK_CONFIG_COLUMNS, null, null, null, null, "typ ASC");
        try {
            while (cursor.moveToNext()) {
                list.add(cursorToSchlagwerkConfig(cursor));
            }
        } finally {
            cursor.close();
        }
        return list;
    }
    
    /**
     * Lädt die Schlagwerk-Konfiguration für einen Typ (1 oder 2).
     */
    public SchlagwerkConfig getSchlagwerkConfigByTyp(int typ) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.query(TABLE_SCHLAGWERK_CONFIG, SCHLAGWERK_CONFIG_COLUMNS, "typ = ?", new String[]{String.valueOf(typ)}, null, null, null);
        try {
            if (cursor.moveToFirst()) {
                return cursorToSchlagwerkConfig(cursor);
            }
        } finally {
            cursor.close();
        }
        return null;
    }
    
    private static SchlagwerkConfig cursorToSchlagwerkConfig(Cursor cursor) {
        SchlagwerkConfig c = new SchlagwerkConfig();
        c.id = cursor.getInt(0);
        c.typ = cursor.getInt(1);
        c.startZeit = cursor.isNull(2) ? null : cursor.getString(2);
        c.endeZeit = cursor.isNull(3) ? null : cursor.getString(3);
        c.hammerStundenRelais = cursor.getInt(4);
        c.hammerViertelRelais = cursor.getInt(5);
        c.pauseViertelMs = cursor.getInt(6);
        c.pauseViertelZuStundeMs = cursor.getInt(7);
        c.halbstundeSchlaganzahl = cursor.getInt(8);
        c.volleStundeAnzahl = cursor.getInt(9);
        c.schlagenWennMelodieLaeuft = cursor.getInt(10) == 1;
        c.keinSchlagwerkOstern = cursor.getInt(11) == 1;
        c.keinSchlagwerkKarfreitag = cursor.getInt(12) == 1;
        c.keinSchlagwerkKarsamstag = cursor.getInt(13) == 1;
        c.aktiv = cursor.getInt(14) == 1;
        c.sortierung = cursor.getInt(15);
        return c;
    }
    
    /**
     * Speichert eine Schlagwerk-Konfiguration (INSERT oder UPDATE nach typ).
     * Wenn keine id gesetzt ist, wird nach typ gesucht und die vorhandene Zeile aktualisiert.
     */
    public void saveSchlagwerkConfig(SchlagwerkConfig config) {
        SQLiteDatabase db = getWritableDatabase();
        if (config.id <= 0) {
            SchlagwerkConfig existing = getSchlagwerkConfigByTyp(config.typ);
            if (existing != null) {
                config.id = existing.id;
            }
        }
        android.content.ContentValues values = new android.content.ContentValues();
        values.put("typ", config.typ);
        values.put("start_zeit", config.startZeit != null ? config.startZeit : "");
        values.put("ende_zeit", config.endeZeit != null ? config.endeZeit : "");
        values.put("hammer_stunden_relais", config.hammerStundenRelais);
        values.put("hammer_viertel_relais", config.hammerViertelRelais);
        values.put("pause_viertel_ms", config.pauseViertelMs);
        values.put("pause_viertel_zu_stunde_ms", config.pauseViertelZuStundeMs);
        values.put("halbstunde_schlaganzahl", config.halbstundeSchlaganzahl);
        values.put("volle_stunde_anzahl", config.volleStundeAnzahl);
        values.put("schlagen_wenn_melodie_laeuft", config.schlagenWennMelodieLaeuft ? 1 : 0);
        values.put("kein_schlagwerk_ostern", config.keinSchlagwerkOstern ? 1 : 0);
        values.put("kein_schlagwerk_karfreitag", config.keinSchlagwerkKarfreitag ? 1 : 0);
        values.put("kein_schlagwerk_karsamstag", config.keinSchlagwerkKarsamstag ? 1 : 0);
        values.put("aktiv", config.aktiv ? 1 : 0);
        values.put("sortierung", config.sortierung);
        values.put("updated_at", "CURRENT_TIMESTAMP");
        
        if (config.id > 0) {
            int rows = db.update(TABLE_SCHLAGWERK_CONFIG, values, "id = ?", new String[]{String.valueOf(config.id)});
            Log.d(TAG, "Schlagwerk-Config Typ " + config.typ + " aktualisiert: " + rows + " Zeilen");
        } else {
            long newId = db.insert(TABLE_SCHLAGWERK_CONFIG, null, values);
            config.id = (int) newId;
            Log.d(TAG, "Schlagwerk-Config Typ " + config.typ + " erstellt: ID " + config.id);
        }
    }
    
    /**
     * Migriert Vorschwingen-Daten aus System.xls Sheet 9 in die Datenbank.
     */
    public void migrateVorschwingenFromExcel(SQLiteDatabase db) {
        try {
            // Prüfe ob bereits Daten in der Datenbank vorhanden sind
            Cursor checkCursor = db.query(TABLE_VORSCHWINGEN_CONFIG, 
                new String[]{"COUNT(*) as count"}, null, null, null, null, null);
            boolean hasData = false;
            if (checkCursor.moveToFirst()) {
                hasData = checkCursor.getInt(0) > 0;
            }
            checkCursor.close();
            
            if (hasData) {
                Log.d(TAG, "Vorschwingen-Daten bereits in Datenbank vorhanden, überspringe Migration");
                return;
            }
            
            String sdCardPath = android.os.Environment.getExternalStorageDirectory().getPath();
            String systemXlsPath = sdCardPath + StaticConstants.excellSystemString;
            java.io.File systemFile = new java.io.File(systemXlsPath);
            
            if (!systemFile.exists()) {
                Log.d(TAG, "System.xls nicht gefunden, überspringe Vorschwingen-Migration");
                return;
            }
            
            ExcelRead excelread = new ExcelRead();
            excelread.openXlsSheet(systemXlsPath, 9); // Sheet 9 = Vorschwingen
            
            Log.d(TAG, "Migriere Vorschwingen-Daten aus System.xls Sheet 9");
            
            int zeilen = excelread.getCellZeilen();
            int sortierung = 0;
            
            // Ab Zeile 2 (Index 2) lesen
            for (int i = 2; i < zeilen; i++) {
                try {
                    // Spalte 0: Vorschwingen Relais
                    String vorschwingenRelaisStr = excelread.getCellString(0, i);
                    if (vorschwingenRelaisStr == null || vorschwingenRelaisStr.trim().isEmpty()) {
                        break; // Leere Zeile = Ende
                    }
                    
                    // Spalte 1: Läuten Relais
                    String laeutenRelaisStr = excelread.getCellString(1, i);
                    if (laeutenRelaisStr == null || laeutenRelaisStr.trim().isEmpty()) {
                        break;
                    }
                    
                    // Spalte 2: Zeit/sec
                    String zeitStr = excelread.getCellString(2, i);
                    if (zeitStr == null || zeitStr.trim().isEmpty()) {
                        break;
                    }
                    
                    // Spalte 3: Vorschwingen Platine
                    String vorschwingenPlatineStr = excelread.getCellString(3, i);
                    if (vorschwingenPlatineStr == null || vorschwingenPlatineStr.trim().isEmpty()) {
                        break;
                    }
                    
                    // Spalte 4: Läuten Platine
                    String laeutenPlatineStr = excelread.getCellString(4, i);
                    if (laeutenPlatineStr == null || laeutenPlatineStr.trim().isEmpty()) {
                        break;
                    }
                    
                    VorschwingenConfig config = new VorschwingenConfig();
                    config.vorschwingenRelais = Integer.parseInt(vorschwingenRelaisStr.trim());
                    config.laeutenRelais = Integer.parseInt(laeutenRelaisStr.trim());
                    config.zeitSekunden = Integer.parseInt(zeitStr.trim());
                    config.vorschwingenPlatine = Integer.parseInt(vorschwingenPlatineStr.trim());
                    config.laeutenPlatine = Integer.parseInt(laeutenPlatineStr.trim());
                    config.aktiv = true;
                    config.sortierung = sortierung++;
                    
                    // Berechne Relais-Nummern
                    config.vorschwingenRelaisBerechnet = config.vorschwingenRelais + ((config.vorschwingenPlatine - 1) * 32);
                    config.laeutenRelaisBerechnet = config.laeutenRelais + ((config.laeutenPlatine - 1) * 32);
                    
                    // In Datenbank speichern
                    android.content.ContentValues values = new android.content.ContentValues();
                    values.put("vorschwingen_relais", config.vorschwingenRelais);
                    values.put("laeuten_relais", config.laeutenRelais);
                    values.put("zeit_sekunden", config.zeitSekunden);
                    values.put("vorschwingen_platine", config.vorschwingenPlatine);
                    values.put("laeuten_platine", config.laeutenPlatine);
                    values.put("vorschwingen_relais_berechnet", config.vorschwingenRelaisBerechnet);
                    values.put("laeuten_relais_berechnet", config.laeutenRelaisBerechnet);
                    values.put("aktiv", 1);
                    values.put("sortierung", config.sortierung);
                    
                    db.insert(TABLE_VORSCHWINGEN_CONFIG, null, values);
                    Log.d(TAG, "Vorschwingen migriert: Vorschwingen Relais=" + config.vorschwingenRelais + 
                          " (Platine " + config.vorschwingenPlatine + ", berechnet=" + config.vorschwingenRelaisBerechnet + 
                          "), Läuten Relais=" + config.laeutenRelais + 
                          " (Platine " + config.laeutenPlatine + ", berechnet=" + config.laeutenRelaisBerechnet + 
                          "), Zeit=" + config.zeitSekunden + " Sekunden");
                } catch (Exception e) {
                    Log.w(TAG, "Fehler beim Migrieren von Vorschwingen Zeile " + i, e);
                }
            }
            
            excelread.closeWorkbook();
            Log.d(TAG, "Vorschwingen-Migration abgeschlossen");
            
        } catch (Exception e) {
            Log.e(TAG, "Fehler bei Migration von Vorschwingen-Daten aus Excel", e);
        }
    }
    
    /**
     * Lädt alle Tagtypen aus der Datenbank.
     */
    public List<Tagtyp> getAllTagtypen() {
        List<Tagtyp> tagtypen = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        
        Cursor cursor = db.query(TABLE_TAGTYPEN,
            new String[]{"id", "name", "programm_typ"},
            null, null, null, null, "name ASC");
        
        try {
            while (cursor.moveToNext()) {
                Tagtyp tagtyp = new Tagtyp();
                tagtyp.id = cursor.getInt(0);
                tagtyp.name = cursor.getString(1);
                tagtyp.programmTyp = cursor.getString(2);
                tagtypen.add(tagtyp);
            }
        } finally {
            cursor.close();
        }
        
        return tagtypen;
    }
    
    /**
     * Lädt alle Tagtypen mit dem angegebenen programm_typ (z. B. "festtag_fest", "festtag_variabel").
     * Wird verwendet, um feste/variable Festtage-Listen ohne System.xls Sheet 12/13 aus der DB zu füllen.
     */
    public List<Tagtyp> getTagtypenByProgrammTyp(String programmTyp) {
        if (programmTyp == null || programmTyp.trim().isEmpty()) {
            return new ArrayList<>();
        }
        List<Tagtyp> tagtypen = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.query(TABLE_TAGTYPEN,
            new String[]{"id", "name", "programm_typ"},
            "programm_typ = ?",
            new String[]{programmTyp.trim()},
            null, null, "name ASC");
        try {
            while (cursor.moveToNext()) {
                Tagtyp tagtyp = new Tagtyp();
                tagtyp.id = cursor.getInt(0);
                tagtyp.name = cursor.getString(1);
                tagtyp.programmTyp = cursor.getString(2);
                tagtypen.add(tagtyp);
            }
        } finally {
            cursor.close();
        }
        return tagtypen;
    }
    
    /**
     * Entfernt .xls Endung vom Tagtyp-Namen (falls vorhanden).
     */
    private String removeXlsExtension(String name) {
        if (name == null) return null;
        String trimmed = name.trim();
        if (trimmed.toLowerCase().endsWith(".xls")) {
            return trimmed.substring(0, trimmed.length() - 4);
        }
        return trimmed;
    }
    
    /**
     * Lädt einen Tagtyp nach Name.
     * Unterstützt sowohl Namen mit als auch ohne .xls Endung (für Rückwärtskompatibilität).
     */
    public Tagtyp getTagtypByName(String name) {
        SQLiteDatabase db = getReadableDatabase();
        
        Log.d(TAG, "getTagtypByName: Suche Tagtyp mit Name: '" + name + "'");
        
        // Versuche zuerst exakte Suche
        Cursor cursor = db.query(TABLE_TAGTYPEN,
            new String[]{"id", "name", "programm_typ"},
            "name = ?",
            new String[]{name},
            null, null, null);
        
        Log.d(TAG, "getTagtypByName: Gefundene Zeilen (exakt): " + cursor.getCount());
        
        try {
            if (cursor.moveToFirst()) {
                Tagtyp tagtyp = new Tagtyp();
                tagtyp.id = cursor.getInt(0);
                tagtyp.name = cursor.getString(1);
                tagtyp.programmTyp = cursor.getString(2);
                Log.d(TAG, "getTagtypByName: Tagtyp gefunden - ID: " + tagtyp.id + ", Name: '" + tagtyp.name + "', Typ: " + tagtyp.programmTyp);
                return tagtyp;
            }
        } finally {
            cursor.close();
        }
        
        // Falls nicht gefunden: Versuche ohne .xls (wenn Name mit .xls endet)
        String nameWithoutXls = removeXlsExtension(name);
        if (nameWithoutXls != null && !nameWithoutXls.equals(name)) {
            cursor = db.query(TABLE_TAGTYPEN,
                new String[]{"id", "name", "programm_typ"},
                "name = ?",
                new String[]{nameWithoutXls},
                null, null, null);
            
            Log.d(TAG, "getTagtypByName: Gefundene Zeilen (ohne .xls): " + cursor.getCount());
            
            try {
                if (cursor.moveToFirst()) {
                    Tagtyp tagtyp = new Tagtyp();
                    tagtyp.id = cursor.getInt(0);
                    tagtyp.name = cursor.getString(1);
                    tagtyp.programmTyp = cursor.getString(2);
                    Log.d(TAG, "getTagtypByName: Tagtyp gefunden (ohne .xls) - ID: " + tagtyp.id + ", Name: '" + tagtyp.name + "', Typ: " + tagtyp.programmTyp);
                    return tagtyp;
                }
            } finally {
                cursor.close();
            }
        }
        
        // Falls nicht gefunden: Versuche mit .xls (wenn Name nicht mit .xls endet)
        if (nameWithoutXls != null && nameWithoutXls.equals(name) && !name.toLowerCase().endsWith(".xls")) {
            String nameWithXls = name + ".xls";
            cursor = db.query(TABLE_TAGTYPEN,
                new String[]{"id", "name", "programm_typ"},
                "name = ?",
                new String[]{nameWithXls},
                null, null, null);
            
            Log.d(TAG, "getTagtypByName: Gefundene Zeilen (mit .xls): " + cursor.getCount());
            
            try {
                if (cursor.moveToFirst()) {
                    Tagtyp tagtyp = new Tagtyp();
                    tagtyp.id = cursor.getInt(0);
                    tagtyp.name = cursor.getString(1);
                    tagtyp.programmTyp = cursor.getString(2);
                    Log.d(TAG, "getTagtypByName: Tagtyp gefunden (mit .xls) - ID: " + tagtyp.id + ", Name: '" + tagtyp.name + "', Typ: " + tagtyp.programmTyp);
                    return tagtyp;
                }
            } finally {
                cursor.close();
            }
        }
        
        Log.d(TAG, "getTagtypByName: Kein Tagtyp gefunden für: '" + name + "'");
        return null;
    }
    
    /**
     * Speichert einen Tagtyp (INSERT oder UPDATE).
     * Entfernt automatisch .xls Endung vom Namen vor dem Speichern.
     */
    public Tagtyp saveTagtyp(String name, String programmTyp) {
        SQLiteDatabase db = getWritableDatabase();
        
        // Entferne .xls Endung vom Namen
        String nameWithoutXls = removeXlsExtension(name);
        
        // Prüfe ob Tagtyp bereits existiert (unterstützt beide Varianten)
        Tagtyp existing = getTagtypByName(nameWithoutXls);
        
        if (existing != null) {
            // UPDATE - verwende den Namen aus der DB (kann mit oder ohne .xls sein)
            android.content.ContentValues values = new android.content.ContentValues();
            values.put("programm_typ", programmTyp);
            values.put("updated_at", "CURRENT_TIMESTAMP");
            
            // Aktualisiere auch den Namen, falls er noch .xls enthält
            if (existing.name.toLowerCase().endsWith(".xls") && !nameWithoutXls.toLowerCase().endsWith(".xls")) {
                values.put("name", nameWithoutXls);
                existing.name = nameWithoutXls;
            }
            
            db.update(TABLE_TAGTYPEN, values, "id = ?", new String[]{String.valueOf(existing.id)});
            Log.d(TAG, "Tagtyp aktualisiert: " + existing.name);
            existing.programmTyp = programmTyp;
            return existing;
        } else {
            // INSERT - speichere ohne .xls
            android.content.ContentValues values = new android.content.ContentValues();
            values.put("name", nameWithoutXls);
            values.put("programm_typ", programmTyp);
            
            long id = db.insert(TABLE_TAGTYPEN, null, values);
            Log.d(TAG, "Tagtyp erstellt: " + nameWithoutXls + " (ID: " + id + ")");
            
            Tagtyp tagtyp = new Tagtyp();
            tagtyp.id = (int) id;
            tagtyp.name = nameWithoutXls;
            tagtyp.programmTyp = programmTyp;
            return tagtyp;
        }
    }
    
    /**
     * Löscht einen Tagtyp (und alle zugehörigen Programme durch CASCADE).
     * Unterstützt sowohl Namen mit als auch ohne .xls Endung.
     */
    public void deleteTagtyp(String name) {
        SQLiteDatabase db = getWritableDatabase();
        Tagtyp tagtyp = getTagtypByName(name);
        if (tagtyp != null) {
            // Zuerst alle Programme dieses Tagtyps löschen (SQLite CASCADE ist oft inaktiv)
            int progDeleted = db.delete(TABLE_PROGRAMME, "tagtyp_id = ?", new String[]{String.valueOf(tagtyp.id)});
            Log.d(TAG, "deleteTagtyp: " + progDeleted + " Programme für Tagtyp '" + tagtyp.name + "' gelöscht");
            db.delete(TABLE_TAGTYPEN, "id = ?", new String[]{String.valueOf(tagtyp.id)});
            Log.d(TAG, "Tagtyp gelöscht: " + tagtyp.name);
        } else {
            Log.w(TAG, "Tagtyp nicht gefunden zum Löschen: " + name);
        }
    }
    
    /**
     * Lädt alle Programme eines Tagtyps aus der Datenbank.
     */
    public List<Programm> getProgrammeByTagtyp(String tagtypName) {
        List<Programm> programme = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        
        Log.d(TAG, "getProgrammeByTagtyp: Suche Tagtyp: '" + tagtypName + "'");
        
        // Hole Tagtyp-ID
        Tagtyp tagtyp = getTagtypByName(tagtypName);
        
        // Wenn Tagtyp nicht in DB existiert (z. B. wurde gelöscht), keine automatische Neuanlage –
        // sonst würde ein gelöschter Programmtag bei jedem Aufruf (TagesSuche, API, UhrThread) wieder erscheinen.
        if (tagtyp == null) {
            Log.d(TAG, "getProgrammeByTagtyp: Tagtyp nicht in DB: '" + tagtypName + "', gebe leere Liste zurück (kein Auto-Create)");
            return programme;
        }
        
        Cursor cursor = db.query(TABLE_PROGRAMME,
            new String[]{"id", "zeile_index", "startzeit", "funktion", "melodie_name", "dauer_heizung",
                        "montag", "dienstag", "mittwoch", "donnerstag", "freitag", "samstag", "sonntag",
                        "immer", "periodisch", "start_datum", "ende_datum", "verknuepfte_taste", "prioritaet",
                        "festtag_datum", "festtag_jahr"},
            "tagtyp_id = ?",
            new String[]{String.valueOf(tagtyp.id)},
            null, null, "zeile_index ASC");
        
        Log.d(TAG, "getProgrammeByTagtyp: Suche Programme für Tagtyp ID " + tagtyp.id + " (Name: " + tagtypName + "), gefundene Zeilen: " + cursor.getCount());
        
        try {
            while (cursor.moveToNext()) {
                Programm programm = new Programm();
                programm.setId(cursor.getInt(1)); // zeile_index als ID
                programm.setStartzeit(cursor.getString(2));
                programm.setFunktion(cursor.getString(3));
                programm.setMelodieName(cursor.isNull(4) ? null : cursor.getString(4));
                programm.setDauerHeizung(cursor.isNull(5) ? null : cursor.getString(5));
                programm.setMontag(cursor.getInt(6) == 1);
                programm.setDienstag(cursor.getInt(7) == 1);
                programm.setMittwoch(cursor.getInt(8) == 1);
                programm.setDonnerstag(cursor.getInt(9) == 1);
                programm.setFreitag(cursor.getInt(10) == 1);
                programm.setSamstag(cursor.getInt(11) == 1);
                programm.setSonntag(cursor.getInt(12) == 1);
                programm.setImmer(cursor.getInt(13) == 1);
                programm.setPeriodisch(cursor.getInt(14));
                programm.setStartDatum(cursor.isNull(15) ? null : cursor.getString(15));
                programm.setEndeDatum(cursor.isNull(16) ? null : cursor.getString(16));
                programm.setVerknuepfteTaste(cursor.isNull(17) ? null : cursor.getString(17));
                programm.setPrioritaet(cursor.getInt(18));
                programm.setFesttagDatum(cursor.isNull(19) ? null : cursor.getString(19));
                programm.setFesttagJahr(cursor.isNull(20) ? null : cursor.getInt(20));
                programm.setProgrammTyp(tagtyp.programmTyp);
                programme.add(programm);
            }
        } finally {
            cursor.close();
        }
        
        // Kein automatisches Excel-Nachladen mehr – nur DB. Excel nur bei manuellem Import.
        if (programme.isEmpty()) {
            Log.d(TAG, "getProgrammeByTagtyp: Keine Programme in DB für " + tagtypName + " (Excel wird nicht mehr automatisch geladen)");
        }
        
        return programme;
    }
    
    /**
     * Versucht, Programme aus einer Excel-Datei zu lesen und in die DB zu übernehmen.
     * @param tagtypName Tagtyp-Name (z.B. "Normalprogramm")
     * @param filePath   voller Pfad zur .xls-Datei
     * @param programme  Liste, die gefüllt wird (wird nicht geleert)
     * @return true, wenn mindestens ein Programm migriert wurde
     */
    private boolean tryMigrateProgrammeFromExcelFile(String tagtypName, String filePath, List<Programm> programme) {
        try {
            ExcelRead excelRead = new ExcelRead();
            excelRead.openXls(filePath);
            int zeilen = excelRead.getCellZeilen();
            int startSize = programme.size();
            for (int zeile = 3; zeile < zeilen; zeile++) {
                try {
                    String startzeit = excelRead.getCellString(TagesSuche.SPALTE_A_STARTZEIT, zeile);
                    if (startzeit == null || startzeit.trim().isEmpty()) {
                        break;
                    }
                    Programm programm = excelZeileToProgramm(excelRead, zeile);
                    programm.setId(zeile);
                    saveProgramm(tagtypName, programm);
                    programme.add(programm);
                } catch (Exception e) {
                    Log.w(TAG, "Fehler beim Migrieren von Zeile " + zeile + " aus " + tagtypName, e);
                }
            }
            excelRead.closeWorkbook();
            return programme.size() > startSize;
        } catch (Exception e) {
            Log.e(TAG, "Fehler beim Lesen der Excel-Datei: " + tagtypName + " (" + filePath + "): " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Speichert ein Programm (INSERT oder UPDATE).
     */
    public boolean saveProgramm(String tagtypName, Programm programm) {
        SQLiteDatabase db = getWritableDatabase();
        
        // Entferne .xls Endung vom Tagtyp-Namen
        String tagtypNameClean = removeXlsExtension(tagtypName);
        
        // Hole oder erstelle Tagtyp
        Tagtyp tagtyp = getTagtypByName(tagtypNameClean);
        if (tagtyp == null) {
            // Bestimme Programmtyp basierend auf Dateinamen oder verwende "normal"
            String programmTyp = "normal";
            if (tagtypNameClean.contains("Festtag") || tagtypNameClean.contains("festtag")) {
                programmTyp = "festtag_variabel"; // Default, kann später angepasst werden
            }
            tagtyp = saveTagtyp(tagtypNameClean, programmTyp);
        }
        
        // Prüfe ob Programm bereits existiert
        Cursor cursor = db.query(TABLE_PROGRAMME,
            new String[]{"id"},
            "tagtyp_id = ? AND zeile_index = ?",
            new String[]{String.valueOf(tagtyp.id), String.valueOf(programm.getId())},
            null, null, null);
        
        boolean exists = cursor.moveToFirst();
        cursor.close();
        
        android.content.ContentValues values = new android.content.ContentValues();
        values.put("tagtyp_id", tagtyp.id);
        values.put("zeile_index", programm.getId());
        values.put("startzeit", programm.getStartzeit());
        values.put("funktion", programm.getFunktion());
        values.put("melodie_name", programm.getMelodieName());
        values.put("dauer_heizung", programm.getDauerHeizung());
        values.put("montag", programm.isMontag() ? 1 : 0);
        values.put("dienstag", programm.isDienstag() ? 1 : 0);
        values.put("mittwoch", programm.isMittwoch() ? 1 : 0);
        values.put("donnerstag", programm.isDonnerstag() ? 1 : 0);
        values.put("freitag", programm.isFreitag() ? 1 : 0);
        values.put("samstag", programm.isSamstag() ? 1 : 0);
        values.put("sonntag", programm.isSonntag() ? 1 : 0);
        values.put("immer", programm.isImmer() ? 1 : 0);
        values.put("periodisch", programm.getPeriodisch());
        values.put("start_datum", programm.getStartDatum());
        values.put("ende_datum", programm.getEndeDatum());
        String verknuepfteTaste = programm.getVerknuepfteTaste();
        values.put("verknuepfte_taste", verknuepfteTaste);
        Log.d(TAG, "saveProgramm: Speichere Verknüpfte Taste für Zeile " + programm.getId() + ": '" + verknuepfteTaste + "'");
        values.put("prioritaet", programm.getPrioritaet());
        values.put("festtag_datum", programm.getFesttagDatum());
        if (programm.getFesttagJahr() != null) {
            values.put("festtag_jahr", programm.getFesttagJahr());
        }
        values.put("updated_at", "CURRENT_TIMESTAMP");
        
        if (exists) {
            db.update(TABLE_PROGRAMME, values, 
                "tagtyp_id = ? AND zeile_index = ?",
                new String[]{String.valueOf(tagtyp.id), String.valueOf(programm.getId())});
            Log.d(TAG, "Programm aktualisiert: Tagtyp=" + tagtypName + ", Zeile=" + programm.getId());
        } else {
            db.insert(TABLE_PROGRAMME, null, values);
            Log.d(TAG, "Programm erstellt: Tagtyp=" + tagtypName + ", Zeile=" + programm.getId());
        }
        StaticVariable.programmeDatabaseChanged = true;
        return true;
    }
    
    /**
     * Löscht ein Programm.
     */
    public boolean deleteProgramm(String tagtypName, int zeileIndex) {
        SQLiteDatabase db = getWritableDatabase();
        
        // Entferne .xls Endung für Suche
        String tagtypNameClean = removeXlsExtension(tagtypName);
        Tagtyp tagtyp = getTagtypByName(tagtypNameClean);
        if (tagtyp == null) {
            return false;
        }
        
        int deleted = db.delete(TABLE_PROGRAMME,
            "tagtyp_id = ? AND zeile_index = ?",
            new String[]{String.valueOf(tagtyp.id), String.valueOf(zeileIndex)});
        if (deleted > 0) StaticVariable.programmeDatabaseChanged = true;
        Log.d(TAG, "Programm gelöscht: Tagtyp=" + tagtypName + ", Zeile=" + zeileIndex);
        return deleted > 0;
    }
    
    /**
     * Liefert alle unterschiedlichen, nicht leeren Werte von verknuepfte_taste aus der Programm-Tabelle
     * (also alle in Programmtagen referenzierten Verknüpften Tasten).
     */
    public List<String> getVerknuepfteTastenInProgrammen() {
        List<String> names = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery(
            "SELECT DISTINCT trim(verknuepfte_taste) FROM " + TABLE_PROGRAMME +
            " WHERE verknuepfte_taste IS NOT NULL AND trim(verknuepfte_taste) <> ''", null);
        try {
            while (cursor.moveToNext()) {
                String name = cursor.getString(0);
                if (name != null && !name.isEmpty() && !names.contains(name)) {
                    names.add(name);
                }
            }
        } finally {
            cursor.close();
        }
        return names;
    }
    
    /**
     * Liefert alle Beschriftungen (c2) aus der Tastenconfig, bei denen der Tastentyp "Verknüpft" ist.
     */
    public List<String> getVerknuepfteTastenInConfig() {
        List<String> names = new ArrayList<>();
        List<BeschriftungTastenRow> rows = getBeschriftungTasten();
        for (BeschriftungTastenRow r : rows) {
            String c1 = r.c1 != null ? r.c1.trim() : "";
            if ("verknuepft".equalsIgnoreCase(c1) || "Verknüpft".equals(c1)) {
                String c2 = r.c2 != null ? r.c2.trim() : "";
                if (!c2.isEmpty() && !names.contains(c2)) {
                    names.add(c2);
                }
            }
        }
        return names;
    }

    public int renameVerknuepfteTasteInProgrammen(String oldName, String newName) {
        String oldValue = oldName != null ? oldName.trim() : "";
        String newValue = newName != null ? newName.trim() : "";
        if (oldValue.isEmpty() || newValue.isEmpty() || oldValue.equals(newValue)) {
            return 0;
        }
        SQLiteDatabase db = getWritableDatabase();
        android.content.ContentValues values = new android.content.ContentValues();
        values.put("verknuepfte_taste", newValue);
        int updated = db.update(TABLE_PROGRAMME, values,
                "trim(verknuepfte_taste) = ?",
                new String[]{oldValue});
        if (updated > 0) {
            StaticVariable.programmeDatabaseChanged = true;
            Log.d(TAG, "Verknüpfte Taste in Programmen umbenannt: '" + oldValue + "' -> '" + newValue + "', Treffer=" + updated);
        }
        return updated;
    }
    
    /**
     * Prüft, ob alle in den Programmtagen verwendeten Verknüpften Tasten in der Tastenconfig vorhanden sind.
     * @return Liste der Tastennamen, die in Programmen vorkommen, aber nicht in der Beschriftung-Tasten-Config
     *         (leer = alles in Ordnung)
     */
    public List<String> getFehlendeVerknuepfteTasten() {
        List<FehlendeVerknuepfteTaste> mitTag = getFehlendeVerknuepfteTastenMitTag();
        List<String> fehlend = new ArrayList<>();
        for (FehlendeVerknuepfteTaste f : mitTag) {
            fehlend.add(f.taste);
        }
        return fehlend;
    }

    /**
     * Ergebnis-Eintrag für fehlende Verknüpfte Tasten inkl. Programmtag(en).
     */
    public static class FehlendeVerknuepfteTaste {
        public String taste;
        public List<String> tagtypen = new ArrayList<>();
    }

    /**
     * Prüft, ob alle in den Programmtagen verwendeten Verknüpften Tasten in der Tastenconfig vorhanden sind.
     * @return Liste mit Tastennamen und den Tagtypen, in denen die Taste verwendet wird (leer = alles in Ordnung)
     */
    public List<FehlendeVerknuepfteTaste> getFehlendeVerknuepfteTastenMitTag() {
        List<String> inConfig = getVerknuepfteTastenInConfig();
        List<FehlendeVerknuepfteTaste> fehlend = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery(
            "SELECT DISTINCT trim(p.verknuepfte_taste), t.name FROM " + TABLE_PROGRAMME + " p " +
            "JOIN " + TABLE_TAGTYPEN + " t ON p.tagtyp_id = t.id " +
            "WHERE p.verknuepfte_taste IS NOT NULL AND trim(p.verknuepfte_taste) <> ''", null);
        java.util.Map<String, java.util.Set<String>> tasteToTagtypen = new java.util.LinkedHashMap<>();
        try {
            while (cursor.moveToNext()) {
                String taste = cursor.getString(0);
                String tagtypName = cursor.getString(1);
                if (taste == null || taste.isEmpty()) continue;
                if (!tasteToTagtypen.containsKey(taste)) {
                    tasteToTagtypen.put(taste, new java.util.LinkedHashSet<String>());
                }
                if (tagtypName != null && !tagtypName.isEmpty()) {
                    tasteToTagtypen.get(taste).add(tagtypName);
                }
            }
        } finally {
            cursor.close();
        }
        for (java.util.Map.Entry<String, java.util.Set<String>> e : tasteToTagtypen.entrySet()) {
            String taste = e.getKey();
            if (inConfig.contains(taste)) continue;
            FehlendeVerknuepfteTaste f = new FehlendeVerknuepfteTaste();
            f.taste = taste;
            f.tagtypen = new ArrayList<>(e.getValue());
            fehlend.add(f);
        }
        return fehlend;
    }
    
    /**
     * Prüft beim App-Start, ob alle Tagtypen Programme in der Datenbank haben.
     * Wenn nicht, werden sie automatisch aus Excel migriert.
     * Diese Methode sollte beim App-Start aufgerufen werden.
     * Kann auch manuell über die Web-UI aufgerufen werden, um alle Programme zu importieren.
     */
    public void ensureAllTagtypenHaveProgramme() {
        try {
            String sdCardPath = android.os.Environment.getExternalStorageDirectory().getPath();
            String programmtagePath = sdCardPath + "/Turmtechnik/Programmtage/";
            java.io.File programmtageDir = new java.io.File(programmtagePath);
            
            if (!programmtageDir.exists() || !programmtageDir.isDirectory()) {
                Log.d(TAG, "Programmtage-Ordner nicht gefunden, überspringe Prüfung");
                return;
            }
            
            java.io.File[] files = programmtageDir.listFiles();
            if (files == null) {
                return;
            }
            
            Log.d(TAG, "Prüfe ob alle Tagtypen Programme in der Datenbank haben");
            
            int migrated = 0;
            int checked = 0;
            
            for (java.io.File file : files) {
                if (!file.isFile() || !file.getName().toLowerCase().endsWith(".xls")) {
                    continue;
                }
                
                String fileName = file.getName();
                checked++;
                
                // Prüfe ob Tagtyp in DB existiert
                Tagtyp tagtyp = getTagtypByName(fileName);
                if (tagtyp == null) {
                    // Tagtyp existiert nicht, erstelle ihn und migriere Programme
                    Log.d(TAG, "Tagtyp nicht in DB: " + fileName + ", migriere...");
                    migrateTagtypAndProgrammeFromFile(file);
                    migrated++;
                } else {
                    // Tagtyp existiert, prüfe ob Programme vorhanden sind
                    List<Programm> programme = getProgrammeByTagtyp(fileName);
                    if (programme == null || programme.isEmpty()) {
                        // Keine Programme in DB, migriere aus Excel
                        Log.d(TAG, "Tagtyp " + fileName + " hat keine Programme in DB, migriere aus Excel...");
                        migrateTagtypAndProgrammeFromFile(file);
                        migrated++;
                    }
                }
            }
            
            Log.d(TAG, "Prüfung abgeschlossen: " + checked + " Tagtypen geprüft, " + migrated + " migriert");
            
        } catch (Exception e) {
            Log.e(TAG, "Fehler bei Prüfung der Tagtypen-Programme", e);
        }
    }
    
    /**
     * Lädt Programme für einen Tagtyp aus Excel neu und aktualisiert die Datenbank.
     * Diese Methode kann vom Programmeditor aufgerufen werden, um Programme nach Änderungen in Excel neu zu laden.
     * @param tagtypName Name des Tagtyps (Dateiname)
     * @return Anzahl der migrierten Programme
     */
    public int reloadProgrammeFromExcel(String tagtypName) {
        try {
            String sdCardPath = android.os.Environment.getExternalStorageDirectory().getPath();
            String programmtagePath = sdCardPath + "/Turmtechnik/Programmtage/";
            java.io.File file = new java.io.File(programmtagePath + tagtypName);
            
            if (!file.exists() || !file.isFile()) {
                Log.w(TAG, "Datei nicht gefunden für Reload: " + tagtypName);
                return 0;
            }
            
            // Lösche alle bestehenden Programme für diesen Tagtyp
            // Entferne .xls Endung für Suche
            String tagtypNameClean = removeXlsExtension(tagtypName);
            Tagtyp tagtyp = getTagtypByName(tagtypNameClean);
            if (tagtyp != null) {
                SQLiteDatabase db = getWritableDatabase();
                int deleted = db.delete(TABLE_PROGRAMME, "tagtyp_id = ?", new String[]{String.valueOf(tagtyp.id)});
                Log.d(TAG, "Gelöscht: " + deleted + " alte Programme für " + tagtyp.name);
            }
            
            // Migriere neu aus Excel
            migrateTagtypAndProgrammeFromFile(file);
            
            // Zähle migrierte Programme
            List<Programm> programme = getProgrammeByTagtyp(tagtypName);
            return programme != null ? programme.size() : 0;
            
        } catch (Exception e) {
            Log.e(TAG, "Fehler beim Neuladen von Programmen aus Excel: " + tagtypName, e);
            return 0;
        }
    }
    
    /**
     * Migriert einen einzelnen Tagtyp und seine Programme aus einer Excel-Datei.
     */
    private void migrateTagtypAndProgrammeFromFile(java.io.File file) {
        try {
            String fileName = file.getName();
            String filePath = file.getAbsolutePath();
            
            // Bestimme Programmtyp basierend auf Dateinamen
            String programmTyp = "normal";
            if (fileName.contains("Festtag") || fileName.contains("festtag")) {
                // Versuche festtag_variabel oder festtag_fest zu bestimmen
                try {
                    java.util.Vector<String> fileNamesVariabel = tom.turmtechnik.RepositoryFactory.getTagtypFileNames("festtag_variabel");
                    for (String filePath2 : fileNamesVariabel) {
                        String fileName2 = filePath2.substring(filePath2.lastIndexOf("/") + 1);
                        if (fileName2.equals(fileName)) {
                            programmTyp = "festtag_variabel";
                            break;
                        }
                    }
                    
                    if (programmTyp.equals("normal")) {
                        java.util.Vector<String> fileNamesFest = tom.turmtechnik.RepositoryFactory.getTagtypFileNames("festtag_fest");
                        for (String filePath2 : fileNamesFest) {
                            String fileName2 = filePath2.substring(filePath2.lastIndexOf("/") + 1);
                            if (fileName2.equals(fileName)) {
                                programmTyp = "festtag_fest";
                                break;
                            }
                        }
                    }
                } catch (Exception e) {
                    Log.w(TAG, "Fehler beim Bestimmen des Programmtyps, verwende festtag_variabel", e);
                    programmTyp = "festtag_variabel";
                }
            }
            
            // Erstelle oder hole Tagtyp (entfernt automatisch .xls Endung)
            Tagtyp tagtyp = saveTagtyp(fileName, programmTyp);
            
            // Migriere Programme aus dieser Datei
            ExcelRead excelRead = new ExcelRead();
            excelRead.openXls(filePath);
            
            int zeilen = excelRead.getCellZeilen();
            int programmCount = 0;
            
            // Beginne ab Zeile 4 (Index 3)
            for (int zeile = 3; zeile < zeilen; zeile++) {
                try {
                    String startzeit = excelRead.getCellString(TagesSuche.SPALTE_A_STARTZEIT, zeile);
                    if (startzeit == null || startzeit.trim().isEmpty()) {
                        break; // Ende der Tabelle
                    }
                    
                    // Konvertiere Excel-Zeile zu Programm-Objekt
                    Programm programm = excelZeileToProgramm(excelRead, zeile);
                    programm.setId(zeile); // Zeile als ID
                    
                    // Speichere in Datenbank
                    saveProgramm(fileName, programm);
                    programmCount++;
                } catch (Exception e) {
                    Log.w(TAG, "Fehler beim Migrieren von Zeile " + zeile + " aus " + fileName, e);
                }
            }
            
            excelRead.closeWorkbook();
            Log.d(TAG, "Tagtyp " + fileName + " migriert: " + programmCount + " Programme");
            
        } catch (Exception e) {
            Log.e(TAG, "Fehler beim Migrieren von Tagtyp: " + file.getName(), e);
        }
    }
    
    /**
     * Migriert Tagtypen und Programme aus Excel-Dateien in die Datenbank.
     * Liest alle .xls Dateien aus /Turmtechnik/Programmtage/ und migriert sie.
     */
    private void migrateTagtypenAndProgrammeFromExcel(SQLiteDatabase db) {
        try {
            String sdCardPath = android.os.Environment.getExternalStorageDirectory().getPath();
            String programmtagePath = sdCardPath + "/Turmtechnik/Programmtage/";
            java.io.File programmtageDir = new java.io.File(programmtagePath);
            
            if (!programmtageDir.exists() || !programmtageDir.isDirectory()) {
                Log.d(TAG, "Programmtage-Ordner nicht gefunden, überspringe Migration");
                return;
            }
            
            java.io.File[] files = programmtageDir.listFiles();
            if (files == null) {
                return;
            }
            
            Log.d(TAG, "Migriere Tagtypen und Programme aus Excel-Dateien");
            
            for (java.io.File file : files) {
                if (!file.isFile() || !file.getName().toLowerCase().endsWith(".xls")) {
                    continue;
                }
                
                String fileName = file.getName();
                String filePath = file.getAbsolutePath();
                
                // Bestimme Programmtyp basierend auf Dateinamen
                String programmTyp = "normal";
                if (fileName.contains("Festtag") || fileName.contains("festtag")) {
                    // Versuche festtag_variabel oder festtag_fest zu bestimmen
                    // Standard: festtag_variabel
                    programmTyp = "festtag_variabel";
                }
                
                try {
                    // Erstelle oder hole Tagtyp
                    Tagtyp tagtyp = saveTagtyp(fileName, programmTyp);
                    
                    // Migriere Programme aus dieser Datei
                    ExcelRead excelRead = new ExcelRead();
                    excelRead.openXls(filePath);
                    
                    int zeilen = excelRead.getCellZeilen();
                    int migrated = 0;
                    
                    // Beginne ab Zeile 4 (Index 3)
                    for (int zeile = 3; zeile < zeilen; zeile++) {
                        try {
                            String startzeit = excelRead.getCellString(TagesSuche.SPALTE_A_STARTZEIT, zeile);
                            if (startzeit == null || startzeit.trim().isEmpty()) {
                                break; // Ende der Tabelle
                            }
                            
                            // Konvertiere Excel-Zeile zu Programm-Objekt
                            Programm programm = excelZeileToProgramm(excelRead, zeile);
                            programm.setId(zeile); // Zeile als ID
                            
                            // Speichere in Datenbank
                            saveProgramm(fileName, programm);
                            migrated++;
                        } catch (Exception e) {
                            Log.w(TAG, "Fehler beim Migrieren von Zeile " + zeile + " aus " + fileName, e);
                        }
                    }
                    
                    excelRead.closeWorkbook();
                    Log.d(TAG, "Tagtyp " + fileName + " migriert: " + migrated + " Programme");
                    
                } catch (Exception e) {
                    Log.e(TAG, "Fehler beim Migrieren von Tagtyp: " + fileName, e);
                }
            }
            
            Log.d(TAG, "Tagtypen- und Programme-Migration abgeschlossen");
            
        } catch (Exception e) {
            Log.e(TAG, "Fehler bei Migration von Tagtypen und Programmen", e);
        }
    }
    
    /**
     * Prüft, ob Spalte D (Heizung) vorhanden ist.
     */
    private boolean hasSpalteD(ExcelRead excelRead, int zeile) {
        // Spalte D (Dauer Heizung) gibt es in Excel NICHT!
        // In Excel: Montag ist Spalte D (Index 3)
        // In Datenbank: Montag ist Spalte E (Index 4), weil wir Spalte D (Dauer Heizung) dazwischen einfügen
        // Daher ist hasD beim Excel-Import IMMER false
        return false;
    }
    
    /**
     * Konvertiert eine Excel-Zeile zu einem Programm-Objekt.
     * Verwendet die gleiche Logik wie ExcelProgrammRepository.
     */
    private Programm excelZeileToProgramm(ExcelRead excelRead, int zeile) {
        Programm programm = new Programm();
        
        try {
            // Excel-Struktur: A=Startzeit, B=Funktion, C=Melodie, D=Montag, E=Dienstag, ...
            // Datenbank-Struktur: A=Startzeit, B=Funktion, C=Melodie, D=Dauer Heizung (nur DB), E=Montag, F=Dienstag, ...
            // Spalte D (Dauer Heizung) gibt es in Excel NICHT! Daher verschieben wir alle Spalten nach C um -1
            
            programm.setStartzeit(excelRead.getCellString(TagesSuche.SPALTE_A_STARTZEIT, zeile));
            programm.setFunktion(excelRead.getCellString(TagesSuche.SPALTE_B_FUNKTION, zeile));
            programm.setMelodieName(excelRead.getCellString(TagesSuche.SPALTE_C_MELODIE_NAME, zeile));
            
            // Spalte D (Dauer Heizung) gibt es in Excel NICHT - wird nur in DB verwendet
            boolean hasD = false; // Spalte D ist nie in Excel vorhanden
            programm.setDauerHeizung(null); // Wird später über Web-UI gesetzt
            Log.d(TAG, "excelZeileToProgramm: Zeile " + zeile + ", hasD=false (Spalte D existiert nicht in Excel)");
            
            // Wochentage: In Excel beginnt Montag bei Index 3 (Spalte D), in DB bei Index 4 (Spalte E)
            // Da hasD=false, lesen wir aus Index 3 (was in Excel Spalte D/Montag ist)
            int wochentagStart = TagesSuche.SPALTE_D_DAUER_MINUTEN_HEIZUNG; // Index 3 (Excel Spalte D = Montag)
            Log.d(TAG, "excelZeileToProgramm: Zeile " + zeile + ", wochentagStart = " + wochentagStart + " (Excel Spalte D = Montag)");
            
            // Wochentage aus Excel lesen (Index 3-9, was in Excel Spalten D-J sind)
            programm.setMontag("x".equals(excelRead.getCellString(wochentagStart + 0, zeile)));     // Excel D (3) → DB E (4)
            programm.setDienstag("x".equals(excelRead.getCellString(wochentagStart + 1, zeile)));  // Excel E (4) → DB F (5)
            programm.setMittwoch("x".equals(excelRead.getCellString(wochentagStart + 2, zeile)));    // Excel F (5) → DB G (6)
            programm.setDonnerstag("x".equals(excelRead.getCellString(wochentagStart + 3, zeile)));  // Excel G (6) → DB H (7)
            programm.setFreitag("x".equals(excelRead.getCellString(wochentagStart + 4, zeile)));     // Excel H (7) → DB I (8)
            programm.setSamstag("x".equals(excelRead.getCellString(wochentagStart + 5, zeile)));      // Excel I (8) → DB J (9)
            programm.setSonntag("x".equals(excelRead.getCellString(wochentagStart + 6, zeile)));     // Excel J (9) → DB K (10)
            
            // Immer: In Excel Index 10 (Spalte K), in DB Index 11 (Spalte L)
            int spalteImmer = TagesSuche.SPALTE_L_IMMER - 1; // Index 10 (Excel Spalte K)
            String immerStr = excelRead.getCellString(spalteImmer, zeile);
            programm.setImmer("1".equals(immerStr));
            
            // Periodisch: In Excel Index 11 (Spalte L), in DB Index 12 (Spalte M)
            int spaltePeriodisch = TagesSuche.SPALTE_M_PERIODISCH - 1; // Index 11 (Excel Spalte L)
            String periodischStr = excelRead.getCellString(spaltePeriodisch, zeile);
            if ("1".equals(periodischStr)) {
                programm.setPeriodisch(1); // Sommer
            } else if ("2".equals(periodischStr)) {
                programm.setPeriodisch(2); // Winter
            } else {
                programm.setPeriodisch(0); // Immer
            }
            
            // Start/Ende-Datum: In Excel Index 12-13 (Spalten M-N), in DB Index 13-14 (Spalten N-O)
            int spalteStart = TagesSuche.SPALTE_N_START - 1; // Index 12 (Excel Spalte M)
            int spalteEnde = TagesSuche.SPALTE_O_ENDE - 1;    // Index 13 (Excel Spalte N)
            String startDatum = excelRead.getCellString(spalteStart, zeile);
            String endeDatum = excelRead.getCellString(spalteEnde, zeile);
            
            // Normalisiere leere Strings zu null
            startDatum = (startDatum != null && !startDatum.trim().isEmpty()) ? startDatum.trim() : null;
            endeDatum = (endeDatum != null && !endeDatum.trim().isEmpty()) ? endeDatum.trim() : null;
            
            programm.setStartDatum(startDatum);
            programm.setEndeDatum(endeDatum);
            Log.d(TAG, "excelZeileToProgramm: Zeile " + zeile + ", Start aus Excel Spalte " + spalteStart + " (DB " + TagesSuche.SPALTE_N_START + ") = '" + startDatum + "', Ende aus Excel Spalte " + spalteEnde + " (DB " + TagesSuche.SPALTE_O_ENDE + ") = '" + endeDatum + "'");
            
            // Verknüpfte Taste: In Excel Index 14 (Spalte O), in DB Index 15 (Spalte P)
            int spalteVerknuepfteTaste = TagesSuche.SPALTE_P_VERKNUEPFTE_TASTE - 1; // Index 14 (Excel Spalte O)
            String verknuepfteTaste = excelRead.getCellString(spalteVerknuepfteTaste, zeile);
            
            // Prüfe ob der Wert aus der falschen Spalte kommt
            if (endeDatum != null && endeDatum.length() > 10 && !endeDatum.matches("\\d{1,2}\\.\\d{1,2}(\\.\\d{4})?(\\.\\d+)?(\\.(A|T))?")) {
                Log.w(TAG, "excelZeileToProgramm: WARNUNG Zeile " + zeile + " - Ende-Datum sieht nicht wie Datum aus: '" + endeDatum + "' - möglicherweise falsche Spaltenzuordnung!");
            }
            
            // Normalisiere leere Strings zu null
            verknuepfteTaste = (verknuepfteTaste != null && !verknuepfteTaste.trim().isEmpty()) ? verknuepfteTaste.trim() : null;
            Log.d(TAG, "excelZeileToProgramm: Zeile " + zeile + ", Verknüpfte Taste aus Excel Spalte " + spalteVerknuepfteTaste + " (DB " + TagesSuche.SPALTE_P_VERKNUEPFTE_TASTE + ") = '" + verknuepfteTaste + "'");
            if (verknuepfteTaste != null && !verknuepfteTaste.trim().isEmpty()) {
                programm.setVerknuepfteTaste(verknuepfteTaste.trim());
                Log.d(TAG, "excelZeileToProgramm: Verknüpfte Taste gesetzt: '" + verknuepfteTaste.trim() + "'");
            } else {
                Log.d(TAG, "excelZeileToProgramm: Verknüpfte Taste ist leer oder null");
            }
            
            // Priorität: In Excel Index 15 (Spalte P), in DB Index 16 (Spalte Q)
            int spaltePrioritaet = TagesSuche.SPALTE_Q_PRIORITAET - 1; // Index 15 (Excel Spalte P)
            try {
                String prioritaetStr = excelRead.getCellString(spaltePrioritaet, zeile);
                if (prioritaetStr != null && !prioritaetStr.trim().isEmpty()) {
                    programm.setPrioritaet(Integer.parseInt(prioritaetStr.trim()));
                } else {
                    programm.setPrioritaet(0);
                }
            } catch (NumberFormatException e) {
                programm.setPrioritaet(0);
            }
            
        } catch (Exception e) {
            Log.w(TAG, "Fehler beim Konvertieren von Zeile " + zeile, e);
        }
        
        return programm;
    }
    
    /**
     * Entity-Klasse für Tagtyp.
     */
    public static class Tagtyp {
        public int id;
        public String name;              // Dateiname (z.B. "Weihnachten.xls")
        public String programmTyp;       // "normal", "festtag_variabel", "festtag_fest", "benutzer"
    }
    
    /**
     * Fügt Standard-Osterfeiertage in die Datenbank ein.
     */
    /** Tagtyp für alle Feiertage und Vortage bei Neuanlage/Standard: Normalprogramm (keine Excel-Dateien nötig). */
    private static final String DEFAULT_FEIERTAG_TAGTYP = "Normalprogramm";
    
    private void insertDefaultOsterfeiertage(SQLiteDatabase db) {
        // Liste der Osterfeiertage mit ihren Offsets; alle auf Normalprogramm (inkl. Vortage)
        String[][] osterfeiertage = {
            {"Aschermittwoch", "-46", DEFAULT_FEIERTAG_TAGTYP},
            {"Vortag Palmsonntag", "-8", DEFAULT_FEIERTAG_TAGTYP},
            {"Palmsonntag", "-7", DEFAULT_FEIERTAG_TAGTYP},
            {"Gründonnerstag", "-3", DEFAULT_FEIERTAG_TAGTYP},
            {"Karfreitag", "-2", DEFAULT_FEIERTAG_TAGTYP},
            {"Karsamstag", "-1", DEFAULT_FEIERTAG_TAGTYP},
            {"Ostersonntag", "0", DEFAULT_FEIERTAG_TAGTYP},
            {"Ostermontag", "1", DEFAULT_FEIERTAG_TAGTYP},
            {"Vortag Weißsonntag", "6", DEFAULT_FEIERTAG_TAGTYP},
            {"Weißsonntag", "7", DEFAULT_FEIERTAG_TAGTYP},
            {"Christihimmelfahrt Mittwoch", "38", DEFAULT_FEIERTAG_TAGTYP},
            {"Christihimmelfahrt Donnerstag", "39", DEFAULT_FEIERTAG_TAGTYP},
            {"Christihimmelfahrt Samstag", "41", DEFAULT_FEIERTAG_TAGTYP},
            {"Christihimmelfahrt Sonntag", "42", DEFAULT_FEIERTAG_TAGTYP},
            {"Pingstsamstag", "48", DEFAULT_FEIERTAG_TAGTYP},
            {"Pingstsonntag", "49", DEFAULT_FEIERTAG_TAGTYP},
            {"Pingstmontag", "50", DEFAULT_FEIERTAG_TAGTYP},
            {"Fronleichnam Mittwoch", "60", DEFAULT_FEIERTAG_TAGTYP},
            {"Fronleichnam Donnerstag", "61", DEFAULT_FEIERTAG_TAGTYP},
            {"Fronleichnam Samstag", "63", DEFAULT_FEIERTAG_TAGTYP},
            {"Fronleichnam Sonntag", "64", DEFAULT_FEIERTAG_TAGTYP},
            {"Herzjesu Samstag", "69", DEFAULT_FEIERTAG_TAGTYP},
            {"Herzjesu Sonntag", "70", DEFAULT_FEIERTAG_TAGTYP},
            {"Christkönig Samstag", "210", DEFAULT_FEIERTAG_TAGTYP},
            {"Christkönig Sonntag", "211", DEFAULT_FEIERTAG_TAGTYP},
            {"1. Advent", "217", DEFAULT_FEIERTAG_TAGTYP},
            {"2. Advent", "224", DEFAULT_FEIERTAG_TAGTYP},
            {"3. Advent", "231", DEFAULT_FEIERTAG_TAGTYP},
            {"4. Advent", "238", DEFAULT_FEIERTAG_TAGTYP}
        };
        
        for (String[] feiertag : osterfeiertage) {
            android.content.ContentValues values = new android.content.ContentValues();
            values.put("name", feiertag[0]);
            values.put("oster_offset", Integer.parseInt(feiertag[1]));
            values.put("tagtyp_name", feiertag[2] != null ? feiertag[2] : DEFAULT_FEIERTAG_TAGTYP);
            try {
                db.insert(TABLE_OSTERFEIERTAGE, null, values);
            } catch (Exception e) {
                Log.w(TAG, "Fehler beim Einfügen von Osterfeiertag: " + feiertag[0], e);
            }
        }
        Log.d(TAG, "Standard-Osterfeiertage eingefügt (alle Normalprogramm): " + osterfeiertage.length);
    }
    
    /**
     * Fügt Standard-Schlagwerk-Config für Typ 1 und Typ 2 ein.
     */
    private void insertDefaultSchlagwerkConfig(SQLiteDatabase db) {
        String[] startZeiten = {"08:00", "08:00"};
        String[] endeZeiten = {"18:00", "18:00"};
        for (int typ = 1; typ <= 2; typ++) {
            android.content.ContentValues values = new android.content.ContentValues();
            values.put("typ", typ);
            values.put("start_zeit", startZeiten[typ - 1]);
            values.put("ende_zeit", endeZeiten[typ - 1]);
            values.put("hammer_stunden_relais", 0);
            values.put("hammer_viertel_relais", 0);
            values.put("pause_viertel_ms", 0);
            values.put("pause_viertel_zu_stunde_ms", 0);
            values.put("halbstunde_schlaganzahl", 1);
            values.put("volle_stunde_anzahl", 12);
            values.put("schlagen_wenn_melodie_laeuft", 0);
            values.put("kein_schlagwerk_ostern", 0);
            values.put("kein_schlagwerk_karfreitag", 0);
            values.put("kein_schlagwerk_karsamstag", 0);
            values.put("aktiv", 1);
            values.put("sortierung", typ);
            try {
                db.insert(TABLE_SCHLAGWERK_CONFIG, null, values);
            } catch (Exception e) {
                Log.w(TAG, "Fehler beim Einfügen Schlagwerk-Config Typ " + typ, e);
            }
        }
        Log.d(TAG, "Standard-Schlagwerk-Config (Typ 1 und 2) eingefügt");
    }
    
    /**
     * Fügt Standard-Relais-Kategorien ein (Nebenuhr, Schlagwerk, Vorschwingen, Melodie).
     */
    private void insertDefaultRelaisKategorien(SQLiteDatabase db) {
        String[] namen = {"Nebenuhr", "Schlagwerk", "Vorschwingen", "Melodie"};
        for (int i = 0; i < namen.length; i++) {
            android.content.ContentValues values = new android.content.ContentValues();
            values.put("name", namen[i]);
            values.put("sortierung", i + 1);
            try {
                db.insert(TABLE_RELAIS_KATEGORIEN, null, values);
            } catch (Exception e) {
                Log.w(TAG, "Fehler beim Einfügen Relais-Kategorie " + namen[i], e);
            }
        }
        Log.d(TAG, "Standard-Relais-Kategorien eingefügt");
    }
    
    /** Sonder-IDs für Systemtasten (wie in ConfigWebServer/App). */
    private void insertDefaultUiFunktionen(SQLiteDatabase db) {
        Object[][] defaults = new Object[][]{
            {1100, "stop", "Stop", "Stoppt Glocken, Melodien und setzt die Automatik-Suche neu.", 1},
            {1101, "automatic_toggle", "Automatik", "Schaltet die Automatik ein oder aus.", 2},
            {1102, "home", "Home", "Geht von Unterseiten zur Hauptansicht zurueck.", 3},
            {1103, "help", "Hilfe", "Oeffnet die Hilfe-Seite.", 4},
            {1104, "schlagwerk_toggle", "Schlagwerk", "Schaltet das Schlagwerk ein oder aus.", 5},
            {1202, "open_page_2", "Seite 2", "Oeffnet die zweite Bedienseite.", 6},
            {1203, "open_programm_eingeben", "Programm eingeben", "Oeffnet die Benutzerprogrammeingabe.", 7},
            {1204, "open_programm_abfragen", "Programm abfragen", "Oeffnet die Programmkontrolle.", 8},
            {1205, "open_set_nebenuhr", "Nebenuhr einstellen", "Oeffnet den Nebenuhr-Dialog.", 9},
            {1206, "open_manueler_start", "Manueller Start", "Oeffnet den manuellen Start.", 10}
        };
        for (Object[] row : defaults) {
            android.content.ContentValues values = new android.content.ContentValues();
            values.put("code_alt", (Integer) row[0]);
            values.put("key_name", (String) row[1]);
            values.put("anzeige_name", (String) row[2]);
            values.put("beschreibung", (String) row[3]);
            values.put("aktiv", 1);
            values.put("sortierung", (Integer) row[4]);
            try {
                db.insertWithOnConflict(TABLE_UI_FUNKTION, null, values, SQLiteDatabase.CONFLICT_IGNORE);
            } catch (Exception e) {
                Log.w(TAG, "Fehler beim Einfuegen UI-Funktion " + row[1], e);
            }
        }
        Log.d(TAG, "Standard-UI-Funktionen eingefuegt");
    }

    private static final int SONDER_ID_STOP = 1100;
    private static final int SONDER_ID_AUTOMATIK = 1101;

    private void ensureLayoutItemsBackfilled() {
        SQLiteDatabase db = getWritableDatabase();
        ensureLayoutItemsFromBeschriftungTasten(db, getLayoutGridCols(db));
    }

    private void ensureLayoutItemsFromBeschriftungTasten(SQLiteDatabase db) {
        ensureLayoutItemsFromBeschriftungTasten(db, getLayoutGridCols(db));
    }

    private void ensureLayoutItemsFromBeschriftungTasten(SQLiteDatabase db, int layoutCols) {
        final int normalizedLayoutCols = normalizeLayoutGridCols(layoutCols);
        Cursor countCursor = null;
        try {
            countCursor = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_LAYOUT_ITEM, null);
            if (countCursor.moveToFirst() && countCursor.getInt(0) > 0) {
                return;
            }
        } finally {
            if (countCursor != null) countCursor.close();
        }

        insertDefaultUiFunktionen(db);

        Cursor uiCursor = null;
        Cursor buttonCursor = null;
        boolean startedTransaction = false;
        try {
            java.util.HashMap<Integer, Integer> uiFunktionIdByCode = new java.util.HashMap<>();
            uiCursor = db.query(TABLE_UI_FUNKTION, new String[]{"id", "code_alt"}, null, null, null, null, null);
            while (uiCursor.moveToNext()) {
                uiFunktionIdByCode.put(uiCursor.getInt(1), uiCursor.getInt(0));
            }

            buttonCursor = db.query(TABLE_BESCHRIFTUNG_TASTEN,
                new String[]{"zeile_index", "c1", "c2", "c3", "c4", "c5", "c13", "sound", "sonder_id"},
                null, null, null, null, "zeile_index ASC");

            if (!db.inTransaction()) {
                db.beginTransaction();
                startedTransaction = true;
            }
            while (buttonCursor.moveToNext()) {
                int zeileIndex = buttonCursor.getInt(0);
                String c1 = buttonCursor.isNull(1) ? null : buttonCursor.getString(1);
                String c2 = buttonCursor.isNull(2) ? null : buttonCursor.getString(2);
                String c3 = buttonCursor.isNull(3) ? null : buttonCursor.getString(3);
                String c4 = buttonCursor.isNull(4) ? null : buttonCursor.getString(4);
                String c5 = buttonCursor.isNull(5) ? null : buttonCursor.getString(5);
                String c13 = buttonCursor.isNull(6) ? null : buttonCursor.getString(6);
                String sound = buttonCursor.isNull(7) ? null : buttonCursor.getString(7);
                Integer sonderId = buttonCursor.isNull(8) ? null : buttonCursor.getInt(8);
                int code = sonderId != null ? sonderId : parseIntegerOrDefault(c3, Integer.MIN_VALUE);
                int slotOnPage = zeileIndex % 24;

                android.content.ContentValues values = new android.content.ContentValues();
                values.put("page_nummer", (zeileIndex / 24) + 1);
                values.put("slot_index", slotOnPage);
                values.put("grid_row", slotOnPage / normalizedLayoutCols);
                values.put("grid_col", slotOnPage % normalizedLayoutCols);
                values.put("grid_width", 1);
                values.put("grid_height", 1);
                values.put("label_text", c2);
                values.put("button_id", c13);
                values.put("legacy_code", c3);
                values.put("legacy_platine", c5);
                values.put("legacy_hammerzeit", c4);
                values.put("legacy_funktion", c1);
                values.put("sound", sound);
                values.put("sichtbar", isVisibleLegacyButton(c1, c2) ? 1 : 0);
                values.put("sortierung", zeileIndex);

                Integer uiFunktionId = uiFunktionIdByCode.get(code);
                if (uiFunktionId != null) {
                    values.put("target_kind", "ui_funktion");
                    values.put("target_id", uiFunktionId);
                } else if (isLegacyVerknuepfteTaste(code)) {
                    values.put("target_kind", "verknuepfte_funktion_legacy");
                } else if (isLegacyPopupCode(code)) {
                    values.put("target_kind", "popup_legacy");
                } else if (isLegacyHardwareRelais(c3)) {
                    values.put("target_kind", "hardware_relais_legacy");
                } else {
                    values.put("target_kind", "leer");
                }

                db.insertWithOnConflict(TABLE_LAYOUT_ITEM, null, values, SQLiteDatabase.CONFLICT_REPLACE);
            }
            if (startedTransaction) {
                db.setTransactionSuccessful();
            }
            Log.d(TAG, "layout_item aus beschriftung_tasten initial gefuellt");
        } finally {
            if (startedTransaction && db.inTransaction()) {
                db.endTransaction();
            }
            if (uiCursor != null) uiCursor.close();
            if (buttonCursor != null) buttonCursor.close();
        }
    }

    private static boolean isVisibleLegacyButton(String c1, String c2) {
        return !isEmptyOrEqualsIgnoreCase(c1, "NULL")
            && !isEmptyOrEqualsIgnoreCase(c2, "NULL");
    }

    private static boolean isLegacyHardwareRelais(String c3) {
        int code = parseIntegerOrDefault(c3, Integer.MIN_VALUE);
        return code >= 0 && code < 1000;
    }

    private static boolean isLegacyVerknuepfteTaste(int code) {
        return code >= 2001 && code < 3000;
    }

    private static boolean isLegacyPopupCode(int code) {
        return code >= 3000;
    }

    private static boolean isEmptyOrEqualsIgnoreCase(String value, String match) {
        return value == null || value.trim().isEmpty() || match.equalsIgnoreCase(value.trim());
    }

    private static int parseIntegerOrDefault(String value, int defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (Exception e) {
            return defaultValue;
        }
    }
    
    /**
     * Fügt bei Neuanlage/Werkszustand den Tagtyp „Normalprogramm“ und ein Minimal-Programm ein,
     * damit die App startet (hasNoProgrammeForNormalprogramm() = false).
     */
    private void insertDefaultNormalprogramm(SQLiteDatabase db) {
        try {
            android.content.ContentValues tagtypValues = new android.content.ContentValues();
            tagtypValues.put("name", "Normalprogramm");
            tagtypValues.put("programm_typ", "normal");
            long tagtypId = db.insert(TABLE_TAGTYPEN, null, tagtypValues);
            if (tagtypId <= 0) {
                Log.w(TAG, "insertDefaultNormalprogramm: Tagtyp insert fehlgeschlagen");
                return;
            }
            android.content.ContentValues progValues = new android.content.ContentValues();
            progValues.put("tagtyp_id", (int) tagtypId);
            progValues.put("zeile_index", 0);
            progValues.put("startzeit", "00:00:00");
            progValues.put("funktion", "Melodie");
            progValues.put("melodie_name", "");
            progValues.put("dauer_heizung", (String) null);
            progValues.put("montag", 1);
            progValues.put("dienstag", 1);
            progValues.put("mittwoch", 1);
            progValues.put("donnerstag", 1);
            progValues.put("freitag", 1);
            progValues.put("samstag", 1);
            progValues.put("sonntag", 1);
            progValues.put("immer", 1);
            progValues.put("periodisch", 0);
            progValues.put("prioritaet", 0);
            db.insert(TABLE_PROGRAMME, null, progValues);
            Log.d(TAG, "Normalprogramm (Tagtyp + 1 Programm) für Neuanlage eingefügt");
        } catch (Exception e) {
            Log.e(TAG, "Fehler beim Einfügen Normalprogramm für Neuanlage", e);
        }
    }
    
    /**
     * Fügt bei Neuanlage/Werkszustand 48 Beschriftungstasten ein: Zeile 0 = Leer, 1 = Stop, 2 = Automatik,
     * 3–23 = Leer, 24–47 = NULL. So startet die App mit Stop- und Automatik-Taste.
     */
    private void insertDefaultBeschriftungTastenNeuanlage(SQLiteDatabase db) {
        android.content.ContentValues values = new android.content.ContentValues();
        for (int i = 0; i < BESCHRIFTUNG_TASTEN_DEFAULT_COUNT; i++) {
            values.clear();
            values.put("zeile_index", i);
            if (i == 1) {
                values.put("c1", "Stop");
                values.put("c2", "Stop");
                values.put("c3", "");
                values.put("c4", "");
                values.put("c5", "");
                values.put("c13", "");
                values.put("sonder_id", SONDER_ID_STOP);
                values.put("sound", "");
            } else if (i == 2) {
                values.put("c1", "Automatik");
                values.put("c2", "Automatik");
                values.put("c3", "");
                values.put("c4", "");
                values.put("c5", "");
                values.put("c13", "");
                values.put("sonder_id", SONDER_ID_AUTOMATIK);
                values.put("sound", "");
            } else if (i < 24) {
                putLeerRow(values, i);
            } else {
                putNullRow(values, i);
            }
            try {
                db.insert(TABLE_BESCHRIFTUNG_TASTEN, null, values);
            } catch (Exception e) {
                Log.w(TAG, "Fehler beim Einfügen Beschriftungstaste zeile " + i, e);
            }
        }
        Log.d(TAG, "Beschriftungstasten Neuanlage (Stop, Automatik, Leer/NULL) eingefügt");
    }
    
    /**
     * Fügt die festen Feiertage inkl. Vorfeiertage ein (ohne Excel).
     * Fixe Daten: 1.1, 6.1, 15.8, 1.11, 8.12, 24.12, 25.12, 26.12 + je ein Vorfeiertag (Tag davor).
     * Christkönig (letzter So im Jahreskreis) und 1.–4. Advent bleiben in osterfeiertage (variabel).
     */
    private void insertDefaultFesteFeiertage(SQLiteDatabase db) {
        // name, datum (TT.MM), tagtyp_name = Normalprogramm; Vorfeiertag = Tag vor dem Fest
        String[][] feste = {
            {"Vorfeiertag Neujahr", "31.12", DEFAULT_FEIERTAG_TAGTYP},
            {"Neujahr", "1.1", DEFAULT_FEIERTAG_TAGTYP},
            {"Vorfeiertag Hl. Drei Könige", "5.1", DEFAULT_FEIERTAG_TAGTYP},
            {"Hl. Drei Könige", "6.1", DEFAULT_FEIERTAG_TAGTYP},
            {"Vorfeiertag Mariä Himmelfahrt", "14.8", DEFAULT_FEIERTAG_TAGTYP},
            {"Mariä Himmelfahrt", "15.8", DEFAULT_FEIERTAG_TAGTYP},
            {"Vorfeiertag Allerheiligen", "31.10", DEFAULT_FEIERTAG_TAGTYP},
            {"Allerheiligen", "1.11", DEFAULT_FEIERTAG_TAGTYP},
            {"Vorfeiertag Mariä Empfängnis", "7.12", DEFAULT_FEIERTAG_TAGTYP},
            {"Mariä Empfängnis", "8.12", DEFAULT_FEIERTAG_TAGTYP},
            {"Vorfeiertag Heiligabend", "23.12", DEFAULT_FEIERTAG_TAGTYP},
            {"Heiligabend", "24.12", DEFAULT_FEIERTAG_TAGTYP},
            {"1. Weihnachtstag", "25.12", DEFAULT_FEIERTAG_TAGTYP},
            {"2. Weihnachtstag", "26.12", DEFAULT_FEIERTAG_TAGTYP}
        };
        for (String[] row : feste) {
            android.content.ContentValues values = new android.content.ContentValues();
            values.put("name", row[0]);
            values.put("datum", row[1]);
            values.put("tagtyp_name", row[2]);
            values.put("verschieben_auf_sonntag", 0);
            values.put("sonntag_im_monat", 0);
            try {
                db.insert(TABLE_FESTE_FEIERTAGE, null, values);
            } catch (Exception e) {
                Log.w(TAG, "Fehler beim Einfügen feste Feiertag: " + row[0] + " " + row[1], e);
            }
        }
        Log.d(TAG, "Feste Feiertage inkl. Vorfeiertage eingefügt (1.1; 6.1; 15.8; 1.11; 8.12; 24.–26.12)");
    }
    
    /**
     * Lädt alle Osterfeiertage aus der Datenbank.
     */
    public List<Osterfeiertag> getAllOsterfeiertage() {
        List<Osterfeiertag> feiertage = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        
        Cursor cursor = db.query(TABLE_OSTERFEIERTAGE,
            new String[]{"id", "name", "oster_offset", "tagtyp_name"},
            null, null, null, null, "oster_offset ASC");
        
        try {
            while (cursor.moveToNext()) {
                Osterfeiertag feiertag = new Osterfeiertag();
                feiertag.id = cursor.getInt(0);
                feiertag.name = cursor.getString(1);
                feiertag.osterOffset = cursor.getInt(2);
                feiertag.tagtypName = cursor.isNull(3) ? null : cursor.getString(3);
                feiertage.add(feiertag);
            }
        } finally {
            cursor.close();
        }
        
        return feiertage;
    }
    
    /**
     * Speichert einen Osterfeiertag (INSERT oder UPDATE).
     */
    public Osterfeiertag saveOsterfeiertag(Osterfeiertag feiertag) {
        SQLiteDatabase db = getWritableDatabase();
        
        android.content.ContentValues values = new android.content.ContentValues();
        values.put("name", feiertag.name);
        values.put("oster_offset", feiertag.osterOffset);
        if (feiertag.tagtypName != null && !feiertag.tagtypName.trim().isEmpty()) {
            values.put("tagtyp_name", feiertag.tagtypName);
        } else {
            values.putNull("tagtyp_name");
        }
        values.put("updated_at", "CURRENT_TIMESTAMP");
        
        if (feiertag.id > 0) {
            // UPDATE
            db.update(TABLE_OSTERFEIERTAGE, values, "id = ?", new String[]{String.valueOf(feiertag.id)});
            Log.d(TAG, "Osterfeiertag aktualisiert: " + feiertag.name);
        } else {
            // INSERT
            long id = db.insert(TABLE_OSTERFEIERTAGE, null, values);
            feiertag.id = (int) id;
            Log.d(TAG, "Osterfeiertag erstellt: " + feiertag.name + " (ID: " + id + ")");
        }
        
        return feiertag;
    }
    
    /**
     * Löscht einen Osterfeiertag.
     */
    public void deleteOsterfeiertag(int id) {
        SQLiteDatabase db = getWritableDatabase();
        db.delete(TABLE_OSTERFEIERTAGE, "id = ?", new String[]{String.valueOf(id)});
        Log.d(TAG, "Osterfeiertag gelöscht: ID " + id);
    }
    
    /**
     * Migriert Tagtyp-Namen für Osterfeiertage aus System.xls Sheet 12.
     * Liest nur Spalte A (Tagtyp-Dateiname) ab Zeile 5.
     */
    private void migrateOsterfeiertageFromExcel(SQLiteDatabase db) {
        migrateOsterfeiertageFromExcel(db, false);
    }
    
    /**
     * Migriert Tagtyp-Namen für Osterfeiertage aus System.xls Sheet 12 (öffentliche Methode für manuelle Migration).
     * Liest nur Spalte A (Tagtyp-Dateiname) ab Zeile 5.
     * @param force Wenn true, wird Migration auch ausgeführt wenn bereits Tagtypen vorhanden sind
     */
    public void migrateOsterfeiertageFromExcel(boolean force) {
        SQLiteDatabase db = getWritableDatabase();
        migrateOsterfeiertageFromExcel(db, force);
    }
    
    /**
     * Migriert Tagtyp-Namen für Osterfeiertage aus System.xls Sheet 12.
     * Layout wie in TagesSuche/UhrThread: Zeile 2 = Jahre in Spalten, ab Zeile 5 pro Zeile
     * Spalte A = Tagtyp (Programmtag-Dateiname, z.B. "Karfreitag" oder "Karfreitag.xls").
     * Die Zuordnung zu jedem Feiertag (z.B. Karfreitag) erfolgt namensbasiert: Der Inhalt
     * von Spalte A (ohne .xls) wird mit dem Feiertagsnamen in der DB verglichen, damit
     * jeder Feiertag seinen richtigen Programmtag-Tagtyp erhält – unabhängig von der Zeilenreihenfolge.
     * @param db Datenbank
     * @param force Wenn true, wird Migration auch ausgeführt wenn bereits Tagtypen vorhanden sind
     */
    private void migrateOsterfeiertageFromExcel(SQLiteDatabase db, boolean force) {
        try {
            if (!force) {
                // Prüfe ob bereits Tagtypen zugeordnet sind
                Cursor checkCursor = db.query(TABLE_OSTERFEIERTAGE,
                    new String[]{"COUNT(*) as count"},
                    "tagtyp_name IS NOT NULL AND tagtyp_name != ''",
                    null, null, null, null);
                boolean hasTagtypen = false;
                if (checkCursor.moveToFirst()) {
                    hasTagtypen = checkCursor.getInt(0) > 0;
                }
                checkCursor.close();
                
                if (hasTagtypen) {
                    Log.d(TAG, "Osterfeiertage haben bereits Tagtypen zugeordnet, überspringe Migration");
                    return;
                }
            }
            
            String sdCardPath = android.os.Environment.getExternalStorageDirectory().getPath();
            String systemXlsPath = sdCardPath + StaticConstants.excellSystemString;
            java.io.File systemFile = new java.io.File(systemXlsPath);
            
            if (!systemFile.exists()) {
                Log.d(TAG, "System.xls nicht gefunden, überspringe Osterfeiertage-Migration");
                return;
            }
            
            ExcelRead excelread = new ExcelRead();
            excelread.openXlsSheet(systemXlsPath, 12); // Sheet 12 = Variable Feiertage
            
            Log.d(TAG, "Migriere Osterfeiertage-Tagtypen aus System.xls Sheet 12 (namensbasiert)");
            
            // Alle Osterfeiertage aus DB (für Namensabgleich)
            List<Osterfeiertag> feiertage = getAllOsterfeiertage();
            
            int zeile = 5; // Beginne ab Zeile 5 (wie TagesSuche/UhrThread)
            int zeilen = excelread.getCellZeilen();
            int zugeordnet = 0;
            
            while (zeile < zeilen) {
                try {
                    String tagtypName = excelread.getCellString(0, zeile); // Spalte A = Tagtyp
                    
                    if (tagtypName == null || tagtypName.trim().isEmpty()) {
                        zeile++;
                        continue;
                    }
                    tagtypName = tagtypName.trim();
                    
                    // Filtere ungültige Einträge (z.B. Nebenuhr-Namen)
                    String tagtypNameLower = tagtypName.toLowerCase();
                    if (tagtypNameLower.equals("stubenuhr") ||
                        tagtypNameLower.equals("nebenuhr") ||
                        tagtypNameLower.equals("nebenuhr a") ||
                        tagtypNameLower.equals("nebenuhr b") ||
                        tagtypNameLower.equals("nebenuhr c")) {
                        Log.w(TAG, "Ungültiger Tagtyp ignoriert (Nebenuhr-Name?): " + tagtypName);
                        zeile++;
                        continue;
                    }
                    
                    // Feiertagsname für Abgleich: z.B. "Karfreitag.xls" -> "Karfreitag"
                    String nameForSearch = tagtypName;
                    if (nameForSearch.toLowerCase().endsWith(".xls")) {
                        nameForSearch = nameForSearch.substring(0, nameForSearch.length() - 4).trim();
                    }
                    
                    // Passenden Osterfeiertag in der DB per Namen finden (jeder Feiertag hat einen Programmtag-Tagtyp)
                    Osterfeiertag feiertag = null;
                    for (Osterfeiertag f : feiertage) {
                        if (f.name != null && f.name.trim().equalsIgnoreCase(nameForSearch)) {
                            feiertag = f;
                            break;
                        }
                    }
                    
                    if (feiertag == null) {
                        Log.w(TAG, "Kein Osterfeiertag mit Name \"" + nameForSearch + "\" in DB, Zeile " + zeile + " übersprungen");
                        zeile++;
                        continue;
                    }
                    
                    // Tagtyp in Tabelle tagtypen anlegen/aktualisieren
                    saveTagtyp(tagtypName, "festtag_variabel");
                    
                    // Diesem Feiertag den Tagtyp aus Spalte A zuweisen
                    android.content.ContentValues values = new android.content.ContentValues();
                    values.put("tagtyp_name", tagtypName);
                    values.put("updated_at", "CURRENT_TIMESTAMP");
                    db.update(TABLE_OSTERFEIERTAGE, values, "id = ?", new String[]{String.valueOf(feiertag.id)});
                    
                    Log.d(TAG, "Tagtyp zugeordnet: " + feiertag.name + " (id=" + feiertag.id + ") → " + tagtypName);
                    zugeordnet++;
                    zeile++;
                    
                } catch (Exception e) {
                    Log.w(TAG, "Fehler beim Lesen von Zeile " + zeile, e);
                    zeile++;
                }
            }
            
            excelread.closeWorkbook();
            Log.d(TAG, "Osterfeiertage-Tagtypen Migration abgeschlossen: " + zugeordnet + " Tagtypen namensbasiert zugeordnet");
            
        } catch (Exception e) {
            Log.e(TAG, "Fehler bei Migration von Osterfeiertage-Tagtypen", e);
        }
    }
    
    /**
     * Migriert feste Feiertage aus System.xls Sheet 13.
     * Liest Spalte A (Tagtyp), Spalte B (Bezeichnung) ab Zeile 6 und Spalte D (Datum TT.MM).
     */
    private void migrateFesteFeiertageFromExcel(SQLiteDatabase db) {
        migrateFesteFeiertageFromExcel(db, false);
    }
    
    /**
     * Migriert feste Feiertage aus System.xls Sheet 13 (öffentliche Methode für manuelle Migration).
     * Liest Spalte A (Tagtyp), Spalte B (Bezeichnung) ab Zeile 6 und Spalte D (Datum TT.MM).
     * @param force Wenn true, wird Migration auch ausgeführt wenn bereits Daten vorhanden sind
     */
    public void migrateFesteFeiertageFromExcel(boolean force) {
        SQLiteDatabase db = getWritableDatabase();
        migrateFesteFeiertageFromExcel(db, force);
    }
    
    /**
     * Migriert feste Feiertage aus System.xls Sheet 13.
     * Liest Spalte A (Tagtyp), Spalte B (Bezeichnung) ab Zeile 6 und Spalte D (Datum TT.MM).
     */
    private void migrateFesteFeiertageFromExcel(SQLiteDatabase db, boolean force) {
        try {
            if (!force) {
                // Prüfe ob bereits Daten vorhanden sind
                Cursor checkCursor = db.query(TABLE_FESTE_FEIERTAGE,
                    new String[]{"COUNT(*) as count"},
                    null, null, null, null, null);
                boolean hasData = false;
                if (checkCursor.moveToFirst()) {
                    hasData = checkCursor.getInt(0) > 0;
                }
                checkCursor.close();
                
                if (hasData) {
                    Log.d(TAG, "Feste Feiertage haben bereits Daten, überspringe Migration");
                    return;
                }
            }
            
            String sdCardPath = android.os.Environment.getExternalStorageDirectory().getPath();
            String systemXlsPath = sdCardPath + StaticConstants.excellSystemString;
            java.io.File systemFile = new java.io.File(systemXlsPath);
            
            if (!systemFile.exists()) {
                Log.d(TAG, "System.xls nicht gefunden, überspringe Feste-Feiertage-Migration");
                return;
            }
            
            ExcelRead excelread = new ExcelRead();
            excelread.openXlsSheet(systemXlsPath, 13); // Sheet 13 = Feste Feiertage
            
            Log.d(TAG, "Migriere Feste Feiertage aus System.xls Sheet 13");
            
            int zeile = 6; // Beginne ab Zeile 6 (wie vom Benutzer angegeben)
            int zeilen = excelread.getCellZeilen();
            int migrated = 0;
            
            while (zeile < zeilen) {
                try {
                    String tagtypName = excelread.getCellString(0, zeile); // Spalte A = Tagtyp
                    String bezeichnung = excelread.getCellString(1, zeile); // Spalte B = Bezeichnung
                    String datumStr = excelread.getCellString(3, zeile);   // Spalte D = Datum (TT.MM)
                    
                    if (tagtypName == null || tagtypName.trim().isEmpty()) {
                        break; // Ende der Tabelle
                    }
                    
                    // Datum ist wichtig - wenn leer, dann nichts machen
                    if (datumStr == null || datumStr.trim().isEmpty()) {
                        zeile++;
                        continue; // Überspringe Zeilen ohne Datum
                    }
                    
                    // Bezeichnung ist wichtig - wenn leer, dann nichts machen
                    if (bezeichnung == null || bezeichnung.trim().isEmpty()) {
                        zeile++;
                        continue; // Überspringe Zeilen ohne Bezeichnung
                    }
                    
                    bezeichnung = bezeichnung.trim();
                    
                    // Filtere ungültige Einträge (z.B. Nebenuhr-Namen)
                    String tagtypNameLower = tagtypName.toLowerCase().trim();
                    if (tagtypNameLower.equals("stubenuhr") || 
                        tagtypNameLower.equals("nebenuhr") ||
                        tagtypNameLower.equals("nebenuhr a") ||
                        tagtypNameLower.equals("nebenuhr b") ||
                        tagtypNameLower.equals("nebenuhr c")) {
                        Log.w(TAG, "Ungültiger Tagtyp ignoriert (Nebenuhr-Name?): " + tagtypName);
                        zeile++;
                        continue;
                    }
                    
                    // Tagtyp in Tabelle tagtypen anlegen/aktualisieren, damit er für Programme nutzbar ist
                    saveTagtyp(tagtypName.trim(), "festtag_fest");
                    
                    // Validiere und normalisiere Datum-Format (TT.MM -> immer 02-stellig)
                    String[] datumSplit = datumStr.trim().split("\\.");
                    if (datumSplit.length < 2) {
                        Log.w(TAG, "Ungültiges Datum-Format in Zeile " + zeile + ": " + datumStr);
                        zeile++;
                        continue;
                    }
                    
                    try {
                        int tag = Integer.parseInt(datumSplit[0].trim());
                        int monat = Integer.parseInt(datumSplit[1].trim());
                        // Normalisiere zu "TT.MM" Format (immer 2-stellig)
                        String datumNormalized = String.format("%02d.%02d", tag, monat);
                        
                        // Prüfe ob bereits vorhanden
                        Cursor checkCursor = db.query(TABLE_FESTE_FEIERTAGE,
                            new String[]{"id"},
                            "datum = ?",
                            new String[]{datumNormalized},
                            null, null, null);
                        boolean exists = checkCursor.moveToFirst();
                        checkCursor.close();
                        
                        android.content.ContentValues values = new android.content.ContentValues();
                        values.put("name", bezeichnung);
                        values.put("datum", datumNormalized);
                    values.put("tagtyp_name", tagtypName.trim());
                    values.put("updated_at", "CURRENT_TIMESTAMP");
                    
                        if (exists) {
                            db.update(TABLE_FESTE_FEIERTAGE, values, "datum = ?", 
                                new String[]{datumNormalized});
                            Log.d(TAG, "Fester Feiertag aktualisiert: " + bezeichnung + " (" + datumNormalized + ")");
                        } else {
                            db.insert(TABLE_FESTE_FEIERTAGE, null, values);
                            Log.d(TAG, "Fester Feiertag erstellt: " + bezeichnung + " (" + datumNormalized + ")");
                            migrated++;
                        }
                    } catch (NumberFormatException e) {
                        Log.w(TAG, "Ungültiges Datum-Format in Zeile " + zeile + ": " + datumStr, e);
                        zeile++;
                        continue;
                    }
                    
                    zeile++;
                    
                } catch (Exception e) {
                    Log.w(TAG, "Fehler beim Lesen von Zeile " + zeile, e);
                    zeile++;
                }
            }
            
            excelread.closeWorkbook();
            Log.d(TAG, "Feste-Feiertage Migration abgeschlossen: " + migrated + " Feiertage migriert");
            
        } catch (Exception e) {
            Log.e(TAG, "Fehler bei Migration von Feste-Feiertage", e);
        }
    }
    
    /**
     * Lädt alle festen Feiertage aus der Datenbank.
     */
    public List<FesterFeiertag> getAllFesteFeiertage() {
        List<FesterFeiertag> feiertage = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        
        Cursor cursor = db.query(TABLE_FESTE_FEIERTAGE,
            new String[]{"id", "name", "datum", "tagtyp_name", "verschieben_auf_sonntag", "sonntag_im_monat"},
            null, null, null, null, "datum ASC");
        
        try {
            while (cursor.moveToNext()) {
                FesterFeiertag feiertag = new FesterFeiertag();
                feiertag.id = cursor.getInt(0);
                feiertag.name = cursor.getString(1);
                feiertag.datum = cursor.getString(2);
                feiertag.tagtypName = cursor.isNull(3) ? null : cursor.getString(3);
                feiertag.verschiebenAufSonntag = cursor.getInt(4) == 1;
                feiertag.sonntagImMonat = cursor.isNull(5) ? 0 : cursor.getInt(5);
                feiertage.add(feiertag);
            }
        } finally {
            cursor.close();
        }
        
        return feiertage;
    }
    
    /**
     * Speichert einen festen Feiertag (INSERT oder UPDATE).
     */
    public FesterFeiertag saveFesterFeiertag(FesterFeiertag feiertag) {
        SQLiteDatabase db = getWritableDatabase();
        
        android.content.ContentValues values = new android.content.ContentValues();
        values.put("name", feiertag.name);
        values.put("datum", feiertag.datum);
        if (feiertag.tagtypName != null && !feiertag.tagtypName.trim().isEmpty()) {
            values.put("tagtyp_name", feiertag.tagtypName);
        } else {
            values.putNull("tagtyp_name");
        }
        values.put("verschieben_auf_sonntag", feiertag.verschiebenAufSonntag ? 1 : 0);
        values.put("sonntag_im_monat", feiertag.sonntagImMonat);
        values.put("updated_at", "CURRENT_TIMESTAMP");
        
        if (feiertag.id > 0) {
            // UPDATE
            db.update(TABLE_FESTE_FEIERTAGE, values, "id = ?", new String[]{String.valueOf(feiertag.id)});
            Log.d(TAG, "Fester Feiertag aktualisiert: " + feiertag.name);
        } else {
            // INSERT
            long id = db.insert(TABLE_FESTE_FEIERTAGE, null, values);
            if (id <= 0) {
                Log.w(TAG, "Fester Feiertag konnte nicht erstellt werden (evtl. Name+Datum bereits vorhanden): " + feiertag.name + " " + feiertag.datum);
                return null;
            }
            feiertag.id = (int) id;
            Log.d(TAG, "Fester Feiertag erstellt: " + feiertag.name + " (ID: " + id + ")");
        }
        
        return feiertag;
    }
    
    /**
     * Löscht einen festen Feiertag.
     */
    public void deleteFesterFeiertag(int id) {
        SQLiteDatabase db = getWritableDatabase();
        // Lösche auch alle zugehörigen Sonderfeiertage (CASCADE)
        db.delete(TABLE_SONDERFEIERTAGE, "feiertag_id = ?", new String[]{String.valueOf(id)});
        db.delete(TABLE_FESTE_FEIERTAGE, "id = ?", new String[]{String.valueOf(id)});
        Log.d(TAG, "Fester Feiertag gelöscht: ID " + id);
    }
    
    /**
     * Berechnet und speichert Sonderfeiertage (verschobene Feiertage) für einen Feiertag für mehrere Jahre.
     * @param feiertagId ID des Feiertags
     * @param datum Original-Datum im Format "TT.MM"
     * @param tagtypName Tagtyp-Name (kann null sein)
     * @param startJahr Startjahr (z.B. 2020)
     * @param endJahr Endjahr (z.B. 2030)
     * @param sonntagImMonat 0 = nächster Sonntag, 1-4 = 1.-4. Sonntag im Monat
     */
    public void berechneUndSpeichereSonderfeiertage(int feiertagId, String datum, String tagtypName, int startJahr, int endJahr, int sonntagImMonat) {
        SQLiteDatabase db = getWritableDatabase();
        
        try {
            String[] datumParts = datum.split("\\.");
            if (datumParts.length != 2) {
                Log.w(TAG, "Ungültiges Datum-Format für Sonderfeiertag: " + datum);
                return;
            }
            
            int tag = Integer.parseInt(datumParts[0]);
            int monat = Integer.parseInt(datumParts[1]);
            
            // Lösche alte Sonderfeiertage für diesen Feiertag
            db.delete(TABLE_SONDERFEIERTAGE, "feiertag_id = ?", new String[]{String.valueOf(feiertagId)});
            
            // Berechne für jedes Jahr
            for (int jahr = startJahr; jahr <= endJahr; jahr++) {
                java.util.Calendar cal = java.util.Calendar.getInstance();
                cal.set(jahr, monat - 1, tag); // Calendar: Monat 0-11
                
                if (sonntagImMonat > 0 && sonntagImMonat <= 4) {
                    // Berechne n-ten Sonntag im Monat (gleiche Logik wie in Programmsuche)
                    // Setze auf den 1. Tag des Monats
                    cal.set(jahr, monat - 1, 1);
                    
                    // Finde den ersten Sonntag im Monat
                    int wochentag = cal.get(java.util.Calendar.DAY_OF_WEEK);
                    // Calendar: Sonntag=1, Montag=2, ..., Samstag=7
                    int tageBisErsterSonntag;
                    if (wochentag == 1) {
                        tageBisErsterSonntag = 0; // 1. Tag ist bereits Sonntag
                    } else {
                        tageBisErsterSonntag = 8 - wochentag; // Tage bis zum ersten Sonntag
                    }
                    cal.add(java.util.Calendar.DAY_OF_MONTH, tageBisErsterSonntag);
                    
                    // Addiere (sonntagImMonat - 1) Wochen für den n-ten Sonntag
                    cal.add(java.util.Calendar.DAY_OF_MONTH, (sonntagImMonat - 1) * 7);
                    
                    // Validierung: Prüfe ob das berechnete Datum wirklich ein Sonntag ist
                    int berechneterWochentag = cal.get(java.util.Calendar.DAY_OF_WEEK);
                    if (berechneterWochentag != java.util.Calendar.SUNDAY) {
                        Log.w(TAG, "Warnung: Berechnetes Datum ist kein Sonntag! Jahr=" + jahr + ", Monat=" + monat + ", SonntagNr=" + sonntagImMonat);
                    }
                    
                    // Validierung: Prüfe ob es wirklich der n-te Sonntag im Monat ist
                    java.util.Calendar calTest = java.util.Calendar.getInstance();
                    calTest.set(jahr, monat - 1, 1);
                    int sonntagZaehler = 0;
                    while (calTest.get(java.util.Calendar.MONTH) == (monat - 1)) {
                        if (calTest.get(java.util.Calendar.DAY_OF_WEEK) == java.util.Calendar.SUNDAY) {
                            sonntagZaehler++;
                            if (calTest.get(java.util.Calendar.DAY_OF_MONTH) == cal.get(java.util.Calendar.DAY_OF_MONTH)) {
                                if (sonntagZaehler != sonntagImMonat) {
                                    Log.w(TAG, "Warnung: Berechnetes Datum ist nicht der " + sonntagImMonat + ". Sonntag! Tatsächlich: " + sonntagZaehler + ". Sonntag");
                                }
                                break;
                            }
                        }
                        calTest.add(java.util.Calendar.DAY_OF_MONTH, 1);
                    }
                } else if (sonntagImMonat == -1) {
                    // Verschiebe auf näher zum Sonntag:
                    // Mo/Di/Mi → Sonntag vorher, sonst → Sonntag nachher
                    // Berechnung: Datum -3, dann Sonntag im Bereich +7 Tage
                    cal.add(java.util.Calendar.DAY_OF_MONTH, -3);
                    
                    // Finde den Sonntag im Bereich (datum-3) bis (datum-3+7) = (datum-3) bis (datum+4)
                    int wochentag = cal.get(java.util.Calendar.DAY_OF_WEEK);
                    int tageBisSonntag;
                    if (wochentag == java.util.Calendar.SUNDAY) {
                        tageBisSonntag = 0; // Bereits Sonntag
                    } else {
                        tageBisSonntag = 8 - wochentag; // Tage bis zum nächsten Sonntag
                    }
                    cal.add(java.util.Calendar.DAY_OF_MONTH, tageBisSonntag);
                } else {
                    // sonntagImMonat == 0: Keine Verschiebung, gib Original-Datum zurück
                    // (cal ist bereits auf originalDatum gesetzt)
                }
                
                int berechneterTag = cal.get(java.util.Calendar.DAY_OF_MONTH);
                int berechneterMonat = cal.get(java.util.Calendar.MONTH) + 1; // Zurück zu 1-12
                String berechnetesDatum = String.format("%d.%d", berechneterTag, berechneterMonat);
                
                // Nur speichern, wenn verschobenes Datum != Original-Datum
                if (!berechnetesDatum.equals(datum)) {
                    android.content.ContentValues values = new android.content.ContentValues();
                    values.put("feiertag_id", feiertagId);
                    values.put("jahr", jahr);
                    values.put("berechnetes_datum", berechnetesDatum);
                    // Wenn kein Tagtyp angegeben, verwende "Normalprogramm" als Standard
                    if (tagtypName != null && !tagtypName.trim().isEmpty()) {
                        values.put("tagtyp_name", tagtypName);
                    } else {
                        values.put("tagtyp_name", "Normalprogramm");
                    }
                    values.put("updated_at", "CURRENT_TIMESTAMP");
                    
                    db.insert(TABLE_SONDERFEIERTAGE, null, values);
                    Log.d(TAG, "Sonderfeiertag erstellt: Feiertag-ID " + feiertagId + ", Jahr " + jahr + ", Datum " + berechnetesDatum + " (Original: " + datum + ")");
                    
                    // Erstelle auch "Vorfeiertag" für das berechnete Datum
                    try {
                        // Hole Feiertagsname (nicht mehr benötigt, da Name jetzt "Vorfeiertag" ist)
                        Cursor feiertagCursor = db.query(TABLE_FESTE_FEIERTAGE,
                            new String[]{"name"},
                            "id = ?",
                            new String[]{String.valueOf(feiertagId)},
                            null, null, null);
                        
                        String feiertagName = null;
                        if (feiertagCursor.moveToFirst()) {
                            feiertagName = feiertagCursor.getString(0);
                        }
                        feiertagCursor.close();
                        
                        if (feiertagName != null) {
                            // Berechne Tag vor dem berechneten Datum (für Sonderfeiertag)
                            java.util.Calendar calVorher = (java.util.Calendar) cal.clone();
                            calVorher.add(java.util.Calendar.DAY_OF_MONTH, -1);
                            
                            int tagVorher = calVorher.get(java.util.Calendar.DAY_OF_MONTH);
                            int monatVorher = calVorher.get(java.util.Calendar.MONTH) + 1;
                            String berechnetesDatumVorher = String.format("%d.%d", tagVorher, monatVorher);
                            
                            // Berechne Tag vor dem Original-Datum (für feste_feiertage)
                            // datumParts ist bereits oben definiert (Zeile 2785)
                            if (datumParts.length == 2) {
                                int originalTag = Integer.parseInt(datumParts[0]);
                                int originalMonat = Integer.parseInt(datumParts[1]);
                                
                                java.util.Calendar calOriginalVorher = java.util.Calendar.getInstance();
                                calOriginalVorher.set(jahr, originalMonat - 1, originalTag); // Calendar: Monat 0-11
                                calOriginalVorher.add(java.util.Calendar.DAY_OF_MONTH, -1);
                                
                                int tagOriginalVorher = calOriginalVorher.get(java.util.Calendar.DAY_OF_MONTH);
                                int monatOriginalVorher = calOriginalVorher.get(java.util.Calendar.MONTH) + 1;
                                String originalDatumVorher = String.format("%d.%d", tagOriginalVorher, monatOriginalVorher);
                                
                                // Prüfe ob "Vorfeiertag" Feiertag bereits existiert (mit Original-Datum)
                                String nameVorher = "Vorfeiertag";
                                Cursor vorFeiertagCursor = db.query(TABLE_FESTE_FEIERTAGE,
                                    new String[]{"id"},
                                    "name = ? AND datum = ?",
                                    new String[]{nameVorher, originalDatumVorher},
                                    null, null, null);
                                
                                int vorFeiertagId = feiertagId; // Standard: gleiche Feiertag-ID
                                if (!vorFeiertagCursor.moveToFirst()) {
                                    // Erstelle neuen "Vorfeiertag" Feiertag mit Original-Datum minus 1 Tag
                                    android.content.ContentValues vorFeiertagValues = new android.content.ContentValues();
                                    vorFeiertagValues.put("name", nameVorher);
                                    vorFeiertagValues.put("datum", originalDatumVorher);
                                    vorFeiertagValues.putNull("tagtyp_name");
                                    vorFeiertagValues.put("verschieben_auf_sonntag", 0);
                                    vorFeiertagValues.put("updated_at", "CURRENT_TIMESTAMP");
                                    
                                    long vorFeiertagIdLong = db.insert(TABLE_FESTE_FEIERTAGE, null, vorFeiertagValues);
                                    vorFeiertagId = (int) vorFeiertagIdLong;
                                    Log.d(TAG, "Vorfeiertag erstellt: " + nameVorher + " (Original-Datum: " + originalDatumVorher + ", ID: " + vorFeiertagId + ")");
                                } else {
                                    vorFeiertagId = vorFeiertagCursor.getInt(0);
                                }
                                vorFeiertagCursor.close();
                                
                                // Prüfe ob Sonderfeiertag für "Vorfeiertag" bereits existiert (mit berechnetem Datum minus 1 Tag)
                                Cursor vorSonderfeiertagCursor = db.query(TABLE_SONDERFEIERTAGE,
                                    new String[]{"id"},
                                    "feiertag_id = ? AND jahr = ? AND berechnetes_datum = ?",
                                    new String[]{String.valueOf(vorFeiertagId), String.valueOf(jahr), berechnetesDatumVorher},
                                    null, null, null);
                                
                                if (!vorSonderfeiertagCursor.moveToFirst()) {
                                    // Erstelle Sonderfeiertag für "Vorfeiertag" mit berechnetem Datum minus 1 Tag
                                    android.content.ContentValues vorSonderfeiertagValues = new android.content.ContentValues();
                                    vorSonderfeiertagValues.put("feiertag_id", vorFeiertagId);
                                    vorSonderfeiertagValues.put("jahr", jahr);
                                    vorSonderfeiertagValues.put("berechnetes_datum", berechnetesDatumVorher);
                                    vorSonderfeiertagValues.put("tagtyp_name", "Normalprogramm"); // Standard: Normalprogramm
                                    vorSonderfeiertagValues.put("updated_at", "CURRENT_TIMESTAMP");
                                    
                                    db.insert(TABLE_SONDERFEIERTAGE, null, vorSonderfeiertagValues);
                                    Log.d(TAG, "Vorfeiertag Sonderfeiertag erstellt: " + nameVorher + ", Jahr " + jahr + ", Berechnetes Datum " + berechnetesDatumVorher + " (Original: " + originalDatumVorher + ")");
                                }
                                vorSonderfeiertagCursor.close();
                            }
                        }
                    } catch (Exception e) {
                        Log.w(TAG, "Fehler beim Erstellen von 'Vorfeiertag' für Sonderfeiertag", e);
                        // Fehler wird ignoriert, Haupt-Sonderfeiertag wird trotzdem gespeichert
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Fehler beim Berechnen von Sonderfeiertagen", e);
        }
    }
    
    /**
     * Berechnet das verschobene Datum für einen Sonderfeiertag (ohne DB-Zugriff).
     * @param jahr Jahr (z.B. 2026)
     * @param originalDatum Original-Datum im Format "TT.MM" (z.B. "21.1")
     * @param sonntagImMonat 0 = nächster Sonntag, 1-4 = 1.-4. Sonntag im Monat
     * @return Berechnetes Datum im Format "TT.MM" oder null bei Fehler
     */
    private String berechneSonderfeiertagDatum(int jahr, String originalDatum, int sonntagImMonat) {
        try {
            String[] datumParts = originalDatum.split("\\.");
            if (datumParts.length != 2) {
                Log.w(TAG, "Ungültiges Datum-Format für Sonderfeiertag: " + originalDatum);
                return null;
            }
            
            int tag = Integer.parseInt(datumParts[0]);
            int monat = Integer.parseInt(datumParts[1]);
            
            java.util.Calendar cal = java.util.Calendar.getInstance();
            cal.set(jahr, monat - 1, tag); // Calendar: Monat 0-11
            
            if (sonntagImMonat > 0 && sonntagImMonat <= 4) {
                // Berechne n-ten Sonntag im Monat
                cal.set(jahr, monat - 1, 1);
                
                int wochentag = cal.get(java.util.Calendar.DAY_OF_WEEK);
                int tageBisErsterSonntag;
                if (wochentag == 1) {
                    tageBisErsterSonntag = 0;
                } else {
                    tageBisErsterSonntag = 8 - wochentag;
                }
                cal.add(java.util.Calendar.DAY_OF_MONTH, tageBisErsterSonntag);
                cal.add(java.util.Calendar.DAY_OF_MONTH, (sonntagImMonat - 1) * 7);
            } else if (sonntagImMonat == -1) {
                // Verschiebe auf näher zum Sonntag:
                // Mo/Di/Mi → Sonntag vorher, sonst → Sonntag nachher
                // Berechnung: Datum -3, dann Sonntag im Bereich +7 Tage
                cal.add(java.util.Calendar.DAY_OF_MONTH, -3);
                
                // Finde den Sonntag im Bereich (datum-3) bis (datum-3+7) = (datum-3) bis (datum+4)
                int wochentag = cal.get(java.util.Calendar.DAY_OF_WEEK);
                int tageBisSonntag;
                if (wochentag == java.util.Calendar.SUNDAY) {
                    tageBisSonntag = 0; // Bereits Sonntag
                } else {
                    tageBisSonntag = 8 - wochentag; // Tage bis zum nächsten Sonntag
                }
                cal.add(java.util.Calendar.DAY_OF_MONTH, tageBisSonntag);
            } else {
                // sonntagImMonat == 0: Keine Verschiebung, gib Original-Datum zurück
                // (cal ist bereits auf originalDatum gesetzt)
            }
            
            int berechneterTag = cal.get(java.util.Calendar.DAY_OF_MONTH);
            int berechneterMonat = cal.get(java.util.Calendar.MONTH) + 1;
            return String.format("%d.%d", berechneterTag, berechneterMonat);
        } catch (Exception e) {
            Log.e(TAG, "Fehler beim Berechnen des Sonderfeiertag-Datums", e);
            return null;
        }
    }
    
    /**
     * Gibt den Tagtyp-Namen für einen Sonderfeiertag zurück (basierend auf Datum und Jahr).
     * Wenn kein Eintrag im Cache gefunden wird, wird dynamisch berechnet (wie Osterfeiertage).
     * @param tag Tag (1-31)
     * @param monat Monat (0-11, Calendar.MONTH Format)
     * @param jahr Jahr (z.B. 2026)
     * @return Tagtyp-Dateiname oder null, wenn kein Sonderfeiertag gefunden
     */
    public String getTagtypNameForSonderfeiertag(int tag, int monat, int jahr) {
        SQLiteDatabase db = getReadableDatabase();
        
        // Konvertiere Monat von Calendar-Format (0-11) zu normalem Format (1-12)
        int monatNormal = monat + 1;
        // Normalisiere zu "TT.MM" Format
        String datumStr = String.format("%d.%d", tag, monatNormal);
        String datumStr2 = String.format("%02d.%02d", tag, monatNormal); // Mit führenden Nullen
        
        Log.d(TAG, "getTagtypNameForSonderfeiertag: Suche für Datum " + tag + "." + monatNormal + ", Jahr " + jahr);
        
        // 1. Suche zuerst im Cache (sonderfeiertage Tabelle)
        Cursor cursor = db.query(TABLE_SONDERFEIERTAGE,
            new String[]{"tagtyp_name", "feiertag_id"},
            "(berechnetes_datum = ? OR berechnetes_datum = ?) AND jahr = ?",
            new String[]{datumStr, datumStr2, String.valueOf(jahr)},
            null, null, null);
        
        try {
            if (cursor.moveToFirst()) {
                String tagtypName = cursor.isNull(0) ? null : cursor.getString(0);
                int feiertagId = cursor.getInt(1);
                
                Log.d(TAG, "getTagtypNameForSonderfeiertag: Gefunden im Cache - Feiertag-ID: " + feiertagId + ", Tagtyp: " + tagtypName);
                
                if (tagtypName != null && !tagtypName.trim().isEmpty()) {
                    return getTagtypNameFromFile(tagtypName.trim());
                }
            }
        } finally {
            cursor.close();
        }
        
        // 2. Dynamische Berechnung (wie Osterfeiertage): Suche in feste_feiertage nach Feiertagen mit Verschiebung
        Log.d(TAG, "getTagtypNameForSonderfeiertag: Nicht im Cache gefunden, berechne dynamisch");
        
        Cursor feiertagCursor = db.query(TABLE_FESTE_FEIERTAGE,
            new String[]{"id", "name", "datum", "tagtyp_name", "verschieben_auf_sonntag", "sonntag_im_monat"},
            "verschieben_auf_sonntag = 1",
            null, null, null, null);
        
        try {
            while (feiertagCursor.moveToNext()) {
                int feiertagId = feiertagCursor.getInt(0);
                String feiertagName = feiertagCursor.getString(1);
                String originalDatum = feiertagCursor.getString(2);
                String tagtypName = feiertagCursor.isNull(3) ? null : feiertagCursor.getString(3);
                int sonntagImMonat = feiertagCursor.isNull(5) ? 0 : feiertagCursor.getInt(5);
                
                // Berechne verschobenes Datum für dieses Jahr
                String berechnetesDatum = berechneSonderfeiertagDatum(jahr, originalDatum, sonntagImMonat);
                
                if (berechnetesDatum != null) {
                    // Normalisiere beide Formate (mit/ohne führende Nullen)
                    String berechnetesDatum2 = String.format("%02d.%02d", 
                        Integer.parseInt(berechnetesDatum.split("\\.")[0]),
                        Integer.parseInt(berechnetesDatum.split("\\.")[1]));
                    
                    // Prüfe ob das berechnete Datum mit dem angefragten Datum übereinstimmt
                    if (berechnetesDatum.equals(datumStr) || berechnetesDatum.equals(datumStr2) ||
                        berechnetesDatum2.equals(datumStr) || berechnetesDatum2.equals(datumStr2)) {
                        
                        Log.d(TAG, "getTagtypNameForSonderfeiertag: Dynamisch berechnet - Feiertag: " + feiertagName + 
                            ", Original: " + originalDatum + ", Berechnet: " + berechnetesDatum);
                        
                        // Speichere im Cache für zukünftige Abfragen
                        SQLiteDatabase dbWrite = getWritableDatabase();
                        android.content.ContentValues values = new android.content.ContentValues();
                        values.put("feiertag_id", feiertagId);
                        values.put("jahr", jahr);
                        values.put("berechnetes_datum", berechnetesDatum);
                        if (tagtypName != null && !tagtypName.trim().isEmpty()) {
                            values.put("tagtyp_name", tagtypName);
                        } else {
                            values.put("tagtyp_name", "Normalprogramm");
                        }
                        values.put("updated_at", "CURRENT_TIMESTAMP");
                        
                        // Prüfe ob bereits vorhanden (vermeide Duplikate)
                        Cursor checkCursor = dbWrite.query(TABLE_SONDERFEIERTAGE,
                            new String[]{"id"},
                            "feiertag_id = ? AND jahr = ? AND berechnetes_datum = ?",
                            new String[]{String.valueOf(feiertagId), String.valueOf(jahr), berechnetesDatum},
                            null, null, null);
                        
                        if (!checkCursor.moveToFirst()) {
                            dbWrite.insert(TABLE_SONDERFEIERTAGE, null, values);
                            Log.d(TAG, "getTagtypNameForSonderfeiertag: In Cache gespeichert");
                        }
                        checkCursor.close();
                        
                        // Gib Tagtyp zurück
                        if (tagtypName != null && !tagtypName.trim().isEmpty()) {
                            return getTagtypNameFromFile(tagtypName.trim());
                        }
                    }
                }
            }
        } finally {
            feiertagCursor.close();
        }
        
        return null;
    }
    
    /**
     * Entity-Klasse für Osterfeiertag.
     */
    public static class Osterfeiertag {
        public int id;
        public String name;              // Feiertagsname (z.B. "Karfreitag")
        public int osterOffset;         // Tage +/- zum Osterdatum (z.B. -2)
        public String tagtypName;       // Tagtyp-Dateiname (z.B. "Karfreitag.xls") oder null
    }
    
    /**
     * Entity-Klasse für Fester Feiertag.
     */
    public static class FesterFeiertag {
        public int id;
        public String name;              // Bezeichnung (z.B. "Weihnachten")
        public String datum;            // Fixes Datum im Format "TT.MM" (z.B. "25.12")
        public String tagtypName;       // Tagtyp-Dateiname (z.B. "Weihnachten.xls") oder null
        public boolean verschiebenAufSonntag; // Wenn true: Verschiebung auf Sonntag aktiviert
        public int sonntagImMonat;      // 0 = nächster Sonntag, 1-4 = 1.-4. Sonntag im Monat
    }
    
    /**
     * Entity-Klasse für Sonderfeiertag (verschobener Feiertag).
     */
    public static class Sonderfeiertag {
        public int id;
        public int feiertagId;          // Foreign Key zu feste_feiertage
        public int jahr;                // Jahr (z.B. 2026)
        public String berechnetesDatum; // Berechnetes Datum im Format "TT.MM" (z.B. "18.1")
        public String tagtypName;       // Tagtyp-Dateiname (z.B. "Feiertag.xls") oder null
        public String feiertagName;     // Name des zugehörigen Feiertags (für Anzeige)
        public String originalDatum;    // Original-Datum des Feiertags (für Anzeige)
    }
    
    /**
     * Lädt alle Sonderfeiertage aus der Datenbank.
     */
    public List<Sonderfeiertag> getAllSonderfeiertage() {
        List<Sonderfeiertag> sonderfeiertage = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        
        // JOIN mit feste_feiertage, um Name und Original-Datum zu erhalten
        String query = "SELECT s.id, s.feiertag_id, s.jahr, s.berechnetes_datum, s.tagtyp_name, " +
                      "f.name as feiertag_name, f.datum as original_datum " +
                      "FROM " + TABLE_SONDERFEIERTAGE + " s " +
                      "LEFT JOIN " + TABLE_FESTE_FEIERTAGE + " f ON s.feiertag_id = f.id " +
                      "ORDER BY s.jahr ASC, s.berechnetes_datum ASC";
        
        Cursor cursor = db.rawQuery(query, null);
        
        try {
            while (cursor.moveToNext()) {
                Sonderfeiertag sonderfeiertag = new Sonderfeiertag();
                sonderfeiertag.id = cursor.getInt(0);
                sonderfeiertag.feiertagId = cursor.getInt(1);
                sonderfeiertag.jahr = cursor.getInt(2);
                sonderfeiertag.berechnetesDatum = cursor.getString(3);
                sonderfeiertag.tagtypName = cursor.isNull(4) ? null : cursor.getString(4);
                sonderfeiertag.feiertagName = cursor.isNull(5) ? null : cursor.getString(5);
                sonderfeiertag.originalDatum = cursor.isNull(6) ? null : cursor.getString(6);
                sonderfeiertage.add(sonderfeiertag);
            }
        } finally {
            cursor.close();
        }
        
        return sonderfeiertage;
    }
    
    /**
     * Speichert einen Sonderfeiertag (INSERT oder UPDATE).
     */
    public Sonderfeiertag saveSonderfeiertag(Sonderfeiertag sonderfeiertag) {
        SQLiteDatabase db = getWritableDatabase();
        
        android.content.ContentValues values = new android.content.ContentValues();
        values.put("feiertag_id", sonderfeiertag.feiertagId);
        values.put("jahr", sonderfeiertag.jahr);
        values.put("berechnetes_datum", sonderfeiertag.berechnetesDatum);
        if (sonderfeiertag.tagtypName != null && !sonderfeiertag.tagtypName.trim().isEmpty()) {
            values.put("tagtyp_name", sonderfeiertag.tagtypName);
        } else {
            values.putNull("tagtyp_name");
        }
        values.put("updated_at", "CURRENT_TIMESTAMP");
        
        if (sonderfeiertag.id > 0) {
            // UPDATE
            db.update(TABLE_SONDERFEIERTAGE, values, "id = ?", new String[]{String.valueOf(sonderfeiertag.id)});
            Log.d(TAG, "Sonderfeiertag aktualisiert: ID " + sonderfeiertag.id);
        } else {
            // INSERT
            long id = db.insert(TABLE_SONDERFEIERTAGE, null, values);
            sonderfeiertag.id = (int) id;
            Log.d(TAG, "Sonderfeiertag erstellt: Jahr " + sonderfeiertag.jahr + ", Datum " + sonderfeiertag.berechnetesDatum + " (ID: " + id + ")");
        }
        
        return sonderfeiertag;
    }
    
    /**
     * Löscht einen Sonderfeiertag.
     */
    public void deleteSonderfeiertag(int id) {
        SQLiteDatabase db = getWritableDatabase();
        db.delete(TABLE_SONDERFEIERTAGE, "id = ?", new String[]{String.valueOf(id)});
        Log.d(TAG, "Sonderfeiertag gelöscht: ID " + id);
    }
    
    /**
     * Löscht alle Sonderfeiertage für einen bestimmten Feiertag.
     * @param feiertagId ID des Feiertags
     */
    public void deleteSonderfeiertageByFeiertagId(int feiertagId) {
        SQLiteDatabase db = getWritableDatabase();
        db.delete(TABLE_SONDERFEIERTAGE, "feiertag_id = ?", new String[]{String.valueOf(feiertagId)});
        Log.d(TAG, "Alle Sonderfeiertage gelöscht für Feiertag-ID: " + feiertagId);
    }
    
    /**
     * Löscht alle Sonderfeiertage aus der Datenbank.
     * Da Sonderfeiertage jetzt dynamisch berechnet werden, können alte Einträge gelöscht werden.
     */
    public int deleteAllSonderfeiertage() {
        SQLiteDatabase db = getWritableDatabase();
        int count = db.delete(TABLE_SONDERFEIERTAGE, null, null);
        Log.d(TAG, "Alle Sonderfeiertage gelöscht: " + count + " Einträge");
        return count;
    }
    
    /**
     * Gibt den Tagtyp-Namen für einen festen Feiertag zurück (basierend auf Datum).
     * @param tag Tag (1-31)
     * @param monat Monat (0-11, Calendar.MONTH Format)
     * @return Tagtyp-Dateiname oder null, wenn kein Feiertag gefunden
     */
    public String getTagtypNameForFesterFeiertag(int tag, int monat) {
        return getTagtypNameForFesterFeiertag(tag, monat, java.util.Calendar.getInstance().get(java.util.Calendar.YEAR));
    }
    
    /**
     * Gibt den Tagtyp-Namen für einen festen Feiertag zurück (basierend auf Datum und Jahr).
     * @param tag Tag (1-31)
     * @param monat Monat (0-11, Calendar.MONTH Format)
     * @param jahr Jahr (z.B. 2026)
     * @return Tagtyp-Dateiname oder null, wenn kein Feiertag gefunden
     */
    public String getTagtypNameForFesterFeiertag(int tag, int monat, int jahr) {
        SQLiteDatabase db = getReadableDatabase();
        
        // Konvertiere Monat von Calendar-Format (0-11) zu normalem Format (1-12)
        int monatNormal = monat + 1;
        // Normalisiere zu "TT.MM" Format (immer 2-stellig)
        String datumStr = String.format("%02d.%02d", tag, monatNormal);
        String datumStr2 = String.format("%d.%d", tag, monatNormal); // Ohne führende Nullen
        
        Log.d(TAG, "getTagtypNameForFesterFeiertag: Suche für Datum " + tag + "." + monatNormal + ", Jahr " + jahr + " (Format: '" + datumStr + "' oder '" + datumStr2 + "')");
        
        // 1. Prüfe zuerst Sonderfeiertage (verschobene Feiertage)
        String sonderfeiertagTagtyp = getTagtypNameForSonderfeiertag(tag, monat, jahr);
        if (sonderfeiertagTagtyp != null) {
            return sonderfeiertagTagtyp;
        }
        
        // 2. Normale Suche: Feiertage ohne Verschiebung
        
        // 1. Normale Suche: Feiertage ohne Verschiebung
        Cursor cursor = db.query(TABLE_FESTE_FEIERTAGE,
            new String[]{"tagtyp_name", "name", "datum", "verschieben_auf_sonntag"},
            "(datum = ? OR datum = ?) AND (verschieben_auf_sonntag = 0 OR verschieben_auf_sonntag IS NULL)",
            new String[]{datumStr, datumStr2}, // Prüfe auch ohne führende Nullen
            null, null, null);
        
        Log.d(TAG, "getTagtypNameForFesterFeiertag: Normale Suche - Gefundene Zeilen: " + cursor.getCount());
        
        try {
            if (cursor.moveToFirst()) {
                String tagtypName = cursor.isNull(0) ? null : cursor.getString(0);
                String bezeichnung = cursor.isNull(1) ? null : cursor.getString(1);
                String datum = cursor.isNull(2) ? null : cursor.getString(2);
                
                Log.d(TAG, "getTagtypNameForFesterFeiertag: Gefunden (ohne Verschiebung) - Bezeichnung: " + bezeichnung + ", Tagtyp: " + tagtypName + ", Datum: " + datum);
                
                if (tagtypName != null && !tagtypName.trim().isEmpty()) {
                    return getTagtypNameFromFile(tagtypName.trim());
                }
            }
        } finally {
            cursor.close();
        }
        
        // Kein Feiertag gefunden
        Log.d(TAG, "getTagtypNameForFesterFeiertag: Kein fester Feiertag für " + datumStr + " gefunden");
        return null;
    }
    
    /**
     * Hilfsmethode: Gibt den Tagtyp-Namen zurück, nachdem geprüft wurde, ob die Datei existiert.
     */
    private String getTagtypNameFromFile(String tagtypName) {
        // Stelle sicher, dass der Dateiname korrekt formatiert ist
        // Prüfe ob Datei existiert (mit oder ohne .xls)
        String sdCardPath = android.os.Environment.getExternalStorageDirectory().getPath();
        String programmtagePath = sdCardPath + "/Turmtechnik/Programmtage/";
        
        // Versuche mit .xls
        java.io.File fileWithXls = new java.io.File(programmtagePath + tagtypName);
        if (fileWithXls.exists()) {
            Log.d(TAG, "getTagtypNameFromFile: Datei gefunden: " + fileWithXls.getAbsolutePath());
            return tagtypName;
        }
        
        // Versuche ohne .xls
        if (!tagtypName.toLowerCase().endsWith(".xls")) {
            java.io.File fileWithoutXls = new java.io.File(programmtagePath + tagtypName + ".xls");
            if (fileWithoutXls.exists()) {
                Log.d(TAG, "getTagtypNameFromFile: Datei gefunden (mit .xls): " + fileWithoutXls.getAbsolutePath());
                return tagtypName + ".xls";
            }
        } else {
            // Tagtyp hat .xls, aber Datei existiert nicht - versuche ohne
            String nameWithoutXls = tagtypName.substring(0, tagtypName.length() - 4);
            java.io.File fileWithoutXls = new java.io.File(programmtagePath + nameWithoutXls);
            if (fileWithoutXls.exists()) {
                Log.d(TAG, "getTagtypNameFromFile: Datei gefunden (ohne .xls): " + fileWithoutXls.getAbsolutePath());
                return nameWithoutXls;
            }
        }
        
        // Datei existiert nicht, aber Tagtyp ist in DB - gib trotzdem zurück
        // (Excel-Fallback wird dann verwendet)
        Log.w(TAG, "getTagtypNameFromFile: Tagtyp-Datei nicht gefunden: " + tagtypName + " in " + programmtagePath);
        return tagtypName;
    }
    
    /**
     * Gibt den Tagtyp-Namen für einen Osterfeiertag zurück (basierend auf Datum).
     * Berechnet das Osterdatum für das Jahr und prüft dann die Feiertage.
     * @param tag Tag (1-31)
     * @param monat Monat (0-11, Calendar.MONTH Format)
     * @param jahr Jahr (z.B. 2024)
     * @return Tagtyp-Dateiname oder null, wenn kein Feiertag gefunden
     */
    /**
     * Berechnet Tag und Monat (Calendar: Monat 0-11) eines weihnachtsbezogenen Feiertags (Christkönig, Advent) für ein Jahr.
     * @return int[2] = { tag, monat } oder null wenn Name nicht Christkönig/Advent
     */
    private int[] berechneWeihnachtsbezogenerFeiertagDatum(String feiertagName, int jahr) {
        if (feiertagName == null || (!feiertagName.contains("Christkönig") && !feiertagName.contains("Advent")))
            return null;
        java.util.Calendar weihnachten = java.util.Calendar.getInstance();
        weihnachten.set(jahr, java.util.Calendar.DECEMBER, 25);
        int wochentagWeihnachten = weihnachten.get(java.util.Calendar.DAY_OF_WEEK);
        int tageZurueckZumSonntag = (wochentagWeihnachten == 1) ? 7 : (wochentagWeihnachten - 1);
        java.util.Calendar cal = (java.util.Calendar) weihnachten.clone();
        cal.add(java.util.Calendar.DAY_OF_MONTH, -tageZurueckZumSonntag);
        if (feiertagName.contains("Christkönig")) {
            cal.add(java.util.Calendar.DAY_OF_MONTH, -3 * 7); // 1. Advent
            cal.add(java.util.Calendar.DAY_OF_MONTH, -7);     // Christkönig Sonntag
            if (feiertagName.contains("Samstag"))
                cal.add(java.util.Calendar.DAY_OF_MONTH, -1);
            return new int[]{ cal.get(java.util.Calendar.DAY_OF_MONTH), cal.get(java.util.Calendar.MONTH) };
        }
        if (feiertagName.contains("Advent")) {
            int adventNummer = 1;
            if (feiertagName.contains("2.")) adventNummer = 2;
            else if (feiertagName.contains("3.")) adventNummer = 3;
            else if (feiertagName.contains("4.")) adventNummer = 4;
            cal.add(java.util.Calendar.DAY_OF_MONTH, -(4 - adventNummer) * 7);
            return new int[]{ cal.get(java.util.Calendar.DAY_OF_MONTH), cal.get(java.util.Calendar.MONTH) };
        }
        return null;
    }

    public String getTagtypNameForOsterfeiertag(int tag, int monat, int jahr) {
        SQLiteDatabase db = getReadableDatabase();

        // Zuerst: Weihnachtsbezogene Feiertage (Christkönig, Advent) per berechnetem Datum suchen
        Cursor alleOster = db.query(TABLE_OSTERFEIERTAGE,
            new String[]{"id", "name", "tagtyp_name"},
            null, null, null, null, null);
        try {
            while (alleOster.moveToNext()) {
                String name = alleOster.isNull(1) ? null : alleOster.getString(1);
                if (name != null && (name.contains("Christkönig") || name.contains("Advent"))) {
                    int[] datum = berechneWeihnachtsbezogenerFeiertagDatum(name, jahr);
                    if (datum != null && datum[0] == tag && datum[1] == monat) {
                        String tagtypName = alleOster.isNull(2) ? null : alleOster.getString(2);
                        if (tagtypName != null && !tagtypName.trim().isEmpty()) {
                            tagtypName = tagtypName.trim();
                            String sdCardPath = android.os.Environment.getExternalStorageDirectory().getPath();
                            String programmtagePath = sdCardPath + "/Turmtechnik/Programmtage/";
                            java.io.File fileWithXls = new java.io.File(programmtagePath + tagtypName);
                            if (fileWithXls.exists()) return tagtypName;
                            if (!tagtypName.toLowerCase().endsWith(".xls")) {
                                java.io.File f = new java.io.File(programmtagePath + tagtypName + ".xls");
                                if (f.exists()) return tagtypName + ".xls";
                            } else {
                                String nameWithoutXls = tagtypName.substring(0, tagtypName.length() - 4);
                                if (new java.io.File(programmtagePath + nameWithoutXls).exists()) return nameWithoutXls;
                            }
                            return tagtypName;
                        }
                    }
                }
            }
        } finally {
            alleOster.close();
        }

        Log.d(TAG, "[Programmtag] getTagtypNameForOsterfeiertag: angefragt " + tag + "." + (monat + 1) + "." + jahr);
        java.util.Calendar ostern = berechneOsterdatum(jahr);
        aufMitternacht(ostern);
        Log.d(TAG, "[Programmtag] Osterdatum " + jahr + " = " + ostern.get(java.util.Calendar.DAY_OF_MONTH) + "." + (ostern.get(java.util.Calendar.MONTH) + 1));
        
        // Alle Osterfeiertage durchgehen, Feiertagsdatum = Ostern + Offset berechnen, mit (tag, monat) vergleichen (DST-sicher, keine Millisekunden-Differenz)
        Cursor cursor = db.query(TABLE_OSTERFEIERTAGE,
            new String[]{"tagtyp_name", "name", "oster_offset"},
            null, null, null, null, null);
        try {
            while (cursor.moveToNext()) {
                int offset = cursor.isNull(2) ? 0 : cursor.getInt(2);
                java.util.Calendar feiertagDatum = java.util.Calendar.getInstance();
                feiertagDatum.setTimeInMillis(ostern.getTimeInMillis());
                feiertagDatum.add(java.util.Calendar.DAY_OF_MONTH, offset);
                aufMitternacht(feiertagDatum);
                int fdTag = feiertagDatum.get(java.util.Calendar.DAY_OF_MONTH);
                int fdMonat = feiertagDatum.get(java.util.Calendar.MONTH);
                if (fdTag != tag || fdMonat != monat) continue;
                String tagtypName = cursor.isNull(0) ? null : cursor.getString(0);
                String feiertagName = cursor.isNull(1) ? null : cursor.getString(1);
                Log.d(TAG, "getTagtypNameForOsterfeiertag: Gefunden - Name: " + feiertagName + ", Tagtyp: " + tagtypName + ", Offset: " + offset);
                if (tagtypName != null && !tagtypName.trim().isEmpty()) {
                    tagtypName = tagtypName.trim();
                    String sdCardPath = android.os.Environment.getExternalStorageDirectory().getPath();
                    String programmtagePath = sdCardPath + "/Turmtechnik/Programmtage/";
                    java.io.File fileWithXls = new java.io.File(programmtagePath + tagtypName);
                    if (fileWithXls.exists()) {
                        Log.d(TAG, "getTagtypNameForOsterfeiertag: Datei gefunden: " + fileWithXls.getAbsolutePath());
                        return tagtypName;
                    }
                    if (!tagtypName.toLowerCase().endsWith(".xls")) {
                        java.io.File fileWithoutXls = new java.io.File(programmtagePath + tagtypName + ".xls");
                        if (fileWithoutXls.exists()) {
                            Log.d(TAG, "getTagtypNameForOsterfeiertag: Datei gefunden (mit .xls)");
                            return tagtypName + ".xls";
                        }
                    } else {
                        String nameWithoutXls = tagtypName.substring(0, tagtypName.length() - 4);
                        java.io.File fileWithoutXls = new java.io.File(programmtagePath + nameWithoutXls);
                        if (fileWithoutXls.exists()) {
                            Log.d(TAG, "getTagtypNameForOsterfeiertag: Datei gefunden (ohne .xls)");
                            return nameWithoutXls;
                        }
                    }
                    Log.w(TAG, "getTagtypNameForOsterfeiertag: Tagtyp-Datei nicht gefunden: " + tagtypName);
                    return tagtypName;
                }
            }
            Log.d(TAG, "getTagtypNameForOsterfeiertag: Kein Osterfeiertag für " + tag + "." + (monat + 1) + "." + jahr + " gefunden");
        } finally {
            cursor.close();
        }
        return null;
    }
    
    /**
     * Gibt den Feiertagsnamen und das Datum für ein bestimmtes Datum zurück (für Anzeige).
     * @param tag Tag (1-31)
     * @param monat Monat (0-11, Calendar.MONTH Format)
     * @param jahr Jahr (z.B. 2026)
     * @return FeiertagsInfo mit Name und Datum, oder null wenn kein Feiertag gefunden
     */
    public static class FeiertagsInfo {
        public String name;
        public String datum; // Format: "TT.MM.JJJJ"
        
        public FeiertagsInfo(String name, String datum) {
            this.name = name;
            this.datum = datum;
        }
    }
    
    public FeiertagsInfo getFeiertagsInfo(int tag, int monat, int jahr) {
        Log.d(TAG, "[Feiertag] getFeiertagsInfo: angefragt " + tag + "." + (monat + 1) + "." + jahr);
        // Vortag eines Osterfeiertags (z. B. 17.02 vor Aschermittwoch) nie als Feiertag melden – zuerst prüfen, bevor Tabellen gelesen werden
        if (istVortagOsterfeiertag(tag, monat, jahr)) {
            Log.d(TAG, "[Feiertag] getFeiertagsInfo: ist Vortag eines Osterfeiertags -> return null");
            return null;
        }
        SQLiteDatabase db = getReadableDatabase();
        
        // Konvertiere Monat von Calendar-Format (0-11) zu normalem Format (1-12)
        int monatNormal = monat + 1;
        String datumStr = String.format("%02d.%02d", tag, monatNormal);
        String datumStr2 = String.format("%d.%d", tag, monatNormal);
        String datumVoll = String.format("%02d.%02d.%04d", tag, monatNormal, jahr);
        
        // 1. Prüfe Sonderfeiertage (verschobene Feiertage)
        Cursor sonderCursor = db.query(TABLE_SONDERFEIERTAGE,
            new String[]{"feiertag_id", "berechnetes_datum"},
            "(berechnetes_datum = ? OR berechnetes_datum = ?) AND jahr = ?",
            new String[]{datumStr, datumStr2, String.valueOf(jahr)},
            null, null, null);
        
        try {
            if (sonderCursor.moveToFirst()) {
                int feiertagId = sonderCursor.getInt(0);
                String berechnetesDatum = sonderCursor.getString(1);
                
                // Hole Feiertagsname aus feste_feiertage
                Cursor feiertagCursor = db.query(TABLE_FESTE_FEIERTAGE,
                    new String[]{"name", "datum"},
                    "id = ?",
                    new String[]{String.valueOf(feiertagId)},
                    null, null, null);
                
                if (feiertagCursor.moveToFirst()) {
                    String feiertagName = feiertagCursor.getString(0);
                    feiertagCursor.close();
                    // Ist dieser Feiertag ein Osterfeiertag? Dann nur übernehmen, wenn das angefragte Datum dem berechneten Osterdatum entspricht (vermeidet falsche Einträge in sonderfeiertage).
                    Cursor osterCheck = db.query(TABLE_OSTERFEIERTAGE, new String[]{"oster_offset"}, "name = ?", new String[]{feiertagName}, null, null, null);
                    boolean osterfeiertagOk = true;
                    if (osterCheck.moveToFirst()) {
                        int offset = osterCheck.getInt(0);
                        osterCheck.close();
                        java.util.Calendar ostern = berechneOsterdatum(jahr);
                        aufMitternacht(ostern);
                        java.util.Calendar korrekt = java.util.Calendar.getInstance();
                        korrekt.setTimeInMillis(ostern.getTimeInMillis());
                        korrekt.add(java.util.Calendar.DAY_OF_MONTH, offset);
                        aufMitternacht(korrekt);
                        if (korrekt.get(java.util.Calendar.DAY_OF_MONTH) != tag || korrekt.get(java.util.Calendar.MONTH) != monat) {
                            osterfeiertagOk = false; // Sonderfeiertage-Eintrag ist falsch (z. B. 17.02 statt 18.02 für Aschermittwoch)
                        }
                    } else {
                        osterCheck.close();
                    }
                    if (osterfeiertagOk) {
                        // Vortag eines Osterfeiertags (z. B. 17.02 vor Aschermittwoch) in der Programmabfrage nicht als Feiertag anzeigen
                        if ((feiertagName != null && (feiertagName.equals("Vorfeiertag") || feiertagName.startsWith("Tag vor ") || feiertagName.startsWith("vor ")))
                            && istVortagOsterfeiertag(tag, monat, jahr)) {
                            Log.d(TAG, "[Feiertag] Sonderfeiertage: Vortag -> null");
                            return null;
                        }
                        Log.d(TAG, "[Feiertag] return aus Sonderfeiertage: " + feiertagName);
                        return new FeiertagsInfo(feiertagName, datumVoll);
                    }
                } else {
                    feiertagCursor.close();
                }
            }
        } finally {
            sonderCursor.close();
        }
        
        // 2. Prüfe normale feste Feiertage (ohne Verschiebung)
        Cursor cursor = db.query(TABLE_FESTE_FEIERTAGE,
            new String[]{"name", "datum"},
            "(datum = ? OR datum = ?) AND (verschieben_auf_sonntag = 0 OR verschieben_auf_sonntag IS NULL)",
            new String[]{datumStr, datumStr2},
            null, null, null);
        
        try {
            if (cursor.moveToFirst()) {
                String feiertagName = cursor.getString(0);
                String datum = cursor.getString(1);
                // Wenn der Name ein Osterfeiertag ist: nur übernehmen, wenn das angefragte Datum dem berechneten Osterdatum entspricht (vermeidet falsches festes Datum z. B. 17.02 für Aschermittwoch).
                Cursor osterFix = db.query(TABLE_OSTERFEIERTAGE, new String[]{"oster_offset"}, "name = ?", new String[]{feiertagName}, null, null, null);
                if (osterFix.moveToFirst()) {
                    int offset = osterFix.getInt(0);
                    osterFix.close();
                    java.util.Calendar ostern = berechneOsterdatum(jahr);
                    aufMitternacht(ostern);
                    java.util.Calendar korrekt = java.util.Calendar.getInstance();
                    korrekt.setTimeInMillis(ostern.getTimeInMillis());
                    korrekt.add(java.util.Calendar.DAY_OF_MONTH, offset);
                    aufMitternacht(korrekt);
                    if (korrekt.get(java.util.Calendar.DAY_OF_MONTH) == tag && korrekt.get(java.util.Calendar.MONTH) == monat) {
                        return new FeiertagsInfo(feiertagName, datumVoll);
                    }
                    // Datum passt nicht (z. B. feste_feiertage hat 17.02, berechnet ist 18.02) – nicht zurückgeben, Schritt 3 nutzen
                } else {
                    osterFix.close();
                    // Vortag eines Osterfeiertags nicht als Feiertag anzeigen
                    if ((feiertagName != null && (feiertagName.equals("Vorfeiertag") || feiertagName.startsWith("Tag vor ") || feiertagName.startsWith("vor ")))
                        && istVortagOsterfeiertag(tag, monat, jahr)) {
                        return null;
                    }
                    return new FeiertagsInfo(feiertagName, datumVoll);
                }
            }
        } finally {
            cursor.close();
        }
        
        // 3. Prüfe Osterfeiertage (zuerst weihnachtsbezogene per Datum, dann Oster-Offset)
        Cursor alleOsterInfo = db.query(TABLE_OSTERFEIERTAGE, new String[]{"name"}, null, null, null, null, null);
        try {
            while (alleOsterInfo.moveToNext()) {
                String name = alleOsterInfo.isNull(0) ? null : alleOsterInfo.getString(0);
                if (name != null && (name.contains("Christkönig") || name.contains("Advent"))) {
                    int[] datum = berechneWeihnachtsbezogenerFeiertagDatum(name, jahr);
                    if (datum != null && datum[0] == tag && datum[1] == monat)
                        return new FeiertagsInfo(name, datumVoll);
                }
            }
        } finally {
            alleOsterInfo.close();
        }

        java.util.Calendar ostern = berechneOsterdatum(jahr);
        aufMitternacht(ostern);
        java.util.Calendar aktuellesDatum = java.util.Calendar.getInstance();
        aktuellesDatum.set(jahr, monat, tag);
        aufMitternacht(aktuellesDatum);
        long diffMillis = aktuellesDatum.getTimeInMillis() - ostern.getTimeInMillis();
        int diffTage = (int) (diffMillis / (1000 * 60 * 60 * 24));

        Cursor osterCursor = db.query(TABLE_OSTERFEIERTAGE,
            new String[]{"name"},
            "oster_offset = ?",
            new String[]{String.valueOf(diffTage)},
            null, null, null);

        try {
            if (osterCursor.moveToFirst()) {
                String feiertagName = osterCursor.getString(0);
                Log.d(TAG, "[Feiertag] return aus Oster-Offset (diffTage=" + diffTage + "): " + feiertagName);
                return new FeiertagsInfo(feiertagName, datumVoll);
            }
        } finally {
            osterCursor.close();
        }

        Log.d(TAG, "[Feiertag] kein Feiertag gefunden -> null");
        return null;
    }
    
    /**
     * Entity-Klasse für MIDI-Konfiguration.
     */
    public static class MidiConfig {
        public int id;
        public String dateiname;
        public double abspielgeschwindigkeit;  // Multiplikator (1.0 = normal, 0.5 = halb so schnell, 2.0 = doppelt so schnell)
        public boolean aktiv;
    }
    
    /**
     * Entity-Klasse für MIDI-Note-zu-Relais-Zuordnung (GLOBAL).
     */
    public static class MidiNoteRelais {
        public int id;
        public int note;  // MIDI-Note (0-127)
        public int relaisNummer;
        public int impulsLaengeMs;  // Impuls-Länge in Millisekunden für diese Note
        public boolean aktiv;
    }
    
    /**
     * Lädt alle MIDI-Konfigurationen aus der Datenbank.
     */
    public List<MidiConfig> getAllMidiConfigs() {
        List<MidiConfig> configs = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        
        Cursor cursor = db.query(TABLE_MIDI_CONFIG,
            new String[]{"id", "dateiname", "abspielgeschwindigkeit", "aktiv"},
            null, null, null, null, "dateiname ASC");
        
        try {
            while (cursor.moveToNext()) {
                MidiConfig config = new MidiConfig();
                config.id = cursor.getInt(0);
                config.dateiname = cursor.getString(1);
                config.abspielgeschwindigkeit = cursor.getDouble(2);
                config.aktiv = cursor.getInt(3) == 1;
                configs.add(config);
            }
        } finally {
            cursor.close();
        }
        
        return configs;
    }
    
    /**
     * Lädt eine MIDI-Konfiguration nach Dateiname.
     */
    public MidiConfig getMidiConfig(String dateiname) {
        SQLiteDatabase db = getReadableDatabase();
        
        Cursor cursor = db.query(TABLE_MIDI_CONFIG,
            new String[]{"id", "dateiname", "abspielgeschwindigkeit", "aktiv"},
            "dateiname = ?",
            new String[]{dateiname},
            null, null, null);
        
        try {
            if (cursor.moveToFirst()) {
                MidiConfig config = new MidiConfig();
                config.id = cursor.getInt(0);
                config.dateiname = cursor.getString(1);
                config.abspielgeschwindigkeit = cursor.getDouble(2);
                config.aktiv = cursor.getInt(3) == 1;
                return config;
            }
        } finally {
            cursor.close();
        }
        
        return null;
    }
    
    /**
     * Speichert eine MIDI-Konfiguration (INSERT oder UPDATE).
     */
    public MidiConfig saveMidiConfig(MidiConfig config) {
        SQLiteDatabase db = getWritableDatabase();
        
        android.content.ContentValues values = new android.content.ContentValues();
        values.put("dateiname", config.dateiname);
        values.put("abspielgeschwindigkeit", config.abspielgeschwindigkeit);
        values.put("aktiv", config.aktiv ? 1 : 0);
        values.put("updated_at", "CURRENT_TIMESTAMP");
        
        if (config.id > 0) {
            // UPDATE
            db.update(TABLE_MIDI_CONFIG, values, "id = ?", new String[]{String.valueOf(config.id)});
            Log.d(TAG, "MIDI-Konfiguration aktualisiert: " + config.dateiname);
        } else {
            // INSERT
            long id = db.insert(TABLE_MIDI_CONFIG, null, values);
            config.id = (int) id;
            Log.d(TAG, "MIDI-Konfiguration erstellt: " + config.dateiname + " (ID: " + id + ")");
        }
        
        return config;
    }
    
    /**
     * Löscht eine MIDI-Konfiguration.
     */
    public void deleteMidiConfig(int id) {
        SQLiteDatabase db = getWritableDatabase();
        db.delete(TABLE_MIDI_CONFIG, "id = ?", new String[]{String.valueOf(id)});
        Log.d(TAG, "MIDI-Konfiguration gelöscht: ID " + id);
    }
    
    /**
     * Lädt alle Noten-zu-Relais-Zuordnungen (GLOBAL).
     */
    public List<MidiNoteRelais> getAllMidiNoteRelais() {
        List<MidiNoteRelais> mappings = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        
        Cursor cursor = db.query(TABLE_MIDI_NOTEN_RELAIS,
            new String[]{"id", "note", "relais_nummer", "impuls_laenge_ms", "aktiv"},
            null, null, null, null, "note ASC");
        
        try {
            while (cursor.moveToNext()) {
                MidiNoteRelais mapping = new MidiNoteRelais();
                mapping.id = cursor.getInt(0);
                mapping.note = cursor.getInt(1);
                mapping.relaisNummer = cursor.getInt(2);
                mapping.impulsLaengeMs = cursor.getInt(3);
                mapping.aktiv = cursor.getInt(4) == 1;
                mappings.add(mapping);
            }
        } finally {
            cursor.close();
        }
        
        return mappings;
    }
    
    /**
     * Lädt Noten-zu-Relais-Zuordnung für eine spezifische Note.
     */
    public MidiNoteRelais getMidiNoteRelais(int note) {
        SQLiteDatabase db = getReadableDatabase();
        
        Cursor cursor = db.query(TABLE_MIDI_NOTEN_RELAIS,
            new String[]{"id", "note", "relais_nummer", "impuls_laenge_ms", "aktiv"},
            "note = ?",
            new String[]{String.valueOf(note)},
            null, null, null);
        
        try {
            if (cursor.moveToFirst()) {
                MidiNoteRelais mapping = new MidiNoteRelais();
                mapping.id = cursor.getInt(0);
                mapping.note = cursor.getInt(1);
                mapping.relaisNummer = cursor.getInt(2);
                mapping.impulsLaengeMs = cursor.getInt(3);
                mapping.aktiv = cursor.getInt(4) == 1;
                return mapping;
            }
        } finally {
            cursor.close();
        }
        
        return null;
    }
    
    /**
     * Speichert oder aktualisiert eine Noten-zu-Relais-Zuordnung (GLOBAL).
     */
    public MidiNoteRelais saveMidiNoteRelais(MidiNoteRelais mapping) {
        SQLiteDatabase db = getWritableDatabase();
        
        android.content.ContentValues values = new android.content.ContentValues();
        values.put("note", mapping.note);
        values.put("relais_nummer", mapping.relaisNummer);
        values.put("impuls_laenge_ms", mapping.impulsLaengeMs);
        values.put("aktiv", mapping.aktiv ? 1 : 0);
        values.put("updated_at", "CURRENT_TIMESTAMP");
        
        if (mapping.id > 0) {
            // UPDATE
            db.update(TABLE_MIDI_NOTEN_RELAIS, values, "id = ?", new String[]{String.valueOf(mapping.id)});
            Log.d(TAG, "MIDI-Note-Relais-Zuordnung aktualisiert: Note " + mapping.note + " -> Relais " + mapping.relaisNummer);
        } else {
            // INSERT (oder UPDATE falls Note bereits existiert)
            // Prüfe ob Note bereits existiert
            MidiNoteRelais existing = getMidiNoteRelais(mapping.note);
            if (existing != null) {
                // UPDATE bestehenden Eintrag
                values.put("id", existing.id);
                db.update(TABLE_MIDI_NOTEN_RELAIS, values, "note = ?", new String[]{String.valueOf(mapping.note)});
                mapping.id = existing.id;
                Log.d(TAG, "MIDI-Note-Relais-Zuordnung aktualisiert: Note " + mapping.note + " -> Relais " + mapping.relaisNummer);
            } else {
                // INSERT
                long id = db.insert(TABLE_MIDI_NOTEN_RELAIS, null, values);
                mapping.id = (int) id;
                Log.d(TAG, "MIDI-Note-Relais-Zuordnung erstellt: Note " + mapping.note + " -> Relais " + mapping.relaisNummer + " (ID: " + id + ")");
            }
        }
        
        return mapping;
    }
    
    /**
     * Aktualisiert eine Noten-zu-Relais-Zuordnung für eine spezifische Note.
     */
    public void updateMidiNoteRelais(int note, int relaisNummer, int impulsLaengeMs) {
        SQLiteDatabase db = getWritableDatabase();
        
        android.content.ContentValues values = new android.content.ContentValues();
        values.put("relais_nummer", relaisNummer);
        values.put("impuls_laenge_ms", impulsLaengeMs);
        values.put("updated_at", "CURRENT_TIMESTAMP");
        
        int rowsUpdated = db.update(TABLE_MIDI_NOTEN_RELAIS, values, "note = ?", new String[]{String.valueOf(note)});
        
        if (rowsUpdated == 0) {
            // Eintrag existiert nicht, erstelle neuen
            values.put("note", note);
            values.put("aktiv", 1);
            db.insert(TABLE_MIDI_NOTEN_RELAIS, null, values);
            Log.d(TAG, "MIDI-Note-Relais-Zuordnung erstellt: Note " + note + " -> Relais " + relaisNummer + ", Impuls: " + impulsLaengeMs + "ms");
        } else {
            Log.d(TAG, "MIDI-Note-Relais-Zuordnung aktualisiert: Note " + note + " -> Relais " + relaisNummer + ", Impuls: " + impulsLaengeMs + "ms");
        }
    }
    
    /**
     * Löscht eine Noten-zu-Relais-Zuordnung.
     */
    public void deleteMidiNoteRelais(int note) {
        SQLiteDatabase db = getWritableDatabase();
        db.delete(TABLE_MIDI_NOTEN_RELAIS, "note = ?", new String[]{String.valueOf(note)});
        Log.d(TAG, "MIDI-Note-Relais-Zuordnung gelöscht: Note " + note);
    }
    
    // ========== MELODIEN Entity-Klassen und CRUD-Methoden ==========
    
    /**
     * Entity-Klasse für Melodien.
     */
    public static class Melodie {
        public int id;
        public String name;
        public int vorlaufMinuten;
        public long createdAt;
        public long updatedAt;
    }
    
    /**
     * Entity-Klasse für Melodie-Zeilen.
     */
    public static class MelodieZeile {
        public int id;
        public int melodieId;
        public int zeileIndex;
        public String beginnZeit;  // "HH:MM:SS" Format
        public Double dauerSekunden;  // Dauer in Sekunden
        public String zeiteinheit;  // "Sec" oder "Min"
        public String kloeppelA;
        public String kloeppelB;
        public String kloeppelC;
        public String kloeppelD;
        public String kloeppelE;
        public String kloeppelF;
        public String kloeppelG;
        public String kloeppelH;
        public String kloeppelI;
        public String kloeppelJ;
        public String kloeppelK;
        public String kloeppelL;
        public String kloeppelM;
        public String kloeppelN;
        public String kloeppelO;
        public String kloeppelP;
        public String soundMp3;
        public String melodieXls;
    }
    
    /**
     * Lädt alle Melodien aus der Datenbank.
     */
    public List<Melodie> getAllMelodien() {
        List<Melodie> melodien = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        
        Cursor cursor = db.query(TABLE_MELODIEN, null, null, null, null, null, "name ASC");
        
        while (cursor.moveToNext()) {
            Melodie melodie = new Melodie();
            melodie.id = cursor.getInt(cursor.getColumnIndexOrThrow("id"));
            melodie.name = cursor.getString(cursor.getColumnIndexOrThrow("name"));
            melodie.vorlaufMinuten = cursor.getInt(cursor.getColumnIndexOrThrow("vorlauf_minuten"));
            melodie.createdAt = cursor.getLong(cursor.getColumnIndexOrThrow("created_at"));
            melodie.updatedAt = cursor.getLong(cursor.getColumnIndexOrThrow("updated_at"));
            melodien.add(melodie);
        }
        
        cursor.close();
        return melodien;
    }
    
    /**
     * Lädt eine Melodie nach Name.
     */
    public Melodie getMelodie(String name) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.query(TABLE_MELODIEN, null, "name = ?", new String[]{name}, null, null, null);
        
        Melodie melodie = null;
        if (cursor.moveToFirst()) {
            melodie = new Melodie();
            melodie.id = cursor.getInt(cursor.getColumnIndexOrThrow("id"));
            melodie.name = cursor.getString(cursor.getColumnIndexOrThrow("name"));
            melodie.vorlaufMinuten = cursor.getInt(cursor.getColumnIndexOrThrow("vorlauf_minuten"));
            melodie.createdAt = cursor.getLong(cursor.getColumnIndexOrThrow("created_at"));
            melodie.updatedAt = cursor.getLong(cursor.getColumnIndexOrThrow("updated_at"));
        }
        
        cursor.close();
        return melodie;
    }

    /**
     * Normalisiert einen Melodienamen für Vergleich (trim, mehrere Leerzeichen → eines).
     */
    private static String normalizeMelodieName(String name) {
        if (name == null) return "";
        return name.trim().replaceAll("\\s+", " ");
    }

    /**
     * Ersetzt typische UTF-8-Mojibake (falsch als Latin-1 gelesen), damit Pfad- und DB-Namen vergleichbar sind.
     */
    private static String fixMojibakeForCompare(String name) {
        if (name == null) return "";
        String s = name;
        s = s.replace("Ã¤", "ä").replace("Ã„", "Ä");
        s = s.replace("Ã¶", "ö").replace("Ã–", "Ö");
        s = s.replace("Ã¼", "ü").replace("Ãœ", "Ü");
        s = s.replace("ÃŸ", "ß");
        return s;
    }

    /**
     * Lädt eine Melodie nach Name; versucht exakte Übereinstimmung, danach normalisierten Namen
     * (trim, mehrere Leerzeichen als eines), damit z. B. "F4 Zusammenläuten 1-7   2 Min" die DB-Melodie findet.
     * Berücksichtigt auch typische Kodierungsunterschiede (z. B. Ã¤ vs ä).
     */
    public Melodie getMelodieByNormalizedName(String name) {
        Melodie melodie = getMelodie(name);
        if (melodie != null) return melodie;
        String normalized = normalizeMelodieName(fixMojibakeForCompare(name));
        if (normalized.isEmpty()) return null;
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.query(TABLE_MELODIEN, null, null, null, null, null, null);
        Melodie found = null;
        if (cursor.moveToFirst()) {
            do {
                String dbName = cursor.getString(cursor.getColumnIndexOrThrow("name"));
                String dbNormalized = normalizeMelodieName(fixMojibakeForCompare(dbName));
                if (normalized.equals(dbNormalized)) {
                    found = new Melodie();
                    found.id = cursor.getInt(cursor.getColumnIndexOrThrow("id"));
                    found.name = cursor.getString(cursor.getColumnIndexOrThrow("name"));
                    found.vorlaufMinuten = cursor.getInt(cursor.getColumnIndexOrThrow("vorlauf_minuten"));
                    found.createdAt = cursor.getLong(cursor.getColumnIndexOrThrow("created_at"));
                    found.updatedAt = cursor.getLong(cursor.getColumnIndexOrThrow("updated_at"));
                    break;
                }
            } while (cursor.moveToNext());
        }
        cursor.close();
        return found;
    }
    
    /**
     * Lädt eine Melodie nach ID.
     */
    public Melodie getMelodie(int id) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.query(TABLE_MELODIEN, null, "id = ?", new String[]{String.valueOf(id)}, null, null, null);
        
        Melodie melodie = null;
        if (cursor.moveToFirst()) {
            melodie = new Melodie();
            melodie.id = cursor.getInt(cursor.getColumnIndexOrThrow("id"));
            melodie.name = cursor.getString(cursor.getColumnIndexOrThrow("name"));
            melodie.vorlaufMinuten = cursor.getInt(cursor.getColumnIndexOrThrow("vorlauf_minuten"));
            melodie.createdAt = cursor.getLong(cursor.getColumnIndexOrThrow("created_at"));
            melodie.updatedAt = cursor.getLong(cursor.getColumnIndexOrThrow("updated_at"));
        }
        
        cursor.close();
        return melodie;
    }
    
    /**
     * Speichert oder aktualisiert eine Melodie.
     */
    public Melodie saveMelodie(Melodie melodie) {
        SQLiteDatabase db = getWritableDatabase();
        android.content.ContentValues values = new android.content.ContentValues();
        values.put("name", melodie.name);
        values.put("vorlauf_minuten", melodie.vorlaufMinuten);
        values.put("updated_at", System.currentTimeMillis());
        
        if (melodie.id > 0) {
            // UPDATE
            db.update(TABLE_MELODIEN, values, "id = ?", new String[]{String.valueOf(melodie.id)});
            Log.d(TAG, "Melodie aktualisiert: " + melodie.name);
        } else {
            // INSERT
            values.put("created_at", System.currentTimeMillis());
            long id = db.insert(TABLE_MELODIEN, null, values);
            melodie.id = (int) id;
            melodie.createdAt = System.currentTimeMillis();
            melodie.updatedAt = System.currentTimeMillis();
            Log.d(TAG, "Melodie erstellt: " + melodie.name + " (ID: " + id + ")");
        }
        
        return melodie;
    }
    
    /**
     * Löscht eine Melodie (CASCADE löscht auch alle Zeilen).
     */
    public void deleteMelodie(int id) {
        SQLiteDatabase db = getWritableDatabase();
        db.delete(TABLE_MELODIEN, "id = ?", new String[]{String.valueOf(id)});
        Log.d(TAG, "Melodie gelöscht: ID " + id);
    }
    
    /**
     * Lädt alle Zeilen einer Melodie.
     */
    public List<MelodieZeile> getMelodieZeilen(int melodieId) {
        List<MelodieZeile> zeilen = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        
        Cursor cursor = db.query(TABLE_MELODIE_ZEILEN, null, "melodie_id = ?", 
            new String[]{String.valueOf(melodieId)}, null, null, "zeile_index ASC");
        
        while (cursor.moveToNext()) {
            MelodieZeile zeile = new MelodieZeile();
            zeile.id = cursor.getInt(cursor.getColumnIndexOrThrow("id"));
            zeile.melodieId = cursor.getInt(cursor.getColumnIndexOrThrow("melodie_id"));
            zeile.zeileIndex = cursor.getInt(cursor.getColumnIndexOrThrow("zeile_index"));
            zeile.beginnZeit = cursor.getString(cursor.getColumnIndexOrThrow("beginn_zeit"));
            int dauerIndex = cursor.getColumnIndexOrThrow("dauer_sekunden");
            zeile.dauerSekunden = cursor.isNull(dauerIndex) ? null : cursor.getDouble(dauerIndex);
            zeile.zeiteinheit = cursor.getString(cursor.getColumnIndexOrThrow("zeiteinheit"));
            zeile.kloeppelA = cursor.getString(cursor.getColumnIndexOrThrow("kloeppel_a"));
            zeile.kloeppelB = cursor.getString(cursor.getColumnIndexOrThrow("kloeppel_b"));
            zeile.kloeppelC = cursor.getString(cursor.getColumnIndexOrThrow("kloeppel_c"));
            zeile.kloeppelD = cursor.getString(cursor.getColumnIndexOrThrow("kloeppel_d"));
            zeile.kloeppelE = cursor.getString(cursor.getColumnIndexOrThrow("kloeppel_e"));
            zeile.kloeppelF = cursor.getString(cursor.getColumnIndexOrThrow("kloeppel_f"));
            zeile.kloeppelG = cursor.getString(cursor.getColumnIndexOrThrow("kloeppel_g"));
            zeile.kloeppelH = cursor.getString(cursor.getColumnIndexOrThrow("kloeppel_h"));
            zeile.kloeppelI = cursor.getString(cursor.getColumnIndexOrThrow("kloeppel_i"));
            zeile.kloeppelJ = cursor.getString(cursor.getColumnIndexOrThrow("kloeppel_j"));
            zeile.kloeppelK = cursor.getString(cursor.getColumnIndexOrThrow("kloeppel_k"));
            zeile.kloeppelL = cursor.getString(cursor.getColumnIndexOrThrow("kloeppel_l"));
            zeile.kloeppelM = cursor.getString(cursor.getColumnIndexOrThrow("kloeppel_m"));
            zeile.kloeppelN = cursor.getString(cursor.getColumnIndexOrThrow("kloeppel_n"));
            zeile.kloeppelO = cursor.getString(cursor.getColumnIndexOrThrow("kloeppel_o"));
            zeile.kloeppelP = cursor.getString(cursor.getColumnIndexOrThrow("kloeppel_p"));
            zeile.soundMp3 = cursor.getString(cursor.getColumnIndexOrThrow("sound_mp3"));
            zeile.melodieXls = cursor.getString(cursor.getColumnIndexOrThrow("melodie_xls"));
            zeilen.add(zeile);
        }
        
        cursor.close();
        return zeilen;
    }
    
    /**
     * Speichert oder aktualisiert eine Melodie-Zeile.
     */
    public MelodieZeile saveMelodieZeile(MelodieZeile zeile) {
        SQLiteDatabase db = getWritableDatabase();
        android.content.ContentValues values = new android.content.ContentValues();
        values.put("melodie_id", zeile.melodieId);
        values.put("zeile_index", zeile.zeileIndex);
        values.put("beginn_zeit", zeile.beginnZeit);
        values.put("dauer_sekunden", zeile.dauerSekunden);
        values.put("zeiteinheit", zeile.zeiteinheit);
        values.put("kloeppel_a", zeile.kloeppelA);
        values.put("kloeppel_b", zeile.kloeppelB);
        values.put("kloeppel_c", zeile.kloeppelC);
        values.put("kloeppel_d", zeile.kloeppelD);
        values.put("kloeppel_e", zeile.kloeppelE);
        values.put("kloeppel_f", zeile.kloeppelF);
        values.put("kloeppel_g", zeile.kloeppelG);
        values.put("kloeppel_h", zeile.kloeppelH);
        values.put("kloeppel_i", zeile.kloeppelI);
        values.put("kloeppel_j", zeile.kloeppelJ);
        values.put("kloeppel_k", zeile.kloeppelK);
        values.put("kloeppel_l", zeile.kloeppelL);
        values.put("kloeppel_m", zeile.kloeppelM);
        values.put("kloeppel_n", zeile.kloeppelN);
        values.put("kloeppel_o", zeile.kloeppelO);
        values.put("kloeppel_p", zeile.kloeppelP);
        values.put("sound_mp3", zeile.soundMp3);
        values.put("melodie_xls", zeile.melodieXls);
        
        if (zeile.id > 0) {
            // UPDATE
            db.update(TABLE_MELODIE_ZEILEN, values, "id = ?", new String[]{String.valueOf(zeile.id)});
            Log.d(TAG, "Melodie-Zeile aktualisiert: ID " + zeile.id);
        } else {
            // INSERT
            long id = db.insert(TABLE_MELODIE_ZEILEN, null, values);
            zeile.id = (int) id;
            Log.d(TAG, "Melodie-Zeile erstellt: ID " + id);
        }
        
        return zeile;
    }
    
    /**
     * Löscht eine Melodie-Zeile.
     */
    public void deleteMelodieZeile(int id) {
        SQLiteDatabase db = getWritableDatabase();
        db.delete(TABLE_MELODIE_ZEILEN, "id = ?", new String[]{String.valueOf(id)});
        Log.d(TAG, "Melodie-Zeile gelöscht: ID " + id);
    }
    
    /**
     * Löscht alle Zeilen einer Melodie.
     */
    public void deleteAllMelodieZeilen(int melodieId) {
        SQLiteDatabase db = getWritableDatabase();
        db.delete(TABLE_MELODIE_ZEILEN, "melodie_id = ?", new String[]{String.valueOf(melodieId)});
        Log.d(TAG, "Alle Melodie-Zeilen gelöscht für Melodie ID " + melodieId);
    }
    
    /**
     * Importiert eine Melodie aus einer Excel-Datei.
     * @param excelFilePath Vollständiger Pfad zur Excel-Datei
     * @return Die importierte Melodie oder null bei Fehler
     */
    public Melodie importMelodieFromExcel(String excelFilePath) {
        try {
            java.io.File excelFile = new java.io.File(excelFilePath);
            if (!excelFile.exists()) {
                Log.e(TAG, "Excel-Datei nicht gefunden: " + excelFilePath);
                return null;
            }
            
            // Extrahiere Melodie-Name aus Dateiname (ohne .xls)
            String fileName = excelFile.getName();
            String melodieName = fileName;
            if (melodieName.toLowerCase().endsWith(".xls")) {
                melodieName = melodieName.substring(0, melodieName.length() - 4);
            }
            
            ExcelRead excelread = new ExcelRead();
            excelread.openXls(excelFilePath);
            
            // Erstelle oder lade Melodie
            Melodie melodie = getMelodie(melodieName);
            if (melodie == null) {
                melodie = new Melodie();
                melodie.name = melodieName;
                melodie.vorlaufMinuten = 0;
            }
            
            // Lese Vorlauf-Minuten aus Spalte B, Zeile 1 (Index 1, 1)
            try {
                String vorlaufStr = excelread.getCellString(1, 1);
                if (vorlaufStr != null && !vorlaufStr.trim().isEmpty()) {
                    melodie.vorlaufMinuten = Integer.parseInt(vorlaufStr.trim());
                }
            } catch (Exception e) {
                Log.w(TAG, "Fehler beim Lesen des Vorlaufs, verwende 0", e);
            }
            
            // Speichere Melodie
            melodie = saveMelodie(melodie);
            
            // Lösche alte Zeilen
            deleteAllMelodieZeilen(melodie.id);
            
            // Finde erste Datenzeile (ab Index 1, suche nach Wartezeit in Spalte 3)
            int startZeile = 1;
            int maxZeilen = excelread.getCellZeilen();
            
            while (startZeile < maxZeilen) {
                String testWaitString = excelread.getCellString(3, startZeile);
                if (testWaitString != null && !testWaitString.trim().isEmpty() 
                        && !testWaitString.equals("null") && !testWaitString.equals("0")
                        && !testWaitString.equals("Zeit 100/sec")) {
                    break;
                }
                startZeile++;
            }
            
            if (startZeile >= maxZeilen) {
                Log.w(TAG, "Keine Datenzeilen gefunden in " + excelFilePath);
                excelread.closeWorkbook();
                return melodie;
            }
            
            // Importiere Zeilen
            // WICHTIG: Erste Zeile beginnt immer bei 00:00:00
            double aktuelleBeginnSekunden = 0.0;
            int zeileIndex = 0;
            
            for (int zeile = startZeile; zeile < maxZeilen; zeile++) {
                String waitString = excelread.getCellString(3, zeile);
                
                // Prüfe, ob Wartezeit vorhanden ist (leer, "null" oder "0" bedeutet Ende)
                if (waitString == null || waitString.trim().isEmpty() 
                        || waitString.equals("null") || waitString.equals("0")) {
                    break;
                }
                
                MelodieZeile melodieZeile = new MelodieZeile();
                melodieZeile.melodieId = melodie.id;
                melodieZeile.zeileIndex = zeileIndex;
                
                // Berechne Beginn-Zeit
                // WICHTIG: Erste Zeile (zeileIndex 0) beginnt immer bei 00:00.00
                if (zeileIndex == 0) {
                    aktuelleBeginnSekunden = 0.0;
                }
                melodieZeile.beginnZeit = formatZeit(aktuelleBeginnSekunden);
                
                // Konvertiere Wartezeit von Hundertstel-Sekunden zu Sekunden
                try {
                    int waitHundertstel = Integer.parseInt(waitString.trim());
                    melodieZeile.dauerSekunden = waitHundertstel / 100.0;
                    melodieZeile.zeiteinheit = "Sec";  // Beim Import immer "Sec"
                } catch (NumberFormatException e) {
                    Log.w(TAG, "Fehler beim Parsen der Wartezeit in Zeile " + zeile + ": " + waitString);
                    melodieZeile.dauerSekunden = 0.0;
                    melodieZeile.zeiteinheit = "Sec";
                }
                
                // Prüfe verfügbare Spalten
                // getCellSpalten() gibt die Anzahl zurück: bei 14 Spalten sind Indizes 0-13
                // Also: Index 14 existiert nur wenn maxSpalten >= 15 (also > 14)
                int maxSpalten = excelread.getCellSpalten();
                
                // Lese Kloeppel-Relais (Spalte 4-19) - nur wenn Spalte existiert
                // Spalte 4 existiert wenn maxSpalten >= 5 (also > 4)
                melodieZeile.kloeppelA = (maxSpalten > 4) ? safeGetCellString(excelread, 4, zeile) : null;
                melodieZeile.kloeppelB = (maxSpalten > 5) ? safeGetCellString(excelread, 5, zeile) : null;
                melodieZeile.kloeppelC = (maxSpalten > 6) ? safeGetCellString(excelread, 6, zeile) : null;
                melodieZeile.kloeppelD = (maxSpalten > 7) ? safeGetCellString(excelread, 7, zeile) : null;
                melodieZeile.kloeppelE = (maxSpalten > 8) ? safeGetCellString(excelread, 8, zeile) : null;
                melodieZeile.kloeppelF = (maxSpalten > 9) ? safeGetCellString(excelread, 9, zeile) : null;
                melodieZeile.kloeppelG = (maxSpalten > 10) ? safeGetCellString(excelread, 10, zeile) : null;
                melodieZeile.kloeppelH = (maxSpalten > 11) ? safeGetCellString(excelread, 11, zeile) : null;
                melodieZeile.kloeppelI = (maxSpalten > 12) ? safeGetCellString(excelread, 12, zeile) : null;
                melodieZeile.kloeppelJ = (maxSpalten > 13) ? safeGetCellString(excelread, 13, zeile) : null;
                melodieZeile.kloeppelK = (maxSpalten > 14) ? safeGetCellString(excelread, 14, zeile) : null;
                melodieZeile.kloeppelL = (maxSpalten > 15) ? safeGetCellString(excelread, 15, zeile) : null;
                melodieZeile.kloeppelM = (maxSpalten > 16) ? safeGetCellString(excelread, 16, zeile) : null;
                melodieZeile.kloeppelN = (maxSpalten > 17) ? safeGetCellString(excelread, 17, zeile) : null;
                melodieZeile.kloeppelO = (maxSpalten > 18) ? safeGetCellString(excelread, 18, zeile) : null;
                melodieZeile.kloeppelP = (maxSpalten > 19) ? safeGetCellString(excelread, 19, zeile) : null;
                
                // Lese verschachtelte Melodie (Spalte 20)
                melodieZeile.melodieXls = (maxSpalten > 20) ? safeGetCellString(excelread, 20, zeile) : null;
                
                // Lese Sound-MP3 (Spalte 21)
                melodieZeile.soundMp3 = (maxSpalten > 21) ? safeGetCellString(excelread, 21, zeile) : null;
                
                // Speichere Zeile
                saveMelodieZeile(melodieZeile);
                
                // Berechne Beginn für nächste Zeile
                aktuelleBeginnSekunden += melodieZeile.dauerSekunden;
                zeileIndex++;
            }
            
            excelread.closeWorkbook();
            Log.d(TAG, "Melodie importiert: " + melodieName + " (" + zeileIndex + " Zeilen)");
            return melodie;
            
        } catch (jxl.read.biff.BiffException e) {
            Log.e(TAG, "BiffException beim Importieren der Melodie: " + excelFilePath, e);
            return null;
        } catch (java.io.IOException e) {
            Log.e(TAG, "IOException beim Importieren der Melodie: " + excelFilePath, e);
            return null;
        } catch (Exception e) {
            Log.e(TAG, "Fehler beim Importieren der Melodie: " + excelFilePath, e);
            return null;
        }
    }
    
    /**
     * Sicherer Zugriff auf Excel-Zellen mit Exception-Handling.
     * Gibt null zurück, wenn die Zelle nicht existiert oder leer ist.
     */
    private String safeGetCellString(ExcelRead excelread, int spalte, int zeile) {
        try {
            String value = excelread.getCellString(spalte, zeile);
            if (value == null || value.trim().isEmpty() || value.equals("null")) {
                return null;
            }
            return value.trim();
        } catch (ArrayIndexOutOfBoundsException e) {
            // Spalte existiert nicht
            return null;
        } catch (Exception e) {
            // Andere Fehler - loggen aber nicht abbrechen
            Log.w(TAG, "Fehler beim Lesen von Spalte " + spalte + ", Zeile " + zeile + ": " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Formatiert Sekunden als "HH:MM:SS" String.
     */
    private String formatZeit(double sekunden) {
        // Nur Minuten und Sekunden, keine Stunden
        int totalSekunden = (int) sekunden;
        int minuten = totalSekunden / 60;
        double sek = sekunden % 60.0;
        
        return String.format("%02d:%05.2f", minuten, sek);
    }
    
    /** Prüft, ob (tag, monat) der Vortag (Tag davor) eines Osterfeiertags in diesem Jahr ist. Dann soll in der Programmabfrage weder Feiertag noch der Feiertags-Programmtag angezeigt werden. Liefert false, wenn das Datum selbst ein Osterfeiertag ist (z.&nbsp;B. 04.04 = Karsamstag, 05.04 = Ostersonntag). */
    public boolean istVortagOsterfeiertag(int tag, int monat, int jahr) {
        java.util.Calendar ostern = berechneOsterdatum(jahr);
        aufMitternacht(ostern);
        Cursor c = getReadableDatabase().query(TABLE_OSTERFEIERTAGE, new String[]{"oster_offset", "name"}, null, null, null, null, null);
        try {
            while (c.moveToNext()) {
                int offset = c.getInt(0);
                String name = c.isNull(1) ? "?" : c.getString(1);
                java.util.Calendar feiertagDatum = java.util.Calendar.getInstance();
                feiertagDatum.setTimeInMillis(ostern.getTimeInMillis());
                feiertagDatum.add(java.util.Calendar.DAY_OF_MONTH, offset);
                aufMitternacht(feiertagDatum);
                int fdTag = feiertagDatum.get(java.util.Calendar.DAY_OF_MONTH);
                int fdMonat = feiertagDatum.get(java.util.Calendar.MONTH);
                if (fdTag == tag && fdMonat == monat) {
                    Log.d(TAG, "[Vortag] istVortagOsterfeiertag: NEIN – " + tag + "." + (monat + 1) + "." + jahr + " ist selbst Osterfeiertag (" + name + ")");
                    return false;
                }
            }
            c.moveToPosition(-1);
            while (c.moveToNext()) {
                int offset = c.getInt(0);
                String name = c.isNull(1) ? "?" : c.getString(1);
                java.util.Calendar feiertag = java.util.Calendar.getInstance();
                feiertag.setTimeInMillis(ostern.getTimeInMillis());
                feiertag.add(java.util.Calendar.DAY_OF_MONTH, offset);
                feiertag.add(java.util.Calendar.DAY_OF_MONTH, -1);
                aufMitternacht(feiertag);
                int vTag = feiertag.get(java.util.Calendar.DAY_OF_MONTH);
                int vMonat = feiertag.get(java.util.Calendar.MONTH);
                if (vTag == tag && vMonat == monat) {
                    Log.d(TAG, "[Vortag] istVortagOsterfeiertag: JA – " + tag + "." + (monat + 1) + "." + jahr + " ist Vortag von " + name + " (Offset " + offset + ")");
                    return true;
                }
            }
        } finally {
            c.close();
        }
        Log.d(TAG, "[Vortag] istVortagOsterfeiertag: NEIN für " + tag + "." + (monat + 1) + "." + jahr);
        return false;
    }

    /** Setzt die Uhrzeit eines Calendar auf 00:00:00.000 (für reinen Tagesvergleich). */
    private static void aufMitternacht(java.util.Calendar cal) {
        cal.set(java.util.Calendar.HOUR_OF_DAY, 0);
        cal.set(java.util.Calendar.MINUTE, 0);
        cal.set(java.util.Calendar.SECOND, 0);
        cal.set(java.util.Calendar.MILLISECOND, 0);
    }

    /**
     * Berechnet das Osterdatum für ein gegebenes Jahr (Gauß'sche Osterformel).
     * Gleiche Formel wie in ConfigWebServer, damit Programmabfrage und Osterfeiertage-Seite dasselbe Datum nutzen (z.&nbsp;B. 2026 = 05.04.2026).
     */
    private java.util.Calendar berechneOsterdatum(int jahr) {
        int a = jahr % 19;
        int b = jahr % 4;
        int c = jahr % 7;
        int k = jahr / 100;
        int p = (8 * k + 13) / 25;
        int q = k / 4;
        int m = (15 + k - p - q) % 30;
        int n = (4 + k - q) % 7;
        int d = (19 * a + m) % 30;
        int e = (2 * b + 4 * c + 6 * d + n) % 7;
        int tag = 22 + d + e;
        int monat = 3; // März
        if (tag > 31) {
            tag = tag - 31;
            monat = 4; // April
        }
        if (d == 29 && e == 6) {
            tag = 19;
            monat = 4;
        }
        if (d == 28 && e == 6 && ((11 * m + 11) % 30) < 19) {
            tag = 18;
            monat = 4;
        }
        java.util.Calendar ostern = java.util.Calendar.getInstance();
        ostern.set(jahr, monat - 1, tag);
        return ostern;
    }
}
