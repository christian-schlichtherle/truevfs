/*
 * Copyright (C) 2004-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.fs.archive;

import de.schlichtherle.truezip.entry.Entry;
import de.schlichtherle.truezip.entry.Entry.Type;
import static de.schlichtherle.truezip.fs.FsOutputOptions.NO_OUTPUT_OPTIONS;
import de.schlichtherle.truezip.fs.*;
import de.schlichtherle.truezip.socket.*;
import de.schlichtherle.truezip.util.BitField;
import edu.umd.cs.findbugs.annotations.CreatesObligation;
import java.io.CharConversionException;
import java.io.IOException;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.annotation.WillNotClose;
import javax.annotation.concurrent.Immutable;
import javax.swing.Icon;

/**
 * An abstract factory for components required for accessing archive files.
 * Implementations of this abstract base class are used to access specific
 * archive formats like ZIP, JAR, TZP, TAR, TAR.GZ, TAR.BZ2 etc.
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
     * Returns the I/O buffer pool to use for allocating temporary I/O buffers.
     * <p>
     * Multiple invocations should return the same I/O buffer pool.
     * However, callers should cache the return value for subsequent use in
     * case it isn't always the same.
     *
     * @return The I/O buffer pool to use for allocating temporary I/O buffers.
     */
    protected abstract IOPool<?> getPool();

    /**
     * Returns {@code true} if and only if the archive files produced by this
     * archive driver may contain redundant archive entry contents.
     * If the return value is {@code true}, then an archive file may contain
     * redundant archive entry contents, but only the last contents written
     * should get used when reading the archive file.
     * 
     * @return The implementation in the class {@link FsArchiveDriver} returns
     *         {@code false} for backwards compatibility.
     * @since  TrueZIP 7.3
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
     * @since  TrueZIP 7.3
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
     * Returns a new thread-safe file system controller for the mount point of
     * the given file system model and parent file system controller.
     * <p>
     * When called, the following expression is a precondition:
     * {@code model.getParent().equals(parent.getModel())}
     * <p>
     * Note that an archive file system is always federated and therefore
     * its parent file system controller is never {@code null}.
     * <p>
     * Furthermore, an archive driver implementation is <em>not</em> expected
     * to consider the scheme of the given mount point to determine the class
     * of the returned file system controller.
     * Consequently, it is an error to call this method with a mount point
     * which has a scheme which is not supported by this archive driver.
     * <p>
     * Note again that unlike the other components created by this factory,
     * the returned file system controller must be thread-safe!
     *
     * @param  model the file system model.
     * @param  parent the non-null parent file system controller.
     * @return A new thread-safe file system controller for the given mount
     *         point and parent file system controller.
     */
    @Override
    public FsController<?>
    newController(final FsModel model, final @Nonnull FsController<?> parent) {
        final boolean isLockModel = model instanceof FsLockModel;
        assert !isLockModel;
        final FsLockModel lockModel = isLockModel
                ? (FsLockModel) model
                : new FsLockModel(model);
        return  new FsSyncController<FsLockModel>(
                    new FsLockController(
                        new FsUnlinkController(
                            new FsCacheController(
                                new FsResourceController(
                                    new FsContextController(
                                        new FsDefaultArchiveController<E>(
                                                lockModel, parent, this))),
                                getPool()))));
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
     * @since  TrueZIP 7.1
     */
    public InputSocket<?> getInputSocket(   FsController<?> controller,
                                            FsEntryName name,
                                            BitField<FsInputOption> options) {
        return controller.getInputSocket(name, options);
    }

    /**
     * Creates a new input shop for reading the archive entries for the
     * given {@code model} from the given {@code input} socket's target.
     * <p>
     * Note that the returned input shop does <em>not</em> need to be
     * thread-safe.
     * 
     * @param  model the file system model.
     * @param  input the input socket for reading the contents of the
     *         archive file from its target.
     *         This is guaranteed to be the product of this driver's
     *         {@link #getInputSocket} method.
     * @return A new input shop.
     * @throws IOException on any I/O error.
     *         If the file system entry for the given model exists in the
     *         parent file system and is <em>not</em> a {@link Type#SPECIAL}
     *         type, then this exception is deemed to indicate a
     *         <em>persistent false positive</em> archive file and gets cached
     *         until the file system controller for the given model is
     *         {@linkplain FsController#sync(de.schlichtherle.truezip.util.BitField, de.schlichtherle.truezip.util.ExceptionHandler) synced}
     *         again.
     *         Otherwise, this exception is deemed to indicate a
     *         <em>transient false positive</em> archive file and does not
     *         get cached.
     */
    @CreatesObligation
    public abstract InputShop<E>
    newInputShop(   FsModel model,
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
     * @since  TrueZIP 7.1
     */
    public OutputSocket<?> getOutputSocket( FsController<?> controller,
                                            FsEntryName name,
                                            BitField<FsOutputOption> options,
                                            @CheckForNull Entry template) {
        return controller.getOutputSocket(name, options, template);
    }

    /**
     * Creates a new output shop for writing archive entries for the
     * given {@code model} to the given {@code output} socket's target.
     * <p>
     * Note that the returned output shop does <em>not</em> need to be
     * thread-safe.
     * 
     * @param  model the file system model.
     * @param  output the output socket for writing the contents of the
     *         archive file to its target.
     *         This is guaranteed to be the product of this driver's
     *         {@link #getOutputSocket} method.
     * @param  source the {@link InputShop} if {@code archive} is going to get
     *         updated.
     *         If not {@code null}, this is guaranteed to be the product
     *         of this driver's {@link #newInputShop} factory method.
     *         This feature could get used to copy some meta data which is
     *         specific to the type of archive this driver supports,
     *         e.g. the comment of a ZIP file.
     * @return A new output shop.
     * @throws IOException on any I/O error.
     */
    @CreatesObligation
    public abstract OutputShop<E>
    newOutputShop(  FsModel model,
                    OutputSocket<?> output,
                    @CheckForNull @WillNotClose InputShop<E> source)
    throws IOException;

    /**
     * Equivalent to {@link #newEntry(java.lang.String, de.schlichtherle.truezip.entry.Entry.Type, de.schlichtherle.truezip.entry.Entry, de.schlichtherle.truezip.util.BitField)
     * newEntry(name, type, template, FsOutputOptions.NO_OUTPUT_OPTIONS)}.
     * 
     * @param  name an entry name.
     * @param  type an entry type.
     * @param  template if not {@code null}, then the new entry shall inherit
     *         as much properties from this entry as possible - with the
     *         exception of its name and type.
     * @return A new entry for the given name.
     * @throws CharConversionException if {@code name} contains characters
     *         which are invalid.
     */
    public final E newEntry(String name, Type type, @CheckForNull Entry template)
    throws CharConversionException {
        return newEntry(name, type, template, NO_OUTPUT_OPTIONS);
    }

    /**
     * Returns a new archive entry for the given name.
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
     * @param  mknod when called from {@link FsArchiveController#mknod}, this
     *         is its {@code options} parameter, otherwise it's typically an
     *         empty set.
     * @return A new entry for the given name.
     * @throws CharConversionException if {@code name} contains characters
     *         which are invalid.
     */
    public abstract E newEntry(
            String name,
            Type type,
            @CheckForNull Entry template,
            BitField<FsOutputOption> mknod)
    throws CharConversionException;
}
