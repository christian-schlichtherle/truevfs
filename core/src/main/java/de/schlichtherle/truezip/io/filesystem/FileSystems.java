package de.schlichtherle.truezip.io.filesystem;

import java.net.URI;

/**
 * @author Christian Schlichtherle
 * @version $Id$
 */
public class FileSystems {

    private FileSystems() {
    }

    /**
     * Returns a file system controller for the given mount point.
     * The returned file system controller will use the given parent file
     * system controller to access its root file system entry.
     *
     * @param  mountPoint the non-{@code null}
     *         {@link FileSystemModel#getMountPoint() mount point}
     *         of the (virtual) file system.
     * @param  parentController the nullable file system controller for the
     *         parent file system, if any.
     * @return A non-{@code null} file system controller.
     */
    public static ComponentFileSystemController<?> getController(
            final URI mountPoint,
            final ComponentFileSystemController<?> parentController) {
        throw new UnsupportedOperationException();
    }
}
