package tom.turmtechnik;

//import android.util.Log;

public class HammerManualThread extends Thread
{
    /** Globaler Index (globalOn / Beschriftung). */
    private final int beschriftungTastenIndex;
    /** Index in relaisNew[] (relaisNummer - 1), damit bei mehreren Taps das richtige Relais aus geht. */
    private final int relaisNewIndex;
    private final long delayMs;

    HammerManualThread(int globalIndex, int relaisNummer, long delayMs)
    {
        this.beschriftungTastenIndex = globalIndex;
        this.relaisNewIndex = (relaisNummer > 0) ? (relaisNummer - 1) : 0;
        this.delayMs = delayMs > 0 ? delayMs : 50;
    }

    public void run()
    {
        StaticVariable.hammerThreadLaeuft = true;

        TurmtechnikActivity.startGlockenSound(beschriftungTastenIndex, StaticConstants.PLAY_SOUND_EINMALIG);

        TurmtechnikActivity.globalOn[beschriftungTastenIndex] = true;
        if (relaisNewIndex >= 0 && relaisNewIndex < Serial_IoThread.relaisNew.length) {
            Serial_IoThread.relaisNew[relaisNewIndex] = true;
        }

        sleepTime(delayMs);

        TurmtechnikActivity.globalOn[beschriftungTastenIndex] = false;
        if (relaisNewIndex >= 0 && relaisNewIndex < Serial_IoThread.relaisNew.length) {
            Serial_IoThread.relaisNew[relaisNewIndex] = false;
        }

        StaticVariable.hammerThreadLaeuft = false;
    } // ende von run


    private void sleepTime(long time) {
        try {
            Thread.sleep(time);
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

} // ende der Klasse
