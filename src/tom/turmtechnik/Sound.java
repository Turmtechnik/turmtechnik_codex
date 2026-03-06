package tom.turmtechnik;

import android.media.AudioManager;
import android.media.SoundPool;
//import android.util.Log;

public class Sound {
    private static int soundID1;
    private static int soundID2;
    private static SoundPool soundPool;

    public static void initSound(String filename1, String filename2) {
        soundPool = new SoundPool(10, AudioManager.STREAM_MUSIC, 0);
        soundID1 = soundPool.load(TurmtechnikActivity.sdCardPath + "/Turmtechnik/Grafiken/" + filename1, 1);
        soundID2 = soundPool.load(TurmtechnikActivity.sdCardPath + "/Turmtechnik/Grafiken/" + filename2, 1);
        //Log.i("SOUND INIT" , "=" + filename1 ) ;
        //Log.i("SOUND INIT" , "=" + filename2) ;
    }

    public static void playHammerSound1() {
        //Log.i("soundID1" , "=" + soundID1) ;
        soundPool.play(soundID1, 1.0f, 1.0f, 1, 0, 1f);
    }

    public static void playHammerSound2() {
        //Log.i("soundID2" , "=" + soundID2) ;
        soundPool.play(soundID2, 1.0f, 1.0f, 1, 0, 1f);
    }
}
