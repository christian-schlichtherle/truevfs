/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.ext.jmx;

import java.io.IOException;
import javax.annotation.CheckForNull;
import javax.annotation.concurrent.ThreadSafe;
import javax.management.ObjectName;
import net.java.truevfs.comp.inst.InstrumentingModel;
import net.java.truevfs.comp.jmx.JmxController;
import net.java.truevfs.comp.jmx.JmxModelMXBean;
import static net.java.truevfs.comp.jmx.JmxUtils.deregister;
import static net.java.truevfs.comp.jmx.JmxUtils.register;
import net.java.truevfs.kernel.spec.*;
import net.java.truevfs.kernel.spec.sl.FsDriverMapLocator;
import net.java.truevfs.kernel.spec.sl.FsManagerLocator;

/**
 * @author Christian Schlichtherle
 */
@ThreadSafe
public class JmxModelController
extends InstrumentingModel<JmxDirector> implements JmxController {
    private static final FsMetaDriver
            DRIVER = new FsSimpleMetaDriver(FsDriverMapLocator.SINGLETON);

    public JmxModelController(JmxDirector director, FsModel model) {
        super(director, model);
    }

    @Override
    public void init() {
    }

    protected JmxModelMXBean newView() {
        return new JmxModelView(this);
    }

    private ObjectName name() {
        return director.nameBuilder(FsModel.class)
                .put("mountPoint", ObjectName.quote(
                    getMountPoint().toHierarchicalUri().toString()))
                .get();
    }

    @Override
    public void setMounted(final boolean mounted) {
        try {
            model.setMounted(mounted);
        } finally {
            if (mounted) register(newView(), name());
            else deregister(name());
        }
    }

    public @CheckForNull FsNode stat() {
        final FsMountPoint mmp = model.getMountPoint();
        final FsMountPoint pmp = mmp.getParent();
        final FsMountPoint mp;
        final FsNodeName en;
        if (null != pmp) {
            mp = pmp;
            en = mmp.getPath().getNodeName();
        } else {
            mp = mmp;
            en = FsNodeName.ROOT;
        }
        try {
            return FsManagerLocator
                    .SINGLETON
                    .get()
                    .controller(DRIVER, mp)
                    .stat(FsAccessOptions.NONE, en);
        } catch (IOException ex) {
            return null;
        }
    }

    public void sync() throws FsSyncException {
        new FsFilteringManager( model.getMountPoint(),
                                FsManagerLocator.SINGLETON.get())
                .sync(FsSyncOptions.NONE);
    }
}
