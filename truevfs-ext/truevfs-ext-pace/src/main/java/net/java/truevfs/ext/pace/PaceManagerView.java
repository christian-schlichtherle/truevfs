/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.ext.pace;

import javax.annotation.concurrent.ThreadSafe;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanInfo;
import net.java.truevfs.comp.jmx.JmxManagerView;

/**
 * A view for a {@linkplain PaceManager pace manager}.
 *
 * @author Christian Schlichtherle
 */
@ThreadSafe
final class PaceManagerView
extends JmxManagerView<PaceManager> implements PaceManagerMXBean {

    PaceManagerView(PaceManager manager) {
        super(PaceManagerMXBean.class, manager);
    }

    @Override
    protected String getDescription(MBeanInfo info) {
        return "A pace manager";
    }

    @Override
    protected String getDescription(final MBeanAttributeInfo info) {
        switch (info.getName()) {
        case "MaximumFileSystemsMounted":
            return "The maximum number of mounted file systems.";
        default:
            return super.getDescription(info);
        }
    }

    @Override
    public int getMaximumFileSystemsMounted() {
        return manager.getMaximumFileSystemsMounted();
    }

    @Override
    public void setMaximumFileSystemsMounted(int max) {
        manager.setMaximumFileSystemsMounted(max);
    }
}
