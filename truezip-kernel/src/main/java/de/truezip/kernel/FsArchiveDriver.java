/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.kernel;

import static de.truezip.kernel.FsEntryName.SEPARATOR;
import static de.truezip.kernel.FsEntryName.SEPARATOR_CHAR;
import de.truezip.kernel.cio.Entry.Type;
import static de.truezip.kernel.cio.Entry.Type.DIRECTORY;
import de.truezip.kernel.cio.*;
import static de.truezip.kernel.io.Paths.cutTrailingSeparators;
import de.truezip.kernel.util.BitField;
import edu.umd.cs.findbugs.annotations.CreatesObligation;
import java.io.IOException;
import java.nio.charset.Charset;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.annotation.WillNotClose;
import javax.annotation.concurrent.Immutable;

/**
 * An abstract factory for components required for accessing archive files.
 * Implementations of this abstract base class are used to access specific
 * archive formats like ZIP, TAR et al.
 * <p>
 * Subclasses must be thread-safe and should be immutable!
 *
 * @param  <E> the type of the archive entries.
 * @author Christian Schlichtherle
 */
@Immutable
public abstract class FsArchiveDriver<E extends FsArchiveEntry>
extends FsDriver {

    /**
     * Returns the character set to use for encoding character based meta data
     * such as entry names or file comments to binary data when writing
     * an archive file.
     * Depending on the archive file format, this may get used for decoding
     * binary data when reading an archive file, too.
     * Subsequent calls must return the same object.
     *
     * @return The character set to use for encoding character based meta data
     *         such as entry names or file comments to binary data when writing
     *         an archive file.
     */
    public abstract Charset getCharset();

    /**
     * {@inheritDoc}
     * <p>
     * The implementation in the class {@link FsArchiveDriver} always returns
     * {@code true}.
     */
    @Override
    public final boolean isFederated() {
        return true;
    }

    /**
     * Returns the pool to use for allocating I/O buffers.
     * <p>
     * Multiple invocations may return different I/O buffer pools, so callers
     * may need to cache the result.
     *
     * @return The pool to use for allocating I/O buffers.
     */
    public abstract IOPool<?> getIOPool();

    /**
     * Returns {@code true} if and only if the archive files produced by this
     * archive driver may contain redundant archive entry contents.
     * If the return value is {@code true}, then an archive file may contain
     * redundant archive entry contents, but only the last contents written
     * should get used when reading the archive file.
     * 
     * @return The implementation in the class {@link FsArchiveDriver} returns
     *         {@code false} for backwards compatibility.
     */
    public boolean getRedundantContentSupport() {
        return false;
    }

    /**
     * Returns {@code true} if and only if the archive files produced by this
     * archive driver may contain redundant archive entry meta data.
     * If the return value is {@code true}, then an archive file may contain
     * redundant archive entry meta data, but only the last meta data written
     * should get used when reading the archive file.
     * This usually implies the existence of a central directory in the
     * resulting archive file.
     * 
     * @return The implementation in the class {@link FsArchiveDriver} returns
     *         {@code false} for backwards compatibility.
     */
    public boolean getRedundantMetaDataSupport() {
        return false;
    }

    /**
     * {@inheritDoc}
     * <p>
     * The implementation in the class {@link FsArchiveDriver} simply forwards
     * the call to the given file system manager after asserting that
     * {@code model.getParent().equals(parent.getModel())} is {@code true}.
     */
    @Override
    public final FsController<?> newController(
            FsManager manager,
            FsModel model,
            @Nonnull FsController<?> parent) {
        assert parent.getModel().equals(model.getParent());
        return manager.newController(this, model, parent);
    }

    /**
     * This hook can get overridden by archive drivers in order to decorate the
     * given file system controller with some other file system controller(s).
     * <p>
     * The implementation in the class {@link FsArchiveDriver} simply returns
     * the given controller.
     * 
     * @param  <M> the file system model used by the given controller.
     * @param  controller the file system controller to decorate or return.
     * @return The decorated file system controller or simply
     *         {@code controller}.
     */
    public <M extends FsModel> FsController<M> decorate(
            FsController<M> controller) {
        return controller;
    }

    /**
     * This method gets called by an archive controller in order to create a
     * new input service for its target archive file.
     * <p>
     * The implementation in {@link FsArchiveDriver} simply forwards the call
     * to {@link #getInputSocket}
     * and {@link #newInputService(FsModel, InputSocket)}.
     * 
     * @param  model the file system model for the target archive file.
     * @param  parent the controller for the parent file system with the target
     *         archive file.
     * @param  entry the entry name of the target archive file in the parent
     *         file system.
     * @param  options the options to use when accessing the target archive
     *         file.
     * @return A new input service for reading the target archive file.
     *         Note that this service does <em>not</em> need to be thread-safe!
     * @throws IOException on any I/O error.
     *         If the file system entry for the given model exists in the
     *         parent file system and is <em>not</em> a {@link Type#SPECIAL}
     *         type, then this exception is deemed to indicate a
     *         <em>persistent false positive</em> archive file and gets cached
     *         until the file system controller for the given model is
     *         {@linkplain FsController#sync(BitField, de.truezip.kernel.util.ExceptionHandler) synced}
     *         again.
     *         Otherwise, this exception is deemed to indicate a
     *         <em>transient false positive</em> archive file and does not
     *         get cached.
     */
    @CreatesObligation
    public InputService<E> newInputService(
            FsModel model,
            FsController<?> parent,
            FsEntryName entry,
            BitField<FsAccessOption> options)
    throws IOException {
        return newInputService(model,
                getInputSocket(parent, entry, options));
    }

    /**
     * Creates a new input service for reading the archive entries for the
     * given {@code model} from the given {@code input} socket's target.
     * 
     * @param  model the file system model.
     * @param  input the input socket for reading the contents of the
     *         archive file from its target.
     * @return A new input service.
     *         Note that this service does <em>not</em> need to be thread-safe!
     * @throws IOException on any I/O error.
     * @see    #newInputService(FsModel, FsController, FsEntryName, BitField) 
     */
    @CreatesObligation
    protected abstract InputService<E> newInputService(
            FsModel model,
            InputSocket<?> input)
    throws IOException;

    /**
     * This method gets called by an archive controller in order to create a
     * new output service for its target archive file.
     * <p>
     * The implementation in {@link FsArchiveDriver} simply forwards the call
     * to {@link #getOutputSocket}
     * and {@link #newOutputService(FsModel, InputService, OutputSocket)}.
     * 
     * @param  model the file system model for the target archive file.
     * @param  parent the controller for the parent file system with the target
     *         archive file.
     * @param  entry the entry name of the target archive file in the parent
     *         file system.
     * @param  options the options to use when accessing the target archive
     *         file.
     *         These may get modified as required by overridding this method
     *         or {@link #getInputSocket}. 
     * @param  source the nullable {@link InputService} for the target archive
     *         file.
     *         If not {@code null}, then the target archive file is going to
     *         get updated.
     *         This parameter is guaranteed to be the product of this driver's
     *         factory method
     *         {@link #newInputService(FsModel, FsController, FsEntryName, BitField)}.
     * @return A new output service for writing the target archive file.
     *         Note that this service does <em>not</em> need to be thread-safe!
     * @throws IOException on any I/O error.
     */
    @CreatesObligation
    public OutputService<E> newOutputService(
            FsModel model,
            FsController<?> parent,
            FsEntryName entry,
            BitField<FsAccessOption> options,
            @CheckForNull @WillNotClose InputService<E> source)
    throws IOException {
        return newOutputService(model,
                getOutputSocket(parent, entry, options), source);
    }

    /**
     * Creates a new output service for writing archive entries for the
     * given {@code model} to the given {@code output} socket's target.
     * 
     * @param  model the file system model.
     * @param  output the output socket for writing the contents of the
     *         archive file to its target.
     * @param  source the nullable {@link InputService}.
     * @return A new output service for writing the target archive file.
     *         Note that this service does <em>not</em> need to be thread-safe!
     * @throws IOException on any I/O error.
     * @see    #newOutputService(FsModel, InputService, FsController, FsEntryName, BitField) 
     */
    @CreatesObligation
    protected abstract OutputService<E> newOutputService(
            FsModel model,
            OutputSocket<?> output,
            @CheckForNull @WillNotClose InputService<E> source)
    throws IOException;

    /**
     * Called to prepare reading an archive file artifact of this driver from
     * {@code name} in {@code controller} using {@code options}.
     * <p>
     * This method is overridable to enable modifying the given options
     * before forwarding the call to the given controller.
     * The implementation in the class {@link FsArchiveDriver} simply forwards
     * the call to the given controller with the given options unaltered.
     * 
     * @param  controller the controller to use for reading an artifact of this
     *         driver.
     * @param  name the entry name.
     * @param  options the options to use.
     * @return An input socket for reading an artifact of this driver.
     * @see    #newInputService(FsModel, FsController, FsEntryName, BitField) 
     */
    protected InputSocket<?> getInputSocket(
            FsController<?> controller,
            FsEntryName name,
            BitField<FsAccessOption> options) {
        return controller.getInputSocket(name, options);
    }

    /**
     * Called to prepare writing an archive file artifact of this driver to
     * the entry {@code name} in {@code controller} using {@code options} and
     * the nullable {@code template}.
     * <p>
     * This method is overridable to enable modifying the given options
     * before forwarding the call to the given controller.
     * The implementation in the class {@link FsArchiveDriver} simply forwards
     * the call to the given controller with the given options unaltered.
     * 
     * @param  controller the controller to use for writing an artifact of this
     *         driver.
     * @param  name the entry name.
     * @param  options the options to use.
     * @return An output socket for writing an artifact of this driver.
     * @see    #newOutputService(FsModel, InputService, FsController, FsEntryName, BitField) 
     */
    protected OutputSocket<?> getOutputSocket(
            FsController<?> controller,
            FsEntryName name,
            BitField<FsAccessOption> options) {
        return controller.getOutputSocket(name, options, null);
    }

    /**
     * Equivalent to {@link #newEntry(java.lang.String, de.truezip.kernel.cio.Entry.Type, de.truezip.kernel.cio.Entry, de.truezip.kernel.util.BitField)
     * newEntry(name, type, template, FsAccessOptions.NONE)}.
     * 
     * @param  name an entry name.
     * @param  type an entry type.
     * @param  template if not {@code null}, then the new entry shall inherit
     *         as much properties from this entry as possible - with the
     *         exception of its name and type.
     * @return A new entry for the given name.
     */
    public final E newEntry(String name, Type type, @CheckForNull Entry template) {
        return newEntry(name, type, template, FsAccessOptions.NONE);
    }

    /**
     * Returns a new entry for the given name.
     * The implementation may need to fix this name in order to 
     * form a valid {@link Entry#getName() entry name} for their
     * particular requirements.
     * <p>
     * If {@code template} is not {@code null}, then the returned entry shall
     * inherit as much properties from this template as possible - with the
     * exception of its name and type.
     * Furthermore, if {@code name} and {@code type} are equal to the name and
     * type of this template, then the returned entry shall be a (deep) clone
     * of the template which shares no mutable state with the template.
     *
     * @param  name an entry name.
     * @param  type an entry type.
     * @param  template if not {@code null}, then the new entry shall inherit
     *         as much properties from this entry as possible - with the
     *         exception of its name and type.
     * @param  mknod when called from {@link FsController#mknod}, this is its
     *         {@code options} parameter, otherwise it's typically an empty set.
     * @return A new entry for the given name.
     */
    public abstract E newEntry(
            String name,
            Type type,
            @CheckForNull Entry template,
            BitField<FsAccessOption> mknod);

    /**
     * Normalizes the given entry name so that it forms a valid entry name for
     * ZIP and TAR files by ensuring that the returned entry name ends with the
     * separator character {@code '/'} if and only if {@code type} is
     * {@code DIRECTORY}.
     *
     * @param  name an entry name.
     * @param  type an entry type.
     * @return The normalized entry name.
     */
    protected static String normalize(String name, Type type) {
        return DIRECTORY == type
                ? name.endsWith(SEPARATOR) ? name : name + SEPARATOR_CHAR
                : cutTrailingSeparators(name, SEPARATOR_CHAR);
    }

    /**
     * Returns a string representation of this object for debugging and logging
     * purposes.
     */
    @Override
    public String toString() {
        return String.format("%s[charset=%s, federated=%b, priority=%d]",
                getClass().getName(),
                getCharset(),
                isFederated(),
                getPriority());
    }
}
