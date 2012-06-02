/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.truevfs.extension.jmxjul.jmx;

import javax.annotation.concurrent.Immutable;
import net.truevfs.extension.jmxjul.InstrumentingModel;
import net.truevfs.kernel.FsModel;

/**
 * @author Christian Schlichtherle
 */
@Immutable
final class JmxModel extends InstrumentingModel {

    JmxModel(FsModel model, JmxDirector director) {
        super(model, director);
    }

    @Override
    protected void setTouched(final boolean newTouched) {
        try {
            super.setTouched(newTouched);
        } finally {
            if (newTouched) JmxModelView.  register(model);
            else            JmxModelView.unregister(model);
        }
    }
}
