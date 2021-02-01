/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package global.namespace.truevfs.comp.jmx;

import global.namespace.truevfs.comp.inst.InstrumentingModel;
import global.namespace.truevfs.kernel.api.FsModel;

import javax.management.ObjectName;

/**
 * A controller for a {@linkplain FsModel file system model}.
 *
 * @param  <M> the type of the JMX mediator.
 * @author Christian Schlichtherle
 */
public class JmxModel<M extends JmxMediator<M>>
extends InstrumentingModel<M> implements JmxComponent {

    private final ObjectName objectName;

    public JmxModel(M mediator, FsModel model) {
        super(mediator, model);
        this.objectName = getObjectName();
    }

    private ObjectName getObjectName() {
        return mediator
                .nameBuilder(FsModel.class)
                .put("mountPoint", ObjectName.quote(model.getMountPoint().toHierarchicalUri().toString()))
                .get();
    }

    protected Object newView() { return new JmxModelView<>(model); }

    @Override
    public void activate() { }

    @Override
    public void setMounted(final boolean mounted) {
        if (model.isMounted() != mounted) {
            model.setMounted(mounted);
            if (mounted)
                mediator.register(objectName, newView());
            else
                mediator.deregister(objectName);
        }
    }
}
