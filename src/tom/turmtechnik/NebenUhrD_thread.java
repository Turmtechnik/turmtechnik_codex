package tom.turmtechnik;

import android.util.Log;

import java.util.Calendar;

/**
 * Thread fuer Monduhr D
 *
 * LOGIK:
 * - 60 Impulse = 1 synodischer Monat (29.530588 Tage)
 * - Dauer zwischen Impulsen: 29.530588 / 60 = 0.492176 Tage ~= 11.8 Stunden
 * - Nach 60 Impulsen wird wieder Neumond erreicht -> Reset auf 0
 * - Impuls 0 = Neumond
 * - Impuls 30 = Vollmond (Tag 14.77)
 * - Impuls 60 = Neumond (wird zu 0, Tag 29.53)
 */
public class NebenUhrD_thread extends Thread {
    private final String sourceFileName = "NebenUhrD_thread";

    private static final double SYNODISCHER_MONAT_TAGE = 29.530588;
    // Referenz-Neumond: 30. Dezember 2025 00:00 UTC (korrigiert)
    private static final long REFERENZ_NEUMOND_MS = 1735603200000L;

    private PlatinenDatabaseHelper.NebenuhrConfig nebenuhrConfig;
    /** Letzter Zeitstempel fuer Log "Serial-Verbindung nicht OK" - nur alle 30 s loggen. */
    private long lastSerialNotOkLogMs = 0;
    private static final long SERIAL_NOT_OK_LOG_INTERVAL_MS = 30000;

    public void run() {
        PlatinenDatabaseHelper dbHelper = PlatinenDatabaseHelper.getInstance(TurmtechnikActivity.getRuntimeContext());
        PlatinenDatabaseHelper.NebenuhrConfig config = dbHelper.getNebenuhrByZeile(6); // Zeile 6 = Uhr D (Monduhr)
        if (config == null) {
            Log.w("NebenUhrD_thread", "Monduhr D nicht in Datenbank gefunden. Bitte ueber Web-UI konfigurieren.");
            return;
        }
        this.nebenuhrConfig = config;

        // Lade Zustand aus Datenbank
        StaticVariable.uhrD_lastRelaisA = config.lastRelaisA;
        StaticVariable.uhrD_mondphaseIst = config.mondphaseIst;
        StaticVariable.uhrD_pendingImpuls = config.impulsAusstehend && config.pendingRelaisA != null;
        StaticVariable.uhrD_pendingRelaisA = config.pendingRelaisA != null && config.pendingRelaisA;
        if (config.impulsAusstehend) {
            Log.w("MonduhrD", "impulsAusstehend nach Neustart erkannt - gleicher Impuls wird wiederholt");
        }

        Log.d("MonduhrD", "Konfiguration aus Datenbank geladen: Relais A=" + config.relaisA + ", B=" + config.relaisB);

        while (StaticVariable.uhrD_doRun) {
            // Pruefe, ob Konfiguration neu geladen werden muss (wenn ueber Web-UI geaendert)
            if (StaticVariable.nebenuhrD_configNeuLaden) {
                Log.d("MonduhrD", "Konfiguration wurde geaendert, lade neu...");
                PlatinenDatabaseHelper dbHelperReload = PlatinenDatabaseHelper.getInstance(TurmtechnikActivity.getRuntimeContext());
                PlatinenDatabaseHelper.NebenuhrConfig newConfig = dbHelperReload.getNebenuhrByZeile(6);
                if (newConfig != null) {
                    this.nebenuhrConfig = newConfig;
                    StaticVariable.uhrD_lastRelaisA = newConfig.lastRelaisA;
                    StaticVariable.uhrD_mondphaseIst = newConfig.mondphaseIst;
                    StaticVariable.uhrD_pendingImpuls = newConfig.impulsAusstehend && newConfig.pendingRelaisA != null;
                    StaticVariable.uhrD_pendingRelaisA = newConfig.pendingRelaisA != null && newConfig.pendingRelaisA;
                    Log.d("MonduhrD", "Konfiguration neu geladen: RelaisA=" + newConfig.relaisA + ", RelaisB=" + newConfig.relaisB + ", Impuls1=" + newConfig.impulsDauer1 + ", Impuls2=" + newConfig.impulsDauer2);
                }
                StaticVariable.nebenuhrD_configNeuLaden = false;
            }

            if (StaticVariable.nebenuhrEinstellungsSeiteOffen) {
                Log.d("MonduhrD", "Nebenuhr-Einstellungsseite offen, pausiere");
                sleepTime(5000);
                continue;
            }

            berechneMondphaseSoll();

            if (StaticVariable.uhrD_mondphaseSoll == StaticVariable.uhrD_mondphaseIst) {
                int impulseProMondphase = getImpulseProMondphase();
                double stundenProImpuls = (SYNODISCHER_MONAT_TAGE * 24.0) / impulseProMondphase;

                Log.d("MonduhrD", "Mondphase synchronisiert: Soll=" + StaticVariable.uhrD_mondphaseSoll
                        + " Ist=" + StaticVariable.uhrD_mondphaseIst
                        + " (Naechster Impuls in ca. " + String.format("%.1f", stundenProImpuls) + " Stunden, "
                        + "pruefe alle 5 Minuten)");

                sleepTime(300000);
                continue;
            }

            int differenzMondphase = StaticVariable.uhrD_mondphaseSoll - StaticVariable.uhrD_mondphaseIst;
            if (differenzMondphase < 0) {
                differenzMondphase += getImpulseProMondphase();
            }

            Log.d("MonduhrD", "Mondphase Differenz: Soll=" + StaticVariable.uhrD_mondphaseSoll
                    + " Ist=" + StaticVariable.uhrD_mondphaseIst + " Differenz=" + differenzMondphase
                    + " Impulse noetig");

            while (StaticVariable.uhrD_doRun
                    && StaticVariable.uhrD_mondphaseSoll != StaticVariable.uhrD_mondphaseIst
                    && !StaticVariable.nebenuhrEinstellungsSeiteOffen) {

                if (StaticVariable.uhr_warten[3]) {
                    sleepTime(1000);
                    continue;
                }

                if (Serial_IoThread.getSerialIoStatus2()) {
                    makeOneImpuls();
                    berechneMondphaseSoll();
                } else {
                    long now = System.currentTimeMillis();
                    if (now - lastSerialNotOkLogMs >= SERIAL_NOT_OK_LOG_INTERVAL_MS) {
                        lastSerialNotOkLogMs = now;
                        Log.w("MonduhrD", "Serial-Verbindung nicht OK, warte...");
                    }
                    sleepTime(1000);
                }
            }

            if (StaticVariable.uhrD_mondphaseSoll == StaticVariable.uhrD_mondphaseIst) {
                Log.d("MonduhrD", "Mondphase synchronisiert, warte 1 Minute");
                sleepTime(60000);
            }
        }

        try {
            PlatinenDatabaseHelper dbHelper2 = PlatinenDatabaseHelper.getInstance(TurmtechnikActivity.getRuntimeContext());
            PlatinenDatabaseHelper.NebenuhrConfig saveConfig = dbHelper2.getNebenuhrByZeile(6);
            if (saveConfig != null) {
                saveConfig.mondphaseIst = StaticVariable.uhrD_mondphaseIst;
                saveConfig.lastRelaisA = StaticVariable.uhrD_lastRelaisA;
                saveConfig.impulsAusstehend = StaticVariable.uhrD_pendingImpuls;
                saveConfig.pendingRelaisA = StaticVariable.uhrD_pendingImpuls ? StaticVariable.uhrD_pendingRelaisA : null;
                dbHelper2.saveNebenuhr(saveConfig);
            }
        } catch (Exception e) {
            Log.e("MonduhrD", "Fehler beim Speichern beim Beenden", e);
        }

        System.gc();
    }

    /**
     * Berechnet die aktuelle Mondphase (Soll) basierend auf dem aktuellen Datum.
     * 0 = Neumond, 30 = Vollmond, 60 = Neumond (zyklisch)
     * Verwendet festen Referenz-Neumond: 30. Dezember 2025
     */
    private void berechneMondphaseSoll() {
        Calendar calendar = Calendar.getInstance();
        long aktuellMs = calendar.getTimeInMillis();

        long diffMs = aktuellMs - REFERENZ_NEUMOND_MS;
        double tageSeitNeumond = diffMs / (1000.0 * 60.0 * 60.0 * 24.0);

        while (tageSeitNeumond < 0) {
            tageSeitNeumond += SYNODISCHER_MONAT_TAGE;
        }

        tageSeitNeumond = tageSeitNeumond % SYNODISCHER_MONAT_TAGE;

        int impulseProMondphase = getImpulseProMondphase();
        double mondphase = (tageSeitNeumond / SYNODISCHER_MONAT_TAGE) * impulseProMondphase;
        mondphase = mondphase % impulseProMondphase;
        if (mondphase < 0) {
            mondphase += impulseProMondphase;
        }

        StaticVariable.uhrD_mondphaseSoll = (int) Math.round(mondphase);
        if (StaticVariable.uhrD_mondphaseSoll >= impulseProMondphase) {
            StaticVariable.uhrD_mondphaseSoll = 0;
        }

        if (StaticVariable.uhrD_letzteBerechnungMs == 0
                || (aktuellMs - StaticVariable.uhrD_letzteBerechnungMs) > (5 * 60 * 1000)) {
            StaticVariable.uhrD_letzteBerechnungMs = aktuellMs;
            Log.d("MonduhrD", "Mondphase Soll: " + StaticVariable.uhrD_mondphaseSoll
                    + " (Tage seit Neumond: " + String.format("%.2f", tageSeitNeumond) + ")");
        }
    }

    public synchronized void makeOneImpuls() {
        if (nebenuhrConfig == null) {
            Log.e("MonduhrD", "Keine Konfiguration verfuegbar!");
            return;
        }

        if (StaticVariable.uhr_warten[3]) {
            sleepTime(100);
            return;
        }

        int relaisNumberToUse;
        final boolean nextLastRelaisA;
        if (StaticVariable.uhrD_pendingImpuls) {
            relaisNumberToUse = StaticVariable.uhrD_pendingRelaisA ? nebenuhrConfig.relaisA : nebenuhrConfig.relaisB;
            nextLastRelaisA = StaticVariable.uhrD_pendingRelaisA;
            Log.w("MonduhrD", "Wiederhole offenen Monduhr-Impuls auf Relais " + (nextLastRelaisA ? "A" : "B"));
        } else if (StaticVariable.uhrD_lastRelaisA) {
            relaisNumberToUse = nebenuhrConfig.relaisB;
            nextLastRelaisA = false;
        } else {
            relaisNumberToUse = nebenuhrConfig.relaisA;
            nextLastRelaisA = true;
        }

        if (relaisNumberToUse <= 0) {
            Log.e("MonduhrD", "Kein gueltiges Relais gefunden! relaisA=" + nebenuhrConfig.relaisA + " relaisB=" + nebenuhrConfig.relaisB);
            return;
        }

        int otherRelais = (relaisNumberToUse == nebenuhrConfig.relaisA) ? nebenuhrConfig.relaisB : nebenuhrConfig.relaisA;
        if (otherRelais > 0 && otherRelais <= Serial_IoThread.relaisNew.length) {
            if (Serial_IoThread.relaisNew[otherRelais - 1] != null && Serial_IoThread.relaisNew[otherRelais - 1]) {
                long timeoutMs = System.currentTimeMillis() + (nebenuhrConfig.impulsDauer1 + nebenuhrConfig.impulsDauer2) * 1000L + 1000L;
                while (Serial_IoThread.relaisNew[otherRelais - 1] != null
                        && Serial_IoThread.relaisNew[otherRelais - 1]
                        && System.currentTimeMillis() < timeoutMs) {
                    sleepTime(10);
                }
                if (Serial_IoThread.relaisNew[otherRelais - 1] != null && Serial_IoThread.relaisNew[otherRelais - 1]) {
                    Log.e("MonduhrD", "Anderes Relais (" + otherRelais + ") war nach Timeout noch aktiv! Breche ab.");
                    return;
                }
            }
        }

        if (nebenuhrConfig.relaisA > 0 && nebenuhrConfig.relaisA <= Serial_IoThread.relaisNew.length
                && nebenuhrConfig.relaisB > 0 && nebenuhrConfig.relaisB <= Serial_IoThread.relaisNew.length) {
            boolean relaisAAktiv = Serial_IoThread.relaisNew[nebenuhrConfig.relaisA - 1] != null
                    && Serial_IoThread.relaisNew[nebenuhrConfig.relaisA - 1];
            boolean relaisBAktiv = Serial_IoThread.relaisNew[nebenuhrConfig.relaisB - 1] != null
                    && Serial_IoThread.relaisNew[nebenuhrConfig.relaisB - 1];
            if (relaisAAktiv && relaisBAktiv) {
                long timeoutMs = System.currentTimeMillis() + (nebenuhrConfig.impulsDauer1 + nebenuhrConfig.impulsDauer2) * 1000L + 1000L;
                while ((relaisAAktiv || relaisBAktiv) && System.currentTimeMillis() < timeoutMs) {
                    relaisAAktiv = Serial_IoThread.relaisNew[nebenuhrConfig.relaisA - 1] != null
                            && Serial_IoThread.relaisNew[nebenuhrConfig.relaisA - 1];
                    relaisBAktiv = Serial_IoThread.relaisNew[nebenuhrConfig.relaisB - 1] != null
                            && Serial_IoThread.relaisNew[nebenuhrConfig.relaisB - 1];
                    sleepTime(10);
                }
                if (relaisAAktiv || relaisBAktiv) {
                    Log.e("MonduhrD", "Beide Relais waren nach Timeout noch aktiv! Breche ab.");
                    return;
                }
            }
        }

        final boolean pendingRelaisA = (relaisNumberToUse == nebenuhrConfig.relaisA);
        StaticVariable.uhrD_pendingImpuls = true;
        StaticVariable.uhrD_pendingRelaisA = pendingRelaisA;
        try {
            PlatinenDatabaseHelper dbHelper = PlatinenDatabaseHelper.getInstance(TurmtechnikActivity.getRuntimeContext());
            PlatinenDatabaseHelper.NebenuhrConfig saveConfig = dbHelper.getNebenuhrByZeile(6);
            if (saveConfig != null) {
                saveConfig.impulsAusstehend = true;
                saveConfig.pendingRelaisA = pendingRelaisA;
                dbHelper.saveNebenuhr(saveConfig);
            }
        } catch (Exception e) {
            Log.e("MonduhrD", "Fehler beim Vorab-Speichern vor Monduhr-Impuls", e);
        }

        long impulseStartTimeMs = android.os.SystemClock.elapsedRealtime();
        int platineIndex = (relaisNumberToUse - 1) / 32;
        if (platineIndex >= 0 && platineIndex < StaticVariable.nebenuhrImpulsLaeuftPlatine.length) {
            StaticVariable.nebenuhrImpulsLaeuftPlatine[platineIndex] = true;
        }

        try {
            Serial_IoThread.relaisNew[relaisNumberToUse - 1] = true;
            String relaisLetterD = (relaisNumberToUse == nebenuhrConfig.relaisA) ? "A" : "B";
            LogTurmtechnik2.appendNebenuhrRelaisLogRelaisEinAus("D", relaisLetterD, true);

            if (nebenuhrConfig.relaisA > 0 && nebenuhrConfig.relaisA <= Serial_IoThread.relaisNew.length
                    && nebenuhrConfig.relaisB > 0 && nebenuhrConfig.relaisB <= Serial_IoThread.relaisNew.length) {
                boolean relaisAAktiv = Serial_IoThread.relaisNew[nebenuhrConfig.relaisA - 1] != null
                        && Serial_IoThread.relaisNew[nebenuhrConfig.relaisA - 1];
                boolean relaisBAktiv = Serial_IoThread.relaisNew[nebenuhrConfig.relaisB - 1] != null
                        && Serial_IoThread.relaisNew[nebenuhrConfig.relaisB - 1];
                if (relaisAAktiv && relaisBAktiv) {
                    int otherRelaisAfter = (relaisNumberToUse == nebenuhrConfig.relaisA) ? nebenuhrConfig.relaisB : nebenuhrConfig.relaisA;
                    long timeoutMs = System.currentTimeMillis() + (nebenuhrConfig.impulsDauer1 + nebenuhrConfig.impulsDauer2) * 1000L + 1000L;
                    while (otherRelaisAfter > 0
                            && otherRelaisAfter <= Serial_IoThread.relaisNew.length
                            && Serial_IoThread.relaisNew[otherRelaisAfter - 1] != null
                            && Serial_IoThread.relaisNew[otherRelaisAfter - 1]
                            && System.currentTimeMillis() < timeoutMs) {
                        sleepTime(10);
                    }
                    relaisAAktiv = Serial_IoThread.relaisNew[nebenuhrConfig.relaisA - 1] != null
                            && Serial_IoThread.relaisNew[nebenuhrConfig.relaisA - 1];
                    relaisBAktiv = Serial_IoThread.relaisNew[nebenuhrConfig.relaisB - 1] != null
                            && Serial_IoThread.relaisNew[nebenuhrConfig.relaisB - 1];
                    if (relaisAAktiv && relaisBAktiv) {
                        Log.e("MonduhrD", "Beide Relais nach Einschalten noch aktiv! Breche Impuls ab.");
                        Serial_IoThread.relaisNew[relaisNumberToUse - 1] = false;
                        LogTurmtechnik2.appendNebenuhrRelaisLogRelaisEinAus("D", relaisLetterD, false);
                        return;
                    }
                }
            }

            sleepTime(nebenuhrConfig.impulsDauer1 * 1000L);
            Serial_IoThread.relaisNew[relaisNumberToUse - 1] = false;
            LogTurmtechnik2.appendNebenuhrRelaisLogRelaisEinAus("D", relaisLetterD, false);
            sleepTime(nebenuhrConfig.impulsDauer2 * 1000L);

            if (platineIndex >= 0 && platineIndex < StaticVariable.nebenuhrImpulsLaeuftPlatine.length) {
                StaticVariable.nebenuhrImpulsLaeuftPlatine[platineIndex] = false;
            }

            if (Serial_IoThread.hadCarambolaErrorSince(platineIndex, impulseStartTimeMs)) {
                Log.e("MonduhrD", "Carambola-Fehler waehrend Monduhr-Impuls - gleicher Impuls wird wiederholt.");
                if (relaisNumberToUse > 0 && relaisNumberToUse <= Serial_IoThread.relaisOld.length) {
                    Serial_IoThread.relaisOld[relaisNumberToUse - 1] = false;
                }
                try {
                    PlatinenDatabaseHelper dbHelper = PlatinenDatabaseHelper.getInstance(TurmtechnikActivity.getRuntimeContext());
                    PlatinenDatabaseHelper.NebenuhrConfig c = dbHelper.getNebenuhrByZeile(6);
                    if (c != null) {
                        c.mondphaseIst = StaticVariable.uhrD_mondphaseIst;
                        c.lastRelaisA = StaticVariable.uhrD_lastRelaisA;
                        c.impulsAusstehend = true;
                        c.pendingRelaisA = pendingRelaisA;
                        dbHelper.saveNebenuhr(c);
                    }
                } catch (Exception e) {
                    Log.e("MonduhrD", "Fehler beim Speichern nach Carambola-Fehler", e);
                }
                return;
            }

            StaticVariable.uhrD_lastRelaisA = nextLastRelaisA;
            StaticVariable.uhrD_pendingImpuls = false;
            StaticVariable.uhrD_pendingRelaisA = false;
            incrementMondphaseIst();

            try {
                PlatinenDatabaseHelper dbHelper = PlatinenDatabaseHelper.getInstance(TurmtechnikActivity.getRuntimeContext());
                PlatinenDatabaseHelper.NebenuhrConfig saveConfig = dbHelper.getNebenuhrByZeile(6);
                if (saveConfig != null) {
                    saveConfig.mondphaseIst = StaticVariable.uhrD_mondphaseIst;
                    saveConfig.lastRelaisA = StaticVariable.uhrD_lastRelaisA;
                    saveConfig.impulsAusstehend = false;
                    saveConfig.pendingRelaisA = null;
                    dbHelper.saveNebenuhr(saveConfig);
                    Log.d("MonduhrD", "Mondphase nach erfolgreichem Impuls gespeichert: " + StaticVariable.uhrD_mondphaseIst);
                }
            } catch (Exception e) {
                Log.e("MonduhrD", "Fehler beim Speichern nach Impuls", e);
            }
        } catch (Exception e) {
            Log.e("MonduhrD", "Fehler beim Senden des Impulses", e);
        } finally {
            if (platineIndex >= 0 && platineIndex < StaticVariable.nebenuhrImpulsLaeuftPlatine.length) {
                StaticVariable.nebenuhrImpulsLaeuftPlatine[platineIndex] = false;
            }
        }
    }

    /**
     * Erhoeht die Ist-Mondphase um 1 Impuls.
     * Nach 60 Impulsen (einem synodischen Monat) wird auf 0 zurueckgesetzt (Neumond)
     */
    private void incrementMondphaseIst() {
        StaticVariable.uhrD_mondphaseIst++;
        if (StaticVariable.uhrD_mondphaseIst >= getImpulseProMondphase()) {
            StaticVariable.uhrD_mondphaseIst = 0;
        }
        Log.d("MonduhrD", "Mondphase Ist: " + StaticVariable.uhrD_mondphaseIst
                + " (Soll: " + StaticVariable.uhrD_mondphaseSoll + ")");
    }

    private int getImpulseProMondphase() {
        PlatinenDatabaseHelper dbHelper = PlatinenDatabaseHelper.getInstance(TurmtechnikActivity.getRuntimeContext());
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
