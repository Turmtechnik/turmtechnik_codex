package tom.turmtechnik;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;

/**
 * Created by alfred on 21.03.15.
 */
public class SaveAndLoadHeizung {
    SharedPreferences pref;

    // Konstruktor
    SaveAndLoadHeizung() {
        pref = PreferenceManager.getDefaultSharedPreferences(TurmtechnikActivity.turmtechnikContext);
    }

    public void saveHeizung(long onTime, long offTime, int relaisNumber) {
        SharedPreferences.Editor editor = pref.edit();

        editor.putLong("HeizungOn", onTime);
        editor.putLong("HeizungOff", offTime);
        editor.putInt("HeizungRelais", relaisNumber);
        editor.commit();
    }

    public long[] loadHeizungZeiten() {
        long[] temp = {0, 0};

        temp[0] = pref.getLong("HeizungOn", 0);
        temp[1] = pref.getLong("HeizungOff", 0);

        return temp;
    }

    public int loadHeizungRelais() {
        int temp = pref.getInt("HeizungRelais", 0);
        return temp;
    }

    public void clearHeizung() {
        saveHeizung(0L, 0L, 0);
    }
}
