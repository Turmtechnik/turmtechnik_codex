package tom.turmtechnik;

import android.util.Log;

import java.io.File;
import java.io.IOException;

import jxl.Cell;
import jxl.Sheet;
import jxl.Workbook;
import jxl.WorkbookSettings;
import jxl.read.biff.BiffException;
import jxl.write.Label;
import jxl.write.WritableCell;
import jxl.write.WritableSheet;
import jxl.write.WritableWorkbook;
import jxl.write.WriteException;
import jxl.write.biff.RowsExceededException;

//import android.util.Log;

public class ExcelWrite {

    private File inputWorkbook;
    private File outputWorkbook;
    private WritableWorkbook workbook;
    private WritableSheet sheet;

    //private WritableSheet[] sheetX = new WritableSheet[2]  ;


    private WorkbookSettings ws;
    private Cell cell;

    //public void openXls (String filename) // 7.6.13 geaendert
    public void openXlsWriteSheet(String outputFileName, int sheet_number, String sheetName) throws BiffException, IOException {
        ws = new WorkbookSettings();
        ws.setEncoding("ISO-8859-1");
        //ws.setInitialFileSize(65536);
        ws.setInitialFileSize(32768);
        ws.setGCDisabled(false);
        //ws.setGCDisabled(true);
        //Log.i("file size", "=" + ws.getInitialFileSize()) ;

        //Log.i("workbook", "open");
        workbook = Workbook.createWorkbook(new File(outputFileName), ws);

        sheet = workbook.createSheet(sheetName, sheet_number);
        //sheetX[0] = workbook.createSheet("Tabelle1" , 0) ;
        //sheetX[1] = workbook.createSheet("Tabelle2" , 1) ;


    }



    public void openXlsWriteSheetImport(String outputFileName, int sheet_number, String sheetName) throws BiffException, IOException {
        ws = new WorkbookSettings();
        ws.setEncoding("ISO-8859-1");
        //ws.setInitialFileSize(65536);
        ws.setInitialFileSize(32768);
        ws.setGCDisabled(false);


        workbook = Workbook.createWorkbook(new File(outputFileName), ws);

        sheet = workbook.createSheet(sheetName, sheet_number);

    }


    public WritableWorkbook getWritableWorkbook()
    {
        return workbook ;
    }

    public void setSheet(WritableSheet writableSheet)
    {

        //workbook.importSheet("Tabelle1" , 0, writableSheet) ;


        //sheet = workbook.getSheet(0)  ;

        sheet = writableSheet ;

        Log.e("excelWrite" , "spalten=" + sheet.getColumns()) ;
        Log.e("excelWrite" , "zeilen=" + sheet.getRows()) ;

        //workbook.importSheet("Tabelle1" , 0 , sheet) ;
    }

    //public void importSheet0(String sheetName, int sheetNumber, Sheet sheetToPut)
    //{
    //    sheetX[0] = workbook.importSheet(sheetName, sheetNumber, sheetToPut ) ;
    //}


    public void writeWorkbook() throws IOException {
        workbook.write();
    }


    public void closeWorkbook() throws WriteException, IOException {
        if (workbook != null) {
            workbook.close();
            workbook = null;
            //sheet = null ;
            // Log.i("max memory" , "=" + Runtime.getRuntime().maxMemory());

        }
    }

    public String getCellString(int spalte, int zeile) throws ArrayIndexOutOfBoundsException, Exception {
        {
            if (sheet == null) {
                return "";
            }
            cell = sheet.getCell(spalte, zeile);
            return (cell.getContents());

        }
    }

    public Cell getCell(int spalte, int zeile)
    {
        return sheet.getCell(spalte,zeile) ;
    }

    public void putCellString(int spalte, int zeile, String cellString) throws RowsExceededException, WriteException {
        {
            Label label = new Label(spalte, zeile, cellString);
            ((WritableSheet) sheet).addCell(label);
        }
    }

    //public void putCell(WritableCell cell) throws WriteException
    //{

    //        sheet.addCell(cell);

    //}
    public void putCell(Label label) throws WriteException
    {
        ((WritableSheet) sheet).addCell(label);
    }

    //public void putCell(int spalte, int zeile, Cell cell) throws WriteException
    //{
    //    Object readCell = cell ;
    //    WritableCell newCell = readCell.copyTo(spalte,zeile);
    //    CellFormat readFormat = readCell.getCellFormat();
    //    WritableCellFormat newFormat = new WritableCellFormat(readFormat);
    //    newCell.setCellFormat(newFormat);
    //    ((WritableSheet) sheet).addCell(newCell);

    //}

    public int getCellSpalten() {
        int spalten = sheet.getColumns();
        return (spalten);
    }

    public int getCellZeilen() {
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

}
