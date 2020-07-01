
/**
 * A native CONTROL_VALUE ALMA gadget to control a servo.
 * 
 * Servo: SG90
 * 
 * Servo setup on ESP8266:
 * Brown pin: GND
 * Red pin: VCC (3.3v-6v). ESP8266:3.3v
 * Yellow pin : Sig. ESP8266:D1
 * 
 */
 #include <ESP8266WiFi.h>
 #include <Servo.h>

// WiFi connection
const char* ssid     = "XXXXX";
const char* password = "XXXXX";

// TCP Server socket
const int port = 8082;
WiFiServer wifiServer(port);

// Instantiate servo instance
Servo servo;
const short int SERVO_PIN = 5; //D1
int currentPos;

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

  // Initialize the servo instance
  servo.attach(SERVO_PIN);
  movePointer(0);
  delay(1000);
}

void loop() {
  //Reonnect to network if connection lost.
  while (WiFi.status() != WL_CONNECTED) {
    Serial.println("Recnnecting to WiFi");
    movePointer(0);
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
            String temp = serverRequest.substring(fromIndex);
            indexOfColon = temp.indexOf(":");
            int requestedState = (temp.substring(0, indexOfColon)).toInt();
            // Move pointer to requested position
            movePointer(requestedState);
          }
          // Respond with current state
          String response = "16:" + String(currentPos);
          client.println(response);
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

void movePointer(int requestedPos) {
  // Make sure requested position is within 0-170 (test servo struggled 170-180).
  requestedPos = (requestedPos < 0 ? 0 : (requestedPos > 170 ? 170 : requestedPos));
  // Move the servo pointer.
  servo.write(requestedPos);
  currentPos = requestedPos;
}
