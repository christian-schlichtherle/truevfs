/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.truevfs.kernel.spec.spi;

import net.truevfs.kernel.spec.util.UniqueObject;

/**
 * Defines common properties of service providers.
 * 
 * @author Christian Schlichtherle
 */
public class ServiceProvider extends UniqueObject {

    /**
     * Returns a priority to help service locators to prioritize the services
     * provided by this object.
     * The greater number wins!
     * <p>
     * The implementation in the class {@link ServiceProvider} returns zero.
     * 
     * @return A priority to help service locators to prioritize the services
     *         provided by this object.
     */
    public int getPriority() {
        return 0;
    }

    /**
     * Returns a string representation of this object for debugging and logging
     * purposes.
     */
    @Override
    public String toString() {
        return String.format("%s[priority=%d]",
                getClass().getName(),
                getPriority());
    }
}
