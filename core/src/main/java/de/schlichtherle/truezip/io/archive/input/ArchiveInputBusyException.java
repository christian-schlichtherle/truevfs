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

package de.schlichtherle.truezip.io.archive.input;

import de.schlichtherle.truezip.io.archive.entry.ArchiveEntry;
import de.schlichtherle.truezip.io.file.FileBusyException;

/**
 * Thrown to indicate that the
 * {@link ArchiveInputSocket#newInputStream}
 * method failed because the archive is already busy on input.
 * This exception is guaranteed to be recoverable,
 * meaning it must be possible to read the same entry again as soon as the
 * archive is not busy on input anymore, unless another exceptional condition
 * applies.
 *
 * @author Christian Schlichtherle
 * @version $Id$
 */
public class ArchiveInputBusyException
extends FileBusyException {

    private static final long serialVersionUID = 1983745618753823654L;

    /**
     * Constructs an instance of {@code ArchiveInputBusyException} with
     * the specified archive entry.
     * 
     * @param entry The archive entry which was tried to read while
     *        its associated {@link ArchiveInput} was busy.
     */
    public ArchiveInputBusyException(ArchiveEntry entry) {
        super(entry.getName());
    }
}
