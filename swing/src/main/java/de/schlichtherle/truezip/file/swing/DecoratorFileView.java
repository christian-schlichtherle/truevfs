/*
 * Copyright (C) 2006-2010 Schlichtherle IT Services
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

import edu.umd.cs.findbugs.annotations.Nullable;
import java.io.File;
import javax.swing.filechooser.FileView;
import javax.swing.Icon;

/**
 * An abstract decorator which simply delegates to another instance of
 * {@link FileView}.
 * If the delegate is {@code null}, all methods in this class return
 * {@code null}.
 * Subclasses should override individual methods to implement some specific
 * behaviour.
 * Note that this class does not override any methods in {@link Object}
 * - this should be done in subclasses.
 *
 * @author Christian Schlichtherle
 * @version $Id$
 */
abstract class DecoratorFileView extends FileView {

    /** The nullable decorated file view. */
    @Nullable
    protected final FileView delegate;

    /**
     * Constructs a new decorating file view.
     *
     * @param delegate The file view to be decorated - may be {@code null}.
     */
    protected DecoratorFileView(final FileView delegate) {
        this.delegate = delegate;
    }

    @Override
    public String getDescription(File f) {
        return delegate != null ? delegate.getDescription(f) : null;
    }

    @Override
    public Icon getIcon(File f) {
        return delegate != null ? delegate.getIcon(f) : null;
    }

    @Override
    public String getName(File f) {
        return delegate != null ? delegate.getName(f) : null;
    }

    @Override
    public String getTypeDescription(File f) {
        return delegate != null ? delegate.getTypeDescription(f) : null;
    }

    @Override
    public Boolean isTraversable(File f) {
        return delegate != null ? delegate.isTraversable(f) : Boolean.FALSE;
    }
}
