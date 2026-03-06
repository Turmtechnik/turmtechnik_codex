package tom.turmtechnik;

import android.content.Context;
import android.graphics.Color;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsoluteLayout;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Vector;

import jxl.read.biff.BiffException;

//import android.util.Log;

public class Seite2Layout {
    private final String sourceFileName = "Seite2Layout";
    private final int buchstabenFarbe;

    private int seite2MarginTop = 50;

    //	public Button pfeilRechtsButton ;
//	public Button stopButton ;
//	public Button automaticButton ;
    public TextView infoText;
    public static Vector<Button> buttons; // = new Vector<Button>(); // die buttons auf Seite2

    private final int BESCHRIFTUNG_GLOCKEN_SHEET = 0;
    //	private final int FARBEN_SHEET_NUMBER = 16 ;
    private final int FARBEN_SHEET_NUMBER = 0; // die Tastenhintergrund Grafik namen auf Glocken sheet gegeben
    // 11.6.13

    public static final int ANFANG_SEITE2_INDEX = 25;


//	private Drawable btn_normal_drawable ;
//	private Drawable btn_manual_ein_ok_drawable ;
//	private Drawable btn_manual_ein_error_drawable ;
//	private Drawable btn_automatic_ein_ok_drawable ;
//	private Drawable btn_automatic_ein_error_drawable ;

    private ArrayList<Drawable> btn_normal_drawable = new ArrayList<Drawable>();
    private ArrayList<Drawable> btn_manual_ein_ok_drawable = new ArrayList<Drawable>();
    private ArrayList<Drawable> btn_manual_ein_error_drawable = new ArrayList<Drawable>();
    private ArrayList<Drawable> btn_automatic_ein_ok_drawable = new ArrayList<Drawable>();
    private ArrayList<Drawable> btn_automatic_ein_error_drawable = new ArrayList<Drawable>();

    private static Vector<String> beschriftung = new Vector<String>();
    /** true wenn Seite-2-Beschriftung aus DB (Zeilen 24..47) geladen wurde */
    private static boolean beschriftungSeite2FromDb = false;
//	private Vector<Integer> relaisnumber = new Vector<Integer>();
//	private Vector<Integer> hammerzeit = new Vector<Integer>();

    private static TextView datum;

    private Context context;
    private static AbsoluteLayout absolutelayout;
    //private DigitalClock digitalClock ;

    private int displayWidth;

    private static TextView uhr;

    private int displayHeight;

    private float buchstabenGroesseUhr;
    private float buchstabenGroesseWochentag;
    private float buchstabenGroesseInfoText;
    private float buchstabenGroesseTastenText;
    private int[] buchstabenXY;

    //private int buchstabenGroesse ;


    private float scale;

    private int heightView = ViewGroup.LayoutParams.WRAP_CONTENT;
    private int widthView = ViewGroup.LayoutParams.FILL_PARENT;
    //	private Button glockenButton ;
    private int glockenCount;
    private int kloepelFaengerCount;
    private int gruppeDreiCount; // ab 26.3.2013 eine dritte zeile mit Tasten
    private String inputFilename;

    /** Mapping Slot (0..23) → Button-Index in buttons-Vector. Für Klick-Handler: Tag = Slot-Index, Anzeige-Update braucht Button-Index. */
    private final int[] slotToButtonIndex = new int[24];
    /** Button-Index → Slot-Index (0..23), für getButtonText in buttonOff/buttonOnOK. */
    private final int[] buttonToSlotIndex = new int[24];

    private Button buttonTemp;

    private int index;
    private static ExcelRead excelread;
    private static ExcelRead farbenExcelread;
    private String button_farbe_name;
    /** Zeilen 24–47 aus DB für Seite-2-Farben (c1 = Typ), wenn kein Excel. */
    private java.util.List<PlatinenDatabaseHelper.BeschriftungTastenRow> rowsSeite2FromDb;

    public Seite2Layout(String fileName, Context context) {
        this.context = context;
        inputFilename = fileName;
        buttons = new Vector<Button>(); // die buttons auf Seite2

        // DB-Zeilen für Seite 2 (24–47) vorab laden, damit loadFarben bei fehlendem Excel Vorlagen-Farben nutzen kann
        try {
            if (context != null) {
                java.util.List<PlatinenDatabaseHelper.BeschriftungTastenRow> all = PlatinenDatabaseHelper.getInstance(context.getApplicationContext()).getBeschriftungTasten();
                if (all != null && all.size() >= 48) {
                    rowsSeite2FromDb = new java.util.ArrayList<>(all.subList(24, 48));
                }
            }
        } catch (Exception e) {
            Log.e(sourceFileName, "DB-Zeilen Seite 2 für Farben", e);
        }
        if (rowsSeite2FromDb == null) rowsSeite2FromDb = new java.util.ArrayList<>();

        // die Button Hintergruende: Excel oder DB-Vorlagen (Typ-Farbe)
        farbenExcelread = new ExcelRead();
        try {
            farbenExcelread.openXlsSheet(inputFilename, FARBEN_SHEET_NUMBER);
        } catch (BiffException e) {
            e.printStackTrace();
            farbenExcelread = null;
            new LogExcelError(-1, -1, inputFilename, FARBEN_SHEET_NUMBER, sourceFileName, 115);
        } catch (IOException e) {
            e.printStackTrace();
            farbenExcelread = null;
            new LogExcelError(-1, -1, inputFilename, FARBEN_SHEET_NUMBER, sourceFileName, 119);
        }

        // Achtung für jede Taste extra !!!!  10.6.13
        // wird jetzt in jeder Taste erledigt 11.6.13
        // geht nicht -- viel zu langsam!!! 11.6.13
        // beim Start werden die Grafiken in einem ArrayList gespeichert
/*		
        button_farbe_name = farbenExcelread.getCellString(6, 1); // btn-normal.png
		Log.i("BUTTON_FARBE_NAME" , "=" + button_farbe_name) ;
		btn_normal_drawable = new BitmapDrawable((BitmapFactory.decodeFile(TurmtechnikActivity.sdCardPath + "/Turmtechnik/Grafiken/" + button_farbe_name)));
		
		button_farbe_name = farbenExcelread.getCellString(7, 1) ;
		btn_manual_ein_ok_drawable = new BitmapDrawable((BitmapFactory.decodeFile(TurmtechnikActivity.sdCardPath + "/Turmtechnik/Grafiken/" + button_farbe_name )));
		
		button_farbe_name = farbenExcelread.getCellString(8, 1) ;
		btn_manual_ein_error_drawable = new BitmapDrawable((BitmapFactory.decodeFile(TurmtechnikActivity.sdCardPath + "/Turmtechnik/Grafiken/" + button_farbe_name)));
		
		button_farbe_name = farbenExcelread.getCellString(9, 1) ;
		btn_automatic_ein_ok_drawable = new BitmapDrawable((BitmapFactory.decodeFile(TurmtechnikActivity.sdCardPath + "/Turmtechnik/Grafiken/" + button_farbe_name)));
		
		button_farbe_name = farbenExcelread.getCellString(10, 1) ;
		btn_automatic_ein_error_drawable = new BitmapDrawable((BitmapFactory.decodeFile(TurmtechnikActivity.sdCardPath + "/Turmtechnik/Grafiken/" + button_farbe_name)));
*/
        //Log.e("loadFarben" , "AUFGERUFEN") ;
        loadFarben();

        if (farbenExcelread != null) {
            try { farbenExcelread.closeWorkbook(); } catch (Exception ignored) { }
        }


        beschriftungSeite2FromDb = false;
        try {
            if (context != null) {
                java.util.List<PlatinenDatabaseHelper.BeschriftungTastenRow> rows = PlatinenDatabaseHelper.getInstance(context.getApplicationContext()).getBeschriftungTasten();
                if (rows != null && rows.size() >= 48) {
                    beschriftung.clear();
                    for (int i = 24; i < 48 && i < rows.size(); i++) {
                        PlatinenDatabaseHelper.BeschriftungTastenRow row = rows.get(i);
                        String c2 = row.c2 != null ? row.c2.trim() : "";
                        if (c2.isEmpty() && Seite1Layout.isSystemTasteRow(row))
                            c2 = row.c1 != null ? row.c1.trim() : "";
                        beschriftung.add(c2);
                    }
                    beschriftungSeite2FromDb = true;
                }
            }
        } catch (Exception e) {
            Log.e(sourceFileName, "DB-Lesen Beschriftung Seite 2", e);
        }

        // Nur Datenbank: Kein Excel-Fallback. Bei weniger als 48 Zeilen in der DB in der Web-UI „Aus Excel importieren“ nutzen.
        if (!beschriftungSeite2FromDb) {
            excelread = null;
        } else {
            excelread = null;
        }

        glockenCount = getGlockenCount();
        kloepelFaengerCount = getKloeppelCount();
        gruppeDreiCount = getGruppeDreiCount();

        absolutelayout = new AbsoluteLayout(context);
        AbsoluteLayout.LayoutParams altable = new AbsoluteLayout.LayoutParams(
                ViewGroup.LayoutParams.FILL_PARENT,
                ViewGroup.LayoutParams.FILL_PARENT, 0, 0);


        absolutelayout.setLayoutParams(altable);


        displayWidth = TurmtechnikActivity.getDisplayWith();
        displayHeight = TurmtechnikActivity.getDisplayHeight();

        seite2MarginTop = (int)(displayHeight * StaticConstants.LAYOUT1_SEITE2_MARGIN_TOP_PERCENT);
        // buchstabenGroesseUhr = (displayHeight / 12 ); //  / 6) ;
        // buchstabenGroesse = ( displayHeight /28 );      // 14 ;
//		buchstabenGroesseUhr = ( displayWidth / 16 );
//  	buchstabenGroesse = ( displayWidth / 44);

        //	buchstabenGroesseUhr = (int) ( displayWidth * 0.06f);
        //	buchstabenGroesse = (int) ( displayWidth * 0.02f );

        //	scale = context.getResources().getDisplayMetrics().density;
        //	scale = ( (context.getResources().getDisplayMetrics().xdpi) *
        //			  (context.getResources().getDisplayMetrics().ydpi) ) / 22000;

        DisplayMetrics metrics = context.getResources().getDisplayMetrics();

        buchstabenXY = StaticConstants.leseBuchstabenGroesse();

        float textPxToSp = 1f / metrics.density;
        buchstabenGroesseUhr = (buchstabenXY[0] > 0)
                ? ((buchstabenXY[0] * textPxToSp) + 0.5f)
                : (displayHeight * StaticConstants.LAYOUT1_TEXT_CLOCK_PERCENT * textPxToSp);
        buchstabenGroesseWochentag = (buchstabenXY.length > 1 && buchstabenXY[1] > 0)
                ? ((buchstabenXY[1] * textPxToSp) + 0.5f)
                : (displayHeight * StaticConstants.LAYOUT1_TEXT_WOCHENTAG_PERCENT * textPxToSp);
        buchstabenGroesseInfoText = (buchstabenXY.length > 2 && buchstabenXY[2] > 0)
                ? ((buchstabenXY[2] * textPxToSp) + 0.5f)
                : (displayHeight * StaticConstants.LAYOUT1_TEXT_INFO_PERCENT * textPxToSp);
        buchstabenGroesseTastenText = (buchstabenXY.length > 12 && buchstabenXY[12] > 0)
                ? ((buchstabenXY[12] * textPxToSp) + 0.5f)
                : (displayHeight * StaticConstants.LAYOUT1_TEXT_TASTEN_PERCENT * textPxToSp);

        buchstabenFarbe = buchstabenXY[14] + 0xFF000000; // + Alpha Kanal!

    } // ende Konstruktor Seite1Layout

    public static int getSeite2ButtonsSize() {
        return buttons.size();
    }


    public void initLogo() {
        int logo_x = (int)(displayWidth * StaticConstants.LAYOUT1_LOGO_X_PERCENT) + buchstabenXY[9];
        int logo_y = (int)(displayHeight * StaticConstants.LAYOUT1_LOGO_Y_PERCENT) + buchstabenXY[10] + seite2MarginTop;
        int logo_with = (int)(displayWidth * StaticConstants.LAYOUT1_LOGO_SIZE_PERCENT) + buchstabenXY[11];


//		  int logo_height = ( displayHeight * 114 )  / 400  ;


        ImageView logoView = new ImageView(this.context);

        //logoView.setImageResource(R.raw.kirchturmtechnik_logo4) ;
        logoView.setImageResource(R.drawable.kirchturmtechnik_logo4);

        AbsoluteLayout.LayoutParams alayoutParams =
                //	  new AbsoluteLayout.LayoutParams(LayoutParams.WRAP_CONTENT,LayoutParams.WRAP_CONTENT, logo_x, logo_y);
                new AbsoluteLayout.LayoutParams(logo_with, logo_with, logo_x, logo_y);
        logoView.setLayoutParams(alayoutParams);

        this.absolutelayout.addView(logoView);

    }

    /*
    public void initClock()

      {
           int clock_width = displayWidth /2 ;
           int clock_height = displayHeight / 12 ;
        //  int clock_width = 200;
         // int clock_x = ( displayWidth /2 )  - (clock_width /2  ) ;

           int clock_x = ( displayWidth /2 )  - (clock_width /2  ) + buchstabenXY[3] ;
           int clock_y = 0 + buchstabenXY[4] ;
          //Log.d("displayW", " = " + displayWidth);
          digitalClock = new DigitalClock(this.context);



          AbsoluteLayout.LayoutParams alayoutParams =
                  new AbsoluteLayout.LayoutParams(clock_width, heightView , clock_x , clock_y);
          digitalClock.setLayoutParams(alayoutParams);

          //   digitalClock.setTextSize(TypedValue.COMPLEX_UNIT_SP,buchstabenGroesseUhr);
          digitalClock.setTextSize(buchstabenGroesseUhr);


//	      digitalClock.setMaxHeight(64);
//	      digitalClock.setMaxWidth(60);
          digitalClock.setTextColor(this.context.getResources().getColor(R.color.black));
//	      digitalClock.setBackgroundColor(this.context.getResources().getColor(R.color.red));
          digitalClock.setGravity(Gravity.CENTER_HORIZONTAL);

          this.absolutelayout.addView(digitalClock);
//        digitalClock.setId(1);
          //      return this.linlayout;
       }
      */
    public void initClock() {
        int clock_width = (int)(displayWidth * StaticConstants.LAYOUT1_CLOCK_WIDTH_PERCENT);
        int clock_height = (int)(displayHeight * StaticConstants.LAYOUT1_CLOCK_HEIGHT_PERCENT);
        int clock_x = (displayWidth / 2) - (clock_width / 2) + buchstabenXY[3];
        int clock_y = seite2MarginTop + buchstabenXY[4];
        ///Log.d("displayW", " = " + displayWidth);
        //digitalClock = new DigitalClock(this.context);
        uhr = new TextView(this.context);


        AbsoluteLayout.LayoutParams alayoutParams =
                new AbsoluteLayout.LayoutParams(clock_width, heightView, clock_x, clock_y);
        //digitalClock.setLayoutParams(alayoutParams);
        uhr.setLayoutParams(alayoutParams);
        //   digitalClock.setTextSize(TypedValue.COMPLEX_UNIT_SP,buchstabenGroesseUhr);
        //digitalClock.setTextSize( buchstabenGroesseUhr);
        uhr.setTextSize(buchstabenGroesseUhr);

//	      digitalClock.setMaxHeight(64);
//	      digitalClock.setMaxWidth(60);
        //digitalClock.setTextColor(this.context.getResources().getColor(R.color.black));
        uhr.setTextColor(this.context.getResources().getColor(R.color.black));
//	      digitalClock.setBackgroundColor(this.context.getResources().getColor(R.color.red));
        //digitalClock.setGravity(Gravity.CENTER_HORIZONTAL);
        uhr.setGravity(Gravity.CENTER_HORIZONTAL);

        //this.absolutelayout.addView(digitalClock);
        this.absolutelayout.addView(uhr);
        //digitalClock.setId(1);
        //      return this.linlayout;
    }

    public static void printUhr() {
        uhr.setText(getUhrzeit());
    }

    public static String getUhrzeit() {
        Date dt = new Date();
        SimpleDateFormat df = new SimpleDateFormat("HH:mm:ss");
        return df.format(dt);
    }

    public static String getDatum() {
        Date dt = new Date();
        SimpleDateFormat df = new SimpleDateFormat("dd.MM.yyyy");
        return df.format(dt);
    }

    public static String getWochentag() {
        GregorianCalendar date = new GregorianCalendar();
        int day;

        //String[] weekDayNames = {"SONNTAG", "MONTAG", "DIENSTAG", "MITTWOCH",
        //        "DONNERSTAG", "FREITAG", "SAMSTAG"};
        String[] weekDayNames = new String[7] ;

        for (int i = 0 ; i < 7 ; i ++ )
        {
            weekDayNames[i] = StaticVariable.getUebersetzung(i+1) ;
        }


        day = date.get(Calendar.DAY_OF_WEEK);
        return weekDayNames[day - 1];
    }


    public void initDatum() {
// 		   int datum_width = displayWidth /2 ;
//		   int datum_x = ( displayWidth /2 )  - (datum_width /2  ) ;
//		   int datum_y = displayHeight / 10 ;


        int datum_width = (int)(displayWidth * StaticConstants.LAYOUT1_DATUM_WIDTH_PERCENT);
        int datum_x = (int)(displayWidth * StaticConstants.LAYOUT1_DATUM_X_PERCENT) + buchstabenXY[5];
        int datum_y = (int)(displayHeight * StaticConstants.LAYOUT1_DATUM_Y_PERCENT) + buchstabenXY[6] + seite2MarginTop;


//		   Log.i("displayH", " = " + displayHeight);

        datum = new TextView(this.context);

        AbsoluteLayout.LayoutParams alayoutParams =
                new AbsoluteLayout.LayoutParams(datum_width, heightView, datum_x, datum_y);
        datum.setLayoutParams(alayoutParams);

        datum.setTextColor(this.context.getResources().getColor(R.color.black));
//		   datum.setBackgroundColor(this.context.getResources().getColor(R.color.green));

//		   datum.setTextSize(TypedValue.COMPLEX_UNIT_SP,( buchstabenGroesse  ));
        datum.setTextSize(buchstabenGroesseWochentag);
        datum.setGravity(Gravity.CENTER_HORIZONTAL);
        datum.setText(getWochentag() + "\n");

        //	   datum.setTextSize(TypedValue.COMPLEX_UNIT_DIP,( buchstabenGroesse * 0.7f ));
        datum.append(getDatum());
        //   datum.setGravity(Gravity.CENTER_HORIZONTAL);

        this.absolutelayout.addView(datum);
    }

    public void initInfoText() {
        int infotext_width = (int)(displayWidth * StaticConstants.LAYOUT1_INFOTEXT_WIDTH_PERCENT);
        int infotext_height = (int)(displayHeight * StaticConstants.LAYOUT1_INFOTEXT_HEIGHT_PERCENT);
        int infotext_x = 0;
        int infotext_y = (int)(displayHeight * (StaticConstants.LAYOUT1_INFOTEXT_Y_PERCENT_TOP - StaticConstants.LAYOUT1_INFOTEXT_Y_PERCENT_OFFSET)) + buchstabenXY[8] + seite2MarginTop;

        infoText = new TextView(context);

        //stopButton.setBackgroundDrawable(context.getResources().getDrawable(R.drawable.stop_180x179_transparent));
        AbsoluteLayout.LayoutParams alayoutParams =
                new AbsoluteLayout.LayoutParams(infotext_width, infotext_height, infotext_x, infotext_y);
        infoText.setLayoutParams(alayoutParams);

        infoText.setTextColor(this.context.getResources().getColor(R.color.black));

        infoText.setTextSize(buchstabenGroesseInfoText);
        infoText.setGravity(Gravity.CENTER);
        infoText.setMaxLines(2);
        infoText.setSingleLine(false);
        infoText.setText("EINSTELLUNGEN SEITE2");
        //   automaticButton.setText("AUTOMATIC = EIN");
        this.absolutelayout.addView(infoText);
    }

    public void infoTextBlack() {
        infoText.setTextColor(this.context.getResources().getColor(R.color.black));
    }

    public void infoTextRed() {
        infoText.setTextColor(this.context.getResources().getColor(R.color.red));
    }

    public void infoTextGreen() {
        infoText.setTextColor(this.context.getResources().getColor(R.color.green));
    }

    int glocken_button_width;
    int glocken_button_height;
    int glocken_button_x;
    int glocken_button_y;


    /** slotIndex = Slot auf Seite 2 (0..23). Button-Index für Drawables/buttons wird intern aus buttons.size()-1 ermittelt. */
    public void initGlockenButton(int width, int height, int x, int y, int slotIndex) {
        Button glockenButton = new Button(context);
        buttons.add(glockenButton);

        int buttonIndex = buttons.size() - 1;  // Index in buttons-Liste / für Drawables
        if (slotIndex >= 0 && slotIndex < 24) {
            slotToButtonIndex[slotIndex] = buttonIndex;
            buttonToSlotIndex[buttonIndex] = slotIndex;
        }
        buttons.elementAt(buttonIndex).setTag(Integer.valueOf(slotIndex)); // Slot-Index für Klick-Handler (page2_button_offset = 24 + slotIndex)

        buttons.elementAt(buttonIndex).setTextSize(buchstabenGroesseTastenText);

        AbsoluteLayout.LayoutParams alayoutParams =
                new AbsoluteLayout.LayoutParams(width, height, x, y);
        buttons.elementAt(buttonIndex).setLayoutParams(alayoutParams);

        buttons.elementAt(buttonIndex).setTextColor(buchstabenFarbe);

        Log.e("index", "=" + buttonIndex + " slotIndex=" + slotIndex);
        Log.e("counter relais seite1", "=" + TurmtechnikActivity.BESCHRIFTUNG_TASTEN_SLOTS_PAGE1);

        final int globalOffset = slotIndex + TurmtechnikActivity.BESCHRIFTUNG_TASTEN_SLOTS_PAGE1;
        Integer relNum = globalOffset < TurmtechnikActivity.relaisNumber.length ? TurmtechnikActivity.relaisNumber[globalOffset] : null;
        int relaisNumber = (relNum != null ? relNum : 0);

        Log.e("Seite2Layout", "relaisNumber=" + relaisNumber);

        if ((relaisNumber < StaticConstants.LIMIT_1000_100)  && (relaisNumber > 0)) // ist es eine normale Taste?
        {
            if (globalOffset < TurmtechnikActivity.globalOn.length && TurmtechnikActivity.globalOn[globalOffset] == true) {
                setButtonOnOk(buttonIndex);
            } else {
                setButtonNormal(buttonIndex);
            }

        } else {
            if (relaisNumber > 2000) // eine verknuepfte Taste?
            {
                int idx = slotIndex + Seite1Layout.get_counted_verknuepft_seite1();
                if (idx >= 0 && idx < TurmtechnikActivity.verknuepfteTastenOn.length && Boolean.TRUE.equals(TurmtechnikActivity.verknuepfteTastenOn[idx])) {
                    setButtonOnOk(buttonIndex);
                } else {
                    setButtonNormal(buttonIndex);
                }
            } else {
                setButtonNormal(buttonIndex);
            }
        }

        String buttontext = getButtonText(slotIndex);

        buttons.elementAt(buttonIndex).setText(buttontext);

        buttons.elementAt(buttonIndex).setGravity(Gravity.CENTER);
        this.absolutelayout.addView(buttons.elementAt(buttonIndex));

    }

    /** Liefert den Button-Index für Anzeige-Update (buttonOff/buttonOnOK), oder -1 wenn Slot keinen Button hat. */
    public int getButtonIndexForSlot(int slotIndex) {
        if (slotIndex < 0 || slotIndex >= 24) return -1;
        return slotToButtonIndex[slotIndex];
    }

    /** Liefert den Slot-Index (0..23) für den Button an position buttonIndex, oder -1 wenn ungültig. */
    public int getSlotIndexForButton(int buttonIndex) {
        if (buttonIndex < 0 || buttonIndex >= 24) return -1;
        return buttonToSlotIndex[buttonIndex];
    }

    public void berechne_button_width() {
        if (glockenCount < 3) {
            glocken_button_width = (int)(displayWidth * (1f / 3f));
        } else if (glockenCount < 5) {
            glocken_button_width = (int)(displayWidth * 0.25f);
        } else {
            glocken_button_width = (int)(displayWidth * (1f / 6f));
        }
    }

    int[] buttonX = new int[8];

    private int glockenCountOhneNULL;

    private int kloepelFaengerCountOhneNULL;

    private int gruppeDreiCountOhneNULL;

    /** Tastenbreiten/Positionen in % der Bildschirmbreite – gleiche Aufteilung bei allen Auflösungen. */
    public void berechne_glocken_button_x(int count) {
        switch (count) {
            case 1:
                glocken_button_width = (int)(displayWidth * 0.5f);
                buttonX[0] = (displayWidth / 2) - (glocken_button_width / 2);
                break;
            case 2:
                glocken_button_width = (int)(displayWidth * (1f / 3f));
                int offset_2 = glocken_button_width + (int)(displayWidth * 0.1f);
                buttonX[0] = (int)(displayWidth * (1f / 8f));
                buttonX[1] = buttonX[0] + offset_2;
                break;
            case 3:
                glocken_button_width = (int)(displayWidth * 0.25f);
                int offset_3 = glocken_button_width + (int)(displayWidth * (1f / 16f));
                buttonX[0] = (int)(displayWidth * (1f / 16f));
                buttonX[1] = buttonX[0] + offset_3;
                buttonX[2] = buttonX[1] + offset_3;
                break;
            case 4:
                glocken_button_width = (int)(displayWidth * 0.2f);
                int offset_4 = glocken_button_width + (int)(displayWidth * (1f / 24f));
                buttonX[0] = (int)(displayWidth * (1f / 25f));
                buttonX[1] = buttonX[0] + offset_4;
                buttonX[2] = buttonX[1] + offset_4;
                buttonX[3] = buttonX[2] + offset_4;
                break;
            case 5:
                glocken_button_width = (int)(displayWidth * (1f / 6f));
                int offset_5 = glocken_button_width + (int)(displayWidth * (1f / 34f));
                buttonX[0] = (int)(displayWidth * (1f / 34f));
                buttonX[1] = buttonX[0] + offset_5;
                buttonX[2] = buttonX[1] + offset_5;
                buttonX[3] = buttonX[2] + offset_5;
                buttonX[4] = buttonX[3] + offset_5;
                break;
            case 6:
                glocken_button_width = (int)(displayWidth * (1f / 7f));
                int offset_6 = glocken_button_width + (int)(displayWidth * (1f / 46f));
                buttonX[0] = (int)(displayWidth * (1f / 46f));
                buttonX[1] = buttonX[0] + offset_6;
                buttonX[2] = buttonX[1] + offset_6;
                buttonX[3] = buttonX[2] + offset_6;
                buttonX[4] = buttonX[3] + offset_6;
                buttonX[5] = buttonX[4] + offset_6;
                break;
            case 7:
                glocken_button_width = (int)(displayWidth * (1f / 8f));
                int offset_7 = glocken_button_width + (int)(displayWidth * (1f / 62f));
                buttonX[0] = (int)(displayWidth * (1f / 62f));
                buttonX[1] = buttonX[0] + offset_7;
                buttonX[2] = buttonX[1] + offset_7;
                buttonX[3] = buttonX[2] + offset_7;
                buttonX[4] = buttonX[3] + offset_7;
                buttonX[5] = buttonX[4] + offset_7;
                buttonX[6] = buttonX[5] + offset_7;
                break;
            case 8:
                glocken_button_width = (int)(displayWidth * (1f / 9f));
                int offset_8 = glocken_button_width + (int)(displayWidth * (1f / 80f));
                buttonX[0] = (int)(displayWidth * (1f / 82f));
                buttonX[1] = buttonX[0] + offset_8;
                buttonX[2] = buttonX[1] + offset_8;
                buttonX[3] = buttonX[2] + offset_8;
                buttonX[4] = buttonX[3] + offset_8;
                buttonX[5] = buttonX[4] + offset_8;
                buttonX[6] = buttonX[5] + offset_8;
                buttonX[7] = buttonX[6] + offset_8;
                break;
        }
    }

    /** Leeres Tastenfeld („leer“): Platzhalter ohne Farbe. NULL = keine Taste, kein Platz. */
    private void addLeerPlaceholder(int width, int height, int x, int y) {
        View placeholder = new View(context);
        AbsoluteLayout.LayoutParams lp = new AbsoluteLayout.LayoutParams(width, height, x, y);
        absolutelayout.addView(placeholder, lp);
    }

    public void makeXglockenButtons() {
        for (int i = 0; i < 24; i++) {
            slotToButtonIndex[i] = -1;
            buttonToSlotIndex[i] = -1;
        }

        berechne_glocken_button_x(glockenCountOhneNULL);
        glocken_button_height = (int)(displayHeight * StaticConstants.LAYOUT1_GLOCKEN_BUTTON_HEIGHT_PERCENT);
        glocken_button_y = (int)(displayHeight * StaticConstants.LAYOUT1_GLOCKEN_ROW1_Y_PERCENT);

        int ix = 0;

        //for (int i = 0; i < glockenCount;i++)
        for (int i = 0; i < 8; i++) {
            int leerTasteTemp = leerTaste(i + ANFANG_SEITE2_INDEX);
            if (leerTasteTemp == 0) {
                initGlockenButton(glocken_button_width, glocken_button_height, buttonX[ix], glocken_button_y, i);
                ix++;
            } else if (leerTasteTemp == 1) {
                addLeerPlaceholder(glocken_button_width, glocken_button_height, buttonX[ix], glocken_button_y);
                ix++;
            }
        }

        berechne_glocken_button_x(kloepelFaengerCountOhneNULL);
        glocken_button_y = (int)(displayHeight * StaticConstants.LAYOUT1_GLOCKEN_ROW2_Y_PERCENT);
        ix = 0;
        for (int i = 0; i < 8; i++) {
            int leerTasteTemp = leerTaste(i + 8 + ANFANG_SEITE2_INDEX);
            if (leerTasteTemp == 0) {
                initGlockenButton(glocken_button_width, glocken_button_height, buttonX[ix], glocken_button_y, 8 + i);
                ix++;
            } else if (leerTasteTemp == 1) {
                addLeerPlaceholder(glocken_button_width, glocken_button_height, buttonX[ix], glocken_button_y);
                ix++;
            }
        }
        if (true) // (gruppeDreiCount > 0 )
        {
            berechne_glocken_button_x(gruppeDreiCountOhneNULL);
            glocken_button_y = (int)(displayHeight * StaticConstants.LAYOUT1_GLOCKEN_ROW3_Y_PERCENT);

            ix = 0;
            for (int i = 0; i < 8; i++) {
                int leerTasteTemp = leerTaste(i + 8 + 8 + ANFANG_SEITE2_INDEX);
                if (leerTasteTemp == 0) {
                    initGlockenButton(glocken_button_width, glocken_button_height, buttonX[ix], glocken_button_y, 16 + i);
                    ix++;
                } else if (leerTasteTemp == 1) {
                    addLeerPlaceholder(glocken_button_width, glocken_button_height, buttonX[ix], glocken_button_y);
                    ix++;
                }
            }
        }
    }

    /*
           private int searchButtonColor(int spalte, int index)
           {
               int zeile_index = 1 ;
               int max_zeilen = farbenExcelread.getCellZeilen() ;
               for (int i=0 ; i < max_zeilen ; i++)
               {
                   if( (i)  == index)
                   {
                       break ;  //
                   }
                   else
                   {
                       zeile_index ++ ; // naechste Zeile

                       // leerzeilen ueberspringen
                       while(  (farbenExcelread.getCellString(spalte, zeile_index).equals("")) )
                       {					   if(zeile_index >= max_zeilen)
                           {
                               break ;
                           }
                           zeile_index ++ ;
                       }
                   }
               }

               return zeile_index ;
            }
    */
    public void setButtonOnOk(int index) {
        // 9.6.13 geaendert auf Bitmap sdCard
        //buttons.elementAt(index).setBackgroundDrawable(context.getResources().getDrawable(R.drawable.btn_green));

//		   int tabellen_zeile = searchButtonColor(7, index);
//		   button_farbe_name = farbenExcelread.getCellString(7, tabellen_zeile) ;
//		   btn_manual_ein_ok_drawable = new BitmapDrawable((BitmapFactory.decodeFile(TurmtechnikActivity.sdCardPath + "/Turmtechnik/Grafiken/" + button_farbe_name )));
        Log.e("setButtonOnOk", "index=" + index);
        int safeIdxOk = (btn_manual_ein_ok_drawable.isEmpty()) ? 0 : Math.min(index, btn_manual_ein_ok_drawable.size() - 1);
        buttons.elementAt(index).setBackgroundDrawable(btn_manual_ein_ok_drawable.get(safeIdxOk));
    }

    public void setButtonOnAutomatic(int index) {
        // 10.3.13 geaendert auf Bitmap sdCard
        // buttons.elementAt(index).setBackgroundDrawable(context.getResources().getDrawable(R.drawable.btn_red));
//		   int tabellen_zeile = searchButtonColor(9, index);
//		   button_farbe_name = farbenExcelread.getCellString(9, tabellen_zeile);
//		   btn_automatic_ein_ok_drawable = new BitmapDrawable((BitmapFactory.decodeFile(TurmtechnikActivity.sdCardPath + "/Turmtechnik/Grafiken/" + button_farbe_name )));
        //buttons.elementAt(index).setBackgroundDrawable(btn_automatic_ein_ok_drawable.get(index));
        int safeIdxAuto = (btn_automatic_ein_ok_drawable.isEmpty()) ? 0 : Math.min(index, btn_automatic_ein_ok_drawable.size() - 1);
        Drawable drawableTemp = btn_automatic_ein_ok_drawable.get(safeIdxAuto);
        buttons.elementAt(index).setBackgroundDrawable(drawableTemp);
    }

    public void setButtonNormal(int index) {
        //buttons.elementAt(index).setBackgroundDrawable(context.getResources().getDrawable(R.drawable.btn_gray));
        // 10.3.13 geaender auf Bitmap sdCard

//		   int tabellen_zeile = searchButtonColor(6, index);
//		   button_farbe_name = farbenExcelread.getCellString(6, tabellen_zeile);
//		   btn_normal_drawable = new BitmapDrawable((BitmapFactory.decodeFile(TurmtechnikActivity.sdCardPath + "/Turmtechnik/Grafiken/" + button_farbe_name )));
        int safeIdx = (btn_normal_drawable.isEmpty()) ? 0 : Math.min(index, btn_normal_drawable.size() - 1);
        buttons.elementAt(index).setBackgroundDrawable(btn_normal_drawable.get(safeIdx));
    }

    /** localIndex = Button-Index (für setButtonNormal/buttons). Text kommt per Slot-Index. */
    public void buttonOff(int localIndex) {
        setButtonNormal(localIndex);
        int slotIdx = (localIndex >= 0 && localIndex < 24) ? buttonToSlotIndex[localIndex] : localIndex;
        String buttonText = getButtonText(slotIdx);
        buttons.elementAt(localIndex).setText(buttonText);
    }

    public void buttonOnOK(int localIndex) {
        setButtonOnOk(localIndex);
        int slotIdx = (localIndex >= 0 && localIndex < 24) ? buttonToSlotIndex[localIndex] : localIndex;
        String buttonText = getButtonText(slotIdx);
        buttons.elementAt(localIndex).setText(buttonText);
    }

    public void buttonOnRed(int localIndex) {
        setButtonOnAutomatic(localIndex);
        int slotIdx = (localIndex >= 0 && localIndex < 24) ? buttonToSlotIndex[localIndex] : localIndex;
        String buttonText = getButtonText(slotIdx);
        buttons.elementAt(localIndex).setText(buttonText);
    }

/*
	   private int checkIndexTemp(int index)
	   {
		   if (index >= glockenCount)
		   {
			   index = index - glockenCount + 9 ;
		   }
		   else 
		   {
			   index = index + 1 ;
		   }
		   return index ;
	   }
*/
    //public static void printDatum()
    //{
    //	   datum.setText(getWochentag()+"\n");
    //	   datum.append(getDatum());
    //   }

    public static void printDatum() {
        datum.setText(getWochentag() + "\n");
        datum.append(getDatum());
        if ((StaticVariable.timeServerEinAus.equals("EIN")) || (StaticVariable.serialGPS_OnOff == true)) {
            if (StaticVariable.timeServerOk) {
                //datum.append("\n" + StaticVariable.timeServerIp + "=" + getItpTime());
                datum.append("\nGPS-" + getItpTime());
            } else {
                datum.append("\n" + "--:GPS:--");
            }
        }
    }

    public static String getItpTime() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");

        Calendar cal = Calendar.getInstance();

        cal.setTimeInMillis(System.currentTimeMillis());

        //Log.e("system" , "= " + dateFormat.format(cal.getTime())) ;
        return dateFormat.format(cal.getTime());


    }

    public AbsoluteLayout initLayout() {
        initLogo();
        initClock();
        initDatum();
//	       initStopButton();
        // initPfeilRechtsButton(); wir durch "HOME" und "SEITE" ersetzt 12.6.13
//	       initAutomaticButton();
        makeXglockenButtons();
        initInfoText();

        if (excelread != null) {
            excelread.closeWorkbook();                // test wegen out of memory
            excelread = null;
        }
        System.gc();

        return this.absolutelayout;
    }

    private int getGlockenCount() {
        if (beschriftungSeite2FromDb) {
            glockenCountOhneNULL = 0;
            for (int i = 0; i < 8 && i < beschriftung.size(); i++) {
                String c2 = beschriftung.get(i) != null ? beschriftung.get(i).trim() : "";
                if ("NULL".equals(c2) || "null".equalsIgnoreCase(c2)) continue;
                glockenCountOhneNULL++;
            }
            return Math.min(8, beschriftung.size());
        }
        if (excelread == null) return 0;
        int glockencount = 0;
        glockenCountOhneNULL = 0;

        int max_zeilen = excelread.getCellZeilen();
        max_zeilen = Math.min(max_zeilen, (ANFANG_SEITE2_INDEX + 8));
        for (int i = ANFANG_SEITE2_INDEX; i < max_zeilen; i++) {
            String strtemp = "";
            try {
                strtemp = excelread.getCellString(2, i);
            } catch (ArrayIndexOutOfBoundsException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                new LogExcelError(2, i, inputFilename, BESCHRIFTUNG_GLOCKEN_SHEET, sourceFileName, 700);
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                new LogExcelError(2, i, inputFilename, BESCHRIFTUNG_GLOCKEN_SHEET, sourceFileName, 704);
            }
            if (!((strtemp.equals("null"))))  // || (strtemp.equals("NULL")) ) )
            {
                glockencount++;
            }
            if (!"null".equalsIgnoreCase(strtemp) && !"NULL".equals(strtemp)) glockenCountOhneNULL++;
        }

        Log.e("glockenCount", "OhneNULL " + glockenCountOhneNULL);
        Log.e("glockenCount", " " + glockencount);
        return glockencount;
    }

    private int leerTaste(int index) // 0 == normale Taste
    // 1 == leer Taste
    // 2 == NULL Taste also komplett leere Zeile
    // 3 == outOfBound
    // index = Excel-Zeile (25..48 für Seite 2)
    {
        if (beschriftungSeite2FromDb) {
            int rowIndex = index - ANFANG_SEITE2_INDEX;
            if (rowIndex < 0 || rowIndex >= beschriftung.size()) return 2;
            String c2 = beschriftung.get(rowIndex) != null ? beschriftung.get(rowIndex).trim() : "";
            if ("leer".equalsIgnoreCase(c2)) return 1;
            if ("NULL".equals(c2)) return 2;
            if (c2.isEmpty()) return 2;
            return 0;
        }
        if (excelread == null) return 2;
        //Log.e("leerTaste" , "index=" + index) ;

        if (index > (excelread.getCellZeilen() - 1)) {
            Log.e("ACHTUNG", "outOfBound");
            return 3;
        }

        String temp = "";
        try {
            temp = (excelread.getCellString(2, index));
        } catch (ArrayIndexOutOfBoundsException e) {
            // TODO Auto-generated catch block
            //e.printStackTrace();
            //new LogExcelError(2, index, inputFilename, BESCHRIFTUNG_GLOCKEN_SHEET,sourceFileName,723) ;
            return 3;
        } catch (Exception e) {
            // TODO Auto-generated catch block
            //e.printStackTrace();
            //new LogExcelError(2, index, inputFilename, BESCHRIFTUNG_GLOCKEN_SHEET,sourceFileName,727) ;
            return 3;
        }
        //Log.i("temp" , "=" + temp) ;
        int retCode = 0;
        Log.e("checkLeerTaste", "index=" + index + "temp=" + temp);
        if ((temp.equals("leer"))) {
            retCode = 1;
        }
        if ((temp.equals("NULL"))) {
            retCode = 2;
        }

        Log.e("retCode", "=" + retCode);
        return retCode;
    }

    private int getKloeppelCount() {
        if (beschriftungSeite2FromDb) {
            kloepelFaengerCountOhneNULL = 0;
            for (int i = 8; i < 16 && i < beschriftung.size(); i++) {
                String c2 = beschriftung.get(i) != null ? beschriftung.get(i).trim() : "";
                if ("NULL".equals(c2) || "null".equalsIgnoreCase(c2)) continue;
                kloepelFaengerCountOhneNULL++;
            }
            return Math.min(8, Math.max(0, beschriftung.size() - 8));
        }
        if (excelread == null) return 0;
        int kloeppelcount = 0;
        kloepelFaengerCountOhneNULL = 0;

        int max_zeilen = excelread.getCellZeilen();
        max_zeilen = Math.min(max_zeilen, (ANFANG_SEITE2_INDEX + 8 + 8));
        for (int i = ANFANG_SEITE2_INDEX + 8; i < max_zeilen; i++) {
            String strtemp = "";
            try {
                strtemp = excelread.getCellString(2, i);
            } catch (ArrayIndexOutOfBoundsException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                new LogExcelError(2, i, inputFilename, BESCHRIFTUNG_GLOCKEN_SHEET, sourceFileName, 746);
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                new LogExcelError(2, i, inputFilename, BESCHRIFTUNG_GLOCKEN_SHEET, sourceFileName, 750);
            }
            if (!((strtemp.equals("null"))))  // || (strtemp.equals("NULL")) ) )
            {
                kloeppelcount++;
            }
            if (!"null".equalsIgnoreCase(strtemp) && !"NULL".equals(strtemp)) kloepelFaengerCountOhneNULL++;
        }
        return kloeppelcount;
    }

    private int getGruppeDreiCount() {
        if (beschriftungSeite2FromDb) {
            gruppeDreiCountOhneNULL = 0;
            for (int i = 16; i < 24 && i < beschriftung.size(); i++) {
                String c2 = beschriftung.get(i) != null ? beschriftung.get(i).trim() : "";
                if ("NULL".equals(c2) || "null".equalsIgnoreCase(c2)) continue;
                gruppeDreiCountOhneNULL++;
            }
            return Math.min(8, Math.max(0, beschriftung.size() - 16));
        }
        if (excelread == null) return 0;
        int gruppe3count = 0;
        gruppeDreiCountOhneNULL = 0;
        int max_zeilen = excelread.getCellZeilen();
        max_zeilen = Math.min(max_zeilen, (ANFANG_SEITE2_INDEX + 8 + 8 + 8));
        for (int i = ANFANG_SEITE2_INDEX + 8 + 8; i < max_zeilen; i++) {
            String strtemp = "";
            try {
                strtemp = excelread.getCellString(2, i);
            } catch (ArrayIndexOutOfBoundsException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                new LogExcelError(2, i, inputFilename, BESCHRIFTUNG_GLOCKEN_SHEET, sourceFileName, 772);
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                new LogExcelError(2, i, inputFilename, BESCHRIFTUNG_GLOCKEN_SHEET, sourceFileName, 776);
            }
            if (!((strtemp.equals("null"))))  // || (strtemp.equals("NULL")) ) )
            {
                gruppe3count++;
            }
            if (!"null".equalsIgnoreCase(strtemp) && !"NULL".equals(strtemp)) gruppeDreiCountOhneNULL++;
        }
        return gruppe3count;
    }

    private void loadOnlyButtonText() {   // Log.i("Suche" , "relais, hammer") ;
        int max_zeilen = excelread.getCellZeilen();
        for (int i = ANFANG_SEITE2_INDEX; i < max_zeilen; i++) // max 3 reihen je 8 buttons
        {
            try {
                if (excelread.getCellString(2, i).equals("null")) {
                    break; // leeres Tabellen element
                }


                if (((!excelread.getCellString(2, i).equals("leer")
                        && (!excelread.getCellString(2, i).equals("NULL"))

                )))

                {
                    if (!excelread.getCellString(2, i).toLowerCase().equals("null")) // ist noch eine Glocke, Kloeppel, Hammer?
                    {
                        beschriftung.add(excelread.getCellString(2, i));

                        // 13.3.13 -- das wird alles in Seite1Layout erledigt!!
//					    Log.e("beschriftung:" , (excelread.getCellString(2, i))) ;
//				    
//					    Log.i("schleifen index" , "=" + i) ;
//					    String relaisNumber = excelread.getCellString(3, i);
//					    Log.i("STRING" , "relaisNumber=" + relaisNumber) ;
//					    String PlatineNumber = excelread.getCellString(5, i); 
//					    int relNumber = (Integer.parseInt(relaisNumber)) ;
//    	   		    	int platNumber = (Integer.parseInt(PlatineNumber)) ;
//    	   		    	int rlnumtemp = relNumber + ((platNumber-1) * 32 ) ; // 4 carambola platinen
//    	   		    	Log.e("relaisnumber" , "=" + rlnumtemp) ;
//    	   		    	relaisnumber.add(rlnumtemp) ;
//    	   		    	Log.e("relaisvector.size" , "=" + relaisnumber.size()) ;


                        // int hammertemp = 0 ;
                        // Log.i("index" , "hammertime=" + excelread.getCellString(4, i)) ;
//			   	    	if (!excelread.getCellString(4, i).equals(""))
//			   		    {
//			   			    hammertemp = (Integer.parseInt(excelread.getCellString(4, i))) ;
//			   			    hammertemp = hammertemp * 10 ; // zehntel Sekunden auf ms umwandeln ;
//			   	    	}
                        //  hammerzeit.add(hammertemp) ;
                    }
                }
            } catch (ArrayIndexOutOfBoundsException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                new LogExcelError(2, i, inputFilename, BESCHRIFTUNG_GLOCKEN_SHEET, sourceFileName, 826);
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                new LogExcelError(2, i, inputFilename, BESCHRIFTUNG_GLOCKEN_SHEET, sourceFileName, 830);
            }
        }


    }


    private String getButtonText(int index) {
        return beschriftung.elementAt(index);
    }

    // 13.6.13  von Seite1Layout nehmen
/*	   
	   public int getRelaisNumber (int index)
	   {
		   if (index >= relaisnumber.size())
		   {
			   return 0 ;
		   }
		   else
		   {
			   return relaisnumber.elementAt(index);
	       }
	   }
	   public int getHammerZeit (int index)
	   {
		   if (index >= hammerzeit.size())
		   {
			   return 0 ;
		   }
		   else
		   {
			   return hammerzeit.elementAt(index);
		   }
	   }
*/
    public static int getAnzahlDerBeschriftungen() {

        return beschriftung.size();
    }

    private void readVerknuepfteTastenTexts() {
        int zeilen_max = excelread.getCellZeilen();

        for (int i = ANFANG_SEITE2_INDEX; i < zeilen_max; i++) {
            try {
                if ((excelread.getCellString(1, i).equals("verknuepft"))
                        || excelread.getCellString(1, i).equals("Verknüpft")
                        )

                {
                    String verknuepftTemp = excelread.getCellString(2, i);
                    UhrThread.verknuepfteTastenString.add(verknuepftTemp);
                }
            } catch (ArrayIndexOutOfBoundsException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                new LogExcelError(2, i, inputFilename, BESCHRIFTUNG_GLOCKEN_SHEET, sourceFileName, 918);
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                new LogExcelError(2, i, inputFilename, BESCHRIFTUNG_GLOCKEN_SHEET, sourceFileName, 922);
            }
        }
    }

    /** Farben pro Tastentyp (Vorlagen), wenn keine Grafik in DB/Excel – wie Seite1Layout / Web-UI TYPEN_FARBE. */
    private static Drawable getDrawableForTyp(String c1) {
        if (c1 == null) c1 = "";
        String key = c1.trim();
        int color;
        switch (key) {
            case "Stop": color = Color.parseColor("#f8d7da"); break;
            case "Automatik": color = Color.parseColor("#cff4fc"); break;
            case "Normal": color = Color.parseColor("#e9ecef"); break;
            case "Verknüpft": color = Color.parseColor("#cfe2ff"); break;
            case "Sofort Start": color = Color.parseColor("#d1e7dd"); break;
            case "Schwingen": color = Color.parseColor("#fff3cd"); break;
            case "Melodie": color = Color.parseColor("#e2d5f1"); break;
            case "Ausgang": color = Color.parseColor("#d4b896"); break;
            case "Hammer": color = Color.parseColor("#d4c4a8"); break;
            case "Home": color = Color.parseColor("#d3e3fc"); break;
            case "Help": color = Color.parseColor("#ffe69c"); break;
            case "Schlagwerk": color = Color.parseColor("#c9a86c"); break;
            case "Zweite Seite": color = Color.parseColor("#d4edda"); break;
            case "Programmeingeben": color = Color.parseColor("#cce5ff"); break;
            case "Programmabfrage": color = Color.parseColor("#e7e4f4"); break;
            case "Nebenuhr Stellen": color = Color.parseColor("#fff0e6"); break;
            case "Leer": case "leer": color = Color.parseColor("#f8f9fa"); break;
            case "NULL": color = Color.parseColor("#e2e3e5"); break;
            default: color = Color.parseColor("#e9ecef");
        }
        return new ColorDrawable(color);
    }

    private void loadFarben() {
        if (farbenExcelread == null) {
            // Kein Excel: Farben aus Vorlagen (Tastentyp c1) aus DB – wie Seite 1
            for (int i = 0; i < 24; i++) {
                String c1 = "";
                if (i < rowsSeite2FromDb.size()) {
                    PlatinenDatabaseHelper.BeschriftungTastenRow r = rowsSeite2FromDb.get(i);
                    c1 = r.c1 != null ? r.c1.trim() : "";
                }
                btn_normal_drawable.add(getDrawableForTyp(c1));
            }
            Log.e("loadFarben", "Seite2: Vorlagen-Farben aus DB, size=" + btn_normal_drawable.size());
            return;
        }
        int zeile_index = ANFANG_SEITE2_INDEX;
        int max_zeilen = farbenExcelread.getCellZeilen();
        Log.e("loadFarben", "max_zeilen=" + max_zeilen);
        for (int i = ANFANG_SEITE2_INDEX; i < max_zeilen; i++) {
            Drawable d = null;
            try {
                String c6 = farbenExcelread.getCellString(6, (i));
                if (c6 != null && !c6.toLowerCase().equals("null") && !c6.trim().isEmpty()) {
                    button_farbe_name = c6;
                    Log.e("farbe", "fileNameGeladen=" + button_farbe_name);
                    d = new BitmapDrawable(BitmapFactory.decodeFile(TurmtechnikActivity.sdCardPath + "/Turmtechnik/Grafiken/" + button_farbe_name));
                    if (d != null) btn_normal_drawable.add(d);
                }
            } catch (ArrayIndexOutOfBoundsException e) {
                e.printStackTrace();
                new LogExcelError(6, i, inputFilename, BESCHRIFTUNG_GLOCKEN_SHEET, sourceFileName, 942);
            } catch (Exception e) {
                e.printStackTrace();
                new LogExcelError(6, i, inputFilename, BESCHRIFTUNG_GLOCKEN_SHEET, sourceFileName, 946);
            }
            if (d == null && i - ANFANG_SEITE2_INDEX >= 0 && i - ANFANG_SEITE2_INDEX < rowsSeite2FromDb.size()) {
                String c1 = rowsSeite2FromDb.get(i - ANFANG_SEITE2_INDEX).c1 != null ? rowsSeite2FromDb.get(i - ANFANG_SEITE2_INDEX).c1.trim() : "";
                btn_normal_drawable.add(getDrawableForTyp(c1));
            }

            try {
                if (!(farbenExcelread.getCellString(7, (i)).toLowerCase().equals("null"))) {
                    button_farbe_name = farbenExcelread.getCellString(7, i);
                    btn_manual_ein_ok_drawable.add(new BitmapDrawable((BitmapFactory.decodeFile(TurmtechnikActivity.sdCardPath + "/Turmtechnik/Grafiken/" + button_farbe_name))));
                }
            } catch (ArrayIndexOutOfBoundsException e) {
                e.printStackTrace();
                new LogExcelError(7, i, inputFilename, BESCHRIFTUNG_GLOCKEN_SHEET, sourceFileName, 958);
            } catch (Exception e) {
                e.printStackTrace();
                new LogExcelError(7, i, inputFilename, BESCHRIFTUNG_GLOCKEN_SHEET, sourceFileName, 962);
            }

            try {
                if (!(farbenExcelread.getCellString(8, (i)).toLowerCase().equals("null"))) {
                    button_farbe_name = farbenExcelread.getCellString(8, i);
                    btn_manual_ein_error_drawable.add(new BitmapDrawable((BitmapFactory.decodeFile(TurmtechnikActivity.sdCardPath + "/Turmtechnik/Grafiken/" + button_farbe_name))));
                }
            } catch (ArrayIndexOutOfBoundsException e) {
                e.printStackTrace();
                new LogExcelError(8, i, inputFilename, BESCHRIFTUNG_GLOCKEN_SHEET, sourceFileName, 974);
            } catch (Exception e) {
                e.printStackTrace();
                new LogExcelError(8, i, inputFilename, BESCHRIFTUNG_GLOCKEN_SHEET, sourceFileName, 978);
            }

            try {
                if (!(farbenExcelread.getCellString(9, (i)).toLowerCase().equals("null"))) {
                    button_farbe_name = farbenExcelread.getCellString(9, i);
                    btn_automatic_ein_ok_drawable.add(new BitmapDrawable((BitmapFactory.decodeFile(TurmtechnikActivity.sdCardPath + "/Turmtechnik/Grafiken/" + button_farbe_name))));
                }
            } catch (ArrayIndexOutOfBoundsException e) {
                e.printStackTrace();
                new LogExcelError(9, i, inputFilename, BESCHRIFTUNG_GLOCKEN_SHEET, sourceFileName, 990);
            } catch (Exception e) {
                e.printStackTrace();
                new LogExcelError(9, i, inputFilename, BESCHRIFTUNG_GLOCKEN_SHEET, sourceFileName, 994);
            }

            try {
                if (!(farbenExcelread.getCellString(10, (i)).toLowerCase().equals("null"))) {
                    button_farbe_name = farbenExcelread.getCellString(10, i);
                    btn_manual_ein_error_drawable.add(new BitmapDrawable((BitmapFactory.decodeFile(TurmtechnikActivity.sdCardPath + "/Turmtechnik/Grafiken/" + button_farbe_name))));
                }
            } catch (ArrayIndexOutOfBoundsException e) {
                e.printStackTrace();
                new LogExcelError(10, i, inputFilename, BESCHRIFTUNG_GLOCKEN_SHEET, sourceFileName, 1006);
            } catch (Exception e) {
                e.printStackTrace();
                new LogExcelError(10, i, inputFilename, BESCHRIFTUNG_GLOCKEN_SHEET, sourceFileName, 1010);
            }
        }
        Log.e("btn_normal_drawable", "size=" + btn_normal_drawable.size());
    }


//	   public void closeWorkbook()
//	   {
//		   excelread.closeWorkbook();
//	   }
//	   

} // ende der Klasse
