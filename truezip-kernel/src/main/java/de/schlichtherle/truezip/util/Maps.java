/*
 * Copyright (C) 2011 Schlichtherle IT Services
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package de.schlichtherle.truezip.util;

/**
 * Provides static utility methods for maps.
 * 
 * @since   TrueZIP 7.3
 * @author  Christian Schlichtherle
 * @version $Id$
 */
public class Maps {

    /**
     * Returns the initial capacity for a hash table with a load factor of 0.75.
     * 
     * @param size the number of entries to accommodate space for.
     * @return The initial capacity for a hash table with a load factor of 0.75.
     */
    public static int initialCapacity(int size) {
        return size * 4 / 3 + 1;
    }
}
