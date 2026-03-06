package tom.turmtechnik;

import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Parser für MIDI-Dateien.
 * Extrahiert Note-On-Events aus MIDI-Dateien und konvertiert sie in eine Liste von MidiNoteEvent.
 * 
 * Implementiert einen einfachen MIDI-Parser ohne javax.sound.midi (nicht verfügbar auf Android).
 */
public class MidiParser {
    private static final String TAG = "MidiParser";
    
    /**
     * Repräsentiert ein MIDI-Note-Event mit Note-Nummer und Timestamp.
     */
    public static class MidiNoteEvent {
        public int note;           // MIDI-Note (0-127)
        public long timestampMs;   // Zeitstempel in Millisekunden relativ zum Start
        
        public MidiNoteEvent(int note, long timestampMs) {
            this.note = note;
            this.timestampMs = timestampMs;
        }
    }
    
    /**
     * Parst eine MIDI-Datei und gibt eine Liste von Note-On-Events zurück.
     * 
     * @param midiFilePath Pfad zur MIDI-Datei
     * @return Liste von MidiNoteEvent (sortiert nach Timestamp)
     * @throws IOException wenn die Datei nicht gelesen werden kann
     */
    public static List<MidiNoteEvent> parseMidiFile(String midiFilePath) 
            throws IOException {
        List<MidiNoteEvent> events = new ArrayList<>();
        
        File midiFile = new File(midiFilePath);
        if (!midiFile.exists()) {
            throw new IOException("MIDI-Datei nicht gefunden: " + midiFilePath);
        }
        
        FileInputStream fis = new FileInputStream(midiFile);
        try {
            // Lese Header-Chunk
            byte[] header = new byte[14];
            int bytesRead = fis.read(header);
            if (bytesRead != 14) {
                throw new IOException("MIDI-Header zu kurz");
            }
            
            // Prüfe "MThd" Magic Number
            if (header[0] != 'M' || header[1] != 'T' || header[2] != 'h' || header[3] != 'd') {
                throw new IOException("Ungültige MIDI-Datei: Header nicht gefunden");
            }
            
            // Lese Format (2 Bytes, Big-Endian)
            int format = (header[4] & 0xFF) << 8 | (header[5] & 0xFF);
            
            // Lese Anzahl Tracks (2 Bytes, Big-Endian)
            int numTracks = (header[6] & 0xFF) << 8 | (header[7] & 0xFF);
            
            // Lese Resolution (2 Bytes, Big-Endian)
            // Wenn Bit 15 = 0: Ticks pro Viertelnote
            // Wenn Bit 15 = 1: Frames pro Sekunde (nicht unterstützt)
            int resolutionRaw = (header[10] & 0xFF) << 8 | (header[11] & 0xFF);
            int resolution;
            if ((resolutionRaw & 0x8000) != 0) {
                // Frames pro Sekunde - nicht unterstützt, verwende Standard
                Log.w(TAG, "MIDI-Datei verwendet Frames pro Sekunde, verwende Standard-Resolution");
                resolution = 480; // Standard: 480 Ticks pro Viertelnote
            } else {
                resolution = resolutionRaw;
            }
            
            Log.d(TAG, "MIDI-Format: " + format + ", Tracks: " + numTracks + ", Resolution: " + resolution);
            
            // Standard-Tempo: 500000 Mikrosekunden pro Viertelnote (120 BPM)
            long tempo = 500000;
            long currentTick = 0;
            
            // Parse alle Tracks
            for (int trackNum = 0; trackNum < numTracks; trackNum++) {
                // Lese Track-Header
                byte[] trackHeader = new byte[8];
                bytesRead = fis.read(trackHeader);
                if (bytesRead != 8) {
                    Log.w(TAG, "Track " + trackNum + ": Header zu kurz, überspringe");
                    break;
                }
                
                // Prüfe "MTrk" Magic Number
                if (trackHeader[0] != 'M' || trackHeader[1] != 'T' || 
                    trackHeader[2] != 'r' || trackHeader[3] != 'k') {
                    Log.w(TAG, "Track " + trackNum + ": Ungültiger Track-Header, überspringe");
                    // Versuche, Track-Länge zu lesen und zu überspringen
                    int trackLength = (trackHeader[4] & 0xFF) << 24 | 
                                     (trackHeader[5] & 0xFF) << 16 |
                                     (trackHeader[6] & 0xFF) << 8 |
                                     (trackHeader[7] & 0xFF);
                    fis.skip(trackLength);
                    continue;
                }
                
                // Lese Track-Länge (4 Bytes, Big-Endian)
                int trackLength = (trackHeader[4] & 0xFF) << 24 | 
                                 (trackHeader[5] & 0xFF) << 16 |
                                 (trackHeader[6] & 0xFF) << 8 |
                                 (trackHeader[7] & 0xFF);
                
                // Lese Track-Daten
                byte[] trackData = new byte[trackLength];
                bytesRead = fis.read(trackData);
                if (bytesRead != trackLength) {
                    Log.w(TAG, "Track " + trackNum + ": Daten unvollständig");
                    break;
                }
                
                // Parse Track-Events
                parseTrack(trackData, events, resolution, tempo);
            }
            
        } finally {
            fis.close();
        }
        
        // Sortiere Events nach Timestamp
        events.sort((e1, e2) -> Long.compare(e1.timestampMs, e2.timestampMs));
        
        Log.d(TAG, "MIDI-Datei geparst: " + events.size() + " Note-On-Events gefunden");
        
        return events;
    }
    
    /**
     * Parst einen Track und extrahiert Note-On-Events.
     */
    private static void parseTrack(byte[] trackData, List<MidiNoteEvent> events, 
                                   int resolution, long tempo) {
        int pos = 0;
        long currentTick = 0;
        long tempoUs = tempo; // Tempo in Mikrosekunden pro Viertelnote
        int runningStatus = 0; // Running Status für MIDI-Events
        
        while (pos < trackData.length) {
            // Lese Delta-Time (Variable-Length-Quantity)
            long deltaTime = readVariableLengthQuantity(trackData, pos);
            pos += getVariableLengthQuantityLength(trackData, pos);
            currentTick += deltaTime;
            
            if (pos >= trackData.length) {
                break;
            }
            
            // Lese Event-Type
            int eventByte = trackData[pos] & 0xFF;
            pos++;
            
            // Meta-Event (0xFF)
            if (eventByte == 0xFF) {
                if (pos >= trackData.length) break;
                int metaType = trackData[pos] & 0xFF;
                pos++;
                
                // Lese Meta-Event-Länge
                long metaLength = readVariableLengthQuantity(trackData, pos);
                pos += getVariableLengthQuantityLength(trackData, pos);
                
                // Tempo-Änderung (0x51)
                if (metaType == 0x51 && metaLength == 3 && pos + 3 <= trackData.length) {
                    tempoUs = ((trackData[pos] & 0xFF) << 16) | 
                             ((trackData[pos + 1] & 0xFF) << 8) | 
                             (trackData[pos + 2] & 0xFF);
                }
                
                // Überspringe Meta-Event-Daten
                pos += (int) metaLength;
                continue;
            }
            
            // System-Exclusive (0xF0-0xF7) - überspringe
            if (eventByte >= 0xF0 && eventByte <= 0xF7) {
                if (eventByte == 0xF0 || eventByte == 0xF7) {
                    // System-Exclusive mit variabler Länge
                    long sysexLength = readVariableLengthQuantity(trackData, pos);
                    pos += getVariableLengthQuantityLength(trackData, pos);
                    pos += (int) sysexLength;
                }
                continue;
            }
            
            // MIDI-Event
            int command;
            int channel;
            
            if ((eventByte & 0x80) != 0) {
                // Status-Byte vorhanden
                command = eventByte & 0xF0;
                channel = eventByte & 0x0F;
                runningStatus = eventByte;
            } else {
                // Running Status verwenden
                if (runningStatus == 0) {
                    Log.w(TAG, "Running Status nicht verfügbar, überspringe Event");
                    continue;
                }
                command = runningStatus & 0xF0;
                channel = runningStatus & 0x0F;
                pos--; // Gehe zurück, da dieses Byte die Daten sind
            }
            
            // Note-On-Event (0x90-0x9F)
            if (command == 0x90) {
                if (pos + 2 > trackData.length) {
                    Log.w(TAG, "Note-On-Event unvollständig");
                    break;
                }
                
                int note = trackData[pos] & 0xFF;
                int velocity = trackData[pos + 1] & 0xFF;
                pos += 2;
                
                // Ignoriere Note-On mit Velocity 0 (das ist eigentlich Note-Off)
                if (velocity > 0 && note >= 0 && note <= 127) {
                    // Konvertiere Tick zu Millisekunden
                    long timestampMs = tickToMilliseconds(currentTick, resolution, tempoUs);
                    events.add(new MidiNoteEvent(note, timestampMs));
                }
            } else if (command == 0x80) {
                // Note-Off-Event - überspringe Daten
                if (pos + 2 <= trackData.length) {
                    pos += 2;
                }
            } else if (command >= 0xA0 && command <= 0xEF) {
                // Andere MIDI-Events mit 2 Datenbytes - überspringe
                if (pos + 2 <= trackData.length) {
                    pos += 2;
                }
            } else if (command == 0xC0 || command == 0xD0) {
                // Program Change oder Channel Pressure - 1 Datenbyte
                if (pos + 1 <= trackData.length) {
                    pos += 1;
                }
            }
        }
    }
    
    /**
     * Liest eine Variable-Length-Quantity (VLQ) aus den Daten.
     */
    private static long readVariableLengthQuantity(byte[] data, int pos) {
        long value = 0;
        int shift = 0;
        
        while (pos < data.length) {
            int b = data[pos] & 0xFF;
            value |= (b & 0x7F) << shift;
            pos++;
            
            if ((b & 0x80) == 0) {
                break;
            }
            
            shift += 7;
            if (shift >= 35) {
                // Schutz vor zu langen VLQs
                break;
            }
        }
        
        return value;
    }
    
    /**
     * Gibt die Länge einer Variable-Length-Quantity zurück.
     */
    private static int getVariableLengthQuantityLength(byte[] data, int pos) {
        int length = 0;
        while (pos < data.length && length < 5) {
            int b = data[pos] & 0xFF;
            length++;
            pos++;
            if ((b & 0x80) == 0) {
                break;
            }
        }
        return length;
    }
    
    /**
     * Konvertiert MIDI-Ticks in Millisekunden.
     * 
     * @param tick Tick-Position
     * @param resolution Ticks pro Viertelnote
     * @param tempoUs Mikrosekunden pro Viertelnote
     * @return Millisekunden
     */
    private static long tickToMilliseconds(long tick, int resolution, long tempoUs) {
        // Formel: (tick / resolution) * (tempoUs / 1000)
        // tempoUs ist in Mikrosekunden, daher / 1000 für Millisekunden
        double quarterNotes = (double) tick / resolution;
        double milliseconds = quarterNotes * (tempoUs / 1000.0);
        return Math.round(milliseconds);
    }
}
