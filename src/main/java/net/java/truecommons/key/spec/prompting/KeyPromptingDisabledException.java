/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truecommons.key.spec.prompting;

import net.java.truecommons.key.spec.PersistentUnknownKeyException;

import javax.annotation.Nullable;

/**
 * Thrown to indicate that the retrieval of the key to open or create a
 * protected resource has been disabled.
 * This is normally caused by the client application, but will also happen
 * if the JVM is running in headless mode.
 *
 * @author  Christian Schlichtherle
 */
public class KeyPromptingDisabledException extends PersistentUnknownKeyException  {

    private static final long serialVersionUID = 0;

    public KeyPromptingDisabledException() {
        super("Key prompting has been disabled!");
    }

    public KeyPromptingDisabledException(@Nullable Throwable cause) {
        super("Key prompting has been disabled!", cause);
    }
}
