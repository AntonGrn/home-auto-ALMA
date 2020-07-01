A native CONTROL_VALUE ALMA gadget to control a servo.

HARDWARE REQUIREMENTS:
- ESP8266 WiFi microchip
- SG90 Servo

SETUP:
1. In code: Assign values to ssid and password (for network connection).
2. Upload code ESP8266_CONTROL_VALUE to ESP8266 WiFi microchip.
3. Run ESP8266 to capture its IP-address (for gadget setup in 'gadgets.json')
4. In HomeServer:
   - Add gadget record in 'gadgets.json', under "alma".
   e.g:
   {
     "gadget_id": 10,
     "alias": "Servo",
     "type": "CONTROL_VALUE",
     "poll_delay_seconds": 120,
     "enabled": true,
     "IP_address": "192.168.0.18",
     "TCP_port": 8082,
     "request_spec": null
   }