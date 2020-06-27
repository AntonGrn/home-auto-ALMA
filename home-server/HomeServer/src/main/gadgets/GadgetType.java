package main.gadgets;

/**
 * Supported ALMA gadget types for:
 * - Correct display in the ALMA Android client application.
 * - Verify correct gadget management.
 *
 * @see #CONTROL_ONOFF
 *  - Interactive
 *  - Remotely controlled on/off gadgets (toggle)
 *  - E.g. Lamp.
 * @see #CONTROL_VALUE
 *  - Interactive
 *  - Remotely controlled integer value gadgets
 *  - E.g. Temperature threshold
 * @see #SENSOR_ONOFF
 *  - Not interactive
 *  - Remotely monitor on/off state of sensor gadget
 *  - E.g. Window is closed
 * @see #SENSOR_VALUE
 *  - Not interactive
 *  - Remotely monitor integer state of sensor gadget
 *  - E.g. Temperature sensor
 */

public enum GadgetType {CONTROL_ONOFF, CONTROL_VALUE, SENSOR_ONOFF, SENSOR_VALUE}
