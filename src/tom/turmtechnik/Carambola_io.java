package tom.turmtechnik;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

import android.util.Log;

public class Carambola_io {
    private static final String TAG = Carambola_io.class.getSimpleName();

    /** Pro IP:Port genau ein Lock – verhindert, dass zwei Threads gleichzeitig connect/send/read zur gleichen Platine machen und sich die Antwort „wegnehmen“. */
    private static final ConcurrentHashMap<String, ReentrantLock> connectionLocks = new ConcurrentHashMap<>();

    /** Lock für diese Platine (IP:Port). Muss vor connect() geholt und erst nach vollständigem Lesen der Antwort (oder Timeout) wieder freigegeben werden. */
    public static ReentrantLock getLockFor(String ip, int port) {
        String key = (ip != null ? ip : "") + ":" + port;
        connectionLocks.putIfAbsent(key, new ReentrantLock());
        return connectionLocks.get(key);
    }

    private Socket client = null;

    //private final int port = 50210 ;


    private boolean portOpen;

    ArrayList<Integer> inByteBuffer = new ArrayList<Integer>();
    ArrayList<Integer> sendByteBuffer = new ArrayList<Integer>();

    BufferedReader in = null;
    BufferedInputStream inbytes = null;
    String input = "";


    /**
     * Konstruktor
     *
     * @param ip   des Carambola boards z.B. 192.168.1.4
     * @param port ( 50210 )
     */

    // kein Konstruktor
//	public Carambola_io(String ip, int port)
//	{
//		this.ip = ip ;
//		this.port = port ;
//	}
    public boolean connect(String ip, int port) {
        portOpen = false;
        try {
            client = new Socket();
            client.connect(new InetSocketAddress(ip, port), 1500); // 1,5 s für Scan (ESP kann träge antworten)
            client.setSoTimeout(4000); // Lese-Timeout, damit blockierende Reads nicht ewig warten
            portOpen = true;
            in = null;
            inbytes = null;
            input = "";
            inbytes = new BufferedInputStream(client.getInputStream());
        } catch (UnknownHostException uhx) {
            //Log.e("Fehler", "connect" + uhx.toString()) ;
        } catch (IOException iox) {
            //Log.e("Fehler" , "connect" + iox.toString());
        }
        return portOpen;
    }

    /** True, wenn Socket besteht und verbunden ist (für Verbindungswiederverwendung). */
    public boolean isConnected() {
        return client != null && client.isConnected();
    }

    public void disconnect() {
        if (client != null) {
            try {
                client.close();
            } catch (IOException ex) {
                //Log.i("TAG" , "disconnect" + ex.toString()) ;
            }
            client = null;
            inbytes = null;
            in = null;
        }
    }

    /**
     * Einmalige Abfrage für Netzwerk-Scan: Verbindung zu ip:port, Befehl „Read Ports“ senden,
     * Antwort lesen (kurzer Timeout), Verbindung schließen.
     * @param ip IP-Adresse
     * @param port Port (z. B. 50210)
     * @return Antwort-Bytes oder leere Liste bei Fehler/keiner Antwort
     */
    public ArrayList<Integer> probeAndGetResponse(String ip, int port) {
        inByteBuffer.clear();
        if (!connect(ip, port)) {
            return new ArrayList<>();
        }
        try {
            befehlReadPorts();
            // Kurz warten, damit ESP den Befehl verarbeiten und Antwort senden kann
            Thread.sleep(80);
            try {
                if (client != null) {
                    client.setSoTimeout(3500);  // ESP kann auf Port 23 etwas träge antworten
                }
            } catch (Exception ignored) {
            }
            // Einmal blockierend auf erstes Byte warten (nicht nur pollen), dann Rest lesen
            if (inbytes != null) {
                try {
                    int first = inbytes.read();
                    if (first >= 0) {
                        inByteBuffer.add(first);
                        while (inbytes.available() > 0) {
                            int b = inbytes.read();
                            if (b < 0) break;
                            inByteBuffer.add(b);
                        }
                        Thread.sleep(50);
                        while (inbytes.available() > 0) {
                            int b = inbytes.read();
                            if (b < 0) break;
                            inByteBuffer.add(b);
                        }
                    }
                } catch (IOException e) {
                    if (Log.isLoggable(TAG, Log.DEBUG)) {
                        Log.d(TAG, "probeAndGetResponse read: " + (e.getMessage() != null ? e.getMessage() : e.toString()));
                    }
                }
            }
            return new ArrayList<>(inByteBuffer);
        } catch (Exception e) {
            return new ArrayList<>();
        } finally {
            disconnect();
        }
    }

    public void sendData(String msg) {
        try {
            PrintWriter out = new PrintWriter(client.getOutputStream());
            out.print(msg);
            out.flush();
            // StaticVariable.ergebnisZeile1 = msg;
        } catch (IOException ex) {
            //Log.e(TAG, ex.toString());
        }

    }

    public void sendBytes(ArrayList<Integer> byteFeld) {
        try {
            DataOutputStream out = new DataOutputStream(client.getOutputStream());
            if (!(byteFeld.isEmpty())) {
                for (int i = 0; i < byteFeld.size(); i++) {
                    out.write(byteFeld.get(i));
                }
            }
            out.flush();
            // Logcat carambola: was wann gesendet wird
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < byteFeld.size(); i++) {
                if (i > 0) sb.append(",");
                sb.append(byteFeld.get(i));
            }
            Log.i("carambola", "SEND " + byteFeld.size() + " Bytes: [" + sb + "]");
        } catch (IOException ex) {
            Log.e("carambola", "sendBytes Fehler: " + (ex.getMessage() != null ? ex.getMessage() : ex.toString()));
        }
    }

    public void sendPortsABCD(int portA, int portB, int portC, int portD, int reserve) {
        // Protokoll vom 30.1.2013  erstest byte Befehl, 2..6 Ports, MSP ... LSP, CKSUM_H CKSUM_L
        // Vector<Integer> byteFeld = new Vector<Integer>() ;

        sendByteBuffer.clear();
        sendByteBuffer.add((int) 'A');
        sendByteBuffer.add(reserve); // port E = dummy
        sendByteBuffer.add(portD);
        sendByteBuffer.add(portC);
        sendByteBuffer.add(portB);
        sendByteBuffer.add(portA);

//		byteFeld.add(0) ;
//		byteFeld.add(3);
//		byteFeld.add(20);
//		byteFeld.add(99);
//		byteFeld.add(127);
//		byteFeld.add(128) ;
//		byteFeld.add(254) ;
//		byteFeld.add(255) ;

        int cksum = 0;
        for (int i = 0; i < sendByteBuffer.size(); i++) {
            cksum += sendByteBuffer.get(i);
        }

        // Log.i("int cksum" , "=" + cksum) ;

        sendByteBuffer.add(cksum / 256);
        sendByteBuffer.add(cksum & 0xFF);

        sendBytes(sendByteBuffer);
        //	Log.i("daten" , msg.toString()) ;
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

        //Log.i("int cksum" , "=" + cksum) ;

        sendByteBuffer.add(cksum / 256);
        sendByteBuffer.add(cksum & 0xFF);

        sendBytes(sendByteBuffer);
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

        //Log.i("int cksum" , "=" + cksum) ;

        sendByteBuffer.add(cksum / 256);
        sendByteBuffer.add(cksum & 0xFF);

        sendBytes(sendByteBuffer);
        // inbytes wurde bereits in connect() angelegt
    }

    /**
     * Sendet Lauflicht-Befehl (98,0,0,0,0,0,0,98) – ESP führt Relais 1–n einmal durch.
     * Verbindung muss bereits bestehen.
     */
    public void sendLauflichtBefehl() {
        sendByteBuffer.clear();
        sendByteBuffer.add(98);  // 'b' = Lauflicht
        sendByteBuffer.add(0);
        sendByteBuffer.add(0);
        sendByteBuffer.add(0);
        sendByteBuffer.add(0);
        sendByteBuffer.add(0);
        int cksum = 98;
        sendByteBuffer.add(cksum / 256);
        sendByteBuffer.add(cksum & 0xFF);
        sendBytes(sendByteBuffer);
    }

    private String getStringByteFeld() {
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < sendByteBuffer.size(); i++) {
            sb.append(sendByteBuffer.get(i));
            sb.append(":");
        }

        return sb.toString();
    }

    /**
     * Liest Antwort der Platine blockierend (bis erstes Byte oder timeout).
     * Für Ping-Test nach befehlReadPorts(): Scan-Antwort z. B. "OK 8relais" / "OK 16relais".
     *
     * @param timeoutMs Lese-Timeout in ms
     * @return gelesene Zeichenkette oder leerer String bei Fehler/keiner Antwort
     */
    public String readResponseBlocking(int timeoutMs) {
        if (client == null || inbytes == null) return "";
        try {
            client.setSoTimeout(timeoutMs);
            int first = inbytes.read();
            if (first < 0) return "";
            StringBuilder sb = new StringBuilder();
            sb.append((char) first);
            while (inbytes.available() > 0 && sb.length() < 300) {
                int b = inbytes.read();
                if (b < 0) break;
                sb.append((char) b);
            }
            return sb.toString();
        } catch (IOException e) {
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "readResponseBlocking: " + (e.getMessage() != null ? e.getMessage() : e.toString()));
            }
            disconnect();
            return "";
        }
    }

    public ArrayList<Integer> getSerialBytes() {
        inByteBuffer.clear();
        if (inbytes != null) {
            try {
                input = "";
                while (inbytes.available() > 0) {
                    int inbyte = inbytes.read();
                    if (inbyte < 0) break; // EOF – Verbindung wurde geschlossen, keine weiteren Bytes
                    inByteBuffer.add(inbyte);
                }
                if (!inByteBuffer.isEmpty()) {
                    StringBuilder sb = new StringBuilder();
                    StringBuilder ascii = new StringBuilder();
                    for (int i = 0; i < inByteBuffer.size(); i++) {
                        if (i > 0) sb.append(",");
                        int b = inByteBuffer.get(i);
                        sb.append(b);
                        ascii.append(b >= 32 && b < 127 ? (char) b : '.');
                    }
                    Log.i("carambola", "EMPFANGEN " + inByteBuffer.size() + " Bytes: [" + sb + "] (\"" + ascii + "\")");
                }
            } catch (IOException e) {
                Log.e("carambola", "getSerialBytes Fehler: " + (e.getMessage() != null ? e.getMessage() : e.toString()));
                disconnect();
            }
        }
        return inByteBuffer;
    }

    public void setOutPort(int relais, boolean on) {
        char levelchar = on ? '+' : '-';
        String outString = "/" + makeRelaisString(relais) + levelchar + "/";
        sendData(outString);
    }

    private String makeRelaisString(int relais) {
        String returnString = "0";
        returnString += String.valueOf(relais);
        if (returnString.length() > 2) {
            returnString = returnString.substring(1);
        }
        //Log.i(TAG, "relaisString=" + returnString);
        return returnString;
    }

}
