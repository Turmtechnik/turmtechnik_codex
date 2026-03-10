package tom.turmtechnik;

import java.util.Calendar;

public class NebenUhrC_thread extends Thread {
    /** Nach Abbruch gesetzt; beim nächsten makeOneImpuls wird „Wiederholung“ ins Log geschrieben. */
    private static boolean lastAbortC = false;

    private final String sourceFileName = "NebenUhrC_thread";
    int relaisNumber;
    private PlatinenDatabaseHelper.NebenuhrConfig nebenuhrConfig; // Konfiguration aus Datenbank

    public void run() {
        PlatinenDatabaseHelper dbHelper = PlatinenDatabaseHelper.getInstance(TurmtechnikActivity.getRuntimeContext());
        PlatinenDatabaseHelper.NebenuhrConfig config = dbHelper.getNebenuhrByZeile(5); // Zeile 5 = Uhr C

        if (config == null) {
            android.util.Log.w("NebenUhrC_thread", "Nebenuhr C nicht in Datenbank. Nutzen Sie Web-UI oder 'Import alter Anlagen'.");
            StaticVariable.uhrC_doRun = false;
            return;
        }

        this.nebenuhrConfig = config;
        StaticVariable.uhrC_lastRelaisA = config.lastRelaisA;

        //Log.d("Nebenuhr C Thread" , "Konfiguration aus Datenbank geladen");

        while (StaticVariable.uhrC_doRun == true) {
            // Prüfe, ob Konfiguration neu geladen werden muss (wenn über Web-UI geändert)
            if (StaticVariable.nebenuhrC_configNeuLaden) {
                android.util.Log.d("NebenUhrC_thread", "Konfiguration wurde geändert, lade neu...");
                PlatinenDatabaseHelper dbHelperReload = PlatinenDatabaseHelper.getInstance(TurmtechnikActivity.getRuntimeContext());
                PlatinenDatabaseHelper.NebenuhrConfig newConfig = dbHelperReload.getNebenuhrByZeile(5);
                if (newConfig != null) {
                    this.nebenuhrConfig = newConfig;
                    StaticVariable.uhrC_lastRelaisA = newConfig.lastRelaisA;
                    android.util.Log.d("NebenUhrC_thread", "Konfiguration neu geladen: RelaisA=" + newConfig.relaisA + ", RelaisB=" + newConfig.relaisB + ", Impuls1=" + newConfig.impulsDauer1 + ", Impuls2=" + newConfig.impulsDauer2);
                    if (!newConfig.aktiv) {
                        android.util.Log.d("NebenUhrC_thread", "Nebenuhr C auf inaktiv gestellt, Thread beendet.");
                        StaticVariable.uhrC_doRun = false;
                        break;
                    }
                }
                StaticVariable.nebenuhrC_configNeuLaden = false; // Flag zurücksetzen
            }
            
            // Impulse machen bis Uhrzeit passt (Aufholen durchlaufen lassen).
            // Bei Verbindungsfehler trotzdem makeOneImpuls() aufrufen – sonst gehen Impulse verloren (kein Wiederholversuch).
            while (checkSollIstZeit() == true) {
                if (StaticVariable.nebenuhrC_configNeuLaden) break;
                if (!StaticVariable.uhrC_doRun || (StaticVariable.uhr_zeiteingabeAktiv != null && StaticVariable.uhr_zeiteingabeAktiv.length > 2 && StaticVariable.uhr_zeiteingabeAktiv[2])) break;
                makeOneImpuls();
                if (!Serial_IoThread.getSerialIoStatus2()) {
                    sleepTime(200);
                }
            }

            // Gleichstand erreicht – bis zur nächsten Minute warten (oder sofort weiter bei uhrC_aufholenAnfordern)
            if (StaticVariable.uhrC_doRun) {
                try {
                    java.util.Calendar cal = java.util.Calendar.getInstance();
                    int sec = cal.get(java.util.Calendar.SECOND);
                    int ms = cal.get(java.util.Calendar.MILLISECOND);
                    long sleepMs = (60 - sec) * 1000L - ms + 400;
                    if (sleepMs > 0 && sleepMs < 61000) {
                        final long chunkMs = 300L;
                        while (sleepMs > 0 && StaticVariable.uhrC_doRun) {
                            if (StaticVariable.uhrC_aufholenAnfordern) {
                                StaticVariable.uhrC_aufholenAnfordern = false;
                                break;
                            }
                            long s = Math.min(chunkMs, sleepMs);
                            sleepTime(s);
                            sleepMs -= s;
                        }
                    }
                    // Kurz warten, damit UhrThread/NebenUhrThread die calendarZeit zur vollen Minute
                    // aktualisiert hat – verhindert fehlenden Impuls („eine Minute fehlt“).
                    if (StaticVariable.uhrC_doRun) {
                        sleepTime(500);
                    }
                } catch (Exception e) { /* weiter in der Schleife */ }
            }
        }
        StaticVariable.uhrC_doRun = false;
        System.gc();
    } // ende von run

    /** Aktuelle Echtzeit in Minuten (12h, 0–719). Damit Aufholen nicht von uhrC_calendarZeit abhängt (wird nur 1× pro Minute vom NebenUhrThread gesetzt). */
    private static int getCurrentCalendarMinuten12() {
        Calendar cal = Calendar.getInstance();
        return cal.get(Calendar.HOUR) * 60 + cal.get(Calendar.MINUTE);
    }

    private boolean checkSollIstZeit() {
        // Soll = max(gespeicherte Calendar-Zeit, aktuelle Systemzeit), damit wir beim Aufholen nicht von veralteter uhrC_calendarZeit abhängen
        int soll = Math.max(StaticVariable.uhrC_calendarZeit, getCurrentCalendarMinuten12());
        if (StaticVariable.uhrC_angezeigteZeit == soll) {
            StaticVariable.uhr_warten[2] = false;
            return false; // soll - ist zeit ist O.K.
        } else {
            if (StaticVariable.uhr_warten[2] == true) {
                return false;  // auf naechste Minute warten
            } else {
                return true; // weitermachen mit impulsen
            }
        }
    }

    public synchronized void makeOneImpuls() {
        int timeTemp;

        if (StaticVariable.uhr_warten[2] == true) {
            //Log.i("uhr_warten" , "C = true ") ;
            sleepTime(100);
            return;
        }

        if (nebenuhrConfig == null) {
            android.util.Log.e("NebenUhrC_thread", "Keine Konfiguration verfügbar!");
            return;
        }
        
        // WEICHSELSCHALTUNG: Immer das ANDERE Relais als beim letzten abgeschlossenen Impuls.
        // WICHTIG: uhrC_lastRelaisA erst NACH erfolgreichem Impuls setzen – sonst wechselt die Anzeige/Relaiswahl mitten im Impuls!
        int relaisNumberToUse = 0;
        final boolean nextLastRelaisA;
        if (StaticVariable.uhrC_pendingImpuls) {
            relaisNumberToUse = StaticVariable.uhrC_pendingRelaisA ? nebenuhrConfig.relaisA : nebenuhrConfig.relaisB;
            nextLastRelaisA = StaticVariable.uhrC_pendingRelaisA;
        } else if (StaticVariable.uhrC_lastRelaisA) {
            relaisNumberToUse = nebenuhrConfig.relaisB;
            nextLastRelaisA = false;
        } else {
            relaisNumberToUse = nebenuhrConfig.relaisA;
            nextLastRelaisA = true;
        }
        
        // Kein Relais zugewiesen (0 = Keins) → keinen Impuls ausführen (Uhr C wird dann nicht geschaltet!)
        if (relaisNumberToUse <= 0) {
            android.util.Log.e("NebenUhrC_thread", "Uhr C: Kein Relais konfiguriert (relaisA=" + nebenuhrConfig.relaisA + ", relaisB=" + nebenuhrConfig.relaisB + ") – Nebenuhr C wird nicht geschaltet. Bitte in der Konfiguration Relais A/B für Zeile 5 zuweisen.");
            return;
        }
        // SICHERHEIT: Prüfe, dass nur EIN Relais verwendet wird
        if (relaisNumberToUse > 0) {
            // KRITISCH: Warte bis das ANDERE Relais fertig ist, bevor wir das neue einschalten!
            int otherRelais = (relaisNumberToUse == nebenuhrConfig.relaisA) ? nebenuhrConfig.relaisB : nebenuhrConfig.relaisA;
            if (otherRelais > 0 && otherRelais <= Serial_IoThread.relaisNew.length) {
                if (Serial_IoThread.relaisNew[otherRelais - 1] != null && Serial_IoThread.relaisNew[otherRelais - 1] == true) {
                    android.util.Log.w("NebenUhrC_thread", "WARNUNG: Anderes Relais (" + otherRelais + ") ist noch aktiv, warte bis Impuls fertig ist!");
                    long timeoutMs = System.currentTimeMillis() + (nebenuhrConfig.impulsDauer1 + nebenuhrConfig.impulsDauer2) * 1000 + 1000;
                    while (Serial_IoThread.relaisNew[otherRelais - 1] != null && 
                           Serial_IoThread.relaisNew[otherRelais - 1] == true &&
                           System.currentTimeMillis() < timeoutMs) {
                        sleepTime(10);
                    }
                    if (Serial_IoThread.relaisNew[otherRelais - 1] != null && Serial_IoThread.relaisNew[otherRelais - 1] == true) {
                        android.util.Log.e("NebenUhrC_thread", "FEHLER: Anderes Relais (" + otherRelais + ") war nach Timeout noch aktiv! Breche ab.");
                        return;
                    } else {
                        android.util.Log.d("NebenUhrC_thread", "Anderes Relais (" + otherRelais + ") ist jetzt ausgeschaltet");
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
                    android.util.Log.e("NebenUhrC_thread", "KRITISCHER FEHLER: Beide Relais gleichzeitig aktiv! Warte bis beide fertig sind!");
                    long timeoutMs = System.currentTimeMillis() + (nebenuhrConfig.impulsDauer1 + nebenuhrConfig.impulsDauer2) * 1000 + 1000;
                    while ((relaisAAktiv || relaisBAktiv) && System.currentTimeMillis() < timeoutMs) {
                        relaisAAktiv = (Serial_IoThread.relaisNew[nebenuhrConfig.relaisA - 1] != null && 
                                        Serial_IoThread.relaisNew[nebenuhrConfig.relaisA - 1] == true);
                        relaisBAktiv = (Serial_IoThread.relaisNew[nebenuhrConfig.relaisB - 1] != null && 
                                        Serial_IoThread.relaisNew[nebenuhrConfig.relaisB - 1] == true);
                        sleepTime(10);
                    }
                    if (relaisAAktiv || relaisBAktiv) {
                        android.util.Log.e("NebenUhrC_thread", "FEHLER: Relais waren nach Timeout noch aktiv! Breche ab.");
                        return;
                    }
                }
            }
            
            relaisNumber = relaisNumberToUse;
            if (lastAbortC) {
                LogTurmtechnik2.appendNebenuhrRelaisLogWiederholung("C", relaisNumber, StaticVariable.uhrC_calendarZeit, StaticVariable.uhrC_angezeigteZeit, StaticVariable.uhrC_lastRelaisA);
                lastAbortC = false;
            }
            StaticVariable.uhrC_pendingImpuls = true;
            StaticVariable.uhrC_pendingRelaisA = (relaisNumberToUse == nebenuhrConfig.relaisA);
            if (Serial_IoThread.getSerialIoStatus2()) {
                try {
                    PlatinenDatabaseHelper dbHelper = PlatinenDatabaseHelper.getInstance(TurmtechnikActivity.getRuntimeContext());
                    PlatinenDatabaseHelper.NebenuhrConfig saveConfig = dbHelper.getNebenuhrByZeile(5);
                    if (saveConfig != null) {
                        saveConfig.impulsAusstehend = true;
                        saveConfig.pendingRelaisA = (relaisNumberToUse == nebenuhrConfig.relaisA);
                        dbHelper.saveNebenuhr(saveConfig);
                    }
                } catch (Exception e) {
                    android.util.Log.e("NebenUhrC_thread", "Fehler beim Vorab-Speichern vor Impuls", e);
                }
            }
            //Log.i("Relais" , "Nr.:" + relaisNumber);
            String relaisLetterC = (relaisNumber == nebenuhrConfig.relaisA) ? "A" : "B";
            int platineIndex = (relaisNumber - 1) / 32;
            boolean ersteRunde = true;
            for (;;) {
                if (!ersteRunde) {
                    sleepTime(2000);
                    if (relaisNumber > 0 && relaisNumber <= Serial_IoThread.relaisOld.length) {
                        Serial_IoThread.relaisOld[relaisNumber - 1] = false;
                    }
                }
                ersteRunde = false;
                long impulseStartTimeMs = android.os.SystemClock.elapsedRealtime();
                if (platineIndex >= 0 && platineIndex < StaticVariable.nebenuhrImpulsLaeuftPlatine.length) {
                    StaticVariable.nebenuhrImpulsLaeuftPlatine[platineIndex] = true;
                }
                Serial_IoThread.relaisNew[relaisNumber - 1] = true;                // relais einschalten
                LogTurmtechnik2.appendNebenuhrRelaisLogRelaisEinAus("C", relaisLetterC, true);
            // ZUSÄTZLICHE SICHERHEITSPRÜFUNG: Nach dem Einschalten prüfen
            if (nebenuhrConfig.relaisA > 0 && nebenuhrConfig.relaisA <= Serial_IoThread.relaisNew.length &&
                nebenuhrConfig.relaisB > 0 && nebenuhrConfig.relaisB <= Serial_IoThread.relaisNew.length) {
                boolean relaisAAktiv = (Serial_IoThread.relaisNew[nebenuhrConfig.relaisA - 1] != null && 
                                        Serial_IoThread.relaisNew[nebenuhrConfig.relaisA - 1] == true);
                boolean relaisBAktiv = (Serial_IoThread.relaisNew[nebenuhrConfig.relaisB - 1] != null && 
                                        Serial_IoThread.relaisNew[nebenuhrConfig.relaisB - 1] == true);
                
                if (relaisAAktiv && relaisBAktiv) {
                    android.util.Log.e("NebenUhrC_thread", "KRITISCHER FEHLER NACH EINSCHALTEN: Beide Relais aktiv! Warte bis anderes fertig ist!");
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
                        android.util.Log.e("NebenUhrC_thread", "FEHLER: Beide Relais nach Wartezeit noch aktiv! Breche Impuls ab.");
                        Serial_IoThread.relaisNew[relaisNumber - 1] = false;
                        LogTurmtechnik2.appendNebenuhrRelaisLogRelaisEinAus("C", relaisLetterC, false);
                        LogTurmtechnik2.appendNebenuhrRelaisLogAbbruch("C", relaisNumber, StaticVariable.uhrC_calendarZeit, StaticVariable.uhrC_angezeigteZeit, StaticVariable.uhrC_lastRelaisA);
                        lastAbortC = true;
                        if (platineIndex >= 0 && platineIndex < StaticVariable.nebenuhrImpulsLaeuftPlatine.length) {
                            StaticVariable.nebenuhrImpulsLaeuftPlatine[platineIndex] = false;
                        }
                        return;
                    }
                }
            }
            
            sleepTime(nebenuhrConfig.impulsDauer1 * 1000);                     // warte impuls dauer (Sekunden → Millisekunden)
            Serial_IoThread.relaisNew[relaisNumber - 1] = false;               // relais wieder ausschalten
            LogTurmtechnik2.appendNebenuhrRelaisLogRelaisEinAus("C", relaisLetterC, false);
            sleepTime(nebenuhrConfig.impulsDauer2 * 1000);                      // warte impuls pause (Sekunden → Millisekunden)

            if (platineIndex >= 0 && platineIndex < StaticVariable.nebenuhrImpulsLaeuftPlatine.length) {
                StaticVariable.nebenuhrImpulsLaeuftPlatine[platineIndex] = false;
            }
            if (Serial_IoThread.hadCarambolaErrorSince(platineIndex, impulseStartTimeMs)) {
                if (relaisNumber > 0 && relaisNumber <= Serial_IoThread.relaisOld.length) {
                    Serial_IoThread.relaisOld[relaisNumber - 1] = false;
                }
                android.util.Log.w("NebenUhrC_thread", "Carambola-Fehler – wiederhole Impuls in 2 s, bis er durch ist.");
                if (platineIndex >= 0 && platineIndex < StaticVariable.nebenuhrImpulsLaeuftPlatine.length) {
                    StaticVariable.nebenuhrImpulsLaeuftPlatine[platineIndex] = false;
                }
                continue;
            }
            if (Serial_IoThread.getSerialIoStatus2() && StaticVariable.uhrC_angezeigteZeit != StaticVariable.uhrC_calendarZeit) {
                incrementAngezeigteZeit_C();
                StaticVariable.uhrC_lastRelaisA = nextLastRelaisA;
                StaticVariable.uhrC_pendingImpuls = false;
                StaticVariable.uhrC_pendingRelaisA = false;
                try {
                    PlatinenDatabaseHelper dbHelper = PlatinenDatabaseHelper.getInstance(TurmtechnikActivity.getRuntimeContext());
                    PlatinenDatabaseHelper.NebenuhrConfig c = dbHelper.getNebenuhrByZeile(5);
                    if (c != null) {
                        c.angezeigteZeit = StaticVariable.uhrC_angezeigteZeit;
                        c.lastRelaisA = StaticVariable.uhrC_lastRelaisA;
                        c.impulsAusstehend = false;
                        c.pendingRelaisA = null;
                        dbHelper.saveNebenuhr(c);
                    }
                } catch (Exception e) {
                    android.util.Log.e("NebenUhrC_thread", "Fehler beim Löschen impulsAusstehend nach Impuls", e);
                }
                LogTurmtechnik2.appendNebenuhrRelaisLogErhoehtGespeichert("C",
                        StaticVariable.uhrC_calendarZeit,
                        StaticVariable.uhrC_angezeigteZeit,
                        StaticVariable.uhrC_lastRelaisA);
                if (platineIndex >= 0 && platineIndex < StaticVariable.nebenuhrImpulsLaeuftPlatine.length) {
                    StaticVariable.nebenuhrImpulsLaeuftPlatine[platineIndex] = false;
                }
                return;
            }
            if (!Serial_IoThread.getSerialIoStatus2()) {
                android.util.Log.w("NebenUhrC_thread", "Keine Verbindung – wiederhole Impuls in 2 s, bis er durch ist.");
                continue;
            }
            if (platineIndex >= 0 && platineIndex < StaticVariable.nebenuhrImpulsLaeuftPlatine.length) {
                StaticVariable.nebenuhrImpulsLaeuftPlatine[platineIndex] = false;
            }
            }
        }
    }

    private int computeNextAngezeigteZeit_C() {
        final int maxUhrCount = 719;
        if (StaticVariable.uhrC_angezeigteZeit == StaticVariable.uhrC_calendarZeit) return -1;
        int next = StaticVariable.uhrC_angezeigteZeit + 1;
        if (next > maxUhrCount) next = 0;
        return next;
    }

    private void incrementAngezeigteZeit_C() {
        // Nur 12h-Uhren: 0–719 Minuten
        final int maxUhrCount = 719;
        if (StaticVariable.uhrC_angezeigteZeit == StaticVariable.uhrC_calendarZeit) return;

        StaticVariable.uhrC_angezeigteZeit++;
        if (StaticVariable.uhrC_angezeigteZeit > maxUhrCount) {
            StaticVariable.uhrC_angezeigteZeit = 0;
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
