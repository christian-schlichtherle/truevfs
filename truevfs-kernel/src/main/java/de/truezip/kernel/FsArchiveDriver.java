/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.kernel;

import static de.truezip.kernel.FsEntryName.SEPARATOR;
import static de.truezip.kernel.FsEntryName.SEPARATOR_CHAR;
import de.truezip.kernel.cio.Entry;
import de.truezip.kernel.cio.Entry.Type;
import static de.truezip.kernel.cio.Entry.Type.DIRECTORY;
import de.truezip.kernel.cio.IOPool;
import de.truezip.kernel.cio.InputService;
import de.truezip.kernel.cio.OutputService;
import de.truezip.kernel.io.Sink;
import de.truezip.kernel.io.Source;
import de.truezip.kernel.sl.IOPoolLocator;
import de.truezip.kernel.util.BitField;
import static de.truezip.kernel.util.Paths.cutTrailingSeparators;
import edu.umd.cs.findbugs.annotations.CreatesObligation;
import java.io.CharConversionException;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
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
     * Returns the character set to use for encoding character based meta data
     * such as entry names or file comments to binary data when writing
     * an archive file.
     * Depending on the archive file format, this may get used for decoding
     * binary data when reading an archive file, too.
     * <p>
     * This is an immutable property - multiple calls must return the same
     * object.
     *
     * @return The character set to use for encoding character based meta data
     *         such as entry names or file comments to binary data when writing
     *         an archive file.
     */
    public abstract Charset getCharset();

    /**
     * Ensures that the given entry name can get encoded by this driver's
     * {@linkplain #getCharset() character set}.
     *
     * @param  name an entry name.
     * @throws CharConversionException If the entry name contains characters
     *         which cannot get encoded.
     */
    public final void checkEncodable(final String name)
    throws CharConversionException {
        CharsetEncoder enc = encoder.get();
        if (null == enc) {
            enc = getCharset().newEncoder();
            encoder.set(enc);
        }
        if (!enc.canEncode(name))
            throw new CharConversionException(name +
                    " (entry name not encodable with " + getCharset() + ")");
    }

    private final ThreadLocal<CharsetEncoder> encoder = new ThreadLocal<>();

    /**
     * Returns the pool to use for allocating temporary I/O buffers.
     * <p>
     * This is an immutable property - multiple calls must return the same
     * object.
     * <p>
     * The implementation in the class {@link FsArchiveDriver} calls the
     * equally named method on the {@link IOPoolLocator#SINGLETON}.
     *
     * @return The pool to use for allocating temporary I/O buffers.
     */
    public IOPool<?> getIOPool() {
        return IOPoolLocator.SINGLETON.getIOPool();
    }

    /**
     * Returns {@code true} if and only if the archive files produced by this
     * archive driver may contain redundant archive entry contents.
     * If the return value is {@code true}, then an archive file may contain
     * redundant archive entry contents, but only the last contents written
     * should get used when reading the archive file.
     * <p>
     * This is an immutable property - multiple calls must return the same
     * value.
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
     * <p>
     * This is an immutable property - multiple calls must return the same
     * value.
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
    public final FsController<? extends FsModel> newController(
            FsManager manager,
            FsModel model,
            @Nonnull FsController<? extends FsModel> parent) {
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
     *         Note that this controller may throw {@link RuntimeException}s
     *         for non-local control flow!
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
     * to {@link #source}
     * and {@link #newInput(FsModel, Source)}.
     * 
     * @param  model the file system model for the target archive file.
     * @param  options the options for accessing the target archive file in the
     *         parent file system.
     * @param  controller the controller for the parent file system with the
     *         target archive file.
     * @param  name the entry name of the target archive file in the parent
     *         file system.
     * @return A new input service for reading the target archive file.
     *         Note that this service does <em>not</em> need to be thread-safe!
     * @throws IOException on any I/O error.
     *         If the file system entry for the given model exists in the
     *         parent file system and is <em>not</em> a {@link Type#SPECIAL}
     *         type, then this exception is deemed to indicate a
     *         <em>persistent false positive</em> archive file and gets cached
     *         until the file system controller for the given model gets
     *         {@linkplain FsController#sync(BitField) synced} again.
     *         Otherwise, this exception is deemed to indicate a
     *         <em>transient false positive</em> archive file and does not
     *         get cached.
     */
    @CreatesObligation
    public InputService<E> newInput(
            FsModel model,
            BitField<FsAccessOption> options,
            FsController<?> controller,
            FsEntryName name)
    throws IOException {
        return newInput(model, source(options, controller, name));
    }

    /**
     * Creates a new input service for reading archive entries for the given
     * {@code model} from the target archive file referenced by {@code source}.
     * 
     * @param  model the file system model.
     * @param  source the source for reading the target archive file.
     * @return A new input service.
     *         Note that this service does <em>not</em> need to be thread-safe!
     * @throws IOException on any I/O error.
     * @see    #newInput(FsModel, BitField, FsController, FsEntryName) 
     */
    @CreatesObligation
    protected abstract InputService<E> newInput(
            FsModel model,
            Source source)
    throws IOException;

    /**
     * This method gets called by an archive controller in order to create a
     * new output service for its target archive file.
     * <p>
     * The implementation in {@link FsArchiveDriver} simply forwards the call
     * to {@link #sink}
     * and {@link #newOutput(FsModel, Sink, InputService)}.
     * 
     * @param  model the file system model for the target archive file.
     * @param  options the options for accessing the target archive file in the
     *         parent file system.
     * @param  controller the controller for the parent file system with the
     *         target archive file.
     * @param  name the entry name of the target archive file in the parent
     *         file system.
     * @param  input the nullable {@link InputService} for the target archive
     *         file.
     *         If not {@code null}, then the target archive file is going to
     *         get updated.
     *         This parameter is guaranteed to be the product of this driver's
     *         factory method
     *         {@link #newInput(FsModel, BitField, FsController, FsEntryName)}.
     * @return A new output service for writing the target archive file.
     *         Note that this service does <em>not</em> need to be thread-safe!
     * @throws IOException on any I/O error.
     */
    @CreatesObligation
    public OutputService<E> newOutput(
            FsModel model,
            BitField<FsAccessOption> options,
            FsController<?> controller,
            FsEntryName name,
            @CheckForNull @WillNotClose InputService<E> input)
    throws IOException {
        return newOutput(model, sink(options, controller, name), input);
    }

    /**
     * Creates a new input service for writing archive entries for the given
     * {@code model} to the target archive file referenced by {@code sink}.
     * 
     * @param  model the file system model.
     * @param  sink the sink for writing the target archive file.
     * @param  input the nullable {@link InputService} for the target archive
     *         file.
     *         If not {@code null}, then the target archive file is going to
     *         get updated.
     *         This parameter is guaranteed to be the product of this driver's
     *         factory method
     *         {@link #newInput(FsModel, BitField, FsController, FsEntryName)}.
     * @return A new output service for writing the target archive file.
     *         Note that this service does <em>not</em> need to be thread-safe!
     * @throws IOException on any I/O error.
     * @see    #newOutput(FsModel, BitField, FsController, FsEntryName, InputService) 
     */
    @CreatesObligation
    protected abstract OutputService<E> newOutput(
            FsModel model,
            Sink sink,
            @CheckForNull @WillNotClose InputService<E> input)
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
     * @param  options the options for accessing the file system entry.
     * @param  controller the controller to use for reading an artifact of this
     *         driver.
     * @param  name the entry name.
     * @return A source for reading an artifact of this driver.
     * @see    #newInput(FsModel, BitField, FsController, FsEntryName) 
     */
    protected Source source(
            BitField<FsAccessOption> options,
            FsController<?> controller,
            FsEntryName name) {
        return controller.input(options, name);
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
     * @param  options the options for accessing the file system entry.
     * @param  controller the controller to use for writing an artifact of this
     *         driver.
     * @param  name the entry name.
     * @return A sink for writing an artifact of this driver.
     * @see    #newOutput(FsModel, BitField, FsController, FsEntryName, InputService) 
     */
    protected Sink sink(
            BitField<FsAccessOption> options,
            FsController<?> controller,
            FsEntryName name) {
        return controller.output(options, name, null);
    }

    /**
     * Equivalent to {@link #newEntry(BitField, String, Entry.Type, Entry)
     * entry(FsAccessOptions.NONE, name, type, template)}.
     * 
     * @param  name the entry name.
     * @param  type the entry type.
     * @param  template if not {@code null}, then the new entry shall inherit
     *         as much properties from this entry as possible - with the
     *         exception of its name and type.
     * @return A new entry for the given name.
     */
    public final E newEntry(String name, Type type, @CheckForNull Entry template) {
        return newEntry(FsAccessOptions.NONE, name, type, template);
    }

    /**
     * Returns a new entry for the given name.
     * The implementation may change this name in order to form a valid
     * {@link Entry#getName() entry name} for their particular requirements.
     *
     * @param  options when called from {@link FsController#mknod}, this is its
     *         {@code options} parameter, otherwise it's typically an empty set.
     * @param  name the entry name.
     * @param  type the entry type.
     * @param  template if not {@code null}, then the new entry shall inherit
     *         as much properties from this entry as possible - with the
     *         exception of its name and type.
     * @return A new entry for the given name.
     * @see    #newEntry(String, Entry.Type, Entry)
     */
    public abstract E newEntry(
            BitField<FsAccessOption> options,
            String name,
            Type type,
            @CheckForNull Entry template);

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
    public static String normalize(final String name, final Type type) {
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
        return String.format("%s[federated=%b, charset=%s]",
                getClass().getName(),
                isFederated(),
                getCharset());
    }
}
