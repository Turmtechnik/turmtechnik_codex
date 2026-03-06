package tom.turmtechnik;

import java.io.File;
import java.util.Calendar;
import java.util.Random;

import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.StrictMode;
import android.app.Activity;
import android.app.Dialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
//import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.DigitalClock;
import android.widget.RelativeLayout;
import android.widget.SlidingDrawer;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.AbsoluteLayout;
import android.widget.AbsoluteLayout.LayoutParams;
import android.view.View.OnClickListener;


public class SetNebenuhrActivity extends Activity {
    private final String sourceFileName = "SetNebenuhrActivity";

    static final int TIME_DIALOG_ID = 999;

    private TextView outputView;
    private boolean file_ok = false;
    private int index;
    private SetNebenuhrLayout layout;

    private int hour;
    private int minute;
    private int globalIndex;
    private boolean doRun = true;

    private Handler handler;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.text_layout);

        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        getDisplayParameter();
        //Log.i("displayWidth" , "=" + displayWidth) ;
        //Log.i("displayHeight" , "=" + displayHeight) ;

        outputView = (TextView) this.findViewById(R.id.textView1);
        outputView.append("\n\n");

        outputView = null;
        System.gc();

        layout = new SetNebenuhrLayout(getApplicationContext());

        try {
            setContentView(layout.initLayout());
        } catch (Exception e) {
            android.util.Log.e("SetNebenuhrActivity", "Layout konnte nicht aufgebaut werden", e);
            if (outputView != null) {
                outputView.append("\nFehler Nebenuhr-Layout: " + (e.getMessage() != null ? e.getMessage() : "") + "\nBitte Web-UI oder Beschriftung Tasten prüfen.");
            }
            return;
        }

        layout.pfeilLinksButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                // TODO Auto-generated method stub
                //finish();
                endActivitiSeite3();
            }
        });
        
/*
           layout.checkRebootButton.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View arg0) {
				// TODO Auto-generated method stub
				rebootSU(); 
			}
		});
*/
        layout.helpButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                // TODO Auto-generated method stub
                //finish();
                //endActivitiSeite3();
                startHelpSeite();
            }
        });


        for (int ix = 0; ix < layout.buttons.size() && ix < 4; ix++) { // Maximal 4 Uhren (A, B, C, D)

            index = ix;

            layout.buttons.elementAt(ix).setOnClickListener
                    (new OnClickListener() {

                        int localIndex = index;

                        @Override
                        public void onClick(View v) {
//               				layout.nebenuhrMinute[localIndex]++ ;
//               				layout.nebenuhrStunde[localIndex]++ ;

                            globalIndex = localIndex;

                            // Während Zeiteingabe: Nebenuhr steht – weder laufen noch aufholen
                            if (localIndex < 3 && localIndex < StaticVariable.uhr_warten.length) {
                                StaticVariable.uhr_warten[localIndex] = true;
                                if (localIndex < StaticVariable.uhr_zeiteingabeAktiv.length) {
                                    StaticVariable.uhr_zeiteingabeAktiv[localIndex] = true;
                                }
                                if (localIndex == 0) StaticVariable.uhrA_doRun = false;
                                else if (localIndex == 1) StaticVariable.uhrB_doRun = false;
                                else if (localIndex == 2) StaticVariable.uhrC_doRun = false;
                            }
                            if (localIndex == 3) {
                                StaticVariable.uhrD_doRun = false;
                                if (localIndex < StaticVariable.uhr_zeiteingabeAktiv.length) {
                                    StaticVariable.uhr_zeiteingabeAktiv[localIndex] = true;
                                }
                            }
                            // Prüfe ob Dialog-ID gültig ist (0-3 für A, B, C, D)
                            if (localIndex >= 0 && localIndex <= 3) {
                                showDialog(localIndex);
                            } else {
                                android.util.Log.e("SetNebenuhrActivity", "Ungültiger Dialog-Index: " + localIndex);
                            }


                            //layout.makeTextButton(localIndex) ;
                        }
                    });
        } // ende von for

        // thread fuer update infotext erzeugen
        handler = new Handler();
        startUpdateInfoText();

    } // ende von onCreate

    @Override
    protected void onResume() {
        super.onResume();
        setWartenLaufen();
    }

    // das braucht einen thread in main thread ... wegen Aenderungen in der View
    public void startUpdateInfoText() {
        Runnable runnable = new Runnable() {

            @Override
            public void run() {

                while (doRun) {
                    try {
                        Thread.sleep(250);
                    } catch (InterruptedException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            layout.printTasten();
                        }
                    });
                }
            }
        };
        new Thread(runnable).start();
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        switch (id) {
            case 0:
                //Log.i("global" , "index=" + 0) ;
                int hour_0 = layout.nebenuhrStunde[0];
                int minute_0 = layout.nebenuhrMinute[0];
                boolean local_flag_24_0 = layout.flag_24[0]; //[globalIndex] ; // false = 12 stunden modus
                // set time picker as current time
                return new TimePickerDialog(this, timePickerListener, hour_0, minute_0, local_flag_24_0);

            case 1:
                //Log.i("global" , "index=" + 1) ;
                int hour_1 = layout.nebenuhrStunde[1];
                int minute_1 = layout.nebenuhrMinute[1];
                boolean local_flag_24_1 = layout.flag_24[1]; //[globalIndex] ; // false = 12 stunden modus
                // set time picker as current time
                return new TimePickerDialog(this, timePickerListener, hour_1, minute_1, local_flag_24_1);

            case 2:
                //Log.i("global" , "index=" + 2) ;
                int hour_2 = layout.nebenuhrStunde[2];
                int minute_2 = layout.nebenuhrMinute[2];
                boolean local_flag_24_2 = layout.flag_24[2]; //[globalIndex] ; // false = 12 stunden modus
                // set time picker as current time
                return new TimePickerDialog(this, timePickerListener, hour_2, minute_2, local_flag_24_2);

            case 3:
                // Monduhr D: Zeige Mondphase-Dialog mit SeekBar und Mondanzeige
                return createMondtageDialog(3);

        }
        return null;
    }

    @Override
    protected void onPrepareDialog(int id, Dialog dialog) {
        super.onPrepareDialog(id, dialog);
        if (dialog != null) {
            final int dialogIndex = id;
            dialog.setOnDismissListener(new android.content.DialogInterface.OnDismissListener() {
                @Override
                public void onDismiss(android.content.DialogInterface d) {
                    if (dialogIndex < StaticVariable.uhr_zeiteingabeAktiv.length) {
                        StaticVariable.uhr_zeiteingabeAktiv[dialogIndex] = false;
                    }
                    if (dialogIndex == 3) {
                        StaticVariable.uhrD_doRun = true;
                    }
                    setWartenLaufen();
                    startNebenuhrAufholenSofort();
                }
            });
        }
    }

    /**
     * Erstellt einen Dialog für die Eingabe der Mondphase mit SeekBar (0-59 Impulse) und Mondanzeige.
     * Mondgröße wird begrenzt, damit auf kleinen Bildschirmen (z. B. Android 5) der Schieberegler sichtbar bleibt.
     */
    private Dialog createMondtageDialog(int index) {
        // Aktuelle Mondphase (0-59 Impulse)
        int mondphase = StaticVariable.uhrD_mondphaseIst;
        if (mondphase > 59) mondphase = 59;
        if (mondphase < 0) mondphase = 0;
        
        // Mondgröße in % der Bildschirmhöhe – bei allen Auflösungen gleiche Aufteilung
        int screenHeight = getDisplayHeight();
        final int moonSizePxClamped = Math.max(80, Math.min(280, (int)(screenHeight * StaticConstants.MOND_SIZE_PERCENT_DIALOG)));
        
        // Layout für Dialog
        android.widget.LinearLayout dialogLayout = new android.widget.LinearLayout(this);
        dialogLayout.setOrientation(android.widget.LinearLayout.VERTICAL);
        dialogLayout.setPadding(50, 50, 50, 50);
        
        // Titel-Text
        android.widget.TextView titleText = new android.widget.TextView(this);
        titleText.setText("🌙 Monduhr einstellen");
        titleText.setTextSize(18);
        titleText.setPadding(0, 0, 0, 20);
        titleText.setGravity(android.view.Gravity.CENTER);
        dialogLayout.addView(titleText);
        
        // Mondanzeige (ImageView mit Canvas) – feste, begrenzte Größe (dp-basiert)
        final android.widget.ImageView moonImageView = new android.widget.ImageView(this);
        moonImageView.setLayoutParams(new android.widget.LinearLayout.LayoutParams(moonSizePxClamped, moonSizePxClamped));
        moonImageView.setScaleType(android.widget.ImageView.ScaleType.FIT_CENTER);
        moonImageView.setPadding(0, 0, 0, 20);
        dialogLayout.addView(moonImageView);
        
        // Aktualisiere Mondanzeige mit aktueller Mondphase (Größe übergeben)
        drawMoonOnImageView(moonImageView, mondphase, moonSizePxClamped);
        
        // Anzeige des aktuellen Impuls-Werts
        final android.widget.TextView valueText = new android.widget.TextView(this);
        valueText.setText("Impuls: " + mondphase);
        valueText.setTextSize(16);
        valueText.setPadding(0, 0, 0, 10);
        valueText.setGravity(android.view.Gravity.CENTER);
        dialogLayout.addView(valueText);
        
        // SeekBar für Impulse (0-59)
        final android.widget.SeekBar seekBar = new android.widget.SeekBar(this);
        seekBar.setMax(59);
        seekBar.setProgress(mondphase);
        seekBar.setPadding(0, 0, 0, 20);
        
        // Listener für SeekBar-Änderungen
        seekBar.setOnSeekBarChangeListener(new android.widget.SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(android.widget.SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    // Aktualisiere Mondanzeige und Text
                    drawMoonOnImageView(moonImageView, progress, moonSizePxClamped);
                    valueText.setText("Impuls: " + progress);
                }
            }
            
            @Override
            public void onStartTrackingTouch(android.widget.SeekBar seekBar) {
                // Nicht benötigt
            }
            
            @Override
            public void onStopTrackingTouch(android.widget.SeekBar seekBar) {
                // Nicht benötigt
            }
        });
        
        dialogLayout.addView(seekBar);
        
        // In ScrollView packen, damit auf kleinen Bildschirmen der Schieberegler erreichbar bleibt
        android.widget.ScrollView scrollView = new android.widget.ScrollView(this);
        scrollView.addView(dialogLayout);
        
        // Erstelle AlertDialog
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("🌙 Monduhr einstellen");
        builder.setView(scrollView);
        builder.setPositiveButton("OK", new android.content.DialogInterface.OnClickListener() {
            @Override
            public void onClick(android.content.DialogInterface dialog, int which) {
                int selectedImpulse = seekBar.getProgress();
                if (selectedImpulse > 59) selectedImpulse = 59;
                if (selectedImpulse < 0) selectedImpulse = 0;
                
                StaticVariable.uhrD_mondphaseIst = selectedImpulse;
                
                // Speichere in Datenbank
                PlatinenDatabaseHelper dbHelper = PlatinenDatabaseHelper.getInstance(TurmtechnikActivity.turmtechnikContext);
                PlatinenDatabaseHelper.NebenuhrConfig config = dbHelper.getNebenuhrByZeile(6);
                if (config != null) {
                    config.mondphaseIst = selectedImpulse;
                    dbHelper.saveNebenuhr(config);
                }
                
                // Aktualisiere Button-Anzeige
                layout.makeTextButton(3);
                
                // Aktualisiere Mondanzeige auf Layout1
                Seite1Layout.printMondAnzeige();
            }
        });
        builder.setNegativeButton("Abbrechen", null);
        
        return builder.create();
    }
    
    /**
     * Zeichnet die Mondphase auf einem ImageView basierend auf Impulsen (0-59).
     * @param sizePx Größe des Mondes in Pixel (für kleine Bildschirme begrenzt, damit SeekBar sichtbar bleibt)
     */
    private void drawMoonOnImageView(android.widget.ImageView imageView, int impulse, int sizePx) {
        if (imageView == null) {
            return;
        }
        if (sizePx <= 0) sizePx = 150;
        try {
            // Normalisiere Impuls auf 0-59 Bereich
            impulse = impulse % 60;
            if (impulse < 0) impulse += 60;
            
            int width = sizePx;
            int height = sizePx;
            
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
            
            // 3. Zeichnen der beleuchteten Fläche
            android.graphics.Paint lightPaint = new android.graphics.Paint();
            lightPaint.setColor(0xFFFFF1C1); // #FFF1C1 Mondlicht-Farbe
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
            
            // 4. Optional: Rand für bessere Sichtbarkeit
            android.graphics.Paint strokePaint = new android.graphics.Paint();
            strokePaint.setColor(0xFF555555); // #555
            strokePaint.setStyle(android.graphics.Paint.Style.STROKE);
            strokePaint.setStrokeWidth(2);
            strokePaint.setAntiAlias(true);
            canvas.drawCircle(cx, cy, r, strokePaint);
            
            // Setze das Bitmap
            imageView.setImageBitmap(bitmap);
            
        } catch (Exception e) {
            android.util.Log.e("SetNebenuhrActivity", "Fehler beim Zeichnen der Mondphase: " + e.getMessage(), e);
        }
    }
    
    /**
     * Erzeugt ein Drawable mit der Mondphase für die Taste D im Nebenuhr-Layout.
     * @param context Context für Resources
     * @param phase Mondphase 0–59 (Impulse)
     * @param sizePx Größe in Pixel
     * @return Drawable oder null bei Fehler
     */
    public static android.graphics.drawable.Drawable createMoonPhaseDrawable(android.content.Context context, int phase, int sizePx) {
        if (sizePx <= 0) sizePx = 72;
        phase = phase % 60;
        if (phase < 0) phase += 60;
        try {
            int width = sizePx;
            int height = sizePx;
            android.graphics.Bitmap bitmap = android.graphics.Bitmap.createBitmap(width, height, android.graphics.Bitmap.Config.ARGB_8888);
            android.graphics.Canvas canvas = new android.graphics.Canvas(bitmap);
            int r = Math.min(width, height) / 2 - 4;
            int cx = width / 2;
            int cy = height / 2;
            android.graphics.Paint darkPaint = new android.graphics.Paint();
            darkPaint.setColor(0xFF222222);
            darkPaint.setAntiAlias(true);
            canvas.drawCircle(cx, cy, r, darkPaint);
            double phaseVal;
            if (phase <= 30) {
                phaseVal = phase / 30.0;
            } else {
                phaseVal = 1.0 - ((phase - 30) / 30.0);
                if (phaseVal < 0) phaseVal = 0;
            }
            android.graphics.Paint lightPaint = new android.graphics.Paint();
            lightPaint.setColor(0xFFFFF1C1);
            lightPaint.setAntiAlias(true);
            for (int i = -r; i <= r; i++) {
                double y = Math.sqrt(r * r - i * i);
                if (Double.isNaN(y)) continue;
                int xLeft = cx - (int) y;
                int xRight = cx + (int) y;
                if (phase <= 30) {
                    int xLightEdge = xRight - (int) ((xRight - xLeft) * phaseVal);
                    canvas.drawRect(xLightEdge, cy + i, xRight, cy + i + 1, lightPaint);
                } else {
                    int xLightEdge = xLeft + (int) ((xRight - xLeft) * phaseVal);
                    canvas.drawRect(xLeft, cy + i, xLightEdge, cy + i + 1, lightPaint);
                }
            }
            android.graphics.Paint strokePaint = new android.graphics.Paint();
            strokePaint.setColor(0xFF555555);
            strokePaint.setStyle(android.graphics.Paint.Style.STROKE);
            strokePaint.setStrokeWidth(2);
            strokePaint.setAntiAlias(true);
            canvas.drawCircle(cx, cy, r, strokePaint);
            return new android.graphics.drawable.BitmapDrawable(context.getResources(), bitmap);
        } catch (Exception e) {
            android.util.Log.e("SetNebenuhrActivity", "createMoonPhaseDrawable: " + e.getMessage(), e);
            return null;
        }
    }

    private TimePickerDialog.OnTimeSetListener timePickerListener = new TimePickerDialog.OnTimeSetListener() {


        public void onTimeSet(TimePicker view, int selectedHour,
                              int selectedMinute) {
            // Monduhr D (index 3) wird nicht hier behandelt, sondern im Dialog
            if (globalIndex == 3) {
                return;
            }
            if (globalIndex < StaticVariable.uhr_zeiteingabeAktiv.length) {
                StaticVariable.uhr_zeiteingabeAktiv[globalIndex] = false;
            }
            if (selectedHour > 11) {
                selectedHour -= 12;
            }
            layout.nebenuhrStunde[globalIndex] = selectedHour;
            layout.nebenuhrMinute[globalIndex] = selectedMinute;

            setOneStaticVariable(globalIndex);
            layout.makeTextButton(globalIndex);
            // Monduhr D (index 3) hat keine Warten/Takten-Logik, nur Mondphase
            if (globalIndex != 3) {
                setWartenLaufen();
                startNebenuhrAufholenSofort();
            }
            //        StaticVariable.uhr_warten[globalIndex] = warten_laufen(selectedHour, selectedMinute);
            //        Log.i("Uhr"+globalIndex , "warten = " + StaticVariable.uhr_warten[globalIndex]);
        }
    };
//	   
//   	   private boolean warten_laufen(int selectedHour, int selectedMinute)
//   	   {
//   		   int angezeigteZeit = (selectedHour * 60)+selectedMinute ;
//   	       Calendar	calendar = Calendar.getInstance();
//    	   int momentaneZeit = (calendar.get(Calendar.HOUR)*60) + calendar.get(Calendar.MINUTE) ;
//    	   Log.i("----" , "warten / laufen") ;
//    	   Log.i("angezeigt" , "" + angezeigteZeit) ;
//    	   Log.i("momentane" , "" + momentaneZeit) ;
//    	   if (angezeigteZeit < momentaneZeit)
//    	   {
//    		   return false ;  // Turm Uhr soll laufen  
//    	   }
//    	   else
//    	   {
//    		   return true  ; //  Turm Uhr soll warten
//    	   }
//       }

//   	   	private boolean warten_laufen(int selectedHour, int selectedMinute)
//   	   	{
//   	   		int angezeigteZeit = (selectedHour * 60)+selectedMinute ;
//   	   		Calendar	calendar = Calendar.getInstance();
//   	   		int momentaneZeit = (calendar.get(Calendar.HOUR)*60) + calendar.get(Calendar.MINUTE) ;
//   	   	
//   	   		int differenzBeimWarten = uhrMinutenWarten(angezeigteZeit, momentaneZeit) ; 
//   	   		int differenzBeimTakten = (uhrTakte(angezeigteZeit, momentaneZeit)*impulsDauer() ;
//   	   		
//   	   		if (uhrDifferenz(angezeigteZeit, momentaneZeit) < ( 720 / 2 )) // weniger als 6 Stunde differenz
//   	 		{
//   	   			return false ; // ja uhr weiter takten
//   	 		}
//   	   		else
//   	   		{
//   	   			return true ; // sonst warten
//   	   		}
//   	   	}

    private int uhrMinutenWarten(int angezeigteZeit, int momentaneZeit) // gibt minuten zurueck
    {
        int differenz = 0;

        if (angezeigteZeit > 719) {
            angezeigteZeit = angezeigteZeit - 720; // nachmittag auf vormittag legen
        }

        while (angezeigteZeit != momentaneZeit) {
            momentaneZeit++;
            if (momentaneZeit > 719) {
                momentaneZeit = 0;
            }
            differenz++;
        }

        return differenz;
    }

    private int uhrTakte(int angezeigteZeit, int momentaneZeit) // gibt minuten zurueck
    {
        int differenz = 0;

        if (momentaneZeit > 719) {
            momentaneZeit = momentaneZeit - 720; // nachmittag auf vormittag legen
        }

        while (angezeigteZeit != momentaneZeit) {
            angezeigteZeit++;
            if (angezeigteZeit > 719) {
                angezeigteZeit = 0;
            }
            differenz++;
        }

        return differenz;
    }

    private int impulsDauerA() {
        try {
            PlatinenDatabaseHelper dbHelper = PlatinenDatabaseHelper.getInstance(TurmtechnikActivity.turmtechnikContext);
            PlatinenDatabaseHelper.NebenuhrConfig config = dbHelper.getNebenuhrByZeile(3);
            if (config != null) {
                return (config.impulsDauer1 > 0 ? config.impulsDauer1 : 1) + (config.impulsDauer2 > 0 ? config.impulsDauer2 : 1);
            }
        } catch (Exception e) {
            android.util.Log.e("SetNebenuhrActivity", "impulsDauerA aus DB: " + e.getMessage(), e);
        }
        return 0;
    }

    private int impulsDauerB() {
        try {
            PlatinenDatabaseHelper dbHelper = PlatinenDatabaseHelper.getInstance(TurmtechnikActivity.turmtechnikContext);
            PlatinenDatabaseHelper.NebenuhrConfig config = dbHelper.getNebenuhrByZeile(4);
            if (config != null) {
                return (config.impulsDauer1 > 0 ? config.impulsDauer1 : 1) + (config.impulsDauer2 > 0 ? config.impulsDauer2 : 1);
            }
        } catch (Exception e) {
            android.util.Log.e("SetNebenuhrActivity", "impulsDauerB aus DB: " + e.getMessage(), e);
        }
        return 0;
    }

    private int impulsDauerC() {
        try {
            PlatinenDatabaseHelper dbHelper = PlatinenDatabaseHelper.getInstance(TurmtechnikActivity.turmtechnikContext);
            PlatinenDatabaseHelper.NebenuhrConfig config = dbHelper.getNebenuhrByZeile(5);
            if (config != null) {
                return (config.impulsDauer1 > 0 ? config.impulsDauer1 : 1) + (config.impulsDauer2 > 0 ? config.impulsDauer2 : 1);
            }
        } catch (Exception e) {
            android.util.Log.e("SetNebenuhrActivity", "impulsDauerC aus DB: " + e.getMessage(), e);
        }
        return 0;
    }

    /** Warten vs. Takten (Aufholen) neu berechnen – zentrale Logik in NebenUhrThread (inkl. Monduhr D). */
    private void setWartenLaufen() {
        NebenUhrThread.recomputeWartenLaufen(this);
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

    /**
     * Setzt die angezeigte Zeit der gewählten Nebenuhr. lastRelaisA wird NICHT getoggelt,
     * damit der nächste Aufhol-Impuls das andere Relais verwendet (Wechselschaltung):
     * lastRelaisA = „letztes war A“ → nächster Impuls = B; lastRelaisA = false → nächster = A.
     * Ein Toggle hier würde dasselbe Relais nochmal senden, die Uhr würde nicht weiterlaufen.
     */
    private void setOneStaticVariable(int index) {
        int minutenTemp;

        switch (index) {
            case 0:
                minutenTemp = computeMinutenTemp(layout.nebenuhrStunde[0], layout.nebenuhrMinute[0]);
                StaticVariable.uhrA_angezeigteZeit = minutenTemp;
                StaticVariable.uhrA_aufholenAnfordern = true;
                persistNebenuhrRelaisState(3, StaticVariable.uhrA_lastRelaisA, minutenTemp);
                break;

            case 1:
                minutenTemp = computeMinutenTemp(layout.nebenuhrStunde[1], layout.nebenuhrMinute[1]);
                StaticVariable.uhrB_angezeigteZeit = minutenTemp;
                StaticVariable.uhrB_aufholenAnfordern = true;
                persistNebenuhrRelaisState(4, StaticVariable.uhrB_lastRelaisA, minutenTemp);
                break;

            case 2:
                minutenTemp = computeMinutenTemp(layout.nebenuhrStunde[2], layout.nebenuhrMinute[2]);
                StaticVariable.uhrC_angezeigteZeit = minutenTemp;
                StaticVariable.uhrC_aufholenAnfordern = true;
                persistNebenuhrRelaisState(5, StaticVariable.uhrC_lastRelaisA, minutenTemp);
                break;

            case 3:
                // Monduhr D: Mondphase wird bereits im Dialog gesetzt
                // Hier nichts zu tun, da Mondphase direkt in StaticVariable.uhrD_mondphaseIst gesetzt wird
                break;
        }
    }

    /**
     * Startet sofort den NebenUhr-Thread, damit Aufholen ohne Pause bis zur nächsten vollen Minute beginnt
     * (sonst würde erst bei der nächsten Minute incrementUhren() den Aufhol-Thread starten).
     */
    private void startNebenuhrAufholenSofort() {
        try {
            new NebenUhrThread().start();
        } catch (Exception e) {
            android.util.Log.e("SetNebenuhrActivity", "startNebenuhrAufholenSofort: " + e.getMessage(), e);
        }
    }

    /** Speichert lastRelaisA und angezeigte_zeit für die Nebenuhr in der DB (Zeile 3=A, 4=B, 5=C). */
    private void persistNebenuhrRelaisState(int zeile, boolean lastRelaisA, int angezeigteZeit) {
        try {
            PlatinenDatabaseHelper dbHelper = PlatinenDatabaseHelper.getInstance(TurmtechnikActivity.turmtechnikContext);
            PlatinenDatabaseHelper.NebenuhrConfig config = dbHelper.getNebenuhrByZeile(zeile);
            if (config != null) {
                config.lastRelaisA = lastRelaisA;
                config.angezeigteZeit = angezeigteZeit;
                dbHelper.saveNebenuhr(config);
                String uhrName = (zeile == 3) ? "A" : (zeile == 4) ? "B" : "C";
                LogTurmtechnik2.appendNebenuhrRelaisLogZeitKorrigiert(uhrName, angezeigteZeit, lastRelaisA);
            }
        } catch (Exception e) {
            android.util.Log.e("SetNebenuhrActivity", "persistNebenuhrRelaisState Fehler: " + e.getMessage(), e);
        }
    }


    public int computeMinutenTemp(int stunden, int minuten) {
        return (stunden * 60) + minuten;
    }

    private void endActivitiSeite3() {
        setWartenLaufen();
        startNebenuhrAufholenSofort();
        doRun = false;
        //layout = null ;
        System.gc();
        setResult(android.app.Activity.RESULT_OK); // Damit TurmtechnikActivity nach Erstinstallation UhrThread startet
        finish();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if ((keyCode == KeyEvent.KEYCODE_BACK)) {
            //Log.i("Acitiviti3", "back");

            // finish();
            endActivitiSeite3();
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
        String helpFileName = "help5.png";

        activityHelpSeite.putExtra("help_file_name", helpFileName);

        this.startActivity(activityHelpSeite);
    }

    private void rebootSU() {
        try {
            Process proc = Runtime.getRuntime()
                    .exec(new String[]{"su", "-c", "reboot"});


            proc.waitFor();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

//    @Override
//    public boolean onCreateOptionsMenu(Menu menu) {
//        getMenuInflater().inflate(R.menu.activity_turmtechnik, menu);
//        return true;
//    }


} // ende der Klasse
