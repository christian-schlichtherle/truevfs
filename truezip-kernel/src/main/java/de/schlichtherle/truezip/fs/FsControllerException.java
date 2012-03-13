/*
 * Copyright (C) 2004-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.fs;

import java.io.IOException;
import javax.annotation.CheckForNull;
import javax.annotation.concurrent.Immutable;

/**
 * Indicates an <em>internal</em> exception in a decorator chain of
 * {@linkplain FsController file system controllers}.
 * <p>
 * File system controllers are typically arranged in a decorator and
 * chain-of-responsibility pattern.
 * Unfortunately, some aspects of file system management make it necessary to
 * use exceptions for non-local flow control in these chains.
 * For example:
 * <ul>
 * <li>
 * A file system controller may throw an instance of a sub-class to indicate a
 * false positive archive file.
 * Some other file system controller further up the chain is then expected to
 * catch this exception in order to route the file system operation to the
 * parent file system controller instead.
 * <li>
 * Yet another file system controller may detect a potential dead lock and
 * throw an instance of a sub-class to indicate this.
 * Some other file system controller further up the chain is then expected to
 * catch this exception in order to pause the current thread for a small random
 * time interval and retry the operation.
 * <li>
 * This pattern continues for automatic synchronization etc...
 * </ul>
 * 
 * <h3>How This Applies To You</h3>
 * <p>
 * If you are only using a file system controller, for example by calling
 * {@link FsManager#getController(FsMountPoint, FsCompositeDriver)}, then you
 * don't need to be concerned about file system controller exceptions at all
 * because they shall never pass to client applications (this would be a bug).
 * <p>
 * As an implementor of a file system controller however, for example when
 * writing a custom controller for an archive file system driver by extending
 * this class,  you need to be aware that you may receive file system
 * controller exceptions whenever you call a method on the decorated file
 * system controller.
 * Unless you have special requirements, you don't need to catch such an
 * exception.
 * Just make sure to always leave your controller in a consistent state, for
 * example by protecting all access to the decorated controller with a
 * try-finally block:
 * <pre>{@code
 * \@Override
 * public FsEntry getEntry(FsEntryName name) throws IOException {
 *     prepareMyResources();
 *     try {
 *         return delegate.getEntry(); // may throw FsControllerException, too!
 *     } finally {
 *         cleanUpMyResources();
 *     }
 * }
 * }</pre>
 * 
 * @see    FsDecoratingController
 * @since  TrueZIP 7.5 (renamed from {@code FsException} and changed super
 *         type from {@code IOException} to {@code RuntimeException})
 * @author Christian Schlichtherle
 */
@Immutable
@SuppressWarnings("serial") // serializing an exception for a temporary event is nonsense!
public abstract class FsControllerException extends IOException {

    /**
     * Controls whether or not instances of this class have a regular stack
     * trace or an empty stack trace.
     * If and only if the system property with the name
     * {@code de.schlichtherle.fs.FsControllerException.traceable} is set to
     * {@code true} (case is ignored), then instances of this class will have a
     * regular stack trace, otherwise their stack trace will be empty.
     */
    protected static final boolean TRACEABLE = Boolean
            .getBoolean(FsControllerException.class.getName() + ".traceable");

    private static final StackTraceElement[]
            EMPTY_STACK = new StackTraceElement[0];

    FsControllerException() { }

    FsControllerException(  final FsModel model,
                            final @CheckForNull String message, 
                            final @CheckForNull Throwable cause) {
        super(  TRACEABLE
                    ? null == message
                        ? model.getMountPoint().toString()
                        : model.getMountPoint() + " (" + message + ')'
                    : null,
                cause);
    }

    /**
     * Fills in an empty stack trace for optimum performance.
     * <em>Warning:</em> This method is called from the constructors in the
     * super class {@code Throwable}!
     * 
     * @return {@code this}
     * @see <a href="http://blogs.oracle.com/jrose/entry/longjumps_considered_inexpensive">Longjumps Considered Inexpensive</a>
     */
    @Override
    public FsControllerException fillInStackTrace() {
        if (TRACEABLE)
            super.fillInStackTrace();
        else
            super.setStackTrace(EMPTY_STACK);
        return this;
    }
}
