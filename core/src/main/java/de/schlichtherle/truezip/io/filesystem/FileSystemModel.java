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
package de.schlichtherle.truezip.io.filesystem;

import java.net.URI;

/**
 * Defines the common properties of any file system.
 *
 * @author Christian Schlichtherle
 * @version $Id$
 */
public interface FileSystemModel {

    /**
     * Returns an absolute, hierarchical and normalized Unique Resource
     * Identifier (URI) of the file system's <i>mount point</i> in the
     * federated file system.
     * The path of this URI ends with a {@code '/'} character so that
     * relative URIs can be resolved against it.
     * <p>
     * The mount point may be used to construct error messages or to locate
     * and access file system metadata which is stored outside the federated
     * file system, e.g. in-memory stored passwords for RAES encrypted ZIP
     * files.
     * <p>
     * Implementation note: If the returned URI uses the <i>file scheme</i>,
     * its path needs to be {@link java.io.File#getCanonicalPath() canonical}
     * in order to be really unique.
     *
     * @return A non-{@code null} URI for the mount point of the file system.
     */
    URI getMountPoint();

    /**
     * Returns the model of the parent file system of this composite file
     * system or {@code null} if this composite file system is not a member of
     * another file system.
     */
    FileSystemModel getParentModel();

    /**
     * Returns {@code true} if and only if the contents of this composite file
     * system have been modified so that it needs
     * {@link FileSystemController#sync synchronization} with its parent file
     * system.
     */
    boolean isTouched();
}
