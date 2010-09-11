/*
 * Copyright (C) 2006-2010 Schlichtherle IT Services
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.schlichtherle.truezip.io.archive;

import java.net.URI;

/**
 * Describes general properties of an archive file.
 *
 * @author Christian Schlichtherle
 * @version $Id$
 */
public interface ArchiveDescriptor {

    /**
     * Returns an absolute, hierarchical and normalized Unique Resource
     * Identifier (URI) for the target archive file's <i>mount point</i>
     * in the federated file system.
     * <p>
     * The mount point may be used to construct error messages or to locate
     * and access archive metadata which is stored outside the federated file
     * system, e.g. in-memory stored passwords for RAES encrypted ZIP files.
     * <p>
     * Note that the mount point <em>must not</em> and in some cases even
     * cannot be used to locate and access the archive file in the underlying
     * file system directly, even if the <i>file scheme</i> is used.
     * <p>
     * Implementation notes: If the returned URI uses the <i>file scheme</i>,
     * its path must be canonical in order to be really unique.
     * Furthermore, the path of the URI must end with a {@code '/'} character
     * so that relative URIs can be resolved against it.
     *
     * @return A non-{@code null} URI for the mount point of the target archive
     *         file.
     */
    URI getMountPoint();

    /**
     * @return The descriptor for the enclosing archive of the archive
     *         or {@code null} if it's not enclosed in another archive.
     */
    ArchiveDescriptor getEnclDescriptor();
}
