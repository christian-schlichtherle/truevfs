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

package de.schlichtherle.truezip.io.archive.spi;

import de.schlichtherle.truezip.io.FileBusyException;

/**
 * Thrown to indicate that the {@link InputArchive#getInputStream} method
 * failed because the archive is already busy on input.
 * This exception is guaranteed to be recoverable,
 * meaning it must be possible to read the same entry again as soon as the
 * archive is not busy on input anymore, unless another exceptional condition
 * applies.
 *
 * @author Christian Schlichtherle
 * @version $Id$
 */
public class InputArchiveBusyException extends FileBusyException {
    private static final long serialVersionUID = 1983745618753823654L;

    /**
     * Constructs an instance of {@code InputArchiveBusyException} with
     * the specified archive entry.
     * 
     * @param entry The archive entry which was tried to read while
     *        its associated {@link InputArchive} was busy.
     */
    public InputArchiveBusyException(ArchiveEntry entry) {
        super(entry.getName());
    }
}
