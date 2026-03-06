package tom.turmtechnik;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.media.AudioManager;
import android.media.SoundPool;
//import android.util.Log;

public class OldStartBigClockThread extends Thread {
    private final int SLEEP_TIME = 1000;  // 1 Sekunde sleep
    private final int SET_TIME_OUT = SLEEP_TIME * 60 * 15;  // nach 15 Minuten keine Aktion = grosser Uhr
    // activiy starten
    private int timeOut;

    public void run() {
        timeOut = SET_TIME_OUT; // Uhr aufziehen
        // triggerClockTimer(); // Uhr aufziehen
        while (timeOut > 0) {
            sleepMs(SLEEP_TIME);
            decrementClockTimer();
        }
        // zeit abgelaufen Uhr activity starten


    } // ende von run

    public synchronized void triggerClockTimer() {
        timeOut = SET_TIME_OUT; // Uhr aufziehen
    }

    private synchronized void decrementClockTimer() {
        timeOut--;
    }

    private void sleepMs(long time) {
        try {
            Thread.sleep(time);
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

} // ende der Klasse
