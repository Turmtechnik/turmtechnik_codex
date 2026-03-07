package tom.turmtechnik;

import android.os.Environment;
import android.util.Log;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.NetworkInterface;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Enumeration;
import java.util.Locale;

public class LogTurmtechnik2 {

    /** Geräte-IP für Nebenuhr-Log (gesetzt von Activity oder hier ermittelt), Cache ~2 Min. */
    private static volatile String deviceIpForNebenuhrLog = null;
    private static volatile long deviceIpCacheMs = 0;
    private static final long DEVICE_IP_CACHE_MS = 120_000L;

    /** Setzt die IP fürs Nebenuhr-Log (z. B. aus TurmtechnikActivity beim Start). */
    public static void setDeviceIpForNebenuhrLog(String ip) {
        deviceIpForNebenuhrLog = (ip != null && !ip.trim().isEmpty()) ? ip.trim() : null;
        deviceIpCacheMs = System.currentTimeMillis();
    }

    /** Liefert die aktuelle Geräte-IP für Logzeilen (mit Cache), oder leer wenn unbekannt. */
    private static String getIpForNebenuhrLog() {
        long now = System.currentTimeMillis();
        if (deviceIpForNebenuhrLog != null && (now - deviceIpCacheMs) < DEVICE_IP_CACHE_MS) {
            return deviceIpForNebenuhrLog;
        }
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            String fallback = null;
            while (interfaces != null && interfaces.hasMoreElements()) {
                NetworkInterface ni = interfaces.nextElement();
                boolean up = false;
                try { up = ni.isUp(); } catch (Exception ignored) { }
                Enumeration<java.net.InetAddress> addresses = ni.getInetAddresses();
                while (addresses != null && addresses.hasMoreElements()) {
                    java.net.InetAddress address = addresses.nextElement();
                    if (!address.isLoopbackAddress() && address instanceof Inet4Address) {
                        String ip = address.getHostAddress();
                        if (fallback == null) fallback = ip;
                        if (up) {
                            deviceIpForNebenuhrLog = ip;
                            deviceIpCacheMs = now;
                            return ip;
                        }
                    }
                }
            }
            if (fallback != null) {
                deviceIpForNebenuhrLog = fallback;
                deviceIpCacheMs = now;
                return fallback;
            }
        } catch (Exception e) {
            Log.w("LogTurmtechnik2", "IP für Nebenuhr-Log: " + (e != null ? e.getMessage() : ""));
        }
        return (deviceIpForNebenuhrLog != null) ? deviceIpForNebenuhrLog : "";
    }

    /** Präfix für Nebenuhr-Logzeile: Zeit + optional IP. */
    private static String nebenuhrLogZeitIpPrefix() {
        String ip = getIpForNebenuhrLog();
        String zeit = new SimpleDateFormat("HH:mm:ss.SSS").format(Calendar.getInstance().getTime());
        return ip.isEmpty() ? (zeit + "  ") : (zeit + "  IP " + ip + "  ");
    }

    private String zeit;
    private String manualAutomatic;
    private String tagesDatum;

    private int relais1;
    private int relais2;
    private int relais3;
    private int relais4;

    private SimpleDateFormat formater;

    private Calendar calendar;

    // konstruktor
    public LogTurmtechnik2(String manualAutomatic, int relais1, int relais2, int relais3, int relais4) {

        if (!StaticVariable.logOnOff) {
            return;
        }

        this.manualAutomatic = manualAutomatic;
        this.relais1 = relais1;
        this.relais2 = relais2;
        this.relais3 = relais3;
        this.relais4 = relais4;

        calendar = Calendar.getInstance();
        formater = new SimpleDateFormat("HH:mm:ss");
        //datumPlusZeit = (formater.format(calendar.getTime())) ;  // 1.10.13 10:03
//		Date time = calendar.getTime() ;
        zeit = (formater.format(calendar.getTime()));

        formater = new SimpleDateFormat("dd-MM-yy");
        tagesDatum = (formater.format(calendar.getTime()));

        String verbindung;

        if (!(Serial_IoThread.getSerialIoStatus2())) {
            verbindung = "ERROR ";
        } else {
            verbindung = "O.K. ";
        }


        StringBuilder sb = new StringBuilder();

        sb.append(zeit);
        sb.append("  ");
        sb.append(manualAutomatic);
        sb.append(" Verbindung ");
        sb.append(verbindung);
        sb.append(" Uhr A soll=");
        sb.append(StaticVariable.uhrA_calendarZeit);
        sb.append(" Uhr A ist=");
        sb.append(StaticVariable.uhrA_angezeigteZeit);
        sb.append(" Relais gesendet ");

        String tempString;

        tempString = (Integer.toBinaryString(relais1 | 0x100));
        sb.append(tempString.substring(1));
        sb.append(" ");
        tempString = (Integer.toBinaryString(relais2 | 0x100));
        sb.append(tempString.substring(1));
        sb.append(" ");
        tempString = (Integer.toBinaryString(relais3 | 0x100));
        sb.append(tempString.substring(1));
        sb.append(" ");
        tempString = (Integer.toBinaryString(relais4 | 0x100));
        sb.append(tempString.substring(1));
        sb.append(" ");
        sb.append(StaticVariable.versionString);

        sb.append("\r\n");

        String logString = sb.toString();

        //Log.e("LogTurmtechnik" , "" + logString ) ;

        appendTurmtechnikLog(logString);
        // Nebenuhr: nur noch LogNebenuhrRelais (EIN/AUS bei logOnOff, „erhöht und gespeichert“ immer)
        logString = null;
        sb = null;
        calendar = null;
        formater = null;
    }


    private void appendTurmtechnikLog(String logString) {
        String fileName = Environment.getExternalStorageDirectory().toString() + "/Turmtechnik/LogTurmtechnik-" + tagesDatum + ".txt";

        File logFile = new File(fileName);
        try {
            File parent = logFile.getParentFile();
            if (parent != null && !parent.exists()) parent.mkdirs();
            if (!logFile.exists()) logFile.createNewFile();
            BufferedWriter buf = new BufferedWriter(new FileWriter(logFile, true));
            buf.append(logString);
            // buf.newLine();
            buf.close();
            buf = null;
            logFile = null;

        } catch (IOException e) {
            // was nun?
        }
    }

    /** Aktuelle Echtzeit in Minuten (12h, 0–719). Für Log-Soll: Anzeige soll nicht um eine Minute nachhinken. */
    private static int getCurrentCalendarMinuten12() {
        Calendar cal = Calendar.getInstance();
        return cal.get(Calendar.HOUR) * 60 + cal.get(Calendar.MINUTE);
    }

    /**
     * Formatiert Ist/Soll fürs Nebenuhr-Log: A/B/C = Minuten im 12h-Zyklus (0–719) als "12:00", "1:30" usw.; D = Phase 0–59 als "Phase nn".
     */
    private static String formatNebenuhrIstSoll(String uhrName, int value) {
        if ("D".equals(uhrName)) {
            return "Phase " + value;
        }
        int minuten12h = Math.max(0, Math.min(719, value));
        int stunden = minuten12h / 60;
        int minuten = minuten12h % 60;
        int h12 = (stunden == 0) ? 12 : stunden;
        return String.format(Locale.US, "%d:%02d", h12, minuten);
    }

    /**
     * Löscht Nebenuhr-Logdateien (LogNebenuhrRelais-dd-MM-yy.txt), die älter als 1 Monat sind.
     * Wird beim Neustart ausgeführt.
     */
    private static void deleteNebenuhrLogFilesOlderThanOneMonth() {
        try {
            File dir = new File(Environment.getExternalStorageDirectory().toString() + "/Turmtechnik");
            if (!dir.exists() || !dir.isDirectory()) return;
            File[] files = dir.listFiles();
            if (files == null) return;
            SimpleDateFormat dfDatei = new SimpleDateFormat("dd-MM-yy", Locale.US);
            Calendar calNow = Calendar.getInstance();
            calNow.add(Calendar.DAY_OF_YEAR, -31);
            long grenzeMs = calNow.getTimeInMillis();
            for (File f : files) {
                String name = f.getName();
                if (!name.startsWith("LogNebenuhrRelais-") || !name.endsWith(".txt")) continue;
                String dateStr = name.substring("LogNebenuhrRelais-".length(), name.length() - 4);
                try {
                    Date fileDate = dfDatei.parse(dateStr);
                    if (fileDate != null && fileDate.getTime() < grenzeMs) {
                        if (f.delete()) {
                            Log.d("LogTurmtechnik2", "Nebenuhr-Log gelöscht (älter als 1 Monat): " + name);
                        }
                    }
                } catch (ParseException e) {
                    // Dateiname nicht im Format dd-MM-yy, ignorieren
                }
            }
        } catch (Exception e) {
            Log.e("LogTurmtechnik2", "Nebenuhr-Log Aufräumen: " + e.getMessage());
        }
    }

    /**
     * Schreibt eine Zeile ins Nebenuhr-Log im einheitlichen Format.
     * Format: HH:mm:ss.SSS  UHR A  Ist: 1:30  Soll: 1:32  Letztes Relais: A  Impuls Fehler: Nein
     * Soll für A/B/C: max(übergebener Soll, aktuelle Systemminute), damit „eine Minute nirgends“ im Log verschwindet.
     */
    private static void appendNebenuhrRelaisLogZeile(String uhrName, int ist, int soll, boolean lastRelaisA, boolean impulsFehler) {
        Calendar cal = Calendar.getInstance();
        SimpleDateFormat dfDatei = new SimpleDateFormat("dd-MM-yy");
        String tagesDatum = dfDatei.format(cal.getTime());
        String letztesRelais = lastRelaisA ? "A" : "B";
        String fehler = impulsFehler ? "Ja" : "Nein";
        String istStr = formatNebenuhrIstSoll(uhrName, ist);
        // A/B/C: Soll im Log = effektiver Soll (wie in checkSollIstZeit), sonst hinkt Anzeige um 1 Min nach
        int sollForLog = ("A".equals(uhrName) || "B".equals(uhrName) || "C".equals(uhrName))
                ? Math.max(soll, getCurrentCalendarMinuten12()) : soll;
        String sollStr = formatNebenuhrIstSoll(uhrName, sollForLog);
        String line = nebenuhrLogZeitIpPrefix() + "UHR " + uhrName + "  Ist: " + istStr + "  Soll: " + sollStr + "  Letztes Relais: " + letztesRelais + "  Impuls Fehler: " + fehler + "\r\n";
        String fileName = Environment.getExternalStorageDirectory().toString() + "/Turmtechnik/LogNebenuhrRelais-" + tagesDatum + ".txt";
        File logFile = new File(fileName);
        try {
            File parent = logFile.getParentFile();
            if (parent != null && !parent.exists()) parent.mkdirs();
            if (!logFile.exists()) logFile.createNewFile();
            BufferedWriter buf = new BufferedWriter(new FileWriter(logFile, true));
            buf.append(line);
            buf.close();
        } catch (IOException e) {
            Log.e("LogTurmtechnik2", "LogNebenuhrRelais schreiben: " + e.getMessage());
        }
    }

    /**
     * Logfile Nebenuhr-Relais: EIN-/AUS-Schalten (nur bei logOnOff).
     * Format: UHR A  Ist: ??  Soll: ??  Letztes Relais: A/B  Impuls Fehler: Nein
     */
    public static void appendNebenuhrRelaisLog(String uhrName, int relaisNr, boolean ein, int soll, int ist, boolean lastRelaisA) {
        if (!StaticVariable.logOnOff) return;
        appendNebenuhrRelaisLogZeile(uhrName, ist, soll, lastRelaisA, false);
    }

    /**
     * Auf Soll begrenzt und gespeichert (Uhr war voraus). Format: UHR A  Ist: ??  Soll: ??  Letztes Relais: ??  Impuls Fehler: Nein
     */
    public static void appendNebenuhrRelaisLogAufSollBegrenzt(String uhrName, int ist, boolean lastRelaisA) {
        appendNebenuhrRelaisLogZeile(uhrName, ist, ist, lastRelaisA, false);
    }

    /**
     * Manuelle Zeitkorrektur gespeichert. Format: UHR A  Ist: ??  Soll: ??  Letztes Relais: ??  Impuls Fehler: Nein
     */
    public static void appendNebenuhrRelaisLogZeitKorrigiert(String uhrName, int ist, boolean lastRelaisA) {
        appendNebenuhrRelaisLogZeile(uhrName, ist, ist, lastRelaisA, false);
    }

    /**
     * Schreibt ins Nebenuhr-Log „Zeit gestellt“ (bzw. „Phase gestellt“ für UHR D), wenn im Web-UI eine Nebenuhrzeit eingestellt und gespeichert wird.
     * Gleiche Datei: LogNebenuhrRelais-&lt;dd-MM-yy&gt;.txt
     */
    public static void appendNebenuhrRelaisLogZeitGestellt(String uhrName, int value) {
        Calendar cal = Calendar.getInstance();
        SimpleDateFormat dfDatei = new SimpleDateFormat("dd-MM-yy");
        String tagesDatum = dfDatei.format(cal.getTime());
        String valueStr = formatNebenuhrIstSoll(uhrName, value);
        String event = "D".equals(uhrName) ? "Phase gestellt" : "Zeit gestellt";
        String line = nebenuhrLogZeitIpPrefix() + "UHR " + uhrName + "  " + event + "  " + valueStr + "  (Web-UI)\r\n";
        String fileName = Environment.getExternalStorageDirectory().toString() + "/Turmtechnik/LogNebenuhrRelais-" + tagesDatum + ".txt";
        File logFile = new File(fileName);
        try {
            File parent = logFile.getParentFile();
            if (parent != null && !parent.exists()) parent.mkdirs();
            if (!logFile.exists()) logFile.createNewFile();
            BufferedWriter buf = new BufferedWriter(new FileWriter(logFile, true));
            buf.append(line);
            buf.close();
        } catch (IOException e) {
            Log.e("LogTurmtechnik2", "LogNebenuhrRelais Zeit gestellt: " + e.getMessage());
        }
    }

    /**
     * Crash-/Vorfälle-Log: Neustarts, Beenden der App, Fehler/Exceptions – alles, was nicht normal ist.
     * Nur wenn Anlage „Logfile Fehler/Crashes“ (logFehlerCrashes) eingeschaltet ist.
     * Datei: Turmtechnik/LogCrash-&lt;dd-MM-yy&gt;.txt, Format: HH:mm:ss.SSS  &lt;Nachricht&gt;
     */
    public static void appendCrashLog(String message) {
        if (!StaticVariable.logFehlerCrashes || message == null) return;
        Calendar cal = Calendar.getInstance();
        SimpleDateFormat dfZeit = new SimpleDateFormat("HH:mm:ss.SSS");
        SimpleDateFormat dfDatei = new SimpleDateFormat("dd-MM-yy");
        String zeit = dfZeit.format(cal.getTime());
        String tagesDatum = dfDatei.format(cal.getTime());
        String line = zeit + "  " + message + "\r\n";
        String fileName = Environment.getExternalStorageDirectory().toString() + "/Turmtechnik/LogCrash-" + tagesDatum + ".txt";
        File logFile = new File(fileName);
        try {
            File parent = logFile.getParentFile();
            if (parent != null && !parent.exists()) parent.mkdirs();
            if (!logFile.exists()) logFile.createNewFile();
            BufferedWriter buf = new BufferedWriter(new FileWriter(logFile, true));
            buf.append(line);
            buf.close();
        } catch (IOException e) {
            Log.e("LogTurmtechnik2", "LogCrash schreiben: " + e.getMessage());
        }
    }

    /**
     * Schreibt eine Warnung ins Nebenuhr-Relais-Log (LogNebenuhrRelais-dd-MM-yy.txt).
     * Format: HH:mm:ss.SSS  IP x.x.x.x  UHR A  Warnung: &lt;text&gt;
     */
    public static void appendNebenuhrRelaisLogWarnung(String uhrName, String warnungText) {
        if (warnungText == null) return;
        Calendar cal = Calendar.getInstance();
        SimpleDateFormat dfDatei = new SimpleDateFormat("dd-MM-yy");
        String tagesDatum = dfDatei.format(cal.getTime());
        String line = nebenuhrLogZeitIpPrefix() + "UHR " + uhrName + "  Warnung: " + warnungText + "\r\n";
        String fileName = Environment.getExternalStorageDirectory().toString() + "/Turmtechnik/LogNebenuhrRelais-" + tagesDatum + ".txt";
        File logFile = new File(fileName);
        try {
            File parent = logFile.getParentFile();
            if (parent != null && !parent.exists()) parent.mkdirs();
            if (!logFile.exists()) logFile.createNewFile();
            BufferedWriter buf = new BufferedWriter(new FileWriter(logFile, true));
            buf.append(line);
            buf.close();
        } catch (IOException e) {
            Log.e("LogTurmtechnik2", "LogNebenuhrRelais Warnung schreiben: " + e.getMessage());
        }
    }

    /**
     * Schreibt ins Nebenuhr-Relais-Log einen Neustart-Eintrag (immer, unabhängig von logOnOff).
     * Format: HH:mm:ss.SSS  Neustart  App gestartet
     * Zusätzlich: Hinweis, dass bis zum nächsten Impuls eine Minute fehlen kann (im Log nach „Minute verloren“ suchen).
     */
    public static void appendNebenuhrRelaisLogNeustart() {
        deleteNebenuhrLogFilesOlderThanOneMonth();
        Calendar cal = Calendar.getInstance();
        SimpleDateFormat dfDatei = new SimpleDateFormat("dd-MM-yy");
        String tagesDatum = dfDatei.format(cal.getTime());
        String line = nebenuhrLogZeitIpPrefix() + "Neustart  App gestartet\r\n";
        String line2 = nebenuhrLogZeitIpPrefix() + "Hinweis: Bis zum nächsten Impuls kann eine Minute fehlen (Neustart)\r\n";
        String fileName = Environment.getExternalStorageDirectory().toString() + "/Turmtechnik/LogNebenuhrRelais-" + tagesDatum + ".txt";
        File logFile = new File(fileName);
        try {
            File parent = logFile.getParentFile();
            if (parent != null && !parent.exists()) parent.mkdirs();
            if (!logFile.exists()) logFile.createNewFile();
            BufferedWriter buf = new BufferedWriter(new FileWriter(logFile, true));
            buf.append(line);
            buf.append(line2);
            buf.close();
        } catch (IOException e) {
            Log.e("LogTurmtechnik2", "LogNebenuhrRelais schreiben: " + e.getMessage());
        }
    }

    /**
     * Schreibt ins Nebenuhr-Log eine explizite Zeile „Minute verloren“ (Soll-Zeit + Grund).
     * So ist im Logfile klar ersichtlich, wo Impulse/Minuten verloren gehen (z. B. Suche nach „Minute verloren“).
     * Nur für A/B/C (Nebenuhr Minuten); Soll in 12h-Minuten (0–719).
     */
    public static void appendNebenuhrRelaisLogMinuteVerloren(String uhrName, int soll12h, String grund) {
        if (!"A".equals(uhrName) && !"B".equals(uhrName) && !"C".equals(uhrName)) return;
        String sollStr = formatNebenuhrIstSoll(uhrName, Math.max(0, Math.min(719, soll12h)));
        String line = nebenuhrLogZeitIpPrefix() + "UHR " + uhrName + "  Minute verloren: Soll " + sollStr + " (" + grund + ")\r\n";
        Calendar cal = Calendar.getInstance();
        SimpleDateFormat dfDatei = new SimpleDateFormat("dd-MM-yy");
        String tagesDatum = dfDatei.format(cal.getTime());
        String fileName = Environment.getExternalStorageDirectory().toString() + "/Turmtechnik/LogNebenuhrRelais-" + tagesDatum + ".txt";
        File logFile = new File(fileName);
        try {
            File parent = logFile.getParentFile();
            if (parent != null && !parent.exists()) parent.mkdirs();
            if (!logFile.exists()) logFile.createNewFile();
            BufferedWriter buf = new BufferedWriter(new FileWriter(logFile, true));
            buf.append(line);
            buf.close();
        } catch (IOException e) {
            Log.e("LogTurmtechnik2", "LogNebenuhrRelais Minute verloren: " + e.getMessage());
        }
    }

    /**
     * Abbruch – Impuls unvollständig (Relais wird wiederholt). Format mit Impuls Fehler: Ja
     */
    public static void appendNebenuhrRelaisLogAbbruch(String uhrName, int relaisNr, int soll, int ist, boolean lastRelaisA) {
        appendNebenuhrRelaisLogZeile(uhrName, ist, soll, lastRelaisA, true);
    }

    /**
     * Wiederholung Impuls (nach Abbruch). Format mit Impuls Fehler: Ja
     */
    public static void appendNebenuhrRelaisLogWiederholung(String uhrName, int relaisNr, int soll, int ist, boolean lastRelaisA) {
        appendNebenuhrRelaisLogZeile(uhrName, ist, soll, lastRelaisA, true);
    }

    /**
     * Erhöht und gespeichert (nach Impuls). Format: UHR A  Ist: ??  Soll: ??  Letztes Relais: ??  Impuls Fehler: Nein
     */
    public static void appendNebenuhrRelaisLogErhoehtGespeichert(String uhrName, int soll, int ist, boolean lastRelaisA) {
        appendNebenuhrRelaisLogZeile(uhrName, ist, soll, lastRelaisA, false);
    }

    /**
     * Carambola-Fehler während Impuls – Impuls wird nicht gezählt. Format mit Impuls Fehler: Ja
     * Zusätzlich: „Minute verloren“ für Suche im Logfile.
     */
    public static void appendNebenuhrRelaisLogImpulsFehler(String uhrName, int ist, int soll, boolean lastRelaisA) {
        appendNebenuhrRelaisLogZeile(uhrName, ist, soll, lastRelaisA, true);
        appendNebenuhrRelaisLogMinuteVerloren(uhrName, soll, "Impuls-Fehler/Timeout");
    }

    /** Letzter Log-Zeitpunkt „Keine Verbindung“ pro Uhr (A=0, B=1, C=2, D=3), um das Log nicht zu fluten. */
    private static final long[] lastKeineVerbindungLogMs = new long[4];
    private static final long KEINE_VERBINDUNG_LOG_INTERVAL_MS = 60_000L;

    /**
     * Schreibt ins Nebenuhr-Log „Keine Verbindung – Impuls nicht gezählt“, wenn die Relais-Verbindung fehlt.
     * Maximal alle 60 Sekunden pro Uhr, gleiche Datei: LogNebenuhrRelais-&lt;dd-MM-yy&gt;.txt
     */
    public static void appendNebenuhrRelaisLogKeineVerbindung(String uhrName) {
        appendNebenuhrRelaisLogKeineVerbindung(uhrName, 0, 0, false);
    }

    /**
     * Wie oben, zusätzlich wird eine Zeile im Standardformat mit „Impuls Fehler: Ja“ geschrieben (ist/soll/lastRelaisA für Log).
     */
    public static void appendNebenuhrRelaisLogKeineVerbindung(String uhrName, int ist, int soll, boolean lastRelaisA) {
        int idx = "A".equals(uhrName) ? 0 : "B".equals(uhrName) ? 1 : "C".equals(uhrName) ? 2 : "D".equals(uhrName) ? 3 : -1;
        if (idx < 0) return;
        appendNebenuhrRelaisLogZeile(uhrName, ist, soll, lastRelaisA, true);
        appendNebenuhrRelaisLogMinuteVerloren(uhrName, soll, "Keine Verbindung");
        long now = System.currentTimeMillis();
        if (now - lastKeineVerbindungLogMs[idx] < KEINE_VERBINDUNG_LOG_INTERVAL_MS) return;
        lastKeineVerbindungLogMs[idx] = now;
        Calendar cal = Calendar.getInstance();
        SimpleDateFormat dfDatei = new SimpleDateFormat("dd-MM-yy");
        String tagesDatum = dfDatei.format(cal.getTime());
        String line = nebenuhrLogZeitIpPrefix() + "UHR " + uhrName + "  Keine Verbindung  Impuls nicht gezählt\r\n";
        String fileName = Environment.getExternalStorageDirectory().toString() + "/Turmtechnik/LogNebenuhrRelais-" + tagesDatum + ".txt";
        File logFile = new File(fileName);
        try {
            File parent = logFile.getParentFile();
            if (parent != null && !parent.exists()) parent.mkdirs();
            if (!logFile.exists()) logFile.createNewFile();
            BufferedWriter buf = new BufferedWriter(new FileWriter(logFile, true));
            buf.append(line);
            buf.close();
        } catch (IOException e) {
            Log.e("LogTurmtechnik2", "LogNebenuhrRelais Keine Verbindung: " + e.getMessage());
        }
    }

    /**
     * Schreibt Relais EIN/AUS ins Nebenuhr-Log (EIN A, AUS A, EIN B, AUS B …), um zu sehen, wo Impulse verloren gehen.
     * Gleiche Datei wie appendNebenuhrRelaisLogZeile: LogNebenuhrRelais-&lt;dd-MM-yy&gt;.txt
     */
    public static void appendNebenuhrRelaisLogRelaisEinAus(String uhrName, String relaisLetter, boolean ein) {
        Calendar cal = Calendar.getInstance();
        SimpleDateFormat dfDatei = new SimpleDateFormat("dd-MM-yy");
        String tagesDatum = dfDatei.format(cal.getTime());
        String einAus = ein ? "EIN" : "AUS";
        String line = nebenuhrLogZeitIpPrefix() + "UHR " + uhrName + "  " + einAus + " " + relaisLetter + "\r\n";
        String fileName = Environment.getExternalStorageDirectory().toString() + "/Turmtechnik/LogNebenuhrRelais-" + tagesDatum + ".txt";
        File logFile = new File(fileName);
        try {
            File parent = logFile.getParentFile();
            if (parent != null && !parent.exists()) parent.mkdirs();
            if (!logFile.exists()) logFile.createNewFile();
            BufferedWriter buf = new BufferedWriter(new FileWriter(logFile, true));
            buf.append(line);
            buf.close();
        } catch (IOException e) {
            Log.e("LogTurmtechnik2", "LogNebenuhrRelais EIN/AUS schreiben: " + e.getMessage());
        }
    }

    /**
     * Logfile Melodien: Start-Zeit und Melodiename beim Start, End-Zeit beim Beenden.
     * Nur wenn im Anlagenformular aktiviert (StaticVariable.logAbgelaufeneMelodien).
     * Datei: Turmtechnik/LogAbgelaufeneMelodien-&lt;dd-MM-yy&gt;.txt
     * Format: "Start Zeit: dd.MM.yyyy HH:mm:ss  Melodie: &lt;name&gt;" bzw. "End Zeit: ...  Melodie: ..."
     */

    /** Schreibt eine Zeile in LogAbgelaufeneMelodien (Start oder Ende). */
    private static void appendMelodieLogZeile(String prefix, String melodieName) {
        if (!StaticVariable.logAbgelaufeneMelodien) return;
        if (melodieName == null) melodieName = "";
        Calendar cal = Calendar.getInstance();
        SimpleDateFormat dfDatum = new SimpleDateFormat("dd.MM.yyyy");
        SimpleDateFormat dfZeit = new SimpleDateFormat("HH:mm:ss");
        SimpleDateFormat dfDatei = new SimpleDateFormat("dd-MM-yy");
        String datum = dfDatum.format(cal.getTime());
        String zeit = dfZeit.format(cal.getTime());
        String tagesDatum = dfDatei.format(cal.getTime());
        String line = prefix + ": " + datum + " " + zeit + "  Melodie: " + melodieName.trim() + "\r\n";
        // Gleicher Basis-Pfad wie Rest der App (Melodien, Config etc.), damit die Datei auffindbar ist
        String basePath = (TurmtechnikActivity.sdCardPath != null && !TurmtechnikActivity.sdCardPath.isEmpty())
                ? TurmtechnikActivity.sdCardPath
                : Environment.getExternalStorageDirectory().toString();
        String fileName = basePath + "/Turmtechnik/LogAbgelaufeneMelodien-" + tagesDatum + ".txt";
        File logFile = new File(fileName);
        try {
            File parent = logFile.getParentFile();
            if (parent != null && !parent.exists()) parent.mkdirs();
            if (!logFile.exists()) logFile.createNewFile();
            BufferedWriter buf = new BufferedWriter(new FileWriter(logFile, true));
            buf.append(line);
            buf.close();
        } catch (IOException e) {
            Log.e("LogTurmtechnik2", "LogAbgelaufeneMelodien schreiben fehlgeschlagen: " + fileName + " " + e.getMessage());
        }
    }

    /** Melodie-Start: Start Zeit + Melodie. */
    public static void appendMelodieStart(String melodieName) {
        appendMelodieLogZeile("Start Zeit", melodieName);
    }

    /** Melodie beendet: End Zeit + Melodie. */
    public static void appendAbgelaufeneMelodie(String melodieName) {
        appendMelodieLogZeile("End Zeit", melodieName);
    }
}
