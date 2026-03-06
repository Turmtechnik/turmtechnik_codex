package tom.turmtechnik;

import android.util.Log;

import java.io.File;
import java.io.IOException;

import jxl.Cell;
import jxl.Sheet;
import jxl.Workbook;
import jxl.WorkbookSettings;
import jxl.format.CellFormat;
import jxl.read.biff.BiffException;
import jxl.write.Label;
import jxl.write.WritableCell;
import jxl.write.WritableCellFormat;
import jxl.write.WritableSheet;
import jxl.write.WritableWorkbook;
import jxl.write.WriteException;
import jxl.write.biff.RowsExceededException;

//import android.util.Log;

public class ExcelReadWriteTest
{

    private File jxlFilename;
    private WritableWorkbook workbookCopy;
    private Workbook workbookOriginal;
    //private WritableSheet sheet ;

    //private WritableSheet[] sheetX = new WritableSheet[3]   ;

    //private WritableSheet sheetMy2 ;

    //private WritableSheet blatt3;

    //private WritableSheet blattX ;

    private WorkbookSettings ws;

    private Cell readableCell;
    private WritableCell writableCell ;
    private Sheet readableSheet ;
    private WritableSheet writableSheet ;


    private File inp ;
    private File outp ;

    private String inOutFileName  ;


    public void openXlsWriteSheetCopy(String outputFileName, int sheet_number) throws BiffException, IOException
    {
        inOutFileName = outputFileName ;

        Log.e("inOutFilename" , "=" + inOutFileName) ;
        ws = new WorkbookSettings();
        ws.setEncoding("ISO-8859-1");
        //ws.setInitialFileSize(65536);
        ws.setInitialFileSize(32768);
        ws.setGCDisabled(false);
        //ws.setGCDisabled(true);
        //Log.i("file size", "=" + ws.getInitialFileSize()) ;

        //Log.i("workbook", "open");

        inp = new File(inOutFileName) ;
        outp = new File(inOutFileName + "Temp.xls") ;

        Log.e("inOutFilename" , "inp=" + inp) ;
        Log.e("inOutFilename" , "outp=" + outp) ;

        //workbookOriginal = Workbook.getWorkbook(new File(outputFileName), ws);
        workbookOriginal = Workbook.getWorkbook(inp) ;

        //workbookCopy = Workbook.createWorkbook(new File(outputFileName + "_copy.xls"), workbookOriginal) ;
        //workbookCopy = Workbook.createWorkbook(new File(outputFileName), workbookOriginal);

        workbookCopy = Workbook.createWorkbook(outp, workbookOriginal) ;


        //copySheet("Tabelle1" , 0);


        //sheet = workbook.createSheet(sheetName, sheet_number) ;
        //sheetX[0] = (WritableSheet) workbookOriginal.getSheet(0);
        //sheetX[0] = workbookCopy.importSheet(workbookOriginal.getSheet(0)) ;

        //Log.e("getNumberOfSheets", "=" + workbookCopy.getNumberOfSheets());

        //if (workbookCopy.getNumberOfSheets() > 2) {
        //    workbookCopy.removeSheet(2);
        //}

        //sheetX[1] = workbookCopy.createSheet("Verknüpft / Ausgang" , 2) ;

        //blatt3 = workbookCopy.createSheet("Verknüpft / Ausgang", 2);

        //blattX = workbookCopy.getSheet(sheet_number) ;

        //sheetMy2 = workbookCopy.createSheet("Verknüpft / Ausgang" , 2) ;

        //sheetX[1] = workbookCopy.getSheet(0) ;
    }

    //public void importSheet0(String sheetName, int sheetNumber, Sheet sheetToPut)
    //{
    //    sheetX[0] = workbook.importSheet(sheetName, sheetNumber, sheetToPut ) ;
    //}

    public void writeWorkbook() throws IOException
    {

        workbookCopy.write();
    }

    public void closeWorkbooks() throws WriteException, IOException
    {
        if (workbookOriginal != null)
        {
            workbookOriginal.close();
        }

        if (workbookCopy != null) {
            workbookCopy.close();
            //workbook = null ;
            //sheet = null ;
            // Log.i("max memory" , "=" + Runtime.getRuntime().maxMemory());

        }

    }


    public void putCellStringCopy(int spalte, int zeile, String cellString) throws RowsExceededException, WriteException {
        {
            Label label = new Label(spalte, zeile, cellString);

            writableSheet.addCell(label);
        }
    }

    public String getCellStringOriginal(int spalte, int zeile)
    {
        if (readableCell == null) {
        return "";
    }
        readableCell = readableSheet.getCell(spalte, zeile);
        return (readableCell.getContents());
    }
    public int getCellSpaltenOriginal() {
        int spalten = readableSheet.getColumns();
        return (spalten);
    }

    public int getCellZeilenOriganal() {
        int zeilen = readableSheet.getRows();
        return (zeilen);
    }

    public void saveAndCloseWorkbook() throws IOException, WriteException
    {
        workbookOriginal.close();

        workbookCopy.write();
        workbookCopy.close();
    }



}
