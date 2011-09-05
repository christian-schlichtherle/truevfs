/*
 * Copyright 2011 Schlichtherle IT Services
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package de.schlichtherle.truezip.fs.http;

import de.schlichtherle.truezip.fs.FsController;
import de.schlichtherle.truezip.fs.FsDriver;
import de.schlichtherle.truezip.fs.FsModel;
import de.schlichtherle.truezip.socket.IOPool;
import de.schlichtherle.truezip.socket.IOPoolProvider;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.DefaultAnnotation;
import edu.umd.cs.findbugs.annotations.NonNull;
import net.jcip.annotations.Immutable;

/**
 * A file system driver for the HTTP(S) schemes.
 * 
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@Immutable
@DefaultAnnotation(NonNull.class)
public final class HttpDriver extends FsDriver {

    private final IOPoolProvider provider;

    public HttpDriver(final IOPoolProvider provider) {
        if (null == provider)
            throw new NullPointerException();
        this.provider = provider;
    }

    IOPool<?> getPool() {
        return provider.get();
    }

    @Override
    public FsController<?>
    newController(FsModel model, @CheckForNull FsController<?> parent) {
        assert null == model.getParent()
                ? null == parent
                : model.getParent().equals(parent.getModel());
        if (null != parent)
            throw new IllegalArgumentException();
        return new HttpController(this, model);
    }
}
