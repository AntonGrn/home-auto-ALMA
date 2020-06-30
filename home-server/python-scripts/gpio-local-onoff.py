#!/usr/bin/env python

# Script used by ALMA plugin gadget: 'local_pi_gpio_onoff'
# Execution call: python3 gpio-local-onoff.py [BCM pin] [ON/OFF]

# Import Pi GPIO library, to initialize the GPIO ports.
import RPi.GPIO as GPIO

# Allow arguments to be passed
import sys

# Assign variables
pin = int(sys.argv[1])
requestedState = sys.argv[2]

# Ignore warnings
GPIO.setwarnings(False)
# Use physical pin numbering
GPIO.setmode(GPIO.BCM)
# Set BCM-pin to output. Initial value=off
GPIO.setup(pin, GPIO.OUT, initial=GPIO.LOW)

# Set GPIO state (GPIO.HIGH/GPIO.LOW)
GPIO.output(pin, (requestedState.upper() == 'ON'))
