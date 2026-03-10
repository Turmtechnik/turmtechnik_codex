/*
 * Turmtechnik 24-Kanal ESP8266
 * - 24 Relais über 3x 74HC595 (Shift-Register), TCP-Server Port 23, 8-Byte-Protokoll.
 * - Serial (115200): gleiches 8-Byte-Protokoll für Bluetooth/Adapter; bei 9600-BT: Serial.begin(9600) setzen.
 * - Befehl 97 ('a'): Abfrage, Antwort "OK 24relais" + MAC
 * - Befehl 65 ('A'): Schalten (Bytes 4,5,6 = 24 Relais), Antwort "impulsok"
 * - Befehl 98 ('b'): Lauflicht o.ä., Antwort "impulsok"
 * - WiFiManager: Hotspot "Turmtechnik-Config" (Passwort 12345678) nach 1 Min. ohne WLAN
 * - OTA: Hostname "turmtechnik-24kanal", Firmware-Update per Arduino IDE
 */

#include <ESP8266WiFi.h>
#include <WiFiManager.h>
#include <ArduinoOTA.h>

constexpr uint16_t serverPort = 23;

#define PIN_LED  2
#define OE_PIN   5
#define DATA    14
#define LATCH   12
#define CLOCK   13

WiFiServer server(serverPort);
WiFiClient client;

byte data[8];
int Datensumme, Pruf;
unsigned long Timer;
const unsigned long WIFI_LOST_MS = 60000;
const unsigned long PORTAL_TIMEOUT_S = 120;
unsigned long lastWifiOk = 0;
bool portalWasShown = false;
WiFiManager wm;
uint8_t currentRelayBytes[3] = {0, 0, 0};

void printHexByte(uint8_t value) {
  if (value < 0x10) Serial.print('0');
  Serial.print(value, HEX);
}

void logRelayState(const char* prefix, const uint8_t relayBytes[3]) {
  Serial.print(prefix);
  Serial.print(" DEC[");
  Serial.print((int) relayBytes[0]);
  Serial.print(' ');
  Serial.print((int) relayBytes[1]);
  Serial.print(' ');
  Serial.print((int) relayBytes[2]);
  Serial.print("] HEX[");
  printHexByte(relayBytes[0]);
  Serial.print(' ');
  printHexByte(relayBytes[1]);
  Serial.print(' ');
  printHexByte(relayBytes[2]);
  Serial.println(']');
}

void logRelayChannels(const char* prefix, const uint8_t relayBytes[3]) {
  Serial.print(prefix);
  Serial.print(" Kanaele[");
  bool any = false;
  for (int byteIndex = 0; byteIndex < 3; byteIndex++) {
    for (int bitIndex = 0; bitIndex < 8; bitIndex++) {
      if ((relayBytes[byteIndex] & (1 << bitIndex)) != 0) {
        if (any) Serial.print(' ');
        Serial.print(byteIndex * 8 + bitIndex + 1);
        any = true;
      }
    }
  }
  if (!any) Serial.print('-');
  Serial.println(']');
}

void logCommandFrame(const char* source) {
  Serial.print("[CMD ");
  Serial.print(source);
  Serial.print("] Bytes HEX[");
  for (int i = 0; i < 8; i++) {
    printHexByte(data[i]);
    if (i < 7) Serial.print(' ');
  }
  Serial.print("] DEC[");
  for (int i = 0; i < 8; i++) {
    Serial.print((int) data[i]);
    if (i < 7) Serial.print(' ');
  }
  Serial.println(']');
}

bool isAllZeroFrame() {
  for (int i = 0; i < 8; i++) {
    if (data[i] != 0) return false;
  }
  return true;
}

static uint8_t reverseByte(uint8_t b) {
  uint8_t r = 0;
  for (int i = 0; i < 8; i++) { r = (r << 1) | (b & 1); b >>= 1; }
  return r;
}

void setAllRelaysLow() {
  digitalWrite(LATCH, LOW);
  shiftOut(DATA, CLOCK, LSBFIRST, 0);
  shiftOut(DATA, CLOCK, LSBFIRST, 0);
  shiftOut(DATA, CLOCK, LSBFIRST, 0);
  digitalWrite(LATCH, HIGH);
  currentRelayBytes[0] = 0;
  currentRelayBytes[1] = 0;
  currentRelayBytes[2] = 0;
}

void schaltenFromBuffer(const char* source) {
  uint8_t b0 = data[3];
  uint8_t b1 = data[4];
  uint8_t b2 = data[5];
  uint8_t previousBytes[3] = { currentRelayBytes[0], currentRelayBytes[1], currentRelayBytes[2] };
  digitalWrite(PIN_LED, LOW);
  digitalWrite(LATCH, LOW);
  shiftOut(DATA, CLOCK, LSBFIRST, reverseByte(b0));
  shiftOut(DATA, CLOCK, LSBFIRST, reverseByte(b1));
  shiftOut(DATA, CLOCK, LSBFIRST, reverseByte(b2));
  digitalWrite(LATCH, HIGH);
  digitalWrite(PIN_LED, HIGH);
  currentRelayBytes[0] = b0;
  currentRelayBytes[1] = b1;
  currentRelayBytes[2] = b2;

  Serial.print("[Schalten ");
  Serial.print(source);
  Serial.println("] Relaiswechsel");
  logRelayState("  ALT ", previousBytes);
  logRelayChannels("  ALT ", previousBytes);
  logRelayState("  NEU ", currentRelayBytes);
  logRelayChannels("  NEU ", currentRelayBytes);
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
  wm.setConfigPortalTimeout(PORTAL_TIMEOUT_S);
  wm.setConnectTimeout(20);
  if (wm.autoConnect("Turmtechnik-Config", "12345678"))
    lastWifiOk = millis();
  server.begin();

  ArduinoOTA.setHostname("turmtechnik-24kanal");
  ArduinoOTA.onStart([]() { Serial.println("[OTA] Update startet..."); });
  ArduinoOTA.onEnd([]() { Serial.println("\n[OTA] Update fertig."); });
  ArduinoOTA.onProgress([](unsigned int progress, unsigned int total) {
    Serial.printf("[OTA] Fortschritt %u%%\r", (progress / (total / 100)));
  });
  ArduinoOTA.onError([](ota_error_t error) { Serial.printf("[OTA] Fehler %u\n", error); });
  ArduinoOTA.begin();
  Serial.println("[OTA] Bereit – Hostname: turmtechnik-24kanal");

  printWifiStatus();
  Serial.printf("24-Kanal Relais-Server Port %u bereit (TCP + Serial).\n", serverPort);
}

void processCommand(const char* source) {
  if (isAllZeroFrame()) return;

  logCommandFrame(source);
  Datensumme = (int)data[0] + (int)data[1] + (int)data[2] + (int)data[3] + (int)data[4] + (int)data[5];
  Pruf = (int)data[6] * 256 + (int)data[7];
  if (Datensumme != Pruf) {
    Serial.print("[CMD ");
    Serial.print(source);
    Serial.print("] Pruefsumme FEHLER erwartet=");
    Serial.print(Datensumme);
    Serial.print(" empfangen=");
    Serial.println(Pruf);
    return;
  }

  Serial.print("[CMD ");
  Serial.print(source);
  Serial.print("] Pruefsumme OK ");
  Serial.println(Pruf);

  Timer = 0;
  byte cmd = data[0];

  if (cmd != 97 && cmd != 65 && cmd != 98) {
    Serial.print("[CMD ");
    Serial.print(source);
    Serial.print("] Unbekannter Befehl ");
    Serial.println((int) cmd);
    for (int i = 0; i < 8; i++) data[i] = 0;
    Datensumme = 0;
    Pruf = 0;
    return;
  }

  if (cmd == 97) {
    uint8_t mac[6];
    WiFi.macAddress(mac);
    char macStr[18];
    snprintf(macStr, sizeof(macStr), "%02X:%02X:%02X:%02X:%02X:%02X", mac[0], mac[1], mac[2], mac[3], mac[4], mac[5]);
    if (client && client.connected()) {
      client.print("OK 24relais ");
      client.println(macStr);
      client.flush();
    }
    Serial.print("OK 24relais ");
    Serial.println(macStr);
  } else if (cmd == 65) {
    schaltenFromBuffer(source);
    if (client && client.connected()) {
      client.write("impulsok");
      client.flush();
    }
  } else if (cmd == 98) {
    if (client && client.connected()) {
      client.write("impulsok");
      client.flush();
    }
  }

  for (int i = 0; i < 8; i++) data[i] = 0;
  Datensumme = 0;
  Pruf = 0;
}

void loop() {
  ArduinoOTA.handle();

  if (client.connected() && client.available() >= 8) {
    for (int i = 0; i < 8; i++)
      data[i] = (byte) client.read();
    processCommand("TCP");
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
  /* Nach 5 s ohne Befehl Relais aus (länger als 1,5 s damit Test-Tastendruck sichtbar bleibt) */
  if (Timer > 5000) {
    setAllRelaysLow();
    Serial.println("AUS");
    Timer = 0;
  }

  /* Serial auf ESP8266: serialEvent() wird nicht aufgerufen, daher hier lesen */
  while (Serial.available()) {
    for (int i = 0; i < 7; i++) data[i] = data[i + 1];
    data[7] = (byte) Serial.read();
    processCommand("SER");
  }

  delay(1);
}
