package tom.turmtechnik;

import android.graphics.drawable.Drawable;
import java.util.ArrayList;

/**
 * UI-Zustand und Konfiguration für Layout, Buttons und Info-Text.
 */
public class UIState {
    // Layout-Status
    public static boolean layoutReady = false;
    public static boolean screenIsOff = true;

    // Info-Text
    public static int infoTextIndex = 0;
    public static String[] stringInfoTextField = new String[]{"AUTOMATIK AUSGESCHALTET", "", ""};
    public static String infoStringHeizung = "";
    public static String infoToastText = "";

    // Button-Konfiguration
    public static int tastenBreite;
    public static int tastenHoehe;
    public static int buttonsCount = -1;
    public static int buttonsCountError = 0;
    public static boolean makeBeschriftungTasten = false;

    // Button-Drawables und Texte
    public static Drawable btn_home_drawable;
    public static String btn_home_textstring;
    public static Drawable btn_help_drawable;
    public static String btn_help_textstring;
    public static Drawable btn_automatic_drawable;
    public static String btn_automatic_textstring;
    public static Drawable btn_stop_drawable;
    public static String btn_stop_textstring;

    // Activity-Status
    public static boolean seiteZweiIsRunning = false;
    public static boolean manuelerStartActivityIsRunning = false;

    // Editor
    public static String pathAndFilenameEditor;
    public static int pathAndFilenameEditorIndex = 0;
    public static int fileIndexForEditorTemp = -1;

    // Übersetzungen
    public static ArrayList<String> uebersetzteTexte;
}
