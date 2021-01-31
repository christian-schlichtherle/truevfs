/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.access;

import net.java.truevfs.kernel.spec.FsMountPoint;
import net.java.truevfs.kernel.spec.FsNodeName;
import net.java.truevfs.kernel.spec.FsNodePath;

import java.net.URI;

/**
 * Defines common properties and operations of {@link TFile} and {@link TPath}.
 * This interface is of little practical use and solely exists for providing
 * a common abstraction layer with a common Javadoc.
 * Application developers should not use it - hence the silly name.
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

    /**
     * Returns the absolute URI for this object.
     *
     * @return the absolute URI for this object.
     */
    URI getUri();

    //
    // Conversions.
    //

    /**
     * Returns a file representation of this object.
     *
     * @return A file representation of this object.
     */
    TFile toFile();

    /**
     * Returns a path representation of this object.
     *
     * @return A path representation of this object.
     */
    TPath toPath();
}
