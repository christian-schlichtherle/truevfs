/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package de.schlichtherle.truezip.util;

/**
 * Holds a static boolean telling us if the JSE&nbsp;7 API is availabe to this
 * JVM.
 * This is convenient if an a-priori test is required.
 * 
 * @author Christian Schlichtherle
 * @version $Id$
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
