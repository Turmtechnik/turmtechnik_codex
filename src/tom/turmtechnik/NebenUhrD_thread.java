package tom.turmtechnik;

import android.util.Log;

import java.util.Calendar;

/**
 * Thread für Monduhr D
 * 
 * LOGIK:
 * - 60 Impulse = 1 synodischer Monat (29.530588 Tage)
 * - Dauer zwischen Impulsen: 29.530588 / 60 = 0.492176 Tage ≈ 11.8 Stunden
 * - Nach 60 Impulsen wird wieder Neumond erreicht → Reset auf 0
 * - Impuls 0 = Neumond
 * - Impuls 30 = Vollmond (Tag 14.77)
 * - Impuls 60 = Neumond (wird zu 0, Tag 29.53)
 */
public class NebenUhrD_thread extends Thread {
    private final String sourceFileName = "NebenUhrD_thread";
    
    private static final double SYNODISCHER_MONAT_TAGE = 29.530588; // Tage für einen synodischen Monat
    // Referenz-Neumond: 30. Dezember 2025 00:00 UTC (korrigiert)
    private static final long REFERENZ_NEUMOND_MS = 1735603200000L; // 30. Dez 2025 00:00 UTC
    
    private PlatinenDatabaseHelper.NebenuhrConfig nebenuhrConfig; // Konfiguration aus Datenbank
    /** Letzter Zeitstempel (ms) für Log "Serial-Verbindung nicht OK" – nur alle 30 s loggen. */
    private long lastSerialNotOkLogMs = 0;
    private static final long SERIAL_NOT_OK_LOG_INTERVAL_MS = 30000; // 30 Sekunden

    public void run() {
        PlatinenDatabaseHelper dbHelper = PlatinenDatabaseHelper.getInstance(TurmtechnikActivity.turmtechnikContext);
        PlatinenDatabaseHelper.NebenuhrConfig config = dbHelper.getNebenuhrByZeile(6); // Zeile 6 = Uhr D (Monduhr)
        if (config == null) {
            Log.w("NebenUhrD_thread", "Monduhr D nicht in Datenbank gefunden. Bitte über Web-UI konfigurieren.");
            return;
        }
        this.nebenuhrConfig = config;
        
        // Lade lastRelaisA aus Datenbank
        StaticVariable.uhrD_lastRelaisA = config.lastRelaisA;
        StaticVariable.uhrD_mondphaseIst = config.mondphaseIst;

        Log.d("MonduhrD", "Konfiguration aus Datenbank geladen: Relais A=" + config.relaisA + ", B=" + config.relaisB);

        while (StaticVariable.uhrD_doRun == true) {
            // Prüfe, ob Konfiguration neu geladen werden muss (wenn über Web-UI geändert)
            if (StaticVariable.nebenuhrD_configNeuLaden) {
                Log.d("MonduhrD", "Konfiguration wurde geändert, lade neu...");
                PlatinenDatabaseHelper dbHelperReload = PlatinenDatabaseHelper.getInstance(TurmtechnikActivity.turmtechnikContext);
                PlatinenDatabaseHelper.NebenuhrConfig newConfig = dbHelperReload.getNebenuhrByZeile(6);
                if (newConfig != null) {
                    this.nebenuhrConfig = newConfig;
                    StaticVariable.uhrD_lastRelaisA = newConfig.lastRelaisA;
                    StaticVariable.uhrD_mondphaseIst = newConfig.mondphaseIst; // Mondphase beibehalten
                    Log.d("MonduhrD", "Konfiguration neu geladen: RelaisA=" + newConfig.relaisA + ", RelaisB=" + newConfig.relaisB + ", Impuls1=" + newConfig.impulsDauer1 + ", Impuls2=" + newConfig.impulsDauer2);
                }
                StaticVariable.nebenuhrD_configNeuLaden = false; // Flag zurücksetzen
            }
            
            // Prüfe ob Nebenuhr-Einstellungsseite offen ist (dann pausieren)
            if (StaticVariable.nebenuhrEinstellungsSeiteOffen) {
                Log.d("MonduhrD", "Nebenuhr-Einstellungsseite offen, pausiere");
                sleepTime(5000); // 5 Sekunden warten
                continue;
            }
            
            // Berechne aktuelle Mondphase (Soll) - wird alle 5 Minuten neu berechnet
            berechneMondphaseSoll();
            
            // Prüfe ob Synchronisation erreicht wurde
            if (StaticVariable.uhrD_mondphaseSoll == StaticVariable.uhrD_mondphaseIst) {
                // Berechne Zeit bis zum nächsten Impuls
                // 60 Impulse = 29.530588 Tage = 1 synodischer Monat
                // 1 Impuls = 29.530588 / 60 = 0.492 Tage ≈ 11.8 Stunden
                int impulseProMondphase = getImpulseProMondphase();
                double stundenProImpuls = (SYNODISCHER_MONAT_TAGE * 24.0) / impulseProMondphase;
                
                Log.d("MonduhrD", "Mondphase synchronisiert: Soll=" + StaticVariable.uhrD_mondphaseSoll + 
                      " Ist=" + StaticVariable.uhrD_mondphaseIst + 
                      " (Nächster Impuls in ca. " + String.format("%.1f", stundenProImpuls) + " Stunden, " +
                      "prüfe alle 5 Minuten)");
                
                // Warte 5 Minuten (entspricht der Berechnungsfrequenz)
                sleepTime(300000); // 5 Minuten warten
                continue;
            }
            
            // Berechne zyklische Differenz (kürzester Weg)
            int differenzMondphase = StaticVariable.uhrD_mondphaseSoll - StaticVariable.uhrD_mondphaseIst;
            if (differenzMondphase < 0) {
                differenzMondphase += getImpulseProMondphase();
            }
            
            Log.d("MonduhrD", "Mondphase Differenz: Soll=" + StaticVariable.uhrD_mondphaseSoll + 
                  " Ist=" + StaticVariable.uhrD_mondphaseIst + " Differenz=" + differenzMondphase + 
                  " Impulse nötig");
            
            // Impulse machen bis Mondphase passt
            while (StaticVariable.uhrD_doRun == true && 
                   StaticVariable.uhrD_mondphaseSoll != StaticVariable.uhrD_mondphaseIst &&
                   !StaticVariable.nebenuhrEinstellungsSeiteOffen) {
                
                if (StaticVariable.uhr_warten[3]) {
                    sleepTime(1000);
                    continue;
                }
                
                if (Serial_IoThread.getSerialIoStatus2()) // funkverbindung O.K. ?
                {
                    makeOneImpuls();
                    // Nach jedem Impuls erneut prüfen (Soll kann sich ändern)
                    berechneMondphaseSoll();
                } else {
                    // Nur alle 30 Sekunden loggen, um Log-Flut zu vermeiden
                    long now = System.currentTimeMillis();
                    if (now - lastSerialNotOkLogMs >= SERIAL_NOT_OK_LOG_INTERVAL_MS) {
                        lastSerialNotOkLogMs = now;
                        Log.w("MonduhrD", "Serial-Verbindung nicht OK, warte...");
                    }
                    sleepTime(1000);
                }
            }
            
            // Wenn synchronisiert, warte kurz bevor erneut geprüft wird
            if (StaticVariable.uhrD_mondphaseSoll == StaticVariable.uhrD_mondphaseIst) {
                Log.d("MonduhrD", "Mondphase synchronisiert, warte 1 Minute");
                sleepTime(60000); // 1 Minute warten
            }
        }

        // Speichere Mondphase beim Beenden
        try {
            PlatinenDatabaseHelper dbHelper2 = PlatinenDatabaseHelper.getInstance(TurmtechnikActivity.turmtechnikContext);
            PlatinenDatabaseHelper.NebenuhrConfig saveConfig = dbHelper2.getNebenuhrByZeile(6);
            if (saveConfig != null) {
                saveConfig.mondphaseIst = StaticVariable.uhrD_mondphaseIst;
                saveConfig.lastRelaisA = StaticVariable.uhrD_lastRelaisA;
                dbHelper2.saveNebenuhr(saveConfig);
            }
        } catch (Exception e) {
            Log.e("MonduhrD", "Fehler beim Speichern beim Beenden", e);
        }

        System.gc();
    }

    /**
     * Berechnet die aktuelle Mondphase (Soll) basierend auf dem aktuellen Datum
     * 0 = Neumond, 30 = Vollmond, 60 = Neumond (zyklisch)
     * Verwendet festen Referenz-Neumond: 30. Dezember 2025
     */
    private void berechneMondphaseSoll() {
        Calendar calendar = Calendar.getInstance();
        long aktuellMs = calendar.getTimeInMillis();
        
        // Berechne Tage seit Referenz-Neumond (30. Dezember 2025)
        long diffMs = aktuellMs - REFERENZ_NEUMOND_MS;
        double tageSeitNeumond = diffMs / (1000.0 * 60.0 * 60.0 * 24.0);
        
        // Wenn negativ, bedeutet das, dass wir vor dem Referenz-Neumond sind
        // In diesem Fall müssen wir einen ganzen synodischen Monat hinzufügen
        while (tageSeitNeumond < 0) {
            tageSeitNeumond += SYNODISCHER_MONAT_TAGE;
        }
        
        // Normalisiere auf 0-29.53 Tage (ein synodischer Monat)
        // WICHTIG: Wenn tageSeitNeumond > 29.53, dann ist ein ganzer Monat vergangen
        // → Modulo-Operation setzt automatisch auf 0 zurück (Neumond)
        tageSeitNeumond = tageSeitNeumond % SYNODISCHER_MONAT_TAGE;
        
        // Berechne Mondphase (0-59)
        // Formel: (Tage seit Neumond / 29.530588) * 60 Impulse
        // 0 = Neumond, 30 = Vollmond, 60 = Neumond (wird zu 0)
        int impulseProMondphase = getImpulseProMondphase();
        double mondphase = (tageSeitNeumond / SYNODISCHER_MONAT_TAGE) * impulseProMondphase;
        mondphase = mondphase % impulseProMondphase;
        if (mondphase < 0) mondphase += impulseProMondphase;
        
        StaticVariable.uhrD_mondphaseSoll = (int) Math.round(mondphase);
        // Sicherheitscheck: Wenn >= 60, dann auf 0 zurücksetzen (Neumond)
        if (StaticVariable.uhrD_mondphaseSoll >= impulseProMondphase) {
            StaticVariable.uhrD_mondphaseSoll = 0; // Neumond erreicht
        }
        
        // Nur alle 5 Minuten neu berechnen (um CPU zu sparen)
        if (StaticVariable.uhrD_letzteBerechnungMs == 0 || 
            (aktuellMs - StaticVariable.uhrD_letzteBerechnungMs) > (5 * 60 * 1000)) {
            StaticVariable.uhrD_letzteBerechnungMs = aktuellMs;
            Log.d("MonduhrD", "Mondphase Soll: " + StaticVariable.uhrD_mondphaseSoll + 
                  " (Tage seit Neumond: " + String.format("%.2f", tageSeitNeumond) + ")");
        }
    }

    public synchronized void makeOneImpuls() {
        if (nebenuhrConfig == null) {
            Log.e("MonduhrD", "Keine Konfiguration verfügbar!");
            return;
        }

        if (StaticVariable.uhr_warten[3] == true) {
            sleepTime(100);
            return;
        }

        // WEICHSELSCHALTUNG: Immer das ANDERE Relais verwenden als beim letzten Impuls!
        int relaisNumberToUse = 0;
        
        // WICHTIG: Wähle das ANDERE Relais als beim letzten Impuls
        // Dies verhindert, dass beide Relais gleichzeitig geschaltet werden
        // Die Wechselschaltung ist kritisch für bistabile Relais
        if (StaticVariable.uhrD_lastRelaisA) {
            // Letztes Relais war A → verwende jetzt B
            relaisNumberToUse = nebenuhrConfig.relaisB;
            StaticVariable.uhrD_lastRelaisA = false;
            Log.d("MonduhrD", "Wechselschaltung: Verwende Relais B (" + nebenuhrConfig.relaisB + ") - letztes war A (" + nebenuhrConfig.relaisA + ")");
        } else {
            // Letztes Relais war B (oder noch kein Impuls) → verwende jetzt A
            relaisNumberToUse = nebenuhrConfig.relaisA;
            StaticVariable.uhrD_lastRelaisA = true;
            Log.d("MonduhrD", "Wechselschaltung: Verwende Relais A (" + nebenuhrConfig.relaisA + ") - letztes war B (" + nebenuhrConfig.relaisB + ") oder Start");
        }
        
        // SICHERHEIT: Prüfe, dass nur EIN Relais verwendet wird
        if (relaisNumberToUse == 0) {
            Log.e("MonduhrD", "FEHLER: Kein gültiges Relais gefunden! relaisA=" + nebenuhrConfig.relaisA + " relaisB=" + nebenuhrConfig.relaisB);
            return;
        }
        
        // KRITISCH: Warte bis das ANDERE Relais fertig ist, bevor wir das neue einschalten!
        // Der laufende Impuls muss zu Ende laufen, nicht einfach abbrechen!
        int otherRelais = (relaisNumberToUse == nebenuhrConfig.relaisA) ? nebenuhrConfig.relaisB : nebenuhrConfig.relaisA;
        if (otherRelais > 0 && otherRelais <= Serial_IoThread.relaisNew.length) {
            if (Serial_IoThread.relaisNew[otherRelais - 1] != null && Serial_IoThread.relaisNew[otherRelais - 1] == true) {
                Log.w("MonduhrD", "WARNUNG: Anderes Relais (" + otherRelais + ") ist noch aktiv, warte bis Impuls fertig ist!");
                
                // Warte bis das andere Relais ausgeschaltet wird (mit Timeout)
                long timeoutMs = System.currentTimeMillis() + (nebenuhrConfig.impulsDauer1 + nebenuhrConfig.impulsDauer2) * 1000 + 1000; // Max. Impulsdauer + Pause + 1 Sekunde Puffer
                while (Serial_IoThread.relaisNew[otherRelais - 1] != null && 
                       Serial_IoThread.relaisNew[otherRelais - 1] == true &&
                       System.currentTimeMillis() < timeoutMs) {
                    sleepTime(10); // Kurz warten und erneut prüfen
                }
                
                if (Serial_IoThread.relaisNew[otherRelais - 1] != null && Serial_IoThread.relaisNew[otherRelais - 1] == true) {
                    Log.e("MonduhrD", "FEHLER: Anderes Relais (" + otherRelais + ") war nach Timeout noch aktiv! Breche ab.");
                    return; // Impuls abbrechen, da anderes Relais nicht fertig wurde
                } else {
                    Log.d("MonduhrD", "Anderes Relais (" + otherRelais + ") ist jetzt ausgeschaltet, kann neues Relais einschalten");
                }
            }
        }
        
        // SICHERHEITSPRÜFUNG: Prüfe, ob beide Relais gleichzeitig aktiv sind (kritischer Fehler!)
        if (nebenuhrConfig.relaisA > 0 && nebenuhrConfig.relaisA <= Serial_IoThread.relaisNew.length &&
            nebenuhrConfig.relaisB > 0 && nebenuhrConfig.relaisB <= Serial_IoThread.relaisNew.length) {
            boolean relaisAAktiv = (Serial_IoThread.relaisNew[nebenuhrConfig.relaisA - 1] != null && 
                                    Serial_IoThread.relaisNew[nebenuhrConfig.relaisA - 1] == true);
            boolean relaisBAktiv = (Serial_IoThread.relaisNew[nebenuhrConfig.relaisB - 1] != null && 
                                    Serial_IoThread.relaisNew[nebenuhrConfig.relaisB - 1] == true);
            
            if (relaisAAktiv && relaisBAktiv) {
                Log.e("MonduhrD", "KRITISCHER FEHLER: Beide Relais gleichzeitig aktiv! RelaisA=" + nebenuhrConfig.relaisA + 
                      ", RelaisB=" + nebenuhrConfig.relaisB + " - Warte bis beide fertig sind!");
                
                // Warte bis beide Relais ausgeschaltet werden (mit Timeout)
                long timeoutMs = System.currentTimeMillis() + (nebenuhrConfig.impulsDauer1 + nebenuhrConfig.impulsDauer2) * 1000 + 1000;
                while ((relaisAAktiv || relaisBAktiv) && System.currentTimeMillis() < timeoutMs) {
                    relaisAAktiv = (Serial_IoThread.relaisNew[nebenuhrConfig.relaisA - 1] != null && 
                                    Serial_IoThread.relaisNew[nebenuhrConfig.relaisA - 1] == true);
                    relaisBAktiv = (Serial_IoThread.relaisNew[nebenuhrConfig.relaisB - 1] != null && 
                                    Serial_IoThread.relaisNew[nebenuhrConfig.relaisB - 1] == true);
                    sleepTime(10);
                }
                
                if (relaisAAktiv || relaisBAktiv) {
                    Log.e("MonduhrD", "FEHLER: Relais waren nach Timeout noch aktiv! Breche ab.");
                    return; // Impuls abbrechen
                } else {
                    Log.d("MonduhrD", "Beide Relais sind jetzt ausgeschaltet, kann neues Relais einschalten");
                }
            }
        }
        
        // Sende Impuls mit dem gewählten Relais (NUR EIN Relais!)
        long impulseStartTimeMs = android.os.SystemClock.elapsedRealtime();
        int platineIndex = (relaisNumberToUse - 1) / 32;
        if (platineIndex >= 0 && platineIndex < StaticVariable.nebenuhrImpulsLaeuftPlatine.length) {
            StaticVariable.nebenuhrImpulsLaeuftPlatine[platineIndex] = true;
        }
        try {
            Serial_IoThread.relaisNew[relaisNumberToUse - 1] = true;  // relais einschalten
            String relaisLetterD = (relaisNumberToUse == nebenuhrConfig.relaisA) ? "A" : "B";
            LogTurmtechnik2.appendNebenuhrRelaisLogRelaisEinAus("D", relaisLetterD, true);
            // ZUSÄTZLICHE SICHERHEITSPRÜFUNG: Nach dem Einschalten prüfen, ob beide Relais aktiv sind
            if (nebenuhrConfig.relaisA > 0 && nebenuhrConfig.relaisA <= Serial_IoThread.relaisNew.length &&
                nebenuhrConfig.relaisB > 0 && nebenuhrConfig.relaisB <= Serial_IoThread.relaisNew.length) {
                boolean relaisAAktiv = (Serial_IoThread.relaisNew[nebenuhrConfig.relaisA - 1] != null && 
                                        Serial_IoThread.relaisNew[nebenuhrConfig.relaisA - 1] == true);
                boolean relaisBAktiv = (Serial_IoThread.relaisNew[nebenuhrConfig.relaisB - 1] != null && 
                                        Serial_IoThread.relaisNew[nebenuhrConfig.relaisB - 1] == true);
                
                if (relaisAAktiv && relaisBAktiv) {
                    Log.e("MonduhrD", "KRITISCHER FEHLER NACH EINSCHALTEN: Beide Relais gleichzeitig aktiv! " +
                          "RelaisA=" + nebenuhrConfig.relaisA + ", RelaisB=" + nebenuhrConfig.relaisB + 
                          " - Warte bis anderes Relais fertig ist!");
                    
                    // Warte bis das andere Relais fertig ist (nicht einfach ausschalten!)
                    int otherRelaisAfter = (relaisNumberToUse == nebenuhrConfig.relaisA) ? nebenuhrConfig.relaisB : nebenuhrConfig.relaisA;
                    long timeoutMs = System.currentTimeMillis() + (nebenuhrConfig.impulsDauer1 + nebenuhrConfig.impulsDauer2) * 1000 + 1000;
                    while (otherRelaisAfter > 0 && otherRelaisAfter <= Serial_IoThread.relaisNew.length &&
                           Serial_IoThread.relaisNew[otherRelaisAfter - 1] != null && 
                           Serial_IoThread.relaisNew[otherRelaisAfter - 1] == true &&
                           System.currentTimeMillis() < timeoutMs) {
                        sleepTime(10);
                    }
                    
                    // Prüfe erneut, ob beide noch aktiv sind
                    relaisAAktiv = (Serial_IoThread.relaisNew[nebenuhrConfig.relaisA - 1] != null && 
                                    Serial_IoThread.relaisNew[nebenuhrConfig.relaisA - 1] == true);
                    relaisBAktiv = (Serial_IoThread.relaisNew[nebenuhrConfig.relaisB - 1] != null && 
                                    Serial_IoThread.relaisNew[nebenuhrConfig.relaisB - 1] == true);
                    
                    if (relaisAAktiv && relaisBAktiv) {
                        Log.e("MonduhrD", "FEHLER: Beide Relais nach Wartezeit noch aktiv! Breche Impuls ab.");
                        // JETZT erst ausschalten, da Timeout erreicht
                        Serial_IoThread.relaisNew[relaisNumberToUse - 1] = false;
                        LogTurmtechnik2.appendNebenuhrRelaisLogRelaisEinAus("D", relaisLetterD, false);
                        if (platineIndex >= 0 && platineIndex < StaticVariable.nebenuhrImpulsLaeuftPlatine.length) {
                            StaticVariable.nebenuhrImpulsLaeuftPlatine[platineIndex] = false;
                        }
                        return; // Impuls abbrechen
                    }
                }
            }
            
            sleepTime(nebenuhrConfig.impulsDauer1 * 1000);            // warte impuls dauer (Sekunden → Millisekunden)
            Serial_IoThread.relaisNew[relaisNumberToUse - 1] = false; // relais wieder ausschalten
            LogTurmtechnik2.appendNebenuhrRelaisLogRelaisEinAus("D", relaisLetterD, false);
            sleepTime(nebenuhrConfig.impulsDauer2 * 1000);            // warte impuls pause (Sekunden → Millisekunden)

            if (platineIndex >= 0 && platineIndex < StaticVariable.nebenuhrImpulsLaeuftPlatine.length) {
                StaticVariable.nebenuhrImpulsLaeuftPlatine[platineIndex] = false;
            }
            if (Serial_IoThread.hadCarambolaErrorSince(platineIndex, impulseStartTimeMs)) {
                Log.e("MonduhrD", "Carambola-Fehler während Nebenuhr-Impuls (Platine " + (platineIndex + 1) + ") – Impuls wird nicht gezählt, wird wiederholt.");
                if (relaisNumberToUse > 0 && relaisNumberToUse <= Serial_IoThread.relaisOld.length) {
                    Serial_IoThread.relaisOld[relaisNumberToUse - 1] = false;
                }
                try {
                    PlatinenDatabaseHelper dbHelper = PlatinenDatabaseHelper.getInstance(TurmtechnikActivity.turmtechnikContext);
                    PlatinenDatabaseHelper.NebenuhrConfig c = dbHelper.getNebenuhrByZeile(6);
                    if (c != null) {
                        c.mondphaseIst = StaticVariable.uhrD_mondphaseIst;
                        c.lastRelaisA = StaticVariable.uhrD_lastRelaisA;
                        dbHelper.saveNebenuhr(c);
                    }
                } catch (Exception e) {
                    Log.e("MonduhrD", "Fehler beim Zurücksetzen der DB nach Carambola-Fehler", e);
                }
                return;
            }
            // Nur bei erfolgreichem Impuls Mondphase inkrementieren
            incrementMondphaseIst();
            
            // WICHTIG: Sofort nach erfolgreichem Impuls in Datenbank speichern
            try {
                PlatinenDatabaseHelper dbHelper = PlatinenDatabaseHelper.getInstance(TurmtechnikActivity.turmtechnikContext);
                PlatinenDatabaseHelper.NebenuhrConfig saveConfig = dbHelper.getNebenuhrByZeile(6);
                if (saveConfig != null) {
                    saveConfig.mondphaseIst = StaticVariable.uhrD_mondphaseIst;
                    saveConfig.lastRelaisA = StaticVariable.uhrD_lastRelaisA;
                    dbHelper.saveNebenuhr(saveConfig);
                    Log.d("MonduhrD", "Mondphase nach erfolgreichem Impuls gespeichert: " + StaticVariable.uhrD_mondphaseIst);
                }
            } catch (Exception e) {
                Log.e("MonduhrD", "Fehler beim Speichern nach Impuls: " + e.getMessage());
            }
        } catch (Exception e) {
            Log.e("MonduhrD", "Fehler beim Senden des Impulses: " + e.getMessage());
        } finally {
            if (platineIndex >= 0 && platineIndex < StaticVariable.nebenuhrImpulsLaeuftPlatine.length) {
                StaticVariable.nebenuhrImpulsLaeuftPlatine[platineIndex] = false;
            }
        }
    }

    /**
     * Erhöht die Ist-Mondphase um 1 Impuls
     * Nach 60 Impulsen (einem synodischen Monat) wird auf 0 zurückgesetzt (Neumond)
     */
    private void incrementMondphaseIst() {
        StaticVariable.uhrD_mondphaseIst++;
        // Nach 60 Impulsen = 29.530588 Tage → Neumond erreicht → Reset auf 0
        if (StaticVariable.uhrD_mondphaseIst >= getImpulseProMondphase()) {
            StaticVariable.uhrD_mondphaseIst = 0; // Zurück zu Neumond
        }
        Log.d("MonduhrD", "Mondphase Ist: " + StaticVariable.uhrD_mondphaseIst + 
              " (Soll: " + StaticVariable.uhrD_mondphaseSoll + ")");
    }

    private int getImpulseProMondphase() {
        PlatinenDatabaseHelper dbHelper = PlatinenDatabaseHelper.getInstance(TurmtechnikActivity.turmtechnikContext);
        return dbHelper.getMondImpulseProPhase();
    }

    private void sleepTime(long time) {
        try {
            Thread.sleep(time);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
