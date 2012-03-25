/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.kernel.util;

/**
 * Holds a static boolean telling us if the JSE&nbsp;7 API is availabe to this
 * JVM.
 * This is convenient if an a-priori test is required.
 * 
 * @author Christian Schlichtherle
 */
public final class JSE7 {

    /**
     * {@code true} if and only if the JSE&nbsp;7 API is available to this
     * JVM.
     */
    public static final boolean AVAILABLE;
    static {
        boolean available = true;
        try {
            Class.forName("java.nio.file.Path");
        } catch (ClassNotFoundException notAvailable) {
            available = false;
        }
        AVAILABLE = available;
    }

    private JSE7() { // make lint shut up!
    }
}