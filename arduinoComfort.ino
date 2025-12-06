#include <ESP8266WiFi.h>
#include <ESP8266HTTPClient.h>
#include <WiFiClientSecure.h>
#include <ArduinoJson.h>
#include <DHT.h>
#include <WiFiManager.h>      // <-- MEJORA 1: Para el portal cautivo
#include <LittleFS.h>       // <-- MEJORA 2: Para guardar datos offline

// --- CONFIGURACIÓN DE RED Y FIREBASE ---
// const char* WIFI_SSID = "4br"; // YA NO SE NECESITA
// const char* WIFI_PASSWORD = "12345678"; // YA NO SE NECESITA
const char* FIREBASE_HOST = "smarthomeapp-8e5ac-default-rtdb.firebaseio.com";
const char* FIREBASE_AUTH = "wE8DDsoJpgAVFR40hdfFIpC3SbUis2LhXyZNSbtm";

// --- DEFINICIÓN DE PINES ---
#define DHTPIN      D4
#define LDR_PIN     A0
#define RELE_PIN    D5 // Ventilador
#define LED_PIN     D1 // Luces
#define BUZZER_PIN  D2 // Alarma
#define LED_STATUS  2  // LED integrado en la placa para indicar estado

// --- OBJETOS ---
WiFiClientSecure client;
DHT dht(DHTPIN, DHT11);

// --- VARIABLES GLOBALES ---
unsigned long lastSendTime = 0;
unsigned long lastReadTime = 0;
const unsigned long sendInterval = 10000; // Enviar datos de sensores cada 10 segundos
const unsigned long readInterval = 2000; // Leer controles cada 2 segundos

// ==================== SETUP ====================
void setup() {
  Serial.begin(115200);
  Serial.println("\n========================================");
  Serial.println("        SISTEMA DE SENSORES Y CONTROL IoT         ");
  Serial.println("========================================");

  pinMode(LED_STATUS, OUTPUT);
  digitalWrite(LED_STATUS, HIGH); // Apagado por defecto (HIGH en LED integrado es OFF)

  if(!LittleFS.begin()){ // <-- MEJORA 2: Iniciar sistema de archivos
    Serial.println("Error al montar LittleFS");
    return;
  }

  dht.begin();
  pinMode(LDR_PIN, INPUT);
  pinMode(RELE_PIN, OUTPUT);
  pinMode(LED_PIN, OUTPUT);
  pinMode(BUZZER_PIN, OUTPUT);

  digitalWrite(RELE_PIN, LOW);
  digitalWrite(LED_PIN, LOW);
  digitalWrite(BUZZER_PIN, LOW);

  // --- MEJORA 1: WiFiManager --- 
  WiFiManager wm;
  // wm.resetSettings(); // Descomentar esta línea UNA VEZ para borrar credenciales guardadas
  Serial.println("Iniciando portal de configuración WiFi...");
  if (!wm.autoConnect("AutoComfort-Config")) {
    Serial.println("Fallo al conectar y se alcanzó el tiempo de espera. Reiniciando...");
    delay(3000);
    ESP.restart();
  }
  
  Serial.println("\nWiFi Conectado!");
  Serial.print("Dirección IP: ");
  Serial.println(WiFi.localIP());

  client.setInsecure();
  client.setBufferSizes(1024, 512);

  Serial.println("\n========================================");
  Serial.println("          SISTEMA LISTO");
  Serial.println("========================================\n");
}

// ==================== LOOP PRINCIPAL ====================
void loop() {
  unsigned long currentTime = millis();

  // MEJORA 3: Indicador visual de estado de conexión
  if (WiFi.status() != WL_CONNECTED) {
    digitalWrite(LED_STATUS, !digitalRead(LED_STATUS)); // Parpadeo rápido si no hay WiFi
    delay(250);
  } else {
    digitalWrite(LED_STATUS, HIGH); // LED apagado si hay WiFi
  }

  // Solo intentar enviar y recibir si hay conexión
  if (WiFi.status() == WL_CONNECTED) {
    procesarColaOffline(); // Primero, intentar enviar datos guardados

    if (currentTime - lastSendTime >= sendInterval) {
      enviarSensoresFirebase();
      lastSendTime = currentTime;
    }

    if (currentTime - lastReadTime >= readInterval) {
      leerControlesFirebase();
      lastReadTime = currentTime;
    }
  }

  delay(10);
}

// ==================== FUNCIONES FIREBASE Y OFFLINE ====================
void enviarSensoresFirebase() {
  float temperatura = dht.readTemperature();
  float humedad = dht.readHumidity();
  int luz = analogRead(LDR_PIN);

  if (isnan(temperatura) || isnan(humedad)) {
    Serial.println("Error al leer el sensor DHT11.");
    return;
  }

  StaticJsonBuffer<200> jsonBuffer;
  JsonObject& root = jsonBuffer.createObject();
  root["temperatura"] = temperatura;
  root["humedad"] = humedad;
  root["luz"] = luz;
  root["timestamp"] = millis(); // Añadir marca de tiempo es buena práctica

  String jsonData;
  root.printTo(jsonData);
  
  if (WiFi.status() != WL_CONNECTED) {
      Serial.println("Sin conexión. Guardando datos en la cola.");
      guardarEnCola(jsonData);
      return;
  }

  // Si hay conexión, enviar datos en tiempo real
  String url = "https://" + String(FIREBASE_HOST) + "/sensores.json";
  if (sendHttpRequest("PUT", url, jsonData)) {
    Serial.println("Datos de sensores enviados a Firebase (en tiempo real).");
  } else {
    Serial.println("Error al enviar datos. Guardando en cola.");
    guardarEnCola(jsonData);
  }
}

// MEJORA 2: Función para guardar datos si no hay conexión
void guardarEnCola(String data) {
  File file = LittleFS.open("/cola.txt", "a"); // 'a' para añadir al final
  if (!file) {
    Serial.println("Error al abrir el archivo de cola para escritura");
    return;
  }
  if (file.println(data)) {
    Serial.println("Dato guardado en la cola.");
  } else {
    Serial.println("Error al escribir en la cola.");
  }
  file.close();
}

// MEJORA 2: Función para procesar la cola de datos guardados
void procesarColaOffline() {
  if (LittleFS.exists("/cola.txt")) {
    Serial.println("Se encontró una cola de datos. Procesando...");
    File file = LittleFS.open("/cola.txt", "r");
    String url = "https://" + String(FIREBASE_HOST) + "/sensores_historial.json"; // Enviar a un nodo diferente

    while (file.available()) {
      String line = file.readStringUntil('\n');
      if (line.length() > 0) {
        if (!sendHttpRequest("POST", url, line)) {
          Serial.println("Fallo al enviar dato de la cola. Se reintentará más tarde.");
          file.close();
          return; // Salir para no perder datos si la conexión falla a mitad
        }
      }
    }
    file.close();
    LittleFS.remove("/cola.txt"); // Borrar el archivo solo si se envió todo con éxito
    Serial.println("Cola de datos procesada y enviada.");
  }
}

// --- El resto de funciones (leerControles, get/send HttpRequest) se mantienen igual ---

void leerControlesFirebase() {
  String url = "https://" + String(FIREBASE_HOST) + "/controls.json";
  String payload;

  if (getHttpRequest(url, payload)) {
    StaticJsonBuffer<200> jsonBuffer;
    JsonObject& root = jsonBuffer.parseObject(payload);

    if (!root.success()) {
      Serial.println("Error al parsear JSON de controles.");
      return;
    }

    bool fanOn = root["fan"];
    bool lightsOn = root["lights"];
    bool alarmOn = root["alarm"];

    digitalWrite(RELE_PIN, fanOn ? HIGH : LOW);
    digitalWrite(LED_PIN, lightsOn ? HIGH : LOW);
    digitalWrite(BUZZER_PIN, alarmOn ? HIGH : LOW);
    
    Serial.println("Controles actualizados desde Firebase.");
  }
}

bool getHttpRequest(String url, String& payload) {
    HTTPClient https;
    if (strlen(FIREBASE_AUTH) > 0) {
        url += "?auth=" + String(FIREBASE_AUTH);
    }

    if (https.begin(client, url)) {
        int httpCode = https.GET();
        if (httpCode == HTTP_CODE_OK) {
            payload = https.getString();
            https.end();
            return true;
        }
        https.end();
    }
    return false;
}

bool sendHttpRequest(String method, String url, String payload) {
  HTTPClient https;
  if (strlen(FIREBASE_AUTH) > 0) {
    url += "?auth=" + String(FIREBASE_AUTH);
  }

  if (https.begin(client, url)) {
    https.addHeader("Content-Type", "application/json");
    int httpCode = -1;
    if (method == "PUT") {
        httpCode = https.PUT(payload);
    } else if (method == "POST") {
        httpCode = https.POST(payload);
    }

    if (httpCode == HTTP_CODE_OK) {
        https.end();
        return true;
    }
    https.end();
  }
  return false;
}
