package tom.turmtechnik;

/**
 * Entity-Klasse für eine Platine.
 */
public class Platine {
    public int id;
    public int platineNummer;  // 1-4
    public String ipAdresse;
    public int port;
    public boolean aktiv;
    public String macAdresse;  // Für Bluetooth-Modus
    public boolean online;       // Für Bluetooth-Modus
    public int relaisAnzahl;     // Anzahl der Relais auf dieser Platine (Standard: 32)
    /** Erwartete WiFi-Impuls-Antwort (z. B. "impulsok"). Leer = alte Logik (mind. 8 Bytes). */
    public String impulsAntwortErwartet;
    
    public Platine() {
        this.id = 0;
        this.platineNummer = 0;
        this.ipAdresse = null;
        this.port = 50210;  // Default-Port
        this.aktiv = false;
        this.macAdresse = null;
        this.online = false;
        this.relaisAnzahl = 32;  // Standard: 32 Relais pro Platine
        this.impulsAntwortErwartet = null;
    }
    
    public Platine(int platineNummer, String ipAdresse, int port) {
        this.id = 0;
        this.platineNummer = platineNummer;
        this.ipAdresse = ipAdresse;
        this.port = port;
        this.aktiv = (ipAdresse != null && !ipAdresse.equals("0") && !ipAdresse.trim().isEmpty());
        this.macAdresse = null;
        this.online = false;
        this.relaisAnzahl = 32;  // Standard: 32 Relais pro Platine
        this.impulsAntwortErwartet = null;
    }
}
