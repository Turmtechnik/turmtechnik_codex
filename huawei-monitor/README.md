# Huawei Einspeisung

Eigenstaendige Android-App fuer die lokale Abfrage eines Huawei-Wechselrichters oder Smart Meters per Modbus TCP.

## Was die App kann

- Verbindet sich direkt zum Smart Dongle per `IP:Port`
- Liest ein frei konfigurierbares Holding-Register als `signed int32`
- Zeigt positiv als `Einspeisung`, negativ als `Netzbezug`
- Speichert Host, Port, Unit-ID, Register und Intervall lokal
- Kann Modbus-Geraete im aktuellen WLAN auf Port `502` suchen
- Hat einen Vollbildmodus fuer eine reine Leistungsanzeige
- Zeigt Smart-Meter-Livewerte fuer Spannung, Strom, Leistungsarten und Energiezaehler

## Standardwerte

- Port: `502`
- Unit-ID: `1`
- Register: `32278`
- Intervall: `5` Sekunden

`32278` ist als Standard fuer die aktive Leistung am Huawei-Power-Meter gesetzt. Wenn dein Huawei-Aufbau andere Register nutzt, kannst du die Adresse direkt in der App aendern.

## Alternative Presets

- `Preset Smart Meter`: Register `32278`
- `Preset Wechselrichter`: Register `32080`

Das Wechselrichter-Preset zeigt haeufig die aktuelle WR-Wirkleistung, nicht zwingend die reine Netzeinspeisung.

## Starten

1. Projekt in Android Studio oeffnen.
2. Das Modul `huawei-monitor` auswaehlen.
3. App auf Tablet oder Handy installieren.
4. IP des Smart Dongles eintragen und `Start` druecken.

## Komfortfunktionen

- `Dongle suchen` prueft das aktuelle WLAN auf erreichbare Modbus-TCP-Geraete und uebernimmt den ersten Treffer in das Host-Feld.
- `Vollbild` blendet die Einstellungen aus und zeigt nur die aktuelle Leistung gross an.

## Smart-Meter-Dashboard

Die App liest im Smart-Meter-Modus einen offiziellen Huawei-Registerblock und zeigt daraus:

- Phasenspannungen `L1-L3`
- Phasenstroeme `L1-L3`
- Wirkleistung
- Blindleistung
- Scheinleistung
- Leistungsfaktor `cos phi`
- Bezugszaehler
- Einspeisezaehler

## Hinweise

- Wenn Einspeisung und Bezug vertauscht angezeigt werden, `Vorzeichen umkehren` aktivieren.
- Die App nutzt absichtlich keinen Fremd-Modbus-Client, sondern einen einfachen eingebauten Modbus-TCP-Leser fuer Function Code `03`.
