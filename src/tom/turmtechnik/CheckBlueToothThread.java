package tom.turmtechnik;

import java.util.ArrayList;

import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.util.Log;

public class CheckBlueToothThread extends Thread {

    // veraendert Stativariable.bluetoothStatus
    //        1 = O.K.
    //        2 = keine bluetooth hardware unterstuetzung
    //        3 = keine verbindung

    private BluetoothSerial_io bluetooth_io;


    private int portA = 0;
    private int portB = 0;
    private int portC = 0;
    private int portD = 0;
    private int picNumber = 1;

    int readErrorCount = 0;

    public void run() {
        bluetooth_io = new BluetoothSerial_io();

        if (!bluetooth_io.adapterOk()) {
            StaticVariable.bluetoothStatus = 2; // geraet hat keine bluetooth hardware

        } else {
            if (checkEcho()) {
                StaticVariable.bluetoothStatus = 1; // verbindung O.K.
            } else {
                StaticVariable.bluetoothStatus = 3; // 3 mal keine Verbindung
            }
        }

    }

    private boolean checkEcho() {


        readErrorCount = 0;
        for (int i = 0; i < 3; i++) {

            if (bluetooth_io.connect()) {

                bluetooth_io.sendPortsABCD(portA, portB, portC, portD, picNumber);
                sleepTime(100);

                bluetooth_io.befehlReadPorts();
                sleepTime(200); // pic zeit lassen fuer Antwort

                // tempList.clear();
                ArrayList<Integer> tempList = bluetooth_io.getSerialBytes();

                bluetooth_io.disconnect();

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
