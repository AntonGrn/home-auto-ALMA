package main.gadgets;

public class Gadget_rf433MHz extends Gadget {

    /**
     * Control of rf (433MHz) outlets from Raspberry Pi ALMA HomeServer hub,
     * using python module rpi-rf:
     * @see <a href="https://pypi.org/project/rpi-rf/"/a>
     */
    // Absolute path to python script 'rpi-rf_send' (inclusive)
    private final String pathTo433MHzSendScript;
    // 'rpi-rf_send' arguments:
    private final int GPIO;
    private final int protocol;
    private final int pulseLength;
    private final int CODE_ON;
    private final int CODE_OFF;

    public Gadget_rf433MHz(int gadgetID, String name, String pathToScript, int gpio, int protocol, int pulseLength, int codeON, int codeOFF) {
        super(gadgetID, name, GadgetType.CONTROL_ONOFF, 120);
        pathTo433MHzSendScript = pathToScript;
        this.GPIO = gpio;
        this.protocol = protocol;
        this.pulseLength = pulseLength;
        this.CODE_ON = codeON;
        this.CODE_OFF = codeOFF;
    }

    @Override
    public void poll() {
        // 433MHz plugs are one way communication (no feedback).
        // = Ignore poll
        // Another approach would be to use the poll to re-assure
        // that the gadget is at the last requested state, by
        // sending a new request of that state at every poll.
    }

    @Override
    public void alterState(int requestedState) throws Exception {
        if (type == GadgetType.CONTROL_ONOFF || type == GadgetType.CONTROL_VALUE) {
            int stateCode = (requestedState == 1 ? CODE_ON : CODE_OFF);
            // Create 'rpi-rf_send' command, format:
            //  python3 rpi-rf_send CODE [-g GPIO] [-p PULSELENGTH] [-t PROTOCOL]
            String rpi_rf_send = String.format("%s%s%s%s%s%s%s%s%s%s",
                    "python3 ", pathTo433MHzSendScript, " ",stateCode, " -g ", GPIO, " -p ", pulseLength, " -t ", protocol);
            sendCommand(rpi_rf_send);
            // No feedback from 433MHz switches.
            setState(requestedState);
        }
    }

    @Override
    public String sendCommand(String command) throws Exception {
        Runtime.getRuntime().exec(command);
        return null;
    }
}
