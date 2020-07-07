Create JSON files named `config.json`, `gadgets.json` and `automations.json`
Place the files accessible for the HomeServer application.
Specify the paths to the files in HomeServer class `JSON_reader`:
```java
// Specify path to JSON file 'gadgets.json':
gadgetFileJSON = "/path/to/gadgets.json";
// Specify path to JSON file 'config.json':
configFileJSON = "/path/to/config.json";
// Specify path to JSON 'automations.json'
automationsFileJSON = "/path/to/automations.json";
```
