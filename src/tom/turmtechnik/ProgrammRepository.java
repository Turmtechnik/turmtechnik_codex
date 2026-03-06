package tom.turmtechnik;

import java.util.List;

/**
 * Repository-Interface für Programm-Verwaltung.
 * Abstrahiert den Zugriff auf Programme (Excel oder Datenbank).
 */
public interface ProgrammRepository {
    /**
     * Gibt alle Programme zurück.
     * @param programmTyp "normal", "festtag_variabel", "festtag_fest", "benutzer" oder null für alle
     * @param tagtypName Optional: Name der Tagtyp-Datei (z.B. "Weihnachten.xls") für festtag_variabel/festtag_fest
     * @return Liste aller Programme
     */
    List<Programm> getAllProgramme(String programmTyp, String tagtypName);

    /**
     * Gibt ein Programm anhand der Zeilen-ID zurück.
     * @param id Excel-Zeilen-Index (0-basiert)
     * @return Programm oder null wenn nicht gefunden
     */
    Programm getProgrammById(int id);

    /**
     * Speichert ein Programm (erstellt neu oder aktualisiert).
     * @param programm Das zu speichernde Programm
     * @return true wenn erfolgreich
     */
    boolean saveProgramm(Programm programm);

    /**
     * Löscht ein Programm.
     * @param id Excel-Zeilen-Index (0-basiert)
     * @return true wenn erfolgreich
     */
    boolean deleteProgramm(int id);

    /**
     * Gibt die Anzahl der Programme zurück.
     * @return Anzahl der Programme
     */
    int getProgrammCount();
}
