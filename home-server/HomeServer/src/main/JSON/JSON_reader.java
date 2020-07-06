package main.JSON;

import main.gadgets.*;
import main.gadgets.automations.*;
import main.gadgets.plugins.Gadget_local_Pi_CPU_temp;
import main.gadgets.plugins.Gadget_local_Pi_GPIO_onoff;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.FileReader;
import java.io.Reader;
import java.util.ArrayList;

public class JSON_reader {

    private final String gadgetFileJSON;
    private final String configFileJSON;
    private final String automationsFileJSON;
    private final Object lock_gadgetFile;
    private final Object lock_configFile;
    private final Object lock_automationsFile;

    public JSON_reader() {
        // Specify path to JSON file 'gadgets.json':
        gadgetFileJSON = "/path/to/gadgets.json";
        // Specify path to JSON file 'config.json':
        configFileJSON = "/path/to/config.json";
        // Specify path to JSON 'automations.json'
        automationsFileJSON = "/path/to/automations.json";
        // Lock objects
        lock_gadgetFile = new Object();
        lock_configFile = new Object();
        lock_automationsFile = new Object();
    }

    // =================================== HOME SERVER DATA ===================================================

    // Called by class Server at system boot
    public String[] loadConfigData() throws Exception {
        synchronized (lock_configFile) {
            try {
                String[] configData = new String[6];
                JSONObject jsonSystemData = toJsonObject(configFileJSON);
                configData[0] = ((Long) jsonSystemData.get("hub_ID")).toString();
                configData[1] = (String) jsonSystemData.get("hub_alias");
                configData[2] = (String) jsonSystemData.get("hub_password");
                configData[3] = ((Boolean) jsonSystemData.get("hub_debug_mode")).toString();
                configData[4] = (String) jsonSystemData.get("public_server_IP");
                configData[5] = ((Long) jsonSystemData.get("public_server_port")).toString();
                return configData;
            } catch (Exception e) {
                throw new Exception("Unable to read JSON file 'config.json'");
            }
        }
    }

    // ================================ LOAD ALL GADGETS =====================================================

    // Called by class Server at system boot
    public ArrayList<Gadget> loadAllGadgets() throws Exception {
        synchronized (lock_gadgetFile) {
            // Read in file as JSON object
            JSONObject jsonGadgets = toJsonObject(gadgetFileJSON);
            // Read all gadget architectures
            ArrayList<Gadget> gadgets_ALMA = loadGadgets_ALMA((JSONArray) jsonGadgets.get("alma"));
            ArrayList<Gadget> gadgets_TP_Link = loadGadgets_TP_Link((JSONArray) jsonGadgets.get("tp_link"));
            ArrayList<Gadget> gadgets_rf433MHz = loadGadgets_rf433MHz((JSONArray) jsonGadgets.get("rf433MHz"));
            ArrayList<Gadget> gadgets_plugin = loadGadgets_plugins((JSONArray) jsonGadgets.get("plugins"));
            // Concat all architectures
            ArrayList<Gadget> allGadgets = new ArrayList<>();
            allGadgets.addAll(gadgets_ALMA);
            allGadgets.addAll(gadgets_TP_Link);
            allGadgets.addAll(gadgets_rf433MHz);
            allGadgets.addAll(gadgets_plugin);
            // Return entire gadgetList
            return allGadgets;
        }
    }

    // =========================== LOAD GADGETS BASED ON ARCHITECTURE ========================================

    private ArrayList<Gadget> loadGadgets_ALMA(JSONArray gadgets) {
        ArrayList<Gadget> gadgets_ALMA = new ArrayList<>();
        for (Object object : gadgets) {
            JSONObject jsonGadget = (JSONObject) object;
            if ((Boolean) jsonGadget.get("enabled")) {
                int gadgetID = ((Long) jsonGadget.get("gadget_id")).intValue();
                String alias = (String) jsonGadget.get("alias");
                GadgetType gadgetType = GadgetType.valueOf((String) jsonGadget.get("type"));
                long pollDelaySeconds = (Long) jsonGadget.get("poll_delay_seconds");
                String IP = (String) jsonGadget.get("IP_address");
                int port = ((Long) jsonGadget.get("TCP_port")).intValue();
                String requestSpec = (String) jsonGadget.get("request_spec");
                // Add gadget to list
                gadgets_ALMA.add(new Gadget_ALMA(gadgetID, alias, gadgetType, pollDelaySeconds, IP, port, requestSpec));
            }
        }
        return gadgets_ALMA;
    }

    private ArrayList<Gadget> loadGadgets_TP_Link(JSONArray gadgets) {
        ArrayList<Gadget> gadgets_TP_Link = new ArrayList<>();
        for (Object object : gadgets) {
            JSONObject jsonGadget = (JSONObject) object;
            if ((Boolean) jsonGadget.get("enabled")) {
                int gadgetID = ((Long) jsonGadget.get("gadget_id")).intValue();
                String alias = (String) jsonGadget.get("alias");
                long pollDelaySeconds = (Long) jsonGadget.get("poll_delay_seconds");
                String IP = (String) jsonGadget.get("IP_address");
                int port = ((Long) jsonGadget.get("TCP_port")).intValue();
                String model = (String) jsonGadget.get("model");
                // Add gadget to list
                switch (model) {
                    case "HS100":
                        gadgets_TP_Link.add(new Gadget_HS100(gadgetID, alias, pollDelaySeconds, IP, port));
                        break;
                    case "HS110":
                        gadgets_TP_Link.add(new Gadget_HS110(gadgetID, alias, pollDelaySeconds, IP, port));
                        break;
                }
            }
        }
        return gadgets_TP_Link;
    }

    private ArrayList<Gadget> loadGadgets_rf433MHz(JSONArray gadgets) {
        ArrayList<Gadget> gadgets_rf433MHz = new ArrayList<>();
        for (Object object : gadgets) {
            JSONObject jsonGadget = (JSONObject) object;
            if ((Boolean) jsonGadget.get("enabled")) {
                int gadgetID = ((Long) jsonGadget.get("gadget_id")).intValue();
                String alias = (String) jsonGadget.get("alias");
                String pathToScript = (String) jsonGadget.get("path_rpi-rf_send");
                int gpio = ((Long) jsonGadget.get("gpio_BCM")).intValue();
                int protocol = ((Long) jsonGadget.get("protocol")).intValue();
                int pulseLength = ((Long) jsonGadget.get("pulse_length")).intValue();
                int codeON = ((Long) jsonGadget.get("code_ON")).intValue();
                int codeOFF = ((Long) jsonGadget.get("code_OFF")).intValue();
                gadgets_rf433MHz.add(new Gadget_rf433MHz(gadgetID, alias, pathToScript, gpio, protocol, pulseLength, codeON, codeOFF));
            }
        }
        return gadgets_rf433MHz;
    }

    private ArrayList<Gadget> loadGadgets_plugins(JSONArray gadgets) {
        ArrayList<Gadget> gadgets_plugin = new ArrayList<>();
        for (Object object : gadgets) {
            JSONObject jsonGadget = (JSONObject) object;
            int gadgetID = ((Long) jsonGadget.get("gadget_id")).intValue();
            String alias = (String) jsonGadget.get("alias");
            if ((Boolean) jsonGadget.get("enabled")) {
                switch ((String) jsonGadget.get("plugin_id")) {
                    case "local_pi_cpu_temp":
                        long pollDelaySeconds = (Long) jsonGadget.get("poll_delay_seconds");
                        gadgets_plugin.add(new Gadget_local_Pi_CPU_temp(gadgetID, alias, pollDelaySeconds));
                        break;
                    case "local_pi_gpio_onoff":
                        int gpio = ((Long) jsonGadget.get("gpio_BCM")).intValue();
                        String pathToScript = (String) jsonGadget.get("path_gpio-local-onoff.py");
                        gadgets_plugin.add(new Gadget_local_Pi_GPIO_onoff(gadgetID, alias, pathToScript, gpio));
                        break;
                }
            }
        }
        return gadgets_plugin;
    }

    // ================================ LOAD AUTOMATIONS =====================================================

    // Called by class Server at system boot
    public ArrayList<Automation> loadAutomations() throws Exception {
        synchronized (lock_automationsFile) {
            ArrayList<Automation> automations = new ArrayList<>();
            // Read in file as JSON array
            JSONArray jsonAutomations = toJsonArray(automationsFileJSON);
            for (Object object : jsonAutomations) {
                JSONObject jsonAutomation = (JSONObject) object;
                String name = (String) jsonAutomation.get("name");
                if ((Boolean) jsonAutomation.get("enabled")) {
                    // Load automation timer
                    JSONObject jsonTimer = (JSONObject) jsonAutomation.get("timer");
                    int hours = ((Long) jsonTimer.get("hours")).intValue();
                    int minutes = ((Long) jsonTimer.get("minutes")).intValue();
                    int seconds = ((Long) jsonTimer.get("seconds")).intValue();
                    Timer timer = new Timer(hours, minutes, seconds);
                    // Load action(s)
                    ArrayList<Action> actions = new ArrayList<>();
                    JSONArray jsonActions = (JSONArray) jsonAutomation.get("action");
                    for (Object obj : jsonActions) {
                        JSONObject jsonAction = (JSONObject) obj;
                        int targetGadgetID = ((Long) jsonAction.get("gadget_id")).intValue();
                        int targetState = ((Long) jsonAction.get("state")).intValue();
                        actions.add(new Action(targetGadgetID, targetState));
                    }
                    // Load automation trigger
                    JSONObject trigger = (JSONObject) jsonAutomation.get("trigger");
                    // Create automation based on trigger type
                    switch ((String) trigger.get("type")) {
                        case "event":
                            int triggerGadgetID = ((Long) trigger.get("gadget_id")).intValue();
                            StateCondition condition = StateCondition.valueOf(((String) trigger.get("state_condition")).toUpperCase());
                            int triggerState = ((Long) trigger.get("state")).intValue();
                            automations.add(new Automation_Event(name, timer, actions, triggerGadgetID, triggerState, condition));
                            break;
                        case "time":
                            String time[] = ((String) trigger.get("time")).split(":");
                            int hour = Integer.parseInt(time[0]);
                            int minute = Integer.parseInt(time[1]);
                            automations.add(new Automation_Time(name, timer, actions, hour, minute));
                            break;
                        default:
                            throw new Exception("Invalid trigger type");
                    }
                }
            }
            return automations;
        }
    }


    // ===================================== UTILITY METHODS ==================================================

    private JSONObject toJsonObject(String jsonFile) throws Exception {
        try (Reader reader = new FileReader(jsonFile)) {
            JSONParser parser = new JSONParser();
            return (JSONObject) parser.parse(reader);
        }
    }

    private JSONArray toJsonArray(String jsonFile) throws Exception {
        try (Reader reader = new FileReader(jsonFile)) {
            JSONParser parser = new JSONParser();
            return (JSONArray) parser.parse(reader);
        }
    }

}
