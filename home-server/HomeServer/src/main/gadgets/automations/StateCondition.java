package main.gadgets.automations;

/**
 * Used by Automation_Event to set condition for a gadget state trigger.
 * "If eventState [CONDITION] trigger state."
 * eventState = Actual state of gadget.
 */

public enum StateCondition {EQUAL_TO, GREATER_THAN, LESS_THAN, GREATER_OR_EQUAL_TO, LESS_OR_EQUAL_TO}
