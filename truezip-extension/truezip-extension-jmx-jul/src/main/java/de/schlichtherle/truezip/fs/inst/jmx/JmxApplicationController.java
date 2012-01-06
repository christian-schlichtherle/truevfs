/*
 * Copyright 2004-2012 Schlichtherle IT Services
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package de.schlichtherle.truezip.fs.inst.jmx;

import de.schlichtherle.truezip.fs.FsController;
import de.schlichtherle.truezip.fs.FsModel;

/**
 * @author  Christian Schlichtherle
 * @version $Id$
 */
final class JmxApplicationController<M extends FsModel>
extends JmxController<M> {

    JmxApplicationController(
            FsController<? extends M> controller,
            JmxDirector director) {
        super(controller, director);
    }

    @Override
    JmxIOStatistics getIOStatistics() {
        return ((JmxDirector) director).getApplicationIOStatistics();
    }
}
