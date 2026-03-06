package tom.turmtechnik;

import android.app.Activity;
import android.app.Dialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.StrictMode;
import android.util.Log;
import android.view.Display;
import android.view.KeyEvent;
import android.view.View;
import android.widget.TextView;
import android.widget.TimePicker;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

import jxl.read.biff.BiffException;

//import android.util.Log;


public class ManuelerStartActivity extends Activity {
    private final String sourceFileName = "ManuelerStartActivity";

    private TextView outputView;

    private boolean file_ok = false;
    private String inputFileString;
    private int index;

    private ManuelerStartLayout layout;

    private int displayWidth;
    private int displayHeight;

    //public static boolean einAus = false ;

    //private final int BESCHRIFTUNG_GLOCKEN_SHEET = 0 ; // 10 ;

    private Handler handler;

    protected boolean doRun;

    private static ArrayList<String> beschriftungTasteArray = new ArrayList<String>();
    private static ArrayList<String> auszufuehrendeMelodieArray = new ArrayList<String>();
    //private static int indexMelodieStart = 0 ; // index in array, echte melodie offset
    // ist index + 3 !

    private long gewaehlteStartzeitMs = 0;
    private long startzeitMinimumMs;
    private TagesSuche tagesSuche;
    private int neuBerechnenCount = 0;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.text_layout);

        Log.e("Seite2Activity", "onCreate");

        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        getDisplayParameter();
        //Log.i("displayWidth" , "=" + displayWidth) ;
        //Log.i("displayHeight" , "=" + displayHeight) ;

        StartTurmtechnikService.setTimeTurmtechnik(60 * 30); // 30 Minuten Manueller Start anzeigen

        UhrThread.newSearchAutomaticStart = true;
        StaticVariable.stringInfoTextField[0] = "suche nächsten Start...";

        outputView = (TextView) this.findViewById(R.id.textView1);
        outputView.append("\n\n");

        inputFileString = (Environment.getExternalStorageDirectory().getPath() +
                "/Turmtechnik/Config/Manuelle Programme.xls");

        // 7.6.13 braucht nicht mehr geprueft zu werden wurde schon in Turmtechnik.java getestet
        file_ok = true;
        File checkfile = new File(inputFileString);

        if (checkfile.exists()) {
            file_ok = true;
        } else {
            file_ok = false;
        }


        //     outputView.append("das flag = "+ file_ok + "\n");

//        checkfile = null ;  // fuer garbage collector freigeben
        System.gc();

        if (file_ok == false) {
            outputView.append("\"" + inputFileString + "\" \n\n nicht vorhanden\n");
        }

        if (file_ok == true) {

            tagesSuche = new TagesSuche();

            loadManuelleProgramme();


            outputView = null;
            System.gc();

            layout = new ManuelerStartLayout(inputFileString, getApplicationContext());
            setContentView(layout.initLayout());

            startzeitMinimumMs = berechneStartzeitMinimum();
            setGewaehlteStartzeitMs();

            if (gewaehlteStartzeitMs < startzeitMinimumMs) {
                gewaehlteStartzeitMs = startzeitMinimumMs;
                setHourAndMinuteFromMs(gewaehlteStartzeitMs);
            }
            //layout.printStartzeitFromMs(gewaehlteStartzeitMs);
            Log.e("startzeitm", "minimumMS=" + startzeitMinimumMs + " " + convertMsToTimeString(startzeitMinimumMs));
            Log.e("startzeitm", "gewaehltMs=" + gewaehlteStartzeitMs + " " + convertMsToTimeString(gewaehlteStartzeitMs));

            layout.printStartzeitFromMs(gewaehlteStartzeitMs);


            layout.zureckButton.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View v) {
                    // TODO Auto-generated method stub
                    //finish();
                    endActivityManuellStart();
                }
            });

            layout.einAusButton.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View v) {
                    // TODO Auto-generated method stub
                    // startSeite3();
                    switchEinAus();
                }
            });

            layout.timeButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {

                    showDialog(0);
                }
            });

            layout.minusButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    layout.decrementStartTime();
                    setGewaehlteStartzeitMs();
                    layout.printStartzeitFromMs(gewaehlteStartzeitMs);
                }
            });

            layout.plusButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    layout.incrementStartTime();
                    setGewaehlteStartzeitMs();
                    layout.printStartzeitFromMs(gewaehlteStartzeitMs);
                }
            });

            layout.pfeilNachObenButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (StaticVariable.indexMelodieStart < (beschriftungTasteArray.size() - 1 - 3)) {
                        StaticVariable.indexMelodieStart++;
                        printManuelleProgramme(StaticVariable.indexMelodieStart);
                        startzeitMinimumMs = berechneStartzeitMinimum();
                        gewaehlteStartzeitMs = startzeitMinimumMs;
                        setHourAndMinuteFromMs(gewaehlteStartzeitMs);
                        layout.printStartzeitFromMs(gewaehlteStartzeitMs);
                        Log.e("startzeitm", "minimumMS=" + startzeitMinimumMs + " " + convertMsToTimeString(startzeitMinimumMs));
                        Log.e("startzeitm", "gewaehltMs=" + gewaehlteStartzeitMs + " " + convertMsToTimeString(gewaehlteStartzeitMs));


                    }

                }
            });

            layout.pfeilNachUntenButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (StaticVariable.indexMelodieStart > -3) {
                        StaticVariable.indexMelodieStart--;
                        printManuelleProgramme(StaticVariable.indexMelodieStart);
                        startzeitMinimumMs = berechneStartzeitMinimum();
                        gewaehlteStartzeitMs = startzeitMinimumMs;
                        setHourAndMinuteFromMs(gewaehlteStartzeitMs);
                        layout.printStartzeitFromMs(gewaehlteStartzeitMs);
                        Log.e("startzeitm", "minimumMS=" + startzeitMinimumMs + " " + convertMsToTimeString(startzeitMinimumMs));
                        Log.e("startzeitm", "gewaehltMs=" + gewaehlteStartzeitMs + " " + convertMsToTimeString(gewaehlteStartzeitMs));

                    }
                }
            });

            layout.stopButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    MelodieThreadNew.doRunOff();
                    StaticVariable.pathStoppedByUser = (StaticVariable.pathAndFileNameNextMelodie != null ? StaticVariable.pathAndFileNameNextMelodie : "");

                    TagesSuche tagesSuche = new TagesSuche();

                    TurmtechnikActivity.allMelodieRelaisOffAusfuehren();

                    UhrThread.blockReady = 0; // block
                    // fertig ,
                    // wieder
                    // ready
                    StaticVariable.stringInfoTextField[0] = "Stop betätigt";
                    UhrThread.newSearchAutomaticStart = true;
                    // allRelaisOff();
                    StaticVariable.changeInternetBenutzerprogramme++;
                    StaticVariable.changeInternetVerknuepfteTasten++;
                    LogTurmtechnik2 logTemp =
                            new LogTurmtechnik2("Stop gedrueckt", 0, 0, 0, 0);
                    logTemp = null;
                    System.gc();
                    //sendTastenStatusSeite1();
                    //setRestartTimer(5);  // am 18.1.14 wieder ausgebaut
                    //restartTurmtechnik();

                }
            });


        } // ende von if file ok

        if (StaticVariable.manuellerStartEinAus) {
            layout.setEinAusGelb();
        } else {
            layout.setEinAusGrau();
        }

// thread fuer update infotext erzeugen
        handler = new Handler();
        startUpdateInfoText();

    } // ende von onCreate

    public String convertMsToTimeString(long ms) {
        Date date = new Date(ms);

        DateFormat sdf = new SimpleDateFormat("yyyy:MM:dd:HH:mm:ss");

        String timeString = sdf.format(date);

        return timeString;
    }

    private void setGewaehlteStartzeitMs() {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, StaticVariable.startStundeManuell);
        cal.set(Calendar.MINUTE, StaticVariable.startMinuteManuell);

        gewaehlteStartzeitMs = cal.getTimeInMillis();
    }

    private void setHourAndMinuteFromMs(long ms) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(ms);

        StaticVariable.startStundeManuell = cal.get(Calendar.HOUR_OF_DAY);
        StaticVariable.startMinuteManuell = cal.get(Calendar.MINUTE);
    }

    @Override
    protected void onStart() {
        Log.e("ManuelerStartActivity", "[ACTIVITY] onStart");
        super.onStart();
    }

    @Override
    protected void onResume() {
        Log.e("ManuelerStartActivity", "[ACTIVITY] onResume");
        super.onResume();
        StaticVariable.manuelerStartActivityIsRunning = true;
    }

    @Override
    protected void onPause() {
        Log.e("ManuelerStart2Activity", "[ACTIVITY] onPause");
        super.onPause();
    }

    @Override
    protected void onRestart() {
        Log.e("ManuelerStartActivity", "[ACTIVITY] onRestart");
        super.onRestart();
    }

    @Override
    protected void onStop() {
        Log.e("ManuelerStartActivity", "[ACTIVITY] onStop");
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        Log.e("ManuelerStartActivity", "[ACTIVITY] onStart");
        super.onDestroy();
    }

    private void switchEinAus() {
        if (StaticVariable.manuellerStartEinAus) {
            StaticVariable.manuellerStartEinAus = false;
            StaticVariable.manuellerStartAktiviert = false;
            layout.einAusButton.setText("AUS");
            layout.setEinAusGrau();
        } else {
            StaticVariable.manuellerStartEinAus = true;
            StaticVariable.manuellerStartAktiviert = true;
            layout.einAusButton.setText("EIN");
            layout.setEinAusGelb();
        }

    }

    private int infoTextWait = 0;

    public void startUpdateInfoText() {


        Runnable runnable = new Runnable() {

            @Override
            public void run() {

                doRun = true;


                printManuelleProgramme(0);

                while (doRun) {
                    try {
                        Thread.sleep(250);
                        infoTextWait++;
                    } catch (InterruptedException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            // TODO Auto-generated method stub
                            //Log.i("im" , "handler") ;
                            //if(StaticVariable.internetChanged)
                            //{
                            //StaticVariable.internetChanged = false ;
                            //layout = new Seite2Layout(inputFileString, getApplicationContext());
                            //setContentView(layout.initLayout());
                            //Log.e("beleuchte" , "TastenAutomatik gerufen") ;

                            //beleuchteTastenAutomatik();
                            layout.printDatum();
                            layout.printUhr();
                            //layout.infoText.setText(StaticVariable.stringInfoTextField[0]);


                            if (infoTextWait > 40) {
                                infoTextWait = 0;

                                //if ((UhrThread.blockReady != 0) || (!Serial_IoThread.getSerialIoStatus()))
                                if ((UhrThread.blockReady != 0) || (! TurmtechnikActivity.zehnmalErrorIoStatusOk()))
                                {
                                    layout.infoTextRed();

                                } else {

                                    layout.infoTextBlack();

                                }

                                if (TurmtechnikActivity.flagAutomaticOnOff == false) {
                                    StaticVariable.stringInfoTextField[0] = "Automatik ausgeschaltet";
                                }

                                StaticVariable.stringInfoTextField[2] = StaticVariable.infoStringHeizung;

                                TurmtechnikActivity.incrementInfoTextIndex();
                                while (StaticVariable.stringInfoTextField[StaticVariable.infoTextIndex].equals("")) {
                                    TurmtechnikActivity.incrementInfoTextIndex();
                                }

                                layout.infoText.setText("[" + (StaticVariable.infoTextIndex + 1) + "] " + StaticVariable.stringInfoTextField[StaticVariable.infoTextIndex]);
                                layout.infoText.setText(StaticVariable.stringInfoTextField[StaticVariable.infoTextIndex]);


                                neuBerechnenCount++;

                                if ((neuBerechnenCount > 20)) {

                                    neuBerechnenCount = 0;

                                    startzeitMinimumMs = berechneStartzeitMinimum();

                                    Log.e("manueler", "StartEinAus=" + StaticVariable.manuellerStartEinAus);

                                    if (!StaticVariable.manuellerStartEinAus) {

                                        if (gewaehlteStartzeitMs < startzeitMinimumMs) {

                                            gewaehlteStartzeitMs = startzeitMinimumMs;
                                            setHourAndMinuteFromMs(gewaehlteStartzeitMs);
                                            layout.printStartzeitFromMs(gewaehlteStartzeitMs);
                                            Log.e("startzeitm", "minimumMS=" + startzeitMinimumMs + " " + convertMsToTimeString(startzeitMinimumMs));
                                            Log.e("startzeitm", "gewaehltMs=" + gewaehlteStartzeitMs + " " + convertMsToTimeString(gewaehlteStartzeitMs));
                                        }
                                    }
                                }

                                if (!StaticVariable.manuellerStartEinAus) {
                                    if ((gewaehlteStartzeitMs < startzeitMinimumMs)) {
                                        gewaehlteStartzeitMs = startzeitMinimumMs;
                                        setHourAndMinuteFromMs(gewaehlteStartzeitMs);
                                        layout.printStartzeitFromMs(gewaehlteStartzeitMs);
                                    }
                                }

                            }
                        }

                    });
                }
            }
        };
        new Thread(runnable).start();
    }


    private long berechneStartzeitMinimum() {
        long vorlaufZeitPlusVorschwingzeit = tagesSuche.getErsteVorlaufzeitMs(auszufuehrendeMelodieArray.get(StaticVariable.indexMelodieStart + 3));


        Calendar cal = Calendar.getInstance();

        long berechTemp = cal.getTimeInMillis();
        berechTemp += (vorlaufZeitPlusVorschwingzeit + 120000); // + 2 Minuten

        return berechTemp;
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if ((keyCode == KeyEvent.KEYCODE_BACK)) {
            //Log.i("Acitiviti2", "back");


            // finish();
            endActivityManuellStart();
            return true;

        }
        return false;
    }


    private void endActivityManuellStart() {
        if (file_ok) {
//	    		while (layout.buttons.size()!=0)
//	    		{
//	    			layout.buttons.remove(0);
//	    		}
            // 		layout.closeWorkbook();
            //layout = null ;

        }

        //doRun = false ;

        //    	layout.endVerknuepfteTastenThread();

        //StaticVariable.manuelerStartActivityIsRunning = false ;

        //System.gc();

        finish();
        //Intent returnActivity = new Intent(ManuelerStartActivity.this, Seite2Activity.class);
        //ManuelerStartActivity.this.startActivity(returnActivity);
    }

    private void getDisplayParameter() {
        Display display = this.getWindowManager().getDefaultDisplay();
        displayWidth = display.getWidth();
        displayHeight = display.getHeight();
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        switch (id) {
            case 0:
                //Log.i("global" , "index=" + 0) ;
                int hour = StaticVariable.startStundeManuell;
                int minute = StaticVariable.startMinuteManuell;
                boolean local_flag_24 = true; //[globalIndex] ; // false = 12 stunden modus
                // set time picker as current time
                return new TimePickerDialog(this, timePickerListener, hour, minute, local_flag_24);

            //case 1 :
            //Log.i("global" , "index=" + 1) ;
            //	int hour_1 = layout.nebenuhrStunde[1] ;
            //	int minute_1 = layout.nebenuhrMinute[1] ;
            //	boolean local_flag_24_1 = layout.flag_24[1] ; //[globalIndex] ; // false = 12 stunden modus
            // set time picker as current time
            //	return new TimePickerDialog(this, timePickerListener, hour_1 , minute_1, local_flag_24_1);

            //case 2 :
            //Log.i("global" , "index=" + 2) ;
            //	int hour_2 = layout.nebenuhrStunde[2] ;
            //	int minute_2 = layout.nebenuhrMinute[2] ;
            //	boolean local_flag_24_2 = layout.flag_24[2] ; //[globalIndex] ; // false = 12 stunden modus
            // set time picker as current time
            //	return new TimePickerDialog(this, timePickerListener, hour_2 , minute_2, local_flag_24_2);

        }
        return null;
    }

    private TimePickerDialog.OnTimeSetListener timePickerListener = new TimePickerDialog.OnTimeSetListener() {


        public void onTimeSet(TimePicker view, int selectedHour,
                              int selectedMinute) {
            StaticVariable.startStundeManuell = selectedHour;
            StaticVariable.startMinuteManuell = selectedMinute;


            //layout.timeButton.setText("Um " + selectedHour + ":" + selectedMinute);
            setGewaehlteStartzeitMs();
            layout.printStartzeitFromMs(gewaehlteStartzeitMs);
        }
    };

    private void loadManuelleProgramme() {

        ExcelRead excelread = new ExcelRead();
        try {
            excelread.openXls(inputFileString);
        } catch (BiffException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
            new LogExcelError(-1, -1, inputFileString, 0, sourceFileName, 750);
        } catch (IOException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
            new LogExcelError(-1, -1, inputFileString, 0, sourceFileName, 754);
        }

        int max_zeilen = excelread.getCellZeilen();

        for (int i = 4; i < max_zeilen; i++) {
            try {
                if (!(excelread.getCellString(0, i).equals(""))) {
                    beschriftungTasteArray.add(excelread.getCellString(0, i));
                    auszufuehrendeMelodieArray.add(excelread.getCellString(2, i));
                }
            } catch (ArrayIndexOutOfBoundsException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                new LogExcelError(3, i, inputFileString, 0, sourceFileName, 770);
            } catch (Exception e) {
                // TODO uto-generated catch block
                e.printStackTrace();
                new LogExcelError(3, i, inputFileString, 0, sourceFileName, 774);
            }
        }

        excelread.closeWorkbook();
        //Log.i("password", "=" + return_string) ;
        showLogManuelleArrays();
    }

    private void showLogManuelleArrays() {
        int size = beschriftungTasteArray.size();
        for (int i = 0; i < size; i++) {
            Log.i("Beschriftung, ausf.", "=" + beschriftungTasteArray.get(i) + "=" + auszufuehrendeMelodieArray.get(i));
        }
    }

    private void printManuelleProgramme(int startIndex) {
        for (int i = 0; i < 7; i++) {
            if (((i + startIndex) < beschriftungTasteArray.size()) && ((i + startIndex) >= 0)) {
                layout.beuntzerTextViewArray[i].setText(beschriftungTasteArray.get(i + startIndex));
            } else {
                layout.beuntzerTextViewArray[i].setText("");
            }
        }
    }

    public static String getManuellenStartFilename(int futureStunde, int futureMinute)
    // wenn nicht aktuell --> return ""
    {
        String retManuelerStartFilename = "";

        if (StaticVariable.manuellerStartEinAus) {
            if ((futureStunde == StaticVariable.startStundeManuell) && (futureMinute == StaticVariable.startMinuteManuell)) {
                retManuelerStartFilename = auszufuehrendeMelodieArray.get(StaticVariable.indexMelodieStart + 3);
            }
        }

        return retManuelerStartFilename;
    }

    public static String getBeschriftungTaste() {
        String retBeschriftungTaste = "";

        if (StaticVariable.manuellerStartEinAus) {
            retBeschriftungTaste = beschriftungTasteArray.get(StaticVariable.indexMelodieStart + 3);
        }

        return retBeschriftungTaste;
    }

    public static String getMelodieStartTime() {
        String temp = "" + (pad(StaticVariable.startStundeManuell)) + ":" +
                (pad(StaticVariable.startMinuteManuell));
        return temp;
    }

    private static String pad(int c) {
        if (c >= 10)
            return String.valueOf(c);
        else
            return "0" + String.valueOf(c);
    }
} // ende der Klasse
