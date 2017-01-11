package edu.cmu.iot;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * The main window for the IoT house
 *
 * Project: LG Exec Ed Program
 * Copyright: Copyright (c) 2015 Jeffrey S. Gennari
 * Versions:
 * 1.0 November 2015 - initial version
 */
public class IoTControlWindow extends JFrame {
    private JButton connectButton;
    private JPanel alarmPanel;
    private JPanel HvacPanel;
    private JPanel proximityPanel;
    private JRadioButton celciusRadioButton;
    private JRadioButton farenheitRadioButton;
    private JPanel hvacModePanel;
    private JRadioButton heaterRadioButton;
    private JRadioButton airConditionerRadioButton;
    private JTextArea logText;
    private JMenu fileMenu;
    private JMenu logMenu;
    private JSlider tempControlSlider;
    private JPanel hvacPanel;
    private JPanel humidPanel;
    private JPanel mainPanel;
    private JTextField houseAddressText;
    private JLabel houseAddressLabel;
    private JSplitPane mainSplitPane;
    private JPanel connPanel;
    private JLabel tempControlLabel;
    private JPanel homeAwayPanel;
    private JPanel lightPanel;
    private JCheckBox lightCheckbox;
    private JScrollPane logScrollBar;
    private JLabel hvacModeLabel;
    private JButton alarmTimeoutButton;
    private JPanel controlPanel;
    private JRadioButton alarmEnabledRadioButton;
    private JRadioButton alarmDisabledRadioButton;
    private JLabel doorStatusLabel;
    private JLabel tempLabel;
    private JLabel humidLabel;
    private JLabel humidControlLabel;
    private JButton openCloseDoorButton;
    private JCheckBox humidifierCheckbox;
    private JLabel alarmActiveLabel;
    private JLabel alarmStatusLabel;
    private JLabel hvacLabel;
    private JLabel homeAwayLabel;
    private ButtonGroup alarmButtonGroup;
    private ButtonGroup hvacButtonGroup;
    private ButtonGroup tempUnitButtonGroup;
    private JMenuBar mainMenuBar;
    private LoginWindow loginWindow;
    private IoTControlManager controller;

    // state control variables for the GUI
    private Boolean humidifierControlState, lightControlState, alarmControlState,
            doorControlState, proximityControlState, runHeater, runChiller,
            alarmActive;
    private Integer tempSettingControl;

    private Boolean isEnabled;

    /** the current state of the GUI */
    private Hashtable<String, Object> state;

    /**
     * Constructor for the main control Window
     *
     * @param settingsDir the settings directory
     * @throws java.awt.HeadlessException
     */
    public IoTControlWindow(String settingsDir) throws HeadlessException {
        super("Tartan Platform");

        $$$setupUI$$$();
        setContentPane(mainPanel);
        pack();
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        controller = new IoTControlManager(this, settingsDir);
        controller.loadSettings();

        mainSplitPane.setResizeWeight(.7d);

        Vector<UserLoginInfo> users = controller.loadUsers();

        loginWindow = new LoginWindow(this, users);
        loginWindow.setVisible(true);
        loginWindow.setModal(true);

        humidifierControlState = null;
        lightControlState = null;
        alarmControlState = null;
        doorControlState = null;
        proximityControlState = null;
        runHeater = null;
        runChiller = null;
        alarmActive = null;
        tempSettingControl = null;

        state = new Hashtable<String, Object>();

        if (loginWindow.isSucceeded()) {
            if (true) {
                setVisible(true);
                updateLog("Welcome " + loginWindow.getUsername() + "\n");
            }
        } else {
            System.exit(1);
        }

        alarmTimeoutButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {

                Hashtable<String, Object> settings = controller.getUserSettings();
                Integer alarmDelay = null;
                if (settings.containsKey(IoTValues.ALARM_DELAY)) {
                    alarmDelay = (Integer) settings.get(IoTValues.ALARM_DELAY);
                }

                String newDelay = JOptionPane.showInputDialog(IoTControlWindow.this, "Enter new alarm delay", alarmDelay);

                Hashtable<String, Object> stateUpdate = new Hashtable<String, Object>();
                stateUpdate.put(IoTValues.ALARM_DELAY, Integer.parseInt(newDelay));

                updateAlarmDelay(stateUpdate);

            }
        });

        connectButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {

                if (controller.isConnected() == false) {
                    if (controller.connectToHouse(houseAddressText.getText())) {

                        homeAwayLabel.setText("---");
                        enableControls();


                        updateLog("Connected to " + houseAddressText.getText() + "\n\n");

                        connectButton.setText("Disconnect");
                        // Initial control values.
                        tempSettingControl = 70;
                        humidifierControlState = false;
                        alarmActive = false;
                        runHeater = true;
                        runChiller = false;

                        Hashtable<String, Object> initState = new Hashtable<String, Object>();

                        initState.put(IoTValues.TARGET_TEMP, tempSettingControl);
                        initState.put(IoTValues.HUMIDIFIER_STATE, humidifierControlState);
                        initState.put(IoTValues.ALARM_ACTIVE, alarmActive);
                        if (runHeater) {
                            initState.put(IoTValues.HVAC_MODE, "Heater");
                        }
                        if (runChiller) {
                            initState.put(IoTValues.HVAC_MODE, "Chiller");
                        }

                        updateState(initState);

                    } else {
                        updateLog("Could not connect to " + houseAddressText.getText());
                    }

                } else {
                    controller.disconnectFromHouse();
                    updateLog("Disconnected from " + houseAddressText.getText());

                    homeAwayLabel.setText("---");
                    disableControls();

                    connectButton.setText("Connect");
                }
            }
        });

        lightCheckbox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JCheckBox cb = (JCheckBox) e.getSource();
                if (cb.isSelected()) {
                    System.out.println("Light On");
                    lightControlState = true;
                } else {
                    System.out.println("Light Off");
                    lightControlState = false;
                }
                requestStateUpdate();
            }
        });

        tempControlSlider.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                JSlider source = (JSlider) e.getSource();
                if (!source.getValueIsAdjusting()) {
                    int temp = (int) source.getValue();
                    System.out.println(temp);

                    tempSettingControl = temp;
                    requestStateUpdate();
                }
            }
        });

        ActionListener listener = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JRadioButton rb = (JRadioButton) e.getSource();
                if (rb.getText().equals("Enabled")) {
                    alarmControlState = true;
                } else {
                    if (alarmControlState) {
                        // disable the alarm
                        String passCode = JOptionPane.showInputDialog(IoTControlWindow.this, "Enter Alarm Passcode");
                        state.put(IoTValues.PASSCODE, passCode);
                    }
                    alarmControlState = false;

                }
                requestStateUpdate();
            }
        };
        alarmEnabledRadioButton.addActionListener(listener);
        alarmDisabledRadioButton.addActionListener(listener);


        // Handle HVAC mode
        ActionListener listener1 = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JRadioButton rb = (JRadioButton) e.getSource();
                if (rb.getText().equals("Heater")) {
                    runHeater = true;
                } else {
                    runChiller = true;
                }
                requestStateUpdate();
            }
        };
        heaterRadioButton.addActionListener(listener1);
        airConditionerRadioButton.addActionListener(listener1);

        ActionListener listener2 = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JRadioButton rb = (JRadioButton) e.getSource();
                if (rb.getText().equals("Celcius")) {
                    tempLabel.setText("Temperature: " + String.valueOf(tempSettingControl) + "C");
                } else {
                    tempLabel.setText("Temperature: " + String.valueOf(tempSettingControl) + "F");
                }
            }
        };
        celciusRadioButton.addActionListener(listener2);
        farenheitRadioButton.addActionListener(listener2);

        humidifierCheckbox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JCheckBox cb = (JCheckBox) e.getSource();
                if (cb.isSelected()) {
                    System.out.println("Humidifier On");
                    humidifierControlState = true;
                } else {
                    System.out.println("Humidifier Off");
                    humidifierControlState = false;
                }
                requestStateUpdate();
            }
        });
        openCloseDoorButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // alternate
                if (doorControlState != null) {
                    doorControlState = !doorControlState;
                }
                requestStateUpdate();
            }
        });
    }

    public Boolean getIsEnabled() {
        return isEnabled;
    }

    /**
     * Enable the control panel
     */
    public void enableControls() {

        isEnabled = true;
        lightCheckbox.setEnabled(true);
        openCloseDoorButton.setEnabled(true);
        alarmEnabledRadioButton.setEnabled(true);
        alarmDisabledRadioButton.setEnabled(true);
        tempControlSlider.setEnabled(true);
        heaterRadioButton.setEnabled(true);
        airConditionerRadioButton.setEnabled(true);
        celciusRadioButton.setEnabled(true);
        farenheitRadioButton.setEnabled(true);
        humidifierCheckbox.setEnabled(true);
    }

    /**
     * Disable the control panel
     */
    public void disableControls() {

        isEnabled = false;
        lightCheckbox.setEnabled(false);
        openCloseDoorButton.setEnabled(false);
        alarmEnabledRadioButton.setEnabled(false);
        alarmDisabledRadioButton.setEnabled(false);
        tempControlSlider.setEnabled(false);
        heaterRadioButton.setEnabled(false);
        airConditionerRadioButton.setEnabled(false);
        celciusRadioButton.setEnabled(false);
        farenheitRadioButton.setEnabled(false);
        humidifierCheckbox.setEnabled(false);
    }

    /**
     * Create the main menu
     */
    private void createMainMenu() {

        mainMenuBar = new JMenuBar();

        JMenuItem addUserItem = new JMenuItem("Add User");
        JMenuItem exitItem = new JMenuItem("Exit");

        fileMenu = new JMenu("File");
        fileMenu.add(addUserItem);
        fileMenu.add(exitItem);

        exitItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                dispatchEvent(new WindowEvent(IoTControlWindow.this, WindowEvent.WINDOW_CLOSING));
            }
        });

        JMenuItem openLogItem = new JMenuItem("Open Log");
        JMenuItem exportLogItem = new JMenuItem("Export Log");
        JMenuItem clearLogItem = new JMenuItem("Clear Log");

        clearLogItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                logText.setText("");
            }
        });

        addUserItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                NewUserDialog nud = new NewUserDialog(controller, IoTControlWindow.this);
                nud.setVisible(true);
            }
        });

        exportLogItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JFileChooser chooser = new JFileChooser();
                chooser.setDialogTitle("Save log file");

                int userSelection = chooser.showSaveDialog(IoTControlWindow.this);

                if (userSelection == JFileChooser.APPROVE_OPTION) {
                    File fileToSave = chooser.getSelectedFile();
                    try {
                        FileWriter fw = new FileWriter(chooser.getSelectedFile() + ".log");
                        fw.write(logText.getText());
                        fw.close();
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
            }
        });

        logMenu = new JMenu("Log");
        logMenu.add(openLogItem);
        logMenu.add(exportLogItem);
        logMenu.add(clearLogItem);

        mainMenuBar.add(fileMenu);
        mainMenuBar.add(logMenu);

        setJMenuBar(mainMenuBar);
    }

    /**
     * Handle changes initiated from the control panel
     */
    public void requestStateUpdate() {

        if (tempSettingControl != null) {
            state.put(IoTValues.TARGET_TEMP, tempSettingControl);
        }
        if (humidifierControlState != null) {
            state.put(IoTValues.HUMIDIFIER_STATE, humidifierControlState);
        }
        if (doorControlState != null) {
            state.put(IoTValues.DOOR_STATE, doorControlState);
        }
        if (lightControlState != null) {
            state.put(IoTValues.LIGHT_STATE, lightControlState);
        }
        if (proximityControlState != null) {
            state.put(IoTValues.PROXIMITY_STATE, proximityControlState);
        }
        if (alarmControlState != null) {
            state.put(IoTValues.ALARM_STATE, alarmControlState);
        }

        if (runHeater != null) {
            if (runHeater) {
                state.put(IoTValues.HVAC_MODE, "Heater");
            }
        }

        if (runChiller != null) {
            if (runChiller) {
                state.put(IoTValues.HVAC_MODE, "Chiller");
            }
        }

        // have the controller evaluate the new state
        controller.processStateUpdate(state);

        // reset the state
        state.clear();
    }

    /**
     * Update user settings
     *
     * @param settings the new settings
     */
    public void updateAlarmDelay(Hashtable<String, Object> settings) {
        controller.updateSettings(settings);
        updateLog("Updated settings\n");
    }

    /**
     * update the state of the control panel GUI
     *
     * @param newState the new state
     */
    public void updateState(Hashtable<String, Object> newState) {

        Set<String> keys = newState.keySet();
        for (String key : keys) {

            if (key.equals(IoTValues.HUMIDITY_READING)) {
                Integer humidityReading = (Integer) newState.get(key);
                humidLabel.setText("Humidity: " + String.valueOf(humidityReading) + "%");
            } else if (key.equals(IoTValues.TEMP_READING)) {
                Integer tempReading = (Integer) newState.get(key);
                if (farenheitRadioButton.isSelected()) {

                    tempLabel.setText("Temperature: " + String.valueOf(tempReading) + "F");
                    farenheitRadioButton.setSelected(true);
                    celciusRadioButton.setSelected(false);
                } else {

                    Integer celTempReading = new Double(((tempReading - 32) * 5) / 9).intValue();

                    tempLabel.setText("Temperature: " + String.valueOf(celTempReading) + "C");
                    farenheitRadioButton.setSelected(false);
                    celciusRadioButton.setSelected(true);
                }
            } else if (key.equals(IoTValues.HUMIDIFIER_STATE)) {
                humidifierControlState = (Boolean) newState.get(key);
                humidifierCheckbox.setSelected(humidifierControlState);

            } else if (key.equals(IoTValues.DOOR_STATE)) {
                Boolean newDoorState = (Boolean) newState.get(key);
                if (newDoorState) {
                    doorStatusLabel.setText("Door Status: OPEN");
                    doorControlState = true;
                } else {
                    doorStatusLabel.setText("Door Status: CLOSED");
                    doorControlState = false;
                }
            } else if (key.equals(IoTValues.LIGHT_STATE)) {
                Boolean newLightState = (Boolean) newState.get(key);
                if (newLightState) {
                    lightCheckbox.setSelected(true);
                    lightControlState = true;
                } else {
                    lightCheckbox.setSelected(false);
                    lightControlState = false;
                }
            } else if (key.equals(IoTValues.PROXIMITY_STATE)) {
                Boolean newProximityState = (Boolean) newState.get(key);

                if (newProximityState) {
                    homeAwayLabel.setText("Home is OCCUPIED");
                    proximityControlState = true;
                } else {
                    homeAwayLabel.setText("Home is VACANT");
                    proximityControlState = false;
                }
            } else if (key.equals(IoTValues.ALARM_STATE)) {
                Boolean newAlarmState = (Boolean) newState.get(key);

                if (newAlarmState) {
                    alarmEnabledRadioButton.setSelected(true);
                    alarmDisabledRadioButton.setSelected(false);
                    alarmControlState = true;
                } else {
                    alarmEnabledRadioButton.setSelected(false);
                    alarmDisabledRadioButton.setSelected(true);
                    alarmControlState = false;
                }
            } else if (key.equals(IoTValues.ALARM_ACTIVE)) {
                Boolean newAlarmActive = (Boolean) newState.get(key);
                if (newAlarmActive) {
                    alarmActiveLabel.setText("ALARM");
                    alarmActiveLabel.setForeground(Color.RED);
                    alarmActive = true;
                } else {
                    alarmActiveLabel.setText("Status: Normal");
                    alarmActiveLabel.setForeground(Color.BLACK);
                    alarmActive = false;
                }
            } else if (key.equals(IoTValues.HVAC_MODE)) {

                String hvacMode = (String) newState.get(key);

                if (hvacMode.equals("Heater")) {
                    heaterRadioButton.setSelected(true);
                    airConditionerRadioButton.setSelected(false);

                    Boolean newHeaterState = (Boolean) newState.get(IoTValues.HEATER_STATE);
                    if (newHeaterState != null) {
                        if (newHeaterState) {
                            runHeater = true;
                            runChiller = false;
                            hvacLabel.setText("HVAC Status: On");
                        } else {
                            hvacLabel.setText("HVAC Status: Off");
                            runHeater = false;
                        }
                    }
                } else if (hvacMode.equals("Chiller")) {
                    heaterRadioButton.setSelected(false);
                    airConditionerRadioButton.setSelected(true);
                    Boolean newChillerState = (Boolean) newState.get(IoTValues.CHILLER_STATE);
                    if (newChillerState != null) {
                        if (newChillerState) {
                            runChiller = true;
                            runHeater = false;
                            hvacLabel.setText("HVAC Status: On");
                        } else {
                            hvacLabel.setText("HVAC Status: Off");
                            runChiller = false;
                        }
                    }
                }
            }
        }
    }

    /**
     * Update the log with multiple events
     *
     * @param log the list of log events
     */
    public void updateLog(Vector<String> log) {
        for (String msg : log) {
            updateLog(msg);
        }
    }

    /**
     * Add a log entry
     *
     * @param logEntry the new log entry
     */
    public void updateLog(String logEntry) {
        Long timeStamp = System.currentTimeMillis();
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd,yyyy HH:mm");
        logText.append("[" + sdf.format(new Date(timeStamp)) + "]: " + logEntry + "\n");
        logText.setCaretPosition(logText.getDocument().getLength());
    }

    /**
     * GUI setup
     */
    private void createUIComponents() {

        createMainMenu();
        createHvacPanel();
        pack();
    }

    /**
     * Set up the slider
     */
    private void createHvacPanel() {

        tempControlSlider = new JSlider(JSlider.HORIZONTAL, 50, 80, 70);
        tempControlSlider.setLabelTable(tempControlSlider.createStandardLabels(5));
        tempControlSlider.setPaintLabels(true);
    }

    /**
     * Method generated by IntelliJ IDEA GUI Designer
     * >>> IMPORTANT!! <<<
     * DO NOT edit this method OR call it in your code!
     *
     * @noinspection ALL
     */
    private void $$$setupUI$$$() {
        createUIComponents();
        mainPanel = new JPanel();
        mainPanel.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(1, 6, new Insets(0, 0, 0, 0), -1, -1));
        mainPanel.setPreferredSize(new Dimension(1000, 319));
        mainSplitPane = new JSplitPane();
        mainSplitPane.setDividerLocation(400);
        mainPanel.add(mainSplitPane, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 5, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_BOTH, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, null, new Dimension(250, 200), null, 0, false));
        mainSplitPane.setBorder(BorderFactory.createTitledBorder(""));
        connPanel = new JPanel();
        connPanel.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(3, 1, new Insets(0, 0, 0, 0), -1, -1));
        connPanel.setEnabled(false);
        mainSplitPane.setLeftComponent(connPanel);
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(2, 2, new Insets(0, 0, 0, 0), -1, -1));
        connPanel.add(panel1, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_BOTH, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        houseAddressText = new JTextField();
        panel1.add(houseAddressText, new com.intellij.uiDesigner.core.GridConstraints(1, 1, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        houseAddressLabel = new JLabel();
        houseAddressLabel.setText("House Address:");
        panel1.add(houseAddressLabel, new com.intellij.uiDesigner.core.GridConstraints(1, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        logScrollBar = new JScrollPane();
        panel1.add(logScrollBar, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 2, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_BOTH, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        logText = new JTextArea();
        logText.setEditable(false);
        logText.setEnabled(true);
        logScrollBar.setViewportView(logText);
        alarmTimeoutButton = new JButton();
        alarmTimeoutButton.setText("Set Away Timeout");
        connPanel.add(alarmTimeoutButton, new com.intellij.uiDesigner.core.GridConstraints(2, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        connectButton = new JButton();
        connectButton.setText("Connect");
        connPanel.add(connectButton, new com.intellij.uiDesigner.core.GridConstraints(1, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        controlPanel = new JPanel();
        controlPanel.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(2, 3, new Insets(0, 0, 0, 0), -1, -1));
        mainSplitPane.setRightComponent(controlPanel);
        proximityPanel = new JPanel();
        proximityPanel.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(2, 1, new Insets(0, 0, 0, 0), -1, -1));
        controlPanel.add(proximityPanel, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_BOTH, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        homeAwayPanel = new JPanel();
        homeAwayPanel.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(3, 2, new Insets(0, 1, 0, 0), -1, -1));
        proximityPanel.add(homeAwayPanel, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_BOTH, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        homeAwayPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.black), "Proximity"));
        doorStatusLabel = new JLabel();
        doorStatusLabel.setText("Door Status: ");
        homeAwayPanel.add(doorStatusLabel, new com.intellij.uiDesigner.core.GridConstraints(2, 0, 1, 2, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        openCloseDoorButton = new JButton();
        openCloseDoorButton.setEnabled(false);
        openCloseDoorButton.setText("Open/Close Door");
        homeAwayPanel.add(openCloseDoorButton, new com.intellij.uiDesigner.core.GridConstraints(1, 0, 1, 2, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        homeAwayLabel = new JLabel();
        homeAwayLabel.setText("---");
        homeAwayPanel.add(homeAwayLabel, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        lightPanel = new JPanel();
        lightPanel.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(2, 1, new Insets(0, 0, 0, 0), -1, -1));
        proximityPanel.add(lightPanel, new com.intellij.uiDesigner.core.GridConstraints(1, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_BOTH, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        lightPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(new Color(-16777216)), "Light"));
        lightCheckbox = new JCheckBox();
        lightCheckbox.setEnabled(false);
        lightCheckbox.setText("On/Off");
        lightPanel.add(lightCheckbox, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final com.intellij.uiDesigner.core.Spacer spacer1 = new com.intellij.uiDesigner.core.Spacer();
        lightPanel.add(spacer1, new com.intellij.uiDesigner.core.GridConstraints(1, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_VERTICAL, 1, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        final com.intellij.uiDesigner.core.Spacer spacer2 = new com.intellij.uiDesigner.core.Spacer();
        controlPanel.add(spacer2, new com.intellij.uiDesigner.core.GridConstraints(0, 1, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_VERTICAL, 1, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        alarmPanel = new JPanel();
        alarmPanel.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(1, 5, new Insets(0, 0, 0, 0), -1, -1));
        alarmPanel.setBackground(new Color(-1118482));
        controlPanel.add(alarmPanel, new com.intellij.uiDesigner.core.GridConstraints(1, 0, 1, 3, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_BOTH, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        alarmPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.black), "Alarm"));
        alarmStatusLabel = new JLabel();
        alarmStatusLabel.setText("Status:");
        alarmPanel.add(alarmStatusLabel, new com.intellij.uiDesigner.core.GridConstraints(0, 2, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final com.intellij.uiDesigner.core.Spacer spacer3 = new com.intellij.uiDesigner.core.Spacer();
        alarmPanel.add(spacer3, new com.intellij.uiDesigner.core.GridConstraints(0, 4, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        alarmActiveLabel = new JLabel();
        alarmActiveLabel.setText("");
        alarmPanel.add(alarmActiveLabel, new com.intellij.uiDesigner.core.GridConstraints(0, 3, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        alarmEnabledRadioButton = new JRadioButton();
        alarmEnabledRadioButton.setEnabled(false);
        alarmEnabledRadioButton.setText("Enabled");
        alarmPanel.add(alarmEnabledRadioButton, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        alarmDisabledRadioButton = new JRadioButton();
        alarmDisabledRadioButton.setEnabled(false);
        alarmDisabledRadioButton.setText("Disabled");
        alarmPanel.add(alarmDisabledRadioButton, new com.intellij.uiDesigner.core.GridConstraints(0, 1, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        HvacPanel = new JPanel();
        HvacPanel.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(4, 1, new Insets(0, 0, 0, 0), -1, -1));
        controlPanel.add(HvacPanel, new com.intellij.uiDesigner.core.GridConstraints(0, 2, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_BOTH, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        HvacPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(new Color(-16777216)), "HVAC"));
        hvacPanel = new JPanel();
        hvacPanel.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(2, 3, new Insets(0, 0, 0, 0), -1, -1));
        HvacPanel.add(hvacPanel, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_BOTH, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        hvacPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createRaisedBevelBorder(), null));
        farenheitRadioButton = new JRadioButton();
        farenheitRadioButton.setEnabled(false);
        farenheitRadioButton.setText("Farenheit");
        hvacPanel.add(farenheitRadioButton, new com.intellij.uiDesigner.core.GridConstraints(0, 2, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        celciusRadioButton = new JRadioButton();
        celciusRadioButton.setEnabled(false);
        celciusRadioButton.setText("Celcius");
        hvacPanel.add(celciusRadioButton, new com.intellij.uiDesigner.core.GridConstraints(0, 1, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        tempLabel = new JLabel();
        tempLabel.setText("Temperature: ");
        hvacPanel.add(tempLabel, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        tempControlLabel = new JLabel();
        tempControlLabel.setText("Temperature Control:");
        hvacPanel.add(tempControlLabel, new com.intellij.uiDesigner.core.GridConstraints(1, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        tempControlSlider.setEnabled(false);
        tempControlSlider.setMajorTickSpacing(5);
        tempControlSlider.setMinorTickSpacing(1);
        hvacPanel.add(tempControlSlider, new com.intellij.uiDesigner.core.GridConstraints(1, 1, 1, 2, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        humidPanel = new JPanel();
        humidPanel.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(2, 2, new Insets(0, 0, 0, 0), -1, -1));
        HvacPanel.add(humidPanel, new com.intellij.uiDesigner.core.GridConstraints(1, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_BOTH, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        humidPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createRaisedBevelBorder(), null, TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, new Font(humidPanel.getFont().getName(), humidPanel.getFont().getStyle(), humidPanel.getFont().getSize()), new Color(-16777216)));
        humidLabel = new JLabel();
        humidLabel.setText("Humidity: ");
        humidPanel.add(humidLabel, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 2, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        humidControlLabel = new JLabel();
        humidControlLabel.setEnabled(true);
        humidControlLabel.setText("Dehumidifier:");
        humidPanel.add(humidControlLabel, new com.intellij.uiDesigner.core.GridConstraints(1, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        humidifierCheckbox = new JCheckBox();
        humidifierCheckbox.setEnabled(false);
        humidifierCheckbox.setText("On/Off");
        humidPanel.add(humidifierCheckbox, new com.intellij.uiDesigner.core.GridConstraints(1, 1, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel2 = new JPanel();
        panel2.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(2, 2, new Insets(0, 0, 0, 0), -1, -1));
        HvacPanel.add(panel2, new com.intellij.uiDesigner.core.GridConstraints(3, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_BOTH, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        hvacModePanel = new JPanel();
        hvacModePanel.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(1, 4, new Insets(0, 0, 0, 0), -1, -1));
        panel2.add(hvacModePanel, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 2, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_BOTH, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        hvacModePanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createRaisedBevelBorder(), null));
        heaterRadioButton = new JRadioButton();
        heaterRadioButton.setEnabled(false);
        heaterRadioButton.setText("Heater");
        hvacModePanel.add(heaterRadioButton, new com.intellij.uiDesigner.core.GridConstraints(0, 1, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        hvacModeLabel = new JLabel();
        hvacModeLabel.setText("Mode:");
        hvacModePanel.add(hvacModeLabel, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        airConditionerRadioButton = new JRadioButton();
        airConditionerRadioButton.setEnabled(false);
        airConditionerRadioButton.setText("Air Conditioner");
        hvacModePanel.add(airConditionerRadioButton, new com.intellij.uiDesigner.core.GridConstraints(0, 2, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final com.intellij.uiDesigner.core.Spacer spacer4 = new com.intellij.uiDesigner.core.Spacer();
        hvacModePanel.add(spacer4, new com.intellij.uiDesigner.core.GridConstraints(0, 3, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        hvacLabel = new JLabel();
        hvacLabel.setText("HVAC Status: Off");
        HvacPanel.add(hvacLabel, new com.intellij.uiDesigner.core.GridConstraints(2, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final com.intellij.uiDesigner.core.Spacer spacer5 = new com.intellij.uiDesigner.core.Spacer();
        mainPanel.add(spacer5, new com.intellij.uiDesigner.core.GridConstraints(0, 5, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_VERTICAL, 1, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        tempUnitButtonGroup = new ButtonGroup();
        tempUnitButtonGroup.add(celciusRadioButton);
        tempUnitButtonGroup.add(farenheitRadioButton);
        hvacButtonGroup = new ButtonGroup();
        hvacButtonGroup.add(heaterRadioButton);
        hvacButtonGroup.add(airConditionerRadioButton);
        alarmButtonGroup = new ButtonGroup();
        alarmButtonGroup.add(alarmEnabledRadioButton);
        alarmButtonGroup.add(alarmDisabledRadioButton);
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return mainPanel;
    }
}
