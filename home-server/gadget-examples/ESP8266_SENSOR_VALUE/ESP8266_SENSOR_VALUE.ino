#include <ESP8266WiFi.h>
#include <DHT.h>

// WiFi connection
const char* ssid     = "XXXXX";
const char* password = "XXXXX";

// TCP Server socket
const int port = 8082;
WiFiServer wifiServer(port);

// DHT temperature & humidity sensor
#define DHTTYPE DHT11
#define DHTPIN 2
DHT dht(DHTPIN, DHTTYPE);

void setup() {

  Serial.begin(115200);

  //Set up as station
  WiFi.mode(WIFI_STA);
  // Network device name (optional)
  WiFi.hostname("ALMA ESP gadget");
  // Initialize the WiFi instence
  WiFi.begin(ssid, password);

  //Connect to network
  while (WiFi.status() != WL_CONNECTED) {
    delay(1000);
    Serial.print("Connecting to WiFi: ");
    Serial.println(ssid);
  }
  Serial.println("WiFi connected");
  Serial.println("IP address: ");
  Serial.println(WiFi.localIP());

  // Initialize the socket server instance
  wifiServer.begin();

  // Initialize the DHT instance
  dht.begin();
}

void loop() {
  //Reonnect to network if connection lost.
  while (WiFi.status() != WL_CONNECTED) {
    Serial.println("Recnnecting to WiFi");
    delay(1000);
  }

  // Poll to check if socket client is available
  // Non-blocking.
  // Returns a WiFiClient object
  WiFiClient client = wifiServer.available();

  // If the returned WiFiClient is valid:
  if (client) {
    Serial.println("Valid client connected");
    // While the client is connected:
    while (client.connected()) {
      // While there are something to read from the client:
      while (client.available() > 0) { // Returns 0 until you read something
        Serial.println("Message from home server available:");
        String encryptedRequest = client.readStringUntil('\n');
        String serverRequest = encryptDecrypt(encryptedRequest);
        Serial.print("Command: ");
        Serial.println(serverRequest);

        // Process server message according to ALMA communication protocol
        if (serverRequest.startsWith("15")) {
          // 15: Poll request.
          // Derive request_spec from ALMA command, format: 15:request_spec
          int indexOfColon = serverRequest.indexOf(":");
          int fromIndex = indexOfColon + 1;
          String request_spec = serverRequest.substring(fromIndex);
          Serial.print("request_spec: ");
          Serial.println(request_spec);
          String requested_data = "0";
          if (request_spec.startsWith("temperature")) {
            requested_data = getTemperature();
          } else if (request_spec.startsWith("humidity")) {
            requested_data = getHumidity();
          }
          // Respond with requested data
          String response = "16:" + requested_data;
          client.println(encryptDecrypt(response));
          client.flush();
          Serial.print("Response sent to home server");
          Serial.println(response);
        }
      }
      delay(10);
    }
    client.stop();
  }
  delay(500);
}

String getTemperature() {
  float temperature = dht.readTemperature();
  String stringTemp = String(temperature);
  int indexOfDecimal = stringTemp.indexOf(".");
  Serial.print(stringTemp);
  Serial.println(" C");
  return stringTemp.substring(0, indexOfDecimal);
}

String getHumidity() {
  float humidity = dht.readHumidity();
  String stringHumid = String(humidity);
  int indexOfDecimal = stringHumid.indexOf(".");
  Serial.print(stringHumid);
  Serial.println(" %");
  return stringHumid.substring(0, indexOfDecimal);
}

String encryptDecrypt(String input) {
  char key[3] = {'F', 'K', 'Q'};
  String output = "";
  for(int i = 0 ; i < input.length() ; i++) {
    output += (char) (input.charAt(i) ^ key[i % (sizeof(key))]);
  }
  return output;
}
