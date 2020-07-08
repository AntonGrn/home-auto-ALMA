package main.gadgets.plugins;

import main.gadgets.Gadget;
import main.gadgets.GadgetType;

import java.io.IOException;

public class Gadget_local_Pi_GPIO_onoff extends Gadget {
    /**
     * In 'gadgets.json':
     * plugin_id: "local_pi_gpio_onoff"
     **
     * Note: This plugin is configured to work in HomeServers
     * running on a Raspberry Pi 3 Model B+.
     */

    private final String pathToGpioLocalOnoffPy;
    private final int GPIO;

    public Gadget_local_Pi_GPIO_onoff(int gadgetID, String alias, int GPIO) {
        super(gadgetID, alias, GadgetType.CONTROL_ONOFF, 120);
        this.pathToGpioLocalOnoffPy = "./python-scripts/gpio-local-onoff.py";
        this.GPIO = GPIO;
        setState(0); // Assumed initial state of non-feedback gadget.
    }

    @Override
    public void poll() {
        // GPIO onoff is one way communication (no feedback).
        isPresent = true;
    }

    @Override
    public void alterState(int requestedState) throws Exception {
        String state = (requestedState == 1 ? "ON" : "OFF");
        String systemRequest = String.format("%s %s %s %s",
                "python3", pathToGpioLocalOnoffPy, GPIO, state);
        sendCommand(systemRequest);
        // No feedback requested from GPIO.
        setState(requestedState);
    }

    @Override
    protected String sendCommand(String command) throws Exception {
        Runtime.getRuntime().exec(command);
        return null;
    }
}
