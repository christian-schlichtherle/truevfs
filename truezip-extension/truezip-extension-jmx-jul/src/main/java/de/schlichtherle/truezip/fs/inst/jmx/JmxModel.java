/*
 * Copyright 2004-2012 Schlichtherle IT Services
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package de.schlichtherle.truezip.fs.inst.jmx;

import de.schlichtherle.truezip.fs.FsModel;
import de.schlichtherle.truezip.fs.inst.InstrumentingModel;
import javax.annotation.concurrent.Immutable;

/**
 * @author  Christian Schlichtherle
 * @version $Id$
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
