/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.fs.inst.jmx;

import de.schlichtherle.truezip.fs.FsModel;
import de.schlichtherle.truezip.fs.inst.InstrumentingModel;
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