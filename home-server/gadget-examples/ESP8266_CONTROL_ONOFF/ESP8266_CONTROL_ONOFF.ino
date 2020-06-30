#include <ESP8266WiFi.h

const short int BUILTIN_LED1 = 2; //GPIO2
const short int BUILTIN_LED2 = 16;//GPIO16
//The ESP8266 built in LEDs operate in inverted mode
const boolean ON = LOW;
const boolean OFF = HIGH;

// Current state of gadget (for reporting to HomeServer)
String currentState = "0";

// WiFi connection
const char* ssid     = "XXXXX";
const char* password = "XXXXX";

// TCP Server socket
const int port = 8082;
WiFiServer wifiServer(port);

void setup() {
  pinMode(BUILTIN_LED1, OUTPUT);
  pinMode(BUILTIN_LED2, OUTPUT);
  pinOFF();

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
}

void loop() {
  //Reonnect to network if connection lost.
  while (WiFi.status() != WL_CONNECTED) {
    Serial.println("Recnnecting to WiFi");
    pinOFF();
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
        String serverRequest = client.readStringUntil('\n');
        Serial.print("Command: ");
        Serial.println(serverRequest);

        // Process server message according to ALMA communication protocol
        if (serverRequest.startsWith("10") || serverRequest.startsWith("15")) {
          // 10: Request to alter state. 15: Poll request.
          if (serverRequest.startsWith("10")) {
            // Derive requested state from ALMA command, format: 10:state[DERIVE]:request_spec[IGNORE]
            int indexOfColon = serverRequest.indexOf(":");
            int fromIndex = indexOfColon + 1;
            int toIndex = fromIndex + 1;
            String requestedState = serverRequest.substring(fromIndex, toIndex);
            Serial.print("Requested state: ");
            Serial.println(requestedState);
            if (requestedState.equals("1")) {
              pinON();
            } else if (requestedState.equals("0")) {
              pinOFF();
            }
          }
          // Respond with current state
          String response = "16:" + currentState;
          client.println(response);
          client.flush();
        }
      }
      delay(10);

    }
    client.stop();
  }
  delay(500);
}

void pinON() {
  digitalWrite(BUILTIN_LED1, ON);
  digitalWrite(BUILTIN_LED2, ON);
  currentState = "1";
}

void pinOFF() {
  digitalWrite(BUILTIN_LED1, OFF);
  digitalWrite(BUILTIN_LED2, OFF);
  currentState = "0";
}
