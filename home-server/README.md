
# HomeServer
The HomeServer instance is the key component of your ALMA home automation system. In fact, the HomeServer instance is all you need to start automating your home. By connecting your HomeServer instance to an active ALMA PublicServer, you can also control and monitor your smart home remotely using the ALMA AndroidClient.

See [main page](link) for more information.

# Get started with your ALMA HomeServer

There are no hard-coding involved in setting up your ALMA HomeServer. However, there are some configurations needed.

## Step 1: Required files

**Approach 1:** Complete files setup (ready to deploy)

Unzip [home-server-ALMA](link), containing:
* Required files and file structure.
* Executable JAR instance of the HomeServer application.

**Approach 2:** Manual files setup

1. Copy the [*json* folder](link) and the [*python-scripts* folder](link) into a directory of your choosing.
2. Create an executable JAR file from the HomeServer [java project](link).
3. (Optional) For daemon deployment; create a bash script that can be called to launch the executable JAR.
4. Place the JAR-file (and the optional bash script) in the same directory as the *json* folder and *python-scripts* folder. 

Required project structure:
```
/home-server-ALMA (name of your choosing)
├── HomeServer.jar
├── homeServer.sh (optional)
├── json
│   ├── automations.json
│   ├── config.json
│   └── gadgets.json
└── python-scripts
    ├── gpio-local-onoff.py (optional)
    └── rpi-rf_send (optional)
```

Example bash script:
```bash
#!/bin/bash

#Run the application
java -jar HomeServer.jar
```

## Step 2: Settings

Specify desired settings in `config.json`.
```yaml
{
  "debug_mode": Boolean. Trigger additional logging.
  "public_server_connection": Boolean. Enable/disable connection to public server.
  "hub_alias": String. Optional naming of your home server instance.
  "hub_ID": Integer. Unique ID of your home server instance. For public server connection.
  "hub_password": String. Password for remote authentication. For public server connection.
  "public_server_IP": String. IP-address of public server.
  "public_server_port": Integer. TCP-port of public server.
}
```
**Note:** With an established connection to PublicServer, no port-forwarding is needed for remote access from an AndroidClient.

## Step 3:  Run HomeServer
Since the ALMA home server is intended to be run on a Raspberry Pi (acting as the hub of your smart home system), the HomeServer application should be configured to launch as a Linux daemon. One suggested approach is to set it up as a `systemd` background service on your Raspberry Pi. This setup allows easy managagement and debugging with real-time log tracking, while still leaving your Pi available to run other/additional services and operations of your liking in parrallel.  to serve additional purposes of your liking

# Get started with Gadgets and Automations
Once the HomeServer is running inside your LAN, you can start introducing and configuring gadgets and automations via their respective json files (`gadgets.json` and `automations.json`).

See [main page](link) for information about gadgets and automations.

#### Example of `gadgets.json`
```yaml
{
  "alma": [
    {
      "gadget_id": 1,
      "alias": "TV Lamp",
      "type": "CONTROL_ONOFF",
      "poll_delay_seconds": 10,
      "enabled": false,
      "IP_address": "192.168.0.15",
      "TCP_port": 8082,
      "request_spec": null
    },
    {
      "gadget_id": 2,
      "alias": "Temperature (C)",
      "type": "SENSOR_VALUE",
      "poll_delay_seconds": 60,
      "enabled": true,
      "IP_address": "192.168.0.13",
      "TCP_port": 8082,
      "request_spec": "temperature"
    },
    {
      "gadget_id": 3,
      "alias": "Humidity (%)",
      "type": "SENSOR_VALUE",
      "poll_delay_seconds": 120,
      "enabled": true,
      "IP_address": "192.168.0.13",
      "TCP_port": 8082,
      "request_spec": "humidity"
    },
    {
      "gadget_id": 4,
      "alias": "Camera Servo",
      "type": "CONTROL_VALUE",
      "poll_delay_seconds": 120,
      "enabled": true,
      "IP_address": "192.168.0.23",
      "TCP_port": 8082,
      "request_spec": null
    },

  ],
  "tp_link": [
    {
      "gadget_id": 5,
      "alias": "Kitchen Lamp",
      "poll_delay_seconds": 30,
      "enabled": true,
      "IP_address": "192.168.0.12",
      "TCP_port": 9999,
      "model": "HS110"
    }
  ],
  "rf433MHz": [
    {
      "gadget_id": 6,
      "alias": "Bedroom Fan",
      "enabled": false,
      "gpio_BCM": 17,
      "protocol": 1,
      "pulse_length": 317,
      "code_ON": "5587221",
      "code_OFF": "5587220"
    }
  ],
  "plugins": [
    {
      "gadget_id": 7,
      "alias": "System Pi CPU temp (C)",
      "poll_delay_seconds": 15,
      "enabled": true,
      "plugin_id": "local_pi_cpu_temp"
    },
    {
      "gadget_id": 8,
      "alias": "Raspberry Pi LED",
      "enabled": true,
      "plugin_id": "local_pi_gpio_onoff",
      "gpio_BCM": 4,
    }
  ]
}

```

#### Example of `automations.json`

