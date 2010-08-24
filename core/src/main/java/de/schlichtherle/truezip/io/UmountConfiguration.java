package de.schlichtherle.truezip.io;

import de.schlichtherle.truezip.io.archive.controller.ArchiveExceptionBuilder;
import java.io.IOException;

/**
 * Holds the configuration parameters for
 * {@link ArchiveController#umount(UmountConfiguration)}.
 * Note that this class is immutable and declared final.
 * Using any setter method returns a {@link #clone()} which has the respective
 * property set as specified.
 * So in order to build a configuration by setting multiple properties,
 * you need to use the following idiom:
 * <p>
 * <pre>{@code
 * UmountConfiguration config = new UmountConfiguration()
 *      .setProperty1(property1)
 *      .setProperty2(property2)
 *      .setProperty3(property3);
 * }</pre>
 * 
 * @author Christian Schlichtherle
 * @version $Id$
 */
final class UmountConfiguration implements Cloneable {
    private ArchiveExceptionBuilder archiveExceptionBuilder;
    private boolean waitForInputStreams;
    private boolean closeInputStreams;
    private boolean waitForOutputStreams;
    private boolean closeOutputStreams;
    private boolean release;
    private boolean reassemble;

    /** Returns a <em>shallow</em> clone of this instance. */
    @Override
    public UmountConfiguration clone() {
        try {
            return (UmountConfiguration) super.clone();
        } catch (CloneNotSupportedException cannotHappen) {
            throw new AssertionError(cannotHappen);
        }
    }

    /** The handler to use to process archive exceptions. */
    public ArchiveExceptionBuilder getArchiveExceptionBuilder() {
        return archiveExceptionBuilder;
    }

    /** Returns a clone of this instance with the property set as specified. */
    public UmountConfiguration setArchiveExceptionBuilder(
            final ArchiveExceptionBuilder archiveControllerExceptionBuilder) {
        final UmountConfiguration clone = clone();
        clone.archiveExceptionBuilder = archiveControllerExceptionBuilder;
        return clone;
    }

    /**
     * Suppose any other thread has still one or more archive entry input
     * streams open to an archive file.
     * Then if and only if this property is {@code true}, the respective
     * archive controller will wait until all other threads have closed
     * their archive entry input streams before proceeding with the update
     * of the archive file.
     * Archive entry input streams opened (and not yet closed) by the
     * current thread are always ignored.
     * If the current thread gets interrupted while waiting, it will
     * stop waiting and proceed normally as if this property is
     * {@code false}.
     * <p>
     * Beware: If a stream has not been closed because the client
     * application does not always properly close its streams, even on an
     * {@link IOException} (which is a typical bug in many Java
     * applications), then the respective archive controller will not
     * return from the update until the current thread gets interrupted!
     */
    public boolean getWaitForInputStreams() {
        return waitForInputStreams;
    }

    /** Returns a clone of this instance with the property set as specified. */
    public UmountConfiguration setWaitForInputStreams(
            final boolean waitForInputStreams) {
        final UmountConfiguration clone = clone();
        clone.waitForInputStreams = waitForInputStreams;
        return clone;
    }

    /**
     * Suppose there are any open input streams for any archive entries of
     * an archive file because the client application has forgot to close
     * all {@link FileInputStream} objects or another thread is still busy
     * doing I/O on the archive file.
     * Then if this property is {@code true}, the respective archive
     * controller will proceed to update the archive file anyway and
     * finally throw an {@link ArchiveFileBusyWarningException} to indicate
     * that any subsequent operations on these streams will fail with an
     * {@link ArchiveEntryStreamClosedException} because they have been
     * forced to close.
     * <p>
     * This may also be used to recover a client application from a
     * {@link FileBusyException} thrown by a constructor of
     * {@link FileInputStream} or {@link FileOutputStream}.
     * <p>
     * If this property is {@code false}, the archive file is <em>not</em>
     * updated and an {@link ArchiveFileBusyException} is thrown to
     * indicate that the application must close all entry input streams
     * first.
     */
    public boolean getCloseInputStreams() {
        return closeInputStreams;
    }

    /** Returns a clone of this instance with the property set as specified. */
    public UmountConfiguration setCloseInputStreams(
            final boolean closeInputStreams) {
        final UmountConfiguration clone = clone();
        clone.closeInputStreams = closeInputStreams;
        return clone;
    }

    /**
     * Similar to {@code waitInputStreams},
     * but applies to archive entry output streams instead.
     */
    public boolean getWaitForOutputStreams() {
        return waitForOutputStreams;
    }

    /** Returns a clone of this instance with the property set as specified. */
    public UmountConfiguration setWaitForOutputStreams(
            final boolean waitForOutputStreams) {
        final UmountConfiguration clone = clone();
        clone.waitForOutputStreams = waitForOutputStreams;
        return clone;
    }

    /**
     * Similar to {@code closeInputStreams},
     * but applies to archive entry output streams instead.
     * <p>
     * If this parameter is {@code true}, then
     * {@code closeInputStreams} must be {@code true}, too.
     * Otherwise, an {@code IllegalArgumentException} is thrown.
     */
    public boolean getCloseOutputStreams() {
        return closeOutputStreams;
    }

    /** Returns a clone of this instance with the property set as specified. */
    public UmountConfiguration setCloseOutputStreams(
            final boolean closeOutputStreams) {
        final UmountConfiguration clone = clone();
        clone.closeOutputStreams = closeOutputStreams;
        return clone;
    }

    /**
     * If this property is {@code true}, the archive file is completely
     * released in order to enable subsequent read/write access to it for third
     * parties such as other processes or the package {@link java.io}
     * <em>before</em> TrueZIP is used again to read from or write to the
     * archive file.
     * <p>
     * If this property is {@code true}, some temporary files might be retained
     * for caching in order to enable faster subsequent access to the archive
     * file again.
     * <p>
     * Note that temporary files are always deleted by TrueZIP unless the JVM
     * is terminated unexpectedly. This property solely exists to control
     * cooperation with third parties or enabling faster access.
     */
    public boolean getRelease() {
        return release;
    }

    /** Returns a clone of this instance with the property set as specified. */
    public UmountConfiguration setRelease(final boolean release) {
        final UmountConfiguration clone = clone();
        clone.release = release;
        return clone;
    }

    /**
     * Let's assume an archive file is enclosed in another archive file.
     * Then if this property is {@code true}, the updated archive file is
     * also written to its enclosing archive file.
     * Note that this property <em>must</em> be set to {@code true} if the
     * property {@code umount} is set to {@code true} as well.
     * Failing to comply to this requirement may throw an
     * {@link AssertionError} and will incur loss of data!
     */
    public boolean getReassemble() {
        return reassemble;
    }

    /** Returns a clone of this instance with the property set as specified. */
    public UmountConfiguration setReassemble(final boolean reassemble) {
        final UmountConfiguration clone = clone();
        clone.reassemble = reassemble;
        return clone;
    }
}
