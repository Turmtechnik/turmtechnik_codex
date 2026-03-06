package tom.turmtechnik;

/**
 * Datenquelle für den Programm-Editor: entweder Excel-Datei oder Datenbank.
 * Ermöglicht den gleichen Editor-UI-Code für beide Quellen.
 */
public interface IEditorDataSource {

    /** Liest Zellenwert (Spalte wie TagesSuche.SPALTE_*, Zeile = 0-basiert bei DB, Excel-Zeilenindex bei Excel). */
    String getCellString(int spalte, int zeile);

    /** Schreibt Zellenwert. */
    void putCellString(int spalte, int zeile, String value);

    /** Anzahl nutzbarer Zeilen (bei DB = Programm-Anzahl, bei Excel = ausreichend groß). */
    int getRowCount();

    /** Speichern (Excel: Workbook speichern; DB: aktuelles Programm in DB schreiben). */
    void save() throws Exception;

    /** true = Datenbank, false = Excel. */
    boolean isDatabaseMode();

    /**
     * Kopiert die Zeile fromRow und gibt den Index der neuen Zeile zurück (nur bei DB sinnvoll; bei Excel wird 0 zurückgegeben).
     */
    int copyRow(int fromRow);
}
