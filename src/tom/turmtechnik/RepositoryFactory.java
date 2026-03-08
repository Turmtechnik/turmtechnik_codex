package tom.turmtechnik;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.util.Vector;

/**
 * Factory für Repository-Instanzen.
 * Laufzeit-Zugriffe auf Programme gehen ausschließlich über die Datenbank.
 * Excel-Dateien werden nur noch für Import/Discovery gelesen.
 */
public class RepositoryFactory {
    private static final String TAG = "RepositoryFactory";

    private static String getSdCardPath() {
        return Environment.getExternalStorageDirectory().getPath();
    }

    public static ProgrammRepository getProgrammRepository(String programmTyp) {
        Context context = TurmtechnikActivity.turmtechnikContext;
        if (context == null) {
            Log.w(TAG, "Kein App-Context vorhanden, verwende DB-Repository ohne expliziten Context");
        }
        return new DatabaseProgrammRepository(context, programmTyp);
    }

    public static ProgrammRepository getProgrammRepository(Context context, String programmTyp) {
        return new DatabaseProgrammRepository(context, programmTyp);
    }

    /**
     * Lädt die Programmtage-Dateinamen direkt aus dem Programmtage-Ordner.
     * Diese Methode bleibt für Import/Discovery erhalten.
     */
    private static Vector<String> loadFesttageFileNames(String programmTyp) {
        Vector<String> fileNames = new Vector<String>();
        String sdCardPath = getSdCardPath();
        String programmtagePath = sdCardPath + "/Turmtechnik/Programmtage/";
        File programmtageDir = new File(programmtagePath);

        if (!programmtageDir.exists() || !programmtageDir.isDirectory()) {
            Log.w(TAG, "Programmtage-Ordner nicht gefunden: " + programmtagePath);
            return fileNames;
        }

        try {
            File[] files = programmtageDir.listFiles();
            if (files == null) {
                return fileNames;
            }

            for (File file : files) {
                if (file.isFile() && file.getName().toLowerCase().endsWith(".xls")) {
                    String fileName = file.getName();
                    String fullPath = file.getAbsolutePath();
                    String fileNameLower = fileName.toLowerCase().trim();
                    if (fileNameLower.equals("stubenuhr.xls")
                            || fileNameLower.equals("nebenuhr.xls")
                            || fileNameLower.equals("nebenuhr a.xls")
                            || fileNameLower.equals("nebenuhr b.xls")
                            || fileNameLower.equals("nebenuhr c.xls")) {
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
