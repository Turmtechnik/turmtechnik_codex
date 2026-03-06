package tom.turmtechnik;

import java.util.HashMap;
import java.util.Map;

/**
 * Mapping-Konfiguration für Excel zu Datenbank Migration.
 * Definiert, welche Excel-Zellen in welche Datenbankfelder migriert werden.
 */
public class ExcelToDatabaseMapping {
    
    /**
     * Mapping für System.xls Sheet 14 (Platinen & IO-Konfiguration).
     */
    public static class Sheet14Mapping {
        public static final int SHEET_NUMBER = 14;
        
        /**
         * Mapping-Definition: Excel-Zelle -> Datenbank-Feld
         * Format: "spalte,zeile" -> {tabelle, feld, key (für io_config)}
         */
        public static final Map<String, CellMapping> MAPPINGS = new HashMap<>();
        
        static {
            // Modus (0, 1) -> io_config: modus
            MAPPINGS.put("0,1", new CellMapping(
                "io_config", "modus", null, 
                "Bluetooth Modus (\"AUS\" = bluetooth, sonst wifi)"
            ));
            
            // IP-Adressen Platinen 1-4 (1-4, 1) -> platinen_config: ip_adresse
            for (int i = 1; i <= 4; i++) {
                MAPPINGS.put(i + ",1", new CellMapping(
                    "platinen_config", "ip_adresse", "platine_nummer=" + i,
                    "IP-Adresse Platine " + i
                ));
            }
            
            // Ports Platinen 1-4 (5-8, 1) -> platinen_config: port
            for (int i = 1; i <= 4; i++) {
                MAPPINGS.put((i + 4) + ",1", new CellMapping(
                    "platinen_config", "port", "platine_nummer=" + i,
                    "Port Platine " + i
                ));
            }
            
            // Scan-Relais MS (1, 2) -> io_config: scan_relais_ms
            MAPPINGS.put("1,2", new CellMapping(
                "io_config", "scan_relais_ms", null,
                "Scan-Relais MS"
            ));
            
            // Power off Akku % (1, 3) -> io_config: power_off_akku_prozent
            MAPPINGS.put("1,3", new CellMapping(
                "io_config", "power_off_akku_prozent", null,
                "Power off Akku Prozent"
            ));
            
            // Logfile On/Off (1, 4) -> io_config: logfile_on_off
            MAPPINGS.put("1,4", new CellMapping(
                "io_config", "logfile_on_off", null,
                "Logfile On/Off"
            ));
            
            // Reboot OK Flag (1, 5) -> io_config: reboot_ok_flag
            MAPPINGS.put("1,5", new CellMapping(
                "io_config", "reboot_ok_flag", null,
                "Reboot OK Flag"
            ));
            
            // Autostart der App (1, 6) -> io_config: autostart_der_app
            MAPPINGS.put("1,6", new CellMapping(
                "io_config", "autostart_der_app", null,
                "Autostart der App"
            ));
            
            // Password Exit Normal (2, 9) -> io_config: password_exit_normal
            MAPPINGS.put("2,9", new CellMapping(
                "io_config", "password_exit_normal", null,
                "Password Exit Normal"
            ));
        }
        
        /**
         * Gibt das Mapping für eine Excel-Zelle zurück.
         * @param spalte Spalte (0-basiert)
         * @param zeile Zeile (0-basiert)
         * @return CellMapping oder null wenn nicht gefunden
         */
        public static CellMapping getMapping(int spalte, int zeile) {
            String key = spalte + "," + zeile;
            return MAPPINGS.get(key);
        }
    }
    
    /**
     * Mapping für System.xls Sheet 15 (Nebenuhr-Konfiguration).
     */
    public static class Sheet15Mapping {
        public static final int SHEET_NUMBER = 15;
        
        /**
         * Mapping-Definition für Nebenuhr A, B, C (Zeilen 3, 4, 5).
         */
        public static final Map<String, CellMapping> MAPPINGS = new HashMap<>();
        
        static {
            // Für jede Nebenuhr (A=Zeile 3, B=Zeile 4, C=Zeile 5)
            String[] uhrNamen = {"A", "B", "C"};
            int[] zeilen = {3, 4, 5};
            
            for (int i = 0; i < uhrNamen.length; i++) {
                String uhrName = uhrNamen[i];
                int zeile = zeilen[i];
                
                // Impuls-Zeit (0, zeile) -> nebenuhr_config: impuls_zeit
                MAPPINGS.put("0," + zeile, new CellMapping(
                    "nebenuhr_config", "impuls_zeit", "uhr_name=" + uhrName + ",zeile_index=" + zeile,
                    "Impuls-Zeit Nebenuhr " + uhrName + " (Sekunden)"
                ));
                
                // Impuls-Pause (1, zeile) -> nebenuhr_config: impuls_pause
                MAPPINGS.put("1," + zeile, new CellMapping(
                    "nebenuhr_config", "impuls_pause", "uhr_name=" + uhrName + ",zeile_index=" + zeile,
                    "Impuls-Pause Nebenuhr " + uhrName + " (Sekunden)"
                ));
                
                // Relais-Nummer (gerade) (3, zeile) -> nebenuhr_config: relais_gerade
                MAPPINGS.put("3," + zeile, new CellMapping(
                    "nebenuhr_config", "relais_gerade", "uhr_name=" + uhrName + ",zeile_index=" + zeile,
                    "Relais-Nummer (gerade) Nebenuhr " + uhrName
                ));
                
                // Relais-Nummer (ungerade) (4, zeile) -> nebenuhr_config: relais_ungerade
                MAPPINGS.put("4," + zeile, new CellMapping(
                    "nebenuhr_config", "relais_ungerade", "uhr_name=" + uhrName + ",zeile_index=" + zeile,
                    "Relais-Nummer (ungerade) Nebenuhr " + uhrName
                ));
                
                // Uhr-Name (5, zeile) -> nebenuhr_config: uhr_name_display
                MAPPINGS.put("5," + zeile, new CellMapping(
                    "nebenuhr_config", "uhr_name_display", "uhr_name=" + uhrName + ",zeile_index=" + zeile,
                    "Uhr-Name Nebenuhr " + uhrName
                ));
            }
        }
        
        /**
         * Gibt das Mapping für eine Excel-Zelle zurück.
         * @param spalte Spalte (0-basiert)
         * @param zeile Zeile (0-basiert)
         * @return CellMapping oder null wenn nicht gefunden
         */
        public static CellMapping getMapping(int spalte, int zeile) {
            String key = spalte + "," + zeile;
            return MAPPINGS.get(key);
        }
    }
    
    /**
     * Repräsentiert ein Mapping von Excel-Zelle zu Datenbank-Feld.
     */
    public static class CellMapping {
        /** Datenbank-Tabelle */
        public final String tabelle;
        
        /** Datenbank-Feld/Spalte */
        public final String feld;
        
        /** Zusätzliche Bedingung (z.B. "platine_nummer=1" oder "key=modus") */
        public final String bedingung;
        
        /** Beschreibung des Feldes */
        public final String beschreibung;
        
        public CellMapping(String tabelle, String feld, String bedingung, String beschreibung) {
            this.tabelle = tabelle;
            this.feld = feld;
            this.bedingung = bedingung;
            this.beschreibung = beschreibung;
        }
        
        @Override
        public String toString() {
            return String.format("CellMapping{tabelle='%s', feld='%s', bedingung='%s', beschreibung='%s'}", 
                tabelle, feld, bedingung, beschreibung);
        }
    }
    
    /**
     * Gibt alle Mappings für ein bestimmtes Sheet zurück.
     * @param sheetNumber Sheet-Nummer (14 oder 15)
     * @return Map mit allen Mappings oder null wenn Sheet nicht unterstützt
     */
    public static Map<String, CellMapping> getAllMappings(int sheetNumber) {
        if (sheetNumber == Sheet14Mapping.SHEET_NUMBER) {
            return Sheet14Mapping.MAPPINGS;
        } else if (sheetNumber == Sheet15Mapping.SHEET_NUMBER) {
            return Sheet15Mapping.MAPPINGS;
        }
        return null;
    }
}
