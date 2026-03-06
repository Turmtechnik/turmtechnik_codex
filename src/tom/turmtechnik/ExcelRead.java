package tom.turmtechnik;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;

import jxl.Cell;
import jxl.Sheet;
import jxl.Workbook;
import jxl.WorkbookSettings;
import jxl.read.biff.BiffException;

//import android.util.Log;

public class ExcelRead {
    
    /**
     * PrintStream, das alle Ausgaben ignoriert (für Unterdrückung von JXL-Warnungen).
     */
    private static class NullPrintStream extends PrintStream {
        public NullPrintStream() {
            super(new java.io.ByteArrayOutputStream());
        }
    }

    private File inputWorkbook;
    private Workbook workbook;
    private Sheet sheet;
    private WorkbookSettings ws;
    private Cell cell;

    private final int INIT_FILE_SIZE = 32768;
    //private final int INIT_FILE_SIZE = 32768  /4 ;

    public Boolean checkFile(String filename) {
        inputWorkbook = new File(filename);
        if (inputWorkbook.exists()) {
            return true;
        }
        return false;
    }


    //public void openXls (String filename) // 7.6.13 geaendert
    public void openXlsSheet(String filename, int sheet_number) throws BiffException, IOException
    {
        filename = checkXls(filename);

        //Log.i("workbook", "open");
        inputWorkbook = new File(filename);
        //Log.i("filename" , filename) ;

        ws = new WorkbookSettings();
        ws.setEncoding("ISO-8859-1");
        //ws.setInitialFileSize(65536);
        //ws.setInitialFileSize(32768);
        ws.setInitialFileSize(INIT_FILE_SIZE);
        ws.setGCDisabled(false);
        //ws.setGCDisabled(true);
        //Log.i("file size", "=" + ws.getInitialFileSize()) ;

        System.gc();
        
        // Unterdrücke JXL-Warnungen (z.B. "Cannot read name", "Cannot read name ranges")
        PrintStream originalErr = System.err;
        try {
            System.setErr(new NullPrintStream());
            workbook = Workbook.getWorkbook(inputWorkbook, ws);
        } finally {
            System.setErr(originalErr);
        }
        ws = null;
        
        // Prüfe, ob das Sheet existiert (Workbook kann 0 Sheets haben → kein getSheet aufrufen)
        int numberOfSheets = workbook.getNumberOfSheets();
        if (numberOfSheets == 0) {
            android.util.Log.w("ExcelRead", "Workbook hat keine Sheets: " + filename);
            sheet = null;
        } else {
            try {
                if (sheet_number >= numberOfSheets) {
                    android.util.Log.w("ExcelRead", "Sheet " + sheet_number + " existiert nicht in " + filename + " (nur " + numberOfSheets + " Sheets vorhanden). Verwende Sheet 0.");
                    sheet = workbook.getSheet(0);
                } else {
                    sheet = workbook.getSheet(sheet_number);
                }
            } catch (IndexOutOfBoundsException e) {
                android.util.Log.w("ExcelRead", "Workbook/Sheet-Zugriff fehlgeschlagen (Index: " + sheet_number + ", Size: 0?): " + filename + " – " + e.getMessage());
                sheet = null;
            }
        }

        //Log.i("spalten" , "" + getCellSpalten());
        //Log.i("zeilen" , "" + getCellZeilen());

        inputWorkbook = null;
        ws = null;

        //StaticVariable.excelOpen ++ ;
        //Log.e("excelOpen" , "= " + StaticVariable.excelOpen) ;
        //Log.e("excelClose" , "= " + StaticVariable.excelClose) ;
        //Log.e("freeMemory" , "kb =" + (Runtime.getRuntime().freeMemory()) / 1024) ;

        System.gc();
    }

    public Sheet getSheet0() {
        if (workbook == null || workbook.getNumberOfSheets() == 0) return null;
        try {
            return workbook.getSheet(0);
        } catch (IndexOutOfBoundsException e) {
            android.util.Log.w("ExcelRead", "getSheet(0) fehlgeschlagen (Workbook evtl. ohne Sheets): " + e.getMessage());
            return null;
        }
    }


    public Workbook getWorkbook() {

        return workbook;
    }


    public void openXls(String filename) throws BiffException, IOException // 7.6.13 geaendert
    {
        openXlsSheet(filename, 0);  // standart sheet 0
    }


    public void openXls_save(String filename) throws BiffException, IOException
    // 7.6.13 geaendert
//	public void openXls_save (String filename, int sheet_number) throws BiffException, IOException
    {

        //Log.i("workbook", "open");
        inputWorkbook = new File(filename);

        ws = new WorkbookSettings();
        ws.setEncoding("ISO-8859-1");
        //ws.setInitialFileSize(65536); // 18.9.2013  probleme mit out of memory error
        //ws.setInitialFileSize(32768); // 4.1.2014 -- test weil speicher ueberlaueft
        ws.setInitialFileSize(INIT_FILE_SIZE);
        ws.setGCDisabled(false);
        //ws.setGCDisabled(true);
        //Log.i("file size", "=" + ws.getInitialFileSize()) ;

        // Unterdrücke JXL-Warnungen (z.B. "Cannot read name", "Cannot read name ranges")
        PrintStream originalErr = System.err;
        try {
            System.setErr(new NullPrintStream());
            workbook = Workbook.getWorkbook(inputWorkbook, ws);
        } finally {
            System.setErr(originalErr);
        }
        ws = null;

        // Nur Sheet öffnen, wenn das Workbook mindestens ein Sheet hat (sonst IndexOutOfBoundsException)
        if (workbook.getNumberOfSheets() > 0) {
            try {
                sheet = workbook.getSheet(0);  //7.6.13 geaendert
            } catch (IndexOutOfBoundsException e) {
                android.util.Log.w("ExcelRead", "getSheet(0) fehlgeschlagen in openXls_save: " + e.getMessage());
                sheet = null;
            }
        } else {
            sheet = null;
        }

        //Log.i("spalten" , "" + getCellSpalten());
        //Log.i("zeilen" , "" + getCellZeilen());

        inputWorkbook = null;
        ws = null;

        System.gc();
    }

    public void closeWorkbook() {
        if (workbook != null) {
            workbook.close();
            workbook = null;
            sheet = null;
            // Log.i("max memory" , "=" + Runtime.getRuntime().maxMemory());

        }

        System.gc();
        //Log.i("workbook" , "close");

        //SaticVariable.excelClose ++ ;
        //Log.e("excelOpen" , "= " + StaticVariable.excelOpen) ;
        //Log.e("excelClose" , "= " + StaticVariable.excelClose) ;
        //Log.e("freeMemory" , "kb =" + (Runtime.getRuntime().freeMemory()) / 1024) ;
    }

    public String getCellString(int spalte, int zeile) throws ArrayIndexOutOfBoundsException, Exception {
        if (sheet == null) {
            return "";
        }
        if (spalte < 0 || zeile < 0 || spalte >= sheet.getColumns() || zeile >= sheet.getRows()) {
            return "";
        }
        try {
            cell = sheet.getCell(spalte, zeile);
            return (cell != null ? cell.getContents() : "");
        } catch (IndexOutOfBoundsException e) {
            return "";
        }
    }

    public Cell getCell(int spalte, int zeile) {
        if (sheet == null) return null;
        if (spalte < 0 || zeile < 0 || spalte >= sheet.getColumns() || zeile >= sheet.getRows()) return null;
        try {
            return sheet.getCell(spalte, zeile);
        } catch (IndexOutOfBoundsException e) {
            return null;
        }
    }

    public int getCellSpalten() {
        if (sheet == null) {
            return 0;
        }
        int spalten = sheet.getColumns();
        return (spalten);
    }

    public int getCellZeilen() {
        if (sheet == null) {
            return 0;
        }
        int zeilen = sheet.getRows();
        return (zeilen);
    }

    private String checkXls(String inputFilename) {
        String outputFilename;

        if (inputFilename.endsWith(".xls")) {
            outputFilename = inputFilename;
        } else {
            outputFilename = inputFilename + ".xls";
        }
        return outputFilename;
    }


	/*
    private void getMemoryInfo()
	{
		ActivityManager activityManager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
		MemoryInfo mi = new MemoryInfo();
		activityManager.getMemoryInfo(mi);
		Log.e("Verfuegbarer" , "Speicher = " + mi.availMem) ;
	}
	*/
}
