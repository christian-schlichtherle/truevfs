/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.comp.jmx;

import javax.annotation.concurrent.ThreadSafe;
import javax.management.ObjectName;
import net.java.truevfs.comp.inst.InstrumentingModel;
import static net.java.truevfs.comp.jmx.JmxUtils.deregister;
import static net.java.truevfs.comp.jmx.JmxUtils.register;
import net.java.truevfs.kernel.spec.FsModel;

/**
 * A controller for a {@linkplain FsModel file system model}.
 * 
 * @param  <M> the type of the JMX mediator.
 * @author Christian Schlichtherle
 */
@ThreadSafe
public class JmxModel<M extends JmxMediator<M>>
extends InstrumentingModel<M> implements JmxColleague {

    private final ObjectName name;

    public JmxModel(M mediator, FsModel model) {
        super(mediator, model);
        this.name = name();
    }

    private ObjectName name() {
        return mediator.nameBuilder(FsModel.class)
                .put("mountPoint", ObjectName.quote(
                    getMountPoint().toHierarchicalUri().toString()))
                .get();
    }

    protected Object newView() { return new JmxModelView<>(model); }

    @Override
    public void start() { }

    @Override
    public void setMounted(final boolean mounted) {
        try {
            model.setMounted(mounted);
        } finally {
            if (mounted) register(name, newView());
            else deregister(name);
        }
    }
}