package main.gadgets.automations;

public class Action {
    public int targetGadgetID;
    public int targetGadgetState;

    public Action(int targetGadgetID, int targetGadgetState) {
        this.targetGadgetID = targetGadgetID;
        this.targetGadgetState = targetGadgetState;
    }

    public String toAlmaString() {
        // Form ALMA request to alter gadget state.
        return String.format("9:-1:%s:%s", targetGadgetID, targetGadgetState);
    }
}
