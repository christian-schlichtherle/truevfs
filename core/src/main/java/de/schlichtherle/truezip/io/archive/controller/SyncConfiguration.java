package de.schlichtherle.truezip.io.archive.controller;

import java.io.IOException;
import java.io.InputStream;

/**
 * Holds the configuration parameters for
 * {@link ArchiveControllers#sync(String, SyncConfiguration)} and
 * {@link ArchiveController#sync(SyncConfiguration)}.
 * Note that this class is immutable and declared final.
 * Using any setter method returns a {@link #clone()} which has the respective
 * property set as specified.
 * So in order to build a configuration by setting multiple properties,
 * you need to use the following idiom:
 * <p>
 * <pre>{@code
 * SyncConfiguration config = new SyncConfiguration()
 *      .setProperty1(property1)
 *      .setProperty2(property2)
 *      .setProperty3(property3);
 * }</pre>
 * 
 * @author Christian Schlichtherle
 * @version $Id$
 */
public final class SyncConfiguration implements Cloneable {
    private SyncExceptionBuilder syncExceptionBuilder
            = new DefaultSyncExceptionBuilder();
    private boolean waitForInputStreams = false;
    private boolean closeInputStreams = true;
    private boolean waitForOutputStreams = false;
    private boolean closeOutputStreams = true;
    private boolean umount = true;
    private boolean reassemble = true;

    /** Returns a <em>shallow</em> clone of this instance. */
    @Override
    public SyncConfiguration clone() {
        try {
            return (SyncConfiguration) super.clone();
        } catch (CloneNotSupportedException cannotHappen) {
            throw new AssertionError(cannotHappen);
        }
    }

    /**
     * The handler to use to process archive exceptions.
     * <p>
     * The default value for this property is
     * a {@code new} {@link DefaultSyncExceptionBuilder}{@code ()}.
     */
    public SyncExceptionBuilder getSyncExceptionBuilder() {
        return syncExceptionBuilder;
    }

    /** Returns a clone of this instance with the property set as specified. */
    public SyncConfiguration setSyncExceptionBuilder(
            final SyncExceptionBuilder archiveFileExceptionBuilder) {
        final SyncConfiguration clone = clone();
        clone.syncExceptionBuilder = archiveFileExceptionBuilder;
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
     * <p>
     * The default value for this property is {@code false}.
     */
    public boolean getWaitForInputStreams() {
        return waitForInputStreams;
    }

    /** Returns a clone of this instance with the property set as specified. */
    public SyncConfiguration setWaitForInputStreams(
            final boolean waitForInputStreams) {
        final SyncConfiguration clone = clone();
        clone.waitForInputStreams = waitForInputStreams;
        return clone;
    }

    /**
     * Suppose there are any open input streams for any archive entries of
     * an archive file because the client application has forgot to
     * {@link InputStream#close()} all {@code InputStream} objects or another
     * thread is still busy doing I/O on the archive file.
     * Then if this property is {@code true}, the respective archive
     * controller will proceed to update the archive file anyway and
     * finally throw an {@link ArchiveBusyWarningException} to indicate
     * that any subsequent operations on these streams will fail with an
     * {@link ArchiveEntryStreamClosedException} because they have been
     * forced to close.
     * <p>
     * If this property is {@code false}, the archive file is <em>not</em>
     * updated and an {@link ArchiveBusyException} is thrown to
     * indicate that the application must close all entry input streams
     * first.
     * <p>
     * The default value for this property is {@code true}.
     */
    public boolean getCloseInputStreams() {
        return closeInputStreams;
    }

    /** Returns a clone of this instance with the property set as specified. */
    public SyncConfiguration setCloseInputStreams(
            final boolean closeInputStreams) {
        final SyncConfiguration clone = clone();
        clone.closeInputStreams = closeInputStreams;
        return clone;
    }

    /**
     * Similar to {@code waitInputStreams},
     * but applies to archive entry output streams instead.
     * <p>
     * The default value for this property is {@code false}.
     */
    public boolean getWaitForOutputStreams() {
        return waitForOutputStreams;
    }

    /** Returns a clone of this instance with the property set as specified. */
    public SyncConfiguration setWaitForOutputStreams(
            final boolean waitForOutputStreams) {
        final SyncConfiguration clone = clone();
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
     * <p>
     * The default value for this property is {@code true}.
     */
    public boolean getCloseOutputStreams() {
        return closeOutputStreams;
    }

    /** Returns a clone of this instance with the property set as specified. */
    public SyncConfiguration setCloseOutputStreams(
            final boolean closeOutputStreams) {
        final SyncConfiguration clone = clone();
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
     * <p>
     * The default value for this property is {@code true}.
     */
    public boolean getUmount() {
        return umount;
    }

    /** Returns a clone of this instance with the property set as specified. */
    public SyncConfiguration setUmount(final boolean umount) {
        final SyncConfiguration clone = clone();
        clone.umount = umount;
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
     * <p>
     * The default value for this property is {@code true}.
     */
    public boolean getReassemble() {
        return reassemble;
    }

    /** Returns a clone of this instance with the property set as specified. */
    public SyncConfiguration setReassemble(final boolean reassemble) {
        final SyncConfiguration clone = clone();
        clone.reassemble = reassemble;
        return clone;
    }
}
