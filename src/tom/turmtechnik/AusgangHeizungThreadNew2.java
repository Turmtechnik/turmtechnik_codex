package tom.turmtechnik;

import android.util.Log;


import java.util.Calendar;

public class AusgangHeizungThreadNew2 extends Thread {
    private final String sourceFileName = "AusgangHeizungThreadNew2";

    long[] onAndOffSekunden = {0L, 0L};

    private boolean heizungThreadRun = true;

    public AusgangHeizungThreadNew2() {
        heizungThreadRun = true;
    }

    /** Maximale Wartezeit (ms), danach wird fortgesetzt auch wenn BenutzerMelodienActivity nicht lief. */
    private static final long MAX_WAIT_INIT_MS = 60_000L;

    public void run() {
        // Sofort nach Neustart: gespeicherten Ausgangszustand wiederherstellen (Relais wieder ein,
        // wenn Zeit noch nicht um). Damit bleibt der Ausgang nach App-Absturz/Neustart an bis zur geplanten Ausschaltzeit.
        try {
            if (TurmtechnikActivity.getRuntimeContext() != null) {
                SaveAndLoadHeizung restore = new SaveAndLoadHeizung();
                int relais = restore.loadHeizungRelais();
                if (relais > 0) {
                    long[] zeiten = restore.loadHeizungZeiten();
                    long jetzt = System.currentTimeMillis() / 1000L;
                    if (zeiten[0] > 0 && zeiten[1] > 0 && jetzt >= zeiten[0] && jetzt < zeiten[1]) {
                        relaisOn(relais);
                        Log.d(sourceFileName, "Ausgang nach Neustart wiederhergestellt: Relais " + relais + " bis " + zeiten[1]);
                    }
                }
            }
        } catch (Exception e) {
            Log.w(sourceFileName, "Wiederherstellung Ausgang nach Neustart fehlgeschlagen: " + e.getMessage());
        }

        int waitCount = 0;
        long waitStartMs = System.currentTimeMillis();
        while (StaticVariable.helpForStartBenutzermelodien == true) {
            waitRealTime(500);
            waitCount++;
            // Nur alle 10 Durchläufe (ca. 5 s) loggen, um Log-Spam zu vermeiden
            if (waitCount == 1) {
                Log.d("AusgangHeizungThreadNew2", "Warte auf Benutzerprogramm-Initialisierung ...");
            } else if (waitCount % 10 == 0) {
                Log.d("AusgangHeizungThreadNew2", "Warte weiter auf Benutzerprogramm-Initialisierung (" + (waitCount * 500 / 1000) + " s)");
            }
            if (System.currentTimeMillis() - waitStartMs >= MAX_WAIT_INIT_MS) {
                Log.w("AusgangHeizungThreadNew2", "Timeout beim Warten auf Benutzerprogramm-Initialisierung, fahre fort.");
                StaticVariable.helpForStartBenutzermelodien = false;
                break;
            }
        }

        SaveAndLoadHeizung saveAndLoadHeizung = new SaveAndLoadHeizung();

        while (heizungThreadRun) {
            StaticVariable.heizungRelaisNumber3 = saveAndLoadHeizung.loadHeizungRelais();
            //Log.e("Relais Heizung" , "= " + StaticVariable.heizungRelaisNumber2) ;

            if (StaticVariable.heizungEingeschaltetTimer > 0) {
                StaticVariable.heizungEingeschaltetTimer--;
            }

            if ((StaticVariable.heizungRelaisNumber3 > 0)) {
                onAndOffSekunden = saveAndLoadHeizung.loadHeizungZeiten();

                Calendar calendar = Calendar.getInstance();

                long momentanMsDurchTausend = (calendar.getTimeInMillis()) / 1000L;

                //Log.e("onAndOffSekunden", "[0]=" + onAndOffSekunden[0] + "[1]=" + onAndOffSekunden[1] + "momentan=" + momentanMsDurchTausend);

                if (momentanMsDurchTausend < onAndOffSekunden[1]) // momentan < ausschaltzeit ?
                {
                    if (momentanMsDurchTausend > onAndOffSekunden[0]) // momentan > einschaltzeit?
                    {
                        relaisOn(StaticVariable.heizungRelaisNumber3);
                        StaticVariable.heizungEingeschaltetTimer = 70; // 70 Sekunden heizungssuche blockieren
                    }
                    //else
                    //{
                    //    relaisOff(StaticVariable.heizungRelaisNumber);
                    //    StaticVariable.infoStringHeizung = "";
                    //}
                } else {
                    relaisOff(StaticVariable.heizungRelaisNumber3);
                    StaticVariable.infoStringHeizung = "";
                    saveAndLoadHeizung.clearHeizung();
                    heizungThreadRun = false;
                }
            }
            waitRealTime(1050); // etwas asynchron zu normalen zeiten
        }

    } // ende von run

    private void relaisOn(int relaisNummer) {
        int relaisOffset = getRelaisOffset(relaisNummer);
        TurmtechnikActivity.globalOn[relaisOffset] = true;
        // Immer setzen, damit nach Neustart (oder wenn Verbindung später steht) das Relais an die Hardware gesendet wird
        if (relaisNummer >= 1 && relaisNummer <= Serial_IoThread.relaisNew.length) {
            Serial_IoThread.relaisNew[relaisNummer - 1] = true;
        }
    }

    private void relaisOff(int relaisNummer) {
        int relaisOffset = getRelaisOffset(relaisNummer);
        TurmtechnikActivity.globalOn[relaisOffset] = false;
        if (relaisNummer >= 1 && relaisNummer <= Serial_IoThread.relaisNew.length) {
            Serial_IoThread.relaisNew[relaisNummer - 1] = false;
        }
    }

    // Meteor Code entfernt: sendTastenStatusMeteor()
    // Meteor Code entfernt: makeMapObject()

    private int getRelaisOffset(int relaisNumber) {
        int returnRelaisOffset = 0;

        for (int i = 0; i < TurmtechnikActivity.RELAIS_COUNT; i++) {
            if (TurmtechnikActivity.relaisNumber[i] == relaisNumber) {
                returnRelaisOffset = i;
                break;
            }
        }

        return returnRelaisOffset;
    }


    private void waitRealTime(long ms) {
        long startMs = System.currentTimeMillis();
        long endMs = startMs + ms;

        TagesSuche tagesSuche = new TagesSuche();


        while ((System.currentTimeMillis() < endMs) && (heizungThreadRun == true)) {
            //Log.e("SystemMs" , "System=" + tagesSuche.convertMsToTimeString(System.currentTimeMillis()) +
            //                    " endMs=" + tagesSuche.convertMsToTimeString(endMs)) ;
            sleepMs(50);
        }

    }

    private void sleepMs(long time) {
        time = time / 10;
        for (int i = 0; i < time; i++) {
            if (heizungThreadRun == false) {
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

    public void endHeizungThread() {
        heizungThreadRun = false;
    }

} // ende der Klasse
