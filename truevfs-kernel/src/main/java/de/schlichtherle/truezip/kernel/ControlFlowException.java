/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.kernel;

import de.truezip.kernel.FsCompositeDriver;
import de.truezip.kernel.FsController;
import de.truezip.kernel.FsManager;
import de.truezip.kernel.FsMountPoint;
import javax.annotation.concurrent.ThreadSafe;

/**
 * Indicates a condition which requires non-local control flow within a
 * decorator chain of {@linkplain FsController file system controllers}.
 * <p>
 * File system controllers are typically arranged in a decorator and
 * chain-of-responsibility pattern.
 * Unfortunately, some aspects of file system management make it necessary to
 * use exceptions for non-local control flow in these chains.
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
 *         return delegate.getEntry(); // may throw ControlFlowException, too!
 *     } finally {
 *         cleanUpMyResources();
 *     }
 * }
 * }</pre>
 * 
 * @author Christian Schlichtherle
 */
@ThreadSafe
@SuppressWarnings("serial") // serializing a control flow exception is nonsense!
public abstract class ControlFlowException extends RuntimeException {

    /**
     * Controls whether or not instances of this class have a regular stack
     * trace or an empty stack trace.
     * If and only if the system property with the name
     * {@code de.schlichtherle.truezip.kernel.ControlFlowException.traceable}
     * is set to {@code true} (whereby case is ignored), then instances of this
     * class will have a regular stack trace, otherwise their stack trace will
     * be empty.
     * This should be set to {@code true} for debugging purposes only.
     */
    static final boolean TRACEABLE = Boolean
            .getBoolean(ControlFlowException.class.getName() + ".traceable");

    public ControlFlowException() {
        super(null, null, TRACEABLE, TRACEABLE);
    }

    public ControlFlowException(final Throwable cause) {
        super(null, cause, TRACEABLE, TRACEABLE);
    }
}
