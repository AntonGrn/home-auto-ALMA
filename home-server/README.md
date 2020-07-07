1. Create JSON files named `config.json`, `gadgets.json` and `automations.json`
2. Place the files accessible for the HomeServer application.
3. Specify the paths to the files in HomeServer class `JSON_reader`:
```java
// Specify path to JSON file 'gadgets.json':
gadgetFileJSON = "/path/to/gadgets.json";
// Specify path to JSON file 'config.json':
configFileJSON = "/path/to/config.json";
// Specify path to JSON 'automations.json'
automationsFileJSON = "/path/to/automations.json";
```
1. Create a JSON file named `config.json`.
2. Place the file accessible for the HomeServer application.
3. Specify the path to the file in HomeServer class `JSON_reader`:
```java
configFileJSON = "/path/to/config.json";
```
```
folder-ALMA
- HomeServer.jar
- config.json
- automations.json
- gadgets.json
```
