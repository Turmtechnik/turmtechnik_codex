package tom.turmtechnik;

import java.io.IOException;

import jxl.read.biff.BiffException;
import jxl.write.WriteException;

/**
 * Datenquelle für den Programm-Editor: Excel-Datei über ExcelReadWrite.
 * Zeile = Excel-Zeilenindex (z. B. 3 = erste Datenzeile).
 */
public class EditorDataExcel implements IEditorDataSource {

    private final ExcelReadWrite excelReadWrite;
    private static final int MAX_ROWS = 500;

    public EditorDataExcel(ExcelReadWrite excelReadWrite) {
        this.excelReadWrite = excelReadWrite;
    }

    @Override
    public String getCellString(int spalte, int zeile) {
        try {
            String s = excelReadWrite.getCellString(spalte, zeile);
            return s != null ? s : "";
        } catch (Exception e) {
            return "";
        }
    }

    @Override
    public void putCellString(int spalte, int zeile, String value) {
        try {
            excelReadWrite.putCellStringKeepFormat(spalte, zeile, value != null ? value : "");
        } catch (Exception e) {
            // ignore
        }
    }

    @Override
    public int getRowCount() {
        return MAX_ROWS;
    }

    @Override
    public void save() throws IOException, WriteException {
        excelReadWrite.saveAndCloseWorkbooks();
    }

    @Override
    public boolean isDatabaseMode() {
        return false;
    }

    @Override
    public int copyRow(int fromRow) {
        return fromRow;
    }

    public ExcelReadWrite getExcelReadWrite() {
        return excelReadWrite;
    }
}
