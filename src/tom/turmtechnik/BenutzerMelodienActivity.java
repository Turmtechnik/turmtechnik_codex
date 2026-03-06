package tom.turmtechnik;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.TimePickerDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.StrictMode;
import android.util.Log;
import android.view.Display;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import jxl.read.biff.BiffException;


public class BenutzerMelodienActivity extends Activity {

    private static final String sourceFileName = "BenutzerMelodienActivity";
    /** Anzahl Slots wie in der Web-UI (ConfigWebServer.BENUTZERPROGRAMME_MAX). */
    private static final int BENUTZERPROGRAMME_MAX = 20;

//	private final String benutzerMelodien = "/Turmtechnik/Config/Benutzermelodien.xls";

    private String benutzerMelodien = TurmtechnikActivity.benutzerMelodienFileString;

    /** Zwischenablage für Kopieren/Einfügen (wie Web-UI). */
    private static CopyPasteBuffer copyPasteBuffer = null;
    private static class CopyPasteBuffer {
        boolean on;
        int tag, monat, jahr, stunde, minute;
        String melodieName;
    }

    protected static ArrayList<String> filenameSondertag = new ArrayList<String>();
    protected static ArrayList<String> filenameSondermelodien = new ArrayList<String>();
    protected static ArrayList<String> beschriftungSondermelodien = new ArrayList<String>();
    protected static ArrayList<String> beschriftungSofortstart = new ArrayList<String>();

//	protected static final Vector<Integer> minuten =  new Vector<Integer>() ;
//	protected static final Vector<Integer> stunden = new Vector<Integer>() ;
//	protected static final Vector<Integer> monate = new Vector<Integer>() ;
//	protected static final Vector<Integer> tage = new Vector<Integer>() ;

    private TextView outputView;
    private boolean file_ok = false;
    private String inputFileString;
    private int index;
    private static BenutzerMelodienLayout layout;

    private static int hour;
    private static int minute;
    private int globalIndex;
    private boolean doRun = true;

    private ExcelRead excelread;

    private Handler handler;

    private static int displayWidth;
    private static int displayHeight;

    private static int lastBenutzerMelodieIndex = 0;

    private static Calendar calendar;

    /** Referenzen auf Zeilen-Views der Tabellen-UI (für Aktualisierung nach Date/Time-Picker). */
    private Button[] tableDatumButtons;
    private Button[] tableZeitButtons;
    private Spinner[] tableMelodieSpinners;
    private Spinner[] tableTagtypSpinners;
    private Button[] tableSpeichernButtons;

    /** Slots mit ungespeicherten Änderungen (Speichern-Button wird rot). */
    private final Set<Integer> dirtySlots = new HashSet<>();

    private void markDirty(int slotIndex) {
        dirtySlots.add(slotIndex);
        if (tableSpeichernButtons != null && slotIndex >= 0 && slotIndex < tableSpeichernButtons.length && tableSpeichernButtons[slotIndex] != null)
            tableSpeichernButtons[slotIndex].setBackgroundColor(Color.parseColor("#dc3545"));
    }

    /** Speichert alle Slots mit ungespeicherten Änderungen (wie Web-UI „Speichern und verlassen“). */
    private void saveAllDirty() {
        TurmtechnikActivity.ensureBenutzerprogrammeSize(BENUTZERPROGRAMME_MAX);
        List<Integer> list = new ArrayList<>(dirtySlots);
        for (int slotIndex : list) {
            if (slotIndex < 0 || slotIndex >= BENUTZERPROGRAMME_MAX) continue;
            String datumText = (tableDatumButtons != null && tableDatumButtons[slotIndex] != null && tableDatumButtons[slotIndex].getText() != null)
                    ? tableDatumButtons[slotIndex].getText().toString() : "";
            String zeitText = (tableZeitButtons != null && tableZeitButtons[slotIndex] != null && tableZeitButtons[slotIndex].getText() != null)
                    ? tableZeitButtons[slotIndex].getText().toString() : "";
            String melodieName = "";
            if (tableMelodieSpinners != null && tableMelodieSpinners[slotIndex] != null && tableMelodieSpinners[slotIndex].getSelectedItem() != null)
                melodieName = tableMelodieSpinners[slotIndex].getSelectedItem().toString();
            String tagtypName = "";
            if (tableTagtypSpinners != null && tableTagtypSpinners[slotIndex] != null && tableTagtypSpinners[slotIndex].getSelectedItem() != null)
                tagtypName = tableTagtypSpinners[slotIndex].getSelectedItem().toString();
            syncRowToStaticVariable(slotIndex, datumText, zeitText, melodieName, tagtypName);
            if (slotIndex < StaticVariable.benutzerMelodieTasteOn2.size()) StaticVariable.benutzerMelodieTasteOn2.set(slotIndex, true);
            TurmtechnikActivity.saveBenutzerprogrammeToDb(this);
            dirtySlots.remove(slotIndex);
            if (tableSpeichernButtons != null && slotIndex < tableSpeichernButtons.length && tableSpeichernButtons[slotIndex] != null)
                tableSpeichernButtons[slotIndex].setBackgroundColor(Color.parseColor("#28a745"));
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.text_layout);

        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        getDisplayParameter();
        outputView = (TextView) this.findViewById(R.id.textView1);
        outputView.append("\n\n");
        outputView = null;
        System.gc();

        // Neue Oberfläche wie Web-UI: Tabelle mit Dropdown (Melodie), Datum-, Zeitauswahl, Löschen/Kopieren/Einfügen/Speichern
        TurmtechnikActivity.ensureBenutzerprogrammeSize(BENUTZERPROGRAMME_MAX);
        TurmtechnikActivity.loadBenutzerprogrammeFromDb(this);
        setContentView(R.layout.activity_benutzerprogramme);
        initNewTableUi();
        findViewById(R.id.benutzerprogramme_home).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) { showVerlassenBestätigung(); }
        });
        View btnNeu = findViewById(R.id.benutzerprogramme_neu);
        if (btnNeu != null) {
            btnNeu.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) { addNeuesProgramm(); }
            });
        }
        findViewById(R.id.benutzerprogramme_help).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) { startHelpSeite(); }
        });
        // System.xls Sheet 15 (Benutzerprogramme) optional – Daten kommen aus der DB (loadBenutzerprogrammeFromDb)
        String systemXlsPath = TurmtechnikActivity.sdCardPath + StaticConstants.excellSystemString;
        excelread = new ExcelRead();
        try {
            excelread.openXlsSheet(systemXlsPath, 15);
        } catch (BiffException e) {
            Log.w(sourceFileName, "System.xls Sheet 15 nicht lesbar (BiffException). Benutzerprogramme aus DB.", e);
            new LogExcelError(-1, -1, systemXlsPath, 15, sourceFileName, 121);
            excelread = null;
        } catch (IOException e) {
            Log.w(sourceFileName, "System.xls nicht gefunden/nicht lesbar (IOException). Benutzerprogramme aus DB.", e);
            new LogExcelError(-1, -1, systemXlsPath, 15, sourceFileName, 125);
            excelread = null;
        }
    }

    /** Slot gilt als belegt, wenn Aktiv an oder Melodie gesetzt oder Datum/Uhrzeit vom Standard abweicht. */
    private static boolean isSlotBelegt(int slotIndex) {
        TurmtechnikActivity.ensureBenutzerprogrammeSize(BENUTZERPROGRAMME_MAX);
        if (slotIndex < StaticVariable.benutzerMelodieTasteOn2.size() && StaticVariable.benutzerMelodieTasteOn2.get(slotIndex))
            return true;
        String name = getMelodieNameForSlot(slotIndex);
        if (name != null && !name.trim().isEmpty()) return true;
        if (slotIndex < StaticVariable.tage.size() && slotIndex < StaticVariable.monate.size() && slotIndex < StaticVariable.jahre.size()) {
            int t = StaticVariable.tage.get(slotIndex);
            int m = StaticVariable.monate.get(slotIndex);
            if (t != 1 || m != 1) return true;
        }
        if (slotIndex < StaticVariable.stunden.size() && slotIndex < StaticVariable.minuten.size()) {
            int h = StaticVariable.stunden.get(slotIndex);
            int min = StaticVariable.minuten.get(slotIndex);
            if (h != 12 || min != 0) return true;
        }
        return false;
    }

    /** Ersten freien Slot mit Standardwerten belegen und Tabelle neu aufbauen. */
    private void addNeuesProgramm() {
        TurmtechnikActivity.ensureBenutzerprogrammeSize(BENUTZERPROGRAMME_MAX);
        for (int i = 0; i < BENUTZERPROGRAMME_MAX; i++) {
            if (!isSlotBelegt(i)) {
                if (i < StaticVariable.benutzerMelodieTasteOn2.size()) StaticVariable.benutzerMelodieTasteOn2.set(i, true);
                Calendar cal = Calendar.getInstance();
                if (i < StaticVariable.tage.size()) StaticVariable.tage.set(i, cal.get(Calendar.DAY_OF_MONTH));
                if (i < StaticVariable.monate.size()) StaticVariable.monate.set(i, cal.get(Calendar.MONTH) + 1);
                if (i < StaticVariable.jahre.size()) StaticVariable.jahre.set(i, cal.get(Calendar.YEAR));
                if (i < StaticVariable.stunden.size()) StaticVariable.stunden.set(i, 12);
                if (i < StaticVariable.minuten.size()) StaticVariable.minuten.set(i, 0);
                if (StaticVariable.benutzerMelodieName != null && i < StaticVariable.benutzerMelodieName.size()) StaticVariable.benutzerMelodieName.set(i, "");
                TurmtechnikActivity.saveBenutzerprogrammeToDb(this);
                initNewTableUi();
                Toast.makeText(this, "Neues Programm in Slot " + (i + 1) + " angelegt.", Toast.LENGTH_SHORT).show();
                return;
            }
        }
        Toast.makeText(this, "Maximal " + BENUTZERPROGRAMME_MAX + " Programme möglich.", Toast.LENGTH_SHORT).show();
    }

    /** Füllt die Tabellen-UI nur mit belegten Slots: Dropdown Melodie, Datum/Uhrzeit-Button, Löschen/Kopieren/Einfügen/Speichern. */
    private void initNewTableUi() {
        List<PlatinenDatabaseHelper.Melodie> melodienListe = new ArrayList<>();
        try {
            melodienListe = PlatinenDatabaseHelper.getInstance(this).getAllMelodien();
        } catch (Exception e) {
            Log.e(sourceFileName, "Melodien aus DB laden", e);
        }
        final List<String> melodienNamen = new ArrayList<>();
        melodienNamen.add("");
        for (PlatinenDatabaseHelper.Melodie m : melodienListe) {
            String n = m.name != null ? m.name.trim() : "";
            melodienNamen.add(n);
        }

        LinearLayout rowsContainer = findViewById(R.id.benutzerprogramme_rows);
        if (rowsContainer == null) return;
        rowsContainer.removeAllViews();
        tableDatumButtons = new Button[BENUTZERPROGRAMME_MAX];
        tableZeitButtons = new Button[BENUTZERPROGRAMME_MAX];
        tableMelodieSpinners = new Spinner[BENUTZERPROGRAMME_MAX];
        tableTagtypSpinners = new Spinner[BENUTZERPROGRAMME_MAX];
        tableSpeichernButtons = new Button[BENUTZERPROGRAMME_MAX];
        LayoutInflater inflater = LayoutInflater.from(this);

        List<String> tagtypNamen = new ArrayList<>();
        tagtypNamen.add("");
        try {
            List<PlatinenDatabaseHelper.Tagtyp> tagtypenList = PlatinenDatabaseHelper.getInstance(this).getAllTagtypen();
            if (tagtypenList != null) {
                for (PlatinenDatabaseHelper.Tagtyp t : tagtypenList) {
                    if (t != null && t.name != null && !t.name.trim().isEmpty())
                        tagtypNamen.add(t.name.trim());
                }
            }
        } catch (Exception e) {
            Log.e(sourceFileName, "Tagtypen laden", e);
        }
        if (!tagtypNamen.contains("Normalprogramm")) tagtypNamen.add("Normalprogramm");

        List<Integer> belegtIndices = new ArrayList<>();
        for (int i = 0; i < BENUTZERPROGRAMME_MAX; i++) {
            if (isSlotBelegt(i)) belegtIndices.add(i);
        }

        for (int listPos = 0; listPos < belegtIndices.size(); listPos++) {
            final int slotIndex = belegtIndices.get(listPos);
            View row = inflater.inflate(R.layout.item_benutzerprogramm_row, rowsContainer, false);
            TextView nr = row.findViewById(R.id.row_nr);
            Button datumBtn = row.findViewById(R.id.row_datum);
            Button zeitBtn = row.findViewById(R.id.row_zeit);
            Spinner melodieSpinner = row.findViewById(R.id.row_melodie);
            Spinner tagtypSpinner = row.findViewById(R.id.row_tagtyp);
            Button loeschen = row.findViewById(R.id.row_loeschen);
            Button kopieren = row.findViewById(R.id.row_kopieren);
            Button einfuegen = row.findViewById(R.id.row_einfuegen);
            Button speichern = row.findViewById(R.id.row_speichern);

            nr.setText(String.valueOf(listPos + 1));
            datumBtn.setText(formatDatum(slotIndex));
            zeitBtn.setText(formatZeit(slotIndex));
            tableDatumButtons[slotIndex] = datumBtn;
            tableZeitButtons[slotIndex] = zeitBtn;
            tableMelodieSpinners[slotIndex] = melodieSpinner;
            tableTagtypSpinners[slotIndex] = tagtypSpinner;

            ArrayAdapter<String> tagtypAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, tagtypNamen);
            tagtypAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            tagtypSpinner.setAdapter(tagtypAdapter);
            String aktTagtyp = getTagtypNameForSlot(slotIndex);
            if (aktTagtyp != null && !aktTagtyp.isEmpty()) {
                int tpos = tagtypNamen.indexOf(aktTagtyp);
                if (tpos >= 0) tagtypSpinner.setSelection(tpos);
                else {
                    tagtypNamen.add(aktTagtyp);
                    tagtypAdapter.notifyDataSetChanged();
                    tagtypSpinner.setSelection(tagtypNamen.size() - 1);
                }
                TurmtechnikActivity.ensureBenutzerprogrammeSize(BENUTZERPROGRAMME_MAX);
                if (slotIndex < StaticVariable.stunden.size()) StaticVariable.stunden.set(slotIndex, -1);
                if (slotIndex < StaticVariable.minuten.size()) StaticVariable.minuten.set(slotIndex, -1);
                zeitBtn.setText("Ganzer Tag");
                melodieSpinner.setVisibility(View.GONE);
            } else {
                melodieSpinner.setVisibility(View.VISIBLE);
            }

            ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, melodienNamen);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            melodieSpinner.setAdapter(adapter);
            String aktMelodie = getMelodieNameForSlot(slotIndex);
            if (aktMelodie != null && !aktMelodie.isEmpty()) {
                int pos = melodienNamen.indexOf(aktMelodie);
                if (pos >= 0) melodieSpinner.setSelection(pos);
                else {
                    melodienNamen.add(aktMelodie);
                    adapter.notifyDataSetChanged();
                    melodieSpinner.setSelection(melodienNamen.size() - 1);
                }
            }
            einfuegen.setVisibility(copyPasteBuffer != null ? View.VISIBLE : View.GONE);

            // Gespeicherte Programme leicht grün hervorheben
            row.setBackgroundColor(Color.parseColor("#E8F5E9"));

            tableSpeichernButtons[slotIndex] = speichern;
            speichern.setBackgroundColor(dirtySlots.contains(slotIndex) ? Color.parseColor("#dc3545") : Color.parseColor("#28a745"));

            datumBtn.setOnClickListener(v -> {
                globalIndex = slotIndex;
                showDialog(1);
            });
            zeitBtn.setOnClickListener(v -> {
                globalIndex = slotIndex;
                showDialog(0);
            });
            Button ganzerTagBtn = row.findViewById(R.id.row_ganzer_tag);
            ganzerTagBtn.setOnClickListener(v -> {
                TurmtechnikActivity.ensureBenutzerprogrammeSize(BENUTZERPROGRAMME_MAX);
                if (slotIndex < StaticVariable.stunden.size()) StaticVariable.stunden.set(slotIndex, -1);
                if (slotIndex < StaticVariable.minuten.size()) StaticVariable.minuten.set(slotIndex, -1);
                zeitBtn.setText("Ganzer Tag");
                markDirty(slotIndex);
            });
            melodieSpinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(android.widget.AdapterView<?> parent, View view, int pos, long id) {
                    String sel = (pos > 0 && pos <= melodienNamen.size()) ? melodienNamen.get(pos) : "";
                    TurmtechnikActivity.ensureBenutzerprogrammeSize(BENUTZERPROGRAMME_MAX);
                    if (StaticVariable.benutzerMelodieName != null && slotIndex < StaticVariable.benutzerMelodieName.size())
                        StaticVariable.benutzerMelodieName.set(slotIndex, sel != null ? sel : "");
                    StaticVariable.benutzerGeandert = true;
                    markDirty(slotIndex);
                }
                @Override
                public void onNothingSelected(android.widget.AdapterView<?> parent) {}
            });
            tagtypSpinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(android.widget.AdapterView<?> parent, View view, int pos, long id) {
                    String sel = (pos >= 0 && pos < tagtypNamen.size()) ? (tagtypNamen.get(pos) != null ? tagtypNamen.get(pos) : "") : "";
                    TurmtechnikActivity.ensureBenutzerprogrammeSize(BENUTZERPROGRAMME_MAX);
                    if (StaticVariable.benutzerTagtypName != null && slotIndex < StaticVariable.benutzerTagtypName.size())
                        StaticVariable.benutzerTagtypName.set(slotIndex, sel != null ? sel : "");
                    // Bei Programmtag-Auswahl: automatisch Ganzer Tag setzen und Melodie ausblenden
                    if (sel != null && !sel.trim().isEmpty()) {
                        if (slotIndex < StaticVariable.stunden.size()) StaticVariable.stunden.set(slotIndex, -1);
                        if (slotIndex < StaticVariable.minuten.size()) StaticVariable.minuten.set(slotIndex, -1);
                        zeitBtn.setText("Ganzer Tag");
                        melodieSpinner.setVisibility(View.GONE);
                    } else {
                        if (slotIndex < StaticVariable.stunden.size()) StaticVariable.stunden.set(slotIndex, 12);
                        if (slotIndex < StaticVariable.minuten.size()) StaticVariable.minuten.set(slotIndex, 0);
                        zeitBtn.setText(formatZeit(slotIndex));
                        melodieSpinner.setVisibility(View.VISIBLE);
                    }
                    StaticVariable.benutzerGeandert = true;
                    markDirty(slotIndex);
                }
                @Override
                public void onNothingSelected(android.widget.AdapterView<?> parent) {}
            });
            loeschen.setOnClickListener(v -> {
                TurmtechnikActivity.ensureBenutzerprogrammeSize(BENUTZERPROGRAMME_MAX);
                if (slotIndex < StaticVariable.benutzerMelodieTasteOn2.size()) StaticVariable.benutzerMelodieTasteOn2.set(slotIndex, false);
                if (slotIndex < StaticVariable.tage.size()) StaticVariable.tage.set(slotIndex, 1);
                if (slotIndex < StaticVariable.monate.size()) StaticVariable.monate.set(slotIndex, 1);
                if (slotIndex < StaticVariable.jahre.size()) StaticVariable.jahre.set(slotIndex, Calendar.getInstance().get(Calendar.YEAR));
                if (slotIndex < StaticVariable.stunden.size()) StaticVariable.stunden.set(slotIndex, 12);
                if (slotIndex < StaticVariable.minuten.size()) StaticVariable.minuten.set(slotIndex, 0);
                if (StaticVariable.benutzerMelodieName != null && slotIndex < StaticVariable.benutzerMelodieName.size()) StaticVariable.benutzerMelodieName.set(slotIndex, "");
                TurmtechnikActivity.saveBenutzerprogrammeToDb(BenutzerMelodienActivity.this);
                initNewTableUi();
                Toast.makeText(BenutzerMelodienActivity.this, "Programm gelöscht.", Toast.LENGTH_SHORT).show();
            });
            kopieren.setOnClickListener(v -> {
                copyPasteBuffer = new CopyPasteBuffer();
                copyPasteBuffer.on = true;
                copyPasteBuffer.tag = slotIndex < StaticVariable.tage.size() ? StaticVariable.tage.get(slotIndex) : 1;
                copyPasteBuffer.monat = slotIndex < StaticVariable.monate.size() ? StaticVariable.monate.get(slotIndex) : 1;
                copyPasteBuffer.jahr = slotIndex < StaticVariable.jahre.size() ? StaticVariable.jahre.get(slotIndex) : Calendar.getInstance().get(Calendar.YEAR);
                copyPasteBuffer.stunde = slotIndex < StaticVariable.stunden.size() ? StaticVariable.stunden.get(slotIndex) : 12;
                copyPasteBuffer.minute = slotIndex < StaticVariable.minuten.size() ? StaticVariable.minuten.get(slotIndex) : 0;
                copyPasteBuffer.melodieName = getMelodieNameForSlot(slotIndex);
                if (copyPasteBuffer.melodieName == null) copyPasteBuffer.melodieName = "";
                for (int j = 0; j < rowsContainer.getChildCount(); j++) {
                    View r = rowsContainer.getChildAt(j);
                    Button ef = r.findViewById(R.id.row_einfuegen);
                    if (ef != null) ef.setVisibility(View.VISIBLE);
                }
                Toast.makeText(BenutzerMelodienActivity.this, "Slot " + (slotIndex + 1) + " kopiert. Einfügen bei anderem Slot wählen.", Toast.LENGTH_SHORT).show();
            });
            einfuegen.setOnClickListener(v -> {
                if (copyPasteBuffer == null) return;
                TurmtechnikActivity.ensureBenutzerprogrammeSize(BENUTZERPROGRAMME_MAX);
                if (slotIndex < StaticVariable.benutzerMelodieTasteOn2.size()) StaticVariable.benutzerMelodieTasteOn2.set(slotIndex, true);
                if (slotIndex < StaticVariable.tage.size()) StaticVariable.tage.set(slotIndex, copyPasteBuffer.tag);
                if (slotIndex < StaticVariable.monate.size()) StaticVariable.monate.set(slotIndex, copyPasteBuffer.monat);
                if (slotIndex < StaticVariable.jahre.size()) StaticVariable.jahre.set(slotIndex, copyPasteBuffer.jahr);
                if (slotIndex < StaticVariable.stunden.size()) StaticVariable.stunden.set(slotIndex, copyPasteBuffer.stunde);
                if (slotIndex < StaticVariable.minuten.size()) StaticVariable.minuten.set(slotIndex, copyPasteBuffer.minute);
                if (StaticVariable.benutzerMelodieName != null && slotIndex < StaticVariable.benutzerMelodieName.size())
                    StaticVariable.benutzerMelodieName.set(slotIndex, copyPasteBuffer.melodieName != null ? copyPasteBuffer.melodieName : "");
                datumBtn.setText(formatDatum(slotIndex));
                zeitBtn.setText(formatZeit(slotIndex));
                int pos = melodienNamen.indexOf(copyPasteBuffer.melodieName != null ? copyPasteBuffer.melodieName : "");
                if (pos >= 0) melodieSpinner.setSelection(pos);
                TurmtechnikActivity.saveBenutzerprogrammeToDb(BenutzerMelodienActivity.this);
                dirtySlots.remove(slotIndex);
                speichern.setBackgroundColor(Color.parseColor("#28a745"));
                Toast.makeText(BenutzerMelodienActivity.this, "In Slot " + (slotIndex + 1) + " eingefügt.", Toast.LENGTH_SHORT).show();
            });
            speichern.setOnClickListener(v -> {
                TurmtechnikActivity.ensureBenutzerprogrammeSize(BENUTZERPROGRAMME_MAX);
                String tagtypVal = (tagtypSpinner.getSelectedItem() != null) ? tagtypSpinner.getSelectedItem().toString() : "";
                syncRowToStaticVariable(slotIndex, datumBtn.getText() != null ? datumBtn.getText().toString() : "", zeitBtn.getText() != null ? zeitBtn.getText().toString() : "", melodieSpinner.getSelectedItem() != null ? melodieSpinner.getSelectedItem().toString() : "", tagtypVal);
                StaticVariable.benutzerMelodieTasteOn2.set(slotIndex, true);
                TurmtechnikActivity.saveBenutzerprogrammeToDb(BenutzerMelodienActivity.this);
                dirtySlots.remove(slotIndex);
                speichern.setBackgroundColor(Color.parseColor("#28a745"));
                StaticVariable.benutzerGeandert = true;
                StaticVariable.programmeDatabaseChanged = true;
                Toast.makeText(BenutzerMelodienActivity.this, "Slot " + (slotIndex + 1) + " gespeichert.", Toast.LENGTH_SHORT).show();
            });

            rowsContainer.addView(row);
        }
    }

    /**
     * Übernimmt die angezeigten Werte einer Zeile (Datum, Zeit, Melodie, Programmtag) in StaticVariable für den Slot.
     */
    private void syncRowToStaticVariable(int slotIndex, String datumText, String zeitText, String melodieName, String tagtypName) {
        TurmtechnikActivity.ensureBenutzerprogrammeSize(BENUTZERPROGRAMME_MAX);
        if (slotIndex < 0 || slotIndex >= BENUTZERPROGRAMME_MAX) return;
        // Datum parsen (dd.MM.yyyy oder d.M.yyyy)
        if (datumText != null && !datumText.isEmpty() && !"Datum".equals(datumText)) {
            String[] d = datumText.trim().split("\\.");
            if (d.length >= 3) {
                try {
                    int tag = Integer.parseInt(d[0].trim());
                    int monat = Integer.parseInt(d[1].trim());
                    int jahr = Integer.parseInt(d[2].trim());
                    if (slotIndex < StaticVariable.tage.size()) StaticVariable.tage.set(slotIndex, tag);
                    if (slotIndex < StaticVariable.monate.size()) StaticVariable.monate.set(slotIndex, monat >= 1 && monat <= 12 ? monat : 1);
                    if (slotIndex < StaticVariable.jahre.size()) StaticVariable.jahre.set(slotIndex, jahr);
                } catch (NumberFormatException ignored) {}
            }
        }
        // Zeit parsen: "Ganzer Tag" oder leer = nur Programmtag (keine Uhrzeit), sonst HH:mm
        if (zeitText != null && "Ganzer Tag".equalsIgnoreCase(zeitText.trim())) {
            if (slotIndex < StaticVariable.stunden.size()) StaticVariable.stunden.set(slotIndex, -1);
            if (slotIndex < StaticVariable.minuten.size()) StaticVariable.minuten.set(slotIndex, -1);
        } else if (zeitText != null && !zeitText.isEmpty() && !"Zeit".equals(zeitText) && !"Ganzer Tag".equalsIgnoreCase(zeitText.trim())) {
            String[] z = zeitText.trim().split(":");
            if (z.length >= 2) {
                try {
                    int stunde = Integer.parseInt(z[0].trim());
                    int minute = Integer.parseInt(z[1].trim());
                    if (slotIndex < StaticVariable.stunden.size()) StaticVariable.stunden.set(slotIndex, Math.max(0, Math.min(23, stunde)));
                    if (slotIndex < StaticVariable.minuten.size()) StaticVariable.minuten.set(slotIndex, Math.max(0, Math.min(59, minute)));
                } catch (NumberFormatException ignored) {}
            }
        }
        if (StaticVariable.benutzerMelodieName != null && slotIndex < StaticVariable.benutzerMelodieName.size())
            StaticVariable.benutzerMelodieName.set(slotIndex, melodieName != null ? melodieName.trim() : "");
        if (StaticVariable.benutzerTagtypName != null && slotIndex < StaticVariable.benutzerTagtypName.size())
            StaticVariable.benutzerTagtypName.set(slotIndex, tagtypName != null ? tagtypName.trim() : "");
    }

    private static String formatDatum(int index) {
        if (index >= StaticVariable.tage.size() || index >= StaticVariable.monate.size() || index >= StaticVariable.jahre.size()) return "Datum";
        int t = StaticVariable.tage.get(index);
        int m = StaticVariable.monate.get(index);
        int y = StaticVariable.jahre.get(index);
        if (t <= 0 || m <= 0) return "Datum";
        return pad(t) + "." + pad(m) + "." + y;
    }

    private static String formatZeit(int index) {
        if (index >= StaticVariable.stunden.size() || index >= StaticVariable.minuten.size()) return "Zeit";
        int h = StaticVariable.stunden.get(index);
        int m = StaticVariable.minuten.get(index);
        if (h < 0 || m < 0) return "Ganzer Tag";
        return pad(h) + ":" + pad(m);
    }

    /** Holt den TimePicker aus dem Dialog (funktioniert auch vor API 26, wo getTimePicker() nicht existiert). */
    private static TimePicker findTimePickerInDialog(Dialog dialog) {
        if (dialog == null || dialog.getWindow() == null) return null;
        View root = dialog.getWindow().getDecorView();
        if (root instanceof ViewGroup) return findTimePickerInView((ViewGroup) root);
        return null;
    }

    private static TimePicker findTimePickerInView(ViewGroup group) {
        for (int i = 0; i < group.getChildCount(); i++) {
            View v = group.getChildAt(i);
            if (v instanceof TimePicker) return (TimePicker) v;
            if (v instanceof ViewGroup) {
                TimePicker tp = findTimePickerInView((ViewGroup) v);
                if (tp != null) return tp;
            }
        }
        return null;
    }

    // Alter Code (Layout aus Excel/DB) entfernt – es wird die neue Tabellen-UI (initNewTableUi) verwendet.

    public static void initSofortStartFromInternet() {
        Log.e("bin vor", "initDateAndTime");
        initDateAndTime(StaticVariable.benutzerMelodienIndex); // momentanes Datum, und
        // momentane Zeit + 1 Minute
        Log.e("bin nach", "dem initDateAndTime");
        StaticVariable.benutzerMelodieTasteOn2.set(StaticVariable.benutzerMelodienIndex, true);
        Log.e("initSofortStart", "SEND DATUM");
        if (layout != null) {
            layout.setTimerStartButtonGreen(StaticVariable.benutzerMelodienIndex);
            layout.makeTextDateButton(StaticVariable.benutzerMelodienIndex);
            layout.makeTextTimeButton(StaticVariable.benutzerMelodienIndex);
        }
    }

    public static void printDateAndTime() {
        if (layout != null) {
            layout.makeTextDateButton(StaticVariable.benutzerMelodienIndex);
            layout.makeTextTimeButton(StaticVariable.benutzerMelodienIndex);
        }
    }

    // das braucht einen thread in main thread ... wegen Aenderungen in der View
    public void startUpdateInfoText() {
        Runnable runnable = new Runnable() {

            @Override
            public void run() {

                while (doRun) {
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            // TODO Auto-generated method stub

                            //layout.printTasten();

                            int schleifeMax = StaticVariable.benutzerMelodieTasteOn2.size();
                            if (layout != null) {
                                for (int i = 0; i < schleifeMax; i++) {
                                    if (StaticVariable.benutzerMelodieTasteOn2.get(i) == true) {
                                        layout.setTimerStartButtonGreen(i);
                                    } else {
                                        layout.setTimerStartButtonGray(i);
                                    }
                                    layout.makeTextDateButton(i);
                                    layout.makeTextTimeButton(i);
                                }
                            }
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
                int hour;
                int minute;

                //Log.e("dialog" , "first time") ;

                //Log.e("global" , "index=" + globalIndex) ;
                if (StaticVariable.minuten.get(globalIndex) == -1) {
                    calendar = Calendar.getInstance();

                    minute = calendar.get(Calendar.MINUTE);  // minuten takt von der Echtzeit uhr
                    hour = calendar.get(Calendar.HOUR_OF_DAY);

                    minute++;
                    if (minute > 59) {
                        minute = 0;
                        hour++;
                        if (hour > 23) {
                            hour = 0;
                        }
                    }


                } else {
                    minute = StaticVariable.minuten.get(globalIndex);
                    hour = StaticVariable.stunden.get(globalIndex);
                }

                boolean flag_24 = true;
                // TimePickerDialog so anpassen, dass Tastatureingaben beim OK übernommen werden
                return new TimePickerDialog(this, timePickerListener, hour, minute, flag_24) {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (which == DialogInterface.BUTTON_POSITIVE) {
                            TimePicker tp = findTimePickerInDialog(this);
                            if (tp != null) {
                                tp.clearFocus(); // Tastatureingaben werden so in den Picker übernommen
                                int h = Build.VERSION.SDK_INT >= 23 ? tp.getHour() : tp.getCurrentHour();
                                int m = Build.VERSION.SDK_INT >= 23 ? tp.getMinute() : tp.getCurrentMinute();
                                timePickerListener.onTimeSet(tp, h, m);
                            }
                            dismiss();
                        } else {
                            super.onClick(dialog, which);
                        }
                    }
                };

            case 1:

                //Log.e("global" , "index=" + globalIndex) ;

                int year;
                int monthOfYear;
                int dayOfMonth;

                calendar = Calendar.getInstance();
                year = calendar.get(Calendar.YEAR);

                if (StaticVariable.tage.get(globalIndex) == -1) {

                    dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH);  // minuten takt von der Echtzeit uhr
                    monthOfYear = calendar.get(Calendar.MONTH);

                } else {
                    dayOfMonth = StaticVariable.tage.get(globalIndex);
                    int monatStored = StaticVariable.monate.get(globalIndex);
                    monthOfYear = (monatStored >= 1 && monatStored <= 12) ? (monatStored - 1) : calendar.get(Calendar.MONTH);

                }

                // DatePickerDialog so anpassen, dass Tastatureingaben beim OK übernommen werden
                return new DatePickerDialog(this, datePickerListener, year, monthOfYear, dayOfMonth) {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (which == DialogInterface.BUTTON_POSITIVE) {
                            DatePicker dp = getDatePicker();
                            if (dp != null) {
                                dp.clearFocus(); // Tastatureingaben werden so in den Picker übernommen
                                int y = dp.getYear();
                                int mo = dp.getMonth();
                                int d = dp.getDayOfMonth();
                                datePickerListener.onDateSet(dp, y, mo, d);
                            }
                            dismiss();
                        } else {
                            super.onClick(dialog, which);
                        }
                    }
                };
            //    return new DatePickerDialog(this, 0, datePickerListener, year, monthOfYear, dayOfMonth) ;
        }
        return null;

    }

    @Override
    protected void onPrepareDialog(int id, Dialog dialog) {
        switch (id) {
            case 0:
                int hour;
                int minute;

                //Log.e("dialog" , "first time") ;

                //Log.e("global" , "index=" + globalIndex) ;
                if (StaticVariable.minuten.get(globalIndex) == -1) {
                    calendar = Calendar.getInstance();

                    minute = calendar.get(Calendar.MINUTE);  // minuten takt von der Echtzeit uhr
                    hour = calendar.get(Calendar.HOUR_OF_DAY);

                    minute++;
                    if (minute > 59) {
                        minute = 0;
                        hour++;
                        if (hour > 23) {
                            hour = 0;
                        }
                    }


                } else {
                    minute = StaticVariable.minuten.get(globalIndex);
                    hour = StaticVariable.stunden.get(globalIndex);
                }

                boolean flag_24 = true;
                // set time picker as current time
                ((TimePickerDialog) dialog).updateTime(hour, minute);
                break;

            case 1:

                //Log.e("global" , "index=" + globalIndex) ;

                int year;
                int monthOfYear;
                int dayOfMonth;

                calendar = Calendar.getInstance();
                year = calendar.get(Calendar.YEAR);

                if (StaticVariable.tage.get(globalIndex) == -1) {

                    dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH);  // minuten takt von der Echtzeit uhr
                    monthOfYear = calendar.get(Calendar.MONTH);

                } else {
                    dayOfMonth = StaticVariable.tage.get(globalIndex);
                    int monatStored = StaticVariable.monate.get(globalIndex);
                    monthOfYear = (monatStored >= 1 && monatStored <= 12) ? (monatStored - 1) : calendar.get(Calendar.MONTH);

                }
                ((DatePickerDialog) dialog).updateDate(year, monthOfYear, dayOfMonth);
        }

    }


    private static void initDateAndTime(int index) {
        // 31.01.2015 wegen vorlauf jetzt momentan + 3 minuten
        // 14.02.2015 Achtung wegen vorlauf in Melodie
        //            und Vorschwingen
        //            zu den 3 Minuten dazurechnen!!

        calendar = Calendar.getInstance();

        minute = calendar.get(Calendar.MINUTE);  // minuten takt von der Echtzeit uhr
        hour = calendar.get(Calendar.HOUR_OF_DAY);

        int incrementMinuten = 1;

        int vorlaufPlusVorschwingenSekunden = getVorlaufPlusVorschwingenSekunden(index);

        Log.e("vorlaufPlusVorschw.", "Sekunden=" + vorlaufPlusVorschwingenSekunden);

        incrementMinuten += (vorlaufPlusVorschwingenSekunden / 60) + 1;

        Log.e("increment", "minuten=" + incrementMinuten);

        for (int i = 0; i < incrementMinuten; i++) {
            minute++;
            if (minute > 59) {
                minute = 0;
                hour++;
                if (hour > 23) {
                    hour = 0;
                }
            }
        }

        StaticVariable.minuten.set(index, minute);
        StaticVariable.stunden.set(index, hour);

        StaticVariable.tage.set(index, (calendar.get(Calendar.DAY_OF_MONTH)));
        StaticVariable.monate.set(index, (calendar.get(Calendar.MONTH) + 1)); // 1-12 (Januar=1)
        StaticVariable.jahre.set(index, (calendar.get((Calendar.YEAR))));

        UhrThread.newSearchAutomaticStart = true; // sofort neu suchen

    }

    private static int getVorlaufPlusVorschwingenSekunden(int index) {
        int vorlaufPlusVorschwingenSekunden = 0;

        String pathAndFilenameMelodie =
                TurmtechnikActivity.sdCardPath
                        + "/Turmtechnik/Melodien/"
                        + filenameSondermelodien.get(index);


        ExcelRead excelRead = null;
        excelRead = new ExcelRead();

        try {
            excelRead.openXls(pathAndFilenameMelodie);
        } catch (BiffException e) {
            ////Log.e("getMeloieKloeppelVerwendet" , "BiffException") ;
            new LogExcelError(-1, -1, pathAndFilenameMelodie, 0, sourceFileName, 577);
            e.printStackTrace();
        } catch (IOException e) {
            ////Log.e("getMeloieKloeppelVerwendet" , "IOException") ;
            new LogExcelError(-1, -1, pathAndFilenameMelodie, 0, sourceFileName, 581);
            e.printStackTrace();
        }

        if (excelRead == null) {
            return 0;
        }

        int zeilen = excelRead.getCellZeilen();
        int spalten = excelRead.getCellSpalten();

        int vorschwingzeiten = StaticVariable.vorschwingZeitSekunden.size();

        StaticVariable.vorschwingenStartzeitenMotorRelais.clear();

        int vorlaufVonDerMelodie = 0;

        try {
            String vorlaufAusMelodieString = excelRead.getCellString(1, 1);
            vorlaufVonDerMelodie = (Integer.parseInt(vorlaufAusMelodieString)) * 60; // Minuten --> Sekunden
        } catch (Exception e) {

        }


        int vorschwingenZeitSekunden = 0;

        // L1 ... LX pruefen
        for (int i = 0; i < vorschwingzeiten; i++) {

            String nullEinsString = "";

            try {
                nullEinsString = excelRead.getCellString(4 + i, 3);

            } catch (Exception e) {

            }

            if (nullEinsString.equals("1")) {
                vorschwingenZeitSekunden = Math.max(vorschwingenZeitSekunden, StaticVariable.vorschwingZeitSekunden.get(i));
            }

        }

        if (excelRead != null) {
            excelRead.closeWorkbook();
        }
        return vorschwingenZeitSekunden + vorlaufVonDerMelodie;
    }


    private static void clrDateAndTime(int index) {
        StaticVariable.minuten.set(index, -1);
        StaticVariable.stunden.set(index, -1);
        StaticVariable.tage.set(index, -1);
        StaticVariable.monate.set(index, -1);
        StaticVariable.jahre.set(index, -1);
    }

    private DatePickerDialog.OnDateSetListener datePickerListener = new DatePickerDialog.OnDateSetListener() {

        @Override
        public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {

            //Log.i("Tag" , "= " +  dayOfMonth) ;
            //Log.i("Monat" , "= " + monthOfYear) ;
            //Log.i("Jahr" , "= " + year) ;

            StaticVariable.tage.set(globalIndex, dayOfMonth);
            StaticVariable.monate.set(globalIndex, monthOfYear + 1); // 1-12 (DatePicker liefert 0-11)
            StaticVariable.jahre.set(globalIndex, year);

            if (tableDatumButtons != null && globalIndex >= 0 && globalIndex < tableDatumButtons.length && tableDatumButtons[globalIndex] != null)
                tableDatumButtons[globalIndex].setText(formatDatum(globalIndex));
            else if (layout != null)
                layout.makeTextDateButton(globalIndex);
            markDirty(globalIndex);
        }
    };

    private TimePickerDialog.OnTimeSetListener timePickerListener = new TimePickerDialog.OnTimeSetListener() {

        public void onTimeSet(TimePicker view, int selectedHour,
                              int selectedMinute) {

            StaticVariable.minuten.set(globalIndex, selectedMinute);
            StaticVariable.stunden.set(globalIndex, selectedHour);

            if (tableZeitButtons != null && globalIndex >= 0 && globalIndex < tableZeitButtons.length && tableZeitButtons[globalIndex] != null)
                tableZeitButtons[globalIndex].setText(formatZeit(globalIndex));
            else if (layout != null)
                layout.makeTextTimeButton(globalIndex);
            markDirty(globalIndex);
        }
    };


    private void startProgrammKontrolle() {
        Intent activityProgrammKontrolle = new Intent(BenutzerMelodienActivity.this, ProgrammKontrolleActivity.class);
        BenutzerMelodienActivity.this.startActivity(activityProgrammKontrolle);
    }

    private void startHelpSeite() {
//		    	String helpFileName = "" ;
//		    	// lese Help File Name
//		    	ExcelRead excelRead = new ExcelRead() ;
//		    	excelRead.openXlsSheet(sdCardPath + "/Turmtechnik/Config/System.xls", BESCHRIFTUNG_GLOCKEN_SHEET);
//		    	int max_zeilen = excelRead.getCellZeilen() ;
//		    	for (int i = 1 ; i < max_zeilen; i ++)
//		    	{
//		    		if(excelRead.getCellString(3, i).equals("103"))
//		    		{
//		    			// wenn die Help Taste gefunden ist:
//		    			//Log.i("103" , "gefunden") ;
//		    			//Log.i("index i" , "=" + i) ;
//		    			helpFileName = (excelRead.getCellString(1, i));
//		    			//Log.i("helpFileName" , "=" + helpFileName) ;
//		    			break ;
//		    		}
//		    	}
//		    	excelRead.closeWorkbook() ;

        Intent activityHelpSeite = new Intent(this, HelpSeiteActivity.class);

        //Log.i("helpFileName" , "=" + helpFileName) ;
        String helpFileName = "help3.png";

        activityHelpSeite.putExtra("help_file_name", helpFileName);

        this.startActivity(activityHelpSeite);
    }

    private void getDisplayParameter() {
        Display display = this.getWindowManager().getDefaultDisplay();
        displayWidth = display.getWidth();
        displayHeight = display.getHeight();
    }


    private void endActivitySonderMelodien() {
        doRun = false;
        if (excelread != null) {
            try {
                excelread.closeWorkbook();
            } catch (Exception e) {
                Log.e(sourceFileName, "closeWorkbook beim Beenden", e);
            }
            excelread = null;
        }
        TurmtechnikActivity.saveBenutzerprogrammeToDb(this);
        System.gc();
        finish();
    }

    /** Bestätigungsdialog vor Verlassen – wie Web-UI: bei ungespeicherten Änderungen Speichern / Verwerfen / Abbrechen. */
    private void showVerlassenBestätigung() {
        if (!dirtySlots.isEmpty()) {
            new AlertDialog.Builder(this)
                    .setMessage("Sie haben ungespeicherte Änderungen. Möchten Sie vor dem Verlassen speichern?")
                    .setPositiveButton("Speichern und verlassen", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            saveAllDirty();
                            Toast.makeText(BenutzerMelodienActivity.this, "Alle Änderungen gespeichert.", Toast.LENGTH_SHORT).show();
                            endActivitySonderMelodien();
                        }
                    })
                    .setNeutralButton("Verwerfen und verlassen", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            TurmtechnikActivity.loadBenutzerprogrammeFromDb(BenutzerMelodienActivity.this);
                            doRun = false;
                            if (excelread != null) {
                                try { excelread.closeWorkbook(); } catch (Exception e) { Log.e(sourceFileName, "closeWorkbook", e); }
                                excelread = null;
                            }
                            System.gc();
                            finish();
                        }
                    })
                    .setNegativeButton("Abbrechen", null)
                    .show();
        } else {
            new AlertDialog.Builder(this)
                    .setMessage("Zur Startseite (Layout) wechseln?")
                    .setPositiveButton("Verlassen", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            endActivitySonderMelodien();
                        }
                    })
                    .setNegativeButton("Abbrechen", null)
                    .show();
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            showVerlassenBestätigung();
            return true;
        }
        return false;
    }

    // return leeren String "" wenn Zeit nicht passt, sonst Filename von Melodie.xls
    public static String checkStartBenutzerMelodie(int jahr, int monat, int tag, int stunde, int minute) {
        lastBenutzerMelodieIndex = 0;
        int temp = StaticVariable.benutzerMelodieTasteOn2.size();
        // Sicherstellen, dass alle Vektoren mindestens temp Einträge haben (IndexOutOfBounds vermeiden)
        if (StaticVariable.minuten.size() < temp || StaticVariable.stunden.size() < temp
                || StaticVariable.tage.size() < temp || StaticVariable.monate.size() < temp || StaticVariable.jahre.size() < temp) {
            return "";
        }

        for (int i = 0; i < temp; i++) {
            if (StaticVariable.benutzerMelodieTasteOn2.get(i) == true) {
                int slotMin = StaticVariable.minuten.get(i);
                int slotStd = StaticVariable.stunden.get(i);
                if (slotStd < 0 || slotMin < 0) continue; // Nur Programmtag (ganzer Tag), keine Melodie-Zeit
                if (minute == slotMin) {
                    if ((stunde == slotStd) || (stunde > 23)) {
                        if (tag == StaticVariable.tage.get(i)) {
                            if ((monat + 1) == StaticVariable.monate.get(i)) { // monat 0-11 (Calendar), monate 1-12
                                if (jahr == StaticVariable.jahre.get(i)) {
                                    String mel = getMelodieNameForSlot(i);
                                    if (mel == null || mel.toLowerCase().equals("null")) {
                                        return "";
                                    } else {
                                        return mel;
                                    }
                                }
                            }
                        }
                    }
                }
            }

            lastBenutzerMelodieIndex++;
        }
        return "";
    }

    /**
     * Prüft, ob das gegebene Datum (tag, monat, jahr) mit einem aktiven Benutzerprogramm-Slot übereinstimmt.
     * Gibt bei Treffer immer "Benutzerprogramm" zurück (Priorität vor Normal-/Feiertagsprogramm),
     * unabhängig von filenameSondertag (damit Web-UI-konfigurierte Programme erkannt werden).
     * monat: Calendar.MONTH (0–11).
     */
    public static String isBenutzerProgrammTag(int tag, int monat, int jahr) {
        int benutzerMelodienTasteOnSize = StaticVariable.benutzerMelodieTasteOn2.size();
        if (StaticVariable.tage.size() < benutzerMelodienTasteOnSize || StaticVariable.monate.size() < benutzerMelodienTasteOnSize
                || StaticVariable.jahre.size() < benutzerMelodienTasteOnSize) {
            return "";
        }

        for (int i = 0; i < benutzerMelodienTasteOnSize; i++) {
            if (StaticVariable.benutzerMelodieTasteOn2.get(i) == true) {
                if (tag == StaticVariable.tage.get(i)) {
                    if ((monat + 1) == StaticVariable.monate.get(i)) { // monat 0-11 (Calendar), monate 1-12
                        if (jahr == StaticVariable.jahre.get(i)) {
                            Log.e("isBenutzerProgammTag", "Benutzerprogramm-Slot " + i + " trifft auf " + tag + "." + (monat + 1) + "." + jahr);
                            return "Benutzerprogramm";
                        }
                    }
                }
            }
        }
        return "";
    }

    /** Melodiename für Slot i: aus DB (StaticVariable.benutzerMelodieName) oder Excel (filenameSondermelodien). */
    public static String getMelodieNameForSlot(int i) {
        if (StaticVariable.benutzerMelodieName != null && i < StaticVariable.benutzerMelodieName.size()) {
            String m = StaticVariable.benutzerMelodieName.get(i);
            if (m != null && !m.trim().isEmpty()) return m;
        }
        if (filenameSondermelodien != null && i < filenameSondermelodien.size()) {
            return filenameSondermelodien.get(i);
        }
        return "";
    }

    /** Programmtag (Tagtyp) für Benutzerprogramm-Slot i (aus DB/StaticVariable). */
    public static String getTagtypNameForSlot(int i) {
        if (StaticVariable.benutzerTagtypName != null && i < StaticVariable.benutzerTagtypName.size()) {
            String t = StaticVariable.benutzerTagtypName.get(i);
            return t != null ? t.trim() : "";
        }
        return "";
    }

    public static String checkStartBenutzerTag(int monat, int tag) {

        lastBenutzerMelodieIndex = 0;
        int temp = StaticVariable.benutzerMelodieTasteOn2.size();

        for (int i = 0; i < temp; i++) {
            if (StaticVariable.benutzerMelodieTasteOn2.get(i) == true) {


                if (tag == StaticVariable.tage.get(i)) {
                    if ((monat + 1) == StaticVariable.monate.get(i)) { // monat 0-11 (Calendar), monate 1-12
                        return getMelodieNameForSlot(i);
                    }
                }

            }

            lastBenutzerMelodieIndex++;
        }
        return "";
    }

    public static String getBeschriftungTaste() {
        if (beschriftungSondermelodien == null || lastBenutzerMelodieIndex < 0
                || lastBenutzerMelodieIndex >= beschriftungSondermelodien.size())
            return "";
        return beschriftungSondermelodien.get(lastBenutzerMelodieIndex);
    }

    public synchronized static String getMelodieStartTime() {
        if (StaticVariable.stunden == null || StaticVariable.minuten == null
                || lastBenutzerMelodieIndex < 0
                || lastBenutzerMelodieIndex >= StaticVariable.stunden.size()
                || lastBenutzerMelodieIndex >= StaticVariable.minuten.size())
            return "";
        return "" + (pad(StaticVariable.stunden.get(lastBenutzerMelodieIndex)) + ":" +
                pad(StaticVariable.minuten.get(lastBenutzerMelodieIndex)));
    }

    private static String pad(int c) {
        if (c >= 10)
            return String.valueOf(c);
        else
            return "0" + String.valueOf(c);
    }

    public static void clearLastMelodieButton() {
        if (lastBenutzerMelodieIndex < StaticVariable.benutzerMelodieTasteOn2.size()) {

            StaticVariable.benutzerMelodieTasteOn2.set(lastBenutzerMelodieIndex, false);
            //clrDateAndTime(lastBenutzerMelodieIndex);
            // DDP Code entfernt
        }
    }

    private void saveBenutzerTasten() {
        Log.e("SAVE", "BenutzerTasten");


        SharedPreferences pref = getSharedPreferences("Turmtechnik", 0);
        SharedPreferences.Editor editor = pref.edit();
        int zeilen = StaticVariable.benutzerMelodieTasteOn2.size();

        for (int i = 0; i < zeilen; i++) {
            editor.putBoolean("bmtOn" + i, (StaticVariable.benutzerMelodieTasteOn2.get(i)));
            editor.putInt("stunden" + i, (StaticVariable.stunden.get(i)));
            editor.putInt("minuten" + i, (StaticVariable.minuten.get(i)));
            editor.putInt("tage" + i, (StaticVariable.tage.get(i)));
            editor.putInt("monate" + i, (StaticVariable.monate.get(i)));
            editor.putInt("jahre" + i, (StaticVariable.jahre.get(i)));
        }

        editor.commit();
    }

//    @Override
//    public boolean onCreateOptionsMenu(Menu menu) {
//        getMenuInflater().inflate(R.menu.activity_turmtechnik, menu);
//        return true;
//    }


} // ende der Klasse
