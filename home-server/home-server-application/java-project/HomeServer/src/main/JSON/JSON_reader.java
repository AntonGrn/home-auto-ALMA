package main.JSON;

import main.gadgets.*;
import main.automations.*;
import main.gadgets.plugins.Gadget_local_Pi_CPU_temp;
import main.gadgets.plugins.Gadget_local_Pi_GPIO_onoff;
import main.settings.Settings;
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
        // JSON file paths
        gadgetFileJSON = "./json/gadgets.json";
        configFileJSON = "./json/config.json";
        automationsFileJSON = "./json/automations.json";
        // Lock objects
        lock_gadgetFile = new Object();
        lock_configFile = new Object();
        lock_automationsFile = new Object();
    }

    // =================================== HOME SERVER DATA ===================================================

    // Called by class Server at system boot
    public Settings loadConfigData() throws Exception {
        synchronized (lock_configFile) {
            try {
                JSONObject jsonSystemData = toJsonObject(configFileJSON);
                boolean debugMode = (Boolean) jsonSystemData.get("debug_mode");
                boolean remoteAccess = (Boolean) jsonSystemData.get("public_server_connection");
                String hubAlias = (String) jsonSystemData.get("hub_alias");
                int hubID = ((Long) jsonSystemData.get("hub_ID")).intValue();
                String hubPwd = (String) jsonSystemData.get("hub_password");
                String publServerIP = (String) jsonSystemData.get("public_server_IP");
                int pubServerPort = ((Long) jsonSystemData.get("public_server_port")).intValue();
                return new Settings(debugMode, remoteAccess, hubAlias, hubID, hubPwd, publServerIP, pubServerPort);
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
                int gpio = ((Long) jsonGadget.get("gpio_BCM")).intValue();
                int protocol = ((Long) jsonGadget.get("protocol")).intValue();
                int pulseLength = ((Long) jsonGadget.get("pulse_length")).intValue();
                int codeON = ((Long) jsonGadget.get("code_ON")).intValue();
                int codeOFF = ((Long) jsonGadget.get("code_OFF")).intValue();
                gadgets_rf433MHz.add(new Gadget_rf433MHz(gadgetID, alias, gpio, protocol, pulseLength, codeON, codeOFF));
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
                        gadgets_plugin.add(new Gadget_local_Pi_GPIO_onoff(gadgetID, alias, gpio));
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
