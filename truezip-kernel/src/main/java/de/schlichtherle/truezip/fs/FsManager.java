/*
 * Copyright (C) 2004-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.fs;

import static de.schlichtherle.truezip.fs.FsSyncOption.*;
import de.schlichtherle.truezip.util.BitField;
import de.schlichtherle.truezip.util.ExceptionHandler;
import java.io.IOException;
import java.util.Iterator;
import javax.annotation.concurrent.ThreadSafe;

/**
 * An abstract container which manages the life cycle of controllers for
 * federated file systems.
 * A file system is federated if and only if it's a member of a parent
 * (virtual) file system.
 * <p>
 * Sub-classes must be thread-safe, too.
 *
 * @author Christian Schlichtherle
 */
@ThreadSafe
public abstract class FsManager
implements Iterable<FsController<?>> {

    /**
     * Returns a thread-safe file system controller for the given mount point.
     * If and only if the given mount point addresses a federated file system,
     * the returned file system controller is remembered for life cycle
     * management, i.e. future lookup and {@link #sync synchronization}
     * operations.
     *
     * @param  mountPoint the mount point of the file system.
     * @param  driver the file system composite driver which shall get used to
     *         create a new file system controller if required.
     * @return A thread-safe file system controller for the given mount point.
     */
    public abstract FsController<?>
    getController(FsMountPoint mountPoint, FsCompositeDriver driver);

    /**
     * Returns the number of federated file systems managed by this instance.
     *
     * @return The number of federated file systems managed by this instance.
     */
    public abstract int getSize();

    /**
     * Returns an iterator over the controllers of all federated file systems
     * managed by this instance.
     * <p>
     * Note that the iterated file system controllers must be ordered so that
     * all file systems appear before any of their parent file systems.
     * <p>
     * Last, but not least: The iterator must be consistent in multithreaded
     * environments!
     *
     * @return An iterator over the controllers of all federated file systems
     *         managed by this instance.
     */
    @Override
    public abstract Iterator<FsController<?>> iterator();

    /**
     * @deprecated Use {@link FsSyncOptions#UMOUNT} instead.
     */
    @Deprecated
    public static final BitField<FsSyncOption> UMOUNT = FsSyncOptions.UMOUNT;

    /**
     * Commits all unsynchronized changes to the contents of all federated file
     * systems managed by this instance to their respective parent file system,
     * releases the associated resources (e.g. target archive files) for
     * access by third parties (e.g. other processes), cleans up any temporary
     * allocated resources (e.g. temporary files) and purges any cached data.
     * Note that temporary resources may get allocated even if the federated
     * file systems were accessed read-only.
     * As a side effect, this will reset the state of the respective file
     * system controllers.
     * <p>
     * This method calls {@link #sync sync(options, builder)}, where builder is
     * an instance of {@link FsSyncExceptionBuilder}.
     * If the call succeeds, the builder's {@link FsSyncExceptionBuilder#check}
     * method is called to check out any {@link FsSyncWarningException}, too.
     *
     * @param  options a bit field of synchronization options.
     * @throws FsSyncException if committing the changes fails for any reason.
     * @throws IllegalArgumentException if the combination of synchronization
     *         options is illegal, e.g. if
     *         {@link FsSyncOption#FORCE_CLOSE_INPUT} is cleared and
     *         {@link FsSyncOption#FORCE_CLOSE_OUTPUT} is set or if the
     *         synchronization option {@link FsSyncOption#ABORT_CHANGES} is set.
     */
    public final void
    sync(final BitField<FsSyncOption> options) throws FsSyncException {
        final FsSyncExceptionBuilder builder = new FsSyncExceptionBuilder();
        sync(options, builder);
        builder.check();
    }

    /**
     * Commits all unsynchronized changes to the contents of all federated file
     * systems managed by this instance to their respective parent file system,
     * releases the associated resources (e.g. target archive files) for
     * access by third parties (e.g. other processes), cleans up any temporary
     * allocated resources (e.g. temporary files) and purges any cached data.
     * Note that temporary resources may get allocated even if the federated
     * file systems were accessed read-only.
     * As a side effect, this will reset the state of the respective file
     * system controllers.
     *
     * @param  options a bit field of synchronization options.
     * @param  handler the exception handling strategy for consuming input
     *         {@code IOException}s and mapping them to output
     *         {@code IOException}s.
     * @param  <X> The type of the {@code IOException} to throw at the
     *         discretion of the exception {@code handler}.
     * @throws X at the discretion of the exception {@code handler}
     *         upon the occurence of any {@code IOException}.
     * @throws IllegalArgumentException if the combination of synchronization
     *         options is illegal, e.g. if
     *         {@link FsSyncOption#FORCE_CLOSE_INPUT} is cleared and
     *         {@link FsSyncOption#FORCE_CLOSE_OUTPUT} is set or if the
     *         synchronization option {@link FsSyncOption#ABORT_CHANGES} is set.
     */
    public <X extends IOException> void
    sync(   final BitField<FsSyncOption> options,
            final ExceptionHandler<? super IOException, X> handler)
    throws X {
        if (options.get(FORCE_CLOSE_OUTPUT) && !options.get(FORCE_CLOSE_INPUT)
                || options.get(ABORT_CHANGES))
            throw new IllegalArgumentException();

        for (final FsController<?> controller : this) {
            try {
                controller.sync(options, handler);
            } catch (final IOException ex) {
                handler.warn(ex);
            }
        }
    }

    /**
     * Two file system managers are considered equal if and only if they are
     * identical. This can't get overriden.
     * 
     * @param that the object to compare.
     */
    @SuppressWarnings(value = "EqualsWhichDoesntCheckParameterClass")
    @Override
    public final boolean equals(Object that) {
        return this == that;
    }

    /**
     * Returns a hash code which is consistent with {@link #equals}.
     * This can't get overriden.
     */
    @Override
    public final int hashCode() {
        return super.hashCode();
    }

    /**
     * Returns a string representation of this object for debugging and logging
     * purposes.
     */
    @Override
    public String toString() {
        return String.format("%s[size=%d]",
                getClass().getName(),
                getSize());
    }
}
