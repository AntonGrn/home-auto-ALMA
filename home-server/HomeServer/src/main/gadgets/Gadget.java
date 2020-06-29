package main.gadgets;

public abstract class Gadget {

    /**
     * @see #poll() Polls the gadget state and presence at interval: pollDelayMs (read from file: gadgets.json)
     * - Objective: set isPresent
     * - Objective: set state
     * @see #alterState(int) Sends request to gadget to alter its state (for non-sensor gadgets)
     * - Objective: communicate state change request to gadget
     * - Objective: set state
     * @see #sendCommand(String) Used by #poll() and #alterState() to communicate with the gadget
     * - Objective: communicate with gadget according to gadget architecture
     *
     * @see #isPresent
     *  true: Gadget is displayed for Android clients
     *  false: Gadget is not displayed for Android clients
     */

    public final int gadgetID;
    public final String alias;
    public final GadgetType type;
    private int state;
    public long lastPollTime;
    public final long pollDelayMs;
    public boolean isPresent;

    // Gadgets are instantiated from JSON file (gadgets.json) at system boot
    public Gadget(int gadgetID, String alias, GadgetType type, long pollDelaySeconds) {
        this.gadgetID = gadgetID;
        this.alias = alias;
        this.type = type;
        state = -1;
        pollDelayMs = pollDelaySeconds * 1000;
        isPresent = false;
    }

    public abstract void poll();
    public abstract void alterState(int requestedState) throws Exception;
    public abstract String sendCommand(String command) throws Exception;

    public int getState() {
        return state;
    }

    public void setState(int newState) {
        boolean isOnOffGadget = (type == GadgetType.CONTROL_ONOFF || type == GadgetType.SENSOR_ONOFF);
        if (isOnOffGadget) {
            state = (newState == 1 ? 1 : 0);
        } else {
            state = newState;
        }
    }

}
