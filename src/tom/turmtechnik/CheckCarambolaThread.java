package tom.turmtechnik;

import android.util.Log;

import java.util.ArrayList;

public class CheckCarambolaThread extends Thread {

    // veraendert Stativariable.bluetoothStatus
    //        1 = O.K.
    //        2 = keine bluetooth hardware unterstuetzung
    //        3 = keine verbindung

    private Carambola_io carambola_io;


    private int portA = 0;
    private int portB = 0;
    private int portC = 0;
    private int portD = 0;
    private int picNumber = 1;

    int readErrorCount = 0;

    public void run() {
        carambola_io = new Carambola_io();

        // checkCarambolaThread mit carambolaStatus -1 starten

        // Nur bis min(ipList, portList), um IndexOutOfBoundsException bei leerer/unterschiedlicher Größe zu vermeiden
        int n = (StaticVariable.ipList != null && StaticVariable.portList != null)
                ? Math.min(StaticVariable.ipList.size(), StaticVariable.portList.size()) : 0;
        for (int i = 0; i < n; i++) {
            if (!checkEcho(StaticVariable.ipList.get(i), StaticVariable.portList.get(i))) {
                StaticVariable.carambolaStatus = (i);
                break;
            }
        }

        StaticVariable.carambolaStatus++;
    }

    private boolean checkEcho(String ip, int port) {

        Log.e("checkEcho", "ip=" + ip);
        readErrorCount = 0;
        for (int i = 0; i < 3; i++) {

            boolean checkConnect = carambola_io.connect(ip, port);
            Log.e("checkConnect", "=" + checkConnect);
            if (checkConnect) {

                Log.e("checkCarambolaThread", "sendPortABCD");
                carambola_io.sendPortsABCD(portA, portB, portC, portD, picNumber);
                sleepTime(100);

                carambola_io.befehlReadPorts();
                sleepTime(200); // pic zeit lassen fuer Antwort

                // tempList.clear();
                ArrayList<Integer> tempList = carambola_io.getSerialBytes();

                carambola_io.disconnect();

                if (tempList.size() == 8) {
                    if (tempList.get(0) == 'a') {
                        return true; // wenn echo 'a' passt dann sofort beenden
                    } else {
                        readErrorCount++;
                    }

                } else {
                    readErrorCount++;
                }
            } else {
                return false;
            }
        }

        // return (readErrorCount == 3 );

        if (readErrorCount == 3) {
            return false;
        } else {
            return true;
        }

    }

    private void sleepTime(long time) {

        try {
            Thread.sleep(time);
        } catch (InterruptedException e) {
            Log.i("SLEEP", e.toString());
        }

    }

}
