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
public class JmxSyncMediator extends JmxMediator {

    public JmxSyncMediator(String subject) {
        super(subject, 1);
    }

    @Override
    protected JmxStatistics<?> newStatistics(int offset) {
        return new JmxSyncStatistics(this, offset);
    }

    @Override
    void rotateStatistics() {
    }
}
