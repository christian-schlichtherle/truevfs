/*
 * Copyright (C) 2011 Schlichtherle IT Services
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package de.schlichtherle.truezip.util;

import java.nio.file.Path;

/**
 * Holds a static boolean telling us if the JSE 7 API is availabe for this JVM.
 * 
 * @author Christian Schlichtherle
 * @version $Id$
 */
public final class JSE7 {

    /**
     * {@code true} if and only if the JSE 7 API is available for this JVM.
     */
    public static final boolean AVAILABLE;
    static {
        boolean available;
        try {
            Path.class.getName();
            available = true;
        } catch (NoClassDefFoundError notAvailable) {
            available = false;
        }
        AVAILABLE = available;
    }

    private JSE7() { // make lint shut up!
    }
}
