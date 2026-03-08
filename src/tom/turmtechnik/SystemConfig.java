package tom.turmtechnik;

/**
 * System-Konfiguration und Status-Variablen.
 * Enthält Variablen für System-Status, Hammer, Excel, und andere System-Konfigurationen.
 */
public class SystemConfig {
    // System-Status
    public static String serialNumber;
    public static String versionString;
    public static boolean autostartDerApp = true;
    public static boolean flagRebootOk = false;
    public static int errorCountToReboot = 0;

    // Hammer
    public static boolean hammerThreadLaeuft = false;
    public static int hammerDelayTime2 = 0;  // fuer HammerManualThread
    public static int hammerRelais = 0; // fuer HammerManualThread
    public static int hammerIndex = 0; // fuer HammerManualThread
    public static int hammerSound = 0; // fuer HammerManualThread

    // Excel
    public static int excelOpen = 0;
    public static int excelClose = 0;

    // Thread-Status
    public static boolean ddpThreadRun = true;
    // public static boolean sntpThreadRun = true; // Nicht mehr verwendet - TimeSyncThread verwendet doRun-Flag
    //public static boolean nmeaThreadRun = true;

    // Relais/Scan
    public static int scanRelaisMS;
    public static int turnOffVerknuepft4 = -1;

    // Sonstiges
    public static int minutenTakt = 0;
    public static int bigClockTimeout; // wird in TurmtechnikActivity gesetzt
    public static boolean sendTageslisteAktiv = false;
    public static int batt_shut_down_level2 = 75; // bei unter 75% ausschalten
    public static boolean programmAbfrageAktiv2 = false;
    public static boolean stopBetaetigt = false;
    public static boolean logOnOff;

    // Übersetzungs-Hilfsmethode (von StaticVariable übernommen)
    public static String getUebersetzung(int index) {
        return StaticVariable.getUebersetzung(index);
    }
}
