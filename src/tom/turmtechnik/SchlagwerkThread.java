package tom.turmtechnik;

import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

public class SchlagwerkThread extends Thread {
    private final String sourceFileName = "SchlagwerkThread";


    private String hammerMelodieFilename;

    ExcelRead excelread;

    ExcelRead excelReadSWVx;

    /** Cache: SWV-Name (z.B. SWV2) -> Pause in ms. Verhindert wiederholtes Öffnen von Schlagwerkzeiten.xls pro Zeile. */
    private Map<String, Integer> swvxCache;

    Calendar calendar;

//	int beginnSchlagwerk1 ; // zeit in sekunden seit Mitternacht
//	int endeSchlagwerk1 ; 
//	int beginnSchlagwerk2 ;
//	int endeSchlagwerk2 ;

    int minutenMomentan;

    String fileName;


    //	StaticVariable.hammerDelayTime = (hammerZeit[localIndex] * 10 ) ;
//	StaticVariable.hammerIndex = localIndex ;
//	StaticVariable.hammerRelais = relaisNumber[localIndex] ;
//
    //neu 26.09.16 -- erweiterung auf nur eine Glockenmelodie mit den Haemmern abspielen
    //neuer Konstrukor
    public SchlagwerkThread(String hammerMelodieFilename) // null = normal wie immer, sonst nur Melodie aus /Schlagwerk/Schlagmelodien abspielen
    {

        this.hammerMelodieFilename = hammerMelodieFilename;
    }


    public void run() {
        //26.09.16 // auch nur Hammermelodie abspielen
        if (hammerMelodieFilename != null)
        {
            nurHammerMelodieAbspielen(hammerMelodieFilename);
        } else {
            // 2.04.2016
            // Schlagwerk per Taste abschaltbar
            if (StaticVariable.flagSchlagwerkOnOff) {
                //if (UhrThread.blockReady == 0) // schlagwerk nur wenn kein automatic Programm aktiv
                // 4.12.2014 -- neu in Schlagwerkzeiten.xls ein/ausschaltbar
                if ((UhrThread.blockReady == 0) || (StaticVariable.schlagwerkWaehrendMelodieLeuten == true)) {
                    calendar = Calendar.getInstance();

                    int minutenSave = calendar.get(Calendar.MINUTE);
                    int stundenSave = calendar.get(Calendar.HOUR_OF_DAY);

                    minutenMomentan = ((stundenSave * 60) + (minutenSave));
                    //Log.e("momentane minuten", "gespeicher");


                    //Log.e("minuten Momentan", "=" + minutenMomentan);
                    //Log.e("beginnSchlagwerk1", "=" + StaticVariable.beginnSchlagwerk1);
                    //Log.e("endeSchlagwerk1", "=" + StaticVariable.endeSchlagwerk1);
                    //Log.e("beginnSchlagwerk2", "=" + StaticVariable.beginnSchlagwerk2);
                    //Log.e("endeSchlagwerk2", "=" + StaticVariable.endeSchlagwerk2);

                    if ((inTimeRange(minutenMomentan, StaticVariable.beginnSchlagwerk1, StaticVariable.endeSchlagwerk1))) {
                        //Log.e("SCHLAGWERK", "1 aktiv");
                        doSchlagwerk1();
                    } else {
                        //Log.e("SCHLAGWERK2", "CHECK");
                        if ((inTimeRange(minutenMomentan, StaticVariable.beginnSchlagwerk2, StaticVariable.endeSchlagwerk2))) {
                            //Log.e("SCHLAGWERK", "2 aktiv");
                            doSchlagwerk2();
                        } else {
                            //Log.e("SCHLAGWERK", "NICHT aktiv");
                        }
                    }
                    ////Log.e("close" , "workboook") ;
                    //excelread.closeWorkbook() ;

                    calendar = null;
                    System.gc();

                } // ende von if blockReade == 0
            } else {
                Log.i("Schlagwerk", "AUSGESCHALTET");
            }
        }

    } // ende von run


    private void nurHammerMelodieAbspielen(String hammerMelodieFilename)
    {
        fileName = makeStringHammerMelodie(hammerMelodieFilename);
        Log.e("nurHammer" , "filename=" + fileName) ;
        doSchlagwerk(fileName);
    }

    private boolean inTimeRange(int minutenMomentan, int beginnZeit, int endeZeit) // alles in Minuten!
    {
        int timeCounter = beginnZeit;

        //Log.e("zeit" , "momentan=" + minutenMomentan + "beginn=" + beginnZeit + "ende=" + endeZeit) ;

        if (beginnZeit == endeZeit)  // z.B. 00:00 00:00
        {

            //Log.e("nicht" , "in Time Range") ;
            return false;  // quasi ausgeschaltet

        }

        while (timeCounter != endeZeit + 1) {
            if (minutenMomentan == timeCounter) {
                return true; // o.k. momentane zeit im beginn -- ende breich
            }

            timeCounter++;
            if (timeCounter > 1439) // > 23:59 ?
            {
                timeCounter = 0;
            }
            // Log.i("timeCounter" , ("=")+timeCounter) ;
        }
        return false; // keine uebereinstimmung im Zeitbereich
    }

    private int getMinuten(String excelZeit) {
        String[] zeitSplit = excelZeit.split(":");
        //Log.i("zeitSplit" , "[0]" + zeitSplit[0]) ;
        //Log.i("zeitSplit" , "[1]" + zeitSplit[1]) ;
        //Log.i("zeitSplit" , "[2]" + zeitSplit[2]) ;

        int stunden = Integer.parseInt(zeitSplit[0]);
        int minuten = Integer.parseInt(zeitSplit[1]);
        //	 int sekunden = Integer.parseInt(zeitSplit[2]) ;

        //Log.i("stunden" , "=" + stunden) ;
        //Log.i("minuten" , "=" + minuten) ;
        //	 Log.i("sekunden" , "=" + sekunden) ;

        int zeitTemp = ((stunden * 60) + (minuten));
        //Log.i("zeitTemp" , "=" + zeitTemp ) ;

        return zeitTemp;
    }


    private int getSekunden(String excelZeit) {
        String[] zeitSplit = excelZeit.split(":");
        //Log.i("zeitSplit" , "[0]" + zeitSplit[0]) ;
        //Log.i("zeitSplit" , "[1]" + zeitSplit[1]) ;
        //Log.i("zeitSplit" , "[2]" + zeitSplit[2]) ;

        int stunden = Integer.parseInt(zeitSplit[0]);
        int minuten = Integer.parseInt(zeitSplit[1]);
        int sekunden = Integer.parseInt(zeitSplit[2]);

        //Log.i("stunden" , "=" + stunden) ;
        //Log.i("minuten" , "=" + minuten) ;
        //Log.i("sekunden" , "=" + sekunden) ;

        int zeitTemp = ((stunden * 3600) + (minuten * 60) + sekunden);
        //Log.i("zeitTemp" , "=" + zeitTemp ) ;

        //	 int zeitTemp = ((Integer.parseInt(zeitSplit[0])*3600) +
        //			          (Integer.parseInt(zeitSplit[1])*60) + Integer.parseInt(zeitSplit[2])) ;

        return zeitTemp;

    }

    private void doSchlagwerk1() {
        fileName = makeStringSchlagwerk("/Schlagwerk_1/");
        doSchlagwerk(fileName);
    }

    private void doSchlagwerk2() {
        // sound abspielen 14.6.2013

        fileName = makeStringSchlagwerk("/Schlagwerk_2/");
        doSchlagwerk(fileName);
    }

    /** Wird von runTest aufgerufen. Führt eine Test-Datei aus (z. B. 15_Min.xls, 3_Uhr.xls). */
    void runTestFile(String fullPath) {
        doSchlagwerk(fullPath);
    }

    private static String getEnvironment_TurmtechnikStatic() {
        String makeString = (Environment.getExternalStorageDirectory().getPath() + "/Turmtechnik");
        File checkfile = new File(makeString);
        if (!checkfile.exists()) {
            makeString = (Environment.getDataDirectory().getPath() + "/Turmtechnik");
        }
        return makeString;
    }

    /** testKind: "viertel1","viertel2","viertel3","viertel4","stunde","stundeAktuell" */
    private static String getTestFileName(String testKind) {
        if (testKind == null) return null;
        if ("viertel1".equals(testKind)) return "15_Min.xls";
        if ("viertel2".equals(testKind)) return "30_Min.xls";
        if ("viertel3".equals(testKind)) return "45_Min.xls";
        if ("viertel4".equals(testKind)) return "12_Uhr.xls";
        if ("stunde".equals(testKind)) return "1_Uhr.xls";
        if ("stundeAktuell".equals(testKind)) {
            int h = Calendar.getInstance().get(Calendar.HOUR);
            return (h == 0 ? "12_Uhr.xls" : h + "_Uhr.xls");
        }
        return null;
    }

    /** Startet einen Test (1/4, 1/2, 3/4, 4/4, Stundenschlag, aktuelle Stunde) in einem Hintergrund-Thread. */
    public static void runTest(int typ, String testKind) {
        final String fileName = getTestFileName(testKind);
        if (fileName == null) return;
        final String base = getEnvironment_TurmtechnikStatic();
        final String fullPath = base + "/Schlagwerk" + typ + "/" + fileName;
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    new SchlagwerkThread(null).runTestFile(fullPath);
                } catch (Exception e) {
                    Log.e("Schlagwerk", "Test fehlgeschlagen: " + e.getMessage());
                }
            }
        }).start();
    }

    private void doSchlagwerk(String fileName) {
        excelread = new ExcelRead();
        try {
            //Log.i ("versuch" , "SchlagwerkX oeffnen") ;
            //Log.i ("filename" , "= " + fileName) ;
            excelread.openXls_save(fileName);
            doSchlagwerkExakt();            // o.k. file vorhanden... abarbeiten
            excelread.closeWorkbook();
        } catch (Exception e) {
            //Log.i("ist nicht da" , "nichts tun") ;
        }
    }

    private void doSchlagwerkExakt() {

        //Log.e("Schlagwerk", "doSchlagwerkExakt");
        int zeile = 3;

        try {

            while (!excelread.getCellString(3, zeile).equals("")) // noch zeilen vorhanden ?
            {
                // entsprechende Hammer relais ein/aus schalten
                // public static Integer[] relaisNumber = new Integer[16]; // so ist das in Turmtechnik
                //	public static Integer[] hammerZeit = new Integer[16];   // die erste hammerZeit != 0 ist der 1.te H.

                int momentanerHammer = sucheErstenHammer();
                int ersterHammer = momentanerHammer;


                for (int i = 4; i < 8; i++) {
                    //Log.i("momentaner" , "Hammer=" + momentanerHammer) ;
                    if (momentanerHammer != -1) //noch ein Hammer vorhanden ?
                    {
                        int relaisNumber = getRelaisNumber(momentanerHammer);
                        // Log.i("relaisNumber" , "= " + relaisNumber ) ;

                        if ((excelread.getCellString(i, zeile)).equals("")) // kein x ?
                        {
                            //Log.i("kein" , "XXX") ;
                            TurmtechnikActivity.globalOn[momentanerHammer] = false;
                            Serial_IoThread.relaisNew[relaisNumber] = false;
                        } else {
                            //Sound.playHammerSound() ;  // 15.6.13 Hammer Sound eingebaut
                            // wegen 2 Hammer
                            if (momentanerHammer == ersterHammer) {
                                Sound.playHammerSound1();
                            } else {
                                Sound.playHammerSound2();
                            }

                            //Log.i("XXX" , "vorhanden") ;
                            TurmtechnikActivity.globalOn[momentanerHammer] = true;
                            Serial_IoThread.relaisNew[relaisNumber] = true;
                        }
                        momentanerHammer = sucheNaechstenHammer(momentanerHammer);
                    }

                }

                //String schaltzeit = excelread.getCellString(3, zeile) ;
                //int time = Integer.parseInt(schaltzeit) * 10 ;
                // neu 3.4.16 es gibt die Variablen SWV1-5 , SchlagWerkVariable1 = Hamme ein Zeit

                int time;
                String schaltzeitString = excelread.getCellString(3, zeile);
                //Log.e("Schlagwerk", "schaltzeitString=" + schaltzeitString);
                if (schaltzeitString.equals("SWV1")) {
                    time = StaticVariable.schlagWerkVariable1;
                } else {
                    time = readTimeSWVx(schaltzeitString);
                }

                //Log.e("Schlagwerk", "warte ms=" + time);
                sleepTime(time);                                    // zeit von Tabelle warten

                zeile++;
                //Log.i("ZEILE" , "=" + zeile) ;
            }
        } catch (ArrayIndexOutOfBoundsException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            new LogExcelError(3, zeile, fileName, 0, sourceFileName, 277);
        } catch (NumberFormatException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            new LogExcelError(3, zeile, fileName, 0, sourceFileName, 281);
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            new LogExcelError(3, zeile, fileName, 0, sourceFileName, 285);
        }
        alleHaemmerAus();
    }

    private int readTimeSWVx(String SWVx) {
        int defaultMs = StaticVariable.schlagWerkVariable1;
        if (SWVx == null || SWVx.trim().isEmpty()) {
            return defaultMs;
        }
        ensureSwvxCache();
        if (swvxCache != null && swvxCache.containsKey(SWVx)) {
            return swvxCache.get(SWVx);
        }
        return defaultMs;
    }

    /** Lädt Schlagwerkzeiten.xls einmal und füllt swvxCache (SWV2–SWV5 etc. -> Pause ms). */
    private void ensureSwvxCache() {
        if (swvxCache != null) {
            return;
        }
        swvxCache = new HashMap<>();
        String fileNameSWVx = getEnvironment_Turmtechnik() + "/Schlagwerk/Schlagwerkzeiten.xls";
        ExcelRead reader = new ExcelRead();
        try {
            reader.openXlsSheet(fileNameSWVx, 1);
            for (int i = 3; i < 8; i++) {
                try {
                    String name = reader.getCellString(2, i);
                    String valueStr = reader.getCellString(1, i);
                    if (name != null && !name.trim().isEmpty() && valueStr != null && !valueStr.trim().isEmpty()) {
                        int ms = Integer.parseInt(valueStr.trim());
                        swvxCache.put(name.trim(), ms);
                    }
                } catch (Exception e) {
                    // Zeile überspringen
                }
            }
            reader.closeWorkbook();
        } catch (Exception e) {
            swvxCache.clear();
        }
    }

    private void alleHaemmerAus() {
        int momentanerHammer = sucheErstenHammer();

        for (int i = 0; i < TurmtechnikActivity.RELAIS_COUNT; i++) {
            if (momentanerHammer != -1) {
                int relaisNumber = getRelaisNumber(momentanerHammer);
                //Log.i("relaisHammer" , "= " + relaisNumber ) ;
                TurmtechnikActivity.globalOn[momentanerHammer] = false;
                Serial_IoThread.relaisNew[relaisNumber] = false;
            }
            momentanerHammer = sucheNaechstenHammer(momentanerHammer);

        }

    }

    private int sucheErstenHammer() {
        ////Log.e("suche" , "ersten Hammer") ;
        for (int i = 0; i < TurmtechnikActivity.RELAIS_COUNT; i++) {
            if (TurmtechnikActivity.hammerZeit[i] > 0) {
                ////Log.e("HAMMER" , "=" + i ) ;
                return i;
            }
        }
        ////Log.e("KEIN" , "Hammer gefunden") ;
        return -1; // kein Hammer gefunden

    }

    private int sucheNaechstenHammer(int momentanerHammer) {
        for (int i = momentanerHammer + 1; i < TurmtechnikActivity.RELAIS_COUNT; i++) {
            if (TurmtechnikActivity.hammerZeit[i] > 0) {
                return i;
            }
        }
        return -1; // kein hammer gefunden
    }

    private int getRelaisNumber(int momentanerHammer) {
        return (TurmtechnikActivity.relaisNumber[momentanerHammer]) - 1;
    }


    final String[] minutenFile = new String[]{"15_Min.xls", "30_Min.xls", "45_Min.xls"};

    final String[] stundenFile = new String[]{"12_Uhr.xls", "1_Uhr.xls", "2_Uhr.xls", "3_Uhr.xls", "4_Uhr.xls",
            "5_Uhr.xls", "6_Uhr.xls", "7_Uhr.xls", "8_Uhr.xls",
            "9_Uhr.xls", "10_Uhr.xls", "11_Uhr.xls"};

    private String makeStringSchlagwerk(String SchlagwerkNummer) {
        String workString = getEnvironment_Turmtechnik(); // path und "Turmtechnik" festgelegt
        workString = workString + "/Schlagwerk" + SchlagwerkNummer;
        int minutenSave = calendar.get(Calendar.MINUTE);

        if (minutenSave != 0)  // nicht volle Stunde
        {
            minutenSave = (minutenSave / 15) - 1;
            workString = workString + minutenFile[minutenSave];
            //Log.i("NICHT VOLL" , "=" + workString) ;
        } else   // volle Stunde
        {
            int stundenSave = calendar.get(Calendar.HOUR);
            //Log.i("Stunde" , "=" + stundenSave) ;
            workString = workString + stundenFile[stundenSave];
            //Log.i("Volle Stunde" , "=" + workString) ;
        }

        return workString;
    }

    private String makeStringHammerMelodie(String hammerMelodieFilename) {
        String workString = getEnvironment_Turmtechnik(); // path und "Turmtechnik" festgelegt
        workString = workString + "/Schlagwerk/Schlagmelodien/" + hammerMelodieFilename;

        return workString;
    }

    private String getEnvironment_Turmtechnik() {
        String makeString = (Environment.getExternalStorageDirectory().getPath() + "/Turmtechnik");

        File checkfile = new File(makeString);
        if (checkfile.exists()) {
            //Log.i("prog sd", "Turmtechnik vorhanden");
        } else {
            makeString = (Environment.getDataDirectory().getPath() + "/Turmtechnik");
        }

        return makeString;
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
