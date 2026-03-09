package tom.turmtechnik;

import android.content.Context;
import android.net.wifi.WifiManager;
import android.util.Log;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.concurrent.locks.ReentrantLock;

//import android.util.Log;

public class Carambola_IoThread extends Thread {

    private int _platineNummerMinus1;
    /** Alle Platine-Indizes, die dieselbe IP:Port nutzen – Status wird für alle gesetzt (nur eine Verbindung pro Gerät). */
    private int[] _indicesForSameConnection;
    private String _iP;
    private int _port;

    private Carambola_io carambola_serial_io;
    private int localConnectionErrorCount = 0;

    ArrayList<Integer> readSerialList = new ArrayList<Integer>();

    /** Antwort-Bytes über alle Leseversuche hinweg sammeln („impulsok“ kann über mehrere checkAntwortOk()-Aufrufe verteilt ankommen). */
    private final StringBuilder responseAccum = new StringBuilder();

    /** Nach so vielen aufeinanderfolgenden Connect-Fehlern: WLAN kurz aus/an (nur Platine 0, max. alle 2 Min). */
    private static final int CONNECT_ERROR_BEFORE_WIFI_CYCLE = 6;
    private static final long MIN_WIFI_CYCLE_INTERVAL_MS = 2 * 60 * 1000L;
    private static volatile long lastWifiCycleMs = 0;

    /** Nur beim allerersten Connect nach App-Start Relais-Flags dieser Platine auf „aus“ setzen (verhindert Relais 16 beim Start). Bei Wiederverbindung nicht, damit Tastenstellungen erhalten bleiben. */
    private static final boolean[] firstConnectDoneForPlatine = new boolean[4];

    /** Beim Start des Serial_IoThread (App-Start / Neustart WLAN) aufrufen, damit jeder Carambola-Thread beim nächsten Connect „alle aus“ sendet. */
    public static void resetFirstConnectFlags() {
        for (int i = 0; i < firstConnectDoneForPlatine.length; i++) {
            firstConnectDoneForPlatine[i] = false;
        }
    }

    //public static final int portZuIpPlatine1 = 50210 ; // portZuPlatin2 = 50211 ...)

    private final int IO_THREAD_SLEEP_TIME = 10; // kürzer = schnellere Tastenreaktion (vorher 20 ms)
    private int checkChangeCount = 0;

    /** Zeitpunkt (elapsedRealtime) des letzten Sends – Antwort muss innerhalb RESPONSE_TIMEOUT_MS kommen. */
    private long lastSendTimeMs = 0;
    /** Wann zuletzt „impulsok“ (oder erwartete Antwort) empfangen wurde – für Mindestabstand bis zum nächsten periodischen Send. */
    private long lastSuccessfulResponseTimeMs = 0;
    /** Antwort muss innerhalb dieser Zeit kommen, sonst Verbindungsfehler. Kurz (500 ms) – dann wird öfter wiederholt, es geht nichts verloren. */
    private static final long RESPONSE_TIMEOUT_MS = 500;
    /** Nach Empfang der Antwort mindestens so lange warten, bis wieder periodisch gesendet wird (nicht sofort wieder senden – ESP entlasten). Bei Tastendruck (Relais-Änderung) wird trotzdem sofort gesendet. */
    private static final long MIN_SEND_INTERVAL_AFTER_RESPONSE_MS = 180;
    /** Wenn sich der Relais-Zustand nicht geändert hat: mindestens alle 1 s periodisch senden (Keepalive – ESP schaltet sonst ab). */
    private static final long PERIODIC_SEND_WHEN_UNCHANGED_MS = 1000;

//	  private final int minimalCheckTime = 20 ; // minimal alle 400 ms PIC ansprechen
//	  private final int minimalCheckTime = 40 ; // minimal alle 800 ms PIC ansprechen 5.6.13
//	  private final int minimalCheckTime = 50 ; // minimal alle 1000 ms PIC ansprechen 5.6.13

    /** Anzahl Schleifendurchläufe (à IO_THREAD_SLEEP_TIME ms), nach der periodisch gesendet wird. scanRelaisMS in echte Zeit umrechnen. */
    private int getMinimalCheckTime() {
        int ms = StaticVariable.scanRelaisMS;
        if (ms < 10) ms = 10;
        return ms / IO_THREAD_SLEEP_TIME;
    }

    private int[] sendByteBuffer = {0, 0, 0, 0, 1,}; // erstes byte = port a
    // dann port b, c, d, und pic nummer
    // muss man umgekehrt schicken !!
    // als erstes byte 4 (das 5te) -- Pic Nummer
    // 12.2.13 -- mit Thomas gesprochen
    // doch keine pic nummer --> reserve
    // vorlauefig immer 1
    // dann port d, c, b, a !!
//	  private int portA ;
//	  private int portB ;
//	  private int portC ;
//	  private int portD ;
//	  private int picNumber ;


    //private static  Boolean[] relaisNew = new Boolean[TurmtechnikActivity.RELAIS_COUNT];
    //private Boolean[] relaisOld = new Boolean[TurmtechnikActivity.RELAIS_COUNT];

    private boolean etwasGesendetCarambola = false;

    /** Lock für diese Platine (IP:Port), gehalten von Send bis Antwort/Timeout – verhindert, dass Web-Ping/Test und I/O-Thread sich die Antwort wegnehmen. */
    private ReentrantLock connectionLockHeld = null;

//	  AvrNetIo avr1=new AvrNetIo("192.168.1.51",50290);

//	  AvrNetIo avr1=new AvrNetIo("192.168.1.51",2000); // test fuer Thomas, 30.11.2012

//	  AvrNetIo avr2=new AvrNetIo("192.168.1.52",50290);

    // achtung avr2 gesperrt !!! 2.10.12

    /** @param platineNummer Index für Relais-Offset (0–3); bei mehreren Platinen mit gleicher IP nur einer pro Verbindung. */
    public Carambola_IoThread(int platineNummer, String IP, int port) {
        _platineNummerMinus1 = platineNummer;
        _indicesForSameConnection = new int[] { platineNummer };
        _iP = IP;
        _port = port;
    }

    /** Eine Verbindung für mehrere Einträge in ipList mit gleicher IP:Port (nur ein Thread pro Gerät). */
    public Carambola_IoThread(int primaryPlatineIndex, int[] indicesSharingThisConnection, String IP, int port) {
        _platineNummerMinus1 = primaryPlatineIndex;
        _indicesForSameConnection = indicesSharingThisConnection != null && indicesSharingThisConnection.length > 0
                ? indicesSharingThisConnection : new int[] { primaryPlatineIndex };
        _iP = IP;
        _port = port;
    }


    public void run() {
        carambola_serial_io = new Carambola_io();

        initRelaisFlags();

        while (StaticVariable.serial_io_ThreadsRun) {
            //incrementCarambolaNumberIndex();

            checkChange(_platineNummerMinus1);

            checkAntwort();  // zuerst auf Antowrt von Relais Platine achten, probleme mit WIFI Nebenuhr , 6.02.17

            sleepTime(IO_THREAD_SLEEP_TIME); // pruefe alle 20 ms aenderung
        }
    }


    /** Setzt Verbindungsstatus für alle Indizes; bei Fehler (ok=false) wird lastCarambolaErrorTimeMs gesetzt. */
    private void setStatusForAllIndices(boolean ok) {
        setStatusForAllIndices(ok, true);
    }

    /**
     * @param setErrorTimestamp bei false nur serial_io_status4 setzen, lastCarambolaErrorTimeMs nicht setzen
     *                          (verhindert, dass Nebenuhr während des Wartens auf Antwort fälschlich "Carambola-Fehler" sieht)
     * Bei ok=true: lastCarambolaErrorTimeMs NICHT zurücksetzen – sonst überschreibt ein anderer Carambola-Thread
     * (der „impulsok“ bekommt) den Timeout eines Threads, und die Nebenuhr sieht „Impuls Fehler: Nein“ trotz Logcat-Timeout.
     */
    private void setStatusForAllIndices(boolean ok, boolean setErrorTimestamp) {
        long t = android.os.SystemClock.elapsedRealtime();
        for (int i = 0; i < _indicesForSameConnection.length; i++) {
            int idx = _indicesForSameConnection[i];
            if (idx >= 0 && idx < 4) {
                StaticVariable.serial_io_status4[idx] = ok;
                if (ok) {
                    // Nicht lastCarambolaErrorTimeMs = 0 – sonst Race: anderer Thread bekommt Antwort, überschreibt Timeout dieses Threads
                } else if (setErrorTimestamp) {
                    StaticVariable.lastCarambolaErrorTimeMs[idx] = t;
                }
            }
        }
    }

    /**
     * Prüft nur beim Polling: Antwort eingetroffen oder Timeout.
     * Kein blockierendes Warten – weniger Verkehr, Nebenuhr sieht während des Wartens keinen Fehler.
     */
    private void checkAntwort() {
        if (!etwasGesendetCarambola) return;

        try {
            long now = android.os.SystemClock.elapsedRealtime();
            if (now - lastSendTimeMs > RESPONSE_TIMEOUT_MS) {
                sleepTime(80);
                if (tryReadResponse()) {
                    lastSuccessfulResponseTimeMs = android.os.SystemClock.elapsedRealtime();
                    setStatusForAllIndices(true);
                    releaseConnectionLock();
                    etwasGesendetCarambola = false;
                    if (carambola_serial_io != null) {
                        carambola_serial_io.disconnect();
                    }
                    return;
                }
                Log.e("carambola", "Keine Antwort Platine=" + _platineNummerMinus1 + " innerhalb " + RESPONSE_TIMEOUT_MS + " ms");
                // Immer Fehlerzeit setzen, damit die Nebenuhr „Impuls Fehler: Ja“ loggt und den Impuls nicht als erfolgreich zählt
                // (vorher wurde bei laufendem Impuls kein Fehler gesetzt → Log zeigte fälschlich „Impuls Fehler: Nein“ trotz Timeout)
                setStatusForAllIndices(false, true);
                releaseConnectionLock();
                etwasGesendetCarambola = false;
                responseAccum.setLength(0);
                if (carambola_serial_io != null) {
                    carambola_serial_io.disconnect();
                }
                sleepTime(200);
                return;
            }

            if (tryReadResponse()) {
                lastSuccessfulResponseTimeMs = android.os.SystemClock.elapsedRealtime();
                setStatusForAllIndices(true);
                releaseConnectionLock();
                etwasGesendetCarambola = false;
                if (carambola_serial_io != null) {
                    carambola_serial_io.disconnect();
                }
            }
        } catch (Throwable t) {
            // Nur bei Exception Lock/Flag freigeben – nicht bei normalem Durchlauf, sonst wird sofort erneut gesendet (Doppel-SEND)
            if (connectionLockHeld != null) {
                releaseConnectionLock();
                etwasGesendetCarambola = false;
            }
            android.util.Log.e("carambola", "checkAntwort Exception: " + (t.getMessage() != null ? t.getMessage() : t.toString()), t);
        }
    }

    /** Lock für diese Platine freigeben (nach Antwort oder Timeout). */
    private void releaseConnectionLock() {
        if (connectionLockHeld != null) {
            try {
                connectionLockHeld.unlock();
            } catch (Exception ignored) { }
            connectionLockHeld = null;
        }
    }

    /** Kurzes, nicht blockierendes Lesen (1–2 Leseversuche, je ~5 ms) – Antwort wird über mehrere Schleifendurchläufe eingesammelt. */
    private boolean tryReadResponse() {
        if (carambola_serial_io == null) return false;
        String expected = DEFAULT_IMPULS_ANTWORT;
        try {
            android.content.Context runtimeContext = TurmtechnikActivity.getRuntimeContext();
            if (runtimeContext != null) {
                Platine platine = PlatinenDatabaseHelper.getInstance(runtimeContext).getPlatine(_platineNummerMinus1 + 1);
                if (platine != null && platine.impulsAntwortErwartet != null && !platine.impulsAntwortErwartet.trim().isEmpty()) {
                    expected = platine.impulsAntwortErwartet.trim();
                }
            }
        } catch (Exception e) {
            android.util.Log.w("Carambola_IoThread", "Platine-Konfiguration nicht geladen, nutze " + DEFAULT_IMPULS_ANTWORT, e);
        }

        for (int attempt = 0; attempt < 2; attempt++) {
            if (attempt > 0) sleepTime(5);
            readSerialList.clear();
            readSerialList = carambola_serial_io.getSerialBytes();
            if (!readSerialList.isEmpty()) {
                for (int i = 0; i < readSerialList.size(); i++) {
                    responseAccum.append((char) (int) readSerialList.get(i));
                }
                if (responseAccum.length() > 500) {
                    responseAccum.delete(0, responseAccum.length() - 200);
                }
                String response = responseAccum.toString();
                String responseLower = response.toLowerCase();
                String expectedLower = expected.toLowerCase();
                // Case-insensitiv prüfen; zusätzlich jede Antwort mit „impulsok“ akzeptieren (z. B. „OK 8relaisimpulsok“)
                boolean ok = responseLower.contains(expectedLower) || responseLower.contains("impulsok");
                if (ok) {
                    long elapsed = android.os.SystemClock.elapsedRealtime() - lastSendTimeMs;
                    Log.e("carambola", "Antwort O.K. (" + expected + ") Platine=" + _platineNummerMinus1 + " nach " + elapsed + " ms");
                    responseAccum.setLength(0);
                    return true;
                }
            }
        }
        return false;
    }

    /** Wie Serial_IoThread: relaisOld = relaisNew = false, damit beim Start keine Relais-Flanke gesendet wird (Nebenuhr würde sonst ungezählt vorrücken). */
    private void initRelaisFlags() {
        for (int i = 0; i < TurmtechnikActivity.RELAIS_COUNT; i++) {
            Serial_IoThread.relaisOld[i] = false;
            Serial_IoThread.relaisNew[i] = false;
        }
    }

    private int[] bitMask = {0x01, 0x02, 0x04, 0x08, 0x10, 0x20, 0x40, 0x80,};

    private void convertFlagsToBits(int platineNumberMinusEins) {
        sendByteBuffer[0] = 0;
        sendByteBuffer[1] = 0;
        sendByteBuffer[2] = 0;
        sendByteBuffer[3] = 0;
        int platinenNumberOffset = platineNumberMinusEins * (4 * 8);  // offset 32 Relais
        //for(int i = 0 ; i < TurmtechnikActivity.RELAIS_COUNT ; i++)
        for (int i = 0; i < (4 * 8); i++) // immer 4 bytes = 32 Relais werden gesendet
        {
            int mask = bitMask[i % 8];
            //	 Log.i("bitMask" , "=" + Integer.toHexString(mask)) ;
            int byteIndex = i / 8;
            //	 Log.i("byteIndex" , "=" + byteIndex ) ;
            if (Serial_IoThread.relaisNew[i + platinenNumberOffset] == true) {
                sendByteBuffer[byteIndex] = sendByteBuffer[byteIndex] | mask;
            } else {
                sendByteBuffer[byteIndex] = sendByteBuffer[byteIndex] & (~mask);
            }

        }

//		for(int i2 = 0 ; i2 < sendByteBuffer.length ; i2++)
//	    {
//			 Log.i("SendByteBuffer" , "=" + Integer.toHexString(sendByteBuffer[i2])) ;
//	    }
    }

    private void checkChange(int carambola_number) {
        carambola_number = carambola_number * 32;
        boolean relaisChanged = false;

        //for (int i = 0; i < 32; i++)
        for (int i = carambola_number ; i < (carambola_number+32); i ++)
        {
            //if (Serial_IoThread.relaisOld[i + carambola_number] != Serial_IoThread.relaisNew[i + carambola_number]) {
           //     relaisChanged = true;
            //    Serial_IoThread.relaisOld[i + carambola_number] = Serial_IoThread.relaisNew[i + carambola_number];
                //		//doRelais(i+1,relaisNew[i]);
            //    checkChangeCount = 0;
            //}
            if (Serial_IoThread.relaisOld[i ]  != Serial_IoThread.relaisNew[i ])
            {
                relaisChanged = true;
                // Erst relaisOld aktualisieren und senden, wenn keine Antwort mehr aussteht – sonst
                // würde z. B. Nebenuhr-Impuls (1 s EIN) nach 1 s AUS senden, bevor „impulsok“ kam, und Impulse gingen verloren.
                if (!etwasGesendetCarambola) {
                    Serial_IoThread.relaisOld[i ] = Serial_IoThread.relaisNew[i ];
                }
                checkChangeCount = 0;
            }

        }

        if (relaisChanged) // hat sich der Zustand von einem oder mehrern Relais geaendert?
        {
            // Nur senden, wenn keine Antwort vom vorherigen Send aussteht (Nebenuhr: erst nach „impulsok“ nächsten Zustand senden).
            if (!etwasGesendetCarambola) {
                Serial_IoThread.logRelaisBinaryState();
                sendAllRelais();
                LogTurmtechnik2 logTemp =
                        new LogTurmtechnik2("Automatic", sendByteBuffer[0], sendByteBuffer[1], sendByteBuffer[2], sendByteBuffer[3]);
                logTemp = null;
            }
        } else {
            checkChangeCount++;
            // Nur periodisch senden, wenn kein Send aussteht und seit letzter Antwort genug Zeit (bei unverändertem Zustand mind. 2 s, damit ESP/Nebenuhr nicht überflutet werden)
            long now = android.os.SystemClock.elapsedRealtime();
            long msSinceLastResponse = now - lastSuccessfulResponseTimeMs;
            boolean minIntervalElapsed = msSinceLastResponse >= MIN_SEND_INTERVAL_AFTER_RESPONSE_MS;
            boolean periodicIntervalElapsed = msSinceLastResponse >= PERIODIC_SEND_WHEN_UNCHANGED_MS;
            if (checkChangeCount > getMinimalCheckTime() && !etwasGesendetCarambola && minIntervalElapsed && periodicIntervalElapsed) {
                sendAllRelais();
                checkChangeCount = 0;
            }
        }
    }

    private void sendAllRelais() {
        // Log.e("ACHTUNG!" , "sende alle relais") ;
        // hier fuer carambola richtiges offset fuer weitere ip adressen einbauen !!!!!!!!!

        // Demo-Modus: gleicher Ablauf wie Normalbetrieb, nur ESP-Antwort als „erhalten“ setzen (keine echte Verbindung)
        if (StaticVariable.platinenDemoModus) {
            setStatusForAllIndices(true);
            lastSuccessfulResponseTimeMs = android.os.SystemClock.elapsedRealtime();
            return;
        }

        // Wir werden nur aufgerufen, wenn keine Antwort mehr aussteht (!etwasGesendetCarambola) – kein blockierendes Lesen hier
        convertFlagsToBits(_platineNummerMinus1); // flag buffer auf sende bytes(bites) konvertieren
        // Exklusiv-Zugriff auf diese Platine: verhindert, dass ein anderer Thread (z. B. Web-Ping) gleichzeitig connect/send/read macht und die Antwort „wegnimmt“
        connectionLockHeld = Carambola_io.getLockFor(_iP, _port);
        connectionLockHeld.lock();
        try {
            boolean wasAlreadyConnected = carambola_serial_io.isConnected();
            boolean connectOk = wasAlreadyConnected;
            final int CONNECT_RETRIES = 3;
            final int CONNECT_RETRY_DELAY_MS = 300;

            if (!connectOk) {
                // Bei vielen Fehlern kurz pausieren, damit ESP/Netzwerk nicht überlastet werden
                if (localConnectionErrorCount > 3) {
                    sleepTime(2000);
                }
                carambola_serial_io.disconnect();
                for (int r = 0; r < CONNECT_RETRIES && !connectOk; r++) {
                    if (r > 0) {
                        sleepTime(CONNECT_RETRY_DELAY_MS);
                    }
                    connectOk = carambola_serial_io.connect(_iP, _port);
                }
            }

            if (connectOk) {
                localConnectionErrorCount = 0;
                if (!wasAlreadyConnected) {
                    Log.e("carambola", "connect O.K. Platine=" + _platineNummerMinus1);
                    // Nur beim allerersten Connect: „alle aus“ senden und Relais-Flags zurücksetzen.
                    if (_platineNummerMinus1 >= 0 && _platineNummerMinus1 < firstConnectDoneForPlatine.length && !firstConnectDoneForPlatine[_platineNummerMinus1]) {
                        for (int idx : _indicesForSameConnection) {
                            if (idx >= 0 && idx < firstConnectDoneForPlatine.length) firstConnectDoneForPlatine[idx] = true;
                        }
                        sendByteBuffer[0] = 0;
                        sendByteBuffer[1] = 0;
                        sendByteBuffer[2] = 0;
                        sendByteBuffer[3] = 0;
                        int offset = _platineNummerMinus1 * 32;
                        for (int i = offset; i < offset + 32 && i < TurmtechnikActivity.RELAIS_COUNT; i++) {
                            Serial_IoThread.relaisOld[i] = false;
                            Serial_IoThread.relaisNew[i] = false;
                        }
                        if (offset <= 15 && 15 < offset + 32) {
                            Serial_IoThread.relaisNew[15] = false;
                            Serial_IoThread.relaisOld[15] = false;
                        }
                    }
                }
                carambola_serial_io.send5Bytes(sendByteBuffer);
                responseAccum.setLength(0);
                lastSendTimeMs = android.os.SystemClock.elapsedRealtime();
                etwasGesendetCarambola = true;
            } else {
                Log.e("carambola", "connect Error nach " + CONNECT_RETRIES + " Versuchen Platine=" + _platineNummerMinus1);
                localConnectionErrorCount++;
                setStatusForAllIndices(false);
                if (_platineNummerMinus1 == 0 && !StaticVariable.bt_io_ok
                        && localConnectionErrorCount >= CONNECT_ERROR_BEFORE_WIFI_CYCLE) {
                    long now = android.os.SystemClock.elapsedRealtime();
                    if (now - lastWifiCycleMs >= MIN_WIFI_CYCLE_INTERVAL_MS) {
                        lastWifiCycleMs = now;
                        localConnectionErrorCount = 0;
                        cycleWifiOffOn();
                    }
                }
            }
        } finally {
            // Lock nur freigeben, wenn wir nicht auf Antwort warten (sonst gibt checkAntwort() frei)
            if (!etwasGesendetCarambola && connectionLockHeld != null) {
                releaseConnectionLock();
            }
        }
    }

    /** WLAN kurz aus- und wieder einschalten, wenn Verbindung längere Zeit nicht zustande kommt (nur im WLAN-Modus). */
    private void cycleWifiOffOn() {
        Context ctx = TurmtechnikActivity.getRuntimeContext();
        if (ctx == null) return;
        try {
            WifiManager wm = (WifiManager) ctx.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            if (wm == null) return;
            Log.e("carambola", "Keine Verbindung – WLAN wird kurz aus- und wieder eingeschaltet (Platine 0)");
            wm.setWifiEnabled(false);
            sleepTime(4000);
            wm.setWifiEnabled(true);
            sleepTime(8000);
            Log.e("carambola", "WLAN wieder eingeschaltet, Verbindung wird erneut versucht");
        } catch (Exception e) {
            Log.e("carambola", "Fehler beim WLAN-Zyklus: " + (e.getMessage() != null ? e.getMessage() : e.toString()));
        }
    }

    /** Pro Aufruf nur kurz warten (nicht 400+ ms), damit die Schleife im Takt bleibt – Scan-Relais 100 ms kann so eingehalten werden wie bei Bluetooth. Antwort wird über mehrere Durchläufe „eingesammelt“. */
    private static final int ANTWORT_READ_ATTEMPTS = 4;
    private static final int ANTWORT_READ_DELAY_MS = 15;

    /** Nach so vielen Durchläufen ohne gültige Antwort aufgeben. Ausreichend hoch (z. B. 40 ≈ 2,5 s), damit bei hoher WLAN-Latenz (Ping 2–90 ms) die Antwort noch ankommt. */
    /** Höher = toleranter bei langsamer Antwort (z. B. während Nebenuhr-Impuls). 100 × 10 ms ≈ 1 s. */
    private static final int ANTWORT_MAX_VERSUCHE = 100;

    /** Standard-Antwort des ESP nach Schalten; ESP kann immer dieselbe Antwort senden (evtl. mit weiterem Text danach). */
    private static final String DEFAULT_IMPULS_ANTWORT = "impulsok";

    private boolean checkAntwortOk() {
        if (!etwasGesendetCarambola) {
            return true;
        }

        readSerialList.clear();
        boolean serialIoStatus = false;
        long startMs = android.os.SystemClock.elapsedRealtime();

        if (carambola_serial_io != null) {
            String expected = DEFAULT_IMPULS_ANTWORT;
            try {
                android.content.Context runtimeContext = TurmtechnikActivity.getRuntimeContext();
                if (runtimeContext != null) {
                    Platine platine = PlatinenDatabaseHelper.getInstance(runtimeContext).getPlatine(_platineNummerMinus1 + 1);
                    if (platine != null && platine.impulsAntwortErwartet != null && !platine.impulsAntwortErwartet.trim().isEmpty()) {
                        expected = platine.impulsAntwortErwartet.trim();
                    }
                }
            } catch (Exception e) {
                android.util.Log.w("Carambola_IoThread", "Platine-Konfiguration nicht geladen, nutze " + DEFAULT_IMPULS_ANTWORT, e);
            }

            for (int attempt = 0; attempt < ANTWORT_READ_ATTEMPTS && !serialIoStatus; attempt++) {
                if (attempt > 0) {
                    sleepTime(ANTWORT_READ_DELAY_MS);
                }
                readSerialList = carambola_serial_io.getSerialBytes();
                if (!readSerialList.isEmpty()) {
                    int lenBefore = responseAccum.length();
                    for (int i = 0; i < readSerialList.size(); i++) {
                        responseAccum.append((char) (int) readSerialList.get(i));
                    }
                    if (responseAccum.length() > 500) {
                        responseAccum.delete(0, responseAccum.length() - 200);
                    }
                    String response = responseAccum.toString();
                    String responseLower = response.toLowerCase();
                    String expectedLower = expected.toLowerCase();
                    if (responseLower.contains(expectedLower) || responseLower.contains("impulsok")) {
                        serialIoStatus = true;
                        long elapsed = android.os.SystemClock.elapsedRealtime() - startMs;
                        Log.e("carambola", "Antwort O.K. (" + expected + ") Platine=" + _platineNummerMinus1 + " nach " + elapsed + " ms");
                        responseAccum.setLength(0);
                    }
                }
            }
        }

        return serialIoStatus;
    }

	/*
    public static void changeRelais(int relais_number, boolean flag)
	{
		relais_number=Math.max(relais_number, 1);
		relais_number=Math.min(relais_number, TurmtechnikActivity.RELAIS_COUNT);
		// Log.d("relais_number" , "" + relais_number); 
		Serial_IoThread.relaisNew[relais_number-1]=flag ;
	}
	*/

    private void sleepTime(long time) {
        try {
            Thread.sleep(time);
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

}
