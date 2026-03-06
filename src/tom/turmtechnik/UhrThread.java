package tom.turmtechnik;

import android.os.Environment;
import android.util.Log;
//import android.util.Log;
//import android.util.Log;

import com.awr_technology.sunrisesunset.Location;
import com.awr_technology.sunrisesunset.SunriseSunsetCalculator;
import tom.turmtechnik.TagesSuche;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;
import java.util.Vector;

import jxl.read.biff.BiffException;

public class UhrThread extends Thread {
    private final static String sourceFileName = "UhrThread";

    private boolean esWarBenutzerProgramm = false;
    private boolean melodienGefunden = false;
    private boolean heizungGefunden = false;

    //private static int normalOderFesttag = 0 ;  //  0 == normale Tage
    //  1 == fester Festtag
    //  2 == variabler Festtag

    private final int SPALTE_C_MELODIE_NAME = 2;
    private final int SPALTE_B_FUNKTION_NAME = 1;
    private final int SPALTE_A_STARTZEIT = 0;

    private static int UPDATE_EXCEL = 30;  // alle 30 Minuten neu laden
    /** Lock: Nur ein Melodie-Start gleichzeitig (verhindert 4x Start bei mehreren UhrThread-Instanzen). */
    private static final Object MELODIE_START_LOCK = new Object();
    private boolean doRun = true;

    private int zeilenNummer = -1;
    private static ExcelRead excelread = null;
    private int zeile = -1;
    private String zeitStringFromProgrammliste;
    Calendar calendar;     /// = Calendar.getInstance ();

    private int futureStunde;
    private int futureMinute;
    //private int futureSekunde ;
    private int futureTag;
    private int futureMonat;
    private int futureJahr;

    // fuer Festtage check

    private int festtageTag = 0;
    private int festtageMonat = 0;
    private int festtageJahr = 0;


    public static Vector<String> blockFileNamen = new Vector<String>();

    public static int blockReady = 0; // 0 = kein block aktiv sonst block nummer

    public static boolean newSearchAutomaticStart = true;

    //private Boolean[] incrementFutureTime = new Boolean[] { true, true, true } ;

    // enthaelt alle zur aktuellen Zeit passenden offsets von oben in die Programmliste (Zeile)
    // z.B. 12:00  -->   4, 7, 9
    private Vector<Integer> aktuelleZeitZeilenListe = new Vector<Integer>();


    public static Vector<String> verknuepfteTastenString = new Vector<String>();

    private boolean prognoseModus = false;

    private int minutenSave = 0;
    private int sekundenSave = 0;

    private String melodiePath =
            (Environment.getExternalStorageDirectory().getPath() + "/Turmtechnik/Melodien/");

    private String melodieString = "";

    private String excelTableFileName;

    private static boolean normalProgrammFlag;

    private int countMinutenwechsel = UPDATE_EXCEL;

    TagesSuche tagesSuche = null;
    private boolean esWarManuellerStart = false;
    private boolean melodieVorbeginnTimeGefunden;

    /** Im DB-Modus: gecachtes nächstes Programm (Excel-Zeile), -1 = nicht gesetzt. */
    private int nextProgrammZeile = -1;
    private int nextProgrammStunde = -1;
    private int nextProgrammMinute = -1;
    /** Beginn-Zeit (Programmstart minus Vorlauf) in ms. Vorlauf = Melodien-Vorlauf (Minuten), nicht Vorschwingen (Sekunden pro Glocke). */
    private long nextProgrammBeginnMs = -1;
    /** Öffentlich für Programmsuche: Activity schließt sich, wenn nur noch ≤2 s bis Start. */
    public static volatile long nextProgrammBeginnMsPublic = -1;
    /** true, wenn in diesem Lauf das gecachte Programm ausgeführt werden soll (Liste nicht leeren, keine DB-Abfrage). */
    private boolean dbModeExecuteThisRun = false;

    public static boolean getNormalProgrammFlag() {
        return normalProgrammFlag;
    }

    private SchlagwerkThread schlagwerkthread;
    private NebenUhrThread nebenuhrthread;

    int seite2_button_offset = TurmtechnikActivity.BESCHRIFTUNG_TASTEN_SLOTS_PAGE1;
    int page2_button_offset = seite2_button_offset;

    public void run() {

        // excelread = new ExcelRead() ;
        //excelread.openXls(TurmtechnikActivity.normalprogrammFileString);
        // wegen Festtagen geaendert 9.4.2013

        long waitStartMs = System.currentTimeMillis();
        final long maxWaitInitMs = 60_000L; // max. 60 s warten
        while (StaticVariable.helpForStartBenutzermelodien == true) {
            sleepTime(500);
            if (System.currentTimeMillis() - waitStartMs >= maxWaitInitMs) {
                Log.w(sourceFileName, "Timeout beim Warten auf Benutzerprogramm-Initialisierung, fahre fort.");
                StaticVariable.helpForStartBenutzermelodien = false;
                break;
            }
        }

        excelread = null;

        calendar = Calendar.getInstance();
        minutenSave = calendar.get(Calendar.MINUTE);  // init minutenSave
        newSearchAutomaticStart = true;

        excelTableFileName = "";

        while (doRun) {
            //Log.e("uhrThread" , "laueft") ;
            sleepTime(1); // pruefe alle 40 ms
            //Log.i("block thread" , "ready=" + blockReady); // ist block Thread fertig?

            calendar = Calendar.getInstance();

            // Sofort Programmabfrage ausführen, wenn Verknüpfte-Taste geändert wurde (nicht auf nächste Sekunde warten)
            if (StaticVariable.refreshInfoTextVerknuepfteTaste) {
                StaticVariable.refreshInfoTextVerknuepfteTaste = false;
                newSearchAutomaticStart = true;
                if (blockReady == 0) {
                    checkNextAutomaticStart();
                }
            }

            int sekundenTemp = calendar.get(Calendar.SECOND); // 28.01.15 sekunden wegen vorschwingen

            //Log.e("sekunden" , "save=" + sekundenSave + "temp=" + sekundenTemp) ;
            if ((sekundenTemp != sekundenSave)) {
                sekundenSave = sekundenTemp;
                Long momentanMs = (calendar.getTimeInMillis());

                checkVorschwingen(momentanMs);
                checkMelodieStart(momentanMs);
                //checkHeizungStart(momentanMs) ; 21.03.15 AusgangHeizungsThreadNew2 prueft das
                // Nächsten Automatik-Start nur einmal pro Sekunde prüfen (nicht bei jedem 1-ms-Tick)
                if (blockReady == 0) {
                    checkNextAutomaticStart();
                }
            }

            int minuTemp = calendar.get(Calendar.MINUTE);  // minuten takt von der Echtzeit uhr
            if ((minuTemp != minutenSave)) // ||  (excelread == null) )
            {
                //Log.i("PROGRAMM" , "= " + excelTableFileName ) ;
                // zum Test!!! ********************************************************
                //festtageTag = 0 ; // damit alle Minuten neu geschaut wird!

                //Log.e("ACHTUNG" , "Minutenwechsel") ;
                StaticVariable.minutenTakt++; // fuer saveNebenuhr in Turmtechnik

                countMinutenwechsel--;

                minutenSave = minuTemp;

                // Jede Minute Suche auslösen, damit nächste Programmzeit und ggf. Start korrekt sind
                newSearchAutomaticStart = true;

                if ((minuTemp == 00) || (minuTemp == 15) || (minuTemp == 30) || (minuTemp == 45)) {
                    //Log.e("schlagwerk" , "gestartet") ;
                    schlagwerkthread = new SchlagwerkThread(null); // normaler Schlagwerk thread
                    schlagwerkthread.setPriority(Thread.MAX_PRIORITY);
                    schlagwerkthread.start();

                    //StaticVariable.schlagwerkTrigger = true ;
                    //7.9.16 sync nmea time nicht alle 15 Minuten
                    // aber bei Stop Taste
                    // oder nach dem Neustart
                }


                nebenuhrthread = new NebenUhrThread();
                nebenuhrthread.start();
                //		new NebenUhrThread().start();
                //Log.e("nebenuhr" , "gestartet") ;
            }

            // Nur neu laden bei Datumswechsel, Benutzeränderung oder wenn noch keine TagesSuche (nicht bei excelread==null, sonst im DB-Modus bei jedem Tick!)
            boolean datumGeaendert = (festtageTag != calendar.get(Calendar.DAY_OF_MONTH)) ||
                    (festtageMonat != calendar.get(Calendar.MONTH)) ||
                    (festtageJahr != calendar.get(Calendar.YEAR));
            boolean nochNichtInitialisiert = (tagesSuche == null || excelTableFileName == null || excelTableFileName.isEmpty());
            if (datumGeaendert || nochNichtInitialisiert || (StaticVariable.benutzerGeandert == true)) {
                if (StaticVariable.benutzerGeandert == true) {
                    StaticVariable.benutzerGeandert = false;
                }
                if (excelread == null && !nochNichtInitialisiert) {
                    //Log.e("EXCELREAD" , "= null (DB-Modus)") ;
                }
                // newSearchAutomaticStart = true ; // 28.10.2013
                // wegen alle minuten Tabelle neu oeffnen
                // fern update mit dropbox z.B.

                festtageTag = calendar.get(Calendar.DAY_OF_MONTH);
                festtageMonat = calendar.get(Calendar.MONTH);
                festtageJahr = calendar.get(Calendar.YEAR); //+ 2 ; // ***************************
                // + 2 war notwendig zum Test der Zukunft !!! ( z.B. 2015 )

                // Benutzerprogramm kann für dieses Datum einen Programmtag erzwingen (ganzer Tag umschalten)
                android.content.Context ctx = TurmtechnikActivity.turmtechnikContext;
                if (ctx != null) TurmtechnikActivity.loadBenutzerprogrammeFromDb(ctx);
                String benutzerTagtyp = TurmtechnikActivity.getBenutzerprogrammTagtypForDate(festtageTag, festtageMonat, festtageJahr);
                if (benutzerTagtyp != null && !benutzerTagtyp.isEmpty()) {
                    excelTableFileName = benutzerTagtyp;
                    if (excelTableFileName.toLowerCase().endsWith(".xls")) excelTableFileName = excelTableFileName.substring(0, excelTableFileName.length() - 4);
                    normalProgrammFlag = "Normalprogramm".equals(excelTableFileName);
                    Log.d(sourceFileName, "Tagtyp aus Benutzerprogramm für " + festtageTag + "." + (festtageMonat + 1) + "." + festtageJahr + ": " + excelTableFileName);
                } else {
                    // Tagtyp aus Datum (Normalprogramm/Feiertag). Benutzerprogramme laufen zusätzlich, wenn Datum+Uhrzeit passen.
                    excelTableFileName = TagesSuche.getPathAndFilenameToday(festtageTag, festtageMonat, festtageJahr);
                    if (excelTableFileName == null || excelTableFileName.equals("")) {
                        excelTableFileName = "Normalprogramm";
                        normalProgrammFlag = true;
                    } else {
                        if (excelTableFileName.toLowerCase().endsWith(".xls")) {
                            excelTableFileName = excelTableFileName.substring(0, excelTableFileName.length() - 4);
                        }
                        normalProgrammFlag = false;
                    }
                }

                // Prüfe ob Programme in der Datenbank existieren
                boolean programmeInDB = false;
                String tagtypNameForDB = excelTableFileName;
                if (!programmeInDB) {
                    try {
                        android.content.Context context = TurmtechnikActivity.turmtechnikContext;
                        if (context != null) {
                            PlatinenDatabaseHelper dbHelper = PlatinenDatabaseHelper.getInstance(context);
                            java.util.List<Programm> programme = dbHelper.getProgrammeByTagtyp(tagtypNameForDB);
                            if (programme != null && !programme.isEmpty()) {
                                programmeInDB = true;
                                Log.d(sourceFileName, "Programme in DB gefunden für " + tagtypNameForDB + " (" + programme.size() + " Programme), verwende DB statt Excel");
                                excelTableFileName = tagtypNameForDB;
                            } else {
                                Log.d(sourceFileName, "Keine Programme in DB für " + tagtypNameForDB + ", versuche Excel-Fallback");
                            }
                        }
                    } catch (Exception e) {
                        Log.e(sourceFileName, "Fehler beim Prüfen der DB für " + tagtypNameForDB, e);
                    }
                }

                // Excel nur als Fallback: Wenn für diesen Tagtyp (z. B. Normalprogramm) keine Programme in der DB sind,
                // wird die alte .xls-Datei aus Programmtage/ geladen. Sobald programmeInDB=true, bleibt excelread=null.
                // Siehe DATENBANK_MIGRATION.md / WEB_UI_MIGRATION.md für den Migrationsstand.
                if (!programmeInDB) {
                    // Konvertiere zurück zu Dateipfad für Excel
                    String excelFilePath;
                    if (excelTableFileName.equals("Normalprogramm")) {
                        excelFilePath = TurmtechnikActivity.normalprogrammFileString;
                    } else {
                        excelFilePath = TurmtechnikActivity.sdCardPath
                                + "/Turmtechnik/Programmtage/" + excelTableFileName + ".xls";
                    }
                    
                    // Prüfe, ob die Datei existiert (bei Benutzerprogramm nicht hier, da programmeInDB schon true)
                    java.io.File checkFile = new java.io.File(excelFilePath);
                    if (!checkFile.exists()) {
                        Log.w(sourceFileName, "Excel-Datei nicht gefunden: " + excelFilePath + ", verwende Normalprogramm");
                        excelFilePath = TurmtechnikActivity.normalprogrammFileString;
                        excelTableFileName = "Normalprogramm";
                        normalProgrammFlag = true;
                        checkFile = new java.io.File(excelFilePath);
                        if (!checkFile.exists()) {
                            Log.e(sourceFileName, "Auch Normalprogramm-Datei nicht gefunden: " + excelFilePath);
                            excelread = null;
                        }
                    }

                    if (checkFile.exists()) {
                        excelread = new ExcelRead();
                        try {
                            excelread.openXls(excelFilePath);
                        } catch (BiffException e) {
                            e.printStackTrace();
                            new LogExcelError(-1, -1, excelFilePath, -1, sourceFileName, 171);
                            Log.e(sourceFileName, "Fehler beim Öffnen von Excel-Datei: " + excelFilePath);
                            excelread = null;
                        } catch (IOException e) {
                            e.printStackTrace();
                            new LogExcelError(-1, -1, excelFilePath, -1, sourceFileName, 175);
                            Log.e(sourceFileName, "Fehler beim Öffnen von Excel-Datei: " + excelFilePath);
                            excelread = null;
                        }
                    }
                } else {
                    // Programme sind in DB, excelread bleibt null
                    excelread = null;
                    Log.d(sourceFileName, "Verwende Datenbank, excelread bleibt null");
                }

                // TagesSuche kann mit null excelread arbeiten, wenn Programme aus DB kommen
                // WICHTIG: TagesSuche muss immer initialisiert werden, auch wenn excelread null ist
                tagesSuche = new TagesSuche(excelread, excelTableFileName);
                Log.d(sourceFileName, "TagesSuche initialisiert mit excelTableFileName=" + excelTableFileName + ", excelread=" + (excelread != null ? "vorhanden" : "null"));
                // Suche nächstes Programm sofort neu (damit Infozeile „nächstes Programm“ z. B. bei Benutzerprogramm-Tag aktualisiert wird)
                newSearchAutomaticStart = true;
            }


			   		/*  // 10.02.2015 wegen vorlauf ist alles anders
                       if ((TurmtechnikActivity.flagAutomaticOnOff)==true && blockReady == 0)
			   		{
			   	   			// pruefe zuerst die Benutzer Melodien
			   				melodieString = BenutzerMelodienActivity.checkStartBenutzerMelodie
			   						(calendar.get(Calendar.YEAR),
			   						 calendar.get(Calendar.MONTH),
			   						 calendar.get(Calendar.DAY_OF_MONTH),
			   						 calendar.get(Calendar.HOUR_OF_DAY),
			   						 calendar.get(Calendar.MINUTE)) ;
			   				
			   				if( ! melodieString.equals(""))
			   				{
			   					String blockPath = melodiePath + melodieString ;
			   					blockReady = 999 ; // zeige sonderblock ist aktiv
			   					MelodieThreadNew blockthread = new MelodieThreadNew(blockPath);
			   					blockthread.start();
			   					// TurmtechnikActivity.stringInfoText = "suche nächsten Start..." ;
			   					BenutzerMelodienActivity.clearLastMelodieButton();
			   					
			   					newSearchAutomaticStart = true ;
			   					
			   					if(countMinutenwechsel <= 0)
			   					{	
			   						countMinutenwechsel = UPDATE_EXCEL ;
			   						if (excelread != null) {
			   							excelread.closeWorkbook(); // excel tabelle freigeben
			   							excelread = null ;
			   							//Log.e("excelread" , "geschlossen") ;
			   						}
			   						System.gc();
			   						sleepTime(200);
			   					}
			   			    			   					
			   					sleepTime (1500); // damit nicht zur selben zeit nocheinmal gestartet wird !!
			   				}
			   				
			   				// was ist mit dem Feiertag????  6.11.2014
			   				
			   			    // pruefe normales Programm
			   			    if ( (excelread != null ) && (blockReady != 999) && (checkTime()) )
			   				{
			   					Log.e("timeOk", "starte block thread");
			   		    		
			   					String blockName = "" ;
			   					String funktionName ="" ;
                                String melodieNameOderZeit ="" ;
			   					//switch(normalOderFesttag)
			   					//case 0: 
			   					//	 blockName = (TurmtechnikActivity.normalProgrammblockFileNamen.elementAt(zeilenNummer-3));
			   					//	 break ;
			   					
			   					//case 1:
			   					//	 blockName = (TurmtechnikActivity.festeFesttageTagtypFileNamen.elementAt(zeilenNummer-3));
			   					//	 break ;
			   			    	
			   					//case 2:
			   					//	 blockName = (TurmtechnikActivity.variableFesttageTagtypFileNamen.elementAt(zeilenNummer-3));
			   					//	 break ;
			   					//
			   					
			   					try {
									blockName = TurmtechnikActivity.sdCardPath + 
											"/Turmtechnik/Melodien/" +
											excelread.getCellString(SPALTE_C_MELODIE_NAME, zeilenNummer) + ".xls";
                                    funktionName = excelread.getCellString(SPALTE_B_FUNKTION_NAME , zeilenNummer) ;
                                    melodieNameOderZeit =  excelread.getCellString(SPALTE_C_MELODIE_NAME, zeilenNummer) ;
								} catch (ArrayIndexOutOfBoundsException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								} catch (Exception e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}		
			   					
			   					Log.e("melodieName" , "=" + blockName) ;
			   			    	
			   			    	if( ( funktionName.equals("Melodie")) )
			   			    	{
			   			    		Log.i("String" , "Melodie gefunden") ;
			   			    		blockReady = zeilenNummer ; // block aktiv anzeigen
			   			    		//MelodieThread blockthread = new MelodieThread(TurmtechnikActivity.normalProgrammblockFileNamen.elementAt(zeilenNummer-3));
				   					//blockthread.start();
			   			    		//new MelodieThread(TurmtechnikActivity.normalProgrammblockFileNamen.elementAt(zeilenNummer-3)).start();
			   			    		new MelodieThreadNew(blockName).start();
			   			    	}
			   			    	else
			   			    	{
			   			    		blockReady = 0 ; // bei Ausgang thread (Heizung) nicht blockieren. 
			   			    						 // neuen thread zulassen
			   			    		//AusgangHeizungThread ausgangHeizungThread =
			   			    		//		new AusgangHeizungThread(TurmtechnikActivity.normalProgrammblockFileNamen.elementAt(zeilenNummer-3)); 
			   			    		//ausgangHeizungThread.start();		
			   			    		new AusgangHeizungThreadNew(melodieNameOderZeit, funktionName).start();
			   			    	}
			   			    	
			   					setInfoText("suche nächsten Start...") ;
			   					newSearchAutomaticStart = true ;
			   					sleepTime (1500); // damit nicht zur selben zeit nocheinmal gestartet wird !!
			   					
			   				}
			   	   			else
			   	   			{
			   	   				if( (blockReady == 0) && (excelread != null) )
			   	   				{	
			   	   					checkNextAutomaticStart();
			   	   					
			   	   				}
			   	   			}
			   	  	}
			   		else
			   		{
//			   			if (TurmtechnikActivity.automaticOn==false)
//			   			{
//			   				TurmtechnikActivity.stringInfoText = "Automatic ausgeschaltet" ;
//			   			}
			   			if (blockReady != 0 && blockReady != 999)
			   			{
			   				setInfoText(getBlockMomentanText(blockReady)) ;
			   			}
			   			if (blockReady == 999)
			   			{
			   				setInfoText(melodieString) ;
			   			}
			   			
			   			
			   			// checkNextAutomaticStart();
			   		}
			   		*/

        }

        if (excelread != null) {
            excelread.closeWorkbook();
        }
        System.gc();

    } // ende von run

    //private void setInfoText(String infoText)
    //{
    //if(StaticVariable.serial_io_status == true)
    //{
    //		TurmtechnikActivity.stringInfoText = (infoText) ;
    //}
    //else
    //{
    //	TurmtechnikActivity.stringInfoText = "Achtung keine Verbindung zu den Schaltrelais!" ;
    //}
    //}


    private void checkNextAutomaticStart() {
        if (StaticVariable.sofortStartPopupGefunden) {
            return;
        }

        // WICHTIG: Stelle sicher, dass tagesSuche und excelTableFileName initialisiert sind
        // Wenn nicht, initialisiere sie jetzt (z.B. beim ersten Start oder wenn DB verwendet wird)
        if (tagesSuche == null || excelTableFileName == null || excelTableFileName.isEmpty()) {
            // Initialisiere tagesSuche und excelTableFileName
            festtageTag = calendar.get(Calendar.DAY_OF_MONTH);
            festtageMonat = calendar.get(Calendar.MONTH);
            festtageJahr = calendar.get(Calendar.YEAR);
            android.content.Context ctxInit = TurmtechnikActivity.turmtechnikContext;
            if (ctxInit != null) TurmtechnikActivity.loadBenutzerprogrammeFromDb(ctxInit);
            String benutzerTagtypInit = TurmtechnikActivity.getBenutzerprogrammTagtypForDate(festtageTag, festtageMonat, festtageJahr);
            if (benutzerTagtypInit != null && !benutzerTagtypInit.isEmpty()) {
                excelTableFileName = benutzerTagtypInit;
                if (excelTableFileName.toLowerCase().endsWith(".xls")) excelTableFileName = excelTableFileName.substring(0, excelTableFileName.length() - 4);
                normalProgrammFlag = "Normalprogramm".equals(excelTableFileName);
            } else {
                excelTableFileName = TagesSuche.getPathAndFilenameToday(festtageTag, festtageMonat, festtageJahr);
                if (excelTableFileName == null || excelTableFileName.equals("")) {
                    excelTableFileName = "Normalprogramm";
                    normalProgrammFlag = true;
                } else {
                    if (excelTableFileName.toLowerCase().endsWith(".xls")) excelTableFileName = excelTableFileName.substring(0, excelTableFileName.length() - 4);
                    normalProgrammFlag = false;
                }
            }
            
            // Prüfe ob Programme in DB sind
            boolean programmeInDB = false;
            if (!programmeInDB) {
                try {
                    android.content.Context context = TurmtechnikActivity.turmtechnikContext;
                    if (context != null) {
                        PlatinenDatabaseHelper dbHelper = PlatinenDatabaseHelper.getInstance(context);
                        java.util.List<Programm> programme = dbHelper.getProgrammeByTagtyp(excelTableFileName);
                        if (programme != null && !programme.isEmpty()) {
                            programmeInDB = true;
                            excelread = null; // Verwende DB
                        }
                    }
                } catch (Exception e) {
                    Log.e(sourceFileName, "Fehler beim Prüfen der DB in checkNextAutomaticStart", e);
                }
            }
            
            // Initialisiere tagesSuche
            tagesSuche = new TagesSuche(excelread, excelTableFileName);
            Log.d(sourceFileName, "checkNextAutomaticStart: tagesSuche initialisiert mit excelTableFileName=" + excelTableFileName + ", excelread=" + (excelread != null ? "vorhanden" : "null"));
        }

        // DB-Modus: Immer nächstes Programm suchen (Anzeige aktuell), Ausführung wenn Beginn-Zeit (Start − Vorlauf) erreicht
        // WICHTIG: Beginn mit den *bisherigen* nextProgramm-Werten prüfen, BEVOR findNextProgrammeFromDatabase() aufgerufen wird.
        // Sonst überspringt findNextProgrammeFromDatabase() das Programm (beginnMs <= currentMs), und es wird nie ausgeführt.
        if (excelread == null) {
            // Sicherstellen, dass Programm-Map geladen ist (für zeileOk/getProgrammByZeile/Melodie-Start)
            if (tagesSuche != null) {
                tagesSuche.ensureProgrammeFromDatabaseLoaded();
            }
            if (StaticVariable.programmeDatabaseChanged) {
                StaticVariable.programmeDatabaseChanged = false;
                newSearchAutomaticStart = true;
            }
            futureStunde = calendar.get(Calendar.HOUR_OF_DAY);
            futureMinute = calendar.get(Calendar.MINUTE);
            futureTag = calendar.get(Calendar.DAY_OF_MONTH);
            futureMonat = calendar.get(Calendar.MONTH);
            futureJahr = calendar.get(Calendar.YEAR);
            long currentMs = calendar.getTimeInMillis();
            boolean beginnErreicht = nextProgrammZeile >= 0 && (nextProgrammBeginnMs < 0 || currentMs >= nextProgrammBeginnMs);
            Log.w("ProgrammStart", "DB-Check: nextProgrammZeile=" + nextProgrammZeile + " nextProgrammBeginnMs=" + nextProgrammBeginnMs
                    + " currentMs=" + currentMs + " beginnErreicht=" + beginnErreicht + " (Zeile>=0 und currentMs>=beginnMs)");
            if (beginnErreicht) {
                // Zeitpunkt zum Starten des Programms ist erreicht → dieses Programm jetzt ausführen
                // WICHTIG: futureStunde/futureMinute = Programm-Startzeit (nextProgramm*), nicht aktuelle Uhrzeit!
                // Sonst wird msStartMelode mit 11:58 statt 12:00 berechnet → momentanMs > msStartMelode → Melodie startet nicht.
                Log.w("ProgrammStart", "Beginn erreicht -> dbModeExecuteThisRun=true, Liste=[zeile " + nextProgrammZeile + "] ProgrammStart " + nextProgrammStunde + ":" + nextProgrammMinute);
                futureStunde = nextProgrammStunde;
                futureMinute = nextProgrammMinute;
                futureTag = calendar.get(Calendar.DAY_OF_MONTH);
                futureMonat = calendar.get(Calendar.MONTH);
                futureJahr = calendar.get(Calendar.YEAR);
                aktuelleZeitZeilenListe.clear();
                aktuelleZeitZeilenListe.add(nextProgrammZeile);
                dbModeExecuteThisRun = true;
            }
            // Danach immer nächstes Programm suchen (für Anzeige und nächsten Lauf)
            findNextProgrammeFromDatabase();
            if (newSearchAutomaticStart) {
                newSearchAutomaticStart = false;
                melodienGefunden = false;
                heizungGefunden = false;
                berechneSonnenAufUndUntergang();
                if (!beginnErreicht && !StaticVariable.sofortStartPopupFlag) {
                    return;
                }
            } else if (!beginnErreicht && !StaticVariable.sofortStartPopupFlag) {
                return;
            }
        }

        if (newSearchAutomaticStart == true) {
            // dumpVerknuepft() ;

            futureStunde = calendar.get(Calendar.HOUR_OF_DAY);
            futureMinute = calendar.get(Calendar.MINUTE);
            //	futureSekunde = calendar.get(Calendar.SECOND);
            futureTag = calendar.get(Calendar.DAY_OF_MONTH);
            futureMonat = calendar.get(Calendar.MONTH);
            futureJahr = calendar.get(Calendar.YEAR);

            melodienGefunden = false;
            heizungGefunden = false;

            berechneSonnenAufUndUntergang();


            // bei aktueller zeit zu suchen beginnen
            //	TurmtechnikActivity.stringInfoText = "suche nächsten Start..." ;
            //for (int i = 0; i < 3 ; i++)
            //{
            //	incrementFutureTime[i]=true;
            //}
            newSearchAutomaticStart = false;
        }

        zeilenNummer = -1;

        if (!dbModeExecuteThisRun) {
            aktuelleZeitZeilenListe.clear();
        }
        
        // WICHTIG: Setze zukünftiges Datum in tagesSuche, damit zeileOk() das richtige Datum verwendet
        if (tagesSuche != null) {
            tagesSuche.setFutureDate(futureJahr, futureMonat, futureTag);
        }

        // Bei dbModeExecuteThisRun ist die Liste schon mit dem Programm zur Beginn-Zeit gefüllt – nicht überschreiben.
        // Sonst: Liste aus aktueller Minute füllen (Excel oder DB).
        if (!dbModeExecuteThisRun) {
            makeZeitenZeilenFuture();
        }

        //incrementSearchTime();

        if (StaticVariable.sofortStartPopupFlag) {
            // Namen normalisieren (Trim, Tabs/mehrfache Leerzeichen), damit DB-Abgleich mit Melodie-Tabelle funktioniert
            String sofortStartPopupFilename = StaticVariable.sofortStartPopupFilename != null
                    ? StaticVariable.sofortStartPopupFilename.trim().replaceAll("\\s+", " ").trim()
                    : "";

            setFutureTimeToStartTime();
            ;

            String pathAndFileName = makePathAndFilenameMelodieOhneXls(sofortStartPopupFilename);
            String benutzerMelodieMitStartZeit = tagesSuche.pad(futureStunde) + ":" + tagesSuche.pad(futureMinute) + " " + sofortStartPopupFilename;
            tagesSuche.setVorschwingZeitenZurMelodie(benutzerMelodieMitStartZeit, pathAndFileName, futureJahr, futureMonat, futureTag, futureStunde, futureMinute);
            String vorlaufZeitZurMelodie = tagesSuche.getBeginnTime2(sofortStartPopupFilename, futureJahr, futureMonat, futureTag, futureStunde, futureMinute);
            // Infozeile: berechneten Beginn (wie bei anderen Programmen) anzeigen, nicht die Startzeit
            String beginnStr = (vorlaufZeitZurMelodie != null && !vorlaufZeitZurMelodie.isEmpty()) ? vorlaufZeitZurMelodie : (tagesSuche.pad(futureStunde) + ":" + tagesSuche.pad(futureMinute));
            StaticVariable.stringInfoTextField[0] = (sofortStartPopupFilename != null ? sofortStartPopupFilename : "") + "  " + beginnStr;
            //StaticVariable.sofortStartPopupFlag = false ;

            newSearchAutomaticStart = true; // suche neu starten
            StaticVariable.sofortStartPopupGefunden = true;
            return;
            // esWarManuellerStart = true ;

        }

        if (StaticVariable.manuellerStartAktiviert) {
            String manuellerStartFilename = ManuelerStartActivity.getManuellenStartFilename(futureStunde, futureMinute);

            if (!manuellerStartFilename.equals("")) {
                StaticVariable.pathStoppedByUser = ""; // Nutzer startet explizit – Sperre aufheben
                String pathAndFileName = makePathAndFilenameMelodieOhneXls(manuellerStartFilename);
                String benutzerMelodieMitStartZeit = tagesSuche.pad(futureStunde) + ":" + tagesSuche.pad(futureMinute) + " " + manuellerStartFilename;
                tagesSuche.setVorschwingZeitenZurMelodie(benutzerMelodieMitStartZeit, pathAndFileName, futureJahr, futureMonat, futureTag, futureStunde, futureMinute);
                String vorlaufZeitZurMelodie = tagesSuche.getBeginnTime2(manuellerStartFilename, futureJahr, futureMonat, futureTag, futureStunde, futureMinute);

                String startStr = tagesSuche.pad(futureStunde) + ":" + tagesSuche.pad(futureMinute);
                StaticVariable.stringInfoTextField[0] = (manuellerStartFilename != null ? manuellerStartFilename : "") + "  " + startStr;
                newSearchAutomaticStart = true; // suche neu starten
                esWarManuellerStart = true;

            }
        }

        // Programmsuche offen: Anzeige überspringen, aber geplante Programmausführung trotzdem durchführen
        if (StaticVariable.programmAbfrageAktiv2 && !dbModeExecuteThisRun) {
            return;
        }

        if( ! melodienGefunden)  // wenn schon eine Melodie gefunden ist, nicht mehr nach Benutzer suchen, aber nach Heizung schon... 23.05.17
        {
            // check ob Benutzer Melodie aktuell
            String benutzerMelodie = BenutzerMelodienActivity.checkStartBenutzerMelodie
                    (futureJahr, futureMonat, futureTag, futureStunde, futureMinute);

            //Log.e("benutzer melodie" , "=" + benutzerMelodie) ;


            if (!benutzerMelodie.equals(""))
            {
                // etwas gefunden das passt:
                //TurmtechnikActivity.stringInfoText = BenutzerMelodienActivity.getMelodieStartTime() + " " +
                //    									 BenutzerMelodienActivity.getBeschriftungTaste() ;
                // + benutzerMelodie ;

                String pathAndFileName = makePathAndFilenameMelodieOhneXls(benutzerMelodie);

                String benutzerMelodieMitStartZeit = tagesSuche.pad(futureStunde) + ":" + tagesSuche.pad(futureMinute) + " " + benutzerMelodie;

                //Log.e("Benutzer pathAndFileName" , "=" + pathAndFileName) ;
                tagesSuche.setVorschwingZeitenZurMelodie(benutzerMelodieMitStartZeit, pathAndFileName, futureJahr, futureMonat, futureTag, futureStunde, futureMinute);
                String vorlaufZeitZurMelodie = tagesSuche.getBeginnTime2(benutzerMelodie, futureJahr, futureMonat, futureTag, futureStunde, futureMinute);


                String startStr = tagesSuche.pad(futureStunde) + ":" + tagesSuche.pad(futureMinute);
                StaticVariable.stringInfoTextField[0] = (benutzerMelodie != null ? benutzerMelodie : "") + "  " + startStr;

                //BenutzerMelodienActivity.clearLastMelodieButton();
                esWarBenutzerProgramm = true;
                melodienGefunden = true;

                newSearchAutomaticStart = true; // suche neu starten

            }

        }

            if (countMinutenwechsel <= 0) {
                countMinutenwechsel = UPDATE_EXCEL;
                if (excelread != null) {
                    excelread.closeWorkbook(); // excel tabelle freigeben
                    excelread = null;
                    //Log.e("excelread" , "geschlossen") ;
                }
                System.gc();
                sleepTime(200);
            }
        // if ( futureMinute == 59 && futureStunde == 23 && TurmtechnikActivity.automaticOn == true)
        if (futureMinute == 00 && futureStunde == 00 && TurmtechnikActivity.flagAutomaticOnOff == true) {
            //TurmtechnikActivity.stringInfoText = "nächster Start --> morgen" ;
            if ((melodienGefunden == false)) {
                StaticVariable.stringInfoTextField[0] = StaticVariable.getUebersetzung(9) ; //("nächster Start --> morgen");

                StaticVariable.vorschwingenStartzeitenMotorRelais.clear();


            }
            if ((heizungGefunden == false)) {
                StaticVariable.infoStringHeizung = "";
                StaticVariable.nextHeizungStartSekunden = 0;
            }

            newSearchAutomaticStart = true;

            if (countMinutenwechsel <= 0) {
                countMinutenwechsel = UPDATE_EXCEL;
                if (excelread != null) {
                    excelread.closeWorkbook(); // excel tabelle freigeben
                    excelread = null;
                    //Log.e("excelread" , "geschlossen") ;
                }
                System.gc();
                sleepTime(200);
            }

        }

//		Log.d("futureStunde" , "= " +futureStunde);
//		Log.d("futureMinute" , "= " +futureMinute);
//		Log.d("futureSekunde" , "= " +futureSekunde);

        if (aktuelleZeitZeilenListe.size() > 0)

        {
            Log.w("ProgrammStart", "Liste size=" + aktuelleZeitZeilenListe.size() + " Zeilen=" + aktuelleZeitZeilenListe.toString() + " dbModeExecuteThisRun=" + dbModeExecuteThisRun);
            // Bei dbModeExecuteThisRun wurde melodienGefunden ggf. schon im Benutzerprogramm-Anzeige-Block (checkStartBenutzerMelodie) gesetzt.
            // Damit die Melodie für die Liste [1002] trotzdem gestartet wird, hier zurücksetzen.
            if (dbModeExecuteThisRun) {
                melodienGefunden = false;
            }
            prognoseModus = true;
            sucheBlockNummer();
            prognoseModus = false;

            if (zeilenNummer == -1) {
                Log.w("ProgrammStart", "zeileOk für alle Einträge FALSE -> zeilenNummer=-1 (Programm startet NICHT; prüfe TagesSuche.zeileOk / programmOkFromDatabase)");
            } else {
                Log.w("ProgrammStart", "zeilenNummer=" + zeilenNummer + " (wird verarbeitet)");
            }

            if (zeilenNummer != -1) {
                //Log.e("zeilenNummer", "=" + zeilenNummer) ;

                String melodieName[] = tagesSuche.getMelodieName(zeilenNummer); // [0] = MelodieName oder zeit fuer Heizung
                // [1] = funktionName z.B Melodie oder Heizung
                // [2] = spalte immer entweder "x" oder nichts
                if (melodieName == null || melodieName.length < 3) {
                    Log.w("ProgrammStart", "getMelodieName lieferte ungültiges Array für Zeile " + zeilenNummer + " – überspringe Start.");
                    melodienGefunden = true;
                } else {
                Log.w("ProgrammStart", "getMelodieName Zeile " + zeilenNummer + ": name='" + (melodieName[0] != null ? melodieName[0] : "null") + "' funktion='" + (melodieName[1] != null ? melodieName[1] : "null") + "' spalte='" + (melodieName.length > 2 && melodieName[2] != null ? melodieName[2] : "null") + "' melodienGefunden=" + melodienGefunden);

                String pathAndFileName = TurmtechnikActivity.sdCardPath + "/Turmtechnik/Melodien/" + (melodieName[0] != null ? melodieName[0] : "");

                String benutzerMelodieMitStartZeit = tagesSuche.pad(futureStunde) + ":" + tagesSuche.pad(futureMinute) + " " + (melodieName[0] != null ? melodieName[0] : "");

                // Null-sicher: "Melodie".equals(melodieName[1]) statt melodieName[1].equals("Melodie")
                if ("Melodie".equals(melodieName[1]) && (melodienGefunden == false)) {

                    long vorlaufZeitTemp = (tagesSuche.getErsteVorlaufzeitMs(melodieName[0]));
                    Date dateStartMelodie = new Date(futureJahr - 1900, futureMonat, futureTag, futureStunde, futureMinute);
                    long msStartMelode = dateStartMelodie.getTime();

                    long beginntimeTemp = msStartMelode - vorlaufZeitTemp;

                    long momentanMs = (calendar.getTimeInMillis());
                    Log.w("ProgrammStart", "Melodie-Zweig: zeile=" + zeilenNummer + " name=" + melodieName[0]
                            + " momentanMs=" + momentanMs + " beginntimeTemp=" + beginntimeTemp + " msStartMelode=" + msStartMelode
                            + " (momentan<beginn? " + (momentanMs < beginntimeTemp) + " momentan<=start? " + (momentanMs <= msStartMelode) + ")");
                    //Log.e("uhrTest1", "momentanMs=" + tagesSuche.convertMsToTimeString(momentanMs));

                    if (momentanMs < beginntimeTemp) {
                        tagesSuche.setVorschwingZeitenZurMelodie(benutzerMelodieMitStartZeit, pathAndFileName, futureJahr, futureMonat, futureTag, futureStunde, futureMinute);

                        StaticVariable.stringInfoTextField[0] = tagesSuche.getInfoText2(zeilenNummer, futureJahr, futureMonat, futureTag, futureStunde, futureMinute);

                        //Log.e("stringInfoTextField[0]", "=" + StaticVariable.stringInfoTextField[0]) ;

                        melodienGefunden = true;
                        melodieVorbeginnTimeGefunden = true;
                        if (!(melodieName[2].equals("x")))  // spalte immer checken
                        {
                            StaticVariable.turnOffVerknuepft4 = tagesSuche.getNurEinmalOffset();
                        }
                    } else {
                        // Beginn-Zeit bereits erreicht oder überschritten → Vorschwingen setzen und Melodie starten
                        if (momentanMs <= msStartMelode) {
                            tagesSuche.setVorschwingZeitenZurMelodie(benutzerMelodieMitStartZeit, pathAndFileName, futureJahr, futureMonat, futureTag, futureStunde, futureMinute);
                            if (!(melodieName[2].equals("x"))) {
                                StaticVariable.turnOffVerknuepft4 = tagesSuche.getNurEinmalOffset();
                            }
                            // Immer den Pfad der aktuell auszuführenden Zeile verwenden – pathAndFileNameNextMelodie
                            // wurde ggf. schon von findNextProgrammeFromDatabase() mit dem nächsten Programm überschrieben.
                            // Bei leerem Melodienamen (z. B. Benutzer-Slot nicht belegt) nicht starten und nicht pathAndFileNameNextMelodie verwenden
                            String pathToStart;
                            if (melodieName[0] == null || melodieName[0].trim().isEmpty()) {
                                pathToStart = null;
                            } else {
                                pathToStart = (pathAndFileName != null && !pathAndFileName.isEmpty())
                                        ? (pathAndFileName.toLowerCase().endsWith(".xls") ? pathAndFileName : pathAndFileName + ".xls")
                                        : (StaticVariable.pathAndFileNameNextMelodie != null && !StaticVariable.pathAndFileNameNextMelodie.isEmpty() ? StaticVariable.pathAndFileNameNextMelodie : null);
                            }
                            boolean kannStarten = (StaticVariable.melodieThreadNew == null && pathToStart != null && !pathToStart.isEmpty());
                            boolean nichtGestoppt = (StaticVariable.pathStoppedByUser == null || StaticVariable.pathStoppedByUser.isEmpty()
                                    || !StaticVariable.pathStoppedByUser.equals(pathToStart));
                            Log.w("ProgrammStart", "Start-Versuch: pathToStart=" + pathToStart + " melodieThreadNew==null? " + (StaticVariable.melodieThreadNew == null)
                                    + " kannStarten? " + kannStarten + " nichtGestoppt? " + nichtGestoppt);
                            if (kannStarten && nichtGestoppt) {
                                synchronized (MELODIE_START_LOCK) {
                                    if (StaticVariable.melodieThreadNew == null) {
                                        StaticVariable.melodieThreadNew = new MelodieThreadNew(pathToStart, msStartMelode);
                                        StaticVariable.melodieThreadNew.start();
                                    }
                                }
                                blockReady = 2;
                                Log.w("ProgrammStart", "Melodie GESTARTET: " + pathToStart);
                                StaticVariable.stringInfoTextField[0] = (StaticVariable.nameNextMelodie != null ? ("momentan Aktiv: " + StaticVariable.nameNextMelodie) : tagesSuche.getInfoText2(zeilenNummer, futureJahr, futureMonat, futureTag, futureStunde, futureMinute));
                            } else if (!kannStarten || !nichtGestoppt) {
                                Log.w("ProgrammStart", "Melodie NICHT gestartet: kannStarten=" + kannStarten + " nichtGestoppt=" + nichtGestoppt);
                            }
                            melodienGefunden = true;
                        }
                        if (!melodienGefunden) {
                            if ((momentanMs <= msStartMelode) && (melodieVorbeginnTimeGefunden)) {
                                melodienGefunden = true;
                            } else {
                                melodieVorbeginnTimeGefunden = false;
                            }
                        }
                    }

                } else {
                    Log.w("ProgrammStart", "Melodie-Zweig nicht betreten: funktion='" + (melodieName[1] != null ? melodieName[1] : "null") + "' (erwartet Melodie) melodienGefunden=" + melodienGefunden);
                }
                if (!"Melodie".equals(melodieName[1]) && (heizungGefunden == false)) {
                    // bei heizung:
                    //tagesSuche.clrVorschwingZeitenZurMelodie() ;
                    //StaticVariable.vorschwingenStartzeitenMotorRelais.clear();
                    if (StaticVariable.heizungEingeschaltetTimer == 0)
                    {
                        tagesSuche.setHeizungStartSekunden(melodieName[1], melodieName[0], futureJahr, futureMonat, futureTag, futureStunde, futureMinute);
                        heizungGefunden = true;
                        StaticVariable.infoStringHeizung = tagesSuche.pad(futureStunde) + ":" + tagesSuche.pad(futureMinute) + " " + melodieName[1] + " " + formatHHmmSS(melodieName[0]) ; // + " Minuten";
                    }
                }

                if (melodienGefunden && heizungGefunden) {
                    newSearchAutomaticStart = true; // suche neu starten
                }
                if (excelread == null && zeilenNummer != -1) {
                    newSearchAutomaticStart = true;
                    nextProgrammZeile = -1;
                    nextProgrammBeginnMs = -1;
                    nextProgrammBeginnMsPublic = -1;
                }

                if (countMinutenwechsel <= 0) {
                    countMinutenwechsel = UPDATE_EXCEL;
                    if (excelread != null) {
                        excelread.closeWorkbook(); // excel tabelle freigeben
                        excelread = null;
                        //Log.e("excelread" , "geschlossen") ;
                    }
                    System.gc();
                    sleepTime(100);
                }

                } // Ende else (melodieName gültig)
            } else  // sonst weiter suchen
            {
                // Log.d("sonst" , "weiter suchen");
                //for (int i = 0; i < 3 ; i++)
                //{
                //	incrementFutureTime[i]=true;
                //}

            }
        }
        if (excelread != null) {
            incrementSearchTime();
        }
    }

    /**
     * Prüft ob die aktuelle Uhrzeit (calendar) >= Programmstart (stunde:minute) ist.
     * Ermöglicht Nachholen eines Programms, wenn es um die exakte Minute verpasst wurde (z.B. blockReady != 0).
     */
    private boolean isCurrentTimeAtOrPast(int stunde, int minute) {
        int currentStunde = calendar.get(Calendar.HOUR_OF_DAY);
        int currentMinute = calendar.get(Calendar.MINUTE);
        return currentStunde > stunde || (currentStunde == stunde && currentMinute >= minute);
    }

    /**
     * Sucht einmal das nächste Programm des Tages (Startzeit >= aktuelle Zeit) in der DB,
     * setzt nextProgrammZeile/Stunde/Minute und den Infotext.
     * Wird im DB-Modus nur bei newSearchAutomaticStart aufgerufen, nicht jede Sekunde.
     */
    /** Zeilen-Offset für Benutzerprogramm-Slots: nextProgrammZeile = BENUTZERPROGRAMM_ZEILE_OFFSET + slotIndex (0..19). */
    private static final int BENUTZERPROGRAMM_ZEILE_OFFSET = 1000;

    private void findNextProgrammeFromDatabase() {
        nextProgrammZeile = -1;
        nextProgrammStunde = -1;
        nextProgrammMinute = -1;
        nextProgrammBeginnMs = -1;
        nextProgrammBeginnMsPublic = -1;
        if (tagesSuche == null || excelTableFileName == null || excelTableFileName.isEmpty()) {
            return;
        }
        try {
            // Kandidat: nächstes Benutzerprogramm für heute (wird später mit Programmtag verglichen)
            long candidateBenutzerBeginnMs = Long.MAX_VALUE;
            int candidateBenutzerSlot = -1;
            int candidateBenutzerStunde = 25;
            int candidateBenutzerMinute = 61;
            android.content.Context ctx = TurmtechnikActivity.turmtechnikContext;
            if (ctx != null) {
                TurmtechnikActivity.loadBenutzerprogrammeFromDb(ctx);
            }
            int size = StaticVariable.benutzerMelodieTasteOn2.size();
            for (int i = 0; i < size; i++) {
                if (i >= StaticVariable.tage.size() || i >= StaticVariable.monate.size() || i >= StaticVariable.jahre.size()
                        || i >= StaticVariable.stunden.size() || i >= StaticVariable.minuten.size()) continue;
                if (!Boolean.TRUE.equals(StaticVariable.benutzerMelodieTasteOn2.get(i))) continue;
                if (StaticVariable.tage.get(i) != futureTag) continue;
                if (StaticVariable.monate.get(i) != (futureMonat + 1)) continue;
                if (StaticVariable.jahre.get(i) != futureJahr) continue;
                int stunde = StaticVariable.stunden.get(i);
                int minute = StaticVariable.minuten.get(i);
                // Nur Programmtag (ganzer Tag): keine Uhrzeit – Slot nicht als „nächstes Programm“ mit Melodie
                if (stunde < 0 || minute < 0) continue;
                long startMs = new Date(futureJahr - 1900, futureMonat, futureTag, stunde, minute, 0).getTime();
                String melodieName = BenutzerMelodienActivity.getMelodieNameForSlot(i);
                long vorlaufMs = (tagesSuche != null && melodieName != null && !melodieName.isEmpty())
                        ? tagesSuche.getErsteVorlaufzeitMs(melodieName) : 0;
                long beginnMs = startMs - vorlaufMs;
                if (beginnMs <= calendar.getTimeInMillis()) continue;
                if (beginnMs < candidateBenutzerBeginnMs) {
                    candidateBenutzerBeginnMs = beginnMs;
                    candidateBenutzerSlot = i;
                    candidateBenutzerStunde = stunde;
                    candidateBenutzerMinute = minute;
                }
            }

            android.content.Context context = TurmtechnikActivity.turmtechnikContext;
            if (context == null) {
                return;
            }
            PlatinenDatabaseHelper dbHelper = PlatinenDatabaseHelper.getInstance(context);
            java.util.List<Programm> programme = dbHelper.getProgrammeByTagtyp(excelTableFileName);
            if (programme == null || programme.isEmpty()) {
                String alt = (excelTableFileName != null && excelTableFileName.toLowerCase().endsWith(".xls"))
                    ? excelTableFileName.substring(0, excelTableFileName.length() - 4)
                    : excelTableFileName + ".xls";
                programme = dbHelper.getProgrammeByTagtyp(alt);
            }
            if (programme == null || programme.isEmpty()) {
                // Kein Programmtag – nur Benutzerprogramm-Kandidat nutzen, falls vorhanden
                if (candidateBenutzerSlot >= 0) {
                    nextProgrammZeile = BENUTZERPROGRAMM_ZEILE_OFFSET + candidateBenutzerSlot;
                    nextProgrammStunde = candidateBenutzerStunde;
                    nextProgrammMinute = candidateBenutzerMinute;
                    nextProgrammBeginnMs = candidateBenutzerBeginnMs;
                    nextProgrammBeginnMsPublic = candidateBenutzerBeginnMs;
                    String melodieName = BenutzerMelodienActivity.getMelodieNameForSlot(candidateBenutzerSlot);
                    String startStr = (tagesSuche != null ? tagesSuche.pad(candidateBenutzerStunde) : String.format("%02d", candidateBenutzerStunde)) + ":" + (tagesSuche != null ? tagesSuche.pad(candidateBenutzerMinute) : String.format("%02d", candidateBenutzerMinute));
                    StaticVariable.stringInfoTextField[0] = (melodieName != null ? melodieName : "") + "  " + startStr;
                    if (melodieName != null && !melodieName.isEmpty()) {
                        String pathAndFileName = makePathAndFilenameMelodieOhneXls(melodieName);
                        String benutzerMelodieMitStartZeit = startStr + " " + melodieName;
                        tagesSuche.setVorschwingZeitenZurMelodie(benutzerMelodieMitStartZeit, pathAndFileName, futureJahr, futureMonat, futureTag, candidateBenutzerStunde, candidateBenutzerMinute);
                    }
                } else {
                    nextProgrammBeginnMsPublic = -1;
                    StaticVariable.stringInfoTextField[0] = (StaticVariable.getUebersetzung(9) != null ? StaticVariable.getUebersetzung(9) : "nächster Start --> morgen");
                }
                return;
            }

            long currentMs = calendar.getTimeInMillis();
            long bestBeginnMs = Long.MAX_VALUE;
            int bestStunde = 25;
            int bestMinute = 61;
            Programm bestProgramm = null;

            for (Programm programm : programme) {
                String startzeit = programm.getStartzeit();
                if (startzeit == null || startzeit.trim().isEmpty()) {
                    continue;
                }
                int stunde;
                int minute;
                if (startzeit.contains(":")) {
                    String[] zeitSplit = startzeit.split(":");
                    try {
                        stunde = Integer.parseInt(zeitSplit[0].trim());
                        minute = zeitSplit.length > 1 ? Integer.parseInt(zeitSplit[1].trim()) : 0;
                        // Minute >= 60 normalisieren (z. B. "18:61" → 19:01), sonst Anzeige "18:61" obwohl um 19:00 geläutet wird
                        if (minute >= 60) {
                            stunde += minute / 60;
                            minute = minute % 60;
                        }
                        if (minute < 0) minute = 0;
                    } catch (NumberFormatException e) {
                        continue;
                    }
                } else if ("SA".equals(startzeit)) {
                    if (StaticVariable.sonnenAufgangStringGerundet == null || StaticVariable.sonnenAufgangStringGerundet.isEmpty()) {
                        continue;
                    }
                    String[] zeitSplit = StaticVariable.sonnenAufgangStringGerundet.split(":");
                    try {
                        stunde = Integer.parseInt(zeitSplit[0].trim());
                        minute = zeitSplit.length > 1 ? Integer.parseInt(zeitSplit[1].trim()) : 0;
                        if (minute >= 60) { stunde += minute / 60; minute = minute % 60; }
                        if (minute < 0) minute = 0;
                    } catch (NumberFormatException e) {
                        continue;
                    }
                } else if ("SU".equals(startzeit)) {
                    if (StaticVariable.sonnenUntergangStringGerundet == null || StaticVariable.sonnenUntergangStringGerundet.isEmpty()) {
                        continue;
                    }
                    String[] zeitSplit = StaticVariable.sonnenUntergangStringGerundet.split(":");
                    try {
                        stunde = Integer.parseInt(zeitSplit[0].trim());
                        minute = zeitSplit.length > 1 ? Integer.parseInt(zeitSplit[1].trim()) : 0;
                        if (minute >= 60) { stunde += minute / 60; minute = minute % 60; }
                        if (minute < 0) minute = 0;
                    } catch (NumberFormatException e) {
                        continue;
                    }
                } else {
                    continue;
                }

                if (stunde > 23) {
                    stunde = stunde % 24;
                }
                if (stunde < 0) stunde = 0;

                if (!tagesSuche.programmOkFromDatabase(programm, futureJahr, futureMonat, futureTag)) {
                    continue;
                }

                // Beginn = Startzeit minus Vorlauf (getErsteVorlaufzeitMs; für Melodie). Vorschwingen ist getrennt (pro Glocke in Sek.).
                long startMs = new Date(futureJahr - 1900, futureMonat, futureTag, stunde, minute, 0).getTime();
                long vorlaufMs = 0;
                if ("Melodie".equals(programm.getFunktion())) {
                    String melName = programm.getMelodieName();
                    if (melName != null && !melName.trim().isEmpty()) {
                        vorlaufMs = tagesSuche.getErsteVorlaufzeitMs(melName);
                    }
                }
                long beginnMs = startMs - vorlaufMs;
                if (beginnMs <= currentMs) {
                    continue;
                }
                // Gleicher Zeitpunkt (Std:MIN): nach Priorität wählen (höhere Priorität = höherer Wert 1–9)
                boolean besser = beginnMs < bestBeginnMs
                    || (bestProgramm != null && beginnMs == bestBeginnMs && programm.getPrioritaet() > bestProgramm.getPrioritaet());
                if (besser) {
                    bestBeginnMs = beginnMs;
                    bestStunde = stunde;
                    bestMinute = minute;
                    bestProgramm = programm;
                }
            }

            // Früheren von Benutzerprogramm-Kandidat und Programmtag wählen. Bei gleichem Zeitpunkt: Benutzerprogramm hat Vorrang.
            // (Sofortstart wird in checkNextAutomaticStart zuerst geprüft und hat damit höchste Priorität.)
            if (candidateBenutzerSlot >= 0 && (bestProgramm == null || candidateBenutzerBeginnMs <= bestBeginnMs)) {
                Log.w("ProgrammStart", "findNext: gewählt BENUTZERPROGRAMM Slot=" + candidateBenutzerSlot + " Zeile=" + (BENUTZERPROGRAMM_ZEILE_OFFSET + candidateBenutzerSlot) + " BeginnMs=" + candidateBenutzerBeginnMs);
                nextProgrammZeile = BENUTZERPROGRAMM_ZEILE_OFFSET + candidateBenutzerSlot;
                nextProgrammStunde = candidateBenutzerStunde;
                nextProgrammMinute = candidateBenutzerMinute;
                nextProgrammBeginnMs = candidateBenutzerBeginnMs;
                nextProgrammBeginnMsPublic = candidateBenutzerBeginnMs;
                String melodieName = BenutzerMelodienActivity.getMelodieNameForSlot(candidateBenutzerSlot);
                String startStr = (tagesSuche != null ? tagesSuche.pad(candidateBenutzerStunde) : String.format("%02d", candidateBenutzerStunde)) + ":" + (tagesSuche != null ? tagesSuche.pad(candidateBenutzerMinute) : String.format("%02d", candidateBenutzerMinute));
                StaticVariable.stringInfoTextField[0] = (melodieName != null ? melodieName : "") + "  " + startStr;
                if (melodieName != null && !melodieName.isEmpty()) {
                    String pathAndFileName = makePathAndFilenameMelodieOhneXls(melodieName);
                    String benutzerMelodieMitStartZeit = startStr + " " + melodieName;
                    tagesSuche.setVorschwingZeitenZurMelodie(benutzerMelodieMitStartZeit, pathAndFileName, futureJahr, futureMonat, futureTag, candidateBenutzerStunde, candidateBenutzerMinute);
                }
            } else if (bestProgramm != null) {
                Log.w("ProgrammStart", "findNext: gewählt NORMALPROGRAMM id=" + bestProgramm.getId() + " Zeile=" + (bestProgramm.getId() + 3) + " BeginnMs=" + bestBeginnMs + " " + bestStunde + ":" + bestMinute);
                nextProgrammZeile = bestProgramm.getId() + 3;
                nextProgrammStunde = bestStunde;
                nextProgrammMinute = bestMinute;
                nextProgrammBeginnMs = bestBeginnMs;
                nextProgrammBeginnMsPublic = bestBeginnMs;
                StaticVariable.stringInfoTextField[0] = tagesSuche.getInfoText2(nextProgrammZeile, futureJahr, futureMonat, futureTag, bestStunde, bestMinute);
                if ("Melodie".equals(bestProgramm.getFunktion())) {
                    String melName = bestProgramm.getMelodieName();
                    if (melName != null && !melName.trim().isEmpty()) {
                        String pathAndFileName = makePathAndFilenameMelodieOhneXls(melName);
                        String benutzerMelodieMitStartZeit = tagesSuche.pad(bestStunde) + ":" + tagesSuche.pad(bestMinute) + " " + melName;
                        tagesSuche.setVorschwingZeitenZurMelodie(benutzerMelodieMitStartZeit, pathAndFileName, futureJahr, futureMonat, futureTag, bestStunde, bestMinute);
                    }
                }
            } else {
                Log.w("ProgrammStart", "findNext: KEIN Programm (bestProgramm=null, candidateBenutzerSlot=" + candidateBenutzerSlot + ") -> nächster Start morgen");
                nextProgrammBeginnMsPublic = -1;
                // Morgen + erstes Programm des nächsten Tages anzeigen
                Calendar morgenCal = Calendar.getInstance();
                morgenCal.set(futureJahr, futureMonat, futureTag);
                morgenCal.add(Calendar.DAY_OF_MONTH, 1);
                int morgenJahr = morgenCal.get(Calendar.YEAR);
                int morgenMonat = morgenCal.get(Calendar.MONTH);
                int morgenTag = morgenCal.get(Calendar.DAY_OF_MONTH);
                String morgenInfo = getFirstProgrammeInfoTextForDay(programme, morgenJahr, morgenMonat, morgenTag);
                if (morgenInfo != null && !morgenInfo.isEmpty()) {
                    // Immer fest "Morgen" verwenden – Index 32 ist in der Sprachdatei für Periodizität ("alle 2 Wochen" etc.) belegt
                    StaticVariable.stringInfoTextField[0] = "Morgen um " + morgenInfo;
                } else {
                    StaticVariable.stringInfoTextField[0] = (StaticVariable.getUebersetzung(9) != null ? StaticVariable.getUebersetzung(9) : "nächster Start --> morgen");
                }
            }
        } catch (Exception e) {
            Log.e("ProgrammStart", "findNextProgrammeFromDatabase: Fehler", e);
            Log.e(sourceFileName, "findNextProgrammeFromDatabase: Fehler", e);
            nextProgrammBeginnMsPublic = -1;
            StaticVariable.stringInfoTextField[0] = (StaticVariable.getUebersetzung(9) != null ? StaticVariable.getUebersetzung(9) : "nächster Start --> morgen");
        }
    }

    /**
     * Ermittelt den Anzeigetext (Zeit + Name) des ersten Programms eines gegebenen Tages aus der Programmliste.
     * Für die Anzeige "Morgen – erstes Programm des nächsten Tages" wenn heute kein Programm mehr in der Liste steht.
     */
    private String getFirstProgrammeInfoTextForDay(java.util.List<Programm> programme, int jahr, int monat, int tag) {
        if (programme == null || programme.isEmpty() || tagesSuche == null) return null;
        int bestStunde = 25;
        int bestMinute = 61;
        Programm bestProgramm = null;
        for (Programm programm : programme) {
            String startzeit = programm.getStartzeit();
            if (startzeit == null || startzeit.trim().isEmpty()) continue;
            int stunde;
            int minute;
            if (startzeit.contains(":")) {
                String[] zeitSplit = startzeit.split(":");
                try {
                    stunde = Integer.parseInt(zeitSplit[0].trim());
                    minute = zeitSplit.length > 1 ? Integer.parseInt(zeitSplit[1].trim()) : 0;
                    if (minute >= 60) { stunde += minute / 60; minute = minute % 60; }
                    if (minute < 0) minute = 0;
                } catch (NumberFormatException e) { continue; }
            } else if ("SA".equals(startzeit)) {
                if (StaticVariable.sonnenAufgangStringGerundet == null || StaticVariable.sonnenAufgangStringGerundet.isEmpty()) continue;
                String[] zeitSplit = StaticVariable.sonnenAufgangStringGerundet.split(":");
                try {
                    stunde = Integer.parseInt(zeitSplit[0].trim());
                    minute = zeitSplit.length > 1 ? Integer.parseInt(zeitSplit[1].trim()) : 0;
                    if (minute >= 60) { stunde += minute / 60; minute = minute % 60; }
                    if (minute < 0) minute = 0;
                } catch (NumberFormatException e) { continue; }
            } else if ("SU".equals(startzeit)) {
                if (StaticVariable.sonnenUntergangStringGerundet == null || StaticVariable.sonnenUntergangStringGerundet.isEmpty()) continue;
                String[] zeitSplit = StaticVariable.sonnenUntergangStringGerundet.split(":");
                try {
                    stunde = Integer.parseInt(zeitSplit[0].trim());
                    minute = zeitSplit.length > 1 ? Integer.parseInt(zeitSplit[1].trim()) : 0;
                    if (minute >= 60) { stunde += minute / 60; minute = minute % 60; }
                    if (minute < 0) minute = 0;
                } catch (NumberFormatException e) { continue; }
            } else continue;
            if (stunde > 23) stunde = stunde % 24;
            if (stunde < 0) stunde = 0;
            if (!tagesSuche.programmOkFromDatabase(programm, jahr, monat, tag)) continue;
            if (stunde < bestStunde || (stunde == bestStunde && minute < bestMinute)
                    || (bestProgramm != null && stunde == bestStunde && minute == bestMinute && programm.getPrioritaet() > bestProgramm.getPrioritaet())) {
                bestStunde = stunde;
                bestMinute = minute;
                bestProgramm = programm;
            }
        }
        if (bestProgramm == null) return null;
        return tagesSuche.formatMorgenUmProgramm(bestProgramm, bestStunde, bestMinute);
    }

    private String formatHHmmSS(String minutenString)
    {
        long minutenIntMs = (Integer.parseInt(minutenString)) * 60 * 1000 ;

        SimpleDateFormat sdf = new SimpleDateFormat(":mm:ss") ;
        sdf.setTimeZone(TimeZone.getDefault());

        long hours = minutenIntMs / (60L * 60L * 1000L) ;

        return hours + sdf.format(minutenIntMs) ;


    }

    private void dumpVerknuepft() {
        //Log.i("dumpVerknuepft", "=");
        for (int i = 0; i < 24; i++) {
            //Log.i("dumpVerknuepft", "i=" + i + " --> " + TurmtechnikActivity.verknuepfteTastenOn[i]);
        }
    }

    private String makePathAndFilenameMelodieOhneXls(String MelodieName) {
        return TurmtechnikActivity.sdCardPath + "/Turmtechnik/Melodien/" + MelodieName;
    }

    private void incrementSearchTime() {
        // Log.d("incremet" , "serach time");

        //if (futureMinute==59 && futureStunde==23)
        if (futureMinute == 00 && futureStunde == 00) {
            return; // bei 23:59 stehen bleiben
        }

        //if (incrementFutureTime[2]==true)
        //{
//			futureSekunde ++ ;
//			if (futureSekunde > 59)
//			{
//				futureSekunde = 0 ;
//			}
//		}	
//		
//		if (incrementFutureTime[2]==false && incrementFutureTime[1]==true)
//		{ 
        futureMinute++;
        if (futureMinute > 59) {
            futureMinute = 0;
            futureStunde++;
            if (futureStunde > 23) {
                futureStunde = 0;
            }
        }


//		}

//		if (incrementFutureTime[1]==false && incrementFutureTime[0]==true)
//		{
//			futureStunde ++ ;
//			if (futureStunde > 23)
//	     	{
//					futureStunde = 0 ; 
//	     	}
//		}

    }

    private void makeZeitenZeilenFuture() {
        if ((blockReady != 0) || (TurmtechnikActivity.flagAutomaticOnOff == false)) {
            return;
        }
        
        // Wenn excelread null ist, verwende Datenbank
        if (excelread == null) {
            makeZeitenZeilenFutureFromDatabase();
            return;
        }
        
        zeile = 3; // beginne bei der ersten zeit zelle in programmliste

        //Log.e("vor" , "get zeit");

        //int returnCode = 0 ;
        int zeilen = excelread.getCellZeilen();

        while (zeile < zeilen) {
            zeitStringFromProgrammliste = getZeit(zeile);
            if (zeitStringFromProgrammliste.equals("")) {
                break; //auf leerzeile getroffenn == ende der Tabelle
            }
            //Log.e("zeit=" , "" + zeit);

            String[] zeitSplit = {"25", "61"}; // default == unsinnige zeit

            if (zeitStringFromProgrammliste.contains(":")) {
                zeitSplit = zeitStringFromProgrammliste.split(":");
            } else {
                // 1.6.16 nach Sonnenaufgang und Sonntenuntergang pruefen --> SA/SU

                if (zeitStringFromProgrammliste.equals("SA")) // SonnenAufgang
                {
                    zeitSplit = StaticVariable.sonnenAufgangStringGerundet.split(":");
                }

                if (zeitStringFromProgrammliste.equals("SU")) // SonnenAufgang
                {
                    zeitSplit = StaticVariable.sonnenUntergangStringGerundet.split(":");
                }
            }

            int programmlisteMinute = Integer.parseInt(zeitSplit[1]);

            if (futureMinute == programmlisteMinute) {
                //    		 Log.d("minute", "future passt");
                //    		 incrementFutureTime[1]=false ;

                int programmlisteStunde = Integer.parseInt(zeitSplit[0]);
                if (programmlisteStunde > 23) {
                    programmlisteStunde = calendar.get(Calendar.HOUR_OF_DAY);
                }

                if (futureStunde == programmlisteStunde) {

                    //  			Log.d("stunde", "future passt");
                    // Stunde, Minute und Sekunde passt
                    aktuelleZeitZeilenListe.add(zeile);
                }
            }

            zeile++;
        }
    }
    
    /**
     * Erstellt die aktuelleZeitZeilenListe aus Datenbank-Programmen.
     * Wird verwendet, wenn excelread null ist (Programme kommen aus DB).
     */
    private void makeZeitenZeilenFutureFromDatabase() {
        if (tagesSuche == null || excelTableFileName == null || excelTableFileName.isEmpty()) {
            Log.w(sourceFileName, "makeZeitenZeilenFutureFromDatabase: tagesSuche oder excelTableFileName ist null/leer");
            return;
        }
        
        aktuelleZeitZeilenListe.clear();
        
        // Stelle sicher, dass calendar initialisiert ist
        if (calendar == null) {
            calendar = Calendar.getInstance();
            Log.d(sourceFileName, "makeZeitenZeilenFutureFromDatabase: calendar initialisiert");
        }
        
        try {
            android.content.Context context = TurmtechnikActivity.turmtechnikContext;
            if (context == null) {
                Log.w(sourceFileName, "makeZeitenZeilenFutureFromDatabase: Context ist null");
                return;
            }
            
            PlatinenDatabaseHelper dbHelper = PlatinenDatabaseHelper.getInstance(context);
            java.util.List<Programm> programme = dbHelper.getProgrammeByTagtyp(excelTableFileName);
            
            if (programme == null || programme.isEmpty()) {
                Log.d(sourceFileName, "makeZeitenZeilenFutureFromDatabase: Keine Programme in DB für " + excelTableFileName);
                return;
            }
            
            Log.d(sourceFileName, "makeZeitenZeilenFutureFromDatabase: Prüfe " + programme.size() + " Programme für " + excelTableFileName + " (Zeit: " + futureStunde + ":" + futureMinute + ", Datum: " + futureTag + "." + (futureMonat+1) + "." + futureJahr + ")");
            
            // Durchlaufe alle Programme und prüfe, ob die Zeit passt
            for (Programm programm : programme) {
                String startzeit = programm.getStartzeit();
                if (startzeit == null || startzeit.trim().isEmpty()) {
                    continue;
                }
                
                String[] zeitSplit = {"25", "61"}; // default == unsinnige zeit
                
                if (startzeit.contains(":")) {
                    zeitSplit = startzeit.split(":");
                } else {
                    // SA/SU (Sonnenaufgang/Sonnenuntergang)
                    if (startzeit.equals("SA")) {
                        if (StaticVariable.sonnenAufgangStringGerundet == null || StaticVariable.sonnenAufgangStringGerundet.isEmpty()) {
                            continue; // Sonnenaufgang nicht verfügbar
                        }
                        zeitSplit = StaticVariable.sonnenAufgangStringGerundet.split(":");
                    } else if (startzeit.equals("SU")) {
                        if (StaticVariable.sonnenUntergangStringGerundet == null || StaticVariable.sonnenUntergangStringGerundet.isEmpty()) {
                            continue; // Sonnenuntergang nicht verfügbar
                        }
                        zeitSplit = StaticVariable.sonnenUntergangStringGerundet.split(":");
                    } else {
                        continue; // Ungültige Zeit
                    }
                }
                
                try {
                    int programmlisteMinute = Integer.parseInt(zeitSplit[1]);
                    int programmlisteStunde = Integer.parseInt(zeitSplit[0]);
                    if (programmlisteMinute >= 60) {
                        programmlisteStunde += programmlisteMinute / 60;
                        programmlisteMinute = programmlisteMinute % 60;
                    }
                    if (programmlisteMinute < 0) programmlisteMinute = 0;
                    if (programmlisteStunde > 23) {
                        programmlisteStunde = programmlisteStunde % 24;
                    }
                    if (programmlisteStunde < 0) programmlisteStunde = 0;
                    
                    if (futureMinute == programmlisteMinute && futureStunde == programmlisteStunde) {
                        // Zeit passt! Prüfe auch ob Programm für das Datum/Wochentag/Periodisch passt
                        // Verwende programmOkFromDatabase für vollständige Prüfung
                        // WICHTIG: futureMonat ist 0-basiert (Januar=0), programmOkFromDatabase erwartet auch 0-basiert
                        if (tagesSuche.programmOkFromDatabase(programm, futureJahr, futureMonat, futureTag)) {
                            // Zeit passt! Verwende zeile_index als Zeilen-Nummer (entspricht Excel-Zeile)
                            // Excel beginnt bei Zeile 3 (Index 3), DB zeile_index beginnt bei 0
                            // Daher: DB zeile_index 0 = Excel Zeile 3, DB zeile_index 1 = Excel Zeile 4, etc.
                            int excelZeile = programm.getId() + 3; // zeile_index (getId()) + 3 für Excel-Kompatibilität
                            aktuelleZeitZeilenListe.add(excelZeile);
                            Log.d(sourceFileName, "makeZeitenZeilenFutureFromDatabase: Programm gefunden! ID=" + programm.getId() + ", Startzeit=" + startzeit + ", Excel-Zeile=" + excelZeile);
                        } else {
                            Log.d(sourceFileName, "makeZeitenZeilenFutureFromDatabase: Programm ID=" + programm.getId() + " Zeit passt, aber Datum/Wochentag/Periodisch passt nicht");
                        }
                    }
                } catch (NumberFormatException e) {
                    Log.w(sourceFileName, "makeZeitenZeilenFutureFromDatabase: Ungültige Zeit in Programm ID " + programm.getId() + ": " + startzeit);
                }
            }
            
            Log.d(sourceFileName, "makeZeitenZeilenFutureFromDatabase: " + aktuelleZeitZeilenListe.size() + " passende Programme gefunden");
        } catch (Exception e) {
            Log.e(sourceFileName, "Fehler in makeZeitenZeilenFutureFromDatabase", e);
        }
    }

	/*
    private String getInfoText(int zeile)
	{
		String infoText ;
		
		//InfoText = "nächster Automatik Start -->   " ;
		infoText = "" ;
		// neu 24.09.2014 wegen Startzeit z.B. 18:29 text aus Tabelle spalte 18 holen 
		// am 18.01.2015 wieder ausgebaut
        //try
		//{
			
		//	infoText += (excelread.getCellString(18, zeile)) ;
		//	if( ! (infoText.equals("")) )
		//	{
		//		return infoText ;
		//	}
		//}
		//catch (ArrayIndexOutOfBoundsException aiobe)
		//{
			// wenn es nicht geht normal weitermachen
		//	Log.e("ArrayOutOfBounds" , "Exception") ;
		//} catch (Exception e) {
		//	Log.e("getInfoText" , "Exception") ;
		//}
		
		try {
			infoText = (excelread.getCellString(SPALTE_A_STARTZEIT , zeile))  + "  ";
		} catch (ArrayIndexOutOfBoundsException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			new LogExcelError(0, zeile, excelTableFileName, 0,sourceFileName,509 ) ;
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			new LogExcelError(0, zeile, excelTableFileName, 0,sourceFileName,513 ) ;
		}
		try {
			infoText += excelread.getCellString(SPALTE_C_MELODIE_NAME, zeile);
		} catch (ArrayIndexOutOfBoundsException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			new LogExcelError(1, zeile, excelTableFileName, 0,sourceFileName,520 ) ;
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			new LogExcelError(1, zeile, excelTableFileName, 0 ,sourceFileName,524) ;
		}
		return infoText ;
	}

	*/

    /*
	private String getBlockMomentanText(int zeile)
	{
		String InfoText ;
		InfoText = "momentan aktiv: " ;
		try {
			InfoText += (excelread.getCellString(SPALTE_A_STARTZEIT, zeile)) + "  ";
            InfoText += (excelread.getCellString(SPALTE_B_FUNKTION_NAME, zeile)) + " " ;
		} catch (ArrayIndexOutOfBoundsException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			new LogExcelError(0, zeile, excelTableFileName, 0,sourceFileName,538 ) ;
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			new LogExcelError(0, zeile, excelTableFileName, 0 ,sourceFileName,542) ;
		}
		try {
			InfoText += excelread.getCellString(SPALTE_C_MELODIE_NAME, zeile);
		} catch (ArrayIndexOutOfBoundsException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			new LogExcelError(1, zeile, excelTableFileName, 0 ,sourceFileName,549) ;
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			new LogExcelError(1, zeile, excelTableFileName, 0 ,sourceFileName,553) ;
		}
		return InfoText ;
	}
	*/

	/*
	private boolean checkTime()
	{
		if(excelread == null)
		{
			return false ; // noch keine tabelle gefunden!!
		}
		zeilenNummer = -1 ;
		
	    aktuelleZeitZeilenListe.clear();
		makeZeitenZeilenListe();
	
		sucheBlockNummer();
				
	    if (zeilenNummer != -1)
	    {
	    
	    	//printVerknuepfteTasten();
	    	return true ;
	    }
	    else 
	    {	
	    	return false ; 
	    }			
	}
	*/

    private void printVerknuepfteTasten() {
        if (verknuepfteTastenString.size() > 0) {
            for (int i = 0; i < verknuepfteTastenString.size(); i++) {
                // Log.i("verkn. Taste" , "=" + verknuepfteTastenString.elementAt(i));
                // Log.i("verkn. Taste" , "on=" + TurmtechnikActivity.verknuepfteTastenOn[i]);
            }
        } else {
            // Log.i("verkn. Tasten" , ("nicht gewaehlt"));
        }
    }

    private void sucheBlockNummer() {
        zeilenNummer = -1;

//		aktuelleZeitZeilenListe.clear();
//		makeZeitenZeilenListe();

        if (aktuelleZeitZeilenListe.size() == 0) {
            // Log.i("startZeiten", "ist 0 ");
            return; // keine zeit passt
        }

        for (int i = 0; i < aktuelleZeitZeilenListe.size(); i++) {
            //Log.i("start Zeit " , "index="+aktuelleZeitZeilenListe.elementAt(i) );
        }

        // jetzt auf prioritaet untersuchen

        //int tempIndex = getIndexPrioritaet();
        //Log.i("prioritaet" , ""+tempIndex);
        // wenn prioritaet gefunden, schauen ob passt

        //if  (tagesSuche.zeileOk(aktuelleZeitZeilenListe.elementAt(tempIndex)))  // passt alles bei der Prioritaetszeile? // oder zeile 0
        //{
        //	zeilenNummer = aktuelleZeitZeilenListe.elementAt(tempIndex);
        //Log.d("akt. block Nr.:" , "" + blockNummer);
        //	return ;
        //}
        // wenn nicht aus startZeitenIndex entfernen
        //else
        //{
        //	aktuelleZeitZeilenListe.remove(tempIndex);
        //}


        // jetzt auf prioritaet untersuchen
        //Log.e("vor", "prioritaet");

        for (int i = 9; i > 0; i--) {
            int tempIndex = getIndexPrioritaet(i);
            //Log.e("prioritaet", "" + tempIndex);
            // wenn prioritaet gefunden, schauen ob passt
            if (tempIndex != -1) {
                if (tagesSuche.zeileOk(aktuelleZeitZeilenListe.elementAt(tempIndex)))  // passt alles bei der Prioritaetszeile? // oder zeile 0
                {
                    zeilenNummer = aktuelleZeitZeilenListe.elementAt(tempIndex);
                    //Log.d("akt. block Nr.:" , "" + blockNummer);
                    return; // getMelodieString(blockOffset);  //
                }
                // wenn nicht aus startZeitenIndex entfernen
                else {
                    aktuelleZeitZeilenListe.remove(tempIndex);
                }
            }
        }

        // und liste von oben nach unten suchen ob alles passt...
        // wochentag, verknuepfte Taste etc...
        zeilenNummer = -1;

        if (aktuelleZeitZeilenListe.size() != 0) {
            //Log.i("startZeitenIndex" , "size=" + aktuelleZeitZeilenListe.size()) ;
            for (int i = 0; i < aktuelleZeitZeilenListe.size(); i++) {
                //Log.d("startZeitenIndex" , "i=" + i + "elementAt(i) = " + aktuelleZeitZeilenListe.elementAt(i));
                if (tagesSuche.zeileOk(aktuelleZeitZeilenListe.elementAt(i))) {
                    //Log.i("startZeitenIndex" , "elementAt(i) = " + aktuelleZeitZeilenListe.elementAt(i));
                    zeilenNummer = aktuelleZeitZeilenListe.elementAt(i);
                    return;
                }
            }
        }
    } // ende sucheBlockNummer

 
	/* 27.01.2015 in TageSuche gegeben
    private boolean zeileOk(int blockIndex)
	{
		if (!wochenTagOk(blockIndex))
		{
			zeilenNummer = -1 ;
			//Log.i("Wochentag" , "passt nicht ") ;
			return false ;
		}
		
		if (!verknuepfteTasteOn(blockIndex))
		{	
			zeilenNummer = -1 ;
			//Log.i("verkn. Taste" , "ist nicht ON" );
			return false ;
		}
		
		if (!periodischSommerWinterImmer(blockIndex))
		{
			zeilenNummer = -1 ;
			//Log.d("Sommer Winter" , "passt nicht");
			return false ;
		}
		if (!startEndeDatum(blockIndex))
		{
			zeilenNummer = -1 ;
			//Log.d("Start Ende" , "passt nicht");
			return false ;
		}
			
			
			
		// weitere pruefungen
	
		return true ;

	}

	*/

    /*
        if (!immerOK)
        {
               blockNummer = -1 ;
               return ;
        }

        if (!verknuepfteTasteOn)
        {
            blockNummer = -1 ;
            return ;
        }

        if (!periodisch)
        {
            blockNummer = -1 ;
            return ;
        }

        if (!ryhtmus)
        {
            blockNummer = -1 ;
            return ;
        }

        if (!reserve)
        {
            blockNummer = -1 ;
            return ;
        }

    } // ende suche blockNummer
*/
    private boolean startEndeDatum(int zeile) {
        try {
            if (excelread.getCellString(11, zeile) == ("")) // ist eine Start zeit angegeben?
            {
                // Log.i("keine" , "Startzeit");
                return true;  // keine startzeit, o.k. melden
            } else {
                String startZeit = excelread.getCellString(11, zeile).trim();
                String stopZeit = excelread.getCellString(12, zeile).trim();

                //Log.d("startZeit" , "= " + startZeit);
                //Log.d("stopZeit" , "= " + stopZeit);

                String[] startZeitSplit = startZeit.split("\\.");
                String[] stopZeitSplit = stopZeit.split("\\.");

//		String[] zeitSplit = zeit.split(":");


                //Log.d("gesplitet" , "length = " + startZeitSplit.length);
                //Log.i("Split" , "[0] = " + startZeitSplit[0]);
                //Log.i("Split" , "[1] = " + startZeitSplit[1]);

                int programmlisteStartZeitTag = Integer.parseInt(startZeitSplit[0]);
                int programmlisteStartZeitMonat = Integer.parseInt(startZeitSplit[1]);

                int programmlisteStopZeitTag = Integer.parseInt(stopZeitSplit[0]);
                int programmlisteStopZeitMonat = Integer.parseInt(stopZeitSplit[1]);

                return (checkDatumBetween(programmlisteStartZeitTag, programmlisteStartZeitMonat, programmlisteStopZeitTag, programmlisteStopZeitMonat));
            }
        } catch (ArrayIndexOutOfBoundsException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            new LogExcelError(11, zeile, excelTableFileName, 0, sourceFileName, 762);
        } catch (NumberFormatException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            new LogExcelError(11, zeile, excelTableFileName, 0, sourceFileName, 766);
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            new LogExcelError(11, zeile, excelTableFileName, 0, sourceFileName, 770);
        }
        return false;

    }
/*	
	private boolean checkDatumBetween(int startZeitTag, int startZeitMonat, int stopZeitTag, int stopZeitMonat)
	{
//		Log.i("check Datum" , "between");
//		Log.d("Start" , ""+ startZeitTag + " " + startZeitMonat);
//		Log.d("Stop" , ""+ stopZeitTag + " " + stopZeitMonat);
		
		int tag = calendar.get(Calendar.DAY_OF_MONTH);
		//Log.d("DAY_OF_MONTH" , "= " + tag) ;
		int monat = (calendar.get(Calendar.MONTH))+1;
		//Log.d("MONTH" , "= " + monat) ;
		
		if (monat >= startZeitMonat  && monat <= stopZeitMonat ) // passt das Monat ?
		{
			//Log.i("Monat" , "passt");
			if ( tag >= startZeitTag  && tag <= stopZeitTag ) // passt der Tag ?
			{
				//Log.i("Tag" , "passt");
				return true ;
			}
			else 
			{
				//Log.i("Tag" , "falsch");
				return false ;
			}
		}
		else
		{
			//Log.i("Monat" , "falsch");
			return false ;
		}
		
	}
*/


    private boolean checkDatumBetween(int startZeitTag, int startZeitMonat, int stopZeitTag, int stopZeitMonat) {
        int tag = calendar.get(Calendar.DAY_OF_MONTH);
        //Log.d("DAY_OF_MONTH" , "= " + tag) ;
        int monat = (calendar.get(Calendar.MONTH)) + 1; // monate beginne im Kalender bei 0
        //Log.d("MONTH" , "= " + monat) ;

        //Log.e("DAY_OF_MONTH" , "=" + tag) ;
        //Log.e("MONTH" , "= " + monat) ;
        //Log.e("startZeitTag" , "=" + startZeitTag) ;
        //Log.e("startZeitMonat" , "=" + startZeitMonat) ;
        //Log.e("stopZeitTag" , "=" + stopZeitTag) ;
        //Log.e("stopZeitMonat" , "=" + stopZeitMonat) ;

        if ((monat == startZeitMonat) && (monat == stopZeitMonat)) {
            return ((tag >= startZeitTag) && (tag <= stopZeitTag));
        } else {
            // die Monate sind nicht gleich:
            if (monat == startZeitMonat) {
                return tag >= startZeitTag;
            }
            if (monat == stopZeitMonat) {
                return tag <= stopZeitTag;
            }

            // wenn Monat start und ende nicht gleich sind
            // und das Monat ist weder startMonat noch stopMonat:

            do {
                // jedes weitere monat anschauen
                monat++;
                if (monat > 12) {
                    monat = 1;
                }

                if (monat == startZeitMonat) {
                    // treffe ich zuerst auf start Monat bin ich nicht zwischen start und ende
                    return false;
                }

            }
            while (monat != stopZeitMonat);
            // treffe ich auf das stopMonat bin ich dazwischen
            return true;
        }
    }


    private boolean periodischSommerWinterImmer(int zeile) {
        String swi = "";
        try {
            swi = (excelread.getCellString(10, zeile).trim());
        } catch (ArrayIndexOutOfBoundsException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            new LogExcelError(10, zeile, excelTableFileName, 0, sourceFileName, 871);
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            new LogExcelError(10, zeile, excelTableFileName, 0, sourceFileName, 875);
        }

        //Log.i("Periodisch" , "=" + swi);

//		if (swi.equals("1"));
//		{
//			Log.i("swi","= true");
//		}


        if (swi.equals("1") && (!isSommerzeit())) {
            return false;   // fehler  wenn Programmliste = sommerzeit und datum = winterzeit
        }

        if (swi.equals("2") && (isSommerzeit())) {
            return false;  // fehler wenn winterzeit gewuenscht aber es ist sommerzeit
        }

        //Log.d("Sommer/Winter","keine");
        return true;  // nicht sommerzeit und auch nicht winterzeit = immer
    }

    private boolean isSommerzeit() {
        boolean temp;
        //	temp =  calendar.getTimeZone().inDaylightTime(calendar.getTime());
        //	temp = calendar.getTimeZone().inDaylightTime(calendar.getTime());
        TimeZone cet = TimeZone.getTimeZone("CET");
        temp = cet.inDaylightTime(calendar.getTime());

        //Log.i("Sommerzeit" , "=" + temp );

        return temp;
    }

    private boolean immerOn(int zeile) {
        try {
            if (excelread.getCellString(9, zeile) == "") {
                return false;
            } else {
                return true;

            }
        } catch (ArrayIndexOutOfBoundsException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            new LogExcelError(9, zeile, excelTableFileName, 0, sourceFileName, 928);
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            new LogExcelError(9, zeile, excelTableFileName, 0, sourceFileName, 932);
        }
        return false;
    }

    /*
        private boolean verknuepfteTasteOn(int zeile)
        {
            try {
                if ( excelread.getCellString(14, zeile).trim() != "" )  // gibts ueberhaupt eine verkn. Taste?
                {
                    if ( verknuepfteTastenString.size() > 0 ) // wurde aktivity 2 ueberhaupt gestartet?
                    {
                        for (int i = 0; i < verknuepfteTastenString.size(); i++)
                        {
                            String activity2String = (verknuepfteTastenString.elementAt(i)).trim();
                            String programmlisteString = (excelread.getCellString(14 , zeile)).trim();
    //					Log.i("activity2String" , "" + activity2String);
    //					Log.i("proglisteString" , "" + programmlisteString);
                                    if (activity2String.equals(programmlisteString)) // wenn Taste vorhanden, ist sie ON?
                            {
    //						Log.d("die 2 strings" , "passen");
    //						Log.i("verkn. Taste", "" + TurmtechnikActivity.verknuepfteTastenOn[i]);
                                if (TurmtechnikActivity.verknuepfteTastenOn[i] == true )
                                {
                                    //Log.i("verkn. Taste" , "" + activity2String + "= true" );
                                    if(!immerOn(zeile))
                                    {
                                        if (!prognoseModus)
                                        {
                                            TurmtechnikActivity.verknuepfteTastenOn[i]=false ;
                                            int relaisOffsetForVerknuepfteTaste = TurmtechnikActivity.getRelaisOffsetForVerknuepfteTaste(i) ;
                                            if(relaisOffsetForVerknuepfteTaste != -1)
                                            {
                                                // Meteor Code entfernt
                                            }
                                                // aber wie das anzeigen wenn gerade Seite2Activity laeft ?
                                        }
                                    }
                                    return true ;
                                }
                                else
                                {
                                    //Log.i("verkn. Taste" , "" + activity2String + "= false" );
                                    return false ;
                                }
                            }
                            else
                            {
                                //Log.d("die strings" , "sind nicht gleich") ;
                            }
                        }
                        return false ;
                    }
                    else
                    {
                        return false ;
                    }
                }
                else
                {
                    return true ; // keine verknuepfte Taste in der Programmliste eingegeben
                }
            } catch (ArrayIndexOutOfBoundsException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                new LogExcelError(14, zeile, excelTableFileName, 0 ,sourceFileName, 992) ;
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                new LogExcelError(14, zeile, excelTableFileName, 0 ,sourceFileName,996 ) ;
            }
            return false ;
        }
        */
    private void makeZeitenZeilenListe() {
        zeile = 3; // beginne bei der ersten zeit zelle

        // Log.i("vor" , "get zeit");
        int zeilen = excelread.getCellZeilen();

        while (zeile < zeilen) {
            zeitStringFromProgrammliste = getZeit(zeile);
            if (zeitStringFromProgrammliste.equals("")) {
                break;
            }
            // Log.i("zeit=" , "" + zeit);

            if (zeitStringFromProgrammliste.contains(":"))  // 1.6.16 ist es eine normale uhrzeit hh:mm ?
            // oder neu SA/SU sonnenaufgang oder sonnenuntergang ?
            {
                String[] zeitSplit = zeitStringFromProgrammliste.split(":");

                int programmlisteMinute = Integer.parseInt(zeitSplit[1]);
                int minute = calendar.get(Calendar.MINUTE);

                if (minute == programmlisteMinute) {
                    // Log.d("minute", "passt");

                    int programmlisteStunde = Integer.parseInt(zeitSplit[0]);
                    if (programmlisteStunde > 23) {
                        programmlisteStunde = calendar.get(Calendar.HOUR_OF_DAY);
                    }

                    int stunde = calendar.get(Calendar.HOUR_OF_DAY);
                    if (stunde == programmlisteStunde) {
                        //Log.d("stunde", "passt");
                        aktuelleZeitZeilenListe.add(zeile);
                    }
                }
            } else {
                // ab 1.6.16 pruefe auf SA oder SU
            }

            zeile++;
        }
    }

    private String getZeit(int localZeile) {
        String zeitTemp = "--";
        try {
            zeitTemp = excelread.getCellString(0, localZeile);
        } catch (ArrayIndexOutOfBoundsException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            new LogExcelError(0, localZeile, excelTableFileName, 0, sourceFileName, 1051);
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            new LogExcelError(0, localZeile, excelTableFileName, 0, sourceFileName, 1055);
        }
        // Log.d("getZeit=" , "" + zeit);
        return zeitTemp;
    }


    //private int getIndexPrioritaet()
    //{
    //	int i ;
    //	for ( i = 0 ; i < aktuelleZeitZeilenListe.size(); i++)
    //	{
    //		if (tagesSuche.getPrioritaet(aktuelleZeitZeilenListe.elementAt(i)) > 1 )
    //		{
    //			return i ;
    //		}
    //	}
    //	if (i == aktuelleZeitZeilenListe.size())
    //	{
    //Log.d("keine" , "prioritaet");
    //		return 0 ;
    //	}
    //	return 0 ;
    //}

    private int getIndexPrioritaet(int prioritaetNummer) // von 3 bis 0 , 3 == hoechste Prioritaet
    {

        //Log.i("prioritaet", "Nummer=" + prioritaetNummer);
        for (int i = 0; i < aktuelleZeitZeilenListe.size(); i++) {
            if (tagesSuche.getPrioritaet(aktuelleZeitZeilenListe.elementAt(i)) == prioritaetNummer) {
                //Log.i("prioritaet", "gefunden index=" + i);
                return i;  // wenn eine Prioritaet gefunden
            }
        }

        //Log.d("keine" , "prioritaet");
        return -1;
    }


/*
	private int getPrioritaet(int localZeile)
	{
		try {
			return Integer.parseInt(excelread.getCellString(17, localZeile));
		} catch (NumberFormatException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			new LogExcelError(17, localZeile, excelTableFileName, 0 ,sourceFileName,1086) ;
		} catch (ArrayIndexOutOfBoundsException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			new LogExcelError(17, localZeile, excelTableFileName, 0 ,sourceFileName, 1090) ;
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			new LogExcelError(17, localZeile, excelTableFileName, 0 ,sourceFileName, 1094) ;
		}
		return 0;
	}
*/


    private boolean wochenTagOk(int zeile) {
        //Log.i("check" , "wochenTagOk=" + zeile);
        int wochentag = calendar.get(Calendar.DAY_OF_WEEK);
        if (wochentag == 1) {
            wochentag = 6;
        } else {
            wochentag = wochentag - 2;  // Montag == 0
        }

        // Wochentage beginnen immer bei Spalte E (Index 4), da Spalte D (Heizung) automatisch eingefügt wird, wenn sie fehlt
        int spalteWochentagStart = TagesSuche.SPALTE_E_WOCHENTAG_MONTAG; // Index 4

        //Log.i("aktuelle zeile" , " " + zeile);
        String programmlisteWochentag = "";
        try {
            programmlisteWochentag = excelread.getCellString(wochentag + spalteWochentagStart, zeile);
        } catch (ArrayIndexOutOfBoundsException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            new LogExcelError(wochentag + spalteWochentagStart, zeile, excelTableFileName, 0, sourceFileName, 1121);
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            new LogExcelError(wochentag + spalteWochentagStart, zeile, excelTableFileName, 0, sourceFileName, 1125);
        }
        //Log.d("programm Liste" + wochentag, "wochentag="+programmlisteWochentag);
        if (programmlisteWochentag == "") // kein x beim Wochentag?
        {
            return false;
        } else {
            return true;
        }
    }

    /** Delegation an TagesSuche – System.xls Sheet 12/13 nicht mehr, Festtage nur aus DB. */
    public synchronized static String getPathAndFilenameToday(int festtageTag, int festtageMonat, int festtageJahr) {
        return TagesSuche.getPathAndFilenameToday(festtageTag, festtageMonat, festtageJahr);
    }
    /* Ehemaliger Excel-Block (Sheet 13/12) entfernt – war Duplikat von TagesSuche.getPathAndFilenameToday.
    	int zeilen = excelreadTemp.getCellZeilen();
    	//Log.i("FESTE" , "Zeilen=" + zeilen) ;

    	int zeile = 5 ; // suche ab 5 te Zeile

    	int tagTemp ;
		int monatTemp ;

    	while(zeile < zeilen)
    	{
    		String datumTemp ="";
			try {
				datumTemp = excelreadTemp.getCellString(3, zeile);
			} catch (ArrayIndexOutOfBoundsException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				new LogExcelError(3, zeile, TurmtechnikActivity.newFesteFesttageFileString, 0 , sourceFileName, 1174) ;
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				new LogExcelError(0, zeile, TurmtechnikActivity.newFesteFesttageFileString, 0 , sourceFileName, 1178) ;
			}
    		if(datumTemp.equals(""))
    		{
    			break ; // keine Werte mehr
    		}

    		String[] datumSplit = datumTemp.split("\\.");

    	//	String[] zeitSplit = zeit.split(":");

    		//Log.i("datumTemp" , "= " + datumTemp) ;
    		//Log.i("datumSplit" , "length= " + datumSplit.length ) ;
    		//Log.i("datumInhalt" , "= " + datumSplit[0] + " " + datumSplit[1]) ;

    		tagTemp = -1 ;
    		monatTemp = -1 ;

    		if(datumSplit.length > 1)
    		{
    			try
    			{
    				tagTemp = Integer.parseInt(datumSplit[0]) ;
    				monatTemp = Integer.parseInt(datumSplit[1])-1 ; // intern Jaenner = 0
    			}
    			catch(NumberFormatException nfe)
    			{
    				tagTemp = -1 ;
    				monatTemp = -1 ;
    			}
    		}
    		if(festtageTag == tagTemp )
    		{
    			if(festtageMonat == monatTemp)
    			{
    				try {
						retString = excelreadTemp.getCellString(0, zeile);
					} catch (ArrayIndexOutOfBoundsException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} // lese Tagtyp
    				excelreadTemp.closeWorkbook(); // heute ist ein fester Festtag
    				//normalOderFesttag = 1 ;
    				return retString ;
    			}
    		}

    		zeile ++ ; // weiter suchen
    	}
    	excelreadTemp.closeWorkbook();
    	//Log.i("Feste" , "Feiertage durchsucht" ) ;

    	// jetzt nach variablen Festtagen suchen

	    excelreadTemp = new ExcelRead() ;

//    	excelreadTemp.openXls(TurmtechnikActivity.variableFesttageFileString) ;
	    //9.6.13 geaendert auf /Config/Sheet.xls sheet 12
	    try {
			excelreadTemp.openXlsSheet(TurmtechnikActivity.newVariableFesttageFileString, TurmtechnikActivity.VARIABLE_FESTTAGE_SHEET_NUMBER);
		} catch (BiffException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
			new LogExcelError(-1, -1, TurmtechnikActivity.newVariableFesttageFileString, -1, sourceFileName, 1243) ;
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
			new LogExcelError(-1, -1, TurmtechnikActivity.newVariableFesttageFileString, -1,sourceFileName, 1247) ;
		}

    	zeilen = excelreadTemp.getCellZeilen();
    	zeile = 5 ; // suche ab 5 te Zeile

    	int spalte = 3 ;
    	int jahrTemp ;

    	// jetzt spalte suchen

    	while (spalte < excelreadTemp.getCellSpalten())
    	{
    		try
    		{
    			String jahrString = "";
				try {
					jahrString = excelreadTemp.getCellString(spalte, 2);
				} catch (ArrayIndexOutOfBoundsException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					new LogExcelError(0, zeile, TurmtechnikActivity.newVariableFesttageFileString, 0 ,sourceFileName,1268) ;
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					new LogExcelError(0, zeile, TurmtechnikActivity.newVariableFesttageFileString, 0 ,sourceFileName,1272) ;
				}
    			//Log.i("jahrString" , "= " + jahrString) ;
    			jahrTemp = Integer.parseInt(jahrString) ;
    			//Log.i("jahrTemp" , "= " + jahrTemp ) ;
    		}
    		catch(NumberFormatException nfe)
    		{
    			jahrTemp = -1 ;
    		}

    		if(festtageJahr == jahrTemp)
    		{
    			//Log.i("JAHR break" , "gefunden spalte=" + spalte) ;
    			break ;
    		}


    		spalte ++ ;
    	}

    	if (spalte >= excelreadTemp.getCellSpalten())
    	{
    		// kein Jahr in der Tabelle passt zum heurigen Jahr ---> Abbruch!
    		return retString ; // return ""
    	}

    	// jetzt zeile fuer zeile in der gefundenen Jahreszahl spalte durchsuchen
    	//Log.i("SPALTE" , "DURCHSUCHEN " + spalte) ;

    	while(zeile < zeilen)
    	{
    		String datumTemp = "";
			try {
				datumTemp = excelreadTemp.getCellString(spalte, zeile);
			} catch (ArrayIndexOutOfBoundsException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				new LogExcelError(spalte, zeile, TurmtechnikActivity.newVariableFesttageFileString , 0 ,sourceFileName,1310) ;
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				new LogExcelError(spalte, zeile, TurmtechnikActivity.newVariableFesttageFileString , 0 ,sourceFileName, 1314) ;
			}
    		if(datumTemp.equals(""))
    		{
    			break ; // keine Werte mehr
    		}


    		String[] datumSplit = datumTemp.split("\\.");

    		tagTemp = -1 ;
    		monatTemp = -1 ;
    		if(datumSplit.length > 1)
    		{
    			try
    			{
    				tagTemp = Integer.parseInt(datumSplit[0]) ;
    				monatTemp = Integer.parseInt(datumSplit[1])-1 ; // intern Jaenner = 0
    			}
    			catch(NumberFormatException nfe)
    			{
    				tagTemp = -1 ;
    				monatTemp = -1 ;
    			}
    		}
    	    //Log.i("tagTEMP" , "=" + tagTemp) ;
    	   // Log.i("monatTemp" , "=" + monatTemp) ;

    		if(festtageTag == tagTemp )
    		{
    			if(festtageMonat == monatTemp)
    			{
    				try {
						retString = excelreadTemp.getCellString(0, zeile);
					} catch (ArrayIndexOutOfBoundsException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
						new LogExcelError(0, zeile, TurmtechnikActivity.newVariableFesttageFileString , 0, sourceFileName, 1351 ) ;
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
						new LogExcelError(0, zeile, TurmtechnikActivity.newVariableFesttageFileString , 0 , sourceFileName, 1355) ;
					} // lese Tagtyp
    				excelreadTemp.closeWorkbook(); // heute ist ein variabler Festtag
    				//normalOderFesttag = 2 ;
    				return retString ; // ende!
    			}
    		}

    		zeile ++ ; // sonst weiter suchen
    	}

    	excelreadTemp.closeWorkbook();
    	excelreadTemp = null ;

    	return retString ;
    } // ende von check Festtage
    */

//    private int startZeitenBlockNr(int index)
//    {
//    	return aktuelleZeitZeilenListe.elementAt(index);
//    }


    public void endUhrThread() {
        doRun = false;
    }

    // Meteor Code entfernt: sendTastenStatusMeteor()
    // Meteor Code entfernt: makeMapObject()

    private void checkMelodieStart(long momentanMs) {

        if ((TurmtechnikActivity.flagAutomaticOnOff) == true) // && blockReady == 0)
        {

            long momentanMsDurchTausend = momentanMs / 1000 / 5;

            //if(tagesSuche != null) {
            //Log.e("checkMelodieStart", "=" + tagesSuche.convertMsToTimeString(momentanMs) + " " + tagesSuche.convertMsToTimeString(StaticVariable.nextMelodieStartSekunden * 1000));
            //}
            //Log.e("mstartcheck" , "=" + momentanMsDurchTausend + StaticVariable.nextMelodieStartSekunden / 5 ) ;

            if (momentanMsDurchTausend == (StaticVariable.nextMelodieStartSekunden2 / 5)) {
                //Log.e("melodie", "threadStart = O.K.");
                if (StaticVariable.melodieThreadNew == null && StaticVariable.pathAndFileNameNextMelodie != null
                        && !StaticVariable.pathAndFileNameNextMelodie.isEmpty()) {
                    // Nach Stop: gestopptes Programm nicht wieder automatisch starten
                    if (StaticVariable.pathStoppedByUser != null && !StaticVariable.pathStoppedByUser.isEmpty()
                            && StaticVariable.pathStoppedByUser.equals(StaticVariable.pathAndFileNameNextMelodie)) {
                        return; // dieses Programm wurde gestoppt, nicht erneut starten
                    }
                    synchronized (MELODIE_START_LOCK) {
                        if (StaticVariable.melodieThreadNew == null) {
                            StaticVariable.melodieThreadNew =
                                    new MelodieThreadNew(StaticVariable.pathAndFileNameNextMelodie);
                            StaticVariable.melodieThreadNew.start();
                        }
                    }

                    blockReady = 2; // einfach nicht 0 also Block(Melodie) ist aktiv

                    StaticVariable.stringInfoTextField[0] = ("momentan Aktiv: " + StaticVariable.nameNextMelodie);
                } else {
                    //Log.e("melodie", "thread nicht null");
                }
            }
        }
    }

    // 21.03.2015 das prueft jetzt AusgangHeizungThreadNew2 selber
     /*
     private void checkHeizungStart( long momentanMs)
     {
         achtung neu machen!!!!
         long momentanMsDurchTausend = (momentanMs / 1000) / 10;

         if(tagesSuche!=null)
         {
             //Log.e("checkHeizung", "momentanMs=" + tagesSuche.convertMsToTimeString(momentanMsDurchTausend*10000) +
             //        " nextHeizungStart=" + tagesSuche.convertMsToTimeString(StaticVariable.nextHeizungStartSekunden * 1000));
         }

         if (momentanMsDurchTausend == (StaticVariable.nextHeizungStartSekunden/10) )
         {
             if (StaticVariable.ausgangHeizungThreadNew == null)
             {
                 Log.e("starte", "Heizung thread");

                 StaticVariable.ausgangHeizungThreadNew = new AusgangHeizungThreadNew
                         (StaticVariable.nextHeizungLaufzeitMinutenString,
                                 StaticVariable.nextHeizungFunktionsName
                         ) ;
                 StaticVariable.ausgangHeizungThreadNew.start();
             }
         }
     }
     */

    /**
     * Schaltet Vorschwing-Motor-Relais pro Ausgang (j) genau zu dessen Startzeit ein.
     * Jeder Ausgang j hat eigene Startzeiten (vorschwingenStartzeitenMotorRelais.get(j)) und
     * eigene Vorschwing-Dauer (vorschwingZeitSekunden.get(j)) – Vergleich in vollen Sekunden,
     * damit nicht alle Relais in derselben 5-Sekunden-Periode zugleich schalten.
     */
    private synchronized void checkVorschwingen(long momentanMs) {
        if (StaticVariable.vorschwingenStartzeitenMotorRelais.size() == 0) return;

        long momentanSec = momentanMs / 1000;
        int vorschwingZeiten = StaticVariable.vorschwingenStartzeitenMotorRelais.size();

        for (int j = 0; j < vorschwingZeiten; j++) {
            ArrayList<Long> startZeiten = StaticVariable.vorschwingenStartzeitenMotorRelais.get(j);
            if (startZeiten == null) continue;
            if (j >= StaticVariable.vorschwingenMotorRelaisNeu.size()) continue;

            for (int i = 0; i < startZeiten.size(); i++) {
                long startSekunden = startZeiten.get(i);
                if (momentanSec != startSekunden) continue;

                if (esWarBenutzerProgramm) {
                    BenutzerMelodienActivity.clearLastMelodieButton();
                    esWarBenutzerProgramm = false;
                }
                if (esWarManuellerStart) {
                    esWarManuellerStart = false;
                }
                blockReady = 1;

                int vorschwingRelais = StaticVariable.vorschwingenMotorRelaisNeu.get(j);
                if (vorschwingRelais != 0) {
                    // Relais 16 in den ersten 60 s nach App-Start nicht durch Vorschwingen einschalten (verhindert „Relais 16 hartnäckig an“ beim Start)
                    if (vorschwingRelais == 16 && android.os.SystemClock.elapsedRealtime() < 60 * 1000)
                        continue;
                    Log.e("checkVorschwingen", "vorschwingRelais=" + vorschwingRelais + " Ausgang j=" + j);
                    int relaisOffset = getRelaisOffset(vorschwingRelais);
                    TurmtechnikActivity.globalOn[relaisOffset] = true;
                    Serial_IoThread.relaisNew[vorschwingRelais - 1] = true;
                }
            }
        }
    }

    private int getRelaisOffset(int relaisNumber) {
        int returnRelaisOffset = 0;

        for (int i = 0; i < TurmtechnikActivity.RELAIS_COUNT; i++) {
            if (TurmtechnikActivity.relaisNumber[i] == relaisNumber) {
                returnRelaisOffset = i;
                break;
            }
        }

        return returnRelaisOffset;
    }

    private void setFutureTimeToStartTime() {
        Calendar calendar = Calendar.getInstance();

        long timeTempMs = calendar.getTimeInMillis();
        //timeTempMs += 120000L ;
        calendar.setTimeInMillis(timeTempMs);

        futureJahr = calendar.get(Calendar.YEAR);
        futureMonat = calendar.get(Calendar.MONTH);
        futureTag = calendar.get(Calendar.DAY_OF_MONTH);
        futureStunde = StaticVariable.startStundeManuell;   //calendar.get(Calendar.HOUR_OF_DAY) ;
        futureMinute = StaticVariable.startMinuteManuell;   //calendar.get(Calendar.MINUTE) ;
    }

    private void berechneSonnenAufUndUntergang() {
        android.content.Context context = TurmtechnikActivity.turmtechnikContext;
        PlatinenDatabaseHelper.AnlagenstandortConfig config = TurmtechnikActivity.loadAnlagenstandortConfig(context);
        if (config == null) return;

        String breitengrad = config.breitengrad != null ? config.breitengrad.replaceAll(",", ".") : "";
        String laengengrad = config.laengengrad != null ? config.laengengrad.replaceAll(",", ".") : "";
        String anpassungMinutenSonnenaufgang = config.anpassungSonnenaufgangMinuten != null ? config.anpassungSonnenaufgangMinuten : "0";
        String anpassungMinutenSonnenuntergang = config.anpassungSonnenuntergangMinuten != null ? config.anpassungSonnenuntergangMinuten : "0";
        String rundenString = config.rundenMinuten != null ? config.rundenMinuten : "5";
        if (breitengrad.isEmpty() || laengengrad.isEmpty()) return;

        //Log.i("anlageUStandort", "breitengrad=" + breitengrad);
        //Log.i("anlageUStandort", "laengengrad=" + laengengrad);
        //Log.i("anlageUStandort", "anpassung SA=" + anpassungMinutenSonnenaufgang);
        //Log.i("anlageUStandort", "anpassung SU=" + anpassungMinutenSonnenuntergang);
        //Log.i("anlageUStandort", "rundenString=" + rundenString);

        //sonnenAufgangBerechnen ;
        Location location = new Location(breitengrad, laengengrad);
        SunriseSunsetCalculator sunriseSunsetCalculator = new SunriseSunsetCalculator(location, TimeZone.getDefault());

        Calendar calendar = Calendar.getInstance();

        int sAsUjahr = calendar.get(Calendar.YEAR);
        int sAsUmonat = calendar.get(Calendar.MONTH);
        int sAsUtag = calendar.get(Calendar.DAY_OF_MONTH);

        //Log.i("anlageUStandort", "jahr=" + sAsUjahr + " monat=" + sAsUmonat + " tag=" + sAsUtag);

        if ((sAsUmonat == 5) && (sAsUtag == 20))  // komischer Fehler Sonnenuntergang am 20.06.
        {
            sAsUtag = 21;
        }

        //Log.i("anlageUStandort", "jahr=" + sAsUjahr + " monat=" + sAsUmonat + " tag=" + sAsUtag);
        calendar.set(sAsUjahr, sAsUmonat, sAsUtag);

        //Log.i("anlageUStandort" , "calendar=" + calendar) ;


        Calendar officialSunrise = sunriseSunsetCalculator.getOfficialSunriseCalendarForDate(calendar);
        Calendar officialSunset = sunriseSunsetCalculator.getOfficialSunsetCalendarForDate(calendar);

        String stringSunriseOfficiale = sunriseSunsetCalculator.getOfficialSunriseForDate(calendar);
        StaticVariable.sonnenAufgangString = stringSunriseOfficiale;
        String stringSunsetOfficiale = sunriseSunsetCalculator.getOfficialSunsetForDate(calendar);
        StaticVariable.sonnenUntergangString = stringSunsetOfficiale;
        //String stringSunriseAstronomic = sunriseSunsetCalculator.getAstronomicalSunriseForDate(Calendar.getInstance()) ;
        //String stringSunsetAstronomic = sunriseSunsetCalculator.getAstronomicalSunsetForDate(Calendar.getInstance()) ;
        //Log.e("anlageStandort" , "Sonnenaufgang=" + officialSunrise) ;
        //Log.i("anlageUStandort", "Sonnenaufgang Officiale=" + stringSunriseOfficiale);
        //Log.e("anlageStandort" , "Sonnenaufgang Astronomic=" + stringSunriseAstronomic) ;
        //Log.e("anlageStandort" , "Sonnenuntergang=" + officialSunset) ;
        //Log.i("anlageUStandort", "Sonnenuntergang Officiale=" + stringSunsetOfficiale);
        //Log.e("anlageStandort" , "Sonnenaufgang Astronomic=" + stringSunsetAstronomic) ;


        Date sonnenaufgangTempDate = officialSunrise.getTime();
        long sonnenaufgangTempMs = sonnenaufgangTempDate.getTime();
        long anpassungMsAufgang = (Long.parseLong(anpassungMinutenSonnenaufgang)) * (60 * 1000);
        long rundenLong = (Long.parseLong(rundenString)) * (60 * 1000);
        //Log.i("anlageStandort" , "sonnensufgangTempMs=" + sonnenaufgangTempMs) ;
        //Log.i("anlageStandort" , "rundenString long=" + rundenLong ) ;
        sonnenaufgangTempMs = sonnenaufgangTempMs + anpassungMsAufgang;
        sonnenaufgangTempMs = roundMintues(sonnenaufgangTempMs, rundenLong);
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");
        String sonnenaufgangKorrigiert = sdf.format(sonnenaufgangTempMs);
        //Log.i("anlageUStandort", "Sonnenaufgang Korrigiert=" + sonnenaufgangKorrigiert);

        Date sonnenuntergangTempDate = officialSunset.getTime();
        long sonnenuntergangTempMs = sonnenuntergangTempDate.getTime();
        long anpassungMsUntergang = (Long.parseLong(anpassungMinutenSonnenuntergang)) * (60 * 1000);
        long rundenLongUntergang = (Long.parseLong(rundenString)) * (60 * 1000);
        //Log.i("anlageStandort" , "sonnensufgangTempMs=" + sonnenaufgangTempMs) ;
        //Log.i("anlageStandort" , "rundenString long=" + rundenLong ) ;
        sonnenuntergangTempMs = sonnenuntergangTempMs + anpassungMsUntergang;
        sonnenuntergangTempMs = roundMintues(sonnenuntergangTempMs, rundenLongUntergang);
        //SimpleDateFormat sdf =  new SimpleDateFormat("HH:mm") ;
        String sonnenuntergangKorrigiert = sdf.format(sonnenuntergangTempMs);
        //Log.i("anlageUStandort", "Sonnenuntergang Korrigiert=" + sonnenuntergangKorrigiert);

        //plusMinusBerechnen ;
        //wertRunden ;
        //sonnenUntergangBerechnen ;
        //plusMinusBerechnen ;
        //wertRunden ;

        StaticVariable.sonnenAufgangStringGerundet = sonnenaufgangKorrigiert;
        StaticVariable.sonnenUntergangStringGerundet = sonnenuntergangKorrigiert;
    }

    private long roundMintues(long number, long round) {
        long halfRound = round / 2;
        long nummberTemp = number;
        if (halfRound == 0) {
            return nummberTemp;
        }
        nummberTemp = nummberTemp + halfRound;
        nummberTemp = nummberTemp / round;
        nummberTemp = nummberTemp * round;
        return nummberTemp;
    }

    private void sleepTime(long time) {
        try {
            Thread.sleep(time);
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }


    public static void setExcelreadNull()
    {
        excelread = null ;
        newSearchAutomaticStart = true ;
    }
} // ende der Klasse
