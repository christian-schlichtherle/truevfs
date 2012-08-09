/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.ext.jmx;

/**
 * Implements the colleague role of the mediator pattern for instrumentation.
 * 
 * @author Christian Schlichtherle
 */
public interface JmxColleague {

    /**
     * Starts this colleague.
     * This hook gets called by the {@link JmxMediator} once after construction
     * of this object in order to enable it to perform startup operations.
     */
    void start();
}
