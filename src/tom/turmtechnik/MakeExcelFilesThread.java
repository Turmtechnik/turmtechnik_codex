package tom.turmtechnik;

import android.util.Log;

import org.apache.http.HttpEntity;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import jxl.read.biff.BiffException;
import jxl.write.WriteException;
import jxl.write.biff.RowsExceededException;

public class MakeExcelFilesThread extends Thread {
    private static String urlGetBeschriftungTastenString =
            "http://app.turmtechnik.com/json.php?tokenid=" + StaticVariable.fernwartungServerId
                    + "&tokenpw=" + StaticVariable.fernwartungServerPassword + "&reqtype=konfig&reqtable=tasten";

    private final String[] beschriftungTastenErsteZeile =
            {"nummer", "funktion", "name", "relais", "himpuls", "platine", "normal",
                    "meinok", "meinerror", "aeinok", "aeinerror", "sound", "status"
            };

    private ExcelWrite excelWrite;

    public void run() {
        StringBuilder builder = new StringBuilder();
        HttpClient client = new DefaultHttpClient();
        HttpGet httpGet = new HttpGet(urlGetBeschriftungTastenString);
        try {
            org.apache.http.HttpResponse response = client.execute(httpGet);
            StatusLine statusLine = response.getStatusLine();
            int statusCode = statusLine.getStatusCode();
            if (statusCode == 200) {
                HttpEntity entity = response.getEntity();
                InputStream content = entity.getContent();
                BufferedReader reader = new BufferedReader(new InputStreamReader(content));
                String line;
                while ((line = reader.readLine()) != null) {
                    //Log.e("LINE" , "=" + line) ;
                    builder.append(line);
                }
            } else {
                Log.e("READ JSON", "Fehler beim download File");
            }
        } catch (ClientProtocolException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        String gelesenVonJsonRequest = builder.toString();
        //Log.e("MakeExcelFilesThread" , "gelesen: " + gelesenVonJsonRequest) ;

        excelWrite = new ExcelWrite();
        try {
            excelWrite.openXlsWriteSheet(TurmtechnikActivity.sdCardPath
                            + "/Turmtechnik/Config/Beschriftung-Tasten.xls", 0 /* sheet_number */
                    , "Beschriftung Tasten" /* sheet_name */);
        } catch (BiffException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        } catch (IOException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }

        for (int i = 0; i < beschriftungTastenErsteZeile.length; i++) {
            // mache zeile 0 = ueberschrift
            writeOneCell(i, 0, beschriftungTastenErsteZeile[i]);
        }

        try {
            JSONArray jsonArray = new JSONArray(gelesenVonJsonRequest);
            Log.e("jsonArray Eintraege", "= " + jsonArray.length());
            for (int y = 0; y < jsonArray.length(); y++) {
                JSONObject jsonObject = jsonArray.getJSONObject(y);

                for (int x = 0; x < beschriftungTastenErsteZeile.length; x++) {
                    Log.e("" + beschriftungTastenErsteZeile[x], "= "
                            + jsonObject.getString(beschriftungTastenErsteZeile[x]));

                    writeOneCell(x, y + 1, jsonObject.getString(beschriftungTastenErsteZeile[x]));
                }
            }

            writeAndCloseExcel();
            setRestartTimer(2); // restart Turmtechnik in 2 sekunden

        } catch (JSONException e) {
            // TODO Auto-generated catch block
            Log.e("JSON", "Exception=" + e.toString());
            writeAndCloseExcel();
        }


    } // ende von run

    private void writeOneCell(int x, int y, String cellString) {
        try {
            excelWrite.putCellString(x, y, cellString);
        } catch (RowsExceededException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (WriteException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    private void writeAndCloseExcel() {
        try {
            excelWrite.writeWorkbook();
            excelWrite.closeWorkbook();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            Log.e("FEHLER 5", "beim Excel schreiben");
        } catch (WriteException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            Log.e("FEHLER 6", "beim Excel schreiben");
        }
    }

    public synchronized void setRestartTimer(int restartTime) {
        StartTurmtechnikService.setTimeTurmtechnik(restartTime);
    }


    private void sleepTime(long time) {
        try {
            Thread.sleep(time);
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

} // ende der Klasse
