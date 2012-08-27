package net.java.truevfs.access;

import java.net.URI;
import net.java.truevfs.kernel.spec.FsMountPoint;
import net.java.truevfs.kernel.spec.FsNodeName;
import net.java.truevfs.kernel.spec.FsNodePath;

/**
 * Defines common properties and operations of {@link TFile} and {@link TPath}.
 * FIXME: Give this interface a more meaningful name. Proposals are welcome!
 * 
 * @author Christian Schlichtherle
 */
public interface TRex {

    //
    // Properties.
    //

    /**
     * Returns the {@link TArchiveDetector} which was used to detect any
     * archive files in the path name of this object at construction time.
     * 
     * @return The {@link TArchiveDetector} which was used to detect any
     *         archive files in the path name of this object at construction
     *         time.
     */
    TArchiveDetector getArchiveDetector();

    /**
     * Returns the file system node path with an absolute URI.
     * Note that multiple calls to this method result in objects which are
     * required to compare {@linkplain Object#equals equal}, but are not
     * necessarily identical.
     * 
     * @return the file system node path with an absolute URI.
     */
    FsNodePath getNodePath();

    /**
     * Returns the file system mount point for this path.
     * Note that multiple calls to this method result in objects which are
     * required to compare {@linkplain Object#equals equal}, but are not
     * necessarily identical.
     * 
     * @return the file system mount point for this path.
     */
    FsMountPoint getMountPoint();

    /**
     * Returns the file system entry name.
     * Note that multiple calls to this method result in objects which are
     * required to compare {@linkplain Object#equals equal}, but are not
     * necessarily identical.
     * 
     * @return the file system entry name.
     */
    FsNodeName getNodeName();

    //
    // Conversions.
    //

    URI toUri();
    TFile toFile();
    TPath toPath();
}
