
# About HomeServer
The HomeServer instance is the key component of your ALMA home automation system. In fact, if you register your HomeServer instance to an active ALMA PublicServer, the HomeServer instance is all you need to start automating you home. With the ALMA AndroidClient you can also control and monitor your smart home remotely.

See [main page](link) for more information.

# Get started with your ALMA HomeServer

There are no hard-coding involved in setting up your ALMA HomeServer. However, there are some configurations needed.

## Step 1: Required files

**Approach 1:** Download complete packet

Download [compiled version](link) of HomeServer including all required configuration files.

**Approach 2:** Manual files setup

1. Create the following JSON files: `config.json`, `gadgets.json` and `automations.json`. See [main page](link) for more information about purpose and format.
- `config.json`:
```yaml
{
  "alma": [],
  "tp_link": [],
  "rf433MHz": [],
  "plugins": []
}
```
- `gadgets.json`:
```yaml 
[]
```
   - See [main page](link) for more information about purpose and format.
2. Create a JAR-file from the HomeServer [java project](link).
3. Place the JAR-file in the same directory as the HomeServer JSON files created above. Required structure:
```
/home-auto-ALMA (name of your choosing)
├── HomeServer.jar
├── automations.json
├── config.json
└── gadgets.json
```
## Step 2: Connection to PublicServer

Specify connection credentials in `config.json`. See [main page](link).

## Step 3:  Run HomeServer
Since the ALMA home server is intended to be run on a Raspberry Pi (acting as the hub of your smart home system), the HomeServer application should be configured to launch as a Linux daemon. One suggested approach is to set it up as a `systemd` background service on your Raspberry Pi. This setup allows easy managagement and debugging with real-time log tracking, while still leaving your Pi available to run other services and operations of your liking in parrallel.  to serve additional purposes of your liking

# Get started with Gadgets
Once the HomeServer is running inside your LAN with a successful connection to a PublicServer instance, you can start introducing and configuring gadgets.

See [main page](link) for information about gadgets.

## Native ALMA gadgets
Any software capable to setup a Socket TCP Server instance can confirm to native ALMA gadgets to gather integer value states.

## TP-Link gadgets

## RF 433 MHz gadgets

# Get started with Automations
See main page.
