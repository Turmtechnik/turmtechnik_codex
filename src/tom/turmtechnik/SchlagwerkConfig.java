package tom.turmtechnik;

/**
 * Konfiguration und Status für Schlagwerk.
 */
public class SchlagwerkConfig {
    public static boolean flagSchlagwerkOnOff = true;
    public static int beginnSchlagwerk1; // zeit in sekunden seit Mitternacht
    public static int endeSchlagwerk1;
    public static int beginnSchlagwerk2;
    public static int endeSchlagwerk2;
    // neu 4.12.2014 schlagwerk auch während eine Melodie geleutet wird // in Schlagwerkzeiten EIN/AUS
    public static boolean schlagwerkWaehrendMelodieLeuten;
    public static int schlagWerkVariable1 = 0; // das ist SWV1 z.b. in 10_Uhr.xls
}
