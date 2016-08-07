/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.access;

import net.java.truecommons.shed.BitField;
import net.java.truecommons.shed.Filter;
import net.java.truecommons.shed.Visitor;
import net.java.truevfs.kernel.spec.*;

import java.net.URI;
import java.net.URISyntaxException;

import static net.java.truevfs.access.ExpertFeature.Reason.THE_PRESENCE_OR_ABSENCE_OF_SOME_OPTIONS_MAY_YIELD_UNWANTED_SIDE_EFFECTS;
import static net.java.truevfs.kernel.spec.FsSyncOptions.UMOUNT;
import static net.java.truevfs.kernel.spec.FsUriModifier.CANONICALIZE;

/**
 * Static utility methods for virtual file system operations with global scope.
 * If you are not sure which method you should use, try {@link #umount()} -
 * it does the right thing for most use cases.
 *
 * @author Christian Schlichtherle
 */
public final class TVFS {

    private TVFS() { }

    /**
     * Commits all pending changes for all (nested) archive files to their
     * respective parent file system, closes their associated target archive
     * file in order to allow access by third parties
     * (e.g.&#160;other processes), cleans up any temporary allocated resources
     * (e.g.&#160;temporary files) and purges any cached data.
     * <p>
     * Note that temporary files may of used even for read-only access to
     * archive files, so calling this operation is essential.
     * However, if the client application never calls this operation, then it
     * gets called by a shutdown hook.
     * The shutdown hook gets removed as soon as this operation gets called in
     * order to leak no memory in dynamic class loader environments.
     * <p>
     * Calling this method is equivalent to
     * {@link #sync(BitField) sync(FsSyncOptions.UMOUNT)}.
     *
     * @throws FsSyncWarningException if <em>only</em> warning conditions
     *         apply.
     *         This implies that the respective parent file system has been
     *         synchronized with constraints, e.g. if an unclosed archive entry
     *         stream gets forcibly closed.
     * @throws FsSyncException if any error conditions apply.
     */
    public static void umount()
    throws FsSyncWarningException, FsSyncException {
        sync(UMOUNT);
    }

    /**
     * Commits all pending changes for all (nested) archive files within the
     * given (virtual) directory {@code tree} to their respective parent file
     * system, closes their associated target archive file in order to allow
     * access by third parties (e.g.&#160;other processes), cleans up any
     * temporary allocated resources (e.g.&#160;temporary files) and purges any
     * cached data.
     * <p>
     * Calling this method is equivalent to
     * {@link #sync(TFile, BitField) sync(tree, FsSyncOptions.UMOUNT)}.
     *
     * @param  tree a file or directory in the (virtual) file system space.
     * @throws FsSyncWarningException if <em>only</em> warning conditions
     *         apply.
     *         This implies that the respective parent file system has been
     *         synchronized with constraints, e.g. if an unclosed archive entry
     *         stream gets forcibly closed.
     * @throws FsSyncException if any error conditions apply.
     */
    public static void umount(TFile tree)
    throws FsSyncWarningException, FsSyncException {
        sync(tree, UMOUNT);
    }

    /**
     * Commits all pending changes for all (nested) archive files within the
     * given (virtual) directory {@code tree} to their respective parent file
     * system, closes their associated target archive file in order to allow
     * access by third parties (e.g.&#160;other processes), cleans up any
     * temporary allocated resources (e.g.&#160;temporary files) and purges any
     * cached data.
     * <p>
     * Calling this method is equivalent to
     * {@link #sync(FsMountPoint, BitField) sync(tree, FsSyncOptions.UMOUNT)}.
     *
     * @param  tree a file or directory in the (virtual) file system space.
     * @throws FsSyncWarningException if <em>only</em> warning conditions
     *         apply.
     *         This implies that the respective parent file system has been
     *         synchronized with constraints, e.g. if an unclosed archive entry
     *         stream gets forcibly closed.
     * @throws FsSyncException if any error conditions apply.
     */
    public static void umount(FsMountPoint tree)
    throws FsSyncWarningException, FsSyncException {
        sync(tree, UMOUNT);
    }

    /**
     * Returns a mount point for the given (virtual) directory {@code tree}.
     * If {@code tree} refers to a (prospective) archive file, then its mount
     * point gets returned.
     * Otherwise, the path of the file object is used to create a new mount
     * point.
     * Note that making up an artificial mount point like this will only work
     * for use with {@link FsManager#accept(Filter, Visitor)}.
     *
     * @param  tree a file or directory in the (virtual) file system space.
     * @return A mount point for the given (virtual) directory tree.
     */
    @SuppressWarnings("deprecation")
    static FsMountPoint mountPoint(final TFile tree) {
        if (tree.isArchive()) {
            return tree.getController().getModel().getMountPoint(); // fast path
            //return tree.getNodePath().getMountPoint(); // slow path
        }
        try {
            return new FsMountPoint(new URI(tree.getFile().toURI() + "/"),
                                    CANONICALIZE);
        } catch (final URISyntaxException ex) {
            throw new AssertionError(ex);
        }
    }

    /**
     * Commits all pending changes for all (nested) archive files to their
     * respective parent file system with respect to the given options.
     *
     * @param  options an array of options for the synchronization operation.
     * @throws IllegalArgumentException if the combination of synchronization
     *         options is illegal, e.g. if {@code FsSyncOption.ABORT_CHANGES}
     *         is set.
     * @throws FsSyncWarningException if <em>only</em> warning conditions
     *         apply.
     *         This implies that the respective parent file system has been
     *         synchronized with constraints, e.g. if
     *         {@code FsSyncOption.FORCE_CLOSE_IO} is set and an unclosed
     *         archive entry stream gets forcibly closed.
     * @throws FsSyncException if any error conditions apply.
     */
    @ExpertFeature(THE_PRESENCE_OR_ABSENCE_OF_SOME_OPTIONS_MAY_YIELD_UNWANTED_SIDE_EFFECTS)
    public static void sync(FsSyncOption... options)
    throws FsSyncWarningException, FsSyncException {
        sync(FsSyncOptions.of(options));
    }

    /**
     * Commits all pending changes for all (nested) archive files to their
     * respective parent file system with respect to the given options.
     *
     * @param  options a bit field of options for the synchronization operation.
     * @throws IllegalArgumentException if the combination of synchronization
     *         options is illegal, e.g. if {@code FsSyncOption.ABORT_CHANGES}
     *         is set.
     * @throws FsSyncWarningException if <em>only</em> warning conditions
     *         apply.
     *         This implies that the respective parent file system has been
     *         synchronized with constraints, e.g. if
     *         {@code FsSyncOption.FORCE_CLOSE_IO} is set and an unclosed
     *         archive entry stream gets forcibly closed.
     * @throws FsSyncException if any error conditions apply.
     */
    @ExpertFeature(THE_PRESENCE_OR_ABSENCE_OF_SOME_OPTIONS_MAY_YIELD_UNWANTED_SIDE_EFFECTS)
    public static void sync(BitField<FsSyncOption> options)
    throws FsSyncWarningException, FsSyncException {
        new FsSync()
                .manager(TConfig.current().getManager())
                .options(options)
                .run();
    }

    /**
     * Commits all pending changes for all (nested) archive files within the
     * given (virtual) directory {@code tree} to their respective parent file
     * system with respect to the given options.
     *
     * @param  tree a file or directory in the (virtual) file system space.
     * @param  options an array of options for the synchronization operation.
     * @throws IllegalArgumentException if the combination of synchronization
     *         options is illegal, e.g. if {@code FsSyncOption.ABORT_CHANGES}
     *         is set.
     * @throws FsSyncWarningException if <em>only</em> warning conditions
     *         apply.
     *         This implies that the respective parent file system has been
     *         synchronized with constraints, e.g. if
     *         {@code FsSyncOption.FORCE_CLOSE_IO} is set and an unclosed
     *         archive entry stream gets forcibly closed.
     * @throws FsSyncException if any error conditions apply.
     */
    @ExpertFeature(THE_PRESENCE_OR_ABSENCE_OF_SOME_OPTIONS_MAY_YIELD_UNWANTED_SIDE_EFFECTS)
    public static void sync(TFile tree, FsSyncOption... options)
    throws FsSyncWarningException, FsSyncException {
        sync(tree, FsSyncOptions.of(options));
    }

    /**
     * Commits all pending changes for all (nested) archive files within the
     * given (virtual) directory {@code tree} to their respective parent file
     * system with respect to the given options.
     *
     * @param  tree a file or directory in the (virtual) file system space.
     * @param  options a bit field of options for the synchronization operation.
     * @throws IllegalArgumentException if the combination of synchronization
     *         options is illegal, e.g. if {@code FsSyncOption.ABORT_CHANGES}
     *         is set.
     * @throws FsSyncWarningException if <em>only</em> warning conditions
     *         apply.
     *         This implies that the respective parent file system has been
     *         synchronized with constraints, e.g. if
     *         {@code FsSyncOption.FORCE_CLOSE_IO} is set and an unclosed
     *         archive entry stream gets forcibly closed.
     * @throws FsSyncException if any error conditions apply.
     */
    @ExpertFeature(THE_PRESENCE_OR_ABSENCE_OF_SOME_OPTIONS_MAY_YIELD_UNWANTED_SIDE_EFFECTS)
    public static void sync(TFile tree, BitField<FsSyncOption> options)
    throws FsSyncWarningException, FsSyncException {
        sync(mountPoint(tree), options);
    }

    /**
     * Commits all pending changes for all (nested) archive files within the
     * given (virtual) directory {@code tree} to their respective parent file
     * system with respect to the given options.
     *
     * @param  tree a file or directory in the (virtual) file system space.
     * @param  options an array of options for the synchronization operation.
     * @throws IllegalArgumentException if the combination of synchronization
     *         options is illegal, e.g. if {@code FsSyncOption.ABORT_CHANGES}
     *         is set.
     * @throws FsSyncWarningException if <em>only</em> warning conditions
     *         apply.
     *         This implies that the respective parent file system has been
     *         synchronized with constraints, e.g. if
     *         {@code FsSyncOption.FORCE_CLOSE_IO} is set and an unclosed
     *         archive entry stream gets forcibly closed.
     * @throws FsSyncException if any error conditions apply.
     */
    @ExpertFeature(THE_PRESENCE_OR_ABSENCE_OF_SOME_OPTIONS_MAY_YIELD_UNWANTED_SIDE_EFFECTS)
    public static void sync(FsMountPoint tree, FsSyncOption... options)
    throws FsSyncWarningException, FsSyncException {
        sync(tree, FsSyncOptions.of(options));
    }

    /**
     * Commits all pending changes for all (nested) archive files within the
     * given (virtual) directory {@code tree} to their respective parent file
     * system with respect to the given options.
     *
     * @param  tree a file or directory in the (virtual) file system space.
     * @param  options a bit field of options for the synchronization operation.
     * @throws IllegalArgumentException if the combination of synchronization
     *         options is illegal, e.g. if {@code FsSyncOption.ABORT_CHANGES}
     *         is set.
     * @throws FsSyncWarningException if <em>only</em> warning conditions
     *         apply.
     *         This implies that the respective parent file system has been
     *         synchronized with constraints, e.g. if
     *         {@code FsSyncOption.FORCE_CLOSE_IO} is set and an unclosed
     *         archive entry stream gets forcibly closed.
     * @throws FsSyncException if any error conditions apply.
     */
    @ExpertFeature(THE_PRESENCE_OR_ABSENCE_OF_SOME_OPTIONS_MAY_YIELD_UNWANTED_SIDE_EFFECTS)
    public static void sync(FsMountPoint tree, BitField<FsSyncOption> options)
    throws FsSyncWarningException, FsSyncException {
        new FsSync()
                .manager(TConfig.current().getManager())
                .filter(FsControllerFilter.forPrefix(tree))
                .options(options)
                .run();
    }

}
