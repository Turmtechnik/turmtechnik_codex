package tom.turmtechnik;

//import android.util.Log;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;
import java.util.Vector;

import jxl.read.biff.BiffException;

/**
 * Created by alfred on 16.01.15.
 */
public class TagesSuche {
    private static final String sourceFileName2 = "TagesSuche";
    private String filePathAndName2;

    long timeStartVorlauf2;

    private final boolean version30 = false;

    public static final int SPALTE_A_STARTZEIT = 0;
    public static final int SPALTE_B_FUNKTION = 1;
    public static final int SPALTE_C_MELODIE_NAME = 2;


    // alles verschoben um 1 nach rechts // 9.8.16
/*
    private final int SPALTE_D_WOCHENTAG_MONTAG = 3 ;
    private final int SPALTE_K_IMMER = 10 ;
    private final int SPALTE_L_PERIODISCH = 11 ;
    private final int SPALTE_M_START = 12 ;
    private final int SPALTE_N_ENDE = 13 ;
    private final int SPALTE_O_VERKNUEPFTE_TASTE = 14 ;
    private final int SPALTE_P_PRIORITAET = 15 ;
*/

    public static final int SPALTE_D_DAUER_MINUTEN_HEIZUNG = 3; // Kann vorhanden sein oder fehlen (dann wird Dummy erstellt)
    public static final int SPALTE_E_WOCHENTAG_MONTAG = 4; // Wochentage beginnen IMMER bei Spalte E (4)
    public static final int SPALTE_L_IMMER = 11;
    public static final int SPALTE_M_PERIODISCH = 12;
    public static final int SPALTE_N_START = 13;
    public static final int SPALTE_O_ENDE = 14;
    public static final int SPALTE_P_VERKNUEPFTE_TASTE = 15;
    public static final int SPALTE_Q_PRIORITAET = 16;


    Calendar calendar;

    private Vector<Integer> aktuelleZeitZeilenListe = new Vector<Integer>();

    ExcelRead excelread2;
    private int localTurnOffVerknuepft;
    private boolean flag_A_oder_T_gefunden;
    
    // Für DB-Programme: Speichere Programme und Mapping von Excel-Zeile zu Programm-ID
    private java.util.List<Programm> programmeFromDatabase = null;
    private java.util.Map<Integer, Programm> zeileToProgrammMap = null; // Excel-Zeile -> Programm
    
    // Für zukünftige Zeiten: Speichere zukünftiges Datum für zeileOk()
    private int futureJahr = -1;
    private int futureMonat = -1;
    private int futureTag = -1;

    /** Zeilen 1000..1019 = Benutzerprogramm-Slot 0..19 (wie in UhrThread.BENUTZERPROGRAMM_ZEILE_OFFSET). */
    private static final int BENUTZERPROGRAMM_ZEILE_OFFSET = 1000;
    private static final int BENUTZERPROGRAMM_ZEILE_MAX = 1020;

    public TagesSuche() {

    }

    public TagesSuche(ExcelRead excelRead1, String filePathAndName) {
        excelread2 = excelRead1;
        filePathAndName2 = filePathAndName;
        calendar = GregorianCalendar.getInstance();
        
        // Wenn excelread2 null ist, lade Programme aus DB
        if (excelread2 == null && filePathAndName != null && !filePathAndName.isEmpty()) {
            loadProgrammeFromDatabase(filePathAndName);
        }
    }
    
    /**
     * Lädt Programme aus der Datenbank für den gegebenen Tagtyp.
     * Probiert bei leerem Ergebnis zusätzlich die Variante mit bzw. ohne ".xls".
     */
    private void loadProgrammeFromDatabase(String tagtypName) {
        try {
            android.content.Context context = TurmtechnikActivity.turmtechnikContext;
            if (context == null) return;
            PlatinenDatabaseHelper dbHelper = PlatinenDatabaseHelper.getInstance(context);
            programmeFromDatabase = dbHelper.getProgrammeByTagtyp(tagtypName);
            if ((programmeFromDatabase == null || programmeFromDatabase.isEmpty()) && tagtypName != null) {
                String alt = tagtypName.toLowerCase().endsWith(".xls")
                    ? tagtypName.substring(0, tagtypName.length() - 4)
                    : tagtypName + ".xls";
                programmeFromDatabase = dbHelper.getProgrammeByTagtyp(alt);
                if (programmeFromDatabase != null && !programmeFromDatabase.isEmpty()) {
                    android.util.Log.d("TagesSuche", "loadProgrammeFromDatabase: Programm geladen mit alternativem Tagtyp-Namen: " + alt);
                }
            }
            if (programmeFromDatabase != null && !programmeFromDatabase.isEmpty()) {
                zeileToProgrammMap = new java.util.HashMap<>();
                for (Programm programm : programmeFromDatabase) {
                    int excelZeile = programm.getId() + 3;
                    zeileToProgrammMap.put(excelZeile, programm);
                }
                android.util.Log.d("TagesSuche", "loadProgrammeFromDatabase: " + programmeFromDatabase.size() + " Programme geladen für " + tagtypName);
            } else {
                zeileToProgrammMap = null;
                android.util.Log.w("TagesSuche", "loadProgrammeFromDatabase: Keine Programme in DB für " + tagtypName);
            }
        } catch (Exception e) {
            android.util.Log.e("TagesSuche", "Fehler beim Laden von Programmen aus DB", e);
        }
    }
    
    /**
     * Stellt sicher, dass zeileToProgrammMap im DB-Modus geladen ist (z. B. falls beim Konstruktor Context noch null war).
     * Wird vom UhrThread vor sucheBlockNummer/zeileOk aufgerufen, damit getProgrammByZeile() und Melodie-Start funktionieren.
     */
    public void ensureProgrammeFromDatabaseLoaded() {
        if (excelread2 == null && filePathAndName2 != null && !filePathAndName2.isEmpty()
                && (zeileToProgrammMap == null || zeileToProgrammMap.isEmpty())) {
            loadProgrammeFromDatabase(filePathAndName2);
        }
    }
    
    /**
     * Konvertiert Excel-Zeile zu DB-Programm-ID (Excel-Zeile - 3 = zeile_index).
     */
    private Programm getProgrammByZeile(int excelZeile) {
        if (zeileToProgrammMap != null) {
            return zeileToProgrammMap.get(excelZeile);
        }
        return null;
    }
    
    /**
     * Setzt das Datum für die Suche nach zukünftigen Programmen (für zeileOk()).
     * Wird von UhrThread.checkNextAutomaticStart() aufgerufen.
     */
    public void setFutureDate(int jahr, int monat, int tag) {
        this.futureJahr = jahr;
        this.futureMonat = monat;
        this.futureTag = tag;
    }

    public String pad(int c) {
        if (c >= 10)
            return String.valueOf(c);
        else
            return "0" + String.valueOf(c);
    }

    /** Normalisiert Startzeit "HH:mm" (z. B. "18:61" → "19:01"), damit keine ungültigen Minuten angezeigt werden. */
    public static String normalizeStartzeitDisplay(String startzeit) {
        if (startzeit == null || !startzeit.contains(":")) return startzeit;
        String[] parts = startzeit.trim().split(":");
        if (parts.length < 2) return startzeit;
        try {
            int h = Integer.parseInt(parts[0].trim());
            int m = Integer.parseInt(parts[1].trim());
            if (m >= 60) {
                h += m / 60;
                m = m % 60;
            }
            if (m < 0) m = 0;
            if (h > 23) h = h % 24;
            if (h < 0) h = 0;
            return (h >= 10 ? "" : "0") + h + ":" + (m >= 10 ? "" : "0") + m;
        } catch (NumberFormatException e) {
            return startzeit;
        }
    }

    public String checkProgrammliste2(int jahr, int monat, int tag, int checkStunde, int checkMinute, ExcelRead excelread1, String filePathAndName1) {

        filePathAndName2 = filePathAndName1;
        excelread2 = excelread1;

        calendar = new GregorianCalendar(jahr, monat, tag); // teste fiktiven Tag
        
        // Wenn excelread2 null ist, lade Programme aus DB
        if (excelread2 == null && filePathAndName1 != null && !filePathAndName1.isEmpty()) {
            loadProgrammeFromDatabase(extractTagtypNameFromPath(filePathAndName1));
        }

        // NUR Datenbank: Programme aus der Datenbank laden (KEIN Excel-Fallback mehr)
        String tagtypName = extractTagtypNameFromPath(filePathAndName1);
        if (tagtypName != null && !tagtypName.trim().isEmpty()) {
            try {
                android.content.Context context = TurmtechnikActivity.turmtechnikContext;
                if (context != null) {
                    PlatinenDatabaseHelper dbHelper = PlatinenDatabaseHelper.getInstance(context);
                    java.util.List<Programm> programme = dbHelper.getProgrammeByTagtyp(tagtypName);
                    
                    if (programme != null && !programme.isEmpty()) {
                        android.util.Log.d("TagesSuche", "checkProgrammliste2: " + programme.size() + " Programme aus DB für " + tagtypName);
                        // Verwende Programme aus Datenbank
                        return checkProgrammliste2FromDatabase(jahr, monat, tag, checkStunde, checkMinute, programme);
                    } else {
                        android.util.Log.e("TagesSuche", "FEHLER: Keine Programme in DB für " + tagtypName + "! Bitte 'Import Excel Programmtage' in Web-UI verwenden.");
                        return ""; // Keine Programme gefunden
                    }
                } else {
                    android.util.Log.e("TagesSuche", "FEHLER: Context ist null, kann Programme nicht aus DB laden");
                    return "";
                }
            } catch (Exception e) {
                android.util.Log.e("TagesSuche", "FEHLER beim Laden aus DB: " + e.getMessage(), e);
                e.printStackTrace();
                return ""; // Fehler beim Laden
            }
        } else {
            android.util.Log.e("TagesSuche", "FEHLER: Tagtyp-Name konnte nicht aus Pfad extrahiert werden: " + filePathAndName1);
            return "";
        }
        
        // Excel-Fallback ENTFERNT - nur noch Datenbank!
        // Alle Pfade haben bereits ein return Statement, daher ist dieser Code nie erreichbar
    }
    
    /**
     * Extrahiert den Tagtyp-Namen aus dem Dateipfad.
     * Entfernt Pfad und .xls-Erweiterung falls vorhanden.
     */
    private String extractTagtypNameFromPath(String filePath) {
        if (filePath == null || filePath.trim().isEmpty()) {
            return null;
        }
        
        // Extrahiere Dateiname aus Pfad
        int lastSlash = filePath.lastIndexOf("/");
        String fileName = (lastSlash >= 0) ? filePath.substring(lastSlash + 1) : filePath;
        
        // Entferne .xls-Erweiterung falls vorhanden
        if (fileName.toLowerCase().endsWith(".xls")) {
            fileName = fileName.substring(0, fileName.length() - 4);
        }
        
        return fileName;
    }
    
    /**
     * Lädt Programme aus der Datenbank und sucht nach passenden Programmen für die gegebene Zeit.
     * Diese Methode funktioniert ähnlich wie checkProgrammliste2, verwendet aber Datenbank-Programme statt Excel.
     */
    public String checkProgrammliste2FromDatabase(int jahr, int monat, int tag, int checkStunde, int checkMinute, java.util.List<Programm> programme) {
        android.util.Log.d("TagesSuche", "checkProgrammliste2FromDatabase: Prüfe " + programme.size() + " Programme für " + checkStunde + ":" + checkMinute);
        
        // Initialisiere Calendar für Datums-Prüfungen
        calendar = new GregorianCalendar(jahr, monat, tag);
        
        // Erstelle eine Liste von Programmen, die zur gesuchten Zeit passen
        java.util.List<Programm> passendeProgramme = new java.util.ArrayList<>();
        
        for (Programm programm : programme) {
            String startzeit = programm.getStartzeit();
            if (startzeit == null || startzeit.trim().isEmpty()) {
                android.util.Log.d("TagesSuche", "checkProgrammliste2FromDatabase: Programm ID " + programm.getId() + " hat keine Startzeit (null oder leer)");
                continue;
            }
            
            android.util.Log.d("TagesSuche", "checkProgrammliste2FromDatabase: Prüfe Programm ID " + programm.getId() + " mit Startzeit: '" + startzeit + "'");
            
            // Parse Startzeit
            String[] zeitSplit = {"25", "61"}; // default == unsinnige zeit

            if (startzeit.contains(":")) {
                zeitSplit = startzeit.split(":");
                android.util.Log.d("TagesSuche", "checkProgrammliste2FromDatabase: Zeit geparst: " + zeitSplit[0] + ":" + zeitSplit[1]);
            } else if (startzeit.equals("SA")) {
                // Sonnenaufgang
                if (StaticVariable.sonnenAufgangStringGerundet != null) {
                    zeitSplit = StaticVariable.sonnenAufgangStringGerundet.split(":");
                    android.util.Log.d("TagesSuche", "checkProgrammliste2FromDatabase: Sonnenaufgang: " + zeitSplit[0] + ":" + zeitSplit[1]);
            } else {
                    android.util.Log.d("TagesSuche", "checkProgrammliste2FromDatabase: Sonnenaufgang nicht verfügbar");
                    continue;
                }
            } else if (startzeit.equals("SU")) {
                // Sonnenuntergang
                if (StaticVariable.sonnenUntergangStringGerundet != null) {
                    zeitSplit = StaticVariable.sonnenUntergangStringGerundet.split(":");
                    android.util.Log.d("TagesSuche", "checkProgrammliste2FromDatabase: Sonnenuntergang: " + zeitSplit[0] + ":" + zeitSplit[1]);
                } else {
                    android.util.Log.d("TagesSuche", "checkProgrammliste2FromDatabase: Sonnenuntergang nicht verfügbar");
                    continue;
                }
            } else if (startzeit.equals("99")) {
                // Spezialfall: Zeit passt immer
                android.util.Log.d("TagesSuche", "checkProgrammliste2FromDatabase: Programm ID " + programm.getId() + " hat Zeit '99' - passt immer");
                passendeProgramme.add(programm);
                continue;
            } else {
                android.util.Log.d("TagesSuche", "checkProgrammliste2FromDatabase: Programm ID " + programm.getId() + " hat unbekanntes Zeitformat: '" + startzeit + "'");
                continue;
            }
            
            // Prüfe ob Zeit passt
            try {
                int programmMinute = Integer.parseInt(zeitSplit[1]);
                int programmStunde = Integer.parseInt(zeitSplit[0]);
                
                android.util.Log.d("TagesSuche", "checkProgrammliste2FromDatabase: Vergleiche Programm-Zeit " + programmStunde + ":" + programmMinute + " mit gesuchter Zeit " + checkStunde + ":" + checkMinute);
                
                if (checkMinute == programmMinute && checkStunde == programmStunde) {
                    android.util.Log.d("TagesSuche", "checkProgrammliste2FromDatabase: Zeit passt! Programm ID " + programm.getId() + " hinzugefügt");
                    passendeProgramme.add(programm);
                } else {
                    android.util.Log.d("TagesSuche", "checkProgrammliste2FromDatabase: Zeit passt nicht (" + programmStunde + ":" + programmMinute + " != " + checkStunde + ":" + checkMinute + ")");
                }
            } catch (NumberFormatException e) {
                android.util.Log.w("TagesSuche", "Ungültige Zeit in Programm ID " + programm.getId() + ": " + startzeit + " (zeitSplit[0]='" + zeitSplit[0] + "', zeitSplit[1]='" + zeitSplit[1] + "')");
            }
        }
        
        if (passendeProgramme.isEmpty()) {
            android.util.Log.d("TagesSuche", "checkProgrammliste2FromDatabase: Keine passenden Programme gefunden");
            return "";
        }
        
        android.util.Log.d("TagesSuche", "checkProgrammliste2FromDatabase: " + passendeProgramme.size() + " passende Programme gefunden");
        
        // Finde das beste Programm (nach Priorität und zeileOk-Prüfung)
        // Erstelle temporäre ExcelRead-Struktur für zeileOk-Prüfung
        // Da zeileOk() ExcelRead verwendet, müssen wir die Programme in eine temporäre Struktur konvertieren
        // Oder wir implementieren eine zeileOk-Variante für Datenbank-Programme
        
        // Für jetzt: Verwende das erste passende Programm mit höchster Priorität
        Programm bestesProgramm = null;
        int hoechstePrioritaet = -1;
        
        for (Programm programm : passendeProgramme) {
            // Prüfe ob Programm passt (Wochentag, Verknüpfte Taste, Periodizität, Datum)
            if (programmOkFromDatabase(programm, jahr, monat, tag)) {
                if (programm.getPrioritaet() > hoechstePrioritaet) {
                    hoechstePrioritaet = programm.getPrioritaet();
                    bestesProgramm = programm;
                }
            }
        }
        
        // Wenn kein Programm mit Priorität gefunden, nimm das erste passende
        if (bestesProgramm == null) {
            for (Programm programm : passendeProgramme) {
                if (programmOkFromDatabase(programm, jahr, monat, tag)) {
                    bestesProgramm = programm;
                    break;
                }
            }
        }
        
        if (bestesProgramm == null) {
            android.util.Log.d("TagesSuche", "checkProgrammliste2FromDatabase: Kein Programm hat alle Prüfungen bestanden");
            return "";
        }
        
        // Erstelle Rückgabe-String im gleichen Format wie Excel-Version
        String returnTimeTemp = bestesProgramm.getStartzeit();
        if (bestesProgramm.getFunktion() != null && bestesProgramm.getFunktion().equals("Melodie")) {
            String melodieName = bestesProgramm.getMelodieName();
            if (melodieName != null) {
                returnTimeTemp += " " + melodieName;
                String beginnTimeTemp = getBeginnTime2(melodieName, jahr, monat, tag, checkStunde, checkMinute);
                returnTimeTemp += " --> " + StaticVariable.getUebersetzung(8) + " " + beginnTimeTemp;
            }
        } else {
            returnTimeTemp += " " + (bestesProgramm.getFunktion() != null ? bestesProgramm.getFunktion() : "");
            returnTimeTemp += " --> 0" + (bestesProgramm.getDauerHeizung() != null ? bestesProgramm.getDauerHeizung() : "");
        }
        
        android.util.Log.d("TagesSuche", "checkProgrammliste2FromDatabase: Gefunden: " + returnTimeTemp);
        return returnTimeTemp;
    }
    
    /**
     * Prüft ob ein Datenbank-Programm für das gegebene Datum passt.
     * Äquivalent zu zeileOk(), aber für Datenbank-Programme.
     */
    public boolean programmOkFromDatabase(Programm programm, int jahr, int monat, int tag) {
        return programmOkFromDatabase(programm, jahr, monat, tag, false);
    }

    /**
     * Wie programmOkFromDatabase(Programm, int, int, int). Bei nurFuerAnzeige=true wird die
     * Verknüpfte-Taste-Prüfung übersprungen (z. B. für Programmabfrage-Seite: alle passenden Programme anzeigen).
     */
    public boolean programmOkFromDatabase(Programm programm, int jahr, int monat, int tag, boolean nurFuerAnzeige) {
        // Ausführliche Logs nur auf Verbose (vermeidet Flut beim Aufbau der Tagesprogrammliste / Soforttaste)
        android.util.Log.v("TagesSuche", "programmOkFromDatabase: Prüfe Programm ID=" + programm.getId() + ", Startzeit=" + programm.getStartzeit() + ", Datum=" + tag + "." + (monat + 1) + "." + jahr);
        
        // Initialisiere Calendar für das gegebene Datum (monat ist 0-basiert)
        if (calendar == null) {
            calendar = new GregorianCalendar(jahr, monat, tag);
        } else {
            calendar.set(jahr, monat, tag);
        }
        
        // Wochentag-Prüfung
        int wochentag = calendar.get(Calendar.DAY_OF_WEEK);
        if (wochentag == 1) {
            wochentag = 6;  // Sonntag wird zu Index 6
        } else {
            wochentag = wochentag - 2;  // Montag == 0, Dienstag == 1, ..., Samstag == 5
        }
        
        boolean wochentagOk = false;
        switch (wochentag) {
            case 0: wochentagOk = programm.isMontag(); break;
            case 1: wochentagOk = programm.isDienstag(); break;
            case 2: wochentagOk = programm.isMittwoch(); break;
            case 3: wochentagOk = programm.isDonnerstag(); break;
            case 4: wochentagOk = programm.isFreitag(); break;
            case 5: wochentagOk = programm.isSamstag(); break;
            case 6: wochentagOk = programm.isSonntag(); break;
        }
        
        if (!wochentagOk) {
            android.util.Log.v("TagesSuche", "programmOkFromDatabase: ID=" + programm.getId() + " Wochentag passt nicht - ABGELEHNT");
            return false;
        }
        
        // Verknüpfte Taste-Prüfung (bei nurFuerAnzeige überspringen, damit Programmabfrage alle passenden Programme zeigt)
        if (!nurFuerAnzeige && programm.getVerknuepfteTaste() != null && !programm.getVerknuepfteTaste().trim().isEmpty()) {
            String verknuepfteTaste = programm.getVerknuepfteTaste().trim();
            boolean tasteAktiv = false;
            synchronized (TurmtechnikActivity.VERKNUEPFTE_TASTEN_LOCK) {
                if (UhrThread.verknuepfteTastenString != null && UhrThread.verknuepfteTastenString.size() > 0) {
                    for (int i = 0; i < UhrThread.verknuepfteTastenString.size(); i++) {
                        String activity2String = UhrThread.verknuepfteTastenString.elementAt(i) != null ? UhrThread.verknuepfteTastenString.elementAt(i).trim() : "";
                        if (activity2String.equalsIgnoreCase(verknuepfteTaste)) {
                            if (TurmtechnikActivity.verknuepfteTastenOn != null
                                && i < TurmtechnikActivity.verknuepfteTastenOn.length
                                && Boolean.TRUE.equals(TurmtechnikActivity.verknuepfteTastenOn[i])) {
                                tasteAktiv = true;
                                if (!programm.isImmer()) {
                                    localTurnOffVerknuepft = i;
                                }
                            }
                            break;
                        }
                    }
                }
            }
            if (!tasteAktiv) {
                if (UhrThread.verknuepfteTastenString == null || UhrThread.verknuepfteTastenString.isEmpty()) {
                    android.util.Log.v("TagesSuche", "programmOkFromDatabase: ID=" + programm.getId() + " Verknüpfte Taste erforderlich, keine aktiv - ABGELEHNT");
                } else {
                    android.util.Log.v("TagesSuche", "programmOkFromDatabase: ID=" + programm.getId() + " Verknüpfte Taste nicht aktiv - ABGELEHNT");
                }
                return false;
            }
        }
        
        // Periodizität-Prüfung
        int periodisch = programm.getPeriodisch();
        boolean isSommer = isSommerzeit();
        if (periodisch == 1 && !isSommer) {
            android.util.Log.v("TagesSuche", "programmOkFromDatabase: ID=" + programm.getId() + " Periodizität Sommer, ist Winter - ABGELEHNT");
            return false;
        }
        if (periodisch == 2 && isSommer) {
            android.util.Log.v("TagesSuche", "programmOkFromDatabase: ID=" + programm.getId() + " Periodizität Winter, ist Sommer - ABGELEHNT");
            return false;
        }
        
        // Start/Ende-Datum-Prüfung
        String startDatum = programm.getStartDatum();
        String endeDatum = programm.getEndeDatum();
        if (startDatum != null && !startDatum.trim().isEmpty()) {
            boolean datumOk = startEndeDatumFromDatabase(startDatum, endeDatum, jahr, monat, tag);
            if (!datumOk) {
                android.util.Log.v("TagesSuche", "programmOkFromDatabase: ID=" + programm.getId() + " Datum " + startDatum + "-" + endeDatum + " passt nicht zu " + tag + "." + (monat + 1) + " - ABGELEHNT");
                return false;
            }
        }
        
        android.util.Log.v("TagesSuche", "programmOkFromDatabase: ID=" + programm.getId() + " - AKZEPTIERT");
        return true;
    }
    
    /**
     * Prüft ob das gegebene Datum innerhalb des Start/Ende-Datums liegt.
     * Äquivalent zu startEndeDatum(), aber für Datenbank-Programme.
     */
    private boolean startEndeDatumFromDatabase(String startZeit, String stopZeit, int jahr, int monat, int tag) {
        try {
            if (startZeit == null || startZeit.trim().isEmpty()) {
                return true;  // keine startzeit, o.k. melden
            }
            
            if (stopZeit == null) {
                stopZeit = "";
            }
            
            startZeit = startZeit.trim();
            stopZeit = stopZeit.trim();
            
            // Prüfe auf .W Format (1.W–4.W = 1.–4. Woche des Monats), inkl. Startdatum „ab TT.MM.JJJJ“
            if (stopZeit.endsWith(".W") && !stopZeit.contains("Wochen")) {
                if (!isDatumAmStartdatumOderDanach(startZeit, jahr, monat, tag)) {
                    return false;
                }
                return startEndeVersion_W_FromDatabase(stopZeit, tag);
            }
            
            // Prüfe auf .A Format (z.B. 23.03.2016, 7.A oder 14.A = alle 2 Wochen)
            if (stopZeit.contains("A")) {
                return startEndeVersion_A_FromDatabase(startZeit, stopZeit, jahr, monat, tag);
            }
            
            // Prüfe auf .T Format (z.B. 8.T bis 14.T)
            if (stopZeit.contains("T")) {
                return startEndeVersion_T_FromDatabase(startZeit, stopZeit, tag);
            }
            
            // Normale Datums-Prüfung (z.B. 1.4 bis 31.4)
            String[] startZeitSplit = startZeit.split("\\.");
            String[] stopZeitSplit = stopZeit.split("\\.");
            
            if (startZeitSplit.length < 2 || stopZeitSplit.length < 2) {
                android.util.Log.w("TagesSuche", "startEndeDatumFromDatabase: Ungültiges Datumsformat: Start='" + startZeit + "', Ende='" + stopZeit + "'");
                return false;
            }
            
            int programmlisteStartZeitTag = Integer.parseInt(startZeitSplit[0]);
            int programmlisteStartZeitMonat = Integer.parseInt(startZeitSplit[1]);
            
            int programmlisteStopZeitTag = Integer.parseInt(stopZeitSplit[0]);
            int programmlisteStopZeitMonat = Integer.parseInt(stopZeitSplit[1]);
            
            return checkDatumBetween(programmlisteStartZeitTag, programmlisteStartZeitMonat, programmlisteStopZeitTag, programmlisteStopZeitMonat);
            
        } catch (NumberFormatException e) {
            android.util.Log.w("TagesSuche", "startEndeDatumFromDatabase: NumberFormatException: " + e.getMessage());
            return false;
        } catch (Exception e) {
            android.util.Log.w("TagesSuche", "startEndeDatumFromDatabase: Exception: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Prüft .A Format (z.B. 23.03.2016, 7.A - wiederholt sich alle 7 Tage ab dem Startdatum).
     */
    private boolean startEndeVersion_A_FromDatabase(String startZeit, String stopZeit, int jahr, int monat, int tag) {
        try {
            String startZeitSplit[] = startZeit.split("\\.");
            String stopZeitSplit[] = stopZeit.split("\\.");
            
            if (startZeitSplit.length < 3 || stopZeitSplit.length < 1) {
                return false;
            }
            
            long wiederholungsTag = Integer.parseInt(stopZeitSplit[0]);
            
            int startTag = Integer.parseInt(startZeitSplit[0]);
            int startMonat = Integer.parseInt(startZeitSplit[1]) - 1; // Monat beginnt bei 0
            int startJahr = Integer.parseInt(startZeitSplit[2]);
            
            Date datumTabelle = new Date((startJahr - 1900), startMonat, startTag);
            Date checkDatum = new Date((jahr - 1900), monat, tag);
            
            long datumTabelleLongMs = datumTabelle.getTime();
            long datumCheckLongMs = checkDatum.getTime();
            
            long datumTabelleLongTage = datumTabelleLongMs / (1000 * 60 * 60 * 24);
            long datumCheckLongTage = datumCheckLongMs / (1000 * 60 * 60 * 24);
            
            long differenzTage = datumCheckLongTage - datumTabelleLongTage;
            
            if (datumCheckLongMs >= datumTabelleLongMs) {
                long datumDurchTageRest = differenzTage % wiederholungsTag;
                return (datumDurchTageRest == 0);
            }
            
            return false;
        } catch (Exception e) {
            android.util.Log.w("TagesSuche", "startEndeVersion_A_FromDatabase: Exception: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Prüft .T Format (z.B. 8.T bis 14.T - vom 8. bis zum 14. Tag des Monats).
     */
    private boolean startEndeVersion_T_FromDatabase(String startZeit, String stopZeit, int tag) {
        try {
            String[] startZeitSplit = startZeit.split("\\.");
            String[] stopZeitSplit = stopZeit.split("\\.");
            
            if (startZeitSplit.length < 1 || stopZeitSplit.length < 1) {
                return false;
            }
            
            int startTagInt = Integer.parseInt(startZeitSplit[0]);
            int stopTagInt = Integer.parseInt(stopZeitSplit[0]);
            
            return (tag >= startTagInt) && (tag <= stopTagInt);
        } catch (Exception e) {
            android.util.Log.w("TagesSuche", "startEndeVersion_T_FromDatabase: Exception: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Liefert true, wenn das gegebene Datum (jahr, monat, tag) am Startdatum oder danach liegt.
     * startZeit z. B. "28.2.2025" oder "28.2" (ohne Jahr = aktuelles Jahr annehmen). monat ist 0-basiert (Calendar).
     */
    private boolean isDatumAmStartdatumOderDanach(String startZeit, int jahr, int monat, int tag) {
        if (startZeit == null || startZeit.trim().isEmpty()) {
            return true;
        }
        try {
            String[] parts = startZeit.trim().split("\\.");
            if (parts.length < 2) return true;
            int startTag = Integer.parseInt(parts[0].trim());
            int startMonat1Based = Integer.parseInt(parts[1].trim());
            int startJahr = parts.length >= 3 ? Integer.parseInt(parts[2].trim()) : jahr;
            int monat1Based = monat + 1;
            if (jahr > startJahr) return true;
            if (jahr < startJahr) return false;
            if (monat1Based > startMonat1Based) return true;
            if (monat1Based < startMonat1Based) return false;
            return tag >= startTag;
        } catch (Exception e) {
            return true;
        }
    }

    /**
     * Prüft .W Format (1.W–4.W = 1.–4. Woche des Monats).
     * 1.W = Tag 1–7, 2.W = 8–14, 3.W = 15–21, 4.W = 22–31.
     */
    private boolean startEndeVersion_W_FromDatabase(String stopZeit, int tag) {
        try {
            String[] stopZeitSplit = stopZeit.split("\\.");
            if (stopZeitSplit.length < 2 || !"W".equals(stopZeitSplit[1])) {
                return false;
            }
            int woche = Integer.parseInt(stopZeitSplit[0]);
            if (woche < 1 || woche > 4) {
                return false;
            }
            int minTag = (woche - 1) * 7 + 1;
            int maxTag = (woche == 4) ? 31 : woche * 7;
            return (tag >= minTag) && (tag <= maxTag);
        } catch (Exception e) {
            android.util.Log.w("TagesSuche", "startEndeVersion_W_FromDatabase: Exception: " + e.getMessage());
            return false;
        }
    }

    /**
     * Formatiert das erste Programm für die Anzeige "Morgen um HH:MM  Name".
     * Rückgabe: "HH:MM  Name" (Zeit zuerst, dann Name/Dauer), damit UhrThread "Morgen um " + Rückgabe anzeigen kann.
     */
    public String formatMorgenUmProgramm(Programm programm, int stunde, int minute) {
        if (programm == null) return "";
        String zeit = pad(stunde) + ":" + pad(minute);
        String funktion = programm.getFunktion() != null ? programm.getFunktion() : "";
        String namePart;
        if ("Melodie".equals(funktion)) {
            namePart = programm.getMelodieName() != null ? programm.getMelodieName() : "";
            if (isInfoTextHiddenName(namePart)) namePart = "";
        } else if ("Ausgang".equals(funktion)) {
            String melName = programm.getMelodieName() != null ? programm.getMelodieName() : "";
            namePart = isInfoTextHiddenName(melName) ? "" : melName;
            String dauer = dauerNurWennZeitformat(programm.getDauerHeizung());
            if (!dauer.isEmpty()) namePart = namePart.isEmpty() ? dauer : (namePart + "  " + dauer);
        } else {
            namePart = dauerNurWennZeitformat(programm.getDauerHeizung());
        }
        return namePart.isEmpty() ? zeit : (zeit + "  " + namePart);
    }

    /**
     * Prüft, ob ein Name/Text in der Info-Zeile ausgeblendet werden soll (z. B. Notizen wie "alle 2 Wochen").
     */
    private static boolean isInfoTextHiddenName(String name) {
        if (name == null || name.trim().isEmpty()) return true;
        String s = name.trim();
        return "alle 2 wochen".equalsIgnoreCase(s);
    }

    /**
     * Gibt die Dauer-String nur zurück, wenn sie wie eine Zeit (hh:mm oder hh:mm:ss) aussieht.
     * Verhindert, dass Freitext aus Spalte D (z. B. "alle 2 Wochen") in der Anzeige "nächstes Programm" erscheint.
     */
    private static String dauerNurWennZeitformat(String dauerHeizung) {
        if (dauerHeizung == null || dauerHeizung.trim().isEmpty()) return "";
        String s = dauerHeizung.trim();
        if (s.matches("\\d{1,2}:\\d{2}(:\\d{2})?")) return s;
        return "";
    }

    public String getInfoText2(int zeile, int jahr, int monat, int tag, int stunde, int minute) {
        String infoText = "";

        // DB-Modus: Werte aus Programm holen (excelread2 ist null)
        if (excelread2 == null) {
            // Benutzerprogramm-Slot: Zeile 1000..1019 – unabhängig von filePathAndName2 (wie in zeileOk/getMelodieName)
            if (zeile >= BENUTZERPROGRAMM_ZEILE_OFFSET && zeile < BENUTZERPROGRAMM_ZEILE_MAX) {
                int slotIndex = zeile - BENUTZERPROGRAMM_ZEILE_OFFSET;
                if (StaticVariable.stunden != null && StaticVariable.minuten != null
                        && slotIndex < StaticVariable.stunden.size() && slotIndex < StaticVariable.minuten.size()) {
                    int h = StaticVariable.stunden.get(slotIndex);
                    int m = StaticVariable.minuten.get(slotIndex);
                    String melodieTemp = BenutzerMelodienActivity.getMelodieNameForSlot(slotIndex);
                    String startStr = pad(h) + ":" + pad(m);
                    infoText = (melodieTemp != null ? melodieTemp : "") + "  " + startStr;
                    return infoText;
                }
            }
            // Ggf. Programme nachladen, falls Map leer (z. B. wenn Konstruktor mit Pfad aufgerufen wurde, aber Tagtyp-Name anders war)
            String tagtypName = filePathAndName2 != null ? (filePathAndName2.contains("/") ? extractTagtypNameFromPath(filePathAndName2) : filePathAndName2) : null;
            if ((zeileToProgrammMap == null || zeileToProgrammMap.isEmpty()) && tagtypName != null && !tagtypName.isEmpty()) {
                loadProgrammeFromDatabase(tagtypName);
            }
            Programm programm = getProgrammByZeile(zeile);
            if (programm == null && programmeFromDatabase != null) {
                for (Programm p : programmeFromDatabase) {
                    if (p.getId() + 3 == zeile) {
                        programm = p;
                        break;
                    }
                }
            }
            if (programm != null) {
                String startZeit = programm.getStartzeit() != null ? programm.getStartzeit() : "";
                startZeit = normalizeStartzeitDisplay(startZeit); // "18:61" → "19:01" für Anzeige
                String funktionNameTemp = programm.getFunktion() != null ? programm.getFunktion() : "";
                String melodieTemp;
                if ("Melodie".equals(funktionNameTemp)) {
                    melodieTemp = programm.getMelodieName() != null ? programm.getMelodieName() : "";
                    if (isInfoTextHiddenName(melodieTemp)) melodieTemp = "";
                    if (!melodieTemp.isEmpty()) {
                        String beginnStMinSs = getBeginnTime2(melodieTemp, jahr, monat, tag, stunde, minute);
                        infoText = (beginnStMinSs != null ? beginnStMinSs : startZeit) + " → " + melodieTemp;
                    } else {
                        infoText = startZeit;
                    }
                } else if ("Ausgang".equals(funktionNameTemp)) {
                    String ausgangName = programm.getMelodieName() != null ? programm.getMelodieName().trim() : "";
                    if (isInfoTextHiddenName(ausgangName)) ausgangName = "";
                    String dauer = dauerNurWennZeitformat(programm.getDauerHeizung());
                    infoText = "Beginn " + startZeit + "  Ausgang: " + (ausgangName.isEmpty() ? "—" : ausgangName);
                    if (!dauer.isEmpty()) infoText += "  Dauer " + dauer;
                } else {
                    melodieTemp = dauerNurWennZeitformat(programm.getDauerHeizung());
                    infoText = startZeit + (melodieTemp.isEmpty() ? "" : "  " + melodieTemp);
                }
                return infoText;
            }
            // DB-Modus, aber kein Programm für diese Zeile – neutrale Anzeige statt „Melodie Startzeit Fehler“
            if (programmeFromDatabase != null && !programmeFromDatabase.isEmpty()) {
                infoText = StaticVariable.getUebersetzung(8) + " Zeile " + zeile;
            } else {
                infoText = (StaticVariable.getUebersetzung(8) != null ? StaticVariable.getUebersetzung(8) : "Beginn") + " —";
            }
            return infoText;
        }

        try {
            String startZeit = (excelread2.getCellString(SPALTE_A_STARTZEIT, zeile));
            String melodieTemp = (excelread2.getCellString(SPALTE_C_MELODIE_NAME, zeile));
            String funktionNameTemp = (excelread2.getCellString(SPALTE_B_FUNKTION, zeile));

            if ("Melodie".equals(funktionNameTemp)) {
                if (melodieTemp != null && !melodieTemp.trim().isEmpty() && startZeit != null && startZeit.matches("\\d{1,2}:\\d{2}")) {
                    String[] parts = startZeit.split(":");
                    if (parts.length >= 2) {
                        try {
                            int h = Integer.parseInt(parts[0].trim());
                            int m = Integer.parseInt(parts[1].trim());
                            String beginnStMinSs = getBeginnTime2(melodieTemp.trim(), jahr, monat, tag, h, m);
                            infoText = (beginnStMinSs != null ? beginnStMinSs : startZeit) + " → " + melodieTemp.trim();
                        } catch (NumberFormatException ignored) {
                            infoText = melodieTemp + "  " + startZeit;
                        }
                    } else {
                        infoText = melodieTemp + "  " + startZeit;
                    }
                } else {
                    infoText = melodieTemp + "  " + startZeit;
                }
            } else if ("Ausgang".equals(funktionNameTemp)) {
                String ausgangName = melodieTemp != null ? melodieTemp.trim() : "";
                String dauer = "";
                try {
                    String dauerCell = excelread2.getCellString(SPALTE_D_DAUER_MINUTEN_HEIZUNG, zeile);
                    dauer = dauerNurWennZeitformat(dauerCell);
                } catch (Exception ignored) { }
                infoText = "Beginn " + startZeit + "  Ausgang: " + (ausgangName.isEmpty() ? "—" : ausgangName);
                if (!dauer.isEmpty()) infoText += "  Dauer " + dauer;
            } else {
                infoText = startZeit + "  " + melodieTemp;
            }
        } catch (Exception e) {
            e.printStackTrace();
            infoText = (StaticVariable.getUebersetzung(8) != null ? StaticVariable.getUebersetzung(8) : "Beginn") + " —";
        }

        return infoText;
    }

    public String[] getMelodieName(int zeile) {
        String melodieNameTemp[] = {"", "", ""};

        // Benutzerprogramm-Slot 1000..1019 immer zuerst (unabhängig von excelread2/filePathAndName2),
        // sonst wird bei excelread2!=null Zeile 1002 als Excel-Zeile gelesen → Fehler / falsche Werte.
        if (zeile >= BENUTZERPROGRAMM_ZEILE_OFFSET && zeile < BENUTZERPROGRAMM_ZEILE_MAX) {
            int slotIndex = zeile - BENUTZERPROGRAMM_ZEILE_OFFSET;
            String name = BenutzerMelodienActivity.getMelodieNameForSlot(slotIndex);
            melodieNameTemp[0] = name != null ? name : "";
            melodieNameTemp[1] = "Melodie";
            melodieNameTemp[2] = "0";
            return melodieNameTemp;
        }

        // DB-Modus: Werte aus Programm holen
        if (excelread2 == null) {
            Programm programm = getProgrammByZeile(zeile);
            if (programm != null) {
                melodieNameTemp[1] = programm.getFunktion() != null ? programm.getFunktion() : "";
                melodieNameTemp[2] = programm.isImmer() ? "1" : "0";
                if ("Melodie".equals(melodieNameTemp[1])) {
                    String name = programm.getMelodieName();
                    if (name != null && name.length() > 4 && name.toLowerCase().endsWith(".xls"))
                        name = name.substring(0, name.length() - 4).trim();
                    melodieNameTemp[0] = name != null ? name : "";
                } else if ("Ausgang".equals(melodieNameTemp[1])) {
                    // Ausgang: [0] = Dauer in Sekunden (für HH:mm:ss), [1] = Ausgangs-Beschriftung (melodie_name)
                    String dauer = programm.getDauerHeizung();
                    if (dauer != null && !dauer.trim().isEmpty() && dauer.contains(":")) {
                        String[] parts = dauer.trim().split(":");
                        try {
                            int h = parts.length > 0 && !parts[0].isEmpty() ? Integer.parseInt(parts[0].trim()) : 0;
                            int m = parts.length > 1 && !parts[1].isEmpty() ? Integer.parseInt(parts[1].trim()) : 0;
                            int s = parts.length > 2 && !parts[2].isEmpty() ? Integer.parseInt(parts[2].trim()) : 0;
                            melodieNameTemp[0] = String.valueOf(h * 3600 + m * 60 + s);
                        } catch (NumberFormatException e) {
                            melodieNameTemp[0] = "60";
                        }
                    } else {
                        melodieNameTemp[0] = "60";
                    }
                    melodieNameTemp[1] = programm.getMelodieName() != null ? programm.getMelodieName() : "";
                } else {
                    // Heizung: Dauer "hh:mm:ss" in Minuten umrechnen
                    String dauer = programm.getDauerHeizung();
                    if (dauer != null && !dauer.trim().isEmpty() && dauer.contains(":")) {
                        String[] parts = dauer.trim().split(":");
                        try {
                            int h = parts.length > 0 && !parts[0].isEmpty() ? Integer.parseInt(parts[0].trim()) : 0;
                            int m = parts.length > 1 && !parts[1].isEmpty() ? Integer.parseInt(parts[1].trim()) : 0;
                            melodieNameTemp[0] = String.valueOf(h * 60 + m);
                        } catch (NumberFormatException e) {
                            melodieNameTemp[0] = "1";
                        }
                    } else {
                        melodieNameTemp[0] = "1";
                    }
                }
                return melodieNameTemp;
            }
        }
        
        try {

            melodieNameTemp[1] = (excelread2.getCellString(SPALTE_B_FUNKTION, zeile));

            melodieNameTemp[2] = (excelread2.getCellString(SPALTE_L_IMMER, zeile));
            //      melodieNameTemp[2] = (excelread2.getCellString(SPALTE_K_IMMER, zeile));

            if ((melodieNameTemp[1].equals("Melodie"))) // neu 9.8.16 wegen Dauer Min Spalte D
            {
                melodieNameTemp[0] = (excelread2.getCellString(SPALTE_C_MELODIE_NAME, zeile));

            } else {
                // vorher war melodieName[0] auch die Zeit 9.8.16
                //melodieNameTemp[0] = (excelread2.getCellString(SPALTE_D_DAUER_MINUTEN_HEIZUNG, zeile));
                //19.01.2017 auf Wunsch von Thomas, aenderung von Minute auf hh:mm:ss
                //wegen interner verarbeitung auf Minuten umrechnen:
                String dauer_hhmmss = (excelread2.getCellString(SPALTE_D_DAUER_MINUTEN_HEIZUNG, zeile));
                //Log.e("getMelodieName" , "zeile=" + zeile) ;
                //Log.e("getMelodieName" , "dauer_hhmmss=" + dauer_hhmmss) ;

                String[] dauerSplit = {"00", "00" , "00"};

                if (dauer_hhmmss.contains(":"))
                {
                    dauerSplit = dauer_hhmmss.split(":");
                    if(dauerSplit[0].equals(""))
                    {
                        dauerSplit[0]="0" ;
                    }
                    int minutenIntTemp = (Integer.parseInt(dauerSplit[0]) * 60)  ;
                    minutenIntTemp = minutenIntTemp + (Integer.parseInt(dauerSplit[1]) ) ;
                    melodieNameTemp[0] = Integer.toString(minutenIntTemp) ;
                }
                else
                {
                    melodieNameTemp[0] = "1" ;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        //Log.e("getMelodieName" , "melodieNameTemp=" + melodieNameTemp[0]) ;
        return melodieNameTemp;
    }

    //public long getBeginVorschwingenMs(String pathAndFileNameMelodie, int jahr, int monat, int tag, int stunde, int minute) {
      //  long vorlaufZeitTemp = (getErsteVorlaufzeitMs(pathAndFileNameMelodie));
        //Date dateStartMelodie = new Date(jahr, monat, tag, stunde, minute);
      //  long msStartMelode = dateStartMelodie.getTime();

       // long timeStartVorlauf = msStartMelode - vorlaufZeitTemp;

       // return timeStartVorlauf;

   // }

    public void setHeizungStartSekunden(String funktionName, String laufzeitMinuten, int jahr, int monat, int tag, int stunde, int minute) {
        StaticVariable.nextHeizungFunktionsName = funktionName;
        StaticVariable.heizungRelaisNumber3 = getRelaisNumber(funktionName);
        if (laufzeitMinuten == null || laufzeitMinuten.trim().isEmpty()) {
            laufzeitMinuten = "1";
        }
        StaticVariable.nextHeizungLaufzeitMinutenString = laufzeitMinuten;

        // Bei Ausgang: laufzeitMinuten aus getMelodieName als Sekunden (z. B. 70 für 00:01:10)
        long laufzeitSekunden;
        try {
            laufzeitSekunden = Long.parseLong(laufzeitMinuten.trim());
            if (laufzeitSekunden < 0) laufzeitSekunden = 60;
        } catch (NumberFormatException e) {
            laufzeitSekunden = 60;
        }

        Date startZeit = new Date((jahr - 1900), monat, tag, stunde, minute, 0);

        long startZeitMs = startZeit.getTime();
        long startZeitMsDurchTausend = startZeitMs / 1000;
        long stopZeitMsDurchTausend = startZeitMsDurchTausend + laufzeitSekunden;

        StaticVariable.nextHeizungStartSekunden = startZeitMsDurchTausend;

        SaveAndLoadHeizung saveAndLoadHeizung = new SaveAndLoadHeizung();
        if (StaticVariable.heizungRelaisNumber3 > 0) {
            saveAndLoadHeizung.saveHeizung(startZeitMsDurchTausend, stopZeitMsDurchTausend, StaticVariable.heizungRelaisNumber3);
        }
    }

    public static int getRelaisNumber(String funktionNameString) {
        int relaisNummerTemp = -1;
        if (funktionNameString == null || funktionNameString.trim().isEmpty()) return -1;
        String suchName = funktionNameString.trim();
        if (TurmtechnikActivity.turmtechnikContext != null) {
            try {
                java.util.List<PlatinenDatabaseHelper.BeschriftungTastenRow> rows = PlatinenDatabaseHelper.getInstance(TurmtechnikActivity.turmtechnikContext).getBeschriftungTasten();
                for (PlatinenDatabaseHelper.BeschriftungTastenRow r : rows) {
                    String nameTemp = r.c2 != null ? r.c2.trim() : "";
                    if (nameTemp.isEmpty() || !nameTemp.equalsIgnoreCase(suchName)) continue;
                    int platineNummer = 1;
                    try {
                        if (r.c5 != null && !r.c5.trim().isEmpty()) platineNummer = (int) Long.parseLong(r.c5.trim());
                    } catch (Exception e) { }
                    String relaisNumberString = r.c3 != null ? r.c3.trim() : "-1";
                    try {
                        relaisNummerTemp = Integer.parseInt(relaisNumberString);
                        relaisNummerTemp = relaisNummerTemp + ((platineNummer - 1) * 32);
                        return relaisNummerTemp;
                    } catch (NumberFormatException e) { }
                }
            } catch (Exception e) {
                // Fallback zu Excel
            }
        }
        ExcelRead excelread = null;
        String beschriftungTastenFilename = TurmtechnikActivity.beschriftungTastenFileString;
        try {
            excelread = new ExcelRead();
            excelread.openXls(beschriftungTastenFilename);
            int zeilen = excelread.getCellZeilen();
            for (int i = 1; i < zeilen; i++) {
                String nameTemp;
                try { nameTemp = excelread.getCellString(2, i); } catch (Exception e) { nameTemp = ""; }
                if (nameTemp != null && !nameTemp.trim().isEmpty() && nameTemp.trim().equalsIgnoreCase(suchName)) {
                    int platineNummer = 1;
                    try { platineNummer = (int) Long.parseLong(excelread.getCellString(5, i)); } catch (Exception e) { }
                    String relaisNumberString = "-1";
                    try { relaisNumberString = excelread.getCellString(3, i); } catch (Exception e) { }
                    try {
                        relaisNummerTemp = Integer.parseInt(relaisNumberString);
                        relaisNummerTemp = relaisNummerTemp + ((platineNummer - 1) * 32);
                    } catch (Exception e) { }
                    break;
                }
            }
        } catch (BiffException | IOException e) {
            // Kein LogExcelError: Bei DB-Betrieb fehlt die Excel-Datei oft; würde LogError.txt fluten (spalte -1 zeile -1)
            android.util.Log.d("TagesSuche", "Beschriftung-Tasten.xls nicht gelesen (Relais-Suche Excel-Fallback): " + e.getMessage());
        } finally {
            if (excelread != null) try { excelread.closeWorkbook(); } catch (Exception e) { }
        }
        return relaisNummerTemp;
    }

    public void setVorschwingZeitenZurMelodie(String melodieName, String pathAndFileNameMelodie, int jahr, int monat, int tag, int stunde, int minute) {
        // Melodiename aus Pfad extrahieren (Pfad ist .../Melodien/Name oder .../Melodien/Name.xls)
        String nameFuerDb = pathAndFileNameMelodie;
        if (nameFuerDb != null && nameFuerDb.contains("/")) {
            nameFuerDb = nameFuerDb.substring(nameFuerDb.lastIndexOf('/') + 1);
        }
        if (nameFuerDb != null && nameFuerDb.toLowerCase().endsWith(".xls")) {
            nameFuerDb = nameFuerDb.substring(0, nameFuerDb.length() - 4);
        }
        if (nameFuerDb == null) nameFuerDb = "";
        // Trim und Tabs/mehrfache Leerzeichen entfernen, damit Abgleich mit DB (z. B. aus Dropdown) funktioniert
        nameFuerDb = nameFuerDb.trim().replaceAll("\\s+", " ").trim();

        Date startZeit = new Date((jahr - 1900), monat, tag, stunde, minute, 0);
        long startZeitMs = startZeit.getTime();
        long startZeitMsDurchTausend = startZeitMs / 1000;

        StaticVariable.vorschwingenStartzeitenMotorRelais.clear();

        // Vorschwingen bei Bedarf nachladen (Sofort/Manuel/Benutzer), falls beim App-Start noch nicht geladen
        if (StaticVariable.vorschwingZeitSekunden.isEmpty()) {
            android.content.Context ctx = TurmtechnikActivity.turmtechnikContext;
            if (ctx != null) TurmtechnikActivity.ensureVorschwingenLoaded(ctx);
        }

        // Zuerst aus Datenbank: Vorlauf und Pfad (Melodien + Vorschwingen-Config sind in der DB)
        // getMelodieByNormalizedName: findet auch bei abweichenden Leerzeichen (z. B. "1-7   2 Min" vs "1-7 2 Min")
        android.content.Context context = TurmtechnikActivity.turmtechnikContext;
        if (context != null && !nameFuerDb.isEmpty()) {
            try {
                PlatinenDatabaseHelper dbHelper = PlatinenDatabaseHelper.getInstance(context);
                PlatinenDatabaseHelper.Melodie melodie = dbHelper.getMelodieByNormalizedName(nameFuerDb);
                if (melodie != null) {
                    long vorlaufSekunden = melodie.vorlaufMinuten * 60L;
                    long nextStart = startZeitMsDurchTausend - vorlaufSekunden;
                    long nowSec = System.currentTimeMillis() / 1000;
                    // Wenn Startzeit in der Vergangenheit (z. B. „jetzt“), sofort beim nächsten Tick starten
                    if (nextStart <= nowSec) {
                        nextStart = nowSec;
                    }
                    StaticVariable.nextMelodieStartSekunden2 = nextStart;
                    // Pfad ohne .xls – Melodie kommt aus DB, MelodieThreadNew findet sie per Namen
                    StaticVariable.pathAndFileNameNextMelodie = TurmtechnikActivity.sdCardPath + "/Turmtechnik/Melodien/" + melodie.name;
                    StaticVariable.nameNextMelodie = melodieName;
                    // Vorschwing-Startzeiten: pro Melodie-Relais j und pro Einschaltpunkt (Zeile mit Kloeppel=1)
                    // Einschaltpunkt = startZeitMsDurchTausend - vorlauf + timeOffset; Vorschwing-Ein = Einschaltpunkt - vorschwingZeitSekunden.get(j)
                    java.util.List<PlatinenDatabaseHelper.MelodieZeile> dbZeilen = dbHelper.getMelodieZeilen(melodie.id);
                    int vorschwingzeiten = StaticVariable.vorschwingZeitSekunden.size();
                    // Zeitoffset pro Zeile: aus beginnZeit; falls 0, aus kumulierter Dauer vorheriger Zeilen (damit jede Zeile eigenen Einschaltpunkt hat)
                    int[] timeOffsetProZeile = null;
                    if (dbZeilen != null && !dbZeilen.isEmpty()) {
                        timeOffsetProZeile = new int[dbZeilen.size()];
                        for (int rowIdx = 0; rowIdx < dbZeilen.size(); rowIdx++) {
                            PlatinenDatabaseHelper.MelodieZeile z = dbZeilen.get(rowIdx);
                            int off = parseBeginnZeitToSekunden(z.beginnZeit);
                            if (off == 0 && rowIdx > 0) {
                                for (int r = 0; r < rowIdx; r++) {
                                    Double d = dbZeilen.get(r).dauerSekunden;
                                    off += (d != null ? (int) Math.round(d) : 0);
                                }
                            }
                            timeOffsetProZeile[rowIdx] = off;
                        }
                    }
                    for (int j = 0; j < vorschwingzeiten; j++) {
                        long vorschwingSekundenJ = StaticVariable.getVorschwingSekundenForColumn(j);
                        android.util.Log.w("ProgrammStart", "Vorschwingen Spalte j=" + j + " Relais=" + (j < StaticVariable.laeutenKloeppelRelais.size() ? StaticVariable.laeutenKloeppelRelais.get(j) : "?") + " zeitSekunden=" + vorschwingSekundenJ);
                        ArrayList<Long> startZeitTemp = new ArrayList<Long>();
                        if (dbZeilen != null && timeOffsetProZeile != null && j < getKloeppelCount()) {
                            for (int rowIdx = 0; rowIdx < dbZeilen.size(); rowIdx++) {
                                PlatinenDatabaseHelper.MelodieZeile zeile = dbZeilen.get(rowIdx);
                                if (!"1".equals(getKloeppelForSpalte(zeile, j))) continue;
                                int timeOffsetSekunden = timeOffsetProZeile[rowIdx];
                                long einschaltpunktSekunden = startZeitMsDurchTausend - vorlaufSekunden + timeOffsetSekunden;
                                long startZeitSekunden = einschaltpunktSekunden - vorschwingSekundenJ;
                                startZeitTemp.add(startZeitSekunden);
                            }
                        }
                        StaticVariable.vorschwingenStartzeitenMotorRelais.add(startZeitTemp);
                    }
                    return;
                }
            } catch (Exception e) {
                android.util.Log.w("TagesSuche", "setVorschwingZeitenZurMelodie: DB-Lesen fehlgeschlagen, Fallback Excel: " + e.getMessage());
            }
        }

        // Fallback: Excel (wenn Melodie nicht in DB oder Context fehlt)
        String pathMitXls = (pathAndFileNameMelodie != null && !pathAndFileNameMelodie.toLowerCase().endsWith(".xls"))
                ? (pathAndFileNameMelodie + ".xls") : pathAndFileNameMelodie;

        ExcelRead excelRead = new ExcelRead();
        boolean excelGeoeffnet = false;
        try {
            excelRead.openXls(pathMitXls);
            excelGeoeffnet = true;
        } catch (BiffException e) {
            android.util.Log.e("TagesSuche", "Melodien-XLS konnte nicht gelesen werden (Format/Beschädigung): " + pathMitXls + " – Melodie in DB importieren oder Datei prüfen.");
            new LogExcelError(-1, -1, pathMitXls, 0, "TagesSuche", 227);
            e.printStackTrace();
        } catch (IOException e) {
            android.util.Log.e("TagesSuche", "Melodien-XLS konnte nicht geöffnet werden (Datei fehlt?): " + pathMitXls + " – Melodie in DB importieren oder Pfad prüfen.");
            new LogExcelError(-1, -1, pathMitXls, 0, "TagesSuche", 231);
            e.printStackTrace();
        }
        if (!excelGeoeffnet) {
            return; // Nicht mit nicht geöffneter Datei weiterarbeiten (vermeidet Folgefehler)
        }

        int zeilen = excelRead.getCellZeilen();
        int vorschwingzeiten = StaticVariable.vorschwingZeitSekunden.size();

        Long vorlaufVonDerMelodie = 0L;
        try {
            String vorlaufAusMelodieString = excelRead.getCellString(1, 1);
            vorlaufVonDerMelodie = (Long.parseLong(vorlaufAusMelodieString)) * 60L; // Minuten --> Sekunden
        } catch (Exception e) {
        }

        StaticVariable.nextMelodieStartSekunden2 = (startZeitMsDurchTausend - vorlaufVonDerMelodie);
        StaticVariable.pathAndFileNameNextMelodie = (pathMitXls);
        StaticVariable.nameNextMelodie = melodieName;

        for (int j = 0; j < vorschwingzeiten; j++) {
            ArrayList<Long> startZeitTemp = new ArrayList<Long>();
            long vorschwingSekundenJ = StaticVariable.getVorschwingSekundenForColumn(j);
            for (int i = 3; i < zeilen; i++) {
                String nullEinsString = "";
                String timeOffsetString = "0";
                try {
                    nullEinsString = excelRead.getCellString(4 + j, i);
                    timeOffsetString = excelRead.getCellString(0, i);
                } catch (Exception e) {
                }
                if (nullEinsString.equals("1")) {
                    String[] beginnString = timeOffsetString.split(":");
                    int minuten = beginnString.length > 1 ? Integer.parseInt(beginnString[1]) : 0;
                    int sekunden = beginnString.length > 2 ? Integer.parseInt(beginnString[2]) : 0;
                    int timeOffsetSekunden = (minuten * 60) + sekunden;
                    // Pro Melodie-Relais j: Vorschwing-Ein = Einschaltpunkt dieses Relais − Vorschwingzeit
                    long einschaltpunktSekunden = startZeitMsDurchTausend - vorlaufVonDerMelodie + timeOffsetSekunden;
                    Long startZeitSekunden = einschaltpunktSekunden - vorschwingSekundenJ;
                    startZeitTemp.add(startZeitSekunden);
                }
            }
            android.util.Log.w("ProgrammStart", "Vorschwingen (Excel) Spalte j=" + j + " Relais=" + (j < StaticVariable.laeutenKloeppelRelais.size() ? StaticVariable.laeutenKloeppelRelais.get(j) : "?") + " zeitSekunden=" + vorschwingSekundenJ);
            StaticVariable.vorschwingenStartzeitenMotorRelais.add(startZeitTemp);
        }
    }

    /**
     * Parst beginn_zeit (z. B. "00:01:30" oder "01:30" oder "00:00.00") in Sekunden.
     * Wird für Vorschwing-Startzeiten aus DB verwendet (vorschwingen-config + melodie_zeilen).
     */
    private static int parseBeginnZeitToSekunden(String beginnZeit) {
        if (beginnZeit == null || beginnZeit.trim().isEmpty()) return 0;
        String s = beginnZeit.trim().replace('.', ':');
        String[] parts = s.split(":");
        try {
            if (parts.length >= 3) {
                int h = Integer.parseInt(parts[0].trim());
                int m = Integer.parseInt(parts[1].trim());
                int sec = Integer.parseInt(parts[2].trim());
                return h * 3600 + m * 60 + sec;
            }
            if (parts.length == 2) {
                int m = Integer.parseInt(parts[0].trim());
                int sec = Integer.parseInt(parts[1].trim());
                return m * 60 + sec;
            }
            if (parts.length == 1) return Integer.parseInt(parts[0].trim());
        } catch (Exception e) {
            return 0;
        }
        return 0;
    }

    private void showBeginnZeiten() {
        int beginVorschwingzeitenSize = StaticVariable.vorschwingenStartzeitenMotorRelais.size();
        ArrayList<Long> motorStartZeiten = new ArrayList<Long>();

        for (int j = 0; j < beginVorschwingzeitenSize; j++) {
            motorStartZeiten = StaticVariable.vorschwingenStartzeitenMotorRelais.get(j);

            for (int i = 0; i < motorStartZeiten.size(); i++) {

                long vorschwingTemp = (motorStartZeiten.get(i)) * 1000;

                //Date starttime = new Date(vorschwingTemp);

                //DateFormat sdf = new SimpleDateFormat("yyyy:MM:dd:HH:mm:ss");

                //String stringVorschwingString = sdf.format(starttime);

                ////Log.e("showBeginnZeiten", "=" + vorschwingTemp + "=" + convertMsToTimeString(vorschwingTemp) );
            }
        }
    }

    public String convertMsToTimeString(long ms) {
        Date date = new Date(ms);

        DateFormat sdf = new SimpleDateFormat("yyyy:MM:dd:HH:mm:ss");

        String timeString = sdf.format(date);

        return timeString;
    }

    /** Anzahl Kloeppel-Spalten (A=0 .. P=15) in MelodieZeile. */
    private static int getKloeppelCount() {
        return 16;
    }

    /** Liefert Kloeppel-Wert (z. B. "0" oder "1") für Spalte j (0=A, 1=B, … 15=P) aus einer MelodieZeile. */
    private static String getKloeppelForSpalte(PlatinenDatabaseHelper.MelodieZeile zeile, int j) {
        if (zeile == null || j < 0 || j >= 16) return "";
        String v = null;
        switch (j) {
            case 0: v = zeile.kloeppelA; break;
            case 1: v = zeile.kloeppelB; break;
            case 2: v = zeile.kloeppelC; break;
            case 3: v = zeile.kloeppelD; break;
            case 4: v = zeile.kloeppelE; break;
            case 5: v = zeile.kloeppelF; break;
            case 6: v = zeile.kloeppelG; break;
            case 7: v = zeile.kloeppelH; break;
            case 8: v = zeile.kloeppelI; break;
            case 9: v = zeile.kloeppelJ; break;
            case 10: v = zeile.kloeppelK; break;
            case 11: v = zeile.kloeppelL; break;
            case 12: v = zeile.kloeppelM; break;
            case 13: v = zeile.kloeppelN; break;
            case 14: v = zeile.kloeppelO; break;
            case 15: v = zeile.kloeppelP; break;
            default: return "";
        }
        return (v != null ? v.trim() : "");
    }

    //public void clrVorschwingZeitenZurMelodie()
    //{

    //    int vorschwingzeiten = StaticVariable.vorschwingZeitSekunden.size() ;

    //    StaticVariable.beginnVorschwingenStartinMsDurchTausend.clear();

    //    for(int i = 0 ; i < vorschwingzeiten ; i++)
    //    {
    //         StaticVariable.beginnVorschwingenStartinMsDurchTausend.add(0L);
    //    }
    // }

    private boolean[] getMelodieKloeppelVerwendet(String pathAndFileNameMelodie) {
        boolean[] verwendetTemp = {false, false, false, false, false, false, false, false};

        ////Log.e("getMeloieKloeppelVerwendet" , "path=" + pathAndFileNameMelodie) ;

        ExcelRead excelRead = new ExcelRead();

        try {
            excelRead.openXls(pathAndFileNameMelodie);
        } catch (BiffException e) {
            //////Log.e("getMeloieKloeppelVerwendet" , "BiffException") ;
            new LogExcelError(-1, -1, pathAndFileNameMelodie, 0, "TagesSuche", 253);
            e.printStackTrace();
        } catch (IOException e) {
            //////Log.e("getMeloieKloeppelVerwendet" , "IOException") ;
            new LogExcelError(-1, -1, pathAndFileNameMelodie, 0, "TagesSuche", 257);
            e.printStackTrace();
        }

        int zeilen = excelRead.getCellZeilen();
        int spalten = excelRead.getCellSpalten();
        spalten = Math.min(spalten, 4 + 8);

        String temp = null;

        for (int j = 0; j < spalten; j++) {
            for (int i = 3; i < zeilen; i++) {
                try {
                    temp = excelRead.getCellString(4 + j, i);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                if (temp.equals("1")) {
                    verwendetTemp[j] = true;
                    break;
                }
            }
        }

        excelRead.closeWorkbook();

        return verwendetTemp;
    }

    private String[] sucheMelodieString() {
        android.util.Log.d("TagesSuche", "sucheMelodieString: Start, aktuelleZeitZeilenListe.size()=" + aktuelleZeitZeilenListe.size());
        String[] melodieString = {"", "", "" }; //[0] = melodieString, [1] , = funktionString
        //  [2] = startZeitString

        int blockOffset = -1;


        //for (int i = 0; i < aktuelleZeitZeilenListe.size(); i++)
        //{
        //Log.i("start Zeit " , "index="+aktuelleZeitZeilenListe.elementAt(i) );
        //}

        // jetzt auf prioritaet untersuchen
        //Log.e("vor", "prioritaet");

        for (int i = 9; i > 0; i--) {
            int tempIndex = getIndexPrioritaet(i);
            //Log.e("prioritaet", "" + tempIndex);
            // wenn prioritaet gefunden, schauen ob passt
            if (tempIndex != -1) {
                int zeile = aktuelleZeitZeilenListe.elementAt(tempIndex);
                android.util.Log.d("TagesSuche", "sucheMelodieString: Prüfe Zeile " + zeile + " mit Priorität " + i);
                if (zeileOk(zeile))  // passt alles bei der Prioritaetszeile? // oder zeile 0
                {
                    blockOffset = zeile;
                    android.util.Log.d("TagesSuche", "sucheMelodieString: Zeile " + zeile + " OK, hole MelodieString");
                    //Log.d("akt. block Nr.:" , "" + blockNummer);

                    StaticVariable.fileIndexForEditorTemp = blockOffset ;
                    return getMelodieString(blockOffset);  //
                }
                // wenn nicht aus startZeitenIndex entfernen
                else {
                    android.util.Log.d("TagesSuche", "sucheMelodieString: Zeile " + zeile + " NICHT OK (zeileOk=false), entferne aus Liste");
                    aktuelleZeitZeilenListe.remove(tempIndex);
                }
            }
        }

        // und liste von oben nach unten suchen ob alles passt...
        // wochentag, verknuepfte Taste etc...
        blockOffset = -1;

        if (aktuelleZeitZeilenListe.size() != 0) {
            //Log.i("startZeitenIndex" , "size=" + aktuelleZeitZeilenListe.size()) ;
            for (int i = 0; i < aktuelleZeitZeilenListe.size(); i++) {
                int zeile = aktuelleZeitZeilenListe.elementAt(i);
                android.util.Log.d("TagesSuche", "sucheMelodieString: Prüfe Zeile " + zeile + " (ohne Priorität)");
                //Log.d("startZeitenIndex" , "i=" + i + "elementAt(i) = " + aktuelleZeitZeilenListe.elementAt(i));
                if (zeileOk(zeile)) {
                    android.util.Log.d("TagesSuche", "sucheMelodieString: Zeile " + zeile + " OK, hole MelodieString");
                    //Log.i("startZeitenIndex" , "elementAt(i) = " + aktuelleZeitZeilenListe.elementAt(i));
                    blockOffset = zeile;
                    StaticVariable.fileIndexForEditorTemp = blockOffset ;
                    melodieString = (getMelodieString(blockOffset));

                    return melodieString;
                } else {
                    android.util.Log.d("TagesSuche", "sucheMelodieString: Zeile " + zeile + " NICHT OK (zeileOk=false)");
                }
            }
        }
        android.util.Log.d("TagesSuche", "sucheMelodieString: Keine passende Zeile gefunden, return leeres Array");
        return melodieString;
    } // ende sucheMelodieString


    public String getBeginnTime2(String melodieName, int jahr, int monat, int tag, int stunde, int minute) {
        String stringTemp = "00:00:00";
        long vorlaufZeitTemp = 0;

        ////Log.e("getBeginnTime" , "Anfang") ;
        vorlaufZeitTemp = (getErsteVorlaufzeitMs(melodieName));
        ////Log.e("getBeginnTime" , "vorlaufZeitTemp=" + vorlaufZeitTemp) ;
        ////Log.e("getBeginnTime" , "melodieName=" + melodieName) ;
        ////Log.e("getBeginnTime" , "stunde=" + stunde) ;
        ////Log.e("getBeginnTime" , "minute=" + minute) ;

        // jetzt Startzeit String berechnen (Date-Konstruktor erwartet Jahr − 1900)
        Date dateStartMelodie = new Date(jahr - 1900, monat, tag, stunde, minute);
        long msStartMelode = dateStartMelodie.getTime();

        timeStartVorlauf2 = msStartMelode - vorlaufZeitTemp;

        Date sDate = new Date(timeStartVorlauf2);

        DateFormat sdf = new SimpleDateFormat("HH:mm:ss");

        stringTemp = sdf.format(sDate);

        ////Log.e("getBeginnTime" , "return=" + stringTemp) ;

        return stringTemp;
    }

    /**
     * Liefert für die Infotext-Zeile den Beginn als "St:Min = Startzeit - Vorlauf - Vorschwingen" mit konkreten Werten,
     * z. B. "12:30 = 12:45 - 10 Min - 5 s". Leer-String wenn keine Melodie/Startzeit.
     */
    public String getBeginnFormulaPrefix(String melodieName, int jahr, int monat, int tag, int stunde, int minute) {
        if (melodieName == null || melodieName.trim().isEmpty()) return "";
        String startStr = pad(stunde) + ":" + pad(minute);
        String beginnStr = getBeginnTime2(melodieName.trim(), jahr, monat, tag, stunde, minute);
        if (beginnStr == null || beginnStr.isEmpty()) return "";
        android.content.Context ctx = TurmtechnikActivity.turmtechnikContext;
        if (ctx == null) return "";
        try {
            PlatinenDatabaseHelper dbHelper = PlatinenDatabaseHelper.getInstance(ctx);
            int vorlaufMinuten = 0;
            PlatinenDatabaseHelper.Melodie melodie = dbHelper.getMelodieByNormalizedName(melodieName.trim());
            if (melodie != null) vorlaufMinuten = melodie.vorlaufMinuten;
            int maxVorschwingenSekunden = 0;
            for (PlatinenDatabaseHelper.VorschwingenConfig v : dbHelper.getAllVorschwingen()) {
                if (v.aktiv && v.zeitSekunden > maxVorschwingenSekunden) maxVorschwingenSekunden = v.zeitSekunden;
            }
            return "St:Min = Startzeit - Vorlauf - Vorschwingen: " + beginnStr + " = " + startStr + " - " + vorlaufMinuten + " Min - " + maxVorschwingenSekunden + " s  ";
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * Liefert für die Infotext-Anzeige den Beginn-Bereich als "Startzeit HH:mm, Vorlauf X Min, Vorschwingen Y s, Beginn HH:mm:ss".
     * So kann der Nutzer die Berechnung (Startzeit − Vorlauf − Vorschwingen → Beginn) prüfen.
     */
    public String getBeginnDetailString(String melodieName, int jahr, int monat, int tag, int stunde, int minute) {
        String startStr = pad(stunde) + ":" + pad(minute);
        String beginnStr = (melodieName != null && !melodieName.trim().isEmpty())
                ? getBeginnTime2(melodieName.trim(), jahr, monat, tag, stunde, minute) : startStr;
        android.content.Context ctx = TurmtechnikActivity.turmtechnikContext;
        if (ctx == null || melodieName == null || melodieName.trim().isEmpty()) {
            return (StaticVariable.getUebersetzung(8) != null ? StaticVariable.getUebersetzung(8) : "Beginn") + " " + beginnStr;
        }
        try {
            PlatinenDatabaseHelper dbHelper = PlatinenDatabaseHelper.getInstance(ctx);
            int vorlaufMinuten = 0;
            PlatinenDatabaseHelper.Melodie melodie = dbHelper.getMelodieByNormalizedName(melodieName.trim());
            if (melodie != null) vorlaufMinuten = melodie.vorlaufMinuten;
            int maxVorschwingenSekunden = 0;
            for (PlatinenDatabaseHelper.VorschwingenConfig v : dbHelper.getAllVorschwingen()) {
                if (v.aktiv && v.zeitSekunden > maxVorschwingenSekunden) maxVorschwingenSekunden = v.zeitSekunden;
            }
            return "Startzeit " + startStr + ", Vorlauf " + vorlaufMinuten + " Min, Vorschwingen " + maxVorschwingenSekunden + " s, " + (StaticVariable.getUebersetzung(8) != null ? StaticVariable.getUebersetzung(8) : "Beginn") + " " + beginnStr;
        } catch (Exception e) {
            return (StaticVariable.getUebersetzung(8) != null ? StaticVariable.getUebersetzung(8) : "Beginn") + " " + beginnStr;
        }
    }

    //public long getBeginnTime2Ms ()
    //{

    //    return timeStartVorlauf2 ;
    //}

    private String[] getMelodieString(int zeile) {
        String[] returnString = {"", "", ""}; // [0] = melodieString, [1] = Funktion/NameString ;
        // [2] = startZeitString
        try {

            returnString[1] = excelread2.getCellString(SPALTE_B_FUNKTION, zeile);
            if(returnString[1].equals("Melodie"))
            {
                returnString[0] = excelread2.getCellString(SPALTE_C_MELODIE_NAME, zeile);
            }
            else
            {
                returnString[0] = excelread2.getCellString(SPALTE_D_DAUER_MINUTEN_HEIZUNG, zeile) ;
            }
            returnString[2] = excelread2.getCellString(SPALTE_A_STARTZEIT, zeile);

            // braucht man nicht mehr 16.1.2015:
            //        + " --> " ;
            //tempString += excelread2.getCellString(18, offset) ;

            ////Log.e("getMelodieString" , "zeile=" + zeile + " name=" + returnString[0]) ;
            return returnString;

        } catch (ArrayIndexOutOfBoundsException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            new LogExcelError(11, zeile, filePathAndName2, 0, sourceFileName2, 178);
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            new LogExcelError(11, zeile, filePathAndName2, 0, sourceFileName2, 182);
        }
        return returnString;
    }


    public String getFunktionName(int zeile) {
        String returnString = "";
        try {
            returnString = excelread2.getCellString(SPALTE_B_FUNKTION, zeile);
            //if ((returnString.equals("Melodie"))) {
            //    returnString = "";
            //}


        } catch (Exception e) {
            new LogExcelError(11, zeile, filePathAndName2, 0, sourceFileName2, 363);

        }
        return returnString;
    }

    private long getLaengsteVorlaufzeitMs(String melodieString) {
        String stringEnde = melodieString.substring(melodieString.length() - 4);
        if (!(stringEnde.equals(".xls"))) {
            melodieString += ".xls";
        }
        String pathAndFileNameMelodie = TurmtechnikActivity.sdCardPath + "/Turmtechnik/Melodien/" + melodieString;


        ////Log.e("pathAndFileNameMelodie" , "=" + pathAndFileNameMelodie) ;

        ArrayList<Integer> motorRelaisVerwendet = new ArrayList<Integer>();

        motorRelaisVerwendet = getMotorRelaisVerwendet(pathAndFileNameMelodie);
        //Log.e("motor", "relaisVerwendetSize=" + motorRelaisVerwendet.size());
        for (int i = 0; i < motorRelaisVerwendet.size(); i++) {
            //Log.e("motor", "relais=" + motorRelaisVerwendet.get(i));
        }

        long laengsteVorlaufZeit = berechneLaengsteVorlaufzeit(motorRelaisVerwendet);

        String vorlaufMelodieString = "0";

        ExcelRead excelRead = new ExcelRead();

        try {
            excelRead.openXls(pathAndFileNameMelodie);
            vorlaufMelodieString = excelRead.getCellString(1, 1);

        } catch (BiffException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }

        long echteVorlaufZeit = laengsteVorlaufZeit + (Long.parseLong(vorlaufMelodieString) * 60 * 1000);

        return echteVorlaufZeit;

    }

    /**
     * Liefert die Zeit in ms, die vor der Programstartzeit abgezogen wird → Beginn = Programstart − Rückgabe.
     * Vorlauf und Vorschwingen sind getrennt zu sehen:
     * - Vorlauf = Melodien-Vorlauf (vorlauf_minuten): wie viele Minuten vor Start „Beginn“ ist (Thread-Start, Anzeige).
     * - Vorschwingen = pro Glocke in Sekunden (vorschwingen_config): Motor X Sek. vor jedem Klöppel-Schlag. Siehe checkVorschwingen / setVorschwingZeitenZurMelodie.
     * Rückgabe = max(Melodien-Vorlauf, längste Vorschwingzeit in ms), damit der Motor rechtzeitig ein geht.
     */
    public long getErsteVorlaufzeitMs(String melodieString) {
        if (melodieString == null || melodieString.trim().isEmpty()) return 0;
        melodieString = melodieString.trim();
        String nameFuerDb = melodieString;
        if (nameFuerDb != null && nameFuerDb.toLowerCase().endsWith(".xls")) {
            nameFuerDb = nameFuerDb.substring(0, nameFuerDb.length() - 4);
        }
        if (nameFuerDb == null) nameFuerDb = "";
        nameFuerDb = nameFuerDb.trim().replaceAll("\\s+", " ").trim();

        android.content.Context context = TurmtechnikActivity.turmtechnikContext;
        if (context != null && StaticVariable.vorschwingZeitSekunden.isEmpty()) {
            TurmtechnikActivity.ensureVorschwingenLoaded(context);
        }
        long laengsteVorschwingenMs = 0;
        for (int i = 0; i < StaticVariable.vorschwingZeitSekunden.size(); i++) {
            long t = StaticVariable.getVorschwingSekundenForColumn(i) * 1000L;
            if (t > laengsteVorschwingenMs) laengsteVorschwingenMs = t;
        }

        // Aus Datenbank: Vorlauf (vorlauf_minuten). Beginn muss mindestens laengsteVorschwingenMs vor Start sein, damit der Motor rechtzeitig ein geht.
        if (context != null && !nameFuerDb.isEmpty()) {
            try {
                PlatinenDatabaseHelper dbHelper = PlatinenDatabaseHelper.getInstance(context);
                PlatinenDatabaseHelper.Melodie melodie = dbHelper.getMelodieByNormalizedName(nameFuerDb);
                if (melodie != null) {
                    long vorlaufMs = melodie.vorlaufMinuten * 60L * 1000L;
                    return Math.max(vorlaufMs, laengsteVorschwingenMs);
                }
            } catch (Exception e) {
                android.util.Log.v("TagesSuche", "getErsteVorlaufzeitMs: DB-Lesen fehlgeschlagen, Fallback Excel: " + e.getMessage());
            }
        }

        // Fallback Excel: Vorlauf aus Zelle (Minuten). Mindestens laengsteVorschwingenMs, damit Motor rechtzeitig ein geht.
        String stringEnde = melodieString.length() >= 4 ? melodieString.substring(melodieString.length() - 4) : "";
        if (!(stringEnde.equals(".xls"))) {
            melodieString = melodieString + ".xls";
        }
        String pathAndFileNameMelodie = TurmtechnikActivity.sdCardPath + "/Turmtechnik/Melodien/" + melodieString;
        String vorlaufMelodieString = "0";
        ExcelRead excelRead = new ExcelRead();
        try {
            excelRead.openXls(pathAndFileNameMelodie);
            vorlaufMelodieString = excelRead.getCellString(1, 1);
        } catch (BiffException e) {
        } catch (IOException e) {
        } catch (Exception e) {
        }
        long vorlaufMelodieLong = 0;
        try {
            vorlaufMelodieLong = Long.parseLong(vorlaufMelodieString) * 60 * 1000;
        } catch (Exception e) {
        }
        return Math.max(vorlaufMelodieLong, laengsteVorschwingenMs);
    }

    private long berechneLaengsteVorlaufzeit(ArrayList<Integer> motorRelaisVerwendet) {
        int anzahlRelais = motorRelaisVerwendet.size();

        long vorlaufLaengsteVorlaufzeit = 0;

        for (int i = 0; i < StaticVariable.vorschwingZeitSekunden.size(); i++) {
            //Log.e("berechneLaengste", "StaticVariable.vorschwingZeitSekunden i=" + i + "Wert=" + StaticVariable.vorschwingZeitSekunden);
        }

        for (int i = 0; i < anzahlRelais; i++) {
            int vorschwingIndex = motorRelaisVerwendet.get(i);
            //Log.e("berechneLaengste", "vorschwingIndex=" + vorschwingIndex);
            long vorlaufTimeTemp = StaticVariable.getVorschwingSekundenForColumn(vorschwingIndex);
            //Log.e("berechneLaengste", "vorlaufTimeTemp=" + vorlaufTimeTemp);
            vorlaufLaengsteVorlaufzeit = Math.max(vorlaufLaengsteVorlaufzeit, vorlaufTimeTemp);
            //Log.e("berechneLaengste", "vorlaufLaengsteVorlaufzeit=" + vorlaufLaengsteVorlaufzeit);
        }
        vorlaufLaengsteVorlaufzeit = vorlaufLaengsteVorlaufzeit * 1000; // sekunden --> millisekunden
        //Log.e("berechneLaengste", "vorlaufLaengsteVorlaufzeit=" + vorlaufLaengsteVorlaufzeit);
        return vorlaufLaengsteVorlaufzeit;
    }

    private ArrayList<Integer> getMotorRelaisVerwendet(String pathAndFileNameMelodie) {
        ArrayList<Integer> relaisTemp = new ArrayList<Integer>();

        ExcelRead excelRead = new ExcelRead();

        try {
            excelRead.openXls(pathAndFileNameMelodie);
        } catch (BiffException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        int zeilen = excelRead.getCellZeilen();
        int spalten = excelRead.getCellSpalten();
        spalten = Math.min(spalten, 8);

        String temp = null;

        for (int j = 0; j < spalten; j++) {
            for (int i = 3; i < zeilen; i++) {
                try {
                    temp = excelRead.getCellString(4 + j, i);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                if (temp.equals("1")) {
                    relaisTemp.add(j); // das Relais in dieser Spalte ist verwendet (0... max 7)
                    break;
                }
            }
        }

        excelRead.closeWorkbook();

        return relaisTemp;
    }

    private int getIndexPrioritaet(int prioritaetNummer) // von 3 bis 0 , 3 == hoechste Prioritaet
    {

        //Log.i("prioritaet", "Nummer=" + prioritaetNummer);
        for (int i = 0; i < aktuelleZeitZeilenListe.size(); i++) {
            if (getPrioritaet(aktuelleZeitZeilenListe.elementAt(i)) == prioritaetNummer) {
                //Log.i("prioritaet", "gefunden index=" + i);
                return i;  // wenn eine Prioritaet gefunden
            }
        }

        //Log.d("keine" , "prioritaet");
        return -1;
    }

    public int getPrioritaet(int localZeile) {
        // Benutzerprogramm-Slot 1000..1019: nicht in zeileToProgrammMap (nur Normalprogramm), Priorität 0 – kein Zugriff auf Excel/Map
        if (localZeile >= BENUTZERPROGRAMM_ZEILE_OFFSET && localZeile < BENUTZERPROGRAMM_ZEILE_MAX) {
            return 0;
        }
        // Wenn Programme aus DB kommen (excelread2 ist null), verwende DB-Programm
        if (excelread2 == null) {
            Programm programm = getProgrammByZeile(localZeile);
            if (programm != null) {
                int prioritaet = programm.getPrioritaet();
                android.util.Log.d("TagesSuche", "getPrioritaet (DB): Zeile " + localZeile + " (Programm ID " + programm.getId() + ") = " + prioritaet);
                return prioritaet;
            } else {
                android.util.Log.w("TagesSuche", "getPrioritaet: Kein Programm in zeileToProgrammMap für Zeile " + localZeile);
                return 0;
            }
        }
        
        // Excel-Modus: Verwende normale Excel-Logik
        try {
            return Integer.parseInt(excelread2.getCellString(SPALTE_Q_PRIORITAET, localZeile));
            //return Integer.parseInt(excelread2.getCellString(SPALTE_P_PRIORITAET, localZeile));
        } catch (NumberFormatException e) {
            // TODO Auto-generated catch block
            //e.printStackTrace();
            //new LogExcelError(0, localZeile, filePathAndName2, 0, sourceFileName2, 212) ;
            // 24.06.16 kein Error mehr, wenn keine Zahl da steht wird 0 angenommen
            return 0;
        } catch (ArrayIndexOutOfBoundsException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            new LogExcelError(0, localZeile, filePathAndName2, 0, sourceFileName2, 216);
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            new LogExcelError(0, localZeile, filePathAndName2, 0, sourceFileName2, 220);
        }
        return 0;
    }

    public boolean zeileOk(int blockIndex) {
        android.util.Log.d("TagesSuche", "zeileOk: Prüfe Zeile " + blockIndex);
        //Log.e("ZeileOkCheck", "blockIndex=" + blockIndex);
        
        // Wenn Programme aus DB kommen (excelread2 ist null), verwende programmOkFromDatabase
        if (excelread2 == null) {
            // Benutzerprogramm-Slot: Zeile 1000..1019 – unabhängig von filePathAndName2, da bei dbModeExecuteThisRun
            // die Liste [1002] aus findNext kommen kann, während filePathAndName2 noch „Normalprogramm“ ist.
            if (blockIndex >= BENUTZERPROGRAMM_ZEILE_OFFSET && blockIndex < BENUTZERPROGRAMM_ZEILE_MAX) {
                android.util.Log.d("TagesSuche", "zeileOk (Benutzerprogramm): Slot " + (blockIndex - BENUTZERPROGRAMM_ZEILE_OFFSET) + " = true");
                return true;
            }
            Programm programm = getProgrammByZeile(blockIndex);
            if (programm == null) {
                android.util.Log.w("TagesSuche", "zeileOk: Kein Programm gefunden für Excel-Zeile " + blockIndex);
                android.util.Log.w("ProgrammStart", "zeileOk FALSE: Zeile " + blockIndex + " – getProgrammByZeile=null (Tagtyp/zeileToProgrammMap?)");
                return false;
            }

            // Verwende programmOkFromDatabase für vollständige Prüfung
            // WICHTIG: Verwende zukünftiges Datum, wenn gesetzt (für Suche nach zukünftigen Programmen)
            // Ansonsten verwende aktuelles Datum
            int jahr, monat, tag;
            if (futureJahr >= 0 && futureMonat >= 0 && futureTag >= 0) {
                // Verwende zukünftiges Datum
                jahr = futureJahr;
                monat = futureMonat;
                tag = futureTag;
                android.util.Log.d("TagesSuche", "zeileOk (DB): Verwende zukünftiges Datum " + tag + "." + (monat + 1) + "." + jahr);
            } else {
                // Verwende aktuelles Datum
                if (calendar == null) {
                    calendar = GregorianCalendar.getInstance();
                }
                jahr = calendar.get(Calendar.YEAR);
                monat = calendar.get(Calendar.MONTH);
                tag = calendar.get(Calendar.DAY_OF_MONTH);
                android.util.Log.d("TagesSuche", "zeileOk (DB): Verwende aktuelles Datum " + tag + "." + (monat + 1) + "." + jahr);
            }
            
            boolean ok = programmOkFromDatabase(programm, jahr, monat, tag);
            android.util.Log.d("TagesSuche", "zeileOk (DB): Zeile " + blockIndex + " (Programm ID " + programm.getId() + ") = " + ok);
            if (!ok) {
                android.util.Log.w("ProgrammStart", "zeileOk FALSE: Zeile " + blockIndex + " (ID " + programm.getId() + ") – programmOkFromDatabase=false (Wochentag/Datum/Verknüpfte Taste?)");
            }
            return ok;
        }
        
        // Excel-Modus: Verwende normale Prüfung
        // WICHTIG: Die Wochentag-Prüfung wird IMMER durchgeführt, auch für Feiertage!
        // An Feiertagen gilt das gleiche wie an normalen Programmtagen - nur der Tagtyp ist anders.
        // Die Wochentag-Prüfung wird NICHT übersprungen.
        if (!wochenTagOk(blockIndex)) {
            android.util.Log.d("TagesSuche", "zeileOk: Zeile " + blockIndex + " NICHT OK - Wochentag passt nicht");
            //Log.e("Ze Wochentag", "passt nicht ");
            return false;
        }
        android.util.Log.d("TagesSuche", "zeileOk: Wochentag OK für Zeile " + blockIndex);
        //Log.e("ZeileOK wochenTagOk", "true");

        if (!verknuepfteTasteOn(blockIndex)) {
            android.util.Log.d("TagesSuche", "zeileOk: Zeile " + blockIndex + " NICHT OK - Verknüpfte Taste nicht an");
            //Log.e("ZeileOK verkn. Taste", "ist nicht ON");
            return false;
        } else {
            android.util.Log.d("TagesSuche", "zeileOk: Verknüpfte Taste OK für Zeile " + blockIndex);
            //Log.e("ZeileOK verkn. Taste", "ist ON - OK");
        }


        if (!periodischSommerWinterImmer(blockIndex)) {
            android.util.Log.d("TagesSuche", "zeileOk: Zeile " + blockIndex + " NICHT OK - Periodizität passt nicht");
            //Log.e("ZeileOK Sommer Winter", "passt nicht");
            return false;
        }
        android.util.Log.d("TagesSuche", "zeileOk: Periodizität OK für Zeile " + blockIndex);

        if (!startEndeDatum(blockIndex)) {
            android.util.Log.d("TagesSuche", "zeileOk: Zeile " + blockIndex + " NICHT OK - Datum passt nicht");
            //Log.e("ZeileOK Start Ende", "passt nicht");
            return false;
        }
        android.util.Log.d("TagesSuche", "zeileOk: Datum OK für Zeile " + blockIndex);

        // eventuell weitere pruefungen
        android.util.Log.d("TagesSuche", "zeileOk: Zeile " + blockIndex + " ist OK");
        return true;
    }

    private boolean startEndeDatum(int zeile) {
        try {
            if (excelread2.getCellString(SPALTE_N_START, zeile).equals("")) // ist eine Start zeit angegeben?
            //if (excelread2.getCellString(SPALTE_M_START, zeile).equals("") ) // ist eine Start zeit angegeben?
            {
                // Log.i("keine" , "Startzeit");
                return true;  // keine startzeit, o.k. melden
            } else {
                String startZeit = excelread2.getCellString(SPALTE_N_START, zeile).trim();
                String stopZeit = excelread2.getCellString(SPALTE_O_ENDE, zeile).trim();
                //String startZeit = excelread2.getCellString(SPALTE_M_START, zeile).trim();
                //String stopZeit = excelread2.getCellString(SPALTE_N_ENDE, zeile).trim();

                // neu ab 6.4.2016 es gibt jetzt auch z.B. 8.T bit 14.T
                // oder 23.03.2016. 7.A
                // das mit dem .T ist z,B. vom 8. Tag bis zum 14. Tag
                // das mit dem .A beginnt z.B. am 23.03.2016 und wiederholt sich A lle 7 Tage von da ab.
                // 1.W–4.W = 1.–4. Woche des Monats

                flag_A_oder_T_gefunden = false;

                if (stopZeit.endsWith(".W") && !stopZeit.contains("Wochen")) {
                    return startEndeVersion_W_FromDatabase(stopZeit, calendar.get(Calendar.DAY_OF_MONTH));
                }

                if (startEndeVersion_A(startZeit, stopZeit)) {
                    //Log.e("CDB" , ".A") ;
                    return true;
                }

                if (startEndeVersion_T(startZeit, stopZeit)) {
                    //Log.e("CDB" , ".T") ;
                    return true;
                }

                if (flag_A_oder_T_gefunden) {
                    //Log.e("CDB" , ".A oder .B gefunden -- return") ;
                    return false;  // zwar gefunden aber nichts hat gepasst
                }

                // das ist die normale alte Methode z.B Start 1.4 bits Ende 31.4
                String[] startZeitSplit = startZeit.split("\\.");
                String[] stopZeitSplit = stopZeit.split("\\.");

//		String[] zeitSplit = zeit.split(":");


                //Log.d("gesplitet" , "length = " + startZeitSplit.length);
                //Log.i("Split" , "[0] = " + startZeitSplit[0]);
                //Log.i("Split" , "[1] = " + startZeitSplit[1]);

                int programmlisteStartZeitTag = Integer.parseInt(startZeitSplit[0]);
                int programmlisteStartZeitMonat = Integer.parseInt(startZeitSplit[1]);

                int programmlisteStopZeitTag = Integer.parseInt(stopZeitSplit[0]);
                int programmlisteStopZeitMonat = Integer.parseInt(stopZeitSplit[1]);


                return (checkDatumBetween(programmlisteStartZeitTag, programmlisteStartZeitMonat, programmlisteStopZeitTag, programmlisteStopZeitMonat));
            }
        } catch (ArrayIndexOutOfBoundsException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            new LogExcelError(11, zeile, filePathAndName2, 0, sourceFileName2, 290);
        } catch (NumberFormatException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            new LogExcelError(11, zeile, filePathAndName2, 0, sourceFileName2, 294);
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            new LogExcelError(11, zeile, filePathAndName2, 0, sourceFileName2, 298);
        }
        return false;

    }

    private boolean startEndeVersion_A(String startZeit, String stopZeit) {

        boolean retFlag = false;

        if (!stopZeit.contains("A")) {
            return retFlag;
        }

        try {
            //Log.e("startEnde" , "A gefunden" ) ;
            flag_A_oder_T_gefunden = true;
            String startZeitSplit[] = startZeit.split("\\.");
            String stopZeitSplit[] = stopZeit.split("\\.");
            long wiederholungsTag = Integer.parseInt(stopZeitSplit[0]);

            // [0] == Tag
            // [1] == Monat
            // [2] == Jahr
            int tag = Integer.parseInt(startZeitSplit[0]);
            int monat = Integer.parseInt(startZeitSplit[1]) - 1;
            int jahr = Integer.parseInt(startZeitSplit[2]);

            //Log.e("startEnde" , "tag=" + tag) ;
            //Log.e("startEnde" , "monat=" + monat) ;
            //Log.e("startEnde" , "jahr=" + jahr) ;

            //Date datumTabelle = new Date( (jahr - 1900) , monat, tag);
            //Date datumHeute = new Date() ;

            Date datumTabelle = new Date((jahr - 1900), monat, tag);
            int checkJahr = calendar.get(Calendar.YEAR);
            int checkMonat = calendar.get(Calendar.MONTH);
            int checkTag = calendar.get(Calendar.DAY_OF_MONTH);
            //Date datumHeute = calendar.getTime() ;
            Date checkDatum = new Date((checkJahr - 1900), checkMonat, checkTag);

            long datumTabelleLongMs = datumTabelle.getTime();
            long datumCheckLongMs = checkDatum.getTime();

            //Log.e("startEnde" , "datumTabelleMs=" + convertMsToTimeString(datumTabelleLongMs)) ;
            //Log.e("startEnde" , "datumCheckMs=" + convertMsToTimeString(datumCheckLongMs)) ;

            Long datumTabelleLongTage = datumTabelleLongMs / (1000 * 60 * 60 * 24);
            Long datumCheckLongTage = datumCheckLongMs / (1000 * 60 * 60 * 24);
            //Log.e("startEnde" , "TabelleTage=" + datumTabelleLongTage) ;
            //Log.e("startEnde" , "CheckTage=" + datumCheckLongTage ) ;


            //long differenzMs = datumHeuteLongMs - datumTabelleLongMs ;
            //Log.e("startEnde" , "differenzMs=" + differenzMs ) ;

            //long differenzTage = differenzMs / (1000 * 60 * 60 * 24 ) ;
            long differenzTage = datumCheckLongTage - datumTabelleLongTage;
            //Log.e("startEnde" , "differenzTage=" + differenzTage) ;


            if ((datumCheckLongMs >= datumTabelleLongMs)) {
                long datumDurchTageRest = differenzTage % wiederholungsTag;
                //Log.e("startEnde" , "datumDurchTageRest=" + datumDurchTageRest) ;

                if (datumDurchTageRest == 0) {
                    //Log.e("startEnde" , "der Rest war 0 also true") ;
                    retFlag = true;
                }
            }
        } catch (Exception e) {

        }

        return retFlag;
    }

    private boolean startEndeVersion_T(String startZeit, String stopZeit) {
        boolean retFlag = false;

        if (!stopZeit.contains("T")) {

            return retFlag;
        }

        try {

            flag_A_oder_T_gefunden = true;
            //Log.e("startEnde" , "T gefunden" ) ;

            String[] startZeitSplit = startZeit.split("\\.");
            String[] stopZeitSplit = stopZeit.split("\\.");

            int startTagInt = Integer.parseInt(startZeitSplit[0]);
            int stopTagInt = Integer.parseInt(stopZeitSplit[0]);

            //Log.e("startEnde" , "startTagInt=" + startTagInt) ;
            //Log.e("startEnde" , "stopTagInt=" + stopTagInt) ;

            int tag = calendar.get(Calendar.DAY_OF_MONTH);
            //Log.e("startEnde" , "tag=" + tag )  ;


            if ((tag >= startTagInt) && (tag <= stopTagInt)) {
                //Log.e("startEnde" , "tag >= startTagInt && tag <= stopTagInt == true" ) ;
                retFlag = true;
            }
        } catch (Exception e) {

        }

        return retFlag;

    }

    private boolean checkDatumBetween(int startZeitTag, int startZeitMonat, int stopZeitTag, int stopZeitMonat) {
        int tag = calendar.get(Calendar.DAY_OF_MONTH);
        //Log.e("CDB DAY_OF_MONTH", "= " + tag) ;
        int monat = (calendar.get(Calendar.MONTH)) + 1; // monate beginne im Kalender bei 0
        //Log.e("CDB MONTH" , "= " + monat) ;

        //Log.e("CDB DAY_OF_MONTH" , "=" + tag) ;
        //Log.e("CDB MONTH" , "= " + monat) ;
        //Log.e("CDB startZeitTag" , "=" + startZeitTag) ;
        //Log.e("CDB startZeitMonat" , "=" + startZeitMonat) ;
        //Log.e("CDB stopZeitTag" , "=" + stopZeitTag) ;
        //Log.e("CDB stopZeitMonat" , "=" + stopZeitMonat) ;

        if ((monat == startZeitMonat) && (monat == stopZeitMonat)) {
            return ((tag >= startZeitTag) && tag <= stopZeitTag);
        } else {
            // die Monate sind nicht gleich:
            if (monat == startZeitMonat) {
                return tag >= startZeitTag;
            }
            if (monat == stopZeitMonat) {
                return tag <= stopZeitTag;
            }

            // wenn Monat start und ende nicht gleich sind
            // und das Monat ist weder startMonat noch stopMonat:

            do {
                // jedes weitere monat anschauen
                monat++;
                if (monat > 12) {
                    monat = 1;
                }

                if (monat == startZeitMonat) {
                    // treffe ich zuerst auf start Monat bin ich nicht zwischen start und ende
                    //Log.e("CDB nichts" , "gefunden --> false") ;
                    return false;
                }

            }
            while (monat != stopZeitMonat);
            // treffe ich auf das stopMonat bin ich dazwischen
            //Log.e("CDB gefunden" , "--> true") ;
            return true;
        }
    }


    private boolean periodischSommerWinterImmer(int zeile) {
        String swi = "";
        try {
            swi = (excelread2.getCellString(SPALTE_M_PERIODISCH, zeile).trim());
            //swi = (excelread2.getCellString(SPALTE_L_PERIODISCH, zeile).trim());
        } catch (ArrayIndexOutOfBoundsException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            new LogExcelError(10, zeile, filePathAndName2, 0, sourceFileName2, 371);
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            new LogExcelError(10, zeile, filePathAndName2, 0, sourceFileName2, 375);
        }

        //Log.i("Periodisch" , "=" + swi);

//		if (swi.equals("1"));
//		{
//			Log.i("swi","= true");
//		}

        //////Log.e("swi" , "=" + swi ) ;

        if (swi.equals("1") && (!isSommerzeit())) {
            return false;   // fehler  wenn Programmliste = sommerzeit und datum = winterzeit
        }

        if (swi.equals("2") && (isSommerzeit())) {
            return false;  // fehler wenn winterzeit gewuenscht aber es ist sommerzeit
        }

        //Log.d("Sommer/Winter","keine");
        return true;  // nicht sommerzeit und auch nicht winterzeit = immer
    }

    private boolean isSommerzeit() {
        boolean temp;
        //	temp =  calendar.getTimeZone().inDaylightTime(calendar.getTime());
        //	temp = calendar.getTimeZone().inDaylightTime(calendar.getTime());
        TimeZone cet = TimeZone.getTimeZone("CET");
        temp = cet.inDaylightTime(calendar.getTime());

        //Log.i("Sommerzeit" , "=" + temp );

        return temp;
    }

    private boolean verknuepfteTasteOn(int zeile) {
        //StaticVariable.turnOffVerknuepft = -1 ;
        localTurnOffVerknuepft = -1;

        //if(StaticVariable.melodieAktiv)
        //{
        //    StaticVariable.turnOffVerknuepft3 = -1;
        //    return false ;
        //}
        try {
            if (!(excelread2.getCellString(SPALTE_P_VERKNUEPFTE_TASTE, zeile).trim().equals("")))  // gibts ueberhaupt eine verkn. Taste?
            //if ( ! (excelread2.getCellString(SPALTE_O_VERKNUEPFTE_TASTE, zeile).trim().equals("")) )  // gibts ueberhaupt eine verkn. Taste?
            {
                if (UhrThread.verknuepfteTastenString.size() > 0) // wurde aktivity 2 ueberhaupt gestartet?
                {
                    for (int i = 0; i < UhrThread.verknuepfteTastenString.size(); i++) {
                        String activity2String = UhrThread.verknuepfteTastenString.elementAt(i) != null ? (UhrThread.verknuepfteTastenString.elementAt(i)).trim() : "";
                        String programmlisteString = (excelread2.getCellString(SPALTE_P_VERKNUEPFTE_TASTE, zeile)).trim();
                        //String programmlisteString = (excelread2.getCellString(SPALTE_O_VERKNUEPFTE_TASTE , zeile)).trim();
//					Log.i("activity2String" , "" + activity2String);
//					Log.i("proglisteString" , "" + programmlisteString);
                        if (activity2String.equalsIgnoreCase(programmlisteString)) // wenn Taste vorhanden, ist sie ON?
                        {
//						Log.d("die 2 strings" , "passen");
//						Log.i("verkn. Taste", "" + TurmtechnikActivity.verknuepfteTastenOn[i]);
                            if (TurmtechnikActivity.verknuepfteTastenOn != null && i < TurmtechnikActivity.verknuepfteTastenOn.length && Boolean.TRUE.equals(TurmtechnikActivity.verknuepfteTastenOn[i])) {
                                //Log.i("verkn. Taste" , "" + activity2String + "= true" );
                                //if(!immerOn(zeile))
                                //{
                                //	if (!prognoseModus)
                                //	{
                                //		TurmtechnikActivity.verknuepfteTastenOn[i]=false ;
                                // aber wie das anzeigen wenn gerade Seite2Activity laeft ?
                                //	}
                                //}

                                String immerOn = excelread2.getCellString(SPALTE_L_IMMER, zeile).toLowerCase();
                                //String immerOn = excelread2.getCellString(SPALTE_K_IMMER, zeile).toLowerCase() ;


                                if (!(immerOn.equals("x"))) {

                                    localTurnOffVerknuepft = i;
                                    //Log.e("nicht immer", "verknuepft=" + StaticVariable.turnOffVerknuepft4);

                                }
                                return true;
                            } else {
                                //Log.i("verkn. Taste" , "" + activity2String + "= false" );
                                return false;
                            }
                        } else {
                            //Log.d("die strings" , "sind nicht gleich") ;
                        }
                    }
                    return false;
                } else {
                    return false;
                }
            } else {
                return true; // keine verknuepfte Taste in der Programmliste eingegeben
            }
        } catch (ArrayIndexOutOfBoundsException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            new LogExcelError(14, zeile, filePathAndName2, 0, sourceFileName2, 466);
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            new LogExcelError(14, zeile, filePathAndName2, 0, sourceFileName2, 470);
        }
        return false;
    }

    public int getNurEinmalOffset() {
        return localTurnOffVerknuepft;
    }

    private boolean wochenTagOk(int zeile) {
        //Log.i("check" , "wochenTagOk=" + zeile);


        int wochentag = calendar.get(Calendar.DAY_OF_WEEK);
        
        // Calendar.DAY_OF_WEEK: 1=Sonntag, 2=Montag, 3=Dienstag, 4=Mittwoch, 5=Donnerstag, 6=Freitag, 7=Samstag
        android.util.Log.e("Wochentag", "Calendar.DAY_OF_WEEK=" + wochentag + " (1=Sonntag, 2=Montag, ..., 7=Samstag)");

        if (wochentag == 1) {
            wochentag = 6;  // Sonntag wird zu Index 6
        } else {
            wochentag = wochentag - 2;  // Montag == 0, Dienstag == 1, ..., Samstag == 5
        }
        
        // wochentag ist jetzt: Montag=0, Dienstag=1, Mittwoch=2, Donnerstag=3, Freitag=4, Samstag=5, Sonntag=6
        android.util.Log.e("Wochentag", "Nach Umrechnung: wochentag=" + wochentag + " (Montag=0, Dienstag=1, ..., Sonntag=6)");
        
        // Wochentage beginnen immer bei Spalte E (Index 4), da Spalte D (Heizung) automatisch eingefügt wird, wenn sie fehlt
        int spalteWochentagStart = SPALTE_E_WOCHENTAG_MONTAG; // Index 4
        
        int spalte = wochentag + spalteWochentagStart;
        android.util.Log.e("Wochentag", "Spalte für Wochentag: " + spalte + " (spalteWochentagStart=" + spalteWochentagStart + ")");
        android.util.Log.e("Wochentag", "Spalten-Mapping: Montag=Spalte " + (0 + spalteWochentagStart) + 
                ", Dienstag=" + (1 + spalteWochentagStart) + 
                ", Mittwoch=" + (2 + spalteWochentagStart) + 
                ", Donnerstag=" + (3 + spalteWochentagStart) + 
                ", Freitag=" + (4 + spalteWochentagStart) + 
                ", Samstag=" + (5 + spalteWochentagStart) + 
                ", Sonntag=" + (6 + spalteWochentagStart));

        //Log.i("aktuelle zeile" , " " + zeile);
        String programmlisteWochentag = "";
        try {
            programmlisteWochentag = excelread2.getCellString(spalte, zeile);
            android.util.Log.e("Wochentag", "Gelesen aus Spalte " + spalte + ", Zeile " + zeile + ": '" + programmlisteWochentag + "'");
            //programmlisteWochentag = excelread2.getCellString(wochentag + SPALTE_D_WOCHENTAG_MONTAG, zeile);
            // ////Log.e("programmlisteWochentag" , "=" + programmlisteWochentag) ;
        } catch (ArrayIndexOutOfBoundsException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            android.util.Log.e("Wochentag", "FEHLER: ArrayIndexOutOfBoundsException für Spalte " + spalte + ", Zeile " + zeile);
            new LogExcelError(spalte, zeile, filePathAndName2, 0, sourceFileName2, 499);
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            android.util.Log.e("Wochentag", "FEHLER: Exception beim Lesen von Spalte " + spalte + ", Zeile " + zeile + ": " + e.getMessage());
            new LogExcelError(11, zeile, filePathAndName2, 0, sourceFileName2, 503);
        }
        //////Log.e("programm Liste=" + wochentag, "wochentag="+programmlisteWochentag);
        if (!(programmlisteWochentag.equals("x"))) // kein x beim Wochentag?
        {
            android.util.Log.e("Wochentag", "Wochentag passt NICHT: '" + programmlisteWochentag + "' ist nicht 'x'");
            return false;
        } else {
            android.util.Log.e("Wochentag", "Wochentag passt: '" + programmlisteWochentag + "' ist 'x'");
            return true;
        }
    }

    public static String getPathAndFilenameToday(int festtageTag, int festtageMonat, int festtageJahr) {
        ////Log.e("Festtag" , "Tag=" + festtageTag) ;
        ////Log.e("Festtag" , "Monat=" + festtageMonat) ;
        ////Log.e("Festtag" , "Jahr=" + festtageJahr) ;
        String retString = "";

        //normalOderFesttag = 0 ;
        
        android.util.Log.d("TagesSuche", "getPathAndFilenameToday: Suche Tagtyp für " + festtageTag + "." + (festtageMonat + 1) + "." + festtageJahr + 
                " (Monat 0-basiert: " + festtageMonat + ")");
        
        // NEU: Versuche zuerst aus Datenbank zu lesen (wenn aktiviert)
        if (StaticConstants.USE_DATABASE_FOR_FEIERTAGE_TAGTYPEN) {
            try {
                android.content.Context context = TurmtechnikActivity.turmtechnikContext;
                if (context != null) {
                    PlatinenDatabaseHelper dbHelper = PlatinenDatabaseHelper.getInstance(context);
                    
                    // 1. Zuerst Osterfeiertage (variable Feiertage wie Aschermittwoch) – haben Vorrang vor festen/Sonderfeiertagen
                    android.util.Log.d("TagesSuche", "getPathAndFilenameToday: Prüfe Osterfeiertage für " + festtageTag + "." + (festtageMonat + 1) + "." + festtageJahr);
                    String tagtypName = dbHelper.getTagtypNameForOsterfeiertag(festtageTag, festtageMonat, festtageJahr);
                    if (tagtypName != null && !tagtypName.trim().isEmpty()) {
                        android.util.Log.d("TagesSuche", "getPathAndFilenameToday: Osterfeiertag aus DB gefunden: " + tagtypName);
                        return tagtypName;
                    }
                    // 2. Dann feste Feiertage (inkl. Sonderfeiertage)
                    android.util.Log.d("TagesSuche", "getPathAndFilenameToday: Prüfe feste Feiertage für " + festtageTag + "." + (festtageMonat + 1) + "." + festtageJahr);
                    tagtypName = dbHelper.getTagtypNameForFesterFeiertag(festtageTag, festtageMonat, festtageJahr);
                    if (tagtypName != null && !tagtypName.trim().isEmpty()) {
                        android.util.Log.d("TagesSuche", "getPathAndFilenameToday: Fester Feiertag aus DB gefunden: " + tagtypName);
                        return tagtypName;
                    }
                    // Kein Excel mehr: Kein Feiertag für dieses Datum → Normalprogramm
                    android.util.Log.d("TagesSuche", "getPathAndFilenameToday: Kein Feiertag in DB, verwende Normalprogramm");
                    return "Normalprogramm";
                } else {
                    android.util.Log.w("TagesSuche", "getPathAndFilenameToday: Context ist null, verwende Normalprogramm");
                    return "Normalprogramm";
                }
            } catch (Exception e) {
                android.util.Log.w("TagesSuche", "getPathAndFilenameToday: Fehler beim Lesen aus DB, verwende Normalprogramm: " + e.getMessage());
                e.printStackTrace();
                return "Normalprogramm";
            }
        }

        // System.xls Sheet 12/13 entfernt – Festtage nur aus DB (siehe oben). Fallback:
        return "Normalprogramm";
    } // ende von getPathAndFilenameToday

} // ende der Klasse

