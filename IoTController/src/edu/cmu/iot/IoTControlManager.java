package edu.cmu.iot;

import java.io.*;
import java.util.*;

/**
 * Controls the state of the IoT house. This class manages house state; it is the focal point for changing settings
 * adding users, and ensuring that the house remains in a consistent state
 *
 * Project: LG Exec Ed Program
 * Copyright: 2015 Jeffrey S. Gennari
 * Versions:
 * 1.0 November 2015 - initial version
 */
public class IoTControlManager {

    /** connection to the house */
    private IoTConnectManager connMgr;

    /** reference to main window*/
    private IoTControlWindow controlWindow;

    /** current state variables */

    private Integer tempReading;         // the current temperature
    private Integer humidityReading;     // the current humidity
    private Boolean newDoorState;        // the state of the door (true if open, false if closed)
    private Boolean newLightState;       // the state of the light (true if on, false if off)
    private Boolean newProximityState;   // the state of the proximity sensor (true of house occupied, false if vacant)
    private Boolean newAlarmState;       // the alarm state (true if enabled, false if disabled)
    private Boolean newHumidifierState;  // the humidifier state (true if on, false if off)
    private Boolean newHeaterOnState;    // the heater state (true if on, false if off)
    private Boolean newChillerOnState;   // the chiller state (true if on, false if off)
    private Boolean newAlarmActiveState; // the alarm active state (true if alarm sounding, false if alarm not sounding)
    private String newHvacSetting;       // the HVAC mode setting, either Heater or Chiller


    /** previous values of state variables - used to detect change and revert to a good state */
    private Boolean oldDoorState, oldLightState,
            oldProximityState, oldAlarmState, oldHumidifierState,
            oldHeaterOnState, oldChillerOnState, oldAlarmActiveState;
    private String oldHvacSetting;

    private Integer targetTempSetting;   // the user-desired temperature setting

    /** the given alarm passcode */
    private String alarmPassCode;

    /** the user settings */
    private Hashtable<String, Object> userSettings;

    /** the path to user settings and credentials */
    private String settingsPath;

    /**  the log messages */
    private Vector<String> logMessages;

    /** Thread to manage state updates */
    private Thread updateThread = null;

    /**
     * Constructor for the controller
     *
     * @param controlWin  the window to be managed
     * @param path the path to settings files
     */
    public IoTControlManager(IoTControlWindow controlWin, String path) {

        logMessages = new Vector<String>();
        controlWindow = controlWin;

        userSettings = new Hashtable<String, Object >();

        settingsPath = path;

        connMgr = null;

        newAlarmActiveState = false;

    }

    /**
     * Load the registered users from a database (file).
     * @return the set of valid users
     */
    public Vector<UserLoginInfo> loadUsers() {
        Vector<UserLoginInfo> users = new Vector<UserLoginInfo>();

        try {
            File file = new File(settingsPath + File.separator + IoTValues.USERS_DB);

            BufferedReader br = new BufferedReader(new FileReader(file));

            String line;
            while ((line = br.readLine()) != null) {
                String [] entry = line.split("=");
                users.add(new UserLoginInfo(entry[0],entry[1]));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return users;
    }

    /**
     * Load the user preferences from a database (file).
     */
    public void loadSettings() {
        Properties props = new Properties();
        InputStream is = null;

        // First try loading from the current directory
        try {
            File f = new File(settingsPath + File.separator + IoTValues.SETTINGS_FILE);
            is = new FileInputStream( f );
            props.load( is );
        } catch ( Exception e ) { e.printStackTrace(); }

        alarmPassCode = props.getProperty(IoTValues.ALARM_PASSCODE, "passcode");
        Integer alarmDelay = new Integer(props.getProperty(IoTValues.ALARM_DELAY, "5"));

        Hashtable<String, Object> initialSettings = new Hashtable<String, Object>();
        initialSettings.put(IoTValues.ALARM_DELAY, alarmDelay);

        // update the settings
        updateSettings(initialSettings);
    }

    /**
     * Update user settings (the alarm delay)
     *
     * @param newSettings the new user settings.
     */
    public void updateSettings(Hashtable<String, Object> newSettings) {
        if (userSettings!=null) {
            userSettings.putAll(newSettings);
        }
    }

    /**
     * Fetch the user settings
     * @return the user settings
     */
    public Hashtable<String, Object> getUserSettings() {
        return userSettings;
    }

    public void addUser(String newUsername, String newPassword) {
        try {
            File f = new File(settingsPath + File.separator + IoTValues.USERS_DB);
            BufferedWriter bw = new BufferedWriter(new FileWriter(f, true));
            bw.write(newUsername+"="+newPassword + "\n");
            bw.close();
            controlWindow.updateLog("Added new user: " + newUsername);
        } catch (Exception e) {e.printStackTrace(); }
    }


    private void startHouseUpdateThread() {

        updateThread = new Thread(new Runnable() {
            @Override
            public void run() {
                Hashtable<String, Object> state = null;
                Integer missedUpdates = 0;
                while (true) {

                    synchronized (connMgr) {
                        if (connMgr.isConnected() == false) {
                            return;
                        }
                        state = connMgr.getState();
                    }
                    if (state != null) {
                        processStateUpdate(state);
                        missedUpdates=0;
                        if (controlWindow.isEnabled() == false) {
                            controlWindow.enableControls();
                        }
                    } else {
                        missedUpdates++;
                    }

                    if (missedUpdates > 6) { // 6 missed updates is 30 seconds
                        controlWindow.updateLog("Warning: lost contact with house");
                        revertToLastState();
                        if (controlWindow.isEnabled()) {
                            controlWindow.disableControls();
                        }
                    }

                    // currently a 5sec delay
                    try { Thread.sleep(5000); } catch (InterruptedException ie) { }
                }
            }
        });
        updateThread.start();
    }

    /**
     * Determine if the new state is feasible. If it is not, then discard the processStateUpdate and/or revert
     * to safe state
     * @param newState the new state
     */
    public synchronized void processStateUpdate(Hashtable<String, Object> newState) {

        synchronized (connMgr) {
            synchronized(newState) {

                // determine if the new state should be accepted. Make any necessary corrections
                evaluateNewState(newState);

                updateInternalState();

                // the current state has been evaluated
                connMgr.setState(getCurrentState());
            }
        }
    }

    /**
     * Get the current state
     *
     * @return the current state
     */
    private synchronized Hashtable<String,Object> getCurrentState() {
        Hashtable<String,Object> state = new Hashtable<String, Object>();
        if (tempReading!=null)
            state.put(IoTValues.TEMP_READING, tempReading);
        if (newHumidifierState!=null)
            state.put(IoTValues.HUMIDIFIER_STATE,newHumidifierState);
        if (humidityReading!=null)
            state.put(IoTValues.HUMIDITY_READING,humidityReading);
        if (newDoorState!=null)
            state.put(IoTValues.DOOR_STATE,newDoorState);
        if (newLightState!=null)
            state.put(IoTValues.LIGHT_STATE,newLightState);
        if (newProximityState!=null)
            state.put(IoTValues.PROXIMITY_STATE,newProximityState);
        if (newAlarmState!=null)
            state.put(IoTValues.ALARM_STATE,newAlarmState);
        if (newAlarmActiveState!=null)
            state.put(IoTValues.ALARM_ACTIVE, newAlarmActiveState);
        if (newHvacSetting!=null)
            state.put(IoTValues.HVAC_MODE,newHvacSetting);
        if (newHeaterOnState!=null)
            state.put(IoTValues.HEATER_STATE,newHeaterOnState);
        if (newChillerOnState!=null)
            state.put(IoTValues.CHILLER_STATE,newChillerOnState);

        return state;
    }

    /**
     * Issue the processStateUpdate to the house. Save the last state to detect changes
     */
    private synchronized void updateInternalState() {

        Hashtable<String,Object> state = getCurrentState();

        if (state.isEmpty() == false) {
            controlWindow.updateState(state);
            controlWindow.updateLog(logMessages);

            saveLastState();

            logMessages.clear();
        }
    }

    /**
     * Save the current state to determine changes
     */
    private synchronized void saveLastState() {

        if (newProximityState!=null)
            oldProximityState = newProximityState;
        if (newHeaterOnState!=null)
            oldHeaterOnState = newHeaterOnState;
        if (newChillerOnState!=null)
            oldChillerOnState = newChillerOnState;
        if (newHumidifierState!=null)
            oldHumidifierState = newHumidifierState;
        if (newAlarmActiveState!=null)
            oldAlarmActiveState = newAlarmActiveState;
        if (newDoorState!=null)
            oldDoorState = newDoorState;
        if (newLightState!=null)
            oldLightState = newLightState;
        if (newAlarmState!=null)
            oldAlarmState = newAlarmState;
        if (newHvacSetting!=null)
            oldHvacSetting = new String(newHvacSetting);
    }

    /**
     * Revert to the last known, good state
     */
    private synchronized void revertToLastState() {

        if (oldProximityState!=null)
            newProximityState = oldProximityState;

        if (oldHeaterOnState!=null)
            newHeaterOnState = oldHeaterOnState;

        if (oldChillerOnState!=null)
            newChillerOnState = oldChillerOnState;

        if (oldHumidifierState!=null)
            newHumidifierState = oldHumidifierState;

        if (oldAlarmActiveState!=null)
            newAlarmActiveState = oldAlarmActiveState;

        if (oldDoorState!=null)
            newDoorState = oldDoorState;

        if (oldLightState!=null)
            newLightState = oldLightState;

        if (oldAlarmState!=null)
            newAlarmState = oldAlarmState;

        if (oldHvacSetting!=null)
            newHvacSetting = new String(oldHvacSetting);

    }

    /**
     * Connect to a house
     *
     * @param houseAddress the network address of the house. Once connected, this method starts a new thread to
     *                     update house state
     * @return true if connected, false otherwise
     */
    public Boolean connectToHouse(String houseAddress) {

        connMgr = new IoTConnectManager(IoTConnection.getConnection(houseAddress));
        if (connMgr != null) {
            startHouseUpdateThread();

            return true;
        }
        return false;
    }

    /**
     * Disconnect from a house
     */
    public void disconnectFromHouse() {
        if (connMgr.isConnected()) {
            connMgr.disconnectFromHouse();
        }
    }

    /**
     * Start a timer when the house becomes unoccupied. When the timer expires, lock the house down
     */
    private void startAwayTimer() {
        Timer t = new Timer();

        Integer awayTimeout = (Integer) userSettings.get(IoTValues.ALARM_DELAY);

        t.schedule(new TimerTask() {

            /**
             * This anonymous thread updates the state when the house is vacant
             */
            @Override
            public void run() {

                StringBuffer logMsg = new StringBuffer();
                logMsg.append("Automatically: ");

                if (!newProximityState) {
                    // the house has not been occupied for a specified
                    // amount of time: turn off light, set alarm, and close door
                    if (newLightState) {
                        newLightState = false;
                        logMsg.append("turning off light ");
                    }
                    if (!newAlarmState) {
                        newAlarmState = true;
                        logMsg.append("setting alarm ");
                    }
                    if (newDoorState) {
                        newDoorState = false;
                        logMsg.append("closing door ");
                    }

                    synchronized (logMessages) {
                        logMessages.add(logMsg.toString());
                    }

                    processStateUpdate(getCurrentState());

                }
            }
        }, awayTimeout*1000);
    }

    /**
     * Ensure the requested state is permitted. This method
     * checks each state variable to ensure that the house remains
     * in a consistent state.
     *
     * @param state The new state to evaluate
     */
    public void evaluateNewState(Hashtable<String, Object> state) {

        System.out.println("Evaluating new state");

        String givenPassCode = null;

        Set<String> keys = state.keySet();
        for (String key : keys) {

            if (key.equals(IoTValues.TEMP_READING)) {
                tempReading = (Integer) state.get(key);
            } else if (key.equals(IoTValues.HUMIDITY_READING)) {
                humidityReading = (Integer) state.get(key);
            } else if (key.equals(IoTValues.TARGET_TEMP)) {
                targetTempSetting = (Integer) state.get(key);
            } else if (key.equals(IoTValues.HUMIDIFIER_STATE)) {
                newHumidifierState = (Boolean) state.get(key);
            } else if (key.equals(IoTValues.DOOR_STATE)) {
                newDoorState = (Boolean) state.get(key);
            } else if (key.equals(IoTValues.LIGHT_STATE)) {
                newLightState = (Boolean) state.get(key);
            } else if (key.equals(IoTValues.PROXIMITY_STATE)) {
                newProximityState = (Boolean) state.get(key);
            } else if (key.equals(IoTValues.ALARM_STATE)) {
                newAlarmState = (Boolean) state.get(key);
            } else if (key.equals(IoTValues.HVAC_MODE)) {
                newHvacSetting = (String) state.get(key);
            } else if (key.equals(IoTValues.PASSCODE)) {
                givenPassCode = (String) state.get(key);
            }
        }

        if (newLightState!=null) {
            if (newLightState != oldLightState) {

                if (newLightState) {
                    if (!newProximityState) {
                        logMessages.add("Cannot turn on light because user not home");
                        newLightState = false;

                    } else {
                        logMessages.add("Turning on light");
                    }
                } else if (!newLightState) {
                    logMessages.add("Turning off light");
                }
            }
        }

        // Door state changed
        if (newDoorState!=null) {
            if (newDoorState != oldDoorState) {

                // The door is now open
                if (newDoorState) {

                    if (newAlarmState && !newProximityState) {
                        // door open and no one home and the alarm is set - sound alarm
                        logMessages.add("Activating alarm");
                        newAlarmActiveState = true;
                    }
                    // House vacant, close the door
                    else if (!newProximityState) {
                        // close the door
                        newDoorState = false;
                        logMessages.add("Closing the door because user not home");
                    } else {
                        logMessages.add("Opened door");
                    }

                    // The door is open the alarm is to be set and somebody is home - this is not allowed so discard the processStateUpdate

                }
                // The door is now closed
                else if (!newDoorState) {
                    // the door is closed - if the house is suddenly occupied this is a break-in
                    if (newAlarmState && newProximityState) {
                        logMessages.add("Break-in detected - activating alarm");
                        newAlarmActiveState = true;
                    } else {
                        logMessages.add("Closed door");
                    }
                }
            }
        }

        if (newProximityState!=null) {
            // proximity state changed
            if (newProximityState != oldProximityState) {

                // the house is not occupied
                if (!newProximityState) {

                    logMessages.add("User not home");

                    startAwayTimer();
                }
                // the user has arrived
                else if (newProximityState) {
                    logMessages.add("User is home");
                    // if the alarm has been disabled, then turn on the light for the user
                    if (oldAlarmState!=null) {
                        if (!newLightState && !oldAlarmState) {
                            newLightState = true;
                            logMessages.add("Turning on light");
                        }
                    }
                }
            }
        }

        if (newAlarmState!=null) {
            // alarm control state changed
            if (newAlarmState != oldAlarmState) {

                // set the alarm
                if (newAlarmState) {

                    logMessages.add("Enabling alarm");

                    // alarm to be set and user left and door open
                    if (!newProximityState && newDoorState) {
                        newDoorState = false;
                        logMessages.add("Closing the door because alarm enabled and user not home");
                    }

                } else if (!newAlarmState) { // attempt to disable alarm

                    if (!newProximityState) { // && newDoorState
                        newAlarmState = true;

                        logMessages.add("Cannot disable the alarm, user not home");
                    } else if (givenPassCode != null) {
                        if (givenPassCode.compareTo(alarmPassCode) < 0) {
                            logMessages.add("Cannot disable alarm, invalid passcode given");
                            newAlarmState = true;

                        } else {
                            logMessages.add("Correct passcode entered; Disabled alarm");
                        }
                    }
                }

                if (oldAlarmState != null) {
                    if (oldAlarmState && !newAlarmState) { // alarm disabled
                        newAlarmActiveState = false;
                    }
                }
            }
        }

        // determine if the alarm should sound. There are two cases
        // 1. the door is opened when no one is home
        // 2. the house is suddenly occupied
        try {
            if ((newAlarmState && newDoorState && (!newProximityState && oldProximityState)) || (newAlarmState && !newDoorState && (newProximityState && !oldProximityState))) {
                logMessages.add("Activating alarm");
                newAlarmActiveState = true;
            }
        } catch (NullPointerException npe) {
            // Not enough information to evaluate alarm
            logMessages.add("Warning: Not enough information to evaluate alarm");
        }


        // manage the HVAC control
        if (newHvacSetting!=null) {
            if (newHvacSetting.equals("Heater")) {

                if (oldChillerOnState!=null) {
                    if (oldChillerOnState==true) {
                        logMessages.add("Turning off air conditioner");
                    }
                }
                newChillerOnState = false; // can't run AC
                newHumidifierState = false; // can't run dehumidifier with heater

                // Is the heater needed?
                if (tempReading!=null) {
                    if (tempReading < targetTempSetting) {
                        if (oldHeaterOnState != null) {
                            if (!oldHeaterOnState) {
                                logMessages.add("Turning on heater, target temperature = " + targetTempSetting
                                        + "F, current temperature = " + tempReading + "F");
                                newHeaterOnState = true;
                            }
                            // Heater already on
                        } else {
                            // heater not yet on
                            logMessages.add("Turning on heater target temperature = " + targetTempSetting
                                    + "F, current temperature = " + tempReading + "F");
                            newHeaterOnState = true;
                        }
                    } else {
                        // Heater not needed
                        if (oldHeaterOnState != null) {
                            if (oldHeaterOnState) {
                                logMessages.add("Turning off heater target temperature = " + targetTempSetting
                                        + "F, current temperature = " + tempReading + "F");
                            }
                        }
                        newHeaterOnState = false;
                    }
                }
            }

            if (newHvacSetting.equals("Chiller")) {

                if (oldHeaterOnState!=null) {
                    if (oldHeaterOnState==true) {
                        logMessages.add("Turning off heater");
                    }
                }
                newHeaterOnState = false; // can't run heater

                // AC needed
                if (tempReading!=null) {
                    if (tempReading > targetTempSetting) {
                        if (oldChillerOnState != null) {
                            if (!oldChillerOnState) {
                                logMessages.add("Turning on air conditioner target temperature = "
                                        + targetTempSetting + "F, current temperature = " + tempReading + "F");
                                newChillerOnState = true;
                            } // AC already on
                        } else {
                            logMessages.add("Turning on air conditioner target temperature = "
                                    + targetTempSetting + "F, current temperature = " + tempReading + "F");
                            newChillerOnState = true;

                        }
                    }
                    // AC not needed
                    else {
                        if (oldChillerOnState != null) {
                            if (oldChillerOnState) {
                                logMessages.add("Turning off air conditioner target temperature = "
                                        + targetTempSetting + "F, current temperature = " + tempReading + "F");
                            }
                        }
                        newChillerOnState = false;
                    }
                }
            }
        }
        if (newHumidifierState!=null) {
            if (newHumidifierState != oldHumidifierState) {
                if (newHumidifierState && newHvacSetting.equals("Chiller")) {
                    logMessages.add("Enabled Dehumidifier");
                } else {
                    logMessages.add("Disabled Dehumidifier");
                    newHumidifierState = false;
                }
            }
        }
    }

    /**
     * Get the connected state
     *
     * @return true if connected to the house, false otherwise
     */
    public Boolean isConnected() {
        if (connMgr == null) {
            return false;
        }
        return connMgr.isConnected();
    }
}