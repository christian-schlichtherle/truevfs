/*
 * Copyright 2004-2012 Schlichtherle IT Services
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package de.schlichtherle.truezip.file.swing;

import javax.annotation.Nullable;
import java.io.File;
import javax.swing.Icon;
import javax.swing.filechooser.FileView;

/**
 * A decorator for a file view.
 *
 * @author Christian Schlichtherle
 * @version $Id$
 */
public abstract class TDecoratingFileView extends FileView {

    /** The decorated file view. */
    protected final FileView delegate;

    /**
     * Constructs a new decorating file view.
     *
     * @param fileView The file view to decorate.
     */
    protected TDecoratingFileView(final FileView fileView) {
        if (null == fileView)
            throw new NullPointerException();
        this.delegate = fileView;
    }

    @Override
    public @Nullable String getName(File f) {
        return delegate.getName(f);
    }

    @Override
    public @Nullable String getDescription(File f) {
        return delegate.getDescription(f);
    }

    @Override
    public @Nullable String getTypeDescription(File f) {
        return delegate.getTypeDescription(f);
    }

    @Override
    public @Nullable Icon getIcon(File f) {
        return delegate.getIcon(f);
    }

    @Override
    public @Nullable Boolean isTraversable(File f) {
        return delegate.isTraversable(f);
    }

    /**
     * Returns a string representation of this object for debugging and logging
     * purposes.
     */
    @Override
    public String toString() {
        return new StringBuilder()
                .append(getClass().getName())
                .append("[delegate=")
                .append(delegate)
                .append(']')
                .toString();
    }
}
