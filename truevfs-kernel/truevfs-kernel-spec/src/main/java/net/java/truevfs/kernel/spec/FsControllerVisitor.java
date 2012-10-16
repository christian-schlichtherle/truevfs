/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.kernel.spec;

import java.io.IOException;
import javax.annotation.concurrent.ThreadSafe;
import net.java.truecommons.shed.AbstractExceptionBuilder;
import net.java.truecommons.shed.Filter;

/**
 * A visitor for {@linkplain FsController file system controllers}.
 * <p>
 * Implementations should be thread-safe.
 *
 * @param  <X> the type of I/O exceptions to be thrown by {@link #visit}.
 * @author Christian Schlichtherle
 */
@ThreadSafe
public interface FsControllerVisitor<X extends IOException> {

    /**
     * Returns a filter for file system controllers.
     * 
     * @return A filter for file system controllers.
     */
    Filter<? super FsController> filter();

    /**
     * Returns an abstract exception builder for gathering any exceptions
     * resulting from {@link #visit visit()}ing the file system controllers
     * which have been accepted by the {@link #filter}.
     * 
     * @return An abstract exception builder for gathering any exceptions
     * resulting from {@link #visit visit()}ing the file system controllers
     * which have been accepted by the {@link #filter}.
     */
    AbstractExceptionBuilder<X, X> builder();

    /**
     * Visits the given file system controller.
     * 
     * @param  controller the file system controller to visit.
     * @throws IOException at the discretion of the implementation. 
     */
    void visit(FsController controller) throws X;
}
