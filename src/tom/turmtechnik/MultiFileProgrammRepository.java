package tom.turmtechnik;

import android.util.Log;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import jxl.read.biff.BiffException;

/**
 * Repository, das mehrere Excel-Dateien zusammenfasst.
 * Wird für festtag_fest und festtag_variabel verwendet, da diese mehrere Programmtage-Dateien im Ordner "Programmtage" enthalten.
 */
public class MultiFileProgrammRepository implements ProgrammRepository {
    private static final String TAG = "MultiFileProgrammRepository";
    private Vector<String> filePaths;
    private String programmTyp;

    public MultiFileProgrammRepository(Vector<String> filePaths, String programmTyp) {
        this.filePaths = filePaths != null ? filePaths : new Vector<String>();
        this.programmTyp = programmTyp;
    }

    @Override
    public List<Programm> getAllProgramme(String programmTyp, String tagtypName) {
        List<Programm> allProgramme = new ArrayList<>();
        
        if (filePaths == null || filePaths.isEmpty()) {
            Log.w(TAG, "Keine Dateien für Programmtag " + this.programmTyp + " gefunden");
            return allProgramme;
        }
        
        // Durch alle Programmtage-Dateien iterieren
        for (int fileIndex = 0; fileIndex < filePaths.size(); fileIndex++) {
            String filePath = filePaths.elementAt(fileIndex);
            
            // Wenn Tagtyp angegeben ist, nur diese Datei verarbeiten
            if (tagtypName != null && !tagtypName.trim().isEmpty()) {
                // Extrahiere Dateinamen aus dem Pfad
                String fileName = filePath.substring(filePath.lastIndexOf("/") + 1);
                if (!fileName.equals(tagtypName) && !fileName.equals(tagtypName.trim())) {
                    // Dateiname stimmt nicht überein, überspringe diese Datei
                    continue;
                }
            }
            
            try {
                ExcelRead excelRead = new ExcelRead();
                excelRead.openXls(filePath);
                
                int zeilen = excelRead.getCellZeilen();
                // Beginne ab Zeile 4 (Index 3), da Zeile 0-1 Header sind und Zeile 2 (Index 2) die Überschrift ist
                for (int zeile = 3; zeile < zeilen; zeile++) {
                    try {
                        String startzeit = excelRead.getCellString(TagesSuche.SPALTE_A_STARTZEIT, zeile);
                        if (startzeit == null || startzeit.trim().isEmpty()) {
                            break; // Ende der Tabelle
                        }
                        
                        Programm programm = zeileToProgramm(excelRead, zeile, fileIndex);
                        if (programmTyp == null || programmTyp.equals(this.programmTyp)) {
                            allProgramme.add(programm);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Fehler beim Lesen von Zeile " + zeile + " in Datei " + filePath, e);
                        // Weiter zur nächsten Zeile
                    }
                }
                
                excelRead.closeWorkbook();
            } catch (BiffException e) {
                Log.e(TAG, "Fehler beim Öffnen der Excel-Datei (BiffException): " + filePath, e);
            } catch (IOException e) {
                Log.e(TAG, "Fehler beim Öffnen der Excel-Datei (IOException): " + filePath, e);
            } catch (Exception e) {
                Log.e(TAG, "Fehler beim Lesen der Excel-Datei: " + filePath, e);
            }
        }
        
        Log.i(TAG, "Gefunden: " + allProgramme.size() + " Programme aus " + filePaths.size() + " Dateien" + 
              (tagtypName != null ? " (gefiltert nach: " + tagtypName + ")" : ""));
        return allProgramme;
    }

    @Override
    public Programm getProgrammById(int id) {
        // ID enthält sowohl Datei-Index als auch Zeilen-Index
        // Format: fileIndex * 10000 + zeileIndex
        int fileIndex = id / 10000;
        int zeileIndex = id % 10000;
        
        if (fileIndex < 0 || fileIndex >= filePaths.size()) {
            Log.e(TAG, "Ungültiger Datei-Index: " + fileIndex);
            return null;
        }
        
        String filePath = filePaths.elementAt(fileIndex);
        try {
            ExcelRead excelRead = new ExcelRead();
            excelRead.openXls(filePath);
            Programm programm = zeileToProgramm(excelRead, zeileIndex, fileIndex);
            excelRead.closeWorkbook();
            return programm;
        } catch (Exception e) {
            Log.e(TAG, "Fehler beim Lesen von Programm ID " + id + " aus Datei " + filePath, e);
            return null;
        }
    }

    /**
     * Prüft, ob Spalte D (Heizung) vorhanden ist.
     * Wenn Spalte D leer ist oder "x" enthält (was ein Wochentag wäre), dann fehlt Spalte D.
     */
    private boolean hasSpalteD(ExcelRead excelRead, int zeile) {
        try {
            String spalteD = excelRead.getCellString(TagesSuche.SPALTE_D_DAUER_MINUTEN_HEIZUNG, zeile);
            if (spalteD == null || spalteD.trim().isEmpty() || "x".equals(spalteD.trim())) {
                return false; // Spalte D fehlt
            }
            return true; // Spalte D ist vorhanden
        } catch (Exception e) {
            // Bei Fehler annehmen, dass Spalte D fehlt
            return false;
        }
    }
    
    private Programm zeileToProgramm(ExcelRead excelRead, int zeile, int fileIndex) {
        // ID = fileIndex * 10000 + zeile (um Datei und Zeile zu kombinieren)
        Programm p = new Programm(fileIndex * 10000 + zeile);
        
        try {
            p.setStartzeit(excelRead.getCellString(TagesSuche.SPALTE_A_STARTZEIT, zeile));
            p.setFunktion(excelRead.getCellString(TagesSuche.SPALTE_B_FUNKTION, zeile));
            p.setMelodieName(excelRead.getCellString(TagesSuche.SPALTE_C_MELODIE_NAME, zeile));
            p.setDauerHeizung(excelRead.getCellString(TagesSuche.SPALTE_D_DAUER_MINUTEN_HEIZUNG, zeile));
            
            // Prüfe, ob Spalte D vorhanden ist
            boolean hasD = hasSpalteD(excelRead, zeile);
            int wochentagStart = hasD ? TagesSuche.SPALTE_E_WOCHENTAG_MONTAG : TagesSuche.SPALTE_D_DAUER_MINUTEN_HEIZUNG;
            
            // Wochentage (beginnen bei Index 4 wenn Spalte D vorhanden, sonst bei Index 3)
            p.setMontag("x".equals(excelRead.getCellString(wochentagStart + 0, zeile)));
            p.setDienstag("x".equals(excelRead.getCellString(wochentagStart + 1, zeile)));
            p.setMittwoch("x".equals(excelRead.getCellString(wochentagStart + 2, zeile)));
            p.setDonnerstag("x".equals(excelRead.getCellString(wochentagStart + 3, zeile)));
            p.setFreitag("x".equals(excelRead.getCellString(wochentagStart + 4, zeile)));
            p.setSamstag("x".equals(excelRead.getCellString(wochentagStart + 5, zeile)));
            p.setSonntag("x".equals(excelRead.getCellString(wochentagStart + 6, zeile)));
            
            // Immer (Spalte L, Index 11)
            String immerStr = excelRead.getCellString(TagesSuche.SPALTE_L_IMMER, zeile);
            p.setImmer("1".equals(immerStr));
            
            // Periodisch (Spalte M, Index 12)
            String periodischStr = excelRead.getCellString(TagesSuche.SPALTE_M_PERIODISCH, zeile);
            if ("1".equals(periodischStr)) {
                p.setPeriodisch(1); // Sommer
            } else if ("2".equals(periodischStr)) {
                p.setPeriodisch(2); // Winter
            } else {
                p.setPeriodisch(0); // Immer
            }
            
            // Start/Ende-Datum (Spalte N-O, Index 13-14)
            p.setStartDatum(excelRead.getCellString(TagesSuche.SPALTE_N_START, zeile));
            p.setEndeDatum(excelRead.getCellString(TagesSuche.SPALTE_O_ENDE, zeile));
            
            // Verknüpfte Taste (Spalte P, Index 15)
            String verknuepfteTaste = excelRead.getCellString(TagesSuche.SPALTE_P_VERKNUEPFTE_TASTE, zeile);
            if (verknuepfteTaste != null && !verknuepfteTaste.trim().isEmpty()) {
                p.setVerknuepfteTaste(verknuepfteTaste.trim());
            }
            
            // Priorität (Spalte Q, Index 16)
            try {
                String prioritaetStr = excelRead.getCellString(TagesSuche.SPALTE_Q_PRIORITAET, zeile);
                if (prioritaetStr != null && !prioritaetStr.trim().isEmpty()) {
                    p.setPrioritaet(Integer.parseInt(prioritaetStr.trim()));
                } else {
                    p.setPrioritaet(0);
                }
            } catch (NumberFormatException e) {
                p.setPrioritaet(0);
            }
            
            p.setProgrammTyp(this.programmTyp);
            
        } catch (Exception e) {
            Log.e(TAG, "Fehler beim Konvertieren von Zeile " + zeile + " zu Programm", e);
        }
        
        return p;
    }

    @Override
    public boolean saveProgramm(Programm programm) {
        // Für MultiFileProgrammRepository müssen wir die richtige Datei finden
        // Die ID enthält: fileIndex * 10000 + zeileIndex
        int fileIndex = programm.getId() / 10000;
        int zeileIndex = programm.getId() % 10000;
        
        if (fileIndex < 0 || fileIndex >= filePaths.size()) {
            Log.e(TAG, "Ungültiger Datei-Index: " + fileIndex + " für Programm ID " + programm.getId());
            return false;
        }
        
        String filePath = filePaths.elementAt(fileIndex);
        
        try {
            ExcelReadWrite excelReadWrite = new ExcelReadWrite();
            excelReadWrite.openXlsReadWrite(filePath, 0);
            
            int zeile = zeileIndex;
            
            // Startzeit
            excelReadWrite.putCellStringKeepFormat(TagesSuche.SPALTE_A_STARTZEIT, zeile, programm.getStartzeit());
            // Funktion
            excelReadWrite.putCellStringKeepFormat(TagesSuche.SPALTE_B_FUNKTION, zeile, programm.getFunktion());
            // Melodienname
            excelReadWrite.putCellStringKeepFormat(TagesSuche.SPALTE_C_MELODIE_NAME, zeile, programm.getMelodieName());
            // Dauer Heizung
            excelReadWrite.putCellStringKeepFormat(TagesSuche.SPALTE_D_DAUER_MINUTEN_HEIZUNG, zeile, programm.getDauerHeizung());
            
            // Wochentage
            excelReadWrite.putCellStringKeepFormat(TagesSuche.SPALTE_E_WOCHENTAG_MONTAG + 0, zeile, programm.isMontag() ? "x" : "");
            excelReadWrite.putCellStringKeepFormat(TagesSuche.SPALTE_E_WOCHENTAG_MONTAG + 1, zeile, programm.isDienstag() ? "x" : "");
            excelReadWrite.putCellStringKeepFormat(TagesSuche.SPALTE_E_WOCHENTAG_MONTAG + 2, zeile, programm.isMittwoch() ? "x" : "");
            excelReadWrite.putCellStringKeepFormat(TagesSuche.SPALTE_E_WOCHENTAG_MONTAG + 3, zeile, programm.isDonnerstag() ? "x" : "");
            excelReadWrite.putCellStringKeepFormat(TagesSuche.SPALTE_E_WOCHENTAG_MONTAG + 4, zeile, programm.isFreitag() ? "x" : "");
            excelReadWrite.putCellStringKeepFormat(TagesSuche.SPALTE_E_WOCHENTAG_MONTAG + 5, zeile, programm.isSamstag() ? "x" : "");
            excelReadWrite.putCellStringKeepFormat(TagesSuche.SPALTE_E_WOCHENTAG_MONTAG + 6, zeile, programm.isSonntag() ? "x" : "");
            
            // Immer
            excelReadWrite.putCellStringKeepFormat(TagesSuche.SPALTE_L_IMMER, zeile, programm.isImmer() ? "1" : "0");
            
            // Periodisch
            String periodischStr = "";
            if (programm.getPeriodisch() == 1) {
                periodischStr = "1"; // Sommer
            } else if (programm.getPeriodisch() == 2) {
                periodischStr = "2"; // Winter
            }
            excelReadWrite.putCellStringKeepFormat(TagesSuche.SPALTE_M_PERIODISCH, zeile, periodischStr);
            
            // Start/Ende-Datum
            excelReadWrite.putCellStringKeepFormat(TagesSuche.SPALTE_N_START, zeile, programm.getStartDatum());
            excelReadWrite.putCellStringKeepFormat(TagesSuche.SPALTE_O_ENDE, zeile, programm.getEndeDatum());
            
            // Verknüpfte Taste
            excelReadWrite.putCellStringKeepFormat(TagesSuche.SPALTE_P_VERKNUEPFTE_TASTE, zeile, programm.getVerknuepfteTaste());
            
            // Priorität
            excelReadWrite.putCellStringKeepFormat(TagesSuche.SPALTE_Q_PRIORITAET, zeile, String.valueOf(programm.getPrioritaet()));
            
            // Speichern
            excelReadWrite.saveAndCloseWorkbooks();
            excelReadWrite.copyTempWorkbook();
            
            Log.i(TAG, "Programm gespeichert in Datei: " + filePath + " Zeile: " + zeile);
            return true;
            
        } catch (Exception e) {
            Log.e(TAG, "Fehler beim Speichern von Programm ID " + programm.getId() + " in Datei " + filePath, e);
            return false;
        }
    }

    @Override
    public boolean deleteProgramm(int id) {
        // TODO: Implementierung für Multi-File-Löschung
        Log.w(TAG, "deleteProgramm() für MultiFileProgrammRepository noch nicht implementiert");
        return false;
    }

    @Override
    public int getProgrammCount() {
        int count = 0;
        
        if (filePaths == null || filePaths.isEmpty()) {
            return 0;
        }
        
        for (String filePath : filePaths) {
            try {
                ExcelRead excelRead = new ExcelRead();
                excelRead.openXls(filePath);
                
                int zeilen = excelRead.getCellZeilen();
                // Beginne ab Zeile 4 (Index 3), da Zeile 0-1 Header sind und Zeile 2 (Index 2) die Überschrift ist
                for (int zeile = 3; zeile < zeilen; zeile++) {
                    try {
                        String startzeit = excelRead.getCellString(TagesSuche.SPALTE_A_STARTZEIT, zeile);
                        if (startzeit == null || startzeit.trim().isEmpty()) {
                            break;
                        }
                        count++;
                    } catch (Exception e) {
                        break;
                    }
                }
                
                excelRead.closeWorkbook();
            } catch (Exception e) {
                Log.e(TAG, "Fehler beim Zählen in Datei " + filePath, e);
            }
        }
        
        return count;
    }
}
