package tom.turmtechnik;

import android.content.Context;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.DigitalClock;
import android.widget.FrameLayout;
import android.widget.TextView;

import java.io.IOException;
import java.util.List;
import java.util.Vector;

import jxl.read.biff.BiffException;

public class SetNebenuhrLayout {


    private final String sourceFileName = "SetNebenuhrLayout";
    private final float buchstabenGroesseTimeButton;

    private ExcelRead excelreadTastenbeschriftung;


    public Button pfeilLinksButton;
    public Button helpButton;
//	public Button checkRebootButton ;

    public TextView seite3Text;
    public Vector<Button> buttons = new Vector<Button>();

    private Context context;
    private int displayWidth;
    private int displayHeight;
    private int nebenUhrCount;

    public int[] nebenuhrStunde = new int[4]; // { 0,0,0,0 } ; A, B, C, D
    public int[] nebenuhrMinute = new int[4]; // { 0,0,0,0 } ; A, B, C, D
    public Boolean[] flag_24 = new Boolean[4]; // A, B, C, D

    private FrameLayout framelayout;
    private DigitalClock digitalClock;

    private float buchstabenGroesseUhr;
    private float buchstabenGroesseWochentag;
    private float buchstabenGroesseInfoText;
    private float buchstabenGroesseTastenText;
    private int[] buchstabenXY;

    private final int TASTEN_GROESSE_SHEET = 1;

    private String benutzerMelodienPathAndFilename;


    private int buchstabenGroesse;
    private int buchstabenGroesseExcellTimeButton;

    private String timeButtonHintergrundGrafikName;

    private Drawable time_button_drawable;


    private String[] uhrName = {"A", "B", "C", "D"};

    public SetNebenuhrLayout(Context context) {
        setTimeLocal();

        this.context = context;

        benutzerMelodienPathAndFilename = TurmtechnikActivity.benutzerMelodienFileString;

        excelreadTastenbeschriftung = new ExcelRead();

        try {
            excelreadTastenbeschriftung.openXlsSheet(benutzerMelodienPathAndFilename, TASTEN_GROESSE_SHEET);
        } catch (BiffException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            new LogExcelError(-1, -1, benutzerMelodienPathAndFilename, TASTEN_GROESSE_SHEET, sourceFileName, 79);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        try {
            String buchstabenGroesseString = excelreadTastenbeschriftung.getCellString(1, 7);
            buchstabenGroesseExcellTimeButton = Integer.parseInt(buchstabenGroesseString);
            timeButtonHintergrundGrafikName = excelreadTastenbeschriftung.getCellString(1, 6);
        } catch (Exception e) {
            buchstabenGroesseExcellTimeButton = 22; // Fallback, damit Button-Text auf Nebenuhr-Seite sichtbar bleibt
        }
        excelreadTastenbeschriftung.closeWorkbook();


        framelayout = new FrameLayout(context);
        FrameLayout.LayoutParams fltable = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.FILL_PARENT, ViewGroup.LayoutParams.FILL_PARENT);
        framelayout.setLayoutParams(fltable);

        displayWidth = SetNebenuhrActivity.getDisplayWith();
        displayHeight = SetNebenuhrActivity.getDisplayHeight();

        buchstabenGroesse = StaticConstants.getBuchstabenGroesse(displayHeight);

        DisplayMetrics metrics = context.getResources().getDisplayMetrics();

        buchstabenXY = StaticConstants.leseBuchstabenGroesse();

        // buchstabenGroesse = StaticConstants.getBuchstabenGroesse(displayHeight);
        buchstabenGroesseUhr = (buchstabenXY[0] / metrics.density + 0.5f);
        buchstabenGroesseWochentag = (buchstabenXY[1] / metrics.density + 0.5f);
        buchstabenGroesseInfoText = (buchstabenXY[2] / metrics.density + 0.5f);
        buchstabenGroesseTastenText = ((buchstabenXY[12] / metrics.density + 0.5f));

        float timeButtonSp = (buchstabenGroesseExcellTimeButton > 0 ? buchstabenGroesseExcellTimeButton : 22) / (metrics.density + 0.5f);
        buchstabenGroesseTimeButton = Math.max(timeButtonSp, 14f); // Mindestgröße 14sp, damit Text immer lesbar ist


        //	setTimeLocal() ;

    } // ende Konstruktor Seite2Layout

    private void setTimeLocal() {
        // Nebenuhren A, B, C: Zeit in Stunden und Minuten
        nebenuhrStunde[0] = StaticVariable.uhrA_angezeigteZeit / 60;
        nebenuhrStunde[1] = StaticVariable.uhrB_angezeigteZeit / 60;
        nebenuhrStunde[2] = StaticVariable.uhrC_angezeigteZeit / 60;

        nebenuhrMinute[0] = StaticVariable.uhrA_angezeigteZeit % 60;
        nebenuhrMinute[1] = StaticVariable.uhrB_angezeigteZeit % 60;
        nebenuhrMinute[2] = StaticVariable.uhrC_angezeigteZeit % 60;
        
        // Monduhr D: Mondphase (0-59) - keine Stunden/Minuten, sondern Mondphase
        // Für Anzeige: Mondphase als "Phase X" anzeigen
        nebenuhrStunde[3] = StaticVariable.uhrD_mondphaseIst; // Mondphase als "Stunde" speichern (für Anzeige)
        nebenuhrMinute[3] = 0; // Nicht verwendet für Monduhr
    }

    private void initHomeButton() {
        //	   int width = displayWidth / 10 ;
        //	   int height = displayHeight / 10 ;

        int width = StaticVariable.tastenBreite;
        int height = StaticVariable.tastenHoehe;

        String stringTemp = StaticVariable.tastenBreite + "/" + StaticVariable.tastenHoehe;
        //	   Log.e("breite/hoehe" , "=" + stringTemp ) ;

        //	   int x = displayWidth - (displayWidth/50) - width ;
        //	   int y = (displayHeight/50);
        pfeilLinksButton = new Button(context);
        //pfeilLinksButton.setBackgroundDrawable(context.getResources().getDrawable(R.drawable.pfeil_links_blau_100x102));
        pfeilLinksButton.setBackgroundDrawable(StaticVariable.btn_home_drawable);
        pfeilLinksButton.setText(StaticVariable.btn_home_textstring);
        pfeilLinksButton.setTextSize(buchstabenGroesseTastenText);


        FrameLayout.LayoutParams flayoutParams =
                new FrameLayout.LayoutParams(width, height);
        pfeilLinksButton.setLayoutParams(flayoutParams);

        //   flayoutParams.leftMargin = 20 ;  // ist gleich  x position
        //   flayoutParams.topMargin = 10 ;   // ist gleich  y position

        flayoutParams.leftMargin = 20;
        flayoutParams.topMargin = 10;


        flayoutParams.gravity = Gravity.TOP + Gravity.LEFT;

        this.framelayout.addView(pfeilLinksButton);
    }


    private void initHelpButton() {
        //	   int width = displayWidth / 10 ;
        //	   int height = displayHeight / 10 ;
        int width = StaticVariable.tastenBreite;
        int height = StaticVariable.tastenHoehe;
        //	   int x = displayWidth - (displayWidth/50) - width ;
        //	   int y = (displayHeight/50);
        helpButton = new Button(context);
        //pfeilLinksButton.setBackgroundDrawable(context.getResources().getDrawable(R.drawable.pfeil_links_blau_100x102));
        helpButton.setBackgroundDrawable(StaticVariable.btn_help_drawable);
        helpButton.setText(StaticVariable.btn_help_textstring);
        helpButton.setTextSize(buchstabenGroesseTastenText);
//			   pfeilLinksButton.setTextColor(0xffffff); // geht nicht ueber resources !!??

        FrameLayout.LayoutParams flayoutParams =
                new FrameLayout.LayoutParams(width, height);
        helpButton.setLayoutParams(flayoutParams);

        //flayoutParams.leftMargin = 20 ;  // ist gleich  x position
        flayoutParams.leftMargin = 10;
        //flayoutParams.topMargin = 10 ;   // ist gleich  y position
        flayoutParams.topMargin = 10;
        flayoutParams.gravity = Gravity.TOP + Gravity.RIGHT;

        this.framelayout.addView(helpButton);
    }

/*/home/afred/Dropbox/Android-Projekte/Turmtechnik-studio2-vorschwingen/src/com/awr_technology/turmtechnik
           private void initRebootButton()
		   {
		//	   int width = displayWidth / 10 ;
		//	   int height = displayHeight / 10 ;
			   int width = StaticVariable.tastenBreite  ;
			   int height = StaticVariable.tastenHoehe ;
		//	   int x = displayWidth - (displayWidth/50) - width ;
		//	   int y = (displayHeight/50);
			   checkRebootButton = new Button(context);
			   //pfeilLinksButton.setBackgroundDrawable(context.getResources().getDrawable(R.drawable.pfeil_links_blau_100x102));
			   checkRebootButton.setBackgroundDrawable(StaticVariable.btn_home_drawable);
			   checkRebootButton.setText("TEST reboot");
			  
//			   pfeilLinksButton.setTextColor(0xffffff); // geht nicht ueber resources !!??
			   
			   FrameLayout.LayoutParams flayoutParams =
			   new FrameLayout.LayoutParams(width, height); 
			   checkRebootButton.setLayoutParams(flayoutParams);
			
			   //flayoutParams.leftMargin = 20 ;  // ist gleich  x position
			   flayoutParams.leftMargin = (width  ) + ( (width / 10) * 70) ;
			   //flayoutParams.topMargin = 10 ;   // ist gleich  y position
			   flayoutParams.topMargin = displayHeight - height - 50 ;
			   flayoutParams.gravity = Gravity.TOP + Gravity.LEFT; 
			   
			   this.framelayout.addView(checkRebootButton);
		   }
*/

    private void initTextFeld() {
        int width = displayWidth - (displayWidth / 4);
        int height = displayHeight / 4;

        seite3Text = new TextView(context);
        FrameLayout.LayoutParams textParams =
                new FrameLayout.LayoutParams(width, height);
        seite3Text.setLayoutParams(textParams);
        textParams.leftMargin = 10;
        textParams.topMargin = 10;
        textParams.gravity = Gravity.TOP + Gravity.CENTER_HORIZONTAL;

        seite3Text.setTextColor(this.context.getResources().getColor(R.color.black));

        //seite3Text.setTextSize(buchstabenGroesse * 2 );
        seite3Text.setTextSize(buchstabenGroesseInfoText);
        //
        seite3Text.setGravity(Gravity.CENTER_HORIZONTAL);
        //seite3Text.setText("Zeit von Nebenuhr eingeben");
        seite3Text.setText(StaticVariable.getUebersetzung(10)) ;
        this.framelayout.addView(seite3Text);
    }

    public void initTimeButton(int width, int height, int x, int y, int index) {
        // Sicherheitsprüfung: Prüfe ob Index gültig ist
        if (index < 0 || index >= uhrName.length) {
            android.util.Log.e("SetNebenuhrLayout", "Ungültiger Index in initTimeButton: " + index + " (uhrName.length=" + uhrName.length + ")");
            return;
        }
        
        Button timeButton = new Button(context);
        buttons.add(timeButton);
        
        // Prüfe ob Button erfolgreich hinzugefügt wurde
        if (buttons.size() <= index) {
            android.util.Log.e("SetNebenuhrLayout", "Button konnte nicht hinzugefügt werden, Index: " + index);
            return;
        }

        //buttons.elementAt(index).setTextSize(height / 7);
        buttons.elementAt(index).setTextSize(buchstabenGroesseTimeButton);

        FrameLayout.LayoutParams flayoutParams =
                new FrameLayout.LayoutParams(width, height);
        buttons.elementAt(index).setLayoutParams(flayoutParams);

        android.graphics.Bitmap bmp = null;
        if (TurmtechnikActivity.sdCardPath != null && timeButtonHintergrundGrafikName != null && !timeButtonHintergrundGrafikName.trim().isEmpty()) {
            bmp = BitmapFactory.decodeFile(TurmtechnikActivity.sdCardPath +
                    "/Turmtechnik/Grafiken/" + timeButtonHintergrundGrafikName);
        }
        if (bmp != null) {
            time_button_drawable = new BitmapDrawable(bmp);
            buttons.elementAt(index).setBackgroundDrawable(time_button_drawable);
            buttons.elementAt(index).setTextColor(Color.WHITE);
        } else {
            buttons.elementAt(index).setBackgroundColor(Color.parseColor("#999999")); // light_gray
            buttons.elementAt(index).setTextColor(Color.BLACK);
        }

        flayoutParams.leftMargin = x;
        flayoutParams.topMargin = y;
        flayoutParams.gravity = Gravity.TOP + Gravity.LEFT;

        makeTextButton(index);

        framelayout.addView(buttons.elementAt(index));
    }

    public void makeTextButton(int index) {
        // Sicherheitsprüfung: Prüfe ob Index gültig ist
        if (index < 0 || index >= buttons.size() || index >= uhrName.length) {
            android.util.Log.e("SetNebenuhrLayout", "Ungültiger Index in makeTextButton: " + index + " (buttons.size()=" + buttons.size() + ", uhrName.length=" + uhrName.length + ")");
            return;
        }
        
        String buttontext;
        
        // Prüfe ob es die Monduhr D ist (index 3 oder uhrName[index] == "D")
        if (index == 3 || (uhrName.length > index && "D".equals(uhrName[index]))) {
            // Monduhr D: Mond als Bild auf der Taste, darunter "D" und Phase
            int mondphase = StaticVariable.uhrD_mondphaseIst;
            buttontext = (uhrName[index] + "\n"
                    + "Phase " + mondphase);
            android.graphics.drawable.Drawable moonDrawable = SetNebenuhrActivity.createMoonPhaseDrawable(context, mondphase, 72);
            if (moonDrawable != null) {
                moonDrawable.setBounds(0, 0, 72, 72);
                buttons.elementAt(index).setCompoundDrawables(null, moonDrawable, null, null);
                buttons.elementAt(index).setCompoundDrawablePadding(4);
            }
        } else {
            // Nebenuhren A, B, C: Zeige Zeit
            if (index < nebenuhrStunde.length && index < nebenuhrMinute.length) {
                buttontext = (uhrName[index] + "\n"
                        + (pad(nebenuhrStunde[index])) + ":" + (pad(nebenuhrMinute[index])));
            } else {
                android.util.Log.e("SetNebenuhrLayout", "Index außerhalb des Arrays: " + index);
                return;
            }
        }

        buttons.elementAt(index).setText(buttontext);
    }

    public void printTasten() {
        setTimeLocal();
        // Aktualisiere alle Buttons (A, B, C, D)
        for (int i = 0; i < nebenUhrCount && i < buttons.size(); i++) {
            makeTextButton(i);
        }
    }

    private static String pad(int c) {
        if (c >= 10)
            return String.valueOf(c);
        else
            return "0" + String.valueOf(c);
    }


    public void makeXtimeButtons() {
        berechneNebenUhrCountAndGetNames();
        
        // Tasten in % der Auflösung – gleiche Aufteilung bei allen Bildschirmen
        int paddingHorizontal = Math.max(24, (int)(displayWidth * StaticConstants.NEBENUHR_PADDING_H_PERCENT));
        int gapBetweenButtons = Math.max(16, (int)(displayWidth * StaticConstants.NEBENUHR_GAP_PERCENT));
        int gapBetweenRows = Math.max(12, (int)(displayHeight * StaticConstants.NEBENUHR_ROW_GAP_PERCENT));

        int timeButtonWidth = (displayWidth - 2 * paddingHorizontal - gapBetweenButtons) / 2;
        int timeButtonHeight = (int)(displayHeight * StaticConstants.NEBENUHR_TIME_BUTTON_HEIGHT_PERCENT);

        int blockTotalWidth = 2 * timeButtonWidth + gapBetweenButtons;
        int startX = (displayWidth - blockTotalWidth) / 2;

        int timeButtonY1 = (int)(displayHeight * StaticConstants.NEBENUHR_ROW1_Y_PERCENT);
        int timeButtonY2 = timeButtonY1 + timeButtonHeight + gapBetweenRows;

        int maxButtons = Math.min(nebenUhrCount, 4);
        for (int i = 0; i < maxButtons; i++) {
            int x, y;
            if (i < 2) {
                x = startX + i * (timeButtonWidth + gapBetweenButtons);
                y = timeButtonY1;
            } else {
                x = startX + (i - 2) * (timeButtonWidth + gapBetweenButtons);
                y = timeButtonY2;
            }
            initTimeButton(timeButtonWidth, timeButtonHeight, x, y, i);
        }
    }

    private void berechneNebenUhrCountAndGetNames() {
        nebenUhrCount = 0;

        PlatinenDatabaseHelper dbHelper = PlatinenDatabaseHelper.getInstance(TurmtechnikActivity.turmtechnikContext);
        List<PlatinenDatabaseHelper.NebenuhrConfig> nebenuhren = dbHelper.getAllNebenuhren();

        if (nebenuhren != null && !nebenuhren.isEmpty()) {
            // Lade aus Datenbank - sortiere nach zeile_index (3=A, 4=B, 5=C, 6=D)
            java.util.Collections.sort(nebenuhren, new java.util.Comparator<PlatinenDatabaseHelper.NebenuhrConfig>() {
                @Override
                public int compare(PlatinenDatabaseHelper.NebenuhrConfig a, PlatinenDatabaseHelper.NebenuhrConfig b) {
                    return Integer.compare(a.zeile, b.zeile);
                }
            });
            
            for (PlatinenDatabaseHelper.NebenuhrConfig config : nebenuhren) {
                if (nebenUhrCount < uhrName.length && nebenUhrCount < 4) {
                    // Immer 12-Stunden-Modus für A, B, C
                    // Monduhr D hat keinen 12/24-Modus
                    if (!"MOND".equals(config.modus)) {
                        flag_24[nebenUhrCount] = false;
                    } else {
                        flag_24[nebenUhrCount] = false; // Auch für Monduhr D false (nicht verwendet)
                    }
                    uhrName[nebenUhrCount] = (config.uhrNameDisplay != null) ? config.uhrNameDisplay : config.uhrName;
                    nebenUhrCount++;
                }
            }
        }
        
        // Fallback: Wenn keine Nebenuhren in DB, mindestens 4 Slots anzeigen (Seite nicht leer)
        if (nebenUhrCount == 0) {
            android.util.Log.w("SetNebenuhrLayout", "Keine Nebenuhr-Konfiguration gefunden, zeige Standard A/B/C/D");
            uhrName[0] = "Nebenuhr A";
            uhrName[1] = "Nebenuhr B";
            uhrName[2] = "Nebenuhr C";
            uhrName[3] = "Monduhr D";
            for (int i = 0; i < 4; i++) flag_24[i] = false;
            nebenUhrCount = 4;
        }

        if (nebenUhrCount < 3) {
            android.util.Log.w("SetNebenuhrLayout", "Warnung: Nur " + nebenUhrCount + " Nebenuhren gefunden, erwartet mindestens 3");
        }

        //Log.d("nebenUhrCount" , "=" + nebenUhrCount) ;
    }

    public void initClock() {
        int clock_width = (int)(displayWidth * StaticConstants.NEBENUHR_CLOCK_WIDTH_PERCENT);
        int clock_height = (int)(displayHeight * StaticConstants.NEBENUHR_CLOCK_HEIGHT_PERCENT);

        digitalClock = new DigitalClock(this.context);

        FrameLayout.LayoutParams flayoutParams =
                new FrameLayout.LayoutParams(clock_width, clock_height);
        digitalClock.setLayoutParams(flayoutParams);

        digitalClock.setTextSize(buchstabenGroesseUhr);

        flayoutParams.topMargin = (int)(displayHeight * StaticConstants.NEBENUHR_CLOCK_Y_PERCENT);
        flayoutParams.gravity = Gravity.TOP + Gravity.CENTER_HORIZONTAL;

        digitalClock.setTextColor(this.context.getResources().getColor(R.color.black));

        digitalClock.setGravity(Gravity.CENTER_HORIZONTAL);

        this.framelayout.addView(digitalClock);
    }

    public FrameLayout initLayout() {
        makeXtimeButtons();
        initHomeButton();
        initHelpButton();
        //initRebootButton();
        initTextFeld();
        initClock();

        return framelayout;
    }

} // ende von Seite3layout 
