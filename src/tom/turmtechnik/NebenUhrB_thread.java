package tom.turmtechnik;

import java.util.Calendar;

public class NebenUhrB_thread extends Thread {
    /** Nach Abbruch gesetzt; beim nächsten makeOneImpuls wird „Wiederholung“ ins Log geschrieben. */
    private static boolean lastAbortB = false;

    private final String sourceFileName = "NebenUhrB_thread";
    int relaisNumber;
    private PlatinenDatabaseHelper.NebenuhrConfig nebenuhrConfig; // Konfiguration aus Datenbank

    public void run() {
        PlatinenDatabaseHelper dbHelper = PlatinenDatabaseHelper.getInstance(TurmtechnikActivity.turmtechnikContext);
        PlatinenDatabaseHelper.NebenuhrConfig config = dbHelper.getNebenuhrByZeile(4); // Zeile 4 = Uhr B

        if (config == null) {
            android.util.Log.w("NebenUhrB_thread", "Nebenuhr B nicht in Datenbank. Nutzen Sie Web-UI oder 'Import alter Anlagen'.");
            StaticVariable.uhrB_doRun = false;
            return;
        }

        this.nebenuhrConfig = config;
        StaticVariable.uhrB_lastRelaisA = config.lastRelaisA;

        //Log.d("Nebenuhr B Thread" , "Konfiguration aus Datenbank geladen");

        while (StaticVariable.uhrB_doRun == true) {
            // Prüfe, ob Konfiguration neu geladen werden muss (wenn über Web-UI geändert)
            if (StaticVariable.nebenuhrB_configNeuLaden) {
                android.util.Log.d("NebenUhrB_thread", "Konfiguration wurde geändert, lade neu...");
                PlatinenDatabaseHelper dbHelperReload = PlatinenDatabaseHelper.getInstance(TurmtechnikActivity.turmtechnikContext);
                PlatinenDatabaseHelper.NebenuhrConfig newConfig = dbHelperReload.getNebenuhrByZeile(4);
                if (newConfig != null) {
                    this.nebenuhrConfig = newConfig;
                    StaticVariable.uhrB_lastRelaisA = newConfig.lastRelaisA;
                    android.util.Log.d("NebenUhrB_thread", "Konfiguration neu geladen: RelaisA=" + newConfig.relaisA + ", RelaisB=" + newConfig.relaisB + ", Impuls1=" + newConfig.impulsDauer1 + ", Impuls2=" + newConfig.impulsDauer2);
                    if (!newConfig.aktiv) {
                        android.util.Log.d("NebenUhrB_thread", "Nebenuhr B auf inaktiv gestellt, Thread beendet.");
                        StaticVariable.uhrB_doRun = false;
                        break;
                    }
                }
                StaticVariable.nebenuhrB_configNeuLaden = false; // Flag zurücksetzen
            }
            
            // Impulse machen bis Uhrzeit passt (Aufholen durchlaufen lassen).
            // Bei Verbindungsfehler trotzdem makeOneImpuls() aufrufen – sonst gehen Impulse verloren (kein Wiederholversuch).
            while (checkSollIstZeit() == true) {
                if (StaticVariable.nebenuhrB_configNeuLaden) break;
                if (!StaticVariable.uhrB_doRun || (StaticVariable.uhr_zeiteingabeAktiv != null && StaticVariable.uhr_zeiteingabeAktiv.length > 1 && StaticVariable.uhr_zeiteingabeAktiv[1])) break;
                makeOneImpuls();
                if (!Serial_IoThread.getSerialIoStatus2()) {
                    sleepTime(200);
                }
            }

            // Gleichstand erreicht – bis zur nächsten Minute warten (oder sofort weiter bei uhrB_aufholenAnfordern)
            if (StaticVariable.uhrB_doRun) {
                try {
                    java.util.Calendar cal = java.util.Calendar.getInstance();
                    int sec = cal.get(java.util.Calendar.SECOND);
                    int ms = cal.get(java.util.Calendar.MILLISECOND);
                    long sleepMs = (60 - sec) * 1000L - ms + 400;
                    if (sleepMs > 0 && sleepMs < 61000) {
                        final long chunkMs = 300L;
                        while (sleepMs > 0 && StaticVariable.uhrB_doRun) {
                            if (StaticVariable.uhrB_aufholenAnfordern) {
                                StaticVariable.uhrB_aufholenAnfordern = false;
                                break;
                            }
                            long s = Math.min(chunkMs, sleepMs);
                            sleepTime(s);
                            sleepMs -= s;
                        }
                    }
                    // Kurz warten, damit UhrThread/NebenUhrThread die calendarZeit zur vollen Minute
                    // aktualisiert hat – verhindert fehlenden Impuls („eine Minute fehlt“).
                    if (StaticVariable.uhrB_doRun) {
                        sleepTime(500);
                    }
                } catch (Exception e) { /* weiter in der Schleife */ }
            }
        }
        StaticVariable.uhrB_doRun = false;
        System.gc();
    } // ende von run

    /** Aktuelle Echtzeit in Minuten (12h, 0–719). Damit Aufholen nicht von veralteter uhrB_calendarZeit abhängt. */
    private static int getCurrentCalendarMinuten12() {
        Calendar cal = Calendar.getInstance();
        return cal.get(Calendar.HOUR) * 60 + cal.get(Calendar.MINUTE);
    }

    private boolean checkSollIstZeit() {
        int soll = Math.max(StaticVariable.uhrB_calendarZeit, getCurrentCalendarMinuten12());
        if (StaticVariable.uhrB_angezeigteZeit == soll) {
            StaticVariable.uhr_warten[1] = false;
            return false; // soll - ist zeit ist O.K.
        } else {
            if (StaticVariable.uhr_warten[1] == true) {
                return false;  // auf naechste Minute warten
            } else {
                return true; // weitermachen mit impulsen
            }
        }
    }

    public synchronized void makeOneImpuls() {
        int timeTemp;

        if (StaticVariable.uhr_warten[1] == true) {
            //Log.i("uhr_warten" , "B = true ") ;
            sleepTime(100);
            return;
        }

        if (nebenuhrConfig == null) {
            android.util.Log.e("NebenUhrB_thread", "Keine Konfiguration verfügbar!");
            return;
        }
        
        // WEICHSELSCHALTUNG: Immer das ANDERE Relais als beim letzten abgeschlossenen Impuls.
        // WICHTIG: uhrB_lastRelaisA erst NACH erfolgreichem Impuls setzen – sonst wechselt die Anzeige/Relaiswahl mitten im Impuls!
        int relaisNumberToUse = 0;
        final boolean nextLastRelaisA;
        if (StaticVariable.uhrB_lastRelaisA) {
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
            int otherRelais = (relaisNumberToUse == nebenuhrConfig.relaisA) ? nebenuhrConfig.relaisB : nebenuhrConfig.relaisA;
            if (otherRelais > 0 && otherRelais <= Serial_IoThread.relaisNew.length) {
                if (Serial_IoThread.relaisNew[otherRelais - 1] != null && Serial_IoThread.relaisNew[otherRelais - 1] == true) {
                    android.util.Log.w("NebenUhrB_thread", "WARNUNG: Anderes Relais (" + otherRelais + ") ist noch aktiv, warte bis Impuls fertig ist!");
                    long timeoutMs = System.currentTimeMillis() + (nebenuhrConfig.impulsDauer1 + nebenuhrConfig.impulsDauer2) * 1000 + 1000;
                    while (Serial_IoThread.relaisNew[otherRelais - 1] != null && 
                           Serial_IoThread.relaisNew[otherRelais - 1] == true &&
                           System.currentTimeMillis() < timeoutMs) {
                        sleepTime(10);
                    }
                    if (Serial_IoThread.relaisNew[otherRelais - 1] != null && Serial_IoThread.relaisNew[otherRelais - 1] == true) {
                        android.util.Log.e("NebenUhrB_thread", "FEHLER: Anderes Relais (" + otherRelais + ") war nach Timeout noch aktiv! Breche ab.");
                        return;
                    } else {
                        android.util.Log.d("NebenUhrB_thread", "Anderes Relais (" + otherRelais + ") ist jetzt ausgeschaltet");
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
                    android.util.Log.e("NebenUhrB_thread", "KRITISCHER FEHLER: Beide Relais gleichzeitig aktiv! Warte bis beide fertig sind!");
                    long timeoutMs = System.currentTimeMillis() + (nebenuhrConfig.impulsDauer1 + nebenuhrConfig.impulsDauer2) * 1000 + 1000;
                    while ((relaisAAktiv || relaisBAktiv) && System.currentTimeMillis() < timeoutMs) {
                        relaisAAktiv = (Serial_IoThread.relaisNew[nebenuhrConfig.relaisA - 1] != null && 
                                        Serial_IoThread.relaisNew[nebenuhrConfig.relaisA - 1] == true);
                        relaisBAktiv = (Serial_IoThread.relaisNew[nebenuhrConfig.relaisB - 1] != null && 
                                        Serial_IoThread.relaisNew[nebenuhrConfig.relaisB - 1] == true);
                        sleepTime(10);
                    }
                    if (relaisAAktiv || relaisBAktiv) {
                        android.util.Log.e("NebenUhrB_thread", "FEHLER: Relais waren nach Timeout noch aktiv! Breche ab.");
                        return;
                    }
                }
            }
            
            relaisNumber = relaisNumberToUse;
            if (lastAbortB) {
                LogTurmtechnik2.appendNebenuhrRelaisLogWiederholung("B", relaisNumber, StaticVariable.uhrB_calendarZeit, StaticVariable.uhrB_angezeigteZeit, StaticVariable.uhrB_lastRelaisA);
                lastAbortB = false;
            }
            int newIstB = computeNextAngezeigteZeit_B();
            if (newIstB >= 0 && Serial_IoThread.getSerialIoStatus2()) {
                try {
                    PlatinenDatabaseHelper dbHelper = PlatinenDatabaseHelper.getInstance(TurmtechnikActivity.turmtechnikContext);
                    PlatinenDatabaseHelper.NebenuhrConfig saveConfig = dbHelper.getNebenuhrByZeile(4);
                    if (saveConfig != null) {
                        saveConfig.lastRelaisA = nextLastRelaisA;
                        saveConfig.angezeigteZeit = newIstB;
                        saveConfig.impulsAusstehend = true;
                        dbHelper.saveNebenuhr(saveConfig);
                    }
                } catch (Exception e) {
                    android.util.Log.e("NebenUhrB_thread", "Fehler beim Vorab-Speichern vor Impuls", e);
                }
            }
            //Log.i("Relais" , "Nr.:" + relaisNumber);
            long impulseStartTimeMs = android.os.SystemClock.elapsedRealtime();
            int platineIndex = (relaisNumber - 1) / 32;
            if (platineIndex >= 0 && platineIndex < StaticVariable.nebenuhrImpulsLaeuftPlatine.length) {
                StaticVariable.nebenuhrImpulsLaeuftPlatine[platineIndex] = true;
            }
            Serial_IoThread.relaisNew[relaisNumber - 1] = true;                // relais einschalten
            String relaisLetterB = (relaisNumber == nebenuhrConfig.relaisA) ? "A" : "B";
            LogTurmtechnik2.appendNebenuhrRelaisLogRelaisEinAus("B", relaisLetterB, true);
            // ZUSÄTZLICHE SICHERHEITSPRÜFUNG: Nach dem Einschalten prüfen
            if (nebenuhrConfig.relaisA > 0 && nebenuhrConfig.relaisA <= Serial_IoThread.relaisNew.length &&
                nebenuhrConfig.relaisB > 0 && nebenuhrConfig.relaisB <= Serial_IoThread.relaisNew.length) {
                boolean relaisAAktiv = (Serial_IoThread.relaisNew[nebenuhrConfig.relaisA - 1] != null && 
                                        Serial_IoThread.relaisNew[nebenuhrConfig.relaisA - 1] == true);
                boolean relaisBAktiv = (Serial_IoThread.relaisNew[nebenuhrConfig.relaisB - 1] != null && 
                                        Serial_IoThread.relaisNew[nebenuhrConfig.relaisB - 1] == true);
                
                if (relaisAAktiv && relaisBAktiv) {
                    android.util.Log.e("NebenUhrB_thread", "KRITISCHER FEHLER NACH EINSCHALTEN: Beide Relais aktiv! Warte bis anderes fertig ist!");
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
                        android.util.Log.e("NebenUhrB_thread", "FEHLER: Beide Relais nach Wartezeit noch aktiv! Breche Impuls ab.");
                        Serial_IoThread.relaisNew[relaisNumber - 1] = false;
                        LogTurmtechnik2.appendNebenuhrRelaisLogRelaisEinAus("B", relaisLetterB, false);
                        LogTurmtechnik2.appendNebenuhrRelaisLogAbbruch("B", relaisNumber, StaticVariable.uhrB_calendarZeit, StaticVariable.uhrB_angezeigteZeit, StaticVariable.uhrB_lastRelaisA);
                        lastAbortB = true;
                        if (platineIndex >= 0 && platineIndex < StaticVariable.nebenuhrImpulsLaeuftPlatine.length) {
                            StaticVariable.nebenuhrImpulsLaeuftPlatine[platineIndex] = false;
                        }
                        return;
                    }
                }
            }
            
            sleepTime(nebenuhrConfig.impulsDauer1 * 1000);                     // warte impuls dauer (Sekunden → Millisekunden)
            Serial_IoThread.relaisNew[relaisNumber - 1] = false;                // relais wieder ausschalten
            LogTurmtechnik2.appendNebenuhrRelaisLogRelaisEinAus("B", relaisLetterB, false);
            sleepTime(nebenuhrConfig.impulsDauer2 * 1000);                      // warte impuls pause (Sekunden → Millisekunden)

            if (platineIndex >= 0 && platineIndex < StaticVariable.nebenuhrImpulsLaeuftPlatine.length) {
                StaticVariable.nebenuhrImpulsLaeuftPlatine[platineIndex] = false;
            }
            if (Serial_IoThread.hadCarambolaErrorSince(platineIndex, impulseStartTimeMs)) {
                android.util.Log.e("NebenUhrB_thread", "Carambola-Fehler während Nebenuhr-Impuls (Platine " + (platineIndex + 1) + ") – Impuls wird nicht gezählt, wird wiederholt.");
                if (relaisNumber > 0 && relaisNumber <= Serial_IoThread.relaisOld.length) {
                    Serial_IoThread.relaisOld[relaisNumber - 1] = false;
                }
                try {
                    PlatinenDatabaseHelper dbHelper = PlatinenDatabaseHelper.getInstance(TurmtechnikActivity.turmtechnikContext);
                    PlatinenDatabaseHelper.NebenuhrConfig c = dbHelper.getNebenuhrByZeile(4);
                    if (c != null) {
                        c.angezeigteZeit = StaticVariable.uhrB_angezeigteZeit;
                        c.lastRelaisA = StaticVariable.uhrB_lastRelaisA;
                        c.impulsAusstehend = false;
                        dbHelper.saveNebenuhr(c);
                    }
                } catch (Exception e) {
                    android.util.Log.e("NebenUhrB_thread", "Fehler beim Zurücksetzen der DB nach Carambola-Fehler", e);
                }
                LogTurmtechnik2.appendNebenuhrRelaisLogImpulsFehler("B", StaticVariable.uhrB_angezeigteZeit, StaticVariable.uhrB_calendarZeit, StaticVariable.uhrB_lastRelaisA);
                return;
            }
            if (Serial_IoThread.getSerialIoStatus2() && StaticVariable.uhrB_angezeigteZeit != StaticVariable.uhrB_calendarZeit) {
                incrementAngezeigteZeit_B();
                StaticVariable.uhrB_lastRelaisA = nextLastRelaisA;  // Relais-Wechsel nur nach fertigem Impuls!
                try {
                    PlatinenDatabaseHelper dbHelper = PlatinenDatabaseHelper.getInstance(TurmtechnikActivity.turmtechnikContext);
                    PlatinenDatabaseHelper.NebenuhrConfig c = dbHelper.getNebenuhrByZeile(4);
                    if (c != null) {
                        c.angezeigteZeit = StaticVariable.uhrB_angezeigteZeit;
                        c.lastRelaisA = StaticVariable.uhrB_lastRelaisA;
                        c.impulsAusstehend = false;
                        dbHelper.saveNebenuhr(c);
                    }
                } catch (Exception e) {
                    android.util.Log.e("NebenUhrB_thread", "Fehler beim Löschen impulsAusstehend nach Impuls", e);
                }
                LogTurmtechnik2.appendNebenuhrRelaisLogErhoehtGespeichert("B",
                        StaticVariable.uhrB_calendarZeit,
                        StaticVariable.uhrB_angezeigteZeit,
                        StaticVariable.uhrB_lastRelaisA);
            } else if (!Serial_IoThread.getSerialIoStatus2()) {
                LogTurmtechnik2.appendNebenuhrRelaisLogKeineVerbindung("B");
            }
            if (platineIndex >= 0 && platineIndex < StaticVariable.nebenuhrImpulsLaeuftPlatine.length) {
                StaticVariable.nebenuhrImpulsLaeuftPlatine[platineIndex] = false;
            }
        }

    }

    private int computeNextAngezeigteZeit_B() {
        final int maxUhrCount = 719;
        if (StaticVariable.uhrB_angezeigteZeit == StaticVariable.uhrB_calendarZeit) return -1;
        int next = StaticVariable.uhrB_angezeigteZeit + 1;
        if (next > maxUhrCount) next = 0;
        return next;
    }

    private void incrementAngezeigteZeit_B() {
        // Nur 12h-Uhren: 0–719 Minuten
        final int maxUhrCount = 719;
        if (StaticVariable.uhrB_angezeigteZeit == StaticVariable.uhrB_calendarZeit) return;

        StaticVariable.uhrB_angezeigteZeit++;
        if (StaticVariable.uhrB_angezeigteZeit > maxUhrCount) {
            StaticVariable.uhrB_angezeigteZeit = 0;
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
