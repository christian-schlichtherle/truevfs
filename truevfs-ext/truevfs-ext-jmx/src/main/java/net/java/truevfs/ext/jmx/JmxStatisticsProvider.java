/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.ext.jmx;

import net.java.truevfs.ext.jmx.model.IoStatistics;

/**
 * @author Christian Schlichtherle
 */
public interface JmxStatisticsProvider {
    IoStatistics getStatistics();
}
