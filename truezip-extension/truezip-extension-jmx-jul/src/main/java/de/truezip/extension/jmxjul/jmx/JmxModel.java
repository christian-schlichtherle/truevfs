/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.extension.jmxjul.jmx;

import de.truezip.extension.jmxjul.InstrumentingModel;
import de.truezip.kernel.FsModel;
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
            model.setTouched(newTouched);
        } finally {
            if (newTouched) JmxModelView.  register(model);
            else            JmxModelView.unregister(model);
        }
    }
}