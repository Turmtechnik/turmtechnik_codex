package tom.turmtechnik;

/**
 * Entity-Klasse für ein Programm-Eintrag.
 * Repräsentiert eine Zeile aus der Programm-Excel-Datei.
 */
public class Programm {
    private int id; // Excel-Zeilen-Index (0-basiert)
    private String startzeit; // "HH:MM:SS" oder "SA"/"SU"
    private String funktion; // "Melodie" oder "Heizung"
    private String melodieName; // Dateiname ohne .xls
    private String dauerHeizung; // "hh:mm:ss" Format
    private boolean montag;
    private boolean dienstag;
    private boolean mittwoch;
    private boolean donnerstag;
    private boolean freitag;
    private boolean samstag;
    private boolean sonntag;
    private boolean immer; // "1" = true
    private int periodisch; // 0=immer, 1=Sommer, 2=Winter
    private String startDatum; // "TT.MM" oder "TT.MM.JJJJ.A" oder "TT.T"
    private String endeDatum; // "TT.MM" oder "TT.MM.JJJJ.A" oder "TT.T"
    private String verknuepfteTaste; // Name der Taste oder null
    private int prioritaet; // 1-9
    private String programmTyp; // "normal", "festtag_variabel", "festtag_fest", "benutzer"
    private String festtagDatum; // Für Feiertage
    private Integer festtagJahr; // Für variable Feiertage

    // Konstruktoren
    public Programm() {
    }

    public Programm(int id) {
        this.id = id;
    }

    // Getter und Setter
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getStartzeit() {
        return startzeit;
    }

    public void setStartzeit(String startzeit) {
        this.startzeit = startzeit;
    }

    public String getFunktion() {
        return funktion;
    }

    public void setFunktion(String funktion) {
        this.funktion = funktion;
    }

    public String getMelodieName() {
        return melodieName;
    }

    public void setMelodieName(String melodieName) {
        this.melodieName = melodieName;
    }

    public String getDauerHeizung() {
        return dauerHeizung;
    }

    public void setDauerHeizung(String dauerHeizung) {
        this.dauerHeizung = dauerHeizung;
    }

    public boolean isMontag() {
        return montag;
    }

    public void setMontag(boolean montag) {
        this.montag = montag;
    }

    public boolean isDienstag() {
        return dienstag;
    }

    public void setDienstag(boolean dienstag) {
        this.dienstag = dienstag;
    }

    public boolean isMittwoch() {
        return mittwoch;
    }

    public void setMittwoch(boolean mittwoch) {
        this.mittwoch = mittwoch;
    }

    public boolean isDonnerstag() {
        return donnerstag;
    }

    public void setDonnerstag(boolean donnerstag) {
        this.donnerstag = donnerstag;
    }

    public boolean isFreitag() {
        return freitag;
    }

    public void setFreitag(boolean freitag) {
        this.freitag = freitag;
    }

    public boolean isSamstag() {
        return samstag;
    }

    public void setSamstag(boolean samstag) {
        this.samstag = samstag;
    }

    public boolean isSonntag() {
        return sonntag;
    }

    public void setSonntag(boolean sonntag) {
        this.sonntag = sonntag;
    }

    public boolean isImmer() {
        return immer;
    }

    public void setImmer(boolean immer) {
        this.immer = immer;
    }

    public int getPeriodisch() {
        return periodisch;
    }

    public void setPeriodisch(int periodisch) {
        this.periodisch = periodisch;
    }

    public String getStartDatum() {
        return startDatum;
    }

    public void setStartDatum(String startDatum) {
        this.startDatum = startDatum;
    }

    public String getEndeDatum() {
        return endeDatum;
    }

    public void setEndeDatum(String endeDatum) {
        this.endeDatum = endeDatum;
    }

    public String getVerknuepfteTaste() {
        return verknuepfteTaste;
    }

    public void setVerknuepfteTaste(String verknuepfteTaste) {
        this.verknuepfteTaste = verknuepfteTaste;
    }

    public int getPrioritaet() {
        return prioritaet;
    }

    public void setPrioritaet(int prioritaet) {
        this.prioritaet = prioritaet;
    }

    public String getProgrammTyp() {
        return programmTyp;
    }

    public void setProgrammTyp(String programmTyp) {
        this.programmTyp = programmTyp;
    }

    public String getFesttagDatum() {
        return festtagDatum;
    }

    public void setFesttagDatum(String festtagDatum) {
        this.festtagDatum = festtagDatum;
    }

    public Integer getFesttagJahr() {
        return festtagJahr;
    }

    public void setFesttagJahr(Integer festtagJahr) {
        this.festtagJahr = festtagJahr;
    }
}
