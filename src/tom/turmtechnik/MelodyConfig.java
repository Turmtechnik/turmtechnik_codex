package tom.turmtechnik;

import java.util.ArrayList;
import java.util.Vector;

/**
 * Konfiguration und Status für Melodien und Benutzerprogramme.
 */
public class MelodyConfig {
    // Melodie-Status
    public static boolean melodieAktiv = false;
    public static int indexMelodieStart = 0;  // index in array, echte melodie offset ist index + 3 !
    public static MelodieThreadNew melodieThreadNew = null;

    // Nächste Melodie
    public static String nameNextMelodie = "";
    public static String pathAndFileNameNextMelodie = "";
    public static long nextMelodieStartSekunden2 = 0;

    // Vorschwingen
    public static ArrayList<ArrayList<Long>> vorschwingenStartzeitenMotorRelais = new ArrayList<ArrayList<Long>>();
    public static ArrayList<Integer> vorschwingenMotorRelaisNeu = new ArrayList<Integer>();
    public static ArrayList<Integer> laeutenKloeppelRelais = new ArrayList<Integer>();
    public static ArrayList<Integer> vorschwingZeitSekunden = new ArrayList<Integer>();

    // Manueller Start
    public static boolean manuellerStartEinAus = false;
    public static int startStundeManuell = 0;
    public static int startMinuteManuell = 0;
    public static boolean manuellerStartAktiviert = false;

    // Benutzerprogramme
    public static boolean benutzerGeandert = false;
    public static boolean sendUserProgrammRemote2 = true;
    public static ArrayList<String> userProgramIds = new ArrayList<String>();
    public static boolean saveBenutzerProgrammSofort = false;
    public static int userProgramsCount = -1;
    public static int benutzerMelodienIndex = -1;
    public static boolean helpForStartBenutzermelodien = true;

    // Benutzer-Melodie-Buttons
    public static Vector<Boolean> benutzerMelodieTasteOn2 = new Vector<Boolean>();
    protected static Vector<Boolean> sofortStartButtonOn = new Vector<Boolean>();
    public static final Vector<Integer> minuten = new Vector<Integer>();
    public static final Vector<Integer> stunden = new Vector<Integer>();
    public static final Vector<Integer> monate = new Vector<Integer>();
    public static final Vector<Integer> tage = new Vector<Integer>();
    public static final Vector<Integer> jahre = new Vector<Integer>();

    protected static boolean firstStartMelodie;

    // Sofort-Start Popup
    public static boolean sofortStartPopupFlag = false;
    public static int sofortStartPopupRelaisNummerAktiv = 0;
    public static String sofortStartPopupFilename = "";
    public static boolean sofortStartPopupGefunden = false;
}
