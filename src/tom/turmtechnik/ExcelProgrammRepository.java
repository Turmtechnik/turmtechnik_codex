package tom.turmtechnik;

import android.util.Log;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import jxl.read.biff.BiffException;

/**
 * Excel-Implementierung des ProgrammRepository.
 * Liest und schreibt Programme aus/in Excel-Dateien.
 */
public class ExcelProgrammRepository implements ProgrammRepository {
    private static final String TAG = "ExcelProgrammRepository";
    private ExcelRead excelRead;
    private ExcelReadWrite excelReadWrite;
    private String filePath;
    private String programmTyp;

    public ExcelProgrammRepository(String filePath, String programmTyp) {
        this.filePath = filePath;
        this.programmTyp = programmTyp;
    }

    private void ensureExcelRead() {
        if (excelRead == null) {
            excelRead = new ExcelRead();
            try {
                excelRead.openXls(filePath);
            } catch (BiffException e) {
                Log.e(TAG, "Fehler beim Öffnen der Excel-Datei (BiffException): " + filePath, e);
                throw new RuntimeException("Excel-Datei konnte nicht geöffnet werden", e);
            } catch (IOException e) {
                Log.e(TAG, "Fehler beim Öffnen der Excel-Datei (IOException): " + filePath, e);
                throw new RuntimeException("Excel-Datei konnte nicht geöffnet werden", e);
            }
        }
    }

    private void ensureExcelReadWrite() {
        if (excelReadWrite == null) {
            excelReadWrite = new ExcelReadWrite();
            try {
                excelReadWrite.openXlsReadWrite(filePath, 0);
            } catch (BiffException e) {
                Log.e(TAG, "Fehler beim Öffnen der Excel-Datei zum Schreiben (BiffException): " + filePath, e);
                throw new RuntimeException("Excel-Datei konnte nicht geöffnet werden", e);
            } catch (IOException e) {
                Log.e(TAG, "Fehler beim Öffnen der Excel-Datei zum Schreiben (IOException): " + filePath, e);
                throw new RuntimeException("Excel-Datei konnte nicht geöffnet werden", e);
            }
        }
    }

    @Override
    public List<Programm> getAllProgramme(String programmTyp, String tagtypName) {
        // tagtypName wird für ExcelProgrammRepository ignoriert (nur für MultiFileProgrammRepository relevant)
        ensureExcelRead();
        List<Programm> programme = new ArrayList<>();
        
        int zeilen = excelRead.getCellZeilen();
        // Beginne ab Zeile 4 (Index 3), da Zeile 0-1 Header sind und Zeile 2 (Index 2) die Überschrift ist
        for (int zeile = 3; zeile < zeilen; zeile++) {
            try {
                String startzeit = excelRead.getCellString(TagesSuche.SPALTE_A_STARTZEIT, zeile);
                if (startzeit == null || startzeit.trim().isEmpty()) {
                    break; // Ende der Tabelle
                }
                
                Programm programm = zeileToProgramm(zeile);
                if (programmTyp == null || programmTyp.equals(this.programmTyp)) {
                    programme.add(programm);
                }
            } catch (Exception e) {
                Log.e(TAG, "Fehler beim Lesen von Zeile " + zeile, e);
                // Weiter zur nächsten Zeile
            }
        }
        
        return programme;
    }

    @Override
    public Programm getProgrammById(int id) {
        ensureExcelRead();
        return zeileToProgramm(id);
    }

    /**
     * Prüft, ob Spalte D (Heizung) vorhanden ist.
     * Wenn Spalte D leer ist oder "x" enthält (was ein Wochentag wäre), dann fehlt Spalte D.
     */
    private boolean hasSpalteD(int zeile) {
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
    
    private Programm zeileToProgramm(int zeile) {
        Programm p = new Programm(zeile);
        
        try {
            p.setStartzeit(excelRead.getCellString(TagesSuche.SPALTE_A_STARTZEIT, zeile));
            p.setFunktion(excelRead.getCellString(TagesSuche.SPALTE_B_FUNKTION, zeile));
            p.setMelodieName(excelRead.getCellString(TagesSuche.SPALTE_C_MELODIE_NAME, zeile));
            p.setDauerHeizung(excelRead.getCellString(TagesSuche.SPALTE_D_DAUER_MINUTEN_HEIZUNG, zeile));
            
            // Prüfe, ob Spalte D vorhanden ist
            boolean hasD = hasSpalteD(zeile);
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
        ensureExcelReadWrite();
        
        try {
            int zeile = programm.getId();
            
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
            
            // ExcelReadWrite neu öffnen für weitere Operationen
            excelReadWrite = null;
            
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Fehler beim Speichern von Programm Zeile " + programm.getId(), e);
            return false;
        }
    }

    @Override
    public boolean deleteProgramm(int id) {
        // Excel unterstützt kein Löschen von Zeilen einfach
        // Stattdessen: Zeile leeren
        ensureExcelReadWrite();
        
        try {
            // Alle Spalten leeren
            for (int spalte = 0; spalte < 20; spalte++) {
                excelReadWrite.putCellStringKeepFormat(spalte, id, "");
            }
            
            excelReadWrite.saveAndCloseWorkbooks();
            excelReadWrite.copyTempWorkbook();
            excelReadWrite = null;
            
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Fehler beim Löschen von Programm Zeile " + id, e);
            return false;
        }
    }

    @Override
    public int getProgrammCount() {
        ensureExcelRead();
        int count = 0;
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
        
        return count;
    }

    public void close() {
        if (excelRead != null) {
            excelRead.closeWorkbook();
            excelRead = null;
        }
        if (excelReadWrite != null) {
            try {
                excelReadWrite.saveAndCloseWorkbooks();
                excelReadWrite.copyTempWorkbook();
            } catch (Exception e) {
                Log.e(TAG, "Fehler beim Schließen von ExcelReadWrite", e);
            }
            excelReadWrite = null;
        }
    }
}
