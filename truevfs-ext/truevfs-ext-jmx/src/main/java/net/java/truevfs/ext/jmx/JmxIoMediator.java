/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.ext.jmx;

import javax.annotation.concurrent.ThreadSafe;

/**
 * @author Christian Schlichtherle
 */
@ThreadSafe
public class JmxIoMediator extends JmxMediator {

    public JmxIoMediator(String subject) {
        super(subject);
    }

    @Override
    protected JmxStatistics<?> newStatistics(int offset) {
        return new JmxIoStatistics(this, offset);
    }
}
