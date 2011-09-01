/*
 * Copyright (C) 2011 Schlichtherle IT Services
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package de.schlichtherle.truezip.fs;

/**
 * @author Christian Schlichtherle
 * @version $Id$
 */
public class DummyDriver extends FsDriver {

    @Override
    public FsController<?>
    newController(FsModel model, FsController<?> parent) {
        assert null == model.getParent()
                    ? null == parent
                    : model.getParent().equals(parent.getModel());
        return new DummyController<FsModel>(model, parent);
    }
}
