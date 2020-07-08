Requirements: Python 3 installed on HomeServer device.

Python scripts required to operate the following ALMA gadget(s):
---------------------------------------------------------------
GADGET:          Plugin; plugin_id: "local_pi_gpio_onoff"
REQUIRED SCRIPT: gpio-local-onoff.py
STATUS:          Included by default. 
---------------------------------------------------------------
GADGET:          rf433MHz
REQUIRED SCRIPT: rpi-rf_send
STATUS:          Not included by default.
WEB SITE:        https://pypi.org/project/rpi-rf/
NOTE:            Only needed for 433MHz switch communication.
---------------------------------------------------------------
