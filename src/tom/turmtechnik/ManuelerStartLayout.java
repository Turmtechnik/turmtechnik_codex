package tom.turmtechnik;

import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsoluteLayout;
import android.widget.AbsoluteLayout.LayoutParams;
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


//import static tom.turmtechnik.R.drawable.balken_pfeil_nach_oben;

//import android.util.Log;

public class ManuelerStartLayout {
    private final String sourceFileName = "ManualStartLayout";

    private int seite2MarginTop = 0;

    private int spalte4;
    private int spalte3;
    private int spalte2;
    private int spalte1;
    private int startBlockUnten;

    private int viewWidth;
    private int viewHeight;

    //private float rasterWidth ;
    //private float rasterHeight ;

    private float fredPixelHeight;
    private float fredPixelWidth;

    public Button zureckButton;
    public Button stopButton;
    public Button einAusButton;
    public Button minusButton;
    public Button plusButton;
    public Button timeButton;
    public Button pfeilNachObenButton;
    public Button pfeilNachUntenButton;

    public TextView[] beuntzerTextViewArray = new TextView[7];

    public TextView infoText;
    public static Vector<Button> buttons; // = new Vector<Button>(); // die buttons auf Seite2

    private final int BESCHRIFTUNG_GLOCKEN_SHEET = 0;
    //	private final int FARBEN_SHEET_NUMBER = 16 ;
    private final int FARBEN_SHEET_NUMBER = 0; // die Tastenhintergrund Grafik namen auf Glocken sheet gegeben
    // 11.6.13

    private final int ANFANG_SEITE2_INDEX = 25;

    //public int startStunde = 12;
    //public int startMinute = 0 ;


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
//	private Vector<Integer> relaisnumber = new Vector<Integer>();
//	private Vector<Integer> hammerzeit = new Vector<Integer>();

    private static TextView datum;

    private Context context;
    private static AbsoluteLayout absolutelayoutFeldOben;
    private static AbsoluteLayout parentAbsoluteLayout;
    //private static FrameLayout frameLayout ;
    private static View manualStartView;
    //private DigitalClock digitalClock ;

    //private int displayWidth ;

    private static TextView uhr;

    //private int displayHeight ;

    private float buchstabenGroesseUhr;
    private float buchstabenGroesseWochentag;
    private float buchstabenGroesseInfoText;
    private float buchstabenGroesseTastenText;
    private int[] buchstabenXY;

    private int buchstabenGroesse;


    private float scale;

    private int heightViewOben = ViewGroup.LayoutParams.WRAP_CONTENT;
    private int widthViewOben = ViewGroup.LayoutParams.FILL_PARENT;
    //	private Button glockenButton ;
    private int glockenCount;
    private int kloepelFaengerCount;
    private int gruppeDreiCount; // ab 26.3.2013 eine dritte zeile mit Tasten
    private String inputFilename;

    private Button buttonTemp;

    private int index;
    private static ExcelRead excelread;
    private static ExcelRead farbenExcelread;
    private String button_farbe_name;


    public ManuelerStartLayout(String fileName, Context context) {
        this.context = context;
        inputFilename = fileName;
        buttons = new Vector<Button>(); // die buttons auf Seite2

        // die Button Hintergruende von SD Card laden:
        farbenExcelread = new ExcelRead();
        try {
            farbenExcelread.openXlsSheet(inputFilename, FARBEN_SHEET_NUMBER);
        } catch (BiffException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            new LogExcelError(-1, -1, inputFilename, FARBEN_SHEET_NUMBER, sourceFileName, 115);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
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
        //loadFarben() ;

        farbenExcelread.closeWorkbook();


        excelread = new ExcelRead();
        try {
            excelread.openXlsSheet(inputFilename, BESCHRIFTUNG_GLOCKEN_SHEET);
        } catch (BiffException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            new LogExcelError(-1, -1, inputFilename, BESCHRIFTUNG_GLOCKEN_SHEET, sourceFileName, 154);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            new LogExcelError(-1, -1, inputFilename, BESCHRIFTUNG_GLOCKEN_SHEET, sourceFileName, 158);
        }
        //loadOnlyButtonText();
        //readVerknuepfteTastenTexts();

		/*
        glockenCount = getGlockenCount() ;
	    kloepelFaengerCount = getKloeppelCount() ;
		gruppeDreiCount = getGruppeDreiCount() ;
		*/

        //absolutelayout = new AbsoluteLayout(context);
        //LayoutParams altable = new LayoutParams(
        //		ViewGroup.LayoutParams.FILL_PARENT,
        //		ViewGroup.LayoutParams.FILL_PARENT, 0, 0 ) ;


        //absolutelayout.setLayoutParams(altable);


        LayoutInflater layoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        manualStartView = layoutInflater.inflate(R.layout.manueler_start, null);

        absolutelayoutFeldOben = (AbsoluteLayout) manualStartView.findViewById(R.id.feldOben);
        parentAbsoluteLayout = (AbsoluteLayout) manualStartView.findViewById(R.id.manueler_start_layout);

        viewWidth = TurmtechnikActivity.usableWith;
        viewHeight = TurmtechnikActivity.usableHeight;

        fredPixelWidth = viewWidth / 800f;
        fredPixelHeight = viewHeight / 480f;

        //dpHeight = MainActivity.dpHeight ;
        //dpWidth = MainActivity.dpWidth ;

        //displayWidth = MainActivity.getDisplayWith();
        //displayHeight = MainActivity.getDisplayHeight();

        //seite2MarginTop = displayHeight / 20 ;
        seite2MarginTop = viewHeight / 20;

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

        buchstabenXY = StaticConstants.leseBuchstabenGroesse(); // das Feld mit xy koordinaten einlesen

        // buchstabenGroesse = StaticConstants.getBuchstabenGroesse(displayHeight);
        buchstabenGroesseUhr = (buchstabenXY[0] / metrics.density + 0.5f);
        buchstabenGroesseWochentag = (buchstabenXY[1] / metrics.density + 0.5f);
        buchstabenGroesseInfoText = (buchstabenXY[2] / metrics.density + 0.5f);
        buchstabenGroesseTastenText = ((buchstabenXY[12] / metrics.density + 0.5f));

    } // ende Konstruktor Seite1Layout

    public static int getSeite2ButtonsSize() {
        return buttons.size();
    }


    public void initLogo() {
        //int logo_x = displayWidth / 70 ;
        //int logo_y = displayHeight / 40  ;
        //int logo_with = ( displayWidth * 382 ) / 1300 ;
        int logo_x = (viewWidth / 40) + buchstabenXY[9];
        int logo_y = (viewHeight / 40) + buchstabenXY[10] + seite2MarginTop;
//		  int logo_with = ( displayWidth * 50 ) / 300 ;
        int logo_with = ((viewWidth * 50) / 300) + buchstabenXY[11];


//		  int logo_height = ( displayHeight * 114 )  / 400  ;


        ImageView logoView = new ImageView(this.context);

        logoView.setImageResource(R.drawable.kirchturmtechnik_logo4);

        LayoutParams alayoutParams =
                //	  new AbsoluteLayout.LayoutParams(LayoutParams.WRAP_CONTENT,LayoutParams.WRAP_CONTENT, logo_x, logo_y);
                new LayoutParams(logo_with, logo_with, logo_x, logo_y);
        logoView.setLayoutParams(alayoutParams);

        this.absolutelayoutFeldOben.addView(logoView);

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
        int clock_width = viewWidth / 2;
        int clock_height = viewHeight / 12;
        //  int clock_width = 200;
        int clock_x = (viewWidth / 2) - (clock_width / 2) + buchstabenXY[3];
        int clock_y = seite2MarginTop + buchstabenXY[4];
        ///Log.d("displayW", " = " + displayWidth);
        //digitalClock = new DigitalClock(this.context);
        uhr = new TextView(this.context);


        LayoutParams alayoutParams =
                new LayoutParams(clock_width, heightViewOben, clock_x, clock_y);
        //digitalClock.setLayoutParams(alayoutParams);
        uhr.setLayoutParams(alayoutParams);
        //   digitalClock.setTextSize(TypedValue.COMPLEX_UNIT_SP,buchstabenGroesseUhr);
        //digitalClock.setTextSize( buchstabenGroesseUhr);
        uhr.setTextSize(buchstabenGroesseUhr);
        uhr.setTextColor(context.getResources().getColor(R.color.black));

//	      digitalClock.setMaxHeight(64);
//	      digitalClock.setMaxWidth(60);
        //digitalClock.setTextColor(this.context.getResources().getColor(R.color.black));
        //uhr.setTextColor(this.context.getResources().getColor(R.color.black));
//	      digitalClock.setBackgroundColor(this.context.getResources().getColor(R.color.red));
        //digitalClock.setGravity(Gravity.CENTER_HORIZONTAL);
        uhr.setGravity(Gravity.CENTER_HORIZONTAL);

        //this.absolutelayout.addView(digitalClock);
        this.absolutelayoutFeldOben.addView(uhr);
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
        String[] weekDayNames = {"SONNTAG", "MONTAG", "DIENSTAG", "MITTWOCH",
                "DONNERSTAG", "FREITAG", "SAMSTAG"};
        day = date.get(Calendar.DAY_OF_WEEK);
        return weekDayNames[day - 1];
    }


    public void initDatum() {
// 		   int datum_width = displayWidth /2 ;
//		   int datum_x = ( displayWidth /2 )  - (datum_width /2  ) ;
//		   int datum_y = displayHeight / 10 ;


        //int datum_x = ( displayWidth * 8 ) / 12 ;
        int datum_width = viewWidth / 3;
        int datum_x = ((viewWidth * 9) / 13) + buchstabenXY[5];
        int datum_y = (viewHeight / 50) + buchstabenXY[6] + seite2MarginTop;


//		   Log.i("displayH", " = " + displayHeight);

        datum = new TextView(this.context);

        LayoutParams alayoutParams =
                new LayoutParams(datum_width, heightViewOben, datum_x, datum_y);
        datum.setLayoutParams(alayoutParams);

        // datum.setTextColor(this.context.getResources().getColor(R.color.black));
//		   datum.setBackgroundColor(this.context.getResources().getColor(R.color.green));

//		   datum.setTextSize(TypedValue.COMPLEX_UNIT_SP,( buchstabenGroesse  ));
        datum.setTextSize(buchstabenGroesseWochentag);
        datum.setTextColor(context.getResources().getColor(R.color.black));
        datum.setGravity(Gravity.CENTER_HORIZONTAL);
        datum.setText(getWochentag() + "\n");

        //	   datum.setTextSize(TypedValue.COMPLEX_UNIT_DIP,( buchstabenGroesse * 0.7f ));
        datum.append(getDatum());
        //   datum.setGravity(Gravity.CENTER_HORIZONTAL);

        this.absolutelayoutFeldOben.addView(datum);
    }

    public void initInfoText() {
        int infotext_width = viewWidth;
        int infotext_height = viewHeight / 10;
        int infotext_x = 0;
        //	 int infotext_y =  displayHeight - ( (displayHeight / 4) ) + ( displayHeight / 700 ) ;

        int infotext_y = (viewHeight / 4 - (viewHeight / 20) + buchstabenXY[8] + seite2MarginTop);

        infoText = new TextView(context);

        //stopButton.setBackgroundDrawable(context.getResources().getDrawable(R.drawable.stop_180x179_transparent));
        LayoutParams alayoutParams =
                new LayoutParams(infotext_width, infotext_height, infotext_x, infotext_y);
        infoText.setLayoutParams(alayoutParams);

        //infoText.setTextColor(this.context.getResources().getColor(R.color.black));

        infoText.setTextSize(buchstabenGroesseInfoText);
        infoText.setTextColor(context.getResources().getColor(R.color.black));
        infoText.setGravity(Gravity.CENTER);
        infoText.setMaxLines(2);
        infoText.setSingleLine(false);
        infoText.setText("EINSTELLUNGEN SEITE2");
        //   automaticButton.setText("AUTOMATIC = EIN");
        this.absolutelayoutFeldOben.addView(infoText);
    }

    public void infoTextBlack() {
        //infoText.setTextColor(this.context.getResources().getColor(R.color.black));
    }

    public void infoTextRed() {
        //infoText.setTextColor(this.context.getResources().getColor(R.color.red));
    }

    public void infoTextGreen() {
        //infoText.setTextColor(this.context.getResources().getColor(R.color.green));
    }


	/*
	   int glocken_button_width ;
	   int glocken_button_height ;
	   int glocken_button_x  ;
	   int glocken_button_y ;


	   public void initGlockenButton(int width, int height, int x, int y, int index)
	   {
		   Button glockenButton = new Button(context);
		   buttons.add(glockenButton);

		   index = buttons.size()-1 ;  // 26.3.13 wegen platzhalter Taste "leer"

		//   buttons.elementAt(index).setTextSize(TypedValue.COMPLEX_UNIT_DIP,( buchstabenGroesse * 0.5f));
		//     buttons.elementAt(index).setTextSize(buchstabenGroesse );

		     //buttons.elementAt(index).setTextSize( width / 10);            //(width/9);
		   	 buttons.elementAt(index).setTextSize(buchstabenGroesseTastenText);

		     //   glockenButton.setTextSize(TypedValue.COMPLEX_UNIT_DIP,( buchstabenGroesse ));
		//   glockenButton.setBackgroundDrawable(this.context.getResources().getDrawable(R.drawable.btn_21));


		   LayoutParams alayoutParams =
		    	//	  new AbsoluteLayout.LayoutParams(button_width, button_height, button_x , button_y);
		              new LayoutParams(width, height, x, y);
				   buttons.elementAt(index).setLayoutParams(alayoutParams);
		 
		   buttons.elementAt(index).setTextColor(this.context.getResources().getColor(R.color.white));
		
		   Log.e("index" , "=" + index) ;
		   Log.e("counter relais seite1" , "=" + TurmtechnikActivity.BESCHRIFTUNG_TASTEN_SLOTS_PAGE1) ;
		   
		  
		   int relaisNumber = TurmtechnikActivity.relaisNumber[index+TurmtechnikActivity.BESCHRIFTUNG_TASTEN_SLOTS_PAGE1] ;
		   
		   Log.e("Seite2Layout" , "relaisNumber=" + relaisNumber) ;
		   
		   if( (relaisNumber < 100 ) && (relaisNumber >0) ) // ist es eine normale Taste?
		   {	   
			   if(TurmtechnikActivity.globalOn[index] == true)
			   {
				   setButtonOnOk(index);
			   }
			   else
			   {
				   setButtonNormal(index);
			   }
			   
		   }
		   else
		   {
			   if(relaisNumber > 2000) // eine verknuepfte Taste?
			   {
				   int vIdx = index + Seite1Layout.get_counted_verknuepft_seite1();
				   if (vIdx >= 0 && vIdx < TurmtechnikActivity.verknuepfteTastenOn.length && Boolean.TRUE.equals(TurmtechnikActivity.verknuepfteTastenOn[vIdx]))
				   {
					   setButtonOnOk(index);
				   }
				   else
				   {
					   setButtonNormal(index);
				   }
			   }
			   else
			   {
				   setButtonNormal(index);
			   }
		   }
		   
		   
		   String buttontext = getButtonText(index);
		   
		   buttons.elementAt(index).setText(buttontext);
		   
		   buttons.elementAt(index).setGravity(Gravity.CENTER);
		   this.absolutelayout.addView(buttons.elementAt(index));
		   
	   } */

	   /*
	   public void berechne_button_width()
	   {
		   if (glockenCount < 3)
		   {
			   	glocken_button_width=displayWidth / 3 ;
		   }
		   else if (glockenCount < 5)
		   		{ 
			   		glocken_button_width=displayWidth / 4 ;
		   		}
		        else 
		        {
		        	glocken_button_width=displayWidth / 6 ;
		        }
		  // StaticVariable.tastenBreite = glocken_button_width ;
	   } */


    /*
       public void makeXglockenButtons()
       {

           berechne_glocken_button_x(glockenCountOhneNULL);
           glocken_button_height = displayHeight / 5 ;
           // StaticVariable.tastenHoehe = glocken_button_height ;

           glocken_button_y = (( displayHeight / 14) * 5)  - (displayHeight / 20) ;

           int ix = 0 ;

           //for (int i = 0; i < glockenCount;i++)
           for (int i = 0; i < 8 ; i++)
           {
             Log.e("glockenCount" , "=" + i) ;
             int leerTasteTemp = leerTaste(i + ANFANG_SEITE2_INDEX) ;
             if( leerTasteTemp == 0 )
             {
                   initGlockenButton(glocken_button_width, glocken_button_height, buttonX[ix], glocken_button_y, i);
                   ix ++ ;
             }
             if( leerTasteTemp == 1 ) // "leer Taste ist platzhalter"
             {
                 ix++ ;
             }
             // bei "NULL" ix nicht erhoehen
           }

           berechne_glocken_button_x(kloepelFaengerCountOhneNULL);
           glocken_button_y = (( displayHeight / 9 ) * 5)  - (displayHeight / 20) ;
           //Log.i("vor kloepel Schleife" , "count=" + kloepelFaengerCount ) ;
           ix = 0 ;
           //for (int i = 0; i < kloepelFaengerCount;i++)
           for (int i = 0; i < 8 ; i++ )
           {
               Log.e("kloeppelFaengerCount" , "=" + i) ;
               int leerTasteTemp = leerTaste(i + 8 + ANFANG_SEITE2_INDEX) ;
               //if( leerTaste(i + 8 + ANFANG_SEITE2_INDEX) == false)
              if (leerTasteTemp == 0 )
              {
                   initGlockenButton(glocken_button_width, glocken_button_height, buttonX[ix], glocken_button_y, i+glockenCount);
                   ix ++ ;
              }
              if( leerTasteTemp == 1 ) // "leer Taste ist platzhalter"
              {
                 ix++ ;
              }
                 // bei "NULL" ix nicht erhoehen
           }
           if (true) // (gruppeDreiCount > 0 )
           {
               berechne_glocken_button_x(gruppeDreiCountOhneNULL);
               glocken_button_y = (( displayHeight * 30 ) / 40 )  - (displayHeight / 20) ;

               ix = 0 ;
               //for (int i = 0 ; i < gruppeDreiCount; i++ )
               for (int i = 0 ; i < 8 ; i ++ )
               {
                   Log.e("gruppeDreiCount" , "=" + i) ;
                   int leerTasteTemp = leerTaste(i + 8 + 8 + ANFANG_SEITE2_INDEX) ;
                   if ( leerTasteTemp == 0 )
                   {
                       initGlockenButton(glocken_button_width, glocken_button_height, buttonX[ix], glocken_button_y, i+glockenCount);
                       ix ++ ;
                   }
                   if( leerTasteTemp == 1 ) // "leer Taste ist platzhalter"
                    {
                       ix++ ;
                    }
                      // bei "NULL" ix nicht erhoehen
               }
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
        buttons.elementAt(index).setBackgroundDrawable(btn_manual_ein_ok_drawable.get(index));
    }

    public void setButtonOnAutomatic(int index) {
        // 10.3.13 geaendert auf Bitmap sdCard
        // buttons.elementAt(index).setBackgroundDrawable(context.getResources().getDrawable(R.drawable.btn_red));
//		   int tabellen_zeile = searchButtonColor(9, index);
//		   button_farbe_name = farbenExcelread.getCellString(9, tabellen_zeile);
//		   btn_automatic_ein_ok_drawable = new BitmapDrawable((BitmapFactory.decodeFile(TurmtechnikActivity.sdCardPath + "/Turmtechnik/Grafiken/" + button_farbe_name )));
        buttons.elementAt(index).setBackgroundDrawable(btn_automatic_ein_ok_drawable.get(index));
    }

    public void setButtonNormal(int index) {
        //buttons.elementAt(index).setBackgroundDrawable(context.getResources().getDrawable(R.drawable.btn_gray));
        // 10.3.13 geaender auf Bitmap sdCard

//		   int tabellen_zeile = searchButtonColor(6, index);
//		   button_farbe_name = farbenExcelread.getCellString(6, tabellen_zeile);
//		   btn_normal_drawable = new BitmapDrawable((BitmapFactory.decodeFile(TurmtechnikActivity.sdCardPath + "/Turmtechnik/Grafiken/" + button_farbe_name )));
        buttons.elementAt(index).setBackgroundDrawable(btn_normal_drawable.get(index));
    }

    /*
	   public void buttonOff(int localIndex)
	   {
		   setButtonNormal(localIndex);
		//   String buttonText = getButtonText(checkIndexTemp(localIndex));
		   String buttonText = getButtonText(localIndex);
		   buttons.elementAt(localIndex).setText(buttonText);
	   }
	   
	   public void buttonOnOK(int localIndex)
	   {
		   //setButtonRed(localIndex);
		   setButtonOnOk(localIndex);
		//   String buttonText = getButtonText(checkIndexTemp(localIndex));
		   String buttonText = getButtonText(localIndex);
		   buttons.elementAt(localIndex).setText(buttonText);
	   }
	   
	   public void buttonOnRed(int localIndex)
	   {
		   setButtonOnAutomatic(localIndex);
		   String buttonText = getButtonText(localIndex);
		   buttons.elementAt(localIndex).setText(buttonText);
	   }
	*/
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


    private void initSpaltenUndZeilen() {

        spalte4 = (int) (fredPixelWidth * 660.0f);
        spalte3 = (int) (fredPixelWidth * 490.0f);
        spalte2 = (int) (fredPixelWidth * 82.0f);                //(displayWidth * 100) / 880 ;
        spalte1 = (int) (fredPixelWidth * 6.0f);

        startBlockUnten = viewHeight - (int) (fredPixelHeight * 310.0f); //displayHeight * 100 ) /  320 ;

        int y = viewHeight - (int) (fredPixelHeight * 310.0f);

    }


    private void initZurueckButton() {
        initSpaltenUndZeilen();

        Log.e("view", "width=" + viewWidth);
        Log.e("view", "heigth=" + viewHeight);
        Log.e("view", "fredPixelWidth=" + fredPixelWidth);
        Log.e("view", "fredPixelHeight=" + fredPixelHeight);

        int width = (int) (fredPixelWidth * 134.0f);
        int height = (int) (fredPixelHeight * 70.0f);
        int x = spalte4;
        //int y = (int) (fredPixelHeight * 350.0f) ;
        int y = viewHeight - height - (int) (fredPixelHeight * 2);
        Log.e("view", "y=" + y);

        float textSize = getTextSizeZurueck();

        zureckButton = new Button(context);

        AbsoluteLayout.LayoutParams aboluteParams =
                new AbsoluteLayout.LayoutParams(width, height, x, y);
        zureckButton.setLayoutParams(aboluteParams);

        zureckButton.setBackgroundColor(context.getResources().getColor(R.color.blue));
        zureckButton.setText("Zurück");
        //zureckButton.setTextSize(TypedValue.COMPLEX_UNIT_SP, 60);
        zureckButton.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize);
        zureckButton.setGravity(Gravity.CENTER_HORIZONTAL | Gravity.CENTER_VERTICAL);

        this.parentAbsoluteLayout.addView(zureckButton);
    }


    private void initStopButton() {
        //initSpaltenUndZeilen() ;

        int width = (int) (fredPixelWidth * 134.0f);
        int height = (int) (fredPixelHeight * 70.0f);
        int x = spalte4;
        int y = viewHeight - (int) (fredPixelHeight * 310.0f);
        float textSize = getTextSizeZurueck();

        stopButton = new Button(context);

        AbsoluteLayout.LayoutParams aboluteParams =
                new AbsoluteLayout.LayoutParams(width, height, x, y);
        stopButton.setLayoutParams(aboluteParams);

        stopButton.setBackgroundColor(context.getResources().getColor(R.color.red));
        stopButton.setText("Stop");
        //zureckButton.setTextSize(COMPLEX_UNIT_SP, 60);
        stopButton.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize);
        stopButton.setGravity(Gravity.CENTER_HORIZONTAL | Gravity.CENTER_VERTICAL);

        this.parentAbsoluteLayout.addView(stopButton);
    }

    private float getTextSizeZurueck() {
        float textSizeTemp;

        textSizeTemp = (fredPixelHeight * 34.0f);

        return textSizeTemp;

        //float multiplikator ;

        //if(fredPixelWidth > 1 )
        //{
        //	multiplikator = 36.0f ;
        //}
        //else
        //{
        //	multiplikator = 19.0f ;
        //}

        //return (int) (fredPixelWidth * multiplikator ) ;
    }

    private float[] getTextSizeBenutzer() {
        float returnWert[];
        //if(fredPixelWidth > 1 )
        //{
        returnWert = new float[]{10, 20, 30, 40, 30, 20, 10};
        //}
        //else
        //{
        //    returnInt = new int[] {10, 20, 30, 40, 30, 20, 10};
        //}
        return returnWert;
    }

    private void initEinAusButton() {
        //initSpaltenUndZeilen() ;

        int width = (int) (fredPixelWidth * 134.0f);
        int height = (int) (fredPixelHeight * 150.0f);
        int x = spalte4;
        int y = viewHeight - (int) (fredPixelHeight * 230.0f);
        float textSize = getTextSizeZurueck();

        einAusButton = new Button(context);

        //einAusButton.setBackgroundColor(context.getResources().getColor(R.color.yellow));
        setEinAusGrau();
        //einAusButton.setText("AUS");
        einAusButton.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize);
        einAusButton.setTextColor(context.getResources().getColor(R.color.black));
        einAusButton.setGravity(Gravity.CENTER_HORIZONTAL | Gravity.CENTER_VERTICAL);

        AbsoluteLayout.LayoutParams flayoutParams =
                new AbsoluteLayout.LayoutParams(width, height, x, y);
        einAusButton.setLayoutParams(flayoutParams);


        this.parentAbsoluteLayout.addView(einAusButton);
    }

    public void setEinAusGelb() {
        einAusButton.setBackgroundColor(context.getResources().getColor(R.color.yellow));
    }

    public void setEinAusGrau() {
        einAusButton.setBackgroundColor(context.getResources().getColor(R.color.light_gray));
    }


    private void initMinusButton() {
        //initSpaltenUndZeilen() ;

        int width = (int) (fredPixelWidth * 165.0f);
        int height = (int) (fredPixelHeight * 70.0f);
        int x = spalte3;
        int y = viewHeight - (int) (fredPixelHeight * 310.0f);
        float textSize = getTextSizeZurueck();


        minusButton = new Button(context);

        minusButton.setBackgroundColor(context.getResources().getColor(R.color.light_gray));
        minusButton.setText("-");
        minusButton.setTextColor(context.getResources().getColor(R.color.green));
        minusButton.setTypeface(null, Typeface.BOLD);
        minusButton.setTextSize(TypedValue.COMPLEX_UNIT_PX, (textSize * 2));
        //minusButton.setGravity(Gravity.CENTER_HORIZONTAL | Gravity.CENTER_VERTICAL);
        minusButton.setGravity(Gravity.CENTER_HORIZONTAL);

        //int topPadding = ((height * 100) / 440) ;
        int topPadding = (int) (textSize / 2);
        topPadding = -topPadding;
        minusButton.setPadding(0, topPadding, 0, 0);

        AbsoluteLayout.LayoutParams flayoutParams =
                new AbsoluteLayout.LayoutParams(width, height, x, y);
        minusButton.setLayoutParams(flayoutParams);

        this.parentAbsoluteLayout.addView(minusButton);
    }


    private void initPlusButton() {
        //initSpaltenUndZeilen() ;

        int width = (int) (fredPixelWidth * 165.0f);
        int height = (int) (fredPixelHeight * 70.0f);
        int x = spalte3;
        int y = viewHeight - height - (int) (fredPixelHeight * 2);
        float textSize = getTextSizeZurueck();


        plusButton = new Button(context);

        plusButton.setBackgroundColor(context.getResources().getColor(R.color.light_gray));
        plusButton.setText("+");
        plusButton.setTextColor(context.getResources().getColor(R.color.green));
        plusButton.setTypeface(null, Typeface.BOLD);
        plusButton.setTextSize(TypedValue.COMPLEX_UNIT_PX, (textSize * 2));
        //minusButton.setGravity(Gravity.CENTER_HORIZONTAL | Gravity.CENTER_VERTICAL);
        plusButton.setGravity(Gravity.CENTER_HORIZONTAL);

        //int topPadding = ((height * 100) / 440) ;
        int topPadding = (int) (textSize / 2);
        topPadding = -topPadding;
        plusButton.setPadding(0, topPadding, 0, 0);

        AbsoluteLayout.LayoutParams flayoutParams =
                new AbsoluteLayout.LayoutParams(width, height, x, y);
        plusButton.setLayoutParams(flayoutParams);


        this.parentAbsoluteLayout.addView(plusButton);
    }


    private void initTimeButton() {
        //initSpaltenUndZeilen() ;
        int width = (int) (fredPixelWidth * 165.0f);
        int height = (int) (fredPixelHeight * 150.0f);
        int x = spalte3;
        int y = viewHeight - (int) (fredPixelHeight * 230.0f);
        float textSize = getTextSizeZurueck();


        timeButton = new Button(context);

        timeButton.setBackgroundColor(context.getResources().getColor(R.color.light_gray));
        //timeButton.setText("Um 12:00");
        timeButton.setTypeface(null, Typeface.BOLD);
        timeButton.setTextColor(context.getResources().getColor(R.color.green));
        timeButton.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize);

        timeButton.setGravity(Gravity.CENTER_HORIZONTAL | Gravity.CENTER_VERTICAL);

        AbsoluteLayout.LayoutParams flayoutParams =
                new AbsoluteLayout.LayoutParams(width, height, x, y);
        timeButton.setLayoutParams(flayoutParams);


        this.parentAbsoluteLayout.addView(timeButton);
    }


    private void initPfeilNachObenButton() {
        //initSpaltenUndZeilen() ;
        int width = (int) (fredPixelWidth * 70);
        int height = (int) (fredPixelHeight * 150);
        int x = spalte1;
        int y = viewHeight - (int) (fredPixelHeight * 310.0f);

        pfeilNachObenButton = new Button(context);

        pfeilNachObenButton.setBackgroundResource(R.drawable.balken_pfeil_nach_oben);

        AbsoluteLayout.LayoutParams flayoutParams =
                new AbsoluteLayout.LayoutParams(width, height, x, y);
        pfeilNachObenButton.setLayoutParams(flayoutParams);

        this.parentAbsoluteLayout.addView(pfeilNachObenButton);

    }


    private void initPfeilNachUntenButton() {
        //initSpaltenUndZeilen() ;

        int width = (int) (fredPixelWidth * 70);
        int height = (int) (fredPixelHeight * 150);
        int x = spalte1;
        int y = viewHeight - (int) (fredPixelHeight * 152f);

        pfeilNachUntenButton = new Button(context);

        pfeilNachUntenButton.setBackgroundResource(R.drawable.balken_pfeil_nach_unten);

        AbsoluteLayout.LayoutParams flayoutParams =
                new AbsoluteLayout.LayoutParams(width, height, x, y);
        pfeilNachUntenButton.setLayoutParams(flayoutParams);

        this.parentAbsoluteLayout.addView(pfeilNachUntenButton);


    }


    private TextView makeTextViewBenutzerprogramm(int width, int height, int x, int y, String text, float textSize, int textColor) {
        TextView textView = new TextView(context);
        textView.setBackgroundColor(context.getResources().getColor(R.color.green));
        textView.setText(text);
        textView.setTextColor(textColor);
        //textView.setTextSize(TypedValue.COMPLEX_UNIT_SP , textSize);

        textView.setTextSize(TypedValue.COMPLEX_UNIT_PX, (textSize * fredPixelHeight));
        textView.setGravity(Gravity.CENTER_HORIZONTAL | Gravity.CENTER_VERTICAL);

        AbsoluteLayout.LayoutParams flayoutParams =
                new AbsoluteLayout.LayoutParams(width, height, x, y);
        textView.setLayoutParams(flayoutParams);


        return textView;

    }

    private void initBenutzerTexte() {
        int textBlack = context.getResources().getColor(R.color.black);
        int textRed = context.getResources().getColor(R.color.red);
        int breite = (int) (fredPixelWidth * 402); //dp2px(640) ; //(displayWidth * 100) / 202 ;
        int hoehe = (int) (fredPixelHeight * 40); //dp2px(58) ; //(displayHeight * 100) / 1275 ;
        int offsetY = hoehe;
        int startY = startBlockUnten;
        int mitteOffset = (hoehe * 2) - (int) (fredPixelHeight * 10);

        int[] hoeheY = {hoehe, hoehe, hoehe, mitteOffset, hoehe, hoehe, hoehe};
        int[] textColorY = {textBlack, textBlack, textBlack, textRed, textBlack, textBlack, textBlack};
        float[] textSizeY = getTextSizeBenutzer();

        for (int i = 0; i < 7; i++) {
            beuntzerTextViewArray[i] = makeTextViewBenutzerprogramm(breite, hoeheY[i], spalte2, startY, "Benutzerprogramm " + (i + 1), textSizeY[i], textColorY[i]);
            this.parentAbsoluteLayout.addView(beuntzerTextViewArray[i]);

            if (i == 3) {
                startY += mitteOffset;
            } else {
                startY += offsetY;
            }
        }

    }

    //public AbsoluteLayout initLayout()
    public View initLayout() {
        initLogo();
        initClock();
        initDatum();
        initInfoText();

        initZurueckButton();
        initStopButton();
        initEinAusButton();
        initMinusButton();
        initPlusButton();
        initTimeButton();
        initPfeilNachObenButton();
        initPfeilNachUntenButton();

        initBenutzerTexte();


        printEinAus();
        excelread.closeWorkbook();                // test wegen out of memory
        excelread = null;
        System.gc();

        // return this.absolutelayout;
        return this.manualStartView;
    }

		/*
	   private int getGlockenCount()
	   {
		   int glockencount = 0 ;
		   glockenCountOhneNULL = 0 ;
		   
		   int max_zeilen = excelread.getCellZeilen() ;
		   max_zeilen = Math.min(max_zeilen, (ANFANG_SEITE2_INDEX+8) ) ;
		   for (int i=ANFANG_SEITE2_INDEX; i < max_zeilen; i++)
		   {
			   String strtemp = "";
			try {
				strtemp = excelread.getCellString(2, i);
			} catch (ArrayIndexOutOfBoundsException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				new LogExcelError(2, i, inputFilename, BESCHRIFTUNG_GLOCKEN_SHEET,sourceFileName,700) ;
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				new LogExcelError(2, i, inputFilename, BESCHRIFTUNG_GLOCKEN_SHEET,sourceFileName,704) ;
			}
			 if ( ! ( (strtemp.equals("null"))))  // || (strtemp.equals("NULL")) ) )
			   {
				   glockencount++;
			   }
			   if ( ! ( (strtemp.equals("null")) || (strtemp.equals("NULL"))  )  )  
			   {
				   glockenCountOhneNULL ++ ;
			   }
		   } 
		   
		   Log.e("glockenCount" , "OhneNULL " + glockenCountOhneNULL) ;
		   Log.e("glockenCount" , " " + glockencount ) ;
		   return glockencount ;
	   }
	   */

	   /*
	   
	   private int leerTaste(int index) // 0 == normale Taste
										// 1 == leer Taste
										// 2 == NULL Taste also komplett leere Zeile
	   									// 3 == outOfBound
	   {
		   //Log.e("leerTaste" , "index=" + index) ;
		   
		   if(index > (excelread.getCellZeilen()-1) )
		   {
			   Log.e("ACHTUNG" , "outOfBound") ;
			   return 3 ;
		   }
		   
		   String temp = "";
		try {
			temp = (excelread.getCellString(2, index));
		} catch (ArrayIndexOutOfBoundsException e) {
			// TODO Auto-generated catch block
			//e.printStackTrace();
			//new LogExcelError(2, index, inputFilename, BESCHRIFTUNG_GLOCKEN_SHEET,sourceFileName,723) ;
			return 3 ;
		} catch (Exception e) {
			// TODO Auto-generated catch block
			//e.printStackTrace();
			//new LogExcelError(2, index, inputFilename, BESCHRIFTUNG_GLOCKEN_SHEET,sourceFileName,727) ;
			return 3 ;
		}
		   //Log.i("temp" , "=" + temp) ;
		 int  retCode = 0 ;
		    Log.e("checkLeerTaste" , "index=" + index + "temp=" + temp) ;
		    if( ( temp.equals("leer")) )
		    {
		    	retCode = 1 ;
		    }
		    if( (temp.equals("NULL")))
		    {
		    	retCode = 2 ;
		    }
		    
		   Log.e("retCode" , "=" + retCode) ;
		   return retCode ;
	   }
	   */

		/*
	   
	   private int getKloeppelCount()
	   {
		   int kloeppelcount = 0 ;
		   kloepelFaengerCountOhneNULL = 0 ;
		   
		   int max_zeilen = excelread.getCellZeilen() ;
		   max_zeilen = Math.min(max_zeilen, (ANFANG_SEITE2_INDEX+8+8) ) ;
		   for (int i=ANFANG_SEITE2_INDEX + 8 ; i < max_zeilen; i++)
		   {
			   String strtemp = "";
			try {
				strtemp = excelread.getCellString(2, i);
			} catch (ArrayIndexOutOfBoundsException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				new LogExcelError(2, i, inputFilename, BESCHRIFTUNG_GLOCKEN_SHEET,sourceFileName,746) ;
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				new LogExcelError(2, i, inputFilename, BESCHRIFTUNG_GLOCKEN_SHEET,sourceFileName,750) ;
			}
			if ( ! ( (strtemp.equals("null"))))  // || (strtemp.equals("NULL")) ) )
			   {
				   kloeppelcount ++;
			   }
			   if ( ! ( (strtemp.equals("null")) || (strtemp.equals("NULL"))  )  )  
			   {
				   kloepelFaengerCountOhneNULL ++ ;
			   }
		   } 
		   //Log.i("kloeppelcount" , "=" + kloeppelcount) ;
		   return kloeppelcount ;
	   }
	   */

	/*
	   private int getGruppeDreiCount()
	   {
		   int gruppe3count = 0 ;
		   gruppeDreiCountOhneNULL = 0 ;
		   int max_zeilen = excelread.getCellZeilen() ;
		   max_zeilen = Math.min(max_zeilen, (ANFANG_SEITE2_INDEX+8+8+8) ) ;
		   for (int i = ANFANG_SEITE2_INDEX + 8 + 8   ; i < max_zeilen ; i++)
		   {
			   String strtemp = "";
			try {
				strtemp = excelread.getCellString(2, i );
			} catch (ArrayIndexOutOfBoundsException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				new LogExcelError(2, i, inputFilename, BESCHRIFTUNG_GLOCKEN_SHEET,sourceFileName,772) ;
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				new LogExcelError(2, i, inputFilename, BESCHRIFTUNG_GLOCKEN_SHEET,sourceFileName,776) ;
			}
			if ( ! ( (strtemp.equals("null"))))  // || (strtemp.equals("NULL")) ) )
			   {
				   gruppe3count++;
			   }
			   if ( ! ( (strtemp.equals("null")) || (strtemp.equals("NULL"))  )  )  
			   {
				   gruppeDreiCountOhneNULL ++ ;
			   }
		   }
		   return gruppe3count ;
	   }
	*/

	/*
	private void loadOnlyButtonText()
	   {   // Log.i("Suche" , "relais, hammer") ;
		   int max_zeilen = excelread.getCellZeilen()  ;
		   for (int i = ANFANG_SEITE2_INDEX ; i < max_zeilen ; i++) // max 3 reihen je 8 buttons
		   {	
			   try 
			   {
				   if ( excelread.getCellString(2, i).equals("null"))
					  {
						  break ; // leeres Tabellen element
					  }
				   
				   
				if 	( ( ( ! excelread.getCellString(2, i).equals("leer") 
							   &&  ( ! excelread.getCellString(2, i).equals("NULL") )
							   
					) ) ) 
					
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
				new LogExcelError(2, i, inputFilename, BESCHRIFTUNG_GLOCKEN_SHEET,sourceFileName,826) ;
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				new LogExcelError(2, i, inputFilename, BESCHRIFTUNG_GLOCKEN_SHEET,sourceFileName,830) ;
			}
		   }
		   

	   }
	   
	      
	   private String getButtonText(int index)
	   {
		   return beschriftung.elementAt(index);
	   }

	   */

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
                    //UhrThread.verknuepfteTastenString.add(verknuepftTemp);
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

    private String convertUhrzeitToPrint(int stunde, int minute) {
        Calendar cal = Calendar.getInstance();
        cal.set(2015, 9, 29, stunde, minute);
        Date dt = cal.getTime();
        SimpleDateFormat df = new SimpleDateFormat("HH:mm");
        return df.format(dt);
    }

    //public void printTimeButtonTime() {

    //	timeButton.setText("Um " + convertUhrzeitToPrint(StaticVariable.startStundeManuell, StaticVariable.startMinuteManuell));
    //}

    public void printStartzeitFromMs(long ms) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm");
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(ms);
        timeButton.setText("Um " + dateFormat.format(cal.getTime()));
    }

    public void printEinAus() {
        if (StaticVariable.manuellerStartEinAus) {
            einAusButton.setText("EIN");
        } else {
            einAusButton.setText("AUS");
        }
    }

    public void incrementStartTime() {
        StaticVariable.startMinuteManuell++;
        if (StaticVariable.startMinuteManuell > 59) {
            StaticVariable.startMinuteManuell = 0;
            StaticVariable.startStundeManuell++;
            if (StaticVariable.startStundeManuell > 23) {
                StaticVariable.startStundeManuell = 0;
            }
        }
    }

    public void decrementStartTime() {
        StaticVariable.startMinuteManuell--;
        if (StaticVariable.startMinuteManuell < 0) {
            StaticVariable.startMinuteManuell = 59;
            StaticVariable.startStundeManuell--;
            if (StaticVariable.startStundeManuell < 0) {
                StaticVariable.startStundeManuell = 23;
            }
        }
    }

    public int dpToPx(int dp) {
        DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
        int px = Math.round(dp * (displayMetrics.xdpi / DisplayMetrics.DENSITY_DEFAULT));
        return px;
    }

    public int pxToDp(int px) {
        DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
        int dp = Math.round(px / (displayMetrics.xdpi / DisplayMetrics.DENSITY_DEFAULT));
        return dp;
    }

    private int dp2px(int dp) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, context.getResources().getDisplayMetrics());
    }

//	   public void closeWorkbook()
//	   {
//		   excelread.closeWorkbook();
//	   }
//	   

} // ende der Klasse
