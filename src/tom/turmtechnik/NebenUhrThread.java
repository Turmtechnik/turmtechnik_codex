package tom.turmtechnik;

import android.util.Log;

import java.util.Calendar;

public class NebenUhrThread extends Thread {
    private final String sourceFileName = "NebenUhrThread";
    private Calendar calendar;

    private NebenUhrA_thread nebenuhrA_thread = null;
    private NebenUhrB_thread nebenuhrB_thread = null;
    private NebenUhrC_thread nebenuhrC_thread = null;
    private NebenUhrD_thread nebenuhrD_thread = null;

    /** Statische Referenzen, damit von außen (z. B. Web-UI Confirm) geprüft werden kann, ob der Thread läuft, und er ggf. neu gestartet werden kann. */
    private static volatile NebenUhrA_thread nebenuhrA_threadRef = null;
    private static volatile NebenUhrB_thread nebenuhrB_threadRef = null;
    private static volatile NebenUhrC_thread nebenuhrC_threadRef = null;

    // Konfigurationen aus Datenbank
    private PlatinenDatabaseHelper.NebenuhrConfig nebenuhrA_config;
    private PlatinenDatabaseHelper.NebenuhrConfig nebenuhrB_config;
    private PlatinenDatabaseHelper.NebenuhrConfig nebenuhrC_config;
    private PlatinenDatabaseHelper.NebenuhrConfig nebenuhrD_config;

    private int minutenSave;
    private int stunden_12_save;

    public void run() {
        // hier echtzeit uhr einbauen

        calendar = Calendar.getInstance();
        minutenSave = calendar.get(Calendar.MINUTE);
        stunden_12_save = calendar.get(Calendar.HOUR);

        // Lade Konfigurationen aus Datenbank
        PlatinenDatabaseHelper dbHelper = PlatinenDatabaseHelper.getInstance(TurmtechnikActivity.turmtechnikContext);
        nebenuhrA_config = dbHelper.getNebenuhrByZeile(3); // Zeile 3 = Uhr A
        nebenuhrB_config = dbHelper.getNebenuhrByZeile(4); // Zeile 4 = Uhr B
        nebenuhrC_config = dbHelper.getNebenuhrByZeile(5); // Zeile 5 = Uhr C
        nebenuhrD_config = dbHelper.getNebenuhrByZeile(6); // Zeile 6 = Uhr D (Monduhr)
        
        // Lade lastRelaisA aus Datenbank für Nebenuhren A, B, C – aber nur wenn der jeweilige
        // Impuls-Thread nicht schon läuft (z. B. beim Aufholen). Sonst würde der jede Minute
        // startende NebenUhrThread den vom laufenden Thread gesetzten Stand überschreiben und
        // es könnten zwei Impulse hintereinander auf dasselbe Relais kommen.
        if (nebenuhrA_config != null && !StaticVariable.uhrA_doRun) {
            StaticVariable.uhrA_lastRelaisA = nebenuhrA_config.lastRelaisA;
        }
        if (nebenuhrB_config != null && !StaticVariable.uhrB_doRun) {
            StaticVariable.uhrB_lastRelaisA = nebenuhrB_config.lastRelaisA;
        }
        if (nebenuhrC_config != null && !StaticVariable.uhrC_doRun) {
            StaticVariable.uhrC_lastRelaisA = nebenuhrC_config.lastRelaisA;
        }
        if (nebenuhrD_config != null) {
            StaticVariable.uhrD_lastRelaisA = nebenuhrD_config.lastRelaisA;
            StaticVariable.uhrD_mondphaseIst = nebenuhrD_config.mondphaseIst;
        }
        incrementUhren(); // incrementiert alle 1 Minute in Echtzeit die
        // echtzeit Minutenzähler der Nebenuhren A,B,C
        // und startet den zugehörigen Impuls-Task
        
        // Monduhr D: Starte Thread wenn aktiv (Soll wird erst im Thread berechnet, daher nicht auf Soll!=Ist prüfen)
        // WICHTIG: Synchronisiert, um zu verhindern, dass mehrere Threads gleichzeitig gestartet werden
        synchronized (NebenUhrThread.class) {
            if (nebenuhrD_config != null && nebenuhrD_config.aktiv) {
                // Prüfe, ob bereits ein Thread läuft (sowohl isAlive() als auch uhrD_doRun prüfen)
                boolean threadLaeuft = (nebenuhrD_thread != null && nebenuhrD_thread.isAlive()) || StaticVariable.uhrD_doRun;
                
                if (!threadLaeuft) {
                    // Stoppe alten Thread falls vorhanden (sollte nicht nötig sein, aber sicherheitshalber)
                    if (nebenuhrD_thread != null && nebenuhrD_thread.isAlive()) {
                        Log.w("NebenUhrThread", "WARNUNG: Alter NebenUhrD_thread läuft noch, stoppe ihn...");
                        StaticVariable.uhrD_doRun = false;
                        try {
                            nebenuhrD_thread.join(1000); // Warte max. 1 Sekunde
                        } catch (InterruptedException e) {
                            Log.e("NebenUhrThread", "Fehler beim Warten auf alten Thread", e);
                        }
                    }
                    
                    // Starte neuen Thread (uhrD_doRun SOFORT setzen, bevor Thread gestartet wird)
                    StaticVariable.uhrD_doRun = true;
                    nebenuhrD_thread = new NebenUhrD_thread();
                    nebenuhrD_thread.start();
                    Log.d("NebenUhrThread", "NebenUhrD_thread gestartet - beginnt mit Aufholen (Thread-ID: " + nebenuhrD_thread.getId() + ")");
                } else {
                    // Thread läuft bereits oder wird gerade gestartet, nichts tun
                    if (nebenuhrD_thread != null) {
                        Log.d("NebenUhrThread", "NebenUhrD_thread läuft bereits (Thread-ID: " + nebenuhrD_thread.getId() + ", isAlive: " + nebenuhrD_thread.isAlive() + ", uhrD_doRun: " + StaticVariable.uhrD_doRun + "), kein neuer Start nötig");
                    }
                }
            } else if (nebenuhrD_config != null && !nebenuhrD_config.aktiv) {
                // Monduhr ist deaktiviert, stoppe Thread falls er läuft
                if (nebenuhrD_thread != null && nebenuhrD_thread.isAlive()) {
                    Log.d("NebenUhrThread", "Monduhr D deaktiviert, stoppe Thread");
                    StaticVariable.uhrD_doRun = false;
                }
            }
        }

        calendar = null;
        System.gc();
    } // ende von run

    private synchronized void incrementUhren() // erhoehe minuten timer und starte den Nebenuhr A,B,C task wenn er nicht laeuft
    {
        // A, B, C: nur 12h (0–719). D: 12h (719).
        final int maxUhrCount12 = 719;
        int calendarMinuten12;

        // Nach Load: erste volle Minute abwarten, dann Soll = RTC setzen; danach Aufholen oder Warten
        if (StaticVariable.nebenuhrWaitFirstFullMinute) {
            StaticVariable.nebenuhrWaitFirstFullMinute = false;
            if (nebenuhrA_config != null && nebenuhrA_config.aktiv) {
                StaticVariable.uhrA_calendarZeit = getCalendarMinuten();
            }
            if (nebenuhrB_config != null && nebenuhrB_config.aktiv) {
                StaticVariable.uhrB_calendarZeit = getCalendarMinuten();
            }
            if (nebenuhrC_config != null && nebenuhrC_config.aktiv) {
                StaticVariable.uhrC_calendarZeit = getCalendarMinuten();
            }
            // Warten vs. Takten: NU voraus → warten (keine Impulse), NU zurück → aufholen
            if (nebenuhrA_config != null && nebenuhrA_config.aktiv) setWartenLaufen_A();
            if (nebenuhrB_config != null && nebenuhrB_config.aktiv) setWartenLaufen_B();
            if (nebenuhrC_config != null && nebenuhrC_config.aktiv) setWartenLaufen_C();
        }

        // Uhr A: nur wenn aktiv – nur 12h (0–719). Während Zeiteingabe (Timepicker offen) Kalender nicht vorrücken lassen.
        if (nebenuhrA_config != null && nebenuhrA_config.aktiv) {
            if (StaticVariable.uhr_zeiteingabeAktiv.length <= 0 || !StaticVariable.uhr_zeiteingabeAktiv[0]) {
                int calendarA = getCalendarMinuten();
                boolean aAufholen = !StaticVariable.uhr_warten[0] && (StaticVariable.uhrA_angezeigteZeit != StaticVariable.uhrA_calendarZeit);
                if (!aAufholen) {
                    StaticVariable.uhrA_calendarZeit++;
                    if (StaticVariable.uhrA_calendarZeit > maxUhrCount12) {
                        StaticVariable.uhrA_calendarZeit = 0;
                    }
                    if (calendarA != StaticVariable.uhrA_calendarZeit) {
                        StaticVariable.uhrA_calendarZeit = calendarA;
                        setWartenLaufen_A();
                    }
                }
                Log.e("warte/", "Uhr A calendar=" + StaticVariable.uhrA_calendarZeit + " angezeigt " + StaticVariable.uhrA_angezeigteZeit + (aAufholen ? " (Aufholen, RTC ignoriert)" : ""));
            }
            if (StaticVariable.uhrA_doRun == false && !StaticVariable.uhr_zeiteingabeAktiv[0]) {
                StaticVariable.uhrA_doRun = true;
                nebenuhrA_thread = new NebenUhrA_thread();
                nebenuhrA_threadRef = nebenuhrA_thread;
                nebenuhrA_thread.start();
            }
        } else if (nebenuhrA_config != null && !nebenuhrA_config.aktiv) {
            StaticVariable.uhrA_doRun = false;
        }

        // Uhr B: nur wenn aktiv – nur 12h (0–719). Während Zeiteingabe Kalender nicht vorrücken lassen.
        if (nebenuhrB_config != null && nebenuhrB_config.aktiv) {
            if (StaticVariable.uhr_zeiteingabeAktiv.length <= 1 || !StaticVariable.uhr_zeiteingabeAktiv[1]) {
                int calendarB = getCalendarMinuten();
                boolean bAufholen = !StaticVariable.uhr_warten[1] && (StaticVariable.uhrB_angezeigteZeit != StaticVariable.uhrB_calendarZeit);
                if (!bAufholen) {
                    StaticVariable.uhrB_calendarZeit++;
                    if (StaticVariable.uhrB_calendarZeit > maxUhrCount12) {
                        StaticVariable.uhrB_calendarZeit = 0;
                    }
                    if (calendarB != StaticVariable.uhrB_calendarZeit) {
                        StaticVariable.uhrB_calendarZeit = calendarB;
                        setWartenLaufen_B();
                    }
                }
            }
            if (StaticVariable.uhrB_doRun == false && !StaticVariable.uhr_zeiteingabeAktiv[1]) {
                StaticVariable.uhrB_doRun = true;
                nebenuhrB_thread = new NebenUhrB_thread();
                nebenuhrB_threadRef = nebenuhrB_thread;
                nebenuhrB_thread.start();
            }
        } else if (nebenuhrB_config != null && !nebenuhrB_config.aktiv) {
            StaticVariable.uhrB_doRun = false;
        }

        // Uhr C: nur wenn aktiv – nur 12h (0–719). Während Zeiteingabe Kalender nicht vorrücken lassen.
        if (nebenuhrC_config != null && nebenuhrC_config.aktiv) {
            if (StaticVariable.uhr_zeiteingabeAktiv.length <= 2 || !StaticVariable.uhr_zeiteingabeAktiv[2]) {
                int calendarC = getCalendarMinuten();
                boolean cAufholen = !StaticVariable.uhr_warten[2] && (StaticVariable.uhrC_angezeigteZeit != StaticVariable.uhrC_calendarZeit);
                if (!cAufholen) {
                    StaticVariable.uhrC_calendarZeit++;
                    if (StaticVariable.uhrC_calendarZeit > maxUhrCount12) {
                        StaticVariable.uhrC_calendarZeit = 0;
                    }
                    if (calendarC != StaticVariable.uhrC_calendarZeit) {
                        StaticVariable.uhrC_calendarZeit = calendarC;
                        setWartenLaufen_C();
                    }
                }
            }
            if (StaticVariable.uhrC_doRun == false && !StaticVariable.uhr_zeiteingabeAktiv[2]) {
                StaticVariable.uhrC_doRun = true;
                nebenuhrC_thread = new NebenUhrC_thread();
                nebenuhrC_threadRef = nebenuhrC_thread;
                nebenuhrC_thread.start();
            }
        } else if (nebenuhrC_config != null && !nebenuhrC_config.aktiv) {
            StaticVariable.uhrC_doRun = false;
        }

        // Uhr D: 12h, RTC wie bisher (kein Aufhol-Schutz nötig bzw. D hat eigenes Verhalten)
        if (nebenuhrD_config != null && "12".equals(nebenuhrD_config.modus)) {
            StaticVariable.uhrD_calendarZeit++;
            if (StaticVariable.uhrD_calendarZeit > maxUhrCount12) {
                StaticVariable.uhrD_calendarZeit = 0;
            }
            calendarMinuten12 = getCalendarMinuten();
            if (calendarMinuten12 != StaticVariable.uhrD_calendarZeit) {
                StaticVariable.uhrD_calendarZeit = calendarMinuten12;
                setWartenLaufen_D();
            }
        }
    }

    /**
     * Startet den Nebenuhr-Thread A/B/C sofort neu, falls er nicht läuft (z. B. nach Timepicker-Hold war er beendet).
     * Wird von der Web-UI nach Confirm aufgerufen, damit die Uhr nicht bis zum nächsten Minuten-Takt warten muss.
     */
    public static void restartNebenuhrIfNeeded(android.content.Context ctx, int index) {
        if (ctx == null || index < 0 || index > 2) return;
        PlatinenDatabaseHelper dbHelper = PlatinenDatabaseHelper.getInstance(ctx);
        if (index == 0) {
            StaticVariable.uhrA_doRun = true;
            if (nebenuhrA_threadRef == null || !nebenuhrA_threadRef.isAlive()) {
                PlatinenDatabaseHelper.NebenuhrConfig config = dbHelper.getNebenuhrByZeile(3);
                if (config != null && config.aktiv) {
                    nebenuhrA_threadRef = new NebenUhrA_thread();
                    nebenuhrA_threadRef.start();
                    Log.d("NebenUhrThread", "Nebenuhr A nach Confirm neu gestartet");
                }
            }
        } else if (index == 1) {
            StaticVariable.uhrB_doRun = true;
            if (nebenuhrB_threadRef == null || !nebenuhrB_threadRef.isAlive()) {
                PlatinenDatabaseHelper.NebenuhrConfig config = dbHelper.getNebenuhrByZeile(4);
                if (config != null && config.aktiv) {
                    nebenuhrB_threadRef = new NebenUhrB_thread();
                    nebenuhrB_threadRef.start();
                    Log.d("NebenUhrThread", "Nebenuhr B nach Confirm neu gestartet");
                }
            }
        } else if (index == 2) {
            StaticVariable.uhrC_doRun = true;
            if (nebenuhrC_threadRef == null || !nebenuhrC_threadRef.isAlive()) {
                PlatinenDatabaseHelper.NebenuhrConfig config = dbHelper.getNebenuhrByZeile(5);
                if (config != null && config.aktiv) {
                    nebenuhrC_threadRef = new NebenUhrC_thread();
                    nebenuhrC_threadRef.start();
                    Log.d("NebenUhrThread", "Nebenuhr C nach Confirm neu gestartet");
                }
            }
        }
    }

    /** 12h-Minuten (0–719) für Nebenuhren A, B, C. */
    private int getCalendarMinuten() {
        return (stunden_12_save * 60) + minutenSave;
    }

//	private void printSheetDebug()
//	{
//		for (int j = 0 ; j < 7 ; j ++)
//		{
//			for ( int i = 0 ; i < 10; i ++) 
//			{
//			// Log.i("while" , "zeile=" + excelread.getCellString(i, j));
//			}
//		}
//	}

    /**
     * Berechnet Warten vs. Takten (Aufholen) neu und setzt uhr_warten[0..3].
     * Soll nach Laden der Nebenuhr (Start), nach Öffnen/Stellen in SetNebenuhrActivity und nach Web-UI-Confirm aufgerufen werden.
     */
    public static void recomputeWartenLaufen(android.content.Context context) {
        if (context == null) return;
        int min12 = (java.util.Calendar.getInstance().get(java.util.Calendar.HOUR) * 60)
                + java.util.Calendar.getInstance().get(java.util.Calendar.MINUTE);
        PlatinenDatabaseHelper dbHelper = PlatinenDatabaseHelper.getInstance(context);
        // A
        PlatinenDatabaseHelper.NebenuhrConfig configA = dbHelper.getNebenuhrByZeile(3);
        if (configA != null && configA.aktiv) {
            int dw = uhrMinutenWartenStatic(StaticVariable.uhrA_angezeigteZeit, min12) * 60;
            int impulsDauer = (configA.impulsDauer1 + configA.impulsDauer2);
            if (impulsDauer <= 0) impulsDauer = 1;
            int dt = uhrTakteStatic(StaticVariable.uhrA_angezeigteZeit, min12) * impulsDauer;
            StaticVariable.uhr_warten[0] = dt > dw;
        }
        // B
        PlatinenDatabaseHelper.NebenuhrConfig configB = dbHelper.getNebenuhrByZeile(4);
        if (configB != null && configB.aktiv) {
            int dw = uhrMinutenWartenStatic(StaticVariable.uhrB_angezeigteZeit, min12) * 60;
            int impulsDauer = (configB.impulsDauer1 + configB.impulsDauer2);
            if (impulsDauer <= 0) impulsDauer = 1;
            int dt = uhrTakteStatic(StaticVariable.uhrB_angezeigteZeit, min12) * impulsDauer;
            StaticVariable.uhr_warten[1] = dt > dw;
        }
        // C
        PlatinenDatabaseHelper.NebenuhrConfig configC = dbHelper.getNebenuhrByZeile(5);
        if (configC != null && configC.aktiv) {
            int dw = uhrMinutenWartenStatic(StaticVariable.uhrC_angezeigteZeit, min12) * 60;
            int impulsDauer = (configC.impulsDauer1 + configC.impulsDauer2);
            if (impulsDauer <= 0) impulsDauer = 1;
            int dt = uhrTakteStatic(StaticVariable.uhrC_angezeigteZeit, min12) * impulsDauer;
            StaticVariable.uhr_warten[2] = dt > dw;
        }
        // D (Monduhr: 12h-Vergleich wie A/B/C; Soll für Vergleich = aktuelle RTC)
        StaticVariable.uhrD_calendarZeit = min12;
        PlatinenDatabaseHelper.NebenuhrConfig configD = dbHelper.getNebenuhrByZeile(6);
        if (configD != null && configD.aktiv) {
            int dw = uhrMinutenWartenStatic(StaticVariable.uhrD_angezeigteZeit, StaticVariable.uhrD_calendarZeit) * 60;
            int impulsDauer = (configD.impulsDauer1 + configD.impulsDauer2);
            if (impulsDauer <= 0) impulsDauer = 1;
            int dt = uhrTakteStatic(StaticVariable.uhrD_angezeigteZeit, StaticVariable.uhrD_calendarZeit) * impulsDauer;
            StaticVariable.uhr_warten[3] = dt > dw;
        }
    }

    /** 12h: max 719. Statische Variante für recomputeWartenLaufen. */
    private static int uhrMinutenWartenStatic(int angezeigteZeit, int momentaneZeit) {
        int differenz = 0;
        final int max12 = 719;
        int maxMin = max12;
        if (angezeigteZeit > maxMin) angezeigteZeit = angezeigteZeit - 720;
        if (momentaneZeit > maxMin) momentaneZeit = momentaneZeit - 720;
        while (angezeigteZeit != momentaneZeit) {
            momentaneZeit++;
            if (momentaneZeit > maxMin) momentaneZeit = 0;
            differenz++;
        }
        return differenz;
    }

    /** 12h: max 719. Statische Variante für recomputeWartenLaufen. */
    private static int uhrTakteStatic(int angezeigteZeit, int momentaneZeit) {
        int differenz = 0;
        final int max12 = 719;
        int maxMin = max12;
        if (momentaneZeit > maxMin) momentaneZeit = momentaneZeit - 720;
        if (angezeigteZeit > maxMin) angezeigteZeit = angezeigteZeit - 720;
        while (angezeigteZeit != momentaneZeit) {
            angezeigteZeit++;
            if (angezeigteZeit > maxMin) angezeigteZeit = 0;
            differenz++;
        }
        return differenz;
    }

    /** 12h: max 719. */
    private int uhrMinutenWarten(int angezeigteZeit, int momentaneZeit) // gibt minuten zurueck
    {
        int differenz = 0;
        final int maxMin = 719;

        if (angezeigteZeit > maxMin) {
            angezeigteZeit = angezeigteZeit - 720;
        }
        if (momentaneZeit > maxMin) {
            momentaneZeit = momentaneZeit - 720;
        }

        Log.e("warte/uhrMinutenWarten", "angezeigt=" + angezeigteZeit + " momentan=" + momentaneZeit + " max=" + maxMin);

        while (angezeigteZeit != momentaneZeit) {
            momentaneZeit++;
            if (momentaneZeit > maxMin) {
                momentaneZeit = 0;
            }
            differenz++;
        }

        Log.e("warte/minuten", "=" + differenz);
        return differenz;
    }

    /** 12h: max 719. */
    private int uhrTakte(int angezeigteZeit, int momentaneZeit) // gibt minuten zurueck
    {
        int differenz = 0;
        final int maxMin = 719;

        if (momentaneZeit > maxMin) {
            momentaneZeit = momentaneZeit - 720;
        }
        if (angezeigteZeit > maxMin) {
            angezeigteZeit = angezeigteZeit - 720;
        }

        while (angezeigteZeit != momentaneZeit) {
            angezeigteZeit++;
            if (angezeigteZeit > maxMin) {
                angezeigteZeit = 0;
            }
            differenz++;
        }

        return differenz;
    }

    private int impulsDauerA() {
        if (nebenuhrA_config != null) {
            return nebenuhrA_config.impulsDauer1 + nebenuhrA_config.impulsDauer2;
        }
        return 0;
    }

    private int impulsDauerB() {
        if (nebenuhrB_config != null) {
            return nebenuhrB_config.impulsDauer1 + nebenuhrB_config.impulsDauer2;
        }
        return 0;
    }

    private int impulsDauerC() {
        if (nebenuhrC_config != null) {
            return nebenuhrC_config.impulsDauer1 + nebenuhrC_config.impulsDauer2;
        }
        return 0;
    }

    private int impulsDauerD() {
        if (nebenuhrD_config != null) {
            return nebenuhrD_config.impulsDauer1 + nebenuhrD_config.impulsDauer2;
        }
        return 0;
    }

    int differenzBeimWarten;
    int differenzBeimTakten;

    private void setWartenLaufen_A() {
        differenzBeimWarten = uhrMinutenWarten(StaticVariable.uhrA_angezeigteZeit, StaticVariable.uhrA_calendarZeit);
        differenzBeimWarten = differenzBeimWarten * 60; // in sekunden

        int impulsDauer = impulsDauerA();
        if (impulsDauer <= 0) impulsDauer = 1; // Verhindert: Takten=0 → Uhr würde nie warten und vorgehen
        differenzBeimTakten = uhrTakte(StaticVariable.uhrA_angezeigteZeit, StaticVariable.uhrA_calendarZeit);
        differenzBeimTakten = (differenzBeimTakten * impulsDauer);

        Log.e("warte/takten A", "= " + differenzBeimTakten + "= " + differenzBeimWarten);
        if (differenzBeimTakten <= differenzBeimWarten) {
            StaticVariable.uhr_warten[0] = false; // takten ist schneller
        } else {
            StaticVariable.uhr_warten[0] = true; // warten ist schneller
        }

        Log.e("warte/flag", "=" + StaticVariable.uhr_warten[0]);
    }

    private void setWartenLaufen_B() {
        differenzBeimWarten = uhrMinutenWarten(StaticVariable.uhrB_angezeigteZeit, StaticVariable.uhrB_calendarZeit);
        differenzBeimWarten = differenzBeimWarten * 60;
        int impulsDauer = impulsDauerB();
        if (impulsDauer <= 0) impulsDauer = 1;
        differenzBeimTakten = uhrTakte(StaticVariable.uhrB_angezeigteZeit, StaticVariable.uhrB_calendarZeit);
        differenzBeimTakten = (differenzBeimTakten * impulsDauer);
        //Log.i("warte/takten B" , "= " + differenzBeimWarten + "= " + differenzBeimTakten) ;
        if (differenzBeimTakten <= differenzBeimWarten) {
            StaticVariable.uhr_warten[1] = false; // takten ist schneller
        } else {
            StaticVariable.uhr_warten[1] = true; // warten ist schneller
        }
    }

    private void setWartenLaufen_C() {
        differenzBeimWarten = uhrMinutenWarten(StaticVariable.uhrC_angezeigteZeit, StaticVariable.uhrC_calendarZeit);
        differenzBeimWarten = differenzBeimWarten * 60;
        int impulsDauer = impulsDauerC();
        if (impulsDauer <= 0) impulsDauer = 1;
        differenzBeimTakten = uhrTakte(StaticVariable.uhrC_angezeigteZeit, StaticVariable.uhrC_calendarZeit);
        differenzBeimTakten = (differenzBeimTakten * impulsDauer);
        //Log.i("warte/takten c" , "= " + differenzBeimWarten + "= " + differenzBeimTakten) ;
        if (differenzBeimTakten <= differenzBeimWarten) {
            StaticVariable.uhr_warten[2] = false; // takten ist schneller
        } else {
            StaticVariable.uhr_warten[2] = true; // warten ist schneller
        }
    }

    private void setWartenLaufen_D() {
        differenzBeimWarten = uhrMinutenWarten(StaticVariable.uhrD_angezeigteZeit, StaticVariable.uhrD_calendarZeit);
        differenzBeimWarten = differenzBeimWarten * 60;
        differenzBeimTakten = uhrTakte(StaticVariable.uhrD_angezeigteZeit, StaticVariable.uhrD_calendarZeit);
        differenzBeimTakten = (differenzBeimTakten * impulsDauerD());
        if (differenzBeimTakten <= differenzBeimWarten) {
            StaticVariable.uhr_warten[3] = false; // takten ist schneller
        } else {
            StaticVariable.uhr_warten[3] = true; // warten ist schneller
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
