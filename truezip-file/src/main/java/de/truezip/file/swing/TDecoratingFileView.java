/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.file.swing;

import java.io.File;
import javax.annotation.Nullable;
import javax.swing.Icon;
import javax.swing.filechooser.FileView;

/**
 * A decorator for a file view.
 *
 * @author Christian Schlichtherle
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
        return String.format("%s[delegate=%s]",
                getClass().getName(),
                delegate);
    }
}
