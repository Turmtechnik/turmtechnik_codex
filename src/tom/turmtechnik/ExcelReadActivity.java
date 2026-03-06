package tom.turmtechnik;

//import java.io.IOException;

import java.io.File;
import java.io.IOException;

import jxl.Cell;
import jxl.Sheet;
import jxl.Workbook;
import jxl.WorkbookSettings;
import jxl.read.biff.BiffException;

import android.app.Activity;
import android.os.Bundle;
import android.os.Environment;
import android.os.StrictMode;
import android.widget.LinearLayout;
import android.widget.TextView;

public class ExcelReadActivity extends Activity {

    private TextView outputView;
    private String inputFileString;
    private File inputWorkbook;
    private Boolean fileOk = false;

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //      setContentView(R.layout.main);

        //      PoiGet test = new PoiGet();

        //      ExcelGet test = new ExcelGet();

//       StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
//       StrictMode.setThreadPolicy(policy);

        LinearLayout viewGroup = new LinearLayout(this);
        outputView = new TextView(this);
        outputView.setText("Textausgabe: ");
        this.setContentView(viewGroup);
        viewGroup.addView(outputView);

        outputView.append("Hallo Fred" + "\n");

        //     test.setInputFile(Environment.getExternalStorageDirectory().getPath()+"/Kirchurmtechnik/Programmliste.xls");
        //    test.setInputFile(Environment.getExternalStorageDirectory().getPath()+"/Turmtechnik/Beschriftung Glocken Variante2.xls");
        //    outputView.append("Input file:\n "+test.getInputFile()+"\n");

//		test.setInputFile(Environment.getDataDirectory().getPath()+"/Turmtechnik/Beschriftung Glocken Variante2.xls");
//		outputView.append("Input file:\n "+test.getInputFile()+"\n");      


        inputFileString = (Environment.getExternalStorageDirectory().getPath() + "/Turmtechnik/Beschriftung Glocken Variante2.xls");
        inputWorkbook = new File(inputFileString);
        if (inputWorkbook.exists()) {
            outputView.append("Daten auf SD Karte vorhanden\n");
            fileOk = true;
        } else {
            inputFileString = (Environment.getDataDirectory().getPath() + "/Turmtechnik/Beschriftung Glocken Variante2.xls");
            inputWorkbook = new File(inputFileString);
            if (inputWorkbook.exists()) {
                outputView.append("Daten im Telefonspeicher vorhanden\n");
                fileOk = true;
            } else {
                outputView.append("keine Daten vorhanden\n");

            }
        }


        Workbook te = null;

        String cellString;

        if (fileOk) {

            WorkbookSettings ws = new WorkbookSettings();
            ws.setEncoding("ISO-8859-1");
            //        ws.setEncoding("Cp1252");  // geht auch
            //        ws.setExcelDisplayLanguage("DE"); // brauchts nicht
            //        ws.setExcelRegionalSettings("DE"); // brauchts nicht

//            test.makeWorkbook();

            try {
                te = Workbook.getWorkbook(inputWorkbook, ws);
            } catch (BiffException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            outputView.append("File:" + inputWorkbook.getAbsolutePath() + "\n");

            Sheet sheet = te.getSheet(0);

            for (int zeile = 1; zeile <= 16; zeile++) {

                for (int spalte = 0; spalte < 4; spalte++) {

                    Cell cell = sheet.getCell(spalte, zeile);
                    cellString = cell.getContents();


                    outputView.append(cellString + " ");
                    if (spalte == 2 && cellString != "") {
                        outputView.append("Relais ");
                    }
                }
                outputView.append("\n");
            }
        }
    }
}