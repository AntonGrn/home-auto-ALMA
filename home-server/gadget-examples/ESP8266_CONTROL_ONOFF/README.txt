A native CONTROL_ONOFF ALMA gadget to control ESP8266 GPIO.

HARDWARE REQUIREMENTS:
- ESP8266 WiFi microchip
 
SETUP:
1. In code: Assign values to ssid and password (for network connection).
2. Upload code ESP8266_CONTROL_ONOFF to ESP8266 WiFi microchip.
3. Run ESP8266 to capture its IP (for setup in 'gadgets.json)
4. In HomeServer:
   - Add gadget record in 'gadgets.json', under "alma".
   e.g:
   {
    "gadget_id": 5,
    "alias": "Kitchen lamp",
    "type": "CONTROL_ONOFF",
    "poll_delay_seconds": 30,
    "enabled": true,
    "IP_address": 192.168.121,
    "TCP_port": 8082,
    "request_spec": null
   }
