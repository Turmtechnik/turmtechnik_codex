package tom.turmtechnik;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import android.os.Environment;
import android.util.Log;

public class LogExcelError {

    private String datumPlusZeit;

    private int excelX;
    private int excelY;
    private int sheetNumber;
    private String excelFilename;
    private Calendar calendar;
    private String sourceFileName;
    private int lineNumber;

    // konstruktor
    public LogExcelError(int excelX, int excelY, String excelFilename, int sheetNumber, String sourceFileName, int lineNumber) {

        calendar = Calendar.getInstance();
        SimpleDateFormat formater = new SimpleDateFormat();
        datumPlusZeit = (formater.format(calendar.getTime()));  // 1.10.13 10:03

        this.excelX = excelX;
        this.excelY = excelY;
        this.excelFilename = excelFilename;
        this.sheetNumber = sheetNumber;
        this.sourceFileName = sourceFileName;
        this.lineNumber = lineNumber;

        // hier dann dass logfile abspeichern
        // excelX und excelY und sheetNumber in String zum speichern verwandeln
        String stringExcelX = Integer.toString(excelX);
        String stringExcelY = Integer.toString(excelY);
        String stringSheetNumber = Integer.toString(sheetNumber);
        String stringLineNumber = Integer.toString(lineNumber);
        StringBuilder sb = new StringBuilder();

        sb.append(datumPlusZeit);
        sb.append(" ");
        sb.append(excelFilename);
        sb.append(" ");
        sb.append("sheet ");
        sb.append(stringSheetNumber);
        sb.append(" ");
        sb.append("spalte ");
        sb.append(stringExcelX);
        sb.append(" zeile ");
        sb.append(stringExcelY);
        sb.append(" source ");
        sb.append(sourceFileName);
        sb.append(" line ");
        sb.append(stringLineNumber);
        sb.append("\r\n");

        String errorString = sb.toString();

        Log.e("LogExcelError", "" + errorString);

        if (StaticVariable.logFehlerCrashes) {
            appendErrorLog(errorString);
            LogTurmtechnik2.appendCrashLog("Fehler (Excel/Config): " + excelFilename + " sheet " + sheetNumber + " " + sourceFileName + ":" + lineNumber);
        }

    }

    private void appendErrorLog(String errorString) {
        String fileName = Environment.getExternalStorageDirectory().toString() + "/Turmtechnik/LogError.txt";

        File logFile = new File(fileName);
        try {
            File parent = logFile.getParentFile();
            if (parent != null && !parent.exists()) parent.mkdirs();
            if (!logFile.exists()) logFile.createNewFile();
            BufferedWriter buf = new BufferedWriter(new FileWriter(logFile, true));
            buf.append(errorString);
            // buf.newLine();
            buf.close();
        } catch (IOException e) {
            // was nun?
        }
    }
}
