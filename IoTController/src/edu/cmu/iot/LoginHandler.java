package edu.cmu.iot;

import java.util.Vector;

/**
 * Validate username and password
 *
 * Project: LG Exec Ed Program
 * Copyright: Copyright (c) 2015 Jeffrey S. Gennari
 * Versions:
 * 1.0 November 2015 - initial version
 */
public class LoginHandler {

    /** the number of unsuccessful login attempts */
    private int times;

    /** the list of valid users */
    Vector<UserLoginInfo> validUsers;

    /**
     * The constructor for the login handler
     * @param vu list of valid users
     */
    public LoginHandler(Vector<UserLoginInfo> vu) {
        times = 0;
        validUsers = vu;
    }

    public void resetHandler() {
        times = 0;
    }

    /**
     * Authenticate the username and password
     * @param username the user name
     * @param password the password
     * @return true if authenticated, false otherwise
     * @throws LoginAttemptsExceededException
     */
    public Boolean authenticate(String username, String password) throws LoginAttemptsExceededException {

        if (times > 3) throw new LoginAttemptsExceededException();

        for (UserLoginInfo uli : validUsers) {
            String un = uli.getUserName();
            String pw = uli.getPassword();
            if (username.equals(un) && password.equals(pw)) {
                return true;
            }
        }
        // increment the invalid login count
        times++;
        return false;
    }
}

/**
 * Represents user login information
 */
class UserLoginInfo {

    /** the user credentials */
    private String userName, password;

    public UserLoginInfo(String userName, String password) {
        this.userName = userName;
        this.password = password;
    }

    /**
     *  Get the username
     * @return the user name
     */
    public String getUserName() {
        return userName;
    }

    /**
     * Set the username
     * @param userName
     */
    public void setUserName(String userName) {
        this.userName = userName;
    }

    /**
     * Get password
     * @return
     */
    public String getPassword() {
        return password;
    }

    /**
     * Set password
     * @param password
     */
    public void setPassword(String password) {
        this.password = password;
    }
}
