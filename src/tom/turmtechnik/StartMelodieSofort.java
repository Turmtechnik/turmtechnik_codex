package tom.turmtechnik;

import android.app.TimePickerDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.util.Log;
import android.view.Gravity;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import java.io.IOException;
import java.text.BreakIterator;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import jxl.read.biff.BiffException;

import static android.app.TimePickerDialog.*;

/**
 * Created by alfred on 06.11.15.
 */
public class StartMelodieSofort
{
    String sourceFileName = "StartMelodieSofort";

    String melodieName = "";
    String beschriftungTaste = "";

    Context context;

    private long gewaehlteStartzeitMs = 0;
    private long startzeitMinimumMs;
    private TagesSuche tagesSuche;

    private boolean gestartetOderAbgebrochenFlag = false;
    private long vorlaufZeitPlusVorschwingzeit;
    private BreakIterator txtTime;
    /** Offset der Soforttaste (0, 1, 2, …) für Speichern in DB. */
    private int offsetSofortStartTaste = -1;

    public StartMelodieSofort(Context context) {
        this.context = context;
    }


    public void setStartMelodieSofort(int offsetSofortStartTaste) {
        this.offsetSofortStartTaste = offsetSofortStartTaste;
        Log.e("sofort", "offset=" + offsetSofortStartTaste);

        tagesSuche = new TagesSuche();

        melodieName = "";
        beschriftungTaste = "";
        try {
            android.content.Context appContext = (context != null ? context.getApplicationContext() : null);
            if (appContext == null) appContext = TurmtechnikActivity.turmtechnikContext;
            if (appContext != null) {
                PlatinenDatabaseHelper dbHelper = PlatinenDatabaseHelper.getInstance(appContext);
                String dbMelodie = dbHelper.getConfigValue("sofort_melodie_" + offsetSofortStartTaste);
                if (dbMelodie != null && !dbMelodie.trim().isEmpty()) {
                    melodieName = dbMelodie.trim().replaceAll("\\s+", " ").trim();
                    beschriftungTaste = melodieName;
                }
            }
        } catch (Exception e) {
            android.util.Log.e(sourceFileName, "DB-Lesen Soforttaste " + offsetSofortStartTaste, e);
        }

        if (melodieName.isEmpty()) {
            ExcelRead excelRead = new ExcelRead();

        String sofortstartProgrammPathAndFilename = TurmtechnikActivity.sdCardPath + "/Turmtechnik/Config/Sofortstart Programme.xls";

        try {
            excelRead.openXls(sofortstartProgrammPathAndFilename);
            String zelle = excelRead.getCellString(2, (offsetSofortStartTaste + 4));
            melodieName = (zelle != null ? zelle.trim().replaceAll("\\s+", " ") : "").trim();
            String beschriftungZelle = excelRead.getCellString(0, (offsetSofortStartTaste + 4));
            beschriftungTaste = (beschriftungZelle != null ? beschriftungZelle.trim() : "");

        } catch (BiffException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
            Log.e("Biff", "error");
            new LogExcelError(0, 0, sofortstartProgrammPathAndFilename, -1, sourceFileName, 28);

        } catch (IOException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
            Log.e("IO", "error");
            new LogExcelError(0, 0, sofortstartProgrammPathAndFilename, -1, sourceFileName, 34);
        } catch (Exception e) {
            e.printStackTrace();
        }
        }

        if (!(melodieName.equals(""))) {

            startzeitMinimumMs = berechneStartzeitMinimum();
            gewaehlteStartzeitMs = startzeitMinimumMs;
            setHourAndMinuteFromMs(gewaehlteStartzeitMs);


            if (gewaehlteStartzeitMs < startzeitMinimumMs) {
                gewaehlteStartzeitMs = startzeitMinimumMs;
                setHourAndMinuteFromMs(gewaehlteStartzeitMs);
            }
            //popupStartOk() ;
            popupTimePicker(StaticVariable.startStundeManuell, StaticVariable.startMinuteManuell);

        }

    }
/*
    private void popupStartOk()
    {
		AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(context);
		alertDialogBuilder.setTitle(" Start " + getStartzeitStringFromMs(gewaehlteStartzeitMs) );
		alertDialogBuilder.setMessage("Melodie: " + beschriftungTaste);

		alertDialogBuilder.setPositiveButton
				("Start Melodie",
						new DialogInterface.OnClickListener() {
			            	@Override
							public void onClick(DialogInterface dialog, int which) {
				        	StaticVariable.sofortStartPopupFilename = melodieName;
							StaticVariable.sofortStartPopupFlag = true;
						}
		});

		alertDialogBuilder.setNegativeButton
				("abbrechen",
						new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int whichButton) {
								// Canceled.
								StaticVariable.sofortStartPopupFlag = false;
							}
						});

		alertDialogBuilder.show() ;
	}
*/


    private void popupTimePicker(int mHour, int mMinute) {

        //Toast.makeText(context, "popupTimePicker" , Toast.LENGTH_LONG).show();

        alfredTimeDialog(mHour, mMinute, context);

        //Dialog alfredTimeDialogShow = alfredTimeDialog(mHour, mMinute, context) ;

        //alfredTimeDialogShow.show();

    }

    private void alfredTimeDialog(int mHour, int mMinute, final Context context) {

        gestartetOderAbgebrochenFlag = true;

        TextView title = new TextView(context);

        title.setText(" " + beschriftungTaste + " ");
        title.setGravity(Gravity.CENTER);
        title.setTextColor(Color.WHITE);
        title.setTextSize(30);

        TimePickerDialog timeDialogBuilder = new TimePickerDialog(context, timePickerListener2, mHour, mMinute, true);


        //TimePickerDialog timeDialogBuilder = new TimePickerDialog(
        //		context,
        //		new TimePickerDialog.OnTimeSetListener()
        //		{
        //			@Override
        //			public void onTimeSet(TimePicker view, int hourOfday, int minute)
        //			{
        //				Log.e("timePickerListenerNEU", "gestartetFlag=" + gestartetOderAbgebrochenFlag) ;
        //			}
        //		}
        //		, mHour, mMinute , true ) ;


        timeDialogBuilder.setTitle(beschriftungTaste);
        timeDialogBuilder.setCustomTitle(title);
        //timeDialogBuilder.setMessage("um: ");
        timeDialogBuilder.setCancelable(true);


        timeDialogBuilder.setOnDismissListener(dismissListener);

        timeDialogBuilder.setButton(DialogInterface.BUTTON_POSITIVE, "AKTIVIEREN", timeDialogBuilder);

/*
            timeDialogBuilder.setButton(BUTTON_POSITIVE,
					"AKTIVIEREN",
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {
							gestartetOderAbgebrochenFlag = true;

						}
					});
*/

        timeDialogBuilder.setButton(BUTTON_NEGATIVE,
                "DEAKTIVIEREN",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        gestartetOderAbgebrochenFlag = false;
                        //Toast.makeText(context, "de-aktivieren betaetigt" , Toast.LENGTH_LONG).show();
                    }
                }
        );

        timeDialogBuilder.show();

    }


    public DialogInterface.OnDismissListener dismissListener = new DialogInterface.OnDismissListener() {
        @Override
        public void onDismiss(DialogInterface dialogInterface) {
            Log.e("onDismiss", "gestartetFlag=" + gestartetOderAbgebrochenFlag);
            if (!gestartetOderAbgebrochenFlag) {
                StaticVariable.sofortStartPopupFlag = false;
                StaticVariable.sofortStartPopupGefunden = false;
                UhrThread.newSearchAutomaticStart = true;
                Toast.makeText(context, "DEAKTIVIERT", Toast.LENGTH_LONG).show();
            }
        }
    };

    public TimePicker.OnTimeChangedListener otcl = new TimePicker.OnTimeChangedListener() {
        @Override
        public void onTimeChanged(TimePicker timePicker, int i, int i1) {

        }
    };

    public OnTimeSetListener timePickerListener2 = new OnTimeSetListener() {

        @Override
        public final void onTimeSet(TimePicker view, int selectedHour, int selectedMinute) {
            Log.e("timePickerListener", "gestartetFlag=" + gestartetOderAbgebrochenFlag);
            if (gestartetOderAbgebrochenFlag) {

                StaticVariable.startStundeManuell = selectedHour;
                StaticVariable.startMinuteManuell = selectedMinute;

                setGewaehlteStartzeitMs();


                if (gewaehlteStartzeitMs < startzeitMinimumMs) {
                    gewaehlteStartzeitMs = startzeitMinimumMs;
                    setHourAndMinuteFromMs(gewaehlteStartzeitMs);
                }

                //gewaehlteStartzeitMs += vorlaufZeitPlusVorschwingzeit ;
                //setHourAndMinuteFromMs(gewaehlteStartzeitMs);

                StaticVariable.sofortStartPopupFilename = melodieName;
                StaticVariable.sofortStartPopupFlag = true;
                Calendar cal = Calendar.getInstance();
                int jahr = cal.get(Calendar.YEAR);
                int monat = cal.get(Calendar.MONTH);
                int tag = cal.get(Calendar.DAY_OF_MONTH);
                String vorlaufZeitZurMelodie = tagesSuche.getBeginnTime2(melodieName, jahr, monat, tag, StaticVariable.startStundeManuell, StaticVariable.startMinuteManuell);
                // Infozeile: berechneten Beginn (wie bei anderen Programmen) anzeigen, nicht die Startzeit
                String beginnStr = (vorlaufZeitZurMelodie != null && !vorlaufZeitZurMelodie.isEmpty()) ? vorlaufZeitZurMelodie : (tagesSuche.pad(StaticVariable.startStundeManuell) + ":" + tagesSuche.pad(StaticVariable.startMinuteManuell));
                StaticVariable.stringInfoTextField[0] = (melodieName != null ? melodieName : "") + "  " + beginnStr;
                UhrThread.newSearchAutomaticStart = true;
                StaticVariable.sofortStartPopupGefunden = false;
                if (offsetSofortStartTaste >= 0) {
                    try {
                        android.content.Context appContext = (context != null ? context.getApplicationContext() : null);
                        if (appContext == null) appContext = TurmtechnikActivity.turmtechnikContext;
                        if (appContext != null) {
                            PlatinenDatabaseHelper.getInstance(appContext).setConfigValue("sofort_melodie_" + offsetSofortStartTaste, melodieName);
                        }
                    } catch (Exception e) {
                        android.util.Log.e(sourceFileName, "DB-Speichern Soforttaste " + offsetSofortStartTaste, e);
                    }
                }
                Toast.makeText(context, "Melodie " + beschriftungTaste + " gestartet " + getStartzeitStringFromMs(gewaehlteStartzeitMs), Toast.LENGTH_LONG).show();
            } else {

                StaticVariable.sofortStartPopupFlag = false;
                StaticVariable.sofortStartPopupGefunden = false;
                UhrThread.newSearchAutomaticStart = true;
                Toast.makeText(context, "DEAKTIVIERT", Toast.LENGTH_LONG).show();
            }
        }
    };

    private long berechneStartzeitMinimum() {
        vorlaufZeitPlusVorschwingzeit = tagesSuche.getErsteVorlaufzeitMs(melodieName);


        Calendar cal = Calendar.getInstance();

        long berechTemp = cal.getTimeInMillis();
        berechTemp += (vorlaufZeitPlusVorschwingzeit + 120000); // + 2 Minuten

        return berechTemp;
    }

    public String getStartzeitStringFromMs(long ms) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm");
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(ms);
        return ("um " + dateFormat.format(cal.getTime()));
    }

    private void setHourAndMinuteFromMs(long ms) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(ms);

        StaticVariable.startStundeManuell = cal.get(Calendar.HOUR_OF_DAY);
        StaticVariable.startMinuteManuell = cal.get(Calendar.MINUTE);
    }


    private void setGewaehlteStartzeitMs() {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, StaticVariable.startStundeManuell);
        cal.set(Calendar.MINUTE, StaticVariable.startMinuteManuell);

        gewaehlteStartzeitMs = cal.getTimeInMillis();
    }


}

