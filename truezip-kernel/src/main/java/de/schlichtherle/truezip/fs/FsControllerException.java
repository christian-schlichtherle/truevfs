/*
 * Copyright 2004-2012 Schlichtherle IT Services
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package de.schlichtherle.truezip.fs;

import java.io.IOException;
import net.jcip.annotations.Immutable;

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
 * because they are never thrown to client applications (this would be a bug).
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
 * @see     FsDecoratingController
 * @since   TrueZIP 7.5 (renamed from {@code FsException} and changed super
 *          type from {@code IOException} to {@code RuntimeException})
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@Immutable
@SuppressWarnings("serial") // serializing an exception for a temporary event is nonsense!
public abstract class FsControllerException extends IOException {
    FsControllerException() {
    }

    FsControllerException(Throwable cause) {
        super(cause);
    }
}
