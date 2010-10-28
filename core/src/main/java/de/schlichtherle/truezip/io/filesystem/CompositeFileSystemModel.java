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

/**
 * Defines the common properties of any composite file system.
 *
 * @author Christian Schlichtherle
 * @version $Id$
 */
public interface CompositeFileSystemModel extends FileSystemModel {

    /**
     * Returns the model of the parent file system of this composite file
     * system or {@code null} if this composite file system is not a member of
     * another file system.
     */
    FileSystemModel getParentModel();

    /**
     * Returns {@code true} if and only if the contents of this composite file
     * system have been modified so that it needs
     * {@link CompositeFileSystemController#sync synchronization} with its
     * parent file system.
     */
    boolean isTouched();
}
