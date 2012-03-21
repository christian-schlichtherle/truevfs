/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.file;

import static de.schlichtherle.truezip.fs.FsSyncOptions.UMOUNT;
import de.schlichtherle.truezip.fs.*;
import de.schlichtherle.truezip.util.BitField;

/**
 * Static utility methods for virtual file system operations with global scope.
 * If you are not sure which method you should use, try {@link #umount()} -
 * it does the right thing for most use cases.
 * <p>
 * The primary purpose of this class is to consolidate the many variants and
 * incarnations of {@link TFile#umount} alias {@link TFile#sync} alias ... into
 * one class which is not tightly coupled to a particular client API,
 * i.e. TrueZIP File* or TrueZIP Path.
 * All other variants and incarnations now forward the call to this class and
 * are declared to be deprecated.
 * 
 * @since  TrueZIP 7.5
 * @author Christian Schlichtherle
 */
public final class TVFS {

    /* Can't touch this - hammer time! */
    private TVFS() { }

    /**
     * Commits all pending changes for all (nested) archive files to their
     * respective parent file system, closes their associated target archive
     * file in order to allow access by third parties
     * (e.g.&#160;other processes), cleans up any temporary allocated resources
     * (e.g.&#160;temporary files) and purges any cached data.
     * <p>
     * Note that temporary files may get used even for read-only access to
     * archive files, so calling this operation is essential.
     * However, if the client application never calls this operation, then it
     * gets called by a shutdown hook.
     * The shutdown hook gets removed as soon as this operation gets called in
     * order to leak no memory.
     * <p>
     * Calling this method is equivalent to
     * {@link #sync(BitField) sync(FsSyncOptions.UMOUNT)}.
     *
     * @throws FsSyncWarningException if <em>only</em> warning conditions
     *         apply.
     *         This implies that the respective parent file system has been
     *         synchronized with constraints, e.g. if an unclosed archive entry
     *         stream was forcibly closed.
     * @throws FsSyncException if any error conditions apply.
     *         This implies some loss of data!
     */
    public static void umount() throws FsSyncException {
        sync(UMOUNT);
    }

    /**
     * Commits all pending changes for all (nested) archive files within the
     * (virtual) directory tree referenced by the (prospective)
     * {@code mountPoint} to their respective parent file system, closes their
     * associated target archive file in order to allow access by third parties
     * (e.g.&#160;other processes), cleans up any temporary allocated resources
     * (e.g.&#160;temporary files) and purges any cached data.
     * <p>
     * Calling this method is equivalent to
     * {@link #sync(TFile, BitField) sync(archive, FsSyncOptions.UMOUNT)}.
     *
     * @param  mountPoint the prospective mount point of a (federated) file
     *         system, i.e. any (virtual) file system entry.
     * @throws IllegalArgumentException if {@code archive} is not a top level
     *         archive file.
     * @throws FsSyncWarningException if <em>only</em> warning conditions
     *         apply.
     *         This implies that the respective parent file system has been
     *         synchronized with constraints, e.g. if
     *         {@code FsSyncOption.FORCE_CLOSE_INPUT} or
     *         {@code FsSyncOption.FORCE_CLOSE_OUTPUT} is set and an unclosed
     *         archive entry stream is forcibly closed.
     * @throws FsSyncException if any error conditions apply.
     *         This implies some loss of data!
     * @see    TFile#isTopLevelArchive()
     */
    public static void umount(TFile mountPoint) throws FsSyncException {
        sync(mountPoint, UMOUNT);
    }

    /**
     * Commits all pending changes for all (nested) archive files within the
     * (virtual) directory tree referenced by the (prospective)
     * {@code mountPoint} to their respective parent file system, closes their
     * associated target archive file in order to allow access by third parties
     * (e.g.&#160;other processes), cleans up any temporary allocated resources
     * (e.g.&#160;temporary files) and purges any cached data.
     * <p>
     * Calling this method is equivalent to
     * {@link #sync(FsMountPoint, BitField) sync(mountPoint, FsSyncOptions.UMOUNT)}.
     *
     * @param  mountPoint the mount point of a federated file system, i.e. a
     *         prospective archive file.
     * @throws FsSyncWarningException if <em>only</em> warning conditions
     *         apply.
     *         This implies that the respective parent file system has been
     *         synchronized with constraints, e.g. if an unclosed archive entry
     *         stream was forcibly closed.
     * @throws FsSyncException if any error conditions apply.
     *         This implies some loss of data!
     */
    public static void umount(FsMountPoint mountPoint) throws FsSyncException {
        sync(mountPoint, UMOUNT);
    }

    /**
     * Commits all pending changes for all (nested) archive files to their
     * respective parent file system with respect to the given option.
     *
     * @param  option an option for the synchronization operation.
     * @throws IllegalArgumentException if the combination of synchronization
     *         options is illegal, e.g. if
     *         {@code FsSyncOption.FORCE_CLOSE_INPUT} is cleared and
     *         {@code FsSyncOption.FORCE_CLOSE_OUTPUT} is set or if
     *         {@code FsSyncOption.ABORT_CHANGES} is set.
     * @throws FsSyncWarningException if <em>only</em> warning conditions
     *         apply.
     *         This implies that the respective parent file system has been
     *         synchronized with constraints, e.g. if
     *         {@code FsSyncOption.FORCE_CLOSE_INPUT} or
     *         {@code FsSyncOption.FORCE_CLOSE_OUTPUT} is set and an unclosed
     *         archive entry stream is forcibly closed.
     * @throws FsSyncException if any error conditions apply.
     *         This implies some loss of data!
     */
    public static void sync(FsSyncOption option)
    throws FsSyncException {
        sync(BitField.of(option));
    }

    /**
     * Commits all pending changes for all (nested) archive files to their
     * respective parent file system with respect to the given options.
     *
     * @param  option an option for the synchronization operation.
     * @param  options an array of additional options for the synchronization
     *         operation.
     * @throws IllegalArgumentException if the combination of synchronization
     *         options is illegal, e.g. if
     *         {@code FsSyncOption.FORCE_CLOSE_INPUT} is cleared and
     *         {@code FsSyncOption.FORCE_CLOSE_OUTPUT} is set or if
     *         {@code FsSyncOption.ABORT_CHANGES} is set.
     * @throws FsSyncWarningException if <em>only</em> warning conditions
     *         apply.
     *         This implies that the respective parent file system has been
     *         synchronized with constraints, e.g. if
     *         {@code FsSyncOption.FORCE_CLOSE_INPUT} or
     *         {@code FsSyncOption.FORCE_CLOSE_OUTPUT} is set and an unclosed
     *         archive entry stream is forcibly closed.
     * @throws FsSyncException if any error conditions apply.
     *         This implies some loss of data!
     */
    public static void sync(FsSyncOption option, FsSyncOption... options)
    throws FsSyncException {
        sync(BitField.of(option, options));
    }

    /**
     * Commits all pending changes for all (nested) archive files to their
     * respective parent file system with respect to the given options.
     *
     * @param  options a bit field of options for the synchronization operation.
     * @throws IllegalArgumentException if the combination of synchronization
     *         options is illegal, e.g. if
     *         {@code FsSyncOption.FORCE_CLOSE_INPUT} is cleared and
     *         {@code FsSyncOption.FORCE_CLOSE_OUTPUT} is set or if
     *         {@code FsSyncOption.ABORT_CHANGES} is set.
     * @throws FsSyncWarningException if <em>only</em> warning conditions
     *         apply.
     *         This implies that the respective parent file system has been
     *         synchronized with constraints, e.g. if
     *         {@code FsSyncOption.FORCE_CLOSE_INPUT} or
     *         {@code FsSyncOption.FORCE_CLOSE_OUTPUT} is set and an unclosed
     *         archive entry stream is forcibly closed.
     * @throws FsSyncException if any error conditions apply.
     *         This implies some loss of data!
     */
    @SuppressWarnings("deprecation")
    public static void sync(BitField<FsSyncOption> options)
    throws FsSyncException {
        TConfig.get().getFsManager().sync(options);
    }

    /**
     * Commits all pending changes for all (nested) archive files within the
     * (virtual) directory tree referenced by the (prospective)
     * {@code mountPoint} to their respective parent file system with respect
     * to the given option.
     *
     * @param  mountPoint the prospective mount point of a (federated) file
     *         system, i.e. any (virtual) file system entry.
     * @param  option an option for the synchronization operation.
     * @throws IllegalArgumentException if {@code archive} is not a top level
     *         archive file or the combination of synchronization options is
     *         illegal, e.g. if
     *         {@code FsSyncOption.FORCE_CLOSE_INPUT} is cleared and
     *         {@code FsSyncOption.FORCE_CLOSE_OUTPUT} is set or if
     *         {@code FsSyncOption.ABORT_CHANGES} is set.
     * @throws FsSyncWarningException if <em>only</em> warning conditions
     *         apply.
     *         This implies that the respective parent file system has been
     *         synchronized with constraints, e.g. if
     *         {@code FsSyncOption.FORCE_CLOSE_INPUT} or
     *         {@code FsSyncOption.FORCE_CLOSE_OUTPUT} is set and an unclosed
     *         archive entry stream is forcibly closed.
     * @throws FsSyncException if any error conditions apply.
     *         This implies some loss of data!
     * @see    TFile#isTopLevelArchive()
     */
    public static void sync(TFile mountPoint,
                            FsSyncOption option)
    throws FsSyncException {
        sync(mountPoint, BitField.of(option));
    }

    /**
     * Commits all pending changes for all (nested) archive files within the
     * (virtual) directory tree referenced by the (prospective)
     * {@code mountPoint} to their respective parent file system with respect
     * to the given options.
     *
     * @param  mountPoint the prospective mount point of a (federated) file
     *         system, i.e. any (virtual) file system entry.
     * @param  option an option for the synchronization operation.
     * @param  options an array of additional options for the synchronization
     *         operation.
     * @throws IllegalArgumentException if {@code archive} is not a top level
     *         archive file or the combination of synchronization options is
     *         illegal, e.g. if
     *         {@code FsSyncOption.FORCE_CLOSE_INPUT} is cleared and
     *         {@code FsSyncOption.FORCE_CLOSE_OUTPUT} is set or if
     *         {@code FsSyncOption.ABORT_CHANGES} is set.
     * @throws FsSyncWarningException if <em>only</em> warning conditions
     *         apply.
     *         This implies that the respective parent file system has been
     *         synchronized with constraints, e.g. if
     *         {@code FsSyncOption.FORCE_CLOSE_INPUT} or
     *         {@code FsSyncOption.FORCE_CLOSE_OUTPUT} is set and an unclosed
     *         archive entry stream is forcibly closed.
     * @throws FsSyncException if any error conditions apply.
     *         This implies some loss of data!
     * @see    TFile#isTopLevelArchive()
     */
    public static void sync(TFile mountPoint,
                            FsSyncOption option,
                            FsSyncOption... options)
    throws FsSyncException {
        sync(mountPoint, BitField.of(option, options));
    }

    /**
     * Commits all pending changes for all (nested) archive files within the
     * (virtual) directory tree referenced by the (prospective)
     * {@code mountPoint} to their respective parent file system with respect
     * to the given options.
     *
     * @param  mountPoint a federated file system, i.e. a prospective archive file.
     * @param  options a bit field of options for the synchronization operation.
     * @throws IllegalArgumentException if {@code archive} is not a prospective
     *         archive file or the combination of synchronization options is
     *         illegal, e.g. if
     *         {@code FsSyncOption.FORCE_CLOSE_INPUT} is cleared and
     *         {@code FsSyncOption.FORCE_CLOSE_OUTPUT} is set or if
     *         {@code FsSyncOption.ABORT_CHANGES} is set.
     * @throws FsSyncWarningException if <em>only</em> warning conditions
     *         apply.
     *         This implies that the respective parent file system has been
     *         synchronized with constraints, e.g. if
     *         {@code FsSyncOption.FORCE_CLOSE_INPUT} or
     *         {@code FsSyncOption.FORCE_CLOSE_OUTPUT} is set and an unclosed
     *         archive entry stream is forcibly closed.
     * @throws FsSyncException if any error conditions apply.
     *         This implies some loss of data!
     * @see    TFile#isTopLevelArchive()
     */
    public static void sync(TFile mountPoint,
                            BitField<FsSyncOption> options)
    throws FsSyncException {
        sync(mountPoint.toFsPath().getMountPoint(), options);
    }

    /**
     * Commits all pending changes for all (nested) archive files within the
     * (virtual) directory tree referenced by the (prospective)
     * {@code mountPoint} to their respective parent file system with respect
     * to the given option.
     *
     * @param  mountPoint the prospective mount point of a (federated) file
     *         system, i.e. any (virtual) file system entry.
     * @param  option an option for the synchronization operation.
     * @throws IllegalArgumentException if the combination of synchronization
     *         options is illegal, e.g. if
     *         {@code FsSyncOption.FORCE_CLOSE_INPUT} is cleared and
     *         {@code FsSyncOption.FORCE_CLOSE_OUTPUT} is set or if
     *         {@code FsSyncOption.ABORT_CHANGES} is set.
     * @throws FsSyncWarningException if <em>only</em> warning conditions
     *         apply.
     *         This implies that the respective parent file system has been
     *         synchronized with constraints, e.g. if
     *         {@code FsSyncOption.FORCE_CLOSE_INPUT} or
     *         {@code FsSyncOption.FORCE_CLOSE_OUTPUT} is set and an unclosed
     *         archive entry stream is forcibly closed.
     * @throws FsSyncException if any error conditions apply.
     *         This implies some loss of data!
     */
    public static void sync(FsMountPoint mountPoint,
                            FsSyncOption option)
    throws FsSyncException {
        sync(mountPoint, BitField.of(option));
    }

    /**
     * Commits all pending changes for all (nested) archive files within the
     * (virtual) directory tree referenced by the (prospective)
     * {@code mountPoint} to their respective parent file system with respect
     * to the given options.
     *
     * @param  mountPoint the prospective mount point of a (federated) file
     *         system, i.e. any (virtual) file system entry.
     * @param  option an option for the synchronization operation.
     * @param  options an array of additional options for the synchronization
     *         operation.
     * @throws IllegalArgumentException if the combination of synchronization
     *         options is illegal, e.g. if
     *         {@code FsSyncOption.FORCE_CLOSE_INPUT} is cleared and
     *         {@code FsSyncOption.FORCE_CLOSE_OUTPUT} is set or if
     *         {@code FsSyncOption.ABORT_CHANGES} is set.
     * @throws FsSyncWarningException if <em>only</em> warning conditions
     *         apply.
     *         This implies that the respective parent file system has been
     *         synchronized with constraints, e.g. if
     *         {@code FsSyncOption.FORCE_CLOSE_INPUT} or
     *         {@code FsSyncOption.FORCE_CLOSE_OUTPUT} is set and an unclosed
     *         archive entry stream is forcibly closed.
     * @throws FsSyncException if any error conditions apply.
     *         This implies some loss of data!
     */
    public static void sync(FsMountPoint mountPoint,
                            FsSyncOption option,
                            FsSyncOption... options)
    throws FsSyncException {
        sync(mountPoint, BitField.of(option, options));
    }

    /**
     * Commits all pending changes for all (nested) archive files within the
     * (virtual) directory tree referenced by the (prospective)
     * {@code mountPoint} to their respective parent file system with respect
     * to the given options.
     *
     * @param  mountPoint the prospective mount point of a (federated) file
     *         system, i.e. any (virtual) file system entry.
     * @param  options a bit field of options for the synchronization operation.
     * @throws IllegalArgumentException if the combination of synchronization
     *         options is illegal, e.g. if
     *         {@code FsSyncOption.FORCE_CLOSE_INPUT} is cleared and
     *         {@code FsSyncOption.FORCE_CLOSE_OUTPUT} is set or if
     *         {@code FsSyncOption.ABORT_CHANGES} is set.
     * @throws FsSyncWarningException if <em>only</em> warning conditions
     *         apply.
     *         This implies that the respective parent file system has been
     *         synchronized with constraints, e.g. if
     *         {@code FsSyncOption.FORCE_CLOSE_INPUT} or
     *         {@code FsSyncOption.FORCE_CLOSE_OUTPUT} is set and an unclosed
     *         archive entry stream is forcibly closed.
     * @throws FsSyncException if any error conditions apply.
     *         This implies some loss of data!
     */
    @SuppressWarnings("deprecation")
    public static void sync(FsMountPoint mountPoint,
                            BitField<FsSyncOption> options)
    throws FsSyncException {
        new FsFilteringManager(TConfig.get().getFsManager(), mountPoint)
                .sync(options);
    }
}
