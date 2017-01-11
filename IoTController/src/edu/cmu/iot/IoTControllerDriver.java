package edu.cmu.iot;

/**
 * Driver for the IoT house control panel
 *
 * Project: LG Exec Ed Program
 * Copyright: Copyright (c) 2015 Jeffrey S. Gennari
 * Versions:
 * 1.0 November 2015 - initial version
 */
public class IoTControllerDriver {

    private IoTControlWindow mainWindow;

    public IoTControllerDriver(String settingsDir) {

        mainWindow = new IoTControlWindow(settingsDir);

    }

    public static void main(String[] args) {

        IoTControllerDriver controller = new IoTControllerDriver(args[0]);
    }
}