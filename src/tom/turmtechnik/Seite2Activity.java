package tom.turmtechnik;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.StrictMode;
import android.text.Editable;
import android.util.Log;
import android.view.Display;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.EditText;
import android.widget.TextView;


import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

import jxl.read.biff.BiffException;

//import android.util.Log;


public class Seite2Activity extends Activity {
    private final String sourceFileName = "Seite2Activity";

    private final String verknuepfteTasten = "/Turmtechnik/Config/Beschriftung Verknuepfte Tasten.xls";

    private TextView outputView;
    private boolean file_ok = false;
    private String inputFileString;
    private int index;
    private int seite2_button_offset;
    private Seite2Layout layout;

    private final int BESCHRIFTUNG_GLOCKEN_SHEET = 0; // 10 ;

    private Handler handler;

    protected boolean doRun;

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

        UhrThread.newSearchAutomaticStart = true;
        StaticVariable.stringInfoTextField[0] = "suche nächsten Start...";

        outputView = (TextView) this.findViewById(R.id.textView1);
        outputView.append("\n\n");


//        inputFileString = (Environment.getExternalStorageDirectory().getPath()+verknuepfteTasten);

        // 7.6.13 neu wegen System.lxm  multi sheet
        //inputFileString = (Environment.getExternalStorageDirectory().getPath()+StaticConstants.excellSystemString) ;

        // wurde wieder geaendert!! 9.8.2014

        inputFileString = (Environment.getExternalStorageDirectory().getPath() +
                "/Turmtechnik/Config/Beschriftung-Tasten.xls");

        // 7.6.13 braucht nicht mehr geprueft zu werden wurde schon in Turmtechnik.java getestet
        file_ok = true;
        /*
        File checkfile = new File(inputFileString);
          
        if  (checkfile.exists())
        {
      	  file_ok = true;
        }
        // 7.6.13 neu wegen System.lxm multi sheet
     
      
        else	
        {
      	  inputFileString = (Environment.getDataDirectory().getPath()+verknuepfteTasten);
      	  if (checkfile.exists())
      			  {
      		  			file_ok = true ;
      			  }
        }
        */

        //     outputView.append("das flag = "+ file_ok + "\n");

//        checkfile = null ;  // fuer garbage collector freigeben
        System.gc();

        if (file_ok == false) {
            outputView.append("\"" + verknuepfteTasten + "\" \n\n nicht vorhanden\n");
        }

        if (file_ok == true) {
            outputView = null;
            System.gc();

            layout = new Seite2Layout(inputFileString, getApplicationContext());
            setContentView(layout.initLayout());
            seite2_button_offset = TurmtechnikActivity.BESCHRIFTUNG_TASTEN_SLOTS_PAGE1;
            ensurePage2RelaisNumbersFromDb(); // vor den Click-Listenern, damit relaisNumber/hammerZeit für Seite2 gesetzt sind

// 12.6.13 Seite2 soll wie Seite1 sein
/*          	
            layout.pfeilLinksButton.setOnClickListener(new OnClickListener() {
			
        		@Override
        		public void onClick(View v) {
        			// TODO Auto-generated method stub
        			//finish();
        			endActivitiSeite2();
        		}
        	});
       
        	layout.pfeilRechtsButton.setOnClickListener(new OnClickListener() {
				
				@Override
				public void onClick(View v) {
					// TODO Auto-generated method stub
					// startSeite3();
					startSonderMelodien() ;
				}
			});
 */

            // 13.6.13  die gesamte logik wegen der Tasten

            seite2_button_offset = TurmtechnikActivity.BESCHRIFTUNG_TASTEN_SLOTS_PAGE1;
            //Log.i("seite2_buttons_offset" , "=" + seite2_buttons_offset_temp) ;

            for (int ix = 0; ix < layout.buttons.size(); ix++) {
                final int buttonIndex = ix;
                // Tag (Slot-Index) wurde bereits in initGlockenButton gesetzt – nicht überschreiben

                layout.buttons.elementAt(ix).setOnClickListener
                        (new OnClickListener() {

                            @Override
                            public void onClick(View v) {
                                int slotIndex = (v.getTag() instanceof Integer) ? (Integer) v.getTag() : 0;
                                int buttonIndex = layout.getButtonIndexForSlot(slotIndex);
                                if (buttonIndex < 0 || layout.buttons == null || buttonIndex >= layout.buttons.size()) return;
                                int page2_button_offset = seite2_button_offset + slotIndex;
                                if (page2_button_offset >= TurmtechnikActivity.relaisNumber.length) return;

                                StaticVariable.changeInternetVerknuepfteTasten++;

                                if (TurmtechnikActivity.relaisNumber[page2_button_offset] != null
                                        && TurmtechnikActivity.relaisNumber[page2_button_offset] < StaticConstants.LIMIT_1000_100
                                        && TurmtechnikActivity.relaisNumber[page2_button_offset] > 0) // normale Relais-Taste (1..99)?
                                {
                                    if (TurmtechnikActivity.globalOn[page2_button_offset] == true)
                                    {
                                        TurmtechnikActivity.globalOn[page2_button_offset] = false;
                                        stopGlockenSound(page2_button_offset);
                                        layout.buttonOff(buttonIndex);
                                        Serial_IoThread.changeRelais(TurmtechnikActivity.relaisNumber[page2_button_offset], false);
                                    } else {
                                        //Log.i("Hammer" , "zeit=" + hammerZeit[localIndex]) ;
                                        if (TurmtechnikActivity.hammerZeit[page2_button_offset] > 0) // ist es ein Hammer ?
                                        {

                                            checkHammer(page2_button_offset);

                                        }
                                        else
                                        {
                                            if (TurmtechnikActivity.relaisNumber[page2_button_offset] == StaticVariable.heizungRelaisNumberManual2)
                                            {
                                                if (StaticVariable.heizungOnTimer > 0) {
                                                    // thread laeft noch
                                                    Log.e("Heizung", "endHeizungManualThread");
                                                    TurmtechnikActivity.heizungManualThread.endHeizungManualThread();

                                                }

                                                // jetzt einen neuen starten
                                                TurmtechnikActivity.heizungManualThread = new HeizungManualThread();
                                                TurmtechnikActivity.heizungManualThread.start();
                                                Log.e("Heizung", "heizungThrad neu gestartet");

                                                //Calendar calendar = Calendar.getInstance();

                                                //long momentanMsDurchTausend = (calendar.getTimeInMillis()) / 1000L;

                                                //long momentanPlusAchtStunden = momentanMsDurchTausend + (60 * 60 * 8); // acht Stunden

                                                //new SaveAndLoadHeizung().saveHeizung(momentanMsDurchTausend, momentanPlusAchtStunden);
                                            }


                                            new LogTurmtechnik2("manul", 0, 0, 0, 0);

                                            TurmtechnikActivity.globalOn[page2_button_offset] = true;
                                            startGlockenSound(page2_button_offset);
                                            layout.buttonOnOK(buttonIndex);
                                            if (TurmtechnikActivity.relaisNumber[page2_button_offset] > 0) {
                                                Serial_IoThread.changeRelais(TurmtechnikActivity.relaisNumber[page2_button_offset], true);
                                            }
                                        }
                                    }
                                } else // relais nummer > 100 oder 0
                                {
                                    if (TurmtechnikActivity.relaisNumber[page2_button_offset] == null) return;
                                    if (TurmtechnikActivity.relaisNumber[page2_button_offset] > 3000) {
                                        int offset = (TurmtechnikActivity.relaisNumber[page2_button_offset] - 3001);

                                        StaticVariable.sofortStartPopupRelaisNummerAktiv = (TurmtechnikActivity.relaisNumber[page2_button_offset]);
                                        StartMelodieSofort startMelodieSofort = new StartMelodieSofort(Seite2Activity.this);
                                        startMelodieSofort.setStartMelodieSofort(offset);
                                    }

                                    if (TurmtechnikActivity.relaisNumber[page2_button_offset] > 2000
                                            && TurmtechnikActivity.relaisNumber[page2_button_offset] < 3000) // ist es eine verknuepfte Taste?
                                    {
                                        int offset = (TurmtechnikActivity.relaisNumber[page2_button_offset] - 2001);
                                        offset = Math.max(offset, 0);
                                        offset = Math.min(UhrThread.verknuepfteTastenString.size(), offset);
                                        if (offset < TurmtechnikActivity.verknuepfteTastenOn.length && Boolean.TRUE.equals(TurmtechnikActivity.verknuepfteTastenOn[offset])) {
                                            synchronized (TurmtechnikActivity.VERKNUEPFTE_TASTEN_LOCK) { TurmtechnikActivity.verknuepfteTastenOn[offset] = false; }
                                            layout.buttonOff(buttonIndex);
                                            // Meteor Code entfernt
                                        } else {
                                            synchronized (TurmtechnikActivity.VERKNUEPFTE_TASTEN_LOCK) { TurmtechnikActivity.verknuepfteTastenOn[offset] = true; }
                                            layout.buttonOnOK(buttonIndex);
                                            // Meteor Code entfernt
                                        }
                                    }
                                    if ((TurmtechnikActivity.relaisNumber[page2_button_offset] > StaticConstants.LIMIT_1000_200) &&
                                            (TurmtechnikActivity.relaisNumber[page2_button_offset] < StaticConstants.LIMIT_1000_300)) // ist es eine "SEITE" Taste?
                                    {
                                        //layout.buttonOnOK(buttonIndex);

                                        String password = (readPassword(TurmtechnikActivity.relaisNumber[page2_button_offset]));
                                        if (!((password.equals("")) || (password.equals("NULL")))) {
                                            checkPasswordAndStartIntent(TurmtechnikActivity.relaisNumber[page2_button_offset], password);
                                            //layout.buttonOff(buttonIndex);

                                        } else // hier weiter wenn kein Passwort vergeben wurde
                                        {
                                            //Log.i("im 200er else " , "ZWEIG") ;
                                            startSeite(TurmtechnikActivity.relaisNumber[page2_button_offset]);
                                            //layout.buttonOff(buttonIndex);
                                        }

                                    }

                                    if (TurmtechnikActivity.relaisNumber[page2_button_offset] == StaticConstants.STOP)  // Taste stop ?
                                    {
                                        MelodieThreadNew.doRunOff();
                                        UhrThread.blockReady = 0; // block fertig , wieder ready
                                        //stringInfoText = "suche nächsten Start..." ;
                                        UhrThread.newSearchAutomaticStart = true;
                                        allRelaisOff();
                                    }
                                    if (TurmtechnikActivity.relaisNumber[page2_button_offset] == StaticConstants.AUTOMATIC || TurmtechnikActivity.relaisNumber[page2_button_offset] == 101) // Taste Automatik ein/aus (101 = alte Konfiguration)
                                    {
                                        changeAutomatic();
                                        //changeAutomaticMeteor(stringOnOff);
                                    }
                                    if (TurmtechnikActivity.relaisNumber[page2_button_offset] == StaticConstants.HOME) // Tast HOME
                                    {
                                        endActivitiSeite2();
                                    }

                                    if (TurmtechnikActivity.relaisNumber[page2_button_offset] == StaticConstants.HELP) // Help Taste?
                                    {
                                        startHelpSeite();
                                    }
                                }
                            }
                        });
            } // ende von for

        } // ende von if



// thread fuer update infotext erzeugen
        handler = new Handler();
        startUpdateInfoText();

    } // ende von onCreate

    private void checkHammer(int localIndex)
    {
        if (TurmtechnikActivity.hammerZeit[localIndex] > 0) // ist
        // es
        // ein
        // Hammer
        // ?
        {
            if (StaticVariable.hammerThreadLaeuft == false) {
                StaticVariable.hammerDelayTime2 = (TurmtechnikActivity.hammerZeit[localIndex]);
                StaticVariable.hammerSound = 1;
                for (int i = localIndex + 1; i < TurmtechnikActivity.RELAIS_COUNT; i++) {
                    if (TurmtechnikActivity.hammerZeit[i] > 0) {
                        StaticVariable.hammerSound = 0;
                        break;
                    }
                }
                LogTurmtechnik2 logTurmtechnik = new LogTurmtechnik2("Hammer manual", 0, 0, 0, 0);
                logTurmtechnik = null;

                int relNr = (TurmtechnikActivity.relaisNumber[localIndex] != null) ? TurmtechnikActivity.relaisNumber[localIndex] : 0;
                long delay = (TurmtechnikActivity.hammerZeit[localIndex] != null) ? TurmtechnikActivity.hammerZeit[localIndex] : 50;
                HammerManualThread hammermanualthread = new HammerManualThread(localIndex, relNr, delay);
                hammermanualthread.start();
            }
        }
    }

    @Override
    protected void onStart() {
        Log.e("Seite2Activity", "[ACTIVITY] onStart");
        super.onStart();
    }

    @Override
    protected void onResume() {
        Log.e("Seite2Activity", "[ACTIVITY] onResume");
        super.onResume();
        infoTextWait = 25;
        StaticVariable.seiteZweiIsRunning = true;
        startUpdateInfoText();
    }

    @Override
    protected void onPause() {
        Log.e("Seite2Activity", "[ACTIVITY] onPause");
        doRun = false;
        super.onPause();
    }

    @Override
    protected void onRestart() {
        Log.e("Seite2Activity", "[ACTIVITY] onRestart");
        super.onRestart();
    }

    @Override
    protected void onStop() {
        Log.e("Seite2Activity", "[ACTIVITY] onStop");
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        Log.e("Seite2Activity", "[ACTIVITY] onStart");
        super.onDestroy();
    }

    private void startGlockenSound(int index)
    {
        String soundName = null;
        Log.e("startGlockenSound", "index=" + index);
        try {
            if (this.getApplicationContext() != null) {
                java.util.List<PlatinenDatabaseHelper.BeschriftungTastenRow> rows = PlatinenDatabaseHelper.getInstance(this.getApplicationContext()).getBeschriftungTasten();
                if (rows != null && index >= 0 && index < rows.size()) {
                    PlatinenDatabaseHelper.BeschriftungTastenRow r = rows.get(index);
                    if (r.sound != null && !r.sound.trim().isEmpty()) {
                        soundName = r.sound.trim();
                    }
                    StaticVariable.soundGlocke.playGlockeSound(soundName, index, -1);
                    return;
                }
            }
        } catch (Exception e) { }
        ExcelRead excelread = null;
        try {
            excelread = new ExcelRead();
            excelread.openXls(TurmtechnikActivity.beschriftungTastenFileString);
            int zeilen = excelread.getCellZeilen();
            int spalten = excelread.getCellSpalten();
            if (zeilen > 0 && spalten > 11 && index >= 0) {
                int row = Math.min(index + 1, zeilen);
                soundName = excelread.getCellString(11, row);
            }
        } catch (BiffException | IOException e1) {
            Log.e("Biff", "error");
            new LogExcelError(0, 0, TurmtechnikActivity.normalprogrammFileString, -1, sourceFileName, 333);
        } catch (Exception e) { e.printStackTrace(); }
        finally {
            try { if (excelread != null) excelread.closeWorkbook(); } catch (Exception e) { }
        }
        Log.e("soundGlocke", "name=" + soundName);
        if (soundName != null && !soundName.isEmpty())
            StaticVariable.soundGlocke.playGlockeSound(soundName, index, -1);

        //String pathAndFilename = TurmtechnikActivity.sdCardPath + "/Turmtechnik/Sound/" + soundName ;
        //File file = new File(pathAndFilename) ;
        //MediaPlayer momentanSound = MediaPlayer.create(TurmtechnikActivity.this,Uri.fromFile(file)) ;
        //momentanSound.start();


    }

    private void stopGlockenSound(int index) {
        if (StaticVariable.soundGlocke != null) {
            StaticVariable.soundGlocke.stopGlockenSound(index);
        }
    }

    /**
     * Füllt relaisNumber und hammerZeit für alle 24 Seite-2-Slots (Indizes seite2_button_offset+0..23) aus der DB.
     * Muss slotbasiert (0..23) laufen, damit auch Slots nach NULL-Tasten den richtigen Relais-Eintrag bekommen.
     */
    private void ensurePage2RelaisNumbersFromDb() {
        if (layout == null) return;
        try {
            java.util.List<PlatinenDatabaseHelper.BeschriftungTastenRow> rows = PlatinenDatabaseHelper.getInstance(getApplicationContext()).getBeschriftungTasten();
            final int page2Slots = 24;
            for (int slot = 0; slot < page2Slots; slot++) {
                int globalOffset = seite2_button_offset + slot;
                if (globalOffset >= TurmtechnikActivity.relaisNumber.length) break;
                if (TurmtechnikActivity.relaisNumber[globalOffset] != null && TurmtechnikActivity.relaisNumber[globalOffset] > 0) continue;
                if (rows != null && globalOffset < rows.size()) {
                    PlatinenDatabaseHelper.BeschriftungTastenRow r = rows.get(globalOffset);
                    String c3 = (r.c3 != null ? r.c3 : "").trim();
                    String c5 = (r.c5 != null ? r.c5 : "1").trim();
                    int relNumber = 0;
                    if (!c3.isEmpty() && !"null".equalsIgnoreCase(c3) && !"leer".equalsIgnoreCase(c3)) {
                        try {
                            relNumber = Integer.parseInt(c3);
                        } catch (NumberFormatException e) { }
                    }
                    int platNumber = 1;
                    if (!c5.isEmpty() && !"null".equalsIgnoreCase(c5) && !"leer".equalsIgnoreCase(c5)) {
                        try {
                            platNumber = (int) (Double.parseDouble(c5));
                            if (platNumber < 1) platNumber = 1;
                        } catch (NumberFormatException e) { }
                    }
                    int relaisBerechnet = relNumber + ((platNumber - 1) * 32);
                    TurmtechnikActivity.relaisNumber[globalOffset] = relaisBerechnet;
                    String c4 = (r.c4 != null ? r.c4 : "").trim();
                    int hammertemp = 0;
                    if (!c4.isEmpty() && !c4.equalsIgnoreCase("null") && !c4.equalsIgnoreCase("leer")) {
                        try {
                            hammertemp = (int) (Double.parseDouble(c4) * 10);
                        } catch (NumberFormatException e) { }
                    }
                    TurmtechnikActivity.hammerZeit[globalOffset] = hammertemp;
                } else if (TurmtechnikActivity.beschriftungTastenFileString != null && !TurmtechnikActivity.beschriftungTastenFileString.isEmpty()) {
                    try {
                        ExcelRead excelread = new ExcelRead();
                        excelread.openXls(TurmtechnikActivity.beschriftungTastenFileString);
                        int zeilen = excelread.getCellZeilen();
                        int excelRow = zeilen > 0 ? Math.min(globalOffset + 1, zeilen) : 0;
                        if (zeilen > 0 && excelRow >= 1 && excelRow <= zeilen) {
                            String c3 = excelread.getCellString(3, excelRow);
                            String c5 = excelread.getCellString(5, excelRow);
                            int relNumber = 0;
                            try {
                                relNumber = (c3 == null || c3.trim().isEmpty()) ? 0 : Integer.parseInt(c3.trim().replace(",", ".").split("\\.")[0]);
                            } catch (Exception e) { }
                            int platNumber = 1;
                            try {
                                platNumber = (c5 == null || c5.trim().isEmpty()) ? 1 : (int) Double.parseDouble(c5.trim().replace(",", "."));
                                if (platNumber < 1) platNumber = 1;
                            } catch (Exception e) { }
                            TurmtechnikActivity.relaisNumber[globalOffset] = relNumber + ((platNumber - 1) * 32);
                            String c4 = excelread.getCellString(4, excelRow);
                            int hammertemp = 0;
                            try {
                                if (c4 != null && !c4.trim().isEmpty()) hammertemp = (int) (Double.parseDouble(c4.trim().replace(",", ".")) * 10);
                            } catch (Exception e) { }
                            TurmtechnikActivity.hammerZeit[globalOffset] = hammertemp;
                        }
                        excelread.closeWorkbook();
                    } catch (Exception ex) {
                        Log.w(sourceFileName, "Excel-Fallback für Seite2 Relais Zeile " + (globalOffset + 1) + ": " + ex.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            Log.e(sourceFileName, "ensurePage2RelaisNumbersFromDb", e);
        }
    }

    // das braucht einen thread in main thread ... wegen Aenderungen in der View
    // das braucht einen thread in main thread ... wegen Aenderungen in der View
    private int infoTextWait = 0;

    public void startUpdateInfoText() {
        Runnable runnable = new Runnable() {

            @Override
            public void run() {

                doRun = true;
                while (doRun) {
                    try {
                        Thread.sleep(500);
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
                            beleuchteTastenAutomatik();
                            layout.printDatum();
                            layout.printUhr();
                            Log.e("seite2Activity", "infoTextWait=" + infoTextWait);
                            if (infoTextWait > 12) {
                                infoTextWait = 0;
                                Log.e("seite2Activity", "infoTextWait2=" + infoTextWait);

                                //f ((UhrThread.blockReady != 0) || (!Serial_IoThread.getSerialIoStatus()))
                                if ((UhrThread.blockReady != 0) || ( ! TurmtechnikActivity.zehnmalErrorIoStatusOk() ))
                                {
                                    layout.infoTextRed();

                                } else {

                                    layout.infoTextBlack();

                                }

                                //layout.infoText.setText(StaticVariable.stringInfoTextField[0]);

                                if (TurmtechnikActivity.flagAutomaticOnOff == false) {
                                    //StaticVariable.stringInfoTextField[0] = "Automatik ausgeschaltet";
                                    StaticVariable.stringInfoTextField[0] = StaticVariable.getUebersetzung(0) ;
                                }

                                StaticVariable.stringInfoTextField[2] = StaticVariable.infoStringHeizung;

                                TurmtechnikActivity.incrementInfoTextIndex();
                                while (StaticVariable.stringInfoTextField[StaticVariable.infoTextIndex].equals("")) {
                                    TurmtechnikActivity.incrementInfoTextIndex();
                                }

                                layout.infoText.setText("[" + (StaticVariable.infoTextIndex + 1) + "] " + StaticVariable.stringInfoTextField[StaticVariable.infoTextIndex]);
                                layout.infoText.setText(StaticVariable.stringInfoTextField[StaticVariable.infoTextIndex]);

                            }
                        }
                    });
                }
            }
        };
        new Thread(runnable).start();
    }


    /** Aktualisiert die Tasten-Anzeige (Ein/Aus) nach Slot, nicht nach Button-Index – wichtig bei NULL-Slots. */
    private void beleuchteTastenAutomatik() {
        for (int i = 0; i < layout.buttons.size(); i++) {
            int slotIndex = layout.getSlotIndexForButton(i);
            if (slotIndex < 0) continue;
            int globalOffset = seite2_button_offset + slotIndex;
            if (globalOffset >= TurmtechnikActivity.relaisNumber.length) continue;

            if (TurmtechnikActivity.relaisNumber[globalOffset] != null
                    && TurmtechnikActivity.relaisNumber[globalOffset] < StaticConstants.LIMIT_1000_100) {
                if (TurmtechnikActivity.globalOn[globalOffset] == true) {
                    if (UhrThread.blockReady != 0) {
                        if (TurmtechnikActivity.zehnmalErrorIoStatusOk()) {
                            layout.buttonOnRed(i);
                        } else {
                            layout.buttonOnRed(i);
                        }
                    } else {
                        if (TurmtechnikActivity.zehnmalErrorIoStatusOk()) {
                            layout.buttonOnOK(i);
                        } else {
                            layout.buttonOnOK(i);
                        }
                    }
                } else {
                    layout.buttonOff(i);
                }
            } else if (TurmtechnikActivity.relaisNumber[globalOffset] != null) {
                if (TurmtechnikActivity.relaisNumber[globalOffset] == StaticConstants.STOP) {
                    //layout.buttonOnStop(i);
                }
                if (TurmtechnikActivity.relaisNumber[globalOffset] != null && (TurmtechnikActivity.relaisNumber[globalOffset] == StaticConstants.AUTOMATIC || TurmtechnikActivity.relaisNumber[globalOffset] == 101)) {
                    int automatikTasteIndex = sucheAutomaticTasteSeite2();
                    if (automatikTasteIndex >= 0) {
                        if (TurmtechnikActivity.flagAutomaticOnOff == false) {
                            layout.buttonOff(automatikTasteIndex);
                        } else {
                            layout.buttonOnOK(automatikTasteIndex);
                        }
                    }
                }
                if ((TurmtechnikActivity.relaisNumber[globalOffset] > 2000)
                        && (TurmtechnikActivity.relaisNumber[globalOffset] < 3000)) {
                    int verknuepftOffset = TurmtechnikActivity.relaisNumber[globalOffset] - 2001;
                    if (verknuepftOffset >= 0 && verknuepftOffset < TurmtechnikActivity.verknuepfteTastenOn.length
                            && Boolean.TRUE.equals(TurmtechnikActivity.verknuepfteTastenOn[verknuepftOffset])) {
                        layout.buttonOnOK(i);
                    } else {
                        layout.buttonOff(i);
                    }
                }
                if (TurmtechnikActivity.relaisNumber[globalOffset] > 3000) {
                    if ((StaticVariable.sofortStartPopupFlag)
                            && (TurmtechnikActivity.relaisNumber[globalOffset] == StaticVariable.sofortStartPopupRelaisNummerAktiv)) {
                        layout.buttonOnOK(i);
                    } else {
                        layout.buttonOff(i);
                    }
                }
            }
        }
    }

    // Meteor Code entfernt: changeAutomaticMeteor()

    private int sucheAutomaticTasteSeite2() {
        int index = -1;
        int offset = TurmtechnikActivity.BESCHRIFTUNG_TASTEN_SLOTS_PAGE1; // Seite2 beginnt nach allen Relais von Seite1
        for (int i = offset; i < TurmtechnikActivity.RELAIS_COUNT; i++) {
            Integer rn = TurmtechnikActivity.relaisNumber[i];
            if (rn != null && (rn == StaticConstants.AUTOMATIC || rn == 101)) { // 101 = alte Konfiguration
                index = i - offset;
                break;
            }
        }
        return index;
    }


    private void changeAutomatic() {
        int automatikTasteIndex = sucheAutomaticTasteSeite2();

        if (automatikTasteIndex < 0) {
            return;
        }

        if (TurmtechnikActivity.flagAutomaticOnOff == true) {
            MelodieThreadNew.doRunOff();
            StaticVariable.stopBetaetigt = true;
            StaticVariable.pathStoppedByUser = (StaticVariable.pathAndFileNameNextMelodie != null ? StaticVariable.pathAndFileNameNextMelodie : "");
            TurmtechnikActivity.allMelodieRelaisOffAusfuehren();
            TurmtechnikActivity.flagAutomaticOnOff = false;
            layout.buttonOff(automatikTasteIndex); // setAutomaticRed();
            //StaticVariable.stringInfoTextField[0] = "Automatik ausgeschaltet";
            StaticVariable.stringInfoTextField[0] = StaticVariable.getUebersetzung(0) ;
            StaticVariable.stringInfoTextField[2] = "";
        } else {
            TurmtechnikActivity.flagAutomaticOnOff = true;
            // layout.setAutomaticGreen();
            layout.buttonOnOK(automatikTasteIndex);
            //stringInfoText = "suche nächsten automatik Start..." ;
            //StaticVariable.stringInfoTextField[0] = "suche nächsten automatik Start...";
            StaticVariable.stringInfoTextField[0] = StaticVariable.getUebersetzung(17);
            StaticVariable.stringInfoTextField[2] = "";
            StaticVariable.sofortStartPopupGefunden = false;
            UhrThread.newSearchAutomaticStart = true;
            // Sofort Programmabfrage und Anzeige „nächstes Programm“, nicht erst bei nächster Sekunde
            StaticVariable.refreshInfoTextVerknuepfteTaste = true;
            StaticVariable.infoTextRefreshCount = 5;
        }
    }


    private void allRelaisOff() {
        for (int ix = 0; ix < TurmtechnikActivity.RELAIS_COUNT; ix++) {
            TurmtechnikActivity.globalOn[ix] = false;
            Serial_IoThread.changeRelais(ix + 1, false);
            if (ix < layout.buttons.size()) {
                layout.buttonOff(ix);
            }
        }
    }

    private void startSeite3() {
        Intent aktivitySeite3 = new Intent(Seite2Activity.this, SetNebenuhrActivity.class);
        Seite2Activity.this.startActivity(aktivitySeite3);
    }

    private void startSonderMelodien() {
        Intent activitySonderMelodien = new Intent(Seite2Activity.this, BenutzerMelodienActivity.class);
        Seite2Activity.this.startActivity(activitySonderMelodien);
    }

    private static int displayWidth;
    private static int displayHeight;

    private void getDisplayParameter() {
        Display display = this.getWindowManager().getDefaultDisplay();
        displayWidth = display.getWidth();
        displayHeight = display.getHeight();
    }

    public static int getDisplayWith() {
        return displayWidth;
    }

    public static int getDisplayHeight() {
        return displayHeight;
    }


    private void endActivitiSeite2() {
        if (file_ok) {
//	    		while (layout.buttons.size()!=0)
//	    		{
//	    			layout.buttons.remove(0);
//	    		}
            // 		layout.closeWorkbook();
            //layout = null ;
            doRun = false;
        }

        //    	layout.endVerknuepfteTastenThread();

        StaticVariable.seiteZweiIsRunning = false;

        System.gc();

        //finish();
        Intent returnActivity = new Intent(Seite2Activity.this, TurmtechnikActivity.class);
        Seite2Activity.this.startActivity(returnActivity);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if ((keyCode == KeyEvent.KEYCODE_BACK)) {
            TurmtechnikActivity.showExitPasswordDialogFrom(this);
            return true;
        }
        return false;
    }

    private String readPassword(int relais_number) {
        String relais_number_string = String.valueOf(relais_number);
        String return_string = "";

        ExcelRead excelread = new ExcelRead();
        try {
            excelread.openXlsSheet(inputFileString, BESCHRIFTUNG_GLOCKEN_SHEET);
        } catch (BiffException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
            new LogExcelError(-1, -1, inputFileString, BESCHRIFTUNG_GLOCKEN_SHEET, sourceFileName, 539);
        } catch (IOException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
            new LogExcelError(-1, -1, inputFileString, BESCHRIFTUNG_GLOCKEN_SHEET, sourceFileName, 543);
        }

        int max_zeilen = excelread.getCellZeilen();
        for (int i = 1; i < max_zeilen; i++) {
            try {
                if (excelread.getCellString(3, i).equals(relais_number_string)) {
                    return_string = excelread.getCellString(1, i).trim();
                    break;
                }
            } catch (ArrayIndexOutOfBoundsException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                new LogExcelError(3, i, inputFileString, BESCHRIFTUNG_GLOCKEN_SHEET, sourceFileName, 558);
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                new LogExcelError(3, i, inputFileString, BESCHRIFTUNG_GLOCKEN_SHEET, sourceFileName, 562);
            }
        }

        excelread.closeWorkbook();
        //Log.i("password", "=" + return_string) ;
        return return_string;
    }

    private void checkPasswordAndStartIntent(final int relais_number, final String excell_password) {
        AlertDialog.Builder alert = new AlertDialog.Builder(this);

//	    	alert.setTitle("Title");
        alert.setMessage("Passwort eingeben:");
//	    	alert.setMessage("Dies ist nun ein Roman\nÜber mehrere\nZeilen") ;

        // Set an EditText view to get user input
        final EditText input = new EditText(this);
        alert.setView(input);

        alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                Editable value = input.getText();
                String value_string = value.toString();
                // Do something with value!
                //Log.i("value_string", "=" + value + ":") ;
                //Log.i("password" , "=" + excell_password + ":") ;

                if (excell_password.equals(value_string)) {
                    //Log.i("password" , "OK") ;
                    startSeite(relais_number);
                } else {
                    //Log.i("password" , "ERROR") ;
                }
            }
        });

        alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                // Canceled.

            }
        });

        AlertDialog d = alert.create();
        TurmtechnikActivity.applyDialogAboveKeyboard(d);
        d.show();
    }

    private void startSeite2() {
        Intent testSeite2 = new Intent(this, Seite2Activity.class);
        this.startActivity(testSeite2);
    }

    /** Öffnet die Web-UI (Benutzerprogramme) – wie auf Seite 1, einheitliche Oberfläche. */
    private void startBenutzerMelodien() {
        TurmtechnikActivity.openWebUiInBrowser(this, "/benutzerprogramme.html");
    }

    private void startManuelerStart() {
        //setRestartTimer(60 * 15); // 15 Minuten
        Log.e("START", "ManuelerStart");
        Intent activityManuelerStart = new Intent(Seite2Activity.this,
                ManuelerStartActivity.class);
        Seite2Activity.this.startActivity(activityManuelerStart);
    }


    private void startProgrammKontrolle() {
        Intent startKontrolle = new Intent(this, ProgrammKontrolleActivity.class);
        this.startActivity(startKontrolle);
    }

    private void startSetNebenuhr() {
        //Log.e("START" , "SetNebenuhr") ;
        Intent aktivitySetNebenuhr = new Intent(this, SetNebenuhrActivity.class);
        this.startActivity(aktivitySetNebenuhr);
    }

    private void startHelpSeite() {
        String helpFileName = "";
        // lese Help File Name
        ExcelRead excelRead = new ExcelRead();
        try {
            excelRead.openXlsSheet(TurmtechnikActivity.sdCardPath + "/Turmtechnik/Config/System.xls", BESCHRIFTUNG_GLOCKEN_SHEET);
        } catch (BiffException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
            new LogExcelError(-1, -1, TurmtechnikActivity.sdCardPath + "/Turmtechnik/Config/System.xls", BESCHRIFTUNG_GLOCKEN_SHEET, sourceFileName, 648);
        } catch (IOException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
            new LogExcelError(-1, -1, TurmtechnikActivity.sdCardPath + "/Turmtechnik/Config/System.xls", BESCHRIFTUNG_GLOCKEN_SHEET, sourceFileName, 652);
        }
        int max_zeilen = excelRead.getCellZeilen();
        for (int i = 25; i < max_zeilen; i++) // die Seite 2 beginnt ab Zeile 25 !
        {
            try {
                //if(excelRead.getCellString(3, i).equals("103"))
                if (excelRead.getCellString(3, i).equals(Integer.toString(StaticConstants.HELP))) {
                    // wenn die Help Taste gefunden ist:
                    //Log.i("103" , "gefunden") ;
                    //Log.i("index i" , "=" + i) ;
                    helpFileName = (excelRead.getCellString(1, i));
                    //Log.i("helpFileName" , "=" + helpFileName) ;
                    break;
                }
            } catch (ArrayIndexOutOfBoundsException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                new LogExcelError(1, i, inputFileString, BESCHRIFTUNG_GLOCKEN_SHEET, sourceFileName, 669);
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                new LogExcelError(1, i, inputFileString, BESCHRIFTUNG_GLOCKEN_SHEET, sourceFileName, 673);
            }
        }
        excelRead.closeWorkbook();

        Intent activityHelpSeite = new Intent(this, HelpSeiteActivity.class);

        //Log.i("helpFileName" , "=" + helpFileName) ;
        activityHelpSeite.putExtra("help_file_name", helpFileName);

        this.startActivity(activityHelpSeite);
    }


    private void startSeite(int seiten_nummer) // entspricht der Relaisnummer
    {
        //Log.i("Seiten Nummer" , "=" + seiten_nummer) ;

        switch (seiten_nummer) {
            case StaticConstants.PROGRAMM_EINGEBEN:
                startBenutzerMelodien();
                break;

            case StaticConstants.PROGRAMM_ABFRAGEN:
                startProgrammKontrolle();
                break;

            case StaticConstants.SET_NEBENUHR:
                startSetNebenuhr();
                break;

            case StaticConstants.MANUELER_START:
                startManuelerStart();

        }
    }

    // Meteor Code entfernt: sendTastenStatusMeteor()
    // Meteor Code entfernt: makeMapObject()

//    @Override
//    public boolean onCreateOptionsMenu(Menu menu) {
//        getMenuInflater().inflate(R.menu.activity_turmtechnik, menu);
//        return true;
//    }


} // ende der Klasse
