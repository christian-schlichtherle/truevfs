/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.kernel;

import de.truezip.kernel.addr.FsEntryName;
import static de.truezip.kernel.addr.FsEntryName.SEPARATOR;
import static de.truezip.kernel.addr.FsEntryName.SEPARATOR_CHAR;
import de.truezip.kernel.cio.Entry.Type;
import static de.truezip.kernel.cio.Entry.Type.DIRECTORY;
import de.truezip.kernel.cio.*;
import static de.truezip.kernel.io.Paths.cutTrailingSeparators;
import de.truezip.kernel.option.AccessOption;
import de.truezip.kernel.option.AccessOptions;
import de.truezip.kernel.util.BitField;
import edu.umd.cs.findbugs.annotations.CreatesObligation;
import java.io.CharConversionException;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.annotation.WillNotClose;
import javax.annotation.concurrent.Immutable;
import javax.swing.Icon;

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

    private final Charset charset;

    /**
     * Constructs a new archive driver.
     *
     * @param charset the character set to use for encoding entry names and
     *        probably other meta data when reading or writing an archive file.
     */
    protected FsArchiveDriver(final Charset charset) {
        if (null == (this.charset = charset))
            throw new NullPointerException();
    }

    /**
     * Returns the character set provided to the constructor.
     * Subsequent calls must return the same object.
     *
     * @return The character set to use for encoding entry names and
     *         probably other meta data when writing an archive file.
     *         Depending on the archive file format, this may get used for
     *         reading an archive file, too.
     */
    public Charset getCharset() {
        return charset;
    }

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
     * Returns the icon that should be displayed for the given archive file
     * if it's open/expanded in the view.
     * <p>
     * The implementation in the abstract class {@code FsArchiveDriver} simply
     * returns {@code null}.
     *
     * @param  model the file system model.
     * @return The icon that should be displayed for the given archive file
     *         if it's open/expanded in the view.
     *         If {@code null} is returned, a default icon should be displayed.
     */
    public @CheckForNull Icon getOpenIcon(FsModel model) {
        return null;
    }

    /**
     * Returns the icon that should be displayed for the given archive file
     * if it's closed/collapsed in the view.
     * <p>
     * The implementation in the abstract class {@code FsArchiveDriver} simply
     * returns {@code null}.
     *
     * @param  model the file system model.
     * @return The icon that should be displayed for the given archive file
     *         if it's closed/collapsed in the view.
     *         If {@code null} is returned, a default icon should be displayed.
     */
    public @CheckForNull Icon getClosedIcon(FsModel model) {
        return null;
    }

    /**
     * {@inheritDoc}
     * <p>
     * The implementation in the class {@link FsArchiveDriver} simply forwards
     * the call to the given file system manager after asserting that
     * {@code model.getParent().equals(parent.getModel())} is {@code true}.
     */
    @Override
    public final FsController<?>
    newController(  FsManager manager,
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
     * Called to prepare reading an archive file artifact of this driver from
     * {@code name} in {@code controller} using {@code options}.
     * <p>
     * This method may get overridden in order to modify the given options
     * before forwarding the call to the given controller.
     * The implementation in the class {@link FsArchiveDriver} simply forwards
     * the call to the given controller with the given options unaltered.
     * 
     * @param  controller the controller to use for reading an artifact of this
     *         driver.
     * @param  name the entry name.
     * @param  options the options to use.
     * @return An input socket for reading an artifact of this driver.
     */
    public InputSocket<?> getInputSocket(   FsController<?> controller,
                                            FsEntryName name,
                                            BitField<AccessOption> options) {
        return controller.getInputSocket(name, options);
    }

    /**
     * Creates a new input service for reading the archive entries for the
     * given {@code model} from the given {@code input} socket's target.
     * <p>
     * Note that the returned input service does <em>not</em> need to be
     * thread-safe.
     * 
     * @param  model the file system model.
     * @param  input the input socket for reading the contents of the
     *         archive file from its target.
     *         This is guaranteed to be the product of this driver's
     *         {@link #getInputSocket} method.
     * @return A new input service.
     * @throws IOException on any I/O error.
     *         If the file system entry for the given model exists in the
     *         parent file system and is <em>not</em> a {@link Type#SPECIAL}
     *         type, then this exception is deemed to indicate a
     *         <em>persistent false positive</em> archive file and gets cached
     *         until the file system controller for the given model is
     *         {@linkplain FsController#sync(de.truezip.kernel.util.BitField, de.truezip.kernel.util.ExceptionHandler) synced}
     *         again.
     *         Otherwise, this exception is deemed to indicate a
     *         <em>transient false positive</em> archive file and does not
     *         get cached.
     */
    @CreatesObligation
    public abstract InputService<E>
    newInputService(FsModel model,
                    InputSocket<?> input)
    throws IOException;

    /**
     * Called to prepare writing an archive file artifact of this driver to
     * the entry {@code name} in {@code controller} using {@code options} and
     * the nullable {@code template}.
     * <p>
     * This method may get overridden in order to modify the given options
     * before forwarding the call to the given controller.
     * The implementation in the class {@link FsArchiveDriver} simply forwards
     * the call to the given controller with the given options unaltered.
     * 
     * @param  controller the controller to use for writing an artifact of this
     *         driver.
     * @param  name the entry name.
     * @param  options the options to use.
     * @param  template the template to use.
     * @return An output socket for writing an artifact of this driver.
     */
    public OutputSocket<?> getOutputSocket( FsController<?> controller,
                                            FsEntryName name,
                                            BitField<AccessOption> options,
                                            @CheckForNull Entry template) {
        return controller.getOutputSocket(name, options, template);
    }

    /**
     * Creates a new output service for writing archive entries for the
     * given {@code model} to the given {@code output} socket's target.
     * <p>
     * Note that the returned output service does <em>not</em> need to be
     * thread-safe.
     * 
     * @param  model the file system model.
     * @param  output the output socket for writing the contents of the
     *         archive file to its target.
     *         This is guaranteed to be the product of this driver's
     *         {@link #getOutputSocket} method.
     * @param  source the {@link InputService} if {@code archive} is going to
     *         get updated.
     *         If not {@code null}, this is guaranteed to be the product
     *         of this driver's {@link #newInputService} factory method.
     *         This feature could get used to copy some meta data which is
     *         specific to the type of archive this driver supports,
     *         e.g. the comment of a ZIP file.
     * @return A new output service.
     * @throws IOException on any I/O error.
     */
    @CreatesObligation
    public abstract OutputService<E>
    newOutputService(   FsModel model,
                        OutputSocket<?> output,
                        @CheckForNull @WillNotClose InputService<E> source)
    throws IOException;

    /**
     * Equivalent to {@link #newEntry(java.lang.String, de.truezip.kernel.cio.Entry.Type, de.truezip.kernel.cio.Entry, de.truezip.kernel.util.BitField)
     * newEntry(name, type, template, AccessOptions.NONE)}.
     * 
     * @param  name an entry name.
     * @param  type an entry type.
     * @param  template if not {@code null}, then the new entry shall inherit
     *         as much properties from this entry as possible - with the
     *         exception of its name and type.
     * @return A new entry for the given name.
     */
    public final E newEntry(String name, Type type, @CheckForNull Entry template) {
        return newEntry(name, type, template, AccessOptions.NONE);
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
            BitField<AccessOption> mknod);

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
     * Ensures that the given entry name can get encoded by this driver's
     * character set.
     * 
     * @param  name an entry name.
     * @throws CharConversionException If the entry name contains characters
     *         which cannot get encoded.
     */
    public final void checkEncodable(final String name)
    throws CharConversionException {
        if (!encoder.canEncode(name))
            throw new CharConversionException(name +
                    " (not encodable with " + getCharset() + ")");
    }

    private final ThreadLocalCharsetEncoder
            encoder = new ThreadLocalCharsetEncoder();

    private final class ThreadLocalCharsetEncoder
    extends ThreadLocal<CharsetEncoder> {
        @Override
        protected CharsetEncoder initialValue() {
            return getCharset().newEncoder();
        }

        boolean canEncode(CharSequence cs) {
            return get().canEncode(cs);
        }
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
