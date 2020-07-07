
# ALMA HomeServer
The HomeServer instance is the key component of your ALMA home automation system. In fact, if you register your homeServer instance to an active ALMA PublicServer (to obtain remote login credentials), the HomeServer instance is all you need to start automating you home. With the ALMA AndroidClient you can also control and monitor your smart home remotely.

Additional information about the HomeServer and other ALMA nodes can be found on the repository main page.

# Get started with your ALMA HomeServer
There are no hard-coding requirements involved in setting up your ALMA HomeServer. However, there are some configurations needed.

1. Create JSON files named `config.json`, `gadgets.json` and `automations.json`
  * Location requirement: Same directory as the running PublicServer instance.
  - Read about the JSON files on the repository main page.
  - See full/extended examples of the required JSON files below.
2. Place the files accessible for the HomeServer application.
3. Specify the paths to the files in HomeServer class `JSON_reader`:
```java
gadgetFileJSON = "/path/to/gadgets.json";
configFileJSON = "/path/to/config.json";
automationsFileJSON = "/path/to/automations.json";
```
Since the ALMA home server is intended to be run on a Raspberry Pi acting as the hub of your smart home system, the HomeServer application should be configured to launch as a Linux daemon. One suggested approach is to set it up as a `systemd` background service on your Raspberry Pi. This setup allows easy managagement and debugging with real-time log tracking, while still leaving your Pi availbale to run other services and operations of your liking in parrallel.  to serve additional purposes of your liking

Information of how to run a Java project as a Linux daemon can be found online. Here is a suggested approach:
- Create a JAR-file from the HomeServer java applicaiton.
- Place the JAR-file in the same directory as the HomeServer JSON-files created above. Required structure:
```
File structure
```
- Create a bash script which launches the jar. E.g:
```
bash script example
```
- Create a systemd service in which you specify the path to the bash-file and set up additonal parameters of your liking.

```
folder-ALMA
- HomeServer.jar
- config.json
- automations.json
- gadgets.json
```
