package tom.turmtechnik;

import android.bluetooth.BluetoothAdapter;
import java.util.ArrayList;

/**
 * Konfiguration und Status für I/O-Verbindungen (Bluetooth, Serial, Carambola, Internet).
 */
public class IOConfig {
    // Bluetooth
    public static boolean bluetoothMode;
    public static BluetoothAdapter myBluetoothAdapter;
    public static int bluetoothStatus;  // 0 = noch nicht getestet, 1 = keine Bluetooth hardware, 2 = keine Verbindung
    public static boolean bt_io_ok;

    // Carambola
    public static int carambolaStatus; // -1 vor dem checkCarambolaThread, 0 wenn alles O.K., ab 1 die IP Adresse wo ein Fehler aufgetreten ist
    public static boolean carambola_io_ok;

    // Serial IO Status
    public static boolean serial_io_status4[] = {false, true, true, true}; // ist true wenn ein echo von bluetooth oder carambole gekommen ist
    public static boolean serial_io_ThreadsRun;

    // IP/Port Listen
    public static ArrayList<String> ipList = new ArrayList<String>();
    public static ArrayList<Integer> portList = new ArrayList<Integer>();

    // Internet
    public static boolean internetOnOffFlag = true;
    public static int internetOnStunden = 0;
    public static int internetOnMinuten = 0;
    public static int internetOffStunden = 0;
    public static int internetOffMinuten = 0;
    public static int changeInternetVerknuepfteTasten = 0;
    public static int changeInternetBenutzerprogramme = 0;
    public static boolean internetChanged = false;

    // Fernwartung (veraltet, aber noch vorhanden)
    public static String fernwartungServerId;
    public static String fernwartungServerPassword;
    public static String gelesenVonJsonRequest = "";
}
