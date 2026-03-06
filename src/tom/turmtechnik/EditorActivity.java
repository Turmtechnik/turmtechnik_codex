package tom.turmtechnik;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.app.Activity;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Calendar;

import static android.content.DialogInterface.BUTTON_NEGATIVE;


public class EditorActivity extends Activity
{
    Button buttonZeilenNummer ;
    Button buttonUhrzeit ;
    Button buttonMelodieAusgang ;
    Button buttonSpeichern ;
    Button buttonImmerEinmalig ;
    Button buttonSommerWinterImmer ;
    Button buttonAbbruch ;
    Button buttonAnfang;
    Button buttonEnde ;
    Button buttonKopieren ;


    //Button buttonMontag ;
    //Button buttonDienstag ;
    //Button buttonMittwoch ;
    //Button buttonDonnerstag ;
    //Button buttonFreitag ;
    //Button buttonSamstag ;
    //Button buttonSonntag ;

    ArrayList<Button> buttonWochentage ;

    ExcelReadWrite excelReadWrite ;

    /** Datenquelle: Excel oder Datenbank. */
    private IEditorDataSource editorData;
    /** Aktuelle Zeile (Excel: pathAndFilenameEditorIndex, DB: Index in Liste 0-basiert). */
    private int currentEditRow;

    boolean flagMelodie = true ;

    boolean timeDialogAbgebrochenFlag2 ;
    Context context;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_editor);

        context = this ;

        excelReadWrite = new ExcelReadWrite();

        Log.e("EditorActivity", "onCreate: pathAndFilenameEditor = " + StaticVariable.pathAndFilenameEditor);
        Log.e("EditorActivity", "onCreate: pathAndFilenameEditorIndex = " + currentEditRow);

        if (StaticVariable.pathAndFilenameEditor == null || StaticVariable.pathAndFilenameEditor.isEmpty()) {
            if (TurmtechnikActivity.normalprogrammFileString != null && !TurmtechnikActivity.normalprogrammFileString.isEmpty()) {
                StaticVariable.pathAndFilenameEditor = TurmtechnikActivity.normalprogrammFileString;
            } else {
                Toast.makeText(this, "Fehler: Programmdatei nicht gefunden", Toast.LENGTH_LONG).show();
                finish();
                return;
            }
        }

        // Datenbank-Modus: "DB:TagtypName"
        if (StaticVariable.pathAndFilenameEditor.startsWith("DB:")) {
            String tagtypName = StaticVariable.pathAndFilenameEditor.substring(3).trim();
            if (tagtypName.isEmpty()) tagtypName = "Normalprogramm";
            try {
                java.util.List<Programm> programmeList = PlatinenDatabaseHelper.getInstance(getApplicationContext()).getProgrammeByTagtyp(tagtypName);
                if (programmeList == null || programmeList.isEmpty()) {
                    Toast.makeText(this, "Keine Programme für " + tagtypName + " in der Datenbank.", Toast.LENGTH_LONG).show();
                    finish();
                    return;
                }
                editorData = new EditorDataDatabase(this, tagtypName, programmeList);
                int idx = StaticVariable.pathAndFilenameEditorIndex;
                currentEditRow = (idx >= 0 && idx < programmeList.size()) ? idx : 0;
                Log.e("EditorActivity", "Datenbank-Modus: " + tagtypName + ", Zeile " + (currentEditRow + 1) + "/" + programmeList.size());
            } catch (Exception e) {
                Log.e("EditorActivity", "Fehler beim Laden aus DB", e);
                Toast.makeText(this, "Fehler: " + e.getMessage(), Toast.LENGTH_LONG).show();
                finish();
                return;
            }
        } else {
            // Excel-Modus
            currentEditRow = StaticVariable.pathAndFilenameEditorIndex >= 0 ? StaticVariable.pathAndFilenameEditorIndex : 3;
            if (currentEditRow < 3) currentEditRow = 3;
            java.io.File file = new java.io.File(StaticVariable.pathAndFilenameEditor);
            if (!file.exists()) {
                Toast.makeText(this, "Fehler: Datei nicht gefunden: " + file.getName(), Toast.LENGTH_LONG).show();
                finish();
                return;
            }
            try {
                excelReadWrite.openXlsReadWrite(StaticVariable.pathAndFilenameEditor, 0);
                editorData = new EditorDataExcel(excelReadWrite);
            } catch (Exception e) {
                Log.e("EditorActivity", "Fehler beim Öffnen der Excel-Datei", e);
                Toast.makeText(this, "Fehler beim Öffnen der Programmdatei", Toast.LENGTH_LONG).show();
                finish();
                return;
            }
        }

        if( editorData.getCellString(TagesSuche.SPALTE_B_FUNKTION , currentEditRow).equals("Melodie") )
        {
            flagMelodie = true ;
        }
        else
        {
            flagMelodie = false ;
        }

        buttonWochentage = new ArrayList<>() ;



        buttonUhrzeit = (Button) findViewById(R.id.button_uhrzeit) ;
        final String momentaneUhrzeitString  = editorData.getCellString(TagesSuche.SPALTE_A_STARTZEIT, currentEditRow);
        buttonUhrzeit.setText(momentaneUhrzeitString) ;
        buttonUhrzeit.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                int momentanStunde ;
                int momentanMinute ;

                String[] momentaneUhrzeitStringSplit = momentaneUhrzeitString.split(":")  ;

                momentanStunde = Integer.parseInt(momentaneUhrzeitStringSplit[0]) ;
                momentanMinute = Integer.parseInt(momentaneUhrzeitStringSplit[1]) ;


                //popupStartOk() ;
                popupTimePicker(momentanStunde, momentanMinute);
            }
        });

        buttonZeilenNummer = (Button) findViewById(R.id.button_programm_zeilennnummer) ;
        int displayZeile = editorData.isDatabaseMode() ? (currentEditRow + 1) : (currentEditRow - 2);
        buttonZeilenNummer.setText( StaticVariable.getUebersetzung(18) + " " + displayZeile) ;


        String melodieAusgangName ;

        if(flagMelodie)
        {
            melodieAusgangName = editorData.getCellString(TagesSuche.SPALTE_C_MELODIE_NAME, currentEditRow) ;
        }
        else
        {
            melodieAusgangName = editorData.getCellString(TagesSuche.SPALTE_B_FUNKTION, currentEditRow) ;
        }


        buttonZeilenNummer.append("  " + melodieAusgangName);


        buttonZeilenNummer.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                incrementEditZeile();
                try {
                    if (editorData.isDatabaseMode()) editorData.save(); else ((EditorDataExcel)editorData).getExcelReadWrite().saveAndCloseWorkbooks();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                Intent intent = new Intent(EditorActivity.this, EditorActivity.class) ;
                EditorActivity.this.startActivity(intent) ;

                finish();
            }
        });

        buttonMelodieAusgang = (Button) findViewById(R.id.button_melodie_ausgang) ;

        String melodieHeizungStringBezeichnung ;

        if(flagMelodie)
        {
            //melodieHeizungStringBezeichnung = "Melodie" ;
            melodieHeizungStringBezeichnung = StaticVariable.getUebersetzung(19) ;
        }
        else
        {
            //melodieHeizungStringBezeichnung = "Ausgang" ;
            melodieHeizungStringBezeichnung = StaticVariable.getUebersetzung(20);
        }


        buttonMelodieAusgang.setText(melodieHeizungStringBezeichnung);

        buttonMelodieAusgang.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {

            }
        });

        //buttonMontag = (Button) findViewById(R.id.button_montag) ;
        //buttonWochentage.add(buttonMontag) ;
        buttonWochentage.add((Button) findViewById(R.id.button_montag)) ;

        String wochentagStringX = editorData.getCellString(TagesSuche.SPALTE_E_WOCHENTAG_MONTAG, currentEditRow);
        wochentagStringX = wochentagStringX.toLowerCase() ;
        //String wochtangAnzeige = "MO";

        //buttonMontag.setText(wochtangAnzeige) ;

        boolean flagOnOff = wochentagStringX.equals("x") ;
        setWochentagColor( 0 , flagOnOff);



        buttonWochentage.get(0).setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                changeWochentag(0) ;
            }
        });

        //****
        //buttonDienstag = (Button) findViewById(R.id.button_dienstag) ;
        //buttonWochentage.add(buttonDienstag) ;
        buttonWochentage.add((Button) findViewById(R.id.button_dienstag)) ;

        wochentagStringX = editorData.getCellString(TagesSuche.SPALTE_E_WOCHENTAG_MONTAG + 1, currentEditRow);
        wochentagStringX = wochentagStringX.toLowerCase() ;
        //String wochtangAnzeige = "MO";

        //buttonMontag.setText(wochtangAnzeige) ;

        flagOnOff = wochentagStringX.equals("x") ;
        setWochentagColor( 1 , flagOnOff);

        buttonWochentage.get(1).setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                changeWochentag(1) ;
            }
        });

        //****
        //buttonMittwoch = (Button) findViewById(R.id.button_mittwoch) ;
        buttonWochentage.add((Button) findViewById(R.id.button_mittwoch)) ;

        wochentagStringX = editorData.getCellString(TagesSuche.SPALTE_E_WOCHENTAG_MONTAG + 2, currentEditRow);
        wochentagStringX = wochentagStringX.toLowerCase() ;
        //String wochtangAnzeige = "MO";

        //buttonMontag.setText(wochtangAnzeige) ;

        flagOnOff = wochentagStringX.equals("x") ;
        setWochentagColor( 2 , flagOnOff);

        buttonWochentage.get(2).setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                changeWochentag(2) ;
            }
        });

        //****

        buttonWochentage.add((Button) findViewById(R.id.button_donnerstag)) ;

        wochentagStringX = editorData.getCellString(TagesSuche.SPALTE_E_WOCHENTAG_MONTAG + 3, currentEditRow);
        wochentagStringX = wochentagStringX.toLowerCase() ;
        //String wochtangAnzeige = "MO";

        //buttonMontag.setText(wochtangAnzeige) ;

        flagOnOff = wochentagStringX.equals("x") ;
        setWochentagColor( 3 , flagOnOff);

        buttonWochentage.get(3).setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                changeWochentag(3) ;
            }
        });

        //****

        buttonWochentage.add( (Button) findViewById(R.id.button_freitag)) ;

        wochentagStringX = editorData.getCellString(TagesSuche.SPALTE_E_WOCHENTAG_MONTAG + 4, currentEditRow);
        wochentagStringX = wochentagStringX.toLowerCase() ;
        //String wochtangAnzeige = "MO";

        //buttonMontag.setText(wochtangAnzeige) ;

        flagOnOff = wochentagStringX.equals("x") ;
        setWochentagColor( 4 , flagOnOff);

        buttonWochentage.get(4).setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                changeWochentag(4) ;
            }
        });

        //****

        buttonWochentage.add( (Button) findViewById(R.id.button_samstag)) ;

        wochentagStringX = editorData.getCellString(TagesSuche.SPALTE_E_WOCHENTAG_MONTAG + 5, currentEditRow);
        wochentagStringX = wochentagStringX.toLowerCase() ;
        //String wochtangAnzeige = "MO";

        //buttonMontag.setText(wochtangAnzeige) ;

        flagOnOff = wochentagStringX.equals("x") ;
        setWochentagColor( 5 , flagOnOff);

        buttonWochentage.get(5).setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                changeWochentag(5) ;
            }
        });

        //****

        buttonWochentage.add( (Button) findViewById(R.id.button_sonntag)) ;

        wochentagStringX = editorData.getCellString(TagesSuche.SPALTE_E_WOCHENTAG_MONTAG + 6, currentEditRow);
        wochentagStringX = wochentagStringX.toLowerCase() ;
        //String wochtangAnzeige = "MO";

        //buttonMontag.setText(wochtangAnzeige) ;

        flagOnOff = wochentagStringX.equals("x") ;
        setWochentagColor( 6 , flagOnOff);

        buttonWochentage.get(6).setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                changeWochentag(6) ;
            }
        });


        buttonImmerEinmalig = (Button) findViewById(R.id.button_immer_einmalig) ;
        boolean flagImmerEinmalig = false ;

        if(editorData.getCellString(TagesSuche.SPALTE_L_IMMER, currentEditRow).toLowerCase().equals("x"))
        {
            flagImmerEinmalig = true ;
        }

        String immerEinmalig ;
        immerEinmalig = getTextEinmaligImmer(flagImmerEinmalig) ;

        buttonImmerEinmalig.setText(immerEinmalig) ;

        buttonImmerEinmalig.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                showImmerEinmaligDialog();
            }
        });

        buttonSommerWinterImmer = (Button) findViewById(R.id.button_sommer_winter_immer) ;

        String  stringSommerWinterImmer = "0" ;

        stringSommerWinterImmer = (editorData.getCellString(TagesSuche.SPALTE_M_PERIODISCH , currentEditRow)) ;

        buttonSommerWinterImmer.setText(getTextSommerWinterImmer(stringSommerWinterImmer)) ;

        buttonSommerWinterImmer.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                showSommerWinterImmerDialog();
            }
        });

        buttonAnfang = (Button) findViewById(R.id.button_anfang) ;
        String tempString = editorData.getCellString(TagesSuche.SPALTE_N_START,currentEditRow) ;
        if( ! tempString.contains(".") )
        {
            tempString = "1.1" ;
        }

        //buttonAnfang.setText("Anfang " + tempString);
        buttonAnfang.setText(StaticVariable.getUebersetzung(21) + ":  "  + tempString);

        buttonAnfang.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                String excelTemp = editorData.getCellString(TagesSuche.SPALTE_N_START, currentEditRow) ;
                if( ! excelTemp.contains(".") )
                {
                    excelTemp = "1.1" ;
                }
                String[] stringSplit = excelTemp.split("\\.") ;
                Calendar calendar = Calendar.getInstance() ;

                int mYear = calendar.get(Calendar.YEAR) ;
                int mMonth = (Integer.parseInt(stringSplit[1])-1) ;
                int mDay = Integer.parseInt(stringSplit[0]) ; ;
                //dateDialogSpalte = TagesSuche.SPALTE_N_START ;

                DatePickerDialog dialog = new DatePickerDialog(context, new mDateSetListenerAnfang() , mYear, mMonth, mDay );
                dialog.show();
            }
        });

        buttonEnde = (Button) findViewById(R.id.button_ende) ;
        tempString = editorData.getCellString(TagesSuche.SPALTE_O_ENDE,currentEditRow) ;
        if( ! tempString.contains(".") )
        {
            tempString = "31.12" ;
            buttonEnde.setText(StaticVariable.getUebersetzung(22) + ":  " + tempString) ;
        }
        else
        {
            if(  (tempString.contains("W") ) )
            {
                String[] tempSplit = tempString.split("\\.") ;
                tempString = getTextWocheTag(Integer.parseInt(tempSplit[0])-1) ;
                buttonEnde.setText(tempString) ;

            }
            else if(  (tempString.contains("T") ) )
            {
                String[] tempSplit = tempString.split("\\.");

                if (tempSplit[0].equals("14"))
                {
                    tempString = getTextWocheTag(5);
                } else
                {
                    tempString = getTextWocheTag(4);
                }
                buttonEnde.setText(tempString) ;
            }
            else
            {
                buttonEnde.setText(StaticVariable.getUebersetzung(22) + ":  " + tempString) ;
            }
        }

        //buttonEnde.setText("Ende " + tempString);
        //buttonEnde.setText(StaticVariable.getUebersetzung(22) + ":  " + tempString) ;

        final String clickEndeString = tempString ;

        buttonEnde.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                String excelTemp = editorData.getCellString(TagesSuche.SPALTE_O_ENDE, currentEditRow) ;
                if( ! excelTemp.contains(".") )
                {
                    excelTemp = "31.12" ;
                }

                String[] stringSplit = excelTemp.split("\\.") ;
                Calendar calendar = Calendar.getInstance() ;

                int mYear = calendar.get(Calendar.YEAR) ;

                int mMonth = 11 ;
                try
                {
                    mMonth = (Integer.parseInt(stringSplit[1]) - 1);
                }
                catch(NumberFormatException e)
                {
                    // wenn monat ein "W" oder ein "T" sind
                }

                int mDay = Integer.parseInt(stringSplit[0]) ; ;
                //dateDialogSpalte = TagesSuche.SPALTE_O_ENDE ;

                DatePickerDialog dialog = new DatePickerDialog(context, new mDateSetListenerEnde() , mYear, mMonth, mDay );
                dialog.setButton(DialogInterface.BUTTON_NEUTRAL, "Woche/Tag" , new DialogInterface.OnClickListener()
                {
                        public void onClick(DialogInterface dialog, int id)
                        {
                            // hier woche/tag dialog starten
                            showWocheTagDialog() ;
                            dialog.dismiss();
                        }
                });
                dialog.show();
            }
        });

        buttonSpeichern = (Button) findViewById(R.id.button_speichern) ;

        buttonSpeichern.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                try
                {
                    try { if (editorData.isDatabaseMode()) editorData.save(); else ((EditorDataExcel)editorData).getExcelReadWrite().saveAndCloseWorkbooks(); } catch (Exception e) { e.printStackTrace(); }
                    if (!editorData.isDatabaseMode()) ((EditorDataExcel)editorData).getExcelReadWrite().copyTempWorkbook();
                    UhrThread.setExcelreadNull();  // damit das Programm neu eingelesen wird


                } catch (Exception e)
                {
                    e.printStackTrace();
                }

                finish();
            }
        });

        buttonAbbruch = (Button) findViewById(R.id.button_abbruch) ;

        buttonAbbruch.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                try
                {
                    try { if (editorData.isDatabaseMode()) editorData.save(); else ((EditorDataExcel)editorData).getExcelReadWrite().saveAndCloseWorkbooks(); } catch (Exception e) { e.printStackTrace(); }
                } catch (Exception e)
                {
                    e.printStackTrace();
                }

                finish();
            }
        });

        buttonKopieren = (Button) findViewById(R.id.button_kopieren) ;

        buttonKopieren.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                currentEditRow = kopiereMomentaneZeile() ;

                try {
                    if (editorData.isDatabaseMode()) {
                        editorData.save();
                    } else {
                        ((EditorDataExcel) editorData).getExcelReadWrite().saveAndCloseWorkbooks();
                        ((EditorDataExcel) editorData).getExcelReadWrite().copyTempWorkbook();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }




                Intent intent = new Intent(EditorActivity.this, EditorActivity.class) ;
                EditorActivity.this.startActivity(intent) ;

                finish();

            }
        });


    } // ende von onCreate

    private int kopiereMomentaneZeile()
    {
        if (editorData.isDatabaseMode()) {
            return editorData.copyRow(currentEditRow);
        }
        ExcelReadWrite excel = ((EditorDataExcel) editorData).getExcelReadWrite();
        int maximalSpalte = excel.getCellSpalten();
        int neuerIndex = sucheFreieZeile();
        for (int i = 0; i < maximalSpalte; i++) {
            excel.writeCopy(currentEditRow, neuerIndex, i);
        }
        return neuerIndex;
    }

    private int sucheFreieZeile()
    {
        if (editorData.isDatabaseMode()) {
            return editorData.getRowCount();
        }
        int nextFreeIndex = currentEditRow;
        while (nextFreeIndex < editorData.getRowCount() && !editorData.getCellString(0, nextFreeIndex).equals("")) {
            nextFreeIndex++;
        }
        return nextFreeIndex;
    }

    private void refreshUIFromCurrentRow() {
        int displayZeile = editorData.isDatabaseMode() ? (currentEditRow + 1) : (currentEditRow - 2);
        String name = editorData.getCellString(TagesSuche.SPALTE_B_FUNKTION, currentEditRow).equals("Melodie")
                ? editorData.getCellString(TagesSuche.SPALTE_C_MELODIE_NAME, currentEditRow)
                : editorData.getCellString(TagesSuche.SPALTE_B_FUNKTION, currentEditRow);
        buttonZeilenNummer.setText(StaticVariable.getUebersetzung(18) + " " + displayZeile + "  " + name);
        buttonUhrzeit.setText(editorData.getCellString(TagesSuche.SPALTE_A_STARTZEIT, currentEditRow));
        buttonAnfang.setText(StaticVariable.getUebersetzung(21) + " " + editorData.getCellString(TagesSuche.SPALTE_N_START, currentEditRow));
        buttonEnde.setText(StaticVariable.getUebersetzung(22) + " " + editorData.getCellString(TagesSuche.SPALTE_O_ENDE, currentEditRow));
        String immerStr = editorData.getCellString(TagesSuche.SPALTE_L_IMMER, currentEditRow);
        buttonImmerEinmalig.setText(immerStr != null && immerStr.toLowerCase().equals("x") ? getTextEinmaligImmer(true) : getTextEinmaligImmer(false));
        String swi = editorData.getCellString(TagesSuche.SPALTE_M_PERIODISCH, currentEditRow);
        buttonSommerWinterImmer.setText(getTextSommerWinterImmer(swi != null ? swi : "0"));
    }

    private void popupTimePicker(int mHour, int mMinute) {

        //Toast.makeText(context, "popupTimePicker" , Toast.LENGTH_LONG).show();

        alfredTimeDialog(mHour, mMinute, context);

        //Dialog alfredTimeDialogShow = alfredTimeDialog(mHour, mMinute, context) ;

        //alfredTimeDialogShow.show();

    }

    private void alfredTimeDialog(int mHour, int mMinute, final Context context) {

        timeDialogAbgebrochenFlag2 = false;

        TextView title = new TextView(context);

        //title.setText("Start Zeit:");
        title.setText(StaticVariable.getUebersetzung(23));
        title.setGravity(Gravity.CENTER);
        title.setTextColor(Color.WHITE);
        title.setTextSize(30);

        TimePickerDialog timeDialogBuilder = new TimePickerDialog(context, timePickerListener2, mHour, mMinute, true);

        timeDialogBuilder.setCancelable(true);


        timeDialogBuilder.setOnDismissListener(dismissListener);

        //timeDialogBuilder.setButton(DialogInterface.BUTTON_POSITIVE, "O.K.", timeDialogBuilder);
        timeDialogBuilder.setButton(DialogInterface.BUTTON_POSITIVE, StaticVariable.getUebersetzung(24), timeDialogBuilder);


        //timeDialogBuilder.setButton(BUTTON_NEGATIVE, "CANCEL",
        timeDialogBuilder.setButton(BUTTON_NEGATIVE, StaticVariable.getUebersetzung(25),
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {

                        //Toast.makeText(context, "de-aktivieren betaetigt" , Toast.LENGTH_LONG).show();
                    }
                }
        );



        timeDialogBuilder.show();

    }

    public DialogInterface.OnDismissListener dismissListener = new DialogInterface.OnDismissListener() {
        @Override
        public void onDismiss(DialogInterface dialogInterface) {
            //Log.e("onDismiss", "gestartetFlag=" + gestartetOderAbgebrochenFlag);
            //if (timeDialogAbgebrochenFlag2)
            //{

            // Toast.makeText(context, "DEAKTIVIERT", Toast.LENGTH_LONG).show();
            // }
        }
    };

    public TimePicker.OnTimeChangedListener otcl = new TimePicker.OnTimeChangedListener() {
        @Override
        public void onTimeChanged(TimePicker timePicker, int i, int i1) {

        }
    };

    public TimePickerDialog.OnTimeSetListener timePickerListener2 = new TimePickerDialog.OnTimeSetListener() {

        @Override
        public final void onTimeSet(TimePicker view, int selectedHour, int selectedMinute) {
            Log.e("timePickerListener", "gestartetFlag=" + timeDialogAbgebrochenFlag2);
            if (! timeDialogAbgebrochenFlag2 )
            {

                //int startZeitStunde = selectedHour;
                //int startZeitMinute = selectedMinute;

                String startzeitString =  getZeitString(selectedHour) + ":" + getZeitString(selectedMinute) ;
                buttonUhrzeit.setText(startzeitString);
                editorData.putCellString(TagesSuche.SPALTE_A_STARTZEIT,currentEditRow,startzeitString);
            }



    }
};

private String getZeitString (int zahl)
{
    if(zahl < 10)
    {
        return "0" + Integer.toString(zahl) ;
    }
    else
    {
        return  Integer.toString(zahl) ;
    }
}


    private void showImmerEinmaligDialog()
    {

        // custom dialog
        final Dialog dialog = new Dialog(context);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.preferences) ;
        //List<String> stringList=new ArrayList<>();  // here is list
        //for(int i=0;i<5;i++) {
        //    stringList.add("RadioButton " + (i + 1));
        //}
        final RadioGroup radioGroup = (RadioGroup) dialog.findViewById(R.id.immer_einmalig);

        //for(int i=0;i<stringList.size();i++){
        //    RadioButton rb=new RadioButton(context); // dynamically creating RadioButton and adding to RadioGroup.
        //    rb.setText(stringList.get(i));
        //    rb.setTextColor(Color.BLUE);
        //    rg.addView(rb);
        //}

        RadioButton radioButton ;

        radioButton = new RadioButton(context) ;
        radioButton.setText(getTextEinmaligImmer(true)) ;
        radioButton.setTextSize(30);
        radioButton.setTextColor(Color.BLACK);
        radioGroup.addView(radioButton);

        radioButton = new RadioButton(context) ;
        radioButton.setText(getTextEinmaligImmer(false)) ;
        radioButton.setTextSize(30);
        radioButton.setTextColor(Color.BLACK);
        radioGroup.addView(radioButton);

        dialog.show();

        radioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener()
        {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId)
            {
                //int childCount = group.getChildCount();
                //for (int x = 0; x < childCount; x++) {
                //    RadioButton btn = (RadioButton) group.getChildAt(x);
                //    if (btn.getId() == checkedId) {
                //        Log.e("selected RadioButton->",btn.getText().toString());

                //    }
                //}
                RadioButton btn ;
                btn = (RadioButton) group.getChildAt(0) ;
                if(btn.getId() == checkedId)
                {
                    Log.e("RadioButton" , "=" + getTextEinmaligImmer(true)) ;
                    editorData.putCellString(TagesSuche.SPALTE_L_IMMER, currentEditRow, "x"); ;
                    buttonImmerEinmalig.setText(getTextEinmaligImmer(true)) ;
                }
                btn = (RadioButton) group.getChildAt(1) ;
                if(btn.getId() == checkedId)
                {
                    Log.e("RadioButton" , "=" + getTextEinmaligImmer(false)) ;
                    editorData.putCellString(TagesSuche.SPALTE_L_IMMER, currentEditRow, ""); ;
                    buttonImmerEinmalig.setText(getTextEinmaligImmer(false)) ;
                }

                dialog.dismiss();
            }
        });

    }

    private void showSommerWinterImmerDialog()
    {

        final Dialog dialog = new Dialog(context);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.preferences) ;

        final RadioGroup radioGroup = (RadioGroup) dialog.findViewById(R.id.immer_einmalig);

        RadioButton radioButton ;

        radioButton = new RadioButton(context) ;
        radioButton.setText(getTextSommerWinterImmer("0")) ;
        radioButton.setTextSize(30);
        radioButton.setTextColor(Color.BLACK);
        radioGroup.addView(radioButton);

        radioButton = new RadioButton(context) ;
        radioButton.setText(getTextSommerWinterImmer("1")) ;
        radioButton.setTextSize(30);
        radioButton.setTextColor(Color.BLACK);
        radioGroup.addView(radioButton);

        radioButton = new RadioButton(context) ;
        radioButton.setText(getTextSommerWinterImmer("2")) ;
        radioButton.setTextSize(30);
        radioButton.setTextColor(Color.BLACK);
        radioGroup.addView(radioButton);

        dialog.show();

        radioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener()
        {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId)
            {
                RadioButton btn ;
                btn = (RadioButton) group.getChildAt(0) ;
                if(btn.getId() == checkedId)
                {
                    Log.e("RadioButton" , "=" + getTextEinmaligImmer(true)) ;
                    editorData.putCellString(TagesSuche.SPALTE_M_PERIODISCH, currentEditRow, "0"); ;
                    buttonSommerWinterImmer.setText(getTextSommerWinterImmer("0")) ;
                }
                btn = (RadioButton) group.getChildAt(1) ;
                if(btn.getId() == checkedId)
                {
                    Log.e("RadioButton" , "=" + getTextEinmaligImmer(false)) ;
                    editorData.putCellString(TagesSuche.SPALTE_M_PERIODISCH, currentEditRow, "1"); ;
                    buttonSommerWinterImmer.setText(getTextSommerWinterImmer("1")) ;
                }

                btn = (RadioButton) group.getChildAt(2) ;
                if(btn.getId() == checkedId)
                {
                    Log.e("RadioButton" , "=" + getTextEinmaligImmer(false)) ;
                    editorData.putCellString(TagesSuche.SPALTE_M_PERIODISCH, currentEditRow, "2"); ;
                    buttonSommerWinterImmer.setText(getTextSommerWinterImmer("2")) ;
                }

                dialog.dismiss();
            }
        });

    }

    private void showWocheTagDialog()

    {

        final Dialog dialog = new Dialog(context);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.preferences) ;

        final RadioGroup radioGroup = (RadioGroup) dialog.findViewById(R.id.immer_einmalig);

        RadioButton radioButton ;

        radioButton = new RadioButton(context) ;
        radioButton.setText(getTextWocheTag(0)) ;
        radioButton.setTextSize(30);
        radioButton.setTextColor(Color.BLACK);
        radioGroup.addView(radioButton);

        radioButton = new RadioButton(context) ;
        radioButton.setText(getTextWocheTag(1)) ;
        radioButton.setTextSize(30);
        radioButton.setTextColor(Color.BLACK);
        radioGroup.addView(radioButton);

        radioButton = new RadioButton(context) ;
        radioButton.setText(getTextWocheTag(2)) ;
        radioButton.setTextSize(30);
        radioButton.setTextColor(Color.BLACK);
        radioGroup.addView(radioButton);

        radioButton = new RadioButton(context) ;
        radioButton.setText(getTextWocheTag(3)) ;
        radioButton.setTextSize(30);
        radioButton.setTextColor(Color.BLACK);
        radioGroup.addView(radioButton);
        dialog.show();

        radioButton = new RadioButton(context) ;
        radioButton.setText(getTextWocheTag(4)) ;
        radioButton.setTextSize(30);
        radioButton.setTextColor(Color.BLACK);
        radioGroup.addView(radioButton);

        radioButton = new RadioButton(context) ;
        radioButton.setText(getTextWocheTag(5)) ;
        radioButton.setTextSize(30);
        radioButton.setTextColor(Color.BLACK);
        radioGroup.addView(radioButton);



        radioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener()
        {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId)
            {
                RadioButton btn ;
                btn = (RadioButton) group.getChildAt(0) ;
                if(btn.getId() == checkedId)
                {
                    editorData.putCellString(TagesSuche.SPALTE_O_ENDE, currentEditRow, "1.W"); ;
                    buttonEnde.setText(getTextWocheTag(0)) ;
                }
                btn = (RadioButton) group.getChildAt(1) ;
                if(btn.getId() == checkedId)
                {
                    editorData.putCellString(TagesSuche.SPALTE_O_ENDE, currentEditRow, "2.W"); ;
                    buttonEnde.setText(getTextWocheTag(1)) ;
                }

                btn = (RadioButton) group.getChildAt(2) ;
                if(btn.getId() == checkedId)
                {
                    editorData.putCellString(TagesSuche.SPALTE_O_ENDE, currentEditRow, "3.W"); ;
                    buttonEnde.setText(getTextWocheTag(2)) ;
                }

                btn = (RadioButton) group.getChildAt(3) ;
                if(btn.getId() == checkedId)
                {
                    editorData.putCellString(TagesSuche.SPALTE_O_ENDE, currentEditRow, "4.W"); ;
                    buttonEnde.setText(getTextWocheTag(3)) ;
                }

                btn = (RadioButton) group.getChildAt(4) ;
                if(btn.getId() == checkedId)
                {
                    editorData.putCellString(TagesSuche.SPALTE_O_ENDE, currentEditRow, "7.T"); ;
                    buttonEnde.setText(getTextWocheTag(4)) ;
                }

                btn = (RadioButton) group.getChildAt(5) ;
                if(btn.getId() == checkedId)
                {
                    editorData.putCellString(TagesSuche.SPALTE_O_ENDE, currentEditRow, "14.T"); ;
                    buttonEnde.setText(getTextWocheTag(5)) ;
                }



                dialog.dismiss();
            }
        });

    }

    private String getTextWocheTag(int index)
    {
        String retString = "" ;

        retString = StaticVariable.getUebersetzung(index + 31) ;

//        switch (index)
//        {
//            case 0: retString = "jede Woche" ; break;
//            case 1: retString = "alle zwei Wochen" ; break;
//            case 2: retString = "alle drei Wochen" ; break;
//            case 3: retString = "alle vier Wochen" ; break;
//            case 4: retString = "jeden siebten Tag" ; break;
//            case 5: retString = "alle 14 Tage" ; break;
//        }

        return retString ;
    }

    private String getTextEinmaligImmer(boolean flag)
    {
        if(flag)
        {
            //return "Immer" ;
            return StaticVariable.getUebersetzung(26) ;
        }
        else
        {
            //return "Einmalig" ;
            return StaticVariable.getUebersetzung(27) ;
        }
    }

    private String getTextSommerWinterImmer(String sommerWinterImmer)
    {
        //String retString = "Immer" ;
        String retString = StaticVariable.getUebersetzung(28);

        switch (sommerWinterImmer)
        {
            //case "1": retString = "Sommer" ;
            case "1": retString = StaticVariable.getUebersetzung(29) ;
                break;
            //case "2": retString = "Winter" ;
            case "2": retString = StaticVariable.getUebersetzung(30) ;
                break;
        }
        return retString ;
    }

private void changeWochentag(int offsetVomMontag)
    {
        if (editorData.getCellString(TagesSuche.SPALTE_E_WOCHENTAG_MONTAG + offsetVomMontag, currentEditRow).toLowerCase().equals("x"))
        {
            editorData.putCellString(TagesSuche.SPALTE_E_WOCHENTAG_MONTAG + offsetVomMontag, currentEditRow, "");

            setWochentagColor(offsetVomMontag, false);

        } else
        {
            editorData.putCellString(TagesSuche.SPALTE_E_WOCHENTAG_MONTAG + offsetVomMontag, currentEditRow, "x");
            setWochentagColor(offsetVomMontag, true);
        }
    }

    private void setWochentagColor(int offsetVomMontag, boolean on_off)
    {
        Button buttonTemp ;
        buttonTemp = buttonWochentage.get(offsetVomMontag) ;

        if(on_off)
        {
            buttonTemp.setTextColor(Color.BLACK);
            buttonTemp.setBackgroundColor(Color.YELLOW);
        }
        else
        {
            buttonTemp.setTextColor(Color.BLACK);
            buttonTemp.setBackgroundColor(Color.LTGRAY);
        }
    }

    private void incrementEditZeile()
    {
        currentEditRow++;
        if (currentEditRow >= editorData.getRowCount() || editorData.getCellString(0, currentEditRow).equals("")) {
            currentEditRow = editorData.isDatabaseMode() ? 0 : 3;
        }
        if (!editorData.isDatabaseMode()) {
            StaticVariable.pathAndFilenameEditorIndex = currentEditRow;
        }
        refreshUIFromCurrentRow();
    }

    public class mDateSetListenerAnfang implements DatePickerDialog.OnDateSetListener
    {
        int mYear ;
        int mMonth ;
        int mDay ;



        @Override
        public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth)
        {

            // getCalender();
            mYear = year;
            mMonth = monthOfYear + 1;
            mDay = dayOfMonth;

            Log.e("onDataSet" , "year=" + year + " monthOfYear=" + monthOfYear + " dayOfMonth=" + dayOfMonth ) ;

            String dateString ;

            dateString = "" + mDay + "." + mMonth ;

            Log.e("onDataSet" , "dateString=" + dateString ) ;

            editorData.putCellString(TagesSuche.SPALTE_N_START,currentEditRow,dateString);


                //buttonAnfang.setText("Anfang " + dateString) ;
                buttonAnfang.setText(StaticVariable.getUebersetzung(21) + " " + dateString) ;

        }


    }

    public class mDateSetListenerEnde implements DatePickerDialog.OnDateSetListener
    {
        int mYear ;
        int mMonth ;
        int mDay ;



        @Override
        public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth)
        {

            // getCalender();
            mYear = year;
            mMonth = monthOfYear + 1;
            mDay = dayOfMonth;

            Log.e("onDataSet" , "year=" + year + " monthOfYear=" + monthOfYear + " dayOfMonth=" + dayOfMonth ) ;

            String dateString ;

            dateString = "" + mDay + "." + mMonth ;

            Log.e("onDataSet" , "dateString=" + dateString ) ;

            editorData.putCellString(TagesSuche.SPALTE_O_ENDE,currentEditRow,dateString);
           //buttonEnde.setText("Ende " + dateString);
            buttonEnde.setText(StaticVariable.getUebersetzung(22) + " " + dateString) ;

        }

    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) // frist speicher ??
    {
        if ((keyCode == KeyEvent.KEYCODE_BACK)) {
            if (StaticConstants.DEBUG) {
                // Log.i("key", "back");
            }

            try {
                editorData.save();
            } catch (Exception e) {
                e.printStackTrace();
            }

            finish();

            return true;
        }

//        if ((keyCode == KeyEvent.KEYCODE_HOME)) {
//            // Log.i("key", "home");
//
//            try
//            {
//                try { if (editorData.isDatabaseMode()) editorData.save(); else ((EditorDataExcel)editorData).getExcelReadWrite().saveAndCloseWorkbooks(); } catch (Exception e) { e.printStackTrace(); }
//            } catch (IOException e)
//            {
//                e.printStackTrace();
//            } catch (WriteException e)
//            {
//                e.printStackTrace();
//            }
//
//            finish();
//
//            return true;
//        }


        return true;

    }


} // ende der Klasse
