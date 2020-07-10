
# HomeServer
The HomeServer instance is the key component of your ALMA home automation system. In fact, if you register your HomeServer instance to an active ALMA PublicServer, the HomeServer instance is all you need to start automating your home. With the ALMA AndroidClient you can also control and monitor your smart home remotely.

See [main page](link) for more information.

# Get started with your ALMA HomeServer

There are no hard-coding involved in setting up your ALMA HomeServer. However, there are some configurations needed.

Because of the services provided by [PublicServer](link), no port-forwarding is needed for remote access using the [AndroidClient](link).

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

## Step 2: Connection to PublicServer

Specify connection credentials in `config.json`. See [main page](link).
```yaml
{
  "hub_ID": Integer. Unique ID of your home server instance (hub),
  "hub_alias": String. Optional name of your home server,
  "hub_password": String. Password for public server authentication,
  "hub_debug_mode": Boolean. Trigger additional log generations,
  "public_server_IP": String. IP-address of public server,
  "public_server_port": Integer. TCP-port of public server
}
```

## Step 3:  Run HomeServer
Since the ALMA home server is intended to be run on a Raspberry Pi (acting as the hub of your smart home system), the HomeServer application should be configured to launch as a Linux daemon. One suggested approach is to set it up as a `systemd` background service on your Raspberry Pi. This setup allows easy managagement and debugging with real-time log tracking, while still leaving your Pi available to run other/additional services and operations of your liking in parrallel.  to serve additional purposes of your liking

# Get started with Gadgets and Automations
Once the HomeServer is running inside your LAN with a successful connection to a PublicServer instance, you can start introducing and configuring gadgets and automations via their respective json files (`gadgets.json` and `automations.json`).

See [main page](link) for information about gadgets and automations.

#### Example of `gadgets.json`

#### Example of `automations.json`

