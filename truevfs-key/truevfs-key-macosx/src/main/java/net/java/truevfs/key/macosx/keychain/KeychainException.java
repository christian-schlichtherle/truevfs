/*
 * Copyright (C) 2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.key.macosx.keychain;

import static net.java.truevfs.key.macosx.keychain.Security.*;

/**
 * Indicates an error when using Apple's Keychain Services API.
 *
 * @author Christian Schlichtherle
 */
public class KeychainException extends Exception {

    private static final long serialVersionUID = 0;

    private final int status;

    /**
     * Constructs a new keychain exception with the given error status code.
     *
     * @param status the error status code as defined by the Keychain API.
     */
    static KeychainException create(final int status) {
        switch (status) {
            case errSecDuplicateItem:   return new DuplicateItemException();
            default:                    return new KeychainException(status);
        }
    }

    KeychainException(final int status) { this.status = status; }

    /**
     * Returns the error status code from Apple's Keychain Services API.
     *
     * @see    <a href="https://developer.apple.com/library/mac/#documentation/security/Reference/keychainservices/Reference/reference.html">Mac Developer Library: Keychain Services Reference</a>
     */
    public int getStatus() { return status; }

    @Override
    public String getMessage() {
        return String.format("%s (%d)", message(status), status);
    }
}
