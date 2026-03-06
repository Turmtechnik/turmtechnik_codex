package tom.turmtechnik;

import java.util.ArrayList;

import android.util.Log;

public class Serial_IoThread extends Thread {

    boolean antwortOk = false;
    boolean etwasGesendetCarambola = false;

    ArrayList<Integer> readSerialList = new ArrayList<Integer>();

    //public static final int portZuIpPlatine1 = 50210 ; // portZuPlatin2 = 50211 ...)

    private int checkChangeCount = 0;
    private final int IO_THREAD_SLEEP_TIME = 30; // check alle 50 ms --> 18.3.13 alle 20 ms

    private int antwortVersuche = 0;

//	  private final int minimalCheckTime = 20 ; // minimal alle 400 ms PIC ansprechen
//	  private final int minimalCheckTime = 40 ; // minimal alle 800 ms PIC ansprechen 5.6.13
//	  private final int minimalCheckTime = 50 ; // minimal alle 1000 ms PIC ansprechen 5.6.13

    private final int minimalCheckTime = (StaticVariable.scanRelaisMS / 20);

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


    private BluetoothSerial_io bt_serial_io;

    //bis zu 4 eigene Kanäle abhaengig von ip list size
    //private Carambola_io  carambola_serial_io ;
    //private ArrayList<Carambola_io> carambola_serial_io_list = new ArrayList<Carambola_io>() ;
    private Thread[] carambolaThreads = new Thread[4]; // maximal 4 threads


    private int carambola_number_index = 0;

//	  final int ausgaenge = 8 ;

//	  boolean avrconnect = false; 

    //private boolean doRun = true ;

    public static Boolean[] relaisNew = new Boolean[TurmtechnikActivity.RELAIS_COUNT];
    public static Boolean[] relaisOld = new Boolean[TurmtechnikActivity.RELAIS_COUNT];
    //private int localConnectionErrorCount = 0 ;

//	  AvrNetIo avr1=new AvrNetIo("192.168.1.51",50290);

//	  AvrNetIo avr1=new AvrNetIo("192.168.1.51",2000); // test fuer Thomas, 30.11.2012

//	  AvrNetIo avr2=new AvrNetIo("192.168.1.52",50290);

    // achtung avr2 gesperrt !!! 2.10.12


    public void run() {
        StaticVariable.serial_io_ThreadsRun = true;

        // Relais-Flags vor dem Start der Carambola-Threads auf „alle aus“, damit beim App-Start nicht Relais 16 o.ä. versehentlich gesendet wird
        initRelaisFlags();
        // Relais 16 (Index 15) explizit aus – verhindert bekanntes Problem „Relais 16 beim Start an“
        relaisNew[15] = false;
        relaisOld[15] = false;

        // Demo-Modus: gleicher Ablauf wie Normalbetrieb, nur Antwort der Platinen als „erhalten“ setzen (keine echte Hardware)
        if (StaticVariable.platinenDemoModus && StaticVariable.bt_io_ok) {
            android.util.Log.i("Serial_IoThread", "Platinen-Demo-Modus (Bluetooth-Pfad): Antwort wird simuliert");
            StaticVariable.serial_io_status4[0] = true;
            StaticVariable.errorCountToReboot = 0;
            while (StaticVariable.serial_io_ThreadsRun) {
                checkChange(0);
                sleepTime(IO_THREAD_SLEEP_TIME);
            }
            return;
        }

        if (StaticVariable.bt_io_ok) {
            bt_serial_io = new BluetoothSerial_io();
            bt_serial_io.connect();
            // Beim Start sofort „alle aus“ an die Hardware senden (verhindert Relais 16 vom vorherigen Zustand)
            sendAllRelais();
        } else {
            // Beim Start der WLAN-Threads First-Connect-Flags zurücksetzen
            Carambola_IoThread.resetFirstConnectFlags();
            // Nur ein Thread pro eindeutige IP:Port (mehrere Einträge mit gleicher IP = ein Gerät → verhindert „Keine Antwort“ durch konkurrierende Verbindungen)
            // ipList/portList müssen gleiche Größe haben, sonst IndexOutOfBoundsException vermeiden
            int listSize = (StaticVariable.ipList != null && StaticVariable.portList != null)
                    ? Math.min(StaticVariable.ipList.size(), StaticVariable.portList.size()) : 0;
            java.util.ArrayList<Integer> started = new java.util.ArrayList<>();
            int threadIdx = 0;
            for (int i = 0; i < listSize && threadIdx < carambolaThreads.length; i++) {
                if (started.contains(i)) continue;
                String ip = StaticVariable.ipList.get(i);
                int port = StaticVariable.portList.get(i);
                java.util.ArrayList<Integer> indicesSame = new java.util.ArrayList<>();
                for (int j = 0; j < listSize; j++) {
                    if (ip.equals(StaticVariable.ipList.get(j)) && port == StaticVariable.portList.get(j)) {
                        indicesSame.add(j);
                        started.add(j);
                    }
                }
                int[] arr = new int[indicesSame.size()];
                for (int k = 0; k < indicesSame.size(); k++) arr[k] = indicesSame.get(k);
                StaticVariable.serial_io_ThreadsRun = true;
                carambolaThreads[threadIdx] = new Carambola_IoThread(i, arr, ip, port);
                carambolaThreads[threadIdx].start();
                threadIdx++;
            }
        }

        if (StaticVariable.bt_io_ok) {
            while (StaticVariable.serial_io_ThreadsRun) {
                //Log.e("blue" , "lauft") ;
                //incrementCarambolaNumberIndex();
                //checkChange(carambola_number_index);
                checkChange(0);
                sleepTime(IO_THREAD_SLEEP_TIME); // pruefe alle 20 ms aenderung
            }

            bt_serial_io.disconnect(); // neu 4.11.15
            bt_serial_io = null;

        } // neu 8.7.15
        // bei Carambola endet dieser Thread
        // jede ip Adresse hat seinen eigenen thread
    }

    //public void endSerialThread()
    //{
    //	doRun = false ;
    //}


    /*
    private void incrementCarambolaNumberIndex()
	{

		if(StaticVariable.bt_io_ok)
		{
			carambola_number_index = 0 ;
		}
		else
		{

			if(etwasGesendetCarambola)
			{
				boolean temp = checkAntwortOk() ;
				if(temp)
				{
					StaticVariable.serial_io_status = true ;
					antwortVersuche = 0 ;
					etwasGesendetCarambola = false ;
				}
				else
				{
					antwortVersuche ++ ;
					if(antwortVersuche > 20 ) // 1000 ms keine Antwort
					{
						Log.e("carambola" , "antwort versuche=" + antwortVersuche) ;
						StaticVariable.serial_io_status = false;
						etwasGesendetCarambola = false;
						antwortVersuche = 0 ;
					}
				}
			}
			else
			{
				carambola_number_index++;
				if (carambola_number_index >= StaticVariable.ipList.size()) {
					carambola_number_index = 0;
				}
			}
		}	
	}
	*/

    /**
     * Relais-Flags für den IO-Thread initialisieren.
     * relaisOld = relaisNew = false, damit beim Start KEINE Relais-Änderung gesendet wird.
     * Sonst würde „alle aus“ (relaisOld=true → relaisNew=false) an die Hardware gesendet;
     * bei Nebenuhr-Relais kann diese Flanke die Zeiger vorrücken, ohne dass die App den Impuls zählt („Uhr geht voraus“ nach Neustart).
     */
    private void initRelaisFlags() {
        for (int i = 0; i < TurmtechnikActivity.RELAIS_COUNT; i++) {
            relaisOld[i] = false;
            relaisNew[i] = false;
        }
        // Relais 16 (Index 15) explizit aus – bekanntes Problem „Relais 16 beim Start an“
        if (TurmtechnikActivity.RELAIS_COUNT > 15) {
            relaisNew[15] = false;
            relaisOld[15] = false;
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
            if (relaisNew[i + platinenNumberOffset] == true) {
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

        for (int i = 0; i < 32; i++) {
            if (relaisOld[i + carambola_number] != relaisNew[i + carambola_number]) {
                relaisChanged = true;
                relaisOld[i + carambola_number] = relaisNew[i + carambola_number];
                //		//doRelais(i+1,relaisNew[i]);
                checkChangeCount = 0;
            }
        }

        if (relaisChanged) // hat sich der Zustand von einem oder mehrern Relais geaendert?
        {
            // Binäre Ausgabe des Relais-Zustands
            logRelaisBinaryState();
            
            sendAllRelais();
            if (StaticVariable.logOnOff) {
                LogTurmtechnik2 logTemp =
                        new LogTurmtechnik2("Automatic", sendByteBuffer[0], sendByteBuffer[1], sendByteBuffer[2], sendByteBuffer[3]);
                logTemp = null;
            }
            if ((StaticVariable.bt_io_ok == false) && (StaticVariable.ipList.size()) > 1) {
                for (int i = 0; i < StaticVariable.ipList.size(); i++) {
                    //incrementCarambolaNumberIndex(); // neu 23.06.15 damit auch die anderen geschickt werden
                    sendAllRelais();
                }

            }
        } else {
            checkChangeCount++;
            if (checkChangeCount > minimalCheckTime) {
                sendAllRelais();
                checkChangeCount = 0;
            }
        }
    }

    //public boolean checkFinish()
    //{
    //	boolean finish = true ;

    //	for (int i = 0 ; i < 16 ; i++)
    //	{
    //		if  (relaisOld[i] != relaisNew[i])
    //		{
    //			finish = false ;
    //		}
    //	}
    //	return finish;
    //}

/*	
	private void doRelais(int relais_number, boolean flag )
	{
		//Log.i("rel:"+relais_number , "flag:" + flag);
		if  (relais_number < 9)  // 1. Avr board? 
		{
			avr1.connect();
			avr1.setOutPort(relais_number, flag);
			avr1.disconnect(); 
		}
		else
		{
	//		avr2.connect();
	//		avr2.setOutPort((relais_number-8), flag);
	//		avr2.disconnect();
		}
	}
*/


    private void sendAllRelais() {
        // Log.e("ACHTUNG!" , "sende alle relais") ;
        // hier fuer carambola richtiges offset fuer weitere ip adressen einbauen !!!!!!!!!

        if (StaticVariable.platinenDemoModus) {
            StaticVariable.serial_io_status4[0] = true;
            StaticVariable.errorCountToReboot = 0;
            return;
        }
        if (StaticVariable.bt_io_ok == true) {
            convertFlagsToBits(0); // im bluetooth modus nur die ersten 32 Relais schicken
            bt_serial_io.send5Bytes(sendByteBuffer);
            //Log.e("blue" , "send") ;

            bt_serial_io.befehlReadPorts();

            for (int i = 0; i < 5; i++) {
                sleepTime(100);
                int serialTemp = 0;
                serialTemp = bt_serial_io.available();
                if (serialTemp > 0) {
                    //Log.e("blue" , "aviable=" + serialTemp + " index i=" + i) ;
                    break;
                }
                //Log.e("blue" , "aviable=" + serialTemp + "i ndex i=" + i) ;
            }

            //sleepTime(200); // 100 ms zeit fuer Antwort
            // email von thomas .. schlaegt zur stunde 6 mal statt 4 mal
            readSerialList.clear();
            readSerialList = bt_serial_io.getSerialBytes();
//			// vorlaeufig nur schauen ob irgendwas gekommen ist !!!!
            if (readSerialList.size() > 0) {
                StaticVariable.serial_io_status4[0] = true;
                StaticVariable.errorCountToReboot = 0;
            } else {
                StaticVariable.errorCountToReboot++;
                StaticVariable.serial_io_status4[0] = false;
                bt_serial_io.disconnect();
                sleepTime(500);
                bt_serial_io.connect();
                sleepTime(10000);
            }

            //Log.e("blue" , "status=" + StaticVariable.serial_io_status4[0]) ;

            // noch irgendwie echo verarbeiten!!!
        }

        /* ab 8.7.15 fuer carambola eigene threads
		else 
		{
			// hier carambola schicken!  jedes Relais hat eventuell andere ip adresse???!!!
			// noch irgendwie echo verarbeiten

			checkAntwortOk() ;

			convertFlagsToBits(carambola_number_index); // flag buffer auf sende bytes(bites) konvertieren
			boolean connectOk = false ;

			if(carambola_serial_io_list.size() > 0)
			{


				if (carambola_serial_io_list.get(carambola_number_index) != null) {

					//antwortOk = checkAntortOk() ;

					//carambola_serial_io.disconnect(); // 11.06.15 zuerst alte verbindun beenden

					connectOk = carambola_serial_io_list.get(carambola_number_index)
							.connect(StaticVariable.ipList.get(carambola_number_index));
				}
			}
			if(connectOk)
			{
				localConnectionErrorCount = 0 ;
				Log.e("carambola", "connect O.K.") ;
				carambola_serial_io_list.get(carambola_number_index).send5Bytes(sendByteBuffer) ;
				carambola_serial_io_list.get(carambola_number_index).befehlReadPorts();

				etwasGesendetCarambola = true ;
						//sleepTime(100) ;
						//checkAntwortOk() ;
				        //etwasGesendetCarambola = false ;

				//carambola_serial_io_list.get(carambola_number_index).befehlReadPorts();
				//sleepTime(500); // 100 ms zeit fuer Antwort
				//readSerialList.clear();
				//readSerialList = carambola_serial_io_list.get(carambola_number_index).getSerialBytes();
				// vorlaeufig nur schauen ob irgendwas gekommen ist !!!!
				//if(readSerialList.size()>0)
				//{
				//	Log.e("carambola" , "Antwort O.K.") ;
				//	StaticVariable.serial_io_status = true ;
				//}
				//else
				//{
				//	Log.e("carambola" , "keine Antwort") ;
				//	StaticVariable.serial_io_status = false ;
				//	//StaticVariable.serial_io_status = true ;
                //    carambola_serial_io_list.get(carambola_number_index).disconnect() ;
				//}
			

			}
			else 
			{
				Log.e("carambola" , "connect Error") ;
				localConnectionErrorCount ++ ;
				if(localConnectionErrorCount > 5)
				{
					StaticVariable.serial_io_status = false;
				}
				//StaticVariable.serial_io_status = true ;
			         	
			}
		}
		*/

        //	TurmtechnikActivity.stringInfoText = "Serial Status: " + StaticVariable.serial_io_status ;
    }

    /* ab 8.5.15 weggegeben
	private boolean checkAntwortOk()
	{
		if( ! etwasGesendetCarambola)
		{
			return true;
		}

		readSerialList.clear();

		boolean serialIoStatus = false ;

		if (carambola_serial_io_list.get(carambola_number_index) != null)
		{
			Log.e("carambola" , "not null") ;
			readSerialList = carambola_serial_io_list.get(carambola_number_index).getSerialBytes();
			// vorlaeufig nur schauen ob irgendwas gekommen ist !!!!
			if (readSerialList.size() > 0) {
				Log.e("carambola", "Antwort O.K.");
				serialIoStatus = true;
			} else {
				Log.e("carambola", "keine Antwort");
				serialIoStatus = false;
				//StaticVariable.serial_io_status = true ;
				//carambola_serial_io_list.get(carambola_number_index).disconnect();
			}
		}
		else
		{
			serialIoStatus = false ;
		}

		return serialIoStatus ;
	}
	*/

    public static void changeRelais(int relais_number, boolean flag) {
        relais_number = Math.max(relais_number, 1);
        relais_number = Math.min(relais_number, TurmtechnikActivity.RELAIS_COUNT);
        // Log.d("relais_number" , "" + relais_number);
        relaisNew[relais_number - 1] = flag;
    }
    
    /**
     * Gibt den Zustand aller Relais als Binärstring im Logcat aus.
     * 1 = Relais EIN, 0 = Relais AUS
     * Format: "RELAIS_BIN [1-32]: 10101010... [33-64]: 01010101..."
     */
    public static void logRelaisBinaryState() {
        StringBuilder binaryString = new StringBuilder();
        binaryString.append("RELAIS_BIN");
        
        // Gruppiere in Blöcke von 32 Relais (entspricht einer Platine)
        int blockSize = 32;
        int totalRelais = TurmtechnikActivity.RELAIS_COUNT;
        
        for (int blockStart = 0; blockStart < totalRelais; blockStart += blockSize) {
            int blockEnd = Math.min(blockStart + blockSize, totalRelais);
            binaryString.append(" [").append(blockStart + 1).append("-").append(blockEnd).append("]: ");
            
            for (int i = blockStart; i < blockEnd; i++) {
                if (relaisNew[i] != null && relaisNew[i]) {
                    binaryString.append("1");
                } else {
                    binaryString.append("0");
                }
            }
        }
        
        Log.e("RelaisState", binaryString.toString());
    }

    /**
     * Liefert true, wenn die Verbindung zur Steuerung (Bluetooth oder WLAN) bereit ist.
     * Im Demo-Modus: immer true, damit Relais-Anzeige (ein/aus) und UI normal funktionieren.
     * Bei Bluetooth: nur Platine 0 (Index 0).
     * Bei WLAN: nur die konfigurierten Platinen (0 bis ipList.size()-1), damit die Nebenuhr auch mit WiFi läuft.
     */
    public static boolean getSerialIoStatus2() {
        if (StaticVariable.platinenDemoModus) return true;
        if (StaticVariable.bt_io_ok) {
            return StaticVariable.serial_io_status4[0];
        }
        // WLAN/Carambola: nur die konfigurierten Platinen prüfen
        int n = (StaticVariable.ipList != null) ? StaticVariable.ipList.size() : 0;
        if (n <= 0) {
            return false;
        }
        for (int i = 0; i < n && i < 4; i++) {
            if (!StaticVariable.serial_io_status4[i]) {
                return false;
            }
        }
        return true;
    }

    /**
     * True, wenn auf der angegebenen Platine seit dem Zeitstempel ein Carambola-Fehler aufgetreten ist.
     * Nur relevant bei WLAN-Modus (carambola_io_ok). Nebenuhr soll Impuls nicht zählen, wenn während des Impulses ein Fehler auftrat.
     * @param platineIndex 0-based Platine (0 = erste Platine)
     * @param sinceMs elapsedRealtime() zum Impulsstart
     */
    public static boolean hadCarambolaErrorSince(int platineIndex, long sinceMs) {
        if (!StaticVariable.carambola_io_ok || platineIndex < 0 || platineIndex >= 4) {
            return false;
        }
        return StaticVariable.lastCarambolaErrorTimeMs[platineIndex] >= sinceMs;
    }

    private void rebootSU() {
        try {
            Process proc = Runtime.getRuntime()
                    .exec(new String[]{"su", "-c", "reboot"});


            proc.waitFor();

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void sleepTime(long time) {
        try {
            Thread.sleep(time);
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    //public void stopAvrNetIoThread()
    //{
    // checkChange();
    //	doRun = false;
    //}
}
