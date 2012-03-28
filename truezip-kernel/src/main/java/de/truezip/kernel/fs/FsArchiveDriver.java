/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.kernel.fs;

import de.truezip.kernel.cio.Entry.Type;
import de.truezip.kernel.cio.*;
import de.truezip.kernel.fs.addr.FsEntryName;
import de.truezip.kernel.fs.option.FsInputOption;
import de.truezip.kernel.fs.option.FsOutputOption;
import de.truezip.kernel.fs.option.FsOutputOptions;
import de.truezip.kernel.util.BitField;
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
     * @param  parent the parent file system controller.
     * @param  manager the file system manager for the new controller.
     * @return A new thread-safe file system controller for the given mount
     *         point and parent file system controller.
     */
    @Override
    public final FsController<?>
    newController(  FsManager manager,
                    FsModel model,
                    @Nonnull FsController<?> parent) {
        assert null == model.getParent()
                ? null == parent
                : model.getParent().equals(parent.getModel());
        if (null == parent)
            throw new IllegalArgumentException();
        return manager.newController(this, model, parent);
    }

    public <M extends FsModel> FsController<? extends M> decorate(
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
                                            BitField<FsInputOption> options) {
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
                                            BitField<FsOutputOption> options,
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
     * newEntry(name, type, template, FsOutputOptions.NONE)}.
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
        return newEntry(name, type, template, FsOutputOptions.NONE);
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
     * @param  mknod when called from {@link FsController#mknod}, this is its
     *         {@code options} parameter, otherwise it's typically an empty set.
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