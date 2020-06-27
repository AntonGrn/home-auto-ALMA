package main.gadgets;

public class Gadget_rf433MHz extends Gadget {

    public Gadget_rf433MHz(int gadgetID, String name, GadgetType type, long pollDelaySeconds) {
        super(gadgetID, name, type, pollDelaySeconds);
    }

    @Override
    public void poll() {}

    @Override
    public void alterState(int requestedState) throws Exception {

    }

    @Override
    public String sendCommand(String command) throws Exception {
        return null;
    }
}
