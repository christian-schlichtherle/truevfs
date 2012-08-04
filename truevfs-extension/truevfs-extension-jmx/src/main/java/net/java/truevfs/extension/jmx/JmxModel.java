/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.extension.jmx;

import javax.annotation.concurrent.Immutable;
import net.java.truevfs.component.instrumentation.InstrumentingModel;
import net.java.truevfs.kernel.spec.FsModel;

/**
 * @author Christian Schlichtherle
 */
@Immutable
final class JmxModel extends InstrumentingModel<JmxDirector> {

    JmxModel(JmxDirector director, FsModel model) {
        super(director, model);
    }

    @Override
    public void setMounted(final boolean mounted) {
        try {
            model.setMounted(mounted);
        } finally {
            if (mounted) JmxModelView.register(model);
            else JmxModelView.unregister(model);
        }
    }
}
