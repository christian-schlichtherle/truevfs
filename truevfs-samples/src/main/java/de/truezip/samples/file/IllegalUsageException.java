/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.samples.file;

/**
 * Indicates illegal application parameters.
 * 
 * @author Christian Schlichtherle
 */
public class IllegalUsageException extends Exception {
    private static final long serialVersionUID = 1985623981423542464L;

    public IllegalUsageException(String msg) {
        super(msg);
    }
}