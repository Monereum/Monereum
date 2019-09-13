package com.quorum.tessera.passwords;

import com.quorum.tessera.io.SystemAdapter;

import java.util.Objects;

/**
 * Allows a password to be read from the System console if it is available
 * otherwise reads from the provided input stream
 */
public interface PasswordReader {

    /**
     * Read a password from either system console or the given stream
     * @return the read password, which may be empty if no password is given
     */
    String readPasswordFromConsole();

    /**
     * Requests user input for a password until two matching consecutive entries are made
     *
     * @return The password that the user has input
     */
    default String requestUserPassword() {

        for(;;) {

            sys().out().println("Enter a password if you want to lock the private key or leave blank");
            final String password = this.readPasswordFromConsole();

            sys().out().println("Please re-enter the password (or lack of) to confirm");
            final String passwordCheck = this.readPasswordFromConsole();

            if(Objects.equals(password, passwordCheck)) {
                return password;
            } else {
                sys().out().println("Passwords did not match, try again...");
            }

        }

    }
    
    static SystemAdapter sys() {
        return SystemAdapter.INSTANCE;
    }

}
