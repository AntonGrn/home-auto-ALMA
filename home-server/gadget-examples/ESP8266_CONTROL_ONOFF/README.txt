A native CONTTROL_ONOFF ALMA gadget.
 
SETUP:
1. In code: Assign values to ssid and password (for network connection).
2. Upload code ESP8266_CONTROL_ONOFF to ESP8266 WiFi microchip.
3. In HomeServer:
   - Add json record for gadget in 'gadgets.json', under "alma".
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
