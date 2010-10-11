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
package de.schlichtherle.truezip.io.socket;

import de.schlichtherle.truezip.io.FileBusyException;

/**
 * Indicates that a {@link CommonOutputSocket common output socket} or its
 * {@link CommonOutputSocketFactory common output socket factory} is busy on
 * output.
 * This exception is guaranteed to be recoverable, meaning it should be
 * possible to write the same common entry again as soon as the destination is
 * not busy anymore and unless another exceptional condition applies.
 *
 * @see InputBusyException
 * @author Christian Schlichtherle
 * @version $Id$
 */
public class OutputBusyException
extends FileBusyException {

    private static final long serialVersionUID = 962318648273654198L;
    
    /**
     * Constructs a new {@code OutputBusyException} with the specified
     * common entry.
     *
     * @param entry The common entry which was tried to write while the
     *        destination was busy.
     */
    public OutputBusyException(CommonEntry entry) {
        super(entry.getName());
    }
}
