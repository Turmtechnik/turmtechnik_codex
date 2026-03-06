package tom.turmtechnik;

import android.os.Environment;
import android.util.Log;
//import android.util.Log;

public class StaticConstants {
    public static final boolean DEBUG = true;
    
    // Feature-Flag: Feiertage-Tagtyp-Zuordnung aus Datenbank lesen (mit Rollback auf Excel)
    // Setze auf true, um Feiertage-Tagtypen aus DB zu lesen, false für Excel
    public static final boolean USE_DATABASE_FOR_FEIERTAGE_TAGTYPEN = true;

    public static final int PLAY_SOUND_EINMALIG =  0 ;
    public static final int PLAY_SOUND_IMMMER =  -1 ;
    public static final int PLAY_SOUND_3_MAL =  3 ; // 3 = sound 3 mal spielen
    public static final int PLAY_SOUND_2_MAL =  2 ; // 3 = sound 3 mal spielen

    //30.07.2016 -- umbau auf 256 Relaisnummern, aenderung der 10X, und 20X Nummern auf 110X .. 120X Nummern

    public static final int STOP = 1100;
    public static final int AUTOMATIC = 1101;
    public static final int HOME = 1102;
    public static final int HELP = 1103;
    public static final int SCHLAGWERK_ON_OFF = 1104;

    public static final int ZWEITE_SEITE = 1202;
    public static final int PROGRAMM_EINGEBEN = 1203;
    public static final int PROGRAMM_ABFRAGEN = 1204;
    public static final int SET_NEBENUHR = 1205;
    public static final int MANUELER_START = 1206;
    public static final int LIMIT_1000_100 = 1000;
    public static final int LIMIT_1000_200 = 1200;
    public static final int LIMIT_1000_300 = 1300;

//	8.9.16 wieder zurueck weg pflege alter Anlagen
/*
    public static final int STOP = 100 ;
	public static final int AUTOMATIC = 101 ;
	public static final int HOME = 102 ;
	public static final int HELP = 103 ;
	public static final int SCHLAGWERK_ON_OFF = 104 ;

	public static final int ZWEITE_SEITE = 202 ;
	public static final int PROGRAMM_EINGEBEN = 203 ;
	public static final int PROGRAMM_ABFRAGEN =204 ;
	public static final int SET_NEBENUHR = 205 ;
	public static final int MANUELER_START = 206 ;
	public static final int LIMIT_1000_100 = 100 ;
	public static final int LIMIT_1000_200 = 200 ;
	public static final int LIMIT_1000_300 = 300 ;
*/

    //public static  final int BATT_SHUT_DOWN_LEVEL = 75; // bei unter 75% ausschalten
    // ab 12.1.2014 variabel


    public static final String FERNSTEUERN_SPEICHER_ORT_STRING = "/Turmtechnik/Config/Fernsteuern-Speicher-Ort.xls";


    public static final String variableFesttageString = "/Turmtechnik/Programmtage/Festtage/Variable Festtage.xls";
    public static final String festeFesttageString = "/Turmtechnik/Programmtage/Festtage/Feste Festtage.xls";
    public static final String benutzerMelodienString = "/Turmtechnik/Config/Benutzermelodien.xls";
    public static final String normalprogrammString = "/Turmtechnik/Programmtage/Normalprogramm.xls";

    //	public static final String beschriftungGlockenString = "/Turmtechnik/Config/Beschriftung Glocken.xls";
    // geaendert auf multi sheet System.xls 7.6.13
    public static final String excellSystemString = "/Turmtechnik/Config/System.xls";

    /** Mondanzeige: Größe in % der Bildschirmbreite (0.05 = 5%) für Layout – gleiche Aufteilung bei allen Auflösungen. */
    public static final float MOND_SIZE_PERCENT_LAYOUT = 0.05f;
    /** Mondanzeige: Größe in % der Bildschirmhöhe (0.18 = 18%) für Dialog – gleiche Aufteilung bei allen Auflösungen. */
    public static final float MOND_SIZE_PERCENT_DIALOG = 0.18f;

    // ---------- Layout Seite 1: alle Maße in % von Bildschirmbreite/-höhe, gleiche Aufteilung bei allen Auflösungen ----------
    /** Logo: X-Position = Anteil der Breite (1/40). */
    public static final float LAYOUT1_LOGO_X_PERCENT = 1f / 40f;
    /** Logo: Y-Position = Anteil der Höhe (1/40). */
    public static final float LAYOUT1_LOGO_Y_PERCENT = 1f / 40f;
    /** Logo: Kantenlänge = Anteil der Breite (50/300 ≈ 16,67%). */
    public static final float LAYOUT1_LOGO_SIZE_PERCENT = 50f / 300f;
    /** Uhr: Breite = 50% der Bildschirmbreite. */
    public static final float LAYOUT1_CLOCK_WIDTH_PERCENT = 0.5f;
    /** Uhr: Höhe = Anteil der Höhe (1/12). */
    public static final float LAYOUT1_CLOCK_HEIGHT_PERCENT = 1f / 12f;
    /** Datum: Breite = 1/3 der Bildschirmbreite. */
    public static final float LAYOUT1_DATUM_WIDTH_PERCENT = 1f / 3f;
    /** Datum: X-Position = 9/13 der Breite. */
    public static final float LAYOUT1_DATUM_X_PERCENT = 9f / 13f;
    /** Datum: Y-Position = 1/50 der Höhe. */
    public static final float LAYOUT1_DATUM_Y_PERCENT = 1f / 50f;
    /** Infotext: Breite = 100%. */
    public static final float LAYOUT1_INFOTEXT_WIDTH_PERCENT = 1f;
    /** Infotext: Höhe = 1/10 der Bildschirmhöhe. */
    public static final float LAYOUT1_INFOTEXT_HEIGHT_PERCENT = 1f / 10f;
    /** Infotext: Y-Position = 1/4 - 1/20 der Höhe. */
    public static final float LAYOUT1_INFOTEXT_Y_PERCENT_TOP = 1f / 4f;
    public static final float LAYOUT1_INFOTEXT_Y_PERCENT_OFFSET = 1f / 20f;
    /** Stop-Button: Breite = 1/8, Höhe = 1/5, X = rechts 1/8 Abstand, Y = 1/30. */
    public static final float LAYOUT1_STOP_WIDTH_PERCENT = 1f / 8f;
    public static final float LAYOUT1_STOP_HEIGHT_PERCENT = 1f / 5f;
    public static final float LAYOUT1_STOP_X_RIGHT_MARGIN_PERCENT = 1f / 8f;
    public static final float LAYOUT1_STOP_Y_PERCENT = 1f / 30f;
    /** Glocken-Tasten: Höhe = 1/5 der Bildschirmhöhe. */
    public static final float LAYOUT1_GLOCKEN_BUTTON_HEIGHT_PERCENT = 1f / 5f;
    /** Glocken-Tasten: Zeile 1 Y = (5/14 - 1/20) der Höhe, Zeile 2 = (5/9 - 1/20), Zeile 3 = (30/40 - 1/20). */
    public static final float LAYOUT1_GLOCKEN_ROW1_Y_PERCENT = (5f / 14f) - (1f / 20f);
    public static final float LAYOUT1_GLOCKEN_ROW2_Y_PERCENT = (5f / 9f) - (1f / 20f);
    public static final float LAYOUT1_GLOCKEN_ROW3_Y_PERCENT = (30f / 40f) - (1f / 20f);
    /** Textgrößen als Anteil der Bildschirmhöhe (Fallback wenn Config 0), damit Schrift bei allen Auflösungen proportional. */
    public static final float LAYOUT1_TEXT_CLOCK_PERCENT = 0.055f;
    public static final float LAYOUT1_TEXT_WOCHENTAG_PERCENT = 0.035f;
    public static final float LAYOUT1_TEXT_INFO_PERCENT = 0.032f;
    public static final float LAYOUT1_TEXT_TASTEN_PERCENT = 0.028f;
    /** Seite 2: oberer Rand = Anteil der Höhe (1/20). */
    public static final float LAYOUT1_SEITE2_MARGIN_TOP_PERCENT = 1f / 20f;

    /** Nebenuhr-Seite: Uhr und Tasten in % – gleiche Aufteilung bei allen Auflösungen. */
    public static final float NEBENUHR_CLOCK_WIDTH_PERCENT = 0.5f;
    public static final float NEBENUHR_CLOCK_HEIGHT_PERCENT = 0.25f;
    public static final float NEBENUHR_CLOCK_Y_PERCENT = 0.1f;
    public static final float NEBENUHR_TIME_BUTTON_HEIGHT_PERCENT = 1f / 6f;
    public static final float NEBENUHR_PADDING_H_PERCENT = 1f / 24f;
    public static final float NEBENUHR_GAP_PERCENT = 1f / 40f;
    public static final float NEBENUHR_ROW_GAP_PERCENT = 1f / 50f;
    public static final float NEBENUHR_ROW1_Y_PERCENT = (6f / 14f);
    public static final float NEBENUHR_ROW2_OFFSET_PERCENT = 1f / 6f;

    public static final String nebenUhrString = "/Turmtechnik/Config/Nebenuhr.xls";
    public static final String schlagwerkString = "/Turmtechnik/Schlagwerk/Schlagwerkzeiten.xls";
    //	gaendert auf multi sheet System.xls sheet 14 ,  7.6.13
    //  public static final String bluetoothConfigString = "/Turmtechnik/Config/Bluetooth_config.xls" ;

/*// original von breite abgeleitet .. eventuell besser von hoehe ableiten 	
	public static int getBuchstabenGroesse(int displayWidth)
	{
		int buchstabenGroesse ;
		if ( displayWidth > 1279)
		{
			buchstabenGroesse = ( displayWidth / 94);
		//	buchstabenGroesseUhr = buchstabenGroesse * 7 ;
		}
		else if ( displayWidth > 1023)
		{
			buchstabenGroesse = ( displayWidth / 88);
			//buchstabenGroesseUhr = buchstabenGroesse * 7 ;
		}
		else if ( displayWidth > 799 )
		{
			buchstabenGroesse = ( displayWidth / 86 );
			//buchstabenGroesseUhr = buchstabenGroesse * 7 ;
		}
			else if ( displayWidth > 479)
			{
				buchstabenGroesse = ( displayWidth / 76);
			//	buchstabenGroesseUhr = buchstabenGroesse * 7;
			}
			else
			{
				buchstabenGroesse = ( displayWidth / 70);
			//	buchstabenGroesseUhr = buchstabenGroesse * 7 ;
		    }
		return buchstabenGroesse ;
	}
}
*/

    public static int getBuchstabenGroesse(int displayHeight) {
        if (displayHeight > 719) {
            return displayHeight / 80;
        } else {
            if (displayHeight > 480) {
                return displayHeight / 40;
            } else {
                return displayHeight / 60;
            }
        }

    }

    public static int[] leseBuchstabenGroesse() {
        String filename =
                (Environment.getExternalStorageDirectory().getPath() + "/Turmtechnik/Config/TextSize_config.xls");

        int temp[] = {
                56, 16, 14,        // default Schriftgroessen Uhr,Wochentag,Info Text
                0, 0,        // Uhr x,y
                0, 0,        // Wochentag x,y
                0, -10,    // Info Text x,y
                0, 0,        // Logo x,y
                -20,        // Logo size
                20,        // Buchstabengroesse Tastenbeschriftung
                10,        // Tastenblock Y offset
                0x00FF00    // [14] default Tasten Text Farbe
        };

        ExcelRead excelread = new ExcelRead();

        try {

            //Log.i ("versuch" , "TextSize_config open") ;
            //Log.i ("filename" , "= " + filename) ;
            excelread.openXls_save(filename);
            // o.k. file vorhanden... damit arbeiten
            //Log.i("open" , "gelungen") ;
            temp[0] = Integer.parseInt(excelread.getCellString(1, 1)); // Schriftgroesse Uhr
            temp[1] = Integer.parseInt(excelread.getCellString(1, 2)); // Schriftgroesse Wochentag
            temp[2] = Integer.parseInt(excelread.getCellString(1, 3)); // Schriftgroesse Info Text
            temp[3] = Integer.parseInt(excelread.getCellString(2, 1)); // Uhr x
            temp[4] = Integer.parseInt(excelread.getCellString(3, 1)); // Uhr y
            temp[5] = Integer.parseInt(excelread.getCellString(4, 1)); // Wochentag x
            temp[6] = Integer.parseInt(excelread.getCellString(5, 1)); // Wochentag y
            temp[7] = Integer.parseInt(excelread.getCellString(6, 1)); // Info Text x
            temp[8] = Integer.parseInt(excelread.getCellString(7, 1)); // Info Text y
            temp[9] = Integer.parseInt(excelread.getCellString(8, 1)); // Logo x
            temp[10] = Integer.parseInt(excelread.getCellString(9, 1)); // Logo y
            temp[11] = Integer.parseInt(excelread.getCellString(10, 1)); // Logo size
            temp[12] = Integer.parseInt(excelread.getCellString(1, 4)); // Tasten Text Schriftgroesse
            temp[13] = Integer.parseInt(excelread.getCellString(11, 1)); // Tasteblock Y offset
            //Log.e("buchstaben" , "x-11,y-1 = " + excelread.getCellString(11,1)) ;
            //Log.e("buchstaben" , "x-1,y-5 = " + excelread.getCellString(1,5)) ;
            long tempLong = Long.parseLong(excelread.getCellString(1, 5), 16);
            temp[14] = (int) tempLong;
            // temp[14] = Integer.parseInt(excelread.getCellString(1,5)) ; // RGB Tasten Text Farbe

            excelread.closeWorkbook();
        } catch (Exception e) {
            //Log.e("buchstabenF[14]" , "="+temp[14]) ;
            //Log.i("ist nicht da" , "nichts tun") ;
        }
        //Log.i("temp[0]" , "=" + temp[0]) ;
        //Log.i("temp[1]" , "=" + temp[1]) ;
        //Log.i("temp[2]" , "=" + temp[2]) ;
        //Log.e("buchstabenF[14]" , "="+temp[14]) ;

        return temp;
    }
}