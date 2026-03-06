package tom.turmtechnik;

import android.media.SoundPool;
import android.util.Log;


/**
 * Created by afred on 22.09.16.
 */

public class SoundGlocke {
    //private SoundPool sounds ;
    //private int soundID;
    //  private String filename;

    // public SoundGlocke(String filenname)
    // {
    //     this.filename = filenname ;
    // }



    /*
    public void initGlockeSound(String fname)
    {
        filename = fname ;


        //sound = new SoundPool(10, AudioManager.STREAM_MUSIC, 0 );


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            createNewSoundPool();
        } else {
            createOldSoundPool();
        }


        //Log.i("SOUND INIT" , "=" + filename1 ) ;
        //Log.i("SOUND INIT" , "=" + filename2) ;
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    protected void createNewSoundPool(){
        AudioAttributes attributes = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build();
        sounds = new SoundPool.Builder()
                .setAudioAttributes(attributes)
                .build();
    }

    @SuppressWarnings("deprecation")
    protected void createOldSoundPool(){
        sounds = new SoundPool(25,AudioManager.STREAM_MUSIC,0);
    }

    */


    public void playGlockeSound(final String filename, final int soundGlockeIndex, final int loop) // 0=ein mal, -1 = repeat 3 = drei mal...
    {
        if (filename == null || filename.trim().isEmpty()) {
            return; // Kein Dateiname → nicht laden (vermeidet "Sound/null"-Fehler)
        }
        int offset = (getSoundArraysOffset(soundGlockeIndex));
        Log.e("sound", "playGlockenSound offset=" + offset);
        if (offset == -1) // wenn sound noch nicht geladen war
        {
            final int soundID = StaticVariable.soundPool2.load(TurmtechnikActivity.sdCardPath + "/Turmtechnik/Sound/" + filename, 1);
            //sounds.load(TurmtechnikActivity.sdCardPath + "/Turmtechnik/Sound/" + filename, 1);
            Log.e("Sound", "ID = " + soundID);

            StaticVariable.soundPool2.setOnLoadCompleteListener(new SoundPool.OnLoadCompleteListener() {
                @Override
                public void onLoadComplete(SoundPool soundPool, int sampleId, int status) {

                    Log.e("sound", "playGlockenSound=" + soundID);

                    //sounds.play(soundID, 1.0f, 1.0f, 99, loop, 1f);
                    int streamID = startGlockeSound(soundID, loop);
                    StaticVariable.soundIndexList.add(soundGlockeIndex);
                    StaticVariable.soundIDsList.add(soundID);
                    StaticVariable.streamIDsList.add(streamID);
                    Log.e("sound", "playGlockenSound streamID=" + streamID);
                    // Stopp war aufgerufen, bevor Laden fertig war – sofort wieder stoppen
                    if (StaticVariable.stopGlockenSoundPending != null && StaticVariable.stopGlockenSoundPending.remove(soundGlockeIndex)) {
                        if (StaticVariable.soundPool2 != null) {
                            StaticVariable.soundPool2.stop(streamID);
                        }
                    }
                }
            });
        } else {
            Log.e("sound", "playGlockenSound offset=" + offset);
            int soundID = (getSoundID(offset));
            Log.e("sound", "vorhanden soundID =" + soundID);
            int streamID = startGlockeSound(soundID, loop);
            StaticVariable.streamIDsList.set(offset, streamID);
            Log.e("sound", "streamID =" + streamID);
        }
    }

    public int startGlockeSound(final int soundID, final int loop) {
        int streamID = StaticVariable.soundPool2.play(soundID, 1.0f, 1.0f, 99, loop, 1f);
        Log.e("sound", "startGlockeSound soundID = " + soundID);
        return streamID;
    }


    public void stopGlockenSound(int soundIndex) {
        int offset = getSoundArraysOffset(soundIndex);
        Log.e("sound", "stopGlockenSound offset=" + offset);
        if (offset != -1) {
            int streamID = getStreamID(offset);
            Log.e("sound", "stop getStreamID(offset)=" + streamID);
            if (StaticVariable.soundPool2 != null) {
                StaticVariable.soundPool2.stop(streamID);
            }
        } else {
            // Sound lädt noch asynchron – beim Load-Complete sofort stoppen
            if (StaticVariable.stopGlockenSoundPending != null) {
                StaticVariable.stopGlockenSoundPending.add(soundIndex);
            }
        }
    }

    private int getSoundArraysOffset(int soundIndex) {
        if (StaticVariable.soundIndexList == null) return -1;
        for (int i = 0; i < StaticVariable.soundIndexList.size(); i++) {
            if (StaticVariable.soundIndexList.get(i) == soundIndex) {
                return i;

            }
        }
        return -1;
    }

    public int getStreamID(int offset) {
        return StaticVariable.streamIDsList.get(offset);

    }

    public int getSoundID(int offset) {
        return StaticVariable.soundIDsList.get(offset);

    }
}
