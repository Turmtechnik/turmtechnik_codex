package tom.turmtechnik;

import android.content.Context;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.ViewGroup;
import android.view.WindowManager.LayoutParams;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ScrollView;

import java.io.IOException;
import java.util.Vector;

import jxl.read.biff.BiffException;

public class BenutzerMelodienLayout {
    private final String sourceFileName = "BenutzerMelodienLayout";

    private int benutzerMelodienZeilen = 0;

    public Button homeButton; // neu ab  17.6.13
    //	public Button pfeilLinksButton;
    public Button helpButton; // wird jetzt home Button

    public Vector<Button> sofortStartButtons = new Vector<Button>();
    public Vector<Button> melodie_buttons = new Vector<Button>();
    protected Vector<Button> time_buttons = new Vector<Button>();
    protected Vector<Button> date_buttons = new Vector<Button>();


    private Context context;
    private int displayWidth;
    private int displayHeight;

    private FrameLayout framelayout;

    //private float buchstabenGroesseUhr ;
    //private int buchstabenGroesse ;

    private float buchstabenGroesseIntExcel;

    private Drawable buttonBackgroundOffSofortStartDrawable;
    private Drawable buttonBackgroundOnSofortStartDrawable;
    private Drawable buttonBackgroundOffEinAusDrawable;
    private Drawable buttonBackgroundOnEinAusDrawable;
    private Drawable buttonBackgroundDatumDrawable;
    private Drawable buttonBackgroundUhrzeitDrawable;


    //	private static ExcelRead excelread ;
    private String inputFilename;

    private int zeile1Count;
    private int zeile2Count;

    private ScrollView scroll;

    //private int buttonZeilen = 1 ;

    int button_y_pos;
    int button_x_pos;
    int delta_button_y;
    private ExcelRead excelreadBeschriftungTasten;
    private ExcelRead excelreadTastenGroesse;
    /** Wenn nicht null: Tasten aus DB (beschriftung_tasten), Excel wird für Beschriftung nicht genutzt. */
    private java.util.List<PlatinenDatabaseHelper.BeschriftungTastenRow> beschriftungRowsFromDb;
    private java.util.List<PlatinenDatabaseHelper.BeschriftungTastenRow> sofortStartRowsFromDb;
    private boolean excelBeschriftungOpened = false;

    private float buchstabenGroesseTastenText2;

    //int[] buchstabenXY ;

    private final int BESCHRIFTUNG_TASTEN_SHEET = 0;
    private final int TASTEN_BESCHRIFTUNG_GROESSE_SHEET = 1;

    //private int formularZeilen2 = 0 ;

    public BenutzerMelodienLayout(String fileName, Context context) {

        this.context = context;
        inputFilename = fileName;

        excelreadBeschriftungTasten = new ExcelRead();
        excelreadTastenGroesse = new ExcelRead();
        if (context != null) {
            try {
                java.util.List<PlatinenDatabaseHelper.BeschriftungTastenRow> rows = PlatinenDatabaseHelper.getInstance(context).getBeschriftungTasten();
                if (rows != null && !rows.isEmpty()) {
                    beschriftungRowsFromDb = rows;
                    sofortStartRowsFromDb = new java.util.ArrayList<>();
                    for (PlatinenDatabaseHelper.BeschriftungTastenRow r : rows) {
                        String f = r.c1 != null ? r.c1.trim() : "";
                        if ("Sofort Start".equalsIgnoreCase(f)) sofortStartRowsFromDb.add(r);
                    }
                    benutzerMelodienZeilen = rows.size();
                }
            } catch (Exception e) {
                Log.w(sourceFileName, "Beschriftung aus DB fehlgeschlagen, nutze Excel", e);
            }
        }
        // Nur Datenbank: Kein Excel-Fallback für Beschriftung. Bei leerer DB in der Web-UI „Aus Excel importieren“ nutzen.
        if (beschriftungRowsFromDb == null) {
            benutzerMelodienZeilen = 0;
        }
        try {
            excelreadTastenGroesse.openXlsSheet(inputFilename, TASTEN_BESCHRIFTUNG_GROESSE_SHEET);
        } catch (BiffException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            new LogExcelError(-1, -1, inputFilename, TASTEN_BESCHRIFTUNG_GROESSE_SHEET, sourceFileName, 97);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

//		zeile1Count = getZeile1Count() ;
//		zeile2Count = getZeile2Count() ;

        try {
            String buchstabenGroesseExcellString = excelreadTastenGroesse.getCellString(1, 5);
            buchstabenGroesseIntExcel = Integer.parseInt(buchstabenGroesseExcellString);

            String grafikFilename = excelreadTastenGroesse.getCellString(1, 1);
            buttonBackgroundOffSofortStartDrawable = new BitmapDrawable((BitmapFactory.decodeFile(TurmtechnikActivity.sdCardPath +
                    "/Turmtechnik/Grafiken/" + grafikFilename)));

            grafikFilename = excelreadTastenGroesse.getCellString(2, 1);
            buttonBackgroundOnSofortStartDrawable = new BitmapDrawable((BitmapFactory.decodeFile(TurmtechnikActivity.sdCardPath +
                    "/Turmtechnik/Grafiken/" + grafikFilename)));

            grafikFilename = excelreadTastenGroesse.getCellString(1, 2);
            buttonBackgroundOffEinAusDrawable = buttonBackgroundOnSofortStartDrawable = new BitmapDrawable((BitmapFactory.decodeFile(TurmtechnikActivity.sdCardPath +
                    "/Turmtechnik/Grafiken/" + grafikFilename)));

            grafikFilename = excelreadTastenGroesse.getCellString(2, 2);
            buttonBackgroundOnEinAusDrawable = new BitmapDrawable((BitmapFactory.decodeFile(TurmtechnikActivity.sdCardPath +
                    "/Turmtechnik/Grafiken/" + grafikFilename)));

            grafikFilename = excelreadTastenGroesse.getCellString(1, 3);
            buttonBackgroundDatumDrawable = buttonBackgroundOnSofortStartDrawable = new BitmapDrawable((BitmapFactory.decodeFile(TurmtechnikActivity.sdCardPath +
                    "/Turmtechnik/Grafiken/" + grafikFilename)));

            grafikFilename = excelreadTastenGroesse.getCellString(1, 4);
            buttonBackgroundUhrzeitDrawable = buttonBackgroundOnSofortStartDrawable = new BitmapDrawable((BitmapFactory.decodeFile(TurmtechnikActivity.sdCardPath +
                    "/Turmtechnik/Grafiken/" + grafikFilename)));
        } catch (Exception e) {

        }

        scroll = new ScrollView(context);
//		scroll.setBackgroundColor(android.R.color.transparent);
//		scroll.setBackgroundColor(getResources().getColor(android.R.color.transparent)) ;
        scroll.setBackgroundColor(Color.TRANSPARENT);
        scroll.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT));


        framelayout = new FrameLayout(context);
        FrameLayout.LayoutParams fltable = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.FILL_PARENT, ViewGroup.LayoutParams.FILL_PARENT);
        framelayout.setLayoutParams(fltable);

        displayWidth = TurmtechnikActivity.getDisplayWith();
        displayHeight = TurmtechnikActivity.getDisplayHeight();

        //buchstabenGroesse = StaticConstants.getBuchstabenGroesse(displayWidth);
        //buchstabenGroesseUhr = buchstabenGroesse * 7 ;

        DisplayMetrics metrics = context.getResources().getDisplayMetrics();

        //buchstabenXY = StaticConstants.leseBuchstabenGroesse() ;
        //buchstabenGroesseTastenText = ( (buchstabenXY[12] / metrics.density + 0.5f)) ;
        //buchstabenGroesseUhr = ( ( buchstabenXY[0] / metrics.density + 0.5f)) ;
        buchstabenGroesseTastenText2 = (buchstabenGroesseIntExcel / (metrics.density + 0.5f));

    } // ende Konstruktor Seite2Layout
/*	
           public void initPfeilLinksButton()
		   {
			   int width = displayWidth / 16 ;
			   int height = displayHeight / 10 ;
			  // int x = displayWidth - (displayWidth/50) - width ;
			   
		//	   int x = (displayWidth/50);
		//	   int y = (displayHeight/50);
		
			   pfeilLinksButton = new Button(context);
			   pfeilLinksButton.setBackgroundDrawable(context.getResources().getDrawable(R.drawable.pfeil_links_blau_100x102));
			   FrameLayout.LayoutParams flayoutParams =
			   new FrameLayout.LayoutParams(width, height); 
			   pfeilLinksButton.setLayoutParams(flayoutParams);
			
			   flayoutParams.leftMargin = 20 ;
			   flayoutParams.topMargin = 10 ;
			   flayoutParams.gravity = Gravity.TOP + Gravity.LEFT; 
			   
			   this.framelayout.addView(pfeilLinksButton);
		   }
*/


    public void initHomeButton() {
        //int width = displayWidth / 8 ;
        //int height = displayHeight / 10 ;
        int width = StaticVariable.tastenBreite;
        int height = StaticVariable.tastenHoehe;

        // int x = displayWidth - (displayWidth/50) - width ;

        //	   int x = (displayWidth/50);
        //	   int y = (displayHeight/50);

        homeButton = new Button(context);

        homeButton.setBackgroundDrawable(StaticVariable.btn_home_drawable);
        FrameLayout.LayoutParams flayoutParams =
                new FrameLayout.LayoutParams(width, height);
        homeButton.setLayoutParams(flayoutParams);
        homeButton.setText(StaticVariable.btn_home_textstring);
        //flayoutParams.leftMargin = 20 ;
        flayoutParams.leftMargin = displayWidth / 32;
        flayoutParams.topMargin = 10;
        flayoutParams.gravity = Gravity.TOP + Gravity.LEFT;

        this.framelayout.addView(homeButton);
    }

    public void initHelpButton() {
        //   int width = displayWidth / 8 ;
        //   int height = displayHeight / 10 ;
        int width = StaticVariable.tastenBreite;
        int height = StaticVariable.tastenHoehe;
        //   int x = displayWidth - (displayWidth/50) - width ;
        //   int y = (displayHeight/50);
        helpButton = new Button(context);
        //pfeilRechtsButton.setBackgroundDrawable(context.getResources().getDrawable(R.drawable.pfeil_rechts_blau_100x102));
        helpButton.setBackgroundDrawable(StaticVariable.btn_help_drawable);
        FrameLayout.LayoutParams flayoutParams =
                new FrameLayout.LayoutParams(width, height);
        helpButton.setLayoutParams(flayoutParams);
        helpButton.setText(StaticVariable.btn_help_textstring);

        // flayoutParams.leftMargin = displayWidth - width - 20 ;
        //flayoutParams.leftMargin = width + 40 ;
        flayoutParams.leftMargin = (displayWidth / 32) + width + 20;
        flayoutParams.topMargin = 10;
        flayoutParams.gravity = Gravity.TOP + Gravity.LEFT;

        this.framelayout.addView(helpButton);
    }


    public void berechne_button_width() {
        if (zeile1Count < 3) {
            melodie_button_width = displayWidth / 3;
        } else if (zeile1Count < 5) {
            melodie_button_width = displayWidth / 4;
        } else {
            melodie_button_width = displayWidth / 6;
        }
    }


    int melodie_button_width;
    int melodie_button_height;
    int melodie_button_x;
    int melodie_button_y;


    public void initMelodieButton(int width, int height, int x, int y, int index, String buttonText) {
        Button glockenButton = new Button(context);
        melodie_buttons.add(glockenButton);

        //melodie_buttons.elementAt(index).setTextSize(width / 10.5f);

        melodie_buttons.elementAt(index).setTextSize(buchstabenGroesseTastenText2);


        melodie_buttons.elementAt(index).setText(buttonText);

        FrameLayout.LayoutParams flayoutParams =

                new FrameLayout.LayoutParams(width, height);
        melodie_buttons.elementAt(index).setLayoutParams(flayoutParams);

        melodie_buttons.elementAt(index).setTextColor(this.context.getResources().getColor(R.color.white));


        //if (StaticVariable.firstStartMelodie == true)
        //{
        //	   StaticVariable.benutzerMelodieTasteOn2.add(false) ;
        //}

        //if  (StaticVariable.benutzerMelodieTasteOn2.elementAt(index)==false)
        //{
        //	   setTimerStartButtonGray(index);
        // }
        //else
        //{
        setTimerStartButtonGreen(index);
        //}

        int index_temp = index;
        if (index_temp >= zeile1Count) {
            index_temp = index - zeile1Count + 8;
        }
//			   String buttontext = getButtonText(index_temp);

//			   buttons.elementAt(index).setText(buttontext);

        flayoutParams.leftMargin = x;
        flayoutParams.topMargin = y;
        flayoutParams.gravity = Gravity.TOP + Gravity.LEFT;


        framelayout.addView(melodie_buttons.elementAt(index));

    }

    public void initSofortStartButton(int width, int height, int x, int y, int index, String buttonText) {
        Button sofortStartButton = new Button(context);
        sofortStartButtons.add(sofortStartButton);

        //sofortStartButtons.elementAt(index).setTextSize(width / 10.5f);
        sofortStartButtons.elementAt(index).setTextSize(buchstabenGroesseTastenText2);
        sofortStartButtons.elementAt(index).setText(buttonText);

        FrameLayout.LayoutParams flayoutParams =

                new FrameLayout.LayoutParams(width, height);
        sofortStartButtons.elementAt(index).setLayoutParams(flayoutParams);

        sofortStartButtons.elementAt(index).setTextColor(this.context.getResources().getColor(R.color.white));


        if (StaticVariable.firstStartMelodie == true) {
            StaticVariable.sofortStartButtonOn.add(false);
        }

        if (StaticVariable.sofortStartButtonOn.elementAt(index) == false) {
            setSofortStartButtonGray(index);
        } else {
            setSofortStartButtonGreen(index);
        }

        int index_temp = index;
        if (index_temp >= zeile1Count) {
            index_temp = index - zeile1Count + 8;
        }
//			   String buttontext = getButtonText(index_temp);

//			   buttons.elementAt(index).setText(buttontext);

        flayoutParams.leftMargin = x;
        flayoutParams.topMargin = y;
        flayoutParams.gravity = Gravity.TOP + Gravity.LEFT;


        framelayout.addView(sofortStartButtons.elementAt(index));

    }

    public void makeButtonsStartTime() {

        button_x_pos = displayWidth / 4;
        //button_y_pos = displayHeight / 7 ;
        button_y_pos = StaticVariable.tastenHoehe + 50;
        delta_button_y = (displayHeight / 16) * 3;

        int button_height = displayHeight / 6;
        int button_width = displayWidth / 5;


        //buttonZeilen = 0 ;
        String textTemp = "";


        int dataRows = getBeschriftungDataRowCount();
        for (int i = 0; i < dataRows; i++) {
            try {
                textTemp = getBeschriftungCell(0, i);
                initMelodieButton(button_width, button_height, button_x_pos, button_y_pos, i, textTemp);
                BenutzerMelodienActivity.beschriftungSondermelodien.add(textTemp);
                BenutzerMelodienActivity.filenameSondermelodien.add(getBeschriftungCell(2, i));
                BenutzerMelodienActivity.filenameSondertag.add(getBeschriftungCell(3, i));
                button_y_pos += delta_button_y;
            } catch (Exception e) {
                new LogExcelError(0, i, inputFilename, -3, sourceFileName, 353);
            }
        }
    }

    public void makeButtonsSofortStart() {
        button_x_pos = displayWidth / 32;
        button_y_pos = StaticVariable.tastenHoehe + 50;
        delta_button_y = (displayHeight / 16) * 3;
        int button_height = displayHeight / 6;
        int button_width = displayWidth / 6;
        String textTemp = "";
        if (sofortStartRowsFromDb != null) {
            for (int i = 0; i < sofortStartRowsFromDb.size(); i++) {
                PlatinenDatabaseHelper.BeschriftungTastenRow r = sofortStartRowsFromDb.get(i);
                textTemp = r.c2 != null ? r.c2 : "";
                initSofortStartButton(button_width, button_height, button_x_pos, button_y_pos, i, textTemp);
                BenutzerMelodienActivity.beschriftungSofortstart.add(textTemp);
                button_y_pos += delta_button_y;
            }
            return;
        }
        int dataRows = getBeschriftungDataRowCount();
        for (int i = 0; i < dataRows; i++) {
            try {
                textTemp = excelreadTastenGroesse.getCellString(0, 1);
                String auszufuehrendeMelodie = getBeschriftungCell(2, i).toLowerCase().trim();
                if (!auszufuehrendeMelodie.equals("null")) {
                    initSofortStartButton(button_width, button_height, button_x_pos, button_y_pos, i, textTemp);
                } else {
                    Button sofortStartButton = new Button(context);
                    sofortStartButtons.add(sofortStartButton);
                }
                BenutzerMelodienActivity.beschriftungSofortstart.add(auszufuehrendeMelodie);
                button_y_pos += delta_button_y;
            } catch (Exception e) { }
        }
    }

    public void setTimerStartButtonGreen(int index) {
        //melodie_buttons.elementAt(index).setBackgroundDrawable(context.getResources().getDrawable(R.drawable.btn_green));
        melodie_buttons.elementAt(index).setBackgroundDrawable(buttonBackgroundOnEinAusDrawable);
    }

    //		   public void setButtonRed(int index)
//		   {
//			   buttons.elementAt(index).setBackgroundDrawable(context.getResources().getDrawable(R.drawable.btn_red));
//		   }
    public void setTimerStartButtonGray(int index) {
        melodie_buttons.elementAt(index).setBackgroundDrawable(buttonBackgroundOffEinAusDrawable);
    }

    public void setSofortStartButtonGreen(int index) {
        //sofortStartButtons.elementAt(index).setBackgroundDrawable(context.getResources().getDrawable(R.drawable.btn_green));
        sofortStartButtons.elementAt(index).setBackgroundDrawable(buttonBackgroundOnSofortStartDrawable);
    }

    public void setSofortStartButtonGray(int index) {
        //sofortStartButtons.elementAt(index).setBackgroundDrawable(context.getResources().getDrawable(R.drawable.btn_gray));
        sofortStartButtons.elementAt(index).setBackgroundDrawable(buttonBackgroundOffSofortStartDrawable);
    }

    /*
               public void buttonOff(int localIndex)
               {
                   setButtonGray(localIndex);
                   String buttonText = getButtonText(checkIndexTemp(localIndex));
                   buttons.elementAt(localIndex).setText(buttonText);
               }

               public void buttonOn(int localIndex)
               {
                   //setButtonRed(localIndex);
                   setButtonGreen(localIndex);
                   String buttonText = getButtonText(checkIndexTemp(localIndex));
                   buttons.elementAt(localIndex).setText(buttonText);
               }

    */
    public void initTimeButton(int width, int height, int x, int y, int index) {
        Button timeButton = new Button(context);
        time_buttons.add(timeButton);

        StaticVariable.minuten.add(-1);
        StaticVariable.stunden.add(-1);

        //time_buttons.elementAt(index).setTextSize(height / 3);
        time_buttons.elementAt(index).setTextSize(buchstabenGroesseTastenText2);
        time_buttons.elementAt(index).setBackgroundDrawable(buttonBackgroundUhrzeitDrawable);

        FrameLayout.LayoutParams flayoutParams =

                new FrameLayout.LayoutParams(width, height);
        time_buttons.elementAt(index).setLayoutParams(flayoutParams);

        time_buttons.elementAt(index).setTextColor(this.context.getResources().getColor(R.color.white));

        flayoutParams.leftMargin = x;
        flayoutParams.topMargin = y;
        flayoutParams.gravity = Gravity.TOP + Gravity.LEFT;

        makeTextTimeButton(index);

        framelayout.addView(time_buttons.elementAt(index));
    }

    public void makeXtimeButtons() {
        // berechneNebenUhrCount() ;
        button_x_pos = (displayWidth * 14) / 18;
        //button_y_pos = displayHeight / 7 ;
        button_y_pos = StaticVariable.tastenHoehe + 50;
        delta_button_y = (displayHeight / 16) * 3;

        int button_height = displayHeight / 6;

        int timeButtonWidth = ((displayWidth * 10) / 60);

        int dataRows = getBeschriftungDataRowCount();
        for (int i = 0; i < dataRows; i++) {
            String auszufuehrendeMelodie = getBeschriftungCell(2, i).toLowerCase().trim();
            if (!auszufuehrendeMelodie.equals("null")) {
                initTimeButton(timeButtonWidth, button_height, button_x_pos, button_y_pos, i);
            } else {
                Button timeButton = new Button(context);
                time_buttons.add(timeButton);
            }
            button_y_pos += delta_button_y;
        }
    }

    public void initDateButton(int width, int height, int x, int y, int index) {
        Button timeButton = new Button(context);
        date_buttons.add(timeButton);

        StaticVariable.tage.add(-1);
        StaticVariable.monate.add(-1);
        StaticVariable.jahre.add(-1);

        //date_buttons.elementAt(index).setTextSize(height / 3);
        date_buttons.elementAt(index).setTextSize(buchstabenGroesseTastenText2);
        date_buttons.elementAt(index).setBackgroundDrawable(buttonBackgroundDatumDrawable);

        FrameLayout.LayoutParams flayoutParams =

                new FrameLayout.LayoutParams(width, height);
        date_buttons.elementAt(index).setLayoutParams(flayoutParams);

        date_buttons.elementAt(index).setTextColor(this.context.getResources().getColor(R.color.white));

        flayoutParams.leftMargin = x;
        flayoutParams.topMargin = y;
        flayoutParams.gravity = Gravity.TOP + Gravity.LEFT;

        makeTextDateButton(index);

        framelayout.addView(date_buttons.elementAt(index));
    }

    public void makeXdateButtons() {
        // berechneNebenUhrCount() ;
        button_x_pos = (displayWidth * 5) / 10;
        //button_y_pos = displayHeight / 7 ;
        button_y_pos = StaticVariable.tastenHoehe + 50;
        delta_button_y = (displayHeight / 16) * 3;

        int button_height = displayHeight / 6;

        int dateButtonWidth = ((displayWidth * 10) / 40);

        int dataRows = getBeschriftungDataRowCount();
        for (int i = 0; i < dataRows; i++) {
            initDateButton(dateButtonWidth, button_height, button_x_pos, button_y_pos, i);
            button_y_pos += delta_button_y;
        }
    }

    public void makeTextTimeButton(int index) {
        String buttontext = "Zeit"; // "--:--" ;
        if (StaticVariable.minuten.get(index) != -1) {
            buttontext = (pad(StaticVariable.stunden.get(index)) + ":"
                    + (pad(StaticVariable.minuten.get(index))));
        }
        time_buttons.elementAt(index).setText(buttontext);
    }

    public void makeTextDateButton(int index) {
        String buttontext = "Datum"; // "--.--" ;
        if (StaticVariable.tage.get(index) != -1) {
            buttontext = (pad(StaticVariable.tage.get(index))) + "."
                    + (pad(StaticVariable.monate.get(index))) // bereits 1-12
                    + "." + StaticVariable.jahre.get(index);
        }
        date_buttons.elementAt(index).setText(buttontext);
    }

    private static String pad(int c) {
        if (c >= 10)
            return String.valueOf(c);
        else
            return "0" + String.valueOf(c);
    }


    private int checkIndexTemp(int index) {
        if (index >= zeile1Count) {
            index = index - zeile1Count + 8;
        } else {
            // index = index + 1 ;
        }
        return index;
    }

    public FrameLayout initLayout() {
//		   initClock();
//	       initDatum();
//		   initStopButton();
//		   initPfeilLinksButton();
        initHelpButton();
        initHomeButton();
        makeButtonsSofortStart();
        makeButtonsStartTime();
        makeXtimeButtons();
        makeXdateButtons();
        // readButtonTexts();

        scroll.addView(framelayout);

        // return framelayout;
        return scroll;
    }

    /** Liefert Zellenwert; dataRowIndex = 0-basierter Datenzeilen-Index (bei Excel = Zeile 4, 5, …). */
    private String getBeschriftungCell(int col, int dataRowIndex) {
        if (beschriftungRowsFromDb != null) {
            if (dataRowIndex < 0 || dataRowIndex >= beschriftungRowsFromDb.size()) return "";
            PlatinenDatabaseHelper.BeschriftungTastenRow r = beschriftungRowsFromDb.get(dataRowIndex);
            if (col == 0) return r.c2 != null ? r.c2 : "";
            if (col == 2) return r.c13 != null ? r.c13 : "";
            if (col == 3) return "";
            return "";
        }
        if (!excelBeschriftungOpened || excelreadBeschriftungTasten == null) return "";
        try {
            return excelreadBeschriftungTasten.getCellString(col, dataRowIndex + 4);
        } catch (Exception e) {
            return "";
        }
    }

    /** Anzahl Datenzeilen: bei DB = Liste, bei Excel = Zeilen ab 4. */
    private int getBeschriftungDataRowCount() {
        if (beschriftungRowsFromDb != null) return beschriftungRowsFromDb.size();
        return Math.max(0, benutzerMelodienZeilen - 4);
    }

    public void closeWorkbook() {
        if (excelBeschriftungOpened && excelreadBeschriftungTasten != null) {
            try { excelreadBeschriftungTasten.closeWorkbook(); } catch (Exception e) { }
        }
        if (excelreadTastenGroesse != null) {
            try { excelreadTastenGroesse.closeWorkbook(); } catch (Exception e) { }
        }
    }

}
