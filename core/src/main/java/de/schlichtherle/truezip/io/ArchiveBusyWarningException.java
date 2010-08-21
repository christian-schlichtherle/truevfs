/*
 * Copyright (C) 2005-2010 Schlichtherle IT Services
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

package de.schlichtherle.truezip.io;

/**
 * Thrown if an archive file has been successfully updated, but some input
 * or output streams for its entries have been forced to close.
 * The canonical path name of the archive file is provided as the detail
 * message.
 * <p>
 * With the exception of their {@code close()} method, any subsequent
 * I/O operation on the closed entry streams will throw an
 * {@link ArchiveEntryStreamClosedException}.
 *
 * @see <a href="package-summary.html#streams">Using Archive Entry Streams</a>
 * @see File#umount
 * @see File#update
 * @author Christian Schlichtherle
 * @version $Id$
 * @since TrueZIP 6.0
 */
public class ArchiveBusyWarningException extends ArchiveWarningException {
    private static final long serialVersionUID = 2635419873651362891L;

    ArchiveBusyWarningException(ArchiveException priorException, String cPath) {
        super(priorException, cPath);
    }
}
