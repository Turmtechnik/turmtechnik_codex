package tom.turmtechnik;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.util.Vector;

import jxl.read.biff.BiffException;

/**
 * Factory für Repository-Instanzen.
 * Erstellt die passenden Repository-Implementierungen basierend auf dem Programmtyp.
 */
public class RepositoryFactory {
    private static final String TAG = "RepositoryFactory";
    
    // Feature-Flag: Später auf true setzen, wenn Datenbank verwendet werden soll
    private static final boolean USE_DATABASE = false;
    
    private static String getSdCardPath() {
        return Environment.getExternalStorageDirectory().getPath();
    }

    /**
     * Erstellt ein ProgrammRepository für den angegebenen Programmtyp.
     * @param programmTyp "normal", "festtag_variabel", "festtag_fest", "benutzer"
     * @return ProgrammRepository-Instanz
     */
    public static ProgrammRepository getProgrammRepository(String programmTyp) {
        if (USE_DATABASE) {
            // Später: return new DatabaseProgrammRepository(programmTyp);
            Log.w(TAG, "Datenbank-Repository noch nicht implementiert, verwende Excel");
        }
        
        // Für festtag_fest und festtag_variabel: Verwende MultiFileProgrammRepository
        // Diese enthalten mehrere Programmtage-Dateien im Ordner "Programmtage"
        if ("festtag_fest".equals(programmTyp)) {
            Vector<String> fileNames = TurmtechnikActivity.festeFesttageTagtypFileNamen;
            // Falls noch nicht geladen, lade sie jetzt
            if (fileNames == null || fileNames.isEmpty()) {
                fileNames = loadFesttageFileNames("festtag_fest");
            }
            if (fileNames != null && !fileNames.isEmpty()) {
                return new MultiFileProgrammRepository(fileNames, programmTyp);
            } else {
                Log.w(TAG, "Keine feste Festtage-Dateien gefunden, verwende Standard-Repository");
            }
        } else if ("festtag_variabel".equals(programmTyp)) {
            Vector<String> fileNames = TurmtechnikActivity.variableFesttageTagtypFileNamen;
            // Falls noch nicht geladen, lade sie jetzt
            if (fileNames == null || fileNames.isEmpty()) {
                fileNames = loadFesttageFileNames("festtag_variabel");
            }
            if (fileNames != null && !fileNames.isEmpty()) {
                return new MultiFileProgrammRepository(fileNames, programmTyp);
            } else {
                Log.w(TAG, "Keine variable Festtage-Dateien gefunden, verwende Standard-Repository");
            }
        }
        
        // Excel-Repository für normale Programme und Benutzerprogramme
        String filePath = getProgrammFilePath(programmTyp);
        return new ExcelProgrammRepository(filePath, programmTyp);
    }

    private static String getProgrammFilePath(String programmTyp) {
        String sdCardPath = getSdCardPath();
        
        switch (programmTyp) {
            case "normal":
                return sdCardPath + StaticConstants.normalprogrammString;
            case "festtag_variabel":
                return sdCardPath + StaticConstants.variableFesttageString;
            case "festtag_fest":
                return sdCardPath + StaticConstants.festeFesttageString;
            case "benutzer":
                return sdCardPath + StaticConstants.benutzerMelodienString;
            default:
                Log.e(TAG, "Unbekannter Programmtyp: " + programmTyp + ", verwende Normalprogramm");
                return sdCardPath + StaticConstants.normalprogrammString;
        }
    }
    
    /**
     * Lädt die Programmtage-Dateinamen direkt aus dem Programmtage-Ordner.
     * Liest ALLE .xls Dateien aus /Turmtechnik/Programmtage/ (NICHT aus System.xls!).
     * @param programmTyp "festtag_fest" oder "festtag_variabel" (wird aktuell ignoriert, alle Dateien werden geladen)
     * @return Vector mit vollständigen Dateipfaden
     */
    private static Vector<String> loadFesttageFileNames(String programmTyp) {
        Vector<String> fileNames = new Vector<String>();
        String sdCardPath = getSdCardPath();
        String programmtagePath = sdCardPath + "/Turmtechnik/Programmtage/";
        java.io.File programmtageDir = new java.io.File(programmtagePath);
        
        if (!programmtageDir.exists() || !programmtageDir.isDirectory()) {
            Log.w(TAG, "Programmtage-Ordner nicht gefunden: " + programmtagePath);
            return fileNames;
        }
        
        try {
            java.io.File[] files = programmtageDir.listFiles();
            if (files == null) {
                return fileNames;
            }
            
            for (java.io.File file : files) {
                if (file.isFile() && file.getName().toLowerCase().endsWith(".xls")) {
                    String fileName = file.getName();
                    String fullPath = file.getAbsolutePath();
                    
                    // Filtere ungültige Einträge (z.B. Nebenuhr-Namen)
                    String fileNameLower = fileName.toLowerCase().trim();
                    if (fileNameLower.equals("stubenuhr.xls") || 
                        fileNameLower.equals("nebenuhr.xls") ||
                        fileNameLower.equals("nebenuhr a.xls") ||
                        fileNameLower.equals("nebenuhr b.xls") ||
                        fileNameLower.equals("nebenuhr c.xls")) {
                        Log.w(TAG, "Ungültiger Eintrag ignoriert (Nebenuhr-Name?): " + fileName);
                        continue;
                    }
                    
                    fileNames.add(fullPath);
                }
            }
            
            Log.d(TAG, "Tagtypen aus Programmtage-Ordner geladen: " + fileNames.size() + " Dateien");
        } catch (Exception e) {
            Log.e(TAG, "Fehler beim Laden der Festtage-Dateinamen aus Programmtage-Ordner: " + programmTyp, e);
        }
        
        return fileNames;
    }
    
    /**
     * Gibt die Tagtyp-Dateinamen für einen Programmtyp zurück.
     * @param programmTyp "festtag_fest" oder "festtag_variabel"
     * @return Vector mit vollständigen Dateipfaden
     */
    public static Vector<String> getTagtypFileNames(String programmTyp) {
        if ("festtag_fest".equals(programmTyp)) {
            Vector<String> fileNames = TurmtechnikActivity.festeFesttageTagtypFileNamen;
            if (fileNames == null || fileNames.isEmpty()) {
                fileNames = loadFesttageFileNames("festtag_fest");
            }
            return fileNames != null ? fileNames : new Vector<String>();
        } else if ("festtag_variabel".equals(programmTyp)) {
            Vector<String> fileNames = TurmtechnikActivity.variableFesttageTagtypFileNamen;
            if (fileNames == null || fileNames.isEmpty()) {
                fileNames = loadFesttageFileNames("festtag_variabel");
            }
            return fileNames != null ? fileNames : new Vector<String>();
        }
        return new Vector<String>();
    }
}
