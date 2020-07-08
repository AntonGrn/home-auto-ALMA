package main.gadgets.plugins;

import main.gadgets.Gadget;
import main.gadgets.GadgetType;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

public class Gadget_local_Pi_CPU_temp extends Gadget {
    /**
     * In 'gadgets.json':
     * plugin_id: "local_pi_cpu_temp"
     **
     * Note: This plugin is configured to work in HomeServers
     * running on a Raspberry Pi 3 Model B+.
     */

    public Gadget_local_Pi_CPU_temp(int gadgetID, String alias, long pollDelaySeconds) {
        super(gadgetID, alias, GadgetType.SENSOR_VALUE, pollDelaySeconds);
    }

    @Override
    public void poll() {
        try {
            setState(read_CPU_temp());
            isPresent = true;
        } catch (Exception e) {
            isPresent = false;
        }
    }

    @Override
    public void alterState(int requestedState) throws Exception {
        // Not called on a SENSOR_VALUE type gadget.
        throw new Exception("Attempt to alter state on SENSOR_VALUE gadget " + alias);
    }

    @Override
    protected String sendCommand(String command) throws Exception {
        // Not called.
        return null;
    }

    private int read_CPU_temp() throws Exception {
        String fileName = "/sys/class/thermal/thermal_zone0/temp";
        String line = null;
        int tempC = 0;
        try {
            try (BufferedReader bufferedReader = new BufferedReader(new FileReader(fileName))) {

                while ((line = bufferedReader.readLine()) != null) {
                    tempC = (Integer.parseInt(line) / 1000);
                }
            }
        } catch (FileNotFoundException ex) {
            throw new Exception("Unable to open file '" + fileName + "'");
        } catch (IOException ex) {
            throw new Exception("Error reading file '" + fileName + "'");
        } catch (NumberFormatException e) {
            throw new Exception("Invalid number format of value: " + line);
        }
        return tempC;
    }
}
