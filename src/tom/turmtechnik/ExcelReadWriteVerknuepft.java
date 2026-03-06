package tom.turmtechnik;

import android.util.Log;

import java.io.File;
import java.io.IOException;

import jxl.Cell;
import jxl.Workbook;
import jxl.WorkbookSettings;
import jxl.read.biff.BiffException;
import jxl.write.Label;
import jxl.write.WritableSheet;
import jxl.write.WritableWorkbook;
import jxl.write.WriteException;
import jxl.write.biff.RowsExceededException;

//import android.util.Log;

public class ExcelReadWriteVerknuepft
{

    private File jxlFilename;
    private WritableWorkbook workbookCopy;
    private Workbook workbookOriginal;
    //private WritableSheet sheet ;

    //private WritableSheet[] sheetX = new WritableSheet[3]   ;

    //private WritableSheet sheetMy2 ;

    private WritableSheet blatt3;

    private WorkbookSettings ws;
    private Cell cell;

    //public void openXls (String filename) // 7.6.13 geaendert
    public void openXlsWriteSheetCopy(String outputFileName, int sheet_number, String sheetName) throws BiffException, IOException {
        ws = new WorkbookSettings();
        ws.setEncoding("ISO-8859-1");
        //ws.setInitialFileSize(65536);
        ws.setInitialFileSize(32768);
        ws.setGCDisabled(false);
        //ws.setGCDisabled(true);
        //Log.i("file size", "=" + ws.getInitialFileSize()) ;

        //Log.i("workbook", "open");

        workbookOriginal = Workbook.getWorkbook(new File(outputFileName), ws);

        //workbookCopy = Workbook.createWorkbook(new File(outputFileName + "_copy.xls"), workbookOriginal) ;
        workbookCopy = Workbook.createWorkbook(new File(outputFileName), workbookOriginal);

        //sheet = workbook.createSheet(sheetName, sheet_number) ;
        //sheetX[0] = (WritableSheet) workbookOriginal.getSheet(0);
        //sheetX[0] = workbookCopy.importSheet(workbookOriginal.getSheet(0)) ;

        Log.e("getNumberOfSheets", "=" + workbookCopy.getNumberOfSheets());

        if (workbookCopy.getNumberOfSheets() > 2) {
            workbookCopy.removeSheet(2);
        }

        //sheetX[1] = workbookCopy.createSheet("Verknüpft / Ausgang" , 2) ;

        blatt3 = workbookCopy.createSheet("Verknüpft / Ausgang", 2);

        //sheetMy2 = workbookCopy.createSheet("Verknüpft / Ausgang" , 2) ;

        //sheetX[1] = workbookCopy.getSheet(0) ;
    }

    //public void importSheet0(String sheetName, int sheetNumber, Sheet sheetToPut)
    //{
    //    sheetX[0] = workbook.importSheet(sheetName, sheetNumber, sheetToPut ) ;
    //}

    public void writeWorkbook() throws IOException {
        workbookCopy.write();
    }

    public void closeWorkbook() throws WriteException, IOException {
        if (workbookCopy != null) {
            workbookCopy.close();
            //workbook = null ;
            //sheet = null ;
            // Log.i("max memory" , "=" + Runtime.getRuntime().maxMemory());

        }
        if (workbookOriginal != null) {
            workbookOriginal.close();
        }
    }


    public void putCellStringBlatt3(int spalte, int zeile, String cellString) throws RowsExceededException, WriteException {
        {
            Label label = new Label(spalte, zeile, cellString);

            blatt3.addCell(label);
        }
    }

    public int getCellSpaltenBlatt3(int sheetIndex) {
        int spalten = blatt3.getColumns();
        return (spalten);
    }

    public int getCellZeilen(int sheetIndex) {
        int zeilen = blatt3.getRows();
        return (zeilen);
    }


}
