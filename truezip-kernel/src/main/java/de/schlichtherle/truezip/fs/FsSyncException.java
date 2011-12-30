/*
 * Copyright (C) 2011 Schlichtherle IT Services
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package de.schlichtherle.truezip.fs;

import de.schlichtherle.truezip.io.SequentialIOException;
import edu.umd.cs.findbugs.annotations.DefaultAnnotation;
import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import java.io.IOException;
import net.jcip.annotations.ThreadSafe;

/**
 * Indicates an exceptional condition when synchronizing the changes in a
 * federated file system to its parent file system.
 * Unless this is an instance of the sub-class {@link FsSyncWarningException},
 * an exception of this class implies that some or all
 * of the data of the federated file system has been lost.
 *
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@ThreadSafe
@DefaultAnnotation(NonNull.class)
public class FsSyncException extends SequentialIOException {
    private static final long serialVersionUID = 4893219420357369739L;

    /**
     * This constructor is for exclusive use by {@link FsSyncExceptionBuilder}.
     *
     * @deprecated This method is only public in order to allow reflective
     *             access - do <em>not</em> call it directly!
     */
    @Deprecated
    public FsSyncException(String message) {
        super(message);
    }

    public FsSyncException(FsModel model, @Nullable IOException cause) {
        super(model.getMountPoint().toString(), cause);
    }

    FsSyncException(FsModel model, @Nullable IOException cause, int priority) {
        super(model.getMountPoint().toString(), cause, priority);
    }

    @Override
    public @Nullable IOException getCause() {
        return (IOException) super.getCause();
    }

    @Override
    @edu.umd.cs.findbugs.annotations.SuppressWarnings("BC_UNCONFIRMED_CAST")
    public final FsSyncException initCause(final @Nullable Throwable cause) {
        //assert super.getCause() instanceof IOException;
        super.initCause((IOException) cause);
        return this;
    }
}
