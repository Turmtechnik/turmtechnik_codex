/*
 * Turmtechnik 8-Kanal ESP8266
 * - 8 Relais direkt an GPIO, TCP-Server Port 23, 8-Byte-Protokoll.
 * - Befehl 97 ('a'): Antwort "OK 8relais"
 * - Befehl 65 ('A'): Schalten (Byte 6 = Relais 1–8), Antwort "OK 8relais" + "impulsok"
 * - Befehl 98 ('b'): Lauflicht, Antwort "impulsok"
 * - WiFiManager: nach 1 Min. ohne WLAN Hotspot "Turmtechnik-Config"
 * - OTA: Firmware-Update per Arduino IDE oder espota, Hostname "turmtechnik-8kanal"
 */

#include <ESP8266WiFi.h>
#include <WiFiManager.h>
#include <ArduinoOTA.h>

constexpr uint16_t serverPort = 23;

#define PIN_LED 2
#define PIN_RELAY1 16
#define PIN_RELAY2 14
#define PIN_RELAY3 12
#define PIN_RELAY4 13
#define PIN_RELAY5 15
#define PIN_RELAY6 0
#define PIN_RELAY7 4
#define PIN_RELAY8 5

WiFiServer server(serverPort);
WiFiClient client;

byte data[8];
int Datensumme, Pruf;
unsigned long Timer;
const unsigned long WIFI_LOST_MS = 60000;
const unsigned long PORTAL_TIMEOUT_S = 60;
unsigned long lastWifiOk = 0;
bool portalWasShown = false;
WiFiManager wm;

void setAllRelaysLow() {
  digitalWrite(PIN_RELAY1, LOW);
  digitalWrite(PIN_RELAY2, LOW);
  digitalWrite(PIN_RELAY3, LOW);
  digitalWrite(PIN_RELAY4, LOW);
  digitalWrite(PIN_RELAY5, LOW);
  digitalWrite(PIN_RELAY6, LOW);
  digitalWrite(PIN_RELAY7, LOW);
  digitalWrite(PIN_RELAY8, LOW);
}

void printWifiStatus() {
  if (WiFi.status() == WL_CONNECTED) {
    Serial.println("[WLAN] verbunden");
    Serial.print("[WLAN] IP: ");
    Serial.println(WiFi.localIP());
  } else {
    Serial.println("[WLAN] nicht verbunden");
  }
}

void setup() {
  Serial.begin(115200);
  pinMode(PIN_LED, OUTPUT);
  pinMode(PIN_RELAY1, OUTPUT);
  pinMode(PIN_RELAY2, OUTPUT);
  pinMode(PIN_RELAY3, OUTPUT);
  pinMode(PIN_RELAY4, OUTPUT);
  pinMode(PIN_RELAY5, OUTPUT);
  pinMode(PIN_RELAY6, OUTPUT);
  pinMode(PIN_RELAY7, OUTPUT);
  pinMode(PIN_RELAY8, OUTPUT);
  digitalWrite(PIN_LED, LOW);
  setAllRelaysLow();

  wm.setDebugOutput(false);
  wm.setConfigPortalTimeout(120);
  wm.setConnectTimeout(20);
  if (wm.autoConnect("Turmtechnik-Config", "12345678"))
    lastWifiOk = millis();
  server.begin();

  ArduinoOTA.setHostname("turmtechnik-8kanal");
  // Optional: Passwort für OTA (sonst jeder im WLAN kann flashen): ArduinoOTA.setPassword("dein-ota-passwort");
  ArduinoOTA.onStart([]() {
    Serial.println("[OTA] Update startet...");
  });
  ArduinoOTA.onEnd([]() {
    Serial.println("\n[OTA] Update fertig.");
  });
  ArduinoOTA.onProgress([](unsigned int progress, unsigned int total) {
    Serial.printf("[OTA] Fortschritt %u%%\r", (progress / (total / 100)));
  });
  ArduinoOTA.onError([](ota_error_t error) {
    Serial.printf("[OTA] Fehler %u\n", error);
  });
  ArduinoOTA.begin();
  Serial.println("[OTA] Bereit – in Arduino IDE: Werkzeuge → Port → turmtechnik-8kanal (oder IP flashen)");

  printWifiStatus();
  Serial.printf("8-Kanal Relais-Server Port %d bereit.\n", serverPort);
}

void loop() {
  ArduinoOTA.handle();

  // Daten zuerst verarbeiten: alle anstehenden Bytes lesen, dann Befehl auswerten
  if (client.connected() && client.available() > 0) {
    while (client.available() > 0) {
      for (int i = 0; i < 7; i++) data[i] = data[i + 1];
      data[7] = (byte) client.read();

      Datensumme = data[0] + data[1] + data[2] + data[3] + data[4] + data[5];
      Pruf = (int)data[6] * 256 + (int)data[7];
      if (Datensumme != Pruf) continue;

      digitalWrite(PIN_LED, HIGH);
      Timer = 0;
      byte cmd = data[0];

      if (cmd == 97) {
        uint8_t mac[6];
        WiFi.macAddress(mac);
        char macStr[18];
        snprintf(macStr, sizeof(macStr), "%02X:%02X:%02X:%02X:%02X:%02X", mac[0], mac[1], mac[2], mac[3], mac[4], mac[5]);
        client.print("OK 8relais ");
        client.println(macStr);
        client.flush();
        Serial.printf("OK 8relais %s\n", macStr);
      } else if (cmd == 65) {
        schalten();
        client.write("OK 8relais");
        client.write("impulsok");
        client.flush();
      } else if (cmd == 98) {
        lauflicht8();
        client.write("impulsok");
        client.flush();
        Serial.println("[Lauflicht] Ende");
      }

      for (int i = 0; i < 8; i++) data[i] = 0;
      Datensumme = 0;
      Pruf = 0;
    }
  }

  if (server.hasClient()) {
    if (client && client.connected())
      server.available().stop();
    else {
      client = server.available();
      digitalWrite(PIN_LED, LOW);
    }
  }

  static bool wifiWasConnected = false;
  if (WiFi.status() != WL_CONNECTED) {
    wifiWasConnected = false;
    unsigned long now = millis();
    if (lastWifiOk != 0 && (now - lastWifiOk) >= WIFI_LOST_MS && !portalWasShown) {
      portalWasShown = true;
      wm.setConfigPortalTimeout(PORTAL_TIMEOUT_S);
      wm.startConfigPortal("Turmtechnik-Config", "12345678");
      lastWifiOk = millis();
    }
  } else {
    if (!wifiWasConnected) {
      printWifiStatus();
      wifiWasConnected = true;
    }
    lastWifiOk = millis();
    portalWasShown = false;
  }

  Timer++;
  if (Timer > 10000) {
    setAllRelaysLow();
    Serial.println("Timer Überlauf – Relais aus");
    Timer = 0;
  }
  delay(1);
}

void schalten() {
  uint8_t r = data[5];  // Relais 1–8: Bit0 = Rel1 … Bit7 = Rel8
  digitalWrite(PIN_RELAY1, (r & (1 << 0)) ? HIGH : LOW);
  digitalWrite(PIN_RELAY2, (r & (1 << 1)) ? HIGH : LOW);
  digitalWrite(PIN_RELAY3, (r & (1 << 2)) ? HIGH : LOW);
  digitalWrite(PIN_RELAY4, (r & (1 << 3)) ? HIGH : LOW);
  digitalWrite(PIN_RELAY5, (r & (1 << 4)) ? HIGH : LOW);
  digitalWrite(PIN_RELAY6, (r & (1 << 5)) ? HIGH : LOW);
  digitalWrite(PIN_RELAY7, (r & (1 << 6)) ? HIGH : LOW);
  digitalWrite(PIN_RELAY8, (r & (1 << 7)) ? HIGH : LOW);
  Serial.print("[Relais] 1-8: 0b");
  for (int i = 7; i >= 0; i--) Serial.print((r >> i) & 1);
  Serial.println();
}

void lauflicht8() {
  const int pauseMs = 200;
  const int pins[] = { PIN_RELAY1, PIN_RELAY2, PIN_RELAY3, PIN_RELAY4, PIN_RELAY5, PIN_RELAY6, PIN_RELAY7, PIN_RELAY8 };
  Serial.println("[Lauflicht] Start 1x durch");
  for (int i = 0; i < 8; i++) {
    for (int j = 0; j < 8; j++) digitalWrite(pins[j], (j == i) ? HIGH : LOW);
    delay(pauseMs);
  }
  setAllRelaysLow();
}
