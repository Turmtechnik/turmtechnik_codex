package tom.turmtechnik;

import android.content.Context;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

/**
 * Laufzeit-Repository für Programme auf Basis der Datenbank.
 * Excel bleibt ausschließlich für den Import erhalten.
 */
public class DatabaseProgrammRepository implements ProgrammRepository {
    private static final String TAG = "DatabaseProgrammRepo";

    private final Context context;
    private final String programmTyp;

    public DatabaseProgrammRepository(Context context, String programmTyp) {
        this.context = context != null ? context.getApplicationContext() : null;
        this.programmTyp = programmTyp != null ? programmTyp.trim() : "normal";
    }

    @Override
    public List<Programm> getAllProgramme(String requestedProgrammTyp, String tagtypName) {
        PlatinenDatabaseHelper dbHelper = getDbHelper();
        if (dbHelper == null) {
            return new ArrayList<Programm>();
        }

        if (tagtypName != null && !tagtypName.trim().isEmpty()) {
            PlatinenDatabaseHelper.Tagtyp tagtyp = dbHelper.getTagtypByName(tagtypName.trim());
            if (tagtyp == null) {
                return new ArrayList<Programm>();
            }
            if (!matchesProgrammTyp(tagtyp.programmTyp, requestedProgrammTyp)) {
                return new ArrayList<Programm>();
            }
            return dbHelper.getProgrammeByTagtyp(tagtyp.name);
        }

        List<Programm> programme = new ArrayList<Programm>();
        String effectiveProgrammTyp = getEffectiveProgrammTyp(requestedProgrammTyp);
        for (PlatinenDatabaseHelper.Tagtyp tagtyp : dbHelper.getTagtypenByProgrammTyp(effectiveProgrammTyp)) {
            programme.addAll(dbHelper.getProgrammeByTagtyp(tagtyp.name));
        }
        return programme;
    }

    @Override
    public Programm getProgrammById(int id) {
        List<Programm> programme = getAllProgramme(programmTyp, null);
        for (Programm programm : programme) {
            if (programm.getId() == id) {
                return programm;
            }
        }
        return null;
    }

    @Override
    public boolean saveProgramm(Programm programm) {
        if (programm == null) {
            return false;
        }
        PlatinenDatabaseHelper dbHelper = getDbHelper();
        if (dbHelper == null) {
            return false;
        }
        String tagtypName = resolveDefaultTagtypName(dbHelper);
        if (tagtypName == null) {
            Log.w(TAG, "saveProgramm ohne auflösbaren Tagtyp nicht möglich");
            return false;
        }
        return dbHelper.saveProgramm(tagtypName, programm);
    }

    @Override
    public boolean deleteProgramm(int id) {
        PlatinenDatabaseHelper dbHelper = getDbHelper();
        if (dbHelper == null) {
            return false;
        }
        for (PlatinenDatabaseHelper.Tagtyp tagtyp : dbHelper.getTagtypenByProgrammTyp(programmTyp)) {
            if (dbHelper.deleteProgramm(tagtyp.name, id)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public int getProgrammCount() {
        return getAllProgramme(programmTyp, null).size();
    }

    private PlatinenDatabaseHelper getDbHelper() {
        if (context == null) {
            Log.w(TAG, "Kein Context verfügbar, DB-Repository kann nicht verwendet werden");
            return null;
        }
        return PlatinenDatabaseHelper.getInstance(context);
    }

    private String getEffectiveProgrammTyp(String requestedProgrammTyp) {
        if (requestedProgrammTyp != null && !requestedProgrammTyp.trim().isEmpty()) {
            return requestedProgrammTyp.trim();
        }
        return programmTyp;
    }

    private boolean matchesProgrammTyp(String actualProgrammTyp, String requestedProgrammTyp) {
        String effectiveProgrammTyp = getEffectiveProgrammTyp(requestedProgrammTyp);
        return effectiveProgrammTyp.equals(actualProgrammTyp);
    }

    private String resolveDefaultTagtypName(PlatinenDatabaseHelper dbHelper) {
        List<PlatinenDatabaseHelper.Tagtyp> tagtypen = dbHelper.getTagtypenByProgrammTyp(programmTyp);
        if (tagtypen == null || tagtypen.isEmpty()) {
            if ("normal".equals(programmTyp)) {
                return "Normalprogramm";
            }
            return null;
        }
        return tagtypen.get(0).name;
    }
}
