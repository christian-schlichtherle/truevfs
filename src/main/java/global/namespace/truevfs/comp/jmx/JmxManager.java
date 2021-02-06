/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package global.namespace.truevfs.comp.jmx;

import global.namespace.truevfs.comp.inst.InstrumentingManager;
import global.namespace.truevfs.kernel.api.FsManager;

import javax.management.ObjectName;

/**
 * A controller for a {@linkplain FsManager file system manager}.
 *
 * @param  <M> the type of the JMX mediator.
 * @author Christian Schlichtherle
 */
public class JmxManager<M extends JmxMediator<M>>
extends InstrumentingManager<M> implements JmxComponent {

    public JmxManager(M mediator, FsManager manager) {
        super(mediator, manager);
    }

    private ObjectName getObjectName() {
        return mediator.nameBuilder(FsManager.class).get();
    }

    protected Object newView() { return new JmxManagerView<>(manager); }

    @Override
    public void activate() { mediator.register(getObjectName(), newView()); }
}
