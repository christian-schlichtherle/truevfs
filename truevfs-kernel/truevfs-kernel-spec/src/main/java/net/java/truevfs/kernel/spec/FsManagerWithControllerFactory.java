package net.java.truevfs.kernel.spec;

/**
 * A file system manager with a file system controller factory.
 *
 * @since  TrueVFS 0.10
 * @author Christian Schlichtherle
 */
public interface FsManagerWithControllerFactory
extends FsManager,
        FsControllerFactory<FsArchiveDriver<? extends FsArchiveEntry>> {
}
