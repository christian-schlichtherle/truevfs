package net.java.truevfs.kernel.spec;

/**
 * A file system manager with a file system controller factory which accepts
 * any archive driver as its context.
 *
 * @since  TrueVFS 0.10
 * @author Christian Schlichtherle
 */
public interface FsArchiveManager
extends FsManager,
        FsControllerFactory<FsArchiveDriver<? extends FsArchiveEntry>> {
}
