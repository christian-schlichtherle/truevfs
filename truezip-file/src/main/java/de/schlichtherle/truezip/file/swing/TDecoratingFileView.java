/*
 * Copyright (C) 2006-2011 Schlichtherle IT Services
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.schlichtherle.truezip.file.swing;

import edu.umd.cs.findbugs.annotations.DefaultAnnotation;
import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import java.io.File;
import javax.swing.filechooser.FileView;
import javax.swing.Icon;

/**
 * A decorator for a file view.
 *
 * @author Christian Schlichtherle
 * @version $Id$
 */
@DefaultAnnotation(NonNull.class)
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
