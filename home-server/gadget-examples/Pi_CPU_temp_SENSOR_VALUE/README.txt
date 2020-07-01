A native SENSOR_VALUE ALMA gadget.

Monitors the CPU temperature of a Raspberry Pi:
- Remote Pi (reachable by IP) OR local Pi (HomeServer hub).
- Note: for monitoring CPU temperature of local Pi (HomeServer hub),
consider instead using ALMA HomeServer plugin 'local_pi_cpu_temp',
which doesn't require setting up an additional Linux daemon service.

SETUP:
1. Run Pi_CPU_temp_SENSOR_VALUE as Linux daemon service on remote/local 
   Raspberry Pi.
2. In HomeServer:
   Add json record for gadget in 'gadgets.json', under "alma".
   e.g:
   {
    "gadget_id": 4,
    "alias": "System Pi CPU temp(C)",
    "type": "SENSOR_VALUE",
    "poll_delay_seconds": 30,
    "enabled": true,
    "IP_address": 192.168.120,
    "TCP_port": 8084,
    "request_spec": null
  }
