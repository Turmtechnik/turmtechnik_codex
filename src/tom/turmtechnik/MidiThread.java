package tom.turmtechnik;

import android.util.Log;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Thread für die Abspielung von MIDI-Dateien mit Relais-Impulsen.
 * Parst MIDI-Dateien, lädt Noten-zu-Relais-Zuordnungen aus der Datenbank
 * und sendet Relais-Impulse entsprechend den Note-On-Events.
 */
public class MidiThread extends Thread {
    private static final String TAG = "MidiThread";
    private static boolean doRun = true;
    private static MidiThread currentInstance = null;
    
    private String midiFilePath;
    
    public MidiThread(String midiFilePath) {
        this.midiFilePath = midiFilePath;
        doRun = true;
        currentInstance = this;
        Log.d(TAG, "MidiThread erstellt für: " + midiFilePath);
    }
    
    public static void stopCurrent() {
        doRun = false;
        if (currentInstance != null) {
            currentInstance.interrupt();
        }
    }
    
    @Override
    public void run() {
        StaticVariable.melodieAktiv = true;
        Log.d(TAG, "MidiThread gestartet für: " + midiFilePath);
        
        try {
            // 1. MIDI-Datei parsen
            List<MidiParser.MidiNoteEvent> events;
            try {
                events = MidiParser.parseMidiFile(midiFilePath);
            } catch (IOException e) {
                Log.e(TAG, "Fehler beim Parsen der MIDI-Datei: " + midiFilePath, e);
                return;
            }
            
            if (events.isEmpty()) {
                Log.w(TAG, "Keine Note-On-Events in MIDI-Datei gefunden");
                return;
            }
            
            // 2. Konfiguration aus DB laden (Geschwindigkeit)
            String fileName = new java.io.File(midiFilePath).getName();
            PlatinenDatabaseHelper dbHelper = PlatinenDatabaseHelper.getInstance(TurmtechnikActivity.turmtechnikContext);
            PlatinenDatabaseHelper.MidiConfig config = dbHelper.getMidiConfig(fileName);
            
            double abspielgeschwindigkeit = 1.0;
            if (config != null) {
                abspielgeschwindigkeit = config.abspielgeschwindigkeit;
                if (!config.aktiv) {
                    Log.d(TAG, "MIDI-Datei ist inaktiv, überspringe Abspielung");
                    return;
                }
            } else {
                Log.d(TAG, "Keine Konfiguration für MIDI-Datei gefunden, verwende Standard-Geschwindigkeit 1.0");
            }
            
            Log.d(TAG, "Abspielgeschwindigkeit: " + abspielgeschwindigkeit + "x");
            
            // 3. Noten-zu-Relais-Mappings aus DB laden
            List<PlatinenDatabaseHelper.MidiNoteRelais> mappings = dbHelper.getAllMidiNoteRelais();
            Map<Integer, PlatinenDatabaseHelper.MidiNoteRelais> noteToRelaisMap = new HashMap<>();
            
            for (PlatinenDatabaseHelper.MidiNoteRelais mapping : mappings) {
                if (mapping.aktiv) {
                    noteToRelaisMap.put(mapping.note, mapping);
                }
            }
            
            Log.d(TAG, "Geladene Noten-Relais-Zuordnungen: " + noteToRelaisMap.size());
            
            if (noteToRelaisMap.isEmpty()) {
                Log.w(TAG, "Keine aktiven Noten-Relais-Zuordnungen gefunden, kann MIDI nicht abspielen");
                return;
            }
            
            // 4. Events abspielen
            long startTime = System.currentTimeMillis();
            long lastTimestampMs = 0;
            
            for (MidiParser.MidiNoteEvent event : events) {
                if (!doRun || isInterrupted()) {
                    Log.d(TAG, "MidiThread wurde gestoppt");
                    break;
                }
                
                // Timestamp mit Geschwindigkeit anpassen
                long adjustedTimestampMs = Math.round(event.timestampMs / abspielgeschwindigkeit);
                
                // Warte bis zum Timestamp
                long elapsedTime = System.currentTimeMillis() - startTime;
                long waitTime = adjustedTimestampMs - elapsedTime;
                
                if (waitTime > 0) {
                    try {
                        Thread.sleep(waitTime);
                    } catch (InterruptedException e) {
                        Log.d(TAG, "MidiThread wurde unterbrochen");
                        break;
                    }
                }
                
                // Prüfe, ob Zuordnung für diese Note existiert
                PlatinenDatabaseHelper.MidiNoteRelais mapping = noteToRelaisMap.get(event.note);
                if (mapping == null) {
                    Log.d(TAG, "Keine Zuordnung für Note " + event.note + ", überspringe");
                    continue;
                }
                
                int relaisNummer = mapping.relaisNummer;
                int impulsLaengeMs = mapping.impulsLaengeMs;
                
                // Prüfe, ob Relais-Nummer gültig ist
                if (relaisNummer < 1 || relaisNummer > Serial_IoThread.relaisNew.length) {
                    Log.w(TAG, "Ungültige Relais-Nummer: " + relaisNummer + " für Note " + event.note);
                    continue;
                }
                
                // Relais-Impuls senden
                Log.d(TAG, "Note " + event.note + " -> Relais " + relaisNummer + 
                      " für " + impulsLaengeMs + "ms");
                
                // Relais einschalten
                Serial_IoThread.relaisNew[relaisNummer - 1] = true;
                
                // In separatem Thread nach impulsLaengeMs wieder ausschalten
                final int finalRelaisNummer = relaisNummer;
                new Thread(() -> {
                    try {
                        Thread.sleep(impulsLaengeMs);
                        if (Serial_IoThread.relaisNew != null && 
                            finalRelaisNummer > 0 && 
                            finalRelaisNummer <= Serial_IoThread.relaisNew.length) {
                            Serial_IoThread.relaisNew[finalRelaisNummer - 1] = false;
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }).start();
            }
            
            Log.d(TAG, "MidiThread abgeschlossen");
            
        } catch (Exception e) {
            Log.e(TAG, "Fehler im MidiThread", e);
        } finally {
            StaticVariable.melodieAktiv = false;
            currentInstance = null;
        }
    }
}
