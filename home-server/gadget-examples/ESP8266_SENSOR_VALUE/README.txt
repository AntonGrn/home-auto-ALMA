A native SENSOR_VALUE ALMA gadget to monitor ESP8266 sensor values.

NOTE
This gadget unit provides 2 gadget services:
- Temperature sensor.
- Humidity sensor.
Each gadget service is represented by its own gadget record in 
'gadgets.json', by specifying 'request_spec' (see example below).

HARDWARE REQUIREMENTS:
- ESP8266 WiFi microchip
- DHT11 Temperature & humidity sensor

SETUP:
1. In code: Assign values to ssid and password (for network connection).
2. Upload code ESP8266_SENSOR_VALUE to ESP8266 WiFi microchip.
3. Run ESP8266 to capture its IP-address (for gadget setup in 'gadgets.json')
4. In HomeServer:
   - Add gadget record(s) in 'gadgets.json', under "alma".
   e.g:
   {
     "gadget_id": 3,
     "alias": "Temperature (C)",
     "type": "SENSOR_VALUE",
     "poll_delay_seconds": 30,
     "enabled": true,
     "IP_address": "192.168.0.13",
     "TCP_port": 8082,
     "request_spec": "temperature"
   },
   {
     "gadget_id": 4,
     "alias": "Humidity (%)",
     "type": "SENSOR_VALUE",
     "poll_delay_seconds": 60,
     "enabled": true,
     "IP_address": "192.168.0.13",
     "TCP_port": 8082,
     "request_spec": "humidity"
   }
