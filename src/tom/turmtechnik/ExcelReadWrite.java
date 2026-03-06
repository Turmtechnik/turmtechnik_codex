package tom.turmtechnik;

import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import jxl.Cell;
import jxl.CellFeatures;
import jxl.Sheet;
import jxl.Workbook;
import jxl.WorkbookSettings;
import jxl.format.CellFormat;
import jxl.read.biff.BiffException;
import jxl.write.Label;
import jxl.write.WritableCell;
import jxl.write.WritableCellFeatures;
import jxl.write.WritableCellFormat;
import jxl.write.WritableSheet;
import jxl.write.WritableWorkbook;
import jxl.write.WriteException;
import jxl.write.biff.RowsExceededException;

//import android.util.Log;

public class ExcelReadWrite
{

    private ExcelWrite excelWrite ;
    private ExcelRead excelRead ;

    private String inputFileName ;
    private String outputFileName ;


    public void openXlsReadWrite (String inFileName, int sheet_number) throws BiffException, IOException
    {
        if (inFileName == null || inFileName.isEmpty()) {
            throw new IllegalArgumentException("inFileName ist null oder leer");
        }
        
        inputFileName = inFileName;
        outputFileName = inFileName + "_temp.xls";

        excelRead = new ExcelRead() ;
        excelWrite = new ExcelWrite() ;

        if (excelRead == null) {
            throw new NullPointerException("excelRead ist null nach Initialisierung");
        }
        if (excelWrite == null) {
            throw new NullPointerException("excelWrite ist null nach Initialisierung");
        }

        excelRead.openXlsSheet(inputFileName, sheet_number);
        excelWrite.openXlsWriteSheetImport(outputFileName, sheet_number, "Tabelle1");

        //copySheetStrings(sheet_number) ;
        copySheet(sheet_number);

        //try
        //{
        //    WritableSheet sheetTemp = createSheetCopy(excelRead.getWorkbook(), 0 , 0 , excelWrite.getWritableWorkbook()) ;
        //    Log.e("sheet=" , "=" + sheetTemp) ;

        //   excelWrite.setSheet(sheetTemp) ;

        //} catch (WriteException e)
        //{
        //    e.printStackTrace();
       // }
    }


    private void copySheetStrings(int sheetNumber)
    {
        int spalten = excelRead.getCellSpalten() ;
        int zeilen = excelRead.getCellZeilen() ;
        String stringTemp = "";


        for (int j = 0 ; j < zeilen; j++)
        {
            for (int i = 0 ; i < spalten; i++)
            {
                try
                {
                    stringTemp = excelRead.getCellString(i, j) ;
                } catch (Exception e)
                {
                    Log.e("copySheet" , "read Error" ) ;

                }

                try
                {
                    excelWrite.putCellString(i,j, stringTemp);
                }
                catch (WriteException e)
                {
                    Log.e("copySheet" , "write Error" ) ;
                }
            }
        }
    }

    private void copySheet(int sheetNumber)
    {
        int spalten = excelRead.getCellSpalten() ;
        int zeilen = excelRead.getCellZeilen() ;
        String stringTemp = "";
        Label label = null;

        Map<CellFormat, WritableCellFormat> definedFormats = new HashMap<CellFormat, WritableCellFormat>();

        for (int j = 0 ; j < zeilen; j++)
        {
            for (int i = 0 ; i < spalten; i++)
            {
                try
                {
                    //stringTemp = excelRead.getCellString(i, j) ;
                    Cell readCell = null;
                    String labelString = "";
                    
                    // Prüfe, ob die Zelle in der Quelldatei existiert
                    if (i < excelRead.getCellSpalten()) {
                        readCell = excelRead.getCell(i, j);
                        labelString = (readCell != null) ? readCell.getContents() : "";
                    } else {
                        // Spalte existiert nicht in der Quelldatei - leere Zelle
                        labelString = "";
                    }
                    
                    if(labelString.startsWith(":"))
                    {
                        labelString = "00" + labelString ;
                    }
                    //label = new Label(i, j, readCell.getContents());
                    label = new Label(i, j, labelString);
                    //Log.e("labelString=" , "=" + labelString) ;

                    if (readCell != null) {
                        try {
                            CellFormat readFormat = readCell.getCellFormat();
                            if (readFormat != null)
                            {
                                if (!definedFormats.containsKey(readFormat))
                                {
                                    definedFormats.put(readFormat, new WritableCellFormat(readFormat));
                                }
                                label.setCellFormat(definedFormats.get(readFormat));
                            }
                        } catch (Exception e) {
                            Log.e("copySheet", "Fehler beim Kopieren des Zellformats in Spalte " + i + ", Zeile " + j + ": " + e.getMessage());
                            // Weiter ohne Format
                        }

                        // CellFeatures werden nicht kopiert, da sie AssertionFailed-Fehler verursachen können
                        // Die Features (Kommentare, Validierung) sind für die Programm-Tabellen nicht kritisch
                        // try {
                        //     CellFeatures readFeatures = readCell.getCellFeatures();
                        //     if (readFeatures != null)
                        //     {
                        //         label.setCellFeatures(new WritableCellFeatures(readFeatures));
                        //     }
                        // } catch (Exception e) {
                        //     Log.e("copySheet", "CellFeatures können nicht kopiert werden in Spalte " + i + ", Zeile " + j + ": " + e.getMessage());
                        // }
                    }

                }
                catch (Exception e)
                {
                    Log.e("copySheet" , "read Error in Spalte " + i + ", Zeile " + j + ": " + e.getMessage()) ;

                }

                try
                {
                    //excelWrite.putCellString(i,j, stringTemp);
                    excelWrite.putCell(label); ;
                }
                catch (WriteException e)
                {
                    Log.e("copySheet" , "write Error in Spalte " + i + ", Zeile " + j + ": " + e.getMessage()) ;
                }
            }
        }
    }

    public void putCellStringKeepFormat(int spalte, int zeile, String newString)
    {
        Label label = null ;

        Map<CellFormat, WritableCellFormat> definedFormats = new HashMap<CellFormat, WritableCellFormat>();

        try
        {
            // Prüfe, ob die Zelle in der Quelldatei existiert
            Cell readCell = null;
            if (spalte < excelRead.getCellSpalten()) {
                readCell = excelRead.getCell(spalte, zeile);
            }
            
            // Erstelle Label mit neuem String
            label = new Label(spalte, zeile, newString) ;

            // Wenn die Zelle in der Quelldatei existiert, kopiere das Format
            if (readCell != null) {
                try {
                    CellFormat readFormat = readCell.getCellFormat();
                    if (readFormat != null)
                    {
                        if (!definedFormats.containsKey(readFormat))
                        {
                            definedFormats.put(readFormat, new WritableCellFormat(readFormat));
                        }
                        label.setCellFormat(definedFormats.get(readFormat));
                    }
                } catch (Exception e) {
                    Log.e("putCellStringKeepFormat", "Fehler beim Kopieren des Zellformats in Spalte " + spalte + ", Zeile " + zeile + ": " + e.getMessage());
                    // Weiter ohne Format
                }

                // CellFeatures werden nicht kopiert, da sie AssertionFailed-Fehler verursachen können
                // Die Features (Kommentare, Validierung) sind für die Programm-Tabellen nicht kritisch
                // try {
                //     CellFeatures readFeatures = readCell.getCellFeatures();
                //     if (readFeatures != null)
                //     {
                //         label.setCellFeatures(new WritableCellFeatures(readFeatures));
                //     }
                // } catch (Exception e) {
                //     Log.e("putCellStringKeepFormat", "CellFeatures können nicht kopiert werden in Spalte " + spalte + ", Zeile " + zeile + ": " + e.getMessage());
                // }
            }
            // Wenn die Zelle nicht existiert (z.B. Spalte D fehlt), wird sie ohne Format erstellt

        }
        catch (Exception e)
        {
            Log.e("putCellStringKeepFormat" , "read Error in Spalte " + spalte + ", Zeile " + zeile + ": " + e.getMessage()) ;
            // Erstelle Label ohne Format, falls Lesen fehlschlägt
            label = new Label(spalte, zeile, newString) ;
        }

        try
        {
            //excelWrite.putCellString(i,j, stringTemp);
            excelWrite.putCell(label); ;
        }
        catch (WriteException e)
        {
            Log.e("putCellStringKeepFormat" , "write Error in Spalte " + spalte + ", Zeile " + zeile + ": " + e.getMessage()) ;
        }
    }

    public void writeCopy(int sourceZeile, int targetZeile, int spalte)
    {
        Label label = null ;
        String stringTemp ;

        Map<CellFormat, WritableCellFormat> definedFormats = new HashMap<CellFormat, WritableCellFormat>();

        try
        {

            stringTemp = excelRead.getCellString(spalte, sourceZeile) ;
            Cell readCell = excelRead.getCell(spalte, sourceZeile);

            Log.e("copyCell" , "sourceZeile=" + sourceZeile + " targetZeile=" + targetZeile + " spalte=" + spalte + " stringTemp=" + stringTemp) ;

            label = new Label(spalte, targetZeile, stringTemp) ;
            //Log.e("labelString=" , "=" + labelStri1ng) ;

            CellFormat readFormat = (readCell != null) ? readCell.getCellFormat() : null;
            if (readFormat != null)
            {
                if (!definedFormats.containsKey(readFormat))
                {
                    definedFormats.put(readFormat, new WritableCellFormat(readFormat));
                }
                label.setCellFormat(definedFormats.get(readFormat));
            }

            CellFeatures readFeatures = (readCell != null) ? readCell.getCellFeatures() : null;
            if (readFeatures != null)
            {
                label.setCellFeatures(new WritableCellFeatures(readFeatures));
            }

        }
        catch (Exception e)
        {
            Log.e("copyCell" , "read Error" ) ;

        }

        try
        {
            //excelWrite.putCellString(i,j, stringTemp);
            excelWrite.putCell(label); ;
        }
        catch (WriteException e)
        {
            Log.e("copyCell" , "write Error" ) ;
        }
    }

    private WritableSheet createSheetCopy(Workbook w, int from, int to,
                                                 WritableWorkbook writeableWorkbook) throws WriteException {
        Sheet sheet = w.getSheet(from);
        WritableSheet newSheet = writeableWorkbook.getSheet(to);
        // Avoid warning
        // "Maximum number of format records exceeded. Using default format."
        Map<CellFormat, WritableCellFormat> definedFormats = new HashMap<CellFormat, WritableCellFormat>();
        for (int colIdx = 0; colIdx < sheet.getColumns(); colIdx++)
        {
            newSheet.setColumnView(colIdx, sheet.getColumnView(colIdx));

            for (int rowIdx = 0; rowIdx < sheet.getRows(); rowIdx++)
            {
                if (colIdx == 0) {
                    newSheet.setRowView(rowIdx, sheet.getRowView(rowIdx));
                }
                Cell readCell = sheet.getCell(colIdx, rowIdx);
                Label label = new Label(colIdx, rowIdx, readCell.getContents());
                CellFormat readFormat = readCell.getCellFormat();
                if (readFormat != null)
                {
                    if (!definedFormats.containsKey(readFormat))
                    {
                        definedFormats.put(readFormat, new WritableCellFormat(
                                readFormat));
                    }
                    label.setCellFormat(definedFormats.get(readFormat));
                }
                newSheet.addCell(label);
            }
        }


        return newSheet;
    }

    public String getCellString(int spalte, int zeile)
    {
        try
        {
            return excelWrite.getCellString(spalte,zeile) ;

        } catch (Exception e)
        {
            return "" ;
        }
    }

    public void putCellStringWithoutFormat(int spalte, int zeile, String cellString)
    {
        try
        {
            excelWrite.putCellString(spalte,zeile,cellString);
        } catch (WriteException e)
        {
            //  e.printStackTrace();
            Log.e("readWrite" , "putCellString Error") ;
        }
    }

    public void saveAndCloseWorkbooks() throws IOException, WriteException
    {
        //excelRead.closeWorkbook();

        excelWrite.writeWorkbook();
        excelWrite.closeWorkbook();

        excelRead.closeWorkbook();

        //renameCopy() ;
    }

    public void closeWorkbooks() throws IOException, WriteException
    {
        excelRead.closeWorkbook();
        excelWrite.closeWorkbook();
    }

    public void copyTempWorkbook()
    {
        File from = new File(inputFileName) ;
        from.delete() ;
        File to = new File(outputFileName) ;
        to.renameTo(from) ;
    }

    public int getCellSpalten()
    {
        return excelRead.getCellSpalten() ;
    }
}
