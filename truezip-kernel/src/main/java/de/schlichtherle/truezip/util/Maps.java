/*
 * Copyright (C) 2004-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.util;

import javax.annotation.concurrent.Immutable;

/**
 * Static utility methods for maps.
 * 
 * @since   TrueZIP 7.3
 * @author  Christian Schlichtherle
 */
@Immutable
public class Maps {

    /* Can't touch this - hammer time! */
    private Maps() { }

    /**
     * Returns the initial capacity for a hash table with a load factor of 0.75.
     * 
     * @param size the number of entries to accommodate space for.
     * @return The initial capacity for a hash table with a load factor of 0.75.
     */
    public static int initialCapacity(final int size) {
        return size * 4 / 3 + 1;
    }
}
