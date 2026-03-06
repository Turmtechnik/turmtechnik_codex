package tom.turmtechnik;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

public class BluetoothSerial_io {
    private static final String TAG = BluetoothSerial_io.class.getSimpleName();
    /** Timeout in ms für connect – verhindert langes Blockieren, wenn BT-Gerät nicht erreichbar ist. */
    private static final int CONNECT_TIMEOUT_MS = 12_000;
    //	BluetoothAdapter mBluetoothAdapter ; // = BluetoothAdapter.getDefaultAdapter();
    private String btMac = null;
    private BluetoothDevice btDevice;
    private BluetoothSocket btSocket;
    private OutputStream btOutbputStream;
    private InputStream btInputStream;
    private ArrayList<Integer> sendByteBuffer = new ArrayList<Integer>();
    private ArrayList<Integer> inByteBuffer = new ArrayList<Integer>();
    byte[] byteBuffer = new byte[256];
    private final UUID serial_uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"); //UUID for serial connection

//	public BluetoothSerial_io()  // konstruktor
//	{
//		mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter() ;
//	}


    public boolean isEnabled() {
        return StaticVariable.myBluetoothAdapter.isEnabled();
    }

    public boolean adapterOk() {
        if (StaticVariable.myBluetoothAdapter == null) {
            Log.e(TAG, "bt_adapter = null");
            return false;
        } else {
            Log.i(TAG, "bt_adapter O.K");
            return true;
        }
    }


    public boolean connect() {

        Log.e("CONNECT", "aufgerufen");
        // Alte Verbindung zuerst schließen, sonst „RFCOMM already at opened state“ beim erneuten Connect
        closeSocketAndStreams();
        btMac = null;

        StaticVariable.myBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (StaticVariable.myBluetoothAdapter == null) {
            Log.e(TAG, "Bluetooth-Adapter nicht verfügbar (getDefaultAdapter = null)");
            return false;
        }
        if (!StaticVariable.myBluetoothAdapter.isEnabled()) {
            try {
                StaticVariable.myBluetoothAdapter.enable();
            } catch (SecurityException e) {
                Log.e(TAG, "Bluetooth aktivieren fehlgeschlagen (Permission?): " + e.getMessage());
                return false;
            }
        }

        Set<BluetoothDevice> pairedDevices;
        try {
            pairedDevices = StaticVariable.myBluetoothAdapter.getBondedDevices();
        } catch (SecurityException e) {
            Log.e(TAG, "getBondedDevices fehlgeschlagen (BLUETOOTH_CONNECT erteilen?): " + e.getMessage());
            return false;
        }
        if (pairedDevices == null) {
            Log.e(TAG, "getBondedDevices lieferte null");
            return false;
        }
        if (pairedDevices.size() > 0) {
            boolean connectionOK = false;

            Iterator<BluetoothDevice> it = pairedDevices.iterator();

            while (it.hasNext()) {
                BluetoothDevice device = (BluetoothDevice) it.next();
                Log.e("found " + device.getName(), "mac=" + device.getAddress());

                btMac = device.getAddress();
                try {
                    btDevice = StaticVariable.myBluetoothAdapter.getRemoteDevice(btMac);
                } catch (SecurityException e) {
                    Log.e(TAG, "getRemoteDevice fehlgeschlagen (BLUETOOTH_CONNECT?): " + e.getMessage());
                    continue;
                }
                if (btDevice == null) {
                    Log.e(TAG, "getRemoteDevice lieferte null für MAC " + btMac);
                    continue;
                }

                try {
                    btSocket = btDevice.createRfcommSocketToServiceRecord(serial_uuid);
                } catch (IOException e) {
                    continue;
                } catch (SecurityException e) {
                    Log.e(TAG, "createRfcommSocket fehlgeschlagen (Permission?): " + e.getMessage());
                    continue;
                }

                connectionOK = connectSocketWithTimeout(btSocket, CONNECT_TIMEOUT_MS);
                if (!connectionOK) {
                    try {
                        btSocket = fallbackCreateRfcommSocket(device);
                        if (btSocket != null) {
                            connectionOK = connectSocketWithTimeout(btSocket, CONNECT_TIMEOUT_MS);
                        }
                    } catch (Exception e2) {
                        Log.e("btSocket", "connect error");
                    }
                } else {
                    Log.e("btSocket", "connect O.K.");
                }

                if (connectionOK) {
                    try {
                        btOutbputStream = btSocket.getOutputStream();
                        btInputStream = btSocket.getInputStream();
                    } catch (IOException e) {
                        e.printStackTrace();
                        connectionOK = false;
                    }
                    if (connectionOK) {
                        StaticVariable.myBluetoothAdapter.cancelDiscovery();
                        Log.e("return", "true");
                        return true;
                    }
                }
                try {
                    if (btSocket != null) {
                        btSocket.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
                btSocket = null;
            }
        }
        Log.e("connect", "return false");
        return false;
    }

    /**
     * Führt socket.connect() in einem Hilfsthread aus und wartet maximal timeoutMs.
     * Verhindert langes Blockieren, wenn das Gerät nicht erreichbar ist.
     */
    private boolean connectSocketWithTimeout(final BluetoothSocket socket, int timeoutMs) {
        if (socket == null) return false;
        final AtomicBoolean ok = new AtomicBoolean(false);
        final Thread connectThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    socket.connect();
                    ok.set(true);
                } catch (IOException e) {
                    Log.e("btSocket", "connect error: " + (e.getMessage() != null ? e.getMessage() : ""));
                }
            }
        });
        connectThread.start();
        try {
            connectThread.join(timeoutMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        if (connectThread.isAlive()) {
            connectThread.interrupt();
            try { socket.close(); } catch (IOException ignored) { }
            Log.e("btSocket", "connect timeout");
            return false;
        }
        return ok.get();
    }

    private BluetoothSocket fallbackCreateRfcommSocket(BluetoothDevice device) {
        try {
            Method m = device.getClass().getMethod("createRfcommSocket", int.class);
            return (BluetoothSocket) m.invoke(device, 1);
        } catch (Exception e) {
            return null;
        }
    }
    
    /**
     * Gibt die MAC-Adresse des verbundenen Bluetooth-Geräts zurück.
     */
    public String getBtMac() {
        return btMac;
    }

    /**
     * Schließt nur die Socket-Verbindung zum Gerät. Der Bluetooth-Adapter bleibt an
     * (disable() würde den gesamten BT-Stack ausschalten und „already at opened state“ / Reconnect-Probleme verursachen).
     */
    public void disconnect() {
        closeSocketAndStreams();
    }

    /** Schließt Socket und Streams, falls vorhanden. Adapter wird nicht verändert. */
    private void closeSocketAndStreams() {
        try {
            if (btOutbputStream != null) {
                try { btOutbputStream.close(); } catch (IOException ignored) { }
                btOutbputStream = null;
            }
            if (btInputStream != null) {
                try { btInputStream.close(); } catch (IOException ignored) { }
                btInputStream = null;
            }
            if (btSocket != null) {
                btSocket.close();
                btSocket = null;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void writeByte(int sendByte) {
        try {
            btOutbputStream.write(sendByte);

        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public void writeSendByteBuffer() throws IOException {
        if (!sendByteBuffer.isEmpty()) {
            int anzahl = sendByteBuffer.size();
            //   Log.i("anzahl" , "=" + anzahl);

            for (int i = 0; i < anzahl; i++) {
                int temp = sendByteBuffer.get(i);
                byteBuffer[i] = (byte) temp;
            }

            if (btOutbputStream != null) {
                btOutbputStream.write(byteBuffer, 0, anzahl);
            } else {
                throw new IOException("Bluetooth-Stream nicht verbunden");
            }
        }
    }

    /** Schließt bei Schreibfehler die Verbindung, damit Reconnect sauber funktioniert. */
    private void writeSendByteBufferOrCloseOnError() {
        try {
            writeSendByteBuffer();
        } catch (IOException e) {
            e.printStackTrace();
            closeSocketAndStreams();
        }
    }


    public int available() {
        try {
            if (btInputStream != null) {
                return btInputStream.available();
            } else {
                return 0;
            }
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return 0;
        }
    }

    public int readByte() {
        try {
            return btInputStream.read();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return -1;
        }
    }


    public void sendBytes(ArrayList<Integer> byteFeld) {

        if (!(byteFeld.isEmpty())) {
            writeSendByteBufferOrCloseOnError();
        }
//			for(int i = 0 ; i < byteFeld.size(); i++)
//			{
//				writeByte(byteFeld.get(i));
//			}
    }


    public void sendPortsABCD(int portA, int portB, int portC, int portD, int picNumber) {
        // Protokoll vom 30.1.2013  erstest byte Befehl, 2..6 Ports, MSP ... LSP, CKSUM_H CKSUM_L
        // Vector<Integer> byteFeld = new Vector<Integer>() ;

        sendByteBuffer.clear();
        sendByteBuffer.add((int) 'A');
        sendByteBuffer.add(picNumber); // port E = dummy
        sendByteBuffer.add(portD); // port D = dummy
        sendByteBuffer.add(portC);
        sendByteBuffer.add(portB);
        sendByteBuffer.add(portA);

        int cksum = 0;
        for (int i = 0; i < sendByteBuffer.size(); i++) {
            cksum += sendByteBuffer.get(i);
        }

        //   -->  Log.i("int cksum" , "=" + cksum) ;

        sendByteBuffer.add(cksum / 256);
        sendByteBuffer.add(cksum & 0xFF);

        writeSendByteBufferOrCloseOnError();

        //sendBytes(sendByteBuffer) ;

        //Log.i("daten" , msg.toString()) ;
    }

    public void send5Bytes(int[] ports) // Logik von Herrn Berger ist umgekehrt
    {


        sendByteBuffer.clear();
        sendByteBuffer.add((int) 'A');
        sendByteBuffer.add(ports[4]); // picNumber
        sendByteBuffer.add(ports[3]); // portA
        sendByteBuffer.add(ports[2]); // portB
        sendByteBuffer.add(ports[1]); // portC
        sendByteBuffer.add(ports[0]); // portd

        int cksum = 0;
        for (int i = 0; i < sendByteBuffer.size(); i++) {
            cksum += sendByteBuffer.get(i);
        }

        // --> Log.i("int cksum" , "=" + cksum) ;

        sendByteBuffer.add(cksum / 256);
        sendByteBuffer.add(cksum & 0xFF);

        writeSendByteBufferOrCloseOnError();
    }

    private String getStringByteFeld() {
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < sendByteBuffer.size(); i++) {
            sb.append(sendByteBuffer.get(i));
            sb.append(":");
        }

        return sb.toString();
    }

    public void befehlReadPorts() {
        sendByteBuffer.clear();
        sendByteBuffer.add((int) 'a');
        sendByteBuffer.add(0); // port E = dummy
        sendByteBuffer.add(0); // port D = dummy
        sendByteBuffer.add(0);
        sendByteBuffer.add(0);
        sendByteBuffer.add(0);

        int cksum = 0;
        for (int i = 0; i < sendByteBuffer.size(); i++) {
            cksum += sendByteBuffer.get(i);
        }

        // --> Log.i("int cksum" , "=" + cksum) ;

        sendByteBuffer.add(cksum / 256);
        sendByteBuffer.add(cksum & 0xFF);

        writeSendByteBufferOrCloseOnError();
        //sendBytes(sendByteBuffer) ;
    }

    public ArrayList<Integer> getSerialBytes() {
        //Log.i("lese" , "getSerialBytes") ;

        inByteBuffer.clear();
        while (available() > 0) {
            int inbyte = readByte();

            //Log.i("BYTE","=" + inbyte) ;

            inByteBuffer.add(inbyte);
            //Log.i("inByteBuffer" , "=" + inByteBuffer.size()) ;
        }
        return inByteBuffer;
    }


}// Ende	



