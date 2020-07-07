
# ALMA HomeServer
The HomeServer instance is the key component of your ALMA home automation system. In fact, if you register your homeServer instance to an active ALMA PublicServer (to obtain remote login credentials), the HomeServer instance is all you need to start automating you home. With the ALMA AndroidClient you can also control and monitor your smart home remotely.

Additional information about the HomeServer and other ALMA nodes can be found on the repository main page.

# Get started with your ALMA HomeServer

There are no hard-coding requirements involved in setting up your ALMA HomeServer. However, there are some configurations needed.

## Step 1: Setup required files

**Approach 1:** Download complete packet

For easy, plug-and-play setup, download compiled version of HomeServer including all required configuration files.

**Approach 2:** Manually

1. Create required JSON files named `config.json`, `gadgets.json` and `automations.json`
   - Read about the JSON files on the repository main page.
   - See full/extended examples of the required JSON files below.
2. Create a JAR-file from the HomeServer java project.
3. Place the JAR-file in the same directory as the HomeServer JSON-files created above. Required structure:
```
/home-auto-ALMA (name of your choosing)
├── HomeServer.jar
├── automations.json
├── config.json
└── gadgets.json
```
## Step 2: config.json

Specify the connection credentials to the PublicServer. See main page.

## Step 3:  Run HomeServer
Since the ALMA home server is intended to be run on a Raspberry Pi acting as the hub of your smart home system, the HomeServer application should be configured to launch as a Linux daemon. One suggested approach is to set it up as a `systemd` background service on your Raspberry Pi. This setup allows easy managagement and debugging with real-time log tracking, while still leaving your Pi availbale to run other services and operations of your liking in parrallel.  to serve additional purposes of your liking

# Setup gadgets
Once the HomeServer is running inside your LAN with a successful connection to a PublicServer instance, you can start introducing and configuring gadgets.

## Native ALMA gadgets

## TP-Link gadgets

# Setup automations
See main page.
