package tom.turmtechnik;

/**
 * Konfiguration und Status für Zeitsynchronisation (Zeitserver, GPS, Sonnenauf-/untergang).
 * Hinweis: Nebenuhr-Laufzeitdaten (Uhr A/B/C/D) liegen ausschließlich in StaticVariable;
 * siehe docs/NEBENUHR_ARCHITEKTUR.md. Die früher hier vorhandenen uhrA/B/C_* und uhr_warten
 * wurden entfernt, da ungenutzt (Nebenuhr-Code nutzt nur StaticVariable).
 */
public class TimeConfig {
    // Zeitserver-Konfiguration (tatsächliche Nutzung erfolgt über StaticVariable)
    public static boolean timeServerOk = false;
    public static String timeServerEinAus = "EIN";
    public static String timeServerIp = "141.2.22.74"; // Time Server uni Frankfurt
    public static String timeServerMaxOffsetMinuten = "5";
    public static String timeServerAbfrageIntervallMs = "500";
    public static long readSntpTimeMs = 0; // wird noch von NMEA_gps_clock verwendet (GPS-Zeitsynchronisation)
    public static boolean schlagwerkTriggerSyncTime2 = false;

    // GPS/Serial GPS (tatsächliche Nutzung erfolgt über StaticVariable)
    public static boolean serialGPS_OnOff = false;
    public static String serialGPS_IP = "10.0.0.1";
    public static int serialGPS_port = 50210;
    public static int nmeaGpsNoData = 0;

    // Sonnenaufgang/Untergang (tatsächliche Nutzung erfolgt über StaticVariable)
    public static String sonnenAufgangStringGerundet = "25:61"; // default == unsinnige zeit
    public static String sonnenUntergangStringGerundet = "25:61"; // default == unsinnige zeit
    public static String sonnenAufgangString = "25:61"; // default == unsinnige zeit
    public static String sonnenUntergangString = "25:61"; // default == unsinnige zeit
}
