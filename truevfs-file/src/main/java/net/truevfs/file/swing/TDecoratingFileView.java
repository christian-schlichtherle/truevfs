/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.truevfs.file.swing;

import java.io.File;
import java.util.Objects;
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
    protected final FileView fv;

    /**
     * Constructs a new decorating file view.
     *
     * @param fv The file view to decorate.
     */
    protected TDecoratingFileView(final FileView fv) {
        this.fv = Objects.requireNonNull(fv);
    }

    @Override
    public @Nullable String getName(File f) {
        return fv.getName(f);
    }

    @Override
    public @Nullable String getDescription(File f) {
        return fv.getDescription(f);
    }

    @Override
    public @Nullable String getTypeDescription(File f) {
        return fv.getTypeDescription(f);
    }

    @Override
    public @Nullable Icon getIcon(File f) {
        return fv.getIcon(f);
    }

    @Override
    public @Nullable Boolean isTraversable(File f) {
        return fv.isTraversable(f);
    }

    /**
     * Returns a string representation of this object for debugging and logging
     * purposes.
     */
    @Override
    public String toString() {
        return String.format("%s[fv=%s]",
                getClass().getName(),
                fv);
    }
}
