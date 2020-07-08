
# About HomeServer
The HomeServer instance is the key component of your ALMA home automation system. In fact, if you register your HomeServer instance to an active ALMA PublicServer, the HomeServer instance is all you need to start automating you home. With the ALMA AndroidClient you can also control and monitor your smart home remotely.

See [main page](link) for more information.

# Get started with your ALMA HomeServer

There are no hard-coding involved in setting up your ALMA HomeServer. However, there are some configurations needed.

## Step 1: Required files

**Approach 1:** Unzip complete files setup

Unzip [compiled version (jar)](link) of HomeServer including all required configuration files.

**Approach 2:** Manual files setup

1. Copy the [*json* folder](link) and the [*python-scripts* folder](link) into a directory of your choosing.
2. Create a JAR-file from the HomeServer [java project](link).
3. Place the JAR-file in the same directory as the *json* folder and *python-scripts* folder. 

Required project structure:
```
/home-auto-ALMA (name of your choosing)
├── HomeServer.jar
├── json
│   ├── automations.json
│   ├── config.json
│   └── gadgets.json
└── python-scripts
    ├── config.json (optional)
    └── rpi-rf_send (optional)
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
Since the ALMA home server is intended to be run on a Raspberry Pi (acting as the hub of your smart home system), the HomeServer application should be configured to launch as a Linux daemon. One suggested approach is to set it up as a `systemd` background service on your Raspberry Pi. This setup allows easy managagement and debugging with real-time log tracking, while still leaving your Pi available to run other services and operations of your liking in parrallel.  to serve additional purposes of your liking

# Get started with Gadgets and Automations
Once the HomeServer is running inside your LAN with a successful connection to a PublicServer instance, you can start introducing and configuring gadgets and automations.

See [main page](link) for information about gadgets and automations.

## Native ALMA gadgets
Any software capable to setup a Socket TCP Server instance can confirm to native ALMA gadgets to gather integer value states.

## TP-Link gadgets

## RF 433 MHz gadgets

# Get started with Automations
See main page.
