package tom.turmtechnik;

import android.bluetooth.BluetoothAdapter;
import android.graphics.drawable.Drawable;
import android.media.SoundPool;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

/**
 * Statische Variablen für die gesamte App.
 * 
 * TODO: Diese Klasse wird schrittweise in thematische Config-Klassen aufgeteilt:
 * - TimeConfig für Zeitserver und Uhren
 * - SoundConfig für Sound
 * - UIState für UI-Zustand
 * - MelodyConfig für Melodien
 * - IOConfig für I/O-Verbindungen
 * - HeatingConfig für Heizung
 * - SchlagwerkConfig für Schlagwerk
 * - SystemConfig für System-Konfiguration
 * 
 * Siehe REFACTORING_PLAN.md für Migrations-Strategie.
 */
public class StaticVariable
{
    // ========== Editor/File Management ==========
    public static String pathAndFilenameEditor;
    public static int pathAndFilenameEditorIndex = 0;
    public static int fileIndexForEditorTemp = -1;

    // ========== Internet/Network ==========
    public static boolean internetOnOffFlag = true;
    public static int internetOnStunden = 0;
    public static int internetOnMinuten = 0;
    public static int internetOffStunden = 0;
    public static int internetOffMinuten = 0;
    public static int changeInternetVerknuepfteTasten = 0;
    /** Wenn true: UhrThread führt sofort Programmabfrage aus (nach Verknüpfte-Taste). */
    public static boolean refreshInfoTextVerknuepfteTaste = false;
    /** Wenn true: Programme in DB wurden geändert – UhrThread sucht beim nächsten Lauf neu. */
    public static boolean programmeDatabaseChanged = false;
    /** Wenn > 0: UI erzwingt sofortige Anzeige-Aktualisierung (wird nach jedem Update dekrementiert). */
    public static int infoTextRefreshCount = 0;
    public static int changeInternetBenutzerprogramme = 0;
    public static boolean internetChanged = false;
    public static String fernwartungServerId;
    public static String fernwartungServerPassword;
    public static String gelesenVonJsonRequest = "";

    // ========== UI/Text ==========
    public static ArrayList<String> uebersetzteTexte;
    public static String infoToastText = "";
    public static int infoTextIndex = 0;
    public static String[] stringInfoTextField = new String[]{"AUTOMATIK AUSGESCHALTET", "", ""};
    public static String infoStringHeizung = "";
    public static boolean layoutReady = false;
    public static boolean screenIsOff = true;
    public static boolean seiteZweiIsRunning = false;
    public static boolean manuelerStartActivityIsRunning = false;
    public static int tastenBreite;
    public static int tastenHoehe;
    public static int buttonsCount = -1;
    public static int buttonsCountError = 0;
    public static boolean makeBeschriftungTasten = false;
    public static Drawable btn_home_drawable;
    public static String btn_home_textstring;
    public static Drawable btn_help_drawable;
    public static String btn_help_textstring;
    public static Drawable btn_automatic_drawable;
    public static String btn_automatic_textstring;
    public static Drawable btn_stop_drawable;
    public static String btn_stop_textstring;

    // ========== Sound ==========
    public static SoundPool soundPool2;
    public static SoundGlocke soundGlocke;
    public static ArrayList<Integer> soundIndexList;
    public static ArrayList<Integer> soundIDsList;
    public static ArrayList<Integer> streamIDsList;
    /** Indizes, für die stop vor dem Ende des asynchronen Ladens aufgerufen wurde – beim Load-Complete sofort stoppen. */
    public static java.util.Set<Integer> stopGlockenSoundPending = java.util.Collections.synchronizedSet(new java.util.HashSet<Integer>());

    // ========== System ==========
    public static String serialNumber;
    public static String versionString;
    public static boolean autostartDerApp = true;
    public static boolean flagRebootOk = false;
    public static int errorCountToReboot = 0;
    public static boolean hammerThreadLaeuft = false;
    public static int hammerDelayTime2 = 0;
    public static int hammerRelais = 0;
    public static int hammerIndex = 0;
    public static int hammerSound = 0;
    public static int excelOpen = 0;
    public static int excelClose = 0;
    public static boolean ddpThreadRun = true;
    // public static boolean sntpThreadRun = true; // Nicht mehr verwendet - TimeSyncThread verwendet doRun-Flag
    //public static boolean nmeaThreadRun = true;
    public static int scanRelaisMS;
    public static int turnOffVerknuepft4 = -1;
    public static int minutenTakt = 0;
    public static int bigClockTimeout = TurmtechnikActivity.SET_BIG_CLOCK_TIME;
    public static boolean sendTageslisteAktiv = false;
    public static int batt_shut_down_level2 = 75; // bei unter 75% ausschalten
    /** Einschalt-Schwelle = Ausschalt + 1 %, damit nach Abschaltung nicht sofort wieder gestartet wird (bei Netz startet die App sofort). */
    public static int getBattEinschaltLevel() { return Math.min(100, batt_shut_down_level2 + 1); }
    public static boolean programmAbfrageAktiv2 = false;
    public static boolean stopBetaetigt = false;
    public static boolean logOnOff;
    /** Logfile Abgelaufene Melodien (Datum, Uhrzeit, Melodiename) – aus Anlagenformular. */
    public static boolean logAbgelaufeneMelodien = false;
    /** Logfile Fehler/Crashes (Gründe der Abstürze) – aus Anlagenformular. */
    public static boolean logFehlerCrashes = true;

    // ========== Sunrise/Sunset ==========
    public static String sonnenAufgangStringGerundet = "25:61"; // default == unsinnige zeit
    public static String sonnenUntergangStringGerundet = "25:61"; // default == unsinnige zeit
    public static String sonnenAufgangString = "25:61"; // default == unsinnige zeit
    public static String sonnenUntergangString = "25:61"; // default == unsinnige zeit

    // ========== Schlagwerk ==========
    public static boolean flagSchlagwerkOnOff = true;
    public static int beginnSchlagwerk1; // zeit in sekunden seit Mitternacht
    public static int endeSchlagwerk1;
    public static int beginnSchlagwerk2;
    public static int endeSchlagwerk2;
    // neu 4.12.2014 schlagwerk auch während eine Melodie geleutet wird // in Schlagwerkzeiten EIN/AUS
    public static boolean schlagwerkWaehrendMelodieLeuten;
    public static int schlagWerkVariable1 = 0; // das ist SWV1 z.b. in 10_Uhr.xls

    // ========== Melody/Playback ==========
    public static boolean melodieAktiv = false;
    public static int indexMelodieStart = 0;  // index in array, echte melodie offset ist index + 3 !
    public static MelodieThreadNew melodieThreadNew = null;
    public static String nameNextMelodie = "";
    /** Anzeige der gerade ausgegebenen Melodie-Zeile (z. B. "Zeile 5 von 32"), von MelodieThreadNew gesetzt. */
    public static String currentMelodieZeileAnzeige = "";
    /** Dauer der aktuellen Zeile (z. B. "1,2 s") für Web-UI. */
    public static String currentMelodieZeileDauer = "";
    /** Relais G1–G16 der aktuellen Zeile (Schwingen/Klöppel): 1 = ein (rot), 0 = aus (grün). */
    public static int[] currentMelodieZeileRelais = new int[16];
    /** Vorschwing/Motor-Relais G1–G16: 1 = ein (rot), 0 = aus (Punkt mit weißem Rand). */
    public static int[] currentMelodieZeileVorschwing = new int[16];
    public static String pathAndFileNameNextMelodie = "";
    /** Nach Stop: Pfad der gestoppten Melodie – dieses Programm wird nicht wieder automatisch gestartet (bis Nutzer z.B. Sofortstart/Manuell startet). */
    public static String pathStoppedByUser = "";
    public static long nextMelodieStartSekunden2 = 0;
    public static ArrayList<ArrayList<Long>> vorschwingenStartzeitenMotorRelais = new ArrayList<ArrayList<Long>>();
    public static ArrayList<Integer> vorschwingenMotorRelaisNeu = new ArrayList<Integer>();
    //public static ArrayList<Integer> vorschwingenPlatinenNummer = new ArrayList<Integer>();
    public static ArrayList<Integer> laeutenKloeppelRelais = new ArrayList<Integer>();
    //public static ArrayList<Integer> laeuteonPlatinenNummer = new ArrayList<Integer>();
    public static ArrayList<Integer> vorschwingZeitSekunden = new ArrayList<Integer>();
    /** Vorschwingzeit in Sekunden pro Läuten-Relais (berechnet). Damit jede Glocke ihre konfigurierte Zeit bekommt, unabhängig von der Reihenfolge der Config-Zeilen. */
    public static Map<Integer, Integer> vorschwingZeitSekundenByRelais = new HashMap<Integer, Integer>();

    /** Liefert die Vorschwingzeit in Sekunden für die Glocke mit Spalten-Index j (0=A, 1=B, …). Nutzt die Relais-Zuordnung, damit die richtige Glocke die richtige Zeit bekommt. */
    public static int getVorschwingSekundenForColumn(int columnIndex) {
        if (columnIndex < 0) return 0;
        if (columnIndex < laeutenKloeppelRelais.size()) {
            Integer relais = laeutenKloeppelRelais.get(columnIndex);
            if (relais != null && vorschwingZeitSekundenByRelais.containsKey(relais))
                return vorschwingZeitSekundenByRelais.get(relais);
        }
        if (columnIndex < vorschwingZeitSekunden.size())
            return vorschwingZeitSekunden.get(columnIndex);
        return 0;
    }
    //public static ArrayList<Long> beginnVorschwingenStartinMsDurchTausend = new ArrayList<Long>();

    // ========== Manual Start ==========
    public static boolean manuellerStartEinAus = false;
    public static int startStundeManuell = 0;
    public static int startMinuteManuell = 0;
    public static boolean manuellerStartAktiviert = false;

    // ========== Heating ==========
    public static int heizungOnTimer = 0;
    public static int heizungEingeschaltetTimer = 0;
    public static String nextHeizungFunktionsName = "";
    public static int heizungRelaisNumber3 = 0;
    public static int heizungRelaisNumberManual2 = 0;
    public static String nextHeizungLaufzeitMinutenString = "0";
    public static long nextHeizungStartSekunden = 0L;
    public static long nextHeizungStopSekunden = 0L;

    // ========== Time Server ==========
    public static boolean timeServerOk = false;
    public static String timeServerEinAus = "AUS";
    public static String timeServerIp = "141.2.22.74"; //Time Server uni Frankfurt
    public static String timeServerMaxOffsetMinuten = "5";
    public static String timeServerAbfrageIntervallMs = "500";
    public static long readSntpTimeMs = 0; // wird noch von NMEA_gps_clock verwendet (GPS-Zeitsynchronisation)
    public static boolean schlagwerkTriggerSyncTime2 = false;

    // ========== GPS/Serial GPS ==========
    public static boolean serialGPS_OnOff = false;
    public static String serialGPS_IP = "10.0.0.1";
    public static int serialGPS_port = 50210;
    public static int nmeaGpsNoData = 0;

    // ========== Clocks (Uhr A/B/C) ==========
    public static int uhrA_calendarZeit = 0;    // in minuten
    public static int uhrA_angezeigteZeit = 0;
    public static boolean uhrA_doRun; // flag fuer thread auslaufen lassen
    public static boolean uhrA_lastRelaisA = false; // true = letztes Relais war A, false = letztes Relais war B
    public static int uhrB_calendarZeit = 0;
    public static int uhrB_angezeigteZeit = 0;
    public static boolean uhrB_doRun;
    public static boolean uhrB_lastRelaisA = false; // true = letztes Relais war A, false = letztes Relais war B
    public static int uhrC_calendarZeit = 0;
    public static int uhrC_angezeigteZeit = 0;
    public static boolean uhrC_doRun;
    public static boolean uhrC_lastRelaisA = false; // true = letztes Relais war A, false = letztes Relais war B
    // Monduhr D: historische 12h-Felder bleiben für Altpfade erhalten; aktiv genutzt wird der Mond-Impulswert
    public static int uhrD_calendarZeit = 0;      // Altwert in 12h-Minuten (0–719), aktuell für Monduhr D nicht relevant
    public static int uhrD_angezeigteZeit = 0;   // Altwert in 12h-Minuten (0–719), aktuell für Monduhr D nicht relevant
    public static int uhrD_mondphaseSoll = 0;    // Soll-Impulswert im konfigurierten Mondzyklus
    public static int uhrD_mondphaseIst = 0;      // Ist-Impulswert im konfigurierten Mondzyklus
    public static boolean uhrD_doRun;            // Flag für Thread auslaufen lassen
    public static boolean uhrD_lastRelaisA = false; // true = letztes Relais war A, false = letztes Relais war B
    public static long uhrD_letzteBerechnungMs = 0; // Zeitpunkt der letzten Mondphase-Berechnung
    public static boolean nebenuhrEinstellungsSeiteOffen = false; // Flag: Nebenuhr-Einstellungsseite ist offen
    /** Nach Load: auf erste volle Minute warten, dann Soll=RTC setzen und normal aufholen/warten. */
    public static boolean nebenuhrWaitFirstFullMinute = false;
    // Flags für Neuladen der Nebenuhr-Konfiguration (wenn über Web-UI geändert)
    public static boolean nebenuhrA_configNeuLaden = false; // Flag: Nebenuhr A Konfiguration neu laden
    public static boolean nebenuhrB_configNeuLaden = false; // Flag: Nebenuhr B Konfiguration neu laden
    public static boolean nebenuhrC_configNeuLaden = false; // Flag: Nebenuhr C Konfiguration neu laden
    public static boolean nebenuhrD_configNeuLaden = false; // Flag: Nebenuhr D (Monduhr) Konfiguration neu laden
    // flag fuer zeit aufholen bzw. stop waehrend uhrzeit einstellung (A, B, C, D)
    public static Boolean[] uhr_warten = new Boolean[]{false, false, false, false};
    /** True wenn Zeiteingabe-Dialog für diese Nebenuhr offen ist (A=0, B=1, C=2). Dann läuft die Uhr nicht und holt nicht auf. */
    public static volatile boolean[] uhr_zeiteingabeAktiv = new boolean[]{false, false, false, false};
    /** Wenn true: schlafenden NebenUhr-Thread sofort wecken (nach manueller Zeiteingabe in SetNebenuhrActivity). */
    public static volatile boolean uhrA_aufholenAnfordern = false;
    public static volatile boolean uhrB_aufholenAnfordern = false;
    public static volatile boolean uhrC_aufholenAnfordern = false;

    // ========== User Programs ==========
    public static boolean benutzerGeandert = false;
    public static boolean sendUserProgrammRemote2 = true;
    public static ArrayList<String> userProgramIds = new ArrayList<String>();
    public static boolean saveBenutzerProgrammSofort = false;
    public static int userProgramsCount = -1;
    public static int benutzerMelodienIndex = -1;
    public static boolean helpForStartBenutzermelodien = true;
    public static Vector<Boolean> benutzerMelodieTasteOn2 = new Vector<Boolean>();
    /** Melodiename pro Benutzerprogramm-Slot (aus DB oder Excel). Wird in DB gespeichert/geladen. */
    public static ArrayList<String> benutzerMelodieName = new ArrayList<String>();
    /** Programmtag (Tagtyp) pro Benutzerprogramm-Slot für dieses Datum (z. B. Normalprogramm, Heiligabend). Leer = Kalender verwenden. */
    public static ArrayList<String> benutzerTagtypName = new ArrayList<String>();
    protected static Vector<Boolean> sofortStartButtonOn = new Vector<Boolean>();
    public static final Vector<Integer> minuten = new Vector<Integer>();
    public static final Vector<Integer> stunden = new Vector<Integer>();
    public static final Vector<Integer> monate = new Vector<Integer>();
    public static final Vector<Integer> tage = new Vector<Integer>();
    public static final Vector<Integer> jahre = new Vector<Integer>();
    protected static boolean firstStartMelodie;
    public static boolean sofortStartPopupFlag = false;
    public static int sofortStartPopupRelaisNummerAktiv = 0;
    public static String sofortStartPopupFilename = "";
    public static boolean sofortStartPopupGefunden = false;

    // ========== Bluetooth/Serial IO ==========
    public static boolean bluetoothMode;
    public static BluetoothAdapter myBluetoothAdapter;
    public static int bluetoothStatus;  // 0 = noch nicht getestet, 1 = keine Bluetooth hardware, 2 = keine Verbindung
    public static boolean bt_io_ok;
    public static int carambolaStatus; // -1 vor dem checkCarambolaThread, 0 wenn alles O.K., ab 1 die IP Adresse wo ein Fehler aufgetreten ist
    public static boolean carambola_io_ok;
    public static boolean serial_io_status4[] = {false, true, true, true}; // ist true wenn ein echo von bluetooth oder carambole gekommen ist
    /** Zeitstempel (elapsedRealtime) des letzten Carambola-Fehlers pro Platine; für Nebenuhr: Impuls nur zählen wenn kein Fehler während des Impulses. */
    public static long[] lastCarambolaErrorTimeMs = new long[4];
    /** Pro Platine: true während ein Nebenuhr-Impuls läuft. Nur dann setzt Carambola bei Timeout lastCarambolaErrorTimeMs (Glocken/andere Relais: nur Zustand senden, Zeit egal). */
    public static boolean[] nebenuhrImpulsLaeuftPlatine = new boolean[4];
    public static ArrayList<String> ipList = new ArrayList<String>();
    public static ArrayList<Integer> portList = new ArrayList<Integer>();
    public static boolean serial_io_ThreadsRun;
    /** Demo-Modus Platinen: keine echte Hardware-Ansteuerung, Relais nur in Software. */
    public static boolean platinenDemoModus = false;

    /** Deutsche Fallbacks, wenn Sprachdatei.xls nicht geladen werden konnte (z. B. Permission denied). */
    private static String getDefaultUebersetzung(int index) {
        switch (index) {
            case 0: return "AUTOMATIK AUSGESCHALTET";
            case 1: return "SONNTAG";
            case 2: return "MONTAG";
            case 3: return "DIENSTAG";
            case 4: return "MITTWOCH";
            case 5: return "DONNERSTAG";
            case 6: return "FREITAG";
            case 7: return "SAMSTAG";
            case 8: return "Beginn";
            case 9: return "nächster Start --> morgen";
            case 10: return "Nebenuhr";
            case 11: return "Bitte warten";
            case 12: return "";
            case 13: return "Datum";
            case 14: return "Sonnenaufgang";
            case 15: return "Sonnenuntergang";
            case 16: return "";
            case 17: return "AUTOMATIK EINGESCHALTET";
            default: return (index >= 18 && index <= 31) ? ("?" + index) : (" x " + index + " ? ");
        }
    }

    public static String getUebersetzung(int index) {
        if (uebersetzteTexte == null) {
            return getDefaultUebersetzung(index);
        }
        if (index < 0 || index >= uebersetzteTexte.size()) {
            return " x " + index + " ? ";
        }
        String s = uebersetzteTexte.get(index);
        return (s != null ? s : getDefaultUebersetzung(index));
    }
}
