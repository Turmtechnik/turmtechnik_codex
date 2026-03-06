package tom.turmtechnik;

import android.util.Log;

import java.util.Calendar;

public class NebenUhrA_thread extends Thread {
    /** Nach Abbruch gesetzt; beim nächsten makeOneImpuls wird „Wiederholung“ ins Log geschrieben. */
    private static boolean lastAbortA = false;

    private final String sourceFileName = "NebenUhrA_thread";
    int relaisNumber;
    private PlatinenDatabaseHelper.NebenuhrConfig nebenuhrConfig; // Konfiguration aus Datenbank

    public void run() {
        PlatinenDatabaseHelper dbHelper = PlatinenDatabaseHelper.getInstance(TurmtechnikActivity.turmtechnikContext);
        PlatinenDatabaseHelper.NebenuhrConfig config = dbHelper.getNebenuhrByZeile(3); // Zeile 3 = Uhr A

        if (config == null) {
            Log.w("NebenUhrA_thread", "Nebenuhr A nicht in Datenbank. Nutzen Sie Web-UI oder 'Import alter Anlagen'.");
            StaticVariable.uhrA_doRun = false;
            return;
        }

        this.nebenuhrConfig = config;
        StaticVariable.uhrA_lastRelaisA = config.lastRelaisA;

        //Log.d("Nebenuhr A Thread" , "Konfiguration aus Datenbank geladen");

        while (StaticVariable.uhrA_doRun == true) {
            // Prüfe, ob Konfiguration neu geladen werden muss (wenn über Web-UI geändert)
            if (StaticVariable.nebenuhrA_configNeuLaden) {
                Log.d("NebenUhrA_thread", "Konfiguration wurde geändert, lade neu...");
                PlatinenDatabaseHelper dbHelperReload = PlatinenDatabaseHelper.getInstance(TurmtechnikActivity.turmtechnikContext);
                PlatinenDatabaseHelper.NebenuhrConfig newConfig = dbHelperReload.getNebenuhrByZeile(3);
                if (newConfig != null) {
                    this.nebenuhrConfig = newConfig;
                    StaticVariable.uhrA_lastRelaisA = newConfig.lastRelaisA;
                    Log.d("NebenUhrA_thread", "Konfiguration neu geladen: RelaisA=" + newConfig.relaisA + ", RelaisB=" + newConfig.relaisB + ", Impuls1=" + newConfig.impulsDauer1 + ", Impuls2=" + newConfig.impulsDauer2);
                    if (!newConfig.aktiv) {
                        Log.d("NebenUhrA_thread", "Nebenuhr A auf inaktiv gestellt, Thread beendet.");
                        StaticVariable.uhrA_doRun = false;
                        break;
                    }
                }
                StaticVariable.nebenuhrA_configNeuLaden = false; // Flag zurücksetzen
            }
            
            // Impulse machen bis Uhrzeit passt (Aufholen durchlaufen lassen).
            // Bei Verbindungsfehler trotzdem makeOneImpuls() aufrufen – sonst gehen Impulse verloren (kein Wiederholversuch).
            while (checkSollIstZeit() == true) {
                if (StaticVariable.nebenuhrA_configNeuLaden) break;
                if (!StaticVariable.uhrA_doRun || (StaticVariable.uhr_zeiteingabeAktiv != null && StaticVariable.uhr_zeiteingabeAktiv.length > 0 && StaticVariable.uhr_zeiteingabeAktiv[0])) break;
                makeOneImpuls();
                if (!Serial_IoThread.getSerialIoStatus2()) {
                    sleepTime(200);
                }
            }

            // Gleichstand erreicht – bis zur nächsten Minute warten (oder sofort weiter bei uhrA_aufholenAnfordern)
            if (StaticVariable.uhrA_doRun) {
                try {
                    java.util.Calendar cal = java.util.Calendar.getInstance();
                    int sec = cal.get(java.util.Calendar.SECOND);
                    int ms = cal.get(java.util.Calendar.MILLISECOND);
                    long sleepMs = (60 - sec) * 1000L - ms + 400;
                    if (sleepMs > 0 && sleepMs < 61000) {
                        final long chunkMs = 300L;
                        while (sleepMs > 0 && StaticVariable.uhrA_doRun) {
                            if (StaticVariable.uhrA_aufholenAnfordern) {
                                StaticVariable.uhrA_aufholenAnfordern = false;
                                break;
                            }
                            long s = Math.min(chunkMs, sleepMs);
                            sleepTime(s);
                            sleepMs -= s;
                        }
                    }
                    // Kurz warten, damit UhrThread/NebenUhrThread die calendarZeit zur vollen Minute
                    // aktualisiert hat – verhindert fehlenden Impuls („eine Minute fehlt“).
                    if (StaticVariable.uhrA_doRun) {
                        sleepTime(500);
                    }
                } catch (Exception e) { /* weiter in der Schleife */ }
            }
        }
        StaticVariable.uhrA_doRun = false;
        System.gc();
    } // ende von run

    /** Aktuelle Echtzeit in Minuten (12h, 0–719). Damit Aufholen nicht von veralteter uhrA_calendarZeit abhängt. */
    private static int getCurrentCalendarMinuten12() {
        Calendar cal = Calendar.getInstance();
        return cal.get(Calendar.HOUR) * 60 + cal.get(Calendar.MINUTE);
    }

    private boolean checkSollIstZeit() {
        int soll = Math.max(StaticVariable.uhrA_calendarZeit, getCurrentCalendarMinuten12());
        if (StaticVariable.uhrA_angezeigteZeit == soll) {
            StaticVariable.uhr_warten[0] = false;
            return false; // soll - ist zeit ist O.K.
        } else {
            if (StaticVariable.uhr_warten[0]) {
                return false;  // auf naechste Minute warten
            } else {
                return true; // weitermachen mit impulsen
            }
        }
    }

    public synchronized void makeOneImpuls() {
        int timeTemp;

        if (StaticVariable.uhr_warten[0] == true) {
            //Log.i("uhr_warten" , "A = true ") ;
            sleepTime(100);
            return;
        }

        if (nebenuhrConfig == null) {
            Log.e("NebenUhrA_thread", "Keine Konfiguration verfügbar!");
            return;
        }
        
        // WEICHSELSCHALTUNG: Immer das ANDERE Relais als beim letzten abgeschlossenen Impuls.
        // WICHTIG: uhrA_lastRelaisA erst NACH erfolgreichem Impuls setzen – sonst wechselt die Anzeige/Relaiswahl mitten im Impuls!
        int relaisNumberToUse = 0;
        final boolean nextLastRelaisA;  // Wert, den lastRelaisA nach diesem Impuls haben soll (nur bei Erfolg übernehmen)
        if (StaticVariable.uhrA_lastRelaisA) {
            relaisNumberToUse = nebenuhrConfig.relaisB;
            nextLastRelaisA = false;
        } else {
            relaisNumberToUse = nebenuhrConfig.relaisA;
            nextLastRelaisA = true;
        }
        
        // Kein Relais zugewiesen (0 = Keins) → keinen Impuls ausführen
        if (relaisNumberToUse <= 0) {
            return;
        }
        // SICHERHEIT: Prüfe, dass nur EIN Relais verwendet wird
        if (relaisNumberToUse > 0) {
            // KRITISCH: Warte bis das ANDERE Relais fertig ist, bevor wir das neue einschalten!
            // Der laufende Impuls muss zu Ende laufen, nicht einfach abbrechen!
            int otherRelais = (relaisNumberToUse == nebenuhrConfig.relaisA) ? nebenuhrConfig.relaisB : nebenuhrConfig.relaisA;
            if (otherRelais > 0 && otherRelais <= Serial_IoThread.relaisNew.length) {
                if (Serial_IoThread.relaisNew[otherRelais - 1] != null && Serial_IoThread.relaisNew[otherRelais - 1] == true) {
                    Log.w("NebenUhrA_thread", "WARNUNG: Anderes Relais (" + otherRelais + ") ist noch aktiv, warte bis Impuls fertig ist!");
                    
                    // Warte bis das andere Relais ausgeschaltet wird (mit Timeout)
                    long timeoutMs = System.currentTimeMillis() + (nebenuhrConfig.impulsDauer1 + nebenuhrConfig.impulsDauer2) * 1000 + 1000;
                    while (Serial_IoThread.relaisNew[otherRelais - 1] != null && 
                           Serial_IoThread.relaisNew[otherRelais - 1] == true &&
                           System.currentTimeMillis() < timeoutMs) {
                        sleepTime(10);
                    }
                    
                    if (Serial_IoThread.relaisNew[otherRelais - 1] != null && Serial_IoThread.relaisNew[otherRelais - 1] == true) {
                        Log.e("NebenUhrA_thread", "FEHLER: Anderes Relais (" + otherRelais + ") war nach Timeout noch aktiv! Breche ab.");
                        return;
                    } else {
                        Log.d("NebenUhrA_thread", "Anderes Relais (" + otherRelais + ") ist jetzt ausgeschaltet");
                    }
                }
            }
            
            // SICHERHEITSPRÜFUNG: Prüfe, ob beide Relais gleichzeitig aktiv sind
            if (nebenuhrConfig.relaisA > 0 && nebenuhrConfig.relaisA <= Serial_IoThread.relaisNew.length &&
                nebenuhrConfig.relaisB > 0 && nebenuhrConfig.relaisB <= Serial_IoThread.relaisNew.length) {
                boolean relaisAAktiv = (Serial_IoThread.relaisNew[nebenuhrConfig.relaisA - 1] != null && 
                                        Serial_IoThread.relaisNew[nebenuhrConfig.relaisA - 1] == true);
                boolean relaisBAktiv = (Serial_IoThread.relaisNew[nebenuhrConfig.relaisB - 1] != null && 
                                        Serial_IoThread.relaisNew[nebenuhrConfig.relaisB - 1] == true);
                
                if (relaisAAktiv && relaisBAktiv) {
                    Log.e("NebenUhrA_thread", "KRITISCHER FEHLER: Beide Relais gleichzeitig aktiv! Warte bis beide fertig sind!");
                    long timeoutMs = System.currentTimeMillis() + (nebenuhrConfig.impulsDauer1 + nebenuhrConfig.impulsDauer2) * 1000 + 1000;
                    while ((relaisAAktiv || relaisBAktiv) && System.currentTimeMillis() < timeoutMs) {
                        relaisAAktiv = (Serial_IoThread.relaisNew[nebenuhrConfig.relaisA - 1] != null && 
                                        Serial_IoThread.relaisNew[nebenuhrConfig.relaisA - 1] == true);
                        relaisBAktiv = (Serial_IoThread.relaisNew[nebenuhrConfig.relaisB - 1] != null && 
                                        Serial_IoThread.relaisNew[nebenuhrConfig.relaisB - 1] == true);
                        sleepTime(10);
                    }
                    if (relaisAAktiv || relaisBAktiv) {
                        Log.e("NebenUhrA_thread", "FEHLER: Relais waren nach Timeout noch aktiv! Breche ab.");
                        return;
                    }
                }
            }
            
            relaisNumber = relaisNumberToUse;
            if (lastAbortA) {
                LogTurmtechnik2.appendNebenuhrRelaisLogWiederholung("A", relaisNumber, StaticVariable.uhrA_calendarZeit, StaticVariable.uhrA_angezeigteZeit, StaticVariable.uhrA_lastRelaisA);
                lastAbortA = false;
            }
            // Vorab nur lastRelaisA + impulsAusstehend setzen, NICHT angezeigteZeit erhöhen.
            // Sonst: Neustart nach Vorab-Save aber vor Send → DB hat schon neue Zeit, Uhr hat Impuls nie bekommen → Minute geht verloren.
            // angezeigteZeit wird erst nach erfolgreichem Impuls gespeichert.
            if (Serial_IoThread.getSerialIoStatus2()) {
                try {
                    PlatinenDatabaseHelper dbHelper = PlatinenDatabaseHelper.getInstance(TurmtechnikActivity.turmtechnikContext);
                    PlatinenDatabaseHelper.NebenuhrConfig saveConfig = dbHelper.getNebenuhrByZeile(3);
                    if (saveConfig != null) {
                        saveConfig.lastRelaisA = nextLastRelaisA;
                        saveConfig.impulsAusstehend = true;
                        dbHelper.saveNebenuhr(saveConfig);
                    }
                } catch (Exception e) {
                    Log.e("NebenUhrA_thread", "Fehler beim Vorab-Speichern vor Impuls", e);
                }
            }
            //Log.i("Relais" , "Nr.:" + relaisNumber);
            long impulseStartTimeMs = android.os.SystemClock.elapsedRealtime();
            int platineIndex = (relaisNumber - 1) / 32;
            if (platineIndex >= 0 && platineIndex < StaticVariable.nebenuhrImpulsLaeuftPlatine.length) {
                StaticVariable.nebenuhrImpulsLaeuftPlatine[platineIndex] = true;
            }
            Serial_IoThread.relaisNew[relaisNumber - 1] = true;                // relais einschalten
            String relaisLetterA = (relaisNumber == nebenuhrConfig.relaisA) ? "A" : "B";
            LogTurmtechnik2.appendNebenuhrRelaisLogRelaisEinAus("A", relaisLetterA, true);
            // ZUSÄTZLICHE SICHERHEITSPRÜFUNG: Nach dem Einschalten prüfen
            if (nebenuhrConfig.relaisA > 0 && nebenuhrConfig.relaisA <= Serial_IoThread.relaisNew.length &&
                nebenuhrConfig.relaisB > 0 && nebenuhrConfig.relaisB <= Serial_IoThread.relaisNew.length) {
                boolean relaisAAktiv = (Serial_IoThread.relaisNew[nebenuhrConfig.relaisA - 1] != null && 
                                        Serial_IoThread.relaisNew[nebenuhrConfig.relaisA - 1] == true);
                boolean relaisBAktiv = (Serial_IoThread.relaisNew[nebenuhrConfig.relaisB - 1] != null && 
                                        Serial_IoThread.relaisNew[nebenuhrConfig.relaisB - 1] == true);
                
                if (relaisAAktiv && relaisBAktiv) {
                    Log.e("NebenUhrA_thread", "KRITISCHER FEHLER NACH EINSCHALTEN: Beide Relais aktiv! Warte bis anderes fertig ist!");
                    int otherRelaisAfter = (relaisNumber == nebenuhrConfig.relaisA) ? nebenuhrConfig.relaisB : nebenuhrConfig.relaisA;
                    long timeoutMs = System.currentTimeMillis() + (nebenuhrConfig.impulsDauer1 + nebenuhrConfig.impulsDauer2) * 1000 + 1000;
                    while (otherRelaisAfter > 0 && otherRelaisAfter <= Serial_IoThread.relaisNew.length &&
                           Serial_IoThread.relaisNew[otherRelaisAfter - 1] != null && 
                           Serial_IoThread.relaisNew[otherRelaisAfter - 1] == true &&
                           System.currentTimeMillis() < timeoutMs) {
                        sleepTime(10);
                    }
                    relaisAAktiv = (Serial_IoThread.relaisNew[nebenuhrConfig.relaisA - 1] != null && 
                                    Serial_IoThread.relaisNew[nebenuhrConfig.relaisA - 1] == true);
                    relaisBAktiv = (Serial_IoThread.relaisNew[nebenuhrConfig.relaisB - 1] != null && 
                                    Serial_IoThread.relaisNew[nebenuhrConfig.relaisB - 1] == true);
                    if (relaisAAktiv && relaisBAktiv) {
                        Log.e("NebenUhrA_thread", "FEHLER: Beide Relais nach Wartezeit noch aktiv! Breche Impuls ab.");
                        Serial_IoThread.relaisNew[relaisNumber - 1] = false;
                        LogTurmtechnik2.appendNebenuhrRelaisLogRelaisEinAus("A", relaisLetterA, false);
                        LogTurmtechnik2.appendNebenuhrRelaisLogAbbruch("A", relaisNumber, StaticVariable.uhrA_calendarZeit, StaticVariable.uhrA_angezeigteZeit, StaticVariable.uhrA_lastRelaisA);
                        lastAbortA = true;
                        if (platineIndex >= 0 && platineIndex < StaticVariable.nebenuhrImpulsLaeuftPlatine.length) {
                            StaticVariable.nebenuhrImpulsLaeuftPlatine[platineIndex] = false;
                        }
                        return;
                    }
                }
            }
            
            sleepTime(nebenuhrConfig.impulsDauer1 * 1000);                     // warte impuls dauer (Sekunden → Millisekunden)
            Serial_IoThread.relaisNew[relaisNumber - 1] = false;                // relais wieder ausschalten
            LogTurmtechnik2.appendNebenuhrRelaisLogRelaisEinAus("A", relaisLetterA, false);
            sleepTime(nebenuhrConfig.impulsDauer2 * 1000);                      // warte impuls pause (Sekunden → Millisekunden)

            if (platineIndex >= 0 && platineIndex < StaticVariable.nebenuhrImpulsLaeuftPlatine.length) {
                StaticVariable.nebenuhrImpulsLaeuftPlatine[platineIndex] = false;
            }
            // Impuls nur zählen wenn Carambola-Verbindung während des Impulses OK war (kein Fehler)
            if (Serial_IoThread.hadCarambolaErrorSince(platineIndex, impulseStartTimeMs)) {
                Log.e("NebenUhrA_thread", "Carambola-Fehler während Nebenuhr-Impuls (Platine " + (platineIndex + 1) + ") – Impuls wird nicht gezählt, wird wiederholt.");
                // relaisOld zurücksetzen, damit die Wiederholung als Änderung (false→true) erkannt wird und Carambola erneut sendet
                if (relaisNumber > 0 && relaisNumber <= Serial_IoThread.relaisOld.length) {
                    Serial_IoThread.relaisOld[relaisNumber - 1] = false;
                }
                try {
                    PlatinenDatabaseHelper dbHelper = PlatinenDatabaseHelper.getInstance(TurmtechnikActivity.turmtechnikContext);
                    PlatinenDatabaseHelper.NebenuhrConfig c = dbHelper.getNebenuhrByZeile(3);
                    if (c != null) {
                        c.angezeigteZeit = StaticVariable.uhrA_angezeigteZeit;
                        c.lastRelaisA = StaticVariable.uhrA_lastRelaisA;
                        c.impulsAusstehend = false;
                        dbHelper.saveNebenuhr(c);
                    }
                } catch (Exception e) {
                    Log.e("NebenUhrA_thread", "Fehler beim Zurücksetzen der DB nach Carambola-Fehler", e);
                }
                LogTurmtechnik2.appendNebenuhrRelaisLogImpulsFehler("A", StaticVariable.uhrA_angezeigteZeit, StaticVariable.uhrA_calendarZeit, StaticVariable.uhrA_lastRelaisA);
                return;
            }
            // „Ist“ auf neuen Wert setzen; erst JETZT Relais wechseln (Impuls war erfolgreich).
            if (Serial_IoThread.getSerialIoStatus2() && StaticVariable.uhrA_angezeigteZeit != StaticVariable.uhrA_calendarZeit) {
                incrementAngezeigteZeit_A();
                StaticVariable.uhrA_lastRelaisA = nextLastRelaisA;  // Relais-Wechsel nur nach fertigem Impuls!
                try {
                    PlatinenDatabaseHelper dbHelper = PlatinenDatabaseHelper.getInstance(TurmtechnikActivity.turmtechnikContext);
                    PlatinenDatabaseHelper.NebenuhrConfig c = dbHelper.getNebenuhrByZeile(3);
                    if (c != null) {
                        c.angezeigteZeit = StaticVariable.uhrA_angezeigteZeit;
                        c.lastRelaisA = StaticVariable.uhrA_lastRelaisA;
                        c.impulsAusstehend = false;
                        dbHelper.saveNebenuhr(c);
                    }
                } catch (Exception e) {
                    Log.e("NebenUhrA_thread", "Fehler beim Löschen impulsAusstehend nach Impuls", e);
                }
                LogTurmtechnik2.appendNebenuhrRelaisLogErhoehtGespeichert("A",
                        StaticVariable.uhrA_calendarZeit,
                        StaticVariable.uhrA_angezeigteZeit,
                        StaticVariable.uhrA_lastRelaisA);
            } else if (!Serial_IoThread.getSerialIoStatus2()) {
                int soll = Math.max(StaticVariable.uhrA_calendarZeit, getCurrentCalendarMinuten12());
                LogTurmtechnik2.appendNebenuhrRelaisLogKeineVerbindung("A", StaticVariable.uhrA_angezeigteZeit, soll, StaticVariable.uhrA_lastRelaisA);
            }
            if (platineIndex >= 0 && platineIndex < StaticVariable.nebenuhrImpulsLaeuftPlatine.length) {
                StaticVariable.nebenuhrImpulsLaeuftPlatine[platineIndex] = false;
            }
        }

    }

    /** Berechnet den nächsten Ist-Wert (0–719) ohne StaticVariable zu ändern. Gibt -1 zurück wenn schon Gleichstand. */
    private int computeNextAngezeigteZeit_A() {
        final int maxUhrCount = 719;
        if (StaticVariable.uhrA_angezeigteZeit == StaticVariable.uhrA_calendarZeit) return -1;
        int next = StaticVariable.uhrA_angezeigteZeit + 1;
        if (next > maxUhrCount) next = 0;
        return next;
    }

    private void incrementAngezeigteZeit_A() {
        // Nur 12h-Uhren: 0–719 Minuten
        final int maxUhrCount = 719;
        // Sicherheit: nie über Soll hinaus erhöhen (verhindert „Uhr geht voraus“)
        if (StaticVariable.uhrA_angezeigteZeit == StaticVariable.uhrA_calendarZeit) return;

        StaticVariable.uhrA_angezeigteZeit++;
        if (StaticVariable.uhrA_angezeigteZeit > maxUhrCount) {
            StaticVariable.uhrA_angezeigteZeit = 0;
        }
    }

    private void sleepTime(long time) {
        try {
            Thread.sleep(time);
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

} // ende der Klasse
