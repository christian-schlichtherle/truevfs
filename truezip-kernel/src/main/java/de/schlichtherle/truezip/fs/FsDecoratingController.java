/*
 * Copyright 2004-2012 Schlichtherle IT Services
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package de.schlichtherle.truezip.fs;

import de.schlichtherle.truezip.entry.Entry;
import de.schlichtherle.truezip.entry.Entry.Access;
import de.schlichtherle.truezip.entry.Entry.Type;
import de.schlichtherle.truezip.socket.InputSocket;
import de.schlichtherle.truezip.socket.OutputSocket;
import de.schlichtherle.truezip.util.BitField;
import de.schlichtherle.truezip.util.ExceptionHandler;
import edu.umd.cs.findbugs.annotations.DefaultAnnotation;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.io.IOException;
import java.util.Map;
import javax.swing.Icon;
import net.jcip.annotations.ThreadSafe;

/**
 * An abstract decorator for a file system controller.
 *
 * <a name="FsControllerException"/><h3>File System Controller Exceptions</h3>
 * <p>
 * File system controllers are typically arranged in a decorator and
 * chain-of-responsibility pattern.
 * Unfortunately, some aspects of file system management make it necessary to
 * use exceptions for non-local flow control in these chains.
 * For example, a file system controller may throw an exception to indicate a
 * false positive federated (archive) file system.
 * Some other file system controller further up the chain is then expected to
 * catch this exception in order to route the file system operation to the
 * parent file system controller instead.
 * For this particular purpose, the exception type {@link FsControllerException}
 * has been created as a sub-class of {@link RuntimeException}.
 * <p>
 * If you are only using a file system controller, for example by calling
 * {@link FsManager#getController(FsMountPoint, FsCompositeDriver)}, then you
 * don't need to be concerned about file system controller exceptions at all
 * because they are never thrown to client applications (this would be a bug).
 * <p>
 * As an implementor of a file system controller however, for example when
 * writing a custom controller for an archive file system driver by extending
 * this class, then you need to be aware that you may receive file system
 * controller exceptions whenever you call a method on the decorated file
 * system controller.
 * Unless you have special requirements, you don't need to catch such an
 * exception.
 * Just make sure to always leave your controller in a consistent state, for
 * example by protecting all access to the decorated controller with a
 * try-finally block:
 * <pre>{@code
 *     prepareMyResources();
 *     try {
 *         return delegate.getEntry(); // may throw FsControllerException!
 *     } finally {
 *         cleanUpMyResources();
 *     }
 * }</pre>
 * 
 * <h3>Stack Traces</h3>
 * <p>
 * Because I/O operations can fail anytime, it's expected that users will be
 * frequently facing stack traces which include stack frames of many file
 * system decorator classes.
 * Now because of their complex chaining, it is generally recommended that any
 * implementations go the extra boilerplate mile to make nice readable stack
 * traces, for example by avoiding to use anonymous inner classes, declaring
 * methods package private which are called from member classes and even
 * overriding unchanged methods with exactly the same code as their super class
 * wherever practical.
 *
 * @param   <M> The type of the file system model.
 * @param   <C> The type of the decorated file system controller.
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@ThreadSafe
@DefaultAnnotation(NonNull.class)
public abstract class FsDecoratingController<
        M extends FsModel,
        C extends FsController<? extends M>>
extends FsModelController<M> {

    /** The decorated file system controller. */
    protected final C delegate;

    /**
     * Constructs a new decorating file system controller.
     *
     * @param controller the decorated file system controller.
     */
    protected FsDecoratingController(final C controller) {
        super(controller.getModel());
        this.delegate = controller;
    }

    @Override
    public FsController<?> getParent() {
        return delegate.getParent();
    }

    /**
     * {@inheritDoc}
     * 
     * @throws FsControllerException See <a href="#FsControllerException">File System Controller Exceptions</a>.
     */
    @Override
    @Deprecated
    public Icon getOpenIcon() throws IOException {
        return delegate.getOpenIcon();
    }

    /**
     * {@inheritDoc}
     * 
     * @throws FsControllerException See <a href="#FsControllerException">File System Controller Exceptions</a>.
     */
    @Override
    @Deprecated
    public Icon getClosedIcon() throws IOException {
        return delegate.getClosedIcon();
    }

    /**
     * {@inheritDoc}
     * 
     * @throws FsControllerException See <a href="#FsControllerException">File System Controller Exceptions</a>.
     */
    @Override
    public boolean isReadOnly() throws IOException {
        return delegate.isReadOnly();
    }

    /**
     * {@inheritDoc}
     * 
     * @throws FsControllerException See <a href="#FsControllerException">File System Controller Exceptions</a>.
     */
    @Override
    public FsEntry getEntry(FsEntryName name)
    throws IOException {
        return delegate.getEntry(name);
    }

    /**
     * {@inheritDoc}
     * 
     * @throws FsControllerException See <a href="#FsControllerException">File System Controller Exceptions</a>.
     */
    @Override
    public boolean isReadable(FsEntryName name) throws IOException {
        return delegate.isReadable(name);
    }

    /**
     * {@inheritDoc}
     * 
     * @throws FsControllerException See <a href="#FsControllerException">File System Controller Exceptions</a>.
     */
    @Override
    public boolean isWritable(FsEntryName name) throws IOException {
        return delegate.isWritable(name);
    }

    /**
     * {@inheritDoc}
     * 
     * @throws FsControllerException See <a href="#FsControllerException">File System Controller Exceptions</a>.
     */
    @Override
    public boolean isExecutable(FsEntryName name) throws IOException {
        return delegate.isExecutable(name);
    }

    /**
     * {@inheritDoc}
     * 
     * @throws FsControllerException See <a href="#FsControllerException">File System Controller Exceptions</a>.
     */
    @Override
    public void setReadOnly(FsEntryName name) throws IOException {
        delegate.setReadOnly(name);
    }

    /**
     * {@inheritDoc}
     * 
     * @throws FsControllerException See <a href="#FsControllerException">File System Controller Exceptions</a>.
     */
    @Override
    public boolean setTime(
            FsEntryName name,
            Map<Access, Long> times,
            BitField<FsOutputOption> options)
    throws IOException {
        return delegate.setTime(name, times, options);
    }

    /**
     * {@inheritDoc}
     * 
     * @throws FsControllerException See <a href="#FsControllerException">File System Controller Exceptions</a>.
     */
    @Override
    public boolean setTime(
            FsEntryName name,
            BitField<Access> types,
            long value,
            BitField<FsOutputOption> options)
    throws IOException {
        return delegate.setTime(name, types, value, options);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Note that the returned input socket may throw
     * {@link FsControllerException}s from any of its methods which are
     * declared to throw an {@link IOException}, too.
     * 
     * @see <a href="#FsControllerException">File System Controller Exceptions</a>
     */
    @Override
    public InputSocket<?>
    getInputSocket( FsEntryName name,
                    BitField<FsInputOption> options) {
        return delegate.getInputSocket(name, options);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Note that the returned input socket may throw
     * {@link FsControllerException}s from any of its methods which are
     * declared to throw an {@link IOException}, too.
     * 
     * @see <a href="#FsControllerException">File System Controller Exceptions</a>
     */
    @Override
    public OutputSocket<?>
    getOutputSocket(    FsEntryName name,
                        BitField<FsOutputOption> options,
                        Entry template) {
        return delegate.getOutputSocket(name, options, template);
    }

    /**
     * {@inheritDoc}
     * 
     * @throws FsControllerException See <a href="#FsControllerException">File System Controller Exceptions</a>.
     */
    @Override
    public void
    mknod(  FsEntryName name,
            Type type,
            BitField<FsOutputOption> options,
            Entry template)
    throws IOException {
        delegate.mknod(name, type, options, template);
    }

    /**
     * {@inheritDoc}
     * 
     * @throws FsControllerException See <a href="#FsControllerException">File System Controller Exceptions</a>.
     */
    @Override
    public void unlink(FsEntryName name, BitField<FsOutputOption> options)
    throws IOException {
        delegate.unlink(name, options);
    }

    /**
     * {@inheritDoc}
     * 
     * @throws FsControllerException See <a href="#FsControllerException">File System Controller Exceptions</a>.
     */
    @Override
    public <X extends IOException> void
    sync(   BitField<FsSyncOption> options,
            ExceptionHandler<? super FsSyncException, X> handler)
    throws X {
        delegate.sync(options, handler);
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
