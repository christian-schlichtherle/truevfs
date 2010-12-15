/*
 * Copyright (C) 2010 Schlichtherle IT Services
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
package de.schlichtherle.truezip.io.archive.filesystem;

import de.schlichtherle.truezip.io.archive.driver.ArchiveDriver;
import de.schlichtherle.truezip.io.archive.entry.ArchiveEntry;
import de.schlichtherle.truezip.io.filesystem.FileSystemEntry;

/**
 * An empty interface which distinguishes entries created by an archive file
 * system from any other entries, in particular those created by the
 * {@link ArchiveDriver#newEntry factory method} of an archive driver.
 * With the help of this empty interface, an archive file system ensures that
 * when a new archive entry is created, the {@code template} parameter is
 * <em>not</em> an instance of this interface, but possibly a product of the
 * archive entry factory in the archive driver.
 * This enables an archive driver to copy properties specific to its type of
 * archive entries, e.g. the compressed size of ZIP entries.
 * 
 * @author  Christian Schlichtherle
 * @version $Id$
 */
public interface ArchiveFileSystemEntry<AE extends ArchiveEntry>
extends FileSystemEntry<AE> {
}
