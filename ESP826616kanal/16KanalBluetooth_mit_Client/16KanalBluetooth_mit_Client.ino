/*
 * Turmtechnik 16-Kanal ESP8266
 * - 16 Relais über 2x 74HC595, TCP-Server Port 23, 8-Byte-Protokoll.
 * - Befehl 97 ('a'): Antwort "OK 16relais"
 * - Befehl 65 ('A'): Schalten, Antwort "impulsok"
 * - Befehl 98 ('b'): Lauflicht, Antwort "impulsok"
 * - WiFiManager: nach 1 Min. ohne WLAN Hotspot "Turmtechnik-Config"
 * - OTA: Firmware-Update per Arduino IDE oder espota, Hostname "turmtechnik-16kanal"
 */

#include <ESP8266WiFi.h>
#include <WiFiManager.h>
#include <ArduinoOTA.h>

constexpr uint16_t serverPort = 23;

#define PIN_LED 2
#define OE_PIN  5
#define DATA   14
#define LATCH  12
#define CLOCK  13

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
  digitalWrite(LATCH, LOW);
  shiftOut(DATA, CLOCK, LSBFIRST, 0);
  shiftOut(DATA, CLOCK, LSBFIRST, 0);
  digitalWrite(LATCH, HIGH);
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
  pinMode(OE_PIN, OUTPUT);
  pinMode(DATA, OUTPUT);
  pinMode(LATCH, OUTPUT);
  pinMode(CLOCK, OUTPUT);
  digitalWrite(PIN_LED, LOW);
  digitalWrite(OE_PIN, LOW);
  setAllRelaysLow();

  wm.setDebugOutput(false);
  wm.setConfigPortalTimeout(120);
  wm.setConnectTimeout(20);
  if (wm.autoConnect("Turmtechnik-Config", "12345678"))
    lastWifiOk = millis();
  server.begin();

  ArduinoOTA.setHostname("turmtechnik-16kanal");
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
  Serial.println("[OTA] Bereit – in Arduino IDE: Werkzeuge → Port → turmtechnik-16kanal (oder IP flashen)");

  printWifiStatus();
  Serial.printf("16-Kanal Relais-Server Port %d bereit.\n", serverPort);
}

void loop() {
  ArduinoOTA.handle();

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
        digitalWrite(PIN_LED, HIGH);
        uint8_t mac[6];
        WiFi.macAddress(mac);
        char macStr[18];
        snprintf(macStr, sizeof(macStr), "%02X:%02X:%02X:%02X:%02X:%02X", mac[0], mac[1], mac[2], mac[3], mac[4], mac[5]);
        client.print("OK 16relais ");
        client.println(macStr);
        client.flush();
        Serial.printf("OK 16relais %s\n", macStr);
      } else if (cmd == 65) {
        schalten();
        client.write("OK 16relais");
        client.write("impulsok");
        client.flush();
      } else if (cmd == 98) {
        client.write("impulsok");
        client.flush();
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
    Serial.println("AUS");
    Timer = 0;
  }
  delay(1);
}

static uint8_t reverseByte(uint8_t b) {
  uint8_t r = 0;
  for (int i = 0; i < 8; i++) { r = (r << 1) | (b & 1); b >>= 1; }
  return r;
}

void schalten() {
  uint16_t v = (uint16_t)data[5] | ((uint16_t)data[4] << 8);
  uint8_t byte1 = (uint8_t)(v & 0xFF);   // Relais 1–8 (Bit0=Rel1)
  uint8_t byte2 = (uint8_t)(v >> 8);     // Relais 9–16 (Bit0=Rel9)

  Serial.print("[Relais] 1-8:  0b");
  for (int i = 7; i >= 0; i--) Serial.print((byte1 >> i) & 1);
  Serial.print("  9-16: 0b");
  for (int i = 7; i >= 0; i--) Serial.print((byte2 >> i) & 1);
  Serial.println();

  digitalWrite(PIN_LED, LOW);
  digitalWrite(LATCH, LOW);
  shiftOut(DATA, CLOCK, LSBFIRST, reverseByte(byte2));
  shiftOut(DATA, CLOCK, LSBFIRST, reverseByte(byte1));
  digitalWrite(LATCH, HIGH);
  digitalWrite(PIN_LED, HIGH);
}


