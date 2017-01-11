package edu.cmu.iot;

import java.io.*;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Hashtable;

/**
 * A connection to an IoT-enabled house. This class handles the network connection to the house
 *
 * Project: LG Exec Ed Program
 * Copyright: 2015 Jeffrey S. Gennari
 * Versions:
 * 1.0 November 2015 - initial version
 */
public class IoTConnection {
    private Boolean isConnected = false;

    /** connection settings */
    private String address = null;

    private final Integer PORT = 5050; // the default port for the house

    /** The connection is private so it can be controlled */
    private static IoTConnection connection = null;
    private Socket houseSocket=null;
    private BufferedWriter out=null;
    private BufferedReader in = null;

    private IoTConnection() { }

    /**
     * Get the house address
     * @return the address
     */
    public String getAddress() {
        return address;
    }

    /**
     * Get an existing connection, or make a new one
     * @param addr the house address
     * @return the established connection or null
     */
    public static IoTConnection getConnection(String addr) {
        if (addr!=null) {
            if (connection != null) {
                if (connection.isConnected()) {
                    if (connection.getAddress().equals(addr)) {
                        return connection; // already connected to this house
                    } else {
                        connection.disconnect(); // connect to new house
                        connection.connect(addr);
                    }
                } else {
                    connection.connect(addr); // connect to house
                }
            } else {
                // first connection
                connection = new IoTConnection();
                connection.connect(addr);
            }
        }
        // invalid address - return current connection
        if (connection.isConnected()) {
            return connection;
        }
        return null;
    }

    /**
     * Get connection state
     * @return true if connected, false otherwise
     */
    public Boolean isConnected() {
        return isConnected;
    }

    /**
     * Send a message to the house and get a response
     * @param msg the message to send
     * @return the response
     */
    public String sendMessageToHouse(String msg) {
        try {

            out.write(msg, 0, msg.length());
            out.flush();

            return in.readLine();

        } catch (IOException ioe) {
            //ioe.printStackTrace();
        }
        return null;
    }

    /**
     * Disconnect from the house
     */
    public void disconnect() {
        if (houseSocket!=null) {
            if (houseSocket.isConnected()) {
                try {
                    houseSocket.close();
                } catch (IOException e) {

                }
            }
        }
        isConnected = false;
    }

    /**
     * Connect to the house
     * @param addr the address of the house
     * @return true if connection successful, false otherwise
     */
    private Boolean connect(String addr) {
        address = addr;

        try {
            houseSocket = new Socket(address, PORT);

            out = new BufferedWriter(new OutputStreamWriter(houseSocket.getOutputStream()));
            in = new BufferedReader(new InputStreamReader( houseSocket.getInputStream()));

        } catch (UnknownHostException uhe) {
            System.out.println("Unknown host: " + address);
            return false;
        } catch (IOException ioe){
            return false;
        }
        isConnected = true;
        return true;
    }

}
