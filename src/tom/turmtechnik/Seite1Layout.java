package tom.turmtechnik;

import android.content.Context;
import android.graphics.Color;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsoluteLayout;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Vector;

import jxl.read.biff.BiffException;

//import android.util.Log;


public class Seite1Layout {
    private final String sourceFileName = "Seite1Layout";
    private final int buchstabenFarbe;

    //	public Button pfeilRechtsButton ;
    public Button stopButton;
    public Button automaticButton;
    public TextView infoText;
    public static Vector<Button> buttons = new Vector<Button>();

    //private final int BESCHRIFTUNG_GLOCKEN_SHEET = 10 ; // 14.2.14 wieder geaendert auf eigene datei
    private final int BESCHRIFTUNG_GLOCKEN_SHEET_NEW = 0; // /Turmtechnik/Config/Beschriftung-Tasten.xls												  // datei = Beschriftung-Tasten.xls
    //	private final int FARBEN_SHEET_NUMBER = 16 ;
//	private final int FARBEN_SHEET_NUMBER = 10 ; // die Tastenhintergrund Grafik namen auf Glocken sheet gegeben
    // 11.6.13
    private final int FARBEN_SHEET_NUMBER_NEW = 0;

    private int[] buchstabenXY;

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
    public static Vector<Integer> relaisnumber = new Vector<Integer>();
    public static Vector<Integer> hammerzeit = new Vector<Integer>();

    // public static Vector<Integer> fernwartungId = new Vector<Integer>(); // vorherige Fernwartung
    // neu 29.06.2014
    public static Vector<String> buttonId = new Vector<String>();

    private static TextView datum;
    private static ImageView mondAnzeige; // ImageView für Monduhr-Anzeige (Canvas-basiert)

    private Context context;
    private static Context staticContext; // Für statische Methoden
    private static AbsoluteLayout absolutelayout;
    //private DigitalClock digitalClock ;

    private static TextView uhr;

    private int displayWidth;
    private int displayHeight;

    /** Nach 5 Sekunden Logo-Druck wird dies aufgerufen (z. B. für Anlage-Import). */
    private Runnable onAnlageImportRequestedListener;

    private float buchstabenGroesseUhr;
    private float buchstabenGroesseWochentag;
    private float buchstabenGroesseInfoText;
    private float buchstabenGroesseTastenText;

    //private int buchstabenGroesse ;


    private float scale;

    private int heightView = ViewGroup.LayoutParams.WRAP_CONTENT;
    private int widthView = ViewGroup.LayoutParams.FILL_PARENT;
    //	private Button glockenButton ;
    private int glockenCount;
    private int kloepelFaengerCount;
    private int gruppeDreiCount; // ab 26.3.2013 eine dritte zeile mit Tasten
    private String beschriftungTastenFilename;

    private Button buttonTemp;

    private int index;
    private static ExcelRead excelread;
    private static ExcelRead farbenExcelread;
    private String button_farbe_name;
    /** Wenn nicht null: Tasten/Farben aus DB; farbenExcelread wird für diese Zeilen nicht genutzt. */
    private static java.util.List<PlatinenDatabaseHelper.BeschriftungTastenRow> beschriftungRowsFromDb;

    static int counted_relais_total;
    static int counted_verknuepft_seite1;

    public Seite1Layout(String fileName, Context context) {
        StaticVariable.layoutReady = false;

        this.context = context;
        staticContext = context; // Für statische Methoden speichern
        beschriftungTastenFilename = fileName;

        // Zuerst aus DB laden, damit bei fehlender Excel-Datei Farben/Tasten aus DB genutzt werden
        loadButtonText_andRelaisNumber_andHammerZeit();

        // Button-Hintergründe: Excel nur wenn Datei vorhanden, sonst nur DB (beschriftungRowsFromDb)
        farbenExcelread = new ExcelRead();
        try {
            farbenExcelread.openXlsSheet(beschriftungTastenFilename, FARBEN_SHEET_NUMBER_NEW);
        } catch (BiffException e) {
            e.printStackTrace();
            farbenExcelread = null;
            if (beschriftungRowsFromDb == null) {
                new LogExcelError(-1, -1, beschriftungTastenFilename, FARBEN_SHEET_NUMBER_NEW, sourceFileName, 121);
            }
        } catch (IOException e) {
            e.printStackTrace();
            farbenExcelread = null;
            if (beschriftungRowsFromDb == null) {
                new LogExcelError(-1, -1, beschriftungTastenFilename, FARBEN_SHEET_NUMBER_NEW, sourceFileName, 125);
            }
        }

        loadFarben();

        loadHomeButton();
        loadHelpButton();
        loadAutomaticButton();
        loadStopButton();

        if (farbenExcelread != null) {
            try { farbenExcelread.closeWorkbook(); } catch (Exception ignored) { }
        }
        // Nur Datenbank: Excel für Beschriftung nicht mehr öffnen. Bei leerer DB in Web-UI „Aus Excel importieren“ nutzen.
        excelread = null;
        readVerknuepfteTastenTexts();
        counted_relais_total = countRelaisTotal();
        counted_verknuepft_seite1 = countVerknuepftSeite1();

        glockenCount = getGlockenCount();
        kloepelFaengerCount = getKloeppelCount();
        gruppeDreiCount = getGruppeDreiCount();

        //Log.e("glockenCount" ,"=" + glockenCount) ;
        //Log.e("kloeppelCount" , "=" + kloepelFaengerCount) ;
        //Log.e("gruppe3Count" , "=" + gruppeDreiCount ) ;

        absolutelayout = new AbsoluteLayout(context);
        AbsoluteLayout.LayoutParams altable = new AbsoluteLayout.LayoutParams(
                ViewGroup.LayoutParams.FILL_PARENT,
                ViewGroup.LayoutParams.FILL_PARENT, 0, 0);
        absolutelayout.setLayoutParams(altable);

        displayWidth = TurmtechnikActivity.getDisplayWith();
        displayHeight = TurmtechnikActivity.getDisplayHeight();
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

//		DisplayMetrics dm = new DisplayMetrics() ;


        buchstabenXY = StaticConstants.leseBuchstabenGroesse(); // das Feld mit xy koordinaten einlesen

        // Textgrößen: aus Config (Pixel/density) oder Fallback = % der Bildschirmhöhe → gleiche Aufteilung bei allen Auflösungen
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
        //buchstabenFarbe = 0xFF00FF00 ;
        Log.e("buchstabeFarbe", "=" + Integer.toHexString(buchstabenFarbe));

        //Log.i("buchstaben UHR" , "=" + buchstabenGroesseUhr) ;
        //Log.i("buchstaben Wochentag" , "=" + buchstabenGroesseWochentag) ;
        //Log.i("buchstaben InfoText" , "=" + buchstabenGroesseInfoText) ;

    } // ende Konstruktor Seite1Layout

    public void initLogo() {
        int logo_x = (int)(displayWidth * StaticConstants.LAYOUT1_LOGO_X_PERCENT) + buchstabenXY[9];
        int logo_y = (int)(displayHeight * StaticConstants.LAYOUT1_LOGO_Y_PERCENT) + buchstabenXY[10];
        int logo_with = (int)(displayWidth * StaticConstants.LAYOUT1_LOGO_SIZE_PERCENT) + buchstabenXY[11];
        //	  int logo_height = ( displayHeight * 50 )  / 100  ;


        ImageView logoView = new ImageView(this.context);

        //logoView.setImageResource(R.raw.turmtechnik_nur_logo_klein) ;
        //logoView.setImageResource(R.raw.kirchturmtechnik_logo4) ;

        logoView.setImageResource(R.drawable.kirchturmtechnik_logo4);


        AbsoluteLayout.LayoutParams alayoutParams =
                //	  new AbsoluteLayout.LayoutParams(LayoutParams.WRAP_CONTENT,LayoutParams.WRAP_CONTENT, logo_x, logo_y);
                new AbsoluteLayout.LayoutParams(logo_with, logo_with, logo_x, logo_y);
        //	  new AbsoluteLayout.LayoutParams(logo_with, LayoutParams.WRAP_CONTENT, logo_x, logo_y) ;
        //	  new AbsoluteLayout.LayoutParams(logo_with, logo_height, 0 , 0 );

        logoView.setLayoutParams(alayoutParams);

        // Länger als 5 Sekunden auf Logo drücken → Anlage-Import (Passwort wird in Activity abgefragt)
        final Handler longPressHandler = new Handler(Looper.getMainLooper());
        final Runnable triggerImport = new Runnable() {
            @Override
            public void run() {
                if (onAnlageImportRequestedListener != null) {
                    onAnlageImportRequestedListener.run();
                }
            }
        };
        logoView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        longPressHandler.postDelayed(triggerImport, 5000);
                        return true;
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        longPressHandler.removeCallbacks(triggerImport);
                        return true;
                }
                return false;
            }
        });

        this.absolutelayout.addView(logoView);

    }

    /** Setzt den Callback, der nach 5 Sekunden Logo-Druck ausgeführt wird (Anlage importieren). */
    public void setOnAnlageImportRequestedListener(Runnable listener) {
        this.onAnlageImportRequestedListener = listener;
    }

    public void initClock() {
        int clock_width = (int)(displayWidth * StaticConstants.LAYOUT1_CLOCK_WIDTH_PERCENT);
        int clock_height = (int)(displayHeight * StaticConstants.LAYOUT1_CLOCK_HEIGHT_PERCENT);
        int clock_x = (displayWidth / 2) - (clock_width / 2) + buchstabenXY[3];
        int clock_y = 0 + buchstabenXY[4];
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


    public static String getDatum() {
        Date dt = new Date();
        SimpleDateFormat df = new SimpleDateFormat("dd.MM.yyyy");
        return df.format(dt);
    }

    public static String getUhrzeit() {
        Date dt = new Date();
        SimpleDateFormat df = new SimpleDateFormat("HH:mm:ss");
        return df.format(dt);
    }

    public static String getItpTime() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(System.currentTimeMillis());
        return dateFormat.format(cal.getTime());
    }

    public static String getWochentag() {
        GregorianCalendar date = new GregorianCalendar();
        int day;
        String[] weekDayNames = new String[7];
        for (int i = 0; i < 7; i++) {
            weekDayNames[i] = StaticVariable.getUebersetzung(i + 1);
        }
        day = date.get(Calendar.DAY_OF_WEEK);
        return weekDayNames[day - 1];
    }


    public void initDatum() {
        int datum_width = (int)(displayWidth * StaticConstants.LAYOUT1_DATUM_WIDTH_PERCENT);
        int datum_x = (int)(displayWidth * StaticConstants.LAYOUT1_DATUM_X_PERCENT) + buchstabenXY[5];
        int datum_y = (int)(displayHeight * StaticConstants.LAYOUT1_DATUM_Y_PERCENT) + buchstabenXY[6];

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

    public void initMondAnzeige() {
        // Mondgröße in % der Bildschirmauflösung – bei allen Auflösungen gleiche Aufteilung
        // WICHTIG: Mondgröße ist unabhängig von Tastengröße
        int mond_width;
        int mond_height;

        if (buchstabenXY.length > 17 && buchstabenXY[17] > 0 && buchstabenXY[17] <= 100) {
            // Config-Wert 1–100 = Prozent der Bildschirmbreite (z. B. 5 = 5%)
            float percent = buchstabenXY[17] / 100f;
            mond_width = (int)(displayWidth * percent);
            mond_height = mond_width;
        } else {
            // Fallback: Anteil der Bildschirmbreite (StaticConstants.MOND_SIZE_PERCENT_LAYOUT)
            int sizePx = (int)(displayWidth * StaticConstants.MOND_SIZE_PERCENT_LAYOUT);
            mond_width = Math.max(24, sizePx);
            mond_height = mond_width;
        }
        
        // Position: Links neben dem Logo
        int mond_x;
        int mond_y;
        
        if (buchstabenXY.length > 16 && buchstabenXY[15] == 0 && buchstabenXY[16] == 0) {
            // Automatische Positionierung: Mittig auf dem Logo (% der Auflösung)
            int logo_x = (int)(displayWidth * StaticConstants.LAYOUT1_LOGO_X_PERCENT) + buchstabenXY[9];
            int logo_y = (int)(displayHeight * StaticConstants.LAYOUT1_LOGO_Y_PERCENT) + buchstabenXY[10];
            int logo_with = (int)(displayWidth * StaticConstants.LAYOUT1_LOGO_SIZE_PERCENT) + buchstabenXY[11];
            
            // Mond mittig auf dem Logo (horizontal und vertikal zentriert)
            mond_x = logo_x + (logo_with / 2) - (mond_width / 2); // Horizontal zentriert
            mond_y = logo_y + (logo_with / 2) - (mond_height / 2); // Vertikal zentriert
            
            // Sicherstellen, dass Mond nicht außerhalb des Bildschirms ist
            if (mond_x < 0) {
                mond_x = 5; // Mindestabstand vom linken Rand
            }
            if (mond_y < 0) {
                mond_y = logo_y; // Falls zu weit oben, auf Logo-Höhe setzen
            }
        } else {
            // Verwende Config-Werte oder berechne mittig auf Logo
            if (buchstabenXY.length > 15 && buchstabenXY[15] != 0) {
                mond_x = buchstabenXY[15];
            } else {
                int logo_x = (int)(displayWidth * StaticConstants.LAYOUT1_LOGO_X_PERCENT) + buchstabenXY[9];
                int logo_with = (int)(displayWidth * StaticConstants.LAYOUT1_LOGO_SIZE_PERCENT) + buchstabenXY[11];
                mond_x = logo_x + (logo_with / 2) - (mond_width / 2);
            }
            if (buchstabenXY.length > 16 && buchstabenXY[16] != 0) {
                mond_y = buchstabenXY[16];
            } else {
                int logo_y = (int)(displayHeight * StaticConstants.LAYOUT1_LOGO_Y_PERCENT) + buchstabenXY[10];
                int logo_with = (int)(displayWidth * StaticConstants.LAYOUT1_LOGO_SIZE_PERCENT) + buchstabenXY[11];
                mond_y = logo_y + (logo_with / 2) - (mond_height / 2);
            }
        }
        
        mondAnzeige = new ImageView(this.context);
        
        AbsoluteLayout.LayoutParams alayoutParams =
                new AbsoluteLayout.LayoutParams(mond_width, mond_height, mond_x, mond_y);
        mondAnzeige.setLayoutParams(alayoutParams);
        
        // Initialisiere mit leerem Bitmap, wird von printMondAnzeige() gesetzt
        android.graphics.Bitmap emptyBitmap = android.graphics.Bitmap.createBitmap(mond_width, mond_height, android.graphics.Bitmap.Config.ARGB_8888);
        mondAnzeige.setImageBitmap(emptyBitmap);
        mondAnzeige.setScaleType(ImageView.ScaleType.FIT_CENTER);
        
        this.absolutelayout.addView(mondAnzeige);
        
        // Initiale Anzeige
        printMondAnzeige();
    }

    /** Referenz-Neumond (1.1.2025 00:00 UTC) und synodischer Monat – für aktuelle Mondphase wenn keine Monduhr aktiv. */
    private static final long MOND_REFERENZ_NEUMOND_MS = 1735603200000L;
    private static final double MOND_SYNODISCHER_MONAT_TAGE = 29.530588;
    private static final int MOND_IMPULSE_PRO_PHASE = 60;

    /** Liefert die aktuelle astronomische Mondphase (0–59), gleiche Formel wie API mondphase-aktuell. */
    private static int getCurrentMoonPhase() {
        try {
            long nowMs = Calendar.getInstance().getTimeInMillis();
            long diffMs = nowMs - MOND_REFERENZ_NEUMOND_MS;
            double tageSeitNeumond = diffMs / (1000.0 * 60.0 * 60.0 * 24.0);
            while (tageSeitNeumond < 0) tageSeitNeumond += MOND_SYNODISCHER_MONAT_TAGE;
            tageSeitNeumond = tageSeitNeumond % MOND_SYNODISCHER_MONAT_TAGE;
            double mondphase = (tageSeitNeumond / MOND_SYNODISCHER_MONAT_TAGE) * MOND_IMPULSE_PRO_PHASE;
            mondphase = mondphase % MOND_IMPULSE_PRO_PHASE;
            if (mondphase < 0) mondphase += MOND_IMPULSE_PRO_PHASE;
            int phase = (int) Math.round(mondphase);
            if (phase >= MOND_IMPULSE_PRO_PHASE) phase = 0;
            return phase;
        } catch (Exception e) {
            return 0;
        }
    }

    /** True, wenn eine Monduhr (Nebenuhr D, zeile 6) aktiv konfiguriert ist – dann Anzeige der eingestellten Phase. */
    private static boolean hasMonduhrAktiv() {
        if (staticContext == null) return false;
        try {
            java.util.List<PlatinenDatabaseHelper.NebenuhrConfig> list = PlatinenDatabaseHelper.getInstance(staticContext).getAllNebenuhren();
            if (list == null) return false;
            for (PlatinenDatabaseHelper.NebenuhrConfig n : list) {
                if (n.zeile == 6 && n.aktiv) return true;
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Aktualisiert die Mondanzeige: Bei aktiver Monduhr die eingestellte Phase, sonst den aktuellen Mondstand.
     */
    public static void printMondAnzeige() {
        if (mondAnzeige == null) {
            return;
        }
        
        try {
            int mondphase = hasMonduhrAktiv() ? StaticVariable.uhrD_mondphaseIst : getCurrentMoonPhase();
            drawMoonOnCanvas(mondphase);
        } catch (Exception e) {
            Log.e("Seite1Layout", "Fehler beim Aktualisieren der Mondanzeige: " + e.getMessage(), e);
            // Bei Fehler: Leeres Bitmap setzen
            if (mondAnzeige != null) {
                int width = mondAnzeige.getWidth() > 0 ? mondAnzeige.getWidth() : 200;
                int height = mondAnzeige.getHeight() > 0 ? mondAnzeige.getHeight() : 200;
                android.graphics.Bitmap emptyBitmap = android.graphics.Bitmap.createBitmap(width, height, android.graphics.Bitmap.Config.ARGB_8888);
                mondAnzeige.setImageBitmap(emptyBitmap);
            }
        }
    }
    
    /**
     * Zeichnet die Mondphase auf einem Canvas/Bitmap basierend auf Impulsen (0-59)
     * Gleiche Logik wie im Schieberegler-Dialog für konsistente Darstellung
     */
    private static void drawMoonOnCanvas(int impulse) {
        if (mondAnzeige == null) {
            return;
        }
        
        try {
            // Normalisiere Impuls auf 0-59 Bereich
            impulse = impulse % 60;
            if (impulse < 0) impulse += 60;
            
            int width = mondAnzeige.getWidth();
            int height = mondAnzeige.getHeight();
            
            // Fallback, falls Größe noch nicht gesetzt
            if (width <= 0 || height <= 0) {
                width = 50; // Noch kleinerer Fallback für kleine Mondanzeige
                height = 50;
            }
            
            // Erstelle Bitmap und Canvas
            android.graphics.Bitmap bitmap = android.graphics.Bitmap.createBitmap(width, height, android.graphics.Bitmap.Config.ARGB_8888);
            android.graphics.Canvas canvas = new android.graphics.Canvas(bitmap);
            
            int r = Math.min(width, height) / 2 - 10; // Radius mit etwas Abstand
            int cx = width / 2;
            int cy = height / 2;
            
            // 1. Hintergrund (Dunkle Seite des Mondes)
            android.graphics.Paint darkPaint = new android.graphics.Paint();
            darkPaint.setColor(0xFF222222); // #222
            darkPaint.setAntiAlias(true);
            canvas.drawCircle(cx, cy, r, darkPaint);
            
            // 2. Berechnung der Phase
            // Impulse 0-59: 0 = Neumond, 30 = Vollmond, 59 = fast Neumond
            // Phase von 0 (Neumond, alles dunkel) bis 1 (Vollmond, alles hell)
            double phase;
            if (impulse <= 30) {
                // Zunehmend: 0-30 -> 0 bis 1 (Neumond bis Vollmond)
                phase = impulse / 30.0;
            } else {
                // Abnehmend: 30-59 -> 1 bis 0 (Vollmond bis Neumond)
                phase = 1.0 - ((impulse - 30) / 30.0);
                if (phase < 0) phase = 0;
            }
            
            // 3. Zeichnen der beleuchteten Fläche (gleiche Logik wie im Schieberegler-Dialog)
            android.graphics.Paint lightPaint = new android.graphics.Paint();
            lightPaint.setColor(0xFFFFF1C1); // #FFF1C1 Mondlicht-Farbe (gleich wie im Dialog)
            lightPaint.setAntiAlias(true);
            
            // Zeichne die beleuchtete Fläche
            // Von rechts nach links hell werden (zunehmend) oder dunkler werden (abnehmend)
            for (int i = -r; i <= r; i++) {
                // y-Koordinate der Lichtkante berechnen
                double y = Math.sqrt(r * r - i * i);
                if (Double.isNaN(y)) continue;
                
                // x-Koordinate der Lichtkante basierend auf Phase
                int xLeft = cx - (int)y;
                int xRight = cx + (int)y;
                
                if (impulse <= 30) {
                    // Zunehmend: Von rechts nach links hell werden
                    // Phase 0 = Neumond (alles dunkel), Phase 1 = Vollmond (alles hell)
                    int xLightEdge = xRight - (int)((xRight - xLeft) * phase);
                    canvas.drawRect(xLightEdge, cy + i, xRight, cy + i + 1, lightPaint);
                } else {
                    // Abnehmend: Von rechts nach links dunkler werden
                    int xLightEdge = xLeft + (int)((xRight - xLeft) * phase);
                    canvas.drawRect(xLeft, cy + i, xLightEdge, cy + i + 1, lightPaint);
                }
            }
            
            // 4. Optional: Rand für bessere Sichtbarkeit (gleich wie im Dialog)
            android.graphics.Paint strokePaint = new android.graphics.Paint();
            strokePaint.setColor(0xFF555555); // #555
            strokePaint.setStyle(android.graphics.Paint.Style.STROKE);
            strokePaint.setStrokeWidth(2);
            strokePaint.setAntiAlias(true);
            canvas.drawCircle(cx, cy, r, strokePaint);
            
            // Setze das Bitmap
            mondAnzeige.setImageBitmap(bitmap);
            
        } catch (Exception e) {
            Log.e("Seite1Layout", "Fehler beim Zeichnen der Mondphase: " + e.getMessage(), e);
        }
    }
    
    /**
     * Alte Methode für Kompatibilität - ruft printMondAnzeige() auf
     */
    public static void updateMondAnzeige() {
        printMondAnzeige();
    }

    public void initInfoText() {
        int infotext_width = (int)(displayWidth * StaticConstants.LAYOUT1_INFOTEXT_WIDTH_PERCENT);
        int infotext_height = (int)(displayHeight * StaticConstants.LAYOUT1_INFOTEXT_HEIGHT_PERCENT);
        int infotext_x = 0 + buchstabenXY[7];
        int infotext_y = (int)(displayHeight * (StaticConstants.LAYOUT1_INFOTEXT_Y_PERCENT_TOP - StaticConstants.LAYOUT1_INFOTEXT_Y_PERCENT_OFFSET)) + buchstabenXY[8];

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
        infoText.setText("INFO TEXT");
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


    /** slotIndex = Slot auf Seite 1 (0..23), für Tag/Relais/Text; Button-Index für View intern. */
    public void initGlockenButton(int width, int height, int x, int y, int slotIndex) {
        Button glockenButton = new Button(context);
        buttons.add(glockenButton);

        int buttonIndex = buttons.size() - 1;
        if (slotIndex >= 0 && slotIndex < 24) slotToButtonIndex[slotIndex] = buttonIndex;
        buttons.elementAt(buttonIndex).setTag(Integer.valueOf(slotIndex));

        buttons.elementAt(buttonIndex).setTextSize(buchstabenGroesseTastenText);

        AbsoluteLayout.LayoutParams alayoutParams = new AbsoluteLayout.LayoutParams(width, height, x, y);
        buttons.elementAt(buttonIndex).setLayoutParams(alayoutParams);

        buttons.elementAt(buttonIndex).setTextColor(buchstabenFarbe);
        setButtonNormal(buttonIndex);

        String buttontext = getButtonText(slotIndex);
        buttons.elementAt(buttonIndex).setText(buttontext);

        buttons.elementAt(buttonIndex).setGravity(Gravity.CENTER);
        this.absolutelayout.addView(buttons.elementAt(buttonIndex));

    }


    public void initStopButton() {
        int width = (int)(displayWidth * StaticConstants.LAYOUT1_STOP_WIDTH_PERCENT);
        int height = (int)(displayHeight * StaticConstants.LAYOUT1_STOP_HEIGHT_PERCENT);
        int x = (int)(displayWidth * (1f - StaticConstants.LAYOUT1_STOP_X_RIGHT_MARGIN_PERCENT)) - width;
        int y = (int)(displayHeight * StaticConstants.LAYOUT1_STOP_Y_PERCENT);

        stopButton = new Button(context);
        // das war die Stop Tafel
        //		   stopButton.setBackgroundDrawable(context.getResources().getDrawable(R.drawable.stop_180x179_transparent));


        AbsoluteLayout.LayoutParams alayoutParams =
                new AbsoluteLayout.LayoutParams(width, height, x, y);
        stopButton.setLayoutParams(alayoutParams);

        stopButton.setBackgroundDrawable(context.getResources().getDrawable(R.drawable.btn_red));
        stopButton.setText("STOP");
        stopButton.setTextColor(this.context.getResources().getColor(R.color.white));


        this.absolutelayout.addView(stopButton);

//		   stopButton.setOnClickListener(new OnClickListener() 
//		   {
//			   @Override
//			   public void onClick(View v) 
//			   {
//				   clrXglockenButtons();
//			   }
//		   });
    }
   

/*	   
       public void initAutomaticButton()
	   {
		   int width =  displayWidth / 4 ;  // displayWidth / 6 ;
		   int height = displayHeight / 6 ; //heightView ;
		   int x = displayWidth/46 ;  // displayWidth - (displayWidth/8) - width ;
		   int y = (displayHeight/30)*5 ;
		   
		   automaticButton = new Button(context);
		   automaticButton.setBackgroundDrawable(context.getResources().getDrawable(R.drawable.btn_green));
		   AbsoluteLayout.LayoutParams alayoutParams =
		   new AbsoluteLayout.LayoutParams(width, height, x, y); 
		   automaticButton.setLayoutParams(alayoutParams);
		   automaticButton.setText("AUTOMATIC = EIN");
		   automaticButton.setTextColor(this.context.getResources().getColor(R.color.white));
		   this.absolutelayout.addView(automaticButton);
       }
*/

    // 12.3.13 das Pfeil Konzept wird verworfen, es gibt nun Buttons "HOME" und "SEITE"
/*	   
	   public void initPfeilRechtsButton()
	   {
		   int width = displayWidth / 16 ;
		   int height = displayHeight / 10 ;
		   int x = displayWidth - (displayWidth/50) - width ;
		   int y = (displayHeight/50);
		   pfeilRechtsButton = new Button(context);
		   pfeilRechtsButton.setBackgroundDrawable(context.getResources().getDrawable(R.drawable.pfeil_rechts_blau_100x102));
		   AbsoluteLayout.LayoutParams alayoutParams =
		   new AbsoluteLayout.LayoutParams(width, height, x, y); 
		   pfeilRechtsButton.setLayoutParams(alayoutParams);
		   this.absolutelayout.addView(pfeilRechtsButton);
	   }
*/	   
	   
/*	   
 	   public void makeGlockenButtons()
	   {
		   //berechneGlockenButton_xy();
	//	   glocken_button_width = displayWidth /3 ;
		   glocken_button_height = displayHeight / 5 ;
		   glocken_button_x = ( displayWidth /4 )  - (glocken_button_width /2  ) ;
		   glocken_button_y = ( displayHeight / 10 ) * 3 ;
		   
		   initGlockenButton(glocken_button_width, glocken_button_height, glocken_button_x, glocken_button_y, 0);
		   initListenerGlockenButton(0);
		   
		   glocken_button_x = ((displayWidth /4 ) * 3 ) - (glocken_button_width /2);
		   initGlockenButton(glocken_button_width, glocken_button_height, glocken_button_x, glocken_button_y, 1);
		   initListenerGlockenButton(1);
	   }
*/


    //	   public void berechne_button_width()
//	   {
//		   if (glockenCountOhneNULL < 3)
//		   {
//			   	glocken_button_width=displayWidth / 3 ;
//		   }
//		   else if (glockenCountOhneNULL < 5)
//		   		{ 
//			   		glocken_button_width=displayWidth / 4 ;
//		   		}
//		        else 
//		        {
//		        	glocken_button_width=displayWidth / 6 ;
//		        }
//		   
//	   }
//	
    int[] buttonX = new int[8];

    private int glockenCountOhneNULL;

    private int kloepelFaengerCountOhneNULL;

    private int gruppeDreiCountOhneNULL;

    /** Slot (0..23) → Button-Index, für buttonOff/buttonOnOK wenn Tag = Slot-Index. */
    private final int[] slotToButtonIndex = new int[24];


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

    /** Leeres Tastenfeld („leer“): Platzhalter ohne Farbe, nimmt einen Platz in der Zeile ein. */
    private void addLeerPlaceholder(int width, int height, int x, int y) {
        View placeholder = new View(context);
        AbsoluteLayout.LayoutParams lp = new AbsoluteLayout.LayoutParams(width, height, x, y);
        absolutelayout.addView(placeholder, lp);
    }

    public void makeXglockenButtons() {
        buttons.clear();
        for (int i = 0; i < 24; i++) slotToButtonIndex[i] = -1;

        berechne_glocken_button_x(glockenCountOhneNULL);
        StaticVariable.tastenBreite = glocken_button_width;
        glocken_button_height = (int)(displayHeight * StaticConstants.LAYOUT1_GLOCKEN_BUTTON_HEIGHT_PERCENT);
        StaticVariable.tastenHoehe = glocken_button_height;
        glocken_button_y = (int)(displayHeight * StaticConstants.LAYOUT1_GLOCKEN_ROW1_Y_PERCENT) + buchstabenXY[13];

        int ix = 0;
        for (int i = 0; i < 8; i++) {
            int leerTasteTemp = leerTaste(i + 1);
            if (leerTasteTemp == 0) {
                initGlockenButton(glocken_button_width, glocken_button_height, buttonX[ix], glocken_button_y, i);
                ix++;
            } else if (leerTasteTemp == 1) {
                addLeerPlaceholder(glocken_button_width, glocken_button_height, buttonX[ix], glocken_button_y);
                ix++;
            }
        }

        berechne_glocken_button_x(kloepelFaengerCountOhneNULL);
        glocken_button_y = (int)(displayHeight * StaticConstants.LAYOUT1_GLOCKEN_ROW2_Y_PERCENT) + buchstabenXY[13];

        ix = 0;
        for (int i = 0; i < 8; i++) {
            int leerTasteTemp = leerTaste(i + 1 + 8);
            if (leerTasteTemp == 0) {
                initGlockenButton(glocken_button_width, glocken_button_height, buttonX[ix], glocken_button_y, 8 + i);
                ix++;
            } else if (leerTasteTemp == 1) {
                addLeerPlaceholder(glocken_button_width, glocken_button_height, buttonX[ix], glocken_button_y);
                ix++;
            }
        }

        if (gruppeDreiCountOhneNULL > 0) {
            berechne_glocken_button_x(gruppeDreiCountOhneNULL);
            glocken_button_y = (int)(displayHeight * StaticConstants.LAYOUT1_GLOCKEN_ROW3_Y_PERCENT) + buchstabenXY[13];

            ix = 0;
            for (int i = 0; i < 8; i++) {
                int leerTasteTemp = leerTaste(i + 1 + 8 + 8);
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
	   public void clrXglockenButtons()
	   {
		   for (int i = 0; i < glockenCount; i++)
		   {
			   setButtonGreen(i);
			   String buttontext = getButtonText(i+1) ;
			   buttons.elementAt(i).setText(buttontext);
		   }
		   for (int i =0; i < kloepelFaengerCount; i++)
		   {
			   setButtonGreen(i+glockenCount);
			   String buttontext = getButtonText(i+9);
			   buttons.elementAt(i+glockenCount).setText(buttontext);
		   }
		   for (int i = 0 ; i < gruppeDreiCount; i ++)
		   {
			   
		   }
	   }
*/

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
                       try {
                        while(  (farbenExcelread.getCellString(spalte, zeile_index).equals("null"))
                                ||
                                ( (farbenExcelread.getCellString(7 , (i ) ).equals("NULL")) )
                                ||
                                (farbenExcelread.getCellString(spalte, zeile_index).equals("NULL"))
                             )
                           {
                               if(zeile_index >= max_zeilen)
                               {
                                   break ;
                               }
                               zeile_index ++ ;
                           }
                    } catch (ArrayIndexOutOfBoundsException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                        new LogExcelError(spalte, zeile_index, beschriftungTastenFilename, FARBEN_SHEET_NUMBER_NEW,sourceFileName,702) ;
                    } catch (Exception e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                        new LogExcelError(spalte, zeile_index, beschriftungTastenFilename, FARBEN_SHEET_NUMBER_NEW,sourceFileName,706) ;
                    }
                   }
               }

               return zeile_index ;
            }
    */
    public void setButtonOnOk(int index) {
        if (index < 0 || index >= buttons.size() || index >= btn_manual_ein_ok_drawable.size()) return;
        buttons.elementAt(index).setBackgroundDrawable(btn_manual_ein_ok_drawable.get(index));
    }

    private void setButtonOnError(int index) {
        if (index < 0 || index >= buttons.size() || index >= btn_manual_ein_error_drawable.size()) return;
        buttons.elementAt(index).setBackgroundDrawable(btn_manual_ein_error_drawable.get(index));
    }

    public void setButtonOnAutomatic(int index) {
        if (index < 0 || index >= buttons.size() || index >= btn_automatic_ein_ok_drawable.size()) return;
        buttons.elementAt(index).setBackgroundDrawable(btn_automatic_ein_ok_drawable.get(index));
    }

    private void setButtonOnAutomaticError(int index) {
        if (index < 0 || index >= buttons.size() || index >= btn_automatic_ein_error_drawable.size()) return;
        buttons.elementAt(index).setBackgroundDrawable(btn_automatic_ein_error_drawable.get(index));
    }

    public void setButtonNormal(int index) {
        //buttons.elementAt(index).setBackgroundDrawable(context.getResources().getDrawable(R.drawable.btn_gray));
        // 10.3.13 geaender auf Bitmap sdCard

//		   int tabellen_zeile = searchButtonColor(6, index);
//		   button_farbe_name = farbenExcelread.getCellString(6, tabellen_zeile);
//		   btn_normal_drawable = new BitmapDrawable((BitmapFactory.decodeFile(TurmtechnikActivity.sdCardPath + "/Turmtechnik/Grafiken/" + button_farbe_name )));

        if (index >= 0 && index < buttons.size() && index < btn_normal_drawable.size()) {
            buttons.elementAt(index).setBackgroundDrawable(btn_normal_drawable.get(index));
        }
    }

    /** localIndex = Slot-Index (0..23) aus getTag(). */
    public void buttonOff(int localIndex) {
        int bi = (localIndex >= 0 && localIndex < 24) ? slotToButtonIndex[localIndex] : localIndex;
        if (bi < 0 || bi >= buttons.size()) return;
        setButtonNormal(bi);
        String buttonText = getButtonText(localIndex);
        buttons.elementAt(bi).setText(buttonText);
    }

    public void buttonOnOK(int localIndex) {
        int bi = (localIndex >= 0 && localIndex < 24) ? slotToButtonIndex[localIndex] : localIndex;
        if (bi < 0 || bi >= buttons.size()) return;
        setButtonOnOk(bi);
        String buttonText = getButtonText(localIndex);
        buttons.elementAt(bi).setText(buttonText);
    }

    public void buttonOnError(int localIndex) {
        int bi = (localIndex >= 0 && localIndex < 24) ? slotToButtonIndex[localIndex] : localIndex;
        if (bi < 0 || bi >= buttons.size()) return;
        setButtonOnError(bi);
        buttons.elementAt(bi).setText(getButtonText(localIndex));
    }

    public void buttonOnRed(int localIndex) {
        int bi = (localIndex >= 0 && localIndex < 24) ? slotToButtonIndex[localIndex] : localIndex;
        if (bi < 0 || bi >= buttons.size()) return;
        setButtonOnAutomatic(bi);
        buttons.elementAt(bi).setText(getButtonText(localIndex));
    }

    public void buttonOnErrorRed(int localIndex) {
        int bi = (localIndex >= 0 && localIndex < 24) ? slotToButtonIndex[localIndex] : localIndex;
        if (bi < 0 || bi >= buttons.size()) return;
        setButtonOnAutomaticError(bi);
        buttons.elementAt(bi).setText(getButtonText(localIndex));
    }

    public void buttonOnStop(int localIndex) {
        int bi = (localIndex >= 0 && localIndex < 24) ? slotToButtonIndex[localIndex] : localIndex;
        if (bi < 0 || bi >= buttons.size()) return;
        setButtonOnAutomatic(bi);
        buttons.elementAt(bi).setText(getButtonText(localIndex));
    }

    private int checkIndexTemp(int index) {
        if (index >= glockenCount) {
            index = index - glockenCount + 9;
        } else {
            index = index + 1;
        }
        return index;
    }

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
        // Web-Server IP-Adresse anzeigen
        String webIp = TurmtechnikActivity.getWebServerIpAddress();
        if (webIp != null && !webIp.isEmpty()) {
            datum.append("\nWeb:" + webIp + ":8080");
        }
        // Versionsanzeige
        String version = getVersionString();
        if (version != null && !version.isEmpty()) {
            datum.append("\n" + version);
        }
    }
    
    /**
     * Holt die App-Version aus dem PackageManager.
     */
    private static String getVersionString() {
        try {
            if (staticContext != null) {
                android.content.pm.PackageManager pm = staticContext.getPackageManager();
                String packageName = staticContext.getPackageName();
                android.content.pm.PackageInfo packageInfo = pm.getPackageInfo(packageName, 0);
                return "V" + packageInfo.versionName;
            }
        } catch (Exception e) {
            // Fallback auf StaticVariable.versionString
            if (StaticVariable.versionString != null && !StaticVariable.versionString.isEmpty()) {
                return StaticVariable.versionString;
            }
        }
        return null;
    }

    public static void printUhr() {
        uhr.setText(getUhrzeit());
    }

    public AbsoluteLayout initLayout() {
        initLogo();
        //Log.e("initLoge" , "erledigt") ;
        initClock();
        //Log.e("initClock" , "erledigt") ;
        initDatum();
        //Log.e("initDatum" , "erledigt") ;
        initMondAnzeige();
        //Log.e("initMondAnzeige" , "erledigt") ;
//	       initStopButton();
        // initPfeilRechtsButton(); wir durch "HOME" und "SEITE" ersetzt 12.6.13
//	       initAutomaticButton();
        makeXglockenButtons();
        //Log.e("makeXglockenButtons" , "erledigt") ;
        initInfoText();
        //Log.e("initInfoText" , "erledigt") ;
        if (excelread != null) {
            excelread.closeWorkbook();
            excelread = null;
        }
        System.gc();

        AbsoluteLayout tempAbsoluteLayout = this.absolutelayout;
        StaticVariable.layoutReady = true;
        //return this.absolutelayout;
        return tempAbsoluteLayout;
    }

    private int getGlockenCount() {
        if (beschriftungRowsFromDb != null) {
            glockenCountOhneNULL = 0;
            for (int i = 0; i < 8 && i < beschriftungRowsFromDb.size(); i++) {
                PlatinenDatabaseHelper.BeschriftungTastenRow row = beschriftungRowsFromDb.get(i);
                String c2 = row.c2 != null ? row.c2.trim() : "";
                if ("NULL".equals(c2) || "null".equalsIgnoreCase(c2)) continue;
                glockenCountOhneNULL++;
            }
            return Math.min(8, beschriftungRowsFromDb.size());
        }
        if (excelread == null) return 0;
        int glockencount = 0;
        glockenCountOhneNULL = 0;
        int glockenZeilen = excelread.getCellZeilen();
        glockenZeilen = Math.min(glockenZeilen, 9);
        for (int i = 1; i < glockenZeilen; i++) {
            String strtemp = "";
            try {
                strtemp = excelread.getCellString(2, i);

                //Log.e("strtemp" , "= " + strtemp) ;

            } catch (ArrayIndexOutOfBoundsException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                new LogExcelError(2, i, beschriftungTastenFilename, FARBEN_SHEET_NUMBER_NEW, sourceFileName, 849);
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            if (!((strtemp.equals(""))))  // || (strtemp.equals("NULL")) ) )
            {
                glockencount++;
            }
            if (!"null".equalsIgnoreCase(strtemp) && !"NULL".equals(strtemp)) glockenCountOhneNULL++;
        }
        return glockencount;
    }

    /** Zeile ist Systemtaste (Stop, Help, Home, Zweite Seite, …), auch bei leerem c2. Öffentlich für Seite2Layout. */
    public static boolean isSystemTasteRow(PlatinenDatabaseHelper.BeschriftungTastenRow r) {
        if (r == null) return false;
        if (r.sonderId != null) return true;
        String c1 = (r.c1 != null ? r.c1 : "").trim();
        return "Stop".equalsIgnoreCase(c1) || "Automatik".equalsIgnoreCase(c1) || "Home".equalsIgnoreCase(c1)
                || "Help".equalsIgnoreCase(c1) || "Schlagwerk".equalsIgnoreCase(c1) || "Zweite Seite".equalsIgnoreCase(c1)
                || "Programmeingeben".equalsIgnoreCase(c1) || "Programmabfrage".equalsIgnoreCase(c1) || "Nebenuhr Stellen".equalsIgnoreCase(c1);
    }

    private int leerTaste(int index) // 0 == keine leer oder NULL Taste
    // 1 == leer Taste
    // 2 == NULL Taste also komplett leere Zeile
    // index = 1-based (Excel row 1..24 for page 1)
    {
        if (beschriftungRowsFromDb != null) {
            int rowIndex = index - 1;
            if (rowIndex < 0 || rowIndex >= beschriftungRowsFromDb.size()) return 2;
            PlatinenDatabaseHelper.BeschriftungTastenRow row = beschriftungRowsFromDb.get(rowIndex);
            String c2 = row.c2 != null ? row.c2.trim() : "";
            if ("leer".equalsIgnoreCase(c2)) return 1;
            if ("NULL".equals(c2)) return 2;
            if (c2.isEmpty()) return isSystemTasteRow(row) ? 0 : 2; // Systemtasten mit leerem c2 = echte Taste, sonst NULL
            return 0;
        }
        if (excelread == null) return 2;
        //Log.e("leerTaste" , "index=" + index) ;
        String temp = "";
        try {
            temp = (excelread.getCellString(2, index).trim());
        } catch (ArrayIndexOutOfBoundsException e) {
            // TODO Auto-generated catch block
            //e.printStackTrace();
            //new LogExcelError(2, index, beschriftungTastenFilename, FARBEN_SHEET_NUMBER_NEW,sourceFileName,871) ;
            // 17.06.15 ausgebaut macht bei letzter Zeile irrtuemlich Fehler
        } catch (Exception e) {
            // TODO Auto-generated catch block
            //e.printStackTrace();
            //new LogExcelError(2, index, beschriftungTastenFilename, FARBEN_SHEET_NUMBER_NEW,sourceFileName,875) ;
        }
        //Log.i("temp" , "=" + temp) ;
        int retCode = 0;
        //Log.e("checkLeerTaste" , "index=" + index + "temp=" + temp) ;
        if ((temp.equals("leer"))) {
            retCode = 1;
        }
        if ((temp.equals("NULL"))) {
            retCode = 2;
        }
        if ((temp.equals(""))) {
            retCode = 2; // 17.3.2015 bei gar nichts z.b. Gruppe 3 und Tabellen ende
        }

        //Log.e("retCode" , "=" + retCode) ;
        return retCode;
    }

    private int getKloeppelCount() {
        if (beschriftungRowsFromDb != null) {
            kloepelFaengerCountOhneNULL = 0;
            for (int i = 8; i < 16 && i < beschriftungRowsFromDb.size(); i++) {
                PlatinenDatabaseHelper.BeschriftungTastenRow row = beschriftungRowsFromDb.get(i);
                String c2 = row.c2 != null ? row.c2.trim() : "";
                if ("NULL".equals(c2) || "null".equalsIgnoreCase(c2)) continue;
                kloepelFaengerCountOhneNULL++;
            }
            return Math.min(8, Math.max(0, beschriftungRowsFromDb.size() - 8));
        }
        if (excelread == null) return 0;
        int kloeppelcount = 0;
        kloepelFaengerCountOhneNULL = 0;
        int zeilen = excelread.getCellZeilen();
        zeilen = Math.min(zeilen, 17);

        for (int i = 9; i < zeilen; i++) {
            String strtemp = "null";
            try {
                strtemp = excelread.getCellString(2, i);

                //Log.e("strtemp" , "= " + strtemp) ;
            } catch (ArrayIndexOutOfBoundsException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                new LogExcelError(2, i, beschriftungTastenFilename, FARBEN_SHEET_NUMBER_NEW, sourceFileName, 894);
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                new LogExcelError(2, i, beschriftungTastenFilename, FARBEN_SHEET_NUMBER_NEW, sourceFileName, 898);
            }
            if (!((strtemp.equals("")))) //|| (strtemp.equals("NULL")) ) )
            {
                kloeppelcount++;
            }
            if (!"null".equalsIgnoreCase(strtemp) && !"NULL".equals(strtemp)) kloepelFaengerCountOhneNULL++;
        }
        return kloeppelcount;
    }

    private int getGruppeDreiCount() {
        if (beschriftungRowsFromDb != null) {
            gruppeDreiCountOhneNULL = 0;
            for (int i = 16; i < 24 && i < beschriftungRowsFromDb.size(); i++) {
                PlatinenDatabaseHelper.BeschriftungTastenRow row = beschriftungRowsFromDb.get(i);
                String c2 = row.c2 != null ? row.c2.trim() : "";
                if ("NULL".equals(c2) || "null".equalsIgnoreCase(c2)) continue;
                gruppeDreiCountOhneNULL++;
            }
            return Math.min(8, Math.max(0, beschriftungRowsFromDb.size() - 16));
        }
        if (excelread == null) return 0;
        int gruppe3count = 0;
        int zeilen = excelread.getCellZeilen();
        zeilen = Math.min(zeilen, 25);

        //for (int i = 17 ; i < 17+8 ; i++)
        for (int i = 17; i < zeilen; i++) {
            String strtemp = "null";
            try {
                strtemp = excelread.getCellString(2, i);

                //Log.e("strtemp" , "= " + strtemp) ;
            } catch (ArrayIndexOutOfBoundsException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                new LogExcelError(2, i, beschriftungTastenFilename, FARBEN_SHEET_NUMBER_NEW, sourceFileName, 919);
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                new LogExcelError(2, i, beschriftungTastenFilename, FARBEN_SHEET_NUMBER_NEW, sourceFileName, 923);
            }
            if (!((strtemp.equals("")))) // || (strtemp.equals("NULL")) ) )
            {
                gruppe3count++;
            }
            if (!"null".equalsIgnoreCase(strtemp) && !"NULL".equals(strtemp)) gruppeDreiCountOhneNULL++;
        }
        return gruppe3count;
    }

    /** Farben pro Tastentyp (Vorlagen), wenn keine Grafik (c6) in DB – wie in der Web-UI TYPEN_FARBE. */
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

    /** Liefert Grafik/Sonder-Wert: bei DB aus Zeile (oneBasedRowIndex 1-based wie Excel), sonst aus farbenExcelread.
     * Grafik-Spalten (6–10): wenn in DB leer, Fallback auf Excel, damit Tasten-Farben aus Beschriftung-Tasten.xls genutzt werden. */
    private static String getFarbenCell(int col, int oneBasedRowIndex) {
        if (beschriftungRowsFromDb != null && oneBasedRowIndex >= 1 && oneBasedRowIndex <= beschriftungRowsFromDb.size()) {
            PlatinenDatabaseHelper.BeschriftungTastenRow r = beschriftungRowsFromDb.get(oneBasedRowIndex - 1);
            if (col == 1) return r.c1 != null ? r.c1 : "";
            if (col == 2) return r.c2 != null ? r.c2 : "";
            if (col == 3) return r.sonderId != null ? String.valueOf(r.sonderId) : "";
            // Grafik-Spalten: nur wenn in DB befüllt nutzen, sonst Fallback auf Excel (c6–c10 sind oft leer in DB)
            if (col == 6) { String v = r.c6 != null ? r.c6.trim() : ""; if (!v.isEmpty()) return r.c6; }
            if (col == 7) { String v = r.c7 != null ? r.c7.trim() : ""; if (!v.isEmpty()) return r.c7; }
            if (col == 8) { String v = r.c8 != null ? r.c8.trim() : ""; if (!v.isEmpty()) return r.c8; }
            if (col == 9) { String v = r.c9 != null ? r.c9.trim() : ""; if (!v.isEmpty()) return r.c9; }
            if (col == 10) { String v = r.c10 != null ? r.c10.trim() : ""; if (!v.isEmpty()) return r.c10; }
            if (col != 6 && col != 7 && col != 8 && col != 9 && col != 10) return "";
        }
        try {
            return farbenExcelread != null ? farbenExcelread.getCellString(col, oneBasedRowIndex) : "";
        } catch (Exception e) {
            return "";
        }
    }

    /** Füllt beschriftung, relaisnumber, hammerzeit, buttonId slotbasiert: genau 24 Einträge für Seite 1 (Index = Slot 0..23). NULL/leer = Platzhalter („“, 0). Damit stimmen Klick und Relais auch nach NULL-Slots. */
    private void loadButtonTextFromDatabase(java.util.List<PlatinenDatabaseHelper.BeschriftungTastenRow> rows) {
        int page1Slots = Math.min(24, rows != null ? rows.size() : 0);
        for (int slot = 0; slot < page1Slots; slot++) {
            PlatinenDatabaseHelper.BeschriftungTastenRow r = rows.get(slot);
            String c2 = (r.c2 != null ? r.c2 : "").trim();
            if ("leer".equalsIgnoreCase(c2) || "NULL".equals(c2) || "null".equals(c2)) {
                beschriftung.add("");
                relaisnumber.add(0);
                buttonId.add("");
                hammerzeit.add(0);
                continue;
            }
            if (c2.isEmpty()) {
                if (!isSystemTasteRow(r)) {
                    beschriftung.add("");
                    relaisnumber.add(0);
                    buttonId.add("");
                    hammerzeit.add(0);
                    continue;
                }
                c2 = (r.c1 != null ? r.c1 : "").trim();
            }
            String c3 = (r.c3 != null ? r.c3 : "").trim();
            String c5 = (r.c5 != null ? r.c5 : "1").trim();
            int relNumber = 0;
            if (r.sonderId != null) {
                relNumber = r.sonderId;
            } else if (!c3.isEmpty() && !"null".equalsIgnoreCase(c3) && !"leer".equalsIgnoreCase(c3)) {
                try {
                    relNumber = Integer.parseInt(c3);
                } catch (NumberFormatException e) {
                    relNumber = 0;
                }
            }
            int platNumber = 1;
            if (!c5.isEmpty() && !"null".equalsIgnoreCase(c5) && !"leer".equalsIgnoreCase(c5)) {
                try {
                    platNumber = (int) (Double.parseDouble(c5));
                    if (platNumber < 1) platNumber = 1;
                } catch (NumberFormatException e) {
                    platNumber = 1;
                }
            }
            beschriftung.add(c2);
            relaisnumber.add(relNumber + ((platNumber - 1) * 32));
            buttonId.add(r.c13 != null ? r.c13 : "");
            int hammertemp = 0;
            String c4 = r.c4 != null ? r.c4 : "";
            if (!c4.equals("null") && !c4.equals("NULL") && !c4.trim().isEmpty() && !"leer".equalsIgnoreCase(c4.trim())) {
                try {
                    hammertemp = (int) (Double.parseDouble(c4.trim())) * 10;
                } catch (NumberFormatException e) {
                    hammertemp = 0;
                }
            }
            hammerzeit.add(hammertemp);
        }
    }

    private void loadButtonText_andRelaisNumber_andHammerZeit() {   // Log.i("Suche" , "relais, hammer") ;
        // Statische Vektoren zuerst leeren, damit bei erneutem Layout-Aufbau keine doppelten Einträge entstehen (verhindert Einfrieren)
        beschriftung.clear();
        relaisnumber.clear();
        hammerzeit.clear();
        buttonId.clear();
        // Zuerst aus DB laden (Import aus Beschriftung-Tasten.xls beim Start), sonst Excel-Fallback
        try {
            if (context != null) {
                PlatinenDatabaseHelper dbHelper = PlatinenDatabaseHelper.getInstance(context);
                java.util.List<PlatinenDatabaseHelper.BeschriftungTastenRow> rows = dbHelper.getBeschriftungTasten();
                if (rows != null && !rows.isEmpty()) {
                    beschriftungRowsFromDb = rows;
                    loadButtonTextFromDatabase(rows);
                    return;
                }
            }
        } catch (Exception e) {
            Log.w(sourceFileName, "Beschriftung aus DB fehlgeschlagen", e);
        }
        // Nur Datenbank: Kein Excel-Fallback. Bei leerer DB in der Web-UI „Aus Excel importieren“ nutzen.
        beschriftungRowsFromDb = null;
        return;
    }

    @SuppressWarnings("unused")
    private void loadButtonText_ExcelFallback_DEAKTIVIERT() {
        int momentaneId = 0;
        int sheetZeilen = excelread.getCellZeilen();
        //for (int i = 1 ; i < 1 + 24 + 24 ; i++) // 3 Reihen 8 Tasten 1te Seite, das selbe 2te Seite //13.6.13
        for (int i = 1; i < sheetZeilen; i++) {
            try {

                if (excelread.getCellString(2, i).equals("")) {
                    break; // leeres Tabellen element
                }
                if (((!excelread.getCellString(2, i).equals("leer")
                        && (!excelread.getCellString(2, i).equals("NULL"))

                )
                )) {
                    if (!((excelread.getCellString(2, i).equals("null"))
                            || (excelread.getCellString(2, i).equals("NULL"))
                    )
                            ) // ist noch eine Glocke, Kloeppel, Hammer?
                    {
                        beschriftung.add(excelread.getCellString(2, i));

                        //Log.e("beschriftung:" , (excelread.getCellString(2, i))) ;

                        //Log.i("schleifen index" , "=" + i) ;
                        String relaisNumber = excelread.getCellString(3, i);

                        //Log.i("STRING" , "relaisNumber=" + relaisNumber) ;
                        String PlatineNumber = excelread.getCellString(5, i);
                        int relNumber = (Integer.parseInt(relaisNumber));

                        int platNumber;
                        if (PlatineNumber.equals("null") || PlatineNumber.equals("NULL")) {
                            platNumber = 1;
                        } else {

                            platNumber = (int) (Double.parseDouble(PlatineNumber));
                        }


                        int rlnumtemp = relNumber + ((platNumber - 1) * 32); // 4 carambola platinen
                        Log.e("platnummber", "final=" + platNumber);
                        Log.e("relaisnumber", "final=" + rlnumtemp);
                        relaisnumber.add(rlnumtemp);
                        //fernwartungId.add(momentaneId) ;
                        buttonId.add(excelread.getCellString(13, i));
                        //Log.e("relaisvector.size" , "=" + relaisnumber.size()) ;

//			       relaisnumber.add(Integer.parseInt(relaisNumber));


                        // Achtung!!! Hammer kann auf Taste sein oder auch nicht!!
                        // wir in der naechsten Schleife getestet

                        int hammertemp = 0;
                        // Log.i("index" , "hammertime=" + excelread.getCellString(4, i)) ;
                        if (!((excelread.getCellString(4, i).equals("null"))
                                || (excelread.getCellString(4, i).equals("NULL"))
                                || (excelread.getCellString(4, i).equals(""))
                        )
                                ) {
                            hammertemp = (int) (Double.parseDouble(excelread.getCellString(4, i)));
                            hammertemp = hammertemp * 10; // zehntel Sekunden auf ms umwandeln ;
                        }
                        hammerzeit.add(hammertemp);
                    }
                    momentaneId++;
                } else {
                    momentaneId++; // leer id's ueberspringen
                }

            } catch (ArrayIndexOutOfBoundsException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                new LogExcelError(2, i, beschriftungTastenFilename, FARBEN_SHEET_NUMBER_NEW, sourceFileName, 995);
            } catch (NumberFormatException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                new LogExcelError(2, i, beschriftungTastenFilename, FARBEN_SHEET_NUMBER_NEW, sourceFileName, 999);
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                new LogExcelError(2, i, beschriftungTastenFilename, FARBEN_SHEET_NUMBER_NEW, sourceFileName, 1003);
            }
        }

        int hammertemp = 0;
        int zeilen = excelread.getCellZeilen();
        //Log.i("HAMMER" , "zeilen=" + zeilen) ;

        for (int i = 1; i < zeilen; i++) {
            hammertemp = 0;
            // hammerzeit != 0 und keine Beschriftung!!! dann hinzufuegen
            // bei beschriftung wird das schon in der oberen schleife hinzugefueget!!!
            try {

                if ((!excelread.getCellString(4, i).equals("null") && (excelread.getCellString(2, i).equals("null")))
                        &&
                        (!excelread.getCellString(4, i).equals("NULL") && (excelread.getCellString(2, i).equals("NULL")))
                        ) {
                    hammertemp = (int) (Double.parseDouble(excelread.getCellString(4, i)));
                    hammertemp = hammertemp * 10; // zehntel Sekunden auf ms umwandeln ;

                    //Log.e("hammerindex" , "=" + i ) ;
                    //Log.e("hammerzeit" , "=" + hammertemp ) ;

                    String relaisNumber = excelread.getCellString(3, i);
                    String platineNumber = excelread.getCellString(5, i);
                    int relNumber = 0;
                    int platNumber = 1;
                    if (!relaisNumber.equals("NULL")) {
                        relNumber = (Integer.parseInt(relaisNumber));
                    }

                    if (!platineNumber.equals("NULL")) {
                        platNumber = (int) (Double.parseDouble(platineNumber));

                    }
                    relaisnumber.add(relNumber + ((platNumber - 1) * 32)); // 4 carambola platinen

                    hammerzeit.add(hammertemp);
                }
            } catch (ArrayIndexOutOfBoundsException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                new LogExcelError(4, i, beschriftungTastenFilename, FARBEN_SHEET_NUMBER_NEW, sourceFileName, 1041);
            } catch (NumberFormatException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                new LogExcelError(4, i, beschriftungTastenFilename, FARBEN_SHEET_NUMBER_NEW, sourceFileName, 1045);
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                new LogExcelError(4, i, beschriftungTastenFilename, FARBEN_SHEET_NUMBER_NEW, sourceFileName, 1049);
            }

        }
        //Log.e("hammervectror" , "size=" + hammerzeit.size() );
        //Log.e("relaisvector" , "size=" + relaisnumber.size() );
    }

    /** Anzahl Slots/Zeilen für Seite 1 (max 24 = 3×8). Wird u.a. als Offset für Seite 2 genutzt (relaisNumber[offset+0], …). */
    private int countRelaisTotal() // die Relais beginnen bei zeile 1
    {
        if (beschriftungRowsFromDb != null) {
            return Math.min(24, beschriftungRowsFromDb.size()); // Seite 2 beginnt bei Index 24
        }
        if (excelread == null) return 0;
        int relais_count = 0;
        int zeilen = excelread.getCellZeilen();
        zeilen = Math.min(zeilen, 25); // maximal 3 Reichen zu 8 Tasten moeglich
        // aber plus 1 weil beginnt bei 1
        //for (int i = 1 ; i < 1 + 8 + 8 + 8 ; i++) // 3 * 8 Tasten moeglich
        for (int i = 1; i < zeilen; i++) {
            try {
                //Log.e("excelread" , "=" + excelread.getCellString(3, i)) ;
                if (!((excelread.getCellString(3, i).equals("null"))
                        ||
                        (excelread.getCellString(3, i).equals("0.0"))
                        ||
                        (excelread.getCellString(3, i).equals("NULL"))
                        ||
                        (excelread.getCellString(3, i).equals(""))
                )
                        ) {
                    relais_count++;
                    //Log.e("count Relais" , "=" + relais_count) ;
                }
            } catch (ArrayIndexOutOfBoundsException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                new LogExcelError(3, i, beschriftungTastenFilename, FARBEN_SHEET_NUMBER_NEW, sourceFileName, 1074);
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                new LogExcelError(3, i, beschriftungTastenFilename, FARBEN_SHEET_NUMBER_NEW, sourceFileName, 1078);
            }
        }

        return relais_count;
    }

    private int countVerknuepftSeite1() {
        if (beschriftungRowsFromDb != null) {
            int n = 0;
            for (PlatinenDatabaseHelper.BeschriftungTastenRow r : beschriftungRowsFromDb) {
                String c1 = r.c1 != null ? r.c1.trim() : "";
                if ("verknuepft".equalsIgnoreCase(c1) || "Verknüpft".equals(c1)) n++;
            }
            return n;
        }
        if (excelread == null) return 0;
        int zeilen = excelread.getCellZeilen();
        int verknuepft_count = 0;
        for (int i = 1; i < zeilen; i++) {
            try {
                if ((excelread.getCellString(1, i).equals("verknuepft"))
                        || excelread.getCellString(1, i).equals("Verknüpft")
                        ) {
                    verknuepft_count++;
                }
            } catch (ArrayIndexOutOfBoundsException e) {
                e.printStackTrace();
                new LogExcelError(1, i, beschriftungTastenFilename, FARBEN_SHEET_NUMBER_NEW, sourceFileName, 1098);
            } catch (Exception e) {
                e.printStackTrace();
                new LogExcelError(1, i, beschriftungTastenFilename, FARBEN_SHEET_NUMBER_NEW, sourceFileName, 1102);
            }
        }
        return verknuepft_count;
    }

    public static int get_counted_relais_total() {
        return counted_relais_total;
    }

    public static int get_counted_verknuepft_seite1() {
        return counted_verknuepft_seite1;
    }

    private String getButtonText(int index) {
        if (index < 0 || index >= beschriftung.size()) return "";
        return beschriftung.elementAt(index);
    }

    public int getRelaisNumber(int index) {
        if (index >= relaisnumber.size()) {
            return 0;
        } else {
            return relaisnumber.elementAt(index);
        }
    }

    public int getHammerZeit(int index) {
        if (index >= hammerzeit.size()) {
            return 0;
        } else {
            return hammerzeit.elementAt(index);
        }
    }

    /* neue Version ab 29.06.2014
           public int getFernwartungID (int index)
           {
               if (index >= fernwartungId.size())
               {
                   return -1 ;
               }
               else
               {
                   return fernwartungId.elementAt(index) ;

               }
           }
    */
    public String getButtonId(int index) {
        if (index >= buttonId.size()) {
            return "null";
        } else {
            return buttonId.elementAt(index);
        }
    }


    public static int getAnzahlDerBeschriftungen() {

        return beschriftung.size();
    }

    private void readVerknuepfteTastenTexts() {
        UhrThread.verknuepfteTastenString.clear();
        if (beschriftungRowsFromDb != null) {
            for (PlatinenDatabaseHelper.BeschriftungTastenRow r : beschriftungRowsFromDb) {
                String c1 = r.c1 != null ? r.c1.trim() : "";
                if ("verknuepft".equalsIgnoreCase(c1) || "Verknüpft".equals(c1)) {
                    String name = r.c2 != null ? r.c2.trim() : "";
                    if (!name.isEmpty()) UhrThread.verknuepfteTastenString.add(name);
                }
            }
            return;
        }
        if (excelread == null) return;
        int zeilen_max = excelread.getCellZeilen();
        for (int i = 1; i < zeilen_max; i++) {
            try {
                if ((excelread.getCellString(1, i).equals("verknuepft"))
                        || excelread.getCellString(1, i).equals("Verknüpft")
                        ) {
                    String verknuepftTemp = excelread.getCellString(2, i);
                    UhrThread.verknuepfteTastenString.add(verknuepftTemp);
                }
            } catch (ArrayIndexOutOfBoundsException e) {
                e.printStackTrace();
                new LogExcelError(2, i, beschriftungTastenFilename, FARBEN_SHEET_NUMBER_NEW, sourceFileName, 1181);
            } catch (Exception e) {
                e.printStackTrace();
                new LogExcelError(2, i, beschriftungTastenFilename, FARBEN_SHEET_NUMBER_NEW, sourceFileName, 1185);
            }
        }
    }

    /** Lädt Bitmap aus Grafiken-Ordner nur wenn Dateiname nicht leer ist und Datei existiert. */
    private static Drawable decodeGrafikFileSafe(String fileName) {
        if (fileName == null || fileName.trim().isEmpty()) return null;
        String path = TurmtechnikActivity.sdCardPath + "/Turmtechnik/Grafiken/" + fileName.trim();
        File file = new File(path);
        if (!file.exists() || !file.isFile()) return null;
        try {
            return new BitmapDrawable(BitmapFactory.decodeFile(path));
        } catch (Exception e) {
            return null;
        }
    }

    private void loadFarben() {
        int zeile_index = 1;
        Log.e("load", "Farben");
        int max_zeilen = (beschriftungRowsFromDb != null) ? beschriftungRowsFromDb.size() : (farbenExcelread != null ? farbenExcelread.getCellZeilen() : 0);
        if (beschriftungRowsFromDb != null && max_zeilen > 0) max_zeilen++; // 1-based loop
        for (int i = 1; i < max_zeilen; i++) {

            try {
                String cellStringTemp = getFarbenCell(6, i);
                Drawable d = null;
                if (!((cellStringTemp.equals("null"))
                        ||
                        (cellStringTemp.equals("NULL"))
                        ||
                        (cellStringTemp.trim().isEmpty())
                )
                        ) {
                    button_farbe_name = getFarbenCell(6, i); // btn-normal.png
                    Log.e("button_farbe_name 6", "=" + button_farbe_name);
                    d = decodeGrafikFileSafe(button_farbe_name);
                    if (d != null) btn_normal_drawable.add(d);
                }
                // Keine Grafik (c6) in DB/Excel → Farbe aus Vorlage (Tastentyp c1), damit Tasten nicht alle gleich aussehen
                if (d == null && beschriftungRowsFromDb != null && i >= 1 && i <= beschriftungRowsFromDb.size()) {
                    String c1 = getFarbenCell(1, i);
                    btn_normal_drawable.add(getDrawableForTyp(c1));
                }
            } catch (ArrayIndexOutOfBoundsException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                new LogExcelError(6, i, beschriftungTastenFilename, FARBEN_SHEET_NUMBER_NEW, sourceFileName, 1211);
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                new LogExcelError(6, i, beschriftungTastenFilename, FARBEN_SHEET_NUMBER_NEW, sourceFileName, 1215);
            }

            try {
                if (!((getFarbenCell(7, i).equals("null"))
                        ||
                        (getFarbenCell(7, i).equals("NULL"))
                        ||
                        (getFarbenCell(7, i).trim().isEmpty())
                )
                        ) {
                    button_farbe_name = getFarbenCell(7, i);
                    Drawable d = decodeGrafikFileSafe(button_farbe_name);
                    if (d != null) btn_manual_ein_ok_drawable.add(d);
                }
            } catch (ArrayIndexOutOfBoundsException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                new LogExcelError(7, i, beschriftungTastenFilename, FARBEN_SHEET_NUMBER_NEW, sourceFileName, 1232);
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                new LogExcelError(7, i, beschriftungTastenFilename, FARBEN_SHEET_NUMBER_NEW, sourceFileName, 1236);
            }

            try {
                if (!((getFarbenCell(8, i).equals("null"))
                        ||
                        (getFarbenCell(8, i).equals("NULL"))
                        ||
                        (getFarbenCell(8, i).trim().isEmpty())
                )
                        ) {
                    button_farbe_name = getFarbenCell(8, i);
                    Drawable d = decodeGrafikFileSafe(button_farbe_name);
                    if (d != null) btn_manual_ein_error_drawable.add(d);
                }
            } catch (ArrayIndexOutOfBoundsException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                new LogExcelError(8, i, beschriftungTastenFilename, FARBEN_SHEET_NUMBER_NEW, sourceFileName, 1253);
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                new LogExcelError(8, i, beschriftungTastenFilename, FARBEN_SHEET_NUMBER_NEW, sourceFileName, 1257);
            }

            try {
                if (!((getFarbenCell(9, i).equals("null"))
                        ||
                        (getFarbenCell(9, i).equals("NULL"))
                        ||
                        (getFarbenCell(9, i).trim().isEmpty())
                )
                        ) {
                    button_farbe_name = getFarbenCell(9, i);
                    Drawable d = decodeGrafikFileSafe(button_farbe_name);
                    if (d != null) btn_automatic_ein_ok_drawable.add(d);
                }
            } catch (ArrayIndexOutOfBoundsException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                new LogExcelError(9, i, beschriftungTastenFilename, FARBEN_SHEET_NUMBER_NEW, sourceFileName, 1274);
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                new LogExcelError(9, i, beschriftungTastenFilename, FARBEN_SHEET_NUMBER_NEW, sourceFileName, 1278);
            }

            try {
                String c10 = getFarbenCell(10, i);
                if (!((c10.equals("null"))
                        ||
                        (c10.equals("NULL"))
                        ||
                        (c10.trim().isEmpty())
                )
                        ) {
                    button_farbe_name = c10;
                    Drawable d = decodeGrafikFileSafe(button_farbe_name);
                    if (d != null) btn_automatic_ein_error_drawable.add(d);
                }
            } catch (ArrayIndexOutOfBoundsException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                new LogExcelError(10, i, beschriftungTastenFilename, FARBEN_SHEET_NUMBER_NEW, sourceFileName, 1295);
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                new LogExcelError(10, i, beschriftungTastenFilename, FARBEN_SHEET_NUMBER_NEW, sourceFileName, 1299);
            }

        }
        //Log.i("btn_manual_ein_ok" , "=" + btn_manual_ein_ok_drawable.size() );
        //Log.i("btn_manual_ein_error" , "=" + btn_manual_ein_error_drawable.size() );
        //Log.i("btn_automatic_ein_ok" , "=" + btn_automatic_ein_ok_drawable.size() );
        //Log.i("btn_automatic_ein_error" , "=" + btn_automatic_ein_error_drawable.size() );
    }


    private void loadHomeButton() {
        Log.e("btnHome", "loadHomeButton");
        String buttonHomeFileName = "";

        int max_zeilen = (beschriftungRowsFromDb != null) ? beschriftungRowsFromDb.size() : (farbenExcelread != null ? farbenExcelread.getCellZeilen() : 0);
        if (beschriftungRowsFromDb != null && max_zeilen > 0) max_zeilen++;
        for (int i = 1; i < max_zeilen; i++) {
            try {
                //if(farbenExcelread.getCellString(3, i).equals("102")) // eine home Taste gefunden?
                if (getFarbenCell(3, i).equals(Integer.toString(StaticConstants.HOME)))  // eine home Taste gefunden?
                {
                    // wenn eine Home Taste gefunden ist:
                    //Log.i("102" , "gefunden") ;
                    //Log.i("index i" , "=" + i) ;
                    buttonHomeFileName = (getFarbenCell(6, i));
                    //Log.e("btnHome" , "fileName=" + buttonHomeFileName) ;
                    StaticVariable.btn_home_textstring = (getFarbenCell(2, i));
                    //Log.e("btnHome" , "textString" + StaticVariable.btn_home_textstring) ;
                    //Log.i("helpFileName" , "=" + helpFileName) ;
                    break;
                }
            } catch (ArrayIndexOutOfBoundsException e) {
                // TODO Auto-generated catch block
                //Log.e("btnHome" , "catch 1") ;
                new LogExcelError(3, i, beschriftungTastenFilename, FARBEN_SHEET_NUMBER_NEW, sourceFileName, 1331);
            } catch (Exception e) {
                // TODO Auto-generated catch block
                //Log.e("btnHome" , "catch 2") ;
                new LogExcelError(3, i, beschriftungTastenFilename, FARBEN_SHEET_NUMBER_NEW, sourceFileName, 1335);
            }
        }

        //Log.i("BUTTON_FARBE_NAME" , "=" + button_farbe_name) ;
        if (buttonHomeFileName != null && !buttonHomeFileName.isEmpty()) {
            String filePath = TurmtechnikActivity.sdCardPath + "/Turmtechnik/Grafiken/" + buttonHomeFileName;
            File file = new File(filePath);
            if (file.exists() && file.isFile()) {
                StaticVariable.btn_home_drawable = new BitmapDrawable((BitmapFactory.decodeFile(filePath)));
            } else {
                Log.w("btnHome", "Home-Button-Datei nicht gefunden: " + filePath);
            }
        } else {
            Log.w("btnHome", "Home-Button-Dateiname ist leer");
        }
    }

    private void loadHelpButton() {
        String buttonHelpFileName = "";
        int max_zeilen = (beschriftungRowsFromDb != null) ? beschriftungRowsFromDb.size() : (farbenExcelread != null ? farbenExcelread.getCellZeilen() : 0);
        if (beschriftungRowsFromDb != null && max_zeilen > 0) max_zeilen++;
        for (int i = 1; i < max_zeilen; i++) {
            try {
                //if(farbenExcelread.getCellString(3, i).equals("103")) // eine HELP Taste gefunden?
                if (getFarbenCell(3, i).equals(Integer.toString(StaticConstants.HELP)))  // eine home Taste gefunden?
                {
                    // wenn eine Home Taste gefunden ist:
                    //Log.i("102" , "gefunden") ;
                    //Log.i("index i" , "=" + i) ;
                    buttonHelpFileName = (getFarbenCell(6, i));
                    StaticVariable.btn_help_textstring = (getFarbenCell(2, i));
                    //Log.i("helpFileName" , "=" + helpFileName) ;
                    break;
                }
            } catch (ArrayIndexOutOfBoundsException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                new LogExcelError(3, i, beschriftungTastenFilename, FARBEN_SHEET_NUMBER_NEW, sourceFileName, 1364);
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                new LogExcelError(3, i, beschriftungTastenFilename, FARBEN_SHEET_NUMBER_NEW, sourceFileName, 1368);
            }
        }

        //Log.i("BUTTON_FARBE_NAME" , "=" + button_farbe_name) ;
        StaticVariable.btn_help_drawable = new BitmapDrawable((BitmapFactory.decodeFile(TurmtechnikActivity.sdCardPath + "/Turmtechnik/Grafiken/" + buttonHelpFileName)));
    }

    private void loadAutomaticButton() {
        String buttonHelpFileName = "";

        int max_zeilen = (beschriftungRowsFromDb != null) ? beschriftungRowsFromDb.size() : (farbenExcelread != null ? farbenExcelread.getCellZeilen() : 0);
        if (beschriftungRowsFromDb != null && max_zeilen > 0) max_zeilen++;
        for (int i = 1; i < max_zeilen; i++) {
            try {
                if (getFarbenCell(3, i).equals(Integer.toString(StaticConstants.AUTOMATIC))) // eine AUTOMATIC Taste gefunden?
                {
                    // wenn eine Home Taste gefunden ist:
                    //Log.i("102" , "gefunden") ;
                    //Log.i("index i" , "=" + i) ;
                    buttonHelpFileName = (getFarbenCell(6, i));
                    StaticVariable.btn_automatic_textstring = (getFarbenCell(2, i));
                    //Log.i("helpFileName" , "=" + helpFileName) ;
                    break;
                }
            } catch (ArrayIndexOutOfBoundsException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                new LogExcelError(3, i, beschriftungTastenFilename, FARBEN_SHEET_NUMBER_NEW, sourceFileName, 1397);
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                new LogExcelError(3, i, beschriftungTastenFilename, FARBEN_SHEET_NUMBER_NEW, sourceFileName, 1401);
            }
        }

        //Log.i("BUTTON_FARBE_NAME" , "=" + button_farbe_name) ;
        StaticVariable.btn_automatic_drawable = new BitmapDrawable((BitmapFactory.decodeFile(TurmtechnikActivity.sdCardPath + "/Turmtechnik/Grafiken/" + buttonHelpFileName)));
    }

    private void loadStopButton() {
        String buttonStopFileName = "";
        int max_zeilen = (beschriftungRowsFromDb != null) ? beschriftungRowsFromDb.size() : (farbenExcelread != null ? farbenExcelread.getCellZeilen() : 0);
        if (beschriftungRowsFromDb != null && max_zeilen > 0) max_zeilen++;
        for (int i = 1; i < max_zeilen; i++) {
            try {
                //if(farbenExcelread.getCellString(3, i).equals("100")) // eine STOP Taste gefunden?
                if (getFarbenCell(3, i).equals(Integer.toString(StaticConstants.STOP))) // eine STOP Taste gefunden?
                {
                    // wenn eine Stop Taste gefunden ist:
                    //Log.i("100" , "gefunden") ;
                    //Log.i("index i" , "=" + i) ;
                    buttonStopFileName = (getFarbenCell(6, i));
                    StaticVariable.btn_stop_textstring = (getFarbenCell(2, i));
                    //Log.i("BTN HELP STRING" , "=" + StaticVariable.btn_stop_textstring) ;
                    //Log.i("helpFileName" , "=" + helpFileName) ;
                    break;
                }
            } catch (ArrayIndexOutOfBoundsException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                new LogExcelError(3, i, beschriftungTastenFilename, FARBEN_SHEET_NUMBER_NEW, sourceFileName, 1431);
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                new LogExcelError(3, i, beschriftungTastenFilename, FARBEN_SHEET_NUMBER_NEW, sourceFileName, 1435);
            }
        }

        //Log.i("BUTTON_FARBE_NAME" , "=" + button_farbe_name) ;
        StaticVariable.btn_stop_drawable = new BitmapDrawable((BitmapFactory.decodeFile(TurmtechnikActivity.sdCardPath + "/Turmtechnik/Grafiken/" + buttonStopFileName)));
    }

//	   public void closeWorkbook()
//	   {
//		   excelread.closeWorkbook();
//	   }
//	   

} // ende der Klasse
