package tom.turmtechnik;

import android.util.Log;


import java.io.IOException;

import jxl.read.biff.BiffException;

public class HeizungManualThread extends Thread {
    private final String sourceFileName = "HeizungManualThread";

    private ExcelRead excelRead;
    private final int HEIZUNG_TIMER_SHEET_NUMBER = 10;
    //private int heizungOnTime = 0 ;

    long[] onAndOffSekunden = {0L, 0L};

    private boolean heizungManualThreadRun = true;

    public HeizungManualThread() {
        heizungManualThreadRun = true;
    }

    public void run() {
        // zuerst Zeit auf System sheet 10 (1,3) holen
        excelRead = new ExcelRead();

        try {
            excelRead.openXlsSheet(TurmtechnikActivity.systemFileString, HEIZUNG_TIMER_SHEET_NUMBER);
        } catch (BiffException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        String heizungTimeMinutenString = "0";

        try {
            heizungTimeMinutenString = excelRead.getCellString(1, 3);

        } catch (Exception e) {
            //e.printStackTrace();
            heizungTimeMinutenString = "0";
        }

        Log.e("Heizung", "String=" + heizungTimeMinutenString);

        try {
            StaticVariable.heizungOnTimer = Integer.parseInt(heizungTimeMinutenString);
        } catch (Exception e) {
            StaticVariable.heizungOnTimer = 0;
        }

        excelRead.closeWorkbook();

        Log.e("Heizung StaticVariable", ".heizungOnTimer=" + StaticVariable.heizungOnTimer);

        // wenn die Zeit 0 nichts machen --> thread beenden
        if (StaticVariable.heizungOnTimer == 0) {
            heizungManualThreadRun = false;
        }

        // ansonsten diese Zeit warten, dann Heizung ausschalten und Thread beenden


        while (heizungManualThreadRun) {
            Log.e("HeizungManual", "thread laeft");
            waitRealTime(1000 * 60); // 1 Minute warten
            StaticVariable.heizungOnTimer--;
            Log.e("HeizungManual", "Restzeit=" + StaticVariable.heizungOnTimer);
            if (StaticVariable.heizungOnTimer <= 0) {
                heizungManualThreadRun = false;
                heizungRelaisAusschalten();
            }

        }
    }  // ende vom HeizungManualThread

    private void heizungRelaisAusschalten() {
        relaisOff(StaticVariable.heizungRelaisNumberManual2);

    }

    private void relaisOn(int relaisNummer) {

        int relaisOffset = getRelaisOffset(relaisNummer);
        TurmtechnikActivity.globalOn[relaisOffset] = true;

        if (StaticVariable.serial_io_status4[0] == true) {
            Serial_IoThread.relaisNew[relaisNummer - 1] = true;
            // Meteor Code entfernt
        }
    }

    private void relaisOff(int relaisNummer) {
        int relaisOffset = getRelaisOffset(relaisNummer);
        TurmtechnikActivity.globalOn[relaisOffset] = false;

        if (StaticVariable.serial_io_status4[0] == true) {
            Serial_IoThread.relaisNew[relaisNummer - 1] = false;
            // Meteor Code entfernt
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


        while ((System.currentTimeMillis() < endMs) && (heizungManualThreadRun)) {
            //Log.e("SystemMs" , "System=" + tagesSuche.convertMsToTimeString(System.currentTimeMillis()) +
            //                    " endMs=" + tagesSuche.convertMsToTimeString(endMs)) ;
            sleepMs(50);
        }

    }

    private void sleepMs(long time) {
        time = time / 10;
        for (int i = 0; i < time; i++) {
            if (heizungManualThreadRun == false) {
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

    public void endHeizungManualThread() {

        heizungManualThreadRun = false;
    }

} // ende der Klasse
