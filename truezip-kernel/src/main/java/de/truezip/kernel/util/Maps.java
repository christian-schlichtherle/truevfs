/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.kernel.util;

import javax.annotation.concurrent.Immutable;

/**
 * Static utility methods for maps.
 * 
 * @since   TrueZIP 7.3
 * @author  Christian Schlichtherle
 */
@Immutable
public final class Maps {

    /**
     * The number of entries which should be additionally accomodatable by a
     * hash map with a load factor of 75% before resizing it, which is {@value}.
     * When a new hash map gets created, this constant should get used in order
     * to compute the initial capacity or overhead for additional entries.
     * 
     * @see   #initialCapacity(int)
     * @since TrueZIP 8
     */
    public static final int OVERHEAD_SIZE = (64 - 1) * 3 / 4; // consider 75% load factor

    /**
     * Returns the initial capacity for a hash table with a load factor of 75%.
     * 
     * @param  size the number of entries to accommodate space for.
     * @return The initial capacity for a hash table with a load factor of 75%.
     * @see    #OVERHEAD_SIZE
     */
    public static int initialCapacity(final int size) {
        return size * 4 / 3 + 1;
    }

    /* Can't touch this - hammer time! */
    private Maps() { }
}
