/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package global.namespace.truevfs.ext.pacemaker;

import global.namespace.truevfs.comp.jmx.JmxManagerView;

import javax.management.MBeanAttributeInfo;
import javax.management.MBeanInfo;

/**
 * A view for a {@link PaceManager}.
 * <p>
 * This class is thread-safe.
 *
 * @author Christian Schlichtherle
 */
final class PaceManagerView extends JmxManagerView<PaceManager> implements PaceManagerMXBean {

    public PaceManagerView(PaceManager manager) {
        super(manager, PaceManagerMXBean.class);
    }

    @Override
    protected String getDescription(MBeanInfo info) {
        return "A pace maker for the file system manager.";
    }

    @Override
    protected String getDescription(MBeanAttributeInfo info) {
        return "MaximumFileSystemsMounted".equals(info.getName())
                ? "The maximum number of mounted file systems."
                : super.getDescription(info);
    }

    @Override
    public int getMaximumFileSystemsMounted() {
        return manager.getMaximumSize();
    }

    @Override
    public void setMaximumFileSystemsMounted(int maxMounted) {
        manager.setMaximumSize(maxMounted);
    }
}
