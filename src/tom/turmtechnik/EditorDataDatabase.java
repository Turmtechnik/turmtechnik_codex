package tom.turmtechnik;

import android.content.Context;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

/**
 * Datenquelle für den Programm-Editor: lädt und speichert Programme in der Datenbank.
 * Spalten-Indizes wie TagesSuche (SPALTE_A_STARTZEIT = 0, etc.).
 */
public class EditorDataDatabase implements IEditorDataSource {

    private static final String TAG = "EditorDataDatabase";

    private final Context context;
    private final String tagtypName;
    private final List<Programm> programmeList;

    public EditorDataDatabase(Context context, String tagtypName, List<Programm> programmeList) {
        this.context = context.getApplicationContext();
        this.tagtypName = tagtypName == null ? "Normalprogramm" : tagtypName.trim();
        this.programmeList = programmeList != null ? new ArrayList<>(programmeList) : new ArrayList<Programm>();
    }

    @Override
    public String getCellString(int spalte, int zeile) {
        if (zeile < 0 || zeile >= programmeList.size()) {
            return "";
        }
        Programm p = programmeList.get(zeile);
        return getCellFromProgramm(p, spalte);
    }

    private static String getCellFromProgramm(Programm p, int spalte) {
        switch (spalte) {
            case 0: return p.getStartzeit() != null ? p.getStartzeit() : "";
            case 1: return p.getFunktion() != null ? p.getFunktion() : "";
            case 2: return (p.getMelodieName() != null && !p.getMelodieName().isEmpty()) ? p.getMelodieName() : (p.getDauerHeizung() != null ? p.getDauerHeizung() : "");
            case 3: return p.getDauerHeizung() != null ? p.getDauerHeizung() : "";
            case 4: return p.isMontag() ? "x" : "";
            case 5: return p.isDienstag() ? "x" : "";
            case 6: return p.isMittwoch() ? "x" : "";
            case 7: return p.isDonnerstag() ? "x" : "";
            case 8: return p.isFreitag() ? "x" : "";
            case 9: return p.isSamstag() ? "x" : "";
            case 10: return p.isSonntag() ? "x" : "";
            case 11: return p.isImmer() ? "x" : "";
            case 12: return String.valueOf(p.getPeriodisch());
            case 13: return p.getStartDatum() != null ? p.getStartDatum() : "";
            case 14: return p.getEndeDatum() != null ? p.getEndeDatum() : "";
            case 15: return p.getVerknuepfteTaste() != null ? p.getVerknuepfteTaste() : "";
            case 16: return String.valueOf(p.getPrioritaet());
            default: return "";
        }
    }

    @Override
    public void putCellString(int spalte, int zeile, String value) {
        if (zeile < 0 || zeile >= programmeList.size()) {
            return;
        }
        Programm p = programmeList.get(zeile);
        setCellInProgramm(p, spalte, value);
    }

    private static void setCellInProgramm(Programm p, int spalte, String value) {
        if (value == null) value = "";
        String v = value.trim().toLowerCase();
        switch (spalte) {
            case 0: p.setStartzeit(value); break;
            case 1: p.setFunktion(value); break;
            case 2: p.setMelodieName(value); break;
            case 3: p.setDauerHeizung(value); break;
            case 4: p.setMontag("x".equals(v)); break;
            case 5: p.setDienstag("x".equals(v)); break;
            case 6: p.setMittwoch("x".equals(v)); break;
            case 7: p.setDonnerstag("x".equals(v)); break;
            case 8: p.setFreitag("x".equals(v)); break;
            case 9: p.setSamstag("x".equals(v)); break;
            case 10: p.setSonntag("x".equals(v)); break;
            case 11: p.setImmer("x".equals(v)); break;
            case 12:
                try { p.setPeriodisch(Integer.parseInt(value.trim())); } catch (NumberFormatException e) { p.setPeriodisch(0); }
                break;
            case 13: p.setStartDatum(value); break;
            case 14: p.setEndeDatum(value); break;
            case 15: p.setVerknuepfteTaste(value.isEmpty() ? null : value); break;
            case 16:
                try { p.setPrioritaet(Integer.parseInt(value.trim())); } catch (NumberFormatException e) { p.setPrioritaet(0); }
                break;
        }
    }

    @Override
    public int getRowCount() {
        return programmeList.size();
    }

    @Override
    public void save() throws Exception {
        PlatinenDatabaseHelper dbHelper = PlatinenDatabaseHelper.getInstance(context);
        for (Programm p : programmeList) {
            dbHelper.saveProgramm(tagtypName, p);
        }
        Log.d(TAG, "Gespeichert: " + programmeList.size() + " Programme für " + tagtypName);
    }

    /** Speichert nur die Zeile zeile in die DB (für schnelleres Speichern). */
    public void saveRow(int zeile) {
        if (zeile < 0 || zeile >= programmeList.size()) return;
        PlatinenDatabaseHelper dbHelper = PlatinenDatabaseHelper.getInstance(context);
        dbHelper.saveProgramm(tagtypName, programmeList.get(zeile));
    }

    @Override
    public boolean isDatabaseMode() {
        return true;
    }

    @Override
    public int copyRow(int fromRow) {
        if (fromRow < 0 || fromRow >= programmeList.size()) return fromRow;
        Programm source = programmeList.get(fromRow);
        Programm copy = new Programm();
        copy.setStartzeit(source.getStartzeit());
        copy.setFunktion(source.getFunktion());
        copy.setMelodieName(source.getMelodieName());
        copy.setDauerHeizung(source.getDauerHeizung());
        copy.setMontag(source.isMontag());
        copy.setDienstag(source.isDienstag());
        copy.setMittwoch(source.isMittwoch());
        copy.setDonnerstag(source.isDonnerstag());
        copy.setFreitag(source.isFreitag());
        copy.setSamstag(source.isSamstag());
        copy.setSonntag(source.isSonntag());
        copy.setImmer(source.isImmer());
        copy.setPeriodisch(source.getPeriodisch());
        copy.setStartDatum(source.getStartDatum());
        copy.setEndeDatum(source.getEndeDatum());
        copy.setVerknuepfteTaste(source.getVerknuepfteTaste());
        copy.setPrioritaet(source.getPrioritaet());
        programmeList.add(copy);
        PlatinenDatabaseHelper.getInstance(context).saveProgramm(tagtypName, copy);
        return programmeList.size() - 1;
    }

    public List<Programm> getProgrammeList() {
        return programmeList;
    }
}
