package main.JSON;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.FileReader;
import java.io.Reader;

public class JSON_reader {

    // Specify path to JSON file 'config.json' :
    private static final String configFileJSON = "/home/anton/projectALMA/config.json";
    private static final Object lock_configFile = new Object();

    public static int[] loadServerSpecs() throws Exception {
        synchronized (lock_configFile) {
            JSONObject public_server = toJsonObject("public_server");
            int tcp_port = ((Long)public_server.get("tcp_port")).intValue();
            int thread_pool_limit = ((Long)public_server.get("thread_pool")).intValue();
            int debugMode = ((Boolean)public_server.get("debug_mode")) ? 1 : 0;
            return new int[]{tcp_port, thread_pool_limit, debugMode};
        }
    }

    public static String[] loadSpecsClientDB() throws Exception {
        synchronized (lock_configFile) {
            JSONObject database_clients = toJsonObject("database_clients");
            String ip = (String)database_clients.get("ip");
            String port = ((Long)database_clients.get("port")).toString();
            String database = (String)database_clients.get("database");
            String account = (String)database_clients.get("account");
            String password = (String)database_clients.get("password");
            return new String[]{ip, port, database, account, password};
        }
    }

    public static String[] loadSpecsTrafficLogsDB() throws Exception {
        synchronized (lock_configFile) {
            JSONObject database_traffic_logs = toJsonObject("database_traffic_logs");
            String ip = (String)database_traffic_logs.get("ip");
            String port = ((Long)database_traffic_logs.get("port")).toString();
            String database = (String)database_traffic_logs.get("database");
            String account = (String)database_traffic_logs.get("account");
            String password = (String)database_traffic_logs.get("password");
            return new String[]{ip, port, database, account, password};
        }
    }

    private static JSONObject toJsonObject(String objectKey) throws Exception {
        try (Reader reader = new FileReader(configFileJSON)) {
            JSONParser parser = new JSONParser();
            JSONObject jsonObject = (JSONObject) parser.parse(reader);
            return (JSONObject) jsonObject.get(objectKey);
        }
    }
}
