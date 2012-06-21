/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.truevfs.extension.jmxjul.jmx;

import javax.annotation.concurrent.Immutable;
import net.truevfs.extension.jmxjul.InstrumentingModel;
import net.truevfs.kernel.spec.FsModel;

/**
 * @author Christian Schlichtherle
 */
@Immutable
final class JmxModel extends InstrumentingModel {

    JmxModel(JmxDirector director, FsModel model) {
        super(director, model);
    }

    @Override
    public void setTouched(final boolean newTouched) {
        try {
            model.setTouched(newTouched);
        } finally {
            if (newTouched) JmxModelView.  register(model);
            else            JmxModelView.unregister(model);
        }
    }
}
