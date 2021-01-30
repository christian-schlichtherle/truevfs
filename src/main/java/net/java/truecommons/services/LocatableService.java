/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truecommons.services;

import javax.annotation.concurrent.Immutable;

/**
 * A locatable service.
 *
 * @see    Locator
 * @author Christian Schlichtherle
 */
@Immutable
public abstract class LocatableService {

    /**
     * Returns a priority to help {@link Locator}s to prioritize this object.
     * <p>
     * The implementation in the class {@link LocatableService} returns zero.
     *
     * @return A priority to help {@link Locator}s to prioritize this object.
     */
    public int getPriority() { return 0; }

    /**
     * Returns a string representation of this locatable object for debugging
     * and logging purposes.
     */
    @Override
    public String toString() {
        return String.format("%s[priority=%d]",
                getClass().getName(),
                getPriority());
    }
}
