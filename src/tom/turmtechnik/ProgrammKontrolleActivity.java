package tom.turmtechnik;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.awr_technology.sunrisesunset.Location;
import com.awr_technology.sunrisesunset.SunriseSunsetCalculator;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import java.util.Vector;

import jxl.read.biff.BiffException;


public class ProgrammKontrolleActivity extends Activity {
    private final String sourceFileName = "ProgrammKontrolleActivity";

    //public Vector<Button> editButtons = new Vector<Button>();
    public Button buttonEdit ;

    private ArrayList<String> pathAndFileNameListForEditor = new ArrayList<>();
    private ArrayList<Integer> indexForEditor = new ArrayList<>() ;
    public Context context ;
    private LinearLayout linearLayout ;
    private int buttonsIndex = 0 ;
    private boolean printThreadReady = false ;

    private int onClickIndex = 0 ;
    private int globalIndex;

    //private String stringBitteWarten = "\n\t\tBitte warten.. Die Programmlisten werden durchsucht.\n";
    private String stringBitteWarten = "\n\t\t" + StaticVariable.getUebersetzung(11) + "\n";

    private StringBuffer outputStringBuffer;

    private String sonnenAufgangString;
    private String sonnenUntergangString;
    private String sonnenAufgangStringGerundet;
    private String sonnenUntergangStringGerundet;

    private boolean newDate = false;

    private String filePathAndName;
    private String tagesProgrammName;

    private TextView textView;

    private ExcelRead excelread;

    private int displayWidth;
    private int displayHeight;

    int pfeilWidth;
    int pfeilHeight;

    RelativeLayout.LayoutParams rlayoutParams;

    private int jahrNeu;
    private int monatNeu;
    private int tagNeu;

    private int checkMinute;
    private int checkStunde;

    private Button btDate;

    private Vector<Integer> aktuelleZeitZeilenListe = new Vector<Integer>();

    Calendar calendar;

    private boolean doRun;

    protected Handler handler;

    TagesSuche tagesSuche;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        StaticVariable.programmAbfrageAktiv2 = true;

        tagesSuche = new TagesSuche();

        context  = getApplicationContext() ;

        setContentView(R.layout.programm_kontrolle_layout);


//        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
//        StrictMode.setThreadPolicy(policy);

        getDisplayParameter();
//        Log.i("displayWidth" , "=" + displayWidth) ;
//    	Log.i("displayHeight" , "=" + displayHeight) ;



        outputStringBuffer = new StringBuffer();

        //outputStringBuffer.append("Programmtag prüfen");
        outputStringBuffer.append(StaticVariable.getUebersetzung(12) );

        //pfeilWidth = displayWidth / 10 ;
        //pfeilHeight = displayHeight / 10 ;
        pfeilWidth = StaticVariable.tastenBreite;
        pfeilHeight = StaticVariable.tastenHoehe;

        textView = (TextView) this.findViewById(R.id.outputText);

        Calendar calendar = Calendar.getInstance();
        jahrNeu = calendar.get(Calendar.YEAR);
        monatNeu = calendar.get(Calendar.MONTH);
        tagNeu = calendar.get(Calendar.DAY_OF_MONTH);


        //excelread = new ExcelRead() ;
        //String fileName = (TurmtechnikActivity.nebenUhrFileString);
        //excelread.openXls(fileName);

        linearLayout = (LinearLayout) this.findViewById(R.id.linear_layout_button_feld) ;

        Button btLinks = (Button) this.findViewById(R.id.buttonPfeilLinks);

        // neu ab 17.6.13 links Taste wird home Taste
        btLinks.setBackgroundDrawable(StaticVariable.btn_home_drawable);
        btLinks.setText(StaticVariable.btn_home_textstring);
        btLinks.setTextColor(getResources().getColor(R.color.white));

        rlayoutParams = new RelativeLayout.LayoutParams(pfeilWidth, pfeilHeight);
        btLinks.setLayoutParams(rlayoutParams);
        rlayoutParams.leftMargin = 20;
        rlayoutParams.topMargin = 10;

        btLinks.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                // TODO Auto-generated method stub
                //finish();
                endActivityProgrammKontrolle();
            }
        });


        Button btRechts = (Button) this.findViewById(R.id.buttonPfeilRechts);

        btRechts.setBackgroundDrawable(StaticVariable.btn_help_drawable);
        btRechts.setText(StaticVariable.btn_help_textstring);
        btRechts.setTextColor(getResources().getColor(R.color.white));

        rlayoutParams = new RelativeLayout.LayoutParams(pfeilWidth, pfeilHeight);
        btRechts.setLayoutParams(rlayoutParams);
        // neu ab 18.6.13 rechts Taste wird help Taste


        rlayoutParams.leftMargin = pfeilWidth + 40;
        rlayoutParams.topMargin = 10;


        btRechts.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                // TODO Auto-generated method stub
                //finish();
                //startSetNebenuhr();
                startHelpSeite();
            }
        });


        btDate = (Button) findViewById(R.id.buttonGetDatum);

        btDate.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                // TODO Auto-generated method stub
                showDialog(0);
                // suche Programme:
                //printProgrammListe() ;



            }

        });

        btDate.setText( StaticVariable.getUebersetzung(13) ) ;


        buttonEdit = (Button) findViewById(R.id.button_editor) ;

        buttonEdit.setText("Editor") ;

        buttonEdit.setOnClickListener(new OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                // DB oder Excel: App-Editor öffnen (unterstützt beides)
                if (filePathAndName != null && !filePathAndName.isEmpty()) {
                    StaticVariable.pathAndFilenameEditor = filePathAndName;
                    Log.e("ProgrammKontrolle", "pathAndFilenameEditor gesetzt auf: " + filePathAndName);
                } else if (TurmtechnikActivity.normalprogrammFileString != null && !TurmtechnikActivity.normalprogrammFileString.isEmpty()) {
                    StaticVariable.pathAndFilenameEditor = TurmtechnikActivity.normalprogrammFileString; // default
                    Log.e("ProgrammKontrolle", "pathAndFilenameEditor gesetzt auf normalprogrammFileString: " + TurmtechnikActivity.normalprogrammFileString);
                } else {
                    Log.e("ProgrammKontrolle", "FEHLER: Weder filePathAndName noch normalprogrammFileString sind gesetzt!");
                    Toast.makeText(ProgrammKontrolleActivity.this, "Fehler: Programmdatei nicht gefunden", Toast.LENGTH_LONG).show();
                    return;
                }

                // Setze pathAndFilenameEditorIndex
                if (pathAndFileNameListForEditor.size() > 0 && indexForEditor.size() > 0) {
                    StaticVariable.pathAndFilenameEditorIndex = indexForEditor.get(0);
                    Log.e("ProgrammKontrolle", "pathAndFilenameEditorIndex aus Liste: " + StaticVariable.pathAndFilenameEditorIndex);
                } else {
                    StaticVariable.pathAndFilenameEditorIndex = 3; // erste editierbare Zeile im Normalprogramm.xls
                    Log.e("ProgrammKontrolle", "pathAndFilenameEditorIndex auf Standardwert 3 gesetzt");
                }

                StartTurmtechnikService.setTimeTurmtechnik(60 * 15); // 15 Minuten

                Intent startEditor = new Intent(ProgrammKontrolleActivity.this, EditorActivity.class);
                ProgrammKontrolleActivity.this.startActivity(startEditor);
                // Nicht finish() – nach Schließen des Editors zurück zur Programmliste,
                // damit Seite2Activity nicht „übersprungen“ wird und korrekt erhalten bleibt
            }
        });

        // thread fuer update infotext erzeugen
        handler = new Handler();
        startUpdateInfoText();

    } // ende von onCreate

    private class printProgrammlisteThread extends Thread
    {
        public void run()
        {

            //DdpThread.clrProgrammAbfragen();

            //Log.i("PRINT" , "THREAD gestartet") ;

            outputStringBuffer.append(stringBitteWarten);

            // Prüfe ob Programme mit SA oder SU vorhanden sind, bevor Sonnenaufgang/Sonnenuntergang angezeigt wird
            boolean hatSAProgramme = false;
            boolean hatSUProgramme = false;
            
            try {
                // Tagtyp: zuerst aus Benutzerprogramm (ganzer Tag umgeschaltet), sonst aus Kalender/Feiertagen
                TurmtechnikActivity.loadBenutzerprogrammeFromDb(context);
                String tagtypName = TurmtechnikActivity.getBenutzerprogrammTagtypForDate(tagNeu, monatNeu, jahrNeu);
                if (tagtypName == null || tagtypName.trim().isEmpty()) {
                    tagtypName = TagesSuche.getPathAndFilenameToday(tagNeu, monatNeu, jahrNeu);
                }
                if (tagtypName != null && tagtypName.toLowerCase().endsWith(".xls")) {
                    tagtypName = tagtypName.substring(0, tagtypName.length() - 4);
                }
                if (tagtypName == null || tagtypName.equals("")) {
                    tagtypName = "Normalprogramm";
                }
                
                PlatinenDatabaseHelper dbHelper = PlatinenDatabaseHelper.getInstance(context);
                List<Programm> programme = dbHelper.getProgrammeByTagtyp(tagtypName);
                
                // Prüfe ob Programme SA oder SU verwenden
                if (programme != null) {
                    for (Programm programm : programme) {
                        if (programm.getStartzeit() != null) {
                            String startzeit = programm.getStartzeit().trim();
                            if (startzeit.equals("SA")) {
                                hatSAProgramme = true;
                            } else if (startzeit.equals("SU")) {
                                hatSUProgramme = true;
                            }
                        }
                    }
                }
            } catch (Exception e) {
                Log.e("ProgrammKontrolle", "Fehler beim Prüfen der Programme auf SA/SU", e);
            }
            
            // Berechne Sonnenaufgang/Sonnenuntergang nur wenn benötigt
            if (hatSAProgramme || hatSUProgramme) {
                berechneSonnenAufUndUntergang(jahrNeu, monatNeu, tagNeu);
                
                // Zeige nur an, wenn Programme vorhanden sind
                outputStringBuffer.append("\n" + StaticVariable.getUebersetzung(14) + " " + sonnenAufgangString);
                outputStringBuffer.append("  " + StaticVariable.getUebersetzung(15) + " " + sonnenUntergangString + "\n");
            }
            
            // Zeige Feiertag an, falls vorhanden
            try {
                PlatinenDatabaseHelper dbHelper = PlatinenDatabaseHelper.getInstance(context);
                PlatinenDatabaseHelper.FeiertagsInfo feiertagsInfo = dbHelper.getFeiertagsInfo(tagNeu, monatNeu, jahrNeu);
                if (feiertagsInfo != null && feiertagsInfo.name != null && !feiertagsInfo.name.trim().isEmpty()) {
                    outputStringBuffer.append("Feiertag: " + feiertagsInfo.name + " (" + feiertagsInfo.datum + ")\n");
                }
            } catch (Exception e) {
                Log.e("ProgrammKontrolle", "Fehler beim Abrufen der Feiertagsinfo", e);
            }
            // Benutzerprogramme aus DB neu laden, damit Web-UI-Änderungen in der Programmabfrage sichtbar sind
            TurmtechnikActivity.loadBenutzerprogrammeFromDb(context);
            filePathAndName = "";
            if (filePathAndName == null || filePathAndName.equals(""))
            {
                Log.e("getPathAndFilename", "Suche Tagtyp für Datum: " + tagNeu + "." + (monatNeu+1) + "." + jahrNeu + " (monatNeu=" + monatNeu + ", 0-basiert)");
                // Tagtyp: zuerst aus Benutzerprogramm (ganzer Tag umgeschaltet), sonst aus Kalender/Feiertagen
                String tagtypName = TurmtechnikActivity.getBenutzerprogrammTagtypForDate(tagNeu, monatNeu, jahrNeu);
                if (tagtypName == null || tagtypName.trim().isEmpty()) {
                    tagtypName = TagesSuche.getPathAndFilenameToday(tagNeu, monatNeu, jahrNeu);
                }
                if (tagtypName != null && tagtypName.toLowerCase().endsWith(".xls")) {
                    tagtypName = tagtypName.substring(0, tagtypName.length() - 4);
                }
                Log.e("getPathAndFilename", "Tagtyp aus DB/Excel=" + (tagtypName != null ? tagtypName : "null") + " für " + tagNeu + "." + (monatNeu+1) + "." + jahrNeu);
                
                // Prüfe ob Programme in DB sind
                boolean programmeInDB = false;
                if (tagtypName != null && !tagtypName.equals("")) {
                    Log.e("getPathAndFilename", "Tagtyp gefunden: '" + tagtypName + "', prüfe Programme in DB");
                    try {
                        PlatinenDatabaseHelper dbHelper = PlatinenDatabaseHelper.getInstance(context);
                        List<Programm> programme = dbHelper.getProgrammeByTagtyp(tagtypName);
                        Log.e("getPathAndFilename", "Programme in DB für " + tagtypName + ": " + programme.size());
                        if (programme.size() > 0) {
                            programmeInDB = true;
                            Log.e("getPathAndFilename", "Programme werden aus DB geladen - verwende DB statt Excel");
                            // Setze filePathAndName auf einen speziellen Marker, um zu signalisieren, dass DB verwendet werden soll
                            filePathAndName = "DB:" + tagtypName;
                        } else {
                            Log.w("getPathAndFilename", "Keine Programme in DB für Tagtyp: " + tagtypName + ", verwende Excel");
                        }
                    } catch (Exception e) {
                        Log.e("getPathAndFilename", "Fehler beim Prüfen der DB", e);
                        e.printStackTrace();
                    }
                } else {
                    Log.w("getPathAndFilename", "Kein Tagtyp gefunden (leer oder null), verwende Normalprogramm");
                }
                
                // NUR Datenbank - KEIN Excel-Fallback mehr!
                if (!programmeInDB) {
                    if (tagtypName.equals("")) {
                        // Kein Tagtyp gefunden, versuche Normalprogramm aus DB
                        try {
                            PlatinenDatabaseHelper dbHelper = PlatinenDatabaseHelper.getInstance(context);
                            List<Programm> normalProgramme = dbHelper.getProgrammeByTagtyp("Normalprogramm.xls");
                            if (normalProgramme != null && normalProgramme.size() > 0) {
                                programmeInDB = true;
                                filePathAndName = "DB:Normalprogramm.xls";
                                Log.e("getPathAndFilename", "Verwende Normalprogramm aus DB");
                            } else {
                                Log.e("getPathAndFilename", "FEHLER: Keine Programme in DB für Normalprogramm! Bitte 'Import Excel Programmtage' in Web-UI verwenden.");
                                outputStringBuffer.append("\nFEHLER: Keine Programme in Datenbank gefunden!\n");
                                outputStringBuffer.append("Bitte 'Import Excel Programmtage' in Web-UI verwenden.\n");
                                return; // Beende Thread
                            }
                        } catch (Exception e) {
                            Log.e("getPathAndFilename", "FEHLER beim Laden von Normalprogramm aus DB", e);
                            outputStringBuffer.append("\nFEHLER: Keine Programme in Datenbank gefunden!\n");
                            outputStringBuffer.append("Bitte 'Import Excel Programmtage' in Web-UI verwenden.\n");
                            return; // Beende Thread
                        }
                    } else {
                        Log.e("getPathAndFilename", "FEHLER: Keine Programme in DB für Tagtyp: " + tagtypName + "! Bitte 'Import Excel Programmtage' in Web-UI verwenden.");
                        outputStringBuffer.append("\nFEHLER: Keine Programme in Datenbank für " + tagtypName + " gefunden!\n");
                        outputStringBuffer.append("Bitte 'Import Excel Programmtage' in Web-UI verwenden.\n");
                        return; // Beende Thread
                    }
                }

            } else {

                //String temp = TurmtechnikActivity.sdCardPath
                //			      + "/Turmtechnik/Programmtage/" + filePathAndName ; // + ".xls" ;
                //filePathAndName = temp ;
            }

            // Extrahiere Tagtyp-Name aus Pfad (für Anzeige): ohne "DB:"-Präfix und ohne ".xls"
            int lastSlash = filePathAndName.lastIndexOf("/");
            int lastDot = filePathAndName.lastIndexOf(".");
            if (lastSlash >= 0 && lastDot > lastSlash) {
                tagesProgrammName = filePathAndName.substring(lastSlash + 1, lastDot);
            } else if (lastSlash >= 0) {
                tagesProgrammName = filePathAndName.substring(lastSlash + 1);
            } else {
                tagesProgrammName = filePathAndName;
            }
            if (tagesProgrammName != null && tagesProgrammName.startsWith("DB:")) {
                tagesProgrammName = tagesProgrammName.substring(3);
            }
            if (tagesProgrammName != null && tagesProgrammName.toLowerCase().endsWith(".xls")) {
                tagesProgrammName = tagesProgrammName.substring(0, tagesProgrammName.length() - 4);
            }

            outputStringBuffer.append("\n" + tagesProgrammName + ":\n"); // Programmliste anzeigen
            // TEST:
            // DDP Code entfernt


            // NUR Datenbank - KEIN Excel mehr!
            boolean useDatabase = false;
            String tagtypForDatabase = null;
            
            if (filePathAndName != null && filePathAndName.startsWith("DB:")) {
                useDatabase = true;
                tagtypForDatabase = filePathAndName.substring(3);
                Log.e("getPathAndFilename", "Verwende Programme aus Datenbank für Tagtyp: " + tagtypForDatabase);
            } else {
                Log.e("getPathAndFilename", "FEHLER: filePathAndName hat kein 'DB:' Präfix: " + filePathAndName);
                outputStringBuffer.append("\nFEHLER: Interner Fehler - Programme müssen aus Datenbank geladen werden!\n");
                return;
            }

            // Programmliste für den Tag einmal laden (für DB-Suche in der Schleife)
            List<Programm> programmeForDay = null;
            if (useDatabase && tagtypForDatabase != null) {
                try {
                    PlatinenDatabaseHelper dbHelper = PlatinenDatabaseHelper.getInstance(context);
                    programmeForDay = dbHelper.getProgrammeByTagtyp(tagtypForDatabase);
                    if (programmeForDay == null) programmeForDay = new java.util.ArrayList<Programm>();
                    Log.e("getPathAndFilename", "Programme für Tag geladen: " + programmeForDay.size() + " für " + tagtypForDatabase);
                } catch (Exception e) {
                    Log.e("getPathAndFilename", "Fehler beim Laden der Programme für " + tagtypForDatabase, e);
                    programmeForDay = new java.util.ArrayList<Programm>();
                }
            }

            String melodieString;

            checkMinute = 01;
            checkStunde = 0; // beginne bei 00:00 ende bei 23:59
            pathAndFileNameListForEditor.clear();
            indexForEditor.clear();
            buttonsIndex = 0 ;

            while (!((checkStunde == 00) && (checkMinute == 00))) {
                melodieString = BenutzerMelodienActivity.checkStartBenutzerMelodie(jahrNeu, monatNeu, tagNeu, checkStunde, checkMinute);

                if (!melodieString.equals("")) {
                    String beginnTemp = tagesSuche.getBeginnTime2(melodieString, jahrNeu, monatNeu, tagNeu, checkStunde, checkMinute);
                    String melodieAnzeige = melodieString.toLowerCase().endsWith(".xls") ? melodieString.substring(0, melodieString.length() - 4) : melodieString;
                    outputStringBuffer.append("\n" + tagesSuche.pad(checkStunde) + ":" + tagesSuche.pad(checkMinute) + " "
                            + "Benutzerprogramm " + melodieAnzeige + "  --> Beginn " + beginnTemp);

                    pathAndFileNameListForEditor.add(filePathAndName);
                    indexForEditor.add(StaticVariable.fileIndexForEditorTemp);
                    buttonsIndex++ ;
                }

                // Nur Normalprogramm/Feiertag prüfen, wenn in dieser Minute kein Benutzerprogramm gefunden wurde (Benutzerprogramm hat Priorität)
                if (useDatabase && programmeForDay != null && melodieString.equals("")) {
                    try {
                        melodieString = tagesSuche.checkProgrammliste2FromDatabase(jahrNeu, monatNeu, tagNeu, checkStunde, checkMinute, programmeForDay);
                    } catch (Exception e) {
                        Log.e("getPathAndFilename", "Fehler checkProgrammliste2FromDatabase", e);
                        melodieString = "";
                    }
                }


                if (!melodieString.equals("")) {
                    Log.i("startsWith", "=" + melodieString);
                    if (melodieString.startsWith("SA")) {
                        melodieString = new StringBuilder(melodieString).insert(2, " " + sonnenAufgangStringGerundet + " ").toString();
                    }

                    if (melodieString.startsWith("SU")) {
                        melodieString = new StringBuilder(melodieString).insert(2, " " + sonnenUntergangStringGerundet + " ").toString();
                    }
                    //String stringTemp = ("\n" + tagesSuche.pad(checkStunde) + ":" + tagesSuche.pad(checkMinute) + " " + melodieString) ;
                    //outputStringBuffer.append("\n" + tagesSuche.pad(checkStunde) + ":" + tagesSuche.pad(checkMinute) + " " + melodieString) ;
                    outputStringBuffer.append("\n" + melodieString);

                    //initEditButton(100,20,buttonsIndex,melodieString);
                    pathAndFileNameListForEditor.add(filePathAndName);
                    indexForEditor.add(StaticVariable.fileIndexForEditorTemp);
                    buttonsIndex++ ;
                    //DdpThread.sendProgrammAbfrage(stringTemp);
                }

                incrementCheckTime();
                //Log.e("CheckTime" , "= " + checkStunde + ":" + checkMinute) ;
            }

            //outputStringBuffer.append("\n\nSuche beendet.");
            outputStringBuffer.append("\n\n" + StaticVariable.getUebersetzung(16));
            outputStringBuffer.delete(0, stringBitteWarten.length());
            // Excel wird nicht mehr verwendet, daher kein closeWorkbook() mehr nötig


            printThreadReady = true ;

            //Log.i("PRINT" , "thread beendet") ;

        }
    } // ende vom ProgrammlisteThread

   /*
    public void initEditButton(int width, int height, int index, String buttonText) {
        Button editButton = new Button(context);
        editButtons.add(editButton);

        //sofortStartButtons.elementAt(index).setTextSize(width / 10.5f);
        //sofortStartButtons.elementAt(index).setTextSize(buchstabenGroesseTastenText2);
        editButtons.elementAt(index).setText(buttonText);

        editButton.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));

        //FrameLayout.LayoutParams flayoutParams =

        //        new FrameLayout.LayoutParams(width, height);
        //editButtons.elementAt(index).setLayoutParams(flayoutParams);

        //editButtons.elementAt(index).setTextColor(this.context.getResources().getColor(R.color.white));

        //LinearLayout.LayoutParams linearParams =
        //        new LinearLayout.LayoutParams(width,height) ;

//			   String buttontext = getButtonText(index_temp);

//			   buttons.elementAt(index).setText(buttontext);

        //flayoutParams.leftMargin = x;
        //flayoutParams.topMargin = y;
        //flayoutParams.gravity = Gravity.TOP + Gravity.LEFT;


        //linearLayout.addView(editButtons.elementAt(index));

    }
*/

    private void berechneSonnenAufUndUntergang(int sAsUjahr, int sAsUmonat, int sAsUtag) {
        PlatinenDatabaseHelper.AnlagenstandortConfig config = TurmtechnikActivity.loadAnlagenstandortConfig(this);
        if (config == null) return;

        String breitengrad = config.breitengrad != null ? config.breitengrad.replaceAll(",", ".") : "";
        String laengengrad = config.laengengrad != null ? config.laengengrad.replaceAll(",", ".") : "";
        String anpassungMinutenSonnenaufgang = config.anpassungSonnenaufgangMinuten != null ? config.anpassungSonnenaufgangMinuten : "0";
        String anpassungMinutenSonnenuntergang = config.anpassungSonnenuntergangMinuten != null ? config.anpassungSonnenuntergangMinuten : "0";
        String rundenString = config.rundenMinuten != null ? config.rundenMinuten : "5";
        if (breitengrad.isEmpty() || laengengrad.isEmpty()) return;

        Log.i("anlageStandort", "breitengrad=" + breitengrad);
        Log.i("anlageStandort", "laengengrad=" + laengengrad);
        Log.i("anlageStandort", "anpassung SA=" + anpassungMinutenSonnenaufgang);
        Log.i("anlageStandort", "anpassung SU=" + anpassungMinutenSonnenuntergang);
        Log.i("anlageStandort", "rundenString=" + rundenString);

        //sonnenAufgangBerechnen ;
        Location location = new Location(breitengrad, laengengrad);
        SunriseSunsetCalculator sunriseSunsetCalculator = new SunriseSunsetCalculator(location, TimeZone.getDefault());

        Calendar calendar = Calendar.getInstance();

        if ((sAsUmonat == 05) && (sAsUtag == 20))  // komischer Fehler Sonnenuntergang am 20.06.
        {
            sAsUtag = 21;
        }
        calendar.set(sAsUjahr, sAsUmonat, sAsUtag);


        Calendar officialSunrise = sunriseSunsetCalculator.getOfficialSunriseCalendarForDate(calendar);
        Calendar officialSunset = sunriseSunsetCalculator.getOfficialSunsetCalendarForDate(calendar);

        String stringSunriseOfficiale = sunriseSunsetCalculator.getOfficialSunriseForDate(calendar);
        sonnenAufgangString = stringSunriseOfficiale;
        String stringSunsetOfficiale = sunriseSunsetCalculator.getOfficialSunsetForDate(calendar);
        sonnenUntergangString = stringSunsetOfficiale;
        //String stringSunriseAstronomic = sunriseSunsetCalculator.getAstronomicalSunriseForDate(Calendar.getInstance()) ;
        //String stringSunsetAstronomic = sunriseSunsetCalculator.getAstronomicalSunsetForDate(Calendar.getInstance()) ;
        //Log.e("anlageStandort" , "Sonnenaufgang=" + officialSunrise) ;
        Log.i("anlageStandort", "Sonnenaufgang Officiale=" + stringSunriseOfficiale);
        //Log.e("anlageStandort" , "Sonnenaufgang Astronomic=" + stringSunriseAstronomic) ;
        //Log.e("anlageStandort" , "Sonnenuntergang=" + officialSunset) ;
        Log.i("anlageStandort", "Sonnenuntergang Officiale=" + stringSunsetOfficiale);
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
        Log.i("anlageStandort", "Sonnenaufgang Korrigiert=" + sonnenaufgangKorrigiert);

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
        Log.i("anlageStandort", "Sonnenuntergang Korrigiert=" + sonnenuntergangKorrigiert);

        //plusMinusBerechnen ;
        //wertRunden ;
        //sonnenUntergangBerechnen ;
        //plusMinusBerechnen ;
        //wertRunden ;

        sonnenAufgangStringGerundet = sonnenaufgangKorrigiert;
        sonnenUntergangStringGerundet = sonnenuntergangKorrigiert;
    }

    private long roundMintues(long number, long round) {
        long halfRound = round / 2;
        long nummberTemp = number;
        nummberTemp = nummberTemp + halfRound;
        nummberTemp = nummberTemp / round;
        nummberTemp = nummberTemp * round;
        return nummberTemp;
    }


    /* !!!! das alles ist jetzt in der Klasse TagesSuche
    private String checkProgrammlisteOLD(int checkMinute, int checkStunde)
    {

        String zeitString = "" ;

        int zeile = 3 ; // beginne bei der ersten zeit zelle in programmliste

        // Log.i("vor" , "get zeit");

        calendar = new GregorianCalendar(jahr,monat,tag) ; // teste fiktiven Tag

        int zeilen = excelread.getCellZeilen() ;
        aktuelleZeitZeilenListe.clear();

        while (zeile < zeilen)
        {
            try {
                zeitString = excelread.getCellString(0, zeile) ;

            } catch (ArrayIndexOutOfBoundsException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                new LogExcelError(0, zeile, filePathAndName, 0, sourceFileName, 281) ;
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                new LogExcelError(0, zeile, filePathAndName, 0 , sourceFileName, 285) ;
            }
            if(zeitString.equals("")) // Tabellen ende? = leere Zelle
            {
                break ;
            }
            String[] zeitSplit = zeitString.split("\\:");
            //Log.e("zeitSplit" , "lenght=" + zeitSplit.length) ;
            if(zeitSplit.length != 3)
            {
                new LogExcelError(0, zeile, filePathAndName, 0, sourceFileName, 295) ;
                outputStringBuffer.append("\n" + "Die Zeit Zelle hat kein Text Format") ;
            }
            else
            {
                int programmlisteMinute = Integer.parseInt(zeitSplit[1]);

                if (checkMinute == programmlisteMinute)
                {
                    // Log.d("minute", "passt");

                    int programmlisteStunde = Integer.parseInt(zeitSplit[0]);

                    if (checkStunde == programmlisteStunde)
                    {
                        //Log.d("stunde", "passt");
                        aktuelleZeitZeilenListe.add(zeile);
                    }
                }
            }

            zeile ++ ;
        }

        if (aktuelleZeitZeilenListe.size() > 0 )
        {
            return sucheMelodieString() ;
        }
        return "" ;  // nichts gefunden
    }
	
	private String sucheMelodieString()
		{
			String melodieString = "" ;
			
			int blockOffset = -1 ;
			
		
			for (int i = 0; i < aktuelleZeitZeilenListe.size(); i++)
			{
				//Log.i("start Zeit " , "index="+aktuelleZeitZeilenListe.elementAt(i) );
			}
			
			// jetzt auf prioritaet untersuchen
			
			int tempIndex = getIndexPrioritaet();
	 		//Log.i("prioritaet" , ""+tempIndex);
			// wenn prioritaet gefunden, schauen ob passt
			
			if  (zeileOk(aktuelleZeitZeilenListe.elementAt(tempIndex)))  // passt alles bei der Prioritaetszeile? // oder zeile 0
			{
				blockOffset = aktuelleZeitZeilenListe.elementAt(tempIndex);
				//Log.d("akt. block Nr.:" , "" + blockNummer);
				return getMelodieString(blockOffset) ;  // 
			}
				// wenn nicht aus startZeitenIndex entfernen
			else
			{
				aktuelleZeitZeilenListe.remove(tempIndex);
			}
			
			// und liste von oben nach unten suchen ob alles passt...
			// wochentag, verknuepfte Taste etc...
			blockOffset = -1 ;
			
			if  (aktuelleZeitZeilenListe.size()!=0)
			{
				//Log.i("startZeitenIndex" , "size=" + aktuelleZeitZeilenListe.size()) ;
				for (int i = 0; i < aktuelleZeitZeilenListe.size(); i++)
				{
					//Log.d("startZeitenIndex" , "i=" + i + "elementAt(i) = " + aktuelleZeitZeilenListe.elementAt(i));
					if (zeileOk(aktuelleZeitZeilenListe.elementAt(i)))
					{
						//Log.i("startZeitenIndex" , "elementAt(i) = " + aktuelleZeitZeilenListe.elementAt(i));
						blockOffset = aktuelleZeitZeilenListe.elementAt(i) ;
						return getMelodieString(blockOffset);
					}
				}
			}
			return melodieString ;
		} // ende sucheMelodieString
	
	private String getMelodieString(int offset)
	{
		String tempString = "" ;
		try {
			tempString = excelread.getCellString(1, offset) + " --> " ;
			tempString += excelread.getCellString(18, offset) ;
			return tempString ;
			
		} catch (ArrayIndexOutOfBoundsException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			new LogExcelError(11, offset, filePathAndName, 0, sourceFileName, 384) ;
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			new LogExcelError(11, offset, filePathAndName, 0, sourceFileName, 388) ;
		}
		return tempString ;
	}
	
	private int getIndexPrioritaet()
	{
		int i ;
		for ( i = 0 ; i < aktuelleZeitZeilenListe.size(); i++)
		{
			if (getPrioritaet(aktuelleZeitZeilenListe.elementAt(i)) > 1 )
			{
				return i ;
			}
		}
		if (i == aktuelleZeitZeilenListe.size())
		{
			//Log.d("keine" , "prioritaet");
			return 0 ;
		}
		return 0 ;
	}	
	
	private int getPrioritaet(int localZeile)
	{
		try {
			return Integer.parseInt(excelread.getCellString(17, localZeile));
		} catch (NumberFormatException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			new LogExcelError(0, localZeile, filePathAndName, 0, sourceFileName, 418) ;
		} catch (ArrayIndexOutOfBoundsException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			new LogExcelError(0, localZeile, filePathAndName, 0, sourceFileName, 422) ;
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			new LogExcelError(0, localZeile, filePathAndName, 0, sourceFileName, 426) ;
		}
		return 0 ;
	}
	
	private boolean zeileOk(int blockIndex)
	{
		// WICHTIG: Die Wochentag-Prüfung wird IMMER durchgeführt, auch für Feiertage!
		// An Feiertagen gilt das gleiche wie an normalen Programmtagen - nur der Tagtyp ist anders.
		// Die Wochentag-Prüfung wird NICHT übersprungen.
		if (!wochenTagOk(blockIndex))
		{
			Log.e("Wochentag" , "passt nicht ") ;
			return false ;
		}
		
		if (!verknuepfteTasteOn(blockIndex))
		{	
			
			Log.e("verkn. Taste" , "ist nicht ON" );
			return false ;
		}
		
		if (!periodischSommerWinterImmer(blockIndex))
		{
			Log.e("Sommer Winter" , "passt nicht");
			return false ;
		}
		if (!startEndeDatum(blockIndex))
		{
			Log.e("Start Ende" , "passt nicht");
			return false ;
		}
			
			
			
		// eventuell weitere pruefungen
	
		return true ;
	}
	
	private boolean startEndeDatum(int zeile)
	{
		try {
			if (excelread.getCellString(11, zeile).equals("") ) // ist eine Start zeit angegeben?
			{
				// Log.i("keine" , "Startzeit");
				return true ;  // keine startzeit, o.k. melden
			}
			else
			{
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
			new LogExcelError(11, zeile, filePathAndName, 0, sourceFileName, 501) ;
		} catch (NumberFormatException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			new LogExcelError(11, zeile, filePathAndName, 0, sourceFileName, 505) ;
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			new LogExcelError(11, zeile, filePathAndName, 0, sourceFileName, 509) ;
		}
		return false;
		
	}

	
	private boolean checkDatumBetween(int startZeitTag, int startZeitMonat, int stopZeitTag, int stopZeitMonat)
	{
		int tag = calendar.get(Calendar.DAY_OF_MONTH);
		//Log.d("DAY_OF_MONTH" , "= " + tag) ;
		int monat = (calendar.get(Calendar.MONTH))+1; // monate beginne im Kalender bei 0
		//Log.d("MONTH" , "= " + monat) ;
		
		Log.e("DAY_OF_MONTH" , "=" + tag) ;
		Log.e("MONTH" , "= " + monat) ;
		Log.e("startZeitTag" , "=" + startZeitTag) ;
		Log.e("startZeitMonat" , "=" + startZeitMonat) ;
		Log.e("stopZeitTag" , "=" + stopZeitTag) ;
		Log.e("stopZeitMonat" , "=" + stopZeitMonat) ;
		
		if ( (monat == startZeitMonat) && (monat == stopZeitMonat) )
		{
			return  ( (tag >= startZeitTag) && tag <= stopZeitTag) ;
		}
		else
		{
			// die Monate sind nicht gleich:
			if (monat == startZeitMonat)
			{
				return tag >= startZeitTag ;
			}
			if (monat == stopZeitMonat)
			{
				return tag <= stopZeitTag ;
			}
			
			// wenn Monat start und ende nicht gleich sind 
			// und das Monat ist weder startMonat noch stopMonat:
				
			do
			{
				// jedes weitere monat anschauen
				monat ++ ;
				if (monat > 12)
				{
					monat = 1 ;
				}
								
				if (monat == startZeitMonat)
				{
					// treffe ich zuerst auf start Monat bin ich nicht zwischen start und ende
					return false ;
				}
				
			}
			while ( monat != stopZeitMonat) ;
			// treffe ich auf das stopMonat bin ich dazwischen
			return true ;
		}
	}
	
	
	private boolean periodischSommerWinterImmer(int zeile)
	{
		String swi = "";
		try {
			swi = (excelread.getCellString(10, zeile).trim());
		} catch (ArrayIndexOutOfBoundsException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			new LogExcelError(10, zeile, filePathAndName, 0, sourceFileName, 611) ;
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			new LogExcelError(10, zeile, filePathAndName, 0, sourceFileName, 615) ;
		}
		
		//Log.i("Periodisch" , "=" + swi);
		
//		if (swi.equals("1"));
//		{
//			Log.i("swi","= true");
//		}
		
		//Log.e("swi" , "=" + swi ) ;
		
		if (swi.equals("1") && (!isSommerzeit()))
		{
			return false ;   // fehler  wenn Programmliste = sommerzeit und datum = winterzeit
		}
		
		if (swi.equals("2") && (isSommerzeit()))
		{
			return false ;  // fehler wenn winterzeit gewuenscht aber es ist sommerzeit   
		}
		
		//Log.d("Sommer/Winter","keine");
		return true  ;  // nicht sommerzeit und auch nicht winterzeit = immer
	}
	
	private boolean isSommerzeit()
	{
		boolean temp ;
	//	temp =  calendar.getTimeZone().inDaylightTime(calendar.getTime());
	//	temp = calendar.getTimeZone().inDaylightTime(calendar.getTime());
		TimeZone cet = TimeZone.getTimeZone("CET");
		temp = cet.inDaylightTime(calendar.getTime());
		
		//Log.i("Sommerzeit" , "=" + temp );
		
		return temp ;
	}
	
	private boolean verknuepfteTasteOn(int zeile)
	{
		try {
			if ( excelread.getCellString(14, zeile).trim() != "" )  // gibts ueberhaupt eine verkn. Taste?
			{
				if ( UhrThread.verknuepfteTastenString.size() > 0 ) // wurde aktivity 2 ueberhaupt gestartet?
				{
					for (int i = 0; i < UhrThread.verknuepfteTastenString.size(); i++)
					{
						String activity2String = UhrThread.verknuepfteTastenString.elementAt(i) != null ? (UhrThread.verknuepfteTastenString.elementAt(i)).trim() : "";
						String programmlisteString = (excelread.getCellString(14 , zeile)).trim();
//					Log.i("activity2String" , "" + activity2String);
//					Log.i("proglisteString" , "" + programmlisteString);
								if (activity2String.equalsIgnoreCase(programmlisteString)) // wenn Taste vorhanden, ist sie ON?
						{
//						Log.d("die 2 strings" , "passen"); 
//						Log.i("verkn. Taste", "" + TurmtechnikActivity.verknuepfteTastenOn[i]);
							if (TurmtechnikActivity.verknuepfteTastenOn != null && i < TurmtechnikActivity.verknuepfteTastenOn.length && Boolean.TRUE.equals(TurmtechnikActivity.verknuepfteTastenOn[i]))
							{
								//Log.i("verkn. Taste" , "" + activity2String + "= true" );
								//if(!immerOn(zeile))
								//{
								//	if (!prognoseModus)
								//	{
								//		TurmtechnikActivity.verknuepfteTastenOn[i]=false ;
									// aber wie das anzeigen wenn gerade Seite2Activity laeft ?
								//	}
								//}
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
			new LogExcelError(14, zeile, filePathAndName, 0, sourceFileName, 709) ;
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			new LogExcelError(14, zeile, filePathAndName, 0, sourceFileName, 713) ;
		}
		return false;
	}
	
	 private boolean wochenTagOk(int zeile)
	    {
	    	//Log.i("check" , "wochenTagOk=" + zeile);
	    	
		   
		 
		    int wochentag = calendar.get(Calendar.DAY_OF_WEEK);
	    		    	
	    	if (wochentag==1)
	    	{
	    		wochentag=6;
	    	}
	    	else 
	    	{
	    		wochentag=wochentag-2;  // Montag == 0
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
				new LogExcelError(wochentag + spalteWochentagStart, zeile, filePathAndName, 0, sourceFileName, 742) ;
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				new LogExcelError(11, zeile, filePathAndName, 0, sourceFileName, 746) ;
			}
	    	//Log.d("programm Liste" + wochentag, "wochentag="+programmlisteWochentag);
	    	if   (programmlisteWochentag=="") // kein x beim Wochentag?
	    	{
	    		return false ;
	    	}
	    	else 
	    	{
	    		return true ;
	    	}
	    }

     */  // !!!! das alles ist jetzt in der Klasse TagesSuche  16.1.2015

    private void incrementCheckTime() {
        // Log.d("incremet" , "serach time");

        //if ( (checkMinute == 59) && (checkStunde == 23) )
        if ((checkMinute == 00) && (checkStunde == 00)) {
            return; // bei 23:59 stehen bleiben
        }

        checkMinute++;
        if (checkMinute > 59) {
            checkMinute = 0;
            checkStunde++;
            if (checkStunde > 23) {
                checkStunde = 0;
            }
        }
    }


    private void startSetNebenuhr() {
        Intent activitySetNebenuhr = new Intent(ProgrammKontrolleActivity.this, SetNebenuhrActivity.class);
        ProgrammKontrolleActivity.this.startActivity(activitySetNebenuhr);
    }

    private void getDisplayParameter() {
        Display display = this.getWindowManager().getDefaultDisplay();
        displayWidth = display.getWidth();
        displayHeight = display.getHeight();
    }


    // das braucht einen thread in main thread ... wegen Aenderungen in der View
    public void startUpdateInfoText() {
        doRun = true;
        Runnable runnable = new Runnable() {

            @Override
            public void run() {

                while (doRun)
                {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                    handler.post(new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            // Programmsuche schließen, wenn nur noch ≤2 s bis zum nächsten Programmstart (damit Programm sicher startet)
                            long beginnMs = UhrThread.nextProgrammBeginnMsPublic;
                            if (beginnMs > 0) {
                                long restMs = beginnMs - System.currentTimeMillis();
                                if (restMs >= 0 && restMs <= 2000) {
                                    StaticVariable.programmAbfrageAktiv2 = false;
                                    finish();
                                    return;
                                }
                            }
                            if (newDate)
                            {
                                printThreadReady = false ;
                                outputStringBuffer.delete(0, outputStringBuffer.length());
                                printProgrammlisteThread printThread = new printProgrammlisteThread();
                                printThread.start();
                                // printProgrammListe();
                                // Log.i("PRINT" , "Programmliste") ;
                                newDate = false;
                            }

                            textView.setText((outputStringBuffer).toString());


                            if(printThreadReady)
                            {
                                //linearLayout.removeAllViews();

//                                for (int i = 0; i < editButtons.size(); i++)
//                                {
//                                    linearLayout.addView(editButtons.elementAt(i));
//                                }
//
//                                for (int ix = 0; ix < editButtons.size(); ix++) {
//
//                                    onClickIndex = ix;
//
//                                    editButtons.elementAt(ix).setOnClickListener
//                                            (new OnClickListener() {
//
//                                                int localIndex = onClickIndex;
//
//                                                @Override
//                                                public void onClick(View v)
//                                                {
//
//                                                    globalIndex = localIndex;
//
//                                                    Log.e("programmKontrolle" , "globalIndex=" + globalIndex) ;
//
//                                                    StaticVariable.pathAndFilenameEditor = pathAndFileNameListForEditor.get(globalIndex) ;
//                                                    StaticVariable.pathAndFilenameEditorIndex = indexForEditor.get(globalIndex) ;
//
//                                                    StartTurmtechnikService.setTimeTurmtechnik(60 * 15); // 15 Minuten
//
//                                                    Intent startEditor = new Intent(ProgrammKontrolleActivity.this,
//                                                            EditorActivity.class);
//                                                    ProgrammKontrolleActivity.this.startActivity(startEditor);
//
//                                                    finish();
//                                                }
//
//
//                                            });
                                //} // ende von for

                                //editButton = new Button(context);

                                //editButton.setText("Editor") ;
                                //editButton.setTextColor(context.getResources().getColor(R.color.white));
                               // editButton.setBackgroundColor(context.getResources().getColor(R.color.black));


                               // editButton.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,
                              //          LinearLayout.LayoutParams.WRAP_CONTENT));

                            //FrameLayout.LayoutParams flayoutParams =

                            //       new FrameLayout.LayoutParams(200, 100);
                            //editButtons.elementAt(index).setLayoutParams(flayoutParams);

                            //editButtons.elementAt(index).setTextColor(this.context.getResources().getColor(R.color.white));

                            //LinearLayout.LayoutParams linearParams =
                            //        new LinearLayout.LayoutParams(width,height) ;

//			   String buttontext = getButtonText(index_temp);

//			   buttons.elementAt(index).setText(buttontext);

                            //flayoutParams.leftMargin = x;
                            //flayoutParams.topMargin = y;
                            //flayoutParams.gravity = Gravity.TOP + Gravity.LEFT;


                            //linearLayout.addView(editButtons.elementAt(index));
                                //linearLayout.addView(editButton);

                               // editButton = (Button) findViewById(R.id.button_editor) ;

                               // buttonEdit.setText("Editor") ;
                                //editButton.setTextColor(context.getResources().getColor(R.color.white));
                                // editButton.setBackgroundColor(context.getResources().getColor(R.color.black));

//                                buttonEdit.setOnClickListener(new OnClickListener()
//                                {
//                                              @Override
//                                              public void onClick(View v)
//                                              {
//                                                   //StaticVariable.pathAndFilenameEditor = pathAndFileNameListForEditor.get(0);
//                                                   //StaticVariable.pathAndFilenameEditorIndex = indexForEditor.get(0);
//
//                                                  //pathAndFileNameListForEditor.add(filePathAndName);
//                                                  //indexForEditor.add(StaticVariable.fileIndexForEditorTemp);
//
//                                                   StaticVariable.pathAndFilenameEditor = filePathAndName ;
//                                                   StaticVariable.pathAndFilenameEditorIndex = 3 ; // erste editierbare Zeile im Normalprogramm.xls
//
//                                                   StartTurmtechnikService.setTimeTurmtechnik(60 * 15); // 15 Minuten                                                    Intent startEditor = new Intent(ProgrammKontrolleActivity.this,
//
//                                                   Intent startEditor = new Intent(ProgrammKontrolleActivity.this, EditorActivity.class);
//                                                   ProgrammKontrolleActivity.this.startActivity(startEditor);
//
//                                                   finish();
//                                              }
//                                });

                                printThreadReady = false;

                            }

                        }
                    });
                }
            }
        };
        new Thread(runnable).start();
    }


    private DatePickerDialog.OnDateSetListener datePickerListener = new DatePickerDialog.OnDateSetListener() {


        @Override
        public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {

            //Log.i("Tag" , "= " +  dayOfMonth) ;
            //Log.i("Monat" , "= " + monthOfYear) ;
            //Log.i("Jahr" , "= " + year) ;

            jahrNeu = year;
            monatNeu = monthOfYear; // DatePicker gibt bereits 0-basiert zurück (0=Januar, 11=Dezember), genau wie Calendar.MONTH
            tagNeu = dayOfMonth;
            beschrifteButton(jahrNeu, monatNeu, tagNeu);

            newDate = true;
            // printProgrammListe() ;
        }
    };

    private void beschrifteButton(int jahr, int monat, int tag) {
        //btDate.setText("Datum auswählen: " + tagesSuche.pad(tag) + "." + tagesSuche.pad(monat + 1) + "." + jahr);
        btDate.setText( StaticVariable.getUebersetzung(13) + " " + tagesSuche.pad(tag) + "." + tagesSuche.pad(monat + 1) + "." + jahr );
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        // DatePickerDialog erwartet 0-basierten Monat (0=Januar, 11=Dezember), genau wie Calendar.MONTH
        return new DatePickerDialog(this, datePickerListener, jahrNeu, monatNeu, tagNeu);
    }


    //private static String pad(int c) {
    //		if (c >= 10)
    //			return String.valueOf(c);
    //		else
    //			return "0" + String.valueOf(c);
    //    }

    private void endActivityProgrammKontrolle() {
        // setStaticVariable();

        //  	setWartenLaufen();
        //  	excelread.closeWorkbook();
        //  	doRun = false ;
        //layout = null ;
        StaticVariable.programmAbfrageAktiv2 = false;
        System.gc();
        finish();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if ((keyCode == KeyEvent.KEYCODE_BACK)) {
            //Log.i("Acitivity 3", "back");

            // finish();
            endActivityProgrammKontrolle();
            return true;
        }

        return false;
    }

    private void startHelpSeite() {
//	    	String helpFileName = "" ;
//	    	// lese Help File Name
//	    	ExcelRead excelRead = new ExcelRead() ;
//	    	excelRead.openXlsSheet(sdCardPath + "/Turmtechnik/Config/System.xls", BESCHRIFTUNG_GLOCKEN_SHEET);
//	    	int max_zeilen = excelRead.getCellZeilen() ;
//	    	for (int i = 1 ; i < max_zeilen; i ++)
//	    	{
//	    		if(excelRead.getCellString(3, i).equals("103"))
//	    		{
//	    			// wenn die Help Taste gefunden ist:
//	    			//Log.i("103" , "gefunden") ;
//	    			//Log.i("index i" , "=" + i) ;
//	    			helpFileName = (excelRead.getCellString(1, i));
//	    			//Log.i("helpFileName" , "=" + helpFileName) ;
//	    			break ;
//	    		}
//	    	}
//	    	excelRead.closeWorkbook() ;

        Intent activityHelpSeite = new Intent(this, HelpSeiteActivity.class);

        //Log.i("helpFileName" , "=" + helpFileName) ;
        String helpFileName = "help4.png";

        activityHelpSeite.putExtra("help_file_name", helpFileName);

        this.startActivity(activityHelpSeite);
    }


//    @Override
//    public boolean onCreateOptionsMenu(Menu menu) {
//        getMenuInflater().inflate(R.menu.activity_turmtechnik, menu);
//        return true;
//    }


} // ende der Klasse
