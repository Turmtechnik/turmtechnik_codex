package com.awr_technology.nmea_gps_clock;

import android.os.Handler;
import android.os.SystemClock;

import com.awr_technology.marine.nmea.parser.GGAParser;
import com.awr_technology.marine.nmea.parser.RMCParser;
import com.awr_technology.marine.nmea.parser.ZDAParser;
import com.awr_technology.marine.nmea.util.Time;
import com.awr_technology.sntp_client.ShellInterface;
import tom.turmtechnik.StaticVariable;

import java.io.BufferedInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

//import android.util.Log;

//import com.awr_technology.readport.R;

public class NMEA_gps_clock {
    //int hourTemp  ;
    //int minuteTemp  ;
    //int secondsTemp ;
    //int lastHour ;
    //int lastMinute ;
    //int lastSeconds ;

    long millisNow;
    long lastMillis;

    private boolean nmeaThreadRun;

    private long timeToleranzMin;
    private long timeToleranzMax;

    private ZDAParser zda;
    private GGAParser gga;
    private RMCParser rmc;

    //private String serialGPS_time = "" ;

    private int[] serialGPS_int_time = {-1, -1, -1};

    // private long serialGPS_ms_time = -1 ;

    private ServerSocket serverSocket;

    Handler updateConversationHandler;

    //Thread serverThread = null;
    protected Thread clientThread = null;

    //public static final int SERVERPORT = 50210 ; // fuer GPS Stream, wegen Uhrzeit von GPS
    //public static final String TEST_IP = "10.0.0.1" ;
    protected Socket clientSocket = null;
    public boolean portOpen = false;

    protected String iPadresse = StaticVariable.serialGPS_IP;
    protected int port = StaticVariable.serialGPS_port;
    private int timeServerErrorCount;


    //public static final String EXAMPLE = "$GPZDA,032915,07,08,2004,00,00*4D";


    public void startClientThread() {
        Thread clientThread = new Thread(new ClientThread());
        clientThread.start();
    }

    //public String getSerialGPS_timeString()
    //{
    //    return serialGPS_time ;
    //}

    public int[] getSerialGPS_timeInteger() {
        return serialGPS_int_time;
    }

    //public long getSerialGPS_ms_time()
    //{
    //    return serialGPS_ms_time ;
    //}

    class ClientThread implements Runnable {


        public void run() {
            nmeaThreadRun = true;
            portOpen = false;
            Socket socket = null;

            //Log.e("serial client gestartet", "portOpen=" + portOpen + " nmeaThreadRun=" + nmeaThreadRun);


            while ((nmeaThreadRun == true) && (portOpen == false)) {
                //Log.e("serial client", "thread");
                clientSocket = new Socket();
                try {
                    clientSocket.connect(new InetSocketAddress(iPadresse, port), 2500);
                    portOpen = true;
                } catch (UnknownHostException uhx) {
                    portOpen = false;

                  //  Log.e("serial Fehler", "connect" + uhx.toString());
                    //updateConversationHandler.post(new updateUIThreadTime
                    //        ("ERROR connect to IP: " + iPadresse + " port: " + port));
                } catch (IOException iox) {
                    portOpen = false;
                    //Log.e("serial Fehler", "connect" + iox.toString());
                    //updateConversationHandler.post(new updateUIThreadTime
                    //        ("ERROR connect to IP: " + iPadresse + " port: " + port));
                }


                if (portOpen) {
                    //Log.e("serial zweig", "1");
                    //updateConversationHandler.post(new updateUIThreadTime
                    //        ("Verbunden mit IP: " + iPadresse + " port: " + port));
                    //updateConversationHandler.post(new updateUIThread("Warte auf Daten..."));
                } else {
                    //Log.e("serial zweig", "2");
                    //updateConversationHandler.post(new updateUIThreadTime
                    //        ("ERROR connect to IP: " + iPadresse + " port: " + port));
                    //updateConversationHandler.post(new updateUIThread
                    //        ("ERROR connect to IP: " + iPadresse + " port: " + port));

                }


                //while ( (!Thread.currentThread().isInterrupted()) && StaticVariable.threadRun )
                //{
                if (portOpen) {
                    try {
                        socket = clientSocket;

                        communicationThreadClient commThread = new communicationThreadClient(socket);

                        new Thread(commThread).start();
                    } catch (Exception e) {
                        // ist nicht gegangen.
                    }


                } else {
                    //Log.e("serial port", "open error");
                    //updateConversationHandler.post(new updateUIThreadTime
                    //        ("ERROR connect to IP: " + iPadresse + " port: " + port));
                    stopNmea();
                }

                sleepMs(5000);
            } // ende von while

            //}

        } // ende von run

        private void sleepMs(long time) {

            try {
                Thread.sleep(time);     //Thread.sleep(time);
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }


    } // ende der Klasse NMEA_gps_clock


    class communicationThreadClient implements Runnable {


        private Socket serverSocket;

        //private BufferedReader input;

        private BufferedInputStream inbytes = null;


        public communicationThreadClient(Socket serverSocket) {

            //Log.e("serial", "communicationThreadClient");

            this.serverSocket = serverSocket;


            try {

                // this.input = new BufferedReader(new InputStreamReader(this.serverSocket.getInputStream()));
                inbytes = new BufferedInputStream(serverSocket.getInputStream());


            } catch (IOException e) {

                e.printStackTrace();

            }

        }


        public void run() {

            timeToleranzMin = 2000; // mindestens differenz 2 Sekunden
            timeToleranzMax = 1000 * 60 * (Integer.parseInt(StaticVariable.timeServerMaxOffsetMinuten));

            StringBuffer stringBuffer = new StringBuffer();

            while ((!Thread.currentThread().isInterrupted()) && nmeaThreadRun) {

                try {

                    //String read = input.readLine();

                    if (inbytes.available() > 0) {
                        StaticVariable.nmeaGpsNoData = 0;
                        int tempByte = inbytes.read();
                        //Log.e("gspTemp" , "=" + tempByte) ;
                        if (tempByte == 10) // line feed = fertiger String
                        {
                            // fertigen String auswerten....

                            String gpsString = (stringBuffer.toString());
                            //gpsStringAuswerten(stringBuffer.toString());

                            boolean timeFromGpsOK = setTimesFromGps(gpsString);
                            // serialGPS_int_time = getIntegerTimeFromGps(gpsString) ;
                            // die int werte werden in getTimeFromGps(gpsString) gesetzt !!!
                            //Log.e("OK", "=" + timeFromGpsOK);
                            if (timeFromGpsOK && (lastMillis != 0)) {
                                StaticVariable.timeServerOk = true;
                                //StaticVariable.readSntpTimeMs = sntpClient.getNtpTime() ;
                                // die Zeit am Display wird auf jeden Fall aktualisiert
                                setSystemClock();
                                timeServerErrorCount = 0;
                            } else {
                                timeServerErrorCount++;
                                if (timeServerErrorCount > 10) {
                                    StaticVariable.timeServerOk = false;
                                }
                            }

                            stringBuffer = new StringBuffer();

                        } else {
                            if (tempByte != 13) // cr vergessen
                            {
                                stringBuffer.append((char) tempByte);
                            }
                        }

                        //String tempString = Integer.toHexString(tempByte) ;
                        //Log.i("tcp empfangen" , "=" + tempString ) ;
                    } else {
                        StaticVariable.nmeaGpsNoData++;
                        sleepMs(1000);

                    }

                    //input.read(temp, 0 , 16) ;

                    //StringBuffer stringBuffer = new StringBuffer() ;

                    //stringBuffer.append(temp) ;

                    //String read = stringBuffer.toString() ;

                    //String read = stringBuffer.toString() ;

                    //    updateConversationHandler.post(new updateUIThread(read));

                    //String timeFromGps = getTimeFromGps(gpsString) ;

                    //    updateConversationHandler.post(new updateUIThreadTime(timeFromGps)) ;


                } catch (IOException e) {

                    e.printStackTrace();
                }

            }

            try {
                this.serverSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

            StaticVariable.nmeaGpsNoData = 10000; // zeige an das thread endet
        } // ende von run

        private void sleepMs(long time) {

            try {
                Thread.sleep(time);     //Thread.sleep(time);
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

    } // ende der Klasse




     /*
    private void gpsStringAuswerten(String gpsString) {
        Log.i("gspString", "=" + gpsString);
        updateConversationHandler.post(new updateUIThread(gpsString));

        String timeFromGps = getTimeFromGps(gpsString);
        if (timeFromGps != null) {
            updateConversationHandler.post(new updateUIThreadTime(timeFromGps));
        }
    }



        } class updateUIThread implements Runnable {

            private String msg;


            public updateUIThread(String str) {
                this.msg = str;

            }

            @Override

            public void run() {

                //infoText.setText(infoText.getText().toString()+"Client Says: \n"+ msg + "\n");
                if (portOpen) {
                    infoText.setText("Verbunden mit " + "IP " + iPadresse + " port " + port + "\n\n");
                    infoText.append("GPS String: " + msg + "\n");
                } else {
                    infoText.setText(msg);
                }

    }

    class updateUIThreadTime implements Runnable {

        private String msg;


        public updateUIThreadTime(String str) {
            this.msg = str;

        }

        @Override

        public void run() {

            //infoText.setText(infoText.getText().toString()+"Client Says: \n"+ msg + "\n");
            timeText.setText("GPS Time: " + msg);
        }

    }
    */


    private void setSystemClock() {

        // StaticVariable.readSntpTimeMs= sntpClient.getNtpTime() ;

        long systemMs = System.currentTimeMillis();

        Date utcDate = new Date(systemMs);

        utcDate.setHours(serialGPS_int_time[0]);
        utcDate.setMinutes(serialGPS_int_time[1]);
        utcDate.setSeconds(serialGPS_int_time[2]);


        Date gmt = convertToLocal(utcDate, systemMs);

        //StaticVariable.readSntpTimeMs = getSerialGPS_ms_time() ;
        //StaticVariable.readSntpTimeMs = momentanDate.getTime() ;
        StaticVariable.readSntpTimeMs = gmt.getTime();

        //Log.e("serial readTime" , "=" + StaticVariable.readSntpTimeMs + "datum" + convertMsToTimeString(StaticVariable.readSntpTimeMs)) ;
        //Log.e("serial systemMs" , "=" + systemMs + "datum" + convertMsToTimeString(systemMs)) ;

        //long differenzMs = Math.abs(StaticVariable.readSntpTimeMs - systemMs ) ;
        long differenzMs = Math.abs(StaticVariable.readSntpTimeMs - systemMs);
        //Log.e("serial differenzMs" , "=" + differenzMs) ;
        //Log.e("serial timeToleranzMin" , "=" + timeToleranzMin) ;
        //Log.e("serial timeToleranzMax" , "=" + timeToleranzMax) ;

        //boolean setTimeFlag = (StaticVariable.schlagwerkTrigger && (differenzMs < timeToleranzMax)) ;
        boolean setTimeFlag = (StaticVariable.schlagwerkTriggerSyncTime2 && (differenzMs < timeToleranzMax));
        //Log.e("nmea", "SchlagwerkTrigger =" + StaticVariable.schlagwerkTriggerSyncTime2);
        //Log.e("nmea", "setTimeFlag=" + setTimeFlag);
        //boolean setTimeFlag = true ;

        //if( (differenzMs > timeToleranzMin) && (differenzMs < timeToleranzMax) && setTimeFlag )
        if ((differenzMs < timeToleranzMax) && setTimeFlag) {
            StaticVariable.schlagwerkTriggerSyncTime2 = false;

            //StaticVariable.stringInfoTextField[0] = "Zeitsynchronisation";
            //Log.e("nmea", "Zeitsynchronisation");

            //Looper.prepare();
            //Toast.makeText(TurmtechnikActivity.turmtechnikContext, "Zeitsynchronisation", Toast.LENGTH_LONG).show();

            //StaticVariable.infoToastText=("Zeitsynchronisation");
            //setTime(StaticVariable.readSntpTimeMs); // funktioniert nicht auf android 5.0 amazon fire
            setLinuxTime(StaticVariable.readSntpTimeMs);
        }
    }

    private Date convertToLocal(Date utc, long systemMs) {
        Date localDate = new Date(utc.getTime() + TimeZone.getDefault().getOffset(systemMs));
        return localDate;
    }

    public String convertMsToTimeString(long ms) {
        Date date = new Date(ms);

        DateFormat sdf = new SimpleDateFormat("yyyy:MM:dd:HH:mm:ss");

        String timeString = sdf.format(date);

        return timeString;
    }

    public void setTime(long time) {
        if (ShellInterface.isSuAvailable()) {
            StaticVariable.infoToastText = ("root O.K. --> Zeitsynchronisation");
            ShellInterface.runCommand("chmod 666 /dev/alarm");
            SystemClock.setCurrentTimeMillis(time);
            ShellInterface.runCommand("chmod 664 /dev/alarm");
        } else {
            //StaticVariable.stringInfoTextField[2] = "ntp server root Error" ;
            StaticVariable.infoToastText = ("keine root rechte! --> Zeitsynchronisation nicht möglich!");
        }
    }

    private void setLinuxTime(long timeMS) {
        String linuxTimeString = "";

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd.HHmmss");
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(timeMS);
        linuxTimeString = dateFormat.format(cal.getTime());
        //Log.i("linuxTimeString", "=" + linuxTimeString);

        try {
            Process process = Runtime.getRuntime().exec("su");
            DataOutputStream os = new DataOutputStream(process.getOutputStream());
            String linuxTimeSetString = "date -s " + linuxTimeString + "; \n";
          //  Log.i("linuxTimeSetString", "=" + linuxTimeSetString);
            os.writeBytes(linuxTimeSetString);
            //os.writeBytes("date -s 20120419.024012; \n");  // das funktionert auf dem Amazon Fire
            StaticVariable.infoToastText = ("Zeitsynchronisation");
        } catch (Exception e) {
            //Log.d("setLinuxTime", "error==" + e.toString());
            e.printStackTrace();
        }

    }


    public void stopNmea() {


        try {
            if (serverSocket != null) {
                serverSocket.close();
            }
            if (clientSocket != null) {
                clientSocket.close();
            }
        } catch (IOException e) {

            e.printStackTrace();

        }
    }

    private boolean setTimesFromGps(String gpsString) {

        //Log.e("serial gpsString", "=" + gpsString);
        Time t = null;
        boolean gpsOk = false;

        try {
            //zda = new ZDAParser(EXAMPLE) ;

            gga = new GGAParser(gpsString);
            t = gga.getTime();
        } catch (Exception e) {
            //Log.e("serial Error", "im String");
        }

        if (t == null) {
            try {
                //zda = new ZDAParser(EXAMPLE) ;

                zda = new ZDAParser(gpsString);
                t = zda.getTime();
            } catch (Exception e) {
                //Log.e("serial Error", "im String");
            }
        }


        if (t == null) {
            try {
                //zda = new ZDAParser(EXAMPLE)

                rmc = new RMCParser(gpsString);
                t = rmc.getTime();
            } catch (Exception e) {
                //Log.e("serial Error", "im String");
            }
        }


        if (t != null) {
            //gpsTime = //"iso08601=" + t.toISO8601() +
            //        " string=" + t.toString() ; // +
            //" Stunde=" + t.getHour() +             // int
            //" Minute=" + t.getMinutes() +        // int
            //" Sekunde=" + t.getSeconds();       // double


            //serialGPS_time = t.toString() ;


            millisNow = t.getMilliseconds();
            //hourTemp = t.getHour() ;
            //minuteTemp = t.getMinutes() ;
            //secondsTemp = (int) t.getSeconds() ;

            if (timesTempAndLastOK()) {

                serialGPS_int_time[0] = t.getHour();
                serialGPS_int_time[1] = t.getMinutes();
                serialGPS_int_time[2] = (int) t.getSeconds();
                copyTempToLast();

                //serialGPS_int_time[0] = hourTemp ;
                //serialGPS_int_time[1] = minuteTemp ;
                //serialGPS_int_time[2] = secondsTemp ;
                gpsOk = true;

            } else {
                copyTempToLast();
                gpsOk = false;
            }


            //serialGPS_ms_time = t.getMilliseconds() ;

        }
        return gpsOk;
    }

    private boolean timesTempAndLastOK() {

        //Log.e("OK Last" , "=" + lastHour + "," + lastMinute + "," + lastSeconds) ;
        //Log.e("OK Temp" , "=" + hourTemp + "," + minuteTemp + "," + secondsTemp) ;


        //if(lastMinute != minuteTemp)
        //{
        //    return false ;
        //}
        //if(lastHour != hourTemp)
        //{
        //    return false ;
        //}
        //if(lastSeconds == secondsTemp)
        //{
        //    return true ;
        //}
        //if(lastSeconds+1 == secondsTemp)
        //{
        //    return true ;
        //}
        //else
        //{
        //    return false ;
        //}

        //Log.e("OK" , "Last="  + lastMillis + " now=" + millisNow) ;


        if ((millisNow < lastMillis)) {
            //Log.e("O.K." , "ERROR millisNow < lastMillis") ;
            nmeaThreadRun = false;
            StaticVariable.nmeaGpsNoData = 10000; // restart nmea Thread
            StaticVariable.timeServerOk = false;
            return false;
        } else {
            return true;
        }
    }

    private void copyTempToLast() {
        //lastHour = hourTemp ;
        //lastMinute = minuteTemp ;
        //lastSeconds = secondsTemp ;

        lastMillis = millisNow;

    }

    public void endNmeaThread() {
        nmeaThreadRun = false;
    }

}

