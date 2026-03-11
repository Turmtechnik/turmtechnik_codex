package tom.turmtechnik;

import android.util.Log;

import java.io.IOException;
import java.util.List;

import jxl.read.biff.BiffException;

import static tom.turmtechnik.TurmtechnikActivity.beschriftungTastenFileString;
import static tom.turmtechnik.TurmtechnikActivity.normalprogrammFileString;

public class MelodieThreadNew extends Thread {

    private static boolean doRun = true;
    private static MelodieThreadNew currentInstance = null;
    private String pathUndfileNameMelodie;
    /** Programstart in ms (z. B. 12:00). Wenn > 0: Thread wartet bis zu diesem Zeitpunkt, bevor die erste Zeile ausgeführt wird – damit Vorschwingen (Motor 90 s vorher) eingehalten wird. */
    private long programStartMs;

    private ExcelRead excelread;
    private int cellSpalten;
    private final String sourceFileName = "MelodieThread";
    private final int maxGlockenAnzahl = 16; // von 8 auf 16 erhoeht 15.08.16
    private int zeile ;
    private int maxZeilen ;


    public MelodieThreadNew(String pathUndfileNameMelodie) {
        this(pathUndfileNameMelodie, 0);
    }

    /**
     * @param pathUndfileNameMelodie Pfad zur Melodie-Datei
     * @param programStartMs Programstart in ms (z. B. 12:00). Wenn > 0: Thread wartet bis zu diesem Zeitpunkt vor der ersten Zeile, damit Motor (Vorschwingen) vor Klöppel startet.
     */
    public MelodieThreadNew(String pathUndfileNameMelodie, long programStartMs) {
        StaticVariable.melodieAktiv = true;
        this.pathUndfileNameMelodie = pathUndfileNameMelodie;
        this.programStartMs = programStartMs;
        Log.e("constructor", "MelodieThread fileAndPath=" + this.pathUndfileNameMelodie + " programStartMs=" + programStartMs);
        doRun = true;
        currentInstance = this;
    }

    public void run() {
        StaticVariable.melodieAktiv = true;

        Log.e("MelodieThread", "Anfang von run ");
        Log.e("MelodieThread", "turnOffVerknuepft=" + StaticVariable.turnOffVerknuepft4);
        Log.e("MelodieThread", "path und filename=" + pathUndfileNameMelodie);
        // Log Start Zeit + Melodie (wenn Log Abgelaufene Melodien aktiv)
        if (StaticVariable.logAbgelaufeneMelodien && pathUndfileNameMelodie != null) {
            String melodiename = pathUndfileNameMelodie;
            int lastSlash = melodiename.lastIndexOf('/');
            if (lastSlash >= 0 && lastSlash + 1 < melodiename.length())
                melodiename = melodiename.substring(lastSlash + 1);
            if (melodiename.length() > 4 && melodiename.toLowerCase().endsWith(".xls"))
                melodiename = melodiename.substring(0, melodiename.length() - 4).trim();
            LogTurmtechnik2.appendMelodieStart(melodiename);
        }
        // Bei Melodie-Start „Start: [Name]“ an Webhook/Telegram senden (wenn in Anlagendaten ausgewählt)
        String displayName = (StaticVariable.nameNextMelodie != null && !StaticVariable.nameNextMelodie.trim().isEmpty()) ? StaticVariable.nameNextMelodie.trim() : extractMelodieNameFromPath(pathUndfileNameMelodie);
        if (TurmtechnikActivity.turmtechnikContext != null) TurmtechnikActivity.sendMelodieStartIfConfigured(TurmtechnikActivity.turmtechnikContext, displayName);
        // Prüfe, ob es eine MIDI-Datei ist
        String lowerPath = pathUndfileNameMelodie.toLowerCase();
        if (lowerPath.endsWith(".mid") || lowerPath.endsWith(".midi")) {
            Log.d("MelodieThread", "MIDI-Datei erkannt, starte MidiThread");
            // MIDI-Datei: Verwende MidiThread (als separater Thread)
            MidiThread midiThread = new MidiThread(pathUndfileNameMelodie);
            midiThread.start();
            // Warte auf Beendigung des MidiThread
            try {
                midiThread.join();
            } catch (InterruptedException e) {
                Log.e("MelodieThread", "Warten auf MidiThread unterbrochen", e);
                Thread.currentThread().interrupt();
            }
            // End-Zeit für Melodien-Log (auch bei MIDI)
            if (StaticVariable.logAbgelaufeneMelodien && pathUndfileNameMelodie != null) {
                String melodienameEnd = pathUndfileNameMelodie;
                int lastSlash = melodienameEnd.lastIndexOf('/');
                if (lastSlash >= 0 && lastSlash + 1 < melodienameEnd.length())
                    melodienameEnd = melodienameEnd.substring(lastSlash + 1);
                if (melodienameEnd.length() > 4 && melodienameEnd.toLowerCase().endsWith(".xls"))
                    melodienameEnd = melodienameEnd.substring(0, melodienameEnd.length() - 4).trim();
                LogTurmtechnik2.appendAbgelaufeneMelodie(melodienameEnd);
            }
            StaticVariable.melodieAktiv = false;
            StaticVariable.currentMelodieZeileAnzeige = "";
            StaticVariable.currentMelodieZeileDauer = "";
            if (StaticVariable.currentMelodieZeileRelais != null) { for (int i = 0; i < StaticVariable.currentMelodieZeileRelais.length; i++) StaticVariable.currentMelodieZeileRelais[i] = 0; } if (StaticVariable.currentMelodieZeileVorschwing != null) { for (int i = 0; i < StaticVariable.currentMelodieZeileVorschwing.length; i++) StaticVariable.currentMelodieZeileVorschwing[i] = 0; }
            currentInstance = null;
            return;
        }
        
        // Prüfe, ob Melodie in Datenbank existiert (exakt oder normalisierter Name für gleiche Anzeige)
        String melodieName = extractMelodieNameFromPath(pathUndfileNameMelodie);
        PlatinenDatabaseHelper dbHelper = PlatinenDatabaseHelper.getInstance(TurmtechnikActivity.turmtechnikContext);
        PlatinenDatabaseHelper.Melodie melodie = dbHelper != null ? dbHelper.getMelodieByNormalizedName(melodieName) : null;
        
        if (melodie != null) {
            // Melodie aus Datenbank laden
            Log.d("MelodieThread", "Melodie gefunden in Datenbank: " + melodieName);
            List<PlatinenDatabaseHelper.MelodieZeile> zeilen = dbHelper.getMelodieZeilen(melodie.id);
            
            if (zeilen != null && !zeilen.isEmpty()) {
                // Verwende virtuelles ExcelRead-Objekt aus DB-Daten
                excelread = createVirtualExcelReadFromDatabase(zeilen);
                Log.d("MelodieThread", "Melodie aus Datenbank geladen: " + zeilen.size() + " Zeilen");
            } else {
                Log.w("MelodieThread", "Melodie in DB gefunden, aber keine Zeilen vorhanden. Fallback zu Excel.");
                String excelPath = pathUndfileNameMelodie;
                if (!new java.io.File(excelPath).exists() && excelPath != null && !excelPath.toLowerCase().endsWith(".xls")) {
                    String withXls = excelPath + ".xls";
                    if (new java.io.File(withXls).exists()) excelPath = withXls;
                }
                if (new java.io.File(excelPath).exists()) {
                    excelread = new ExcelRead();
                    try {
                        excelread.openXls(excelPath);
                    } catch (BiffException e1) {
                        e1.printStackTrace();
                        Log.e("BiffException", "MelodieThread zeile 38");
                        new LogExcelError(-1, -1, pathUndfileNameMelodie, -1, "MelodieThread", 31);
                    } catch (IOException e1) {
                        Log.e("IOExeption", "MelodieThread zeile 42");
                        e1.printStackTrace();
                        new LogExcelError(-1, -1, pathUndfileNameMelodie, -1, "MelodieThread", 35);
                    }
                } else {
                    Log.e("MelodieThread", "Excel-Datei nicht vorhanden und keine Zeilen in DB – Abbruch.");
                    StaticVariable.melodieAktiv = false;
                    StaticVariable.currentMelodieZeileAnzeige = "";
                    StaticVariable.currentMelodieZeileDauer = "";
                    if (StaticVariable.currentMelodieZeileRelais != null) { for (int i = 0; i < StaticVariable.currentMelodieZeileRelais.length; i++) StaticVariable.currentMelodieZeileRelais[i] = 0; } if (StaticVariable.currentMelodieZeileVorschwing != null) { for (int i = 0; i < StaticVariable.currentMelodieZeileVorschwing.length; i++) StaticVariable.currentMelodieZeileVorschwing[i] = 0; }
                    currentInstance = null;
                    return;
                }
            }
        } else {
            // Excel-Datei nur, wenn Datei existiert – Pfad kann mit oder ohne .xls sein
            java.io.File melodieFile = new java.io.File(pathUndfileNameMelodie);
            if (!melodieFile.exists() && pathUndfileNameMelodie != null && !pathUndfileNameMelodie.toLowerCase().endsWith(".xls")) {
                java.io.File melodieFileXls = new java.io.File(pathUndfileNameMelodie + ".xls");
                if (melodieFileXls.exists()) {
                    melodieFile = melodieFileXls;
                    pathUndfileNameMelodie = pathUndfileNameMelodie + ".xls";
                }
            }
            if (melodieFile.exists()) {
                Log.d("MelodieThread", "Melodie nicht in Datenbank gefunden, verwende Excel-Datei: " + pathUndfileNameMelodie);
                excelread = new ExcelRead();
                try {
                    excelread.openXls(pathUndfileNameMelodie);
                } catch (BiffException e1) {
                    e1.printStackTrace();
                    Log.e("BiffException", "MelodieThread zeile 38");
                    new LogExcelError(-1, -1, pathUndfileNameMelodie, -1, "MelodieThread", 31);
                } catch (IOException e1) {
                    Log.e("IOExeption", "MelodieThread zeile 42");
                    e1.printStackTrace();
                    new LogExcelError(-1, -1, pathUndfileNameMelodie, -1, "MelodieThread", 35);
                }
            } else {
                Log.w("MelodieThread", "Melodie weder in DB (normalisiert) noch als Datei gefunden: " + pathUndfileNameMelodie + " – Abbruch.");
                StaticVariable.melodieAktiv = false;
                StaticVariable.currentMelodieZeileAnzeige = "";
                StaticVariable.currentMelodieZeileDauer = "";
                if (StaticVariable.currentMelodieZeileRelais != null) { for (int i = 0; i < StaticVariable.currentMelodieZeileRelais.length; i++) StaticVariable.currentMelodieZeileRelais[i] = 0; } if (StaticVariable.currentMelodieZeileVorschwing != null) { for (int i = 0; i < StaticVariable.currentMelodieZeileVorschwing.length; i++) StaticVariable.currentMelodieZeileVorschwing[i] = 0; }
                currentInstance = null;
                return;
            }
        }

        if (excelread == null) {
            Log.e("MelodieThread", "ExcelRead nicht initialisiert – Abbruch.");
            StaticVariable.melodieAktiv = false;
            StaticVariable.currentMelodieZeileAnzeige = "";
            StaticVariable.currentMelodieZeileDauer = "";
            if (StaticVariable.currentMelodieZeileRelais != null) { for (int i = 0; i < StaticVariable.currentMelodieZeileRelais.length; i++) StaticVariable.currentMelodieZeileRelais[i] = 0; } if (StaticVariable.currentMelodieZeileVorschwing != null) { for (int i = 0; i < StaticVariable.currentMelodieZeileVorschwing.length; i++) StaticVariable.currentMelodieZeileVorschwing[i] = 0; }
            currentInstance = null;
            return;
        }
        Log.e("MelodieThread", "BlockThread filePath=" + pathUndfileNameMelodie);
        cellSpalten = excelread.getCellSpalten();
        Log.e("MelodieThread", "CellSpalten nach open " + cellSpalten);

        while (doRun) {
            // Excel-Zeilen: 0=Header1, 1=Header2, 2=Spaltenüberschriften, 3=erste Datenzeile (00:00:00)
            // JXL ist 0-basiert, daher Index 2 = Excel-Zeile 3 (Spaltenüberschriften)
            // Index 3 = Excel-Zeile 4 (erste Datenzeile)
            // ABER: Wenn Daten bei Excel-Zeile 2 beginnen, dann Index 1
            // Prüfe zuerst, wo die Daten beginnen
            zeile = 1; // Starte bei Index 1 (Excel-Zeile 2) und suche die erste Zeile mit Wartezeit
            
            // printSheetDebug();
            try {
                Log.e("MelodieThread", "getCellZeilen=" + excelread.getCellZeilen());
                maxZeilen = excelread.getCellZeilen() ;
                
                // Finde die erste Zeile mit Daten (Wartezeit in Spalte 3)
                while (zeile < excelread.getCellZeilen()) {
                    String testWaitString = excelread.getCellString(3, zeile);
                    if (testWaitString != null && !testWaitString.trim().isEmpty() 
                            && !testWaitString.equals("null") && !testWaitString.equals("0")
                            && !testWaitString.equals("Zeit 100/sec")) { // Header-Zeile überspringen
                        Log.e("MelodieThread", "Erste Datenzeile gefunden bei Index " + zeile + " (Wartezeit='" + testWaitString + "')");
                        break;
                    }
                    zeile++;
                }
                
                if (zeile >= excelread.getCellZeilen()) {
                    Log.e("MelodieThread", "FEHLER: Keine Datenzeile gefunden! Melodie-Datei ist leer.");
                    break;
                }

                // Warten bis Programstart (z. B. 12:00), damit die erste Zeile nicht schon beim „Beginn“ (z. B. 11:58:30) läuft.
                // So kann checkVorschwingen den Motor 90 s vorher einschalten; Klöppel kommt erst zur Programstart-Zeit.
                if (programStartMs > 0 && doRun) {
                    long wartenBisMs = programStartMs - System.currentTimeMillis();
                    if (wartenBisMs > 0) {
                        Log.w("MelodieThread", "Warte " + (wartenBisMs / 1000) + " s bis Programstart (damit Vorschwingen vor Klöppel).");
                        waitRealTime(wartenBisMs);
                    }
                }

                // Vorschwing-Schaltzeitpunkte vorausberechnen: Für jede Zeile T ist Motor-Ein = T − Vorschwingzeit.
                // Alle MelodieRelais-Schaltzeitpunkte (0/1) sind absolut; Vorschwing wird davon abgeleitet.
                preScheduleVorschwingMotors(zeile);

                long cumulativeMs = 0; // Ab Melodie-Start bis Zeilenbeginn (für Motor-Logik bei "0")
                while (zeile < excelread.getCellZeilen()) {
                    // Anzeige für Web-UI: Zeile, Dauer, Relais G1–G16 (ein=rot, aus=grün)
                    int zeilenAnzahl = excelread.getCellZeilen();
                    StaticVariable.currentMelodieZeileAnzeige = "Zeile " + zeile + " von " + zeilenAnzahl;
                    buildZeilenInhaltForDisplay(excelread, zeile);

                    // Kanäle mit "0" sofort aus (Kloeppel + Motor); Motor nur aus wenn T_motor (nächstes "1" − Vorschwing) noch in der Zukunft
                    schalteKloeppelAusFuerZeile(zeile, cumulativeMs);

                    String waitString = excelread.getCellString(3, zeile);
                    Log.e("MelodieThread", "getCellString 3,zeile=" + zeile + " waitString=" + waitString);
                    
                    // Prüfe, ob Wartezeit vorhanden ist (leer, "null" oder "0" bedeutet Ende)
                    if (waitString == null || waitString.trim().isEmpty() 
                            || waitString.equals("null") || waitString.equals("0")) {
                        Log.e("MelodieThread", "break ausgefuehrt - keine Wartezeit in Zeile " + zeile + " (waitString='" + waitString + "')");
                        break;
                    }
                    
                    Log.e("MelodieThread", "zeile:" + waitString);

                    // Wartezeit parsen (kann auch leer sein für reine Wartezeiten ohne Aktion)
                    // "Zeit 100/sec" bedeutet: 100 = 1 Sekunde, 500 = 5 Sekunden
                    // Umrechnung: wait * 10 = Millisekunden (100 * 10 = 1000ms = 1 Sekunde)
                    int wait = 0;
                    try {
                        int waitHundertstel = Integer.parseInt(waitString.trim());
                        wait = waitHundertstel * 10; // hundertstel auf ms umrechnen
                        Log.e("MelodieThread", "Wartezeit: " + waitHundertstel + " Hundertstel = " + wait + "ms = " + (wait / 1000.0) + " Sekunden");
                    } catch (NumberFormatException e) {
                        Log.e("MelodieThread", "Fehler beim Parsen der Wartezeit in Zeile " + zeile + ": " + waitString);
                        wait = 0; // Wenn nicht parsebar, keine Wartezeit
                    }

                    // Melodie-Nocke: Kloeppel (Schlag) zum absoluten Zeitpunkt dieser Zeile schalten
                    Log.e("MelodieThread", "Rufe schalteKloeppel() auf für Zeile " + zeile + " mit wait=" + wait + "ms (" + (wait / 1000.0) + " Sekunden)");
                    long zeitVorSchalten = System.currentTimeMillis();
                    schalteKloeppel(zeile, wait, cumulativeMs);
                    updateCurrentMelodieZeileRelaisFromSent();
                    long zeitNachSchalten = System.currentTimeMillis();
                    Log.e("MelodieThread", "schalteKloeppel() abgeschlossen für Zeile " + zeile + " (Dauer: " + (zeitNachSchalten - zeitVorSchalten) + "ms)");

                    // Sound und verschachtelte Melodien NACH dem Schalten der Kloeppel
                    try {
                        String soundMp3 = null;
                        String melodieXls = null;
                        
                        // Prüfe, ob Spalte 21 existiert (soundMp3)
                        if (cellSpalten > 21) {
                            soundMp3 = excelread.getCellString(21, zeile);
                            Log.e("MelodieThread", "soundMp3=" + soundMp3);
                        }
                        
                        // Prüfe, ob Spalte 20 existiert (melodieXls)
                        if (cellSpalten > 20) {
                            melodieXls = excelread.getCellString(20, zeile);
                            Log.e("MelodieThread", "MelodieXls=" + melodieXls);
                        }

                        if (melodieXls != null && !melodieXls.equals("")) {
                            starteMelodieXls(melodieXls);
                        }

                        if (soundMp3 != null && !soundMp3.equals("")) {
                            starteSoundMp3(soundMp3, zeile); // die Zeile ist soundID
                        }
                    } catch (Exception e) {
                        Log.e("MelodieThread", "Fehler beim Lesen von Sound/Melodie in Zeile " + zeile + ": " + e.getMessage());
                        // Weiter mit der Melodie, auch wenn Sound/Melodie fehlt
                    }


                    // Zeilendauer abwarten (0 und 1 sind absolute Zeitpunkte; Vorschwing wurde vorausberechnet)
                    Log.e("warten", "Wartezeit vor waitRealTime(): " + wait + "ms = " + (wait / 1000.0) + " Sekunden");
                    long zeitVorWarten = System.currentTimeMillis();
                    waitRealTime(wait);
                    long zeitNachWarten = System.currentTimeMillis();
                    long tatsaechlicheWartezeit = zeitNachWarten - zeitVorWarten;
                    Log.e("warten", "waitRealTime() abgeschlossen. Erwartet: " + wait + "ms, Tatsächlich: " + tatsaechlicheWartezeit + "ms, Differenz: " + (tatsaechlicheWartezeit - wait) + "ms");
                    cumulativeMs += wait;
                    zeile++;
                }
            } catch (Exception e) {
                Log.e("MelodieThread", "FEHLER in Melodie-Schleife: " + e.getMessage());
                Log.e("MelodieThread", "Exception-Typ: " + e.getClass().getName());
                Log.e("MelodieThread", "Aktuelle Zeile: " + zeile);
                e.printStackTrace();
                new LogExcelError(3, zeile, pathUndfileNameMelodie, 0, "MelodieThread", 67);
            }


            Log.e("block thread", "fertig alle relais aus");
            // 7.6.2013 wegen Heizung etc. nicht automatisch ausschalten
            // schalteGlockenAus();
            // 23.9.2013 zur sicherheit aber nur Melodien ausschalten // Glocken, Kloeppel
            //allMelodieRelaisOff();

            TurmtechnikActivity.allMelodieRelaisOffAusfuehren();
            if (StaticVariable.logAbgelaufeneMelodien && pathUndfileNameMelodie != null) {
                String melodiename = pathUndfileNameMelodie;
                int lastSlash = melodiename.lastIndexOf('/');
                if (lastSlash >= 0 && lastSlash + 1 < melodiename.length())
                    melodiename = melodiename.substring(lastSlash + 1);
                if (melodiename.length() > 4 && melodiename.toLowerCase().endsWith(".xls"))
                    melodiename = melodiename.substring(0, melodiename.length() - 4).trim();
                LogTurmtechnik2.appendAbgelaufeneMelodie(melodiename);
            }
            StaticVariable.currentMelodieZeileAnzeige = "";
            StaticVariable.currentMelodieZeileDauer = "";
            if (StaticVariable.currentMelodieZeileRelais != null) { for (int i = 0; i < StaticVariable.currentMelodieZeileRelais.length; i++) StaticVariable.currentMelodieZeileRelais[i] = 0; } if (StaticVariable.currentMelodieZeileVorschwing != null) { for (int i = 0; i < StaticVariable.currentMelodieZeileVorschwing.length; i++) StaticVariable.currentMelodieZeileVorschwing[i] = 0; }
            currentInstance = null;

//			   	AvrNetIoThread.relaisNew[2]=true ;	
//		    	sleepTime(5000); // nur fuer test
//		    	AvrNetIoThread.relaisNew[2]=false ;

            doRun = false;
        }
        excelread.closeWorkbook();
        excelread = null;
        System.gc();

        checkClearVerknueftWegenImmer();

        UhrThread.blockReady = 0; // block fertig , wieder ready
        UhrThread.newSearchAutomaticStart = true;
        StaticVariable.stringInfoTextField[0] = "suche nächsten Start...";
        StaticVariable.changeInternetBenutzerprogramme++; //datum und Zeit
        //und on/off auf Internet aktualisieren
        StaticVariable.manuellerStartEinAus = false;
        StaticVariable.sofortStartPopupFlag = false;
        StaticVariable.sofortStartPopupGefunden = false;

        StaticVariable.melodieAktiv = false;
        StaticVariable.currentMelodieZeileAnzeige = "";
        StaticVariable.currentMelodieZeileDauer = "";
        if (StaticVariable.currentMelodieZeileRelais != null) { for (int i = 0; i < StaticVariable.currentMelodieZeileRelais.length; i++) StaticVariable.currentMelodieZeileRelais[i] = 0; } if (StaticVariable.currentMelodieZeileVorschwing != null) { for (int i = 0; i < StaticVariable.currentMelodieZeileVorschwing.length; i++) StaticVariable.currentMelodieZeileVorschwing[i] = 0; }
        currentInstance = null;


        StaticVariable.melodieThreadNew = null;

    } // ende von run

    private void starteSoundMp3(String soundName, int soundID) {
        SoundGlocke soundGlocke = new SoundGlocke();
        Log.e("MelodieThread", "starteSoundMpr fname=" + soundName);

        //soundGlocke.initGlockeSound(soundName);
        soundGlocke.playGlockeSound(soundName, 100, 0);  // nur einmal spielen
    }

    private void starteMelodieXls(String hammerMelodie) {
        SchlagwerkThread schlagwerkthread = new SchlagwerkThread(hammerMelodie); // normaler Schlagwerk thread
        schlagwerkthread.setPriority(Thread.MAX_PRIORITY);
        schlagwerkthread.start();
    }

    private void checkClearVerknueftWegenImmer() {
        Log.e("MelodieThreadEnde", "turnOffVerknuepft=" + StaticVariable.turnOffVerknuepft4);
        if (StaticVariable.turnOffVerknuepft4 != -1) {
            // hier verknuepft ausschalten , flag, optische Taste und Internet
            synchronized (TurmtechnikActivity.VERKNUEPFTE_TASTEN_LOCK) {
                TurmtechnikActivity.verknuepfteTastenOn[StaticVariable.turnOffVerknuepft4] = false;
            }
            TurmtechnikActivity.saveVerknuepfteTastenFromBackground(TurmtechnikActivity.turmtechnikContext);
            int relaisOffset = TurmtechnikActivity.getRelaisOffsetForVerknuepfteTaste(StaticVariable.turnOffVerknuepft4);
            String buttonId = TurmtechnikActivity.getButtonId((relaisOffset));
            StaticVariable.turnOffVerknuepft4 = -1;
        }
    }

    /*
        private void printSheetDebug()
        {
            for (int j = 0 ; j < 7 ; j ++)
            {
                for ( int i = 0 ; i < 10; i ++)
                Log.i("while" , "zeile=" + excelread.getCellString(i, j));

            }
        }
    */

    /**
     * Schaltet zu Zeilenbeginn alle Kanäle mit "0" aus: Kloeppel sofort.
     * Motor muss immer Vorschwingzeit vor dem nächsten Klöppel-"1" ein sein. Bei "0" prüfen, ob es ein nächstes "1" gibt:
     * T_motor = Zeitpunkt, an dem Motor für den nächsten Schlag ein sein muss (= T_next − Vorschwingzeit).
     * Ist T_motor schon vorbei (T_now >= T_motor) → Motor anlassen (nicht aus). Sonst Motor aus; Wiedereinschaltung bei T_motor ist von preScheduleVorschwingMotors geplant.
     */
    private void schalteKloeppelAusFuerZeile(int zeile, long tNowMs) {
        int maxCellSpalten = Math.min(maxGlockenAnzahl + 4, cellSpalten);
        for (int spalte = 4; spalte < maxCellSpalten && doRun; spalte++) {
            try {
                String kloeppelEinAus = excelread.getCellString(spalte, zeile);
                if (kloeppelEinAus == null || !kloeppelEinAus.trim().equals("0")) continue;
                int outputIndex = spalte - 4;
                // Kloeppel AUS (immer, "0" = kein Schlag)
                if (outputIndex < StaticVariable.laeutenKloeppelRelais.size()) {
                    int relaisKloeppel = StaticVariable.laeutenKloeppelRelais.get(outputIndex);
                    if (relaisKloeppel > 0) {
                        int globalOnOffset = getGlobalOnOffset(relaisKloeppel);
                        if (globalOnOffset >= 0 && globalOnOffset < TurmtechnikActivity.globalOn.length) {
                            TurmtechnikActivity.globalOn[globalOnOffset] = false;
                            stopGlockenSound(globalOnOffset);
                        }
                        if (relaisKloeppel <= Serial_IoThread.relaisNew.length) {
                            Serial_IoThread.changeRelais(relaisKloeppel, false);
                        }
                    }
                }
                // Motor AUS nur wenn T_motor (nächstes "1" − Vorschwing) noch in der Zukunft; sonst Motor anlassen
                if (outputIndex < StaticVariable.vorschwingenMotorRelaisNeu.size() && motorAusschaltenOk(spalte, zeile, tNowMs)) {
                    int relaisMotor = StaticVariable.vorschwingenMotorRelaisNeu.get(outputIndex);
                    if (relaisMotor > 0) {
                        int globalOnOffset = getGlobalOnOffset(relaisMotor);
                        if (globalOnOffset >= 0 && globalOnOffset < TurmtechnikActivity.globalOn.length) {
                            TurmtechnikActivity.globalOn[globalOnOffset] = false;
                        }
                        if (relaisMotor <= Serial_IoThread.relaisNew.length) {
                            Serial_IoThread.changeRelais(relaisMotor, false);
                        }
                    }
                }
            } catch (Exception e) {
                // ignorieren
            }
        }
    }

    /**
     * Liefert die maximale Vorschwingzeit (in ms) aller Ausgänge, die in dieser Zeile "1" haben.
     * Für die Wartezeit zwischen Motor-Nocke (Vorschwing ein) und Melodie-Nocke (Kloeppel ein).
     */
    private long getMaxVorschwingMsForZeile(int zeile) {
        int maxCellSpalten = Math.min(maxGlockenAnzahl + 4, cellSpalten);
        long maxMs = 0;
        for (int spalte = 4; spalte < maxCellSpalten && doRun; spalte++) {
            try {
                String kloeppelEinAus = excelread.getCellString(spalte, zeile);
                if (kloeppelEinAus == null || !kloeppelEinAus.trim().equals("1")) continue;
                int outputIndex = spalte - 4;
                int sec = StaticVariable.getVorschwingSekundenForColumn(outputIndex);
                if (sec > 0) {
                    long ms = sec * 1000L;
                    if (ms > maxMs) maxMs = ms;
                }
            } catch (Exception e) {
                // ignorieren
            }
        }
        return maxMs;
    }

    /**
     * Für jedes "1" wird die Vorschwing-Einschaltzeit neu berechnet: Motor-Ein = T − Vorschwingzeit (T = Zeitpunkt dieses "1").
     * Pause < Vorschwingzeit → Motor bleibt an (Einschaltzeitpunkt läge in der Vergangenheit; motorAusschaltenOk verhindert Ausschalten).
     * Pause >= Vorschwingzeit → Motor bleibt bis T − Vorschwingzeit aus, dann an.
     */
    private void preScheduleVorschwingMotors(int firstDataRow) {
        int maxCellSpalten = Math.min(maxGlockenAnzahl + 4, cellSpalten);
        long cumulativeMs = 0;
        for (int r = firstDataRow; r < excelread.getCellZeilen() && doRun; r++) {
            String waitString = null;
            try {
                waitString = excelread.getCellString(3, r);
            } catch (Exception e) { }
            if (waitString == null || waitString.trim().isEmpty()
                    || waitString.equals("null") || waitString.equals("0")) break;
            int waitMs = 0;
            try {
                waitMs = Integer.parseInt(waitString.trim()) * 10;
            } catch (Exception e) { }
            // Zeile r hat absoluten Zeitpunkt cumulativeMs. Vorschwing für "1" = cumulativeMs − vorschwingMs
            boolean firstInRow = true;
            for (int spalte = 4; spalte < maxCellSpalten && doRun; spalte++) {
                try {
                    String kloeppelEinAus = excelread.getCellString(spalte, r);
                    if (kloeppelEinAus == null || !kloeppelEinAus.trim().equals("1")) continue;
                    if (!firstInRow) {
                        try { Thread.sleep(25); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
                    }
                    firstInRow = false;
                    int outputIndex = spalte - 4;
                    if (outputIndex < 0) continue;
                    int relaisMotor = 0;
                    long vorschwingenMs = 0;
                    if (outputIndex < StaticVariable.vorschwingenMotorRelaisNeu.size()) {
                        relaisMotor = StaticVariable.vorschwingenMotorRelaisNeu.get(outputIndex);
                    }
                    vorschwingenMs = StaticVariable.getVorschwingSekundenForColumn(outputIndex) * 1000L;
                    long delayMs = cumulativeMs - vorschwingenMs;
                    if (delayMs < 0) delayMs = 0;
                    if (relaisMotor > 0) {
                        new MelodieStartMotor(delayMs, relaisMotor).start();
                    }
                } catch (Exception e) { }
            }
            cumulativeMs += waitMs;
        }
    }

    private void schalteKloeppel(int zeile, long wait, long tNowMs) {
        Log.e("Melodie", "schalte kloeppel");
        int spalte = 4;

        int globalOnOffset = 0; // eigener Index wegen der Taste 100 = Stop, und 101 = Automatik

        int aktuelleRelaisNummerKloeppel = 0;
        int aktuelleRelaisNummerMotor = 0;
        //int maxCellSpalten = (Math.min (8+4, cellSpalten) ) ; // die ersten 4 ueberspringen plus max 8 relais
        int maxCellSpalten = (Math.min( (maxGlockenAnzahl + spalte) , cellSpalten)); // die ersten 4 ueberspringen plus max 8 relais
        Log.e("Melodie" , "max GlockenAnzahl= " + maxGlockenAnzahl);
        Log.e("Melodie" , "max CellSpalten= " + maxCellSpalten);

        while ( (spalte < maxCellSpalten) && doRun)
        {
            Log.e("Melodie" , "CellSpalten= " + maxCellSpalten);
            Log.e("Melodie" , "Spalte=" + spalte);
            Log.e("Melodie" , "Zeile=" + zeile);
            //	Log.i("Beschriftung" , "= " + excelread.getCellString(spalte, 2));

            // * das war die alte Methode, neu 2.2.2015 nur mehr Kloeppel schalten

            try {
                String kloeppelEinAus = excelread.getCellString(spalte, zeile);
                
                // Prüfe, ob die Zelle null oder leer ist
                if (kloeppelEinAus == null) {
                    kloeppelEinAus = "";
                }
                kloeppelEinAus = kloeppelEinAus.trim();
                
                // Leere Zelle = keine Aktion für diese Glocke, einfach überspringen
                if (kloeppelEinAus.isEmpty()) {
                    // Keine Aktion - nur Wartezeit
                    spalte++;
                    continue;
                }
                
                if (kloeppelEinAus.equals("0"))
                {
                    // Kloeppel AUS
                    aktuelleRelaisNummerKloeppel = StaticVariable.laeutenKloeppelRelais.get(spalte - 4);
                    Log.e("Melodie" , "Kloeppel AUS - RelaisNummberKloeppel=" + aktuelleRelaisNummerKloeppel);
                    
                    if (aktuelleRelaisNummerKloeppel > 0) {
                        globalOnOffset = getGlobalOnOffset(aktuelleRelaisNummerKloeppel);
                        
                        if (globalOnOffset >= 0 && globalOnOffset < TurmtechnikActivity.globalOn.length) {
                            TurmtechnikActivity.globalOn[globalOnOffset] = false;
                            stopGlockenSound(globalOnOffset);
                            
                            // Verwende changeRelais() statt direkt relaisNew[] zu setzen
                            if (aktuelleRelaisNummerKloeppel > 0 && aktuelleRelaisNummerKloeppel <= Serial_IoThread.relaisNew.length) {
                                Serial_IoThread.changeRelais(aktuelleRelaisNummerKloeppel, false);
                                Log.e("Melodie", "Relais " + aktuelleRelaisNummerKloeppel + " AUS geschaltet");
                            }
                        }
                    }
                    // Meteor Code entfernt


                    // Mit Kloeppel auch Motor ausschalten
                    // wenn laenger ausgeschaltet als vorschwingen Zeit

                    boolean motorOffFlag = (motorAusschaltenOk(spalte, zeile, tNowMs));
                    Log.e("Melodie" , "motorAusflag?= " + motorOffFlag);
                    if (motorOffFlag) {
                        Log.e("Melodie" , "MotorAus?=true");
                        // bei langer Pause auch Motor ausschalten
                        aktuelleRelaisNummerMotor = StaticVariable.vorschwingenMotorRelaisNeu.get(spalte - 4);
                        Log.e("Melodie" , "aktuelle RelaisNummberMotor=" + aktuelleRelaisNummerMotor);
                        
                        if (aktuelleRelaisNummerMotor > 0) {
                            globalOnOffset = getGlobalOnOffset(aktuelleRelaisNummerMotor);
                            if (globalOnOffset >= 0 && globalOnOffset < TurmtechnikActivity.globalOn.length) {
                                TurmtechnikActivity.globalOn[globalOnOffset] = false;
                                
                                // Verwende changeRelais() statt direkt relaisNew[] zu setzen
                                if (aktuelleRelaisNummerMotor > 0 && aktuelleRelaisNummerMotor <= Serial_IoThread.relaisNew.length) {
                                    Serial_IoThread.changeRelais(aktuelleRelaisNummerMotor, false);
                                    Log.e("Melodie", "Motor-Relais " + aktuelleRelaisNummerMotor + " AUS geschaltet");
                                }
                            }
                        }
                        // Meteor Code entfernt

                    } else {
                        Log.e("Melodie" , "MotorAus?=false");
                    }

                    // Motoren der nächsten Zeile werden zu Zeilenbeginn mit wait=0 gestartet (Hauptschleife),
                    // nicht mehr hier mit Verzögerung – damit „alle ein für X Sekunden“ korrekt läuft.

                } else if (kloeppelEinAus.equals("1")) {
                    // Kloeppel EIN
                    aktuelleRelaisNummerKloeppel = StaticVariable.laeutenKloeppelRelais.get(spalte - 4);
                    Log.e("Melodie" , "Kloeppel EIN - RelaisNummberKloeppel=" + aktuelleRelaisNummerKloeppel);
                    
                    if (aktuelleRelaisNummerKloeppel > 0) {
                        globalOnOffset = getGlobalOnOffset(aktuelleRelaisNummerKloeppel);
                        Log.e("Melodie" , "aktuell! RelaisOffset=" + globalOnOffset);
                        
                        if (globalOnOffset >= 0 && globalOnOffset < TurmtechnikActivity.globalOn.length) {
                            TurmtechnikActivity.globalOn[globalOnOffset] = true;
                            startGlockenSound(globalOnOffset);
                            
                            // Verwende changeRelais() statt direkt relaisNew[] zu setzen
                            if (aktuelleRelaisNummerKloeppel > 0 && aktuelleRelaisNummerKloeppel <= Serial_IoThread.relaisNew.length) {
                                Serial_IoThread.changeRelais(aktuelleRelaisNummerKloeppel, true);
                                Log.e("Melodie", "Relais " + aktuelleRelaisNummerKloeppel + " EIN geschaltet");
                            } else {
                                Log.e("Melodie", "FEHLER: Ungültige Relais-Nummer: " + aktuelleRelaisNummerKloeppel);
                            }
                        } else {
                            Log.e("Melodie", "FEHLER: Ungültiger globalOnOffset: " + globalOnOffset);
                        }
                    } else {
                        Log.e("Melodie", "FEHLER: Relais-Nummer ist 0 oder negativ: " + aktuelleRelaisNummerKloeppel);
                    }

                    // HINWEIS: Motor wird NICHT hier eingeschaltet, sondern durch das Vorschwingen-System
                    // (MelodieStartMotor-Thread), das in setStartMotor() gestartet wird.
                    // Das Vorschwingen startet automatisch vor dem Kloeppel-EIN, basierend auf der
                    // vorschwingZeitSekunden-Konfiguration.
                    // Meteor Code entfernt
                } else {
                    // Unbekannter Wert - loggen aber ignorieren
                    Log.e("Melodie", "Unbekannter kloeppelEinAus-Wert in Spalte " + spalte + ", Zeile " + zeile + ": '" + kloeppelEinAus + "'");
                }
            } catch (Exception e) {
                // new LogExcelError(spalte, zeile, pathUndfileNameMelodie, 0, sourceFileName , 151) ;
            }


            spalte++;

        }

    }

    private void setStartNextMotors(int momentaneZeile)
    {
        // suche von momentaner zeile bis zum naechsten Glocke ein
        Log.e("Melodie"  , "setStartNextMotors") ;
        int wait = 0 ;
        boolean[] kloeppelVerwendet = new boolean[maxGlockenAnzahl] ;
        for(int i = 0 ; i < maxGlockenAnzahl; i++)
        {
            kloeppelVerwendet[i] = false ;
        }
        for(int i = momentaneZeile ; i < maxZeilen ; i ++ )
        {


            //Log.e("Melodie"  , "inDerZeileIstEineGlocke i->Zeile=" + i + " wait=" + wait) ;
            int kloeppelTemp = (istInDerZeileGlockeEin(i)) ;

            if(kloeppelTemp > -1)
            {
                if (!(kloeppelVerwendet[kloeppelTemp]))
                {
                    Log.e("Melodie", "inDerZeileIstEineGlocke i->Zeile=" + i + " wait=" + wait + " kloeppelVerwendet-offset=" + kloeppelTemp);
                    kloeppelVerwendet[kloeppelTemp] = true;
                    setStartMotor(i-1, wait);
                }
            }
            int waittemp  = 0;
            try
            {
                waittemp = Integer.parseInt(excelread.getCellString(3, i ));
            } catch (Exception e)
            {
                e.printStackTrace();
            }
            waittemp = waittemp * 10; // hundertstel auf ms umrechnen
            wait = wait + waittemp ;

            Log.e("Melodie"  , "wait=" + wait) ;
        }
    }

    private int istInDerZeileGlockeEin(int zeileLocal)
    {
        int maxCellSpalten = ( (Math.min( (maxGlockenAnzahl  + 4 ), cellSpalten)) ); // die ersten 4 ueberspringen plus max 8 relais
        int spalteLocal = 4;
        String kloeppelEinAus = "" ;

        while (spalteLocal < maxCellSpalten && doRun)
        {
            kloeppelEinAus = "" ;

            try
            {
                kloeppelEinAus = excelread.getCellString(spalteLocal, zeileLocal);
                // Prüfe, ob die Zelle null oder leer ist
                if (kloeppelEinAus == null) {
                    kloeppelEinAus = "";
                }
            } catch (Exception e)
            {
                kloeppelEinAus = "";
                // Log nur bei unerwarteten Fehlern, nicht bei leeren Zellen
                if (!(e instanceof ArrayIndexOutOfBoundsException)) {
                    e.printStackTrace();
                }
            }
            
            // Log nur wenn Wert vorhanden ist, um Log-Spam zu reduzieren
            if (!kloeppelEinAus.isEmpty()) {
                Log.e("Melodie"  , "inDerZeileIstEineGlocke? Zeile=" + zeileLocal + " Spalte=" + spalteLocal + " kloeppelEinAus=" + kloeppelEinAus) ;
            }
            
            if (kloeppelEinAus.equals("1"))
            {
                return spalteLocal-4 ;
            }
            spalteLocal++ ;
        }

        return -1 ;
    }

    private void setStartMotor(int zeile, long wait)
    {
        zeile++; // naechste zeile analysieren

        Log.e("doStartMotor", "check" + "zeile=" + zeile);
        int spalte = 4;

        int globalOnOffset = 0; // eigener Index wegen der Taste 100 = Stop, und 101 = Automatik

        int aktuelleRelaisNummerKloeppel = 0;
        int aktuelleRelaisNummerMotor = 0;
        int maxCellSpalten = ( (Math.min( (maxGlockenAnzahl  + 4 ), cellSpalten)) ); // die ersten 4 ueberspringen plus max 8 relais

        long waitMinusVorschwingen = 0;

        if (zeile >= excelread.getCellZeilen()) {
            return;
        }


        // Pro Ausgang (Spalte) eigene Vorschwing-Zeit: Motor j schaltet (wait - vorschwingZeitSekunden[j]) ms vor dem Schlag ein.
        while (spalte < maxCellSpalten && doRun) {
            Log.e("doStartMotor", "Spalte= " + spalte);
            Log.e("doStartMotor", "Zeile=" + zeile);

            try {
                String kloeppelEinAus = excelread.getCellString(spalte, zeile);
                Log.e("doStartMotor", "kloeppelEinAus=" + kloeppelEinAus);
                if (kloeppelEinAus != null && kloeppelEinAus.equals("1")) {
                    int outputIndex = spalte - 4;
                    if (outputIndex < 0) {
                        spalte++;
                        continue;
                    }
                    // Vorschwing-Zeit und Motor-Relais pro Ausgang (outputIndex), nicht gemeinsam für alle
                    int relaisMotor = 0;
                    long vorschwingenMs = 0;
                    if (outputIndex < StaticVariable.vorschwingenMotorRelaisNeu.size()) {
                        relaisMotor = StaticVariable.vorschwingenMotorRelaisNeu.get(outputIndex);
                    }
                    vorschwingenMs = StaticVariable.getVorschwingSekundenForColumn(outputIndex) * 1000L;

                    waitMinusVorschwingen = wait - vorschwingenMs;
                    if (waitMinusVorschwingen < 0) {
                        Log.e("doStartMotor", "WARNUNG: Vorschwingen (" + vorschwingenMs + "ms) > Wartezeit (" + wait + "ms). Motor Ausgang " + outputIndex + " sofort.");
                        waitMinusVorschwingen = 0;
                    }

                    Log.e("doStartMotor", "Ausgang " + outputIndex + " wait=" + wait + "ms vorschwingen=" + vorschwingenMs + "ms -> Motor in " + waitMinusVorschwingen + "ms Relais=" + relaisMotor);
                    if (relaisMotor > 0) {
                        // Bei mehreren Nocken in einer Zeile (wait=0): kurze Verzögerung zwischen den Starts,
                        // damit nicht alle Relais exakt gleichzeitig schalten (Stromspitzen, Bus, Thread-Konkurrenz).
                        if (wait == 0 && waitMinusVorschwingen == 0) {
                            try { Thread.sleep(25); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
                        }
                        new MelodieStartMotor(waitMinusVorschwingen, relaisMotor).start();
                    }
                }
            } catch (Exception e) {
                // new LogExcelError(spalte, zeile, pathUndfileNameMelodie, 0, sourceFileName , 151) ;
            }
            spalte++;
        }
    }

    /**
     * Bei "0": Prüft pro Spalte, ob der Motor ausgeschaltet werden darf.
     * Ablauf pro Spalte (Klöppelfänger/Motor): Motor muss Vorschwingzeit vor dem nächsten "1" ein sein.
     * - Nächstes "1" in dieser Spalte suchen → T_next (Zeitpunkt Klöppel), T_motor = T_next − Vorschwingzeit.
     * - Ist T_motor schon vorbei (T_now >= T_motor) → Motor anlassen (return false).
     * - Sonst Motor darf aus; Wiedereinschaltung bei T_motor ist geplant (return true).
     * - Kein folgendes "1" mehr in dieser Spalte → Motor (Klöppelfänger) ausschalten (return true).
     */
    private boolean motorAusschaltenOk(int spalte, int zeile, long tNowMs) {
        Log.e("motorAus?", "spalte=" + spalte + " zeile=" + zeile + " tNowMs=" + tNowMs);
        // In dieser Spalte: Pause von Zeilenbeginn bis zum nächsten "1" (Summe Wartezeiten in ms)
        long pauseMs = 0;
        boolean naechstesEinsGefunden = false;

        try {
            // Nur diese Spalte prüfen – Ablauf ist pro Spalte unabhängig
            for (int i = zeile; i < excelread.getCellZeilen(); i++) {
                String cellVal = excelread.getCellString(spalte, i);
                String onOffString = (cellVal != null ? cellVal.trim() : "");
                Log.e("motor", "onOffString(i)=" + onOffString);
                if ("1".equals(onOffString)) {
                    naechstesEinsGefunden = true;
                    break;
                }
                String waitStr = excelread.getCellString(3, i); // Wartezeit in Hundertstelsekunden bis zur nächsten Zeile
                int waitHundertstel = 0;
                try {
                    waitHundertstel = Integer.parseInt(waitStr != null ? waitStr.trim() : "0");
                } catch (Exception e2) {
                    // ignore
                }
                pauseMs += waitHundertstel * 10L; // in ms
                Log.e("motor", "pauseMs=" + pauseMs);
            }
        } catch (Exception e) {
            naechstesEinsGefunden = false;
        }

        if (!naechstesEinsGefunden) {
            // Kein folgendes "1" in dieser Spalte → Motor (Klöppelfänger) für diese Spalte ausschalten
            Log.e("motor", "kein naechstes 1 in Spalte " + spalte + " -> Motor AUS (true)");
            return true;
        }

        // T_next = Zeitpunkt des nächsten "1", T_motor = wann Motor für diesen Schlag ein sein muss
        long tNextMs = tNowMs + pauseMs;
        long vorschwingMs = 0;
        int idx = spalte - 4;
        int sec = StaticVariable.getVorschwingSekundenForColumn(idx);
        vorschwingMs = sec * 1000L;
        long tMotorMs = tNextMs - vorschwingMs; // Zeitpunkt, an dem Motor wieder ein sein muss

        // Ist T_motor schon vorbei oder jetzt? → Motor anlassen (nicht aus). Sonst aus, Wiedereinschaltung bei T_motor ist geplant.
        boolean motorAusOk = tNowMs < tMotorMs;
        Log.e("motor", "tNextMs=" + tNextMs + " tMotorMs=" + tMotorMs + " vorschwingMs=" + vorschwingMs + " -> motorAusOk=" + motorAusOk);
        return motorAusOk;
    }

    private int getGlobalOnOffset(int relaisNumber)
    {
        int returnRelaisOffset = 0;

        for (int i = 0; i < TurmtechnikActivity.RELAIS_COUNT; i++) {
            if (TurmtechnikActivity.relaisNumber[i] == relaisNumber) {
                returnRelaisOffset = i;
                Log.e("getGlobalOnOffset", "=" + returnRelaisOffset);
                return returnRelaisOffset;
                //break ;
            }
        }

        Log.e("getGlobalOnOffset", "=" + returnRelaisOffset);

        return returnRelaisOffset;
    }

    private void schalteGlockenAus() {
        int cellSpalten = excelread.getCellSpalten() - 4;
        int relaisOffset = 0; // eigener Index wegen der Taste 100 = Stop, und 101 = Automatik
        for (int i = 0; i < cellSpalten; i++) {
            while (TurmtechnikActivity.relaisNumber[relaisOffset] > 99) {
                relaisOffset++; // suche naechstes echtes relais
            }

            int relaisNumberTemp = TurmtechnikActivity.relaisNumber[relaisOffset];
            //if(relaisNumberTemp < 100)
            //{
            TurmtechnikActivity.globalOn[i] = false;
            Serial_IoThread.relaisNew[relaisNumberTemp - 1] = false;
            //}

            //else
            //{
            //	cellSpalten ++ ; // wegen stop (= relais# 100) ueberspringen
            //}
            relaisOffset++;
        }
    }

    /*
    private void allMelodieRelaisOff() {
		ExcelRead excelread = new ExcelRead();
		try {
			excelread.openXlsSheet(TurmtechnikActivity.beschriftungTastenFileString, TurmtechnikActivity.BESCHRIFTUNG_GLOCKEN_SHEET_NEW);
		} catch (BiffException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
			new LogExcelError(-1, -1, TurmtechnikActivity.beschriftungTastenFileString, -1 , sourceFileName, 206) ;
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
			new LogExcelError(-1, -1, TurmtechnikActivity.beschriftungTastenFileString, -1 , sourceFileName, 210) ;
		}
	
			int zeilen = excelread.getCellZeilen() - 1;
			int aktuelleZeile = 0;
			for (int i = 0; i < zeilen; i++)
			{
				try	
				{
				
				
					if (!(excelread.getCellString(3, i + 1)).equals("null")) // relais
																	// vorhanden?
					{
						if (excelread.getCellString(1, i + 1).equals("Melodie")) 
						{
							TurmtechnikActivity.globalOn[aktuelleZeile] = false;
							int relaisNumber = Integer.parseInt(excelread
									.getCellString(3, i + 1));
							Serial_IoThread.relaisNew[relaisNumber-1] = false;
							// layout.buttonOff(aktuelleZeile);
						}
						aktuelleZeile++;
					}
				}
				catch (Exception e) 
				{
					new LogExcelError(3, i + 1, TurmtechnikActivity.beschriftungTastenFileString, TurmtechnikActivity.BESCHRIFTUNG_GLOCKEN_SHEET_NEW, sourceFileName, 237) ;
				}	
					
			}
		
		
				
		
		excelread.closeWorkbook();
	}
	*/

    // Meteor Code entfernt: sendTastenStatusMeteor()
    // Meteor Code entfernt: makeMapObject()

    private void waitRealTime(long ms) {
        if (ms == 0) {
            return;
        }

        long startMs = System.currentTimeMillis();
        long endMs = startMs + ms;

        while ((System.currentTimeMillis() < endMs) && (doRun == true)) {
            sleepMs(50);
        }

    }

    private void startGlockenSound(int index)
    {
        String soundName = null;
        if (TurmtechnikActivity.turmtechnikContext != null) {
            try {
                List<PlatinenDatabaseHelper.BeschriftungTastenRow> rows = PlatinenDatabaseHelper.getInstance(TurmtechnikActivity.turmtechnikContext).getBeschriftungTasten();
                if (rows != null && index >= 0 && index < rows.size()) {
                    PlatinenDatabaseHelper.BeschriftungTastenRow r = rows.get(index);
                    if (r.sound != null && !r.sound.trim().isEmpty()) {
                        soundName = r.sound.trim();
                    }
                    if (soundName != null && !soundName.isEmpty()) {
                        StaticVariable.soundGlocke.playGlockeSound(soundName, index, -1);
                    }
                    return;
                }
            } catch (Exception e) { }
        }
        ExcelRead excelread = null;
        try {
            excelread = new ExcelRead();
            excelread.openXls(beschriftungTastenFileString);
            soundName = excelread.getCellString(11, (index + 1));
        } catch (BiffException | IOException e1) {
            Log.e("Biff", "error");
            new LogExcelError(0, 0, normalprogrammFileString, -1, sourceFileName, 1627);
        } catch (Exception e) { e.printStackTrace(); }
        finally {
            try { if (excelread != null) excelread.closeWorkbook(); } catch (Exception e) { }
        }
        Log.e("soundGlocke", "name=" + soundName);
        if (soundName != null && !soundName.isEmpty()) {
            StaticVariable.soundGlocke.playGlockeSound(soundName, index, -1);
        }

        //String pathAndFilename = TurmtechnikActivity.sdCardPath + "/Turmtechnik/Sound/" + soundName ;
        //File file = new File(pathAndFilename) ;
        //MediaPlayer momentanSound = MediaPlayer.create(TurmtechnikActivity.this,Uri.fromFile(file)) ;
        //momentanSound.start();


    }

    private void stopGlockenSound(int index) {
        if (StaticVariable.soundGlocke != null) {
            StaticVariable.soundGlocke.stopGlockenSound(index);
        }
    }

    private void stopAllGlockenSounds() {


        for (int i = 0; i < StaticVariable.streamIDsList.size(); i++) {
            //Log.e("sound" , "soundIdList= " + StaticVariable.soundIDsList.get(i)) ;
            Log.e("sound", "streamIdList= " + StaticVariable.streamIDsList.get(i));
            //Log.e("sound" , "soundIndexList= " + StaticVariable.soundIndexList.get(i)) ;

            //stopGlockenSound(StaticVariable.soundIndexList.get(i));
            StaticVariable.soundPool2.stop(StaticVariable.streamIDsList.get(i));
        }
    }


    private void sleepMs(long ms) {
        long time = ms / 10;
        for (int i = 0; i < time; i++) {
            if (doRun == false) {
                break;
            }
            try {
                Thread.sleep(10);     //Thread.sleep(time);
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

    public static void doRunOff() {
        doRun = false;
        Log.e("MelodieThread", "doRunOff() aufgerufen - Melodie wird gestoppt");
    }
    
    /**
     * Prüft, ob eine Melodie aktuell läuft
     * @return true wenn eine Melodie aktiv ist, sonst false
     */
    public static boolean isRunning() {
        return doRun && StaticVariable.melodieAktiv && currentInstance != null;
    }
    
    /**
     * Gibt die aktuelle Thread-Instanz zurück
     * @return die aktuelle MelodieThread-Instanz oder null
     */
    public static MelodieThreadNew getCurrentInstance() {
        return currentInstance;
    }
    
    /**
     * Setzt nur die Dauer der aktuellen Zeile für Web-UI. Relais-Zustand kommt von gesendeten Werten (updateCurrentMelodieZeileRelaisFromSent).
     */
    private void buildZeilenInhaltForDisplay(ExcelRead excelread, int zeile) {
        if (excelread == null) return;
        try {
            StaticVariable.currentMelodieZeileDauer = "";
            if (StaticVariable.currentMelodieZeileRelais == null) StaticVariable.currentMelodieZeileRelais = new int[16];
            if (StaticVariable.currentMelodieZeileVorschwing == null) StaticVariable.currentMelodieZeileVorschwing = new int[16];
            for (int i = 0; i < 16; i++) { StaticVariable.currentMelodieZeileRelais[i] = 0; StaticVariable.currentMelodieZeileVorschwing[i] = 0; }
            String waitStr = excelread.getCellString(3, zeile);
            if (waitStr != null && !waitStr.trim().isEmpty() && !waitStr.equals("Zeit 100/sec")) {
                try {
                    int hundertstel = Integer.parseInt(waitStr.trim());
                    double sekunden = hundertstel / 100.0;
                    if (sekunden >= 0)
                        StaticVariable.currentMelodieZeileDauer = String.format(java.util.Locale.GERMAN, "%.1f s", sekunden);
                } catch (NumberFormatException ignored) { }
            }
        } catch (Exception e) {
            Log.e("MelodieThread", "buildZeilenInhaltForDisplay Fehler: " + e.getMessage());
        }
    }

    /**
     * Übernimmt den Zustand der Relais G1–G16 aus den tatsächlich gesendeten Werten (Serial_IoThread.relaisNew).
     * Muss nach schalteKloeppel() aufgerufen werden, damit die Anzeige den gesendeten Zustand zeigt.
     */
    private void updateCurrentMelodieZeileRelaisFromSent() {
        if (StaticVariable.currentMelodieZeileRelais == null) StaticVariable.currentMelodieZeileRelais = new int[16];
        if (StaticVariable.currentMelodieZeileVorschwing == null) StaticVariable.currentMelodieZeileVorschwing = new int[16];
        for (int i = 0; i < 16; i++) {
            StaticVariable.currentMelodieZeileRelais[i] = 0;
            StaticVariable.currentMelodieZeileVorschwing[i] = 0;
        }
        if (Serial_IoThread.relaisNew == null) return;
        if (StaticVariable.laeutenKloeppelRelais != null) {
            for (int i = 0; i < 16 && i < StaticVariable.laeutenKloeppelRelais.size(); i++) {
                int relaisNum = StaticVariable.laeutenKloeppelRelais.get(i);
                if (relaisNum >= 1 && relaisNum <= Serial_IoThread.relaisNew.length) {
                    Boolean on = Serial_IoThread.relaisNew[relaisNum - 1];
                    StaticVariable.currentMelodieZeileRelais[i] = (on != null && on) ? 1 : 0;
                }
            }
        }
        if (StaticVariable.vorschwingenMotorRelaisNeu != null) {
            for (int i = 0; i < 16 && i < StaticVariable.vorschwingenMotorRelaisNeu.size(); i++) {
                int relaisNum = StaticVariable.vorschwingenMotorRelaisNeu.get(i);
                if (relaisNum >= 1 && relaisNum <= Serial_IoThread.relaisNew.length) {
                    Boolean on = Serial_IoThread.relaisNew[relaisNum - 1];
                    StaticVariable.currentMelodieZeileVorschwing[i] = (on != null && on) ? 1 : 0;
                }
            }
        }
    }

    /**
     * Extrahiert den Melodienamen aus dem Dateipfad.
     * Beispiel: "/Turmtechnik/Melodien/Glocke1.xls" -> "Glocke1"
     */
    private String extractMelodieNameFromPath(String path) {
        if (path == null || path.isEmpty()) {
            return "";
        }
        
        // Extrahiere Dateiname
        String fileName = path;
        int lastSlash = path.lastIndexOf('/');
        if (lastSlash >= 0 && lastSlash < path.length() - 1) {
            fileName = path.substring(lastSlash + 1);
        }
        
        // Entferne .xls Endung
        if (fileName.toLowerCase().endsWith(".xls")) {
            fileName = fileName.substring(0, fileName.length() - 4);
        }
        
        return fileName;
    }
    
    /**
     * Erstellt ein virtuelles ExcelRead-Objekt aus Datenbank-Zeilen.
     * Emuliert die ExcelRead-Interface für die bestehende Logik.
     */
    private ExcelRead createVirtualExcelReadFromDatabase(List<PlatinenDatabaseHelper.MelodieZeile> zeilen) {
        return new DatabaseMelodieExcelRead(zeilen);
    }
    
    /**
     * Wrapper-Klasse, die ExcelRead emuliert, aber Daten aus der Datenbank liest.
     */
    private static class DatabaseMelodieExcelRead extends ExcelRead {
        private List<PlatinenDatabaseHelper.MelodieZeile> zeilen;
        private int maxSpalten = 22; // Spalte 0-21 (0-3: Metadaten, 4-19: Kloeppel, 20: Melodie, 21: Sound)
        
        public DatabaseMelodieExcelRead(List<PlatinenDatabaseHelper.MelodieZeile> zeilen) {
            this.zeilen = zeilen != null ? zeilen : new java.util.ArrayList<>();
        }
        
        @Override
        public String getCellString(int spalte, int zeile) throws ArrayIndexOutOfBoundsException, Exception {
            // Die bestehende Logik beginnt bei Index 1 zu suchen (nach Header-Zeilen)
            // In der Datenbank haben wir nur Datenzeilen (sortiert nach zeile_index)
            // Excel-Logik: Index 1 = erste Datenzeile, Index 0 = Header
            // DB-Logik: Index 0 = erste Datenzeile
            // Also: Excel zeile 1 -> DB Index 0, Excel zeile 2 -> DB Index 1, etc.
            int dbZeileIndex = zeile - 1;
            
            if (dbZeileIndex < 0 || dbZeileIndex >= this.zeilen.size()) {
                return "";
            }
            
            PlatinenDatabaseHelper.MelodieZeile melodieZeile = this.zeilen.get(dbZeileIndex);
            
            // Spalte 3: Wartezeit (in Hundertstel-Sekunden)
            // Konvertiere dauerSekunden zu Hundertstel: dauerSekunden * 100
            if (spalte == 3) {
                if (melodieZeile.dauerSekunden != null) {
                    int wartezeitHundertstel = (int) Math.round(melodieZeile.dauerSekunden * 100.0);
                    return String.valueOf(wartezeitHundertstel);
                }
                return "";
            }
            
            // Spalte 4-19: Kloeppel G1-G16
            if (spalte >= 4 && spalte <= 19) {
                int kloeppelIndex = spalte - 4; // 0-15 für A-P
                String[] kloeppelFields = {
                    "kloeppelA", "kloeppelB", "kloeppelC", "kloeppelD",
                    "kloeppelE", "kloeppelF", "kloeppelG", "kloeppelH",
                    "kloeppelI", "kloeppelJ", "kloeppelK", "kloeppelL",
                    "kloeppelM", "kloeppelN", "kloeppelO", "kloeppelP"
                };
                
                if (kloeppelIndex < kloeppelFields.length) {
                    try {
                        java.lang.reflect.Field field = melodieZeile.getClass().getField(kloeppelFields[kloeppelIndex]);
                        Object value = field.get(melodieZeile);
                        return value != null ? value.toString() : "";
                    } catch (Exception e) {
                        Log.e("DatabaseMelodieExcelRead", "Fehler beim Lesen von " + kloeppelFields[kloeppelIndex], e);
                        return "";
                    }
                }
            }
            
            // Spalte 20: Verschachtelte Melodie
            if (spalte == 20) {
                return melodieZeile.melodieXls != null ? melodieZeile.melodieXls : "";
            }
            
            // Spalte 21: Sound-MP3
            if (spalte == 21) {
                return melodieZeile.soundMp3 != null ? melodieZeile.soundMp3 : "";
            }
            
            // Alle anderen Spalten: leer
            return "";
        }
        
        @Override
        public int getCellSpalten() {
            return maxSpalten;
        }
        
        @Override
        public int getCellZeilen() {
            // Die bestehende Logik erwartet, dass Index 1+ die Datenzeilen sind
            // Index 0 = Header, Index 1+ = Datenzeilen
            // Also geben wir size() + 1 zurück, damit Index 0 (Header) und Index 1 bis size() (Daten) verfügbar sind
            return zeilen.size() + 1;
        }
        
        @Override
        public void closeWorkbook() {
            // Nichts zu schließen bei virtueller ExcelRead
            zeilen = null;
        }
    }
} // ende der Klasse
