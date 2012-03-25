/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.extension.jmxjul.jmx;

import de.truezip.kernel.fs.FsModel;
import de.truezip.extension.jmxjul.InstrumentingModel;
import javax.annotation.concurrent.Immutable;

/**
 * @author  Christian Schlichtherle
 */
@Immutable
final class JmxModel extends InstrumentingModel {

    JmxModel(FsModel model, JmxDirector director) {
        super(model, director);
    }

    @Override
    public void setTouched(final boolean newTouched) {
        try {
            delegate.setTouched(newTouched);
        } finally {
            if (newTouched) JmxModelView.  register(delegate);
            else            JmxModelView.unregister(delegate);
        }
    }
}