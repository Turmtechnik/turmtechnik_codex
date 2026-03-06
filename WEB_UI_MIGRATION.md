# Web-UI Migration: Schrittweise Umstellung der Config-Punkte

## Strategie: Web-UI als Zwischenschritt

**Vorteile dieser Herangehensweise:**
- ✅ **Schrittweise Migration**: Jeder Config-Bereich einzeln umstellen
- ✅ **Sofort testbar**: Web-UI kann parallel zu Excel betrieben werden
- ✅ **Remote Control**: Web-UI kann von überall aus bedient werden
- ✅ **Einfache Wartung**: HTML/CSS/JS statt native Android-UI
- ✅ **Responsive Design**: Funktioniert auf Desktop, Tablet, Smartphone
- ✅ **Keine App-Updates nötig**: UI-Änderungen ohne App-Update

## Architektur

```
┌─────────────────┐
│   Web Browser   │  (Desktop, Tablet, Smartphone)
│   (HTML/JS)     │
└────────┬────────┘
         │ HTTP/REST API
         ▼
┌─────────────────┐
│  Android App    │
│  WebView/Server │  (Embedded WebView oder HTTP Server)
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│  Config Layer   │  (Neue Abstraktionsschicht)
│  (Repository)   │
└────────┬────────┘
         │
    ┌────┴────┐
    ▼        ▼
┌────────┐ ┌────────┐
│ Excel  │ │ SQLite │  (Parallel, dann Migration)
│ (Alt)  │ │ (Neu)  │
└────────┘ └────────┘
```

## Config-Bereiche (basierend auf Config-Klassen)

### 1. **Programm-Editor** (Priorität: HOCH)
**Aktuell:** `EditorActivity.java` + Excel-Dateien
**Web-UI:** Programm-Tabellen bearbeiten (Normalprogramm, Feiertage, Benutzerprogramme)

**Features:**
- Programmzeilen anzeigen/bearbeiten
- Startzeit, Funktion, Melodienname
- Wochentage (Mo-So) aktivieren/deaktivieren
- Priorität, Verknüpfte Taste, Start/Ende-Datum
- Sommer/Winter/Immer-Flags

### 2. **Zeitserver & GPS** (Priorität: MITTEL)
**Aktuell:** `TimeConfig.java` + `System.xls` Sheet
**Web-UI:** Zeitserver- und GPS-Konfiguration

**Features:**
- Zeitserver IP/Port konfigurieren
- GPS IP/Port konfigurieren
- Synchronisations-Intervalle
- Zeitserver ein/aus

### 3. **Nebenuhr-Konfiguration** (Priorität: MITTEL)
**Aktuell:** `SetNebenuhrActivity.java` + `Nebenuhr.xls`
**Web-UI:** Nebenuhr A, B, C konfigurieren

**Features:**
- Uhr A/B/C Zeiten setzen
- Kalenderzeit vs. Angezeigte Zeit
- Uhr-Status anzeigen

### 4. **Schlagwerk-Konfiguration** (Priorität: NIEDRIG)
**Aktuell:** `SchlagwerkConfig.java` + `Schlagwerkzeiten.xls`
**Web-UI:** Schlagwerk-Zeiten bearbeiten

**Features:**
- Schlagwerk-Zeiten hinzufügen/bearbeiten
- Schlagwerk ein/aus
- Beginn/Ende-Zeiten

### 5. **I/O-Konfiguration** (Priorität: MITTEL)
**Aktuell:** `IOConfig.java` + `System.xls` Sheet 14
**Web-UI:** Bluetooth, Serial, Carambola konfigurieren

**Features:**
- Bluetooth-Modus
- Serial-IO-Status
- Carambola-Status
- Internet-Zeitfenster

### 6. **System-Konfiguration** (Priorität: NIEDRIG)
**Aktuell:** `SystemConfig.java` + `System.xls` (Multi-Sheet)
**Web-UI:** System-Einstellungen

**Features:**
- Autostart ein/aus
- Serial Number
- Version
- Thread-Status

### 7. **Melodien-Verwaltung** (Priorität: HOCH)
**Aktuell:** Melodie-Dateien (`.xls`) im Dateisystem
**Web-UI:** Melodien anzeigen/bearbeiten

**Features:**
- Melodien-Liste anzeigen
- Melodie-Zeilen bearbeiten
- Vorlauf-Zeit setzen
- Kloeppel-Konfiguration

### 8. **Beschriftung Tasten** (Priorität: NIEDRIG)
**Aktuell:** `Beschriftung Tasten.xls`
**Web-UI:** Button-Beschriftungen und Relais-Zuordnung

**Features:**
- Button-Namen bearbeiten
- Relais-Nummern zuordnen
- Platinen-Nummern

## Implementierungs-Plan

### Phase 1: Web-Server in Android App (1-2 Wochen)

#### Option A: Embedded HTTP Server (Empfohlen)
```java
// NanoHTTPD oder ähnlich
public class ConfigWebServer extends NanoHTTPD {
    public ConfigWebServer(int port) {
        super(port);
    }
    
    @Override
    public Response serve(IHTTPSession session) {
        String uri = session.getUri();
        // REST API Endpoints
        if (uri.equals("/api/programme")) {
            return getProgramme();
        } else if (uri.equals("/api/programme/save")) {
            return saveProgramm(session);
        }
        // HTML-Seiten
        else if (uri.equals("/")) {
            return newFixedLengthResponse(Response.Status.OK, "text/html", getIndexHTML());
        }
    }
}
```

#### Option B: WebView mit lokalen HTML-Dateien
```java
// WebView in Activity
WebView webView = findViewById(R.id.webview);
webView.loadUrl("file:///android_asset/config/index.html");
webView.addJavascriptInterface(new ConfigJSInterface(), "Android");
```

**Empfehlung:** Option A (HTTP Server) für Remote-Zugriff

### Phase 2: REST API erstellen (1 Woche)

#### API-Endpunkte

**Programme:**
```
GET    /api/programme                    - Liste aller Programme
GET    /api/programme/:id                - Einzelnes Programm
POST   /api/programme                    - Neues Programm erstellen
PUT    /api/programme/:id                - Programm aktualisieren
DELETE /api/programme/:id                - Programm löschen
```

**Zeitserver:**
```
GET    /api/time/config                  - Zeitserver-Konfiguration
PUT    /api/time/config                  - Zeitserver-Konfiguration speichern
GET    /api/time/status                   - Zeitserver-Status
```

**Nebenuhr:**
```
GET    /api/nebenuhr/:uhr                - Nebenuhr-Konfiguration (A/B/C)
PUT    /api/nebenuhr/:uhr                - Nebenuhr-Konfiguration speichern
```

**Schlagwerk:**
```
GET    /api/schlagwerk                   - Schlagwerk-Zeiten
POST   /api/schlagwerk                   - Schlagwerk-Zeit hinzufügen
PUT    /api/schlagwerk/:id                - Schlagwerk-Zeit aktualisieren
DELETE /api/schlagwerk/:id                - Schlagwerk-Zeit löschen
```

**Melodien:**
```
GET    /api/melodien                     - Liste aller Melodien
GET    /api/melodien/:name                - Melodie-Details
GET    /api/melodien/:name/zeilen        - Melodie-Zeilen
PUT    /api/melodien/:name/zeilen/:index  - Melodie-Zeile aktualisieren
```

### Phase 3: Config-Repository-Layer (1 Woche)

**Abstraktionsschicht zwischen Web-UI und Excel/Datenbank:**

```java
public interface ProgrammRepository {
    List<Programm> getAllProgramme();
    Programm getProgrammById(int id);
    void saveProgramm(Programm programm);
    void deleteProgramm(int id);
}

// Implementierung 1: Excel (aktuell)
public class ExcelProgrammRepository implements ProgrammRepository {
    // Verwendet ExcelRead/ExcelWrite
}

// Implementierung 2: Datenbank (später)
public class DatabaseProgrammRepository implements ProgrammRepository {
    // Verwendet Room/SQLite
}

// Factory
public class RepositoryFactory {
    public static ProgrammRepository getProgrammRepository() {
        if (useDatabase) {
            return new DatabaseProgrammRepository();
        } else {
            return new ExcelProgrammRepository();
        }
    }
}
```

### Phase 4: Web-UI erstellen (2-3 Wochen)

#### Technologie-Stack
- **Frontend:** HTML5, CSS3, JavaScript (Vanilla oder Vue.js/React)
- **Styling:** Bootstrap oder Tailwind CSS
- **API:** REST API (JSON)

#### Beispiel: Programm-Editor

**HTML:**
```html
<!DOCTYPE html>
<html>
<head>
    <title>Turmtechnik - Programm Editor</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.1.3/dist/css/bootstrap.min.css" rel="stylesheet">
</head>
<body>
    <div class="container mt-4">
        <h1>Programm Editor</h1>
        <table class="table" id="programmeTable">
            <thead>
                <tr>
                    <th>Startzeit</th>
                    <th>Funktion</th>
                    <th>Melodie</th>
                    <th>Mo</th><th>Di</th><th>Mi</th><th>Do</th><th>Fr</th><th>Sa</th><th>So</th>
                    <th>Priorität</th>
                    <th>Aktionen</th>
                </tr>
            </thead>
            <tbody id="programmeBody">
                <!-- Wird per JavaScript gefüllt -->
            </tbody>
        </table>
        <button class="btn btn-primary" onclick="addProgramm()">Neues Programm</button>
    </div>
    <script src="programm-editor.js"></script>
</body>
</html>
```

**JavaScript:**
```javascript
// programm-editor.js
async function loadProgramme() {
    const response = await fetch('/api/programme');
    const programme = await response.json();
    
    const tbody = document.getElementById('programmeBody');
    tbody.innerHTML = '';
    
    programme.forEach(p => {
        const row = createProgrammRow(p);
        tbody.appendChild(row);
    });
}

async function saveProgramm(programm) {
    const response = await fetch('/api/programme/' + programm.id, {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(programm)
    });
    
    if (response.ok) {
        loadProgramme(); // Neu laden
    }
}
```

### Phase 5: Schrittweise Migration (4-6 Wochen)

#### Schritt 1: Programm-Editor (2 Wochen)
1. Web-UI für Programme erstellen
2. REST API implementieren
3. Excel-Repository erstellen
4. Testen: Web-UI → Excel
5. EditorActivity optional weiter nutzen (Fallback)

#### Schritt 2: Zeitserver & GPS (1 Woche)
1. Web-UI für Zeitserver-Konfiguration
2. REST API erweitern
3. Excel-Repository erweitern
4. Testen

#### Schritt 3: Nebenuhr (1 Woche)
1. Web-UI für Nebenuhr
2. REST API erweitern
3. Excel-Repository erweitern
4. SetNebenuhrActivity optional weiter nutzen

#### Schritt 4: Schlagwerk (1 Woche)
1. Web-UI für Schlagwerk
2. REST API erweitern
3. Excel-Repository erweitern

#### Schritt 5: Melodien (1 Woche)
1. Web-UI für Melodien-Verwaltung
2. REST API erweitern
3. Excel-Repository erweitern

### Phase 6: Datenbank-Migration (Parallel möglich)

**Während Web-UI läuft, kann parallel Datenbank vorbereitet werden:**

1. Datenbank-Schema erstellen
2. Database-Repository implementieren
3. Feature-Flag: `useDatabase = true/false`
4. Testen: Web-UI → Datenbank
5. Excel-Repository entfernen

## Technologie-Empfehlungen

### Backend (Android)
- **NanoHTTPD** - Leichter HTTP-Server (empfohlen)
- **Ktor** - Kotlin HTTP-Server (wenn Kotlin gewünscht)
- **Spring Boot** - Overkill für Android, aber möglich

### Frontend
- **Vanilla JavaScript** - Einfach, keine Dependencies
- **Vue.js** - Leichtgewichtig, einfach zu lernen
- **React** - Wenn Team React kennt

### Styling
- **Bootstrap 5** - Schnell, responsive
- **Tailwind CSS** - Modern, flexibel
- **Material Design** - Wenn Android-Look gewünscht

## Sicherheit

### Authentifizierung
```java
// Einfache Token-basierte Auth
public class AuthMiddleware {
    private static final String SECRET_TOKEN = "dein-geheimer-token";
    
    public boolean isValid(String token) {
        return SECRET_TOKEN.equals(token);
    }
}
```

### HTTPS (für Remote-Zugriff)
- Let's Encrypt Zertifikat
- Oder: Self-signed Zertifikat (nur für lokales Netzwerk)

## Vorteile dieser Strategie

✅ **Schrittweise Migration**: Jeder Bereich einzeln
✅ **Keine Breaking Changes**: Excel funktioniert weiterhin
✅ **Sofort testbar**: Web-UI parallel zu Excel
✅ **Remote Control**: Von überall aus bedienbar
✅ **Einfache Wartung**: HTML/CSS/JS statt native UI
✅ **Responsive**: Funktioniert auf allen Geräten
✅ **Keine App-Updates**: UI-Änderungen ohne Update
✅ **Vorbereitung für Datenbank**: Repository-Pattern bereits vorhanden

## Nachteile / Herausforderungen

⚠️ **Web-Server in App**: Zusätzlicher Code
⚠️ **Performance**: HTTP-Overhead (minimal)
⚠️ **Offline**: Web-UI braucht Verbindung (lokal OK)
⚠️ **Sicherheit**: Authentifizierung nötig

## Beispiel-Implementierung

### 1. Web-Server starten
```java
// In TurmtechnikActivity.onCreate()
public class TurmtechnikActivity extends Activity {
    private ConfigWebServer webServer;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Web-Server starten
        webServer = new ConfigWebServer(8080);
        try {
            webServer.start();
            Log.i("WebServer", "Gestartet auf Port 8080");
        } catch (IOException e) {
            Log.e("WebServer", "Fehler beim Starten", e);
        }
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (webServer != null) {
            webServer.stop();
        }
    }
}
```

### 2. REST API Endpoint
```java
// In ConfigWebServer
@Override
public Response serve(IHTTPSession session) {
    String uri = session.getUri();
    String method = session.getMethod().name();
    
    if (uri.equals("/api/programme") && method.equals("GET")) {
        ProgrammRepository repo = RepositoryFactory.getProgrammRepository();
        List<Programm> programme = repo.getAllProgramme();
        
        Gson gson = new Gson();
        String json = gson.toJson(programme);
        
        return newFixedLengthResponse(Response.Status.OK, 
            "application/json", json);
    }
    
    // ... weitere Endpoints
}
```

### 3. Web-UI Zugriff
- **Lokal:** `http://localhost:8080` (von Android-Gerät)
- **Remote:** `http://<IP-Adresse>:8080` (vom PC/Tablet)

## Zeitplan

| Phase | Dauer | Beschreibung |
|-------|-------|--------------|
| Phase 1 | 1-2 Wochen | Web-Server in App |
| Phase 2 | 1 Woche | REST API erstellen |
| Phase 3 | 1 Woche | Repository-Layer |
| Phase 4 | 2-3 Wochen | Web-UI erstellen |
| Phase 5 | 4-6 Wochen | Schrittweise Migration |
| Phase 6 | 2-3 Wochen | Datenbank-Migration (parallel) |
| **Gesamt** | **11-16 Wochen** | Vollständige Migration |

## Empfehlung

**Start mit Programm-Editor:**
1. Wichtigster Config-Bereich
2. Meist genutzt
3. Gute Testbarkeit
4. Sofortiger Nutzen

**Dann schrittweise weitere Bereiche:**
- Zeitserver & GPS
- Nebenuhr
- Schlagwerk
- Melodien
- System

**Parallel Datenbank vorbereiten:**
- Während Web-UI läuft
- Repository-Pattern erleichtert Migration
- Feature-Flag für einfaches Umschalten

## Nächste Schritte

1. ✅ Web-Server in App integrieren (NanoHTTPD)
2. ✅ REST API für Programme erstellen
3. ✅ Excel-Repository implementieren
4. ✅ Web-UI für Programm-Editor erstellen
5. ✅ Testen: Web-UI → Excel
6. ✅ Weitere Config-Bereiche hinzufügen
7. ✅ Datenbank-Repository parallel entwickeln
8. ✅ Migration: Excel → Datenbank
