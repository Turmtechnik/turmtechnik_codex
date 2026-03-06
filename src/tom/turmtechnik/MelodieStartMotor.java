package tom.turmtechnik;

import android.util.Log;



/**
 * Created by alfred on 24.01.16.
 */
public class MelodieStartMotor extends Thread {
    private long wait;
    private int relaisNummerMotor;
    private boolean doRun = true;
    int globalOnOffset;

    MelodieStartMotor(long wait, int relaisNummerMotor) {
        StaticVariable.stopBetaetigt = false;
        this.wait = wait;
        this.relaisNummerMotor = relaisNummerMotor;

        Log.e("MelodieMotor" , "StartMotor relaisNummerMotor=" + relaisNummerMotor + " wait=" + wait ) ;
}
    public void run()
    {
        Log.e("MelodieMotor" , "doRun=" + doRun);
        StaticVariable.stopBetaetigt = false;
        waitRealTime(wait);
        Log.e("MelodieMotor" , "StaticVariable.stopBetaetigt=" + StaticVariable.stopBetaetigt ) ;
        if (!StaticVariable.stopBetaetigt)
        {
            if(relaisNummerMotor != 0)
            {
                schalteMotorEin(relaisNummerMotor);
            }
        }
        //schalteMotorEin(relaisNummerMotor);
    }

    private void schalteMotorEin(int relaisNummerMotor)
    {
        Log.e("MelodieMotor" , "Schalte ein relaisNummerMotor=" + relaisNummerMotor + " wait=" + wait ) ;
        globalOnOffset = getGlobalOnOffset(relaisNummerMotor);
        Log.e("MelodieMotor" , "globalOnOffset=" + globalOnOffset) ;
        
        if (globalOnOffset >= 0 && globalOnOffset < TurmtechnikActivity.globalOn.length) {
            TurmtechnikActivity.globalOn[globalOnOffset] = true;
            
            // Verwende changeRelais() statt direkt relaisNew[] zu setzen
            if (relaisNummerMotor > 0 && relaisNummerMotor <= TurmtechnikActivity.RELAIS_COUNT) {
                Serial_IoThread.changeRelais(relaisNummerMotor, true);
                Log.e("MelodieMotor", "Motor-Relais " + relaisNummerMotor + " EIN geschaltet (Vorschwingen)");
            } else {
                Log.e("MelodieMotor", "FEHLER: Ungültige Relais-Nummer: " + relaisNummerMotor);
            }
        } else {
            Log.e("MelodieMotor", "FEHLER: Ungültiger globalOnOffset: " + globalOnOffset);
        }
        // Meteor Code entfernt
    }

    // Meteor Code entfernt: sendTastenStatusMeteor()
    // Meteor Code entfernt: makeMapObject()

    private int getGlobalOnOffset(int relaisNumber) {
        int returnRelaisOffset = 0;

        for (int i = 0; i < TurmtechnikActivity.RELAIS_COUNT; i++) {
            //Log.e("searchGlobalOnOffset" , "i=" + i + " relNr[i]=" + TurmtechnikActivity.relaisNumber[i] +
            //			"momentanRelNummer=" + relaisNumber) ;

            if (TurmtechnikActivity.relaisNumber[i] == relaisNumber) {
                returnRelaisOffset = i;
                Log.e("MelodieMotor" , "Motor getGlobalOnOffset=" + returnRelaisOffset);
                return returnRelaisOffset;
                //break ;
            }
        }

        Log.e("MelodieMotor" , "Motor getGlobalOnOffset=" + returnRelaisOffset);

        return returnRelaisOffset;
    }

    private void waitRealTime(long ms)
    {
        Log.e("MelodieMotor" , "waitRealTime ms=" + ms);

        if (ms <= 0) {
            return;
        }

        long startMs = System.currentTimeMillis();
        long endMs = startMs + ms;

        Log.e("MelodieMotor" , "end ms=" + ms);

        while ((System.currentTimeMillis() < endMs) && (doRun == true)) {
            sleepMs(50);
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
}
